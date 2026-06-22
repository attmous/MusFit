# Food Add Flow — Slice B (Create tab) Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Fold manual food creation + barcode autofill + a meal/recipe entry point into an inline **Create** tab in `AddFoodScreen`, and route barcode scans into that Create tab (autofilled) instead of the old bottom sheet.

**Architecture:** Reuse the existing manual/barcode machinery (`ProductFields`, `NutritionFields`, `BarcodeLookupSummary`, `logFood`, `saveScannedProductToDatabase`, `lookupBarcode`). Expose one new `internal` composable `CreateFoodForm` from `FoodScreen.kt` that the (separate-file) `AddFoodScreen` calls. Add a small ViewModel entry point `onScannedBarcode` so scanning routes to the Create tab with auto-lookup.

**Tech Stack:** Jetpack Compose, Hilt, existing FoodViewModel/FoodRepository.

---

### Task 1: ViewModel — `onScannedBarcode` routes scans into Create (TDD)

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/food/FoodViewModel.kt`
- Test: `app/src/test/java/com/musfit/ui/food/FoodViewModelTest.kt`

- [ ] **Step 1 — failing test.** With the existing `FakeProductProvider` returning a `Found` product, assert that calling `viewModel.onScannedBarcode("4001234567890")` then advancing the dispatcher yields `state.addTab == AddTab.Create`, `state.isAddPanelVisible`, `state.sheetMode == FoodSheetMode.AddFood`, `state.addMode == FoodAddMode.Saved`, and that `state.productName` / `state.caloriesPer100g` are autofilled from the fake product.
- [ ] **Step 2 — run red.** `.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.food.FoodViewModelTest" --no-daemon --console=plain` → FAIL (unresolved `onScannedBarcode`).
- [ ] **Step 3 — implement.** Add to FoodViewModel:
```kotlin
/** Entry point when the barcode scanner returns: show the Create tab and auto-look-up. */
fun onScannedBarcode(barcode: String) {
    onBarcodeChanged(barcode)
    mutableState.update {
        it.copy(
            isAddPanelVisible = true,
            sheetMode = FoodSheetMode.AddFood,
            addMode = FoodAddMode.Saved,
            addTab = AddTab.Create,
            message = null,
        )
    }
    lookupBarcode()
}
```
- [ ] **Step 4 — run green.** Same test command → PASS.
- [ ] **Step 5 — commit** (`feat(food): route barcode scans into the Create tab`).

### Task 2: `CreateFoodForm` composable (internal, in FoodScreen.kt)

**Files:** Modify `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`

- [ ] **Step 1.** Add imports: `androidx.compose.material.icons.outlined.QrCodeScanner`, `androidx.compose.material.icons.outlined.DocumentScanner`, `androidx.compose.material3.TextButton`.
- [ ] **Step 2.** Add `internal fun CreateFoodForm(...)` near `BarcodeFoodForm`. Params: `state`, `onScanBarcode`, `onScanLabel`, the eight field handlers (`onProductNameChanged`, `onBrandChanged`, `onQuantityChanged`, `onAmountServingChoiceSelected`, `onCaloriesChanged`, `onProteinChanged`, `onCarbsChanged`, `onFatChanged`), `onSaveProduct`, `onLogFood`, `onCreateRecipe`. Body:
```kotlin
Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onScanBarcode, enabled = !state.isLoading && !state.isSaving, modifier = Modifier.weight(1f)) {
            Icon(Icons.Outlined.QrCodeScanner, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Scan barcode")
        }
        OutlinedButton(onClick = onScanLabel, enabled = !state.isLoading && !state.isSaving, modifier = Modifier.weight(1f)) {
            Icon(Icons.Outlined.DocumentScanner, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Scan label")
        }
    }
    if (state.lookupResult != null) BarcodeLookupSummary(state = state)
    ProductFields(state, onProductNameChanged, onBrandChanged, onQuantityChanged, onAmountServingChoiceSelected)
    NutritionFields(state, onCaloriesChanged, onProteinChanged, onCarbsChanged, onFatChanged)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onSaveProduct, enabled = !state.isLoading && !state.isSaving, modifier = Modifier.weight(1f)) {
            Text(if (state.isSaving) "Saving" else "Save to database")
        }
        Button(onClick = onLogFood, enabled = !state.isLoading && !state.isSaving, modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = MusFitTheme.colors.brand)) {
            Text(if (state.isSaving) "Logging" else "Log food")
        }
    }
    HorizontalDivider(color = MusFitTheme.colors.outline)
    TextButton(onClick = onCreateRecipe, modifier = Modifier.fillMaxWidth()) { Text("Create a meal or recipe instead") }
}
```
(Confirm exact `ProductFields`/`NutritionFields` parameter names against FoodScreen.kt:4624 / 4677 before wiring.)

### Task 3: AddFoodScreen Create tab uses `CreateFoodForm`

**Files:** Modify `app/src/main/java/com/musfit/ui/food/AddFoodScreen.kt`

- [ ] **Step 1.** Replace the `onCreateFood` param with the new param set: `onScanLabel`, the eight field handlers, `onSaveProduct`, `onLogFood`, `onCreateRecipe`.
- [ ] **Step 2.** Replace the `AddTab.Create` stub body with a call to `CreateFoodForm(state = state, onScanBarcode = onScanClick, onScanLabel = onScanLabel, ...handlers..., onSaveProduct = onSaveProduct, onLogFood = onLogFood, onCreateRecipe = onCreateRecipe)`.

### Task 4: Wire FoodScreen + reroute scan entry points

**Files:** Modify `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`

- [ ] **Step 1.** Update the `AddFoodScreen(...)` call: drop `onCreateFood`; add `onScanLabel = viewModel::startLabelScanPlaceholder` (Slice C swaps this to launch OCR capture), `onProductNameChanged = viewModel::onProductNameChanged`, `onBrandChanged = viewModel::onBrandChanged`, `onQuantityChanged = viewModel::onQuantityChanged`, `onAmountServingChoiceSelected = viewModel::onAmountServingChoiceSelected`, `onCaloriesChanged = viewModel::onCaloriesChanged`, `onProteinChanged = viewModel::onProteinChanged`, `onCarbsChanged = viewModel::onCarbsChanged`, `onFatChanged = viewModel::onFatChanged`, `onSaveProduct = viewModel::saveScannedProductToDatabase`, `onLogFood = viewModel::logFood`, `onCreateRecipe = { viewModel.openRecipeEditor(null) }`.
- [ ] **Step 2.** Add `fun startLabelScanPlaceholder()` to FoodViewModel: `mutableState.update { it.copy(addTab = AddTab.Create, message = "Label scanning arrives next — enter the values below for now.") }`. (Replaced in Slice C.)
- [ ] **Step 3.** Change the scanned-barcode `LaunchedEffect` (FoodScreen.kt ~107) to `viewModel.onScannedBarcode(scannedBarcode); onScannedBarcodeConsumed()` (remove the `selectAddMode(FoodAddMode.Barcode)`).
- [ ] **Step 4.** Update the empty-diary `Barcode` action to `viewModel.openAddFood("snacks"); viewModel.selectAddTab(AddTab.Create); onScanClick()` (remove `selectAddMode(Barcode)`).

### Task 5: Verify + commit

- [ ] **Step 1.** `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain` → green.
- [ ] **Step 2.** On-device smoke: Create tab shows form; Scan barcode → returns to Create autofilled; Log food adds to meal.
- [ ] **Step 3.** Commit (`feat(food): inline Create tab with barcode autofill and recipe entry`).
