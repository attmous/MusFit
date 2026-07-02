# Floating-button consolidation — remove the nav-bar "+" slot

**Date:** 2026-07-02
**Status:** approved (Approach 1 of 3)
**Supersedes:** the "bottom nav / FAB stays as-is" non-goal in
[`2026-07-01-design-language-consistency-design.md`](2026-07-01-design-language-consistency-design.md)
(Non-goals list) — the pill nav itself still stays; only the adjacent FAB slot is removed.

## Problem

The app currently shows up to three coral floating squares, two of them stacked on Today:

| Button | Where | What it does |
|---|---|---|
| `FabSquare` "+" in `FloatingPillNav` | **every** tab (incl. Settings) | `go(Food.route)` — just navigates to Food |
| `ChatPreviewFab` (with "Soon" badge) | Today, floating bottom-right | opens the "coach chat is coming" sheet |
| Food's quick-add FAB | Food, in-screen bottom-end | expands the meal picker → add-food flow |

The nav-bar "+" is redundant everywhere: on Food it re-navigates to the tab you're on; on the
other tabs the Food pill sits directly beside it. Its global `contentDescription = "Add food"`
is also misleading on non-Food screens. On Today it visually stacks under the chat FAB.

## Decision

*Amended 2026-07-02 after user review of the built result: the original decision ("bar is
navigation only", chat FAB stays floating on Today) shipped as commit `e2f3f02` but did not match
the user's intent — the chat function should live in the bar slot the "+" occupied. The revised
decision below supersedes it; the `e2f3f02` removal of `FabSquare`/`onFab` stands.*

**The bar slot beside the nav pill hosts the coach chat button on every tab; add-food floating
actions belong to Food's own content.**

- `FloatingPillNav` regains its trailing button slot, now hosting `ChatPreviewFab` (the coral
  52dp rounded square with the "Soon" badge from
  [`2026-07-01-today-coach-feed-design.md`](2026-07-01-today-coach-feed-design.md)) on **all
  tabs**, so the pill width stays constant and the coach is reachable from anywhere.
- Tapping it opens the existing `ChatPreviewSheet` ("coach chat is coming"). Sheet visibility is
  plain UI state (`rememberSaveable`) in `AppNavGraph` — it is app-level UI, not Today state, so
  `TodayViewModel` loses `isChatPreviewVisible` / `openChatPreview` / `closeChatPreview` (and
  their test), and `TodayScreen` loses the floating `ChatPreviewFab`, the `ChatFabClearance`
  spacer, and its `ChatPreviewSheet` rendering.
- **Food:** its in-screen quick-add FAB is untouched.
- The old global "+" (navigate-to-Food) stays removed — its function was redundant.

### Rejected alternatives

2. *Chat button moves into the nav-bar slot (Today only)* — nav pill width would jump between
   tabs, fighting the sliding-indicator animation; a button inside the bar reads as global.
3. *Contextual per-tab action slot* (Training = start workout, Profile = log weight) — a new
   feature system out of scope; can be revisited if those tabs ever need a primary action.

## Changes

Phase 1 (shipped as `e2f3f02`): [`AppNavGraph.kt`](../../../app/src/main/java/com/musfit/ui/AppNavGraph.kt)

- Delete the `FabSquare` composable and its call (plus the `tabAccentFor(AppDestination.Today)`
  accent lookup feeding it) from `FloatingPillNav`.
- Drop the `onFab` parameter and the `onFab = { go(AppDestination.Food.route) }` call-site.
- Remove imports that become unused (e.g. `Icons.Outlined.Add`).

Phase 2 (the amendment):

- [`AppNavGraph.kt`](../../../app/src/main/java/com/musfit/ui/AppNavGraph.kt): `FloatingPillNav`
  gains an `onChat: () -> Unit` parameter and renders `ChatPreviewFab(onClick = onChat)` after
  the pill (restoring the Row's `CenterVertically` alignment and 12dp spacing);
  `AppNavGraph` holds `chatPreviewVisible` in `rememberSaveable` and renders `ChatPreviewSheet`
  when set.
- [`TodayScreen.kt`](../../../app/src/main/java/com/musfit/ui/today/TodayScreen.kt): remove the
  floating `ChatPreviewFab`, the `ChatFabClearance` constant + spacer, the `ChatPreviewSheet`
  rendering, and the `Box` overlay wrapper that existed only to host the FAB.
- [`TodayViewModel.kt`](../../../app/src/main/java/com/musfit/ui/today/TodayViewModel.kt): remove
  `isChatPreviewVisible`, `openChatPreview`, `closeChatPreview`; delete their test in
  `TodayViewModelTest`.
- `ChatPreviewFab` / `ChatPreviewSheet` composables themselves are unchanged and stay in
  `CoachFeedUi.kt`.

Phase 3 (second user-review amendment, 2026-07-02): the bar chat button matches the pill and
loses the badge.

- `ChatPreviewFab` (in `CoachFeedUi.kt`) drops the "Soon" badge and its wrapping overlay `Box`,
  becoming just the coral `Surface` + chat icon; sizing moves to the call site (the internal
  `size(52.dp)` goes). The not-yet-real cue survives in the `contentDescription`
  ("Coach chat (coming soon)") and in the sheet itself, so the button still doesn't fake a
  feature.
- `FloatingPillNav`'s Row gets `height(IntrinsicSize.Min)`; the chat button is called with
  `Modifier.fillMaxHeight().aspectRatio(1f)` so its top/bottom align exactly with the nav pill
  at any font scale, as a square.

No repository or Room changes. All Food code untouched.

## Verification

- Existing test suite (minus the deleted chat-preview ViewModel test) must stay green
  (`testDebugUnitTest lintDebug assembleDebug`).
- On-device (Pixel 8 Pro) screenshots: Today AND one other tab (e.g. Training) showing the
  chat square beside the pill with the "Soon" badge and no other floating button; Food showing
  chat square + its own quick-add FAB; tapping the chat square opens the "coming soon" sheet —
  light mode is sufficient since the change is structural, not color.
