package com.musfit.ui.training

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import com.musfit.data.repository.WeeklyTrainingVolume
import com.musfit.domain.model.ExerciseProgress
import com.musfit.domain.model.TrainingTrendPoint
import com.musfit.ui.text.UiText
import com.musfit.ui.text.resolve
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import com.musfit.ui.training.e1rmChartSelectionDescription as typedE1rmChartSelectionDescription
import com.musfit.ui.training.e1rmChartSummary as typedE1rmChartSummary
import com.musfit.ui.training.e1rmDataRowDescription as typedE1rmDataRowDescription
import com.musfit.ui.training.weeklyVolumeChartSelectionDescription as typedWeeklyVolumeChartSelectionDescription
import com.musfit.ui.training.weeklyVolumeChartSummary as typedWeeklyVolumeChartSummary
import com.musfit.ui.training.weeklyVolumeDataRowDescription as typedWeeklyVolumeDataRowDescription

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w400dp-h1600dp-mdpi")
class TrainingProgressChartAccessibilityTest {
    @get:Rule
    val compose = createComposeRule()

    private val weeklyVolumeTab = hasText("Weekly volume") and
        SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

    private val today = LocalDate.now()
    private val accent = TabAccent(
        color = androidx.compose.ui.graphics.Color.Blue,
        onColor = androidx.compose.ui.graphics.Color.White,
        container = androidx.compose.ui.graphics.Color.LightGray,
        onContainer = androidx.compose.ui.graphics.Color.Black,
    )

    @Test
    fun charts_exposeOneBoundedImageNodeAndConditionalActions() {
        val trend = trendPoints(3)
        val weeks = volumeWeeks(3)
        setProgressContent(trend = trend, weeks = weeks)

        val e1rm = compose.onNodeWithContentDescription(e1rmChartSummary("Back Squat", trend))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    e1rmChartSelectionDescription(trend, 2),
                ),
            )
        compose.onAllNodes(
            hasContentDescription(e1rmChartSummary("Back Squat", trend)),
            useUnmergedTree = true,
        ).assertCountEquals(1)
        assertTrue(e1rm.fetchSemanticsNode().boundsInRoot.width > 0f)
        assertTrue(e1rm.fetchSemanticsNode().boundsInRoot.height > 0f)
        assertEquals(
            listOf("Previous data point", "Show data table"),
            customActionLabels(e1rm.fetchSemanticsNode().config[SemanticsActions.CustomActions]),
        )

        invokeCustomAction(e1rm, "Previous data point")
        e1rm.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                e1rmChartSelectionDescription(trend, 1),
            ),
        )
        assertEquals(
            listOf("Previous data point", "Next data point", "Show data table"),
            customActionLabels(e1rm.fetchSemanticsNode().config[SemanticsActions.CustomActions]),
        )
        compose.onNodeWithTag(e1rmVisualSelectionTag(trend[1])).assertExists()
        compose.onAllNodes(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                e1rmChartSelectionDescription(trend, 1),
            ),
            useUnmergedTree = true,
        ).assertCountEquals(1)

        val weekly = compose.onNodeWithContentDescription(weeklyVolumeChartSummary(weeks))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image))
        compose.onAllNodes(
            hasContentDescription(weeklyVolumeChartSummary(weeks)),
            useUnmergedTree = true,
        ).assertCountEquals(1)
        assertTrue(weekly.fetchSemanticsNode().boundsInRoot.width > 0f)
        assertTrue(weekly.fetchSemanticsNode().boundsInRoot.height > 0f)
        invokeCustomAction(weekly, "Previous data point")
        compose.onNodeWithTag(weeklyVolumeVisualSelectionTag(weeks[1])).assertExists()
        compose.onAllNodes(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                weeklyVolumeChartSelectionDescription(weeks, 1),
            ),
            useUnmergedTree = true,
        ).assertCountEquals(1)

        invokeCustomAction(e1rm, "Show data table")
        compose.onNodeWithContentDescription(e1rmDataRowDescription(trend[1]))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
    }

    @Test
    fun chartPointerAndKeyboard_updateTheSameSelectedDateState() {
        val trend = trendPoints(4)
        setProgressContent(trend = trend, weeks = emptyList())
        val chart = compose.onNodeWithContentDescription(e1rmChartSummary("Back Squat", trend))

        chart.performTouchInput { click(Offset(1f, 48f)) }
        chart.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                e1rmChartSelectionDescription(trend, 0),
            ),
        )

        chart.performSemanticsAction(SemanticsActions.RequestFocus)
        chart.performKeyInput { pressKey(Key.MoveEnd) }
        chart.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                e1rmChartSelectionDescription(trend, 3),
            ),
        )
        chart.performKeyInput { pressKey(Key.DirectionLeft) }
        chart.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                e1rmChartSelectionDescription(trend, 2),
            ),
        )
        chart.performKeyInput { pressKey(Key.MoveHome) }
        chart.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                e1rmChartSelectionDescription(trend, 0),
            ),
        )
        chart.performKeyInput { pressKey(Key.DirectionRight) }
        chart.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                e1rmChartSelectionDescription(trend, 1),
            ),
        )
        chart.performKeyInput { pressKey(Key.DirectionDown) }
        chart.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                e1rmChartSelectionDescription(trend, 2),
            ),
        )
        chart.performKeyInput { pressKey(Key.DirectionUp) }
        chart.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                e1rmChartSelectionDescription(trend, 1),
            ),
        )
    }

    @Test
    fun weeklyPointer_usesBarSlotBoundaries() {
        val weeks = volumeWeeks(6)
        setProgressContent(trend = emptyList(), weeks = weeks, includeProgress = false)
        val chart = compose.onNodeWithContentDescription(weeklyVolumeChartSummary(weeks))
        val bounds = chart.fetchSemanticsNode().boundsInRoot

        chart.performTouchInput { click(Offset(bounds.width * 0.15f, bounds.height / 2f)) }

        chart.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                weeklyVolumeChartSelectionDescription(weeks, 0),
            ),
        )
    }

    @Test
    fun rtlPointer_selectsTheVisuallyTappedPointAndBar() {
        val trend = trendPoints(4)
        val weeks = volumeWeeks(4)
        setProgressContent(
            trend = trend,
            weeks = weeks,
            layoutDirection = LayoutDirection.Rtl,
        )
        val e1rm = compose.onNodeWithContentDescription(e1rmChartSummary("Back Squat", trend))
        val e1rmBounds = e1rm.fetchSemanticsNode().boundsInRoot

        e1rm.performTouchInput { click(Offset(e1rmBounds.width - 1f, e1rmBounds.height / 2f)) }
        e1rm.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                e1rmChartSelectionDescription(trend, 0),
            ),
        )

        val weekly = compose.onNodeWithContentDescription(weeklyVolumeChartSummary(weeks))
        val weeklyBounds = weekly.fetchSemanticsNode().boundsInRoot
        weekly.performTouchInput { click(Offset(weeklyBounds.width - 1f, weeklyBounds.height / 2f)) }
        weekly.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                weeklyVolumeChartSelectionDescription(weeks, 0),
            ),
        )
    }

    @Test
    fun rtlKeyboard_horizontalArrowsFollowVisualDirection() {
        val trend = trendPoints(4)
        setProgressContent(
            trend = trend,
            weeks = emptyList(),
            layoutDirection = LayoutDirection.Rtl,
        )
        val chart = compose.onNodeWithContentDescription(e1rmChartSummary("Back Squat", trend))

        chart.performSemanticsAction(SemanticsActions.RequestFocus)
        chart.performKeyInput { pressKey(Key.MoveHome) }
        chart.performKeyInput { pressKey(Key.DirectionLeft) }
        chart.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                e1rmChartSelectionDescription(trend, 1),
            ),
        )
        chart.performKeyInput { pressKey(Key.DirectionRight) }
        chart.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                e1rmChartSelectionDescription(trend, 0),
            ),
        )
    }

    @Test
    fun keyboardFocus_activatesTheVisibleChartFocusRingState() {
        val trend = trendPoints(3)
        setProgressContent(trend = trend, weeks = emptyList())
        val chart = compose.onNodeWithContentDescription(e1rmChartSummary("Back Squat", trend))

        chart.performSemanticsAction(SemanticsActions.RequestFocus)
        compose.waitForIdle()

        compose.onNodeWithTag(FOCUSED_PROGRESS_CHART_TEST_TAG).assertExists()
        chart.assert(SemanticsMatcher.expectValue(SemanticsProperties.Focused, true))
    }

    @Test
    fun selectedDateIdentity_survivesInsertionBeforeThePoint() {
        val initial = trendPoints(3)
        var trend by mutableStateOf(initial)
        compose.setContent {
            MusFitTheme {
                TrainingProgressContent(
                    data = TrainingProgressContentData(
                        progress = progress(trend),
                        period = TrainingProgressPeriod.Year,
                        weeklyVolume = emptyList(),
                        recentPrs = emptyList(),
                    ),
                    accent = accent,
                    actions = TrainingProgressContentActions(onOpenAllExercises = {}),
                )
            }
        }
        val chart = compose.onNodeWithContentDescription(e1rmChartSummary("Back Squat", initial))
        invokeCustomAction(chart, "Previous data point")
        val selectedDate = initial[1].dateEpochDay

        val inserted = TrainingTrendPoint(
            dateEpochDay = today.minusDays(3).toEpochDay(),
            volumeKg = 700.0,
            bestEstimatedOneRepMaxKg = 97.0,
        )
        trend = listOf(initial[2], inserted, initial[0], initial[1])
        compose.waitForIdle()
        val sorted = (initial + inserted).sortedBy { it.dateEpochDay }

        compose.onNodeWithContentDescription(e1rmChartSummary("Back Squat", sorted)).assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                e1rmChartSelectionDescription(sorted, sorted.indexOfFirst { it.dateEpochDay == selectedDate }),
            ),
        )
    }

    @Test
    fun dataTable_usesTabsLazyRowsAndCompleteAssociations() {
        val trend = trendPoints(365)
        val weeks = volumeWeeks(6)
        setProgressContent(trend = trend, weeks = weeks)

        val dataButton = compose.onNodeWithContentDescription("View progress data")
        assertTrue(dataButton.fetchSemanticsNode().boundsInRoot.height >= 48f)
        dataButton.performClick()
        compose.onNodeWithText("Progress data").assertExists()
        compose.onNodeWithText("e1RM").assertExists()
        compose.onNode(weeklyVolumeTab).assertExists()

        val newestTrendRow = e1rmDataRowDescription(trend.last())
        val newestRow = compose.onNodeWithContentDescription(newestTrendRow)
            .assertExists()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        assertTrue(newestRow.fetchSemanticsNode().boundsInRoot.height >= 48f)

        val previousPoint = trend[trend.lastIndex - 1]
        val previousRow = compose.onNodeWithContentDescription(e1rmDataRowDescription(previousPoint))
        previousRow.performClick()
        previousRow.assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        newestRow.assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, false))
        compose.onNodeWithText("Progress data").assertExists()

        val oldestTrendRow = e1rmDataRowDescription(trend.first())
        compose.onNodeWithContentDescription(oldestTrendRow).assertDoesNotExist()
        compose.onNode(hasScrollToIndexAction())
            .performScrollToNode(hasContentDescription(oldestTrendRow))
        compose.onNodeWithContentDescription(oldestTrendRow).assertExists()

        compose.onNode(weeklyVolumeTab).performClick()
        val latestWeekRow = compose.onNodeWithContentDescription(weeklyVolumeDataRowDescription(weeks.last()))
            .assertExists()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        val previousWeek = weeks[weeks.lastIndex - 1]
        compose.onNodeWithContentDescription(weeklyVolumeDataRowDescription(previousWeek))
            .performClick()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        latestWeekRow.assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, false))

        compose.onNodeWithContentDescription("Close progress data").performClick()
        compose.onNodeWithText("Progress data").assertDoesNotExist()
        compose.onNodeWithContentDescription(e1rmChartSummary("Back Squat", trend)).assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                e1rmChartSelectionDescription(trend, trend.lastIndex - 1),
            ),
        )
        compose.onNodeWithContentDescription(weeklyVolumeChartSummary(weeks)).assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                weeklyVolumeChartSelectionDescription(weeks, weeks.lastIndex - 1),
            ),
        )
    }

    @Test
    fun emptyCharts_haveSafeBoundariesAndTableMessages() {
        setProgressContent(trend = emptyList(), weeks = emptyList(), includeProgress = false)
        compose.onAllNodes(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image),
        ).assertCountEquals(2)
        val emptyE1rm = compose.onNodeWithContentDescription("Estimated one rep max chart. No data.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "No data"))
        compose.onNodeWithContentDescription("Weekly training volume chart. No data.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "No data"))
        assertEquals(
            listOf("Show data table"),
            customActionLabels(emptyE1rm.fetchSemanticsNode().config[SemanticsActions.CustomActions]),
        )
        invokeCustomAction(emptyE1rm, "Show data table")
        compose.onNodeWithText("No e1RM data yet.").assertExists()
        compose.onNode(weeklyVolumeTab).performClick()
        compose.onNodeWithText("No weekly volume data yet.").assertExists()
    }

    @Test
    fun singlePointCharts_onlyExposeTheTableAction() {
        val singleTrend = trendPoints(1)
        val singleWeek = volumeWeeks(1)
        setProgressContent(trend = singleTrend, weeks = singleWeek)
        val e1rmActions = compose
            .onNodeWithContentDescription(e1rmChartSummary("Back Squat", singleTrend))
            .fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
        val weeklyActions = compose
            .onNodeWithContentDescription(weeklyVolumeChartSummary(singleWeek))
            .fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
        assertEquals(listOf("Show data table"), customActionLabels(e1rmActions))
        assertEquals(listOf("Show data table"), customActionLabels(weeklyActions))
    }

    private fun setProgressContent(
        trend: List<TrainingTrendPoint>,
        weeks: List<WeeklyTrainingVolume>,
        includeProgress: Boolean = true,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ) {
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                MusFitTheme {
                    TrainingProgressContent(
                        data = TrainingProgressContentData(
                            progress = if (includeProgress) progress(trend) else null,
                            period = TrainingProgressPeriod.Year,
                            weeklyVolume = weeks,
                            recentPrs = emptyList(),
                        ),
                        accent = accent,
                        actions = TrainingProgressContentActions(onOpenAllExercises = {}),
                    )
                }
            }
        }
    }

    private fun trendPoints(count: Int): List<TrainingTrendPoint> = List(count) { index ->
        val daysAgo = count - index - 1L
        TrainingTrendPoint(
            dateEpochDay = today.minusDays(daysAgo).toEpochDay(),
            volumeKg = 1_000.0 + index,
            bestEstimatedOneRepMaxKg = 100.0 + index,
        )
    }

    private fun volumeWeeks(count: Int): List<WeeklyTrainingVolume> = List(count) { index ->
        WeeklyTrainingVolume(
            weekStartEpochDay = today.minusWeeks((count - index - 1).toLong()).toEpochDay(),
            workoutCount = index + 1,
            completedSetCount = (index + 1) * 4,
            totalVolumeKg = 800.0 + index * 600.0,
        )
    }

    private fun progress(trend: List<TrainingTrendPoint>): ExerciseProgress = ExerciseProgress(
        exerciseId = "back-squat",
        exerciseName = "Back Squat",
        equipment = "Barbell",
        targetMuscles = "Legs",
        heaviestWeightKg = 120.0,
        maxReps = 8,
        bestEstimatedOneRepMaxKg = trend.lastOrNull()?.bestEstimatedOneRepMaxKg ?: 0.0,
        bestWorkoutVolumeKg = 2_000.0,
        trend = trend,
    )

    private fun e1rmChartSummary(exerciseName: String?, trend: List<TrainingTrendPoint>): String = typedE1rmChartSummary(exerciseName, trend).resolveForTest()

    private fun e1rmChartSelectionDescription(trend: List<TrainingTrendPoint>, selectedIndex: Int): String = typedE1rmChartSelectionDescription(trend, selectedIndex).resolveForTest()

    private fun weeklyVolumeChartSummary(weeks: List<WeeklyTrainingVolume>): String = typedWeeklyVolumeChartSummary(weeks).resolveForTest()

    private fun weeklyVolumeChartSelectionDescription(weeks: List<WeeklyTrainingVolume>, selectedIndex: Int): String = typedWeeklyVolumeChartSelectionDescription(weeks, selectedIndex).resolveForTest()

    private fun e1rmDataRowDescription(point: TrainingTrendPoint): String = typedE1rmDataRowDescription(point).resolveForTest()

    private fun weeklyVolumeDataRowDescription(week: WeeklyTrainingVolume): String = typedWeeklyVolumeDataRowDescription(week).resolveForTest()

    private fun UiText.resolveForTest(): String = resolve(ApplicationProvider.getApplicationContext<android.content.Context>().resources)

    private fun customActionLabels(
        actions: List<androidx.compose.ui.semantics.CustomAccessibilityAction>,
    ): List<String> = actions.map { it.label }

    private fun invokeCustomAction(
        node: androidx.compose.ui.test.SemanticsNodeInteraction,
        label: String,
    ) {
        val action = node
            .fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
            .singleOrNull { it.label == label }
        compose.runOnIdle {
            checkNotNull(action).action()
        }
    }
}
