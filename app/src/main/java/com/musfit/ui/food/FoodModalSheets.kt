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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.draw.clipToBounds
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
        Text("Shopping list", style = MaterialTheme.typography.headlineSmall)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

        HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("Manual item")
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
            MusFitOutlinedButton(onClick = onAddManualClick, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
                Text("Add item")
            }
        }

        state.message?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MusFitTheme.colors.brand,
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
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SectionTitle(group.category)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        group.items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleSmall,
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
                            HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
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
    onBarcodeCompareClick: () -> Unit,
    onOpenFoodDetailClick: (String) -> Unit,
    onEditFoodClick: (String) -> Unit,
    onSaveOnlineFoodClick: (String) -> Unit,
    onImportStarterFoodsClick: () -> Unit,
    onNutritionLabelScanClick: () -> Unit,
    onMergeDuplicateFoodsClick: (String, List<String>) -> Unit,
    onFavoriteClick: (String, Boolean) -> Unit,
    onReportFoodClick: (String) -> Unit,
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

        MusFitOutlinedButton(onClick = onImportStarterFoodsClick, modifier = Modifier.fillMaxWidth()) {
            Text("Import starter foods")
        }

        MusFitOutlinedButton(onClick = onBarcodeCompareClick, modifier = Modifier.fillMaxWidth()) {
            Text("Compare barcodes")
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
                color = MusFitTheme.colors.brand,
            )
        }

        if (state.onlineFoodResults.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionTitle("Online results")
                Column(modifier = Modifier.fillMaxWidth()) {
                    state.onlineFoodResults.forEach { result ->
                        OnlineFoodResultRow(
                            result = result,
                            isSaving = state.isSaving,
                            onSaveClick = { onSaveOnlineFoodClick(result.barcode) },
                        )
                    }
                }
            }
        }

        if (state.duplicateFoodGroups.isNotEmpty()) {
            DuplicateFoodGroupsSection(
                duplicateGroups = state.duplicateFoodGroups,
                isSaving = state.isSaving,
                onMergeDuplicateFoodsClick = onMergeDuplicateFoodsClick,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionTitle("Saved foods")
            if (foods.isEmpty()) {
                Text(
                    text = if (state.foodDatabaseQuery.isBlank()) "No saved foods yet" else "No matching foods",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    foods.forEach { food ->
                        SavedFoodDatabaseRow(
                            food = food,
                            onDetailClick = { onOpenFoodDetailClick(food.id) },
                            onEditClick = { onEditFoodClick(food.id) },
                            onFavoriteClick = { onFavoriteClick(food.id, !food.isFavorite) },
                            onReportClick = { onReportFoodClick(food.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun FastingTimerPanel(
    state: FoodUiState,
    onProgramSelected: (String) -> Unit,
    onStartTimeChanged: (String) -> Unit,
    onCustomFastingChanged: (String) -> Unit,
    onCustomEatingChanged: (String) -> Unit,
    onApplyCustomClick: () -> Unit,
) {
    val timer = state.fastingTimer
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 640.dp)
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Fasting timer", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = timer.statusLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
        }

        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            timer.programs.forEach { program ->
                FilterChip(
                    selected = program.isSelected,
                    onClick = { onProgramSelected(program.id) },
                    label = { Text(program.title) },
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("Today")
            ProgressBar(progress = timer.progress.toFloat(), color = MusFitTheme.colors.brand)
            NutritionFactRow("Fast", timer.fastingWindowLabel, "Fasting window")
            NutritionFactRow("Eat", timer.eatingWindowLabel, "Eating window")
        }

        HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)

        OutlinedTextField(
            value = timer.fastingStartInput,
            onValueChange = onStartTimeChanged,
            label = { Text("Fast starts") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("Custom split")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = timer.customFastingHoursInput,
                    onValueChange = onCustomFastingChanged,
                    label = { Text("Fast h") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = timer.customEatingHoursInput,
                    onValueChange = onCustomEatingChanged,
                    label = { Text("Eat h") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }
            Button(
                onClick = onApplyCustomClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand),
            ) {
                Text("Apply custom")
            }
        }

        state.message?.let { message ->
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MusFitTheme.colors.brand)
        }
    }
}

@Composable
internal fun BarcodeComparisonPanel(
    state: FoodUiState,
    onBarcodeChanged: (BarcodeComparisonSide, String) -> Unit,
    onCompareClick: () -> Unit,
) {
    val comparison = state.barcodeComparison
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 640.dp)
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Barcode comparison", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Compare saved foods or Open Food Facts products per 100 g.",
                style = MaterialTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = comparison.leftBarcodeInput,
                onValueChange = { onBarcodeChanged(BarcodeComparisonSide.Left, it) },
                label = { Text("Left barcode") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = comparison.rightBarcodeInput,
                onValueChange = { onBarcodeChanged(BarcodeComparisonSide.Right, it) },
                label = { Text("Right barcode") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }

        Button(
            onClick = onCompareClick,
            enabled = !state.barcodeComparison.isLoading,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand),
        ) {
            Text(if (state.barcodeComparison.isLoading) "Comparing" else "Compare")
        }

        state.message?.let { message ->
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MusFitTheme.colors.brand)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BarcodeComparisonItemCard(
                title = "Left",
                item = comparison.leftItem,
                modifier = Modifier.weight(1f),
            )
            BarcodeComparisonItemCard(
                title = "Right",
                item = comparison.rightItem,
                modifier = Modifier.weight(1f),
            )
        }

        if (comparison.highlights.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionTitle("Per 100 g comparison")
                Column(modifier = Modifier.fillMaxWidth()) {
                    comparison.highlights.forEach { highlight ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(highlight.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(
                                text = "${highlight.leftValue} / ${highlight.rightValue}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MusFitTheme.colors.onSurfaceVariant,
                            )
                            Text(
                                text = when (highlight.winnerSide) {
                                    BarcodeComparisonSide.Left -> "Left"
                                    BarcodeComparisonSide.Right -> "Right"
                                    null -> "Even"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = MusFitTheme.colors.brand,
                            )
                        }
                        HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
                    }
                }
            }
        }
    }
}

@Composable
private fun BarcodeComparisonItemCard(
    title: String,
    item: BarcodeComparisonItemUiState?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MusFitTheme.colors.brand)
        if (item == null) {
            Text(
                text = "No product loaded",
                style = MaterialTheme.typography.bodySmall,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
        } else {
            Text(item.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                text = listOfNotNull(item.sourceLabel, item.brand, item.barcode).joinToString(" - "),
                style = MaterialTheme.typography.bodySmall,
                color = MusFitTheme.colors.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${item.caloriesPer100g.formatNutritionDisplay()} kcal - P ${item.proteinPer100g.formatNutritionDisplay()} - C ${item.carbsPer100g.formatNutritionDisplay()} - F ${item.fatPer100g.formatNutritionDisplay()}",
                style = MaterialTheme.typography.bodySmall,
                color = MusFitTheme.colors.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun OnlineFoodResultRow(
    result: OnlineFoodResultUiState,
    isSaving: Boolean,
    onSaveClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FoodThumb(imageUrl = result.imageUrl, fallback = Icons.Outlined.Restaurant)
            Column(modifier = Modifier.weight(1f)) {
                Text(result.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = listOfNotNull(result.brand, result.category, "${result.caloriesPer100g.roundToInt()} kcal / 100g").joinToString(" - "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            MusFitOutlinedButton(onClick = onSaveClick, enabled = !isSaving) {
                Text("Save")
            }
        }
        HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
    }
}

@Composable
private fun DuplicateFoodGroupsSection(
    duplicateGroups: List<FoodDuplicateGroupUiState>,
    isSaving: Boolean,
    onMergeDuplicateFoodsClick: (String, List<String>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionTitle("Potential duplicates")
        Column(modifier = Modifier.fillMaxWidth()) {
            duplicateGroups.forEach { group ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = group.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${group.reason} - ${group.duplicateFoodIds.size + 1} foods",
                            style = MaterialTheme.typography.bodySmall,
                            color = MusFitTheme.colors.onSurfaceVariant,
                        )
                    }
                    MusFitOutlinedButton(
                        onClick = { onMergeDuplicateFoodsClick(group.primaryFoodId, group.duplicateFoodIds) },
                        enabled = !isSaving,
                    ) {
                        Text("Merge")
                    }
                }
                HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
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
    onReportClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FoodThumb(imageUrl = food.imageUrl, fallback = Icons.Outlined.Restaurant)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(
                        food.trust.label,
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
                MusFitOutlinedButton(onClick = onFavoriteClick) {
                    Text(if (food.isFavorite) "Starred" else "Star")
                }
                MusFitOutlinedButton(onClick = onReportClick) {
                    Text("Report")
                }
                MusFitOutlinedButton(onClick = onDetailClick) {
                    Text("Detail")
                }
                MusFitOutlinedButton(onClick = onEditClick) {
                    Text("Edit")
                }
            }
        }
        HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
    }
}

@Composable
internal fun FoodDetailPanel(
    state: FoodUiState,
    onEditClick: () -> Unit,
    onLogClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onReportClick: () -> Unit,
    onCorrectClick: () -> Unit,
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
                Text(food.name, style = MaterialTheme.typography.headlineSmall)
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

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("Nutrition facts")
            NutritionFactRow("Calories", "${food.caloriesPer100g.roundToInt()} kcal", "per 100 g")
            NutritionFactRow("Protein", "${food.proteinPer100g.roundToInt()} g", "per 100 g")
            NutritionFactRow("Carbs", "${food.carbsPer100g.roundToInt()} g", "per 100 g")
            NutritionFactRow("Fat", "${food.fatPer100g.roundToInt()} g", "per 100 g")
            NutritionFactRow("Fiber", "${food.fiberPer100g.roundToInt()} g", "per 100 g")
            NutritionFactRow("Sugar", "${food.sugarPer100g.roundToInt()} g", "per 100 g")
            NutritionFactRow("Sat fat", "${food.saturatedFatPer100g.roundToInt()} g", "per 100 g")
            NutritionFactRow("Sodium", "${food.sodiumMgPer100g.roundToInt()} mg", "per 100 g")
        }

        HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("Trust and source")
            NutritionFactRow(food.trust.label, food.sourceLabel, food.trust.explanation)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MusFitOutlinedButton(onClick = onReportClick, modifier = Modifier.weight(1f)) {
                    Text(if (food.trust.isReported) "Reported" else "Report")
                }
                MusFitOutlinedButton(onClick = onCorrectClick, modifier = Modifier.weight(1f)) {
                    Text(food.trust.actionLabel)
                }
            }
        }

        if (food.servings.isNotEmpty()) {
            SectionTitle("Servings")
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
                Text(state.foodEntryActionVerb)
            }
            MusFitOutlinedButton(onClick = onFavoriteClick, modifier = Modifier.weight(1f)) {
                Text(if (food.isFavorite) "Unstar" else "Star")
            }
            MusFitOutlinedButton(onClick = onEditClick, modifier = Modifier.weight(1f)) {
                Text("Edit")
            }
        }

        state.message?.let { Text(it, color = MusFitTheme.colors.brand) }
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
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(unit, style = MaterialTheme.typography.bodySmall, color = MusFitTheme.colors.onSurfaceVariant)
        }
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MusFitTheme.colors.brandInk)
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
            mealDefinitions = state.visibleMealDefinitions,
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
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SectionTitle("Preview before saving")
            Text(
                text = "${editor.previewCaloriesKcal.roundToInt()} kcal",
                style = MaterialTheme.typography.headlineSmall,
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
                color = MusFitTheme.colors.brand,
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
            MusFitOutlinedButton(
                onClick = onMarkLoggedClick,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Mark logged")
            }
        }

        SectionTitle("Copy item")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.visibleMealDefinitions.forEach { choice ->
                MusFitOutlinedButton(
                    onClick = { onCopyToMealClick(choice.id) },
                    enabled = !state.isSaving,
                ) {
                    Text(choice.title)
                }
            }
            MusFitOutlinedButton(
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
    onToggleHidden: (String) -> Unit,
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
        Text("Meal settings", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Turn a meal off to hide it from the diary. Anything already logged there still counts toward your day.",
            style = MaterialTheme.typography.bodySmall,
            color = MusFitTheme.colors.onSurfaceVariant,
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            state.mealDefinitions.forEach { meal ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(meal.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            listOfNotNull(
                                if (meal.isDefault) "Default" else "Custom",
                                meal.timeLabel,
                                "Order ${meal.sortOrder}",
                                if (meal.isHidden) "Hidden" else null,
                            ).joinToString(" - "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MusFitTheme.colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Switch(
                        checked = !meal.isHidden,
                        onCheckedChange = { onToggleHidden(meal.id) },
                    )
                    MusFitOutlinedButton(onClick = { onEditClick(meal.id) }) {
                        Text("Edit")
                    }
                }
                HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(if (state.editingMealDefinitionId == null) "Add custom meal" else "Edit meal")
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

        state.message?.let { Text(it, color = MusFitTheme.colors.brand) }
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
    val editor = state.savedFoodEditor ?: return
    val isExistingFood = editor.id != null
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
            )
            Text(
                text = "Food database item",
                style = MaterialTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
        }

        OutlinedTextField(
            value = editor.name,
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
                value = editor.brand,
                onValueChange = onBrandChanged,
                label = { Text("Brand") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = editor.servingGrams,
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
                value = editor.servingName,
                onValueChange = onServingNameChanged,
                label = { Text("Serving name") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = editor.category,
                onValueChange = onCategoryChanged,
                label = { Text("Category") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }

        OutlinedTextField(
            value = editor.barcode,
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
            Text("Favorite", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Switch(checked = editor.isFavorite, onCheckedChange = onFavoriteChanged)
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
                color = MusFitTheme.colors.brand,
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
            MusFitOutlinedButton(
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
    val editor = state.savedFoodEditor ?: return
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
            )
            Text(
                text = "Review fields before saving",
                style = MaterialTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
        }

        MusFitOutlinedButton(
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Capture label photo")
        }

        OutlinedTextField(
            value = editor.name,
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
                value = editor.brand,
                onValueChange = onBrandChanged,
                label = { Text("Brand") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = editor.servingGrams,
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
                value = editor.servingName,
                onValueChange = onServingNameChanged,
                label = { Text("Serving") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = editor.category,
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
                color = MusFitTheme.colors.brand,
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
    val editor = state.savedFoodEditor ?: return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Per 100 g")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SmallNumberField(
                label = "Calories",
                value = editor.caloriesPer100g,
                onValueChange = onCaloriesChanged,
                modifier = Modifier.weight(1f),
            )
            SmallNumberField(
                label = "Protein",
                value = editor.proteinPer100g,
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
                value = editor.carbsPer100g,
                onValueChange = onCarbsChanged,
                modifier = Modifier.weight(1f),
            )
            SmallNumberField(
                label = "Fat",
                value = editor.fatPer100g,
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
                value = editor.fiberPer100g,
                onValueChange = onFiberChanged,
                modifier = Modifier.weight(1f),
            )
            SmallNumberField(
                label = "Sugar",
                value = editor.sugarPer100g,
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
                value = editor.saturatedFatPer100g,
                onValueChange = onSaturatedFatChanged,
                modifier = Modifier.weight(1f),
            )
            SmallNumberField(
                label = "Sodium mg",
                value = editor.sodiumMgPer100g,
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
                value = editor.potassiumMgPer100g,
                onValueChange = onPotassiumChanged,
                modifier = Modifier.weight(1f),
            )
            SmallNumberField(
                label = "Calcium mg",
                value = editor.calciumMgPer100g,
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
                value = editor.ironMgPer100g,
                onValueChange = onIronChanged,
                modifier = Modifier.weight(1f),
            )
            SmallNumberField(
                label = "Vit D mcg",
                value = editor.vitaminDMcgPer100g,
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
                value = editor.vitaminCMgPer100g,
                onValueChange = onVitaminCChanged,
                modifier = Modifier.weight(1f),
            )
            SmallNumberField(
                label = "Magnesium mg",
                value = editor.magnesiumMgPer100g,
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
    onProgramApply: (String) -> Unit,
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
        FoodProgramCatalog(
            programs = state.foodPrograms,
            isSaving = state.isSaving,
            onProgramApply = onProgramApply,
        )
        HorizontalDivider(color = MusFitTheme.colors.outline)
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
private fun FoodProgramCatalog(
    programs: List<FoodProgramUiState>,
    isSaving: Boolean,
    onProgramApply: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Programs",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MusFitTheme.colors.brandInk,
        )
        programs.forEach { program ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (program.isSelected) MusFitTheme.colors.brand.copy(alpha = 0.08f) else MusFitTheme.colors.surface,
                shape = MusFitTheme.shapes.small,
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = program.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MusFitTheme.colors.brandInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = program.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MusFitTheme.colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        MusFitOutlinedButton(
                            onClick = { onProgramApply(program.id) },
                            enabled = !isSaving && !program.isSelected,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(if (program.isSelected) "Active" else "Apply")
                        }
                    }
                    Text(
                        text = program.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = program.macroTargetsLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MusFitTheme.colors.brand,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Habits: ${program.suggestedHabits.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Plan: ${program.mealPlanningTip}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
internal fun RecipeBrowserScreen(
    state: FoodUiState,
    onCloseClick: () -> Unit,
    onForwardClick: () -> Unit,
    onHomeClick: () -> Unit,
    onPreviousDayClick: () -> Unit,
    onNextDayClick: () -> Unit,
    onTodayClick: () -> Unit,
    onMealChanged: (String) -> Unit,
    onServingsChanged: (String) -> Unit,
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
    onSearchQueryChanged: (String) -> Unit,
    onDiscoveryFilterChanged: (RecipeDiscoveryFilter) -> Unit,
    onDiscoveryItemClick: (String) -> Unit,
    onLogRecipeClick: (String) -> Unit,
    onPlanRecipeClick: (String) -> Unit,
    onReviewIdeaClick: (String) -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val isEditorPage = state.sheetMode == FoodSheetMode.RecipeEditor && state.recipeEditor != null
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MusFitTheme.colors.background)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RecipeBrowserToolbar(
            title = if (isEditorPage) "Recipe builder" else "Recipe browser",
            subtitle = if (isEditorPage) "Build, edit, and save" else "Discover, plan, and manage",
            forwardEnabled = !isEditorPage,
            onBackClick = {
                if (isEditorPage) {
                    onHomeClick()
                } else {
                    onCloseClick()
                }
            },
            onForwardClick = onForwardClick,
            onHomeClick = onHomeClick,
        )
        if (isEditorPage) {
            RecipeEditorPanel(
                state = state,
                onNameChanged = onNameChanged,
                onCategoryChanged = onCategoryChanged,
                onServingNameChanged = onServingNameChanged,
                onServingsCountChanged = onServingsCountChanged,
                onCookedYieldChanged = onCookedYieldChanged,
                onIngredientFoodChanged = onIngredientFoodChanged,
                onIngredientServingChoiceSelected = onIngredientServingChoiceSelected,
                onIngredientQuantityChanged = onIngredientQuantityChanged,
                onAddIngredientClick = onAddIngredientClick,
                onEditRecipeClick = onEditRecipeClick,
                onDuplicateRecipeClick = onDuplicateRecipeClick,
                onFavoriteClick = onFavoriteClick,
                onDiscoveryFilterChanged = onDiscoveryFilterChanged,
                onDiscoveryItemClick = onDiscoveryItemClick,
                onSaveClick = onSaveClick,
                onDeleteClick = onDeleteClick,
                modifier = Modifier.weight(1f),
                showBrowserSections = false,
                isFullScreen = true,
            )
        } else {
            RecipeBrowserHome(
                state = state,
                modifier = Modifier.weight(1f),
                onPreviousDayClick = onPreviousDayClick,
                onNextDayClick = onNextDayClick,
                onTodayClick = onTodayClick,
                onMealChanged = onMealChanged,
                onServingsChanged = onServingsChanged,
                onSearchQueryChanged = onSearchQueryChanged,
                onFilterChanged = onDiscoveryFilterChanged,
                onReviewIdeaClick = onReviewIdeaClick,
                onLogRecipeClick = onLogRecipeClick,
                onPlanRecipeClick = onPlanRecipeClick,
                onEditRecipeClick = onEditRecipeClick,
                onFavoriteClick = onFavoriteClick,
                onCreateClick = onForwardClick,
            )
        }
    }
}

@Composable
private fun RecipeBrowserToolbar(
    title: String,
    subtitle: String,
    forwardEnabled: Boolean,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    onHomeClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MusFitTheme.colors.onSurface)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MusFitTheme.colors.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MusFitTheme.colors.onSurfaceVariant)
        }
        IconButton(onClick = onForwardClick, enabled = forwardEnabled) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Create recipe",
                tint = if (forwardEnabled) MusFitTheme.colors.onSurface else MusFitTheme.colors.onSurfaceVariant.copy(alpha = 0.38f),
            )
        }
        IconButton(onClick = onHomeClick) {
            Icon(Icons.Filled.Home, contentDescription = "Recipe browser home", tint = MusFitTheme.colors.onSurface)
        }
    }
}

@Composable
private fun RecipeBrowserHome(
    state: FoodUiState,
    modifier: Modifier = Modifier,
    onPreviousDayClick: () -> Unit,
    onNextDayClick: () -> Unit,
    onTodayClick: () -> Unit,
    onMealChanged: (String) -> Unit,
    onServingsChanged: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onFilterChanged: (RecipeDiscoveryFilter) -> Unit,
    onReviewIdeaClick: (String) -> Unit,
    onLogRecipeClick: (String) -> Unit,
    onPlanRecipeClick: (String) -> Unit,
    onEditRecipeClick: (String) -> Unit,
    onFavoriteClick: (String, Boolean) -> Unit,
    onCreateClick: () -> Unit,
) {
    val lanes = recipeBrowserMealLanes(state.recipeDiscovery.visibleItems, state.visibleMealDefinitions)
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RecipeBrowserSearchBar(
                query = state.recipeDiscovery.query,
                onQueryChanged = onSearchQueryChanged,
            )
            RecipeBrowserTargetCard(
                state = state,
                onPreviousDayClick = onPreviousDayClick,
                onNextDayClick = onNextDayClick,
                onTodayClick = onTodayClick,
                onMealChanged = onMealChanged,
                onServingsChanged = onServingsChanged,
            )
            RecipeBrowserFilters(
                selectedFilter = state.recipeDiscovery.filter,
                onDiscoveryFilterChanged = onFilterChanged,
            )
            state.message?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .clipToBounds()
                .verticalScroll(rememberScrollState())
                .padding(top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (lanes.isEmpty()) {
                RecipeBrowserEmptyText("No recipes match this search yet.")
            } else {
                lanes.forEach { lane ->
                    RecipeBrowserLaneSection(
                        lane = lane,
                        state = state,
                        onLogRecipeClick = onLogRecipeClick,
                        onPlanRecipeClick = onPlanRecipeClick,
                        onReviewIdeaClick = onReviewIdeaClick,
                        onEditRecipeClick = onEditRecipeClick,
                        onFavoriteClick = onFavoriteClick,
                    )
                }
            }
            Button(
                onClick = onCreateClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand),
            ) {
                Text("Create recipe")
            }
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
    onDiscoveryFilterChanged: (RecipeDiscoveryFilter) -> Unit,
    onDiscoveryItemClick: (String) -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBrowserSections: Boolean = true,
    isFullScreen: Boolean = false,
) {
    val editor = state.recipeEditor ?: return
    val panelSizeModifier =
        if (isFullScreen) {
            Modifier.fillMaxSize()
        } else {
            Modifier.heightIn(max = 640.dp)
        }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(panelSizeModifier)
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(if (editor.editingRecipeId == null) "Recipe" else "Edit recipe", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (showBrowserSections && editor.editingRecipeId == null) {
            RecipeDiscoveryCatalog(
                state = state,
                onFilterChanged = onDiscoveryFilterChanged,
                onItemClick = onDiscoveryItemClick,
            )
        }
        if (showBrowserSections && state.recipes.isNotEmpty() && editor.editingRecipeId == null) {
            SavedRecipesSection(
                recipes = state.recipes,
                onEditRecipeClick = onEditRecipeClick,
                onDuplicateRecipeClick = onDuplicateRecipeClick,
                onFavoriteClick = onFavoriteClick,
            )
        }
        OutlinedTextField(editor.name, onNameChanged, label = { Text("Recipe name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(editor.category, onCategoryChanged, label = { Text("Category") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(editor.servingName, onServingNameChanged, label = { Text("Serving") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                editor.servingsCount,
                onServingsCountChanged,
                label = { Text("Servings") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                editor.cookedYieldGrams,
                onCookedYieldChanged,
                label = { Text("Cooked yield g") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = "${editor.servingGrams.ifBlank { "0" }} g per ${editor.servingName.ifBlank { "serving" }}",
            style = MaterialTheme.typography.bodySmall,
            color = MusFitTheme.colors.onSurfaceVariant,
        )
        Text("Ingredients", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.savedFoods.forEach { food ->
                FilterChip(
                    selected = editor.ingredientFoodId == food.id,
                    onClick = { onIngredientFoodChanged(food.id) },
                    label = { Text(food.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }
        }
        if (editor.ingredientServingChoices.isNotEmpty()) {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                editor.ingredientServingChoices.forEach { choice ->
                    FilterChip(
                        selected = editor.ingredientServingChoiceId == choice.id,
                        onClick = { onIngredientServingChoiceSelected(choice.id) },
                        label = { Text(choice.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                editor.ingredientQuantityGrams,
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
        editor.ingredients.forEach { ingredient ->
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
        if (editor.editingRecipeId != null) {
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
private fun SavedRecipesSection(
    recipes: List<RecipeUiState>,
    onEditRecipeClick: (String) -> Unit,
    onDuplicateRecipeClick: (String) -> Unit,
    onFavoriteClick: (String, Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Saved recipes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        recipes.forEach { recipe ->
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
                        MusFitOutlinedButton(onClick = { onFavoriteClick(recipe.id, !recipe.isFavorite) }) {
                            Text(if (recipe.isFavorite) "Starred" else "Star")
                        }
                        MusFitOutlinedButton(onClick = { onEditRecipeClick(recipe.id) }) {
                            Text("Edit")
                        }
                        MusFitOutlinedButton(onClick = { onDuplicateRecipeClick(recipe.id) }) {
                            Text("Copy")
                        }
                    }
                }
            }
        }
    }
}

private val RecipeDiscoveryFilter.label: String
    get() =
        when (this) {
            RecipeDiscoveryFilter.All -> "All"
            RecipeDiscoveryFilter.HighProtein -> "High protein"
            RecipeDiscoveryFilter.LowCarb -> "Low carb"
            RecipeDiscoveryFilter.Vegetarian -> "Vegetarian"
            RecipeDiscoveryFilter.Quick -> "Quick"
            RecipeDiscoveryFilter.Favorites -> "Favorites"
            RecipeDiscoveryFilter.Program -> "Program"
        }

internal data class RecipeBrowserSections(
    val savedRecipes: List<RecipeDiscoveryItemUiState>,
    val recipeIdeas: List<RecipeDiscoveryItemUiState>,
)

internal fun sectionRecipeBrowserItems(items: List<RecipeDiscoveryItemUiState>): RecipeBrowserSections =
    RecipeBrowserSections(
        savedRecipes = items.filter { it.isSavedRecipe && it.sourceRecipeId != null },
        recipeIdeas = items.filterNot { it.isSavedRecipe && it.sourceRecipeId != null },
    )

internal data class RecipeBrowserLane(
    val id: String,
    val title: String,
    val items: List<RecipeDiscoveryItemUiState>,
)

internal fun recipeBrowserMealLanes(
    items: List<RecipeDiscoveryItemUiState>,
    mealDefinitions: List<FoodMealDefinitionUiState>,
): List<RecipeBrowserLane> {
    val lanes =
        mealDefinitions
            .sortedBy { it.sortOrder }
            .mapNotNull { meal ->
                val laneItems = items.filter { meal.id in it.mealTypeIds }
                if (laneItems.isEmpty()) {
                    null
                } else {
                    RecipeBrowserLane(id = meal.id, title = meal.title, items = laneItems)
                }
            }
    return lanes.ifEmpty {
        if (items.isEmpty()) emptyList() else listOf(RecipeBrowserLane("all", "All recipes", items))
    }
}

@Composable
private fun RecipeBrowserSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = null, tint = MusFitTheme.colors.onSurfaceVariant)
        },
        placeholder = { Text("Find recipes") },
    )
}

@Composable
private fun RecipeBrowserTargetCard(
    state: FoodUiState,
    onPreviousDayClick: () -> Unit,
    onNextDayClick: () -> Unit,
    onTodayClick: () -> Unit,
    onMealChanged: (String) -> Unit,
    onServingsChanged: (String) -> Unit,
) {
    Surface(color = MusFitTheme.colors.surfaceVariant, shape = MusFitTheme.shapes.small) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Target", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onPreviousDayClick) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous day")
                }
                Text(
                    text = state.recipeBrowserDate.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MusFitTheme.colors.onSurface,
                )
                TextButton(onClick = onTodayClick) {
                    Text("Today")
                }
                IconButton(onClick = onNextDayClick) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Next day")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    MealTypeChips(
                        selectedMealType = state.recipeBrowserMealType,
                        mealDefinitions = state.visibleMealDefinitions,
                        onMealChanged = onMealChanged,
                    )
                }
                OutlinedTextField(
                    value = state.recipeServingsToLog,
                    onValueChange = onServingsChanged,
                    label = { Text("Serv") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.width(104.dp),
                )
            }
        }
    }
}

@Composable
private fun RecipeBrowserFilters(
    selectedFilter: RecipeDiscoveryFilter,
    onDiscoveryFilterChanged: (RecipeDiscoveryFilter) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RecipeDiscoveryFilter.entries.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onDiscoveryFilterChanged(filter) },
                label = { Text(filter.label) },
            )
        }
    }
}

@Composable
private fun RecipeBrowserLaneSection(
    lane: RecipeBrowserLane,
    state: FoodUiState,
    onLogRecipeClick: (String) -> Unit,
    onPlanRecipeClick: (String) -> Unit,
    onReviewIdeaClick: (String) -> Unit,
    onEditRecipeClick: (String) -> Unit,
    onFavoriteClick: (String, Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = lane.title.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MusFitTheme.colors.onSurface,
            )
            Text(
                text = "${lane.items.size} meals",
                style = MaterialTheme.typography.labelMedium,
                color = MusFitTheme.colors.brand,
            )
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            lane.items.forEach { item ->
                RecipeBrowserCatalogCard(
                    item = item,
                    state = state,
                    onLogRecipeClick = onLogRecipeClick,
                    onPlanRecipeClick = onPlanRecipeClick,
                    onReviewIdeaClick = onReviewIdeaClick,
                    onEditRecipeClick = onEditRecipeClick,
                    onFavoriteClick = onFavoriteClick,
                )
            }
        }
    }
}

@Composable
private fun RecipeBrowserCatalogCard(
    item: RecipeDiscoveryItemUiState,
    state: FoodUiState,
    onLogRecipeClick: (String) -> Unit,
    onPlanRecipeClick: (String) -> Unit,
    onReviewIdeaClick: (String) -> Unit,
    onEditRecipeClick: (String) -> Unit,
    onFavoriteClick: (String, Boolean) -> Unit,
) {
    val sourceRecipeId = item.sourceRecipeId
    Surface(
        color = MusFitTheme.colors.surfaceVariant,
        shape = MusFitTheme.shapes.small,
        modifier = Modifier.width(176.dp),
    ) {
        Column {
            Box {
                RecipeBrowserThumbnail(
                    key = item.thumbnailKey,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(108.dp),
                )
                Surface(
                    color = MusFitTheme.colors.surface.copy(alpha = 0.88f),
                    shape = CircleShape,
                    modifier = Modifier.padding(8.dp),
                ) {
                    Text(
                        text = if (item.isSavedRecipe) "Saved" else "Idea",
                        style = MaterialTheme.typography.labelSmall,
                        color = MusFitTheme.colors.brand,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MusFitTheme.colors.onSurface,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${item.caloriesKcal.formatNutritionDisplay()} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.tagLabels.isNotEmpty()) {
                    Text(
                        text = item.tagLabels.take(2).joinToString(" - "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MusFitTheme.colors.brand,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (sourceRecipeId != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        MusFitOutlinedButton(
                            onClick = { onLogRecipeClick(sourceRecipeId) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        ) {
                            Text("Log", maxLines = 1)
                        }
                        Button(
                            onClick = { onPlanRecipeClick(sourceRecipeId) },
                            enabled = !state.isSaving,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        ) {
                            Text(if (state.isSaving) "Saving" else "Plan", maxLines = 1)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { onEditRecipeClick(sourceRecipeId) }, modifier = Modifier.weight(1f)) {
                            Text("Edit", maxLines = 1)
                        }
                        TextButton(onClick = { onFavoriteClick(sourceRecipeId, !item.isFavorite) }, modifier = Modifier.weight(1f)) {
                            Text(if (item.isFavorite) "Starred" else "Star", maxLines = 1)
                        }
                    }
                } else {
                    Button(
                        onClick = { onReviewIdeaClick(item.id) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Text("Review", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeBrowserThumbnail(
    key: String,
    modifier: Modifier = Modifier,
) {
    val palette =
        when (key) {
            "salmon-plate" -> listOf(Color(0xFFE8875F), Color(0xFFF3D6A2), Color(0xFF6F9B73))
            "chicken-bowl" -> listOf(Color(0xFFD4A253), Color(0xFFF0D98F), Color(0xFF2F8B5B))
            "chickpea-bowl" -> listOf(Color(0xFFD6B45B), Color(0xFFF1E0A3), Color(0xFF7BA768))
            "breakfast-bowl", "overnight-oats" -> listOf(Color(0xFFE6C7A2), Color(0xFFF3E5CF), Color(0xFF8C6B52))
            "muffins" -> listOf(Color(0xFFB87948), Color(0xFFE8C28B), Color(0xFF7A4A2A))
            "kale-salad" -> listOf(Color(0xFF4E9A62), Color(0xFFDCE9B0), Color(0xFFB64F42))
            "bean-dip" -> listOf(Color(0xFF79A96B), Color(0xFFF2D18B), Color(0xFF5C8A4D))
            "soup" -> listOf(Color(0xFFC8643D), Color(0xFFF0B45B), Color(0xFF7B3E2F))
            "snack-box" -> listOf(Color(0xFF9E7B5F), Color(0xFFEAD8B8), Color(0xFF4D8A72))
            else -> listOf(Color(0xFFB98A5B), Color(0xFFE8D3A5), Color(0xFF6B9B70))
        }
    Canvas(modifier = modifier.clip(MusFitTheme.shapes.small)) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(palette[1], palette[0]),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            ),
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.78f),
            radius = size.minDimension * 0.34f,
            center = Offset(size.width * 0.5f, size.height * 0.55f),
        )
        drawCircle(
            color = palette[2].copy(alpha = 0.9f),
            radius = size.minDimension * 0.18f,
            center = Offset(size.width * 0.42f, size.height * 0.52f),
        )
        drawCircle(
            color = palette[0].copy(alpha = 0.9f),
            radius = size.minDimension * 0.14f,
            center = Offset(size.width * 0.58f, size.height * 0.48f),
        )
        drawCircle(
            color = palette[1].copy(alpha = 0.95f),
            radius = size.minDimension * 0.12f,
            center = Offset(size.width * 0.54f, size.height * 0.64f),
        )
        drawRect(
            color = Color.White.copy(alpha = 0.28f),
            topLeft = Offset(size.width * 0.1f, size.height * 0.08f),
            size = Size(size.width * 0.8f, size.height * 0.12f),
        )
    }
}

@Composable
private fun RecipeBrowserSection(
    title: String,
    helper: String,
    emptyText: String,
    items: List<RecipeDiscoveryItemUiState>,
    state: FoodUiState,
    onPlanRecipeClick: (String) -> Unit,
    onEditRecipeClick: (String) -> Unit,
    onFavoriteClick: (String, Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RecipeBrowserSectionHeader(title = title, helper = helper)
        if (items.isEmpty()) {
            RecipeBrowserEmptyText(emptyText)
        } else {
            items.forEach { item ->
                val sourceRecipeId = item.sourceRecipeId ?: return@forEach
                RecipeBrowserItemCard(item = item) {
                    Column(
                        modifier = Modifier.width(150.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.End,
                    ) {
                        MusFitOutlinedButton(
                            onClick = { onFavoriteClick(sourceRecipeId, !item.isFavorite) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (item.isFavorite) "Starred" else "Star", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        MusFitOutlinedButton(
                            onClick = { onEditRecipeClick(sourceRecipeId) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Edit")
                        }
                        Button(
                            onClick = { onPlanRecipeClick(sourceRecipeId) },
                            enabled = !state.isSaving,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand),
                        ) {
                            Text(if (state.isSaving) "Saving" else "Plan")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeBrowserIdeasSection(
    title: String,
    helper: String,
    emptyText: String,
    items: List<RecipeDiscoveryItemUiState>,
    onReviewIdeaClick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RecipeBrowserSectionHeader(title = title, helper = helper)
        if (items.isEmpty()) {
            RecipeBrowserEmptyText(emptyText)
        } else {
            items.forEach { item ->
                RecipeBrowserItemCard(item = item) {
                    Button(
                        onClick = { onReviewIdeaClick(item.id) },
                        modifier = Modifier.width(150.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand),
                    ) {
                        Text("Review recipe", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeBrowserSectionHeader(
    title: String,
    helper: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MusFitTheme.colors.onSurface,
        )
        Text(
            text = helper,
            style = MaterialTheme.typography.bodySmall,
            color = MusFitTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun RecipeBrowserEmptyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MusFitTheme.colors.onSurfaceVariant,
    )
}

@Composable
private fun RecipeBrowserItemCard(
    item: RecipeDiscoveryItemUiState,
    action: @Composable () -> Unit,
) {
    Surface(color = MusFitTheme.colors.surfaceVariant, shape = MusFitTheme.shapes.small) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${item.caloriesKcal.formatNutritionDisplay()} kcal - P ${item.proteinGrams.formatNutritionDisplay()} - C ${item.carbsGrams.formatNutritionDisplay()} - F ${item.fatGrams.formatNutritionDisplay()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.tagLabels.isNotEmpty()) {
                    Text(
                        text = item.tagLabels.joinToString(" - "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MusFitTheme.colors.brand,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            action()
        }
    }
}

@Composable
private fun RecipeDiscoveryCatalog(
    state: FoodUiState,
    onFilterChanged: (RecipeDiscoveryFilter) -> Unit,
    onItemClick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Recipe discovery", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RecipeDiscoveryFilter.entries.forEach { filter ->
                FilterChip(
                    selected = state.recipeDiscovery.filter == filter,
                    onClick = { onFilterChanged(filter) },
                    label = { Text(filter.label) },
                )
            }
        }
        if (state.recipeDiscovery.visibleItems.isEmpty()) {
            Text(
                text = "No recipes match this filter yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
        } else {
            state.recipeDiscovery.visibleItems.take(8).forEach { item ->
                Surface(color = MusFitTheme.colors.surfaceVariant, shape = MusFitTheme.shapes.small) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                text = item.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MusFitTheme.colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${item.caloriesKcal.formatNutritionDisplay()} kcal - P ${item.proteinGrams.formatNutritionDisplay()} - C ${item.carbsGrams.formatNutritionDisplay()} - F ${item.fatGrams.formatNutritionDisplay()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MusFitTheme.colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (item.tagLabels.isNotEmpty()) {
                                Text(
                                    text = item.tagLabels.joinToString(" - "),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MusFitTheme.colors.brand,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Button(onClick = { onItemClick(item.id) }, modifier = Modifier.width(96.dp)) {
                            Text(if (item.isSavedRecipe) state.foodEntryActionVerb else "Use")
                        }
                    }
                }
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
        Text("Meal templates", style = MaterialTheme.typography.headlineSmall)
        state.mealTemplateEditor?.let { editor ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Edit template")
                OutlinedTextField(
                    value = editor.name,
                    onValueChange = onNameChanged,
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                MealTypeChips(
                    selectedMealType = editor.mealType,
                    mealDefinitions = state.visibleMealDefinitions,
                    onMealChanged = onMealTypeChanged,
                )
                Text("Items", style = MaterialTheme.typography.titleSmall)
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
            HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
        }
        if (state.mealTemplates.isEmpty()) {
            Text("No meal templates yet", color = MusFitTheme.colors.onSurfaceVariant)
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                state.mealTemplates.forEach { template ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column {
                            Text(template.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                template.itemSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MusFitTheme.colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (template.isFavorite) {
                                Text("Favorite", color = MusFitTheme.colors.brand, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MusFitOutlinedButton(onClick = { onTemplateClick(template.id) }) {
                                Text(state.foodEntryActionVerb)
                            }
                            MusFitOutlinedButton(onClick = { onFavoriteClick(template.id, !template.isFavorite) }) {
                                Text(if (template.isFavorite) "Starred" else "Star")
                            }
                            MusFitOutlinedButton(onClick = { onEditClick(template.id) }) {
                                Text("Edit")
                            }
                            MusFitOutlinedButton(onClick = { onDuplicateClick(template.id) }) {
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
                    HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
                }
            }
        }
        state.message?.let { Text(it, color = MusFitTheme.colors.brand) }
    }
}
