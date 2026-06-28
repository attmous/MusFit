package com.musfit.domain.training

import org.junit.Assert.assertEquals
import org.junit.Test

class RoutineDisplayCalculatorTest {
    @Test
    fun estimatedMinutes_zeroOrNegativeSetsIsZero() {
        assertEquals(0, RoutineDisplayCalculator.estimatedMinutes(0))
        assertEquals(0, RoutineDisplayCalculator.estimatedMinutes(-4))
    }

    @Test
    fun estimatedMinutes_roundsToNearestFiveMinutes() {
        assertEquals(5, RoutineDisplayCalculator.estimatedMinutes(1)) // 3 -> 5
        assertEquals(15, RoutineDisplayCalculator.estimatedMinutes(5)) // 15
        assertEquals(35, RoutineDisplayCalculator.estimatedMinutes(12)) // 36 -> 35
        assertEquals(40, RoutineDisplayCalculator.estimatedMinutes(14)) // 42 -> 40
    }

    @Test
    fun topMuscles_dedupesTitleCasesTrimsAndCaps() {
        val muscles = RoutineDisplayCalculator.topMuscles("chest,triceps,chest,quads, glutes,shoulders")
        assertEquals(listOf("Chest", "Triceps", "Quads"), muscles)
    }

    @Test
    fun topMuscles_isCaseInsensitiveAndRespectsLimit() {
        val muscles = RoutineDisplayCalculator.topMuscles("Back, back ,BICEPS", limit = 5)
        assertEquals(listOf("Back", "Biceps"), muscles)
    }

    @Test
    fun topMuscles_blankOrSeparatorsOnlyReturnsEmpty() {
        assertEquals(emptyList<String>(), RoutineDisplayCalculator.topMuscles(""))
        assertEquals(emptyList<String>(), RoutineDisplayCalculator.topMuscles(" , , "))
    }
}
