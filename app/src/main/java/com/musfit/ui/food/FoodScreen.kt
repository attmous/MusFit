package com.musfit.ui.food

import com.musfit.data.repository.FoodGoalMode
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.musfit.ui.theme.MusFitTheme
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.health.connect.client.PermissionController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
    val selectedMealDetail = state.selectedMealDetailForDisplay()
    var moreExpanded by rememberSaveable { mutableStateOf(false) }
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
                FoodSummaryHeader(
                    state = state,
                    onPreviousDayClick = viewModel::goToPreviousDay,
                    onNextDayClick = viewModel::goToNextDay,
                    onTodayClick = viewModel::goToToday,
                    onQuickAddClick = {
                        viewModel.openAddFood("snacks")
                        viewModel.selectAddMode(FoodAddMode.Quick)
                    },
                    onGoalClick = viewModel::openGoalEditor,
                    onTemplatesClick = viewModel::openMealTemplates,
                    onRecipeClick = viewModel::openRecipeEditor,
                    onMealsClick = viewModel::openMealSettings,
                    onShoppingClick = viewModel::openShoppingList,
                    onPlanningModeClick = viewModel::togglePlanningMode,
                    onCopyDayToTomorrowClick = viewModel::copySelectedDayToTomorrow,
                )

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    MacroProgressRow(state.macroProgress)

                    MessageBanner(
                        message = state.message,
                        canUndoDelete = state.lastDeletedDiaryEntry != null,
                        onUndoDeleteClick = viewModel::undoDeleteDiaryEntry,
                    )

                    SectionTitle("Meal diary")
                    if (state.isFoodDiaryEmpty) {
                        EmptyDiaryStartCard(
                            actions = state.emptyDiaryActions,
                            onActionClick = { actionType ->
                                when (actionType) {
                                    EmptyDiaryActionType.Breakfast -> viewModel.openAddFood("breakfast")
                                    EmptyDiaryActionType.Barcode -> {
                                        viewModel.openAddFood("snacks")
                                        viewModel.selectAddTab(AddTab.Create)
                                        onScanClick()
                                    }
                                    EmptyDiaryActionType.Ai -> {
                                        viewModel.openAddFood("snacks")
                                        viewModel.selectAddMode(FoodAddMode.Ai)
                                    }
                                }
                            },
                        )
                    }
                    state.mealSections.forEach { meal ->
                        MealSectionCard(
                            meal = meal,
                            onMealClick = { viewModel.openAddFood(meal.id) },
                            onAddClick = { viewModel.openAddFood(meal.id) },
                            onEntryClick = viewModel::openDiaryEntryEditor,
                        )
                    }

                    MoreSection(expanded = moreExpanded, onToggle = { moreExpanded = !moreExpanded }) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            DayRatingCard(state.dayRating)
                            DailyInsightsSection(state.dailyInsights)
                            WeeklyPlanStrip(state.weeklyPlan)
                            AdvancedNutritionProgressRow(state.advancedNutritionProgress)
                            MicronutrientRow(state.micronutrients)
                            WaterTrackerCard(
                                state = state,
                                onQuickWaterClick = viewModel::logQuickWater,
                                onCustomAmountChanged = viewModel::onWaterCustomAmountChanged,
                                onCustomAddClick = viewModel::logCustomWater,
                                onGoalChanged = viewModel::onWaterGoalChanged,
                                onGoalSaveClick = viewModel::saveWaterGoal,
                            )
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
                            FoodDatabasePreview(
                                savedFoods = state.savedFoods,
                                onOpenClick = viewModel::openFoodDatabase,
                            )
                        }
                    }
                }
            }
        }

        if (selectedMealDetail == null && !state.isAddPanelVisible) {
            var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

            if (fabMenuExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { fabMenuExpanded = false },
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (fabMenuExpanded) {
                    state.mealSections.forEach { meal ->
                        MealPickerItem(
                            meal = meal,
                            onClick = {
                                fabMenuExpanded = false
                                viewModel.openAddFood(meal.id)
                            },
                        )
                    }
                }
                FloatingActionButton(
                    onClick = { fabMenuExpanded = !fabMenuExpanded },
                    containerColor = MusFitTheme.colors.accent,
                    contentColor = MusFitTheme.colors.onAccent,
                ) {
                    Icon(
                        imageVector = if (fabMenuExpanded) Icons.Filled.Close else Icons.Filled.Add,
                        contentDescription = if (fabMenuExpanded) "Close meal picker" else "Add food",
                    )
                }
            }
        }
    }

    if (state.isAddPanelVisible && (state.sheetMode != FoodSheetMode.AddFood || state.addMode != FoodAddMode.Saved)) {
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
                        onOpenFoodDetailClick = viewModel::openSavedFoodDetail,
                        onEditFoodClick = viewModel::openSavedFoodEditor,
                        onSaveOnlineFoodClick = viewModel::saveOnlineFoodResult,
                        onImportStarterFoodsClick = viewModel::seedStarterFoods,
                        onNutritionLabelScanClick = viewModel::openNutritionLabelScan,
                        onMergeDuplicateFoodsClick = viewModel::mergeDuplicateFoods,
                        onFavoriteClick = viewModel::toggleFavoriteFood,
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
                        onSaveClick = viewModel::saveFoodGoal,
                    )

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
                        onSaveClick = viewModel::saveRecipe,
                        onDeleteClick = { state.editingRecipeId?.let(viewModel::deleteRecipe) },
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
            }
        }
    }
}

@Composable
private fun MoreSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            onClick = onToggle,
            color = MusFitTheme.colors.surface,
            shape = MusFitTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "More details",
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

@Composable
private fun FoodSummaryHeader(
    state: FoodUiState,
    onPreviousDayClick: () -> Unit,
    onNextDayClick: () -> Unit,
    onTodayClick: () -> Unit,
    onQuickAddClick: () -> Unit,
    onGoalClick: () -> Unit,
    onTemplatesClick: () -> Unit,
    onRecipeClick: () -> Unit,
    onMealsClick: () -> Unit,
    onShoppingClick: () -> Unit,
    onPlanningModeClick: () -> Unit,
    onCopyDayToTomorrowClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(MusFitTheme.colors.brandGradient))
            .padding(start = 20.dp, top = 18.dp, end = 8.dp, bottom = 24.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Food",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MusFitTheme.colors.brandInk,
                )
                Box {
                    var menuOpen by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More actions", tint = MusFitTheme.colors.brandInk)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Goals") }, onClick = { menuOpen = false; onGoalClick() })
                        DropdownMenuItem(text = { Text("Meals") }, onClick = { menuOpen = false; onMealsClick() })
                        DropdownMenuItem(text = { Text("Templates") }, onClick = { menuOpen = false; onTemplatesClick() })
                        DropdownMenuItem(text = { Text("Recipes") }, onClick = { menuOpen = false; onRecipeClick() })
                        DropdownMenuItem(text = { Text("Shopping list") }, onClick = { menuOpen = false; onShoppingClick() })
                        DropdownMenuItem(
                            text = { Text(if (state.isPlanningMode) "Planning: on" else "Planning: off") },
                            onClick = { menuOpen = false; onPlanningModeClick() },
                        )
                        DropdownMenuItem(
                            text = { Text("Copy day to tomorrow") },
                            enabled = !state.isSaving,
                            onClick = { menuOpen = false; onCopyDayToTomorrowClick() },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPreviousDayClick) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous day", tint = MusFitTheme.colors.brandInk)
                }
                Text(
                    text = state.selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("EEE · d MMM")),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MusFitTheme.colors.brandInk,
                    modifier = Modifier.clickable(onClick = onTodayClick),
                )
                IconButton(onClick = onNextDayClick) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Next day", tint = MusFitTheme.colors.brandInk)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SummarySideMetric(label = "Eaten", value = state.eatenCaloriesKcal)
                CalorieRing(
                    eatenCalories = state.eatenCaloriesKcal,
                    remainingCalories = state.remainingCaloriesKcal,
                    calorieGoal = state.calorieGoalKcal,
                )
                SummarySideMetric(label = "Goal", value = state.calorieGoalKcal)
            }
        }
    }
}

@Composable
private fun SummarySideMetric(
    label: String,
    value: Double,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MusFitTheme.colors.brandInk.copy(alpha = 0.76f),
        )
        Text(
            text = value.roundToInt().toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = MusFitTheme.colors.brandInk,
        )
    }
}

@Composable
private fun WeeklyPlanStrip(planDays: List<FoodPlanDayUiState>) {
    if (planDays.isEmpty()) {
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        planDays.forEach { day ->
            Card(
                modifier = Modifier.width(112.dp),
                colors = CardDefaults.cardColors(containerColor = MusFitTheme.colors.surface),
                shape = MusFitTheme.shapes.small,
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(day.dayLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "${day.loggedCaloriesKcal.roundToInt()} logged",
                        style = MaterialTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Text(
                        "${day.plannedCaloriesKcal.roundToInt()} planned",
                        style = MaterialTheme.typography.bodySmall,
                        color = MusFitTheme.colors.brand,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun CalorieRing(
    eatenCalories: Double,
    remainingCalories: Double,
    calorieGoal: Double,
) {
    val progress = (eatenCalories / calorieGoal).toFloat().coerceIn(0f, 1f)
    val brandInk = MusFitTheme.colors.brandInk

    Box(
        modifier = Modifier.size(176.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 16.dp.toPx()
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
            drawArc(
                color = brandInk.copy(alpha = 0.18f),
                startAngle = 145f,
                sweepAngle = 250f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                color = brandInk.copy(alpha = 0.62f),
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
                text = "Remaining",
                style = MaterialTheme.typography.bodyMedium,
                color = MusFitTheme.colors.brandInk,
            )
            Text(
                text = remainingCalories.roundToInt().toString(),
                style = MaterialTheme.typography.displayMedium,
                color = MusFitTheme.colors.brandInk,
            )
            Text(
                text = "Goal ${calorieGoal.roundToInt()} kcal",
                style = MaterialTheme.typography.labelLarge,
                color = MusFitTheme.colors.brandInk.copy(alpha = 0.78f),
            )
        }
    }
}

@Composable
internal fun MacroProgressRow(macros: List<FoodMacroProgressUiState>) {
    val macroColors = MusFitTheme.colors.macroColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        macros.forEachIndexed { index, macro ->
            MacroProgressCard(
                macro = macro,
                color = macroColors[index % macroColors.size],
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MacroProgressCard(
    macro: FoodMacroProgressUiState,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.height(98.dp),
        colors = CardDefaults.cardColors(containerColor = MusFitTheme.colors.surface),
        shape = MusFitTheme.shapes.small,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = macro.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${macro.currentGrams.roundToInt()}/${macro.goalGrams.roundToInt()}g",
                style = MaterialTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurfaceVariant,
                maxLines = 1,
            )
            ProgressBar(
                progress = (macro.currentGrams / macro.goalGrams).toFloat().coerceIn(0f, 1f),
                color = color,
            )
        }
    }
}

@Composable
private fun EmptyDiaryStartCard(
    actions: List<EmptyDiaryActionUiState>,
    onActionClick: (EmptyDiaryActionType) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MusFitTheme.colors.surface,
        shape = MusFitTheme.shapes.small,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Build today's food",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MusFitTheme.colors.brandInk,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                actions.forEach { action ->
                    OutlinedButton(
                        onClick = { onActionClick(action.type) },
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = action.accessibilityLabel },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = action.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayRatingCard(rating: FoodRatingUiState) {
    val accent = rating.tone.ratingColor()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MusFitTheme.colors.surface,
        shape = MusFitTheme.shapes.small,
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
        }
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
        shape = MusFitTheme.shapes.small,
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
        shape = MusFitTheme.shapes.small,
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
        shape = MusFitTheme.shapes.small,
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
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
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
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onCopyYesterdayClick) {
                Text("Copy yesterday")
            }
            OutlinedButton(onClick = onSaveTemplateClick) {
                Text("Save template")
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
            shape = MusFitTheme.shapes.small,
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

        SectionTitle("Logged items")
        if (meal.entries.isNotEmpty()) {
            MealDetailSortChips(
                selectedSortMode = sortMode,
                onSortModeChanged = onSortModeChanged,
            )
        }
        if (meal.entries.isEmpty()) {
            Surface(
                color = MusFitTheme.colors.surface,
                shape = MusFitTheme.shapes.small,
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
                shape = MusFitTheme.shapes.small,
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    meal.entries.forEachIndexed { index, entry ->
                        if (index > 0) {
                            HorizontalDivider(color = MusFitTheme.colors.outline)
                        }
                        DiaryEntryRow(
                            entry = entry,
                            showContributions = true,
                            onClick = { onEntryClick(entry.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MealDetailSortChips(
    selectedSortMode: MealDetailSortMode,
    onSortModeChanged: (MealDetailSortMode) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MealDetailSortChoices.forEach { choice ->
            FilterChip(
                selected = selectedSortMode == choice,
                onClick = { onSortModeChanged(choice) },
                label = { Text(choice.label) },
            )
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
        shape = MusFitTheme.shapes.small,
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
                OutlinedButton(
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
        shape = MusFitTheme.shapes.small,
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
                Text(
                    text = "Meal intake",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    meal.rating?.let { rating -> RatingPill(rating) }
                    Text(
                        text = "${meal.caloriesKcal.roundToInt()} kcal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MusFitTheme.colors.brandInk,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ProgressBar(
                    progress = meal.calorieProgress.toFloat(),
                    color = MusFitTheme.colors.brand,
                )
                Text(
                    text = "Target ${meal.calorieTargetKcal.roundToInt()} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }

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

            if (meal.advancedNutritionProgress.isNotEmpty()) {
                HorizontalDivider(color = MusFitTheme.colors.outline)
                AdvancedNutritionProgressColumn(meal.advancedNutritionProgress)
            }
            if (meal.micronutrients.isNotEmpty()) {
                HorizontalDivider(color = MusFitTheme.colors.outline)
                MicronutrientGrid(meal.micronutrients)
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
private fun MealPickerItem(
    meal: FoodMealSectionUiState,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            color = MusFitTheme.colors.surface,
            shape = MusFitTheme.shapes.small,
            shadowElevation = 3.dp,
        ) {
            Text(
                text = meal.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MusFitTheme.colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = MusFitTheme.colors.surface,
            shadowElevation = 3.dp,
            modifier = Modifier.size(48.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = mealTypeIcon(meal.id, meal.title),
                    contentDescription = "Add to ${meal.title}",
                    tint = MusFitTheme.colors.brand,
                )
            }
        }
    }
}

@Composable
private fun MealSectionCard(
    meal: FoodMealSectionUiState,
    onMealClick: () -> Unit,
    onAddClick: () -> Unit,
    onEntryClick: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MusFitTheme.colors.surface),
        shape = MusFitTheme.shapes.small,
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
                    shape = MusFitTheme.shapes.small,
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
                                text = meal.summaryLabel(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MusFitTheme.colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            meal.rating?.let { rating ->
                                Spacer(modifier = Modifier.height(6.dp))
                                RatingPill(rating)
                            }
                        }
                    }
                }

                Button(
                    onClick = onAddClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MusFitTheme.colors.accent,
                        contentColor = MusFitTheme.colors.onAccent,
                    ),
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add to ${meal.title}")
                }
            }

            if (meal.entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MusFitTheme.colors.outline)
                meal.entries.forEach { entry ->
                    DiaryEntryRow(
                        entry = entry,
                        onClick = { onEntryClick(entry.id) },
                    )
                }
            }
        }
    }
}

private fun FoodMealSectionUiState.summaryLabel(): String =
    when {
        caloriesKcal > 0.0 && plannedCaloriesKcal > 0.0 ->
            "${caloriesKcal.roundToInt()} logged | ${plannedCaloriesKcal.roundToInt()} planned"
        caloriesKcal > 0.0 -> "${caloriesKcal.roundToInt()} kcal logged"
        plannedCaloriesKcal > 0.0 -> "${plannedCaloriesKcal.roundToInt()} kcal planned"
        else -> recommendation
    }

@Composable
private fun DiaryEntryRow(
    entry: FoodMealEntryUiState,
    showContributions: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = MusFitTheme.shapes.small,
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
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${entry.quantityGrams.roundToInt()} g",
                        style = MaterialTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
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
                    text = "P ${entry.proteinGrams.formatNutritionDisplay()} g | C ${entry.carbsGrams.formatNutritionDisplay()} g | F ${entry.fatGrams.formatNutritionDisplay()} g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (showContributions) {
                    MealItemContributionBars(entry = entry)
                }
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

@Composable
private fun MealItemContributionBars(entry: FoodMealEntryUiState) {
    val macroColors = MusFitTheme.colors.macroColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, end = 12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        MealItemContributionBar(
            label = "Calories",
            progress = entry.calorieContribution,
            color = MusFitTheme.colors.brand,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MealItemContributionBar(
                label = "P",
                progress = entry.proteinContribution,
                color = macroColors[1],
                modifier = Modifier.weight(1f),
            )
            MealItemContributionBar(
                label = "C",
                progress = entry.carbsContribution,
                color = macroColors[0],
                modifier = Modifier.weight(1f),
            )
            MealItemContributionBar(
                label = "F",
                progress = entry.fatContribution,
                color = macroColors[2],
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MealItemContributionBar(
    label: String,
    progress: Double,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MusFitTheme.colors.onSurfaceVariant,
            modifier = Modifier.width(46.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        ProgressBar(
            progress = progress.toFloat().coerceIn(0f, 1f),
            color = color,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun FoodDatabasePreview(
    savedFoods: List<SavedFoodUiState>,
    onOpenClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle("Food database")
            OutlinedButton(onClick = onOpenClick) {
                Text("Open")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MusFitTheme.colors.surface),
            shape = MusFitTheme.shapes.small,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (savedFoods.isEmpty()) {
                    Text(
                        text = "No saved foods yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                } else {
                    savedFoods.take(3).forEach { food ->
                        SavedFoodSummaryRow(food)
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedFoodSummaryRow(food: SavedFoodUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = food.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = food.brand ?: "${food.defaultServingGrams.roundToInt()} g serving",
                style = MaterialTheme.typography.bodySmall,
                color = MusFitTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = "${food.caloriesPerServingKcal.roundToInt()} kcal",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MusFitTheme.colors.brand,
        )
    }
}



internal val FoodGoalMode.label: String
    get() =
        when (this) {
            FoodGoalMode.Balanced -> "Balanced"
            FoodGoalMode.HighProtein -> "High protein"
            FoodGoalMode.KetoLowCarb -> "Keto / low carb"
            FoodGoalMode.MuscleGain -> "Muscle gain"
            FoodGoalMode.WeightLoss -> "Weight loss"
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

