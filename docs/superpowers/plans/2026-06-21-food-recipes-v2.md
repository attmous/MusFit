# Food Recipes V2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make recipes reusable real-tracking objects with ingredient serving units, cooked yield, recipe servings, per-serving nutrition, edit/delete/duplicate, and fractional logging.

**Architecture:** Extend the existing recipe tables instead of introducing a parallel recipe store. Keep recipe ingredients persisted as gram totals for nutrition math, but also save the selected unit label, unit grams, and unit quantity so edits preserve user intent. Add cooked yield and serving count to recipes, derive per-serving nutrition from total ingredient nutrition divided by servings, and continue logging recipes as temporary recipe-backed foods.

**Tech Stack:** Android Kotlin, Jetpack Compose Material 3, Hilt ViewModels, Room migrations/schema export, Kotlin Flow/coroutines, Robolectric/JUnit.

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
- Produces expected fields on `RecipeUpsertInput`, `RecipeIngredientInput`, `RecipeIngredient`, and `Recipe`: `servings`, `cookedYieldGrams`, `unitLabel`, `unitGrams`, `unitQuantity`
- Produces expected repository method: `duplicateRecipe(recipeId: String, name: String): String`

- [x] Add a repository test that a recipe with 4 servings, 720g cooked yield, and serving-unit ingredient inputs calculates per-serving nutrition and logs 0.5 servings correctly.
- [x] Add a repository test that `duplicateRecipe` copies ingredients, cooked yield, servings, category, serving name, and favorite state into a distinct recipe.
- [x] Add a ViewModel test that selecting an ingredient serving option stores unit metadata and computed grams in `RecipeUpsertInput`.
- [x] Add a ViewModel test that duplicate recipe calls the repository and surfaces a concise success message.
- [x] Run focused tests and confirm they fail for missing Recipes V2 API.

### Task 2: Room And Repository

**Files:**
- Modify: `app/src/main/java/com/musfit/data/local/entity/FoodEntities.kt`
- Modify: `app/src/main/java/com/musfit/data/local/dao/FoodDao.kt`
- Modify: `app/src/main/java/com/musfit/data/local/MusFitDatabase.kt`
- Modify: `app/src/main/java/com/musfit/core/di/DatabaseModule.kt`
- Modify: `app/src/main/java/com/musfit/data/repository/FoodRepository.kt`
- Add: `app/schemas/com.musfit.data.local.MusFitDatabase/9.json`

**Interfaces:**
- Consumes: existing version 8 recipe and ingredient rows
- Produces: Room version 9 with default-safe recipe serving/yield and ingredient unit columns

- [x] Add `servings` and `cookedYieldGrams` to `RecipeEntity`, `RecipeUpsertInput`, and `Recipe`.
- [x] Add `unitLabel`, `unitGrams`, and `unitQuantity` to `RecipeIngredientEntity`, `RecipeIngredientInput`, and `RecipeIngredient`.
- [x] Add `MIGRATION_8_9` with default values: recipe `servings = 1`, recipe `cookedYieldGrams = 0`, ingredient `unitLabel = 'g'`, `unitGrams = 1`, and `unitQuantity = quantityGrams` via safe SQL.
- [x] Update recipe row queries, mapping, validation, nutrition calculation, logging, delete, favorite, and duplicate paths.

### Task 3: ViewModel And UI

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/food/FoodViewModel.kt`
- Modify: `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`

**Interfaces:**
- Produces state fields for recipe serving count, cooked yield, ingredient serving choices, selected ingredient serving, and duplicate recipe name

- [x] Add recipe editor inputs for servings count and cooked yield grams, defaulting new recipes to `1` serving and `100`g yield.
- [x] Add ingredient serving option selection from the chosen saved food: `g`, default serving, and saved food serving options.
- [x] Save ingredient unit metadata while preserving gram totals for nutrition calculations.
- [x] Show compact per-serving calories/protein/carbs/fat and ingredient summaries in the editor and add-food recipe list.
- [x] Add duplicate action for saved recipes and wire it to `duplicateRecipe`.

### Task 4: Verification, Commit, Push

**Files:**
- Verify all modified files with `git diff`

**Interfaces:**
- Consumes: focused Food tests and full app gate
- Produces: pushed commit on `origin/master`

- [x] Run focused Food ViewModel and repository tests.
- [x] Run `. .\.superpowers\sdd\android-env.ps1` then `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain`.
- [ ] Commit as `feat: upgrade food recipes`.
- [ ] Push to `origin/master`.
- [ ] Check `adb devices`; install only if a device is connected.
