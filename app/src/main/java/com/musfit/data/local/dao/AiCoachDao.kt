package com.musfit.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.musfit.data.local.entity.AiCoachSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiCoachDao {
    @Query("SELECT * FROM ai_coach_settings WHERE accountId = :accountId LIMIT 1")
    fun observeSettings(accountId: String): Flow<AiCoachSettingsEntity?>

    @Query("SELECT * FROM ai_coach_settings WHERE accountId = :accountId LIMIT 1")
    suspend fun getSettings(accountId: String): AiCoachSettingsEntity?

    @Upsert
    suspend fun upsertSettings(settings: AiCoachSettingsEntity)
}
