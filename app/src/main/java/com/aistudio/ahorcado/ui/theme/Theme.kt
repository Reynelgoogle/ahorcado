package com.aistudio.ahorcado.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

/**
 * Sistema visual "Doodle" extraído del icono del juego:
 * fondo azul marino profundo, madera naranja, fichas crema,
 * contornos negros gruesos y tipografía redondeada en negrita.
 */
object DoodleColors {
    /** Azul marino del fondo del icono. */
    val Navy = Color(0xFF1B2A5E)

    /** Azul marino más profundo para degradados. */
    val NavyDeep = Color(0xFF121D45)

    /** Naranja madera de la horca. */
    val Wood = Color(0xFFE8963C)

    /** Naranja madera oscuro para sombras. */
    val WoodDark = Color(0xFFC97A24)

    /** Crema de las fichas de letras. */
    val Cream = Color(0xFFF7EFD8)

    /** Crema oscuro para bordes/sombras de fichas. */
    val CreamDark = Color(0xFFE2D3AC)

    /** Tinta: contornos gruesos estilo doodle. */
    val Ink = Color(0xFF14100C)

    /** Blanco del muñeco. */
    val Paper = Color(0xFFFFFFFF)

    /** Verde acierto (apagado, dentro de la paleta). */
    val Leaf = Color(0xFF7FB069)

    /** Rojo error (apagado, dentro de la paleta). */
    val Brick = Color(0xFFD1604D)

    /** Dorado para la corona del ganador. */
    val Gold = Color(0xFFF2C14E)

    /** Colores de jugador (fichas). */
    val PlayerColors = listOf(
        Color(0xFFE8963C), // naranja
        Color(0xFF7FB069), // verde
        Color(0xFF6FA8DC), // azul claro
        Color(0xFFC27BA0), // rosado
    )
}

private val DoodleScheme = darkColorScheme(
    primary = DoodleColors.Wood,
    onPrimary = DoodleColors.Ink,
    secondary = DoodleColors.Cream,
    onSecondary = DoodleColors.Ink,
    tertiary = DoodleColors.Leaf,
    background = DoodleColors.Navy,
    onBackground = DoodleColors.Paper,
    surface = DoodleColors.NavyDeep,
    onSurface = DoodleColors.Paper,
    surfaceVariant = Color(0xFF24346E),
    onSurfaceVariant = DoodleColors.Cream,
    error = DoodleColors.Brick,
)

private val DoodleTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 40.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 26.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 15.sp,
    ),
)

private val DoodleShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
)

@Composable
fun AhorcadoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DoodleScheme,
        typography = DoodleTypography,
        shapes = DoodleShapes,
        content = content,
    )
}
