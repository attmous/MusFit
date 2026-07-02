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

Snapshot date: 2026-07-03. Schema version 28.

## Where Food lives

| Path | Responsibility |
| --- | --- |
| `ui/food/FoodScreen.kt` | Diary screen, summary/header, meal detail, and the `FoodSheetMode` dispatch (~2,000 lines). |
| `ui/food/FoodComponents.kt` | Shared leaf composables + formatters (`ProgressBar`, `FoodThumb`, `SectionTitle`, …). |
| `ui/food/FoodTrackersUi.kt` | Water + Health Connect "More details" cards. |
| `ui/food/FoodModalSheets.kt` | The `FoodSheetMode` panels (database, editors, goals, recipes, templates, shopping, barcode comparison, fasting timer). |
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
per-item macro rows, deterministic daily insights, day/meal/per-food ratings
with factor drill-down, a weekly MusFit score, derived daily habit trackers, and
a 7-day plan strip.

**Add flow (`FoodAddMode`).** Saved (recents, same-as-yesterday, favorites,
templates, recipes), Manual, Barcode (Open Food Facts lookup → edit → save and/or
log), Quick calories (with favorite presets), and AI text logging. AI text uses
a deterministic local estimator for simple descriptions and always opens an
editable review draft before logging. "Keep adding" mode keeps the sheet open
after each log.

**Saved food database.** Full editor (name, brand, barcode, category, favorite,
per-100 g vs per-serving, custom servings, full macros + micros, delete,
duplicate), local search, online search/import, duplicate detection + merge, and
starter-food import. Saved foods expose local trust/source guidance
(`FoodTrustUiState`) for barcode imports, manual entries, nutrition-label scans,
and user-reported review needs. Reporting is local UI state; correction reuses
the saved-food editor and repository upsert path. The database also exposes a
barcode comparison sheet (`BarcodeComparisonUiState`) that compares two saved or
Open Food Facts barcode products side by side using per-100 g calories, macros,
sugar, and sodium.

**Servings.** Per-food default plus custom units (label ↔ grams), with a live
amount-based nutrition preview.

**Recipes v2 and discovery.** Ingredients, serving units, cooked yield, auto
per-serving nutrition, edit/duplicate/delete/favorite, and fractional-serving
logging. The recipe sheet also includes a local discovery catalog layered in the
ViewModel/UI, with filters for high protein, low carb, vegetarian, quick,
favorites, and selected-program relevance. Saved recipes are reused as catalog
items via `RecipeUiState`; curated ideas prefill the existing recipe editor for
review before saving.

**Meal templates v2.** Editable items, duplicate/favorite, "save current meal as
template", and log-to-any-meal.

**Custom meals.** Rename, optional time, reorder.

**Goals and programs.** Calorie + macro + advanced-nutrient targets, diet modes
(`FoodGoalMode`), include-training-calories, and a net-carbs toggle. The goals
sheet also exposes a local program catalog: Balanced, High Protein, Muscle Gain,
Weight Loss, Keto Low Carb, Mediterranean-style, and Clean Eating. Applying a
program persists the matching `FoodGoal` targets and shows suggested habits and
meal-planning guidance.

**Planning, shopping, water.** Planned-vs-logged state, copy day/meal, shopping
list generated from planned meals (grouped, checkable, manual adds), and water
tracking with a goal.

**Fasting.** A local-first fasting timer panel (`FastingTimerUiState`) exposes
12:12, 14:10, 16:8, and custom 24-hour split programs. It stores only current
UI state for the MVP: selected plan, start time, derived fasting/eating windows,
and progress as the fast-hours share of the day.

**Quality, habits, and weekly score.** Ratings remain deterministic/local-first.
Day and meal ratings expose calorie, protein, fiber, sodium, and diet-mode
factors; logged foods get compact quality ratings; fruit, vegetable, fish, and
water habits are derived from today's diary and water progress. The weekly
MusFit score combines seven-day nutrition consistency, tracked-day hydration,
weekly fruit/vegetable/fish habit coverage, and a neutral training factor until
Food has a real weekly training/Health Connect signal. The range is the
selected date's trailing seven-day window.

**Progress stats.** `FoodProgressSummary` reuses the range diary/water queries
for 28-day local history. `FoodProgressStatsUiState` derives weekly and monthly
cards with tracked days, average calories/protein, calorie-target adherence,
hydration adherence, habit coverage, and calorie trend labels.

**Health Connect.** Food/hydration export with a sync-state card.

**Cross-cutting.** Favorites across foods/templates/recipes/quick-logs, undo
delete, copy/move entries, and mark planned → logged.

**Shells (intentional).** Nutrition-label OCR is a camera + ML Kit feed into a
best-effort `NutritionLabelParser` that extracts calories, macros, fiber, sugar,
saturated fat, and sodium when present, then exposes a confidence label and
parsed-field count for review before save/log. AI voice/photo logging are UX
shells. Cloud AI is out of scope (local-first).

## Remaining Food work

The Lifesum-style Food parity loop is closed. Remaining Food work is intentionally
limited to hardening and explicitly deferred local-first scope:

- AI voice/photo logging remain UX shells; cloud AI stays out of scope.
- Nutrition-label OCR parsing is best-effort and always review-before-save/log.
- Weekly MusFit score uses a neutral training factor until a real weekly
  Training/Health Connect signal is connected.
- Food trust reports are local UI state, not a persisted review queue.
- Fasting timer state is MVP-local UI state; notifications and fasting history
  are out of scope for this loop.

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
  insights, rating, weekly score, progress stats, quality factors, habit
  trackers)
- Meals (sections, definitions, selected detail, sort)
- Planning (weekly plan, planning mode)
- Add flow (panel visibility, sheet mode, add mode, add tab, keep-adding,
  recents, same-as-yesterday, database query, local AI draft review, nutrition
  label scan review)
- Barcode / manual draft (barcode, name, brand, per-100 g macros, quantity,
  lookup result, amount preview, serving choices)
- Saved foods (all, visible, duplicates, detail, trust state, local review flags)
  **+ saved-food editor inputs**
- Barcode comparison (`BarcodeComparisonUiState`, loaded comparison items,
  nutrient highlights)
- Fasting timer (`FastingTimerUiState`, preset/custom program inputs)
- **Diary entry editor inputs**
- **Recipe editor inputs**
- Recipe discovery (`RecipeDiscoveryItemUiState`, `RecipeDiscoveryFilter`)
- **Meal template editor inputs**
- **Goal editor inputs**
- Program catalog (`FoodProgramUiState`) derived from local goal presets
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
- Parser output includes label calories, macros, selected advanced nutrients,
  parsed-field count, and a confidence label.
- Planned (Tier 1): a `ui/food` presentation-calculator extraction for the
  currently in-ViewModel `buildDailyInsights`, `buildDayRating`, and weekly
  score heuristics.

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

### Tier 1b — editor sub-state objects — DONE

Each editor's flat input fields in `FoodUiState` were collapsed into a dedicated
sub-state data class, one editor at a time, each behavior-preserving and verified:

- [x] `DiaryEntryEditorState` — 15 flat `editingDiaryEntry*` fields → one nullable
  `diaryEntryEditor`.
- [x] `GoalEditorState` — 11 `goal*Input` fields → a **non-nullable** `goalEditor`
  (the goal draft is always present/synced, never nulled, so non-nullable
  preserves behavior exactly).
- [x] `MealTemplateEditorState` — 6 `template*` fields → nullable `mealTemplateEditor`.
- [x] `RecipeEditorState` — 12 `recipe*` editor fields → nullable `recipeEditor`.
  Note: `recipeServingsToLog` stays a flat field — it is the recipe-**log** quantity
  (RecipeQuickList / `logRecipe`), not the editor.
- [x] `SavedFoodEditorState` — 22 `editingSavedFoodId`/`savedFood*` fields → nullable
  `savedFoodEditor`. (Earlier worry that this editor shared the add-draft fields was
  **wrong** — it uses entirely dedicated `savedFood*` fields. The overloaded prefix
  fields `savedFoodQuantityGrams`, `savedFoods`, `selectedSavedFoodDetail` are the
  picker/database, not the editor, and stay flat.)

Result: `FoodUiState`'s flat editor-input fields (~80) collapsed into 6 sub-state
objects, making "which fields are live in which mode" explicit and each editor
unit-reasonable in isolation.

### Tier 2 — split `FoodScreen.kt` along feature seams (mechanical)

Relocate composables into sibling files in the same `com.musfit.ui.food` package
(private composables that cross a file boundary become `internal`). No behavior
change. **Done — FoodScreen.kt went from ~4,900 to ~1,980 lines.**

- [x] `FoodComponents.kt` — shared primitives (`FoodThumb`, `ProgressBar`,
  `SectionTitle`, avatars, `SmallNumberField`, formatters).
- [x] `FoodTrackersUi.kt` — water and Health Connect cards.
- [x] `FoodModalSheets.kt` — the `FoodSheetMode` panels and their helpers
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
