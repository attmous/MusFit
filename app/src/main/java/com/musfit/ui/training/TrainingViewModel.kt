package com.musfit.ui.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musfit.data.repository.LoggedWorkoutSet
import com.musfit.data.repository.TrainingRepository
import com.musfit.domain.model.WorkoutSetInput
import com.musfit.domain.training.WorkoutCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrainingUiState(
    val exerciseName: String = "",
    val reps: String = "",
    val weightKg: String = "",
    val sets: List<LoggedWorkoutSet> = emptyList(),
    val totalVolumeKg: Double = 0.0,
    val bestEstimatedOneRepMaxKg: Double = 0.0,
)

@HiltViewModel
class TrainingViewModel @Inject constructor(
    private val repository: TrainingRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TrainingUiState())
    val state: StateFlow<TrainingUiState> = mutableState.asStateFlow()

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
