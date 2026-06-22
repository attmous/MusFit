# Training Active Sets Layout Design

## Goal

Make MusFit's active workout logger feel closer to a serious training app by replacing the current per-set text-field cards with a compact set table layout inspired by the provided Hevy screenshots. The work should copy the layout idea, not Hevy's theme, assets, illustrations, or exact branding.

## Scope

This slice only changes the active workout route. Training home, routine cards, the exercise library, history, progress, global theme, and bottom navigation stay out of scope except where compile wiring requires a small signature update.

## Layout

The active workout screen should start with a compact top row: back action, workout title, `Finish`, and a secondary discard action. Under that, show a dense summary strip for completed set count and total volume, followed by the current rest timer when visible.

Each exercise block should be visually unframed or lightly grouped, with:

- exercise name as the primary heading
- target reps and optional notes under the title
- a small rest timer/status line
- a table header: `SET`, `PREVIOUS`, `KG`, `REPS`, `RPE`, check
- set rows with stable columns and subtle alternating background
- warm-up sets labeled `W`; working sets numbered by working-set order
- previous values shown exactly as the repository provides them, or `-`
- kg/reps/rpe editable inline through compact text fields
- a check control at the far right
- a full-width `+ Add Set` action below the table

The user specifically wants to keep MusFit's philosophy of having the library, RPE, reps, and kg visible, so the table keeps those columns. The current duplicate/delete actions remain available but secondary; they should not dominate the main row layout.

## Behavior

Existing repository and ViewModel behavior should remain intact:

- completing a valid set toggles completion through `toggleWorkoutSetCompletion`
- blank/invalid sets still cannot be completed
- editing kg/reps/rpe/notes still calls `updateWorkoutSetFields`
- adding a set still calls `addWorkoutSet`
- duplicating the last set remains available
- deleting a set remains available
- RPE remains optional

No new database schema is required.

## Testing

The change is mostly Compose layout, but set labeling should be tested because it affects the visible table behavior:

- warm-up sets display `W`
- working sets are numbered by working-set sequence, not raw row index
- previous labels fall back to `-`
- kg/reps/rpe display values compactly

Run focused Training tests first, then full `testDebugUnitTest lintDebug assembleDebug`.
