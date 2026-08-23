package com.example.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Indicador visual de vidas restantes estilo boceto.
 */
@Composable
fun LivesIndicator(
    errors: Int,
    maxErrors: Int = 6,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val remainingLives = (maxErrors - errors).coerceAtLeast(0)

    val heartColor by animateColorAsState(
        targetValue = when {
            remainingLives <= 1 -> Color(0xFFEF4444)
            remainingLives <= 3 -> Color(0xFFF59E0B)
            else -> Color(0xFFE11D48)
        },
        label = "heartColor"
    )

    Row(
        modifier = modifier
            .testTag("lives_indicator")
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        for (i in 1..maxErrors) {
            val isAlive = i <= remainingLives
            Icon(
                imageVector = if (isAlive) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (isAlive) "Vida activa" else "Vida perdida",
                tint = if (isAlive) heartColor else if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1),
                modifier = Modifier
                    .size(24.dp)
                    .padding(horizontal = 2.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "$remainingLives/$maxErrors",
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = if (remainingLives <= 1) Color(0xFFEF4444) else if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
        )
    }
}
