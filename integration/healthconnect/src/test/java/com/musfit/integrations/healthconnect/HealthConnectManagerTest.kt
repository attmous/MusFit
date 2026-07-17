package com.musfit.integrations.healthconnect

import android.content.Context
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.test.core.app.ApplicationProvider
import com.musfit.domain.health.HealthConnectAvailability
import com.musfit.domain.health.HealthConnectDailyReadResult
import com.musfit.domain.health.HealthConnectDeleteResult
import com.musfit.domain.health.HealthConnectMetric
import com.musfit.domain.health.HealthConnectStatus
import com.musfit.domain.health.ImportedBodyMetric
import com.musfit.domain.health.ImportedDailyHealthSummary
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Duration
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.CancellationException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HealthConnectManagerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun readDailySummary_returnsEmptySummary_whenHealthConnectUnavailable() = runTest {
        val factory = FakeClientFactory()
        val manager = managerWith(
            status = HealthConnectStatus(HealthConnectAvailability.NotInstalled, emptySet()),
            factory = factory,
        )

        val result = manager.readDailySummary(LocalDate.of(2026, 6, 20))

        assertEquals(HealthConnectDailyReadResult.Unavailable::class, result::class)
        assertEquals(
            ImportedDailyHealthSummary(
                steps = null,
                activeCaloriesKcal = null,
                totalCaloriesKcal = null,
                distanceMeters = null,
                sleepMinutes = null,
                exerciseMinutes = null,
                exerciseSessionCount = null,
                latestWeightKg = null,
                latestBodyFatPercent = null,
                restingHeartRateBpm = null,
                hrvRmssdMillis = null,
                bodyMetrics = emptyList(),
            ),
            result.summary,
        )
        assertEquals(0, factory.createCount)
    }

    @Test
    fun readDailySummary_readsOnlyGrantedMetrics() = runTest {
        val client = FakeHealthConnectClientAdapter(
            steps = 9_876L,
            activeCaloriesKcal = 321.5,
            latestWeightKg = 81.2,
            totalCaloriesKcal = 2_350.0,
            distanceMeters = 6_200.0,
            sleepMinutes = 435L,
            exerciseMinutes = 55L,
            exerciseSessionCount = 2,
            latestBodyFatPercent = 18.4,
            restingHeartRateBpm = 55L,
            hrvRmssdMillis = 62.0,
        )
        val factory = FakeClientFactory(client)
        val manager = managerWith(
            status = HealthConnectStatus(
                availability = HealthConnectAvailability.Available,
                grantedPermissions = setOf(
                    readStepsPermission(),
                    readRestingHeartRatePermission(),
                ),
            ),
            factory = factory,
        )

        val summary = manager.readDailySummary(LocalDate.of(2026, 6, 20))

        assertEquals(9_876L, summary.steps)
        assertNull(summary.activeCaloriesKcal)
        assertNull(summary.totalCaloriesKcal)
        assertNull(summary.distanceMeters)
        assertNull(summary.sleepMinutes)
        assertNull(summary.exerciseMinutes)
        assertNull(summary.exerciseSessionCount)
        assertNull(summary.latestWeightKg)
        assertNull(summary.latestBodyFatPercent)
        assertEquals(55L, summary.restingHeartRateBpm)
        assertNull(summary.hrvRmssdMillis)
        assertEquals(1, factory.createCount)
        assertEquals(1, client.stepsCalls)
        assertEquals(0, client.activeCaloriesCalls)
        assertEquals(0, client.totalCaloriesCalls)
        assertEquals(0, client.distanceCalls)
        assertEquals(0, client.sleepCalls)
        assertEquals(0, client.exerciseDurationCalls)
        assertEquals(0, client.exerciseSessionCountCalls)
        assertEquals(0, client.latestWeightMetricCalls)
        assertEquals(0, client.latestBodyFatMetricCalls)
        assertEquals(1, client.restingHeartRateCalls)
        assertEquals(0, client.hrvCalls)
    }

    @Test
    fun readDailySummary_reportsLegitimateEmpty_whenPermissionGrantedButNoStepRecordsExist() = runTest {
        val client = FakeHealthConnectClientAdapter(steps = null)
        val factory = FakeClientFactory(client)
        val manager = managerWith(
            status = HealthConnectStatus(
                availability = HealthConnectAvailability.Available,
                grantedPermissions = setOf(readStepsPermission()),
            ),
            factory = factory,
        )

        val result = manager.readDailySummary(LocalDate.of(2026, 7, 3))

        assertEquals(HealthConnectDailyReadResult.Empty::class, result::class)
        assertNull(result.steps)
        assertEquals(1, client.stepsCalls)
    }

    @Test
    fun readDailySummary_filtersStepsToPreferredSource_insteadOfUnifiedTotal() = runTest {
        val client = FakeHealthConnectClientAdapter(
            steps = 9_800L,
            stepsByOrigin = mapOf(
                "com.google.android.apps.fitness" to 5_800L,
                "android" to 3_100L,
            ),
        )
        val factory = FakeClientFactory(client)
        val manager = managerWith(
            status = HealthConnectStatus(
                availability = HealthConnectAvailability.Available,
                grantedPermissions = setOf(readStepsPermission()),
            ),
            factory = factory,
        )

        val summary = manager.readDailySummary(
            LocalDate.of(2026, 6, 20),
            preferredStepsPackage = "com.google.android.apps.fitness",
        )

        assertEquals(5_800L, summary.steps)
        assertEquals(0, client.stepsCalls)
        assertEquals(1, client.stepsForOriginsCalls)
    }

    @Test
    fun readDailySummary_usesUnifiedSteps_whenNoPreferredSource() = runTest {
        val client = FakeHealthConnectClientAdapter(
            steps = 9_800L,
            stepsByOrigin = mapOf("com.google.android.apps.fitness" to 5_800L),
        )
        val factory = FakeClientFactory(client)
        val manager = managerWith(
            status = HealthConnectStatus(
                availability = HealthConnectAvailability.Available,
                grantedPermissions = setOf(readStepsPermission()),
            ),
            factory = factory,
        )

        val summary = manager.readDailySummary(LocalDate.of(2026, 6, 20), preferredStepsPackage = null)

        assertEquals(9_800L, summary.steps)
        assertEquals(1, client.stepsCalls)
        assertEquals(0, client.stepsForOriginsCalls)
    }

    @Test
    fun readDailySummaries_batchesSevenDaysIntoOneCallPerMetric() = runTest {
        val startDate = LocalDate.of(2026, 7, 6)
        val endDate = LocalDate.of(2026, 7, 12)
        val client = FakeHealthConnectClientAdapter().apply {
            batchedSteps = (0L..6L).associate { offset ->
                startDate.plusDays(offset) to (1_000L + offset)
            }
        }
        val manager = managerWith(
            status = HealthConnectStatus(
                availability = HealthConnectAvailability.Available,
                grantedPermissions = setOf(readStepsPermission()),
            ),
            factory = FakeClientFactory(client),
        )

        val results = manager.readDailySummaries(startDate, endDate)

        assertEquals((0L..6L).map { 1_000L + it }, results.values.map { it.steps })
        assertEquals(1, client.batchedStepsCalls)
        assertEquals(0, client.stepsCalls)
    }

    @Test
    fun readAllHealthConnectPages_consumesEveryContinuationToken() = runTest {
        val requestedTokens = mutableListOf<String?>()

        val records = readAllHealthConnectPages { pageToken ->
            requestedTokens += pageToken
            when (pageToken) {
                null -> HealthConnectRecordPage(records = (0 until 1_000).toList(), nextPageToken = "page-2")
                "page-2" -> HealthConnectRecordPage(records = (1_000 until 1_500).toList(), nextPageToken = null)
                else -> error("Unexpected page token: $pageToken")
            }
        }

        assertEquals(1_500, records.size)
        assertEquals(0, records.first())
        assertEquals(1_499, records.last())
        assertEquals(listOf(null, "page-2"), requestedTokens)
    }

    @Test
    fun healthConnectDateRange_preservesCalendarDaysAcrossDstTransition() {
        val range = HealthConnectDateRange(
            startDate = LocalDate.of(2026, 3, 28),
            endDateInclusive = LocalDate.of(2026, 3, 29),
            zoneId = ZoneId.of("Europe/Berlin"),
        )

        assertEquals(
            listOf(LocalDate.of(2026, 3, 28), LocalDate.of(2026, 3, 29)),
            range.dates,
        )
        assertEquals(47L, Duration.between(range.startTime, range.endTime).toHours())
    }

    @Test
    fun readStepSources_returnsPerOriginTotalsSortedDescending() = runTest {
        val client = FakeHealthConnectClientAdapter(
            stepsByOrigin = mapOf(
                "android" to 900L,
                "com.google.android.apps.fitness" to 5_800L,
            ),
        )
        val factory = FakeClientFactory(client)
        val manager = managerWith(
            status = HealthConnectStatus(
                availability = HealthConnectAvailability.Available,
                grantedPermissions = setOf(readStepsPermission()),
            ),
            factory = factory,
        )

        val sources = manager.readStepSources(LocalDate.of(2026, 6, 20))

        assertEquals(
            listOf("com.google.android.apps.fitness", "android"),
            sources.map { it.packageName },
        )
        assertEquals(5_800L, sources.first().steps)
        assertEquals("Your phone", sources.last().label)
        assertEquals(1, client.readStepCountsByOriginCalls)
    }

    @Test
    fun readStepSources_returnsEmpty_whenStepsPermissionMissing() = runTest {
        val client = FakeHealthConnectClientAdapter(stepsByOrigin = mapOf("android" to 900L))
        val factory = FakeClientFactory(client)
        val manager = managerWith(
            status = HealthConnectStatus(
                availability = HealthConnectAvailability.Available,
                grantedPermissions = emptySet(),
            ),
            factory = factory,
        )

        val sources = manager.readStepSources(LocalDate.of(2026, 6, 20))

        assertEquals(0, sources.size)
        assertEquals(0, client.readStepCountsByOriginCalls)
    }

    @Test
    fun requestablePermissions_includeCoreFitnessReadsAndUseRestingHeartRate_notGenericHeartRate() = runTest {
        val manager = managerWith(
            status = HealthConnectStatus(HealthConnectAvailability.NotInstalled, emptySet()),
            factory = FakeClientFactory(),
        )

        val permissions = manager.requestablePermissions()

        assertEquals(true, readStepsPermission() in permissions)
        assertEquals(true, readActiveCaloriesPermission() in permissions)
        assertEquals(true, readTotalCaloriesPermission() in permissions)
        assertEquals(true, readDistancePermission() in permissions)
        assertEquals(true, readSleepPermission() in permissions)
        assertEquals(true, readExercisePermission() in permissions)
        assertEquals(true, readWeightPermission() in permissions)
        assertEquals(true, readBodyFatPermission() in permissions)
        assertEquals(true, readRestingHeartRatePermission() in permissions)
        assertEquals(true, readHeartRateVariabilityPermission() in permissions)
        assertEquals(true, writeExercisePermission() in permissions)
        assertEquals(false, readHeartRatePermission() in permissions)
    }

    @Test
    fun readDailySummary_readsHeartRateVariability_whenPermissionGranted() = runTest {
        val client = FakeHealthConnectClientAdapter(hrvRmssdMillis = 64.0)
        val factory = FakeClientFactory(client)
        val manager = managerWith(
            status = HealthConnectStatus(
                availability = HealthConnectAvailability.Available,
                grantedPermissions = setOf(readHeartRateVariabilityPermission()),
            ),
            factory = factory,
        )

        val summary = manager.readDailySummary(LocalDate.of(2026, 7, 7))

        assertEquals(64.0, summary.hrvRmssdMillis ?: 0.0, 0.01)
        assertEquals(1, client.hrvCalls)
        assertEquals(0, client.restingHeartRateCalls)
    }

    @Test
    fun readDailySummary_readsAllGrantedFitnessMetricsAndBodyMetrics() = runTest {
        val client = FakeHealthConnectClientAdapter(
            steps = 12_345L,
            activeCaloriesKcal = 540.0,
            totalCaloriesKcal = 2_410.0,
            distanceMeters = 7_500.0,
            sleepMinutes = 462L,
            exerciseMinutes = 63L,
            exerciseSessionCount = 2,
            latestWeightKg = 80.4,
            latestBodyFatPercent = 17.8,
            restingHeartRateBpm = 52L,
            hrvRmssdMillis = 64.0,
        )
        val factory = FakeClientFactory(client)
        val manager = managerWith(
            status = HealthConnectStatus(
                availability = HealthConnectAvailability.Available,
                grantedPermissions = setOf(
                    readStepsPermission(),
                    readActiveCaloriesPermission(),
                    readTotalCaloriesPermission(),
                    readDistancePermission(),
                    readSleepPermission(),
                    readExercisePermission(),
                    readWeightPermission(),
                    readBodyFatPermission(),
                    readRestingHeartRatePermission(),
                    readHeartRateVariabilityPermission(),
                ),
            ),
            factory = factory,
        )

        val summary = manager.readDailySummary(LocalDate.of(2026, 6, 20))

        assertEquals(12_345L, summary.steps)
        assertEquals(540.0, summary.activeCaloriesKcal ?: 0.0, 0.01)
        assertEquals(2_410.0, summary.totalCaloriesKcal ?: 0.0, 0.01)
        assertEquals(7_500.0, summary.distanceMeters ?: 0.0, 0.01)
        assertEquals(462L, summary.sleepMinutes)
        assertEquals(63L, summary.exerciseMinutes)
        assertEquals(2, summary.exerciseSessionCount)
        assertEquals(80.4, summary.latestWeightKg ?: 0.0, 0.01)
        assertEquals(17.8, summary.latestBodyFatPercent ?: 0.0, 0.01)
        assertEquals(52L, summary.restingHeartRateBpm)
        assertEquals(64.0, summary.hrvRmssdMillis ?: 0.0, 0.01)
        assertEquals(
            listOf("weight", "body_fat"),
            summary.bodyMetrics.map { it.type },
        )
        assertEquals(1, factory.createCount)
        assertEquals(1, client.stepsCalls)
        assertEquals(1, client.activeCaloriesCalls)
        assertEquals(1, client.totalCaloriesCalls)
        assertEquals(1, client.distanceCalls)
        assertEquals(1, client.sleepCalls)
        assertEquals(1, client.exerciseDurationCalls)
        assertEquals(1, client.exerciseSessionCountCalls)
        assertEquals(1, client.latestWeightMetricCalls)
        assertEquals(1, client.latestBodyFatMetricCalls)
        assertEquals(1, client.restingHeartRateCalls)
        assertEquals(1, client.hrvCalls)
    }

    @Test
    fun readDailySummary_returnsPartialResultWhenOneGrantedMetricFails() = runTest {
        val client = FakeHealthConnectClientAdapter(
            steps = 8_500L,
            activeCaloriesFailure = IllegalStateException("provider read failed"),
        )
        val manager = managerWith(
            status = HealthConnectStatus(
                availability = HealthConnectAvailability.Available,
                grantedPermissions = setOf(readStepsPermission(), readActiveCaloriesPermission()),
            ),
            factory = FakeClientFactory(client),
        )

        val result = manager.readDailySummary(LocalDate.of(2026, 7, 14))
        val partial = result as HealthConnectDailyReadResult.Partial

        assertEquals(8_500L, partial.summary.steps)
        assertEquals(setOf(HealthConnectMetric.Steps), partial.completedMetrics)
        assertEquals(setOf(HealthConnectMetric.ActiveCalories), partial.failures.map { it.metric }.toSet())
    }

    @Test
    fun readDailySummary_returnsFailureWhenEveryGrantedMetricFails() = runTest {
        val client = FakeHealthConnectClientAdapter(
            stepsFailure = IllegalStateException("provider read failed"),
        )
        val manager = managerWith(
            status = HealthConnectStatus(
                availability = HealthConnectAvailability.Available,
                grantedPermissions = setOf(readStepsPermission()),
            ),
            factory = FakeClientFactory(client),
        )

        val result = manager.readDailySummary(LocalDate.of(2026, 7, 14))

        assertEquals(HealthConnectDailyReadResult.Failure::class, result::class)
        assertEquals("provider read failed", (result as HealthConnectDailyReadResult.Failure).message)
    }

    @Test
    fun readDailySummary_distinguishesRevokedPermissionsFromLegitimateEmpty() = runTest {
        val manager = managerWith(
            status = HealthConnectStatus(
                availability = HealthConnectAvailability.Available,
                grantedPermissions = emptySet(),
            ),
            factory = FakeClientFactory(),
        )

        val result = manager.readDailySummary(LocalDate.of(2026, 7, 14))
        val unavailable = result as HealthConnectDailyReadResult.Unavailable

        assertEquals(com.musfit.domain.health.HealthConnectUnavailableReason.PermissionsUnavailable, unavailable.reason)
    }

    @Test
    fun readDailySummary_rethrowsCancellationWithinOneSecond() = runTest {
        val client = FakeHealthConnectClientAdapter(
            stepsFailure = CancellationException("cancelled by caller"),
        )
        val manager = managerWith(
            status = HealthConnectStatus(
                availability = HealthConnectAvailability.Available,
                grantedPermissions = setOf(readStepsPermission()),
            ),
            factory = FakeClientFactory(client),
        )

        val startedAt = System.nanoTime()
        var cancellation: CancellationException? = null
        try {
            manager.readDailySummary(LocalDate.of(2026, 7, 14))
        } catch (caught: CancellationException) {
            cancellation = caught
        }

        assertEquals("cancelled by caller", cancellation?.message)
        assertEquals(true, (System.nanoTime() - startedAt) / 1_000_000 < 1_000L)
    }

    @Test
    fun exportWorkout_returnsNull_whenWritePermissionMissing() = runTest {
        val factory = FakeClientFactory()
        val manager = managerWith(
            status = HealthConnectStatus(
                availability = HealthConnectAvailability.Available,
                grantedPermissions = setOf(readStepsPermission()),
            ),
            factory = factory,
        )

        val exportedId = manager.exportWorkoutFixture(session = workoutSession(), sets = completedSets())

        assertNull(exportedId)
        assertEquals(0, factory.createCount)
    }

    @Test
    fun exportWorkout_returnsNull_whenHealthConnectUnavailable() = runTest {
        val factory = FakeClientFactory()
        val manager = managerWith(
            status = HealthConnectStatus(HealthConnectAvailability.NotSupported, emptySet()),
            factory = factory,
        )

        val exportedId = manager.exportWorkoutFixture(session = workoutSession(), sets = completedSets())

        assertNull(exportedId)
        assertEquals(0, factory.createCount)
    }

    @Test
    fun exportWorkout_insertsExerciseSession_andReturnsInsertedId_whenWritePermissionGranted() = runTest {
        val client = FakeHealthConnectClientAdapter(insertedRecordId = "health-connect-record-123")
        val factory = FakeClientFactory(client)
        val manager = managerWith(
            status = HealthConnectStatus(
                availability = HealthConnectAvailability.Available,
                grantedPermissions = setOf(writeExercisePermission()),
            ),
            factory = factory,
        )

        val exportedId = manager.exportWorkoutFixture(session = workoutSession(), sets = completedSets())

        assertEquals("health-connect-record-123", exportedId)
        assertEquals(1, factory.createCount)
        assertEquals(1, client.insertCalls)
    }

    @Test
    fun foodRequestablePermissions_includeNutritionAndHydrationWritePermissionsOnly() = runTest {
        val manager = managerWith(
            status = HealthConnectStatus(HealthConnectAvailability.NotInstalled, emptySet()),
            factory = FakeClientFactory(),
        )

        val permissions = manager.foodRequestablePermissions()

        assertEquals(true, writeNutritionPermission() in permissions)
        assertEquals(true, writeHydrationPermission() in permissions)
        assertEquals(false, writeExercisePermission() in permissions)
    }

    @Test
    fun exportFood_returnsNull_whenHealthConnectUnavailable() = runTest {
        val factory = FakeClientFactory()
        val manager = managerWith(
            status = HealthConnectStatus(HealthConnectAvailability.NotSupported, emptySet()),
            factory = factory,
        )

        val result = manager.exportFood(foodExportPayload())

        assertNull(result)
        assertEquals(0, factory.createCount)
    }

    @Test
    fun exportFood_writesNutritionAndHydrationRecords_whenPermissionsGranted() = runTest {
        val client = FakeHealthConnectClientAdapter()
        val factory = FakeClientFactory(client)
        val manager = managerWith(
            status = HealthConnectStatus(
                availability = HealthConnectAvailability.Available,
                grantedPermissions = setOf(writeNutritionPermission(), writeHydrationPermission()),
            ),
            factory = factory,
        )

        val result = manager.exportFood(foodExportPayload())

        assertEquals(2, result?.nutritionRecordCount)
        assertEquals(1, result?.hydrationRecordCount)
        assertEquals(setOf("breakfast", "snacks"), result?.nutritionProviderRecordIds?.keys)
        assertEquals("hydration-record-id", result?.hydrationProviderRecordId)
        assertEquals(1, factory.createCount)
        assertEquals(1, client.insertNutritionCalls)
        assertEquals(1, client.insertHydrationCalls)
        assertEquals(2, client.insertedNutritionRecords.size)
        assertEquals(
            2,
            client.insertedNutritionRecords.map { record -> record.metadata.clientRecordId }.distinct().size,
        )
        assertEquals(
            1L,
            client.insertedHydrationRecord?.metadata?.clientRecordVersion,
        )
    }

    @Test
    fun exportFood_hydrationOnlySkipsNutritionInsert() = runTest {
        val client = FakeHealthConnectClientAdapter()
        val factory = FakeClientFactory(client)
        val manager = managerWith(
            status = HealthConnectStatus(
                availability = HealthConnectAvailability.Available,
                grantedPermissions = setOf(writeHydrationPermission()),
            ),
            factory = factory,
        )

        val result = manager.exportFood(foodExportPayload().copy(meals = emptyList()))

        assertEquals(0, result?.nutritionRecordCount)
        assertEquals(1, result?.hydrationRecordCount)
        assertEquals(0, client.insertNutritionCalls)
        assertEquals(1, client.insertHydrationCalls)
    }

    @Test
    fun deleteAuthoredRecords_deletesStableClientIdsByRecordType() = runTest {
        val client = FakeHealthConnectClientAdapter()
        val manager = managerWith(
            status = HealthConnectStatus(
                availability = HealthConnectAvailability.Available,
                grantedPermissions = setOf(
                    writeExercisePermission(),
                    writeNutritionPermission(),
                    writeHydrationPermission(),
                ),
            ),
            factory = FakeClientFactory(client),
        )
        val records = setOf(
            HealthConnectAuthoredRecord(HealthConnectAuthoredRecordType.Workout, "workout-client"),
            HealthConnectAuthoredRecord(HealthConnectAuthoredRecordType.Nutrition, "nutrition-client-1"),
            HealthConnectAuthoredRecord(HealthConnectAuthoredRecordType.Nutrition, "nutrition-client-2"),
            HealthConnectAuthoredRecord(HealthConnectAuthoredRecordType.Hydration, "hydration-client"),
        )

        val result = manager.deleteAuthoredRecords(records)

        assertEquals(HealthConnectDeleteResult.Complete(records), result)
        assertEquals(listOf("workout-client"), client.deletedWorkoutClientIds)
        assertEquals(listOf("nutrition-client-1", "nutrition-client-2"), client.deletedNutritionClientIds)
        assertEquals(listOf("hydration-client"), client.deletedHydrationClientIds)
    }

    @Test
    fun deleteAuthoredRecords_reportsPartialWithoutCallingUnpermittedType() = runTest {
        val client = FakeHealthConnectClientAdapter()
        val manager = managerWith(
            status = HealthConnectStatus(
                availability = HealthConnectAvailability.Available,
                grantedPermissions = setOf(writeNutritionPermission()),
            ),
            factory = FakeClientFactory(client),
        )
        val nutrition = HealthConnectAuthoredRecord(
            HealthConnectAuthoredRecordType.Nutrition,
            "nutrition-client",
        )
        val workout = HealthConnectAuthoredRecord(
            HealthConnectAuthoredRecordType.Workout,
            "workout-client",
        )

        val result = manager.deleteAuthoredRecords(setOf(nutrition, workout)) as HealthConnectDeleteResult.Partial

        assertEquals(setOf(nutrition), result.deletedRecords)
        assertEquals(listOf(HealthConnectAuthoredRecordType.Workout), result.failures.map { it.type })
        assertEquals(emptyList<String>(), client.deletedWorkoutClientIds)
        assertEquals(listOf("nutrition-client"), client.deletedNutritionClientIds)
    }

    @Test
    fun deleteAuthoredRecords_returnsUnavailableWithoutCreatingClient() = runTest {
        val factory = FakeClientFactory()
        val manager = managerWith(
            status = HealthConnectStatus(HealthConnectAvailability.NotInstalled, emptySet()),
            factory = factory,
        )

        val result = manager.deleteAuthoredRecords(
            setOf(HealthConnectAuthoredRecord(HealthConnectAuthoredRecordType.Workout, "workout-client")),
        )

        assertEquals(HealthConnectDeleteResult.Unavailable::class, result::class)
        assertEquals(0, factory.createCount)
    }

    @Test
    fun deleteAuthoredRecords_emptyRequestIsANoOp() = runTest {
        val factory = FakeClientFactory()
        val manager = managerWith(
            status = HealthConnectStatus(HealthConnectAvailability.Available, emptySet()),
            factory = factory,
        )

        val result = manager.deleteAuthoredRecords(emptySet())

        assertEquals(HealthConnectDeleteResult.Complete(emptySet()), result)
        assertEquals(0, factory.createCount)
    }

    @Test
    fun deleteAuthoredRecords_returnsUnavailableWhenEveryWritePermissionIsMissing() = runTest {
        val factory = FakeClientFactory()
        val manager = managerWith(
            status = HealthConnectStatus(HealthConnectAvailability.Available, setOf(readStepsPermission())),
            factory = factory,
        )

        val result = manager.deleteAuthoredRecords(
            setOf(HealthConnectAuthoredRecord(HealthConnectAuthoredRecordType.Workout, "workout-client")),
        )

        assertEquals(HealthConnectDeleteResult.Unavailable::class, result::class)
        assertEquals(0, factory.createCount)
    }

    @Test
    fun deleteAuthoredRecords_keepsEntireTypePendingWhenProviderBatchFails() = runTest {
        val client = FakeHealthConnectClientAdapter().apply {
            nutritionDeleteFailure = IllegalStateException("provider delete failed")
        }
        val manager = managerWith(
            status = HealthConnectStatus(
                HealthConnectAvailability.Available,
                setOf(writeNutritionPermission()),
            ),
            factory = FakeClientFactory(client),
        )

        val result = manager.deleteAuthoredRecords(
            setOf(HealthConnectAuthoredRecord(HealthConnectAuthoredRecordType.Nutrition, "nutrition-client")),
        ) as HealthConnectDeleteResult.Failure

        assertEquals("provider delete failed", result.message)
        assertEquals(emptySet<HealthConnectAuthoredRecord>(), result.deletedRecords)
    }

    @Test
    fun deleteAuthoredRecords_rethrowsProviderCancellation() = runTest {
        val client = FakeHealthConnectClientAdapter().apply {
            hydrationDeleteFailure = CancellationException("cancelled by caller")
        }
        val manager = managerWith(
            status = HealthConnectStatus(
                HealthConnectAvailability.Available,
                setOf(writeHydrationPermission()),
            ),
            factory = FakeClientFactory(client),
        )

        var cancellation: CancellationException? = null
        try {
            manager.deleteAuthoredRecords(
                setOf(HealthConnectAuthoredRecord(HealthConnectAuthoredRecordType.Hydration, "hydration-client")),
            )
        } catch (caught: CancellationException) {
            cancellation = caught
        }

        assertEquals("cancelled by caller", cancellation?.message)
    }

    private fun managerWith(
        status: HealthConnectStatus,
        factory: FakeClientFactory,
    ): HealthConnectGateway = HealthConnectManager(
        context = context,
        statusReader = { status },
        clientFactory = { factory.create() },
    )

    private fun workoutSession() = WorkoutSessionEntity(
        accountId = "local-default",
        id = "session-1",
        routineId = null,
        startedAtEpochMillis = 1_700_000_000_000,
        endedAtEpochMillis = 1_700_003_600_000,
        notes = "Push day",
        healthConnectRecordId = null,
        healthConnectLastExportedAtEpochMillis = null,
    )

    private fun completedSets() = listOf(
        WorkoutSetEntity(
            accountId = "local-default",
            id = "set-1",
            sessionId = "session-1",
            exerciseId = "bench",
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

    private fun foodExportPayload() = HealthConnectFoodExportPayload(
        date = LocalDate.of(2026, 6, 20),
        meals = listOf(
            HealthConnectFoodMealExport(
                mealType = "breakfast",
                name = "Breakfast",
                caloriesKcal = 410.0,
                proteinGrams = 32.0,
                carbsGrams = 45.0,
                fatGrams = 12.0,
                fiberGrams = 8.0,
                sugarGrams = 12.0,
                saturatedFatGrams = 3.0,
                sodiumMilligrams = 450.0,
                potassiumMilligrams = 500.0,
                calciumMilligrams = 220.0,
                ironMilligrams = 4.0,
                vitaminDMicrograms = 2.5,
                vitaminCMilligrams = 40.0,
                magnesiumMilligrams = 90.0,
            ),
            HealthConnectFoodMealExport(
                mealType = "snacks",
                name = "Snacks",
                caloriesKcal = 180.0,
                proteinGrams = 6.0,
                carbsGrams = 25.0,
                fatGrams = 7.0,
            ),
        ),
        hydrationMilliliters = 750.0,
    )

    private fun readStepsPermission() = HealthPermission.getReadPermission(StepsRecord::class)

    private fun readActiveCaloriesPermission() = HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)

    private fun readTotalCaloriesPermission() = HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)

    private fun readDistancePermission() = HealthPermission.getReadPermission(DistanceRecord::class)

    private fun readSleepPermission() = HealthPermission.getReadPermission(SleepSessionRecord::class)

    private fun readExercisePermission() = HealthPermission.getReadPermission(ExerciseSessionRecord::class)

    private fun readWeightPermission() = HealthPermission.getReadPermission(WeightRecord::class)

    private fun readBodyFatPermission() = HealthPermission.getReadPermission(BodyFatRecord::class)

    private fun readHeartRatePermission() = HealthPermission.getReadPermission(HeartRateRecord::class)

    private fun readRestingHeartRatePermission() = HealthPermission.getReadPermission(RestingHeartRateRecord::class)

    private fun readHeartRateVariabilityPermission() = HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class)

    private fun writeExercisePermission() = HealthPermission.getWritePermission(ExerciseSessionRecord::class)

    private fun writeNutritionPermission() = HealthPermission.getWritePermission(NutritionRecord::class)

    private fun writeHydrationPermission() = HealthPermission.getWritePermission(HydrationRecord::class)

    private class FakeClientFactory(
        private val client: FakeHealthConnectClientAdapter = FakeHealthConnectClientAdapter(),
    ) {
        var createCount: Int = 0
            private set

        fun create(): HealthConnectClientAdapter {
            createCount += 1
            return client
        }
    }

    private class FakeHealthConnectClientAdapter(
        private val steps: Long? = null,
        private val stepsByOrigin: Map<String, Long> = emptyMap(),
        private val activeCaloriesKcal: Double? = null,
        private val totalCaloriesKcal: Double? = null,
        private val distanceMeters: Double? = null,
        private val sleepMinutes: Long? = null,
        private val exerciseMinutes: Long? = null,
        private val exerciseSessionCount: Int? = null,
        private val latestWeightKg: Double? = null,
        private val latestBodyFatPercent: Double? = null,
        private val restingHeartRateBpm: Long? = null,
        private val hrvRmssdMillis: Double? = null,
        private val insertedRecordId: String? = "exported-record-id",
        private val stepsFailure: Throwable? = null,
        private val activeCaloriesFailure: Throwable? = null,
    ) : HealthConnectClientAdapter {
        var stepsCalls: Int = 0
            private set
        var batchedSteps: Map<LocalDate, Long?>? = null
        var batchedStepsCalls: Int = 0
            private set
        var stepsForOriginsCalls: Int = 0
            private set
        var readStepCountsByOriginCalls: Int = 0
            private set
        var activeCaloriesCalls: Int = 0
            private set
        var totalCaloriesCalls: Int = 0
            private set
        var distanceCalls: Int = 0
            private set
        var sleepCalls: Int = 0
            private set
        var exerciseDurationCalls: Int = 0
            private set
        var exerciseSessionCountCalls: Int = 0
            private set
        var latestWeightMetricCalls: Int = 0
            private set
        var latestBodyFatMetricCalls: Int = 0
            private set
        var restingHeartRateCalls: Int = 0
            private set
        var hrvCalls: Int = 0
            private set
        var insertCalls: Int = 0
            private set
        var insertNutritionCalls: Int = 0
            private set
        var insertHydrationCalls: Int = 0
            private set
        var insertedNutritionRecords: List<NutritionRecord> = emptyList()
            private set
        var insertedHydrationRecord: HydrationRecord? = null
            private set
        var deletedWorkoutClientIds: List<String> = emptyList()
            private set
        var deletedNutritionClientIds: List<String> = emptyList()
            private set
        var deletedHydrationClientIds: List<String> = emptyList()
            private set
        var workoutDeleteFailure: Throwable? = null
        var nutritionDeleteFailure: Throwable? = null
        var hydrationDeleteFailure: Throwable? = null

        override suspend fun aggregateSteps(range: HealthConnectTimeRange): Long? {
            stepsCalls += 1
            stepsFailure?.let { throw it }
            return steps
        }

        override suspend fun aggregateStepsByDay(range: HealthConnectDateRange): Map<LocalDate, Long?> {
            batchedStepsCalls += 1
            return batchedSteps ?: super.aggregateStepsByDay(range)
        }

        override suspend fun aggregateStepsForOrigins(
            range: HealthConnectTimeRange,
            packageNames: Set<String>,
        ): Long? {
            stepsForOriginsCalls += 1
            val matching = stepsByOrigin.filterKeys { it in packageNames }
            return if (matching.isEmpty()) null else matching.values.sum()
        }

        override suspend fun readStepCountsByOrigin(range: HealthConnectTimeRange): Map<String, Long> {
            readStepCountsByOriginCalls += 1
            return stepsByOrigin
        }

        override suspend fun aggregateActiveCalories(range: HealthConnectTimeRange): Double? {
            activeCaloriesCalls += 1
            activeCaloriesFailure?.let { throw it }
            return activeCaloriesKcal
        }

        override suspend fun aggregateTotalCalories(range: HealthConnectTimeRange): Double? {
            totalCaloriesCalls += 1
            return totalCaloriesKcal
        }

        override suspend fun aggregateDistanceMeters(range: HealthConnectTimeRange): Double? {
            distanceCalls += 1
            return distanceMeters
        }

        override suspend fun aggregateSleepMinutes(range: HealthConnectTimeRange): Long? {
            sleepCalls += 1
            return sleepMinutes
        }

        override suspend fun aggregateExerciseMinutes(range: HealthConnectTimeRange): Long? {
            exerciseDurationCalls += 1
            return exerciseMinutes
        }

        override suspend fun readExerciseSessionCount(range: HealthConnectTimeRange): Int? {
            exerciseSessionCountCalls += 1
            return exerciseSessionCount
        }

        override suspend fun readLatestWeightMetric(range: HealthConnectTimeRange): ImportedBodyMetric? {
            latestWeightMetricCalls += 1
            return latestWeightKg?.let {
                ImportedBodyMetric(
                    type = "weight",
                    value = it,
                    unit = "kg",
                    measuredAtEpochMillis = 1_700_000_000_000L,
                    externalId = "weight-record",
                )
            }
        }

        override suspend fun readLatestBodyFatMetric(range: HealthConnectTimeRange): ImportedBodyMetric? {
            latestBodyFatMetricCalls += 1
            return latestBodyFatPercent?.let {
                ImportedBodyMetric(
                    type = "body_fat",
                    value = it,
                    unit = "%",
                    measuredAtEpochMillis = 1_700_000_100_000L,
                    externalId = "body-fat-record",
                )
            }
        }

        override suspend fun readLatestRestingHeartRate(range: HealthConnectTimeRange): Long? {
            restingHeartRateCalls += 1
            return restingHeartRateBpm
        }

        override suspend fun readLatestHeartRateVariabilityRmssdMillis(range: HealthConnectTimeRange): Double? {
            hrvCalls += 1
            return hrvRmssdMillis
        }

        override suspend fun insertExerciseSession(record: ExerciseSessionRecord): String? {
            insertCalls += 1
            return insertedRecordId
        }

        override suspend fun insertNutritionRecords(records: List<NutritionRecord>): List<String> {
            insertNutritionCalls += 1
            insertedNutritionRecords = records
            return records.indices.map { index -> "nutrition-record-$index" }
        }

        override suspend fun insertHydrationRecord(record: HydrationRecord): String? {
            insertHydrationCalls += 1
            insertedHydrationRecord = record
            return "hydration-record-id"
        }

        override suspend fun deleteExerciseSessions(clientRecordIds: List<String>) {
            workoutDeleteFailure?.let { throw it }
            deletedWorkoutClientIds = clientRecordIds
        }

        override suspend fun deleteNutritionRecords(clientRecordIds: List<String>) {
            nutritionDeleteFailure?.let { throw it }
            deletedNutritionClientIds = clientRecordIds
        }

        override suspend fun deleteHydrationRecords(clientRecordIds: List<String>) {
            hydrationDeleteFailure?.let { throw it }
            deletedHydrationClientIds = clientRecordIds
        }
    }
}
