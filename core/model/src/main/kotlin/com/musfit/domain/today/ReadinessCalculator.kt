package com.musfit.domain.today

import kotlin.math.roundToInt

data class DailyReadinessSample(
    val epochDay: Long,
    val sleepMinutes: Long?,
    val restingHeartRateBpm: Long?,
    val hrvRmssdMillis: Double?,
)

data class ReadinessScore(
    val score: Int,
    val level: ReadinessLevel,
)

enum class ReadinessLevel(val label: String) {
    Low("Low"),
    Moderate("Moderate"),
    High("High"),
}

/**
 * MusFit's local readiness estimate. Google Health's proprietary score is not exposed as a
 * Health Connect record, so this uses the same public signal family: sleep, HRV, and RHR.
 */
object ReadinessCalculator {
    private const val MIN_BASELINE_SAMPLES = 3
    private const val SLEEP_TARGET_MINUTES = 480.0

    fun resolve(today: DailyReadinessSample?, recent: List<DailyReadinessSample>): ReadinessScore? {
        today ?: return null
        val sleep = today.sleepMinutes?.takeIf { it > 0L }?.toDouble() ?: return null
        val rhr = today.restingHeartRateBpm?.takeIf { it > 0L }?.toDouble() ?: return null
        val hrv = today.hrvRmssdMillis?.takeIf { it > 0.0 } ?: return null

        val previous = recent.filter { it.epochDay < today.epochDay }
        val rhrBaseline = previous.mapNotNull { it.restingHeartRateBpm?.takeIf { value -> value > 0L } }
            .takeIf { it.size >= MIN_BASELINE_SAMPLES }
            ?.average()
            ?: return null
        val hrvBaseline = previous.mapNotNull { it.hrvRmssdMillis?.takeIf { value -> value > 0.0 } }
            .takeIf { it.size >= MIN_BASELINE_SAMPLES }
            ?.average()
            ?: return null

        val sleepScore = (sleep / SLEEP_TARGET_MINUTES * 100.0).coerceIn(0.0, 100.0)
        val hrvScore = recoveryRatioScore(hrv / hrvBaseline)
        val rhrScore = recoveryRatioScore(rhrBaseline / rhr)
        val score = (sleepScore * 0.40 + hrvScore * 0.35 + rhrScore * 0.25)
            .roundToInt()
            .coerceIn(0, 100)

        return ReadinessScore(score = score, level = score.toReadinessLevel())
    }

    private fun recoveryRatioScore(ratio: Double): Double = (50.0 + (ratio - 1.0) * 150.0).coerceIn(0.0, 100.0)

    private fun Int.toReadinessLevel(): ReadinessLevel = when {
        this >= 65 -> ReadinessLevel.High
        this >= 30 -> ReadinessLevel.Moderate
        else -> ReadinessLevel.Low
    }
}
