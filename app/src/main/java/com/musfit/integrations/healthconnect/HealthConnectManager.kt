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
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class HealthConnectManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : HealthConnectGateway {

    private val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
    )

    override suspend fun status(): HealthConnectStatus {
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

    override suspend fun requestablePermissions(): Set<String> = permissions

    override suspend fun readDailySummary(date: LocalDate): ImportedDailyHealthSummary {
        val client = HealthConnectClient.getOrCreate(context)
        val zoneId = ZoneId.systemDefault()
        val dayStart = date.atStartOfDay(zoneId).toInstant()
        val dayEnd = date.plusDays(1).atStartOfDay(zoneId).toInstant()
        val range = TimeRangeFilter.between(dayStart, dayEnd)

        val steps = runCatching {
            client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = range,
                ),
            )[StepsRecord.COUNT_TOTAL]
        }.getOrNull()

        val activeCalories = runCatching {
            client.aggregate(
                AggregateRequest(
                    metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                    timeRangeFilter = range,
                ),
            )[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories
        }.getOrNull()

        val latestWeight = runCatching {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = range,
                ),
            ).records.maxByOrNull { it.time }?.weight?.inKilograms
        }.getOrNull()

        val lowestHeartRate = runCatching {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = range,
                ),
            ).records
                .flatMap { it.samples }
                .minOfOrNull { it.beatsPerMinute }
        }.getOrNull()

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
        val response = HealthConnectClient.getOrCreate(context).insertRecords(
            listOf(HealthConnectRecordMapper.toExerciseSessionRecord(session, sets)),
        )
        return response.recordIdsList.firstOrNull()
    }

    private companion object {
        const val HEALTH_CONNECT_PROVIDER_PACKAGE = "com.google.android.apps.healthdata"
    }
}
