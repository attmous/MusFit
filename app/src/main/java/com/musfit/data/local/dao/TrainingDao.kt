package com.musfit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.musfit.data.local.entity.ExerciseEntity
import com.musfit.data.local.entity.ExerciseNoteEntity
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

data class ExerciseDetailRow(
    val id: String,
    val name: String,
    val category: String,
    val equipment: String?,
    val targetMuscles: String,
    val primaryMuscles: String,
    val secondaryMuscles: String,
    val instructions: String?,
    val localNotes: String?,
    val isCustom: Boolean,
    val imageUrl: String?,
    val gifUrl: String?,
)

@Dao
interface TrainingDao {
    @Query("SELECT * FROM exercises WHERE accountId IS NULL OR accountId = :accountId ORDER BY isCustom ASC, name ASC")
    fun observeExercises(accountId: String): Flow<List<ExerciseEntity>>

    @Query(
        """
        SELECT *
        FROM exercises
        WHERE (accountId IS NULL OR accountId = :accountId)
        AND (:query = '' OR name LIKE '%' || :query || '%' OR targetMuscles LIKE '%' || :query || '%' OR primaryMuscles LIKE '%' || :query || '%' OR secondaryMuscles LIKE '%' || :query || '%')
        AND (:muscle IS NULL OR targetMuscles LIKE '%' || :muscle || '%' OR primaryMuscles LIKE '%' || :muscle || '%' OR secondaryMuscles LIKE '%' || :muscle || '%')
        AND (:equipment IS NULL OR equipment = :equipment)
        ORDER BY isCustom ASC, name ASC
        """,
    )
    fun observeExercisesFiltered(
        accountId: String,
        query: String,
        muscle: String?,
        equipment: String?,
    ): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE (accountId IS NULL OR accountId = :accountId) AND name = :name ORDER BY accountId IS NULL ASC LIMIT 1")
    suspend fun getExerciseByName(accountId: String, name: String): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE (accountId IS NULL OR accountId = :accountId) AND id = :exerciseId LIMIT 1")
    suspend fun getExercise(accountId: String, exerciseId: String): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE (accountId IS NULL OR accountId = :accountId) AND LOWER(name) = LOWER(:name) ORDER BY accountId IS NULL ASC LIMIT 1")
    suspend fun getExerciseByNormalizedName(accountId: String, name: String): ExerciseEntity?

    @Query(
        """
        SELECT exercises.id AS id,
            exercises.name AS name,
            exercises.category AS category,
            exercises.equipment AS equipment,
            exercises.targetMuscles AS targetMuscles,
            exercises.primaryMuscles AS primaryMuscles,
            exercises.secondaryMuscles AS secondaryMuscles,
            exercises.instructions AS instructions,
            exercise_notes.notes AS localNotes,
            exercises.isCustom AS isCustom,
            exercises.imageUrl AS imageUrl,
            exercises.gifUrl AS gifUrl
        FROM exercises
        LEFT JOIN exercise_notes ON exercise_notes.accountId = :accountId AND exercise_notes.exerciseId = exercises.id
        WHERE (exercises.accountId IS NULL OR exercises.accountId = :accountId)
        AND exercises.id = :exerciseId
        LIMIT 1
        """,
    )
    suspend fun getExerciseDetail(accountId: String, exerciseId: String): ExerciseDetailRow?

    @Query("SELECT * FROM routines WHERE accountId = :accountId ORDER BY createdAtEpochMillis DESC")
    fun observeRoutines(accountId: String): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routine_folders WHERE accountId = :accountId ORDER BY sortOrder ASC, name ASC")
    fun observeRoutineFolders(accountId: String): Flow<List<RoutineFolderEntity>>

    @Query("SELECT * FROM routine_folders WHERE accountId = :accountId AND id = :folderId LIMIT 1")
    suspend fun getRoutineFolder(accountId: String, folderId: String): RoutineFolderEntity?

    @Query("SELECT * FROM routine_folders WHERE accountId = :accountId AND LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getRoutineFolderByName(accountId: String, name: String): RoutineFolderEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM routine_folders WHERE accountId = :accountId")
    suspend fun getMaxRoutineFolderSortOrder(accountId: String): Int

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
        LEFT JOIN routine_folders ON routine_folders.accountId = routines.accountId AND routine_folders.id = routines.folderId
        LEFT JOIN routine_exercises ON routine_exercises.accountId = routines.accountId AND routine_exercises.routineId = routines.id
        LEFT JOIN exercises ON exercises.id = routine_exercises.exerciseId
        WHERE routines.accountId = :accountId
        GROUP BY routines.id
        ORDER BY routine_folders.sortOrder ASC, routine_folders.name ASC, routines.updatedAtEpochMillis DESC, routines.name ASC
        """,
    )
    fun observeRoutineSummaries(accountId: String): Flow<List<RoutineSummaryRow>>

    @Query("SELECT * FROM workout_sessions WHERE accountId = :accountId ORDER BY startedAtEpochMillis DESC")
    fun observeWorkoutSessions(accountId: String): Flow<List<WorkoutSessionEntity>>

    @Query(
        """
        SELECT workout_sessions.id AS sessionId,
            workout_sessions.title AS title,
            workout_sessions.startedAtEpochMillis AS startedAtEpochMillis,
            COALESCE(SUM(CASE WHEN workout_sets.completed = 1 THEN 1 ELSE 0 END), 0) AS completedSetCount,
            COALESCE(SUM(CASE WHEN workout_sets.completed = 1 AND workout_sets.reps IS NOT NULL AND workout_sets.weightKg IS NOT NULL THEN workout_sets.reps * workout_sets.weightKg ELSE 0 END), 0) AS totalVolumeKg
        FROM workout_sessions
        LEFT JOIN workout_sets ON workout_sets.accountId = workout_sessions.accountId AND workout_sets.sessionId = workout_sessions.id
        WHERE workout_sessions.accountId = :accountId
        AND workout_sessions.status = 'active'
        GROUP BY workout_sessions.id
        ORDER BY workout_sessions.startedAtEpochMillis DESC
        LIMIT 1
        """,
    )
    fun observeActiveWorkoutSummary(accountId: String): Flow<ActiveWorkoutSummaryRow?>

    @Query("SELECT * FROM workout_sessions WHERE accountId = :accountId AND status = 'active' ORDER BY startedAtEpochMillis DESC LIMIT 1")
    fun observeActiveWorkoutSession(accountId: String): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM training_settings WHERE accountId = :accountId AND id = 'default' LIMIT 1")
    fun observeTrainingSettings(accountId: String): Flow<TrainingSettingsEntity?>

    @Query("SELECT * FROM training_settings WHERE accountId = :accountId AND id = 'default' LIMIT 1")
    suspend fun getTrainingSettings(accountId: String): TrainingSettingsEntity?

    @Query("SELECT * FROM workout_sessions WHERE accountId = :accountId AND id = :sessionId LIMIT 1")
    suspend fun getWorkoutSession(accountId: String, sessionId: String): WorkoutSessionEntity?

    @Query("SELECT * FROM routines WHERE accountId = :accountId AND id = :routineId LIMIT 1")
    suspend fun getRoutine(accountId: String, routineId: String): RoutineEntity?

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
        WHERE routine_exercises.accountId = :accountId
        AND routine_exercises.routineId = :routineId
        AND (exercises.accountId IS NULL OR exercises.accountId = :accountId)
        ORDER BY routine_exercises.sortOrder ASC
        """,
    )
    suspend fun getRoutineExerciseDetailRows(accountId: String, routineId: String): List<RoutineExerciseDetailRow>

    @Query(
        """
        SELECT routine_exercise_sets.id AS id,
            routine_exercise_sets.routineExerciseId AS routineExerciseId,
            routine_exercise_sets.sortOrder AS sortOrder,
            routine_exercise_sets.setType AS setType,
            routine_exercise_sets.targetReps AS targetReps,
            routine_exercise_sets.targetWeightKg AS targetWeightKg
        FROM routine_exercise_sets
        INNER JOIN routine_exercises ON routine_exercises.accountId = routine_exercise_sets.accountId AND routine_exercises.id = routine_exercise_sets.routineExerciseId
        WHERE routine_exercises.accountId = :accountId
        AND routine_exercises.routineId = :routineId
        ORDER BY routine_exercises.sortOrder ASC, routine_exercise_sets.sortOrder ASC
        """,
    )
    suspend fun getRoutineExerciseSetDetailRows(accountId: String, routineId: String): List<RoutineExerciseSetDetailRow>

    @Query("SELECT * FROM workout_sessions WHERE accountId = :accountId ORDER BY startedAtEpochMillis DESC LIMIT 1")
    suspend fun getLatestWorkoutSession(accountId: String): WorkoutSessionEntity?

    @Query(
        """
        SELECT *
        FROM workout_sessions
        WHERE accountId = :accountId
        AND status = 'active'
        ORDER BY startedAtEpochMillis DESC
        LIMIT 1
        """,
    )
    suspend fun getLatestActiveWorkoutSession(accountId: String): WorkoutSessionEntity?

    @Query(
        """
        SELECT *
        FROM workout_sessions
        WHERE accountId = :accountId
        AND status = 'completed'
        AND EXISTS (
            SELECT 1
            FROM workout_sets
            WHERE workout_sets.accountId = workout_sessions.accountId
            AND workout_sets.sessionId = workout_sessions.id
            AND workout_sets.completed = 1
        )
        ORDER BY startedAtEpochMillis DESC
        LIMIT 1
        """,
    )
    suspend fun getLatestCompletedWorkoutSession(accountId: String): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE accountId = :accountId AND id = :sessionId AND status = 'completed' LIMIT 1")
    suspend fun getCompletedWorkoutSession(accountId: String, sessionId: String): WorkoutSessionEntity?

    @Query(
        """
        SELECT workout_sessions.id AS sessionId,
            workout_sessions.title AS title,
            workout_sessions.startedAtEpochMillis AS startedAtEpochMillis,
            workout_sessions.endedAtEpochMillis AS endedAtEpochMillis,
            COALESCE(SUM(CASE WHEN workout_sets.completed = 1 THEN 1 ELSE 0 END), 0) AS completedSetCount,
            COALESCE(SUM(CASE WHEN workout_sets.completed = 1 AND workout_sets.reps IS NOT NULL AND workout_sets.weightKg IS NOT NULL THEN workout_sets.reps * workout_sets.weightKg ELSE 0 END), 0) AS totalVolumeKg
        FROM workout_sessions
        LEFT JOIN workout_sets ON workout_sets.accountId = workout_sessions.accountId AND workout_sets.sessionId = workout_sessions.id
        WHERE workout_sessions.accountId = :accountId
        AND workout_sessions.status = 'completed'
        GROUP BY workout_sessions.id
        ORDER BY workout_sessions.startedAtEpochMillis DESC
        """,
    )
    fun observeWorkoutHistorySummaries(accountId: String): Flow<List<WorkoutHistorySummaryRow>>

    @Query(
        """
        SELECT workout_sessions.id AS sessionId,
            workout_sessions.title AS title,
            workout_sessions.startedAtEpochMillis AS startedAtEpochMillis,
            workout_sessions.endedAtEpochMillis AS endedAtEpochMillis,
            COALESCE(SUM(CASE WHEN workout_sets.completed = 1 THEN 1 ELSE 0 END), 0) AS completedSetCount,
            COALESCE(SUM(CASE WHEN workout_sets.completed = 1 AND workout_sets.reps IS NOT NULL AND workout_sets.weightKg IS NOT NULL THEN workout_sets.reps * workout_sets.weightKg ELSE 0 END), 0) AS totalVolumeKg
        FROM workout_sessions
        LEFT JOIN workout_sets ON workout_sets.accountId = workout_sessions.accountId AND workout_sets.sessionId = workout_sessions.id
        WHERE workout_sessions.accountId = :accountId
        AND workout_sessions.id = :sessionId
        AND workout_sessions.status = 'completed'
        GROUP BY workout_sessions.id
        LIMIT 1
        """,
    )
    suspend fun getWorkoutHistorySummary(accountId: String, sessionId: String): WorkoutHistorySummaryRow?

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
        INNER JOIN workout_sessions ON workout_sessions.accountId = workout_sets.accountId AND workout_sessions.id = workout_sets.sessionId
        INNER JOIN exercises ON exercises.id = workout_sets.exerciseId
        WHERE workout_sets.accountId = :accountId
        AND workout_sets.exerciseId = :exerciseId
        AND workout_sessions.status = 'completed'
        ORDER BY workout_sessions.startedAtEpochMillis ASC, workout_sets.sortOrder ASC
        """,
    )
    fun observeExerciseProgressSetRows(accountId: String, exerciseId: String): Flow<List<ExerciseProgressSetRow>>

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
        INNER JOIN workout_sessions ON workout_sessions.accountId = workout_sets.accountId AND workout_sessions.id = workout_sets.sessionId
        INNER JOIN exercises ON exercises.id = workout_sets.exerciseId
        WHERE workout_sets.accountId = :accountId
        AND workout_sessions.status = 'completed'
        ORDER BY workout_sessions.startedAtEpochMillis ASC, workout_sets.sortOrder ASC
        """,
    )
    fun observeCompletedExerciseProgressSetRows(accountId: String): Flow<List<ExerciseProgressSetRow>>

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
        WHERE workout_sets.accountId = :accountId
        AND workout_sets.sessionId = :sessionId
        AND (exercises.accountId IS NULL OR exercises.accountId = :accountId)
        ORDER BY workout_sets.sortOrder ASC
        """,
    )
    fun observeWorkoutSetDetailRows(accountId: String, sessionId: String): Flow<List<WorkoutSetDetailRow>>

    @Query("SELECT * FROM routine_exercises WHERE accountId = :accountId AND routineId = :routineId ORDER BY sortOrder")
    fun observeRoutineExercises(accountId: String, routineId: String): Flow<List<RoutineExerciseEntity>>

    @Query("SELECT * FROM routine_exercises WHERE accountId = :accountId AND routineId = :routineId ORDER BY sortOrder")
    suspend fun getRoutineExercises(accountId: String, routineId: String): List<RoutineExerciseEntity>

    @Query("SELECT * FROM workout_sets WHERE accountId = :accountId AND sessionId = :sessionId ORDER BY sortOrder")
    fun observeWorkoutSets(accountId: String, sessionId: String): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets WHERE accountId = :accountId AND sessionId = :sessionId ORDER BY sortOrder")
    suspend fun getWorkoutSets(accountId: String, sessionId: String): List<WorkoutSetEntity>

    @Query("SELECT * FROM workout_sets WHERE accountId = :accountId AND id = :setId LIMIT 1")
    suspend fun getWorkoutSet(accountId: String, setId: String): WorkoutSetEntity?

    @Query("SELECT * FROM workout_sets WHERE accountId = :accountId AND sessionId = :sessionId AND exerciseId = :exerciseId ORDER BY sortOrder DESC LIMIT 1")
    suspend fun getLastWorkoutSetForExercise(accountId: String, sessionId: String, exerciseId: String): WorkoutSetEntity?

    @Query(
        """
        SELECT workout_sets.*
        FROM workout_sets
        INNER JOIN workout_sessions ON workout_sessions.accountId = workout_sets.accountId AND workout_sessions.id = workout_sets.sessionId
        WHERE workout_sets.accountId = :accountId
        AND workout_sets.exerciseId = :exerciseId
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
        accountId: String,
        exerciseId: String,
        beforeStartedAtEpochMillis: Long,
    ): WorkoutSetEntity?

    @Query(
        """
        SELECT workout_sets.*
        FROM workout_sets
        INNER JOIN workout_sessions ON workout_sessions.accountId = workout_sets.accountId AND workout_sessions.id = workout_sets.sessionId
        WHERE workout_sets.accountId = :accountId
        AND workout_sets.exerciseId = :exerciseId
        AND workout_sets.completed = 1
        AND workout_sets.reps IS NOT NULL
        AND workout_sets.weightKg IS NOT NULL
        AND workout_sessions.status = 'completed'
        AND workout_sessions.startedAtEpochMillis < :beforeStartedAtEpochMillis
        """,
    )
    suspend fun getCompletedSetsForExerciseBefore(
        accountId: String,
        exerciseId: String,
        beforeStartedAtEpochMillis: Long,
    ): List<WorkoutSetEntity>

    @Query("SELECT MAX(sortOrder) FROM workout_sets WHERE accountId = :accountId AND sessionId = :sessionId")
    suspend fun getMaxWorkoutSetSortOrder(accountId: String, sessionId: String): Int?

    @Query(
        """
        SELECT *
        FROM workout_sets
        WHERE accountId = :accountId
        AND sessionId = :sessionId
        AND completed = 1
        ORDER BY sortOrder
        """,
    )
    suspend fun getCompletedWorkoutSets(accountId: String, sessionId: String): List<WorkoutSetEntity>

    @Query(
        "SELECT workout_sets.* FROM workout_sets " +
            "INNER JOIN workout_sessions ON workout_sessions.accountId = workout_sets.accountId AND workout_sessions.id = workout_sets.sessionId " +
            "WHERE workout_sessions.accountId = :accountId " +
            "AND workout_sessions.startedAtEpochMillis >= :startEpochMillis " +
            "AND workout_sessions.startedAtEpochMillis < :endEpochMillis " +
            "AND workout_sessions.status = 'completed' " +
            "ORDER BY workout_sessions.startedAtEpochMillis, workout_sets.sortOrder",
    )
    fun observeWorkoutSetsForDate(
        accountId: String,
        startEpochMillis: Long,
        endEpochMillis: Long,
    ): Flow<List<WorkoutSetEntity>>

    @Upsert
    suspend fun upsertExerciseDefinition(exercise: ExerciseEntity)

    @Update
    suspend fun updateExerciseDefinition(exercise: ExerciseEntity): Int

    @Upsert
    suspend fun upsertSharedExercises(exercises: List<ExerciseEntity>)

    @Upsert
    suspend fun upsertExerciseDefinitions(exercises: List<ExerciseEntity>)

    @Upsert
    suspend fun upsertExerciseNote(note: ExerciseNoteEntity)

    @Query("DELETE FROM exercise_notes WHERE accountId = :accountId AND exerciseId = :exerciseId")
    suspend fun deleteExerciseNote(accountId: String, exerciseId: String)

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

    @Query("UPDATE workout_sets SET completed = :completed WHERE accountId = :accountId AND id = :setId")
    suspend fun updateWorkoutSetCompletion(accountId: String, setId: String, completed: Boolean)

    @Query("UPDATE workout_sets SET supersetGroupId = :groupId WHERE accountId = :accountId AND sessionId = :sessionId AND exerciseId = :exerciseId")
    suspend fun setExerciseSupersetGroup(accountId: String, sessionId: String, exerciseId: String, groupId: String?)

    @Query("UPDATE workout_sets SET supersetGroupId = NULL WHERE accountId = :accountId AND sessionId = :sessionId AND supersetGroupId = :groupId")
    suspend fun clearSupersetGroup(accountId: String, sessionId: String, groupId: String)

    @Query("DELETE FROM workout_sets WHERE accountId = :accountId AND id = :setId")
    suspend fun deleteWorkoutSetById(accountId: String, setId: String)

    @Query("UPDATE workout_sessions SET status = :status, endedAtEpochMillis = :endedAtEpochMillis WHERE accountId = :accountId AND id = :sessionId")
    suspend fun updateWorkoutSessionStatus(accountId: String, sessionId: String, status: String, endedAtEpochMillis: Long?)

    @Query("DELETE FROM routine_exercises WHERE accountId = :accountId AND routineId = :routineId")
    suspend fun deleteRoutineExercises(accountId: String, routineId: String)

    @Query("UPDATE routines SET folderId = NULL WHERE accountId = :accountId AND folderId = :folderId")
    suspend fun clearRoutineFolder(accountId: String, folderId: String)

    @Query("UPDATE routines SET folderId = :folderId, updatedAtEpochMillis = :updatedAtEpochMillis WHERE accountId = :accountId AND id = :routineId")
    suspend fun updateRoutineFolderAssignment(accountId: String, routineId: String, folderId: String?, updatedAtEpochMillis: Long)

    @Query("DELETE FROM routine_folders WHERE accountId = :accountId AND id = :folderId")
    suspend fun deleteRoutineFolderById(accountId: String, folderId: String)

    @Query("UPDATE workout_sessions SET routineId = NULL WHERE accountId = :accountId AND routineId = :routineId")
    suspend fun clearWorkoutRoutine(accountId: String, routineId: String)

    @Query("DELETE FROM routines WHERE accountId = :accountId AND id = :routineId")
    suspend fun deleteRoutineById(accountId: String, routineId: String)
}
