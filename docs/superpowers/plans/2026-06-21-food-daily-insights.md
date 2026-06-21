# Slice 21: Daily Food Insights

## Goal
Show deterministic coaching-style Food insights without AI or long text.

## Scope
- Derive compact insights from current diary totals, nutrient details, goals, and meal sections.
- Cover protein low, fiber low, sodium high, balanced meal, and what to add next.
- Render a small Food diary section that stays useful when no food is logged.

## Rules
- Keep insights local and deterministic.
- Limit the visible list to a few high-signal items.
- Do not persist insights; they are derived from the current day.

## Tests First
- ViewModel test for low protein/fiber plus suggested next add.
- ViewModel test for sodium warning and balanced meal recognition.

## Verification
Run focused Food ViewModel tests, then `testDebugUnitTest lintDebug assembleDebug` before commit and push.
