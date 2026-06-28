package com.musfit.ui.today

import com.musfit.domain.today.WeeklyGoals
import com.musfit.domain.today.WeightPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BuildWeeklyChartsTest {
    private fun weekly(
        caloriesPerDay: List<Double?> = List(7) { 2000.0 },
        calorieGoalKcal: Double = 2000.0,
        weightPoints: List<WeightPoint> = emptyList(),
    ) = WeeklyGoals(
        sessionsDone = 3,
        sessionTarget = 5,
        calorieOnTargetDays = 4,
        trackedDays = 7,
        stepGoalDays = 2,
        weightAvgKg = 80.5,
        weightDeltaKg = -0.6,
        targetWeightKg = 78.0,
        caloriesPerDay = caloriesPerDay,
        calorieGoalKcal = calorieGoalKcal,
        stepsPerDay = List(7) { 9000L },
        stepGoal = 10000L,
        weightPoints = weightPoints,
    )

    @Test
    fun mapsSevenLabelledBarsInMondayFirstOrder() {
        val state = buildWeeklyCharts(weekly(), todayIndex = 2)
        assertEquals(7, state.calorieBars.size)
        assertEquals(listOf("M", "T", "W", "T", "F", "S", "S"), state.calorieBars.map { it.label })
        assertEquals(2000.0, state.calorieBars[2].calories!!, 0.001)
    }

    @Test
    fun defaultsSelectionToTodayWhenTracked() {
        assertEquals(4, buildWeeklyCharts(weekly(), todayIndex = 4).defaultSelectedIndex)
    }

    @Test
    fun defaultsSelectionToLastTrackedDayWhenTodayUntracked() {
        val cals = listOf(2000.0, 2100.0, 1900.0, null, null, null, null)
        assertEquals(2, buildWeeklyCharts(weekly(caloriesPerDay = cals), todayIndex = 5).defaultSelectedIndex)
    }

    @Test
    fun defaultSelectionIsNullWhenNothingTracked() {
        assertNull(buildWeeklyCharts(weekly(caloriesPerDay = List(7) { null }), todayIndex = 1).defaultSelectedIndex)
    }

    @Test
    fun surfacesTargetTrendAndStats() {
        val points = listOf(WeightPoint(1L, 81.0), WeightPoint(2L, 80.2))
        val state = buildWeeklyCharts(weekly(weightPoints = points), todayIndex = 0)
        assertEquals(2000.0, state.calorieTarget!!, 0.001)
        assertEquals(listOf(81.0, 80.2), state.weightTrend)
        assertEquals(80.2, state.latestWeightKg!!, 0.001)
        assertEquals(-0.6, state.weightDeltaKg!!, 0.001)
        assertEquals(4, state.onTargetDays)
        assertEquals(3, state.sessionsDone)
        assertEquals(5, state.sessionTarget)
        assertEquals(2, state.stepGoalDays)
    }

    @Test
    fun noGoalYieldsNullTarget() {
        assertNull(buildWeeklyCharts(weekly(calorieGoalKcal = 0.0), todayIndex = 0).calorieTarget)
    }
}
