package com.musfit.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musfit.data.local.entity.DailyHealthSummaryEntity
import com.musfit.data.repository.FoodRepository
import com.musfit.data.repository.HealthRepository
import com.musfit.data.repository.TrainingRepository
import com.musfit.data.repository.TrainingSummary
import com.musfit.domain.model.NutritionTotals
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

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

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
    private val trainingRepository: TrainingRepository,
    private val healthRepository: HealthRepository,
) : ViewModel() {
    private var dateProvider: () -> LocalDate = { LocalDate.now() }
    private val mutableState = MutableStateFlow(TodayUiState())
    val state: StateFlow<TodayUiState> = mutableState.asStateFlow()

    init {
        observeToday()
    }

    internal constructor(
        foodRepository: FoodRepository,
        trainingRepository: TrainingRepository,
        healthRepository: HealthRepository,
        dateProvider: () -> LocalDate,
    ) : this(foodRepository, trainingRepository, healthRepository) {
        this.dateProvider = dateProvider
    }

    private fun observeToday() {
        val date = dateProvider()
        viewModelScope.launch {
            combine(
                foodRepository.observeDailyNutrition(date),
                trainingRepository.observeDailyTrainingSummary(date),
                healthRepository.observeDailySummary(date),
            ) { nutrition, training, health ->
                toUiState(
                    nutrition = nutrition,
                    training = training,
                    health = health,
                )
            }.collect { uiState ->
                mutableState.value = uiState
            }
        }
    }
}

private fun toUiState(
    nutrition: NutritionTotals,
    training: TrainingSummary,
    health: DailyHealthSummaryEntity?,
): TodayUiState =
    TodayUiState(
        caloriesKcal = nutrition.caloriesKcal,
        proteinGrams = nutrition.proteinGrams,
        carbsGrams = nutrition.carbsGrams,
        fatGrams = nutrition.fatGrams,
        steps = health?.steps,
        activeCaloriesKcal = health?.activeCaloriesKcal,
        bodyWeightKg = health?.latestWeightKg,
        trainingSummary = training.displaySummary(),
    )

private fun TrainingSummary.displaySummary(): String =
    if (completedSetCount == 0) {
        "No workout logged today"
    } else {
        "$completedSetCount ${if (completedSetCount == 1) "set" else "sets"} - ${totalVolumeKg.formatMetric()} kg volume"
    }

private fun Double.formatMetric(): String =
    if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", this)
    }
