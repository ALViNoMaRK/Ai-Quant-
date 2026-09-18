package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = Color(0xFF00354E),
    primaryContainer = Color(0xFF0C4A6E),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = GoldAccent,
    onSecondary = Color(0xFF451A03),
    secondaryContainer = Color(0xFF78350F),
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = EmeraldGreen,
    background = TerminalBgDark,
    onBackground = TextPrimaryDark,
    surface = TerminalSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = TerminalSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = TerminalBorderDark,
    error = CrimsonRed
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = Color(0xFFD97706),
    onSecondary = Color.White,
    tertiary = EmeraldGreen,
    background = TerminalBgLight,
    onBackground = TextPrimaryLight,
    surface = TerminalSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextSecondaryLight,
    outline = TerminalBorderLight,
    error = CrimsonRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to professional financial terminal dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
