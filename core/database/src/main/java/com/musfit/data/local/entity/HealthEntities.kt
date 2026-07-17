package com.musfit.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "body_metrics",
    primaryKeys = ["accountId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["accountId", "type", "measuredAtEpochMillis"])],
)
data class BodyMetricEntity(
    val accountId: String,
    val id: String,
    val type: String,
    val value: Double,
    val unit: String,
    val measuredAtEpochMillis: Long,
    val source: String,
    val externalId: String?,
)

@Entity(
    tableName = "daily_health_summaries",
    primaryKeys = ["accountId", "dateEpochDay"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class DailyHealthSummaryEntity(
    val accountId: String,
    val dateEpochDay: Long,
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

@Entity(
    tableName = "health_connect_sync_state",
    primaryKeys = ["accountId", "key"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class HealthConnectSyncStateEntity(
    val accountId: String,
    val key: String,
    val isAvailable: Boolean,
    val grantedPermissionsCsv: String,
    val lastImportAtEpochMillis: Long?,
    val lastExportAtEpochMillis: Long?,
    val lastFailureMessage: String?,
    val preferredStepsPackage: String? = null,
)

@Entity(
    tableName = "health_connect_export_records",
    primaryKeys = ["accountId", "recordType", "localEntityId"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["accountId", "recordType"])],
)
data class HealthConnectExportRecordEntity(
    val accountId: String,
    val recordType: String,
    val localEntityId: String,
    val clientRecordId: String,
    val clientRecordVersion: Long,
    val payloadFingerprint: String,
    val providerRecordId: String,
    val exportedAtEpochMillis: Long,
)
