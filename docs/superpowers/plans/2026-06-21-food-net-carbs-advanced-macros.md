# Food Net Carbs And Advanced Macros Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add persistent net-carbs display mode plus fiber, sugar, saturated fat, and sodium progress against Food goals.

**Architecture:** Extend the existing `FoodGoal` model and Room `food_goals` table with a net-carbs boolean. Build day and meal nutrient progress from existing `FoodDiary.detailTotals` and `FoodDiaryMeal.detailTotals`, then render compact Compose progress cards in the main diary and meal detail.

**Tech Stack:** Android Kotlin, Jetpack Compose Material 3, Hilt ViewModels, Room, Kotlin Flow, Robolectric/JUnit.

## Global Constraints

- Android-only; keep Food local-first and scoped to `com.musfit`.
- Use existing Food ViewModel, repository, Room, and Compose patterns.
- Write failing ViewModel/repository tests before production code.
- Before Gradle commands on Windows run: `. .\.superpowers\sdd\android-env.ps1`.

---

### Task 1: Net Carbs State And Persistence Tests

**Files:**
- Modify: `app/src/test/java/com/musfit/ui/food/FoodViewModelTest.kt`
- Modify: `app/src/test/java/com/musfit/data/repository/LocalFoodRepositoryTest.kt`

**Interfaces:**
- Consumes: `FoodGoal`, `FoodUiState.macroProgress`, `FoodViewModel.saveFoodGoal()`
- Produces: expected API `FoodGoal.useNetCarbs`, `FoodUiState.useNetCarbs`, `FoodUiState.goalUseNetCarbsInput`

- [ ] Add a ViewModel test that enables net carbs, saves goals, and expects the carbs macro to be labeled `Net carbs` with current grams equal to total carbs minus fiber.
- [ ] Add a repository test that persists and observes `FoodGoal(useNetCarbs = true)`.
- [ ] Run focused tests and confirm they fail because the net-carbs API does not exist yet.

### Task 2: Advanced Nutrient Progress Tests

**Files:**
- Modify: `app/src/test/java/com/musfit/ui/food/FoodViewModelTest.kt`

**Interfaces:**
- Consumes: `FoodDiary.detailTotals`, `FoodGoal.fiberGrams`, `FoodGoal.sugarGrams`, `FoodGoal.saturatedFatGrams`, `FoodGoal.sodiumMilligrams`
- Produces: expected API `FoodNutrientProgressUiState` and `FoodMealSectionUiState.advancedNutritionProgress`

- [ ] Add a ViewModel test that expects day progress rows for fiber target and sugar/saturated fat/sodium limits.
- [ ] Add a ViewModel test that expects meal detail progress rows use the same goals.
- [ ] Run focused tests and confirm they fail for missing state.

### Task 3: Room And Repository Implementation

**Files:**
- Modify: `app/src/main/java/com/musfit/data/local/entity/FoodEntities.kt`
- Modify: `app/src/main/java/com/musfit/data/local/MusFitDatabase.kt`
- Modify: `app/src/main/java/com/musfit/core/di/DatabaseModule.kt`
- Modify: `app/src/main/java/com/musfit/data/repository/FoodRepository.kt`
- Add: `app/schemas/com.musfit.data.local.MusFitDatabase/7.json`

**Interfaces:**
- Consumes: existing migration chain `MIGRATION_1_2` through `MIGRATION_5_6`
- Produces: `MIGRATION_6_7` adding `food_goals.useNetCarbs INTEGER NOT NULL DEFAULT 0`

- [ ] Add `useNetCarbs: Boolean = false` to `FoodGoal`.
- [ ] Add `@ColumnInfo(defaultValue = "0") val useNetCarbs: Boolean = false` to `FoodGoalEntity`.
- [ ] Map the field in `toEntity()` and `toFoodGoal()`.
- [ ] Bump `MusFitDatabase` to version 7 and register `MIGRATION_6_7`.
- [ ] Generate/verify schema 7 during the full Gradle gate.

### Task 4: ViewModel And Compose Implementation

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/food/FoodViewModel.kt`
- Modify: `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`

**Interfaces:**
- Consumes: `FoodDiary.detailTotals`, `FoodGoal.useNetCarbs`
- Produces: `FoodNutrientProgressUiState`, `FoodUiState.advancedNutritionProgress`, meal-level progress, and goal editor net-carbs switch

- [ ] Add `useNetCarbs` and `goalUseNetCarbsInput` to `FoodUiState`.
- [ ] Add `onGoalUseNetCarbsChanged(Boolean)` and save it in `saveFoodGoal()`.
- [ ] Update macro progress calculation to label carbs as `Net carbs` and subtract fiber when enabled.
- [ ] Add advanced progress rows for fiber as target and sugar/saturated fat/sodium as limits.
- [ ] Render advanced progress below macro cards and inside meal detail; add a switch to the goal editor.

### Task 5: Verification, Commit, Push

**Files:**
- Verify all modified files with `git diff`

**Interfaces:**
- Consumes: full Food and app verification commands
- Produces: pushed commit on `origin/master`

- [ ] Run focused Food tests.
- [ ] Run `. .\.superpowers\sdd\android-env.ps1` then `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain`.
- [ ] Commit as `feat: add net carbs food goals`.
- [ ] Push to `origin/master`.
- [ ] Check `adb devices`; install and launch only if a device is connected.
