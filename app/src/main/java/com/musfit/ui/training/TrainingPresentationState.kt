package com.musfit.ui.training

import androidx.compose.runtime.Immutable

/** Routine dashboard, library, detail, editor, and exercise-library state. */
@Immutable
data class TrainingRoutinesLibraryUiState(
    val content: TrainingUiState,
)

/** Active-workout and completed-workout history state. */
@Immutable
data class TrainingActiveHistoryUiState(
    val content: TrainingUiState,
)

internal object TrainingPresentationReducers {
    @Suppress("LongMethod")
    fun routinesLibrary(state: TrainingUiState): TrainingRoutinesLibraryUiState = TrainingRoutinesLibraryUiState(
        content = TrainingUiState(
            selectedSection = state.selectedSection,
            homeRoutines = state.homeRoutines,
            visibleRoutines = state.visibleRoutines,
            homeFolders = state.homeFolders,
            exercises = state.exercises,
            activeWorkoutSummary = state.activeWorkoutSummary,
            workoutHistory = state.workoutHistory,
            historyOverview = state.historyOverview,
            weeklySessionTarget = state.weeklySessionTarget,
            dashboard = state.dashboard,
            exerciseEditor = state.exerciseEditor,
            selectedExerciseDetail = state.selectedExerciseDetail,
            exerciseDetailNotesInput = state.exerciseDetailNotesInput,
            exerciseDetailTarget = state.exerciseDetailTarget,
            routineEditor = state.routineEditor,
            routineFolderEditor = state.routineFolderEditor,
            routineExercisePickerSelectedIds = state.routineExercisePickerSelectedIds,
            routineExercisePickerSearchQuery = state.routineExercisePickerSearchQuery,
            routineExercisePickerFilters = state.routineExercisePickerFilters,
            routineExercisePickerFilterSheetOpen = state.routineExercisePickerFilterSheetOpen,
            loggedExerciseIds = state.loggedExerciseIds,
            selectedRoutineDetail = state.selectedRoutineDetail,
            finishConfirmationOpen = state.finishConfirmationOpen,
            discardConfirmationOpen = state.discardConfirmationOpen,
            message = state.message,
        ),
    )

    fun activeHistory(state: TrainingUiState): TrainingActiveHistoryUiState = TrainingActiveHistoryUiState(
        content = TrainingUiState(
            selectedSection = state.selectedSection,
            exercises = state.exercises,
            activeWorkoutSummary = state.activeWorkoutSummary,
            activeWorkout = state.activeWorkout,
            workoutHistory = state.workoutHistory,
            historyOverview = state.historyOverview,
            selectedWorkoutDetail = state.selectedWorkoutDetail,
            replaceExerciseTargetId = state.replaceExerciseTargetId,
            activeWorkoutNotesInput = state.activeWorkoutNotesInput,
            trainingSettings = state.trainingSettings,
            restTimerDefaultSecondsInput = state.restTimerDefaultSecondsInput,
            restTimer = state.restTimer,
            finishConfirmationOpen = state.finishConfirmationOpen,
            discardConfirmationOpen = state.discardConfirmationOpen,
            message = state.message,
        ),
    )
}
