package com.musfit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.musfit.data.local.entity.ExerciseEntity
import com.musfit.data.local.entity.RoutineEntity
import com.musfit.data.local.entity.RoutineExerciseEntity
import com.musfit.data.local.entity.RoutineExerciseSetEntity
import com.musfit.data.local.entity.RoutineFolderEntity
import com.musfit.data.local.entity.TrainingSettingsEntity
import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.Flow

data class RoutineSummaryRow(
    val id: String,
    val name: String,
    val notes: String?,
    val exerciseCount: Int,
    val targetSetCount: Int,
    val isStarter: Boolean,
    val programName: String?,
    val tags: String,
    val folderId: String?,
    val folderName: String?,
    val primaryMuscles: String,
)

data class ActiveWorkoutSummaryRow(
    val sessionId: String,
    val title: String?,
    val startedAtEpochMillis: Long,
    val completedSetCount: Int,
    val totalVolumeKg: Double,
)

data class RoutineExerciseDetailRow(
    val id: String,
    val routineId: String,
    val exerciseId: String,
    val exerciseName: String,
    val category: String,
    val equipment: String?,
    val targetMuscles: String,
    val isCustom: Boolean,
    val sortOrder: Int,
    val targetSets: Int,
    val targetReps: String?,
    val restSeconds: Int?,
    val imageUrl: String?,
    val gifUrl: String?,
)

data class RoutineExerciseSetDetailRow(
    val id: String,
    val routineExerciseId: String,
    val sortOrder: Int,
    val setType: String,
    val targetReps: String?,
    val targetWeightKg: Double?,
)

data class WorkoutSetDetailRow(
    val setId: String,
    val sessionId: String,
    val exerciseId: String,
    val exerciseName: String,
    val category: String,
    val equipment: String?,
    val targetMuscles: String,
    val isCustom: Boolean,
    val sortOrder: Int,
    val setType: String,
    val reps: Int?,
    val weightKg: Double?,
    val rpe: Double?,
    val notes: String?,
    val completed: Boolean,
    val supersetGroupId: String? = null,
    val restSeconds: Int? = null,
    val imageUrl: String? = null,
    val gifUrl: String? = null,
)

data class WorkoutHistorySummaryRow(
    val sessionId: String,
    val title: String?,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long?,
    val completedSetCount: Int,
    val totalVolumeKg: Double,
)

data class ExerciseProgressSetRow(
    val sessionId: String,
    val exerciseId: String,
    val exerciseName: String,
    val category: String,
    val equipment: String?,
    val targetMuscles: String,
    val isCustom: Boolean,
    val startedAtEpochMillis: Long,
    val reps: Int?,
    val weightKg: Double?,
    val completed: Boolean,
)

@Dao
interface TrainingDao {
    @Query("SELECT * FROM exercises ORDER BY name")
    fun observeExercises(): Flow<List<ExerciseEntity>>

    @Query(
        """
        SELECT *
        FROM exercises
        WHERE (:query = '' OR name LIKE '%' || :query || '%' OR targetMuscles LIKE '%' || :query || '%' OR primaryMuscles LIKE '%' || :query || '%' OR secondaryMuscles LIKE '%' || :query || '%')
        AND (:muscle IS NULL OR targetMuscles LIKE '%' || :muscle || '%' OR primaryMuscles LIKE '%' || :muscle || '%' OR secondaryMuscles LIKE '%' || :muscle || '%')
        AND (:equipment IS NULL OR equipment = :equipment)
        ORDER BY isCustom ASC, name ASC
        """,
    )
    fun observeExercisesFiltered(
        query: String,
        muscle: String?,
        equipment: String?,
    ): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE name = :name LIMIT 1")
    suspend fun getExerciseByName(name: String): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE id = :exerciseId LIMIT 1")
    suspend fun getExercise(exerciseId: String): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getExerciseByNormalizedName(name: String): ExerciseEntity?

    @Query("UPDATE exercises SET localNotes = :notes WHERE id = :exerciseId")
    suspend fun updateExerciseLocalNotes(exerciseId: String, notes: String?)

    @Query("SELECT * FROM routines ORDER BY createdAtEpochMillis DESC")
    fun observeRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routine_folders ORDER BY sortOrder ASC, name ASC")
    fun observeRoutineFolders(): Flow<List<RoutineFolderEntity>>

    @Query("SELECT * FROM routine_folders WHERE id = :folderId LIMIT 1")
    suspend fun getRoutineFolder(folderId: String): RoutineFolderEntity?

    @Query("SELECT * FROM routine_folders WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getRoutineFolderByName(name: String): RoutineFolderEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM routine_folders")
    suspend fun getMaxRoutineFolderSortOrder(): Int

    @Query(
        """
        SELECT routines.id AS id,
            routines.name AS name,
            routines.notes AS notes,
            COUNT(routine_exercises.id) AS exerciseCount,
            COALESCE(SUM(routine_exercises.targetSets), 0) AS targetSetCount,
            routines.isStarter AS isStarter,
            routines.programName AS programName,
            routines.tags AS tags,
            routines.folderId AS folderId,
            routine_folders.name AS folderName,
            COALESCE(GROUP_CONCAT(exercises.primaryMuscles), '') AS primaryMuscles
        FROM routines
        LEFT JOIN routine_folders ON routine_folders.id = routines.folderId
        LEFT JOIN routine_exercises ON routine_exercises.routineId = routines.id
        LEFT JOIN exercises ON exercises.id = routine_exercises.exerciseId
        GROUP BY routines.id
        ORDER BY routine_folders.sortOrder ASC, routine_folders.name ASC, routines.updatedAtEpochMillis DESC, routines.name ASC
        """,
    )
    fun observeRoutineSummaries(): Flow<List<RoutineSummaryRow>>

    @Query("SELECT * FROM workout_sessions ORDER BY startedAtEpochMillis DESC")
    fun observeWorkoutSessions(): Flow<List<WorkoutSessionEntity>>

    @Query(
        """
        SELECT workout_sessions.id AS sessionId,
            workout_sessions.title AS title,
            workout_sessions.startedAtEpochMillis AS startedAtEpochMillis,
            COALESCE(SUM(CASE WHEN workout_sets.completed = 1 THEN 1 ELSE 0 END), 0) AS completedSetCount,
            COALESCE(SUM(CASE WHEN workout_sets.completed = 1 AND workout_sets.reps IS NOT NULL AND workout_sets.weightKg IS NOT NULL THEN workout_sets.reps * workout_sets.weightKg ELSE 0 END), 0) AS totalVolumeKg
        FROM workout_sessions
        LEFT JOIN workout_sets ON workout_sets.sessionId = workout_sessions.id
        WHERE workout_sessions.status = 'active'
        GROUP BY workout_sessions.id
        ORDER BY workout_sessions.startedAtEpochMillis DESC
        LIMIT 1
        """,
    )
    fun observeActiveWorkoutSummary(): Flow<ActiveWorkoutSummaryRow?>

    @Query("SELECT * FROM workout_sessions WHERE status = 'active' ORDER BY startedAtEpochMillis DESC LIMIT 1")
    fun observeActiveWorkoutSession(): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM training_settings WHERE id = 'default' LIMIT 1")
    fun observeTrainingSettings(): Flow<TrainingSettingsEntity?>

    @Query("SELECT * FROM training_settings WHERE id = 'default' LIMIT 1")
    suspend fun getTrainingSettings(): TrainingSettingsEntity?

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getWorkoutSession(sessionId: String): WorkoutSessionEntity?

    @Query("SELECT * FROM routines WHERE id = :routineId LIMIT 1")
    suspend fun getRoutine(routineId: String): RoutineEntity?

    @Query(
        """
        SELECT routine_exercises.id AS id,
            routine_exercises.routineId AS routineId,
            exercises.id AS exerciseId,
            exercises.name AS exerciseName,
            exercises.category AS category,
            exercises.equipment AS equipment,
            exercises.targetMuscles AS targetMuscles,
            exercises.isCustom AS isCustom,
            routine_exercises.sortOrder AS sortOrder,
            routine_exercises.targetSets AS targetSets,
            routine_exercises.targetReps AS targetReps,
            routine_exercises.restSeconds AS restSeconds,
            exercises.imageUrl AS imageUrl,
            exercises.gifUrl AS gifUrl
        FROM routine_exercises
        INNER JOIN exercises ON exercises.id = routine_exercises.exerciseId
        WHERE routine_exercises.routineId = :routineId
        ORDER BY routine_exercises.sortOrder ASC
        """,
    )
    suspend fun getRoutineExerciseDetailRows(routineId: String): List<RoutineExerciseDetailRow>

    @Query(
        """
        SELECT routine_exercise_sets.id AS id,
            routine_exercise_sets.routineExerciseId AS routineExerciseId,
            routine_exercise_sets.sortOrder AS sortOrder,
            routine_exercise_sets.setType AS setType,
            routine_exercise_sets.targetReps AS targetReps,
            routine_exercise_sets.targetWeightKg AS targetWeightKg
        FROM routine_exercise_sets
        INNER JOIN routine_exercises ON routine_exercises.id = routine_exercise_sets.routineExerciseId
        WHERE routine_exercises.routineId = :routineId
        ORDER BY routine_exercises.sortOrder ASC, routine_exercise_sets.sortOrder ASC
        """,
    )
    suspend fun getRoutineExerciseSetDetailRows(routineId: String): List<RoutineExerciseSetDetailRow>

    @Query("SELECT * FROM workout_sessions ORDER BY startedAtEpochMillis DESC LIMIT 1")
    suspend fun getLatestWorkoutSession(): WorkoutSessionEntity?

    @Query(
        """
        SELECT *
        FROM workout_sessions
        WHERE status = 'active'
        ORDER BY startedAtEpochMillis DESC
        LIMIT 1
        """,
    )
    suspend fun getLatestActiveWorkoutSession(): WorkoutSessionEntity?

    @Query(
        """
        SELECT *
        FROM workout_sessions
        WHERE status = 'completed'
        AND EXISTS (
            SELECT 1
            FROM workout_sets
            WHERE workout_sets.sessionId = workout_sessions.id
            AND workout_sets.completed = 1
        )
        ORDER BY startedAtEpochMillis DESC
        LIMIT 1
        """,
    )
    suspend fun getLatestCompletedWorkoutSession(): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId AND status = 'completed' LIMIT 1")
    suspend fun getCompletedWorkoutSession(sessionId: String): WorkoutSessionEntity?

    @Query(
        """
        SELECT workout_sessions.id AS sessionId,
            workout_sessions.title AS title,
            workout_sessions.startedAtEpochMillis AS startedAtEpochMillis,
            workout_sessions.endedAtEpochMillis AS endedAtEpochMillis,
            COALESCE(SUM(CASE WHEN workout_sets.completed = 1 THEN 1 ELSE 0 END), 0) AS completedSetCount,
            COALESCE(SUM(CASE WHEN workout_sets.completed = 1 AND workout_sets.reps IS NOT NULL AND workout_sets.weightKg IS NOT NULL THEN workout_sets.reps * workout_sets.weightKg ELSE 0 END), 0) AS totalVolumeKg
        FROM workout_sessions
        LEFT JOIN workout_sets ON workout_sets.sessionId = workout_sessions.id
        WHERE workout_sessions.status = 'completed'
        GROUP BY workout_sessions.id
        ORDER BY workout_sessions.startedAtEpochMillis DESC
        """,
    )
    fun observeWorkoutHistorySummaries(): Flow<List<WorkoutHistorySummaryRow>>

    @Query(
        """
        SELECT workout_sessions.id AS sessionId,
            workout_sessions.title AS title,
            workout_sessions.startedAtEpochMillis AS startedAtEpochMillis,
            workout_sessions.endedAtEpochMillis AS endedAtEpochMillis,
            COALESCE(SUM(CASE WHEN workout_sets.completed = 1 THEN 1 ELSE 0 END), 0) AS completedSetCount,
            COALESCE(SUM(CASE WHEN workout_sets.completed = 1 AND workout_sets.reps IS NOT NULL AND workout_sets.weightKg IS NOT NULL THEN workout_sets.reps * workout_sets.weightKg ELSE 0 END), 0) AS totalVolumeKg
        FROM workout_sessions
        LEFT JOIN workout_sets ON workout_sets.sessionId = workout_sessions.id
        WHERE workout_sessions.id = :sessionId
        AND workout_sessions.status = 'completed'
        GROUP BY workout_sessions.id
        LIMIT 1
        """,
    )
    suspend fun getWorkoutHistorySummary(sessionId: String): WorkoutHistorySummaryRow?

    @Query(
        """
        SELECT workout_sessions.id AS sessionId,
            exercises.id AS exerciseId,
            exercises.name AS exerciseName,
            exercises.category AS category,
            exercises.equipment AS equipment,
            exercises.targetMuscles AS targetMuscles,
            exercises.isCustom AS isCustom,
            workout_sessions.startedAtEpochMillis AS startedAtEpochMillis,
            workout_sets.reps AS reps,
            workout_sets.weightKg AS weightKg,
            workout_sets.completed AS completed
        FROM workout_sets
        INNER JOIN workout_sessions ON workout_sessions.id = workout_sets.sessionId
        INNER JOIN exercises ON exercises.id = workout_sets.exerciseId
        WHERE workout_sets.exerciseId = :exerciseId
        AND workout_sessions.status = 'completed'
        ORDER BY workout_sessions.startedAtEpochMillis ASC, workout_sets.sortOrder ASC
        """,
    )
    fun observeExerciseProgressSetRows(exerciseId: String): Flow<List<ExerciseProgressSetRow>>

    @Query(
        """
        SELECT workout_sessions.id AS sessionId,
            exercises.id AS exerciseId,
            exercises.name AS exerciseName,
            exercises.category AS category,
            exercises.equipment AS equipment,
            exercises.targetMuscles AS targetMuscles,
            exercises.isCustom AS isCustom,
            workout_sessions.startedAtEpochMillis AS startedAtEpochMillis,
            workout_sets.reps AS reps,
            workout_sets.weightKg AS weightKg,
            workout_sets.completed AS completed
        FROM workout_sets
        INNER JOIN workout_sessions ON workout_sessions.id = workout_sets.sessionId
        INNER JOIN exercises ON exercises.id = workout_sets.exerciseId
        WHERE workout_sessions.status = 'completed'
        ORDER BY workout_sessions.startedAtEpochMillis ASC, workout_sets.sortOrder ASC
        """,
    )
    fun observeCompletedExerciseProgressSetRows(): Flow<List<ExerciseProgressSetRow>>

    @Query(
        """
        SELECT workout_sets.id AS setId,
            workout_sets.sessionId AS sessionId,
            exercises.id AS exerciseId,
            exercises.name AS exerciseName,
            exercises.category AS category,
            exercises.equipment AS equipment,
            exercises.targetMuscles AS targetMuscles,
            exercises.isCustom AS isCustom,
            workout_sets.sortOrder AS sortOrder,
            workout_sets.setType AS setType,
            workout_sets.reps AS reps,
            workout_sets.weightKg AS weightKg,
            workout_sets.rpe AS rpe,
            workout_sets.notes AS notes,
            workout_sets.completed AS completed,
            workout_sets.supersetGroupId AS supersetGroupId,
            workout_sets.restSeconds AS restSeconds,
            exercises.imageUrl AS imageUrl,
            exercises.gifUrl AS gifUrl
        FROM workout_sets
        INNER JOIN exercises ON exercises.id = workout_sets.exerciseId
        WHERE workout_sets.sessionId = :sessionId
        ORDER BY workout_sets.sortOrder ASC
        """,
    )
    fun observeWorkoutSetDetailRows(sessionId: String): Flow<List<WorkoutSetDetailRow>>

    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId ORDER BY sortOrder")
    fun observeRoutineExercises(routineId: String): Flow<List<RoutineExerciseEntity>>

    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId ORDER BY sortOrder")
    suspend fun getRoutineExercises(routineId: String): List<RoutineExerciseEntity>

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY sortOrder")
    fun observeWorkoutSets(sessionId: String): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY sortOrder")
    suspend fun getWorkoutSets(sessionId: String): List<WorkoutSetEntity>

    @Query("SELECT * FROM workout_sets WHERE id = :setId LIMIT 1")
    suspend fun getWorkoutSet(setId: String): WorkoutSetEntity?

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId AND exerciseId = :exerciseId ORDER BY sortOrder DESC LIMIT 1")
    suspend fun getLastWorkoutSetForExercise(sessionId: String, exerciseId: String): WorkoutSetEntity?

    @Query(
        """
        SELECT workout_sets.*
        FROM workout_sets
        INNER JOIN workout_sessions ON workout_sessions.id = workout_sets.sessionId
        WHERE workout_sets.exerciseId = :exerciseId
        AND workout_sets.completed = 1
        AND workout_sets.reps IS NOT NULL
        AND workout_sets.weightKg IS NOT NULL
        AND workout_sessions.status = 'completed'
        AND workout_sessions.startedAtEpochMillis < :beforeStartedAtEpochMillis
        ORDER BY workout_sessions.startedAtEpochMillis DESC, workout_sets.sortOrder DESC
        LIMIT 1
        """,
    )
    suspend fun getLatestCompletedSetForExerciseBefore(
        exerciseId: String,
        beforeStartedAtEpochMillis: Long,
    ): WorkoutSetEntity?

    @Query(
        """
        SELECT workout_sets.*
        FROM workout_sets
        INNER JOIN workout_sessions ON workout_sessions.id = workout_sets.sessionId
        WHERE workout_sets.exerciseId = :exerciseId
        AND workout_sets.completed = 1
        AND workout_sets.reps IS NOT NULL
        AND workout_sets.weightKg IS NOT NULL
        AND workout_sessions.status = 'completed'
        AND workout_sessions.startedAtEpochMillis < :beforeStartedAtEpochMillis
        """,
    )
    suspend fun getCompletedSetsForExerciseBefore(
        exerciseId: String,
        beforeStartedAtEpochMillis: Long,
    ): List<WorkoutSetEntity>

    @Query("SELECT MAX(sortOrder) FROM workout_sets WHERE sessionId = :sessionId")
    suspend fun getMaxWorkoutSetSortOrder(sessionId: String): Int?

    @Query(
        """
        SELECT *
        FROM workout_sets
        WHERE sessionId = :sessionId
        AND completed = 1
        ORDER BY sortOrder
        """,
    )
    suspend fun getCompletedWorkoutSets(sessionId: String): List<WorkoutSetEntity>

    @Query(
        "SELECT workout_sets.* FROM workout_sets " +
            "INNER JOIN workout_sessions ON workout_sessions.id = workout_sets.sessionId " +
            "WHERE workout_sessions.startedAtEpochMillis >= :startEpochMillis " +
            "AND workout_sessions.startedAtEpochMillis < :endEpochMillis " +
            "AND workout_sessions.status = 'completed' " +
            "ORDER BY workout_sessions.startedAtEpochMillis, workout_sets.sortOrder",
    )
    fun observeWorkoutSetsForDate(
        startEpochMillis: Long,
        endEpochMillis: Long,
    ): Flow<List<WorkoutSetEntity>>

    @Upsert
    suspend fun upsertExercise(exercise: ExerciseEntity)

    @Update
    suspend fun updateExercise(exercise: ExerciseEntity): Int

    @Upsert
    suspend fun upsertExercises(exercises: List<ExerciseEntity>)

    @Upsert
    suspend fun upsertRoutine(routine: RoutineEntity)

    @Upsert
    suspend fun upsertRoutineFolder(folder: RoutineFolderEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRoutineFolder(folder: RoutineFolderEntity): Long

    @Update
    suspend fun updateRoutineFolder(folder: RoutineFolderEntity): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRoutine(routine: RoutineEntity): Long

    @Update
    suspend fun updateRoutine(routine: RoutineEntity): Int

    @Upsert
    suspend fun upsertRoutines(routines: List<RoutineEntity>)

    @Upsert
    suspend fun upsertRoutineExercise(routineExercise: RoutineExerciseEntity)

    @Upsert
    suspend fun upsertRoutineExercises(routineExercises: List<RoutineExerciseEntity>)

    @Upsert
    suspend fun upsertRoutineExerciseSets(routineExerciseSets: List<RoutineExerciseSetEntity>)

    @Upsert
    suspend fun upsertWorkoutSession(session: WorkoutSessionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWorkoutSession(session: WorkoutSessionEntity): Long

    @Update
    suspend fun updateWorkoutSession(session: WorkoutSessionEntity): Int

    @Upsert
    suspend fun upsertWorkoutSet(set: WorkoutSetEntity)

    @Upsert
    suspend fun upsertTrainingSettings(settings: TrainingSettingsEntity)

    @Query("UPDATE workout_sets SET completed = :completed WHERE id = :setId")
    suspend fun updateWorkoutSetCompletion(setId: String, completed: Boolean)

    @Query("UPDATE workout_sets SET supersetGroupId = :groupId WHERE sessionId = :sessionId AND exerciseId = :exerciseId")
    suspend fun setExerciseSupersetGroup(sessionId: String, exerciseId: String, groupId: String?)

    @Query("UPDATE workout_sets SET supersetGroupId = NULL WHERE sessionId = :sessionId AND supersetGroupId = :groupId")
    suspend fun clearSupersetGroup(sessionId: String, groupId: String)

    @Query("DELETE FROM workout_sets WHERE id = :setId")
    suspend fun deleteWorkoutSetById(setId: String)

    @Query("UPDATE workout_sessions SET status = :status, endedAtEpochMillis = :endedAtEpochMillis WHERE id = :sessionId")
    suspend fun updateWorkoutSessionStatus(sessionId: String, status: String, endedAtEpochMillis: Long?)

    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun deleteRoutineExercises(routineId: String)

    @Query("UPDATE routines SET folderId = NULL WHERE folderId = :folderId")
    suspend fun clearRoutineFolder(folderId: String)

    @Query("UPDATE routines SET folderId = :folderId, updatedAtEpochMillis = :updatedAtEpochMillis WHERE id = :routineId")
    suspend fun updateRoutineFolderAssignment(routineId: String, folderId: String?, updatedAtEpochMillis: Long)

    @Query("DELETE FROM routine_folders WHERE id = :folderId")
    suspend fun deleteRoutineFolderById(folderId: String)

    @Query("DELETE FROM routines WHERE id = :routineId")
    suspend fun deleteRoutineById(routineId: String)
}
