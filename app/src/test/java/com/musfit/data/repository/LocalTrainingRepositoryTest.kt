package com.musfit.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.musfit.data.local.MusFitDatabase
import com.musfit.data.local.entity.ExerciseEntity
import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class LocalTrainingRepositoryTest {
    private lateinit var database: MusFitDatabase
    private lateinit var repository: LocalTrainingRepository
    private var currentInstant: Instant = WORKOUT_START

    @Before
    fun setUp() {
        currentInstant = WORKOUT_START
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room.inMemoryDatabaseBuilder(context, MusFitDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository = LocalTrainingRepository(
            database = database,
            trainingDao = database.trainingDao(),
            clock = { currentInstant.toEpochMilli() },
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun addCompletedSet_persistsExerciseSessionSetAndDailySummary() = runTest {
        val savedSet = repository.addCompletedSet(
            exerciseName = "Bench Press",
            reps = 5,
            weightKg = 100.0,
        )

        val sessions = database.trainingDao().observeWorkoutSessions().first()
        val sets = database.trainingDao().getWorkoutSets(sessions.single().id)
        val summary = repository.observeDailyTrainingSummary(WORKOUT_DATE).first()

        assertEquals("Bench Press", savedSet.exerciseName)
        assertEquals(5, savedSet.reps)
        assertEquals(100.0, savedSet.weightKg, 0.01)
        assertEquals(true, savedSet.completed)
        assertEquals(sessions.single().id, sets.single().sessionId)
        assertEquals(1, summary.completedSetCount)
        assertEquals(500.0, summary.totalVolumeKg, 0.01)
        assertEquals(116.67, summary.bestEstimatedOneRepMaxKg, 0.01)
    }

    @Test
    fun setCompletion_updatesPersistedSummary() = runTest {
        val savedSet = repository.addCompletedSet(
            exerciseName = "Squat",
            reps = 3,
            weightKg = 120.0,
        )

        repository.setCompletion(savedSet.id, completed = false)

        val summary = repository.observeDailyTrainingSummary(WORKOUT_DATE).first()
        val sessions = database.trainingDao().observeWorkoutSessions().first()
        val sets = database.trainingDao().getWorkoutSets(sessions.single().id)

        assertEquals(false, sets.single().completed)
        assertEquals(0, summary.completedSetCount)
        assertEquals(0.0, summary.totalVolumeKg, 0.01)
    }

    @Test
    fun getLatestWorkoutForExport_returnsNullWhenOnlyActiveWorkoutExists() = runTest {
        repository.addCompletedSet(
            exerciseName = "Deadlift",
            reps = 2,
            weightKg = 160.0,
        )

        val workout = repository.getLatestWorkoutForExport()

        assertEquals(null, workout)
    }

    @Test
    fun addCompletedSet_afterDateChanges_startsNewSessionForNewDaySummary() = runTest {
        repository.addCompletedSet(
            exerciseName = "Bench Press",
            reps = 5,
            weightKg = 100.0,
        )

        val nextDate = WORKOUT_DATE.plusDays(1)
        currentInstant = nextDate
            .atTime(9, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()

        repository.addCompletedSet(
            exerciseName = "Squat",
            reps = 3,
            weightKg = 120.0,
        )

        val sessions = database.trainingDao().observeWorkoutSessions().first()
        val firstDaySummary = repository.observeDailyTrainingSummary(WORKOUT_DATE).first()
        val nextDaySummary = repository.observeDailyTrainingSummary(nextDate).first()

        assertEquals(2, sessions.size)
        assertEquals(1, firstDaySummary.completedSetCount)
        assertEquals(500.0, firstDaySummary.totalVolumeKg, 0.01)
        assertEquals(1, nextDaySummary.completedSetCount)
        assertEquals(360.0, nextDaySummary.totalVolumeKg, 0.01)
    }

    @Test
    fun seedStarterTrainingData_importsExercisesAndRoutinesOnce() = runTest {
        repository.seedStarterTrainingData()
        repository.seedStarterTrainingData()

        val exercises = repository.observeExercises().first()
        val routines = repository.observeRoutineSummaries().first()

        assertTrue(exercises.any { it.name == "Barbell Bench Press" && !it.isCustom })
        assertTrue(exercises.any { it.name == "Back Squat" && it.equipment == "barbell" })
        assertTrue(routines.any { it.name == "Full Body A" && it.isStarter })
        assertTrue(routines.any { it.name == "Push" && it.exerciseCount >= 4 })
        assertEquals(exercises.map { it.id }.distinct().size, exercises.size)
        assertEquals(routines.map { it.id }.distinct().size, routines.size)
    }

    @Test
    fun observeExercises_filtersBySearchMuscleAndEquipment() = runTest {
        repository.seedStarterTrainingData()

        val bench = repository.observeExercises(query = "bench").first()
        val quads = repository.observeExercises(muscle = "quads").first()
        val dumbbell = repository.observeExercises(equipment = "dumbbell").first()

        assertTrue(bench.all { it.name.contains("Bench", ignoreCase = true) })
        assertTrue(quads.any { it.name == "Back Squat" })
        assertTrue(dumbbell.any { it.name == "Incline Dumbbell Press" })
    }

    @Test
    fun seedStarterTrainingData_whenCustomStarterExerciseExists_seedsRemainingCatalogWithoutDuplicates() = runTest {
        val customBenchPress =
            ExerciseEntity(
                id = "custom-bench-press",
                name = "Barbell Bench Press",
                category = "strength",
                equipment = null,
                targetMuscles = "chest",
                isCustom = true,
            )
        database.trainingDao().upsertExercise(customBenchPress)

        repository.seedStarterTrainingData()

        val exercises = repository.observeExercises().first()
        val routines = repository.observeRoutineSummaries().first()
        val pushRoutineExercises = database.trainingDao().getRoutineExercises("starter-routine-push")
        val matchingBenchPressExercises = exercises.filter { it.name == "Barbell Bench Press" }

        assertEquals(TrainingStarterData.exercises.map { it.name }.sorted(), exercises.map { it.name }.sorted())
        assertEquals(1, matchingBenchPressExercises.size)
        assertTrue(matchingBenchPressExercises.single().isCustom)
        assertEquals(customBenchPress.id, matchingBenchPressExercises.single().id)
        assertEquals(TrainingStarterData.routines.map { it.id }.sorted(), routines.map { it.id }.sorted())
        assertEquals(
            TrainingStarterData.routines.first { it.id == "starter-routine-push" }.exercises.size,
            pushRoutineExercises.size,
        )
        assertTrue(pushRoutineExercises.any { it.exerciseId == customBenchPress.id })
    }

    @Test
    fun getLatestWorkoutForExport_skipsActiveSessionAndReturnsLatestCompletedWorkout() = runTest {
        val exercise =
            ExerciseEntity(
                id = "exercise-deadlift",
                name = "Deadlift",
                category = "strength",
                equipment = "barbell",
                targetMuscles = "back,glutes,hamstrings",
                isCustom = false,
            )
        val completedSession =
            WorkoutSessionEntity(
                id = "session-completed",
                routineId = null,
                title = "Completed workout",
                status = "completed",
                startedAtEpochMillis = WORKOUT_START.toEpochMilli(),
                endedAtEpochMillis = WORKOUT_START.plusSeconds(1800).toEpochMilli(),
                notes = null,
                healthConnectRecordId = null,
                healthConnectLastExportedAtEpochMillis = null,
            )
        val activeSession =
            WorkoutSessionEntity(
                id = "session-active",
                routineId = null,
                title = "Active workout",
                status = "active",
                startedAtEpochMillis = WORKOUT_START.plusSeconds(3600).toEpochMilli(),
                endedAtEpochMillis = null,
                notes = null,
                healthConnectRecordId = null,
                healthConnectLastExportedAtEpochMillis = null,
            )

        database.trainingDao().upsertExercise(exercise)
        database.trainingDao().upsertWorkoutSession(completedSession)
        database.trainingDao().upsertWorkoutSession(activeSession)
        database.trainingDao().upsertWorkoutSet(
            WorkoutSetEntity(
                id = "set-completed",
                sessionId = completedSession.id,
                exerciseId = exercise.id,
                sortOrder = 0,
                setType = "working",
                reps = 2,
                weightKg = 160.0,
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
                sessionId = activeSession.id,
                exerciseId = exercise.id,
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

        val workout = repository.getLatestWorkoutForExport()

        assertNotNull(workout)
        assertEquals(completedSession.id, workout?.session?.id)
        assertEquals("completed", workout?.session?.status)
        assertEquals(listOf("set-completed"), workout?.sets?.map { it.id })
    }

    private companion object {
        val WORKOUT_DATE: LocalDate = LocalDate.of(2026, 6, 20)
        val WORKOUT_START: Instant = WORKOUT_DATE
            .atTime(10, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
    }
}
