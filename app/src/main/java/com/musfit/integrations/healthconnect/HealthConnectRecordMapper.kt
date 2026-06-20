package com.musfit.integrations.healthconnect

import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.metadata.Metadata
import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity
import java.time.Instant
import java.time.ZoneId

object HealthConnectRecordMapper {
    fun toExerciseSessionRecord(
        session: WorkoutSessionEntity,
        sets: List<WorkoutSetEntity>,
    ): ExerciseSessionRecord {
        val startInstant = Instant.ofEpochMilli(session.startedAtEpochMillis)
        val endInstant = Instant.ofEpochMilli(
            session.endedAtEpochMillis ?: (session.startedAtEpochMillis + 1),
        )
        val completedSetCount = sets.count { it.completed }
        val zoneRules = ZoneId.systemDefault().rules

        return ExerciseSessionRecord(
            startTime = startInstant,
            startZoneOffset = zoneRules.getOffset(startInstant),
            endTime = endInstant,
            endZoneOffset = zoneRules.getOffset(endInstant),
            metadata = Metadata.manualEntry(),
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
            title = "MusFit workout: $completedSetCount completed sets",
            notes = session.notes,
        )
    }
}
