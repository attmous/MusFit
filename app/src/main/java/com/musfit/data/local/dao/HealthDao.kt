package com.musfit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.musfit.data.local.entity.BodyMetricEntity
import com.musfit.data.local.entity.DailyHealthSummaryEntity
import com.musfit.data.local.entity.HealthConnectExportRecordEntity
import com.musfit.data.local.entity.HealthConnectSyncStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthDao {
    @Query(
        "SELECT * FROM body_metrics " +
            "WHERE accountId = :accountId AND type = :type AND measuredAtEpochMillis >= :fromEpochMillis " +
            "ORDER BY measuredAtEpochMillis DESC",
    )
    fun observeBodyMetrics(accountId: String, type: String, fromEpochMillis: Long): Flow<List<BodyMetricEntity>>

    @Query(
        "SELECT * FROM body_metrics " +
            "WHERE accountId = :accountId AND type = :type AND measuredAtEpochMillis >= :fromEpochMillis " +
            "ORDER BY measuredAtEpochMillis DESC",
    )
    suspend fun getBodyMetrics(accountId: String, type: String, fromEpochMillis: Long): List<BodyMetricEntity>

    @Query("SELECT * FROM daily_health_summaries WHERE accountId = :accountId AND dateEpochDay = :dateEpochDay LIMIT 1")
    fun observeDailySummary(accountId: String, dateEpochDay: Long): Flow<DailyHealthSummaryEntity?>

    @Query("SELECT * FROM daily_health_summaries WHERE accountId = :accountId AND dateEpochDay = :dateEpochDay LIMIT 1")
    suspend fun getDailySummary(accountId: String, dateEpochDay: Long): DailyHealthSummaryEntity?

    @Query(
        "SELECT * FROM daily_health_summaries " +
            "WHERE accountId = :accountId AND dateEpochDay BETWEEN :startEpochDay AND :endEpochDay ORDER BY dateEpochDay",
    )
    fun observeDailySummariesInRange(accountId: String, startEpochDay: Long, endEpochDay: Long): Flow<List<DailyHealthSummaryEntity>>

    @Query("SELECT * FROM health_connect_sync_state WHERE accountId = :accountId AND key = 'health_connect' LIMIT 1")
    fun observeHealthConnectSyncState(accountId: String): Flow<HealthConnectSyncStateEntity?>

    @Query("SELECT * FROM health_connect_sync_state WHERE accountId = :accountId AND key = 'health_connect' LIMIT 1")
    suspend fun getHealthConnectSyncState(accountId: String): HealthConnectSyncStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBodyMetric(metric: BodyMetricEntity)

    @Query("DELETE FROM body_metrics WHERE accountId = :accountId AND id = :id")
    suspend fun deleteBodyMetric(accountId: String, id: String)

    @Query("UPDATE body_metrics SET value = :value WHERE accountId = :accountId AND id = :id")
    suspend fun updateBodyMetricValue(accountId: String, id: String, value: Double)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailySummary(summary: DailyHealthSummaryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHealthConnectSyncState(state: HealthConnectSyncStateEntity)

    @Query(
        "UPDATE health_connect_sync_state SET preferredStepsPackage = :packageName " +
            "WHERE accountId = :accountId AND key = 'health_connect'",
    )
    suspend fun updatePreferredStepsPackage(accountId: String, packageName: String?)

    @Query(
        "SELECT * FROM health_connect_export_records " +
            "WHERE accountId = :accountId AND recordType = :recordType AND localEntityId = :localEntityId LIMIT 1",
    )
    suspend fun getHealthConnectExportRecord(
        accountId: String,
        recordType: String,
        localEntityId: String,
    ): HealthConnectExportRecordEntity?

    @Query(
        "SELECT * FROM health_connect_export_records " +
            "WHERE accountId = :accountId AND recordType = :recordType ORDER BY localEntityId",
    )
    suspend fun getHealthConnectExportRecords(accountId: String, recordType: String): List<HealthConnectExportRecordEntity>

    @Upsert
    suspend fun upsertHealthConnectExportRecord(record: HealthConnectExportRecordEntity)
}
