# Design Language Phase 4 — Food Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Bring the Food diary onto the shared design language: replace the edge-to-edge Emerald gradient hero (`FoodSummaryHeader`) with a `MusFitScreenHeader` ("Food" title + the overflow menu as its action) plus a contained Emerald `MusFitSummaryCard` holding the date navigation, calorie ring, and Eaten/Goal metrics.

**Architecture:** Presentational. Reuses `MusFitScreenHeader` + `MusFitSummaryCard`. Food's diary keeps its existing custom scroll `Column` (it needs bottom padding for the FAB), so it uses `MusFitScreenHeader` directly (not the full `MusFitScreenScaffold`). Edits only `FoodScreen.kt`. No ViewModel/Room/nav changes.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, existing `MusFitTheme` / `TabAccent` (Food accent = Emerald; `MusFitColors.brand` is that same Emerald).

**Testing note:** No Compose UI tests in this repo. Verify = `testDebugUnitTest lintDebug assembleDebug` passes (the large `FoodViewModelTest` + repository suites stay green — behavior-preserving) + light/dark device screenshot. No new UI unit tests.

**Design rules from the spec** ([2026-07-01-design-language-consistency-design.md](../specs/2026-07-01-design-language-consistency-design.md)): shared header (title + neutral trailing action icon); summary-card bg = `TabAccent.container`, **primary text = `onSurface`** (legibility rule); ring track uses a tint-safe neutral (`onSurface.copy(alpha = 0.12f)`), progress stroke uses the accent (`MusFitColors.brand`); one accent per screen (Emerald); date navigation lives in the summary card, not a gradient hero.

**Scope note:** This converts the Food **diary hero** only. The rest of the Food screen (meal sections, add-food sheets, the many modal panels) keeps its current structure and styling — a deeper token/component sweep there is out of scope for this phase. This is a big screen (`FoodScreen.kt` ~2,000 lines); follow the existing `FoodAddMode`/`FoodSheetMode` conventions and touch only the hero + its helpers.

**Before Gradle** (Windows PowerShell), source env from the **main tree**:
```powershell
. 'C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1'
```
---

## File Structure

- **Modify** `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`:
  - The diary branch's root `Column` (~lines 184–240): drop the edge-to-edge `FoodSummaryHeader(...)` call; render `MusFitScreenHeader` + a new `FoodDiarySummaryCard` at the top of the padded content column; compute the Emerald `accent`.
  - Delete `FoodSummaryHeader` (~lines 637–722); add `FoodDiaryOverflowAction` and `FoodDiarySummaryCard`.
  - Recolor `SummarySideMetric` (~724–741) and `CalorieRing` (~785–830) from `brandInk` to `onSurface` / `brand` / tint-safe track.

---

## Task 1: Food diary hero → shared header + contained Emerald summary card

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`

Read the file first (at least the `FoodScreen` composable ~98–260, `FoodSummaryHeader` ~637–722, `SummarySideMetric` ~724–741, `CalorieRing` ~785–830). Confirm exact line numbers before editing.

### Step 1 — Imports + accent

Ensure these imports exist in `FoodScreen.kt` (add any missing):
```kotlin
import com.musfit.ui.AppDestination
import com.musfit.ui.components.MusFitScreenHeader
import com.musfit.ui.components.MusFitSummaryCard
import com.musfit.ui.theme.TabAccent
import com.musfit.ui.theme.tabAccentFor
```
In the `FoodScreen` composable body, where `state` is collected, add (if not already present):
```kotlin
    val accent = tabAccentFor(AppDestination.Food)
```

### Step 2 — Add `FoodDiaryOverflowAction` (the overflow menu, for the header action slot)

Add this private composable (it reproduces the exact menu that was in `FoodSummaryHeader`, tinted neutral):
```kotlin
@Composable
private fun FoodDiaryOverflowAction(
    state: FoodUiState,
    onGoalClick: () -> Unit,
    onMealsClick: () -> Unit,
    onTemplatesClick: () -> Unit,
    onRecipeClick: () -> Unit,
    onShoppingClick: () -> Unit,
    onFastingClick: () -> Unit,
    onPlanningModeClick: () -> Unit,
    onCopyDayToTomorrowClick: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    IconButton(onClick = { menuOpen = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = "More actions", tint = MusFitTheme.colors.onSurfaceVariant)
    }
    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
        DropdownMenuItem(text = { Text("Goals") }, onClick = { menuOpen = false; onGoalClick() })
        DropdownMenuItem(text = { Text("Meals") }, onClick = { menuOpen = false; onMealsClick() })
        DropdownMenuItem(text = { Text("Templates") }, onClick = { menuOpen = false; onTemplatesClick() })
        DropdownMenuItem(text = { Text("Recipes") }, onClick = { menuOpen = false; onRecipeClick() })
        DropdownMenuItem(text = { Text("Shopping list") }, onClick = { menuOpen = false; onShoppingClick() })
        DropdownMenuItem(text = { Text("Fasting") }, onClick = { menuOpen = false; onFastingClick() })
        DropdownMenuItem(
            text = { Text(if (state.isPlanningMode) "Planning: on" else "Planning: off") },
            onClick = { menuOpen = false; onPlanningModeClick() },
        )
        DropdownMenuItem(
            text = { Text("Copy day to tomorrow") },
            enabled = !state.isSaving,
            onClick = { menuOpen = false; onCopyDayToTomorrowClick() },
        )
    }
}
```

### Step 3 — Add `FoodDiarySummaryCard` (Emerald `MusFitSummaryCard`: date nav + ring + metrics)

```kotlin
@Composable
private fun FoodDiarySummaryCard(
    state: FoodUiState,
    accent: TabAccent,
    onPreviousDayClick: () -> Unit,
    onNextDayClick: () -> Unit,
    onTodayClick: () -> Unit,
) {
    MusFitSummaryCard(accent = accent) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPreviousDayClick) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous day", tint = MusFitTheme.colors.onSurface)
                }
                Text(
                    text = state.selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("EEE · d MMM")),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MusFitTheme.colors.onSurface,
                    modifier = Modifier.clickable(onClick = onTodayClick),
                )
                IconButton(onClick = onNextDayClick) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Next day", tint = MusFitTheme.colors.onSurface)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SummarySideMetric(label = "Eaten", value = state.eatenCaloriesKcal)
                CalorieRing(
                    eatenCalories = state.eatenCaloriesKcal,
                    remainingCalories = state.remainingCaloriesKcal,
                    calorieGoal = state.calorieGoalKcal,
                )
                SummarySideMetric(label = "Goal", value = state.calorieGoalKcal)
            }
        }
    }
}
```

### Step 4 — Recolor `SummarySideMetric` for the tint

Replace its two `Text` colors: the label from `MusFitTheme.colors.brandInk.copy(alpha = 0.76f)` to `MusFitTheme.colors.onSurface.copy(alpha = 0.7f)`, and the value from `MusFitTheme.colors.brandInk` to `MusFitTheme.colors.onSurface`. Leave everything else.

### Step 5 — Recolor `CalorieRing` for the tint

In `CalorieRing`, replace the `val brandInk = MusFitTheme.colors.brandInk` line with:
```kotlin
    val trackColor = MusFitTheme.colors.onSurface.copy(alpha = 0.12f)
    val progressColor = MusFitTheme.colors.brand
```
Then: the **track** `drawArc` color becomes `trackColor` (was `brandInk.copy(alpha = 0.22f)`); the **progress** `drawArc` color becomes `progressColor` (was `brandInk.copy(alpha = 0.92f)`); and every center-label `Text` inside `CalorieRing` that used `MusFitTheme.colors.brandInk` becomes `MusFitTheme.colors.onSurface`. (Read the whole `CalorieRing` body and update each `brandInk` reference accordingly; there are the two arcs plus the center remaining-calories number and any sub-label.)

### Step 6 — Swap the hero at the call site

In the diary branch's root `Column` (the `else { Column( ... verticalScroll ... padding(bottom = 96.dp) ...) { FoodSummaryHeader(...) ; Column(padding(horizontal = 16.dp), spacedBy(16.dp)) { MacroProgressRow(...) ; ... } } }`):

1. **Delete** the entire `FoodSummaryHeader( ... )` call (all its arguments).
2. Change the inner content `Column`'s modifier padding from `Modifier.padding(horizontal = 16.dp)` to `Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)` (gives the header top breathing room now that the edge-to-edge hero is gone).
3. As the **first two children** of that inner content `Column` (before `MacroProgressRow(...)`), add:
```kotlin
                    MusFitScreenHeader(
                        title = "Food",
                        actions = {
                            FoodDiaryOverflowAction(
                                state = state,
                                onGoalClick = viewModel::openGoalEditor,
                                onMealsClick = viewModel::openMealSettings,
                                onTemplatesClick = viewModel::openMealTemplates,
                                onRecipeClick = viewModel::openRecipeEditor,
                                onShoppingClick = viewModel::openShoppingList,
                                onFastingClick = viewModel::openFastingTimer,
                                onPlanningModeClick = viewModel::togglePlanningMode,
                                onCopyDayToTomorrowClick = viewModel::copySelectedDayToTomorrow,
                            )
                        },
                    )
                    FoodDiarySummaryCard(
                        state = state,
                        accent = accent,
                        onPreviousDayClick = viewModel::goToPreviousDay,
                        onNextDayClick = viewModel::goToNextDay,
                        onTodayClick = viewModel::goToToday,
                    )
```
Use the **exact same** action bindings the old `FoodSummaryHeader(...)` call passed for each menu item (verify them at that call site and copy them verbatim — the ones shown above are those bindings). The old `onQuickAddClick` parameter: if `FoodSummaryHeader` did not actually use it in its body (search the old composable), drop it — do not wire a quick-add that didn't exist in the hero. Do not change any `viewModel::` method being called.

### Step 7 — Delete the old `FoodSummaryHeader`

Remove the entire `@Composable private fun FoodSummaryHeader(...) { ... }` (the old edge-to-edge gradient hero). Its date nav, overflow menu, and metrics now live in `FoodDiaryOverflowAction` + `FoodDiarySummaryCard`.

### Step 8 — Verify
```powershell
. 'C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1'; if ($?) { .\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain }
```
Expected: `BUILD SUCCESSFUL`, all existing tests green (no test changes — behavior-preserving), lint clean.

Also confirm no now-unused imports remain (e.g. if `Brush`/`brandGradient` were used only by the deleted `FoodSummaryHeader`, remove the unused import — but verify by search first; `brandInk`/`brandGradient` may still be used elsewhere in the file).

### Step 9 — Device smoke (best-effort) + Commit
If `adb devices` shows a device, install + launch, open the Food tab, tap date chevrons and the overflow menu to confirm no crash (final light/dark visual sign-off is the human's).
```powershell
git add app/src/main/java/com/musfit/ui/food/FoodScreen.kt; git commit -m "feat(food): shared header and contained Emerald summary card"
```

---

## Definition of done (Phase 4)

- The Food diary shows a "Food" title header (with the overflow menu as its action) and a contained Emerald `MusFitSummaryCard` (date nav + calorie ring + Eaten/Goal) instead of the edge-to-edge gradient hero.
- Date navigation, the overflow menu actions, and the calorie ring/metrics all behave exactly as before.
- `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug` passes; `FoodViewModelTest` and the rest stay green and unchanged.
- Food verified in light and dark on device.
- All four tabs now share the design language.
