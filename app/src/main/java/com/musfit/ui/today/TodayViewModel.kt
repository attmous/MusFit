package com.musfit.ui.today

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TodayUiState(
    val caloriesKcal: Double = 0.0,
    val proteinGrams: Double = 0.0,
    val carbsGrams: Double = 0.0,
    val fatGrams: Double = 0.0,
    val steps: Long? = null,
    val activeCaloriesKcal: Double? = null,
    val bodyWeightKg: Double? = null,
    val trainingSummary: String = "No workout logged today",
)

class TodayViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(TodayUiState())
    val state: StateFlow<TodayUiState> = mutableState.asStateFlow()
}
