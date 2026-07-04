# Food planning: dedicated "This week" card

**Date:** 2026-07-04
**Status:** approved (mockup)
**Scope:** Food diary home (`FoodScreen.kt`) only. UI re-composition; no repository/Room/domain changes.
**Follows:** [`2026-07-04-food-main-tab-ui-restructure-design.md`](2026-07-04-food-main-tab-ui-restructure-design.md) (merged in PR #32).

## Problem

After the main-tab restructure, planning is the weak spot:

- **Plan is a *mode* wedged into a row of *actions*.** The quick-actions row is `Recipes · Plan week · Water`, but Recipes and Water are one-shot actions while Plan toggles a persistent mode — mixing two different kinds of control.
- **The Plan button is disconnected from the thing it controls.** The weekly-plan day strip lives inside the calorie hero, far from the Plan tile, so the control and the plan you're building don't read as related.
- **The "you're in planning mode" signal is weak** — only the tile's tint (the prior review flagged this).

## Design

Give planning its own home that owns both the toggle and the strip.

1. **Quick-actions row → two tiles: `Recipes · Water`.** The Plan tile is removed. `FoodQuickActionTile` loses its now-unused `selected` state.
2. **New always-visible `WeeklyPlanCard`**, rendered directly under the quick-actions row:
   - Header: a calendar icon + title (`This week`) on the left, a pill button on the right.
   - Body: the existing 7-day `WeeklyPlanStrip` (relocated out of the hero), unchanged — filled dot = day has planned items, today ringed.
   - **Default state:** neutral surface; outlined pill reading `Plan`.
   - **Planning-active state** (`isPlanningMode`): accent tint + accent border; title reads `Planning this week`; filled-accent pill reading `Done`.
   - The pill toggles the existing `togglePlanningMode()`.
3. **Hero loses the week strip** — `FoodDiarySummaryCard` keeps day-nav + ring + macro strip only.

The card is always visible (chosen over conditional/collapsible) so starting a plan is a one-tap, always-present entry point.

## What does NOT change

- Planning data and behavior: same `state.weeklyPlan`, `state.isPlanningMode`, `togglePlanningMode()`.
- The FAB, the meal diary, the sheets, the `⋮` menu, the collapsible groups.
- No repository/DAO/entity/Room/domain changes.

## Incidental cleanup

Removing the hero's week-strip gate makes `FoodUiState.toPlanningModePresentation()` and `FoodPlanningModePresentation` fully unused in production (they were the last consumer). This change removes both, plus `FoodPlanningModePresentationTest` — closing the "dead presentation fields" follow-up noted on PR #32. The card drives its state directly from `isPlanningMode`.

## Verification

- `testDebugUnitTest lintDebug assembleDebug` green (note: `FoodPlanningModePresentationTest` is deleted, not expected to run).
- On-device: quick-actions row shows only Recipes + Water; the `This week` card sits below it with the strip; tapping `Plan` highlights the card and flips the pill to `Done`; the hero no longer shows the strip.
