package com.musfit.ui.training

import androidx.compose.ui.input.key.Key
import com.musfit.data.repository.TrainingPrRecord
import com.musfit.data.repository.WeeklyTrainingVolume
import com.musfit.domain.model.TrainingTrendPoint
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TrainingProgressDisplayTest {
    private val today: LocalDate = LocalDate.of(2026, 7, 8)

    @Test
    fun filterTrendByPeriod_dropsPointsOlderThanTheWindow() {
        val trend = listOf(
            point(daysAgo = 200, e1rm = 100.0),
            point(daysAgo = 60, e1rm = 119.0),
            point(daysAgo = 5, e1rm = 128.0),
        )

        val twelveWeeks = filterTrendByPeriod(trend, TrainingProgressPeriod.TwelveWeeks, today)
        val year = filterTrendByPeriod(trend, TrainingProgressPeriod.Year, today)

        assertEquals(listOf(119.0, 128.0), twelveWeeks.map { it.bestEstimatedOneRepMaxKg })
        assertEquals(3, year.size)
    }

    @Test
    fun e1rmDeltaKg_isWindowChangeOrNull() {
        assertEquals(
            9.0,
            e1rmDeltaKg(listOf(point(daysAgo = 60, e1rm = 119.0), point(daysAgo = 5, e1rm = 128.0)))!!,
            1e-6,
        )
        assertEquals(null, e1rmDeltaKg(listOf(point(daysAgo = 5, e1rm = 128.0))))
    }

    @Test
    fun deltaLabel_formatsSign() {
        assertEquals("+9 kg", deltaLabel(9.0))
        assertEquals("−2.5 kg", deltaLabel(-2.5))
    }

    @Test
    fun progressHeroOverline_uppercasesTheAnchoredExercise() {
        assertEquals("BACK SQUAT · ESTIMATED 1RM", progressHeroOverline("Back Squat", "Estimated 1RM"))
        assertEquals("ESTIMATED 1RM", progressHeroOverline(null, "Estimated 1RM"))
        assertEquals("ESTIMATED 1RM", progressHeroOverline("  ", "Estimated 1RM"))
    }

    @Test
    fun monthLabelsFor_capsAtFourSpreadMonths() {
        val trend = (0L..180L step 10).map { daysAgo -> point(daysAgo = daysAgo, e1rm = 100.0) }

        val labels = monthLabelsFor(trend)

        assertEquals(4, labels.size)
        assertEquals("Jan", labels.first())
        assertEquals("Jul", labels.last())
    }

    @Test
    fun weekLabel_usesIsoWeekNumber() {
        // Monday 29 Jun 2026 is ISO week 27.
        assertEquals("W27", weekLabel(LocalDate.of(2026, 6, 29).toEpochDay()))
    }

    @Test
    fun volumeTonsLabel_switchesUnitsAtOneTonne() {
        assertEquals("11.2 t", volumeTonsLabel(11_200.0))
        assertEquals("2 t", volumeTonsLabel(2_000.0))
        assertEquals("840 kg", volumeTonsLabel(840.4))
    }

    @Test
    fun chartDescriptions_associateDatesValuesAndPositions() {
        val trend = listOf(
            point(daysAgo = 2, e1rm = 120.0),
            point(daysAgo = 0, e1rm = 128.5),
        )
        val weeks = listOf(
            week(LocalDate.of(2026, 6, 29), volumeKg = 840.0, workouts = 1, sets = 4),
            week(LocalDate.of(2026, 7, 6), volumeKg = 2_000.0, workouts = 2, sets = 8),
        )

        assertEquals(
            "Estimated one rep max chart for Back Squat. 2 data points from July 6, 2026 to July 8, 2026.",
            e1rmChartSummary("Back Squat", trend),
        )
        assertEquals(
            "Selected July 8, 2026, 128.5 kg estimated one rep max, point 2 of 2",
            e1rmChartSelectionDescription(trend, 1),
        )
        assertEquals(
            "Weekly training volume chart. 2 weeks from week of June 29, 2026 to week of July 6, 2026.",
            weeklyVolumeChartSummary(weeks),
        )
        assertEquals(
            "Selected week of July 6, 2026, 2 t total volume, 2 workouts, 8 completed sets, week 2 of 2",
            weeklyVolumeChartSelectionDescription(weeks, 1),
        )
    }

    @Test
    fun chartDescriptions_handleEmptyAndSinglePointBoundaries() {
        val onlyPoint = listOf(point(daysAgo = 0, e1rm = 128.0))
        val onlyWeek = listOf(week(today, volumeKg = 500.0, workouts = 1, sets = 1))

        assertEquals(
            "Estimated one rep max chart for Back Squat. 1 data point on July 8, 2026.",
            e1rmChartSummary("Back Squat", onlyPoint),
        )
        assertEquals("Estimated one rep max chart. No data.", e1rmChartSummary(null, emptyList()))
        assertEquals("No data selected", e1rmChartSelectionDescription(emptyList(), -1))
        assertEquals(
            "Weekly training volume chart. 1 week on week of July 8, 2026.",
            weeklyVolumeChartSummary(onlyWeek),
        )
        assertEquals("Weekly training volume chart. No data.", weeklyVolumeChartSummary(emptyList()))
        assertEquals("No data selected", weeklyVolumeChartSelectionDescription(emptyList(), -1))
    }

    @Test
    fun chartKeyboardNavigation_honorsArrowsHomeEndAndBoundaries() {
        assertEquals(1, chartIndexForKey(Key.DirectionLeft, selectedIndex = 2, lastIndex = 3))
        assertEquals(1, chartIndexForKey(Key.DirectionUp, selectedIndex = 2, lastIndex = 3))
        assertEquals(3, chartIndexForKey(Key.DirectionRight, selectedIndex = 2, lastIndex = 3))
        assertEquals(3, chartIndexForKey(Key.DirectionDown, selectedIndex = 2, lastIndex = 3))
        assertEquals(0, chartIndexForKey(Key.MoveHome, selectedIndex = 2, lastIndex = 3))
        assertEquals(3, chartIndexForKey(Key.MoveEnd, selectedIndex = 1, lastIndex = 3))
        assertEquals(null, chartIndexForKey(Key.DirectionLeft, selectedIndex = 0, lastIndex = 3))
        assertEquals(null, chartIndexForKey(Key.DirectionRight, selectedIndex = 3, lastIndex = 3))
        assertEquals(null, chartIndexForKey(Key.MoveHome, selectedIndex = 0, lastIndex = 3))
        assertEquals(null, chartIndexForKey(Key.MoveEnd, selectedIndex = 3, lastIndex = 3))
    }

    @Test
    fun chartKeyboardNavigation_mirrorsHorizontalArrowsInRtl() {
        assertEquals(3, chartIndexForKey(Key.DirectionLeft, selectedIndex = 2, lastIndex = 3, isRtl = true))
        assertEquals(1, chartIndexForKey(Key.DirectionRight, selectedIndex = 2, lastIndex = 3, isRtl = true))
        assertEquals(1, chartIndexForKey(Key.DirectionUp, selectedIndex = 2, lastIndex = 3, isRtl = true))
        assertEquals(3, chartIndexForKey(Key.DirectionDown, selectedIndex = 2, lastIndex = 3, isRtl = true))
        assertEquals(null, chartIndexForKey(Key.DirectionLeft, selectedIndex = 3, lastIndex = 3, isRtl = true))
        assertEquals(null, chartIndexForKey(Key.DirectionRight, selectedIndex = 0, lastIndex = 3, isRtl = true))
    }

    @Test
    fun chartTapMapping_mirrorsPointAndSlotSelectionInRtl() {
        assertEquals(
            3,
            chartIndexForTap(
                x = 0f,
                width = 300f,
                itemCount = 4,
                mode = ChartTapSelectionMode.Points,
                isRtl = true,
            ),
        )
        assertEquals(
            0,
            chartIndexForTap(
                x = 300f,
                width = 300f,
                itemCount = 4,
                mode = ChartTapSelectionMode.Points,
                isRtl = true,
            ),
        )
        assertEquals(
            3,
            chartIndexForTap(
                x = 1f,
                width = 400f,
                itemCount = 4,
                mode = ChartTapSelectionMode.Slots,
                isRtl = true,
            ),
        )
        assertEquals(
            0,
            chartIndexForTap(
                x = 399f,
                width = 400f,
                itemCount = 4,
                mode = ChartTapSelectionMode.Slots,
                isRtl = true,
            ),
        )
    }

    @Test
    fun chartPointGeometry_mirrorsChronologicalEndpointsInRtl() {
        assertEquals(0f, chartPointX(index = 0, itemCount = 4, width = 300f, isRtl = false), 0.001f)
        assertEquals(300f, chartPointX(index = 3, itemCount = 4, width = 300f, isRtl = false), 0.001f)
        assertEquals(300f, chartPointX(index = 0, itemCount = 4, width = 300f, isRtl = true), 0.001f)
        assertEquals(0f, chartPointX(index = 3, itemCount = 4, width = 300f, isRtl = true), 0.001f)
        assertEquals(200f, chartPointX(index = 1, itemCount = 4, width = 300f, isRtl = true), 0.001f)
    }

    @Test
    fun prMetaLabel_formatsWeightRepsAndRecency() {
        assertEquals(
            "107.5 kg × 5 · Mon",
            prMetaLabel(pr(date = LocalDate.of(2026, 7, 6), reps = 5, weightKg = 107.5), today),
        )
        assertEquals(
            "90 kg × 8 · 27 Jun",
            prMetaLabel(pr(date = LocalDate.of(2026, 6, 27), reps = 8, weightKg = 90.0), today),
        )
    }

    private fun point(daysAgo: Long, e1rm: Double): TrainingTrendPoint = TrainingTrendPoint(
        dateEpochDay = today.minusDays(daysAgo).toEpochDay(),
        volumeKg = 1000.0,
        bestEstimatedOneRepMaxKg = e1rm,
    )

    private fun pr(date: LocalDate, reps: Int, weightKg: Double): TrainingPrRecord = TrainingPrRecord(
        exerciseId = "squat",
        exerciseName = "Back Squat",
        dateEpochDay = date.toEpochDay(),
        reps = reps,
        weightKg = weightKg,
        estimatedOneRepMaxKg = 128.0,
    )

    private fun week(
        date: LocalDate,
        volumeKg: Double,
        workouts: Int,
        sets: Int,
    ): WeeklyTrainingVolume = WeeklyTrainingVolume(
        weekStartEpochDay = date.toEpochDay(),
        workoutCount = workouts,
        completedSetCount = sets,
        totalVolumeKg = volumeKg,
    )
}
