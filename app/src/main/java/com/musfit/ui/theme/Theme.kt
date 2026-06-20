package com.musfit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = MusFitGreen,
    secondary = MusFitBlue,
    surface = MusFitSurface,
    onSurface = MusFitInk,
)

@Composable
fun MusFitTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
