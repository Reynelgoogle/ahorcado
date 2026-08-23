package com.example.presentation.demo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Player
import com.example.presentation.components.HangmanCanvas
import com.example.presentation.components.KeyboardGrid
import com.example.presentation.components.LivesIndicator
import com.example.presentation.components.PlayerChip
import com.example.presentation.components.RevealedWordView
import com.example.utils.SoundHapticsHelper

private val DEMO_WORDS = listOf(
    "ASTRONAUTA",
    "COMPUTADORA",
    "DINOSAURIO",
    "MARIPOSA",
    "CHOCOLATE",
    "ESPERANZA",
    "GALAXIA",
    "AVENTURA"
)

private val BOT_NAMES = listOf("Tú (Jugador)", "Sofía", "Lucas", "Valentina")

/**
 * Pantalla interactiva para probar y validar todos los elementos de la interfaz:
 * Animaciones del Ahorcado, teclado, estados de victoria/derrota, turnos y efectos de sonido/hápticos.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DemoScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val soundHaptics = remember { SoundHapticsHelper(context) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Estado del juego en modo demo interactivo
    var secretWord by remember { mutableStateOf("ASTRONAUTA") }
    var guessedLetters by remember { mutableStateOf(setOf<Char>()) }
    var playerCount by remember { mutableIntStateOf(3) }
    var currentTurnIndex by remember { mutableIntStateOf(0) }
    var showWinDialog by remember { mutableStateOf(false) }
    var showLoseDialog by remember { mutableStateOf(false) }
    var customWordInput by remember { mutableStateOf("") }

    // Errores calculados
    val incorrectLetters = guessedLetters.filter { it !in secretWord }
    val remainingLives = (6 - incorrectLetters.size).coerceAtLeast(0)
    val isWordComplete = secretWord.all { it in guessedLetters }
    val isGameLost = remainingLives == 0

    // Lista dinámica de jugadores demo
    val players = remember(playerCount) {
        (0 until playerCount).map { index ->
            Player(
                id = "demo_player_$index",
                name = BOT_NAMES.getOrElse(index) { "Jugador ${index + 1}" },
                isHost = index == 0,
                colorIndex = index % 4
            )
        }
    }

    val currentPlayer = players.getOrElse(currentTurnIndex % players.size) { players.first() }

    // Función para adivinar una letra en el juego interactivo
    fun guessLetter(letter: Char) {
        if (letter in guessedLetters || isWordComplete || isGameLost) return
        val newGuessed = guessedLetters + letter
        guessedLetters = newGuessed

        if (letter in secretWord) {
            soundHaptics.playCorrect()
            if (secretWord.all { it in newGuessed }) {
                soundHaptics.playWin()
                showWinDialog = true
            }
        } else {
            soundHaptics.playIncorrect()
            val newIncorrect = newGuessed.filter { it !in secretWord }
            if (newIncorrect.size >= 6) {
                soundHaptics.playLose()
                showLoseDialog = true
            } else {
                // Rotar turno al siguiente jugador si falla
                currentTurnIndex = (currentTurnIndex + 1) % players.size
                soundHaptics.playTurnChange()
            }
        }
    }

    fun resetGame(newWord: String = secretWord) {
        secretWord = newWord
        guessedLetters = emptySet()
        currentTurnIndex = 0
        showWinDialog = false
        showLoseDialog = false
    }

    Scaffold(
        modifier = modifier.testTag("demo_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Modo Demo Interactivo", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val nextWord = DEMO_WORDS.random()
                        resetGame(nextWord)
                    }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reiniciar partida")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Juego en Vivo") },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Inspector Visual") },
                    icon = { Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            if (selectedTab == 0) {
                // Pestaña 1: Juego interactivo completo
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Turnos de Jugadores
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(players) { player ->
                                PlayerChip(
                                    player = player,
                                    isCurrentTurn = player.id == currentPlayer.id,
                                    isLocalPlayer = player.id == "demo_player_0"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Banner de Turno
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = if (currentPlayer.id == "demo_player_0")
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (currentPlayer.id == "demo_player_0") Color(0xFF4CAF50) else Color(0xFF2196F3))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (currentPlayer.id == "demo_player_0") "¡Es tu turno!" else "Turno de: ${currentPlayer.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Muñeco y Vidas
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        HangmanCanvas(
                            wrongAttempts = incorrectLetters.size,
                            maxLives = 6,
                            modifier = Modifier.size(150.dp, 160.dp)
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            LivesIndicator(errors = incorrectLetters.size)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Errores: ${incorrectLetters.size}/6",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (incorrectLetters.size >= 4) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Botón de Bot Auto-Guess
                            AssistChip(
                                onClick = {
                                    val availableLetters = ('A'..'Z').filter { it !in guessedLetters }
                                    if (availableLetters.isNotEmpty()) {
                                        // 70% probabilidad de adivinar letra correcta
                                        val correctAvailable = secretWord.filter { it !in guessedLetters }
                                        val pick = if (correctAvailable.isNotEmpty() && (0..10).random() < 7) {
                                            correctAvailable.random()
                                        } else {
                                            availableLetters.random()
                                        }
                                        guessLetter(pick)
                                    }
                                },
                                label = { Text("Simular Bot", style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = {
                                    Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        }
                    }

                    // Palabra Secreta Revelada
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            RevealedWordView(
                                secretWord = secretWord,
                                revealedLetters = guessedLetters,
                                isLost = isGameLost,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Teclado interactivo
                    KeyboardGrid(
                        guessedLetters = guessedLetters,
                        secretWord = secretWord,
                        onLetterClick = { guessLetter(it) },
                        isEnabled = !isWordComplete && !isGameLost,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                // Pestaña 2: Inspector Visual y Playground de Pruebas
                val scrollState = rememberScrollState()
                var sliderErrors by remember { mutableFloatStateOf(3f) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Sección 1: Inspección de Estados del Dibujo del Ahorcado
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🎨 Animación del Ahorcado por Etapas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            HangmanCanvas(
                                wrongAttempts = sliderErrors.toInt(),
                                maxLives = 6,
                                modifier = Modifier.size(170.dp, 190.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            LivesIndicator(errors = sliderErrors.toInt())

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Etapa actual: ${sliderErrors.toInt()} / 6 partes dibujadas",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Slider(
                                value = sliderErrors,
                                onValueChange = { sliderErrors = it },
                                valueRange = 0f..6f,
                                steps = 5,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Button(
                                    onClick = { sliderErrors = (sliderErrors - 1).coerceAtLeast(0f) },
                                    enabled = sliderErrors > 0f
                                ) {
                                    Text("-1 Error")
                                }
                                Button(
                                    onClick = { sliderErrors = (sliderErrors + 1).coerceAtMost(6f) },
                                    enabled = sliderErrors < 6f
                                ) {
                                    Text("+1 Error")
                                }
                            }
                        }
                    }

                    // Sección 2: Pruebas de Efectos de Sonido y Hápticos
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "🔊 Sonidos y Vibración Háptica",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(onClick = { soundHaptics.playCorrect() }) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF4CAF50))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Acierto")
                                }

                                OutlinedButton(onClick = { soundHaptics.playIncorrect() }) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFE53935))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Fallo")
                                }

                                OutlinedButton(onClick = { soundHaptics.playTurnChange() }) {
                                    Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Turno")
                                }

                                OutlinedButton(onClick = { soundHaptics.playWin() }) {
                                    Icon(Icons.Default.Celebration, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFFFA000))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Victoria")
                                }

                                OutlinedButton(onClick = { soundHaptics.playLose() }) {
                                    Icon(Icons.Default.SentimentVeryDissatisfied, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Derrota")
                                }
                            }
                        }
                    }

                    // Sección 3: Configurar Palabra y Jugadores para la Demo
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "👥 Configuración de Sala Demo",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Cantidad de jugadores (1 a 4):", style = MaterialTheme.typography.bodyMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                (1..4).forEach { count ->
                                    FilterChip(
                                        selected = playerCount == count,
                                        onClick = {
                                            playerCount = count
                                            currentTurnIndex = 0
                                        },
                                        label = { Text("$count Jugador${if (count > 1) "es" else ""}") }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Cambiar palabra secreta:", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(6.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                DEMO_WORDS.forEach { word ->
                                    AssistChip(
                                        onClick = {
                                            resetGame(word)
                                            selectedTab = 0
                                        },
                                        label = { Text(word) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = customWordInput,
                                onValueChange = { customWordInput = it.uppercase().filter { c -> c in 'A'..'Z' || c == 'Ñ' } },
                                label = { Text("Escribir palabra personalizada") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    if (customWordInput.isNotBlank()) {
                                        IconButton(onClick = {
                                            if (customWordInput.length >= 3) {
                                                resetGame(customWordInput)
                                                customWordInput = ""
                                                selectedTab = 0
                                            }
                                        }) {
                                            Icon(Icons.Default.Check, contentDescription = "Aplicar palabra")
                                        }
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showWinDialog = true },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
                                ) {
                                    Icon(Icons.Default.Celebration, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ver Victoria")
                                }

                                Button(
                                    onClick = { showLoseDialog = true },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.SentimentVeryDissatisfied, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ver Derrota")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de Victoria Demo
    if (showWinDialog) {
        AlertDialog(
            onDismissRequest = { showWinDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Celebration,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "¡Palabra Adivinada!",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "La palabra era:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = secretWord,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "¡Excelente trabajo en equipo!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val next = DEMO_WORDS.random()
                        resetGame(next)
                    }
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Jugar otra vez")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWinDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Diálogo de Derrota Demo
    if (showLoseDialog) {
        AlertDialog(
            onDismissRequest = { showLoseDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.SentimentVeryDissatisfied,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "¡El Ahorcado se completó!",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "La palabra secreta era:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = secretWord,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val next = DEMO_WORDS.random()
                        resetGame(next)
                    }
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reintentar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLoseDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}
