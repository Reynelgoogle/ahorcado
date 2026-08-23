package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFFFBBF24),
    onSecondary = Color(0xFF451A03),
    secondaryContainer = Color(0xFF78350F),
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = Color(0xFFA78BFA),
    onTertiary = Color(0xFF2E1065),
    tertiaryContainer = Color(0xFF4C1D95),
    onTertiaryContainer = Color(0xFFEDE9FE),
    background = DoodlePaperDark,
    surface = DoodleSurfaceDark,
    outline = DoodlePenBorderDark,
    outlineVariant = Color(0xFF334155),
    error = CoralError
)

private val LightColorScheme = lightColorScheme(
    primary = DoodleInkPrimary,
    onPrimary = DoodleInkOnPrimary,
    primaryContainer = DoodlePaperContainer,
    onPrimaryContainer = DoodleInkOnContainer,
    secondary = DoodleMarkerAmber,
    onSecondary = DoodleMarkerOnAmber,
    secondaryContainer = DoodleMarkerContainer,
    onSecondaryContainer = DoodleMarkerOnContainer,
    tertiary = DoodlePencilPurple,
    onTertiary = DoodlePencilOnPurple,
    tertiaryContainer = DoodlePencilContainer,
    onTertiaryContainer = DoodlePencilOnContainer,
    background = DoodlePaperLight,
    surface = DoodleSurfaceLight,
    outline = DoodlePenBorderLight,
    outlineVariant = Color(0xFFCBD5E1),
    error = CoralError
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Usar tema Stickman intencional por defecto
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
