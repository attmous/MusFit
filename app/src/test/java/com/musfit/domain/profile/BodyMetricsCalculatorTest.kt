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
}
