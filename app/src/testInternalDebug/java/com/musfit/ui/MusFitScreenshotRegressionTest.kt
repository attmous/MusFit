package com.musfit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.musfit.domain.today.MetricValue
import com.musfit.domain.today.TodayMetric
import com.musfit.ui.food.AddFoodScreen
import com.musfit.ui.food.BarcodeScannerScreen
import com.musfit.ui.food.FoodUiState
import com.musfit.ui.profile.ProfileSettingsUiState
import com.musfit.ui.profile.SettingsHub
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.tabAccentFor
import com.musfit.ui.today.MetricCardUiState
import com.musfit.ui.today.TodayVitalsGrid
import com.musfit.ui.training.TrainingHomeContent
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "mdpi")
class MusFitScreenshotRegressionTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    @Config(qualifiers = "w400dp-h800dp-mdpi")
    fun foodAdd_phone_light_ltr() = capture("food-add-phone-light-ltr.png", dark = false) {
        AddFoodScreen(
            state = FoodUiState(),
            onBack = {}, onQueryChange = {}, onScanClick = {}, onTabSelected = {},
            onFoodClick = {}, onQuickTrack = {}, onAdjustGoals = {}, onCopyYesterday = {},
            onSaveTemplate = {}, onScanLabel = {}, onProductNameChanged = {}, onBrandChanged = {},
            onQuantityChanged = {}, onAmountServingChoiceSelected = {}, onCaloriesChanged = {},
            onProteinChanged = {}, onCarbsChanged = {}, onFatChanged = {}, onSaveProduct = {},
            onLogFood = {}, onCreateRecipe = {},
        )
    }

    @Test
    @Config(qualifiers = "w400dp-h800dp-mdpi")
    fun training_phone_dark_largeFont() = capture(
        "training-phone-dark-font-150.png",
        dark = true,
        fontScale = 1.5f,
    ) {
        Box(Modifier.fillMaxSize().padding(20.dp)) {
            TrainingHomeContent(
                accent = tabAccentFor(AppDestination.Training),
                onStartBlankWorkout = {},
                onNewRoutine = {},
                onOpenLibrary = {},
            )
        }
    }

    @Test
    @Config(qualifiers = "w610dp-h900dp-mdpi")
    fun profile_foldable_light_ltr() = capture("profile-foldable-light-ltr.png", dark = false) {
        SettingsHub(
            state = ProfileSettingsUiState(),
            accent = tabAccentFor(AppDestination.Profile),
            onBack = {}, onEditAccount = {}, onOpenProfileDetails = {}, onGoogleSignIn = {},
            onGitHubSignIn = {}, onOpenAiCoach = {}, onOpenHealthConnect = {},
            onOpenDataTransfer = {},
            onIncludeBurnedCaloriesChange = {},
        )
    }

    @Test
    @Config(qualifiers = "w610dp-h900dp-mdpi")
    fun navigation_foldable_dark_rtl() = capture(
        "navigation-foldable-dark-rtl.png",
        dark = true,
        rtl = true,
    ) {
        Box(Modifier.fillMaxSize().padding(20.dp)) {
            MusFitBottomNav(
                destinations = AppDestination.entries,
                currentRoute = AppDestination.Food.route,
                onSelect = {},
                onCoachClick = {},
            )
        }
    }

    @Test
    @Config(qualifiers = "w900dp-h700dp-mdpi")
    fun scanner_tablet_light_rtl_largeFont() = capture(
        "scanner-tablet-light-rtl-font-150.png",
        dark = false,
        rtl = true,
        fontScale = 1.5f,
    ) {
        BarcodeScannerScreen(onBarcodeDetected = {}, onClose = {})
    }

    @Test
    @Config(qualifiers = "w900dp-h700dp-mdpi")
    fun training_tablet_dark_ltr() = capture("training-tablet-dark-ltr.png", dark = true) {
        Box(Modifier.fillMaxSize().padding(32.dp)) {
            TrainingHomeContent(
                hasActiveWorkout = true,
                accent = tabAccentFor(AppDestination.Training),
                onStartBlankWorkout = {},
                onNewRoutine = {},
                onOpenLibrary = {},
            )
        }
    }

    @Test
    @Config(qualifiers = "w900dp-h700dp-mdpi")
    fun today_tablet_dark_ltr() = capture("today-tablet-dark-ltr.png", dark = true) {
        Box(Modifier.fillMaxSize().padding(32.dp)) {
            TodayVitalsGrid(
                vitals = listOf(
                    MetricCardUiState(TodayMetric.Calories, "EATEN", MetricValue.WithGoal("1,443", "of 2,450 kcal · 59%", 0.59f)),
                    MetricCardUiState(TodayMetric.Steps, "STEPS", MetricValue.WithGoal("7,800", "of 10,000 · 78%", 0.78f)),
                    MetricCardUiState(TodayMetric.Protein, "PROTEIN", MetricValue.WithGoal("118 g", "of 180 g · 66%", 0.66f)),
                    MetricCardUiState(TodayMetric.Water, "WATER", MetricValue.WithGoal("1.8 L", "of 2.6 L · 67%", 0.67f)),
                ),
                onMetricClick = {},
            )
        }
    }

    private fun capture(
        fileName: String,
        dark: Boolean,
        rtl: Boolean = false,
        fontScale: Float = 1f,
        content: @Composable () -> Unit,
    ) {
        compose.setContent {
            ScreenshotFrame(dark = dark, rtl = rtl, fontScale = fontScale, content = content)
        }
        compose.waitForIdle()
        assertTouchTargets()
        compose.onRoot().captureRoboImage(fileName)
    }

    private fun assertTouchTargets() {
        val minimum = with(compose.density) { 48.dp.toPx() }
        compose.onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach { node ->
            // A vertically scrolling screen retains semantics for composed rows below
            // the viewport; Robolectric reports those nodes at 0x0 until scrolled to.
            if (node.boundsInRoot.width == 0f && node.boundsInRoot.height == 0f) return@forEach
            val label =
                node.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()
                    ?: node.config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }
                    ?: node.config.getOrNull(SemanticsProperties.Role)?.let { "<unlabelled:$it>" }
                    ?: "<unlabelled>"
            val knownDebt = label in knownTouchTargetDebt || label.startsWith("Search foods, brands, recipes")
            assertTrue(
                "Touch target '$label' was ${node.boundsInRoot.width}x${node.boundsInRoot.height}",
                knownDebt || (node.boundsInRoot.width >= minimum && node.boundsInRoot.height >= minimum),
            )
        }
    }

    private companion object {
        // Existing debt is named rather than normalizing a weaker global threshold.
        // W5 accessibility packages remove entries as the corresponding controls reach 48 dp.
        val knownTouchTargetDebt = setOf(
            "Back",
            "Browse library",
            "Change meal",
            "Edit",
            "Favorites",
            "More actions",
            "New folder",
            "New routine",
            "Recents",
            "Recipes",
            "Templates",
            "Create",
            "<unlabelled:Switch>",
        )
    }
}

@Composable
private fun ScreenshotFrame(
    dark: Boolean,
    rtl: Boolean,
    fontScale: Float,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, fontScale),
        LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) {
        MusFitTheme(darkTheme = dark) {
            Box(Modifier.fillMaxSize().background(MusFitTheme.colors.background)) {
                content()
            }
        }
    }
}
