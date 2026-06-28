package com.musfit.ui.training

import com.musfit.data.repository.LoggedWorkoutSet
import com.musfit.data.repository.LoggedWorkoutSetDetail
import com.musfit.data.repository.ActiveWorkoutDetail
import com.musfit.data.repository.ActiveWorkoutSummary
import com.musfit.data.repository.ExerciseInput
import com.musfit.data.repository.ExerciseDetail
import com.musfit.data.repository.ExerciseGrouping
import com.musfit.data.repository.ExerciseSummary
import com.musfit.data.repository.RoutineSummary
import com.musfit.data.repository.RoutineDetail
import com.musfit.data.repository.RoutineExerciseDetail
import com.musfit.data.repository.RoutineExerciseInput
import com.musfit.data.repository.RoutineInput
import com.musfit.data.repository.TrainingRepository
import com.musfit.data.repository.TrainingSettings
import com.musfit.data.repository.TrainingSettingsInput
import com.musfit.data.repository.TrainingSummary
import com.musfit.data.repository.WorkoutExerciseBlock
import com.musfit.data.repository.WorkoutHistoryDetail
import com.musfit.data.repository.WorkoutHistorySummary
import com.musfit.data.repository.WorkoutSetInputData
import com.musfit.data.repository.WorkoutForExport
import com.musfit.domain.model.ExerciseProgress
import com.musfit.domain.model.TrainingTrendPoint
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
        assertEquals(listOf("Full Body A", "Upper A"), state.routines.map { it.name })
        assertEquals(
            listOf("Barbell Bench Press", "Chest Supported Row"),
            state.exercises.map { it.name },
        )
    }

    @Test
    fun exerciseLibrary_filtersVisibleExercisesBySearchEquipmentAndMuscle() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository)
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
        val viewModel = TrainingViewModel(repository)
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
        assertEquals(TrainingSection.Exercises, viewModel.state.value.selectedSection)
    }

    @Test
    fun exerciseDetail_loadsMetadataAndSavesLocalNotes() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository)
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
    fun openRoutineEditor_forStarterRoutineDoesNotOpenEditor() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openRoutineEditor("routine-full-body-a")
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.routineEditor.isOpen)
        assertTrue(repository.requestedRoutineDetailIds.isEmpty())
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

        viewModel.openRoutineEditor("routine-upper-a")
        dispatcher.scheduler.advanceUntilIdle()

        val editor = viewModel.state.value.routineEditor
        assertTrue(editor.isOpen)
        assertEquals("routine-upper-a", editor.routineId)
        assertEquals("Upper A", editor.name)
        assertEquals("Custom routine", editor.notes)
        assertEquals(1, editor.exercises.size)
        assertEquals("exercise-bench-press", editor.exercises.single().exerciseId)
    }

    @Test
    fun routineProgramFilter_showsProgramOptionsAndVisibleRoutines() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("Full Body", "Upper Lower"), viewModel.state.value.routineProgramOptions)
        assertEquals(listOf("Full Body A", "Upper A"), viewModel.state.value.visibleRoutines.map { it.name })

        viewModel.onRoutineProgramFilterChanged("Upper Lower")

        assertEquals("Upper Lower", viewModel.state.value.selectedRoutineProgram)
        assertEquals(listOf("Upper A"), viewModel.state.value.visibleRoutines.map { it.name })

        viewModel.onRoutineProgramFilterChanged(null)

        assertEquals(null, viewModel.state.value.selectedRoutineProgram)
        assertEquals(listOf("Full Body A", "Upper A"), viewModel.state.value.visibleRoutines.map { it.name })
    }

    @Test
    fun deleteRoutine_forStarterRoutineDoesNotDelete() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteRoutine("routine-full-body-a")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(repository.deletedRoutineIds.isEmpty())
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
    fun routineEditor_reordersExercisesBeforeSave() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository)

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

    @Test
    fun finishAndDiscardRequests_toggleConfirmationState() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository)
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
        val viewModel = TrainingViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.resumeActiveWorkout()
        viewModel.toggleWorkoutSetCompletion("set-1", completed = true)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.requestFinishActiveWorkout()
        viewModel.finishActiveWorkout()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("session-1", repository.finishedSessionId)
        assertFalse(viewModel.state.value.activeWorkoutRouteOpen)
        assertFalse(viewModel.state.value.finishConfirmationOpen)
        assertFalse(viewModel.state.value.restTimer.isVisible)
        assertEquals(TrainingSection.History, viewModel.state.value.selectedSection)
        assertEquals(listOf("session-1"), repository.openedWorkoutDetailSessionIds)
        assertEquals("session-1", viewModel.state.value.selectedWorkoutDetail?.summary?.sessionId)
    }

    @Test
    fun discardActiveWorkout_discardsAndReturnsToRoutinesAndClearsRestTimer() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.resumeActiveWorkout()
        viewModel.toggleWorkoutSetCompletion("set-1", completed = true)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.requestDiscardActiveWorkout()
        viewModel.discardActiveWorkout()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("session-1", repository.discardedSessionId)
        assertFalse(viewModel.state.value.activeWorkoutRouteOpen)
        assertFalse(viewModel.state.value.discardConfirmationOpen)
        assertFalse(viewModel.state.value.restTimer.isVisible)
        assertEquals(TrainingSection.Routines, viewModel.state.value.selectedSection)
        assertEquals(null, viewModel.state.value.selectedWorkoutDetail)
    }

    @Test
    fun completeSet_updatesSummaryAndStartsRunningRestTimer() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository)
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
    fun trainingToolSettings_saveAndApplyRestDefaultToNextCompletedSet() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("120", viewModel.state.value.restTimerDefaultSecondsInput)
        assertEquals("20", viewModel.state.value.plateBarWeightInput)
        assertEquals("25, 20, 15, 10, 5, 2.5, 1.25", viewModel.state.value.availablePlatesInput)

        viewModel.onRestTimerDefaultSecondsChanged("90")
        viewModel.onPlateBarWeightChanged("15")
        viewModel.onAvailablePlatesChanged("20, 10, 5, 2.5, 2.5")
        viewModel.saveTrainingToolSettings()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            TrainingSettingsInput(
                defaultRestSeconds = 90,
                barWeightKg = 15.0,
                availablePlatesKg = listOf(20.0, 10.0, 5.0, 2.5),
            ),
            repository.updatedTrainingSettings,
        )
        assertEquals("90", viewModel.state.value.restTimerDefaultSecondsInput)
        assertEquals("15", viewModel.state.value.plateBarWeightInput)
        assertEquals("20, 10, 5, 2.5", viewModel.state.value.availablePlatesInput)

        viewModel.resumeActiveWorkout()
        viewModel.toggleWorkoutSetCompletion("set-1", completed = true)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(90, viewModel.state.value.restTimer.durationSeconds)
        assertEquals(90, viewModel.state.value.restTimer.remainingSeconds)
    }

    @Test
    fun addSuggestedWarmupSet_addsWarmupSetWithSuggestedValues() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository)
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
        val viewModel = TrainingViewModel(repository)
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
        val viewModel = TrainingViewModel(repository)
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
        val viewModel = TrainingViewModel(repository)
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
        val viewModel = TrainingViewModel(repository)
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
        val viewModel = TrainingViewModel(repository)
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
        val viewModel = TrainingViewModel(repository)
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
        val viewModel = TrainingViewModel(repository)
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
        val viewModel = TrainingViewModel(repository)
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
        val viewModel = TrainingViewModel(repository)
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
    fun selectProgressExercise_loadsProgressForSelectedExercise() = runTest {
        val repository = FakeTrainingRepository()
        val viewModel = TrainingViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.selectSection(TrainingSection.Progress)
        viewModel.selectProgressExercise("exercise-bench-press")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("exercise-bench-press", repository.observedProgressExerciseIds.single())
        assertEquals("exercise-bench-press", viewModel.state.value.selectedProgressExerciseId)
        assertEquals("Barbell Bench Press", viewModel.state.value.selectedExerciseProgress?.exerciseName)
        assertEquals(120.0, viewModel.state.value.selectedExerciseProgress?.heaviestWeightKg ?: 0.0, 0.01)
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
        val deletedRoutineIds = mutableListOf<String>()

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
                ),
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
        )

        private val routineDetails = mutableMapOf(
            "routine-full-body-a" to
                RoutineDetail(
                    id = "routine-full-body-a",
                    name = "Full Body A",
                    notes = "Starter routine",
                    isStarter = true,
                    programName = "Full Body",
                    tags = listOf("beginner", "strength"),
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
            "routine-upper-a" to
                RoutineDetail(
                    id = "routine-upper-a",
                    name = "Upper A",
                    notes = "Custom routine",
                    isStarter = false,
                    programName = "Upper Lower",
                    tags = listOf("upper"),
                    exercises = listOf(
                        RoutineExerciseDetail(
                            id = "routine-upper-a-exercise-1",
                            exercise = exercisesFlow.value.first { it.id == "exercise-bench-press" },
                            sortOrder = 0,
                            targetSets = 3,
                            targetReps = "6",
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

        override fun observeActiveWorkoutDetail(): Flow<ActiveWorkoutDetail?> = activeWorkoutDetail

        override fun observeTrainingSettings(): Flow<TrainingSettings> = trainingSettingsFlow

        override fun observeWorkoutHistory(): Flow<List<WorkoutHistorySummary>> = workoutHistoryFlow

        override fun observeExerciseProgress(exerciseId: String): Flow<ExerciseProgress?> {
            observedProgressExerciseIds += exerciseId
            return progressFlow
        }

        override suspend fun getWorkoutHistoryDetail(sessionId: String): WorkoutHistoryDetail? {
            openedWorkoutDetailSessionIds += sessionId
            return workoutHistoryDetails[sessionId]
        }

        override fun observeDailyTrainingSummary(date: LocalDate): Flow<TrainingSummary> =
            flowOf(TrainingSummary())

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

        override suspend fun getExerciseDetail(exerciseId: String): ExerciseDetail? =
            exerciseDetails[exerciseId]

        override suspend fun updateExerciseLocalNotes(exerciseId: String, notes: String?) {
            updatedExerciseNotesId = exerciseId
            updatedExerciseNotes = notes
            exerciseDetails[exerciseId] = exerciseDetails.getValue(exerciseId).copy(localNotes = notes)
        }

        override suspend fun updateRoutine(routineId: String, input: RoutineInput) {
            updatedRoutineId = routineId
            updatedRoutineInput = input
        }

        override suspend fun duplicateRoutine(routineId: String): String = "$routineId-copy"

        override suspend fun getRoutineDetail(routineId: String): RoutineDetail? {
            requestedRoutineDetailIds += routineId
            return routineDetails[routineId]
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
}
