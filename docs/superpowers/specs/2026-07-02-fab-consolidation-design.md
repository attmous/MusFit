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

**One floating button per tab, owned by the tab's content; the bottom bar is navigation only.**

- Remove the `FabSquare` slot from `FloatingPillNav` (and the `onFab` lambda in `AppNavGraph`);
  the nav pill takes the freed row width.
- **Today:** the coach chat FAB is the tab's single floating button — unchanged in position,
  styling, badge, and behavior (per
  [`2026-07-01-today-coach-feed-design.md`](2026-07-01-today-coach-feed-design.md)).
- **Food:** its in-screen quick-add FAB is untouched.
- **Training / Profile / Settings:** no floating button.

### Rejected alternatives

2. *Chat button moves into the nav-bar slot (Today only)* — nav pill width would jump between
   tabs, fighting the sliding-indicator animation; a button inside the bar reads as global.
3. *Contextual per-tab action slot* (Training = start workout, Profile = log weight) — a new
   feature system out of scope; can be revisited if those tabs ever need a primary action.

## Changes

Single file: [`AppNavGraph.kt`](../../../app/src/main/java/com/musfit/ui/AppNavGraph.kt)

- Delete the `FabSquare` composable and its call (plus the `tabAccentFor(AppDestination.Today)`
  accent lookup feeding it) from `FloatingPillNav`.
- Drop the `onFab` parameter and the `onFab = { go(AppDestination.Food.route) }` call-site.
- Remove imports that become unused (e.g. `Icons.Outlined.Add`).
- The pill `Surface` keeps `weight(1f)` in its `Row` and now spans the full width between the
  16dp margins; the sliding-indicator math is width-derived and needs no change.

No ViewModel, repository, Room, or state changes. `ChatPreviewFab`, `ChatFabClearance`, and all
Food code are untouched.

## Verification

- No unit-testable logic changes; existing test suite must stay green
  (`testDebugUnitTest lintDebug assembleDebug`).
- On-device (Pixel 8 Pro) screenshots: Today (chat FAB only, nav pill full width), Food
  (quick-add FAB only), Training (no floating button) — light mode is sufficient since the
  change is structural, not color.
