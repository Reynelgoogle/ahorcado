package com.example.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.LetterStatus

/**
 * Teclado virtual estilo Stickman / sellos de libreta adaptado al español (QWERTY con Ñ).
 */
@Composable
fun KeyboardGrid(
    guessedLetters: Set<Char>,
    secretWord: String,
    isEnabled: Boolean,
    onLetterClick: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        listOf('Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P'),
        listOf('A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', 'Ñ'),
        listOf('Z', 'X', 'C', 'V', 'B', 'N', 'M')
    )

    Column(
        modifier = modifier
            .testTag("keyboard_grid")
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEach { rowLetters ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                rowLetters.forEach { letter ->
                    val isUsed = letter in guessedLetters
                    val isCorrect = isUsed && secretWord.uppercase().contains(letter)
                    val isIncorrect = isUsed && !isCorrect

                    val status = when {
                        isCorrect -> LetterStatus.CORRECT
                        isIncorrect -> LetterStatus.INCORRECT
                        else -> LetterStatus.UNUSED
                    }

                    KeyButton(
                        letter = letter,
                        status = status,
                        isEnabled = isEnabled && status == LetterStatus.UNUSED,
                        onClick = { onLetterClick(letter) },
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyButton(
    letter: Char,
    status: LetterStatus,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.88f else 1f, label = "keyScale")

    val backgroundColor = when (status) {
        LetterStatus.CORRECT -> Color(0xFF10B981) // Verde acierto estilo sello
        LetterStatus.INCORRECT -> if (isDark) Color(0xFF2D3748).copy(alpha = 0.4f) else Color(0xFFE2E8F0).copy(alpha = 0.5f)
        LetterStatus.UNUSED -> when {
            isEnabled -> if (isDark) Color(0xFF2A3441) else Color(0xFFFFFFFF)
            else -> if (isDark) Color(0xFF1F2937).copy(alpha = 0.5f) else Color(0xFFF1F5F9).copy(alpha = 0.7f)
        }
    }

    val contentColor = when (status) {
        LetterStatus.CORRECT -> Color.White
        LetterStatus.INCORRECT -> if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
        LetterStatus.UNUSED -> when {
            isEnabled -> if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
            else -> if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
        }
    }

    val borderColor = when (status) {
        LetterStatus.CORRECT -> Color(0xFF059669)
        LetterStatus.INCORRECT -> Color.Transparent
        LetterStatus.UNUSED -> when {
            isEnabled -> if (isDark) Color(0xFF475569) else Color(0xFF1E293B)
            else -> Color.Transparent
        }
    }

    Box(
        modifier = modifier
            .testTag("key_$letter")
            .scale(scale)
            .width(33.dp)
            .height(46.dp)
            .shadow(
                elevation = if (status == LetterStatus.UNUSED && isEnabled) (if (isPressed) 1.dp else 3.dp) else 0.dp,
                shape = RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = if (status == LetterStatus.UNUSED && isEnabled) 1.8.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(
                enabled = isEnabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter.toString(),
            color = contentColor,
            fontSize = 17.sp,
            fontWeight = if (status == LetterStatus.CORRECT || (status == LetterStatus.UNUSED && isEnabled)) FontWeight.ExtraBold else FontWeight.Normal
        )
    }
}
