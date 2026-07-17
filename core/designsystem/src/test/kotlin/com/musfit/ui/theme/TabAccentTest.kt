package com.musfit.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TabAccentTest {
    @Test
    fun eachDestinationMapsToItsLightAccentColor() {
        // The M3 Expressive quad — coral/green/indigo/teal; amber is reserved
        // for warning semantics, not a tab accent.
        assertEquals(Coral, tabAccentForLight(TabAccentRole.Today).color)
        assertEquals(Green, tabAccentForLight(TabAccentRole.Food).color)
        assertEquals(Indigo, tabAccentForLight(TabAccentRole.Training).color)
        assertEquals(Teal, tabAccentForLight(TabAccentRole.Profile).color)
    }

    @Test
    fun eachDestinationMapsToItsDarkAccentColor() {
        assertEquals(CoralBright, tabAccentForDark(TabAccentRole.Today).color)
        assertEquals(GreenBright, tabAccentForDark(TabAccentRole.Food).color)
        assertEquals(IndigoBright, tabAccentForDark(TabAccentRole.Training).color)
        assertEquals(TealBright, tabAccentForDark(TabAccentRole.Profile).color)
    }

    // Accent-filled primary buttons render onColor text on a color fill; button labels are
    // labelLarge (normal text), so every pair must meet WCAG AA 4.5:1 in both modes.
    @Test
    fun accentFillOnColorPairsMeetWcagAaInLightMode() {
        TabAccentRole.entries.forEach { destination ->
            val accent = tabAccentForLight(destination)
            val ratio = contrastRatio(accent.color, accent.onColor)
            assertTrue(
                "light $destination onColor-on-color contrast is $ratio, needs >= 4.5",
                ratio >= 4.5,
            )
        }
    }

    @Test
    fun accentFillOnColorPairsMeetWcagAaInDarkMode() {
        TabAccentRole.entries.forEach { destination ->
            val accent = tabAccentForDark(destination)
            val ratio = contrastRatio(accent.color, accent.onColor)
            assertTrue(
                "dark $destination onColor-on-color contrast is $ratio, needs >= 4.5",
                ratio >= 4.5,
            )
        }
    }

    // Hero containers carry their strong ink for display numerals; the pair must
    // stay readable in both modes.
    @Test
    fun containerInkPairsMeetWcagAaInBothModes() {
        TabAccentRole.entries.forEach { destination ->
            listOf(
                "light" to tabAccentForLight(destination),
                "dark" to tabAccentForDark(destination),
            ).forEach { (mode, accent) ->
                val ratio = contrastRatio(accent.container, accent.onContainer)
                assertTrue(
                    "$mode $destination onContainer-on-container contrast is $ratio, needs >= 4.5",
                    ratio >= 4.5,
                )
            }
        }
    }

    private fun contrastRatio(a: Color, b: Color): Double {
        val lighter = maxOf(a.luminance(), b.luminance()).toDouble()
        val darker = minOf(a.luminance(), b.luminance()).toDouble()
        return (lighter + 0.05) / (darker + 0.05)
    }
}
