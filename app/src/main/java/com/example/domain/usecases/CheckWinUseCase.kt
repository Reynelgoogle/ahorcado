package com.example.domain.usecases

/**
 * Caso de uso para verificar si se han revelado todas las letras de la palabra secreta.
 */
class CheckWinUseCase {
    operator fun invoke(secretWord: String, revealedLetters: Set<Char>): Boolean {
        if (secretWord.isEmpty()) return false
        return secretWord.uppercase().all { it in revealedLetters }
    }
}
