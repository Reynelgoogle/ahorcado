package com.example

import com.example.data.models.GameState
import com.example.data.models.GameStatus
import com.example.data.models.Player
import com.example.domain.usecases.GuessLetterUseCase
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testGuessLetterUseCase_correctGuess() {
        val useCase = GuessLetterUseCase()
        val p1 = Player("p1", "Alice", isHost = true)
        val p2 = Player("p2", "Bob", isHost = false)
        val p3 = Player("p3", "Charlie", isHost = false)

        val initialState = GameState(
            status = GameStatus.PLAYING,
            secretWord = "ANDROID",
            revealedLetters = emptySet(),
            guessedLetters = emptySet(),
            errors = 0,
            maxErrors = 6,
            currentTurnPlayerId = "p2",
            players = listOf(p1, p2, p3),
            wordCreatorPlayerId = "p1"
        )

        // Bob guesses 'A' (correct) -> turn should rotate to Charlie (p3), excluding Alice (creator p1)
        val result = useCase(initialState, 'A', "p2")
        val newState = result.updatedState
        assertTrue(newState.revealedLetters.contains('A'))
        assertTrue(newState.guessedLetters.contains('A'))
        assertEquals(0, newState.errors)
        assertEquals("p3", newState.currentTurnPlayerId)
    }

    @Test
    fun testGuessLetterUseCase_wrongGuessRotatesTurnExcludingCreator() {
        val useCase = GuessLetterUseCase()
        val p1 = Player("p1", "HostCreator", isHost = true)
        val p2 = Player("p2", "Guesser1", isHost = false)
        val p3 = Player("p3", "Guesser2", isHost = false)

        val initialState = GameState(
            status = GameStatus.PLAYING,
            secretWord = "KOTLIN",
            revealedLetters = emptySet(),
            guessedLetters = emptySet(),
            errors = 0,
            maxErrors = 6,
            currentTurnPlayerId = "p3",
            players = listOf(p1, p2, p3),
            wordCreatorPlayerId = "p1"
        )

        // Guesser2 guesses 'Z' (wrong) -> turn should wrap back to Guesser1 (p2), skipping Creator p1
        val result = useCase(initialState, 'Z', "p3")
        val newState = result.updatedState
        assertEquals(1, newState.errors)
        assertEquals("p2", newState.currentTurnPlayerId)
    }

    @Test
    fun testGuessLetterUseCase_winCondition() {
        val useCase = GuessLetterUseCase()
        val p1 = Player("p1", "Alice", isHost = true)
        val p2 = Player("p2", "Bob", isHost = false)

        val initialState = GameState(
            status = GameStatus.PLAYING,
            secretWord = "HI",
            revealedLetters = setOf('H'),
            guessedLetters = setOf('H'),
            errors = 0,
            currentTurnPlayerId = "p2",
            players = listOf(p1, p2),
            wordCreatorPlayerId = "p1"
        )

        val result = useCase(initialState, 'I', "p2")
        val finalState = result.updatedState
        assertEquals(GameStatus.WON, finalState.status)
        assertTrue(finalState.revealedLetters.contains('I'))
    }
}


