package com.musfit.ui.food

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BreakfastDining
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation3.LocalListDetailSceneScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.musfit.data.repository.FoodGoalMode
import com.musfit.feature.food.R
import com.musfit.ui.components.ExpressiveBadge
import com.musfit.ui.components.ExpressiveBadgeShape
import com.musfit.ui.components.HeroNumberMediumStyle
import com.musfit.ui.components.InnerScreenHeader
import com.musfit.ui.components.MusFitScreenHeader
import com.musfit.ui.components.MusFitSegmented
import com.musfit.ui.components.PillButton
import com.musfit.ui.components.SheetDragHandle
import com.musfit.ui.components.TonalHeaderIconButton
import com.musfit.ui.components.TonalIconSquare
import com.musfit.ui.components.WavyProgressBar
import com.musfit.ui.components.expressiveBadgeShapeFor
import com.musfit.ui.components.groupedShape
import com.musfit.ui.text.LocalizedFormatter
import com.musfit.ui.text.UiText
import com.musfit.ui.text.asString
import com.musfit.ui.theme.MusFitMotion
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import com.musfit.ui.theme.TabAccentRole
import com.musfit.ui.theme.tabAccentFor
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The two diary surfaces that share one space below the pinned header, swapped
 * by the segmented switcher: the meal diary and today's summary. (Weekly trends
 * now live on the Profile tab's Nutrition trends sub-screen.)
 */
private enum class FoodDiaryTab { Diary, Summary }

@Immutable
private data class FoodDiaryActions(
    val onPreviousDay: () -> Unit,
    val onNextDay: () -> Unit,
    val onToday: () -> Unit,
    val onOpenGoal: () -> Unit,
    val onOpenMeals: () -> Unit,
    val onOpenTemplates: () -> Unit,
    val onOpenShopping: () -> Unit,
    val onOpenRecipes: () -> Unit,
    val onOpenFasting: () -> Unit,
    val onOpenHealthConnect: () -> Unit,
    val onOpenDatabase: () -> Unit,
    val onCopyDayToTomorrow: () -> Unit,
    val onOpenWater: () -> Unit,
    val onQuickAddWater: () -> Unit,
    val onQuickRemoveWater: () -> Unit,
    val onUndoDelete: () -> Unit,
    val onOpenMeal: (String) -> Unit,
    val onAddFood: (String) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
@Suppress("LongMethod", "LongParameterList")
fun FoodScreen(
    renderedKey: FoodNavKey? = null,
    scannedBarcode: String? = null,
    onScanClick: () -> Unit = {},
    onScannedBarcodeConsumed: () -> Unit = {},
    scannedLabelText: String? = null,
    onLabelScanClick: () -> Unit = {},
    onScannedLabelConsumed: () -> Unit = {},
    navigation: FoodNavigationActions = FoodNavigationActions(),
    viewModel: FoodViewModel = hiltViewModel(),
) {
    val routeState by viewModel.routeState.collectAsStateWithLifecycle()
    val diaryState by viewModel.diaryState.collectAsStateWithLifecycle()
    val adaptiveRenderedKey = if (LocalListDetailSceneScope.current != null) renderedKey else null
    val accent = tabAccentFor(TabAccentRole.Food)
    val diaryActions = remember(viewModel, navigation) {
        FoodDiaryActions(
            onPreviousDay = viewModel::goToPreviousDay,
            onNextDay = viewModel::goToNextDay,
            onToday = viewModel::goToToday,
            onOpenGoal = {
                viewModel.openGoalEditor()
                navigation.open(FoodGoalEditorNavKey)
            },
            onOpenMeals = {
                viewModel.openMealSettings()
                navigation.open(FoodMealSettingsNavKey)
            },
            onOpenTemplates = {
                viewModel.openMealTemplates()
                navigation.open(FoodMealTemplatesNavKey)
            },
            onOpenShopping = {
                viewModel.openShoppingList()
                navigation.open(FoodShoppingListNavKey)
            },
            onOpenRecipes = {
                viewModel.openRecipeBrowser()
                navigation.open(FoodRecipeBrowserNavKey)
            },
            onOpenFasting = {
                viewModel.openFastingTimer()
                navigation.open(FoodFastingTimerNavKey)
            },
            onOpenHealthConnect = {
                viewModel.openHealthConnectSheet()
                navigation.open(FoodHealthConnectNavKey)
            },
            onOpenDatabase = {
                viewModel.openFoodDatabase()
                navigation.open(FoodDatabaseNavKey)
            },
            onCopyDayToTomorrow = viewModel::copySelectedDayToTomorrow,
            onOpenWater = {
                viewModel.openWaterSheet()
                navigation.open(FoodWaterNavKey)
            },
            onQuickAddWater = { viewModel.logQuickWater(WATER_QUICK_ADD_MILLILITERS) },
            onQuickRemoveWater = { viewModel.removeQuickWater(WATER_QUICK_ADD_MILLILITERS) },
            onUndoDelete = viewModel::undoDeleteDiaryEntry,
            onOpenMeal = { mealType ->
                viewModel.openMealDetail(mealType)
                navigation.open(FoodMealDetailNavKey(mealType))
            },
            onAddFood = { mealType ->
                viewModel.openAddFood(mealType)
                navigation.open(FoodAddNavKey(mealType))
            },
        )
    }
    val foodHealthConnectPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) {
        viewModel.refreshFoodHealthConnectSync()
    }

    LaunchedEffect(scannedBarcode) {
        if (!scannedBarcode.isNullOrBlank()) {
            viewModel.onScannedBarcode(scannedBarcode)
            onScannedBarcodeConsumed()
        }
    }

    LaunchedEffect(scannedLabelText) {
        if (!scannedLabelText.isNullOrBlank()) {
            viewModel.onScannedLabel(scannedLabelText)
            onScannedLabelConsumed()
        }
    }

    if (routeState.hasActiveSurface) {
        FoodActiveSurface(
            viewModel = viewModel,
            routeState = routeState,
            onScanClick = onScanClick,
            onLabelScanClick = onLabelScanClick,
            onRequestFoodHealthConnectPermissions = foodHealthConnectPermissionLauncher::launch,
            navigation = navigation,
            renderedKey = adaptiveRenderedKey,
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MusFitTheme.colors.background),
        ) {
            FoodDiaryHome(state = diaryState, accent = accent, actions = diaryActions)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongParameterList")
private fun FoodActiveSurface(
    viewModel: FoodViewModel,
    routeState: FoodRouteUiState,
    onScanClick: () -> Unit,
    onLabelScanClick: () -> Unit,
    onRequestFoodHealthConnectPermissions: (Set<String>) -> Unit,
    navigation: FoodNavigationActions,
    renderedKey: FoodNavKey?,
) {
    when (routeState.surfaceGroup) {
        FoodSurfaceGroup.AddDatabase -> {
            val state by viewModel.addDatabaseState.collectAsStateWithLifecycle()
            FoodProjectedSurface(
                state = state.content,
                viewModel = viewModel,
                onScanClick = onScanClick,
                onLabelScanClick = onLabelScanClick,
                navigation = navigation,
                renderedKey = renderedKey,
            )
        }

        FoodSurfaceGroup.EditorPlanning -> {
            val state by viewModel.editorPlanningState.collectAsStateWithLifecycle()
            FoodProjectedSurface(
                state = state.content,
                viewModel = viewModel,
                onScanClick = onScanClick,
                onLabelScanClick = onLabelScanClick,
                navigation = navigation,
                renderedKey = renderedKey,
            )
        }

        FoodSurfaceGroup.Tracker -> FoodTrackerSurface(
            viewModel = viewModel,
            sheetMode = routeState.sheetMode,
            onRequestFoodHealthConnectPermissions = onRequestFoodHealthConnectPermissions,
            navigation = navigation,
        )

        null -> Unit
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FoodTrackerSurface(
    viewModel: FoodViewModel,
    sheetMode: FoodSheetMode?,
    onRequestFoodHealthConnectPermissions: (Set<String>) -> Unit,
    navigation: FoodNavigationActions,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MusFitTheme.colors.background),
    ) {
        ModalBottomSheet(
            onDismissRequest = navigation.back,
            containerColor = MusFitTheme.colors.background,
            dragHandle = {
                SheetDragHandle(modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
            },
        ) {
            when (sheetMode) {
                FoodSheetMode.Water -> FoodWaterTrackerSheet(viewModel)

                FoodSheetMode.HealthConnect -> FoodHealthConnectTrackerSheet(
                    viewModel = viewModel,
                    onRequestPermissions = onRequestFoodHealthConnectPermissions,
                )

                else -> Unit
            }
        }
    }
}

internal fun FoodNavKey.isAdaptiveFoodPane(): Boolean = when (this) {
    FoodDatabaseNavKey,
    is FoodDetailNavKey,
    FoodRecipeBrowserNavKey,
    is FoodRecipeEditorNavKey,
    -> true

    else -> false
}

@Composable
@Suppress("LongMethod")
private fun AdaptiveFoodPane(
    key: FoodNavKey,
    state: FoodUiState,
    viewModel: FoodViewModel,
    onLabelScanClick: () -> Unit,
    navigation: FoodNavigationActions,
) {
    when (key) {
        FoodDatabaseNavKey -> FoodDatabasePanel(
            state = state,
            onSearchChanged = viewModel::onFoodDatabaseQueryChanged,
            onSearchOnlineClick = viewModel::searchOnlineFoods,
            onNewFoodClick = {
                viewModel.openNewSavedFoodEditor()
                navigation.open(FoodSavedFoodEditorNavKey())
            },
            onBarcodeCompareClick = {
                viewModel.openBarcodeComparison()
                navigation.open(FoodBarcodeComparisonNavKey)
            },
            onOpenFoodDetailClick = { foodId ->
                viewModel.openSavedFoodDetail(foodId)
                navigation.open(FoodDetailNavKey(foodId))
            },
            onEditFoodClick = { foodId ->
                viewModel.openSavedFoodEditor(foodId)
                navigation.open(FoodSavedFoodEditorNavKey(foodId))
            },
            onSaveOnlineFoodClick = viewModel::saveOnlineFoodResult,
            onImportStarterFoodsClick = viewModel::seedStarterFoods,
            onNutritionLabelScanClick = onLabelScanClick,
            onMergeDuplicateFoodsClick = viewModel::mergeDuplicateFoods,
            onFavoriteClick = viewModel::toggleFavoriteFood,
            onReportFoodClick = viewModel::reportSavedFoodForReview,
        )

        is FoodDetailNavKey -> FoodDetailPanel(
            state = state,
            onEditClick = {
                viewModel.openSavedFoodEditor(key.foodId)
                navigation.open(FoodSavedFoodEditorNavKey(key.foodId))
            },
            onLogClick = { viewModel.logSavedFood(key.foodId) },
            onFavoriteClick = {
                state.selectedSavedFoodDetail?.let { food ->
                    viewModel.toggleFavoriteFood(food.id, !food.isFavorite)
                }
            },
            onReportClick = { viewModel.reportSavedFoodForReview(key.foodId) },
            onCorrectClick = { viewModel.startSavedFoodCorrection(key.foodId) },
            onQuantityChanged = viewModel::onSavedFoodQuantityChanged,
            onServingSelected = viewModel::onSavedFoodServingSelected,
        )

        FoodRecipeBrowserNavKey -> AdaptiveRecipePane(
            state = state.copy(sheetMode = FoodSheetMode.RecipeBrowser),
            viewModel = viewModel,
            navigation = navigation,
        )

        is FoodRecipeEditorNavKey -> AdaptiveRecipePane(
            state = state.copy(sheetMode = FoodSheetMode.RecipeEditor),
            viewModel = viewModel,
            navigation = navigation,
        )

        else -> Unit
    }
}

@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Suppress("LongMethod", "LongParameterList") // Adaptive host keeps list/editor actions on the same ViewModel instance.
private fun AdaptiveRecipePane(
    state: FoodUiState,
    viewModel: FoodViewModel,
    navigation: FoodNavigationActions,
) {
    var pendingRecipeReplacement by remember { mutableStateOf<(() -> Unit)?>(null) }
    val sceneValue = LocalListDetailSceneScope.current
        ?.scaffoldTransitionScope
        ?.scaffoldStateTransition
        ?.targetState
    val showsListAndDetail = sceneValue != null &&
        sceneValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded &&
        sceneValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded
    val replaceRecipeEditor: (() -> Unit) -> Unit = { replacement ->
        if (state.hasUnsavedRecipeEditorChanges()) {
            pendingRecipeReplacement = replacement
        } else {
            replacement()
        }
    }

    pendingRecipeReplacement?.let { replacement ->
        DiscardRecipeChangesDialog(
            onDismiss = { pendingRecipeReplacement = null },
            onConfirm = {
                pendingRecipeReplacement = null
                replacement()
            },
        )
    }

    RecipeBrowserScreen(
        state = state,
        showEditorBack = !showsListAndDetail,
        onCloseClick = navigation.back,
        onForwardClick = {
            replaceRecipeEditor {
                viewModel.openRecipeEditor(null)
                navigation.open(FoodRecipeEditorNavKey())
            }
        },
        onHomeClick = navigation.back,
        onPreviousDayClick = viewModel::goToPreviousRecipeBrowserDay,
        onNextDayClick = viewModel::goToNextRecipeBrowserDay,
        onTodayClick = viewModel::goToTodayRecipeBrowserDay,
        onMealChanged = viewModel::onRecipeBrowserMealChanged,
        onServingsChanged = viewModel::onRecipeServingsToLogChanged,
        onNameChanged = viewModel::onRecipeNameChanged,
        onCategoryChanged = viewModel::onRecipeCategoryChanged,
        onServingNameChanged = viewModel::onRecipeServingNameChanged,
        onServingsCountChanged = viewModel::onRecipeServingsCountChanged,
        onCookedYieldChanged = viewModel::onRecipeCookedYieldGramsChanged,
        onIngredientFoodChanged = viewModel::onRecipeIngredientFoodChanged,
        onIngredientServingChoiceSelected = viewModel::onRecipeIngredientServingChoiceSelected,
        onIngredientQuantityChanged = viewModel::onRecipeIngredientQuantityChanged,
        onAddIngredientClick = viewModel::addRecipeIngredient,
        onEditRecipeClick = { recipeId ->
            replaceRecipeEditor {
                viewModel.openRecipeEditor(recipeId)
                navigation.open(FoodRecipeEditorNavKey(recipeId))
            }
        },
        onDuplicateRecipeClick = viewModel::duplicateRecipe,
        onFavoriteClick = viewModel::toggleFavoriteRecipe,
        onSearchQueryChanged = viewModel::onRecipeDiscoveryQueryChanged,
        onDiscoveryFilterChanged = viewModel::selectRecipeDiscoveryFilter,
        onDiscoveryItemClick = { itemId ->
            val opensEditor = state.recipeDiscovery.items.firstOrNull { it.id == itemId }?.sourceRecipeId == null
            val openItem = {
                if (viewModel.useRecipeDiscoveryItem(itemId)) navigation.open(FoodRecipeEditorNavKey())
            }
            if (opensEditor) replaceRecipeEditor(openItem) else openItem()
        },
        onLogRecipeClick = viewModel::logRecipeFromBrowser,
        onPlanRecipeClick = viewModel::planRecipe,
        onReviewIdeaClick = { itemId ->
            replaceRecipeEditor {
                if (viewModel.useRecipeDiscoveryItem(itemId)) navigation.open(FoodRecipeEditorNavKey())
            }
        },
        onSaveClick = viewModel::saveRecipe,
        onDeleteClick = { state.recipeEditor?.editingRecipeId?.let(viewModel::deleteRecipe) },
    )
}

@Composable
internal fun DiscardRecipeChangesDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.food_discard_recipe_changes)) },
        text = { Text(stringResource(R.string.food_unsaved_recipe_edits)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.food_discard_changes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.food_keep_editing))
            }
        },
    )
}

internal fun FoodUiState.hasUnsavedRecipeEditorChanges(): Boolean {
    val editor = recipeEditor
    val original = editor?.editingRecipeId?.let { recipeId -> recipes.firstOrNull { it.id == recipeId } }
    return when {
        editor == null -> false

        original == null -> editor != RecipeEditorState()

        else -> editor != RecipeEditorState(
            editingRecipeId = original.id,
            name = original.name,
            category = original.category.orEmpty(),
            servingName = original.servingName,
            servingGrams = original.servingGrams.formatInputNumber(),
            servingsCount = original.servings.formatInputNumber(),
            cookedYieldGrams = original.cookedYieldGrams.formatInputNumber(),
            ingredients = original.ingredients,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod", "CyclomaticComplexMethod", "LongParameterList")
private fun FoodProjectedSurface(
    state: FoodUiState,
    viewModel: FoodViewModel,
    onScanClick: () -> Unit,
    onLabelScanClick: () -> Unit,
    navigation: FoodNavigationActions,
    renderedKey: FoodNavKey?,
) {
    if (renderedKey != null && renderedKey.isAdaptiveFoodPane()) {
        AdaptiveFoodPane(
            key = renderedKey,
            state = state,
            viewModel = viewModel,
            onLabelScanClick = onLabelScanClick,
            navigation = navigation,
        )
        return
    }
    val selectedMealDetail = state.selectedMealDetailForDisplay()
    val selectedMealTemplateName = stringResource(R.string.food_template_name, state.selectedMealTitle)
    val selectedMealDetailTemplateName = selectedMealDetail?.let { meal ->
        stringResource(R.string.food_template_name, meal.title)
    }
    val isRecipeFullScreen =
        state.isAddPanelVisible &&
            (state.sheetMode == FoodSheetMode.RecipeBrowser || state.sheetMode == FoodSheetMode.RecipeEditor)
    // Turn 9: the saved-food and goal editors graduate from sheets to full screens
    // (same hosting pattern as the recipe browser).
    val isSavedFoodEditorFullScreen =
        state.isAddPanelVisible && state.sheetMode == FoodSheetMode.SavedFoodEditor
    val isGoalEditorFullScreen =
        state.isAddPanelVisible && state.sheetMode == FoodSheetMode.GoalEditor

    // A system/gesture back on a full-screen Food surface (meal detail or add-food)
    // closes it and returns to the diary, instead of falling through to the NavHost
    // and popping the whole Food tab out to Today. Only one is ever active at a time;
    // the modal sheets handle their own back.
    BackHandler(enabled = selectedMealDetail != null) { navigation.back() }
    BackHandler(
        enabled = state.isAddPanelVisible && state.sheetMode == FoodSheetMode.AddFood,
    ) { navigation.back() }
    BackHandler(enabled = isRecipeFullScreen) { navigation.back() }
    BackHandler(enabled = isSavedFoodEditorFullScreen || isGoalEditorFullScreen) {
        navigation.back()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MusFitTheme.colors.background),
    ) {
        if (isSavedFoodEditorFullScreen) {
            SavedFoodEditorScreen(
                onBack = navigation.back,
                state = state,
                onNameChanged = viewModel::onSavedFoodNameChanged,
                onBrandChanged = viewModel::onSavedFoodBrandChanged,
                onServingChanged = viewModel::onSavedFoodServingChanged,
                onCaloriesChanged = viewModel::onSavedFoodCaloriesChanged,
                onProteinChanged = viewModel::onSavedFoodProteinChanged,
                onCarbsChanged = viewModel::onSavedFoodCarbsChanged,
                onFatChanged = viewModel::onSavedFoodFatChanged,
                onFiberChanged = viewModel::onSavedFoodFiberChanged,
                onSugarChanged = viewModel::onSavedFoodSugarChanged,
                onSaturatedFatChanged = viewModel::onSavedFoodSaturatedFatChanged,
                onSodiumChanged = viewModel::onSavedFoodSodiumChanged,
                onPotassiumChanged = viewModel::onSavedFoodPotassiumChanged,
                onCalciumChanged = viewModel::onSavedFoodCalciumChanged,
                onIronChanged = viewModel::onSavedFoodIronChanged,
                onVitaminDChanged = viewModel::onSavedFoodVitaminDChanged,
                onVitaminCChanged = viewModel::onSavedFoodVitaminCChanged,
                onMagnesiumChanged = viewModel::onSavedFoodMagnesiumChanged,
                onServingNameChanged = viewModel::onSavedFoodServingNameChanged,
                onBarcodeChanged = viewModel::onSavedFoodBarcodeChanged,
                onCategoryChanged = viewModel::onSavedFoodCategoryChanged,
                onFavoriteChanged = viewModel::onSavedFoodFavoriteChanged,
                onSaveClick = viewModel::saveSavedFood,
                onDuplicateClick = viewModel::duplicateSavedFood,
                onDeleteClick = viewModel::deleteSavedFood,
            )
        } else if (isGoalEditorFullScreen) {
            GoalEditorScreen(
                onBack = navigation.back,
                state = state,
                onCaloriesChanged = viewModel::onGoalCaloriesChanged,
                onProteinChanged = viewModel::onGoalProteinChanged,
                onCarbsChanged = viewModel::onGoalCarbsChanged,
                onFatChanged = viewModel::onGoalFatChanged,
                onFiberChanged = viewModel::onGoalFiberChanged,
                onSugarChanged = viewModel::onGoalSugarChanged,
                onSaturatedFatChanged = viewModel::onGoalSaturatedFatChanged,
                onSodiumChanged = viewModel::onGoalSodiumChanged,
                onModeChanged = viewModel::onGoalModeChanged,
                onTrainingChanged = viewModel::onGoalIncludeTrainingChanged,
                onNetCarbsChanged = viewModel::onGoalUseNetCarbsChanged,
                onProgramApply = viewModel::applyFoodProgram,
                onSaveClick = viewModel::saveFoodGoal,
            )
        } else if (isRecipeFullScreen) {
            RecipeBrowserScreen(
                state = state,
                onCloseClick = navigation.back,
                onForwardClick = {
                    viewModel.openRecipeEditor(null)
                    navigation.open(FoodRecipeEditorNavKey())
                },
                onHomeClick = navigation.back,
                onPreviousDayClick = viewModel::goToPreviousRecipeBrowserDay,
                onNextDayClick = viewModel::goToNextRecipeBrowserDay,
                onTodayClick = viewModel::goToTodayRecipeBrowserDay,
                onMealChanged = viewModel::onRecipeBrowserMealChanged,
                onServingsChanged = viewModel::onRecipeServingsToLogChanged,
                onNameChanged = viewModel::onRecipeNameChanged,
                onCategoryChanged = viewModel::onRecipeCategoryChanged,
                onServingNameChanged = viewModel::onRecipeServingNameChanged,
                onServingsCountChanged = viewModel::onRecipeServingsCountChanged,
                onCookedYieldChanged = viewModel::onRecipeCookedYieldGramsChanged,
                onIngredientFoodChanged = viewModel::onRecipeIngredientFoodChanged,
                onIngredientServingChoiceSelected = viewModel::onRecipeIngredientServingChoiceSelected,
                onIngredientQuantityChanged = viewModel::onRecipeIngredientQuantityChanged,
                onAddIngredientClick = viewModel::addRecipeIngredient,
                onEditRecipeClick = { recipeId ->
                    viewModel.openRecipeEditor(recipeId)
                    navigation.open(FoodRecipeEditorNavKey(recipeId))
                },
                onDuplicateRecipeClick = viewModel::duplicateRecipe,
                onFavoriteClick = viewModel::toggleFavoriteRecipe,
                onSearchQueryChanged = viewModel::onRecipeDiscoveryQueryChanged,
                onDiscoveryFilterChanged = viewModel::selectRecipeDiscoveryFilter,
                onDiscoveryItemClick = { itemId ->
                    if (viewModel.useRecipeDiscoveryItem(itemId)) navigation.open(FoodRecipeEditorNavKey())
                },
                onLogRecipeClick = viewModel::logRecipeFromBrowser,
                onPlanRecipeClick = viewModel::planRecipe,
                onReviewIdeaClick = { itemId ->
                    if (viewModel.useRecipeDiscoveryItem(itemId)) navigation.open(FoodRecipeEditorNavKey())
                },
                onSaveClick = viewModel::saveRecipe,
                onDeleteClick = { state.recipeEditor?.editingRecipeId?.let(viewModel::deleteRecipe) },
            )
        } else if (state.isAddPanelVisible && state.sheetMode == FoodSheetMode.AddFood) {
            AddFoodScreen(
                state = state,
                onBack = navigation.back,
                onQueryChange = viewModel::onFoodDatabaseQueryChanged,
                onScanClick = onScanClick,
                onTabSelected = viewModel::selectAddTab,
                onFoodClick = viewModel::logSavedFood,
                onModeSelected = viewModel::selectAddMode,
                onMealRetarget = viewModel::onMealTypeChanged,
                onOpenTemplates = {
                    viewModel.openMealTemplates()
                    navigation.open(FoodMealTemplatesNavKey)
                },
                onOpenRecipes = {
                    viewModel.openRecipeBrowser()
                    navigation.open(FoodRecipeBrowserNavKey)
                },
                onKeepAddingChanged = viewModel::onKeepAddingFoodsChanged,
                onLogAllYesterday = viewModel::logSameAsYesterday,
                onQuickTrack = { viewModel.selectAddMode(FoodAddMode.Quick) },
                onAdjustGoals = {
                    viewModel.openGoalEditor()
                    navigation.open(FoodGoalEditorNavKey)
                },
                onCopyYesterday = viewModel::copySelectedMealFromYesterday,
                onSaveTemplate = {
                    viewModel.saveSelectedMealAsTemplate(selectedMealTemplateName)
                },
                onScanLabel = onLabelScanClick,
                onProductNameChanged = viewModel::onProductNameChanged,
                onBrandChanged = viewModel::onBrandChanged,
                onQuantityChanged = viewModel::onQuantityChanged,
                onAmountServingChoiceSelected = viewModel::onAmountServingChoiceSelected,
                onCaloriesChanged = viewModel::onCaloriesChanged,
                onProteinChanged = viewModel::onProteinChanged,
                onCarbsChanged = viewModel::onCarbsChanged,
                onFatChanged = viewModel::onFatChanged,
                onSaveProduct = viewModel::saveScannedProductToDatabase,
                onLogFood = viewModel::logFood,
                onCreateRecipe = {
                    viewModel.openRecipeEditor(null)
                    navigation.open(FoodRecipeEditorNavKey())
                },
            )
        } else if (selectedMealDetail != null) {
            MealDetailScreen(
                meal = selectedMealDetail,
                sortMode = state.mealDetailSortMode,
                presentation = MealDetailPresentation(
                    selectedDate = state.selectedDate,
                    dayCalorieBudgetKcal = state.effectiveCalorieBudgetKcal,
                    message = state.message,
                    canUndoDelete = state.lastDeletedDiaryEntry != null,
                ),
                actions = MealDetailActions(
                    onBackClick = navigation.back,
                    onAddFoodClick = {
                        viewModel.openAddFoodFromMealDetail()
                        navigation.open(FoodAddNavKey(state.selectedMealDetailId.orEmpty()))
                    },
                    onCopyYesterdayClick = viewModel::copySelectedMealFromYesterday,
                    onSaveTemplateClick = {
                        viewModel.saveSelectedMealAsTemplate(selectedMealDetailTemplateName.orEmpty())
                    },
                    onMealSettingsClick = {
                        viewModel.openMealSettings()
                        navigation.open(FoodMealSettingsNavKey)
                    },
                    onSortModeChanged = viewModel::onMealDetailSortChanged,
                    onEntryClick = { entryId ->
                        viewModel.openDiaryEntryEditor(entryId)
                        navigation.open(FoodDiaryEntryEditorNavKey(entryId))
                    },
                    onUndoDeleteClick = viewModel::undoDeleteDiaryEntry,
                ),
            )
        }
    }

    if (
        state.isAddPanelVisible &&
        !isRecipeFullScreen &&
        !isSavedFoodEditorFullScreen &&
        !isGoalEditorFullScreen &&
        (state.sheetMode != FoodSheetMode.AddFood || state.addMode != FoodAddMode.Saved)
    ) {
        ModalBottomSheet(
            onDismissRequest = {
                // The AddFood mode sheet floats over the full-screen Saved add
                // surface; dismissing it should fall back there, not tear down
                // the whole add flow.
                if (state.sheetMode == FoodSheetMode.AddFood) {
                    viewModel.selectAddMode(FoodAddMode.Saved)
                } else {
                    navigation.back()
                }
            },
            containerColor = MusFitTheme.colors.background,
            dragHandle = {
                SheetDragHandle(modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
            },
        ) {
            when (state.sheetMode ?: FoodSheetMode.AddFood) {
                FoodSheetMode.AddFood ->
                    AddFoodPanel(
                        state = state,
                        onClose = navigation.back,
                        onMealTargetSelected = viewModel::onMealTypeChanged,
                        onModeSelected = viewModel::selectAddMode,
                        onSavedQuantityChanged = viewModel::onSavedFoodQuantityChanged,
                        onSavedFoodClick = viewModel::logSavedFood,
                        onSavedFoodServingSelected = viewModel::onSavedFoodServingSelected,
                        onKeepAddingChanged = viewModel::onKeepAddingFoodsChanged,
                        onTemplateClick = viewModel::logMealTemplate,
                        onTemplateFavoriteClick = viewModel::toggleFavoriteMealTemplate,
                        onRecipeClick = viewModel::logRecipe,
                        onRecipeFavoriteClick = viewModel::toggleFavoriteRecipe,
                        onRecipeServingsChanged = viewModel::onRecipeServingsToLogChanged,
                        onProductNameChanged = viewModel::onProductNameChanged,
                        onBrandChanged = viewModel::onBrandChanged,
                        onQuantityChanged = viewModel::onQuantityChanged,
                        onAmountServingChoiceSelected = viewModel::onAmountServingChoiceSelected,
                        onCaloriesChanged = viewModel::onCaloriesChanged,
                        onProteinChanged = viewModel::onProteinChanged,
                        onCarbsChanged = viewModel::onCarbsChanged,
                        onFatChanged = viewModel::onFatChanged,
                        onBarcodeChanged = viewModel::onBarcodeChanged,
                        onLookupClick = viewModel::lookupBarcode,
                        onScanClick = onScanClick,
                        onNutritionLabelScanClick = {
                            viewModel.openNutritionLabelScan()
                            navigation.open(FoodNutritionLabelReviewNavKey)
                        },
                        onAiTextChanged = viewModel::onAiLoggingTextChanged,
                        onAiTextDraftClick = viewModel::generateAiTextFoodDraft,
                        onAiVoiceClick = viewModel::startAiVoiceLoggingPlaceholder,
                        onAiPhotoClick = viewModel::startAiPhotoLoggingPlaceholder,
                        onLogFoodClick = viewModel::logFood,
                        onSaveProductClick = viewModel::saveScannedProductToDatabase,
                        onQuickCaloriesChanged = viewModel::onQuickCaloriesChanged,
                        onQuickProteinChanged = viewModel::onQuickProteinChanged,
                        onQuickCarbsChanged = viewModel::onQuickCarbsChanged,
                        onQuickFatChanged = viewModel::onQuickFatChanged,
                        onQuickLogClick = viewModel::quickLog,
                        onQuickSaveFavoriteClick = viewModel::saveFavoriteQuickLog,
                        onFavoriteQuickLogClick = viewModel::logFavoriteQuickLog,
                        onFavoriteQuickLogFavoriteClick = viewModel::toggleFavoriteQuickLog,
                    )

                FoodSheetMode.FoodDatabase ->
                    FoodDatabasePanel(
                        state = state,
                        onSearchChanged = viewModel::onFoodDatabaseQueryChanged,
                        onSearchOnlineClick = viewModel::searchOnlineFoods,
                        onNewFoodClick = {
                            viewModel.openNewSavedFoodEditor()
                            navigation.open(FoodSavedFoodEditorNavKey())
                        },
                        onBarcodeCompareClick = {
                            viewModel.openBarcodeComparison()
                            navigation.open(FoodBarcodeComparisonNavKey)
                        },
                        onOpenFoodDetailClick = { foodId ->
                            viewModel.openSavedFoodDetail(foodId)
                            navigation.open(FoodDetailNavKey(foodId))
                        },
                        onEditFoodClick = { foodId ->
                            viewModel.openSavedFoodEditor(foodId)
                            navigation.open(FoodSavedFoodEditorNavKey(foodId))
                        },
                        onSaveOnlineFoodClick = viewModel::saveOnlineFoodResult,
                        onImportStarterFoodsClick = viewModel::seedStarterFoods,
                        onNutritionLabelScanClick = {
                            viewModel.openNutritionLabelScan()
                            navigation.open(FoodNutritionLabelReviewNavKey)
                        },
                        onMergeDuplicateFoodsClick = viewModel::mergeDuplicateFoods,
                        onFavoriteClick = viewModel::toggleFavoriteFood,
                        onReportFoodClick = viewModel::reportSavedFoodForReview,
                    )

                FoodSheetMode.FoodDetail ->
                    FoodDetailPanel(
                        state = state,
                        onEditClick = {
                            state.selectedSavedFoodDetail?.id?.let { foodId ->
                                viewModel.openSavedFoodEditor(foodId)
                                navigation.open(FoodSavedFoodEditorNavKey(foodId))
                            }
                        },
                        onLogClick = { state.selectedSavedFoodDetail?.id?.let(viewModel::logSavedFood) },
                        onFavoriteClick = {
                            state.selectedSavedFoodDetail?.let { food ->
                                viewModel.toggleFavoriteFood(food.id, !food.isFavorite)
                            }
                        },
                        onReportClick = {
                            state.selectedSavedFoodDetail?.id?.let(viewModel::reportSavedFoodForReview)
                        },
                        onCorrectClick = {
                            state.selectedSavedFoodDetail?.id?.let(viewModel::startSavedFoodCorrection)
                        },
                        onQuantityChanged = viewModel::onSavedFoodQuantityChanged,
                        onServingSelected = viewModel::onSavedFoodServingSelected,
                    )

                FoodSheetMode.DiaryEntryEditor ->
                    DiaryEntryEditorPanel(
                        state = state,
                        onMealChanged = viewModel::onDiaryEntryMealChanged,
                        onQuantityChanged = viewModel::onDiaryEntryQuantityChanged,
                        onServingChoiceSelected = viewModel::onDiaryEntryServingChoiceSelected,
                        onSaveClick = viewModel::saveDiaryEntry,
                        onDeleteClick = viewModel::deleteDiaryEntry,
                        onCopyToMealClick = { mealType -> viewModel.copyDiaryEntryTo(mealType, state.selectedDate) },
                        onCopyTomorrowClick = {
                            viewModel.copyDiaryEntryTo(state.diaryEntryEditor?.mealType.orEmpty(), state.selectedDate.plusDays(1))
                        },
                        onMarkLoggedClick = viewModel::markDiaryEntryLogged,
                    )

                // Hosted full-screen above (Turn 9), like RecipeBrowser.
                FoodSheetMode.SavedFoodEditor -> Unit

                FoodSheetMode.NutritionLabelScan ->
                    NutritionLabelScanPanel(
                        state = state,
                        onNameChanged = viewModel::onSavedFoodNameChanged,
                        onBrandChanged = viewModel::onSavedFoodBrandChanged,
                        onServingChanged = viewModel::onSavedFoodServingChanged,
                        onCaloriesChanged = viewModel::onSavedFoodCaloriesChanged,
                        onProteinChanged = viewModel::onSavedFoodProteinChanged,
                        onCarbsChanged = viewModel::onSavedFoodCarbsChanged,
                        onFatChanged = viewModel::onSavedFoodFatChanged,
                        onFiberChanged = viewModel::onSavedFoodFiberChanged,
                        onSugarChanged = viewModel::onSavedFoodSugarChanged,
                        onSaturatedFatChanged = viewModel::onSavedFoodSaturatedFatChanged,
                        onSodiumChanged = viewModel::onSavedFoodSodiumChanged,
                        onPotassiumChanged = viewModel::onSavedFoodPotassiumChanged,
                        onCalciumChanged = viewModel::onSavedFoodCalciumChanged,
                        onIronChanged = viewModel::onSavedFoodIronChanged,
                        onVitaminDChanged = viewModel::onSavedFoodVitaminDChanged,
                        onVitaminCChanged = viewModel::onSavedFoodVitaminCChanged,
                        onMagnesiumChanged = viewModel::onSavedFoodMagnesiumChanged,
                        onServingNameChanged = viewModel::onSavedFoodServingNameChanged,
                        onCategoryChanged = viewModel::onSavedFoodCategoryChanged,
                        onSaveClick = viewModel::saveSavedFood,
                    )

                FoodSheetMode.BarcodeComparison ->
                    BarcodeComparisonPanel(
                        state = state,
                        onBarcodeChanged = viewModel::onBarcodeComparisonBarcodeChanged,
                        onCompareClick = viewModel::compareBarcodeProducts,
                    )

                FoodSheetMode.FastingTimer ->
                    FastingTimerPanel(
                        state = state,
                        onProgramSelected = viewModel::selectFastingProgram,
                        onStartTimeChanged = viewModel::onFastingStartTimeChanged,
                        onCustomFastingChanged = viewModel::onCustomFastingHoursChanged,
                        onCustomEatingChanged = viewModel::onCustomEatingHoursChanged,
                        onApplyCustomClick = viewModel::applyCustomFastingProgram,
                    )

                // Hosted full-screen above (Turn 9), like RecipeBrowser.
                FoodSheetMode.GoalEditor -> Unit

                FoodSheetMode.RecipeBrowser -> Unit

                FoodSheetMode.RecipeEditor ->
                    RecipeEditorPanel(
                        state = state,
                        onNameChanged = viewModel::onRecipeNameChanged,
                        onCategoryChanged = viewModel::onRecipeCategoryChanged,
                        onServingNameChanged = viewModel::onRecipeServingNameChanged,
                        onServingsCountChanged = viewModel::onRecipeServingsCountChanged,
                        onCookedYieldChanged = viewModel::onRecipeCookedYieldGramsChanged,
                        onIngredientFoodChanged = viewModel::onRecipeIngredientFoodChanged,
                        onIngredientServingChoiceSelected = viewModel::onRecipeIngredientServingChoiceSelected,
                        onIngredientQuantityChanged = viewModel::onRecipeIngredientQuantityChanged,
                        onAddIngredientClick = viewModel::addRecipeIngredient,
                        onEditRecipeClick = { recipeId ->
                            viewModel.openRecipeEditor(recipeId)
                            navigation.open(FoodRecipeEditorNavKey(recipeId))
                        },
                        onDuplicateRecipeClick = viewModel::duplicateRecipe,
                        onFavoriteClick = viewModel::toggleFavoriteRecipe,
                        onDiscoveryFilterChanged = viewModel::selectRecipeDiscoveryFilter,
                        onDiscoveryItemClick = { itemId -> viewModel.useRecipeDiscoveryItem(itemId) },
                        onSaveClick = viewModel::saveRecipe,
                        onDeleteClick = { state.recipeEditor?.editingRecipeId?.let(viewModel::deleteRecipe) },
                    )

                FoodSheetMode.MealTemplates ->
                    MealTemplatesPanel(
                        state = state,
                        onTemplateClick = viewModel::logMealTemplate,
                        onEditClick = { templateId ->
                            viewModel.openMealTemplateEditor(templateId)
                            navigation.open(FoodMealTemplateEditorNavKey(templateId))
                        },
                        onDuplicateClick = viewModel::duplicateMealTemplate,
                        onDeleteClick = viewModel::deleteMealTemplate,
                        onFavoriteClick = viewModel::toggleFavoriteMealTemplate,
                        onNameChanged = viewModel::onTemplateNameChanged,
                        onMealTypeChanged = viewModel::onTemplateMealTypeChanged,
                        onTemplateItemQuantityChanged = viewModel::onTemplateDraftItemQuantityChanged,
                        onTemplateItemRemoveClick = viewModel::removeTemplateDraftItem,
                        onTemplateItemFoodChanged = viewModel::onTemplateItemFoodChanged,
                        onTemplateNewItemQuantityChanged = viewModel::onTemplateNewItemQuantityChanged,
                        onTemplateAddItemClick = viewModel::addTemplateItem,
                        onSaveEditClick = viewModel::saveMealTemplateEdits,
                    )

                FoodSheetMode.MealSettings ->
                    MealSettingsPanel(
                        state = state,
                        onEditClick = { mealId ->
                            viewModel.openMealDefinitionEditor(mealId)
                            navigation.open(FoodMealDefinitionEditorNavKey(mealId))
                        },
                        onToggleHidden = viewModel::toggleMealHidden,
                        onNameChanged = viewModel::onCustomMealNameChanged,
                        onTimeChanged = viewModel::onCustomMealTimeChanged,
                        onSortOrderChanged = viewModel::onCustomMealSortOrderChanged,
                        onSaveClick = viewModel::saveCustomMealDefinition,
                    )

                FoodSheetMode.ShoppingList ->
                    ShoppingListPanel(
                        state = state,
                        onStartDateChanged = viewModel::onShoppingStartDateChanged,
                        onEndDateChanged = viewModel::onShoppingEndDateChanged,
                        onGenerateClick = viewModel::generateShoppingList,
                        onManualNameChanged = viewModel::onManualShoppingNameChanged,
                        onManualCategoryChanged = viewModel::onManualShoppingCategoryChanged,
                        onManualQuantityChanged = viewModel::onManualShoppingQuantityChanged,
                        onAddManualClick = viewModel::addManualShoppingListItem,
                        onToggleItem = viewModel::toggleShoppingListItem,
                    )

                FoodSheetMode.Water,
                FoodSheetMode.HealthConnect,
                -> Unit
            }
        }
    }
}

@Composable
@Suppress("LongMethod")
private fun FoodDiaryHome(
    state: FoodDiaryUiState,
    accent: TabAccent,
    actions: FoodDiaryActions,
) {
    var selectedDiaryTab by rememberSaveable { mutableStateOf(FoodDiaryTab.Diary) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            MusFitScreenHeader(
                title = stringResource(R.string.food_title),
                actions = {
                    FoodDateChip(
                        date = state.selectedDate,
                        onPreviousDayClick = actions.onPreviousDay,
                        onNextDayClick = actions.onNextDay,
                        onTodayClick = actions.onToday,
                    )
                    FoodDiaryOverflowAction(
                        isSaving = state.isSaving,
                        onGoalClick = actions.onOpenGoal,
                        onMealsClick = actions.onOpenMeals,
                        onTemplatesClick = actions.onOpenTemplates,
                        onShoppingClick = actions.onOpenShopping,
                        onRecipesClick = actions.onOpenRecipes,
                        onFastingClick = actions.onOpenFasting,
                        onHealthConnectClick = actions.onOpenHealthConnect,
                        onFoodDatabaseClick = actions.onOpenDatabase,
                        onCopyDayToTomorrowClick = actions.onCopyDayToTomorrow,
                    )
                },
            )
            FoodDiarySummaryCard(state = state, accent = accent)
            if (state.macroProgress.isNotEmpty()) MacroProgressRow(state.macroProgress)
            FoodWaterRow(
                consumedMilliliters = state.waterConsumedMilliliters,
                goalMilliliters = state.waterGoalMilliliters,
                onWaterClick = actions.onOpenWater,
                onQuickAddClick = actions.onQuickAddWater,
                onQuickRemoveClick = actions.onQuickRemoveWater,
            )
            MessageBanner(
                message = state.message,
                canUndoDelete = state.canUndoDelete,
                onUndoDeleteClick = actions.onUndoDelete,
            )
            MusFitSegmented(
                options = FoodDiaryTab.entries,
                selected = selectedDiaryTab,
                accent = accent,
                label = { it.name },
                onSelect = { selectedDiaryTab = it },
            )
            when (selectedDiaryTab) {
                FoodDiaryTab.Diary -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.mealSections.forEachIndexed { index, meal ->
                        MealSummaryRow(
                            meal = meal,
                            badgeShape = expressiveBadgeShapeFor(index),
                            shape = groupedShape(index, state.mealSections.size),
                            onMealClick = {
                                if (meal.entries.isNotEmpty()) actions.onOpenMeal(meal.id) else actions.onAddFood(meal.id)
                            },
                            onAddClick = { actions.onAddFood(meal.id) },
                        )
                    }
                }

                FoodDiaryTab.Summary -> {
                    DayRatingCard(state.dayRating)
                    DailyInsightsSection(state.dailyInsights)
                    FoodHabitTrackerSection(state.habitTrackers)
                    AdvancedNutritionProgressRow(state.advancedNutritionProgress)
                    MicronutrientRow(state.micronutrients)
                }
            }
        }
    }
}

@Composable
private fun FoodWaterTrackerSheet(viewModel: FoodViewModel) {
    val state by viewModel.trackerState.collectAsStateWithLifecycle()
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        WaterTrackerCard(
            state = state,
            onQuickWaterClick = viewModel::logQuickWater,
            onRemoveWaterClick = viewModel::removeQuickWater,
            onCustomAmountChanged = viewModel::onWaterCustomAmountChanged,
            onCustomAddClick = viewModel::logCustomWater,
            onCustomRemoveClick = viewModel::removeCustomWater,
            onGoalChanged = viewModel::onWaterGoalChanged,
            onGoalSaveClick = viewModel::saveWaterGoal,
        )
    }
}

@Composable
private fun FoodHealthConnectTrackerSheet(
    viewModel: FoodViewModel,
    onRequestPermissions: (Set<String>) -> Unit,
) {
    val state by viewModel.trackerState.collectAsStateWithLifecycle()
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        FoodHealthConnectSyncCard(
            state = state,
            onEnabledChanged = viewModel::onFoodHealthConnectSyncEnabledChanged,
            onRequestPermissionsClick = {
                if (state.foodHealthConnectCanRequestPermissions) {
                    onRequestPermissions(state.foodHealthConnectRequestablePermissions)
                }
            },
            onRefreshClick = viewModel::refreshFoodHealthConnectSync,
            onSyncClick = viewModel::syncFoodToHealthConnect,
        )
    }
}

// One 250 ml glass — the amount the water row's quick-add "+" logs, matching the
// water sheet's smallest quick preset.
private const val WATER_QUICK_ADD_MILLILITERS = 250.0

/**
 * Day switcher for the tab header (mock 6b): a floating white pill — ‹ chevron,
 * the selected date as an emphasized label (tap to jump back to today), ›
 * chevron — with a whisper of shadow so it reads as a control on the cream.
 */
@Composable
private fun FoodDateChip(
    date: java.time.LocalDate,
    onPreviousDayClick: () -> Unit,
    onNextDayClick: () -> Unit,
    onTodayClick: () -> Unit,
) {
    val accent = tabAccentFor(TabAccentRole.Food)
    val locale = LocalConfiguration.current.locales[0]
    val formattedDate = remember(date, locale) {
        date.format(DateTimeFormatter.ofPattern("EEE · d MMM", locale))
    }
    Surface(
        color = MusFitTheme.colors.surface,
        shape = CircleShape,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            IconButton(onClick = onPreviousDayClick, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.food_previous_day),
                    tint = MusFitTheme.colors.onSurface,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accent.onContainer,
                maxLines = 1,
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(CircleShape)
                    .clickable(
                        onClickLabel = stringResource(R.string.food_jump_to_today),
                        role = Role.Button,
                        onClick = onTodayClick,
                    )
                    .padding(horizontal = 4.dp, vertical = 6.dp),
            )
            IconButton(onClick = onNextDayClick, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.food_next_day),
                    tint = MusFitTheme.colors.onSurface,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/**
 * The mock-6b water card: a white card with a filled drop icon, "Water" label
 * with the consumed/goal figure, a segmented glass tracker (one cell per 250 ml
 * glass — outer corners fully round, inner 6dp, and the fill edge rounds too so
 * the shape itself marks progress), and a trailing tonal add button. The
 * segments are decorative progress; quick logging belongs to the single
 * labelled add control, while long actions live behind [onWaterClick].
 */
@Composable
private fun FoodWaterRow(
    consumedMilliliters: Double,
    goalMilliliters: Double,
    onWaterClick: () -> Unit,
    onQuickAddClick: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onQuickRemoveClick: () -> Unit,
) {
    val waterColor = MusFitTheme.colors.water
    val emptyColor = MusFitTheme.colors.waterFill
    val consumedLiters = LocalizedFormatter.number(
        consumedMilliliters / 1000.0,
        minimumFractionDigits = 1,
        maximumFractionDigits = 1,
    )
    val goalLiters = LocalizedFormatter.number(
        goalMilliliters / 1000.0,
        minimumFractionDigits = 1,
        maximumFractionDigits = 1,
    )
    val waterProgress = stringResource(R.string.food_water_liters_progress, consumedLiters, goalLiters)
    // Each segment is one 250 ml glass (so the "+" fills exactly one).
    val segmentCount = (goalMilliliters / WATER_QUICK_ADD_MILLILITERS).roundToInt().coerceIn(1, 12)
    val filledSegments = (consumedMilliliters / WATER_QUICK_ADD_MILLILITERS).roundToInt().coerceIn(0, segmentCount)
    Surface(
        onClick = onWaterClick,
        color = MusFitTheme.colors.surface,
        shape = MusFitTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Filled.WaterDrop,
                contentDescription = null,
                tint = waterColor,
                modifier = Modifier.size(20.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.food_water),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MusFitTheme.colors.onSurface,
                        maxLines = 1,
                    )
                    Text(
                        text = waterProgress,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MusFitTheme.colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(segmentCount) { index ->
                        val filled = index < filledSegments
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(20.dp)
                                .clip(waterSegmentShape(index, segmentCount, filledSegments))
                                .background(if (filled) waterColor else emptyColor),
                        )
                    }
                }
            }
            Surface(
                onClick = onQuickAddClick,
                color = emptyColor,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(R.string.food_add_water),
                        tint = waterColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/**
 * Segment corners per mock 6b: the tracker's outer ends are fully round, inner
 * edges 6dp — and the progress edge rounds as well (the last filled cell's
 * trailing corners), so the fill reads as its own pill.
 */
private fun waterSegmentShape(index: Int, count: Int, filledCount: Int): RoundedCornerShape {
    val round = 99.dp
    val inner = 6.dp
    val isFirst = index == 0
    val isLast = index == count - 1
    val isFillEdge = filledCount in 1 until count && index == filledCount - 1
    val left = if (isFirst) round else inner
    val right = if (isLast || isFillEdge) round else inner
    return RoundedCornerShape(topStart = left, bottomStart = left, topEnd = right, bottomEnd = right)
}

@Composable
internal fun FoodUiState.foodEntryActionVerb(): String = stringResource(if (isPlanningMode) R.string.food_plan else R.string.food_log)

@Composable
internal fun FoodUiState.foodEntryActionProgressLabel(): String = stringResource(if (isPlanningMode) R.string.food_planning else R.string.food_logging)

@Composable
internal fun FoodUiState.foodEntryActionLabel(target: String): String = stringResource(R.string.food_action_target, foodEntryActionVerb(), target)

@Composable
internal fun FoodUiState.saveAndFoodEntryActionLabel(): String = stringResource(if (isPlanningMode) R.string.food_save_and_plan else R.string.food_save_and_log)

@Composable
@Suppress("LongParameterList")
private fun FoodDiaryOverflowAction(
    isSaving: Boolean,
    onGoalClick: () -> Unit,
    onMealsClick: () -> Unit,
    onTemplatesClick: () -> Unit,
    onShoppingClick: () -> Unit,
    onRecipesClick: () -> Unit,
    onFastingClick: () -> Unit,
    onHealthConnectClick: () -> Unit,
    onFoodDatabaseClick: () -> Unit,
    onCopyDayToTomorrowClick: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    IconButton(onClick = { menuOpen = true }) {
        Icon(
            Icons.Filled.MoreVert,
            contentDescription = stringResource(R.string.food_more_actions),
            tint = MusFitTheme.colors.onSurfaceVariant,
        )
    }
    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
        DropdownMenuItem(text = { Text(stringResource(R.string.food_recipes)) }, onClick = {
            menuOpen = false
            onRecipesClick()
        })
        DropdownMenuItem(text = { Text(stringResource(R.string.food_goals)) }, onClick = {
            menuOpen = false
            onGoalClick()
        })
        DropdownMenuItem(text = { Text(stringResource(R.string.food_meals)) }, onClick = {
            menuOpen = false
            onMealsClick()
        })
        DropdownMenuItem(text = { Text(stringResource(R.string.food_templates)) }, onClick = {
            menuOpen = false
            onTemplatesClick()
        })
        DropdownMenuItem(text = { Text(stringResource(R.string.food_shopping_list)) }, onClick = {
            menuOpen = false
            onShoppingClick()
        })
        DropdownMenuItem(text = { Text(stringResource(R.string.food_fasting)) }, onClick = {
            menuOpen = false
            onFastingClick()
        })
        DropdownMenuItem(text = { Text(stringResource(R.string.food_health_connect)) }, onClick = {
            menuOpen = false
            onHealthConnectClick()
        })
        DropdownMenuItem(text = { Text(stringResource(R.string.food_database)) }, onClick = {
            menuOpen = false
            onFoodDatabaseClick()
        })
        DropdownMenuItem(
            text = { Text(stringResource(R.string.food_copy_day_to_tomorrow)) },
            enabled = !isSaving,
            onClick = {
                menuOpen = false
                onCopyDayToTomorrowClick()
            },
        )
    }
}

/**
 * The Food hero (mock 6b): a green tonal container with a semicircular calorie
 * gauge — translucent white track, accent arc with a white dot at its end, the
 * remaining kcal as an emphasized display in the middle — and an eaten/goal
 * footer row.
 */
@Composable
private fun FoodDiarySummaryCard(
    state: FoodDiaryUiState,
    accent: TabAccent,
) {
    Surface(
        color = accent.container,
        shape = MusFitTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CalorieGauge(
                eatenCalories = state.eatenCaloriesKcal,
                remainingCalories = state.remainingCaloriesKcal,
                calorieGoal = state.effectiveCalorieBudgetKcal,
                accent = accent,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GaugeFooterStat(
                    figure = LocalizedFormatter.integer(state.eatenCaloriesKcal.roundToInt().toLong()),
                    label = stringResource(R.string.food_eaten),
                    accent = accent,
                )
                GaugeFooterStat(
                    figure = LocalizedFormatter.integer(state.calorieGoalKcal.roundToInt().toLong()),
                    label = stringResource(R.string.food_goal),
                    accent = accent,
                )
            }
        }
    }
}

@Composable
private fun GaugeFooterStat(figure: String, label: String, accent: TabAccent) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = accent.onContainer)) {
                append(figure)
            }
            append(" $label")
        },
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
        color = accent.onContainerVariant,
        maxLines = 1,
    )
}

/** The mock-6b semicircular gauge; the arc sweeps in on entry with a spring. */
@Composable
private fun CalorieGauge(
    eatenCalories: Double,
    remainingCalories: Double,
    calorieGoal: Double,
    accent: TabAccent,
) {
    val progress = (eatenCalories / calorieGoal).toFloat().coerceIn(0f, 1f)
    val animated = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(progress) { animated.animateTo(progress, MusFitMotion.spatial()) }
    val trackColor = Color.White.copy(alpha = 0.7f)
    val arcColor = accent.color

    Box(
        modifier = Modifier
            .size(width = 216.dp, height = 128.dp)
            .padding(top = 4.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 15.dp.toPx()
            val dotRadius = 5.dp.toPx()
            val arcSize = Size(size.width - strokeWidth, (size.width - strokeWidth))
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
            drawArc(
                color = trackColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            val sweep = 180f * animated.value
            if (sweep > 0f) {
                drawArc(
                    color = arcColor,
                    startAngle = 180f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
            // White dot riding the end of the arc.
            val angleRad = Math.toRadians((180f + sweep).toDouble())
            val radius = arcSize.width / 2f
            val center = Offset(size.width / 2f, strokeWidth / 2f + radius)
            drawCircle(
                color = Color.White,
                radius = dotRadius,
                center = Offset(
                    x = center.x + radius * kotlin.math.cos(angleRad).toFloat(),
                    y = center.y + radius * kotlin.math.sin(angleRad).toFloat(),
                ),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = LocalizedFormatter.integer(remainingCalories.roundToInt().toLong()),
                style = MaterialTheme.typography.displayMedium,
                color = accent.onContainer,
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.food_kcal_left),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = accent.onContainerVariant,
            )
        }
    }
}

/** The mock-6b macros card: three columns, each with a mini wavy indicator. */
@Composable
internal fun MacroProgressRow(macros: List<FoodMacroProgressUiState>) {
    val macroColors = MusFitTheme.colors.macroColors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MusFitTheme.colors.surface,
        shape = MusFitTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            macros.forEachIndexed { index, macro ->
                MacroProgressColumn(
                    macro = macro,
                    color = macroColors[index % macroColors.size],
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MacroProgressColumn(
    macro: FoodMacroProgressUiState,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = macro.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MusFitTheme.colors.onSurface,
                maxLines = 1,
            )
            Text(
                text = stringResource(
                    R.string.food_macro_progress,
                    LocalizedFormatter.integer(macro.currentGrams.roundToInt().toLong()),
                    LocalizedFormatter.integer(macro.goalGrams.roundToInt().toLong()),
                ),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MusFitTheme.colors.onSurfaceVariant,
                maxLines = 1,
            )
        }
        WavyProgressBar(
            progress = (macro.currentGrams / macro.goalGrams).toFloat().coerceIn(0f, 1f),
            color = color,
            trackColor = color.copy(alpha = 0.25f),
            strokeWidth = 3.5.dp,
            amplitude = 2.5.dp,
            wavelength = 15.dp,
        )
    }
}

@Composable
private fun DayRatingCard(rating: FoodRatingUiState) {
    val accent = rating.tone.ratingColor()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = accent.copy(alpha = 0.10f).compositeOver(MusFitTheme.colors.surface),
        shape = MusFitTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.food_day_rating),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MusFitTheme.colors.brandInk,
                    )
                    Text(
                        text = rating.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                RatingPill(rating)
            }
            Text(
                text = rating.suggestion,
                style = MaterialTheme.typography.bodySmall,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (rating.factors.isNotEmpty()) {
                HorizontalDivider(color = MusFitTheme.colors.outline)
                RatingFactorColumn(rating.factors)
            }
        }
    }
}

@Composable
internal fun RatingFactorColumn(factors: List<FoodRatingFactorUiState>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        factors.forEach { factor ->
            RatingFactorRow(factor)
        }
    }
}

@Composable
private fun RatingFactorRow(factor: FoodRatingFactorUiState) {
    val accent = factor.tone.ratingColor()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(accent),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = factor.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MusFitTheme.colors.brandInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = factor.valueLabel.asString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = factor.explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MusFitTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FoodHabitTrackerSection(habits: List<FoodHabitTrackerUiState>) {
    if (habits.isEmpty()) {
        return
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MusFitTheme.colors.surface,
        shape = MusFitTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.food_daily_habits),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MusFitTheme.colors.brandInk,
                )
                Text(
                    text = stringResource(
                        R.string.food_macro_progress,
                        LocalizedFormatter.integer(habits.count { it.status == FoodHabitStatus.Complete }.toLong()),
                        LocalizedFormatter.integer(habits.size.toLong()),
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MusFitTheme.colors.brand,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            habits.forEach { habit ->
                HabitTrackerRow(habit)
            }
        }
    }
}

@Composable
private fun HabitTrackerRow(habit: FoodHabitTrackerUiState) {
    val accent = habit.tone.ratingColor()
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MusFitTheme.colors.brandInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = habit.suggestion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = habit.status.label(),
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    text = habit.valueLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        ProgressBar(progress = habit.progress.toFloat().coerceIn(0f, 1f), color = accent)
    }
}

@Composable
private fun DailyInsightsSection(insights: List<FoodInsightUiState>) {
    if (insights.isEmpty()) {
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.food_insights),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MusFitTheme.colors.brandInk,
        )
        insights.forEach { insight ->
            DailyInsightCard(insight)
        }
    }
}

@Composable
private fun DailyInsightCard(insight: FoodInsightUiState) {
    val accent = insight.tone.ratingColor()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MusFitTheme.colors.surface,
        shape = MusFitTheme.shapes.extraLarge,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 42.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MusFitTheme.colors.brandInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = insight.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun RatingPill(rating: FoodRatingUiState) {
    Surface(
        color = rating.tone.ratingColor().copy(alpha = 0.14f),
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = rating.label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = rating.tone.ratingColor(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            maxLines = 1,
        )
    }
}

@Composable
internal fun FoodInsightTone.ratingColor(): Color = when (this) {
    FoodInsightTone.Positive -> MusFitTheme.colors.brand
    FoodInsightTone.Warning -> MusFitTheme.colors.warning
    FoodInsightTone.Neutral -> MusFitTheme.colors.onSurfaceVariant
}

@Composable
private fun FoodHabitStatus.label(): String = stringResource(
    when (this) {
        FoodHabitStatus.Complete -> R.string.food_status_done
        FoodHabitStatus.InProgress -> R.string.food_status_in_progress
        FoodHabitStatus.Missing -> R.string.food_status_not_yet
    },
)

@Composable
private fun AdvancedNutritionProgressRow(nutrients: List<FoodNutrientProgressUiState>) {
    if (nutrients.isEmpty()) {
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        nutrients.forEach { nutrient ->
            AdvancedNutritionProgressCard(
                nutrient = nutrient,
                modifier = Modifier.width(138.dp),
            )
        }
    }
}

@Composable
private fun AdvancedNutritionProgressColumn(nutrients: List<FoodNutrientProgressUiState>) {
    if (nutrients.isEmpty()) {
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        nutrients.forEach { nutrient ->
            AdvancedNutritionProgressListRow(nutrient)
        }
    }
}

@Composable
private fun AdvancedNutritionProgressListRow(nutrient: FoodNutrientProgressUiState) {
    val goal = nutrient.goalValue.takeIf { it.isFinite() && it > 0.0 } ?: 0.0
    val progress = if (goal > 0.0) {
        (nutrient.currentValue / goal).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    val overLimit = nutrient.isLimit && nutrient.currentValue > nutrient.goalValue
    val barColor =
        when {
            overLimit -> MusFitTheme.colors.warning
            nutrient.isLimit -> MusFitTheme.colors.brand
            nutrient.currentValue >= nutrient.goalValue -> MusFitTheme.colors.brand
            else -> MusFitTheme.colors.onSurfaceVariant
        }
    val currentText =
        if (nutrient.unit == "mg") {
            LocalizedFormatter.integer(nutrient.currentValue.roundToInt().toLong())
        } else {
            LocalizedFormatter.number(nutrient.currentValue, maximumFractionDigits = 1)
        }
    val goalText =
        if (nutrient.unit == "mg") {
            LocalizedFormatter.integer(nutrient.goalValue.roundToInt().toLong())
        } else {
            LocalizedFormatter.number(nutrient.goalValue, maximumFractionDigits = 1)
        }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = nutrient.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MusFitTheme.colors.brandInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = currentText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (overLimit) MusFitTheme.colors.warning else MusFitTheme.colors.brandInk,
                    maxLines = 1,
                )
                Text(
                    text = stringResource(R.string.food_nutrient_goal, goalText, nutrient.unit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        ProgressBar(progress = progress, color = barColor)
    }
}

@Composable
private fun AdvancedNutritionProgressCard(
    nutrient: FoodNutrientProgressUiState,
    modifier: Modifier = Modifier,
) {
    val goal = nutrient.goalValue.takeIf { it.isFinite() && it > 0.0 } ?: 0.0
    val progress = if (goal > 0.0) {
        (nutrient.currentValue / goal).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    val color =
        when {
            nutrient.isLimit && nutrient.currentValue > nutrient.goalValue -> MusFitTheme.colors.warning
            nutrient.isLimit -> MusFitTheme.colors.brand
            nutrient.currentValue >= nutrient.goalValue -> MusFitTheme.colors.brand
            else -> MusFitTheme.colors.onSurfaceVariant
        }
    val currentText =
        if (nutrient.unit == "mg") {
            LocalizedFormatter.integer(nutrient.currentValue.roundToInt().toLong())
        } else {
            LocalizedFormatter.number(nutrient.currentValue, maximumFractionDigits = 1)
        }
    val goalText =
        if (nutrient.unit == "mg") {
            LocalizedFormatter.integer(nutrient.goalValue.roundToInt().toLong())
        } else {
            LocalizedFormatter.number(nutrient.goalValue, maximumFractionDigits = 1)
        }

    Surface(
        modifier = modifier.height(82.dp),
        color = MusFitTheme.colors.surface,
        shape = MusFitTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = nutrient.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.food_nutrient_progress,
                    currentText,
                    goalText,
                    nutrient.unit,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MusFitTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            ProgressBar(progress = progress, color = color)
        }
    }
}

@Composable
private fun MicronutrientRow(micronutrients: List<FoodMicronutrientUiState>) {
    if (micronutrients.isEmpty()) {
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        micronutrients.forEach { micronutrient ->
            MicronutrientCard(
                micronutrient = micronutrient,
                modifier = Modifier.width(126.dp),
            )
        }
    }
}

@Composable
private fun MicronutrientGrid(micronutrients: List<FoodMicronutrientUiState>) {
    if (micronutrients.isEmpty()) {
        return
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MusFitTheme.colors.surfaceVariant,
        shape = MusFitTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            micronutrients.chunked(2).forEach { rowNutrients ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    rowNutrients.forEach { micronutrient ->
                        MicronutrientStat(
                            micronutrient = micronutrient,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowNutrients.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MicronutrientStat(
    micronutrient: FoodMicronutrientUiState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = micronutrient.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MusFitTheme.colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(
                R.string.food_value_with_unit,
                LocalizedFormatter.number(
                    micronutrient.value,
                    maximumFractionDigits = if (micronutrient.value < 10.0) 1 else 0,
                ),
                micronutrient.unit,
            ),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MusFitTheme.colors.brandInk,
            maxLines = 1,
        )
    }
}

@Composable
private fun MoreNutritionSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MusFitTheme.colors.onSurfaceVariant,
        )
        content()
    }
}

@Composable
private fun MicronutrientCard(
    micronutrient: FoodMicronutrientUiState,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(68.dp),
        color = MusFitTheme.colors.surfaceVariant,
        shape = MusFitTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = micronutrient.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.food_value_with_unit,
                    LocalizedFormatter.number(
                        micronutrient.value,
                        maximumFractionDigits = if (micronutrient.value < 10.0) 1 else 0,
                    ),
                    micronutrient.unit,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MusFitTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Meal detail (Turn 9 / 9a): tonal-circle header with an overflow menu, a green
 * summary hero (kcal, rating chip, wavy macros, share-of-day footer), sort
 * chips, dense grouped item rows, and a pinned bottom action bar.
 */
@Composable
private fun MealDetailScreen(
    meal: FoodMealSectionUiState,
    sortMode: MealDetailSortMode,
    presentation: MealDetailPresentation,
    actions: MealDetailActions,
) {
    val selectedDate = presentation.selectedDate
    val dayCalorieBudgetKcal = presentation.dayCalorieBudgetKcal
    val accent = tabAccentFor(TabAccentRole.Food)
    val locale = LocalConfiguration.current.locales[0]
    val selectedDateLabel = remember(selectedDate, locale) {
        selectedDate.format(DateTimeFormatter.ofPattern("EEE d MMM", locale))
    }
    var detailExpanded by rememberSaveable(meal.id) { mutableStateOf(false) }
    val hasDetail = meal.rating?.factors?.isNotEmpty() == true ||
        meal.advancedNutritionProgress.isNotEmpty() ||
        meal.micronutrients.isNotEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val itemsLabel = pluralStringResource(R.plurals.food_item_count, meal.entries.size, meal.entries.size)
            InnerScreenHeader(
                title = meal.title,
                onBack = actions.onBackClick,
                subtitle = stringResource(R.string.food_meal_summary, selectedDateLabel, itemsLabel),
                trailing = {
                    Box {
                        var menuOpen by remember { mutableStateOf(false) }
                        TonalHeaderIconButton(
                            icon = Icons.Outlined.MoreHoriz,
                            contentDescription = stringResource(R.string.food_meal_actions),
                            onClick = { menuOpen = true },
                            modifier = Modifier.size(48.dp),
                        )
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.food_copy_yesterday)) },
                                onClick = {
                                    menuOpen = false
                                    actions.onCopyYesterdayClick()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.food_save_as_template)) },
                                onClick = {
                                    menuOpen = false
                                    actions.onSaveTemplateClick()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.food_meal_settings)) },
                                onClick = {
                                    menuOpen = false
                                    actions.onMealSettingsClick()
                                },
                            )
                        }
                    }
                },
            )

            MessageBanner(
                message = presentation.message,
                canUndoDelete = presentation.canUndoDelete,
                onUndoDeleteClick = actions.onUndoDeleteClick,
            )

            MealDetailSummaryHero(
                meal = meal,
                accent = accent,
                dayCalorieBudgetKcal = dayCalorieBudgetKcal,
                showDetailToggle = hasDetail,
                onToggleDetail = { detailExpanded = !detailExpanded },
            )

            if (detailExpanded && hasDetail) {
                MealDetailDrillDownCard(meal)
            }

            if (meal.entries.isEmpty()) {
                Surface(
                    color = MusFitTheme.colors.surface,
                    shape = MusFitTheme.shapes.extraLarge,
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.food_no_food_logged),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.food_add_food_to_meal),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MusFitTheme.colors.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 2.dp),
                ) {
                    MealDetailSortChoices.forEach { choice ->
                        SelectableChip(
                            text = choice.label(),
                            selected = sortMode == choice,
                            onClick = { actions.onSortModeChanged(choice) },
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    meal.entries.forEachIndexed { index, entry ->
                        val quantityLabel = stringResource(
                            R.string.food_grams,
                            entry.quantityGrams.roundToInt(),
                        )
                        val carbs = LocalizedFormatter.number(entry.carbsGrams, maximumFractionDigits = 1)
                        val protein = LocalizedFormatter.number(entry.proteinGrams, maximumFractionDigits = 1)
                        val fat = LocalizedFormatter.number(entry.fatGrams, maximumFractionDigits = 1)
                        FoodListItemRow(
                            index = index,
                            count = meal.entries.size,
                            title = entry.name,
                            subtitle = stringResource(
                                R.string.food_entry_macros,
                                quantityLabel,
                                carbs,
                                protein,
                                fat,
                            ),
                            onClick = { actions.onEntryClick(entry.id) },
                            imageUrl = entry.imageUrl,
                            fallbackIcon = mealDetailEntryIcon(meal.id, meal.title),
                            trailingTop = LocalizedFormatter.integer(entry.caloriesKcal.roundToInt().toLong()),
                            trailingSub = if (entry.isPlanned) stringResource(R.string.food_planned) else quantityLabel,
                        )
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 18.dp),
        ) {
            PillButton(
                text = stringResource(R.string.food_add_food),
                onClick = actions.onAddFoodClick,
                icon = Icons.Filled.Add,
                modifier = Modifier.weight(1f),
            )
            TonalIconSquare(
                icon = Icons.Outlined.ContentCopy,
                contentDescription = stringResource(R.string.food_copy_yesterdays_meal, meal.title),
                onClick = actions.onCopyYesterdayClick,
                containerColor = accent.container,
                contentColor = accent.onContainer,
            )
            TonalIconSquare(
                icon = Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.food_meal_settings),
                onClick = actions.onMealSettingsClick,
            )
        }
    }
}

private data class MealDetailPresentation(
    val selectedDate: java.time.LocalDate,
    val dayCalorieBudgetKcal: Double,
    val message: UiText?,
    val canUndoDelete: Boolean,
)

private data class MealDetailActions(
    val onBackClick: () -> Unit,
    val onAddFoodClick: () -> Unit,
    val onCopyYesterdayClick: () -> Unit,
    val onSaveTemplateClick: () -> Unit,
    val onMealSettingsClick: () -> Unit,
    val onSortModeChanged: (MealDetailSortMode) -> Unit,
    val onEntryClick: (String) -> Unit,
    val onUndoDeleteClick: () -> Unit,
)

/**
 * The 9a summary hero: kcal on the Food container, a white-glass rating chip
 * that opens the rating/nutrition drill-down, three wavy mini macros on
 * white-glass tracks, and the meal's share of today's calorie budget.
 */
@Composable
private fun MealDetailSummaryHero(
    meal: FoodMealSectionUiState,
    accent: TabAccent,
    dayCalorieBudgetKcal: Double,
    showDetailToggle: Boolean,
    onToggleDetail: () -> Unit,
) {
    // The white-glass tint used for chips and wavy tracks on the tonal hero.
    val glass = MusFitTheme.colors.surface.copy(alpha = 0.75f)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = accent.container,
        shape = MusFitTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = LocalizedFormatter.integer(meal.caloriesKcal.roundToInt().toLong()),
                        style = HeroNumberMediumStyle,
                        color = accent.onContainer,
                    )
                    Text(
                        text = stringResource(R.string.food_kcal),
                        style = MaterialTheme.typography.labelMedium,
                        color = accent.onContainerVariant,
                        modifier = Modifier.padding(bottom = 5.dp),
                    )
                }
                val rating = meal.rating
                if (rating != null || showDetailToggle) {
                    Surface(
                        onClick = onToggleDetail,
                        enabled = showDetailToggle,
                        shape = RoundedCornerShape(99.dp),
                        color = glass,
                        contentColor = accent.onContainer,
                        modifier = Modifier.padding(bottom = 2.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            if (rating != null) {
                                Icon(
                                    Icons.Filled.ThumbUp,
                                    contentDescription = null,
                                    tint = accent.color,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                            Text(
                                text = rating?.label ?: stringResource(R.string.food_more_nutrition),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.W800,
                                    letterSpacing = 0.sp,
                                ),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                MealDetailHeroMacro(
                    label = meal.carbsLabel,
                    grams = meal.effectiveCarbsGrams,
                    goalGrams = meal.carbsGoalGrams,
                    color = MusFitTheme.colors.macroColors[0],
                    trackColor = glass,
                    accent = accent,
                    modifier = Modifier.weight(1f),
                )
                MealDetailHeroMacro(
                    label = stringResource(R.string.food_protein),
                    grams = meal.proteinGrams,
                    goalGrams = meal.proteinGoalGrams,
                    color = MusFitTheme.colors.macroColors[1],
                    trackColor = glass,
                    accent = accent,
                    modifier = Modifier.weight(1f),
                )
                MealDetailHeroMacro(
                    label = stringResource(R.string.food_fat),
                    grams = meal.fatGrams,
                    goalGrams = meal.fatGoalGrams,
                    color = MusFitTheme.colors.macroColors[2],
                    trackColor = glass,
                    accent = accent,
                    modifier = Modifier.weight(1f),
                )
            }

            if (dayCalorieBudgetKcal > 0) {
                val percentOfDay = ((meal.caloriesKcal / dayCalorieBudgetKcal) * 100).roundToInt()
                Text(
                    text = stringResource(
                        R.string.food_percent_of_daily_goal,
                        percentOfDay,
                        dayCalorieBudgetKcal.roundToInt(),
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = accent.onContainerVariant,
                )
            }
        }
    }
}

@Composable
private fun MealDetailHeroMacro(
    label: String,
    grams: Double,
    goalGrams: Double,
    color: Color,
    trackColor: Color,
    accent: TabAccent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.W800, letterSpacing = 0.sp),
                color = accent.onContainer,
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.food_grams, grams.roundToInt()),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                color = accent.onContainerVariant,
                maxLines = 1,
            )
        }
        WavyProgressBar(
            progress = if (goalGrams > 0) (grams / goalGrams).toFloat() else 0f,
            color = color,
            trackColor = trackColor,
        )
    }
}

/** The rating/nutrition drill-down the hero chip toggles (was "More nutrition"). */
@Composable
private fun MealDetailDrillDownCard(meal: FoodMealSectionUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MusFitTheme.colors.surface,
        shape = MusFitTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            meal.rating?.factors?.takeIf { it.isNotEmpty() }?.let {
                MoreNutritionSection(title = stringResource(R.string.food_rating_breakdown)) {
                    RatingFactorColumn(it)
                }
            }
            if (meal.advancedNutritionProgress.isNotEmpty()) {
                MoreNutritionSection(title = stringResource(R.string.food_nutrients)) {
                    AdvancedNutritionProgressColumn(meal.advancedNutritionProgress)
                }
            }
            if (meal.micronutrients.isNotEmpty()) {
                MoreNutritionSection(title = stringResource(R.string.food_micronutrients)) {
                    MicronutrientGrid(meal.micronutrients)
                }
            }
        }
    }
}

// Filled badge icon for meal-detail item rows (food badges are always filled;
// entries carry no per-food icon, so the meal's own icon stands in).
private fun mealDetailEntryIcon(id: String, title: String): ImageVector {
    val key = "$id $title".lowercase(Locale.ROOT)
    return when {
        "breakfast" in key -> Icons.Filled.BreakfastDining
        "lunch" in key -> Icons.Filled.LunchDining
        "dinner" in key -> Icons.Filled.DinnerDining
        "snack" in key -> Icons.Filled.Icecream
        else -> Icons.Filled.LocalDining
    }
}

@Composable
private fun MessageBanner(
    message: com.musfit.ui.text.UiText?,
    canUndoDelete: Boolean,
    onUndoDeleteClick: () -> Unit,
) {
    message ?: return

    Surface(
        color = MusFitTheme.colors.positiveContainer,
        shape = MusFitTheme.shapes.extraLarge,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message.asString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (canUndoDelete) {
                MusFitOutlinedButton(
                    onClick = onUndoDeleteClick,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(stringResource(R.string.food_undo))
                }
            }
        }
    }
}

/**
 * A meal as an M3E grouped list (mock 6b): a white header row — 48dp sunny badge
 * in the Food container tint, 17/w800 title, quiet kcal/rating meta, trailing
 * filled 44dp add button — followed by one white row per logged item, all with
 * 4dp gaps and 24dp-outer/8dp-inner corners.
 */
@Composable
private fun MealSummaryRow(
    meal: FoodMealSectionUiState,
    badgeShape: ExpressiveBadgeShape,
    shape: RoundedCornerShape,
    onMealClick: () -> Unit,
    onAddClick: () -> Unit,
) {
    val accent = tabAccentFor(TabAccentRole.Food)
    val summary = meal.mealDiarySummary()
    Surface(
        onClick = onMealClick,
        color = MusFitTheme.colors.surface,
        shape = shape,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExpressiveBadge(
                icon = mealTypeIcon(meal.id, meal.title),
                shape = badgeShape,
                containerColor = accent.container,
                contentColor = accent.onContainerVariant,
                size = 48.dp,
                iconSize = 22.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meal.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MusFitTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildAnnotatedString {
                        append(summary.prefix)
                        if (summary.kcal.isNotEmpty()) {
                            withStyle(
                                SpanStyle(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = accent.onContainer,
                                ),
                            ) {
                                append(summary.kcal)
                            }
                        }
                        append(summary.qualifier)
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                    color = MusFitTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            Surface(
                onClick = onAddClick,
                color = accent.color,
                contentColor = accent.onColor,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(R.string.food_add_to_meal, meal.title),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun FoodGoalMode.label(): String = stringResource(labelResource())

internal fun FoodGoalMode.labelResource(): Int = when (this) {
    FoodGoalMode.Balanced -> R.string.food_goal_mode_balanced
    FoodGoalMode.HighProtein -> R.string.food_goal_mode_high_protein
    FoodGoalMode.KetoLowCarb -> R.string.food_goal_mode_keto_low_carb
    FoodGoalMode.MuscleGain -> R.string.food_goal_mode_muscle_gain
    FoodGoalMode.WeightLoss -> R.string.food_goal_mode_weight_loss
    FoodGoalMode.MediterraneanStyle -> R.string.food_goal_mode_mediterranean
    FoodGoalMode.CleanEating -> R.string.food_goal_mode_clean_eating
    FoodGoalMode.Custom -> R.string.food_goal_mode_custom
}

@Composable
internal fun FoodAddMode.label(): String = stringResource(
    when (this) {
        FoodAddMode.Saved -> R.string.food_add_mode_saved
        FoodAddMode.Manual -> R.string.food_add_mode_manual
        FoodAddMode.Barcode -> R.string.food_add_mode_barcode
        FoodAddMode.Quick -> R.string.food_add_mode_quick
        FoodAddMode.Ai -> R.string.food_add_mode_ai
    },
)

private val MealDetailSortChoices =
    listOf(
        MealDetailSortMode.Logged,
        MealDetailSortMode.Calories,
        MealDetailSortMode.Protein,
        MealDetailSortMode.Name,
    )

@Composable
private fun MealDetailSortMode.label(): String = stringResource(
    when (this) {
        MealDetailSortMode.Logged -> R.string.food_sort_logged
        MealDetailSortMode.Calories -> R.string.food_sort_calories
        MealDetailSortMode.Protein -> R.string.food_sort_protein
        MealDetailSortMode.Name -> R.string.food_sort_name
    },
)
