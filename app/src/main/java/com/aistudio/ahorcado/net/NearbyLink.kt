package com.aistudio.ahorcado.net

import android.content.Context
import android.os.SystemClock
import android.util.Log
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

/**
 * Envoltorio de Nearby Connections (estrategia P2P_STAR).
 *
 * Decisiones de estabilidad:
 * - Las conexiones entrantes se aceptan automáticamente (juego de fiesta),
 *   pero el repositorio decide quién entra a la partida.
 * - Advertising y discovery se controlan por separado: el host sigue
 *   anunciándose en el lobby para aceptar rezagados y lo detiene al empezar.
 * - Todos los callbacks están envueltos en try/catch: un error en la capa
 *   de red nunca tumba el juego.
 */
object NearbyLink {

    private const val TAG = "NearbyLink"
    const val SERVICE_ID = "com.aistudio.ahorcado.p2p"
    private val STRATEGY: Strategy = Strategy.P2P_STAR

    sealed interface LinkEvent {
        /** Un extremo se conectó (ya aceptado). */
        data class PeerConnected(val endpointId: String, val endpointName: String) : LinkEvent

        /** Un extremo se desconectó o falló la conexión. */
        data class PeerLost(val endpointId: String) : LinkEvent

        /** Bytes recibidos de un extremo. */
        data class BytesReceived(val endpointId: String, val bytes: ByteArray) : LinkEvent

        /** Discovery: sala encontrada. */
        data class EndpointFound(val endpointId: String, val name: String) : LinkEvent

        /** Discovery: sala perdida. */
        data class EndpointLost(val endpointId: String) : LinkEvent
    }

    /** Suscriptor único (el repositorio). Se invoca en hilos de Nearby: no bloquear. */
    var listener: ((LinkEvent) -> Unit)? = null

    private var client: ConnectionsClient? = null
    private var nickname: String = "Jugador"
    private val peers = mutableSetOf<String>()
    private val lock = Any()

    /** Copia thread-safe de los extremos conectados. */
    val connectedPeers: Set<String>
        get() = synchronized(lock) { peers.toSet() }

    private fun getClient(context: Context): ConnectionsClient? {
        return try {
            if (client == null) {
                client = Nearby.getConnectionsClient(context.applicationContext)
            }
            client
        } catch (e: Throwable) {
            Log.e(TAG, "ConnectionsClient no disponible", e)
            null
        }
    }

    // ---------------- callbacks ----------------

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            try {
                if (payload.type == Payload.Type.BYTES) {
                    payload.asBytes()?.let { bytes ->
                        listener?.invoke(LinkEvent.BytesReceived(endpointId, bytes))
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "onPayloadReceived", e)
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) = Unit
    }

    private val lifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            try {
                // Aceptación automática: el repositorio filtra quién juega.
                client?.acceptConnection(endpointId, payloadCallback)
            } catch (e: Throwable) {
                Log.e(TAG, "acceptConnection", e)
            }
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            try {
                if (resolution.status.isSuccess) {
                    synchronized(lock) { peers.add(endpointId) }
                    Log.d(TAG, "Conectado: $endpointId")
                    listener?.invoke(LinkEvent.PeerConnected(endpointId, endpointId))
                } else {
                    Log.w(TAG, "Conexión fallida $endpointId: ${resolution.status.statusCode}")
                    synchronized(lock) { peers.remove(endpointId) }
                    listener?.invoke(LinkEvent.PeerLost(endpointId))
                }
            } catch (e: Throwable) {
                Log.e(TAG, "onConnectionResult", e)
            }
        }

        override fun onDisconnected(endpointId: String) {
            try {
                synchronized(lock) { peers.remove(endpointId) }
                Log.d(TAG, "Desconectado: $endpointId")
                listener?.invoke(LinkEvent.PeerLost(endpointId))
            } catch (e: Throwable) {
                Log.e(TAG, "onDisconnected", e)
            }
        }
    }

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            try {
                listener?.invoke(LinkEvent.EndpointFound(endpointId, info.endpointName))
            } catch (e: Throwable) {
                Log.e(TAG, "onEndpointFound", e)
            }
        }

        override fun onEndpointLost(endpointId: String) {
            try {
                listener?.invoke(LinkEvent.EndpointLost(endpointId))
            } catch (e: Throwable) {
                Log.e(TAG, "onEndpointLost", e)
            }
        }
    }

    // ---------------- API ----------------

    /**
     * Empieza a anunciarse como sala. Devuelve false si Play Services no está.
     */
    fun startHosting(context: Context, nickname: String): Boolean {
        return try {
            this.nickname = nickname
            val c = getClient(context) ?: return false
            // Limpia cualquier estado previo sin matar al listener.
            runCatching { c.stopDiscovery() }
            val opts = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
            var ok = false
            val latch = java.util.concurrent.CountDownLatch(1)
            c.startAdvertising(nickname, SERVICE_ID, lifecycleCallback, opts)
                .addOnSuccessListener { ok = true; latch.countDown() }
                .addOnFailureListener { e ->
                    Log.e(TAG, "startAdvertising", e)
                    latch.countDown()
                }
            latch.await(8, java.util.concurrent.TimeUnit.SECONDS)
            ok
        } catch (e: Throwable) {
            Log.e(TAG, "startHosting", e)
            false
        }
    }

    /**
     * Empieza a buscar salas. Devuelve false si Play Services no está.
     */
    fun startDiscovering(context: Context, nickname: String): Boolean {
        return try {
            this.nickname = nickname
            val c = getClient(context) ?: return false
            runCatching { c.stopAdvertising() }
            val opts = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
            var ok = false
            val latch = java.util.concurrent.CountDownLatch(1)
            c.startDiscovery(SERVICE_ID, discoveryCallback, opts)
                .addOnSuccessListener { ok = true; latch.countDown() }
                .addOnFailureListener { e ->
                    Log.e(TAG, "startDiscovery", e)
                    latch.countDown()
                }
            latch.await(8, java.util.concurrent.TimeUnit.SECONDS)
            ok
        } catch (e: Throwable) {
            Log.e(TAG, "startDiscovering", e)
            false
        }
    }

    fun connectTo(endpointId: String) {
        try {
            client?.requestConnection(nickname, endpointId, lifecycleCallback)
                ?.addOnFailureListener { e -> Log.e(TAG, "requestConnection", e) }
        } catch (e: Throwable) {
            Log.e(TAG, "connectTo", e)
        }
    }

    fun send(bytes: ByteArray, targets: Collection<String>) {
        try {
            if (targets.isEmpty()) return
            val c = client ?: return
            val payload = Payload.fromBytes(bytes)
            c.sendPayload(targets.toList(), payload)
                .addOnFailureListener { e -> Log.e(TAG, "sendPayload", e) }
        } catch (e: Throwable) {
            Log.e(TAG, "send", e)
        }
    }

    /** Expulsa a un extremo (el host ya le avisó con KICKED). */
    fun disconnectPeer(endpointId: String) {
        try {
            client?.disconnectFromEndpoint(endpointId)
        } catch (e: Throwable) {
            Log.e(TAG, "disconnectPeer", e)
        } finally {
            synchronized(lock) { peers.remove(endpointId) }
        }
    }

    fun stopAdvertising() {
        runCatching { client?.stopAdvertising() }
    }

    fun stopDiscovery() {
        runCatching { client?.stopDiscovery() }
    }

    /**
     * Corta todo: extremos, advertising y discovery. Llamar al salir al menú.
     */
    fun stopAll() {
        try {
            client?.stopAllEndpoints()
        } catch (e: Throwable) {
            Log.e(TAG, "stopAllEndpoints", e)
        } finally {
            synchronized(lock) { peers.clear() }
        }
    }

    /** Tiempo monotónico para deadlines de turno. */
    fun now(): Long = SystemClock.elapsedRealtime()
}
