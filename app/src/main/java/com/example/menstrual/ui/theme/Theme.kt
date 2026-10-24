package com.example.menstrual.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Rose400,
    secondary = Teal400,
    tertiary = Rose200,
    background = Color(0xFFFCF7F9),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Rose200,
    secondary = Teal400,
    background = Color(0xFF1A1014),
    surface = Color(0xFF241820),
    onPrimary = Color(0xFF2A1A20),
    onSecondary = Color(0xFF0C2A20)
)

@Composable
fun MenstrualTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
