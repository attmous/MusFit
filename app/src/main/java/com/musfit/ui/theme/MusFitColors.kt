package com.musfit.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic color tokens for MusFit. Immutable; the light/dark instances below
 * are the single seam for the Material 3 Expressive warm palette.
 */
data class MusFitColors(
    val brand: Color,
    val onBrand: Color,
    val brandInk: Color,
    val accent: Color,
    val onAccent: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val onSurfaceFaint: Color,
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
    val waterFill: Color,
    val macroCarbsTrack: Color,
    val macroProteinTrack: Color,
    val macroFatTrack: Color,
    val destructiveContainer: Color,
    val onDestructiveContainer: Color,
    val trustChip: Color,
    val onTrustChip: Color,
    val chipSelected: Color,
    val onChipSelected: Color,
) {
    /** Index order preserved from the legacy `MacroColors` list: [carbs, protein, fat]. */
    val macroColors: List<Color> get() = listOf(macroCarbs, macroProtein, macroFat)

    /** Wavy-progress track tints on white cards, same [carbs, protein, fat] order. */
    val macroTracks: List<Color> get() = listOf(macroCarbsTrack, macroProteinTrack, macroFatTrack)
}

val lightMusFitColors = MusFitColors(
    // Brand green — Food's primary and the default filled-control color app-wide.
    brand = Green,
    onBrand = CardWhite,
    brandInk = GreenInk,
    // Interactive coral — coach, links, Today's accent. Text-safe #C2470F here;
    // the brighter BrandCoral is reserved for the chat FAB fill.
    accent = Coral,
    onAccent = CardWhite,
    accentContainer = CoralContainer,
    onAccentContainer = CoralInk,
    // M3 Expressive ground: warm cream with pure white cards — tonal separation
    // only, no borders or shadows on content.
    background = Cream,
    surface = CardWhite,
    surfaceVariant = TonalFill,
    onSurface = InkPrimary,
    onSurfaceVariant = InkSecondary,
    onSurfaceFaint = InkFaint,
    outline = Hairline,
    track = SegmentFill,
    macroProtein = MacroProtein,
    macroCarbs = MacroCarbs,
    macroFat = MacroFat,
    positive = Green,
    positiveContainer = GreenContainer,
    warning = AmberInk,
    warningContainer = AmberContainer,
    water = Water,
    waterFill = WaterFill,
    macroCarbsTrack = MacroCarbsTrack,
    macroProteinTrack = MacroProteinTrack,
    macroFatTrack = MacroFatTrack,
    destructiveContainer = DestructiveContainer,
    onDestructiveContainer = DestructiveInk,
    trustChip = TrustChipFill,
    onTrustChip = TrustChipInk,
    // Near-black "dark chip" for selected filter/sort/unit chips.
    chipSelected = ChipDark,
    onChipSelected = OnChipDark,
)

val darkMusFitColors = MusFitColors(
    brand = GreenBright,
    onBrand = GreenOnDark,
    brandInk = GreenInkDark,
    accent = CoralBright,
    onAccent = CoralOnDark,
    accentContainer = CoralContainerDark,
    onAccentContainer = CoralInkDark,
    // Mirrored rule: warm near-black ground, cards elevated one warm step so the
    // grouped-list containment survives dark mode.
    background = DarkBg,
    surface = DarkCard,
    surfaceVariant = DarkTonalFill,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    onSurfaceFaint = DarkInkFaint,
    outline = DarkHairline,
    track = DarkTrack,
    macroProtein = MacroProteinDark,
    macroCarbs = MacroCarbsDark,
    macroFat = MacroFatDark,
    positive = GreenBright,
    positiveContainer = GreenContainerDark,
    warning = AmberBright,
    warningContainer = AmberContainerDark,
    water = WaterDark,
    waterFill = WaterFillDark,
    macroCarbsTrack = MacroCarbsTrackDark,
    macroProteinTrack = MacroProteinTrackDark,
    macroFatTrack = MacroFatTrackDark,
    destructiveContainer = DestructiveContainerDark,
    onDestructiveContainer = DestructiveInkDark,
    trustChip = DarkTonalFill,
    onTrustChip = TrustChipInkDark,
    // Dark theme inverts the chip: light fill, near-black text.
    chipSelected = DarkOnSurface,
    onChipSelected = DarkBg,
)

val LocalMusFitColors = staticCompositionLocalOf<MusFitColors> {
    error("MusFitColors not provided. Wrap content in MusFitTheme.")
}
