package com.musfit.ui.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musfit.data.repository.ExerciseSummary
import com.musfit.data.repository.TrainingProgressAnalytics
import com.musfit.data.repository.TrainingRepository
import com.musfit.domain.model.ExerciseProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrainingProgressUiState(
    val exercises: List<ExerciseSummary> = emptyList(),
    val selectedProgressExerciseId: String? = null,
    val selectedExerciseProgress: ExerciseProgress? = null,
    val progressAnalytics: TrainingProgressAnalytics = TrainingProgressAnalytics(),
)

/**
 * Backs the Training progress Profile sub-screen: exercise picker, per-exercise PRs/trends, and the
 * muscle/weekly volume analytics. Split out of TrainingViewModel when Progress graduated from a
 * Training section tab to a Profile sub-screen.
 */
@HiltViewModel
class TrainingProgressViewModel @Inject constructor(
    private val repository: TrainingRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TrainingProgressUiState())
    val state: StateFlow<TrainingProgressUiState> = mutableState.asStateFlow()
    private var progressObservationJob: Job? = null

    init {
        viewModelScope.launch {
            repository.observeExercises().collect { exercises ->
                mutableState.update { it.copy(exercises = exercises) }
            }
        }
        viewModelScope.launch {
            repository.observeTrainingProgressAnalytics().collect { analytics ->
                mutableState.update { it.copy(progressAnalytics = analytics) }
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
}
