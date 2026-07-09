package com.musfit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.musfit.data.local.entity.AiCoachChatMessageEntity
import com.musfit.data.local.entity.AiCoachThreadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiCoachChatDao {
    @Query(
        "SELECT * FROM ai_coach_threads WHERE accountId = :accountId " +
            "AND providerKind = :providerKind AND localAgentKind = :localAgentKind LIMIT 1",
    )
    suspend fun getThread(accountId: String, providerKind: String, localAgentKind: String): AiCoachThreadEntity?

    @Query(
        "SELECT * FROM ai_coach_threads WHERE accountId = :accountId " +
            "AND providerKind = :providerKind AND localAgentKind = :localAgentKind LIMIT 1",
    )
    fun observeThread(accountId: String, providerKind: String, localAgentKind: String): Flow<AiCoachThreadEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertThread(thread: AiCoachThreadEntity)

    @Update
    suspend fun updateThread(thread: AiCoachThreadEntity)

    @Query(
        "SELECT * FROM ai_coach_chat_messages WHERE threadId = :threadId " +
            "ORDER BY createdAtEpochMillis ASC",
    )
    fun observeMessages(threadId: String): Flow<List<AiCoachChatMessageEntity>>

    @Query(
        "SELECT * FROM ai_coach_chat_messages WHERE threadId = :threadId " +
            "ORDER BY createdAtEpochMillis DESC LIMIT :limit",
    )
    suspend fun getRecentMessages(threadId: String, limit: Int): List<AiCoachChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AiCoachChatMessageEntity)

    @Update
    suspend fun updateMessage(message: AiCoachChatMessageEntity)

    @Query("DELETE FROM ai_coach_chat_messages WHERE threadId = :threadId")
    suspend fun clearMessages(threadId: String)

    @Query("UPDATE ai_coach_threads SET remoteSessionId = :remoteSessionId, updatedAtEpochMillis = :updatedAt WHERE id = :threadId")
    suspend fun updateRemoteSession(threadId: String, remoteSessionId: String?, updatedAt: Long)

    @Transaction
    suspend fun clearThread(threadId: String, updatedAt: Long) {
        clearMessages(threadId)
        updateRemoteSession(threadId, remoteSessionId = null, updatedAt = updatedAt)
    }
}
