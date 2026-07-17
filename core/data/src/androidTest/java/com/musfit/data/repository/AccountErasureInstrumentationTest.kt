package com.musfit.data.repository

import android.os.SystemClock
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.musfit.data.local.MusFitDatabase
import com.musfit.data.local.entity.AccountEntity
import com.musfit.data.local.entity.AccountSessionEntity
import com.musfit.data.local.entity.DailyHealthSummaryEntity
import com.musfit.domain.health.HealthConnectAvailability
import com.musfit.domain.health.HealthConnectStatus
import com.musfit.domain.health.ImportedDailyHealthSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class AccountErasureInstrumentationTest {
    @Test
    fun twoAccountLargeDeletion_isIsolatedTransactionalAndBounded() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, MusFitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            database.accountDao().upsertAccount(account("account-a", 20L))
            database.accountDao().upsertAccount(account("account-b", 10L))
            database.accountDao().upsertSession(AccountSessionEntity("active", "account-a", 30L))
            seedBodyMetrics(database, "account-a", LARGE_ROW_COUNT)
            seedBodyMetrics(database, "account-b", 1)
            val repository = LocalAccountErasureRepository(
                database = database,
                accountErasureDao = database.accountErasureDao(),
                healthRepository = DeviceFakeHealthRepository(),
                secretStore = DeviceFakeSecretStore(),
                clock = { 50_000L },
            )

            val started = SystemClock.elapsedRealtime()
            val result = repository.erase(
                AccountErasureRequest(AccountErasureScope.ActiveAccount, false),
            )
            val durationMillis = SystemClock.elapsedRealtime() - started

            assertEquals(AccountErasureResult.Complete("account-b"), result)
            assertEquals(0L, count(database, "body_metrics", "account-a"))
            assertEquals(1L, count(database, "body_metrics", "account-b"))
            assertEquals("account-b", database.accountDao().getActiveAccount()?.id)
            assertTrue("$LARGE_ROW_COUNT-row erasure took ${durationMillis}ms", durationMillis < MAX_DELETE_MILLIS)
        } finally {
            database.close()
        }
    }

    private fun seedBodyMetrics(database: MusFitDatabase, accountId: String, count: Int) {
        val sqlite = database.openHelper.writableDatabase
        sqlite.beginTransaction()
        try {
            repeat(count) { index ->
                sqlite.execSQL(
                    "INSERT INTO body_metrics (accountId,id,type,value,unit,measuredAtEpochMillis,source,externalId) VALUES (?,?,?,?,?,?,?,?)",
                    arrayOf<Any?>(accountId, "metric-$index", "weight", 80.0, "kg", index.toLong(), "manual", null),
                )
            }
            sqlite.setTransactionSuccessful()
        } finally {
            sqlite.endTransaction()
        }
    }

    private fun count(database: MusFitDatabase, table: String, accountId: String): Long = database.openHelper.writableDatabase.query("SELECT COUNT(*) FROM $table WHERE accountId = ?", arrayOf(accountId)).use {
        it.moveToFirst()
        it.getLong(0)
    }

    private fun account(id: String, updatedAt: Long) = AccountEntity(id, id, null, null, "local", null, 1L, updatedAt)

    private companion object {
        const val LARGE_ROW_COUNT = 10_000
        const val MAX_DELETE_MILLIS = 5_000L
    }
}

private class DeviceFakeSecretStore : AiCoachSecretStore {
    override suspend fun saveApiKey(accountId: String, apiKey: String) = Unit
    override suspend fun getApiKey(accountId: String): String? = null
    override suspend fun clearApiKey(accountId: String) = Unit
}

private class DeviceFakeHealthRepository : HealthRepository {
    override suspend fun status() = HealthConnectStatus(HealthConnectAvailability.Available, emptySet())
    override suspend fun requestablePermissions(): Set<String> = emptySet()
    override fun observeDailySummary(date: LocalDate): Flow<DailyHealthSummaryEntity?> = flowOf(null)
    override suspend fun importDailySummary(date: LocalDate) = HealthConnectImportResult.Empty(ImportedDailyHealthSummary())
    override suspend fun exportLatestWorkout(): String? = null
}
