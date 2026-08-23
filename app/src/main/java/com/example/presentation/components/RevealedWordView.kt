package com.example.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Componente que muestra las casillas de la palabra secreta estilo fichas de papel de libreta.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RevealedWordView(
    secretWord: String,
    revealedLetters: Set<Char>,
    isLost: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    FlowRow(
        modifier = modifier
            .testTag("revealed_word_view")
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        secretWord.uppercase().forEachIndexed { index, char ->
            val isRevealed = char in revealedLetters
            val isMissingOnLoss = isLost && !isRevealed

            val cardBg = when {
                isMissingOnLoss -> if (isDark) Color(0xFF7F1D1D) else Color(0xFFFEE2E2)
                isRevealed -> if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE)
                else -> if (isDark) Color(0xFF1F2937) else Color(0xFFFFFDF8)
            }

            val cardBorder = when {
                isMissingOnLoss -> Color(0xFFDC2626)
                isRevealed -> Color(0xFF2563EB)
                else -> if (isDark) Color(0xFF475569) else Color(0xFF1E293B)
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .width(34.dp)
                    .height(46.dp)
                    .shadow(2.dp, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(cardBg)
                    .border(1.8.dp, cardBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = isRevealed || isLost,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "letterAnim_$index"
                ) { shouldShow ->
                    if (shouldShow) {
                        Text(
                            text = char.toString(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = when {
                                isMissingOnLoss -> if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626)
                                isRevealed -> if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    } else {
                        // Trazo de lápiz subrayado para letra no descubierta
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 7.dp)
                                .width(18.dp)
                                .height(3.5.dp)
                                .background(
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF334155),
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}
