package com.example.presentation.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.models.GameStatus
import com.example.data.repository.GameUiEvent
import com.example.presentation.components.ConfettiCelebration
import com.example.presentation.components.DoodleButton
import com.example.presentation.components.DoodleCard
import com.example.presentation.components.HangmanCanvas
import com.example.presentation.components.KeyboardGrid
import com.example.presentation.components.LivesIndicator
import com.example.presentation.components.PlayerChip
import com.example.presentation.components.RevealedWordView
import com.example.presentation.components.VisualTheme
import com.example.presentation.components.doodleNotebookBackground
import com.example.presentation.components.shake

/**
 * Pantalla principal del juego activo con Ahorcado Stickman, estética de libreta de bocetos,
 * temporizador con tensión, pistas de categoría, efecto de sacudida y confeti de victoria.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    onExitGame: () -> Unit,
    onPlayAgainRequested: () -> Unit,
    viewModel: GameViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val localPlayer by viewModel.localPlayer.collectAsStateWithLifecycle()
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()

    val charcoalBorder = if (isDark) Color(0xFF64748B) else Color(0xFF1E293B)
    var showThemeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.initSoundHaptics(context)
    }

    LaunchedEffect(gameState) {
        viewModel.handleSoundEffects(gameState)
    }

    LaunchedEffect(Unit) {
        viewModel.gameEvents.collect { event ->
            when (event) {
                is GameUiEvent.NavigateToWordSelection -> onPlayAgainRequested()
                is GameUiEvent.NavigateToLobby -> onExitGame()
                else -> Unit
            }
        }
    }

    val isMyTurn = gameState.currentTurnPlayerId == localPlayer.id
    val isGameOver = gameState.status == GameStatus.WON || gameState.status == GameStatus.LOST
    val currentTurnPlayer = gameState.players.find { it.id == gameState.currentTurnPlayerId }

    // Pulsación visual cuando el temporizador llega a <= 5s
    val isUrgentTime = gameState.turnTimeRemainingSec <= 5 && gameState.status == GameStatus.PLAYING
    val infiniteTransition = rememberInfiniteTransition(label = "urgentPulse")
    val urgentScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isUrgentTime) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val timerColor by animateColorAsState(
        targetValue = when {
            gameState.turnTimeRemainingSec <= 5 -> Color(0xFFEF4444)
            gameState.turnTimeRemainingSec <= 10 -> Color(0xFFF59E0B)
            else -> MaterialTheme.colorScheme.primary
        },
        label = "timerColor"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = modifier.testTag("game_screen"),
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp).border(1.5.dp, charcoalBorder, CircleShape)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Create,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Stickman Ahorcado",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp
                                )
                                if (gameState.category.isNotBlank()) {
                                    Text(
                                        text = "📂 ${gameState.category}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        // Selector de Tema Visual
                        IconButton(onClick = { showThemeDialog = true }) {
                            Icon(Icons.Default.ColorLens, contentDescription = "Cambiar estilo visual", tint = MaterialTheme.colorScheme.secondary)
                        }
                        // Botón Salir
                        IconButton(
                            onClick = {
                                viewModel.onExitGame()
                                onExitGame()
                            }
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Salir de la sala")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .doodleNotebookBackground(isDark)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Barra de Jugadores Stickman
                if (gameState.players.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(gameState.players) { player ->
                            val isTurn = player.id == gameState.currentTurnPlayerId
                            PlayerChip(
                                player = player,
                                isCurrentTurn = isTurn,
                                isLocalPlayer = player.id == localPlayer.id
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 2. Banner de Turno + Temporizador Circular
                DoodleCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (isMyTurn) MaterialTheme.colorScheme.primary else charcoalBorder,
                    backgroundColor = if (isMyTurn) {
                        if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color(0xFFDBEAFE)
                    } else {
                        if (isDark) Color(0xFF1E293B) else Color(0xFFFFFDF8)
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isMyTurn) "✏️ ¡Tu turno de arriesgar!" else "⏳ Turno de ${currentTurnPlayer?.name ?: "otro jugador"}",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = if (isMyTurn) {
                                    if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }

                        // Reloj de Turno
                        if (gameState.status == GameStatus.PLAYING) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .scale(urgentScale)
                                    .background(timerColor.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                                    .border(1.2.dp, timerColor, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = timerColor, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${gameState.turnTimeRemainingSec}s",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = timerColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Indicador de Vidas + Lienzo del Ahorcado Stickman
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Dibujo del Stickman con animación y expresiones dinámicas
                    DoodleCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(180.dp),
                        tapeColor = Color(0xFFFDE047),
                        borderColor = charcoalBorder,
                        backgroundColor = currentTheme.bgCardColor
                    ) {
                        HangmanCanvas(
                            wrongAttempts = gameState.errors,
                            maxLives = gameState.maxErrors,
                            theme = currentTheme,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .testTag("hangman_canvas")
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Indicador de Vidas estilo boceto
                    DoodleCard(
                        modifier = Modifier.height(180.dp),
                        borderColor = charcoalBorder,
                        backgroundColor = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFDF8)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Vidas",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LivesIndicator(
                                errors = gameState.errors,
                                maxErrors = gameState.maxErrors
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Pista Temática
                if (gameState.hint.isNotBlank()) {
                    DoodleCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = charcoalBorder,
                        backgroundColor = if (isDark) Color(0xFF312E81).copy(alpha = 0.4f) else Color(0xFFEDE9FE)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (gameState.hintRevealed) "💡 Pista: ${gameState.hint}" else "💡 Pista disponible (oculta)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (gameState.hintRevealed) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isDark) Color(0xFFEDE9FE) else Color(0xFF4C1D95)
                                )
                            }
                            if (!gameState.hintRevealed) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF7C3AED),
                                    modifier = Modifier.clickable { viewModel.onRevealHint() }
                                ) {
                                    Text(
                                        text = "Revelar",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 5. Letras Adivinadas (con efecto Shake al fallar)
                Box(modifier = Modifier.fillMaxWidth().shake(trigger = gameState.errors)) {
                    RevealedWordView(
                        secretWord = gameState.secretWord,
                        revealedLetters = gameState.revealedLetters,
                        isLost = gameState.status == GameStatus.LOST,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 6. Teclado Virtual con diseño Stickman
                KeyboardGrid(
                    guessedLetters = gameState.guessedLetters,
                    secretWord = gameState.secretWord,
                    isEnabled = isMyTurn && !isGameOver,
                    onLetterClick = { letter -> viewModel.onLetterClicked(letter) }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Lluvia de Confeti al Ganar
        if (gameState.status == GameStatus.WON) {
            ConfettiCelebration(modifier = Modifier.fillMaxSize())
        }

        // Diálogo Fin de Partida con Estilo Boceto
        if (isGameOver) {
            val isWin = gameState.status == GameStatus.WON
            AlertDialog(
                onDismissRequest = { },
                icon = {
                    Icon(
                        imageVector = if (isWin) Icons.Default.Celebration else Icons.Default.SentimentVeryDissatisfied,
                        contentDescription = null,
                        tint = if (isWin) Color(0xFF10B981) else Color(0xFFEF4444),
                        modifier = Modifier.size(52.dp)
                    )
                },
                title = {
                    Text(
                        text = if (isWin) "¡Victoria del Stickman! 🎉" else "¡El Stickman ha caído! ☠️",
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isWin) "¡Salvaron al Stickman adivinando la palabra a tiempo!" else "Se completó el dibujo y se agotaron las vidas.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, charcoalBorder)
                        ) {
                            Text(
                                text = "Palabra: ${gameState.secretWord}",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = if (isDark) Color(0xFFDBEAFE) else Color(0xFF1E3A8A)
                            )
                        }
                    }
                },
                confirmButton = {
                    if (localPlayer.isHost) {
                        DoodleButton(
                            text = "Nueva Ronda",
                            icon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White) },
                            containerColor = MaterialTheme.colorScheme.primary,
                            borderColor = charcoalBorder,
                            onClick = { viewModel.onPlayAgain() },
                            modifier = Modifier.testTag("play_again_button")
                        )
                    } else {
                        Text(
                            text = "Esperando que el anfitrión inicie otra ronda...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            viewModel.onExitGame()
                            onExitGame()
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, charcoalBorder),
                        modifier = Modifier.testTag("exit_game_button")
                    ) {
                        Text("Salir al Lobby", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Diálogo de Selector de Tema Visual
        if (showThemeDialog) {
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                title = { Text("🎨 Elige el Estilo del Dibujo", fontWeight = FontWeight.Black) },
                text = {
                    Column {
                        VisualTheme.values().forEach { theme ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setTheme(theme)
                                        showThemeDialog = false
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentTheme == theme,
                                    onClick = {
                                        viewModel.setTheme(theme)
                                        showThemeDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(theme.displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showThemeDialog = false },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Aceptar", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}
