package com.musfit.ui.training

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TrainingPresentationReducersTest {
    @Test
    fun routinesLibraryProjectionIgnoresActiveWorkoutDetailChanges() {
        val baseline = TrainingUiState(
            routineExercisePickerSearchQuery = "bench",
        )
        val unrelated = baseline.copy(
            activeWorkoutNotesInput = "keep elbows tucked",
            restTimer = RestTimerState(isVisible = true, remainingSeconds = 90),
        )

        assertEquals(
            TrainingPresentationReducers.routinesLibrary(baseline),
            TrainingPresentationReducers.routinesLibrary(unrelated),
        )
        assertNotEquals(
            TrainingPresentationReducers.routinesLibrary(baseline),
            TrainingPresentationReducers.routinesLibrary(
                baseline.copy(routineExercisePickerSearchQuery = "row"),
            ),
        )
    }

    @Test
    fun activeHistoryProjectionIgnoresRoutineEditorChanges() {
        val baseline = TrainingUiState(
            restTimer = RestTimerState(isVisible = true, remainingSeconds = 90),
        )
        val unrelated = baseline.copy(
            routineEditor = RoutineEditorState(isOpen = true, name = "Push"),
            routineExercisePickerSearchQuery = "bench",
        )

        assertEquals(
            TrainingPresentationReducers.activeHistory(baseline),
            TrainingPresentationReducers.activeHistory(unrelated),
        )
        assertNotEquals(
            TrainingPresentationReducers.activeHistory(baseline),
            TrainingPresentationReducers.activeHistory(
                baseline.copy(restTimer = baseline.restTimer.copy(remainingSeconds = 89)),
            ),
        )
    }

    @Test
    fun routeProjectionClassifiesDestinationLifetime() {
        assertEquals(
            TrainingSurfaceGroup.RoutinesLibrary,
            TrainingPresentationReducers.route(
                TrainingUiState(pageStack = listOf(TrainingPage.RoutineEditor)),
            ).surfaceGroup,
        )
        assertEquals(
            TrainingSurfaceGroup.ActiveHistory,
            TrainingPresentationReducers.route(
                TrainingUiState(pageStack = listOf(TrainingPage.ActiveWorkout)),
            ).surfaceGroup,
        )
        assertEquals(
            TrainingSurfaceGroup.ActiveHistory,
            TrainingPresentationReducers.route(
                TrainingUiState(selectedSection = TrainingSection.History),
            ).surfaceGroup,
        )
    }
}
