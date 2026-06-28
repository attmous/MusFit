package com.musfit.domain.charts

/** Normalised geometry for a small bar chart. Pure — no Android/Compose types. */
data class BarChartGeometry(
    val bars: List<Double?>,     // fraction in [0,1] per bar; null passes through (untracked)
    val targetFraction: Double?, // fraction in [0,1] for the target line, or null
    val maxValue: Double,        // the scaled max (with headroom) used for normalisation
)

/**
 * Maps bar values (nullable = untracked) and an optional target to fractions in [0,1] against a
 * shared max with headroom, so the tallest bar and the target line never touch the top edge.
 * Handles empty, all-null, and all-zero series without dividing by zero.
 */
object BarChartScaler {
    fun compute(values: List<Double?>, target: Double?, headroom: Double = 1.15): BarChartGeometry {
        val dataMax = values.filterNotNull().maxOrNull() ?: 0.0
        val rawMax = maxOf(dataMax, target ?: 0.0)
        if (rawMax <= 0.0) {
            return BarChartGeometry(
                bars = values.map { if (it == null) null else 0.0 },
                targetFraction = null,
                maxValue = 0.0,
            )
        }
        val maxValue = rawMax * headroom
        val bars = values.map { v -> v?.let { (it / maxValue).coerceIn(0.0, 1.0) } }
        val targetFraction = target?.takeIf { it > 0.0 }?.let { (it / maxValue).coerceIn(0.0, 1.0) }
        return BarChartGeometry(bars = bars, targetFraction = targetFraction, maxValue = maxValue)
    }
}
