package com.musfit.ui.training

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import com.musfit.data.repository.RoutineDetail
import com.musfit.data.repository.RoutineSummary
import com.musfit.data.repository.WorkoutHistorySummary
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w400dp-h800dp-mdpi")
class TrainingAdaptiveUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val accent = TabAccent(
        color = androidx.compose.ui.graphics.Color.Blue,
        onColor = androidx.compose.ui.graphics.Color.White,
        container = androidx.compose.ui.graphics.Color.LightGray,
        onContainer = androidx.compose.ui.graphics.Color.Black,
    )

    @Test
    fun compactRoutineDetail_hasBackButtonWithMinimumTarget() {
        compose.setContent {
            MusFitTheme {
                RoutineDetailContent(
                    detail = routineDetail(),
                    accent = accent,
                    onStart = {},
                    onEdit = {},
                    onOpenExercise = { _, _ -> },
                    onDuplicate = {},
                    onDelete = {},
                    onClose = {},
                )
            }
        }

        val back = compose.onNode(hasContentDescription("Back"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .fetchSemanticsNode()
        assertTrue(back.boundsInRoot.width >= 48.dp.value)
        assertTrue(back.boundsInRoot.height >= 48.dp.value)
    }

    @Test
    fun expandedRoutineDetail_hidesContextualBackButKeepsPrimaryAction() {
        compose.setContent {
            MusFitTheme {
                RoutineDetailContent(
                    detail = routineDetail(),
                    accent = accent,
                    onStart = {},
                    onEdit = {},
                    onOpenExercise = { _, _ -> },
                    onDuplicate = {},
                    onDelete = {},
                    onClose = {},
                    showBackAction = false,
                )
            }
        }

        compose.onNode(hasContentDescription("Back")).assertDoesNotExist()
        compose.onNodeWithText("Start").assertExists()
    }

    @Test
    fun routineWorkspace_lazilyRendersFiveHundredStableRowsAndKeepsStartTarget() {
        val routines = List(500) { index -> routineSummary(index) }
        compose.setContent {
            MusFitTheme {
                TrainingHomeContent(
                    routines = routines,
                    accent = accent,
                    onStartBlankWorkout = {},
                    onNewRoutine = {},
                    onOpenLibrary = {},
                    showLibraryLink = false,
                    onStartRoutine = {},
                    onOpenRoutineDetail = {},
                    modifier = Modifier,
                )
            }
        }

        compose.onNodeWithText("Routine 499").assertDoesNotExist()
        val start = compose.onNode(hasContentDescription("Start Routine 0"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .fetchSemanticsNode()
        assertTrue(start.boundsInRoot.width >= 48.dp.value)
        assertTrue(start.boundsInRoot.height >= 48.dp.value)
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Routine 499"))
        compose.onNodeWithText("Routine 499").assertExists()
    }

    @Test
    fun history_lazilyRendersFiveHundredKeyedSessions() {
        val now = System.currentTimeMillis()
        val history = List(500) { index ->
            WorkoutHistorySummary(
                sessionId = "session-$index",
                title = "Workout $index",
                startedAtEpochMillis = now,
                endedAtEpochMillis = now + 3_600_000,
                completedSetCount = 4,
                totalVolumeKg = 1_000.0,
            )
        }
        compose.setContent {
            MusFitTheme {
                TrainingHistoryContent(
                    history = history,
                    overview = TrainingHistoryOverview(),
                    accent = accent,
                    calendarOpen = false,
                    onOpenDetail = {},
                )
            }
        }

        compose.onNodeWithText("Workout 499").assertDoesNotExist()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Workout 499"))
        compose.onNodeWithText("Workout 499").assertExists()
    }

    private fun routineDetail() = RoutineDetail(
        id = "routine-1",
        name = "Routine 1",
        notes = null,
        isStarter = false,
        exercises = emptyList(),
    )

    private fun routineSummary(index: Int) = RoutineSummary(
        id = "routine-$index",
        name = "Routine $index",
        notes = null,
        exerciseCount = 3,
        targetSetCount = 9,
        isStarter = false,
    )
}
