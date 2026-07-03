package com.musfit.ui.profile

import com.musfit.data.local.entity.DailyHealthSummaryEntity
import com.musfit.data.repository.Account
import com.musfit.data.repository.AccountAuthProvider
import com.musfit.data.repository.AccountRepository
import com.musfit.data.repository.AiCoachApiKeyUpdate
import com.musfit.data.repository.AiCoachConnection
import com.musfit.data.repository.AiCoachProviderKind
import com.musfit.data.repository.AiCoachRepository
import com.musfit.data.repository.AiCoachSettings
import com.musfit.data.repository.AiCoachSettingsInput
import com.musfit.data.repository.AppSettings
import com.musfit.data.repository.BodyMeasurement
import com.musfit.data.repository.DEFAULT_APP_SETTINGS
import com.musfit.data.repository.DEFAULT_USER_PROFILE
import com.musfit.data.repository.ExternalAuthRepository
import com.musfit.data.repository.ExternalAccountProfile
import com.musfit.data.repository.GitHubDeviceAuthorization
import com.musfit.data.repository.HealthConnectRefreshResult
import com.musfit.data.repository.HealthRepository
import com.musfit.data.repository.LocalAgentKind
import com.musfit.data.repository.ProfileRepository
import com.musfit.data.repository.UserProfile
import com.musfit.data.repository.WeightEntry
import com.musfit.domain.health.HealthConnectAvailability
import com.musfit.domain.health.HealthConnectStatus
import com.musfit.domain.health.ImportedDailyHealthSummary
import com.musfit.domain.profile.ActivityLevel
import com.musfit.domain.profile.GoalType
import com.musfit.domain.profile.RecommendedTargets
import com.musfit.domain.profile.Sex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileSettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private fun settingsViewModel(
        healthRepository: HealthRepository = FakeHealthRepository(),
        accountRepository: AccountRepository = FakeAccountRepository(),
        profileRepository: ProfileRepository = FakeProfileRepository(),
        externalAuthRepository: ExternalAuthRepository = FakeExternalAuthRepository(),
        aiCoachRepository: AiCoachRepository = FakeAiCoachRepository(),
    ) = ProfileSettingsViewModel(
        healthRepository,
        accountRepository,
        profileRepository,
        externalAuthRepository,
        aiCoachRepository,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun refreshStatus_showsAvailableWhenGatewayAvailable() = runTest {
        val viewModel = settingsViewModel()

        viewModel.refreshStatus()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Available", viewModel.state.value.availabilityLabel)
        assertEquals(1, viewModel.state.value.grantedPermissionCount)
        assertEquals(setOf("steps"), viewModel.state.value.requestablePermissions)
        assertEquals(true, viewModel.state.value.canRequestPermissions)
    }

    @Test
    fun refreshStatus_showsInstallMessageWhenGatewayUnavailable() = runTest {
        val viewModel = settingsViewModel(
            healthRepository = FakeHealthRepository(
                status = HealthConnectStatus(
                    availability = HealthConnectAvailability.NotInstalled,
                    grantedPermissions = emptySet(),
                ),
            ),
        )

        viewModel.refreshStatus()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Install or update required", viewModel.state.value.availabilityLabel)
        assertEquals(0, viewModel.state.value.grantedPermissionCount)
        assertTrue(viewModel.state.value.requestablePermissions.isEmpty())
        assertEquals(1, viewModel.state.value.requestablePermissionCount)
        assertEquals(false, viewModel.state.value.canRequestPermissions)
        assertEquals(
            "Install or update Health Connect to sync health data with MusFit.",
            viewModel.state.value.message,
        )
    }

    @Test
    fun refreshStatus_hidesPermissionLauncherWhenGatewayNotSupported() = runTest {
        val viewModel = settingsViewModel(
            healthRepository = FakeHealthRepository(
                status = HealthConnectStatus(
                    availability = HealthConnectAvailability.NotSupported,
                    grantedPermissions = emptySet(),
                ),
                requestablePermissions = setOf("steps", "weight"),
            ),
        )

        viewModel.refreshStatus()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Not supported", viewModel.state.value.availabilityLabel)
        assertTrue(viewModel.state.value.requestablePermissions.isEmpty())
        assertEquals(2, viewModel.state.value.requestablePermissionCount)
        assertEquals(false, viewModel.state.value.canRequestPermissions)
    }

    @Test
    fun refreshStatus_showsEnableSyncMessageWhenNoPermissionsGranted() = runTest {
        val viewModel = settingsViewModel(
            healthRepository = FakeHealthRepository(
                status = HealthConnectStatus(
                    availability = HealthConnectAvailability.Available,
                    grantedPermissions = emptySet(),
                ),
                requestablePermissions = setOf("steps", "weight"),
            ),
        )

        viewModel.refreshStatus()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            "No Health Connect permissions are granted. Tap Enable Health Connect sync to choose what MusFit can access.",
            viewModel.state.value.message,
        )
        assertEquals(setOf("steps", "weight"), viewModel.state.value.requestablePermissions)
    }

    @Test
    fun importToday_persistsAndReportsImportedSummary() = runTest {
        val repository = FakeHealthRepository()
        val viewModel = settingsViewModel(healthRepository = repository)

        viewModel.importToday()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(LocalDate.now(), repository.importedDate)
        assertEquals("Imported 1200 steps, 100 kcal, 7h 30m sleep, and 35 min exercise from Health Connect.", viewModel.state.value.message)
    }

    @Test
    fun syncRecentHealthData_refreshesRecentWindowAndReportsResult() = runTest {
        val repository = FakeHealthRepository(refreshResult = HealthConnectRefreshResult(7, 2))
        val viewModel = settingsViewModel(healthRepository = repository)

        viewModel.syncRecentHealthData()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(LocalDate.now(), repository.refreshDate)
        assertFalse(viewModel.state.value.isHealthConnectSyncing)
        assertEquals("Synced 7 days and 2 body metrics from Health Connect.", viewModel.state.value.message)
    }

    @Test
    fun exportLatestWorkout_reportsExportedWorkoutRecord() = runTest {
        val repository = FakeHealthRepository(exportedRecordId = "record-id")
        val viewModel = settingsViewModel(healthRepository = repository)

        viewModel.exportLatestWorkout()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.exportCalls)
        assertEquals("Exported latest workout to Health Connect.", viewModel.state.value.message)
    }

    @Test
    fun exportLatestWorkout_reportsNoWorkoutWhenRepositoryReturnsNull() = runTest {
        val repository = FakeHealthRepository(exportedRecordId = null)
        val viewModel = settingsViewModel(healthRepository = repository)

        viewModel.exportLatestWorkout()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("No workout was exported. Check permissions and log a workout first.", viewModel.state.value.message)
    }

    @Test
    fun refreshStatus_resetsStaleSuccessStateAfterFailure() = runTest {
        val repository = FakeHealthRepository()
        val viewModel = settingsViewModel(healthRepository = repository)

        viewModel.refreshStatus()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Available", viewModel.state.value.availabilityLabel)
        assertEquals(1, viewModel.state.value.grantedPermissionCount)
        assertEquals(setOf("steps"), viewModel.state.value.requestablePermissions)
        assertEquals(true, viewModel.state.value.canRequestPermissions)

        repository.statusException = IllegalStateException("boom")
        viewModel.refreshStatus()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Unknown", viewModel.state.value.availabilityLabel)
        assertEquals(0, viewModel.state.value.grantedPermissionCount)
        assertEquals(0, viewModel.state.value.requestablePermissionCount)
        assertTrue(viewModel.state.value.requestablePermissions.isEmpty())
        assertEquals(false, viewModel.state.value.canRequestPermissions)
        assertEquals(
            "Unable to refresh Health Connect status right now. Try again from the Profile tab.",
            viewModel.state.value.message,
        )
    }

    @Test
    fun accountState_exposesActiveLocalAccount() = runTest {
        val accountRepository = FakeAccountRepository(
            initial = Account(
                id = "account-1",
                displayName = "Ava",
                email = "ava@example.com",
                remoteUserId = null,
            ),
        )

        val viewModel = settingsViewModel(accountRepository = accountRepository)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(accountRepository.ensured) // init must guarantee an active account row exists
        assertEquals("Ava", viewModel.state.value.account.displayName)
        assertEquals("ava@example.com", viewModel.state.value.account.email)
        assertEquals("Local account", viewModel.state.value.account.providerLabel)
    }

    @Test
    fun init_ensureActiveAccountFailureSurfacesMessage() = runTest {
        val accountRepository = FakeAccountRepository()
        accountRepository.ensureError = IllegalStateException("account table corrupt")

        val viewModel = settingsViewModel(accountRepository = accountRepository)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("account table corrupt", viewModel.state.value.message)
    }

    @Test
    fun saveProfile_failureSurfacesMessage() = runTest {
        val profileRepo = FakeProfileRepository()
        profileRepo.saveProfileError = IllegalStateException("disk full")
        val viewModel = settingsViewModel(profileRepository = profileRepo)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.saveProfile(DEFAULT_USER_PROFILE)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("disk full", viewModel.state.value.message)
    }

    @Test
    fun saveAccount_updatesRepositoryAndClosesEditor() = runTest {
        val accountRepository = FakeAccountRepository()
        val viewModel = settingsViewModel(accountRepository = accountRepository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openAccountEditor()
        viewModel.onAccountNameChanged("Ava")
        viewModel.onAccountEmailChanged("ava@example.com")
        viewModel.saveAccount()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Ava", accountRepository.updatedName)
        assertEquals("ava@example.com", accountRepository.updatedEmail)
        assertEquals(false, viewModel.state.value.accountEditorOpen)
        assertEquals(null, viewModel.state.value.accountErrorMessage)
    }

    @Test
    fun signInWithProvider_updatesRepositoryAndShowsLinkedProvider() = runTest {
        val accountRepository = FakeAccountRepository()
        val viewModel = settingsViewModel(accountRepository = accountRepository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.signInWithProvider(
            ExternalAccountProfile(
                provider = AccountAuthProvider.Google,
                providerUserId = "google-sub-1",
                displayName = "Ava",
                email = "ava@gmail.com",
                avatarUrl = "https://example.com/avatar.png",
            ),
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(AccountAuthProvider.Google, accountRepository.linkedProfile?.provider)
        assertEquals("Ava", viewModel.state.value.account.displayName)
        assertEquals("ava@gmail.com", viewModel.state.value.account.email)
        assertEquals("Google", viewModel.state.value.account.providerLabel)
        assertEquals("Signed in with Google.", viewModel.state.value.message)
    }

    @Test
    fun signInWithProvider_failureSurfacesMessage() = runTest {
        val accountRepository = FakeAccountRepository()
        accountRepository.signInError = IllegalStateException("Google sign-in failed")
        val viewModel = settingsViewModel(accountRepository = accountRepository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.signInWithProvider(
            ExternalAccountProfile(
                provider = AccountAuthProvider.Google,
                providerUserId = "google-sub-1",
                displayName = "Ava",
                email = "ava@gmail.com",
                avatarUrl = null,
            ),
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Google sign-in failed", viewModel.state.value.message)
    }

    @Test
    fun signInWithGitHub_exposesDeviceCodeAndLinksAccount() = runTest {
        val accountRepository = FakeAccountRepository()
        val externalAuthRepository = FakeExternalAuthRepository(
            profile = ExternalAccountProfile(
                provider = AccountAuthProvider.GitHub,
                providerUserId = "42",
                displayName = "octocat",
                email = "octo@github.com",
                avatarUrl = "https://avatars.githubusercontent.com/u/42",
            ),
        )
        val viewModel = settingsViewModel(
            accountRepository = accountRepository,
            externalAuthRepository = externalAuthRepository,
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.signInWithGitHub()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("WDJB-MJHT", viewModel.state.value.githubDeviceCode?.userCode)
        assertEquals("https://github.com/login/device", viewModel.state.value.githubDeviceCode?.verificationUri)
        assertEquals(AccountAuthProvider.GitHub, accountRepository.linkedProfile?.provider)
        assertEquals("Signed in with GitHub.", viewModel.state.value.message)
    }

    @Test
    fun providerSignInActions_explainMissingProviderConfiguration() {
        val actions = providerSignInActions(
            googleConfigured = false,
            githubConfigured = false,
            githubBusy = false,
        )

        assertEquals(false, actions.google.enabled)
        assertEquals("Connect Google", actions.google.buttonLabel)
        assertEquals("Setup needed", actions.google.statusLabel)
        assertEquals("Missing Google OAuth client ID in this build.", actions.google.supportingText)
        assertEquals(false, actions.github.enabled)
        assertEquals("Setup needed", actions.github.statusLabel)
        assertEquals("Missing GitHub OAuth client ID in this build.", actions.github.supportingText)
    }

    @Test
    fun providerSignInActions_showGitHubBusyStateForBothProviders() {
        val actions = providerSignInActions(
            googleConfigured = true,
            githubConfigured = true,
            githubBusy = true,
        )

        assertEquals(false, actions.google.enabled)
        assertEquals("Wait for GitHub to finish first.", actions.google.supportingText)
        assertEquals(false, actions.github.enabled)
        assertEquals("Waiting for GitHub", actions.github.buttonLabel)
        assertEquals("In progress", actions.github.statusLabel)
        assertEquals("Enter the code in GitHub to finish linking your local account.", actions.github.supportingText)
    }

    @Test
    fun saveAccount_blankNameKeepsEditorOpenWithValidation() = runTest {
        val accountRepository = FakeAccountRepository()
        val viewModel = settingsViewModel(accountRepository = accountRepository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openAccountEditor()
        viewModel.onAccountNameChanged("   ")
        viewModel.saveAccount()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, viewModel.state.value.accountEditorOpen)
        assertEquals("Account name is required.", viewModel.state.value.accountErrorMessage)
        assertEquals(null, accountRepository.updatedName)
    }

    @Test
    fun openAccountEditor_prefillsFromActiveAccountAndEditingClearsError() = runTest {
        val accountRepository = FakeAccountRepository(
            initial = Account(id = "account-1", displayName = "Ava", email = "ava@example.com", remoteUserId = null),
        )
        val viewModel = settingsViewModel(accountRepository = accountRepository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openAccountEditor()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("Ava", viewModel.state.value.accountNameInput)
        assertEquals("ava@example.com", viewModel.state.value.accountEmailInput)

        viewModel.onAccountNameChanged("")
        viewModel.saveAccount() // blank → error
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onAccountNameChanged("A") // editing clears the error
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(null, viewModel.state.value.accountErrorMessage)
    }

    @Test
    fun profileDetails_exposesProfileAndLatestWeightForEditor() = runTest {
        val profileRepo = FakeProfileRepository(
            profile = UserProfile(Sex.Male, 0L, 180.0, ActivityLevel.Moderate, GoalType.Maintain, 0.0, 80.0),
            latestWeight = WeightEntry("w1", 1_000L, 80.0, "manual"),
        )
        val viewModel = settingsViewModel(profileRepository = profileRepo)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(180.0, viewModel.state.value.profile.heightCm!!, 0.001)
        assertEquals(80.0, viewModel.state.value.latestWeightKg!!, 0.001)
    }

    @Test
    fun aiCoachState_exposesSavedProvider() = runTest {
        val aiCoachRepository = FakeAiCoachRepository(
            initial = AiCoachSettings(
                providerKind = AiCoachProviderKind.OpenAiCompatible,
                baseUrl = "https://api.example.com/",
                modelName = "gpt-4.1-mini",
                localAgentKind = LocalAgentKind.Custom,
                hasApiKey = true,
            ),
        )

        val viewModel = settingsViewModel(aiCoachRepository = aiCoachRepository)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(AiCoachProviderKind.OpenAiCompatible, viewModel.state.value.aiCoach.providerKind)
        assertEquals("API-compatible endpoint", viewModel.state.value.aiCoach.providerLabel)
        assertEquals("https://api.example.com/", viewModel.state.value.aiCoach.endpointLabel)
        assertEquals("gpt-4.1-mini", viewModel.state.value.aiCoach.modelLabel)
        assertEquals("Key saved", viewModel.state.value.aiCoach.apiKeyLabel)
    }

    @Test
    fun openAiCoachEditor_prefillsSavedSettingsAndSaveDelegatesInput() = runTest {
        val aiCoachRepository = FakeAiCoachRepository(
            initial = AiCoachSettings(
                providerKind = AiCoachProviderKind.LocalAgent,
                baseUrl = "http://10.0.2.2:8989/",
                modelName = "",
                localAgentKind = LocalAgentKind.OpenClaw,
                hasApiKey = false,
            ),
        )
        val viewModel = settingsViewModel(aiCoachRepository = aiCoachRepository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openAiCoachEditor()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(true, viewModel.state.value.aiCoachEditorOpen)
        assertEquals(AiCoachProviderKind.LocalAgent, viewModel.state.value.aiCoachProviderInput)
        assertEquals("http://10.0.2.2:8989/", viewModel.state.value.aiCoachBaseUrlInput)
        assertEquals(LocalAgentKind.OpenClaw, viewModel.state.value.aiCoachLocalAgentInput)

        viewModel.onAiCoachProviderChanged(AiCoachProviderKind.OpenAiCompatible)
        viewModel.onAiCoachBaseUrlChanged("https://api.example.com")
        viewModel.onAiCoachModelNameChanged("gpt-4.1-mini")
        viewModel.onAiCoachApiKeyChanged("sk-test")
        viewModel.saveAiCoachSettings()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, viewModel.state.value.aiCoachEditorOpen)
        assertEquals("AI coach setup saved.", viewModel.state.value.message)
        assertEquals(
            AiCoachSettingsInput(
                providerKind = AiCoachProviderKind.OpenAiCompatible,
                baseUrl = "https://api.example.com",
                modelName = "gpt-4.1-mini",
                localAgentKind = LocalAgentKind.OpenClaw,
                apiKey = AiCoachApiKeyUpdate.Replace("sk-test"),
            ),
            aiCoachRepository.savedInput,
        )
    }

    @Test
    fun saveAiCoachSettings_withoutNewKeyKeepsExistingKey() = runTest {
        val aiCoachRepository = FakeAiCoachRepository(
            initial = AiCoachSettings(
                providerKind = AiCoachProviderKind.OpenAiCompatible,
                baseUrl = "https://api.example.com/",
                modelName = "gpt-4.1-mini",
                localAgentKind = LocalAgentKind.Custom,
                hasApiKey = true,
            ),
        )
        val viewModel = settingsViewModel(aiCoachRepository = aiCoachRepository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openAiCoachEditor()
        viewModel.saveAiCoachSettings()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(AiCoachApiKeyUpdate.KeepExisting, aiCoachRepository.savedInput?.apiKey)
    }

    @Test
    fun saveAiCoachSettings_failureKeepsEditorOpenWithError() = runTest {
        val aiCoachRepository = FakeAiCoachRepository()
        aiCoachRepository.saveError = IllegalArgumentException("Enter a valid http(s) base URL.")
        val viewModel = settingsViewModel(aiCoachRepository = aiCoachRepository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openAiCoachEditor()
        viewModel.onAiCoachProviderChanged(AiCoachProviderKind.OpenAiCompatible)
        viewModel.onAiCoachBaseUrlChanged("bad")
        viewModel.onAiCoachModelNameChanged("gpt-4.1-mini")
        viewModel.saveAiCoachSettings()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, viewModel.state.value.aiCoachEditorOpen)
        assertEquals("Enter a valid http(s) base URL.", viewModel.state.value.aiCoachErrorMessage)
    }

    @Test
    fun clearAiCoachApiKey_delegatesAndUpdatesMessage() = runTest {
        val aiCoachRepository = FakeAiCoachRepository(
            initial = AiCoachSettings(
                providerKind = AiCoachProviderKind.OpenAiCompatible,
                baseUrl = "https://api.example.com/",
                modelName = "gpt-4.1-mini",
                localAgentKind = LocalAgentKind.Custom,
                hasApiKey = true,
            ),
        )
        val viewModel = settingsViewModel(aiCoachRepository = aiCoachRepository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.clearAiCoachApiKey()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, aiCoachRepository.clearCalls)
        assertEquals("AI coach API key cleared.", viewModel.state.value.message)
    }

    private class FakeHealthRepository(
        private var status: HealthConnectStatus = HealthConnectStatus(
            availability = HealthConnectAvailability.Available,
            grantedPermissions = setOf("steps"),
        ),
        private val requestablePermissions: Set<String> = setOf("steps"),
        private val exportedRecordId: String? = "record-id",
        private val refreshResult: HealthConnectRefreshResult = HealthConnectRefreshResult(1, 0),
    ) : HealthRepository {
        var statusException: Throwable? = null
        var importedDate: LocalDate? = null
        var refreshDate: LocalDate? = null
        var exportCalls = 0

        override suspend fun status(): HealthConnectStatus {
            statusException?.let { throw it }
            return status
        }

        override suspend fun requestablePermissions(): Set<String> = requestablePermissions

        override fun observeDailySummary(date: LocalDate): Flow<DailyHealthSummaryEntity?> =
            flowOf(null)

        override suspend fun importDailySummary(date: LocalDate): ImportedDailyHealthSummary {
            importedDate = date
            return ImportedDailyHealthSummary(
                steps = 1200,
                activeCaloriesKcal = 100.0,
                totalCaloriesKcal = 2_000.0,
                distanceMeters = 3_000.0,
                sleepMinutes = 450,
                exerciseMinutes = 35,
                exerciseSessionCount = 1,
                latestWeightKg = null,
                latestBodyFatPercent = null,
                restingHeartRateBpm = null,
            )
        }

        override suspend fun refreshRecentData(endDate: LocalDate, days: Int): HealthConnectRefreshResult {
            refreshDate = endDate
            return refreshResult
        }

        override suspend fun exportLatestWorkout(): String? {
            exportCalls += 1
            return exportedRecordId
        }
    }

    private class FakeAccountRepository(
        initial: Account = Account("local-default", "You", null, null),
    ) : AccountRepository {
        private val active = MutableStateFlow(initial)
        var updatedName: String? = null
        var updatedEmail: String? = null
        var ensured = false
        var ensureError: Throwable? = null
        var signInError: Throwable? = null
        var linkedProfile: ExternalAccountProfile? = null

        override fun observeActiveAccount(): Flow<Account> = active

        override fun observeAccounts(): Flow<List<Account>> = MutableStateFlow(listOf(active.value))

        override suspend fun ensureActiveAccount(): Account {
            ensureError?.let { throw it }
            ensured = true
            return active.value
        }

        override suspend fun createAccount(displayName: String, email: String?): String = "created"

        override suspend fun updateActiveAccount(displayName: String, email: String?) {
            updatedName = displayName
            updatedEmail = email
            active.value = active.value.copy(displayName = displayName, email = email)
        }

        override suspend fun switchAccount(accountId: String) = Unit

        override suspend fun signInWithProvider(profile: ExternalAccountProfile): Account {
            signInError?.let { throw it }
            linkedProfile = profile
            val linked = active.value.copy(
                displayName = profile.displayName,
                email = profile.email,
                remoteUserId = "${profile.provider.storageValue}:${profile.providerUserId}",
                authProvider = profile.provider,
                avatarUrl = profile.avatarUrl,
            )
            active.value = linked
            return linked
        }
    }

    private class FakeProfileRepository(
        private val profile: UserProfile = DEFAULT_USER_PROFILE,
        private val latestWeight: WeightEntry? = null,
    ) : ProfileRepository {
        var saveProfileError: Throwable? = null
        override fun observeProfile(): Flow<UserProfile> = flowOf(profile)
        override suspend fun saveProfile(profile: UserProfile) {
            saveProfileError?.let { throw it }
        }
        override fun observeRecommendedTargets(): Flow<RecommendedTargets?> = flowOf(null)
        override suspend fun logWeight(weightKg: Double, source: String) = Unit
        override fun observeLatestWeight(): Flow<WeightEntry?> = flowOf(latestWeight)
        override fun observeWeightSeries(sinceEpochMillis: Long): Flow<List<WeightEntry>> =
            flowOf(listOfNotNull(latestWeight))
        override suspend fun logMeasurement(type: String, value: Double, unit: String) = Unit
        override suspend fun deleteEntry(id: String) = Unit
        override suspend fun updateEntryValue(id: String, value: Double) = Unit
        override fun observeRecentMeasurements(sinceEpochMillis: Long): Flow<Map<String, List<BodyMeasurement>>> =
            flowOf(emptyMap())
        override fun observeSettings(): Flow<AppSettings> = flowOf(DEFAULT_APP_SETTINGS)
        override suspend fun saveSettings(settings: AppSettings) = Unit
    }

    private class FakeExternalAuthRepository(
        private val profile: ExternalAccountProfile = ExternalAccountProfile(
            provider = AccountAuthProvider.GitHub,
            providerUserId = "42",
            displayName = "octocat",
            email = null,
            avatarUrl = null,
        ),
    ) : ExternalAuthRepository {
        override val isGitHubConfigured: Boolean = true

        override suspend fun signInWithGitHub(
            onDeviceAuthorization: suspend (GitHubDeviceAuthorization) -> Unit,
        ): ExternalAccountProfile {
            onDeviceAuthorization(
                GitHubDeviceAuthorization(
                    userCode = "WDJB-MJHT",
                    verificationUri = "https://github.com/login/device",
                    expiresInSeconds = 900,
                ),
            )
            return profile
        }
    }

    private class FakeAiCoachRepository(
        initial: AiCoachSettings = AiCoachSettings(),
    ) : AiCoachRepository {
        private val settings = MutableStateFlow(initial)
        var savedInput: AiCoachSettingsInput? = null
        var saveError: Throwable? = null
        var clearCalls = 0

        override fun observeSettings(): Flow<AiCoachSettings> = settings

        override suspend fun saveSettings(input: AiCoachSettingsInput) {
            saveError?.let { throw it }
            savedInput = input
            settings.value = settings.value.copy(
                providerKind = input.providerKind,
                baseUrl = input.baseUrl,
                modelName = input.modelName,
                localAgentKind = input.localAgentKind,
                hasApiKey = input.apiKey is AiCoachApiKeyUpdate.Replace || settings.value.hasApiKey,
            )
        }

        override suspend fun clearApiKey() {
            clearCalls += 1
            settings.value = settings.value.copy(hasApiKey = false)
        }

        override suspend fun activeConnection(): AiCoachConnection? = null
    }
}
