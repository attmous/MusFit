@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.musfit.ui.food

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RiceBowl
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DinnerDining
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LunchDining
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.musfit.data.repository.FoodGoalMode
import com.musfit.domain.model.FoodNutrition
import com.musfit.domain.nutrition.NutritionCalculator
import com.musfit.ui.components.ExpressiveBadge
import com.musfit.ui.components.HairlineDetailRow
import com.musfit.ui.components.HeroNumberMediumStyle
import com.musfit.ui.components.InnerScreenHeader
import com.musfit.ui.components.PillButton
import com.musfit.ui.components.SectionOverline
import com.musfit.ui.components.StepperCircleButton
import com.musfit.ui.components.TonalHeaderIconButton
import com.musfit.ui.components.TonalIconSquare
import com.musfit.ui.components.WavyProgressBar
import com.musfit.ui.components.expressiveBadgeShapeFor
import com.musfit.ui.components.gridGroupShape
import com.musfit.ui.components.groupedShape
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.NeutralOutline
import com.musfit.ui.theme.NeutralOutlineDark
import com.musfit.ui.theme.TabAccentRole
import com.musfit.ui.theme.tabAccentFor
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed

private fun LazyListScope.verticalGap(key: String, height: Dp) {
    item(key = key, contentType = "vertical-gap") {
        Spacer(modifier = Modifier.height(height))
    }
}

private fun LazyGridScope.verticalGap(key: String, height: Dp) {
    item(
        key = key,
        span = { GridItemSpan(maxLineSpan) },
        contentType = "vertical-gap",
    ) {
        Spacer(modifier = Modifier.height(height))
    }
}

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
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 640.dp),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 28.dp),
    ) {
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Food database", style = MaterialTheme.typography.headlineSmall)
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
        }
        verticalGap("gap-after-header", 14.dp)

        item(key = "import") {
            MusFitOutlinedButton(onClick = onImportStarterFoodsClick, modifier = Modifier.fillMaxWidth()) {
                Text("Import starter foods")
            }
        }
        verticalGap("gap-after-import", 14.dp)

        item(key = "compare") {
            MusFitOutlinedButton(onClick = onBarcodeCompareClick, modifier = Modifier.fillMaxWidth()) {
                Text("Compare barcodes")
            }
        }
        verticalGap("gap-after-compare", 14.dp)

        item(key = "search") {
            OutlinedTextField(
                value = state.foodDatabaseQuery,
                onValueChange = onSearchChanged,
                label = { Text("Search foods") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        verticalGap("gap-after-search", 14.dp)

        item(key = "online-search") {
            Button(
                onClick = onSearchOnlineClick,
                enabled = !state.isSearchingFoods && !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand),
            ) {
                Text(if (state.isSearchingFoods) "Searching" else "Search online foods")
            }
        }
        verticalGap("gap-after-online-search", 14.dp)

        state.message?.let { message ->
            item(key = "message") {
                Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MusFitTheme.colors.brand)
            }
            verticalGap("gap-after-message", 14.dp)
        }

        if (state.onlineFoodResults.isNotEmpty()) {
            item(key = "online-heading") { SectionTitle("Online results") }
            verticalGap("gap-after-online-heading", 4.dp)
            items(
                items = state.onlineFoodResults,
                key = { "online-${it.barcode}" },
                contentType = { "online-food" },
            ) { result ->
                OnlineFoodResultRow(
                    result = result,
                    isSaving = state.isSaving,
                    onSaveClick = { onSaveOnlineFoodClick(result.barcode) },
                )
            }
            verticalGap("gap-after-online-results", 14.dp)
        }

        if (state.duplicateFoodGroups.isNotEmpty()) {
            item(key = "duplicates-heading") { SectionTitle("Potential duplicates") }
            verticalGap("gap-after-duplicates-heading", 4.dp)
            items(
                items = state.duplicateFoodGroups,
                key = { "duplicate-${it.primaryFoodId}" },
                contentType = { "duplicate-food-group" },
            ) { group ->
                DuplicateFoodGroupRow(
                    group = group,
                    isSaving = state.isSaving,
                    onMergeDuplicateFoodsClick = onMergeDuplicateFoodsClick,
                )
            }
            verticalGap("gap-after-duplicates", 14.dp)
        }

        item(key = "saved-heading") { SectionTitle("Saved foods") }
        verticalGap("gap-after-saved-heading", 4.dp)
        if (foods.isEmpty()) {
            item(key = "saved-empty") {
                Text(
                    text = if (state.foodDatabaseQuery.isBlank()) "No saved foods yet" else "No matching foods",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }
        } else {
            items(
                items = foods,
                key = SavedFoodUiState::id,
                contentType = { "saved-food" },
            ) { food ->
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
private fun DuplicateFoodGroupRow(
    group: FoodDuplicateGroupUiState,
    isSaving: Boolean,
    onMergeDuplicateFoodsClick: (String, List<String>) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
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

@Composable
private fun SavedFoodDatabaseRow(
    food: SavedFoodUiState,
    onDetailClick: () -> Unit,
    onEditClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onReportClick: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("food-database-row-${food.id}"),
    ) {
        val useCompactActions = maxWidth < 560.dp
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = if (useCompactActions) 2.dp else 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FoodThumb(imageUrl = food.imageUrl, fallback = Icons.Outlined.Restaurant)
                SavedFoodDatabaseSummary(food = food, modifier = Modifier.weight(1f))
                if (!useCompactActions) {
                    SavedFoodDatabaseTextActions(
                        food = food,
                        onFavoriteClick = onFavoriteClick,
                        onReportClick = onReportClick,
                        onDetailClick = onDetailClick,
                        onEditClick = onEditClick,
                    )
                }
            }
            if (useCompactActions) {
                SavedFoodDatabaseIconActions(
                    food = food,
                    onFavoriteClick = onFavoriteClick,
                    onReportClick = onReportClick,
                    onDetailClick = onDetailClick,
                    onEditClick = onEditClick,
                )
            }
            HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
        }
    }
}

@Composable
private fun SavedFoodDatabaseSummary(food: SavedFoodUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
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
}

@Composable
private fun SavedFoodDatabaseTextActions(
    food: SavedFoodUiState,
    onFavoriteClick: () -> Unit,
    onReportClick: () -> Unit,
    onDetailClick: () -> Unit,
    onEditClick: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        MusFitOutlinedButton(onClick = onFavoriteClick) { Text(if (food.isFavorite) "Starred" else "Star") }
        MusFitOutlinedButton(onClick = onReportClick) { Text("Report") }
        MusFitOutlinedButton(onClick = onDetailClick) { Text("Detail") }
        MusFitOutlinedButton(onClick = onEditClick) { Text("Edit") }
    }
}

@Composable
private fun SavedFoodDatabaseIconActions(
    food: SavedFoodUiState,
    onFavoriteClick: () -> Unit,
    onReportClick: () -> Unit,
    onDetailClick: () -> Unit,
    onEditClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onFavoriteClick,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = if (food.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (food.isFavorite) "Remove ${food.name} from favorites" else "Add ${food.name} to favorites",
            )
        }
        IconButton(
            onClick = onReportClick,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(Icons.Outlined.ErrorOutline, contentDescription = "Report ${food.name}")
        }
        IconButton(
            onClick = onDetailClick,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(Icons.Outlined.ChevronRight, contentDescription = "View ${food.name} details")
        }
        IconButton(
            onClick = onEditClick,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(Icons.Outlined.Edit, contentDescription = "Edit ${food.name}")
        }
    }
}

// ---------------------------------------------------------------------------
// Pure presentation helpers for the Turn 9 restyle (unit-tested).
// ---------------------------------------------------------------------------

/** EU labeling convention: salt g = sodium mg × 2.5 / 1000. */
internal fun saltGramsFromSodiumMg(sodiumMg: Double): Double = sodiumMg * 2.5 / 1000.0

/** Steps a free-text gram amount by [deltaGrams], clamped to at least [minGrams]. */
internal fun steppedGramsInput(current: String, deltaGrams: Double, minGrams: Double = 1.0): String {
    val value = current.toDoubleOrNull() ?: 0.0
    return (value + deltaGrams).coerceAtLeast(minGrams).formatNutritionDisplay()
}

/** Steps a free-text kcal amount by [deltaKcal], clamped to at least 0. */
internal fun steppedCaloriesInput(current: String, deltaKcal: Int): String {
    val value = current.toDoubleOrNull() ?: 0.0
    return (value + deltaKcal).coerceAtLeast(0.0).roundToInt().toString()
}

/** "% of day" chip label, or null when no positive day budget exists. */
internal fun percentOfDayLabel(amountKcal: Double, dayBudgetKcal: Double): String? = if (dayBudgetKcal > 0.0) "${(amountKcal / dayBudgetKcal * 100).roundToInt()}% of day" else null

/**
 * Wavy-bar fill for a macro amount: fraction of the daily gram target when one
 * is set; otherwise capped against a 100 g reference so the bar stays honest.
 */
internal fun macroFillFraction(amountGrams: Double, dayGoalGrams: Double): Float = if (dayGoalGrams > 0.0) {
    (amountGrams / dayGoalGrams).toFloat().coerceIn(0f, 1f)
} else {
    (amountGrams / 100.0).toFloat().coerceIn(0f, 1f)
}

/**
 * Macro split (carbs %, protein %, fat %) of the calories the macro gram
 * inputs account for (4/4/9 kcal per g). Sums to 100 when any macro is set;
 * (0, 0, 0) when none are. [caloriesInput] is accepted per the screen contract
 * but the split normalizes over macro-derived kcal so percentages always
 * describe the split itself.
 */
internal fun macroSplitPercents(
    @Suppress("UNUSED_PARAMETER") caloriesInput: String,
    carbs: String,
    protein: String,
    fat: String,
): Triple<Int, Int, Int> {
    val carbsKcal = (carbs.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0) * 4.0
    val proteinKcal = (protein.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0) * 4.0
    val fatKcal = (fat.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0) * 9.0
    val total = carbsKcal + proteinKcal + fatKcal
    if (total <= 0.0) return Triple(0, 0, 0)
    val carbsPct = (carbsKcal / total * 100).roundToInt()
    val proteinPct = (proteinKcal / total * 100).roundToInt()
    val fatPct = (100 - carbsPct - proteinPct).coerceAtLeast(0)
    return Triple(carbsPct, proteinPct, fatPct)
}

/** The meal the add/detail flows currently target, lowercased for pill copy. */
private fun FoodUiState.detailTargetMealLabel(): String = (visibleMealDefinitions.firstOrNull { it.id == mealType }?.title ?: selectedMealTitle).lowercase()

@Composable
internal fun FoodDetailPanel(
    state: FoodUiState,
    onEditClick: () -> Unit,
    onLogClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onReportClick: () -> Unit,
    onCorrectClick: () -> Unit,
    onQuantityChanged: (String) -> Unit,
    onServingSelected: (String, Double) -> Unit,
) {
    val food = state.selectedSavedFoodDetail
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 680.dp)
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 18.dp, end = 18.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (food == null) {
            Text("Food not found", style = MusFitTheme.typography.titleMedium)
            return@Column
        }
        val grams = state.savedFoodQuantityGrams.toDoubleOrNull() ?: food.defaultServingGrams
        val amount = NutritionCalculator.nutritionForAmount(
            FoodNutrition(
                caloriesKcal = food.caloriesPer100g,
                proteinGrams = food.proteinPer100g,
                carbsGrams = food.carbsPer100g,
                fatGrams = food.fatPer100g,
            ),
            grams,
        )

        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FoodDetailHeader(food = food, onFavoriteClick = onFavoriteClick)
            FoodDetailTrustRow(food = food, onReportClick = onReportClick, onCorrectClick = onCorrectClick)
            FoodDetailAmountCard(
                food = food,
                quantityInput = state.savedFoodQuantityGrams,
                onQuantityChanged = onQuantityChanged,
                onServingSelected = onServingSelected,
            )
            FoodDetailPreviewHero(
                food = food,
                grams = grams,
                amountKcal = amount.caloriesKcal,
                dayBudgetKcal = state.effectiveCalorieBudgetKcal,
            )
            FoodDetailMacrosCard(amount = amount, state = state)
            FoodDetailDetailsList(food = food, grams = grams)
            state.message?.let {
                Text(it, style = MusFitTheme.typography.bodyMedium, color = MusFitTheme.colors.brand)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            TonalIconSquare(
                icon = Icons.Outlined.Edit,
                contentDescription = "Edit food",
                onClick = onEditClick,
            )
            PillButton(
                text = "${state.foodEntryActionVerb} to ${state.detailTargetMealLabel()} · ${amount.caloriesKcal.roundToInt()} kcal",
                onClick = onLogClick,
                icon = Icons.Filled.Add,
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FoodDetailHeader(food: SavedFoodUiState, onFavoriteClick: () -> Unit) {
    val accent = tabAccentFor(TabAccentRole.Food)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (!food.imageUrl.isNullOrBlank()) {
            FoodThumb(imageUrl = food.imageUrl, fallback = Icons.Filled.RiceBowl, size = 52.dp)
        } else {
            ExpressiveBadge(
                icon = Icons.Filled.RiceBowl,
                shape = expressiveBadgeShapeFor(0),
                containerColor = accent.container,
                contentColor = accent.onContainerVariant,
                size = 52.dp,
                iconSize = 24.dp,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = food.name,
                style = MusFitTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.W800,
                    letterSpacing = (-0.3).sp,
                ),
                color = MusFitTheme.colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(food.brand, food.category).joinToString(" · ").ifBlank { food.sourceLabel },
                style = MusFitTheme.typography.bodySmall,
                color = MusFitTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onFavoriteClick) {
            Icon(
                imageVector = if (food.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (food.isFavorite) "Remove from favorites" else "Add to favorites",
                tint = if (food.isFavorite) MusFitTheme.colors.macroProtein else MusFitTheme.colors.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun FoodDetailTrustRow(
    food: SavedFoodUiState,
    onReportClick: () -> Unit,
    onCorrectClick: () -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        when (food.trust.level) {
            FoodTrustLevel.Imported ->
                FoodTrustChip(Icons.Outlined.Verified, MusFitTheme.colors.brand, food.sourceLabel)

            FoodTrustLevel.Manual ->
                FoodTrustChip(Icons.Outlined.Edit, MusFitTheme.colors.onSurfaceVariant, "Edited by you")

            FoodTrustLevel.NeedsReview ->
                FoodTrustChip(Icons.Outlined.ErrorOutline, MusFitTheme.colors.warning, food.trust.label)
        }
        TrustActionChip(text = food.trust.actionLabel, onClick = onCorrectClick)
        TrustActionChip(
            text = if (food.trust.isReported) "Reported" else "Report",
            onClick = onReportClick,
            enabled = !food.trust.isReported,
        )
    }
}

@Composable
private fun TrustActionChip(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(99.dp),
        color = MusFitTheme.colors.trustChip,
        contentColor = MusFitTheme.colors.onTrustChip,
    ) {
        Text(
            text = text,
            style = MusFitTheme.typography.labelSmall.copy(fontSize = 11.5.sp, letterSpacing = 0.sp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun FoodDetailAmountCard(
    food: SavedFoodUiState,
    quantityInput: String,
    onQuantityChanged: (String) -> Unit,
    onServingSelected: (String, Double) -> Unit,
) {
    Surface(color = MusFitTheme.colors.surface, shape = RoundedCornerShape(24.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                StepperCircleButton(
                    icon = Icons.Outlined.Remove,
                    contentDescription = "Remove 10 grams",
                    onClick = { onQuantityChanged(steppedGramsInput(quantityInput, -10.0)) },
                )
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = quantityInput.toDoubleOrNull()?.formatNutritionDisplay() ?: quantityInput,
                        style = MusFitTheme.typography.displaySmall.copy(
                            fontSize = 34.sp,
                            lineHeight = 36.sp,
                            fontWeight = FontWeight.W800,
                            letterSpacing = (-0.8).sp,
                        ),
                        color = MusFitTheme.colors.onSurface,
                        maxLines = 1,
                    )
                    Text(
                        text = "g",
                        style = MusFitTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.W600),
                        color = MusFitTheme.colors.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                    )
                }
                StepperCircleButton(
                    icon = Icons.Outlined.Add,
                    contentDescription = "Add 10 grams",
                    filled = true,
                    onClick = { onQuantityChanged(steppedGramsInput(quantityInput, 10.0)) },
                )
            }
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val maxItemsPerRow = when {
                    maxWidth < 280.dp -> 1
                    maxWidth < 440.dp -> 2
                    else -> Int.MAX_VALUE
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    maxItemsInEachRow = maxItemsPerRow,
                ) {
                    val matchesServing = food.servings.any { quantityInput == it.grams.formatNutritionDisplay() }
                    SelectableChip(
                        text = "grams",
                        selected = !matchesServing,
                        onClick = { onQuantityChanged(food.defaultServingGrams.formatNutritionDisplay()) },
                        unselectedContainer = MusFitTheme.colors.surfaceVariant,
                    )
                    food.servings.forEach { serving ->
                        SelectableChip(
                            text = "${serving.label} · ${serving.grams.formatNutritionDisplay()} g",
                            selected = quantityInput == serving.grams.formatNutritionDisplay(),
                            onClick = { onServingSelected(food.id, serving.grams) },
                            unselectedContainer = MusFitTheme.colors.surfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodDetailPreviewHero(
    food: SavedFoodUiState,
    grams: Double,
    amountKcal: Double,
    dayBudgetKcal: Double,
) {
    val accent = tabAccentFor(TabAccentRole.Food)
    Surface(color = accent.container, shape = RoundedCornerShape(28.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${amountKcal.roundToInt()}",
                        style = MusFitTheme.typography.displaySmall.copy(
                            fontSize = 36.sp,
                            lineHeight = 38.sp,
                            fontWeight = FontWeight.W800,
                            letterSpacing = (-1).sp,
                        ),
                        color = accent.onContainer,
                    )
                    Text(
                        text = "kcal",
                        style = MusFitTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.W600),
                        color = accent.onContainerVariant,
                        modifier = Modifier.padding(start = 5.dp, bottom = 5.dp),
                    )
                }
                Text(
                    text = "for ${grams.formatNutritionDisplay()} g · ${food.caloriesPer100g.roundToInt()} kcal / 100 g",
                    style = MusFitTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = accent.onContainerVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            percentOfDayLabel(amountKcal, dayBudgetKcal)?.let { label ->
                Surface(
                    color = MusFitTheme.colors.surface.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(99.dp),
                ) {
                    Text(
                        text = label,
                        style = MusFitTheme.typography.labelSmall.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.W800,
                            letterSpacing = 0.sp,
                        ),
                        color = accent.onContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FoodDetailMacrosCard(amount: FoodNutrition, state: FoodUiState) {
    val colors = MusFitTheme.colors
    val entries = listOf(
        Triple("Carbs", amount.carbsGrams, state.carbsGoalGrams),
        Triple("Protein", amount.proteinGrams, state.proteinGoalGrams),
        Triple("Fat", amount.fatGrams, state.fatGoalGrams),
    )
    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val stackLabels = maxWidth < 440.dp
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                entries.forEachIndexed { index, entry ->
                    FoodDetailMacroEntry(
                        entry = entry,
                        colorIndex = index,
                        stackLabels = stackLabels,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun FoodDetailMacroEntry(
    entry: Triple<String, Double, Double>,
    colorIndex: Int,
    stackLabels: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MusFitTheme.colors
    Column(
        verticalArrangement = Arrangement.spacedBy(7.dp),
        modifier = modifier,
    ) {
        FoodDetailMacroLabels(
            label = entry.first,
            amountGrams = entry.second,
            colorIndex = colorIndex,
            stackLabels = stackLabels,
        )
        WavyProgressBar(
            progress = macroFillFraction(entry.second, entry.third),
            color = colors.macroColors[colorIndex],
            trackColor = colors.macroTracks[colorIndex],
        )
    }
}

@Composable
private fun FoodDetailMacroLabels(
    label: String,
    amountGrams: Double,
    colorIndex: Int,
    stackLabels: Boolean,
) {
    val colors = MusFitTheme.colors
    val value = "${amountGrams.formatNutritionDisplay()} g"
    if (stackLabels) {
        Text(
            text = label,
            style = MusFitTheme.typography.labelSmall.copy(
                fontSize = 11.5.sp,
                fontWeight = FontWeight.W800,
                letterSpacing = 0.sp,
            ),
            color = colors.macroColors[colorIndex],
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MusFitTheme.typography.labelSmall.copy(
                fontSize = 11.5.sp,
                letterSpacing = 0.sp,
            ),
            color = colors.onSurfaceVariant,
            maxLines = 1,
        )
    } else {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                style = MusFitTheme.typography.labelSmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.W800,
                    letterSpacing = 0.sp,
                ),
                color = colors.macroColors[colorIndex],
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = value,
                style = MusFitTheme.typography.labelSmall.copy(
                    fontSize = 11.5.sp,
                    letterSpacing = 0.sp,
                ),
                color = colors.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun FoodDetailDetailsList(food: SavedFoodUiState, grams: Double) {
    val scale = grams / 100.0
    var expanded by rememberSaveable { mutableStateOf(false) }
    // Only real per-100g fields, scaled to the chosen amount — never invented.
    val micronutrientRows = listOf(
        "Sat fat" to "${(food.saturatedFatPer100g * scale).formatNutritionDisplay()} g",
        "Potassium" to "${(food.potassiumMgPer100g * scale).formatMicronutrientDisplay()} mg",
        "Calcium" to "${(food.calciumMgPer100g * scale).formatMicronutrientDisplay()} mg",
        "Iron" to "${(food.ironMgPer100g * scale).formatMicronutrientDisplay()} mg",
        "Vitamin D" to "${(food.vitaminDMcgPer100g * scale).formatMicronutrientDisplay()} mcg",
        "Vitamin C" to "${(food.vitaminCMgPer100g * scale).formatMicronutrientDisplay()} mg",
        "Magnesium" to "${(food.magnesiumMgPer100g * scale).formatMicronutrientDisplay()} mg",
    )
    Surface(color = MusFitTheme.colors.surface, shape = RoundedCornerShape(24.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
        ) {
            HairlineDetailRow("Sugar", "${(food.sugarPer100g * scale).formatNutritionDisplay()} g")
            HairlineDetailRow("Fiber", "${(food.fiberPer100g * scale).formatNutritionDisplay()} g")
            HairlineDetailRow(
                label = "Salt",
                value = "${saltGramsFromSodiumMg(food.sodiumMgPer100g * scale).formatNutritionDisplay()} g",
                showDivider = expanded,
            )
            if (expanded) {
                micronutrientRows.forEach { (label, value) ->
                    HairlineDetailRow(label, value)
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 11.dp),
            ) {
                Text(
                    text = if (expanded) "Hide micronutrients" else "All micronutrients (${micronutrientRows.size})",
                    style = MusFitTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, fontWeight = FontWeight.W800),
                    color = MusFitTheme.colors.brand,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MusFitTheme.colors.brand,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
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
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val accent = tabAccentFor(TabAccentRole.Food)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 620.dp)
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Edit diary item",
                style = MusFitTheme.typography.headlineSmall.copy(fontSize = 22.sp, fontWeight = FontWeight.W800),
                color = MusFitTheme.colors.onSurface,
            )
            Text(
                text = editor.name.ifBlank { "Food item" },
                style = MusFitTheme.typography.bodySmall,
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

        FieldCell(
            label = "Amount (g)",
            value = editor.quantityGrams,
            onValueChange = onQuantityChanged,
            shape = RoundedCornerShape(20.dp),
            keyboardType = KeyboardType.Decimal,
            modifier = Modifier.fillMaxWidth(),
        )

        if (editor.servingChoices.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                editor.servingChoices.forEach { choice ->
                    SelectableChip(
                        text = choice.label,
                        selected = editor.quantityGrams == choice.grams.formatNutritionDisplay(),
                        onClick = { onServingChoiceSelected(choice.id) },
                        unselectedContainer = MusFitTheme.colors.surfaceVariant,
                    )
                }
            }
        }

        Surface(color = accent.container, shape = RoundedCornerShape(24.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                Text(
                    text = "Preview",
                    style = MusFitTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.W800),
                    color = accent.onContainer,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${editor.previewCaloriesKcal.roundToInt()} kcal",
                    style = MusFitTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.W800),
                    color = accent.onContainer,
                )
                Text(
                    text = " · C ${editor.previewCarbsGrams.formatNutritionDisplay()} · P ${editor.previewProteinGrams.formatNutritionDisplay()} · F ${editor.previewFatGrams.formatNutritionDisplay()}",
                    style = MusFitTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = accent.onContainerVariant,
                    maxLines = 1,
                )
            }
        }

        state.message?.let { message ->
            Text(
                text = message,
                style = MusFitTheme.typography.bodyMedium,
                color = MusFitTheme.colors.brand,
            )
        }

        PillButton(
            text = if (state.isSaving) "Saving" else "Save changes",
            onClick = onSaveClick,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )

        if (editor.isPlanned) {
            PillButton(
                text = "Mark logged",
                onClick = onMarkLoggedClick,
                enabled = !state.isSaving,
                containerColor = accent.container,
                contentColor = accent.onContainer,
                height = 48.dp,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionOverline("COPY ITEM")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            state.visibleMealDefinitions.forEach { choice ->
                SelectableChip(
                    text = choice.title,
                    selected = null,
                    onClick = { onCopyToMealClick(choice.id) },
                )
            }
            SelectableChip(
                text = "Tomorrow",
                selected = null,
                onClick = onCopyTomorrowClick,
            )
        }

        PillButton(
            text = "Delete from diary",
            onClick = { confirmDelete = true },
            enabled = !state.isSaving,
            containerColor = MusFitTheme.colors.destructiveContainer,
            contentColor = MusFitTheme.colors.onDestructiveContainer,
            icon = Icons.Outlined.Delete,
            height = 52.dp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    if (confirmDelete) {
        ConfirmDeleteDialog(
            title = "Delete from diary?",
            body = "This removes \"${editor.name.ifBlank { "this item" }}\" from the day. You can undo right after.",
            onConfirm = {
                confirmDelete = false
                onDeleteClick()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

/** Shared destructive-action confirmation for delete pills. */
@Composable
private fun ConfirmDeleteDialog(
    title: String,
    body: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MusFitTheme.colors.onDestructiveContainer)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = MusFitTheme.colors.surface,
    )
}

/**
 * Grouped white field cell (Turn 9 form grammar): faint 10.5/700 label over a
 * bold value, positional corner radii supplied by the caller.
 */
@Composable
private fun FieldCell(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    labelColor: Color = MusFitTheme.colors.onSurfaceFaint,
    enabled: Boolean = true,
) {
    Surface(color = MusFitTheme.colors.surface, shape = shape, modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 11.dp),
        ) {
            Text(
                text = label,
                style = MusFitTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.W700,
                    letterSpacing = 0.sp,
                ),
                color = labelColor,
                maxLines = 1,
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                enabled = enabled,
                textStyle = MusFitTheme.typography.bodyMedium.copy(
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.W700,
                    color = MusFitTheme.colors.onSurface,
                ),
                cursorBrush = SolidColor(MusFitTheme.colors.brand),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth(),
            )
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
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        mealDefinitions.forEach { choice ->
            SelectableChip(
                text = choice.title,
                selected = selectedMealType == choice.id,
                onClick = { onMealChanged(choice.id) },
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

/**
 * Turn 9 (9e): the saved-food editor as a full inner screen, hosted from
 * FoodScreen's priority chain. Parameter order after [onBack] matches the old
 * SavedFoodEditorPanel exactly (contract for the host).
 */
@Composable
internal fun SavedFoodEditorScreen(
    onBack: () -> Unit,
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
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MusFitTheme.colors.background)
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        InnerScreenHeader(
            title = if (isExistingFood) "Edit food" else "New food",
            onBack = onBack,
            trailing = {
                PillButton(
                    text = if (state.isSaving) "Saving" else "Save",
                    onClick = onSaveClick,
                    enabled = !state.isSaving,
                    height = 48.dp,
                )
            },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Provenance: real trust data exists only for already-saved foods.
            val savedSource = state.savedFoods.firstOrNull { it.id == editor.id }
            if (savedSource != null) {
                when (savedSource.trust.level) {
                    FoodTrustLevel.Imported ->
                        FoodTrustChip(Icons.Outlined.Verified, MusFitTheme.colors.brand, savedSource.sourceLabel)

                    FoodTrustLevel.Manual ->
                        FoodTrustChip(Icons.Outlined.Edit, MusFitTheme.colors.onSurfaceVariant, savedSource.trust.label)

                    FoodTrustLevel.NeedsReview ->
                        FoodTrustChip(Icons.Outlined.ErrorOutline, MusFitTheme.colors.warning, savedSource.trust.label)
                }
            }

            SectionOverline("IDENTITY")
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FieldCell(
                    label = "Name",
                    value = editor.name,
                    onValueChange = onNameChanged,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    FieldCell(
                        label = "Brand",
                        value = editor.brand,
                        onValueChange = onBrandChanged,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                    )
                    FieldCell(
                        label = "Category",
                        value = editor.category,
                        onValueChange = onCategoryChanged,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                    )
                }
                FieldCell(
                    label = "Barcode",
                    value = editor.barcode,
                    onValueChange = onBarcodeChanged,
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SectionOverline("SERVING")
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    color = MusFitTheme.colors.surface,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = "1 ${editor.servingName.ifBlank { "serving" }} = ${editor.servingGrams.ifBlank { "0" }} g",
                            style = MusFitTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.W700),
                            color = MusFitTheme.colors.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            tint = MusFitTheme.colors.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    FieldCell(
                        label = "Serving name",
                        value = editor.servingName,
                        onValueChange = onServingNameChanged,
                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 20.dp, bottomEnd = 8.dp),
                        modifier = Modifier.weight(1f),
                    )
                    FieldCell(
                        label = "Serving grams",
                        value = editor.servingGrams,
                        onValueChange = onServingChanged,
                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 20.dp),
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                    )
                }
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

            Surface(color = MusFitTheme.colors.surface, shape = RoundedCornerShape(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "Favorite",
                        style = MusFitTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, fontWeight = FontWeight.W700),
                        color = MusFitTheme.colors.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    BrandSwitch(checked = editor.isFavorite, onCheckedChange = onFavoriteChanged)
                }
            }

            state.message?.let { message ->
                Text(
                    text = message,
                    style = MusFitTheme.typography.bodyMedium,
                    color = MusFitTheme.colors.brand,
                )
            }
        }

        if (isExistingFood) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp),
            ) {
                PillButton(
                    text = "Duplicate",
                    onClick = onDuplicateClick,
                    enabled = !state.isSaving,
                    icon = Icons.Outlined.ContentCopy,
                    containerColor = MusFitTheme.colors.surfaceVariant,
                    contentColor = MusFitTheme.colors.onSurface,
                    height = 52.dp,
                    modifier = Modifier.weight(1f),
                )
                PillButton(
                    text = "Delete",
                    onClick = { confirmDelete = true },
                    enabled = !state.isSaving,
                    icon = Icons.Outlined.Delete,
                    containerColor = MusFitTheme.colors.destructiveContainer,
                    contentColor = MusFitTheme.colors.onDestructiveContainer,
                    height = 52.dp,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
    if (confirmDelete) {
        ConfirmDeleteDialog(
            title = "Delete saved food?",
            body = "\"${editor.name.ifBlank { "This food" }}\" is removed from your database. Diary entries keep their logged values.",
            onConfirm = {
                confirmDelete = false
                onDeleteClick()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

/** Brand-tracked M3 switch used across the restyled Food forms. */
@Composable
private fun BrandSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedTrackColor = MusFitTheme.colors.brand,
            checkedThumbColor = MusFitTheme.colors.surface,
            uncheckedTrackColor = if (isSystemInDarkTheme()) NeutralOutlineDark else NeutralOutline,
            uncheckedThumbColor = MusFitTheme.colors.surface,
            uncheckedBorderColor = Color.Transparent,
        ),
    )
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
    var showMicronutrients by rememberSaveable { mutableStateOf(false) }
    val colors = MusFitTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionOverline("NUTRITION PER 100 G")
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // 2×2 macro grid with macro-tinted labels (9e).
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                FieldCell(
                    label = "Calories",
                    value = editor.caloriesPer100g,
                    onValueChange = onCaloriesChanged,
                    shape = gridGroupShape(row = 0, rowCount = 2, column = 0, columnCount = 2, outer = 20.dp),
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                )
                FieldCell(
                    label = "Carbs",
                    value = editor.carbsPer100g,
                    onValueChange = onCarbsChanged,
                    shape = gridGroupShape(row = 0, rowCount = 2, column = 1, columnCount = 2, outer = 20.dp),
                    keyboardType = KeyboardType.Decimal,
                    labelColor = colors.macroCarbs,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                FieldCell(
                    label = "Protein",
                    value = editor.proteinPer100g,
                    onValueChange = onProteinChanged,
                    shape = gridGroupShape(row = 1, rowCount = 2, column = 0, columnCount = 2, outer = 20.dp),
                    keyboardType = KeyboardType.Decimal,
                    labelColor = colors.macroProtein,
                    modifier = Modifier.weight(1f),
                )
                FieldCell(
                    label = "Fat",
                    value = editor.fatPer100g,
                    onValueChange = onFatChanged,
                    shape = gridGroupShape(row = 1, rowCount = 2, column = 1, columnCount = 2, outer = 20.dp),
                    keyboardType = KeyboardType.Decimal,
                    labelColor = colors.macroFat,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Surface(
            onClick = { showMicronutrients = !showMicronutrients },
            color = colors.surface,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            ) {
                Text(
                    text = if (showMicronutrients) "Hide micronutrients" else "Micronutrients (10)",
                    style = MusFitTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, fontWeight = FontWeight.W800),
                    color = colors.brand,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (showMicronutrients) Icons.Filled.ExpandLess else Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = colors.brand,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        if (showMicronutrients) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val rows = listOf(
                    listOf(
                        Triple("Fiber", editor.fiberPer100g, onFiberChanged),
                        Triple("Sugar", editor.sugarPer100g, onSugarChanged),
                    ),
                    listOf(
                        Triple("Sat fat", editor.saturatedFatPer100g, onSaturatedFatChanged),
                        Triple("Sodium mg", editor.sodiumMgPer100g, onSodiumChanged),
                    ),
                    listOf(
                        Triple("Potassium mg", editor.potassiumMgPer100g, onPotassiumChanged),
                        Triple("Calcium mg", editor.calciumMgPer100g, onCalciumChanged),
                    ),
                    listOf(
                        Triple("Iron mg", editor.ironMgPer100g, onIronChanged),
                        Triple("Vit D mcg", editor.vitaminDMcgPer100g, onVitaminDChanged),
                    ),
                    listOf(
                        Triple("Vit C mg", editor.vitaminCMgPer100g, onVitaminCChanged),
                        Triple("Magnesium mg", editor.magnesiumMgPer100g, onMagnesiumChanged),
                    ),
                )
                rows.forEachIndexed { rowIndex, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEachIndexed { columnIndex, cell ->
                            FieldCell(
                                label = cell.first,
                                value = cell.second,
                                onValueChange = cell.third,
                                shape = gridGroupShape(
                                    row = rowIndex,
                                    rowCount = rows.size,
                                    column = columnIndex,
                                    columnCount = row.size,
                                    outer = 20.dp,
                                ),
                                keyboardType = KeyboardType.Decimal,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Turn 9 (9i): nutrition goals as a full inner screen. Parameter order after
 * [onBack] matches the old GoalEditorPanel exactly (contract for the host).
 * [onModeChanged] stays in the contract; mode now changes via programs.
 */
@Composable
internal fun GoalEditorScreen(
    onBack: () -> Unit,
    state: FoodUiState,
    onCaloriesChanged: (String) -> Unit,
    onProteinChanged: (String) -> Unit,
    onCarbsChanged: (String) -> Unit,
    onFatChanged: (String) -> Unit,
    onFiberChanged: (String) -> Unit,
    onSugarChanged: (String) -> Unit,
    onSaturatedFatChanged: (String) -> Unit,
    onSodiumChanged: (String) -> Unit,
    @Suppress("UNUSED_PARAMETER") onModeChanged: (FoodGoalMode) -> Unit,
    onTrainingChanged: (Boolean) -> Unit,
    onNetCarbsChanged: (Boolean) -> Unit,
    onProgramApply: (String) -> Unit,
    onSaveClick: () -> Unit,
) {
    val editor = state.goalEditor
    val accent = tabAccentFor(TabAccentRole.Food)
    val colors = MusFitTheme.colors
    var showAllPrograms by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        InnerScreenHeader(
            title = "Nutrition goals",
            onBack = onBack,
            trailing = {
                PillButton(
                    text = if (state.isSaving) "Saving" else "Save",
                    onClick = onSaveClick,
                    enabled = !state.isSaving,
                    height = 48.dp,
                )
            },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Calorie hero: ±50 kcal steppers around the daily target.
            Surface(color = accent.container, shape = RoundedCornerShape(28.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    StepperCircleButton(
                        icon = Icons.Outlined.Remove,
                        contentDescription = "Remove 50 kcal",
                        onClick = { onCaloriesChanged(steppedCaloriesInput(editor.caloriesKcalInput, -50)) },
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        val calories = editor.caloriesKcalInput.toDoubleOrNull()?.roundToInt() ?: 0
                        Text(
                            text = "%,d".format(calories),
                            style = HeroNumberMediumStyle.copy(letterSpacing = (-1).sp),
                            color = accent.onContainer,
                            maxLines = 1,
                        )
                        Text(
                            text = "kcal daily target · ${editor.modeInput.label.lowercase()}",
                            style = MusFitTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = accent.onContainerVariant,
                            modifier = Modifier.padding(top = 3.dp),
                            maxLines = 1,
                        )
                    }
                    StepperCircleButton(
                        icon = Icons.Outlined.Add,
                        contentDescription = "Add 50 kcal",
                        filled = true,
                        onClick = { onCaloriesChanged(steppedCaloriesInput(editor.caloriesKcalInput, 50)) },
                    )
                }
            }

            GoalMacroSplitCard(editor = editor)

            // Editable gram targets feed the split card live.
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                FieldCell(
                    label = "Carbs g",
                    value = editor.carbsGramsInput,
                    onValueChange = onCarbsChanged,
                    shape = gridGroupShape(row = 0, rowCount = 1, column = 0, columnCount = 3, outer = 20.dp),
                    keyboardType = KeyboardType.Decimal,
                    labelColor = colors.macroCarbs,
                    modifier = Modifier.weight(1f),
                )
                FieldCell(
                    label = "Protein g",
                    value = editor.proteinGramsInput,
                    onValueChange = onProteinChanged,
                    shape = gridGroupShape(row = 0, rowCount = 1, column = 1, columnCount = 3, outer = 20.dp),
                    keyboardType = KeyboardType.Decimal,
                    labelColor = colors.macroProtein,
                    modifier = Modifier.weight(1f),
                )
                FieldCell(
                    label = "Fat g",
                    value = editor.fatGramsInput,
                    onValueChange = onFatChanged,
                    shape = gridGroupShape(row = 0, rowCount = 1, column = 2, columnCount = 3, outer = 20.dp),
                    keyboardType = KeyboardType.Decimal,
                    labelColor = colors.macroFat,
                    modifier = Modifier.weight(1f),
                )
            }

            Surface(color = colors.surface, shape = RoundedCornerShape(24.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                ) {
                    GoalToggleRow(
                        label = "Include training calories",
                        checked = editor.includeTrainingInput,
                        onCheckedChange = onTrainingChanged,
                    )
                    HorizontalDivider(thickness = 1.dp, color = colors.outline)
                    GoalToggleRow(
                        label = "Count net carbs",
                        checked = editor.useNetCarbsInput,
                        onCheckedChange = onNetCarbsChanged,
                    )
                }
            }

            SectionOverline("PROGRAMS")
            val visiblePrograms = if (showAllPrograms) state.foodPrograms else state.foodPrograms.take(3)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                visiblePrograms.forEachIndexed { index, program ->
                    GoalProgramRow(
                        program = program,
                        index = index,
                        count = visiblePrograms.size,
                        isSaving = state.isSaving,
                        onApply = { onProgramApply(program.id) },
                    )
                }
            }
            if (!showAllPrograms && state.foodPrograms.size > 3) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clickable { showAllPrograms = true }
                        .padding(horizontal = 4.dp),
                ) {
                    Text(
                        text = "All ${state.foodPrograms.size} programs",
                        style = MusFitTheme.typography.bodyMedium.copy(fontSize = 12.5.sp, fontWeight = FontWeight.W800),
                        color = colors.brand,
                    )
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = colors.brand,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }

            SectionOverline("ADVANCED TARGETS")
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    FieldCell(
                        label = "Fiber g",
                        value = editor.fiberGramsInput,
                        onValueChange = onFiberChanged,
                        shape = gridGroupShape(row = 0, rowCount = 2, column = 0, columnCount = 2, outer = 20.dp),
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                    )
                    FieldCell(
                        label = "Sugar g",
                        value = editor.sugarGramsInput,
                        onValueChange = onSugarChanged,
                        shape = gridGroupShape(row = 0, rowCount = 2, column = 1, columnCount = 2, outer = 20.dp),
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    FieldCell(
                        label = "Sat fat g",
                        value = editor.saturatedFatGramsInput,
                        onValueChange = onSaturatedFatChanged,
                        shape = gridGroupShape(row = 1, rowCount = 2, column = 0, columnCount = 2, outer = 20.dp),
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                    )
                    FieldCell(
                        label = "Sodium mg",
                        value = editor.sodiumMgInput,
                        onValueChange = onSodiumChanged,
                        shape = gridGroupShape(row = 1, rowCount = 2, column = 1, columnCount = 2, outer = 20.dp),
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            state.message?.let {
                Text(it, style = MusFitTheme.typography.bodyMedium, color = colors.brand)
            }
        }
    }
}

@Composable
private fun GoalMacroSplitCard(editor: GoalEditorState) {
    val colors = MusFitTheme.colors
    val (carbsPct, proteinPct, fatPct) = macroSplitPercents(
        caloriesInput = editor.caloriesKcalInput,
        carbs = editor.carbsGramsInput,
        protein = editor.proteinGramsInput,
        fat = editor.fatGramsInput,
    )
    val entries = listOf(
        Triple("Carbs", carbsPct, editor.carbsGramsInput),
        Triple("Protein", proteinPct, editor.proteinGramsInput),
        Triple("Fat", fatPct, editor.fatGramsInput),
    )
    Surface(color = colors.surface, shape = RoundedCornerShape(24.dp)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Macro split",
                    style = MusFitTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.W800),
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = editor.modeInput.label,
                    style = MusFitTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = colors.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                entries.forEachIndexed { index, entry ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = entry.first,
                                style = MusFitTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.W800,
                                    letterSpacing = 0.sp,
                                ),
                                color = colors.macroColors[index],
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "${entry.second}%",
                                style = MusFitTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 0.sp),
                                color = colors.onSurfaceVariant,
                            )
                        }
                        WavyProgressBar(
                            progress = entry.second / 100f,
                            color = colors.macroColors[index],
                            trackColor = colors.macroTracks[index],
                        )
                        Text(
                            text = "${entry.third.ifBlank { "0" }} g",
                            style = MusFitTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 0.sp),
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MusFitTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, fontWeight = FontWeight.W700),
            color = MusFitTheme.colors.onSurface,
            modifier = Modifier.weight(1f),
        )
        BrandSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun GoalProgramRow(
    program: FoodProgramUiState,
    index: Int,
    count: Int,
    isSaving: Boolean,
    onApply: () -> Unit,
) {
    val colors = MusFitTheme.colors
    val accent = tabAccentFor(TabAccentRole.Food)
    // Badge palette cycles rose → water → amber → green down the list.
    val (badgeContainer, badgeInk) = when (index % 4) {
        0 -> colors.destructiveContainer to colors.onDestructiveContainer
        1 -> colors.waterFill to colors.water
        2 -> colors.warningContainer to colors.warning
        else -> accent.container to accent.onContainerVariant
    }
    val badgeIcon = when (index % 4) {
        0 -> Icons.Outlined.FitnessCenter
        1 -> Icons.Outlined.Eco
        2 -> Icons.Outlined.LocalFireDepartment
        else -> Icons.Outlined.Restaurant
    }
    Surface(color = colors.surface, shape = groupedShape(index, count)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 13.dp),
        ) {
            ExpressiveBadge(
                icon = badgeIcon,
                shape = expressiveBadgeShapeFor(index),
                containerColor = badgeContainer,
                contentColor = badgeInk,
                size = 44.dp,
                iconSize = 20.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = program.title,
                    style = MusFitTheme.typography.titleSmall.copy(fontWeight = FontWeight.W800, letterSpacing = (-0.2).sp),
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = program.subtitle,
                    style = MusFitTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(
                onClick = onApply,
                enabled = !isSaving && !program.isSelected,
                shape = RoundedCornerShape(99.dp),
                color = if (program.isSelected) accent.container else colors.surfaceVariant,
                contentColor = if (program.isSelected) accent.onContainer else colors.onSurface,
            ) {
                Text(
                    text = if (program.isSelected) "Applied" else "Apply",
                    style = MusFitTheme.typography.labelMedium.copy(fontWeight = FontWeight.W800),
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp),
                )
            }
        }
    }
}

@Composable
@Suppress("LongMethod", "LongParameterList") // Browser/editor host intentionally forwards one stable action contract.
internal fun RecipeBrowserScreen(
    state: FoodUiState,
    showEditorBack: Boolean = true,
    recipeListState: LazyGridState = rememberLazyGridState(),
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
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (isEditorPage) {
            val editorTitle = if (state.recipeEditor.editingRecipeId == null) "New recipe" else "Edit recipe"
            if (showEditorBack) {
                InnerScreenHeader(title = editorTitle, onBack = onHomeClick)
            } else {
                Text(
                    text = editorTitle,
                    style = MusFitTheme.typography.headlineSmall,
                    color = MusFitTheme.colors.onSurface,
                )
            }
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
                recipeListState = recipeListState,
                modifier = Modifier.weight(1f),
                onBackClick = onCloseClick,
                onPreviousDayClick = onPreviousDayClick,
                onNextDayClick = onNextDayClick,
                onTodayClick = onTodayClick,
                onMealChanged = onMealChanged,
                onServingsChanged = onServingsChanged,
                onSearchQueryChanged = onSearchQueryChanged,
                onFilterChanged = onDiscoveryFilterChanged,
                onItemClick = onDiscoveryItemClick,
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

private fun RecipeDiscoveryFilter.chipLabel(): String = if (this == RecipeDiscoveryFilter.All) "For you" else label

private const val RECIPE_GRID_COLUMN_COUNT = 2

internal fun recipeGridShape(index: Int, itemCount: Int): RoundedCornerShape {
    require(index in 0 until itemCount)
    val row = index / RECIPE_GRID_COLUMN_COUNT
    val rowCount = (itemCount + RECIPE_GRID_COLUMN_COUNT - 1) / RECIPE_GRID_COLUMN_COUNT
    val column = index % RECIPE_GRID_COLUMN_COUNT
    val populatedColumns = if (row == rowCount - 1 && itemCount % RECIPE_GRID_COLUMN_COUNT != 0) {
        itemCount % RECIPE_GRID_COLUMN_COUNT
    } else {
        RECIPE_GRID_COLUMN_COUNT
    }
    return gridGroupShape(
        row = row,
        rowCount = rowCount,
        column = column,
        columnCount = populatedColumns,
    )
}

@Composable
@Suppress("CyclomaticComplexMethod", "LongMethod", "LongParameterList") // One lazy scene owns ordered full-span and keyed-grid sections.
private fun RecipeBrowserHome(
    state: FoodUiState,
    recipeListState: LazyGridState,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onPreviousDayClick: () -> Unit,
    onNextDayClick: () -> Unit,
    onTodayClick: () -> Unit,
    onMealChanged: (String) -> Unit,
    onServingsChanged: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onFilterChanged: (RecipeDiscoveryFilter) -> Unit,
    onItemClick: (String) -> Unit,
    onReviewIdeaClick: (String) -> Unit,
    onLogRecipeClick: (String) -> Unit,
    onPlanRecipeClick: (String) -> Unit,
    onEditRecipeClick: (String) -> Unit,
    onFavoriteClick: (String, Boolean) -> Unit,
    onCreateClick: () -> Unit,
) {
    val accent = tabAccentFor(TabAccentRole.Food)
    val colors = MusFitTheme.colors
    var searchVisible by rememberSaveable { mutableStateOf(state.recipeDiscovery.query.isNotBlank()) }
    var showMyRecipes by rememberSaveable { mutableStateOf(false) }
    val mealTitle = state.visibleMealDefinitions
        .firstOrNull { it.id == state.recipeBrowserMealType }?.title
        ?: state.recipeBrowserMealType
    val visibleItems = state.recipeDiscovery.visibleItems
    val featured = visibleItems.firstOrNull { it.programRelevant } ?: visibleItems.firstOrNull()
    val gridItems = visibleItems.filterNot { it.id == featured?.id }
    val hasEmptyDiscovery = gridItems.isEmpty() && featured == null
    val hasDiscoverySection = hasEmptyDiscovery || gridItems.isNotEmpty()
    val myRecipesHeadingIndex =
        (if (state.message != null) 2 else 0) +
            (if (featured != null) 2 else 0) +
            2 +
            when {
                hasEmptyDiscovery -> 2
                gridItems.isNotEmpty() -> gridItems.size + 1
                else -> 0
            }

    LaunchedEffect(showMyRecipes, myRecipesHeadingIndex) {
        if (showMyRecipes) {
            recipeListState.animateScrollToItem(myRecipesHeadingIndex)
        }
    }

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InnerScreenHeader(
            title = "Recipes",
            onBack = onBackClick,
            trailing = {
                TonalHeaderIconButton(
                    icon = Icons.Outlined.Search,
                    contentDescription = "Search recipes",
                    onClick = { searchVisible = !searchVisible },
                    modifier = Modifier.size(48.dp),
                )
            },
        )

        if (searchVisible || state.recipeDiscovery.query.isNotBlank()) {
            Surface(color = colors.surface, shape = RoundedCornerShape(99.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 13.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        if (state.recipeDiscovery.query.isEmpty()) {
                            Text(
                                text = "Find recipes",
                                style = MusFitTheme.typography.bodyMedium.copy(fontSize = 14.5.sp),
                                color = colors.onSurfaceFaint,
                            )
                        }
                        BasicTextField(
                            value = state.recipeDiscovery.query,
                            onValueChange = onSearchQueryChanged,
                            singleLine = true,
                            textStyle = MusFitTheme.typography.bodyMedium.copy(
                                fontSize = 14.5.sp,
                                color = colors.onSurface,
                            ),
                            cursorBrush = SolidColor(colors.brand),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            RecipeDiscoveryFilter.entries.forEach { filter ->
                SelectableChip(
                    text = filter.chipLabel(),
                    selected = state.recipeDiscovery.filter == filter,
                    onClick = { onFilterChanged(filter) },
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(RECIPE_GRID_COLUMN_COUNT),
            state = recipeListState,
            modifier = Modifier
                .weight(1f)
                .clipToBounds(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            state.message?.let {
                item(
                    key = "message",
                    span = { GridItemSpan(maxLineSpan) },
                ) {
                    Text(
                        text = it,
                        style = MusFitTheme.typography.bodyMedium,
                        color = colors.brand,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                verticalGap("gap-after-message", 12.dp)
            }

            if (featured != null) {
                item(
                    key = "featured-${featured.id}",
                    span = { GridItemSpan(maxLineSpan) },
                ) {
                    RecipeFeaturedCard(
                        item = featured,
                        mealTitle = mealTitle,
                        isSaving = state.isSaving,
                        onPlanClick = onPlanRecipeClick,
                        onReviewClick = onReviewIdeaClick,
                    )
                }
                verticalGap("gap-after-featured", 12.dp)
            }

            // Compact plan target: day, meal, and servings feed plan/log calls.
            item(
                key = "plan-target",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                RecipePlanTargetCard(
                    state = state,
                    mealTitle = mealTitle,
                    onPreviousDayClick = onPreviousDayClick,
                    onNextDayClick = onNextDayClick,
                    onTodayClick = onTodayClick,
                    onMealChanged = onMealChanged,
                    onServingsChanged = onServingsChanged,
                )
            }
            if (hasDiscoverySection || showMyRecipes) {
                verticalGap("gap-after-plan-target", 12.dp)
            }

            if (hasEmptyDiscovery) {
                item(
                    key = "empty",
                    span = { GridItemSpan(maxLineSpan) },
                ) {
                    RecipeBrowserEmptyText("No recipes match this search yet.")
                }
                if (showMyRecipes) {
                    verticalGap("gap-after-empty", 12.dp)
                }
            } else if (gridItems.isNotEmpty()) {
                gridItemsIndexed(
                    items = gridItems,
                    key = { _, recipe -> "discovery-${recipe.id}" },
                    contentType = { _, _ -> "recipe-discovery-card" },
                ) { index, recipeItem ->
                    val rowIndex = index / RECIPE_GRID_COLUMN_COUNT
                    val rowCount = (gridItems.size + RECIPE_GRID_COLUMN_COUNT - 1) / RECIPE_GRID_COLUMN_COUNT
                    RecipeGridCard(
                        item = recipeItem,
                        shape = recipeGridShape(index = index, itemCount = gridItems.size),
                        onClick = { onItemClick(recipeItem.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (rowIndex < rowCount - 1) 8.dp else 0.dp),
                    )
                }
                if (showMyRecipes) {
                    verticalGap("gap-after-discovery", 12.dp)
                }
            }

            if (showMyRecipes) {
                item(
                    key = "my-recipes-heading",
                    span = { GridItemSpan(maxLineSpan) },
                ) {
                    SectionOverline("MY RECIPES")
                }
                verticalGap("gap-after-my-recipes-heading", 12.dp)
                if (state.recipes.isEmpty()) {
                    item(
                        key = "my-recipes-empty",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        RecipeBrowserEmptyText("No saved recipes yet — build one with New recipe.")
                    }
                } else {
                    gridItemsIndexed(
                        items = state.recipes,
                        key = { _, recipe -> "saved-${recipe.id}" },
                        span = { _, _ -> GridItemSpan(maxLineSpan) },
                        contentType = { _, _ -> "saved-recipe" },
                    ) { index, recipe ->
                        Column {
                            FoodListItemRow(
                                index = index,
                                count = state.recipes.size,
                                title = recipe.name,
                                subtitle = listOfNotNull(
                                    recipe.itemSummary.ifBlank { null },
                                    "${recipe.servings.formatNutritionDisplay()} servings",
                                ).joinToString(" · "),
                                onClick = { onEditRecipeClick(recipe.id) },
                                fallbackIcon = Icons.Filled.RiceBowl,
                                badgeSize = 44.dp,
                                trailingContent = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        IconButton(onClick = { onFavoriteClick(recipe.id, !recipe.isFavorite) }) {
                                            Icon(
                                                imageVector = if (recipe.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                                contentDescription = if (recipe.isFavorite) "Unfavorite" else "Favorite",
                                                tint = if (recipe.isFavorite) colors.macroProtein else colors.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                        Surface(
                                            onClick = { onLogRecipeClick(recipe.id) },
                                            enabled = !state.isSaving,
                                            color = accent.container,
                                            contentColor = accent.onContainer,
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.size(48.dp),
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Add,
                                                    contentDescription = "${state.foodEntryActionVerb} recipe",
                                                    modifier = Modifier.size(20.dp),
                                                )
                                            }
                                        }
                                    }
                                },
                            )
                            if (index < state.recipes.lastIndex) {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
        ) {
            PillButton(
                text = "My recipes · ${state.recipes.size}",
                onClick = { showMyRecipes = !showMyRecipes },
                icon = Icons.Outlined.Bookmark,
                containerColor = accent.container,
                contentColor = accent.onContainer,
                height = 54.dp,
                modifier = Modifier.weight(1f),
            )
            PillButton(
                text = "New recipe",
                onClick = onCreateClick,
                icon = Icons.Outlined.Add,
                height = 54.dp,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
@Suppress("LongParameterList")
internal fun RecipePlanTargetCard(
    state: FoodUiState,
    mealTitle: String,
    onPreviousDayClick: () -> Unit,
    onNextDayClick: () -> Unit,
    onTodayClick: () -> Unit,
    onMealChanged: (String) -> Unit,
    onServingsChanged: (String) -> Unit,
) {
    val colors = MusFitTheme.colors
    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth < 420.dp) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    RecipePlanDateControls(
                        state = state,
                        onPreviousDayClick = onPreviousDayClick,
                        onNextDayClick = onNextDayClick,
                        onTodayClick = onTodayClick,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    RecipePlanMealControls(
                        state = state,
                        mealTitle = mealTitle,
                        onMealChanged = onMealChanged,
                        onServingsChanged = onServingsChanged,
                        modifier = Modifier.width(190.dp),
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    RecipePlanDateControls(
                        state = state,
                        onPreviousDayClick = onPreviousDayClick,
                        onNextDayClick = onNextDayClick,
                        onTodayClick = onTodayClick,
                        modifier = Modifier.weight(1f),
                    )
                    RecipePlanMealControls(
                        state = state,
                        mealTitle = mealTitle,
                        onMealChanged = onMealChanged,
                        onServingsChanged = onServingsChanged,
                        modifier = Modifier.width(190.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecipePlanDateControls(
    state: FoodUiState,
    onPreviousDayClick: () -> Unit,
    onNextDayClick: () -> Unit,
    onTodayClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MusFitTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        IconButton(onClick = onPreviousDayClick) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous day", tint = colors.onSurface)
        }
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clickable(onClickLabel = "Jump to today", onClick = onTodayClick),
        ) {
            Text(
                text = state.recipeBrowserDate.toString(),
                style = MusFitTheme.typography.labelMedium.copy(fontWeight = FontWeight.W700),
                color = colors.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "Today",
                style = MusFitTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                color = colors.brand,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        IconButton(onClick = onNextDayClick) {
            Icon(Icons.Filled.ChevronRight, contentDescription = "Next day", tint = colors.onSurface)
        }
    }
}

@Composable
private fun RecipePlanMealControls(
    state: FoodUiState,
    mealTitle: String,
    onMealChanged: (String) -> Unit,
    onServingsChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        MealTargetChip(
            label = mealTitle,
            meals = state.visibleMealDefinitions,
            onMealSelected = onMealChanged,
            modifier = Modifier.weight(1f),
        )
        FieldCell(
            label = "Serv",
            value = state.recipeServingsToLog,
            onValueChange = onServingsChanged,
            shape = RoundedCornerShape(16.dp),
            keyboardType = KeyboardType.Decimal,
            modifier = Modifier.width(72.dp),
        )
    }
}

@Composable
internal fun RecipeFeaturedCard(
    item: RecipeDiscoveryItemUiState,
    mealTitle: String,
    isSaving: Boolean,
    onPlanClick: (String) -> Unit,
    onReviewClick: (String) -> Unit,
) {
    val accent = tabAccentFor(TabAccentRole.Food)
    val sourceRecipeId = item.sourceRecipeId
    val actionText = if (sourceRecipeId != null) "Plan ${mealTitle.lowercase()}" else "Review"
    val onAction = {
        if (sourceRecipeId != null) onPlanClick(sourceRecipeId) else onReviewClick(item.id)
    }
    Surface(
        color = accent.container,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            RecipeBrowserThumbnail(
                key = item.thumbnailKey,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
            )
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth < 360.dp) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(start = 18.dp, end = 14.dp, top = 14.dp, bottom = 16.dp),
                    ) {
                        RecipeFeaturedSummary(item = item, modifier = Modifier.fillMaxWidth())
                        PillButton(
                            text = actionText,
                            onClick = onAction,
                            enabled = !isSaving,
                            height = 48.dp,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 14.dp, top = 14.dp, bottom = 16.dp),
                    ) {
                        RecipeFeaturedSummary(item = item, modifier = Modifier.weight(1f))
                        PillButton(
                            text = actionText,
                            onClick = onAction,
                            enabled = !isSaving,
                            height = 48.dp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeFeaturedSummary(item: RecipeDiscoveryItemUiState, modifier: Modifier = Modifier) {
    val accent = tabAccentFor(TabAccentRole.Food)
    Column(modifier = modifier) {
        Text(
            text = item.title,
            style = MusFitTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.W800),
            color = accent.onContainer,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = buildList {
                add("${item.caloriesKcal.roundToInt()} kcal")
                add("${item.proteinGrams.formatNutritionDisplay()} g protein")
                item.tagLabels.firstOrNull()?.let(::add)
            }.joinToString(" · "),
            style = MusFitTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = accent.onContainerVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
        )
    }
}

@Composable
private fun RecipeGridCard(
    item: RecipeDiscoveryItemUiState,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(onClick = onClick, color = MusFitTheme.colors.surface, shape = shape, modifier = modifier) {
        Column {
            RecipeBrowserThumbnail(
                key = item.thumbnailKey,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp),
            )
            Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 11.dp, bottom = 13.dp)) {
                Text(
                    text = item.title,
                    style = MusFitTheme.typography.titleSmall.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W800,
                        letterSpacing = (-0.2).sp,
                    ),
                    color = MusFitTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildList {
                        add("${item.caloriesKcal.roundToInt()} kcal")
                        add("P ${item.proteinGrams.formatNutritionDisplay()}")
                        if (item.isSavedRecipe) add("Saved")
                    }.joinToString(" · "),
                    style = MusFitTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = MusFitTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
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
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
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
            .padding(start = if (isFullScreen) 0.dp else 18.dp, end = if (isFullScreen) 0.dp else 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (!isFullScreen) {
            Text(
                text = if (editor.editingRecipeId == null) "Recipe" else "Edit recipe",
                style = MusFitTheme.typography.headlineSmall.copy(fontSize = 22.sp, fontWeight = FontWeight.W800),
                color = MusFitTheme.colors.onSurface,
            )
        }
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
        SectionOverline("RECIPE")
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FieldCell(
                label = "Recipe name",
                value = editor.name,
                onValueChange = onNameChanged,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                FieldCell(
                    label = "Category",
                    value = editor.category,
                    onValueChange = onCategoryChanged,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                )
                FieldCell(
                    label = "Serving",
                    value = editor.servingName,
                    onValueChange = onServingNameChanged,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                FieldCell(
                    label = "Servings",
                    value = editor.servingsCount,
                    onValueChange = onServingsCountChanged,
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 20.dp, bottomEnd = 8.dp),
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                )
                FieldCell(
                    label = "Cooked yield g",
                    value = editor.cookedYieldGrams,
                    onValueChange = onCookedYieldChanged,
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 20.dp),
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Text(
            text = "${editor.servingGrams.ifBlank { "0" }} g per ${editor.servingName.ifBlank { "serving" }}",
            style = MusFitTheme.typography.bodySmall,
            color = MusFitTheme.colors.onSurfaceVariant,
        )
        SectionOverline("INGREDIENTS")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(items = state.savedFoods, key = SavedFoodUiState::id) { food ->
                SelectableChip(
                    text = food.name,
                    selected = editor.ingredientFoodId == food.id,
                    onClick = { onIngredientFoodChanged(food.id) },
                )
            }
        }
        if (editor.ingredientServingChoices.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(items = editor.ingredientServingChoices, key = RecipeIngredientServingChoiceUiState::id) { choice ->
                    SelectableChip(
                        text = choice.label,
                        selected = editor.ingredientServingChoiceId == choice.id,
                        onClick = { onIngredientServingChoiceSelected(choice.id) },
                        unselectedContainer = MusFitTheme.colors.surfaceVariant,
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            FieldCell(
                label = "Amount g",
                value = editor.ingredientQuantityGrams,
                onValueChange = onIngredientQuantityChanged,
                shape = RoundedCornerShape(20.dp),
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
            )
            PillButton(
                text = "Add",
                onClick = onAddIngredientClick,
                icon = Icons.Outlined.Add,
                containerColor = MusFitTheme.colors.surfaceVariant,
                contentColor = MusFitTheme.colors.onSurface,
                height = 48.dp,
            )
        }
        if (editor.ingredients.isNotEmpty()) {
            Surface(color = MusFitTheme.colors.surface, shape = RoundedCornerShape(20.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    editor.ingredients.forEachIndexed { index, ingredient ->
                        HairlineDetailRow(
                            label = ingredient.foodName,
                            value = "${ingredient.unitQuantity.formatNutritionDisplay()} ${ingredient.unitLabel} · ${ingredient.quantityGrams.roundToInt()} g",
                            showDivider = index != editor.ingredients.lastIndex,
                        )
                    }
                }
            }
        }
        state.message?.let { Text(it, style = MusFitTheme.typography.bodyMedium, color = MusFitTheme.colors.brand) }
        PillButton(
            text = if (state.isSaving) "Saving" else "Save recipe",
            onClick = onSaveClick,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        if (editor.editingRecipeId != null) {
            PillButton(
                text = "Delete recipe",
                onClick = { confirmDelete = true },
                enabled = !state.isSaving,
                icon = Icons.Outlined.Delete,
                containerColor = MusFitTheme.colors.destructiveContainer,
                contentColor = MusFitTheme.colors.onDestructiveContainer,
                height = 52.dp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    if (confirmDelete) {
        ConfirmDeleteDialog(
            title = "Delete recipe?",
            body = "\"${editor.name.ifBlank { "This recipe" }}\" is removed from your recipes.",
            onConfirm = {
                confirmDelete = false
                onDeleteClick()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun SavedRecipesSection(
    recipes: List<RecipeUiState>,
    onEditRecipeClick: (String) -> Unit,
    onDuplicateRecipeClick: (String) -> Unit,
    onFavoriteClick: (String, Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionTitle("Saved recipes")
        Column(modifier = Modifier.fillMaxWidth()) {
            recipes.forEach { recipe ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(recipe.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            listOfNotNull(
                                recipe.itemSummary,
                                "${recipe.servings.formatNutritionDisplay()} servings",
                                "${recipe.cookedYieldGrams.formatNutritionDisplay()} g yield",
                                if (recipe.isFavorite) "Favorite" else null,
                            ).joinToString(" - "),
                            style = MaterialTheme.typography.bodySmall,
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
                HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
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

internal fun sectionRecipeBrowserItems(items: List<RecipeDiscoveryItemUiState>): RecipeBrowserSections = RecipeBrowserSections(
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

/**
 * Recipe photo placeholder: diagonal stripes in the Food accent family (the
 * Turn 9 "recipe photo" strip). [key] only shifts the stripe phase so
 * neighboring cards read as different placeholders — no hardcoded palettes.
 */
@Composable
private fun RecipeBrowserThumbnail(
    key: String,
    modifier: Modifier = Modifier,
) {
    val accent = tabAccentFor(TabAccentRole.Food)
    val base = accent.container
    val stripe = MusFitTheme.colors.surfaceVariant
    val phase = (key.hashCode().absoluteValue % 3) * 7f
    Canvas(modifier = modifier) {
        drawRect(color = base)
        val step = 20.dp.toPx()
        var x = -size.height + phase
        while (x < size.width) {
            drawLine(
                color = stripe.copy(alpha = 0.5f),
                start = Offset(x, size.height),
                end = Offset(x + size.height, 0f),
                strokeWidth = step / 2f,
            )
            x += step * 2f
        }
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
private fun RecipeDiscoveryCatalog(
    state: FoodUiState,
    onFilterChanged: (RecipeDiscoveryFilter) -> Unit,
    onItemClick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Recipe discovery")
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
            Column(modifier = Modifier.fillMaxWidth()) {
                state.recipeDiscovery.visibleItems.take(8).forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                    HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
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
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 560.dp),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 28.dp),
    ) {
        item(key = "meal-template-title") {
            Text("Meal templates", style = MaterialTheme.typography.headlineSmall)
        }
        verticalGap("gap-after-meal-template-title", 14.dp)
        state.mealTemplateEditor?.let { editor ->
            item(key = "meal-template-editor") {
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
                    editor.items.forEachIndexed { index, templateItem ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = templateItem.quantityGrams,
                                onValueChange = { onTemplateItemQuantityChanged(index, it) },
                                label = { Text(templateItem.foodName) },
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
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(items = state.savedFoods, key = SavedFoodUiState::id) { food ->
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
            verticalGap("gap-after-meal-template-editor", 14.dp)
            item(key = "meal-template-editor-divider") {
                HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
            }
            verticalGap("gap-after-meal-template-editor-divider", 14.dp)
        }
        if (state.mealTemplates.isEmpty()) {
            item(key = "meal-template-empty") {
                Text("No meal templates yet", color = MusFitTheme.colors.onSurfaceVariant)
            }
        } else {
            items(items = state.mealTemplates, key = MealTemplateUiState::id) { template ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("meal-template-row-${template.id}")
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
        state.message?.let { message ->
            verticalGap("gap-before-meal-template-message", 14.dp)
            item(key = "meal-template-message") {
                Text(message, color = MusFitTheme.colors.brand)
            }
        }
    }
}
