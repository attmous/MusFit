package com.musfit.domain.profile

import kotlin.math.roundToInt

object BodyMetricsCalculator {
    fun bodyMassIndex(weightKg: Double, heightCm: Double): Double? {
        if (weightKg <= 0.0 || heightCm <= 0.0) return null
        val heightM = heightCm / 100.0
        return (weightKg / (heightM * heightM) * 10.0).roundToInt() / 10.0
    }

    fun goalProgressFraction(startWeightKg: Double, currentWeightKg: Double, goalWeightKg: Double): Double? {
        val total = startWeightKg - goalWeightKg
        if (total == 0.0) return null
        return ((startWeightKg - currentWeightKg) / total).coerceIn(0.0, 1.0)
    }

    fun changeOverWindow(points: List<Pair<Long, Double>>, windowMillis: Long, nowMillis: Long): Double? {
        if (points.size < 2) return null
        val sorted = points.sortedBy { it.first }
        val latest = sorted.last()
        val cutoff = nowMillis - windowMillis
        val baseline = sorted.lastOrNull { it.first <= cutoff } ?: sorted.first()
        if (baseline.first == latest.first) return null
        return latest.second - baseline.second
    }
}
