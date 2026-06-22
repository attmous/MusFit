package com.musfit.ui.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musfit.data.repository.ActiveWorkoutDetail
import com.musfit.data.repository.ActiveWorkoutSummary
import com.musfit.data.repository.ExerciseGrouping
import com.musfit.data.repository.ExerciseInput
import com.musfit.data.repository.ExerciseSummary
import com.musfit.data.repository.LoggedWorkoutSet
import com.musfit.data.repository.LoggedWorkoutSetDetail
import com.musfit.data.repository.WorkoutSetInputData
import com.musfit.data.repository.RoutineExerciseInput
import com.musfit.data.repository.RoutineInput
import com.musfit.data.repository.RoutineSummary
import com.musfit.data.repository.TrainingRepository
import com.musfit.data.repository.WorkoutHistoryDetail
import com.musfit.data.repository.WorkoutHistorySummary
import com.musfit.domain.model.WorkoutSetInput
import com.musfit.domain.model.ExerciseProgress
import com.musfit.domain.training.WorkoutCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TrainingSection {
    Routines,
    Exercises,
    History,
    Progress,
}

data class RoutineEditorState(
    val routineId: String? = null,
    val name: String = "",
    val notes: String = "",
    val exercises: List<RoutineExerciseInput> = emptyList(),
    val isOpen: Boolean = false,
)

data class ExerciseEditorState(
    val isOpen: Boolean = false,
    val name: String = "",
    val category: String = "strength",
    val equipment: String = "",
    val targetMuscles: String = "",
)

data class RestTimerState(
    val isVisible: Boolean = false,
    val sourceSetId: String? = null,
    val durationSeconds: Int = DEFAULT_REST_TIMER_SECONDS,
    val remainingSeconds: Int = 0,
    val isRunning: Boolean = false,
)

data class TrainingUiState(
    val selectedSection: TrainingSection = TrainingSection.Routines,
    val routines: List<RoutineSummary> = emptyList(),
    val exercises: List<ExerciseSummary> = emptyList(),
    val visibleExercises: List<ExerciseSummary> = emptyList(),
    val activeWorkoutSummary: ActiveWorkoutSummary? = null,
    val activeWorkout: ActiveWorkoutDetail? = null,
    val workoutHistory: List<WorkoutHistorySummary> = emptyList(),
    val selectedProgressExerciseId: String? = null,
    val selectedExerciseProgress: ExerciseProgress? = null,
    val selectedWorkoutDetail: WorkoutHistoryDetail? = null,
    val exerciseSearchQuery: String = "",
    val exerciseMuscleFilter: String? = null,
    val exerciseEquipmentFilter: String? = null,
    val exerciseEditor: ExerciseEditorState = ExerciseEditorState(),
    val exerciseName: String = "",
    val reps: String = "",
    val weightKg: String = "",
    val sets: List<LoggedWorkoutSet> = emptyList(),
    val totalVolumeKg: Double = 0.0,
    val bestEstimatedOneRepMaxKg: Double = 0.0,
    val routineEditor: RoutineEditorState = RoutineEditorState(),
    val activeWorkoutRouteOpen: Boolean = false,
    val restTimer: RestTimerState = RestTimerState(),
    val finishConfirmationOpen: Boolean = false,
    val discardConfirmationOpen: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class TrainingViewModel @Inject constructor(
    private val repository: TrainingRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TrainingUiState())
    val state: StateFlow<TrainingUiState> = mutableState.asStateFlow()
    private var progressObservationJob: Job? = null

    init {
        viewModelScope.launch {
            repository.seedStarterTrainingData()
        }
        viewModelScope.launch {
            combine(
                repository.observeRoutineSummaries(),
                repository.observeExercises(),
                repository.observeActiveWorkoutSummary(),
            ) { routines, exercises, activeWorkout ->
                Triple(routines, exercises, activeWorkout)
            }.collect { (routines, exercises, activeWorkout) ->
                mutableState.update {
                    it.copy(
                        routines = routines,
                        exercises = exercises,
                        activeWorkoutSummary = activeWorkout,
                    ).withFilteredExercises()
                }
            }
        }
        viewModelScope.launch {
            repository.observeActiveWorkoutDetail().collect { activeWorkout ->
                mutableState.update { it.copy(activeWorkout = activeWorkout) }
            }
        }
        viewModelScope.launch {
            repository.observeWorkoutHistory().collect { history ->
                mutableState.update { it.copy(workoutHistory = history) }
            }
        }
    }

    fun selectSection(section: TrainingSection) {
        mutableState.update { it.copy(selectedSection = section) }
    }

    fun resumeActiveWorkout() {
        mutableState.update { it.copy(activeWorkoutRouteOpen = true) }
    }

    fun openWorkoutDetail(sessionId: String) {
        viewModelScope.launch {
            mutableState.update {
                it.copy(selectedWorkoutDetail = repository.getWorkoutHistoryDetail(sessionId))
            }
        }
    }

    fun selectProgressExercise(exerciseId: String) {
        mutableState.update {
            it.copy(
                selectedProgressExerciseId = exerciseId,
                selectedExerciseProgress = null,
            )
        }
        progressObservationJob?.cancel()
        progressObservationJob = viewModelScope.launch {
            repository.observeExerciseProgress(exerciseId).collect { progress ->
                mutableState.update { current ->
                    if (current.selectedProgressExerciseId == exerciseId) {
                        current.copy(selectedExerciseProgress = progress)
                    } else {
                        current
                    }
                }
            }
        }
    }

    fun closeWorkoutDetail() {
        mutableState.update { it.copy(selectedWorkoutDetail = null) }
    }

    fun closeActiveWorkoutRoute() {
        mutableState.update {
            it.copy(
                activeWorkoutRouteOpen = false,
                selectedSection = TrainingSection.Routines,
            )
        }
    }

    fun openRoutineEditor(routineId: String?) {
        if (routineId != null && isStarterRoutine(routineId)) {
            mutableState.update { it.copy(message = "Starter routines are read-only templates.") }
            return
        }
        viewModelScope.launch {
            val detail = routineId?.let { repository.getRoutineDetail(it) }
            mutableState.update {
                it.copy(
                    routineEditor = RoutineEditorState(
                        routineId = routineId,
                        name = detail?.name.orEmpty(),
                        notes = detail?.notes.orEmpty(),
                        exercises = detail?.exercises?.map { exercise ->
                            RoutineExerciseInput(
                                exerciseId = exercise.exercise.id,
                                targetSets = exercise.targetSets,
                                targetReps = exercise.targetReps,
                            )
                        }.orEmpty(),
                        isOpen = true,
                    ),
                )
            }
        }
    }

    fun closeRoutineEditor() {
        mutableState.update { it.copy(routineEditor = RoutineEditorState()) }
    }

    fun onRoutineNameChanged(value: String) {
        mutableState.update { it.copy(routineEditor = it.routineEditor.copy(name = value)) }
    }

    fun onRoutineNotesChanged(value: String) {
        mutableState.update { it.copy(routineEditor = it.routineEditor.copy(notes = value)) }
    }

    fun addRoutineExercise(exerciseId: String) {
        val exerciseExists = state.value.exercises.any { it.id == exerciseId }
        if (!exerciseExists) return

        mutableState.update {
            it.copy(
                routineEditor = it.routineEditor.copy(
                    exercises = it.routineEditor.exercises + RoutineExerciseInput(
                        exerciseId = exerciseId,
                        targetSets = 3,
                        targetReps = "8",
                    ),
                ),
            )
        }
    }

    fun removeRoutineExercise(index: Int) {
        mutableState.update { current ->
            if (index !in current.routineEditor.exercises.indices) {
                current
            } else {
                current.copy(
                    routineEditor = current.routineEditor.copy(
                        exercises = current.routineEditor.exercises.filterIndexed { itemIndex, _ ->
                            itemIndex != index
                        },
                    ),
                )
            }
        }
    }

    fun onRoutineExerciseTargetSetsChanged(index: Int, value: String) {
        val parsedSets = value.filter(Char::isDigit).toIntOrNull()?.coerceAtLeast(1) ?: 1
        mutableState.update { current ->
            if (index !in current.routineEditor.exercises.indices) {
                current
            } else {
                current.copy(
                    routineEditor = current.routineEditor.copy(
                        exercises = current.routineEditor.exercises.mapIndexed { itemIndex, exercise ->
                            if (itemIndex == index) {
                                exercise.copy(targetSets = parsedSets)
                            } else {
                                exercise
                            }
                        },
                    ),
                )
            }
        }
    }

    fun onRoutineExerciseTargetRepsChanged(index: Int, value: String) {
        val sanitized = value.trim().takeIf { it.isNotEmpty() }
        mutableState.update { current ->
            if (index !in current.routineEditor.exercises.indices) {
                current
            } else {
                current.copy(
                    routineEditor = current.routineEditor.copy(
                        exercises = current.routineEditor.exercises.mapIndexed { itemIndex, exercise ->
                            if (itemIndex == index) {
                                exercise.copy(targetReps = sanitized)
                            } else {
                                exercise
                            }
                        },
                    ),
                )
            }
        }
    }

    fun moveRoutineExerciseUp(index: Int) {
        moveRoutineExercise(fromIndex = index, toIndex = index - 1)
    }

    fun moveRoutineExerciseDown(index: Int) {
        moveRoutineExercise(fromIndex = index, toIndex = index + 1)
    }

    fun saveRoutineEditor() {
        val editor = state.value.routineEditor
        if (editor.routineId != null && isStarterRoutine(editor.routineId)) {
            mutableState.update { it.copy(message = "Starter routines are read-only templates.") }
            closeRoutineEditor()
            return
        }
        val input = RoutineInput(editor.name, editor.notes, editor.exercises)
        viewModelScope.launch {
            if (editor.routineId == null) {
                repository.createRoutine(input)
            } else {
                repository.updateRoutine(editor.routineId, input)
            }
            closeRoutineEditor()
        }
    }

    fun duplicateRoutine(routineId: String) {
        viewModelScope.launch {
            repository.duplicateRoutine(routineId)
        }
    }

    fun deleteRoutine(routineId: String) {
        if (isStarterRoutine(routineId)) {
            mutableState.update { it.copy(message = "Starter routines are read-only templates.") }
            return
        }
        viewModelScope.launch {
            repository.deleteRoutine(routineId)
            if (state.value.routineEditor.routineId == routineId) {
                closeRoutineEditor()
            }
        }
    }

    fun startBlankWorkout() {
        viewModelScope.launch {
            repository.startBlankWorkout()
            mutableState.update { it.copy(activeWorkoutRouteOpen = true) }
        }
    }

    fun startRoutine(routineId: String) {
        viewModelScope.launch {
            repository.startWorkoutFromRoutine(routineId)
            mutableState.update { it.copy(activeWorkoutRouteOpen = true) }
        }
    }

    fun onExerciseChanged(value: String) {
        mutableState.update { it.copy(exerciseName = value) }
    }

    fun onExerciseSearchQueryChanged(value: String) {
        mutableState.update { it.copy(exerciseSearchQuery = value).withFilteredExercises() }
    }

    fun onExerciseMuscleFilterChanged(value: String?) {
        mutableState.update {
            it.copy(exerciseMuscleFilter = value?.trim()?.takeIf(String::isNotEmpty))
                .withFilteredExercises()
        }
    }

    fun onExerciseEquipmentFilterChanged(value: String?) {
        mutableState.update {
            it.copy(exerciseEquipmentFilter = value?.trim()?.takeIf(String::isNotEmpty))
                .withFilteredExercises()
        }
    }

    fun clearExerciseFilters() {
        mutableState.update {
            it.copy(
                exerciseSearchQuery = "",
                exerciseMuscleFilter = null,
                exerciseEquipmentFilter = null,
            ).withFilteredExercises()
        }
    }

    fun openCustomExerciseEditor() {
        mutableState.update {
            it.copy(
                selectedSection = TrainingSection.Exercises,
                exerciseEditor = ExerciseEditorState(isOpen = true),
            )
        }
    }

    fun closeCustomExerciseEditor() {
        mutableState.update { it.copy(exerciseEditor = ExerciseEditorState()) }
    }

    fun onCustomExerciseNameChanged(value: String) {
        mutableState.update { it.copy(exerciseEditor = it.exerciseEditor.copy(name = value)) }
    }

    fun onCustomExerciseCategoryChanged(value: String) {
        mutableState.update { it.copy(exerciseEditor = it.exerciseEditor.copy(category = value)) }
    }

    fun onCustomExerciseEquipmentChanged(value: String) {
        mutableState.update { it.copy(exerciseEditor = it.exerciseEditor.copy(equipment = value)) }
    }

    fun onCustomExerciseTargetMusclesChanged(value: String) {
        mutableState.update { it.copy(exerciseEditor = it.exerciseEditor.copy(targetMuscles = value)) }
    }

    fun saveCustomExercise() {
        val editor = state.value.exerciseEditor
        val name = editor.name.trim()
        if (name.isBlank()) {
            mutableState.update { it.copy(message = "Exercise name is required.") }
            return
        }
        val input = ExerciseInput(
            name = name,
            category = editor.category.trim().ifBlank { "strength" },
            equipment = editor.equipment.trim().takeIf { it.isNotBlank() },
            targetMuscles = editor.targetMuscles.trim(),
        )
        viewModelScope.launch {
            repository.createCustomExercise(input)
            mutableState.update {
                it.copy(
                    selectedSection = TrainingSection.Exercises,
                    exerciseEditor = ExerciseEditorState(),
                    message = "Exercise saved.",
                ).withFilteredExercises()
            }
        }
    }

    fun onRepsChanged(value: String) {
        mutableState.update { it.copy(reps = value.filter(Char::isDigit)) }
    }

    fun onWeightChanged(value: String) {
        mutableState.update { it.copy(weightKg = value.sanitizeDecimalInput()) }
    }

    fun addSet() {
        val currentState = state.value
        val reps = currentState.reps.toIntOrNull() ?: return
        val weightKg = currentState.weightKg.toDoubleOrNull() ?: return
        if (reps <= 0 || weightKg <= 0.0) return

        viewModelScope.launch {
            val savedSet = repository.addCompletedSet(
                exerciseName = currentState.exerciseName,
                reps = reps,
                weightKg = weightKg,
            )
            mutableState.update {
                it.copy(
                    reps = "",
                    weightKg = "",
                    sets = it.sets + savedSet,
                ).withCalculatedSummary()
            }
        }
    }

    fun toggleSetCompletion(setIndex: Int) {
        val currentSets = state.value.sets
        if (setIndex !in currentSets.indices) return

        val set = currentSets[setIndex]
        val updatedSet = set.copy(completed = !set.completed)
        val nextSets = currentSets.toMutableList().apply { this[setIndex] = updatedSet }

        mutableState.update { it.copy(sets = nextSets).withCalculatedSummary() }
        viewModelScope.launch {
            repository.setCompletion(setId = updatedSet.id, completed = updatedSet.completed)
        }
    }

    fun toggleWorkoutSetCompletion(setId: String, completed: Boolean) {
        val set = state.value.activeWorkout
            ?.exerciseBlocks
            ?.flatMap { it.sets }
            ?.firstOrNull { it.id == setId }
            ?: return
        if (completed && !set.isValidForCompletion()) {
            return
        }
        viewModelScope.launch {
            repository.updateWorkoutSet(
                setId,
                WorkoutSetInputData(
                    setType = set.setType,
                    reps = set.reps,
                    weightKg = set.weightKg,
                    rpe = set.rpe,
                    notes = set.notes,
                    completed = completed,
                ),
            )
            if (completed) {
                startRestTimer(setId)
            }
        }
    }

    fun tickRestTimer() {
        mutableState.update { current ->
            val timer = current.restTimer
            if (!timer.isVisible || !timer.isRunning) {
                current
            } else {
                val nextRemaining = (timer.remainingSeconds - 1).coerceAtLeast(0)
                current.copy(
                    restTimer = timer.copy(
                        isVisible = nextRemaining > 0,
                        isRunning = nextRemaining > 0,
                        remainingSeconds = nextRemaining,
                    ),
                )
            }
        }
    }

    fun pauseRestTimer() {
        mutableState.update { current ->
            if (!current.restTimer.isVisible) {
                current
            } else {
                current.copy(restTimer = current.restTimer.copy(isRunning = false))
            }
        }
    }

    fun resumeRestTimer() {
        mutableState.update { current ->
            val timer = current.restTimer
            if (!timer.isVisible || timer.remainingSeconds <= 0) {
                current
            } else {
                current.copy(restTimer = timer.copy(isRunning = true))
            }
        }
    }

    fun skipRestTimer() {
        mutableState.update { current ->
            current.copy(
                restTimer = current.restTimer.copy(
                    isVisible = false,
                    isRunning = false,
                    remainingSeconds = 0,
                ),
            )
        }
    }

    fun adjustRestTimerSeconds(deltaSeconds: Int) {
        if (deltaSeconds == 0) return
        mutableState.update { current ->
            val timer = current.restTimer
            if (!timer.isVisible) {
                current
            } else {
                val nextRemaining = (timer.remainingSeconds + deltaSeconds).coerceAtLeast(0)
                current.copy(
                    restTimer = timer.copy(
                        isVisible = nextRemaining > 0,
                        isRunning = timer.isRunning && nextRemaining > 0,
                        remainingSeconds = nextRemaining,
                    ),
                )
            }
        }
    }

    fun addExerciseToActiveWorkout(exerciseId: String) {
        val sessionId = state.value.activeWorkout?.sessionId ?: return
        viewModelScope.launch {
            repository.addExerciseToActiveWorkout(sessionId, exerciseId)
        }
    }

    fun addWorkoutSet(exerciseId: String) {
        val sessionId = state.value.activeWorkout?.sessionId ?: return
        viewModelScope.launch {
            repository.addSetToExercise(
                sessionId = sessionId,
                exerciseId = exerciseId,
                input = WorkoutSetInputData(
                    setType = "working",
                    reps = null,
                    weightKg = null,
                    rpe = null,
                    notes = null,
                    completed = false,
                ),
            )
        }
    }

    fun duplicateLastWorkoutSet(exerciseId: String) {
        val sessionId = state.value.activeWorkout?.sessionId ?: return
        viewModelScope.launch {
            repository.duplicateLastSet(sessionId, exerciseId)
        }
    }

    fun updateWorkoutSetFields(
        setId: String,
        setType: String,
        reps: String,
        weightKg: String,
        rpe: String,
        notes: String,
    ) {
        val existing = state.value.activeWorkout
            ?.exerciseBlocks
            ?.flatMap { it.sets }
            ?.firstOrNull { it.id == setId }
            ?: return
        viewModelScope.launch {
            repository.updateWorkoutSet(
                setId = setId,
                input = WorkoutSetInputData(
                    setType = setType.trim().ifBlank { existing.setType },
                    reps = reps.filter(Char::isDigit).toIntOrNull(),
                    weightKg = weightKg.sanitizeDecimalInput().toDoubleOrNull(),
                    rpe = rpe.sanitizeDecimalInput().toDoubleOrNull(),
                    notes = notes,
                    completed = existing.completed,
                ),
            )
        }
    }

    fun deleteWorkoutSet(setId: String) {
        viewModelScope.launch {
            repository.deleteWorkoutSet(setId)
        }
    }

    /** Pair the given standalone exercise with the next standalone exercise below it into a superset. */
    fun makeSupersetWithNext(exerciseId: String) {
        val active = state.value.activeWorkout ?: return
        val groupings = active.exerciseGroupings
        val index = groupings.indexOfFirst {
            it is ExerciseGrouping.Single && it.block.exercise.id == exerciseId
        }
        if (index < 0) return
        val next = groupings.drop(index + 1)
            .filterIsInstance<ExerciseGrouping.Single>()
            .firstOrNull() ?: return
        viewModelScope.launch {
            repository.createSuperset(active.sessionId, exerciseId, next.block.exercise.id)
        }
    }

    fun dissolveSuperset(groupId: String) {
        val sessionId = state.value.activeWorkout?.sessionId ?: return
        viewModelScope.launch {
            repository.dissolveSuperset(sessionId, groupId)
        }
    }

    fun requestFinishActiveWorkout() {
        if (state.value.activeWorkout == null) return
        mutableState.update {
            it.copy(
                finishConfirmationOpen = true,
                discardConfirmationOpen = false,
            )
        }
    }

    fun cancelFinishActiveWorkout() {
        mutableState.update { it.copy(finishConfirmationOpen = false) }
    }

    fun requestDiscardActiveWorkout() {
        if (state.value.activeWorkout == null) return
        mutableState.update {
            it.copy(
                discardConfirmationOpen = true,
                finishConfirmationOpen = false,
            )
        }
    }

    fun cancelDiscardActiveWorkout() {
        mutableState.update { it.copy(discardConfirmationOpen = false) }
    }

    fun finishActiveWorkout() {
        val sessionId = state.value.activeWorkout?.sessionId ?: return
        viewModelScope.launch {
            repository.finishWorkout(sessionId)
            val completedDetail = repository.getWorkoutHistoryDetail(sessionId)
            mutableState.update {
                it.copy(
                    activeWorkoutRouteOpen = false,
                    finishConfirmationOpen = false,
                    discardConfirmationOpen = false,
                    selectedSection = TrainingSection.History,
                    selectedWorkoutDetail = completedDetail,
                    restTimer = RestTimerState(),
                )
            }
        }
    }

    fun discardActiveWorkout() {
        val sessionId = state.value.activeWorkout?.sessionId ?: return
        viewModelScope.launch {
            repository.discardWorkout(sessionId)
            mutableState.update {
                it.copy(
                    activeWorkoutRouteOpen = false,
                    finishConfirmationOpen = false,
                    discardConfirmationOpen = false,
                    selectedSection = TrainingSection.Routines,
                    selectedWorkoutDetail = null,
                    restTimer = RestTimerState(),
                )
            }
        }
    }

    private fun TrainingUiState.withCalculatedSummary(): TrainingUiState {
        val personalRecords = WorkoutCalculator.personalRecords(
            sets.map { set ->
                WorkoutSetInput(
                    exerciseId = set.exerciseName,
                    reps = set.reps,
                    weightKg = set.weightKg,
                    completed = set.completed,
                )
            },
        )
        return copy(
            totalVolumeKg = personalRecords.totalVolumeKg,
            bestEstimatedOneRepMaxKg = personalRecords.bestEstimatedOneRepMaxKg,
        )
    }

    private fun TrainingUiState.withFilteredExercises(): TrainingUiState {
        val query = exerciseSearchQuery.trim()
        val muscle = exerciseMuscleFilter?.trim()?.takeIf(String::isNotEmpty)
        val equipment = exerciseEquipmentFilter?.trim()?.takeIf(String::isNotEmpty)
        val filtered = exercises.filter { exercise ->
            val matchesQuery = query.isBlank() ||
                exercise.name.contains(query, ignoreCase = true) ||
                exercise.targetMuscles.contains(query, ignoreCase = true) ||
                exercise.equipment.orEmpty().contains(query, ignoreCase = true)
            val matchesMuscle = muscle == null ||
                exercise.targetMuscles.contains(muscle, ignoreCase = true)
            val matchesEquipment = equipment == null ||
                exercise.equipment.equals(equipment, ignoreCase = true)

            matchesQuery && matchesMuscle && matchesEquipment
        }
        return copy(visibleExercises = filtered)
    }

    private fun moveRoutineExercise(fromIndex: Int, toIndex: Int) {
        mutableState.update { current ->
            val exercises = current.routineEditor.exercises
            if (fromIndex !in exercises.indices || toIndex !in exercises.indices) {
                current
            } else {
                val nextExercises = exercises.toMutableList()
                val moved = nextExercises.removeAt(fromIndex)
                nextExercises.add(toIndex, moved)
                current.copy(routineEditor = current.routineEditor.copy(exercises = nextExercises))
            }
        }
    }

    private fun String.sanitizeDecimalInput(): String {
        val trimmed = trim()
        val builder = StringBuilder(trimmed.length)
        var dotSeen = false

        for (char in trimmed) {
            when {
                char.isDigit() -> builder.append(char)
                char == '.' && !dotSeen -> {
                    builder.append(char)
                    dotSeen = true
                }
            }
        }

        return builder.toString()
    }

    private fun LoggedWorkoutSetDetail.isValidForCompletion(): Boolean =
        (reps ?: 0) > 0 && (weightKg ?: 0.0) > 0.0

    private fun startRestTimer(setId: String) {
        mutableState.update { current ->
            val durationSeconds = current.restTimer.durationSeconds.coerceAtLeast(1)
            current.copy(
                restTimer = RestTimerState(
                    isVisible = true,
                    sourceSetId = setId,
                    durationSeconds = durationSeconds,
                    remainingSeconds = durationSeconds,
                    isRunning = true,
                ),
            )
        }
    }

    private fun isStarterRoutine(routineId: String?): Boolean =
        routineId != null && state.value.routines.any { it.id == routineId && it.isStarter }
}

private const val DEFAULT_REST_TIMER_SECONDS = 120
