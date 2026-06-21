# Food Water Tracking Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add local-first water tracking to the Food miniapp with quick amounts, custom amount logging, a daily goal, and persisted daily progress.

**Architecture:** Store water logs as dated Room rows and keep the daily water target on the existing Food goal model. The repository exposes a `FoodWaterSummary` Flow for the selected date, and `FoodViewModel` maps it into a compact summary card with quick buttons, custom entry, and goal editing.

**Tech Stack:** Android Kotlin, Jetpack Compose Material 3, Hilt ViewModels, Room, Kotlin Flow/coroutines, Robolectric/JUnit.

## Global Constraints

- Keep the work Android-only and local-first.
- Do not implement Health Connect hydration sync in this slice; structure names so Slice 19 can add it later.
- Water appears in the Food diary summary area, above the meal diary.
- Quick buttons log `250 ml` and `500 ml`; custom input logs any positive milliliter amount.
- Daily water goal defaults to `2000 ml` and persists.
- Before Gradle commands on Windows run: `. .\.superpowers\sdd\android-env.ps1`.

---

### Task 1: Failing Tests

**Files:**
- Modify: `app/src/test/java/com/musfit/ui/food/FoodViewModelTest.kt`
- Modify: `app/src/test/java/com/musfit/data/repository/LocalFoodRepositoryTest.kt`

**Interfaces:**
- Produces repository models: `FoodWaterSummary(date, consumedMilliliters, goalMilliliters)` and `WaterLogInput(date, amountMilliliters)`.
- Produces repository APIs: `observeWaterSummary(date)`, `logWater(input)`, and `updateWaterGoal(goalMilliliters)`.
- Produces ViewModel state/actions: `waterConsumedMilliliters`, `waterGoalMilliliters`, `waterProgress`, `waterCustomAmountInput`, `waterGoalInput`, `logQuickWater`, `logCustomWater`, `saveWaterGoal`.

- [x] Add a ViewModel test that observes water summary, logs `250 ml`, logs a custom amount, and updates the daily goal.
- [x] Add a repository test that logs multiple water entries, ignores another date in today totals, and persists the updated goal.
- [x] Run focused Food tests and confirm they fail because water models/APIs/state do not exist.

### Task 2: Repository And Room Storage

**Files:**
- Modify: `app/src/main/java/com/musfit/data/local/entity/FoodEntities.kt`
- Modify: `app/src/main/java/com/musfit/data/local/dao/FoodDao.kt`
- Modify: `app/src/main/java/com/musfit/data/local/MusFitDatabase.kt`
- Modify: `app/src/main/java/com/musfit/core/di/DatabaseModule.kt`
- Modify: `app/src/main/java/com/musfit/data/repository/FoodRepository.kt`
- Add schema: `app/schemas/com.musfit.data.local.MusFitDatabase/12.json`

**Interfaces:**
- Consumes: selected date and existing `FoodGoal`.
- Produces: persisted daily water totals and goal updates.

- [x] Add `WaterEntryEntity`, Room version `12`, and migration `11 -> 12`.
- [x] Add DAO methods for observing daily water total and inserting water entries.
- [x] Add `waterGoalMilliliters` to `FoodGoal` / `FoodGoalEntity` with default `2000`.
- [x] Implement `observeWaterSummary(date)`, `logWater(input)`, and `updateWaterGoal(goalMilliliters)`.

### Task 3: ViewModel And Compose UX

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/food/FoodViewModel.kt`
- Modify: `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`

**Interfaces:**
- Consumes: `FoodWaterSummary` and water repository methods.
- Produces: compact water tracker in the Food summary area.

- [x] Add water state fields and collect water summary for the selected date.
- [x] Add quick/custom water log actions with validation and concise messages.
- [x] Add water goal input/save action with validation and persisted goal update.
- [x] Render `WaterTrackerCard` above the meal diary with progress, `250 ml` / `500 ml` buttons, custom amount input, and goal input.

### Task 4: Verification, Commit, Push

**Files:**
- Verify all modified files with `git diff`.

**Interfaces:**
- Consumes: focused Food tests and full app gate.
- Produces: pushed commit on `origin/master`.

- [x] Run focused Food ViewModel and repository tests.
- [x] Run `. .\.superpowers\sdd\android-env.ps1` then `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain`.
- [x] Commit as `feat: add food water tracking`.
- [x] Push to `origin/master`.
- [x] Check `adb devices`; install only if a device is connected.
