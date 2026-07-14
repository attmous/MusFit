package com.musfit.data.repository

import com.musfit.data.local.dao.AiCoachChatDao
import com.musfit.data.local.entity.AiCoachChatMessageEntity
import com.musfit.data.local.entity.AiCoachThreadEntity
import com.musfit.data.remote.coach.AiCoachEndpointPolicy
import com.musfit.data.remote.coach.CoachCompletionClient
import com.musfit.data.remote.coach.HermesChatMessage
import com.musfit.data.remote.coach.HermesChatRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

enum class AiCoachChatRole { User, Assistant }

enum class AiCoachChatMessageStatus { Sending, Sent, Failed }

data class AiCoachChatMessage(
    val id: String,
    val role: AiCoachChatRole,
    val content: String,
    val status: AiCoachChatMessageStatus,
    val errorMessage: String?,
    val createdAtEpochMillis: Long,
)

interface AiCoachChatRepository {
    fun observeMessages(): Flow<List<AiCoachChatMessage>>
    suspend fun sendMessage(content: String, systemPrompt: String)
    suspend fun clearThread()
    suspend fun testConnection()
}

@OptIn(ExperimentalCoroutinesApi::class)
class LocalAiCoachChatRepository @Inject constructor(
    private val chatDao: AiCoachChatDao,
    private val accountRepository: AccountRepository,
    private val aiCoachRepository: AiCoachRepository,
    private val coachCompletionClient: CoachCompletionClient,
) : AiCoachChatRepository {
    private var clock: () -> Long = { System.currentTimeMillis() }

    internal constructor(
        chatDao: AiCoachChatDao,
        accountRepository: AccountRepository,
        aiCoachRepository: AiCoachRepository,
        coachCompletionClient: CoachCompletionClient,
        clock: () -> Long,
    ) : this(chatDao, accountRepository, aiCoachRepository, coachCompletionClient) {
        this.clock = clock
    }

    override fun observeMessages(): Flow<List<AiCoachChatMessage>> = accountRepository.observeActiveAccount().flatMapLatest { account ->
        aiCoachRepository.observeSettings().flatMapLatest { settings ->
            if (settings.providerKind == AiCoachProviderKind.Disabled) {
                flowOf(emptyList())
            } else {
                chatDao.observeThread(
                    accountId = account.id,
                    providerKind = settings.providerKind.name,
                    localAgentKind = settings.localAgentKind.name,
                ).flatMapLatest { thread ->
                    if (thread == null) {
                        flowOf(emptyList())
                    } else {
                        chatDao.observeMessages(thread.id).map { rows -> rows.mapNotNull { it.toChatMessage() } }
                    }
                }
            }
        }
    }

    override suspend fun sendMessage(content: String, systemPrompt: String) {
        val trimmed = content.trim()
        require(trimmed.isNotBlank()) { "Ask coach something first." }
        val connection = aiCoachRepository.activeConnection()
            ?: throw IllegalStateException("Choose a coach connection in Profile settings first.")
        AiCoachEndpointPolicy.requireAllowed(connection.baseUrl)
        val account = accountRepository.ensureActiveAccount()
        val thread = ensureThread(account.id, connection)
        val now = clock()
        val userMessage = AiCoachChatMessageEntity(
            id = UUID.randomUUID().toString(),
            threadId = thread.id,
            role = AiCoachChatRole.User.name,
            content = trimmed,
            status = AiCoachChatMessageStatus.Sent.name,
            errorMessage = null,
            createdAtEpochMillis = now,
        )
        chatDao.insertMessage(userMessage)

        val assistantMessage = AiCoachChatMessageEntity(
            id = UUID.randomUUID().toString(),
            threadId = thread.id,
            role = AiCoachChatRole.Assistant.name,
            content = "",
            status = AiCoachChatMessageStatus.Sending.name,
            errorMessage = null,
            createdAtEpochMillis = now + 1,
        )
        chatDao.insertMessage(assistantMessage)

        val responseResult = runCatching {
            val recent = chatDao.getRecentMessages(thread.id, HISTORY_LIMIT)
                .asReversed()
                .filter { it.id != assistantMessage.id && it.status == AiCoachChatMessageStatus.Sent.name }
            val useRemoteSession = connection.apiKey != null && thread.remoteSessionId != null
            val requestMessages = if (useRemoteSession) {
                listOf(HermesChatMessage(role = "user", content = trimmed))
            } else {
                recent.mapNotNull { row ->
                    row.toChatMessage()?.let { message ->
                        HermesChatMessage(role = message.role.openAiRole(), content = message.content)
                    }
                }.takeLast(HISTORY_LIMIT)
            }.trimHistoryText()
            coachCompletionClient.chat(
                HermesChatRequest(
                    connection = connection,
                    systemPrompt = systemPrompt,
                    messages = requestMessages,
                    idempotencyKey = userMessage.id,
                    remoteSessionId = thread.remoteSessionId,
                ),
            )
        }
        val requestError = responseResult.exceptionOrNull()
        if (requestError is CancellationException) {
            withContext(NonCancellable) {
                chatDao.deleteMessage(assistantMessage.id)
            }
            throw requestError
        }
        if (requestError != null) {
            chatDao.updateMessage(
                assistantMessage.copy(
                    content = "I could not reach the coach connection.",
                    status = AiCoachChatMessageStatus.Failed.name,
                    errorMessage = requestError.message ?: "Hermes request failed.",
                ),
            )
            throw requestError
        }
        val response = responseResult.getOrThrow()
        chatDao.updateMessage(
            assistantMessage.copy(
                content = response.content,
                status = AiCoachChatMessageStatus.Sent.name,
                errorMessage = null,
            ),
        )
        response.remoteSessionId?.let {
            chatDao.updateRemoteSession(thread.id, it, updatedAt = clock())
        }
    }

    override suspend fun clearThread() {
        val connection = aiCoachRepository.activeConnection() ?: return
        val account = accountRepository.ensureActiveAccount()
        val thread = ensureThread(account.id, connection)
        chatDao.clearThread(thread.id, updatedAt = clock())
    }

    override suspend fun testConnection() {
        val connection = aiCoachRepository.activeConnection()
            ?: throw IllegalStateException("Choose a coach connection in Profile settings first.")
        coachCompletionClient.testConnection(connection)
    }

    private suspend fun ensureThread(accountId: String, connection: AiCoachConnection): AiCoachThreadEntity {
        val existing = chatDao.getThread(
            accountId = accountId,
            providerKind = connection.providerKind.name,
            localAgentKind = connection.localAgentKind.name,
        )
        if (existing != null) return existing
        val now = clock()
        val thread = AiCoachThreadEntity(
            id = UUID.randomUUID().toString(),
            accountId = accountId,
            providerKind = connection.providerKind.name,
            localAgentKind = connection.localAgentKind.name,
            remoteSessionId = null,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        chatDao.upsertThread(thread)
        return thread
    }

    private fun AiCoachChatMessageEntity.toChatMessage(): AiCoachChatMessage? {
        val role = runCatching { AiCoachChatRole.valueOf(role) }.getOrNull() ?: return null
        val status = runCatching { AiCoachChatMessageStatus.valueOf(status) }.getOrNull() ?: return null
        return AiCoachChatMessage(
            id = id,
            role = role,
            content = content,
            status = status,
            errorMessage = errorMessage,
            createdAtEpochMillis = createdAtEpochMillis,
        )
    }

    private fun AiCoachChatRole.openAiRole(): String = when (this) {
        AiCoachChatRole.User -> "user"
        AiCoachChatRole.Assistant -> "assistant"
    }

    private fun List<HermesChatMessage>.trimHistoryText(): List<HermesChatMessage> {
        var remaining = HISTORY_CHARACTER_BUDGET
        return asReversed().mapNotNull { message ->
            val cost = message.content.length
            if (cost <= 0 || cost > remaining) {
                null
            } else {
                remaining -= cost
                message
            }
        }.asReversed()
    }

    private companion object {
        const val HISTORY_LIMIT = 24
        const val HISTORY_CHARACTER_BUDGET = 12_000
    }
}
