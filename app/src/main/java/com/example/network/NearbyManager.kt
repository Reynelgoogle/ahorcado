package com.example.network

import android.content.Context
import android.util.Log
import com.example.data.models.DiscoveredHost
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class NearbyConnectionState {
    object Idle : NearbyConnectionState()
    data class Advertising(val localName: String) : NearbyConnectionState()
    object Discovering : NearbyConnectionState()
    data class Connected(val connectedEndpoints: List<String>, val isHost: Boolean) : NearbyConnectionState()
    data class Error(val message: String) : NearbyConnectionState()
}

/**
 * Gestor de Nearby Connections con estrategia P2P_STAR (1 Host conectado a hasta N Clientes).
 */
object NearbyManager {

    private const val TAG = "NearbyManager"
    const val SERVICE_ID = "com.example.ahorcado.p2p"
    val STRATEGY: Strategy = Strategy.P2P_STAR

    private var connectionsClient: ConnectionsClient? = null
    private var isHostMode: Boolean = false
    private var localPlayerName: String = "Jugador"

    private val _connectionState = MutableStateFlow<NearbyConnectionState>(NearbyConnectionState.Idle)
    val connectionState: StateFlow<NearbyConnectionState> = _connectionState.asStateFlow()

    private val _discoveredHosts = MutableStateFlow<List<DiscoveredHost>>(emptyList())
    val discoveredHosts: StateFlow<List<DiscoveredHost>> = _discoveredHosts.asStateFlow()

    private val connectedEndpointIds = mutableListOf<String>()

    // Callback para datos recibidos
    var onPayloadReceived: ((endpointId: String, bytes: ByteArray) -> Unit)? = null
    var onEndpointConnected: ((endpointId: String, endpointName: String) -> Unit)? = null
    var onEndpointDisconnected: ((endpointId: String) -> Unit)? = null

    private fun getClient(context: Context): ConnectionsClient? {
        return try {
            if (connectionsClient == null) {
                connectionsClient = Nearby.getConnectionsClient(context.applicationContext)
            }
            connectionsClient
        } catch (e: Throwable) {
            Log.e(TAG, "Error inicializando ConnectionsClient: ${e.message}", e)
            null
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            try {
                if (payload.type == Payload.Type.BYTES) {
                    val bytes = payload.asBytes() ?: return
                    onPayloadReceived?.invoke(endpointId, bytes)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error procesando payload: ${e.message}")
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // No se requiere para BYTES pequeños
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            try {
                Log.d(TAG, "Conexión iniciada con: $endpointId (${info.endpointName}). Aceptando automáticamente...")
                connectionsClient?.acceptConnection(endpointId, payloadCallback)
            } catch (e: Throwable) {
                Log.e(TAG, "Error al aceptar conexión con $endpointId: ${e.message}")
            }
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            try {
                if (resolution.status.isSuccess) {
                    Log.d(TAG, "Conexión exitosa con $endpointId")
                    if (!connectedEndpointIds.contains(endpointId)) {
                        connectedEndpointIds.add(endpointId)
                    }
                    _connectionState.value = NearbyConnectionState.Connected(
                        connectedEndpoints = connectedEndpointIds.toList(),
                        isHost = isHostMode
                    )
                    onEndpointConnected?.invoke(endpointId, endpointId)
                } else {
                    Log.w(TAG, "Conexión rechazada/fallida con $endpointId: ${resolution.status.statusCode}")
                    connectedEndpointIds.remove(endpointId)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error en onConnectionResult: ${e.message}")
            }
        }

        override fun onDisconnected(endpointId: String) {
            try {
                Log.d(TAG, "Endpoint desconectado: $endpointId")
                connectedEndpointIds.remove(endpointId)
                onEndpointDisconnected?.invoke(endpointId)
                if (connectedEndpointIds.isEmpty() && !isHostMode) {
                    _connectionState.value = NearbyConnectionState.Idle
                } else {
                    _connectionState.value = NearbyConnectionState.Connected(
                        connectedEndpoints = connectedEndpointIds.toList(),
                        isHost = isHostMode
                    )
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error en onDisconnected: ${e.message}")
            }
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            try {
                Log.d(TAG, "Anfitrión encontrado: $endpointId - ${info.endpointName}")
                val currentList = _discoveredHosts.value.toMutableList()
                if (currentList.none { it.endpointId == endpointId }) {
                    currentList.add(DiscoveredHost(endpointId = endpointId, name = info.endpointName))
                    _discoveredHosts.value = currentList
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error en onEndpointFound: ${e.message}")
            }
        }

        override fun onEndpointLost(endpointId: String) {
            try {
                Log.d(TAG, "Anfitrión perdido: $endpointId")
                val currentList = _discoveredHosts.value.toMutableList()
                currentList.removeAll { it.endpointId == endpointId }
                _discoveredHosts.value = currentList
            } catch (e: Throwable) {
                Log.e(TAG, "Error en onEndpointLost: ${e.message}")
            }
        }
    }

    fun startAdvertising(context: Context, hostName: String, onResult: (Boolean, String?) -> Unit) {
        try {
            stopAll(context)
            isHostMode = true
            localPlayerName = hostName
            val client = getClient(context)
            if (client == null) {
                _connectionState.value = NearbyConnectionState.Error("Google Play Services no disponible.")
                onResult(false, "Google Play Services no disponible.")
                return
            }

            val advertisingOptions = AdvertisingOptions.Builder()
                .setStrategy(STRATEGY)
                .build()

            client.startAdvertising(hostName, SERVICE_ID, connectionLifecycleCallback, advertisingOptions)
                .addOnSuccessListener {
                    Log.d(TAG, "Publicando sala como Host: $hostName")
                    _connectionState.value = NearbyConnectionState.Advertising(hostName)
                    onResult(true, null)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Fallo al iniciar publicidad: ${e.message}", e)
                    _connectionState.value = NearbyConnectionState.Error("Aviso Nearby: ${e.localizedMessage ?: e.message}")
                    onResult(false, e.message)
                }
        } catch (e: Throwable) {
            Log.e(TAG, "Excepción en startAdvertising: ${e.message}", e)
            _connectionState.value = NearbyConnectionState.Error("No se pudo iniciar sala: ${e.message}")
            onResult(false, e.message)
        }
    }

    fun startDiscovery(context: Context, clientName: String, onResult: (Boolean, String?) -> Unit) {
        try {
            stopAll(context)
            isHostMode = false
            localPlayerName = clientName
            _discoveredHosts.value = emptyList()
            val client = getClient(context)
            if (client == null) {
                _connectionState.value = NearbyConnectionState.Error("Google Play Services no disponible.")
                onResult(false, "Google Play Services no disponible.")
                return
            }

            val discoveryOptions = DiscoveryOptions.Builder()
                .setStrategy(STRATEGY)
                .build()

            client.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, discoveryOptions)
                .addOnSuccessListener {
                    Log.d(TAG, "Buscando salas...")
                    _connectionState.value = NearbyConnectionState.Discovering
                    onResult(true, null)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Fallo al iniciar descubrimiento: ${e.message}", e)
                    _connectionState.value = NearbyConnectionState.Error("Aviso búsqueda: ${e.localizedMessage ?: e.message}")
                    onResult(false, e.message)
                }
        } catch (e: Throwable) {
            Log.e(TAG, "Excepción en startDiscovery: ${e.message}", e)
            _connectionState.value = NearbyConnectionState.Error("No se pudo buscar salas: ${e.message}")
            onResult(false, e.message)
        }
    }

    fun requestConnection(endpointId: String, onResult: (Boolean, String?) -> Unit) {
        try {
            val client = connectionsClient ?: run {
                onResult(false, "Nearby Client no inicializado")
                return
            }

            val list = _discoveredHosts.value.map {
                if (it.endpointId == endpointId) it.copy(isConnecting = true) else it
            }
            _discoveredHosts.value = list

            client.requestConnection(localPlayerName, endpointId, connectionLifecycleCallback)
                .addOnSuccessListener {
                    Log.d(TAG, "Solicitud de conexión enviada a: $endpointId")
                    onResult(true, null)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error enviando solicitud de conexión: ${e.message}", e)
                    val restoredList = _discoveredHosts.value.map {
                        if (it.endpointId == endpointId) it.copy(isConnecting = false) else it
                    }
                    _discoveredHosts.value = restoredList
                    onResult(false, e.message)
                }
        } catch (e: Throwable) {
            Log.e(TAG, "Excepción en requestConnection: ${e.message}", e)
            onResult(false, e.message)
        }
    }

    fun sendPayload(bytes: ByteArray, targetEndpointIds: List<String> = connectedEndpointIds) {
        try {
            if (targetEndpointIds.isEmpty()) return
            val client = connectionsClient ?: return
            val payload = Payload.fromBytes(bytes)
            client.sendPayload(targetEndpointIds, payload)
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error enviando payload a $targetEndpointIds: ${e.message}")
                }
        } catch (e: Throwable) {
            Log.e(TAG, "Excepción en sendPayload: ${e.message}")
        }
    }

    fun stopDiscovery() {
        try {
            connectionsClient?.stopDiscovery()
        } catch (e: Throwable) {
            Log.e(TAG, "Error en stopDiscovery: ${e.message}")
        }
    }

    fun stopAdvertising() {
        try {
            connectionsClient?.stopAdvertising()
        } catch (e: Throwable) {
            Log.e(TAG, "Error en stopAdvertising: ${e.message}")
        }
    }

    fun stopAll(context: Context? = null) {
        try {
            val client = if (context != null) getClient(context) else connectionsClient
            client?.stopAllEndpoints()
        } catch (e: Throwable) {
            Log.e(TAG, "Error en stopAllEndpoints: ${e.message}")
        } finally {
            connectedEndpointIds.clear()
            _discoveredHosts.value = emptyList()
            _connectionState.value = NearbyConnectionState.Idle
        }
    }
}

