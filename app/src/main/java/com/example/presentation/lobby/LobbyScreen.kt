package com.example.presentation.lobby

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.network.NearbyConnectionState
import com.example.presentation.components.DoodleButton
import com.example.presentation.components.DoodleCard
import com.example.presentation.components.PlayerChip
import com.example.presentation.components.StickmanAvatar
import com.example.presentation.components.StickmanHeroIllustration
import com.example.presentation.components.doodleNotebookBackground
import com.example.utils.rememberNearbyPermissionsState

/**
 * Pantalla principal del Lobby con diseño estilizado de Libreta de Bocetos y Stickman.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(
    onNavigateToWordSelection: () -> Unit,
    onNavigateToGame: () -> Unit,
    onNavigateToDemo: () -> Unit = {},
    viewModel: LobbyViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val discoveredHosts by viewModel.discoveredHosts.collectAsStateWithLifecycle()
    val players by viewModel.players.collectAsStateWithLifecycle()
    val localPlayer by viewModel.localPlayer.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val permissionState = rememberNearbyPermissionsState()
    val scrollState = rememberScrollState()

    val charcoalBorder = if (isDark) Color(0xFF64748B) else Color(0xFF1E293B)

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        modifier = modifier.testTag("lobby_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp).border(1.5.dp, charcoalBorder, CircleShape)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Create,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Stickman Ahorcado",
                                fontWeight = FontWeight.Black,
                                fontSize = 19.sp,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Multijugador P2P & Cuaderno",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .doodleNotebookBackground(isDark)
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Banner con Mascota Stickman y Bocadillo de Texto
            DoodleCard(
                modifier = Modifier.fillMaxWidth(),
                tapeColor = Color(0xFFFDE047),
                borderColor = charcoalBorder,
                backgroundColor = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFDF8)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StickmanHeroIllustration(
                        modifier = Modifier.size(95.dp, 105.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0xFF0F172A) else Color(0xFFFEF3C7),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, charcoalBorder, RoundedCornerShape(12.dp))
                        ) {
                            Text(
                                text = "💬 \"¡Adivina la palabra antes de que se complete el dibujo del Stickman!\"",
                                modifier = Modifier.padding(10.dp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFFDE047) else Color(0xFF78350F),
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sección de Perfil del Jugador
            DoodleCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("player_name_card"),
                borderColor = charcoalBorder,
                backgroundColor = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StickmanAvatar(
                                colorIndex = localPlayer.colorIndex,
                                name = uiState.playerName,
                                size = 38.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Tu Avatar de Boceto",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Personaliza tu nombre de artista",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = uiState.playerName,
                        onValueChange = { viewModel.setPlayerName(it) },
                        label = { Text("Nombre del jugador") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = charcoalBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_name_input")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Estado de Conexión y Acciones del Juego
            when (val state = connectionState) {
                is NearbyConnectionState.Idle -> {
                    Text(
                        text = "✏️ Elige cómo quieres jugar",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155),
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DoodleButton(
                            text = "Crear Sala",
                            icon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color.White) },
                            containerColor = MaterialTheme.colorScheme.primary,
                            borderColor = charcoalBorder,
                            onClick = {
                                if (permissionState.hasPermissions) {
                                    viewModel.startHosting(context)
                                } else {
                                    permissionState.requestPermissions()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("create_room_button")
                        )

                        DoodleButton(
                            text = "Unirme",
                            icon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                            containerColor = Color(0xFF0D9488),
                            borderColor = charcoalBorder,
                            onClick = {
                                if (permissionState.hasPermissions) {
                                    viewModel.startDiscovery(context)
                                } else {
                                    permissionState.requestPermissions()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("search_rooms_button")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    DoodleButton(
                        text = "Partida Rápida / Individual",
                        icon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White) },
                        containerColor = Color(0xFFD97706),
                        borderColor = charcoalBorder,
                        onClick = {
                            viewModel.startSoloPractice {
                                onNavigateToWordSelection()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_play_button")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    DoodleCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("demo_mode_button"),
                        tapeColor = Color(0xFFA78BFA),
                        borderColor = charcoalBorder,
                        backgroundColor = if (isDark) Color(0xFF312E81) else Color(0xFFEDE9FE)
                    ) {
                        Surface(
                            color = Color.Transparent,
                            onClick = onNavigateToDemo,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF7C3AED),
                                        modifier = Modifier.size(36.dp).border(1.2.dp, charcoalBorder, CircleShape)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.SportsEsports,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "🎮 Modo Demo & Pruebas",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isDark) Color(0xFFEDE9FE) else Color(0xFF4C1D95)
                                        )
                                        Text(
                                            text = "Probar muñeco, trazos, sonidos y turnos",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isDark) Color(0xFFC4B5FD) else Color(0xFF6D28D9)
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFEDE9FE) else Color(0xFF4C1D95)
                                )
                            }
                        }
                    }
                }

                is NearbyConnectionState.Advertising -> {
                    DoodleCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hosting_card"),
                        borderColor = charcoalBorder,
                        backgroundColor = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color(0xFFDBEAFE)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Sala abierta: ${state.localName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Esperando que se unan los otros Stickmans (Máx. 4)...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { viewModel.resetToIdle() },
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, charcoalBorder)
                            ) {
                                Text("Cancelar Sala", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                is NearbyConnectionState.Discovering -> {
                    DoodleCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("discovering_card"),
                        borderColor = charcoalBorder,
                        backgroundColor = if (isDark) Color(0xFF0F172A) else Color(0xFFFEF3C7)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.5.dp,
                                    color = Color(0xFFD97706)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Buscando salas de dibujo...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            if (discoveredHosts.isEmpty()) {
                                Text(
                                    text = "Asegúrate de que el Anfitrión tenga su sala abierta con Bluetooth activado.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(discoveredHosts) { host ->
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.5.dp, charcoalBorder, RoundedCornerShape(10.dp))
                                                .testTag("discovered_host_${host.endpointId}"),
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surface
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "Sala de ${host.name}",
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "ID: ${host.endpointId}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                if (host.isConnecting) {
                                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                                } else {
                                                    Button(
                                                        onClick = { viewModel.joinRoom(host) },
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.testTag("join_button_${host.endpointId}")
                                                    ) {
                                                        Text("Unirse", fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { viewModel.resetToIdle() },
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, charcoalBorder)
                            ) {
                                Text("Detener Búsqueda", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                is NearbyConnectionState.Connected -> {
                    DoodleCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("connected_status_card"),
                        borderColor = charcoalBorder,
                        backgroundColor = if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = Color(0xFF059669)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (state.isHost) "¡Eres el Anfitrión de la Sala!" else "¡Conectado a la Sala!",
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF065F46)
                                )
                                Text(
                                    text = "${players.size}/4 Stickmans listos para jugar",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857)
                                )
                            }
                        }
                    }
                }

                is NearbyConnectionState.Error -> {
                    DoodleCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = Color(0xFFDC2626),
                        backgroundColor = if (isDark) Color(0xFF7F1D1D) else Color(0xFFFEE2E2)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = state.message,
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { viewModel.resetToIdle() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Reintentar / Volver", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Lista de Jugadores en la Sala (hasta 4)
            if (players.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "👥 Stickmans en la mesa (${players.size}/4)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    if (localPlayer.isHost && players.size < 4) {
                        OutlinedButton(
                            onClick = { viewModel.addLocalPlayer() },
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, charcoalBorder),
                            modifier = Modifier.testTag("add_local_player_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Jugador", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    players.forEach { player ->
                        PlayerChip(
                            player = player,
                            isCurrentTurn = false,
                            isLocalPlayer = player.id == localPlayer.id,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botón para continuar
                if (localPlayer.isHost) {
                    DoodleButton(
                        text = "Elegir Palabra Secreta",
                        icon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        borderColor = charcoalBorder,
                        onClick = onNavigateToWordSelection,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("choose_word_button")
                    )
                } else {
                    DoodleCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = charcoalBorder,
                        backgroundColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Esperando que el Anfitrión elija la palabra...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
