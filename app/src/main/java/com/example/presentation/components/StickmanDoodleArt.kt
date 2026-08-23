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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.GameUtils

/**
 * Fondo con textura de libreta de bocetos / papel cuadriculado sutil.
 */
fun Modifier.doodleNotebookBackground(isDark: Boolean = false): Modifier = this.drawBehind {
    val lineColor = if (isDark) Color(0xFF28303A) else Color(0xFFE5DFD3)
    val step = 28.dp.toPx()
    val width = size.width
    val height = size.height

    // Líneas horizontales de cuaderno
    var y = step
    while (y < height) {
        drawLine(
            color = lineColor.copy(alpha = 0.45f),
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1f
        )
        y += step
    }

    // Margen rojo clásico de libreta a la izquierda
    val marginX = 36.dp.toPx()
    val marginColor = if (isDark) Color(0xFFE11D48).copy(alpha = 0.3f) else Color(0xFFF43F5E).copy(alpha = 0.35f)
    drawLine(
        color = marginColor,
        start = Offset(marginX, 0f),
        end = Offset(marginX, height),
        strokeWidth = 1.5f
    )
}

/**
 * Tarjeta estilo boceto / recorte de papel con bordes gruesos y cinta adhesiva opcional.
 */
@Composable
fun DoodleCard(
    modifier: Modifier = Modifier,
    tapeColor: Color? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    cornerRadius: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(4.dp, shape = RoundedCornerShape(cornerRadius), ambientColor = Color.Black.copy(alpha = 0.15f))
            .background(backgroundColor, shape = RoundedCornerShape(cornerRadius))
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(2.dp)
    ) {
        content()

        // Cinta adhesiva decorativa en la parte superior central
        if (tapeColor != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-8).dp)
                    .width(44.dp)
                    .height(14.dp)
                    .rotate(-2f)
                    .background(tapeColor.copy(alpha = 0.85f), RoundedCornerShape(2.dp))
                    .border(0.8.dp, Color.Black.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
            )
        }
    }
}

/**
 * Botón con estilo Doodle/Cómic táctil con borde marcado y efecto de rebote al pulsar.
 */
@Composable
fun DoodleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = Color.White,
    borderColor: Color = Color.Black.copy(alpha = 0.8f),
    icon: (@Composable () -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed) 0.96f else 1f

    Surface(
        modifier = modifier
            .scale(scale)
            .height(52.dp)
            .border(2.dp, if (enabled) borderColor else borderColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .shadow(if (isPressed) 1.dp else 4.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = if (enabled) containerColor else containerColor.copy(alpha = 0.4f),
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = if (enabled) contentColor else contentColor.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Avatar Stickman dibujado con accesorios personalizados (Gorra, Lentes, Corona, etc.)
 */
@Composable
fun StickmanAvatar(
    colorIndex: Int,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val accentColor = GameUtils.getPlayerColor(colorIndex)
    val isDark = isSystemInDarkTheme()
    val inkColor = if (isDark) Color.White else Color(0xFF1E293B)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.18f))
            .border(2.dp, accentColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.15f)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = (w * 0.08f).coerceIn(2f, 4f)

            val headCenter = Offset(w * 0.5f, h * 0.45f)
            val headRadius = w * 0.32f

            // Cabeza
            drawCircle(
                color = inkColor,
                radius = headRadius,
                center = headCenter,
                style = Stroke(width = stroke)
            )

            // Ojos según colorIndex
            val eyeY = headCenter.y - headRadius * 0.1f
            val eyeXOff = headRadius * 0.35f
            when (colorIndex % 4) {
                0 -> {
                    // Ojos redondos clásicos
                    drawCircle(inkColor, radius = stroke * 0.8f, center = Offset(headCenter.x - eyeXOff, eyeY))
                    drawCircle(inkColor, radius = stroke * 0.8f, center = Offset(headCenter.x + eyeXOff, eyeY))
                    // Sonrisa
                    drawArc(
                        color = inkColor,
                        startAngle = 20f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(headCenter.x - eyeXOff, headCenter.y - headRadius * 0.2f),
                        size = Size(eyeXOff * 2, headRadius * 0.7f),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                1 -> {
                    // Lentes cool
                    val glassesRadius = eyeXOff * 0.9f
                    drawCircle(accentColor, radius = glassesRadius, center = Offset(headCenter.x - eyeXOff, eyeY), style = Stroke(stroke * 0.9f))
                    drawCircle(accentColor, radius = glassesRadius, center = Offset(headCenter.x + eyeXOff, eyeY), style = Stroke(stroke * 0.9f))
                    drawLine(accentColor, Offset(headCenter.x - eyeXOff + glassesRadius, eyeY), Offset(headCenter.x + eyeXOff - glassesRadius, eyeY), strokeWidth = stroke * 0.9f)
                    // Sonrisa ladeada
                    drawLine(inkColor, Offset(headCenter.x - eyeXOff * 0.4f, headCenter.y + headRadius * 0.4f), Offset(headCenter.x + eyeXOff * 0.8f, headCenter.y + headRadius * 0.25f), strokeWidth = stroke, cap = StrokeCap.Round)
                }
                2 -> {
                    // Corona / Diadema
                    val crownY = headCenter.y - headRadius - (h * 0.08f)
                    val p = Path().apply {
                        moveTo(headCenter.x - eyeXOff * 1.2f, headCenter.y - headRadius * 0.8f)
                        lineTo(headCenter.x - eyeXOff * 1.2f, crownY)
                        lineTo(headCenter.x - eyeXOff * 0.5f, crownY + (h * 0.08f))
                        lineTo(headCenter.x, crownY - (h * 0.04f))
                        lineTo(headCenter.x + eyeXOff * 0.5f, crownY + (h * 0.08f))
                        lineTo(headCenter.x + eyeXOff * 1.2f, crownY)
                        lineTo(headCenter.x + eyeXOff * 1.2f, headCenter.y - headRadius * 0.8f)
                    }
                    drawPath(p, color = accentColor, style = Stroke(width = stroke * 0.9f, cap = StrokeCap.Round))
                    // Ojos de guiño
                    drawCircle(inkColor, radius = stroke * 0.8f, center = Offset(headCenter.x - eyeXOff, eyeY))
                    drawLine(inkColor, Offset(headCenter.x + eyeXOff - stroke, eyeY), Offset(headCenter.x + eyeXOff + stroke, eyeY), strokeWidth = stroke, cap = StrokeCap.Round)
                }
                else -> {
                    // Gorra hacia el lado
                    drawLine(accentColor, Offset(headCenter.x - headRadius, headCenter.y - headRadius * 0.6f), Offset(headCenter.x + headRadius * 1.3f, headCenter.y - headRadius * 0.8f), strokeWidth = stroke * 1.2f, cap = StrokeCap.Round)
                    drawCircle(inkColor, radius = stroke * 0.8f, center = Offset(headCenter.x - eyeXOff, eyeY))
                    drawCircle(inkColor, radius = stroke * 0.8f, center = Offset(headCenter.x + eyeXOff, eyeY))
                }
            }
        }
    }
}

/**
 * Stickman dinámico animado para la cabecera del Lobby y pantallas clave.
 */
@Composable
fun StickmanHeroIllustration(
    modifier: Modifier = Modifier,
    isWaving: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "stickmanHero")
    val armWave by infiniteTransition.animateFloat(
        initialValue = -25f,
        targetValue = 25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "armWave"
    )
    val bodyBounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bodyBounce"
    )

    val isDark = isSystemInDarkTheme()
    val inkColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
    val primaryColor = MaterialTheme.colorScheme.primary
    val yellowAccent = Color(0xFFFBBF24)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = (w * 0.032f).coerceIn(3.5f, 6.5f)

        val originY = h * 0.35f + bodyBounce
        val headRadius = w * 0.16f
        val headCenter = Offset(w * 0.45f, originY)
        val neckY = headCenter.y + headRadius
        val hipY = neckY + (h * 0.28f)
        val hipX = headCenter.x

        // 1. Cabeza
        drawCircle(
            color = inkColor,
            radius = headRadius,
            center = headCenter,
            style = Stroke(width = stroke)
        )

        // Cara sonriente
        val eyeX = headRadius * 0.35f
        val eyeY = headCenter.y - headRadius * 0.15f
        drawCircle(inkColor, radius = stroke * 0.7f, center = Offset(headCenter.x - eyeX, eyeY))
        drawCircle(inkColor, radius = stroke * 0.7f, center = Offset(headCenter.x + eyeX, eyeY))
        drawArc(
            color = inkColor,
            startAngle = 15f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(headCenter.x - eyeX, headCenter.y - headRadius * 0.2f),
            size = Size(eyeX * 2, headRadius * 0.7f),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )

        // 2. Torso
        drawLine(
            color = inkColor,
            start = Offset(hipX, neckY),
            end = Offset(hipX, hipY),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        // 3. Brazo Izquierdo (Sosteniendo lápiz gigante)
        val shoulderY = neckY + (h * 0.06f)
        val leftHand = Offset(hipX - (w * 0.22f), shoulderY - (h * 0.12f) + (armWave * 0.4f))
        drawLine(
            color = inkColor,
            start = Offset(hipX, shoulderY),
            end = Offset(hipX - (w * 0.12f), shoulderY - (h * 0.02f)),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = inkColor,
            start = Offset(hipX - (w * 0.12f), shoulderY - (h * 0.02f)),
            end = leftHand,
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        // Lápiz gigante de dibujo
        rotate(degrees = -35f + armWave * 0.3f, pivot = leftHand) {
            drawRect(
                color = yellowAccent,
                topLeft = Offset(leftHand.x - (w * 0.04f), leftHand.y - (h * 0.2f)),
                size = Size(w * 0.08f, h * 0.22f)
            )
            // Punta de lápiz
            val pencilPath = Path().apply {
                moveTo(leftHand.x - (w * 0.04f), leftHand.y - (h * 0.2f))
                lineTo(leftHand.x + (w * 0.04f), leftHand.y - (h * 0.2f))
                lineTo(leftHand.x, leftHand.y - (h * 0.28f))
                close()
            }
            drawPath(pencilPath, color = Color(0xFFD97706))
            // Grafito
            drawCircle(inkColor, radius = stroke * 0.8f, center = Offset(leftHand.x, leftHand.y - (h * 0.27f)))
        }

        // 4. Brazo Derecho (Saludando con onda)
        val rightElbow = Offset(hipX + (w * 0.15f), shoulderY - (h * 0.05f))
        val rightHand = Offset(rightElbow.x + (w * 0.12f), rightElbow.y - (h * 0.12f) + armWave)
        drawLine(
            color = inkColor,
            start = Offset(hipX, shoulderY),
            end = rightElbow,
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = inkColor,
            start = rightElbow,
            end = rightHand,
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        // Líneas de movimiento de saludo
        drawLine(primaryColor, Offset(rightHand.x + 8f, rightHand.y - 12f), Offset(rightHand.x + 20f, rightHand.y - 16f), strokeWidth = stroke * 0.7f, cap = StrokeCap.Round)
        drawLine(primaryColor, Offset(rightHand.x + 12f, rightHand.y), Offset(rightHand.x + 26f, rightHand.y), strokeWidth = stroke * 0.7f, cap = StrokeCap.Round)

        // 5. Piernas
        // Pierna izquierda
        drawLine(
            color = inkColor,
            start = Offset(hipX, hipY),
            end = Offset(hipX - (w * 0.14f), hipY + (h * 0.22f)),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        // Pierna derecha
        drawLine(
            color = inkColor,
            start = Offset(hipX, hipY),
            end = Offset(hipX + (w * 0.14f), hipY + (h * 0.22f)),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}
