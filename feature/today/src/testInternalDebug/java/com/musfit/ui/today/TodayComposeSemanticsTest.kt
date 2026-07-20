package com.musfit.ui.today

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import com.musfit.data.repository.CoachMessage
import com.musfit.domain.coach.CoachAction
import com.musfit.domain.coach.CoachMessageCategory
import com.musfit.domain.today.TodayMetric
import com.musfit.ui.text.UiText
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccentRole
import com.musfit.ui.theme.tabAccentFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w900dp-h1200dp-mdpi")
class TodayComposeSemanticsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun coachCard_withoutAction_mergesCopyWithoutExposingFakeClick() {
        val dismissed = mutableListOf<Long>()
        setCoachFeed(message(action = null, isRead = false), onDismiss = dismissed::add)

        val card = compose.onNodeWithText(TITLE)
        card.assert(hasClickAction().not())
        card.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                "Unread",
            ),
        )
        assertEquals(
            listOf("Dismiss message"),
            customActionLabels(card),
        )

        val mergedText = card.fetchSemanticsNode().config[SemanticsProperties.Text].map { it.text }
        assertTrue(mergedText.contains(TITLE))
        assertTrue(mergedText.contains(BODY))
        assertFalse(mergedText.contains("Open Food"))

        invokeCustomAction(card, "Dismiss message")
        assertEquals(listOf(MESSAGE_ID), dismissed)
    }

    @Test
    fun coachCard_withAction_exposesOneButtonWithMinimumTouchTarget() {
        val actions = mutableListOf<CoachAction>()
        setCoachFeed(message(action = CoachAction.OpenFood), onAction = actions::add)

        compose.onAllNodes(hasClickAction()).assertCountEquals(1)
        compose.onNodeWithText(TITLE).assert(hasClickAction().not())

        val action = compose.onNodeWithText("Open Food")
            .assert(hasClickAction())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        assertMinimumTouchTarget(action)
        action.performTouchInput { click() }

        assertEquals(listOf(CoachAction.OpenFood), actions)
    }

    @Test
    fun coachCard_keepsLongPressDismissGesture() {
        val dismissed = mutableListOf<Long>()
        setCoachFeed(message(action = null), onDismiss = dismissed::add)

        compose.onNodeWithText(TITLE).performTouchInput { longClick() }

        assertEquals(listOf(MESSAGE_ID), dismissed)
    }

    @Test
    fun coachCard_keepsSwipeDismissGesture() {
        val dismissed = mutableListOf<Long>()
        setCoachFeed(message(action = null), onDismiss = dismissed::add)

        compose.onNodeWithText(TITLE).performTouchInput { swipeLeft() }
        compose.waitUntil(timeoutMillis = 5_000) { dismissed.isNotEmpty() }

        assertEquals(listOf(MESSAGE_ID), dismissed)
    }

    @Test
    fun coachFab_isNamedButtonWithMinimumTouchTarget() {
        compose.setContent {
            MusFitTheme { ChatPreviewFab(onClick = {}) }
        }

        val fab = compose.onNodeWithContentDescription("Ask coach")
            .assert(hasClickAction())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        assertMinimumTouchTarget(fab)
    }

    @Test
    fun readinessChip_isNamedButtonWithMinimumTouchTarget() {
        compose.setContent {
            MusFitTheme {
                ReadinessHeaderChip(
                    readiness = TodayReadinessUiState(score = 82, levelLabel = UiText.Verbatim("Good")),
                    onClick = {},
                    accent = tabAccentFor(TabAccentRole.Today),
                )
            }
        }

        val chip = compose.onNodeWithContentDescription("Readiness estimate 82, Good")
            .assert(hasClickAction())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        assertMinimumTouchTarget(chip)
    }

    @Test
    fun vitalsTile_isNamedButtonWithMinimumTouchTarget() {
        val opened = mutableListOf<TodayMetric>()
        compose.setContent {
            MusFitTheme {
                TodayVitalsGrid(
                    vitals = listOf(
                        MetricCardUiState(
                            metric = TodayMetric.Calories,
                            label = UiText.Verbatim("Calories"),
                            value = MetricValueUiState.WithGoal(
                                figure = UiText.Verbatim("10"),
                                caption = UiText.Verbatim("of 20 kcal"),
                                progress = 0.5f,
                            ),
                        ),
                    ),
                    onMetricClick = opened::add,
                )
            }
        }

        val tile = compose.onNodeWithContentDescription("Calories: 10 of 20 kcal")
            .assert(hasClickAction())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        assertMinimumTouchTarget(tile)
        tile.performClick()
        assertEquals(listOf(TodayMetric.Calories), opened)
    }

    @Test
    fun coachComposer_keepsInputNameAfterTextEntryAndExposesSendButton() {
        compose.setContent {
            MusFitTheme {
                CoachChatComposer(
                    input = "How should I train today?",
                    isSending = false,
                    onInputChanged = {},
                    onSend = {},
                )
            }
        }

        val input = compose.onNodeWithContentDescription("Coach message")
            .assert(hasSetTextAction())
        assertMinimumTouchTarget(input)

        val send = compose.onNodeWithContentDescription("Send coach message")
            .assert(hasClickAction())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        assertMinimumTouchTarget(send)
    }

    @Test
    fun dashboardRows_exposeCheckboxStateAndNamedReorderButtons() {
        val toggled = mutableListOf<TodayMetric>()
        val moved = mutableListOf<Pair<TodayMetric, Boolean>>()
        compose.setContent {
            MusFitTheme {
                DashboardEditSheet(
                    state = TodayUiState(
                        editPins = listOf(TodayMetric.Calories, TodayMetric.Steps),
                        stepGoalInput = "10000",
                        sessionTargetInput = "3",
                    ),
                    onTogglePin = toggled::add,
                    onMovePin = { metric, up -> moved += metric to up },
                    onStepGoalChanged = {},
                    onSessionTargetChanged = {},
                    onSave = {},
                    onDismiss = {},
                )
            }
        }

        val calories = compose.onNodeWithText("Eaten")
            .assert(hasClickAction())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
        assertMinimumTouchTarget(calories)
        calories.performClick()
        assertEquals(listOf(TodayMetric.Calories), toggled)

        val moveUp = compose.onNodeWithContentDescription("Move Eaten up").assertIsNotEnabled()
        assertMinimumTouchTarget(moveUp)
        val moveDown = compose.onNodeWithContentDescription("Move Eaten down")
            .assert(hasClickAction())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        assertMinimumTouchTarget(moveDown)
        moveDown.performClick()
        assertEquals(listOf(TodayMetric.Calories to false), moved)
    }

    private fun setCoachFeed(
        message: CoachMessage,
        onAction: (CoachAction) -> Unit = {},
        onDismiss: (Long) -> Unit = {},
    ) {
        compose.setContent {
            MusFitTheme {
                CoachFeed(
                    groups = listOf(CoachFeedDayGroup(label = UiText.Verbatim("Today"), messages = listOf(message))),
                    onAction = onAction,
                    onDismiss = onDismiss,
                )
            }
        }
    }

    private fun message(
        action: CoachAction?,
        isRead: Boolean = true,
    ) = CoachMessage(
        id = MESSAGE_ID,
        day = LocalDate.of(2026, 7, 20),
        ruleKey = "test-message",
        category = CoachMessageCategory.Nutrition,
        title = TITLE,
        body = BODY,
        action = action,
        firstSeenAtEpochMillis = 0L,
        isRead = isRead,
        source = "test",
    )

    private fun assertMinimumTouchTarget(interaction: SemanticsNodeInteraction) {
        val minimum = with(compose.density) { 48.dp.toPx() }
        val bounds = interaction.fetchSemanticsNode().boundsInRoot
        assertTrue("Expected width >= 48dp but was ${bounds.width}px", bounds.width >= minimum)
        assertTrue("Expected height >= 48dp but was ${bounds.height}px", bounds.height >= minimum)
    }

    private fun customActionLabels(node: SemanticsNodeInteraction): List<String> = node.fetchSemanticsNode().config.getOrNull(SemanticsActions.CustomActions).orEmpty().map { it.label }

    private fun invokeCustomAction(node: SemanticsNodeInteraction, label: String) {
        val action = node.fetchSemanticsNode()
            .config
            .getOrNull(SemanticsActions.CustomActions)
            ?.singleOrNull { it.label == label }
        compose.runOnIdle { checkNotNull(action).action() }
    }

    private companion object {
        const val MESSAGE_ID = 42L
        const val TITLE = "Protein is tracking low"
        const val BODY = "A protein-rich snack would close the gap."
    }
}
