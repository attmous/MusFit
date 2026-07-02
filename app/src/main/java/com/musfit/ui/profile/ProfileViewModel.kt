package com.musfit.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musfit.data.repository.Account
import com.musfit.data.repository.AccountRepository
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
import com.musfit.data.repository.WorkoutHistorySummary
import com.musfit.domain.health.HealthConnectAvailability
import com.musfit.domain.profile.BodyMetricsCalculator
import com.musfit.domain.profile.RecommendedTargets
import com.musfit.domain.today.WeeklyGoalsCalculator
import com.musfit.domain.today.countSessionsInWeek
import com.musfit.ui.food.label
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Period
import java.time.temporal.TemporalAdjusters
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
    val sparkline: List<Double>,
    val entryCount: Int,
)

/** One launcher row on the Profile hub: the diet plan or the training program. */
data class PlanCard(val id: String, val title: String, val subtitle: String)

data class AccountUiState(
    val displayName: String = "You",
    val email: String? = null,
    val isLocalOnly: Boolean = true,
)

private data class AccountEditorState(
    val open: Boolean = false,
    val nameInput: String = "",
    val emailInput: String = "",
    val errorMessage: String? = null,
)

data class ProfileUiState(
    val isLoaded: Boolean = false,
    val account: AccountUiState = AccountUiState(),
    val accountEditorOpen: Boolean = false,
    val accountNameInput: String = "",
    val accountEmailInput: String = "",
    val accountErrorMessage: String? = null,
    val profile: UserProfile? = null,
    val ageYears: Int? = null,
    val hero: WeightHeroState = WeightHeroState(),
    val tiles: List<MeasurementTile> = emptyList(),
    val isProfileComplete: Boolean = false,
    val recommendedTargets: RecommendedTargets? = null,
    val weightEntries: List<WeightEntry> = emptyList(),
    val measurementEntries: Map<String, List<BodyMeasurement>> = emptyMap(),
    val planCards: List<PlanCard> = emptyList(),
    val isHealthConnectNudgeVisible: Boolean = false,
    val message: String? = null,
)

private val MEASUREMENT_LABELS = mapOf(
    "waist" to "Waist", "chest" to "Chest", "arms" to "Arms",
    "thighs" to "Thighs", "hips" to "Hips", "body_fat" to "Body fat",
)

private fun defaultUnitFor(type: String) = if (type == "body_fat") "%" else "cm"

@HiltViewModel
class ProfileViewModel internal constructor(
    private val accountRepository: AccountRepository,
    private val profileRepository: ProfileRepository,
    private val healthRepository: HealthRepository,
    private val foodRepository: FoodRepository,
    private val trainingRepository: TrainingRepository,
    private val goalsRepository: GoalsRepository,
    private val dateProvider: () -> LocalDate,
    private val clock: () -> Long,
) : ViewModel() {
    private val messageFlow = MutableStateFlow<String?>(null)
    private val accountEditorFlow = MutableStateFlow(AccountEditorState())
    private val nudgeFlow = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            runCatching { accountRepository.ensureActiveAccount() }
                .onFailure { messageFlow.value = it.message ?: "Could not prepare your local account." }
        }
        refreshHealthConnectNudge()
    }

    @Inject
    constructor(
        accountRepository: AccountRepository,
        profileRepository: ProfileRepository,
        healthRepository: HealthRepository,
        foodRepository: FoodRepository,
        trainingRepository: TrainingRepository,
        goalsRepository: GoalsRepository,
    ) : this(
        accountRepository = accountRepository,
        profileRepository = profileRepository,
        healthRepository = healthRepository,
        foodRepository = foodRepository,
        trainingRepository = trainingRepository,
        goalsRepository = goalsRepository,
        dateProvider = { LocalDate.now() },
        clock = { System.currentTimeMillis() },
    )

    private val dataState: Flow<ProfileUiState> = combine(
        profileRepository.observeProfile(),
        profileRepository.observeRecommendedTargets(),
        profileRepository.observeWeightSeries(0L),
        profileRepository.observeRecentMeasurements(0L),
    ) { profile, targets, weightSeries, measurements ->
        // One date per emission: the window anchors and the age must never disagree across
        // a midnight rollover.
        val today = dateProvider()
        val todayEpochDay = today.toEpochDay()
        val sparkFromMillis = (todayEpochDay - 90L) * DAY_MILLIS
        val complete = profile.sex != null && profile.heightCm != null &&
            profile.birthDateEpochDay != null && weightSeries.isNotEmpty()
        ProfileUiState(
            isLoaded = true,
            profile = profile,
            ageYears = profile.birthDateEpochDay?.let { ageYears(it, today) },
            hero = buildWeightHero(profile, weightSeries, todayEpochDay),
            tiles = MEASUREMENT_TYPES.map { type ->
                buildMeasurementTile(type, measurements[type].orEmpty(), sparkFromMillis)
            },
            isProfileComplete = complete,
            recommendedTargets = targets,
            weightEntries = weightSeries,
            measurementEntries = measurements,
        )
    }

    private val planCardsFlow: Flow<List<PlanCard>> = combine(
        foodRepository.observeFoodGoal(),
        trainingRepository.observeRoutineSummaries(),
        trainingRepository.observeWorkoutHistory(),
        goalsRepository.observeUserGoals(),
    ) { goal, routines, history, userGoals ->
        buildPlanCards(goal, routines, history, userGoals, dateProvider())
    }

    // Pre-combined pairs keep the outer combine within kotlinx's 5-flow typed cap.
    private val uiExtras = combine(messageFlow, accountEditorFlow) { message, editor -> message to editor }
    private val hubExtras = combine(planCardsFlow, nudgeFlow) { cards, nudge -> cards to nudge }

    val state: StateFlow<ProfileUiState> = combine(
        dataState,
        accountRepository.observeActiveAccount(),
        uiExtras,
        hubExtras,
    ) { base, account, (message, editor), (cards, nudge) ->
        base.copy(
            account = account.toUiState(),
            message = message,
            accountEditorOpen = editor.open,
            accountNameInput = editor.nameInput,
            accountEmailInput = editor.emailInput,
            accountErrorMessage = editor.errorMessage,
            planCards = cards,
            isHealthConnectNudgeVisible = nudge,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ProfileUiState())

    /** Re-checked on screen resume — permissions are granted OUTSIDE the app, and the
     *  ViewModel survives Profile→Settings→Profile, so init-only would never hide it. */
    fun onScreenResumed() = refreshHealthConnectNudge()

    private fun refreshHealthConnectNudge() {
        viewModelScope.launch {
            runCatching { healthRepository.status() }.onSuccess { status ->
                nudgeFlow.value = status.availability != HealthConnectAvailability.Available ||
                    status.grantedPermissions.isEmpty()
            }
        }
    }

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch { profileRepository.saveProfile(profile) }
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

    fun applyTargetsToFood() {
        val targets = state.value.recommendedTargets ?: return
        viewModelScope.launch {
            runCatching {
                val current = foodRepository.observeFoodGoal().first()
                foodRepository.updateFoodGoal(
                    current.copy(
                        dailyCaloriesKcal = targets.caloriesKcal,
                        proteinGrams = targets.proteinGrams,
                        carbsGrams = targets.carbsGrams,
                        fatGrams = targets.fatGrams,
                    ),
                )
            }.onSuccess {
                messageFlow.value = "Applied your targets to Food goals."
            }.onFailure {
                messageFlow.value = it.message ?: "Could not apply targets to Food."
            }
        }
    }

    fun openAccountEditor() {
        val account = state.value.account
        accountEditorFlow.value = AccountEditorState(
            open = true,
            nameInput = account.displayName,
            emailInput = account.email.orEmpty(),
        )
    }

    fun closeAccountEditor() {
        accountEditorFlow.value = AccountEditorState()
    }

    fun onAccountNameChanged(value: String) {
        accountEditorFlow.update { it.copy(nameInput = value, errorMessage = null) }
    }

    fun onAccountEmailChanged(value: String) {
        accountEditorFlow.update { it.copy(emailInput = value, errorMessage = null) }
    }

    fun saveAccount() {
        val editor = accountEditorFlow.value
        if (editor.nameInput.isBlank()) {
            accountEditorFlow.update { it.copy(errorMessage = "Account name is required.") }
            return
        }
        viewModelScope.launch {
            runCatching {
                accountRepository.updateActiveAccount(
                    displayName = editor.nameInput.trim(),
                    email = editor.emailInput.trim().takeIf { it.isNotBlank() },
                )
            }.onSuccess {
                accountEditorFlow.value = AccountEditorState()
            }.onFailure { error ->
                accountEditorFlow.update {
                    it.copy(errorMessage = error.message ?: "Could not save account.")
                }
            }
        }
    }

    fun dismissMessage() {
        messageFlow.value = null
    }

    private fun ageYears(birthDateEpochDay: Long, today: LocalDate): Int =
        Period.between(LocalDate.ofEpochDay(birthDateEpochDay), today).years.coerceAtLeast(0)
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
): MeasurementTile = MeasurementTile(
    type = type,
    label = MEASUREMENT_LABELS[type] ?: type,
    value = history.firstOrNull()?.value,
    unit = history.firstOrNull()?.unit ?: defaultUnitFor(type),
    // Deltas subtract raw values across rows; safe while the log dialog fixes one unit per type.
    deltaFromPrevious = history.getOrNull(1)?.let { prev -> history.first().value - prev.value },
    sparkline = history.filter { it.measuredAtEpochMillis >= sparkFromMillis }
        .map { it.value }.reversed(),
    entryCount = history.size,
)

private fun buildPlanCards(
    goal: FoodGoal,
    routines: List<RoutineSummary>,
    history: List<WorkoutHistorySummary>,
    userGoals: UserGoals,
    today: LocalDate,
): List<PlanCard> {
    // Protein-led modes headline the protein target; everything else headlines calories.
    val dietFigure = when (goal.mode) {
        FoodGoalMode.HighProtein, FoodGoalMode.MuscleGain -> "${goal.proteinGrams.roundToInt()} g protein target"
        else -> "${goal.dailyCaloriesKcal.roundToInt()} kcal target"
    }
    val diet = PlanCard(id = "diet", title = "${goal.mode.label} diet", subtitle = "$dietFigure · manage in Food")

    // Monday-anchored week on UTC epoch days, matching Today's sessions metric.
    val weekStartMillis = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay() * DAY_MILLIS
    val sessions = countSessionsInWeek(history.map { it.startedAtEpochMillis }, weekStartMillis)
    val routine = routines.firstOrNull() // the coach's existing "next routine" convention
    val training = if (routine == null) {
        PlanCard(id = "training", title = "No program yet", subtitle = "Set one up in Training")
    } else {
        val target = userGoals.weeklySessionTarget
        val subtitle = if (target > 0) "$sessions of $target sessions this week" else "$sessions sessions this week"
        PlanCard(id = "training", title = routine.programName ?: routine.name, subtitle = subtitle)
    }
    return listOf(diet, training)
}

private fun Account.toUiState() =
    AccountUiState(
        displayName = displayName,
        email = email,
        isLocalOnly = remoteUserId == null,
    )

private const val DAY_MILLIS = 86_400_000L
