package com.musfit.ui.today

import com.musfit.domain.today.MetricSnapshot
import com.musfit.domain.today.TodayMetric
import com.musfit.feature.today.R
import com.musfit.ui.text.UiText
import com.musfit.ui.text.uiText
import java.util.Locale

sealed interface MetricValueUiState {
    data class WithGoal(val figure: UiText, val caption: UiText, val progress: Float) : MetricValueUiState
    data class Plain(val figure: UiText, val caption: UiText) : MetricValueUiState
    data class NoData(val caption: UiText) : MetricValueUiState
}

private val metricLabelResources = mapOf(
    TodayMetric.Calories to R.string.today_metric_eaten,
    TodayMetric.Protein to R.string.today_metric_protein,
    TodayMetric.Carbs to R.string.today_metric_carbs,
    TodayMetric.Fat to R.string.today_metric_fat,
    TodayMetric.Water to R.string.today_metric_water,
    TodayMetric.Steps to R.string.today_metric_steps,
    TodayMetric.Weight to R.string.today_metric_weight,
    TodayMetric.BodyFat to R.string.today_metric_body_fat,
    TodayMetric.Sessions to R.string.today_metric_sessions,
    TodayMetric.Sleep to R.string.today_metric_sleep,
    TodayMetric.Exercise to R.string.today_metric_exercise,
    TodayMetric.ActiveCalories to R.string.today_metric_active_calories,
    TodayMetric.RestingHeartRate to R.string.today_metric_resting_heart_rate,
    TodayMetric.CalorieBalance to R.string.today_metric_balance,
    TodayMetric.LoggingStreak to R.string.today_metric_logging_streak,
)

private val nutritionMetrics = setOf(
    TodayMetric.Calories,
    TodayMetric.Protein,
    TodayMetric.Carbs,
    TodayMetric.Fat,
    TodayMetric.Water,
)

private val bodyMetrics = setOf(TodayMetric.Steps, TodayMetric.Weight, TodayMetric.BodyFat)

internal fun TodayMetric.presentationLabel(): UiText = uiText(metricLabelResources.getValue(this))

internal fun resolveMetricPresentation(
    metric: TodayMetric,
    snapshot: MetricSnapshot,
    locale: Locale = Locale.getDefault(),
): MetricValueUiState = when (metric) {
    in nutritionMetrics -> resolveNutritionMetric(metric, snapshot, locale)
    in bodyMetrics -> resolveBodyMetric(metric, snapshot, locale)
    else -> resolveActivityMetric(metric, snapshot, locale)
}
