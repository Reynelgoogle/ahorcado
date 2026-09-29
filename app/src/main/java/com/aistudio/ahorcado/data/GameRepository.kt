package com.aistudio.ahorcado.data

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.aistudio.ahorcado.domain.logic.HangmanEngine
import com.aistudio.ahorcado.domain.logic.HangmanEngine.MAX_PLAYERS
import com.aistudio.ahorcado.domain.logic.HangmanEngine.POINTS_PER_LETTER
import com.aistudio.ahorcado.domain.logic.HangmanEngine.TURN_SECONDS
import com.aistudio.ahorcado.domain.logic.HangmanEngine.WIN_BONUS
import com.aistudio.ahorcado.domain.model.FoundHost
import com.aistudio.ahorcado.domain.model.GamePhase
import com.aistudio.ahorcado.domain.model.NetMode
import com.aistudio.ahorcado.domain.model.Player
import com.aistudio.ahorcado.domain.model.PublicState
import com.aistudio.ahorcado.net.MessageProtocol
import com.aistudio.ahorcado.net.NearbyLink
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Fuente única de verdad de la partida.
 *
 * Modelo autoritativo:
 * - En modo HOST y OFFLINE este dispositivo decide todo: valida cada letra
 *   (turno, identidad y fase), lleva el temporizador por deadline y reparte puntos.
 * - En modo CLIENT solo se aplica el estado que envía el host y se le piden
 *   acciones (adivinar, pista).
 * - La palabra secreta vive solo en [secret] (memoria del host) y jamás se
 *   incluye en el estado público durante la ronda.
 */
object GameRepository {

    private const val TAG = "GameRepository"

    sealed interface UiEvent {
        data class Toast(val message: String) : UiEvent
        data object GoToMenu : UiEvent
        data object GoToLobby : UiEvent
        data object GoToWord : UiEvent
        data object GoToGame : UiEvent
        data class PlayerJoined(val name: String) : UiEvent
        data class PlayerLeft(val name: String) : UiEvent
        data object KickedOut : UiEvent
        data object RoomFullNotice : UiEvent
        data object HostGone : UiEvent
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _mode = MutableStateFlow(NetMode.OFFLINE)
    val modeFlow: StateFlow<NetMode> = _mode.asStateFlow()
    var mode: NetMode
        get() = _mode.value
        private set(value) { _mode.value = value }

    private val _localPlayer = MutableStateFlow(Player(id = newId(), name = "Jugador"))
    val localPlayer: StateFlow<Player> = _localPlayer.asStateFlow()

    private val _public = MutableStateFlow(PublicState())
    val publicState: StateFlow<PublicState> = _public.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private val _foundHosts = MutableStateFlow<List<FoundHost>>(emptyList())
    val foundHosts: StateFlow<List<FoundHost>> = _foundHosts.asStateFlow()

    /** Palabra secreta: solo existe en el host / modo offline. Nunca viaja en claro. */
    private var secret: String = ""

    /** Solo host: extremo de red -> id de jugador (verificación de identidad). */
    private val endpointToPid = mutableMapOf<String, String>()

    /** Solo cliente: extremo del host. */
    private var hostEndpointId: String? = null

    /** Si true, el próximo PeerLost no genera aviso (expulsión / sala llena / salida). */
    private var suppressPeerLost = false

    private var deadlineJob: Job? = null

    private fun newId(): String = UUID.randomUUID().toString()
    private fun authoritative(): Boolean = mode == NetMode.HOST || mode == NetMode.OFFLINE

    /** Patrón enmascarado "C_S_": información pública, no filtra la palabra. */
    private fun patternFor(secretWord: String, revealed: Set<Char>): String =
        secretWord.map { if (it in revealed) it else '_' }.joinToString("")

    // ==================== creación / unión ====================

    fun hostRoom(context: Context, name: String) {
        resetAll()
        mode = NetMode.HOST
        val me = Player(
            id = newId(),
            name = name.ifBlank { "Anfitrión" }.take(24),
            isHost = true,
            colorIndex = 0,
        )
        _localPlayer.value = me
        _public.value = PublicState(
            phase = GamePhase.LOBBY,
            players = listOf(me),
            creatorId = me.id,
            turnPlayerId = me.id,
        )
        NearbyLink.listener = { ev -> scope.launch { onLinkEvent(ev) } }
        scope.launch(Dispatchers.IO) {
            val ok = NearbyLink.startHosting(context, me.name)
            if (!ok) _events.tryEmit(UiEvent.Toast("No se pudo crear la sala: revisa Play Services"))
        }
        _events.tryEmit(UiEvent.GoToLobby)
    }

    fun discoverRooms(context: Context, name: String) {
        resetAll()
        mode = NetMode.CLIENT
        val me = Player(id = newId(), name = name.ifBlank { "Jugador" }.take(24))
        _localPlayer.value = me
        _public.value = PublicState(phase = GamePhase.LOBBY, players = listOf(me))
        NearbyLink.listener = { ev -> scope.launch { onLinkEvent(ev) } }
        scope.launch(Dispatchers.IO) {
            val ok = NearbyLink.startDiscovering(context, me.name)
            if (!ok) _events.tryEmit(UiEvent.Toast("No se pudo buscar salas: revisa Play Services"))
        }
        _events.tryEmit(UiEvent.GoToLobby)
    }

    fun joinRoom(endpointId: String) {
        if (mode != NetMode.CLIENT) return
        NearbyLink.stopDiscovery()
        NearbyLink.connectTo(endpointId)
    }

    /** Juego local en este teléfono: el primer nombre crea la palabra. */
    fun startOfflineRoom(names: List<String>) {
        resetAll()
        mode = NetMode.OFFLINE
        val clean = names.map { it.ifBlank { "Jugador" }.take(24) }.take(MAX_PLAYERS)
        require(clean.size >= 2) { "Se necesitan al menos 2 jugadores" }
        val players = clean.mapIndexed { i, n ->
            Player(id = newId(), name = n, isHost = i == 0, colorIndex = i % MAX_PLAYERS)
        }
        _localPlayer.value = players.first()
        _public.value = PublicState(
            phase = GamePhase.WORD_PICK,
            players = players,
            creatorId = players.first().id,
            turnPlayerId = players.first().id,
        )
        _events.tryEmit(UiEvent.GoToWord)
    }

    // ==================== flujo de ronda ====================

    /**
     * El creador fija la palabra y arranca la ronda. Solo en modo autoritativo.
     */
    fun pickWord(raw: String, category: String, hint: String): HangmanEngine.WordValidation {
        val validation = HangmanEngine.validateWord(raw)
        if (validation !is HangmanEngine.WordValidation.Ok) return validation
        if (!authoritative()) {
            return HangmanEngine.WordValidation.Err("Solo el anfitrión elige la palabra")
        }
        val s = _public.value
        if (s.phase != GamePhase.WORD_PICK && s.phase != GamePhase.LOBBY) {
            return HangmanEngine.WordValidation.Err("La ronda ya empezó")
        }

        secret = validation.normalized
        val ids = s.players.map { it.id }
        val firstTurn = HangmanEngine.nextTurnId(ids, s.creatorId, s.creatorId) ?: s.creatorId
        val now = SystemClock.elapsedRealtime()
        val next = s.copy(
            phase = GamePhase.PLAYING,
            wordLength = secret.length,
            pattern = patternFor(secret, emptySet()),
            category = category.ifBlank { "General" }.take(24),
            hint = hint.trim().take(120),
            hintRevealed = false,
            revealed = emptySet(),
            guessed = emptySet(),
            errors = 0,
            turnPlayerId = firstTurn,
            turnDeadline = now + TURN_SECONDS * 1000L,
            turnSeconds = TURN_SECONDS,
            lastLetter = null,
            lastPlayerId = null,
            lastCorrect = null,
            winnerId = null,
            secretWord = "",
        )
        setAndBroadcast(next)
        if (mode == NetMode.HOST) NearbyLink.stopAdvertising()
        armDeadline()
        _events.tryEmit(UiEvent.GoToGame)
        return validation
    }

    /** Letra jugada por el jugador local (o por el jugador en turno en modo offline). */
    fun guess(letter: Char) = guessAs(letter, _localPlayer.value.id)

    fun guessAs(letter: Char, playerId: String) {
        val l = letter.uppercaseChar()
        if (l !in HangmanEngine.ALPHABET) return
        when (mode) {
            NetMode.HOST, NetMode.OFFLINE -> applyAuthoritativeGuess(l, playerId)
            NetMode.CLIENT -> {
                val host = hostEndpointId ?: return
                NearbyLink.send(MessageProtocol.guess(playerId, l), listOf(host))
            }
        }
    }

    fun requestHint() = requestHintAs(_localPlayer.value.id)

    fun requestHintAs(playerId: String) {
        when (mode) {
            NetMode.HOST, NetMode.OFFLINE -> {
                val s = _public.value
                if (s.phase == GamePhase.PLAYING && !s.hintRevealed && s.hint.isNotBlank()) {
                    setAndBroadcast(s.copy(hintRevealed = true))
                }
            }
            NetMode.CLIENT -> {
                val host = hostEndpointId ?: return
                NearbyLink.send(MessageProtocol.hint(playerId), listOf(host))
            }
        }
    }

    /** Solo host: expulsa a un jugador. */
    fun kick(playerId: String) {
        if (mode != NetMode.HOST) return
        val endpoint = endpointToPid.entries.firstOrNull { it.value == playerId }?.key
        if (endpoint != null) {
            NearbyLink.send(MessageProtocol.kicked(), listOf(endpoint))
            scope.launch {
                delay(400)
                NearbyLink.disconnectPeer(endpoint)
            }
        }
        removePlayer(playerId, announce = true)
    }

    /** Solo autoritativo: vuelve a la elección de palabra para otra ronda. */
    fun rematch() {
        if (!authoritative()) return
        deadlineJob?.cancel()
        secret = ""
        val s = _public.value
        val next = s.copy(
            phase = GamePhase.WORD_PICK,
            wordLength = 0,
            pattern = "",
            hint = "",
            hintRevealed = false,
            revealed = emptySet(),
            guessed = emptySet(),
            errors = 0,
            turnPlayerId = s.creatorId,
            turnDeadline = 0L,
            lastLetter = null,
            lastPlayerId = null,
            lastCorrect = null,
            winnerId = null,
            secretWord = "",
        )
        setAndBroadcast(next)
        if (mode == NetMode.HOST) {
            // Reanuncia para aceptar jugadores nuevos entre rondas.
            scope.launch(Dispatchers.IO) {
                NearbyLink.startHosting(
                    appContext ?: return@launch,
                    _localPlayer.value.name,
                )
            }
        }
        _events.tryEmit(UiEvent.GoToWord)
    }

    fun leaveToMenu() {
        val pid = _localPlayer.value.id
        if (mode == NetMode.CLIENT) {
            hostEndpointId?.let { NearbyLink.send(MessageProtocol.leave(pid), listOf(it)) }
        }
        resetAll()
        _events.tryEmit(UiEvent.GoToMenu)
    }

    // ==================== red (host) ====================

    private fun onLinkEvent(ev: NearbyLink.LinkEvent) {
        when (ev) {
            is NearbyLink.LinkEvent.PeerConnected -> onPeerConnected(ev.endpointId)
            is NearbyLink.LinkEvent.PeerLost -> onPeerLost(ev.endpointId)
            is NearbyLink.LinkEvent.BytesReceived -> onBytes(ev.endpointId, ev.bytes)
            is NearbyLink.LinkEvent.EndpointFound ->
                if (mode == NetMode.CLIENT) {
                    val cur = _foundHosts.value
                    if (cur.none { it.endpointId == ev.endpointId }) {
                        _foundHosts.value = cur + FoundHost(ev.endpointId, ev.name)
                    }
                }
            is NearbyLink.LinkEvent.EndpointLost ->
                if (mode == NetMode.CLIENT) {
                    _foundHosts.value = _foundHosts.value.filter { it.endpointId != ev.endpointId }
                }
        }
    }

    private fun onPeerConnected(endpointId: String) {
        if (mode == NetMode.HOST) {
            // El cliente se identifica con HELLO; el host le responde con el estado.
            Log.d(TAG, "Extremo conectado: $endpointId (esperando HELLO)")
        } else if (mode == NetMode.CLIENT) {
            if (hostEndpointId == null) {
                hostEndpointId = endpointId
                NearbyLink.send(
                    MessageProtocol.hello(_localPlayer.value.id, _localPlayer.value.name),
                    listOf(endpointId),
                )
            }
        }
    }

    private fun onPeerLost(endpointId: String) {
        if (mode == NetMode.HOST) {
            val pid = endpointToPid.remove(endpointId) ?: return
            val s = _public.value
            if (s.phase == GamePhase.LOBBY || s.phase == GamePhase.WORD_PICK) {
                // En sala: se elimina de la lista.
                removePlayer(pid, announce = true)
            } else {
                // En partida: se marca desconectado y se reasigna el turno si era el suyo.
                val players = s.players.map {
                    if (it.id == pid) it.copy(connected = false) else it
                }
                var next = s.copy(players = players)
                if (s.turnPlayerId == pid) {
                    val ids = players.filter { it.connected }.map { it.id }
                    next = next.copy(
                        turnPlayerId = HangmanEngine.nextTurnId(ids, s.creatorId, pid)
                            ?: ids.firstOrNull().orEmpty(),
                        turnDeadline = SystemClock.elapsedRealtime() + TURN_SECONDS * 1000L,
                    )
                }
                setAndBroadcast(next)
                armDeadline()
                s.players.find { it.id == pid }?.let {
                    _events.tryEmit(UiEvent.PlayerLeft(it.name))
                }
            }
        } else if (mode == NetMode.CLIENT) {
            if (suppressPeerLost) {
                suppressPeerLost = false
                return
            }
            if (endpointId == hostEndpointId) {
                hostEndpointId = null
                _events.tryEmit(UiEvent.HostGone)
                resetAll()
                _events.tryEmit(UiEvent.GoToMenu)
            }
        }
    }

    private fun onBytes(endpointId: String, bytes: ByteArray) {
        when (val msg = MessageProtocol.parse(bytes)) {
            is MessageProtocol.NetMessage.Hello -> if (mode == NetMode.HOST) onHello(endpointId, msg)
            is MessageProtocol.NetMessage.Guess -> if (mode == NetMode.HOST) onRemoteGuess(endpointId, msg)
            is MessageProtocol.NetMessage.Hint -> if (mode == NetMode.HOST) onRemoteHint(endpointId, msg)
            is MessageProtocol.NetMessage.Leave -> if (mode == NetMode.HOST) {
                val pid = endpointToPid.remove(endpointId)
                if (pid != null) removePlayer(pid, announce = true)
            }
            is MessageProtocol.NetMessage.State -> if (mode == NetMode.CLIENT) onState(msg)
            is MessageProtocol.NetMessage.Kicked -> if (mode == NetMode.CLIENT) {
                suppressPeerLost = true
                _events.tryEmit(UiEvent.KickedOut)
                resetAll()
                _events.tryEmit(UiEvent.GoToMenu)
            }
            is MessageProtocol.NetMessage.RoomFull -> if (mode == NetMode.CLIENT) {
                suppressPeerLost = true
                _events.tryEmit(UiEvent.RoomFullNotice)
            }
            is MessageProtocol.NetMessage.Unknown -> Unit
        }
    }

    private fun onHello(endpointId: String, msg: MessageProtocol.NetMessage.Hello) {
        val s = _public.value
        val known = s.players.find { it.id == msg.playerId }
        if (known != null) {
            // Reconexión con el mismo id: se reasocia el extremo, sin duplicar.
            endpointToPid[endpointId] = msg.playerId
            val players = s.players.map {
                if (it.id == msg.playerId) it.copy(connected = true, name = msg.name.take(24)) else it
            }
            setAndBroadcast(s.copy(players = players))
            _events.tryEmit(UiEvent.PlayerJoined(msg.name.take(24)))
            return
        }
        if (s.phase != GamePhase.LOBBY || s.players.size >= MAX_PLAYERS) {
            NearbyLink.send(MessageProtocol.roomFull(), listOf(endpointId))
            scope.launch {
                delay(400)
                NearbyLink.disconnectPeer(endpointId)
            }
            return
        }
        val player = Player(
            id = msg.playerId,
            name = msg.name.take(24).ifBlank { "Jugador" },
            colorIndex = s.players.size % MAX_PLAYERS,
        )
        endpointToPid[endpointId] = player.id
        setAndBroadcast(s.copy(players = s.players + player))
        _events.tryEmit(UiEvent.PlayerJoined(player.name))
    }

    private fun onRemoteGuess(endpointId: String, msg: MessageProtocol.NetMessage.Guess) {
        // Verificación de identidad: el extremo debe corresponder al jugador que dice ser.
        val mapped = endpointToPid[endpointId]
        if (mapped == null || mapped != msg.playerId) {
            Log.w(TAG, "GUESS con identidad no verificada: $endpointId")
            return
        }
        applyAuthoritativeGuess(msg.letter, msg.playerId)
    }

    private fun onRemoteHint(endpointId: String, msg: MessageProtocol.NetMessage.Hint) {
        val mapped = endpointToPid[endpointId]
        if (mapped == null || mapped != msg.playerId) return
        val s = _public.value
        if (s.phase == GamePhase.PLAYING && !s.hintRevealed && s.hint.isNotBlank()) {
            setAndBroadcast(s.copy(hintRevealed = true))
        }
    }

    private fun onState(msg: MessageProtocol.NetMessage.State) {
        val prev = _public.value.phase
        val next = msg.state
        _public.value = next
        _localPlayer.value = _localPlayer.value.copy(
            score = next.players.find { it.id == _localPlayer.value.id }?.score ?: 0,
        )
        if (next.phase == GamePhase.PLAYING && prev != GamePhase.PLAYING) {
            _events.tryEmit(UiEvent.GoToGame)
        }
    }

    // ==================== lógica autoritativa ====================

    /**
     * Aplica una letra con todas las validaciones: fase, turno, identidad del
     * turno (el creador no adivina), jugador conectado y letra válida.
     */
    private fun applyAuthoritativeGuess(letter: Char, playerId: String) {
        val s = _public.value
        if (s.phase != GamePhase.PLAYING) return
        if (playerId != s.turnPlayerId) return
        if (playerId == s.creatorId) return
        val player = s.players.find { it.id == playerId }
        if (player == null || !player.connected) return

        val res = HangmanEngine.applyGuess(
            HangmanEngine.RoundProgress(secret, s.revealed, s.guessed, s.errors),
            letter,
        )
        if (res.alreadyGuessed) return

        var players = s.players
        if (res.correct) {
            val pts = POINTS_PER_LETTER * res.occurrences
            players = players.map {
                if (it.id == playerId) it.copy(score = it.score + pts) else it
            }
        }

        val now = SystemClock.elapsedRealtime()
        val next = when {
            res.won -> {
                players = players.map {
                    if (it.id == playerId) it.copy(score = it.score + WIN_BONUS) else it
                }
                s.copy(
                    revealed = res.progress.revealed,
                    guessed = res.progress.guessed,
                    errors = res.progress.errors,
                    players = players,
                    phase = GamePhase.ROUND_OVER,
                    pattern = secret,
                    secretWord = secret,
                    turnDeadline = 0L,
                    lastLetter = letter,
                    lastPlayerId = playerId,
                    lastCorrect = true,
                    winnerId = playerId,
                )
            }
            res.lost -> s.copy(
                revealed = res.progress.revealed,
                guessed = res.progress.guessed,
                errors = res.progress.errors,
                players = players,
                phase = GamePhase.ROUND_OVER,
                pattern = secret,
                secretWord = secret,
                turnDeadline = 0L,
                lastLetter = letter,
                lastPlayerId = playerId,
                lastCorrect = false,
            )
            else -> {
                val ids = players.filter { it.connected }.map { it.id }
                val nextId = HangmanEngine.nextTurnId(ids, s.creatorId, playerId)
                    ?: playerId
                s.copy(
                    revealed = res.progress.revealed,
                    guessed = res.progress.guessed,
                    errors = res.progress.errors,
                    players = players,
                    pattern = patternFor(secret, res.progress.revealed),
                    turnPlayerId = nextId,
                    turnDeadline = now + TURN_SECONDS * 1000L,
                    lastLetter = letter,
                    lastPlayerId = playerId,
                    lastCorrect = res.correct,
                )
            }
        }
        setAndBroadcast(next)
        armDeadline()
    }

    /** Vence el turno actual: rota al siguiente o suma error si hay un solo adivinador. */
    private fun onDeadline() {
        val s = _public.value
        if (!authoritative() || s.phase != GamePhase.PLAYING) return
        val ids = s.players.filter { it.connected }.map { it.id }
        val guessers = ids.filter { it != s.creatorId }
        val now = SystemClock.elapsedRealtime()
        val next = if (guessers.size <= 1) {
            val errors = s.errors + 1
            if (errors >= s.maxErrors) {
                s.copy(
                    errors = errors,
                    phase = GamePhase.ROUND_OVER,
                    pattern = secret,
                    secretWord = secret,
                    turnDeadline = 0L,
                    lastLetter = null,
                    lastPlayerId = null,
                    lastCorrect = false,
                )
            } else {
                s.copy(
                    errors = errors,
                    turnDeadline = now + TURN_SECONDS * 1000L,
                    lastLetter = null,
                    lastPlayerId = null,
                    lastCorrect = false,
                )
            }
        } else {
            val nextId = HangmanEngine.nextTurnId(ids, s.creatorId, s.turnPlayerId)
                ?: s.turnPlayerId
            s.copy(
                turnPlayerId = nextId,
                turnDeadline = now + TURN_SECONDS * 1000L,
                lastLetter = null,
                lastPlayerId = null,
                lastCorrect = null,
            )
        }
        setAndBroadcast(next)
        armDeadline()
    }

    /**
     * Arma el temporizador del turno con deadline absoluto: si el host
     * minimiza la app y vuelve, el deadline ya vencido se procesa de inmediato.
     */
    private fun armDeadline() {
        deadlineJob?.cancel()
        if (!authoritative()) return
        val deadline = _public.value.turnDeadline
        if (_public.value.phase != GamePhase.PLAYING || deadline <= 0L) return
        val expected = deadline
        deadlineJob = scope.launch {
            val wait = expected - SystemClock.elapsedRealtime()
            if (wait > 0) delay(wait)
            // Solo expira si el deadline no cambió (una jugada lo mueve).
            if (_public.value.turnDeadline == expected) onDeadline()
        }
    }

    private fun removePlayer(playerId: String, announce: Boolean) {
        val s = _public.value
        val player = s.players.find { it.id == playerId } ?: return
        val players = s.players.filter { it.id != playerId }
        var next = s.copy(players = players)
        if (s.turnPlayerId == playerId && s.phase == GamePhase.PLAYING) {
            val ids = players.filter { it.connected }.map { it.id }
            next = next.copy(
                turnPlayerId = HangmanEngine.nextTurnId(ids, s.creatorId, playerId)
                    ?: ids.firstOrNull().orEmpty(),
                turnDeadline = SystemClock.elapsedRealtime() + TURN_SECONDS * 1000L,
            )
        }
        setAndBroadcast(next)
        armDeadline()
        if (announce) _events.tryEmit(UiEvent.PlayerLeft(player.name))
    }

    private fun setAndBroadcast(s: PublicState) {
        _public.value = s
        if (mode == NetMode.HOST) {
            NearbyLink.send(MessageProtocol.state(s), NearbyLink.connectedPeers)
        }
    }

    // ==================== utilidades ====================

    private var appContext: Context? = null

    fun attachAppContext(context: Context) {
        appContext = context.applicationContext
    }

    private fun resetAll() {
        deadlineJob?.cancel()
        deadlineJob = null
        NearbyLink.stopAll()
        endpointToPid.clear()
        hostEndpointId = null
        suppressPeerLost = false
        secret = ""
        _foundHosts.value = emptyList()
        _public.value = PublicState()
    }
}
