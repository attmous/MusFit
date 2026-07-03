package com.musfit.ui.food

import org.junit.Assert.assertEquals
import org.junit.Test

class FoodBarcodeScannerSpotlightTest {
    @Test
    fun transform_peaksAtMiddleOfPulse() {
        val start = foodBarcodeScannerSpotlightTransform(progress = 0f)
        val peak = foodBarcodeScannerSpotlightTransform(progress = 0.5f)
        val end = foodBarcodeScannerSpotlightTransform(progress = 1f)

        assertEquals(1f, start.containerScale, 0.0001f)
        assertEquals(1.06f, peak.containerScale, 0.0001f)
        assertEquals(1f, end.containerScale, 0.0001f)

        assertEquals(1f, start.iconScale, 0.0001f)
        assertEquals(1.12f, peak.iconScale, 0.0001f)
        assertEquals(1f, end.iconScale, 0.0001f)

        assertEquals(0.84f, start.containerAlpha, 0.0001f)
        assertEquals(1f, peak.containerAlpha, 0.0001f)
        assertEquals(0.84f, end.containerAlpha, 0.0001f)

        assertEquals(0.14f, start.borderAlpha, 0.0001f)
        assertEquals(0.42f, peak.borderAlpha, 0.0001f)
        assertEquals(0.14f, end.borderAlpha, 0.0001f)
    }

    @Test
    fun transform_clampsProgressToPulseBounds() {
        val belowStart = foodBarcodeScannerSpotlightTransform(progress = -0.25f)
        val start = foodBarcodeScannerSpotlightTransform(progress = 0f)
        val pastEnd = foodBarcodeScannerSpotlightTransform(progress = 1.25f)
        val end = foodBarcodeScannerSpotlightTransform(progress = 1f)

        assertEquals(start, belowStart)
        assertEquals(end, pastEnd)
    }
}
