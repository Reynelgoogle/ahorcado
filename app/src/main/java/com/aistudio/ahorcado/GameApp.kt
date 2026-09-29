package com.aistudio.ahorcado

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aistudio.ahorcado.data.GameRepository
import com.aistudio.ahorcado.ui.screens.GameScreen
import com.aistudio.ahorcado.ui.screens.LobbyScreen
import com.aistudio.ahorcado.ui.screens.MenuScreen
import com.aistudio.ahorcado.ui.screens.WordScreen

object Routes {
    const val MENU = "menu"
    const val LOBBY = "lobby"
    const val WORD = "word"
    const val GAME = "game"
}

@Composable
fun GameApp() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Eventos globales del repositorio -> navegación y avisos.
    LaunchedEffect(Unit) {
        GameRepository.events.collect { ev ->
            when (ev) {
                is GameRepository.UiEvent.Toast ->
                    Toast.makeText(context, ev.message, Toast.LENGTH_SHORT).show()
                is GameRepository.UiEvent.PlayerJoined ->
                    Toast.makeText(context, "✅ ${ev.name} se unió", Toast.LENGTH_SHORT).show()
                is GameRepository.UiEvent.PlayerLeft ->
                    Toast.makeText(context, "👋 ${ev.name} salió", Toast.LENGTH_SHORT).show()
                GameRepository.UiEvent.KickedOut ->
                    Toast.makeText(context, "Fuiste expulsado de la sala", Toast.LENGTH_LONG).show()
                GameRepository.UiEvent.RoomFullNotice ->
                    Toast.makeText(context, "Sala llena o partida en curso", Toast.LENGTH_LONG).show()
                GameRepository.UiEvent.HostGone ->
                    Toast.makeText(context, "El anfitrión se desconectó", Toast.LENGTH_LONG).show()
                GameRepository.UiEvent.GoToMenu ->
                    navController.navigate(Routes.MENU) {
                        popUpTo(0) { inclusive = true }
                    }
                GameRepository.UiEvent.GoToLobby ->
                    navController.navigate(Routes.LOBBY) {
                        popUpTo(Routes.MENU)
                    }
                GameRepository.UiEvent.GoToWord ->
                    navController.navigate(Routes.WORD) {
                        popUpTo(Routes.MENU)
                    }
                GameRepository.UiEvent.GoToGame ->
                    navController.navigate(Routes.GAME) {
                        popUpTo(Routes.MENU)
                    }
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.MENU) {
        composable(Routes.MENU) { MenuScreen() }
        composable(Routes.LOBBY) {
            LobbyScreen(onNavigateToWord = { navController.navigate(Routes.WORD) })
        }
        composable(Routes.WORD) { WordScreen() }
        composable(Routes.GAME) { GameScreen() }
    }
}
