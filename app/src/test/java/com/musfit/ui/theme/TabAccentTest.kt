package com.musfit.ui.theme

import com.musfit.ui.AppDestination
import org.junit.Assert.assertEquals
import org.junit.Test

class TabAccentTest {
    @Test
    fun eachDestinationMapsToItsAccentColor() {
        assertEquals(Coral, tabAccentFor(AppDestination.Today).color)
        assertEquals(Emerald, tabAccentFor(AppDestination.Food).color)
        assertEquals(Indigo, tabAccentFor(AppDestination.Training).color)
        assertEquals(Teal, tabAccentFor(AppDestination.Profile).color)
    }
}
