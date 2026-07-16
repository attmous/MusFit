package com.musfit.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.musfit.data.local.MusFitDatabase
import com.musfit.data.local.entity.AccountEntity
import com.musfit.data.local.entity.AccountSessionEntity
import com.musfit.data.local.entity.AiCoachSettingsEntity
import com.musfit.data.local.entity.DailyHealthSummaryEntity
import com.musfit.data.local.entity.LOCAL_DEFAULT_ACCOUNT_ID
import com.musfit.domain.health.HealthConnectAvailability
import com.musfit.domain.health.HealthConnectDeleteResult
import com.musfit.domain.health.HealthConnectStatus
import com.musfit.domain.health.ImportedDailyHealthSummary
import com.musfit.integrations.healthconnect.HealthConnectAuthoredRecordType
import com.musfit.integrations.healthconnect.HealthConnectDeleteFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class LocalAccountErasureRepositoryTest {
    private lateinit var database: MusFitDatabase
    private lateinit var secrets: FakeSecretStore
    private lateinit var health: FakeHealthRepository
    private lateinit var repository: LocalAccountErasureRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MusFitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        secrets = FakeSecretStore()
        health = FakeHealthRepository()
        repository = LocalAccountErasureRepository(
            database = database,
            accountErasureDao = database.accountErasureDao(),
            healthRepository = health,
            secretStore = secrets,
            clock = { 50_000L },
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun eraseActiveAccount_removesOnlyActiveOwnedRowsAndSecret_thenSelectsFallback() = runTest {
        seedAccount("account-a", updatedAt = 20L)
        seedAccount("account-b", updatedAt = 10L)
        activate("account-a")
        seedAiSettings("account-a")
        seedAiSettings("account-b")
        secrets.saveApiKey("account-a", "secret-a")
        secrets.saveApiKey("account-b", "secret-b")

        val result = repository.erase(
            AccountErasureRequest(AccountErasureScope.ActiveAccount, deleteAuthoredHealthRecords = false),
        )

        assertEquals(AccountErasureResult.Complete("account-b"), result)
        assertNull(database.accountDao().getAccount("account-a"))
        assertNull(database.aiCoachDao().getSettings("account-a"))
        assertNotNull(database.accountDao().getAccount("account-b"))
        assertNotNull(database.aiCoachDao().getSettings("account-b"))
        assertEquals("account-b", database.accountDao().getActiveAccount()?.id)
        assertNull(secrets.getApiKey("account-a"))
        assertEquals("secret-b", secrets.getApiKey("account-b"))
        assertEquals(emptyList<String>(), health.deletedAccountIds)
    }

    @Test
    fun eraseAll_removesEveryAccountAndSecret_thenCreatesValidLocalFallback() = runTest {
        seedAccount("account-a", updatedAt = 20L)
        seedAccount("account-b", updatedAt = 10L)
        activate("account-a")
        seedAiSettings("account-a")
        seedAiSettings("account-b")
        secrets.saveApiKey("account-a", "secret-a")
        secrets.saveApiKey("account-b", "secret-b")

        val result = repository.erase(
            AccountErasureRequest(AccountErasureScope.AllAccounts, deleteAuthoredHealthRecords = false),
        )

        assertEquals(AccountErasureResult.Complete(LOCAL_DEFAULT_ACCOUNT_ID), result)
        assertEquals(listOf(LOCAL_DEFAULT_ACCOUNT_ID), database.accountErasureDao().getAccounts().map { it.id })
        assertEquals(LOCAL_DEFAULT_ACCOUNT_ID, database.accountDao().getActiveAccount()?.id)
        assertNull(secrets.getApiKey("account-a"))
        assertNull(secrets.getApiKey("account-b"))
        assertNull(database.aiCoachDao().getSettings("account-a"))
        assertNull(database.aiCoachDao().getSettings("account-b"))
    }

    @Test
    fun requestedHealthCleanup_failureKeepsLocalAccountRowsAndSecretForSafeRetry() = runTest {
        seedAccount("account-a", updatedAt = 20L)
        activate("account-a")
        seedAiSettings("account-a")
        secrets.saveApiKey("account-a", "secret-a")
        health.deleteResult = HealthConnectDeleteResult.Failure("Health Connect denied deletion.")

        val result = repository.erase(
            AccountErasureRequest(AccountErasureScope.ActiveAccount, deleteAuthoredHealthRecords = true),
        )

        assertEquals(AccountErasureResult.HealthCleanupFailed("Health Connect denied deletion."), result)
        assertNotNull(database.accountDao().getAccount("account-a"))
        assertNotNull(database.aiCoachDao().getSettings("account-a"))
        assertEquals("secret-a", secrets.getApiKey("account-a"))
        assertEquals(listOf("account-a"), health.deletedAccountIds)
    }

    @Test
    fun eraseActiveAccount_emptyDatabaseCreatesValidLocalFallback() = runTest {
        val result = repository.erase(
            AccountErasureRequest(AccountErasureScope.ActiveAccount, deleteAuthoredHealthRecords = false),
        )

        assertEquals(AccountErasureResult.Complete(LOCAL_DEFAULT_ACCOUNT_ID), result)
        assertEquals(LOCAL_DEFAULT_ACCOUNT_ID, database.accountDao().getActiveAccount()?.id)
        assertEquals(50_000L, database.accountDao().getActiveAccount()?.createdAtEpochMillis)
    }

    @Test
    fun eraseAll_withSuccessfulHealthCleanupDeletesEveryTargetBeforeLocalErasure() = runTest {
        seedAccount("account-a", updatedAt = 20L)
        seedAccount("account-b", updatedAt = 10L)
        activate("account-a")

        val result = repository.erase(
            AccountErasureRequest(AccountErasureScope.AllAccounts, deleteAuthoredHealthRecords = true),
        )

        assertEquals(AccountErasureResult.Complete(LOCAL_DEFAULT_ACCOUNT_ID), result)
        assertEquals(listOf("account-a", "account-b"), health.deletedAccountIds)
        assertEquals(listOf(LOCAL_DEFAULT_ACCOUNT_ID), database.accountErasureDao().getAccounts().map { it.id })
    }

    @Test
    fun requestedHealthCleanup_partialFailurePreservesLocalDataAndReportsRetry() = runTest {
        seedAccount("account-a", updatedAt = 20L)
        activate("account-a")
        health.deleteResult = HealthConnectDeleteResult.Partial(
            deletedRecords = emptySet(),
            failures = listOf(
                HealthConnectDeleteFailure(HealthConnectAuthoredRecordType.Workout, "provider failure"),
            ),
        )

        val result = repository.erase(
            AccountErasureRequest(AccountErasureScope.ActiveAccount, deleteAuthoredHealthRecords = true),
        )

        assertEquals(
            AccountErasureResult.HealthCleanupFailed(
                "Health Connect deleted some MusFit records, but 1 failed. Retry before erasing local data.",
            ),
            result,
        )
        assertNotNull(database.accountDao().getAccount("account-a"))
    }

    @Test
    fun requestedHealthCleanup_unavailablePreservesLocalDataAndMessage() = runTest {
        seedAccount("account-a", updatedAt = 20L)
        activate("account-a")
        health.deleteResult = HealthConnectDeleteResult.Unavailable("Health Connect is unavailable.")

        val result = repository.erase(
            AccountErasureRequest(AccountErasureScope.ActiveAccount, deleteAuthoredHealthRecords = true),
        )

        assertEquals(AccountErasureResult.HealthCleanupFailed("Health Connect is unavailable."), result)
        assertNotNull(database.accountDao().getAccount("account-a"))
    }

    private suspend fun seedAccount(id: String, updatedAt: Long) {
        database.accountDao().upsertAccount(
            AccountEntity(id, id, null, null, "local", null, 1L, updatedAt),
        )
    }

    private suspend fun activate(id: String) {
        database.accountDao().upsertSession(AccountSessionEntity("active", id, 30L))
    }

    private suspend fun seedAiSettings(accountId: String) {
        database.aiCoachDao().upsertSettings(
            AiCoachSettingsEntity(accountId, "Disabled", "", "", "Custom", true, 1L),
        )
    }
}

private class FakeSecretStore : AiCoachSecretStore {
    private val values = mutableMapOf<String, String>()
    override suspend fun saveApiKey(accountId: String, apiKey: String) {
        values[accountId] = apiKey
    }
    override suspend fun getApiKey(accountId: String): String? = values[accountId]
    override suspend fun clearApiKey(accountId: String) {
        values.remove(accountId)
    }
}

private class FakeHealthRepository : HealthRepository {
    val deletedAccountIds = mutableListOf<String>()
    var deleteResult: HealthConnectDeleteResult = HealthConnectDeleteResult.Complete(emptySet())

    override suspend fun status() = HealthConnectStatus(HealthConnectAvailability.Available, emptySet())
    override suspend fun requestablePermissions(): Set<String> = emptySet()
    override fun observeDailySummary(date: LocalDate): Flow<DailyHealthSummaryEntity?> = flowOf(null)
    override suspend fun importDailySummary(date: LocalDate) = HealthConnectImportResult.Empty(ImportedDailyHealthSummary())
    override suspend fun exportLatestWorkout(): String? = null
    override suspend fun deleteAuthoredRecords(accountId: String?): HealthConnectDeleteResult {
        deletedAccountIds += requireNotNull(accountId)
        return deleteResult
    }
}
