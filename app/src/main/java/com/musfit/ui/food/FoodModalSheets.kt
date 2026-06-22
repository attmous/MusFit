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

// Modal bottom-sheet panels for the Food screen (database, detail, diary-entry
// editor, saved-food editor, OCR review, goals, recipes, templates, meals,
// shopping). Dispatched from FoodScreen by FoodSheetMode. Extracted verbatim
// from FoodScreen.kt with no behavior change.

@Composable
internal fun ShoppingListPanel(
    state: FoodUiState,
    onStartDateChanged: (String) -> Unit,
    onEndDateChanged: (String) -> Unit,
    onGenerateClick: () -> Unit,
    onManualNameChanged: (String) -> Unit,
    onManualCategoryChanged: (String) -> Unit,
    onManualQuantityChanged: (String) -> Unit,
    onAddManualClick: () -> Unit,
    onToggleItem: (String, Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 640.dp)
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Shopping list", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Surface(color = MusFitTheme.colors.surfaceVariant, shape = MusFitTheme.shapes.small) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = state.shoppingStartDateInput,
                        onValueChange = onStartDateChanged,
                        label = { Text("Start") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = state.shoppingEndDateInput,
                        onValueChange = onEndDateChanged,
                        label = { Text("End") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Button(
                    onClick = onGenerateClick,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand),
                ) {
                    Text(if (state.isSaving) "Generating" else "Generate from plan")
                }
            }
        }

        Surface(color = MusFitTheme.colors.surfaceVariant, shape = MusFitTheme.shapes.small) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Manual item", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = state.manualShoppingNameInput,
                    onValueChange = onManualNameChanged,
                    label = { Text("Item") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = state.manualShoppingCategoryInput,
                        onValueChange = onManualCategoryChanged,
                        label = { Text("Category") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = state.manualShoppingQuantityInput,
                        onValueChange = onManualQuantityChanged,
                        label = { Text("g") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedButton(onClick = onAddManualClick, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
                    Text("Add item")
                }
            }
        }

        state.message?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        if (state.shoppingListGroups.isEmpty()) {
            Text(
                text = "No shopping items yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
        } else {
            state.shoppingListGroups.forEach { group ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = group.category,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MusFitTheme.colors.brand,
                    )
                    group.items.forEach { item ->
                        Surface(color = MusFitTheme.colors.surface, shape = MusFitTheme.shapes.small) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = buildList {
                                            add(item.quantityLabel)
                                            if (item.isManual) add("Manual")
                                        }.joinToString(" - "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MusFitTheme.colors.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                FilterChip(
                                    selected = item.isChecked,
                                    onClick = { onToggleItem(item.id, !item.isChecked) },
                                    label = { Text(if (item.isChecked) "Checked" else "Needed") },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun FoodDatabasePanel(
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
    val foods = state.visibleSavedFoods
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
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }
            Button(
                onClick = onNewFoodClick,
                colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand),
            ) {
                Text("New")
            }
        }

        OutlinedButton(onClick = onImportStarterFoodsClick, modifier = Modifier.fillMaxWidth()) {
            Text("Import starter foods")
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
            colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand),
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
                color = MusFitTheme.colors.brand,
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
            color = MusFitTheme.colors.brand,
        )

        if (foods.isEmpty()) {
            Surface(
                color = MusFitTheme.colors.surfaceVariant,
                shape = MusFitTheme.shapes.small,
            ) {
                Text(
                    text = if (state.foodDatabaseQuery.isBlank()) "No saved foods yet" else "No matching foods",
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MusFitTheme.colors.onSurfaceVariant,
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
        color = MusFitTheme.colors.positiveContainer,
        shape = MusFitTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FoodThumb(imageUrl = result.imageUrl, fallback = Icons.Outlined.Restaurant)
            Column(modifier = Modifier.weight(1f)) {
                Text(result.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = listOfNotNull(result.brand, result.category, "${result.caloriesPer100g.roundToInt()} kcal / 100g").joinToString(" - "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
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
            color = MusFitTheme.colors.brand,
        )
        duplicateGroups.forEach { group ->
            Surface(
                color = MusFitTheme.colors.surfaceVariant,
                shape = MusFitTheme.shapes.small,
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
                            color = MusFitTheme.colors.onSurfaceVariant,
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
        color = MusFitTheme.colors.surfaceVariant,
        shape = MusFitTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FoodThumb(imageUrl = food.imageUrl, fallback = Icons.Outlined.Restaurant)
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
                    color = MusFitTheme.colors.onSurfaceVariant,
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
internal fun FoodDetailPanel(
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
            FoodAvatar(text = food.name, color = MusFitTheme.colors.brand.copy(alpha = 0.24f))
            Column(modifier = Modifier.weight(1f)) {
                Text(food.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    listOfNotNull(food.brand, food.category, food.barcode?.let { "Barcode $it" }).joinToString(" - ")
                        .ifBlank { "${food.defaultServingGrams.roundToInt()} g serving" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Surface(color = MusFitTheme.colors.surfaceVariant, shape = MusFitTheme.shapes.small) {
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
                colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand),
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
            Text(unit, style = MaterialTheme.typography.bodySmall, color = MusFitTheme.colors.onSurfaceVariant)
        }
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MusFitTheme.colors.brandInk)
    }
}

@Composable
internal fun DiaryEntryEditorPanel(
    state: FoodUiState,
    onMealChanged: (String) -> Unit,
    onQuantityChanged: (String) -> Unit,
    onServingChoiceSelected: (String) -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCopyToMealClick: (String) -> Unit,
    onCopyTomorrowClick: () -> Unit,
    onMarkLoggedClick: () -> Unit,
) {
    val editor = state.diaryEntryEditor ?: return
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
                text = editor.name.ifBlank { "Food item" },
                style = MaterialTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        MealTypeChips(
            selectedMealType = editor.mealType,
            mealDefinitions = state.mealDefinitions,
            onMealChanged = onMealChanged,
        )

        OutlinedTextField(
            value = editor.quantityGrams,
            onValueChange = onQuantityChanged,
            label = { Text("Amount (g)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )

        if (editor.servingChoices.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                editor.servingChoices.forEach { choice ->
                    FilterChip(
                        selected = editor.quantityGrams == choice.grams.formatNutritionDisplay(),
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
                .background(MusFitTheme.colors.surfaceVariant)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Preview before saving",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
            Text(
                text = "${editor.previewCaloriesKcal.roundToInt()} kcal",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "P ${editor.previewProteinGrams.formatNutritionDisplay()}g",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
                Text(
                    text = "C ${editor.previewCarbsGrams.formatNutritionDisplay()}g",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
                Text(
                    text = "F ${editor.previewFatGrams.formatNutritionDisplay()}g",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MusFitTheme.colors.onSurfaceVariant,
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
            colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand),
        ) {
            Text(if (state.isSaving) "Saving" else "Save changes")
        }

        if (editor.isPlanned) {
            OutlinedButton(
                onClick = onMarkLoggedClick,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Mark logged")
            }
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
internal fun MealSettingsPanel(
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
            Surface(color = MusFitTheme.colors.surfaceVariant, shape = MusFitTheme.shapes.small) {
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
                            color = MusFitTheme.colors.onSurfaceVariant,
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

        Surface(color = MusFitTheme.colors.surfaceVariant, shape = MusFitTheme.shapes.small) {
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
                    colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand),
                ) {
                    Text(if (state.isSaving) "Saving" else "Save meal")
                }
            }
        }

        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
internal fun SavedFoodEditorPanel(
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
    onPotassiumChanged: (String) -> Unit,
    onCalciumChanged: (String) -> Unit,
    onIronChanged: (String) -> Unit,
    onVitaminDChanged: (String) -> Unit,
    onVitaminCChanged: (String) -> Unit,
    onMagnesiumChanged: (String) -> Unit,
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
                color = MusFitTheme.colors.onSurfaceVariant,
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
            onPotassiumChanged = onPotassiumChanged,
            onCalciumChanged = onCalciumChanged,
            onIronChanged = onIronChanged,
            onVitaminDChanged = onVitaminDChanged,
            onVitaminCChanged = onVitaminCChanged,
            onMagnesiumChanged = onMagnesiumChanged,
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
            colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand),
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
internal fun NutritionLabelScanPanel(
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
    onPotassiumChanged: (String) -> Unit,
    onCalciumChanged: (String) -> Unit,
    onIronChanged: (String) -> Unit,
    onVitaminDChanged: (String) -> Unit,
    onVitaminCChanged: (String) -> Unit,
    onMagnesiumChanged: (String) -> Unit,
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
                color = MusFitTheme.colors.onSurfaceVariant,
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
            onPotassiumChanged = onPotassiumChanged,
            onCalciumChanged = onCalciumChanged,
            onIronChanged = onIronChanged,
            onVitaminDChanged = onVitaminDChanged,
            onVitaminCChanged = onVitaminCChanged,
            onMagnesiumChanged = onMagnesiumChanged,
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
            colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand),
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
    onPotassiumChanged: (String) -> Unit,
    onCalciumChanged: (String) -> Unit,
    onIronChanged: (String) -> Unit,
    onVitaminDChanged: (String) -> Unit,
    onVitaminCChanged: (String) -> Unit,
    onMagnesiumChanged: (String) -> Unit,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SmallNumberField(
                label = "Potassium mg",
                value = state.savedFoodPotassiumMgPer100g,
                onValueChange = onPotassiumChanged,
                modifier = Modifier.weight(1f),
            )
            SmallNumberField(
                label = "Calcium mg",
                value = state.savedFoodCalciumMgPer100g,
                onValueChange = onCalciumChanged,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SmallNumberField(
                label = "Iron mg",
                value = state.savedFoodIronMgPer100g,
                onValueChange = onIronChanged,
                modifier = Modifier.weight(1f),
            )
            SmallNumberField(
                label = "Vit D mcg",
                value = state.savedFoodVitaminDMcgPer100g,
                onValueChange = onVitaminDChanged,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SmallNumberField(
                label = "Vit C mg",
                value = state.savedFoodVitaminCMgPer100g,
                onValueChange = onVitaminCChanged,
                modifier = Modifier.weight(1f),
            )
            SmallNumberField(
                label = "Magnesium mg",
                value = state.savedFoodMagnesiumMgPer100g,
                onValueChange = onMagnesiumChanged,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun GoalEditorPanel(
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
    onNetCarbsChanged: (Boolean) -> Unit,
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
            SmallNumberField("Calories", state.goalEditor.caloriesKcalInput, onCaloriesChanged, Modifier.weight(1f))
            SmallNumberField("Protein", state.goalEditor.proteinGramsInput, onProteinChanged, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SmallNumberField("Carbs", state.goalEditor.carbsGramsInput, onCarbsChanged, Modifier.weight(1f))
            SmallNumberField("Fat", state.goalEditor.fatGramsInput, onFatChanged, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SmallNumberField("Fiber", state.goalEditor.fiberGramsInput, onFiberChanged, Modifier.weight(1f))
            SmallNumberField("Sugar", state.goalEditor.sugarGramsInput, onSugarChanged, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SmallNumberField("Sat fat", state.goalEditor.saturatedFatGramsInput, onSaturatedFatChanged, Modifier.weight(1f))
            SmallNumberField("Sodium mg", state.goalEditor.sodiumMgInput, onSodiumChanged, Modifier.weight(1f))
        }
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FoodGoalMode.entries.forEach { mode ->
                FilterChip(
                    selected = state.goalEditor.modeInput == mode,
                    onClick = { onModeChanged(mode) },
                    label = { Text(mode.label) },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Net carbs", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Switch(checked = state.goalEditor.useNetCarbsInput, onCheckedChange = onNetCarbsChanged)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Include training calories", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Switch(checked = state.goalEditor.includeTrainingInput, onCheckedChange = onTrainingChanged)
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        Button(
            onClick = onSaveClick,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand),
        ) {
            Text(if (state.isSaving) "Saving" else "Save goals")
        }
    }
}

@Composable
internal fun RecipeEditorPanel(
    state: FoodUiState,
    onNameChanged: (String) -> Unit,
    onCategoryChanged: (String) -> Unit,
    onServingNameChanged: (String) -> Unit,
    onServingsCountChanged: (String) -> Unit,
    onCookedYieldChanged: (String) -> Unit,
    onIngredientFoodChanged: (String) -> Unit,
    onIngredientServingChoiceSelected: (String) -> Unit,
    onIngredientQuantityChanged: (String) -> Unit,
    onAddIngredientClick: () -> Unit,
    onEditRecipeClick: (String) -> Unit,
    onDuplicateRecipeClick: (String) -> Unit,
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
                Surface(color = MusFitTheme.colors.surfaceVariant, shape = MusFitTheme.shapes.small) {
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
                                    "${recipe.servings.formatNutritionDisplay()} servings",
                                    "${recipe.cookedYieldGrams.formatNutritionDisplay()} g yield",
                                    if (recipe.isFavorite) "Favorite" else null,
                                ).joinToString(" - "),
                                color = MusFitTheme.colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
                            OutlinedButton(onClick = { onFavoriteClick(recipe.id, !recipe.isFavorite) }) {
                                Text(if (recipe.isFavorite) "Starred" else "Star")
                            }
                            OutlinedButton(onClick = { onEditRecipeClick(recipe.id) }) {
                                Text("Edit")
                            }
                            OutlinedButton(onClick = { onDuplicateRecipeClick(recipe.id) }) {
                                Text("Copy")
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                state.recipeServingsCount,
                onServingsCountChanged,
                label = { Text("Servings") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                state.recipeCookedYieldGrams,
                onCookedYieldChanged,
                label = { Text("Cooked yield g") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = "${state.recipeServingGrams.ifBlank { "0" }} g per ${state.recipeServingName.ifBlank { "serving" }}",
            style = MaterialTheme.typography.bodySmall,
            color = MusFitTheme.colors.onSurfaceVariant,
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
        if (state.recipeIngredientServingChoices.isNotEmpty()) {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.recipeIngredientServingChoices.forEach { choice ->
                    FilterChip(
                        selected = state.recipeIngredientServingChoiceId == choice.id,
                        onClick = { onIngredientServingChoiceSelected(choice.id) },
                        label = { Text(choice.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                state.recipeIngredientQuantityGrams,
                onIngredientQuantityChanged,
                label = { Text("Amount") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onAddIngredientClick, modifier = Modifier.width(112.dp)) {
                Text("Add")
            }
        }
        state.recipeIngredients.forEach { ingredient ->
            Surface(color = MusFitTheme.colors.surfaceVariant, shape = MusFitTheme.shapes.small) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(ingredient.foodName, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = listOf(
                            "${ingredient.unitQuantity.formatNutritionDisplay()} ${ingredient.unitLabel}",
                            "${ingredient.quantityGrams.roundToInt()} g",
                        ).joinToString(" - "),
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                }
            }
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        Button(
            onClick = onSaveClick,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand),
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
internal fun MealTemplatesPanel(
    state: FoodUiState,
    onTemplateClick: (String) -> Unit,
    onEditClick: (String) -> Unit,
    onDuplicateClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onFavoriteClick: (String, Boolean) -> Unit,
    onNameChanged: (String) -> Unit,
    onMealTypeChanged: (String) -> Unit,
    onTemplateItemQuantityChanged: (Int, String) -> Unit,
    onTemplateItemRemoveClick: (Int) -> Unit,
    onTemplateItemFoodChanged: (String) -> Unit,
    onTemplateNewItemQuantityChanged: (String) -> Unit,
    onTemplateAddItemClick: () -> Unit,
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
        state.mealTemplateEditor?.let { editor ->
            Surface(color = MusFitTheme.colors.surfaceVariant, shape = MusFitTheme.shapes.small) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Edit template", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = editor.name,
                        onValueChange = onNameChanged,
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    MealTypeChips(
                        selectedMealType = editor.mealType,
                        mealDefinitions = state.mealDefinitions,
                        onMealChanged = onMealTypeChanged,
                    )
                    Text("Items", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    editor.items.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = item.quantityGrams,
                                onValueChange = { onTemplateItemQuantityChanged(index, it) },
                                label = { Text(item.foodName) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedButton(
                                onClick = { onTemplateItemRemoveClick(index) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.width(104.dp),
                            ) {
                                Text("Remove")
                            }
                        }
                    }
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.savedFoods.forEach { food ->
                            FilterChip(
                                selected = editor.newItemFoodId == food.id,
                                onClick = { onTemplateItemFoodChanged(food.id) },
                                label = { Text(food.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = editor.newItemQuantityGrams,
                            onValueChange = onTemplateNewItemQuantityChanged,
                            label = { Text("Amount g") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                        Button(onClick = onTemplateAddItemClick, modifier = Modifier.width(96.dp)) {
                            Text("Add")
                        }
                    }
                    Button(
                        onClick = onSaveEditClick,
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand),
                    ) {
                        Text(if (state.isSaving) "Saving" else "Save template")
                    }
                }
            }
        }
        if (state.mealTemplates.isEmpty()) {
            Text("No meal templates yet", color = MusFitTheme.colors.onSurfaceVariant)
        } else {
            state.mealTemplates.forEach { template ->
                Surface(color = MusFitTheme.colors.surfaceVariant, shape = MusFitTheme.shapes.small) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column {
                            Text(template.name, fontWeight = FontWeight.SemiBold)
                            Text(template.itemSummary, color = MusFitTheme.colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (template.isFavorite) {
                                Text("Favorite", color = MusFitTheme.colors.brand, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
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
