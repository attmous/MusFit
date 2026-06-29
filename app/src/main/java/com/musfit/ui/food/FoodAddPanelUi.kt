@file:OptIn(ExperimentalMaterial3Api::class)

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

// Add-food panel and its entry-mode forms (saved picker, manual, barcode,
// quick calories, AI shell, create-food) plus shared product/nutrition field
// rows. Dispatched from FoodScreen for FoodSheetMode.AddFood. Extracted
// verbatim from FoodScreen.kt with no behavior change.

@Composable
internal fun AddFoodPanel(
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
    onAiTextChanged: (String) -> Unit,
    onAiTextDraftClick: () -> Unit,
    onAiVoiceClick: () -> Unit,
    onAiPhotoClick: () -> Unit,
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
                color = MusFitTheme.colors.onSurfaceVariant,
            )
        }

        AddModeTabs(
            selectedMode = state.addMode,
            onModeSelected = onModeSelected,
        )

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

            FoodAddMode.Ai ->
                AiLoggingForm(
                    state = state,
                    onAiTextChanged = onAiTextChanged,
                    onAiTextDraftClick = onAiTextDraftClick,
                    onAiVoiceClick = onAiVoiceClick,
                    onAiPhotoClick = onAiPhotoClick,
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
            Surface(color = MusFitTheme.colors.surfaceVariant, shape = MusFitTheme.shapes.small) {
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
                            color = MusFitTheme.colors.onSurfaceVariant,
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
                        colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand),
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
        FoodAddMode.entries.filter { it != FoodAddMode.Ai }.forEach { mode ->
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
                color = MusFitTheme.colors.onSurfaceVariant,
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
        color = MusFitTheme.colors.surfaceVariant,
        shape = MusFitTheme.shapes.small,
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
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = onClick,
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand),
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
                color = MusFitTheme.colors.surfaceVariant,
                shape = MusFitTheme.shapes.small,
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
                            color = MusFitTheme.colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        MusFitOutlinedButton(onClick = { onFavoriteClick(template.id, !template.isFavorite) }) {
                            Text(if (template.isFavorite) "Starred" else "Star")
                        }
                        MusFitOutlinedButton(onClick = { onTemplateClick(template.id) }) {
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
                color = MusFitTheme.colors.surfaceVariant,
                shape = MusFitTheme.shapes.small,
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
                                "P ${recipe.proteinPerServingGrams.roundToInt()}g",
                                "C ${recipe.carbsPerServingGrams.roundToInt()}g",
                                "F ${recipe.fatPerServingGrams.roundToInt()}g",
                                if (recipe.isFavorite) "Favorite" else null,
                            ).joinToString(" - "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MusFitTheme.colors.onSurfaceVariant,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        MusFitOutlinedButton(onClick = { onFavoriteClick(recipe.id, !recipe.isFavorite) }) {
                            Text(if (recipe.isFavorite) "Starred" else "Star")
                        }
                        MusFitOutlinedButton(onClick = { onRecipeClick(recipe.id) }) {
                            Text("Log")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiLoggingForm(
    state: FoodUiState,
    onAiTextChanged: (String) -> Unit,
    onAiTextDraftClick: () -> Unit,
    onAiVoiceClick: () -> Unit,
    onAiPhotoClick: () -> Unit,
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
        OutlinedTextField(
            value = state.aiLoggingText,
            onValueChange = onAiTextChanged,
            label = { Text("Describe meal") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onAiTextDraftClick,
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand),
            ) {
                Text("Review text", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            MusFitOutlinedButton(
                onClick = onAiVoiceClick,
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f),
            ) {
                Text("Voice", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            MusFitOutlinedButton(
                onClick = onAiPhotoClick,
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f),
            ) {
                Text("Photo", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        if (state.aiLoggingHasDraft) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MusFitTheme.shapes.small,
                color = MusFitTheme.colors.positiveContainer,
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "${state.aiLoggingDraftSourceLabel ?: "AI"} draft",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MusFitTheme.colors.brandInk,
                    )
                    state.aiLoggingDraftReview?.let { review ->
                        Text(
                            text = review,
                            style = MaterialTheme.typography.bodySmall,
                            color = MusFitTheme.colors.onSurfaceVariant,
                        )
                    }
                }
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
            Button(
                onClick = onLogFoodClick,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand),
            ) {
                Text(if (state.isSaving) "Logging" else "Log reviewed food")
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
            colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand),
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

        MusFitOutlinedButton(
            onClick = onScanClick,
            enabled = !state.isLoading && !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Scan barcode")
        }

        if (state.lookupResult != null) {
            BarcodeLookupSummary(state = state)
        }

        NutritionLabelScanReview(state = state)

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
                MusFitOutlinedButton(
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
                    colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand),
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
                colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand),
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
        shape = MusFitTheme.shapes.small,
        color = MusFitTheme.colors.positiveContainer,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Product loaded",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MusFitTheme.colors.brandInk,
            )
            Text(
                text = listOfNotNull(
                    state.productName.takeIf { it.isNotBlank() },
                    state.brand.takeIf { it.isNotBlank() },
                    state.barcode.takeIf { it.isNotBlank() },
                ).joinToString(" - "),
                style = MaterialTheme.typography.bodySmall,
                color = MusFitTheme.colors.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun NutritionLabelScanReview(
    state: FoodUiState,
) {
    val review = state.nutritionLabelScanReview ?: return
    val parsedDetails = listOfNotNull(
        state.fiberPer100g.takeIf { it.isNotBlank() }?.let { "Fiber $it g" },
        state.sugarPer100g.takeIf { it.isNotBlank() }?.let { "Sugar $it g" },
        state.saturatedFatPer100g.takeIf { it.isNotBlank() }?.let { "Sat fat $it g" },
        state.sodiumMgPer100g.takeIf { it.isNotBlank() }?.let { "Sodium $it mg" },
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MusFitTheme.shapes.small,
        color = MusFitTheme.colors.positiveContainer,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = review.confidenceLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MusFitTheme.colors.brandInk,
            )
            Text(
                text = "${review.parsedFieldCount} fields found. Review before saving.",
                style = MaterialTheme.typography.bodySmall,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
            if (parsedDetails.isNotEmpty()) {
                Text(
                    text = parsedDetails.joinToString("  |  "),
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
internal fun CreateFoodForm(
    state: FoodUiState,
    onScanBarcode: () -> Unit,
    onScanLabel: () -> Unit,
    onProductNameChanged: (String) -> Unit,
    onBrandChanged: (String) -> Unit,
    onQuantityChanged: (String) -> Unit,
    onAmountServingChoiceSelected: (String) -> Unit,
    onCaloriesChanged: (String) -> Unit,
    onProteinChanged: (String) -> Unit,
    onCarbsChanged: (String) -> Unit,
    onFatChanged: (String) -> Unit,
    onSaveProduct: () -> Unit,
    onLogFood: () -> Unit,
    onCreateRecipe: () -> Unit,
) {
    // Outer = section rhythm (10dp); inner field-group gaps (ProductFields/NutritionFields)
    // are tighter (8dp) so grouping reads as inner < outer, per M3 spacing hierarchy.
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MusFitOutlinedButton(
                onClick = onScanBarcode,
                enabled = !state.isLoading && !state.isSaving,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.QrCodeScanner, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Scan barcode")
            }
            MusFitOutlinedButton(
                onClick = onScanLabel,
                enabled = !state.isLoading && !state.isSaving,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.DocumentScanner, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Scan label")
            }
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
        // Live total for the chosen serving, directly under the amount so it visibly updates as you type.
        state.amountNutritionPreview?.let { preview ->
            AmountNutritionPreview(preview = preview)
        }
        NutritionFields(
            state = state,
            onCaloriesChanged = onCaloriesChanged,
            onProteinChanged = onProteinChanged,
            onCarbsChanged = onCarbsChanged,
            onFatChanged = onFatChanged,
            showAmountPreview = false,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MusFitOutlinedButton(
                onClick = onSaveProduct,
                enabled = !state.isLoading && !state.isSaving,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (state.isSaving) "Saving" else "Save to database")
            }
            Button(
                onClick = onLogFood,
                enabled = !state.isLoading && !state.isSaving,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand),
            ) {
                Text(if (state.isSaving) "Logging" else "Log food")
            }
        }

        HorizontalDivider(color = MusFitTheme.colors.outline)
        TextButton(onClick = onCreateRecipe, modifier = Modifier.fillMaxWidth()) {
            Text("Create a meal or recipe instead")
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    showAmountPreview: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Per 100 g",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MusFitTheme.colors.brand,
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
        if (showAmountPreview) {
            state.amountNutritionPreview?.let { preview ->
                AmountNutritionPreview(preview = preview)
            }
        }
    }
}

@Composable
private fun AmountNutritionPreview(
    preview: FoodAmountNutritionPreviewUiState,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MusFitTheme.shapes.small,
        color = MusFitTheme.colors.positiveContainer,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "For ${preview.quantityGrams.formatNutritionDisplay()} g",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MusFitTheme.colors.brandInk,
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
            color = MusFitTheme.colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MusFitTheme.colors.onSurface,
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
                Surface(color = MusFitTheme.colors.surfaceVariant, shape = MusFitTheme.shapes.small) {
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
                                color = MusFitTheme.colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            MusFitOutlinedButton(onClick = { onFavoriteQuickLogFavoriteClick(preset.id, !preset.isFavorite) }) {
                                Text(if (preset.isFavorite) "Starred" else "Star")
                            }
                            MusFitOutlinedButton(onClick = { onFavoriteQuickLogClick(preset.id) }) {
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
            MusFitOutlinedButton(
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
                colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand),
            ) {
                Text(if (state.isSaving) "Logging" else "Log")
            }
        }
    }
}
