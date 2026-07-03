@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.musfit.ui.food

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musfit.ui.theme.MusFitTheme
import kotlin.math.roundToInt

@Composable
fun AddFoodScreen(
    state: FoodUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onScanClick: () -> Unit,
    onTabSelected: (AddTab) -> Unit,
    onFoodClick: (String) -> Unit,
    onQuickTrack: () -> Unit,
    onAdjustGoals: () -> Unit,
    onCopyYesterday: () -> Unit,
    onSaveTemplate: () -> Unit,
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MusFitTheme.colors.background),
    ) {
        Surface(color = MusFitTheme.colors.surface) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 6.dp, end = 6.dp, top = 12.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MusFitTheme.colors.onSurface)
                    }
                    val meal = state.mealSections.firstOrNull { it.id == state.mealType }
                    val mealCalories = meal?.caloriesKcal ?: 0.0
                    val mealPlannedCalories = meal?.plannedCaloriesKcal ?: 0.0
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.selectedMealTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MusFitTheme.colors.onSurface,
                        )
                        Text(
                            text = if (state.isPlanningMode) {
                                if (mealPlannedCalories > 0.0) {
                                    "${mealPlannedCalories.roundToInt()} kcal planned"
                                } else {
                                    "Planning for this meal"
                                }
                            } else if (mealCalories > 0.0) {
                                "${mealCalories.roundToInt()} kcal logged"
                            } else {
                                "Nothing logged yet"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MusFitTheme.colors.onSurfaceVariant,
                        )
                    }
                    Box {
                        var menuOpen by remember { mutableStateOf(false) }
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More actions", tint = MusFitTheme.colors.onSurface)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text("Quick track") }, onClick = { menuOpen = false; onQuickTrack() })
                            DropdownMenuItem(text = { Text("Copy yesterday") }, onClick = { menuOpen = false; onCopyYesterday() })
                            DropdownMenuItem(text = { Text("Save as template") }, onClick = { menuOpen = false; onSaveTemplate() })
                            DropdownMenuItem(text = { Text("Adjust goals") }, onClick = { menuOpen = false; onAdjustGoals() })
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = state.foodDatabaseQuery,
                        onValueChange = onQueryChange,
                        placeholder = { Text("Food, meal or brand") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        shape = MusFitTheme.shapes.large,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = onScanClick,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(MusFitTheme.shapes.medium)
                            .background(MusFitTheme.colors.positiveContainer),
                    ) {
                        Icon(Icons.Outlined.QrCodeScanner, contentDescription = "Scan barcode", tint = MusFitTheme.colors.brand)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
        ) {
            // Daily-intake context is redundant while authoring a new food; show it on the browse tabs only.
            if (state.addTab != AddTab.Create) {
                DailyIntakeCard(state)
            }
            AddTabRow(selected = state.addTab, onTabSelected = onTabSelected)

            val query = state.foodDatabaseQuery
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                when (state.addTab) {
                    AddTab.Recents ->
                        if (query.isBlank()) {
                            if (state.sameAsYesterday.isNotEmpty()) {
                                SectionLabel("SAME AS YESTERDAY?")
                                state.sameAsYesterday.forEach { AddFoodRow(it, state.foodEntryActionVerb, onFoodClick) }
                            }
                            state.recentFoods.firstOrNull()?.let { last ->
                                SectionLabel("LAST TRACKED")
                                AddFoodRow(last, state.foodEntryActionVerb, onFoodClick)
                            }
                            if (state.recentFoods.size > 1) {
                                SectionLabel("ALL RECENTS")
                                state.recentFoods.drop(1).forEach { AddFoodRow(it, state.foodEntryActionVerb, onFoodClick) }
                            }
                            if (state.recentFoods.isEmpty() && state.sameAsYesterday.isEmpty()) {
                                EmptyHint("Search or scan a barcode to ${state.foodEntryActionVerb.lowercase()} your first food.")
                            }
                        } else {
                            if (state.visibleSavedFoods.isEmpty()) {
                                EmptyHint("No saved food matches \"$query\". Scan a barcode or create it.")
                            }
                            state.visibleSavedFoods.forEach { AddFoodRow(it, state.foodEntryActionVerb, onFoodClick) }
                        }

                    AddTab.Favorites -> {
                        val favorites = state.savedFoods.filter { it.isFavorite }
                        if (favorites.isEmpty()) {
                            val actionNoun = if (state.isPlanningMode) "planning" else "logging"
                            EmptyHint("Foods you favorite show up here for one-tap $actionNoun.")
                        }
                        favorites.forEach { AddFoodRow(it, state.foodEntryActionVerb, onFoodClick) }
                    }

                    AddTab.Create -> {
                        Spacer(Modifier.height(6.dp))
                        CreateFoodForm(
                            state = state,
                            onScanBarcode = onScanClick,
                            onScanLabel = onScanLabel,
                            onProductNameChanged = onProductNameChanged,
                            onBrandChanged = onBrandChanged,
                            onQuantityChanged = onQuantityChanged,
                            onAmountServingChoiceSelected = onAmountServingChoiceSelected,
                            onCaloriesChanged = onCaloriesChanged,
                            onProteinChanged = onProteinChanged,
                            onCarbsChanged = onCarbsChanged,
                            onFatChanged = onFatChanged,
                            onSaveProduct = onSaveProduct,
                            onLogFood = onLogFood,
                            onCreateRecipe = onCreateRecipe,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyIntakeCard(state: FoodUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        colors = CardDefaults.cardColors(containerColor = MusFitTheme.colors.surface),
        shape = MusFitTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Daily intake", fontWeight = FontWeight.Bold, color = MusFitTheme.colors.onSurface)
                Text(
                    "${state.eatenCaloriesKcal.roundToInt()} / ${state.calorieGoalKcal.roundToInt()} kcal",
                    fontWeight = FontWeight.Bold,
                    color = MusFitTheme.colors.onSurface,
                )
            }
            Spacer(Modifier.height(12.dp))
            MacroProgressRow(state.macroProgress)
        }
    }
}

@Composable
private fun AddTabRow(selected: AddTab, onTabSelected: (AddTab) -> Unit) {
    val tabs = AddTab.entries
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = tab == selected
            SegmentedButton(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MusFitTheme.colors.positiveContainer,
                    activeContentColor = MusFitTheme.colors.brand,
                    activeBorderColor = MusFitTheme.colors.brand,
                    inactiveContainerColor = MusFitTheme.colors.surface,
                    inactiveContentColor = MusFitTheme.colors.onSurfaceVariant,
                ),
                icon = {},
                label = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                    )
                },
            )
        }
    }
}

private val AddTab.label: String
    get() = when (this) {
        AddTab.Recents -> "Recents"
        AddTab.Favorites -> "Favorites"
        AddTab.Create -> "Create"
    }

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MusFitTheme.colors.onSurfaceVariant,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MusFitTheme.colors.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun AddFoodRow(
    food: SavedFoodUiState,
    actionVerb: String,
    onFoodClick: (String) -> Unit,
) {
    Surface(
        onClick = { onFoodClick(food.id) },
        color = MusFitTheme.colors.surface,
        shape = MusFitTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FoodThumb(imageUrl = food.imageUrl, fallback = Icons.Outlined.Restaurant)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MusFitTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${food.caloriesPerServingKcal.roundToInt()} kcal · ${food.defaultServingGrams.roundToInt()} g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MusFitTheme.colors.positiveContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "$actionVerb ${food.name}", tint = MusFitTheme.colors.brand)
            }
        }
    }
}
