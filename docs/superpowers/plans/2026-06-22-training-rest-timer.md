# Training Rest Timer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a Hevy-style rest timer for active workouts that starts when a valid set is completed and can be adjusted, paused, resumed, or skipped.

**Architecture:** Keep timer state and actions in `TrainingViewModel` so the timer survives recomposition and stays tied to workout lifecycle actions. Use a small Compose tick effect to call the ViewModel once per second while the timer is visible and running.

**Tech Stack:** Android Kotlin, Jetpack Compose Material 3, Kotlin coroutines, Flow, JUnit ViewModel tests.

---

### Task 1: ViewModel Timer Behavior

**Files:**
- Modify: `app/src/test/java/com/musfit/ui/training/TrainingViewModelTest.kt`
- Modify: `app/src/main/java/com/musfit/ui/training/TrainingViewModel.kt`

- [ ] Write failing tests that completing a valid active set starts a running timer at 120 seconds, ticking reduces remaining time, pause prevents ticking, resume continues ticking, add/subtract time clamps safely, skip hides the timer, and finishing/discarding clears it.
- [ ] Run `.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.training.TrainingViewModelTest" --no-daemon --console=plain` and verify the new tests fail because timer actions do not exist yet.
- [ ] Add `remainingSeconds` and `isRunning` to `RestTimerState`, add `tickRestTimer`, `pauseRestTimer`, `resumeRestTimer`, `skipRestTimer`, and `adjustRestTimerSeconds` actions, and start the timer when `toggleWorkoutSetCompletion(..., completed = true)` succeeds.
- [ ] Run the focused ViewModel test command again and verify it passes.

### Task 2: Active Workout Timer Controls

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/training/TrainingActiveWorkoutContent.kt`
- Modify: `app/src/main/java/com/musfit/ui/training/TrainingScreen.kt`
- Modify: `app/src/test/java/com/musfit/ui/training/TrainingActiveWorkoutContentTest.kt`

- [ ] Write failing display-helper tests for timer labels and visibility text.
- [ ] Replace passive rest timer text with a compact timer banner that shows remaining time plus `-15s`, pause/resume, `+15s`, and skip controls.
- [ ] Remove duplicated exercise-local timer rows so the active workout layout stays clean.
- [ ] Run active workout content tests and full verification.

### Task 3: Device Verification

**Files:**
- Build output: `app/build/outputs/apk/debug/app-debug.apk`

- [ ] Run `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain`.
- [ ] Install the debug APK with `adb install -r app\build\outputs\apk\debug\app-debug.apk`.
- [ ] Launch `com.musfit` and confirm no `AndroidRuntime` crash is emitted.
