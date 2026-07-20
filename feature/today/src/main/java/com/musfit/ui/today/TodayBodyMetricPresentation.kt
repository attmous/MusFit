package com.musfit.ui.today

import com.musfit.domain.today.MetricSnapshot
import com.musfit.domain.today.TodayMetric
import com.musfit.feature.today.R
import com.musfit.ui.text.UiText
import com.musfit.ui.text.uiText
import java.util.Locale
import kotlin.math.abs

internal fun resolveBodyMetric(
    metric: TodayMetric,
    snapshot: MetricSnapshot,
    locale: Locale,
): MetricValueUiState = when (metric) {
    TodayMetric.Steps -> stepsMetric(snapshot, locale)
    TodayMetric.Weight -> weightMetric(snapshot, locale)
    TodayMetric.BodyFat -> bodyFatMetric(snapshot, locale)
    else -> error("Non-body metric: ${metric.id}")
}

private fun stepsMetric(snapshot: MetricSnapshot, locale: Locale): MetricValueUiState = snapshot.steps?.let { steps ->
    if (snapshot.stepGoal > 0L) {
        metricWithGoal(
            figure = integerText(steps, locale),
            caption = uiText(
                R.string.today_of_value_percent,
                UiText.Argument.Nested(integerText(snapshot.stepGoal, locale)),
                textArgument(percent(steps.toDouble(), snapshot.stepGoal.toDouble()).toLong(), locale),
            ),
            value = steps.toDouble(),
            goal = snapshot.stepGoal.toDouble(),
        )
    } else {
        MetricValueUiState.Plain(integerText(steps, locale), uiText(R.string.today_steps))
    }
} ?: MetricValueUiState.NoData(uiText(R.string.today_not_connected))

private fun weightMetric(snapshot: MetricSnapshot, locale: Locale): MetricValueUiState = snapshot.latestWeightKg?.let { weight ->
    val figure = uiText(R.string.today_value_kg, textArgument(weight, locale))
    val caption = snapshot.weightDeltaKg?.let { delta ->
        uiText(
            R.string.today_weight_delta_7_days,
            UiText.Argument.Text(if (delta < 0) "\u2212" else "+"),
            textArgument(abs(delta), locale),
        )
    } ?: uiText(R.string.today_latest)
    MetricValueUiState.Plain(figure, caption)
} ?: MetricValueUiState.NoData(uiText(R.string.today_no_data))

private fun bodyFatMetric(snapshot: MetricSnapshot, locale: Locale): MetricValueUiState = snapshot.bodyFatPercent?.let { bodyFat ->
    val caption = snapshot.bodyFatDelta?.let { delta ->
        uiText(
            R.string.today_body_fat_delta,
            UiText.Argument.Text(if (delta < 0) "\u2212" else "+"),
            textArgument(abs(delta), locale),
        )
    } ?: uiText(R.string.today_latest)
    MetricValueUiState.Plain(uiText(R.string.today_value_percent, textArgument(bodyFat, locale)), caption)
} ?: MetricValueUiState.NoData(uiText(R.string.today_no_data))
