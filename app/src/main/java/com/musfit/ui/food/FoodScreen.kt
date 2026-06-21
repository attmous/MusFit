package com.musfit.ui.food

import com.musfit.data.repository.FoodGoalMode
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodScreen(
    scannedBarcode: String? = null,
    onScanClick: () -> Unit = {},
    onScannedBarcodeConsumed: () -> Unit = {},
    viewModel: FoodViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val selectedMealDetail = state.selectedMealDetailForDisplay()

    LaunchedEffect(scannedBarcode) {
        if (!scannedBarcode.isNullOrBlank()) {
            viewModel.onBarcodeChanged(scannedBarcode)
            viewModel.selectAddMode(FoodAddMode.Barcode)
            onScannedBarcodeConsumed()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FoodBackground),
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
                    state.mealSections.forEach { meal ->
                        MealSectionCard(
                            meal = meal,
                            onMealClick = { viewModel.openMealDetail(meal.id) },
                            onAddClick = { viewModel.openAddFood(meal.id) },
                            onEntryClick = viewModel::openDiaryEntryEditor,
                        )
                    }

                    FoodDatabasePreview(
                        savedFoods = state.savedFoods,
                        onOpenClick = viewModel::openFoodDatabase,
                    )
                }
            }
        }

    }

    if (state.isAddPanelVisible) {
        ModalBottomSheet(
            onDismissRequest = viewModel::closeAddFood,
            containerColor = Color.White,
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
                            viewModel.copyDiaryEntryTo(state.editingDiaryEntryMealType, state.selectedDate.plusDays(1))
                        },
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
                        onSaveClick = viewModel::saveFoodGoal,
                    )

                FoodSheetMode.RecipeEditor ->
                    RecipeEditorPanel(
                        state = state,
                        onNameChanged = viewModel::onRecipeNameChanged,
                        onCategoryChanged = viewModel::onRecipeCategoryChanged,
                        onServingNameChanged = viewModel::onRecipeServingNameChanged,
                        onServingGramsChanged = viewModel::onRecipeServingGramsChanged,
                        onIngredientFoodChanged = viewModel::onRecipeIngredientFoodChanged,
                        onIngredientQuantityChanged = viewModel::onRecipeIngredientQuantityChanged,
                        onAddIngredientClick = viewModel::addRecipeIngredient,
                        onEditRecipeClick = { recipeId -> viewModel.openRecipeEditor(recipeId) },
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
            }
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
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFEFFF72), Color(0xFF63EF69), Color(0xFFB8F56A)),
                ),
            )
            .padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 24.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Food",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = HeaderInk,
                    )
                    Text(
                        text = state.selectedDate.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = HeaderInk.copy(alpha = 0.75f),
                    )
                }

                OutlinedButton(
                    onClick = onQuickAddClick,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HeaderInk),
                ) {
                    Text("Quick calories")
                }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = onPreviousDayClick) { Text("<") }
                OutlinedButton(onClick = onTodayClick) { Text("Today") }
                OutlinedButton(onClick = onNextDayClick) { Text(">") }
                OutlinedButton(onClick = onGoalClick) { Text("Goals") }
                OutlinedButton(onClick = onMealsClick) { Text("Meals") }
                OutlinedButton(onClick = onTemplatesClick) { Text("Templates") }
                OutlinedButton(onClick = onRecipeClick) { Text("Recipe") }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
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
            color = HeaderInk.copy(alpha = 0.76f),
        )
        Text(
            text = value.roundToInt().toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = HeaderInk,
        )
    }
}

@Composable
private fun CalorieRing(
    eatenCalories: Double,
    remainingCalories: Double,
    calorieGoal: Double,
) {
    val progress = (eatenCalories / calorieGoal).toFloat().coerceIn(0f, 1f)

    Box(
        modifier = Modifier.size(176.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 16.dp.toPx()
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
            drawArc(
                color = HeaderInk.copy(alpha = 0.18f),
                startAngle = 145f,
                sweepAngle = 250f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                color = HeaderInk.copy(alpha = 0.62f),
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
                color = HeaderInk,
            )
            Text(
                text = remainingCalories.roundToInt().toString(),
                style = MaterialTheme.typography.displayMedium,
                color = Color.Black,
            )
            Text(
                text = "Goal ${calorieGoal.roundToInt()} kcal",
                style = MaterialTheme.typography.labelLarge,
                color = HeaderInk.copy(alpha = 0.78f),
            )
        }
    }
}

@Composable
private fun MacroProgressRow(macros: List<FoodMacroProgressUiState>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        macros.forEachIndexed { index, macro ->
            MacroProgressCard(
                macro = macro,
                color = MacroColors[index % MacroColors.size],
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp),
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
                color = Color(0xFF67615D),
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
private fun ProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.18f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color),
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF706D6A),
    )
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
            color = Color.White,
            shape = RoundedCornerShape(8.dp),
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
                    color = Color(0xFF706D6A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "+",
                    style = MaterialTheme.typography.headlineSmall,
                    color = ActionGreen,
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
                color = Color.White,
                shape = RoundedCornerShape(8.dp),
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
                        color = Color(0xFF706D6A),
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    meal.entries.forEachIndexed { index, entry ->
                        if (index > 0) {
                            HorizontalDivider(color = Color(0xFFEDE8E4))
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
        shape = RoundedCornerShape(8.dp),
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp),
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
                Text(
                    text = "${meal.caloriesKcal.roundToInt()} kcal",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = HeaderInk,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ProgressBar(
                    progress = meal.calorieProgress.toFloat(),
                    color = ActionGreen,
                )
                Text(
                    text = "Target ${meal.calorieTargetKcal.roundToInt()} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF706D6A),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MealMacroMetric(
                    label = "Carbs",
                    grams = meal.carbsGrams,
                    goalGrams = 260.0,
                    color = MacroColors[0],
                    modifier = Modifier.weight(1f),
                )
                MealMacroMetric(
                    label = "Protein",
                    grams = meal.proteinGrams,
                    goalGrams = 104.0,
                    color = MacroColors[1],
                    modifier = Modifier.weight(1f),
                )
                MealMacroMetric(
                    label = "Fat",
                    grams = meal.fatGrams,
                    goalGrams = 69.0,
                    color = MacroColors[2],
                    modifier = Modifier.weight(1f),
                )
            }

            if (
                meal.fiberGrams > 0.0 ||
                meal.sugarGrams > 0.0 ||
                meal.saturatedFatGrams > 0.0 ||
                meal.sodiumMilligrams > 0.0
            ) {
                HorizontalDivider(color = Color(0xFFEDE8E4))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MealDetailNutritionLine(
                        firstLabel = "Fiber",
                        firstValue = "${meal.fiberGrams.formatNutritionDisplay()} g",
                        secondLabel = "Sugar",
                        secondValue = "${meal.sugarGrams.formatNutritionDisplay()} g",
                    )
                    MealDetailNutritionLine(
                        firstLabel = "Sat fat",
                        firstValue = "${meal.saturatedFatGrams.formatNutritionDisplay()} g",
                        secondLabel = "Sodium",
                        secondValue = "${meal.sodiumMilligrams.roundToInt()} mg",
                    )
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
            color = Color(0xFF706D6A),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = HeaderInk,
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
            color = Color(0xFF706D6A),
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
    onEntryClick: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp),
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
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        modifier = Modifier.padding(end = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MealInitial(title = meal.title)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = meal.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = if (meal.caloriesKcal > 0.0) {
                                    "${meal.caloriesKcal.roundToInt()} kcal logged"
                                } else {
                                    meal.recommendation
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF6D6864),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Button(
                    onClick = onAddClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE9E3DF),
                        contentColor = Color(0xFF766C66),
                    ),
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text("+", style = MaterialTheme.typography.headlineSmall)
                }
            }

            if (meal.entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFEDE8E4))
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

@Composable
private fun MealInitial(title: String) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(Color(0xFFF6F2EF)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title.first().toString(),
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF315847),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun FoodAvatar(
    text: String,
    color: Color = Color(0xFFF6F2EF),
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.firstOrNull()?.uppercase().orEmpty(),
            style = MaterialTheme.typography.titleMedium,
            color = HeaderInk,
            fontWeight = FontWeight.Bold,
        )
    }
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
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${entry.quantityGrams.roundToInt()} g",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF706D6A),
                )
                Text(
                    text = "P ${entry.proteinGrams.formatNutritionDisplay()} g | C ${entry.carbsGrams.formatNutritionDisplay()} g | F ${entry.fatGrams.formatNutritionDisplay()} g",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF706D6A),
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
                color = Color(0xFF315847),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun MealItemContributionBars(entry: FoodMealEntryUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, end = 12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        MealItemContributionBar(
            label = "Calories",
            progress = entry.calorieContribution,
            color = ActionGreen,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MealItemContributionBar(
                label = "P",
                progress = entry.proteinContribution,
                color = MacroColors[1],
                modifier = Modifier.weight(1f),
            )
            MealItemContributionBar(
                label = "C",
                progress = entry.carbsContribution,
                color = MacroColors[0],
                modifier = Modifier.weight(1f),
            )
            MealItemContributionBar(
                label = "F",
                progress = entry.fatContribution,
                color = MacroColors[2],
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
            color = Color(0xFF706D6A),
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
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(8.dp),
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
                        color = Color(0xFF706D6A),
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
                color = Color(0xFF706D6A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = "${food.caloriesPerServingKcal.roundToInt()} kcal",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF315847),
        )
    }
}

@Composable
private fun FoodDatabasePanel(
    state: FoodUiState,
    onSearchChanged: (String) -> Unit,
    onSearchOnlineClick: () -> Unit,
    onNewFoodClick: () -> Unit,
    onOpenFoodDetailClick: (String) -> Unit,
    onEditFoodClick: (String) -> Unit,
    onSaveOnlineFoodClick: (String) -> Unit,
    onImportStarterFoodsClick: () -> Unit,
    onNutritionLabelScanClick: () -> Unit,
    onMergeDuplicateFoodsClick: (String, List<String>) -> Unit,
    onFavoriteClick: (String, Boolean) -> Unit,
) {
    val foods = state.filteredDatabaseFoods()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 640.dp)
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Food database",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${state.savedFoods.size} saved foods",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF706D6A),
                )
            }
            Button(
                onClick = onNewFoodClick,
                colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
            ) {
                Text("New")
            }
        }

        OutlinedButton(onClick = onImportStarterFoodsClick, modifier = Modifier.fillMaxWidth()) {
            Text("Import starter foods")
        }

        OutlinedButton(onClick = onNutritionLabelScanClick, modifier = Modifier.fillMaxWidth()) {
            Text("Scan nutrition label")
        }

        OutlinedTextField(
            value = state.foodDatabaseQuery,
            onValueChange = onSearchChanged,
            label = { Text("Search foods") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = onSearchOnlineClick,
            enabled = !state.isSearchingFoods && !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
        ) {
            Text(if (state.isSearchingFoods) "Searching" else "Search online foods")
        }

        state.message?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        if (state.onlineFoodResults.isNotEmpty()) {
            Text(
                text = "Online results",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF315847),
            )
            state.onlineFoodResults.forEach { result ->
                OnlineFoodResultRow(
                    result = result,
                    isSaving = state.isSaving,
                    onSaveClick = { onSaveOnlineFoodClick(result.barcode) },
                )
            }
        }

        if (state.duplicateFoodGroups.isNotEmpty()) {
            DuplicateFoodGroupsSection(
                duplicateGroups = state.duplicateFoodGroups,
                isSaving = state.isSaving,
                onMergeDuplicateFoodsClick = onMergeDuplicateFoodsClick,
            )
        }

        Text(
            text = "Saved foods",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF315847),
        )

        if (foods.isEmpty()) {
            Surface(
                color = Color(0xFFF7F4F1),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = if (state.foodDatabaseQuery.isBlank()) "No saved foods yet" else "No matching foods",
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF706D6A),
                )
            }
        } else {
            foods.forEach { food ->
                SavedFoodDatabaseRow(
                    food = food,
                    onDetailClick = { onOpenFoodDetailClick(food.id) },
                    onEditClick = { onEditFoodClick(food.id) },
                    onFavoriteClick = { onFavoriteClick(food.id, !food.isFavorite) },
                )
            }
        }
    }
}

@Composable
private fun OnlineFoodResultRow(
    result: OnlineFoodResultUiState,
    isSaving: Boolean,
    onSaveClick: () -> Unit,
) {
    Surface(
        color = Color(0xFFEFFAF0),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FoodAvatar(text = result.name, color = ActionGreen.copy(alpha = 0.28f))
            Column(modifier = Modifier.weight(1f)) {
                Text(result.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = listOfNotNull(result.brand, result.category, "${result.caloriesPer100g.roundToInt()} kcal / 100g").joinToString(" - "),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF706D6A),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            OutlinedButton(onClick = onSaveClick, enabled = !isSaving) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun DuplicateFoodGroupsSection(
    duplicateGroups: List<FoodDuplicateGroupUiState>,
    isSaving: Boolean,
    onMergeDuplicateFoodsClick: (String, List<String>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Potential duplicates",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF315847),
        )
        duplicateGroups.forEach { group ->
            Surface(
                color = Color(0xFFFFF7E8),
                shape = RoundedCornerShape(8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = group.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${group.reason} - ${group.duplicateFoodIds.size + 1} foods",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF706D6A),
                        )
                    }
                    OutlinedButton(
                        onClick = { onMergeDuplicateFoodsClick(group.primaryFoodId, group.duplicateFoodIds) },
                        enabled = !isSaving,
                    ) {
                        Text("Merge")
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedFoodDatabaseRow(
    food: SavedFoodUiState,
    onDetailClick: () -> Unit,
    onEditClick: () -> Unit,
    onFavoriteClick: () -> Unit,
) {
    Surface(
        color = Color(0xFFF7F4F1),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(
                        food.sourceLabel,
                        food.brand,
                        "${food.defaultServingGrams.roundToInt()} g",
                        "${food.caloriesPerServingKcal.roundToInt()} kcal",
                    ).joinToString(" - "),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF706D6A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onFavoriteClick) {
                    Text(if (food.isFavorite) "Starred" else "Star")
                }
                OutlinedButton(onClick = onDetailClick) {
                    Text("Detail")
                }
                OutlinedButton(onClick = onEditClick) {
                    Text("Edit")
                }
            }
        }
    }
}

@Composable
private fun FoodDetailPanel(
    state: FoodUiState,
    onEditClick: () -> Unit,
    onLogClick: () -> Unit,
    onFavoriteClick: () -> Unit,
) {
    val food = state.selectedSavedFoodDetail
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 640.dp)
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (food == null) {
            Text("Food not found", style = MaterialTheme.typography.titleMedium)
            return@Column
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FoodAvatar(text = food.name, color = ActionGreen.copy(alpha = 0.24f))
            Column(modifier = Modifier.weight(1f)) {
                Text(food.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    listOfNotNull(food.brand, food.category, food.barcode?.let { "Barcode $it" }).joinToString(" - ")
                        .ifBlank { "${food.defaultServingGrams.roundToInt()} g serving" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF706D6A),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Surface(color = Color(0xFFF7F4F1), shape = RoundedCornerShape(8.dp)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Nutrition facts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                NutritionFactRow("Calories", "${food.caloriesPer100g.roundToInt()} kcal", "per 100 g")
                NutritionFactRow("Protein", "${food.proteinPer100g.roundToInt()} g", "per 100 g")
                NutritionFactRow("Carbs", "${food.carbsPer100g.roundToInt()} g", "per 100 g")
                NutritionFactRow("Fat", "${food.fatPer100g.roundToInt()} g", "per 100 g")
                NutritionFactRow("Fiber", "${food.fiberPer100g.roundToInt()} g", "per 100 g")
                NutritionFactRow("Sugar", "${food.sugarPer100g.roundToInt()} g", "per 100 g")
                NutritionFactRow("Sat fat", "${food.saturatedFatPer100g.roundToInt()} g", "per 100 g")
                NutritionFactRow("Sodium", "${food.sodiumMgPer100g.roundToInt()} mg", "per 100 g")
            }
        }

        if (food.servings.isNotEmpty()) {
            Text("Servings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                food.servings.forEach { serving ->
                    FilterChip(
                        selected = false,
                        onClick = {},
                        label = { Text("${serving.label} ${serving.grams.roundToInt()}g") },
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onLogClick,
                enabled = !state.isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
                modifier = Modifier.weight(1f),
            ) {
                Text("Log")
            }
            OutlinedButton(onClick = onFavoriteClick, modifier = Modifier.weight(1f)) {
                Text(if (food.isFavorite) "Unstar" else "Star")
            }
            OutlinedButton(onClick = onEditClick, modifier = Modifier.weight(1f)) {
                Text("Edit")
            }
        }

        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun NutritionFactRow(
    label: String,
    value: String,
    unit: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(unit, style = MaterialTheme.typography.bodySmall, color = Color(0xFF706D6A))
        }
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = HeaderInk)
    }
}

@Composable
private fun DiaryEntryEditorPanel(
    state: FoodUiState,
    onMealChanged: (String) -> Unit,
    onQuantityChanged: (String) -> Unit,
    onServingChoiceSelected: (String) -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCopyToMealClick: (String) -> Unit,
    onCopyTomorrowClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 560.dp)
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Edit diary item",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = state.editingDiaryEntryName.ifBlank { "Food item" },
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF706D6A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        MealTypeChips(
            selectedMealType = state.editingDiaryEntryMealType,
            mealDefinitions = state.mealDefinitions,
            onMealChanged = onMealChanged,
        )

        OutlinedTextField(
            value = state.editingDiaryEntryQuantityGrams,
            onValueChange = onQuantityChanged,
            label = { Text("Amount (g)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.editingDiaryEntryServingChoices.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.editingDiaryEntryServingChoices.forEach { choice ->
                    FilterChip(
                        selected = state.editingDiaryEntryQuantityGrams == choice.grams.formatNutritionDisplay(),
                        onClick = { onServingChoiceSelected(choice.id) },
                        label = { Text(choice.label) },
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFF5F2ED))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Preview before saving",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF706D6A),
            )
            Text(
                text = "${state.editingDiaryEntryPreviewCaloriesKcal.roundToInt()} kcal",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "P ${state.editingDiaryEntryPreviewProteinGrams.formatNutritionDisplay()}g",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF706D6A),
                )
                Text(
                    text = "C ${state.editingDiaryEntryPreviewCarbsGrams.formatNutritionDisplay()}g",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF706D6A),
                )
                Text(
                    text = "F ${state.editingDiaryEntryPreviewFatGrams.formatNutritionDisplay()}g",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF706D6A),
                )
            }
        }

        state.message?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Button(
            onClick = onSaveClick,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
        ) {
            Text(if (state.isSaving) "Saving" else "Save changes")
        }

        Text("Copy item", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.mealDefinitions.forEach { choice ->
                OutlinedButton(
                    onClick = { onCopyToMealClick(choice.id) },
                    enabled = !state.isSaving,
                ) {
                    Text(choice.title)
                }
            }
            OutlinedButton(
                onClick = onCopyTomorrowClick,
                enabled = !state.isSaving,
            ) {
                Text("Tomorrow")
            }
        }

        OutlinedButton(
            onClick = onDeleteClick,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Text("Delete from diary")
        }
    }
}

@Composable
private fun MealTypeChips(
    selectedMealType: String,
    mealDefinitions: List<FoodMealDefinitionUiState>,
    onMealChanged: (String) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        mealDefinitions.forEach { choice ->
            FilterChip(
                selected = selectedMealType == choice.id,
                onClick = { onMealChanged(choice.id) },
                label = { Text(choice.title) },
            )
        }
    }
}

@Composable
private fun MealSettingsPanel(
    state: FoodUiState,
    onEditClick: (String) -> Unit,
    onNameChanged: (String) -> Unit,
    onTimeChanged: (String) -> Unit,
    onSortOrderChanged: (String) -> Unit,
    onSaveClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 580.dp)
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Meal settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        state.mealDefinitions.forEach { meal ->
            Surface(color = Color(0xFFF7F4F1), shape = RoundedCornerShape(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(meal.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            listOf(
                                if (meal.isDefault) "Default" else "Custom",
                                meal.timeLabel,
                                "Order ${meal.sortOrder}",
                            ).joinToString(" - "),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF706D6A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    OutlinedButton(onClick = { onEditClick(meal.id) }) {
                        Text("Edit")
                    }
                }
            }
        }

        Surface(color = Color(0xFFF7F4F1), shape = RoundedCornerShape(8.dp)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (state.editingMealDefinitionId == null) "Add custom meal" else "Edit meal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                OutlinedTextField(
                    value = state.customMealNameInput,
                    onValueChange = onNameChanged,
                    label = { Text("Meal name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = state.customMealTimeInput,
                        onValueChange = onTimeChanged,
                        label = { Text("Time HH:mm") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = state.customMealSortOrderInput,
                        onValueChange = onSortOrderChanged,
                        label = { Text("Order") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                Button(
                    onClick = onSaveClick,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
                ) {
                    Text(if (state.isSaving) "Saving" else "Save meal")
                }
            }
        }

        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun SavedFoodEditorPanel(
    state: FoodUiState,
    onNameChanged: (String) -> Unit,
    onBrandChanged: (String) -> Unit,
    onServingChanged: (String) -> Unit,
    onCaloriesChanged: (String) -> Unit,
    onProteinChanged: (String) -> Unit,
    onCarbsChanged: (String) -> Unit,
    onFatChanged: (String) -> Unit,
    onFiberChanged: (String) -> Unit,
    onSugarChanged: (String) -> Unit,
    onSaturatedFatChanged: (String) -> Unit,
    onSodiumChanged: (String) -> Unit,
    onServingNameChanged: (String) -> Unit,
    onBarcodeChanged: (String) -> Unit,
    onCategoryChanged: (String) -> Unit,
    onFavoriteChanged: (Boolean) -> Unit,
    onSaveClick: () -> Unit,
    onDuplicateClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val isExistingFood = state.editingSavedFoodId != null
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 640.dp)
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = if (isExistingFood) "Edit saved food" else "New saved food",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Food database item",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF706D6A),
            )
        }

        OutlinedTextField(
            value = state.savedFoodName,
            onValueChange = onNameChanged,
            label = { Text("Food name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = state.savedFoodBrand,
                onValueChange = onBrandChanged,
                label = { Text("Brand") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = state.savedFoodServingGrams,
                onValueChange = onServingChanged,
                label = { Text("Serving (g)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = state.savedFoodServingName,
                onValueChange = onServingNameChanged,
                label = { Text("Serving name") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = state.savedFoodCategory,
                onValueChange = onCategoryChanged,
                label = { Text("Category") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }

        OutlinedTextField(
            value = state.savedFoodBarcode,
            onValueChange = onBarcodeChanged,
            label = { Text("Barcode") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Favorite", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Switch(checked = state.savedFoodIsFavorite, onCheckedChange = onFavoriteChanged)
        }

        SavedFoodNutritionFields(
            state = state,
            onCaloriesChanged = onCaloriesChanged,
            onProteinChanged = onProteinChanged,
            onCarbsChanged = onCarbsChanged,
            onFatChanged = onFatChanged,
            onFiberChanged = onFiberChanged,
            onSugarChanged = onSugarChanged,
            onSaturatedFatChanged = onSaturatedFatChanged,
            onSodiumChanged = onSodiumChanged,
        )

        state.message?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Button(
            onClick = onSaveClick,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
        ) {
            Text(if (state.isSaving) "Saving" else "Save food")
        }

        if (isExistingFood) {
            OutlinedButton(
                onClick = onDuplicateClick,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Duplicate food")
            }

            OutlinedButton(
                onClick = onDeleteClick,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text("Delete saved food")
            }
        }
    }
}

@Composable
private fun NutritionLabelScanPanel(
    state: FoodUiState,
    onNameChanged: (String) -> Unit,
    onBrandChanged: (String) -> Unit,
    onServingChanged: (String) -> Unit,
    onCaloriesChanged: (String) -> Unit,
    onProteinChanged: (String) -> Unit,
    onCarbsChanged: (String) -> Unit,
    onFatChanged: (String) -> Unit,
    onFiberChanged: (String) -> Unit,
    onSugarChanged: (String) -> Unit,
    onSaturatedFatChanged: (String) -> Unit,
    onSodiumChanged: (String) -> Unit,
    onServingNameChanged: (String) -> Unit,
    onCategoryChanged: (String) -> Unit,
    onSaveClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 640.dp)
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Nutrition label scan",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Review fields before saving",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF706D6A),
            )
        }

        OutlinedButton(
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Capture label photo")
        }

        OutlinedTextField(
            value = state.savedFoodName,
            onValueChange = onNameChanged,
            label = { Text("Food name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = state.savedFoodBrand,
                onValueChange = onBrandChanged,
                label = { Text("Brand") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = state.savedFoodServingGrams,
                onValueChange = onServingChanged,
                label = { Text("Serving g") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = state.savedFoodServingName,
                onValueChange = onServingNameChanged,
                label = { Text("Serving") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = state.savedFoodCategory,
                onValueChange = onCategoryChanged,
                label = { Text("Category") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }

        SavedFoodNutritionFields(
            state = state,
            onCaloriesChanged = onCaloriesChanged,
            onProteinChanged = onProteinChanged,
            onCarbsChanged = onCarbsChanged,
            onFatChanged = onFatChanged,
            onFiberChanged = onFiberChanged,
            onSugarChanged = onSugarChanged,
            onSaturatedFatChanged = onSaturatedFatChanged,
            onSodiumChanged = onSodiumChanged,
        )

        state.message?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Button(
            onClick = onSaveClick,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
        ) {
            Text(if (state.isSaving) "Saving" else "Save food")
        }
    }
}

@Composable
private fun SavedFoodNutritionFields(
    state: FoodUiState,
    onCaloriesChanged: (String) -> Unit,
    onProteinChanged: (String) -> Unit,
    onCarbsChanged: (String) -> Unit,
    onFatChanged: (String) -> Unit,
    onFiberChanged: (String) -> Unit,
    onSugarChanged: (String) -> Unit,
    onSaturatedFatChanged: (String) -> Unit,
    onSodiumChanged: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Per 100 g",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SmallNumberField(
                label = "Calories",
                value = state.savedFoodCaloriesPer100g,
                onValueChange = onCaloriesChanged,
                modifier = Modifier.weight(1f),
            )
            SmallNumberField(
                label = "Protein",
                value = state.savedFoodProteinPer100g,
                onValueChange = onProteinChanged,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SmallNumberField(
                label = "Carbs",
                value = state.savedFoodCarbsPer100g,
                onValueChange = onCarbsChanged,
                modifier = Modifier.weight(1f),
            )
            SmallNumberField(
                label = "Fat",
                value = state.savedFoodFatPer100g,
                onValueChange = onFatChanged,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SmallNumberField(
                label = "Fiber",
                value = state.savedFoodFiberPer100g,
                onValueChange = onFiberChanged,
                modifier = Modifier.weight(1f),
            )
            SmallNumberField(
                label = "Sugar",
                value = state.savedFoodSugarPer100g,
                onValueChange = onSugarChanged,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SmallNumberField(
                label = "Sat fat",
                value = state.savedFoodSaturatedFatPer100g,
                onValueChange = onSaturatedFatChanged,
                modifier = Modifier.weight(1f),
            )
            SmallNumberField(
                label = "Sodium mg",
                value = state.savedFoodSodiumMgPer100g,
                onValueChange = onSodiumChanged,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GoalEditorPanel(
    state: FoodUiState,
    onCaloriesChanged: (String) -> Unit,
    onProteinChanged: (String) -> Unit,
    onCarbsChanged: (String) -> Unit,
    onFatChanged: (String) -> Unit,
    onFiberChanged: (String) -> Unit,
    onSugarChanged: (String) -> Unit,
    onSaturatedFatChanged: (String) -> Unit,
    onSodiumChanged: (String) -> Unit,
    onModeChanged: (FoodGoalMode) -> Unit,
    onTrainingChanged: (Boolean) -> Unit,
    onSaveClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 640.dp)
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Nutrition goals", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SmallNumberField("Calories", state.goalCaloriesKcalInput, onCaloriesChanged, Modifier.weight(1f))
            SmallNumberField("Protein", state.goalProteinGramsInput, onProteinChanged, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SmallNumberField("Carbs", state.goalCarbsGramsInput, onCarbsChanged, Modifier.weight(1f))
            SmallNumberField("Fat", state.goalFatGramsInput, onFatChanged, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SmallNumberField("Fiber", state.goalFiberGramsInput, onFiberChanged, Modifier.weight(1f))
            SmallNumberField("Sugar", state.goalSugarGramsInput, onSugarChanged, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SmallNumberField("Sat fat", state.goalSaturatedFatGramsInput, onSaturatedFatChanged, Modifier.weight(1f))
            SmallNumberField("Sodium mg", state.goalSodiumMgInput, onSodiumChanged, Modifier.weight(1f))
        }
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FoodGoalMode.entries.forEach { mode ->
                FilterChip(
                    selected = state.goalModeInput == mode,
                    onClick = { onModeChanged(mode) },
                    label = { Text(mode.name) },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Include training calories", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Switch(checked = state.goalIncludeTrainingInput, onCheckedChange = onTrainingChanged)
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        Button(
            onClick = onSaveClick,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
        ) {
            Text(if (state.isSaving) "Saving" else "Save goals")
        }
    }
}

@Composable
private fun RecipeEditorPanel(
    state: FoodUiState,
    onNameChanged: (String) -> Unit,
    onCategoryChanged: (String) -> Unit,
    onServingNameChanged: (String) -> Unit,
    onServingGramsChanged: (String) -> Unit,
    onIngredientFoodChanged: (String) -> Unit,
    onIngredientQuantityChanged: (String) -> Unit,
    onAddIngredientClick: () -> Unit,
    onEditRecipeClick: (String) -> Unit,
    onFavoriteClick: (String, Boolean) -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 640.dp)
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(if (state.editingRecipeId == null) "Recipe" else "Edit recipe", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (state.recipes.isNotEmpty() && state.editingRecipeId == null) {
            Text("Saved recipes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            state.recipes.forEach { recipe ->
                Surface(color = Color(0xFFF7F4F1), shape = RoundedCornerShape(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(recipe.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                listOfNotNull(
                                    recipe.itemSummary,
                                    if (recipe.isFavorite) "Favorite" else null,
                                ).joinToString(" - "),
                                color = Color(0xFF706D6A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = { onFavoriteClick(recipe.id, !recipe.isFavorite) }) {
                                Text(if (recipe.isFavorite) "Starred" else "Star")
                            }
                            OutlinedButton(onClick = { onEditRecipeClick(recipe.id) }) {
                                Text("Edit")
                            }
                        }
                    }
                }
            }
        }
        OutlinedTextField(state.recipeName, onNameChanged, label = { Text("Recipe name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(state.recipeCategory, onCategoryChanged, label = { Text("Category") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(state.recipeServingName, onServingNameChanged, label = { Text("Serving") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        OutlinedTextField(
            state.recipeServingGrams,
            onServingGramsChanged,
            label = { Text("Serving grams") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Ingredients", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.savedFoods.forEach { food ->
                FilterChip(
                    selected = state.recipeIngredientFoodId == food.id,
                    onClick = { onIngredientFoodChanged(food.id) },
                    label = { Text(food.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                state.recipeIngredientQuantityGrams,
                onIngredientQuantityChanged,
                label = { Text("Ingredient g") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onAddIngredientClick, modifier = Modifier.width(112.dp)) {
                Text("Add")
            }
        }
        state.recipeIngredients.forEach { ingredient ->
            Surface(color = Color(0xFFF7F4F1), shape = RoundedCornerShape(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(ingredient.foodName, fontWeight = FontWeight.SemiBold)
                    Text("${ingredient.quantityGrams.roundToInt()} g", color = Color(0xFF706D6A))
                }
            }
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        Button(
            onClick = onSaveClick,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
        ) {
            Text(if (state.isSaving) "Saving" else "Save recipe")
        }
        if (state.editingRecipeId != null) {
            OutlinedButton(
                onClick = onDeleteClick,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text("Delete recipe")
            }
        }
    }
}

@Composable
private fun MealTemplatesPanel(
    state: FoodUiState,
    onTemplateClick: (String) -> Unit,
    onEditClick: (String) -> Unit,
    onDuplicateClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onFavoriteClick: (String, Boolean) -> Unit,
    onNameChanged: (String) -> Unit,
    onMealTypeChanged: (String) -> Unit,
    onSaveEditClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 560.dp)
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Meal templates", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (state.editingTemplateId != null) {
            Surface(color = Color(0xFFF7F4F1), shape = RoundedCornerShape(8.dp)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Edit template", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = state.templateNameInput,
                        onValueChange = onNameChanged,
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    MealTypeChips(
                        selectedMealType = state.templateMealTypeInput,
                        mealDefinitions = state.mealDefinitions,
                        onMealChanged = onMealTypeChanged,
                    )
                    Button(
                        onClick = onSaveEditClick,
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
                    ) {
                        Text(if (state.isSaving) "Saving" else "Save template")
                    }
                }
            }
        }
        if (state.mealTemplates.isEmpty()) {
            Text("No meal templates yet", color = Color(0xFF706D6A))
        } else {
            state.mealTemplates.forEach { template ->
                Surface(color = Color(0xFFF7F4F1), shape = RoundedCornerShape(8.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column {
                            Text(template.name, fontWeight = FontWeight.SemiBold)
                            Text(template.itemSummary, color = Color(0xFF706D6A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (template.isFavorite) {
                                Text("Favorite", color = ActionGreen, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onTemplateClick(template.id) }) {
                                Text("Log")
                            }
                            OutlinedButton(onClick = { onFavoriteClick(template.id, !template.isFavorite) }) {
                                Text(if (template.isFavorite) "Starred" else "Star")
                            }
                            OutlinedButton(onClick = { onEditClick(template.id) }) {
                                Text("Edit")
                            }
                            OutlinedButton(onClick = { onDuplicateClick(template.id) }) {
                                Text("Duplicate")
                            }
                            OutlinedButton(
                                onClick = { onDeleteClick(template.id) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            ) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun AddFoodPanel(
    state: FoodUiState,
    onModeSelected: (FoodAddMode) -> Unit,
    onSavedQuantityChanged: (String) -> Unit,
    onSavedFoodClick: (String) -> Unit,
    onSavedFoodServingSelected: (String, Double) -> Unit,
    onKeepAddingChanged: (Boolean) -> Unit,
    onTemplateClick: (String) -> Unit,
    onTemplateFavoriteClick: (String, Boolean) -> Unit,
    onRecipeClick: (String) -> Unit,
    onRecipeFavoriteClick: (String, Boolean) -> Unit,
    onRecipeServingsChanged: (String) -> Unit,
    onProductNameChanged: (String) -> Unit,
    onBrandChanged: (String) -> Unit,
    onQuantityChanged: (String) -> Unit,
    onAmountServingChoiceSelected: (String) -> Unit,
    onCaloriesChanged: (String) -> Unit,
    onProteinChanged: (String) -> Unit,
    onCarbsChanged: (String) -> Unit,
    onFatChanged: (String) -> Unit,
    onBarcodeChanged: (String) -> Unit,
    onLookupClick: () -> Unit,
    onScanClick: () -> Unit,
    onNutritionLabelScanClick: () -> Unit,
    onLogFoodClick: () -> Unit,
    onSaveProductClick: () -> Unit,
    onQuickCaloriesChanged: (String) -> Unit,
    onQuickProteinChanged: (String) -> Unit,
    onQuickCarbsChanged: (String) -> Unit,
    onQuickFatChanged: (String) -> Unit,
    onQuickLogClick: () -> Unit,
    onQuickSaveFavoriteClick: () -> Unit,
    onFavoriteQuickLogClick: (String) -> Unit,
    onFavoriteQuickLogFavoriteClick: (String, Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 640.dp)
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Add to ${state.selectedMealTitle}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${state.remainingCaloriesKcal.roundToInt()} kcal remaining today",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF706D6A),
            )
        }

        AddModeTabs(
            selectedMode = state.addMode,
            onModeSelected = onModeSelected,
        )

        OutlinedButton(onClick = onNutritionLabelScanClick, modifier = Modifier.fillMaxWidth()) {
            Text("Scan nutrition label")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Keep adding",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Switch(
                checked = state.keepAddingFoods,
                onCheckedChange = onKeepAddingChanged,
            )
        }

        state.message?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        FavoriteAddSection(
            items = state.favoriteAddItems,
            isSaving = state.isSaving,
            onFoodClick = onSavedFoodClick,
            onTemplateClick = onTemplateClick,
            onRecipeClick = onRecipeClick,
            onQuickLogClick = onFavoriteQuickLogClick,
        )

        when (state.addMode) {
            FoodAddMode.Saved ->
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SavedFoodPicker(
                        state = state,
                        onQuantityChanged = onSavedQuantityChanged,
                        onSavedFoodClick = onSavedFoodClick,
                        onServingSelected = onSavedFoodServingSelected,
                    )
                    TemplateQuickList(
                        templates = state.mealTemplates,
                        onTemplateClick = onTemplateClick,
                        onFavoriteClick = onTemplateFavoriteClick,
                    )
                    RecipeQuickList(
                        state = state,
                        onRecipeServingsChanged = onRecipeServingsChanged,
                        onRecipeClick = onRecipeClick,
                        onFavoriteClick = onRecipeFavoriteClick,
                    )
                }

            FoodAddMode.Manual ->
                ManualFoodForm(
                    state = state,
                    onProductNameChanged = onProductNameChanged,
                    onBrandChanged = onBrandChanged,
                    onQuantityChanged = onQuantityChanged,
                    onAmountServingChoiceSelected = onAmountServingChoiceSelected,
                    onCaloriesChanged = onCaloriesChanged,
                    onProteinChanged = onProteinChanged,
                    onCarbsChanged = onCarbsChanged,
                    onFatChanged = onFatChanged,
                    onLogFoodClick = onLogFoodClick,
                )

            FoodAddMode.Barcode ->
                BarcodeFoodForm(
                    state = state,
                    onBarcodeChanged = onBarcodeChanged,
                    onLookupClick = onLookupClick,
                    onScanClick = onScanClick,
                    onProductNameChanged = onProductNameChanged,
                    onBrandChanged = onBrandChanged,
                    onQuantityChanged = onQuantityChanged,
                    onAmountServingChoiceSelected = onAmountServingChoiceSelected,
                    onCaloriesChanged = onCaloriesChanged,
                    onProteinChanged = onProteinChanged,
                    onCarbsChanged = onCarbsChanged,
                    onFatChanged = onFatChanged,
                    onLogFoodClick = onLogFoodClick,
                    onSaveProductClick = onSaveProductClick,
                )

            FoodAddMode.Quick ->
                QuickCalorieForm(
                    state = state,
                    onQuickCaloriesChanged = onQuickCaloriesChanged,
                    onQuickProteinChanged = onQuickProteinChanged,
                    onQuickCarbsChanged = onQuickCarbsChanged,
                    onQuickFatChanged = onQuickFatChanged,
                    onQuickLogClick = onQuickLogClick,
                    onQuickSaveFavoriteClick = onQuickSaveFavoriteClick,
                    onFavoriteQuickLogClick = onFavoriteQuickLogClick,
                    onFavoriteQuickLogFavoriteClick = onFavoriteQuickLogFavoriteClick,
                )
        }
    }
}

@Composable
private fun FavoriteAddSection(
    items: List<FavoriteAddItemUiState>,
    isSaving: Boolean,
    onFoodClick: (String) -> Unit,
    onTemplateClick: (String) -> Unit,
    onRecipeClick: (String) -> Unit,
    onQuickLogClick: (String) -> Unit,
) {
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Favorites",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        items.forEach { item ->
            Surface(color = Color(0xFFF7F4F1), shape = RoundedCornerShape(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF706D6A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Button(
                        onClick = {
                            when (item.type) {
                                FavoriteAddItemType.Food -> onFoodClick(item.id)
                                FavoriteAddItemType.MealTemplate -> onTemplateClick(item.id)
                                FavoriteAddItemType.Recipe -> onRecipeClick(item.id)
                                FavoriteAddItemType.QuickLog -> onQuickLogClick(item.id)
                            }
                        },
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
                    ) {
                        Text(if (isSaving) "Adding" else "Add")
                    }
                }
            }
        }
    }
}

@Composable
private fun AddModeTabs(
    selectedMode: FoodAddMode,
    onModeSelected: (FoodAddMode) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FoodAddMode.entries.forEach { mode ->
            FilterChip(
                selected = selectedMode == mode,
                onClick = { onModeSelected(mode) },
                label = { Text(mode.label) },
            )
        }
    }
}

@Composable
private fun SavedFoodPicker(
    state: FoodUiState,
    onQuantityChanged: (String) -> Unit,
    onSavedFoodClick: (String) -> Unit,
    onServingSelected: (String, Double) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = state.savedFoodQuantityGrams,
            onValueChange = onQuantityChanged,
            label = { Text("Amount (g)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.savedFoods.isEmpty()) {
            Text(
                text = "No saved foods yet",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF706D6A),
            )
        } else {
            state.savedFoods.forEach { food ->
                SavedFoodPickerRow(
                    food = food,
                    isSaving = state.isSaving,
                    selectedServingGrams = state.selectedSavedFoodServingGramsByFoodId[food.id],
                    onServingSelected = { grams -> onServingSelected(food.id, grams) },
                    onClick = { onSavedFoodClick(food.id) },
                )
            }
        }
    }
}

@Composable
private fun SavedFoodPickerRow(
    food: SavedFoodUiState,
    isSaving: Boolean,
    selectedServingGrams: Double?,
    onServingSelected: (Double) -> Unit,
    onClick: () -> Unit,
) {
    Surface(
        color = Color(0xFFF7F4F1),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = food.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${food.defaultServingGrams.roundToInt()} g - ${food.caloriesPerServingKcal.roundToInt()} kcal",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF706D6A),
                    )
                }
                Button(
                    onClick = onClick,
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
                ) {
                    Text(if (isSaving) "Adding" else "Add")
                }
            }

            val servingOptions = food.servings.ifEmpty {
                listOf(SavedFoodServingUiState("${food.id}:default", food.servingName ?: "${food.defaultServingGrams.roundToInt()} g", food.defaultServingGrams))
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                servingOptions.forEach { serving ->
                    FilterChip(
                        selected = selectedServingGrams == serving.grams,
                        onClick = { onServingSelected(serving.grams) },
                        label = { Text(serving.label) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplateQuickList(
    templates: List<MealTemplateUiState>,
    onTemplateClick: (String) -> Unit,
    onFavoriteClick: (String, Boolean) -> Unit,
) {
    if (templates.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Meal templates",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        templates.forEach { template ->
            Surface(
                color = Color(0xFFF7F4F1),
                shape = RoundedCornerShape(8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(template.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            listOfNotNull(
                                template.itemSummary,
                                if (template.isFavorite) "Favorite" else null,
                            ).joinToString(" - "),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF706D6A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { onFavoriteClick(template.id, !template.isFavorite) }) {
                            Text(if (template.isFavorite) "Starred" else "Star")
                        }
                        OutlinedButton(onClick = { onTemplateClick(template.id) }) {
                            Text("Log")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeQuickList(
    state: FoodUiState,
    onRecipeServingsChanged: (String) -> Unit,
    onRecipeClick: (String) -> Unit,
    onFavoriteClick: (String, Boolean) -> Unit,
) {
    if (state.recipes.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Recipes",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        OutlinedTextField(
            value = state.recipeServingsToLog,
            onValueChange = onRecipeServingsChanged,
            label = { Text("Recipe servings") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        state.recipes.forEach { recipe ->
            Surface(
                color = Color(0xFFF7F4F1),
                shape = RoundedCornerShape(8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(recipe.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = listOfNotNull(
                                "${recipe.caloriesPerServingKcal.roundToInt()} kcal",
                                "${recipe.proteinPerServingGrams.roundToInt()} g protein",
                                if (recipe.isFavorite) "Favorite" else null,
                            ).joinToString(" - "),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF706D6A),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { onFavoriteClick(recipe.id, !recipe.isFavorite) }) {
                            Text(if (recipe.isFavorite) "Starred" else "Star")
                        }
                        OutlinedButton(onClick = { onRecipeClick(recipe.id) }) {
                            Text("Log")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualFoodForm(
    state: FoodUiState,
    onProductNameChanged: (String) -> Unit,
    onBrandChanged: (String) -> Unit,
    onQuantityChanged: (String) -> Unit,
    onAmountServingChoiceSelected: (String) -> Unit,
    onCaloriesChanged: (String) -> Unit,
    onProteinChanged: (String) -> Unit,
    onCarbsChanged: (String) -> Unit,
    onFatChanged: (String) -> Unit,
    onLogFoodClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ProductFields(
            state = state,
            onProductNameChanged = onProductNameChanged,
            onBrandChanged = onBrandChanged,
            onQuantityChanged = onQuantityChanged,
            onAmountServingChoiceSelected = onAmountServingChoiceSelected,
        )
        NutritionFields(
            state = state,
            onCaloriesChanged = onCaloriesChanged,
            onProteinChanged = onProteinChanged,
            onCarbsChanged = onCarbsChanged,
            onFatChanged = onFatChanged,
        )
        Button(
            onClick = onLogFoodClick,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
        ) {
            Text(if (state.isSaving) "Logging" else "Log food")
        }
    }
}

@Composable
private fun BarcodeFoodForm(
    state: FoodUiState,
    onBarcodeChanged: (String) -> Unit,
    onLookupClick: () -> Unit,
    onScanClick: () -> Unit,
    onProductNameChanged: (String) -> Unit,
    onBrandChanged: (String) -> Unit,
    onQuantityChanged: (String) -> Unit,
    onAmountServingChoiceSelected: (String) -> Unit,
    onCaloriesChanged: (String) -> Unit,
    onProteinChanged: (String) -> Unit,
    onCarbsChanged: (String) -> Unit,
    onFatChanged: (String) -> Unit,
    onLogFoodClick: () -> Unit,
    onSaveProductClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = state.barcode,
                onValueChange = onBarcodeChanged,
                label = { Text("Barcode") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = onLookupClick,
                enabled = !state.isLoading && !state.isSaving,
                modifier = Modifier.width(108.dp),
            ) {
                Text(if (state.isLoading) "Loading" else "Lookup")
            }
        }

        OutlinedButton(
            onClick = onScanClick,
            enabled = !state.isLoading && !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Scan barcode")
        }

        if (state.lookupResult != null) {
            BarcodeLookupSummary(state = state)
        }

        ProductFields(
            state = state,
            onProductNameChanged = onProductNameChanged,
            onBrandChanged = onBrandChanged,
            onQuantityChanged = onQuantityChanged,
            onAmountServingChoiceSelected = onAmountServingChoiceSelected,
        )
        NutritionFields(
            state = state,
            onCaloriesChanged = onCaloriesChanged,
            onProteinChanged = onProteinChanged,
            onCarbsChanged = onCarbsChanged,
            onFatChanged = onFatChanged,
        )
        if (state.lookupResult != null || state.barcode.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onSaveProductClick,
                    enabled = !state.isLoading && !state.isSaving,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (state.isSaving) "Saving" else "Save product")
                }
                Button(
                    onClick = onLogFoodClick,
                    enabled = !state.isLoading && !state.isSaving,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
                ) {
                    Text(
                        if (state.isSaving) {
                            "Logging"
                        } else if (state.lookupResult == null) {
                            "Save and log"
                        } else {
                            "Log food"
                        },
                    )
                }
            }
        } else {
            Button(
                onClick = onLogFoodClick,
                enabled = !state.isLoading && !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
            ) {
                Text(if (state.isSaving) "Logging" else "Log barcode food")
            }
        }
    }
}

@Composable
private fun BarcodeLookupSummary(
    state: FoodUiState,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF4FBF1),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Product loaded",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF164A2A),
            )
            Text(
                text = listOfNotNull(
                    state.productName.takeIf { it.isNotBlank() },
                    state.brand.takeIf { it.isNotBlank() },
                    state.barcode.takeIf { it.isNotBlank() },
                ).joinToString(" - "),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF706D6A),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ProductFields(
    state: FoodUiState,
    onProductNameChanged: (String) -> Unit,
    onBrandChanged: (String) -> Unit,
    onQuantityChanged: (String) -> Unit,
    onAmountServingChoiceSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = state.productName,
            onValueChange = onProductNameChanged,
            label = { Text("Food name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = state.brand,
                onValueChange = onBrandChanged,
                label = { Text("Brand") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = state.quantityGrams,
                onValueChange = onQuantityChanged,
                label = { Text("Amount (g)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
        }
        if (state.amountServingChoices.isNotEmpty()) {
            val selectedAmount = state.quantityGrams.toDoubleOrNull()
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.amountServingChoices.forEach { choice ->
                    FilterChip(
                        selected = selectedAmount == choice.grams,
                        onClick = { onAmountServingChoiceSelected(choice.id) },
                        label = { Text(choice.label) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NutritionFields(
    state: FoodUiState,
    onCaloriesChanged: (String) -> Unit,
    onProteinChanged: (String) -> Unit,
    onCarbsChanged: (String) -> Unit,
    onFatChanged: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Per 100 g",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SmallNumberField(
                label = "Calories",
                value = state.caloriesPer100g,
                onValueChange = onCaloriesChanged,
                modifier = Modifier.weight(1f),
            )
            SmallNumberField(
                label = "Protein",
                value = state.proteinPer100g,
                onValueChange = onProteinChanged,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SmallNumberField(
                label = "Carbs",
                value = state.carbsPer100g,
                onValueChange = onCarbsChanged,
                modifier = Modifier.weight(1f),
            )
            SmallNumberField(
                label = "Fat",
                value = state.fatPer100g,
                onValueChange = onFatChanged,
                modifier = Modifier.weight(1f),
            )
        }
        state.amountNutritionPreview?.let { preview ->
            AmountNutritionPreview(preview = preview)
        }
    }
}

@Composable
private fun AmountNutritionPreview(
    preview: FoodAmountNutritionPreviewUiState,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF4FBF1),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "For ${preview.quantityGrams.formatNutritionDisplay()} g",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF164A2A),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AmountNutritionMetric(
                    label = "Calories",
                    value = "${preview.caloriesKcal.roundToInt()} kcal",
                    modifier = Modifier.weight(1f),
                )
                AmountNutritionMetric(
                    label = "Protein",
                    value = "${preview.proteinGrams.formatNutritionDisplay()} g",
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AmountNutritionMetric(
                    label = "Carbs",
                    value = "${preview.carbsGrams.formatNutritionDisplay()} g",
                    modifier = Modifier.weight(1f),
                )
                AmountNutritionMetric(
                    label = "Fat",
                    value = "${preview.fatGrams.formatNutritionDisplay()} g",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AmountNutritionMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF706D6A),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF151514),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun QuickCalorieForm(
    state: FoodUiState,
    onQuickCaloriesChanged: (String) -> Unit,
    onQuickProteinChanged: (String) -> Unit,
    onQuickCarbsChanged: (String) -> Unit,
    onQuickFatChanged: (String) -> Unit,
    onQuickLogClick: () -> Unit,
    onQuickSaveFavoriteClick: () -> Unit,
    onFavoriteQuickLogClick: (String) -> Unit,
    onFavoriteQuickLogFavoriteClick: (String, Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (state.quickCaloriePresets.isNotEmpty()) {
            Text("Favorite quick logs", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            state.quickCaloriePresets.forEach { preset ->
                Surface(color = Color(0xFFF7F4F1), shape = RoundedCornerShape(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(preset.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                listOf(
                                    "${preset.caloriesKcal.roundToInt()} kcal",
                                    "P ${preset.proteinGrams.roundToInt()}",
                                    "C ${preset.carbsGrams.roundToInt()}",
                                    "F ${preset.fatGrams.roundToInt()}",
                                    if (preset.isFavorite) "Favorite" else "Saved",
                                ).joinToString(" - "),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF706D6A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = { onFavoriteQuickLogFavoriteClick(preset.id, !preset.isFavorite) }) {
                                Text(if (preset.isFavorite) "Starred" else "Star")
                            }
                            OutlinedButton(onClick = { onFavoriteQuickLogClick(preset.id) }) {
                                Text("Log")
                            }
                        }
                    }
                }
            }
        }
        OutlinedTextField(
            value = state.quickCaloriesKcal,
            onValueChange = onQuickCaloriesChanged,
            label = { Text("Calories") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SmallNumberField(
                label = "Protein",
                value = state.quickProteinGrams,
                onValueChange = onQuickProteinChanged,
                modifier = Modifier.weight(1f),
            )
            SmallNumberField(
                label = "Carbs",
                value = state.quickCarbsGrams,
                onValueChange = onQuickCarbsChanged,
                modifier = Modifier.weight(1f),
            )
            SmallNumberField(
                label = "Fat",
                value = state.quickFatGrams,
                onValueChange = onQuickFatChanged,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onQuickSaveFavoriteClick,
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f),
            ) {
                Text("Save favorite")
            }
            Button(
                onClick = onQuickLogClick,
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
            ) {
                Text(if (state.isSaving) "Logging" else "Log")
            }
        }
    }
}

@Composable
private fun SmallNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

private fun Double.formatNutritionDisplay(): String {
    val tenths = (this * 10.0).roundToInt()
    val whole = tenths / 10
    val decimal = tenths % 10
    return if (decimal == 0) {
        whole.toString()
    } else {
        "$whole.$decimal"
    }
}

private fun FoodUiState.filteredDatabaseFoods(): List<SavedFoodUiState> {
    val query = foodDatabaseQuery.trim().lowercase()
    if (query.isBlank()) {
        return savedFoods
    }
    return savedFoods.filter { food ->
        food.name.lowercase().contains(query) ||
            food.brand.orEmpty().lowercase().contains(query)
    }
}

private val FoodAddMode.label: String
    get() =
        when (this) {
            FoodAddMode.Saved -> "Saved"
            FoodAddMode.Manual -> "Manual"
            FoodAddMode.Barcode -> "Barcode"
            FoodAddMode.Quick -> "Quick"
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

private val FoodBackground = Color(0xFFF0ECE7)
private val HeaderInk = Color(0xFF073F34)
private val ActionGreen = Color(0xFF43F05A)
private val MacroColors = listOf(
    Color(0xFF99A7FF),
    Color(0xFFFF91B4),
    Color(0xFFC7A7FF),
)
