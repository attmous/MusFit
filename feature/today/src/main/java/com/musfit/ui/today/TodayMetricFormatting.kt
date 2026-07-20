package com.musfit.ui.today

import com.musfit.feature.today.R
import com.musfit.ui.text.LocalizedFormatter
import com.musfit.ui.text.UiText
import com.musfit.ui.text.uiText
import java.util.Locale
import kotlin.math.roundToInt

internal fun metricWithGoal(figure: UiText, caption: UiText, value: Double, goal: Double) = MetricValueUiState.WithGoal(figure, caption, ratio(value, goal))

internal fun integerText(value: Long, locale: Locale): UiText = UiText.Verbatim(LocalizedFormatter.integer(value, locale = locale))

internal fun textArgument(value: Long, locale: Locale) = UiText.Argument.Text(LocalizedFormatter.integer(value, locale = locale))

internal fun textArgument(value: Double, locale: Locale) = UiText.Argument.Text(LocalizedFormatter.number(value, maximumFractionDigits = 1, locale = locale))

internal fun litersText(milliliters: Double, locale: Locale): UiText = uiText(R.string.today_value_liters, textArgument(milliliters / 1000.0, locale))

internal fun durationText(minutes: Long, locale: Locale): UiText {
    val safe = minutes.coerceAtLeast(0L)
    val hours = safe / 60L
    val remainder = safe % 60L
    return if (hours > 0L) {
        uiText(
            R.string.today_duration_hours_minutes,
            textArgument(hours, locale),
            UiText.Argument.Text(
                LocalizedFormatter.integer(
                    remainder,
                    grouping = false,
                    locale = locale,
                    minimumIntegerDigits = 2,
                ),
            ),
        )
    } else {
        uiText(R.string.today_duration_minutes, textArgument(remainder, locale))
    }
}

internal fun percent(value: Double, goal: Double): Int = (value / goal * 100.0).roundToInt()

internal fun ratio(value: Double, goal: Double): Float = if (goal <= 0.0) 0f else (value / goal).coerceIn(0.0, 1.0).toFloat()
