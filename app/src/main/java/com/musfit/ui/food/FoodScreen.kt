package com.musfit.ui.food

import com.musfit.data.repository.FoodGoalMode
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.DinnerDining
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.LunchDining
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.Color
import com.musfit.ui.theme.MusFitTheme
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.health.connect.client.PermissionController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.musfit.ui.AppDestination
import com.musfit.ui.components.MusFitScreenHeader
import com.musfit.ui.components.MusFitSummaryCard
import com.musfit.ui.theme.TabAccent
import com.musfit.ui.theme.tabAccentFor
import kotlin.math.roundToInt

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
    val state by viewModel.state.collectAsState()
    val accent = tabAccentFor(AppDestination.Food)
    val selectedMealDetail = state.selectedMealDetailForDisplay()
    val isRecipeFullScreen =
        state.isAddPanelVisible &&
            (state.sheetMode == FoodSheetMode.RecipeBrowser || state.sheetMode == FoodSheetMode.RecipeEditor)
    var todaySummaryExpanded by rememberSaveable { mutableStateOf(false) }
    var trendsExpanded by rememberSaveable { mutableStateOf(false) }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MusFitTheme.colors.background),
    ) {
        if (selectedMealDetail != null) {
            MealDetailScreen(
                meal = selectedMealDetail,
                sortMode = state.mealDetailSortMode,
                message = state.message,
                canUndoDelete = state.lastDeletedDiaryEntry != null,
                onBackClick = viewModel::closeMealDetail,
                onAddFoodClick = viewModel::openAddFoodFromMealDetail,
                onCopyYesterdayClick = viewModel::copySelectedMealFromYesterday,
                onSaveTemplateClick = { viewModel.saveSelectedMealAsTemplate("${selectedMealDetail.title} template") },
                onSortModeChanged = viewModel::onMealDetailSortChanged,
                onEntryClick = viewModel::openDiaryEntryEditor,
                onUndoDeleteClick = viewModel::undoDeleteDiaryEntry,
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
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    MusFitScreenHeader(
                        title = "Food",
                        actions = {
                            FoodDateChip(
                                date = state.selectedDate,
                                onPreviousDayClick = viewModel::goToPreviousDay,
                                onNextDayClick = viewModel::goToNextDay,
                                onTodayClick = viewModel::goToToday,
                            )
                            FoodDiaryOverflowAction(
                                state = state,
                                onGoalClick = viewModel::openGoalEditor,
                                onMealsClick = viewModel::openMealSettings,
                                onTemplatesClick = viewModel::openMealTemplates,
                                onShoppingClick = viewModel::openShoppingList,
                                onRecipesClick = viewModel::openRecipeBrowser,
                                onFastingClick = viewModel::openFastingTimer,
                                onHealthConnectClick = viewModel::openHealthConnectSheet,
                                onFoodDatabaseClick = viewModel::openFoodDatabase,
                                onCopyDayToTomorrowClick = viewModel::copySelectedDayToTomorrow,
                            )
                        },
                    )
                    FoodDiarySummaryCard(
                        state = state,
                        accent = accent,
                    )

                    FoodWaterRow(
                        consumedMilliliters = state.waterConsumedMilliliters,
                        goalMilliliters = state.waterGoalMilliliters,
                        onWaterClick = viewModel::openWaterSheet,
                        onQuickAddClick = { viewModel.logQuickWater(WATER_QUICK_ADD_MILLILITERS) },
                    )

                    MessageBanner(
                        message = state.message,
                        canUndoDelete = state.lastDeletedDiaryEntry != null,
                        onUndoDeleteClick = viewModel::undoDeleteDiaryEntry,
                    )

                    SectionTitle("Meal diary")
                    state.mealSections.forEach { meal ->
                        MealSectionCard(
                            meal = meal,
                            // Tapping a meal opens its detail once it has logged items;
                            // an empty meal jumps straight to add-food. The + is always quick-add.
                            onMealClick = {
                                if (meal.entries.isNotEmpty()) {
                                    viewModel.openMealDetail(meal.id)
                                } else {
                                    viewModel.openAddFood(meal.id)
                                }
                            },
                            onAddClick = { viewModel.openAddFood(meal.id) },
                        )
                    }

                    CollapsibleGroup(
                        title = "Today's summary",
                        expanded = todaySummaryExpanded,
                        onToggle = { todaySummaryExpanded = !todaySummaryExpanded },
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            DayRatingCard(state.dayRating)
                            DailyInsightsSection(state.dailyInsights)
                            FoodHabitTrackerSection(state.habitTrackers)
                            AdvancedNutritionProgressRow(state.advancedNutritionProgress)
                            MicronutrientRow(state.micronutrients)
                        }
                    }

                    CollapsibleGroup(
                        title = "Trends",
                        expanded = trendsExpanded,
                        onToggle = { trendsExpanded = !trendsExpanded },
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            WeeklyMusFitScoreCard(state.weeklyScore)
                            FoodProgressStatsCard(state.progressStats)
                        }
                    }
                }
            }
            FloatingActionButton(
                onClick = {
                    defaultAddMealId(state.mealSections, java.time.LocalTime.now().hour)
                        ?.let(viewModel::openAddFood)
                },
                containerColor = MusFitTheme.colors.brand,
                contentColor = MusFitTheme.colors.onAccent,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 96.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add food")
            }
        }

    }

    if (
        state.isAddPanelVisible &&
        !isRecipeFullScreen &&
        (state.sheetMode != FoodSheetMode.AddFood || state.addMode != FoodAddMode.Saved)
    ) {
        ModalBottomSheet(
            onDismissRequest = viewModel::closeAddFood,
            containerColor = MusFitTheme.colors.surface,
        ) {
            when (state.sheetMode ?: FoodSheetMode.AddFood) {
                FoodSheetMode.AddFood ->
                    AddFoodPanel(
                        state = state,
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

                FoodSheetMode.SavedFoodEditor ->
                    SavedFoodEditorPanel(
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

                FoodSheetMode.GoalEditor ->
                    GoalEditorPanel(
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
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        WaterTrackerCard(
                            state = state,
                            onQuickWaterClick = viewModel::logQuickWater,
                            onCustomAmountChanged = viewModel::onWaterCustomAmountChanged,
                            onCustomAddClick = viewModel::logCustomWater,
                            onGoalChanged = viewModel::onWaterGoalChanged,
                            onGoalSaveClick = viewModel::saveWaterGoal,
                        )
                    }

                FoodSheetMode.HealthConnect ->
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        FoodHealthConnectSyncCard(
                            state = state,
                            onEnabledChanged = viewModel::onFoodHealthConnectSyncEnabledChanged,
                            onRequestPermissionsClick = {
                                if (state.foodHealthConnectCanRequestPermissions) {
                                    foodHealthConnectPermissionLauncher.launch(
                                        state.foodHealthConnectRequestablePermissions,
                                    )
                                }
                            },
                            onRefreshClick = viewModel::refreshFoodHealthConnectSync,
                            onSyncClick = viewModel::syncFoodToHealthConnect,
                        )
                    }
            }
        }
    }
}

// One 250 ml glass — the amount the water row's quick-add "+" logs, matching the
// water sheet's smallest quick preset.
private const val WATER_QUICK_ADD_MILLILITERS = 250.0

/**
 * Compact date pill for the tab header: ‹ chevron, the selected date (tap to jump
 * back to today), › chevron. Replaces the in-card date navigation.
 */
@Composable
private fun FoodDateChip(
    date: java.time.LocalDate,
    onPreviousDayClick: () -> Unit,
    onNextDayClick: () -> Unit,
    onTodayClick: () -> Unit,
) {
    Surface(
        color = MusFitTheme.colors.surface,
        shape = CircleShape,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            IconButton(onClick = onPreviousDayClick, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.ChevronLeft,
                    contentDescription = "Previous day",
                    tint = MusFitTheme.colors.onSurface,
                )
            }
            Text(
                text = date.format(java.time.format.DateTimeFormatter.ofPattern("EEE · d MMM")),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MusFitTheme.colors.onSurface,
                maxLines = 1,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onTodayClick)
                    .padding(horizontal = 4.dp, vertical = 6.dp),
            )
            IconButton(onClick = onNextDayClick, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = "Next day",
                    tint = MusFitTheme.colors.onSurface,
                )
            }
        }
    }
}

/**
 * At-a-glance water row: drop icon + label + "x of y L", a cup-dot progress gauge,
 * and a quick-add "+". Tapping the row opens the full water sheet.
 */
@Composable
private fun FoodWaterRow(
    consumedMilliliters: Double,
    goalMilliliters: Double,
    onWaterClick: () -> Unit,
    onQuickAddClick: () -> Unit,
) {
    val waterColor = MusFitTheme.colors.water
    // Each segment is one 250 ml glass (so the "+" fills exactly one), capped for width.
    val segmentCount = (goalMilliliters / WATER_QUICK_ADD_MILLILITERS).roundToInt().coerceIn(1, 8)
    val filledSegments = (consumedMilliliters / WATER_QUICK_ADD_MILLILITERS).roundToInt().coerceIn(0, segmentCount)
    Surface(
        onClick = onWaterClick,
        color = MusFitTheme.colors.surface,
        shape = MusFitTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Outlined.WaterDrop,
                contentDescription = null,
                tint = waterColor,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = "Water",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MusFitTheme.colors.onSurface,
                maxLines = 1,
            )
            Text(
                text = formatWaterLiters(consumedMilliliters, goalMilliliters),
                style = MaterialTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurfaceVariant,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(segmentCount) { index ->
                    Box(
                        modifier = Modifier
                            .size(width = 6.dp, height = 18.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(if (index < filledSegments) waterColor else waterColor.copy(alpha = 0.22f)),
                    )
                }
            }
            IconButton(onClick = onQuickAddClick) {
                Icon(Icons.Filled.Add, contentDescription = "Add water", tint = waterColor)
            }
        }
    }
}

private fun formatWaterLiters(consumedMilliliters: Double, goalMilliliters: Double): String =
    "%.1f of %.1f L".format(consumedMilliliters / 1000.0, goalMilliliters / 1000.0)

@Composable
private fun CollapsibleGroup(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            onClick = onToggle,
            color = MusFitTheme.colors.surface,
            shape = MusFitTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MusFitTheme.colors.onSurface,
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MusFitTheme.colors.onSurfaceVariant,
                )
            }
        }
        if (expanded) {
            content()
        }
    }
}

internal val FoodUiState.foodEntryActionVerb: String
    get() = if (isPlanningMode) "Plan" else "Log"

internal val FoodUiState.foodEntryActionProgressLabel: String
    get() = if (isPlanningMode) "Planning" else "Logging"

internal fun FoodUiState.foodEntryActionLabel(target: String): String =
    "$foodEntryActionVerb $target"

internal val FoodUiState.saveAndFoodEntryActionLabel: String
    get() = if (isPlanningMode) "Save and plan" else "Save and log"

@Composable
private fun FoodDiaryOverflowAction(
    state: FoodUiState,
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
        DropdownMenuItem(text = { Text("Recipes") }, onClick = { menuOpen = false; onRecipesClick() })
        DropdownMenuItem(text = { Text("Goals") }, onClick = { menuOpen = false; onGoalClick() })
        DropdownMenuItem(text = { Text("Meals") }, onClick = { menuOpen = false; onMealsClick() })
        DropdownMenuItem(text = { Text("Templates") }, onClick = { menuOpen = false; onTemplatesClick() })
        DropdownMenuItem(text = { Text("Shopping list") }, onClick = { menuOpen = false; onShoppingClick() })
        DropdownMenuItem(text = { Text("Fasting") }, onClick = { menuOpen = false; onFastingClick() })
        DropdownMenuItem(text = { Text("Health Connect") }, onClick = { menuOpen = false; onHealthConnectClick() })
        DropdownMenuItem(text = { Text("Food database") }, onClick = { menuOpen = false; onFoodDatabaseClick() })
        DropdownMenuItem(
            text = { Text("Copy day to tomorrow") },
            enabled = !state.isSaving,
            onClick = { menuOpen = false; onCopyDayToTomorrowClick() },
        )
    }
}

@Composable
private fun FoodDiarySummaryCard(
    state: FoodUiState,
    accent: TabAccent,
) {
    MusFitSummaryCard(accent = accent) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CalorieRing(
                    eatenCalories = state.eatenCaloriesKcal,
                    remainingCalories = state.remainingCaloriesKcal,
                    calorieGoal = state.effectiveCalorieBudgetKcal,
                    contentColor = accent.onContainer,
                )
                Column(modifier = Modifier.weight(1f)) {
                    SummaryMetricRow(label = "Eaten", value = state.eatenCaloriesKcal, color = accent.onContainer)
                    SummaryMetricDivider(color = accent.onContainer)
                    SummaryMetricRow(label = "Burned", value = state.burnedCaloriesKcal, color = accent.onContainer)
                    SummaryMetricDivider(color = accent.onContainer)
                    SummaryMetricRow(label = "Goal", value = state.calorieGoalKcal, color = accent.onContainer)
                }
            }
            if (state.macroProgress.isNotEmpty()) {
                HeroMacroStrip(macros = state.macroProgress, contentColor = accent.onContainer)
            }
        }
    }
}

@Composable
private fun HeroMacroStrip(
    macros: List<FoodMacroProgressUiState>,
    contentColor: Color,
) {
    val macroColors = MusFitTheme.colors.macroColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        macros.forEachIndexed { index, macro ->
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = macro.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                        maxLines = 1,
                    )
                    Text(
                        text = "${macro.currentGrams.roundToInt()}/${macro.goalGrams.roundToInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.7f),
                        maxLines = 1,
                    )
                }
                ProgressBar(
                    progress = (macro.currentGrams / macro.goalGrams).toFloat().coerceIn(0f, 1f),
                    color = macroColors[index % macroColors.size],
                )
            }
        }
    }
}

@Composable
private fun SummaryMetricRow(
    label: String,
    value: Double,
    color: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = color.copy(alpha = 0.8f),
            maxLines = 1,
        )
        Text(
            text = value.roundToInt().toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
private fun SummaryMetricDivider(color: Color) {
    HorizontalDivider(thickness = 1.dp, color = color.copy(alpha = 0.15f))
}

@Composable
private fun CalorieRing(
    eatenCalories: Double,
    remainingCalories: Double,
    calorieGoal: Double,
    contentColor: Color,
) {
    val progress = (eatenCalories / calorieGoal).toFloat().coerceIn(0f, 1f)
    val trackColor = MusFitTheme.colors.onSurface.copy(alpha = 0.12f)
    val progressColor = MusFitTheme.colors.brand

    Box(
        modifier = Modifier.size(150.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
            drawArc(
                color = trackColor,
                startAngle = 145f,
                sweepAngle = 250f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                color = progressColor,
                startAngle = 145f,
                sweepAngle = 250f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = remainingCalories.roundToInt().toString(),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
            )
            Text(
                text = "kcal left",
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.78f),
            )
        }
    }
}

@Composable
internal fun MacroProgressRow(macros: List<FoodMacroProgressUiState>) {
    val macroColors = MusFitTheme.colors.macroColors
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MusFitTheme.colors.surface),
        shape = MusFitTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = macro.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                text = "${macro.currentGrams.roundToInt()}/${macro.goalGrams.roundToInt()}",
                style = MaterialTheme.typography.bodySmall,
                color = MusFitTheme.colors.onSurfaceVariant,
                maxLines = 1,
            )
        }
        ProgressBar(
            progress = (macro.currentGrams / macro.goalGrams).toFloat().coerceIn(0f, 1f),
            color = color,
        )
    }
}

@Composable
private fun WeeklyMusFitScoreCard(score: FoodWeeklyScoreUiState) {
    val accent = score.tone.ratingColor()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = accent.copy(alpha = 0.10f).compositeOver(MusFitTheme.colors.surface),
        shape = MusFitTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = score.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MusFitTheme.colors.brandInk,
                    )
                    Text(
                        text = score.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = score.score.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        maxLines = 1,
                    )
                }
            }
            ProgressBar(progress = score.score / 100f, color = accent)
            Text(
                text = score.suggestion,
                style = MaterialTheme.typography.bodySmall,
                color = accent,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (score.factors.isNotEmpty()) {
                HorizontalDivider(color = MusFitTheme.colors.outline)
                RatingFactorColumn(score.factors)
            }
        }
    }
}

@Composable
private fun FoodProgressStatsCard(stats: FoodProgressStatsUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MusFitTheme.colors.surface,
        shape = MusFitTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Progress stats",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MusFitTheme.colors.brandInk,
            )
            FoodProgressPeriodRow(stats.weekly)
            HorizontalDivider(color = MusFitTheme.colors.outline)
            FoodProgressPeriodRow(stats.monthly)
        }
    }
}

@Composable
private fun FoodProgressPeriodRow(period: FoodProgressPeriodUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(period.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                period.trackedDaysLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MusFitTheme.colors.brand,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // Scannable caption/value grid instead of a run-on sentence of metrics.
        period.metrics.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                pair.forEach { metric ->
                    FoodProgressMetricCell(metric = metric, modifier = Modifier.weight(1f))
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        Text(
            text = period.trendLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MusFitTheme.colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FoodProgressMetricCell(
    metric: FoodProgressMetricUiState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            text = metric.caption,
            style = MaterialTheme.typography.labelSmall,
            color = MusFitTheme.colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = metric.value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MusFitTheme.colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
private fun RatingFactorColumn(factors: List<FoodRatingFactorUiState>) {
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
private fun FoodInsightTone.ratingColor(): Color =
    when (this) {
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

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        nutrients.chunked(2).forEach { rowNutrients ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowNutrients.forEach { nutrient ->
                    AdvancedNutritionProgressCard(
                        nutrient = nutrient,
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

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        micronutrients.chunked(2).forEach { rowNutrients ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowNutrients.forEach { micronutrient ->
                    MicronutrientCard(
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

@Composable
private fun MealDetailScreen(
    meal: FoodMealSectionUiState,
    sortMode: MealDetailSortMode,
    message: String?,
    canUndoDelete: Boolean,
    onBackClick: () -> Unit,
    onAddFoodClick: () -> Unit,
    onCopyYesterdayClick: () -> Unit,
    onSaveTemplateClick: () -> Unit,
    onSortModeChanged: (MealDetailSortMode) -> Unit,
    onEntryClick: (String) -> Unit,
    onUndoDeleteClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 18.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MusFitOutlinedButton(
                onClick = onBackClick,
                modifier = Modifier.size(48.dp),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text("<", style = MaterialTheme.typography.titleLarge)
            }
            Text(
                text = meal.title.uppercase(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            var menuOpen by remember { mutableStateOf(false) }
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "Meal actions",
                    tint = MusFitTheme.colors.onSurfaceVariant,
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Copy yesterday") },
                    onClick = { menuOpen = false; onCopyYesterdayClick() },
                )
                DropdownMenuItem(
                    text = { Text("Save template") },
                    onClick = { menuOpen = false; onSaveTemplateClick() },
                )
            }
        }

        MessageBanner(
            message = message,
            canUndoDelete = canUndoDelete,
            onUndoDeleteClick = onUndoDeleteClick,
        )

        Surface(
            onClick = onAddFoodClick,
            color = MusFitTheme.colors.surface,
            shape = MusFitTheme.shapes.extraLarge,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Food, meal or brand",
                    style = MaterialTheme.typography.titleMedium,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "+",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MusFitTheme.colors.brand,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }

        MealDetailMacroCard(meal)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle("Logged items")
            if (meal.entries.isNotEmpty()) {
                MealDetailSortMenu(
                    selectedSortMode = sortMode,
                    onSortModeChanged = onSortModeChanged,
                )
            }
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MusFitTheme.colors.surface),
                shape = MusFitTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    meal.entries.forEachIndexed { index, entry ->
                        if (index > 0) {
                            HorizontalDivider(color = MusFitTheme.colors.outline)
                        }
                        DiaryEntryRow(
                            entry = entry,
                            onClick = { onEntryClick(entry.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MealDetailSortMenu(
    selectedSortMode: MealDetailSortMode,
    onSortModeChanged: (MealDetailSortMode) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { open = true },
            color = Color.Transparent,
            shape = MusFitTheme.shapes.extraLarge,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selectedSortMode.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = "Sort logged items",
                    tint = MusFitTheme.colors.onSurfaceVariant,
                )
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            MealDetailSortChoices.forEach { choice ->
                DropdownMenuItem(
                    text = { Text(choice.label) },
                    onClick = { open = false; onSortModeChanged(choice) },
                )
            }
        }
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
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
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
                color = MaterialTheme.colorScheme.onSurface,
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

@Composable
private fun MealDetailMacroCard(meal: FoodMealSectionUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MusFitTheme.colors.surface),
        shape = MusFitTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "${meal.caloriesKcal.roundToInt()}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MusFitTheme.colors.brandInk,
                    )
                    Text(
                        text = "/ ${meal.calorieTargetKcal.roundToInt()} kcal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MusFitTheme.colors.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                meal.rating?.let { rating -> RatingPill(rating) }
            }

            ProgressBar(
                progress = meal.calorieProgress.toFloat(),
                color = MusFitTheme.colors.brand,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MealMacroMetric(
                    label = meal.carbsLabel,
                    grams = meal.effectiveCarbsGrams,
                    goalGrams = meal.carbsGoalGrams,
                    color = MusFitTheme.colors.macroColors[0],
                    modifier = Modifier.weight(1f),
                )
                MealMacroMetric(
                    label = "Protein",
                    grams = meal.proteinGrams,
                    goalGrams = meal.proteinGoalGrams,
                    color = MusFitTheme.colors.macroColors[1],
                    modifier = Modifier.weight(1f),
                )
                MealMacroMetric(
                    label = "Fat",
                    grams = meal.fatGrams,
                    goalGrams = meal.fatGoalGrams,
                    color = MusFitTheme.colors.macroColors[2],
                    modifier = Modifier.weight(1f),
                )
            }

            val ratingFactors = meal.rating?.factors?.takeIf { it.isNotEmpty() }
            val hasDetail = ratingFactors != null ||
                meal.advancedNutritionProgress.isNotEmpty() ||
                meal.micronutrients.isNotEmpty()
            if (hasDetail) {
                var detailExpanded by rememberSaveable(meal.id) { mutableStateOf(false) }
                HorizontalDivider(color = MusFitTheme.colors.outline)
                Surface(
                    onClick = { detailExpanded = !detailExpanded },
                    color = Color.Transparent,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "More nutrition",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Icon(
                            imageVector = if (detailExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (detailExpanded) "Collapse nutrition detail" else "Expand nutrition detail",
                            tint = MusFitTheme.colors.onSurfaceVariant,
                        )
                    }
                }
                if (detailExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        ratingFactors?.let { RatingFactorColumn(it) }
                        if (meal.advancedNutritionProgress.isNotEmpty()) {
                            AdvancedNutritionProgressColumn(meal.advancedNutritionProgress)
                        }
                        if (meal.micronutrients.isNotEmpty()) {
                            MicronutrientGrid(meal.micronutrients)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MealDetailNutritionLine(
    firstLabel: String,
    firstValue: String,
    secondLabel: String,
    secondValue: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MealDetailNutritionValue(
            label = firstLabel,
            value = firstValue,
            modifier = Modifier.weight(1f),
        )
        MealDetailNutritionValue(
            label = secondLabel,
            value = secondValue,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MealDetailNutritionValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MusFitTheme.colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MusFitTheme.colors.brandInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MealMacroMetric(
    label: String,
    grams: Double,
    goalGrams: Double,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        ProgressBar(
            progress = (grams / goalGrams).toFloat().coerceIn(0f, 1f),
            color = color,
        )
        Text(
            text = "${grams.roundToInt()} g / ${goalGrams.roundToInt()} g",
            style = MaterialTheme.typography.bodySmall,
            color = MusFitTheme.colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MealSectionCard(
    meal: FoodMealSectionUiState,
    onMealClick: () -> Unit,
    onAddClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MusFitTheme.colors.surface),
        shape = MusFitTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = onMealClick,
                    color = Color.Transparent,
                    shape = MusFitTheme.shapes.extraLarge,
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        modifier = Modifier.padding(end = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(MusFitTheme.shapes.medium)
                                .background(MusFitTheme.colors.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = mealTypeIcon(meal.id, meal.title),
                                contentDescription = null,
                                tint = MusFitTheme.colors.brand,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = meal.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = meal.compactDiarySummaryLabel(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MusFitTheme.colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Surface(
                    onClick = onAddClick,
                    color = MusFitTheme.colors.brand.copy(alpha = 0.12f),
                    shape = CircleShape,
                    modifier = Modifier.size(38.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Add to ${meal.title}",
                            tint = MusFitTheme.colors.brand,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }

            if (meal.entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MusFitTheme.colors.outline)
                Text(
                    text = meal.entries.compactDiaryEntriesLabel(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onMealClick)
                        .padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun DiaryEntryRow(
    entry: FoodMealEntryUiState,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = MusFitTheme.shapes.extraLarge,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FoodThumb(
                imageUrl = entry.imageUrl,
                fallback = Icons.Outlined.Restaurant,
                modifier = Modifier.padding(end = 12.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = entry.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    entry.rating?.let { rating ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(rating.tone.ratingColor()),
                        )
                    }
                    if (entry.isPlanned) {
                        Text(
                            text = "Planned",
                            style = MaterialTheme.typography.labelSmall,
                            color = MusFitTheme.colors.brand,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Text(
                    text = "${entry.quantityGrams.roundToInt()} g · P ${entry.proteinGrams.formatNutritionDisplay()}  C ${entry.carbsGrams.formatNutritionDisplay()}  F ${entry.fatGrams.formatNutritionDisplay()} g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(
                text = "${entry.caloriesKcal.roundToInt()} kcal",
                style = MaterialTheme.typography.bodyMedium,
                color = MusFitTheme.colors.brand,
                fontWeight = FontWeight.SemiBold,
            )
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

