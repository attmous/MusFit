# Design Language Phase 1 — Foundation + Today Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract the first shared design-language composables (`MusFitScreenScaffold`, `MusFitScreenHeader`, `MusFitSummaryCard`) and convert the Today tab to use them, establishing the pattern the other three tabs will follow.

**Architecture:** Purely presentational. Two new files in `com.musfit.ui.components` hold the shared scaffold/header and the summary card, both driven by the existing `MusFitTheme` tokens and `TabAccent`. Today's screen swaps its bespoke header `Row` for `MusFitScreenScaffold` and its `DailyRingsCard` for a `MusFitSummaryCard`. No ViewModel, repository, Room, or navigation changes.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, existing `MusFitTheme` / `TabAccent` token layer.

**Testing note:** This codebase has no Compose UI tests — only ViewModel/repository/domain JUnit tests. These composables have no unit-test seam, so each task is verified by: it compiles (`assembleDebug`), lint is clean (`lintDebug`), the existing unit suite stays green (`testDebugUnitTest`), and a manual light/dark device screenshot check. No new UI unit tests are written (there is no pure logic introduced in this phase). This is consistent with CLAUDE.md, which scopes TDD to behavior changes; this phase changes none.

**Scope note:** This is Phase 1 of the four-phase rollout in
[`docs/superpowers/specs/2026-07-01-design-language-consistency-design.md`](../specs/2026-07-01-design-language-consistency-design.md).
Phases 2 (Training), 3 (Profile), and 4 (Food) each get their own plan once these
shared components are proven here. The remaining component-language primitives
(`SectionHeader`, segmented control, button tiers, chip, `EmptyState`) are built
in the phase whose tab first needs them, per the spec's Section 5 preamble — not
speculatively here.

**Before any Gradle command**, source the toolchain env in the PowerShell session (per CLAUDE.md / AGENTS.md):

```powershell
. .\.superpowers\sdd\android-env.ps1
```

If Gradle fails on `app/build` with `AccessDeniedException` / `Cannot snapshot` (OneDrive flakiness), recover per AGENTS.md (`.\gradlew.bat --stop`, wait, delete `app\build`) and rerun.

---

## File Structure

- **Create** `app/src/main/java/com/musfit/ui/components/MusFitScaffold.kt` — `MusFitScreenHeader` (title + optional subtitle + trailing actions) and `MusFitScreenScaffold` (background + scroll + padding + header + content slot). One responsibility: the shared screen chrome.
- **Create** `app/src/main/java/com/musfit/ui/components/MusFitSummaryCard.kt` — `MusFitSummaryCard`, the reusable accent-tinted "headline moment" card.
- **Modify** `app/src/main/java/com/musfit/ui/today/TodayScreen.kt` — root screen uses `MusFitScreenScaffold`; `DailyRingsCard` uses `MusFitSummaryCard`; `MacroBar` gains an accent label color.

---

## Task 1: Shared screen scaffold + header

**Files:**
- Create: `app/src/main/java/com/musfit/ui/components/MusFitScaffold.kt`

- [ ] **Step 1: Create the scaffold + header file**

Create `app/src/main/java/com/musfit/ui/components/MusFitScaffold.kt` with exactly:

```kotlin
package com.musfit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.musfit.ui.theme.MusFitTheme

/**
 * The shared tab header: title (left) + optional muted subtitle + trailing icon
 * actions (right). One idiom for every tab — same type, height, and alignment.
 */
@Composable
fun MusFitScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MusFitTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MusFitTheme.colors.onSurface,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MusFitTheme.typography.bodyMedium,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/**
 * The shared scrolling screen container: cream background, standard edge padding,
 * a [MusFitScreenHeader] at the top, then a vertically-spaced content slot. Tabs
 * that need a non-scrolling or lazy container use [MusFitScreenHeader] directly.
 */
@Composable
fun MusFitScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MusFitTheme.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(MusFitTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(MusFitTheme.spacing.lg),
    ) {
        MusFitScreenHeader(title = title, subtitle = subtitle, actions = actions)
        content()
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run:

```powershell
. .\.superpowers\sdd\android-env.ps1
.\gradlew.bat assembleDebug --no-daemon --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```powershell
git add app/src/main/java/com/musfit/ui/components/MusFitScaffold.kt
git commit -m "feat(ui): add shared screen scaffold and header"
```

---

## Task 2: Shared summary card

**Files:**
- Create: `app/src/main/java/com/musfit/ui/components/MusFitSummaryCard.kt`

- [ ] **Step 1: Create the summary card file**

Create `app/src/main/java/com/musfit/ui/components/MusFitSummaryCard.kt` with exactly:

```kotlin
package com.musfit.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent

/**
 * The reusable "headline moment" card: a contained (inset), soft accent-tinted
 * card each tab fills with its key stat. Background = [TabAccent.container]; place
 * text with [TabAccent.onContainer] and figure strokes with [TabAccent.color].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusFitSummaryCard(
    accent: TabAccent,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (onClick != null) {
        Surface(
            onClick = onClick,
            color = accent.container,
            shape = MusFitTheme.shapes.large,
            modifier = modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(MusFitTheme.spacing.lg), content = content)
        }
    } else {
        Surface(
            color = accent.container,
            shape = MusFitTheme.shapes.large,
            modifier = modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(MusFitTheme.spacing.lg), content = content)
        }
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run:

```powershell
.\gradlew.bat assembleDebug --no-daemon --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```powershell
git add app/src/main/java/com/musfit/ui/components/MusFitSummaryCard.kt
git commit -m "feat(ui): add shared accent-tinted summary card"
```

---

## Task 3: Convert Today to the shared scaffold + summary card

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/today/TodayScreen.kt` — the `TodayScreen` body (root `Column` + header `Row`), `DailyRingsCard`, and `MacroBar`.

Context: `TodayScreen`'s root is currently a `Column(fillMaxSize + background + verticalScroll + padding(16), spacedBy(16))` whose first child is a header `Row` (title `Column` + `IconButton(Tune)`). `DailyRingsCard` is a `Surface(color = surface, shape = extraLarge)`. We replace the root+header with `MusFitScreenScaffold`, and retint the rings card via `MusFitSummaryCard`.

- [ ] **Step 1: Add the imports**

In `TodayScreen.kt`, add these imports alongside the existing ones:

```kotlin
import androidx.compose.ui.graphics.Color
import com.musfit.ui.components.MusFitScreenScaffold
import com.musfit.ui.components.MusFitSummaryCard
```

(If `androidx.compose.ui.graphics.Color` is already imported, do not duplicate it.)

After Steps 2–4 below, the root screen no longer scrolls or sizes itself (the scaffold does), so remove any now-unused imports from `TodayScreen.kt` — expected to be `androidx.compose.foundation.verticalScroll`, `androidx.compose.foundation.rememberScrollState`, and `androidx.compose.foundation.layout.fillMaxSize` (keep `background`, `padding`, `fillMaxWidth`, etc., which other composables in the file still use). Verify by search before deleting each.

- [ ] **Step 2: Replace the root Column + header Row with `MusFitScreenScaffold`**

Replace this block (the opening of the `Column` through the end of the header `Row`, currently lines ~71–100):

```kotlin
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MusFitTheme.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Today",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MusFitTheme.colors.onSurface,
                )
                Text(
                    text = state.dateLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }
            IconButton(onClick = viewModel::openGoalsEditor) {
                Icon(Icons.Outlined.Tune, contentDescription = "Edit goals", tint = MusFitTheme.colors.onSurfaceVariant)
            }
        }
```

with:

```kotlin
    MusFitScreenScaffold(
        title = "Today",
        subtitle = state.dateLabel,
        actions = {
            IconButton(onClick = viewModel::openGoalsEditor) {
                Icon(Icons.Outlined.Tune, contentDescription = "Edit goals", tint = MusFitTheme.colors.onSurfaceVariant)
            }
        },
    ) {
```

The rest of the body (the `state.coach?.let { ... }`, `val accent = tabAccentFor(...)`, `DailyRingsCard(...)`, `state.weeklyCharts?.let { ... }`, and the trailing `GlimpseTile` `Row`) stays exactly as-is inside this new trailing content lambda, and the existing closing `}` of the old `Column` now closes the scaffold's content lambda.

- [ ] **Step 3: Retint `DailyRingsCard` via `MusFitSummaryCard`**

Replace the whole `DailyRingsCard` composable (currently lines ~155–197) with:

```kotlin
@Composable
private fun DailyRingsCard(
    rings: List<DailyRingUiState>,
    macros: MacroBreakdownUiState,
    onClick: () -> Unit,
) {
    val accent = tabAccentFor(AppDestination.Today)
    MusFitSummaryCard(accent = accent, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            rings.forEach { ring ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MetricRing(progress = ring.progress, color = ringColor(ring.kind)) {
                        Text(
                            text = "${(ring.progress * 100).roundToInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = accent.onContainer,
                        )
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = ring.kind.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = accent.onContainer,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        MacroBar(macros = macros, labelColor = accent.onContainer)
    }
}
```

Note: the `@OptIn(ExperimentalMaterial3Api::class)` that was on the old `DailyRingsCard` is no longer needed here (the clickable `Surface` now lives inside `MusFitSummaryCard`), so it is intentionally dropped from this composable.

- [ ] **Step 4: Give `MacroBar` an accent label color**

Replace the `MacroBar` signature and its summary `Text` color (currently lines ~199–221). Change the signature from:

```kotlin
@Composable
private fun MacroBar(macros: MacroBreakdownUiState) {
```

to:

```kotlin
@Composable
private fun MacroBar(macros: MacroBreakdownUiState, labelColor: Color = MusFitTheme.colors.onSurfaceVariant) {
```

and change the trailing summary `Text`'s color from `color = MusFitTheme.colors.onSurfaceVariant,` to `color = labelColor,`. Everything else in `MacroBar` (the macro-colored bars) stays unchanged.

- [ ] **Step 5: Verify full build, lint, and the existing test suite**

Run:

```powershell
. .\.superpowers\sdd\android-env.ps1
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain
```

Expected: `BUILD SUCCESSFUL`, all existing unit tests pass (Today/Food/Training/Profile ViewModel + repository suites), lint clean. Behavior is unchanged, so no test should change.

- [ ] **Step 6: Visual check on device (light + dark)**

Install and launch, then screenshot the Today tab in both light and dark mode and confirm: the header title/subtitle/goals-icon match the other tabs' intended layout, and the rings card is now a soft-Coral tinted card with legible `%`/labels and macro summary. Use the device serial and `adb pull` screenshot workflow from AGENTS.md / memory.

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell monkey -p com.musfit -c android.intent.category.LAUNCHER 1
```

- [ ] **Step 7: Commit**

```powershell
git add app/src/main/java/com/musfit/ui/today/TodayScreen.kt
git commit -m "feat(today): adopt shared scaffold and summary card"
```

---

## Definition of done (Phase 1)

- `MusFitScreenScaffold`, `MusFitScreenHeader`, and `MusFitSummaryCard` exist in `com.musfit.ui.components` and compile.
- Today renders through `MusFitScreenScaffold`; its rings card is a Coral `MusFitSummaryCard`.
- `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug` passes; existing tests unchanged and green.
- Today verified in light and dark on device.
- The shared components are ready for Phase 2 (Training) to reuse.
