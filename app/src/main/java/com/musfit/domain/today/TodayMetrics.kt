package com.musfit.domain.today

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** The pool of pinnable Today-carousel metrics. [id] is the persisted identity. */
enum class TodayMetric(val id: String, val label: String) {
    Calories("calories", "Calories"),
    Protein("protein", "Protein"),
    Carbs("carbs", "Carbs"),
    Fat("fat", "Fat"),
    Water("water", "Water"),
    Steps("steps", "Steps"),
    Weight("weight", "Weight"),
    BodyFat("body_fat", "Body fat"),
    Sessions("sessions", "Sessions"),
    Sleep("sleep", "Sleep"),
    Exercise("exercise", "Exercise"),
    ActiveCalories("active_calories", "Active kcal"),
    RestingHeartRate("resting_hr", "Resting HR"),
    CalorieBalance("calorie_balance", "Balance"),
    LoggingStreak("streak", "Streak"),
    ;

    companion object {
        fun fromId(id: String): TodayMetric? = entries.firstOrNull { it.id == id }

        /** Fresh-install / defensive-fallback pins — the Turn 8 vitals-grid four.
         *  Seeded by migration 25→26; migration 35→36 appends water for users
         *  still on the untouched three-pin default. */
        val DEFAULT_PINS = listOf(Calories, Steps, Protein, Water)
    }
}

/** Consecutive logged days counting back from today (from yesterday if today is still empty). */
object LoggingStreakCalculator {
    fun streakDays(loggedEpochDays: List<Long>, todayEpochDay: Long): Int {
        val logged = loggedEpochDays.toHashSet()
        var cursor = if (todayEpochDay in logged) todayEpochDay else todayEpochDay - 1
        var streak = 0
        while (cursor in logged) {
            streak++
            cursor--
        }
        return streak
    }
}

/** The three display states every carousel metric resolves to. */
sealed interface MetricValue {
    data class WithGoal(val figure: String, val caption: String, val progress: Float) : MetricValue

    data class Plain(val figure: String, val caption: String) : MetricValue

    data class NoData(val caption: String) : MetricValue
}

/** Everything the resolver needs, assembled by the ViewModel from live flows. */
data class MetricSnapshot(
    val caloriesKcal: Double,
    val calorieGoalKcal: Double,
    val proteinGrams: Double,
    val proteinGoalGrams: Double,
    val carbsGrams: Double,
    val carbsGoalGrams: Double,
    val fatGrams: Double,
    val fatGoalGrams: Double,
    val waterMl: Double,
    val waterGoalMl: Double,
    val steps: Long?,
    val stepGoal: Long,
    val latestWeightKg: Double?,
    val weightDeltaKg: Double?,
    val bodyFatPercent: Double?,
    val bodyFatDelta: Double?,
    val sessionsDone: Int,
    val sessionTarget: Int,
    val activeCaloriesKcal: Double?,
    val sleepMinutes: Long?,
    val exerciseMinutes: Long?,
    val exerciseSessionCount: Int?,
    val restingHeartRateBpm: Long?, // Long to match DailyHealthSummaryEntity
    val loggingStreakDays: Int,
)

/** Pure mapping of one metric + snapshot → display state (spec §1 formulas). */
object MetricResolver {
    fun resolve(metric: TodayMetric, s: MetricSnapshot): MetricValue = when (metric) {
        TodayMetric.Calories ->
            if (s.calorieGoalKcal > 0.0) {
                // Turn 8 vitals tile: the EATEN figure with the full goal spelled
                // out ("of 2,450 kcal", never "2.5k") and the unclamped percent.
                MetricValue.WithGoal(
                    figure = formatGrouped(s.caloriesKcal.roundToInt()),
                    caption = "of ${formatGrouped(s.calorieGoalKcal.roundToInt())} kcal · ${percent(s.caloriesKcal, s.calorieGoalKcal)}%",
                    progress = ratio(s.caloriesKcal, s.calorieGoalKcal),
                )
            } else {
                MetricValue.Plain(formatGrouped(s.caloriesKcal.roundToInt()), "kcal eaten")
            }
        TodayMetric.Protein -> gramMetric(s.proteinGrams, s.proteinGoalGrams)
        TodayMetric.Carbs -> gramMetric(s.carbsGrams, s.carbsGoalGrams)
        TodayMetric.Fat -> gramMetric(s.fatGrams, s.fatGoalGrams)
        TodayMetric.Water ->
            if (s.waterGoalMl > 0.0) {
                MetricValue.WithGoal(
                    figure = formatLiters(s.waterMl),
                    caption = "of ${formatLiters(s.waterGoalMl)} · ${percent(s.waterMl, s.waterGoalMl)}%",
                    progress = ratio(s.waterMl, s.waterGoalMl),
                )
            } else {
                MetricValue.Plain(formatLiters(s.waterMl), "today")
            }
        TodayMetric.Steps -> {
            val steps = s.steps ?: return MetricValue.NoData("Not connected")
            if (s.stepGoal > 0L) {
                MetricValue.WithGoal(
                    figure = formatCount(steps),
                    caption = "of ${formatCount(s.stepGoal)} · ${percent(steps.toDouble(), s.stepGoal.toDouble())}%",
                    progress = ratio(steps.toDouble(), s.stepGoal.toDouble()),
                )
            } else {
                MetricValue.Plain(formatCount(steps), "steps")
            }
        }
        TodayMetric.Weight -> {
            val weight = s.latestWeightKg ?: return MetricValue.NoData("No data")
            val caption = s.weightDeltaKg?.let { delta ->
                val sign = if (delta < 0) "−" else "+"
                "$sign${abs(delta).format1()} kg · 7d"
            } ?: "latest"
            MetricValue.Plain("${weight.format1()} kg", caption)
        }
        TodayMetric.BodyFat -> {
            val bodyFat = s.bodyFatPercent ?: return MetricValue.NoData("No data")
            val caption = s.bodyFatDelta?.let { delta ->
                val sign = if (delta < 0) "−" else "+"
                "$sign${abs(delta).format1()} pts"
            } ?: "latest"
            MetricValue.Plain("${bodyFat.format1()}%", caption)
        }
        TodayMetric.Sessions ->
            if (s.sessionTarget > 0) {
                MetricValue.WithGoal(
                    figure = "${s.sessionsDone}/${s.sessionTarget}",
                    caption = "this week",
                    progress = ratio(s.sessionsDone.toDouble(), s.sessionTarget.toDouble()),
                )
            } else {
                MetricValue.Plain(s.sessionsDone.toString(), "this week")
            }
        TodayMetric.Sleep -> {
            val minutes = s.sleepMinutes ?: return MetricValue.NoData("Not connected")
            MetricValue.Plain(formatDuration(minutes), "sleep")
        }
        TodayMetric.Exercise -> {
            val minutes = s.exerciseMinutes ?: return MetricValue.NoData("Not connected")
            val caption = when (val count = s.exerciseSessionCount) {
                null -> "exercise"
                1 -> "1 session"
                else -> "$count sessions"
            }
            MetricValue.Plain(formatDuration(minutes), caption)
        }
        TodayMetric.ActiveCalories -> {
            val active = s.activeCaloriesKcal ?: return MetricValue.NoData("Not connected")
            MetricValue.Plain(active.roundToInt().toString(), "active kcal")
        }
        TodayMetric.RestingHeartRate -> {
            val bpm = s.restingHeartRateBpm ?: return MetricValue.NoData("Not connected")
            MetricValue.Plain("$bpm bpm", "resting")
        }
        TodayMetric.CalorieBalance -> {
            val active = s.activeCaloriesKcal ?: return MetricValue.NoData("Not connected")
            MetricValue.Plain((s.caloriesKcal - active).roundToInt().toString(), "kcal in − out")
        }
        TodayMetric.LoggingStreak ->
            MetricValue.Plain("${s.loggingStreakDays} d", "logging streak")
    }

    private fun gramMetric(grams: Double, goalGrams: Double): MetricValue =
        if (goalGrams > 0.0) {
            MetricValue.WithGoal(
                figure = "${grams.roundToInt()} g",
                caption = "of ${goalGrams.roundToInt()} g · ${percent(grams, goalGrams)}%",
                progress = ratio(grams, goalGrams),
            )
        } else {
            MetricValue.Plain("${grams.roundToInt()} g", "today")
        }

    private fun ratio(value: Double, goal: Double): Float =
        if (goal <= 0.0) 0f else (value / goal).coerceIn(0.0, 1.0).toFloat()

    /** Unclamped display percent — over-goal reads "112%", the wave still caps at full. */
    private fun percent(value: Double, goal: Double): Int = (value / goal * 100.0).roundToInt()

    private fun formatGrouped(value: Int): String = String.format(Locale.US, "%,d", value)

    private fun formatCount(value: Long): String =
        String.format(Locale.US, "%,d", value).replace(',', '.')

    /** Water reads in liters on the vitals tile ("1.5 L"); whole liters drop the decimal. */
    private fun formatLiters(milliliters: Double): String {
        val liters = milliliters / 1000.0
        val text = if (liters % 1.0 == 0.0) liters.toInt().toString() else String.format(Locale.US, "%.1f", liters)
        return "$text L"
    }

    private fun formatDuration(minutes: Long): String {
        val safeMinutes = minutes.coerceAtLeast(0L)
        val hours = safeMinutes / 60L
        val remainder = safeMinutes % 60L
        return if (hours > 0L) {
            "${hours}h ${remainder.toString().padStart(2, '0')}m"
        } else {
            "${remainder}m"
        }
    }

    private fun Double.format1(): String =
        if (this % 1.0 == 0.0) toInt().toString() else String.format(Locale.US, "%.1f", this)
}

/**
 * The Turn 8 vitals grid replaces the hero carousel: every pinned metric is one
 * tile, rendered two per row in pin order. Empty pins fall back to the default
 * four (eaten kcal, steps, protein, water).
 */
fun vitalsTileMetrics(pins: List<TodayMetric>): List<TodayMetric> =
    pins.ifEmpty { TodayMetric.DEFAULT_PINS }

/**
 * Sessions-metric week: Monday-start, [weekStartMillis, weekStartMillis + 7 days) —
 * Today's existing convention (recorded deviation #3), pinned here by test.
 */
fun countSessionsInWeek(sessionStartMillis: List<Long>, weekStartMillis: Long): Int =
    sessionStartMillis.count { it >= weekStartMillis && it < weekStartMillis + 7 * 86_400_000L }
