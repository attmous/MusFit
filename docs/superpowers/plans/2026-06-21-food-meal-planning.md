# Food Meal Planning Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add simple Food meal planning so users can plan future days, copy meals or days, view a 7-day plan, and convert planned food into logged food.

**Architecture:** Extend existing `meals` and `meal_items` storage with a lightweight `status` on `meal_items` (`logged` or `planned`). Keep logged totals backward-compatible for diary progress, add planned totals and 7-day plan summaries from the same joined rows, and expose planning actions through the existing Food ViewModel and Compose screens.

**Tech Stack:** Android Kotlin, Jetpack Compose Material 3, Hilt ViewModels, Room, Kotlin Flow/coroutines, Robolectric/JUnit.

## Global Constraints

- Keep the work Android-only and local-first.
- Keep `FoodDiary.totals` and existing calorie progress based on logged entries only.
- Planned entries must be visible in the diary but visually distinct from logged entries.
- Planned entries must be convertible to logged entries without recreating foods.
- Before Gradle commands on Windows run: `. .\.superpowers\sdd\android-env.ps1`.

---

### Task 1: Failing Planning Tests

**Files:**
- Modify: `app/src/test/java/com/musfit/data/repository/LocalFoodRepositoryTest.kt`
- Modify: `app/src/test/java/com/musfit/ui/food/FoodViewModelTest.kt`

**Interfaces:**
- Produces expected enum `FoodDiaryEntryStatus` with `Logged` and `Planned`.
- Produces expected repository APIs: `planSavedFood`, `markDiaryEntryLogged`, `copyMeal(..., status)`, `copyDay`.
- Produces expected ViewModel state/actions: `isPlanningMode`, `weeklyPlan`, `togglePlanningMode`, `markDiaryEntryLogged`, `copySelectedDayToTomorrow`, and planned-row display state.

- [x] Add a repository test that plans a saved food for tomorrow, confirms logged totals stay zero, confirms planned totals include the item, then marks the item logged.
- [x] Add a repository test that copies a whole day as planned entries to a future date.
- [x] Add a ViewModel test that toggles planning mode, plans a saved food on a future date, exposes a planned item badge, and marks it logged.
- [x] Run focused tests and confirm they fail for missing planning APIs and state.

### Task 2: Repository And Room Planning Model

**Files:**
- Modify: `app/src/main/java/com/musfit/data/local/entity/FoodEntities.kt`
- Modify: `app/src/main/java/com/musfit/data/local/dao/FoodDao.kt`
- Modify: `app/src/main/java/com/musfit/data/local/MusFitDatabase.kt`
- Modify: `app/src/main/java/com/musfit/core/di/DatabaseModule.kt`
- Modify: `app/src/main/java/com/musfit/data/repository/FoodRepository.kt`

**Interfaces:**
- Consumes: existing `MealEntity`, `MealItemEntity`, `FoodDiaryEntryRow`, `insertMealItem`, and copy helpers.
- Produces: planned/logged status in diary rows plus `FoodPlanDay` summaries for a 7-day window.

- [x] Add `status` to `MealItemEntity` with default `"logged"`, bump Room to version 10, and add migration `9 -> 10`.
- [x] Add `status` to joined DAO rows and repository diary mapping.
- [x] Add `FoodDiaryEntryStatus`, planned totals on `FoodDiary` and `FoodDiaryMeal`, and `FoodPlanDay`.
- [x] Add repository methods for planning saved foods, marking entries logged, copying meals with a target status, copying days, and observing 7-day plans.
- [x] Keep existing log/copy behavior defaulting to logged status.

### Task 3: ViewModel And Compose Planning UX

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/food/FoodViewModel.kt`
- Modify: `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`

**Interfaces:**
- Consumes: repository planning APIs and `FoodPlanDay`.
- Produces: planning mode toggle, compact 7-day plan strip, planned badges, and mark-logged action.

- [x] Add `FoodPlanDayUiState`, `isPlanningMode`, and planned totals to existing meal/entry UI state.
- [x] Collect a 7-day plan window from the selected date and update it when date changes.
- [x] Route saved-food actions through planned or logged repository calls based on planning mode.
- [x] Add header controls for planning mode and copying the selected day to tomorrow.
- [x] Render planned badges on diary rows and a `Mark logged` action in the diary entry editor for planned items.

### Task 4: Verification, Commit, Push

**Files:**
- Verify all modified files with `git diff`.

**Interfaces:**
- Consumes: focused Food tests and full app gate.
- Produces: pushed commit on `origin/master`.

- [x] Run focused Food ViewModel and repository tests.
- [x] Run `. .\.superpowers\sdd\android-env.ps1` then `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain`.
- [ ] Commit as `feat: add food meal planning`.
- [ ] Push to `origin/master`.
- [ ] Check `adb devices`; install only if a device is connected.
