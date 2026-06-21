# MusFit Food UI — Slice 2: Structure & Hierarchy

**Date:** 2026-06-21
**Status:** Approved design pending review, then plan
**Area:** Food miniapp (`com.musfit.ui.food`), app navigation (`com.musfit.ui`)
**Builds on:** Slice 1 (`2026-06-21-food-design-system-reskin-design.md`) — the `MusFitTheme` token layer.

## Background

Slice 1 gave the Food screen a coherent skin but kept the original layout: a cluttered header
(ten outlined action buttons in a scroll strip), and the meal diary buried beneath a weekly strip, six
stat cards, a day rating, and an insights section. This slice restructures the screen to the agreed
north-star: a clean header, the meal diary surfaced directly under the macros, secondary content
collapsed behind a "More" expander, category-icon meal cards, a coral FAB, and real bottom-nav icons.

This is the second of three slices. Slice 3 (food imagery) builds on this slice's meal-card structure.

## Goals

1. Rework the header: title + an overflow menu for management actions, a centered date navigator, and the
   calorie ring flanked by compact metrics.
2. Surface the meal diary directly under the macro cards.
3. Collapse all secondary sections behind a single, collapsed-by-default **"More"** expander.
4. Redesign meal-section cards with category icons and a coral add button.
5. Add a coral **FAB** that opens the add-food flow.
6. Replace the letter-based bottom-nav icons with real Material icons across all four tabs.

## Non-goals (out of scope for Slice 2)

- Food photos / image loading — Slice 3.
- New nutrition data or training-calorie ("Burned") integration. The ring keeps **Eaten / Goal** (the data
  we have); an exercise-calories metric is a later enhancement.
- Spacing/typography token application beyond what these new components need (the broader pass stays deferred).
- Dark theme values.
- Changes to Today / Training / Health beyond the shared bottom-nav icons.

## Design

### New dependency

Add `androidx.compose.material:material-icons-extended` (via the Compose BOM) for the meal-category, nav,
FAB, and overflow icons. Declared in `gradle/libs.versions.toml` and `app/build.gradle.kts`.

### A. Header

Replace the gradient header's contents (keep the `brandGradient` background and rounded bottom):

- **Row 1:** `Food` title (`brandInk`) on the left; an **overflow icon button** (`Icons.Default.MoreVert`,
  `brandInk`) on the right. Tapping it opens a `DropdownMenu` with the management actions currently crammed
  into the strip: **Goals, Meals, Templates, Recipes, Shopping, Planning (toggle), Copy day to tomorrow**.
  Each item calls the existing ViewModel handler.
- **Row 2 (centered):** date navigator — `‹  Sun · 21 Jun  ›`. Left chevron = previous day, right = next day,
  tapping the date label = jump to today. (Reuses `goToPreviousDay` / `goToNextDay` / `goToToday`.) When the
  selected date is not today, append a small "Today" text affordance.
- **Row 3:** `Eaten` (left) · **CalorieRing** (center) · `Goal` (right) — the existing `SummarySideMetric`
  composables and `CalorieRing`, unchanged.

`Quick calories` leaves the header — quick logging is reachable from the FAB's add flow (it is already an
add mode), so it is not lost.

### B. Body order & the "More" expander

Below the header, in order:

1. **Macro cards** (`MacroProgressRow`) — unchanged.
2. **`MessageBanner`** (transient feedback / undo) — unchanged.
3. **Meal diary** — the `SectionTitle("Meal diary")` + the redesigned `MealSectionCard`s (section C), plus the
   empty-diary start card when the diary is empty.
4. **"More" expander** — a tappable row (`More details` + a rotating chevron) that toggles a
   collapsed-by-default block containing every remaining secondary section, in this order: Day rating,
   Daily insights, Weekly plan strip, Advanced nutrition, Micronutrients, Water tracker, Health Connect sync,
   Food database preview.

Expanded/collapsed state is **UI-local** (`rememberSaveable { mutableStateOf(false) }` in `FoodScreen`) — no
ViewModel change needed. (A later slice may promote individual items, e.g. water, out of "More".)

### C. Meal-section cards

- Replace the first-letter avatar (`MealInitial`) with a **category icon**: a rounded-square tinted container
  (`surfaceVariant`) holding a Material icon chosen by meal type — Breakfast→`Icons.Outlined.BakeryDining`,
  Lunch→`Icons.Outlined.LunchDining`, Dinner→`Icons.Outlined.DinnerDining`, Snacks→`Icons.Outlined.Cookie`,
  default/custom→`Icons.Outlined.Restaurant`. A small `mealTypeIcon(id/title)` helper maps to the icon.
- The add button on each meal card becomes **coral** (`accent` background, `onAccent` content) with an
  `Icons.Default.Add` icon instead of the "+" text glyph.
- Title, summary, rating pill, and the entry rows are otherwise unchanged (entry-row photos arrive in Slice 3).

### D. Floating action button

Wrap the screen body in a `Scaffold` with a `floatingActionButton`. A coral
`FloatingActionButton` (`accent` / `onAccent`, `Icons.Default.Add`) opens the add-food flow via
`viewModel.openAddFood(defaultMealId)` where `defaultMealId` is the first meal section's id (the meal is
still changeable inside the flow). The FAB is hidden while the meal-detail screen or a bottom sheet is shown.

### E. Bottom-nav icons

- Add an `icon: ImageVector` to each `AppDestination` entry: Today→`Icons.Outlined.Today`,
  Food→`Icons.Outlined.Restaurant`, Training→`Icons.Outlined.FitnessCenter`, Health→`Icons.Outlined.MonitorHeart`.
- `AppNavGraph` renders `NavigationBarItem(icon = { Icon(destination.icon, contentDescription = destination.label) })`
  instead of the first-letter `Text`. Selected tint is the theme's coral (already wired); labels stay.

## Components / files

| File | Change |
|---|---|
| `gradle/libs.versions.toml` | Add `androidx-compose-material-icons-extended` library |
| `app/build.gradle.kts` | Add the icons-extended dependency |
| `app/src/main/java/com/musfit/ui/AppDestination.kt` | Add `icon: ImageVector` per destination |
| `app/src/main/java/com/musfit/ui/AppNavGraph.kt` | Use icons in `NavigationBarItem` |
| `app/src/main/java/com/musfit/ui/food/FoodScreen.kt` | Header rework, Scaffold+FAB, meal-card icons + coral add, "More" expander, body reorder |

ViewModel: no new state required (the "More" toggle is UI-local; the FAB and overflow reuse existing handlers).
If a small helper is added (e.g. `mealTypeIcon`), it lives in `FoodScreen.kt`.

## Testing & verification

- The change is mostly presentation/structure, so verification is build + visual. Existing ViewModel/repository
  tests must stay green (no logic change).
- `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug` green.
- Install and screenshot the Food tab (collapsed and expanded "More"), the meal-detail screen, and the four
  tabs to confirm the new nav icons. Confirm: meals sit directly under macros; secondary content is hidden
  until "More" is expanded; the FAB opens the add flow; category icons and the coral add buttons render.

## Risks & mitigations

- **`material-icons-extended` size** — it is large but standard; debug builds are unaffected and R8 strips
  unused icons in release. Acceptable.
- **Large edits in `FoodScreen.kt`** — the header and body are heavily restructured. Mitigate by moving the
  secondary sections into the "More" block without changing their internals, and by screenshot-diffing.
- **Scaffold nesting** — `FoodScreen` already runs inside the app `Scaffold` (bottom nav). Adding a screen-level
  `Scaffold` for the FAB must not double-pad or fight insets; alternatively place the FAB in a `Box` overlay
  aligned bottom-end. The plan will use a `Box` overlay to avoid nested-Scaffold inset issues.

## Definition of done

- Header shows title + overflow menu + centered date nav + ring; the ten-button strip is gone.
- Meal diary sits directly under the macros; all secondary sections are inside a collapsed-by-default "More".
- Meal cards use category icons and coral add buttons; a coral FAB opens the add flow.
- All four bottom-nav tabs show real Material icons.
- Build gate green; existing tests pass; before/after screenshots captured.
