# Food Meal Templates V2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make meal templates editable reusable meals, not immutable snapshots.

**Architecture:** Reuse the existing `meal_templates` and `meal_template_items` Room tables. Add repository input models for template item updates, replace a template's item rows inside one transaction, and keep existing duplicate/favorite/log behavior. Extend the existing template bottom sheet with item quantity editing, add-item food chips, and remove actions.

**Tech Stack:** Android Kotlin, Jetpack Compose Material 3, Hilt ViewModels, Room, Kotlin Flow/coroutines, Robolectric/JUnit.

## Global Constraints

- Keep the work Android-only and local-first.
- Follow existing Food repository, ViewModel, and Compose bottom-sheet patterns.
- Write failing repository and ViewModel tests before production code.
- Before Gradle commands on Windows run: `. .\.superpowers\sdd\android-env.ps1`.

---

### Task 1: Failing Tests

**Files:**
- Modify: `app/src/test/java/com/musfit/data/repository/LocalFoodRepositoryTest.kt`
- Modify: `app/src/test/java/com/musfit/ui/food/FoodViewModelTest.kt`

**Interfaces:**
- Produces expected repository inputs: `MealTemplateItemInput(foodId: String, quantityGrams: Double)` and `MealTemplateUpdateInput(templateId: String, name: String, mealType: String, items: List<MealTemplateItemInput>)`
- Produces expected repository method: `updateMealTemplate(input: MealTemplateUpdateInput)`
- Produces expected ViewModel template item draft state and item edit handlers

- [x] Add a repository test that updates a template's name, meal type, item set, and quantities, then logs the edited template as normal diary items.
- [x] Add a ViewModel test that opens a template, edits item quantity, removes one item, adds a saved food item, and saves through `updateMealTemplate`.
- [x] Run focused tests and confirm they fail for the missing update API and handlers.

### Task 2: Repository Template Editing

**Files:**
- Modify: `app/src/main/java/com/musfit/data/repository/FoodRepository.kt`

**Interfaces:**
- Consumes: existing `MealTemplateEntity`, `MealTemplateItemEntity`, and `FoodDao` methods
- Produces: `updateMealTemplate(input: MealTemplateUpdateInput)`

- [x] Add `MealTemplateItemInput` and `MealTemplateUpdateInput` data classes.
- [x] Add `FoodRepository.updateMealTemplate(input: MealTemplateUpdateInput)`.
- [x] Implement `LocalFoodRepository.updateMealTemplate` as one transaction: validate template, validate items, update metadata, delete old item rows, insert new sorted rows.
- [x] Keep existing `renameMealTemplate`, `duplicateMealTemplate`, `toggleFavoriteMealTemplate`, and `logMealTemplate` behavior compatible.

### Task 3: ViewModel And UI

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/food/FoodViewModel.kt`
- Modify: `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`

**Interfaces:**
- Produces: template editor draft items with `foodId`, `foodName`, and editable grams

- [x] Add `MealTemplateItemDraftUiState` and include template items on `MealTemplateUiState`.
- [x] Open template editor with current template items loaded into draft state.
- [x] Add handlers for template item quantity changes, removing items, choosing a saved food, adding an item, and saving all changes.
- [x] Update `saveMealTemplateEdits` to call `updateMealTemplate` with draft items.
- [x] Render editable item rows and add-item controls inside `MealTemplatesPanel`.

### Task 4: Verification, Commit, Push

**Files:**
- Verify all modified files with `git diff`

**Interfaces:**
- Consumes: focused Food tests and full app gate
- Produces: pushed commit on `origin/master`

- [x] Run focused Food ViewModel and repository tests.
- [x] Run `. .\.superpowers\sdd\android-env.ps1` then `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain`.
- [ ] Commit as `feat: upgrade food meal templates`.
- [ ] Push to `origin/master`.
- [ ] Check `adb devices`; install only if a device is connected.
