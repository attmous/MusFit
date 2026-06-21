# Slice 23: Food UX Polish Pass

## Goal
Make the Food diary feel more polished and quicker to start using, especially when the day is empty.

## Scope
- Add a compact empty-day start card with direct actions for breakfast, barcode, and AI draft logging.
- Derive empty-state action labels from ViewModel state so the behavior is testable.
- Add accessibility labels for the new start actions.

## Rules
- Keep the polish local to Food.
- Do not change data models or logging behavior.
- Preserve the existing Add Food sheet and modes.

## Tests First
- ViewModel test for empty diary start actions.
- ViewModel test that the empty state disappears after logged food exists.

## Verification
Run focused Food ViewModel tests, then `testDebugUnitTest lintDebug assembleDebug`.
