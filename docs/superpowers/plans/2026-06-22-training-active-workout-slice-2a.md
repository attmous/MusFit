# Training Active-Workout — Slice 2a Plan (re-skin + rest timer + PR + plates)

> **For agentic workers:** Use superpowers:executing-plans. Steps use checkbox (`- [ ]`).

**Goal:** Redesign the active-workout logger to a Hevy-style B1 + Indigo screen, and add a live rest timer, PR badges, and a plate calculator. No schema change (supersets are Slice 2b).

**Architecture:** New pure `domain/training` helpers (`PlateCalculator`, est-1RM PR check) — unit-tested. `TrainingViewModel` gets a real rest-timer countdown + extend/skip. The repository adds each exercise block's pre-workout best est-1RM for PR badges. `TrainingActiveWorkoutContent.kt` is re-skinned onto `MusFitTheme` + the Training Indigo accent (`tabAccentFor(AppDestination.Training)`), with RPE/notes in an expandable per-set row.

---

### Task 1: `PlateCalculator` (pure, TDD)
**Files:** new `domain/training/PlateCalculator.kt`, test `domain/training/PlateCalculatorTest.kt`.
- [ ] **Step 1 — failing tests:** 100 kg with 20 kg bar → `[20.0, 20.0]` per side (40/side); 60 kg → `[20.0]`; ≤ bar (e.g. 20, 15) → empty; 102.5 → `[20.0, 20.0, 1.25]`; un-loadable remainder rounds down (best-effort).
- [ ] **Step 2 — implement:**
```kotlin
package com.musfit.domain.training

object PlateCalculator {
    val DEFAULT_PLATES = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)

    fun platesPerSide(totalKg: Double, barKg: Double = 20.0, plates: List<Double> = DEFAULT_PLATES): List<Double> {
        if (totalKg <= barKg) return emptyList()
        var perSide = (totalKg - barKg) / 2.0
        val result = mutableListOf<Double>()
        for (plate in plates.sortedDescending()) {
            while (perSide >= plate - 1e-6) {
                result += plate
                perSide -= plate
            }
        }
        return result
    }
}
```
- [ ] **Step 3 — run** `testDebugUnitTest --tests "com.musfit.domain.training.PlateCalculatorTest"` → PASS.

### Task 2: Pre-workout best 1RM for PR badges
**Files:** `data/repository/TrainingRepository.kt`, `ui/training/TrainingActiveWorkoutContent.kt`, test `data/repository/LocalTrainingRepositoryTest.kt`.
- [ ] **Step 1.** Add `val priorBestEstimatedOneRepMaxKg: Double = 0.0` to `WorkoutExerciseBlock` (TrainingRepository.kt:133).
- [ ] **Step 2.** In `observeActiveWorkoutDetail`'s block mapping, compute each exercise's best est-1RM from **completed** sets in sessions started **before** the active session (reuse the existing completed-set/history queries + `WorkoutCalculator.estimatedOneRepMax`); set `priorBestEstimatedOneRepMaxKg`.
- [ ] **Step 3.** Add `val isPr: Boolean` to `WorkoutSetRowDisplay`; in `formatWorkoutSetRowsForDisplay(sets, priorBest1RM)`, set `isPr = set.completed && set.weightKg != null && set.reps != null && WorkoutCalculator.estimatedOneRepMax(set.weightKg, set.reps) > priorBest1RM + 1e-6`. (Thread `priorBest1RM` from the block.)
- [ ] **Step 4.** Repository test: a completed set heavier than history flags `priorBestEstimatedOneRepMaxKg` correctly.

### Task 3: Live rest timer (ViewModel)
**Files:** `ui/training/TrainingViewModel.kt`, test `ui/training/TrainingViewModelTest.kt`.
- [ ] **Step 1.** Extend `RestTimerState` with `val remainingSeconds: Int = durationSeconds` (and keep `durationSeconds`).
- [ ] **Step 2.** Add a `restTimerJob: Job?`. On set completion (VM:504-508), start the timer: set `RestTimerState(isVisible = true, sourceSetId = setId, remainingSeconds = 120)` and launch a coroutine that, while `remainingSeconds > 0`, `delay(1_000)` and decrements; at 0 sets `isVisible = false`. Use the injected clock/dispatcher already present so it's test-controllable. Add `fun extendRest()` (+15 to remaining) and `fun skipRest()` (cancel job, `isVisible=false`).
- [ ] **Step 3.** Test (advance the `StandardTestDispatcher`): complete a set → `remainingSeconds == 120`; advance 3 s → `117`; `skipRest()` → not visible; `extendRest()` adds 15.

### Task 4: Re-skin `TrainingActiveWorkoutContent`
**Files:** `ui/training/TrainingActiveWorkoutContent.kt` (+ pass `onExtendRest`/`onSkipRest` through `TrainingScreen.kt`).
- [ ] **Step 1.** `val accent = tabAccentFor(AppDestination.Training)`. Replace all `MaterialTheme.colorScheme.*` with `MusFitTheme.colors.*` + `accent.*`; `RoundedCornerShape(..)` → `MusFitTheme.shapes`.
- [ ] **Step 2 — top bar:** Back · title + **elapsed timer** (from `workout.startedAtEpochMillis`, ticking via a `LaunchedEffect` 1-s loop) · **Finish** (Indigo). Discard → an overflow ⋮.
- [ ] **Step 3 — rest bar:** Indigo `accent.container` bar showing `Rest · ${remainingSeconds.toMinSec()}` with **+15s** (`onExtendRest`) and **Skip** (`onSkipRest`); hidden when not visible.
- [ ] **Step 4 — exercise card:** white `Surface`, Indigo name; set-table header SET · PREVIOUS · KG · REPS · ✓ (drop the RPE column); rows with editable KG/REPS + an Indigo ✓ box; a small Indigo **PR** chip when `row.isPr`; a tappable chevron to expand a per-set detail row holding **RPE** + **notes** fields; under the focused/last set, a **plate line** `Plates · ${PlateCalculator.platesPerSide(weight).joinToString(" + ")} / side` (hidden when empty). Keep Add set; move Duplicate into the set's expandable detail.
- [ ] **Step 5 — wiring:** `TrainingScreen.kt` passes `onExtendRest = viewModel::extendRest`, `onSkipRest = viewModel::skipRest` into `TrainingActiveWorkoutContent`.

### Task 5: Verify + commit
- [ ] `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug` → green.
- [ ] On-device: start a workout → Indigo logger; complete a set → rest bar counts down (+15s/Skip work); a heavier-than-history set shows PR; the plate line matches the weight.
- [ ] Commit (`feat(training): Hevy active-workout logger — rest timer, PR badges, plates`).

## Notes
- RPE stays in the data model; it just moves from a table column to the expandable per-set detail.
- Elapsed + rest countdown tick in the UI/VM respectively; both stop when the screen/workout closes.
