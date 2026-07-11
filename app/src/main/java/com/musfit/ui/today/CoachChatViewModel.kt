package com.musfit.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musfit.data.local.entity.DailyHealthSummaryEntity
import com.musfit.data.repository.AiCoachChatMessage
import com.musfit.data.repository.AiCoachChatRepository
import com.musfit.data.repository.AiCoachProviderKind
import com.musfit.data.repository.FoodGoal
import com.musfit.data.repository.FoodRepository
import com.musfit.data.repository.FoodWaterSummary
import com.musfit.data.repository.GoalsRepository
import com.musfit.data.repository.HealthRepository
import com.musfit.data.repository.LocalAgentKind
import com.musfit.data.repository.ProfileRepository
import com.musfit.data.repository.RoutineSummary
import com.musfit.data.repository.TrainingRepository
import com.musfit.data.repository.UserGoals
import com.musfit.data.repository.UserProfile
import com.musfit.data.repository.WorkoutHistorySummary
import com.musfit.data.repository.AiCoachRepository
import com.musfit.domain.model.NutritionTotals
import com.musfit.ui.permissions.LOCAL_NETWORK_PERMISSION_DENIED_MESSAGE
import com.musfit.ui.permissions.requiresLocalNetworkPermission
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class CoachChatUiState(
    val messages: List<AiCoachChatMessage> = emptyList(),
    val input: String = "",
    val isSending: Boolean = false,
    val isConfigured: Boolean = false,
    val providerLabel: String = "Off",
    val requiresLocalNetworkPermission: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class CoachChatViewModel @Inject constructor(
    private val aiCoachRepository: AiCoachRepository,
    private val aiCoachChatRepository: AiCoachChatRepository,
    private val foodRepository: FoodRepository,
    private val trainingRepository: TrainingRepository,
    private val healthRepository: HealthRepository,
    private val goalsRepository: GoalsRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(CoachChatUiState())

    val state: StateFlow<CoachChatUiState> = combine(
        mutableState,
        aiCoachRepository.observeSettings(),
        aiCoachChatRepository.observeMessages(),
    ) { base, settings, messages ->
        base.copy(
            messages = messages,
            isConfigured = settings.providerKind != AiCoachProviderKind.Disabled,
            providerLabel = settings.providerKind.chatLabel(settings.localAgentKind),
            requiresLocalNetworkPermission = requiresLocalNetworkPermission(settings.baseUrl),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CoachChatUiState())

    fun onInputChanged(value: String) {
        mutableState.update { it.copy(input = value, errorMessage = null) }
    }

    fun send() {
        val prompt = state.value.input.trim()
        if (prompt.isBlank() || state.value.isSending) return
        mutableState.update { it.copy(input = "", isSending = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                aiCoachChatRepository.sendMessage(prompt, buildSystemPrompt())
            }.onFailure { error ->
                mutableState.update {
                    it.copy(errorMessage = error.message ?: "Coach is not reachable.")
                }
            }
            mutableState.update { it.copy(isSending = false) }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            runCatching { aiCoachChatRepository.clearThread() }
                .onFailure { error ->
                    mutableState.update { it.copy(errorMessage = error.message ?: "Could not clear coach chat.") }
                }
        }
    }

    fun reportLocalNetworkPermissionDenied() {
        mutableState.update {
            it.copy(errorMessage = LOCAL_NETWORK_PERMISSION_DENIED_MESSAGE)
        }
    }

    fun testConnection() {
        mutableState.update { it.copy(errorMessage = null) }
        viewModelScope.launch {
            runCatching { aiCoachChatRepository.testConnection() }
                .onFailure { error ->
                    mutableState.update { it.copy(errorMessage = error.message ?: "Coach is not reachable.") }
                }
        }
    }

    private suspend fun buildSystemPrompt(): String {
        val today = LocalDate.now()
        val nutrition = foodRepository.observeDailyNutrition(today).first()
        val foodGoal = foodRepository.observeFoodGoal().first()
        val water = foodRepository.observeWaterSummary(today).first()
        val health = healthRepository.observeDailySummary(today).first()
        val userGoals = goalsRepository.observeUserGoals().first()
        val profile = profileRepository.observeProfile().first()
        val routines = trainingRepository.observeRoutineSummaries().first()
        val history = trainingRepository.observeWorkoutHistory().first()
        return coachSystemPrompt(
            date = today,
            nutrition = nutrition,
            foodGoal = foodGoal,
            water = water,
            health = health,
            userGoals = userGoals,
            profile = profile,
            routines = routines,
            history = history,
        )
    }
}

internal fun coachSystemPrompt(
    date: LocalDate,
    nutrition: NutritionTotals,
    foodGoal: FoodGoal,
    water: FoodWaterSummary,
    health: DailyHealthSummaryEntity?,
    userGoals: UserGoals,
    profile: UserProfile,
    routines: List<RoutineSummary>,
    history: List<WorkoutHistorySummary>,
): String {
    val lastWorkout = history.maxByOrNull { it.startedAtEpochMillis }
    val daysSinceWorkout = lastWorkout?.let {
        ((System.currentTimeMillis() - it.startedAtEpochMillis) / DAY_MILLIS).coerceAtLeast(0L)
    }
    val nextRoutine = routines.firstOrNull()
    val lines = listOf(
        "You are MusFit Coach inside an Android fitness and nutrition tracker.",
        "Be concise, practical, and specific. You are read-only: do not claim you can log, edit, or delete MusFit data.",
        "If action is needed, tell the user where to do it in MusFit.",
        "Current local context for $date:",
        "Food: ${nutrition.caloriesKcal.round()} / ${foodGoal.dailyCaloriesKcal.round()} kcal, " +
            "protein ${nutrition.proteinGrams.round()} / ${foodGoal.proteinGrams.round()} g, " +
            "carbs ${nutrition.carbsGrams.round()} / ${foodGoal.carbsGrams.round()} g, " +
            "fat ${nutrition.fatGrams.round()} / ${foodGoal.fatGrams.round()} g.",
        "Water: ${water.consumedMilliliters.round()} / ${water.goalMilliliters.round()} ml.",
        "Health: steps ${health?.steps ?: 0} / ${userGoals.stepGoal}, " +
            "active calories ${health?.activeCaloriesKcal?.round() ?: 0} kcal, " +
            "sleep ${health?.sleepMinutes?.let { "${it / 60}h ${it % 60}m" } ?: "unknown"}.",
        "Training: last workout ${daysSinceWorkout?.let { "$it days ago (${lastWorkout.title})" } ?: "not logged yet"}, " +
            "next routine ${nextRoutine?.name ?: "none selected"}, weekly target ${userGoals.weeklySessionTarget} sessions.",
        "Profile: goal weight ${profile.goalWeightKg?.format1() ?: "not set"} kg, activity ${profile.activityLevel}.",
    )
    return lines.joinToString("\n")
}

private fun AiCoachProviderKind.chatLabel(localAgentKind: LocalAgentKind): String = when (this) {
    AiCoachProviderKind.Disabled -> "Off"
    AiCoachProviderKind.OpenAiCompatible -> "API coach"
    AiCoachProviderKind.LocalAgent -> when (localAgentKind) {
        LocalAgentKind.HermesAgent -> "Hermes"
        LocalAgentKind.OpenClaw -> "OpenClaw"
        LocalAgentKind.Custom -> "Local agent"
    }
}

private fun Double.round(): String = roundToInt().toString()

private fun Double.format1(): String = String.format(Locale.US, "%.1f", this)

private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
