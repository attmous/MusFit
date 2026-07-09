package com.musfit.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigationChromeTest {

    @Test
    fun bottomNavMetricsReserveClearanceAndUseLargerIcons() {
        assertTrue(
            "Bottom nav should reserve a taller touch and clearance area.",
            MusFitBottomNavMetrics.BarHeight.value >= 88f,
        )
        assertTrue(
            "Bottom nav icons should be larger than the compact 22dp baseline.",
            MusFitBottomNavMetrics.IconSize.value >= 26f,
        )
        assertTrue(
            "Bottom nav should keep fixed bottom clearance before system-bar insets.",
            MusFitBottomNavMetrics.BottomPadding.value >= 14f,
        )
    }
}
