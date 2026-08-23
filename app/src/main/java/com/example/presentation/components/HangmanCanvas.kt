package com.example.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

enum class VisualTheme(
    val displayName: String,
    val gallowsColor: Color,
    val ropeColor: Color,
    val bodyColor: Color,
    val accentColor: Color,
    val bgCardColor: Color
) {
    SKETCHBOOK(
        displayName = "Cuaderno",
        gallowsColor = Color(0xFF332F2C),
        ropeColor = Color(0xFFB45309),
        bodyColor = Color(0xFF1E293B),
        accentColor = Color(0xFFEF4444),
        bgCardColor = Color(0xFFFFFDF9)
    ),
    CLASSIC(
        displayName = "Madera",
        gallowsColor = Color(0xFF5D4037),
        ropeColor = Color(0xFFD7CCC8),
        bodyColor = Color(0xFFD32F2F),
        accentColor = Color(0xFFFFB74D),
        bgCardColor = Color(0xFF231F20)
    ),
    CHALKBOARD(
        displayName = "Pizarra",
        gallowsColor = Color(0xFFE2E8F0),
        ropeColor = Color(0xFFCBD5E1),
        bodyColor = Color(0xFFFDE047),
        accentColor = Color(0xFF4ADE80),
        bgCardColor = Color(0xFF1E293B)
    ),
    CYBER_NEON(
        displayName = "Cyber Neón",
        gallowsColor = Color(0xFF00E5FF),
        ropeColor = Color(0xFFFF007F),
        bodyColor = Color(0xFFFFE600),
        accentColor = Color(0xFF00FF66),
        bgCardColor = Color(0xFF0F172A)
    )
}

/**
 * Lienzo del Stickman Ahorcado con estilo de dibujo a mano / libreta de bocetos,
 * físicas de balanceo en la cuerda y expresiones faciales dinámicas según el número de fallos.
 */
@Composable
fun HangmanCanvas(
    wrongAttempts: Int,
    maxLives: Int = 6,
    theme: VisualTheme = VisualTheme.SKETCHBOOK,
    modifier: Modifier = Modifier
) {
    // Animadores de interpolación suave para cada trazo
    val headProgress = remember { Animatable(0f) }
    val torsoProgress = remember { Animatable(0f) }
    val leftArmProgress = remember { Animatable(0f) }
    val rightArmProgress = remember { Animatable(0f) }
    val leftLegProgress = remember { Animatable(0f) }
    val rightLegProgress = remember { Animatable(0f) }

    LaunchedEffect(wrongAttempts) {
        if (wrongAttempts >= 1) headProgress.animateTo(1f, tween(280, easing = FastOutSlowInEasing)) else headProgress.snapTo(0f)
        if (wrongAttempts >= 2) torsoProgress.animateTo(1f, tween(240, easing = FastOutSlowInEasing)) else torsoProgress.snapTo(0f)
        if (wrongAttempts >= 3) leftArmProgress.animateTo(1f, tween(240, easing = FastOutSlowInEasing)) else leftArmProgress.snapTo(0f)
        if (wrongAttempts >= 4) rightArmProgress.animateTo(1f, tween(240, easing = FastOutSlowInEasing)) else rightArmProgress.snapTo(0f)
        if (wrongAttempts >= 5) leftLegProgress.animateTo(1f, tween(240, easing = FastOutSlowInEasing)) else leftLegProgress.snapTo(0f)
        if (wrongAttempts >= 6) rightLegProgress.animateTo(1f, tween(240, easing = FastOutSlowInEasing)) else rightLegProgress.snapTo(0f)
    }

    // Suave balanceo del muñeco cuando está colgado
    val infiniteTransition = rememberInfiniteTransition(label = "stickmanSway")
    val swayAngle by infiniteTransition.animateFloat(
        initialValue = if (wrongAttempts > 0) -3.5f else 0f,
        targetValue = if (wrongAttempts > 0) 3.5f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sway"
    )

    // Temblor de pánico si le queda 1 vida
    val panicShake by infiniteTransition.animateFloat(
        initialValue = if (wrongAttempts == maxLives - 1) -2f else 0f,
        targetValue = if (wrongAttempts == maxLives - 1) 2f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(80, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "panicShake"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val strokeWidth = (w * 0.024f).coerceIn(3.5f, 8f)

        // Estructura de la horca (Estilo boceto a mano)
        val baseX1 = w * 0.10f
        val baseX2 = w * 0.55f
        val baseY = h * 0.88f
        val poleX = w * 0.28f
        val topY = h * 0.12f
        val beamEndX = w * 0.72f
        val ropeY = h * 0.26f

        // 1. Base doble para efecto boceto
        drawLine(
            color = theme.gallowsColor,
            start = Offset(baseX1, baseY),
            end = Offset(baseX2, baseY),
            strokeWidth = strokeWidth * 1.4f,
            cap = StrokeCap.Round
        )

        // 2. Poste vertical
        drawLine(
            color = theme.gallowsColor,
            start = Offset(poleX, baseY),
            end = Offset(poleX, topY),
            strokeWidth = strokeWidth * 1.3f,
            cap = StrokeCap.Round
        )

        // 3. Viga superior
        drawLine(
            color = theme.gallowsColor,
            start = Offset(poleX, topY),
            end = Offset(beamEndX, topY),
            strokeWidth = strokeWidth * 1.2f,
            cap = StrokeCap.Round
        )

        // 4. Soporte diagonal de madera/boceto
        drawLine(
            color = theme.gallowsColor,
            start = Offset(poleX, topY + (h * 0.14f)),
            end = Offset(poleX + (w * 0.14f), topY),
            strokeWidth = strokeWidth * 0.9f,
            cap = StrokeCap.Round
        )

        // 5. Cuerda colgante
        drawLine(
            color = theme.ropeColor,
            start = Offset(beamEndX, topY),
            end = Offset(beamEndX, ropeY),
            strokeWidth = strokeWidth * 0.85f,
            cap = StrokeCap.Round
        )

        // Puntos de anclaje del Stickman
        val headRadius = w * 0.095f
        val pivotPoint = Offset(beamEndX, ropeY)

        rotate(degrees = swayAngle + panicShake, pivot = pivotPoint) {
            val headCenter = Offset(beamEndX, ropeY + headRadius)
            val torsoStart = Offset(beamEndX, headCenter.y + headRadius)
            val torsoEnd = Offset(beamEndX, torsoStart.y + (h * 0.22f))
            val armY = torsoStart.y + (h * 0.05f)
            val armLengthX = w * 0.14f
            val armLengthY = h * 0.09f
            val legLengthX = w * 0.13f
            val legLengthY = h * 0.16f

            // 1. Cabeza y cara del Stickman
            if (headProgress.value > 0f) {
                val sweepAngle = 360f * headProgress.value
                drawArc(
                    color = theme.bodyColor,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(headCenter.x - headRadius, headCenter.y - headRadius),
                    size = Size(headRadius * 2, headRadius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Expresiones faciales del Stickman según el peligro
                if (headProgress.value >= 0.9f) {
                    val eyeXOffset = headRadius * 0.35f
                    val eyeY = headCenter.y - (headRadius * 0.12f)
                    val isDead = wrongAttempts >= maxLives
                    val isTerrified = wrongAttempts == maxLives - 1
                    val isNervous = wrongAttempts in 3..4

                    when {
                        isDead -> {
                            // Ojos en X (Derrota / Muerto)
                            drawXEye(Offset(headCenter.x - eyeXOffset, eyeY), headRadius * 0.22f, theme.accentColor, strokeWidth * 0.8f)
                            drawXEye(Offset(headCenter.x + eyeXOffset, eyeY), headRadius * 0.22f, theme.accentColor, strokeWidth * 0.8f)
                            // Lengua cómica afuera
                            drawLine(theme.accentColor, Offset(headCenter.x, headCenter.y + headRadius * 0.3f), Offset(headCenter.x + 6f, headCenter.y + headRadius * 0.6f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                            // Halo flotante angelical arriba
                            drawArc(
                                color = Color(0xFFFBBF24),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = Offset(headCenter.x - headRadius * 0.8f, headCenter.y - headRadius * 1.6f),
                                size = Size(headRadius * 1.6f, headRadius * 0.5f),
                                style = Stroke(width = strokeWidth * 0.8f)
                            )
                        }
                        isTerrified -> {
                            // Ojos de pánico (> <)
                            drawTerrifiedEye(Offset(headCenter.x - eyeXOffset, eyeY), headRadius * 0.2f, theme.bodyColor, strokeWidth)
                            drawTerrifiedEye(Offset(headCenter.x + eyeXOffset, eyeY), headRadius * 0.2f, theme.bodyColor, strokeWidth, isLeft = false)
                            // Boca abierta temblando
                            drawArc(
                                color = theme.accentColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = true,
                                topLeft = Offset(headCenter.x - 5f, headCenter.y + headRadius * 0.25f),
                                size = Size(10f, 12f)
                            )
                            // Gotitas de sudor de pánico
                            drawSweatDrop(Offset(headCenter.x + headRadius * 1.1f, headCenter.y - headRadius * 0.2f), headRadius * 0.25f, Color(0xFF38BDF8))
                        }
                        isNervous -> {
                            // Ojos abiertos y boca torcida
                            drawCircle(theme.bodyColor, radius = strokeWidth * 0.6f, center = Offset(headCenter.x - eyeXOffset, eyeY))
                            drawCircle(theme.bodyColor, radius = strokeWidth * 0.6f, center = Offset(headCenter.x + eyeXOffset, eyeY))
                            // Boca en zigzag
                            drawLine(theme.bodyColor, Offset(headCenter.x - eyeXOffset * 0.6f, headCenter.y + headRadius * 0.35f), Offset(headCenter.x, headCenter.y + headRadius * 0.45f), strokeWidth = strokeWidth * 0.8f, cap = StrokeCap.Round)
                            drawLine(theme.bodyColor, Offset(headCenter.x, headCenter.y + headRadius * 0.45f), Offset(headCenter.x + eyeXOffset * 0.6f, headCenter.y + headRadius * 0.3f), strokeWidth = strokeWidth * 0.8f, cap = StrokeCap.Round)
                            // Gota de sudor
                            drawSweatDrop(Offset(headCenter.x + headRadius * 0.9f, headCenter.y - headRadius * 0.1f), headRadius * 0.2f, Color(0xFF38BDF8))
                        }
                        else -> {
                            // Cara normal / expectante
                            drawCircle(theme.bodyColor, radius = strokeWidth * 0.55f, center = Offset(headCenter.x - eyeXOffset, eyeY))
                            drawCircle(theme.bodyColor, radius = strokeWidth * 0.55f, center = Offset(headCenter.x + eyeXOffset, eyeY))
                            // Línea de boca simple
                            drawLine(theme.bodyColor, Offset(headCenter.x - eyeXOffset * 0.5f, headCenter.y + headRadius * 0.35f), Offset(headCenter.x + eyeXOffset * 0.5f, headCenter.y + headRadius * 0.35f), strokeWidth = strokeWidth * 0.8f, cap = StrokeCap.Round)
                        }
                    }
                }
            }

            // 2. Torso
            if (torsoProgress.value > 0f) {
                val currentTorsoEnd = Offset(
                    torsoStart.x,
                    torsoStart.y + ((torsoEnd.y - torsoStart.y) * torsoProgress.value)
                )
                drawLine(
                    color = theme.bodyColor,
                    start = torsoStart,
                    end = currentTorsoEnd,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            // 3. Brazo Izquierdo (Moviéndose con expresividad)
            if (leftArmProgress.value > 0f) {
                val armAngleMod = if (wrongAttempts >= 4) -10f else 0f
                val endX = beamEndX - (armLengthX * leftArmProgress.value)
                val endY = armY + (armLengthY * leftArmProgress.value) + armAngleMod
                drawLine(
                    color = theme.bodyColor,
                    start = Offset(beamEndX, armY),
                    end = Offset(endX, endY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            // 4. Brazo Derecho
            if (rightArmProgress.value > 0f) {
                val armAngleMod = if (wrongAttempts >= 4) -10f else 0f
                val endX = beamEndX + (armLengthX * rightArmProgress.value)
                val endY = armY + (armLengthY * rightArmProgress.value) + armAngleMod
                drawLine(
                    color = theme.bodyColor,
                    start = Offset(beamEndX, armY),
                    end = Offset(endX, endY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            // 5. Pierna Izquierda
            if (leftLegProgress.value > 0f) {
                val endX = beamEndX - (legLengthX * leftLegProgress.value)
                val endY = torsoEnd.y + (legLengthY * leftLegProgress.value)
                drawLine(
                    color = theme.bodyColor,
                    start = torsoEnd,
                    end = Offset(endX, endY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            // 6. Pierna Derecha
            if (rightLegProgress.value > 0f) {
                val endX = beamEndX + (legLengthX * rightLegProgress.value)
                val endY = torsoEnd.y + (legLengthY * rightLegProgress.value)
                drawLine(
                    color = theme.bodyColor,
                    start = torsoEnd,
                    end = Offset(endX, endY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

private fun DrawScope.drawXEye(center: Offset, size: Float, color: Color, strokeWidth: Float) {
    drawLine(color, Offset(center.x - size, center.y - size), Offset(center.x + size, center.y + size), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    drawLine(color, Offset(center.x - size, center.y + size), Offset(center.x + size, center.y - size), strokeWidth = strokeWidth, cap = StrokeCap.Round)
}

private fun DrawScope.drawTerrifiedEye(center: Offset, size: Float, color: Color, strokeWidth: Float, isLeft: Boolean = true) {
    val dir = if (isLeft) 1 else -1
    drawLine(color, Offset(center.x - size * dir, center.y - size), Offset(center.x + size * dir, center.y), strokeWidth = strokeWidth * 0.8f, cap = StrokeCap.Round)
    drawLine(color, Offset(center.x + size * dir, center.y), Offset(center.x - size * dir, center.y + size), strokeWidth = strokeWidth * 0.8f, cap = StrokeCap.Round)
}

private fun DrawScope.drawSweatDrop(center: Offset, size: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - size)
        cubicTo(center.x + size * 0.8f, center.y, center.x + size * 0.8f, center.y + size, center.x, center.y + size)
        cubicTo(center.x - size * 0.8f, center.y + size, center.x - size * 0.8f, center.y, center.x, center.y - size)
        close()
    }
    drawPath(path, color = color)
}
