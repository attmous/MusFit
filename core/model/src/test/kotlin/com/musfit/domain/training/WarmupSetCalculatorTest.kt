package com.musfit.domain.training

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WarmupSetCalculatorTest {
    @Test
    fun suggestions_scaleToWorkingWeightAndRoundToNearestPlateIncrement() {
        val suggestions = WarmupSetCalculator.suggestions(
            workingWeightKg = 100.0,
            workingReps = 5,
            barWeightKg = 20.0,
        )

        assertEquals(
            listOf(
                WarmupSetSuggestion(weightKg = 50.0, reps = 8),
                WarmupSetSuggestion(weightKg = 70.0, reps = 5),
                WarmupSetSuggestion(weightKg = 85.0, reps = 3),
            ),
            suggestions,
        )
    }

    @Test
    fun suggestions_emptyWhenWorkingWeightIsAtOrBelowBar() {
        assertTrue(WarmupSetCalculator.suggestions(workingWeightKg = 20.0, workingReps = 5, barWeightKg = 20.0).isEmpty())
        assertTrue(WarmupSetCalculator.suggestions(workingWeightKg = null, workingReps = 5, barWeightKg = 20.0).isEmpty())
    }
}
