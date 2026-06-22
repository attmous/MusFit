# Training Supersets — Slice 2b Plan (pairs, make-with-next, v18→v19)

> **For agentic workers:** Use superpowers:executing-plans. Steps use checkbox (`- [ ]`). TDD: write the failing test, run it red, implement, run green. Source `.\.superpowers\sdd\android-env.ps1` before any gradle.

**Goal:** Let the active-workout logger group **two** exercises into a superset (Hevy-style), so the user alternates sets between them, with a clean Indigo grouped card and a one-tap "Make superset with next".

**Approved decisions:** pairs only (exactly 2 exercises); create via **"Make superset with next"** (auto-pair the standalone block below); allow **one bounded `sortOrder` re-index** on create so members render adjacently; **History stays flat** for this slice (the column persists for a later additive grouping).

**Architecture:** One nullable `supersetGroupId TEXT` on `workout_sets` (v18→v19, `ALTER … ADD COLUMN`). Grouping is **derived post-hoc**: `toActiveWorkoutDetail` keeps `exerciseBlocks` (History/export/tests untouched) and adds `exerciseGroupings: List<ExerciseGrouping>` (`Single | Superset`) with derived A/B labels. Repo ops `createSuperset`/`dissolveSuperset` (+ inheritance on add-set, auto-dissolve on underflow). New Indigo `SupersetGroupCard`. A fresh workout is all `Single` → looks identical to today until a superset is made.

---

### Task 1: Schema + migration (v18 → v19)
**Files:** `data/local/entity/TrainingEntities.kt`, `core/di/DatabaseModule.kt`, `data/local/MusFitDatabase.kt`, schema `app/schemas/com.musfit.data.local.MusFitDatabase/19.json`, test `data/local/MusFitDatabaseTest.kt`.

- [ ] **Step 1 — failing test:** in `MusFitDatabaseTest.kt`, `workoutSet_supersetGroupId_roundTrips`: on the in-memory DB, insert exercise+session, upsert a `WorkoutSetEntity` with `supersetGroupId = "grp-1"` and a second with default; assert `getWorkoutSet` reads back `"grp-1"` and `null`. Run red (column doesn't exist).
- [ ] **Step 2 — implement:**
  - `WorkoutSetEntity`: append `val supersetGroupId: String? = null` (after `completed`).
  - `DatabaseModule.kt`: add after `MIGRATION_17_18`:
    ```kotlin
    private val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE workout_sets ADD COLUMN supersetGroupId TEXT")
        }
    }
    ```
    Register `MIGRATION_18_19,` in `addMigrations(...)` after `MIGRATION_17_18,`.
  - `MusFitDatabase.kt`: `version = 18` → `version = 19`.
- [ ] **Step 3 — regen schema:** run `.\gradlew.bat :app:compileDebugKotlin` so Room writes `19.json`. Do **not** hand-edit. Confirm it has `"version": 19`, a `supersetGroupId TEXT` column (no `notNull`). Run the test green. Commit `19.json` with the change.

### Task 2: DAO projection + writers
**Files:** `data/local/dao/TrainingDao.kt`, test `data/repository/LocalTrainingRepositoryTest.kt` (or a DAO test).

- [ ] **Step 1 — failing test:** tag a session's exercise via the new writer, observe detail rows, assert `supersetGroupId` surfaces and `clearSupersetGroup` nulls a whole group.
- [ ] **Step 2 — implement:**
  - `WorkoutSetDetailRow`: add `val supersetGroupId: String? = null`.
  - `observeWorkoutSetDetailRows` SELECT: add `workout_sets.supersetGroupId AS supersetGroupId,`. **Keep `ORDER BY workout_sets.sortOrder ASC`.**
  - Add writers:
    ```kotlin
    @Query("UPDATE workout_sets SET supersetGroupId = :groupId WHERE sessionId = :sessionId AND exerciseId = :exerciseId")
    suspend fun setExerciseSupersetGroup(sessionId: String, exerciseId: String, groupId: String?)

    @Query("UPDATE workout_sets SET supersetGroupId = NULL WHERE sessionId = :sessionId AND supersetGroupId = :groupId")
    suspend fun clearSupersetGroup(sessionId: String, groupId: String)
    ```
- [ ] **Step 3 — green.**

### Task 3: Domain shape + label derivation + grouping fold (pure)
**Files:** `data/repository/TrainingRepository.kt`, test `data/repository/LocalTrainingRepositoryTest.kt`.

- [ ] **Step 1 — failing test:** a session with two exercises sharing one `supersetGroupId` + one standalone → `toActiveWorkoutDetail` returns `exerciseGroupings` in first-occurrence order: a `Superset` holding both members with labels `"A"`,`"B"`, then a `Single`; and `exerciseBlocks` still equals the flat ordered list; `completedSetCount`/`totalVolumeKg` unchanged.
- [ ] **Step 2 — implement:**
  - `WorkoutExerciseBlock`: add `val supersetGroupId: String? = null` and `val supersetLabel: String? = null`.
  - `LoggedWorkoutSetDetail`: add `val supersetGroupId: String? = null`.
  - Add types after `WorkoutExerciseBlock`:
    ```kotlin
    data class SupersetGroup(val supersetGroupId: String, val exerciseBlocks: List<WorkoutExerciseBlock>)
    sealed interface ExerciseGrouping {
        data class Single(val block: WorkoutExerciseBlock) : ExerciseGrouping
        data class Superset(val group: SupersetGroup) : ExerciseGrouping
    }
    ```
  - `ActiveWorkoutDetail`: keep `exerciseBlocks`; add `val exerciseGroupings: List<ExerciseGrouping> = emptyList()`.
  - In `toActiveWorkoutDetail`: extract the block body into `buildWorkoutExerciseBlock(...)`; derive `exerciseId -> (groupId, label A/B…)` in one sortOrder pass; build ordered blocks (stamp groupId/label); fold **consecutive** blocks sharing a non-null groupId into `Superset`, else `Single`. Keep `exerciseBlocks = orderedBlocks`.
- [ ] **Step 3 — green.**

### Task 4: Repo ops — create / dissolve (+ bounded re-index)
**Files:** `data/repository/TrainingRepository.kt` (interface + `LocalTrainingRepository`), test `data/repository/LocalTrainingRepositoryTest.kt`.

- [ ] **Step 1 — failing test:** `createSuperset` rejects when not exactly 2 distinct exercises or either is already grouped (returns `null`); on success both members' sets share one minted `supersetGroupId` and are contiguous in `sortOrder` (non-members keep relative order); `dissolveSuperset` nulls the column so the group vanishes from the next emission. Ops no-op when session not active.
- [ ] **Step 2 — implement (all in `database.withTransaction`, guarded by `session.status == WORKOUT_STATUS_ACTIVE`):**
  - `createSuperset(sessionId, exerciseAId, exerciseBId): String?` — validate 2 distinct + neither already grouped; mint `UUID`; `setExerciseSupersetGroup` for each; then a stable re-index of this session's sets so the two members' blocks are adjacent (anchor at the first member's first set), preserving all other relative order; re-`upsertWorkoutSet`. Return groupId (null on validation failure).
  - `dissolveSuperset(sessionId, groupId)` — `clearSupersetGroup(sessionId, groupId)` (sortOrder untouched; fully reversible).
- [ ] **Step 3 — green.**

### Task 5: Inheritance + auto-dissolve
**Files:** `data/repository/TrainingRepository.kt`, test `data/repository/LocalTrainingRepositoryTest.kt`.

- [ ] **Step 1 — failing test:** `addSetToExercise` for an exercise in a group inherits its `supersetGroupId`; `duplicateLastSet` carries it; deleting the last remaining set such that the group has `< 2` exercises with sets auto-dissolves (clears the column).
- [ ] **Step 2 — implement:** in `addSetToExercise`, read `getLastWorkoutSetForExercise(sessionId, exerciseId)?.supersetGroupId` and set it on the new row. In `deleteWorkoutSet`, after delete, if the affected group now has `< 2` distinct exercises with sets, `clearSupersetGroup`.
- [ ] **Step 3 — green.**

### Task 6: ViewModel commands
**Files:** `ui/training/TrainingViewModel.kt`, test `ui/training/TrainingViewModelTest.kt`.

- [ ] **Step 1 — failing test (with `FakeTrainingRepository`):** `makeSupersetWithNext(exerciseId)` finds the next `Single` grouping in `state.activeWorkout.exerciseGroupings` and calls `createSuperset(exerciseId, nextId)`; no-op when there is no next standalone block. `dissolveSuperset(groupId)` delegates with the active sessionId.
- [ ] **Step 2 — implement:** mirror `addWorkoutSet` (read `state.value.activeWorkout?.sessionId`, `viewModelScope.launch`, delegate). Add `makeSupersetWithNext`, `dissolveSuperset`. The active-workout flow re-emits — no manual state mutation.
- [ ] **Step 3 — green.**

### Task 7: UI — grouped rendering + affordances (Indigo)
**Files:** `ui/training/TrainingActiveWorkoutContent.kt`, `ui/training/TrainingScreen.kt`.

- [ ] **Step 1.** Add `nested: Boolean = false` to `ActiveExerciseBlock`; when true it drops its own `Surface`/card chrome and renders just the inner `Column` (so it sits inside the group card). Add a `supersetLabel` A/B badge (Box 22.dp, CircleShape, `accent.container`/`accent.onContainer`) beside the `FitnessCenter` icon when non-null.
- [ ] **Step 2.** New `SupersetGroupCard(group, accent, …set callbacks…, onDissolveSuperset)`: `Surface(MusFitTheme.colors.surface, shapes.large)` → Row of a 3.dp `accent.color` rail + a Column with a `Surface(accent.container)` "SUPERSET" chip, each member via `ActiveExerciseBlock(nested = true)`, and a footer `TextButton("Dissolve superset", accent.color)`.
- [ ] **Step 3.** Replace the flat `workout.exerciseBlocks.forEach { ActiveExerciseBlock(...) }` with `workout.exerciseGroupings.forEach { when … Single -> ActiveExerciseBlock(...); Superset(size 2) -> SupersetGroupCard(...); Superset(size 1, transient) -> ActiveExerciseBlock(...) }`.
- [ ] **Step 4.** On a standalone `ActiveExerciseBlock`, add an overflow `IconButton(MoreVert, tint = accent.color)` → `DropdownMenu` with **"Make superset with next"** → `onMakeSuperset(block.exercise.id)` (disabled when there is no next standalone block).
- [ ] **Step 5.** Thread callbacks `TrainingActiveWorkoutContent` → `TrainingScreen.kt`: `onMakeSuperset = viewModel::makeSupersetWithNext`, `onDissolveSuperset = viewModel::dissolveSuperset`.

### Task 8: Full verification + device + commit
- [ ] `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain` → green. (OneDrive AccessDenied → `--stop`, delete `app/build`, rerun.)
- [ ] On the dev phone: start a workout with 2+ exercises → "Make superset with next" → Indigo grouped card with A/B badges; add a set inside it (stays grouped); dissolve → back to two standalone blocks.
- [ ] Commit `feat(training): supersets — pair exercises in the active-workout logger (v19)` including `19.json`.

## Notes
- Pairs-only: a group is always exactly 2, so there is no "add exercise to superset"; "remove" == "dissolve". Auto-dissolve only matters when a member loses all its sets.
- `sortOrder` stays the single ordering truth; `createSuperset`'s bounded re-index is the **only** write that touches it.
- History/export read flat rows in `sortOrder` and must **not** filter on the new column.
