# Slice 20: AI Logging Shell

## Goal
Add an AI logging shell inside the Food add flow without introducing real AI, accounts, uploads, or automatic saves.

## Scope
- Add an `AI` add mode alongside Saved, Manual, Barcode, and Quick.
- Support text entry plus voice and photo placeholders.
- Generate deterministic editable draft fields for food name, amount, calories, protein, carbs, and fat.
- Require the user to review and tap the existing log action before anything is saved.

## Tests First
- ViewModel test: text AI draft fills editable fields and does not call the repository until `logFood()`.
- ViewModel test: voice/photo placeholders create review drafts and do not save automatically.

## Implementation Notes
- Reuse existing `ProductFields`, `NutritionFields`, and `logFood()` behavior.
- Keep drafts local UI state only.
- No camera, microphone, network, or LLM calls in this slice.

## Verification
Run focused Food ViewModel tests, then the full `testDebugUnitTest lintDebug assembleDebug` gate before committing and pushing.
