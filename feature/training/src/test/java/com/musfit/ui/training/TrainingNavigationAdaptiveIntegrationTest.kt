package com.musfit.ui.training

import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.musfit.data.repository.ActiveWorkoutDetail
import com.musfit.data.repository.ActiveWorkoutSummary
import com.musfit.data.repository.GoalsRepository
import com.musfit.data.repository.LoggedWorkoutSet
import com.musfit.data.repository.RoutineDetail
import com.musfit.data.repository.RoutineInput
import com.musfit.data.repository.RoutineSummary
import com.musfit.data.repository.TrainingRepository
import com.musfit.data.repository.TrainingSummary
import com.musfit.data.repository.UserGoals
import com.musfit.data.repository.WorkoutForExport
import com.musfit.ui.theme.MusFitTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

private const val ROUTINE_ID = "adaptive-routine"
private const val ROUTINE_NAME = "Adaptive Routine"
private const val ACTIVE_WORKOUT_NAME = "Adaptive workout"
private const val TEST_TIMEOUT_MILLIS = 5_000L

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w1200dp-h800dp-mdpi")
class TrainingNavigationAdaptiveIntegrationTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun expandedHost_keepsListAndDetailTogetherAndUsesFullScreenLeafRoutes() {
        val repository = NavigationTrainingRepository()
        val viewModel = TrainingViewModel(repository, NavigationGoalsRepository())

        compose.setContent {
            MusFitTheme {
                TrainingNavigation(viewModel = viewModel)
            }
        }

        waitForText("All")
        compose.onNodeWithText("All").performClick()

        // This placeholder is supplied by the real ListDetailSceneStrategy entry metadata.
        waitForText("Select a routine")
        compose.onNodeWithText("Routines").assertExists()
        compose.onNodeWithText(ROUTINE_NAME).performClick()

        waitUntilTextCount(ROUTINE_NAME, 2)
        compose.onNodeWithText("Select a routine").assertDoesNotExist()
        compose.onNodeWithText("Routines").assertExists()
        compose.onNodeWithText("Start").assertExists()
        // Only the list pane owns a contextual back action in the expanded scene.
        compose.onAllNodesWithContentDescription("Back").assertCountEquals(1)
        assertEquals(1, repository.maximumRoutineCollectors)

        compose.onNodeWithText("Edit").performClick()

        waitForContentDescription("Cancel")
        compose.onNodeWithText("Routines").assertDoesNotExist()
        compose.onNodeWithText("Start").assertDoesNotExist()
        compose.onNodeWithContentDescription("Cancel").performClick()

        waitUntilTextCount(ROUTINE_NAME, 2)
        compose.onNodeWithText("Routines").assertExists()
        compose.onNodeWithText("Start").performClick()

        waitForText("Finish")
        compose.onNodeWithText(ACTIVE_WORKOUT_NAME).assertExists()
        compose.onNodeWithText("Routines").assertDoesNotExist()
        compose.onNodeWithText("Finish").assertExists()
        compose.onNodeWithContentDescription("Back").performClick()

        waitUntilTextCount(ROUTINE_NAME, 2)
        compose.onNodeWithText("Routines").assertExists()
        compose.onNodeWithText("Start").assertExists()
        compose.onAllNodesWithContentDescription("Back").assertCountEquals(1)
        assertEquals(1, repository.maximumRoutineCollectors)
        assertEquals(1, repository.maximumActiveWorkoutCollectors)
    }

    @Test
    fun compactExpandedDirectiveChanges_keepSelectionAndOneRepositoryCollector() {
        val repository = NavigationTrainingRepository()
        val viewModel = TrainingViewModel(repository, NavigationGoalsRepository())
        var directive by mutableStateOf(
            PaneScaffoldDirective.Default.copy(maxHorizontalPartitions = 1),
        )

        compose.setContent {
            MusFitTheme {
                TrainingNavigationHost(
                    viewModel = viewModel,
                    paneScaffoldDirectiveOverride = directive,
                )
            }
        }

        waitForText("All")
        compose.onNodeWithText("All").performClick()
        waitForText(ROUTINE_NAME)
        compose.onNodeWithText(ROUTINE_NAME).performClick()
        waitForText("Start")
        compose.onAllNodesWithText(ROUTINE_NAME).assertCountEquals(1)

        compose.runOnIdle {
            directive = PaneScaffoldDirective.Default.copy(maxHorizontalPartitions = 2)
        }
        waitUntilTextCount(ROUTINE_NAME, 2)
        compose.onNodeWithText("Routines").assertExists()
        compose.onNodeWithText("Start").assertExists()

        compose.runOnIdle {
            directive = PaneScaffoldDirective.Default.copy(maxHorizontalPartitions = 1)
        }
        waitUntilTextCount(ROUTINE_NAME, 1)
        compose.onNodeWithText("Start").assertExists()

        compose.runOnIdle {
            directive = PaneScaffoldDirective.Default.copy(maxHorizontalPartitions = 2)
        }
        waitUntilTextCount(ROUTINE_NAME, 2)
        compose.onNodeWithText("Start").assertExists()
        assertEquals(1, repository.maximumRoutineCollectors)
        assertEquals(1, repository.maximumActiveWorkoutCollectors)
    }

    private fun waitForText(text: String) {
        compose.waitUntil(timeoutMillis = TEST_TIMEOUT_MILLIS) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitUntilTextCount(text: String, count: Int) {
        compose.waitUntil(timeoutMillis = TEST_TIMEOUT_MILLIS) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().size == count
        }
    }

    private fun waitForContentDescription(description: String) {
        compose.waitUntil(timeoutMillis = TEST_TIMEOUT_MILLIS) {
            compose.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()
        }
    }
}

private class NavigationGoalsRepository : GoalsRepository {
    override fun observeUserGoals(): Flow<UserGoals> = flowOf(UserGoals())

    override suspend fun updateUserGoals(goals: UserGoals) = Unit
}

private class NavigationTrainingRepository : TrainingRepository {
    private val routine = RoutineSummary(
        id = ROUTINE_ID,
        name = ROUTINE_NAME,
        notes = "Expanded navigation integration fixture",
        exerciseCount = 0,
        targetSetCount = 0,
        isStarter = false,
    )
    private val routines = MutableStateFlow(listOf(routine))
    private val activeWorkoutSummary = MutableStateFlow<ActiveWorkoutSummary?>(null)
    private val activeWorkoutDetail = MutableStateFlow<ActiveWorkoutDetail?>(null)
    private var activeRoutineCollectors = 0
    private var activeWorkoutCollectors = 0

    var maximumRoutineCollectors: Int = 0
        private set
    var maximumActiveWorkoutCollectors: Int = 0
        private set

    override fun observeRoutineSummaries(): Flow<List<RoutineSummary>> = routines
        .onStart {
            activeRoutineCollectors += 1
            maximumRoutineCollectors = maxOf(maximumRoutineCollectors, activeRoutineCollectors)
        }
        .onCompletion { activeRoutineCollectors -= 1 }

    override fun observeActiveWorkoutSummary(): Flow<ActiveWorkoutSummary?> = activeWorkoutSummary

    override fun observeActiveWorkoutDetail(): Flow<ActiveWorkoutDetail?> = activeWorkoutDetail
        .onStart {
            activeWorkoutCollectors += 1
            maximumActiveWorkoutCollectors = maxOf(maximumActiveWorkoutCollectors, activeWorkoutCollectors)
        }
        .onCompletion { activeWorkoutCollectors -= 1 }

    override suspend fun getRoutineDetail(routineId: String): RoutineDetail? = routine
        .takeIf { it.id == routineId }
        ?.let {
            RoutineDetail(
                id = it.id,
                name = it.name,
                notes = it.notes,
                isStarter = it.isStarter,
                exercises = emptyList(),
            )
        }

    override suspend fun startWorkoutFromRoutine(routineId: String): String {
        check(routineId == routine.id)
        val sessionId = "adaptive-session"
        val now = System.currentTimeMillis()
        activeWorkoutSummary.value = ActiveWorkoutSummary(
            sessionId = sessionId,
            title = ACTIVE_WORKOUT_NAME,
            startedAtEpochMillis = now,
            completedSetCount = 0,
            totalVolumeKg = 0.0,
        )
        activeWorkoutDetail.value = ActiveWorkoutDetail(
            sessionId = sessionId,
            title = ACTIVE_WORKOUT_NAME,
            startedAtEpochMillis = now,
            completedSetCount = 0,
            totalVolumeKg = 0.0,
            exerciseBlocks = emptyList(),
        )
        return sessionId
    }

    override suspend fun addCompletedSet(exerciseName: String, reps: Int, weightKg: Double) = LoggedWorkoutSet(
        id = "unused",
        exerciseName = exerciseName,
        reps = reps,
        weightKg = weightKg,
        completed = true,
    )

    override suspend fun setCompletion(setId: String, completed: Boolean) = Unit

    override fun observeDailyTrainingSummary(date: LocalDate): Flow<TrainingSummary> = flowOf(TrainingSummary())

    override suspend fun createRoutine(input: RoutineInput): String = "unused"

    override suspend fun updateRoutine(routineId: String, input: RoutineInput) = Unit

    override suspend fun duplicateRoutine(routineId: String): String = "$routineId-copy"

    override suspend fun deleteRoutine(routineId: String) = Unit

    override suspend fun startBlankWorkout(): String = "unused"

    override suspend fun getLatestWorkoutForExport(): WorkoutForExport? = null

    override suspend fun markWorkoutExported(
        sessionId: String,
        recordId: String,
        exportedAtEpochMillis: Long,
    ) = Unit
}
