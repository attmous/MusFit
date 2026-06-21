package com.musfit.data.repository

import androidx.room.withTransaction
import com.musfit.data.local.MusFitDatabase
import com.musfit.data.local.dao.ActiveWorkoutSummaryRow
import com.musfit.data.local.dao.RoutineSummaryRow
import com.musfit.data.local.dao.TrainingDao
import com.musfit.data.local.entity.ExerciseEntity
import com.musfit.data.local.entity.RoutineEntity
import com.musfit.data.local.entity.RoutineExerciseEntity
import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity
import com.musfit.domain.model.WorkoutSetInput
import com.musfit.domain.training.WorkoutCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Instant
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

data class ExerciseSummary(
    val id: String,
    val name: String,
    val category: String,
    val equipment: String?,
    val targetMuscles: String,
    val isCustom: Boolean,
)

data class RoutineSummary(
    val id: String,
    val name: String,
    val notes: String?,
    val exerciseCount: Int,
    val targetSetCount: Int,
    val isStarter: Boolean,
)

data class ActiveWorkoutSummary(
    val sessionId: String,
    val title: String,
    val startedAtEpochMillis: Long,
    val completedSetCount: Int,
    val totalVolumeKg: Double,
)

interface TrainingRepository {
    suspend fun addCompletedSet(
        exerciseName: String,
        reps: Int,
        weightKg: Double,
    ): LoggedWorkoutSet

    suspend fun setCompletion(setId: String, completed: Boolean)

    fun observeExercises(
        query: String = "",
        muscle: String? = null,
        equipment: String? = null,
    ): Flow<List<ExerciseSummary>> = flowOf(emptyList())

    fun observeRoutineSummaries(): Flow<List<RoutineSummary>> = flowOf(emptyList())

    fun observeActiveWorkoutSummary(): Flow<ActiveWorkoutSummary?> = flowOf(null)

    fun observeDailyTrainingSummary(date: LocalDate): Flow<TrainingSummary>

    suspend fun getLatestWorkoutForExport(): WorkoutForExport?

    suspend fun seedStarterTrainingData() = Unit

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
                setType = SET_TYPE_WORKING,
                reps = reps,
                weightKg = weightKg,
                durationSeconds = null,
                distanceMeters = null,
                rpe = null,
                notes = null,
                completed = true,
            )

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

    override fun observeExercises(
        query: String,
        muscle: String?,
        equipment: String?,
    ): Flow<List<ExerciseSummary>> =
        trainingDao.observeExercisesFiltered(query.trim(), muscle, equipment)
            .map { exercises -> exercises.map { it.toSummary() } }

    override fun observeRoutineSummaries(): Flow<List<RoutineSummary>> =
        trainingDao.observeRoutineSummaries().map { rows -> rows.map { it.toSummary() } }

    override fun observeActiveWorkoutSummary(): Flow<ActiveWorkoutSummary?> =
        trainingDao.observeActiveWorkoutSummary().map { row -> row?.toSummary() }

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
        val session = trainingDao.getLatestCompletedWorkoutSession() ?: return null
        val sets = trainingDao.getWorkoutSets(session.id)
        return if (sets.isEmpty()) null else WorkoutForExport(session = session, sets = sets)
    }

    override suspend fun seedStarterTrainingData() {
        database.withTransaction {
            val now = clock()
            val resolvedExerciseIds =
                TrainingStarterData.exercises.associate { definition ->
                    val existingExercise = trainingDao.getExerciseByName(definition.name)
                    if (existingExercise == null) {
                        trainingDao.upsertExercise(
                            ExerciseEntity(
                                id = definition.id,
                                name = definition.name,
                                category = "strength",
                                equipment = definition.equipment,
                                targetMuscles = definition.targetMuscles,
                                isCustom = false,
                            ),
                        )
                    }
                    definition.id to (existingExercise?.id ?: definition.id)
                }
            trainingDao.upsertRoutines(
                TrainingStarterData.routines.map {
                    RoutineEntity(
                        id = it.id,
                        name = it.name,
                        notes = it.notes,
                        createdAtEpochMillis = now,
                        updatedAtEpochMillis = now,
                        isStarter = true,
                    )
                },
            )
            trainingDao.upsertRoutineExercises(
                TrainingStarterData.routines.flatMap { routine ->
                    routine.exercises.mapIndexed { index, exercise ->
                        RoutineExerciseEntity(
                            id = "${routine.id}-${exercise.exerciseId}",
                            routineId = routine.id,
                            exerciseId = resolvedExerciseIds.getValue(exercise.exerciseId),
                            sortOrder = index,
                            targetSets = exercise.targetSets,
                            targetReps = exercise.targetReps,
                        )
                    }
                },
            )
        }
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
        if (existingSession != null && existingSession.startedAtEpochMillis.isSameDayAs(now)) {
            val completedSession =
                existingSession.copy(
                    status = WORKOUT_STATUS_COMPLETED,
                    endedAtEpochMillis = now,
                )
            trainingDao.upsertWorkoutSession(completedSession)
            return completedSession
        }

        val session = WorkoutSessionEntity(
            id = UUID.randomUUID().toString(),
            routineId = null,
            title = DEFAULT_WORKOUT_TITLE,
            status = WORKOUT_STATUS_COMPLETED,
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
        const val WORKOUT_STATUS_ACTIVE = "active"
        const val WORKOUT_STATUS_COMPLETED = "completed"
        const val WORKOUT_STATUS_DISCARDED = "discarded"
        const val SET_TYPE_WORKING = "working"
        const val SET_TYPE_WARM_UP = "warmup"
        const val DEFAULT_WORKOUT_TITLE = "Blank workout"
    }
}

private data class DayRange(
    val startEpochMillis: Long,
    val endEpochMillis: Long,
)

private fun ExerciseEntity.toSummary(): ExerciseSummary =
    ExerciseSummary(
        id = id,
        name = name,
        category = category,
        equipment = equipment,
        targetMuscles = targetMuscles,
        isCustom = isCustom,
    )

private fun RoutineSummaryRow.toSummary(): RoutineSummary =
    RoutineSummary(
        id = id,
        name = name,
        notes = notes,
        exerciseCount = exerciseCount,
        targetSetCount = targetSetCount,
        isStarter = isStarter,
    )

private fun ActiveWorkoutSummaryRow.toSummary(): ActiveWorkoutSummary =
    ActiveWorkoutSummary(
        sessionId = sessionId,
        title = title ?: "Blank workout",
        startedAtEpochMillis = startedAtEpochMillis,
        completedSetCount = completedSetCount,
        totalVolumeKg = totalVolumeKg,
    )

private fun LocalDate.dayRange(zoneId: ZoneId = ZoneId.systemDefault()): DayRange =
    DayRange(
        startEpochMillis = atStartOfDay(zoneId).toInstant().toEpochMilli(),
        endEpochMillis = plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli(),
    )

private fun Long.isSameDayAs(other: Long, zoneId: ZoneId = ZoneId.systemDefault()): Boolean =
    Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate() ==
        Instant.ofEpochMilli(other).atZone(zoneId).toLocalDate()
