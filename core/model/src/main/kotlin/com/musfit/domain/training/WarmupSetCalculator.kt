package com.musfit.domain.training

import kotlin.math.round

data class WarmupSetSuggestion(
    val weightKg: Double,
    val reps: Int,
)

object WarmupSetCalculator {
    private val steps = listOf(
        0.50 to 8,
        0.70 to 5,
        0.85 to 3,
    )

    fun suggestions(
        workingWeightKg: Double?,
        workingReps: Int?,
        barWeightKg: Double = 20.0,
    ): List<WarmupSetSuggestion> {
        val workWeight = workingWeightKg ?: return emptyList()
        if (workingReps == null || workingReps <= 0 || workWeight <= barWeightKg) return emptyList()
        return steps
            .map { (percentage, reps) ->
                WarmupSetSuggestion(
                    weightKg = roundToNearestPlateIncrement(workWeight * percentage, barWeightKg),
                    reps = reps,
                )
            }
            .filter { it.weightKg >= barWeightKg && it.weightKg < workWeight }
            .distinctBy { it.weightKg }
    }

    private fun roundToNearestPlateIncrement(weightKg: Double, barWeightKg: Double): Double = round(weightKg.coerceAtLeast(barWeightKg) / 2.5) * 2.5
}
