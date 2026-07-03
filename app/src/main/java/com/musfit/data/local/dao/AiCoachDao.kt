package com.musfit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musfit.data.local.entity.AiCoachSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiCoachDao {
    @Query("SELECT * FROM ai_coach_settings WHERE accountId = :accountId LIMIT 1")
    fun observeSettings(accountId: String): Flow<AiCoachSettingsEntity?>

    @Query("SELECT * FROM ai_coach_settings WHERE accountId = :accountId LIMIT 1")
    suspend fun getSettings(accountId: String): AiCoachSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: AiCoachSettingsEntity)
}
