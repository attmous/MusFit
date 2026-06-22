# Training Home Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Polish the Training landing screen so routines and workout start actions are the primary experience while Quick set becomes secondary.

**Architecture:** Keep the existing `TrainingScreen`, `TrainingRoutineContent`, and `TrainingViewModel` contracts. Add small pure UI helpers in `TrainingRoutineContent.kt` for testable disclosure/action-label behavior, then restyle Compose surfaces around the existing callbacks.

**Tech Stack:** Android Kotlin, Jetpack Compose Material 3, Hilt ViewModels, Kotlin Flow/coroutines, JUnit unit tests.

---

### Task 1: Test Home Disclosure And Routine Actions

**Files:**
- Modify: `app/src/test/java/com/musfit/ui/training/TrainingHomeContentTest.kt`
- Modify: `app/src/main/java/com/musfit/ui/training/TrainingRoutineContent.kt`

- [ ] **Step 1: Write the failing tests**

Create `TrainingHomeContentTest` with tests for `nextQuickLogExpanded` and `routineCardActions`.

```kotlin
@Test
fun nextQuickLogExpanded_togglesPanelState() {
    assertEquals(true, nextQuickLogExpanded(current = false))
    assertEquals(false, nextQuickLogExpanded(current = true))
}

@Test
fun routineCardActions_keepsStarterRoutineReadOnlyButStartable() {
    val actions = routineCardActions(isStarter = true)
    assertEquals(listOf("Start", "Duplicate"), actions)
}
```

- [ ] **Step 2: Run the focused test to verify RED**

```powershell
. 'C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1'; .\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.training.TrainingHomeContentTest" --no-daemon --console=plain
```

Expected: compile failure because the helper functions do not exist.

- [ ] **Step 3: Implement minimal helpers**

Add internal helpers in `TrainingRoutineContent.kt`:

```kotlin
internal fun nextQuickLogExpanded(current: Boolean): Boolean = !current

internal fun routineCardActions(isStarter: Boolean): List<String> =
    if (isStarter) {
        listOf("Start", "Duplicate")
    } else {
        listOf("Start", "Edit", "Duplicate", "Delete")
    }
```

- [ ] **Step 4: Run the focused test to verify GREEN**

Run the same focused command and expect success.

### Task 2: Compact Training Screen Header And Quick Log

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/training/TrainingScreen.kt`

- [ ] **Step 1: Replace the always-visible Quick set card**

Move `QuickSetLoggerCard` behind a local `quickLogExpanded` state in `TrainingScreen`. Show a compact `TextButton` or `Button` row labeled `Quick log`; show the existing card only when expanded.

- [ ] **Step 2: Keep active workout resume above routines**

Keep `state.activeWorkoutSummary` above the section tabs, but render it as a dense banner with title, completed sets, volume, and a Resume action.

- [ ] **Step 3: Run focused Training ViewModel tests**

```powershell
. 'C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1'; .\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.training.TrainingViewModelTest" --no-daemon --console=plain
```

Expected: success because callbacks and state contracts remain unchanged.

### Task 3: Restyle Routine Cards

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/training/TrainingRoutineContent.kt`

- [ ] **Step 1: Make start empty workout full-width**

Render `Start empty workout` as a full-width primary action above routine cards and keep `New routine` as a secondary compact action.

- [ ] **Step 2: Make routine cards scan-friendly**

Use compact cards with title, `N exercises - M sets`, full-width `Start`, and secondary actions derived from `routineCardActions`.

- [ ] **Step 3: Run focused helper test**

```powershell
. 'C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1'; .\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.training.TrainingHomeContentTest" --no-daemon --console=plain
```

Expected: success.

### Task 4: Final Verification And Commit

**Files:**
- Review all modified files.

- [ ] **Step 1: Run full verification**

```powershell
. 'C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1'; .\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain
```

Expected: success.

- [ ] **Step 2: Inspect diff scope**

```powershell
git diff --name-status
```

Expected: only Training UI/test files and the two Training docs.

- [ ] **Step 3: Commit**

```powershell
git add docs/superpowers/specs/2026-06-22-training-home-cleanup-design.md docs/superpowers/plans/2026-06-22-training-home-cleanup.md app/src/main/java/com/musfit/ui/training/TrainingScreen.kt app/src/main/java/com/musfit/ui/training/TrainingRoutineContent.kt app/src/test/java/com/musfit/ui/training/TrainingHomeContentTest.kt
git commit -m "feat: polish training home layout"
```
