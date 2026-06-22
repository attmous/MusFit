# Training Redesign — Slice 1 (Per-tab color + Hevy overview) Plan

> **For agentic workers:** Use superpowers:executing-plans. Steps use checkbox (`- [ ]`).

**Goal:** Add the per-tab accent system (Training = Indigo) with a color-coded bottom nav, and re-skin the Training overview into a decluttered, routines-first Hevy-style home on B1 tokens.

**Architecture:** New `TabAccent` token + `tabAccentFor(AppDestination)` in the theme. `AppNavGraph` colors the active nav item per tab. `TrainingScreen.kt` + the Routines/Exercises/History/Progress content move from plain Material onto `MusFitTheme.colors`/`.shapes` + the Indigo accent. UI-only — `TrainingViewModel`/repository unchanged.

---

### Task 1: Per-tab accent tokens (TDD for the mapping)
**Files:** `ui/theme/Color.kt`, new `ui/theme/TabAccent.kt`, test `ui/theme/TabAccentTest.kt`.
- [ ] **Step 1 — colors.** Add to `Color.kt`: `val Indigo = Color(0xFF3D5AF1)`, `val IndigoInk = Color(0xFF1E2A78)`, `val IndigoContainer = Color(0xFFE4E7FD)`, `val Teal = Color(0xFF0E9594)`, `val TealInk = Color(0xFF0B5E5D)`, `val TealContainer = Color(0xFFD9F0EF)`.
- [ ] **Step 2 — token + mapping.** New `TabAccent.kt`:
```kotlin
package com.musfit.ui.theme

import androidx.compose.ui.graphics.Color
import com.musfit.ui.AppDestination

data class TabAccent(val color: Color, val onColor: Color, val container: Color, val onContainer: Color)

fun tabAccentFor(destination: AppDestination): TabAccent = when (destination) {
    AppDestination.Today -> TabAccent(Coral, CardWhite, CoralContainer, CoralInk)
    AppDestination.Food -> TabAccent(Emerald, CardWhite, PositiveContainer, EmeraldInk)
    AppDestination.Training -> TabAccent(Indigo, CardWhite, IndigoContainer, IndigoInk)
    AppDestination.Health -> TabAccent(Teal, CardWhite, TealContainer, TealInk)
}
```
(Confirm `Coral`, `CoralContainer`, `CoralInk`, `Emerald`, `PositiveContainer`, `EmeraldInk`, `CardWhite` already exist in `Color.kt`.)
- [ ] **Step 3 — test.** `TabAccentTest`: assert `tabAccentFor(Training).color == Indigo`, `tabAccentFor(Food).color == Emerald`, `tabAccentFor(Today).color == Coral`, `tabAccentFor(Health).color == Teal`.
- [ ] **Step 4 — run** `testDebugUnitTest --tests "com.musfit.ui.theme.TabAccentTest"` → PASS.

### Task 2: Color-code the bottom nav
**Files:** `ui/AppNavGraph.kt`.
- [ ] In the `NavigationBarItem` loop, set per-tab selected colors:
```kotlin
val accent = tabAccentFor(destination)
NavigationBarItem(
    selected = currentRoute == destination.route,
    onClick = { ... },
    label = { Text(destination.label) },
    icon = { Icon(destination.icon, contentDescription = destination.label) },
    colors = NavigationBarItemDefaults.colors(
        selectedIconColor = accent.onContainer,
        selectedTextColor = accent.color,
        indicatorColor = accent.container,
    ),
)
```
Add imports `NavigationBarItemDefaults` and `com.musfit.ui.theme.tabAccentFor`.

### Task 3: Re-skin the Training overview (Hevy home)
**Files:** `ui/training/TrainingScreen.kt`, `ui/training/TrainingRoutineContent.kt`.

- [ ] **Step 1 — accent handle.** At the top of `TrainingScreen`, `val accent = tabAccentFor(AppDestination.Training)`. Wrap the screen `Column` background with `MusFitTheme.colors.background`.
- [ ] **Step 2 — header.** Replace the bare `Text("Training", headlineSmall)` with a header: title (`headlineMedium`, bold, `MusFitTheme.colors.onSurface`) + a weekly-stat subtitle computed in-UI from `state.workoutHistory` (count + summed `totalVolumeKg` for sessions whose `startedAtEpochMillis` is within the last 7 days → "This week · N workouts · X kg"; if none, "No workouts yet"). Optional History shortcut icon (Indigo) that calls `viewModel.selectSection(TrainingSection.History)`.
- [ ] **Step 3 — remove Quick-set.** Delete the `QuickSetLoggerCard(...)` call (TrainingScreen.kt:92-99) and the `QuickSetLoggerCard` composable (218-280). (Leave the VM quick-set funcs; unused.)
- [ ] **Step 4 — resume banner.** Replace the `state.activeWorkoutSummary?.let { ElevatedCard {...} }` (77-91) with an Indigo resume banner: `Surface(color = accent.container, shape = MusFitTheme.shapes.large)` containing a play icon (`accent.color`), "Workout in progress" (`accent.onContainer`, bold) + "${completedSetCount} sets · ${volume} kg" + a Resume button (`accent.color` bg) → `viewModel.resumeActiveWorkout()`.
- [ ] **Step 5 — section chips.** Replace `SingleChoiceSegmentedButtonRow` (107-117) with a `Row(horizontalScroll)` of chip `Surface`s — selected chip `color = accent.container`, text `accent.onContainer`, bold; unselected `surface` with `outline` border + `onSurfaceVariant` text. Each → `viewModel.selectSection(section)`.
- [ ] **Step 6 — message.** Re-skin `state.message` text to `MusFitTheme.colors.onSurfaceVariant`.
- [ ] **Step 7 — routine content.** Rewrite `TrainingRoutineContent`: a prominent **Start empty workout** `Button` (full width, `accent.color`) → `onStartBlank`; a "YOUR ROUTINES" label + per-routine white `Surface` cards (`MusFitTheme.colors.surface`, `shapes.large`) with name (`titleMedium`, bold), "${exerciseCount} exercises · ${targetSetCount} sets" (`onSurfaceVariant`), a **Start** button (`accent.color`) + a ⋮ overflow `DropdownMenu` (Edit / Duplicate / Delete — Edit/Delete hidden when `isStarter`); a trailing **New routine** outlined row → `onEditRoutine(null)`. Pass `accent` into `TrainingRoutineContent` as a param (`accent: TabAccent`). Drop muscle tags (not in `RoutineSummary`).
- [ ] **Step 8 — other sections.** Re-skin `TrainingExerciseLibraryContent` (in TrainingScreen.kt), `TrainingHistoryContent`, `TrainingProgressContent`, `TrainingRoutineEditor`, and `FilterChipRow` by swapping `MaterialTheme.colorScheme.*` → `MusFitTheme.colors.*`, `Card`/`ElevatedCard`/`ListItem` → `Surface`/`Column` with `MusFitTheme.colors.surface` + `shapes`, and primary buttons/active chips → `accent.color`. Keep all callbacks/data identical. (Pattern-level; match the Food/Today card style.)

### Task 4: Verify + commit
- [ ] `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug` → green.
- [ ] On-device: Training home shows Indigo accent, decluttered routines-first layout, color-coded nav (Training tab indigo).
- [ ] Commit (`feat(training): per-tab color coding + Hevy-style overview re-skin`).

## Notes
- Weekly stat + section chips are presentational; no VM change.
- Muscle tags on routine cards are deferred (not in `RoutineSummary`); could be added later via a routine→exercise join.
