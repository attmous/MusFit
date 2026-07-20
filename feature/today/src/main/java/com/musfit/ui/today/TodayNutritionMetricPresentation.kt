package com.musfit.ui.today

import com.musfit.domain.today.MetricSnapshot
import com.musfit.domain.today.TodayMetric
import com.musfit.feature.today.R
import com.musfit.ui.text.UiText
import com.musfit.ui.text.uiText
import java.util.Locale
import kotlin.math.roundToInt

internal fun resolveNutritionMetric(
    metric: TodayMetric,
    snapshot: MetricSnapshot,
    locale: Locale,
): MetricValueUiState = when (metric) {
    TodayMetric.Calories -> if (snapshot.calorieGoalKcal > 0.0) {
        metricWithGoal(
            figure = integerText(snapshot.caloriesKcal.roundToInt().toLong(), locale),
            caption = uiText(
                R.string.today_of_kcal_percent,
                textArgument(snapshot.calorieGoalKcal.roundToInt().toLong(), locale),
                textArgument(percent(snapshot.caloriesKcal, snapshot.calorieGoalKcal).toLong(), locale),
            ),
            value = snapshot.caloriesKcal,
            goal = snapshot.calorieGoalKcal,
        )
    } else {
        MetricValueUiState.Plain(integerText(snapshot.caloriesKcal.roundToInt().toLong(), locale), uiText(R.string.today_kcal_eaten))
    }

    TodayMetric.Protein -> gramMetric(snapshot.proteinGrams, snapshot.proteinGoalGrams, locale)

    TodayMetric.Carbs -> gramMetric(snapshot.carbsGrams, snapshot.carbsGoalGrams, locale)

    TodayMetric.Fat -> gramMetric(snapshot.fatGrams, snapshot.fatGoalGrams, locale)

    TodayMetric.Water -> waterMetric(snapshot, locale)

    else -> error("Non-nutrition metric: ${metric.id}")
}

private fun gramMetric(value: Double, goal: Double, locale: Locale): MetricValueUiState {
    val figure = uiText(R.string.today_value_grams, textArgument(value.roundToInt().toLong(), locale))
    return if (goal > 0.0) {
        metricWithGoal(
            figure = figure,
            caption = uiText(
                R.string.today_of_grams_percent,
                textArgument(goal.roundToInt().toLong(), locale),
                textArgument(percent(value, goal).toLong(), locale),
            ),
            value = value,
            goal = goal,
        )
    } else {
        MetricValueUiState.Plain(figure, uiText(R.string.today_today))
    }
}

private fun waterMetric(snapshot: MetricSnapshot, locale: Locale): MetricValueUiState = if (snapshot.waterGoalMl > 0.0) {
    metricWithGoal(
        figure = litersText(snapshot.waterMl, locale),
        caption = uiText(
            R.string.today_of_value_percent,
            UiText.Argument.Nested(litersText(snapshot.waterGoalMl, locale)),
            textArgument(percent(snapshot.waterMl, snapshot.waterGoalMl).toLong(), locale),
        ),
        value = snapshot.waterMl,
        goal = snapshot.waterGoalMl,
    )
} else {
    MetricValueUiState.Plain(litersText(snapshot.waterMl, locale), uiText(R.string.today_today))
}
