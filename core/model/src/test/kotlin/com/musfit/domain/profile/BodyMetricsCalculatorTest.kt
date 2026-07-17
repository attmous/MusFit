package com.musfit.domain.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BodyMetricsCalculatorTest {
    @Test
    fun bmi_computesAndRoundsToOneDecimal() {
        assertEquals(24.7, BodyMetricsCalculator.bodyMassIndex(weightKg = 80.0, heightCm = 180.0)!!, 0.001)
    }

    @Test
    fun bmi_returnsNullForNonPositiveInputs() {
        assertNull(BodyMetricsCalculator.bodyMassIndex(0.0, 180.0))
        assertNull(BodyMetricsCalculator.bodyMassIndex(80.0, 0.0))
    }

    @Test
    fun goalProgress_isFractionFromStartTowardGoal() {
        assertEquals(0.5, BodyMetricsCalculator.goalProgressFraction(90.0, 84.0, 78.0)!!, 0.001)
    }

    @Test
    fun goalProgress_clampsToZeroOneAndHandlesNoDelta() {
        assertEquals(1.0, BodyMetricsCalculator.goalProgressFraction(90.0, 70.0, 78.0)!!, 0.001)
        assertEquals(0.0, BodyMetricsCalculator.goalProgressFraction(90.0, 95.0, 78.0)!!, 0.001)
        assertNull(BodyMetricsCalculator.goalProgressFraction(78.0, 78.0, 78.0))
    }

    @Test
    fun changeOverWindow_usesBaselineAtOrBeforeCutoff() {
        val day = 86_400_000L
        val points = listOf(0L to 86.0, 7L * day to 85.0, 14L * day to 84.0)
        val delta = BodyMetricsCalculator.changeOverWindow(points, windowMillis = 7L * day, nowMillis = 14L * day)
        assertEquals(-1.0, delta!!, 0.001)
    }

    @Test
    fun changeOverWindow_fallsBackToEarliestWhenAllWithinWindow() {
        val day = 86_400_000L
        val points = listOf(10L * day to 85.0, 14L * day to 84.0)
        val delta = BodyMetricsCalculator.changeOverWindow(points, windowMillis = 7L * day, nowMillis = 14L * day)
        assertEquals(-1.0, delta!!, 0.001)
    }

    @Test
    fun changeOverWindow_nullForFewerThanTwoPoints() {
        assertNull(BodyMetricsCalculator.changeOverWindow(listOf(5L to 80.0), windowMillis = 10L, nowMillis = 20L))
        assertNull(BodyMetricsCalculator.changeOverWindow(emptyList(), windowMillis = 10L, nowMillis = 20L))
    }
}
