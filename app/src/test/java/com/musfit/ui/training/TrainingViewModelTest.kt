package com.musfit.ui.training

import com.musfit.data.repository.LoggedWorkoutSet
import com.musfit.data.repository.ActiveWorkoutSummary
import com.musfit.data.repository.ExerciseSummary
import com.musfit.data.repository.RoutineSummary
import com.musfit.data.repository.TrainingRepository
import com.musfit.data.repository.TrainingSummary
import com.musfit.data.repository.WorkoutForExport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TrainingViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun addCompletedSet_persistsSetAndUpdatesVolume() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository)

        viewModel.onExerciseChanged("Bench Press")
        viewModel.onRepsChanged("5")
        viewModel.onWeightChanged("100")
        viewModel.addSet()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.sets.size)
        assertEquals("Bench Press", repository.savedExerciseName)
        assertEquals(5, repository.savedReps)
        assertEquals(100.0, repository.savedWeightKg ?: 0.0, 0.01)
        assertEquals(500.0, state.totalVolumeKg, 0.01)
        assertEquals(116.67, state.bestEstimatedOneRepMaxKg, 0.01)
    }

    @Test
    fun addSet_rejectsZeroReps() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository)

        viewModel.onExerciseChanged("Bench Press")
        viewModel.onRepsChanged("0")
        viewModel.onWeightChanged("100")
        viewModel.addSet()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.sets.isEmpty())
        assertEquals(0, repository.addCalls)
        assertEquals(0.0, state.totalVolumeKg, 0.01)
    }

    @Test
    fun onRepsChanged_stripsNegativeSignBeforeAdd() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository)

        viewModel.onExerciseChanged("Bench Press")
        viewModel.onRepsChanged("-5")
        viewModel.onWeightChanged("100")
        viewModel.addSet()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("5", viewModel.state.value.sets.single().reps.toString())
        assertEquals(5, repository.savedReps)
    }

    @Test
    fun addSet_rejectsZeroWeight() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository)

        viewModel.onExerciseChanged("Bench Press")
        viewModel.onRepsChanged("5")
        viewModel.onWeightChanged("0")
        viewModel.addSet()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.sets.isEmpty())
        assertEquals(0, repository.addCalls)
        assertEquals(0.0, state.totalVolumeKg, 0.01)
    }

    @Test
    fun onWeightChanged_stripsNegativeSignBeforeAdd() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository)

        viewModel.onExerciseChanged("Bench Press")
        viewModel.onRepsChanged("5")
        viewModel.onWeightChanged("-100")
        viewModel.addSet()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(100.0, viewModel.state.value.sets.single().weightKg, 0.01)
        assertEquals(100.0, repository.savedWeightKg ?: 0.0, 0.01)
    }

    @Test
    fun toggleSetCompletion_updatesRepositoryAndExcludesIncompleteSetFromVolume() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository)

        viewModel.onExerciseChanged("Bench Press")
        viewModel.onRepsChanged("5")
        viewModel.onWeightChanged("100")
        viewModel.addSet()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleSetCompletion(setIndex = 0)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.sets[0].completed)
        assertEquals("set-1", repository.updatedSetId)
        assertEquals(false, repository.updatedCompleted)
        assertEquals(0.0, state.totalVolumeKg, 0.01)
        assertEquals(0.0, state.bestEstimatedOneRepMaxKg, 0.01)
    }

    @Test
    fun addSet_withBlankExercise_usesCustomLabel() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository)

        viewModel.onRepsChanged("8")
        viewModel.onWeightChanged("60")
        viewModel.addSet()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Custom exercise", state.sets.single().exerciseName)
        assertTrue(state.sets.single().completed)
        assertEquals(480.0, state.totalVolumeKg, 0.01)
    }

    @Test
    fun initialState_isRoutinesFirstAndSeedsStarterTrainingData() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value

        assertEquals(TrainingSection.Routines, state.selectedSection)
        assertEquals(1, repository.seedCalls)
        assertEquals(listOf("Full Body A"), state.routines.map { it.name })
        assertEquals(listOf("Barbell Bench Press"), state.exercises.map { it.name })
    }

    @Test
    fun resumeActiveWorkout_updatesVisibleMessageState() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository)

        viewModel.resumeActiveWorkout()

        assertEquals("Active workout resume is not wired yet.", viewModel.state.value.message)
    }

    private class FakeTrainingRepository : TrainingRepository {
        var addCalls = 0
        var seedCalls = 0
        var savedExerciseName: String? = null
        var savedReps: Int? = null
        var savedWeightKg: Double? = null
        var updatedSetId: String? = null
        var updatedCompleted: Boolean? = null
        private val routinesFlow = MutableStateFlow(
            listOf(
                RoutineSummary(
                    id = "routine-full-body-a",
                    name = "Full Body A",
                    notes = "Starter routine",
                    exerciseCount = 5,
                    targetSetCount = 15,
                    isStarter = true,
                ),
            ),
        )
        private val exercisesFlow = MutableStateFlow(
            listOf(
                ExerciseSummary(
                    id = "exercise-bench-press",
                    name = "Barbell Bench Press",
                    category = "strength",
                    equipment = "barbell",
                    targetMuscles = "chest,triceps,shoulders",
                    isCustom = false,
                ),
            ),
        )
        private val activeWorkoutFlow = MutableStateFlow<ActiveWorkoutSummary?>(null)

        override suspend fun addCompletedSet(
            exerciseName: String,
            reps: Int,
            weightKg: Double,
        ): LoggedWorkoutSet {
            addCalls += 1
            savedExerciseName = exerciseName
            savedReps = reps
            savedWeightKg = weightKg
            return LoggedWorkoutSet(
                id = "set-$addCalls",
                exerciseName = exerciseName.ifBlank { "Custom exercise" },
                reps = reps,
                weightKg = weightKg,
                completed = true,
            )
        }

        override suspend fun setCompletion(setId: String, completed: Boolean) {
            updatedSetId = setId
            updatedCompleted = completed
        }

        override fun observeExercises(
            query: String,
            muscle: String?,
            equipment: String?,
        ): Flow<List<ExerciseSummary>> = exercisesFlow

        override fun observeRoutineSummaries(): Flow<List<RoutineSummary>> = routinesFlow

        override fun observeActiveWorkoutSummary(): Flow<ActiveWorkoutSummary?> = activeWorkoutFlow

        override suspend fun seedStarterTrainingData() {
            seedCalls += 1
        }

        override fun observeDailyTrainingSummary(date: LocalDate): Flow<TrainingSummary> =
            flowOf(TrainingSummary())

        override suspend fun getLatestWorkoutForExport(): WorkoutForExport? = null

        override suspend fun markWorkoutExported(
            sessionId: String,
            recordId: String,
            exportedAtEpochMillis: Long,
        ) = Unit
    }
}
