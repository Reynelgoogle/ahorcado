package com.example.domain.usecases

import com.example.data.models.GameState
import com.example.data.models.GameStatus

data class GuessResult(
    val isCorrect: Boolean,
    val updatedState: GameState,
    val isGameOver: Boolean,
    val isWon: Boolean
)

/**
 * Caso de uso que procesa un intento de letra, actualiza errores, letras adivinadas y rota turnos.
 */
class GuessLetterUseCase(
    private val checkWinUseCase: CheckWinUseCase = CheckWinUseCase()
) {
    operator fun invoke(currentState: GameState, letter: Char, guessingPlayerId: String): GuessResult {
        val uppercaseLetter = letter.uppercaseChar()

        // Si ya se adivinó esa letra, no hacer nada
        if (uppercaseLetter in currentState.guessedLetters) {
            return GuessResult(
                isCorrect = false,
                updatedState = currentState,
                isGameOver = currentState.status != GameStatus.PLAYING,
                isWon = currentState.status == GameStatus.WON
            )
        }

        val isMatch = currentState.secretWord.uppercase().contains(uppercaseLetter)
        val newGuessed = currentState.guessedLetters + uppercaseLetter
        val newRevealed = if (isMatch) currentState.revealedLetters + uppercaseLetter else currentState.revealedLetters
        val newErrors = if (!isMatch) currentState.errors + 1 else currentState.errors

        val hasWon = checkWinUseCase(currentState.secretWord, newRevealed)
        val hasLost = newErrors >= currentState.maxErrors

        val newStatus = when {
            hasWon -> GameStatus.WON
            hasLost -> GameStatus.LOST
            else -> GameStatus.PLAYING
        }

        // Rotación de turno entre los adivinadores (excluyendo al creador de la palabra)
        val guessers = currentState.players.filter { it.id != currentState.wordCreatorPlayerId }
        val activePlayers = if (guessers.isNotEmpty()) guessers else currentState.players

        val nextPlayerId = if (newStatus == GameStatus.PLAYING && activePlayers.isNotEmpty()) {
            val currentIndex = activePlayers.indexOfFirst { it.id == guessingPlayerId }
            if (currentIndex != -1) {
                val nextIndex = (currentIndex + 1) % activePlayers.size
                activePlayers[nextIndex].id
            } else {
                activePlayers.first().id
            }
        } else {
            currentState.currentTurnPlayerId
        }

        val updatedState = currentState.copy(
            guessedLetters = newGuessed,
            revealedLetters = newRevealed,
            errors = newErrors,
            status = newStatus,
            currentTurnPlayerId = nextPlayerId,
            lastGuessedLetter = uppercaseLetter,
            lastGuessedPlayerId = guessingPlayerId,
            lastGuessedCorrect = isMatch
        )

        return GuessResult(
            isCorrect = isMatch,
            updatedState = updatedState,
            isGameOver = hasWon || hasLost,
            isWon = hasWon
        )
    }
}
