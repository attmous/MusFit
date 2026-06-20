package com.musfit.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AppDestinationTest {

    @Test
    fun entries_match_expected_bottom_tab_contract() {
        assertEquals(
            listOf("today", "food", "training", "health"),
            AppDestination.entries.map { it.route },
        )
        assertEquals(
            listOf("Today", "Food", "Training", "Health"),
            AppDestination.entries.map { it.label },
        )
    }

    @Test
    fun barcodeScannerRoute_is_separate_from_bottom_navigation_routes() {
        assertEquals("barcode-scanner", BARCODE_SCANNER_ROUTE)
        assertEquals(false, AppDestination.entries.any { it.route == BARCODE_SCANNER_ROUTE })
    }
}
