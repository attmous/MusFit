package com.musfit.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musfit.data.local.entity.DailyHealthSummaryEntity
import com.musfit.data.repository.BodyMeasurement
import com.musfit.data.repository.FoodRepository
import com.musfit.data.repository.HealthRepository
import com.musfit.data.repository.MEASUREMENT_TYPES
import com.musfit.data.repository.ProfileRepository
import com.musfit.data.repository.UserProfile
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

data class ProfileUiState(
    val isLoaded: Boolean = false,
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
    val vitals: VitalsSummary? = null,
    val message: String? = null,
)

private val MEASUREMENT_LABELS = mapOf(
    "waist" to "Waist", "chest" to "Chest", "arms" to "Arms",
    "thighs" to "Thighs", "hips" to "Hips", "body_fat" to "Body fat",
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val healthRepository: HealthRepository,
    private val foodRepository: FoodRepository,
) : ViewModel() {
    private val today = LocalDate.now()
    private val messageFlow = MutableStateFlow<String?>(null)

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
        )
    }

    val state: StateFlow<ProfileUiState> = combine(
        dataState,
        healthRepository.observeDailySummary(today),
        messageFlow,
    ) { base, summary, message ->
        base.copy(vitals = summary?.toVitals(), message = message)
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
