package com.musfit.integrations.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity
import com.musfit.domain.health.HealthConnectAvailability
import com.musfit.domain.health.HealthConnectStatus
import com.musfit.domain.health.ImportedBodyMetric
import com.musfit.domain.health.ImportedDailyHealthSummary
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

    private val permissions = setOf(
        READ_STEPS_PERMISSION,
        READ_ACTIVE_CALORIES_PERMISSION,
        READ_TOTAL_CALORIES_PERMISSION,
        READ_DISTANCE_PERMISSION,
        READ_SLEEP_PERMISSION,
        READ_EXERCISE_PERMISSION,
        READ_WEIGHT_PERMISSION,
        READ_BODY_FAT_PERMISSION,
        READ_RESTING_HEART_RATE_PERMISSION,
        WRITE_EXERCISE_PERMISSION,
    )
    private val foodPermissions = setOf(
        WRITE_NUTRITION_PERMISSION,
        WRITE_HYDRATION_PERMISSION,
    )

    override suspend fun status(): HealthConnectStatus = statusReader()

    override suspend fun requestablePermissions(): Set<String> = permissions

    override suspend fun foodRequestablePermissions(): Set<String> = foodPermissions

    override suspend fun readDailySummary(date: LocalDate): ImportedDailyHealthSummary {
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

        if (
            !canReadSteps &&
            !canReadActiveCalories &&
            !canReadTotalCalories &&
            !canReadDistance &&
            !canReadSleep &&
            !canReadExercise &&
            !canReadWeight &&
            !canReadBodyFat &&
            !canReadRestingHeartRate
        ) {
            return EMPTY_SUMMARY
        }

        val client = clientFactory()
        val range = date.asHealthConnectTimeRange()

        val steps = if (canReadSteps) {
            runCatching { client.aggregateSteps(range) }.getOrNull()
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
            bodyMetrics = listOfNotNull(latestWeight, latestBodyFat),
        )
    }

    override suspend fun exportWorkout(
        session: WorkoutSessionEntity,
        sets: List<WorkoutSetEntity>,
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
                HealthConnectRecordMapper.toExerciseSessionRecord(session, sets),
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

        val nutritionRecords = if (canWriteNutrition) {
            payload.meals
                .filter { meal -> meal.caloriesKcal > 0.0 || meal.proteinGrams > 0.0 || meal.carbsGrams > 0.0 || meal.fatGrams > 0.0 }
                .map { meal -> HealthConnectRecordMapper.toNutritionRecord(payload.date, meal) }
        } else {
            emptyList()
        }
        val hydrationRecord = if (canWriteHydration && payload.hydrationMilliliters > 0.0) {
            HealthConnectRecordMapper.toHydrationRecord(payload.date, payload.hydrationMilliliters)
        } else {
            null
        }

        if (nutritionRecords.isEmpty() && hydrationRecord == null) {
            return HealthConnectFoodExportResult(nutritionRecordCount = 0, hydrationRecordCount = 0)
        }

        return runCatching {
            val client = clientFactory()
            val nutritionCount = if (nutritionRecords.isNotEmpty()) {
                client.insertNutritionRecords(nutritionRecords)
            } else {
                0
            }
            val hydrationCount = hydrationRecord?.let { record ->
                if (client.insertHydrationRecord(record) != null) 1 else 0
            } ?: 0
            HealthConnectFoodExportResult(
                nutritionRecordCount = nutritionCount,
                hydrationRecordCount = hydrationCount,
            )
        }.getOrNull()
    }

    private companion object {
        const val HEALTH_CONNECT_PROVIDER_PACKAGE = "com.google.android.apps.healthdata"
        val READ_STEPS_PERMISSION = HealthPermission.getReadPermission(StepsRecord::class)
        val READ_WEIGHT_PERMISSION = HealthPermission.getReadPermission(WeightRecord::class)
        val READ_BODY_FAT_PERMISSION = HealthPermission.getReadPermission(BodyFatRecord::class)
        val READ_ACTIVE_CALORIES_PERMISSION =
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
        val READ_TOTAL_CALORIES_PERMISSION =
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
        val READ_DISTANCE_PERMISSION = HealthPermission.getReadPermission(DistanceRecord::class)
        val READ_SLEEP_PERMISSION = HealthPermission.getReadPermission(SleepSessionRecord::class)
        val READ_EXERCISE_PERMISSION = HealthPermission.getReadPermission(ExerciseSessionRecord::class)
        val READ_RESTING_HEART_RATE_PERMISSION =
            HealthPermission.getReadPermission(RestingHeartRateRecord::class)
        val WRITE_EXERCISE_PERMISSION =
            HealthPermission.getWritePermission(ExerciseSessionRecord::class)
        val WRITE_NUTRITION_PERMISSION =
            HealthPermission.getWritePermission(NutritionRecord::class)
        val WRITE_HYDRATION_PERMISSION =
            HealthPermission.getWritePermission(HydrationRecord::class)
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

internal data class HealthConnectTimeRange(
    val startTime: Instant,
    val endTime: Instant,
) {
    fun asTimeRangeFilter(): TimeRangeFilter = TimeRangeFilter.between(startTime, endTime)
}

internal interface HealthConnectClientAdapter {
    suspend fun aggregateSteps(range: HealthConnectTimeRange): Long?

    suspend fun aggregateActiveCalories(range: HealthConnectTimeRange): Double?

    suspend fun aggregateTotalCalories(range: HealthConnectTimeRange): Double?

    suspend fun aggregateDistanceMeters(range: HealthConnectTimeRange): Double?

    suspend fun aggregateSleepMinutes(range: HealthConnectTimeRange): Long?

    suspend fun aggregateExerciseMinutes(range: HealthConnectTimeRange): Long?

    suspend fun readExerciseSessionCount(range: HealthConnectTimeRange): Int?

    suspend fun readLatestWeightMetric(range: HealthConnectTimeRange): ImportedBodyMetric?

    suspend fun readLatestBodyFatMetric(range: HealthConnectTimeRange): ImportedBodyMetric?

    suspend fun readLatestRestingHeartRate(range: HealthConnectTimeRange): Long?

    suspend fun insertExerciseSession(record: ExerciseSessionRecord): String?

    suspend fun insertNutritionRecords(records: List<NutritionRecord>): Int

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

    override suspend fun aggregateActiveCalories(range: HealthConnectTimeRange): Double? =
        client.aggregate(
            AggregateRequest(
                metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                timeRangeFilter = range.asTimeRangeFilter(),
            ),
        )[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories

    override suspend fun aggregateTotalCalories(range: HealthConnectTimeRange): Double? =
        client.aggregate(
            AggregateRequest(
                metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                timeRangeFilter = range.asTimeRangeFilter(),
            ),
        )[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories

    override suspend fun aggregateDistanceMeters(range: HealthConnectTimeRange): Double? =
        client.aggregate(
            AggregateRequest(
                metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                timeRangeFilter = range.asTimeRangeFilter(),
            ),
        )[DistanceRecord.DISTANCE_TOTAL]?.inMeters

    override suspend fun aggregateSleepMinutes(range: HealthConnectTimeRange): Long? =
        client.aggregate(
            AggregateRequest(
                metrics = setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL),
                timeRangeFilter = range.asTimeRangeFilter(),
            ),
        )[SleepSessionRecord.SLEEP_DURATION_TOTAL]?.toMinutes()

    override suspend fun aggregateExerciseMinutes(range: HealthConnectTimeRange): Long? =
        client.aggregate(
            AggregateRequest(
                metrics = setOf(ExerciseSessionRecord.EXERCISE_DURATION_TOTAL),
                timeRangeFilter = range.asTimeRangeFilter(),
            ),
        )[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]?.toMinutes()

    override suspend fun readExerciseSessionCount(range: HealthConnectTimeRange): Int? =
        client.readRecords(
            ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = range.asTimeRangeFilter(),
            ),
        ).records.size

    override suspend fun readLatestWeightMetric(range: HealthConnectTimeRange): ImportedBodyMetric? =
        client.readRecords(
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

    override suspend fun readLatestBodyFatMetric(range: HealthConnectTimeRange): ImportedBodyMetric? =
        client.readRecords(
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

    override suspend fun readLatestRestingHeartRate(range: HealthConnectTimeRange): Long? =
        client.readRecords(
            ReadRecordsRequest(
                recordType = RestingHeartRateRecord::class,
                timeRangeFilter = range.asTimeRangeFilter(),
            ),
        ).records.maxByOrNull { it.time }?.beatsPerMinute

    override suspend fun insertExerciseSession(record: ExerciseSessionRecord): String? =
        client.insertRecords(listOf(record)).recordIdsList.firstOrNull()

    override suspend fun insertNutritionRecords(records: List<NutritionRecord>): Int =
        client.insertRecords(records).recordIdsList.size

    override suspend fun insertHydrationRecord(record: HydrationRecord): String? =
        client.insertRecords(listOf(record)).recordIdsList.firstOrNull()
}

private fun LocalDate.asHealthConnectTimeRange(zoneId: ZoneId = ZoneId.systemDefault()): HealthConnectTimeRange {
    val dayStart = atStartOfDay(zoneId).toInstant()
    val dayEnd = plusDays(1).atStartOfDay(zoneId).toInstant()
    return HealthConnectTimeRange(startTime = dayStart, endTime = dayEnd)
}
