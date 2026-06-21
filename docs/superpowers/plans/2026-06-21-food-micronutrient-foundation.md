# Food Micronutrient Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add foundation support for sodium, potassium, calcium, iron, vitamin D, vitamin C, and magnesium without making the Food diary feel crowded.

**Architecture:** Extend the existing `NutritionDetails` object, Room `foods` table, DAO projection rows, and repository mapping. Reuse the saved-food nutrition field component for editor and label-scan input, then expose compact day and meal micronutrient cards from existing diary detail totals.

**Tech Stack:** Android Kotlin, Jetpack Compose Material 3, Hilt ViewModels, Room, Kotlin Flow, Open Food Facts, Robolectric/JUnit.

## Global Constraints

- Keep the work Android-only and local-first.
- Preserve existing sodium behavior while adding the remaining six micronutrients.
- Write failing ViewModel/repository tests before production code.
- Before Gradle commands on Windows run: `. .\.superpowers\sdd\android-env.ps1`.

---

### Task 1: Failing Tests

**Files:**
- Modify: `app/src/test/java/com/musfit/ui/food/FoodViewModelTest.kt`
- Modify: `app/src/test/java/com/musfit/data/repository/LocalFoodRepositoryTest.kt`
- Modify: `app/src/test/java/com/musfit/data/remote/food/OpenFoodFactsProductProviderTest.kt`

**Interfaces:**
- Produces expected fields on `NutritionDetails`: `potassiumMilligrams`, `calciumMilligrams`, `ironMilligrams`, `vitaminDMicrograms`, `vitaminCMilligrams`, `magnesiumMilligrams`
- Produces expected ViewModel state: saved-food micronutrient inputs and `micronutrients`

- [x] Add a repository test that `upsertSavedFood` round-trips the six new micronutrients and diary totals aggregate them.
- [x] Add a ViewModel test that saved-food editor inputs save micronutrient values.
- [x] Add a ViewModel test that day and meal detail state expose compact micronutrient rows.
- [x] Add an Open Food Facts parser test for micronutrient fields.
- [x] Run focused tests and confirm they fail for the missing API.

### Task 2: Data And Persistence

**Files:**
- Modify: `app/src/main/java/com/musfit/data/repository/FoodRepository.kt`
- Modify: `app/src/main/java/com/musfit/data/local/entity/FoodEntities.kt`
- Modify: `app/src/main/java/com/musfit/data/local/dao/FoodDao.kt`
- Modify: `app/src/main/java/com/musfit/data/local/MusFitDatabase.kt`
- Modify: `app/src/main/java/com/musfit/core/di/DatabaseModule.kt`
- Add: `app/schemas/com.musfit.data.local.MusFitDatabase/8.json`

**Interfaces:**
- Consumes: existing `NutritionDetails` and Room version 7
- Produces: Room version 8 with default-zero nutrient columns

- [x] Add six default-zero micronutrient fields to `NutritionDetails`.
- [x] Add matching `foods` columns and DAO row projections.
- [x] Update repository insert, update, duplicate, recipe, detail total, and entity conversion paths.
- [x] Add `MIGRATION_7_8` with default-zero `ALTER TABLE foods ADD COLUMN` statements.

### Task 3: ViewModel And UI

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/food/FoodViewModel.kt`
- Modify: `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`

**Interfaces:**
- Produces: `FoodMicronutrientUiState(label, value, unit)`, day `micronutrients`, and meal `micronutrients`

- [x] Add saved-food input fields and change handlers for potassium, calcium, iron, vitamin D, vitamin C, and magnesium.
- [x] Include those fields in new-food, edit-food, duplicate-food, online-food, label-scan, and detail UI state mapping.
- [x] Render micronutrients in a compact horizontal row for the diary and a compact two-column grid in meal detail.
- [x] Keep sodium in the existing advanced progress cards and also include it in the micronutrient row.

### Task 4: Open Food Facts

**Files:**
- Modify: `app/src/main/java/com/musfit/data/remote/food/OpenFoodFactsModels.kt`
- Modify: `app/src/main/java/com/musfit/data/remote/food/OpenFoodFactsProductProvider.kt`

**Interfaces:**
- Consumes: Open Food Facts nutriment fields `potassium_100g`, `calcium_100g`, `iron_100g`, `vitamin-d_100g`, `vitamin-c_100g`, `magnesium_100g`
- Produces: app units in milligrams except vitamin D in micrograms

- [x] Parse the new nutriments.
- [x] Convert gram-based Open Food Facts values to app units.
- [x] Preserve existing sodium conversion.

### Task 5: Verification, Commit, Push

**Files:**
- Verify all modified files with `git diff`

**Interfaces:**
- Consumes: focused Food tests and full app gate
- Produces: pushed commit on `origin/master`

- [x] Run focused Food and Open Food Facts tests.
- [x] Run `. .\.superpowers\sdd\android-env.ps1` then `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain`.
- [ ] Commit as `feat: add food micronutrient foundation`.
- [ ] Push to `origin/master`.
- [ ] Check `adb devices`; install only if a device is connected.
