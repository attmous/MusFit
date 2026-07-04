# Food main-tab UI restructure — unified hero, quick actions, grouped detail

**Date:** 2026-07-04
**Status:** approved (design)
**Scope:** Food diary "home" screen (`FoodScreen.kt` diary column) only.
Behavior-preserving re-composition of existing components plus one small
tracker sheet — no repository/Room changes.

**Relationships:**
- Aligns with [`2026-07-02-fab-consolidation-design.md`](2026-07-02-fab-consolidation-design.md):
  "add-food floating actions belong to Food's own content." This spec **restores**
  a Food quick-add FAB (removed in `3d2577c "Polish food diary meal summaries"`
  during meal-card densification), with a simpler behavior than the old
  expand-to-meal-picker FAB added in `700fc4f`.
- Builds on [`2026-07-01-design-language-consistency-design.md`](2026-07-01-design-language-consistency-design.md):
  keeps `MusFitScreenHeader` + `MusFitSummaryCard` as the shared scaffold.

## Problem

The main tab accreted three recent additions (Browse recipes, weekly planning,
densified meal diary) as bolt-ons, and the hierarchy now points at the wrong
things:

- **The loudest element is a secondary action.** The full-width filled brand
  `FoodPrimaryActionRow` ("Browse recipes") is the single strongest CTA on the
  screen, while the *daily* action — logging food — has no prominent entry point
  (only the per-meal `+`; the diary FAB was removed).
- **Calories and macros are one story split across two cards.** `FoodDiarySummaryCard`
  (calorie ring) and `MacroProgressRow` are separate, with the Browse-recipes
  button wedged between them.
- **"More details" is a grab-bag.** The single `MoreSection` accordion hides ten
  heterogeneous cards — daily actions (Water), analytics (weekly score, progress
  stats), and reference (Food database, Health Connect) — behind one toggle.
  Users either never open it or open it into a wall.
- **Water — a daily habit — is buried** inside that accordion.
- **Redundant navigation.** Recipes is reachable from both the big button and the
  `⋮` overflow; planning from a `Plan` chip, the overflow, and a status card.
  There is also a dead duplicate `FoodSheetMode.RecipeBrowser -> Unit` branch.

Goals for this pass (user-selected): **cleaner hierarchy** and **integrate the new
features**, via a **layout restructure**.

## Design

### Main scroll order (top → bottom)

1. **Header** — `MusFitScreenHeader("Food")` with just the `⋮` tools menu. The
   header `Plan` chip (`FoodPlanningModeButton`) is removed — planning now lives
   in the Plan tile — so the three redundant planning entry points collapse to one.
2. **Unified hero** — day navigator → calorie ring → macro bars, in one card.
3. **Quick-actions row (3-up)** — Recipes · Plan week · Water.
4. **Meal diary** — `SectionTitle("Meal diary")` + the existing `MealSectionCard`
   list. **Unchanged.**
5. **"Today's summary"** (collapsible group).
6. **"Trends"** (collapsible group).

Plus a **floating `+` FAB** overlaid bottom-end for Add food.

### 1. Unified hero

`MacroProgressRow` folds into `FoodDiarySummaryCard`: the day navigator stays on
top, the calorie ring in the middle (Eaten / "kcal left" / Goal), and the P/C/F
progress bars render as a compact strip beneath the ring, inside the same
`MusFitSummaryCard`. `MacroProgressRow`/`MacroProgressColumn` are reused as-is,
just relocated. This deletes a standalone card and the gap the Browse-recipes
button occupied.

Planning: when `isPlanningMode` (or a plan exists), the `WeeklyPlanStrip` renders
inside the hero under the ring. The separate `PlanningModeStatusCard` is retired;
its "Done" affordance is carried by the selected state of the Plan tile (below).
`MessageBanner` renders directly under the hero.

### 2. Quick-actions row + FAB

Replace `FoodPrimaryActionRow` with a new 3-up `FoodQuickActionsRow` sitting
directly under the hero:

| Tile | Action | Wiring |
| --- | --- | --- |
| **Recipes** | open recipe browser | existing `openRecipeBrowser()` |
| **Plan week** | toggle planning mode; reads *selected* while active | existing `togglePlanningMode()` + `isPlanningMode` |
| **Water** | open Water sheet | new `openWaterSheet()` → `FoodSheetMode.Water` |

This demotes "Browse recipes" from loudest element to a peer, gives weekly
planning a first-class home (retiring the chip-plus-status-card sprawl), and
pulls Water out of the accordion.

**FAB (Add food).** Reintroduce a `FloatingActionButton` in the diary `Box`
(`Alignment.BottomEnd`, padding clearing the bottom nav — the diary column already
reserves `bottom = 96.dp`). `onClick` opens the existing Add-food flow defaulted
to the **time-appropriate meal** (breakfast / lunch / dinner / snack chosen from
the current clock against `state.mealSections`); the meal stays switchable inside
the Add panel. This is simpler than the removed `700fc4f` FAB (no scrim, no
expand-to-meal-picker menu, no `MealPickerItem`). The per-meal `+` on each
`MealSectionCard` stays for targeted adds.

Default-meal selection is a small pure helper (e.g. `internal fun defaultAddMealId(
sections, now): String?`) so it is unit-testable and Android-free where possible.

### 3. Grouped detail — replace the single accordion

Retire `MoreSection` (one toggle → ten cards). Introduce **two labeled
collapsibles**, each reusing the existing card composables verbatim:

- **"Today's summary"** — `DayRatingCard`, `DailyInsightsSection`,
  `FoodHabitTrackerSection`, `AdvancedNutritionProgressRow`, `MicronutrientRow`.
- **"Trends"** — `WeeklyMusFitScoreCard`, `FoodProgressStatsCard`.

`WaterTrackerCard`, `FoodHealthConnectSyncCard`, and `FoodDatabasePreview` leave
this section entirely (see Tools menu / Water sheet).

Each collapsible keeps `rememberSaveable` expand state, independent of the other.

### Tools menu (`⋮`)

`FoodDiaryOverflowAction` is re-scoped to occasional/reference features:

`Goals · Meals · Templates · Shopping list · Fasting · Health Connect ·
Food database · Copy day to tomorrow`

- **Removed** from the menu: *Recipes* and *Start/Finish planning* (now the
  Recipes and Plan tiles).
- **Added** to the menu: *Food database* (`openFoodDatabase()` → existing
  `FoodSheetMode.FoodDatabase`) and *Health Connect*.
- *Shopping list* and *Fasting* already open as sheets and stay in the menu.

### Water & Health Connect sheets

Both are thin `ModalBottomSheet` wrappers around cards that already exist in
`FoodTrackersUi.kt`, consistent with the state-driven sheet convention:

- **`FoodSheetMode.Water`** — renders `WaterTrackerCard` (quick add, custom amount,
  goal) in a sheet. Opened by the Water tile via `openWaterSheet()`.
- **`FoodSheetMode.HealthConnect`** — renders `FoodHealthConnectSyncCard` in a
  sheet. Opened by the Health Connect menu item via `openHealthConnectSheet()`.

Both reuse the existing card composables and their existing VM callbacks
(`logQuickWater`, `onWaterGoalChanged`, `syncFoodToHealthConnect`, the HC
permission launcher, etc.) — no new tracker logic.

## What does NOT change

- The **meal diary** cards (`MealSectionCard`, `DiaryEntryRow`, meal detail,
  contribution bars) — the densified spine stays as tuned.
- The **Add-food flow**, all editors, the recipe browser, and every other
  `FoodSheetMode` panel.
- **No** repository, DAO, entity, Room-migration, or domain-model changes.
- **No** new analytics, cloud, or AI surface. Local-first unchanged.

## Component / state change summary

`FoodScreen.kt`
- Reorder the diary `Column`; move `MacroProgressRow` into `FoodDiarySummaryCard`.
- Delete `FoodPrimaryActionRow`; add `FoodQuickActionsRow` (3 tiles).
- Delete `MoreSection` usage; add two grouped collapsibles ("Today's summary",
  "Trends").
- Overlay a `FloatingActionButton` in the root `Box`; add `defaultAddMealId(...)`.
- Retire `PlanningModeStatusCard`; render `WeeklyPlanStrip` inside the hero.
- Remove the header `FoodPlanningModeButton` chip (planning is the Plan tile).
- Extend `FoodDiaryOverflowAction` (add Health Connect + Food database; drop
  Recipes + Planning).
- Remove the dead duplicate `FoodSheetMode.RecipeBrowser -> Unit` branch; wire the
  two new sheet modes into the `when (sheetMode)` dispatch.

`FoodViewModel.kt` / `FoodUiState`
- Add `FoodSheetMode.Water` and `FoodSheetMode.HealthConnect`.
- Add `openWaterSheet()` and `openHealthConnectSheet()` (set `sheetMode` +
  `isAddPanelVisible`, mirroring the existing sheet-open helpers).
- Existing tracker/HC callbacks are reused unchanged.

## Testing

- `FoodViewModelTest` — add cases: `openWaterSheet()` / `openHealthConnectSheet()`
  set the expected `sheetMode`; `defaultAddMealId(...)` picks the right meal by
  time boundaries (and falls back sensibly when custom meals are configured).
- All existing `FoodViewModelTest` / `LocalFoodRepositoryTest` cases stay green.
- Pure helper `defaultAddMealId` gets direct unit coverage.

## Verification

- `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain`
  passes.
- On the seeded emulator (`install-seed-musfit.ps1 -Reset`), screenshot/UI-tree
  evidence of: the restructured diary (hero with macros, 3-tile row, two grouped
  collapsibles), the FAB opening Add defaulted to the current meal, the Water
  sheet, and the `⋮` menu reaching Health Connect + Food database.

## Open defaults (chosen; easy to revisit)

- **FAB default meal** = time-of-day bucket. Alternative considered: last-used
  meal. Time-of-day is more predictable for a fresh day.
- **Health Connect** demoted to a sheet rather than kept inline in "Trends".
  Alternative: fold Water + Health Connect into one "Trackers" sheet — rejected to
  keep the prominent Water action single-purpose.
