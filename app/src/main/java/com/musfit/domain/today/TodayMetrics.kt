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
    ActiveCalories("active_calories", "Active kcal"),
    RestingHeartRate("resting_hr", "Resting HR"),
    CalorieBalance("calorie_balance", "Balance"),
    LoggingStreak("streak", "Streak"),
    ;

    companion object {
        fun fromId(id: String): TodayMetric? = entries.firstOrNull { it.id == id }

        /** Fresh-install / defensive-fallback pins; also seeded by migration 25→26. */
        val DEFAULT_PINS = listOf(Calories, Steps, Protein)
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
    val restingHeartRateBpm: Long?, // Long to match DailyHealthSummaryEntity
    val loggingStreakDays: Int,
)

/** Pure mapping of one metric + snapshot → display state (spec §1 formulas). */
object MetricResolver {
    fun resolve(metric: TodayMetric, s: MetricSnapshot): MetricValue = when (metric) {
        TodayMetric.Calories ->
            if (s.calorieGoalKcal > 0.0) {
                val remaining = s.calorieGoalKcal - s.caloriesKcal
                MetricValue.WithGoal(
                    // Grouped figure ("1,450"); over-goal shows the overage instead of clamping to 0.
                    figure = formatGrouped(abs(remaining).roundToInt()),
                    caption = if (remaining >= 0.0) "kcal left" else "kcal over",
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
                    figure = "${s.waterMl.roundToInt()} ml",
                    caption = "of ${s.waterGoalMl.roundToInt()} ml",
                    progress = ratio(s.waterMl, s.waterGoalMl),
                )
            } else {
                MetricValue.Plain("${s.waterMl.roundToInt()} ml", "today")
            }
        TodayMetric.Steps -> {
            val steps = s.steps ?: return MetricValue.NoData("Not connected")
            if (s.stepGoal > 0L) {
                MetricValue.WithGoal(
                    figure = formatCount(steps),
                    caption = "of ${formatCount(s.stepGoal)}",
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
                caption = "of ${goalGrams.roundToInt()} g",
                progress = ratio(grams, goalGrams),
            )
        } else {
            MetricValue.Plain("${grams.roundToInt()} g", "today")
        }

    private fun ratio(value: Double, goal: Double): Float =
        if (goal <= 0.0) 0f else (value / goal).coerceIn(0.0, 1.0).toFloat()

    private fun formatGrouped(value: Int): String = String.format(Locale.US, "%,d", value)

    private fun formatCount(value: Long): String =
        if (value >= 1000) {
            val thousands = value / 1000.0
            if (thousands % 1.0 == 0.0) "${thousands.toInt()}k" else String.format(Locale.US, "%.1fk", thousands)
        } else {
            value.toString()
        }

    private fun Double.format1(): String =
        if (this % 1.0 == 0.0) toInt().toString() else String.format(Locale.US, "%.1f", this)
}

/** One derived carousel page: page 1 carries the hero, overflow pages are chip grids. */
data class CarouselPage(val hero: TodayMetric?, val chips: List<TodayMetric>)

/**
 * Pure page derivation (spec §1): page 1 = pins[0] hero + pins[1..2] chips;
 * pages 2+ = remaining pins in chunks of four. Empty pins fall back to defaults.
 */
fun buildCarouselPages(pins: List<TodayMetric>): List<CarouselPage> {
    val effective = pins.ifEmpty { TodayMetric.DEFAULT_PINS }
    val heroPage = CarouselPage(hero = effective[0], chips = effective.drop(1).take(2))
    val overflow = effective.drop(3).chunked(4).map { CarouselPage(hero = null, chips = it) }
    return listOf(heroPage) + overflow
}

/**
 * Sessions-metric week: Monday-start, [weekStartMillis, weekStartMillis + 7 days) —
 * Today's existing convention (recorded deviation #3), pinned here by test.
 */
fun countSessionsInWeek(sessionStartMillis: List<Long>, weekStartMillis: Long): Int =
    sessionStartMillis.count { it >= weekStartMillis && it < weekStartMillis + 7 * 86_400_000L }
