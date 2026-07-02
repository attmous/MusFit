package com.musfit.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusFitColorsTest {
    @Test
    fun macroColors_preserveLegacyCarbsProteinFatOrder() {
        // FoodScreen indexes the macro list as [0]=Carbs, [1]=Protein, [2]=Fat.
        // The token-derived list MUST keep that order or the macro UI mis-colors.
        val colors = lightMusFitColors
        assertEquals(
            listOf(colors.macroCarbs, colors.macroProtein, colors.macroFat),
            colors.macroColors,
        )
    }

    @Test
    fun lightPalette_usesApprovedBrandAccentBackground() {
        assertEquals(Color(0xFF1E7A53), lightMusFitColors.brand)
        assertEquals(Color(0xFFFF7A66), lightMusFitColors.accent)
        assertEquals(Color(0xFFFBF7F1), lightMusFitColors.background)
    }

    @Test
    fun darkPaletteIsActuallyDark_andDiffersFromLight() {
        assertTrue("dark background must be darker than light",
            darkMusFitColors.background.luminance() < lightMusFitColors.background.luminance())
        assertTrue("dark surface must be dark", darkMusFitColors.surface.luminance() < 0.2f)
        assertTrue("light surface must be light", lightMusFitColors.surface.luminance() > 0.8f)
    }

    @Test
    fun darkMacroColors_preserveCarbsProteinFatOrder() {
        assertEquals(
            listOf(darkMusFitColors.macroCarbs, darkMusFitColors.macroProtein, darkMusFitColors.macroFat),
            darkMusFitColors.macroColors,
        )
    }

    // accent/onAccent and brand/onBrand style filled controls (Food FAB, onSecondary);
    // their content must meet WCAG AA 4.5:1 on the fill in both palettes.
    @Test
    fun accentAndBrandFillPairsMeetWcagAa() {
        listOf("light" to lightMusFitColors, "dark" to darkMusFitColors).forEach { (mode, colors) ->
            assertTrue(
                "$mode onAccent-on-accent contrast is ${contrastRatio(colors.accent, colors.onAccent)}, needs >= 4.5",
                contrastRatio(colors.accent, colors.onAccent) >= 4.5,
            )
            assertTrue(
                "$mode onBrand-on-brand contrast is ${contrastRatio(colors.brand, colors.onBrand)}, needs >= 4.5",
                contrastRatio(colors.brand, colors.onBrand) >= 4.5,
            )
        }
    }

    private fun contrastRatio(a: Color, b: Color): Double {
        val lighter = maxOf(a.luminance(), b.luminance()).toDouble()
        val darker = minOf(a.luminance(), b.luminance()).toDouble()
        return (lighter + 0.05) / (darker + 0.05)
    }
}
