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
}
