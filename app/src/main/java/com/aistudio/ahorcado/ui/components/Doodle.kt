package com.aistudio.ahorcado.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.ahorcado.domain.logic.HangmanEngine
import com.aistudio.ahorcado.domain.model.Player
import com.aistudio.ahorcado.ui.theme.DoodleColors

/**
 * Botón estilo doodle del icono: relleno sólido, borde negro grueso
 * y sombra dura desplazada.
 */
@Composable
fun DoodleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = DoodleColors.Wood,
    textColor: Color = DoodleColors.Ink,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(modifier = modifier) {
        // Sombra dura
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 5.dp)
                .clip(shape)
                .background(if (enabled) DoodleColors.Ink else DoodleColors.Ink.copy(alpha = 0.25f)),
        )
        // Cara del botón
        Box(
            modifier = Modifier
                .clip(shape)
                .background(if (enabled) color else color.copy(alpha = 0.45f))
                .border(BorderStroke(3.dp, DoodleColors.Ink), shape)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 13.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) textColor else textColor.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Ficha de letra estilo las del icono: crema, borde negro grueso.
 */
@Composable
fun LetterTile(
    letter: Char?,
    state: HangmanEngine.KeyState,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    tileSize: Dp = 44.dp,
) {
    val (bg, fg) = when (state) {
        HangmanEngine.KeyState.UNUSED -> DoodleColors.Cream to DoodleColors.Ink
        HangmanEngine.KeyState.CORRECT -> DoodleColors.Leaf to DoodleColors.Ink
        HangmanEngine.KeyState.WRONG -> DoodleColors.CreamDark to DoodleColors.Ink.copy(alpha = 0.35f)
    }
    val shape = RoundedCornerShape(10.dp)
    val clickableMod =
        if (onClick != null && state == HangmanEngine.KeyState.UNUSED) {
            Modifier.clickable(onClick = onClick)
        } else {
            Modifier
        }
    Box(
        modifier = modifier
            .size(tileSize)
            .clip(shape)
            .background(bg)
            .border(BorderStroke(2.5.dp, DoodleColors.Ink), shape)
            .then(clickableMod),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter?.toString() ?: "",
            fontWeight = FontWeight.Black,
            fontSize = (tileSize.value * 0.48f).sp,
            color = fg,
        )
    }
}

/**
 * Casillas de la palabra: muestra el patrón enmascarado ("C_S_").
 * En ROUND_OVER el patrón ya es la palabra completa.
 */
@Composable
fun WordBlanks(
    pattern: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
    ) {
        pattern.forEachIndexed { i, ch ->
            val visible = ch != '_'
            val shape = RoundedCornerShape(10.dp)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(shape)
                    .background(DoodleColors.Cream)
                    .border(BorderStroke(2.5.dp, DoodleColors.Ink), shape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (visible) ch.toString() else "–",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = if (visible) DoodleColors.Navy else DoodleColors.Ink.copy(alpha = 0.4f),
                )
            }
            if (i < pattern.length - 1) Spacer(Modifier.width(6.dp))
        }
    }
}

/**
 * Chip de jugador con color, nombre y puntos. Resaltado si es su turno.
 */
@Composable
fun PlayerChip(
    player: Player,
    isTurn: Boolean,
    isMe: Boolean,
    modifier: Modifier = Modifier,
) {
    val base = DoodleColors.PlayerColors[player.colorIndex % DoodleColors.PlayerColors.size]
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (isTurn) base else base.copy(alpha = 0.35f))
            .border(
                BorderStroke(if (isTurn) 3.dp else 2.dp, DoodleColors.Ink),
                shape,
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = (if (isMe) "⭐ " else "") + player.name.take(10),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            color = DoodleColors.Ink,
            maxLines = 1,
        )
        Text(
            text = "${player.score} pts",
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = DoodleColors.Ink.copy(alpha = 0.75f),
        )
        if (!player.connected) {
            Text(
                text = "desconectado",
                fontSize = 10.sp,
                color = DoodleColors.Brick,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * Anillo de cuenta regresiva del turno.
 */
@Composable
fun CountdownRing(
    remainingSec: Int,
    totalSec: Int,
    modifier: Modifier = Modifier,
) {
    val frac = (remainingSec.coerceIn(0, totalSec).toFloat() / totalSec.coerceAtLeast(1))
        .coerceIn(0f, 1f)
    Box(
        modifier = modifier.size(52.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = 6.dp.toPx()
            drawArc(
                color = DoodleColors.Ink,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(stroke + 3.dp.toPx()),
            )
            drawArc(
                color = if (remainingSec <= 5) DoodleColors.Brick else DoodleColors.Wood,
                startAngle = -90f,
                sweepAngle = 360f * frac,
                useCenter = false,
                style = Stroke(stroke),
            )
        }
        Text(
            text = remainingSec.toString(),
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            color = DoodleColors.Paper,
        )
    }
}

/**
 * Puntos de errores: círculos estilo doodle.
 */
@Composable
fun ErrorPips(errors: Int, maxErrors: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(maxErrors) { i ->
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (i < errors) DoodleColors.Brick else DoodleColors.Paper.copy(alpha = 0.25f))
                    .border(BorderStroke(2.dp, DoodleColors.Ink), CircleShape),
            )
        }
    }
}

/** Título doodle grande con sombra dura. */
@Composable
fun DoodleTitle(text: String, modifier: Modifier = Modifier, fontSizeSp: Float = 44f) {
    Box(modifier = modifier) {
        Text(
            text = text,
            fontWeight = FontWeight.Black,
            fontSize = fontSizeSp.sp,
            color = DoodleColors.Ink,
            modifier = Modifier.offset(x = 3.dp, y = 4.dp),
            textAlign = TextAlign.Center,
        )
        Text(
            text = text,
            fontWeight = FontWeight.Black,
            fontSize = fontSizeSp.sp,
            color = DoodleColors.Cream,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.labelLarge,
        color = DoodleColors.Cream.copy(alpha = 0.8f),
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
}
