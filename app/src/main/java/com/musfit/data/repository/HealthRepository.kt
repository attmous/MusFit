package com.musfit.data.repository

import com.musfit.data.local.dao.HealthDao
import com.musfit.data.local.dao.TrainingDao
import com.musfit.data.local.entity.BodyMetricEntity
import com.musfit.data.local.entity.DailyHealthSummaryEntity
import com.musfit.data.local.entity.HealthConnectExportRecordEntity
import com.musfit.data.local.entity.HealthConnectSyncStateEntity
import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.domain.health.HealthConnectAvailability
import com.musfit.domain.health.HealthConnectDailyReadResult
import com.musfit.domain.health.HealthConnectMetric
import com.musfit.domain.health.HealthConnectMetricFailure
import com.musfit.domain.health.HealthConnectStatus
import com.musfit.domain.health.ImportedBodyMetric
import com.musfit.domain.health.ImportedDailyHealthSummary
import com.musfit.domain.health.StepSource
import com.musfit.integrations.healthconnect.HealthConnectGateway
import com.musfit.integrations.healthconnect.HealthConnectRecordIdentity
import com.musfit.integrations.healthconnect.workoutExportFingerprint
import kotlinx.coroutines.CancellationException
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
    val partialDayCount: Int = 0,
    val emptyDayCount: Int = 0,
    val failedDayCount: Int = 0,
)

sealed interface HealthConnectImportResult {
    val summary: ImportedDailyHealthSummary

    data class Complete(override val summary: ImportedDailyHealthSummary) : HealthConnectImportResult

    data class Partial(
        override val summary: ImportedDailyHealthSummary,
        val failures: List<HealthConnectMetricFailure>,
    ) : HealthConnectImportResult

    data class Empty(override val summary: ImportedDailyHealthSummary) : HealthConnectImportResult

    data class Cleared(override val summary: ImportedDailyHealthSummary) : HealthConnectImportResult

    data class Unavailable(val message: String) : HealthConnectImportResult {
        override val summary: ImportedDailyHealthSummary = ImportedDailyHealthSummary()
    }

    data class Failure(val message: String) : HealthConnectImportResult {
        override val summary: ImportedDailyHealthSummary = ImportedDailyHealthSummary()
    }
}

interface HealthRepository {
    suspend fun status(): HealthConnectStatus

    suspend fun requestablePermissions(): Set<String>

    fun observeDailySummary(date: LocalDate): Flow<DailyHealthSummaryEntity?>

    suspend fun importDailySummary(date: LocalDate): HealthConnectImportResult

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
            val currentStatus = readStatusOrNull()
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

    override suspend fun importDailySummary(date: LocalDate): HealthConnectImportResult {
        val accountId = accountRepository.ensureActiveAccount().id
        val existingSyncState = healthDao.getHealthConnectSyncState(accountId)
        val readResult = gateway.readDailySummary(date, existingSyncState?.preferredStepsPackage)
        val dateEpochDay = date.toEpochDay()
        val existingSummary = healthDao.getDailySummary(accountId, dateEpochDay)
        val target = HealthConnectImportTarget(accountId, dateEpochDay, existingSummary, existingSyncState)
        return when (readResult) {
            is HealthConnectDailyReadResult.Failure -> persistImportFailure(
                accountId = accountId,
                existing = existingSyncState,
                status = readResult.status,
                message = readResult.message,
            ).let { HealthConnectImportResult.Failure(readResult.message) }

            is HealthConnectDailyReadResult.Unavailable -> persistImportFailure(
                accountId = accountId,
                existing = existingSyncState,
                status = readResult.status,
                message = readResult.message,
            ).let { HealthConnectImportResult.Unavailable(readResult.message) }

            is HealthConnectDailyReadResult.Complete -> persistSuccessfulImport(
                target,
                SuccessfulHealthRead(
                    summary = readResult.summary,
                    completedMetrics = readResult.completedMetrics,
                    status = readResult.status,
                ),
            )

            is HealthConnectDailyReadResult.Partial -> persistSuccessfulImport(
                target,
                SuccessfulHealthRead(
                    summary = readResult.summary,
                    completedMetrics = readResult.completedMetrics,
                    status = readResult.status,
                    failures = readResult.failures,
                ),
            )

            is HealthConnectDailyReadResult.Empty -> persistSuccessfulImport(
                target,
                SuccessfulHealthRead(
                    summary = readResult.summary,
                    completedMetrics = readResult.completedMetrics,
                    status = readResult.status,
                    emptyRead = true,
                ),
            )
        }
    }

    override suspend fun refreshRecentData(endDate: LocalDate, days: Int): HealthConnectRefreshResult {
        val safeDays = days.coerceAtLeast(1)
        var importedDayCount = 0
        var bodyMetricCount = 0
        var partialDayCount = 0
        var emptyDayCount = 0
        var failedDayCount = 0
        for (offset in safeDays - 1 downTo 0) {
            when (val result = importDailySummary(endDate.minusDays(offset.toLong()))) {
                is HealthConnectImportResult.Complete -> {
                    importedDayCount += 1
                    bodyMetricCount += result.summary.bodyMetrics.size
                }

                is HealthConnectImportResult.Partial -> {
                    importedDayCount += 1
                    partialDayCount += 1
                    bodyMetricCount += result.summary.bodyMetrics.size
                }

                is HealthConnectImportResult.Empty,
                is HealthConnectImportResult.Cleared,
                -> {
                    importedDayCount += 1
                    emptyDayCount += 1
                }

                is HealthConnectImportResult.Failure,
                is HealthConnectImportResult.Unavailable,
                -> failedDayCount += 1
            }
        }
        return HealthConnectRefreshResult(
            importedDayCount = importedDayCount,
            bodyMetricCount = bodyMetricCount,
            partialDayCount = partialDayCount,
            emptyDayCount = emptyDayCount,
            failedDayCount = failedDayCount,
        )
    }

    private suspend fun persistImportFailure(
        accountId: String,
        existing: HealthConnectSyncStateEntity?,
        status: HealthConnectStatus?,
        message: String,
    ) {
        healthDao.upsertHealthConnectSyncState(
            HealthConnectSyncStateEntity(
                accountId = accountId,
                key = SYNC_STATE_KEY,
                isAvailable = status?.availability == HealthConnectAvailability.Available,
                grantedPermissionsCsv = status?.grantedPermissions
                    ?.sorted()
                    ?.joinToString(",")
                    ?: existing?.grantedPermissionsCsv.orEmpty(),
                lastImportAtEpochMillis = existing?.lastImportAtEpochMillis,
                lastExportAtEpochMillis = existing?.lastExportAtEpochMillis,
                lastFailureMessage = message,
                preferredStepsPackage = existing?.preferredStepsPackage,
            ),
        )
    }

    private suspend fun persistSuccessfulImport(
        target: HealthConnectImportTarget,
        read: SuccessfulHealthRead,
    ): HealthConnectImportResult {
        val now = clock()
        val clearedExistingValue = read.emptyRead && target.existingSummary.hasValueFor(read.completedMetrics)
        val mergedSummary = target.existingSummary.mergeImported(
            accountId = target.accountId,
            dateEpochDay = target.dateEpochDay,
            imported = read.summary,
            completedMetrics = read.completedMetrics,
            updatedAtEpochMillis = now,
        )
        val failureMessage = read.failures
            .takeIf(List<*>::isNotEmpty)
            ?.joinToString("; ") { failure -> "${failure.metric}: ${failure.message}" }
        healthDao.persistHealthConnectImport(
            summary = mergedSummary,
            bodyMetrics = read.summary.bodyMetrics.map { metric -> metric.toBodyMetricEntity(target.accountId) },
            syncState = HealthConnectSyncStateEntity(
                accountId = target.accountId,
                key = SYNC_STATE_KEY,
                isAvailable = true,
                grantedPermissionsCsv = read.status.grantedPermissions.sorted().joinToString(","),
                lastImportAtEpochMillis = now,
                lastExportAtEpochMillis = target.existingSyncState?.lastExportAtEpochMillis,
                lastFailureMessage = failureMessage,
                preferredStepsPackage = target.existingSyncState?.preferredStepsPackage,
            ),
        )
        return when {
            read.failures.isNotEmpty() -> HealthConnectImportResult.Partial(read.summary, read.failures)
            read.emptyRead && clearedExistingValue -> HealthConnectImportResult.Cleared(read.summary)
            read.emptyRead -> HealthConnectImportResult.Empty(read.summary)
            else -> HealthConnectImportResult.Complete(read.summary)
        }
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
        val currentStatus = readStatusOrNull()
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

    @Suppress("TooGenericExceptionCaught")
    private suspend fun readStatusOrNull(): HealthConnectStatus? = try {
        gateway.status()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val HEALTH_EXPORT_TYPE_WORKOUT = "workout"
        const val SYNC_STATE_KEY = "health_connect"
        const val WEIGHT_METRIC_TYPE = "weight"
    }
}

private data class HealthConnectImportTarget(
    val accountId: String,
    val dateEpochDay: Long,
    val existingSummary: DailyHealthSummaryEntity?,
    val existingSyncState: HealthConnectSyncStateEntity?,
)

private data class SuccessfulHealthRead(
    val summary: ImportedDailyHealthSummary,
    val completedMetrics: Set<HealthConnectMetric>,
    val status: HealthConnectStatus,
    val failures: List<HealthConnectMetricFailure> = emptyList(),
    val emptyRead: Boolean = false,
)

private fun DailyHealthSummaryEntity?.mergeImported(
    accountId: String,
    dateEpochDay: Long,
    imported: ImportedDailyHealthSummary,
    completedMetrics: Set<HealthConnectMetric>,
    updatedAtEpochMillis: Long,
): DailyHealthSummaryEntity {
    fun <T> resolved(metric: HealthConnectMetric, importedValue: T?, existingValue: T?): T? = if (metric in completedMetrics) importedValue else existingValue

    return DailyHealthSummaryEntity(
        accountId = accountId,
        dateEpochDay = dateEpochDay,
        steps = resolved(HealthConnectMetric.Steps, imported.steps, this?.steps),
        activeCaloriesKcal = resolved(
            HealthConnectMetric.ActiveCalories,
            imported.activeCaloriesKcal,
            this?.activeCaloriesKcal,
        ),
        totalCaloriesKcal = resolved(
            HealthConnectMetric.TotalCalories,
            imported.totalCaloriesKcal,
            this?.totalCaloriesKcal,
        ),
        distanceMeters = resolved(HealthConnectMetric.Distance, imported.distanceMeters, this?.distanceMeters),
        sleepMinutes = resolved(HealthConnectMetric.Sleep, imported.sleepMinutes, this?.sleepMinutes),
        exerciseMinutes = resolved(
            HealthConnectMetric.ExerciseDuration,
            imported.exerciseMinutes,
            this?.exerciseMinutes,
        ),
        exerciseSessionCount = resolved(
            HealthConnectMetric.ExerciseSessions,
            imported.exerciseSessionCount,
            this?.exerciseSessionCount,
        ),
        latestWeightKg = resolved(HealthConnectMetric.Weight, imported.latestWeightKg, this?.latestWeightKg),
        latestBodyFatPercent = resolved(
            HealthConnectMetric.BodyFat,
            imported.latestBodyFatPercent,
            this?.latestBodyFatPercent,
        ),
        restingHeartRateBpm = resolved(
            HealthConnectMetric.RestingHeartRate,
            imported.restingHeartRateBpm,
            this?.restingHeartRateBpm,
        ),
        hrvRmssdMillis = resolved(
            HealthConnectMetric.HeartRateVariability,
            imported.hrvRmssdMillis,
            this?.hrvRmssdMillis,
        ),
        updatedAtEpochMillis = updatedAtEpochMillis,
    )
}

private fun DailyHealthSummaryEntity?.hasValueFor(metrics: Set<HealthConnectMetric>): Boolean {
    if (this == null) return false
    return metrics.any { metric ->
        when (metric) {
            HealthConnectMetric.Steps -> steps != null
            HealthConnectMetric.ActiveCalories -> activeCaloriesKcal != null
            HealthConnectMetric.TotalCalories -> totalCaloriesKcal != null
            HealthConnectMetric.Distance -> distanceMeters != null
            HealthConnectMetric.Sleep -> sleepMinutes != null
            HealthConnectMetric.ExerciseDuration -> exerciseMinutes != null
            HealthConnectMetric.ExerciseSessions -> exerciseSessionCount != null
            HealthConnectMetric.Weight -> latestWeightKg != null
            HealthConnectMetric.BodyFat -> latestBodyFatPercent != null
            HealthConnectMetric.RestingHeartRate -> restingHeartRateBpm != null
            HealthConnectMetric.HeartRateVariability -> hrvRmssdMillis != null
        }
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
