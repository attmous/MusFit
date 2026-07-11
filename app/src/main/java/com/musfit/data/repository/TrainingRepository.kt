package com.musfit.data.repository

import androidx.room.withTransaction
import com.musfit.data.local.MusFitDatabase
import com.musfit.data.local.dao.ActiveWorkoutSummaryRow
import com.musfit.data.local.dao.ExerciseProgressSetRow
import com.musfit.data.local.dao.RoutineExerciseDetailRow
import com.musfit.data.local.dao.RoutineSummaryRow
import com.musfit.data.local.dao.TrainingDao
import com.musfit.data.local.dao.WorkoutHistorySummaryRow
import com.musfit.data.local.dao.WorkoutSetDetailRow
import com.musfit.data.local.entity.ExerciseEntity
import com.musfit.data.local.entity.RoutineEntity
import com.musfit.data.local.entity.RoutineExerciseEntity
import com.musfit.data.local.entity.RoutineExerciseSetEntity
import com.musfit.data.local.entity.RoutineFolderEntity
import com.musfit.data.local.entity.TrainingSettingsEntity
import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity
import com.musfit.domain.model.ExerciseProgress
import com.musfit.domain.model.ExerciseProgressSetInput
import com.musfit.domain.model.WorkoutSetInput
import com.musfit.domain.training.PersonalRecordCalculator
import com.musfit.domain.training.PersonalRecordSetInput
import com.musfit.domain.training.PlateCalculator
import com.musfit.domain.training.RoutineDisplayCalculator
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

data class MuscleGroupProgress(
    val muscle: String,
    val completedSetCount: Int,
    val totalVolumeKg: Double,
)

data class WeeklyTrainingVolume(
    val weekStartEpochDay: Long,
    val workoutCount: Int,
    val completedSetCount: Int,
    val totalVolumeKg: Double,
)

data class TrainingProgressAnalytics(
    val muscleGroups: List<MuscleGroupProgress> = emptyList(),
    val weeklyVolume: List<WeeklyTrainingVolume> = emptyList(),
)

/** A dated PR event (best of its day) for the Progress page's "Recent PRs" list. */
data class TrainingPrRecord(
    val exerciseId: String,
    val exerciseName: String,
    val dateEpochDay: Long,
    val reps: Int,
    val weightKg: Double,
    val estimatedOneRepMaxKg: Double,
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
    val primaryMuscles: String = targetMuscles,
    val secondaryMuscles: String = "",
    val imageUrl: String? = null,
    val gifUrl: String? = null,
)

data class ExerciseDetail(
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
    val imageUrl: String? = null,
    val gifUrl: String? = null,
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
    val programName: String? = null,
    val tags: List<String> = emptyList(),
    val folderId: String? = null,
    val folderName: String? = null,
    val muscleGroups: List<String> = emptyList(),
)

data class RoutineFolder(
    val id: String,
    val name: String,
    val sortOrder: Int,
)

data class RoutineSetInput(
    val setType: String = "working",
    val targetReps: String? = null,
    val targetWeightKg: Double? = null,
)

data class RoutineExerciseDetail(
    val id: String,
    val exercise: ExerciseSummary,
    val sortOrder: Int,
    val targetSets: Int,
    val targetReps: String?,
    val restSeconds: Int? = null,
    val setPlans: List<RoutineSetInput> = emptyList(),
)

data class RoutineDetail(
    val id: String,
    val name: String,
    val notes: String?,
    val isStarter: Boolean,
    val exercises: List<RoutineExerciseDetail>,
    val programName: String? = null,
    val tags: List<String> = emptyList(),
    val folderId: String? = null,
    val folderName: String? = null,
)

data class RoutineInput(
    val name: String,
    val notes: String?,
    val exercises: List<RoutineExerciseInput>,
    val programName: String? = null,
    val tags: List<String> = emptyList(),
    val folderId: String? = null,
    val folderName: String? = null,
)

data class RoutineExerciseInput(
    val exerciseId: String,
    val targetSets: Int,
    val targetReps: String?,
    val restSeconds: Int? = null,
    val setPlans: List<RoutineSetInput> = emptyList(),
)

data class ActiveWorkoutSummary(
    val sessionId: String,
    val title: String,
    val startedAtEpochMillis: Long,
    val completedSetCount: Int,
    val totalVolumeKg: Double,
)

data class TrainingSettings(
    val defaultRestSeconds: Int = 120,
    val barWeightKg: Double = 20.0,
    val availablePlatesKg: List<Double> = PlateCalculator.DEFAULT_PLATES,
)

data class TrainingSettingsInput(
    val defaultRestSeconds: Int,
    val barWeightKg: Double,
    val availablePlatesKg: List<Double>,
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
    val targetReps: String? = null,
    val reps: Int?,
    val weightKg: Double?,
    val rpe: Double?,
    val notes: String?,
    val completed: Boolean,
    val previousLabel: String?,
    val supersetGroupId: String? = null,
    val restSeconds: Int? = null,
)

data class WorkoutExerciseBlock(
    val exercise: ExerciseSummary,
    val targetReps: String?,
    val sets: List<LoggedWorkoutSetDetail>,
    val priorBestEstimatedOneRepMaxKg: Double = 0.0,
    val supersetGroupId: String? = null,
    val supersetLabel: String? = null,
)

/** Two or more exercise blocks the user is supersetting (alternating sets between them). */
data class SupersetGroup(
    val supersetGroupId: String,
    val exerciseBlocks: List<WorkoutExerciseBlock>,
)

/** How an exercise appears in the active-workout logger: standalone, or part of a superset. */
sealed interface ExerciseGrouping {
    data class Single(val block: WorkoutExerciseBlock) : ExerciseGrouping
    data class Superset(val group: SupersetGroup) : ExerciseGrouping
}

data class ActiveWorkoutDetail(
    val sessionId: String,
    val title: String,
    val startedAtEpochMillis: Long,
    val completedSetCount: Int,
    val totalVolumeKg: Double,
    val exerciseBlocks: List<WorkoutExerciseBlock>,
    val notes: String? = null,
    val exerciseGroupings: List<ExerciseGrouping> = emptyList(),
)

data class WorkoutHistorySummary(
    val sessionId: String,
    val title: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long?,
    val completedSetCount: Int,
    val totalVolumeKg: Double,
)

data class WorkoutRecapSummary(
    val durationSeconds: Int = 0,
    val exerciseCount: Int = 0,
    val completedSetCount: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val personalRecordCount: Int = 0,
    val notes: String? = null,
)

data class WorkoutHistoryDetail(
    val summary: WorkoutHistorySummary,
    val exerciseBlocks: List<WorkoutExerciseBlock>,
    val exerciseGroupings: List<ExerciseGrouping> = emptyList(),
    val recap: WorkoutRecapSummary = WorkoutRecapSummary(),
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

    suspend fun getExerciseDetail(exerciseId: String): ExerciseDetail? = null

    suspend fun updateExerciseLocalNotes(exerciseId: String, notes: String?) = Unit

    fun observeRoutineSummaries(): Flow<List<RoutineSummary>> = flowOf(emptyList())

    fun observeRoutineFolders(): Flow<List<RoutineFolder>> = flowOf(emptyList())

    fun observeActiveWorkoutSummary(): Flow<ActiveWorkoutSummary?> = flowOf(null)

    fun observeActiveWorkoutDetail(): Flow<ActiveWorkoutDetail?> = flowOf(null)

    fun observeTrainingSettings(): Flow<TrainingSettings> = flowOf(TrainingSettings())

    fun observeWorkoutHistory(): Flow<List<WorkoutHistorySummary>> = flowOf(emptyList())

    fun observeExerciseProgress(exerciseId: String): Flow<ExerciseProgress?> = flowOf(null)

    fun observeTrainingProgressAnalytics(): Flow<TrainingProgressAnalytics> = flowOf(TrainingProgressAnalytics())

    fun observeRecentPersonalRecords(): Flow<List<TrainingPrRecord>> = flowOf(emptyList())

    /** Ids of exercises with at least one completed logged set — backs "only exercises I've done". */
    fun observeLoggedExerciseIds(): Flow<Set<String>> = flowOf(emptySet())

    suspend fun getWorkoutHistoryDetail(sessionId: String): WorkoutHistoryDetail? = null

    fun observeDailyTrainingSummary(date: LocalDate): Flow<TrainingSummary>

    suspend fun createRoutine(input: RoutineInput): String

    suspend fun updateRoutine(routineId: String, input: RoutineInput)

    suspend fun duplicateRoutine(routineId: String): String

    suspend fun deleteRoutine(routineId: String)

    suspend fun getRoutineDetail(routineId: String): RoutineDetail?

    suspend fun createRoutineFolder(name: String): String = ""

    suspend fun updateRoutineFolder(folderId: String, name: String) = Unit

    suspend fun deleteRoutineFolder(folderId: String) = Unit

    suspend fun assignRoutineToFolder(routineId: String, folderId: String?) = Unit

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

    suspend fun moveExerciseInActiveWorkout(sessionId: String, exerciseId: String, direction: Int) = Unit

    suspend fun replaceExerciseInActiveWorkout(sessionId: String, fromExerciseId: String, toExerciseId: String) = Unit

    suspend fun updateActiveWorkoutNotes(sessionId: String, notes: String?) = Unit

    suspend fun moveWorkoutSet(setId: String, direction: Int) = Unit

    suspend fun updateTrainingSettings(input: TrainingSettingsInput) = Unit

    /** Pair two exercises in the active session into a superset; returns the new group id, or null if invalid. */
    suspend fun createSuperset(sessionId: String, exerciseAId: String, exerciseBId: String): String? = null

    /** Ungroup a superset back into standalone exercises (sortOrder is left intact). */
    suspend fun dissolveSuperset(sessionId: String, groupId: String) = Unit

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
    private val exerciseDataset: ExerciseDatasetProvider,
) : TrainingRepository {
    private var clock: () -> Long = { System.currentTimeMillis() }
    private var activeSessionId: String? = null

    internal constructor(
        database: MusFitDatabase,
        trainingDao: TrainingDao,
        clock: () -> Long,
        exerciseDataset: ExerciseDatasetProvider = ExerciseDatasetProvider { emptyList() },
    ) : this(database, trainingDao, exerciseDataset) {
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
            primaryMuscles = input.targetMuscles.trim(),
            secondaryMuscles = "",
            instructions = null,
            localNotes = null,
        )
        trainingDao.upsertExercise(exercise)
        return exercise.id
    }

    override suspend fun getExerciseDetail(exerciseId: String): ExerciseDetail? =
        trainingDao.getExercise(exerciseId)?.toDetail()

    override suspend fun updateExerciseLocalNotes(exerciseId: String, notes: String?) {
        trainingDao.updateExerciseLocalNotes(exerciseId, notes?.trim()?.takeIf { it.isNotBlank() })
    }

    override fun observeRoutineSummaries(): Flow<List<RoutineSummary>> =
        trainingDao.observeRoutineSummaries().map { rows -> rows.map { it.toSummary() } }

    override fun observeRoutineFolders(): Flow<List<RoutineFolder>> =
        trainingDao.observeRoutineFolders().map { rows -> rows.map { it.toFolder() } }

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

    override fun observeTrainingSettings(): Flow<TrainingSettings> =
        trainingDao.observeTrainingSettings().map { entity -> entity?.toSettings() ?: TrainingSettings() }

    override fun observeWorkoutHistory(): Flow<List<WorkoutHistorySummary>> =
        trainingDao.observeWorkoutHistorySummaries().map { rows ->
            rows.map { it.toHistorySummary() }
        }

    override fun observeExerciseProgress(exerciseId: String): Flow<ExerciseProgress?> =
        trainingDao.observeExerciseProgressSetRows(exerciseId).map { rows ->
            rows.toExerciseProgress()
        }

    override fun observeTrainingProgressAnalytics(): Flow<TrainingProgressAnalytics> =
        trainingDao.observeCompletedExerciseProgressSetRows().map { rows ->
            rows.toTrainingProgressAnalytics()
        }

    override fun observeRecentPersonalRecords(): Flow<List<TrainingPrRecord>> =
        trainingDao.observeCompletedExerciseProgressSetRows().map { rows ->
            PersonalRecordCalculator.recentPersonalRecords(
                rows.mapNotNull { row ->
                    val reps = row.reps
                    val weightKg = row.weightKg
                    if (!row.completed || reps == null || weightKg == null || reps <= 0 || weightKg <= 0.0) {
                        null
                    } else {
                        PersonalRecordSetInput(
                            exerciseId = row.exerciseId,
                            exerciseName = row.exerciseName,
                            dateEpochDay = row.startedAtEpochMillis.trainingDate().toEpochDay(),
                            reps = reps,
                            weightKg = weightKg,
                        )
                    }
                },
            ).map { event ->
                TrainingPrRecord(
                    exerciseId = event.exerciseId,
                    exerciseName = event.exerciseName,
                    dateEpochDay = event.dateEpochDay,
                    reps = event.reps,
                    weightKg = event.weightKg,
                    estimatedOneRepMaxKg = event.estimatedOneRepMaxKg,
                )
            }
        }

    override fun observeLoggedExerciseIds(): Flow<Set<String>> =
        trainingDao.observeCompletedExerciseProgressSetRows().map { rows ->
            rows.filter { it.completed }.map { it.exerciseId }.toSet()
        }

    override suspend fun getWorkoutHistoryDetail(sessionId: String): WorkoutHistoryDetail? {
        val session = trainingDao.getCompletedWorkoutSession(sessionId) ?: return null
        val summary = trainingDao.getWorkoutHistorySummary(sessionId)
            ?.toHistorySummary()
            ?: return null
        val detail = trainingDao.observeWorkoutSetDetailRows(session.id).first()
            .toActiveWorkoutDetail(session, trainingDao)
        return WorkoutHistoryDetail(
            summary = summary,
            exerciseBlocks = detail.exerciseBlocks,
            exerciseGroupings = detail.exerciseGroupings,
            recap = detail.toWorkoutRecapSummary(summary),
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
        val nextInput = input.copy(
            programName = input.programName ?: existing.programName,
            tags = input.tags.ifEmpty { existing.tags.parseTags() },
            folderId = input.folderId ?: existing.folderId,
        )
        saveRoutine(
            routineId = routineId,
            input = nextInput,
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
                        restSeconds = it.restSeconds,
                        setPlans = it.setPlans,
                    )
                },
                programName = detail.programName,
                tags = detail.tags,
                folderId = detail.folderId,
                folderName = detail.folderName,
            ),
        )
    }

    override suspend fun deleteRoutine(routineId: String) {
        trainingDao.deleteRoutineById(routineId)
    }

    override suspend fun getRoutineDetail(routineId: String): RoutineDetail? {
        val routine = trainingDao.getRoutine(routineId) ?: return null
        val folder = routine.folderId?.let { trainingDao.getRoutineFolder(it) }
        val setPlansByExercise = trainingDao.getRoutineExerciseSetDetailRows(routineId)
            .groupBy { it.routineExerciseId }
        val exercises = trainingDao.getRoutineExerciseDetailRows(routineId).map { row ->
            val setPlans = setPlansByExercise[row.id]
                .orEmpty()
                .map { setRow ->
                    RoutineSetInput(
                        setType = setRow.setType,
                        targetReps = setRow.targetReps,
                        targetWeightKg = setRow.targetWeightKg,
                    )
                }
                .ifEmpty { row.defaultSetPlans() }
            RoutineExerciseDetail(
                id = row.id,
                exercise = ExerciseSummary(
                    id = row.exerciseId,
                    name = row.exerciseName,
                    category = row.category,
                    equipment = row.equipment,
                    targetMuscles = row.targetMuscles,
                    isCustom = row.isCustom,
                    imageUrl = row.imageUrl.toAvailableExerciseMediaUrl(),
                    gifUrl = row.gifUrl.toAvailableExerciseMediaUrl(),
                ),
                sortOrder = row.sortOrder,
                targetSets = row.targetSets,
                targetReps = row.targetReps,
                restSeconds = row.restSeconds,
                setPlans = setPlans,
            )
        }
        return RoutineDetail(
            id = routine.id,
            name = routine.name,
            notes = routine.notes,
            isStarter = routine.isStarter,
            programName = routine.programName,
            tags = routine.tags.parseTags(),
            folderId = routine.folderId,
            folderName = folder?.name,
            exercises = exercises,
        )
    }

    override suspend fun createRoutineFolder(name: String): String {
        val normalizedName = name.trim()
        require(normalizedName.isNotBlank()) { "Folder name is required" }
        trainingDao.getRoutineFolderByName(normalizedName)?.let { return it.id }
        val now = clock()
        val folder = RoutineFolderEntity(
            id = UUID.randomUUID().toString(),
            name = normalizedName,
            sortOrder = trainingDao.getMaxRoutineFolderSortOrder() + 1,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        trainingDao.upsertRoutineFolder(folder)
        return folder.id
    }

    override suspend fun updateRoutineFolder(folderId: String, name: String) {
        val folder = trainingDao.getRoutineFolder(folderId) ?: return
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) return
        trainingDao.updateRoutineFolder(
            folder.copy(
                name = normalizedName,
                updatedAtEpochMillis = clock(),
            ),
        )
    }

    override suspend fun deleteRoutineFolder(folderId: String) {
        database.withTransaction {
            trainingDao.clearRoutineFolder(folderId)
            trainingDao.deleteRoutineFolderById(folderId)
        }
    }

    override suspend fun assignRoutineToFolder(routineId: String, folderId: String?) {
        val routine = trainingDao.getRoutine(routineId) ?: return
        val resolvedFolderId = folderId
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { id -> trainingDao.getRoutineFolder(id)?.id ?: return }
        if (routine.folderId == resolvedFolderId) return
        trainingDao.updateRoutineFolderAssignment(
            routineId = routineId,
            folderId = resolvedFolderId,
            updatedAtEpochMillis = clock(),
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
            val setPlansByExercise = trainingDao.getRoutineExerciseSetDetailRows(routine.id)
                .groupBy { it.routineExerciseId }
            val sets = routineExercises.flatMap { routineExercise ->
                val setPlans = setPlansByExercise[routineExercise.id]
                    .orEmpty()
                    .ifEmpty {
                        (0 until routineExercise.targetSets.coerceAtLeast(1)).map { setIndex ->
                            com.musfit.data.local.dao.RoutineExerciseSetDetailRow(
                                id = "${routineExercise.id}-set-$setIndex",
                                routineExerciseId = routineExercise.id,
                                sortOrder = setIndex,
                                setType = SET_TYPE_WORKING,
                                targetReps = routineExercise.targetReps,
                                targetWeightKg = null,
                            )
                        }
                    }
                setPlans.mapIndexed { setIndex, setPlan ->
                    val targetReps = setPlan.targetReps?.trim()?.takeIf(String::isNotEmpty)
                    WorkoutSetEntity(
                        id = UUID.randomUUID().toString(),
                        sessionId = sessionId,
                        exerciseId = routineExercise.exerciseId,
                        sortOrder = routineExercise.sortOrder * 100 + setIndex,
                        setType = setPlan.setType.normalizedSetType(),
                        reps = targetReps?.toIntOrNull(),
                        weightKg = setPlan.targetWeightKg,
                        durationSeconds = null,
                        distanceMeters = null,
                        rpe = null,
                        notes = encodeWorkoutSetNotes(targetReps = targetReps, userNote = null),
                        completed = false,
                        restSeconds = routineExercise.restSeconds,
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
        // A new set inherits the exercise's current superset membership (null for a standalone exercise).
        val lastExerciseSet = trainingDao.getLastWorkoutSetForExercise(sessionId, exerciseId)
        val inheritedGroupId = lastExerciseSet?.supersetGroupId
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
            supersetGroupId = inheritedGroupId,
            restSeconds = lastExerciseSet?.restSeconds,
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
        // Auto-dissolve a superset that no longer has at least two exercises with sets.
        val groupId = existing.supersetGroupId ?: return
        val remaining = trainingDao.getWorkoutSets(existing.sessionId).filter { it.supersetGroupId == groupId }
        if (remaining.map { it.exerciseId }.distinct().size < 2) {
            trainingDao.clearSupersetGroup(existing.sessionId, groupId)
        }
    }

    override suspend fun updateActiveWorkoutNotes(sessionId: String, notes: String?) {
        val session = trainingDao.getWorkoutSession(sessionId) ?: return
        if (session.status != WORKOUT_STATUS_ACTIVE) return
        trainingDao.updateWorkoutSession(
            session.copy(notes = notes?.trim()?.takeIf { it.isNotBlank() }),
        )
    }

    override suspend fun moveWorkoutSet(setId: String, direction: Int) {
        if (direction == 0) return
        val existing = trainingDao.getWorkoutSet(setId) ?: return
        val session = trainingDao.getWorkoutSession(existing.sessionId) ?: return
        if (session.status != WORKOUT_STATUS_ACTIVE) return

        val ordered = trainingDao.getWorkoutSets(existing.sessionId)
        val currentIndex = ordered.indexOfFirst { it.id == setId }
        if (currentIndex < 0) return
        val targetIndex = (currentIndex + direction.coerceIn(-1, 1)).coerceIn(0, ordered.lastIndex)
        if (targetIndex == currentIndex) return

        val reordered = ordered.toMutableList()
        val moved = reordered.removeAt(currentIndex)
        reordered.add(targetIndex, moved)
        reordered.forEachIndexed { index, set ->
            if (set.sortOrder != index) {
                trainingDao.upsertWorkoutSet(set.copy(sortOrder = index))
            }
        }
    }

    override suspend fun moveExerciseInActiveWorkout(sessionId: String, exerciseId: String, direction: Int) {
        if (direction == 0) return
        val session = trainingDao.getWorkoutSession(sessionId) ?: return
        if (session.status != WORKOUT_STATUS_ACTIVE) return

        // Group the session's sets into exercise blocks in their current display order, swap the
        // target block with its neighbour, then re-flatten the sort orders sequentially.
        val ordered = trainingDao.getWorkoutSets(sessionId)
        val blocks = LinkedHashMap<String, MutableList<WorkoutSetEntity>>()
        ordered.forEach { blocks.getOrPut(it.exerciseId) { mutableListOf() }.add(it) }
        val exerciseOrder = blocks.keys.toMutableList()
        val currentIndex = exerciseOrder.indexOf(exerciseId)
        if (currentIndex < 0) return
        val targetIndex = (currentIndex + direction.coerceIn(-1, 1)).coerceIn(0, exerciseOrder.lastIndex)
        if (targetIndex == currentIndex) return
        exerciseOrder.removeAt(currentIndex)
        exerciseOrder.add(targetIndex, exerciseId)

        var sortOrder = 0
        exerciseOrder.forEach { exId ->
            blocks.getValue(exId).forEach { set ->
                if (set.sortOrder != sortOrder) {
                    trainingDao.upsertWorkoutSet(set.copy(sortOrder = sortOrder))
                }
                sortOrder++
            }
        }
    }

    override suspend fun replaceExerciseInActiveWorkout(sessionId: String, fromExerciseId: String, toExerciseId: String) {
        if (fromExerciseId == toExerciseId) return
        val session = trainingDao.getWorkoutSession(sessionId) ?: return
        if (session.status != WORKOUT_STATUS_ACTIVE) return
        trainingDao.getWorkoutSets(sessionId)
            .filter { it.exerciseId == fromExerciseId }
            .forEach { trainingDao.upsertWorkoutSet(it.copy(exerciseId = toExerciseId)) }
    }

    override suspend fun updateTrainingSettings(input: TrainingSettingsInput) {
        val settings = input.normalized()
        trainingDao.upsertTrainingSettings(
            TrainingSettingsEntity(
                defaultRestSeconds = settings.defaultRestSeconds,
                barWeightKg = settings.barWeightKg,
                availablePlatesKg = settings.availablePlatesKg.toPlateCsv(),
            ),
        )
    }

    override suspend fun createSuperset(
        sessionId: String,
        exerciseAId: String,
        exerciseBId: String,
    ): String? =
        database.withTransaction {
            val session = trainingDao.getWorkoutSession(sessionId) ?: return@withTransaction null
            if (session.status != WORKOUT_STATUS_ACTIVE) return@withTransaction null
            if (exerciseAId == exerciseBId) return@withTransaction null
            val sets = trainingDao.getWorkoutSets(sessionId)
            val aSets = sets.filter { it.exerciseId == exerciseAId }
            val bSets = sets.filter { it.exerciseId == exerciseBId }
            // Both exercises must be present in the session and currently ungrouped.
            if (aSets.isEmpty() || bSets.isEmpty()) return@withTransaction null
            if (aSets.any { it.supersetGroupId != null } || bSets.any { it.supersetGroupId != null }) {
                return@withTransaction null
            }
            val groupId = UUID.randomUUID().toString()
            trainingDao.setExerciseSupersetGroup(sessionId, exerciseAId, groupId)
            trainingDao.setExerciseSupersetGroup(sessionId, exerciseBId, groupId)
            reindexSupersetContiguous(sessionId, exerciseAId, exerciseBId)
            groupId
        }

    override suspend fun dissolveSuperset(sessionId: String, groupId: String) {
        val session = trainingDao.getWorkoutSession(sessionId) ?: return
        if (session.status != WORKOUT_STATUS_ACTIVE) return
        trainingDao.clearSupersetGroup(sessionId, groupId)
    }

    /**
     * Re-index this session's [sortOrder]s so the two members' sets are contiguous — the first
     * member (by current order) followed by the second — anchored at the earliest member position,
     * while preserving the relative order of every other set. The only write that touches sortOrder.
     */
    private suspend fun reindexSupersetContiguous(
        sessionId: String,
        exerciseAId: String,
        exerciseBId: String,
    ) {
        val ordered = trainingDao.getWorkoutSets(sessionId)
        val firstA = ordered.indexOfFirst { it.exerciseId == exerciseAId }
        val firstB = ordered.indexOfFirst { it.exerciseId == exerciseBId }
        if (firstA < 0 || firstB < 0) return
        val (firstMember, secondMember) = if (firstA <= firstB) {
            exerciseAId to exerciseBId
        } else {
            exerciseBId to exerciseAId
        }
        val memberBlock = ordered.filter { it.exerciseId == firstMember } +
            ordered.filter { it.exerciseId == secondMember }
        val newOrder = mutableListOf<WorkoutSetEntity>()
        var inserted = false
        ordered.forEach { set ->
            if (set.exerciseId == firstMember || set.exerciseId == secondMember) {
                if (!inserted) {
                    newOrder += memberBlock
                    inserted = true
                }
            } else {
                newOrder += set
            }
        }
        newOrder.forEachIndexed { index, set ->
            if (set.sortOrder != index) {
                trainingDao.upsertWorkoutSet(set.copy(sortOrder = index))
            }
        }
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
        // The bundled catalog (1,324 exercises) is imported once; reads/parsing happen off the
        // transaction. Backfill then attaches dataset media to the built-in starter exercises.
        val needsDatasetImport = trainingDao.getExercise(DATASET_GATE_EXERCISE_ID) == null
        val datasetEntities = if (needsDatasetImport) {
            exerciseDataset.load().map { it.toExerciseEntity() }
        } else {
            emptyList()
        }
        database.withTransaction {
            val now = clock()
            val resolvedFolderIds = TrainingStarterData.routines
                .map { it.programName }
                .distinct()
                .associateWith { programName ->
                    val existingFolder = trainingDao.getRoutineFolderByName(programName)
                    if (existingFolder != null) {
                        existingFolder.id
                    } else {
                        val folder = RoutineFolderEntity(
                            id = programName.toRoutineFolderId(),
                            name = programName,
                            sortOrder = trainingDao.getMaxRoutineFolderSortOrder() + 1,
                            createdAtEpochMillis = now,
                            updatedAtEpochMillis = now,
                        )
                        trainingDao.upsertRoutineFolder(folder)
                        folder.id
                    }
                }
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
                                primaryMuscles = definition.primaryMuscles,
                                secondaryMuscles = definition.secondaryMuscles,
                                instructions = definition.instructions,
                            ),
                        )
                    } else if (!existingExercise.isCustom) {
                        trainingDao.updateExercise(
                            existingExercise.copy(
                                name = definition.name,
                                category = "strength",
                                equipment = definition.equipment,
                                targetMuscles = definition.targetMuscles,
                                isCustom = false,
                                primaryMuscles = definition.primaryMuscles,
                                secondaryMuscles = definition.secondaryMuscles,
                                instructions = definition.instructions,
                            ),
                        )
                    }
                    definition.id to (existingExercise?.id ?: definition.id)
                }
            TrainingStarterData.routines.forEach { definition ->
                val existingRoutine = trainingDao.getRoutine(definition.id)
                val routineExerciseRows =
                    definition.exercises.mapIndexed { index, exercise ->
                        RoutineExerciseEntity(
                            id = "${definition.id}-${exercise.exerciseId}",
                            routineId = definition.id,
                            exerciseId = resolvedExerciseIds.getValue(exercise.exerciseId),
                            sortOrder = index,
                            targetSets = exercise.targetSets,
                            targetReps = exercise.targetReps,
                        )
                    }
                val shouldSeedRoutineExercises: Boolean
                val routine =
                    if (existingRoutine == null) {
                        shouldSeedRoutineExercises = true
                        RoutineEntity(
                            id = definition.id,
                            name = definition.name,
                            notes = definition.notes,
                            createdAtEpochMillis = now,
                            updatedAtEpochMillis = now,
                            isStarter = true,
                            programName = definition.programName,
                            tags = definition.tags.toTagCsv(),
                            folderId = resolvedFolderIds[definition.programName],
                        )
                    } else {
                        shouldSeedRoutineExercises = trainingDao.getRoutineExercises(definition.id).isEmpty()
                        existingRoutine.copy(
                            isStarter = true,
                            programName = existingRoutine.programName?.takeIf(String::isNotBlank) ?: definition.programName,
                            tags = existingRoutine.tags.ifBlank { definition.tags.toTagCsv() },
                            folderId = existingRoutine.folderId ?: resolvedFolderIds[definition.programName],
                        )
                    }
                upsertWorkoutRoutine(routine)
                if (shouldSeedRoutineExercises) {
                    trainingDao.upsertRoutineExercises(routineExerciseRows)
                    trainingDao.upsertRoutineExerciseSets(
                        routineExerciseRows.flatMap { routineExercise ->
                            val targetReps = routineExercise.targetReps
                            (0 until routineExercise.targetSets.coerceAtLeast(1)).map { setIndex ->
                                RoutineExerciseSetEntity(
                                    id = "${routineExercise.id}-set-$setIndex",
                                    routineExerciseId = routineExercise.id,
                                    sortOrder = setIndex,
                                    setType = SET_TYPE_WORKING,
                                    targetReps = targetReps,
                                    targetWeightKg = null,
                                )
                            }
                        },
                    )
                }
            }
            if (datasetEntities.isNotEmpty()) {
                trainingDao.upsertExercises(datasetEntities)
            }
            backfillStarterExerciseMedia()
        }
    }

    /**
     * Attaches dataset thumbnails/animations (and instructions, when missing) to the built-in
     * starter exercises via the curated [STARTER_EXERCISE_DATASET_IDS] map. Idempotent and cheap:
     * skips any starter that already has media, a custom exercise, or an unmapped/absent source.
     */
    private suspend fun backfillStarterExerciseMedia() {
        STARTER_EXERCISE_DATASET_IDS.forEach { (exerciseName, datasetId) ->
            val starter = trainingDao.getExerciseByName(exerciseName) ?: return@forEach
            if (starter.isCustom || starter.imageUrl != null || starter.gifUrl != null) return@forEach
            val source = trainingDao.getExercise("$EXERCISE_DATASET_ID_PREFIX$datasetId") ?: return@forEach
            trainingDao.updateExercise(
                starter.copy(
                    imageUrl = source.imageUrl,
                    gifUrl = source.gifUrl,
                    instructions = starter.instructions ?: source.instructions,
                ),
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

    private suspend fun upsertWorkoutRoutine(routine: RoutineEntity) =
        trainingDao.upsertRoutine(routine)

    private suspend fun saveRoutine(
        routineId: String,
        input: RoutineInput,
        isStarter: Boolean,
        createdAt: Long,
    ) {
        require(input.name.isNotBlank()) { "Routine name is required" }
        database.withTransaction {
            val now = clock()
            val folderId = resolveRoutineFolderId(input, now)
            upsertWorkoutRoutine(
                RoutineEntity(
                    id = routineId,
                    name = input.name.trim(),
                    notes = input.notes?.trim()?.takeIf { it.isNotBlank() },
                    createdAtEpochMillis = createdAt,
                    updatedAtEpochMillis = now,
                    isStarter = isStarter,
                    programName = input.programName?.trim()?.takeIf { it.isNotBlank() },
                    tags = input.tags.toTagCsv(),
                    folderId = folderId,
                ),
            )
            trainingDao.deleteRoutineExercises(routineId)
            val routineExercises = input.exercises.mapIndexed { index, exercise ->
                val setPlans = exercise.normalizedSetPlans()
                RoutineExerciseEntity(
                    id = "$routineId-${exercise.exerciseId}-$index",
                    routineId = routineId,
                    exerciseId = exercise.exerciseId,
                    sortOrder = index,
                    targetSets = setPlans.size.coerceAtLeast(1),
                    targetReps = exercise.targetReps?.trim()?.takeIf { it.isNotBlank() }
                        ?: setPlans.firstNotNullOfOrNull { it.targetReps?.trim()?.takeIf(String::isNotBlank) },
                    restSeconds = exercise.restSeconds?.coerceIn(MIN_REST_SECONDS, MAX_REST_SECONDS),
                )
            }
            trainingDao.upsertRoutineExercises(routineExercises)
            val setRows = routineExercises.zip(input.exercises).flatMap { (routineExercise, exercise) ->
                exercise.normalizedSetPlans().mapIndexed { setIndex, setPlan ->
                    RoutineExerciseSetEntity(
                        id = "${routineExercise.id}-set-$setIndex",
                        routineExerciseId = routineExercise.id,
                        sortOrder = setIndex,
                        setType = setPlan.setType.normalizedSetType(),
                        targetReps = setPlan.targetReps?.trim()?.takeIf { it.isNotBlank() },
                        targetWeightKg = setPlan.targetWeightKg?.takeIf { it.isFinite() && it > 0.0 },
                    )
                }
            }
            trainingDao.upsertRoutineExerciseSets(setRows)
        }
    }

    private suspend fun resolveRoutineFolderId(input: RoutineInput, now: Long): String? {
        val folderName = input.folderName?.trim()?.takeIf { it.isNotBlank() }
        if (folderName != null) {
            trainingDao.getRoutineFolderByName(folderName)?.let { return it.id }
        }
        input.folderId?.trim()?.takeIf { it.isNotBlank() }?.let { folderId ->
            trainingDao.getRoutineFolder(folderId)?.let { return it.id }
        }
        if (folderName == null) return null
        val folder = RoutineFolderEntity(
            id = UUID.randomUUID().toString(),
            name = folderName,
            sortOrder = trainingDao.getMaxRoutineFolderSortOrder() + 1,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        trainingDao.upsertRoutineFolder(folder)
        return folder.id
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

    private suspend fun upsertWorkoutSession(session: WorkoutSessionEntity) =
        trainingDao.upsertWorkoutSession(session)

    private companion object {
        const val DEFAULT_EXERCISE_NAME = "Custom exercise"
        const val WORKOUT_STATUS_ACTIVE = "active"
        const val WORKOUT_STATUS_COMPLETED = "completed"
        const val WORKOUT_STATUS_DISCARDED = "discarded"
        const val SET_TYPE_WORKING = "working"
        const val SET_TYPE_WARM_UP = "warmup"
        const val DEFAULT_WORKOUT_TITLE = "Blank workout"

        /** Presence of the first dataset exercise marks the bundled catalog as already imported. */
        private const val DATASET_GATE_EXERCISE_ID = "${EXERCISE_DATASET_ID_PREFIX}0001"
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
        primaryMuscles = primaryMuscles.ifBlank { targetMuscles },
        secondaryMuscles = secondaryMuscles,
        imageUrl = imageUrl.toAvailableExerciseMediaUrl(),
        gifUrl = gifUrl.toAvailableExerciseMediaUrl(),
    )

private fun ExerciseEntity.toDetail(): ExerciseDetail =
    ExerciseDetail(
        id = id,
        name = name,
        category = category,
        equipment = equipment,
        targetMuscles = targetMuscles,
        primaryMuscles = primaryMuscles.ifBlank { targetMuscles },
        secondaryMuscles = secondaryMuscles,
        instructions = instructions,
        localNotes = localNotes,
        isCustom = isCustom,
        imageUrl = imageUrl.toAvailableExerciseMediaUrl(),
        gifUrl = gifUrl.toAvailableExerciseMediaUrl(),
    )

private fun RoutineSummaryRow.toSummary(): RoutineSummary =
    RoutineSummary(
        id = id,
        name = name,
        notes = notes,
        exerciseCount = exerciseCount,
        targetSetCount = targetSetCount,
        isStarter = isStarter,
        programName = programName,
        tags = tags.parseTags(),
        folderId = folderId,
        folderName = folderName,
        muscleGroups = RoutineDisplayCalculator.topMuscles(primaryMuscles),
    )

private fun RoutineFolderEntity.toFolder(): RoutineFolder =
    RoutineFolder(
        id = id,
        name = name,
        sortOrder = sortOrder,
    )

private fun String?.toAvailableExerciseMediaUrl(): String? =
    this?.let(::exerciseMediaUrl)

private fun RoutineExerciseDetailRow.defaultSetPlans(): List<RoutineSetInput> =
    (0 until targetSets.coerceAtLeast(1)).map {
        RoutineSetInput(
            setType = "working",
            targetReps = targetReps,
            targetWeightKg = null,
        )
    }

private fun RoutineExerciseInput.normalizedSetPlans(): List<RoutineSetInput> {
    val plans = setPlans.ifEmpty {
        (0 until targetSets.coerceAtLeast(1)).map {
            RoutineSetInput(
                setType = "working",
                targetReps = targetReps,
                targetWeightKg = null,
            )
        }
    }
    return plans
        .map { plan ->
            RoutineSetInput(
                setType = plan.setType.normalizedSetType(),
                targetReps = plan.targetReps?.trim()?.takeIf { it.isNotBlank() },
                targetWeightKg = plan.targetWeightKg?.takeIf { it.isFinite() && it > 0.0 },
            )
        }
        .ifEmpty { listOf(RoutineSetInput(setType = "working", targetReps = targetReps)) }
}

private fun String.normalizedSetType(): String =
    when (lowercase().trim()) {
        "warmup", "warm-up", "warm_up" -> "warmup"
        "drop", "drop-set", "drop_set" -> "drop"
        "failure", "fail" -> "failure"
        else -> "working"
    }

private fun String.parseTags(): List<String> =
    split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()

private fun List<String>.toTagCsv(): String =
    map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString(",")

private fun String.toRoutineFolderId(): String =
    "folder-" + trim()
        .lowercase()
        .replace("&", "and")
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')

private fun TrainingSettingsEntity.toSettings(): TrainingSettings =
    TrainingSettings(
        defaultRestSeconds = defaultRestSeconds.coerceIn(MIN_REST_SECONDS, MAX_REST_SECONDS),
        barWeightKg = barWeightKg.takeIf { it > 0.0 } ?: 20.0,
        availablePlatesKg = availablePlatesKg.parsePlateCsv(),
    )

private fun TrainingSettingsInput.normalized(): TrainingSettings =
    TrainingSettings(
        defaultRestSeconds = defaultRestSeconds.coerceIn(MIN_REST_SECONDS, MAX_REST_SECONDS),
        barWeightKg = barWeightKg.takeIf { it > 0.0 } ?: 20.0,
        availablePlatesKg = availablePlatesKg
            .filter { it > 0.0 }
            .distinct()
            .sortedDescending()
            .ifEmpty { PlateCalculator.DEFAULT_PLATES },
    )

private fun String.parsePlateCsv(): List<Double> =
    split(",")
        .mapNotNull { it.trim().toDoubleOrNull() }
        .filter { it > 0.0 }
        .distinct()
        .sortedDescending()
        .ifEmpty { PlateCalculator.DEFAULT_PLATES }

private fun List<Double>.toPlateCsv(): String =
    filter { it > 0.0 }
        .distinct()
        .sortedDescending()
        .joinToString(",") { it.formatPlateCsvValue() }

private fun Double.formatPlateCsvValue(): String =
    if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        toString()
    }

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

private fun List<ExerciseProgressSetRow>.toTrainingProgressAnalytics(): TrainingProgressAnalytics {
    val validRows = filter { row ->
        row.completed && (row.reps ?: 0) > 0 && (row.weightKg ?: 0.0) > 0.0
    }
    val muscleGroups = validRows
        .flatMap { row ->
            row.targetMuscles.parseMuscleList().map { muscle -> muscle to row }
        }
        .groupBy({ it.first }, { it.second })
        .map { (muscle, rows) ->
            MuscleGroupProgress(
                muscle = muscle,
                completedSetCount = rows.size,
                totalVolumeKg = rows.sumOf { it.volumeKg() },
            )
        }
        .sortedWith(compareByDescending<MuscleGroupProgress> { it.totalVolumeKg }.thenBy { it.muscle })

    val weeklyVolume = validRows
        .groupBy { row -> row.startedAtEpochMillis.trainingDate().weekStartEpochDay() }
        .toSortedMap()
        .map { (weekStartEpochDay, rows) ->
            WeeklyTrainingVolume(
                weekStartEpochDay = weekStartEpochDay,
                workoutCount = rows.map { it.sessionId }.distinct().size,
                completedSetCount = rows.size,
                totalVolumeKg = rows.sumOf { it.volumeKg() },
            )
        }

    return TrainingProgressAnalytics(
        muscleGroups = muscleGroups,
        weeklyVolume = weeklyVolume,
    )
}

private fun String.parseMuscleList(): List<String> =
    split(",")
        .map { it.trim().lowercase() }
        .filter { it.isNotBlank() }
        .distinct()

private fun ExerciseProgressSetRow.volumeKg(): Double = (reps ?: 0) * (weightKg ?: 0.0)

private fun Long.trainingDate(): LocalDate =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

private fun LocalDate.weekStartEpochDay(): Long =
    minusDays((dayOfWeek.value - 1).toLong()).toEpochDay()

private data class ParsedWorkoutSetNotes(
    val targetReps: String?,
    val userNote: String?,
)

private suspend fun List<WorkoutSetDetailRow>.toActiveWorkoutDetail(
    session: WorkoutSessionEntity,
    trainingDao: TrainingDao,
): ActiveWorkoutDetail {
    val distinctExerciseIds = map { it.exerciseId }.distinct()
    // Per-set LAST labels: the most recent prior session's completed sets for the
    // exercise, in set order, so row N can show what set N lifted last time.
    val previousLabels = distinctExerciseIds.associateWith { exerciseId ->
        val latestSet = trainingDao.getLatestCompletedSetForExerciseBefore(
            exerciseId = exerciseId,
            beforeStartedAtEpochMillis = session.startedAtEpochMillis,
        ) ?: return@associateWith emptyList()
        trainingDao.getCompletedSetsForExerciseBefore(
            exerciseId = exerciseId,
            beforeStartedAtEpochMillis = session.startedAtEpochMillis,
        )
            .filter { it.sessionId == latestSet.sessionId }
            .sortedBy { it.sortOrder }
            .mapNotNull { it.toPreviousLabel() }
    }
    val priorBest1RM = distinctExerciseIds.associateWith { exerciseId ->
        trainingDao.getCompletedSetsForExerciseBefore(
            exerciseId = exerciseId,
            beforeStartedAtEpochMillis = session.startedAtEpochMillis,
        ).maxOfOrNull { set ->
            val reps = set.reps
            val weightKg = set.weightKg
            if (reps != null && weightKg != null) {
                WorkoutCalculator.estimatedOneRepMax(weightKg = weightKg, reps = reps)
            } else {
                0.0
            }
        } ?: 0.0
    }
    // Derive an A/B label per exercise within its superset (first member = A), in sortOrder order.
    val groupMemberCount = mutableMapOf<String, Int>()
    val exerciseSupersetLabels = mutableMapOf<String, String>()
    distinctExerciseIds.forEach { exerciseId ->
        val groupId = first { it.exerciseId == exerciseId }.supersetGroupId
        if (groupId != null) {
            val index = groupMemberCount.getOrDefault(groupId, 0)
            exerciseSupersetLabels[exerciseId] = ('A' + index).toString()
            groupMemberCount[groupId] = index + 1
        }
    }

    val orderedBlocks = groupBy { it.exerciseId }.map { (_, exerciseRows) ->
        buildWorkoutExerciseBlock(exerciseRows, previousLabels, priorBest1RM, exerciseSupersetLabels)
    }

    // Fold consecutive blocks sharing a non-null supersetGroupId into one Superset grouping.
    val groupings = mutableListOf<ExerciseGrouping>()
    orderedBlocks.forEach { block ->
        val last = groupings.lastOrNull()
        val groupId = block.supersetGroupId
        when {
            groupId != null && last is ExerciseGrouping.Superset && last.group.supersetGroupId == groupId ->
                groupings[groupings.lastIndex] =
                    ExerciseGrouping.Superset(last.group.copy(exerciseBlocks = last.group.exerciseBlocks + block))
            groupId != null ->
                groupings += ExerciseGrouping.Superset(SupersetGroup(groupId, listOf(block)))
            else ->
                groupings += ExerciseGrouping.Single(block)
        }
    }

    val completedRows = filter { it.completed && it.reps != null && it.weightKg != null }
    return ActiveWorkoutDetail(
        sessionId = session.id,
        title = session.title ?: "Blank workout",
        startedAtEpochMillis = session.startedAtEpochMillis,
        completedSetCount = completedRows.size,
        totalVolumeKg = completedRows.sumOf { (it.reps ?: 0) * (it.weightKg ?: 0.0) },
        exerciseBlocks = orderedBlocks,
        notes = session.notes,
        exerciseGroupings = groupings,
    )
}

private fun buildWorkoutExerciseBlock(
    exerciseRows: List<WorkoutSetDetailRow>,
    previousLabels: Map<String, List<String>>,
    priorBest1RM: Map<String, Double>,
    exerciseSupersetLabels: Map<String, String>,
): WorkoutExerciseBlock {
    val first = exerciseRows.first()
    val parsedNotes = exerciseRows.associate { row -> row.setId to parseWorkoutSetNotes(row.notes) }
    val groupId = first.supersetGroupId
    return WorkoutExerciseBlock(
        exercise = ExerciseSummary(
            id = first.exerciseId,
            name = first.exerciseName,
            category = first.category,
            equipment = first.equipment,
            targetMuscles = first.targetMuscles,
            isCustom = first.isCustom,
            imageUrl = first.imageUrl.toAvailableExerciseMediaUrl(),
            gifUrl = first.gifUrl.toAvailableExerciseMediaUrl(),
        ),
        targetReps = exerciseRows.firstNotNullOfOrNull { row -> parsedNotes.getValue(row.setId).targetReps },
        priorBestEstimatedOneRepMaxKg = priorBest1RM[first.exerciseId] ?: 0.0,
        supersetGroupId = groupId,
        supersetLabel = exerciseSupersetLabels[first.exerciseId],
        sets = exerciseRows.mapIndexed { index, row ->
            val exercisePreviousLabels = previousLabels[row.exerciseId].orEmpty()
            LoggedWorkoutSetDetail(
                id = row.setId,
                exerciseId = row.exerciseId,
                setType = row.setType,
                targetReps = parsedNotes.getValue(row.setId).targetReps,
                reps = row.reps,
                weightKg = row.weightKg,
                rpe = row.rpe,
                notes = parsedNotes.getValue(row.setId).userNote,
                completed = row.completed,
                // Positional match against last time's sets; extra sets reuse the
                // final prior set so a new fourth set still shows a reference.
                previousLabel = exercisePreviousLabels.getOrNull(index)
                    ?: exercisePreviousLabels.lastOrNull(),
                supersetGroupId = groupId,
                restSeconds = row.restSeconds,
            )
        },
    )
}

private fun ActiveWorkoutDetail.toWorkoutRecapSummary(summary: WorkoutHistorySummary): WorkoutRecapSummary =
    WorkoutRecapSummary(
        durationSeconds = summary.workoutDurationSeconds(),
        exerciseCount = exerciseBlocks.size,
        completedSetCount = summary.completedSetCount,
        totalVolumeKg = summary.totalVolumeKg,
        personalRecordCount = exerciseBlocks.sumOf { it.personalRecordSetCount() },
        notes = notes,
    )

private fun WorkoutHistorySummary.workoutDurationSeconds(): Int {
    val endedAt = endedAtEpochMillis ?: startedAtEpochMillis
    return ((endedAt - startedAtEpochMillis).coerceAtLeast(0L) / 1000L).toInt()
}

private fun WorkoutExerciseBlock.personalRecordSetCount(): Int =
    sets.count { set ->
        val normalizedSetType = set.setType.lowercase()
        val isWarmup = normalizedSetType == "warmup" || normalizedSetType == "warm-up"
        val isDropSet = normalizedSetType == "drop"
        val reps = set.reps
        val weightKg = set.weightKg
        !isWarmup && !isDropSet && set.completed && reps != null && weightKg != null &&
            WorkoutCalculator.estimatedOneRepMax(weightKg, reps) > priorBestEstimatedOneRepMaxKg + 1e-6
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

private const val MIN_REST_SECONDS = 15
private const val MAX_REST_SECONDS = 900
private const val TARGET_REPS_PREFIX = "__musfit_target_reps__:"
