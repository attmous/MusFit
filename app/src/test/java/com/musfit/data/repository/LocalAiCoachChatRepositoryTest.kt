package com.musfit.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.musfit.data.local.MusFitDatabase
import com.musfit.data.remote.coach.CoachCompletionClient
import com.musfit.data.remote.coach.HermesChatRequest
import com.musfit.data.remote.coach.HermesChatResponse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalAiCoachChatRepositoryTest {
    private lateinit var database: MusFitDatabase
    private lateinit var accountRepository: LocalAccountRepository
    private lateinit var aiCoachRepository: LocalAiCoachRepository
    private lateinit var secretStore: FakeAiCoachSecretStore
    private lateinit var completionClient: FakeCoachCompletionClient
    private lateinit var repository: LocalAiCoachChatRepository
    private var clockMillis = 20_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MusFitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        accountRepository = LocalAccountRepository(
            accountDao = database.accountDao(),
            clock = { clockMillis += 1_000L; clockMillis },
        )
        secretStore = FakeAiCoachSecretStore()
        aiCoachRepository = LocalAiCoachRepository(
            aiCoachDao = database.aiCoachDao(),
            accountRepository = accountRepository,
            secretStore = secretStore,
            clock = { clockMillis += 1_000L; clockMillis },
        )
        completionClient = FakeCoachCompletionClient()
        repository = LocalAiCoachChatRepository(
            chatDao = database.aiCoachChatDao(),
            accountRepository = accountRepository,
            aiCoachRepository = aiCoachRepository,
            coachCompletionClient = completionClient,
            clock = { clockMillis += 1_000L; clockMillis },
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun sendMessage_persistsUserAndAssistantReplyWithRadxaHermesConnection() = runTest {
        saveHermesConnection()

        repository.sendMessage("What should I focus on tonight?", systemPrompt = "read-only context")

        val messages = repository.observeMessages().first()
        assertEquals(2, messages.size)
        assertEquals(AiCoachChatRole.User, messages[0].role)
        assertEquals("What should I focus on tonight?", messages[0].content)
        assertEquals(AiCoachChatRole.Assistant, messages[1].role)
        assertEquals("Hydrate, eat protein, and start Lower Strength.", messages[1].content)
        assertEquals(AiCoachChatMessageStatus.Sent, messages[1].status)

        val request = completionClient.requests.single()
        assertEquals("http://192.168.178.113:8080/v1/", request.connection.baseUrl)
        assertEquals("hermes-agent", request.connection.modelName)
        assertEquals("radxa-key", request.connection.apiKey)
        assertEquals("read-only context", request.systemPrompt)
        assertEquals(listOf("user"), request.messages.map { it.role })
        assertEquals(listOf("What should I focus on tonight?"), request.messages.map { it.content })

        val thread = database.aiCoachChatDao().getThread(
            accountId = "local-default",
            providerKind = AiCoachProviderKind.LocalAgent.name,
            localAgentKind = LocalAgentKind.HermesAgent.name,
        )
        assertNotNull(thread)
        assertEquals("radxa-session-1", thread?.remoteSessionId)
    }

    @Test
    fun sendMessage_usesRemoteSessionForFollowUpWithoutResendingFullHistory() = runTest {
        saveHermesConnection()
        repository.sendMessage("First question", systemPrompt = "context")
        completionClient.reply = HermesChatResponse("Second reply", remoteSessionId = "radxa-session-1")

        repository.sendMessage("Second question", systemPrompt = "context")

        val followUp = completionClient.requests.last()
        assertEquals("radxa-session-1", followUp.remoteSessionId)
        assertEquals(listOf("Second question"), followUp.messages.map { it.content })
    }

    private suspend fun saveHermesConnection() {
        aiCoachRepository.saveSettings(
            AiCoachSettingsInput(
                providerKind = AiCoachProviderKind.LocalAgent,
                baseUrl = "http://192.168.178.113:8080/v1",
                modelName = "hermes-agent",
                localAgentKind = LocalAgentKind.HermesAgent,
                apiKey = AiCoachApiKeyUpdate.Replace("radxa-key"),
            ),
        )
    }

    private class FakeCoachCompletionClient : CoachCompletionClient {
        val requests = mutableListOf<HermesChatRequest>()
        var reply = HermesChatResponse("Hydrate, eat protein, and start Lower Strength.", remoteSessionId = "radxa-session-1")

        override suspend fun testConnection(connection: AiCoachConnection) = Unit

        override suspend fun chat(request: HermesChatRequest): HermesChatResponse {
            requests += request
            return reply
        }
    }

    private class FakeAiCoachSecretStore : AiCoachSecretStore {
        private val keys = mutableMapOf<String, String>()

        override suspend fun saveApiKey(accountId: String, apiKey: String) {
            keys[accountId] = apiKey
        }

        override suspend fun getApiKey(accountId: String): String? = keys[accountId]

        override suspend fun clearApiKey(accountId: String) {
            keys.remove(accountId)
        }
    }
}
