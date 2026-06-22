package com.musfit.data.repository

import androidx.room.withTransaction
import com.musfit.data.local.MusFitDatabase
import com.musfit.data.local.dao.ActiveWorkoutSummaryRow
import com.musfit.data.local.dao.ExerciseProgressSetRow
import com.musfit.data.local.dao.RoutineSummaryRow
import com.musfit.data.local.dao.TrainingDao
import com.musfit.data.local.dao.WorkoutHistorySummaryRow
import com.musfit.data.local.dao.WorkoutSetDetailRow
import com.musfit.data.local.entity.ExerciseEntity
import com.musfit.data.local.entity.RoutineEntity
import com.musfit.data.local.entity.RoutineExerciseEntity
import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity
import com.musfit.domain.model.ExerciseProgress
import com.musfit.domain.model.ExerciseProgressSetInput
import com.musfit.domain.model.WorkoutSetInput
import com.musfit.domain.training.WorkoutCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
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

data class ExerciseInput(
    val name: String,
    val category: String,
    val equipment: String?,
    val targetMuscles: String,
)

data class RoutineSummary(
    val id: String,
    val name: String,
    val notes: String?,
    val exerciseCount: Int,
    val targetSetCount: Int,
    val isStarter: Boolean,
)

data class RoutineExerciseDetail(
    val id: String,
    val exercise: ExerciseSummary,
    val sortOrder: Int,
    val targetSets: Int,
    val targetReps: String?,
)

data class RoutineDetail(
    val id: String,
    val name: String,
    val notes: String?,
    val isStarter: Boolean,
    val exercises: List<RoutineExerciseDetail>,
)

data class RoutineInput(
    val name: String,
    val notes: String?,
    val exercises: List<RoutineExerciseInput>,
)

data class RoutineExerciseInput(
    val exerciseId: String,
    val targetSets: Int,
    val targetReps: String?,
)

data class ActiveWorkoutSummary(
    val sessionId: String,
    val title: String,
    val startedAtEpochMillis: Long,
    val completedSetCount: Int,
    val totalVolumeKg: Double,
)

data class WorkoutSetInputData(
    val setType: String,
    val reps: Int?,
    val weightKg: Double?,
    val rpe: Double?,
    val notes: String?,
    val completed: Boolean,
)

data class LoggedWorkoutSetDetail(
    val id: String,
    val exerciseId: String,
    val setType: String,
    val reps: Int?,
    val weightKg: Double?,
    val rpe: Double?,
    val notes: String?,
    val completed: Boolean,
    val previousLabel: String?,
)

data class WorkoutExerciseBlock(
    val exercise: ExerciseSummary,
    val targetReps: String?,
    val sets: List<LoggedWorkoutSetDetail>,
)

data class ActiveWorkoutDetail(
    val sessionId: String,
    val title: String,
    val startedAtEpochMillis: Long,
    val completedSetCount: Int,
    val totalVolumeKg: Double,
    val exerciseBlocks: List<WorkoutExerciseBlock>,
)

data class WorkoutHistorySummary(
    val sessionId: String,
    val title: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long?,
    val completedSetCount: Int,
    val totalVolumeKg: Double,
)

data class WorkoutHistoryDetail(
    val summary: WorkoutHistorySummary,
    val exerciseBlocks: List<WorkoutExerciseBlock>,
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

    suspend fun createCustomExercise(input: ExerciseInput): String = ""

    fun observeRoutineSummaries(): Flow<List<RoutineSummary>> = flowOf(emptyList())

    fun observeActiveWorkoutSummary(): Flow<ActiveWorkoutSummary?> = flowOf(null)

    fun observeActiveWorkoutDetail(): Flow<ActiveWorkoutDetail?> = flowOf(null)

    fun observeWorkoutHistory(): Flow<List<WorkoutHistorySummary>> = flowOf(emptyList())

    fun observeExerciseProgress(exerciseId: String): Flow<ExerciseProgress?> = flowOf(null)

    suspend fun getWorkoutHistoryDetail(sessionId: String): WorkoutHistoryDetail? = null

    fun observeDailyTrainingSummary(date: LocalDate): Flow<TrainingSummary>

    suspend fun createRoutine(input: RoutineInput): String

    suspend fun updateRoutine(routineId: String, input: RoutineInput)

    suspend fun duplicateRoutine(routineId: String): String

    suspend fun deleteRoutine(routineId: String)

    suspend fun getRoutineDetail(routineId: String): RoutineDetail?

    suspend fun startBlankWorkout(): String

    suspend fun startWorkoutFromRoutine(routineId: String): String

    suspend fun addExerciseToActiveWorkout(sessionId: String, exerciseId: String) = Unit

    suspend fun addSetToExercise(
        sessionId: String,
        exerciseId: String,
        input: WorkoutSetInputData,
    ): String = ""

    suspend fun duplicateLastSet(sessionId: String, exerciseId: String): String? = null

    suspend fun updateWorkoutSet(setId: String, input: WorkoutSetInputData) = Unit

    suspend fun deleteWorkoutSet(setId: String) = Unit

    suspend fun finishWorkout(sessionId: String) = Unit

    suspend fun discardWorkout(sessionId: String) = Unit

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

    override suspend fun createCustomExercise(input: ExerciseInput): String {
        val name = input.name.trim()
        require(name.isNotBlank()) { "Exercise name is required" }
        val existing = trainingDao.getExerciseByNormalizedName(name)
        if (existing != null) {
            return existing.id
        }

        val exercise = ExerciseEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            category = input.category.trim().ifBlank { "strength" },
            equipment = input.equipment?.trim()?.takeIf { it.isNotBlank() },
            targetMuscles = input.targetMuscles.trim(),
            isCustom = true,
        )
        trainingDao.upsertExercise(exercise)
        return exercise.id
    }

    override fun observeRoutineSummaries(): Flow<List<RoutineSummary>> =
        trainingDao.observeRoutineSummaries().map { rows -> rows.map { it.toSummary() } }

    override fun observeActiveWorkoutSummary(): Flow<ActiveWorkoutSummary?> =
        trainingDao.observeActiveWorkoutSummary().map { row -> row?.toSummary() }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeActiveWorkoutDetail(): Flow<ActiveWorkoutDetail?> =
        trainingDao.observeActiveWorkoutSession().flatMapLatest { session ->
            if (session == null) {
                flowOf(null)
            } else {
                trainingDao.observeWorkoutSetDetailRows(session.id).map { rows ->
                    rows.toActiveWorkoutDetail(session, trainingDao)
                }
            }
        }

    override fun observeWorkoutHistory(): Flow<List<WorkoutHistorySummary>> =
        trainingDao.observeWorkoutHistorySummaries().map { rows ->
            rows.map { it.toHistorySummary() }
        }

    override fun observeExerciseProgress(exerciseId: String): Flow<ExerciseProgress?> =
        trainingDao.observeExerciseProgressSetRows(exerciseId).map { rows ->
            rows.toExerciseProgress()
        }

    override suspend fun getWorkoutHistoryDetail(sessionId: String): WorkoutHistoryDetail? {
        val session = trainingDao.getCompletedWorkoutSession(sessionId) ?: return null
        val summary = trainingDao.observeWorkoutHistorySummaries().first()
            .firstOrNull { it.sessionId == sessionId }
            ?.toHistorySummary()
            ?: return null
        val detail = trainingDao.observeWorkoutSetDetailRows(session.id).first()
            .toActiveWorkoutDetail(session, trainingDao)
        return WorkoutHistoryDetail(
            summary = summary,
            exerciseBlocks = detail.exerciseBlocks,
        )
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

    override suspend fun createRoutine(input: RoutineInput): String {
        require(input.name.isNotBlank()) { "Routine name is required" }
        val routineId = UUID.randomUUID().toString()
        saveRoutine(routineId, input, isStarter = false, createdAt = clock())
        return routineId
    }

    override suspend fun updateRoutine(routineId: String, input: RoutineInput) {
        val existing = trainingDao.getRoutine(routineId) ?: return
        saveRoutine(
            routineId = routineId,
            input = input,
            isStarter = existing.isStarter,
            createdAt = existing.createdAtEpochMillis,
        )
    }

    override suspend fun duplicateRoutine(routineId: String): String {
        val detail = getRoutineDetail(routineId) ?: return createRoutine(
            RoutineInput(name = "Routine Copy", notes = null, exercises = emptyList()),
        )
        return createRoutine(
            RoutineInput(
                name = "${detail.name} Copy",
                notes = detail.notes,
                exercises = detail.exercises.map {
                    RoutineExerciseInput(
                        exerciseId = it.exercise.id,
                        targetSets = it.targetSets,
                        targetReps = it.targetReps,
                    )
                },
            ),
        )
    }

    override suspend fun deleteRoutine(routineId: String) {
        trainingDao.deleteRoutineById(routineId)
    }

    override suspend fun getRoutineDetail(routineId: String): RoutineDetail? {
        val routine = trainingDao.getRoutine(routineId) ?: return null
        val exercises = trainingDao.getRoutineExerciseDetailRows(routineId).map { row ->
            RoutineExerciseDetail(
                id = row.id,
                exercise = ExerciseSummary(
                    id = row.exerciseId,
                    name = row.exerciseName,
                    category = row.category,
                    equipment = row.equipment,
                    targetMuscles = row.targetMuscles,
                    isCustom = row.isCustom,
                ),
                sortOrder = row.sortOrder,
                targetSets = row.targetSets,
                targetReps = row.targetReps,
            )
        }
        return RoutineDetail(
            id = routine.id,
            name = routine.name,
            notes = routine.notes,
            isStarter = routine.isStarter,
            exercises = exercises,
        )
    }

    override suspend fun startBlankWorkout(): String =
        database.withTransaction {
            createWorkoutSession(routineId = null, title = DEFAULT_WORKOUT_TITLE)
        }

    override suspend fun startWorkoutFromRoutine(routineId: String): String =
        database.withTransaction {
            val existingActiveSession = trainingDao.getLatestActiveWorkoutSession()
            if (existingActiveSession != null) {
                activeSessionId = existingActiveSession.id
                return@withTransaction existingActiveSession.id
            }
            val routine = trainingDao.getRoutine(routineId)
            if (routine == null) {
                return@withTransaction createWorkoutSession(
                    routineId = null,
                    title = DEFAULT_WORKOUT_TITLE,
                )
            }
            val sessionId = createWorkoutSession(routineId = routine.id, title = routine.name)
            val routineExercises = trainingDao.getRoutineExercises(routine.id)
            val sets = routineExercises.flatMap { routineExercise ->
                (0 until routineExercise.targetSets.coerceAtLeast(1)).map { setIndex ->
                    val targetReps = routineExercise.targetReps?.trim()?.takeIf(String::isNotEmpty)
                    WorkoutSetEntity(
                        id = UUID.randomUUID().toString(),
                        sessionId = sessionId,
                        exerciseId = routineExercise.exerciseId,
                        sortOrder = routineExercise.sortOrder * 100 + setIndex,
                        setType = SET_TYPE_WORKING,
                        reps = targetReps?.toIntOrNull(),
                        weightKg = null,
                        durationSeconds = null,
                        distanceMeters = null,
                        rpe = null,
                        notes = encodeWorkoutSetNotes(targetReps = targetReps, userNote = null),
                        completed = false,
                    )
                }
            }
            sets.forEach { trainingDao.upsertWorkoutSet(it) }
            sessionId
        }

    override suspend fun addExerciseToActiveWorkout(sessionId: String, exerciseId: String) {
        val session = trainingDao.getWorkoutSession(sessionId) ?: return
        if (session.status != WORKOUT_STATUS_ACTIVE) return
        addSetToExercise(
            sessionId = sessionId,
            exerciseId = exerciseId,
            input = WorkoutSetInputData(
                setType = SET_TYPE_WORKING,
                reps = null,
                weightKg = null,
                rpe = null,
                notes = null,
                completed = false,
            ),
        )
    }

    override suspend fun addSetToExercise(
        sessionId: String,
        exerciseId: String,
        input: WorkoutSetInputData,
    ): String {
        val session = trainingDao.getWorkoutSession(sessionId) ?: return ""
        if (session.status != WORKOUT_STATUS_ACTIVE) return ""
        val nextSortOrder = (trainingDao.getMaxWorkoutSetSortOrder(sessionId) ?: -1) + 1
        val set = WorkoutSetEntity(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            exerciseId = exerciseId,
            sortOrder = nextSortOrder,
            setType = input.setType,
            reps = input.reps,
            weightKg = input.weightKg,
            durationSeconds = null,
            distanceMeters = null,
            rpe = input.rpe,
            notes = input.notes?.trim()?.takeIf { it.isNotBlank() },
            completed = input.completed,
        )
        trainingDao.upsertWorkoutSet(set)
        return set.id
    }

    override suspend fun duplicateLastSet(sessionId: String, exerciseId: String): String? {
        val session = trainingDao.getWorkoutSession(sessionId) ?: return null
        if (session.status != WORKOUT_STATUS_ACTIVE) return null
        val last = trainingDao.getLastWorkoutSetForExercise(sessionId, exerciseId) ?: return null
        return addSetToExercise(
            sessionId = sessionId,
            exerciseId = exerciseId,
            input = WorkoutSetInputData(
                setType = last.setType,
                reps = last.reps,
                weightKg = last.weightKg,
                rpe = last.rpe,
                notes = last.notes,
                completed = false,
            ),
        )
    }

    override suspend fun updateWorkoutSet(setId: String, input: WorkoutSetInputData) {
        val existing = trainingDao.getWorkoutSet(setId) ?: return
        val session = trainingDao.getWorkoutSession(existing.sessionId) ?: return
        if (session.status != WORKOUT_STATUS_ACTIVE) return
        if (input.completed && ((input.reps ?: 0) <= 0 || (input.weightKg ?: 0.0) <= 0.0)) return
        val existingNotes = parseWorkoutSetNotes(existing.notes)
        trainingDao.upsertWorkoutSet(
            existing.copy(
                setType = input.setType,
                reps = input.reps,
                weightKg = input.weightKg,
                rpe = input.rpe,
                notes = encodeWorkoutSetNotes(
                    targetReps = existingNotes.targetReps,
                    userNote = input.notes?.trim()?.takeIf { it.isNotBlank() },
                ),
                completed = input.completed,
            ),
        )
    }

    override suspend fun deleteWorkoutSet(setId: String) {
        val existing = trainingDao.getWorkoutSet(setId) ?: return
        val session = trainingDao.getWorkoutSession(existing.sessionId) ?: return
        if (session.status != WORKOUT_STATUS_ACTIVE) return
        trainingDao.deleteWorkoutSetById(setId)
    }

    override suspend fun finishWorkout(sessionId: String) {
        val session = trainingDao.getWorkoutSession(sessionId) ?: return
        if (session.status != WORKOUT_STATUS_ACTIVE) return
        trainingDao.updateWorkoutSessionStatus(sessionId, WORKOUT_STATUS_COMPLETED, clock())
        if (activeSessionId == sessionId) {
            activeSessionId = null
        }
    }

    override suspend fun discardWorkout(sessionId: String) {
        val session = trainingDao.getWorkoutSession(sessionId) ?: return
        if (session.status != WORKOUT_STATUS_ACTIVE) return
        trainingDao.updateWorkoutSessionStatus(sessionId, WORKOUT_STATUS_DISCARDED, clock())
        if (activeSessionId == sessionId) {
            activeSessionId = null
        }
    }

    override suspend fun getLatestWorkoutForExport(): WorkoutForExport? {
        val session = trainingDao.getLatestCompletedWorkoutSession() ?: return null
        val sets = trainingDao.getCompletedWorkoutSets(session.id)
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
            TrainingStarterData.routines.forEach { definition ->
                val existingRoutine = trainingDao.getRoutine(definition.id)
                val routine =
                    if (existingRoutine == null) {
                        RoutineEntity(
                            id = definition.id,
                            name = definition.name,
                            notes = definition.notes,
                            createdAtEpochMillis = now,
                            updatedAtEpochMillis = now,
                            isStarter = true,
                        )
                    } else {
                        existingRoutine.copy(
                            name = definition.name,
                            notes = definition.notes,
                            updatedAtEpochMillis = now,
                            isStarter = true,
                        )
                    }
                upsertWorkoutRoutine(routine)
            }
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
        upsertWorkoutSession(
            session.copy(
                healthConnectRecordId = recordId,
                healthConnectLastExportedAtEpochMillis = exportedAtEpochMillis,
            ),
        )
    }

    private suspend fun currentOrNewSession(now: Long): WorkoutSessionEntity {
        val cachedSession = activeSessionId?.let { trainingDao.getWorkoutSession(it) }
        if (cachedSession?.status == WORKOUT_STATUS_ACTIVE && cachedSession.startedAtEpochMillis.isSameDayAs(now)) {
            return cachedSession
        }
        if (cachedSession == null || cachedSession.status != WORKOUT_STATUS_ACTIVE) {
            activeSessionId = null
        }
        val activeSession = trainingDao.getLatestActiveWorkoutSession()
        if (activeSession != null && activeSession.startedAtEpochMillis.isSameDayAs(now)) {
            activeSessionId = activeSession.id
            return activeSession
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
        upsertWorkoutSession(session)
        return session
    }

    private suspend fun upsertWorkoutRoutine(routine: RoutineEntity) {
        val inserted = trainingDao.insertRoutine(routine)
        if (inserted == -1L) {
            trainingDao.updateRoutine(routine)
        }
    }

    private suspend fun saveRoutine(
        routineId: String,
        input: RoutineInput,
        isStarter: Boolean,
        createdAt: Long,
    ) {
        require(input.name.isNotBlank()) { "Routine name is required" }
        database.withTransaction {
            val now = clock()
            upsertWorkoutRoutine(
                RoutineEntity(
                    id = routineId,
                    name = input.name.trim(),
                    notes = input.notes?.trim()?.takeIf { it.isNotBlank() },
                    createdAtEpochMillis = createdAt,
                    updatedAtEpochMillis = now,
                    isStarter = isStarter,
                ),
            )
            trainingDao.deleteRoutineExercises(routineId)
            trainingDao.upsertRoutineExercises(
                input.exercises.mapIndexed { index, exercise ->
                    RoutineExerciseEntity(
                        id = "$routineId-${exercise.exerciseId}-$index",
                        routineId = routineId,
                        exerciseId = exercise.exerciseId,
                        sortOrder = index,
                        targetSets = exercise.targetSets.coerceAtLeast(1),
                        targetReps = exercise.targetReps?.trim()?.takeIf { it.isNotBlank() },
                    )
                },
            )
        }
    }

    private suspend fun createWorkoutSession(routineId: String?, title: String): String {
        val existingActiveSession = trainingDao.getLatestActiveWorkoutSession()
        if (existingActiveSession != null) {
            activeSessionId = existingActiveSession.id
            return existingActiveSession.id
        }
        val now = clock()
        val session = WorkoutSessionEntity(
            id = UUID.randomUUID().toString(),
            routineId = routineId,
            title = title,
            status = WORKOUT_STATUS_ACTIVE,
            startedAtEpochMillis = now,
            endedAtEpochMillis = null,
            notes = null,
            healthConnectRecordId = null,
            healthConnectLastExportedAtEpochMillis = null,
        )
        activeSessionId = session.id
        trainingDao.upsertWorkoutSession(session)
        return session.id
    }

    private suspend fun upsertWorkoutSession(session: WorkoutSessionEntity) {
        val inserted = trainingDao.insertWorkoutSession(session)
        if (inserted == -1L) {
            trainingDao.updateWorkoutSession(session)
        }
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

private fun WorkoutHistorySummaryRow.toHistorySummary(): WorkoutHistorySummary =
    WorkoutHistorySummary(
        sessionId = sessionId,
        title = title ?: "Blank workout",
        startedAtEpochMillis = startedAtEpochMillis,
        endedAtEpochMillis = endedAtEpochMillis,
        completedSetCount = completedSetCount,
        totalVolumeKg = totalVolumeKg,
    )

private fun List<ExerciseProgressSetRow>.toExerciseProgress(): ExerciseProgress? {
    val first = firstOrNull() ?: return null
    val inputs = mapNotNull { row ->
        val reps = row.reps
        val weightKg = row.weightKg
        if (reps == null || weightKg == null) {
            null
        } else {
            ExerciseProgressSetInput(
                dateEpochDay = Instant.ofEpochMilli(row.startedAtEpochMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .toEpochDay(),
                reps = reps,
                weightKg = weightKg,
                completed = row.completed,
            )
        }
    }
    if (inputs.none { it.completed && it.reps > 0 && it.weightKg > 0.0 }) {
        return null
    }
    return WorkoutCalculator.exerciseProgress(
        exerciseId = first.exerciseId,
        exerciseName = first.exerciseName,
        equipment = first.equipment,
        targetMuscles = first.targetMuscles,
        sets = inputs,
    )
}

private data class ParsedWorkoutSetNotes(
    val targetReps: String?,
    val userNote: String?,
)

private suspend fun List<WorkoutSetDetailRow>.toActiveWorkoutDetail(
    session: WorkoutSessionEntity,
    trainingDao: TrainingDao,
): ActiveWorkoutDetail {
    val previousLabels = map { it.exerciseId }
        .distinct()
        .associateWith { exerciseId ->
            trainingDao.getLatestCompletedSetForExerciseBefore(
                exerciseId = exerciseId,
                beforeStartedAtEpochMillis = session.startedAtEpochMillis,
            )?.toPreviousLabel()
        }
    val blocks = groupBy { it.exerciseId }.map { (_, exerciseRows) ->
        val first = exerciseRows.first()
        val parsedNotes = exerciseRows.associate { row -> row.setId to parseWorkoutSetNotes(row.notes) }
        WorkoutExerciseBlock(
            exercise = ExerciseSummary(
                id = first.exerciseId,
                name = first.exerciseName,
                category = first.category,
                equipment = first.equipment,
                targetMuscles = first.targetMuscles,
                isCustom = first.isCustom,
            ),
            targetReps = exerciseRows.firstNotNullOfOrNull { row -> parsedNotes.getValue(row.setId).targetReps },
            sets = exerciseRows.map { row ->
                LoggedWorkoutSetDetail(
                    id = row.setId,
                    exerciseId = row.exerciseId,
                    setType = row.setType,
                    reps = row.reps,
                    weightKg = row.weightKg,
                    rpe = row.rpe,
                    notes = parsedNotes.getValue(row.setId).userNote,
                    completed = row.completed,
                    previousLabel = previousLabels[row.exerciseId],
                )
            },
        )
    }
    val completedRows = filter { it.completed && it.reps != null && it.weightKg != null }
    return ActiveWorkoutDetail(
        sessionId = session.id,
        title = session.title ?: "Blank workout",
        startedAtEpochMillis = session.startedAtEpochMillis,
        completedSetCount = completedRows.size,
        totalVolumeKg = completedRows.sumOf { (it.reps ?: 0) * (it.weightKg ?: 0.0) },
        exerciseBlocks = blocks,
    )
}

private fun WorkoutSetEntity.toPreviousLabel(): String? {
    val reps = reps ?: return null
    val weightKg = weightKg ?: return null
    return "${weightKg.formatCompactKg()} kg x $reps"
}

private fun encodeWorkoutSetNotes(targetReps: String?, userNote: String?): String? {
    val metadata = targetReps?.trim()?.takeIf { it.isNotEmpty() }?.let { "$TARGET_REPS_PREFIX$it" }
    return listOfNotNull(metadata, userNote?.trim()?.takeIf { it.isNotEmpty() })
        .takeIf { it.isNotEmpty() }
        ?.joinToString("\n")
}

private fun parseWorkoutSetNotes(notes: String?): ParsedWorkoutSetNotes {
    if (notes.isNullOrEmpty()) {
        return ParsedWorkoutSetNotes(targetReps = null, userNote = null)
    }
    val lines = notes.lines()
    val firstLine = lines.firstOrNull()
    return if (firstLine != null && firstLine.startsWith(TARGET_REPS_PREFIX)) {
        ParsedWorkoutSetNotes(
            targetReps = firstLine.removePrefix(TARGET_REPS_PREFIX).trim().takeIf { it.isNotEmpty() },
            userNote = lines.drop(1).joinToString("\n").trim().takeIf { it.isNotEmpty() },
        )
    } else {
        ParsedWorkoutSetNotes(targetReps = null, userNote = notes)
    }
}

private fun Double.formatCompactKg(): String =
    if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        toString().trimEnd('0').trimEnd('.')
    }

private fun LocalDate.dayRange(zoneId: ZoneId = ZoneId.systemDefault()): DayRange =
    DayRange(
        startEpochMillis = atStartOfDay(zoneId).toInstant().toEpochMilli(),
        endEpochMillis = plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli(),
    )

private fun Long.isSameDayAs(other: Long, zoneId: ZoneId = ZoneId.systemDefault()): Boolean =
    Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate() ==
        Instant.ofEpochMilli(other).atZone(zoneId).toLocalDate()

private const val TARGET_REPS_PREFIX = "__musfit_target_reps__:"
