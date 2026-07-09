package com.musfit.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ai_coach_threads",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["accountId", "providerKind", "localAgentKind"], unique = true),
    ],
)
data class AiCoachThreadEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val providerKind: String,
    val localAgentKind: String,
    val remoteSessionId: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "ai_coach_chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = AiCoachThreadEntity::class,
            parentColumns = ["id"],
            childColumns = ["threadId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["threadId", "createdAtEpochMillis"]),
    ],
)
data class AiCoachChatMessageEntity(
    @PrimaryKey val id: String,
    val threadId: String,
    val role: String,
    val content: String,
    val status: String,
    val errorMessage: String?,
    val createdAtEpochMillis: Long,
)
