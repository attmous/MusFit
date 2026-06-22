# Food Add Flow — Slice C (Label OCR) Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax.

**Goal:** On-device nutrition-label OCR: a CameraX + ML Kit text-recognition capture screen whose recognized text is parsed (best-effort, English + German) into per-100 g macros that autofill the Create tab for review.

**Architecture:** Pure `NutritionLabelParser` in `domain/` (unit-tested, no Android). A `NutritionLabelScannerScreen` clones the `BarcodeScannerScreen` CameraX setup but uses `TextRecognition` and returns the recognized text on a Capture tap. A nav route carries the text back to `FoodScreen`, which calls `FoodViewModel.onScannedLabel(text)` → parse + populate the Create form.

**Tech Stack:** CameraX 1.6.1, ML Kit text-recognition 16.0.1 (bundled Latin model, on-device), Compose, Hilt.

---

### Task 1: Add ML Kit text-recognition dependency

**Files:** `gradle/libs.versions.toml`, `app/build.gradle.kts`

- [ ] **Step 1.** In `[versions]` add `mlkitText = "16.0.1"`. In `[libraries]` add `mlkit-text = { module = "com.google.mlkit:text-recognition", version.ref = "mlkitText" }`.
- [ ] **Step 2.** In `app/build.gradle.kts` next to `implementation(libs.mlkit.barcode)` add `implementation(libs.mlkit.text)`.
- [ ] **Step 3.** `.\gradlew.bat help --no-daemon --console=plain` (or rely on next build) to confirm the catalog resolves.

### Task 2: `NutritionLabelParser` (pure domain, TDD)

**Files:**
- Create: `app/src/main/java/com/musfit/domain/food/NutritionLabelParser.kt`
- Test: `app/src/test/java/com/musfit/domain/food/NutritionLabelParserTest.kt`

- [ ] **Step 1 — failing tests.** Cover: (a) an English label block (Energy 250 kcal, Fat 12g, Carbohydrate 30g, Protein 8g) → all four parsed; (b) a German label with comma decimals (`Brennwert ... 250 kcal`, `Fett 12,5 g`, `Kohlenhydrate 30 g`, `davon gesättigte Fettsäuren 5 g`, `Eiweiß 8 g`) → fat = 12.5 (NOT the saturated 5), all four parsed; (c) an energy line `1046 kJ / 250 kcal` → calories = 250; (d) text with no nutrition words → all null, `hasAnyValue == false`.
- [ ] **Step 2 — run red.** `.\gradlew.bat testDebugUnitTest --tests "com.musfit.domain.food.NutritionLabelParserTest" --no-daemon --console=plain` → FAIL.
- [ ] **Step 3 — implement.**
```kotlin
package com.musfit.domain.food

data class ParsedNutritionLabel(
    val caloriesKcal: Double? = null,
    val proteinGrams: Double? = null,
    val carbsGrams: Double? = null,
    val fatGrams: Double? = null,
) {
    val hasAnyValue: Boolean
        get() = listOfNotNull(caloriesKcal, proteinGrams, carbsGrams, fatGrams).isNotEmpty()
}

object NutritionLabelParser {
    private val NUMBER = Regex("""\d+(?:[.,]\d+)?""")
    private val KCAL = Regex("""(\d+(?:[.,]\d+)?)\s*kcal""")

    fun parse(rawText: String): ParsedNutritionLabel {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        return ParsedNutritionLabel(
            caloriesKcal = calories(lines),
            proteinGrams = firstNumberOnLineMatching(lines, listOf("protein", "eiwei")),
            carbsGrams = firstNumberOnLineMatching(lines, listOf("carbohydrate", "kohlenhydrate", "carbs")),
            fatGrams = fat(lines),
        )
    }

    private fun calories(lines: List<String>): Double? {
        lines.forEach { line ->
            KCAL.find(line.lowercase())?.let { return it.groupValues[1].toNum() }
        }
        return null
    }

    private fun fat(lines: List<String>): Double? {
        val isSaturated = { l: String -> listOf("satur", "gesätt", "gesatt", "davon", "of which").any { it in l } }
        val matches = lines.filter { val l = it.lowercase(); ("fat" in l || "fett" in l) }
        return (matches.firstOrNull { !isSaturated(it.lowercase()) } ?: matches.firstOrNull())
            ?.let { firstNumber(it) }
    }

    private fun firstNumberOnLineMatching(lines: List<String>, keywords: List<String>): Double? {
        val line = lines.firstOrNull { val l = it.lowercase(); keywords.any { k -> k in l } } ?: return null
        return firstNumber(line)
    }

    private fun firstNumber(line: String): Double? = NUMBER.find(line)?.value?.toNum()

    private fun String.toNum(): Double? = replace(',', '.').toDoubleOrNull()
}
```
- [ ] **Step 4 — run green.** Same test command → PASS.
- [ ] **Step 5 — commit** (`feat(food): add on-device nutrition-label parser`).

### Task 3: ViewModel `onScannedLabel` autofills Create (TDD)

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/food/FoodViewModel.kt`
- Test: `app/src/test/java/com/musfit/ui/food/FoodViewModelTest.kt`

- [ ] **Step 1 — failing test.** Call `viewModel.onScannedLabel("Energy 250 kcal\nFat 12 g\nCarbohydrate 30 g\nProtein 8 g")`, then assert `state.addTab == AddTab.Create`, `state.caloriesPer100g == "250"`, `state.proteinPer100g == "8"`, `state.carbsPer100g == "30"`, `state.fatPer100g == "12"`.
- [ ] **Step 2 — run red.**
- [ ] **Step 3 — implement.** Replace `startLabelScanPlaceholder()` with:
```kotlin
fun onScannedLabel(rawText: String) {
    val parsed = NutritionLabelParser.parse(rawText)
    mutableState.update {
        it.copy(
            isAddPanelVisible = true,
            sheetMode = FoodSheetMode.AddFood,
            addMode = FoodAddMode.Saved,
            addTab = AddTab.Create,
            caloriesPer100g = parsed.caloriesKcal?.formatInputNumber() ?: it.caloriesPer100g,
            proteinPer100g = parsed.proteinGrams?.formatInputNumber() ?: it.proteinPer100g,
            carbsPer100g = parsed.carbsGrams?.formatInputNumber() ?: it.carbsPer100g,
            fatPer100g = parsed.fatGrams?.formatInputNumber() ?: it.fatPer100g,
            message = if (parsed.hasAnyValue) "Review the scanned values below." else "Couldn't read the label — enter values manually.",
        ).withAmountNutritionPreview()
    }
}
```
Add `import com.musfit.domain.food.NutritionLabelParser`. (`formatInputNumber` already used in this file.)
- [ ] **Step 4 — run green.**

### Task 4: `NutritionLabelScannerScreen` composable

**Files:** Create `app/src/main/java/com/musfit/ui/food/NutritionLabelScannerScreen.kt`

- [ ] **Step 1.** Clone `BarcodeScannerScreen` structure (permission flow, `ProcessCameraProvider`, `PreviewView`, `ImageAnalysis`, `DisposableEffect`s). Differences:
  - Recognizer: `TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)`.
  - Analyzer: on each frame, `recognizer.process(image).addOnSuccessListener { latestText = it.text }` (keep latest; do not auto-finish).
  - UI overlay: a hint "Point at the nutrition label" + a **Capture** `Button` (enabled when `latestText` is non-blank) that, once, calls `onLabelCaptured(latestText)` then unbinds.
  - Signature: `fun NutritionLabelScannerScreen(onLabelCaptured: (String) -> Unit)`.
- [ ] **Step 2.** Imports: `com.google.mlkit.vision.text.TextRecognition`, `com.google.mlkit.vision.text.latin.TextRecognizerOptions` (+ the CameraX/permission imports from BarcodeScannerScreen).

### Task 5: Nav route + wire label scan end-to-end

**Files:** `app/src/main/java/com/musfit/ui/AppNavGraph.kt`, `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`

- [ ] **Step 1.** In `AppNavGraph.kt` add `const val NUTRITION_LABEL_SCANNER_ROUTE = "nutrition_label_scanner"`, a `scannedLabelText` state, pass `scannedLabelText` + `onLabelScanClick = { navController.navigate(NUTRITION_LABEL_SCANNER_ROUTE) }` + `onScannedLabelConsumed = { scannedLabelText = null }` to `FoodScreen`, and add a `composable(NUTRITION_LABEL_SCANNER_ROUTE) { NutritionLabelScannerScreen(onLabelCaptured = { text -> scannedLabelText = text; navController.popBackStack() }) }` (mirror the barcode route).
- [ ] **Step 2.** `FoodScreen` gains params `scannedLabelText: String? = null`, `onLabelScanClick: () -> Unit = {}`, `onScannedLabelConsumed: () -> Unit = {}`. Add a `LaunchedEffect(scannedLabelText)` → `if (!scannedLabelText.isNullOrBlank()) { viewModel.onScannedLabel(scannedLabelText); onScannedLabelConsumed() }`.
- [ ] **Step 3.** Change the `AddFoodScreen(...)` wiring `onScanLabel = viewModel::startLabelScanPlaceholder` → `onScanLabel = onLabelScanClick`.

### Task 6: Verify + commit

- [ ] **Step 1.** `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain` → green.
- [ ] **Step 2.** On-device: Create → Scan label → camera → Capture → returns to Create with macros filled for review → Log food.
- [ ] **Step 3.** Commit (`feat(food): on-device nutrition-label OCR into Create`).

## Risk / notes
- Parser is best-effort and always lands on the editable Create form; never auto-saves. The `message` tells the user to review.
- Camera permission already requested by the barcode flow; the label scanner reuses the same `Manifest.permission.CAMERA` runtime request.
