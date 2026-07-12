package com.musfit.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.musfit.data.local.MusFitDatabase
import com.musfit.data.local.entity.AiCoachSettingsEntity
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
        assertEquals(0, secretStore.getCalls)
    }

    @Test
    fun observeSettings_exposesOnlyNonSecretInternalHermesDefaultsWhenAppDataIsEmpty() = runTest {
        repository = LocalAiCoachRepository(
            aiCoachDao = database.aiCoachDao(),
            accountRepository = accountRepository,
            secretStore = secretStore,
            debugDefaults = AiCoachDebugDefaults(
                hermesBaseUrl = "https://192.168.178.113:8443/v1",
                hermesModelName = "hermes-agent",
            ),
            clock = { clockMillis += 1_000L; clockMillis },
        )

        val settings = repository.observeSettings().first()
        assertEquals(AiCoachProviderKind.LocalAgent, settings.providerKind)
        assertEquals("https://192.168.178.113:8443/v1/", settings.baseUrl)
        assertEquals("hermes-agent", settings.modelName)
        assertEquals(LocalAgentKind.HermesAgent, settings.localAgentKind)
        assertFalse(settings.hasApiKey)
        assertNull(database.aiCoachDao().getSettings("local-default"))
        assertNull(repository.activeConnection())
        assertEquals(0, secretStore.getCalls)
    }

    @Test
    fun saveOpenAiCompatibleSettings_persistsNormalizedEndpointAndStoresApiKeyOutsideRoom() = runTest {
        repository.saveSettings(
            AiCoachSettingsInput(
                providerKind = AiCoachProviderKind.OpenAiCompatible,
                baseUrl = "  https://127.0.0.1:11434  ",
                modelName = "  llama3.1  ",
                localAgentKind = LocalAgentKind.Custom,
                apiKey = AiCoachApiKeyUpdate.Replace("sk-local"),
            ),
        )

        val settings = repository.observeSettings().first()
        assertEquals(AiCoachProviderKind.OpenAiCompatible, settings.providerKind)
        assertEquals("https://127.0.0.1:11434/", settings.baseUrl)
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
                baseUrl = "https://192.168.1.40:8443/coach",
                modelName = "",
                localAgentKind = LocalAgentKind.OpenClaw,
                apiKey = AiCoachApiKeyUpdate.Clear,
            ),
        )

        val settings = repository.observeSettings().first()
        assertEquals(AiCoachProviderKind.LocalAgent, settings.providerKind)
        assertEquals("https://192.168.1.40:8443/coach/", settings.baseUrl)
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
                    baseUrl = "https://192.168.178.113:8443/v1",
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
    fun saveHermesAgentSettings_doesNotTreatInternalDefaultsAsRuntimeSecret() = runTest {
        repository = LocalAiCoachRepository(
            aiCoachDao = database.aiCoachDao(),
            accountRepository = accountRepository,
            secretStore = secretStore,
            debugDefaults = AiCoachDebugDefaults(
                hermesBaseUrl = "https://192.168.178.113:8443/v1",
                hermesModelName = "hermes-agent",
            ),
            clock = { clockMillis += 1_000L; clockMillis },
        )

        try {
            repository.saveSettings(
                AiCoachSettingsInput(
                    providerKind = AiCoachProviderKind.LocalAgent,
                    baseUrl = "https://192.168.178.113:8443/v1",
                    modelName = "hermes-agent",
                    localAgentKind = LocalAgentKind.HermesAgent,
                    apiKey = AiCoachApiKeyUpdate.KeepExisting,
                ),
            )
            fail("Expected internal metadata defaults without a runtime key to be rejected")
        } catch (expected: IllegalArgumentException) {
            assertEquals("Hermes agent requires the API_SERVER_KEY bearer token.", expected.message)
        }

        assertNull(database.aiCoachDao().getSettings("local-default"))
        assertNull(secretStore.apiKeyFor("local-default"))
        assertNull(repository.activeConnection())
    }

    @Test
    fun saveHermesAgentSettings_keepsExistingApiServerKeyWhenInputBlank() = runTest {
        repository.saveSettings(
            AiCoachSettingsInput(
                providerKind = AiCoachProviderKind.LocalAgent,
                baseUrl = "https://10.0.2.2:8443/v1",
                modelName = "hermes-agent",
                localAgentKind = LocalAgentKind.HermesAgent,
                apiKey = AiCoachApiKeyUpdate.Replace("first-key"),
            ),
        )

        repository.saveSettings(
            AiCoachSettingsInput(
                providerKind = AiCoachProviderKind.LocalAgent,
                baseUrl = "https://192.168.178.113:8443/v1",
                modelName = "hermes-agent",
                localAgentKind = LocalAgentKind.HermesAgent,
                apiKey = AiCoachApiKeyUpdate.KeepExisting,
            ),
        )

        val connection = repository.activeConnection()
        assertEquals("https://192.168.178.113:8443/v1/", connection?.baseUrl)
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
    fun saveSettings_rejectsPublicHttpBeforeAccountSecretOrDaoSideEffects() = runTest {
        try {
            repository.saveSettings(
                AiCoachSettingsInput(
                    providerKind = AiCoachProviderKind.OpenAiCompatible,
                    baseUrl = "http://api.example.com/v1/",
                    modelName = "dummy-model",
                    localAgentKind = LocalAgentKind.Custom,
                    apiKey = AiCoachApiKeyUpdate.Replace("dummy-never-store"),
                ),
            )
            fail("Expected public HTTP to be rejected")
        } catch (_: IllegalArgumentException) {
            assertNull(database.accountDao().getActiveAccount())
            assertNull(database.aiCoachDao().getSettings("local-default"))
            assertEquals(0, secretStore.saveCalls)
            assertEquals(0, secretStore.clearCalls)
            assertEquals(0, secretStore.getCalls)
        }
    }

    @Test
    fun activeConnection_revalidatesPersistedUrlBeforeReadingSecret() = runTest {
        accountRepository.ensureActiveAccount()
        secretStore.saveApiKey("local-default", "dummy-stale-key")
        secretStore.resetCounters()
        database.aiCoachDao().upsertSettings(
            AiCoachSettingsEntity(
                accountId = "local-default",
                providerKind = AiCoachProviderKind.OpenAiCompatible.name,
                baseUrl = "http://api.example.com/v1/",
                modelName = "dummy-model",
                localAgentKind = LocalAgentKind.Custom.name,
                apiKeyStored = true,
                updatedAtEpochMillis = 1L,
            ),
        )

        try {
            repository.activeConnection()
            fail("Expected stale public HTTP connection to be rejected")
        } catch (_: IllegalArgumentException) {
            assertEquals(0, secretStore.getCalls)
        }
    }

    @Test
    fun observeSettings_hidesCredentialStateForInvalidPersistedUrlWithoutReadingOrClearingSecret() = runTest {
        accountRepository.ensureActiveAccount()
        secretStore.saveApiKey("local-default", "runtime-key-must-remain-unread")
        secretStore.resetCounters()
        database.aiCoachDao().upsertSettings(
            AiCoachSettingsEntity(
                accountId = "local-default",
                providerKind = AiCoachProviderKind.OpenAiCompatible.name,
                baseUrl = "http://api.example.com/v1/",
                modelName = "dummy-model",
                localAgentKind = LocalAgentKind.Custom.name,
                apiKeyStored = true,
                updatedAtEpochMillis = 1L,
            ),
        )

        val settings = repository.observeSettings().first()

        assertFalse(settings.hasApiKey)
        assertTrue(database.aiCoachDao().getSettings("local-default")!!.apiKeyStored)
        assertEquals("runtime-key-must-remain-unread", secretStore.apiKeyFor("local-default"))
        assertEquals(0, secretStore.getCalls)
        assertEquals(0, secretStore.clearCalls)
    }

    @Test
    fun activeConnection_reconcilesLegacyStoredFlagWhenRuntimeSecretIsMissing() = runTest {
        accountRepository.ensureActiveAccount()
        database.aiCoachDao().upsertSettings(
            AiCoachSettingsEntity(
                accountId = "local-default",
                providerKind = AiCoachProviderKind.LocalAgent.name,
                baseUrl = "https://192.168.178.113:8443/v1/",
                modelName = "hermes-agent",
                localAgentKind = LocalAgentKind.HermesAgent.name,
                apiKeyStored = true,
                updatedAtEpochMillis = 1L,
            ),
        )

        assertNull(repository.activeConnection())
        assertFalse(database.aiCoachDao().getSettings("local-default")!!.apiKeyStored)
        assertEquals(1, secretStore.getCalls)
        assertEquals(1, secretStore.clearCalls)
    }

    @Test
    fun observeSettings_reconcilesLegacyStoredFlagWhenRuntimeSecretIsMissing() = runTest {
        accountRepository.ensureActiveAccount()
        database.aiCoachDao().upsertSettings(
            AiCoachSettingsEntity(
                accountId = "local-default",
                providerKind = AiCoachProviderKind.LocalAgent.name,
                baseUrl = "https://192.168.178.113:8443/v1/",
                modelName = "hermes-agent",
                localAgentKind = LocalAgentKind.HermesAgent.name,
                apiKeyStored = true,
                updatedAtEpochMillis = 1L,
            ),
        )

        val settings = repository.observeSettings().first()

        assertFalse(settings.hasApiKey)
        assertFalse(database.aiCoachDao().getSettings("local-default")!!.apiKeyStored)
        assertTrue("The missing runtime secret should be checked at least once", secretStore.getCalls >= 1)
        assertEquals(1, secretStore.clearCalls)
    }

    @Test
    fun observeSettings_clearsBlankRuntimeSecretAndStoredFlag() = runTest {
        accountRepository.ensureActiveAccount()
        secretStore.saveApiKey("local-default", "   ")
        secretStore.resetCounters()
        database.aiCoachDao().upsertSettings(
            AiCoachSettingsEntity(
                accountId = "local-default",
                providerKind = AiCoachProviderKind.LocalAgent.name,
                baseUrl = "https://192.168.178.113:8443/v1/",
                modelName = "hermes-agent",
                localAgentKind = LocalAgentKind.HermesAgent.name,
                apiKeyStored = true,
                updatedAtEpochMillis = 1L,
            ),
        )

        val settings = repository.observeSettings().first()

        assertFalse(settings.hasApiKey)
        assertFalse(database.aiCoachDao().getSettings("local-default")!!.apiKeyStored)
        assertNull(secretStore.apiKeyFor("local-default"))
        assertTrue("The blank runtime secret should be read at least once", secretStore.getCalls >= 1)
        assertEquals(1, secretStore.clearCalls)
    }

    @Test
    fun observeSettings_reconcilesMissingStoredFlagWhenRuntimeSecretExists() = runTest {
        accountRepository.ensureActiveAccount()
        secretStore.saveApiKey("local-default", "runtime-key")
        secretStore.resetCounters()
        database.aiCoachDao().upsertSettings(
            AiCoachSettingsEntity(
                accountId = "local-default",
                providerKind = AiCoachProviderKind.LocalAgent.name,
                baseUrl = "https://192.168.178.113:8443/v1/",
                modelName = "hermes-agent",
                localAgentKind = LocalAgentKind.HermesAgent.name,
                apiKeyStored = false,
                updatedAtEpochMillis = 1L,
            ),
        )

        val settings = repository.observeSettings().first()

        assertTrue("Observed settings should expose the runtime key", settings.hasApiKey)
        assertTrue(
            "Reconciliation should persist the runtime-key flag",
            database.aiCoachDao().getSettings("local-default")!!.apiKeyStored,
        )
        assertTrue("The runtime secret should be read at least once", secretStore.getCalls >= 1)
    }

    @Test
    fun activeConnection_reconcilesMissingStoredFlagWhenRuntimeSecretExists() = runTest {
        accountRepository.ensureActiveAccount()
        secretStore.saveApiKey("local-default", "runtime-key")
        secretStore.resetCounters()
        database.aiCoachDao().upsertSettings(
            AiCoachSettingsEntity(
                accountId = "local-default",
                providerKind = AiCoachProviderKind.LocalAgent.name,
                baseUrl = "https://192.168.178.113:8443/v1/",
                modelName = "hermes-agent",
                localAgentKind = LocalAgentKind.HermesAgent.name,
                apiKeyStored = false,
                updatedAtEpochMillis = 1L,
            ),
        )

        assertEquals("runtime-key", repository.activeConnection()?.apiKey)
        assertTrue(database.aiCoachDao().getSettings("local-default")!!.apiKeyStored)
        assertEquals(1, secretStore.getCalls)
    }

    @Test
    fun saveHermesSettings_keepsRuntimeSecretWhenStoredFlagIsMissing() = runTest {
        accountRepository.ensureActiveAccount()
        secretStore.saveApiKey("local-default", "runtime-key")
        secretStore.resetCounters()
        database.aiCoachDao().upsertSettings(
            AiCoachSettingsEntity(
                accountId = "local-default",
                providerKind = AiCoachProviderKind.LocalAgent.name,
                baseUrl = "https://192.168.178.113:8443/v1/",
                modelName = "hermes-agent",
                localAgentKind = LocalAgentKind.HermesAgent.name,
                apiKeyStored = false,
                updatedAtEpochMillis = 1L,
            ),
        )

        repository.saveSettings(
            AiCoachSettingsInput(
                providerKind = AiCoachProviderKind.LocalAgent,
                baseUrl = "https://192.168.178.113:8443/v1/",
                modelName = "hermes-agent",
                localAgentKind = LocalAgentKind.HermesAgent,
                apiKey = AiCoachApiKeyUpdate.KeepExisting,
            ),
        )

        assertTrue(database.aiCoachDao().getSettings("local-default")!!.apiKeyStored)
        assertEquals("runtime-key", repository.activeConnection()?.apiKey)
        assertEquals(2, secretStore.getCalls)
    }

    @Test
    fun observeSettings_clearsRuntimeSecretForDisabledStoredRow() = runTest {
        accountRepository.ensureActiveAccount()
        secretStore.saveApiKey("local-default", "orphan-runtime-key")
        secretStore.resetCounters()
        database.aiCoachDao().upsertSettings(
            AiCoachSettingsEntity(
                accountId = "local-default",
                providerKind = AiCoachProviderKind.Disabled.name,
                baseUrl = "",
                modelName = "",
                localAgentKind = LocalAgentKind.Custom.name,
                apiKeyStored = true,
                updatedAtEpochMillis = 1L,
            ),
        )

        val settings = repository.observeSettings().first()

        assertFalse(settings.hasApiKey)
        assertFalse(database.aiCoachDao().getSettings("local-default")!!.apiKeyStored)
        assertNull(secretStore.apiKeyFor("local-default"))
        assertEquals(0, secretStore.getCalls)
        assertTrue("The orphan runtime secret should be cleared at least once", secretStore.clearCalls >= 1)
    }

    @Test
    fun activeConnection_clearsRuntimeSecretAndStoredFlagForDisabledRowWithoutReadingIt() = runTest {
        accountRepository.ensureActiveAccount()
        secretStore.saveApiKey("local-default", "orphan-runtime-key")
        secretStore.resetCounters()
        database.aiCoachDao().upsertSettings(
            AiCoachSettingsEntity(
                accountId = "local-default",
                providerKind = AiCoachProviderKind.Disabled.name,
                baseUrl = "",
                modelName = "",
                localAgentKind = LocalAgentKind.Custom.name,
                apiKeyStored = true,
                updatedAtEpochMillis = 1L,
            ),
        )

        assertNull(repository.activeConnection())
        assertFalse(database.aiCoachDao().getSettings("local-default")!!.apiKeyStored)
        assertNull(secretStore.apiKeyFor("local-default"))
        assertEquals(0, secretStore.getCalls)
        assertEquals(1, secretStore.clearCalls)
    }

    @Test
    fun saveHermesSettings_rejectsAndReconcilesLegacyStoredFlagWhenRuntimeSecretIsMissing() = runTest {
        accountRepository.ensureActiveAccount()
        database.aiCoachDao().upsertSettings(
            AiCoachSettingsEntity(
                accountId = "local-default",
                providerKind = AiCoachProviderKind.LocalAgent.name,
                baseUrl = "https://192.168.178.113:8443/v1/",
                modelName = "hermes-agent",
                localAgentKind = LocalAgentKind.HermesAgent.name,
                apiKeyStored = true,
                updatedAtEpochMillis = 1L,
            ),
        )

        try {
            repository.saveSettings(
                AiCoachSettingsInput(
                    providerKind = AiCoachProviderKind.LocalAgent,
                    baseUrl = "https://192.168.178.113:8443/v1/",
                    modelName = "hermes-agent",
                    localAgentKind = LocalAgentKind.HermesAgent,
                    apiKey = AiCoachApiKeyUpdate.KeepExisting,
                ),
            )
            fail("Expected the missing runtime API_SERVER_KEY to be rejected")
        } catch (expected: IllegalArgumentException) {
            assertEquals("Hermes agent requires the API_SERVER_KEY bearer token.", expected.message)
        }

        assertFalse(database.aiCoachDao().getSettings("local-default")!!.apiKeyStored)
        assertEquals(1, secretStore.getCalls)
        assertEquals(1, secretStore.clearCalls)
    }

    @Test
    fun invalidDebugDefaultIsNotExposedAsSettingsOrConnection() = runTest {
        repository = LocalAiCoachRepository(
            aiCoachDao = database.aiCoachDao(),
            accountRepository = accountRepository,
            secretStore = secretStore,
            debugDefaults = AiCoachDebugDefaults(
                hermesBaseUrl = "http://api.example.com/v1/",
                hermesModelName = "hermes-agent",
            ),
            clock = { clockMillis += 1_000L; clockMillis },
        )

        assertEquals(AiCoachProviderKind.Disabled, repository.observeSettings().first().providerKind)
        assertNull(repository.activeConnection())
        assertEquals(0, secretStore.getCalls)
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
    fun clearApiKey_disablesPersistedHermesConnection() = runTest {
        repository.saveSettings(
            AiCoachSettingsInput(
                providerKind = AiCoachProviderKind.LocalAgent,
                baseUrl = "https://192.168.178.113:8443/v1",
                modelName = "hermes-agent",
                localAgentKind = LocalAgentKind.HermesAgent,
                apiKey = AiCoachApiKeyUpdate.Replace("runtime-key"),
            ),
        )

        repository.clearApiKey()

        val settings = repository.observeSettings().first()
        assertEquals(AiCoachProviderKind.LocalAgent, settings.providerKind)
        assertFalse(settings.hasApiKey)
        assertNull(repository.activeConnection())
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
                baseUrl = "https://10.0.2.2:8443",
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
        var saveCalls = 0
            private set
        var getCalls = 0
            private set
        var clearCalls = 0
            private set

        override suspend fun saveApiKey(accountId: String, apiKey: String) {
            saveCalls += 1
            keys[accountId] = apiKey
        }

        override suspend fun getApiKey(accountId: String): String? {
            getCalls += 1
            return keys[accountId]
        }

        override suspend fun clearApiKey(accountId: String) {
            clearCalls += 1
            keys.remove(accountId)
        }

        fun apiKeyFor(accountId: String): String? = keys[accountId]

        fun resetCounters() {
            saveCalls = 0
            getCalls = 0
            clearCalls = 0
        }
    }
}
