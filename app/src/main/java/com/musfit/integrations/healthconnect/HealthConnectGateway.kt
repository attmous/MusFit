package com.musfit.integrations.healthconnect

import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity
import com.musfit.domain.health.HealthConnectStatus
import com.musfit.domain.health.ImportedDailyHealthSummary
import java.time.LocalDate

interface HealthConnectGateway {
    suspend fun status(): HealthConnectStatus
    suspend fun requestablePermissions(): Set<String>
    suspend fun foodRequestablePermissions(): Set<String>
    suspend fun readDailySummary(date: LocalDate): ImportedDailyHealthSummary
    suspend fun exportWorkout(session: WorkoutSessionEntity, sets: List<WorkoutSetEntity>): String?
    suspend fun exportFood(payload: HealthConnectFoodExportPayload): HealthConnectFoodExportResult?
}
