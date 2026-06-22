# Training Routine Editor Polish Design

## Goal

Make creating and editing Training routines feel consistent with the cleaned Training home and active workout screens.

## Scope

This slice is UI-only. It keeps the existing routine editor state, repository behavior, routine save/update/delete/duplicate callbacks, exercise add/remove, target sets/reps, and reorder behavior.

## Design

The routine editor becomes a focused editing screen:

- Header row with `Cancel`, `New routine` or `Edit routine`, and `Save`.
- Name and notes fields remain simple and full-width.
- Add exercise becomes a compact expandable searchable picker instead of a dropdown.
- Picker hides suggestions when collapsed, shows a short starter list when expanded, and filters by exercise name, equipment, or target muscles.
- Exercise rows show exercise name, metadata, target sets/reps fields, and icon actions for move up, move down, and remove.
- Existing duplicate/delete routine actions stay available for existing routines but remain secondary.

## Testing

Add focused unit tests for pure picker helper behavior:

- collapsed picker shows no suggestions
- expanded blank picker returns a short capped list excluding already-selected exercises
- query picker filters by name, equipment, or target muscles

Full verification remains `testDebugUnitTest lintDebug assembleDebug`.
