package com.musfit.domain.training

import kotlin.math.abs

/** A point in canvas pixel space. */
data class ChartPoint(val x: Float, val y: Float)

/** Pre-computed geometry for a single-series line chart. Pure — no Android/Compose types. */
data class ChartGeometry(
    val points: List<ChartPoint>,
    val minValue: Double,
    val maxValue: Double,
    val yTicks: List<Double>,
)

/**
 * Pure geometry for the Progress trend chart. Maps a list of values to canvas coordinates with
 * y inverted (higher value -> smaller y, i.e. drawn higher). Handles empty, single-point, and
 * flat (all-equal) series without producing NaN or dividing by zero.
 */
object TrendChartScaler {
    fun computeChartGeometry(
        values: List<Double>,
        widthPx: Float,
        heightPx: Float,
        paddingLeftPx: Float,
        paddingRightPx: Float,
        paddingTopPx: Float,
        paddingBottomPx: Float,
        tickCount: Int = 3,
    ): ChartGeometry {
        if (values.isEmpty()) {
            return ChartGeometry(points = emptyList(), minValue = 0.0, maxValue = 0.0, yTicks = emptyList())
        }
        val minValue = values.min()
        val maxValue = values.max()
        val plotW = (widthPx - paddingLeftPx - paddingRightPx).coerceAtLeast(0f)
        val plotH = (heightPx - paddingTopPx - paddingBottomPx).coerceAtLeast(0f)
        val n = values.size
        val centerY = paddingTopPx + plotH / 2f

        val points = values.mapIndexed { index, value ->
            val x = if (n == 1) widthPx / 2f else paddingLeftPx + index * (plotW / (n - 1))
            val y = if (maxValue == minValue) {
                centerY
            } else {
                val fraction = (value - minValue) / (maxValue - minValue)
                paddingTopPx + plotH * (1f - fraction.toFloat())
            }
            ChartPoint(x = x, y = y)
        }
        return ChartGeometry(
            points = points,
            minValue = minValue,
            maxValue = maxValue,
            yTicks = niceTicks(minValue, maxValue, tickCount),
        )
    }

    /** Index of the point whose x is closest to [touchX]; clamps to the ends. Ties favor the earlier point. */
    fun nearestIndex(touchX: Float, pointXs: List<Float>): Int {
        if (pointXs.isEmpty()) return 0
        var best = 0
        var bestDistance = Float.MAX_VALUE
        pointXs.forEachIndexed { index, x ->
            val distance = abs(x - touchX)
            if (distance < bestDistance) {
                bestDistance = distance
                best = index
            }
        }
        return best
    }

    private fun niceTicks(min: Double, max: Double, count: Int): List<Double> {
        if (count <= 0) return emptyList()
        if (count == 1 || min == max) return listOf(min)
        val step = (max - min) / (count - 1)
        return (0 until count).map { min + it * step }
    }
}
