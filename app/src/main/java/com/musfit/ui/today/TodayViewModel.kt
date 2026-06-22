package com.musfit.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musfit.data.local.entity.DailyHealthSummaryEntity
import com.musfit.data.repository.FoodGoal
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
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

enum class RingKind { Calories, Protein, Steps }

data class DailyRingUiState(
    val kind: RingKind,
    val centerLabel: String,
    val goalLabel: String,
    val progress: Float,
)

data class MacroBreakdownUiState(
    val carbsGrams: Double = 0.0,
    val proteinGrams: Double = 0.0,
    val fatGrams: Double = 0.0,
)

data class TrainingGlimpseUiState(
    val title: String = "No workout yet",
    val subtitle: String = "Tap to start",
    val hasWorkout: Boolean = false,
)

data class TodayUiState(
    val dateLabel: String = "",
    val rings: List<DailyRingUiState> = emptyList(),
    val macros: MacroBreakdownUiState = MacroBreakdownUiState(),
    val training: TrainingGlimpseUiState = TrainingGlimpseUiState(),
    val weightKg: Double? = null,
)

@HiltViewModel
class TodayViewModel internal constructor(
    private val foodRepository: FoodRepository,
    private val trainingRepository: TrainingRepository,
    private val healthRepository: HealthRepository,
    private val dateProvider: () -> LocalDate,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TodayUiState())
    val state: StateFlow<TodayUiState> = mutableState.asStateFlow()

    init {
        observeToday()
    }

    @Inject
    constructor(
        foodRepository: FoodRepository,
        trainingRepository: TrainingRepository,
        healthRepository: HealthRepository,
    ) : this(
        foodRepository = foodRepository,
        trainingRepository = trainingRepository,
        healthRepository = healthRepository,
        dateProvider = { LocalDate.now() },
    )

    private fun observeToday() {
        val date = dateProvider()
        viewModelScope.launch {
            combine(
                foodRepository.observeDailyNutrition(date),
                foodRepository.observeFoodGoal(),
                trainingRepository.observeDailyTrainingSummary(date),
                healthRepository.observeDailySummary(date),
            ) { nutrition, goal, training, health ->
                toUiState(date, nutrition, goal, training, health)
            }.collect { uiState ->
                mutableState.value = uiState
            }
        }
    }
}

private const val DEFAULT_STEP_GOAL = 10_000L

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.US)

private fun toUiState(
    date: LocalDate,
    nutrition: NutritionTotals,
    goal: FoodGoal,
    training: TrainingSummary,
    health: DailyHealthSummaryEntity?,
): TodayUiState {
    val steps = health?.steps ?: 0L
    return TodayUiState(
        dateLabel = date.format(DATE_FORMATTER),
        rings = listOf(
            DailyRingUiState(
                kind = RingKind.Calories,
                centerLabel = nutrition.caloriesKcal.formatMetric(),
                goalLabel = "of ${goal.dailyCaloriesKcal.formatMetric()}",
                progress = ratio(nutrition.caloriesKcal, goal.dailyCaloriesKcal),
            ),
            DailyRingUiState(
                kind = RingKind.Protein,
                centerLabel = "${nutrition.proteinGrams.formatMetric()} g",
                goalLabel = "of ${goal.proteinGrams.formatMetric()} g",
                progress = ratio(nutrition.proteinGrams, goal.proteinGrams),
            ),
            DailyRingUiState(
                kind = RingKind.Steps,
                centerLabel = formatCount(steps),
                goalLabel = "of ${formatCount(DEFAULT_STEP_GOAL)}",
                progress = ratio(steps.toDouble(), DEFAULT_STEP_GOAL.toDouble()),
            ),
        ),
        macros = MacroBreakdownUiState(
            carbsGrams = nutrition.carbsGrams,
            proteinGrams = nutrition.proteinGrams,
            fatGrams = nutrition.fatGrams,
        ),
        training = training.toGlimpse(),
        weightKg = health?.latestWeightKg,
    )
}

private fun ratio(value: Double, goal: Double): Float =
    if (goal <= 0.0) 0f else (value / goal).coerceIn(0.0, 1.0).toFloat()

private fun TrainingSummary.toGlimpse(): TrainingGlimpseUiState =
    if (completedSetCount == 0) {
        TrainingGlimpseUiState()
    } else {
        TrainingGlimpseUiState(
            title = "$completedSetCount ${if (completedSetCount == 1) "set" else "sets"}",
            subtitle = "${totalVolumeKg.formatMetric()} kg volume",
            hasWorkout = true,
        )
    }

private fun formatCount(value: Long): String =
    if (value >= 1000) {
        val thousands = value / 1000.0
        if (thousands % 1.0 == 0.0) {
            "${thousands.toInt()}k"
        } else {
            String.format(Locale.US, "%.1fk", thousands)
        }
    } else {
        value.toString()
    }

private fun Double.formatMetric(): String =
    if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", this)
    }
