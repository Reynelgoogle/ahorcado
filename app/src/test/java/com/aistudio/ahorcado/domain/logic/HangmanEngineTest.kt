package com.aistudio.ahorcado.domain.logic

import org.junit.Assert.*
import org.junit.Test

class HangmanEngineTest {

    @Test
    fun normalize_removesAccentsButKeepsEnye() {
        assertEquals("CAMION", HangmanEngine.normalizeWord("camión"))
        assertEquals("NIÑO", HangmanEngine.normalizeWord("niño"))
        assertEquals("MURCIELAGO", HangmanEngine.normalizeWord("murciélago"))
        assertEquals("ARBOL", HangmanEngine.normalizeWord("ÁRBOL"))
    }

    @Test
    fun validateWord_acceptsValidWords() {
        val r = HangmanEngine.validateWord("Mariposa")
        assertTrue(r is HangmanEngine.WordValidation.Ok)
        assertEquals("MARIPOSA", (r as HangmanEngine.WordValidation.Ok).normalized)
    }

    @Test
    fun validateWord_rejectsBadWords() {
        assertTrue(HangmanEngine.validateWord("") is HangmanEngine.WordValidation.Err)
        assertTrue(HangmanEngine.validateWord("sol") is HangmanEngine.WordValidation.Err)
        assertTrue(HangmanEngine.validateWord("a".repeat(16)) is HangmanEngine.WordValidation.Err)
        assertTrue(HangmanEngine.validateWord("casa123") is HangmanEngine.WordValidation.Err)
        assertTrue(HangmanEngine.validateWord("dos palabras") is HangmanEngine.WordValidation.Err)
    }

    @Test
    fun applyGuess_correctLetterRevealsOccurrences() {
        val p = HangmanEngine.RoundProgress(secret = "CASA")
        val r = HangmanEngine.applyGuess(p, 'a')
        assertTrue(r.correct)
        assertEquals(2, r.occurrences)
        assertEquals(setOf('A'), r.progress.revealed)
        assertEquals(setOf('A'), r.progress.guessed)
        assertEquals(0, r.progress.errors)
        assertFalse(r.won)
        assertFalse(r.lost)
    }

    @Test
    fun applyGuess_wrongLetterAddsError() {
        val p = HangmanEngine.RoundProgress(secret = "CASA")
        val r = HangmanEngine.applyGuess(p, 'z')
        assertFalse(r.correct)
        assertEquals(1, r.progress.errors)
        assertTrue(r.progress.revealed.isEmpty())
    }

    @Test
    fun applyGuess_repeatedLetterIsIgnored() {
        val p = HangmanEngine.RoundProgress(secret = "CASA", guessed = setOf('A'))
        val r = HangmanEngine.applyGuess(p, 'a')
        assertTrue(r.alreadyGuessed)
        assertEquals(0, r.progress.errors)
    }

    @Test
    fun applyGuess_winWhenAllRevealed() {
        val p = HangmanEngine.RoundProgress(secret = "SOL", revealed = setOf('S', 'O'))
        val r = HangmanEngine.applyGuess(p, 'l')
        assertTrue(r.won)
        assertFalse(r.lost)
    }

    @Test
    fun applyGuess_loseAtMaxErrors() {
        val p = HangmanEngine.RoundProgress(secret = "SOL", errors = 5)
        val r = HangmanEngine.applyGuess(p, 'z', maxErrors = 6)
        assertTrue(r.lost)
        assertFalse(r.won)
    }

    @Test
    fun nextTurnId_rotatesSkippingCreator() {
        val ids = listOf("host", "p1", "p2")
        assertEquals("p2", HangmanEngine.nextTurnId(ids, "host", "p1"))
        assertEquals("p1", HangmanEngine.nextTurnId(ids, "host", "p2"))
        // El creador nunca recibe el turno
        assertEquals("p1", HangmanEngine.nextTurnId(ids, "host", "host"))
    }

    @Test
    fun nextTurnId_singleGuesserKeepsTurn() {
        val ids = listOf("host", "p1")
        assertEquals("p1", HangmanEngine.nextTurnId(ids, "host", "p1"))
    }

    @Test
    fun nextTurnId_noPlayersReturnsNull() {
        assertNull(HangmanEngine.nextTurnId(emptyList(), "host", "x"))
    }
}
