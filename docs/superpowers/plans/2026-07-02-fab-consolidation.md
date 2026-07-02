# FAB Consolidation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the redundant global "+" FAB from the bottom nav so each tab has at most one floating button (Today = coach chat, Food = quick-add, others = none).

> **Amended 2026-07-02 (Tasks 3–4):** after user review of the built result, the chat button moves *into* the bar slot the "+" occupied, on every tab; Today's floating chat FAB and its ViewModel state are removed. Tasks 1–2 are complete and stand as shipped (`e2f3f02`).

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

---

### Task 3: Move the coach chat button into the nav-bar slot (all tabs)

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/AppNavGraph.kt`
- Modify: `app/src/main/java/com/musfit/ui/today/TodayScreen.kt`
- Modify: `app/src/main/java/com/musfit/ui/today/TodayViewModel.kt`
- Modify: `app/src/test/java/com/musfit/ui/today/TodayViewModelTest.kt`

**TDD note:** this task removes behavior (the ViewModel chat-preview toggle) and moves declarative UI. The deleted test is the spec of the removed behavior and goes in the same commit; the gate is the full remaining suite + lint + assemble, then on-device checks (Task 4). Do not add new tests — sheet visibility becomes plain Compose UI state, which this repo does not unit-test.

- [ ] **Step 1: Delete the `chatPreview_togglesVisibility` test**

In `app/src/test/java/com/musfit/ui/today/TodayViewModelTest.kt`, delete this whole method (currently lines ~216–225):

```kotlin
    @Test
    fun chatPreview_togglesVisibility() = runTest {
        val viewModel = todayViewModel(coachRepository = FakeCoachRepository())
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openChatPreview()
        assertTrue(viewModel.state.value.isChatPreviewVisible)
        viewModel.closeChatPreview()
        assertEquals(false, viewModel.state.value.isChatPreviewVisible)
    }
```

If `assertTrue` (or any other import) has no remaining usage in the test file afterwards, remove that import too — check with a search before deleting.

- [ ] **Step 2: Remove the chat-preview state from `TodayViewModel`**

In `app/src/main/java/com/musfit/ui/today/TodayViewModel.kt`:

Delete this line from the `TodayUiState` data class (keep `feed` and its trailing comma):

```kotlin
    val isChatPreviewVisible: Boolean = false,
```

Delete these two functions (and the blank line between them):

```kotlin
    fun openChatPreview() = mutableState.update { it.copy(isChatPreviewVisible = true) }

    fun closeChatPreview() = mutableState.update { it.copy(isChatPreviewVisible = false) }
```

- [ ] **Step 3: Remove the floating chat FAB from `TodayScreen`**

In `app/src/main/java/com/musfit/ui/today/TodayScreen.kt`:

Delete the constant near the top of the file:

```kotlin
private val ChatFabClearance = 76.dp
```

Unwrap the `Box` overlay: the body currently reads

```kotlin
    Box(modifier = Modifier.fillMaxSize()) {
        MusFitScreenScaffold(
            title = "Today",
            ...
        ) {
            ...
            Spacer(Modifier.height(ChatFabClearance))
        }

        ChatPreviewFab(
            onClick = viewModel::openChatPreview,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(MusFitTheme.spacing.lg),
        )
    }

    if (state.isChatPreviewVisible) {
        ChatPreviewSheet(onDismiss = viewModel::closeChatPreview)
    }
```

After the edit, `MusFitScreenScaffold(...) { ... }` stands at top level (re-indent its contents one level out), with:
- the `Spacer(Modifier.height(ChatFabClearance))` line deleted,
- the `ChatPreviewFab(...)` block deleted,
- the `if (state.isChatPreviewVisible) { ChatPreviewSheet(...) }` block deleted,
- the wrapping `Box(modifier = Modifier.fillMaxSize()) { ... }` removed.

Then remove any imports the file no longer uses (candidates: `Box`, `fillMaxSize`, `Spacer`, `height`, `Alignment` — verify each with a search; `ChatPreviewFab`/`ChatPreviewSheet` are same-package, no imports involved). Keep everything else (`MusFitTheme` is still used elsewhere in the file).

- [ ] **Step 4: Add the chat button + sheet to `AppNavGraph`**

In `app/src/main/java/com/musfit/ui/AppNavGraph.kt`:

(a) Add two imports in the `com.musfit.ui.today` group (before `TodayScreen`):

```kotlin
import com.musfit.ui.today.ChatPreviewFab
import com.musfit.ui.today.ChatPreviewSheet
```

(b) In `AppNavGraph()`, after the `scannedLabelText` declaration, add:

```kotlin
    var chatPreviewVisible by rememberSaveable { mutableStateOf(false) }
```

(c) At the `Scaffold` call site, wire the new parameter:

```kotlin
        bottomBar = {
            FloatingPillNav(
                destinations = destinations,
                currentRoute = currentRoute,
                onSelect = { go(it.route) },
                onChat = { chatPreviewVisible = true },
            )
        },
```

(d) Render the sheet at function level, between the `Scaffold` block's closing brace and the closing brace of `AppNavGraph()`:

```kotlin
    if (chatPreviewVisible) {
        ChatPreviewSheet(onDismiss = { chatPreviewVisible = false })
    }
```

(e) Update `FloatingPillNav`'s doc comment and signature:

```kotlin
/**
 * M3E-style floating bottom nav: a rounded pill of destinations + the coach-chat button.
 * A single pill indicator sits behind the items and springs to the selected tab on navigation —
 * the one motion in the bar. Add-food floating actions belong to Food's own content.
 */
@Composable
private fun FloatingPillNav(
    destinations: List<AppDestination>,
    currentRoute: String,
    onSelect: (AppDestination) -> Unit,
    onChat: () -> Unit,
) {
```

(f) Restore the Row's alignment/arrangement (two children again):

```kotlin
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
```

(g) After the pill `Surface` block closes (the `}` at one indent inside the `Row`), add the chat button so the tail of `FloatingPillNav` reads:

```kotlin
        }
        ChatPreviewFab(onClick = onChat)
    }
}
```

- [ ] **Step 5: Run the full verification build**

```powershell
. C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain
```

Expected: `BUILD SUCCESSFUL`. OneDrive recovery if needed: `.\gradlew.bat --stop`, wait 3 s, `Remove-Item -Recurse -Force app\build`, rerun.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/musfit/ui/AppNavGraph.kt app/src/main/java/com/musfit/ui/today/TodayScreen.kt app/src/main/java/com/musfit/ui/today/TodayViewModel.kt app/src/test/java/com/musfit/ui/today/TodayViewModelTest.kt
git commit -m "feat(nav): coach chat button takes the bar slot on every tab

The chat preview moves from a Today-only floating FAB into the
FloatingPillNav slot the old + occupied, so the coach is reachable
from anywhere and Today loses its stacked corner button. Sheet
visibility becomes plain UI state in AppNavGraph; TodayViewModel
drops isChatPreviewVisible/openChatPreview/closeChatPreview. Per
amended spec docs/superpowers/specs/2026-07-02-fab-consolidation-design.md.

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 4: On-device verification of the amended layout (Pixel 8 Pro)

**Files:** none (verification only)

- [ ] **Step 1: Install and launch** — same commands as Task 2 Step 1.

- [ ] **Step 2: Today** — screenshot to `today_after2.png`. Verify: the coral chat square (with "Soon" badge) sits in the bottom bar row to the RIGHT of the nav pill; NO floating button in the content area above it.

- [ ] **Step 3: Training** — tap the Training pill item (derive x from the screenshot; the pill is narrower again with the button back), screenshot to `training_after2.png`. Verify: the same chat square is present beside the pill; no other floating button.

- [ ] **Step 4: Food** — tap the Food pill item, screenshot to `food_after2.png`. Verify: chat square beside the pill AND Food's own quick-add FAB floating above in content — exactly two coral elements, not three.

- [ ] **Step 5: Chat sheet opens** — on any tab, tap the chat square (derive coords from the current screenshot), wait 2 s, screenshot to `chat_sheet_open.png`. Verify the "coach chat is coming" sheet is shown. Dismiss it by tapping the dimmed scrim near the top (`input tap 672 300`), screenshot again to confirm it closed.

- [ ] **Step 6: Clean up** — `adb -s 38241FDJG00BLY shell rm -f /sdcard/sc.png`; report per-screenshot findings. No commit.

---

### Task 5: Match the chat button to the pill height and drop the "Soon" badge

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/today/CoachFeedUi.kt`
- Modify: `app/src/main/java/com/musfit/ui/AppNavGraph.kt`

**TDD note:** declarative UI sizing/appearance only — no logic, no new tests; gate is the full suite + lint + assemble, then Task 6 on-device.

- [ ] **Step 1: Simplify `ChatPreviewFab` — remove the badge and internal sizing**

In `app/src/main/java/com/musfit/ui/today/CoachFeedUi.kt`, replace the whole composable:

```kotlin
/** Preview chat FAB with a visible "Soon" badge — opens the "coming soon" sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPreviewFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = tabAccentFor(AppDestination.Today)
    Box(modifier = modifier) {
        Surface(
            onClick = onClick,
            color = accent.color,
            shape = MusFitTheme.shapes.medium,
            shadowElevation = 4.dp,
            modifier = Modifier.size(52.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Coach chat (coming soon)",
                    tint = accent.onColor,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Surface(
            color = MusFitTheme.colors.onSurface,
            shape = MusFitTheme.shapes.small,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 6.dp, y = (-6).dp)
                // The FAB's contentDescription already says "coming soon" — don't
                // surface a duplicate "Soon" node to TalkBack.
                .clearAndSetSemantics {},
        ) {
            Text(
                text = "Soon",
                style = MusFitTheme.typography.labelSmall,
                color = MusFitTheme.colors.surface,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
    }
}
```

with:

```kotlin
/** Coach chat button in the bottom bar — opens the "coming soon" preview sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPreviewFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = tabAccentFor(AppDestination.Today)
    Surface(
        onClick = onClick,
        color = accent.color,
        shape = MusFitTheme.shapes.medium,
        shadowElevation = 4.dp,
        modifier = modifier,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Outlined.ChatBubbleOutline,
                contentDescription = "Coach chat (coming soon)",
                tint = accent.onColor,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
```

Then check `CoachFeedUi.kt` for imports that lost their last usage (candidates: `offset`, `clearAndSetSemantics` — search the file before removing each; `Box`, `size`, `Text`, `padding`, `Alignment` are almost certainly still used by the feed cards, verify rather than assume).

- [ ] **Step 2: Size the button from the bar Row in `AppNavGraph`**

In `app/src/main/java/com/musfit/ui/AppNavGraph.kt`, add these imports to the `androidx.compose.foundation.layout` group (alphabetical):

```kotlin
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
```

Change `FloatingPillNav`'s Row opener from:

```kotlin
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
```

to:

```kotlin
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
```

and the trailing call from:

```kotlin
        ChatPreviewFab(onClick = onChat)
```

to:

```kotlin
        ChatPreviewFab(
            onClick = onChat,
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f),
        )
```

(`height` is already imported. The pill `Surface` keeps `weight(1f)`; with `IntrinsicSize.Min` on the Row, the pill's content height drives the Row, and the chat button stretches to exactly that height as a square.)

- [ ] **Step 3: Run the full verification build**

```powershell
. C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain
```

Expected: `BUILD SUCCESSFUL`. OneDrive recovery if needed: `.\gradlew.bat --stop`, wait 3 s, `Remove-Item -Recurse -Force app\build`, rerun.

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/java/com/musfit/ui/today/CoachFeedUi.kt app/src/main/java/com/musfit/ui/AppNavGraph.kt
git commit -m "feat(nav): chat button fills the bar height, drops the Soon badge

The bar chat square now top/bottom-aligns with the nav pill via
IntrinsicSize.Min on the bar Row + fillMaxHeight/aspectRatio at the
call site; ChatPreviewFab loses its badge overlay and internal 52dp
size. The coming-soon cue stays in the contentDescription and sheet.

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 6: On-device verification of the final bar layout (Pixel 8 Pro)

**Files:** none (verification only)

- [ ] **Step 1: Install and launch** — same commands as Task 2 Step 1.
- [ ] **Step 2: Today** — screenshot to `today_after3.png`. Verify: the chat square's top and bottom edges align with the nav pill's top and bottom edges (same height, sitting on the same baseline); NO "Soon" badge anywhere; no floating button in content.
- [ ] **Step 3: Food** — tap the Food pill item, screenshot to `food_after3.png`. Verify: aligned chat square + Food's own quick-add FAB in content.
- [ ] **Step 4: Sheet still works** — tap the chat square, wait 2 s, screenshot to `chat_sheet_open3.png` (sheet visible), dismiss via scrim tap near the top (`input tap 672 300`), confirm closed with one more screencap (no need to save it if closed).
- [ ] **Step 5: Clean up** — `adb -s 38241FDJG00BLY shell rm -f /sdcard/sc.png`; report per-screenshot findings with the alignment call made explicitly (aligned / not aligned, by roughly how many px if off). No commit.
