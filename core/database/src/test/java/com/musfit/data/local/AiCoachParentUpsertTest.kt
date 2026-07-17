package com.musfit.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.musfit.data.local.entity.AccountEntity
import com.musfit.data.local.entity.AiCoachChatMessageEntity
import com.musfit.data.local.entity.AiCoachSettingsEntity
import com.musfit.data.local.entity.AiCoachThreadEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AiCoachParentUpsertTest {
    private lateinit var database: MusFitDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MusFitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun threadUpsert_preservesMessages() = runTest {
        val graph = insertChatGraph()

        database.aiCoachChatDao().upsertThread(
            graph.thread.copy(
                remoteSessionId = "remote-session-updated",
                updatedAtEpochMillis = 9_000L,
            ),
        )

        val savedThread = database.aiCoachChatDao().getThread(
            accountId = graph.account.id,
            providerKind = graph.thread.providerKind,
            localAgentKind = graph.thread.localAgentKind,
        )
        assertEquals("remote-session-updated", savedThread?.remoteSessionId)
        assertEquals(9_000L, savedThread?.updatedAtEpochMillis)
        assertEquals(
            listOf(graph.message),
            database.aiCoachChatDao().observeMessages(graph.thread.id).first(),
        )
    }

    @Test
    fun settingsUpsert_preservesAccountThreadAndMessages() = runTest {
        val graph = insertChatGraph()

        database.aiCoachDao().upsertSettings(
            graph.settings.copy(
                modelName = "hermes-agent-v2",
                updatedAtEpochMillis = 9_000L,
            ),
        )

        assertEquals(graph.account, database.accountDao().getAccount(graph.account.id))
        assertEquals("hermes-agent-v2", database.aiCoachDao().getSettings(graph.account.id)?.modelName)
        assertEquals(
            graph.thread,
            database.aiCoachChatDao().getThread(
                accountId = graph.account.id,
                providerKind = graph.thread.providerKind,
                localAgentKind = graph.thread.localAgentKind,
            ),
        )
        assertEquals(
            listOf(graph.message),
            database.aiCoachChatDao().observeMessages(graph.thread.id).first(),
        )
    }

    @Test
    fun messageUpsert_updatesLeafWithoutChangingThread() = runTest {
        val graph = insertChatGraph()

        database.aiCoachChatDao().insertMessage(
            graph.message.copy(
                content = "Updated response",
                status = "Sent",
            ),
        )

        assertEquals(
            graph.thread,
            database.aiCoachChatDao().getThread(
                accountId = graph.account.id,
                providerKind = graph.thread.providerKind,
                localAgentKind = graph.thread.localAgentKind,
            ),
        )
        val savedMessage = database.aiCoachChatDao().observeMessages(graph.thread.id).first().single()
        assertEquals("Updated response", savedMessage.content)
        assertEquals("Sent", savedMessage.status)
    }

    @Test
    fun failedChatMetadataTransaction_rollsBackThreadSettingsAndMessages() = runTest {
        val graph = insertChatGraph()
        var rolledBack = false

        try {
            database.withTransaction {
                database.aiCoachDao().upsertSettings(
                    graph.settings.copy(modelName = "temporary-model", updatedAtEpochMillis = 10_000L),
                )
                database.aiCoachChatDao().upsertThread(
                    graph.thread.copy(remoteSessionId = "temporary-session", updatedAtEpochMillis = 10_000L),
                )
                throw ForcedRollback
            }
        } catch (_: ForcedRollbackException) {
            rolledBack = true
        }

        assertTrue(rolledBack)
        assertEquals(graph.settings, database.aiCoachDao().getSettings(graph.account.id))
        assertEquals(
            graph.thread,
            database.aiCoachChatDao().getThread(
                accountId = graph.account.id,
                providerKind = graph.thread.providerKind,
                localAgentKind = graph.thread.localAgentKind,
            ),
        )
        assertEquals(
            listOf(graph.message),
            database.aiCoachChatDao().observeMessages(graph.thread.id).first(),
        )
    }

    private suspend fun insertChatGraph(): ChatGraph {
        val account = AccountEntity(
            id = "account-ai",
            displayName = "AI Test",
            email = null,
            remoteUserId = null,
            authProvider = "local",
            avatarUrl = null,
            createdAtEpochMillis = 1_000L,
            updatedAtEpochMillis = 1_000L,
        )
        val settings = AiCoachSettingsEntity(
            accountId = account.id,
            providerKind = "LocalAgent",
            baseUrl = "http://127.0.0.1:8080/v1/",
            modelName = "hermes-agent",
            localAgentKind = "HermesAgent",
            apiKeyStored = true,
            updatedAtEpochMillis = 2_000L,
        )
        val thread = AiCoachThreadEntity(
            id = "thread-ai",
            accountId = account.id,
            providerKind = settings.providerKind,
            localAgentKind = settings.localAgentKind,
            remoteSessionId = "remote-session-original",
            createdAtEpochMillis = 3_000L,
            updatedAtEpochMillis = 4_000L,
        )
        val message = AiCoachChatMessageEntity(
            id = "message-ai",
            threadId = thread.id,
            role = "Assistant",
            content = "Original response",
            status = "Sent",
            errorMessage = null,
            createdAtEpochMillis = 5_000L,
        )

        database.accountDao().upsertAccount(account)
        database.aiCoachDao().upsertSettings(settings)
        database.aiCoachChatDao().upsertThread(thread)
        database.aiCoachChatDao().insertMessage(message)
        return ChatGraph(account, settings, thread, message)
    }

    private data class ChatGraph(
        val account: AccountEntity,
        val settings: AiCoachSettingsEntity,
        val thread: AiCoachThreadEntity,
        val message: AiCoachChatMessageEntity,
    )

    private object ForcedRollback : ForcedRollbackException()

    private open class ForcedRollbackException : RuntimeException()
}
