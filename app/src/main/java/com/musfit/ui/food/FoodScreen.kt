package com.musfit.ui.food

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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            FoodSummaryHeader(
                state = state,
                onQuickAddClick = {
                    viewModel.openAddFood("snacks")
                    viewModel.selectAddMode(FoodAddMode.Quick)
                },
            )

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                MacroProgressRow(state.macroProgress)

                state.message?.let { message ->
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                SectionTitle("Meal diary")
                state.mealSections.forEach { meal ->
                    MealSectionCard(
                        meal = meal,
                        onAddClick = { viewModel.openAddFood(meal.id) },
                    )
                }

                FoodDatabasePreview(
                    savedFoods = state.savedFoods,
                    onOpenClick = {
                        viewModel.openAddFood("breakfast")
                        viewModel.selectAddMode(FoodAddMode.Saved)
                    },
                )
            }
        }

        FloatingActionButton(
            onClick = { viewModel.openAddFood("breakfast") },
            containerColor = ActionGreen,
            contentColor = Color(0xFF053D2D),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        ) {
            Text(
                text = "+",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }

    if (state.isAddPanelVisible) {
        ModalBottomSheet(
            onDismissRequest = viewModel::closeAddFood,
            containerColor = Color.White,
        ) {
            AddFoodPanel(
                state = state,
                onModeSelected = viewModel::selectAddMode,
                onSavedQuantityChanged = viewModel::onSavedFoodQuantityChanged,
                onSavedFoodClick = viewModel::logSavedFood,
                onProductNameChanged = viewModel::onProductNameChanged,
                onBrandChanged = viewModel::onBrandChanged,
                onQuantityChanged = viewModel::onQuantityChanged,
                onCaloriesChanged = viewModel::onCaloriesChanged,
                onProteinChanged = viewModel::onProteinChanged,
                onCarbsChanged = viewModel::onCarbsChanged,
                onFatChanged = viewModel::onFatChanged,
                onBarcodeChanged = viewModel::onBarcodeChanged,
                onLookupClick = viewModel::lookupBarcode,
                onScanClick = onScanClick,
                onLogFoodClick = viewModel::logFood,
                onQuickCaloriesChanged = viewModel::onQuickCaloriesChanged,
                onQuickProteinChanged = viewModel::onQuickProteinChanged,
                onQuickCarbsChanged = viewModel::onQuickCarbsChanged,
                onQuickFatChanged = viewModel::onQuickFatChanged,
                onQuickLogClick = viewModel::quickLog,
            )
        }
    }
}

@Composable
private fun FoodSummaryHeader(
    state: FoodUiState,
    onQuickAddClick: () -> Unit,
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
                        text = "Today",
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
) {
    Box(
        modifier = Modifier
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
private fun MealSectionCard(
    meal: FoodMealSectionUiState,
    onAddClick: () -> Unit,
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
                Row(
                    modifier = Modifier.weight(1f),
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
                    DiaryEntryRow(entry)
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
private fun DiaryEntryRow(entry: FoodMealEntryUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
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
        }
        Text(
            text = "${entry.caloriesKcal.roundToInt()} kcal",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF315847),
            fontWeight = FontWeight.SemiBold,
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
private fun AddFoodPanel(
    state: FoodUiState,
    onModeSelected: (FoodAddMode) -> Unit,
    onSavedQuantityChanged: (String) -> Unit,
    onSavedFoodClick: (String) -> Unit,
    onProductNameChanged: (String) -> Unit,
    onBrandChanged: (String) -> Unit,
    onQuantityChanged: (String) -> Unit,
    onCaloriesChanged: (String) -> Unit,
    onProteinChanged: (String) -> Unit,
    onCarbsChanged: (String) -> Unit,
    onFatChanged: (String) -> Unit,
    onBarcodeChanged: (String) -> Unit,
    onLookupClick: () -> Unit,
    onScanClick: () -> Unit,
    onLogFoodClick: () -> Unit,
    onQuickCaloriesChanged: (String) -> Unit,
    onQuickProteinChanged: (String) -> Unit,
    onQuickCarbsChanged: (String) -> Unit,
    onQuickFatChanged: (String) -> Unit,
    onQuickLogClick: () -> Unit,
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

        state.message?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        when (state.addMode) {
            FoodAddMode.Saved ->
                SavedFoodPicker(
                    state = state,
                    onQuantityChanged = onSavedQuantityChanged,
                    onSavedFoodClick = onSavedFoodClick,
                )

            FoodAddMode.Manual ->
                ManualFoodForm(
                    state = state,
                    onProductNameChanged = onProductNameChanged,
                    onBrandChanged = onBrandChanged,
                    onQuantityChanged = onQuantityChanged,
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
                    onCaloriesChanged = onCaloriesChanged,
                    onProteinChanged = onProteinChanged,
                    onCarbsChanged = onCarbsChanged,
                    onFatChanged = onFatChanged,
                    onLogFoodClick = onLogFoodClick,
                )

            FoodAddMode.Quick ->
                QuickCalorieForm(
                    state = state,
                    onQuickCaloriesChanged = onQuickCaloriesChanged,
                    onQuickProteinChanged = onQuickProteinChanged,
                    onQuickCarbsChanged = onQuickCarbsChanged,
                    onQuickFatChanged = onQuickFatChanged,
                    onQuickLogClick = onQuickLogClick,
                )
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
    onClick: () -> Unit,
) {
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
    }
}

@Composable
private fun ManualFoodForm(
    state: FoodUiState,
    onProductNameChanged: (String) -> Unit,
    onBrandChanged: (String) -> Unit,
    onQuantityChanged: (String) -> Unit,
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
    onCaloriesChanged: (String) -> Unit,
    onProteinChanged: (String) -> Unit,
    onCarbsChanged: (String) -> Unit,
    onFatChanged: (String) -> Unit,
    onLogFoodClick: () -> Unit,
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

        ProductFields(
            state = state,
            onProductNameChanged = onProductNameChanged,
            onBrandChanged = onBrandChanged,
            onQuantityChanged = onQuantityChanged,
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
            enabled = !state.isLoading && !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
        ) {
            Text(if (state.isSaving) "Logging" else "Log barcode food")
        }
    }
}

@Composable
private fun ProductFields(
    state: FoodUiState,
    onProductNameChanged: (String) -> Unit,
    onBrandChanged: (String) -> Unit,
    onQuantityChanged: (String) -> Unit,
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
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        Button(
            onClick = onQuickLogClick,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
        ) {
            Text(if (state.isSaving) "Logging" else "Log quick calories")
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

private val FoodAddMode.label: String
    get() =
        when (this) {
            FoodAddMode.Saved -> "Saved"
            FoodAddMode.Manual -> "Manual"
            FoodAddMode.Barcode -> "Barcode"
            FoodAddMode.Quick -> "Quick"
        }

private val FoodBackground = Color(0xFFF0ECE7)
private val HeaderInk = Color(0xFF073F34)
private val ActionGreen = Color(0xFF43F05A)
private val MacroColors = listOf(
    Color(0xFF99A7FF),
    Color(0xFFFF91B4),
    Color(0xFFC7A7FF),
)
