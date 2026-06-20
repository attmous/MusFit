package com.musfit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musfit.data.local.entity.BodyMetricEntity
import com.musfit.data.local.entity.DailyHealthSummaryEntity
import com.musfit.data.local.entity.HealthConnectSyncStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthDao {
    @Query(
        "SELECT * FROM body_metrics " +
            "WHERE type = :type AND measuredAtEpochMillis >= :fromEpochMillis " +
            "ORDER BY measuredAtEpochMillis DESC",
    )
    fun observeBodyMetrics(type: String, fromEpochMillis: Long): Flow<List<BodyMetricEntity>>

    @Query(
        "SELECT * FROM body_metrics " +
            "WHERE type = :type AND measuredAtEpochMillis >= :fromEpochMillis " +
            "ORDER BY measuredAtEpochMillis DESC",
    )
    suspend fun getBodyMetrics(type: String, fromEpochMillis: Long): List<BodyMetricEntity>

    @Query("SELECT * FROM daily_health_summaries WHERE dateEpochDay = :dateEpochDay LIMIT 1")
    fun observeDailySummary(dateEpochDay: Long): Flow<DailyHealthSummaryEntity?>

    @Query("SELECT * FROM health_connect_sync_state WHERE key = 'health_connect' LIMIT 1")
    fun observeHealthConnectSyncState(): Flow<HealthConnectSyncStateEntity?>

    @Query("SELECT * FROM health_connect_sync_state WHERE key = 'health_connect' LIMIT 1")
    suspend fun getHealthConnectSyncState(): HealthConnectSyncStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBodyMetric(metric: BodyMetricEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailySummary(summary: DailyHealthSummaryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHealthConnectSyncState(state: HealthConnectSyncStateEntity)
}
