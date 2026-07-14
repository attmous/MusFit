package com.musfit.data.repository

import com.musfit.data.local.dao.HealthDao
import com.musfit.data.local.dao.TrainingDao
import com.musfit.data.local.entity.BodyMetricEntity
import com.musfit.data.local.entity.DailyHealthSummaryEntity
import com.musfit.data.local.entity.HealthConnectExportRecordEntity
import com.musfit.data.local.entity.HealthConnectSyncStateEntity
import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.domain.health.HealthConnectAvailability
import com.musfit.domain.health.HealthConnectStatus
import com.musfit.domain.health.ImportedBodyMetric
import com.musfit.domain.health.ImportedDailyHealthSummary
import com.musfit.domain.health.StepSource
import com.musfit.integrations.healthconnect.HealthConnectGateway
import com.musfit.integrations.healthconnect.HealthConnectRecordIdentity
import com.musfit.integrations.healthconnect.workoutExportFingerprint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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

    /** Per-source step totals for [date], so the user can pick which source MusFit mirrors. */
    suspend fun readStepSources(date: LocalDate): List<StepSource> = emptyList()

    /** The data-origin package MusFit mirrors for steps, or null for the unified total. */
    fun observePreferredStepsPackage(): Flow<String?> = flowOf(null)

    /** Pins the steps source to [packageName] (null restores the unified cross-source total). */
    suspend fun setPreferredStepsPackage(packageName: String?) {}

    suspend fun refreshRecentData(endDate: LocalDate, days: Int = 7): HealthConnectRefreshResult = HealthConnectRefreshResult(importedDayCount = 0, bodyMetricCount = 0)

    suspend fun exportLatestWorkout(): String?

    fun observeDailySummaries(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyHealthSummaryEntity>> = flowOf(emptyList())

    fun observeWeightSeries(fromEpochMillis: Long): Flow<List<BodyMetricEntity>> = flowOf(emptyList())
}

@OptIn(ExperimentalCoroutinesApi::class)
class LocalHealthRepository @Inject constructor(
    private val gateway: HealthConnectGateway,
    private val healthDao: HealthDao,
    private val trainingDao: TrainingDao,
    private val accountRepository: AccountRepository,
) : HealthRepository {
    private var clock: () -> Long = { System.currentTimeMillis() }

    internal constructor(
        gateway: HealthConnectGateway,
        healthDao: HealthDao,
        trainingDao: TrainingDao,
        accountRepository: AccountRepository,
        clock: () -> Long,
    ) : this(gateway, healthDao, trainingDao, accountRepository) {
        this.clock = clock
    }

    override suspend fun status(): HealthConnectStatus = gateway.status()

    override suspend fun requestablePermissions(): Set<String> = gateway.requestablePermissions()

    override fun observeDailySummary(date: LocalDate): Flow<DailyHealthSummaryEntity?> = accountRepository.observeActiveAccount().flatMapLatest { account ->
        healthDao.observeDailySummary(account.id, date.toEpochDay())
    }

    override fun observeDailySummaries(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyHealthSummaryEntity>> = accountRepository.observeActiveAccount().flatMapLatest { account ->
        healthDao.observeDailySummariesInRange(account.id, startDate.toEpochDay(), endDate.toEpochDay())
    }

    override fun observeWeightSeries(fromEpochMillis: Long): Flow<List<BodyMetricEntity>> = accountRepository.observeActiveAccount().flatMapLatest { account ->
        healthDao.observeBodyMetrics(account.id, WEIGHT_METRIC_TYPE, fromEpochMillis)
    }

    override suspend fun readStepSources(date: LocalDate): List<StepSource> = gateway.readStepSources(date)

    override fun observePreferredStepsPackage(): Flow<String?> = accountRepository.observeActiveAccount().flatMapLatest { account ->
        healthDao.observeHealthConnectSyncState(account.id).map { it?.preferredStepsPackage }
    }

    override suspend fun setPreferredStepsPackage(packageName: String?) {
        val accountId = accountRepository.ensureActiveAccount().id
        val existing = healthDao.getHealthConnectSyncState(accountId)
        if (existing == null) {
            val currentStatus = runCatching { gateway.status() }.getOrNull()
            healthDao.upsertHealthConnectSyncState(
                HealthConnectSyncStateEntity(
                    accountId = accountId,
                    key = SYNC_STATE_KEY,
                    isAvailable = currentStatus?.availability == HealthConnectAvailability.Available,
                    grantedPermissionsCsv = currentStatus?.grantedPermissions.orEmpty()
                        .sorted()
                        .joinToString(","),
                    lastImportAtEpochMillis = null,
                    lastExportAtEpochMillis = null,
                    lastFailureMessage = null,
                    preferredStepsPackage = packageName,
                ),
            )
        } else {
            healthDao.updatePreferredStepsPackage(accountId, packageName)
        }
    }

    override suspend fun importDailySummary(date: LocalDate): ImportedDailyHealthSummary {
        val accountId = accountRepository.ensureActiveAccount().id
        val preferredStepsPackage = healthDao.getHealthConnectSyncState(accountId)?.preferredStepsPackage
        val summary = gateway.readDailySummary(date, preferredStepsPackage)
        val now = clock()
        val dateEpochDay = date.toEpochDay()
        val existingSummary = healthDao.getDailySummary(accountId, dateEpochDay)
        val bodyMetrics = summary.bodyMetrics
        healthDao.upsertDailySummary(
            DailyHealthSummaryEntity(
                accountId = accountId,
                dateEpochDay = dateEpochDay,
                steps = summary.steps ?: existingSummary?.steps,
                activeCaloriesKcal = summary.activeCaloriesKcal ?: existingSummary?.activeCaloriesKcal,
                totalCaloriesKcal = summary.totalCaloriesKcal ?: existingSummary?.totalCaloriesKcal,
                distanceMeters = summary.distanceMeters ?: existingSummary?.distanceMeters,
                sleepMinutes = summary.sleepMinutes ?: existingSummary?.sleepMinutes,
                exerciseMinutes = summary.exerciseMinutes ?: existingSummary?.exerciseMinutes,
                exerciseSessionCount = summary.exerciseSessionCount ?: existingSummary?.exerciseSessionCount,
                latestWeightKg = summary.latestWeightKg ?: existingSummary?.latestWeightKg,
                latestBodyFatPercent = summary.latestBodyFatPercent ?: existingSummary?.latestBodyFatPercent,
                restingHeartRateBpm = summary.restingHeartRateBpm ?: existingSummary?.restingHeartRateBpm,
                hrvRmssdMillis = summary.hrvRmssdMillis ?: existingSummary?.hrvRmssdMillis,
                updatedAtEpochMillis = now,
            ),
        )
        bodyMetrics.forEach { metric ->
            healthDao.upsertBodyMetric(metric.toBodyMetricEntity(accountId))
        }
        upsertSyncState(
            accountId = accountId,
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
        val accountId = accountRepository.ensureActiveAccount().id
        val session = trainingDao.getLatestCompletedWorkoutSession(accountId) ?: return null
        val sets = trainingDao.getCompletedWorkoutSets(accountId, session.id)
        if (sets.isEmpty()) return null

        val fingerprint = workoutExportFingerprint(session, sets)
        val existing = healthDao.getHealthConnectExportRecord(accountId, HEALTH_EXPORT_TYPE_WORKOUT, session.id)
        if (existing?.payloadFingerprint == fingerprint) {
            return existing.providerRecordId
        }
        if (existing == null) {
            adoptLegacyWorkoutExport(accountId, session, fingerprint)?.let { return it }
        }

        val identity = HealthConnectRecordIdentity.forWorkout(
            accountId = accountId,
            sessionId = session.id,
            version = existing?.clientRecordVersion?.plus(1) ?: 1,
        )
        val recordId = gateway.exportWorkout(session, sets, identity) ?: return null
        val now = clock()
        persistWorkoutExportRecord(
            HealthConnectExportRecordEntity(
                accountId = accountId,
                recordType = HEALTH_EXPORT_TYPE_WORKOUT,
                localEntityId = session.id,
                clientRecordId = identity.clientRecordId,
                clientRecordVersion = identity.clientRecordVersion,
                payloadFingerprint = fingerprint,
                providerRecordId = recordId,
                exportedAtEpochMillis = now,
            ),
        )
        persistWorkoutProviderMetadata(session, recordId, now)
        upsertSyncState(
            accountId = accountId,
            lastImportAtEpochMillis = null,
            lastExportAtEpochMillis = now,
            lastFailureMessage = null,
        )
        return recordId
    }

    private suspend fun adoptLegacyWorkoutExport(
        accountId: String,
        session: WorkoutSessionEntity,
        fingerprint: String,
    ): String? {
        val providerRecordId = session.healthConnectRecordId?.takeIf(String::isNotBlank) ?: return null
        val identity = HealthConnectRecordIdentity.forWorkout(accountId, session.id, version = 1)
        persistWorkoutExportRecord(
            HealthConnectExportRecordEntity(
                accountId = accountId,
                recordType = HEALTH_EXPORT_TYPE_WORKOUT,
                localEntityId = session.id,
                clientRecordId = identity.clientRecordId,
                clientRecordVersion = identity.clientRecordVersion,
                payloadFingerprint = fingerprint,
                providerRecordId = providerRecordId,
                exportedAtEpochMillis = session.healthConnectLastExportedAtEpochMillis ?: clock(),
            ),
        )
        return providerRecordId
    }

    private suspend fun persistWorkoutExportRecord(record: HealthConnectExportRecordEntity) = healthDao.upsertHealthConnectExportRecord(record)

    private suspend fun persistWorkoutProviderMetadata(
        session: WorkoutSessionEntity,
        providerRecordId: String,
        exportedAt: Long,
    ) {
        val updatedSession = session.copy(
            healthConnectRecordId = providerRecordId,
            healthConnectLastExportedAtEpochMillis = exportedAt,
        )
        if (trainingDao.insertWorkoutSession(updatedSession) == -1L) {
            trainingDao.updateWorkoutSession(updatedSession)
        }
    }

    private suspend fun upsertSyncState(
        accountId: String,
        lastImportAtEpochMillis: Long?,
        lastExportAtEpochMillis: Long?,
        lastFailureMessage: String?,
    ) {
        val existing = healthDao.getHealthConnectSyncState(accountId)
        val currentStatus = runCatching { gateway.status() }.getOrNull()
        val grantedPermissions = currentStatus?.grantedPermissions.orEmpty()
        healthDao.upsertHealthConnectSyncState(
            HealthConnectSyncStateEntity(
                accountId = accountId,
                key = SYNC_STATE_KEY,
                isAvailable = currentStatus?.availability == HealthConnectAvailability.Available,
                grantedPermissionsCsv = grantedPermissions.sorted().joinToString(","),
                lastImportAtEpochMillis = lastImportAtEpochMillis ?: existing?.lastImportAtEpochMillis,
                lastExportAtEpochMillis = lastExportAtEpochMillis ?: existing?.lastExportAtEpochMillis,
                lastFailureMessage = lastFailureMessage,
                preferredStepsPackage = existing?.preferredStepsPackage,
            ),
        )
    }

    private companion object {
        const val HEALTH_EXPORT_TYPE_WORKOUT = "workout"
        const val SYNC_STATE_KEY = "health_connect"
        const val WEIGHT_METRIC_TYPE = "weight"
    }
}

private fun ImportedBodyMetric.toBodyMetricEntity(accountId: String): BodyMetricEntity {
    val externalToken = externalId?.takeIf { it.isNotBlank() } ?: measuredAtEpochMillis.toString()
    return BodyMetricEntity(
        accountId = accountId,
        id = "health-connect-$type-$externalToken",
        type = type,
        value = value,
        unit = unit,
        measuredAtEpochMillis = measuredAtEpochMillis,
        source = "health_connect",
        externalId = externalId,
    )
}
