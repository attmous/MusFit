package com.musfit.ui.profile

import com.musfit.data.local.entity.DailyHealthSummaryEntity
import com.musfit.data.repository.Account
import com.musfit.data.repository.AccountRepository
import com.musfit.data.repository.AppSettings
import com.musfit.data.repository.BodyMeasurement
import com.musfit.data.repository.DEFAULT_APP_SETTINGS
import com.musfit.data.repository.DEFAULT_USER_PROFILE
import com.musfit.data.repository.HealthRepository
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
    ) = ProfileSettingsViewModel(healthRepository, accountRepository, profileRepository)

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
        assertEquals("Imported 1200 steps and 100 kcal from Health Connect.", viewModel.state.value.message)
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

        assertEquals("Ava", viewModel.state.value.account.displayName)
        assertEquals("ava@example.com", viewModel.state.value.account.email)
        assertEquals(true, viewModel.state.value.account.isLocalOnly)
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

    private class FakeHealthRepository(
        private var status: HealthConnectStatus = HealthConnectStatus(
            availability = HealthConnectAvailability.Available,
            grantedPermissions = setOf("steps"),
        ),
        private val requestablePermissions: Set<String> = setOf("steps"),
        private val exportedRecordId: String? = "record-id",
    ) : HealthRepository {
        var statusException: Throwable? = null
        var importedDate: LocalDate? = null
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
                latestWeightKg = null,
                restingHeartRateBpm = null,
            )
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

        override fun observeActiveAccount(): Flow<Account> = active

        override fun observeAccounts(): Flow<List<Account>> = MutableStateFlow(listOf(active.value))

        override suspend fun ensureActiveAccount(): Account {
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
    }

    private class FakeProfileRepository(
        private val profile: UserProfile = DEFAULT_USER_PROFILE,
        private val latestWeight: WeightEntry? = null,
    ) : ProfileRepository {
        override fun observeProfile(): Flow<UserProfile> = flowOf(profile)
        override suspend fun saveProfile(profile: UserProfile) = Unit
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
}
