package com.musfit.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musfit.data.local.entity.DailyHealthSummaryEntity
import com.musfit.data.repository.Account
import com.musfit.data.repository.AccountRepository
import com.musfit.data.repository.BodyMeasurement
import com.musfit.data.repository.FoodRepository
import com.musfit.data.repository.HealthRepository
import com.musfit.data.repository.MEASUREMENT_TYPES
import com.musfit.data.repository.ProfileRepository
import com.musfit.data.repository.UserProfile
import com.musfit.data.repository.WeightEntry
import com.musfit.domain.profile.BodyMetricsCalculator
import com.musfit.domain.profile.RecommendedTargets
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
import java.time.LocalDate
import java.time.Period
import javax.inject.Inject

data class VitalsSummary(
    val steps: Long?,
    val activeCaloriesKcal: Double?,
    val restingHeartRateBpm: Long?,
)

data class MeasurementRow(
    val type: String,
    val label: String,
    val value: Double?,
    val unit: String,
    val deltaFromPrevious: Double?,
)

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
    val latestWeightKg: Double? = null,
    val bmi: Double? = null,
    val bodyFatPercent: Double? = null,
    val isProfileComplete: Boolean = false,
    val recommendedTargets: RecommendedTargets? = null,
    val weightTrend: List<Double> = emptyList(),
    val goalProgressFraction: Double? = null,
    val measurements: List<MeasurementRow> = emptyList(),
    val weightEntries: List<WeightEntry> = emptyList(),
    val measurementEntries: Map<String, List<BodyMeasurement>> = emptyMap(),
    val weeklyWeightDeltaKg: Double? = null,
    val vitals: VitalsSummary? = null,
    val message: String? = null,
)

private val MEASUREMENT_LABELS = mapOf(
    "waist" to "Waist", "chest" to "Chest", "arms" to "Arms",
    "thighs" to "Thighs", "hips" to "Hips", "body_fat" to "Body fat",
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val profileRepository: ProfileRepository,
    private val healthRepository: HealthRepository,
    private val foodRepository: FoodRepository,
) : ViewModel() {
    private val today = LocalDate.now()
    private val messageFlow = MutableStateFlow<String?>(null)
    private val accountEditorFlow = MutableStateFlow(AccountEditorState())

    init {
        viewModelScope.launch {
            runCatching { accountRepository.ensureActiveAccount() }
                .onFailure { messageFlow.value = it.message ?: "Could not prepare your local account." }
        }
    }

    private val dataState: Flow<ProfileUiState> = combine(
        profileRepository.observeProfile(),
        profileRepository.observeRecommendedTargets(),
        profileRepository.observeWeightSeries(0L),
        profileRepository.observeRecentMeasurements(0L),
    ) { profile, targets, weightSeries, measurements ->
        val latestWeight = weightSeries.firstOrNull()?.weightKg
        val height = profile.heightCm
        val bmi = if (latestWeight != null && height != null) {
            BodyMetricsCalculator.bodyMassIndex(latestWeight, height)
        } else {
            null
        }
        val bodyFat = measurements["body_fat"]?.firstOrNull()?.value
        val complete = profile.sex != null && profile.heightCm != null &&
            profile.birthDateEpochDay != null && latestWeight != null
        val startWeight = weightSeries.lastOrNull()?.weightKg
        val goalWeight = profile.goalWeightKg
        val progress = if (startWeight != null && latestWeight != null && goalWeight != null) {
            BodyMetricsCalculator.goalProgressFraction(startWeight, latestWeight, goalWeight)
        } else {
            null
        }
        val weeklyDelta = BodyMetricsCalculator.changeOverWindow(
            points = weightSeries.map { it.measuredAtEpochMillis to it.weightKg },
            windowMillis = 7L * 86_400_000L,
            nowMillis = System.currentTimeMillis(),
        )
        ProfileUiState(
            isLoaded = true,
            profile = profile,
            ageYears = profile.birthDateEpochDay?.let { ageYears(it) },
            latestWeightKg = latestWeight,
            bmi = bmi,
            bodyFatPercent = bodyFat,
            isProfileComplete = complete,
            recommendedTargets = targets,
            weightTrend = weightSeries.map { it.weightKg }.reversed(),
            goalProgressFraction = progress,
            measurements = MEASUREMENT_TYPES.map { type -> measurementRow(type, measurements[type].orEmpty()) },
            weightEntries = weightSeries,
            measurementEntries = measurements,
            weeklyWeightDeltaKg = weeklyDelta,
        )
    }

    val state: StateFlow<ProfileUiState> = combine(
        dataState,
        accountRepository.observeActiveAccount(),
        healthRepository.observeDailySummary(today),
        messageFlow,
        accountEditorFlow,
    ) { base, account, summary, message, editor ->
        base.copy(
            account = account.toUiState(),
            vitals = summary?.toVitals(),
            message = message,
            accountEditorOpen = editor.open,
            accountNameInput = editor.nameInput,
            accountEmailInput = editor.emailInput,
            accountErrorMessage = editor.errorMessage,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ProfileUiState())

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

    private fun measurementRow(type: String, history: List<BodyMeasurement>): MeasurementRow {
        val latest = history.firstOrNull()
        val previous = history.getOrNull(1)
        return MeasurementRow(
            type = type,
            label = MEASUREMENT_LABELS[type] ?: type,
            value = latest?.value,
            unit = latest?.unit ?: if (type == "body_fat") "%" else "cm",
            deltaFromPrevious = if (latest != null && previous != null) latest.value - previous.value else null,
        )
    }

    private fun ageYears(birthDateEpochDay: Long): Int =
        Period.between(LocalDate.ofEpochDay(birthDateEpochDay), today).years.coerceAtLeast(0)
}

private fun DailyHealthSummaryEntity.toVitals() =
    VitalsSummary(steps = steps, activeCaloriesKcal = activeCaloriesKcal, restingHeartRateBpm = restingHeartRateBpm)

private fun Account.toUiState() =
    AccountUiState(
        displayName = displayName,
        email = email,
        isLocalOnly = remoteUserId == null,
    )
