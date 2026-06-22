package com.musfit.data.repository

import com.musfit.data.local.dao.HealthDao
import com.musfit.data.local.dao.TrainingDao
import com.musfit.data.local.entity.DailyHealthSummaryEntity
import com.musfit.data.local.entity.HealthConnectSyncStateEntity
import com.musfit.domain.health.HealthConnectAvailability
import com.musfit.domain.health.HealthConnectStatus
import com.musfit.domain.health.ImportedDailyHealthSummary
import com.musfit.integrations.healthconnect.HealthConnectGateway
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

interface HealthRepository {
    suspend fun status(): HealthConnectStatus

    suspend fun requestablePermissions(): Set<String>

    fun observeDailySummary(date: LocalDate): Flow<DailyHealthSummaryEntity?>

    suspend fun importDailySummary(date: LocalDate): ImportedDailyHealthSummary

    suspend fun exportLatestWorkout(): String?
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

    override suspend fun importDailySummary(date: LocalDate): ImportedDailyHealthSummary {
        val summary = gateway.readDailySummary(date)
        val now = clock()
        healthDao.upsertDailySummary(
            DailyHealthSummaryEntity(
                dateEpochDay = date.toEpochDay(),
                steps = summary.steps,
                activeCaloriesKcal = summary.activeCaloriesKcal,
                latestWeightKg = summary.latestWeightKg,
                restingHeartRateBpm = summary.restingHeartRateBpm,
                updatedAtEpochMillis = now,
            ),
        )
        upsertSyncState(
            lastImportAtEpochMillis = now,
            lastExportAtEpochMillis = null,
            lastFailureMessage = null,
        )
        return summary
    }

    override suspend fun exportLatestWorkout(): String? {
        val session = trainingDao.getLatestCompletedWorkoutSession() ?: return null
        val sets = trainingDao.getWorkoutSets(session.id)
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
    }
}
