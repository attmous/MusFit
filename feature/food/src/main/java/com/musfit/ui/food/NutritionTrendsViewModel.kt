package com.musfit.ui.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musfit.data.repository.FoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.time.LocalDate
import javax.inject.Inject

data class NutritionTrendsUiState(
    val weeklyScore: FoodWeeklyScoreUiState = emptyWeeklyScore(),
    val progressStats: FoodProgressStatsUiState = emptyProgressStats(),
)

/**
 * Backs the Nutrition trends Profile sub-screen. Unlike the Food diary (which scopes trends to the
 * selected diary date), this is anchored to today at open — a "how am I doing lately" review that a
 * fresh nav entry re-anchors on each visit.
 */
@HiltViewModel
class NutritionTrendsViewModel @Inject constructor(
    private val repository: FoodRepository,
) : ViewModel() {
    private val today = LocalDate.now()
    private val mutableState = MutableStateFlow(NutritionTrendsUiState())
    val state: StateFlow<NutritionTrendsUiState> = trendsObservationFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = mutableState.value,
        )

    private fun trendsObservationFlow(): Flow<NutritionTrendsUiState> = channelFlow {
        supervisorScope {
            launchTrendsObserver {
                repository.observeWeeklyFoodSummary(today.minusDays(6)).collect { weekly ->
                    mutableState.update { currentState ->
                        currentState.copy(weeklyScore = weekly.toWeeklyScoreUiState())
                    }
                }
            }
            launchTrendsObserver {
                repository.observeFoodProgressSummary(today.minusDays(27), dayCount = 28).collect { progress ->
                    mutableState.update { currentState ->
                        currentState.copy(progressStats = progress.toProgressStatsUiState())
                    }
                }
            }
            mutableState.collect { send(it) }
        }
    }

    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    private fun CoroutineScope.launchTrendsObserver(observe: suspend () -> Unit) {
        launch {
            try {
                observe()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // Retain this source's last known value while the sibling observation remains live.
            }
        }
    }
}
