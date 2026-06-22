package com.musfit.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.musfit.data.local.MusFitDatabase
import com.musfit.data.local.entity.AccountEntity
import com.musfit.data.local.entity.LOCAL_DEFAULT_ACCOUNT_ID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalAccountRepositoryTest {
    private lateinit var database: MusFitDatabase
    private lateinit var repository: LocalAccountRepository
    private var clockMillis = 10_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MusFitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LocalAccountRepository(
            accountDao = database.accountDao(),
            clock = { clockMillis += 1_000L; clockMillis },
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun ensureActiveAccount_createsDefaultLocalAccountWhenEmpty() = runTest {
        val account = repository.ensureActiveAccount()

        assertEquals(LOCAL_DEFAULT_ACCOUNT_ID, account.id)
        assertEquals("You", account.displayName)
        assertNull(account.email)
        assertEquals(account, repository.observeActiveAccount().first())
        assertEquals(listOf(account), repository.observeAccounts().first())
    }

    @Test
    fun ensureActiveAccount_selectsMostRecentlyUpdatedAccountWhenSessionMissing() = runTest {
        database.accountDao().upsertAccount(
            AccountEntity(
                id = "older",
                displayName = "Older",
                email = null,
                remoteUserId = null,
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 2L,
            ),
        )
        database.accountDao().upsertAccount(
            AccountEntity(
                id = "newer",
                displayName = "Newer",
                email = null,
                remoteUserId = null,
                createdAtEpochMillis = 3L,
                updatedAtEpochMillis = 4L,
            ),
        )

        val account = repository.ensureActiveAccount()

        assertEquals("newer", account.id)
        assertEquals("Newer", repository.observeActiveAccount().first().displayName)
    }

    @Test
    fun updateActiveAccount_trimsNameAndStoresBlankEmailAsNull() = runTest {
        repository.ensureActiveAccount()

        repository.updateActiveAccount(displayName = "  Ava  ", email = "   ")

        val account = repository.observeActiveAccount().first()
        assertEquals("Ava", account.displayName)
        assertNull(account.email)
    }

    @Test
    fun updateActiveAccount_rejectsBlankDisplayName() = runTest {
        repository.ensureActiveAccount()

        try {
            repository.updateActiveAccount(displayName = "   ", email = null)
            fail("Expected blank display name to be rejected")
        } catch (expected: IllegalArgumentException) {
            assertEquals("Account name is required.", expected.message)
        }
    }

    @Test
    fun switchAccount_updatesObservedActiveAccount() = runTest {
        repository.ensureActiveAccount()
        val secondId = repository.createAccount(displayName = "Partner", email = "partner@example.com")

        repository.switchAccount(secondId)

        val active = repository.observeActiveAccount().first()
        assertEquals(secondId, active.id)
        assertEquals("Partner", active.displayName)
        assertEquals("partner@example.com", active.email)
    }
}
