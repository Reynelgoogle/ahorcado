package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.models.GameState
import com.example.data.models.GameStatus
import com.example.data.models.Player
import com.example.domain.usecases.CheckWinUseCase
import com.example.domain.usecases.GuessLetterUseCase
import com.example.domain.usecases.ValidateWordUseCase
import com.example.domain.usecases.WordValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Ahorcado P2P", appName)
  }

  @Test
  fun `validate word use case checks length and characters`() {
    val validateWord = ValidateWordUseCase()

    assertTrue(validateWord("ASTRONAUTA") is WordValidationResult.Valid)
    assertTrue(validateWord("MONTAÑA") is WordValidationResult.Valid)

    // Muy corta
    val shortRes = validateWord("HOLA")
    assertTrue(shortRes is WordValidationResult.Invalid)

    // Con números o símbolos
    val invalidCharRes = validateWord("HOLA123")
    assertTrue(invalidCharRes is WordValidationResult.Invalid)
  }

  @Test
  fun `guess letter use case updates errors and reveals correct letters`() {
    val guessUseCase = GuessLetterUseCase()
    val player1 = Player("1", "Ana", isHost = true)
    val player2 = Player("2", "Bob", isHost = false)
    val initialState = GameState(
      secretWord = "PYTHON",
      status = GameStatus.PLAYING,
      players = listOf(player1, player2),
      currentTurnPlayerId = player1.id
    )

    // Intento correcto de 'P'
    val result1 = guessUseCase(initialState, 'P', player1.id)
    assertTrue(result1.isCorrect)
    assertTrue('P' in result1.updatedState.revealedLetters)
    assertEquals(0, result1.updatedState.errors)
    assertEquals(player2.id, result1.updatedState.currentTurnPlayerId)

    // Intento incorrecto de 'Z'
    val result2 = guessUseCase(result1.updatedState, 'Z', player2.id)
    assertFalse(result2.isCorrect)
    assertEquals(1, result2.updatedState.errors)
    assertEquals(player1.id, result2.updatedState.currentTurnPlayerId)
  }

  @Test
  fun `check win use case detects when word is fully revealed`() {
    val checkWin = CheckWinUseCase()
    val word = "CASA"
    assertFalse(checkWin(word, setOf('C', 'A')))
    assertTrue(checkWin(word, setOf('C', 'A', 'S')))
  }
}

