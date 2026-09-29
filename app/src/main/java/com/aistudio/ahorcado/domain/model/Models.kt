package com.aistudio.ahorcado.domain.model

/**
 * Fase global de la partida. Es lo único que la UI necesita para navegar.
 */
enum class GamePhase {
    /** En sala esperando jugadores / buscando salas. */
    LOBBY,

    /** El creador está eligiendo la palabra. */
    WORD_PICK,

    /** Ronda en curso. */
    PLAYING,

    /** Ronda terminada: se muestra el resultado. */
    ROUND_OVER,
}

/**
 * Cómo participa este dispositivo en la partida.
 */
enum class NetMode {
    /** Varios jugadores en este mismo teléfono (pasan el dispositivo). */
    OFFLINE,

    /** Este dispositivo es el servidor autoritativo (Nearby). */
    HOST,

    /** Este dispositivo es cliente de un host (Nearby). */
    CLIENT,
}

/**
 * Jugador de la partida.
 */
data class Player(
    val id: String,
    val name: String,
    val isHost: Boolean = false,
    val colorIndex: Int = 0,
    val score: Int = 0,
    val connected: Boolean = true,
)

/**
 * Estado público de la partida: lo único que viaja por la red.
 *
 * La palabra secreta NUNCA viaja en claro durante la ronda: solo se incluye
 * en [secretWord] cuando la fase es [GamePhase.ROUND_OVER], para mostrarla
 * en el diálogo final. Los clientes solo conocen [wordLength] y las letras
 * reveladas.
 */
data class PublicState(
    val phase: GamePhase = GamePhase.LOBBY,
    val wordLength: Int = 0,
    val category: String = "",
    val hint: String = "",
    val hintRevealed: Boolean = false,
    val revealed: Set<Char> = emptySet(),
    val guessed: Set<Char> = emptySet(),
    val errors: Int = 0,
    val maxErrors: Int = 6,
    val players: List<Player> = emptyList(),
    val creatorId: String = "",
    val turnPlayerId: String = "",
    /** Momento (SystemClock.elapsedRealtime) en que vence el turno actual. 0 = sin turno. */
    val turnDeadline: Long = 0L,
    val turnSeconds: Int = 20,
    val lastLetter: Char? = null,
    val lastPlayerId: String? = null,
    val lastCorrect: Boolean? = null,
    val winnerId: String? = null,
    /**
     * Patrón enmascarado de la palabra, ej. "C_S_". Lo calcula el host con
     * las letras reveladas: es información pública del juego (posiciones),
     * no filtra la palabra secreta.
     */
    val pattern: String = "",
    /** Solo con contenido en ROUND_OVER. Vacío el resto del tiempo. */
    val secretWord: String = "",
)

/**
 * Sala descubierta por Nearby.
 */
data class FoundHost(
    val endpointId: String,
    val name: String,
)
