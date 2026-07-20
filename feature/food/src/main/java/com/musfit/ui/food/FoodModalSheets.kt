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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import com.musfit.feature.food.R
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
import com.musfit.ui.text.LocalizedFormatter
import com.musfit.ui.text.UiText
import com.musfit.ui.text.asString
import com.musfit.ui.text.uiText
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.NeutralOutline
import com.musfit.ui.theme.NeutralOutlineDark
import com.musfit.ui.theme.TabAccentRole
import com.musfit.ui.theme.tabAccentFor
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import com.musfit.core.designsystem.R as DesignR

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
    val manualLabel = stringResource(R.string.food_manual)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 640.dp)
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.food_shopping_list), style = MaterialTheme.typography.headlineSmall)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.shoppingStartDateInput,
                    onValueChange = onStartDateChanged,
                    label = { Text(stringResource(R.string.food_start)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.shoppingEndDateInput,
                    onValueChange = onEndDateChanged,
                    label = { Text(stringResource(R.string.food_end)) },
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
                Text(
                    stringResource(
                        if (state.isSaving) R.string.food_generating else R.string.food_generate_from_plan,
                    ),
                )
            }
        }

        HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(stringResource(R.string.food_manual_item))
            OutlinedTextField(
                value = state.manualShoppingNameInput,
                onValueChange = onManualNameChanged,
                label = { Text(stringResource(R.string.food_item)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.manualShoppingCategoryInput,
                    onValueChange = onManualCategoryChanged,
                    label = { Text(stringResource(R.string.food_category)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.manualShoppingQuantityInput,
                    onValueChange = onManualQuantityChanged,
                    label = { Text(stringResource(R.string.food_gram_abbreviation)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
            MusFitOutlinedButton(onClick = onAddManualClick, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.food_add_item))
            }
        }

        state.message?.let { message ->
            Text(
                text = message.asString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MusFitTheme.colors.brand,
            )
        }

        if (state.shoppingListGroups.isEmpty()) {
            Text(
                text = stringResource(R.string.food_no_shopping_items),
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
                                            add(item.quantityLabelText.asString())
                                            if (item.isManual) add(manualLabel)
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
                                    label = {
                                        Text(
                                            stringResource(
                                                if (item.isChecked) R.string.food_checked else R.string.food_needed,
                                            ),
                                        )
                                    },
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
                    Text(text = stringResource(R.string.food_database), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = pluralStringResource(
                            R.plurals.food_saved_food_count,
                            state.savedFoods.size,
                            state.savedFoods.size,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = onNewFoodClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand),
                ) {
                    Text(stringResource(R.string.food_new))
                }
            }
        }
        verticalGap("gap-after-header", 14.dp)

        item(key = "import") {
            MusFitOutlinedButton(onClick = onImportStarterFoodsClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.food_import_starter_foods))
            }
        }
        verticalGap("gap-after-import", 14.dp)

        item(key = "compare") {
            MusFitOutlinedButton(onClick = onBarcodeCompareClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.food_compare_barcodes))
            }
        }
        verticalGap("gap-after-compare", 14.dp)

        item(key = "search") {
            OutlinedTextField(
                value = state.foodDatabaseQuery,
                onValueChange = onSearchChanged,
                label = { Text(stringResource(R.string.food_search_foods)) },
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
                Text(
                    stringResource(
                        if (state.isSearchingFoods) R.string.food_searching else R.string.food_search_online_foods,
                    ),
                )
            }
        }
        verticalGap("gap-after-online-search", 14.dp)

        state.message?.let { message ->
            item(key = "message") {
                Text(text = message.asString(), style = MaterialTheme.typography.bodyMedium, color = MusFitTheme.colors.brand)
            }
            verticalGap("gap-after-message", 14.dp)
        }

        if (state.onlineFoodResults.isNotEmpty()) {
            item(key = "online-heading") { SectionTitle(stringResource(R.string.food_online_results)) }
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
            item(key = "duplicates-heading") { SectionTitle(stringResource(R.string.food_potential_duplicates)) }
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

        item(key = "saved-heading") { SectionTitle(stringResource(R.string.food_saved_foods)) }
        verticalGap("gap-after-saved-heading", 4.dp)
        if (foods.isEmpty()) {
            item(key = "saved-empty") {
                Text(
                    text = stringResource(
                        if (state.foodDatabaseQuery.isBlank()) {
                            R.string.food_no_saved_foods
                        } else {
                            R.string.food_no_matching_foods
                        },
                    ),
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
            Text(stringResource(R.string.food_fasting_timer), style = MaterialTheme.typography.headlineSmall)
            Text(
                text = timer.statusLabel.asString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
        }

        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            timer.programs.forEach { program ->
                FilterChip(
                    selected = program.isSelected,
                    onClick = { onProgramSelected(program.id) },
                    label = { Text(program.title.asString()) },
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(stringResource(R.string.food_today))
            ProgressBar(progress = timer.progress.toFloat(), color = MusFitTheme.colors.brand)
            NutritionFactRow(
                stringResource(R.string.food_fast),
                timer.fastingWindowLabel.asString(),
                stringResource(R.string.food_fasting_window),
            )
            NutritionFactRow(
                stringResource(R.string.food_eat),
                timer.eatingWindowLabel.asString(),
                stringResource(R.string.food_eating_window),
            )
        }

        HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)

        OutlinedTextField(
            value = timer.fastingStartInput,
            onValueChange = onStartTimeChanged,
            label = { Text(stringResource(R.string.food_fast_starts)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(stringResource(R.string.food_custom_split))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = timer.customFastingHoursInput,
                    onValueChange = onCustomFastingChanged,
                    label = { Text(stringResource(R.string.food_fast_hours)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = timer.customEatingHoursInput,
                    onValueChange = onCustomEatingChanged,
                    label = { Text(stringResource(R.string.food_eat_hours)) },
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
                Text(stringResource(R.string.food_apply_custom))
            }
        }

        state.message?.let { message ->
            Text(message.asString(), style = MaterialTheme.typography.bodyMedium, color = MusFitTheme.colors.brand)
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
            Text(stringResource(R.string.food_barcode_comparison), style = MaterialTheme.typography.headlineSmall)
            Text(
                text = stringResource(R.string.food_barcode_comparison_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = comparison.leftBarcodeInput,
                onValueChange = { onBarcodeChanged(BarcodeComparisonSide.Left, it) },
                label = { Text(stringResource(R.string.food_left_barcode)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = comparison.rightBarcodeInput,
                onValueChange = { onBarcodeChanged(BarcodeComparisonSide.Right, it) },
                label = { Text(stringResource(R.string.food_right_barcode)) },
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
            Text(
                stringResource(
                    if (state.barcodeComparison.isLoading) R.string.food_comparing else R.string.food_compare,
                ),
            )
        }

        state.message?.let { message ->
            Text(message.asString(), style = MaterialTheme.typography.bodyMedium, color = MusFitTheme.colors.brand)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BarcodeComparisonItemCard(
                title = stringResource(R.string.food_left),
                item = comparison.leftItem,
                modifier = Modifier.weight(1f),
            )
            BarcodeComparisonItemCard(
                title = stringResource(R.string.food_right),
                item = comparison.rightItem,
                modifier = Modifier.weight(1f),
            )
        }

        if (comparison.highlights.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionTitle(stringResource(R.string.food_per_hundred_grams_comparison))
                Column(modifier = Modifier.fillMaxWidth()) {
                    comparison.highlights.forEach { highlight ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(highlight.label.asString(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(
                                text = stringResource(
                                    R.string.food_comparison_values,
                                    highlight.leftValue.asString(),
                                    highlight.rightValue.asString(),
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MusFitTheme.colors.onSurfaceVariant,
                            )
                            Text(
                                text = when (highlight.winnerSide) {
                                    BarcodeComparisonSide.Left -> stringResource(R.string.food_left)
                                    BarcodeComparisonSide.Right -> stringResource(R.string.food_right)
                                    null -> stringResource(R.string.food_even)
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
                text = stringResource(R.string.food_no_product_loaded),
                style = MaterialTheme.typography.bodySmall,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
        } else {
            Text(item.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                text = listOfNotNull(item.sourceText.asString(), item.brand, item.barcode).joinToString(" - "),
                style = MaterialTheme.typography.bodySmall,
                color = MusFitTheme.colors.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.food_per_hundred_grams_macros,
                    item.caloriesPer100g.formatNutritionDisplay(),
                    item.proteinPer100g.formatNutritionDisplay(),
                    item.carbsPer100g.formatNutritionDisplay(),
                    item.fatPer100g.formatNutritionDisplay(),
                ),
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
                    text = listOfNotNull(
                        result.brand,
                        result.category,
                        stringResource(R.string.food_calories_per_hundred_grams, result.caloriesPer100g.roundToInt()),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            MusFitOutlinedButton(onClick = onSaveClick, enabled = !isSaving) {
                Text(stringResource(DesignR.string.common_save))
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
                    text = stringResource(
                        R.string.food_duplicate_reason,
                        group.reasonText.asString(),
                        pluralStringResource(
                            R.plurals.food_food_count,
                            group.duplicateFoodIds.size + 1,
                            group.duplicateFoodIds.size + 1,
                        ),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }
            MusFitOutlinedButton(
                onClick = { onMergeDuplicateFoodsClick(group.primaryFoodId, group.duplicateFoodIds) },
                enabled = !isSaving,
            ) {
                Text(stringResource(R.string.food_merge))
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
                food.trust.label.asString(),
                food.brand,
                stringResource(R.string.food_value_grams, food.defaultServingGrams.formatNutritionDisplay()),
                stringResource(R.string.food_value_kcal, food.caloriesPerServingKcal.formatNutritionDisplay()),
            ).joinToString(stringResource(R.string.food_separator)),
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
        MusFitOutlinedButton(onClick = onFavoriteClick) {
            Text(stringResource(if (food.isFavorite) R.string.food_starred else R.string.food_star))
        }
        MusFitOutlinedButton(onClick = onReportClick) { Text(stringResource(R.string.food_report)) }
        MusFitOutlinedButton(onClick = onDetailClick) { Text(stringResource(R.string.food_detail)) }
        MusFitOutlinedButton(onClick = onEditClick) { Text(stringResource(DesignR.string.common_edit)) }
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
                contentDescription = stringResource(
                    if (food.isFavorite) {
                        R.string.food_remove_named_from_favorites
                    } else {
                        R.string.food_add_named_to_favorites
                    },
                    food.name,
                ),
            )
        }
        IconButton(
            onClick = onReportClick,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(Icons.Outlined.ErrorOutline, contentDescription = stringResource(R.string.food_report_named, food.name))
        }
        IconButton(
            onClick = onDetailClick,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = stringResource(R.string.food_view_named_details, food.name),
            )
        }
        IconButton(
            onClick = onEditClick,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.food_edit_named, food.name))
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
internal fun percentOfDayValue(amountKcal: Double, dayBudgetKcal: Double): Int? = if (dayBudgetKcal > 0.0) {
    (amountKcal / dayBudgetKcal * 100).roundToInt()
} else {
    null
}

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
@Composable
private fun FoodUiState.detailTargetMealLabel(): String = (
    visibleMealDefinitions.firstOrNull { it.id == mealType }?.titleText?.asString()
        ?: selectedMealTitleText.asString()
    )
    .lowercase(LocalConfiguration.current.locales[0])

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
            Text(stringResource(R.string.food_not_found), style = MusFitTheme.typography.titleMedium)
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
                Text(it.asString(), style = MusFitTheme.typography.bodyMedium, color = MusFitTheme.colors.brand)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            TonalIconSquare(
                icon = Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.food_edit_food),
                onClick = onEditClick,
            )
            PillButton(
                text = stringResource(
                    R.string.food_detail_action,
                    state.foodEntryActionVerb(),
                    state.detailTargetMealLabel(),
                    LocalizedFormatter.integer(
                        amount.caloriesKcal.roundToInt().toLong(),
                        locale = LocalConfiguration.current.locales[0],
                    ),
                ),
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
                text = listOfNotNull(food.brand, food.category).joinToString(" · ").ifBlank { food.sourceText.asString() },
                style = MusFitTheme.typography.bodySmall,
                color = MusFitTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onFavoriteClick) {
            Icon(
                imageVector = if (food.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = stringResource(
                    if (food.isFavorite) R.string.food_remove_from_favorites else R.string.food_add_to_favorites,
                ),
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
                FoodTrustChip(Icons.Outlined.Verified, MusFitTheme.colors.brand, food.sourceText.asString())

            FoodTrustLevel.Manual ->
                FoodTrustChip(
                    Icons.Outlined.Edit,
                    MusFitTheme.colors.onSurfaceVariant,
                    stringResource(R.string.food_edited_by_you),
                )

            FoodTrustLevel.NeedsReview ->
                FoodTrustChip(Icons.Outlined.ErrorOutline, MusFitTheme.colors.warning, food.trust.label.asString())
        }
        TrustActionChip(text = food.trust.actionLabel.asString(), onClick = onCorrectClick)
        TrustActionChip(
            text = stringResource(if (food.trust.isReported) R.string.food_reported else R.string.food_report),
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
                    contentDescription = stringResource(R.string.food_remove_ten_grams),
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
                        text = stringResource(R.string.food_gram_abbreviation),
                        style = MusFitTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.W600),
                        color = MusFitTheme.colors.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                    )
                }
                StepperCircleButton(
                    icon = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.food_add_ten_grams),
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
                        text = stringResource(R.string.food_grams_word),
                        selected = !matchesServing,
                        onClick = { onQuantityChanged(food.defaultServingGrams.formatNutritionDisplay()) },
                        unselectedContainer = MusFitTheme.colors.surfaceVariant,
                    )
                    food.servings.forEach { serving ->
                        SelectableChip(
                            text = stringResource(
                                R.string.food_named_serving_grams,
                                serving.label,
                                serving.grams.formatNutritionDisplay(),
                            ),
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
@Suppress("LongMethod")
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
                        text = LocalizedFormatter.integer(
                            amountKcal.roundToInt().toLong(),
                            locale = LocalConfiguration.current.locales[0],
                        ),
                        style = MusFitTheme.typography.displaySmall.copy(
                            fontSize = 36.sp,
                            lineHeight = 38.sp,
                            fontWeight = FontWeight.W800,
                            letterSpacing = (-1).sp,
                        ),
                        color = accent.onContainer,
                    )
                    Text(
                        text = stringResource(R.string.food_kcal),
                        style = MusFitTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.W600),
                        color = accent.onContainerVariant,
                        modifier = Modifier.padding(start = 5.dp, bottom = 5.dp),
                    )
                }
                Text(
                    text = stringResource(
                        R.string.food_detail_amount_calories,
                        grams.formatNutritionDisplay(),
                        food.caloriesPer100g.roundToInt(),
                    ),
                    style = MusFitTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = accent.onContainerVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            percentOfDayValue(amountKcal, dayBudgetKcal)?.let { percent ->
                Surface(
                    color = MusFitTheme.colors.surface.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(99.dp),
                ) {
                    Text(
                        text = stringResource(R.string.food_percent_of_day, percent),
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
        Triple(stringResource(R.string.food_carbs), amount.carbsGrams, state.carbsGoalGrams),
        Triple(stringResource(R.string.food_protein), amount.proteinGrams, state.proteinGoalGrams),
        Triple(stringResource(R.string.food_fat), amount.fatGrams, state.fatGoalGrams),
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
    val value = stringResource(R.string.food_value_grams, amountGrams.formatNutritionDisplay())
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
    val micronutrientRows = foodDetailMicronutrientRows(food = food, scale = scale)
    Surface(color = MusFitTheme.colors.surface, shape = RoundedCornerShape(24.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
        ) {
            HairlineDetailRow(
                stringResource(R.string.food_sugar),
                stringResource(R.string.food_value_grams, (food.sugarPer100g * scale).formatNutritionDisplay()),
            )
            HairlineDetailRow(
                stringResource(R.string.food_fiber),
                stringResource(R.string.food_value_grams, (food.fiberPer100g * scale).formatNutritionDisplay()),
            )
            HairlineDetailRow(
                label = stringResource(R.string.food_salt),
                value = stringResource(
                    R.string.food_value_grams,
                    saltGramsFromSodiumMg(food.sodiumMgPer100g * scale).formatNutritionDisplay(),
                ),
                showDivider = expanded,
            )
            if (expanded) {
                micronutrientRows.forEach { (label, value) ->
                    HairlineDetailRow(label, value)
                }
            }
            FoodDetailMicronutrientToggle(
                expanded = expanded,
                micronutrientCount = micronutrientRows.size,
                onToggle = { expanded = !expanded },
            )
        }
    }
}

@Composable
private fun foodDetailMicronutrientRows(
    food: SavedFoodUiState,
    scale: Double,
): List<Pair<String, String>> = listOf(
    stringResource(R.string.food_saturated_fat) to stringResource(
        R.string.food_value_grams,
        (food.saturatedFatPer100g * scale).formatNutritionDisplay(),
    ),
    stringResource(R.string.food_potassium) to stringResource(
        R.string.food_value_milligrams,
        (food.potassiumMgPer100g * scale).formatMicronutrientDisplay(),
    ),
    stringResource(R.string.food_calcium) to stringResource(
        R.string.food_value_milligrams,
        (food.calciumMgPer100g * scale).formatMicronutrientDisplay(),
    ),
    stringResource(R.string.food_iron) to stringResource(
        R.string.food_value_milligrams,
        (food.ironMgPer100g * scale).formatMicronutrientDisplay(),
    ),
    stringResource(R.string.food_vitamin_d) to stringResource(
        R.string.food_value_micrograms,
        (food.vitaminDMcgPer100g * scale).formatMicronutrientDisplay(),
    ),
    stringResource(R.string.food_vitamin_c) to stringResource(
        R.string.food_value_milligrams,
        (food.vitaminCMgPer100g * scale).formatMicronutrientDisplay(),
    ),
    stringResource(R.string.food_magnesium) to stringResource(
        R.string.food_value_milligrams,
        (food.magnesiumMgPer100g * scale).formatMicronutrientDisplay(),
    ),
)

@Composable
private fun FoodDetailMicronutrientToggle(
    expanded: Boolean,
    micronutrientCount: Int,
    onToggle: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 11.dp),
    ) {
        Text(
            text = if (expanded) {
                stringResource(R.string.food_hide_micronutrients)
            } else {
                pluralStringResource(
                    R.plurals.food_all_micronutrients_count,
                    micronutrientCount,
                    micronutrientCount,
                )
            },
            style = MusFitTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, fontWeight = FontWeight.W800),
            color = MusFitTheme.colors.brand,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = MusFitTheme.colors.brand,
            modifier = Modifier.size(18.dp),
        )
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
                text = stringResource(R.string.food_edit_diary_item),
                style = MusFitTheme.typography.headlineSmall.copy(fontSize = 22.sp, fontWeight = FontWeight.W800),
                color = MusFitTheme.colors.onSurface,
            )
            Text(
                text = editor.name.ifBlank { stringResource(R.string.food_food_item) },
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
            label = stringResource(R.string.food_amount_grams),
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
                    text = stringResource(R.string.food_preview),
                    style = MusFitTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.W800),
                    color = accent.onContainer,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.food_integer_kcal, editor.previewCaloriesKcal.roundToInt()),
                    style = MusFitTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.W800),
                    color = accent.onContainer,
                )
                Text(
                    text = stringResource(
                        R.string.food_macro_summary_values,
                        editor.previewCarbsGrams.formatNutritionDisplay(),
                        editor.previewProteinGrams.formatNutritionDisplay(),
                        editor.previewFatGrams.formatNutritionDisplay(),
                    ),
                    style = MusFitTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = accent.onContainerVariant,
                    maxLines = 1,
                )
            }
        }

        state.message?.let { message ->
            Text(
                text = message.asString(),
                style = MusFitTheme.typography.bodyMedium,
                color = MusFitTheme.colors.brand,
            )
        }

        PillButton(
            text = stringResource(if (state.isSaving) R.string.food_saving else R.string.food_save_changes),
            onClick = onSaveClick,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )

        if (editor.isPlanned) {
            PillButton(
                text = stringResource(R.string.food_mark_logged),
                onClick = onMarkLoggedClick,
                enabled = !state.isSaving,
                containerColor = accent.container,
                contentColor = accent.onContainer,
                height = 48.dp,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionOverline(
            stringResource(R.string.food_copy_item).uppercase(LocalConfiguration.current.locales[0]),
        )
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
                text = stringResource(R.string.food_tomorrow),
                selected = null,
                onClick = onCopyTomorrowClick,
            )
        }

        PillButton(
            text = stringResource(R.string.food_delete_from_diary_action),
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
            title = stringResource(R.string.food_delete_from_diary),
            body = stringResource(
                R.string.food_delete_diary_item_body,
                editor.name.ifBlank { stringResource(R.string.food_this_item) },
            ),
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
                Text(stringResource(DesignR.string.common_delete), color = MusFitTheme.colors.onDestructiveContainer)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(DesignR.string.common_cancel)) }
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
    val defaultLabel = stringResource(R.string.food_default)
    val customLabel = stringResource(R.string.food_custom)
    val hiddenLabel = stringResource(R.string.food_hidden)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 580.dp)
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.food_meal_settings), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.food_meal_settings_summary),
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
                        Text(meal.titleText.asString(), style = MaterialTheme.typography.titleSmall)
                        Text(
                            listOfNotNull(
                                if (meal.isDefault) defaultLabel else customLabel,
                                meal.timeLabelText.asString(),
                                stringResource(R.string.food_order_value, meal.sortOrder),
                                if (meal.isHidden) hiddenLabel else null,
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
                        Text(stringResource(DesignR.string.common_edit))
                    }
                }
                HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(
                stringResource(
                    if (state.editingMealDefinitionId == null) {
                        R.string.food_add_custom_meal
                    } else {
                        R.string.food_edit_meal
                    },
                ),
            )
            OutlinedTextField(
                value = state.customMealNameInput,
                onValueChange = onNameChanged,
                label = { Text(stringResource(R.string.food_meal_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.customMealTimeInput,
                    onValueChange = onTimeChanged,
                    label = { Text(stringResource(R.string.food_time_hh_mm)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.customMealSortOrderInput,
                    onValueChange = onSortOrderChanged,
                    label = { Text(stringResource(R.string.food_order)) },
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
                Text(stringResource(if (state.isSaving) R.string.food_saving else R.string.food_save_meal))
            }
        }

        state.message?.let { Text(it.asString(), color = MusFitTheme.colors.brand) }
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
            title = stringResource(if (isExistingFood) R.string.food_edit_food else R.string.food_new_food),
            onBack = onBack,
            trailing = {
                PillButton(
                    text = stringResource(if (state.isSaving) R.string.food_saving else R.string.food_save),
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
                        FoodTrustChip(Icons.Outlined.Verified, MusFitTheme.colors.brand, savedSource.sourceText.asString())

                    FoodTrustLevel.Manual ->
                        FoodTrustChip(Icons.Outlined.Edit, MusFitTheme.colors.onSurfaceVariant, savedSource.trust.label.asString())

                    FoodTrustLevel.NeedsReview ->
                        FoodTrustChip(Icons.Outlined.ErrorOutline, MusFitTheme.colors.warning, savedSource.trust.label.asString())
                }
            }

            SectionOverline(
                stringResource(R.string.food_identity).uppercase(LocalConfiguration.current.locales[0]),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FieldCell(
                    label = stringResource(R.string.food_name),
                    value = editor.name,
                    onValueChange = onNameChanged,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    FieldCell(
                        label = stringResource(R.string.food_brand),
                        value = editor.brand,
                        onValueChange = onBrandChanged,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                    )
                    FieldCell(
                        label = stringResource(R.string.food_category),
                        value = editor.category,
                        onValueChange = onCategoryChanged,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                    )
                }
                FieldCell(
                    label = stringResource(R.string.food_add_mode_barcode),
                    value = editor.barcode,
                    onValueChange = onBarcodeChanged,
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SectionOverline(
                stringResource(R.string.food_serving).uppercase(LocalConfiguration.current.locales[0]),
            )
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
                            text = stringResource(
                                R.string.food_serving_definition,
                                editor.servingName.ifBlank {
                                    stringResource(R.string.food_serving).lowercase(LocalConfiguration.current.locales[0])
                                },
                                editor.servingGrams.ifBlank { "0" },
                            ),
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
                        label = stringResource(R.string.food_serving_name),
                        value = editor.servingName,
                        onValueChange = onServingNameChanged,
                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 20.dp, bottomEnd = 8.dp),
                        modifier = Modifier.weight(1f),
                    )
                    FieldCell(
                        label = stringResource(R.string.food_serving_grams_label),
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
                        text = stringResource(R.string.food_favorite),
                        style = MusFitTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, fontWeight = FontWeight.W700),
                        color = MusFitTheme.colors.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    BrandSwitch(checked = editor.isFavorite, onCheckedChange = onFavoriteChanged)
                }
            }

            state.message?.let { message ->
                Text(
                    text = message.asString(),
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
                    text = stringResource(R.string.food_duplicate),
                    onClick = onDuplicateClick,
                    enabled = !state.isSaving,
                    icon = Icons.Outlined.ContentCopy,
                    containerColor = MusFitTheme.colors.surfaceVariant,
                    contentColor = MusFitTheme.colors.onSurface,
                    height = 52.dp,
                    modifier = Modifier.weight(1f),
                )
                PillButton(
                    text = stringResource(R.string.food_delete),
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
            title = stringResource(R.string.food_delete_saved_food),
            body = stringResource(
                R.string.food_delete_saved_food_body,
                editor.name.ifBlank { stringResource(R.string.food_this_food) },
            ),
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
                text = stringResource(R.string.food_nutrition_label_scan),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.food_review_fields_before_saving),
                style = MaterialTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
        }

        MusFitOutlinedButton(
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.food_capture_label_photo))
        }

        OutlinedTextField(
            value = editor.name,
            onValueChange = onNameChanged,
            label = { Text(stringResource(R.string.food_food_name)) },
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
                label = { Text(stringResource(R.string.food_brand)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = editor.servingGrams,
                onValueChange = onServingChanged,
                label = { Text(stringResource(R.string.food_serving_grams_label)) },
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
                label = { Text(stringResource(R.string.food_serving)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = editor.category,
                onValueChange = onCategoryChanged,
                label = { Text(stringResource(R.string.food_category)) },
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
                text = message.asString(),
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
            Text(stringResource(if (state.isSaving) R.string.food_saving else R.string.food_save_food))
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
        SectionOverline(
            stringResource(R.string.food_nutrition_per_hundred_grams)
                .uppercase(LocalConfiguration.current.locales[0]),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // 2×2 macro grid with macro-tinted labels (9e).
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                FieldCell(
                    label = stringResource(R.string.food_calories),
                    value = editor.caloriesPer100g,
                    onValueChange = onCaloriesChanged,
                    shape = gridGroupShape(row = 0, rowCount = 2, column = 0, columnCount = 2, outer = 20.dp),
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                )
                FieldCell(
                    label = stringResource(R.string.food_carbs),
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
                    label = stringResource(R.string.food_protein),
                    value = editor.proteinPer100g,
                    onValueChange = onProteinChanged,
                    shape = gridGroupShape(row = 1, rowCount = 2, column = 0, columnCount = 2, outer = 20.dp),
                    keyboardType = KeyboardType.Decimal,
                    labelColor = colors.macroProtein,
                    modifier = Modifier.weight(1f),
                )
                FieldCell(
                    label = stringResource(R.string.food_fat),
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
                    text = if (showMicronutrients) {
                        stringResource(R.string.food_hide_micronutrients)
                    } else {
                        stringResource(R.string.food_micronutrients_count, 10)
                    },
                    style = MusFitTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, fontWeight = FontWeight.W800),
                    color = colors.brand,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (showMicronutrients) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
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
                        Triple(stringResource(R.string.food_fiber), editor.fiberPer100g, onFiberChanged),
                        Triple(stringResource(R.string.food_sugar), editor.sugarPer100g, onSugarChanged),
                    ),
                    listOf(
                        Triple(stringResource(R.string.food_saturated_fat_grams), editor.saturatedFatPer100g, onSaturatedFatChanged),
                        Triple(stringResource(R.string.food_sodium_milligrams), editor.sodiumMgPer100g, onSodiumChanged),
                    ),
                    listOf(
                        Triple(stringResource(R.string.food_potassium_milligrams), editor.potassiumMgPer100g, onPotassiumChanged),
                        Triple(stringResource(R.string.food_calcium_milligrams), editor.calciumMgPer100g, onCalciumChanged),
                    ),
                    listOf(
                        Triple(stringResource(R.string.food_iron_milligrams), editor.ironMgPer100g, onIronChanged),
                        Triple(stringResource(R.string.food_vitamin_d_micrograms), editor.vitaminDMcgPer100g, onVitaminDChanged),
                    ),
                    listOf(
                        Triple(stringResource(R.string.food_vitamin_c_milligrams), editor.vitaminCMgPer100g, onVitaminCChanged),
                        Triple(stringResource(R.string.food_magnesium_milligrams), editor.magnesiumMgPer100g, onMagnesiumChanged),
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
            title = stringResource(R.string.food_nutrition_goals),
            onBack = onBack,
            trailing = {
                PillButton(
                    text = stringResource(if (state.isSaving) R.string.food_saving else R.string.food_save),
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
                        contentDescription = stringResource(R.string.food_remove_fifty_kcal),
                        onClick = { onCaloriesChanged(steppedCaloriesInput(editor.caloriesKcalInput, -50)) },
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        val calories = editor.caloriesKcalInput.toDoubleOrNull()?.roundToInt() ?: 0
                        val locale = LocalConfiguration.current.locales[0]
                        Text(
                            text = LocalizedFormatter.integer(calories.toLong(), locale = locale),
                            style = HeroNumberMediumStyle.copy(letterSpacing = (-1).sp),
                            color = accent.onContainer,
                            maxLines = 1,
                        )
                        Text(
                            text = stringResource(
                                R.string.food_kcal_daily_target,
                                editor.modeInput.label().lowercase(locale),
                            ),
                            style = MusFitTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = accent.onContainerVariant,
                            modifier = Modifier.padding(top = 3.dp),
                            maxLines = 1,
                        )
                    }
                    StepperCircleButton(
                        icon = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.food_add_fifty_kcal),
                        filled = true,
                        onClick = { onCaloriesChanged(steppedCaloriesInput(editor.caloriesKcalInput, 50)) },
                    )
                }
            }

            GoalMacroSplitCard(editor = editor)

            // Editable gram targets feed the split card live.
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                FieldCell(
                    label = stringResource(R.string.food_carbs_grams),
                    value = editor.carbsGramsInput,
                    onValueChange = onCarbsChanged,
                    shape = gridGroupShape(row = 0, rowCount = 1, column = 0, columnCount = 3, outer = 20.dp),
                    keyboardType = KeyboardType.Decimal,
                    labelColor = colors.macroCarbs,
                    modifier = Modifier.weight(1f),
                )
                FieldCell(
                    label = stringResource(R.string.food_protein_grams),
                    value = editor.proteinGramsInput,
                    onValueChange = onProteinChanged,
                    shape = gridGroupShape(row = 0, rowCount = 1, column = 1, columnCount = 3, outer = 20.dp),
                    keyboardType = KeyboardType.Decimal,
                    labelColor = colors.macroProtein,
                    modifier = Modifier.weight(1f),
                )
                FieldCell(
                    label = stringResource(R.string.food_fat_grams),
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
                        label = stringResource(R.string.food_include_training_calories),
                        checked = editor.includeTrainingInput,
                        onCheckedChange = onTrainingChanged,
                    )
                    HorizontalDivider(thickness = 1.dp, color = colors.outline)
                    GoalToggleRow(
                        label = stringResource(R.string.food_count_net_carbs),
                        checked = editor.useNetCarbsInput,
                        onCheckedChange = onNetCarbsChanged,
                    )
                }
            }

            SectionOverline(
                stringResource(R.string.food_programs).uppercase(LocalConfiguration.current.locales[0]),
            )
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
                        text = pluralStringResource(
                            R.plurals.food_all_programs,
                            state.foodPrograms.size,
                            state.foodPrograms.size,
                        ),
                        style = MusFitTheme.typography.bodyMedium.copy(fontSize = 12.5.sp, fontWeight = FontWeight.W800),
                        color = colors.brand,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colors.brand,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }

            SectionOverline(
                stringResource(R.string.food_advanced_targets).uppercase(LocalConfiguration.current.locales[0]),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    FieldCell(
                        label = stringResource(R.string.food_fiber_grams),
                        value = editor.fiberGramsInput,
                        onValueChange = onFiberChanged,
                        shape = gridGroupShape(row = 0, rowCount = 2, column = 0, columnCount = 2, outer = 20.dp),
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                    )
                    FieldCell(
                        label = stringResource(R.string.food_sugar_grams),
                        value = editor.sugarGramsInput,
                        onValueChange = onSugarChanged,
                        shape = gridGroupShape(row = 0, rowCount = 2, column = 1, columnCount = 2, outer = 20.dp),
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    FieldCell(
                        label = stringResource(R.string.food_saturated_fat_grams),
                        value = editor.saturatedFatGramsInput,
                        onValueChange = onSaturatedFatChanged,
                        shape = gridGroupShape(row = 1, rowCount = 2, column = 0, columnCount = 2, outer = 20.dp),
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                    )
                    FieldCell(
                        label = stringResource(R.string.food_sodium_milligrams),
                        value = editor.sodiumMgInput,
                        onValueChange = onSodiumChanged,
                        shape = gridGroupShape(row = 1, rowCount = 2, column = 1, columnCount = 2, outer = 20.dp),
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            state.message?.let {
                Text(it.asString(), style = MusFitTheme.typography.bodyMedium, color = colors.brand)
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
        Triple(stringResource(R.string.food_carbs), carbsPct, editor.carbsGramsInput),
        Triple(stringResource(R.string.food_protein), proteinPct, editor.proteinGramsInput),
        Triple(stringResource(R.string.food_fat), fatPct, editor.fatGramsInput),
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
                    text = stringResource(R.string.food_macro_split),
                    style = MusFitTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.W800),
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = editor.modeInput.label(),
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
                                text = stringResource(R.string.food_percentage, entry.second),
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
                            text = stringResource(R.string.food_value_grams, entry.third.ifBlank { "0" }),
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
                    text = program.title.asString(),
                    style = MusFitTheme.typography.titleSmall.copy(fontWeight = FontWeight.W800, letterSpacing = (-0.2).sp),
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = program.subtitle.asString(),
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
                    text = stringResource(if (program.isSelected) R.string.food_applied else R.string.food_apply),
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
            val editorTitle = stringResource(
                if (state.recipeEditor.editingRecipeId == null) R.string.food_new_recipe else R.string.food_edit_recipe,
            )
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

@Composable
private fun RecipeDiscoveryFilter.chipLabel(): String = stringResource(
    if (this == RecipeDiscoveryFilter.All) R.string.food_filter_for_you else labelResource(),
)

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
        .firstOrNull { it.id == state.recipeBrowserMealType }?.titleText?.asString()
        ?: state.recipeBrowserMealType.mealTitleText().asString()
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
            title = stringResource(R.string.food_recipes),
            onBack = onBackClick,
            trailing = {
                TonalHeaderIconButton(
                    icon = Icons.Outlined.Search,
                    contentDescription = stringResource(R.string.food_search_recipes),
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
                                text = stringResource(R.string.food_find_recipes),
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
                        text = it.asString(),
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
                    RecipeBrowserEmptyText(stringResource(R.string.food_no_recipes_match))
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
                    SectionOverline(
                        stringResource(R.string.food_my_recipes).uppercase(LocalConfiguration.current.locales[0]),
                    )
                }
                verticalGap("gap-after-my-recipes-heading", 12.dp)
                if (state.recipes.isEmpty()) {
                    item(
                        key = "my-recipes-empty",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        RecipeBrowserEmptyText(stringResource(R.string.food_no_saved_recipes))
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
                                    pluralStringResource(
                                        R.plurals.food_servings_count,
                                        if (recipe.servings == 1.0) 1 else 2,
                                        recipe.servings.formatNutritionDisplay(),
                                    ),
                                ).joinToString(stringResource(R.string.food_separator)),
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
                                                contentDescription = stringResource(
                                                    if (recipe.isFavorite) R.string.food_unfavorite else R.string.food_favorite,
                                                ),
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
                                                    contentDescription = stringResource(
                                                        R.string.food_action_recipe,
                                                        state.foodEntryActionVerb(),
                                                    ),
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
                text = pluralStringResource(
                    R.plurals.food_my_recipes_count,
                    state.recipes.size,
                    state.recipes.size,
                ),
                onClick = { showMyRecipes = !showMyRecipes },
                icon = Icons.Outlined.Bookmark,
                containerColor = accent.container,
                contentColor = accent.onContainer,
                height = 54.dp,
                modifier = Modifier.weight(1f),
            )
            PillButton(
                text = stringResource(R.string.food_new_recipe),
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
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.food_previous_day),
                tint = colors.onSurface,
            )
        }
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clickable(onClickLabel = stringResource(R.string.food_jump_to_today), onClick = onTodayClick),
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
                text = stringResource(R.string.food_today),
                style = MusFitTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                color = colors.brand,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        IconButton(onClick = onNextDayClick) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.food_next_day),
                tint = colors.onSurface,
            )
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
            label = stringResource(R.string.food_serving_short),
            value = state.recipeServingsToLog,
            onValueChange = onServingsChanged,
            shape = RoundedCornerShape(16.dp),
            keyboardType = KeyboardType.Decimal,
            modifier = Modifier.width(72.dp),
        )
    }
}

@Composable
@Suppress("LongMethod")
internal fun RecipeFeaturedCard(
    item: RecipeDiscoveryItemUiState,
    mealTitle: String,
    isSaving: Boolean,
    onPlanClick: (String) -> Unit,
    onReviewClick: (String) -> Unit,
) {
    val accent = tabAccentFor(TabAccentRole.Food)
    val sourceRecipeId = item.sourceRecipeId
    val locale = LocalConfiguration.current.locales[0]
    val actionText = if (sourceRecipeId != null) {
        stringResource(R.string.food_plan_meal, mealTitle.lowercase(locale))
    } else {
        stringResource(R.string.food_review)
    }
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
            text = item.titleText.asString(),
            style = MusFitTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.W800),
            color = accent.onContainer,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = buildList {
                add(stringResource(R.string.food_value_kcal, item.caloriesKcal.formatNutritionDisplay()))
                add(stringResource(R.string.food_value_protein_grams, item.proteinGrams.formatNutritionDisplay()))
                item.tagTexts.firstOrNull()?.asString()?.let(::add)
            }.joinToString(stringResource(R.string.food_separator)),
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
                    text = item.titleText.asString(),
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
                        add(stringResource(R.string.food_value_kcal, item.caloriesKcal.formatNutritionDisplay()))
                        add(stringResource(R.string.food_protein_abbreviation, item.proteinGrams.formatNutritionDisplay()))
                        if (item.isSavedRecipe) add(stringResource(R.string.food_saved))
                    }.joinToString(stringResource(R.string.food_separator)),
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
                text = stringResource(if (editor.editingRecipeId == null) R.string.food_recipe else R.string.food_edit_recipe),
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
        SectionOverline(
            stringResource(R.string.food_recipe).uppercase(LocalConfiguration.current.locales[0]),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FieldCell(
                label = stringResource(R.string.food_recipe_name),
                value = editor.name,
                onValueChange = onNameChanged,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                FieldCell(
                    label = stringResource(R.string.food_category),
                    value = editor.category,
                    onValueChange = onCategoryChanged,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                )
                FieldCell(
                    label = stringResource(R.string.food_serving),
                    value = editor.servingName,
                    onValueChange = onServingNameChanged,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                FieldCell(
                    label = stringResource(R.string.food_servings),
                    value = editor.servingsCount,
                    onValueChange = onServingsCountChanged,
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 20.dp, bottomEnd = 8.dp),
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                )
                FieldCell(
                    label = stringResource(R.string.food_cooked_yield_grams),
                    value = editor.cookedYieldGrams,
                    onValueChange = onCookedYieldChanged,
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 20.dp),
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Text(
            text = stringResource(
                R.string.food_grams_per_serving,
                editor.servingGrams.ifBlank { "0" },
                editor.servingName.ifBlank {
                    stringResource(R.string.food_serving).lowercase(LocalConfiguration.current.locales[0])
                },
            ),
            style = MusFitTheme.typography.bodySmall,
            color = MusFitTheme.colors.onSurfaceVariant,
        )
        SectionOverline(
            stringResource(R.string.food_ingredients).uppercase(LocalConfiguration.current.locales[0]),
        )
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
                label = stringResource(R.string.food_amount_grams_short),
                value = editor.ingredientQuantityGrams,
                onValueChange = onIngredientQuantityChanged,
                shape = RoundedCornerShape(20.dp),
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
            )
            PillButton(
                text = stringResource(R.string.food_add),
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
                            value = stringResource(
                                R.string.food_ingredient_amount,
                                ingredient.unitQuantity.formatNutritionDisplay(),
                                ingredient.unitLabel,
                                ingredient.quantityGrams.formatNutritionDisplay(),
                            ),
                            showDivider = index != editor.ingredients.lastIndex,
                        )
                    }
                }
            }
        }
        state.message?.let { Text(it.asString(), style = MusFitTheme.typography.bodyMedium, color = MusFitTheme.colors.brand) }
        PillButton(
            text = stringResource(if (state.isSaving) R.string.food_saving else R.string.food_save_recipe),
            onClick = onSaveClick,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        if (editor.editingRecipeId != null) {
            PillButton(
                text = stringResource(R.string.food_delete_recipe_action),
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
            title = stringResource(R.string.food_delete_recipe),
            body = stringResource(
                R.string.food_delete_recipe_body,
                editor.name.ifBlank { stringResource(R.string.food_this_recipe) },
            ),
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
        SectionTitle(stringResource(R.string.food_saved_recipes))
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
                                pluralStringResource(
                                    R.plurals.food_servings_count,
                                    if (recipe.servings == 1.0) 1 else 2,
                                    recipe.servings.formatNutritionDisplay(),
                                ),
                                stringResource(R.string.food_yield_grams, recipe.cookedYieldGrams.formatNutritionDisplay()),
                                if (recipe.isFavorite) stringResource(R.string.food_favorite) else null,
                            ).joinToString(stringResource(R.string.food_separator)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MusFitTheme.colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
                        MusFitOutlinedButton(onClick = { onFavoriteClick(recipe.id, !recipe.isFavorite) }) {
                            Text(stringResource(if (recipe.isFavorite) R.string.food_starred else R.string.food_star))
                        }
                        MusFitOutlinedButton(onClick = { onEditRecipeClick(recipe.id) }) {
                            Text(stringResource(DesignR.string.common_edit))
                        }
                        MusFitOutlinedButton(onClick = { onDuplicateRecipeClick(recipe.id) }) {
                            Text(stringResource(R.string.food_copy))
                        }
                    }
                }
                HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
            }
        }
    }
}

@androidx.annotation.StringRes
private fun RecipeDiscoveryFilter.labelResource(): Int = when (this) {
    RecipeDiscoveryFilter.All -> R.string.food_filter_all
    RecipeDiscoveryFilter.HighProtein -> R.string.food_recipe_tag_high_protein
    RecipeDiscoveryFilter.LowCarb -> R.string.food_recipe_tag_low_carb
    RecipeDiscoveryFilter.Vegetarian -> R.string.food_recipe_tag_vegetarian
    RecipeDiscoveryFilter.Quick -> R.string.food_recipe_tag_quick
    RecipeDiscoveryFilter.Favorites -> R.string.food_filter_favorites
    RecipeDiscoveryFilter.Program -> R.string.food_filter_program
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
    val titleText: UiText = UiText.Verbatim(title),
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
                    RecipeBrowserLane(id = meal.id, title = meal.title, items = laneItems, titleText = meal.titleText)
                }
            }
    return lanes.ifEmpty {
        if (items.isEmpty()) {
            emptyList()
        } else {
            listOf(RecipeBrowserLane("all", "all", items, uiText(R.string.food_all_recipes)))
        }
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
        SectionTitle(stringResource(R.string.food_recipe_discovery))
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RecipeDiscoveryFilter.entries.forEach { filter ->
                FilterChip(
                    selected = state.recipeDiscovery.filter == filter,
                    onClick = { onFilterChanged(filter) },
                    label = { Text(stringResource(filter.labelResource())) },
                )
            }
        }
        if (state.recipeDiscovery.visibleItems.isEmpty()) {
            Text(
                text = stringResource(R.string.food_no_recipes_match_filter),
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
                            Text(
                                item.titleText.asString(),
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
                            Text(
                                text = stringResource(
                                    R.string.food_recipe_macro_summary,
                                    item.caloriesKcal.formatNutritionDisplay(),
                                    item.proteinGrams.formatNutritionDisplay(),
                                    item.carbsGrams.formatNutritionDisplay(),
                                    item.fatGrams.formatNutritionDisplay(),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MusFitTheme.colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (item.tagTexts.isNotEmpty()) {
                                Text(
                                    text = item.tagTexts.joinAsString(stringResource(R.string.food_separator)),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MusFitTheme.colors.brand,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Button(onClick = { onItemClick(item.id) }, modifier = Modifier.width(96.dp)) {
                            Text(
                                if (item.isSavedRecipe) {
                                    state.foodEntryActionVerb()
                                } else {
                                    stringResource(R.string.food_use)
                                },
                            )
                        }
                    }
                    HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
                }
            }
        }
    }
}

@Composable
private fun List<UiText>.joinAsString(separator: String): String {
    val values = mutableListOf<String>()
    for (text in this) {
        values += text.asString()
    }
    return values.joinToString(separator)
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
            Text(stringResource(R.string.food_meal_templates), style = MaterialTheme.typography.headlineSmall)
        }
        verticalGap("gap-after-meal-template-title", 14.dp)
        state.mealTemplateEditor?.let { editor ->
            item(key = "meal-template-editor") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle(stringResource(R.string.food_edit_template))
                    OutlinedTextField(
                        value = editor.name,
                        onValueChange = onNameChanged,
                        label = { Text(stringResource(R.string.food_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    MealTypeChips(
                        selectedMealType = editor.mealType,
                        mealDefinitions = state.visibleMealDefinitions,
                        onMealChanged = onMealTypeChanged,
                    )
                    Text(stringResource(R.string.food_items), style = MaterialTheme.typography.titleSmall)
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
                                Text(stringResource(DesignR.string.common_remove))
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
                            label = { Text(stringResource(R.string.food_amount_grams_short)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                        Button(onClick = onTemplateAddItemClick, modifier = Modifier.width(96.dp)) {
                            Text(stringResource(DesignR.string.common_add))
                        }
                    }
                    Button(
                        onClick = onSaveEditClick,
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand),
                    ) {
                        Text(stringResource(if (state.isSaving) R.string.food_saving else R.string.food_save_template))
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
                Text(stringResource(R.string.food_no_meal_templates), color = MusFitTheme.colors.onSurfaceVariant)
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
                            Text(
                                stringResource(R.string.food_favorite),
                                color = MusFitTheme.colors.brand,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MusFitOutlinedButton(onClick = { onTemplateClick(template.id) }) {
                            Text(state.foodEntryActionVerb())
                        }
                        MusFitOutlinedButton(onClick = { onFavoriteClick(template.id, !template.isFavorite) }) {
                            Text(stringResource(if (template.isFavorite) R.string.food_starred else R.string.food_star))
                        }
                        MusFitOutlinedButton(onClick = { onEditClick(template.id) }) {
                            Text(stringResource(DesignR.string.common_edit))
                        }
                        MusFitOutlinedButton(onClick = { onDuplicateClick(template.id) }) {
                            Text(stringResource(R.string.food_duplicate))
                        }
                        OutlinedButton(
                            onClick = { onDeleteClick(template.id) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Text(stringResource(DesignR.string.common_delete))
                        }
                    }
                }
                HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
            }
        }
        state.message?.let { message ->
            verticalGap("gap-before-meal-template-message", 14.dp)
            item(key = "meal-template-message") {
                Text(message.asString(), color = MusFitTheme.colors.brand)
            }
        }
    }
}
