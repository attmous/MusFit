package com.musfit.ui.training

import com.musfit.data.repository.LoggedWorkoutSet
import com.musfit.data.repository.RoutineDetail
import com.musfit.data.repository.RoutineInput
import com.musfit.data.repository.TrainingRepository
import com.musfit.data.repository.TrainingSummary
import com.musfit.data.repository.WorkoutForExport
import com.musfit.domain.model.ExerciseProgress
import com.musfit.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TrainingProgressViewModelRetryTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun sameSelection_retriesFailedInitialObservation() = runTest {
        val loaded = progress("Loaded after retry", 125.0)
        val repository = ProgressRetryRepository(
            flow { throw IllegalStateException("initial progress load failed") },
            flowOf(loaded),
        )
        val viewModel = TrainingProgressViewModel(repository)
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect { }
        }

        viewModel.selectProgressExercise(EXERCISE_ID)
        testScheduler.runCurrent()
        assertEquals(1, repository.progressObservationStarts)
        assertNull(viewModel.state.value.selectedExerciseProgress)

        viewModel.selectProgressExercise(EXERCISE_ID)
        testScheduler.runCurrent()

        assertEquals(2, repository.progressObservationStarts)
        assertEquals(loaded, viewModel.state.value.selectedExerciseProgress)
        collector.cancel()
    }

    @Test
    fun sameSelection_retryPreservesLastValueUntilReplacementArrives() = runTest {
        val stale = progress("Last known value", 120.0)
        val refreshed = progress("Refreshed value", 127.5)
        val retryGate = CompletableDeferred<Unit>()
        val repository = ProgressRetryRepository(
            flow {
                emit(stale)
                throw IllegalStateException("progress observation failed after a value")
            },
            flow {
                retryGate.await()
                emit(refreshed)
                awaitCancellation()
            },
        )
        val viewModel = TrainingProgressViewModel(repository)
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect { }
        }

        viewModel.selectProgressExercise(EXERCISE_ID)
        testScheduler.runCurrent()
        assertEquals(stale, viewModel.state.value.selectedExerciseProgress)

        viewModel.selectProgressExercise(EXERCISE_ID)
        testScheduler.runCurrent()
        assertEquals(2, repository.progressObservationStarts)
        assertEquals(stale, viewModel.state.value.selectedExerciseProgress)

        retryGate.complete(Unit)
        testScheduler.runCurrent()
        assertEquals(refreshed, viewModel.state.value.selectedExerciseProgress)
        collector.cancel()
    }

    private fun progress(name: String, heaviestWeightKg: Double) = ExerciseProgress(
        exerciseId = EXERCISE_ID,
        exerciseName = name,
        equipment = "barbell",
        targetMuscles = "chest",
        heaviestWeightKg = heaviestWeightKg,
        maxReps = 5,
        bestEstimatedOneRepMaxKg = heaviestWeightKg * 1.1,
        bestWorkoutVolumeKg = 1_500.0,
        trend = emptyList(),
    )

    private class ProgressRetryRepository(
        vararg observations: Flow<ExerciseProgress?>,
    ) : TrainingRepository {
        private val observations = ArrayDeque(observations.toList())
        var progressObservationStarts = 0
            private set

        override fun observeExerciseProgress(exerciseId: String): Flow<ExerciseProgress?> {
            progressObservationStarts += 1
            return observations.removeFirst()
        }

        override suspend fun addCompletedSet(
            exerciseName: String,
            reps: Int,
            weightKg: Double,
        ): LoggedWorkoutSet = error("Not used")

        override suspend fun setCompletion(setId: String, completed: Boolean) = error("Not used")

        override fun observeDailyTrainingSummary(date: LocalDate): Flow<TrainingSummary> = error("Not used")

        override suspend fun createRoutine(input: RoutineInput): String = error("Not used")

        override suspend fun updateRoutine(routineId: String, input: RoutineInput) = error("Not used")

        override suspend fun duplicateRoutine(routineId: String): String = error("Not used")

        override suspend fun deleteRoutine(routineId: String) = error("Not used")

        override suspend fun getRoutineDetail(routineId: String): RoutineDetail? = error("Not used")

        override suspend fun startBlankWorkout(): String = error("Not used")

        override suspend fun startWorkoutFromRoutine(routineId: String): String = error("Not used")

        override suspend fun finishWorkout(sessionId: String) = error("Not used")

        override suspend fun discardWorkout(sessionId: String) = error("Not used")

        override suspend fun getLatestWorkoutForExport(): WorkoutForExport? = error("Not used")

        override suspend fun markWorkoutExported(
            sessionId: String,
            recordId: String,
            exportedAtEpochMillis: Long,
        ) = error("Not used")
    }

    private companion object {
        const val EXERCISE_ID = "exercise-bench-press"
    }
}
