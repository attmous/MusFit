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
    onAccent = CoralOnAccent,
    accentContainer = CoralContainer,
    onAccentContainer = CoralInk,
    // Health-grade clean: screens sit on the pure surface — no cream app
    // background, no white-card-on-cream chrome.
    background = CardWhite,
    surface = CardWhite,
    surfaceVariant = NeutralFill,
    onSurface = InkPrimary,
    onSurfaceVariant = InkSecondary,
    outline = Hairline,
    track = NeutralTrack,
    macroProtein = MacroProtein,
    macroCarbs = MacroCarbs,
    macroFat = MacroFat,
    positive = Emerald,
    positiveContainer = PositiveContainer,
    warning = CoralInk,
    warningContainer = CoralContainer,
    water = Water,
)

val darkMusFitColors = MusFitColors(
    brand = EmeraldBright,
    onBrand = EmeraldOnDark,
    brandInk = EmeraldInkDark,
    brandGradient = listOf(GradientLime, GradientGreen, EmeraldBright),
    accent = CoralBright,
    onAccent = CoralOnAccent,
    accentContainer = CoralContainerDark,
    onAccentContainer = CoralInkDark,
    // Mirrored rule: content sits directly on the near-black ground.
    background = DarkBg,
    surface = DarkBg,
    surfaceVariant = DarkSurfaceVariant,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    track = DarkTrack,
    macroProtein = MacroProteinDark,
    macroCarbs = MacroCarbsDark,
    macroFat = MacroFatDark,
    positive = EmeraldBright,
    positiveContainer = PositiveContainerDark,
    warning = CoralBright,
    warningContainer = CoralContainerDark,
    water = WaterDark,
)

val LocalMusFitColors = staticCompositionLocalOf<MusFitColors> {
    error("MusFitColors not provided. Wrap content in MusFitTheme.")
}
