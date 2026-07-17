package com.musfit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private fun musFitColorScheme(colors: MusFitColors, dark: Boolean) = if (dark) {
    darkColorScheme(
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
        // MusFitColors.outline is the hairline divider token — too faint for
        // component borders (text fields, chips), which keep a visible gray.
        outline = colors.onSurfaceVariant,
        outlineVariant = colors.outline,
    )
} else {
    lightColorScheme(
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
        outline = colors.onSurfaceVariant,
        outlineVariant = colors.outline,
    )
}

@Composable
fun MusFitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) darkMusFitColors else lightMusFitColors
    CompositionLocalProvider(
        LocalMusFitColors provides colors,
        LocalMusFitSpacing provides MusFitSpacing(),
    ) {
        MaterialTheme(
            colorScheme = musFitColorScheme(colors, darkTheme),
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
