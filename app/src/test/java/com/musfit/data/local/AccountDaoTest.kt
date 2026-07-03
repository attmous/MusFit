package com.musfit.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.musfit.data.local.dao.AccountDao
import com.musfit.data.local.entity.ACTIVE_ACCOUNT_SESSION_KEY
import com.musfit.data.local.entity.AccountEntity
import com.musfit.data.local.entity.AccountSessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccountDaoTest {
    private lateinit var database: MusFitDatabase
    private lateinit var accountDao: AccountDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MusFitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        accountDao = database.accountDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun accountDao_roundTripsAccountsAndActiveSession() = runTest {
        val account = AccountEntity(
            id = "account-1",
            displayName = "Ava",
            email = "ava@example.com",
            remoteUserId = null,
            authProvider = "local",
            avatarUrl = null,
            createdAtEpochMillis = 1_000L,
            updatedAtEpochMillis = 2_000L,
        )
        val session = AccountSessionEntity(
            key = ACTIVE_ACCOUNT_SESSION_KEY,
            activeAccountId = account.id,
            updatedAtEpochMillis = 3_000L,
        )

        accountDao.upsertAccount(account)
        accountDao.upsertSession(session)

        assertEquals(listOf(account), accountDao.observeAccounts().first())
        assertEquals(account, accountDao.getAccount(account.id))
        assertEquals(account, accountDao.getActiveAccount())
        assertEquals(account, accountDao.observeActiveAccount().first())
        assertEquals(session, accountDao.getSession())
        assertNull(accountDao.getAccount("missing"))
    }
}
