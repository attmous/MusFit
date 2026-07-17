package com.musfit.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "ai_coach_settings",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class AiCoachSettingsEntity(
    @PrimaryKey val accountId: String,
    val providerKind: String,
    val baseUrl: String,
    val modelName: String,
    val localAgentKind: String,
    val apiKeyStored: Boolean,
    val updatedAtEpochMillis: Long,
)
