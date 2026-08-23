package com.example.presentation.game

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.GameState
import com.example.data.models.GameStatus
import com.example.data.models.Player
import com.example.data.repository.GameRepository
import com.example.data.repository.GameUiEvent
import com.example.presentation.components.VisualTheme
import com.example.utils.SoundHapticsHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel(
    private val repository: GameRepository = GameRepository
) : ViewModel() {

    val gameState: StateFlow<GameState> = repository.gameState
    val localPlayer: StateFlow<Player> = repository.localPlayer
    val gameEvents: SharedFlow<GameUiEvent> = repository.gameEvents

    private val _currentTheme = MutableStateFlow(VisualTheme.CLASSIC)
    val currentTheme: StateFlow<VisualTheme> = _currentTheme.asStateFlow()

    private var soundHapticsHelper: SoundHapticsHelper? = null

    fun initSoundHaptics(context: Context) {
        if (soundHapticsHelper == null) {
            soundHapticsHelper = SoundHapticsHelper(context)
        }
    }

    fun setTheme(theme: VisualTheme) {
        _currentTheme.value = theme
    }

    fun onLetterClicked(letter: Char) {
        val currentState = gameState.value
        if (currentState.status != GameStatus.PLAYING) return

        // Solo el jugador con el turno actual puede jugar
        if (currentState.currentTurnPlayerId != localPlayer.value.id) return

        soundHapticsHelper?.playKeyPress()
        repository.guessLetter(letter)
    }

    fun onRevealHint() {
        soundHapticsHelper?.playKeyPress()
        repository.revealHint()
    }

    fun onPlayAgain() {
        repository.resetGameToWordSelection()
    }

    fun onExitGame() {
        repository.returnToLobby()
    }

    fun handleSoundEffects(state: GameState) {
        val helper = soundHapticsHelper ?: return
        when (state.status) {
            GameStatus.WON -> helper.playWin()
            GameStatus.LOST -> helper.playLose()
            GameStatus.PLAYING -> {
                if (state.lastGuessedCorrect == true) {
                    helper.playCorrect()
                } else if (state.lastGuessedCorrect == false) {
                    helper.playIncorrect()
                }
            }
            else -> Unit
        }
    }
}
