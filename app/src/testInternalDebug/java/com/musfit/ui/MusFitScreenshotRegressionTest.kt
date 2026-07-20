package com.musfit.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.musfit.data.repository.ExerciseSummary
import com.musfit.data.repository.WeeklyTrainingVolume
import com.musfit.domain.model.ExerciseProgress
import com.musfit.domain.model.TrainingTrendPoint
import com.musfit.ui.components.MusFitScreenHeader
import com.musfit.ui.food.AddFoodScreen
import com.musfit.ui.food.BarcodeScannerScreen
import com.musfit.ui.food.FoodUiState
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.tabAccentFor
import com.musfit.ui.training.ExerciseEditorState
import com.musfit.ui.training.RoutineExercisePickerPage
import com.musfit.ui.training.TrainingHomeContent
import com.musfit.ui.training.TrainingPickerFilters
import com.musfit.ui.training.TrainingProgressContent
import com.musfit.ui.training.TrainingProgressContentActions
import com.musfit.ui.training.TrainingProgressContentData
import com.musfit.ui.training.TrainingProgressPeriod
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import com.musfit.feature.food.R as FoodR

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "mdpi")
class MusFitScreenshotRegressionTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    @Config(qualifiers = "w400dp-h800dp-mdpi")
    fun foodAdd_phone_light_ltr() = capture("food-add-phone-light-ltr.png", dark = false) {
        FoodAddFixture()
    }

    @Test
    @Config(qualifiers = "en-rXA-w400dp-h800dp-mdpi")
    fun foodAdd_phone_pseudo_largeFont() = capture(
        "food-add-phone-pseudo-font-150.png",
        dark = false,
        fontScale = 1.5f,
    ) {
        FoodAddFixture()
    }

    @Test
    @Config(qualifiers = "en-rXA-w400dp-h800dp-mdpi")
    fun screenHeader_phone_pseudo_largeFont() = capture(
        "screen-header-phone-pseudo-font-150.png",
        dark = false,
        fontScale = 1.5f,
    ) {
        Box(Modifier.fillMaxSize().padding(20.dp)) {
            MusFitScreenHeader(
                title = stringResource(FoodR.string.food_title),
                actions = {
                    Text(
                        text = "[Jûļ one] 20, 2026 one two",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                },
            )
        }
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
    @Config(qualifiers = "w400dp-h800dp-mdpi")
    fun navigation_phone_light_ltr() = capture("navigation-phone-light-ltr.png", dark = false) {
        RootNavigationFixture(RootNavigationLayout.Compact)
    }

    @Test
    @Config(qualifiers = "en-rXA-w400dp-h800dp-mdpi")
    fun navigation_phone_pseudo_largeFont() = capture(
        "navigation-phone-pseudo-font-150.png",
        dark = false,
        fontScale = 1.5f,
    ) {
        RootNavigationFixture(RootNavigationLayout.Compact)
    }

    @Test
    @Config(qualifiers = "w610dp-h900dp-mdpi")
    fun navigation_foldable_dark_rtl() = capture(
        "navigation-foldable-dark-rtl.png",
        dark = true,
        rtl = true,
    ) {
        RootNavigationFixture(RootNavigationLayout.Rail)
    }

    @Test
    @Config(qualifiers = "ar-rXB-w610dp-h900dp-mdpi")
    fun navigation_foldable_pseudoRtl() = capture(
        "navigation-foldable-pseudo-rtl.png",
        dark = false,
        rtl = true,
    ) {
        RootNavigationFixture(RootNavigationLayout.Rail)
    }

    @Test
    @Config(qualifiers = "w900dp-h700dp-mdpi")
    fun navigation_tablet_light_ltr() = capture("navigation-tablet-light-ltr.png", dark = false) {
        RootNavigationFixture(RootNavigationLayout.Wide)
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
    fun trainingPicker_tablet_light_ltr() = capture("training-picker-tablet-light-ltr.png", dark = false) {
        RoutineExercisePickerPage(
            exercises = screenshotExercises(),
            currentRoutineExerciseIds = emptySet(),
            selectedExerciseIds = setOf("exercise-1", "exercise-4"),
            searchQuery = "",
            filters = TrainingPickerFilters(),
            filterSheetOpen = false,
            loggedExerciseIds = setOf("exercise-0", "exercise-3"),
            customExerciseEditor = ExerciseEditorState(),
            accent = tabAccentFor(AppDestination.Training),
            onSearchChange = {}, onOpenFilters = {}, onCloseFilters = {},
            onToggleEquipment = {}, onToggleMuscle = {}, onOnlyDoneChange = {},
            onResetFilters = {}, onClearFilters = {}, onToggleExercise = {},
            onOpenCustomExercise = {}, onCloseCustomExercise = {},
            onCustomExerciseNameChange = {}, onCustomExerciseCategoryChange = {},
            onCustomExerciseEquipmentChange = {}, onCustomExerciseTargetMusclesChange = {},
            onSaveCustomExercise = {}, onCancel = {}, onConfirm = {},
        )
    }

    @Test
    @Config(qualifiers = "w900dp-h700dp-mdpi")
    fun trainingProgress_tablet_light_rtl() = capture(
        "training-progress-tablet-light-rtl.png",
        dark = false,
        rtl = true,
    ) {
        val trend = screenshotTrend()
        Box(Modifier.fillMaxSize().padding(24.dp)) {
            TrainingProgressContent(
                data = TrainingProgressContentData(
                    progress = screenshotProgress(trend),
                    period = TrainingProgressPeriod.Year,
                    weeklyVolume = screenshotWeeks(),
                    recentPrs = emptyList(),
                    today = screenshotToday,
                ),
                accent = tabAccentFor(AppDestination.Training),
                actions = TrainingProgressContentActions(onOpenAllExercises = {}),
            )
        }
    }

    @Test
    @Config(qualifiers = "ar-rXB-w400dp-h800dp-mdpi")
    fun trainingProgress_phone_pseudoRtl_largeFont() = capture(
        "training-progress-phone-pseudo-rtl-font-150.png",
        dark = false,
        rtl = true,
        fontScale = 1.5f,
    ) {
        TrainingProgressFixture()
    }

    @Test
    @Config(qualifiers = "de-rDE-w900dp-h700dp-mdpi")
    fun trainingProgress_tablet_german() = capture(
        "training-progress-tablet-german.png",
        dark = false,
    ) {
        TrainingProgressFixture()
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
        val resources = ApplicationProvider.getApplicationContext<Context>().resources
        val localizedKnownDebtResourceIds: List<Int> = listOf(
            FoodR.string.food_change_meal,
            FoodR.string.food_edit,
            FoodR.string.food_favorites,
            FoodR.string.food_more_actions,
            FoodR.string.food_recents,
            FoodR.string.food_recipes,
            FoodR.string.food_templates,
            FoodR.string.food_create,
            FoodR.string.food_add_mode_saved,
            FoodR.string.food_add_mode_manual,
            FoodR.string.food_add_mode_barcode,
            FoodR.string.food_add_mode_quick,
            FoodR.string.food_add_mode_ai,
        )
        val localizedKnownDebt = localizedKnownDebtResourceIds.map { resourceId ->
            resources.getString(resourceId)
        }.toSet()
        val localizedSearchPrefix = resources.getString(FoodR.string.food_search_foods_hint).trimEnd('…')
        compose.onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach { node ->
            // A vertically scrolling screen retains semantics for composed rows below
            // the viewport; Robolectric reports those nodes at 0x0 until scrolled to.
            if (node.boundsInRoot.width == 0f && node.boundsInRoot.height == 0f) return@forEach
            val label =
                node.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()
                    ?: node.config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }
                    ?: node.config.getOrNull(SemanticsProperties.Role)?.let { "<unlabelled:$it>" }
                    ?: "<unlabelled>"
            val knownDebt = label in knownTouchTargetDebt ||
                label in localizedKnownDebt ||
                label.startsWith(localizedSearchPrefix)
            assertTrue(
                "Touch target '$label' was ${node.boundsInRoot.width}x${node.boundsInRoot.height}; semantics=${node.config}",
                knownDebt || (node.boundsInRoot.width >= minimum && node.boundsInRoot.height >= minimum),
            )
        }
    }

    private companion object {
        // Existing debt is named rather than normalizing a weaker global threshold.
        // W5 accessibility packages remove entries as the corresponding controls reach 48 dp.
        val knownTouchTargetDebt = setOf(
            "Back",
            "Change meal",
            "Edit",
            "Favorites",
            "More actions",
            "Recents",
            "Recipes",
            "Templates",
            "Create",
            "<unlabelled:Switch>",
        )
    }
}

@Composable
private fun FoodAddFixture() {
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

@Composable
private fun TrainingProgressFixture() {
    val trend = screenshotTrend()
    Box(Modifier.fillMaxSize().padding(24.dp)) {
        TrainingProgressContent(
            data = TrainingProgressContentData(
                progress = screenshotProgress(trend),
                period = TrainingProgressPeriod.Year,
                weeklyVolume = screenshotWeeks(),
                recentPrs = emptyList(),
                today = screenshotToday,
            ),
            accent = tabAccentFor(AppDestination.Training),
            actions = TrainingProgressContentActions(onOpenAllExercises = {}),
        )
    }
}

private fun screenshotExercises(): List<ExerciseSummary> = List(12) { index ->
    ExerciseSummary(
        id = "exercise-$index",
        name = listOf("Back Squat", "Bench Press", "Barbell Row", "Romanian Deadlift")[index % 4] + " ${index + 1}",
        category = "strength",
        equipment = if (index % 2 == 0) "barbell" else "dumbbell",
        targetMuscles = if (index % 2 == 0) "legs,glutes" else "chest,triceps",
        isCustom = false,
    )
}

private fun screenshotTrend(): List<TrainingTrendPoint> = List(8) { index ->
    TrainingTrendPoint(
        dateEpochDay = screenshotToday.minusWeeks((7 - index).toLong()).toEpochDay(),
        volumeKg = 1_100.0 + index * 175.0,
        bestEstimatedOneRepMaxKg = 98.0 + index * 2.5,
    )
}

private fun screenshotWeeks(): List<WeeklyTrainingVolume> = List(6) { index ->
    WeeklyTrainingVolume(
        weekStartEpochDay = screenshotToday.minusWeeks((5 - index).toLong()).toEpochDay(),
        workoutCount = 2 + index % 3,
        completedSetCount = 12 + index * 3,
        totalVolumeKg = 1_600.0 + index * 525.0,
    )
}

private fun screenshotProgress(trend: List<TrainingTrendPoint>) = ExerciseProgress(
    exerciseId = "back-squat",
    exerciseName = "Back Squat",
    equipment = "barbell",
    targetMuscles = "legs,glutes",
    heaviestWeightKg = 120.0,
    maxReps = 8,
    bestEstimatedOneRepMaxKg = trend.last().bestEstimatedOneRepMaxKg,
    bestWorkoutVolumeKg = 2_325.0,
    trend = trend,
)

private val screenshotToday: LocalDate = LocalDate.of(2026, 7, 13)

@Composable
private fun RootNavigationFixture(layout: RootNavigationLayout) {
    RootNavigationScaffold(
        layout = layout,
        state = RootNavigationState(
            destinations = AppDestination.entries,
            currentRoute = AppDestination.Food.route,
            chromeVisible = true,
        ),
        callbacks = RootNavigationCallbacks(onSelect = {}, onCoachClick = {}),
    ) { modifier ->
        Box(
            modifier
                .fillMaxSize()
                .background(MusFitTheme.colors.background)
                .padding(32.dp),
        ) {
            Text("Adaptive content", color = MusFitTheme.colors.onSurface)
        }
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
