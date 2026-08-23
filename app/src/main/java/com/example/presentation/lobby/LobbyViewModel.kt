package com.example.presentation.lobby

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.DiscoveredHost
import com.example.data.models.GameState
import com.example.data.models.Player
import com.example.data.repository.GameRepository
import com.example.network.NearbyConnectionState
import com.example.network.NearbyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LobbyUiState(
    val playerName: String = "Jugador 1",
    val isHost: Boolean = false,
    val isCreatingRoom: Boolean = false,
    val isSearchingRooms: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

class LobbyViewModel(
    private val repository: GameRepository = GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LobbyUiState())
    val uiState: StateFlow<LobbyUiState> = _uiState.asStateFlow()

    val connectionState: StateFlow<NearbyConnectionState> = NearbyManager.connectionState
    val discoveredHosts: StateFlow<List<DiscoveredHost>> = NearbyManager.discoveredHosts
    val players: StateFlow<List<Player>> = repository.players
    val localPlayer: StateFlow<Player> = repository.localPlayer

    fun setPlayerName(name: String) {
        _uiState.value = _uiState.value.copy(playerName = name)
    }

    fun startHosting(context: Context) {
        val name = _uiState.value.playerName.trim().ifEmpty { "Anfitrión" }
        _uiState.value = _uiState.value.copy(isHost = true, isCreatingRoom = true, errorMessage = null)

        repository.createGameAsHost(context, name) { success, error ->
            _uiState.value = _uiState.value.copy(
                isCreatingRoom = false,
                errorMessage = if (!success) error ?: "Aviso al publicar sala Nearby" else null,
                infoMessage = if (success) "Sala creada. Esperando jugadores..." else null
            )
        }
    }

    fun startSoloPractice(onSuccess: () -> Unit) {
        val name = _uiState.value.playerName.trim().ifEmpty { "Jugador 1" }
        _uiState.value = _uiState.value.copy(isHost = true, isCreatingRoom = false, errorMessage = null)
        repository.startLocalSoloGame(name)
        onSuccess()
    }

    fun addLocalPlayer(name: String = "") {
        val currentSize = players.value.size
        val playerName = name.ifBlank { "Jugador ${currentSize + 1}" }
        repository.addLocalPlayer(playerName)
    }

    fun startDiscovery(context: Context) {
        val name = _uiState.value.playerName.trim().ifEmpty { "Jugador" }
        _uiState.value = _uiState.value.copy(isHost = false, isSearchingRooms = true, errorMessage = null)

        repository.startDiscoveryAsClient(context, name) { success, error ->
            _uiState.value = _uiState.value.copy(
                isSearchingRooms = false,
                errorMessage = if (!success) error ?: "Error al buscar salas" else null,
                infoMessage = if (success) "Buscando salas cercanas..." else null
            )
        }
    }

    fun joinRoom(host: DiscoveredHost) {
        _uiState.value = _uiState.value.copy(errorMessage = null)
        repository.joinHost(host.endpointId) { success, error ->
            if (!success) {
                _uiState.value = _uiState.value.copy(errorMessage = error ?: "No se pudo conectar a la sala")
            }
        }
    }

    fun resetToIdle() {
        NearbyManager.stopAll()
        _uiState.value = _uiState.value.copy(
            isCreatingRoom = false,
            isSearchingRooms = false,
            errorMessage = null,
            infoMessage = null
        )
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, infoMessage = null)
    }
}
