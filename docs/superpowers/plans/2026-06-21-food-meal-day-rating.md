# Slice 22: Food, Meal, and Day Rating

## Goal
Add Lifesum-style ratings for the Food day and each meal, with clear deterministic reasons and suggested improvement.

## Scope
- Derive a day rating from calories, protein, fiber, sodium, and logged state.
- Derive meal ratings from calories, protein, fiber, sodium, and logged state.
- Show a compact day rating and meal rating pills in the Food diary.

## Rules
- Ratings are derived only from local diary/goal data.
- No saved rating state, AI, or remote calls.
- Keep text short: one reason and one suggestion.

## Tests First
- ViewModel test for a balanced day and balanced meal rating.
- ViewModel test for high sodium / low protein lowering ratings with reasons.

## Verification
Run focused Food ViewModel tests, then `testDebugUnitTest lintDebug assembleDebug` before commit and push.
