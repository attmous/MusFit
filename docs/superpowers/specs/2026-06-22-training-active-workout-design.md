# MusFit Training — Active-workout logger redesign (Slice 2)

**Date:** 2026-06-22
**Status:** Approved design pending spec review, then plan
**Area:** Training active-workout UI (`ui/training/TrainingActiveWorkoutContent.kt`) + `TrainingViewModel`, new pure domain helpers (`domain/training`), and a grouping schema change for supersets (2b).

## Background

The active-workout logger is plain Material: a header (Back · "Log workout" · Discard/Finish + a Sets/Volume stat card), a **static** "Rest Timer: OFF" line, an add-exercise bar, and per-exercise set tables (SET · PREVIOUS · KG · REPS · RPE · ✓) with Add/Duplicate. The data model already supports set type (warmup/working), reps, weightKg, RPE, notes, completion, and a previous-set reference.

We redesign it **Hevy-style** on B1 + the Training **Indigo** accent, and add four features (all approved): a **live rest timer**, **PR badges**, a **plate calculator**, and **supersets**.

## Decomposition

- **Slice 2a — Re-skin + live rest timer + PR badges + plate calculator.** No schema change.
- **Slice 2b — Supersets.** Grouping data-model change (migration **v18**) + grouped UI.

## Slice 2a

### Re-skin
- **Top bar:** Back · workout title + an **elapsed workout timer** · Finish (Indigo); Discard moves into an overflow.
- **Exercise cards:** white `Surface` (B1), exercise name in Indigo, a ⋮ menu (remove exercise, add note).
- **Set table:** header SET · PREVIOUS · KG · REPS · ✓; rows with editable KG/REPS fields and an **Indigo ✓** to complete a set; Add set. **RPE + per-set notes move to an expandable per-set detail** (tap a row to reveal), not a permanent column.

### Live rest timer
- Extend `RestTimerState` with `remainingSeconds`. `TrainingViewModel` runs a `viewModelScope` coroutine that ticks `remainingSeconds` down once per second while a rest is active, hiding it at 0. Actions: `extendRest()` (+15 s) and `skipRest()`. Auto-start already fires on `toggleWorkoutSetCompletion`.
- UI: the Indigo rest bar shows `Rest · m:ss` + **+15s** + **Skip**.

### PR badges
- The repository exposes each exercise block's **pre-workout best estimated 1RM** (from completed history before this session) on `WorkoutExerciseBlock` (or via a lookup). A pure helper `isPersonalRecord(set, priorBest1RM)` flags a completed set whose est-1RM (`weightKg * (1 + reps/30)`) exceeds the prior best. UI renders a small Indigo **PR** chip on that set.

### Plate calculator
- New pure `domain/training/PlateCalculator`: `platesPerSide(totalKg, barKg = 20.0, plates = listOf(20.0,15.0,10.0,5.0,2.5,1.25))` → `List<Double>` (greedy per side; returns empty if `totalKg <= barKg`). Unit-tested.
- UI shows the plate breakdown line for the active set's entered weight.

## Slice 2b — Supersets

- **Schema (migration v18):** add a nullable `supersetGroup` (e.g. `TEXT`) to `workout_sets` **or** an exercise-grouping column on the session's exercises; group exercises share a group id, ordered A1/A2…
- **ViewModel:** group / ungroup exercises; maintain A1/A2 ordering; rest/logging flows within a group.
- **UI:** grouped exercise cards under a "SUPERSET A" label with an Indigo rail and A1/A2 tags.

## Architecture

- **TrainingViewModel:** rest-timer countdown coroutine + `extendRest`/`skipRest`; expose per-exercise pre-workout best 1RM for PR badges.
- **Domain (pure, unit-tested):** `PlateCalculator`; a small PR helper (`isPersonalRecord` / est-1RM).
- **Repository:** provide the pre-workout best est-1RM per exercise in `ActiveWorkoutDetail` (reusing the existing completed-set history queries).

## Testing & verification

- `PlateCalculator` unit tests (e.g. 100 kg → [20,20,5,1.25] per side with a 20 kg bar; ≤ bar → empty; odd remainder handled).
- PR-helper unit tests (beats / ties / below prior best).
- A `TrainingViewModel` rest-timer test (advance the test dispatcher, assert countdown + skip/extend).
- Existing training tests stay green.
- `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug` green per slice; on-device screenshots of the logger (rest timer running, a PR badge, the plate line).

## Non-goals

- Supersets are **2b** (kept out of 2a).
- No change to the set/exercise persistence beyond the rest-timer UI state and exposing the PR best (2a) and the grouping column (2b).
- Dark mode.

## Definition of done (design)

The active-workout logger redesign is specced: a Hevy-style B1 + Indigo logger with a top-bar elapsed timer, clean set table, expandable RPE/notes, a live rest timer, PR badges, and a plate calculator (Slice 2a), plus supersets via a grouping data-model change (Slice 2b).
