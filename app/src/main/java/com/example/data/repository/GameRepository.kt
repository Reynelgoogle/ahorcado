package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.models.GameState
import com.example.data.models.GameStatus
import com.example.data.models.Player
import com.example.domain.usecases.GuessLetterUseCase
import com.example.network.MessageProtocol
import com.example.network.NearbyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

sealed class GameUiEvent {
    data class Notification(val message: String) : GameUiEvent()
    data class PlayerJoined(val playerName: String) : GameUiEvent()
    data class GameStarted(val wordLength: Int) : GameUiEvent()
    object NavigateToWordSelection : GameUiEvent()
    object NavigateToLobby : GameUiEvent()
}

/**
 * Repositorio central que sincroniza las jugadas y el estado del juego
 * mediante Nearby Connections.
 */
object GameRepository {

    private const val TAG = "GameRepository"
    private val scope = CoroutineScope(Dispatchers.Main)
    private val guessLetterUseCase = GuessLetterUseCase()

    private val _localPlayer = MutableStateFlow(Player(id = UUID.randomUUID().toString(), name = "Jugador 1", isHost = false))
    val localPlayer: StateFlow<Player> = _localPlayer.asStateFlow()

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players.asStateFlow()

    private val _gameEvents = MutableSharedFlow<GameUiEvent>(extraBufferCapacity = 16)
    val gameEvents: SharedFlow<GameUiEvent> = _gameEvents.asSharedFlow()

    private var turnTimerJob: Job? = null

    // Mapeo de endpointId a Player
    private val endpointToPlayerMap = mutableMapOf<String, Player>()

    init {
        // Escuchar datos de red de Nearby Connections
        NearbyManager.onPayloadReceived = { endpointId, bytes ->
            handleIncomingMessage(endpointId, bytes)
        }

        NearbyManager.onEndpointConnected = { endpointId, _ ->
            if (_localPlayer.value.isHost) {
                Log.d(TAG, "Nuevo endpoint conectado al host: $endpointId. Enviando estado inicial...")
                val stateMsg = MessageProtocol.serializeGameState(_gameState.value)
                NearbyManager.sendPayload(stateMsg, listOf(endpointId))
            } else {
                // Cliente envía su nombre y ID al Host
                Log.d(TAG, "Conectado al host ($endpointId). Enviando PlayerJoin...")
                val joinMsg = MessageProtocol.serializePlayerJoin(_localPlayer.value.name, _localPlayer.value.id)
                NearbyManager.sendPayload(joinMsg, listOf(endpointId))
            }
        }

        NearbyManager.onEndpointDisconnected = { endpointId ->
            val disconnectedPlayer = endpointToPlayerMap.remove(endpointId)
            if (disconnectedPlayer != null) {
                val updatedPlayers = _players.value.filter { it.id != disconnectedPlayer.id }
                _players.value = updatedPlayers

                if (_localPlayer.value.isHost) {
                    val currentTurn = if (_gameState.value.currentTurnPlayerId == disconnectedPlayer.id) {
                        updatedPlayers.firstOrNull()?.id ?: ""
                    } else {
                        _gameState.value.currentTurnPlayerId
                    }
                    val updatedState = _gameState.value.copy(
                        players = updatedPlayers,
                        currentTurnPlayerId = currentTurn
                    )
                    _gameState.value = updatedState
                    broadcastGameState(updatedState)
                }
                _gameEvents.tryEmit(GameUiEvent.Notification("${disconnectedPlayer.name} se ha desconectado."))
            }
        }
    }

    fun createGameAsHost(context: Context, hostName: String, onResult: (Boolean, String?) -> Unit) {
        val hostPlayer = Player(
            id = UUID.randomUUID().toString(),
            name = hostName.ifBlank { "Anfitrión" },
            isHost = true,
            colorIndex = 0
        )
        _localPlayer.value = hostPlayer
        _players.value = listOf(hostPlayer)
        _gameState.value = GameState(
            status = GameStatus.WAITING_PLAYERS,
            players = listOf(hostPlayer),
            currentTurnPlayerId = hostPlayer.id
        )

        NearbyManager.startAdvertising(context, hostName, onResult)
    }

    fun startLocalSoloGame(playerName: String) {
        NearbyManager.stopAll()
        val player = Player(
            id = UUID.randomUUID().toString(),
            name = playerName.ifBlank { "Jugador 1" },
            isHost = true,
            colorIndex = 0
        )
        _localPlayer.value = player
        _players.value = listOf(player)
        _gameState.value = GameState(
            status = GameStatus.WAITING_PLAYERS,
            players = listOf(player),
            currentTurnPlayerId = player.id
        )
    }

    fun addLocalPlayer(name: String) {
        val current = _players.value
        if (current.size >= 4) return
        val newPlayer = Player(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Jugador ${current.size + 1}" },
            isHost = false,
            colorIndex = current.size % 4
        )
        val updated = current + newPlayer
        _players.value = updated
        val updatedState = _gameState.value.copy(
            players = updated,
            currentTurnPlayerId = if (_gameState.value.currentTurnPlayerId.isEmpty()) updated.first().id else _gameState.value.currentTurnPlayerId
        )
        _gameState.value = updatedState
        broadcastGameState(updatedState)
    }

    fun startDiscoveryAsClient(context: Context, clientName: String, onResult: (Boolean, String?) -> Unit) {
        val player = Player(
            id = UUID.randomUUID().toString(),
            name = clientName.ifBlank { "Jugador" },
            isHost = false,
            colorIndex = 1
        )
        _localPlayer.value = player
        _players.value = listOf(player)
        _gameState.value = GameState(
            status = GameStatus.WAITING_PLAYERS,
            players = listOf(player)
        )

        NearbyManager.startDiscovery(context, clientName, onResult)
    }

    fun joinHost(endpointId: String, onResult: (Boolean, String?) -> Unit) {
        NearbyManager.requestConnection(endpointId, onResult)
    }

    fun selectWordAndStartGame(secretWord: String, category: String = "General", hint: String = ""): Boolean {
        if (!_localPlayer.value.isHost) return false

        val currentPlayers = _players.value
        val hostId = _localPlayer.value.id
        val creatorId = hostId
        // En multijugador, los adivinadores son los que no crearon la palabra
        val guessers = currentPlayers.filter { it.id != creatorId }
        val firstTurnId = if (guessers.isNotEmpty()) guessers.first().id else hostId

        val newState = GameState(
            secretWord = secretWord.uppercase().trim(),
            category = category,
            hint = hint,
            hintRevealed = false,
            revealedLetters = emptySet(),
            guessedLetters = emptySet(),
            errors = 0,
            maxErrors = 6,
            status = GameStatus.PLAYING,
            players = currentPlayers,
            wordCreatorPlayerId = creatorId,
            currentTurnPlayerId = firstTurnId,
            turnTimeRemainingSec = 20
        )

        _gameState.value = newState
        broadcastGameState(newState)
        _gameEvents.tryEmit(GameUiEvent.GameStarted(secretWord.length))
        startTurnTimer()
        return true
    }

    fun revealHint() {
        if (_localPlayer.value.isHost) {
            val updated = _gameState.value.copy(hintRevealed = true)
            _gameState.value = updated
            broadcastGameState(updated)
        } else {
            val msg = MessageProtocol.serializeRevealHint()
            NearbyManager.sendPayload(msg)
        }
    }

    fun guessLetter(letter: Char) {
        val upper = letter.uppercaseChar()
        val myId = _localPlayer.value.id

        if (_localPlayer.value.isHost) {
            val res = guessLetterUseCase(_gameState.value, upper, myId)
            val nextState = res.updatedState.copy(turnTimeRemainingSec = 20)
            _gameState.value = nextState
            broadcastGameState(nextState)

            if (nextState.status == GameStatus.PLAYING) {
                startTurnTimer()
            } else {
                turnTimerJob?.cancel()
            }
        } else {
            val msg = MessageProtocol.serializeGuessLetter(upper, myId)
            NearbyManager.sendPayload(msg)
        }
    }

    private fun startTurnTimer() {
        turnTimerJob?.cancel()
        if (!_localPlayer.value.isHost) return

        turnTimerJob = scope.launch {
            while (isActive && _gameState.value.status == GameStatus.PLAYING) {
                delay(1000)
                val current = _gameState.value.turnTimeRemainingSec
                if (current > 1) {
                    val updated = _gameState.value.copy(turnTimeRemainingSec = current - 1)
                    _gameState.value = updated
                    broadcastGameState(updated)
                } else {
                    // Tiempo de turno agotado: pasar al siguiente adivinador
                    val guessers = _gameState.value.players.filter { it.id != _gameState.value.wordCreatorPlayerId }
                    val activeGuessers = if (guessers.isNotEmpty()) guessers else _gameState.value.players
                    if (activeGuessers.size > 1) {
                        val currentIdx = activeGuessers.indexOfFirst { it.id == _gameState.value.currentTurnPlayerId }
                        val nextIdx = if (currentIdx >= 0) (currentIdx + 1) % activeGuessers.size else 0
                        val updated = _gameState.value.copy(
                            currentTurnPlayerId = activeGuessers[nextIdx].id,
                            turnTimeRemainingSec = 20
                        )
                        _gameState.value = updated
                        broadcastGameState(updated)
                    } else {
                        // Un solo adivinador: agotar turno cuenta como error (fallo)
                        val currentErrors = _gameState.value.errors + 1
                        val isLost = currentErrors >= _gameState.value.maxErrors
                        val updated = _gameState.value.copy(
                            errors = currentErrors,
                            status = if (isLost) GameStatus.LOST else GameStatus.PLAYING,
                            turnTimeRemainingSec = 20
                        )
                        _gameState.value = updated
                        broadcastGameState(updated)
                        if (isLost) break
                    }
                }
            }
        }
    }

    fun resetGameToWordSelection() {
        turnTimerJob?.cancel()
        if (!_localPlayer.value.isHost) return
        val currentPlayers = _players.value
        val resetState = GameState(
            status = GameStatus.SELECTING_WORD,
            players = currentPlayers,
            currentTurnPlayerId = currentPlayers.firstOrNull()?.id ?: _localPlayer.value.id
        )
        _gameState.value = resetState
        broadcastGameState(resetState)
        _gameEvents.tryEmit(GameUiEvent.NavigateToWordSelection)
    }

    fun returnToLobby() {
        turnTimerJob?.cancel()
        NearbyManager.stopAll()
        _players.value = listOf(_localPlayer.value)
        _gameState.value = GameState(status = GameStatus.WAITING_PLAYERS)
        _gameEvents.tryEmit(GameUiEvent.NavigateToLobby)
    }

    private fun handleIncomingMessage(endpointId: String, bytes: ByteArray) {
        when (val message = MessageProtocol.parseMessage(bytes)) {
            is MessageProtocol.ParsedMessage.GameStateMessage -> {
                val prevStatus = _gameState.value.status
                val newStatus = message.state.status
                _gameState.value = message.state
                _players.value = message.state.players

                // Sincronización de navegación en clientes al cambiar de estado
                if (newStatus == GameStatus.PLAYING && prevStatus != GameStatus.PLAYING) {
                    _gameEvents.tryEmit(GameUiEvent.GameStarted(message.state.secretWord.length))
                } else if (newStatus == GameStatus.SELECTING_WORD && prevStatus != GameStatus.SELECTING_WORD) {
                    _gameEvents.tryEmit(GameUiEvent.NavigateToWordSelection)
                } else if (newStatus == GameStatus.WAITING_PLAYERS && prevStatus != GameStatus.WAITING_PLAYERS) {
                    _gameEvents.tryEmit(GameUiEvent.NavigateToLobby)
                }
            }

            is MessageProtocol.ParsedMessage.PlayerJoinMessage -> {
                if (_localPlayer.value.isHost) {
                    val nextColorIndex = (_players.value.size) % 4
                    val newPlayer = Player(
                        id = message.playerId,
                        name = message.name,
                        isHost = false,
                        colorIndex = nextColorIndex
                    )
                    endpointToPlayerMap[endpointId] = newPlayer

                    val currentList = _players.value.filter { it.id != newPlayer.id } + newPlayer
                    _players.value = currentList

                    val updatedState = _gameState.value.copy(
                        players = currentList,
                        currentTurnPlayerId = if (_gameState.value.currentTurnPlayerId.isEmpty()) _localPlayer.value.id else _gameState.value.currentTurnPlayerId
                    )
                    _gameState.value = updatedState
                    broadcastGameState(updatedState)
                    _gameEvents.tryEmit(GameUiEvent.PlayerJoined(newPlayer.name))
                }
            }

            is MessageProtocol.ParsedMessage.GuessLetterMessage -> {
                if (_localPlayer.value.isHost) {
                    val res = guessLetterUseCase(_gameState.value, message.letter, message.playerId)
                    val nextState = res.updatedState.copy(turnTimeRemainingSec = 20)
                    _gameState.value = nextState
                    broadcastGameState(nextState)

                    if (nextState.status == GameStatus.PLAYING) {
                        startTurnTimer()
                    } else {
                        turnTimerJob?.cancel()
                    }
                }
            }

            is MessageProtocol.ParsedMessage.RevealHintMessage -> {
                if (_localPlayer.value.isHost) {
                    val updated = _gameState.value.copy(hintRevealed = true)
                    _gameState.value = updated
                    broadcastGameState(updated)
                }
            }

            is MessageProtocol.ParsedMessage.StartGameMessage -> {
                _gameEvents.tryEmit(GameUiEvent.GameStarted(message.secretWord.length))
            }

            is MessageProtocol.ParsedMessage.ResetGameMessage -> {
                _gameEvents.tryEmit(GameUiEvent.NavigateToWordSelection)
            }

            else -> Unit
        }
    }

    private fun broadcastGameState(state: GameState) {
        val bytes = MessageProtocol.serializeGameState(state)
        NearbyManager.sendPayload(bytes)
    }
}
