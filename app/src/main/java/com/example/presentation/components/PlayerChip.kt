package com.example.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Player
import com.example.utils.GameUtils

/**
 * Chip visual que representa un jugador con su Avatar Stickman personalizado.
 */
@Composable
fun PlayerChip(
    player: Player,
    isCurrentTurn: Boolean,
    isLocalPlayer: Boolean,
    modifier: Modifier = Modifier
) {
    val playerColor = GameUtils.getPlayerColor(player.colorIndex)
    val isDark = isSystemInDarkTheme()

    val borderColor by animateColorAsState(
        targetValue = if (isCurrentTurn) playerColor else if (isDark) Color(0xFF475569) else Color(0xFF1E293B),
        label = "borderColor"
    )

    Surface(
        modifier = modifier
            .testTag("player_chip_${player.id}")
            .shadow(if (isCurrentTurn) 4.dp else 1.dp, RoundedCornerShape(14.dp))
            .border(
                width = if (isCurrentTurn) 2.2.dp else 1.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        color = if (isCurrentTurn) {
            if (isDark) playerColor.copy(alpha = 0.22f) else playerColor.copy(alpha = 0.12f)
        } else {
            if (isDark) Color(0xFF1E293B) else Color(0xFFFFFDF8)
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Stickman dibujado
            StickmanAvatar(
                colorIndex = player.colorIndex,
                name = player.name,
                size = 32.dp
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isLocalPlayer) "${player.name} (Tú)" else player.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isCurrentTurn) FontWeight.ExtraBold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (player.isHost) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Anfitrión",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (isCurrentTurn) {
                    Text(
                        text = "✏️ Su Turno",
                        fontSize = 11.sp,
                        color = playerColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
