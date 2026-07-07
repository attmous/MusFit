package com.musfit.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.musfit.data.local.MusFitDatabase
import com.musfit.data.local.entity.DailyHealthSummaryEntity
import com.musfit.data.local.entity.ExerciseEntity
import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity
import com.musfit.domain.health.HealthConnectAvailability
import com.musfit.domain.health.HealthConnectStatus
import com.musfit.domain.health.ImportedBodyMetric
import com.musfit.domain.health.ImportedDailyHealthSummary
import com.musfit.domain.health.StepSource
import com.musfit.integrations.healthconnect.HealthConnectFoodExportPayload
import com.musfit.integrations.healthconnect.HealthConnectFoodExportResult
import com.musfit.integrations.healthconnect.HealthConnectGateway
import kotlinx.coroutines.flow.first
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

@RunWith(RobolectricTestRunner::class)
class LocalHealthRepositoryTest {
    private lateinit var database: MusFitDatabase
    private lateinit var gateway: FakeHealthConnectGateway
    private lateinit var repository: LocalHealthRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room.inMemoryDatabaseBuilder(context, MusFitDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        gateway = FakeHealthConnectGateway()
        repository = LocalHealthRepository(
            gateway = gateway,
            healthDao = database.healthDao(),
            trainingDao = database.trainingDao(),
            clock = { 1_000L },
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun importDailySummary_persistsSummaryAndSyncState() = runTest {
        val date = LocalDate.of(2026, 6, 20)

        repository.importDailySummary(date)

        val summary = database.healthDao().observeDailySummary(date.toEpochDay()).first()
        val syncState = database.healthDao().observeHealthConnectSyncState().first()

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
        val weights = database.healthDao().getBodyMetrics("weight", 0L)
        val bodyFat = database.healthDao().getBodyMetrics("body_fat", 0L)
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
    fun importDailySummary_preservesExistingValuesWhenImportHasNoValue() = runTest {
        val date = LocalDate.of(2026, 6, 20)
        database.healthDao().upsertDailySummary(
            DailyHealthSummaryEntity(
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

        repository.importDailySummary(date)

        val summary = database.healthDao().observeDailySummary(date.toEpochDay()).first()
        assertEquals(7_800L, summary?.steps)
        assertEquals(360.0, summary?.activeCaloriesKcal ?: 0.0, 0.01)
        assertEquals(2_150.0, summary?.totalCaloriesKcal ?: 0.0, 0.01)
        assertEquals(5_200.0, summary?.distanceMeters ?: 0.0, 0.01)
        assertEquals(430L, summary?.sleepMinutes)
        assertEquals(35L, summary?.exerciseMinutes)
        assertEquals(1, summary?.exerciseSessionCount)
        assertEquals(80.9, summary?.latestWeightKg ?: 0.0, 0.01)
        assertEquals(14.8, summary?.latestBodyFatPercent ?: 0.0, 0.01)
        assertEquals(58L, summary?.restingHeartRateBpm)
        assertEquals(66.0, summary?.hrvRmssdMillis ?: 0.0, 0.01)
        assertEquals(1_000L, summary?.updatedAtEpochMillis)
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
        assertEquals(7, result.importedDayCount)
        assertEquals(14, result.bodyMetricCount)
        assertEquals(7, database.healthDao().observeDailySummariesInRange(anchor.minusDays(6).toEpochDay(), anchor.toEpochDay()).first().size)
    }

    @Test
    fun exportLatestWorkout_exportsPersistedWorkoutAndMarksSession() = runTest {
        database.trainingDao().upsertExercise(
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

        val savedSession = database.trainingDao().getWorkoutSession("session-1")
        val syncState = database.healthDao().observeHealthConnectSyncState().first()

        assertEquals("record-id", recordId)
        assertEquals("session-1", gateway.exportedSession?.id)
        assertEquals(1, gateway.exportedSets.size)
        assertEquals("record-id", savedSession?.healthConnectRecordId)
        assertEquals(1_000L, savedSession?.healthConnectLastExportedAtEpochMillis)
        assertNotNull(syncState?.lastExportAtEpochMillis)
    }

    @Test
    fun exportLatestWorkout_preservesExportedWorkoutSetsWhenMetadataIsSaved() = runTest {
        database.trainingDao().upsertExercise(
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

        val savedSets = database.trainingDao().getWorkoutSets("session-1")

        assertEquals(listOf("set-1", "set-2"), savedSets.map { it.id })
    }

    @Test
    fun exportLatestWorkout_skipsActiveSessionAndExportsLatestCompletedWorkout() = runTest {
        database.trainingDao().upsertExercise(
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
        assertEquals("session-completed", gateway.exportedSession?.id)
        assertEquals(listOf("set-completed"), gateway.exportedSets.map { it.id })
        assertNull(database.trainingDao().getWorkoutSession("session-active")?.healthConnectRecordId)
    }

    @Test
    fun exportLatestWorkout_filtersOutIncompleteSetsFromCompletedSession() = runTest {
        database.trainingDao().upsertExercise(
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

        assertEquals(listOf("set-completed"), gateway.exportedSets.map { it.id })
    }

    @Test
    fun exportLatestWorkout_fallsBackWhenNewestCompletedWorkoutHasNoCompletedSets() = runTest {
        database.trainingDao().upsertExercise(
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
        assertEquals("session-older-completed", gateway.exportedSession?.id)
        assertEquals(listOf("set-older-completed"), gateway.exportedSets.map { it.id })
        assertNull(database.trainingDao().getWorkoutSession("session-newer-empty")?.healthConnectRecordId)
    }

    private class FakeHealthConnectGateway : HealthConnectGateway {
        var exportedSession: WorkoutSessionEntity? = null
        var exportedSets: List<WorkoutSetEntity> = emptyList()
        val importedDates = mutableListOf<LocalDate>()
        val preferredStepsPackages = mutableListOf<String?>()
        var stepSources: List<StepSource> = emptyList()
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

        override suspend fun status(): HealthConnectStatus =
            HealthConnectStatus(
                availability = HealthConnectAvailability.Available,
                grantedPermissions = setOf("steps"),
            )

        override suspend fun requestablePermissions(): Set<String> = setOf("steps")

        override suspend fun foodRequestablePermissions(): Set<String> = emptySet()

        override suspend fun readStepSources(date: LocalDate): List<StepSource> = stepSources

        override suspend fun readDailySummary(
            date: LocalDate,
            preferredStepsPackage: String?,
        ): ImportedDailyHealthSummary {
            importedDates += date
            preferredStepsPackages += preferredStepsPackage
            return dailySummaryFactory(date)
        }

        override suspend fun exportWorkout(
            session: WorkoutSessionEntity,
            sets: List<WorkoutSetEntity>,
        ): String {
            exportedSession = session
            exportedSets = sets
            return "record-id"
        }

        override suspend fun exportFood(payload: HealthConnectFoodExportPayload): HealthConnectFoodExportResult? =
            null
    }
}
