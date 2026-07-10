# Food main-tab UI restructure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restructure the Food diary "home" screen for cleaner hierarchy and to integrate the newer features — one unified calories+macros hero, a 3-up quick-actions row (Recipes / Plan / Water) plus a floating Add FAB, and the single "More details" accordion split into two labeled groups — while moving reference features into the `⋮` tools menu.

**Architecture:** Behavior-preserving re-composition of `FoodScreen.kt`'s diary column. Reuses existing composables (`WaterTrackerCard`, `FoodHealthConnectSyncCard`, `MacroProgressColumn` logic, `WeeklyPlanStrip`) and existing `FoodViewModel` actions. The only new ViewModel surface is two thin sheet-open helpers and two `FoodSheetMode` values; the only new pure logic is a time-of-day → default-meal helper. No repository, DAO, entity, Room-migration, or domain-model changes.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Hilt, JUnit + hand-written fakes (`FakeFoodRepository`, `FakeProductProvider`), `StandardTestDispatcher`.

**Spec:** [`docs/superpowers/specs/2026-07-04-food-main-tab-ui-restructure-design.md`](../specs/2026-07-04-food-main-tab-ui-restructure-design.md)

**Build/verify (Windows PowerShell), run from repo root after sourcing the toolchain:**

```powershell
. .\.superpowers\sdd\android-env.ps1
.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.food.*" --no-daemon --console=plain
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain
```

---

## File structure

| File | Change |
| --- | --- |
| `app/src/main/java/com/musfit/ui/food/FoodComponents.kt` | Add pure `defaultAddMealId(sections, hour)` helper. |
| `app/src/main/java/com/musfit/ui/food/FoodViewModel.kt` | Add `FoodSheetMode.Water` + `FoodSheetMode.HealthConnect`; add `openWaterSheet()` + `openHealthConnectSheet()`. |
| `app/src/main/java/com/musfit/ui/food/FoodScreen.kt` | Wire the two new sheets; quick-actions row; unify hero; Add FAB; split "More details"; re-scope the overflow menu; drop the header Plan chip + dead `RecipeBrowser` branch. |
| `app/src/test/java/com/musfit/ui/food/FoodDefaultMealTest.kt` | New pure JUnit test for the default-meal helper. |
| `app/src/test/java/com/musfit/ui/food/FoodViewModelTest.kt` | Add tests for the two sheet-open helpers. |

**Note on UI-only tasks:** Tasks 3–6 change Compose composition, which this project does not cover with unit tests (no Robolectric UI tests). Those tasks are verified by `assembleDebug`/`lintDebug` plus on-device evidence in Task 7 — they have no red/green unit step, which is expected here.

---

## Task 1: Pure default-meal helper

**Files:**
- Create: `app/src/test/java/com/musfit/ui/food/FoodDefaultMealTest.kt`
- Modify: `app/src/main/java/com/musfit/ui/food/FoodComponents.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/musfit/ui/food/FoodDefaultMealTest.kt`:

```kotlin
package com.musfit.ui.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FoodDefaultMealTest {
    private fun meal(id: String, title: String) = FoodMealSectionUiState(
        id = id,
        title = title,
        recommendation = "",
        caloriesKcal = 0.0,
        calorieTargetKcal = 0.0,
        calorieProgress = 0.0,
        entries = emptyList(),
    )

    private val meals = listOf(
        meal("breakfast", "Breakfast"),
        meal("lunch", "Lunch"),
        meal("dinner", "Dinner"),
        meal("snacks", "Snacks"),
    )

    @Test
    fun morningPicksBreakfast() = assertEquals("breakfast", defaultAddMealId(meals, 8))

    @Test
    fun middayPicksLunch() = assertEquals("lunch", defaultAddMealId(meals, 13))

    @Test
    fun eveningPicksDinner() = assertEquals("dinner", defaultAddMealId(meals, 19))

    @Test
    fun lateNightPicksSnacks() = assertEquals("snacks", defaultAddMealId(meals, 23))

    @Test
    fun matchesByTitleWhenIdIsCustom() {
        val custom = listOf(meal("meal-1", "Morning"), meal("meal-2", "Lunch plate"))
        assertEquals("meal-2", defaultAddMealId(custom, 13))
    }

    @Test
    fun fallsBackToFirstWhenNoKeywordMatch() {
        val custom = listOf(meal("meal-1", "Fuel"), meal("meal-2", "Refuel"))
        assertEquals("meal-1", defaultAddMealId(custom, 13))
    }

    @Test
    fun emptyReturnsNull() = assertNull(defaultAddMealId(emptyList(), 8))
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.food.FoodDefaultMealTest" --no-daemon --console=plain`
Expected: FAIL to compile / unresolved reference `defaultAddMealId`.

- [ ] **Step 3: Implement the helper**

In `app/src/main/java/com/musfit/ui/food/FoodComponents.kt`, add this top-level function next to the other pure helpers (e.g. directly after `mealTypeIcon(...)`, around line 87):

```kotlin
/**
 * The meal the diary FAB defaults to, chosen by time of day. Matches the current
 * time bucket's keyword against each section's id+title (mirroring [mealTypeIcon]),
 * falling back to the first section, or null when there are no meals.
 */
internal fun defaultAddMealId(
    sections: List<FoodMealSectionUiState>,
    hour: Int,
): String? {
    if (sections.isEmpty()) return null
    val keyword = when (hour) {
        in 4..10 -> "breakfast"
        in 11..15 -> "lunch"
        in 16..21 -> "dinner"
        else -> "snack"
    }
    val match = sections.firstOrNull { keyword in "${it.id} ${it.title}".lowercase() }
    return (match ?: sections.first()).id
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.food.FoodDefaultMealTest" --no-daemon --console=plain`
Expected: PASS (7 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/musfit/ui/food/FoodComponents.kt app/src/test/java/com/musfit/ui/food/FoodDefaultMealTest.kt
git commit -m "feat(food): add time-of-day default-meal helper for diary add"
```

---

## Task 2: Water and Health Connect sheets

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/food/FoodViewModel.kt` (enum ~94-109; add helpers near `openFoodDatabase` ~1336)
- Modify: `app/src/main/java/com/musfit/ui/food/FoodScreen.kt` (the `when (state.sheetMode ?: FoodSheetMode.AddFood)` block ~360-605)
- Modify: `app/src/test/java/com/musfit/ui/food/FoodViewModelTest.kt`

- [ ] **Step 1: Write the failing tests**

Add to `FoodViewModelTest.kt` (anywhere among the other `@Test` methods):

```kotlin
    @Test
    fun openWaterSheet_showsWaterSheet() = runTest {
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = FakeFoodRepository(),
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openWaterSheet()

        with(viewModel.state.value) {
            assertTrue(isAddPanelVisible)
            assertEquals(FoodSheetMode.Water, sheetMode)
        }
    }

    @Test
    fun openHealthConnectSheet_showsHealthConnectSheet() = runTest {
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = FakeFoodRepository(),
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openHealthConnectSheet()

        with(viewModel.state.value) {
            assertTrue(isAddPanelVisible)
            assertEquals(FoodSheetMode.HealthConnect, sheetMode)
        }
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.food.FoodViewModelTest" --no-daemon --console=plain`
Expected: FAIL — unresolved references `openWaterSheet` / `openHealthConnectSheet` / `FoodSheetMode.Water`.

- [ ] **Step 3: Add the enum values**

In `FoodViewModel.kt`, extend `enum class FoodSheetMode { ... }` (currently ending `ShoppingList,` at ~108) with two values before the closing brace:

```kotlin
    ShoppingList,
    Water,
    HealthConnect,
}
```

- [ ] **Step 4: Add the two open helpers**

In `FoodViewModel.kt`, add next to `openFoodDatabase()` (~1336):

```kotlin
    fun openWaterSheet() {
        mutableState.update {
            it.copy(isAddPanelVisible = true, sheetMode = FoodSheetMode.Water, message = null)
        }
    }

    fun openHealthConnectSheet() {
        mutableState.update {
            it.copy(isAddPanelVisible = true, sheetMode = FoodSheetMode.HealthConnect, message = null)
        }
    }
```

- [ ] **Step 5: Wire the sheets into `FoodScreen` and remove the dead branch**

In `FoodScreen.kt`, inside the `when (state.sheetMode ?: FoodSheetMode.AddFood) { ... }` block:

(a) Delete the duplicated dead line (there are two identical `FoodSheetMode.RecipeBrowser -> Unit` at ~541-542) so only **one** remains.

(b) Add these two branches after the `FoodSheetMode.ShoppingList -> ShoppingListPanel(...)` branch (~604):

```kotlin
                FoodSheetMode.Water ->
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        WaterTrackerCard(
                            state = state,
                            onQuickWaterClick = viewModel::logQuickWater,
                            onCustomAmountChanged = viewModel::onWaterCustomAmountChanged,
                            onCustomAddClick = viewModel::logCustomWater,
                            onGoalChanged = viewModel::onWaterGoalChanged,
                            onGoalSaveClick = viewModel::saveWaterGoal,
                        )
                    }

                FoodSheetMode.HealthConnect ->
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        FoodHealthConnectSyncCard(
                            state = state,
                            onEnabledChanged = viewModel::onFoodHealthConnectSyncEnabledChanged,
                            onRequestPermissionsClick = {
                                if (state.foodHealthConnectCanRequestPermissions) {
                                    foodHealthConnectPermissionLauncher.launch(
                                        state.foodHealthConnectRequestablePermissions,
                                    )
                                }
                            },
                            onRefreshClick = viewModel::refreshFoodHealthConnectSync,
                            onSyncClick = viewModel::syncFoodToHealthConnect,
                        )
                    }
```

- [ ] **Step 6: Run the tests to verify they pass, then build**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.food.FoodViewModelTest" --no-daemon --console=plain`
Expected: PASS.
Run: `.\gradlew.bat assembleDebug --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/musfit/ui/food/FoodViewModel.kt app/src/main/java/com/musfit/ui/food/FoodScreen.kt app/src/test/java/com/musfit/ui/food/FoodViewModelTest.kt
git commit -m "feat(food): add Water and Health Connect bottom sheets"
```

---

## Task 3: Quick-actions row replaces the Browse-recipes button

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`

- [ ] **Step 1: Add the quick-actions composables**

In `FoodScreen.kt`, replace the `FoodPrimaryActionRow` composable (~610-635) with these two composables:

```kotlin
@Composable
private fun FoodQuickActionsRow(
    isPlanningActive: Boolean,
    onRecipesClick: () -> Unit,
    onPlanClick: () -> Unit,
    onWaterClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FoodQuickActionTile(
            icon = Icons.Outlined.Restaurant,
            label = "Recipes",
            selected = false,
            onClick = onRecipesClick,
            modifier = Modifier.weight(1f),
        )
        FoodQuickActionTile(
            icon = Icons.Outlined.Today,
            label = "Plan week",
            selected = isPlanningActive,
            onClick = onPlanClick,
            modifier = Modifier.weight(1f),
        )
        FoodQuickActionTile(
            icon = Icons.Outlined.WaterDrop,
            label = "Water",
            selected = false,
            onClick = onWaterClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun FoodQuickActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = if (selected) {
        MusFitTheme.colors.brand.copy(alpha = 0.14f).compositeOver(MusFitTheme.colors.surface)
    } else {
        MusFitTheme.colors.surface
    }
    val contentTint = if (selected) MusFitTheme.colors.brand else MusFitTheme.colors.onSurfaceVariant
    Surface(
        onClick = onClick,
        color = container,
        shape = MusFitTheme.shapes.extraLarge,
        border = if (selected) BorderStroke(1.dp, MusFitTheme.colors.brand.copy(alpha = 0.4f)) else null,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, contentDescription = null, tint = contentTint, modifier = Modifier.size(22.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentTint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
```

If `Icons.Outlined.WaterDrop` does not resolve, use `Icons.Outlined.LocalDrink` (both are in the extended icon set the project already uses).

- [ ] **Step 2: Swap the call site**

In the diary `Column` (~270-273), replace:

```kotlin
                    FoodPrimaryActionRow(
                        recipeCount = state.recipes.size,
                        onRecipeClick = viewModel::openRecipeBrowser,
                    )
```

with:

```kotlin
                    FoodQuickActionsRow(
                        isPlanningActive = state.isPlanningMode,
                        onRecipesClick = viewModel::openRecipeBrowser,
                        onPlanClick = viewModel::togglePlanningMode,
                        onWaterClick = viewModel::openWaterSheet,
                    )
```

- [ ] **Step 3: Build**

Run: `.\gradlew.bat assembleDebug --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL (no unresolved `FoodPrimaryActionRow`).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/musfit/ui/food/FoodScreen.kt
git commit -m "feat(food): replace Browse-recipes button with quick-actions row"
```

---

## Task 4: Unify the hero (macros + week strip inside the summary card)

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`

- [ ] **Step 1: Add a hero macro strip composable**

In `FoodScreen.kt`, add near `FoodDiarySummaryCard` (do NOT touch `MacroProgressRow`/`MacroProgressColumn` — they are still used by `AddFoodScreen.kt`):

```kotlin
@Composable
private fun HeroMacroStrip(
    macros: List<FoodMacroProgressUiState>,
    contentColor: Color,
) {
    val macroColors = MusFitTheme.colors.macroColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        macros.forEachIndexed { index, macro ->
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = macro.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                        maxLines = 1,
                    )
                    Text(
                        text = "${macro.currentGrams.roundToInt()}/${macro.goalGrams.roundToInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.7f),
                        maxLines = 1,
                    )
                }
                ProgressBar(
                    progress = (macro.currentGrams / macro.goalGrams).toFloat().coerceIn(0f, 1f),
                    color = macroColors[index % macroColors.size],
                )
            }
        }
    }
}
```

- [ ] **Step 2: Render macros + week strip inside `FoodDiarySummaryCard`**

In `FoodDiarySummaryCard` (~838-882), inside the existing `Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { ... }`, after the Eaten/ring/Goal `Row` (the one closing at ~879), add:

```kotlin
            if (state.macroProgress.isNotEmpty()) {
                HeroMacroStrip(macros = state.macroProgress, contentColor = accent.onContainer)
            }
            val planning = state.toPlanningModePresentation()
            if (planning.showStatusCard) {
                WeeklyPlanStrip(planDays = state.weeklyPlan, selectedDate = state.selectedDate)
            }
```

This preserves the old `PlanningModeStatusCard` visibility rule (`showStatusCard` = planning mode OR planned items exist) and keeps `toPlanningModePresentation()` in production use (it is covered by `FoodPlanningModePresentationTest`).

- [ ] **Step 3: Remove the now-duplicated main-scroll pieces**

In the diary `Column`:

(a) Delete the standalone macro row call (~275):

```kotlin
                    MacroProgressRow(state.macroProgress)
```

(b) Delete the planning status card block (~277-284):

```kotlin
                    if (planningPresentation.showStatusCard) {
                        PlanningModeStatusCard(
                            presentation = planningPresentation,
                            planDays = state.weeklyPlan,
                            selectedDate = state.selectedDate,
                            onActionClick = viewModel::togglePlanningMode,
                        )
                    }
```

(c) Delete the now-unused `val planningPresentation = state.toPlanningModePresentation()` (~113) and the `PlanningModeStatusCard` composable (~786-835). Leave `FoodPlanningModePresentation` and `toPlanningModePresentation()` in place (still used by the hero and by tests).

- [ ] **Step 4: Build**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.food.*" lintDebug assembleDebug --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL; Food tests (incl. `FoodPlanningModePresentationTest`) PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/musfit/ui/food/FoodScreen.kt
git commit -m "feat(food): fold macros and week strip into the calorie hero"
```

---

## Task 5: Add FAB and drop the header Plan chip

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`

- [ ] **Step 1: Add the FAB to the diary branch**

In `FoodScreen`, the outer `Box` (~154) has an `else { Column(...) { ... } }` branch for the diary (~230-346). Add the FAB as a second child of that `else` block, immediately after the diary `Column`'s closing brace, still inside the `else`:

```kotlin
            FloatingActionButton(
                onClick = {
                    val mealId = defaultAddMealId(
                        state.mealSections,
                        java.time.LocalTime.now().hour,
                    ) ?: state.mealSections.firstOrNull()?.id
                    if (mealId != null) viewModel.openAddFood(mealId)
                },
                containerColor = MusFitTheme.colors.brand,
                contentColor = MusFitTheme.colors.onAccent,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 96.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add food")
            }
```

`.align(Alignment.BottomEnd)` is valid because the `else` content sits directly in the outer `Box`. Add the import `androidx.compose.material3.FloatingActionButton` if not present.

- [ ] **Step 2: Remove the header Plan chip**

In the `MusFitScreenHeader(actions = { ... })` block (~244-260), delete the `FoodPlanningModeButton(...)` call (~245-248), leaving only `FoodDiaryOverflowAction(...)`. Then delete the now-unused `FoodPlanningModeButton` composable (~722-748).

- [ ] **Step 3: Build**

Run: `.\gradlew.bat assembleDebug --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/musfit/ui/food/FoodScreen.kt
git commit -m "feat(food): add diary Add FAB, remove redundant header Plan chip"
```

---

## Task 6: Split "More details" and re-scope the tools menu

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`

- [ ] **Step 1: Generalize the collapsible to take a title**

Rename `MoreSection` (~637-672) to `CollapsibleGroup` and give it a `title` parameter:

```kotlin
@Composable
private fun CollapsibleGroup(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            onClick = onToggle,
            color = MusFitTheme.colors.surface,
            shape = MusFitTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MusFitTheme.colors.onSurface,
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MusFitTheme.colors.onSurfaceVariant,
                )
            }
        }
        if (expanded) {
            content()
        }
    }
}
```

- [ ] **Step 2: Replace the single accordion with two groups**

In `FoodScreen`, replace the `var moreExpanded by rememberSaveable { mutableStateOf(false) }` (~117) with:

```kotlin
    var todaySummaryExpanded by rememberSaveable { mutableStateOf(false) }
    var trendsExpanded by rememberSaveable { mutableStateOf(false) }
```

Then replace the whole `MoreSection(expanded = moreExpanded, ...) { Column { ... } }` block (~309-344) with:

```kotlin
                    CollapsibleGroup(
                        title = "Today's summary",
                        expanded = todaySummaryExpanded,
                        onToggle = { todaySummaryExpanded = !todaySummaryExpanded },
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            DayRatingCard(state.dayRating)
                            DailyInsightsSection(state.dailyInsights)
                            FoodHabitTrackerSection(state.habitTrackers)
                            AdvancedNutritionProgressRow(state.advancedNutritionProgress)
                            MicronutrientRow(state.micronutrients)
                        }
                    }

                    CollapsibleGroup(
                        title = "Trends",
                        expanded = trendsExpanded,
                        onToggle = { trendsExpanded = !trendsExpanded },
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            WeeklyMusFitScoreCard(state.weeklyScore)
                            FoodProgressStatsCard(state.progressStats)
                        }
                    }
```

`WaterTrackerCard`, `FoodHealthConnectSyncCard`, and `FoodDatabasePreview` are intentionally dropped from the main scroll here (now reached via the Water tile, the Health Connect menu item, and the Food database menu item). Keep the `foodHealthConnectPermissionLauncher` declaration (~118-122) — the Health Connect sheet uses it. Delete the now-unused `FoodDatabasePreview` and `SavedFoodSummaryRow` composables (~2300-end of those two functions).

- [ ] **Step 3: Re-scope the overflow menu**

Change `FoodDiaryOverflowAction` (~750-783): drop the Recipes and planning entries, add Health Connect and Food database. Replace its parameter list and body:

```kotlin
@Composable
private fun FoodDiaryOverflowAction(
    state: FoodUiState,
    onGoalClick: () -> Unit,
    onMealsClick: () -> Unit,
    onTemplatesClick: () -> Unit,
    onShoppingClick: () -> Unit,
    onFastingClick: () -> Unit,
    onHealthConnectClick: () -> Unit,
    onFoodDatabaseClick: () -> Unit,
    onCopyDayToTomorrowClick: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    IconButton(onClick = { menuOpen = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = "More actions", tint = MusFitTheme.colors.onSurfaceVariant)
    }
    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
        DropdownMenuItem(text = { Text("Goals") }, onClick = { menuOpen = false; onGoalClick() })
        DropdownMenuItem(text = { Text("Meals") }, onClick = { menuOpen = false; onMealsClick() })
        DropdownMenuItem(text = { Text("Templates") }, onClick = { menuOpen = false; onTemplatesClick() })
        DropdownMenuItem(text = { Text("Shopping list") }, onClick = { menuOpen = false; onShoppingClick() })
        DropdownMenuItem(text = { Text("Fasting") }, onClick = { menuOpen = false; onFastingClick() })
        DropdownMenuItem(text = { Text("Health Connect") }, onClick = { menuOpen = false; onHealthConnectClick() })
        DropdownMenuItem(text = { Text("Food database") }, onClick = { menuOpen = false; onFoodDatabaseClick() })
        DropdownMenuItem(
            text = { Text("Copy day to tomorrow") },
            enabled = !state.isSaving,
            onClick = { menuOpen = false; onCopyDayToTomorrowClick() },
        )
    }
}
```

- [ ] **Step 4: Update the overflow call site**

In the header `actions` block, update the `FoodDiaryOverflowAction(...)` call (~249-259) to the new parameters:

```kotlin
                            FoodDiaryOverflowAction(
                                state = state,
                                onGoalClick = viewModel::openGoalEditor,
                                onMealsClick = viewModel::openMealSettings,
                                onTemplatesClick = viewModel::openMealTemplates,
                                onShoppingClick = viewModel::openShoppingList,
                                onFastingClick = viewModel::openFastingTimer,
                                onHealthConnectClick = viewModel::openHealthConnectSheet,
                                onFoodDatabaseClick = viewModel::openFoodDatabase,
                                onCopyDayToTomorrowClick = viewModel::copySelectedDayToTomorrow,
                            )
```

- [ ] **Step 5: Build**

Run: `.\gradlew.bat lintDebug assembleDebug --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL (no unresolved `MoreSection`, `FoodDatabasePreview`, or old overflow params).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/musfit/ui/food/FoodScreen.kt
git commit -m "feat(food): split More details into Today's summary and Trends; move reference features to tools menu"
```

---

## Task 7: Full verification and on-device pass

**Files:** none (verification only)

- [ ] **Step 1: Full verification build**

Run: `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL, all unit tests PASS.

- [ ] **Step 2: Install on the seeded emulator/device**

```powershell
.\scripts\android\install-seed-musfit.ps1 -Reset
adb shell monkey -p com.musfit -c android.intent.category.LAUNCHER 1
```

- [ ] **Step 3: Verify the restructured diary on-device**

Capture screenshots / UI-tree evidence of:
- The hero card showing the calorie ring **and** P/C/F macro bars together; macro labels legible on the accent container (if low-contrast, adjust `HeroMacroStrip` `contentColor` alpha).
- The 3-up quick-actions row (Recipes · Plan week · Water); tapping Plan enters planning mode and the tile reads selected with the week strip in the hero.
- The FAB opening Add-food defaulted to the current time's meal; the meal is still switchable in the panel.
- The Water tile opening the Water sheet; the `⋮` menu reaching Health Connect and Food database sheets.
- The two collapsible groups (Today's summary, Trends) expanding independently.
- The FAB clears the bottom pill nav (adjust the `bottom = 96.dp` padding if it overlaps).

- [ ] **Step 4: Final branch state**

Confirm `git status` is clean and all task commits are present. The branch is ready for the normal PR flow (draft PR with the Gradle + emulator verification in the body), per `AGENTS.md`.

---

## Self-review notes

- **Spec coverage:** unified hero (Task 4), quick-actions row + FAB (Tasks 3, 5), grouped detail (Task 6), tools-menu demotion (Task 6), Water/Health Connect sheets (Task 2), header Plan-chip removal + dead-branch cleanup (Tasks 5, 2), FAB default-meal (Task 1). Meal diary cards and all other sheets are deliberately untouched.
- **Preserved-by-design:** `MacroProgressRow`/`MacroProgressColumn` stay (used by `AddFoodScreen`); `toPlanningModePresentation`/`FoodPlanningModePresentation` stay (used by the hero and `FoodPlanningModePresentationTest`).
- **Type consistency:** new symbols — `defaultAddMealId(sections, hour)`, `FoodSheetMode.Water`, `FoodSheetMode.HealthConnect`, `openWaterSheet()`, `openHealthConnectSheet()`, `FoodQuickActionsRow`, `FoodQuickActionTile`, `HeroMacroStrip`, `CollapsibleGroup` — are used with the same signatures at every call site above.
