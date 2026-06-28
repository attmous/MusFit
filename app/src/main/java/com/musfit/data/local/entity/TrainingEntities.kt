package com.musfit.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val equipment: String?,
    val targetMuscles: String,
    val isCustom: Boolean,
    val primaryMuscles: String = targetMuscles,
    val secondaryMuscles: String = "",
    val instructions: String? = null,
    val localNotes: String? = null,
)

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val notes: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
    val isStarter: Boolean = false,
    val programName: String? = null,
    val tags: String = "",
)

@Entity(
    tableName = "routine_exercises",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("routineId"), Index("exerciseId")],
)
data class RoutineExerciseEntity(
    @PrimaryKey val id: String,
    val routineId: String,
    val exerciseId: String,
    val sortOrder: Int,
    val targetSets: Int,
    val targetReps: String?,
)

@Entity(
    tableName = "workout_sessions",
    indices = [Index("routineId"), Index("startedAtEpochMillis"), Index("status")],
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
)
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
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
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("sessionId"), Index("exerciseId")],
)
data class WorkoutSetEntity(
    @PrimaryKey val id: String,
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
)

@Entity(tableName = "training_settings")
data class TrainingSettingsEntity(
    @PrimaryKey val id: String = "default",
    val defaultRestSeconds: Int = 120,
    val barWeightKg: Double = 20.0,
    val availablePlatesKg: String = "25,20,15,10,5,2.5,1.25",
)
