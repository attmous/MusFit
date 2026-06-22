package com.musfit.ui.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musfit.data.repository.ActiveWorkoutDetail
import com.musfit.data.repository.ActiveWorkoutSummary
import com.musfit.data.repository.ExerciseSummary
import com.musfit.data.repository.LoggedWorkoutSet
import com.musfit.data.repository.WorkoutSetInputData
import com.musfit.data.repository.RoutineExerciseInput
import com.musfit.data.repository.RoutineInput
import com.musfit.data.repository.RoutineSummary
import com.musfit.data.repository.TrainingRepository
import com.musfit.domain.model.WorkoutSetInput
import com.musfit.domain.training.WorkoutCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
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

data class RestTimerState(
    val isVisible: Boolean = false,
    val sourceSetId: String? = null,
    val durationSeconds: Int = 120,
)

data class TrainingUiState(
    val selectedSection: TrainingSection = TrainingSection.Routines,
    val routines: List<RoutineSummary> = emptyList(),
    val exercises: List<ExerciseSummary> = emptyList(),
    val activeWorkoutSummary: ActiveWorkoutSummary? = null,
    val activeWorkout: ActiveWorkoutDetail? = null,
    val exerciseSearchQuery: String = "",
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
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.observeActiveWorkoutDetail().collect { activeWorkout ->
                mutableState.update { it.copy(activeWorkout = activeWorkout) }
            }
        }
    }

    fun selectSection(section: TrainingSection) {
        mutableState.update { it.copy(selectedSection = section) }
    }

    fun resumeActiveWorkout() {
        mutableState.update { it.copy(activeWorkoutRouteOpen = true) }
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

    fun saveRoutineEditor() {
        val editor = state.value.routineEditor
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
                mutableState.update {
                    it.copy(restTimer = RestTimerState(isVisible = true, sourceSetId = setId))
                }
            }
        }
    }

    fun finishActiveWorkout() {
        val sessionId = state.value.activeWorkout?.sessionId ?: return
        viewModelScope.launch {
            repository.finishWorkout(sessionId)
            mutableState.update {
                it.copy(activeWorkoutRouteOpen = false, finishConfirmationOpen = false)
            }
        }
    }

    fun discardActiveWorkout() {
        val sessionId = state.value.activeWorkout?.sessionId ?: return
        viewModelScope.launch {
            repository.discardWorkout(sessionId)
            mutableState.update {
                it.copy(activeWorkoutRouteOpen = false, discardConfirmationOpen = false)
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
}
