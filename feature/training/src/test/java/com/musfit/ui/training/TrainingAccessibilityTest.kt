package com.musfit.ui.training

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.musfit.data.repository.ActiveWorkoutDetail
import com.musfit.data.repository.ExerciseSummary
import com.musfit.data.repository.LoggedWorkoutSetDetail
import com.musfit.data.repository.RoutineExerciseInput
import com.musfit.data.repository.WorkoutExerciseBlock
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w400dp-h1400dp-mdpi")
class TrainingAccessibilityTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun activeWorkout_primaryBackAndOverflowControlsHaveOneButtonNodeAndFortyEightDpTarget() {
        compose.setContent {
            MusFitTheme {
                Box(modifier = Modifier.width(320.dp).padding(16.dp)) {
                    TrainingActiveWorkoutContent(
                        workout = activeWorkout(),
                        exercises = listOf(benchExercise()),
                        restTimer = RestTimerState(
                            isVisible = true,
                            sourceSetId = "set-1",
                            durationSeconds = 120,
                            remainingSeconds = 90,
                            isRunning = true,
                        ),
                        workoutNotes = "",
                        restTimerDefaultSecondsInput = "120",
                        barWeightKg = 20.0,
                        availablePlatesKg = listOf(20.0, 10.0, 5.0, 2.5),
                        accent = trainingAccent,
                        onAddExercise = {},
                        onAddSet = {},
                        onAddSuggestedWarmupSet = { _, _, _ -> },
                        onUpdateSet = { _, _, _, _, _, _ -> },
                        onDeleteSet = {},
                        onToggleSet = { _, _ -> },
                        onWorkoutNotesChange = {},
                        onSaveWorkoutNotes = {},
                        onRestTimerDefaultSecondsChange = {},
                        onSaveRestTimer = {},
                        onMoveSetUp = {},
                        onMoveSetDown = {},
                        onTickRestTimer = {},
                        onSkipRestTimer = {},
                        onAdjustRestTimer = {},
                        onMakeSuperset = {},
                        onDissolveSuperset = {},
                        onRemoveExercise = {},
                        onMoveExercise = { _, _ -> },
                        onReplaceExercise = {},
                        replaceExerciseTargetId = null,
                        onReplacePick = {},
                        onReplaceDismiss = {},
                        onOpenCoach = {},
                        onClose = {},
                        onFinish = {},
                        onDiscard = {},
                    )
                }
            }
        }

        listOf(
            hasContentDescription("Back"),
            hasText("Finish") and hasClickAction(),
            hasContentDescription("Workout options"),
            hasContentDescription("Shorten rest by 30 seconds"),
            hasContentDescription("Extend rest by 30 seconds"),
            hasText("Skip") and hasClickAction(),
            hasContentDescription("Exercise options"),
            hasContentDescription("Set 1 options"),
            hasContentDescription("Mark complete"),
            hasContentDescription("Add set"),
            hasContentDescription("Ask coach about this workout"),
        ).forEach { matcher -> compose.onNode(matcher).assertButtonTarget() }

        compose.onNode(hasContentDescription("Deadlift") and hasClickAction())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Deadlift")))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Complete"))
            .assertButtonTarget()
        compose.onNode(hasContentDescription("Deadlift complete")).assertDoesNotExist()

        compose.onNode(hasContentDescription("Weight, kilograms")).assertEditableTarget()
        compose.onNode(hasContentDescription("Repetitions")).assertEditableTarget()
        compose.onNode(hasContentDescription("Set 1 options")).performClick()
        compose.onNodeWithText("RPE & notes").performClick()
        compose.onNode(hasContentDescription("RPE")).assertEditableTarget()
        compose.onNode(hasContentDescription("Set notes")).assertEditableTarget()

        compose.onNode(hasText("Add exercise") and hasClickAction()).performClick()
        compose.onNodeWithText("Bench Press").assertButtonTarget()
    }

    @Test
    fun routineEditor_textFieldsHaveAccessibleNamesAndFortyEightDpTargets() {
        compose.setContent {
            MusFitTheme {
                TrainingRoutineEditor(
                    editor = RoutineEditorState(
                        name = "Strength",
                        exercises = listOf(
                            RoutineExerciseInput(
                                exerciseId = "squat",
                                targetSets = 3,
                                targetReps = "8",
                                restSeconds = 120,
                            ),
                        ),
                        isOpen = true,
                    ),
                    exercises = listOf(squatExercise().copy(imageUrl = "file:///does-not-exist.jpg")),
                    accent = trainingAccent,
                    onNameChange = {},
                    onOpenExercisePicker = {},
                    onRemoveExercise = {},
                    onMoveExerciseUp = {},
                    onMoveExerciseDown = {},
                    onTargetSetsChange = { _, _ -> },
                    onTargetRepsChange = { _, _ -> },
                    onRestSecondsChange = { _, _ -> },
                    onAddSet = {},
                    onRemoveSet = { _, _ -> },
                    onSetTypeChange = { _, _, _ -> },
                    onSetRepsChange = { _, _, _ -> },
                    onSetWeightChange = { _, _, _ -> },
                    onSave = {},
                    onCancel = {},
                )
            }
        }

        compose.onNode(hasContentDescription("Routine name")).assertEditableTarget()
        compose.onNode(hasContentDescription("Back Squat") and hasClickAction())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Back Squat")))
            .performClick()
        compose.onNode(hasContentDescription("Expand Back Squat")).assertDoesNotExist()
        compose.onNode(hasContentDescription("Back Squat") and hasClickAction())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Back Squat")))
        compose.onNode(hasContentDescription("Collapse Back Squat")).assertDoesNotExist()
        listOf("Sets", "Reps", "Rest, seconds").forEach { label ->
            compose.onNode(hasContentDescription(label)).assertEditableTarget()
        }
    }

    @Test
    fun routineEditor_reorderActionsExposeValidDirectionsAndCurrentIndices() {
        val movedUp = mutableListOf<Int>()
        val movedDown = mutableListOf<Int>()
        compose.setContent {
            MusFitTheme {
                TrainingRoutineEditor(
                    editor = RoutineEditorState(
                        name = "Strength",
                        exercises = listOf(
                            routineExercise("squat"),
                            routineExercise("bench"),
                            routineExercise("deadlift"),
                        ),
                        isOpen = true,
                    ),
                    exercises = listOf(squatExercise(), benchExercise(), deadliftExercise()),
                    accent = trainingAccent,
                    onNameChange = {},
                    onOpenExercisePicker = {},
                    onRemoveExercise = {},
                    onMoveExerciseUp = { movedUp += it },
                    onMoveExerciseDown = { movedDown += it },
                    onTargetSetsChange = { _, _ -> },
                    onTargetRepsChange = { _, _ -> },
                    onRestSecondsChange = { _, _ -> },
                    onAddSet = {},
                    onRemoveSet = { _, _ -> },
                    onSetTypeChange = { _, _, _ -> },
                    onSetRepsChange = { _, _, _ -> },
                    onSetWeightChange = { _, _, _ -> },
                    onSave = {},
                    onCancel = {},
                )
            }
        }

        val reorderHandles = compose.onAllNodes(hasContentDescription("Reorder exercise"))
        reorderHandles.assertCountEquals(3)
        val first = reorderHandles[0]
        val middle = reorderHandles[1]
        val last = reorderHandles[2]
        assertEquals(listOf("Move exercise down"), customActionLabels(first))
        assertEquals(
            listOf("Move exercise up", "Move exercise down"),
            customActionLabels(middle),
        )
        assertEquals(listOf("Move exercise up"), customActionLabels(last))

        invokeCustomAction(first, "Move exercise down")
        invokeCustomAction(middle, "Move exercise up")
        invokeCustomAction(last, "Move exercise up")
        assertEquals(listOf(1, 2), movedUp)
        assertEquals(listOf(0), movedDown)
    }

    @Test
    fun exercisePicker_searchHasAccessibleNameAndFortyEightDpTarget() {
        compose.setContent {
            MusFitTheme {
                RoutineExercisePickerPage(
                    exercises = listOf(squatExercise().copy(imageUrl = "file:///does-not-exist.jpg")),
                    currentRoutineExerciseIds = emptySet(),
                    selectedExerciseIds = emptySet(),
                    searchQuery = "",
                    filters = TrainingPickerFilters(),
                    filterSheetOpen = false,
                    loggedExerciseIds = emptySet(),
                    customExerciseEditor = ExerciseEditorState(),
                    accent = trainingAccent,
                    onSearchChange = {},
                    onOpenFilters = {},
                    onCloseFilters = {},
                    onToggleEquipment = {},
                    onToggleMuscle = {},
                    onOnlyDoneChange = {},
                    onResetFilters = {},
                    onClearFilters = {},
                    onToggleExercise = {},
                    onOpenCustomExercise = {},
                    onCloseCustomExercise = {},
                    onCustomExerciseNameChange = {},
                    onCustomExerciseCategoryChange = {},
                    onCustomExerciseEquipmentChange = {},
                    onCustomExerciseTargetMusclesChange = {},
                    onSaveCustomExercise = {},
                    onCancel = {},
                    onConfirm = {},
                )
            }
        }

        compose.onNode(hasContentDescription("Search exercises")).assertEditableTarget()
        compose.onNode(hasText("Back Squat"))
            .assert(hasClickAction())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Off))
        compose.onNodeWithTag("training-exercise-thumbnail-item-squat").assertExists()
    }

    private fun SemanticsNodeInteraction.assertButtonTarget() {
        assert(hasClickAction())
        assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        val bounds = fetchSemanticsNode().boundsInRoot
        assertTrue("Expected at least 48dp width, but was ${bounds.width}", bounds.width >= 48.dp.value)
        assertTrue("Expected at least 48dp height, but was ${bounds.height}", bounds.height >= 48.dp.value)
    }

    private fun SemanticsNodeInteraction.assertEditableTarget() {
        assert(hasSetTextAction())
        val bounds = fetchSemanticsNode().boundsInRoot
        assertTrue("Expected at least 48dp width, but was ${bounds.width}", bounds.width >= 48.dp.value)
        assertTrue("Expected at least 48dp height, but was ${bounds.height}", bounds.height >= 48.dp.value)
    }

    private fun customActionLabels(node: SemanticsNodeInteraction): List<String> = node
        .fetchSemanticsNode()
        .config[SemanticsActions.CustomActions]
        .map { it.label }

    private fun invokeCustomAction(node: SemanticsNodeInteraction, label: String) {
        val action = node
            .fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
            .singleOrNull { it.label == label }
        compose.runOnIdle {
            checkNotNull(action).action()
        }
    }

    private fun routineExercise(exerciseId: String) = RoutineExerciseInput(
        exerciseId = exerciseId,
        targetSets = 3,
        targetReps = "8",
        restSeconds = 120,
    )

    private fun activeWorkout(): ActiveWorkoutDetail {
        val exercise = ExerciseSummary(
            id = "squat",
            name = "Back Squat",
            category = "Strength",
            equipment = "Barbell",
            targetMuscles = "Legs",
            isCustom = false,
        )
        val set = LoggedWorkoutSetDetail(
            id = "set-1",
            exerciseId = exercise.id,
            setType = "working",
            targetReps = "5",
            reps = 5,
            weightKg = 100.0,
            rpe = null,
            notes = null,
            completed = false,
            previousLabel = "95 kg x 5",
        )
        val completedExercise = deadliftExercise()
        val completedSet = LoggedWorkoutSetDetail(
            id = "set-2",
            exerciseId = completedExercise.id,
            setType = "working",
            targetReps = "5",
            reps = 5,
            weightKg = 120.0,
            rpe = 8.0,
            notes = null,
            completed = true,
            previousLabel = "115 kg x 5",
        )
        return ActiveWorkoutDetail(
            sessionId = "session-1",
            title = "Strength",
            startedAtEpochMillis = System.currentTimeMillis(),
            completedSetCount = 1,
            totalVolumeKg = 600.0,
            exerciseBlocks = listOf(
                WorkoutExerciseBlock(
                    exercise = exercise,
                    targetReps = "5",
                    sets = listOf(set),
                ),
                WorkoutExerciseBlock(
                    exercise = completedExercise,
                    targetReps = "5",
                    sets = listOf(completedSet),
                ),
            ),
        )
    }

    private fun benchExercise(): ExerciseSummary = ExerciseSummary(
        id = "bench",
        name = "Bench Press",
        category = "Strength",
        equipment = "Barbell",
        targetMuscles = "Chest",
        isCustom = false,
    )

    private fun squatExercise(): ExerciseSummary = ExerciseSummary(
        id = "squat",
        name = "Back Squat",
        category = "Strength",
        equipment = "Barbell",
        targetMuscles = "Legs",
        isCustom = false,
    )

    private fun deadliftExercise(): ExerciseSummary = ExerciseSummary(
        id = "deadlift",
        name = "Deadlift",
        category = "Strength",
        equipment = "Barbell",
        targetMuscles = "Back",
        isCustom = false,
    )

    private companion object {
        val trainingAccent = TabAccent(
            color = Color(0xFF4B55C5),
            onColor = Color.White,
            container = Color(0xFFE2E4FF),
            onContainer = Color(0xFF1E246B),
        )
    }
}
