package com.musfit.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.musfit.data.local.MusFitDatabase
import com.musfit.domain.profile.ActivityLevel
import com.musfit.domain.profile.GoalType
import com.musfit.domain.profile.Sex
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalProfileRepositoryTest {
    private lateinit var database: MusFitDatabase
    private lateinit var accountRepository: LocalAccountRepository
    private lateinit var repository: LocalProfileRepository
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
        repository = LocalProfileRepository(
            profileDao = database.profileDao(),
            healthDao = database.healthDao(),
            accountRepository = accountRepository,
            clock = { clockMillis += 1_000L; clockMillis },
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun observeProfile_returnsDefaultsWhenEmpty() = runTest {
        val profile = repository.observeProfile().first()
        assertEquals(ActivityLevel.Moderate, profile.activityLevel)
        assertEquals(GoalType.Maintain, profile.goalType)
        assertNull(profile.sex)
        assertNull(profile.heightCm)
    }

    @Test
    fun saveProfile_thenObserve_roundTrips() = runTest {
        repository.saveProfile(
            UserProfile(
                sex = Sex.Male,
                birthDateEpochDay = 9_000L,
                heightCm = 182.0,
                activityLevel = ActivityLevel.Active,
                goalType = GoalType.Lose,
                goalPaceKgPerWeek = 0.5,
                goalWeightKg = 78.0,
            ),
        )
        val profile = repository.observeProfile().first()
        assertEquals(Sex.Male, profile.sex)
        assertEquals(182.0, profile.heightCm!!, 0.001)
        assertEquals(GoalType.Lose, profile.goalType)
    }

    @Test
    fun logWeight_thenObserveLatest_returnsNewest() = runTest {
        repository.logWeight(85.0)
        repository.logWeight(84.2)
        val latest = repository.observeLatestWeight().first()
        assertNotNull(latest)
        assertEquals(84.2, latest!!.weightKg, 0.001)
        assertEquals("manual", latest.source)
    }

    @Test
    fun logMeasurement_storesUnderTypeWithUnit() = runTest {
        repository.logMeasurement("waist", 84.0, "cm")
        val recent = repository.observeRecentMeasurements(0L).first()
        assertEquals(84.0, recent["waist"]!!.first().value, 0.001)
        assertEquals("cm", recent["waist"]!!.first().unit)
    }

    @Test
    fun observeWeightSeries_exposesRowId() = runTest {
        repository.logWeight(85.0)
        val entry = repository.observeWeightSeries(0L).first().first()
        assertTrue(entry.id.isNotBlank())
    }

    @Test
    fun updateEntryValue_changesOnlyThatRow() = runTest {
        repository.logWeight(85.0)
        repository.logWeight(84.0)
        val entries = repository.observeWeightSeries(0L).first()
        val target = entries.first { it.weightKg == 85.0 }

        repository.updateEntryValue(target.id, 86.5)

        val updated = repository.observeWeightSeries(0L).first()
        assertEquals(86.5, updated.first { it.id == target.id }.weightKg, 0.001)
        assertEquals(84.0, updated.first { it.weightKg != 86.5 }.weightKg, 0.001)
    }

    @Test
    fun deleteEntry_removesRow() = runTest {
        repository.logMeasurement("waist", 84.0, "cm")
        val entry = repository.observeRecentMeasurements(0L).first()["waist"]!!.first()

        repository.deleteEntry(entry.id)

        assertTrue(repository.observeRecentMeasurements(0L).first()["waist"]!!.isEmpty())
    }

    @Test
    fun updateEntryValue_rejectsNonPositive() = runTest {
        repository.logWeight(85.0)
        val id = repository.observeWeightSeries(0L).first().first().id
        var threw = false
        try {
            repository.updateEntryValue(id, 0.0)
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun recommendedTargets_nullUntilProfileAndWeightPresent() = runTest {
        assertNull(repository.observeRecommendedTargets().first())
        repository.saveProfile(
            UserProfile(
                sex = Sex.Male,
                birthDateEpochDay = 0L,
                heightCm = 180.0,
                activityLevel = ActivityLevel.Moderate,
                goalType = GoalType.Maintain,
                goalPaceKgPerWeek = 0.0,
                goalWeightKg = 80.0,
            ),
        )
        repository.logWeight(80.0)
        assertNotNull(repository.observeRecommendedTargets().first())
    }

    @Test
    fun observeSettings_returnsDefaultsThenRoundTrips() = runTest {
        assertEquals("metric", repository.observeSettings().first().unitSystem)
        repository.saveSettings(AppSettings(unitSystem = "metric", themeMode = "dark"))
        assertEquals("dark", repository.observeSettings().first().themeMode)
    }

    @Test
    fun profileAndSettings_followActiveAccount() = runTest {
        val first = accountRepository.ensureActiveAccount()
        repository.saveProfile(
            UserProfile(
                sex = Sex.Male,
                birthDateEpochDay = 9_000L,
                heightCm = 182.0,
                activityLevel = ActivityLevel.Active,
                goalType = GoalType.Lose,
                goalPaceKgPerWeek = 0.5,
                goalWeightKg = 78.0,
            ),
        )
        repository.saveSettings(AppSettings(unitSystem = "metric", themeMode = "dark"))

        val secondId = accountRepository.createAccount(displayName = "Partner", email = null)
        accountRepository.switchAccount(secondId)

        assertNull(repository.observeProfile().first().sex)
        assertEquals("system", repository.observeSettings().first().themeMode)

        repository.saveProfile(
            UserProfile(
                sex = Sex.Female,
                birthDateEpochDay = 8_000L,
                heightCm = 170.0,
                activityLevel = ActivityLevel.Light,
                goalType = GoalType.Gain,
                goalPaceKgPerWeek = 0.25,
                goalWeightKg = 68.0,
            ),
        )

        accountRepository.switchAccount(first.id)
        assertEquals(Sex.Male, repository.observeProfile().first().sex)
        assertEquals("dark", repository.observeSettings().first().themeMode)

        accountRepository.switchAccount(secondId)
        assertEquals(Sex.Female, repository.observeProfile().first().sex)
    }
}
