package com.musfit.ui

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.runtime.MutableState
import com.musfit.ui.profile.ProfileSettingsUiState
import com.musfit.ui.profile.SettingsHub
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.tabAccentFor
import com.musfit.ui.training.TrainingHomeContent
import org.junit.Assert.assertEquals
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

        assertEquals(listOf("new-routine", "start-empty"), actions)
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

        assertEquals(listOf("profile", "coach", "health", "transfer"), actions)
    }

    @Test
    fun visitOrder_survivesSavedInstanceStateRestoration() {
        val restoration = StateRestorationTester(compose)
        lateinit var entries: MutableState<List<AppDestination>>
        restoration.setContent {
            entries = rememberAppBackStackEntries()
        }

        compose.runOnIdle {
            entries.value = listOf(AppDestination.Today, AppDestination.Food, AppDestination.Training)
        }

        restoration.emulateSavedInstanceStateRestore()

        compose.runOnIdle {
            assertEquals(
                listOf(AppDestination.Today, AppDestination.Food, AppDestination.Training),
                entries.value,
            )
        }
    }
}
