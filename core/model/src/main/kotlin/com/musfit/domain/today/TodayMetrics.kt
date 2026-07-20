package com.musfit.domain.today

/** The pool of pinnable Today metrics. [id] is the persisted, nonlocalized identity. */
enum class TodayMetric(val id: String) {
    Calories("calories"),
    Protein("protein"),
    Carbs("carbs"),
    Fat("fat"),
    Water("water"),
    Steps("steps"),
    Weight("weight"),
    BodyFat("body_fat"),
    Sessions("sessions"),
    Sleep("sleep"),
    Exercise("exercise"),
    ActiveCalories("active_calories"),
    RestingHeartRate("resting_hr"),
    CalorieBalance("calorie_balance"),
    LoggingStreak("streak"),
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

/** Locale-neutral measurements assembled by the ViewModel for presentation. */
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
    val restingHeartRateBpm: Long?,
    val loggingStreakDays: Int,
)

/** Every pinned metric is one tile, rendered two per row in pin order. */
fun vitalsTileMetrics(pins: List<TodayMetric>): List<TodayMetric> = pins.ifEmpty { TodayMetric.DEFAULT_PINS }

/** Sessions-metric week: Monday-start, [weekStartMillis, weekStartMillis + 7 days). */
fun countSessionsInWeek(sessionStartMillis: List<Long>, weekStartMillis: Long): Int = sessionStartMillis.count { it >= weekStartMillis && it < weekStartMillis + 7 * 86_400_000L }
