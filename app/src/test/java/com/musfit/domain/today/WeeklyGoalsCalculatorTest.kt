package com.musfit.domain.today

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeeklyGoalsCalculatorTest {
    private val weekStart = 1_700_000_000_000L
    private val day = 86_400_000L

    @Test
    fun countsSessionsCaloriesStepsAndWeightTrend() {
        val result = WeeklyGoalsCalculator.compute(
            weekStartMillis = weekStart,
            sessionStartMillis = listOf(weekStart + day, weekStart + 3 * day, weekStart - day),
            sessionTarget = 4,
            loggedCaloriesPerDay = listOf(2000.0, 1900.0, null, 3000.0),
            calorieGoalKcal = 2000.0,
            stepsPerDay = listOf(12_000L, 8_000L, 10_000L),
            stepGoal = 10_000L,
            weights = listOf(weekStart + day to 80.0, weekStart - 2 * day to 81.0),
            targetWeightKg = 78.0,
        )

        assertEquals(2, result.sessionsDone)
        assertEquals(4, result.sessionTarget)
        assertEquals(2, result.calorieOnTargetDays)
        assertEquals(7, result.trackedDays)
        assertEquals(2, result.stepGoalDays)
        assertEquals(80.0, result.weightAvgKg!!, 0.001)
        assertEquals(-1.0, result.weightDeltaKg!!, 0.001)
        assertEquals(78.0, result.targetWeightKg!!, 0.001)
    }

    @Test
    fun nullsWhenNoWeightsOrTarget() {
        val result = WeeklyGoalsCalculator.compute(
            weekStartMillis = weekStart,
            sessionStartMillis = emptyList(),
            sessionTarget = 4,
            loggedCaloriesPerDay = emptyList(),
            calorieGoalKcal = 0.0,
            stepsPerDay = emptyList(),
            stepGoal = 10_000L,
            weights = emptyList(),
            targetWeightKg = 0.0,
        )

        assertEquals(0, result.sessionsDone)
        assertEquals(0, result.calorieOnTargetDays)
        assertEquals(0, result.stepGoalDays)
        assertNull(result.weightAvgKg)
        assertNull(result.weightDeltaKg)
        assertNull(result.targetWeightKg)
    }

    @Test
    fun surfacesPerDaySeriesForCharts() {
        val result = WeeklyGoalsCalculator.compute(
            weekStartMillis = weekStart,
            sessionStartMillis = emptyList(),
            sessionTarget = 4,
            loggedCaloriesPerDay = listOf(2000.0, 1900.0, null, 2100.0, 2050.0, 1800.0, 2200.0),
            calorieGoalKcal = 2000.0,
            stepsPerDay = listOf(12_000L, 8_000L, 10_000L, 9_000L, 11_000L, 7_000L, 10_500L),
            stepGoal = 10_000L,
            // The health DAO returns weights newest-first (ORDER BY measuredAt DESC); the chart needs them chronological.
            weights = listOf(weekStart + day to 80.0, weekStart - 2 * day to 81.0),
            targetWeightKg = 78.0,
        )

        assertEquals(listOf(2000.0, 1900.0, null, 2100.0, 2050.0, 1800.0, 2200.0), result.caloriesPerDay)
        assertEquals(2000.0, result.calorieGoalKcal, 0.001)
        assertEquals(listOf(12_000L, 8_000L, 10_000L, 9_000L, 11_000L, 7_000L, 10_500L), result.stepsPerDay)
        assertEquals(10_000L, result.stepGoal)
        // weightPoints must be ascending by time regardless of input order, so latest = last and the line draws left→right.
        assertEquals(
            listOf(WeightPoint(weekStart - 2 * day, 81.0), WeightPoint(weekStart + day, 80.0)),
            result.weightPoints,
        )
    }
}
