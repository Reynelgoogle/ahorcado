package com.example.domain.usecases

sealed class WordValidationResult {
    object Valid : WordValidationResult()
    data class Invalid(val reason: String) : WordValidationResult()
}

/**
 * Caso de uso para validar la palabra secreta ingresada por el anfitrión.
 */
class ValidateWordUseCase {
    operator fun invoke(word: String): WordValidationResult {
        val trimmed = word.trim()
        if (trimmed.isEmpty()) {
            return WordValidationResult.Invalid("La palabra no puede estar vacía")
        }
        if (trimmed.length < 5) {
            return WordValidationResult.Invalid("La palabra debe tener al menos 5 letras (tiene ${trimmed.length})")
        }
        if (trimmed.length > 15) {
            return WordValidationResult.Invalid("La palabra no debe superar las 15 letras")
        }
        val isAllLetters = trimmed.all { it.isLetter() || it == 'Ñ' || it == 'ñ' }
        if (!isAllLetters) {
            return WordValidationResult.Invalid("La palabra solo debe contener letras del abecedario")
        }
        return WordValidationResult.Valid
    }
}
