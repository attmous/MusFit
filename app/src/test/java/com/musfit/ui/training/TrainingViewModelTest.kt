package com.musfit.ui.training

import com.musfit.data.repository.LoggedWorkoutSet
import com.musfit.data.repository.ActiveWorkoutSummary
import com.musfit.data.repository.ExerciseSummary
import com.musfit.data.repository.RoutineSummary
import com.musfit.data.repository.RoutineDetail
import com.musfit.data.repository.RoutineExerciseDetail
import com.musfit.data.repository.RoutineExerciseInput
import com.musfit.data.repository.RoutineInput
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
        assertEquals(
            listOf("Barbell Bench Press", "Chest Supported Row"),
            state.exercises.map { it.name },
        )
    }

    @Test
    fun resumeActiveWorkout_updatesVisibleMessageState() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository)

        viewModel.resumeActiveWorkout()

        assertTrue(viewModel.state.value.activeWorkoutRouteOpen)
    }

    @Test
    fun openRoutineEditor_forExistingRoutineLoadsRoutineIntoEditor() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository)

        viewModel.openRoutineEditor("routine-full-body-a")
        dispatcher.scheduler.advanceUntilIdle()

        val editor = viewModel.state.value.routineEditor
        assertTrue(editor.isOpen)
        assertEquals("routine-full-body-a", editor.routineId)
        assertEquals("Full Body A", editor.name)
        assertEquals("Starter routine", editor.notes)
        assertEquals(1, editor.exercises.size)
        assertEquals("exercise-bench-press", editor.exercises.single().exerciseId)
    }

    @Test
    fun saveRoutineEditor_forNewRoutinePersistsAndClosesEditor() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository)

        viewModel.openRoutineEditor(null)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onRoutineNameChanged("Leg Day")
        viewModel.onRoutineNotesChanged("Heavy")
        viewModel.saveRoutineEditor()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            RoutineInput(
                name = "Leg Day",
                notes = "Heavy",
                exercises = emptyList(),
            ),
            repository.createdRoutineInput,
        )
        assertFalse(viewModel.state.value.routineEditor.isOpen)
    }

    @Test
    fun routineEditor_addsEditsAndRemovesExercisesBeforeSave() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository)

        viewModel.openRoutineEditor(null)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onRoutineNameChanged("Upper")
        viewModel.addRoutineExercise("exercise-bench-press")
        viewModel.onRoutineExerciseTargetSetsChanged(index = 0, value = "4")
        viewModel.onRoutineExerciseTargetRepsChanged(index = 0, value = "8")
        viewModel.addRoutineExercise("exercise-row")
        viewModel.removeRoutineExercise(index = 1)
        viewModel.saveRoutineEditor()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            RoutineInput(
                name = "Upper",
                notes = "",
                exercises = listOf(
                    RoutineExerciseInput(
                        exerciseId = "exercise-bench-press",
                        targetSets = 4,
                        targetReps = "8",
                    ),
                ),
            ),
            repository.createdRoutineInput,
        )
    }

    @Test
    fun startRoutine_startsWorkoutAndOpensActiveWorkoutRoute() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository)

        viewModel.startRoutine("routine-full-body-a")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("routine-full-body-a", repository.startedRoutineId)
        assertTrue(viewModel.state.value.activeWorkoutRouteOpen)
    }

    @Test
    fun startBlankWorkout_startsWorkoutAndOpensActiveWorkoutRoute() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository)

        viewModel.startBlankWorkout()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.startBlankWorkoutCalls)
        assertTrue(viewModel.state.value.activeWorkoutRouteOpen)
    }

    @Test
    fun closeActiveWorkoutRoute_returnsToRoutinesHome() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository)

        viewModel.resumeActiveWorkout()
        viewModel.closeActiveWorkoutRoute()

        assertFalse(viewModel.state.value.activeWorkoutRouteOpen)
        assertEquals(TrainingSection.Routines, viewModel.state.value.selectedSection)
    }

    private class FakeTrainingRepository : TrainingRepository {
        var addCalls = 0
        var seedCalls = 0
        var savedExerciseName: String? = null
        var savedReps: Int? = null
        var savedWeightKg: Double? = null
        var updatedSetId: String? = null
        var updatedCompleted: Boolean? = null
        var createdRoutineInput: RoutineInput? = null
        var updatedRoutineId: String? = null
        var updatedRoutineInput: RoutineInput? = null
        var startedRoutineId: String? = null
        var startBlankWorkoutCalls = 0
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
                ExerciseSummary(
                    id = "exercise-row",
                    name = "Chest Supported Row",
                    category = "strength",
                    equipment = "machine",
                    targetMuscles = "back,biceps",
                    isCustom = false,
                ),
            ),
        )
        private val activeWorkoutFlow = MutableStateFlow<ActiveWorkoutSummary?>(null)
        private val routineDetails = mutableMapOf(
            "routine-full-body-a" to
                RoutineDetail(
                    id = "routine-full-body-a",
                    name = "Full Body A",
                    notes = "Starter routine",
                    isStarter = true,
                    exercises = listOf(
                        RoutineExerciseDetail(
                            id = "routine-exercise-1",
                            exercise = exercisesFlow.value.first { it.id == "exercise-bench-press" },
                            sortOrder = 0,
                            targetSets = 3,
                            targetReps = "5",
                        ),
                    ),
                ),
        )

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

        override suspend fun createRoutine(input: RoutineInput): String {
            createdRoutineInput = input
            return "new-routine-id"
        }

        override suspend fun updateRoutine(routineId: String, input: RoutineInput) {
            updatedRoutineId = routineId
            updatedRoutineInput = input
        }

        override suspend fun duplicateRoutine(routineId: String): String = "$routineId-copy"

        override suspend fun deleteRoutine(routineId: String) = Unit

        override suspend fun getRoutineDetail(routineId: String): RoutineDetail? = routineDetails[routineId]

        override suspend fun startBlankWorkout(): String {
            startBlankWorkoutCalls += 1
            return "blank-session-id"
        }

        override suspend fun startWorkoutFromRoutine(routineId: String): String {
            startedRoutineId = routineId
            return "session-for-$routineId"
        }

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
