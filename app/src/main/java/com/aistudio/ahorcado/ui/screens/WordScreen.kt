package com.aistudio.ahorcado.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import com.aistudio.ahorcado.data.WordBank
import com.aistudio.ahorcado.domain.logic.HangmanEngine
import com.aistudio.ahorcado.domain.model.NetMode
import com.aistudio.ahorcado.ui.components.DoodleButton
import com.aistudio.ahorcado.ui.components.DoodleTitle
import com.aistudio.ahorcado.ui.components.SectionLabel
import com.aistudio.ahorcado.ui.theme.DoodleColors

/**
 * El creador (anfitrión o jugador 1 en modo local) elige la palabra secreta.
 * Nadie más ve esta pantalla.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordScreen() {
    BackHandler { GameRepository.leaveToMenu() }
    val mode by GameRepository.modeFlow.collectAsState()
    val me by GameRepository.localPlayer.collectAsState()

    var word by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DoodleColors.Navy)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DoodleTitle(text = "PALABRA", fontSizeSp = 36f)
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (mode == NetMode.OFFLINE) {
                "¡Que nadie mire! Escribe la palabra secreta."
            } else {
                "${me.name}, elige la palabra.\nLos demás verán solo las casillas."
            },
            color = DoodleColors.Cream.copy(alpha = 0.8f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))

        SectionLabel("Categoría")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CategoryChip("General", "🎲", category == "General") { category = "General" }
            WordBank.categories.forEach { c ->
                CategoryChip(c.name, c.icon, category == c.name) { category = c.name }
            }
        }
        Spacer(Modifier.height(20.dp))

        SectionLabel("Palabra secreta")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DoodleTextField(
                value = word,
                onValueChange = {
                    word = it.filter { ch -> ch.isLetter() || ch == ' ' }.take(15)
                    error = null
                },
                placeholder = "Ej. MARIPOSA",
                modifier = Modifier.weight(1f),
            )
            DoodleButton(
                text = "🎲",
                onClick = {
                    val pick = WordBank.random()
                    word = pick.word
                    hint = pick.hint
                    category = pick.category
                    error = null
                },
            )
        }
        Spacer(Modifier.height(12.dp))
        SectionLabel("Pista (opcional)")
        DoodleTextField(
            value = hint,
            onValueChange = { if (it.length <= 120) hint = it },
            placeholder = "Ej. Vuela de noche",
            modifier = Modifier.fillMaxWidth(),
        )

        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                text = it,
                color = DoodleColors.Brick,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(24.dp))
        DoodleButton(
            text = "🚀  ¡A jugar!",
            onClick = {
                when (val r = GameRepository.pickWord(word, category, hint)) {
                    is HangmanEngine.WordValidation.Ok -> Unit // el repo navega
                    is HangmanEngine.WordValidation.Err -> error = r.reason
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Entre 4 y 15 letras. Las tildes se quitan solas.",
            color = DoodleColors.Cream.copy(alpha = 0.55f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CategoryChip(
    name: String,
    icon: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Text(
        text = "$icon $name",
        modifier = Modifier
            .clip(shape)
            .background(if (selected) DoodleColors.Wood else DoodleColors.Cream.copy(alpha = 0.18f))
            .border(BorderStroke(2.5.dp, DoodleColors.Ink), shape)
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = if (selected) DoodleColors.Ink else DoodleColors.Cream,
    )
}

@Composable
fun DoodleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = DoodleColors.Ink.copy(alpha = 0.45f)) },
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
        modifier = modifier,
    )
}
