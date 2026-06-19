package com.musfit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musfit.data.local.entity.ExerciseEntity
import com.musfit.data.local.entity.RoutineEntity
import com.musfit.data.local.entity.RoutineExerciseEntity
import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingDao {
    @Query("SELECT * FROM exercises ORDER BY name")
    fun observeExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM routines ORDER BY createdAtEpochMillis DESC")
    fun observeRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM workout_sessions ORDER BY startedAtEpochMillis DESC")
    fun observeWorkoutSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId ORDER BY sortOrder")
    fun observeRoutineExercises(routineId: String): Flow<List<RoutineExerciseEntity>>

    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId ORDER BY sortOrder")
    suspend fun getRoutineExercises(routineId: String): List<RoutineExerciseEntity>

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY sortOrder")
    fun observeWorkoutSets(sessionId: String): Flow<List<WorkoutSetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercise(exercise: ExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoutine(routine: RoutineEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoutineExercise(routineExercise: RoutineExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkoutSession(session: WorkoutSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkoutSet(set: WorkoutSetEntity)
}
