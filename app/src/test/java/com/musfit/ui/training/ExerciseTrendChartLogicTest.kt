package com.musfit.ui.training

import com.musfit.domain.model.TrainingTrendPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseTrendChartLogicTest {
    private val point = TrainingTrendPoint(
        dateEpochDay = 100L,
        volumeKg = 1500.0,
        bestEstimatedOneRepMaxKg = 130.0,
        heaviestWeightKg = 110.0,
    )

    @Test
    fun valueFor_mapsEachMetricToItsField() {
        assertEquals(1500.0, point.valueFor(TrendMetric.Volume), 0.0)
        assertEquals(130.0, point.valueFor(TrendMetric.EstOneRepMax), 0.0)
        assertEquals(110.0, point.valueFor(TrendMetric.Heaviest), 0.0)
    }
}
