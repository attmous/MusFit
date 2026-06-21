package com.musfit.integrations.healthconnect

import android.content.Context
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.test.core.app.ApplicationProvider
import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity
import com.musfit.domain.health.HealthConnectAvailability
import com.musfit.domain.health.HealthConnectStatus
import com.musfit.domain.health.ImportedDailyHealthSummary
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class HealthConnectManagerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun readDailySummary_returnsEmptySummary_whenHealthConnectUnavailable() = runTest {
        val factory = FakeClientFactory()
        val manager = managerWith(
            status = HealthConnectStatus(HealthConnectAvailability.NotInstalled, emptySet()),
            factory = factory,
        )

        val summary = manager.readDailySummary(LocalDate.of(2026, 6, 20))

        assertEquals(
            ImportedDailyHealthSummary(
                steps = null,
                activeCaloriesKcal = null,
                latestWeightKg = null,
                restingHeartRateBpm = null,
            ),
            summary,
        )
        assertEquals(0, factory.createCount)
    }

    @Test
    fun readDailySummary_readsOnlyGrantedMetrics() = runTest {
        val client = FakeHealthConnectClientAdapter(
            steps = 9_876L,
            activeCaloriesKcal = 321.5,
            latestWeightKg = 81.2,
            restingHeartRateBpm = 55L,
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
        assertNull(summary.latestWeightKg)
        assertEquals(55L, summary.restingHeartRateBpm)
        assertEquals(1, factory.createCount)
        assertEquals(1, client.stepsCalls)
        assertEquals(0, client.activeCaloriesCalls)
        assertEquals(0, client.latestWeightCalls)
        assertEquals(1, client.restingHeartRateCalls)
    }

    @Test
    fun requestablePermissions_usesRestingHeartRateReadPermission_notGenericHeartRate() = runTest {
        val manager = managerWith(
            status = HealthConnectStatus(HealthConnectAvailability.NotInstalled, emptySet()),
            factory = FakeClientFactory(),
        )

        val permissions = manager.requestablePermissions()

        assertEquals(true, readRestingHeartRatePermission() in permissions)
        assertEquals(false, readHeartRatePermission() in permissions)
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

        val exportedId = manager.exportWorkout(session = workoutSession(), sets = completedSets())

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

        val exportedId = manager.exportWorkout(session = workoutSession(), sets = completedSets())

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

        val exportedId = manager.exportWorkout(session = workoutSession(), sets = completedSets())

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

        assertEquals(HealthConnectFoodExportResult(nutritionRecordCount = 2, hydrationRecordCount = 1), result)
        assertEquals(1, factory.createCount)
        assertEquals(1, client.insertNutritionCalls)
        assertEquals(1, client.insertHydrationCalls)
        assertEquals(2, client.insertedNutritionRecords.size)
    }

    private fun managerWith(
        status: HealthConnectStatus,
        factory: FakeClientFactory,
    ) = HealthConnectManager(
        context = context,
        statusReader = { status },
        clientFactory = { factory.create() },
    )

    private fun workoutSession() = WorkoutSessionEntity(
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

    private fun foodExportPayload() =
        HealthConnectFoodExportPayload(
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

    private fun readHeartRatePermission() = HealthPermission.getReadPermission(HeartRateRecord::class)

    private fun readRestingHeartRatePermission() =
        HealthPermission.getReadPermission(RestingHeartRateRecord::class)

    private fun writeExercisePermission() =
        HealthPermission.getWritePermission(ExerciseSessionRecord::class)

    private fun writeNutritionPermission() =
        HealthPermission.getWritePermission(NutritionRecord::class)

    private fun writeHydrationPermission() =
        HealthPermission.getWritePermission(HydrationRecord::class)

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
        private val activeCaloriesKcal: Double? = null,
        private val latestWeightKg: Double? = null,
        private val restingHeartRateBpm: Long? = null,
        private val insertedRecordId: String? = "exported-record-id",
    ) : HealthConnectClientAdapter {
        var stepsCalls: Int = 0
            private set
        var activeCaloriesCalls: Int = 0
            private set
        var latestWeightCalls: Int = 0
            private set
        var restingHeartRateCalls: Int = 0
            private set
        var insertCalls: Int = 0
            private set
        var insertNutritionCalls: Int = 0
            private set
        var insertHydrationCalls: Int = 0
            private set
        var insertedNutritionRecords: List<NutritionRecord> = emptyList()
            private set

        override suspend fun aggregateSteps(range: HealthConnectTimeRange): Long? {
            stepsCalls += 1
            return steps
        }

        override suspend fun aggregateActiveCalories(range: HealthConnectTimeRange): Double? {
            activeCaloriesCalls += 1
            return activeCaloriesKcal
        }

        override suspend fun readLatestWeight(range: HealthConnectTimeRange): Double? {
            latestWeightCalls += 1
            return latestWeightKg
        }

        override suspend fun readLatestRestingHeartRate(range: HealthConnectTimeRange): Long? {
            restingHeartRateCalls += 1
            return restingHeartRateBpm
        }

        override suspend fun insertExerciseSession(record: ExerciseSessionRecord): String? {
            insertCalls += 1
            return insertedRecordId
        }

        override suspend fun insertNutritionRecords(records: List<NutritionRecord>): Int {
            insertNutritionCalls += 1
            insertedNutritionRecords = records
            return records.size
        }

        override suspend fun insertHydrationRecord(record: HydrationRecord): String? {
            insertHydrationCalls += 1
            return "hydration-record-id"
        }
    }
}
