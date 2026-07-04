package com.musfit.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AppDestinationTest {

    @Test
    fun entries_match_expected_bottom_tab_contract() {
        assertEquals(
            listOf("today", "food", "training", "profile"),
            AppDestination.entries.map { it.route },
        )
        assertEquals(
            listOf("Today", "Food", "Training", "Profile"),
            AppDestination.entries.map { it.label },
        )
    }

    @Test
    fun barcodeScannerRoute_is_separate_from_bottom_navigation_routes() {
        assertEquals("barcode-scanner", BARCODE_SCANNER_ROUTE)
        assertEquals(false, AppDestination.entries.any { it.route == BARCODE_SCANNER_ROUTE })
    }

    @Test
    fun profileSettingsRoute_keeps_profile_tab_selected() {
        assertEquals(AppDestination.Profile, bottomDestinationForRoute(PROFILE_SETTINGS_ROUTE))
    }

    @Test
    fun scannerRoutes_keep_food_tab_selected() {
        assertEquals(AppDestination.Food, bottomDestinationForRoute(BARCODE_SCANNER_ROUTE))
        assertEquals(AppDestination.Food, bottomDestinationForRoute(NUTRITION_LABEL_SCANNER_ROUTE))
    }
}
