package com.musfit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "body_metrics")
data class BodyMetricEntity(
    @PrimaryKey val id: String,
    val type: String,
    val value: Double,
    val unit: String,
    val measuredAtEpochMillis: Long,
    val source: String,
    val externalId: String?,
)

@Entity(tableName = "daily_health_summaries")
data class DailyHealthSummaryEntity(
    @PrimaryKey val dateEpochDay: Long,
    val steps: Long?,
    val activeCaloriesKcal: Double?,
    val totalCaloriesKcal: Double?,
    val distanceMeters: Double?,
    val sleepMinutes: Long?,
    val exerciseMinutes: Long?,
    val exerciseSessionCount: Int?,
    val latestWeightKg: Double?,
    val latestBodyFatPercent: Double?,
    val restingHeartRateBpm: Long?,
    val hrvRmssdMillis: Double?,
    val updatedAtEpochMillis: Long,
)

@Entity(tableName = "health_connect_sync_state")
data class HealthConnectSyncStateEntity(
    @PrimaryKey val key: String,
    val isAvailable: Boolean,
    val grantedPermissionsCsv: String,
    val lastImportAtEpochMillis: Long?,
    val lastExportAtEpochMillis: Long?,
    val lastFailureMessage: String?,
    val preferredStepsPackage: String? = null,
)
