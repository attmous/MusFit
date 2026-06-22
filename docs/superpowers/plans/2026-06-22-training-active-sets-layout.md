# Training Active Sets Layout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the active workout per-set card editor with a compact Hevy-inspired set table while keeping MusFit's existing Training behavior.

**Architecture:** Keep the state and repository contracts unchanged. Add small pure UI formatting helpers in `TrainingActiveWorkoutContent.kt` for set labels and compact value display, then rebuild the Compose active workout exercise block around a stable column layout.

**Tech Stack:** Android Kotlin, Jetpack Compose Material 3, Hilt ViewModels, Kotlin Flow/coroutines, JUnit unit tests.

---

### Task 1: Set Row Formatting

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/training/TrainingActiveWorkoutContent.kt`
- Create: `app/src/test/java/com/musfit/ui/training/TrainingActiveWorkoutContentTest.kt`

- [ ] **Step 1: Write failing tests**

Add tests for `formatWorkoutSetRowsForDisplay()` using two warm-up sets followed by working sets. Assert visible labels are `W`, `W`, `1`, `2`, previous fallback is `-`, and compact numeric values do not show unnecessary `.0`.

- [ ] **Step 2: Verify tests fail**

Run:

```powershell
. 'C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1'; .\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.training.TrainingActiveWorkoutContentTest" --no-daemon --console=plain
```

Expected: compile failure because the helper does not exist.

- [ ] **Step 3: Implement formatting helper**

Add an internal `WorkoutSetRowDisplay` model and `formatWorkoutSetRowsForDisplay(sets)` helper in `TrainingActiveWorkoutContent.kt`.

- [ ] **Step 4: Verify tests pass**

Run the same focused test command and expect success.

### Task 2: Active Workout Layout

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/training/TrainingActiveWorkoutContent.kt`

- [ ] **Step 1: Rebuild top active workout header**

Use a compact header with back action, title, completed set/volume summary, `Finish`, and `Discard`.

- [ ] **Step 2: Rebuild exercise blocks**

Use exercise heading, target/notes line, rest timer status, compact set table header, alternating rows, inline kg/reps/rpe fields, check button, secondary save/delete row, and full-width `+ Add Set`.

- [ ] **Step 3: Preserve behavior hooks**

Keep existing callbacks: `onAddSet`, `onDuplicateSet`, `onUpdateSet`, `onDeleteSet`, and `onToggleSet`.

### Task 3: Verification

**Files:**
- Review modified files.

- [ ] **Step 1: Run focused tests**

Run:

```powershell
. 'C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1'; .\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.training.TrainingActiveWorkoutContentTest" --tests "com.musfit.ui.training.TrainingViewModelTest" --no-daemon --console=plain
```

Expected: success.

- [ ] **Step 2: Run full verification**

Run:

```powershell
. 'C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1'; .\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain
```

Expected: success.

- [ ] **Step 3: Inspect diff and commit**

Run `git diff --name-status`, confirm scope is Training UI/test/doc only, then commit:

```powershell
git add app/src/main/java/com/musfit/ui/training/TrainingActiveWorkoutContent.kt app/src/test/java/com/musfit/ui/training/TrainingActiveWorkoutContentTest.kt docs/superpowers/specs/2026-06-22-training-active-sets-layout-design.md docs/superpowers/plans/2026-06-22-training-active-sets-layout.md
git commit -m "feat: polish active workout set layout"
```
