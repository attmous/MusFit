package com.musfit.data.repository

import com.musfit.data.local.dao.HealthDao
import com.musfit.data.local.dao.TrainingDao
import com.musfit.data.local.entity.BodyMetricEntity
import com.musfit.data.local.entity.DailyHealthSummaryEntity
import com.musfit.data.local.entity.HealthConnectSyncStateEntity
import com.musfit.domain.health.HealthConnectAvailability
import com.musfit.domain.health.HealthConnectStatus
import com.musfit.domain.health.ImportedBodyMetric
import com.musfit.domain.health.ImportedDailyHealthSummary
import com.musfit.integrations.healthconnect.HealthConnectGateway
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import javax.inject.Inject

data class HealthConnectRefreshResult(
    val importedDayCount: Int,
    val bodyMetricCount: Int,
)

interface HealthRepository {
    suspend fun status(): HealthConnectStatus

    suspend fun requestablePermissions(): Set<String>

    fun observeDailySummary(date: LocalDate): Flow<DailyHealthSummaryEntity?>

    suspend fun importDailySummary(date: LocalDate): ImportedDailyHealthSummary

    suspend fun refreshRecentData(endDate: LocalDate, days: Int = 7): HealthConnectRefreshResult =
        HealthConnectRefreshResult(importedDayCount = 0, bodyMetricCount = 0)

    suspend fun exportLatestWorkout(): String?

    fun observeDailySummaries(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyHealthSummaryEntity>> =
        flowOf(emptyList())

    fun observeWeightSeries(fromEpochMillis: Long): Flow<List<BodyMetricEntity>> =
        flowOf(emptyList())
}

class LocalHealthRepository @Inject constructor(
    private val gateway: HealthConnectGateway,
    private val healthDao: HealthDao,
    private val trainingDao: TrainingDao,
) : HealthRepository {
    private var clock: () -> Long = { System.currentTimeMillis() }

    internal constructor(
        gateway: HealthConnectGateway,
        healthDao: HealthDao,
        trainingDao: TrainingDao,
        clock: () -> Long,
    ) : this(gateway, healthDao, trainingDao) {
        this.clock = clock
    }

    override suspend fun status(): HealthConnectStatus = gateway.status()

    override suspend fun requestablePermissions(): Set<String> = gateway.requestablePermissions()

    override fun observeDailySummary(date: LocalDate): Flow<DailyHealthSummaryEntity?> =
        healthDao.observeDailySummary(date.toEpochDay())

    override fun observeDailySummaries(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyHealthSummaryEntity>> =
        healthDao.observeDailySummariesInRange(startDate.toEpochDay(), endDate.toEpochDay())

    override fun observeWeightSeries(fromEpochMillis: Long): Flow<List<BodyMetricEntity>> =
        healthDao.observeBodyMetrics(WEIGHT_METRIC_TYPE, fromEpochMillis)

    override suspend fun importDailySummary(date: LocalDate): ImportedDailyHealthSummary {
        val summary = gateway.readDailySummary(date)
        val now = clock()
        val bodyMetrics = summary.bodyMetrics
        healthDao.upsertDailySummary(
            DailyHealthSummaryEntity(
                dateEpochDay = date.toEpochDay(),
                steps = summary.steps,
                activeCaloriesKcal = summary.activeCaloriesKcal,
                totalCaloriesKcal = summary.totalCaloriesKcal,
                distanceMeters = summary.distanceMeters,
                sleepMinutes = summary.sleepMinutes,
                exerciseMinutes = summary.exerciseMinutes,
                exerciseSessionCount = summary.exerciseSessionCount,
                latestWeightKg = summary.latestWeightKg,
                latestBodyFatPercent = summary.latestBodyFatPercent,
                restingHeartRateBpm = summary.restingHeartRateBpm,
                updatedAtEpochMillis = now,
            ),
        )
        bodyMetrics.forEach { metric ->
            healthDao.upsertBodyMetric(metric.toBodyMetricEntity())
        }
        upsertSyncState(
            lastImportAtEpochMillis = now,
            lastExportAtEpochMillis = null,
            lastFailureMessage = null,
        )
        return summary
    }

    override suspend fun refreshRecentData(endDate: LocalDate, days: Int): HealthConnectRefreshResult {
        val safeDays = days.coerceAtLeast(1)
        var bodyMetricCount = 0
        for (offset in safeDays - 1 downTo 0) {
            bodyMetricCount += importDailySummary(endDate.minusDays(offset.toLong())).bodyMetrics.size
        }
        return HealthConnectRefreshResult(
            importedDayCount = safeDays,
            bodyMetricCount = bodyMetricCount,
        )
    }

    override suspend fun exportLatestWorkout(): String? {
        val session = trainingDao.getLatestCompletedWorkoutSession() ?: return null
        val sets = trainingDao.getCompletedWorkoutSets(session.id)
        if (sets.isEmpty()) return null

        val recordId = gateway.exportWorkout(session, sets) ?: return null
        val now = clock()
        val updatedSession =
            session.copy(
                healthConnectRecordId = recordId,
                healthConnectLastExportedAtEpochMillis = now,
            )
        val inserted = trainingDao.insertWorkoutSession(updatedSession)
        if (inserted == -1L) {
            trainingDao.updateWorkoutSession(updatedSession)
        }
        upsertSyncState(
            lastImportAtEpochMillis = null,
            lastExportAtEpochMillis = now,
            lastFailureMessage = null,
        )
        return recordId
    }

    private suspend fun upsertSyncState(
        lastImportAtEpochMillis: Long?,
        lastExportAtEpochMillis: Long?,
        lastFailureMessage: String?,
    ) {
        val existing = healthDao.getHealthConnectSyncState()
        val currentStatus = runCatching { gateway.status() }.getOrNull()
        val grantedPermissions = currentStatus?.grantedPermissions.orEmpty()
        healthDao.upsertHealthConnectSyncState(
            HealthConnectSyncStateEntity(
                key = SYNC_STATE_KEY,
                isAvailable = currentStatus?.availability == HealthConnectAvailability.Available,
                grantedPermissionsCsv = grantedPermissions.sorted().joinToString(","),
                lastImportAtEpochMillis = lastImportAtEpochMillis ?: existing?.lastImportAtEpochMillis,
                lastExportAtEpochMillis = lastExportAtEpochMillis ?: existing?.lastExportAtEpochMillis,
                lastFailureMessage = lastFailureMessage,
            ),
        )
    }

    private companion object {
        const val SYNC_STATE_KEY = "health_connect"
        const val WEIGHT_METRIC_TYPE = "weight"
    }
}

private fun ImportedBodyMetric.toBodyMetricEntity(): BodyMetricEntity {
    val externalToken = externalId?.takeIf { it.isNotBlank() } ?: measuredAtEpochMillis.toString()
    return BodyMetricEntity(
        id = "health-connect-$type-$externalToken",
        type = type,
        value = value,
        unit = unit,
        measuredAtEpochMillis = measuredAtEpochMillis,
        source = "health_connect",
        externalId = externalId,
    )
}
