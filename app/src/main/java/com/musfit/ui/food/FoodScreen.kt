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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BreakfastDining
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import coil.compose.AsyncImage
import com.musfit.data.repository.FoodGoalMode
import com.musfit.ui.AppDestination
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
import com.musfit.ui.theme.MusFitMotion
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import com.musfit.ui.theme.tabAccentFor
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodScreen(
    scannedBarcode: String? = null,
    onScanClick: () -> Unit = {},
    onScannedBarcodeConsumed: () -> Unit = {},
    scannedLabelText: String? = null,
    onLabelScanClick: () -> Unit = {},
    onScannedLabelConsumed: () -> Unit = {},
    viewModel: FoodViewModel = hiltViewModel(),
) {
    val routeState by viewModel.routeState.collectAsState()
    val diaryState by viewModel.diaryState.collectAsState()
    val accent = tabAccentFor(AppDestination.Food)
    val diaryActions = remember(viewModel) {
        FoodDiaryActions(
            onPreviousDay = viewModel::goToPreviousDay,
            onNextDay = viewModel::goToNextDay,
            onToday = viewModel::goToToday,
            onOpenGoal = viewModel::openGoalEditor,
            onOpenMeals = viewModel::openMealSettings,
            onOpenTemplates = viewModel::openMealTemplates,
            onOpenShopping = viewModel::openShoppingList,
            onOpenRecipes = viewModel::openRecipeBrowser,
            onOpenFasting = viewModel::openFastingTimer,
            onOpenHealthConnect = viewModel::openHealthConnectSheet,
            onOpenDatabase = viewModel::openFoodDatabase,
            onCopyDayToTomorrow = viewModel::copySelectedDayToTomorrow,
            onOpenWater = viewModel::openWaterSheet,
            onQuickAddWater = { viewModel.logQuickWater(WATER_QUICK_ADD_MILLILITERS) },
            onQuickRemoveWater = { viewModel.removeQuickWater(WATER_QUICK_ADD_MILLILITERS) },
            onUndoDelete = viewModel::undoDeleteDiaryEntry,
            onOpenMeal = viewModel::openMealDetail,
            onAddFood = viewModel::openAddFood,
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
            onScanClick = onScanClick,
            onLabelScanClick = onLabelScanClick,
            onRequestFoodHealthConnectPermissions = foodHealthConnectPermissionLauncher::launch,
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
@Suppress("LongMethod", "CyclomaticComplexMethod")
private fun FoodActiveSurface(
    viewModel: FoodViewModel,
    onScanClick: () -> Unit,
    onLabelScanClick: () -> Unit,
    onRequestFoodHealthConnectPermissions: (Set<String>) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val selectedMealDetail = state.selectedMealDetailForDisplay()
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
    BackHandler(enabled = selectedMealDetail != null) { viewModel.closeMealDetail() }
    BackHandler(
        enabled = state.isAddPanelVisible && state.sheetMode == FoodSheetMode.AddFood,
    ) { viewModel.closeAddFood() }
    BackHandler(enabled = isRecipeFullScreen) {
        if (state.sheetMode == FoodSheetMode.RecipeEditor) {
            viewModel.openRecipeBrowser()
        } else {
            viewModel.closeAddFood()
        }
    }
    BackHandler(enabled = isSavedFoodEditorFullScreen || isGoalEditorFullScreen) {
        viewModel.closeAddFood()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MusFitTheme.colors.background),
    ) {
        if (isSavedFoodEditorFullScreen) {
            SavedFoodEditorScreen(
                onBack = viewModel::closeAddFood,
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
                onBack = viewModel::closeAddFood,
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
                onCloseClick = viewModel::closeAddFood,
                onForwardClick = { viewModel.openRecipeEditor(null) },
                onHomeClick = viewModel::openRecipeBrowser,
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
                onEditRecipeClick = { recipeId -> viewModel.openRecipeEditor(recipeId) },
                onDuplicateRecipeClick = viewModel::duplicateRecipe,
                onFavoriteClick = viewModel::toggleFavoriteRecipe,
                onSearchQueryChanged = viewModel::onRecipeDiscoveryQueryChanged,
                onDiscoveryFilterChanged = viewModel::selectRecipeDiscoveryFilter,
                onDiscoveryItemClick = viewModel::useRecipeDiscoveryItem,
                onLogRecipeClick = viewModel::logRecipeFromBrowser,
                onPlanRecipeClick = viewModel::planRecipe,
                onReviewIdeaClick = viewModel::useRecipeDiscoveryItem,
                onSaveClick = viewModel::saveRecipe,
                onDeleteClick = { state.recipeEditor?.editingRecipeId?.let(viewModel::deleteRecipe) },
            )
        } else if (state.isAddPanelVisible && state.sheetMode == FoodSheetMode.AddFood) {
            AddFoodScreen(
                state = state,
                onBack = viewModel::closeAddFood,
                onQueryChange = viewModel::onFoodDatabaseQueryChanged,
                onScanClick = onScanClick,
                onTabSelected = viewModel::selectAddTab,
                onFoodClick = viewModel::logSavedFood,
                onModeSelected = viewModel::selectAddMode,
                onMealRetarget = viewModel::onMealTypeChanged,
                onOpenTemplates = viewModel::openMealTemplates,
                onOpenRecipes = viewModel::openRecipeBrowser,
                onKeepAddingChanged = viewModel::onKeepAddingFoodsChanged,
                onLogAllYesterday = viewModel::logSameAsYesterday,
                onQuickTrack = { viewModel.selectAddMode(FoodAddMode.Quick) },
                onAdjustGoals = viewModel::openGoalEditor,
                onCopyYesterday = viewModel::copySelectedMealFromYesterday,
                onSaveTemplate = { viewModel.saveSelectedMealAsTemplate("${state.selectedMealTitle} template") },
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
                onCreateRecipe = { viewModel.openRecipeEditor(null) },
            )
        } else if (selectedMealDetail != null) {
            MealDetailScreen(
                meal = selectedMealDetail,
                sortMode = state.mealDetailSortMode,
                selectedDate = state.selectedDate,
                dayCalorieBudgetKcal = state.effectiveCalorieBudgetKcal,
                message = state.message,
                canUndoDelete = state.lastDeletedDiaryEntry != null,
                onBackClick = viewModel::closeMealDetail,
                onAddFoodClick = viewModel::openAddFoodFromMealDetail,
                onCopyYesterdayClick = viewModel::copySelectedMealFromYesterday,
                onSaveTemplateClick = { viewModel.saveSelectedMealAsTemplate("${selectedMealDetail.title} template") },
                onMealSettingsClick = viewModel::openMealSettings,
                onSortModeChanged = viewModel::onMealDetailSortChanged,
                onEntryClick = viewModel::openDiaryEntryEditor,
                onUndoDeleteClick = viewModel::undoDeleteDiaryEntry,
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
                    viewModel.closeAddFood()
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
                        onClose = viewModel::closeAddFood,
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
                        onNutritionLabelScanClick = viewModel::openNutritionLabelScan,
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
                        onNewFoodClick = viewModel::openNewSavedFoodEditor,
                        onBarcodeCompareClick = viewModel::openBarcodeComparison,
                        onOpenFoodDetailClick = viewModel::openSavedFoodDetail,
                        onEditFoodClick = viewModel::openSavedFoodEditor,
                        onSaveOnlineFoodClick = viewModel::saveOnlineFoodResult,
                        onImportStarterFoodsClick = viewModel::seedStarterFoods,
                        onNutritionLabelScanClick = viewModel::openNutritionLabelScan,
                        onMergeDuplicateFoodsClick = viewModel::mergeDuplicateFoods,
                        onFavoriteClick = viewModel::toggleFavoriteFood,
                        onReportFoodClick = viewModel::reportSavedFoodForReview,
                    )

                FoodSheetMode.FoodDetail ->
                    FoodDetailPanel(
                        state = state,
                        onEditClick = { state.selectedSavedFoodDetail?.id?.let(viewModel::openSavedFoodEditor) },
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
                        onEditRecipeClick = { recipeId -> viewModel.openRecipeEditor(recipeId) },
                        onDuplicateRecipeClick = viewModel::duplicateRecipe,
                        onFavoriteClick = viewModel::toggleFavoriteRecipe,
                        onDiscoveryFilterChanged = viewModel::selectRecipeDiscoveryFilter,
                        onDiscoveryItemClick = viewModel::useRecipeDiscoveryItem,
                        onSaveClick = viewModel::saveRecipe,
                        onDeleteClick = { state.recipeEditor?.editingRecipeId?.let(viewModel::deleteRecipe) },
                    )

                FoodSheetMode.MealTemplates ->
                    MealTemplatesPanel(
                        state = state,
                        onTemplateClick = viewModel::logMealTemplate,
                        onEditClick = viewModel::openMealTemplateEditor,
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
                        onEditClick = viewModel::openMealDefinitionEditor,
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

                FoodSheetMode.Water ->
                    FoodWaterTrackerSheet(viewModel)

                FoodSheetMode.HealthConnect ->
                    FoodHealthConnectTrackerSheet(
                        viewModel = viewModel,
                        onRequestPermissions = onRequestFoodHealthConnectPermissions,
                    )
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
                title = "Food",
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
    val state by viewModel.trackerState.collectAsState()
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
    val state by viewModel.trackerState.collectAsState()
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
    val accent = tabAccentFor(AppDestination.Food)
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
            IconButton(onClick = onPreviousDayClick, modifier = Modifier.size(30.dp)) {
                Icon(
                    Icons.Filled.ChevronLeft,
                    contentDescription = "Previous day",
                    tint = MusFitTheme.colors.onSurface,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = date.format(java.time.format.DateTimeFormatter.ofPattern("EEE · d MMM")),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accent.onContainer,
                maxLines = 1,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onTodayClick)
                    .padding(horizontal = 4.dp, vertical = 6.dp),
            )
            IconButton(onClick = onNextDayClick, modifier = Modifier.size(30.dp)) {
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = "Next day",
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
 * the shape itself marks progress), and a trailing tonal add button. Tapping a
 * cell logs a glass; long actions live in the water sheet behind [onWaterClick].
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
                        text = "Water",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MusFitTheme.colors.onSurface,
                        maxLines = 1,
                    )
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MusFitTheme.colors.onSurface,
                                ),
                            ) {
                                append("%.1f".format(consumedMilliliters / 1000.0))
                            }
                            append(" / %.1f L".format(goalMilliliters / 1000.0))
                        },
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
                                .background(if (filled) waterColor else emptyColor)
                                .clickable(onClick = onQuickAddClick),
                        )
                    }
                }
            }
            Surface(
                onClick = onQuickAddClick,
                color = emptyColor,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Add water",
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

internal val FoodUiState.foodEntryActionVerb: String
    get() = if (isPlanningMode) "Plan" else "Log"

internal val FoodUiState.foodEntryActionProgressLabel: String
    get() = if (isPlanningMode) "Planning" else "Logging"

internal fun FoodUiState.foodEntryActionLabel(target: String): String = "$foodEntryActionVerb $target"

internal val FoodUiState.saveAndFoodEntryActionLabel: String
    get() = if (isPlanningMode) "Save and plan" else "Save and log"

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
        Icon(Icons.Filled.MoreVert, contentDescription = "More actions", tint = MusFitTheme.colors.onSurfaceVariant)
    }
    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
        DropdownMenuItem(text = { Text("Recipes") }, onClick = {
            menuOpen = false
            onRecipesClick()
        })
        DropdownMenuItem(text = { Text("Goals") }, onClick = {
            menuOpen = false
            onGoalClick()
        })
        DropdownMenuItem(text = { Text("Meals") }, onClick = {
            menuOpen = false
            onMealsClick()
        })
        DropdownMenuItem(text = { Text("Templates") }, onClick = {
            menuOpen = false
            onTemplatesClick()
        })
        DropdownMenuItem(text = { Text("Shopping list") }, onClick = {
            menuOpen = false
            onShoppingClick()
        })
        DropdownMenuItem(text = { Text("Fasting") }, onClick = {
            menuOpen = false
            onFastingClick()
        })
        DropdownMenuItem(text = { Text("Health Connect") }, onClick = {
            menuOpen = false
            onHealthConnectClick()
        })
        DropdownMenuItem(text = { Text("Food database") }, onClick = {
            menuOpen = false
            onFoodDatabaseClick()
        })
        DropdownMenuItem(
            text = { Text("Copy day to tomorrow") },
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
                    figure = state.eatenCaloriesKcal.roundToInt().toString(),
                    label = "eaten",
                    accent = accent,
                )
                GaugeFooterStat(
                    figure = state.calorieGoalKcal.roundToInt().toString(),
                    label = "goal",
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
                text = remainingCalories.roundToInt().toString(),
                style = MaterialTheme.typography.displayMedium,
                color = accent.onContainer,
                maxLines = 1,
            )
            Text(
                text = "kcal left",
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
                text = "${macro.currentGrams.roundToInt()}/${macro.goalGrams.roundToInt()}",
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
                        text = "Day rating",
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
                    text = factor.valueLabel,
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
                    text = "Daily habits",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MusFitTheme.colors.brandInk,
                )
                Text(
                    text = "${habits.count { it.status == FoodHabitStatus.Complete }} / ${habits.size}",
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
                    text = habit.status.label,
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
            text = "Insights",
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

private val FoodHabitStatus.label: String
    get() =
        when (this) {
            FoodHabitStatus.Complete -> "Done"
            FoodHabitStatus.InProgress -> "In progress"
            FoodHabitStatus.Missing -> "Not yet"
        }

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
            nutrient.currentValue.roundToInt().toString()
        } else {
            nutrient.currentValue.formatNutritionDisplay()
        }
    val goalText =
        if (nutrient.unit == "mg") {
            nutrient.goalValue.roundToInt().toString()
        } else {
            nutrient.goalValue.formatNutritionDisplay()
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
                    text = " / $goalText ${nutrient.unit}",
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
            nutrient.currentValue.roundToInt().toString()
        } else {
            nutrient.currentValue.formatNutritionDisplay()
        }
    val goalText =
        if (nutrient.unit == "mg") {
            nutrient.goalValue.roundToInt().toString()
        } else {
            nutrient.goalValue.formatNutritionDisplay()
        }
    val separator = if (nutrient.isLimit) "/" else "/"

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
                text = "$currentText$separator$goalText ${nutrient.unit}",
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
            text = "${micronutrient.value.formatMicronutrientDisplay()} ${micronutrient.unit}",
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
                text = "${micronutrient.value.formatMicronutrientDisplay()} ${micronutrient.unit}",
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
    selectedDate: java.time.LocalDate,
    dayCalorieBudgetKcal: Double,
    message: String?,
    canUndoDelete: Boolean,
    onBackClick: () -> Unit,
    onAddFoodClick: () -> Unit,
    onCopyYesterdayClick: () -> Unit,
    onSaveTemplateClick: () -> Unit,
    onMealSettingsClick: () -> Unit,
    onSortModeChanged: (MealDetailSortMode) -> Unit,
    onEntryClick: (String) -> Unit,
    onUndoDeleteClick: () -> Unit,
) {
    val accent = tabAccentFor(AppDestination.Food)
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
            val itemsLabel = if (meal.entries.size == 1) "1 item" else "${meal.entries.size} items"
            InnerScreenHeader(
                title = meal.title,
                onBack = onBackClick,
                subtitle = "${selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("EEE d MMM"))} · $itemsLabel",
                trailing = {
                    Box {
                        var menuOpen by remember { mutableStateOf(false) }
                        TonalHeaderIconButton(
                            icon = Icons.Outlined.MoreHoriz,
                            contentDescription = "Meal actions",
                            onClick = { menuOpen = true },
                        )
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Copy yesterday") },
                                onClick = {
                                    menuOpen = false
                                    onCopyYesterdayClick()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Save as template") },
                                onClick = {
                                    menuOpen = false
                                    onSaveTemplateClick()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Meal settings") },
                                onClick = {
                                    menuOpen = false
                                    onMealSettingsClick()
                                },
                            )
                        }
                    }
                },
            )

            MessageBanner(
                message = message,
                canUndoDelete = canUndoDelete,
                onUndoDeleteClick = onUndoDeleteClick,
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
                            text = "No food logged yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Add food to build this meal.",
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
                            text = choice.label,
                            selected = sortMode == choice,
                            onClick = { onSortModeChanged(choice) },
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    meal.entries.forEachIndexed { index, entry ->
                        val quantityLabel = "${entry.quantityGrams.roundToInt()} g"
                        FoodListItemRow(
                            index = index,
                            count = meal.entries.size,
                            title = entry.name,
                            subtitle = "$quantityLabel · C ${entry.carbsGrams.formatNutritionDisplay()} · " +
                                "P ${entry.proteinGrams.formatNutritionDisplay()} · F ${entry.fatGrams.formatNutritionDisplay()}",
                            onClick = { onEntryClick(entry.id) },
                            imageUrl = entry.imageUrl,
                            fallbackIcon = mealDetailEntryIcon(meal.id, meal.title),
                            trailingTop = "${entry.caloriesKcal.roundToInt()}",
                            trailingSub = if (entry.isPlanned) "Planned" else quantityLabel,
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
                text = "Add food",
                onClick = onAddFoodClick,
                icon = Icons.Filled.Add,
                modifier = Modifier.weight(1f),
            )
            TonalIconSquare(
                icon = Icons.Outlined.ContentCopy,
                contentDescription = "Copy yesterday's ${meal.title}",
                onClick = onCopyYesterdayClick,
                containerColor = accent.container,
                contentColor = accent.onContainer,
            )
            TonalIconSquare(
                icon = Icons.Outlined.Edit,
                contentDescription = "Meal settings",
                onClick = onMealSettingsClick,
            )
        }
    }
}

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
                        text = "${meal.caloriesKcal.roundToInt()}",
                        style = HeroNumberMediumStyle,
                        color = accent.onContainer,
                    )
                    Text(
                        text = "kcal",
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
                                text = rating?.label ?: "More nutrition",
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
                    label = "Protein",
                    grams = meal.proteinGrams,
                    goalGrams = meal.proteinGoalGrams,
                    color = MusFitTheme.colors.macroColors[1],
                    trackColor = glass,
                    accent = accent,
                    modifier = Modifier.weight(1f),
                )
                MealDetailHeroMacro(
                    label = "Fat",
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
                    text = "$percentOfDay% of today's ${dayCalorieBudgetKcal.roundToInt()} kcal goal",
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
                text = "${grams.roundToInt()} g",
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
                MoreNutritionSection(title = "Rating breakdown") {
                    RatingFactorColumn(it)
                }
            }
            if (meal.advancedNutritionProgress.isNotEmpty()) {
                MoreNutritionSection(title = "Nutrients") {
                    AdvancedNutritionProgressColumn(meal.advancedNutritionProgress)
                }
            }
            if (meal.micronutrients.isNotEmpty()) {
                MoreNutritionSection(title = "Micronutrients") {
                    MicronutrientGrid(meal.micronutrients)
                }
            }
        }
    }
}

// Filled badge icon for meal-detail item rows (food badges are always filled;
// entries carry no per-food icon, so the meal's own icon stands in).
private fun mealDetailEntryIcon(id: String, title: String): ImageVector {
    val key = "$id $title".lowercase()
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
    message: String?,
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
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (canUndoDelete) {
                MusFitOutlinedButton(
                    onClick = onUndoDeleteClick,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text("Undo")
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
    val accent = tabAccentFor(AppDestination.Food)
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
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Add to ${meal.title}",
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

internal val FoodGoalMode.label: String
    get() =
        when (this) {
            FoodGoalMode.Balanced -> "Balanced"
            FoodGoalMode.HighProtein -> "High protein"
            FoodGoalMode.KetoLowCarb -> "Keto low carb"
            FoodGoalMode.MuscleGain -> "Muscle gain"
            FoodGoalMode.WeightLoss -> "Weight loss"
            FoodGoalMode.MediterraneanStyle -> "Mediterranean-style"
            FoodGoalMode.CleanEating -> "Clean eating"
            FoodGoalMode.Custom -> "Custom"
        }

internal val FoodAddMode.label: String
    get() =
        when (this) {
            FoodAddMode.Saved -> "Saved"
            FoodAddMode.Manual -> "Manual"
            FoodAddMode.Barcode -> "Barcode"
            FoodAddMode.Quick -> "Quick"
            FoodAddMode.Ai -> "AI"
        }

private val MealDetailSortChoices =
    listOf(
        MealDetailSortMode.Logged,
        MealDetailSortMode.Calories,
        MealDetailSortMode.Protein,
        MealDetailSortMode.Name,
    )

private val MealDetailSortMode.label: String
    get() =
        when (this) {
            MealDetailSortMode.Logged -> "Logged"
            MealDetailSortMode.Calories -> "Calories"
            MealDetailSortMode.Protein -> "Protein"
            MealDetailSortMode.Name -> "Name"
        }
