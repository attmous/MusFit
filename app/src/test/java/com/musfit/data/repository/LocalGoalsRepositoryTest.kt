package com.musfit.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.musfit.data.local.MusFitDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalGoalsRepositoryTest {
    private lateinit var database: MusFitDatabase
    private lateinit var accountRepository: LocalAccountRepository
    private lateinit var repository: LocalGoalsRepository
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
        repository = LocalGoalsRepository(
            userGoalsDao = database.userGoalsDao(),
            accountRepository = accountRepository,
            clock = { clockMillis += 1_000L; clockMillis },
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun userGoals_followActiveAccount() = runTest {
        val first = accountRepository.ensureActiveAccount()
        repository.updateUserGoals(UserGoals(stepGoal = 8_000L, weeklySessionTarget = 3, targetWeightKg = 80.0))

        val secondId = accountRepository.createAccount(displayName = "Partner", email = null)
        accountRepository.switchAccount(secondId)

        assertEquals(UserGoals(), repository.observeUserGoals().first())

        repository.updateUserGoals(UserGoals(stepGoal = 12_000L, weeklySessionTarget = 5, targetWeightKg = 70.0))

        accountRepository.switchAccount(first.id)
        assertEquals(8_000L, repository.observeUserGoals().first().stepGoal)
        assertEquals(3, repository.observeUserGoals().first().weeklySessionTarget)

        accountRepository.switchAccount(secondId)
        assertEquals(12_000L, repository.observeUserGoals().first().stepGoal)
        assertEquals(5, repository.observeUserGoals().first().weeklySessionTarget)
    }
}
