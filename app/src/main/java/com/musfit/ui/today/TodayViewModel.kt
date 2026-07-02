package com.musfit.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musfit.data.local.entity.BodyMetricEntity
import com.musfit.data.local.entity.DailyHealthSummaryEntity
import com.musfit.data.repository.BodyMeasurement
import com.musfit.data.repository.CoachMessage
import com.musfit.data.repository.CoachRepository
import com.musfit.data.repository.FoodGoal
import com.musfit.data.repository.FoodRepository
import com.musfit.data.repository.FoodWaterSummary
import com.musfit.data.repository.GoalsRepository
import com.musfit.data.repository.HealthRepository
import com.musfit.data.repository.ProfileRepository
import com.musfit.data.repository.RoutineSummary
import com.musfit.data.repository.TrainingRepository
import com.musfit.data.repository.TrainingSummary
import com.musfit.data.repository.UserGoals
import com.musfit.data.repository.WorkoutHistorySummary
import com.musfit.domain.coach.CoachEngine
import com.musfit.domain.coach.CoachInput
import com.musfit.domain.coach.TimeOfDay
import com.musfit.domain.model.NutritionTotals
import com.musfit.domain.today.LoggingStreakCalculator
import com.musfit.domain.today.MetricResolver
import com.musfit.domain.today.MetricSnapshot
import com.musfit.domain.today.MetricValue
import com.musfit.domain.today.TodayMetric
import com.musfit.domain.today.WeeklyGoals
import com.musfit.domain.today.WeeklyGoalsCalculator
import com.musfit.domain.today.buildCarouselPages
import com.musfit.domain.today.countSessionsInWeek
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
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

data class DayBar(val label: String, val calories: Double?)

data class WeeklyChartsUiState(
    val calorieBars: List<DayBar>,
    val calorieTarget: Double?,
    val onTargetDays: Int,
    val trackedDays: Int,
    val defaultSelectedIndex: Int?,
    val weightTrend: List<Double>,
    val weightDeltaKg: Double?,
    val latestWeightKg: Double?,
    val sessionsDone: Int,
    val sessionTarget: Int,
    val stepGoalDays: Int,
)

data class TrainingGlimpseUiState(
    val title: String = "No workout yet",
    val subtitle: String = "Tap to start",
    val hasWorkout: Boolean = false,
)

data class CoachFeedDayGroup(
    val label: String,
    val messages: List<CoachMessage>,
)

data class MetricCardUiState(
    val metric: TodayMetric,
    val label: String,
    val value: MetricValue,
)

data class CarouselPageUiState(
    val hero: MetricCardUiState?,
    val chips: List<MetricCardUiState>,
)

data class CarouselUiState(val pages: List<CarouselPageUiState> = emptyList())

data class TodayUiState(
    val dateLabel: String = "",
    val rings: List<DailyRingUiState> = emptyList(),
    val macros: MacroBreakdownUiState = MacroBreakdownUiState(),
    val training: TrainingGlimpseUiState = TrainingGlimpseUiState(),
    val weightKg: Double? = null,
    val weekly: WeeklyGoals? = null,
    val weeklyCharts: WeeklyChartsUiState? = null,
    val selectedCalorieDayIndex: Int? = null,
    val carousel: CarouselUiState = CarouselUiState(),
    val isDashboardEditorVisible: Boolean = false,
    val editPins: List<TodayMetric> = emptyList(),
    val stepGoalInput: String = "",
    val sessionTargetInput: String = "",
    val targetWeightInput: String = "",
    val feed: List<CoachFeedDayGroup> = emptyList(),
    val isChatPreviewVisible: Boolean = false,
)

@HiltViewModel
class TodayViewModel internal constructor(
    private val foodRepository: FoodRepository,
    private val trainingRepository: TrainingRepository,
    private val healthRepository: HealthRepository,
    private val goalsRepository: GoalsRepository,
    private val coachRepository: CoachRepository,
    private val profileRepository: ProfileRepository,
    private val dateProvider: () -> LocalDate,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {
    private val mutableState = MutableStateFlow(TodayUiState())
    val state: StateFlow<TodayUiState> = mutableState.asStateFlow()

    private var currentUserGoals = UserGoals()
    private var currentPins: List<TodayMetric> = TodayMetric.DEFAULT_PINS
    private var currentProfileGoalWeightKg: Double? = null

    /** The freshest coach input, pinned to the [activeDate] its flows were anchored to. */
    private var latestCoachInput: Pair<LocalDate, CoachInput>? = null
    private var isResumed = false

    /** The day the coach's date-scoped flows are anchored to; re-resolved on every resume. */
    private val activeDate = MutableStateFlow(dateProvider())

    init {
        observeDaily()
        observeWeekly()
        observeCarousel()
        observeCoach()
        observeFeed()
    }

    @Inject
    constructor(
        foodRepository: FoodRepository,
        trainingRepository: TrainingRepository,
        healthRepository: HealthRepository,
        goalsRepository: GoalsRepository,
        coachRepository: CoachRepository,
        profileRepository: ProfileRepository,
    ) : this(
        foodRepository = foodRepository,
        trainingRepository = trainingRepository,
        healthRepository = healthRepository,
        goalsRepository = goalsRepository,
        coachRepository = coachRepository,
        profileRepository = profileRepository,
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
        val weightFromMillis = (weekStart.toEpochDay() - 30L) * DAY_MILLIS
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
                val todayIndex = date.dayOfWeek.value - 1
                mutableState.update { it.copy(weekly = weekly, weeklyCharts = buildWeeklyCharts(weekly, todayIndex)) }
            }
        }
    }

    private fun observeCarousel() {
        val date = dateProvider()
        val weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekStartMillis = weekStart.toEpochDay() * DAY_MILLIS
        val weightFromMillis = (date.toEpochDay() - 30L) * DAY_MILLIS
        val measurementsFromMillis = (date.toEpochDay() - 90L) * DAY_MILLIS
        viewModelScope.launch {
            val food = combine(
                foodRepository.observeDailyNutrition(date),
                foodRepository.observeFoodGoal(),
                foodRepository.observeWaterSummary(date),
                foodRepository.observeLoggedDayEpochDays(date.toEpochDay() - 365L),
            ) { nutrition, goal, water, loggedDays -> FoodSnapshot(nutrition, goal, water, loggedDays) }
            val health = combine(
                healthRepository.observeDailySummary(date),
                goalsRepository.observeUserGoals(),
                healthRepository.observeWeightSeries(weightFromMillis),
            ) { summary, userGoals, weights -> Triple(summary, userGoals, weights) }
            val bodyTrainingProfile = combine(
                profileRepository.observeRecentMeasurements(measurementsFromMillis),
                trainingRepository.observeWorkoutHistory(),
                profileRepository.observeProfile(),
            ) { measurements, history, profile -> Triple(measurements, history, profile) }
            combine(
                food,
                health,
                bodyTrainingProfile,
                coachRepository.observeDashboardPins(),
            ) { f, h, btp, pins ->
                val (summary, userGoals, weights) = h
                val (measurements, history, profile) = btp
                currentPins = pins
                currentUserGoals = userGoals
                currentProfileGoalWeightKg = profile.goalWeightKg
                buildCarousel(f, summary, userGoals, weights, measurements, history, pins, date, weekStartMillis)
            }.collect { carousel ->
                mutableState.update { it.copy(carousel = carousel) }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeCoach() {
        viewModelScope.launch {
            activeDate.flatMapLatest { date -> coachInputFlow(date).map { input -> date to input } }
                .collect { anchored ->
                    latestCoachInput = anchored
                    if (isResumed) syncCoachFeed()
                }
        }
    }

    private fun coachInputFlow(date: LocalDate): Flow<CoachInput> {
        val weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekStartMillis = weekStart.toEpochDay() * DAY_MILLIS
        val weightFromMillis = (weekStart.toEpochDay() - 7L) * DAY_MILLIS
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
        val waterAndStreak = combine(
            foodRepository.observeWaterSummary(date),
            foodRepository.observeLoggedDayEpochDays(date.toEpochDay() - 365L),
        ) { water, loggedDays -> water to loggedDays }
        return combine(
            nutritionGoalHealth,
            trainingAndGoals,
            healthRepository.observeWeightSeries(weightFromMillis),
            waterAndStreak,
        ) { ngh, tg, weights, ws ->
            val (nutrition, goal, health) = ngh
            val (history, routines, userGoals) = tg
            val (water, loggedDays) = ws
            buildCoachInput(
                date, nutrition, goal, health, history, routines, userGoals, weights,
                water, loggedDays, weekStartMillis,
            )
        }
    }

    private fun observeFeed() {
        viewModelScope.launch {
            // Combining with activeDate re-derives Today/Yesterday labels on every
            // resume re-anchor, even when the sync writes nothing new.
            combine(coachRepository.observeFeed(), activeDate) { messages, today ->
                buildFeedGroups(messages, today)
            }.collect { groups ->
                mutableState.update { it.copy(feed = groups) }
            }
        }
    }

    fun onScreenResumed() {
        isResumed = true
        // Re-anchor date-scoped coach flows — a cached process crossing midnight must
        // generate the new day's messages from the new day's data, never stale flows.
        activeDate.value = dateProvider()
        syncCoachFeed()
    }

    fun onScreenPaused() {
        isResumed = false
        viewModelScope.launch { coachRepository.markAllRead() }
    }

    fun dismissMessage(id: Long) {
        viewModelScope.launch { coachRepository.dismiss(id) }
    }

    fun openChatPreview() = mutableState.update { it.copy(isChatPreviewVisible = true) }

    fun closeChatPreview() = mutableState.update { it.copy(isChatPreviewVisible = false) }

    private fun syncCoachFeed() {
        val (anchor, input) = latestCoachInput ?: return
        viewModelScope.launch {
            // Resolve "today" at invocation time — a cached process crossing midnight
            // must write under the new day, never back-fill the old one. An input built
            // from another day's flows is never written at all: after a rollover the
            // stale-anchored sync self-suppresses and the re-anchored emission from
            // observeCoach drives the first sync of the new day.
            val today = dateProvider()
            if (anchor != today) return@launch
            coachRepository.syncToday(today, CoachEngine.messages(input))
        }
    }

    private fun buildCoachInput(
        date: LocalDate,
        nutrition: NutritionTotals,
        goal: FoodGoal,
        health: DailyHealthSummaryEntity?,
        history: List<WorkoutHistorySummary>,
        routines: List<RoutineSummary>,
        userGoals: UserGoals,
        weights: List<BodyMetricEntity>,
        water: FoodWaterSummary,
        loggedDays: List<Long>,
        weekStartMillis: Long,
    ): CoachInput {
        val nowMillis = clock()
        val lastWorkoutMillis = history.maxOfOrNull { it.startedAtEpochMillis }
        val daysSince = lastWorkoutMillis?.let { ((nowMillis - it) / DAY_MILLIS).toInt().coerceAtLeast(0) }
        val nextRoutine = routines.firstOrNull()
        val (_, weightDelta) = WeeklyGoalsCalculator.weightTrend(
            weights.map { it.measuredAtEpochMillis to it.value },
            weekStartMillis,
        )
        return CoachInput(
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
            carbsGrams = nutrition.carbsGrams,
            carbsGoalGrams = goal.carbsGrams,
            fatGrams = nutrition.fatGrams,
            fatGoalGrams = goal.fatGrams,
            waterMl = water.consumedMilliliters,
            waterGoalMl = water.goalMilliliters,
            loggingStreakDays = LoggingStreakCalculator.streakDays(loggedDays, date.toEpochDay()),
            hasLoggedToday = loggedDays.contains(date.toEpochDay()),
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

    fun openDashboardEditor() {
        val goals = currentUserGoals
        val targetWeight = goals.targetWeightKg.takeIf { it > 0.0 }
            ?: currentProfileGoalWeightKg?.takeIf { it > 0.0 }
        mutableState.update {
            it.copy(
                isDashboardEditorVisible = true,
                editPins = currentPins,
                stepGoalInput = goals.stepGoal.toString(),
                sessionTargetInput = goals.weeklySessionTarget.toString(),
                targetWeightInput = targetWeight?.formatMetric() ?: "",
            )
        }
    }

    fun closeDashboardEditor() = mutableState.update { it.copy(isDashboardEditorVisible = false) }

    fun togglePin(metric: TodayMetric) {
        mutableState.update { state ->
            val pins = state.editPins
            val next = when {
                metric in pins && pins.size <= 1 -> pins // never remove the last pin
                metric in pins -> pins - metric
                else -> pins + metric
            }
            state.copy(editPins = next)
        }
    }

    fun movePin(metric: TodayMetric, up: Boolean) {
        mutableState.update { state ->
            val pins = state.editPins.toMutableList()
            val index = pins.indexOf(metric)
            val target = if (up) index - 1 else index + 1
            if (index == -1 || target !in pins.indices) return@update state
            pins[index] = pins[target].also { pins[target] = metric }
            state.copy(editPins = pins)
        }
    }

    fun onCalorieDaySelected(index: Int) {
        mutableState.update { it.copy(selectedCalorieDayIndex = index) }
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

    fun saveDashboard() {
        val current = state.value
        val goals = UserGoals(
            stepGoal = current.stepGoalInput.toLongOrNull()?.coerceAtLeast(0L) ?: currentUserGoals.stepGoal,
            weeklySessionTarget = current.sessionTargetInput.toIntOrNull()?.coerceAtLeast(0)
                ?: currentUserGoals.weeklySessionTarget,
            // Blank/unparseable input keeps the stored value (the profile-prefill makes this
            // field non-empty far more often; silently zeroing the store would wipe the goal).
            targetWeightKg = current.targetWeightInput.toDoubleOrNull()?.coerceAtLeast(0.0)
                ?: currentUserGoals.targetWeightKg,
        )
        viewModelScope.launch {
            coachRepository.saveDashboardPins(current.editPins)
            goalsRepository.updateUserGoals(goals)
            mutableState.update { it.copy(isDashboardEditorVisible = false) }
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

internal val DAY_LABELS = listOf("M", "T", "W", "T", "F", "S", "S")

private val FEED_DAY_FORMATTER = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())

/** Pure: groups feed messages under day headers, newest day first, newest message first. */
internal fun buildFeedGroups(messages: List<CoachMessage>, today: LocalDate): List<CoachFeedDayGroup> =
    messages
        .groupBy { it.day }
        .toSortedMap(reverseOrder())
        .map { (day, dayMessages) ->
            CoachFeedDayGroup(
                label = when (day) {
                    today -> "Today"
                    today.minusDays(1) -> "Yesterday"
                    else -> day.format(FEED_DAY_FORMATTER)
                },
                messages = dayMessages.sortedByDescending { it.firstSeenAtEpochMillis },
            )
        }

private data class FoodSnapshot(
    val nutrition: NutritionTotals,
    val goal: FoodGoal,
    val water: FoodWaterSummary,
    val loggedDays: List<Long>,
)

private fun buildCarousel(
    food: FoodSnapshot,
    health: DailyHealthSummaryEntity?,
    userGoals: UserGoals,
    weights: List<BodyMetricEntity>,
    measurements: Map<String, List<BodyMeasurement>>,
    history: List<WorkoutHistorySummary>,
    pins: List<TodayMetric>,
    date: LocalDate,
    weekStartMillis: Long,
): CarouselUiState {
    val (_, weightDelta) = WeeklyGoalsCalculator.weightTrend(
        weights.map { it.measuredAtEpochMillis to it.value },
        (date.toEpochDay() - 7L) * DAY_MILLIS,
    )
    val bodyFatSeries = measurements["body_fat"].orEmpty().sortedBy { it.measuredAtEpochMillis }
    val snapshot = MetricSnapshot(
        caloriesKcal = food.nutrition.caloriesKcal,
        calorieGoalKcal = food.goal.dailyCaloriesKcal,
        proteinGrams = food.nutrition.proteinGrams,
        proteinGoalGrams = food.goal.proteinGrams,
        carbsGrams = food.nutrition.carbsGrams,
        carbsGoalGrams = food.goal.carbsGrams,
        fatGrams = food.nutrition.fatGrams,
        fatGoalGrams = food.goal.fatGrams,
        waterMl = food.water.consumedMilliliters,
        waterGoalMl = food.water.goalMilliliters,
        steps = health?.steps,
        stepGoal = userGoals.stepGoal,
        latestWeightKg = weights.maxByOrNull { it.measuredAtEpochMillis }?.value,
        weightDeltaKg = weightDelta,
        bodyFatPercent = bodyFatSeries.lastOrNull()?.value,
        bodyFatDelta = bodyFatSeries.takeLast(2).takeIf { it.size == 2 }?.let { it[1].value - it[0].value },
        sessionsDone = countSessionsInWeek(history.map { it.startedAtEpochMillis }, weekStartMillis),
        sessionTarget = userGoals.weeklySessionTarget,
        activeCaloriesKcal = health?.activeCaloriesKcal,
        restingHeartRateBpm = health?.restingHeartRateBpm,
        loggingStreakDays = LoggingStreakCalculator.streakDays(food.loggedDays, date.toEpochDay()),
    )
    return CarouselUiState(
        pages = buildCarouselPages(pins).map { page ->
            CarouselPageUiState(
                hero = page.hero?.let { MetricCardUiState(it, it.label, MetricResolver.resolve(it, snapshot)) },
                chips = page.chips.map { MetricCardUiState(it, it.label, MetricResolver.resolve(it, snapshot)) },
            )
        },
    )
}

/** Pure mapping from the weekly rollup to chart UI state. [todayIndex] is Monday=0..Sunday=6. */
internal fun buildWeeklyCharts(weekly: WeeklyGoals, todayIndex: Int): WeeklyChartsUiState {
    val bars = DAY_LABELS.mapIndexed { i, label -> DayBar(label = label, calories = weekly.caloriesPerDay.getOrNull(i)) }
    val defaultSelected = when {
        bars.getOrNull(todayIndex)?.calories != null -> todayIndex
        else -> bars.indexOfLast { it.calories != null }.takeIf { it >= 0 }
    }
    return WeeklyChartsUiState(
        calorieBars = bars,
        calorieTarget = weekly.calorieGoalKcal.takeIf { it > 0.0 },
        onTargetDays = weekly.calorieOnTargetDays,
        trackedDays = weekly.trackedDays,
        defaultSelectedIndex = defaultSelected,
        weightTrend = weekly.weightPoints.map { it.valueKg },
        weightDeltaKg = weekly.weightDeltaKg,
        latestWeightKg = weekly.weightPoints.lastOrNull()?.valueKg ?: weekly.weightAvgKg,
        sessionsDone = weekly.sessionsDone,
        sessionTarget = weekly.sessionTarget,
        stepGoalDays = weekly.stepGoalDays,
    )
}

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
