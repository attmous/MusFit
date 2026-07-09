package com.musfit.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigationChromeTest {

    @Test
    fun bottomNavUsesFloatingExpressivePillMetrics() {
        assertTrue(
            "The nav pill bar should use the M3 Expressive rounded container.",
            MusFitBottomNavMetrics.BarCornerRadius.value >= 28f,
        )
        assertTrue(
            "The active-item pill and the coach FAB should share the expressive 22dp radius.",
            MusFitBottomNavMetrics.ActivePillRadius == MusFitBottomNavMetrics.FabCornerRadius,
        )
        assertTrue(
            "The inline coach FAB should be a 58dp rounded square.",
            MusFitBottomNavMetrics.FabSize.value >= 56f,
        )
        assertTrue(
            "The bar should keep fixed bottom clearance before system-bar insets.",
            MusFitBottomNavMetrics.RowBottomPadding.value >= 14f,
        )
    }
}
