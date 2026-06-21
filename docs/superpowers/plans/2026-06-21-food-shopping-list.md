# Food Shopping List Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Generate a Food shopping list from planned meals and recipes, with date-range selection, category grouping, check-off state, and manual additions.

**Architecture:** Add a persisted `shopping_list_items` Room table. Generated rows use stable source keys so regenerating from planned diary entries updates quantities without losing check state. Recipe-backed planned entries expand into their ingredient foods when the recipe can be matched by name; other planned foods are grouped directly by saved food/category.

**Tech Stack:** Android Kotlin, Jetpack Compose Material 3, Hilt ViewModels, Room, Kotlin Flow/coroutines, Robolectric/JUnit.

## Global Constraints

- Keep the work Android-only and local-first.
- Generate only from planned Food entries in the selected date range.
- Group shopping list rows by category, with a fallback `Other`.
- Preserve manual rows and check-off state when regenerating.
- Before Gradle commands on Windows run: `. .\.superpowers\sdd\android-env.ps1`.

---

### Task 1: Failing Tests

**Files:**
- Modify: `app/src/test/java/com/musfit/data/repository/LocalFoodRepositoryTest.kt`
- Modify: `app/src/test/java/com/musfit/ui/food/FoodViewModelTest.kt`

**Interfaces:**
- Produces expected data models: `ShoppingListItem`, `ShoppingListGroup`, `ManualShoppingListItemInput`.
- Produces expected repository APIs: `observeShoppingList()`, `generateShoppingList(startDate, endDate)`, `addManualShoppingListItem(input)`, `toggleShoppingListItem(itemId, isChecked)`.
- Produces expected ViewModel actions: `openShoppingList`, `generateShoppingList`, `addManualShoppingListItem`, `toggleShoppingListItem`, date-range/manual input handlers.

- [x] Add a repository test that planned foods in a date range generate grouped shopping list rows and toggled check state survives regeneration.
- [x] Add a repository test that planned recipe entries expand to ingredient foods and manual items are preserved.
- [x] Add a ViewModel test that opens the shopping list, changes the date range, generates, adds a manual item, and toggles an item checked.
- [x] Run focused tests and confirm they fail for missing shopping list APIs and state.

### Task 2: Repository And Room Storage

**Files:**
- Modify: `app/src/main/java/com/musfit/data/local/entity/FoodEntities.kt`
- Modify: `app/src/main/java/com/musfit/data/local/dao/FoodDao.kt`
- Modify: `app/src/main/java/com/musfit/data/local/MusFitDatabase.kt`
- Modify: `app/src/main/java/com/musfit/core/di/DatabaseModule.kt`
- Modify: `app/src/main/java/com/musfit/data/repository/FoodRepository.kt`
- Add schema: `app/schemas/com.musfit.data.local.MusFitDatabase/11.json`

**Interfaces:**
- Consumes: planned `FoodDiaryEntryRow`, recipe ingredient rows, saved food categories.
- Produces: persisted shopping list groups ordered by checked state, category, and name.

- [x] Add `ShoppingListItemEntity`, Room version 11, and migration `10 -> 11`.
- [x] Add DAO queries for observing, upserting, finding by source key, toggling, deleting generated rows, and inserting manual rows.
- [x] Add repository models and implement `observeShoppingList`.
- [x] Implement `generateShoppingList(startDate, endDate)` from planned diary rows, preserving checked/manual rows.
- [x] Implement manual add and toggle methods.

### Task 3: ViewModel And Compose UX

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/food/FoodViewModel.kt`
- Modify: `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`

**Interfaces:**
- Consumes: repository shopping list models and methods.
- Produces: shopping sheet with date range fields, generated/manual controls, grouped rows, and check-off toggles.

- [x] Add `FoodSheetMode.ShoppingList`, shopping state fields, item/group UI models, and repository collection.
- [x] Add date range input handlers with ISO `yyyy-MM-dd` parsing.
- [x] Add generate/manual-add/toggle ViewModel actions with concise validation messages.
- [x] Add a `Shopping` header button and `ShoppingListPanel`.
- [x] Render category groups, checked state, quantity labels, manual badge, and empty state.

### Task 4: Verification, Commit, Push

**Files:**
- Verify all modified files with `git diff`.

**Interfaces:**
- Consumes: focused Food tests and full app gate.
- Produces: pushed commit on `origin/master`.

- [x] Run focused Food ViewModel and repository tests.
- [x] Run `. .\.superpowers\sdd\android-env.ps1` then `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain`.
- [ ] Commit as `feat: add food shopping list`.
- [ ] Push to `origin/master`.
- [ ] Check `adb devices`; install only if a device is connected.
