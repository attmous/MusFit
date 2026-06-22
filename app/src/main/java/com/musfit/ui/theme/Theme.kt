package com.musfit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private fun musFitColorScheme(colors: MusFitColors) = lightColorScheme(
    primary = colors.brand,
    onPrimary = colors.onBrand,
    secondary = colors.accent,
    onSecondary = colors.onAccent,
    secondaryContainer = colors.accentContainer,
    onSecondaryContainer = colors.onAccentContainer,
    background = colors.background,
    onBackground = colors.onSurface,
    surface = colors.surface,
    onSurface = colors.onSurface,
    surfaceVariant = colors.surfaceVariant,
    onSurfaceVariant = colors.onSurfaceVariant,
    outline = colors.outline,
)

@Composable
fun MusFitTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    // Light only in Slice 1; `darkTheme` is the seam for a later slice.
    val colors = lightMusFitColors
    CompositionLocalProvider(
        LocalMusFitColors provides colors,
        LocalMusFitSpacing provides MusFitSpacing(),
    ) {
        MaterialTheme(
            colorScheme = musFitColorScheme(colors),
            typography = MusFitTypography,
            shapes = MusFitShapes,
            content = content,
        )
    }
}

/** Ergonomic token accessors, mirroring Material 3's `MaterialTheme` object. */
object MusFitTheme {
    val colors: MusFitColors
        @Composable get() = LocalMusFitColors.current
    val spacing: MusFitSpacing
        @Composable get() = LocalMusFitSpacing.current
    val typography: Typography
        @Composable get() = MaterialTheme.typography
    val shapes: Shapes
        @Composable get() = MaterialTheme.shapes
}
