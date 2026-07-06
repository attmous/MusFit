package com.musfit.integrations.healthconnect

import android.content.Context
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.test.core.app.ApplicationProvider
import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity
import com.musfit.domain.health.HealthConnectAvailability
import com.musfit.domain.health.HealthConnectStatus
import com.musfit.domain.health.ImportedBodyMetric
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
                totalCaloriesKcal = null,
                distanceMeters = null,
                sleepMinutes = null,
                exerciseMinutes = null,
                exerciseSessionCount = null,
                latestWeightKg = null,
                latestBodyFatPercent = null,
                restingHeartRateBpm = null,
                bodyMetrics = emptyList(),
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
            totalCaloriesKcal = 2_350.0,
            distanceMeters = 6_200.0,
            sleepMinutes = 435L,
            exerciseMinutes = 55L,
            exerciseSessionCount = 2,
            latestBodyFatPercent = 18.4,
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
        assertNull(summary.totalCaloriesKcal)
        assertNull(summary.distanceMeters)
        assertNull(summary.sleepMinutes)
        assertNull(summary.exerciseMinutes)
        assertNull(summary.exerciseSessionCount)
        assertNull(summary.latestWeightKg)
        assertNull(summary.latestBodyFatPercent)
        assertEquals(55L, summary.restingHeartRateBpm)
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
    }

    @Test
    fun readDailySummary_reportsZeroSteps_whenPermissionGrantedButNoStepRecordsExist() = runTest {
        val client = FakeHealthConnectClientAdapter(steps = null)
        val factory = FakeClientFactory(client)
        val manager = managerWith(
            status = HealthConnectStatus(
                availability = HealthConnectAvailability.Available,
                grantedPermissions = setOf(readStepsPermission()),
            ),
            factory = factory,
        )

        val summary = manager.readDailySummary(LocalDate.of(2026, 7, 3))

        assertEquals(0L, summary.steps)
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
        assertEquals(true, writeExercisePermission() in permissions)
        assertEquals(false, readHeartRatePermission() in permissions)
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
        assertEquals(
            listOf(
                "musfit-food-nutrition-2026-06-20-breakfast",
                "musfit-food-nutrition-2026-06-20-snacks",
            ),
            client.insertedNutritionRecords.map { record -> record.metadata.clientRecordId },
        )
        assertEquals(
            "musfit-food-hydration-2026-06-20",
            client.insertedHydrationRecord?.metadata?.clientRecordId,
        )
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

    private fun readActiveCaloriesPermission() =
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)

    private fun readTotalCaloriesPermission() =
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)

    private fun readDistancePermission() = HealthPermission.getReadPermission(DistanceRecord::class)

    private fun readSleepPermission() = HealthPermission.getReadPermission(SleepSessionRecord::class)

    private fun readExercisePermission() = HealthPermission.getReadPermission(ExerciseSessionRecord::class)

    private fun readWeightPermission() = HealthPermission.getReadPermission(WeightRecord::class)

    private fun readBodyFatPermission() = HealthPermission.getReadPermission(BodyFatRecord::class)

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
        private val insertedRecordId: String? = "exported-record-id",
    ) : HealthConnectClientAdapter {
        var stepsCalls: Int = 0
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

        override suspend fun aggregateSteps(range: HealthConnectTimeRange): Long? {
            stepsCalls += 1
            return steps
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
            insertedHydrationRecord = record
            return "hydration-record-id"
        }
    }
}
