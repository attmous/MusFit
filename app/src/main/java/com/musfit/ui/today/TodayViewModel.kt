package com.musfit.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musfit.data.local.entity.BodyMetricEntity
import com.musfit.data.local.entity.DailyHealthSummaryEntity
import com.musfit.data.repository.FoodGoal
import com.musfit.data.repository.FoodRepository
import com.musfit.data.repository.GoalsRepository
import com.musfit.data.repository.HealthRepository
import com.musfit.data.repository.RoutineSummary
import com.musfit.data.repository.TrainingRepository
import com.musfit.data.repository.TrainingSummary
import com.musfit.data.repository.UserGoals
import com.musfit.data.repository.WorkoutHistorySummary
import com.musfit.domain.coach.CoachBriefing
import com.musfit.domain.coach.CoachEngine
import com.musfit.domain.coach.CoachInput
import com.musfit.domain.coach.TimeOfDay
import com.musfit.domain.model.NutritionTotals
import com.musfit.domain.today.WeeklyGoals
import com.musfit.domain.today.WeeklyGoalsCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
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
    val weekly: WeeklyGoals? = null,
    val isGoalsEditorVisible: Boolean = false,
    val stepGoalInput: String = "",
    val sessionTargetInput: String = "",
    val targetWeightInput: String = "",
    val coach: CoachBriefing? = null,
)

@HiltViewModel
class TodayViewModel internal constructor(
    private val foodRepository: FoodRepository,
    private val trainingRepository: TrainingRepository,
    private val healthRepository: HealthRepository,
    private val goalsRepository: GoalsRepository,
    private val dateProvider: () -> LocalDate,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {
    private val mutableState = MutableStateFlow(TodayUiState())
    val state: StateFlow<TodayUiState> = mutableState.asStateFlow()

    private var currentUserGoals = UserGoals()

    init {
        observeDaily()
        observeWeekly()
        observeCoach()
    }

    @Inject
    constructor(
        foodRepository: FoodRepository,
        trainingRepository: TrainingRepository,
        healthRepository: HealthRepository,
        goalsRepository: GoalsRepository,
    ) : this(
        foodRepository = foodRepository,
        trainingRepository = trainingRepository,
        healthRepository = healthRepository,
        goalsRepository = goalsRepository,
        dateProvider = { LocalDate.now() },
    )

    private fun observeDaily() {
        val date = dateProvider()
        viewModelScope.launch {
            combine(
                foodRepository.observeDailyNutrition(date),
                foodRepository.observeFoodGoal(),
                trainingRepository.observeDailyTrainingSummary(date),
                healthRepository.observeDailySummary(date),
                goalsRepository.observeUserGoals(),
            ) { nutrition, goal, training, health, userGoals ->
                currentUserGoals = userGoals
                buildDaily(date, nutrition, goal, training, health, userGoals)
            }.collect { daily ->
                mutableState.update {
                    it.copy(
                        dateLabel = daily.dateLabel,
                        rings = daily.rings,
                        macros = daily.macros,
                        training = daily.training,
                        weightKg = daily.weightKg,
                    )
                }
            }
        }
    }

    private fun observeWeekly() {
        val date = dateProvider()
        val weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = weekStart.plusDays(6)
        val weekStartMillis = weekStart.toEpochDay() * DAY_MILLIS
        val weightFromMillis = (weekStart.toEpochDay() - 7L) * DAY_MILLIS
        viewModelScope.launch {
            val foodAndTraining = combine(
                foodRepository.observeFoodPlan(weekStart),
                trainingRepository.observeWorkoutHistory(),
                healthRepository.observeDailySummaries(weekStart, weekEnd),
            ) { plan, history, summaries -> Triple(plan, history, summaries) }
            val weightAndGoals = combine(
                healthRepository.observeWeightSeries(weightFromMillis),
                goalsRepository.observeUserGoals(),
                foodRepository.observeFoodGoal(),
            ) { weights, userGoals, foodGoal -> Triple(weights, userGoals, foodGoal) }
            combine(foodAndTraining, weightAndGoals) { ft, wg ->
                val (plan, history, summaries) = ft
                val (weights, userGoals, foodGoal) = wg
                WeeklyGoalsCalculator.compute(
                    weekStartMillis = weekStartMillis,
                    sessionStartMillis = history.map { it.startedAtEpochMillis },
                    sessionTarget = userGoals.weeklySessionTarget,
                    loggedCaloriesPerDay = plan.map { if (it.loggedEntryCount > 0) it.loggedTotals.caloriesKcal else null },
                    calorieGoalKcal = foodGoal.dailyCaloriesKcal,
                    stepsPerDay = summaries.map { it.steps ?: 0L },
                    stepGoal = userGoals.stepGoal,
                    weights = weights.map { it.measuredAtEpochMillis to it.value },
                    targetWeightKg = userGoals.targetWeightKg,
                )
            }.collect { weekly ->
                mutableState.update { it.copy(weekly = weekly) }
            }
        }
    }

    private fun observeCoach() {
        val date = dateProvider()
        val weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekStartMillis = weekStart.toEpochDay() * DAY_MILLIS
        val weightFromMillis = (weekStart.toEpochDay() - 7L) * DAY_MILLIS
        viewModelScope.launch {
            val nutritionGoalHealth = combine(
                foodRepository.observeDailyNutrition(date),
                foodRepository.observeFoodGoal(),
                healthRepository.observeDailySummary(date),
            ) { nutrition, goal, health -> Triple(nutrition, goal, health) }
            val trainingAndGoals = combine(
                trainingRepository.observeWorkoutHistory(),
                trainingRepository.observeRoutineSummaries(),
                goalsRepository.observeUserGoals(),
            ) { history, routines, userGoals -> Triple(history, routines, userGoals) }
            combine(
                nutritionGoalHealth,
                trainingAndGoals,
                healthRepository.observeWeightSeries(weightFromMillis),
            ) { ngh, tg, weights ->
                val (nutrition, goal, health) = ngh
                val (history, routines, userGoals) = tg
                buildCoachBriefing(nutrition, goal, health, history, routines, userGoals, weights, weekStartMillis)
            }.collect { briefing ->
                mutableState.update { it.copy(coach = briefing) }
            }
        }
    }

    private fun buildCoachBriefing(
        nutrition: NutritionTotals,
        goal: FoodGoal,
        health: DailyHealthSummaryEntity?,
        history: List<WorkoutHistorySummary>,
        routines: List<RoutineSummary>,
        userGoals: UserGoals,
        weights: List<BodyMetricEntity>,
        weekStartMillis: Long,
    ): CoachBriefing {
        val nowMillis = clock()
        val lastWorkoutMillis = history.maxOfOrNull { it.startedAtEpochMillis }
        val daysSince = lastWorkoutMillis?.let { ((nowMillis - it) / DAY_MILLIS).toInt().coerceAtLeast(0) }
        val nextRoutine = routines.firstOrNull()
        val (_, weightDelta) = WeeklyGoalsCalculator.weightTrend(
            weights.map { it.measuredAtEpochMillis to it.value },
            weekStartMillis,
        )
        return CoachEngine.briefing(
            CoachInput(
                timeOfDay = timeOfDay(nowMillis),
                firstName = null,
                caloriesKcal = nutrition.caloriesKcal,
                calorieGoalKcal = goal.dailyCaloriesKcal,
                proteinGrams = nutrition.proteinGrams,
                proteinGoalGrams = goal.proteinGrams,
                daysSinceLastWorkout = daysSince,
                nextRoutineName = nextRoutine?.name,
                nextRoutineId = nextRoutine?.id,
                weightDeltaKg = weightDelta,
                targetWeightKg = userGoals.targetWeightKg.takeIf { it > 0.0 },
                stepsToday = health?.steps ?: 0L,
                stepGoal = userGoals.stepGoal,
            ),
        )
    }

    private fun timeOfDay(nowMillis: Long): TimeOfDay {
        val hour = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()).hour
        return when {
            hour < 12 -> TimeOfDay.Morning
            hour < 18 -> TimeOfDay.Afternoon
            else -> TimeOfDay.Evening
        }
    }

    fun openGoalsEditor() {
        val goals = currentUserGoals
        mutableState.update {
            it.copy(
                isGoalsEditorVisible = true,
                stepGoalInput = goals.stepGoal.toString(),
                sessionTargetInput = goals.weeklySessionTarget.toString(),
                targetWeightInput = if (goals.targetWeightKg > 0.0) goals.targetWeightKg.formatMetric() else "",
            )
        }
    }

    fun closeGoalsEditor() {
        mutableState.update { it.copy(isGoalsEditorVisible = false) }
    }

    fun onStepGoalInputChanged(value: String) {
        mutableState.update { it.copy(stepGoalInput = value.filter(Char::isDigit)) }
    }

    fun onSessionTargetInputChanged(value: String) {
        mutableState.update { it.copy(sessionTargetInput = value.filter(Char::isDigit)) }
    }

    fun onTargetWeightInputChanged(value: String) {
        mutableState.update { it.copy(targetWeightInput = value.filter { ch -> ch.isDigit() || ch == '.' }) }
    }

    fun saveUserGoals() {
        val current = state.value
        val goals = UserGoals(
            stepGoal = current.stepGoalInput.toLongOrNull()?.coerceAtLeast(0L) ?: currentUserGoals.stepGoal,
            weeklySessionTarget = current.sessionTargetInput.toIntOrNull()?.coerceAtLeast(0) ?: currentUserGoals.weeklySessionTarget,
            targetWeightKg = current.targetWeightInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
        )
        viewModelScope.launch {
            goalsRepository.updateUserGoals(goals)
            mutableState.update { it.copy(isGoalsEditorVisible = false) }
        }
    }
}

private const val DAY_MILLIS = 86_400_000L
private const val DEFAULT_STEP_GOAL_FLOOR = 1L

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.US)

private data class TodayDaily(
    val dateLabel: String,
    val rings: List<DailyRingUiState>,
    val macros: MacroBreakdownUiState,
    val training: TrainingGlimpseUiState,
    val weightKg: Double?,
)

private fun buildDaily(
    date: LocalDate,
    nutrition: NutritionTotals,
    goal: FoodGoal,
    training: TrainingSummary,
    health: DailyHealthSummaryEntity?,
    userGoals: UserGoals,
): TodayDaily {
    val steps = health?.steps ?: 0L
    val stepGoal = userGoals.stepGoal.coerceAtLeast(DEFAULT_STEP_GOAL_FLOOR)
    return TodayDaily(
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
                goalLabel = "of ${formatCount(stepGoal)}",
                progress = ratio(steps.toDouble(), stepGoal.toDouble()),
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
