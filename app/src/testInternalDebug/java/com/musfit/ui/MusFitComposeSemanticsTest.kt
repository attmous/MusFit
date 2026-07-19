package com.musfit.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.musfit.data.repository.ExerciseSummary
import com.musfit.data.repository.RoutineDetail
import com.musfit.data.repository.RoutineExerciseDetail
import com.musfit.ui.components.MusFitSegmented
import com.musfit.ui.food.FoodDatabaseNavKey
import com.musfit.ui.food.FoodDetailNavKey
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.tabAccentFor
import com.musfit.ui.training.ExerciseGif
import com.musfit.ui.training.ExerciseThumb
import com.musfit.ui.training.RoutineDetailContent
import com.musfit.ui.training.TrainingHomeContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MusFitComposeSemanticsTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun bottomNavigation_dispatchesVisitsAndExposesSelectionSemantics() {
        val visits = mutableListOf<AppDestination>()

        compose.setContent {
            MusFitTheme {
                MusFitBottomNav(
                    destinations = AppDestination.entries,
                    currentRoute = AppDestination.Today.route,
                    onSelect = visits::add,
                    onCoachClick = {},
                )
            }
        }

        compose.onAllNodesWithContentDescription("Today").assertCountEquals(1)
        compose.onNodeWithContentDescription("Today")
            .assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .also(::assertMinimumTouchHeight)
        compose.onNodeWithContentDescription("Food").performClick()
        compose.onNodeWithContentDescription("Training").performClick()

        assertEquals(listOf(AppDestination.Food, AppDestination.Training), visits)
    }

    @Test
    fun rootNavigationLayout_usesCompactRailAndWideBreakpoints() {
        assertEquals(RootNavigationLayout.Compact, rootNavigationLayoutForWidth(599.dp))
        assertEquals(RootNavigationLayout.Rail, rootNavigationLayoutForWidth(600.dp))
        assertEquals(RootNavigationLayout.Rail, rootNavigationLayoutForWidth(839.dp))
        assertEquals(RootNavigationLayout.Wide, rootNavigationLayoutForWidth(840.dp))
    }

    @Test
    fun rootNavigationLayoutChange_preservesNestedDestinationState() {
        val layout = mutableStateOf(RootNavigationLayout.Wide)
        lateinit var foodEntries: MutableList<NavKey>

        compose.setContent {
            MusFitTheme {
                RootNavigationScaffold(
                    layout = layout.value,
                    state = RootNavigationState(
                        destinations = AppDestination.entries,
                        currentRoute = AppDestination.Food.route,
                        chromeVisible = true,
                    ),
                    callbacks = RootNavigationCallbacks(onSelect = {}, onCoachClick = {}),
                ) {
                    foodEntries = rememberNavBackStack(FoodDatabaseNavKey)
                }
            }
        }

        compose.runOnIdle { foodEntries += FoodDetailNavKey("almonds") }
        compose.runOnIdle {
            assertEquals(listOf(FoodDatabaseNavKey, FoodDetailNavKey("almonds")), foodEntries)
        }

        compose.runOnIdle { layout.value = RootNavigationLayout.Compact }
        compose.runOnIdle {
            assertEquals(listOf(FoodDatabaseNavKey, FoodDetailNavKey("almonds")), foodEntries)
        }
    }

    @Test
    fun adaptiveRail_dispatchesDestinationsAndCoachWithSelectedTargetSemantics() {
        val visits = mutableListOf<String>()
        compose.setContent {
            MusFitTheme {
                RootNavigationScaffold(
                    layout = RootNavigationLayout.Rail,
                    state = RootNavigationState(
                        destinations = AppDestination.entries,
                        currentRoute = AppDestination.Today.route,
                        chromeVisible = true,
                    ),
                    callbacks = RootNavigationCallbacks(
                        onSelect = { visits += it.route },
                        onCoachClick = { visits += "coach" },
                    ),
                    content = {},
                )
            }
        }

        compose.onNodeWithText("Today").assertIsSelected().also(::assertMinimumTouchHeight)
        compose.onNodeWithText("Food").performClick()
        compose.onNodeWithText("Coach").performClick()

        assertEquals(listOf("food", "coach"), visits)
    }

    @Test
    fun adaptiveChrome_hidden_removesNavigationForCameraContent() {
        compose.setContent {
            MusFitTheme {
                RootNavigationScaffold(
                    layout = RootNavigationLayout.Wide,
                    state = RootNavigationState(
                        destinations = AppDestination.entries,
                        currentRoute = AppDestination.Food.route,
                        chromeVisible = false,
                    ),
                    callbacks = RootNavigationCallbacks(onSelect = {}, onCoachClick = {}),
                    content = {},
                )
            }
        }

        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Food").fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithText("Coach").assertDoesNotExist()
    }

    @Test
    fun sharedSegmentedControl_exposesSingleSelectedRadioAndMinimumTarget() {
        compose.setContent {
            MusFitTheme {
                MusFitSegmented(
                    options = listOf("Diary", "Summary"),
                    selected = "Diary",
                    accent = tabAccentFor(AppDestination.Food),
                    label = { it },
                    onSelect = {},
                )
            }
        }

        compose.onNodeWithText("Diary")
            .assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .also(::assertMinimumTouchHeight)
        compose.onAllNodesWithText("Diary").assertCountEquals(1)
    }

    @Test
    fun trainingHome_dispatchesFakeBackedPrimaryActions() {
        val actions = mutableListOf<String>()

        compose.setContent {
            MusFitTheme {
                TrainingHomeContent(
                    accent = tabAccentFor(AppDestination.Training),
                    onStartBlankWorkout = { actions += "start-empty" },
                    onNewRoutine = { actions += "new-routine" },
                    onOpenLibrary = { actions += "open-library" },
                )
            }
        }

        compose.onNodeWithText("New routine").performClick()
        compose.onNodeWithText("empty workout").performClick()
        compose.onNodeWithText("No workouts yet").assertDoesNotExist()
        compose.onNodeWithText("whenever you're ready", substring = true).assertDoesNotExist()

        assertEquals(listOf("new-routine", "start-empty"), actions)
    }

    @Test
    fun routineDetail_rendersAccessibleStaticThumbnailAndDispatchesExerciseTarget() {
        val opened = mutableListOf<String>()
        val exercise = ExerciseSummary(
            id = "bench",
            name = "Bench press",
            category = "Strength",
            equipment = "Barbell",
            targetMuscles = "Chest",
            isCustom = false,
            imageUrl = null,
        )

        compose.setContent {
            MusFitTheme {
                RoutineDetailContent(
                    detail = RoutineDetail(
                        id = "push",
                        name = "Push day",
                        notes = null,
                        isStarter = false,
                        exercises = listOf(
                            RoutineExerciseDetail(
                                id = "routine-bench",
                                exercise = exercise,
                                sortOrder = 0,
                                targetSets = 3,
                                targetReps = "5",
                            ),
                        ),
                    ),
                    accent = tabAccentFor(AppDestination.Training),
                    onStart = {},
                    onEdit = {},
                    onOpenExercise = { id, target -> opened += "$id:$target" },
                    onDuplicate = {},
                    onDelete = {},
                    onClose = {},
                )
            }
        }

        compose.onNodeWithText("Bench press").performClick()

        assertEquals(listOf("bench:3 x 5"), opened)
    }

    @Test
    fun exerciseMediaComponents_exposeStaticAndAnimatedDemoSemantics() {
        compose.setContent {
            MusFitTheme {
                ExerciseThumb(
                    imageUrl = null,
                    contentDescription = "Static exercise demo",
                    accent = tabAccentFor(AppDestination.Training),
                    animateGif = false,
                )
                ExerciseGif(
                    gifUrl = "file:///does-not-exist.gif",
                    contentDescription = "Animated exercise demo",
                    accent = tabAccentFor(AppDestination.Training),
                )
            }
        }

        compose.onNodeWithContentDescription("Static exercise demo").fetchSemanticsNode()
        waitForContentDescription("Animated exercise demo")
        compose.onNodeWithContentDescription("Animated exercise demo").fetchSemanticsNode()
    }

    @Test
    fun trainingHome_withActiveWorkout_hidesConflictingStartAction() {
        compose.setContent {
            MusFitTheme {
                TrainingHomeContent(
                    hasActiveWorkout = true,
                    accent = tabAccentFor(AppDestination.Training),
                    onStartBlankWorkout = {},
                    onNewRoutine = {},
                    onOpenLibrary = {},
                )
            }
        }

        compose.onNodeWithText("New routine").assertIsDisplayed()
        compose.onNodeWithText("empty workout").assertDoesNotExist()
    }

    @Test
    fun visitOrder_survivesSavedInstanceStateRestoration() {
        val restoration = StateRestorationTester(compose)
        lateinit var entries: MutableList<NavKey>
        restoration.setContent {
            entries = rememberNavBackStack(TodayNavKey)
        }

        compose.runOnIdle {
            entries += listOf(FoodNavKey, ProfileNavKey, ProfileSettingsNavKey)
        }

        restoration.emulateSavedInstanceStateRestore()

        compose.runOnIdle {
            assertEquals(
                listOf(TodayNavKey, FoodNavKey, ProfileNavKey, ProfileSettingsNavKey),
                entries,
            )
        }
    }

    private fun assertMinimumTouchHeight(interaction: SemanticsNodeInteraction) {
        val minimum = with(compose.density) { 48.dp.toPx() }
        val actual = interaction.fetchSemanticsNode().boundsInRoot.height
        assertTrue("Expected a 48dp touch target but was ${actual}px", actual >= minimum)
    }

    private fun waitForContentDescription(description: String) {
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
