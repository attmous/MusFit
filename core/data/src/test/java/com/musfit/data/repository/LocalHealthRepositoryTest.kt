package com.musfit.data.repository

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import com.musfit.data.local.MusFitDatabase
import com.musfit.data.local.entity.BodyMetricEntity
import com.musfit.data.local.entity.DailyHealthSummaryEntity
import com.musfit.data.local.entity.ExerciseEntity
import com.musfit.data.local.entity.HealthConnectExportRecordEntity
import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity
import com.musfit.domain.health.HealthConnectAuthoredRecord
import com.musfit.domain.health.HealthConnectAuthoredRecordType
import com.musfit.domain.health.HealthConnectAvailability
import com.musfit.domain.health.HealthConnectDailyReadResult
import com.musfit.domain.health.HealthConnectDeleteFailure
import com.musfit.domain.health.HealthConnectDeleteResult
import com.musfit.domain.health.HealthConnectFoodExportPayload
import com.musfit.domain.health.HealthConnectFoodExportResult
import com.musfit.domain.health.HealthConnectGateway
import com.musfit.domain.health.HealthConnectMetric
import com.musfit.domain.health.HealthConnectMetricFailure
import com.musfit.domain.health.HealthConnectRecordIdentity
import com.musfit.domain.health.HealthConnectStatus
import com.musfit.domain.health.HealthConnectUnavailableReason
import com.musfit.domain.health.HealthConnectWorkoutExport
import com.musfit.domain.health.ImportedBodyMetric
import com.musfit.domain.health.ImportedDailyHealthSummary
import com.musfit.domain.health.StepSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
import java.util.concurrent.CancellationException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor

private const val TEST_ACCOUNT_ID = "local-default"

@RunWith(RobolectricTestRunner::class)
class LocalHealthRepositoryTest {
    private lateinit var database: MusFitDatabase
    private lateinit var gateway: FakeHealthConnectGateway
    private lateinit var repository: LocalHealthRepository
    private lateinit var accountRepository: LocalAccountRepository
    private var now: Long = 1_000L
    private val executedSql = CopyOnWriteArrayList<String>()

    @Before
    fun setUp() {
        now = 1_000L
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room.inMemoryDatabaseBuilder(context, MusFitDatabase::class.java)
                .allowMainThreadQueries()
                .setQueryCallback(
                    RoomDatabase.QueryCallback { sqlQuery, _ -> executedSql += sqlQuery },
                    Executor { command -> command.run() },
                )
                .build()
        gateway = FakeHealthConnectGateway()
        accountRepository = LocalAccountRepository(database.accountDao(), clock = { 1_000L })
        runBlocking { accountRepository.ensureActiveAccount() }
        repository = LocalHealthRepository(
            gateway = gateway,
            healthDao = database.healthDao(),
            trainingDao = database.trainingDao(),
            accountRepository = accountRepository,
            clock = { now },
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun importDailySummary_persistsSummaryAndSyncState() = runTest {
        val date = LocalDate.of(2026, 6, 20)

        val result = repository.importDailySummary(date)

        val summary = database.healthDao().observeDailySummary(TEST_ACCOUNT_ID, date.toEpochDay()).first()
        assertEquals(HealthConnectImportResult.Complete::class, result::class)
        val syncState = database.healthDao().observeHealthConnectSyncState(TEST_ACCOUNT_ID).first()

        assertEquals(1234L, summary?.steps)
        assertEquals(250.0, summary?.activeCaloriesKcal ?: 0.0, 0.01)
        assertEquals(2_050.0, summary?.totalCaloriesKcal ?: 0.0, 0.01)
        assertEquals(4_200.0, summary?.distanceMeters ?: 0.0, 0.01)
        assertEquals(435L, summary?.sleepMinutes)
        assertEquals(47L, summary?.exerciseMinutes)
        assertEquals(2, summary?.exerciseSessionCount)
        assertEquals(82.5, summary?.latestWeightKg ?: 0.0, 0.01)
        assertEquals(18.2, summary?.latestBodyFatPercent ?: 0.0, 0.01)
        assertEquals(58L, summary?.restingHeartRateBpm)
        assertEquals(62.5, summary?.hrvRmssdMillis ?: 0.0, 0.01)
        assertEquals(1_000L, summary?.updatedAtEpochMillis)
        val weights = database.healthDao().getBodyMetrics(TEST_ACCOUNT_ID, "weight", 0L)
        val bodyFat = database.healthDao().getBodyMetrics(TEST_ACCOUNT_ID, "body_fat", 0L)
        assertEquals(1, weights.size)
        assertEquals(82.5, weights.single().value, 0.01)
        assertEquals("health_connect", weights.single().source)
        assertEquals("hc-weight-1", weights.single().externalId)
        assertEquals(1, bodyFat.size)
        assertEquals(18.2, bodyFat.single().value, 0.01)
        assertEquals("%", bodyFat.single().unit)
        assertEquals("hc-body-fat-1", bodyFat.single().externalId)
        assertEquals(1_000L, syncState?.lastImportAtEpochMillis)
        assertEquals(true, syncState?.isAvailable)
    }

    @Test
    fun importDailySummary_clearsCompletedMetricsWhenProviderLegitimatelyReturnsEmpty() = runTest {
        val date = LocalDate.of(2026, 6, 20)
        database.healthDao().upsertDailySummary(
            DailyHealthSummaryEntity(
                accountId = TEST_ACCOUNT_ID,
                dateEpochDay = date.toEpochDay(),
                steps = 7_800L,
                activeCaloriesKcal = 360.0,
                totalCaloriesKcal = 2_150.0,
                distanceMeters = 5_200.0,
                sleepMinutes = 430L,
                exerciseMinutes = 35L,
                exerciseSessionCount = 1,
                latestWeightKg = 80.9,
                latestBodyFatPercent = 14.8,
                restingHeartRateBpm = 58L,
                hrvRmssdMillis = 66.0,
                updatedAtEpochMillis = 500L,
            ),
        )
        gateway.dailySummaryFactory = { ImportedDailyHealthSummary() }

        val result = repository.importDailySummary(date)

        val summary = database.healthDao().observeDailySummary(TEST_ACCOUNT_ID, date.toEpochDay()).first()
        assertEquals(HealthConnectImportResult.Cleared::class, result::class)
        assertNull(summary?.steps)
        assertNull(summary?.activeCaloriesKcal)
        assertNull(summary?.totalCaloriesKcal)
        assertNull(summary?.distanceMeters)
        assertNull(summary?.sleepMinutes)
        assertNull(summary?.exerciseMinutes)
        assertNull(summary?.exerciseSessionCount)
        assertNull(summary?.latestWeightKg)
        assertNull(summary?.latestBodyFatPercent)
        assertNull(summary?.restingHeartRateBpm)
        assertNull(summary?.hrvRmssdMillis)
        assertEquals(1_000L, summary?.updatedAtEpochMillis)
    }

    @Test
    fun importDailySummary_partialReadUpdatesSuccessfulMetricAndPreservesFailedMetric() = runTest {
        val date = LocalDate.of(2026, 6, 20)
        database.healthDao().upsertDailySummary(
            DailyHealthSummaryEntity(
                accountId = TEST_ACCOUNT_ID,
                dateEpochDay = date.toEpochDay(),
                steps = 7_800L,
                activeCaloriesKcal = 360.0,
                totalCaloriesKcal = null,
                distanceMeters = null,
                sleepMinutes = null,
                exerciseMinutes = null,
                exerciseSessionCount = null,
                latestWeightKg = null,
                latestBodyFatPercent = null,
                restingHeartRateBpm = null,
                hrvRmssdMillis = null,
                updatedAtEpochMillis = 500L,
            ),
        )
        val status = HealthConnectStatus(HealthConnectAvailability.Available, setOf("steps", "active"))
        gateway.dailyReadResultFactory = {
            HealthConnectDailyReadResult.Partial(
                summary = ImportedDailyHealthSummary(steps = 9_100L),
                completedMetrics = setOf(HealthConnectMetric.Steps),
                failures = listOf(HealthConnectMetricFailure(HealthConnectMetric.ActiveCalories, "provider read failed")),
                status = status,
            )
        }
        now = 2_000L

        val result = repository.importDailySummary(date)
        val summary = database.healthDao().getDailySummary(TEST_ACCOUNT_ID, date.toEpochDay())
        val syncState = database.healthDao().getHealthConnectSyncState(TEST_ACCOUNT_ID)

        assertEquals(HealthConnectImportResult.Partial::class, result::class)
        assertEquals(9_100L, summary?.steps)
        assertEquals(360.0, summary?.activeCaloriesKcal ?: 0.0, 0.01)
        assertEquals(2_000L, summary?.updatedAtEpochMillis)
        assertEquals(2_000L, syncState?.lastImportAtEpochMillis)
        assertEquals(true, syncState?.lastFailureMessage?.contains("provider read failed"))
    }

    @Test
    fun importDailySummary_totalFailurePreservesCachedDataAndDoesNotAdvanceLastSuccess() = runTest {
        val date = LocalDate.of(2026, 6, 20)
        repository.importDailySummary(date)
        val before = database.healthDao().getDailySummary(TEST_ACCOUNT_ID, date.toEpochDay())
        gateway.dailyReadResultFactory = {
            HealthConnectDailyReadResult.Failure(
                message = "provider unavailable",
                status = HealthConnectStatus(HealthConnectAvailability.Available, setOf("steps")),
            )
        }
        now = 2_000L

        val result = repository.importDailySummary(date)
        val after = database.healthDao().getDailySummary(TEST_ACCOUNT_ID, date.toEpochDay())
        val syncState = database.healthDao().getHealthConnectSyncState(TEST_ACCOUNT_ID)

        assertEquals(HealthConnectImportResult.Failure::class, result::class)
        assertEquals(before, after)
        assertEquals(1_000L, syncState?.lastImportAtEpochMillis)
        assertEquals("provider unavailable", syncState?.lastFailureMessage)
    }

    @Test
    fun importDailySummary_permissionRevocationRecordsUnavailableWithoutStaleSuccess() = runTest {
        val date = LocalDate.of(2026, 6, 20)
        gateway.dailyReadResultFactory = {
            HealthConnectDailyReadResult.Unavailable(
                status = HealthConnectStatus(HealthConnectAvailability.Available, emptySet()),
                reason = HealthConnectUnavailableReason.PermissionsUnavailable,
                message = "Health Connect read permissions are unavailable.",
            )
        }

        val result = repository.importDailySummary(date)
        val syncState = database.healthDao().getHealthConnectSyncState(TEST_ACCOUNT_ID)

        assertEquals(HealthConnectImportResult.Unavailable::class, result::class)
        assertNull(database.healthDao().getDailySummary(TEST_ACCOUNT_ID, date.toEpochDay()))
        assertNull(syncState?.lastImportAtEpochMillis)
        assertEquals(true, syncState?.isAvailable)
        assertEquals("Health Connect read permissions are unavailable.", syncState?.lastFailureMessage)
    }

    @Test
    fun importDailySummary_providerDeletionRemovesOnlyStaleHealthConnectBodyMetric() = runTest {
        val date = LocalDate.of(2026, 6, 20)
        val measuredAt = date.toEpochDay() * 86_400_000L + 12 * 60 * 60 * 1_000L
        val providerWeight = bodyMetric("provider-weight", "weight", "health_connect", measuredAt)
        val manualWeight = bodyMetric("manual-weight", "weight", "manual", measuredAt + 1)
        val providerBodyFat = bodyMetric("provider-body-fat", "body_fat", "health_connect", measuredAt + 2)
        database.healthDao().upsertBodyMetric(providerWeight)
        database.healthDao().upsertBodyMetric(manualWeight)
        database.healthDao().upsertBodyMetric(providerBodyFat)
        database.healthDao().upsertDailySummary(
            DailyHealthSummaryEntity(
                accountId = TEST_ACCOUNT_ID,
                dateEpochDay = date.toEpochDay(),
                steps = null,
                activeCaloriesKcal = null,
                totalCaloriesKcal = null,
                distanceMeters = null,
                sleepMinutes = null,
                exerciseMinutes = null,
                exerciseSessionCount = null,
                latestWeightKg = 82.0,
                latestBodyFatPercent = 18.0,
                restingHeartRateBpm = null,
                hrvRmssdMillis = null,
                updatedAtEpochMillis = 500L,
            ),
        )
        val status = HealthConnectStatus(HealthConnectAvailability.Available, setOf("weight"))
        gateway.dailyReadResultFactory = {
            HealthConnectDailyReadResult.Empty(
                completedMetrics = setOf(HealthConnectMetric.Weight),
                status = status,
            )
        }
        executedSql.clear()

        val result = repository.importDailySummary(date)

        assertEquals(HealthConnectImportResult.Cleared::class, result::class)
        assertEquals(listOf(manualWeight), database.healthDao().getBodyMetrics(TEST_ACCOUNT_ID, "weight", 0L))
        assertEquals(listOf(providerBodyFat), database.healthDao().getBodyMetrics(TEST_ACCOUNT_ID, "body_fat", 0L))
        val summary = database.healthDao().getDailySummary(TEST_ACCOUNT_ID, date.toEpochDay())
        assertNull(summary?.latestWeightKg)
        assertEquals(18.0, summary?.latestBodyFatPercent ?: 0.0, 0.01)

        repository.importDailySummary(date)
        assertEquals(listOf(manualWeight), database.healthDao().getBodyMetrics(TEST_ACCOUNT_ID, "weight", 0L))
    }

    @Test
    fun importDailySummary_partialReadReconcilesCompletedBodyTypeAndPreservesFailedType() = runTest {
        val date = LocalDate.of(2026, 6, 20)
        val measuredAt = date.toEpochDay() * 86_400_000L + 12 * 60 * 60 * 1_000L
        val staleWeight = bodyMetric("stale-weight", "weight", "health_connect", measuredAt)
        val retainedBodyFat = bodyMetric("retained-body-fat", "body_fat", "health_connect", measuredAt + 1)
        database.healthDao().upsertBodyMetric(staleWeight)
        database.healthDao().upsertBodyMetric(retainedBodyFat)
        val importedWeight = ImportedBodyMetric(
            type = "weight",
            value = 80.5,
            unit = "kg",
            measuredAtEpochMillis = measuredAt + 2,
            externalId = "new-weight",
        )
        val status = HealthConnectStatus(HealthConnectAvailability.Available, setOf("weight"))
        gateway.dailyReadResultFactory = {
            HealthConnectDailyReadResult.Partial(
                summary = ImportedDailyHealthSummary(
                    latestWeightKg = importedWeight.value,
                    bodyMetrics = listOf(importedWeight),
                ),
                completedMetrics = setOf(HealthConnectMetric.Weight),
                failures = listOf(HealthConnectMetricFailure(HealthConnectMetric.BodyFat, "body fat failed")),
                status = status,
            )
        }

        val result = repository.importDailySummary(date)

        assertEquals(HealthConnectImportResult.Partial::class, result::class)
        val weights = database.healthDao().getBodyMetrics(TEST_ACCOUNT_ID, "weight", 0L)
        assertEquals(listOf("health-connect-weight-new-weight"), weights.map { it.id })
        assertEquals(listOf(retainedBodyFat), database.healthDao().getBodyMetrics(TEST_ACCOUNT_ID, "body_fat", 0L))
        assertEquals(
            1,
            executedSql.count { sql ->
                sql.contains("SELECT * FROM body_metrics", ignoreCase = true) &&
                    sql.contains("source = 'health_connect'", ignoreCase = true)
            },
        )
        assertEquals(
            1,
            executedSql.count { sql ->
                sql.contains("DELETE FROM body_metrics", ignoreCase = true) &&
                    sql.contains("source = 'health_connect'", ignoreCase = true)
            },
        )
    }

    @Test
    fun importDailySummary_permissionRevocationMarksCacheStaleWithoutDeletingBodyMetrics() = runTest {
        val date = LocalDate.of(2026, 6, 20)
        repository.importDailySummary(date)
        val measuredAt = date.toEpochDay() * 86_400_000L + 13 * 60 * 60 * 1_000L
        database.healthDao().upsertBodyMetric(bodyMetric("manual-weight", "weight", "manual", measuredAt))
        val before = database.healthDao().getBodyMetrics(TEST_ACCOUNT_ID, "weight", 0L)
        gateway.dailyReadResultFactory = {
            HealthConnectDailyReadResult.Unavailable(
                status = HealthConnectStatus(HealthConnectAvailability.Available, emptySet()),
                reason = HealthConnectUnavailableReason.PermissionsUnavailable,
                message = "Health Connect read permissions are unavailable.",
            )
        }
        now = 2_000L

        val result = repository.importDailySummary(date)

        assertEquals(HealthConnectImportResult.Unavailable::class, result::class)
        assertEquals(before, database.healthDao().getBodyMetrics(TEST_ACCOUNT_ID, "weight", 0L))
        val syncState = database.healthDao().getHealthConnectSyncState(TEST_ACCOUNT_ID)
        assertEquals(1_000L, syncState?.lastImportAtEpochMillis)
        assertEquals(emptySet<String>(), syncState?.grantedPermissionsCsv?.split(',')?.filter(String::isNotBlank)?.toSet())
        assertEquals("Health Connect read permissions are unavailable.", syncState?.lastFailureMessage)
    }

    @Test
    fun importDailySummary_cancellationPropagatesWithinOneSecondWithoutWritingState() = runTest {
        val date = LocalDate.of(2026, 6, 20)
        gateway.readFailure = CancellationException("cancelled by caller")
        val startedAt = System.nanoTime()
        var cancellation: CancellationException? = null

        try {
            repository.importDailySummary(date)
        } catch (caught: CancellationException) {
            cancellation = caught
        }
        val elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000

        assertEquals("cancelled by caller", cancellation?.message)
        assertEquals(true, elapsedMillis < 1_000L)
        assertNull(database.healthDao().getDailySummary(TEST_ACCOUNT_ID, date.toEpochDay()))
        assertNull(database.healthDao().getHealthConnectSyncState(TEST_ACCOUNT_ID))
    }

    @Test
    fun importDailySummary_passesStoredPreferredStepsPackageToGateway() = runTest {
        val date = LocalDate.of(2026, 6, 20)
        repository.setPreferredStepsPackage("com.google.android.apps.fitness")

        repository.importDailySummary(date)

        assertEquals("com.google.android.apps.fitness", gateway.preferredStepsPackages.last())
        assertEquals(
            "com.google.android.apps.fitness",
            repository.observePreferredStepsPackage().first(),
        )
    }

    @Test
    fun importDailySummary_passesNullPreferredPackage_whenNoneChosen() = runTest {
        repository.importDailySummary(LocalDate.of(2026, 6, 20))

        assertNull(gateway.preferredStepsPackages.last())
    }

    @Test
    fun setPreferredStepsPackage_updatesExistingRowAndCanClear() = runTest {
        repository.importDailySummary(LocalDate.of(2026, 6, 20))

        repository.setPreferredStepsPackage("com.samsung.health")
        assertEquals("com.samsung.health", repository.observePreferredStepsPackage().first())

        // Re-importing must not wipe the chosen source (upsertSyncState preserves it).
        repository.importDailySummary(LocalDate.of(2026, 6, 21))
        assertEquals("com.samsung.health", repository.observePreferredStepsPackage().first())

        repository.setPreferredStepsPackage(null)
        assertNull(repository.observePreferredStepsPackage().first())
    }

    @Test
    fun readStepSources_delegatesToGateway() = runTest {
        gateway.stepSources = listOf(
            StepSource("com.google.android.apps.fitness", "Fit", 5_800L),
            StepSource("android", "Your phone", 900L),
        )

        val sources = repository.readStepSources(LocalDate.of(2026, 6, 20))

        assertEquals(listOf("com.google.android.apps.fitness", "android"), sources.map { it.packageName })
        assertEquals(5_800L, sources.first().steps)
    }

    @Test
    fun refreshRecentData_importsSevenDayWindowEndingAtAnchor() = runTest {
        val anchor = LocalDate.of(2026, 6, 20)

        val result = repository.refreshRecentData(anchor)

        assertEquals(
            (0L..6L).map { anchor.minusDays(6L - it) },
            gateway.importedDates,
        )
        assertEquals(1, gateway.rangeReadCalls)
        assertEquals(0, gateway.dailyReadCalls)
        assertEquals(7, result.importedDayCount)
        assertEquals(14, result.bodyMetricCount)
        assertEquals(
            7,
            database.healthDao().observeDailySummariesInRange(
                TEST_ACCOUNT_ID,
                anchor.minusDays(6).toEpochDay(),
                anchor.toEpochDay(),
            ).first().size,
        )
    }

    @Test
    fun refreshRecentData_countsPartialEmptyAndFailedDaysWithoutReportingStaleSuccess() = runTest {
        val endDate = LocalDate.of(2026, 7, 14)
        val status = HealthConnectStatus(HealthConnectAvailability.Available, setOf("weight"))
        gateway.dailyReadResultFactory = { date ->
            when (date) {
                endDate.minusDays(2) -> HealthConnectDailyReadResult.Partial(
                    summary = ImportedDailyHealthSummary(
                        latestWeightKg = 81.5,
                        bodyMetrics = listOf(
                            ImportedBodyMetric(
                                type = "weight",
                                value = 81.5,
                                unit = "kg",
                                measuredAtEpochMillis = 1_700_000_000_000L,
                                externalId = "partial-weight",
                            ),
                        ),
                    ),
                    completedMetrics = setOf(HealthConnectMetric.Weight),
                    failures = listOf(HealthConnectMetricFailure(HealthConnectMetric.Steps, "steps failed")),
                    status = status,
                )

                endDate.minusDays(1) -> HealthConnectDailyReadResult.Empty(
                    completedMetrics = setOf(HealthConnectMetric.Weight),
                    status = status,
                )

                else -> HealthConnectDailyReadResult.Failure("provider unavailable", status)
            }
        }

        val result = repository.refreshRecentData(endDate, days = 3)

        assertEquals(2, result.importedDayCount)
        assertEquals(1, result.bodyMetricCount)
        assertEquals(1, result.partialDayCount)
        assertEquals(1, result.emptyDayCount)
        assertEquals(1, result.failedDayCount)
    }

    @Test
    fun healthState_followsActiveAccountWithoutCrossAccountLeakage() = runTest {
        val date = LocalDate.of(2026, 6, 20)
        val firstAccount = accountRepository.ensureActiveAccount()
        repository.importDailySummary(date)
        repository.setPreferredStepsPackage("com.samsung.health")

        val secondAccountId = accountRepository.createAccount("Partner")
        accountRepository.switchAccount(secondAccountId)

        assertNull(repository.observeDailySummary(date).first())
        assertEquals(emptyList<Any>(), repository.observeWeightSeries(0L).first())
        assertNull(repository.observePreferredStepsPackage().first())
        repository.setPreferredStepsPackage("com.google.android.apps.fitness")

        accountRepository.switchAccount(firstAccount.id)
        assertEquals(1234L, repository.observeDailySummary(date).first()?.steps)
        assertEquals(1, repository.observeWeightSeries(0L).first().size)
        assertEquals("com.samsung.health", repository.observePreferredStepsPackage().first())

        accountRepository.switchAccount(secondAccountId)
        assertEquals("com.google.android.apps.fitness", repository.observePreferredStepsPackage().first())
    }

    @Test
    fun exportLatestWorkout_exportsPersistedWorkoutAndMarksSession() = runTest {
        database.trainingDao().upsertExerciseDefinition(
            ExerciseEntity(
                id = "exercise-1",
                name = "Bench Press",
                category = "strength",
                equipment = null,
                targetMuscles = "",
                isCustom = true,
            ),
        )
        database.trainingDao().upsertWorkoutSession(
            WorkoutSessionEntity(
                accountId = TEST_ACCOUNT_ID,
                id = "session-1",
                routineId = null,
                title = "Bench workout",
                status = "completed",
                startedAtEpochMillis = 500L,
                endedAtEpochMillis = 900L,
                notes = null,
                healthConnectRecordId = null,
                healthConnectLastExportedAtEpochMillis = null,
            ),
        )
        database.trainingDao().upsertWorkoutSet(
            WorkoutSetEntity(
                accountId = TEST_ACCOUNT_ID,
                id = "set-1",
                sessionId = "session-1",
                exerciseId = "exercise-1",
                sortOrder = 0,
                setType = "working",
                reps = 5,
                weightKg = 100.0,
                durationSeconds = null,
                distanceMeters = null,
                rpe = null,
                notes = null,
                completed = true,
            ),
        )

        val recordId = repository.exportLatestWorkout()

        val savedSession = database.trainingDao().getWorkoutSession(TEST_ACCOUNT_ID, "session-1")
        val syncState = database.healthDao().observeHealthConnectSyncState(TEST_ACCOUNT_ID).first()

        assertEquals("record-id", recordId)
        assertEquals("session-1", gateway.exportedSession?.localSessionId)
        assertEquals(1, gateway.exportedSets.size)
        assertEquals("record-id", savedSession?.healthConnectRecordId)
        assertEquals(1_000L, savedSession?.healthConnectLastExportedAtEpochMillis)
        assertNotNull(syncState?.lastExportAtEpochMillis)
    }

    @Test
    fun exportLatestWorkout_preservesExportedWorkoutSetsWhenMetadataIsSaved() = runTest {
        database.trainingDao().upsertExerciseDefinition(
            ExerciseEntity(
                id = "exercise-1",
                name = "Bench Press",
                category = "strength",
                equipment = null,
                targetMuscles = "",
                isCustom = true,
            ),
        )
        database.trainingDao().upsertWorkoutSession(
            WorkoutSessionEntity(
                accountId = TEST_ACCOUNT_ID,
                id = "session-1",
                routineId = null,
                title = "Bench workout",
                status = "completed",
                startedAtEpochMillis = 500L,
                endedAtEpochMillis = 900L,
                notes = null,
                healthConnectRecordId = null,
                healthConnectLastExportedAtEpochMillis = null,
            ),
        )
        database.trainingDao().upsertWorkoutSet(
            WorkoutSetEntity(
                accountId = TEST_ACCOUNT_ID,
                id = "set-1",
                sessionId = "session-1",
                exerciseId = "exercise-1",
                sortOrder = 0,
                setType = "working",
                reps = 5,
                weightKg = 100.0,
                durationSeconds = null,
                distanceMeters = null,
                rpe = null,
                notes = null,
                completed = true,
            ),
        )
        database.trainingDao().upsertWorkoutSet(
            WorkoutSetEntity(
                accountId = TEST_ACCOUNT_ID,
                id = "set-2",
                sessionId = "session-1",
                exerciseId = "exercise-1",
                sortOrder = 1,
                setType = "working",
                reps = 6,
                weightKg = 102.5,
                durationSeconds = null,
                distanceMeters = null,
                rpe = null,
                notes = null,
                completed = true,
            ),
        )

        repository.exportLatestWorkout()

        val savedSets = database.trainingDao().getWorkoutSets(TEST_ACCOUNT_ID, "session-1")

        assertEquals(listOf("set-1", "set-2"), savedSets.map { it.id })
    }

    @Test
    fun exportLatestWorkout_unchangedRetryUsesPersistedProviderIdentityWithoutAnotherWrite() = runTest {
        seedCompletedWorkout()

        val first = repository.exportLatestWorkout()
        val retry = repository.exportLatestWorkout()
        val persisted = database.healthDao().getHealthConnectExportRecord(
            TEST_ACCOUNT_ID,
            "workout",
            "session-stable",
        )

        assertEquals("record-id", first)
        assertEquals(first, retry)
        assertEquals(1, gateway.exportedIdentities.size)
        assertEquals(1L, gateway.exportedIdentities.single().clientRecordVersion)
        assertEquals("record-id", persisted?.providerRecordId)
        assertEquals(gateway.exportedIdentities.single().clientRecordId, persisted?.clientRecordId)
    }

    @Test
    fun exportLatestWorkout_editKeepsClientIdAndAdvancesVersion() = runTest {
        seedCompletedWorkout()
        repository.exportLatestWorkout()
        val saved = requireNotNull(database.trainingDao().getWorkoutSession(TEST_ACCOUNT_ID, "session-stable"))
        database.trainingDao().upsertWorkoutSession(saved.copy(notes = "Edited after export"))

        repository.exportLatestWorkout()
        val persisted = database.healthDao().getHealthConnectExportRecord(
            TEST_ACCOUNT_ID,
            "workout",
            "session-stable",
        )

        assertEquals(listOf(1L, 2L), gateway.exportedIdentities.map { it.clientRecordVersion })
        assertEquals(1, gateway.exportedIdentities.map { it.clientRecordId }.distinct().size)
        assertEquals(2L, persisted?.clientRecordVersion)
    }

    @Test
    fun exportLatestWorkout_adoptsLegacyProviderIdentityWithoutRewritingHealthConnect() = runTest {
        seedCompletedWorkout()
        val legacySession = requireNotNull(
            database.trainingDao().getWorkoutSession(TEST_ACCOUNT_ID, "session-stable"),
        ).copy(
            healthConnectRecordId = "legacy-provider-id",
            healthConnectLastExportedAtEpochMillis = 777L,
        )
        database.trainingDao().upsertWorkoutSession(legacySession)

        val recordId = repository.exportLatestWorkout()
        val persisted = database.healthDao().getHealthConnectExportRecord(
            TEST_ACCOUNT_ID,
            "workout",
            "session-stable",
        )

        assertEquals("legacy-provider-id", recordId)
        assertEquals(emptyList<HealthConnectRecordIdentity>(), gateway.exportedIdentities)
        assertEquals("legacy-provider-id", persisted?.providerRecordId)
        assertEquals(1L, persisted?.clientRecordVersion)
        assertEquals(777L, persisted?.exportedAtEpochMillis)
        assertNotNull(persisted?.payloadFingerprint)
    }

    @Test
    fun exportLatestWorkout_skipsActiveSessionAndExportsLatestCompletedWorkout() = runTest {
        database.trainingDao().upsertExerciseDefinition(
            ExerciseEntity(
                id = "exercise-1",
                name = "Bench Press",
                category = "strength",
                equipment = null,
                targetMuscles = "",
                isCustom = true,
            ),
        )
        database.trainingDao().upsertWorkoutSession(
            WorkoutSessionEntity(
                accountId = TEST_ACCOUNT_ID,
                id = "session-completed",
                routineId = null,
                title = "Completed workout",
                status = "completed",
                startedAtEpochMillis = 500L,
                endedAtEpochMillis = 900L,
                notes = null,
                healthConnectRecordId = null,
                healthConnectLastExportedAtEpochMillis = null,
            ),
        )
        database.trainingDao().upsertWorkoutSession(
            WorkoutSessionEntity(
                accountId = TEST_ACCOUNT_ID,
                id = "session-active",
                routineId = null,
                title = "Active workout",
                status = "active",
                startedAtEpochMillis = 950L,
                endedAtEpochMillis = null,
                notes = null,
                healthConnectRecordId = null,
                healthConnectLastExportedAtEpochMillis = null,
            ),
        )
        database.trainingDao().upsertWorkoutSet(
            WorkoutSetEntity(
                accountId = TEST_ACCOUNT_ID,
                id = "set-completed",
                sessionId = "session-completed",
                exerciseId = "exercise-1",
                sortOrder = 0,
                setType = "working",
                reps = 5,
                weightKg = 100.0,
                durationSeconds = null,
                distanceMeters = null,
                rpe = null,
                notes = null,
                completed = true,
            ),
        )
        database.trainingDao().upsertWorkoutSet(
            WorkoutSetEntity(
                accountId = TEST_ACCOUNT_ID,
                id = "set-active",
                sessionId = "session-active",
                exerciseId = "exercise-1",
                sortOrder = 0,
                setType = "working",
                reps = 3,
                weightKg = 120.0,
                durationSeconds = null,
                distanceMeters = null,
                rpe = null,
                notes = null,
                completed = true,
            ),
        )

        val recordId = repository.exportLatestWorkout()

        assertEquals("record-id", recordId)
        assertEquals("session-completed", gateway.exportedSession?.localSessionId)
        assertEquals(listOf("set-completed"), gateway.exportedSets.map { it.localSetId })
        assertNull(database.trainingDao().getWorkoutSession(TEST_ACCOUNT_ID, "session-active")?.healthConnectRecordId)
    }

    @Test
    fun exportLatestWorkout_filtersOutIncompleteSetsFromCompletedSession() = runTest {
        database.trainingDao().upsertExerciseDefinition(
            ExerciseEntity(
                id = "exercise-1",
                name = "Bench Press",
                category = "strength",
                equipment = null,
                targetMuscles = "",
                isCustom = true,
            ),
        )
        database.trainingDao().upsertWorkoutSession(
            WorkoutSessionEntity(
                accountId = TEST_ACCOUNT_ID,
                id = "session-1",
                routineId = null,
                title = "Bench workout",
                status = "completed",
                startedAtEpochMillis = 500L,
                endedAtEpochMillis = 900L,
                notes = null,
                healthConnectRecordId = null,
                healthConnectLastExportedAtEpochMillis = null,
            ),
        )
        database.trainingDao().upsertWorkoutSet(
            WorkoutSetEntity(
                accountId = TEST_ACCOUNT_ID,
                id = "set-completed",
                sessionId = "session-1",
                exerciseId = "exercise-1",
                sortOrder = 0,
                setType = "working",
                reps = 5,
                weightKg = 100.0,
                durationSeconds = null,
                distanceMeters = null,
                rpe = null,
                notes = null,
                completed = true,
            ),
        )
        database.trainingDao().upsertWorkoutSet(
            WorkoutSetEntity(
                accountId = TEST_ACCOUNT_ID,
                id = "set-incomplete",
                sessionId = "session-1",
                exerciseId = "exercise-1",
                sortOrder = 1,
                setType = "working",
                reps = 6,
                weightKg = 95.0,
                durationSeconds = null,
                distanceMeters = null,
                rpe = null,
                notes = null,
                completed = false,
            ),
        )

        repository.exportLatestWorkout()

        assertEquals(listOf("set-completed"), gateway.exportedSets.map { it.localSetId })
    }

    @Test
    fun exportLatestWorkout_fallsBackWhenNewestCompletedWorkoutHasNoCompletedSets() = runTest {
        database.trainingDao().upsertExerciseDefinition(
            ExerciseEntity(
                id = "exercise-1",
                name = "Bench Press",
                category = "strength",
                equipment = null,
                targetMuscles = "",
                isCustom = true,
            ),
        )
        database.trainingDao().upsertWorkoutSession(
            WorkoutSessionEntity(
                accountId = TEST_ACCOUNT_ID,
                id = "session-older-completed",
                routineId = null,
                title = "Older workout",
                status = "completed",
                startedAtEpochMillis = 500L,
                endedAtEpochMillis = 900L,
                notes = null,
                healthConnectRecordId = null,
                healthConnectLastExportedAtEpochMillis = null,
            ),
        )
        database.trainingDao().upsertWorkoutSession(
            WorkoutSessionEntity(
                accountId = TEST_ACCOUNT_ID,
                id = "session-newer-empty",
                routineId = null,
                title = "Newer empty workout",
                status = "completed",
                startedAtEpochMillis = 950L,
                endedAtEpochMillis = 1_250L,
                notes = null,
                healthConnectRecordId = null,
                healthConnectLastExportedAtEpochMillis = null,
            ),
        )
        database.trainingDao().upsertWorkoutSet(
            WorkoutSetEntity(
                accountId = TEST_ACCOUNT_ID,
                id = "set-older-completed",
                sessionId = "session-older-completed",
                exerciseId = "exercise-1",
                sortOrder = 0,
                setType = "working",
                reps = 5,
                weightKg = 100.0,
                durationSeconds = null,
                distanceMeters = null,
                rpe = null,
                notes = null,
                completed = true,
            ),
        )
        database.trainingDao().upsertWorkoutSet(
            WorkoutSetEntity(
                accountId = TEST_ACCOUNT_ID,
                id = "set-newer-incomplete",
                sessionId = "session-newer-empty",
                exerciseId = "exercise-1",
                sortOrder = 0,
                setType = "working",
                reps = 6,
                weightKg = 95.0,
                durationSeconds = null,
                distanceMeters = null,
                rpe = null,
                notes = null,
                completed = false,
            ),
        )

        val recordId = repository.exportLatestWorkout()

        assertEquals("record-id", recordId)
        assertEquals("session-older-completed", gateway.exportedSession?.localSessionId)
        assertEquals(listOf("set-older-completed"), gateway.exportedSets.map { it.localSetId })
        assertNull(database.trainingDao().getWorkoutSession(TEST_ACCOUNT_ID, "session-newer-empty")?.healthConnectRecordId)
    }

    @Test
    fun deleteAuthoredRecords_removesOnlyLedgerBackedRecordsAndMakesRetryANoOp() = runTest {
        seedCompletedWorkout()
        repository.exportLatestWorkout()
        listOf("nutrition" to "meal-1", "hydration" to "2026-07-14").forEach { (type, localId) ->
            database.healthDao().upsertHealthConnectExportRecord(exportRecord(type, localId))
        }
        gateway.deleteResultFactory = { records -> HealthConnectDeleteResult.Complete(records) }

        val result = repository.deleteAuthoredRecords()

        assertEquals(HealthConnectDeleteResult.Complete::class, result::class)
        assertEquals(3, gateway.deletedRequests.single().size)
        assertEquals(emptyList<HealthConnectExportRecordEntity>(), database.healthDao().getHealthConnectExportRecords(TEST_ACCOUNT_ID, "workout"))
        assertEquals(emptyList<HealthConnectExportRecordEntity>(), database.healthDao().getHealthConnectExportRecords(TEST_ACCOUNT_ID, "nutrition"))
        assertEquals(emptyList<HealthConnectExportRecordEntity>(), database.healthDao().getHealthConnectExportRecords(TEST_ACCOUNT_ID, "hydration"))
        val session = database.trainingDao().getWorkoutSession(TEST_ACCOUNT_ID, "session-stable")
        assertNull(session?.healthConnectRecordId)
        assertNull(session?.healthConnectLastExportedAtEpochMillis)

        val retry = repository.deleteAuthoredRecords()

        assertEquals(HealthConnectDeleteResult.Complete(emptySet()), retry)
        assertEquals(1, gateway.deletedRequests.size)
    }

    @Test
    fun deleteAuthoredRecords_keepsFailedLedgerRowsForRetry() = runTest {
        val nutrition = exportRecord("nutrition", "meal-1")
        val hydration = exportRecord("hydration", "2026-07-14")
        database.healthDao().upsertHealthConnectExportRecord(nutrition)
        database.healthDao().upsertHealthConnectExportRecord(hydration)
        gateway.deleteResultFactory = { records ->
            val deleted = records.filterTo(linkedSetOf()) { it.type == HealthConnectAuthoredRecordType.Nutrition }
            HealthConnectDeleteResult.Partial(
                deletedRecords = deleted,
                failures = listOf(HealthConnectDeleteFailure(HealthConnectAuthoredRecordType.Hydration, "provider failed")),
            )
        }

        val result = repository.deleteAuthoredRecords()

        assertEquals(HealthConnectDeleteResult.Partial::class, result::class)
        assertEquals(emptyList<HealthConnectExportRecordEntity>(), database.healthDao().getHealthConnectExportRecords(TEST_ACCOUNT_ID, "nutrition"))
        assertEquals(listOf(hydration), database.healthDao().getHealthConnectExportRecords(TEST_ACCOUNT_ID, "hydration"))
    }

    private class FakeHealthConnectGateway : HealthConnectGateway {
        var exportedSession: HealthConnectWorkoutExport? = null
        var exportedSets = emptyList<com.musfit.domain.health.HealthConnectWorkoutSetExport>()
        val exportedIdentities = mutableListOf<HealthConnectRecordIdentity>()
        val importedDates = mutableListOf<LocalDate>()
        val preferredStepsPackages = mutableListOf<String?>()
        var dailyReadCalls: Int = 0
        var rangeReadCalls: Int = 0
        var stepSources: List<StepSource> = emptyList()
        var dailyReadResultFactory: ((LocalDate) -> HealthConnectDailyReadResult)? = null
        var readFailure: Throwable? = null
        val deletedRequests = mutableListOf<Set<HealthConnectAuthoredRecord>>()
        var deleteResultFactory: (Set<HealthConnectAuthoredRecord>) -> HealthConnectDeleteResult = {
            HealthConnectDeleteResult.Complete(it)
        }
        var dailySummaryFactory: (LocalDate) -> ImportedDailyHealthSummary = { date ->
            ImportedDailyHealthSummary(
                steps = 1234L,
                activeCaloriesKcal = 250.0,
                totalCaloriesKcal = 2_050.0,
                distanceMeters = 4_200.0,
                sleepMinutes = 435L,
                exerciseMinutes = 47L,
                exerciseSessionCount = 2,
                latestWeightKg = 82.5,
                latestBodyFatPercent = 18.2,
                restingHeartRateBpm = 58L,
                hrvRmssdMillis = 62.5,
                bodyMetrics = listOf(
                    ImportedBodyMetric(
                        type = "weight",
                        value = 82.5,
                        unit = "kg",
                        measuredAtEpochMillis = date.toEpochDay() * 86_400_000L + 8 * 60 * 60 * 1_000L,
                        externalId = "hc-weight-1",
                    ),
                    ImportedBodyMetric(
                        type = "body_fat",
                        value = 18.2,
                        unit = "%",
                        measuredAtEpochMillis = date.toEpochDay() * 86_400_000L + 8 * 60 * 60 * 1_000L,
                        externalId = "hc-body-fat-1",
                    ),
                ),
            )
        }

        override suspend fun status(): HealthConnectStatus = HealthConnectStatus(
            availability = HealthConnectAvailability.Available,
            grantedPermissions = setOf("steps"),
        )

        override suspend fun requestablePermissions(): Set<String> = setOf("steps")

        override suspend fun foodRequestablePermissions(): Set<String> = emptySet()

        override suspend fun readStepSources(date: LocalDate): List<StepSource> = stepSources

        override suspend fun readDailySummary(
            date: LocalDate,
            preferredStepsPackage: String?,
        ): HealthConnectDailyReadResult {
            dailyReadCalls += 1
            return dailyResult(date, preferredStepsPackage)
        }

        override suspend fun readDailySummaries(
            startDate: LocalDate,
            endDateInclusive: LocalDate,
            preferredStepsPackage: String?,
        ): Map<LocalDate, HealthConnectDailyReadResult> {
            rangeReadCalls += 1
            return generateSequence(startDate) { date -> date.plusDays(1) }
                .takeWhile { date -> !date.isAfter(endDateInclusive) }
                .associateWith { date -> dailyResult(date, preferredStepsPackage) }
        }

        private suspend fun dailyResult(
            date: LocalDate,
            preferredStepsPackage: String?,
        ): HealthConnectDailyReadResult {
            importedDates += date
            preferredStepsPackages += preferredStepsPackage
            readFailure?.let { throw it }
            dailyReadResultFactory?.let { factory -> return factory(date) }
            val summary = dailySummaryFactory(date)
            val currentStatus = status()
            val completedMetrics = HealthConnectMetric.entries.toSet()
            return if (summary == ImportedDailyHealthSummary()) {
                HealthConnectDailyReadResult.Empty(completedMetrics, currentStatus)
            } else {
                HealthConnectDailyReadResult.Complete(summary, completedMetrics, currentStatus)
            }
        }

        override suspend fun exportWorkout(
            workout: HealthConnectWorkoutExport,
            identity: HealthConnectRecordIdentity,
        ): String {
            exportedSession = workout
            exportedSets = workout.sets
            exportedIdentities += identity
            return "record-id"
        }

        override suspend fun exportFood(payload: HealthConnectFoodExportPayload): HealthConnectFoodExportResult? = null

        override suspend fun deleteAuthoredRecords(records: Set<HealthConnectAuthoredRecord>): HealthConnectDeleteResult {
            deletedRequests += records
            return deleteResultFactory(records)
        }
    }

    private fun exportRecord(type: String, localId: String) = HealthConnectExportRecordEntity(
        accountId = TEST_ACCOUNT_ID,
        recordType = type,
        localEntityId = localId,
        clientRecordId = "$type-client-$localId",
        clientRecordVersion = 1,
        payloadFingerprint = "$type-fingerprint",
        providerRecordId = "$type-provider-$localId",
        exportedAtEpochMillis = 900L,
    )

    private fun bodyMetric(
        id: String,
        type: String,
        source: String,
        measuredAtEpochMillis: Long,
    ) = BodyMetricEntity(
        accountId = TEST_ACCOUNT_ID,
        id = id,
        type = type,
        value = if (type == "weight") 82.0 else 18.0,
        unit = if (type == "weight") "kg" else "%",
        measuredAtEpochMillis = measuredAtEpochMillis,
        source = source,
        externalId = id,
    )

    private suspend fun seedCompletedWorkout() {
        database.trainingDao().upsertExerciseDefinition(
            ExerciseEntity(
                id = "exercise-stable",
                name = "Bench Press",
                category = "strength",
                equipment = null,
                targetMuscles = "chest",
                isCustom = true,
            ),
        )
        database.trainingDao().upsertWorkoutSession(
            WorkoutSessionEntity(
                accountId = TEST_ACCOUNT_ID,
                id = "session-stable",
                routineId = null,
                title = "Stable workout",
                status = "completed",
                startedAtEpochMillis = 500L,
                endedAtEpochMillis = 900L,
                notes = null,
                healthConnectRecordId = null,
                healthConnectLastExportedAtEpochMillis = null,
            ),
        )
        database.trainingDao().upsertWorkoutSet(
            WorkoutSetEntity(
                accountId = TEST_ACCOUNT_ID,
                id = "set-stable",
                sessionId = "session-stable",
                exerciseId = "exercise-stable",
                sortOrder = 0,
                reps = 5,
                weightKg = 100.0,
                durationSeconds = null,
                distanceMeters = null,
                rpe = 8.0,
                notes = null,
                completed = true,
            ),
        )
    }
}
