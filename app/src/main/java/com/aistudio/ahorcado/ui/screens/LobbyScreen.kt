package com.aistudio.ahorcado.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.aistudio.ahorcado.domain.model.FoundHost
import com.aistudio.ahorcado.domain.model.NetMode
import com.aistudio.ahorcado.domain.model.Player
import com.aistudio.ahorcado.ui.components.DoodleButton
import com.aistudio.ahorcado.ui.components.DoodleTitle
import com.aistudio.ahorcado.ui.components.PlayerChip
import com.aistudio.ahorcado.ui.components.SectionLabel
import com.aistudio.ahorcado.ui.theme.DoodleColors
import androidx.compose.foundation.BorderStroke

@Composable
fun LobbyScreen(onNavigateToWord: () -> Unit) {
    val mode by GameRepository.modeFlow.collectAsState()
    val state by GameRepository.publicState.collectAsState()
    val me by GameRepository.localPlayer.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DoodleColors.Navy)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (mode) {
            NetMode.HOST -> HostLobby(
                me = me,
                players = state.players,
                onPickWord = onNavigateToWord,
            )
            NetMode.CLIENT -> ClientLobby(
                me = me,
                players = state.players,
            )
            NetMode.OFFLINE -> {
                Text(
                    "Modo local: no hay sala.",
                    color = DoodleColors.Cream,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = { GameRepository.leaveToMenu() }) {
            Text("Salir", color = DoodleColors.Cream.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HostLobby(
    me: Player,
    players: List<Player>,
    onPickWord: () -> Unit,
) {
    DoodleTitle(text = "SALA", fontSizeSp = 36f)
    Text(
        text = "de ${me.name}",
        color = DoodleColors.Cream,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = "Tus amigos verán esta sala en sus teléfonos.\n¡Ya pueden unirse!",
        color = DoodleColors.Cream.copy(alpha = 0.75f),
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(20.dp))
    SectionLabel("Jugadores (${players.size}/4)")
    LazyColumn(
        modifier = Modifier.weight(1f, fill = false),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(players, key = { it.id }) { p ->
            PlayerRow(
                player = p,
                isMe = p.id == me.id,
                showKick = p.id != me.id,
                onKick = { GameRepository.kick(p.id) },
            )
        }
    }
    Spacer(Modifier.height(16.dp))
    DoodleButton(
        text = "✏️  Elegir palabra",
        onClick = onPickWord,
        modifier = Modifier.fillMaxWidth(),
        enabled = players.size >= 2,
    )
    if (players.size < 2) {
        Spacer(Modifier.height(8.dp))
        Text(
            "Espera al menos un jugador más…",
            color = DoodleColors.Cream.copy(alpha = 0.6f),
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun ClientLobby(
    me: Player,
    players: List<Player>,
) {
    val found by GameRepository.foundHosts.collectAsState()
    var connectingTo by remember { mutableStateOf<String?>(null) }
    val joined = players.size > 1

    if (!joined) {
        DoodleTitle(text = "SALAS", fontSizeSp = 36f)
        Spacer(Modifier.height(8.dp))
        Text(
            "Toca una sala para unirte.\nActiva Bluetooth y Wi-Fi.",
            color = DoodleColors.Cream.copy(alpha = 0.75f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        if (found.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = DoodleColors.Wood)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Buscando salas cercanas…",
                        color = DoodleColors.Cream.copy(alpha = 0.7f),
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(found, key = { it.endpointId }) { host ->
                    HostRow(
                        host = host,
                        connecting = connectingTo == host.endpointId,
                        onJoin = {
                            connectingTo = host.endpointId
                            GameRepository.joinRoom(host.endpointId)
                        },
                    )
                }            }
        }
    } else {
        DoodleTitle(text = "EN SALA", fontSizeSp = 36f)
        Spacer(Modifier.height(8.dp))
        Text(
            "Esperando a que el anfitrión elija la palabra…",
            color = DoodleColors.Cream.copy(alpha = 0.75f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        SectionLabel("Jugadores (${players.size}/4)")
        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(players, key = { it.id }) { p ->
                PlayerRow(player = p, isMe = p.id == me.id, showKick = false, onKick = {})
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                color = DoodleColors.Wood,
                modifier = Modifier.size(20.dp),
                strokeWidth = 3.dp,
            )
            Spacer(Modifier.size(8.dp))
            Text(
                "El anfitrión está eligiendo…",
                color = DoodleColors.Cream.copy(alpha = 0.7f),
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun PlayerRow(
    player: Player,
    isMe: Boolean,
    showKick: Boolean,
    onKick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PlayerChip(
            player = player,
            isTurn = false,
            isMe = isMe,
            modifier = Modifier.weight(1f),
        )
        if (showKick) {
            val shape = RoundedCornerShape(10.dp)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(shape)
                    .background(DoodleColors.Brick)
                    .border(BorderStroke(2.5.dp, DoodleColors.Ink), shape)
                    .clickable(onClick = onKick),
                contentAlignment = Alignment.Center,
            ) {
                Text("✕", fontWeight = FontWeight.Black, fontSize = 18.sp, color = DoodleColors.Ink)
            }
        }
    }
}

@Composable
private fun HostRow(
    host: FoundHost,
    connecting: Boolean,
    onJoin: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(DoodleColors.Cream)
            .border(BorderStroke(3.dp, DoodleColors.Ink), shape)
            .clickable(enabled = !connecting, onClick = onJoin)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "🎪 ${host.name}",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 17.sp,
            color = DoodleColors.Ink,
        )
        if (connecting) {
            CircularProgressIndicator(
                color = DoodleColors.WoodDark,
                modifier = Modifier.size(24.dp),
                strokeWidth = 3.dp,
            )
        } else {
            Text(
                "Unirse →",
                fontWeight = FontWeight.Black,
                color = DoodleColors.WoodDark,
            )
        }
    }
}
