# MusFit Food System

The Food miniapp is the largest and most polished area of MusFit. This document
is the authoritative deep-dive: feature map, the state-driven navigation model,
the `FoodUiState` shape, the domain logic Food relies on, and the live refactor
backlog.

It complements, and does not duplicate, the cross-cutting docs:

- [Architecture overview](README.md) — layering, DI, persistence, theme.
- [Screen contracts](screen-contracts.md) — the `FoodScreen` public contract and
  flow sequence diagrams.
- [Data models](data-models.md) — Room entities, repository models, and enums
  (the canonical model reference; not repeated here).

Snapshot date: 2026-06-22. Schema version 21.

## Where Food lives

| Path | Responsibility |
| --- | --- |
| `ui/food/FoodScreen.kt` | Diary screen, summary/header, meal detail, and the `FoodSheetMode` dispatch (~2,000 lines). |
| `ui/food/FoodComponents.kt` | Shared leaf composables + formatters (`ProgressBar`, `FoodThumb`, `SectionTitle`, …). |
| `ui/food/FoodTrackersUi.kt` | Water + Health Connect "More details" cards. |
| `ui/food/FoodModalSheets.kt` | The 10 `FoodSheetMode` panels (database, editors, goals, recipes, templates, shopping). |
| `ui/food/FoodAddPanelUi.kt` | The add-food panel and its entry-mode forms. |
| `ui/food/AddFoodScreen.kt` | Full-screen add-food surface (the `AddFood` sheet mode). |
| `ui/food/FoodViewModel.kt` | Single `@HiltViewModel`; owns `FoodUiState` and every Food action. |
| `ui/food/BarcodeScannerScreen.kt` | CameraX + ML Kit barcode capture route. |
| `ui/food/NutritionLabelScannerScreen.kt` | CameraX + ML Kit OCR capture route. |
| `data/repository/FoodRepository.kt` | `FoodRepository` interface, `LocalFoodRepository`, and Food repo models. |
| `data/local/dao/FoodDao.kt` | Room DAO + Food projection rows. |
| `data/local/entity/FoodEntities.kt` | Food Room entities. |
| `data/remote/food/` | Open Food Facts provider behind `FoodProductProvider`. |
| `domain/nutrition/`, `domain/food/` | Pure nutrition calculators and the OCR parser. |

Food deliberately uses **one ViewModel and one broad UI state** for the diary,
add flow, database, all editors, and related panels. New Food work extends these
files and the `FoodAddMode` / `FoodSheetMode` state-driven conventions rather
than introducing parallel ViewModels.

## Feature map

**Diary (home).** Date navigation, planning mode, calorie ring, macro progress,
advanced-nutrient and micronutrient progress, meal sections (default + custom),
per-item macro rows, deterministic daily insights, a day-rating card, and a
7-day plan strip.

**Add flow (`FoodAddMode`).** Saved (recents, same-as-yesterday, favorites,
templates, recipes), Manual, Barcode (Open Food Facts lookup → edit → save and/or
log), Quick calories (with favorite presets), and an AI shell. "Keep adding"
mode keeps the sheet open after each log.

**Saved food database.** Full editor (name, brand, barcode, category, favorite,
per-100 g vs per-serving, custom servings, full macros + micros, delete,
duplicate), local search, online search/import, duplicate detection + merge, and
starter-food import.

**Servings.** Per-food default plus custom units (label ↔ grams), with a live
amount-based nutrition preview.

**Recipes v2.** Ingredients, serving units, cooked yield, auto per-serving
nutrition, edit/duplicate/delete/favorite, and fractional-serving logging.

**Meal templates v2.** Editable items, duplicate/favorite, "save current meal as
template", and log-to-any-meal.

**Custom meals.** Rename, optional time, reorder.

**Goals.** Calorie + macro + advanced-nutrient targets, diet modes
(`FoodGoalMode`), include-training-calories, and a net-carbs toggle.

**Planning, shopping, water.** Planned-vs-logged state, copy day/meal, shopping
list generated from planned meals (grouped, checkable, manual adds), and water
tracking with a goal.

**Health Connect.** Food/hydration export with a sync-state card.

**Cross-cutting.** Favorites across foods/templates/recipes/quick-logs, undo
delete, copy/move entries, and mark planned → logged.

**Shells (intentional).** Nutrition-label OCR is a camera + ML Kit feed into a
best-effort `NutritionLabelParser` (always reviewed before save); AI voice/photo
logging are UX shells. Cloud AI is out of scope (local-first).

## State-driven navigation

Food avoids separate nav routes for most surfaces and instead drives them from
state. The top-level `FoodScreen` chooses between a full-screen meal detail, the
full-screen add surface, the diary, or a `ModalBottomSheet` keyed by sheet mode.

`FoodSheetMode`:

| Mode | Surface |
| --- | --- |
| `AddFood` | Add-food flow (Saved/Manual/Barcode/Quick/AI). |
| `FoodDatabase` | Saved food database, online import, duplicates, starter import. |
| `FoodDetail` | Read-only saved-food detail and log action. |
| `DiaryEntryEditor` | Edit a logged/planned item: amount, meal, copy, delete, undo. |
| `SavedFoodEditor` | Full saved-food editor. |
| `NutritionLabelScan` | OCR review-before-save fields. |
| `GoalEditor` | Calorie/macro/detail goal editor. |
| `RecipeEditor` | Recipe create/edit/log. |
| `MealTemplates` | Template list, edit, duplicate, favorite, log. |
| `MealSettings` | Custom meal definitions and times. |
| `ShoppingList` | Generated and manual shopping list. |

`FoodAddMode`: `Saved`, `Manual`, `Barcode`, `Quick`, `Ai`.
`AddTab`: `Recents`, `Favorites`, `Create`.
`MealDetailSortMode`: `Logged`, `Calories`, `Protein`, `Name`.

## `FoodUiState`

`FoodUiState` is intentionally broad because one ViewModel backs every Food
surface. Today it is a single flat data class of ~160 fields. The logical groups
(see [screen-contracts.md](screen-contracts.md#food-screen) for the field-level
table):

- Date / loading / message
- Diary summary (eaten/remaining, macro + advanced + micronutrient progress,
  insights, rating)
- Meals (sections, definitions, selected detail, sort)
- Planning (weekly plan, planning mode)
- Add flow (panel visibility, sheet mode, add mode, add tab, keep-adding,
  recents, same-as-yesterday, database query)
- Barcode / manual draft (barcode, name, brand, per-100 g macros, quantity,
  lookup result, amount preview, serving choices)
- Saved foods (all, visible, duplicates, detail) **+ saved-food editor inputs**
- **Diary entry editor inputs**
- **Recipe editor inputs**
- **Meal template editor inputs**
- **Goal editor inputs**
- **Custom meal definition editor inputs**
- Quick calories, water, shopping, Health Connect sync

The **bold** groups are per-editor input fields. Collapsing them into dedicated
nullable sub-state objects is the core of Tier 1 below.

## Domain logic Food relies on

Pure, Android-free calculators (see [data-models.md](data-models.md#domain-models-and-calculators)):

- `domain/nutrition/NutritionCalculator` — meal totals; **target home for the
  amount-preview and progress-accumulation math currently inlined in the
  ViewModel** (Tier 1).
- `domain/food/NutritionLabelParser` — best-effort OCR parsing.
- Planned (Tier 1): `domain/food/DiaryInsightsCalculator` and
  `domain/food/DayRatingCalculator` for the currently in-ViewModel
  `buildDailyInsights` / `buildDayRating` heuristics.

## Testing

- `FoodViewModelTest` — JUnit with `FakeFoodRepository` / `FakeProductProvider`,
  `StandardTestDispatcher`. Pins add/edit/diary behavior.
- `LocalFoodRepositoryTest` — Robolectric + in-memory Room.
- Domain calculators — pure JUnit.

Run the focused Food suites:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.food.FoodViewModelTest" --no-daemon --console=plain
.\gradlew.bat testDebugUnitTest --tests "com.musfit.data.repository.LocalFoodRepositoryTest" --no-daemon --console=plain
```

## Refactor backlog

The layering is clean (no Android types in `domain/`, repository interfaces are
a real boundary, one-directional flow). The weight is concentrated in three
deliberate large surfaces: `FoodUiState` (~160 fields), `FoodViewModel`
(~4,500 lines), and `FoodScreen` (~4,900 lines). The goal of this backlog is to
reduce cognitive load **without changing behavior** and **without** fragmenting
the single-ViewModel state machine.

Each item is behavior-preserving and gated on the full verification command
(`testDebugUnitTest lintDebug assembleDebug`).

### Tier 1a — extract pure logic (lowest risk)

- [x] Move the amount-preview math (`quantity × per-100 g ÷ 100`) into pure
  `NutritionCalculator.nutritionForAmount`; called from the add/preview path.
  (Done — with domain tests.)
- [ ] Move macro / advanced-nutrient / micronutrient **progress accumulation**,
  `buildDailyInsights`, and `buildDayRating` out of the ViewModel.
  **Re-scoped:** these consume the repository `FoodDiary` model and produce
  `*UiState` types, so a pure-`domain/` home would force domain→repository and
  domain→UI dependencies. The honest target is a `ui/food` presentation-calculator
  file (e.g. `FoodSummaryCalculators.kt`) holding them as testable `internal`
  functions — not `domain/`. Deferred.

### Tier 1b — editor sub-state objects (medium risk, NOT YET DONE)

Replace the flat per-editor input fields in `FoodUiState` with dedicated nullable
sub-state data classes, one editor at a time, updating tests per step.

**Cost discovered while planning (do these in this order, each as its own
verified change):**

- [x] `DiaryEntryEditorState` — **done.** Collapsed 15 flat `editingDiaryEntry*`
  fields into one nullable `diaryEntryEditor` object; updated `withDiaryEntryPreview`,
  `DiaryEntryEditorPanel` (in `FoodModalSheets.kt`), and the `FoodViewModelTest`
  assertions. Behavior-preserving; full verification green.
- [ ] `GoalEditorState` — ~19 goal-related test refs.
- [ ] `MealTemplateEditorState` / `RecipeEditorState` — heaviest: ~50–80
  template/recipe test references each. Expect large test churn.
- [ ] `SavedFoodEditorState` — **entangled.** The saved-food editor reuses the
  shared add-draft fields (`productName`, `brand`, `caloriesPer100g`, …); only
  `editingSavedFoodId` is dedicated. Untangling from the add flow is a
  prerequisite, so do this one last.

Each collapses several flat fields into one object and makes "which fields are
live in which mode" obvious. Because of the test coupling above, do **not**
attempt all editors in one pass — land and verify one editor at a time.

### Tier 2 — split `FoodScreen.kt` along feature seams (mechanical)

Relocate composables into sibling files in the same `com.musfit.ui.food` package
(private composables that cross a file boundary become `internal`). No behavior
change. **Done — FoodScreen.kt went from ~4,900 to ~1,980 lines.**

- [x] `FoodComponents.kt` — shared primitives (`FoodThumb`, `ProgressBar`,
  `SectionTitle`, avatars, `SmallNumberField`, formatters).
- [x] `FoodTrackersUi.kt` — water and Health Connect cards.
- [x] `FoodModalSheets.kt` — the 10 `FoodSheetMode` panels and their helpers
  (the editor panels live here rather than a separate `FoodEditorsUi.kt`).
- [x] `FoodAddPanelUi.kt` — the add-food panel and its entry-mode forms.
- [x] `FoodScreen.kt` keeps the diary screen, summary/header, meal detail, and
  the `when (sheetMode)` dispatch.

Remaining optional Tier 2 polish:

- [ ] Split the diary "More details" summary cards (rating, insights, advanced
  nutrition, micronutrients) into a `FoodSummaryUi.kt` if `FoodScreen.kt`
  needs to shrink further.

### Deferred / do NOT

- **Do not** split `FoodViewModel` into per-sheet ViewModels — it breaks the
  unified state machine and the single-fake test model.
- **Do not** introduce a generic `(field, value)` callback bus — it loses the
  type-safety of named `onXxx` actions.
- **Defer** splitting `FoodRepository` into per-domain repositories. The 40-method
  interface with empty-default impls is a mild fat-interface smell, but each impl
  method is < 80 lines; revisit only when a natural seam forces it.
