package com.musfit.domain.training

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrendChartScalerTest {
    private val width = 300f
    private val height = 200f
    private val padL = 36f
    private val padR = 12f
    private val padT = 10f
    private val padB = 18f
    private val plotH = height - padT - padB

    private fun geom(values: List<Double>, tickCount: Int = 3) = TrendChartScaler.computeChartGeometry(values, width, height, padL, padR, padT, padB, tickCount)

    @Test
    fun twoPoints_mapXAcrossPlotWidth() {
        val g = geom(listOf(10.0, 20.0))
        assertEquals(padL, g.points.first().x, 0.01f)
        assertEquals(width - padR, g.points.last().x, 0.01f)
    }

    @Test
    fun yIsInverted_higherValueMapsToSmallerY() {
        val g = geom(listOf(10.0, 20.0))
        // index 0 = 10 (min) -> bottom; index 1 = 20 (max) -> top
        assertEquals(padT + plotH, g.points[0].y, 0.01f)
        assertEquals(padT, g.points[1].y, 0.01f)
        assertTrue(g.points[1].y < g.points[0].y)
    }

    @Test
    fun singlePoint_isCenteredAndFinite() {
        val g = geom(listOf(42.0))
        assertEquals(1, g.points.size)
        assertEquals(width / 2f, g.points[0].x, 0.01f)
        assertEquals(padT + plotH / 2f, g.points[0].y, 0.01f)
        assertTrue(g.points[0].y.isFinite())
    }

    @Test
    fun flatSeries_allYCentered_noDivideByZero() {
        val g = geom(listOf(50.0, 50.0, 50.0))
        val center = padT + plotH / 2f
        assertTrue(g.points.all { it.y == center && it.y.isFinite() })
    }

    @Test
    fun emptyValues_returnsEmptyGeometry() {
        val g = geom(emptyList())
        assertTrue(g.points.isEmpty())
        assertTrue(g.yTicks.isEmpty())
    }

    @Test
    fun niceTicks_areAscendingAndSpanTheRange() {
        val g = geom(listOf(10.0, 30.0))
        assertEquals(3, g.yTicks.size)
        assertEquals(10.0, g.yTicks.first(), 0.01)
        assertEquals(30.0, g.yTicks.last(), 0.01)
        assertTrue(g.yTicks.zipWithNext().all { (a, b) -> a <= b })
    }

    @Test
    fun niceTicks_flatRange_returnsSingleTick() {
        val g = geom(listOf(20.0, 20.0))
        assertEquals(listOf(20.0), g.yTicks)
    }

    @Test
    fun nearestIndex_picksClosestAndClamps() {
        val xs = listOf(0f, 50f, 100f)
        assertEquals(0, TrendChartScaler.nearestIndex(-10f, xs))
        assertEquals(2, TrendChartScaler.nearestIndex(200f, xs))
        assertEquals(1, TrendChartScaler.nearestIndex(60f, xs))
        // Exact midway between index 0 and 1 -> earlier index.
        assertEquals(0, TrendChartScaler.nearestIndex(25f, xs))
    }

    @Test
    fun nearestIndex_emptyReturnsZero() {
        assertEquals(0, TrendChartScaler.nearestIndex(10f, emptyList()))
    }
}
