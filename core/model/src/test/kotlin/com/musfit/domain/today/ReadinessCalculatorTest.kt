package com.musfit.domain.today

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReadinessCalculatorTest {
    @Test
    fun resolve_returnsHighReadinessWhenRecoverySignalsBeatBaseline() {
        val today = sample(epochDay = 10, sleepMinutes = 480, restingHeartRateBpm = 53, hrvRmssdMillis = 68.0)
        val baseline = (3L..9L).map { day ->
            sample(epochDay = day, sleepMinutes = 420, restingHeartRateBpm = 58, hrvRmssdMillis = 55.0)
        }

        val readiness = ReadinessCalculator.resolve(today = today, recent = baseline + today)

        assertEquals(86, readiness?.score)
        assertEquals(ReadinessLevel.High, readiness?.level)
    }

    @Test
    fun resolve_returnsLowReadinessWhenSleepHrvAndRhrAreSuppressed() {
        val today = sample(epochDay = 10, sleepMinutes = 220, restingHeartRateBpm = 72, hrvRmssdMillis = 35.0)
        val baseline = (3L..9L).map { day ->
            sample(epochDay = day, sleepMinutes = 420, restingHeartRateBpm = 58, hrvRmssdMillis = 55.0)
        }

        val readiness = ReadinessCalculator.resolve(today = today, recent = baseline + today)

        assertEquals(24, readiness?.score)
        assertEquals(ReadinessLevel.Low, readiness?.level)
    }

    @Test
    fun resolve_returnsNullUntilCurrentAndBaselineSignalsExist() {
        assertNull(
            ReadinessCalculator.resolve(
                today = sample(epochDay = 10, sleepMinutes = 480, restingHeartRateBpm = 53, hrvRmssdMillis = null),
                recent = emptyList(),
            ),
        )

        val today = sample(epochDay = 10, sleepMinutes = 480, restingHeartRateBpm = 53, hrvRmssdMillis = 68.0)
        val shortBaseline = listOf(
            sample(epochDay = 8, sleepMinutes = 420, restingHeartRateBpm = 58, hrvRmssdMillis = 55.0),
            sample(epochDay = 9, sleepMinutes = 420, restingHeartRateBpm = 58, hrvRmssdMillis = 55.0),
        )

        assertNull(ReadinessCalculator.resolve(today = today, recent = shortBaseline + today))
    }

    private fun sample(
        epochDay: Long,
        sleepMinutes: Long?,
        restingHeartRateBpm: Long?,
        hrvRmssdMillis: Double?,
    ) = DailyReadinessSample(
        epochDay = epochDay,
        sleepMinutes = sleepMinutes,
        restingHeartRateBpm = restingHeartRateBpm,
        hrvRmssdMillis = hrvRmssdMillis,
    )
}
