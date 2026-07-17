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
            clock = {
                clockMillis += 1_000L
                clockMillis
            },
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
        assertEquals(AccountAuthProvider.Local, account.authProvider)
        assertNull(account.avatarUrl)
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
                authProvider = AccountAuthProvider.Local.storageValue,
                avatarUrl = null,
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
                authProvider = AccountAuthProvider.Local.storageValue,
                avatarUrl = null,
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

    @Test
    fun signInWithProvider_claimsActiveLocalAccountAndStoresProviderIdentity() = runTest {
        repository.ensureActiveAccount()

        val account = repository.signInWithProvider(
            ExternalAccountProfile(
                provider = AccountAuthProvider.Google,
                providerUserId = "google-sub-1",
                displayName = "  Ava  ",
                email = "  ava@gmail.com  ",
                avatarUrl = "https://example.com/avatar.png",
            ),
        )

        assertEquals(LOCAL_DEFAULT_ACCOUNT_ID, account.id)
        assertEquals("Ava", account.displayName)
        assertEquals("ava@gmail.com", account.email)
        assertEquals(AccountAuthProvider.Google, account.authProvider)
        assertEquals("google:google-sub-1", account.remoteUserId)
        assertEquals("https://example.com/avatar.png", account.avatarUrl)
        assertEquals(account, repository.observeActiveAccount().first())
    }

    @Test
    fun signInWithProvider_switchesToExistingLinkedAccount() = runTest {
        repository.ensureActiveAccount()
        database.accountDao().upsertAccount(
            AccountEntity(
                id = "github-account",
                displayName = "Old GitHub",
                email = null,
                remoteUserId = "github:42",
                authProvider = AccountAuthProvider.GitHub.storageValue,
                avatarUrl = null,
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 2L,
            ),
        )

        val account = repository.signInWithProvider(
            ExternalAccountProfile(
                provider = AccountAuthProvider.GitHub,
                providerUserId = "42",
                displayName = "octocat",
                email = "octo@github.com",
                avatarUrl = "https://avatars.githubusercontent.com/u/42",
            ),
        )

        assertEquals("github-account", account.id)
        assertEquals("octocat", account.displayName)
        assertEquals("octo@github.com", account.email)
        assertEquals(AccountAuthProvider.GitHub, account.authProvider)
        assertEquals("github:42", account.remoteUserId)
        assertEquals("github-account", repository.observeActiveAccount().first().id)
    }
}
