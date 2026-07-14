package com.musfit.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("accountId")],
    primaryKeys = ["id"],
)
data class ExerciseEntity(
    val id: String,
    val accountId: String? = null,
    val name: String,
    val category: String,
    val equipment: String?,
    val targetMuscles: String,
    val isCustom: Boolean,
    val primaryMuscles: String = targetMuscles,
    val secondaryMuscles: String = "",
    val instructions: String? = null,
    val imageUrl: String? = null,
    val gifUrl: String? = null,
)

@Entity(
    tableName = "exercise_notes",
    primaryKeys = ["accountId", "exerciseId"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("exerciseId")],
)
data class ExerciseNoteEntity(
    val accountId: String,
    val exerciseId: String,
    val notes: String,
)

@Entity(
    tableName = "routine_folders",
    primaryKeys = ["accountId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["accountId", "sortOrder", "name"])],
)
data class RoutineFolderEntity(
    val accountId: String,
    val id: String,
    val name: String,
    val sortOrder: Int,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
)

@Entity(
    tableName = "routines",
    primaryKeys = ["accountId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = RoutineFolderEntity::class,
            parentColumns = ["accountId", "id"],
            childColumns = ["accountId", "folderId"],
        ),
    ],
    indices = [Index(value = ["accountId", "folderId"]), Index(value = ["accountId", "updatedAtEpochMillis"])],
)
data class RoutineEntity(
    val accountId: String,
    val id: String,
    val name: String,
    val notes: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
    val isStarter: Boolean = false,
    val programName: String? = null,
    val tags: String = "",
    val folderId: String? = null,
)

@Entity(
    tableName = "routine_exercises",
    primaryKeys = ["accountId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["accountId", "id"],
            childColumns = ["accountId", "routineId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["accountId", "routineId", "sortOrder"]), Index(value = ["accountId", "exerciseId"]), Index("exerciseId")],
)
data class RoutineExerciseEntity(
    val accountId: String,
    val id: String,
    val routineId: String,
    val exerciseId: String,
    val sortOrder: Int,
    val targetSets: Int,
    val targetReps: String?,
    val restSeconds: Int? = null,
)

@Entity(
    tableName = "routine_exercise_sets",
    primaryKeys = ["accountId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = RoutineExerciseEntity::class,
            parentColumns = ["accountId", "id"],
            childColumns = ["accountId", "routineExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["accountId", "routineExerciseId", "sortOrder"])],
)
data class RoutineExerciseSetEntity(
    val accountId: String,
    val id: String,
    val routineExerciseId: String,
    val sortOrder: Int,
    val setType: String,
    val targetReps: String?,
    val targetWeightKg: Double? = null,
)

@Entity(
    tableName = "workout_sessions",
    primaryKeys = ["accountId", "id"],
    indices = [
        Index(value = ["accountId", "routineId"]),
        Index(value = ["accountId", "startedAtEpochMillis"]),
        Index(value = ["accountId", "status", "startedAtEpochMillis"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["accountId", "id"],
            childColumns = ["accountId", "routineId"],
        ),
    ],
)
data class WorkoutSessionEntity(
    val accountId: String,
    val id: String,
    val routineId: String?,
    val title: String? = null,
    val status: String = "completed",
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long?,
    val notes: String?,
    val healthConnectRecordId: String?,
    val healthConnectLastExportedAtEpochMillis: Long?,
)

@Entity(
    tableName = "workout_sets",
    primaryKeys = ["accountId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["accountId", "id"],
            childColumns = ["accountId", "sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["accountId", "sessionId", "sortOrder"]),
        Index(value = ["accountId", "exerciseId", "completed", "sessionId", "sortOrder"]),
        Index("exerciseId"),
    ],
)
data class WorkoutSetEntity(
    val accountId: String,
    val id: String,
    val sessionId: String,
    val exerciseId: String,
    val sortOrder: Int,
    val setType: String = "working",
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

@Entity(
    tableName = "training_settings",
    primaryKeys = ["accountId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class TrainingSettingsEntity(
    val accountId: String,
    val id: String = "default",
    val defaultRestSeconds: Int = 120,
    val barWeightKg: Double = 20.0,
    val availablePlatesKg: String = "25,20,15,10,5,2.5,1.25",
)
