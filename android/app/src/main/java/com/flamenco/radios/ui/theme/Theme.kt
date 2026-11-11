package com.flamenco.radios.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette « tablao » : rouge carmin, noir, or.
private val Carmine = Color(0xFFB3122E)
private val CarmineLight = Color(0xFFFFB3B5)
private val Gold = Color(0xFFC9A227)
private val Ink = Color(0xFF1A1012)

private val LightColors = lightColorScheme(
    primary = Carmine,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDADA),
    onPrimaryContainer = Color(0xFF40000B),
    secondary = Color(0xFF7A5900),
    secondaryContainer = Color(0xFFFFDF9E),
    tertiary = Gold,
    background = Color(0xFFFFF8F7),
    surface = Color(0xFFFFF8F7),
    surfaceVariant = Color(0xFFF4DDDC),
)

private val DarkColors = darkColorScheme(
    primary = CarmineLight,
    onPrimary = Color(0xFF680017),
    primaryContainer = Color(0xFF8F0D24),
    onPrimaryContainer = Color(0xFFFFDADA),
    secondary = Color(0xFFF0C048),
    secondaryContainer = Color(0xFF5C4200),
    tertiary = Gold,
    background = Ink,
    surface = Ink,
    surfaceVariant = Color(0xFF3A2C2D),
)

@Composable
fun FlamencoTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
