package com.musfit.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
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
}
