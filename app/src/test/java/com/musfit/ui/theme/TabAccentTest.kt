package com.musfit.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.musfit.ui.AppDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TabAccentTest {
    @Test
    fun eachDestinationMapsToItsLightAccentColor() {
        // Four distinct hues — amber/emerald/indigo/teal; coral is reserved for
        // warning semantics, not a tab accent.
        assertEquals(Amber, tabAccentForLight(AppDestination.Today).color)
        assertEquals(Emerald, tabAccentForLight(AppDestination.Food).color)
        assertEquals(Indigo, tabAccentForLight(AppDestination.Training).color)
        assertEquals(Teal, tabAccentForLight(AppDestination.Profile).color)
    }

    @Test
    fun eachDestinationMapsToItsDarkAccentColor() {
        assertEquals(AmberBright, tabAccentForDark(AppDestination.Today).color)
        assertEquals(EmeraldBright, tabAccentForDark(AppDestination.Food).color)
        assertEquals(IndigoBright, tabAccentForDark(AppDestination.Training).color)
        assertEquals(TealBright, tabAccentForDark(AppDestination.Profile).color)
    }

    // Accent-filled primary buttons render onColor text on a color fill; button labels are
    // labelLarge (normal text), so every pair must meet WCAG AA 4.5:1 in both modes.
    @Test
    fun accentFillOnColorPairsMeetWcagAaInLightMode() {
        AppDestination.entries.forEach { destination ->
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
        AppDestination.entries.forEach { destination ->
            val accent = tabAccentForDark(destination)
            val ratio = contrastRatio(accent.color, accent.onColor)
            assertTrue(
                "dark $destination onColor-on-color contrast is $ratio, needs >= 4.5",
                ratio >= 4.5,
            )
        }
    }

    private fun contrastRatio(a: Color, b: Color): Double {
        val lighter = maxOf(a.luminance(), b.luminance()).toDouble()
        val darker = minOf(a.luminance(), b.luminance()).toDouble()
        return (lighter + 0.05) / (darker + 0.05)
    }
}
