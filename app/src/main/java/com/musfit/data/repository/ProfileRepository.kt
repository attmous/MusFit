package com.musfit.data.repository

import com.musfit.data.local.dao.HealthDao
import com.musfit.data.local.dao.ProfileDao
import com.musfit.data.local.entity.AppSettingsEntity
import com.musfit.data.local.entity.BodyMetricEntity
import com.musfit.data.local.entity.UserProfileEntity
import com.musfit.domain.profile.ActivityLevel
import com.musfit.domain.profile.EnergyCalculator
import com.musfit.domain.profile.GoalType
import com.musfit.domain.profile.RecommendedTargets
import com.musfit.domain.profile.Sex
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.Period
import java.util.UUID
import javax.inject.Inject

const val WEIGHT_METRIC_TYPE = "weight"
val MEASUREMENT_TYPES = listOf("waist", "chest", "arms", "thighs", "hips", "body_fat")

data class UserProfile(
    val sex: Sex?,
    val birthDateEpochDay: Long?,
    val heightCm: Double?,
    val activityLevel: ActivityLevel,
    val goalType: GoalType,
    val goalPaceKgPerWeek: Double,
    val goalWeightKg: Double?,
)

data class WeightEntry(val id: String, val measuredAtEpochMillis: Long, val weightKg: Double, val source: String)

data class BodyMeasurement(val id: String, val type: String, val value: Double, val unit: String, val measuredAtEpochMillis: Long)

data class AppSettings(val unitSystem: String, val themeMode: String)

val DEFAULT_USER_PROFILE = UserProfile(
    sex = null,
    birthDateEpochDay = null,
    heightCm = null,
    activityLevel = ActivityLevel.Moderate,
    goalType = GoalType.Maintain,
    goalPaceKgPerWeek = 0.5,
    goalWeightKg = null,
)

val DEFAULT_APP_SETTINGS = AppSettings(unitSystem = "metric", themeMode = "system")

interface ProfileRepository {
    fun observeProfile(): Flow<UserProfile>
    suspend fun saveProfile(profile: UserProfile)
    fun observeRecommendedTargets(): Flow<RecommendedTargets?>
    suspend fun logWeight(weightKg: Double, source: String = "manual")
    fun observeLatestWeight(): Flow<WeightEntry?>
    fun observeWeightSeries(sinceEpochMillis: Long): Flow<List<WeightEntry>>
    suspend fun logMeasurement(type: String, value: Double, unit: String)
    suspend fun deleteEntry(id: String)
    suspend fun updateEntryValue(id: String, value: Double)
    fun observeRecentMeasurements(sinceEpochMillis: Long): Flow<Map<String, List<BodyMeasurement>>>
    fun observeSettings(): Flow<AppSettings>
    suspend fun saveSettings(settings: AppSettings)
}

@OptIn(ExperimentalCoroutinesApi::class)
class LocalProfileRepository @Inject constructor(
    private val profileDao: ProfileDao,
    private val healthDao: HealthDao,
    private val accountRepository: AccountRepository,
) : ProfileRepository {
    private var clock: () -> Long = { System.currentTimeMillis() }

    internal constructor(
        profileDao: ProfileDao,
        healthDao: HealthDao,
        accountRepository: AccountRepository,
        clock: () -> Long,
    ) : this(profileDao, healthDao, accountRepository) {
        this.clock = clock
    }

    override fun observeProfile(): Flow<UserProfile> =
        accountRepository.observeActiveAccount().flatMapLatest { account ->
            profileDao.observeProfile(account.id).map { it?.toUserProfile() ?: DEFAULT_USER_PROFILE }
        }

    override suspend fun saveProfile(profile: UserProfile) {
        val account = accountRepository.ensureActiveAccount()
        profileDao.upsertProfile(profile.toEntity(id = account.id, now = clock()))
    }

    override fun observeRecommendedTargets(): Flow<RecommendedTargets?> =
        combine(observeProfile(), observeLatestWeight()) { profile, weight ->
            val sex = profile.sex ?: return@combine null
            val height = profile.heightCm ?: return@combine null
            val birth = profile.birthDateEpochDay ?: return@combine null
            val current = weight?.weightKg ?: return@combine null
            EnergyCalculator.recommendedTargets(
                sex = sex,
                weightKg = current,
                heightCm = height,
                ageYears = ageYears(birth),
                activityLevel = profile.activityLevel,
                goalType = profile.goalType,
                goalPaceKgPerWeek = profile.goalPaceKgPerWeek,
            )
        }

    override suspend fun logWeight(weightKg: Double, source: String) {
        require(weightKg.isFinite() && weightKg > 0.0) { "Weight must be positive" }
        healthDao.upsertBodyMetric(
            BodyMetricEntity(
                id = UUID.randomUUID().toString(),
                type = WEIGHT_METRIC_TYPE,
                value = weightKg,
                unit = "kg",
                measuredAtEpochMillis = clock(),
                source = source,
                externalId = null,
            ),
        )
    }

    override fun observeLatestWeight(): Flow<WeightEntry?> =
        healthDao.observeBodyMetrics(WEIGHT_METRIC_TYPE, 0L).map { rows ->
            rows.firstOrNull()?.let { WeightEntry(it.id, it.measuredAtEpochMillis, it.value, it.source) }
        }

    override fun observeWeightSeries(sinceEpochMillis: Long): Flow<List<WeightEntry>> =
        healthDao.observeBodyMetrics(WEIGHT_METRIC_TYPE, sinceEpochMillis).map { rows ->
            rows.map { WeightEntry(it.id, it.measuredAtEpochMillis, it.value, it.source) }
        }

    override suspend fun logMeasurement(type: String, value: Double, unit: String) {
        require(value.isFinite() && value > 0.0) { "Measurement must be positive" }
        healthDao.upsertBodyMetric(
            BodyMetricEntity(
                id = UUID.randomUUID().toString(),
                type = type,
                value = value,
                unit = unit,
                measuredAtEpochMillis = clock(),
                source = "manual",
                externalId = null,
            ),
        )
    }

    override suspend fun deleteEntry(id: String) {
        healthDao.deleteBodyMetric(id)
    }

    override suspend fun updateEntryValue(id: String, value: Double) {
        require(value.isFinite() && value > 0.0) { "Value must be positive" }
        healthDao.updateBodyMetricValue(id, value)
    }

    override fun observeRecentMeasurements(sinceEpochMillis: Long): Flow<Map<String, List<BodyMeasurement>>> {
        val typeFlows: List<Flow<Pair<String, List<BodyMeasurement>>>> = MEASUREMENT_TYPES.map { type ->
            healthDao.observeBodyMetrics(type, sinceEpochMillis).map { rows ->
                type to rows.map { BodyMeasurement(it.id, it.type, it.value, it.unit, it.measuredAtEpochMillis) }
            }
        }
        return combine(typeFlows) { pairs -> pairs.toMap() }
    }

    override fun observeSettings(): Flow<AppSettings> =
        accountRepository.observeActiveAccount().flatMapLatest { account ->
            profileDao.observeSettings(account.id).map { it?.toAppSettings() ?: DEFAULT_APP_SETTINGS }
        }

    override suspend fun saveSettings(settings: AppSettings) {
        val account = accountRepository.ensureActiveAccount()
        profileDao.upsertSettings(
            AppSettingsEntity(
                id = account.id,
                unitSystem = settings.unitSystem,
                themeMode = settings.themeMode,
                updatedAtEpochMillis = clock(),
            ),
        )
    }

    private fun ageYears(birthDateEpochDay: Long): Int {
        val birth = LocalDate.ofEpochDay(birthDateEpochDay)
        val today = LocalDate.ofEpochDay(clock() / 86_400_000L)
        return Period.between(birth, today).years.coerceAtLeast(0)
    }
}

private fun UserProfileEntity.toUserProfile() = UserProfile(
    sex = sex?.let { Sex.valueOf(it) },
    birthDateEpochDay = birthDateEpochDay,
    heightCm = heightCm,
    activityLevel = ActivityLevel.valueOf(activityLevel),
    goalType = GoalType.valueOf(goalType),
    goalPaceKgPerWeek = goalPaceKgPerWeek,
    goalWeightKg = goalWeightKg,
)

private fun UserProfile.toEntity(id: String, now: Long) = UserProfileEntity(
    id = id,
    sex = sex?.name,
    birthDateEpochDay = birthDateEpochDay,
    heightCm = heightCm,
    activityLevel = activityLevel.name,
    goalType = goalType.name,
    goalPaceKgPerWeek = goalPaceKgPerWeek,
    goalWeightKg = goalWeightKg,
    updatedAtEpochMillis = now,
)

private fun AppSettingsEntity.toAppSettings() = AppSettings(unitSystem = unitSystem, themeMode = themeMode)
