package com.musfit.ui.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FoodDetailPresentationTest {

    @Test
    fun saltConvertsSodiumMilligramsWithEuFactor() {
        // 400 mg sodium -> 1.0 g salt (x2.5 / 1000).
        assertEquals(1.0, saltGramsFromSodiumMg(400.0), 1e-9)
        assertEquals(0.1, saltGramsFromSodiumMg(40.0), 1e-9)
        assertEquals(0.0, saltGramsFromSodiumMg(0.0), 1e-9)
    }

    @Test
    fun gramsStepperStepsByTenAndClampsAtMinimum() {
        assertEquals("160", steppedGramsInput("150", 10.0))
        assertEquals("140", steppedGramsInput("150", -10.0))
        assertEquals("1", steppedGramsInput("5", -10.0))
        assertEquals("1", steppedGramsInput("1", -10.0))
    }

    @Test
    fun gramsStepperTreatsUnparsableInputAsZero() {
        assertEquals("10", steppedGramsInput("abc", 10.0))
        assertEquals("1", steppedGramsInput("", -10.0))
    }

    @Test
    fun percentOfDayLabelRoundsAndGuardsMissingBudget() {
        assertEquals(4, percentOfDayValue(98.0, 2450.0))
        assertEquals(100, percentOfDayValue(2450.0, 2450.0))
        assertNull(percentOfDayValue(98.0, 0.0))
        assertNull(percentOfDayValue(98.0, -10.0))
    }

    @Test
    fun macroFillUsesDailyGoalWhenAvailable() {
        assertEquals(0.5f, macroFillFraction(90.0, 180.0), 1e-6f)
        assertEquals(1.0f, macroFillFraction(400.0, 180.0), 1e-6f)
    }

    @Test
    fun macroFillFallsBackToHundredGramCapWithoutGoal() {
        assertEquals(0.2f, macroFillFraction(20.0, 0.0), 1e-6f)
        assertEquals(1.0f, macroFillFraction(250.0, 0.0), 1e-6f)
    }
}
