package com.musfit.ui.food

import org.junit.Assert.assertEquals
import org.junit.Test

class FoodGoalPresentationTest {

    @Test
    fun macroSplitNormalizesOverMacroCalories() {
        // 255 g carbs (1020 kcal), 180 g protein (720 kcal), 80 g fat (720 kcal).
        val (carbs, protein, fat) = macroSplitPercents("2450", "255", "180", "80")
        assertEquals(41, carbs)
        assertEquals(29, protein)
        assertEquals(30, fat)
        assertEquals(100, carbs + protein + fat)
    }

    @Test
    fun macroSplitSumsToOneHundredDespiteRounding() {
        val (carbs, protein, fat) = macroSplitPercents("", "100", "100", "100")
        assertEquals(100, carbs + protein + fat)
    }

    @Test
    fun macroSplitGuardsDivisionByZero() {
        assertEquals(Triple(0, 0, 0), macroSplitPercents("2000", "", "", ""))
        assertEquals(Triple(0, 0, 0), macroSplitPercents("", "0", "0", "0"))
    }

    @Test
    fun macroSplitIgnoresNegativeAndUnparsableInputs() {
        val (carbs, protein, fat) = macroSplitPercents("", "-50", "abc", "10")
        assertEquals(0, carbs)
        assertEquals(0, protein)
        assertEquals(100, fat)
    }

    @Test
    fun calorieStepperStepsAndClampsAtZero() {
        assertEquals("2500", steppedCaloriesInput("2450", 50))
        assertEquals("2400", steppedCaloriesInput("2450", -50))
        assertEquals("0", steppedCaloriesInput("25", -50))
        assertEquals("50", steppedCaloriesInput("", 50))
        assertEquals("50", steppedCaloriesInput("abc", 50))
    }
}
