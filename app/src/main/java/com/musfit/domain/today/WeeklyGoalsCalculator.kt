package com.musfit.domain.today

/** Pure weekly-goal rollup for the Today home. All inputs are primitives so the domain stays Android-free. */
data class WeeklyGoals(
    val sessionsDone: Int,
    val sessionTarget: Int,
    val calorieOnTargetDays: Int,
    val trackedDays: Int,
    val stepGoalDays: Int,
    val weightAvgKg: Double?,
    val weightDeltaKg: Double?,
    val targetWeightKg: Double?,
)

object WeeklyGoalsCalculator {
    private const val DAY_MILLIS = 86_400_000L
    private const val WEEK_MILLIS = 7L * DAY_MILLIS
    private const val LOWER = 0.85
    private const val UPPER = 1.15
    const val TRACKED_DAYS = 7

    fun compute(
        weekStartMillis: Long,
        sessionStartMillis: List<Long>,
        sessionTarget: Int,
        loggedCaloriesPerDay: List<Double?>,
        calorieGoalKcal: Double,
        stepsPerDay: List<Long>,
        stepGoal: Long,
        weights: List<Pair<Long, Double>>,
        targetWeightKg: Double,
    ): WeeklyGoals {
        val weekEndMillis = weekStartMillis + WEEK_MILLIS

        val sessionsDone = sessionStartMillis.count { it in weekStartMillis until weekEndMillis }

        val calorieOnTargetDays =
            if (calorieGoalKcal <= 0.0) {
                0
            } else {
                val band = (calorieGoalKcal * LOWER)..(calorieGoalKcal * UPPER)
                loggedCaloriesPerDay.count { it != null && it in band }
            }

        val stepGoalDays = if (stepGoal <= 0L) 0 else stepsPerDay.count { it >= stepGoal }

        val (thisWeekAvg, weightDelta) = weightTrend(weights, weekStartMillis)

        return WeeklyGoals(
            sessionsDone = sessionsDone,
            sessionTarget = sessionTarget,
            calorieOnTargetDays = calorieOnTargetDays,
            trackedDays = TRACKED_DAYS,
            stepGoalDays = stepGoalDays,
            weightAvgKg = thisWeekAvg,
            weightDeltaKg = weightDelta,
            targetWeightKg = targetWeightKg.takeIf { it > 0.0 },
        )
    }

    /** (thisWeekAvgKg, deltaVsPriorWeekKg) — either may be null when there is no data to average. */
    fun weightTrend(weights: List<Pair<Long, Double>>, weekStartMillis: Long): Pair<Double?, Double?> {
        val weekEndMillis = weekStartMillis + WEEK_MILLIS
        val priorStartMillis = weekStartMillis - WEEK_MILLIS
        val thisWeekAvg = weights.filter { it.first in weekStartMillis until weekEndMillis }.map { it.second }.averageOrNull()
        val priorWeekAvg = weights.filter { it.first in priorStartMillis until weekStartMillis }.map { it.second }.averageOrNull()
        val delta = if (thisWeekAvg != null && priorWeekAvg != null) thisWeekAvg - priorWeekAvg else null
        return thisWeekAvg to delta
    }

    private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()
}
