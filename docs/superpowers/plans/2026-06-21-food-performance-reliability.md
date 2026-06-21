# Slice 24: Food Performance And Reliability

## Goal
Keep Food responsive and resilient as the saved food database grows and network conditions vary.

## Scope
- Move saved-food database filtering out of the Compose panel and into `FoodUiState`.
- Search saved foods by name, brand, barcode, and category.
- Handle unexpected online search provider failures without crashing the ViewModel.
- Add Room indexes for saved-food lookup fields.

## Rules
- Keep changes local to Food.
- Preserve current online search and saved-food logging behavior.
- Do not add new services or background sync.

## Tests First
- ViewModel test for saved-food filtering by category and barcode.
- ViewModel test for defensive online search exception handling.

## Verification
Run focused Food ViewModel tests, then `testDebugUnitTest lintDebug assembleDebug`.
