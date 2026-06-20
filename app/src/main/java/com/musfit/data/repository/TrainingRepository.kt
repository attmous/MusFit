package com.musfit.data.repository

import androidx.room.withTransaction
import com.musfit.data.local.MusFitDatabase
import com.musfit.data.local.dao.TrainingDao
import com.musfit.data.local.entity.ExerciseEntity
import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity
import com.musfit.domain.model.WorkoutSetInput
import com.musfit.domain.training.WorkoutCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

data class LoggedWorkoutSet(
    val id: String,
    val exerciseName: String,
    val reps: Int,
    val weightKg: Double,
    val completed: Boolean,
)

data class TrainingSummary(
    val completedSetCount: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val bestEstimatedOneRepMaxKg: Double = 0.0,
)

data class WorkoutForExport(
    val session: WorkoutSessionEntity,
    val sets: List<WorkoutSetEntity>,
)

interface TrainingRepository {
    suspend fun addCompletedSet(
        exerciseName: String,
        reps: Int,
        weightKg: Double,
    ): LoggedWorkoutSet

    suspend fun setCompletion(setId: String, completed: Boolean)

    fun observeDailyTrainingSummary(date: LocalDate): Flow<TrainingSummary>

    suspend fun getLatestWorkoutForExport(): WorkoutForExport?

    suspend fun markWorkoutExported(
        sessionId: String,
        recordId: String,
        exportedAtEpochMillis: Long,
    )
}

class LocalTrainingRepository @Inject constructor(
    private val database: MusFitDatabase,
    private val trainingDao: TrainingDao,
) : TrainingRepository {
    private var clock: () -> Long = { System.currentTimeMillis() }
    private var activeSessionId: String? = null

    internal constructor(
        database: MusFitDatabase,
        trainingDao: TrainingDao,
        clock: () -> Long,
    ) : this(database, trainingDao) {
        this.clock = clock
    }

    override suspend fun addCompletedSet(
        exerciseName: String,
        reps: Int,
        weightKg: Double,
    ): LoggedWorkoutSet {
        require(reps > 0) { "Reps must be positive" }
        require(weightKg.isFinite() && weightKg > 0.0) { "Weight must be positive" }

        return database.withTransaction {
            val now = clock()
            val resolvedExerciseName = exerciseName.trim().ifBlank { DEFAULT_EXERCISE_NAME }
            val exercise = trainingDao.getExerciseByName(resolvedExerciseName)
                ?: ExerciseEntity(
                    id = UUID.randomUUID().toString(),
                    name = resolvedExerciseName,
                    category = "strength",
                    equipment = null,
                    targetMuscles = "",
                    isCustom = true,
                ).also { trainingDao.upsertExercise(it) }

            val session = currentOrNewSession(now)
            val nextSortOrder = trainingDao.getWorkoutSets(session.id).size
            val set = WorkoutSetEntity(
                id = UUID.randomUUID().toString(),
                sessionId = session.id,
                exerciseId = exercise.id,
                sortOrder = nextSortOrder,
                reps = reps,
                weightKg = weightKg,
                durationSeconds = null,
                distanceMeters = null,
                rpe = null,
                notes = null,
                completed = true,
            )

            trainingDao.upsertWorkoutSession(session.copy(endedAtEpochMillis = now))
            trainingDao.upsertWorkoutSet(set)

            LoggedWorkoutSet(
                id = set.id,
                exerciseName = resolvedExerciseName,
                reps = reps,
                weightKg = weightKg,
                completed = true,
            )
        }
    }

    override suspend fun setCompletion(setId: String, completed: Boolean) {
        trainingDao.updateWorkoutSetCompletion(setId, completed)
    }

    override fun observeDailyTrainingSummary(date: LocalDate): Flow<TrainingSummary> {
        val range = date.dayRange()
        return trainingDao.observeWorkoutSetsForDate(
            startEpochMillis = range.startEpochMillis,
            endEpochMillis = range.endEpochMillis,
        ).map { sets ->
            val inputs = sets.mapNotNull { set ->
                val reps = set.reps
                val weightKg = set.weightKg
                if (reps == null || weightKg == null) {
                    null
                } else {
                    WorkoutSetInput(
                        exerciseId = set.exerciseId,
                        reps = reps,
                        weightKg = weightKg,
                        completed = set.completed,
                    )
                }
            }
            val records = WorkoutCalculator.personalRecords(inputs)
            TrainingSummary(
                completedSetCount = inputs.count { it.completed },
                totalVolumeKg = records.totalVolumeKg,
                bestEstimatedOneRepMaxKg = records.bestEstimatedOneRepMaxKg,
            )
        }
    }

    override suspend fun getLatestWorkoutForExport(): WorkoutForExport? {
        val session = trainingDao.getLatestWorkoutSession() ?: return null
        val sets = trainingDao.getWorkoutSets(session.id)
        return if (sets.isEmpty()) null else WorkoutForExport(session = session, sets = sets)
    }

    override suspend fun markWorkoutExported(
        sessionId: String,
        recordId: String,
        exportedAtEpochMillis: Long,
    ) {
        val session = trainingDao.getWorkoutSession(sessionId) ?: return
        trainingDao.upsertWorkoutSession(
            session.copy(
                healthConnectRecordId = recordId,
                healthConnectLastExportedAtEpochMillis = exportedAtEpochMillis,
            ),
        )
    }

    private suspend fun currentOrNewSession(now: Long): WorkoutSessionEntity {
        val existingSession = activeSessionId?.let { trainingDao.getWorkoutSession(it) }
        if (existingSession != null) {
            return existingSession
        }

        val session = WorkoutSessionEntity(
            id = UUID.randomUUID().toString(),
            routineId = null,
            startedAtEpochMillis = now,
            endedAtEpochMillis = now,
            notes = null,
            healthConnectRecordId = null,
            healthConnectLastExportedAtEpochMillis = null,
        )
        activeSessionId = session.id
        trainingDao.upsertWorkoutSession(session)
        return session
    }

    private companion object {
        const val DEFAULT_EXERCISE_NAME = "Custom exercise"
    }
}

private data class DayRange(
    val startEpochMillis: Long,
    val endEpochMillis: Long,
)

private fun LocalDate.dayRange(zoneId: ZoneId = ZoneId.systemDefault()): DayRange =
    DayRange(
        startEpochMillis = atStartOfDay(zoneId).toInstant().toEpochMilli(),
        endEpochMillis = plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli(),
    )
