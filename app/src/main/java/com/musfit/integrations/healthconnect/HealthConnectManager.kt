package com.musfit.integrations.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity
import com.musfit.domain.health.HealthConnectAvailability
import com.musfit.domain.health.HealthConnectStatus
import com.musfit.domain.health.ImportedBodyMetric
import com.musfit.domain.health.ImportedDailyHealthSummary
import com.musfit.domain.health.StepSource
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class HealthConnectManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : HealthConnectGateway {
    private var statusReader: suspend () -> HealthConnectStatus = { readStatus(context) }
    private var clientFactory: () -> HealthConnectClientAdapter = {
        DefaultHealthConnectClientAdapter(HealthConnectClient.getOrCreate(context))
    }

    internal constructor(
        context: Context,
        statusReader: suspend () -> HealthConnectStatus,
        clientFactory: () -> HealthConnectClientAdapter,
    ) : this(context) {
        this.statusReader = statusReader
        this.clientFactory = clientFactory
    }

    private val permissions = HealthPermissionInventory.healthAndWorkoutPermissions
    private val foodPermissions = HealthPermissionInventory.foodPermissions

    override suspend fun status(): HealthConnectStatus = statusReader()

    override suspend fun requestablePermissions(): Set<String> = permissions

    override suspend fun foodRequestablePermissions(): Set<String> = foodPermissions

    override suspend fun readDailySummary(
        date: LocalDate,
        preferredStepsPackage: String?,
    ): ImportedDailyHealthSummary {
        val currentStatus = status()
        if (currentStatus.availability != HealthConnectAvailability.Available) {
            return EMPTY_SUMMARY
        }

        val grantedPermissions = currentStatus.grantedPermissions
        val canReadSteps = READ_STEPS_PERMISSION in grantedPermissions
        val canReadActiveCalories = READ_ACTIVE_CALORIES_PERMISSION in grantedPermissions
        val canReadTotalCalories = READ_TOTAL_CALORIES_PERMISSION in grantedPermissions
        val canReadDistance = READ_DISTANCE_PERMISSION in grantedPermissions
        val canReadSleep = READ_SLEEP_PERMISSION in grantedPermissions
        val canReadExercise = READ_EXERCISE_PERMISSION in grantedPermissions
        val canReadWeight = READ_WEIGHT_PERMISSION in grantedPermissions
        val canReadBodyFat = READ_BODY_FAT_PERMISSION in grantedPermissions
        val canReadRestingHeartRate = READ_RESTING_HEART_RATE_PERMISSION in grantedPermissions
        val canReadHeartRateVariability = READ_HEART_RATE_VARIABILITY_PERMISSION in grantedPermissions

        if (
            !canReadSteps &&
            !canReadActiveCalories &&
            !canReadTotalCalories &&
            !canReadDistance &&
            !canReadSleep &&
            !canReadExercise &&
            !canReadWeight &&
            !canReadBodyFat &&
            !canReadRestingHeartRate &&
            !canReadHeartRateVariability
        ) {
            return EMPTY_SUMMARY
        }

        val client = clientFactory()
        val range = date.asHealthConnectTimeRange()

        val steps = if (canReadSteps) {
            runCatching {
                if (preferredStepsPackage != null) {
                    client.aggregateStepsForOrigins(range, setOf(preferredStepsPackage)) ?: 0L
                } else {
                    client.aggregateSteps(range) ?: 0L
                }
            }.getOrNull()
        } else {
            null
        }

        val activeCalories = if (canReadActiveCalories) {
            runCatching { client.aggregateActiveCalories(range) }.getOrNull()
        } else {
            null
        }

        val totalCalories = if (canReadTotalCalories) {
            runCatching { client.aggregateTotalCalories(range) }.getOrNull()
        } else {
            null
        }

        val distance = if (canReadDistance) {
            runCatching { client.aggregateDistanceMeters(range) }.getOrNull()
        } else {
            null
        }

        val sleepMinutes = if (canReadSleep) {
            runCatching { client.aggregateSleepMinutes(range) }.getOrNull()
        } else {
            null
        }

        val exerciseMinutes = if (canReadExercise) {
            runCatching { client.aggregateExerciseMinutes(range) }.getOrNull()
        } else {
            null
        }

        val exerciseSessionCount = if (canReadExercise) {
            runCatching { client.readExerciseSessionCount(range) }.getOrNull()
        } else {
            null
        }

        val latestWeight = if (canReadWeight) {
            runCatching { client.readLatestWeightMetric(range) }.getOrNull()
        } else {
            null
        }

        val latestBodyFat = if (canReadBodyFat) {
            runCatching { client.readLatestBodyFatMetric(range) }.getOrNull()
        } else {
            null
        }

        val restingHeartRate = if (canReadRestingHeartRate) {
            runCatching { client.readLatestRestingHeartRate(range) }.getOrNull()
        } else {
            null
        }

        val hrvRmssd = if (canReadHeartRateVariability) {
            runCatching { client.readLatestHeartRateVariabilityRmssdMillis(range) }.getOrNull()
        } else {
            null
        }

        return ImportedDailyHealthSummary(
            steps = steps,
            activeCaloriesKcal = activeCalories,
            totalCaloriesKcal = totalCalories,
            distanceMeters = distance,
            sleepMinutes = sleepMinutes,
            exerciseMinutes = exerciseMinutes,
            exerciseSessionCount = exerciseSessionCount,
            latestWeightKg = latestWeight?.value,
            latestBodyFatPercent = latestBodyFat?.value,
            restingHeartRateBpm = restingHeartRate,
            hrvRmssdMillis = hrvRmssd,
            bodyMetrics = listOfNotNull(latestWeight, latestBodyFat),
        )
    }

    override suspend fun readStepSources(date: LocalDate): List<StepSource> {
        val currentStatus = status()
        if (currentStatus.availability != HealthConnectAvailability.Available) {
            return emptyList()
        }
        if (READ_STEPS_PERMISSION !in currentStatus.grantedPermissions) {
            return emptyList()
        }

        val client = clientFactory()
        val range = date.asHealthConnectTimeRange()
        val byOrigin = runCatching { client.readStepCountsByOrigin(range) }.getOrNull().orEmpty()
        return byOrigin
            .map { (packageName, steps) ->
                StepSource(
                    packageName = packageName,
                    label = labelForStepSource(packageName),
                    steps = steps,
                )
            }
            .sortedByDescending { it.steps }
    }

    private fun labelForStepSource(packageName: String): String = when {
        packageName == ON_DEVICE_STEPS_PACKAGE ||
            packageName.startsWith(ON_DEVICE_SPN_PREFIX) -> "Your phone"

        else -> runCatching {
            val packageManager = context.packageManager
            packageManager
                .getApplicationLabel(packageManager.getApplicationInfo(packageName, 0))
                .toString()
        }.getOrNull()?.takeIf { it.isNotBlank() && it != packageName } ?: packageName
    }

    override suspend fun exportWorkout(
        session: WorkoutSessionEntity,
        sets: List<WorkoutSetEntity>,
    ): String? = exportWorkout(
        session = session,
        sets = sets,
        identity = HealthConnectRecordIdentity.forWorkout(session.accountId, session.id, version = 1),
    )

    override suspend fun exportWorkout(
        session: WorkoutSessionEntity,
        sets: List<WorkoutSetEntity>,
        identity: HealthConnectRecordIdentity,
    ): String? {
        val currentStatus = status()
        if (
            currentStatus.availability != HealthConnectAvailability.Available ||
            WRITE_EXERCISE_PERMISSION !in currentStatus.grantedPermissions
        ) {
            return null
        }

        return runCatching {
            clientFactory().insertExerciseSession(
                HealthConnectRecordMapper.toExerciseSessionRecord(session, sets, identity = identity),
            )
        }.getOrNull()
    }

    override suspend fun exportFood(payload: HealthConnectFoodExportPayload): HealthConnectFoodExportResult? {
        val currentStatus = status()
        if (currentStatus.availability != HealthConnectAvailability.Available) {
            return null
        }

        val canWriteNutrition = WRITE_NUTRITION_PERMISSION in currentStatus.grantedPermissions
        val canWriteHydration = WRITE_HYDRATION_PERMISSION in currentStatus.grantedPermissions
        if (!canWriteNutrition && !canWriteHydration) {
            return null
        }

        val nutritionExports = payload.writeableNutritionExports(canWriteNutrition)
        val nutritionRecords = payload.toNutritionRecords(nutritionExports)
        val hydrationRecord = payload.toHydrationRecord(canWriteHydration)

        if (nutritionRecords.isEmpty() && hydrationRecord == null) {
            return HealthConnectFoodExportResult(nutritionRecordCount = 0, hydrationRecordCount = 0)
        }

        return runCatching { insertFoodRecords(nutritionExports, nutritionRecords, hydrationRecord) }.getOrNull()
    }

    private suspend fun insertFoodRecords(
        nutritionExports: List<HealthConnectFoodMealExport>,
        nutritionRecords: List<NutritionRecord>,
        hydrationRecord: HydrationRecord?,
    ): HealthConnectFoodExportResult {
        val client = clientFactory()
        val nutritionProviderIds = if (nutritionRecords.isEmpty()) {
            emptyList()
        } else {
            client.insertNutritionRecords(nutritionRecords)
        }
        val hydrationProviderId = hydrationRecord?.let { record -> client.insertHydrationRecord(record) }
        return HealthConnectFoodExportResult(
            nutritionRecordCount = nutritionProviderIds.size,
            hydrationRecordCount = if (hydrationProviderId != null) 1 else 0,
            nutritionProviderRecordIds = nutritionExports
                .map { meal -> meal.localMealId }
                .zip(nutritionProviderIds)
                .toMap(),
            hydrationProviderRecordId = hydrationProviderId,
        )
    }

    private companion object {
        const val HEALTH_CONNECT_PROVIDER_PACKAGE = "com.google.android.apps.healthdata"

        // On-device steps: "android" historically, a per-device Synthetic Package Name from the
        // June 2026 Health Connect update onwards (e.g. "com.android.healthconnect.phone.<hash>").
        const val ON_DEVICE_STEPS_PACKAGE = "android"
        const val ON_DEVICE_SPN_PREFIX = "com.android.healthconnect.phone."
        val READ_STEPS_PERMISSION = HealthPermissionInventory.readStepsPermission
        val READ_WEIGHT_PERMISSION = HealthPermissionInventory.readWeightPermission
        val READ_BODY_FAT_PERMISSION = HealthPermissionInventory.readBodyFatPermission
        val READ_ACTIVE_CALORIES_PERMISSION = HealthPermissionInventory.readActiveCaloriesPermission
        val READ_TOTAL_CALORIES_PERMISSION = HealthPermissionInventory.readTotalCaloriesPermission
        val READ_DISTANCE_PERMISSION = HealthPermissionInventory.readDistancePermission
        val READ_SLEEP_PERMISSION = HealthPermissionInventory.readSleepPermission
        val READ_EXERCISE_PERMISSION = HealthPermissionInventory.readExercisePermission
        val READ_RESTING_HEART_RATE_PERMISSION = HealthPermissionInventory.readRestingHeartRatePermission
        val READ_HEART_RATE_VARIABILITY_PERMISSION =
            HealthPermissionInventory.readHeartRateVariabilityPermission
        val WRITE_EXERCISE_PERMISSION = HealthPermissionInventory.writeExercisePermission
        val WRITE_NUTRITION_PERMISSION = HealthPermissionInventory.writeNutritionPermission
        val WRITE_HYDRATION_PERMISSION = HealthPermissionInventory.writeHydrationPermission
        val EMPTY_SUMMARY = ImportedDailyHealthSummary(
            steps = null,
            activeCaloriesKcal = null,
            totalCaloriesKcal = null,
            distanceMeters = null,
            sleepMinutes = null,
            exerciseMinutes = null,
            exerciseSessionCount = null,
            latestWeightKg = null,
            latestBodyFatPercent = null,
            restingHeartRateBpm = null,
            hrvRmssdMillis = null,
            bodyMetrics = emptyList(),
        )

        suspend fun readStatus(context: Context): HealthConnectStatus {
            val availability = when (
                HealthConnectClient.getSdkStatus(
                    context = context,
                    providerPackageName = HEALTH_CONNECT_PROVIDER_PACKAGE,
                )
            ) {
                HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.Available

                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                    HealthConnectAvailability.NotInstalled

                else -> HealthConnectAvailability.NotSupported
            }

            val grantedPermissions = if (availability == HealthConnectAvailability.Available) {
                HealthConnectClient
                    .getOrCreate(context)
                    .permissionController
                    .getGrantedPermissions()
            } else {
                emptySet()
            }

            return HealthConnectStatus(
                availability = availability,
                grantedPermissions = grantedPermissions,
            )
        }
    }
}

private fun HealthConnectFoodExportPayload.writeableNutritionExports(
    canWriteNutrition: Boolean,
): List<HealthConnectFoodMealExport> = if (canWriteNutrition) meals.filter { it.hasMacroNutrition() } else emptyList()

private fun HealthConnectFoodExportPayload.toNutritionRecords(
    exports: List<HealthConnectFoodMealExport>,
): List<NutritionRecord> = exports.map { meal ->
    HealthConnectRecordMapper.toNutritionRecord(
        date = date,
        meal = meal,
        identity = HealthConnectRecordIdentity.forNutrition(
            accountId = accountId,
            mealId = meal.localMealId,
            version = meal.clientRecordVersion,
        ),
    )
}

private fun HealthConnectFoodExportPayload.toHydrationRecord(canWriteHydration: Boolean): HydrationRecord? {
    if (!canWriteHydration || hydrationMilliliters <= 0.0) return null
    return HealthConnectRecordMapper.toHydrationRecord(
        date = date,
        milliliters = hydrationMilliliters,
        accountId = accountId,
        identity = HealthConnectRecordIdentity.forHydration(
            accountId = accountId,
            date = date,
            version = hydrationClientRecordVersion,
        ),
    )
}

private fun HealthConnectFoodMealExport.hasMacroNutrition(): Boolean = caloriesKcal > 0.0 || proteinGrams > 0.0 || carbsGrams > 0.0 || fatGrams > 0.0

internal data class HealthConnectTimeRange(
    val startTime: Instant,
    val endTime: Instant,
) {
    fun asTimeRangeFilter(): TimeRangeFilter = TimeRangeFilter.between(startTime, endTime)
}

internal interface HealthConnectClientAdapter {
    suspend fun aggregateSteps(range: HealthConnectTimeRange): Long?

    suspend fun aggregateStepsForOrigins(
        range: HealthConnectTimeRange,
        packageNames: Set<String>,
    ): Long?

    suspend fun readStepCountsByOrigin(range: HealthConnectTimeRange): Map<String, Long>

    suspend fun aggregateActiveCalories(range: HealthConnectTimeRange): Double?

    suspend fun aggregateTotalCalories(range: HealthConnectTimeRange): Double?

    suspend fun aggregateDistanceMeters(range: HealthConnectTimeRange): Double?

    suspend fun aggregateSleepMinutes(range: HealthConnectTimeRange): Long?

    suspend fun aggregateExerciseMinutes(range: HealthConnectTimeRange): Long?

    suspend fun readExerciseSessionCount(range: HealthConnectTimeRange): Int?

    suspend fun readLatestWeightMetric(range: HealthConnectTimeRange): ImportedBodyMetric?

    suspend fun readLatestBodyFatMetric(range: HealthConnectTimeRange): ImportedBodyMetric?

    suspend fun readLatestRestingHeartRate(range: HealthConnectTimeRange): Long?

    suspend fun readLatestHeartRateVariabilityRmssdMillis(range: HealthConnectTimeRange): Double?

    suspend fun insertExerciseSession(record: ExerciseSessionRecord): String?

    suspend fun insertNutritionRecords(records: List<NutritionRecord>): List<String>

    suspend fun insertHydrationRecord(record: HydrationRecord): String?
}

private class DefaultHealthConnectClientAdapter(
    private val client: HealthConnectClient,
) : HealthConnectClientAdapter {
    override suspend fun aggregateSteps(range: HealthConnectTimeRange): Long? = client.aggregate(
        AggregateRequest(
            metrics = setOf(StepsRecord.COUNT_TOTAL),
            timeRangeFilter = range.asTimeRangeFilter(),
        ),
    )[StepsRecord.COUNT_TOTAL]

    override suspend fun aggregateStepsForOrigins(
        range: HealthConnectTimeRange,
        packageNames: Set<String>,
    ): Long? = client.aggregate(
        AggregateRequest(
            metrics = setOf(StepsRecord.COUNT_TOTAL),
            timeRangeFilter = range.asTimeRangeFilter(),
            dataOriginFilter = packageNames.map { DataOrigin(it) }.toSet(),
        ),
    )[StepsRecord.COUNT_TOTAL]

    override suspend fun readStepCountsByOrigin(range: HealthConnectTimeRange): Map<String, Long> = client.readRecords(
        ReadRecordsRequest(
            recordType = StepsRecord::class,
            timeRangeFilter = range.asTimeRangeFilter(),
        ),
    ).records
        .groupBy { it.metadata.dataOrigin.packageName }
        .mapValues { (_, records) -> records.sumOf { it.count } }

    override suspend fun aggregateActiveCalories(range: HealthConnectTimeRange): Double? = client.aggregate(
        AggregateRequest(
            metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
            timeRangeFilter = range.asTimeRangeFilter(),
        ),
    )[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories

    override suspend fun aggregateTotalCalories(range: HealthConnectTimeRange): Double? = client.aggregate(
        AggregateRequest(
            metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
            timeRangeFilter = range.asTimeRangeFilter(),
        ),
    )[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories

    override suspend fun aggregateDistanceMeters(range: HealthConnectTimeRange): Double? = client.aggregate(
        AggregateRequest(
            metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
            timeRangeFilter = range.asTimeRangeFilter(),
        ),
    )[DistanceRecord.DISTANCE_TOTAL]?.inMeters

    override suspend fun aggregateSleepMinutes(range: HealthConnectTimeRange): Long? = client.aggregate(
        AggregateRequest(
            metrics = setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL),
            timeRangeFilter = range.asTimeRangeFilter(),
        ),
    )[SleepSessionRecord.SLEEP_DURATION_TOTAL]?.toMinutes()

    override suspend fun aggregateExerciseMinutes(range: HealthConnectTimeRange): Long? = client.aggregate(
        AggregateRequest(
            metrics = setOf(ExerciseSessionRecord.EXERCISE_DURATION_TOTAL),
            timeRangeFilter = range.asTimeRangeFilter(),
        ),
    )[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]?.toMinutes()

    override suspend fun readExerciseSessionCount(range: HealthConnectTimeRange): Int? = client.readRecords(
        ReadRecordsRequest(
            recordType = ExerciseSessionRecord::class,
            timeRangeFilter = range.asTimeRangeFilter(),
        ),
    ).records.size

    override suspend fun readLatestWeightMetric(range: HealthConnectTimeRange): ImportedBodyMetric? = client.readRecords(
        ReadRecordsRequest(
            recordType = WeightRecord::class,
            timeRangeFilter = range.asTimeRangeFilter(),
        ),
    ).records.maxByOrNull { it.time }?.let { record ->
        ImportedBodyMetric(
            type = "weight",
            value = record.weight.inKilograms,
            unit = "kg",
            measuredAtEpochMillis = record.time.toEpochMilli(),
            externalId = record.metadata.id.takeIf { it.isNotBlank() },
        )
    }

    override suspend fun readLatestBodyFatMetric(range: HealthConnectTimeRange): ImportedBodyMetric? = client.readRecords(
        ReadRecordsRequest(
            recordType = BodyFatRecord::class,
            timeRangeFilter = range.asTimeRangeFilter(),
        ),
    ).records.maxByOrNull { it.time }?.let { record ->
        ImportedBodyMetric(
            type = "body_fat",
            value = record.percentage.value,
            unit = "%",
            measuredAtEpochMillis = record.time.toEpochMilli(),
            externalId = record.metadata.id.takeIf { it.isNotBlank() },
        )
    }

    override suspend fun readLatestRestingHeartRate(range: HealthConnectTimeRange): Long? = client.readRecords(
        ReadRecordsRequest(
            recordType = RestingHeartRateRecord::class,
            timeRangeFilter = range.asTimeRangeFilter(),
        ),
    ).records.maxByOrNull { it.time }?.beatsPerMinute

    override suspend fun readLatestHeartRateVariabilityRmssdMillis(range: HealthConnectTimeRange): Double? = client.readRecords(
        ReadRecordsRequest(
            recordType = HeartRateVariabilityRmssdRecord::class,
            timeRangeFilter = range.asTimeRangeFilter(),
        ),
    ).records.maxByOrNull { it.time }?.heartRateVariabilityMillis

    override suspend fun insertExerciseSession(record: ExerciseSessionRecord): String? = client.insertRecords(listOf(record)).recordIdsList.firstOrNull()

    override suspend fun insertNutritionRecords(records: List<NutritionRecord>): List<String> = client.insertRecords(records).recordIdsList

    override suspend fun insertHydrationRecord(record: HydrationRecord): String? = client.insertRecords(listOf(record)).recordIdsList.firstOrNull()
}

private fun LocalDate.asHealthConnectTimeRange(zoneId: ZoneId = ZoneId.systemDefault()): HealthConnectTimeRange {
    val dayStart = atStartOfDay(zoneId).toInstant()
    val dayEnd = plusDays(1).atStartOfDay(zoneId).toInstant()
    return HealthConnectTimeRange(startTime = dayStart, endTime = dayEnd)
}
