package com.example.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.random.Random

/**
 * Efecto de sacudida (Shake Animation) para cuando se falla una letra o vida.
 */
fun Modifier.shake(trigger: Int): Modifier = composed {
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger > 0) {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 350
                    0f at 0
                    -16f at 50
                    14f at 100
                    -12f at 150
                    10f at 200
                    -6f at 250
                    3f at 300
                    0f at 350
                }
            )
        }
    }

    graphicsLayer {
        translationX = shakeOffset.value
    }
}

private data class ConfettiParticle(
    val xRatio: Float,
    val initialY: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    val rotationSpeed: Float
)

/**
 * Animación de confeti celebratorio para victoria en Jetpack Compose Canvas.
 */
@Composable
fun ConfettiCelebration(modifier: Modifier = Modifier) {
    val particles = remember {
        val colors = listOf(
            Color(0xFFFF1744), Color(0xFFFFEA00), Color(0xFF00E676),
            Color(0xFF2979FF), Color(0xFFFF9100), Color(0xFFE040FB),
            Color(0xFF00E5FF)
        )
        List(60) {
            ConfettiParticle(
                xRatio = Random.nextFloat(),
                initialY = -Random.nextFloat() * 200f,
                speed = Random.nextFloat() * 400f + 250f,
                size = Random.nextFloat() * 14f + 8f,
                color = colors[Random.nextInt(colors.size)],
                rotationSpeed = Random.nextFloat() * 360f + 180f
            )
        }
    }

    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(3500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val progress = animProgress.value

        particles.forEach { p ->
            val curY = ((p.initialY + (h + 300f) * progress * (p.speed / 350f)) % (h + 300f)) - 50f
            val curX = (p.xRatio * w) + (kotlin.math.sin(progress * 8f + p.xRatio * 10f) * 30f)
            val currentRotation = progress * p.rotationSpeed

            rotate(degrees = currentRotation, pivot = Offset(curX, curY)) {
                drawRect(
                    color = p.color,
                    topLeft = Offset(curX, curY),
                    size = Size(p.size, p.size * 0.6f)
                )
            }
        }
    }
}
