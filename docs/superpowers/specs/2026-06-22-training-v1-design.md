# Training V1 Design

Date: 2026-06-22

## Objective

Build Training V1 as a strength-only, routines-first miniapp for MusFit. The user should be able to start from starter or custom routines, log a gym session efficiently, maintain an exercise library, review workout history, and see useful progress signals from local workout data.

Training remains Android-only, local-first, and integrated with the existing MusFit bottom navigation, Today summary, Room database, Hilt ViewModels, Kotlin Flow, and Health Connect workout export boundary. This design intentionally does not add accounts, cloud sync, analytics, subscriptions, social features, cardio tracking, wearable integrations, or AI coaching.

## Approved Product Choices

- Scope: strength training only.
- Home model: routines-first.
- Starter content: starter exercises plus starter routines.
- Set logging detail: full gym workflow with reps, weight, RPE, notes, warm-up/working set type, rest timer shell, previous-set reference, duplicate-last-set, edit, delete, and completion.
- Progress depth: workout history, exercise PRs, and simple trends for volume and estimated one-rep max.
- Implementation approach: vertical Training V1 with staged depth.

## Current Code Context

Training currently has a minimal single-screen set logger:

- `TrainingScreen.kt` lets the user enter exercise, reps, and weight, add a completed set, toggle completion, and see volume and estimated one-rep max.
- `TrainingViewModel.kt` holds local UI state and calls `TrainingRepository`.
- `TrainingRepository.kt` persists exercises, one active same-day session, and sets.
- Room already has `ExerciseEntity`, `RoutineEntity`, `RoutineExerciseEntity`, `WorkoutSessionEntity`, and `WorkoutSetEntity`.
- Today already observes `TrainingRepository.observeDailyTrainingSummary(date)`.
- Health Connect export already uses the latest persisted workout session and sets.

The design should extend these existing layers rather than replace them.

## User Experience

### Training Home

The Training tab opens to a routines-first home with compact internal sections:

- Routines
- Exercises
- History
- Progress

Routines is the default section. It shows starter and custom routines with routine name, exercise count, target set count, and a primary start action. A smaller action starts a blank workout. If an active workout exists, a persistent card appears at the top with elapsed time, completed set count, volume, and a resume action.

The home should feel like a practical gym tool, not a marketing page. It should use dense, readable rows and compact cards with clear actions.

### Routines

Routines support:

- Starter routines imported once for new installs or existing installs without starter training data.
- Custom routine creation.
- Routine editing for name, notes, exercises, target sets, and target reps.
- Exercise add/remove/reorder inside a routine.
- Routine duplicate and delete.
- Starting a workout from a routine.

Starter routine examples can include:

- Full Body A
- Full Body B
- Push
- Pull
- Legs

The starter routine set should stay small enough to be useful without cluttering the app.

### Exercise Library

The exercise library supports:

- Starter strength exercises.
- Search by name.
- Filters for muscle group and equipment.
- Custom exercise creation and editing.
- Exercise detail with recent performance, PRs, and related history.

Starter exercises should include common compound and accessory movements with conservative metadata:

- Name
- Category: `strength`
- Equipment, nullable when not applicable.
- Target muscles as a displayable string or existing-compatible storage format.
- `isCustom = false`

Custom exercises use `isCustom = true`.

### Active Workout

The active workout screen is the core logging experience. A workout can start blank or from a routine. Starting from a routine creates grouped exercise blocks from routine exercises and their targets.

Each workout shows:

- Workout title from the routine name or "Blank workout".
- Elapsed time.
- Completed set count.
- Total volume.
- Finish and discard actions.

Each exercise block shows:

- Exercise name.
- Muscle/equipment metadata when available.
- Previous workout or previous best hint when available.
- Target set/reps hint for routine-based workouts.
- Add set action.
- Duplicate last set action when at least one set exists.
- Remove exercise action.

Each set row supports:

- Set type: warm-up or working.
- Previous set reference, such as last completed kg x reps for that exercise.
- Weight in kg.
- Reps.
- RPE.
- Completed checkbox.
- Notes.
- Edit and delete.

Completing a set recalculates workout volume and PR preview. It also starts a visible rest timer shell. For V1 the rest timer is local UI state only: no notification scheduling, background countdown guarantee, or system alarm integration.

Finishing a workout opens a confirmation summary with duration, exercises, completed sets, and volume. Confirming marks the session complete and makes it appear in history. Discarding removes the active incomplete session and its unneeded sets after confirmation.

### History

History shows completed workouts ordered newest first. Each row/card shows:

- Date.
- Routine or workout name.
- Duration.
- Completed set count.
- Total volume.

Workout detail shows exercises grouped with completed and incomplete sets, set metadata, notes, and summary totals. History should preserve compatibility with Today and Health Connect export by continuing to expose daily summaries and latest completed workout data through repository methods.

### Progress

Progress starts simple and deterministic. It does not need advanced charting dependencies unless the existing project already has one.

Progress supports:

- Exercise picker.
- PR cards for heaviest weight, max reps, best estimated one-rep max, and best workout volume.
- Simple trend data for volume and estimated one-rep max over time.
- Empty states when an exercise has no completed history.

Trend visuals can be compact Compose-rendered line or bar visuals. The important V1 behavior is that trend data is correct and scannable.

## Architecture

Keep the existing direction of dependency:

Compose screen -> `TrainingViewModel` -> `TrainingRepository` -> `TrainingDao` -> Room

Use the existing Hilt repository binding. Use Kotlin Flow for observable home, active workout, history, and progress state. Keep domain calculations pure where possible, especially PR and trend calculations.

The current Training files are small enough to evolve in place initially. If implementation makes a single file unwieldy, split by feature surface:

- `TrainingScreen.kt` for the route and top-level sections.
- `TrainingActiveWorkoutContent.kt` for workout logging composables.
- `TrainingRoutineContent.kt` for routine list/editor composables.
- `TrainingHistoryContent.kt` for history and detail composables.
- `TrainingProgressContent.kt` for progress composables.

Splitting should be driven by readability, not by speculative architecture.

## Repository Use Cases

`TrainingRepository` should grow around four use-case groups.

### Library

- Observe starter and custom exercises.
- Search exercises.
- Create custom exercises.
- Update custom exercises.
- Seed starter exercises once.

### Routines

- Observe routines with exercise summaries.
- Load routine detail.
- Create routine.
- Update routine name and notes.
- Add, remove, and reorder routine exercises.
- Update routine exercise target sets and target reps.
- Duplicate routine.
- Delete routine.
- Seed starter routines once.

### Active Workout

- Start blank workout.
- Start workout from routine.
- Observe active workout.
- Add exercise to active workout.
- Remove exercise from active workout.
- Add set.
- Duplicate last set.
- Update set fields.
- Toggle set completion.
- Delete set.
- Finish workout.
- Discard workout.

### History And Progress

- Observe completed workout summaries.
- Load workout detail.
- Observe progress for an exercise.
- Compute PRs and trend points from completed sets.
- Continue to expose `observeDailyTrainingSummary(date)`.
- Continue to support Health Connect export of the latest completed workout.

## Data Model

Existing entities cover much of the needed model:

- `ExerciseEntity`
- `RoutineEntity`
- `RoutineExerciseEntity`
- `WorkoutSessionEntity`
- `WorkoutSetEntity`

Likely schema additions:

- `ExerciseEntity`: optional source/starter flag if `isCustom = false` is not enough to identify seeded records.
- `RoutineEntity`: optional source/starter flag and updated timestamp if needed for ordering.
- `WorkoutSessionEntity`: explicit status, such as active, completed, discarded, plus optional title. This avoids overloading nullable `endedAtEpochMillis`.
- `WorkoutSetEntity`: set type, such as warm-up or working. RPE and notes already exist.

Any schema change must follow the repository rule:

- Bump `MusFitDatabase` version.
- Add a Room migration in `DatabaseModule.kt`.
- Register the migration.
- Commit the new schema JSON under `app/schemas`.
- Add or update migration tests.

Avoid adding future-only fields for cardio, bodyweight mode, cloud sync, templates beyond routines, or social features in V1.

## State And Navigation

Training can remain a single bottom-nav destination with internal state rather than introducing new app-level destinations for every Training subpage. The ViewModel can expose a single `TrainingUiState` with:

- Selected section: routines, exercises, history, or progress.
- Active workout summary, if present.
- Routine list and selected routine editor state.
- Exercise list and filters.
- Active workout detail state.
- History list and selected workout detail.
- Progress selected exercise and derived metrics.
- Loading and error states for repository operations.

For screens that need back behavior, use ViewModel state first:

- Open routine editor.
- Open active workout.
- Open workout detail.
- Open exercise detail.

If this becomes awkward, introduce Training-specific navigation inside the Training route. Avoid expanding the global bottom-nav destination set.

## Error Handling And Empty States

Training should handle:

- No routines: show starter import or create-routine prompt.
- No exercises: show starter import or create-exercise prompt.
- No active workout: show routine launcher and blank workout action.
- No history: show a concise empty state that points to starting a workout.
- No progress for an exercise: show that completed sets are needed first.
- Invalid set input: prevent saving negative or zero reps/weight for completed weighted sets.
- Repository failures: show a short message and keep editable state intact when possible.

Discard and delete actions require confirmation. Finish workout requires confirmation if there are no completed sets.

## Calculations

Continue using pure domain code for calculations. Extend `WorkoutCalculator` or add focused helpers for:

- Total completed volume.
- Estimated one-rep max using the existing Epley formula.
- Per-exercise PRs.
- Per-workout exercise volume.
- Trend points by workout date.

Incomplete sets should not count toward volume, PRs, Today summaries, or progress trends.

## Health Connect And Today Compatibility

Today must continue to show daily completed set count and volume.

Health Connect export should export the latest completed workout, not an active or discarded workout. Existing export behavior should keep working as Training V1 evolves. If session status is added, update export selection to filter for completed sessions.

## Testing Strategy

Use TDD for behavior changes.

Repository tests should cover:

- Starter exercise and routine seeding is idempotent.
- Creating, editing, duplicating, and deleting routines.
- Starting blank and routine-based workouts.
- Adding, editing, duplicating, completing, and deleting sets.
- Finishing and discarding workouts.
- History only includes completed workouts.
- Progress PRs and trends use completed sets only.
- Today summary remains correct.
- Health Connect latest-workout export ignores active and discarded workouts.

ViewModel tests should cover:

- Default routines-first state.
- Switching sections.
- Active workout resume card state.
- Routine launcher flow.
- Routine editor state transitions.
- Set input validation.
- Completing a set updates summary and rest timer shell state.
- Finish/discard confirmations.
- History and progress selection flows.

Domain tests should cover:

- PR calculations.
- Trend point calculations.
- Incomplete set exclusion.

Full verification before claiming implementation completion remains:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain
```

## Implementation Slices

### Slice 1: Training Foundation And Starter Data

- Add starter exercise and routine seeding.
- Add richer repository models for routine summaries, routine detail, exercise summaries, and active workout summary.
- Show routines-first home with Routines, Exercises, History, and Progress sections.
- Preserve existing simple logging behavior until the active workout slice replaces it.
- Add repository and ViewModel tests for starter data and default home state.

### Slice 2: Routine Editor And Launcher

- Add routine create/edit/delete/duplicate.
- Add exercise search and selection for routines.
- Support target sets and target reps.
- Start blank workout or routine-based workout.
- Add tests for routine CRUD and launching.

### Slice 3: Active Workout Logger

- Add grouped active workout UI.
- Add exercise blocks and editable set rows.
- Support set type, kg, reps, RPE, notes, completion, previous-set hints, duplicate last set, delete, and add set.
- Add rest timer shell as local UI state.
- Add finish and discard confirmations.
- Add tests for active workout state transitions and persistence.

### Slice 4: History And Workout Detail

- Add completed workout list.
- Add workout detail grouped by exercise.
- Ensure Today summary and Health Connect export still use completed workouts correctly.
- Add tests for history selection and export compatibility.

### Slice 5: Progress

- Add exercise picker in Progress.
- Add PR cards.
- Add simple trend visuals or compact trend rows for volume and estimated one-rep max.
- Add tests for PRs and trends.

## Acceptance Criteria

Training V1 is complete when:

- A new or existing local user can start a starter routine, log a strength workout, finish it, and see it in history.
- A user can create and edit custom exercises and routines.
- A user can log sets with weight, reps, RPE, notes, set type, completion, previous-set hints, duplicate-last-set, and delete/edit controls.
- A visible rest timer shell appears after completing a set.
- History and progress are derived from completed local workout data.
- Today continues to show correct training summaries.
- Health Connect workout export continues to work for completed workouts.
- Unit, repository, lint, and debug assemble verification pass.

## Non-Goals For V1

- Cardio tracking.
- Bodyweight or assisted-bodyweight-specific set math.
- Plate calculator.
- Supersets.
- Program periodization.
- Wearable integration.
- Rest timer notifications or background alarms.
- Cloud sync.
- Accounts.
- Social features.
- Analytics.
- Subscriptions.
- AI coaching.
