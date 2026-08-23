package com.example.presentation.word

import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.presentation.components.DoodleButton
import com.example.presentation.components.DoodleCard
import com.example.presentation.components.doodleNotebookBackground

/**
 * Pantalla donde el Anfitrión elige o escribe la palabra secreta con temática de libreta de bocetos.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WordSelectionScreen(
    onNavigateBack: () -> Unit,
    onGameStarted: () -> Unit,
    viewModel: WordSelectionViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val charcoalBorder = if (isDark) Color(0xFF64748B) else Color(0xFF1E293B)

    Scaffold(
        modifier = modifier.testTag("word_selection_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(32.dp).border(1.5.dp, charcoalBorder, CircleShape)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Create,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Palabra Secreta del Boceto",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .doodleNotebookBackground(isDark)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tarjeta de Instrucciones estilo Nota Adhesiva
            DoodleCard(
                modifier = Modifier.fillMaxWidth(),
                tapeColor = Color(0xFFFDE047),
                borderColor = charcoalBorder,
                backgroundColor = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.4f) else Color(0xFFFEF3C7)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "📝 Elige la palabra para el desafío",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color(0xFFBFDBFE) else Color(0xFF78350F)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Selecciona una categoría rápida o escribe una palabra personalizada para que los demás Stickmans adivinen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFF93C5FD) else Color(0xFF92400E)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 1. Selector de Categorías Temáticas
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Categorías Temáticas",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.categories) { cat ->
                    val isSelected = uiState.selectedCategory?.id == cat.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectCategory(cat) },
                        label = { Text("${cat.icon} ${cat.name}", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = charcoalBorder
                        )
                    )
                }
            }

            // Palabras sugeridas de la categoría
            uiState.selectedCategory?.let { currentCategory ->
                Spacer(modifier = Modifier.height(12.dp))
                DoodleCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = charcoalBorder,
                    backgroundColor = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "💡 Palabras de ${currentCategory.name}:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            currentCategory.words.forEach { item ->
                                val isSelected = uiState.wordInput.equals(item.word, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.selectCategorizedWord(item, currentCategory.name) },
                                    label = { Text(item.word, fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFD97706),
                                        selectedLabelColor = Color.White
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = charcoalBorder
                                    ),
                                    modifier = Modifier.testTag("sample_word_${item.word}")
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Campo de Palabra Secreta
            OutlinedTextField(
                value = uiState.wordInput,
                onValueChange = { viewModel.onWordChange(it) },
                label = { Text("Palabra Secreta", fontWeight = FontWeight.Bold) },
                placeholder = { Text("Ej: ASTRONAUTA") },
                isError = uiState.errorMessage != null,
                supportingText = {
                    if (uiState.errorMessage != null) {
                        Text(text = uiState.errorMessage!!, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    } else {
                        Text(text = "📏 ${uiState.wordInput.trim().length} letras (Mínimo 5 letras)", fontWeight = FontWeight.SemiBold)
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = charcoalBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("secret_word_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Campo de Pista Opcional
            OutlinedTextField(
                value = uiState.hintInput,
                onValueChange = { viewModel.onHintChange(it) },
                label = { Text("Pista o Clave (Opcional)", fontWeight = FontWeight.Bold) },
                placeholder = { Text("Ej: Viaja más allá de la atmósfera terrestre") },
                leadingIcon = {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFF59E0B))
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { viewModel.confirmWord(onGameStarted) }
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = charcoalBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hint_input")
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Botón Comenzar Partida
            DoodleButton(
                text = "¡Comenzar Partida!",
                icon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White) },
                containerColor = MaterialTheme.colorScheme.primary,
                borderColor = charcoalBorder,
                enabled = uiState.wordInput.trim().length >= 5,
                onClick = { viewModel.confirmWord(onGameStarted) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("start_game_button")
            )
        }
    }
}
