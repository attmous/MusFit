package com.musfit.domain.charts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BarChartScalerTest {
    @Test
    fun normalisesValuesAgainstMaxWithoutHeadroom() {
        val g = BarChartScaler.compute(values = listOf(1000.0, 2000.0), target = null, headroom = 1.0)
        assertEquals(0.5, g.bars[0]!!, 0.001)
        assertEquals(1.0, g.bars[1]!!, 0.001)
        assertEquals(2000.0, g.maxValue, 0.001)
    }

    @Test
    fun headroomKeepsTallestBarBelowTheTop() {
        val g = BarChartScaler.compute(values = listOf(2000.0), target = null, headroom = 1.15)
        assertTrue(g.bars[0]!! < 1.0)
        assertEquals(2300.0, g.maxValue, 0.001)
    }

    @Test
    fun nullValuesPassThroughAsUntracked() {
        val g = BarChartScaler.compute(values = listOf(1000.0, null, 2000.0), target = null, headroom = 1.0)
        assertNull(g.bars[1])
        assertEquals(0.5, g.bars[0]!!, 0.001)
    }

    @Test
    fun targetCountsTowardsMaxAndGetsItsOwnFraction() {
        val g = BarChartScaler.compute(values = listOf(1000.0), target = 2000.0, headroom = 1.0)
        assertEquals(0.5, g.bars[0]!!, 0.001)
        assertEquals(1.0, g.targetFraction!!, 0.001)
    }

    @Test
    fun allNullSeriesProducesNullsAndZeroMax() {
        val g = BarChartScaler.compute(values = listOf(null, null), target = null)
        assertEquals(listOf<Double?>(null, null), g.bars)
        assertNull(g.targetFraction)
        assertEquals(0.0, g.maxValue, 0.001)
    }

    @Test
    fun allZeroSeriesProducesZeroFractionsNotNaN() {
        val g = BarChartScaler.compute(values = listOf(0.0, 0.0), target = null)
        assertTrue(g.bars.all { it == 0.0 })
        assertEquals(0.0, g.maxValue, 0.001)
    }

    @Test
    fun emptySeriesIsHandled() {
        val g = BarChartScaler.compute(values = emptyList(), target = null)
        assertTrue(g.bars.isEmpty())
        assertNull(g.targetFraction)
    }
}
