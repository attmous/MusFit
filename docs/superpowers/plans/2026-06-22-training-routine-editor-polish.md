# Training Routine Editor Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Polish the Training routine editor and add-exercise picker without changing routine persistence behavior.

**Architecture:** Keep `TrainingRoutineEditor` as the Compose entrypoint and add pure helper functions in `TrainingRoutineContent.kt` for testable picker filtering. The UI uses the existing callbacks from `TrainingViewModel`.

**Tech Stack:** Android Kotlin, Jetpack Compose Material 3, JUnit unit tests.

---

### Task 1: Picker Helper Tests

**Files:**
- Modify: `app/src/test/java/com/musfit/ui/training/TrainingHomeContentTest.kt`
- Modify: `app/src/main/java/com/musfit/ui/training/TrainingRoutineContent.kt`

- [ ] **Step 1: Write failing tests**

Add tests for `routineExercisePickerSuggestions`.

- [ ] **Step 2: Verify RED**

```powershell
. 'C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1'; .\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.training.TrainingHomeContentTest" --no-daemon --console=plain
```

Expected: compile failure because `routineExercisePickerSuggestions` does not exist.

- [ ] **Step 3: Implement helper**

Add `routineExercisePickerSuggestions(exercises, selectedExerciseIds, query, expanded)` returning no suggestions while collapsed, three suggestions when expanded with blank query, and six filtered suggestions when query is nonblank.

- [ ] **Step 4: Verify GREEN**

Run the same focused command and expect success.

### Task 2: Routine Editor UI

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/training/TrainingRoutineContent.kt`

- [ ] **Step 1: Replace editor header**

Use a compact row with `Cancel`, title, and `Save`.

- [ ] **Step 2: Replace dropdown picker**

Use an expandable Surface, search field, and short suggestion list from `routineExercisePickerSuggestions`.

- [ ] **Step 3: Replace text move/remove controls**

Use icon buttons for move up, move down, and remove while keeping content descriptions.

- [ ] **Step 4: Run focused tests**

```powershell
. 'C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1'; .\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.training.TrainingHomeContentTest" --tests "com.musfit.ui.training.TrainingViewModelTest" --no-daemon --console=plain
```

Expected: success.

### Task 3: Final Verification And Commit

**Files:**
- Review all modified files.

- [ ] **Step 1: Run full verification**

```powershell
. 'C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1'; .\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain
```

Expected: success.

- [ ] **Step 2: Commit**

```powershell
git add docs/superpowers/specs/2026-06-22-training-routine-editor-polish-design.md docs/superpowers/plans/2026-06-22-training-routine-editor-polish.md app/src/main/java/com/musfit/ui/training/TrainingRoutineContent.kt app/src/test/java/com/musfit/ui/training/TrainingHomeContentTest.kt
git commit -m "feat: polish training routine editor"
```
