package com.musfit.domain.today

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayMetricsTest {
    private fun snapshot(
        caloriesKcal: Double = 1200.0,
        calorieGoalKcal: Double = 2000.0,
        proteinGrams: Double = 80.0,
        proteinGoalGrams: Double = 150.0,
        carbsGrams: Double = 130.0,
        carbsGoalGrams: Double = 200.0,
        fatGrams: Double = 40.0,
        fatGoalGrams: Double = 60.0,
        waterMl: Double = 1000.0,
        waterGoalMl: Double = 2000.0,
        steps: Long? = 8200L,
        stepGoal: Long = 10_000L,
        latestWeightKg: Double? = 82.4,
        weightDeltaKg: Double? = -0.4,
        bodyFatPercent: Double? = 18.5,
        bodyFatDelta: Double? = -0.3,
        sessionsDone: Int = 2,
        sessionTarget: Int = 4,
        activeCaloriesKcal: Double? = 420.0,
        sleepMinutes: Long? = 465L,
        exerciseMinutes: Long? = 68L,
        exerciseSessionCount: Int? = 2,
        restingHeartRateBpm: Long? = 58L, // matches DailyHealthSummaryEntity.restingHeartRateBpm: Long?
        loggingStreakDays: Int = 5,
    ) = MetricSnapshot(
        caloriesKcal = caloriesKcal,
        calorieGoalKcal = calorieGoalKcal,
        proteinGrams = proteinGrams,
        proteinGoalGrams = proteinGoalGrams,
        carbsGrams = carbsGrams,
        carbsGoalGrams = carbsGoalGrams,
        fatGrams = fatGrams,
        fatGoalGrams = fatGoalGrams,
        waterMl = waterMl,
        waterGoalMl = waterGoalMl,
        steps = steps,
        stepGoal = stepGoal,
        latestWeightKg = latestWeightKg,
        weightDeltaKg = weightDeltaKg,
        bodyFatPercent = bodyFatPercent,
        bodyFatDelta = bodyFatDelta,
        sessionsDone = sessionsDone,
        sessionTarget = sessionTarget,
        activeCaloriesKcal = activeCaloriesKcal,
        sleepMinutes = sleepMinutes,
        exerciseMinutes = exerciseMinutes,
        exerciseSessionCount = exerciseSessionCount,
        restingHeartRateBpm = restingHeartRateBpm,
        loggingStreakDays = loggingStreakDays,
    )

    @Test
    fun calories_withGoalShowsEatenOfGoalWithPercent() {
        // Turn 8 vitals tile: the EATEN figure with the full goal and percent in the sub line.
        val value = MetricResolver.resolve(TodayMetric.Calories, snapshot()) as MetricValue.WithGoal
        assertEquals("1,200", value.figure)
        assertEquals("of 2,000 kcal · 60%", value.caption)
        assertEquals(0.6f, value.progress, 0.001f)
    }

    @Test
    fun calories_withoutGoalShowsEaten() {
        val value = MetricResolver.resolve(TodayMetric.Calories, snapshot(calorieGoalKcal = 0.0))
        assertEquals(MetricValue.Plain("1,200", "kcal eaten"), value)
    }

    @Test
    fun calories_overGoalReportsPercentPastHundredWithFullWave() {
        val value = MetricResolver.resolve(
            TodayMetric.Calories,
            snapshot(caloriesKcal = 3500.0, calorieGoalKcal = 2000.0),
        ) as MetricValue.WithGoal
        assertEquals("3,500", value.figure)
        assertEquals("of 2,000 kcal · 175%", value.caption)
        assertEquals(1.0f, value.progress, 0.001f)
    }

    @Test
    fun steps_nullMeansNotConnected() {
        val value = MetricResolver.resolve(TodayMetric.Steps, snapshot(steps = null))
        assertEquals(MetricValue.NoData("Not connected"), value)
    }

    @Test
    fun steps_withGoalUsesFullDotGroupedCounts() {
        val value = MetricResolver.resolve(
            TodayMetric.Steps,
            snapshot(steps = 5_500L, stepGoal = 10_000L),
        ) as MetricValue.WithGoal

        assertEquals("5.500", value.figure)
        assertEquals("of 10.000 · 55%", value.caption)
    }

    @Test
    fun protein_withGoalShowsGramsOfGoalWithPercent() {
        val value = MetricResolver.resolve(TodayMetric.Protein, snapshot()) as MetricValue.WithGoal
        assertEquals("80 g", value.figure)
        assertEquals("of 150 g · 53%", value.caption)
    }

    @Test
    fun water_withGoalShowsLitersOfGoalWithPercent() {
        val value = MetricResolver.resolve(
            TodayMetric.Water,
            snapshot(waterMl = 1500.0, waterGoalMl = 2500.0),
        ) as MetricValue.WithGoal
        assertEquals("1.5 L", value.figure)
        assertEquals("of 2.5 L · 60%", value.caption)
        assertEquals(0.6f, value.progress, 0.001f)
    }

    @Test
    fun water_wholeLitersDropTheDecimal() {
        val value = MetricResolver.resolve(
            TodayMetric.Water,
            snapshot(waterMl = 2000.0, waterGoalMl = 2000.0),
        ) as MetricValue.WithGoal
        assertEquals("2 L", value.figure)
        assertEquals("of 2 L · 100%", value.caption)
    }

    @Test
    fun weight_showsLatestWithDeltaAndNoDataWhenNeverLogged() {
        val value = MetricResolver.resolve(TodayMetric.Weight, snapshot()) as MetricValue.Plain
        assertEquals("82.4 kg", value.figure)
        assertEquals("−0.4 kg · 7d", value.caption)

        val empty = MetricResolver.resolve(TodayMetric.Weight, snapshot(latestWeightKg = null))
        assertEquals(MetricValue.NoData("No data"), empty)
    }

    @Test
    fun calorieBalance_needsHealthConnect() {
        val value = MetricResolver.resolve(TodayMetric.CalorieBalance, snapshot()) as MetricValue.Plain
        assertEquals("780", value.figure) // 1200 eaten − 420 active
        assertEquals("kcal in − out", value.caption)

        val noHc = MetricResolver.resolve(TodayMetric.CalorieBalance, snapshot(activeCaloriesKcal = null))
        assertEquals(MetricValue.NoData("Not connected"), noHc)
    }

    @Test
    fun sessions_showsDoneVsTarget() {
        val value = MetricResolver.resolve(TodayMetric.Sessions, snapshot()) as MetricValue.WithGoal
        assertEquals("2/4", value.figure)
        assertEquals("this week", value.caption)
        assertEquals(0.5f, value.progress, 0.001f)
    }

    @Test
    fun sleepAndExercise_showHealthConnectAggregates() {
        val sleep = MetricResolver.resolve(TodayMetric.Sleep, snapshot()) as MetricValue.Plain
        assertEquals("7h 45m", sleep.figure)
        assertEquals("sleep", sleep.caption)

        val exercise = MetricResolver.resolve(TodayMetric.Exercise, snapshot()) as MetricValue.Plain
        assertEquals("1h 08m", exercise.figure)
        assertEquals("2 sessions", exercise.caption)
    }

    @Test
    fun remainingMetrics_noDataAndNoGoalBranches() {
        assertEquals(MetricValue.Plain("80 g", "today"), MetricResolver.resolve(TodayMetric.Protein, snapshot(proteinGoalGrams = 0.0)))
        assertEquals(MetricValue.Plain("1 L", "today"), MetricResolver.resolve(TodayMetric.Water, snapshot(waterGoalMl = 0.0)))
        assertEquals(MetricValue.NoData("No data"), MetricResolver.resolve(TodayMetric.BodyFat, snapshot(bodyFatPercent = null)))
        assertEquals(MetricValue.NoData("Not connected"), MetricResolver.resolve(TodayMetric.ActiveCalories, snapshot(activeCaloriesKcal = null)))
        assertEquals(MetricValue.NoData("Not connected"), MetricResolver.resolve(TodayMetric.Sleep, snapshot(sleepMinutes = null)))
        assertEquals(MetricValue.NoData("Not connected"), MetricResolver.resolve(TodayMetric.Exercise, snapshot(exerciseMinutes = null)))
        assertEquals(MetricValue.NoData("Not connected"), MetricResolver.resolve(TodayMetric.RestingHeartRate, snapshot(restingHeartRateBpm = null)))
        assertEquals(MetricValue.Plain("58 bpm", "resting"), MetricResolver.resolve(TodayMetric.RestingHeartRate, snapshot()))
    }

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
