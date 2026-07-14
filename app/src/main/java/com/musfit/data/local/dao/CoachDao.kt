package com.musfit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.musfit.data.local.entity.CoachMessageEntity
import com.musfit.data.local.entity.DashboardPinEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CoachDao {
    @Query(
        "SELECT * FROM coach_messages WHERE accountId = :accountId AND isDismissed = 0 " +
            "ORDER BY dayEpochDay DESC, firstSeenAtEpochMillis DESC",
    )
    fun observeFeed(accountId: String): Flow<List<CoachMessageEntity>>

    @Query("SELECT * FROM coach_messages WHERE accountId = :accountId AND dayEpochDay = :dayEpochDay AND source = :source")
    suspend fun getMessagesForDay(accountId: String, dayEpochDay: Long, source: String): List<CoachMessageEntity>

    @Insert
    suspend fun insert(entity: CoachMessageEntity): Long

    @Update
    suspend fun update(entity: CoachMessageEntity)

    @Query("UPDATE coach_messages SET isDismissed = 1 WHERE accountId = :accountId AND id = :id")
    suspend fun dismiss(accountId: String, id: Long)

    @Query("UPDATE coach_messages SET isRead = 1 WHERE accountId = :accountId AND isRead = 0 AND isDismissed = 0")
    suspend fun markAllRead(accountId: String)

    @Query("DELETE FROM coach_messages WHERE accountId = :accountId AND dayEpochDay < :minDayEpochDay")
    suspend fun prune(accountId: String, minDayEpochDay: Long)

    @Query("SELECT * FROM dashboard_pins WHERE accountId = :accountId ORDER BY position ASC")
    fun observePins(accountId: String): Flow<List<DashboardPinEntity>>

    @Query("DELETE FROM dashboard_pins WHERE accountId = :accountId")
    suspend fun clearPins(accountId: String)

    // Intentional leaf replacement: dashboard_pins has no dependents or foreign keys, and
    // replacePins clears and rebuilds the complete ordered snapshot in one transaction.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPins(pins: List<DashboardPinEntity>)

    @Transaction
    suspend fun replacePins(accountId: String, pins: List<DashboardPinEntity>) {
        clearPins(accountId)
        insertPins(pins)
    }
}
