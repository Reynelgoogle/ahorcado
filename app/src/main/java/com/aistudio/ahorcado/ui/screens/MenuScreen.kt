package com.aistudio.ahorcado.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.ahorcado.R
import com.aistudio.ahorcado.data.GameRepository
import com.aistudio.ahorcado.ui.components.DoodleButton
import com.aistudio.ahorcado.ui.components.DoodleTitle
import com.aistudio.ahorcado.ui.components.SectionLabel
import com.aistudio.ahorcado.ui.rememberNearbyPermissionRequest
import com.aistudio.ahorcado.ui.theme.DoodleColors
import androidx.compose.foundation.BorderStroke

@Composable
fun MenuScreen() {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var showLocalDialog by remember { mutableStateOf(false) }

    val createRoom = rememberNearbyPermissionRequest {
        GameRepository.hostRoom(context, name)
    }
    val findRooms = rememberNearbyPermissionRequest {
        GameRepository.discoverRooms(context, name)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DoodleColors.Navy)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Icono del juego enmarcado estilo doodle
        Box(
            modifier = Modifier
                .size(148.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(DoodleColors.NavyDeep)
                .border(BorderStroke(4.dp, DoodleColors.Ink), RoundedCornerShape(28.dp))
                .padding(6.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.icono_ahorcado),
                contentDescription = "Icono del Ahorcado",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp)),
            )
        }
        Spacer(Modifier.height(16.dp))
        DoodleTitle(text = "AHORCADO")
        Text(
            text = "multijugador local · sin internet",
            color = DoodleColors.Cream.copy(alpha = 0.85f),
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
        )
        Spacer(Modifier.height(24.dp))

        SectionLabel("Tu nombre")
        OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= 24) name = it },
            placeholder = { Text("Ej. Rey", color = DoodleColors.Ink.copy(alpha = 0.45f)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DoodleColors.Cream,
                unfocusedContainerColor = DoodleColors.Cream,
                focusedTextColor = DoodleColors.Ink,
                unfocusedTextColor = DoodleColors.Ink,
                cursorColor = DoodleColors.Ink,
                focusedBorderColor = DoodleColors.Ink,
                unfocusedBorderColor = DoodleColors.Ink,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))

        DoodleButton(
            text = "🎪  Crear sala",
            onClick = createRoom,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        DoodleButton(
            text = "🔍  Unirse a sala",
            onClick = findRooms,
            modifier = Modifier.fillMaxWidth(),
            color = DoodleColors.Cream,
        )
        Spacer(Modifier.height(12.dp))
        DoodleButton(
            text = "📱  Juego local (un teléfono)",
            onClick = { showLocalDialog = true },
            modifier = Modifier.fillMaxWidth(),
            color = DoodleColors.Leaf,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Junta a tus amigos cerca: la sala usa Bluetooth y Wi-Fi, sin internet.",
            color = DoodleColors.Cream.copy(alpha = 0.6f),
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }

    if (showLocalDialog) {
        LocalPlayersDialog(
            onDismiss = { showLocalDialog = false },
            onStart = { n1, n2 ->
                showLocalDialog = false
                GameRepository.startOfflineRoom(listOf(n1, n2))
            },
        )
    }
}

@Composable
private fun LocalPlayersDialog(onDismiss: () -> Unit, onStart: (String, String) -> Unit) {
    var n1 by remember { mutableStateOf("") }
    var n2 by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DoodleColors.NavyDeep,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(
                "Juego local",
                color = DoodleColors.Cream,
                fontWeight = FontWeight.Black,
            )
        },
        text = {
            Column {
                Text(
                    "El primer jugador crea la palabra en secreto.",
                    color = DoodleColors.Cream.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(12.dp))
                LocalNameField("Jugador 1 (crea la palabra)", n1) { n1 = it }
                Spacer(Modifier.height(8.dp))
                LocalNameField("Jugador 2 (adivina)", n2) { n2 = it }
            }
        },
        confirmButton = {
            DoodleButton(
                text = "Empezar",
                onClick = { onStart(n1.ifBlank { "Jugador 1" }, n2.ifBlank { "Jugador 2" }) },
            )
        },
        dismissButton = {
            DoodleButton(text = "Atrás", onClick = onDismiss, color = DoodleColors.Cream)
        },
    )
}

@Composable
private fun LocalNameField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 24) onChange(it) },
        label = { Text(label, color = DoodleColors.Ink.copy(alpha = 0.6f), fontSize = 13.sp) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = DoodleColors.Cream,
            unfocusedContainerColor = DoodleColors.Cream,
            focusedTextColor = DoodleColors.Ink,
            unfocusedTextColor = DoodleColors.Ink,
            cursorColor = DoodleColors.Ink,
            focusedBorderColor = DoodleColors.Ink,
            unfocusedBorderColor = DoodleColors.Ink,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}
