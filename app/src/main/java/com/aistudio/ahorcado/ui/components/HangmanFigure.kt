package com.aistudio.ahorcado.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.aistudio.ahorcado.ui.theme.DoodleColors
import androidx.compose.foundation.BorderStroke

/**
 * El ahorcado estilo doodle del icono: horca de madera naranja con
 * contornos negros gruesos y muñeco blanco, sobre panel azul marino.
 *
 * Partes por número de errores (0-6):
 * 1 cabeza, 2 cuerpo, 3 brazo izq, 4 brazo der, 5 pierna izq, 6 pierna der.
 */
@Composable
fun HangmanFigure(
    errors: Int,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(DoodleColors.NavyDeep)
            .border(BorderStroke(3.dp, DoodleColors.Ink), shape)
            .padding(8.dp),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(1f),
        ) {
            drawGallows()
            drawStickman(errors.coerceIn(0, 6))
        }
    }
}

private fun DrawScope.drawGallows() {
    val w = size.width
    val h = size.height
    val ink = DoodleColors.Ink
    val wood = DoodleColors.Wood
    val woodDark = DoodleColors.WoodDark

    val beam = w * 0.075f          // grosor de la madera
    val baseY = h * 0.90f          // suelo
    val postX = w * 0.22f          // poste vertical
    val topY = h * 0.10f           // viga superior
    val ropeX = w * 0.62f          // x de la soga

    fun woodLine(from: Offset, to: Offset, width: Float) {
        // contorno negro grueso
        drawLine(ink, from, to, strokeWidth = width + w * 0.03f, cap = StrokeCap.Round)
        // madera
        drawLine(wood, from, to, strokeWidth = width, cap = StrokeCap.Round)
        // veta oscura
        drawLine(
            woodDark,
            from + Offset(width * 0.18f, width * 0.18f),
            to + Offset(width * 0.18f, width * 0.18f),
            strokeWidth = width * 0.28f,
            cap = StrokeCap.Round,
        )
    }

    // Base
    woodLine(Offset(w * 0.08f, baseY), Offset(w * 0.92f, baseY), beam)
    // Poste
    woodLine(Offset(postX, baseY), Offset(postX, topY), beam)
    // Viga superior
    woodLine(Offset(postX, topY), Offset(ropeX + beam * 0.4f, topY), beam)
    // Refuerzo diagonal
    woodLine(Offset(postX, topY + h * 0.22f), Offset(postX + w * 0.20f, topY), beam * 0.8f)
    // Soga
    val ropeTop = topY + beam * 0.4f
    val ropeBottom = h * 0.30f
    drawLine(ink, Offset(ropeX, ropeTop), Offset(ropeX, ropeBottom),
        strokeWidth = w * 0.045f, cap = StrokeCap.Round)
    drawLine(Color(0xFFF2C14E), Offset(ropeX, ropeTop), Offset(ropeX, ropeBottom),
        strokeWidth = w * 0.025f, cap = StrokeCap.Round)
}

private fun DrawScope.drawStickman(errors: Int) {
    if (errors <= 0) return
    val w = size.width
    val h = size.height
    val ink = DoodleColors.Ink
    val paper = DoodleColors.Paper

    val cx = w * 0.62f
    val headR = w * 0.085f
    val headCy = h * 0.30f + headR
    val stroke = w * 0.032f

    fun limb(from: Offset, to: Offset) {
        drawLine(ink, from, to, strokeWidth = stroke + w * 0.018f, cap = StrokeCap.Round)
        drawLine(paper, from, to, strokeWidth = stroke, cap = StrokeCap.Round)
    }

    // 1. Cabeza
    drawCircle(ink, radius = headR + w * 0.012f, center = Offset(cx, headCy))
    drawCircle(paper, radius = headR, center = Offset(cx, headCy))

    if (errors >= 2) {
        // 2. Cuerpo
        val neckY = headCy + headR
        limb(Offset(cx, neckY), Offset(cx, neckY + h * 0.20f))
    }
    val shoulderY = headCy + headR + h * 0.045f
    val hipY = headCy + headR + h * 0.20f
    if (errors >= 3) {
        // 3. Brazo izquierdo
        limb(Offset(cx, shoulderY), Offset(cx - w * 0.11f, shoulderY + h * 0.10f))
    }
    if (errors >= 4) {
        // 4. Brazo derecho
        limb(Offset(cx, shoulderY), Offset(cx + w * 0.11f, shoulderY + h * 0.10f))
    }
    if (errors >= 5) {
        // 5. Pierna izquierda
        limb(Offset(cx, hipY), Offset(cx - w * 0.09f, hipY + h * 0.13f))
    }
    if (errors >= 6) {
        // 6. Pierna derecha
        limb(Offset(cx, hipY), Offset(cx + w * 0.09f, hipY + h * 0.13f))
    }
}
