package com.musfit.domain.training

/** Greedy barbell plate breakdown per side. Best-effort: ignores any remainder that no plate can cover. */
object PlateCalculator {
    val DEFAULT_PLATES = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)

    fun platesPerSide(
        totalKg: Double,
        barKg: Double = 20.0,
        plates: List<Double> = DEFAULT_PLATES,
    ): List<Double> {
        if (totalKg <= barKg) return emptyList()
        var perSide = (totalKg - barKg) / 2.0
        val result = mutableListOf<Double>()
        for (plate in plates.sortedDescending()) {
            while (perSide >= plate - 1e-6) {
                result += plate
                perSide -= plate
            }
        }
        return result
    }
}
