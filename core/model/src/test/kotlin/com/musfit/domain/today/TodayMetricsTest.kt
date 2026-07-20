package com.musfit.domain.today

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayMetricsTest {
    @Test
    fun sessions_weekBoundaryIsMondayStartInclusiveExclusive() {
        val weekStart = 1_000_000L
        val weekLen = 7 * 86_400_000L
        assertEquals(
            2,
            countSessionsInWeek(
                listOf(weekStart - 1, weekStart, weekStart + weekLen - 1, weekStart + weekLen),
                weekStart,
            ),
        )
    }

    @Test
    fun vitals_tilesFollowPinOrder() {
        val pins = listOf(TodayMetric.Weight, TodayMetric.Sleep, TodayMetric.Calories)
        assertEquals(pins, vitalsTileMetrics(pins))
    }

    @Test
    fun vitals_emptyPinsFallBackToTheDefaultFour() {
        assertEquals(
            listOf(TodayMetric.Calories, TodayMetric.Steps, TodayMetric.Protein, TodayMetric.Water),
            vitalsTileMetrics(emptyList()),
        )
        assertTrue(TodayMetric.DEFAULT_PINS == vitalsTileMetrics(emptyList()))
    }
}
