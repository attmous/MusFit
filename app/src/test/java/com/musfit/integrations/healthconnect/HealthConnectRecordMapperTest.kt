package com.musfit.integrations.healthconnect

import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class HealthConnectRecordMapperTest {
    @Test
    fun toExerciseSessionRecord_mapsStrengthWorkoutTimeRange() {
        val session = WorkoutSessionEntity(
            id = "session-1",
            routineId = null,
            startedAtEpochMillis = 1_700_000_000_000,
            endedAtEpochMillis = 1_700_003_600_000,
            notes = "Push day",
            healthConnectRecordId = null,
            healthConnectLastExportedAtEpochMillis = null,
        )
        val sets = listOf(
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

        val record = HealthConnectRecordMapper.toExerciseSessionRecord(session, sets)

        assertEquals(session.startedAtEpochMillis, record.startTime.toEpochMilli())
        assertEquals(session.endedAtEpochMillis, record.endTime.toEpochMilli())
        assertEquals("MusFit workout: 1 completed sets", record.title)
    }
}
