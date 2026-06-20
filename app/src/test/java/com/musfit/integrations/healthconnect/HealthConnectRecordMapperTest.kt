package com.musfit.integrations.healthconnect

import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneOffset

class HealthConnectRecordMapperTest {
    @Test
    fun toExerciseSessionRecord_mapsStrengthWorkoutTimeRange() {
        val session = workoutSession(
            startedAtEpochMillis = 1_700_000_000_000,
            endedAtEpochMillis = 1_700_003_600_000,
        )

        val record = HealthConnectRecordMapper.toExerciseSessionRecord(session, completedSets())

        assertEquals(session.startedAtEpochMillis, record.startTime.toEpochMilli())
        assertEquals(session.endedAtEpochMillis, record.endTime.toEpochMilli())
        assertEquals(ZoneOffset.UTC, record.startZoneOffset)
        assertEquals(ZoneOffset.UTC, record.endZoneOffset)
        assertEquals("MusFit workout: 1 completed sets", record.title)
    }

    @Test
    fun toExerciseSessionRecord_usesMinimalPositiveDurationWhenEndTimeMissing_forSdkConstraint() {
        val session = workoutSession(
            startedAtEpochMillis = 1_700_000_000_000,
            endedAtEpochMillis = null,
        )

        val record = HealthConnectRecordMapper.toExerciseSessionRecord(session, completedSets())

        assertEquals(session.startedAtEpochMillis, record.startTime.toEpochMilli())
        assertEquals(session.startedAtEpochMillis + 1, record.endTime.toEpochMilli())
        assertEquals(ZoneOffset.UTC, record.startZoneOffset)
        assertEquals(ZoneOffset.UTC, record.endZoneOffset)
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

}
