@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.musfit.ui.food

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EggAlt
import androidx.compose.material.icons.filled.Flatware
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musfit.feature.food.R
import com.musfit.ui.components.ExpressiveBadge
import com.musfit.ui.components.PillButton
import com.musfit.ui.components.SectionOverline
import com.musfit.ui.components.StepperCircleButton
import com.musfit.ui.components.expressiveBadgeShapeFor
import com.musfit.ui.components.gridGroupShape
import com.musfit.ui.text.LocalizedFormatter
import com.musfit.ui.text.asString
import com.musfit.ui.theme.BrandCoral
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccentRole
import com.musfit.ui.theme.tabAccentFor
import kotlin.math.roundToInt

// Add-food sheet (Turn 9 "9b" chrome) and its entry-mode forms: saved picker,
// manual, barcode (with the 9d match review), quick calories (9f) and the AI
// draft flow (9g), plus the shared grouped product/nutrition field cells.
// Dispatched from FoodScreen for FoodSheetMode.AddFood.

/** ± step for the quick-calories hero stepper. */
private const val QUICK_KCAL_STEP = 25

/**
 * Pure stepper math for the quick-calorie hero: parse the current input
 * (blank/invalid → 0), apply [delta], clamp at zero.
 */
internal fun quickStepperNext(current: String, delta: Int): String {
    val value = current.toDoubleOrNull() ?: 0.0
    return (value + delta).coerceAtLeast(0.0).formatNutritionDisplay()
}

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
    onClose: () -> Unit = {},
    onMealTargetSelected: (String) -> Unit = {},
) {
    // Shared by the AI draft row's edit affordance and the "Adjust items" pill.
    var aiAdjustExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 640.dp)
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .imePadding()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        FoodSheetHeader(
            title = stringResource(if (state.isPlanningMode) R.string.food_plan_food else R.string.food_add_food),
            onClose = onClose,
            chip = {
                MealTargetChip(
                    label = stringResource(R.string.food_to_meal, state.selectedMealTitleText.asString()),
                    meals = state.visibleMealDefinitions,
                    onMealSelected = onMealTargetSelected,
                )
            },
        )

        FoodAddModeRow(
            selected = state.addMode,
            onSelect = onModeSelected,
        )

        state.message?.let { message ->
            Text(
                text = message.asString(),
                style = MusFitTheme.typography.bodyMedium,
                color = MusFitTheme.colors.brand,
            )
        }

        // Weighted scrollable middle; the primary action row below stays pinned.
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (state.addMode) {
                FoodAddMode.Saved ->
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        FavoriteAddSection(
                            items = state.favoriteAddItems,
                            isSaving = state.isSaving,
                            actionVerb = state.foodEntryActionVerb(),
                            actionProgressLabel = state.foodEntryActionProgressLabel(),
                            onFoodClick = onSavedFoodClick,
                            onTemplateClick = onTemplateClick,
                            onRecipeClick = onRecipeClick,
                            onQuickLogClick = onFavoriteQuickLogClick,
                        )
                        SavedFoodPicker(
                            state = state,
                            onQuantityChanged = onSavedQuantityChanged,
                            onSavedFoodClick = onSavedFoodClick,
                            onServingSelected = onSavedFoodServingSelected,
                        )
                        TemplateQuickList(
                            templates = state.mealTemplates,
                            actionVerb = state.foodEntryActionVerb(),
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
                        onSaveProductClick = onSaveProductClick,
                    )

                FoodAddMode.Quick ->
                    QuickCalorieForm(
                        state = state,
                        onQuickCaloriesChanged = onQuickCaloriesChanged,
                        onQuickProteinChanged = onQuickProteinChanged,
                        onQuickCarbsChanged = onQuickCarbsChanged,
                        onQuickFatChanged = onQuickFatChanged,
                        onQuickSaveFavoriteClick = onQuickSaveFavoriteClick,
                    )

                FoodAddMode.Ai ->
                    AiLoggingForm(
                        state = state,
                        adjustExpanded = aiAdjustExpanded,
                        onToggleAdjust = { aiAdjustExpanded = !aiAdjustExpanded },
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
                    )
            }
        }

        // Pinned bottom action row — keep-adding sits beside the final log action.
        when (state.addMode) {
            FoodAddMode.Saved ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    KeepAddingPill(
                        checked = state.keepAddingFoods,
                        onCheckedChange = onKeepAddingChanged,
                    )
                }

            FoodAddMode.Manual ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    KeepAddingPill(
                        checked = state.keepAddingFoods,
                        onCheckedChange = onKeepAddingChanged,
                    )
                    PillButton(
                        text = if (state.isSaving) {
                            state.foodEntryActionProgressLabel()
                        } else {
                            state.foodEntryActionLabel(R.string.food_target_food)
                        },
                        onClick = onLogFoodClick,
                        icon = Icons.Outlined.Add,
                        enabled = !state.isSaving,
                        modifier = Modifier.weight(1f),
                    )
                }

            FoodAddMode.Barcode ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    KeepAddingPill(
                        checked = state.keepAddingFoods,
                        onCheckedChange = onKeepAddingChanged,
                    )
                    PillButton(
                        text = state.barcodeLogLabel(),
                        onClick = onLogFoodClick,
                        icon = Icons.Outlined.Add,
                        enabled = !state.isLoading && !state.isSaving,
                        modifier = Modifier.weight(1f),
                    )
                }

            FoodAddMode.Quick -> {
                val kcal = state.quickCaloriesKcal.toDoubleOrNull()?.roundToInt() ?: 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    KeepAddingPill(
                        checked = state.keepAddingFoods,
                        onCheckedChange = onKeepAddingChanged,
                    )
                    PillButton(
                        text = if (state.isSaving) {
                            state.foodEntryActionProgressLabel()
                        } else {
                            stringResource(
                                R.string.food_action_calories,
                                state.foodEntryActionVerb(),
                                LocalizedFormatter.integer(kcal.toLong()),
                            )
                        },
                        onClick = onQuickLogClick,
                        icon = Icons.Outlined.Bolt,
                        enabled = !state.isSaving,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            FoodAddMode.Ai ->
                if (state.aiLoggingHasDraft) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PillButton(
                            text = stringResource(R.string.food_adjust_items),
                            onClick = { aiAdjustExpanded = !aiAdjustExpanded },
                            icon = Icons.Outlined.Tune,
                            containerColor = MusFitTheme.colors.surfaceVariant,
                            contentColor = MusFitTheme.colors.onSurface,
                            height = 54.dp,
                            modifier = Modifier.weight(1f),
                        )
                        PillButton(
                            text = if (state.isSaving) {
                                state.foodEntryActionProgressLabel()
                            } else {
                                stringResource(R.string.food_log_draft)
                            },
                            onClick = onLogFoodClick,
                            icon = Icons.Outlined.Add,
                            enabled = !state.isSaving,
                            height = 54.dp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    PillButton(
                        text = stringResource(R.string.food_review_text),
                        onClick = onAiTextDraftClick,
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
        }
    }
}

/** Log-pill label for the barcode mode: match → serving-aware, no match → save-and-log. */
@Composable
private fun FoodUiState.barcodeLogLabel(): String {
    val result = lookupResult
    return when {
        isSaving -> foodEntryActionProgressLabel()
        result != null && result.servingQuantityGrams != null -> foodEntryActionLabel(R.string.food_target_one_serving)
        result != null -> foodEntryActionLabel(R.string.food_target_food)
        barcode.isNotBlank() -> saveAndFoodEntryActionLabel()
        else -> foodEntryActionLabel(R.string.food_target_food)
    }
}

@Composable
private fun FavoriteAddSection(
    items: List<FavoriteAddItemUiState>,
    isSaving: Boolean,
    actionVerb: String,
    actionProgressLabel: String,
    onFoodClick: (String) -> Unit,
    onTemplateClick: (String) -> Unit,
    onRecipeClick: (String) -> Unit,
    onQuickLogClick: (String) -> Unit,
) {
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionTitle(stringResource(R.string.food_favorites))
        // Hairline rows — no card chrome inside the sheet.
        Column(modifier = Modifier.fillMaxWidth()) {
            items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = item.subtitleText.asString(),
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
                        Text(if (isSaving) actionProgressLabel else actionVerb)
                    }
                }
                HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
            }
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
            label = { Text(stringResource(R.string.food_amount_grams)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.savedFoods.isEmpty()) {
            Text(
                text = stringResource(R.string.food_no_saved_foods),
                style = MaterialTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
        } else {
            state.savedFoods.forEach { food ->
                SavedFoodPickerRow(
                    food = food,
                    isSaving = state.isSaving,
                    actionVerb = state.foodEntryActionVerb(),
                    actionProgressLabel = state.foodEntryActionProgressLabel(),
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
    actionVerb: String,
    actionProgressLabel: String,
    selectedServingGrams: Double?,
    onServingSelected: (Double) -> Unit,
    onClick: () -> Unit,
) {
    // Hairline row — the serving chips keep their own tap targets.
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
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
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(
                            R.string.food_serving_grams,
                            food.defaultServingGrams.roundToInt(),
                            food.caloriesPerServingKcal.roundToInt(),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = onClick,
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand),
                ) {
                    Text(if (isSaving) actionProgressLabel else actionVerb)
                }
            }

            SavedFoodServingChips(
                food = food,
                selectedServingGrams = selectedServingGrams,
                onServingSelected = onServingSelected,
            )
        }
        HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
    }
}

@Composable
private fun SavedFoodServingChips(
    food: SavedFoodUiState,
    selectedServingGrams: Double?,
    onServingSelected: (Double) -> Unit,
) {
    val servingOptions = food.servings.ifEmpty {
        listOf(
            SavedFoodServingUiState(
                "${food.id}:default",
                food.servingName ?: stringResource(
                    R.string.food_value_grams,
                    LocalizedFormatter.number(food.defaultServingGrams, maximumFractionDigits = 1),
                ),
                food.defaultServingGrams,
            ),
        )
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

@Composable
private fun TemplateQuickList(
    templates: List<MealTemplateUiState>,
    actionVerb: String,
    onTemplateClick: (String) -> Unit,
    onFavoriteClick: (String, Boolean) -> Unit,
) {
    if (templates.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionTitle(stringResource(R.string.food_meal_templates))
        Column(modifier = Modifier.fillMaxWidth()) {
            templates.forEach { template ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(template.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            listOfNotNull(
                                template.itemSummary,
                                if (template.isFavorite) stringResource(R.string.food_favorite) else null,
                            ).joinToString(stringResource(R.string.food_separator)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MusFitTheme.colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        MusFitOutlinedButton(onClick = { onFavoriteClick(template.id, !template.isFavorite) }) {
                            Text(stringResource(if (template.isFavorite) R.string.food_starred else R.string.food_star))
                        }
                        MusFitOutlinedButton(onClick = { onTemplateClick(template.id) }) {
                            Text(actionVerb)
                        }
                    }
                }
                HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
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
        SectionTitle(stringResource(R.string.food_recipes))
        OutlinedTextField(
            value = state.recipeServingsToLog,
            onValueChange = onRecipeServingsChanged,
            label = { Text(stringResource(R.string.food_recipe_servings)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            state.recipes.forEach { recipe ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(recipe.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = listOfNotNull(
                                stringResource(
                                    R.string.food_recipe_macro_summary,
                                    recipe.caloriesPerServingKcal.formatNutritionDisplay(),
                                    recipe.proteinPerServingGrams.formatNutritionDisplay(),
                                    recipe.carbsPerServingGrams.formatNutritionDisplay(),
                                    recipe.fatPerServingGrams.formatNutritionDisplay(),
                                ),
                                if (recipe.isFavorite) stringResource(R.string.food_favorite) else null,
                            ).joinToString(stringResource(R.string.food_separator)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MusFitTheme.colors.onSurfaceVariant,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        MusFitOutlinedButton(onClick = { onFavoriteClick(recipe.id, !recipe.isFavorite) }) {
                            Text(stringResource(if (recipe.isFavorite) R.string.food_starred else R.string.food_star))
                        }
                        MusFitOutlinedButton(onClick = { onRecipeClick(recipe.id) }) {
                            Text(state.foodEntryActionVerb())
                        }
                    }
                }
                HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
            }
        }
    }
}

/**
 * 9g — AI text logging: honest local estimate, single editable draft. The
 * primary action ("Review text" / "Log draft") is pinned by [AddFoodPanel].
 */
@Composable
private fun AiLoggingForm(
    state: FoodUiState,
    adjustExpanded: Boolean,
    onToggleAdjust: () -> Unit,
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
) {
    val accent = tabAccentFor(TabAccentRole.Food)
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Coral squircle badge — the AI flow keeps the brand-coral identity.
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color = BrandCoral, shape = RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MusFitTheme.colors.onAccent,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column {
                Text(
                    text = stringResource(R.string.food_describe_meal),
                    style = MusFitTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                    color = MusFitTheme.colors.onSurface,
                )
                Text(
                    text = stringResource(R.string.food_local_estimate_to_meal, state.selectedMealTitleText.asString()),
                    style = MusFitTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }
        }

        OutlinedTextField(
            value = state.aiLoggingText,
            onValueChange = onAiTextChanged,
            placeholder = {
                Text(
                    text = stringResource(R.string.food_meal_description_example),
                    color = MusFitTheme.colors.onSurfaceFaint,
                )
            },
            minLines = 2,
            shape = RoundedCornerShape(24.dp),
            trailingIcon = {
                IconButton(onClick = onAiVoiceClick) {
                    Icon(
                        Icons.Outlined.Mic,
                        contentDescription = stringResource(R.string.food_voice_input),
                        tint = MusFitTheme.colors.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MusFitTheme.colors.surface,
                unfocusedContainerColor = MusFitTheme.colors.surface,
                focusedBorderColor = MusFitTheme.colors.brand,
                unfocusedBorderColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        // Honesty banner — the AI estimate is local and approximate, always.
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MusFitTheme.colors.warningContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = MusFitTheme.colors.warning,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = stringResource(R.string.food_draft_estimate_warning),
                        style = MusFitTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.W800,
                            letterSpacing = 0.6.sp,
                        ),
                        color = MusFitTheme.colors.warning,
                    )
                }
                Text(
                    text = stringResource(R.string.food_approximate_amounts_warning),
                    style = MusFitTheme.typography.bodySmall,
                    color = MusFitTheme.colors.warning,
                )
            }
        }

        if (state.aiLoggingHasDraft) {
            // The description stays editable after a draft exists — keep a
            // re-estimate path so the field never becomes a dead control.
            TextButton(
                onClick = onAiTextDraftClick,
                enabled = state.aiLoggingText.isNotBlank() && !state.isSaving,
            ) {
                Text(
                    text = stringResource(R.string.food_reestimate_from_text),
                    color = MusFitTheme.colors.brand,
                )
            }
            FoodListItemRow(
                index = 0,
                count = 1,
                title = state.productName.ifBlank {
                    state.aiLoggingDraftNameText?.asString() ?: stringResource(R.string.food_ai_draft)
                },
                subtitle = state.aiLoggingDraftReviewText?.asString()
                    ?: stringResource(
                        R.string.food_named_draft,
                        state.aiLoggingDraftSourceText?.asString() ?: stringResource(R.string.food_ai),
                    ),
                onClick = onToggleAdjust,
                fallbackIcon = Icons.Filled.EggAlt,
                badgeSize = 44.dp,
                trailingContent = {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.food_adjust_draft),
                        tint = MusFitTheme.colors.onSurfaceVariant,
                        modifier = Modifier.size(19.dp),
                    )
                },
            )
            state.amountNutritionPreview?.let { preview ->
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = accent.container,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.food_draft_total),
                            style = MusFitTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.W800,
                            ),
                            color = accent.onContainer,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = stringResource(R.string.food_integer_kcal, preview.caloriesKcal.roundToInt()),
                            style = MusFitTheme.typography.titleLarge.copy(fontSize = 18.sp),
                            color = accent.onContainer,
                        )
                        Text(
                            text = stringResource(
                                R.string.food_macro_summary,
                                preview.carbsGrams.roundToInt(),
                                preview.proteinGrams.roundToInt(),
                                preview.fatGrams.roundToInt(),
                            ),
                            style = MusFitTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = accent.onContainerVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
            if (adjustExpanded) {
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
            }
        } else {
            // Photo capture is still a placeholder — keep it quiet.
            TextButton(onClick = onAiPhotoClick) {
                Text(
                    text = stringResource(R.string.food_photo_placeholder),
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
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
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ProductFields(
            state = state,
            onProductNameChanged = onProductNameChanged,
            onBrandChanged = onBrandChanged,
            onQuantityChanged = onQuantityChanged,
            onAmountServingChoiceSelected = onAmountServingChoiceSelected,
        )
        state.amountNutritionPreview?.let { preview ->
            AmountNutritionPreview(preview = preview)
        }
        NutritionFields(
            state = state,
            onCaloriesChanged = onCaloriesChanged,
            onProteinChanged = onProteinChanged,
            onCarbsChanged = onCarbsChanged,
            onFatChanged = onFatChanged,
        )
    }
}

/**
 * Barcode entry + 9d match review. The log pill is pinned by [AddFoodPanel];
 * fields default hidden behind "Edit details" when a lookup match landed.
 */
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
    onSaveProductClick: () -> Unit,
) {
    val accent = tabAccentFor(TabAccentRole.Food)
    var editDetails by rememberSaveable(state.lookupResult == null) {
        mutableStateOf(state.lookupResult == null)
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GroupedFieldCell(
                label = stringResource(R.string.food_add_mode_barcode),
                value = state.barcode,
                onValueChange = onBarcodeChanged,
                shape = RoundedCornerShape(20.dp),
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
            PillButton(
                text = stringResource(if (state.isLoading) R.string.food_loading else R.string.food_lookup),
                onClick = onLookupClick,
                enabled = !state.isLoading && !state.isSaving,
                containerColor = MusFitTheme.colors.surfaceVariant,
                contentColor = MusFitTheme.colors.onSurface,
            )
        }

        PillButton(
            text = stringResource(R.string.food_scan_barcode),
            onClick = onScanClick,
            icon = Icons.Outlined.QrCodeScanner,
            enabled = !state.isLoading && !state.isSaving,
            containerColor = MusFitTheme.colors.surfaceVariant,
            contentColor = MusFitTheme.colors.onSurface,
            height = 52.dp,
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.lookupResult != null) {
            BarcodeLookupSummary(state = state)
        }

        NutritionLabelScanReview(state = state)

        if (state.lookupResult != null && !editDetails) {
            PillButton(
                text = stringResource(R.string.food_edit_details),
                onClick = { editDetails = true },
                icon = Icons.Outlined.Edit,
                containerColor = accent.container,
                contentColor = accent.onContainer,
                height = 54.dp,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            ProductFields(
                state = state,
                onProductNameChanged = onProductNameChanged,
                onBrandChanged = onBrandChanged,
                onQuantityChanged = onQuantityChanged,
                onAmountServingChoiceSelected = onAmountServingChoiceSelected,
            )
            state.amountNutritionPreview?.let { preview ->
                AmountNutritionPreview(preview = preview)
            }
            NutritionFields(
                state = state,
                onCaloriesChanged = onCaloriesChanged,
                onProteinChanged = onProteinChanged,
                onCarbsChanged = onCarbsChanged,
                onFatChanged = onFatChanged,
            )
        }

        if (state.lookupResult != null || state.barcode.isNotBlank()) {
            MusFitOutlinedButton(
                onClick = onSaveProductClick,
                enabled = !state.isLoading && !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(if (state.isSaving) R.string.food_saving else R.string.food_save_to_database))
            }
        }
    }
}

/**
 * 9d match review: match-confidence chip, product row, per-100g stat tiles
 * and the honesty line. Rendered whenever an Open Food Facts lookup landed.
 */
@Composable
private fun BarcodeLookupSummary(
    state: FoodUiState,
) {
    val accent = tabAccentFor(TabAccentRole.Food)
    val servingGrams = state.lookupResult?.servingQuantityGrams
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            shape = RoundedCornerShape(99.dp),
            color = accent.container,
            contentColor = accent.onContainer,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MusFitTheme.colors.brand,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = stringResource(R.string.food_match_found_open_food_facts),
                    style = MusFitTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.W800,
                    ),
                    maxLines = 1,
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExpressiveBadge(
                icon = Icons.Filled.Flatware,
                shape = expressiveBadgeShapeFor(0),
                containerColor = accent.container,
                contentColor = accent.onContainerVariant,
                size = 52.dp,
                iconSize = 24.dp,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = state.productName.ifBlank { stringResource(R.string.food_scanned_product) },
                    style = MusFitTheme.typography.headlineSmall.copy(fontSize = 19.sp),
                    color = MusFitTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = listOfNotNull(
                    state.brand.takeIf { it.isNotBlank() },
                    servingGrams?.let {
                        stringResource(R.string.food_serving_equals_grams, it.formatNutritionDisplay())
                    },
                ).joinToString(stringResource(R.string.food_separator))
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MusFitTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FoodStatTile(
                label = stringResource(R.string.food_kcal).uppercase(LocalConfiguration.current.locales[0]),
                labelColor = MusFitTheme.colors.onSurfaceVariant,
                value = state.caloriesPer100g.ifBlank { stringResource(R.string.food_not_available_dash) },
                index = 0,
                count = 4,
                modifier = Modifier.weight(1f),
            )
            FoodStatTile(
                label = stringResource(R.string.food_carbs).uppercase(LocalConfiguration.current.locales[0]),
                labelColor = MusFitTheme.colors.macroCarbs,
                value = statTileGrams(state.carbsPer100g),
                index = 1,
                count = 4,
                modifier = Modifier.weight(1f),
            )
            FoodStatTile(
                label = stringResource(R.string.food_protein).uppercase(LocalConfiguration.current.locales[0]),
                labelColor = MusFitTheme.colors.macroProtein,
                value = statTileGrams(state.proteinPer100g),
                index = 2,
                count = 4,
                modifier = Modifier.weight(1f),
            )
            FoodStatTile(
                label = stringResource(R.string.food_fat).uppercase(LocalConfiguration.current.locales[0]),
                labelColor = MusFitTheme.colors.macroFat,
                value = statTileGrams(state.fatPer100g),
                index = 3,
                count = 4,
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = stringResource(R.string.food_imported_values_warning),
            style = MusFitTheme.typography.bodySmall,
            color = MusFitTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun statTileGrams(raw: String): String = raw.takeIf { it.isNotBlank() }
    ?.let { stringResource(R.string.food_value_grams, it) }
    ?: stringResource(R.string.food_not_available_dash)

@Composable
private fun NutritionLabelScanReview(
    state: FoodUiState,
) {
    val review = state.nutritionLabelScanReview ?: return
    val parsedDetails = listOfNotNull(
        state.fiberPer100g.takeIf { it.isNotBlank() }?.let {
            stringResource(R.string.food_named_grams, stringResource(R.string.food_fiber), it)
        },
        state.sugarPer100g.takeIf { it.isNotBlank() }?.let {
            stringResource(R.string.food_named_grams, stringResource(R.string.food_sugar), it)
        },
        state.saturatedFatPer100g.takeIf { it.isNotBlank() }?.let {
            stringResource(R.string.food_named_grams, stringResource(R.string.food_saturated_fat), it)
        },
        state.sodiumMgPer100g.takeIf { it.isNotBlank() }?.let {
            stringResource(R.string.food_named_milligrams, stringResource(R.string.food_sodium), it)
        },
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MusFitTheme.colors.positiveContainer,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = review.confidenceLabel,
                style = MaterialTheme.typography.titleSmall,
                color = MusFitTheme.colors.brandInk,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.food_parsed_fields_found,
                    review.parsedFieldCount,
                    review.parsedFieldCount,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
            if (parsedDetails.isNotEmpty()) {
                Text(
                    text = parsedDetails.joinToString(stringResource(R.string.food_detail_separator)),
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
    val accent = tabAccentFor(TabAccentRole.Food)
    var editDetails by rememberSaveable(state.lookupResult == null) {
        mutableStateOf(state.lookupResult == null)
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PillButton(
                text = stringResource(R.string.food_scan_barcode),
                onClick = onScanBarcode,
                icon = Icons.Outlined.QrCodeScanner,
                enabled = !state.isLoading && !state.isSaving,
                containerColor = MusFitTheme.colors.surfaceVariant,
                contentColor = MusFitTheme.colors.onSurface,
                height = 52.dp,
                modifier = Modifier.weight(1f),
            )
            PillButton(
                text = stringResource(R.string.food_scan_label),
                onClick = onScanLabel,
                icon = Icons.Outlined.DocumentScanner,
                enabled = !state.isLoading && !state.isSaving,
                containerColor = MusFitTheme.colors.surfaceVariant,
                contentColor = MusFitTheme.colors.onSurface,
                height = 52.dp,
                modifier = Modifier.weight(1f),
            )
        }

        if (state.lookupResult != null) {
            BarcodeLookupSummary(state = state)
        }

        if (state.lookupResult != null && !editDetails) {
            PillButton(
                text = stringResource(R.string.food_edit_details),
                onClick = { editDetails = true },
                icon = Icons.Outlined.Edit,
                containerColor = accent.container,
                contentColor = accent.onContainer,
                height = 54.dp,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            ProductFields(
                state = state,
                onProductNameChanged = onProductNameChanged,
                onBrandChanged = onBrandChanged,
                onQuantityChanged = onQuantityChanged,
                onAmountServingChoiceSelected = onAmountServingChoiceSelected,
            )
            // Live total for the chosen serving, directly under the amount.
            state.amountNutritionPreview?.let { preview ->
                AmountNutritionPreview(preview = preview)
            }
            NutritionFields(
                state = state,
                onCaloriesChanged = onCaloriesChanged,
                onProteinChanged = onProteinChanged,
                onCarbsChanged = onCarbsChanged,
                onFatChanged = onFatChanged,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PillButton(
                text = stringResource(if (state.isSaving) R.string.food_saving else R.string.food_save_to_database),
                onClick = onSaveProduct,
                enabled = !state.isLoading && !state.isSaving,
                containerColor = MusFitTheme.colors.surfaceVariant,
                contentColor = MusFitTheme.colors.onSurface,
                height = 54.dp,
                modifier = Modifier.weight(1f),
            )
            PillButton(
                text = when {
                    state.isSaving -> state.foodEntryActionProgressLabel()
                    state.lookupResult?.servingQuantityGrams != null -> state.foodEntryActionLabel(R.string.food_target_one_serving)
                    else -> state.foodEntryActionLabel(R.string.food_target_food)
                },
                onClick = onLogFood,
                icon = Icons.Outlined.Add,
                enabled = !state.isLoading && !state.isSaving,
                height = 54.dp,
                modifier = Modifier.weight(1f),
            )
        }

        HorizontalDivider(color = MusFitTheme.colors.outline)
        TextButton(onClick = onCreateRecipe, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.food_create_meal_or_recipe))
        }
    }
}

/** White grouped field cell — the kit's label-over-value form cell. */
@Composable
private fun GroupedFieldCell(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
    labelColor: Color? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = labelColor ?: MusFitTheme.colors.onSurfaceFaint) },
        singleLine = true,
        shape = shape,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MusFitTheme.colors.surface,
            unfocusedContainerColor = MusFitTheme.colors.surface,
            focusedBorderColor = MusFitTheme.colors.brand,
            unfocusedBorderColor = Color.Transparent,
        ),
        modifier = modifier,
    )
}

@Composable
private fun ProductFields(
    state: FoodUiState,
    onProductNameChanged: (String) -> Unit,
    onBrandChanged: (String) -> Unit,
    onQuantityChanged: (String) -> Unit,
    onAmountServingChoiceSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        GroupedFieldCell(
            label = stringResource(R.string.food_food_name),
            value = state.productName,
            onValueChange = onProductNameChanged,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            GroupedFieldCell(
                label = stringResource(R.string.food_brand),
                value = state.brand,
                onValueChange = onBrandChanged,
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 20.dp, bottomEnd = 8.dp),
                modifier = Modifier.weight(1f),
            )
            GroupedFieldCell(
                label = stringResource(R.string.food_amount_grams),
                value = state.quantityGrams,
                onValueChange = onQuantityChanged,
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 20.dp),
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
            )
        }
        if (state.amountServingChoices.isNotEmpty()) {
            val selectedAmount = state.quantityGrams.toDoubleOrNull()
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                state.amountServingChoices.forEach { choice ->
                    SelectableChip(
                        text = choice.label,
                        selected = selectedAmount == choice.grams,
                        onClick = { onAmountServingChoiceSelected(choice.id) },
                        unselectedContainer = MusFitTheme.colors.surfaceVariant,
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionOverline(
            stringResource(R.string.food_nutrition_per_hundred_grams)
                .uppercase(LocalConfiguration.current.locales[0]),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                GroupedFieldCell(
                    label = stringResource(R.string.food_calories),
                    value = state.caloriesPer100g,
                    onValueChange = onCaloriesChanged,
                    shape = gridGroupShape(row = 0, rowCount = 2, column = 0, columnCount = 2, outer = 20.dp),
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                )
                GroupedFieldCell(
                    label = stringResource(R.string.food_carbs),
                    value = state.carbsPer100g,
                    onValueChange = onCarbsChanged,
                    shape = gridGroupShape(row = 0, rowCount = 2, column = 1, columnCount = 2, outer = 20.dp),
                    labelColor = MusFitTheme.colors.macroCarbs,
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                GroupedFieldCell(
                    label = stringResource(R.string.food_protein),
                    value = state.proteinPer100g,
                    onValueChange = onProteinChanged,
                    shape = gridGroupShape(row = 1, rowCount = 2, column = 0, columnCount = 2, outer = 20.dp),
                    labelColor = MusFitTheme.colors.macroProtein,
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                )
                GroupedFieldCell(
                    label = stringResource(R.string.food_fat),
                    value = state.fatPer100g,
                    onValueChange = onFatChanged,
                    shape = gridGroupShape(row = 1, rowCount = 2, column = 1, columnCount = 2, outer = 20.dp),
                    labelColor = MusFitTheme.colors.macroFat,
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Live-preview hero (9c grammar): big kcal on the green container. */
@Composable
private fun AmountNutritionPreview(
    preview: FoodAmountNutritionPreviewUiState,
) {
    val accent = tabAccentFor(TabAccentRole.Food)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = accent.container,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = LocalizedFormatter.integer(
                        preview.caloriesKcal.roundToInt().toLong(),
                        locale = LocalConfiguration.current.locales[0],
                    ),
                    style = MusFitTheme.typography.headlineMedium,
                    color = accent.onContainer,
                    maxLines = 1,
                )
                Text(
                    text = stringResource(R.string.food_kcal),
                    style = MusFitTheme.typography.bodySmall.copy(fontWeight = FontWeight.W600),
                    color = accent.onContainerVariant,
                    modifier = Modifier.padding(start = 5.dp, bottom = 5.dp),
                )
            }
            Text(
                text = stringResource(
                    R.string.food_amount_macro_summary,
                    preview.quantityGrams.formatNutritionDisplay(),
                    preview.carbsGrams.formatNutritionDisplay(),
                    preview.proteinGrams.formatNutritionDisplay(),
                    preview.fatGrams.formatNutritionDisplay(),
                ),
                style = MusFitTheme.typography.bodySmall,
                color = accent.onContainerVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun quickInputsMatchPreset(state: FoodUiState, preset: QuickCaloriePresetUiState): Boolean = state.quickCaloriesKcal.toDoubleOrNull() == preset.caloriesKcal &&
    state.quickProteinGrams.toDoubleOrNull() == preset.proteinGrams &&
    state.quickCarbsGrams.toDoubleOrNull() == preset.carbsGrams &&
    state.quickFatGrams.toDoubleOrNull() == preset.fatGrams

/**
 * 9f — Quick calories: big ±25 stepper hero, favorite preset chips (tap fills
 * all four inputs), compact macro fields. Log pill pinned by [AddFoodPanel].
 */
@Composable
private fun QuickCalorieForm(
    state: FoodUiState,
    onQuickCaloriesChanged: (String) -> Unit,
    onQuickProteinChanged: (String) -> Unit,
    onQuickCarbsChanged: (String) -> Unit,
    onQuickFatChanged: (String) -> Unit,
    onQuickSaveFavoriteClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MusFitTheme.colors.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
            ) {
                StepperCircleButton(
                    icon = Icons.Outlined.Remove,
                    contentDescription = stringResource(R.string.food_decrease_calories),
                    onClick = { onQuickCaloriesChanged(quickStepperNext(state.quickCaloriesKcal, -QUICK_KCAL_STEP)) },
                    size = 56.dp,
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = state.quickCaloriesKcal.ifBlank { "0" },
                        style = MusFitTheme.typography.displayLarge,
                        color = MusFitTheme.colors.onSurface,
                        maxLines = 1,
                    )
                    Text(
                        text = stringResource(R.string.food_kcal_step, QUICK_KCAL_STEP),
                        style = MusFitTheme.typography.bodySmall.copy(fontWeight = FontWeight.W600),
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                }
                StepperCircleButton(
                    icon = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.food_increase_calories),
                    onClick = { onQuickCaloriesChanged(quickStepperNext(state.quickCaloriesKcal, QUICK_KCAL_STEP)) },
                    size = 56.dp,
                    filled = true,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionOverline(
                stringResource(R.string.food_favorite_presets)
                    .uppercase(LocalConfiguration.current.locales[0]),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                state.quickCaloriePresets.forEach { preset ->
                    SelectableChip(
                        text = stringResource(
                            R.string.food_named_calories,
                            preset.name,
                            preset.caloriesKcal.roundToInt(),
                        ),
                        selected = quickInputsMatchPreset(state, preset),
                        onClick = {
                            onQuickCaloriesChanged(preset.caloriesKcal.formatNutritionDisplay())
                            onQuickProteinChanged(preset.proteinGrams.formatNutritionDisplay())
                            onQuickCarbsChanged(preset.carbsGrams.formatNutritionDisplay())
                            onQuickFatChanged(preset.fatGrams.formatNutritionDisplay())
                        },
                    )
                }
                SelectableChip(
                    text = stringResource(R.string.food_save_preset),
                    selected = null,
                    onClick = onQuickSaveFavoriteClick,
                    unselectedContent = MusFitTheme.colors.brand,
                    leadingIcon = Icons.Outlined.Add,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SmallNumberField(
                label = stringResource(R.string.food_protein),
                value = state.quickProteinGrams,
                onValueChange = onQuickProteinChanged,
                modifier = Modifier.weight(1f),
            )
            SmallNumberField(
                label = stringResource(R.string.food_carbs),
                value = state.quickCarbsGrams,
                onValueChange = onQuickCarbsChanged,
                modifier = Modifier.weight(1f),
            )
            SmallNumberField(
                label = stringResource(R.string.food_fat),
                value = state.quickFatGrams,
                onValueChange = onQuickFatChanged,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
