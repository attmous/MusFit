package com.musfit.ui.food

import org.junit.Assert.assertEquals
import org.junit.Test

class FoodAddItemSpotlightTest {
    @Test
    fun transform_peaksAtMiddleOfPulse() {
        val start = foodAddItemSpotlightTransform(progress = 0f)
        val peak = foodAddItemSpotlightTransform(progress = 0.5f)
        val end = foodAddItemSpotlightTransform(progress = 1f)

        assertEquals(1f, start.rowScale, 0.0001f)
        assertEquals(1.014f, peak.rowScale, 0.0001f)
        assertEquals(1f, end.rowScale, 0.0001f)

        assertEquals(0.12f, start.borderAlpha, 0.0001f)
        assertEquals(0.34f, peak.borderAlpha, 0.0001f)
        assertEquals(0.12f, end.borderAlpha, 0.0001f)

        assertEquals(1f, start.addIconScale, 0.0001f)
        assertEquals(1.08f, peak.addIconScale, 0.0001f)
        assertEquals(1f, end.addIconScale, 0.0001f)

        assertEquals(0.82f, start.addContainerAlpha, 0.0001f)
        assertEquals(1f, peak.addContainerAlpha, 0.0001f)
        assertEquals(0.82f, end.addContainerAlpha, 0.0001f)
    }

    @Test
    fun transform_clampsProgressToPulseBounds() {
        val belowStart = foodAddItemSpotlightTransform(progress = -0.25f)
        val start = foodAddItemSpotlightTransform(progress = 0f)
        val pastEnd = foodAddItemSpotlightTransform(progress = 1.25f)
        val end = foodAddItemSpotlightTransform(progress = 1f)

        assertEquals(start, belowStart)
        assertEquals(end, pastEnd)
    }
}
