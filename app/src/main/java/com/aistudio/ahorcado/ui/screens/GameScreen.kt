package com.aistudio.ahorcado.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.ahorcado.data.GameRepository
import com.aistudio.ahorcado.domain.logic.HangmanEngine
import com.aistudio.ahorcado.domain.model.GamePhase
import com.aistudio.ahorcado.domain.model.NetMode
import com.aistudio.ahorcado.domain.model.Player
import com.aistudio.ahorcado.ui.components.CountdownRing
import com.aistudio.ahorcado.ui.components.DoodleButton
import com.aistudio.ahorcado.ui.components.ErrorPips
import com.aistudio.ahorcado.ui.components.HangmanFigure
import com.aistudio.ahorcado.ui.components.LetterTile
import com.aistudio.ahorcado.ui.components.PlayerChip
import com.aistudio.ahorcado.ui.components.WordBlanks
import com.aistudio.ahorcado.ui.theme.DoodleColors
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameScreen() {
    val state by GameRepository.publicState.collectAsState()
    val me by GameRepository.localPlayer.collectAsState()
    val mode by GameRepository.modeFlow.collectAsState()

    val turnPlayer = state.players.find { it.id == state.turnPlayerId }
    val isMyTurn = mode != NetMode.OFFLINE && state.turnPlayerId == me.id
    val canPlay = state.phase == GamePhase.PLAYING &&
        (isMyTurn || mode == NetMode.OFFLINE)

    // Ticker de cuenta regresiva (el deadline lo pone el host).
    var nowMs by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(state.phase, state.turnDeadline) {
        if (state.phase != GamePhase.PLAYING || state.turnDeadline <= 0L) return@LaunchedEffect
        while (true) {
            nowMs = SystemClock.elapsedRealtime()
            delay(500)
        }
    }
    val remainingSec = ((state.turnDeadline - nowMs) / 1000).toInt().coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DoodleColors.Navy)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ---- jugadores ----
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.players.forEach { p ->
                PlayerChip(
                    player = p,
                    isTurn = p.id == state.turnPlayerId && state.phase == GamePhase.PLAYING,
                    isMe = p.id == me.id,
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        // ---- turno + temporizador + errores ----
        if (state.phase == GamePhase.PLAYING) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = turnLabel(mode, isMyTurn, turnPlayer, me),
                        color = DoodleColors.Cream,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    ErrorPips(errors = state.errors, maxErrors = state.maxErrors)
                }
                CountdownRing(remainingSec = remainingSec, totalSec = state.turnSeconds)
            }
            Spacer(Modifier.height(10.dp))
        }

        // ---- muñeco ----
        HangmanFigure(
            errors = state.errors,
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .padding(vertical = 4.dp),
        )
        Spacer(Modifier.height(10.dp))

        // ---- categoría y pista ----
        if (state.category.isNotBlank()) {
            Text(
                text = "📂 ${state.category}",
                color = DoodleColors.Cream.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(6.dp))
        }
        HintArea(
            hint = state.hint,
            revealed = state.hintRevealed,
            phase = state.phase,
            onReveal = {
                if (mode == NetMode.OFFLINE) {
                    GameRepository.requestHintAs(state.turnPlayerId)
                } else {
                    GameRepository.requestHint()
                }
            },
        )
        Spacer(Modifier.height(10.dp))

        // ---- palabra ----
        if (state.pattern.isNotEmpty()) {
            WordBlanks(pattern = state.pattern)
            Spacer(Modifier.height(6.dp))
        }

        // ---- último intento ----
        state.lastLetter?.let { letter ->
            val who = state.players.find { it.id == state.lastPlayerId }?.name ?: ""
            val mark = if (state.lastCorrect == true) "✓" else "✗"
            val color = if (state.lastCorrect == true) DoodleColors.Leaf else DoodleColors.Brick
            Text(
                text = "$who probó '$letter'  $mark",
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(6.dp))
        }

        // ---- teclado ----
        if (state.phase == GamePhase.PLAYING) {
            val keys = HangmanEngine.keyStates(state.guessed, state.revealed)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                HangmanEngine.ALPHABET.forEach { ch ->
                    val ks = keys[ch] ?: HangmanEngine.KeyState.UNUSED
                    LetterTile(
                        letter = ch,
                        state = ks,
                        onClick = if (canPlay && ks == HangmanEngine.KeyState.UNUSED) {
                            {
                                if (mode == NetMode.OFFLINE) {
                                    GameRepository.guessAs(ch, state.turnPlayerId)
                                } else {
                                    GameRepository.guess(ch)
                                }
                            }
                        } else {
                            null
                        },
                        tileSize = 42.dp,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    // ---- fin de ronda ----
    if (state.phase == GamePhase.ROUND_OVER) {
        RoundOverDialog(
            state = state,
            me = me,
            isAuthoritative = mode != NetMode.CLIENT,
        )
    }
}

private fun turnLabel(
    mode: NetMode,
    isMyTurn: Boolean,
    turnPlayer: Player?,
    me: Player,
): String {
    if (turnPlayer == null) return ""
    return when {
        mode == NetMode.OFFLINE -> "📱 Turno de ${turnPlayer.name}"
        isMyTurn -> "🎯 ¡Es tu turno, ${me.name}!"
        else -> "Turno de ${turnPlayer.name}…"
    }
}

@Composable
private fun HintArea(
    hint: String,
    revealed: Boolean,
    phase: GamePhase,
    onReveal: () -> Unit,
) {
    if (hint.isBlank() || phase != GamePhase.PLAYING) return
    if (revealed) {
        val shape = RoundedCornerShape(12.dp)
        Box(
            modifier = Modifier
                .clip(shape)
                .background(DoodleColors.Cream)
                .border(BorderStroke(2.5.dp, DoodleColors.Ink), shape)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text(
                text = "💡 $hint",
                color = DoodleColors.Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
    } else {
        DoodleButton(
            text = "💡 Ver pista",
            onClick = onReveal,
            color = DoodleColors.Cream,
        )
    }
}

@Composable
private fun RoundOverDialog(
    state: com.aistudio.ahorcado.domain.model.PublicState,
    me: Player,
    isAuthoritative: Boolean,
) {
    val won = state.winnerId != null
    val winner = state.players.find { it.id == state.winnerId }
    val sorted = state.players.sortedByDescending { it.score }

    AlertDialog(
        onDismissRequest = {},
        containerColor = DoodleColors.NavyDeep,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(
                text = if (won) "🎉 ¡${winner?.name ?: ""} adivinó!" else "💀 ¡El ahorcado se completó!",
                color = DoodleColors.Cream,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "La palabra era:",
                    color = DoodleColors.Cream.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = state.secretWord.ifEmpty { state.pattern },
                    color = DoodleColors.Gold,
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp,
                    letterSpacing = 2.sp,
                )
                Spacer(Modifier.height(12.dp))
                sorted.forEach { p ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = (if (p.id == me.id) "⭐ " else "") + p.name,
                            color = DoodleColors.Cream,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${p.score} pts",
                            color = DoodleColors.Wood,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        },
        confirmButton = {
            if (isAuthoritative) {
                DoodleButton(text = "🔄 Nueva ronda", onClick = { GameRepository.rematch() })
            } else {
                Text(
                    "Esperando al anfitrión…",
                    color = DoodleColors.Cream.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                )
            }
        },
        dismissButton = {
            DoodleButton(
                text = "Salir",
                onClick = { GameRepository.leaveToMenu() },
                color = DoodleColors.Cream,
            )
        },
    )
}
