# Design Language Phase 2 — Training Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring the Training tab onto the shared design language: adopt `MusFitScreenScaffold` + header, add the Indigo "this week" `MusFitSummaryCard`, and standardize the section switcher via a new shared `MusFitSegmented` control.

**Architecture:** Presentational. Reuses the Phase 1 components (`MusFitScreenScaffold`, `MusFitScreenHeader`, `MusFitSummaryCard`) shipped in `com.musfit.ui.components`. Adds one new shared component (`MusFitSegmented`). Edits only `TrainingScreen.kt` plus the new component file. No ViewModel/Room/nav changes.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, existing `MusFitTheme` / `TabAccent`.

**Testing note:** No Compose UI tests exist in this repo (only ViewModel/repository/domain JUnit). Verification = `testDebugUnitTest lintDebug assembleDebug` passes (existing tests stay green — behavior-preserving) + a light/dark device screenshot check. No new UI unit tests. Consistent with CLAUDE.md (TDD scoped to behavior changes; this is presentational).

**Design rules carried from the spec** ([2026-07-01-design-language-consistency-design.md](../specs/2026-07-01-design-language-consistency-design.md)): shared header (title + optional subtitle + neutral trailing action icons); summary-card background = `TabAccent.container`, **primary text = `MusFitColors.onSurface`** (the legibility rule — not `onContainer`); `spacing.lg` rhythm; one accent per screen (Training = Indigo).

**Scope note:** This phase does the high-impact consistency (header + summary card + section switcher). A deeper token sweep of buttons/chips/empty-states inside Training's content files (`TrainingRoutineContent`, `TrainingHistoryContent`, `TrainingProgressContent`, `TrainingActiveWorkoutContent`) is deferred to a follow-up polish pass to avoid a large risky diff; it is not required for the tabs to read as one app.

**Before any Gradle command** (Windows PowerShell), source the toolchain env — it lives in the **main tree**, not this worktree:

```powershell
. 'C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1'
```

OneDrive `app\build` flakiness (`AccessDeniedException` / `Cannot snapshot`): recover with `.\gradlew.bat --stop`, wait, delete `app\build`, rerun.

---

## File Structure

- **Create** `app/src/main/java/com/musfit/ui/components/MusFitSegmented.kt` — the shared single-select segmented control (accent active / neutral inactive).
- **Modify** `app/src/main/java/com/musfit/ui/training/TrainingScreen.kt` — main (non-active-workout) screen adopts `MusFitScreenScaffold`; new private `TrainingWeekSummaryCard` (Indigo `MusFitSummaryCard`); old `TrainingHeader` deleted; `SectionTabs` delegates to `MusFitSegmented`.

---

## Task 1: Training main screen → scaffold + header + Indigo summary card

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/training/TrainingScreen.kt`

Read the file first. The non-active path's root `Column` is around lines 136–284; `TrainingHeader` is around lines 287–308.

- [ ] **Step 1: Add imports**

Add to `TrainingScreen.kt`:
```kotlin
import com.musfit.ui.components.MusFitScreenScaffold
import com.musfit.ui.components.MusFitSummaryCard
```
(Do not remove `fillMaxSize` / `verticalScroll` / `rememberScrollState` — they are still used by the active-workout path and `ActiveWorkoutPlaceholder`.)

- [ ] **Step 2: Replace the root `Column` + `TrainingHeader` with `MusFitScreenScaffold`**

Replace this block (root `Column` opening through the `SectionTabs(...)` call — currently ~lines 136–163):
```kotlin
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MusFitTheme.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TrainingHeader(
            overview = state.historyOverview,
            accent = accent,
            onHistory = { viewModel.selectSection(TrainingSection.History) },
        )

        state.activeWorkoutSummary?.let { summary ->
            ResumeBanner(
                title = summary.title,
                subtitle = "${summary.completedSetCount} sets · ${summary.totalVolumeKg.formatKg()} kg",
                accent = accent,
                onResume = viewModel::resumeActiveWorkout,
            )
        }

        SectionTabs(
            selected = state.selectedSection,
            accent = accent,
            onSelect = viewModel::selectSection,
        )
```
with:
```kotlin
    MusFitScreenScaffold(
        title = "Training",
        actions = {
            IconButton(onClick = { viewModel.selectSection(TrainingSection.History) }) {
                Icon(Icons.Outlined.History, contentDescription = "Workout history", tint = MusFitTheme.colors.onSurfaceVariant)
            }
        },
    ) {
        state.activeWorkoutSummary?.let { summary ->
            ResumeBanner(
                title = summary.title,
                subtitle = "${summary.completedSetCount} sets · ${summary.totalVolumeKg.formatKg()} kg",
                accent = accent,
                onResume = viewModel::resumeActiveWorkout,
            )
        }

        TrainingWeekSummaryCard(overview = state.historyOverview, accent = accent)

        SectionTabs(
            selected = state.selectedSection,
            accent = accent,
            onSelect = viewModel::selectSection,
        )
```
The rest of the body (`state.message?.let { ... }` and the `when (state.selectedSection) { ... }`) stays unchanged inside the new content lambda; the old root `Column`'s closing `}` (around line 284) now closes the scaffold content lambda — brace balance preserved, do not add/remove a brace. Note the header action icon uses `onSurfaceVariant` (neutral), per the shared rule (the old `TrainingHeader` tinted it with the accent).

- [ ] **Step 3: Delete the old `TrainingHeader` composable**

Remove the entire `TrainingHeader` composable (the `@Composable private fun TrainingHeader(...) { ... }`, ~lines 287–308). It is now replaced by the scaffold header + the summary card.

- [ ] **Step 4: Add `TrainingWeekSummaryCard` + `TrainingWeekStat`**

Add these two private composables to `TrainingScreen.kt` (e.g., where `TrainingHeader` used to be):
```kotlin
@Composable
private fun TrainingWeekSummaryCard(overview: TrainingHistoryOverview, accent: TabAccent) {
    MusFitSummaryCard(accent = accent) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "This week",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MusFitTheme.colors.onSurface,
            )
            if (overview.currentWeekWorkoutCount == 0) {
                Text(
                    text = "No workouts yet — start one whenever you're ready.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MusFitTheme.colors.onSurface,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TrainingWeekStat(
                        value = overview.currentWeekWorkoutCount.toString(),
                        label = if (overview.currentWeekWorkoutCount == 1) "workout" else "workouts",
                    )
                    TrainingWeekStat(value = "${overview.currentWeekVolumeKg.formatKg()} kg", label = "volume")
                    TrainingWeekStat(value = overview.currentStreakDays.toString(), label = "day streak")
                }
            }
        }
    }
}

@Composable
private fun TrainingWeekStat(value: String, label: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MusFitTheme.colors.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MusFitTheme.colors.onSurface,
        )
    }
}
```
(`TrainingHistoryOverview`, `TabAccent`, and the private `Double.formatKg()` extension are all already in this file/package — reuse them, don't import or redefine.)

- [ ] **Step 5: Verify**

```powershell
. 'C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1'; if ($?) { .\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain }
```
Expected: `BUILD SUCCESSFUL`, existing tests green, lint clean. Behavior unchanged.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/musfit/ui/training/TrainingScreen.kt; git commit -m "feat(training): adopt shared scaffold, header, and week summary card"
```

---

## Task 2: Shared `MusFitSegmented` control + adopt in Training

**Files:**
- Create: `app/src/main/java/com/musfit/ui/components/MusFitSegmented.kt`
- Modify: `app/src/main/java/com/musfit/ui/training/TrainingScreen.kt` (the private `SectionTabs`)

- [ ] **Step 1: Create the shared segmented control**

Create `app/src/main/java/com/musfit/ui/components/MusFitSegmented.kt`:
```kotlin
package com.musfit.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent

/**
 * The shared single-select segmented control: accent-tinted active segment, neutral
 * inactive. One switcher styling for every tab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> MusFitSegmented(
    options: List<T>,
    selected: T,
    accent: TabAccent,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    onSelect: (T) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = accent.container,
                    activeContentColor = accent.onContainer,
                    activeBorderColor = accent.color,
                    inactiveContainerColor = MusFitTheme.colors.surface,
                    inactiveContentColor = MusFitTheme.colors.onSurfaceVariant,
                ),
                icon = {},
                label = {
                    Text(
                        text = label(option),
                        style = MusFitTheme.typography.labelMedium,
                        fontWeight = if (option == selected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                    )
                },
            )
        }
    }
}
```

- [ ] **Step 2: Verify it compiles**

```powershell
. 'C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1'; if ($?) { .\gradlew.bat assembleDebug --no-daemon --console=plain }
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit the component**

```powershell
git add app/src/main/java/com/musfit/ui/components/MusFitSegmented.kt; git commit -m "feat(ui): add shared segmented control"
```

- [ ] **Step 4: Delegate Training's `SectionTabs` to `MusFitSegmented`**

In `TrainingScreen.kt`, replace the entire body of the private `SectionTabs` composable (currently ~lines 333–366) with a delegation:
```kotlin
@Composable
private fun SectionTabs(
    selected: TrainingSection,
    accent: TabAccent,
    onSelect: (TrainingSection) -> Unit,
) {
    MusFitSegmented(
        options = TrainingSection.entries,
        selected = selected,
        accent = accent,
        label = { it.name },
        onSelect = onSelect,
    )
}
```
Then:
- Add import `import com.musfit.ui.components.MusFitSegmented`.
- Remove the now-unused imports `androidx.compose.material3.SegmentedButton`, `androidx.compose.material3.SegmentedButtonDefaults`, `androidx.compose.material3.SingleChoiceSegmentedButtonRow` (verify by searching the file — they should have only been used by `SectionTabs`). Keep `ExperimentalMaterial3Api` (still used by other composables such as `FilterChipRow`). Remove the `@OptIn(ExperimentalMaterial3Api::class)` line directly above `SectionTabs` only if nothing in the new delegating body needs it (it doesn't — `MusFitSegmented` carries its own opt-in).

- [ ] **Step 5: Verify**

```powershell
. 'C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1'; if ($?) { .\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain }
```
Expected: `BUILD SUCCESSFUL`, tests green, lint clean.

- [ ] **Step 6: Device smoke (best-effort) + Commit**

If a device is connected (`adb devices`), install + launch to confirm no crash; final light/dark visual sign-off is the human's.
```powershell
git add app/src/main/java/com/musfit/ui/training/TrainingScreen.kt; git commit -m "feat(training): use shared segmented control for section tabs"
```

---

## Definition of done (Phase 2)

- Training main screen renders through `MusFitScreenScaffold` with the neutral history action; the week stats live in an Indigo `MusFitSummaryCard` (not just the header subtitle); the section switcher uses the shared `MusFitSegmented`.
- `MusFitSegmented` exists in `com.musfit.ui.components` for later tabs.
- `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug` passes; existing tests unchanged and green.
- Training verified in light and dark on device.
