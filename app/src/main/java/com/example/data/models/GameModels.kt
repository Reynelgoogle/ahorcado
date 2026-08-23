package com.example.data.models

/**
 * Representa el estado general del juego de Ahorcado.
 */
enum class GameStatus {
    WAITING_PLAYERS,
    SELECTING_WORD,
    PLAYING,
    WON,
    LOST
}

/**
 * Estado individual de una tecla en el teclado virtual.
 */
enum class LetterStatus {
    UNUSED,
    CORRECT,
    INCORRECT
}

/**
 * Modelo que representa un jugador en la partida.
 */
data class Player(
    val id: String,
    val name: String,
    val isHost: Boolean = false,
    val colorIndex: Int = 0,
    val score: Int = 0
)

/**
 * Categoría temática de palabras.
 */
data class WordCategory(
    val id: String,
    val name: String,
    val icon: String,
    val description: String,
    val words: List<CategorizedWord>
)

data class CategorizedWord(
    val word: String,
    val hint: String
)

/**
 * Estado sincronizado global de la partida.
 */
data class GameState(
    val secretWord: String = "",
    val category: String = "General",
    val hint: String = "",
    val hintRevealed: Boolean = false,
    val revealedLetters: Set<Char> = emptySet(),
    val guessedLetters: Set<Char> = emptySet(),
    val errors: Int = 0,
    val maxErrors: Int = 6,
    val status: GameStatus = GameStatus.WAITING_PLAYERS,
    val players: List<Player> = emptyList(),
    val currentTurnPlayerId: String = "",
    val lastGuessedLetter: Char? = null,
    val lastGuessedPlayerId: String? = null,
    val lastGuessedCorrect: Boolean? = null,
    val turnTimeRemainingSec: Int = 20
)

/**
 * Representa un anfitrión descubierto por Nearby Connections.
 */
data class DiscoveredHost(
    val endpointId: String,
    val name: String,
    val isConnecting: Boolean = false
)
