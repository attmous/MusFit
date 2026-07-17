package com.musfit.ui.profile

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.musfit.data.repository.AiCoachProviderKind
import com.musfit.data.repository.LocalAgentKind
import com.musfit.domain.health.StepSource
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccentRole
import com.musfit.ui.theme.tabAccentFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProfileComposeSemanticsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun connectedSegments_exposeRadioSelectionAndMinimumTouchHeight() {
        val selections = mutableListOf<String>()
        compose.setContent {
            MusFitTheme {
                ConnectedSegmentRow(
                    options = listOf("Metric", "Imperial"),
                    selected = "Metric",
                    label = { it },
                    accent = tabAccentFor(TabAccentRole.Profile),
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
                    accent = tabAccentFor(TabAccentRole.Profile),
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
                ProfileFieldTile(label = "Weight", value = "75", onValueChange = {}, unit = "kg")
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
                    accent = tabAccentFor(TabAccentRole.Profile),
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
        apiKey.assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
        assertMinimumTouchHeight(apiKey)
    }

    @Test
    fun settingsHub_dispatchesFakeBackedProfileAndConnectionActions() {
        val actions = mutableListOf<String>()
        compose.setContent {
            MusFitTheme {
                SettingsHub(
                    state = ProfileSettingsUiState(),
                    accent = tabAccentFor(TabAccentRole.Profile),
                    onBack = { actions += "back" },
                    onEditAccount = { actions += "edit-account" },
                    onOpenProfileDetails = { actions += "profile" },
                    onGoogleSignIn = { actions += "google" },
                    onGitHubSignIn = { actions += "github" },
                    onOpenAiCoach = { actions += "coach" },
                    onOpenHealthConnect = { actions += "health" },
                    onOpenDataTransfer = { actions += "transfer" },
                    onIncludeBurnedCaloriesChange = { actions += "burned:$it" },
                    googleSignInConfigured = false,
                    versionName = "test",
                )
            }
        }

        compose.onNodeWithText("Profile details").performScrollTo().performClick()
        compose.onNodeWithText("AI coach").performScrollTo().performClick()
        compose.onNodeWithText("Health Connect").performScrollTo().performClick()
        compose.onNodeWithText("Data transfer").performScrollTo().performClick()
        compose.onNodeWithText("Data & privacy").performScrollTo().performClick()
        assertEquals(listOf("profile", "coach", "health", "transfer", "transfer"), actions)
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
