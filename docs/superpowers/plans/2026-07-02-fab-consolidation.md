# FAB Consolidation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the redundant global "+" FAB from the bottom nav so each tab has at most one floating button (Today = coach chat, Food = quick-add, others = none).

**Architecture:** Pure UI deletion in a single file, `app/src/main/java/com/musfit/ui/AppNavGraph.kt`: the `FabSquare` composable, its call inside `FloatingPillNav`, the `onFab` parameter, and two now-unused icon imports go away; the nav pill (weight 1f) automatically expands into the freed row width. `ChatPreviewFab` (Today) and Food's in-screen FAB are untouched.

**Tech Stack:** Kotlin, Jetpack Compose M3. No new dependencies, no Room/ViewModel changes.

**Spec:** `docs/superpowers/specs/2026-07-02-fab-consolidation-design.md`

**TDD note:** This change deletes declarative UI with no branching logic; the repo has no Compose UI test infrastructure (unit tests cover ViewModels/repos/domain only), so per the spec there is no new unit test. The regression gate is: full existing suite + lint + assemble stays green, then on-device screenshots confirm the visual end state. Do not add a test scaffold for this.

---

### Task 1: Remove the FAB slot from `FloatingPillNav`

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/AppNavGraph.kt` (5 edits, all shown below)

- [ ] **Step 1: Delete the two icon imports that only serve `FabSquare`**

In the import block (currently lines 18–19), delete exactly these two lines. All other imports stay — `RoundedCornerShape`, `Alignment`, `Box`, `size`, `Icon`, and `Color` are still used by the nav pill / `NavPillItem`.

```kotlin
// DELETE these two lines:
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
```

- [ ] **Step 2: Remove the `onFab` argument at the `Scaffold` call site**

In `AppNavGraph()`, the `bottomBar` lambda currently reads:

```kotlin
        bottomBar = {
            FloatingPillNav(
                destinations = destinations,
                currentRoute = currentRoute,
                onSelect = { go(it.route) },
                onFab = { go(AppDestination.Food.route) },
            )
        },
```

Change it to:

```kotlin
        bottomBar = {
            FloatingPillNav(
                destinations = destinations,
                currentRoute = currentRoute,
                onSelect = { go(it.route) },
            )
        },
```

- [ ] **Step 3: Update `FloatingPillNav`'s doc comment and signature**

Current:

```kotlin
/**
 * M3E-style floating bottom nav: a rounded pill of destinations + a separate rounded-square FAB.
 * A single pill indicator sits behind the items and springs to the selected tab on navigation —
 * the one motion in the bar.
 */
@Composable
private fun FloatingPillNav(
    destinations: List<AppDestination>,
    currentRoute: String,
    onSelect: (AppDestination) -> Unit,
    onFab: () -> Unit,
) {
```

New (drop the FAB mention and the `onFab` parameter):

```kotlin
/**
 * M3E-style floating bottom nav: a rounded pill of destinations. A single pill indicator sits
 * behind the items and springs to the selected tab on navigation — the one motion in the bar.
 * Floating actions belong to tab content (Today's chat FAB, Food's quick-add), not this bar.
 */
@Composable
private fun FloatingPillNav(
    destinations: List<AppDestination>,
    currentRoute: String,
    onSelect: (AppDestination) -> Unit,
) {
```

- [ ] **Step 4: Remove the `FabSquare` call and the now-inert Row arrangement params**

The end of `FloatingPillNav`'s `Row` currently reads:

```kotlin
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
```

and (after the pill `Surface` block closes):

```kotlin
        }
        val fab = tabAccentFor(AppDestination.Today)
        FabSquare(color = fab.color, contentColor = fab.onColor, onClick = onFab)
    }
}
```

With one child left, the Row's alignment/arrangement do nothing. Replace the `Row` opener with:

```kotlin
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
```

and delete the two `fab` lines so the tail is just:

```kotlin
        }
    }
}
```

(The pill `Surface` keeps `Modifier.weight(1f)`, which requires `RowScope` — that's why the `Row` wrapper stays.)

- [ ] **Step 5: Delete the `FabSquare` composable entirely**

At the bottom of the file, delete this whole function:

```kotlin
@Composable
private fun FabSquare(color: Color, contentColor: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = color,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 4.dp,
        modifier = Modifier.size(56.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Outlined.Add,
                contentDescription = "Add food",
                tint = contentColor,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}
```

- [ ] **Step 6: Run the full verification build**

```powershell
. C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain
```

Expected: `BUILD SUCCESSFUL`. If it fails on unresolved references, a still-used import was deleted — re-check Step 1 (only the two `Icons` lines go). If Gradle fails with `AccessDeniedException` / `Cannot snapshot` under `app/build`, that's the OneDrive issue — recover with `.\gradlew.bat --stop`, delete `app\build`, rerun.

- [ ] **Step 7: Commit**

```powershell
git add app/src/main/java/com/musfit/ui/AppNavGraph.kt
git commit -m "refactor(nav): remove redundant global + FAB from bottom bar

One floating button per tab, owned by tab content: Today keeps the
coach chat FAB, Food keeps its quick-add FAB. The nav-bar + only
navigated to Food (adjacent pill does that) and carried a misleading
global 'Add food' description. Per spec
docs/superpowers/specs/2026-07-02-fab-consolidation-design.md.

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 2: On-device verification (Pixel 8 Pro, serial `38241FDJG00BLY`)

**Files:** none (verification only; screenshots go to the session scratchpad)

- [ ] **Step 1: Install and launch**

```powershell
. C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1
adb -s 38241FDJG00BLY install -r app\build\outputs\apk\debug\app-debug.apk
adb -s 38241FDJG00BLY shell am force-stop com.musfit
adb -s 38241FDJG00BLY shell monkey -p com.musfit -c android.intent.category.LAUNCHER 1
```

Expected: `Success` from the install; app opens on Today.

- [ ] **Step 2: Screenshot Today and check it**

```powershell
adb -s 38241FDJG00BLY shell screencap -p /sdcard/sc.png
adb -s 38241FDJG00BLY pull /sdcard/sc.png today_after.png
```

Read the PNG (do NOT use `>` redirection — it corrupts PNGs on PowerShell). Verify: nav pill spans the full width between margins with **no square "+" beside it**; the coach chat FAB (with "Soon" badge) is the only floating button.

- [ ] **Step 3: Screenshot Food**

The pill is wider now. Estimate: pill spans ~56px margins on the 1344px-wide screen, four equal items → Food (2nd item) centers near x≈520. Confirm against the Step 2 screenshot before tapping (multiply displayed coords by the scale factor noted under the image). Then:

```powershell
adb -s 38241FDJG00BLY shell input tap 520 2870
adb -s 38241FDJG00BLY shell screencap -p /sdcard/sc.png
adb -s 38241FDJG00BLY pull /sdcard/sc.png food_after.png
```

Verify: Food's own coral quick-add FAB is the only floating button; no second "+" in the nav bar.

- [ ] **Step 4: Screenshot Training**

Tap the Training pill item (3rd), screenshot the same way to `training_after.png`. Verify: **no** floating button anywhere.

- [ ] **Step 5: Clean up device temp file and report**

```powershell
adb -s 38241FDJG00BLY shell rm -f /sdcard/sc.png
```

Summarize the three screenshots for the user. No commit in this task.
