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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
    onCreateFood: () -> Unit,
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
                    Text(
                        text = state.selectedMealTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MusFitTheme.colors.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Box {
                        var menuOpen by remember { mutableStateOf(false) }
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More actions", tint = MusFitTheme.colors.onSurface)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text("Quick track") }, onClick = { menuOpen = false; onQuickTrack() })
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
            DailyIntakeCard(state)
            AddTabRow(selected = state.addTab, onTabSelected = onTabSelected)

            val query = state.foodDatabaseQuery
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                when (state.addTab) {
                    AddTab.Recents ->
                        if (query.isBlank()) {
                            if (state.sameAsYesterday.isNotEmpty()) {
                                SectionLabel("SAME AS YESTERDAY?")
                                state.sameAsYesterday.forEach { AddFoodRow(it, onFoodClick) }
                            }
                            state.recentFoods.firstOrNull()?.let { last ->
                                SectionLabel("LAST TRACKED")
                                AddFoodRow(last, onFoodClick)
                            }
                            if (state.recentFoods.size > 1) {
                                SectionLabel("ALL RECENTS")
                                state.recentFoods.drop(1).forEach { AddFoodRow(it, onFoodClick) }
                            }
                            if (state.recentFoods.isEmpty() && state.sameAsYesterday.isEmpty()) {
                                EmptyHint("Search or scan a barcode to log your first food.")
                            }
                        } else {
                            if (state.visibleSavedFoods.isEmpty()) {
                                EmptyHint("No saved food matches \"$query\". Scan a barcode or create it.")
                            }
                            state.visibleSavedFoods.forEach { AddFoodRow(it, onFoodClick) }
                        }

                    AddTab.Favorites -> {
                        val favorites = state.savedFoods.filter { it.isFavorite }
                        if (favorites.isEmpty()) {
                            EmptyHint("Foods you favorite show up here for one-tap logging.")
                        }
                        favorites.forEach { AddFoodRow(it, onFoodClick) }
                    }

                    AddTab.Create -> {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = onCreateFood, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Create a food")
                        }
                        Spacer(Modifier.height(8.dp))
                        EmptyHint("Add a food that isn't in any database (scan-to-autofill coming soon).")
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AddTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Surface(
                onClick = { onTabSelected(tab) },
                color = if (isSelected) MusFitTheme.colors.positiveContainer else MusFitTheme.colors.surface,
                shape = MusFitTheme.shapes.small,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MusFitTheme.colors.brand else MusFitTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 10.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
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
private fun AddFoodRow(food: SavedFoodUiState, onFoodClick: (String) -> Unit) {
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
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MusFitTheme.colors.accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add ${food.name}", tint = MusFitTheme.colors.onAccent)
            }
        }
    }
}
