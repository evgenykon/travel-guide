package com.evgenykon.travelguide.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF2E7D32)
private val GreenDark = Color(0xFF81C784)
private val Blue = Color(0xFF1A73E8)

private val LightColors = lightColorScheme(
    primary = Green,
    secondary = Blue,
    tertiary = Color(0xFF00695C)
)

private val DarkColors = darkColorScheme(
    primary = GreenDark,
    secondary = Color(0xFF8AB4F8),
    tertiary = Color(0xFF4DB6AC)
)

@Composable
fun TravelGuideTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
