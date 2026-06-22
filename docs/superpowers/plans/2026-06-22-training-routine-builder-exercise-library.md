# Training Routine Builder Exercise Library Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish the next Training slice by making routines easier to shape and the exercise library useful for search, filters, and custom exercises.

**Architecture:** Training V1 already has routine CRUD, starter routine protection, active workout logging, history, and progress. This slice extends the existing Training repository/ViewModel/Compose flow with custom exercise creation, derived exercise library filters, and routine exercise reordering without adding a new Room table or cross-app dependency.

**Tech Stack:** Android Kotlin, Jetpack Compose Material 3, Hilt ViewModels, Room, Kotlin Flow/coroutines, JUnit/Robolectric unit tests.

---

### Task 1: Repository Custom Exercise Creation

**Files:**
- Modify: `app/src/main/java/com/musfit/data/repository/TrainingRepository.kt`
- Modify: `app/src/main/java/com/musfit/data/local/dao/TrainingDao.kt`
- Test: `app/src/test/java/com/musfit/data/repository/LocalTrainingRepositoryTest.kt`

- [ ] **Step 1: Write failing repository tests**

Add tests that call `createCustomExercise(ExerciseInput(...))`, assert trimmed fields persist as a custom exercise, and assert creating an exercise with an existing starter name returns the existing exercise id instead of duplicating it.

- [ ] **Step 2: Verify repository tests fail**

Run:

```powershell
. 'C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1'; .\gradlew.bat testDebugUnitTest --tests "com.musfit.data.repository.LocalTrainingRepositoryTest" --no-daemon --console=plain
```

Expected: compile failure because `ExerciseInput` and `createCustomExercise` do not exist.

- [ ] **Step 3: Implement minimal repository support**

Add `ExerciseInput`, add `TrainingRepository.createCustomExercise`, add a DAO lookup for normalized exercise names, and implement `LocalTrainingRepository.createCustomExercise` as local-first persistence with trimmed fields and duplicate reuse.

- [ ] **Step 4: Verify repository tests pass**

Run the same `LocalTrainingRepositoryTest` command and expect success.

### Task 2: ViewModel Exercise Library State

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/training/TrainingViewModel.kt`
- Test: `app/src/test/java/com/musfit/ui/training/TrainingViewModelTest.kt`

- [ ] **Step 1: Write failing ViewModel tests**

Add tests for exercise search/filter state and custom exercise save:
- search query narrows `visibleExercises`
- equipment/muscle filters combine with the query
- saving a valid custom exercise calls the repository, clears the form, and selects the Exercises section

- [ ] **Step 2: Verify ViewModel tests fail**

Run:

```powershell
. 'C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1'; .\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.training.TrainingViewModelTest" --no-daemon --console=plain
```

Expected: compile failure because the new state and ViewModel actions do not exist.

- [ ] **Step 3: Implement minimal ViewModel support**

Add `visibleExercises`, `exerciseSearchQuery`, `exerciseMuscleFilter`, `exerciseEquipmentFilter`, and `ExerciseEditorState`. Add actions to change filters, open/close the custom exercise editor, update fields, and save through `TrainingRepository.createCustomExercise`.

- [ ] **Step 4: Verify ViewModel tests pass**

Run the same `TrainingViewModelTest` command and expect success.

### Task 3: Routine Exercise Reordering

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/training/TrainingViewModel.kt`
- Modify: `app/src/main/java/com/musfit/ui/training/TrainingRoutineContent.kt`
- Test: `app/src/test/java/com/musfit/ui/training/TrainingViewModelTest.kt`

- [ ] **Step 1: Write failing reorder test**

Add a test that opens a new routine, adds two exercises, moves the second exercise up, saves, and verifies `RoutineInput.exercises` order is the moved order.

- [ ] **Step 2: Verify reorder test fails**

Run `TrainingViewModelTest` and expect a compile failure because `moveRoutineExerciseUp` or `moveRoutineExerciseDown` does not exist.

- [ ] **Step 3: Implement reorder actions and UI controls**

Add bounds-checked `moveRoutineExerciseUp(index)` and `moveRoutineExerciseDown(index)` to `TrainingViewModel`. Add compact Up/Down text buttons in each routine editor exercise card.

- [ ] **Step 4: Verify reorder tests pass**

Run `TrainingViewModelTest` and expect success.

### Task 4: Compose Exercise Library Polish

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/training/TrainingScreen.kt`
- Modify: `app/src/main/java/com/musfit/ui/training/TrainingRoutineContent.kt`

- [ ] **Step 1: Wire exercise library controls**

Replace the static exercise preview with an exercise library section that includes search, equipment/muscle filter chips, custom exercise form, and dense exercise rows showing equipment, muscles, and custom/library source.

- [ ] **Step 2: Wire routine editor add search**

Use the current exercise list with a small search field in the routine editor add area so long libraries remain usable.

- [ ] **Step 3: Run focused compile/test**

Run:

```powershell
. 'C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1'; .\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.training.TrainingViewModelTest" --tests "com.musfit.data.repository.LocalTrainingRepositoryTest" --no-daemon --console=plain
```

Expected: success.

### Task 5: Final Verification

**Files:**
- Review all modified files.

- [ ] **Step 1: Run full verification**

Run:

```powershell
. 'C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1'; .\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain
```

Expected: success.

- [ ] **Step 2: Inspect diff scope**

Run `git diff --name-status` and verify the diff is limited to Training code/tests and the plan document.

- [ ] **Step 3: Commit**

Commit with:

```powershell
git add app/src/main/java/com/musfit/data/local/dao/TrainingDao.kt app/src/main/java/com/musfit/data/repository/TrainingRepository.kt app/src/main/java/com/musfit/ui/training/TrainingViewModel.kt app/src/main/java/com/musfit/ui/training/TrainingScreen.kt app/src/main/java/com/musfit/ui/training/TrainingRoutineContent.kt app/src/test/java/com/musfit/data/repository/LocalTrainingRepositoryTest.kt app/src/test/java/com/musfit/ui/training/TrainingViewModelTest.kt docs/superpowers/plans/2026-06-22-training-routine-builder-exercise-library.md
git commit -m "feat: polish training routine builder"
```
