package com.musfit.ui.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musfit.data.repository.FoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    private val mutableState = MutableStateFlow(NutritionTrendsUiState())
    val state: StateFlow<NutritionTrendsUiState> = mutableState.asStateFlow()

    init {
        val today = LocalDate.now()
        viewModelScope.launch {
            repository.observeWeeklyFoodSummary(today.minusDays(6)).collect { summary ->
                mutableState.update { it.copy(weeklyScore = summary.toWeeklyScoreUiState()) }
            }
        }
        viewModelScope.launch {
            repository.observeFoodProgressSummary(today.minusDays(27), dayCount = 28).collect { summary ->
                mutableState.update { it.copy(progressStats = summary.toProgressStatsUiState()) }
            }
        }
    }
}
