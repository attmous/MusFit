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
    fun calories_withGoalShowsRemainingAndProgress() {
        val value = MetricResolver.resolve(TodayMetric.Calories, snapshot()) as MetricValue.WithGoal
        assertEquals("800", value.figure)
        assertEquals("kcal left", value.caption)
        assertEquals(0.6f, value.progress, 0.001f)
    }

    @Test
    fun calories_withoutGoalShowsEaten() {
        val value = MetricResolver.resolve(TodayMetric.Calories, snapshot(calorieGoalKcal = 0.0))
        assertEquals(MetricValue.Plain("1,200", "kcal eaten"), value)
    }

    @Test
    fun calories_overGoalShowsOverageWithGrouping() {
        val value = MetricResolver.resolve(
            TodayMetric.Calories,
            snapshot(caloriesKcal = 3500.0, calorieGoalKcal = 2000.0),
        ) as MetricValue.WithGoal
        assertEquals("1,500", value.figure)
        assertEquals("kcal over", value.caption)
        assertEquals(1.0f, value.progress, 0.001f)
    }

    @Test
    fun steps_nullMeansNotConnected() {
        val value = MetricResolver.resolve(TodayMetric.Steps, snapshot(steps = null))
        assertEquals(MetricValue.NoData("Not connected"), value)
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
        assertEquals(MetricValue.Plain("1000 ml", "today"), MetricResolver.resolve(TodayMetric.Water, snapshot(waterGoalMl = 0.0)))
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
    fun pages_threePinsFitOnHeroPage() {
        val pages = buildCarouselPages(listOf(TodayMetric.Calories, TodayMetric.Steps, TodayMetric.Protein))
        assertEquals(1, pages.size)
        assertEquals(TodayMetric.Calories, pages[0].hero)
        assertEquals(listOf(TodayMetric.Steps, TodayMetric.Protein), pages[0].chips)
    }

    @Test
    fun pages_singlePinIsHeroOnly() {
        val pages = buildCarouselPages(listOf(TodayMetric.Weight))
        assertEquals(1, pages.size)
        assertEquals(TodayMetric.Weight, pages[0].hero)
        assertTrue(pages[0].chips.isEmpty())
    }

    @Test
    fun pages_eightPinsOverflowInChunksOfFour() {
        val pins = TodayMetric.entries.take(8)
        val pages = buildCarouselPages(pins)
        assertEquals(3, pages.size) // hero+2, then 4, then 1
        assertEquals(pins[0], pages[0].hero)
        assertEquals(pins.subList(1, 3), pages[0].chips)
        assertEquals(null, pages[1].hero)
        assertEquals(pins.subList(3, 7), pages[1].chips)
        assertEquals(pins.subList(7, 8), pages[2].chips)
    }

    @Test
    fun pages_emptyPinsFallBackToDefaults() {
        val pages = buildCarouselPages(emptyList())
        assertEquals(TodayMetric.Calories, pages[0].hero)
    }
}
