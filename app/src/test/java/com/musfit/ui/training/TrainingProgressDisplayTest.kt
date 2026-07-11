package com.musfit.ui.training

import com.musfit.data.repository.TrainingPrRecord
import com.musfit.domain.model.TrainingTrendPoint
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

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
        assertEquals("BACK SQUAT · ESTIMATED 1RM", progressHeroOverline("Back Squat"))
        assertEquals("ESTIMATED 1RM", progressHeroOverline(null))
        assertEquals("ESTIMATED 1RM", progressHeroOverline("  "))
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

    private fun point(daysAgo: Long, e1rm: Double): TrainingTrendPoint =
        TrainingTrendPoint(
            dateEpochDay = today.minusDays(daysAgo).toEpochDay(),
            volumeKg = 1000.0,
            bestEstimatedOneRepMaxKg = e1rm,
        )

    private fun pr(date: LocalDate, reps: Int, weightKg: Double): TrainingPrRecord =
        TrainingPrRecord(
            exerciseId = "squat",
            exerciseName = "Back Squat",
            dateEpochDay = date.toEpochDay(),
            reps = reps,
            weightKg = weightKg,
            estimatedOneRepMaxKg = 128.0,
        )
}
