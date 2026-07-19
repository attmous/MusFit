package com.musfit.ui.training

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.repeatOnLifecycle
import com.musfit.data.repository.ActiveWorkoutDetail
import com.musfit.data.repository.ActiveWorkoutSummary
import com.musfit.data.repository.ExerciseDetail
import com.musfit.data.repository.ExerciseGrouping
import com.musfit.data.repository.ExerciseInput
import com.musfit.data.repository.ExerciseSummary
import com.musfit.data.repository.GoalsRepository
import com.musfit.data.repository.LoggedWorkoutSet
import com.musfit.data.repository.LoggedWorkoutSetDetail
import com.musfit.data.repository.MuscleGroupProgress
import com.musfit.data.repository.RoutineDetail
import com.musfit.data.repository.RoutineExerciseDetail
import com.musfit.data.repository.RoutineExerciseInput
import com.musfit.data.repository.RoutineFolder
import com.musfit.data.repository.RoutineInput
import com.musfit.data.repository.RoutineSetInput
import com.musfit.data.repository.RoutineSummary
import com.musfit.data.repository.TrainingPrRecord
import com.musfit.data.repository.TrainingProgressAnalytics
import com.musfit.data.repository.TrainingRepository
import com.musfit.data.repository.TrainingSettings
import com.musfit.data.repository.TrainingSettingsInput
import com.musfit.data.repository.TrainingSummary
import com.musfit.data.repository.UserGoals
import com.musfit.data.repository.WeeklyTrainingVolume
import com.musfit.data.repository.WorkoutExerciseBlock
import com.musfit.data.repository.WorkoutForExport
import com.musfit.data.repository.WorkoutHistoryDetail
import com.musfit.data.repository.WorkoutHistorySummary
import com.musfit.data.repository.WorkoutSetInputData
import com.musfit.domain.model.ExerciseProgress
import com.musfit.domain.model.TrainingTrendPoint
import com.musfit.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalCoroutinesApi::class)
class TrainingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    private val dispatcher get() = mainDispatcherRule.dispatcher

    @Test
    fun repositoryObservation_stopsAfterGraceAndSharesDestinationCollectors() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        testScheduler.runCurrent()
        testScheduler.advanceTimeBy(10_000)
        testScheduler.runCurrent()

        assertEquals(0, repository.activeRoutineSummaryCollectors)
        assertEquals(0, repository.routineSummarySubscriptionStarts)

        val routinesCollector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.routinesLibraryState.collect { }
        }
        testScheduler.runCurrent()
        assertEquals(1, repository.activeRoutineSummaryCollectors)

        val activeCollector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.activeHistoryState.collect { }
        }
        testScheduler.runCurrent()
        assertEquals(1, repository.activeRoutineSummaryCollectors)
        assertEquals(1, repository.maxRoutineSummaryCollectors)

        routinesCollector.cancel()
        testScheduler.runCurrent()
        assertEquals(1, repository.activeRoutineSummaryCollectors)

        activeCollector.cancel()
        testScheduler.advanceTimeBy(4_999)
        testScheduler.runCurrent()
        assertEquals(1, repository.activeRoutineSummaryCollectors)

        testScheduler.advanceTimeBy(2)
        testScheduler.runCurrent()
        assertEquals(0, repository.activeRoutineSummaryCollectors)
    }

    @Test
    fun progressObservation_stopsAfterGraceAndSharesCollectors() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingProgressViewModel(repository)
        testScheduler.advanceUntilIdle()

        assertEquals(0, repository.activeProgressExerciseListCollectors)

        val firstCollector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect { }
        }
        val secondCollector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect { }
        }
        testScheduler.runCurrent()
        assertEquals(1, repository.activeProgressExerciseListCollectors)
        assertEquals(1, repository.maxProgressExerciseListCollectors)

        firstCollector.cancel()
        testScheduler.runCurrent()
        assertEquals(1, repository.activeProgressExerciseListCollectors)

        secondCollector.cancel()
        testScheduler.advanceTimeBy(4_999)
        testScheduler.runCurrent()
        assertEquals(1, repository.activeProgressExerciseListCollectors)

        testScheduler.advanceTimeBy(2)
        testScheduler.runCurrent()
        assertEquals(0, repository.activeProgressExerciseListCollectors)
    }

    @Test
    fun repositoryObservation_resumesWithCurrentValuesWithoutDuplicateCollectors() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        val bootstrapCollector = observeTraining(viewModel)

        val originalName = viewModel.state.value.routines.first().name
        bootstrapCollector.cancel()
        testScheduler.advanceTimeBy(5_001)
        testScheduler.runCurrent()
        assertEquals(0, repository.activeRoutineSummaryCollectors)
        repository.renameFirstRoutine("Updated while stopped")
        testScheduler.runCurrent()
        assertEquals(originalName, viewModel.state.value.routines.first().name)

        repeat(3) {
            val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.routinesLibraryState.collect { }
            }
            testScheduler.runCurrent()
            assertEquals("Updated while stopped", viewModel.state.value.routines.first().name)
            assertEquals(1, repository.activeRoutineSummaryCollectors)

            collector.cancel()
            testScheduler.advanceTimeBy(5_001)
            testScheduler.runCurrent()
            assertEquals(0, repository.activeRoutineSummaryCollectors)
        }

        assertEquals(1, repository.maxRoutineSummaryCollectors)
    }

    @Test
    fun lifecycleOwner_stoppedAndStarted_resumesCurrentValuesWithoutDuplicateCollectors() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        testScheduler.runCurrent()
        val owner = object : LifecycleOwner {
            override val lifecycle = LifecycleRegistry.createUnsafe(this)
        }
        val lifecycle = owner.lifecycle
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        val lifecycleCollection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.routinesLibraryState.collect { }
            }
        }
        testScheduler.runCurrent()
        assertEquals(0, repository.activeRoutineSummaryCollectors)

        repeat(3) { cycle ->
            val currentName = "Updated while stopped ${cycle + 1}"
            repository.renameFirstRoutine(currentName)
            testScheduler.runCurrent()
            assertTrue(viewModel.state.value.routines.none { it.name == currentName })

            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
            testScheduler.runCurrent()
            assertEquals(currentName, viewModel.state.value.routines.first().name)
            assertEquals(1, repository.activeRoutineSummaryCollectors)
            assertEquals(cycle + 1, repository.routineSummarySubscriptionStarts)

            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            testScheduler.advanceTimeBy(4_999)
            testScheduler.runCurrent()
            assertEquals(1, repository.activeRoutineSummaryCollectors)
            testScheduler.advanceTimeBy(2)
            testScheduler.runCurrent()
            assertEquals(0, repository.activeRoutineSummaryCollectors)
        }

        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        lifecycleCollection.join()
        assertEquals(1, repository.maxRoutineSummaryCollectors)
    }

    @Test
    fun repositoryObservation_failedSourceDoesNotCancelSibling() = runTest {
        val repository = FakeTrainingRepository().apply {
            failRoutineSummariesAfterCurrentValue()
        }
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        testScheduler.advanceUntilIdle()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.activeHistoryState.collect { }
        }
        testScheduler.runCurrent()

        repository.renameFirstExercise("Sibling still active")
        testScheduler.runCurrent()

        assertEquals("Sibling still active", viewModel.state.value.exercises.first().name)
        assertEquals("routine observation failed", viewModel.state.value.message)
        collector.cancel()
    }

    @Test
    fun mutationsAndRestTimerContinueAfterRepositoryObservationStops() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        val collector = observeTraining(viewModel)
        collector.cancel()
        testScheduler.advanceTimeBy(5_001)
        testScheduler.runCurrent()
        assertEquals(0, repository.activeRoutineSummaryCollectors)

        viewModel.onRoutineExercisePickerSearchChanged("bench")
        viewModel.toggleWorkoutSetCompletion("set-1", completed = true)
        testScheduler.runCurrent()
        val remainingBeforeTick = viewModel.state.value.restTimer.remainingSeconds
        viewModel.tickRestTimer()

        assertEquals("bench", viewModel.state.value.routineExercisePickerSearchQuery)
        assertTrue(viewModel.state.value.restTimer.isRunning)
        assertEquals(remainingBeforeTick - 1, viewModel.state.value.restTimer.remainingSeconds)
        assertEquals(0, repository.activeRoutineSummaryCollectors)
    }

    @Test
    fun progressObservation_sameSelectionKeepsLoadedValueAndStopsWithScreen() = runTest {
        val repository = FakeTrainingRepository().apply {
            useSingleEmissionProgressObservation()
        }
        val viewModel = TrainingProgressViewModel(repository)
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect { }
        }
        testScheduler.runCurrent()

        viewModel.selectProgressExercise("exercise-bench-press")
        testScheduler.runCurrent()
        assertEquals("Barbell Bench Press", viewModel.state.value.selectedExerciseProgress?.exerciseName)
        assertEquals(1, repository.activeSelectedProgressCollectors)

        viewModel.openExercisePicker()
        viewModel.selectProgressExercise("exercise-bench-press")
        testScheduler.runCurrent()
        assertEquals("Barbell Bench Press", viewModel.state.value.selectedExerciseProgress?.exerciseName)
        assertFalse(viewModel.state.value.exercisePickerOpen)
        assertEquals(1, repository.observedProgressExerciseIds.size)

        collector.cancel()
        testScheduler.advanceTimeBy(4_999)
        testScheduler.runCurrent()
        assertEquals(1, repository.activeSelectedProgressCollectors)
        testScheduler.advanceTimeBy(2)
        testScheduler.runCurrent()
        assertEquals(0, repository.activeSelectedProgressCollectors)
    }

    @Test
    fun destinationStateFlowsOnlyEmitForTheirOwnDomain() = runTest {
        val viewModel = TrainingViewModel(FakeTrainingRepository(), FakeGoalsRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.routinesLibraryState.collect { }
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.activeHistoryState.collect { }
        }
        testScheduler.advanceUntilIdle()

        val routinesBeforeActiveChange = viewModel.routinesLibraryState.value
        val activeBeforeActiveChange = viewModel.activeHistoryState.value
        viewModel.onActiveWorkoutNotesChanged("keep elbows tucked")
        testScheduler.advanceUntilIdle()

        assertSame(routinesBeforeActiveChange, viewModel.routinesLibraryState.value)
        assertNotSame(activeBeforeActiveChange, viewModel.activeHistoryState.value)

        val routinesBeforeLibraryChange = viewModel.routinesLibraryState.value
        val activeBeforeLibraryChange = viewModel.activeHistoryState.value
        viewModel.onRoutineExercisePickerSearchChanged("bench")
        testScheduler.advanceUntilIdle()

        assertNotSame(routinesBeforeLibraryChange, viewModel.routinesLibraryState.value)
        assertSame(activeBeforeLibraryChange, viewModel.activeHistoryState.value)
    }

    @Test
    fun restoration_retainsRoutineRouteAndBoundedSetDraft() = runTest {
        val savedStateHandle = SavedStateHandle()
        val repository = FakeTrainingRepository()
        val first = TrainingViewModel(repository, FakeGoalsRepository(), savedStateHandle)
        dispatcher.scheduler.advanceUntilIdle()

        first.openRoutineLibraryPage()
        first.openRoutineDetail("routine-upper-a")
        dispatcher.scheduler.advanceUntilIdle()
        first.openRoutineEditor("routine-upper-a")
        dispatcher.scheduler.advanceUntilIdle()
        first.onRoutineNameChanged("Upper A restored")
        first.onRoutineNotesChanged("Keep the paused draft")
        first.onRoutineExerciseSetRepsChanged(exerciseIndex = 0, setIndex = 0, value = "6")
        first.onRoutineExerciseSetWeightChanged(exerciseIndex = 0, setIndex = 0, value = "102.5")
        dispatcher.scheduler.advanceUntilIdle()

        val restored = TrainingViewModel(repository, FakeGoalsRepository(), savedStateHandle)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("routine-upper-a", restored.state.value.selectedRoutineDetail?.id)
        assertEquals("routine-upper-a", restored.state.value.routineEditor.routineId)
        assertEquals("Upper A restored", restored.state.value.routineEditor.name)
        assertEquals("Keep the paused draft", restored.state.value.routineEditor.notes)
        assertEquals("6", restored.state.value.routineEditor.exercises.first().setPlans.first().targetReps)
        assertEquals(102.5, restored.state.value.routineEditor.exercises.first().setPlans.first().targetWeightKg)
        assertEquals(null, restored.state.value.message)
    }

    @Test
    fun restoration_retainsRoutineExercisePickerDraft() = runTest {
        val savedStateHandle = SavedStateHandle()
        val repository = FakeTrainingRepository()
        val first = TrainingViewModel(repository, FakeGoalsRepository(), savedStateHandle)
        observeTraining(first)
        dispatcher.scheduler.advanceUntilIdle()

        first.openRoutineExercisePicker()
        first.onRoutineExercisePickerSearchChanged("bench")
        first.toggleRoutineExercisePickerEquipment("barbell")
        first.toggleRoutineExercisePickerMuscle("chest")
        first.setRoutineExercisePickerOnlyDone(true)
        first.toggleRoutineExercisePickerSelection("exercise-bench-press")
        dispatcher.scheduler.advanceUntilIdle()

        val restored = TrainingViewModel(repository, FakeGoalsRepository(), savedStateHandle)
        observeTraining(restored)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("bench", restored.state.value.routineExercisePickerSearchQuery)
        assertEquals(setOf("barbell"), restored.state.value.routineExercisePickerFilters.equipment)
        assertEquals(setOf("chest"), restored.state.value.routineExercisePickerFilters.muscles)
        assertTrue(restored.state.value.routineExercisePickerFilters.onlyDone)
        assertEquals(
            setOf("exercise-bench-press"),
            restored.state.value.routineExercisePickerSelectedIds,
        )
    }

    @Test
    fun restoration_reopensRoomOwnedActiveWorkoutWithoutDuplicateSetOrTimerWork() = runTest {
        val savedStateHandle = SavedStateHandle()
        val repository = FakeTrainingRepository()
        val first = TrainingViewModel(repository, FakeGoalsRepository(), savedStateHandle)
        observeTraining(first)
        dispatcher.scheduler.advanceUntilIdle()
        first.resumeActiveWorkout()
        first.toggleWorkoutSetCompletion("set-1", completed = true)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(first.state.value.restTimer.isRunning)
        val setCountBeforeRestore = repository.activeWorkoutDetail.value
            ?.exerciseBlocks
            ?.flatMap { it.sets }
            ?.size

        val restored = TrainingViewModel(repository, FakeGoalsRepository(), savedStateHandle)
        observeTraining(restored)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("session-1", restored.state.value.activeWorkout?.sessionId)
        assertEquals(
            setCountBeforeRestore,
            repository.activeWorkoutDetail.value?.exerciseBlocks?.flatMap { it.sets }?.size,
        )
        assertFalse(restored.state.value.restTimer.isVisible)
        assertFalse(restored.state.value.restTimer.isRunning)
        assertEquals(0, repository.startBlankWorkoutCalls)
    }

    @Test
    fun routeDetailLoads_ignoreLateResultsFromSupersededRequests() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        testScheduler.advanceUntilIdle()
        val routineGate = CompletableDeferred<Unit>()
        val exerciseGate = CompletableDeferred<Unit>()
        val workoutGate = CompletableDeferred<Unit>()
        repository.routineDetailLoadGates["routine-upper-a"] = routineGate
        repository.exerciseDetailLoadGates["exercise-bench-press"] = exerciseGate
        repository.workoutDetailLoadGates["session-history-1"] = workoutGate

        viewModel.openRoutineDetail("routine-upper-a")
        viewModel.openRoutineEditor("routine-upper-a")
        viewModel.openExerciseDetail("exercise-bench-press")
        viewModel.openWorkoutDetail("session-history-1")
        testScheduler.runCurrent()

        viewModel.openRoutineDetail("routine-full-body-a")
        viewModel.openRoutineEditor("routine-full-body-a")
        viewModel.openExerciseDetail("exercise-row")
        viewModel.openWorkoutDetail("session-history-2")
        testScheduler.runCurrent()

        assertEquals("routine-full-body-a", viewModel.state.value.selectedRoutineDetail?.id)
        assertEquals("routine-full-body-a", viewModel.state.value.routineEditor.routineId)
        assertEquals("exercise-row", viewModel.state.value.selectedExerciseDetail?.id)
        assertEquals("session-history-2", viewModel.state.value.selectedWorkoutDetail?.summary?.sessionId)

        routineGate.complete(Unit)
        exerciseGate.complete(Unit)
        workoutGate.complete(Unit)
        testScheduler.advanceUntilIdle()

        assertEquals("routine-full-body-a", viewModel.state.value.selectedRoutineDetail?.id)
        assertEquals("routine-full-body-a", viewModel.state.value.routineEditor.routineId)
        assertEquals("exercise-row", viewModel.state.value.selectedExerciseDetail?.id)
        assertEquals("session-history-2", viewModel.state.value.selectedWorkoutDetail?.summary?.sessionId)
    }

    @Test
    fun closingDetailRoutes_invalidatesLateResults() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        testScheduler.advanceUntilIdle()
        val routineGate = CompletableDeferred<Unit>()
        val exerciseGate = CompletableDeferred<Unit>()
        val workoutGate = CompletableDeferred<Unit>()
        repository.routineDetailLoadGates["routine-upper-a"] = routineGate
        repository.exerciseDetailLoadGates["exercise-bench-press"] = exerciseGate
        repository.workoutDetailLoadGates["session-history-1"] = workoutGate

        viewModel.openRoutineDetail("routine-upper-a")
        viewModel.openRoutineEditor("routine-upper-a")
        viewModel.openRoutineExerciseDetail("exercise-bench-press", target = "3 x 6")
        viewModel.openWorkoutDetail("session-history-1")
        testScheduler.runCurrent()

        viewModel.closeRoutineDetail()
        viewModel.closeRoutineEditor()
        viewModel.closeExerciseDetail()
        viewModel.closeWorkoutDetail()
        routineGate.complete(Unit)
        exerciseGate.complete(Unit)
        workoutGate.complete(Unit)
        testScheduler.advanceUntilIdle()

        assertEquals(null, viewModel.state.value.selectedRoutineDetail)
        assertFalse(viewModel.state.value.routineEditor.isOpen)
        assertEquals(null, viewModel.state.value.selectedExerciseDetail)
        assertEquals("", viewModel.state.value.exerciseDetailNotesInput)
        assertEquals(null, viewModel.state.value.exerciseDetailTarget)
        assertEquals(null, viewModel.state.value.selectedWorkoutDetail)
    }

    @Test
    fun restoration_doesNotOverwriteNewerRouteRequests() = runTest {
        val savedStateHandle = SavedStateHandle()
        val repository = FakeTrainingRepository()
        val first = TrainingViewModel(repository, FakeGoalsRepository(), savedStateHandle)
        testScheduler.advanceUntilIdle()
        first.openRoutineDetail("routine-upper-a")
        first.openRoutineEditor("routine-upper-a")
        first.openRoutineExerciseDetail("exercise-bench-press", target = "3 x 6")
        first.openWorkoutDetail("session-history-1")
        testScheduler.advanceUntilIdle()

        val restoredRoutineGate = CompletableDeferred<Unit>()
        repository.routineDetailLoadGates["routine-upper-a"] = restoredRoutineGate
        val restored = TrainingViewModel(repository, FakeGoalsRepository(), savedStateHandle)
        testScheduler.runCurrent()

        restored.openRoutineDetail("routine-full-body-a")
        restored.openRoutineEditor("routine-full-body-a")
        restored.openRoutineExerciseDetail("exercise-row", target = "4 x 8")
        restored.openWorkoutDetail("session-history-2")
        testScheduler.runCurrent()

        restoredRoutineGate.complete(Unit)
        testScheduler.advanceUntilIdle()

        assertEquals("routine-full-body-a", restored.state.value.selectedRoutineDetail?.id)
        assertEquals("routine-full-body-a", restored.state.value.routineEditor.routineId)
        assertEquals("exercise-row", restored.state.value.selectedExerciseDetail?.id)
        assertEquals("4 x 8", restored.state.value.exerciseDetailTarget)
        assertEquals("session-history-2", restored.state.value.selectedWorkoutDetail?.summary?.sessionId)
    }

    @Test
    fun restoration_doesNotReopenRoutesClosedBeforeRestoreStarts() = runTest {
        val savedStateHandle = SavedStateHandle()
        val repository = FakeTrainingRepository()
        val first = TrainingViewModel(repository, FakeGoalsRepository(), savedStateHandle)
        testScheduler.advanceUntilIdle()
        first.openRoutineDetail("routine-upper-a")
        first.openRoutineEditor("routine-upper-a")
        first.openRoutineExerciseDetail("exercise-bench-press", target = "3 x 6")
        first.openWorkoutDetail("session-history-1")
        testScheduler.advanceUntilIdle()

        val restored = TrainingViewModel(repository, FakeGoalsRepository(), savedStateHandle)
        restored.closeRoutineDetail()
        restored.closeRoutineEditor()
        restored.closeExerciseDetail()
        restored.closeWorkoutDetail()
        testScheduler.advanceUntilIdle()

        assertEquals(null, restored.state.value.selectedRoutineDetail)
        assertFalse(restored.state.value.routineEditor.isOpen)
        assertEquals(null, restored.state.value.selectedExerciseDetail)
        assertEquals("", restored.state.value.exerciseDetailNotesInput)
        assertEquals(null, restored.state.value.exerciseDetailTarget)
        assertEquals(null, restored.state.value.selectedWorkoutDetail)
    }

    @Test
    fun closeActiveWorkoutRoute_stopsRestTimer() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        observeTraining(viewModel)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.resumeActiveWorkout()
        viewModel.toggleWorkoutSetCompletion("set-1", completed = true)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.restTimer.isRunning)

        viewModel.closeActiveWorkoutRoute()

        assertFalse(viewModel.state.value.restTimer.isVisible)
        assertFalse(viewModel.state.value.restTimer.isRunning)
    }

    @Test
    fun addCompletedSet_persistsSetAndUpdatesVolume() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())

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
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())

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
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())

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
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())

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
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())

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
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())

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
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())

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
    fun weeklySessionTarget_streamsFromUserGoals() = runTest {
        val viewModel = TrainingViewModel(
            FakeTrainingRepository(),
            FakeGoalsRepository(UserGoals(weeklySessionTarget = 4)),
        )
        observeTraining(viewModel)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(4, viewModel.state.value.weeklySessionTarget)
    }

    @Test
    fun initialState_isTrainingHomeAndSeedsStarterTrainingData() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        observeTraining(viewModel)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value

        assertEquals("Routines", state.selectedSection.name)
        assertEquals(1, repository.seedCalls)
        assertEquals(listOf("Full Body A", "Upper A"), state.routines.map { it.name })
        assertEquals(
            listOf("Barbell Bench Press", "Chest Supported Row"),
            state.exercises.map { it.name },
        )
    }

    @Test
    fun exerciseLibrary_filtersVisibleExercisesBySearchEquipmentAndMuscle() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        observeTraining(viewModel)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onExerciseSearchQueryChanged("press")

        assertEquals(
            listOf("Barbell Bench Press"),
            viewModel.state.value.visibleExercises.map { it.name },
        )

        viewModel.onExerciseSearchQueryChanged("")
        viewModel.onExerciseEquipmentFilterChanged("machine")

        assertEquals(
            listOf("Chest Supported Row"),
            viewModel.state.value.visibleExercises.map { it.name },
        )

        viewModel.onExerciseMuscleFilterChanged("chest")

        assertTrue(viewModel.state.value.visibleExercises.isEmpty())

        viewModel.clearExerciseFilters()
        viewModel.onExerciseSearchQueryChanged("lats")

        assertEquals(
            listOf("Chest Supported Row"),
            viewModel.state.value.visibleExercises.map { it.name },
        )

        viewModel.onExerciseSearchQueryChanged("")
        viewModel.onExerciseMuscleFilterChanged("lats")

        assertEquals(
            listOf("Chest Supported Row"),
            viewModel.state.value.visibleExercises.map { it.name },
        )
    }

    @Test
    fun saveCustomExercise_persistsInputAndClearsEditor() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openCustomExerciseEditor()
        viewModel.onCustomExerciseNameChanged("  Landmine Press  ")
        viewModel.onCustomExerciseCategoryChanged(" strength ")
        viewModel.onCustomExerciseEquipmentChanged(" landmine ")
        viewModel.onCustomExerciseTargetMusclesChanged(" shoulders, triceps ")
        viewModel.saveCustomExercise()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            ExerciseInput(
                name = "Landmine Press",
                category = "strength",
                equipment = "landmine",
                targetMuscles = "shoulders, triceps",
            ),
            repository.createdExerciseInput,
        )
        assertFalse(viewModel.state.value.exerciseEditor.isOpen)
        assertEquals("", viewModel.state.value.exerciseEditor.name)
        // Since Turn 5 the editor opens in place (picker sheet) — saving must not switch sections.
        assertEquals(TrainingSection.Routines, viewModel.state.value.selectedSection)
    }

    @Test
    fun exerciseDetail_loadsMetadataAndSavesLocalNotes() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.selectSection(TrainingSection.Exercises)
        viewModel.openExerciseDetail("exercise-bench-press")
        dispatcher.scheduler.advanceUntilIdle()

        val detail = viewModel.state.value.selectedExerciseDetail
        assertEquals("Barbell Bench Press", detail?.name)
        assertEquals("chest", detail?.primaryMuscles)
        assertEquals("triceps, shoulders", detail?.secondaryMuscles)
        assertEquals("Existing setup note", viewModel.state.value.exerciseDetailNotesInput)

        viewModel.onExerciseDetailNotesChanged("  Use comp grip  ")
        viewModel.saveExerciseDetailNotes()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("exercise-bench-press", repository.updatedExerciseNotesId)
        assertEquals("Use comp grip", repository.updatedExerciseNotes)
        assertEquals("Use comp grip", viewModel.state.value.selectedExerciseDetail?.localNotes)
        assertEquals("Use comp grip", viewModel.state.value.exerciseDetailNotesInput)

        viewModel.closeExerciseDetail()

        assertEquals(null, viewModel.state.value.selectedExerciseDetail)
        assertEquals("", viewModel.state.value.exerciseDetailNotesInput)
    }

    @Test
    fun openRoutineEditor_forStarterRoutineLoadsAndCanSaveChanges() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        observeTraining(viewModel)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openRoutineEditor("routine-full-body-a")
        dispatcher.scheduler.advanceUntilIdle()

        val editor = viewModel.state.value.routineEditor
        assertTrue(editor.isOpen)
        assertEquals("Full Body A", editor.name)
        assertEquals("Starter Pack", editor.folderName)
        assertEquals(listOf("exercise-bench-press"), editor.exercises.map { it.exerciseId })

        viewModel.onRoutineNameChanged("Full Body A edited")
        viewModel.addRoutineExercises(listOf("exercise-row"))
        viewModel.removeRoutineExercise(index = 0)
        viewModel.saveRoutineEditor()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("routine-full-body-a", repository.updatedRoutineId)
        assertEquals(
            RoutineInput(
                name = "Full Body A edited",
                notes = "Starter routine",
                folderId = "folder-starter-pack",
                folderName = "Starter Pack",
                exercises = listOf(
                    RoutineExerciseInput(
                        exerciseId = "exercise-row",
                        targetSets = 3,
                        targetReps = "8",
                        restSeconds = 120,
                        setPlans = listOf(
                            RoutineSetInput(setType = "working", targetReps = "8"),
                            RoutineSetInput(setType = "working", targetReps = "8"),
                            RoutineSetInput(setType = "working", targetReps = "8"),
                        ),
                    ),
                ),
            ),
            repository.updatedRoutineInput,
        )
        assertFalse(viewModel.state.value.routineEditor.isOpen)
    }

    @Test
    fun routineDetail_opensFromRow_thenEditAndStartBothCloseIt() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openRoutineDetail("routine-upper-a")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("routine-upper-a", viewModel.state.value.selectedRoutineDetail?.id)
        // Opening a routine keeps the user in their current section (Home) rather than
        // yanking them to the pre-made routine Library.
        assertEquals("Routines", viewModel.state.value.selectedSection.name)
        assertTrue(repository.requestedRoutineDetailIds.contains("routine-upper-a"))

        // Editing from the detail layers the editor above it; the detail stays loaded
        // beneath so back returns to it.
        viewModel.openRoutineEditor("routine-upper-a")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("routine-upper-a", viewModel.state.value.selectedRoutineDetail?.id)
        assertTrue(viewModel.state.value.routineEditor.isOpen)

        // Starting the workout from the detail keeps the detail beneath the workout page.
        viewModel.closeRoutineEditor()
        viewModel.openRoutineDetail("routine-upper-a")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("routine-upper-a", viewModel.state.value.selectedRoutineDetail?.id)

        viewModel.startRoutine("routine-upper-a")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("routine-upper-a", viewModel.state.value.selectedRoutineDetail?.id)
        assertEquals("routine-upper-a", repository.startedRoutineId)
    }

    @Test
    fun resumeActiveWorkout_leavesRoomOwnedContentUnchanged() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())

        val before = viewModel.state.value.activeWorkout
        viewModel.resumeActiveWorkout()
        assertEquals(before, viewModel.state.value.activeWorkout)
    }

    @Test
    fun openRoutineEditor_forExistingRoutineLoadsRoutineIntoEditor() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())

        viewModel.openRoutineEditor("routine-upper-a")
        dispatcher.scheduler.advanceUntilIdle()

        val editor = viewModel.state.value.routineEditor
        assertTrue(editor.isOpen)
        assertEquals("routine-upper-a", editor.routineId)
        assertEquals("Upper A", editor.name)
        assertEquals("Custom routine", editor.notes)
        assertEquals("PPL System", editor.folderName)
        assertEquals(1, editor.exercises.size)
        assertEquals("exercise-bench-press", editor.exercises.single().exerciseId)
    }

    @Test
    fun routineFolders_showConfigurableFoldersAndDoNotFilterVisibleRoutinesByProgram() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        observeTraining(viewModel)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("Starter Pack", "PPL System"), viewModel.state.value.routineFolders.map { it.name })
        // Library browses pre-made (starter) routines only; the Home tab owns user-created ones.
        assertEquals(listOf("Full Body A"), viewModel.state.value.visibleRoutines.map { it.name })
        assertTrue(viewModel.state.value.visibleRoutines.all { it.isStarter })
        assertEquals(listOf("Upper A"), viewModel.state.value.homeRoutines.map { it.name })
        assertTrue(viewModel.state.value.homeRoutines.none { it.isStarter })
        // "Starter Pack" only holds the pre-made routine, so it is hidden from Home; the user's own
        // "PPL System" folder stays.
        assertEquals(listOf("PPL System"), viewModel.state.value.homeFolders.map { it.name })

        viewModel.openRoutineFolderEditor(null)
        viewModel.onRoutineFolderNameChanged("  Powerbuilding  ")
        viewModel.saveRoutineFolderEditor()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Powerbuilding", repository.createdFolderName)
        assertFalse(viewModel.state.value.routineFolderEditor.isOpen)

        viewModel.openRoutineFolderEditor("folder-ppl")
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onRoutineFolderNameChanged("PPL System x4")
        viewModel.saveRoutineFolderEditor()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("folder-ppl" to "PPL System x4", repository.updatedFolder)
    }

    @Test
    fun assignRoutineToFolder_delegatesKnownFolderAndUnassignedTargets() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.routinesLibraryState.collect { }
        }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.assignRoutineToFolder("routine-upper-a", "folder-starter-pack")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("routine-upper-a" to "folder-starter-pack", repository.assignedRoutineFolder)
        assertEquals("Starter Pack", viewModel.state.value.homeRoutines.single { it.id == "routine-upper-a" }.folderName)
        assertEquals("Upper A moved to Starter Pack.", viewModel.state.value.message)

        viewModel.assignRoutineToFolder("routine-upper-a", null)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("routine-upper-a" to null, repository.assignedRoutineFolder)
        assertEquals(null, viewModel.state.value.homeRoutines.single { it.id == "routine-upper-a" }.folderId)
        assertEquals("Upper A moved to My routines.", viewModel.state.value.message)
    }

    @Test
    fun assignRoutineToFolder_ignoresUnknownFolderAndNoopDrops() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.assignRoutineToFolder("routine-upper-a", "missing-folder")
        viewModel.assignRoutineToFolder("routine-upper-a", "folder-ppl")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(null, repository.assignedRoutineFolder)
    }

    @Test
    fun dashboard_suggestsRoutineRecentWorkoutAndQuickStarts() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        observeTraining(viewModel)
        dispatcher.scheduler.advanceUntilIdle()

        val initialDashboard = viewModel.state.value.dashboard
        assertEquals("Upper A", initialDashboard.nextSuggestedRoutine?.name)
        assertEquals(listOf("Upper A"), initialDashboard.quickStartRoutines.map { it.name })
        assertEquals("Push", initialDashboard.recentWorkout?.title)
    }

    @Test
    fun routineLibraryPage_opensFromHomeWithoutSwitchingToExerciseLibrary() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openRoutineLibraryPage()

        assertEquals(TrainingSection.Routines, viewModel.state.value.selectedSection)

        viewModel.closeRoutineLibraryPage()

        assertEquals(null, viewModel.state.value.selectedRoutineDetail)
    }

    @Test
    fun deleteRoutine_forStarterRoutineDoesNotDelete() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        observeTraining(viewModel)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteRoutine("routine-full-body-a")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(repository.deletedRoutineIds.isEmpty())
    }

    @Test
    fun saveRoutineEditor_forNewRoutinePersistsAndClosesEditor() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())

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
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        observeTraining(viewModel)

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
                        restSeconds = 120,
                        setPlans = listOf(
                            RoutineSetInput(setType = "working", targetReps = "8"),
                            RoutineSetInput(setType = "working", targetReps = "8"),
                            RoutineSetInput(setType = "working", targetReps = "8"),
                            RoutineSetInput(setType = "working", targetReps = "8"),
                        ),
                    ),
                ),
            ),
            repository.createdRoutineInput,
        )
    }

    @Test
    fun routineExercisePicker_addsSelectedExercisesOnlyAfterConfirm() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        observeTraining(viewModel)

        viewModel.openRoutineEditor(null)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openRoutineExercisePicker()
        viewModel.toggleRoutineExercisePickerSelection("exercise-bench-press")

        assertTrue(viewModel.state.value.routineExercisePickerSelectedIds.contains("exercise-bench-press"))
        assertTrue(viewModel.state.value.routineEditor.exercises.isEmpty())

        viewModel.confirmRoutineExercisePicker()

        assertEquals(listOf("exercise-bench-press"), viewModel.state.value.routineEditor.exercises.map { it.exerciseId })
    }

    @Test
    fun routineEditor_updatesExerciseRestAndSetPlanRowsBeforeSave() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        observeTraining(viewModel)

        viewModel.openRoutineEditor(null)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onRoutineNameChanged("Push")
        viewModel.addRoutineExercise("exercise-bench-press")
        viewModel.onRoutineExerciseRestSecondsChanged(index = 0, value = "210")
        viewModel.onRoutineExerciseSetTypeChanged(exerciseIndex = 0, setIndex = 0, setType = "warmup")
        viewModel.onRoutineExerciseSetRepsChanged(exerciseIndex = 0, setIndex = 0, value = "15")
        viewModel.onRoutineExerciseSetWeightChanged(exerciseIndex = 0, setIndex = 0, value = "40")
        viewModel.addRoutineExerciseSet(index = 0)
        viewModel.onRoutineExerciseSetTypeChanged(exerciseIndex = 0, setIndex = 3, setType = "drop")
        viewModel.onRoutineExerciseSetRepsChanged(exerciseIndex = 0, setIndex = 3, value = "12")
        viewModel.saveRoutineEditor()
        dispatcher.scheduler.advanceUntilIdle()

        val exercise = repository.createdRoutineInput?.exercises?.single()
        assertEquals(210, exercise?.restSeconds)
        assertEquals(listOf("warmup", "working", "working", "drop"), exercise?.setPlans?.map { it.setType })
        assertEquals(listOf("15", "8", "8", "12"), exercise?.setPlans?.map { it.targetReps })
        assertEquals(40.0, exercise?.setPlans?.first()?.targetWeightKg ?: 0.0, 0.01)
    }

    @Test
    fun routineEditor_reordersExercisesBeforeSave() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        observeTraining(viewModel)

        viewModel.openRoutineEditor(null)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onRoutineNameChanged("Upper")
        viewModel.addRoutineExercise("exercise-bench-press")
        viewModel.addRoutineExercise("exercise-row")
        viewModel.moveRoutineExerciseUp(index = 1)
        viewModel.saveRoutineEditor()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            listOf("exercise-row", "exercise-bench-press"),
            repository.createdRoutineInput?.exercises?.map { it.exerciseId },
        )
    }

    @Test
    fun startRoutine_startsRoomOwnedWorkout() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        var destinationOpened = false

        viewModel.startRoutine("routine-full-body-a") { destinationOpened = true }
        assertFalse(destinationOpened)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("routine-full-body-a", repository.startedRoutineId)
        assertTrue(destinationOpened)
    }

    @Test
    fun missingTypedRoutineDestination_requestsPopAfterLookup() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        var destinationPopped = false

        viewModel.openRoutineDetail("missing-routine") { destinationPopped = true }
        assertFalse(destinationPopped)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(destinationPopped)
        assertEquals("Routine not found.", viewModel.state.value.message)
    }

    @Test
    fun missingTypedRoutineEditorDestination_requestsPopWithoutOpeningEditor() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        var destinationPopped = false

        viewModel.openRoutineEditor("missing-routine") { destinationPopped = true }
        assertFalse(destinationPopped)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(destinationPopped)
        assertFalse(viewModel.state.value.routineEditor.isOpen)
        assertEquals("Routine not found.", viewModel.state.value.message)
    }

    @Test
    fun startBlankWorkout_startsRoomOwnedWorkout() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())

        viewModel.startBlankWorkout()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.startBlankWorkoutCalls)
    }

    @Test
    fun closeActiveWorkoutRoute_returnsToTrainingHome() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())

        viewModel.resumeActiveWorkout()
        viewModel.closeActiveWorkoutRoute()

        assertEquals("Routines", viewModel.state.value.selectedSection.name)
    }

    @Test
    fun saveRoutineEditor_openedOverDetail_returnsToRefreshedDetail() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openRoutineDetail("routine-upper-a")
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.openRoutineEditor("routine-upper-a")
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onRoutineNameChanged("Upper A v2")
        var destinationPopped = false
        viewModel.saveRoutineEditor { destinationPopped = true }
        assertFalse(destinationPopped)
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.routineEditor.isOpen)
        assertEquals("Upper A v2", viewModel.state.value.selectedRoutineDetail?.name)
        assertTrue(destinationPopped)
    }

    @Test
    fun finishActiveWorkout_loadsSummaryContentForHistoryDestination() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        observeTraining(viewModel)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.resumeActiveWorkout()
        var completedSessionId: String? = null
        viewModel.finishActiveWorkout { completedSessionId = it }
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("session-1", viewModel.state.value.selectedWorkoutDetail?.summary?.sessionId)
        assertEquals("session-1", completedSessionId)
        viewModel.closeWorkoutDetail()
        assertEquals(null, viewModel.state.value.selectedWorkoutDetail)
        assertEquals(TrainingSection.History, viewModel.state.value.selectedSection)
    }

    @Test
    fun finishAndDiscardRequests_toggleConfirmationState() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        observeTraining(viewModel)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.requestFinishActiveWorkout()
        assertTrue(viewModel.state.value.finishConfirmationOpen)

        viewModel.cancelFinishActiveWorkout()
        assertFalse(viewModel.state.value.finishConfirmationOpen)

        viewModel.requestDiscardActiveWorkout()
        assertTrue(viewModel.state.value.discardConfirmationOpen)

        viewModel.cancelDiscardActiveWorkout()
        assertFalse(viewModel.state.value.discardConfirmationOpen)
    }

    @Test
    fun finishActiveWorkout_opensCompletedWorkoutDetailInHistoryAndClearsRestTimer() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        observeTraining(viewModel)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.resumeActiveWorkout()
        viewModel.toggleWorkoutSetCompletion("set-1", completed = true)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.requestFinishActiveWorkout()
        viewModel.finishActiveWorkout()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("session-1", repository.finishedSessionId)
        assertFalse(viewModel.state.value.finishConfirmationOpen)
        assertFalse(viewModel.state.value.restTimer.isVisible)
        assertEquals(TrainingSection.History, viewModel.state.value.selectedSection)
        assertEquals(listOf("session-1"), repository.openedWorkoutDetailSessionIds)
        assertEquals("session-1", viewModel.state.value.selectedWorkoutDetail?.summary?.sessionId)
    }

    @Test
    fun discardActiveWorkout_discardsAndReturnsToHomeAndClearsRestTimer() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        observeTraining(viewModel)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.resumeActiveWorkout()
        viewModel.toggleWorkoutSetCompletion("set-1", completed = true)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.requestDiscardActiveWorkout()
        var destinationReset = false
        viewModel.discardActiveWorkout { destinationReset = true }
        assertFalse(destinationReset)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("session-1", repository.discardedSessionId)
        assertFalse(viewModel.state.value.discardConfirmationOpen)
        assertFalse(viewModel.state.value.restTimer.isVisible)
        assertEquals("Routines", viewModel.state.value.selectedSection.name)
        assertEquals(null, viewModel.state.value.selectedWorkoutDetail)
        assertTrue(destinationReset)
    }

    @Test
    fun completeSet_updatesSummaryAndStartsRunningRestTimer() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        observeTraining(viewModel)
        dispatcher.scheduler.advanceUntilIdle()

        repository.activeWorkoutDetail.value = ActiveWorkoutDetail(
            sessionId = "session-1",
            title = "Push",
            startedAtEpochMillis = 1_000L,
            completedSetCount = 0,
            totalVolumeKg = 0.0,
            exerciseBlocks = listOf(
                WorkoutExerciseBlock(
                    exercise = repository.exercisesFlow.value.first(),
                    targetReps = "5",
                    sets = listOf(
                        LoggedWorkoutSetDetail(
                            id = "set-1",
                            exerciseId = "exercise-bench-press",
                            setType = "working",
                            reps = 5,
                            weightKg = 100.0,
                            rpe = 8.0,
                            notes = null,
                            completed = false,
                            previousLabel = "95 kg x 5",
                        ),
                    ),
                ),
            ),
        )
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.resumeActiveWorkout()
        viewModel.toggleWorkoutSetCompletion("set-1", completed = true)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("set-1", repository.updatedSetId)
        assertEquals(true, repository.updatedSetInput?.completed)
        assertEquals(true, viewModel.state.value.restTimer.isVisible)
        assertEquals(true, viewModel.state.value.restTimer.isRunning)
        assertEquals("set-1", viewModel.state.value.restTimer.sourceSetId)
        assertEquals(120, viewModel.state.value.restTimer.durationSeconds)
        assertEquals(120, viewModel.state.value.restTimer.remainingSeconds)
    }

    @Test
    fun completeSet_usesRoutineSetRestSecondsBeforeGlobalDefault() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.activeHistoryState.collect { }
        }
        dispatcher.scheduler.advanceUntilIdle()

        repository.activeWorkoutDetail.value = ActiveWorkoutDetail(
            sessionId = "session-1",
            title = "Push",
            startedAtEpochMillis = 1_000L,
            completedSetCount = 0,
            totalVolumeKg = 0.0,
            exerciseBlocks = listOf(
                WorkoutExerciseBlock(
                    exercise = repository.exercisesFlow.value.first(),
                    targetReps = "5",
                    sets = listOf(
                        LoggedWorkoutSetDetail(
                            id = "set-1",
                            exerciseId = "exercise-bench-press",
                            setType = "working",
                            reps = 5,
                            weightKg = 100.0,
                            rpe = 8.0,
                            notes = null,
                            completed = false,
                            previousLabel = "95 kg x 5",
                            restSeconds = 210,
                        ),
                    ),
                ),
            ),
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.resumeActiveWorkout()
        viewModel.toggleWorkoutSetCompletion("set-1", completed = true)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(210, viewModel.state.value.restTimer.durationSeconds)
        assertEquals(210, viewModel.state.value.restTimer.remainingSeconds)
    }

    @Test
    fun restTimerSettings_saveAndApplyRestDefaultToNextCompletedSet() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        observeTraining(viewModel)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("120", viewModel.state.value.restTimerDefaultSecondsInput)
        assertEquals("20", viewModel.state.value.plateBarWeightInput)
        assertEquals("25, 20, 15, 10, 5, 2.5, 1.25", viewModel.state.value.availablePlatesInput)

        viewModel.onRestTimerDefaultSecondsChanged("90")
        viewModel.onPlateBarWeightChanged("15")
        viewModel.onAvailablePlatesChanged("20, 10, 5, 2.5, 2.5")
        viewModel.saveRestTimerSettings()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            TrainingSettingsInput(
                defaultRestSeconds = 90,
                barWeightKg = 20.0,
                availablePlatesKg = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25),
            ),
            repository.updatedTrainingSettings,
        )
        assertEquals("90", viewModel.state.value.restTimerDefaultSecondsInput)
        assertEquals("20", viewModel.state.value.plateBarWeightInput)
        assertEquals("25, 20, 15, 10, 5, 2.5, 1.25", viewModel.state.value.availablePlatesInput)
        assertEquals("Rest timer saved.", viewModel.state.value.message)

        viewModel.resumeActiveWorkout()
        viewModel.toggleWorkoutSetCompletion("set-1", completed = true)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(90, viewModel.state.value.restTimer.durationSeconds)
        assertEquals(90, viewModel.state.value.restTimer.remainingSeconds)
    }

    @Test
    fun addSuggestedWarmupSet_addsWarmupSetWithSuggestedValues() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        observeTraining(viewModel)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.resumeActiveWorkout()
        viewModel.addSuggestedWarmupSet("exercise-bench-press", reps = 8, weightKg = 50.0)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("session-1", repository.addedSetSessionId)
        assertEquals("exercise-bench-press", repository.addedSetExerciseId)
        assertEquals(
            WorkoutSetInputData(
                setType = "warmup",
                reps = 8,
                weightKg = 50.0,
                rpe = null,
                notes = null,
                completed = false,
            ),
            repository.addedSetInput,
        )
    }

    @Test
    fun makeSupersetWithNext_pairsWithNextStandalone_andDissolveDelegates() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.activeHistoryState.collect { }
        }
        dispatcher.scheduler.advanceUntilIdle()

        fun block(id: String) = WorkoutExerciseBlock(
            exercise = ExerciseSummary(
                id = id,
                name = id,
                category = "strength",
                equipment = null,
                targetMuscles = "x",
                isCustom = false,
            ),
            targetReps = null,
            sets = emptyList(),
        )
        val blockA = block("ex-a")
        val blockB = block("ex-b")
        repository.activeWorkoutDetail.value = ActiveWorkoutDetail(
            sessionId = "session-1",
            title = "Push",
            startedAtEpochMillis = 1_000L,
            completedSetCount = 0,
            totalVolumeKg = 0.0,
            exerciseBlocks = listOf(blockA, blockB),
            exerciseGroupings = listOf(ExerciseGrouping.Single(blockA), ExerciseGrouping.Single(blockB)),
        )
        dispatcher.scheduler.advanceUntilIdle()

        // The last standalone block has no "next" -> no-op.
        viewModel.makeSupersetWithNext("ex-b")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(null, repository.createSupersetArgs)

        viewModel.makeSupersetWithNext("ex-a")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(Triple("session-1", "ex-a", "ex-b"), repository.createSupersetArgs)

        viewModel.dissolveSuperset("grp-1")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("session-1" to "grp-1", repository.dissolveSupersetArgs)
    }

    @Test
    fun restTimerControls_tickPauseResumeAdjustAndSkip() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        observeTraining(viewModel)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.resumeActiveWorkout()
        viewModel.toggleWorkoutSetCompletion("set-1", completed = true)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.tickRestTimer()
        assertEquals(119, viewModel.state.value.restTimer.remainingSeconds)

        viewModel.pauseRestTimer()
        viewModel.tickRestTimer()
        assertEquals(false, viewModel.state.value.restTimer.isRunning)
        assertEquals(119, viewModel.state.value.restTimer.remainingSeconds)

        viewModel.resumeRestTimer()
        viewModel.tickRestTimer()
        assertEquals(true, viewModel.state.value.restTimer.isRunning)
        assertEquals(118, viewModel.state.value.restTimer.remainingSeconds)

        viewModel.adjustRestTimerSeconds(15)
        assertEquals(133, viewModel.state.value.restTimer.remainingSeconds)

        viewModel.skipRestTimer()
        assertFalse(viewModel.state.value.restTimer.isVisible)
        assertFalse(viewModel.state.value.restTimer.isRunning)
        assertEquals(0, viewModel.state.value.restTimer.remainingSeconds)
    }

    @Test
    fun restTimerSubtractPastZeroCompletesTimer() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.resumeActiveWorkout()
        viewModel.toggleWorkoutSetCompletion("set-1", completed = true)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.adjustRestTimerSeconds(-200)

        assertFalse(viewModel.state.value.restTimer.isVisible)
        assertFalse(viewModel.state.value.restTimer.isRunning)
        assertEquals(0, viewModel.state.value.restTimer.remainingSeconds)
    }

    @Test
    fun completingAnotherSet_restartsRestTimerFromDefaultDuration() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        observeTraining(viewModel)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.resumeActiveWorkout()
        viewModel.toggleWorkoutSetCompletion("set-1", completed = true)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.tickRestTimer()

        viewModel.toggleWorkoutSetCompletion("set-2", completed = true)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("set-2", viewModel.state.value.restTimer.sourceSetId)
        assertEquals(true, viewModel.state.value.restTimer.isVisible)
        assertEquals(true, viewModel.state.value.restTimer.isRunning)
        assertEquals(120, viewModel.state.value.restTimer.remainingSeconds)
    }

    @Test
    fun completeBlankActiveSet_doesNotPersistCompletionOrShowRestTimer() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        dispatcher.scheduler.advanceUntilIdle()

        repository.activeWorkoutDetail.value = ActiveWorkoutDetail(
            sessionId = "session-1",
            title = "Push",
            startedAtEpochMillis = 1_000L,
            completedSetCount = 0,
            totalVolumeKg = 0.0,
            exerciseBlocks = listOf(
                WorkoutExerciseBlock(
                    exercise = repository.exercisesFlow.value.first(),
                    targetReps = "5",
                    sets = listOf(
                        LoggedWorkoutSetDetail(
                            id = "set-blank",
                            exerciseId = "exercise-bench-press",
                            setType = "working",
                            reps = null,
                            weightKg = null,
                            rpe = null,
                            notes = null,
                            completed = false,
                            previousLabel = null,
                        ),
                    ),
                ),
            ),
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.resumeActiveWorkout()
        viewModel.toggleWorkoutSetCompletion("set-blank", completed = true)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(null, repository.updatedSetId)
        assertEquals(null, repository.updatedSetInput)
        assertFalse(viewModel.state.value.restTimer.isVisible)
    }

    @Test
    fun activeWorkoutActions_addDuplicateAndDeleteDelegateToRepository() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        observeTraining(viewModel)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.resumeActiveWorkout()
        viewModel.addExerciseToActiveWorkout("exercise-row")
        viewModel.addWorkoutSet("exercise-bench-press")
        viewModel.duplicateLastWorkoutSet("exercise-bench-press")
        viewModel.deleteWorkoutSet("set-2")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("session-1", repository.addedExerciseSessionId)
        assertEquals("exercise-row", repository.addedExerciseId)
        assertEquals("session-1", repository.addedSetSessionId)
        assertEquals("exercise-bench-press", repository.addedSetExerciseId)
        assertEquals(false, repository.addedSetInput?.completed)
        assertEquals("session-1", repository.duplicatedSessionId)
        assertEquals("exercise-bench-press", repository.duplicatedExerciseId)
        assertEquals("set-2", repository.deletedSetId)
    }

    @Test
    fun activeWorkoutNotesAndReorder_delegateToRepository() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        observeTraining(viewModel)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.resumeActiveWorkout()
        viewModel.onActiveWorkoutNotesChanged("  Keep rest strict  ")
        viewModel.saveActiveWorkoutNotes()
        viewModel.moveWorkoutSetUp("set-2")
        viewModel.moveWorkoutSetDown("set-1")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Keep rest strict", repository.updatedActiveWorkoutNotes)
        assertEquals(listOf("set-2" to -1, "set-1" to 1), repository.movedWorkoutSets)
        assertEquals("Keep rest strict", viewModel.state.value.activeWorkoutNotesInput)
    }

    @Test
    fun updateWorkoutSetFields_propagatesEditedValues() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        observeTraining(viewModel)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.resumeActiveWorkout()
        viewModel.updateWorkoutSetFields(
            setId = "set-1",
            setType = "warmup",
            reps = "8",
            weightKg = "60.5",
            rpe = "6.5",
            notes = "Smooth",
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("set-1", repository.updatedSetId)
        assertEquals("warmup", repository.updatedSetInput?.setType)
        assertEquals(8, repository.updatedSetInput?.reps)
        assertEquals(60.5, repository.updatedSetInput?.weightKg ?: 0.0, 0.01)
        assertEquals(6.5, repository.updatedSetInput?.rpe ?: 0.0, 0.01)
        assertEquals("Smooth", repository.updatedSetInput?.notes)
        assertEquals(false, repository.updatedSetInput?.completed)
    }

    @Test
    fun openAndCloseWorkoutDetail_updatesSelectedHistoryDetail() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.selectSection(TrainingSection.History)
        viewModel.openWorkoutDetail("session-history-1")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("session-history-1"), repository.openedWorkoutDetailSessionIds)
        assertEquals("session-history-1", viewModel.state.value.selectedWorkoutDetail?.summary?.sessionId)

        viewModel.closeWorkoutDetail()

        assertEquals(null, viewModel.state.value.selectedWorkoutDetail)
    }

    @Test
    fun buildTrainingHistoryOverview_summarizesWeekCalendarAndStreaks() {
        val today = LocalDate.of(2026, 6, 28)
        val overview = buildTrainingHistoryOverview(
            history = listOf(
                historySummary("mon", LocalDate.of(2026, 6, 22), sets = 3, volume = 300.0),
                historySummary("fri", LocalDate.of(2026, 6, 26), sets = 4, volume = 400.0),
                historySummary("sat", LocalDate.of(2026, 6, 27), sets = 5, volume = 500.0),
                historySummary("sun-a", today, sets = 6, volume = 600.0),
                historySummary("sun-b", today, sets = 2, volume = 200.0),
                historySummary("old", LocalDate.of(2026, 5, 31), sets = 8, volume = 800.0),
            ),
            today = today,
        )

        val todayCell = overview.calendarWeeks.flatten().filterNotNull().single { it.date == today }

        assertEquals("June 2026", overview.monthLabel)
        assertEquals(5, overview.currentWeekWorkoutCount)
        assertEquals(4, overview.currentWeekTrainingDayCount)
        assertEquals(20, overview.currentWeekCompletedSetCount)
        assertEquals(2000.0, overview.currentWeekVolumeKg, 0.01)
        assertEquals(3, overview.currentStreakDays)
        assertEquals(3, overview.bestStreakDays)
        assertEquals(2, todayCell.workoutCount)
        assertEquals(8, todayCell.completedSetCount)
        assertEquals(800.0, todayCell.totalVolumeKg, 0.01)
    }

    @Test
    fun historyOverview_updatesFromObservedHistory() = runTest {
        val repository = FakeTrainingRepository()
        val today = LocalDate.now()
        repository.setWorkoutHistory(
            listOf(
                historySummary("today", today, sets = 4, volume = 420.0),
            ),
        )

        val viewModel = TrainingViewModel(repository, FakeGoalsRepository())
        observeTraining(viewModel)
        dispatcher.scheduler.advanceUntilIdle()

        val overview = viewModel.state.value.historyOverview
        assertEquals(1, overview.currentWeekWorkoutCount)
        assertEquals(1, overview.currentWeekTrainingDayCount)
        assertEquals(4, overview.currentWeekCompletedSetCount)
        assertEquals(420.0, overview.currentWeekVolumeKg, 0.01)
    }

    @Test
    fun selectProgressExercise_loadsProgressForSelectedExercise() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingProgressViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect { }
        }
        testScheduler.runCurrent()

        viewModel.selectProgressExercise("exercise-bench-press")
        testScheduler.runCurrent()

        assertEquals("exercise-bench-press", repository.observedProgressExerciseIds.single())
        assertEquals("exercise-bench-press", viewModel.state.value.selectedProgressExerciseId)
        assertEquals("Barbell Bench Press", viewModel.state.value.selectedExerciseProgress?.exerciseName)
        assertEquals(120.0, viewModel.state.value.selectedExerciseProgress?.heaviestWeightKg ?: 0.0, 0.01)
    }

    @Test
    fun progressAnalytics_updatesFromRepository() = runTest {
        val repository = FakeTrainingRepository()
        repository.progressAnalyticsFlow.value = TrainingProgressAnalytics(
            muscleGroups = listOf(MuscleGroupProgress("chest", completedSetCount = 4, totalVolumeKg = 1200.0)),
            weeklyVolume = listOf(WeeklyTrainingVolume(weekStartEpochDay = 20_100L, workoutCount = 2, completedSetCount = 8, totalVolumeKg = 2400.0)),
        )

        val viewModel = TrainingProgressViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect { }
        }
        testScheduler.runCurrent()

        val analytics = viewModel.state.value.progressAnalytics
        assertEquals("chest", analytics.muscleGroups.single().muscle)
        assertEquals(4, analytics.muscleGroups.single().completedSetCount)
        assertEquals(2, analytics.weeklyVolume.single().workoutCount)
    }

    private fun TestScope.observeTraining(viewModel: TrainingViewModel): Job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        viewModel.routinesLibraryState.collect { }
    }.also {
        testScheduler.runCurrent()
    }

    private class FakeGoalsRepository(
        userGoals: UserGoals = UserGoals(),
    ) : GoalsRepository {
        val goals = MutableStateFlow(userGoals)

        override fun observeUserGoals(): Flow<UserGoals> = goals

        override suspend fun updateUserGoals(goals: UserGoals) = Unit
    }

    private class FakeTrainingRepository : TrainingRepository {
        var addCalls = 0
        var seedCalls = 0
        var savedExerciseName: String? = null
        var savedReps: Int? = null
        var savedWeightKg: Double? = null
        var updatedSetId: String? = null
        var updatedCompleted: Boolean? = null
        var updatedSetInput: WorkoutSetInputData? = null
        var createSupersetArgs: Triple<String, String, String>? = null
        var dissolveSupersetArgs: Pair<String, String>? = null
        var createdRoutineInput: RoutineInput? = null
        var createdExerciseInput: ExerciseInput? = null
        var updatedExerciseNotesId: String? = null
        var updatedExerciseNotes: String? = null
        var updatedRoutineId: String? = null
        var updatedRoutineInput: RoutineInput? = null
        var createdFolderName: String? = null
        var updatedFolder: Pair<String, String>? = null
        var deletedFolderId: String? = null
        var assignedRoutineFolder: Pair<String, String?>? = null
        var startedRoutineId: String? = null
        var startBlankWorkoutCalls = 0
        var addedExerciseSessionId: String? = null
        var addedExerciseId: String? = null
        var addedSetSessionId: String? = null
        var addedSetExerciseId: String? = null
        var addedSetInput: WorkoutSetInputData? = null
        var duplicatedSessionId: String? = null
        var duplicatedExerciseId: String? = null
        var deletedSetId: String? = null
        var updatedActiveWorkoutNotes: String? = null
        val movedWorkoutSets = mutableListOf<Pair<String, Int>>()
        var updatedTrainingSettings: TrainingSettingsInput? = null
        var finishedSessionId: String? = null
        var discardedSessionId: String? = null
        val openedWorkoutDetailSessionIds = mutableListOf<String>()
        val observedProgressExerciseIds = mutableListOf<String>()
        val requestedRoutineDetailIds = mutableListOf<String>()
        val workoutDetailLoadGates = mutableMapOf<String, CompletableDeferred<Unit>>()
        val routineDetailLoadGates = mutableMapOf<String, CompletableDeferred<Unit>>()
        val exerciseDetailLoadGates = mutableMapOf<String, CompletableDeferred<Unit>>()
        val deletedRoutineIds = mutableListOf<String>()
        var activeRoutineSummaryCollectors = 0
        var maxRoutineSummaryCollectors = 0
        var routineSummarySubscriptionStarts = 0
        var activeProgressExerciseListCollectors = 0
        var maxProgressExerciseListCollectors = 0
        var activeSelectedProgressCollectors = 0
        private var routineSummaryObservationOverride: Flow<List<RoutineSummary>>? = null
        private var progressObservationFactory: ((String) -> Flow<ExerciseProgress?>)? = null

        private val routinesFlow = MutableStateFlow(
            listOf(
                RoutineSummary(
                    id = "routine-full-body-a",
                    name = "Full Body A",
                    notes = "Starter routine",
                    exerciseCount = 5,
                    targetSetCount = 15,
                    isStarter = true,
                    programName = "Full Body",
                    tags = listOf("beginner", "strength"),
                    folderId = "folder-starter-pack",
                    folderName = "Starter Pack",
                ),
                RoutineSummary(
                    id = "routine-upper-a",
                    name = "Upper A",
                    notes = "Custom routine",
                    exerciseCount = 1,
                    targetSetCount = 3,
                    isStarter = false,
                    programName = "Upper Lower",
                    tags = listOf("upper"),
                    folderId = "folder-ppl",
                    folderName = "PPL System",
                ),
            ),
        )

        private val routineFoldersFlow = MutableStateFlow(
            listOf(
                RoutineFolder(id = "folder-starter-pack", name = "Starter Pack", sortOrder = 0),
                RoutineFolder(id = "folder-ppl", name = "PPL System", sortOrder = 1),
            ),
        )

        val exercisesFlow = MutableStateFlow(
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
                    primaryMuscles = "lats",
                    secondaryMuscles = "upper back, biceps",
                ),
            ),
        )

        private val exerciseDetails = mutableMapOf(
            "exercise-bench-press" to
                ExerciseDetail(
                    id = "exercise-bench-press",
                    name = "Barbell Bench Press",
                    category = "strength",
                    equipment = "barbell",
                    targetMuscles = "chest,triceps,shoulders",
                    primaryMuscles = "chest",
                    secondaryMuscles = "triceps, shoulders",
                    instructions = "Brace, lower with control, and press evenly.",
                    localNotes = "Existing setup note",
                    isCustom = false,
                ),
            "exercise-row" to
                ExerciseDetail(
                    id = "exercise-row",
                    name = "Chest Supported Row",
                    category = "strength",
                    equipment = "machine",
                    targetMuscles = "back,biceps",
                    primaryMuscles = "back",
                    secondaryMuscles = "biceps",
                    instructions = "Pull elbows back without shrugging.",
                    localNotes = null,
                    isCustom = false,
                ),
        )

        private val workoutHistoryFlow = MutableStateFlow(
            listOf(
                WorkoutHistorySummary(
                    sessionId = "session-history-1",
                    title = "Push",
                    startedAtEpochMillis = 1_000L,
                    endedAtEpochMillis = 2_000L,
                    completedSetCount = 2,
                    totalVolumeKg = 800.0,
                ),
            ),
        )

        private val activeWorkoutFlow = MutableStateFlow<ActiveWorkoutSummary?>(null)
        private val progressFlow = MutableStateFlow<ExerciseProgress?>(null)
        private val trainingSettingsFlow = MutableStateFlow(TrainingSettings())
        val progressAnalyticsFlow = MutableStateFlow(TrainingProgressAnalytics())
        private val recentPersonalRecordsFlow = MutableStateFlow<List<TrainingPrRecord>>(emptyList())
        private val loggedExerciseIdsFlow = MutableStateFlow<Set<String>>(emptySet())

        val activeWorkoutDetail = MutableStateFlow<ActiveWorkoutDetail?>(
            ActiveWorkoutDetail(
                sessionId = "session-1",
                title = "Push",
                startedAtEpochMillis = 1_000L,
                completedSetCount = 0,
                totalVolumeKg = 0.0,
                exerciseBlocks = listOf(
                    WorkoutExerciseBlock(
                        exercise = exercisesFlow.value.first(),
                        targetReps = "5",
                        sets = listOf(
                            LoggedWorkoutSetDetail(
                                id = "set-1",
                                exerciseId = "exercise-bench-press",
                                setType = "working",
                                reps = 5,
                                weightKg = 100.0,
                                rpe = 8.0,
                                notes = null,
                                completed = false,
                                previousLabel = "95 kg x 5",
                            ),
                            LoggedWorkoutSetDetail(
                                id = "set-2",
                                exerciseId = "exercise-bench-press",
                                setType = "working",
                                reps = 5,
                                weightKg = 100.0,
                                rpe = null,
                                notes = null,
                                completed = false,
                                previousLabel = "95 kg x 5",
                            ),
                        ),
                    ),
                ),
            ),
        )

        init {
            progressFlow.value =
                ExerciseProgress(
                    exerciseId = "exercise-bench-press",
                    exerciseName = "Barbell Bench Press",
                    equipment = "barbell",
                    targetMuscles = "chest,triceps,shoulders",
                    heaviestWeightKg = 120.0,
                    maxReps = 8,
                    bestEstimatedOneRepMaxKg = 132.0,
                    bestWorkoutVolumeKg = 1500.0,
                    trend = listOf(
                        TrainingTrendPoint(
                            dateEpochDay = 20_000L,
                            volumeKg = 1500.0,
                            bestEstimatedOneRepMaxKg = 132.0,
                        ),
                    ),
                )
        }

        private val workoutHistoryDetails = mutableMapOf(
            "session-history-1" to
                WorkoutHistoryDetail(
                    summary = workoutHistoryFlow.value.single(),
                    exerciseBlocks = activeWorkoutDetail.value?.exerciseBlocks.orEmpty(),
                ),
            "session-history-2" to
                WorkoutHistoryDetail(
                    summary = WorkoutHistorySummary(
                        sessionId = "session-history-2",
                        title = "Pull",
                        startedAtEpochMillis = 3_000L,
                        endedAtEpochMillis = 4_000L,
                        completedSetCount = 3,
                        totalVolumeKg = 900.0,
                    ),
                    exerciseBlocks = activeWorkoutDetail.value?.exerciseBlocks.orEmpty(),
                ),
        )

        fun setWorkoutHistory(history: List<WorkoutHistorySummary>) {
            workoutHistoryFlow.value = history
        }

        fun renameFirstRoutine(name: String) {
            routinesFlow.value = routinesFlow.value.mapIndexed { index, routine ->
                if (index == 0) routine.copy(name = name) else routine
            }
        }

        fun renameFirstExercise(name: String) {
            exercisesFlow.value = exercisesFlow.value.mapIndexed { index, exercise ->
                if (index == 0) exercise.copy(name = name) else exercise
            }
        }

        fun failRoutineSummariesAfterCurrentValue() {
            routineSummaryObservationOverride = flow {
                emit(routinesFlow.value)
                throw IllegalStateException("routine observation failed")
            }
        }

        fun useSingleEmissionProgressObservation() {
            var emitted = false
            progressObservationFactory = {
                flow {
                    if (!emitted) {
                        emitted = true
                        emit(progressFlow.value)
                    }
                    awaitCancellation()
                }
            }
        }

        private val routineDetails = mutableMapOf(
            "routine-full-body-a" to
                RoutineDetail(
                    id = "routine-full-body-a",
                    name = "Full Body A",
                    notes = "Starter routine",
                    isStarter = true,
                    programName = "Full Body",
                    tags = listOf("beginner", "strength"),
                    folderId = "folder-starter-pack",
                    folderName = "Starter Pack",
                    exercises = listOf(
                        RoutineExerciseDetail(
                            id = "routine-exercise-1",
                            exercise = exercisesFlow.value.first { it.id == "exercise-bench-press" },
                            sortOrder = 0,
                            targetSets = 3,
                            targetReps = "5",
                            restSeconds = 180,
                            setPlans = listOf(
                                RoutineSetInput(setType = "working", targetReps = "5"),
                                RoutineSetInput(setType = "working", targetReps = "5"),
                                RoutineSetInput(setType = "working", targetReps = "5"),
                            ),
                        ),
                    ),
                ),
            "routine-upper-a" to
                RoutineDetail(
                    id = "routine-upper-a",
                    name = "Upper A",
                    notes = "Custom routine",
                    isStarter = false,
                    programName = "Upper Lower",
                    tags = listOf("upper"),
                    folderId = "folder-ppl",
                    folderName = "PPL System",
                    exercises = listOf(
                        RoutineExerciseDetail(
                            id = "routine-upper-a-exercise-1",
                            exercise = exercisesFlow.value.first { it.id == "exercise-bench-press" },
                            sortOrder = 0,
                            targetSets = 3,
                            targetReps = "6",
                            restSeconds = 210,
                            setPlans = listOf(
                                RoutineSetInput(setType = "warmup", targetReps = "10"),
                                RoutineSetInput(setType = "working", targetReps = "6"),
                                RoutineSetInput(setType = "working", targetReps = "6"),
                            ),
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
            .onStart {
                activeProgressExerciseListCollectors += 1
                maxProgressExerciseListCollectors = maxOf(
                    maxProgressExerciseListCollectors,
                    activeProgressExerciseListCollectors,
                )
            }
            .onCompletion { activeProgressExerciseListCollectors -= 1 }

        override fun observeRoutineSummaries(): Flow<List<RoutineSummary>> = (routineSummaryObservationOverride ?: routinesFlow)
            .onStart {
                activeRoutineSummaryCollectors += 1
                routineSummarySubscriptionStarts += 1
                maxRoutineSummaryCollectors = maxOf(
                    maxRoutineSummaryCollectors,
                    activeRoutineSummaryCollectors,
                )
            }
            .onCompletion { activeRoutineSummaryCollectors -= 1 }

        override fun observeRoutineFolders(): Flow<List<RoutineFolder>> = routineFoldersFlow

        override suspend fun createRoutineFolder(name: String): String {
            createdFolderName = name
            val folderId = "folder-created"
            routineFoldersFlow.value = routineFoldersFlow.value + RoutineFolder(
                id = folderId,
                name = name,
                sortOrder = routineFoldersFlow.value.size,
            )
            return folderId
        }

        override suspend fun updateRoutineFolder(folderId: String, name: String) {
            updatedFolder = folderId to name
            routineFoldersFlow.value = routineFoldersFlow.value.map { folder ->
                if (folder.id == folderId) folder.copy(name = name) else folder
            }
        }

        override suspend fun deleteRoutineFolder(folderId: String) {
            deletedFolderId = folderId
            routineFoldersFlow.value = routineFoldersFlow.value.filterNot { it.id == folderId }
        }

        override fun observeActiveWorkoutSummary(): Flow<ActiveWorkoutSummary?> = activeWorkoutFlow

        override fun observeActiveWorkoutDetail(): Flow<ActiveWorkoutDetail?> = activeWorkoutDetail

        override fun observeTrainingSettings(): Flow<TrainingSettings> = trainingSettingsFlow

        override fun observeWorkoutHistory(): Flow<List<WorkoutHistorySummary>> = workoutHistoryFlow

        override fun observeExerciseProgress(exerciseId: String): Flow<ExerciseProgress?> {
            observedProgressExerciseIds += exerciseId
            return (progressObservationFactory?.invoke(exerciseId) ?: progressFlow)
                .onStart { activeSelectedProgressCollectors += 1 }
                .onCompletion { activeSelectedProgressCollectors -= 1 }
        }

        override fun observeTrainingProgressAnalytics(): Flow<TrainingProgressAnalytics> = progressAnalyticsFlow

        override fun observeRecentPersonalRecords(): Flow<List<TrainingPrRecord>> = recentPersonalRecordsFlow

        override fun observeLoggedExerciseIds(): Flow<Set<String>> = loggedExerciseIdsFlow

        override suspend fun getWorkoutHistoryDetail(sessionId: String): WorkoutHistoryDetail? {
            openedWorkoutDetailSessionIds += sessionId
            awaitRouteLoadGate(workoutDetailLoadGates[sessionId])
            return workoutHistoryDetails[sessionId]
        }

        override fun observeDailyTrainingSummary(date: LocalDate): Flow<TrainingSummary> = flowOf(TrainingSummary())

        override suspend fun createRoutine(input: RoutineInput): String {
            createdRoutineInput = input
            return "new-routine-id"
        }

        override suspend fun createCustomExercise(input: ExerciseInput): String {
            createdExerciseInput = input
            exercisesFlow.value = exercisesFlow.value + ExerciseSummary(
                id = "custom-exercise-id",
                name = input.name,
                category = input.category,
                equipment = input.equipment,
                targetMuscles = input.targetMuscles,
                isCustom = true,
            )
            exerciseDetails["custom-exercise-id"] = ExerciseDetail(
                id = "custom-exercise-id",
                name = input.name,
                category = input.category,
                equipment = input.equipment,
                targetMuscles = input.targetMuscles,
                primaryMuscles = input.targetMuscles,
                secondaryMuscles = "",
                instructions = null,
                localNotes = null,
                isCustom = true,
            )
            return "custom-exercise-id"
        }

        override suspend fun getExerciseDetail(exerciseId: String): ExerciseDetail? {
            awaitRouteLoadGate(exerciseDetailLoadGates[exerciseId])
            return exerciseDetails[exerciseId]
        }

        override suspend fun updateExerciseLocalNotes(exerciseId: String, notes: String?) {
            updatedExerciseNotesId = exerciseId
            updatedExerciseNotes = notes
            exerciseDetails[exerciseId] = exerciseDetails.getValue(exerciseId).copy(localNotes = notes)
        }

        override suspend fun updateRoutine(routineId: String, input: RoutineInput) {
            updatedRoutineId = routineId
            updatedRoutineInput = input
            routineDetails[routineId]?.let { detail ->
                routineDetails[routineId] = detail.copy(name = input.name, notes = input.notes)
            }
        }

        override suspend fun duplicateRoutine(routineId: String): String = "$routineId-copy"

        override suspend fun getRoutineDetail(routineId: String): RoutineDetail? {
            requestedRoutineDetailIds += routineId
            awaitRouteLoadGate(routineDetailLoadGates[routineId])
            return routineDetails[routineId]
        }

        private suspend fun awaitRouteLoadGate(gate: CompletableDeferred<Unit>?) {
            if (gate == null) return
            // Deliberately ignore Job cancellation so the tests also prove the request-generation
            // guard rejects a repository that completes after cancellation was requested.
            suspendCoroutine<Unit> { continuation ->
                gate.invokeOnCompletion { continuation.resume(Unit) }
            }
        }

        override suspend fun assignRoutineToFolder(routineId: String, folderId: String?) {
            assignedRoutineFolder = routineId to folderId
            val folder = folderId?.let { id -> routineFoldersFlow.value.firstOrNull { it.id == id } }
            routinesFlow.value = routinesFlow.value.map { routine ->
                if (routine.id == routineId) {
                    routine.copy(folderId = folder?.id, folderName = folder?.name)
                } else {
                    routine
                }
            }
            routineDetails[routineId]?.let { detail ->
                routineDetails[routineId] = detail.copy(
                    folderId = folder?.id,
                    folderName = folder?.name,
                )
            }
        }

        override suspend fun startBlankWorkout(): String {
            startBlankWorkoutCalls += 1
            return "blank-session-id"
        }

        override suspend fun startWorkoutFromRoutine(routineId: String): String {
            startedRoutineId = routineId
            return "session-for-$routineId"
        }

        override suspend fun addExerciseToActiveWorkout(sessionId: String, exerciseId: String) {
            addedExerciseSessionId = sessionId
            addedExerciseId = exerciseId
        }

        override suspend fun addSetToExercise(
            sessionId: String,
            exerciseId: String,
            input: WorkoutSetInputData,
        ): String {
            addedSetSessionId = sessionId
            addedSetExerciseId = exerciseId
            addedSetInput = input
            return "new-set-id"
        }

        override suspend fun duplicateLastSet(sessionId: String, exerciseId: String): String? {
            duplicatedSessionId = sessionId
            duplicatedExerciseId = exerciseId
            return "duplicated-set-id"
        }

        override suspend fun updateWorkoutSet(setId: String, input: WorkoutSetInputData) {
            updatedSetId = setId
            updatedSetInput = input
        }

        override suspend fun deleteWorkoutSet(setId: String) {
            deletedSetId = setId
        }

        override suspend fun updateActiveWorkoutNotes(sessionId: String, notes: String?) {
            updatedActiveWorkoutNotes = notes
            activeWorkoutDetail.value = activeWorkoutDetail.value?.copy(notes = notes)
        }

        override suspend fun moveWorkoutSet(setId: String, direction: Int) {
            movedWorkoutSets += setId to direction
        }

        override suspend fun updateTrainingSettings(input: TrainingSettingsInput) {
            updatedTrainingSettings = input
            trainingSettingsFlow.value = TrainingSettings(
                defaultRestSeconds = input.defaultRestSeconds,
                barWeightKg = input.barWeightKg,
                availablePlatesKg = input.availablePlatesKg,
            )
        }

        override suspend fun createSuperset(sessionId: String, exerciseAId: String, exerciseBId: String): String? {
            createSupersetArgs = Triple(sessionId, exerciseAId, exerciseBId)
            return "grp-fake"
        }

        override suspend fun dissolveSuperset(sessionId: String, groupId: String) {
            dissolveSupersetArgs = sessionId to groupId
        }

        override suspend fun deleteRoutine(routineId: String) {
            deletedRoutineIds += routineId
        }

        override suspend fun finishWorkout(sessionId: String) {
            finishedSessionId = sessionId
            workoutHistoryDetails[sessionId] = WorkoutHistoryDetail(
                summary = WorkoutHistorySummary(
                    sessionId = sessionId,
                    title = activeWorkoutDetail.value?.title ?: "Workout",
                    startedAtEpochMillis = activeWorkoutDetail.value?.startedAtEpochMillis ?: 1_000L,
                    endedAtEpochMillis = 2_000L,
                    completedSetCount = activeWorkoutDetail.value?.completedSetCount ?: 0,
                    totalVolumeKg = activeWorkoutDetail.value?.totalVolumeKg ?: 0.0,
                ),
                exerciseBlocks = activeWorkoutDetail.value?.exerciseBlocks.orEmpty(),
            )
        }

        override suspend fun discardWorkout(sessionId: String) {
            discardedSessionId = sessionId
        }

        override suspend fun getLatestWorkoutForExport(): WorkoutForExport? = null

        override suspend fun seedStarterTrainingData() {
            seedCalls += 1
        }

        override suspend fun markWorkoutExported(
            sessionId: String,
            recordId: String,
            exportedAtEpochMillis: Long,
        ) = Unit
    }

    private fun historySummary(
        id: String,
        date: LocalDate,
        sets: Int,
        volume: Double,
    ): WorkoutHistorySummary = WorkoutHistorySummary(
        sessionId = id,
        title = id,
        startedAtEpochMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        endedAtEpochMillis = date.atStartOfDay(ZoneId.systemDefault()).plusHours(1).toInstant().toEpochMilli(),
        completedSetCount = sets,
        totalVolumeKg = volume,
    )
}
