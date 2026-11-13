package com.example.appentregafacil.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF006B5E), onPrimary = Color.White,
    primaryContainer = Color(0xFFB6F2E1), onPrimaryContainer = Color(0xFF00201B),
    secondary = Color(0xFF8C5000), secondaryContainer = Color(0xFFFFDDB5),
    background = Color(0xFFF5FAF7), surface = Color(0xFFF5FAF7),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF73D8C0), primaryContainer = Color(0xFF005045),
    secondary = Color(0xFFFFB960), background = Color(0xFF0D1512), surface = Color(0xFF0D1512),
)

@Composable
fun AppEntregaFacilTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, typography = Typography, content = content)
}
