# Design Language Phase 3 — Profile Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Bring the Profile tab onto the shared design language: drop the Material `TopAppBar` for `MusFitScreenHeader` (keeping the snackbar), and turn the weight card into a Teal `MusFitSummaryCard`.

**Architecture:** Presentational. Reuses the shared `MusFitScreenHeader` and `MusFitSummaryCard`. Profile keeps a thin `Scaffold` purely for its `SnackbarHost` and renders `MusFitScreenHeader` as the first item in its content column. Edits only `ProfileScreen.kt`. No ViewModel/Room/nav changes.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, existing `MusFitTheme` / `TabAccent` (Profile accent = Teal).

**Testing note:** No Compose UI tests in this repo. Verify = `testDebugUnitTest lintDebug assembleDebug` passes (existing tests stay green — behavior-preserving) + light/dark device screenshot. No new UI unit tests.

**Design rules from the spec** ([2026-07-01-design-language-consistency-design.md](../specs/2026-07-01-design-language-consistency-design.md)): shared header (title + neutral trailing action icon); summary-card bg = `TabAccent.container`, **primary text = `onSurface`** (legibility rule); one accent per screen (Teal); figure strokes tint-safe on the container.

**Scope note:** Core consistency only (header + Teal summary card). The remaining Profile cards (Account/Identity/Goal/Measurements/Vitals) stay as-is (neutral). A deeper button/spacing token sweep is deferred.

**Before Gradle** (Windows PowerShell), source env from the **main tree**:
```powershell
. 'C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1'
```
---

## File Structure

- **Modify** `app/src/main/java/com/musfit/ui/profile/ProfileScreen.kt` — `ProfileScreen` (drop `TopAppBar`, add `MusFitScreenHeader`, compute Teal `accent`); `WeightCard` (→ `MusFitSummaryCard`); `Sparkline` (gains a `color` param).

---

## Task 1: Profile → shared header + Teal weight summary card

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/profile/ProfileScreen.kt`

Read the file first. Relevant spots: the `Scaffold(topBar = { TopAppBar(...) }, snackbarHost = ...) { padding -> Column(...) { AccountCard; IdentityCard; GoalCard; WeightCard; MeasurementsCard; VitalsCard } }` (around lines 83–111); `WeightCard` (~348–381); `Sparkline` (~383–401).

### Part A — Shared header (keep the snackbar)

- [ ] **Step 1: Add imports**
```kotlin
import com.musfit.ui.AppDestination
import com.musfit.ui.components.MusFitScreenHeader
import com.musfit.ui.components.MusFitSummaryCard
import com.musfit.ui.theme.TabAccent
import com.musfit.ui.theme.tabAccentFor
```
(`androidx.compose.ui.graphics.Color` is needed by the `Sparkline` change in Part B — add it too if the file doesn't already import it.)

- [ ] **Step 2: Compute the Teal accent**

At the top of the `ProfileScreen` composable body (e.g. right after `val state by viewModel.state.collectAsState()`), add:
```kotlin
val accent = tabAccentFor(AppDestination.Profile)
```

- [ ] **Step 3: Drop `TopAppBar`, render `MusFitScreenHeader` in the content column**

Remove the entire `topBar = { TopAppBar( title = { Text("Profile") }, actions = { IconButton(onClick = onSettingsClick) { Icon(Icons.Outlined.Settings, contentDescription = "Settings") } }, ) },` argument from the `Scaffold(...)` call (keep `snackbarHost = { SnackbarHost(snackbarHostState) }`).

Then, as the **first child** inside the content `Column { ... }` (before `AccountCard(...)`), add:
```kotlin
            MusFitScreenHeader(
                title = "Profile",
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
            )
```
(The `Column` already has `.padding(padding).verticalScroll(...).padding(16.dp)` — leave that. `MaterialTheme.colorScheme.onSurfaceVariant` maps to the same neutral token the other tabs' header icons use.)

- [ ] **Step 4: Remove now-unused imports/opt-ins**

Remove the `androidx.compose.material3.TopAppBar` import. If `ProfileScreen` (or the file) carried `@OptIn(ExperimentalMaterial3Api::class)` **only** for `TopAppBar`, remove that annotation and the `androidx.compose.material3.ExperimentalMaterial3Api` import too — but first search the file for any other experimental-API usage and keep them if still needed. Let the compiler confirm (build in Step 7).

### Part B — Weight card → Teal `MusFitSummaryCard`

- [ ] **Step 5: Give `Sparkline` a color param**

Change `Sparkline` from:
```kotlin
@Composable
private fun Sparkline(values: List<Double>, modifier: Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
```
to:
```kotlin
@Composable
private fun Sparkline(values: List<Double>, color: Color, modifier: Modifier) {
    Canvas(modifier = modifier) {
```
and inside, change the `drawPath(path, color = lineColor, ...)` call to `drawPath(path, color = color, ...)`. (Delete the now-removed `lineColor` line.)

- [ ] **Step 6: Convert `WeightCard` to `MusFitSummaryCard`**

Replace the whole `WeightCard` composable with:
```kotlin
@Composable
private fun WeightCard(state: ProfileUiState, accent: TabAccent, onLog: () -> Unit, onOpenEntries: () -> Unit) {
    MusFitSummaryCard(accent = accent, onClick = onOpenEntries) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Weight",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onLog) { Text("Log") }
            }
            if (state.latestWeightKg != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${state.latestWeightKg.format1()} kg",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        state.weeklyWeightDeltaKg?.let { delta ->
                            val arrow = if (delta < 0) "▼" else if (delta > 0) "▲" else "•"
                            Text(
                                "$arrow ${abs(delta).format1()} kg this week",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    if (state.weightTrend.size >= 2) {
                        Sparkline(values = state.weightTrend, color = accent.color, modifier = Modifier.width(120.dp).height(36.dp))
                    }
                }
            } else {
                Text(
                    "No weight logged yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
```
(This preserves the card's click-to-open-entries behavior via `onClick`, the inner "Log" action, the value/delta/sparkline, and the empty state. Text is `onSurface` per the legibility rule; the sparkline uses the Teal `accent.color`.)

- [ ] **Step 7: Update the `WeightCard` call site**

In `ProfileScreen`'s content column, change the `WeightCard(...)` call to pass the accent:
```kotlin
            WeightCard(state = state, accent = accent, onLog = { showLogWeight = true }, onOpenEntries = { showWeightSheet = true })
```

- [ ] **Step 8: Verify**
```powershell
. 'C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1'; if ($?) { .\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain }
```
Expected: `BUILD SUCCESSFUL`, tests green, lint clean.

- [ ] **Step 9: Device smoke (best-effort) + Commit**

If `adb devices` shows a device, install + launch to confirm no crash (don't fail the task otherwise).
```powershell
git add app/src/main/java/com/musfit/ui/profile/ProfileScreen.kt; git commit -m "feat(profile): shared header and Teal weight summary card"
```

---

## Definition of done (Phase 3)

- Profile renders through `MusFitScreenHeader` (no `TopAppBar`), keeps its snackbar; the weight card is a Teal `MusFitSummaryCard`; other cards unchanged.
- `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug` passes; existing tests green.
- Profile verified in light and dark on device.
