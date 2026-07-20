package com.musfit.ui.today

import com.musfit.domain.today.MetricSnapshot
import com.musfit.domain.today.TodayMetric
import com.musfit.feature.today.R
import com.musfit.ui.text.UiText
import com.musfit.ui.text.pluralUiText
import com.musfit.ui.text.uiText
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class TodayMetricPresentationTest {
    @Test
    fun calories_withGoalUsesLocalizedFullValuesAndUnclampedPercent() {
        val value = resolveMetricPresentation(TodayMetric.Calories, snapshot(), Locale.US)
            as MetricValueUiState.WithGoal

        assertEquals(UiText.Verbatim("1,200"), value.figure)
        assertEquals(
            uiText(
                R.string.today_of_kcal_percent,
                text("2,000"),
                text("60"),
            ),
            value.caption,
        )
        assertEquals(0.6f, value.progress, 0.001f)

        val overGoal = resolveMetricPresentation(
            TodayMetric.Calories,
            snapshot(caloriesKcal = 3_500.0, calorieGoalKcal = 2_000.0),
            Locale.US,
        ) as MetricValueUiState.WithGoal
        assertEquals(
            uiText(R.string.today_of_kcal_percent, text("2,000"), text("175")),
            overGoal.caption,
        )
        assertEquals(1f, overGoal.progress, 0.001f)
    }

    @Test
    fun germanNumbersUseGroupingAndDecimalSeparators() {
        val steps = resolveMetricPresentation(
            TodayMetric.Steps,
            snapshot(steps = 5_500L, stepGoal = 10_000L),
            Locale.GERMANY,
        ) as MetricValueUiState.WithGoal
        assertEquals(UiText.Verbatim("5.500"), steps.figure)
        assertEquals(
            uiText(R.string.today_of_value_percent, UiText.Argument.Nested(UiText.Verbatim("10.000")), text("55")),
            steps.caption,
        )

        val weight = resolveMetricPresentation(TodayMetric.Weight, snapshot(), Locale.GERMANY)
            as MetricValueUiState.Plain
        assertEquals(uiText(R.string.today_value_kg, text("82,4")), weight.figure)
        assertEquals(uiText(R.string.today_weight_delta_7_days, text("−"), text("0,4")), weight.caption)
    }

    @Test
    fun missingAndGoalLessMetricsUseTypedResourceCopy() {
        assertEquals(
            MetricValueUiState.NoData(uiText(R.string.today_not_connected)),
            resolveMetricPresentation(TodayMetric.Steps, snapshot(steps = null), Locale.US),
        )
        assertEquals(
            MetricValueUiState.NoData(uiText(R.string.today_no_data)),
            resolveMetricPresentation(TodayMetric.BodyFat, snapshot(bodyFatPercent = null), Locale.US),
        )
        assertEquals(
            MetricValueUiState.Plain(
                uiText(R.string.today_value_grams, text("80")),
                uiText(R.string.today_today),
            ),
            resolveMetricPresentation(TodayMetric.Protein, snapshot(proteinGoalGrams = 0.0), Locale.US),
        )
    }

    @Test
    fun nutritionAndWaterGoalsKeepUnitsInResources() {
        val protein = resolveMetricPresentation(TodayMetric.Protein, snapshot(), Locale.US)
            as MetricValueUiState.WithGoal
        assertEquals(uiText(R.string.today_value_grams, text("80")), protein.figure)
        assertEquals(
            uiText(R.string.today_of_grams_percent, text("150"), text("53")),
            protein.caption,
        )

        val water = resolveMetricPresentation(
            TodayMetric.Water,
            snapshot(waterMl = 1_500.0, waterGoalMl = 2_500.0),
            Locale.US,
        ) as MetricValueUiState.WithGoal
        assertEquals(uiText(R.string.today_value_liters, text("1.5")), water.figure)
        assertEquals(
            uiText(
                R.string.today_of_value_percent,
                UiText.Argument.Nested(uiText(R.string.today_value_liters, text("2.5"))),
                text("60"),
            ),
            water.caption,
        )
    }

    @Test
    fun sessionsSleepAndExerciseUseTypedCountsAndPlurals() {
        val sessions = resolveMetricPresentation(TodayMetric.Sessions, snapshot(), Locale.US)
            as MetricValueUiState.WithGoal
        assertEquals(uiText(R.string.today_sessions_fraction, text("2"), text("4")), sessions.figure)
        assertEquals(uiText(R.string.today_this_week), sessions.caption)

        val sleep = resolveMetricPresentation(TodayMetric.Sleep, snapshot(), Locale.US)
            as MetricValueUiState.Plain
        assertEquals(uiText(R.string.today_duration_hours_minutes, text("7"), text("45")), sleep.figure)
        assertEquals(uiText(R.string.today_sleep), sleep.caption)

        val exercise = resolveMetricPresentation(TodayMetric.Exercise, snapshot(), Locale.US)
            as MetricValueUiState.Plain
        assertEquals(uiText(R.string.today_duration_hours_minutes, text("1"), text("08")), exercise.figure)
        assertEquals(pluralUiText(R.plurals.today_sessions, 2, text("2")), exercise.caption)
    }

    @Test
    fun balanceAndRestingHeartRateKeepCalculatedValuesTyped() {
        val balance = resolveMetricPresentation(TodayMetric.CalorieBalance, snapshot(), Locale.US)
            as MetricValueUiState.Plain
        assertEquals(UiText.Verbatim("780"), balance.figure)
        assertEquals(uiText(R.string.today_kcal_in_out), balance.caption)

        val heartRate = resolveMetricPresentation(TodayMetric.RestingHeartRate, snapshot(), Locale.US)
            as MetricValueUiState.Plain
        assertEquals(uiText(R.string.today_value_bpm, text("58")), heartRate.figure)
        assertEquals(uiText(R.string.today_resting), heartRate.caption)
    }

    private fun snapshot(
        caloriesKcal: Double = 1_200.0,
        calorieGoalKcal: Double = 2_000.0,
        proteinGrams: Double = 80.0,
        proteinGoalGrams: Double = 150.0,
        waterMl: Double = 1_000.0,
        waterGoalMl: Double = 2_000.0,
        steps: Long? = 8_200L,
        stepGoal: Long = 10_000L,
        latestWeightKg: Double? = 82.4,
        weightDeltaKg: Double? = -0.4,
        bodyFatPercent: Double? = 18.5,
    ) = MetricSnapshot(
        caloriesKcal = caloriesKcal,
        calorieGoalKcal = calorieGoalKcal,
        proteinGrams = proteinGrams,
        proteinGoalGrams = proteinGoalGrams,
        carbsGrams = 130.0,
        carbsGoalGrams = 200.0,
        fatGrams = 40.0,
        fatGoalGrams = 60.0,
        waterMl = waterMl,
        waterGoalMl = waterGoalMl,
        steps = steps,
        stepGoal = stepGoal,
        latestWeightKg = latestWeightKg,
        weightDeltaKg = weightDeltaKg,
        bodyFatPercent = bodyFatPercent,
        bodyFatDelta = -0.3,
        sessionsDone = 2,
        sessionTarget = 4,
        activeCaloriesKcal = 420.0,
        sleepMinutes = 465L,
        exerciseMinutes = 68L,
        exerciseSessionCount = 2,
        restingHeartRateBpm = 58L,
        loggingStreakDays = 5,
    )

    private fun text(value: String) = UiText.Argument.Text(value)
}
