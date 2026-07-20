package com.musfit.ui.today

import com.musfit.domain.today.MetricSnapshot
import com.musfit.domain.today.TodayMetric
import com.musfit.feature.today.R
import com.musfit.ui.text.pluralUiText
import com.musfit.ui.text.uiText
import java.util.Locale
import kotlin.math.roundToInt

internal fun resolveActivityMetric(
    metric: TodayMetric,
    snapshot: MetricSnapshot,
    locale: Locale,
): MetricValueUiState = when (metric) {
    TodayMetric.Sessions -> sessionsMetric(snapshot, locale)
    TodayMetric.Sleep -> sleepMetric(snapshot, locale)
    TodayMetric.Exercise -> exerciseMetric(snapshot, locale)
    TodayMetric.ActiveCalories -> activeCaloriesMetric(snapshot, locale)
    TodayMetric.RestingHeartRate -> restingHeartRateMetric(snapshot, locale)
    TodayMetric.CalorieBalance -> calorieBalanceMetric(snapshot, locale)
    TodayMetric.LoggingStreak -> loggingStreakMetric(snapshot, locale)
    else -> error("Non-activity metric: ${metric.id}")
}

private fun sessionsMetric(snapshot: MetricSnapshot, locale: Locale): MetricValueUiState = if (snapshot.sessionTarget > 0) {
    MetricValueUiState.WithGoal(
        figure = uiText(
            R.string.today_sessions_fraction,
            textArgument(snapshot.sessionsDone.toLong(), locale),
            textArgument(snapshot.sessionTarget.toLong(), locale),
        ),
        caption = uiText(R.string.today_this_week),
        progress = ratio(snapshot.sessionsDone.toDouble(), snapshot.sessionTarget.toDouble()),
    )
} else {
    MetricValueUiState.Plain(integerText(snapshot.sessionsDone.toLong(), locale), uiText(R.string.today_this_week))
}

private fun exerciseMetric(snapshot: MetricSnapshot, locale: Locale): MetricValueUiState = snapshot.exerciseMinutes?.let { minutes ->
    val caption = snapshot.exerciseSessionCount?.let { count ->
        pluralUiText(R.plurals.today_sessions, count, textArgument(count.toLong(), locale))
    } ?: uiText(R.string.today_exercise)
    MetricValueUiState.Plain(durationText(minutes, locale), caption)
} ?: MetricValueUiState.NoData(uiText(R.string.today_not_connected))

private fun sleepMetric(snapshot: MetricSnapshot, locale: Locale): MetricValueUiState = snapshot.sleepMinutes?.let {
    MetricValueUiState.Plain(durationText(it, locale), uiText(R.string.today_sleep))
} ?: MetricValueUiState.NoData(uiText(R.string.today_not_connected))

private fun activeCaloriesMetric(snapshot: MetricSnapshot, locale: Locale): MetricValueUiState = snapshot.activeCaloriesKcal?.let {
    MetricValueUiState.Plain(integerText(it.roundToInt().toLong(), locale), uiText(R.string.today_active_kcal))
} ?: MetricValueUiState.NoData(uiText(R.string.today_not_connected))

private fun restingHeartRateMetric(snapshot: MetricSnapshot, locale: Locale): MetricValueUiState = snapshot.restingHeartRateBpm?.let {
    MetricValueUiState.Plain(uiText(R.string.today_value_bpm, textArgument(it, locale)), uiText(R.string.today_resting))
} ?: MetricValueUiState.NoData(uiText(R.string.today_not_connected))

private fun calorieBalanceMetric(snapshot: MetricSnapshot, locale: Locale): MetricValueUiState = snapshot.activeCaloriesKcal?.let {
    MetricValueUiState.Plain(
        integerText((snapshot.caloriesKcal - it).roundToInt().toLong(), locale),
        uiText(R.string.today_kcal_in_out),
    )
} ?: MetricValueUiState.NoData(uiText(R.string.today_not_connected))

private fun loggingStreakMetric(snapshot: MetricSnapshot, locale: Locale) = MetricValueUiState.Plain(
    uiText(R.string.today_value_days_short, textArgument(snapshot.loggingStreakDays.toLong(), locale)),
    uiText(R.string.today_logging_streak),
)
