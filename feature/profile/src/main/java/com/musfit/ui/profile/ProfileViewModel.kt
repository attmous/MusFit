package com.musfit.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musfit.data.repository.BodyMeasurement
import com.musfit.data.repository.FoodGoal
import com.musfit.data.repository.FoodGoalMode
import com.musfit.data.repository.FoodRepository
import com.musfit.data.repository.GoalsRepository
import com.musfit.data.repository.HealthRepository
import com.musfit.data.repository.MEASUREMENT_TYPES
import com.musfit.data.repository.ProfileRepository
import com.musfit.data.repository.RoutineSummary
import com.musfit.data.repository.TrainingRepository
import com.musfit.data.repository.UserGoals
import com.musfit.data.repository.UserProfile
import com.musfit.data.repository.WeightEntry
import com.musfit.domain.health.HealthConnectAvailability
import com.musfit.domain.profile.BodyMetricsCalculator
import com.musfit.domain.profile.GoalType
import com.musfit.domain.profile.RecommendedTargets
import com.musfit.domain.today.WeeklyGoalsCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

data class WeightHeroState(
    val latestWeightKg: Double? = null,
    val deltaKg: Double? = null,
    val goalWeightKg: Double? = null,
    val goalProgressFraction: Double? = null,
    val bmi: Double? = null,
    val chartSeries: List<Double> = emptyList(),
    val hasAnyEntry: Boolean = false,
)

data class MeasurementTile(
    val type: String,
    val label: String,
    val value: Double?,
    val unit: String,
    val deltaFromPrevious: Double?,
    /** Turn 8 trend row: change vs the value as of 30 days ago; null without a
     *  distinct baseline (single entry, or nothing logged since the window). */
    val delta30d: Double?,
    val sparkline: List<Double>,
    val entryCount: Int,
)

data class ProfileUiState(
    val isLoaded: Boolean = false,
    val profile: UserProfile? = null,
    val hero: WeightHeroState = WeightHeroState(),
    val tiles: List<MeasurementTile> = emptyList(),
    val isProfileComplete: Boolean = false,
    val recommendedTargets: RecommendedTargets? = null,
    val weightEntries: List<WeightEntry> = emptyList(),
    val measurementEntries: Map<String, List<BodyMeasurement>> = emptyMap(),
    /** The "Goals & programs" row subline: pace · diet figure · program ×target. */
    val plansSummary: String = "",
    val isHealthConnectNudgeVisible: Boolean = false,
    val message: String? = null,
)

private val MEASUREMENT_LABELS = mapOf(
    "waist" to "Waist",
    "chest" to "Chest",
    "arms" to "Arms",
    "thighs" to "Thighs",
    "hips" to "Hips",
    "body_fat" to "Body fat",
)

private fun defaultUnitFor(type: String) = if (type == "body_fat") "%" else "cm"

@HiltViewModel
class ProfileViewModel internal constructor(
    private val profileRepository: ProfileRepository,
    private val healthRepository: HealthRepository,
    private val foodRepository: FoodRepository,
    private val trainingRepository: TrainingRepository,
    private val goalsRepository: GoalsRepository,
    private val dateProvider: () -> LocalDate,
) : ViewModel() {
    private val messageFlow = MutableStateFlow<String?>(null)
    private val nudgeFlow = MutableStateFlow(false)

    /** The day every hub window is anchored to; re-resolved on each resume so a
     *  process cached across midnight re-derives its windows from the new day.
     *  Declared before the flow properties that combine over it (init order). */
    private val activeDate = MutableStateFlow(dateProvider())

    init {
        refreshHealthConnectNudge()
    }

    @Inject
    constructor(
        profileRepository: ProfileRepository,
        healthRepository: HealthRepository,
        foodRepository: FoodRepository,
        trainingRepository: TrainingRepository,
        goalsRepository: GoalsRepository,
    ) : this(
        profileRepository = profileRepository,
        healthRepository = healthRepository,
        foodRepository = foodRepository,
        trainingRepository = trainingRepository,
        goalsRepository = goalsRepository,
        dateProvider = { LocalDate.now() },
    )

    // At the 5-arity combine cap — pre-combine pairs (see TodayViewModel.carouselFlow) before adding inputs.
    private val dataState: Flow<ProfileUiState> = combine(
        profileRepository.observeProfile(),
        profileRepository.observeRecommendedTargets(),
        profileRepository.observeWeightSeries(0L),
        profileRepository.observeRecentMeasurements(0L),
        activeDate,
    ) { profile, targets, weightSeries, measurements, today ->
        // One shared date flow, one date per emission: the window anchors must never
        // disagree across a midnight rollover, and a resume re-anchor re-runs the math.
        val todayEpochDay = today.toEpochDay()
        val sparkFromMillis = (todayEpochDay - 90L) * DAY_MILLIS
        val trendFromMillis = (todayEpochDay - 30L) * DAY_MILLIS
        val complete = profile.sex != null && profile.heightCm != null &&
            profile.birthDateEpochDay != null && weightSeries.isNotEmpty()
        ProfileUiState(
            isLoaded = true,
            profile = profile,
            hero = buildWeightHero(profile, weightSeries, todayEpochDay),
            tiles = MEASUREMENT_TYPES.map { type ->
                buildMeasurementTile(type, measurements[type].orEmpty(), sparkFromMillis, trendFromMillis)
            },
            isProfileComplete = complete,
            recommendedTargets = targets,
            weightEntries = weightSeries,
            measurementEntries = measurements,
        )
    }

    // Turn 11 folds the plan cards into one "Goals & programs" subline; the parts
    // are position-independent of the date, so the plans flow no longer needs
    // history or the activeDate anchor.
    private val plansContextFlow: Flow<Triple<FoodGoal, List<RoutineSummary>, UserGoals>> = combine(
        foodRepository.observeFoodGoal(),
        trainingRepository.observeRoutineSummaries(),
        goalsRepository.observeUserGoals(),
    ) { goal, routines, userGoals ->
        Triple(goal, routines, userGoals)
    }

    val state: StateFlow<ProfileUiState> = combine(
        dataState,
        messageFlow,
        plansContextFlow,
        nudgeFlow,
    ) { base, message, plans, nudge ->
        val (goal, routines, userGoals) = plans
        base.copy(
            message = message,
            plansSummary = buildPlansSummary(base.profile, goal, routines, userGoals),
            isHealthConnectNudgeVisible = nudge,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ProfileUiState())

    /** Re-checked on screen resume — permissions are granted OUTSIDE the app, and the
     *  ViewModel survives Profile→Settings→Profile, so init-only would never hide it.
     *  Also re-anchors the date windows: MutableStateFlow dedupes equal values, so
     *  same-day resumes are free. */
    fun onScreenResumed() {
        val today = dateProvider()
        activeDate.value = today
        refreshHealthConnectData(today)
        refreshHealthConnectNudge()
    }

    private fun refreshHealthConnectData(date: LocalDate) {
        viewModelScope.launch {
            runCatching { healthRepository.refreshRecentData(date) }
        }
    }

    private fun refreshHealthConnectNudge() {
        viewModelScope.launch {
            runCatching { healthRepository.status() }.onSuccess { status ->
                nudgeFlow.value = status.availability != HealthConnectAvailability.Available ||
                    status.grantedPermissions.isEmpty()
            }
        }
    }

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch {
            runCatching { profileRepository.saveProfile(profile) }
                .onFailure { messageFlow.value = it.message ?: "Could not save profile." }
        }
    }

    fun logWeight(weightKg: Double) {
        viewModelScope.launch {
            runCatching { profileRepository.logWeight(weightKg) }
                .onFailure { messageFlow.value = it.message ?: "Could not log weight." }
        }
    }

    fun logMeasurement(type: String, value: Double, unit: String) {
        viewModelScope.launch {
            runCatching { profileRepository.logMeasurement(type, value, unit) }
                .onFailure { messageFlow.value = it.message ?: "Could not log measurement." }
        }
    }

    fun editEntry(id: String, value: Double) {
        viewModelScope.launch {
            runCatching { profileRepository.updateEntryValue(id, value) }
                .onFailure { messageFlow.value = it.message ?: "Could not update entry." }
        }
    }

    fun deleteEntry(id: String) {
        viewModelScope.launch {
            runCatching { profileRepository.deleteEntry(id) }
                .onFailure { messageFlow.value = it.message ?: "Could not delete entry." }
        }
    }

    fun dismissMessage() {
        messageFlow.value = null
    }
}

// Windows are epoch-day anchored (UTC day boundaries), matching the Today carousel's
// weight-delta convention so both tabs report the same number.
private fun buildWeightHero(
    profile: UserProfile,
    weightSeries: List<WeightEntry>, // newest-first
    todayEpochDay: Long,
): WeightHeroState {
    val deltaAnchorMillis = (todayEpochDay - 7L) * DAY_MILLIS
    val chartFromMillis = (todayEpochDay - 30L) * DAY_MILLIS
    val latest = weightSeries.firstOrNull()
    val (_, delta) = WeeklyGoalsCalculator.weightTrend(
        weightSeries.map { it.measuredAtEpochMillis to it.weightKg },
        deltaAnchorMillis,
    )
    val goalWeight = profile.goalWeightKg?.takeIf { it > 0.0 }
    val start = weightSeries.lastOrNull() // all-time first entry
    val progress = if (start != null && latest != null && goalWeight != null) {
        BodyMetricsCalculator.goalProgressFraction(start.weightKg, latest.weightKg, goalWeight)
    } else {
        null
    }
    val height = profile.heightCm
    return WeightHeroState(
        latestWeightKg = latest?.weightKg,
        deltaKg = delta,
        goalWeightKg = goalWeight,
        goalProgressFraction = progress?.coerceAtMost(1.0),
        bmi = if (latest != null && height != null) {
            BodyMetricsCalculator.bodyMassIndex(latest.weightKg, height)
        } else {
            null
        },
        chartSeries = weightSeries.filter { it.measuredAtEpochMillis >= chartFromMillis }
            .map { it.weightKg }.reversed(),
        hasAnyEntry = weightSeries.isNotEmpty(),
    )
}

private fun buildMeasurementTile(
    type: String,
    history: List<BodyMeasurement>, // newest-first
    sparkFromMillis: Long,
    trendFromMillis: Long,
): MeasurementTile {
    val latest = history.firstOrNull()
    // Value as of 30 days ago: the newest entry at or before the cutoff, or the
    // oldest entry when the whole history is younger than the window. A baseline
    // that IS the latest entry (single entry, or a stale logger) yields no trend.
    val baseline = history.firstOrNull { it.measuredAtEpochMillis <= trendFromMillis }
        ?: history.lastOrNull()
    return MeasurementTile(
        type = type,
        label = MEASUREMENT_LABELS[type] ?: type,
        value = latest?.value,
        unit = latest?.unit ?: defaultUnitFor(type),
        // Deltas subtract raw values across rows; safe while the log dialog fixes one unit per type.
        deltaFromPrevious = history.getOrNull(1)?.let { prev -> history.first().value - prev.value },
        delta30d = if (latest != null && baseline != null && baseline.id != latest.id) {
            latest.value - baseline.value
        } else {
            null
        },
        sparkline = history.filter { it.measuredAtEpochMillis >= sparkFromMillis }
            .map { it.value }.reversed(),
        entryCount = history.size,
    )
}

/**
 * The Turn 11 "Goals & programs" subline: "Gain 0.3 kg/wk · 2,450 kcal ·
 * Full body ×3". Parts drop out when unset — no pace before a saved goal, no
 * program part without a routine; protein-led diet modes headline protein.
 */
internal fun buildPlansSummary(
    profile: UserProfile?,
    goal: FoodGoal,
    routines: List<RoutineSummary>,
    userGoals: UserGoals,
): String {
    val pace = profile?.let {
        when (it.goalType) {
            GoalType.Maintain -> "Maintain"
            GoalType.Lose -> "Lose ${it.goalPaceKgPerWeek.format1()} kg/wk"
            GoalType.Gain -> "Gain ${it.goalPaceKgPerWeek.format1()} kg/wk"
        }
    }
    val dietFigure = when (goal.mode) {
        FoodGoalMode.HighProtein, FoodGoalMode.MuscleGain ->
            "${goal.proteinGrams.roundToInt()} g protein"

        else -> String.format(Locale.US, "%,d kcal", goal.dailyCaloriesKcal.roundToInt())
    }
    val routine = routines.firstOrNull() // the coach's existing "next routine" convention
    val program = routine?.let {
        val name = it.programName ?: it.name
        if (userGoals.weeklySessionTarget > 0) "$name ×${userGoals.weeklySessionTarget}" else name
    }
    return listOfNotNull(pace, dietFigure, program).joinToString(" · ")
}

private const val DAY_MILLIS = 86_400_000L
