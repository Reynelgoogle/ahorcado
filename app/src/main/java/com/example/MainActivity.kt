package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.repository.GameRepository
import com.example.data.repository.GameUiEvent
import com.example.presentation.demo.DemoScreen
import com.example.presentation.game.GameScreen
import com.example.presentation.lobby.LobbyScreen
import com.example.presentation.word.WordSelectionScreen
import com.example.ui.theme.MyApplicationTheme

object AppDestinations {
    const val LOBBY = "lobby"
    const val WORD_SELECTION = "word_selection"
    const val GAME = "game"
    const val DEMO = "demo"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HangmanAppNavigation()
                }
            }
        }
    }
}

@Composable
fun HangmanAppNavigation() {
    val navController = rememberNavController()

    // Escuchar eventos globales del repositorio para sincronizar navegación entre dispositivos
    LaunchedEffect(Unit) {
        GameRepository.gameEvents.collect { event ->
            when (event) {
                is GameUiEvent.GameStarted -> {
                    navController.navigate(AppDestinations.GAME) {
                        popUpTo(AppDestinations.LOBBY)
                    }
                }
                is GameUiEvent.NavigateToWordSelection -> {
                    navController.navigate(AppDestinations.WORD_SELECTION) {
                        popUpTo(AppDestinations.LOBBY)
                    }
                }
                is GameUiEvent.NavigateToLobby -> {
                    navController.navigate(AppDestinations.LOBBY) {
                        popUpTo(0) { inclusive = true }
                    }
                }
                else -> Unit
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppDestinations.LOBBY
    ) {
        composable(AppDestinations.LOBBY) {
            LobbyScreen(
                onNavigateToWordSelection = {
                    navController.navigate(AppDestinations.WORD_SELECTION)
                },
                onNavigateToGame = {
                    navController.navigate(AppDestinations.GAME)
                },
                onNavigateToDemo = {
                    navController.navigate(AppDestinations.DEMO)
                }
            )
        }

        composable(AppDestinations.DEMO) {
            DemoScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(AppDestinations.WORD_SELECTION) {
            WordSelectionScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onGameStarted = {
                    navController.navigate(AppDestinations.GAME) {
                        popUpTo(AppDestinations.LOBBY)
                    }
                }
            )
        }

        composable(AppDestinations.GAME) {
            GameScreen(
                onExitGame = {
                    navController.navigate(AppDestinations.LOBBY) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onPlayAgainRequested = {
                    navController.navigate(AppDestinations.WORD_SELECTION) {
                        popUpTo(AppDestinations.LOBBY)
                    }
                }
            )
        }
    }
}
