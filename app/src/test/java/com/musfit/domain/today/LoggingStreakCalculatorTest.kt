package com.musfit.domain.today

import org.junit.Assert.assertEquals
import org.junit.Test

class LoggingStreakCalculatorTest {
    @Test
    fun streak_countsBackFromTodayWhenTodayLogged() {
        val days = listOf(100L, 99L, 98L, 96L) // gap at 97
        assertEquals(3, LoggingStreakCalculator.streakDays(days, todayEpochDay = 100L))
    }

    @Test
    fun streak_countsBackFromYesterdayWhenTodayEmpty() {
        val days = listOf(99L, 98L)
        assertEquals(2, LoggingStreakCalculator.streakDays(days, todayEpochDay = 100L))
    }

    @Test
    fun streak_zeroWhenNothingRecent() {
        assertEquals(0, LoggingStreakCalculator.streakDays(listOf(90L), todayEpochDay = 100L))
        assertEquals(0, LoggingStreakCalculator.streakDays(emptyList(), todayEpochDay = 100L))
    }
}
