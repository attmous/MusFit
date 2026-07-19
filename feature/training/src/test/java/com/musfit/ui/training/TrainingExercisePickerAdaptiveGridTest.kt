package com.musfit.ui.training

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.musfit.data.repository.ExerciseSummary
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
@Config(sdk = [35], qualifiers = "w1000dp-h1000dp-mdpi")
class TrainingExercisePickerAdaptiveGridTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun compactWidth_keepsExercisePickerInOneColumn() {
        showPicker(width = 400.dp, exercises = exercises(3))

        val first = compose.onNodeWithTag(exerciseTag(0)).fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithTag(exerciseTag(1)).fetchSemanticsNode().boundsInRoot

        assertEquals(first.left, second.left, POSITION_TOLERANCE_PX)
        assertEquals(first.right, second.right, POSITION_TOLERANCE_PX)
        assertTrue("Expected the second compact item below the first", second.top > first.bottom)
    }

    @Test
    fun expandedWidth_placesExercisePickerItemsInMultipleColumns() {
        showPicker(width = 900.dp, exercises = exercises(4))

        val first = compose.onNodeWithTag(exerciseTag(0)).fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithTag(exerciseTag(1)).fetchSemanticsNode().boundsInRoot
        val third = compose.onNodeWithTag(exerciseTag(2)).fetchSemanticsNode().boundsInRoot
        val create = compose.onNodeWithTag(CREATE_CUSTOM_EXERCISE_TAG).fetchSemanticsNode().boundsInRoot

        assertEquals(first.top, second.top, POSITION_TOLERANCE_PX)
        assertEquals(first.bottom, second.bottom, POSITION_TOLERANCE_PX)
        assertTrue("Expected a second expanded column", second.left > first.right)
        assertEquals(first.top, third.top, POSITION_TOLERANCE_PX)
        assertTrue("Expected a third expanded column", third.left > second.right)
        assertEquals(first.left, create.left, POSITION_TOLERANCE_PX)
        assertEquals(third.right, create.right, POSITION_TOLERANCE_PX)
        assertTrue("Expected the full-width create action below the exercise grid", create.top > first.bottom)
    }

    @Test
    fun widthChange_keepsSearchFilterAndSelectionState() {
        val width = mutableStateOf(400.dp)
        val selectedIds = mutableStateOf(emptySet<String>())
        val searchQuery = mutableStateOf("Exercise")
        val filters = TrainingPickerFilters(equipment = setOf("Barbell"))
        val available = listOf(
            exercise(index = 0, equipment = "Barbell"),
            exercise(index = 1, equipment = "Dumbbell"),
        )

        compose.setContent {
            MusFitTheme {
                Picker(
                    width = width.value,
                    exercises = available,
                    selectedExerciseIds = selectedIds.value,
                    searchQuery = searchQuery.value,
                    filters = filters,
                    onSearchChange = { searchQuery.value = it },
                    onToggleExercise = { id ->
                        selectedIds.value = if (id in selectedIds.value) {
                            selectedIds.value - id
                        } else {
                            selectedIds.value + id
                        }
                    },
                )
            }
        }

        compose.onNodeWithTag(exerciseTag(0))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ToggleableState,
                    ToggleableState.Off,
                ),
            )
            .performClick()
        assertPickerStateIsPreserved()
        compose.runOnIdle { width.value = 900.dp }
        assertPickerStateIsPreserved()
    }

    @Test
    fun fiveHundredExercises_areLazyAndKeepScrollPositionAcrossWidthChange() {
        val width = mutableStateOf(400.dp)
        compose.setContent {
            MusFitTheme {
                Picker(width = width.value, exercises = exercises(500))
            }
        }

        compose.onNodeWithText("Exercise 499").assertDoesNotExist()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Exercise 499"))
        compose.onNodeWithText("Exercise 499").assertExists()

        compose.runOnIdle { width.value = 900.dp }

        compose.onNodeWithText("Exercise 499").assertExists()
        compose.onNodeWithText("Exercise 0").assertDoesNotExist()
    }

    private fun showPicker(width: Dp, exercises: List<ExerciseSummary>) {
        compose.setContent {
            MusFitTheme {
                Picker(width = width, exercises = exercises)
            }
        }
    }

    @Composable
    private fun Picker(
        width: Dp,
        exercises: List<ExerciseSummary>,
        selectedExerciseIds: Set<String> = emptySet(),
        searchQuery: String = "",
        filters: TrainingPickerFilters = TrainingPickerFilters(),
        onSearchChange: (String) -> Unit = {},
        onToggleExercise: (String) -> Unit = {},
    ) {
        Box(modifier = Modifier.width(width).height(900.dp)) {
            RoutineExercisePickerPage(
                exercises = exercises,
                currentRoutineExerciseIds = emptySet(),
                selectedExerciseIds = selectedExerciseIds,
                searchQuery = searchQuery,
                filters = filters,
                filterSheetOpen = false,
                loggedExerciseIds = emptySet(),
                customExerciseEditor = ExerciseEditorState(),
                accent = accent,
                onSearchChange = onSearchChange,
                onOpenFilters = {},
                onCloseFilters = {},
                onToggleEquipment = {},
                onToggleMuscle = {},
                onOnlyDoneChange = {},
                onResetFilters = {},
                onClearFilters = {},
                onToggleExercise = onToggleExercise,
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

    private fun assertPickerStateIsPreserved() {
        compose.onNode(hasContentDescription("Search exercises"))
            .assert(hasText("Exercise"))
        compose.onNodeWithText("Barbell").assertExists()
        compose.onNodeWithTag(exerciseTag(0))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ToggleableState,
                    ToggleableState.On,
                ),
            )
        compose.onNodeWithTag(exerciseTag(1)).assertDoesNotExist()
    }

    private fun exercises(count: Int): List<ExerciseSummary> = List(count) { index -> exercise(index = index, equipment = "Barbell") }

    private fun exercise(index: Int, equipment: String): ExerciseSummary = ExerciseSummary(
        id = "exercise-$index",
        name = "Exercise $index",
        category = "strength",
        equipment = equipment,
        targetMuscles = "full body",
        isCustom = false,
    )

    private fun exerciseTag(index: Int): String = "training-exercise-thumbnail-placeholder-exercise-$index"

    private companion object {
        const val POSITION_TOLERANCE_PX = 0.5f
        const val CREATE_CUSTOM_EXERCISE_TAG = "training-create-custom-exercise"

        val accent = TabAccent(
            color = androidx.compose.ui.graphics.Color.Blue,
            onColor = androidx.compose.ui.graphics.Color.White,
            container = androidx.compose.ui.graphics.Color.LightGray,
            onContainer = androidx.compose.ui.graphics.Color.Black,
        )
    }
}
