package com.musfit.integrations.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity
import com.musfit.domain.health.HealthConnectAvailability
import com.musfit.domain.health.HealthConnectStatus
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
        READ_WEIGHT_PERMISSION,
        READ_ACTIVE_CALORIES_PERMISSION,
        READ_HEART_RATE_PERMISSION,
        WRITE_EXERCISE_PERMISSION,
    )

    override suspend fun status(): HealthConnectStatus = statusReader()

    override suspend fun requestablePermissions(): Set<String> = permissions

    override suspend fun readDailySummary(date: LocalDate): ImportedDailyHealthSummary {
        val currentStatus = status()
        if (currentStatus.availability != HealthConnectAvailability.Available) {
            return EMPTY_SUMMARY
        }

        val grantedPermissions = currentStatus.grantedPermissions
        val canReadSteps = READ_STEPS_PERMISSION in grantedPermissions
        val canReadActiveCalories = READ_ACTIVE_CALORIES_PERMISSION in grantedPermissions
        val canReadWeight = READ_WEIGHT_PERMISSION in grantedPermissions
        val canReadHeartRate = READ_HEART_RATE_PERMISSION in grantedPermissions

        if (!canReadSteps && !canReadActiveCalories && !canReadWeight && !canReadHeartRate) {
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

        val latestWeight = if (canReadWeight) {
            runCatching { client.readLatestWeight(range) }.getOrNull()
        } else {
            null
        }

        val lowestHeartRate = if (canReadHeartRate) {
            runCatching { client.readLowestHeartRate(range) }.getOrNull()
        } else {
            null
        }

        return ImportedDailyHealthSummary(
            steps = steps,
            activeCaloriesKcal = activeCalories,
            latestWeightKg = latestWeight,
            restingHeartRateBpm = lowestHeartRate,
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

    private companion object {
        const val HEALTH_CONNECT_PROVIDER_PACKAGE = "com.google.android.apps.healthdata"
        val READ_STEPS_PERMISSION = HealthPermission.getReadPermission(StepsRecord::class)
        val READ_WEIGHT_PERMISSION = HealthPermission.getReadPermission(WeightRecord::class)
        val READ_ACTIVE_CALORIES_PERMISSION =
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
        val READ_HEART_RATE_PERMISSION = HealthPermission.getReadPermission(HeartRateRecord::class)
        val WRITE_EXERCISE_PERMISSION =
            HealthPermission.getWritePermission(ExerciseSessionRecord::class)
        val EMPTY_SUMMARY = ImportedDailyHealthSummary(
            steps = null,
            activeCaloriesKcal = null,
            latestWeightKg = null,
            restingHeartRateBpm = null,
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

    suspend fun readLatestWeight(range: HealthConnectTimeRange): Double?

    suspend fun readLowestHeartRate(range: HealthConnectTimeRange): Long?

    suspend fun insertExerciseSession(record: ExerciseSessionRecord): String?
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

    override suspend fun readLatestWeight(range: HealthConnectTimeRange): Double? =
        client.readRecords(
            ReadRecordsRequest(
                recordType = WeightRecord::class,
                timeRangeFilter = range.asTimeRangeFilter(),
            ),
        ).records.maxByOrNull { it.time }?.weight?.inKilograms

    override suspend fun readLowestHeartRate(range: HealthConnectTimeRange): Long? =
        client.readRecords(
            ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = range.asTimeRangeFilter(),
            ),
        ).records
            .flatMap { it.samples }
            .minOfOrNull { it.beatsPerMinute }

    override suspend fun insertExerciseSession(record: ExerciseSessionRecord): String? =
        client.insertRecords(listOf(record)).recordIdsList.firstOrNull()
}

private fun LocalDate.asHealthConnectTimeRange(zoneId: ZoneId = ZoneId.systemDefault()): HealthConnectTimeRange {
    val dayStart = atStartOfDay(zoneId).toInstant()
    val dayEnd = plusDays(1).atStartOfDay(zoneId).toInstant()
    return HealthConnectTimeRange(startTime = dayStart, endTime = dayEnd)
}
