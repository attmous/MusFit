package com.musfit.ui.food

import org.junit.Assert.assertEquals
import org.junit.Test

class FoodQuickStepperTest {
    @Test
    fun blankInputStepsFromZero() {
        assertEquals("25", quickStepperNext("", 25))
    }

    @Test
    fun stepsUpByDelta() {
        assertEquals("275", quickStepperNext("250", 25))
    }

    @Test
    fun clampsAtZeroWhenSteppingBelow() {
        assertEquals("0", quickStepperNext("10", -25))
    }

    @Test
    fun zeroStaysZeroWhenDecreasing() {
        assertEquals("0", quickStepperNext("0", -25))
    }

    @Test
    fun preservesDecimalRemainder() {
        assertEquals("137.5", quickStepperNext("112.5", 25))
    }

    @Test
    fun invalidInputTreatedAsZero() {
        assertEquals("0", quickStepperNext("abc", -25))
        assertEquals("25", quickStepperNext("abc", 25))
    }
}
