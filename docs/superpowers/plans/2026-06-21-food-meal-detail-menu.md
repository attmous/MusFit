# Food Meal Detail Menu Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a meal-level Food menu so tapping Breakfast, Lunch, Dinner, or Snacks opens a focused meal detail screen with meal macros, logged items, and add-more actions.

**Architecture:** Reuse the existing Food diary state. Add a selected meal id to `FoodUiState`, expose ViewModel actions for opening/closing meal detail and adding food to that selected meal, and render a full-screen Compose detail view before falling back to the main Food diary view.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Hilt ViewModel, kotlinx.coroutines test, existing repository/Room flows.

---

### Task 1: ViewModel State

**Files:**
- Modify: `app/src/test/java/com/musfit/ui/food/FoodViewModelTest.kt`
- Modify: `app/src/main/java/com/musfit/ui/food/FoodViewModel.kt`

- [ ] Write failing tests proving `openMealDetail("breakfast")` selects the Breakfast section, exposes meal calories and macros, and `closeMealDetail()` clears selection.
- [ ] Run `.\gradlew.bat testDebugUnitTest --tests com.musfit.ui.food.FoodViewModelTest` and verify the tests fail because the API/state does not exist.
- [ ] Add `selectedMealDetailId` to `FoodUiState`.
- [ ] Add macro totals to `FoodMealSectionUiState`: `proteinGrams`, `carbsGrams`, and `fatGrams`.
- [ ] Add `openMealDetail(mealType: String)` and `closeMealDetail()` to `FoodViewModel`.
- [ ] Run the focused ViewModel tests and verify they pass.

### Task 2: Meal Detail Add Flow

**Files:**
- Modify: `app/src/test/java/com/musfit/ui/food/FoodViewModelTest.kt`
- Modify: `app/src/main/java/com/musfit/ui/food/FoodViewModel.kt`

- [ ] Write a failing test proving `openAddFoodFromMealDetail()` opens the existing add panel for the selected meal.
- [ ] Run the focused test and verify it fails because the method does not exist.
- [ ] Add `openAddFoodFromMealDetail()` that delegates to `openAddFood(selectedMealDetailId)` when a meal is selected.
- [ ] Run the focused ViewModel tests and verify they pass.

### Task 3: Compose Meal Detail UI

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`

- [ ] Render `MealDetailScreen` when `state.selectedMealDetailId` matches a meal section.
- [ ] Make the non-button body of each meal card tappable and call `openMealDetail(meal.id)`.
- [ ] Show meal title, back button, calories, carbs/protein/fat totals, logged item rows, and an Add food button/search-style control that calls `openAddFoodFromMealDetail()`.
- [ ] Keep item rows tappable and route to `openDiaryEntryEditor(entry.id)`.
- [ ] Keep the existing `+` buttons as direct add shortcuts.

### Task 4: Verification And Push

**Files:**
- Build outputs only

- [ ] Run `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug`.
- [ ] Install the APK on the connected Pixel 8 Pro.
- [ ] Launch the app, open Food, tap Breakfast/Lunch, verify the meal detail screen and add sheet.
- [ ] Commit and push to `origin/master`.
