package com.musfit.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.musfit.data.repository.AiCoachProviderKind
import com.musfit.data.repository.ExerciseSummary
import com.musfit.data.repository.LocalAgentKind
import com.musfit.data.repository.RoutineDetail
import com.musfit.data.repository.RoutineExerciseDetail
import com.musfit.domain.health.StepSource
import com.musfit.ui.profile.AiCoachEditorSheet
import com.musfit.ui.profile.ConnectedSegmentRow
import com.musfit.ui.profile.HealthConnectSettingsPage
import com.musfit.ui.profile.ProfileFieldTile
import com.musfit.ui.profile.ProfileSettingsUiState
import com.musfit.ui.profile.SettingsHub
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

        compose.onNodeWithContentDescription("Today").assertIsSelected()
        compose.onNodeWithContentDescription("Food").performClick()
        compose.onNodeWithContentDescription("Training").performClick()

        assertEquals(listOf(AppDestination.Food, AppDestination.Training), visits)
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
    fun connectedSegments_exposeRadioSelectionAndMinimumTouchHeight() {
        val selections = mutableListOf<String>()

        compose.setContent {
            MusFitTheme {
                ConnectedSegmentRow(
                    options = listOf("Metric", "Imperial"),
                    selected = "Metric",
                    label = { it },
                    accent = tabAccentFor(AppDestination.Profile),
                    onSelect = selections::add,
                )
            }
        }

        compose.onNodeWithText("Metric").assertIsSelected()
        val imperial = compose.onNodeWithText("Imperial")
        assertMinimumTouchHeight(imperial)
        imperial.performClick()

        assertEquals(listOf("Imperial"), selections)
    }

    @Test
    fun healthStepSources_exposeSelectionAndDisableChangesWhileSyncing() {
        compose.setContent {
            MusFitTheme {
                HealthConnectSettingsPage(
                    state = ProfileSettingsUiState(
                        availabilityLabel = "Available",
                        isHealthConnectSyncing = true,
                        stepSources = listOf(StepSource("phone", "Phone", 1_234)),
                    ),
                    accent = tabAccentFor(AppDestination.Profile),
                    onBack = {},
                    onRequestPermissions = {},
                    onRefresh = {},
                    onSync = {},
                    onExport = {},
                    onSelectStepSource = {},
                )
            }
        }

        compose.onNodeWithText("All sources (unified)").assertIsSelected().assertIsNotEnabled()
        compose.onNodeWithText("Phone").assertIsNotEnabled()
    }

    @Test
    fun profileField_exposesAssociatedLabelAndMinimumTouchHeight() {
        compose.setContent {
            MusFitTheme {
                ProfileFieldTile(
                    label = "Weight",
                    value = "75",
                    onValueChange = {},
                    unit = "kg",
                )
            }
        }

        val field = compose.onNodeWithContentDescription("Weight, kg")
        field.assertIsDisplayed()
        assertMinimumTouchHeight(field)
    }

    @Test
    @Config(qualifiers = "w1000dp-h1200dp-mdpi")
    fun aiCoachApiKey_exposesLabelPasswordAndMinimumTouchHeight() {
        compose.setContent {
            MusFitTheme {
                AiCoachEditorSheet(
                    provider = AiCoachProviderKind.OpenAiCompatible,
                    baseUrl = "https://example.invalid/v1",
                    modelName = "test-model",
                    localAgentKind = LocalAgentKind.Custom,
                    apiKey = "secret",
                    hasSavedApiKey = false,
                    error = null,
                    accent = tabAccentFor(AppDestination.Profile),
                    onProviderChange = {},
                    onBaseUrlChange = {},
                    onModelNameChange = {},
                    onLocalAgentKindChange = {},
                    onApiKeyChange = {},
                    onDismiss = {},
                    onSave = {},
                )
            }
        }

        waitForContentDescription("API key (optional)")
        val apiKey = compose.onNodeWithContentDescription("API key (optional)")
        apiKey.assert(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.Password),
        )
        assertMinimumTouchHeight(apiKey)
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
    fun settingsHub_dispatchesFakeBackedProfileAndConnectionActions() {
        val actions = mutableListOf<String>()

        compose.setContent {
            MusFitTheme {
                SettingsHub(
                    state = ProfileSettingsUiState(),
                    accent = tabAccentFor(AppDestination.Profile),
                    onBack = { actions += "back" },
                    onEditAccount = { actions += "edit-account" },
                    onOpenProfileDetails = { actions += "profile" },
                    onGoogleSignIn = { actions += "google" },
                    onGitHubSignIn = { actions += "github" },
                    onOpenAiCoach = { actions += "coach" },
                    onOpenHealthConnect = { actions += "health" },
                    onOpenDataTransfer = { actions += "transfer" },
                    onIncludeBurnedCaloriesChange = { actions += "burned:$it" },
                )
            }
        }

        compose.onNodeWithText("Profile details").performScrollTo().performClick()
        compose.onNodeWithText("AI coach").performScrollTo().performClick()
        compose.onNodeWithText("Health Connect").performScrollTo().performClick()
        compose.onNodeWithText("Data transfer").performScrollTo().performClick()
        compose.onNodeWithText("Data & privacy").performScrollTo().performClick()

        assertEquals(
            listOf("profile", "coach", "health", "transfer", "transfer"),
            actions,
        )
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
