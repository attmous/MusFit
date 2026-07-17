package com.musfit.ui.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musfit.data.repository.ExerciseSummary
import com.musfit.data.repository.TrainingPrRecord
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

/** The Progress page's period selector (mock 5b): a dropdown, not chips. */
enum class TrainingProgressPeriod(val label: String, val days: Long) {
    TwelveWeeks("12 weeks", 84),
    SixMonths("6 months", 183),
    Year("1 year", 365),
}

data class TrainingProgressUiState(
    val exercises: List<ExerciseSummary> = emptyList(),
    val selectedProgressExerciseId: String? = null,
    val selectedExerciseProgress: ExerciseProgress? = null,
    val progressAnalytics: TrainingProgressAnalytics = TrainingProgressAnalytics(),
    val recentPrs: List<TrainingPrRecord> = emptyList(),
    val loggedExerciseIds: Set<String> = emptySet(),
    val period: TrainingProgressPeriod = TrainingProgressPeriod.TwelveWeeks,
    val exercisePickerOpen: Boolean = false,
)

/**
 * Backs the Progress page (mock 5b): one anchored exercise's e1RM chart, the weekly
 * volume bars, and the cross-exercise "Recent PRs" list. The most recent PR's exercise
 * anchors the page until the user picks another via "All exercises".
 */
@HiltViewModel
class TrainingProgressViewModel @Inject constructor(
    private val repository: TrainingRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TrainingProgressUiState())
    val state: StateFlow<TrainingProgressUiState> = mutableState.asStateFlow()
    private var progressObservationJob: Job? = null
    private var autoSelected = false

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
        viewModelScope.launch {
            repository.observeRecentPersonalRecords().collect { prs ->
                mutableState.update { it.copy(recentPrs = prs) }
                // Default anchor: the most recently PR'd exercise, once, unless the user picked one.
                if (!autoSelected && state.value.selectedProgressExerciseId == null) {
                    prs.firstOrNull()?.let { pr ->
                        autoSelected = true
                        selectProgressExercise(pr.exerciseId)
                    }
                }
            }
        }
        viewModelScope.launch {
            repository.observeLoggedExerciseIds().collect { ids ->
                mutableState.update { it.copy(loggedExerciseIds = ids) }
            }
        }
    }

    fun selectProgressExercise(exerciseId: String) {
        autoSelected = true
        mutableState.update {
            it.copy(
                selectedProgressExerciseId = exerciseId,
                selectedExerciseProgress = null,
                exercisePickerOpen = false,
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

    fun selectPeriod(period: TrainingProgressPeriod) {
        mutableState.update { it.copy(period = period) }
    }

    fun openExercisePicker() {
        mutableState.update { it.copy(exercisePickerOpen = true) }
    }

    fun closeExercisePicker() {
        mutableState.update { it.copy(exercisePickerOpen = false) }
    }
}
