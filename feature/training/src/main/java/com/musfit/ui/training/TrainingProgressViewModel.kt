package com.musfit.ui.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musfit.data.repository.ExerciseSummary
import com.musfit.data.repository.TrainingPrRecord
import com.musfit.data.repository.TrainingProgressAnalytics
import com.musfit.data.repository.TrainingRepository
import com.musfit.domain.model.ExerciseProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
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
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TrainingProgressViewModel @Inject constructor(
    private val repository: TrainingRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TrainingProgressUiState())
    val state: StateFlow<TrainingProgressUiState> = repositoryObservationFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = mutableState.value,
        )
    private var autoSelected = false
    private val progressRetryGeneration = MutableStateFlow(0L)
    private var failedProgressExerciseId: String? = null

    private fun repositoryObservationFlow(): Flow<TrainingProgressUiState> = channelFlow {
        supervisorScope {
            launchRepositoryObserver {
                repository.observeExercises().collect { exercises ->
                    mutableState.update { it.copy(exercises = exercises) }
                }
            }
            launchRepositoryObserver {
                repository.observeTrainingProgressAnalytics().collect { analytics ->
                    mutableState.update { it.copy(progressAnalytics = analytics) }
                }
            }
            launchRepositoryObserver {
                repository.observeRecentPersonalRecords().collect { prs ->
                    mutableState.update { it.copy(recentPrs = prs) }
                    // Default anchor: the most recently PR'd exercise, once, unless the user picked one.
                    if (!autoSelected && mutableState.value.selectedProgressExerciseId == null) {
                        prs.firstOrNull()?.let { pr ->
                            autoSelected = true
                            selectProgressExercise(pr.exerciseId)
                        }
                    }
                }
            }
            launchRepositoryObserver {
                repository.observeLoggedExerciseIds().collect { ids ->
                    mutableState.update { it.copy(loggedExerciseIds = ids) }
                }
            }
            launchRepositoryObserver {
                selectedExerciseProgressFlow().collect { (exerciseId, progress) ->
                    mutableState.update { current ->
                        if (current.selectedProgressExerciseId == exerciseId) {
                            current.copy(selectedExerciseProgress = progress)
                        } else {
                            current
                        }
                    }
                }
            }
            mutableState.collect { send(it) }
        }
    }

    private fun selectedExerciseProgressFlow(): Flow<Pair<String, ExerciseProgress?>> = combine(
        mutableState
            .map { it.selectedProgressExerciseId }
            .distinctUntilChanged(),
        progressRetryGeneration,
    ) { exerciseId, retryGeneration -> exerciseId to retryGeneration }
        .flatMapLatest { (exerciseId, _) ->
            if (exerciseId == null) {
                emptyFlow()
            } else {
                repository.observeExerciseProgress(exerciseId)
                    .onEach {
                        if (mutableState.value.selectedProgressExerciseId == exerciseId) {
                            failedProgressExerciseId = null
                        }
                    }
                    .map { progress -> exerciseId to progress }
                    .catch { error ->
                        if (error is CancellationException) throw error
                        if (mutableState.value.selectedProgressExerciseId == exerciseId) {
                            failedProgressExerciseId = exerciseId
                        }
                        // Retain the last value so an explicit same-exercise retry does not flash empty.
                    }
            }
        }

    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    private fun CoroutineScope.launchRepositoryObserver(observe: suspend () -> Unit): Job = launch {
        try {
            observe()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // Retain this source's last known value while sibling observations remain live.
        }
    }

    fun selectProgressExercise(exerciseId: String) {
        autoSelected = true
        if (mutableState.value.selectedProgressExerciseId == exerciseId) {
            mutableState.update { it.copy(exercisePickerOpen = false) }
            if (failedProgressExerciseId == exerciseId) {
                failedProgressExerciseId = null
                progressRetryGeneration.update { it + 1 }
            }
            return
        }
        failedProgressExerciseId = null
        mutableState.update {
            it.copy(
                selectedProgressExerciseId = exerciseId,
                selectedExerciseProgress = null,
                exercisePickerOpen = false,
            )
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
