# Food Lifesum-Style Parity Loop Ledger

**Goal:** Complete practical Food parity slices one at a time while preserving MusFit's Android-only, local-first MVP constraints.

**Loop constraints:**
- Keep work scoped to Food unless a narrow Today/Profile/Health integration is required.
- Do not add accounts, cloud sync, analytics, subscriptions, social features, or cloud AI.
- Do not copy Lifesum assets, names, layouts, or proprietary scoring logic.
- Use existing single-`FoodViewModel`, Room, Flow, repository, and Compose patterns.
- Add or update focused tests before behavior changes whenever practical.

## Slice Queue

| Slice | Name | Status | Evidence / Notes |
| --- | --- | --- | --- |
| 0 | Parity Audit And Roadmap Ledger | Documented | This ledger created from `AGENTS.md`, `docs/architecture/food-system.md`, `docs/architecture/data-models.md`, and existing Food plans. |
| 1 | Food Quality Ratings | Verified in current loop | `FoodMealEntryUiState.rating`, per-food quality labels, deterministic explanations. |
| 2 | Meal And Day Rating Drill-Down | Verified in current loop | `FoodRatingFactorUiState`, day/meal factors for calories, protein, fiber, sodium, and diet-mode context. |
| 3 | Habit Tracker Foundation | Verified in current loop | `FoodHabitTrackerUiState` for fruit, vegetables, fish, and water. |
| 4 | Weekly Life Score-Style Summary | Verified in current loop | Original `Weekly MusFit score` with transparent nutrition, hydration, habits, and neutral training factor. |
| 5 | Guided Food Programs | Verified in current loop | Goal-sheet program catalog with targets, habits, planning tips, and direct apply for 7 local programs. |
| 6 | Recipe Discovery Library | Verified in current loop | Local recipe discovery catalog, saved-recipe reuse, filters, program relevance, and recipe-editor prefill. |
| 7 | Food Database Trust Layer | Verified in current loop | Local trust/source guidance, report-for-review state, and correction workflow through saved-food editor. |
| 8 | Barcode Comparison Scanner | Verified in current loop | Compare saved or Open Food Facts barcode products side by side with nutrient highlights. |
| 9 | Fasting Timer And Fasting Programs | Verified in current loop | Local fasting panel with 12:12, 14:10, 16:8, custom split, start time, and derived windows. |
| 10 | Food Progress And Statistics | Verified in current loop | 28-day local progress summary with weekly/monthly averages, adherence, hydration, habits, and trends. |
| 11 | Local-First AI/OCR Polish | Verified in current loop | Deterministic local text estimates, OCR advanced-nutrient parsing, confidence review state, and visible review callouts. |

## Current Slice Status

**Active slice:** All queued parity slices are implemented and final closeout verification has passed.

**Current implementation position:** Slices 1-11 are present as uncommitted current-worktree changes and are verified in this loop.

## Audit Summary

### Already Covered Or Enhanced

- Barcode lookup/scanner and Open Food Facts import exist.
- Nutrition-label scanner route and parser review flow exist, including local parsing for macros, fiber, sugar, saturated fat, sodium, and confidence labels.
- AI text logging now creates deterministic local estimates for common simple meal descriptions; AI voice/photo logging remain shells, and cloud AI remains out of scope.
- Food diary, meal sections, custom meals, planning, shopping list, water, Health Connect export, favorites, recipes v2, templates v2, and goals exist.
- Source labels and duplicate merge exist.
- Day and meal ratings existed before this loop; the current worktree adds per-food ratings, rating factors, habit trackers, the weekly MusFit score, and guided Food programs.

### Missing Or Still Needs Enhancement

- No remaining practical Food parity slices in this loop. AI voice/photo logging remain shells by design for the local-first MVP.

## Closeout Audit - 2026-06-28

| Area | Closeout result |
| --- | --- |
| Food quality ratings | Reachable in meal rows, meal detail, and More details; `FoodRatingUiState` is derived from current diary/meal entries; empty diary returns the neutral no-rating state; covered by `ratingDrillDownUsesDietModeAndPerFoodQuality`. |
| Meal and day rating drill-down | Reachable through the day rating card and meal/entry rating UI; factor state includes calories, protein, fiber, sodium, and diet-mode context; empty meals omit meal ratings; covered by the same rating drill-down test. |
| Habit trackers | Reachable under More details; fruit, vegetables, fish, and water derive from diary names plus water summary; empty diary shows missing food habits and water progress; covered by `habitTrackersReflectFruitVegetableFishAndWaterProgress`. |
| Weekly MusFit score | Reachable under More details; repository range summary combines diary, water, and goals for the trailing seven-day window; no tracked days returns the empty weekly score; covered by `weeklyMusFitScoreExplainsNutritionHydrationHabitsAndUnavailableTrainingSignal`, `weeklyMusFitScoreUsesTrailingSevenDayWindow`, and `weeklyFoodSummary_combinesLoggedNutritionAndWaterForSevenDays`. |
| Guided Food programs | Reachable in the goal editor sheet; program catalog is derived from local `FoodGoalMode` presets and persists through `FoodGoal`; covered by `foodProgramsExposeTargetsHabitsAndPlanningGuidance`, `applyingMediterraneanProgramPersistsProgramTargetsAndSelection`, and `foodGoal_roundTripsUserTargetsAndMode`. |
| Recipe discovery/library | Reachable in the recipe editor when creating a recipe; local catalog and saved recipes share `RecipeDiscoveryItemUiState`; empty saved recipes still show catalog ideas; covered by `recipeDiscoveryFiltersLocalCatalogSavedFavoritesAndPrograms` and `usingRecipeDiscoveryItemLogsSavedRecipeOrPrefillsCatalogRecipe`. |
| Food database trust/source layer | Reachable in the food database rows and detail sheet; trust state is local UI state over saved foods/source labels; empty database already shows empty state; covered by `savedFoodsExposeTrustGuidanceForImportsManualEntriesAndLabelScans` and `reportingSavedFoodMarksLocalReviewAndCorrectionOpensEditor`. |
| Barcode comparison scanner | Reachable from Food database -> Compare barcodes; compares saved products first, then Open Food Facts; empty/missing lookups show user messages; covered by two barcode comparison ViewModel tests. |
| Fasting timer/programs | Reachable from the Food header overflow menu; local state handles presets, start time, custom split validation, and default empty state; covered by `fastingProgramsExposePresetWindowsAndStartTimeSchedule` and `customFastingProgramAppliesValidatedLocalSplit`. |
| Food progress/statistics | Reachable under More details; repository range summary drives weekly/monthly stats and empty ranges show zero tracked days; covered by `foodProgressStatsSummarizeWeeklyMonthlyAdherenceAndTrends` and `foodProgressSummary_combinesLoggedNutritionAndWaterForTwentyEightDays`. |
| Local-first AI/OCR polish | Reachable from Add Food -> AI and scan-label flows; deterministic AI text drafts and OCR confidence/details are always user-reviewed before save/log; covered by `aiTextLoggingCreatesEditableDraftWithoutSavingUntilReviewed` and `scannedNutritionLabelParsesAdvancedNutrientsAndConfidence`. |

## Closeout Fixes - 2026-06-28

- Fixed the weekly MusFit score observer to use the selected date's trailing seven-day window (`selectedDate - 6 days` through `selectedDate`) instead of selected date plus six future days.
- Added `FoodViewModelTest.weeklyMusFitScoreUsesTrailingSevenDayWindow` to pin the repository request window.

## Files Touched Per Slice

### Slice 0: Parity Audit And Roadmap Ledger

- `docs/superpowers/plans/2026-06-27-food-lifesum-parity-loop.md`

### Slice 1: Food Quality Ratings

- `app/src/main/java/com/musfit/ui/food/FoodViewModel.kt`
- `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`
- `app/src/test/java/com/musfit/ui/food/FoodViewModelTest.kt`
- `docs/architecture/food-system.md`
- `docs/superpowers/plans/2026-06-26-food-parity-quality-habits.md`

### Slice 2: Meal And Day Rating Drill-Down

- `app/src/main/java/com/musfit/ui/food/FoodViewModel.kt`
- `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`
- `app/src/test/java/com/musfit/ui/food/FoodViewModelTest.kt`
- `docs/architecture/food-system.md`
- `docs/superpowers/plans/2026-06-26-food-parity-quality-habits.md`

### Slice 3: Habit Tracker Foundation

- `app/src/main/java/com/musfit/ui/food/FoodViewModel.kt`
- `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`
- `app/src/test/java/com/musfit/ui/food/FoodViewModelTest.kt`
- `docs/architecture/food-system.md`
- `docs/superpowers/plans/2026-06-26-food-parity-quality-habits.md`

### Slice 4: Weekly Life Score-Style Summary

- `app/src/main/java/com/musfit/data/local/dao/FoodDao.kt`
- `app/src/main/java/com/musfit/data/repository/FoodRepository.kt`
- `app/src/main/java/com/musfit/ui/food/FoodViewModel.kt`
- `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`
- `app/src/test/java/com/musfit/data/repository/LocalFoodRepositoryTest.kt`
- `app/src/test/java/com/musfit/ui/food/FoodViewModelTest.kt`
- `docs/architecture/food-system.md`
- `docs/superpowers/plans/2026-06-27-food-lifesum-parity-loop.md`

### Slice 5: Guided Food Programs

- `app/src/main/java/com/musfit/data/repository/FoodRepository.kt`
- `app/src/main/java/com/musfit/ui/food/FoodViewModel.kt`
- `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`
- `app/src/main/java/com/musfit/ui/food/FoodModalSheets.kt`
- `app/src/test/java/com/musfit/data/repository/LocalFoodRepositoryTest.kt`
- `app/src/test/java/com/musfit/ui/food/FoodViewModelTest.kt`
- `docs/architecture/data-models.md`
- `docs/architecture/food-system.md`
- `docs/superpowers/plans/2026-06-27-food-lifesum-parity-loop.md`

### Slice 6: Recipe Discovery Library

- `app/src/main/java/com/musfit/ui/food/FoodViewModel.kt`
- `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`
- `app/src/main/java/com/musfit/ui/food/FoodModalSheets.kt`
- `app/src/test/java/com/musfit/ui/food/FoodViewModelTest.kt`
- `docs/architecture/food-system.md`
- `docs/superpowers/plans/2026-06-27-food-lifesum-parity-loop.md`

### Slice 7: Food Database Trust Layer

- `app/src/main/java/com/musfit/ui/food/FoodViewModel.kt`
- `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`
- `app/src/main/java/com/musfit/ui/food/FoodModalSheets.kt`
- `app/src/test/java/com/musfit/ui/food/FoodViewModelTest.kt`
- `docs/architecture/food-system.md`
- `docs/superpowers/plans/2026-06-27-food-lifesum-parity-loop.md`

### Slice 8: Barcode Comparison Scanner

- `app/src/main/java/com/musfit/ui/food/FoodViewModel.kt`
- `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`
- `app/src/main/java/com/musfit/ui/food/FoodModalSheets.kt`
- `app/src/test/java/com/musfit/ui/food/FoodViewModelTest.kt`
- `docs/architecture/food-system.md`
- `docs/superpowers/plans/2026-06-27-food-lifesum-parity-loop.md`

### Slice 9: Fasting Timer And Fasting Programs

- `app/src/main/java/com/musfit/ui/food/FoodViewModel.kt`
- `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`
- `app/src/main/java/com/musfit/ui/food/FoodModalSheets.kt`
- `app/src/test/java/com/musfit/ui/food/FoodViewModelTest.kt`
- `docs/architecture/food-system.md`
- `docs/superpowers/plans/2026-06-27-food-lifesum-parity-loop.md`

### Slice 10: Food Progress And Statistics

- `app/src/main/java/com/musfit/data/repository/FoodRepository.kt`
- `app/src/main/java/com/musfit/ui/food/FoodViewModel.kt`
- `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`
- `app/src/test/java/com/musfit/data/repository/LocalFoodRepositoryTest.kt`
- `app/src/test/java/com/musfit/ui/food/FoodViewModelTest.kt`
- `docs/architecture/data-models.md`
- `docs/architecture/food-system.md`
- `docs/superpowers/plans/2026-06-27-food-lifesum-parity-loop.md`

### Slice 11: Local-First AI/OCR Polish

- `app/src/main/java/com/musfit/domain/food/NutritionLabelParser.kt`
- `app/src/main/java/com/musfit/ui/food/FoodViewModel.kt`
- `app/src/main/java/com/musfit/ui/food/FoodAddPanelUi.kt`
- `app/src/test/java/com/musfit/ui/food/FoodViewModelTest.kt`
- `docs/architecture/food-system.md`
- `docs/superpowers/plans/2026-06-27-food-lifesum-parity-loop.md`

## Tests Added Or Changed Per Slice

### Slice 0

- No tests; documentation/audit slice.

### Slice 1

- `FoodViewModelTest.ratingDrillDownUsesDietModeAndPerFoodQuality`

### Slice 2

- `FoodViewModelTest.ratingDrillDownUsesDietModeAndPerFoodQuality`

### Slice 3

- `FoodViewModelTest.habitTrackersReflectFruitVegetableFishAndWaterProgress`

### Slice 4

- `LocalFoodRepositoryTest.weeklyFoodSummary_combinesLoggedNutritionAndWaterForSevenDays`
- `FoodViewModelTest.weeklyMusFitScoreExplainsNutritionHydrationHabitsAndUnavailableTrainingSignal`
- `FoodViewModelTest.weeklyMusFitScoreUsesTrailingSevenDayWindow`

### Slice 5

- `FoodViewModelTest.foodProgramsExposeTargetsHabitsAndPlanningGuidance`
- `FoodViewModelTest.applyingMediterraneanProgramPersistsProgramTargetsAndSelection`
- Changed `LocalFoodRepositoryTest.foodGoal_roundTripsUserTargetsAndMode` to round-trip `FoodGoalMode.MediterraneanStyle`.

### Slice 6

- `FoodViewModelTest.recipeDiscoveryFiltersLocalCatalogSavedFavoritesAndPrograms`
- `FoodViewModelTest.usingRecipeDiscoveryItemLogsSavedRecipeOrPrefillsCatalogRecipe`

### Slice 7

- `FoodViewModelTest.savedFoodsExposeTrustGuidanceForImportsManualEntriesAndLabelScans`
- `FoodViewModelTest.reportingSavedFoodMarksLocalReviewAndCorrectionOpensEditor`

### Slice 8

- `FoodViewModelTest.barcodeComparisonUsesSavedFoodAndRemoteLookupWithMacroHighlights`
- `FoodViewModelTest.barcodeComparisonCanCompareTwoSavedFoodsWithoutRemoteLookup`

### Slice 9

- `FoodViewModelTest.fastingProgramsExposePresetWindowsAndStartTimeSchedule`
- `FoodViewModelTest.customFastingProgramAppliesValidatedLocalSplit`

### Slice 10

- `LocalFoodRepositoryTest.foodProgressSummary_combinesLoggedNutritionAndWaterForTwentyEightDays`
- `FoodViewModelTest.foodProgressStatsSummarizeWeeklyMonthlyAdherenceAndTrends`

### Slice 11

- Changed `FoodViewModelTest.aiTextLoggingCreatesEditableDraftWithoutSavingUntilReviewed` to pin deterministic local text estimates.
- `FoodViewModelTest.scannedNutritionLabelParsesAdvancedNutrientsAndConfidence`

## Verification Results Per Slice

### Slice 0

- Documented: ledger write only; no Gradle command required.

### Slices 1-3

- Prior pass verified:
  - `testDebugUnitTest --tests "com.musfit.ui.food.FoodViewModelTest"`: passed.
  - `testDebugUnitTest --tests "com.musfit.data.repository.LocalFoodRepositoryTest"`: passed.
  - `testDebugUnitTest lintDebug assembleDebug`: passed.
- Current-loop verification:
  - 2026-06-27 `testDebugUnitTest --tests "com.musfit.ui.food.FoodViewModelTest"`: passed.
  - 2026-06-27 `testDebugUnitTest --tests "com.musfit.data.repository.LocalFoodRepositoryTest"`: passed.

### Slice 4

- 2026-06-27 `testDebugUnitTest --tests "com.musfit.ui.food.FoodViewModelTest"`: passed.
- 2026-06-27 `testDebugUnitTest --tests "com.musfit.data.repository.LocalFoodRepositoryTest"`: passed.

### Slice 5

- 2026-06-27 red run: `testDebugUnitTest --tests "com.musfit.ui.food.FoodViewModelTest"` failed on missing program state/action and new enum mode, as expected.
- 2026-06-27 `testDebugUnitTest --tests "com.musfit.ui.food.FoodViewModelTest"`: passed.
- 2026-06-27 `testDebugUnitTest --tests "com.musfit.data.repository.LocalFoodRepositoryTest"`: passed.

### Slice 6

- 2026-06-27 red run: `testDebugUnitTest --tests "com.musfit.ui.food.FoodViewModelTest"` failed on missing recipe discovery state/actions, as expected.
- 2026-06-27 `testDebugUnitTest --tests "com.musfit.ui.food.FoodViewModelTest"`: passed.
- 2026-06-27 `testDebugUnitTest --tests "com.musfit.data.repository.LocalFoodRepositoryTest"`: passed.

### Slice 7

- 2026-06-27 red run: `testDebugUnitTest --tests "com.musfit.ui.food.FoodViewModelTest"` failed on missing trust state/actions, as expected.
- 2026-06-27 `testDebugUnitTest --tests "com.musfit.ui.food.FoodViewModelTest"`: passed.
- 2026-06-27 `testDebugUnitTest --tests "com.musfit.data.repository.LocalFoodRepositoryTest"`: passed.

### Slice 8

- 2026-06-27 red run: `testDebugUnitTest --tests "com.musfit.ui.food.FoodViewModelTest"` failed on missing barcode comparison state/actions, as expected.
- 2026-06-27 `testDebugUnitTest --tests "com.musfit.ui.food.FoodViewModelTest"`: passed.
- 2026-06-27 `testDebugUnitTest --tests "com.musfit.data.repository.LocalFoodRepositoryTest"`: passed.

### Slice 9

- 2026-06-27 red run: `testDebugUnitTest --tests "com.musfit.ui.food.FoodViewModelTest"` failed on missing fasting timer state/actions, as expected.
- 2026-06-27 `testDebugUnitTest --tests "com.musfit.ui.food.FoodViewModelTest"`: passed.
- 2026-06-27 `testDebugUnitTest --tests "com.musfit.data.repository.LocalFoodRepositoryTest"`: passed.

### Slice 10

- 2026-06-27 red run: `testDebugUnitTest --tests "com.musfit.ui.food.FoodViewModelTest"` failed on missing progress summary repository contract and progress stats state, as expected.
- 2026-06-27 `testDebugUnitTest --tests "com.musfit.ui.food.FoodViewModelTest"`: passed.
- 2026-06-27 `testDebugUnitTest --tests "com.musfit.data.repository.LocalFoodRepositoryTest"`: passed.

### Slice 11

- 2026-06-27 red run: `testDebugUnitTest --tests "com.musfit.ui.food.FoodViewModelTest"` failed on missing AI draft review state and OCR advanced-nutrient review state, as expected.
- 2026-06-27 `testDebugUnitTest --tests "com.musfit.ui.food.FoodViewModelTest"`: passed.
- 2026-06-27 `testDebugUnitTest --tests "com.musfit.data.repository.LocalFoodRepositoryTest"`: passed.
- 2026-06-27 post-UI-callout rerun of `testDebugUnitTest --tests "com.musfit.ui.food.FoodViewModelTest"`: passed.

### Closeout - 2026-06-28

- 2026-06-28 red run: `testDebugUnitTest --tests "com.musfit.ui.food.FoodViewModelTest"` failed on `weeklyMusFitScoreUsesTrailingSevenDayWindow`, showing the weekly score requested `2026-06-28` instead of the expected trailing start date `2026-06-22`.
- 2026-06-28 `testDebugUnitTest --tests "com.musfit.ui.food.FoodViewModelTest"`: passed.
- 2026-06-28 `testDebugUnitTest --tests "com.musfit.data.repository.LocalFoodRepositoryTest"`: passed.

## Remaining Follow-Ups

- None for this parity loop.

## Final Verification

- 2026-06-27 `testDebugUnitTest lintDebug assembleDebug`: passed.
- 2026-06-28 `testDebugUnitTest lintDebug assembleDebug`: passed.

## Known Limitations

- AI voice/photo logging remain UX shells by design; no cloud AI is added for the local-first MVP.
- Nutrition-label OCR parsing is best-effort and must remain review-before-save/log.
- Weekly MusFit score uses a neutral training factor until Food has a real weekly training/Health Connect signal.
- Food trust reports are local UI state only; there is no account, cloud review queue, analytics, or moderation workflow.
- Fasting timer state is local UI state for the MVP; it does not create notifications or persisted fasting history.

## Final APK

- Debug APK path after successful `assembleDebug`: `app/build/outputs/apk/debug/app-debug.apk`.

## Blocked Items

- None currently blocked.
