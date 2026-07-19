@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.musfit.ui.food

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musfit.ui.components.InnerScreenHeader
import com.musfit.ui.components.SectionOverline
import com.musfit.ui.components.TonalHeaderIconButton
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccentRole
import com.musfit.ui.theme.tabAccentFor
import kotlin.math.roundToInt

// Full-screen Saved add surface — the 9b "Add food" design adapted from the
// sheet to the screen: cream ground, search pill, 5-mode row, chip tabs,
// same-as-yesterday card, grouped recents, pinned keep-adding pill.
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
    // Defaulted so the existing FoodScreen call site keeps compiling; the
    // orchestrator wires them (selectAddMode / meal retarget / openMealTemplates /
    // openRecipeBrowser / onKeepAddingFoodsChanged).
    onModeSelected: (FoodAddMode) -> Unit = {},
    onMealRetarget: (String) -> Unit = {},
    onOpenTemplates: () -> Unit = {},
    onOpenRecipes: () -> Unit = {},
    onKeepAddingChanged: (Boolean) -> Unit = {},
    // logSavedFood's isSaving guard drops rapid-fire calls, so a plain loop only
    // logs the first item; a batching VM callback should replace the fallback.
    onLogAllYesterday: (() -> Unit)? = null,
) {
    val mealLabel = state.visibleMealDefinitions.firstOrNull { it.id == state.mealType }?.title
        ?: state.selectedMealTitle
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MusFitTheme.colors.background)
            .imePadding(),
    ) {
        InnerScreenHeader(
            title = "Add food",
            onBack = onBack,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp),
        ) {
            MealTargetChip(
                label = "to $mealLabel",
                meals = state.visibleMealDefinitions,
                onMealSelected = onMealRetarget,
            )
            Box {
                var menuOpen by remember { mutableStateOf(false) }
                TonalHeaderIconButton(
                    icon = Icons.Outlined.MoreHoriz,
                    contentDescription = "More actions",
                    onClick = { menuOpen = true },
                    modifier = Modifier.size(48.dp),
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Quick track") }, onClick = {
                        menuOpen = false
                        onQuickTrack()
                    })
                    DropdownMenuItem(text = { Text("Copy yesterday") }, onClick = {
                        menuOpen = false
                        onCopyYesterday()
                    })
                    DropdownMenuItem(text = { Text("Save as template") }, onClick = {
                        menuOpen = false
                        onSaveTemplate()
                    })
                    DropdownMenuItem(text = { Text("Adjust goals") }, onClick = {
                        menuOpen = false
                        onAdjustGoals()
                    })
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item(key = "search") {
                AddFoodSectionWithGap {
                    AddFoodSearchPill(
                        query = state.foodDatabaseQuery,
                        onQueryChange = onQueryChange,
                        onScanClick = onScanClick,
                    )
                }
            }
            item(key = "modes") {
                AddFoodSectionWithGap {
                    FoodAddModeRow(selected = state.addMode, onSelect = onModeSelected)
                }
            }
            item(key = "tabs") {
                AddFoodSectionWithGap {
                    AddFoodTabChips(
                        selected = state.addTab,
                        onTabSelected = onTabSelected,
                        onOpenTemplates = onOpenTemplates,
                        onOpenRecipes = onOpenRecipes,
                    )
                }
            }

            val query = state.foodDatabaseQuery
            when (state.addTab) {
                AddTab.Recents ->
                    if (query.isBlank()) {
                        if (state.sameAsYesterday.isNotEmpty()) {
                            item(key = "yesterday") {
                                AddFoodSectionWithGap {
                                    SameAsYesterdayCard(
                                        items = state.sameAsYesterday,
                                        mealLabel = mealLabel,
                                        actionVerb = state.foodEntryActionVerb,
                                        onLogAll = {
                                            if (onLogAllYesterday != null) {
                                                onLogAllYesterday()
                                            } else {
                                                val first = state.sameAsYesterday.firstOrNull()
                                                if (first != null) onFoodClick(first.id)
                                            }
                                        },
                                    )
                                }
                            }
                        }
                        if (state.recentFoods.isNotEmpty()) {
                            item(key = "recents-heading") {
                                AddFoodSectionWithGap { SectionOverline("RECENTS") }
                            }
                            addFoodItems(
                                foods = state.recentFoods,
                                actionVerb = state.foodEntryActionVerb,
                                onFoodClick = onFoodClick,
                                sectionGapAfter = true,
                            )
                        }
                        if (state.recentFoods.isEmpty() && state.sameAsYesterday.isEmpty()) {
                            item(key = "recents-empty") {
                                AddFoodSectionWithGap {
                                    EmptyHint(
                                        "Search or scan a barcode to " +
                                            "${state.foodEntryActionVerb.lowercase()} your first food.",
                                    )
                                }
                            }
                        }
                        item(key = "recents-intake") { DailyIntakeStrip(state) }
                    } else {
                        if (state.visibleSavedFoods.isEmpty()) {
                            item(key = "search-empty") {
                                EmptyHint("No saved food matches \"$query\". Scan a barcode or create it.")
                            }
                        }
                        addFoodItems(state.visibleSavedFoods, state.foodEntryActionVerb, onFoodClick)
                    }

                AddTab.Favorites -> {
                    val favorites = state.savedFoods.filter { it.isFavorite }
                    if (favorites.isEmpty()) {
                        val actionNoun = if (state.isPlanningMode) "planning" else "logging"
                        item(key = "favorites-empty") {
                            AddFoodSectionWithGap {
                                EmptyHint("Foods you favorite show up here for one-tap $actionNoun.")
                            }
                        }
                    }
                    addFoodItems(
                        foods = favorites,
                        actionVerb = state.foodEntryActionVerb,
                        onFoodClick = onFoodClick,
                        sectionGapAfter = true,
                    )
                    item(key = "favorites-intake") { DailyIntakeStrip(state) }
                }

                AddTab.Create -> {
                    item(key = "create-form") {
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
                        // Preserve the original 14dp section rhythm plus the form's 8dp tail.
                        Spacer(Modifier.height(22.dp))
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp)
                .navigationBarsPadding(),
        ) {
            KeepAddingPill(
                checked = state.keepAddingFoods,
                onCheckedChange = onKeepAddingChanged,
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }
    }
}

/** White r99 search pill with the pulsing barcode-scanner affordance. */
@Composable
private fun AddFoodSearchPill(
    query: String,
    onQueryChange: (String) -> Unit,
    onScanClick: () -> Unit,
) {
    Surface(
        color = MusFitTheme.colors.surface,
        shape = RoundedCornerShape(99.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                tint = MusFitTheme.colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            val textStyle = MusFitTheme.typography.bodyMedium.copy(
                fontSize = 14.5.sp,
                color = MusFitTheme.colors.onSurface,
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = textStyle,
                cursorBrush = SolidColor(MusFitTheme.colors.brand),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search foods, brands, recipes…",
                                style = textStyle,
                                color = MusFitTheme.colors.onSurfaceFaint,
                                maxLines = 1,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            val spotlightProgress by rememberFoodBarcodeScannerSpotlightProgress()
            val spotlight = foodBarcodeScannerSpotlightTransform(spotlightProgress)
            // 48dp interactive box around the 36dp visual circle keeps the
            // primary scanner entry point at the minimum touch-target size.
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "Scan barcode" }
                    .clip(CircleShape)
                    .clickable(
                        onClickLabel = "Scan barcode",
                        role = Role.Button,
                        onClick = onScanClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .graphicsLayer {
                            scaleX = spotlight.containerScale
                            scaleY = spotlight.containerScale
                        }
                        .clip(CircleShape)
                        .background(MusFitTheme.colors.positiveContainer.copy(alpha = spotlight.borderAlpha)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.QrCodeScanner,
                        contentDescription = null,
                        tint = MusFitTheme.colors.brand,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                scaleX = spotlight.iconScale
                                scaleY = spotlight.iconScale
                            },
                    )
                }
            }
        }
    }
}

/** Recents/Favorites/Create tab chips plus the Templates/Recipes shortcuts. */
@Composable
private fun AddFoodTabChips(
    selected: AddTab,
    onTabSelected: (AddTab) -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenRecipes: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        AddTab.entries.forEach { tab ->
            SelectableChip(
                text = tab.label,
                selected = selected == tab,
                onClick = { onTabSelected(tab) },
            )
        }
        SelectableChip(
            text = "Templates",
            selected = null,
            onClick = onOpenTemplates,
        )
        SelectableChip(
            text = "Recipes",
            selected = null,
            onClick = onOpenRecipes,
        )
    }
}

/** Green "Same as yesterday" card with a one-tap "Log all" / "Plan all". */
@Composable
private fun SameAsYesterdayCard(
    items: List<SavedFoodUiState>,
    mealLabel: String,
    actionVerb: String,
    onLogAll: () -> Unit,
) {
    val accent = tabAccentFor(TabAccentRole.Food)
    val totalKcal = items.sumOf { it.caloriesPerServingKcal }.roundToInt()
    Surface(
        color = accent.container,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(percent = 38))
                    .background(MusFitTheme.colors.surface.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.History,
                    contentDescription = null,
                    tint = accent.onContainer,
                    modifier = Modifier.size(21.dp),
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "Same as yesterday",
                    style = MusFitTheme.typography.bodyMedium.copy(fontSize = 14.5.sp, fontWeight = FontWeight.W800),
                    color = accent.onContainer,
                    maxLines = 1,
                )
                Text(
                    text = "$mealLabel · ${items.size} items · $totalKcal kcal",
                    style = MusFitTheme.typography.bodySmall,
                    color = accent.onContainerVariant,
                    maxLines = 1,
                )
            }
            Surface(
                onClick = onLogAll,
                shape = RoundedCornerShape(99.dp),
                color = MusFitTheme.colors.brand,
                contentColor = MusFitTheme.colors.onBrand,
            ) {
                Text(
                    text = "$actionVerb all",
                    style = MusFitTheme.typography.labelMedium.copy(fontWeight = FontWeight.W800),
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
}

/** Grouped white rows; whole-row tap logs, plus a 48dp add button target. */
private fun LazyListScope.addFoodItems(
    foods: List<SavedFoodUiState>,
    actionVerb: String,
    onFoodClick: (String) -> Unit,
    sectionGapAfter: Boolean = false,
) {
    itemsIndexed(
        items = foods,
        key = { _, food -> food.id },
        contentType = { _, _ -> "saved-food" },
    ) { index, food ->
        AddFoodListItem(
            food = food,
            index = index,
            count = foods.size,
            actionVerb = actionVerb,
            onFoodClick = onFoodClick,
            modifier = if (sectionGapAfter && index == foods.lastIndex) {
                Modifier.padding(bottom = 10.dp)
            } else {
                Modifier
            },
        )
    }
}

@Composable
@Suppress("LongParameterList") // Lazy row geometry and action semantics stay explicit at the item boundary.
private fun AddFoodListItem(
    food: SavedFoodUiState,
    index: Int,
    count: Int,
    actionVerb: String,
    onFoodClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = tabAccentFor(TabAccentRole.Food)
    val kcal = food.caloriesPerServingKcal.roundToInt()
    val subtitle = food.servingName?.let { "1 $it · $kcal kcal" }
        ?: "${food.defaultServingGrams.roundToInt()} g · $kcal kcal"
    FoodListItemRow(
        index = index,
        count = count,
        title = food.name,
        subtitle = subtitle,
        onClick = { onFoodClick(food.id) },
        modifier = modifier,
        imageUrl = food.imageUrl,
        fallbackIcon = Icons.Filled.LocalDining,
        badgeSize = 44.dp,
        trailingContent = {
            Surface(
                onClick = { onFoodClick(food.id) },
                shape = RoundedCornerShape(14.dp),
                color = accent.container,
                contentColor = accent.onContainer,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = "$actionVerb ${food.name}",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        },
    )
}

@Composable
private fun AddFoodSectionWithGap(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
    ) {
        content()
    }
}

/** Slim tonal replacement for the old DailyIntakeCard — quiet day context. */
@Composable
private fun DailyIntakeStrip(state: FoodUiState) {
    val accent = tabAccentFor(TabAccentRole.Food)
    Surface(
        color = accent.container,
        shape = RoundedCornerShape(99.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
        ) {
            Text(
                text = "Daily intake",
                style = MusFitTheme.typography.labelMedium.copy(fontWeight = FontWeight.W700),
                color = accent.onContainerVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${state.eatenCaloriesKcal.roundToInt()} / ${state.calorieGoalKcal.roundToInt()} kcal",
                style = MusFitTheme.typography.labelMedium.copy(fontWeight = FontWeight.W800),
                color = accent.onContainer,
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
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MusFitTheme.typography.bodyMedium,
        color = MusFitTheme.colors.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}
