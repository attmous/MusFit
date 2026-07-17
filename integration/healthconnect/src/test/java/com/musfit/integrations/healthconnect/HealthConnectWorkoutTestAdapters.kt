package com.musfit.integrations.healthconnect

import java.time.ZoneId

internal suspend fun HealthConnectGateway.exportWorkoutFixture(
    session: WorkoutSessionEntity,
    sets: List<WorkoutSetEntity>,
): String? {
    val workout = session.toHealthConnectWorkoutExport(sets)
    return exportWorkout(
        workout = workout,
        identity = HealthConnectRecordIdentity.forWorkout(session.accountId, session.id, version = 1),
    )
}

internal fun HealthConnectRecordMapper.toExerciseSessionRecord(
    session: WorkoutSessionEntity,
    sets: List<WorkoutSetEntity>,
    zoneId: ZoneId = ZoneId.systemDefault(),
    identity: HealthConnectRecordIdentity = HealthConnectRecordIdentity.forWorkout(session.accountId, session.id, 1),
) = toExerciseSessionRecord(session.toHealthConnectWorkoutExport(sets), zoneId, identity)

private fun WorkoutSessionEntity.toHealthConnectWorkoutExport(
    sets: List<WorkoutSetEntity>,
) = HealthConnectWorkoutExport(
    accountId = accountId,
    localSessionId = id,
    title = title,
    startedAtEpochMillis = startedAtEpochMillis,
    endedAtEpochMillis = endedAtEpochMillis,
    notes = notes,
    sets = sets.map { set ->
        HealthConnectWorkoutSetExport(
            localSetId = set.id,
            exerciseId = set.exerciseId,
            sortOrder = set.sortOrder,
            setType = set.setType,
            reps = set.reps,
            weightKg = set.weightKg,
            durationSeconds = set.durationSeconds,
            distanceMeters = set.distanceMeters,
            rpe = set.rpe,
            notes = set.notes,
            completed = set.completed,
            supersetGroupId = set.supersetGroupId,
            restSeconds = set.restSeconds,
        )
    },
)
internal data class WorkoutSessionEntity(
    val accountId: String,
    val id: String,
    val routineId: String?,
    val title: String? = null,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long?,
    val notes: String?,
    val healthConnectRecordId: String?,
    val healthConnectLastExportedAtEpochMillis: Long?,
)

internal data class WorkoutSetEntity(
    val accountId: String,
    val id: String,
    val sessionId: String,
    val exerciseId: String,
    val sortOrder: Int,
    val setType: String = "normal",
    val reps: Int?,
    val weightKg: Double?,
    val durationSeconds: Long?,
    val distanceMeters: Double?,
    val rpe: Double?,
    val notes: String?,
    val completed: Boolean,
    val supersetGroupId: String? = null,
    val restSeconds: Int? = null,
)
