# MusFit Food System

The Food miniapp is the largest and most polished area of MusFit. This document
is the feature deep-dive: feature map, typed feature navigation, `FoodUiState`,
domain logic, and a record of the completed structure refactor. For current
cross-cutting defects or new structural work, the July 2026 architecture audit
and remediation backlog take precedence.

It complements, and does not duplicate, the cross-cutting docs:

- [Architecture overview](README.md) — layering, DI, persistence, theme.
- [Screen contracts](screen-contracts.md) — the `FoodScreen` public entrypoint
  and scanner-result flow.
- [Data models](data-models.md) — model ownership and source-of-truth map.

Do not infer the live Room version from this feature document. Derive it from
`core/database/src/main/java/com/musfit/data/local/MusFitDatabase.kt` and the newest
committed schema JSON.

## Where Food lives

| Path | Responsibility |
| --- | --- |
| `ui/food/FoodScreen.kt` | Diary screen, summary/header, meal detail, and the `FoodSheetMode` dispatch. |
| `ui/food/FoodNavigation.kt`, `FoodNavigationContract.kt` | Saveable typed Food/scanner back stack, ID-only keys, process rehydration, and exactly-once scanner results. |
| `ui/food/FoodComponents.kt` | Shared leaf composables + formatters (`ProgressBar`, `FoodThumb`, `SectionTitle`, …). |
| `ui/food/FoodTrackersUi.kt` | Water + Health Connect cards, shown as bottom-sheet content (Water via the quick-actions tile, Health Connect via the tools menu). |
| `ui/food/FoodPresentationState.kt` | Pure diary, tracker, Add/database, editor/planning, and route projections plus Food summary, rating, favorite, filter, and form reducers. |
| `ui/food/FoodModalSheets.kt` | The `FoodSheetMode` panels (database, editors, goals, recipes, templates, shopping, barcode comparison, fasting timer). |
| `ui/food/FoodAddPanelUi.kt` | The add-food panel and its entry-mode forms. |
| `ui/food/AddFoodScreen.kt` | Full-screen add-food surface (the `AddFood` sheet mode). |
| `ui/food/FoodViewModel.kt` | Single route coordinator `@HiltViewModel`; owns Food actions and persistence while exposing independently collected destination-lifetime state slices. |
| `ui/food/BarcodeScannerScreen.kt` | CameraX + ML Kit barcode capture route. |
| `ui/food/NutritionLabelScannerScreen.kt` | CameraX + ML Kit OCR capture route. |
| `ui/food/NutritionTrends.kt`, `NutritionTrendsViewModel.kt`, `NutritionTrendsScreen.kt` | Profile-owned nutrition trends route backed by Food range summaries. |
| `data/repository/FoodRepository.kt` | `FoodRepository` interface, `LocalFoodRepository`, and Food repo models. |
| `data/local/dao/FoodDao.kt` | Room DAO + Food projection rows. |
| `data/local/entity/FoodEntities.kt` | Food Room entities. |
| `data/repository/FoodProductProvider.kt` | Normalized lookup/search port and transport-independent product results consumed by Food UI. |
| `data/remote/food/` | Open Food Facts Retrofit adapter and transport DTOs behind `FoodProductProvider`. |
| `domain/nutrition/`, `domain/food/` | Pure nutrition calculators and the OCR parser. |

Food uses one route-coordinator ViewModel with destination-lifetime projections.
The diary, water/Health Connect, Add/database, and editor/planning surfaces
consume equality-stable `FoodDiaryUiState`, `FoodTrackerUiState`,
`FoodAddDatabaseUiState`, and `FoodEditorPlanningUiState` flows.
`FoodRouteUiState` carries only the active content identity needed by the route
coordinator and classifies which one of those flows is collected. Unrelated
changes do not emit into or recompose other surface groups, and no composable
collects the compatibility aggregate.
The aggregate remains internal to actions, saved-state restoration, and the
repository coordinator. Preserve the established `FoodAddMode` /
`FoodSheetMode` behavior for feature changes.

## Feature map

**Diary (home).** Date navigation, planning mode, calorie ring, macro progress,
advanced-nutrient and micronutrient progress, summarized meal rows (default +
custom meals; Turn 8 moved per-item rows onto the meal detail page),
deterministic daily insights, day/meal/per-food ratings with factor drill-down,
derived daily habit trackers, and a 7-day plan strip.

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
per-serving nutrition, edit/duplicate/delete/favorite, fractional-serving
logging, and recipe planning. The diary exposes a direct recipe-browser action;
the browser has date/meal/serving targets plus local discovery filters for high
protein, low carb, vegetarian, quick, favorites, and selected-program relevance.
Saved recipes are reused as catalog items via `RecipeUiState`; curated ideas
prefill the existing recipe editor for review before saving.

**Meal templates v2.** Editable items, duplicate/favorite, "save current meal as
template", and log-to-any-meal.

**Custom meals.** Rename, optional time, reorder, and show/hide. Meals are the 4
hardcoded defaults (breakfast, lunch, dinner, snacks) merged with `meal_definitions`
rows that override a default by id or add new meals. Hiding is a soft flag
(`meal_definitions.isHidden`) — a hidden meal is excluded from the diary
and from add-target pickers (`FoodUiState.visibleMealDefinitions`) but its logged
food still counts toward day totals; at least one meal must stay visible.

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

**Quality and habits.** Ratings remain deterministic/local-first.
Day and meal ratings expose calorie, protein, fiber, sodium, and diet-mode
factors; logged foods get compact quality ratings; fruit, vegetable, fish, and
water habits are derived from today's diary and water progress.

**Profile-owned nutrition trends.** `NutritionTrendsScreen` is a Profile
secondary route implemented in the Food UI package. Its ViewModel observes a
today-anchored seven-day `FoodWeeklySummary` and 28-day `FoodProgressSummary`.
`NutritionTrends.kt` derives the weekly MusFit score plus weekly/monthly cards
for tracked days, average calories/protein, target and hydration adherence,
habit coverage, and calorie trends. The score uses a neutral training factor
until a weekly Training/Health Connect signal is connected.

**Health Connect.** Food/hydration export implementation with a sync-state card.
An explicit sync treats the current local day as the source of truth: removed
MusFit meal aggregates and zeroed hydration delete only their ledger-backed,
MusFit-authored Health Connect records. Manual and imported provider records are
outside this deletion boundary.

**Cross-cutting.** Favorites across foods/templates/recipes/quick-logs, undo
delete, copy/move entries, and mark planned → logged.

**Shells (intentional).** Nutrition-label OCR is a camera + ML Kit feed into a
best-effort `NutritionLabelParser` that extracts calories, macros, fiber, sugar,
saturated fat, and sodium when present, then exposes a confidence label and
parsed-field count for review before save/log. Food voice/photo logging are UX
shells. The separate global coach supports opt-in local or user-configured API
endpoints but does not currently write Food data.

## Remaining Food work

The Lifesum-style feature-parity loop is closed. The list below is
Food-specific deferred scope; repo-wide data-safety, account, performance,
Health Connect, and architecture work remains in the remediation backlog.

- AI voice/photo logging remain UX shells and require explicit product scope
  before becoming model-backed write flows.
- Nutrition-label OCR parsing is best-effort and always review-before-save/log.
- Provider-side deletion/revocation reconciliation for imported Health data is
  tracked separately by `W3-HC-03B`.
- The Profile nutrition-trends score uses a neutral training factor until a real weekly
  Training/Health Connect signal is connected.
- Food trust reports are local UI state, not a persisted review queue.
- Fasting timer state is MVP-local UI state; notifications and fasting history
  are out of scope for this loop.

## Typed feature navigation

The app shell retains one `FoodNavKey` entry. Inside it, `FoodNavigation` owns a
saveable Navigation 3 stack of serializable `FoodNavKey` values for diary, meal
detail, Add/database, editors, planners, and both scanner producers. Keys carry
only stable IDs and primitive arguments. They are the durable destination
identity across process recreation; `FoodViewModel` continues to own mutable
content and persistence, and the navigation coordinator rehydrates transient
content from the current key.

Within the current typed destination, `FoodScreen` chooses the full-screen or
`ModalBottomSheet` presentation from `FoodSheetMode`. Successful asynchronous
save/delete operations clear their transient content and then reconcile the
matching key; validation or repository failures keep the destination open.
Barcode and OCR values return through a typed result only when the current key
is the matching scanner producer, preventing duplicate handling.

`FoodSheetMode`:

| Mode | Surface |
| --- | --- |
| `AddFood` | Add-food flow (Saved/Manual/Barcode/Quick/AI). |
| `FoodDatabase` | Saved food database, online import, duplicates, starter import. |
| `FoodDetail` | Read-only saved-food detail and log action. |
| `DiaryEntryEditor` | Edit a logged/planned item: amount, meal, copy, delete, undo. |
| `SavedFoodEditor` | Full saved-food editor. |
| `NutritionLabelScan` | OCR review-before-save fields. |
| `BarcodeComparison` | Compare saved/imported barcode foods and nutrient highlights. |
| `FastingTimer` | Local fasting-program timer and progress. |
| `GoalEditor` | Calorie/macro/detail goal editor. |
| `RecipeBrowser` | Full-screen recipe discovery, saved-recipe browsing, and plan-to-date/meal actions. |
| `RecipeEditor` | Recipe create/edit/log. |
| `MealTemplates` | Template list, edit, duplicate, favorite, log. |
| `MealSettings` | Custom meal definitions: rename, time, order, and per-meal show/hide toggle. |
| `ShoppingList` | Generated and manual shopping list. |
| `Water` | Water quick-add, custom amount, and goal controls. |
| `HealthConnect` | Food/hydration sync settings and status. |

`FoodAddMode`: `Saved`, `Manual`, `Barcode`, `Quick`, `Ai`.
`AddTab`: `Recents`, `Favorites`, `Create`.
`MealDetailSortMode`: `Logged`, `Calories`, `Protein`, `Name`.

## `FoodUiState`

`FoodUiState` is the internal compatibility and restoration model because one
ViewModel still coordinates repository mutations across Food. It is projected
before presentation; UI routes never collect it directly. Major editors also
retain dedicated sub-state objects. Its logical groups are:

- Date / loading / message
- Diary summary (eaten/remaining, macro + advanced + micronutrient progress,
  insights, rating, quality factors, and habit trackers)
- Meals (sections, definitions, selected detail, sort)
- Planning (weekly plan, planning mode)
- Add flow (panel visibility, sheet mode, add mode, add tab, keep-adding,
  recents, same-as-yesterday, database query, local AI draft review, nutrition
  label scan review)
- Barcode / manual draft (barcode, name, brand, per-100 g macros, quantity,
  lookup result, amount preview, serving choices)
- Saved foods (all, visible, duplicates, detail, trust state, local review flags)
  plus nullable `SavedFoodEditorState`
- Barcode comparison (`BarcodeComparisonUiState`, loaded comparison items,
  nutrient highlights)
- Fasting timer (`FastingTimerUiState`, preset/custom program inputs)
- Nullable `DiaryEntryEditorState`
- Nullable `RecipeEditorState`
- Recipe discovery (`RecipeDiscoveryItemUiState`, `RecipeDiscoveryFilter`)
- Nullable `MealTemplateEditorState`
- Non-null `GoalEditorState`
- Program catalog (`FoodProgramUiState`) derived from local goal presets
- Custom meal definition editor inputs (still flat)
- Quick calories, water, shopping, Health Connect sync

## Domain logic Food relies on

Pure, Android-free calculators (see [data-models.md](data-models.md#domain)):

- `domain/nutrition/NutritionCalculator` — meal totals and amount-preview
  nutrition math extracted from the ViewModel.
- `domain/food/NutritionLabelParser` — best-effort OCR parsing.
- Parser output includes label calories, macros, selected advanced nutrients,
  parsed-field count, and a confidence label.
- Daily insight, day-rating, nutrient-progress, favorites, database filtering,
  and decimal-form presentation reducers live in `FoodPresentationState.kt` and
  have direct pure unit tests. Weekly score/progress derivation remains in
  `NutritionTrends.kt` and `NutritionTrendsViewModel`.

## Testing

- `FoodViewModelTest` — JUnit with `FakeFoodRepository` / `FakeProductProvider`,
  `StandardTestDispatcher`. Pins add/edit/diary behavior.
- `LocalFoodRepositoryTest` — Robolectric + in-memory Room.
- Domain calculators — pure JUnit.

Run the focused Food suites:

```powershell
.\gradlew.bat testInternalDebugUnitTest --tests "com.musfit.ui.food.FoodViewModelTest" --no-daemon --console=plain
.\gradlew.bat testInternalDebugUnitTest --tests "com.musfit.data.repository.LocalFoodRepositoryTest" --no-daemon --console=plain
```

## Historical Structure-Refactor Record

This section records the earlier behavior-preserving Food cleanup. It is not the
active architecture backlog. The broad Food state surface remains governed by
the current remediation plan; transport and feature-boundary imports are
enforced separately by `ArchitectureBoundaryTest`.

Each item is behavior-preserving and gated on the full verification command
(`verifyReleaseVariantMatrix testInternalDebugUnitTest
testProductionReleaseUnitTest lintInternalDebug
lintProductionRelease assembleInternalDebug assembleInternalDebugAndroidTest
assembleProductionRelease bundleProductionRelease`).

### Tier 1a — extract pure logic (lowest risk)

- [x] Move the amount-preview math (`quantity × per-100 g ÷ 100`) into pure
  `NutritionCalculator.nutritionForAmount`; called from the add/preview path.
  (Done — with domain tests.)
- [x] Move macro / advanced-nutrient / micronutrient **progress accumulation**,
  `buildDailyInsights`, and `buildDayRating` out of the ViewModel.
  **Re-scoped:** these consume the repository `FoodDiary` model and produce
  `*UiState` types, so a pure-`domain/` home would force domain→repository and
  domain→UI dependencies. The honest target is a `ui/food` presentation-calculator
  file (e.g. `FoodSummaryCalculators.kt`) holding them as testable `internal`
  functions — not `domain/`. Completed by `W4-STATE-F1` with byte-for-output
  compatibility tests and independently collected diary/tracker projections.

### Tier 1b — editor sub-state objects — DONE

Each editor's flat input fields in `FoodUiState` were collapsed into a dedicated
sub-state data class, one editor at a time, each behavior-preserving and verified:

- [x] `DiaryEntryEditorState` — flat `editingDiaryEntry*` fields → one nullable
  `diaryEntryEditor`.
- [x] `GoalEditorState` — `goal*Input` fields → a **non-nullable** `goalEditor`
  (the goal draft is always present/synced, never nulled, so non-nullable
  preserves behavior exactly).
- [x] `MealTemplateEditorState` — `template*` fields → nullable `mealTemplateEditor`.
- [x] `RecipeEditorState` — recipe editor fields → nullable `recipeEditor`.
  Note: `recipeServingsToLog` stays a flat field — it is the recipe-**log** quantity
  (RecipeQuickList / `logRecipe`), not the editor.
- [x] `SavedFoodEditorState` — `editingSavedFoodId`/`savedFood*` fields → nullable
  `savedFoodEditor`. (Earlier worry that this editor shared the add-draft fields was
  **wrong** — it uses entirely dedicated `savedFood*` fields. The overloaded prefix
  fields `savedFoodQuantityGrams`, `savedFoods`, `selectedSavedFoodDetail` are the
  picker/database, not the editor, and stay flat.)

Result: the flat editor-input groups collapsed into dedicated sub-state objects, making
"which fields are live in which mode" explicit and each editor unit-reasonable
in isolation.

### Tier 2 — split `FoodScreen.kt` along feature seams (mechanical)

Relocate composables into sibling files in the same `com.musfit.ui.food` package
(private composables that cross a file boundary become `internal`). No behavior
change. **Done.**

- [x] `FoodComponents.kt` — shared primitives (`FoodThumb`, `ProgressBar`,
  `SectionTitle`, avatars, `SmallNumberField`, formatters).
- [x] `FoodTrackersUi.kt` — water and Health Connect cards.
- [x] `FoodModalSheets.kt` — the `FoodSheetMode` panels and their helpers
  (the editor panels live here rather than a separate `FoodEditorsUi.kt`).
- [x] `FoodAddPanelUi.kt` — the add-food panel and its entry-mode forms.
- [x] `FoodScreen.kt` keeps the diary screen, summary/header, meal detail, and
  the `when (sheetMode)` dispatch.

### Decisions Recorded By The Original Refactor

These constraints explain the earlier slice. A newer, explicitly scoped
architecture-remediation package may supersede them.

- **Do not** split `FoodViewModel` into per-sheet ViewModels — it breaks the
  unified state machine and the single-fake test model.
- **Do not** introduce a generic `(field, value)` callback bus — it loses the
  type-safety of named `onXxx` actions.
- **Defer** splitting `FoodRepository` into per-domain repositories until a
  current remediation package identifies and tests a natural seam.
