# Meal Details Cleanup Design

## Goal

The meal details page (`MealDetailScreen`, full-screen) currently shows the
same data in several places, so it reads as cluttered rather than dense. This
slice removes the redundant presentations and folds meal-level nutrition detail
behind an expander, keeping the page scannable while every number stays
reachable.

## Problem (current redundancy)

1. **Per-item macros shown twice** — each logged item renders a
   `P 8 g | C 45 g | F 5 g` text line *and* a set of P/C/F contribution bars
   directly beneath it (`MealItemContributionBars`).
2. **Per-item calories shown twice** — the `280 kcal` number *and* a separate
   "Calories" contribution bar.
3. **Overloaded summary card** — `MealDetailMacroCard` stacks the calorie
   headline, three macro meters, the rating-factor list, the advanced-nutrition
   grid (fiber / sugar / sat-fat / sodium), *and* the micronutrient grid, all
   always-visible at the single-meal level.

## Scope

UI composition only, entirely within
`app/src/main/java/com/musfit/ui/food/FoodScreen.kt`. No ViewModel, repository,
DAO, domain, or Room/migration changes. No new files. The direction chosen is
"collapse extras": strip the duplication, keep a lean always-visible summary,
and move the deeper nutrition detail into a single expander.

Confirmed enabling facts:

- `showContributions = true` is passed to `DiaryEntryRow` in exactly one place
  (the meal detail list); the default is `false` everywhere else, so removing
  the bars affects no other screen.
- `advancedNutritionProgress` and `micronutrients` already exist on
  `FoodMealSectionUiState`; folding them into an expander is pure presentation.

## Design

**Item rows.** Each logged item collapses to one line: thumbnail · name (with
the existing rating dot) · a compact `120 g · P 8  C 45  F 5 g` metadata line ·
`280 kcal` trailing. The `showContributions` branch is removed from
`DiaryEntryRow`, and `MealItemContributionBars` / `MealItemContributionBar` are
deleted as now-dead code.

**Meal summary card (`MealDetailMacroCard`).** The header line becomes the
single calorie statement — `540 / 600 kcal` prominent, with the rating pill on
the right. The redundant "Meal intake" title and the separate "Target 600 kcal"
caption are dropped. The calorie progress bar and the three macro meters
(Carbs / Protein / Fat) stay always-visible.

**"More nutrition" expander.** A single tappable row inside the summary card
(label such as `Fiber, sugar, micronutrients` + chevron) expands to reveal, in
order, the existing rating-factor list, advanced-nutrition grid, and
micronutrient grid — the same composables that render today, hidden by default.
Expand/collapse is ephemeral UI state held in a local `rememberSaveable`
boolean; it is not lifted into the ViewModel or persisted.

**Chrome tidy-up.**

- `Copy yesterday` and `Save template` move off their standalone chip row into a
  `⋯` overflow menu in the header.
- The four sort `FilterChip`s are replaced by a compact item-count label
  (`3 items`) plus a sort dropdown on the right. The dropdown keeps the exact
  same four modes (Logged, Calories, Protein, Name) and drives the existing
  `onSortModeChanged` callback — only the control's shape changes.
- The add-food search bar and the undo/message banner are kept as they are.

## Out of scope / unchanged

- Sorting logic, add-food, copy-yesterday, save-template, and undo behavior are
  all preserved — only their presentation moves.
- No changes to the diary summary screen, the day-level nutrition rows, or any
  food-detail screen.

## Testing

No new ViewModel state is introduced, so existing `FoodViewModelTest` coverage
stays green and no new unit test is warranted (the only new state is a local
`rememberSaveable` toggle, which is not VM-testable without instrumentation).

Full verification: `./gradlew.bat testDebugUnitTest lintDebug assembleDebug`
must pass before completion.
