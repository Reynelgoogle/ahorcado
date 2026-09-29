package com.aistudio.ahorcado.domain.logic

import java.text.Normalizer

/**
 * Lógica pura del ahorcado. Sin dependencias de Android: se puede probar
 * con JUnit en la JVM.
 */
object HangmanEngine {

    /** Teclado del juego. */
    const val ALPHABET = "ABCDEFGHIJKLMNÑOPQRSTUVWXYZ"

    const val MAX_ERRORS = 6
    const val TURN_SECONDS = 20
    const val MAX_PLAYERS = 4
    const val MIN_WORD_LEN = 4
    const val MAX_WORD_LEN = 15

    /** Puntos por cada aparición de una letra acertada. */
    const val POINTS_PER_LETTER = 10

    /** Bono por adivinar la letra que completa la palabra. */
    const val WIN_BONUS = 50

    /**
     * Normaliza una palabra para el juego: mayúsculas y sin tildes,
     * pero conservando la Ñ (que NFD descompondría en N + virgulilla).
     */
    fun normalizeWord(raw: String): String {
        val guarded = raw.replace('Ñ', '\u0001').replace('ñ', '\u0001')
        val decomposed = Normalizer.normalize(guarded, Normalizer.Form.NFD)
        val stripped = decomposed.filter {
            Character.getType(it) != Character.NON_SPACING_MARK.toInt()
        }
        return stripped.replace('\u0001', 'Ñ').uppercase()
    }

    sealed interface WordValidation {
        data class Ok(val normalized: String) : WordValidation
        data class Err(val reason: String) : WordValidation
    }

    fun validateWord(raw: String): WordValidation {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return WordValidation.Err("Escribe una palabra")
        val normalized = normalizeWord(trimmed)
        if (normalized.length < MIN_WORD_LEN) {
            return WordValidation.Err("Mínimo $MIN_WORD_LEN letras")
        }
        if (normalized.length > MAX_WORD_LEN) {
            return WordValidation.Err("Máximo $MAX_WORD_LEN letras")
        }
        if (!normalized.all { it in ALPHABET }) {
            return WordValidation.Err("Solo letras (A-Z y Ñ)")
        }
        return WordValidation.Ok(normalized)
    }

    /**
     * Progreso de la ronda en el lado autoritativo (host u offline).
     */
    data class RoundProgress(
        val secret: String,
        val revealed: Set<Char> = emptySet(),
        val guessed: Set<Char> = emptySet(),
        val errors: Int = 0,
    )

    data class GuessResult(
        val correct: Boolean,
        val occurrences: Int,
        val alreadyGuessed: Boolean,
        val progress: RoundProgress,
        val won: Boolean,
        val lost: Boolean,
    )

    /**
     * Aplica el intento de una letra. No valida turnos ni identidad:
     * eso lo hace el repositorio antes de llamar aquí.
     */
    fun applyGuess(
        progress: RoundProgress,
        letter: Char,
        maxErrors: Int = MAX_ERRORS,
    ): GuessResult {
        val l = letter.uppercaseChar()
        if (l in progress.guessed) {
            return GuessResult(
                correct = false,
                occurrences = 0,
                alreadyGuessed = true,
                progress = progress,
                won = false,
                lost = false,
            )
        }
        val occurrences = progress.secret.count { it == l }
        val correct = occurrences > 0
        val next = progress.copy(
            guessed = progress.guessed + l,
            revealed = if (correct) progress.revealed + l else progress.revealed,
            errors = if (correct) progress.errors else progress.errors + 1,
        )
        val won = correct && next.secret.all { it in next.revealed }
        val lost = !won && next.errors >= maxErrors
        return GuessResult(
            correct = correct,
            occurrences = occurrences,
            alreadyGuessed = false,
            progress = next,
            won = won,
            lost = lost,
        )
    }

    /**
     * Siguiente turno entre los adivinadores (todos menos el creador).
     * Si no hay adivinadores, rota entre todos.
     */
    fun nextTurnId(
        orderedIds: List<String>,
        creatorId: String,
        currentId: String,
    ): String? {
        val guessers = orderedIds.filter { it != creatorId }
        val pool = if (guessers.isNotEmpty()) guessers else orderedIds
        if (pool.isEmpty()) return null
        val idx = pool.indexOf(currentId)
        return if (idx == -1) pool.first() else pool[(idx + 1) % pool.size]
    }

    /**
     * Letras del teclado con su estado, derivadas del estado público.
     */
    enum class KeyState { UNUSED, CORRECT, WRONG }

    fun keyStates(guessed: Set<Char>, revealed: Set<Char>): Map<Char, KeyState> =
        ALPHABET.associateWith { c ->
            when {
                c !in guessed -> KeyState.UNUSED
                c in revealed -> KeyState.CORRECT
                else -> KeyState.WRONG
            }
        }
}
