# Food Parity: Quality Ratings And Habits Plan

> **For agentic workers:** Use superpowers:executing-plans for larger follow-up slices. For the first slice in this file, use TDD inline: write the failing ViewModel tests, run them red, implement, run green.

**Goal:** Move Food closer to Lifesum-style parity without copying Lifesum, while preserving MusFit's local-first Android MVP.

**Architecture:** Treat this as a Food presentation/domain slice, not a new backend. The first implementation derives richer quality ratings and daily habit trackers from existing diary, goal, and water state, avoiding new accounts, cloud AI, analytics, or schema changes.

**Tech Stack:** Android Kotlin, Jetpack Compose Material 3, Hilt ViewModel, Kotlin Flow, existing Room-backed FoodRepository.

## Global Constraints

- Keep changes scoped to Food.
- Keep all new behavior local-first and deterministic.
- Do not add cloud AI, accounts, analytics, subscriptions, or social features.
- Do not copy Lifesum assets, exact layouts, names, or proprietary scoring logic.
- Use TDD for behavior changes.
- Source `.\.superpowers\sdd\android-env.ps1` before Gradle commands on Windows.

## Current Parity Audit

1. **Real AI / multimodal logging:** Partially covered by barcode lookup, nutrition-label OCR review, and text/voice/photo AI shells. Missing real local multimodal interpretation and chat-style edits; cloud AI remains out of scope.
2. **Deeper food, meal, and day ratings:** Partially covered by deterministic day and meal ratings. Missing per-food rating, rating factor drill-down, and diet-mode-specific explanations.
3. **Life Score / weekly health score:** Not present as a Food score. Today has weekly goal infrastructure, but Food lacks a nutrition/hydration/exercise health score with improvement guidance.
4. **Habit trackers:** Water exists. Fruit, vegetables, and fish are only implicit in food categories/text and not first-class.
5. **Guided programs and meal-plan catalog:** Food goal modes exist. Missing browsable MusFit program/plan catalog and program-specific plan surfaces.
6. **Recipe discovery/library:** Recipes exist as user-created/local items. Missing browsable starter recipe catalog with filters and goal/program relevance.
7. **Food database trust layer:** Partially covered by source labels, imported/manual distinction, duplicate detection, and merge. Missing verified/local/imported badges as a consistent trust layer and correction/report flow.
8. **Barcode comparison scanner:** Barcode scanner exists. Missing side-by-side comparison of two scanned products.
9. **Fasting timer / fasting programs:** Not present.
10. **Detailed Food progress/statistics:** Weekly plan strip and Today weekly goals exist. Missing Food-specific weekly/monthly trends, adherence, macro consistency, habit streaks, and rating trends.

## Practical Roadmap

### Slice 1: Quality Rating Drill-Down + Habit Tracker Foundation

Build deterministic, local-first UI state for:

- Day and meal rating factor rows, including calories, protein, fiber, sodium, and diet-mode focus.
- Per-food rating pills on logged meal items.
- Daily habit trackers for fruit, vegetables, fish, and water.

This slice has no schema migration. Fruit/vegetable/fish completion is inferred from logged food names and, when available in saved-food UI state, categories. Water completion uses the existing water summary. The UI appears in Food's "More" section and meal rows/details.

### Slice 2: Food Progress Statistics

Add Food weekly/monthly trend state for calorie adherence, protein consistency, fiber/sodium trends, habit completion, and rating trend. Prefer repository/date-range queries or existing plan streams over storing derived scores.

### Slice 3: Food Database Trust Layer

Standardize verified/local/imported/source badges in database rows, saved-food detail, and add flow. Add a local correction/report queue that marks user review state without sending data anywhere.

### Slice 4: Guided Program And Recipe Discovery Catalog

Add local starter program definitions and a browsable local recipe catalog tagged by goal mode, protein level, prep style, and dietary pattern. Keep recipes original MusFit content.

### Slice 5: Barcode Comparison Scanner

Add a two-product barcode compare flow that reuses the existing scanner and Open Food Facts provider, then renders nutrition and trust differences side by side.

### Slice 6: Fasting Timer

Add a local fasting timer with presets, active interval state, and optional program guidance. Do not tie it to accounts or social features.

### Slice 7: Local Multimodal Logging Improvements

Improve the existing OCR/parser path and AI shell within local-first boundaries. Do not add cloud AI unless explicitly requested later.

## First Slice Test Plan

- Add `FoodViewModelTest` coverage proving a high-protein day rating includes a diet-mode factor and a low-protein warning.
- Add `FoodViewModelTest` coverage proving meal entries expose per-food ratings.
- Add `FoodViewModelTest` coverage proving fruit, vegetable, fish, and water habit trackers are derived from logged diary/water state.

## First Slice Implementation Notes

- Extend `FoodRatingUiState` with `score: Int?` and `factors: List<FoodRatingFactorUiState>` with defaults to preserve existing call sites.
- Add `FoodHabitTrackerUiState` and `FoodHabitStatus` to `FoodUiState`.
- Add `rating: FoodRatingUiState?` to `FoodMealEntryUiState`.
- Keep rating and habit derivation in `FoodViewModel.kt` for this slice, matching the current local pattern. A later refactor can move the pure calculators into a `FoodSummaryCalculators.kt` presentation-calculator file.
- Render factor rows in `DayRatingCard` and the meal detail macro card. Render habit trackers in the Food "More" section. Show compact food rating pills on diary entry rows.

## Verification

Run:

```powershell
. .\.superpowers\sdd\android-env.ps1
.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.food.FoodViewModelTest" --no-daemon --console=plain
.\gradlew.bat testDebugUnitTest --tests "com.musfit.data.repository.LocalFoodRepositoryTest" --no-daemon --console=plain
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain
```
