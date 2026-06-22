package com.musfit.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.musfit.data.local.MusFitDatabase
import com.musfit.data.local.entity.ExerciseEntity
import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity
import com.musfit.domain.health.HealthConnectAvailability
import com.musfit.domain.health.HealthConnectStatus
import com.musfit.domain.health.ImportedDailyHealthSummary
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
        assertEquals(82.5, summary?.latestWeightKg ?: 0.0, 0.01)
        assertEquals(58L, summary?.restingHeartRateBpm)
        assertEquals(1_000L, summary?.updatedAtEpochMillis)
        assertEquals(1_000L, syncState?.lastImportAtEpochMillis)
        assertEquals(true, syncState?.isAvailable)
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

    private class FakeHealthConnectGateway : HealthConnectGateway {
        var exportedSession: WorkoutSessionEntity? = null
        var exportedSets: List<WorkoutSetEntity> = emptyList()

        override suspend fun status(): HealthConnectStatus =
            HealthConnectStatus(
                availability = HealthConnectAvailability.Available,
                grantedPermissions = setOf("steps"),
            )

        override suspend fun requestablePermissions(): Set<String> = setOf("steps")

        override suspend fun foodRequestablePermissions(): Set<String> = emptySet()

        override suspend fun readDailySummary(date: LocalDate): ImportedDailyHealthSummary =
            ImportedDailyHealthSummary(
                steps = 1234L,
                activeCaloriesKcal = 250.0,
                latestWeightKg = 82.5,
                restingHeartRateBpm = 58L,
            )

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
