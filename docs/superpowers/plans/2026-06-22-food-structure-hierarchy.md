# Food Structure & Hierarchy (Slice 2) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restructure the Food screen to the north-star — header with overflow menu + centered date nav, meals surfaced under the macros, secondary sections behind a collapsed "More" expander, category-icon meal cards, a coral FAB, and real bottom-nav icons.

**Architecture:** Pure Compose UI restructure on top of Slice 1's `MusFitTheme` tokens. No data/ViewModel logic changes — the "More" toggle and overflow menu are UI-local; the FAB and menu reuse existing ViewModel handlers. Adds `material-icons-extended` for iconography.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, `androidx.compose.material:material-icons-extended`, Gradle, adb screenshots.

**Spec:** `docs/superpowers/specs/2026-06-21-food-structure-hierarchy-design.md`

**Prerequisite for Gradle/adb (Windows PowerShell):** `. .\.superpowers\sdd\android-env.ps1`

---

## File Structure

| File | Responsibility | Action |
|---|---|---|
| `gradle/libs.versions.toml` | Declare icons-extended | Modify |
| `app/build.gradle.kts` | Add icons-extended dep | Modify |
| `app/src/main/java/com/musfit/ui/AppDestination.kt` | Add `icon` per destination | Modify |
| `app/src/main/java/com/musfit/ui/AppNavGraph.kt` | Render nav icons | Modify |
| `app/src/main/java/com/musfit/ui/food/FoodScreen.kt` | Header, FAB, meal-card icons, "More" expander, body order | Modify |

---

## Task 1: Add `material-icons-extended`

**Files:** `gradle/libs.versions.toml`, `app/build.gradle.kts`

- [ ] **Step 1: Declare the library** — in `libs.versions.toml`, under `[libraries]`, add (version comes from the Compose BOM, so omit `version.ref`):

```toml
androidx-compose-material-icons-extended = { module = "androidx.compose.material:material-icons-extended" }
```

- [ ] **Step 2: Add the dependency** — in `app/build.gradle.kts`, in `dependencies { }` near the other compose deps:

```kotlin
implementation(libs.androidx.compose.material.icons.extended)
```

- [ ] **Step 3: Verify it resolves**

Run: `.\gradlew.bat :app:compileDebugKotlin --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "build: add material-icons-extended

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 2: Bottom-nav icons

**Files:** `app/src/main/java/com/musfit/ui/AppDestination.kt`, `app/src/main/java/com/musfit/ui/AppNavGraph.kt`

- [ ] **Step 1: Add `icon` to `AppDestination`**

```kotlin
package com.musfit.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Today
import androidx.compose.ui.graphics.vector.ImageVector

const val BARCODE_SCANNER_ROUTE = "barcode-scanner"

enum class AppDestination(val route: String, val label: String, val icon: ImageVector) {
    Today(route = "today", label = "Today", icon = Icons.Outlined.Today),
    Food(route = "food", label = "Food", icon = Icons.Outlined.Restaurant),
    Training(route = "training", label = "Training", icon = Icons.Outlined.FitnessCenter),
    Health(route = "health", label = "Health", icon = Icons.Outlined.MonitorHeart),
}
```

- [ ] **Step 2: Render the icon in `AppNavGraph`**

In `AppNavGraph.kt`, replace the `icon = { Text(destination.label.first().toString()) }` lambda in the `NavigationBarItem` with:

```kotlin
icon = { Icon(destination.icon, contentDescription = destination.label) },
```

Add the import `import androidx.compose.material3.Icon`.

- [ ] **Step 3: Verify**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.AppDestinationTest" :app:compileDebugKotlin --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL (the existing `AppDestinationTest` still passes — adding a field with provided values does not change route/label behavior).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/musfit/ui/AppDestination.kt app/src/main/java/com/musfit/ui/AppNavGraph.kt
git commit -m "feat(nav): real Material icons for bottom navigation

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 3: Header rework

**Files:** `app/src/main/java/com/musfit/ui/food/FoodScreen.kt` (the `FoodSummaryHeader` composable)

Replace `FoodSummaryHeader`'s body. Keep the `Box` + `brandGradient` background and padding; replace the inner
`Column` (title row + "Quick calories" button + the ten-button strip + the metrics row) with three rows:
title + overflow menu, centered date nav, and the Eaten/Ring/Goal metrics row.

- [ ] **Step 1: Rewrite `FoodSummaryHeader`**

Required imports (add if missing): `androidx.compose.material3.DropdownMenu`, `androidx.compose.material3.DropdownMenuItem`, `androidx.compose.material3.Icon`, `androidx.compose.material3.IconButton`, `androidx.compose.material.icons.Icons`, `androidx.compose.material.icons.filled.MoreVert`, `androidx.compose.material.icons.filled.ChevronLeft`, `androidx.compose.material.icons.filled.ChevronRight`, `androidx.compose.runtime.getValue`, `androidx.compose.runtime.setValue`, `androidx.compose.runtime.mutableStateOf`, `androidx.compose.runtime.remember`.

Body (keep the existing `FoodSummaryHeader` parameter list; `onQuickAddClick` is no longer used here — quick logging stays reachable from the add-food flow):

```kotlin
Box(
    modifier = Modifier
        .fillMaxWidth()
        .background(Brush.linearGradient(MusFitTheme.colors.brandGradient))
        .padding(start = 20.dp, top = 18.dp, end = 8.dp, bottom = 24.dp),
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Food",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MusFitTheme.colors.brandInk,
            )
            Box {
                var menuOpen by remember { mutableStateOf(false) }
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More actions", tint = MusFitTheme.colors.brandInk)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Goals") }, onClick = { menuOpen = false; onGoalClick() })
                    DropdownMenuItem(text = { Text("Meals") }, onClick = { menuOpen = false; onMealsClick() })
                    DropdownMenuItem(text = { Text("Templates") }, onClick = { menuOpen = false; onTemplatesClick() })
                    DropdownMenuItem(text = { Text("Recipes") }, onClick = { menuOpen = false; onRecipeClick() })
                    DropdownMenuItem(text = { Text("Shopping list") }, onClick = { menuOpen = false; onShoppingClick() })
                    DropdownMenuItem(
                        text = { Text(if (state.isPlanningMode) "Planning: on" else "Planning: off") },
                        onClick = { menuOpen = false; onPlanningModeClick() },
                    )
                    DropdownMenuItem(text = { Text("Copy day to tomorrow") }, onClick = { menuOpen = false; onCopyDayToTomorrowClick() })
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPreviousDayClick) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous day", tint = MusFitTheme.colors.brandInk)
            }
            Text(
                text = state.selectedDate.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MusFitTheme.colors.brandInk,
                modifier = Modifier.clickable(onClick = onTodayClick),
            )
            IconButton(onClick = onNextDayClick) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Next day", tint = MusFitTheme.colors.brandInk)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(end = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SummarySideMetric(label = "Eaten", value = state.eatenCaloriesKcal)
            CalorieRing(
                eatenCalories = state.eatenCaloriesKcal,
                remainingCalories = state.remainingCaloriesKcal,
                calorieGoal = state.calorieGoalKcal,
            )
            SummarySideMetric(label = "Goal", value = state.calorieGoalKcal)
        }
    }
}
```

(Add `import androidx.compose.foundation.clickable` if not present.)

- [ ] **Step 2: Verify**

Run: `.\gradlew.bat :app:compileDebugKotlin --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/musfit/ui/food/FoodScreen.kt
git commit -m "feat(food): rework header with overflow menu and date nav

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 4: Meal-card category icons + coral add button

**Files:** `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`

- [ ] **Step 1: Add a `mealTypeIcon` helper** (near `MealInitial`):

```kotlin
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.DinnerDining
import androidx.compose.material.icons.outlined.LunchDining
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector

private fun mealTypeIcon(id: String, title: String): ImageVector {
    val key = (id + " " + title).lowercase()
    return when {
        "breakfast" in key -> Icons.Outlined.BakeryDining
        "lunch" in key -> Icons.Outlined.LunchDining
        "dinner" in key -> Icons.Outlined.DinnerDining
        "snack" in key -> Icons.Outlined.Cookie
        else -> Icons.Outlined.Restaurant
    }
}
```

- [ ] **Step 2: Replace the avatar + add button in `MealSectionCard`**

Replace the `MealInitial(title = meal.title)` call with a category-icon avatar:

```kotlin
Box(
    modifier = Modifier
        .size(54.dp)
        .clip(MusFitTheme.shapes.medium)
        .background(MusFitTheme.colors.surfaceVariant),
    contentAlignment = Alignment.Center,
) {
    Icon(
        imageVector = mealTypeIcon(meal.id, meal.title),
        contentDescription = null,
        tint = MusFitTheme.colors.brand,
    )
}
```

Replace the add `Button(...)` (the one with `Text("+")`) with a coral icon button:

```kotlin
Button(
    onClick = onAddClick,
    colors = ButtonDefaults.buttonColors(
        containerColor = MusFitTheme.colors.accent,
        contentColor = MusFitTheme.colors.onAccent,
    ),
    shape = CircleShape,
    modifier = Modifier.size(48.dp),
    contentPadding = PaddingValues(0.dp),
) {
    Icon(Icons.Filled.Add, contentDescription = "Add to ${meal.title}")
}
```

Add imports: `androidx.compose.material.icons.filled.Add`, `androidx.compose.material3.Icon`. Leave `MealInitial`
defined (it may be unused now; if lint flags it, delete the `MealInitial` composable).

- [ ] **Step 3: Verify**

Run: `.\gradlew.bat :app:compileDebugKotlin --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/musfit/ui/food/FoodScreen.kt
git commit -m "feat(food): category icons and coral add on meal cards

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 5: Coral FAB

**Files:** `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`

The screen's top-level `Box` (the `if (selectedMealDetail != null) … else …` container) is the FAB host. Add the
FAB as a sibling of the content, aligned bottom-end, shown only on the diary view (not meal-detail) and when no
bottom sheet is open.

- [ ] **Step 1: Add the FAB overlay**

Inside the top-level `Box` (after the `if/else` content block, still inside the `Box`), add:

```kotlin
if (selectedMealDetail == null && !state.isAddPanelVisible) {
    FloatingActionButton(
        onClick = {
            val mealId = state.mealSections.firstOrNull()?.id ?: "breakfast"
            viewModel.openAddFood(mealId)
        },
        containerColor = MusFitTheme.colors.accent,
        contentColor = MusFitTheme.colors.onAccent,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 20.dp, bottom = 24.dp),
    ) {
        Icon(Icons.Filled.Add, contentDescription = "Add food")
    }
}
```

Add imports: `androidx.compose.material3.FloatingActionButton`. (`Icons.Filled.Add`, `Icon`, `Alignment` already imported from Task 4 / existing code.)

- [ ] **Step 2: Verify**

Run: `.\gradlew.bat :app:compileDebugKotlin --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/musfit/ui/food/FoodScreen.kt
git commit -m "feat(food): coral FAB opens the add-food flow

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 6: "More" expander + body order

**Files:** `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`

Reorder the diary `Column` (the `else` branch's scrolling `Column`, currently: WeeklyPlanStrip, EmptyDiaryStartCard,
MacroProgressRow, AdvancedNutritionProgressRow, DayRatingCard, DailyInsightsSection, MicronutrientRow,
WaterTrackerCard, FoodHealthConnectSyncCard, MessageBanner, SectionTitle("Meal diary"), meal sections,
FoodDatabasePreview) into: macros → message banner → meal diary → "More" expander.

- [ ] **Step 1: Add a `MoreSection` composable**

```kotlin
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore

@Composable
private fun MoreSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            onClick = onToggle,
            color = MusFitTheme.colors.surface,
            shape = MusFitTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "More details",
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

- [ ] **Step 2: Reorder the diary `Column` body**

In the `else` branch's inner `Column` (`Modifier.padding(horizontal = 16.dp)`), set the children to this order
(move the existing composables; do not change their internals). Add `var moreExpanded by rememberSaveable { mutableStateOf(false) }`
in `FoodScreen` above the layout:

```kotlin
MacroProgressRow(state.macroProgress)

MessageBanner(
    message = state.message,
    canUndoDelete = state.lastDeletedDiaryEntry != null,
    onUndoDeleteClick = viewModel::undoDeleteDiaryEntry,
)

SectionTitle("Meal diary")
if (state.isFoodDiaryEmpty) {
    EmptyDiaryStartCard(
        actions = state.emptyDiaryActions,
        onActionClick = { actionType -> /* keep the existing when-block exactly */ },
    )
}
state.mealSections.forEach { meal ->
    MealSectionCard(
        meal = meal,
        onMealClick = { viewModel.openMealDetail(meal.id) },
        onAddClick = { viewModel.openAddFood(meal.id) },
        onEntryClick = viewModel::openDiaryEntryEditor,
    )
}

MoreSection(expanded = moreExpanded, onToggle = { moreExpanded = !moreExpanded }) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DayRatingCard(state.dayRating)
        DailyInsightsSection(state.dailyInsights)
        WeeklyPlanStrip(state.weeklyPlan)
        AdvancedNutritionProgressRow(state.advancedNutritionProgress)
        MicronutrientRow(state.micronutrients)
        WaterTrackerCard(
            state = state,
            onQuickWaterClick = viewModel::logQuickWater,
            onCustomAmountChanged = viewModel::onWaterCustomAmountChanged,
            onCustomAddClick = viewModel::logCustomWater,
            onGoalChanged = viewModel::onWaterGoalChanged,
            onGoalSaveClick = viewModel::saveWaterGoal,
        )
        FoodHealthConnectSyncCard(
            state = state,
            onEnabledChanged = viewModel::onFoodHealthConnectSyncEnabledChanged,
            onRequestPermissionsClick = { /* keep the existing launcher block exactly */ },
            onRefreshClick = viewModel::refreshFoodHealthConnectSync,
            onSyncClick = viewModel::syncFoodToHealthConnect,
        )
        FoodDatabasePreview(
            savedFoods = state.savedFoods,
            onOpenClick = viewModel::openFoodDatabase,
        )
    }
}
```

Note: preserve the exact `onActionClick` when-block and the Health Connect `onRequestPermissionsClick` launcher
lambda from the current code — only their position moves.

- [ ] **Step 3: Verify build + tests + lint**

Run: `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/musfit/ui/food/FoodScreen.kt
git commit -m "feat(food): surface meals; collapse secondary into More

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 7: Full verification & screenshots

- [ ] **Step 1: Full gate** — `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain` → BUILD SUCCESSFUL.
- [ ] **Step 2: Install** — `adb install -r app\build\outputs\apk\debug\app-debug.apk` then launch.
- [ ] **Step 3: Screenshots** (via `adb exec-out screencap -p > file.png`, animations off for reliable taps): Food collapsed, Food with "More" expanded, a meal-detail screen, and the four tabs showing the new nav icons.
- [ ] **Step 4: Confirm** — meals sit under the macros; secondary content hidden until "More"; header shows overflow menu + date nav; coral FAB present and opens the add flow; category icons + coral add render; nav shows real icons. No regressions on the other tabs.

## Definition of Done

- Header: overflow menu + centered date nav + ring; ten-button strip removed.
- Meals directly under macros; all secondary sections inside a collapsed-by-default "More".
- Category-icon meal cards with coral add buttons; coral FAB opens the add flow.
- Real Material bottom-nav icons on all four tabs.
- Build gate green; existing tests pass; screenshots captured.
