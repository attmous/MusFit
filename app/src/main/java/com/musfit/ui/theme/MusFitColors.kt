package com.musfit.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic color tokens for MusFit. Immutable; the dark-ready seam — a future
 * `darkMusFitColors` instance drops in here with no call-site changes.
 */
data class MusFitColors(
    val brand: Color,
    val onBrand: Color,
    val brandInk: Color,
    val brandGradient: List<Color>,
    val accent: Color,
    val onAccent: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val track: Color,
    val macroProtein: Color,
    val macroCarbs: Color,
    val macroFat: Color,
    val positive: Color,
    val positiveContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val water: Color,
) {
    /** Index order preserved from the legacy `MacroColors` list: [carbs, protein, fat]. */
    val macroColors: List<Color> get() = listOf(macroCarbs, macroProtein, macroFat)
}

val lightMusFitColors = MusFitColors(
    brand = Emerald,
    onBrand = CardWhite,
    brandInk = EmeraldInk,
    brandGradient = listOf(GradientLime, GradientGreen, GradientEmerald),
    accent = Coral,
    onAccent = CardWhite,
    accentContainer = CoralContainer,
    onAccentContainer = CoralInk,
    background = Cream,
    surface = CardWhite,
    surfaceVariant = WarmFill,
    onSurface = WarmInk,
    onSurfaceVariant = WarmMuted,
    outline = WarmOutline,
    track = WarmTrack,
    macroProtein = MacroProtein,
    macroCarbs = MacroCarbs,
    macroFat = MacroFat,
    positive = Emerald,
    positiveContainer = PositiveContainer,
    warning = CoralInk,
    warningContainer = CoralContainer,
    water = Water,
)

val LocalMusFitColors = staticCompositionLocalOf<MusFitColors> {
    error("MusFitColors not provided. Wrap content in MusFitTheme.")
}
