package com.musfit.integrations.healthconnect

import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity
import com.musfit.domain.health.HealthConnectDailyReadResult
import com.musfit.domain.health.HealthConnectStatus
import com.musfit.domain.health.StepSource
import java.time.LocalDate

interface HealthConnectGateway {
    suspend fun status(): HealthConnectStatus
    suspend fun requestablePermissions(): Set<String>
    suspend fun foodRequestablePermissions(): Set<String>

    /**
     * Reads the day's health summary. When [preferredStepsPackage] is non-null, the steps total is
     * restricted to that single data origin (mirroring one app) instead of the cross-source unified
     * total; when null, the officially-recommended unified aggregate is used.
     */
    suspend fun readDailySummary(
        date: LocalDate,
        preferredStepsPackage: String? = null,
    ): HealthConnectDailyReadResult

    /**
     * Reads an inclusive range of local calendar days. Adapters should override this to batch
     * provider work; the default keeps test and fallback gateways source-compatible.
     */
    suspend fun readDailySummaries(
        startDate: LocalDate,
        endDateInclusive: LocalDate,
        preferredStepsPackage: String? = null,
    ): Map<LocalDate, HealthConnectDailyReadResult> {
        require(!endDateInclusive.isBefore(startDate)) { "endDateInclusive must not precede startDate" }
        return generateSequence(startDate) { date -> date.plusDays(1) }
            .takeWhile { date -> !date.isAfter(endDateInclusive) }
            .associateWith { date -> readDailySummary(date, preferredStepsPackage) }
    }

    /** Per-origin step totals for [date], so the user can choose which source MusFit mirrors. */
    suspend fun readStepSources(date: LocalDate): List<StepSource> = emptyList()

    suspend fun exportWorkout(session: WorkoutSessionEntity, sets: List<WorkoutSetEntity>): String?
    suspend fun exportWorkout(
        session: WorkoutSessionEntity,
        sets: List<WorkoutSetEntity>,
        identity: HealthConnectRecordIdentity,
    ): String? = exportWorkout(session, sets)
    suspend fun exportFood(payload: HealthConnectFoodExportPayload): HealthConnectFoodExportResult?

    suspend fun deleteAuthoredRecords(records: Set<HealthConnectAuthoredRecord>): HealthConnectDeleteResult = HealthConnectDeleteResult.Failure("Health Connect authored-record deletion is not implemented.")
}
