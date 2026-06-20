package com.musfit.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.musfit.data.local.MusFitDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room.inMemoryDatabaseBuilder(context, MusFitDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository = LocalTrainingRepository(
            database = database,
            trainingDao = database.trainingDao(),
            clock = { WORKOUT_START.toEpochMilli() },
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
    fun getLatestWorkoutForExport_returnsPersistedSessionWithSets() = runTest {
        repository.addCompletedSet(
            exerciseName = "Deadlift",
            reps = 2,
            weightKg = 160.0,
        )

        val workout = repository.getLatestWorkoutForExport()

        assertNotNull(workout)
        assertEquals(1, workout?.sets?.size)
        assertEquals(2, workout?.sets?.single()?.reps)
    }

    private companion object {
        val WORKOUT_DATE: LocalDate = LocalDate.of(2026, 6, 20)
        val WORKOUT_START: Instant = WORKOUT_DATE
            .atTime(10, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
    }
}
