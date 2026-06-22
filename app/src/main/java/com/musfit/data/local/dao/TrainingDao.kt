package com.musfit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.musfit.data.local.entity.ExerciseEntity
import com.musfit.data.local.entity.RoutineEntity
import com.musfit.data.local.entity.RoutineExerciseEntity
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
)

@Dao
interface TrainingDao {
    @Query("SELECT * FROM exercises ORDER BY name")
    fun observeExercises(): Flow<List<ExerciseEntity>>

    @Query(
        """
        SELECT *
        FROM exercises
        WHERE (:query = '' OR name LIKE '%' || :query || '%')
        AND (:muscle IS NULL OR targetMuscles LIKE '%' || :muscle || '%')
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

    @Query("SELECT * FROM routines ORDER BY createdAtEpochMillis DESC")
    fun observeRoutines(): Flow<List<RoutineEntity>>

    @Query(
        """
        SELECT routines.id AS id,
            routines.name AS name,
            routines.notes AS notes,
            COUNT(routine_exercises.id) AS exerciseCount,
            COALESCE(SUM(routine_exercises.targetSets), 0) AS targetSetCount,
            routines.isStarter AS isStarter
        FROM routines
        LEFT JOIN routine_exercises ON routine_exercises.routineId = routines.id
        GROUP BY routines.id
        ORDER BY routines.isStarter DESC, routines.updatedAtEpochMillis DESC, routines.name ASC
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
            routine_exercises.targetReps AS targetReps
        FROM routine_exercises
        INNER JOIN exercises ON exercises.id = routine_exercises.exerciseId
        WHERE routine_exercises.routineId = :routineId
        ORDER BY routine_exercises.sortOrder ASC
        """,
    )
    suspend fun getRoutineExerciseDetailRows(routineId: String): List<RoutineExerciseDetailRow>

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
        ORDER BY startedAtEpochMillis DESC
        LIMIT 1
        """,
    )
    suspend fun getLatestCompletedWorkoutSession(): WorkoutSessionEntity?

    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId ORDER BY sortOrder")
    fun observeRoutineExercises(routineId: String): Flow<List<RoutineExerciseEntity>>

    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId ORDER BY sortOrder")
    suspend fun getRoutineExercises(routineId: String): List<RoutineExerciseEntity>

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY sortOrder")
    fun observeWorkoutSets(sessionId: String): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY sortOrder")
    suspend fun getWorkoutSets(sessionId: String): List<WorkoutSetEntity>

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercise(exercise: ExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercises(exercises: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoutine(routine: RoutineEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRoutine(routine: RoutineEntity): Long

    @Update
    suspend fun updateRoutine(routine: RoutineEntity): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoutines(routines: List<RoutineEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoutineExercise(routineExercise: RoutineExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoutineExercises(routineExercises: List<RoutineExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkoutSession(session: WorkoutSessionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWorkoutSession(session: WorkoutSessionEntity): Long

    @Update
    suspend fun updateWorkoutSession(session: WorkoutSessionEntity): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkoutSet(set: WorkoutSetEntity)

    @Query("UPDATE workout_sets SET completed = :completed WHERE id = :setId")
    suspend fun updateWorkoutSetCompletion(setId: String, completed: Boolean)

    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun deleteRoutineExercises(routineId: String)

    @Query("DELETE FROM routines WHERE id = :routineId")
    suspend fun deleteRoutineById(routineId: String)
}
