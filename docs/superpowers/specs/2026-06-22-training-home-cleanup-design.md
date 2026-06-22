# Training Home Cleanup Design

## Goal

Make the Training landing screen feel like a focused workout app home instead of a mixed demo page. The first screen should prioritize active workout resume, starting an empty workout, and scanning routines.

## Scope

This slice changes Training UI composition only. It keeps the existing routines, exercise library, history, progress, active workout, quick set logging behavior, and repository contracts intact.

## Design

The Training home keeps the existing top-level sections: Routines, Exercises, History, and Progress. The Routines view becomes the default workout hub:

- A compact title header anchors the page.
- Active workout resume appears as a dense banner when a workout is in progress.
- Start empty workout is a prominent full-width action.
- Quick set logging is hidden behind a compact Quick log disclosure instead of occupying the first screen.
- Routine cards become scan-friendly rows with routine name, exercise/set count, a primary Start action, and secondary Edit/Duplicate/Delete actions.

The Exercises, History, and Progress sections remain functionally unchanged in this slice. Their deeper polish should be handled in later dedicated slices.

## Testing

Add focused unit tests for pure UI helper functions that shape the cleaned home:

- Quick set panel starts collapsed.
- Quick set panel toggles open/closed.
- Routine action labels hide starter-only destructive actions while keeping duplicate/start visible.

Full verification remains `testDebugUnitTest lintDebug assembleDebug`.
