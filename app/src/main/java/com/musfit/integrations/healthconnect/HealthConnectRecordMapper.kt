package com.musfit.integrations.healthconnect

import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.metadata.Metadata
import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity
import java.time.Instant
import java.time.ZoneOffset

object HealthConnectRecordMapper {
    fun toExerciseSessionRecord(
        session: WorkoutSessionEntity,
        sets: List<WorkoutSetEntity>,
    ): ExerciseSessionRecord {
        val endMillis = session.endedAtEpochMillis ?: session.startedAtEpochMillis
        val completedSetCount = sets.count { it.completed }

        return ExerciseSessionRecord(
            startTime = Instant.ofEpochMilli(session.startedAtEpochMillis),
            startZoneOffset = ZoneOffset.UTC,
            endTime = Instant.ofEpochMilli(endMillis),
            endZoneOffset = ZoneOffset.UTC,
            metadata = Metadata.manualEntry(),
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
            title = "MusFit workout: $completedSetCount completed sets",
            notes = session.notes,
        )
    }
}
