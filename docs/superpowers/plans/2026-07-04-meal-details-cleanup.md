# Meal Details Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the redundant data on the full-screen meal details page and fold meal-level nutrition detail behind an expander, so the page reads dense instead of cluttered.

**Architecture:** Pure Jetpack Compose presentation change, entirely within `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`. No ViewModel, repository, DAO, domain, or Room/migration changes. The three meal-level nutrition components (`RatingFactorColumn`, `AdvancedNutritionProgressColumn`, `MicronutrientGrid`) already exist and already receive their data from `FoodMealSectionUiState`; we only change *where and whether* they render. Expander open/close state is ephemeral local `rememberSaveable`.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Gradle. Verify with the project toolchain (JDK 17 + Android SDK) sourced from the main tree.

**Note on TDD:** This slice adds no new pure logic or ViewModel state — the only new state is a local `rememberSaveable` UI toggle, which the project's test setup (JUnit ViewModel tests with fakes; no Robolectric/Compose UI tests) cannot exercise without instrumentation. Per the design spec, no new unit test is warranted. Each task is therefore **edit → compile-check → commit**, with a final task running the full verification build plus an on-device visual check. Existing `FoodViewModelTest` coverage must stay green throughout.

---

## Preflight

- [ ] **Step 0: Source the toolchain env** (needed once per shell, run from the main tree so User-scope env is inherited)

Run (PowerShell):
```powershell
. .\.superpowers\sdd\android-env.ps1
```
Expected: no error; `java -version` reports 17. If the script is missing, ensure JDK 17 + Android SDK (compileSdk 37, minSdk 28) are on PATH before continuing.

---

## File Structure

Single file touched: `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`.

| Composable | Line (approx, pre-edit) | Change |
|---|---|---|
| `DiaryEntryRow` | 2155 | Drop `showContributions` param + branch; merge qty/macros into one line; move `Planned` badge into the name row |
| `MealItemContributionBars` | 2233 | Delete (dead after above) |
| `MealItemContributionBar` | 2273 | Delete (dead after above) |
| meal-detail entries call site | 1813 | Stop passing `showContributions = true` |
| `MealDetailMacroCard` | 1882 | Slim header to `540 / 600 kcal` + rating pill; wrap rating factors + advanced nutrition + micronutrients in a "More nutrition" expander |
| `MealDetailScreen` header | 1701 | Move `Copy yesterday` / `Save template` into a `⋯` overflow menu; delete the standalone chip row |
| `MealDetailScreen` sort area | 1773 | Replace the sort `FilterChip` row with a count + sort dropdown |
| `MealDetailSortChips` | 1826 | Replace with `MealDetailSortMenu` (dropdown, same modes) |

All required imports (`DropdownMenu`, `DropdownMenuItem`, `IconButton`, `Icons.Filled.MoreVert`, `Icons.Filled.ExpandMore`, `Icons.Filled.ExpandLess`, `rememberSaveable`, `mutableStateOf`, `remember`) are already present in `FoodScreen.kt`. `FilterChip` stays imported (still used at line 727).

---

## Task 1: Collapse the logged-item rows

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/food/FoodScreen.kt` (`DiaryEntryRow`, its call site, and the two contribution-bar composables)

- [ ] **Step 1: Replace `DiaryEntryRow` with the single-line version**

Find the entire `DiaryEntryRow` composable (starts `private fun DiaryEntryRow(`) and replace it with:

```kotlin
@Composable
private fun DiaryEntryRow(
    entry: FoodMealEntryUiState,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = MusFitTheme.shapes.extraLarge,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FoodThumb(
                imageUrl = entry.imageUrl,
                fallback = Icons.Outlined.Restaurant,
                modifier = Modifier.padding(end = 12.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = entry.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    entry.rating?.let { rating ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(rating.tone.ratingColor()),
                        )
                    }
                    if (entry.isPlanned) {
                        Text(
                            text = "Planned",
                            style = MaterialTheme.typography.labelSmall,
                            color = MusFitTheme.colors.brand,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Text(
                    text = "${entry.quantityGrams.roundToInt()} g · P ${entry.proteinGrams.formatNutritionDisplay()}  C ${entry.carbsGrams.formatNutritionDisplay()}  F ${entry.fatGrams.formatNutritionDisplay()} g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(
                text = "${entry.caloriesKcal.roundToInt()} kcal",
                style = MaterialTheme.typography.bodyMedium,
                color = MusFitTheme.colors.brand,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
```

(Note: keep the `@Composable` annotation line that precedes the original `private fun DiaryEntryRow`.)

- [ ] **Step 2: Delete the two contribution-bar composables**

Delete the entire `MealItemContributionBars` composable (starts `@Composable` / `private fun MealItemContributionBars(entry: FoodMealEntryUiState) {`) **and** the entire `MealItemContributionBar` composable (starts `private fun MealItemContributionBar(`) that immediately follows it. Both are now unused.

- [ ] **Step 3: Update the meal-detail entries call site**

Find (inside `MealDetailScreen`, the entries `Card`):
```kotlin
                        DiaryEntryRow(
                            entry = entry,
                            showContributions = true,
                            onClick = { onEntryClick(entry.id) },
                        )
```
Replace with:
```kotlin
                        DiaryEntryRow(
                            entry = entry,
                            onClick = { onEntryClick(entry.id) },
                        )
```

- [ ] **Step 4: Compile-check**

Run:
```powershell
.\gradlew.bat compileDebugKotlin --no-daemon --console=plain
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/musfit/ui/food/FoodScreen.kt
git commit -m "refactor(food): collapse meal-detail item rows, drop contribution bars"
```

---

## Task 2: Rework the meal summary card

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/food/FoodScreen.kt` (`MealDetailMacroCard`)

- [ ] **Step 1: Replace the whole `MealDetailMacroCard` composable**

Find the entire `MealDetailMacroCard` composable (starts `private fun MealDetailMacroCard(meal: FoodMealSectionUiState) {`) and replace it with:

```kotlin
@Composable
private fun MealDetailMacroCard(meal: FoodMealSectionUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MusFitTheme.colors.surface),
        shape = MusFitTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "${meal.caloriesKcal.roundToInt()}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MusFitTheme.colors.brandInk,
                    )
                    Text(
                        text = "/ ${meal.calorieTargetKcal.roundToInt()} kcal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MusFitTheme.colors.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                meal.rating?.let { rating -> RatingPill(rating) }
            }

            ProgressBar(
                progress = meal.calorieProgress.toFloat(),
                color = MusFitTheme.colors.brand,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MealMacroMetric(
                    label = meal.carbsLabel,
                    grams = meal.effectiveCarbsGrams,
                    goalGrams = meal.carbsGoalGrams,
                    color = MusFitTheme.colors.macroColors[0],
                    modifier = Modifier.weight(1f),
                )
                MealMacroMetric(
                    label = "Protein",
                    grams = meal.proteinGrams,
                    goalGrams = meal.proteinGoalGrams,
                    color = MusFitTheme.colors.macroColors[1],
                    modifier = Modifier.weight(1f),
                )
                MealMacroMetric(
                    label = "Fat",
                    grams = meal.fatGrams,
                    goalGrams = meal.fatGoalGrams,
                    color = MusFitTheme.colors.macroColors[2],
                    modifier = Modifier.weight(1f),
                )
            }

            val ratingFactors = meal.rating?.factors?.takeIf { it.isNotEmpty() }
            val hasDetail = ratingFactors != null ||
                meal.advancedNutritionProgress.isNotEmpty() ||
                meal.micronutrients.isNotEmpty()
            if (hasDetail) {
                var detailExpanded by rememberSaveable(meal.id) { mutableStateOf(false) }
                HorizontalDivider(color = MusFitTheme.colors.outline)
                Surface(
                    onClick = { detailExpanded = !detailExpanded },
                    color = Color.Transparent,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "More nutrition",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Icon(
                            imageVector = if (detailExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (detailExpanded) "Collapse nutrition detail" else "Expand nutrition detail",
                            tint = MusFitTheme.colors.onSurfaceVariant,
                        )
                    }
                }
                if (detailExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        ratingFactors?.let { RatingFactorColumn(it) }
                        if (meal.advancedNutritionProgress.isNotEmpty()) {
                            AdvancedNutritionProgressColumn(meal.advancedNutritionProgress)
                        }
                        if (meal.micronutrients.isNotEmpty()) {
                            MicronutrientGrid(meal.micronutrients)
                        }
                    }
                }
            }
        }
    }
}
```

What changed vs. the original: the `"Meal intake"` title and the trailing `"Target … kcal"` caption are gone; the calorie total + target is now the single `540 / 600 kcal` headline with the rating pill beside it; and the rating-factor list, advanced-nutrition grid, and micronutrient grid are now inside the collapsed-by-default `More nutrition` expander instead of always-visible.

- [ ] **Step 2: Compile-check**

Run:
```powershell
.\gradlew.bat compileDebugKotlin --no-daemon --console=plain
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```powershell
git add app/src/main/java/com/musfit/ui/food/FoodScreen.kt
git commit -m "refactor(food): slim meal summary card, fold nutrition detail into expander"
```

---

## Task 3: Header overflow menu + sort dropdown

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/food/FoodScreen.kt` (`MealDetailScreen` header + sort area, and `MealDetailSortChips`)

- [ ] **Step 1: Move the meal actions into a header overflow menu**

Inside `MealDetailScreen`, find the header row plus the standalone action-chip row:
```kotlin
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MusFitOutlinedButton(
                onClick = onBackClick,
                modifier = Modifier.size(48.dp),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text("<", style = MaterialTheme.typography.titleLarge)
            }
            Text(
                text = meal.title.uppercase(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MusFitOutlinedButton(onClick = onCopyYesterdayClick) {
                Text("Copy yesterday")
            }
            MusFitOutlinedButton(onClick = onSaveTemplateClick) {
                Text("Save template")
            }
        }
```
Replace both rows with a single header row:
```kotlin
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MusFitOutlinedButton(
                onClick = onBackClick,
                modifier = Modifier.size(48.dp),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text("<", style = MaterialTheme.typography.titleLarge)
            }
            Text(
                text = meal.title.uppercase(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            var menuOpen by remember { mutableStateOf(false) }
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "Meal actions",
                    tint = MusFitTheme.colors.onSurfaceVariant,
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Copy yesterday") },
                    onClick = { menuOpen = false; onCopyYesterdayClick() },
                )
                DropdownMenuItem(
                    text = { Text("Save template") },
                    onClick = { menuOpen = false; onSaveTemplateClick() },
                )
            }
        }
```

- [ ] **Step 2: Replace the sort chip row with a count + sort dropdown**

Inside `MealDetailScreen`, find:
```kotlin
        SectionTitle("Logged items")
        if (meal.entries.isNotEmpty()) {
            MealDetailSortChips(
                selectedSortMode = sortMode,
                onSortModeChanged = onSortModeChanged,
            )
        }
```
Replace with:
```kotlin
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle("Logged items")
            if (meal.entries.isNotEmpty()) {
                MealDetailSortMenu(
                    selectedSortMode = sortMode,
                    onSortModeChanged = onSortModeChanged,
                )
            }
        }
```

- [ ] **Step 3: Replace `MealDetailSortChips` with `MealDetailSortMenu`**

Find the entire `MealDetailSortChips` composable (starts `@Composable` / `private fun MealDetailSortChips(`) and replace it with:

```kotlin
@Composable
private fun MealDetailSortMenu(
    selectedSortMode: MealDetailSortMode,
    onSortModeChanged: (MealDetailSortMode) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { open = true },
            color = Color.Transparent,
            shape = MusFitTheme.shapes.extraLarge,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selectedSortMode.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = "Sort logged items",
                    tint = MusFitTheme.colors.onSurfaceVariant,
                )
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            MealDetailSortChoices.forEach { choice ->
                DropdownMenuItem(
                    text = { Text(choice.label) },
                    onClick = { open = false; onSortModeChanged(choice) },
                )
            }
        }
    }
}
```

This reuses the existing `MealDetailSortChoices` list and `MealDetailSortMode.label`, so the four modes (Logged, Calories, Protein, Name) and the `onSortModeChanged` wiring are unchanged.

- [ ] **Step 4: Compile-check**

Run:
```powershell
.\gradlew.bat compileDebugKotlin --no-daemon --console=plain
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/musfit/ui/food/FoodScreen.kt
git commit -m "refactor(food): meal-detail actions to overflow menu, sort chips to dropdown"
```

---

## Task 4: Full verification

**Files:** none (verification only)

- [ ] **Step 1: Run the full verification build**

Run:
```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain
```
Expected: `BUILD SUCCESSFUL`; `FoodViewModelTest` and the rest of `testDebugUnitTest` pass; `lintDebug` reports no new errors; APK at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 2: Install and open on the dev phone**

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell monkey -p com.musfit -c android.intent.category.LAUNCHER 1
```
Then, in the app: open the **Food** tab, tap a meal that has logged items to open the full-screen meal details page.

- [ ] **Step 3: Visual check against the design**

Confirm on-device:
- Each logged item is one line (name + rating dot, then `weight · P C F`, calories on the right) — **no P/C/F or Calories bars** under items.
- The summary card shows a single `<kcal> / <target> kcal` headline with the rating pill beside it — no `"Meal intake"` title, no separate `"Target"` caption.
- A `More nutrition` row sits below the macro meters, **collapsed by default**; tapping it reveals rating factors → advanced nutrition → micronutrients; tapping again collapses.
- `Copy yesterday` / `Save template` now live under the header `⋯` menu and still work.
- The sort control is a dropdown; changing it re-sorts the item list.

Capture a screenshot for the record (PowerShell `>` corrupts PNGs — use `adb pull`):
```powershell
adb exec-out screencap -p /sdcard/meal-detail.png
adb pull /sdcard/meal-detail.png .
```

- [ ] **Step 4: Finalize**

If everything looks right, the branch is ready. Use the finishing-a-development-branch skill to decide on merge/PR. Do **not** push unless the user asks.

---

## Self-Review

- **Spec coverage:** Item-row redundancy (markers ① ②) → Task 1. Overloaded summary card / advanced nutrition + micros behind expander (marker ③) → Task 2. Chrome tidy-up (overflow menu + sort dropdown) → Task 3. Testing / full verification → Task 4. No spec requirement is left without a task.
- **Placeholder scan:** No TBD/TODO/"handle edge cases"/"similar to Task N" — every code step shows the full composable.
- **Type consistency:** `MealDetailSortMenu` is defined in Task 3 Step 3 and referenced in Task 3 Step 2; it consumes the existing `MealDetailSortMode`, `MealDetailSortChoices`, and `.label`. `DiaryEntryRow`'s new 2-arg signature (Task 1 Step 1) matches its only call site (Task 1 Step 3). The expander reuses the existing `RatingFactorColumn`, `AdvancedNutritionProgressColumn`, `MicronutrientGrid` signatures unchanged.
- **Scope:** One file, no data-model/VM/DB changes, single coherent plan.
