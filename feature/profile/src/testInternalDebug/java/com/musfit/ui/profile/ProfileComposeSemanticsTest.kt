package com.musfit.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.musfit.data.repository.AccountErasureScope
import com.musfit.data.repository.AiCoachProviderKind
import com.musfit.data.repository.DEFAULT_USER_PROFILE
import com.musfit.data.repository.LocalAgentKind
import com.musfit.domain.health.StepSource
import com.musfit.domain.profile.GoalType
import com.musfit.domain.profile.Sex
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
    fun clickableHeroChip_exposesOneEditButtonWithMinimumTouchSize() {
        var clicks = 0
        compose.setContent {
            MusFitTheme {
                HeroChip(
                    text = "Edit",
                    accent = tabAccentFor(TabAccentRole.Profile),
                    onClick = { clicks += 1 },
                )
            }
        }

        val edit = assertSingleButtonWithText("Edit")
        edit.performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun profileHubRow_exposesOneButtonWithMinimumTouchSize() {
        var clicks = 0
        compose.setContent {
            MusFitTheme {
                ProfileHubRow(
                    title = "Profile details",
                    subtitle = "Goals and body details",
                    shape = RoundedCornerShape(24.dp),
                    onClick = { clicks += 1 },
                )
            }
        }

        val row = assertSingleButtonWithText("Profile details")
        row.performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun profileActionPills_exposeOneButtonEachWithMinimumTouchSize() {
        val actions = mutableListOf<String>()
        compose.setContent {
            MusFitTheme {
                Column {
                    GroupLabel(
                        text = "Connection",
                        actionLabel = "Edit",
                        onAction = { actions += "edit" },
                    )
                    HeroActionPill(
                        text = "Test",
                        accent = tabAccentFor(TabAccentRole.Profile),
                        onClick = { actions += "test" },
                    )
                    TonalActionPill(
                        text = "Refresh",
                        icon = Icons.Outlined.Refresh,
                        accent = tabAccentFor(TabAccentRole.Profile),
                        onClick = { actions += "refresh" },
                    )
                }
            }
        }

        assertSingleButtonWithText("Edit").performClick()
        assertSingleButtonWithText("Test").performClick()
        assertSingleButtonWithText("Refresh").performClick()
        assertEquals(listOf("edit", "test", "refresh"), actions)
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

        compose.onNodeWithText("Cancel").performScrollTo()
        assertSingleButtonWithText("Cancel")
    }

    @Test
    @Config(qualifiers = "w1000dp-h1200dp-mdpi")
    fun aiCoachClear_exposesOneButtonWithMinimumTouchSize() {
        compose.setContent {
            MusFitTheme {
                AiCoachSettingsPage(
                    state = ProfileSettingsUiState(
                        aiCoach = AiCoachSettingsUiState(
                            providerKind = AiCoachProviderKind.OpenAiCompatible,
                            hasApiKey = true,
                        ),
                    ),
                    accent = tabAccentFor(TabAccentRole.Profile),
                    onBack = {},
                    onEdit = {},
                    onClearApiKey = {},
                    onTestConnection = {},
                )
            }
        }

        waitForText("Clear")
        compose.onNodeWithText("Clear").performScrollTo()
        assertSingleButtonWithText("Clear")
    }

    @Test
    @Config(qualifiers = "w1000dp-h1200dp-mdpi")
    fun accountEditorCancel_exposesOneButtonWithMinimumTouchSize() {
        compose.setContent {
            MusFitTheme {
                AccountEditSheet(
                    name = "Test user",
                    email = "",
                    error = null,
                    accent = tabAccentFor(TabAccentRole.Profile),
                    onNameChange = {},
                    onEmailChange = {},
                    onDismiss = {},
                    onSave = {},
                )
            }
        }

        waitForText("Cancel")
        compose.onNodeWithText("Cancel").performScrollTo()
        assertSingleButtonWithText("Cancel")
    }

    @Test
    @Config(qualifiers = "w1000dp-h1200dp-mdpi")
    fun accountErasureHealthRecords_exposesOneLabelledCheckboxTarget() {
        val changes = mutableListOf<Boolean>()
        val label = "Also delete records MusFit authored in Health Connect"
        compose.setContent {
            MusFitTheme {
                AccountErasureDialog(
                    state = ProfileSettingsUiState(
                        accountErasureScope = AccountErasureScope.ActiveAccount,
                    ),
                    onDeleteAuthoredHealthRecordsChange = changes::add,
                    onDismiss = {},
                    onConfirm = {},
                )
            }
        }

        waitForText(label)
        val checkbox = assertSingleInteractiveWithText(label, Role.Checkbox)
        checkbox.performClick()
        assertEquals(listOf(true), changes)
    }

    @Test
    @Config(qualifiers = "w1000dp-h1600dp-mdpi")
    fun profileEditorActions_exposeOneButtonEachWithMinimumTouchSize() {
        compose.setContent {
            MusFitTheme {
                ProfileEditSheet(
                    initial = DEFAULT_USER_PROFILE.copy(
                        sex = Sex.Male,
                        birthDateEpochDay = 0L,
                        heightCm = 180.0,
                        goalType = GoalType.Lose,
                        goalPaceKgPerWeek = 0.3,
                        goalWeightKg = 70.0,
                    ),
                    initialWeightKg = 75.0,
                    onDismiss = {},
                    onSave = { _, _ -> },
                    onApplyTargets = {},
                )
            }
        }

        waitForContentDescription("Close")
        assertSingleButtonWithContentDescription("Close")

        listOf("Decrease pace", "Increase pace").forEach { description ->
            waitForContentDescription(description)
            compose.onNodeWithContentDescription(description).performScrollTo()
            assertSingleButtonWithContentDescription(description)
        }

        waitForText("Apply to Food")
        compose.onNodeWithText("Apply to Food").performScrollTo()
        assertSingleButtonWithText("Apply to Food")

        waitForText("Cancel")
        compose.onNodeWithText("Cancel").performScrollTo()
        assertSingleButtonWithText("Cancel")
    }

    @Test
    @Config(qualifiers = "w1000dp-h1200dp-mdpi")
    fun historyActions_exposeOneButtonEachWithMinimumTouchSize() {
        compose.setContent {
            MusFitTheme {
                MeasurementHistoryScreen(
                    title = "Weight",
                    entries = listOf(
                        HistoryEntry(
                            id = "entry-1",
                            measuredAtEpochMillis = System.currentTimeMillis(),
                            value = 75.0,
                            unit = "kg",
                        ),
                    ),
                    accent = tabAccentFor(TabAccentRole.Profile),
                    onBack = {},
                    onAdd = {},
                    onEdit = { _, _ -> },
                    onDelete = {},
                )
            }
        }

        assertSingleButtonWithContentDescription("Back")
        assertSingleButtonWithContentDescription("Log entry")
        compose.onNodeWithContentDescription("Entry options").performScrollTo()
        assertSingleButtonWithContentDescription("Entry options")
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
        waitForContentDescription("Add burned calories to budget")
        compose.onNodeWithContentDescription("Add burned calories to budget").performScrollTo()
        assertSingleInteractiveWithContentDescription(
            description = "Add burned calories to budget",
            role = Role.Switch,
        ).performClick()
        assertEquals(listOf("profile", "coach", "health", "transfer", "transfer", "burned:true"), actions)
    }

    private fun assertMinimumTouchHeight(interaction: SemanticsNodeInteraction) {
        val minimum = with(compose.density) { 48.dp.toPx() }
        val actual = interaction.fetchSemanticsNode().boundsInRoot.height
        assertTrue("Expected a 48dp touch target but was ${actual}px", actual >= minimum)
    }

    private fun assertMinimumTouchSize(interaction: SemanticsNodeInteraction) {
        val minimum = with(compose.density) { 48.dp.toPx() }
        val bounds = interaction.fetchSemanticsNode().boundsInRoot
        assertTrue("Expected a 48dp-wide touch target but was ${bounds.width}px", bounds.width >= minimum)
        assertTrue("Expected a 48dp-tall touch target but was ${bounds.height}px", bounds.height >= minimum)
    }

    private fun assertInteractiveTarget(
        interaction: SemanticsNodeInteraction,
        role: Role,
    ): SemanticsNodeInteraction {
        interaction.assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
        interaction.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, role))
        assertMinimumTouchSize(interaction)
        return interaction
    }

    private fun assertButton(interaction: SemanticsNodeInteraction): SemanticsNodeInteraction = assertInteractiveTarget(interaction, Role.Button)

    private fun assertSingleInteractiveWithText(
        text: String,
        role: Role,
    ): SemanticsNodeInteraction {
        val nodes = compose.onAllNodesWithText(text).fetchSemanticsNodes()
        assertEquals("Expected one accessible node labelled $text", 1, nodes.size)
        return assertInteractiveTarget(compose.onNodeWithText(text), role)
    }

    private fun assertSingleInteractiveWithContentDescription(
        description: String,
        role: Role,
    ): SemanticsNodeInteraction {
        val nodes = compose.onAllNodesWithContentDescription(description).fetchSemanticsNodes()
        assertEquals("Expected one accessible node labelled $description", 1, nodes.size)
        return assertInteractiveTarget(compose.onNodeWithContentDescription(description), role)
    }

    private fun assertSingleButtonWithText(text: String): SemanticsNodeInteraction {
        val nodes = compose.onAllNodesWithText(text).fetchSemanticsNodes()
        assertEquals("Expected one accessible node labelled $text", 1, nodes.size)
        return assertButton(compose.onNodeWithText(text))
    }

    private fun assertSingleButtonWithContentDescription(description: String): SemanticsNodeInteraction {
        val nodes = compose.onAllNodesWithContentDescription(description).fetchSemanticsNodes()
        assertEquals("Expected one accessible node labelled $description", 1, nodes.size)
        return assertButton(compose.onNodeWithContentDescription(description))
    }

    private fun waitForContentDescription(description: String) {
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForText(text: String) {
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
