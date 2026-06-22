# Task 1 Report: Training Foundation And Starter Data

## Status

Completed.

## Commit

- `876462f` `feat: add training starter routines`

## What I Implemented

- Added Training V1 starter data models and seeded starter exercises/routines in a new `TrainingStarterData.kt`.
- Extended the training Room schema to version 16 with:
  - `RoutineEntity.updatedAtEpochMillis`
  - `RoutineEntity.isStarter`
  - `WorkoutSessionEntity.title`
  - `WorkoutSessionEntity.status`
  - `WorkoutSetEntity.setType`
- Registered `MIGRATION_15_16` and generated `app/schemas/com.musfit.data.local.MusFitDatabase/16.json`.
- Added DAO projections and flows for:
  - filtered exercise browsing
  - routine summaries
  - active workout summary
  - bulk upserts for starter data
- Extended `TrainingRepository` with:
  - `observeExercises(...)`
  - `observeRoutineSummaries()`
  - `observeActiveWorkoutSummary()`
  - `seedStarterTrainingData()`
- Implemented repository mappings and starter data seeding in `LocalTrainingRepository`.
- Made starter data seeding idempotent so repeated seed calls do not duplicate data or violate foreign keys.
- Updated `TrainingViewModel` to:
  - default to `TrainingSection.Routines`
  - seed starter data in `init`
  - collect routines/exercises/active workout flows
  - preserve the existing add-set/toggle-set behavior
- Replaced the minimal Training home screen with a section shell for:
  - Routines
  - Exercises
  - History
  - Progress
  - Active workout card

## TDD Evidence

### Red

1. Added failing repository tests:
   - `seedStarterTrainingData_importsExercisesAndRoutinesOnce`
   - `observeExercises_filtersBySearchMuscleAndEquipment`
2. Added failing ViewModel test:
   - `initialState_isRoutinesFirstAndSeedsStarterTrainingData`
3. Ran:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.musfit.data.repository.LocalTrainingRepositoryTest" --tests "com.musfit.ui.training.TrainingViewModelTest" --no-daemon --console=plain
```

4. Verified red state:
   - compile failed with unresolved references for `seedStarterTrainingData`, `observeExercises`, `observeRoutineSummaries`, `observeActiveWorkoutSummary`, `RoutineSummary`, `ExerciseSummary`, `ActiveWorkoutSummary`, and `TrainingSection`

### Green

1. Implemented the minimum production changes to satisfy the new tests.
2. First green attempt exposed a real defect:
   - repeated starter seeding failed with `SQLiteConstraintException` due to `REPLACE` semantics against foreign-key-linked exercise rows
3. Fixed by making seed logic explicitly idempotent before bulk insert.
4. Re-ran focused tests and got green.

## Tests And Results

### Focused TDD verification

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.musfit.data.repository.LocalTrainingRepositoryTest" --tests "com.musfit.ui.training.TrainingViewModelTest" --no-daemon --console=plain
```

Result: PASS

### Build / schema verification

```powershell
.\gradlew.bat assembleDebug --no-daemon --console=plain
```

Result: PASS

### Schema output

- Verified present: `app/schemas/com.musfit.data.local.MusFitDatabase/16.json`

## Files Changed

- `app/src/main/java/com/musfit/data/local/entity/TrainingEntities.kt`
- `app/src/main/java/com/musfit/data/local/dao/TrainingDao.kt`
- `app/src/main/java/com/musfit/data/local/MusFitDatabase.kt`
- `app/src/main/java/com/musfit/core/di/DatabaseModule.kt`
- `app/src/main/java/com/musfit/data/repository/TrainingStarterData.kt`
- `app/src/main/java/com/musfit/data/repository/TrainingRepository.kt`
- `app/src/main/java/com/musfit/ui/training/TrainingViewModel.kt`
- `app/src/main/java/com/musfit/ui/training/TrainingScreen.kt`
- `app/src/test/java/com/musfit/data/local/MusFitDatabaseTest.kt`
- `app/src/test/java/com/musfit/data/repository/LocalTrainingRepositoryTest.kt`
- `app/src/test/java/com/musfit/ui/training/TrainingViewModelTest.kt`
- `app/schemas/com.musfit.data.local.MusFitDatabase/16.json`

## Self-Review

- Kept the work inside the Task 1-owned file set for production/test changes.
- Preserved the existing architecture: Compose -> TrainingViewModel -> TrainingRepository -> TrainingDao -> Room.
- Preserved existing add-set training behavior while layering in the new routines/exercises foundation.
- Avoided touching unrelated tests by providing default interface implementations for the new repository surface, which kept scope local and compile-safe.

## Concerns

- `resumeActiveWorkout()` is still a placeholder message path. Task 1 only required the section shell and active workout summary surface, not full workout session resume behavior.
- `seedStarterTrainingData()` currently short-circuits based on an existing starter exercise. That is sufficient for Task 1 idempotency, but later slices may want a more explicit starter-data versioning strategy.

## Fixes For Review

### What I fixed

- Restored a reachable compact quick-set logging path in `TrainingScreen` so users can still add and toggle simple sets from the only Training route while the full workout logger is still pending.
- Surfaced `state.message` in the screen, so the existing active-workout resume placeholder now produces visible feedback.
- Made starter seeding robust when a same-name custom exercise already exists by resolving starter routine links against the existing exercise instead of bailing out or creating a duplicate.
- Kept starter routine and routine-exercise seeding idempotent by reusing stable routine-exercise ids while updating their referenced exercise ids as needed.
- Updated latest-workout export selection to use the latest completed workout session only, excluding active sessions from Health Connect export selection.
- Added focused regression tests for:
  - custom/partial starter seeding completion without duplicates
  - completed-only workout export selection
  - visible resume message state

### Tests run and exact results

- `.\gradlew.bat testDebugUnitTest --tests "com.musfit.data.repository.LocalTrainingRepositoryTest" --tests "com.musfit.ui.training.TrainingViewModelTest" --no-daemon --console=plain`
  - Result: `BUILD SUCCESSFUL`
- `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain`
  - Result: `BUILD SUCCESSFUL`

### Files changed

- `app/src/main/java/com/musfit/data/local/dao/TrainingDao.kt`
- `app/src/main/java/com/musfit/data/repository/TrainingRepository.kt`
- `app/src/main/java/com/musfit/ui/training/TrainingScreen.kt`
- `app/src/test/java/com/musfit/data/repository/LocalTrainingRepositoryTest.kt`
- `app/src/test/java/com/musfit/ui/training/TrainingViewModelTest.kt`
- `.superpowers/sdd/task-1-report.md`

### Any concerns

- The restored quick logger is intentionally transitional. It keeps Task 1’s original add/toggle set behavior reachable, but it does not attempt to fill in the active-workout execution flow planned for Task 3.

## Re-Review Fixes 2

### What I fixed

- Updated the production Health Connect export path in `LocalHealthRepository.exportLatestWorkout()` to export only the latest `completed` workout session.
- Restored quick logger export compatibility by making the legacy `addCompletedSet` / `currentOrNewSession` path create and refresh same-day sessions as `completed` with an `endedAtEpochMillis`.
- Removed the internal implementation-note copy from the visible quick logger card.
- Added focused regression coverage for the real Health repository export path and for quick-logger-created workouts being exportable.

### Tests run and exact results

- `.\gradlew.bat testDebugUnitTest --tests "com.musfit.data.repository.LocalTrainingRepositoryTest" --tests "com.musfit.data.repository.LocalHealthRepositoryTest" --no-daemon --console=plain`
  - First run before production changes: `11 tests completed, 2 failed`
  - Second run after production changes: success (`exit code 0`)
- `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain`
  - Result: `BUILD SUCCESSFUL`

### Files changed

- `app/src/main/java/com/musfit/data/repository/HealthRepository.kt`
- `app/src/main/java/com/musfit/data/repository/TrainingRepository.kt`
- `app/src/main/java/com/musfit/ui/training/TrainingScreen.kt`
- `app/src/test/java/com/musfit/data/repository/LocalHealthRepositoryTest.kt`
- `app/src/test/java/com/musfit/data/repository/LocalTrainingRepositoryTest.kt`
- `.superpowers/sdd/task-1-report.md`

### Concerns

- This preserves Task 1 quick logger compatibility by treating its same-day sessions as already completed. It does not implement the later active-workout lifecycle, which still belongs to future Training tasks.

## Re-Review Fixes 3

### What I fixed

- Replaced destructive `WorkoutSessionEntity` parent-row rewrites in the Training repository quick-logger path with non-destructive insert-or-update DAO calls, so same-day session refreshes keep existing `workout_sets` rows.
- Replaced destructive workout-session export metadata writes in both `TrainingRepository.markWorkoutExported(...)` and `LocalHealthRepository.exportLatestWorkout()` with the same non-destructive insert-or-update DAO calls, so Health Connect record metadata updates no longer cascade-delete exported sets.
- Reworked starter routine seeding to update existing `RoutineEntity` rows in place instead of reseeding them through `REPLACE`, which preserves `WorkoutSessionEntity.routineId` links for completed workouts that reference starter routines.
- Added focused regression tests covering same-day quick logging, export metadata persistence, and repeated starter seeding with an existing routine-linked workout session.

### Tests run and exact results

- `.\gradlew.bat testDebugUnitTest --tests "com.musfit.data.repository.LocalTrainingRepositoryTest" --tests "com.musfit.data.repository.LocalHealthRepositoryTest" --no-daemon --console=plain`
  - First run before production changes: `14 tests completed, 3 failed`
  - Failing regressions:
    - `addCompletedSet_multipleSetsSameDay_preservesEarlierSets`
    - `exportLatestWorkout_preservesExportedWorkoutSetsWhenMetadataIsSaved`
    - `seedStarterTrainingData_repeatedSeedPreservesRoutineLinkOnExistingWorkoutSession`
  - Second run after production changes: success (`exit code 0`)
- `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain`
  - Result: `BUILD SUCCESSFUL`

### Files changed

- `app/src/main/java/com/musfit/data/local/dao/TrainingDao.kt`
- `app/src/main/java/com/musfit/data/repository/HealthRepository.kt`
- `app/src/main/java/com/musfit/data/repository/TrainingRepository.kt`
- `app/src/test/java/com/musfit/data/repository/LocalHealthRepositoryTest.kt`
- `app/src/test/java/com/musfit/data/repository/LocalTrainingRepositoryTest.kt`
- `.superpowers/sdd/task-1-report.md`

### Concerns

- `upsertWorkoutSession(...)` and `upsertRoutine(...)` still exist on the DAO for compatibility with older call sites and tests. This fix only removed them from the Task 1 parent-row update paths called out in the review.

## Re-Review Fixes 4

### What I fixed

- Scoped both Health export readers to completed sets only by adding a dedicated DAO read for completed workout sets and using it from:
  - `TrainingRepository.getLatestWorkoutForExport()`
  - `LocalHealthRepository.exportLatestWorkout()`
- Scoped daily training summary input to completed workout sessions only by filtering the date-range workout-set DAO query to `workout_sessions.status = 'completed'`.
- Added focused regression coverage for:
  - repository export filtering of incomplete sets
  - Health export filtering of incomplete sets
  - Today training summary exclusion of active and discarded sessions

### Tests run and exact results

- `.\gradlew.bat testDebugUnitTest --tests "com.musfit.data.repository.LocalTrainingRepositoryTest" --tests "com.musfit.data.repository.LocalHealthRepositoryTest" --no-daemon --console=plain`
  - First run before production changes: `17 tests completed, 3 failed`
  - Failing regressions:
    - `getLatestWorkoutForExport_filtersOutIncompleteSetsFromCompletedSession`
    - `exportLatestWorkout_filtersOutIncompleteSetsFromCompletedSession`
    - `observeDailyTrainingSummary_ignoresActiveAndDiscardedSessions`
  - Second run after production changes: success (`exit code 0`)
- `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain`
  - Result: `BUILD SUCCESSFUL`

### Files changed

- `app/src/main/java/com/musfit/data/local/dao/TrainingDao.kt`
- `app/src/main/java/com/musfit/data/repository/TrainingRepository.kt`
- `app/src/main/java/com/musfit/data/repository/HealthRepository.kt`
- `app/src/test/java/com/musfit/data/repository/LocalTrainingRepositoryTest.kt`
- `app/src/test/java/com/musfit/data/repository/LocalHealthRepositoryTest.kt`
- `.superpowers/sdd/task-1-report.md`

### Concerns

- This stays within Task 1 compatibility scope. It does not add any new active-workout lifecycle behavior beyond excluding non-completed sessions from export and Today summaries.
