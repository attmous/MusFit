# Training Workout Finish Flow Design

## Goal

Complete the main workout loop: start a workout, log sets, confirm finish or discard, then review the completed workout in History.

## Scope

This slice builds on the active workout and Training home cleanup work already on `feat/training-home-cleanup`. It does not change Room schema or repository persistence because completed workout history already exists.

## Design

Finishing and discarding become explicit confirmation actions:

- Tapping Finish opens a confirmation dialog that summarizes the workout title, completed sets, and volume.
- Confirming Finish calls the existing repository finish operation, closes the active workout route, selects History, and opens the completed workout detail when it can be loaded.
- Tapping Discard opens a destructive confirmation dialog.
- Confirming Discard calls the existing repository discard operation, closes the active workout route, returns to Routines, and clears pending confirmation state.

History detail gets a compact workout summary so the post-finish destination feels intentional. It shows the workout title, sets, volume, duration when available, and exercise set rows.

## Testing

Add ViewModel tests for:

- opening and cancelling finish confirmation
- opening and cancelling discard confirmation
- confirming finish calls the repository and opens completed history detail
- confirming discard calls the repository and returns to Routines

Full verification remains `testDebugUnitTest lintDebug assembleDebug`.
