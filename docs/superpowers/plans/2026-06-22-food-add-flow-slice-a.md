# Add-Food Flow — Slice A (Add-Screen Shell) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the overloaded add-food bottom sheet with a full-screen, search-first add view — search + barcode icon, daily-intake card, Recents/Favorites tabs (Create is a stub here, built in Slice B), and a ⋮ menu with Quick track + Adjust goals — reusing the existing search/favorites/logging/barcode logic and adding new "recents" data.

**Architecture:** The add UI moves from a `ModalBottomSheet` to a full-screen `AddFoodScreen` rendered (like the existing meal-detail screen) when `state.isAddPanelVisible` is true — so the existing `openAddFood`/`closeAddFood` state and the FAB/meal-"+" entry points keep working with no nav-graph change. The only new data is recents queries over the diary. The AI forms and the standalone label button are removed.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Room (new recents queries), Robolectric tests, Gradle, adb.

**Spec:** `docs/superpowers/specs/2026-06-22-food-add-flow-streamline-design.md`

**Prereq:** `. .\.superpowers\sdd\android-env.ps1`

**Scope note:** Slice A only. The **Create** tab (food + meal/recipe creation with autofill) is Slice B; **on-device label OCR** is Slice C. In Slice A the Create tab shows a simple "Create a food" entry that routes to the existing manual-create path.

---

## File Structure

| File | Responsibility | Action |
|---|---|---|
| `data/local/dao/FoodDao.kt` | Recents + same-as-yesterday queries | Modify |
| `data/repository/FoodRepository.kt` | Recents repo methods + models | Modify |
| `ui/food/FoodViewModel.kt` | Recents add-screen state + active tab | Modify |
| `ui/food/AddFoodScreen.kt` | New full-screen add view | Create |
| `ui/food/FoodScreen.kt` | Render AddFoodScreen instead of the bottom sheet; delete AI forms + label button | Modify |

---

## Task 1: Recents data (DAO + repository + ViewModel)

**Files:** `FoodDao.kt`, `FoodRepository.kt`, `FoodViewModel.kt`, test `LocalFoodRepositoryTest.kt`

- [ ] **Step 1: Write the failing repository test**

In `app/src/test/java/com/musfit/data/repository/LocalFoodRepositoryTest.kt`, add:

```kotlin
@Test
fun recentFoods_returnsDistinctFoodsMostRecentFirst() = runTest {
    val day = LocalDate.of(2026, 6, 22)
    // log Egg, then Ice cream, then Egg again (same day, breakfast)
    repository.logFood(barcodeLog(name = "Egg", calories = 155.0, mealType = "breakfast", date = day))
    repository.logFood(barcodeLog(name = "Ice cream", calories = 200.0, mealType = "lunch", date = day))
    repository.logFood(barcodeLog(name = "Egg", calories = 155.0, mealType = "breakfast", date = day))

    val recents = repository.observeRecentFoods(limit = 10).first()
    assertEquals(listOf("Egg", "Ice cream"), recents.map { it.name }) // distinct, Egg most recent
}

@Test
fun sameAsYesterday_returnsFoodsLoggedForThatMealYesterday() = runTest {
    val yesterday = LocalDate.of(2026, 6, 21)
    val today = LocalDate.of(2026, 6, 22)
    repository.logFood(barcodeLog(name = "Oats", calories = 300.0, mealType = "breakfast", date = yesterday))
    repository.logFood(barcodeLog(name = "Steak", calories = 400.0, mealType = "dinner", date = yesterday))

    val same = repository.observeSameAsYesterday(mealType = "breakfast", date = today).first()
    assertEquals(listOf("Oats"), same.map { it.name })
}
```

Add a small helper near the other test builders:

```kotlin
private fun barcodeLog(name: String, calories: Double, mealType: String, date: LocalDate) =
    FoodLogInput(
        lookupResult = null,
        barcode = null,
        name = name,
        brand = null,
        nutritionPer100g = nutrition(calories = calories, protein = 1.0, carbs = 1.0, fat = 1.0),
        servingGrams = 100.0,
        mealType = mealType,
        quantityGrams = 100.0,
        date = date,
    )
```

- [ ] **Step 2: Run it — expect FAIL**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.musfit.data.repository.LocalFoodRepositoryTest" --no-daemon --console=plain`
Expected: FAIL — unresolved `observeRecentFoods` / `observeSameAsYesterday`.

- [ ] **Step 3: Add the DAO queries** in `FoodDao.kt`:

```kotlin
@Query(
    "SELECT foods.* FROM foods " +
        "INNER JOIN meal_items ON meal_items.foodId = foods.id " +
        "INNER JOIN meals ON meals.id = meal_items.mealId " +
        "GROUP BY foods.id " +
        "ORDER BY MAX(meals.createdAtEpochMillis) DESC " +
        "LIMIT :limit",
)
fun observeRecentFoods(limit: Int): Flow<List<FoodEntity>>

@Query(
    "SELECT foods.* FROM foods " +
        "INNER JOIN meal_items ON meal_items.foodId = foods.id " +
        "INNER JOIN meals ON meals.id = meal_items.mealId " +
        "WHERE meals.dateEpochDay = :dateEpochDay AND meals.type = :mealType " +
        "GROUP BY foods.id " +
        "ORDER BY MAX(meals.createdAtEpochMillis) DESC",
)
fun observeSameAsYesterday(dateEpochDay: Long, mealType: String): Flow<List<FoodEntity>>
```

- [ ] **Step 4: Add repository methods** in `FoodRepository.kt` (interface + `LocalFoodRepository`), reusing `toSavedFoodItem`:

```kotlin
// interface
fun observeRecentFoods(limit: Int = 20): Flow<List<SavedFoodItem>>
fun observeSameAsYesterday(mealType: String, date: LocalDate): Flow<List<SavedFoodItem>>

// LocalFoodRepository
override fun observeRecentFoods(limit: Int): Flow<List<SavedFoodItem>> =
    foodDao.observeRecentFoods(limit).map { foods -> foods.map { it.toSavedFoodItem(emptyList()) } }

override fun observeSameAsYesterday(mealType: String, date: LocalDate): Flow<List<SavedFoodItem>> =
    foodDao.observeSameAsYesterday(date.minusDays(1).toEpochDay(), mealType)
        .map { foods -> foods.map { it.toSavedFoodItem(emptyList()) } }
```

- [ ] **Step 5: Run the test — expect PASS**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.musfit.data.repository.LocalFoodRepositoryTest" --no-daemon --console=plain`
Expected: PASS.

- [ ] **Step 6: Expose in the ViewModel.** In `FoodViewModel.kt`, add to `FoodUiState`:
  `val recentFoods: List<SavedFoodUiState> = emptyList()`, `val sameAsYesterday: List<SavedFoodUiState> = emptyList()`,
  and `val addTab: AddTab = AddTab.Recents` with `enum class AddTab { Recents, Favorites, Create }` + a
  `fun selectAddTab(tab: AddTab)` handler. Combine `observeRecentFoods()` and (keyed on the open meal +
  selected date) `observeSameAsYesterday(...)` into the state flow that builds `FoodUiState` (follow the existing
  `combine`/`flatMapLatest` pattern used for the diary), mapping each `SavedFoodItem` with the existing
  `toUiState()`.

- [ ] **Step 7: Verify compile** — `.\gradlew.bat :app:compileDebugKotlin --no-daemon --console=plain` → BUILD SUCCESSFUL.

- [ ] **Step 8: Commit** — `feat(food): add recents and same-as-yesterday data`.

---

## Task 2: AddFoodScreen (the full-screen add view)

**Files:** Create `ui/food/AddFoodScreen.kt`.

Build a full-screen composable. Reuse existing state/handlers — do not duplicate logic.

- [ ] **Step 1: Create `AddFoodScreen.kt`** with this structure (fill the row/section bodies using the existing
  `FoodThumb` + food-row patterns from `FoodScreen.kt`):

```kotlin
@Composable
fun AddFoodScreen(
    state: FoodUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onScanClick: () -> Unit,
    onTabSelected: (AddTab) -> Unit,
    onFoodClick: (String) -> Unit,          // logSavedFood(foodId)
    onQuickTrack: () -> Unit,               // open Quick track
    onAdjustGoals: () -> Unit,              // openGoalEditor
    onCreateFood: () -> Unit,               // manual create (Slice B expands this)
) {
    Column(Modifier.fillMaxSize().background(MusFitTheme.colors.background)) {
        // Top bar: back · meal title · overflow (DropdownMenu: Quick track, Adjust goals)
        // Search row: OutlinedTextField(value = state.foodDatabaseQuery, onValueChange = onQueryChange,
        //   trailing = barcode IconButton(onScanClick) using Icons.Outlined.QrCodeScanner)
        // DailyIntakeCard(state)   // calories x/goal + macroProgress bars (reuse MacroProgressRow visuals)
        // Tabs: Recents / Favorites / Create  (onTabSelected)
        // when (state.addTab):
        //   Recents -> if query blank: SAME AS YESTERDAY? (state.sameAsYesterday) + LAST TRACKED +
        //              ALL RECENTS (state.recentFoods); if query non-blank: search results
        //              (state.savedFoods + state.onlineFoodResults)
        //   Favorites -> state.favoriteAddItems rows
        //   Create -> a single "Create a food" button -> onCreateFood (Slice B replaces with the full Create UI)
    }
}

@Composable
private fun DailyIntakeCard(state: FoodUiState) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MusFitTheme.colors.surface),
        shape = MusFitTheme.shapes.large,
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Daily intake", fontWeight = FontWeight.Bold, color = MusFitTheme.colors.onSurface)
                Text("${state.eatenCaloriesKcal.roundToInt()} / ${state.calorieGoalKcal.roundToInt()} kcal",
                    fontWeight = FontWeight.Bold, color = MusFitTheme.colors.onSurface)
            }
            Spacer(Modifier.height(10.dp))
            MacroProgressRow(state.macroProgress)   // existing composable
        }
    }
}
```

  Each food row reuses `FoodThumb(imageUrl, Icons.Outlined.Restaurant)` + name + `kcal · serving` + a coral
  add button (the `MealSectionCard` add-button style), calling `onFoodClick(food.id)`.

- [ ] **Step 2: Verify compile** — `.\gradlew.bat :app:compileDebugKotlin --no-daemon --console=plain` → BUILD SUCCESSFUL.
- [ ] **Step 3: Commit** — `feat(food): add full-screen AddFoodScreen`.

---

## Task 3: Wire it in & remove the old add UI

**Files:** `ui/food/FoodScreen.kt`

- [ ] **Step 1: Render AddFoodScreen instead of the sheet.** In `FoodScreen`'s top-level `Box`, add a branch
  (like the `selectedMealDetail` branch): when `state.isAddPanelVisible`, show
  `AddFoodScreen(state = state, onBack = viewModel::closeAddFood, onQueryChange = viewModel::onFoodDatabaseQueryChanged,
  onScanClick = onScanClick, onTabSelected = viewModel::selectAddTab, onFoodClick = viewModel::logSavedFood,
  onQuickTrack = { viewModel.selectAddMode(FoodAddMode.Quick) /* opens quick UI */ },
  onAdjustGoals = viewModel::openGoalEditor, onCreateFood = { viewModel.selectAddMode(FoodAddMode.Manual) })`
  and hide the diary/FAB underneath (return early, as meal-detail does).

- [ ] **Step 2: Delete the old add UI.** Remove the `if (state.isAddPanelVisible) { ModalBottomSheet { … } }`
  block and the `AddFoodPanel`, `AddModeTabs`, `AiLoggingForm`, `ManualFoodForm` wiring **for the AI mode**, and
  the standalone "Scan nutrition label" `OutlinedButton` + `NutritionLabelScanPanel` usage. Keep
  `BarcodeFoodForm`, `QuickCalorieForm`, `SavedFoodPicker`, `ManualFoodForm` composables — they're reused by
  Quick/Create/Barcode entry points (Quick track and Create route into them via `selectAddMode`). Remove the
  now-unused `FoodAddMode.Ai`, `AiLoggingForm`, and `NutritionLabelScanPanel`. Delete dead handlers
  (`onAiTextChanged`, etc.) and the `FoodSheetMode.NutritionLabelScan` branch.

- [ ] **Step 3: Build gate** — `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain` → BUILD SUCCESSFUL.
  Fix any references to removed symbols (the compiler lists them).

- [ ] **Step 4: Commit** — `feat(food): route add flow to full-screen AddFoodScreen; cut AI + label placeholder`.

---

## Task 4: Verify & screenshots

- [ ] **Step 1** — Build gate green.
- [ ] **Step 2** — Install; tap a meal "+" / the FAB → the full-screen add view opens. Confirm: search +
  barcode icon, daily-intake card, Recents default (same-as-yesterday / last tracked / all recents),
  Favorites tab, ⋮ → Quick track + Adjust goals. Search filters results; tapping a row logs it. Barcode icon
  opens the scanner. AI and the standalone label button are gone.
- [ ] **Step 3** — Screenshots: add screen Recents default, search results, ⋮ menu open.

## Definition of Done

- Add flow is a full-screen search-first view; the bottom-sheet + AI forms + standalone label button are gone.
- Recents (same-as-yesterday / last tracked / all recents) + Favorites tabs work; search and barcode work;
  Quick track + Adjust goals live in the ⋮ menu.
- `testDebugUnitTest lintDebug assembleDebug` green; recents repo test passes; screenshots captured.
- Create tab is a stub routing to manual create (full Create UI is Slice B; label OCR is Slice C).
