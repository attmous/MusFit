# Training Workout Finish Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add confirmation and review flow for finishing or discarding an active Training workout.

**Architecture:** Use existing `TrainingViewModel` state flags and repository methods. Add request/cancel actions for confirmations, keep existing finish/discard methods as confirmation executors, wire Material 3 dialogs in `TrainingScreen`, and polish `TrainingHistoryContent` using existing history data.

**Tech Stack:** Android Kotlin, Jetpack Compose Material 3, Hilt ViewModels, Kotlin Flow/coroutines, JUnit unit tests.

---

### Task 1: ViewModel Confirmation Tests

**Files:**
- Modify: `app/src/test/java/com/musfit/ui/training/TrainingViewModelTest.kt`
- Modify: `app/src/main/java/com/musfit/ui/training/TrainingViewModel.kt`

- [ ] **Step 1: Write failing tests**

Add tests for opening/cancelling finish and discard confirmations, finishing into History detail, and discarding back to Routines.

- [ ] **Step 2: Run focused test to verify RED**

```powershell
. 'C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1'; .\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.training.TrainingViewModelTest" --no-daemon --console=plain
```

Expected: compile failure because request/cancel confirmation actions do not exist.

- [ ] **Step 3: Implement ViewModel actions**

Add `requestFinishActiveWorkout`, `cancelFinishActiveWorkout`, `requestDiscardActiveWorkout`, and `cancelDiscardActiveWorkout`. Update `finishActiveWorkout` to close the active route, select History, and load `getWorkoutHistoryDetail(sessionId)` after finishing. Update `discardActiveWorkout` to close the active route and return to Routines.

- [ ] **Step 4: Run focused test to verify GREEN**

Run the same focused command and expect success.

### Task 2: Compose Confirmation Dialogs

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/training/TrainingScreen.kt`

- [ ] **Step 1: Wire active workout callbacks**

Pass Finish and Discard button taps to the new request methods.

- [ ] **Step 2: Render Material 3 dialogs**

Render one finish confirmation dialog when `state.finishConfirmationOpen` is true and one discard confirmation dialog when `state.discardConfirmationOpen` is true. Confirm buttons call the existing executor methods.

- [ ] **Step 3: Run focused compile/test**

```powershell
. 'C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1'; .\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.training.TrainingViewModelTest" --no-daemon --console=plain
```

Expected: success.

### Task 3: History Detail Polish

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/training/TrainingHistoryContent.kt`

- [ ] **Step 1: Add compact detail summary**

Replace the plain detail text with a compact summary card showing title, set count, volume, and duration when `endedAtEpochMillis` is available.

- [ ] **Step 2: Render exercise set rows**

For each exercise block, render the exercise name and compact set rows containing set type, weight, reps, and optional RPE.

- [ ] **Step 3: Run full verification**

```powershell
. 'C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1'; .\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain
```

Expected: success.

### Task 4: Commit And Deploy

**Files:**
- Review all modified files.

- [ ] **Step 1: Inspect diff scope**

```powershell
git diff --name-status
```

Expected: only Training UI/ViewModel/test files and Training docs.

- [ ] **Step 2: Commit**

```powershell
git add docs/superpowers/specs/2026-06-22-training-workout-finish-flow-design.md docs/superpowers/plans/2026-06-22-training-workout-finish-flow.md app/src/main/java/com/musfit/ui/training/TrainingViewModel.kt app/src/main/java/com/musfit/ui/training/TrainingScreen.kt app/src/main/java/com/musfit/ui/training/TrainingHistoryContent.kt app/src/test/java/com/musfit/ui/training/TrainingViewModelTest.kt
git commit -m "feat: add training workout finish flow"
```

- [ ] **Step 3: Deploy debug APK**

```powershell
. 'C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1'; adb install -r app\build\outputs\apk\debug\app-debug.apk; adb shell monkey -p com.musfit -c android.intent.category.LAUNCHER 1
```
