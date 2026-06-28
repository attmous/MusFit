package com.musfit.ui.theme

import com.musfit.ui.AppDestination
import org.junit.Assert.assertEquals
import org.junit.Test

class TabAccentTest {
    @Test
    fun eachDestinationMapsToItsLightAccentColor() {
        assertEquals(Coral, tabAccentForLight(AppDestination.Today).color)
        assertEquals(Emerald, tabAccentForLight(AppDestination.Food).color)
        assertEquals(Indigo, tabAccentForLight(AppDestination.Training).color)
        assertEquals(Teal, tabAccentForLight(AppDestination.Profile).color)
    }

    @Test
    fun eachDestinationMapsToItsDarkAccentColor() {
        assertEquals(CoralBright, tabAccentForDark(AppDestination.Today).color)
        assertEquals(EmeraldBright, tabAccentForDark(AppDestination.Food).color)
        assertEquals(IndigoBright, tabAccentForDark(AppDestination.Training).color)
        assertEquals(TealBright, tabAccentForDark(AppDestination.Profile).color)
    }
}
