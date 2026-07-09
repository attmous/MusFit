package com.musfit.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.musfit.data.local.MusFitDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalAiCoachRepositoryTest {
    private lateinit var database: MusFitDatabase
    private lateinit var accountRepository: LocalAccountRepository
    private lateinit var secretStore: FakeAiCoachSecretStore
    private lateinit var repository: LocalAiCoachRepository
    private var clockMillis = 10_000L

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
        repository = LocalAiCoachRepository(
            aiCoachDao = database.aiCoachDao(),
            accountRepository = accountRepository,
            secretStore = secretStore,
            clock = { clockMillis += 1_000L; clockMillis },
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun observeSettings_returnsDisabledDefaultsWhenEmpty() = runTest {
        val settings = repository.observeSettings().first()

        assertEquals(AiCoachProviderKind.Disabled, settings.providerKind)
        assertEquals("", settings.baseUrl)
        assertEquals("", settings.modelName)
        assertEquals(LocalAgentKind.Custom, settings.localAgentKind)
        assertFalse(settings.hasApiKey)
        assertNull(repository.activeConnection())
    }

    @Test
    fun saveOpenAiCompatibleSettings_persistsNormalizedEndpointAndStoresApiKeyOutsideRoom() = runTest {
        repository.saveSettings(
            AiCoachSettingsInput(
                providerKind = AiCoachProviderKind.OpenAiCompatible,
                baseUrl = "  http://127.0.0.1:11434  ",
                modelName = "  llama3.1  ",
                localAgentKind = LocalAgentKind.Custom,
                apiKey = AiCoachApiKeyUpdate.Replace("sk-local"),
            ),
        )

        val settings = repository.observeSettings().first()
        assertEquals(AiCoachProviderKind.OpenAiCompatible, settings.providerKind)
        assertEquals("http://127.0.0.1:11434/", settings.baseUrl)
        assertEquals("llama3.1", settings.modelName)
        assertTrue(settings.hasApiKey)
        assertEquals("sk-local", secretStore.apiKeyFor("local-default"))

        val row = database.aiCoachDao().getSettings("local-default")
        assertEquals(true, row?.apiKeyStored)
        assertFalse(row.toString().contains("sk-local"))

        val connection = repository.activeConnection()
        assertEquals(AiCoachProviderKind.OpenAiCompatible, connection?.providerKind)
        assertEquals("sk-local", connection?.apiKey)
    }

    @Test
    fun saveLocalAgentSettings_allowsOpenClawWithoutApiKey() = runTest {
        repository.saveSettings(
            AiCoachSettingsInput(
                providerKind = AiCoachProviderKind.LocalAgent,
                baseUrl = "http://192.168.1.40:8787/coach",
                modelName = "",
                localAgentKind = LocalAgentKind.OpenClaw,
                apiKey = AiCoachApiKeyUpdate.Clear,
            ),
        )

        val settings = repository.observeSettings().first()
        assertEquals(AiCoachProviderKind.LocalAgent, settings.providerKind)
        assertEquals("http://192.168.1.40:8787/coach/", settings.baseUrl)
        assertEquals("", settings.modelName)
        assertEquals(LocalAgentKind.OpenClaw, settings.localAgentKind)
        assertFalse(settings.hasApiKey)

        val connection = repository.activeConnection()
        assertEquals(AiCoachProviderKind.LocalAgent, connection?.providerKind)
        assertEquals(LocalAgentKind.OpenClaw, connection?.localAgentKind)
        assertNull(connection?.apiKey)
    }

    @Test
    fun saveHermesAgentSettings_requiresApiServerKey() = runTest {
        try {
            repository.saveSettings(
                AiCoachSettingsInput(
                    providerKind = AiCoachProviderKind.LocalAgent,
                    baseUrl = "http://192.168.178.113:8080/v1",
                    modelName = "hermes-agent",
                    localAgentKind = LocalAgentKind.HermesAgent,
                    apiKey = AiCoachApiKeyUpdate.KeepExisting,
                ),
            )
            fail("Expected Hermes settings without API_SERVER_KEY to be rejected")
        } catch (expected: IllegalArgumentException) {
            assertEquals("Hermes agent requires the API_SERVER_KEY bearer token.", expected.message)
        }

        assertEquals(AiCoachProviderKind.Disabled, repository.observeSettings().first().providerKind)
        assertNull(repository.activeConnection())
    }

    @Test
    fun saveHermesAgentSettings_keepsExistingApiServerKeyWhenInputBlank() = runTest {
        repository.saveSettings(
            AiCoachSettingsInput(
                providerKind = AiCoachProviderKind.LocalAgent,
                baseUrl = "http://10.0.2.2:8080/v1",
                modelName = "hermes-agent",
                localAgentKind = LocalAgentKind.HermesAgent,
                apiKey = AiCoachApiKeyUpdate.Replace("first-key"),
            ),
        )

        repository.saveSettings(
            AiCoachSettingsInput(
                providerKind = AiCoachProviderKind.LocalAgent,
                baseUrl = "http://192.168.178.113:8080/v1",
                modelName = "hermes-agent",
                localAgentKind = LocalAgentKind.HermesAgent,
                apiKey = AiCoachApiKeyUpdate.KeepExisting,
            ),
        )

        val connection = repository.activeConnection()
        assertEquals("http://192.168.178.113:8080/v1/", connection?.baseUrl)
        assertEquals("first-key", connection?.apiKey)
    }

    @Test
    fun saveSettings_rejectsInvalidUrlAndDoesNotPersist() = runTest {
        try {
            repository.saveSettings(
                AiCoachSettingsInput(
                    providerKind = AiCoachProviderKind.OpenAiCompatible,
                    baseUrl = "not a url",
                    modelName = "gpt-4.1-mini",
                    localAgentKind = LocalAgentKind.Custom,
                    apiKey = AiCoachApiKeyUpdate.KeepExisting,
                ),
            )
            fail("Expected invalid base URL to be rejected")
        } catch (expected: IllegalArgumentException) {
            assertEquals("Enter a valid http(s) base URL.", expected.message)
        }

        assertEquals(AiCoachProviderKind.Disabled, repository.observeSettings().first().providerKind)
    }

    @Test
    fun saveOpenAiCompatibleSettings_requiresModelName() = runTest {
        try {
            repository.saveSettings(
                AiCoachSettingsInput(
                    providerKind = AiCoachProviderKind.OpenAiCompatible,
                    baseUrl = "https://api.example.com",
                    modelName = "   ",
                    localAgentKind = LocalAgentKind.Custom,
                    apiKey = AiCoachApiKeyUpdate.KeepExisting,
                ),
            )
            fail("Expected missing model to be rejected")
        } catch (expected: IllegalArgumentException) {
            assertEquals("Model name is required for API-compatible providers.", expected.message)
        }
    }

    @Test
    fun clearApiKey_updatesPersistedFlagAndConnection() = runTest {
        repository.saveSettings(
            AiCoachSettingsInput(
                providerKind = AiCoachProviderKind.OpenAiCompatible,
                baseUrl = "https://api.example.com/v1",
                modelName = "gpt-4.1-mini",
                localAgentKind = LocalAgentKind.Custom,
                apiKey = AiCoachApiKeyUpdate.Replace("sk-example"),
            ),
        )

        repository.clearApiKey()

        val settings = repository.observeSettings().first()
        assertFalse(settings.hasApiKey)
        assertNull(secretStore.apiKeyFor("local-default"))
        assertNull(repository.activeConnection()?.apiKey)
    }

    @Test
    fun settingsFollowActiveLocalAccount() = runTest {
        repository.saveSettings(
            AiCoachSettingsInput(
                providerKind = AiCoachProviderKind.OpenAiCompatible,
                baseUrl = "https://api.first.example",
                modelName = "first-model",
                localAgentKind = LocalAgentKind.Custom,
                apiKey = AiCoachApiKeyUpdate.Replace("first-key"),
            ),
        )

        val secondId = accountRepository.createAccount(displayName = "Partner", email = null)
        accountRepository.switchAccount(secondId)
        repository.saveSettings(
            AiCoachSettingsInput(
                providerKind = AiCoachProviderKind.LocalAgent,
                baseUrl = "http://10.0.2.2:8989",
                modelName = "",
                localAgentKind = LocalAgentKind.HermesAgent,
                apiKey = AiCoachApiKeyUpdate.Replace("second-key"),
            ),
        )

        assertEquals(LocalAgentKind.HermesAgent, repository.observeSettings().first().localAgentKind)
        assertEquals("second-key", repository.activeConnection()?.apiKey)

        accountRepository.switchAccount("local-default")
        val firstSettings = repository.observeSettings().first()
        assertEquals(AiCoachProviderKind.OpenAiCompatible, firstSettings.providerKind)
        assertEquals("first-model", firstSettings.modelName)
        assertEquals("first-key", repository.activeConnection()?.apiKey)
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

        fun apiKeyFor(accountId: String): String? = keys[accountId]
    }
}
