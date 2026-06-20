package com.musfit.integrations.healthconnect

import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.TimeZone

class HealthConnectRecordMapperTest {
    @Test
    fun toExerciseSessionRecord_mapsStrengthWorkoutTimeRange() {
        withDefaultTimeZone("America/Los_Angeles") {
            val session = workoutSession(
                startedAtEpochMillis = 1_700_000_000_000,
                endedAtEpochMillis = 1_700_003_600_000,
            )

            val record = HealthConnectRecordMapper.toExerciseSessionRecord(session, completedSets())

            assertEquals(session.startedAtEpochMillis, record.startTime.toEpochMilli())
            assertEquals(session.endedAtEpochMillis, record.endTime.toEpochMilli())
            assertEquals(expectedOffset(session.startedAtEpochMillis), record.startZoneOffset)
            assertEquals(expectedOffset(session.endedAtEpochMillis!!), record.endZoneOffset)
            assertEquals("MusFit workout: 1 completed sets", record.title)
        }
    }

    @Test
    fun toExerciseSessionRecord_usesStartTimeWhenEndTimeMissing() {
        withDefaultTimeZone("America/Los_Angeles") {
            val session = workoutSession(
                startedAtEpochMillis = 1_700_000_000_000,
                endedAtEpochMillis = null,
            )

            val record = HealthConnectRecordMapper.toExerciseSessionRecord(session, completedSets())

            assertEquals(session.startedAtEpochMillis, record.startTime.toEpochMilli())
            assertEquals(session.startedAtEpochMillis + 1, record.endTime.toEpochMilli())
            assertEquals(expectedOffset(session.startedAtEpochMillis), record.startZoneOffset)
            assertEquals(expectedOffset(session.startedAtEpochMillis + 1), record.endZoneOffset)
        }
    }

    private fun completedSets(): List<WorkoutSetEntity> = listOf(
        WorkoutSetEntity(
            id = "set-1",
            sessionId = "session-1",
            exerciseId = "bench",
            sortOrder = 0,
            reps = 5,
            weightKg = 100.0,
            durationSeconds = null,
            distanceMeters = null,
            rpe = 8.0,
            notes = null,
            completed = true,
        ),
    )

    private fun workoutSession(
        startedAtEpochMillis: Long,
        endedAtEpochMillis: Long?,
    ) = WorkoutSessionEntity(
        id = "session-1",
        routineId = null,
        startedAtEpochMillis = startedAtEpochMillis,
        endedAtEpochMillis = endedAtEpochMillis,
        notes = "Push day",
        healthConnectRecordId = null,
        healthConnectLastExportedAtEpochMillis = null,
    )

    private fun expectedOffset(epochMillis: Long) = ZoneId.systemDefault()
        .rules
        .getOffset(Instant.ofEpochMilli(epochMillis))

    private fun withDefaultTimeZone(timeZoneId: String, block: () -> Unit) {
        val original = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone(timeZoneId))
        try {
            block()
        } finally {
            TimeZone.setDefault(original)
        }
    }
}
