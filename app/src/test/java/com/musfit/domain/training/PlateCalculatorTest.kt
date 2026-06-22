package com.musfit.domain.training

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlateCalculatorTest {
    @Test
    fun breaksDownCommonWeights() {
        assertEquals(listOf(25.0, 15.0), PlateCalculator.platesPerSide(100.0))
        assertEquals(listOf(20.0), PlateCalculator.platesPerSide(60.0))
        assertEquals(listOf(25.0, 15.0, 1.25), PlateCalculator.platesPerSide(102.5))
        assertEquals(listOf(25.0, 25.0, 10.0), PlateCalculator.platesPerSide(140.0))
    }

    @Test
    fun emptyWhenAtOrBelowBar() {
        assertTrue(PlateCalculator.platesPerSide(20.0).isEmpty())
        assertTrue(PlateCalculator.platesPerSide(15.0).isEmpty())
    }

    @Test
    fun ignoresUnloadableRemainder() {
        // 21 kg → 0.5 per side, smaller than the smallest plate → nothing added.
        assertTrue(PlateCalculator.platesPerSide(21.0).isEmpty())
    }
}
