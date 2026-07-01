# MusFit — Cross-Tab Design Language Consistency

**Date:** 2026-07-01
**Status:** Approved design, pending implementation plan
**Area:** Cross-cutting UI — all four tabs (`com.musfit.ui.today`, `com.musfit.ui.food`, `com.musfit.ui.training`, `com.musfit.ui.profile`) + shared `com.musfit.ui.components` + `com.musfit.ui.theme`

> This is explicitly cross-cutting work spanning all four miniapps, requested by
> the user. It overrides the usual "keep changes scoped to Food" guardrail.

## Background

The four tabs each render their top area — and much of their body — with a
different idiom, so the app reads as four apps stitched together rather than one:

- **Today** ([`TodayScreen.kt`](../../../app/src/main/java/com/musfit/ui/today/TodayScreen.kt)) — an inline
  `headlineMedium` "Today" title + muted date subtitle + a trailing `Tune` icon button, on the plain background.
- **Training** ([`TrainingScreen.kt`](../../../app/src/main/java/com/musfit/ui/training/TrainingScreen.kt)) — the
  same inline-title idiom (`TrainingHeader`): bold title + week-summary subtitle + a trailing `History` icon
  (accent-tinted).
- **Food** ([`FoodScreen.kt`](../../../app/src/main/java/com/musfit/ui/food/FoodScreen.kt)) — an edge-to-edge
  **gradient hero card** (`FoodSummaryHeader`) with bottom-rounded corners, `‹ date ›` navigation, a `MoreVert`
  overflow menu, and the calorie ring. **No "Food" title text at all.**
- **Profile** ([`ProfileScreen.kt`](../../../app/src/main/java/com/musfit/ui/profile/ProfileScreen.kt)) — the only
  tab using a Material 3 `Scaffold` + `TopAppBar` ("Profile" title + `Settings` action).

The theme layer already ships the right tokens — `MusFitSpacing` (4/8/12/16/20/24),
`MusFitShapes` (small 12 / medium 16 / large & extraLarge 28), the `MusFitTypography` M3E scale,
`MusFitColors` semantic tokens, and `TabAccent` (Today = Coral, Food = Emerald, Training = Indigo,
Profile = Teal). The problem is **inconsistent application**: Today/Training hardcode `16.dp`/`14.dp`/`12.dp`,
Food uses its own values and a bespoke gradient hero, and Profile leans on raw `MaterialTheme.shapes`.

## Goals

1. Make the four tabs share **one design language** — same header, same "headline" component, same section
   headers, switchers, buttons, chips, and empty states — differentiated only by each tab's `TabAccent`.
2. Do it by **applying the existing token layer consistently** and extracting a small set of shared
   composables, not by inventing a parallel token system.
3. Keep every tab's identity via its accent; keep the app dense, clean, and practical.
4. Preserve behavior exactly (presentational refactor). No Room/DB, navigation, or data-flow changes.

## Non-goals (out of scope)

- Any Room schema / migration / DB version change (this work touches zero persistence).
- ViewModel or repository logic changes; new features; new data on any tab (the Training "this week" summary
  card surfaces data the ViewModel already exposes).
- Reworking the bottom nav / FAB — `FloatingPillNav` in
  [`AppNavGraph.kt`](../../../app/src/main/java/com/musfit/ui/AppNavGraph.kt) is already unified and stays as-is.
- New color values or a new type scale — the existing `MusFitColors` / `TabAccent` / `MusFitTypography` are the
  source of truth.
- Redesigning the internal content of any single tab beyond adopting the shared language (e.g. the Food diary's
  meal list, Training's routine editor, Profile's measurement sheets keep their current structure).

## Chosen direction

Of three directions considered — **A** calm & dense (pull Food back to plain inline headers everywhere), **B**
branded hero everywhere (spread Food's edge-to-edge gradient to all tabs), and **C** shared bar + summary card —
the user chose **C**.

**C — Shared bar + summary card.** Every tab gets an identical slim header (title + optional subtitle +
trailing actions) on the plain background, and directly beneath it a single reusable, **contained** (inset, not
edge-to-edge) summary card that each tab fills with its headline stat in its own accent tint. Chrome is 100%
consistent; identity comes from the accent; Food's outlier gradient is tamed into the shared card system; it
rolls out tab-by-tab.

## Section 1 — Foundations (one rulebook for existing tokens)

No new tokens. One set of rules everyone follows:

| Concern | Rule |
|---|---|
| **Screen padding** | Every tab uses `spacing.lg` (16dp) horizontal edge padding. |
| **Vertical rhythm** | Between top-level sections: `spacing.lg` (16). Inside a card: `spacing.md` (12). Tight pairs: `spacing.sm` (8). No ad-hoc `14.dp`. |
| **Cards** | One shape scale: primary content cards = `shapes.large` (28dp); compact/nested surfaces = `shapes.medium` (16dp); chips/controls = `shapes.small` (12dp). Flat/low elevation on the cream background — no card invents its own radius. |
| **Accent usage** | Each tab uses its single `TabAccent` for: the summary-card tint, active/selected states, primary-action buttons, and key metric accents. Everything else uses neutral `MusFitColors`. One accent per screen. |
| **Color source** | Only `MusFitColors` + `TabAccent`; no hardcoded hex at call sites. Light and dark palettes both stay correct. |
| **Title type** | All four tab titles use `headlineMedium`. |

## Section 2 — Screen scaffold + header

One shared header contract for all four tabs: **tab title (left) + optional muted subtitle + 0–2 trailing icon
actions (right)** — identical `headlineMedium` title, height, and `spacing.lg` padding. Extracted into the one
new abstraction: `MusFitScreenScaffold` + `MusFitScreenHeader` in `com.musfit.ui.components`; all four screens
adopt it.

Per-tab fill:

| Tab | Subtitle | Trailing action(s) |
|---|---|---|
| Today | date | goals (`Tune`) |
| Food | — | overflow `MoreVert` (Goals, Meals, Templates, Recipes, Shopping, Fasting, Planning, Copy day) |
| Training | week summary | history |
| Profile | — | settings |

Two structural changes fall out of this:

- **Food's date navigation moves out of the gradient hero and becomes the top row of Food's summary card**
  (`‹ Tue · 1 Jul ›`, tappable date = jump to today). The header keeps only "Food" + the overflow menu.
- **Profile drops its `TopAppBar`** and uses the shared in-content header. It **keeps its `SnackbarHost`**
  (the shared scaffold exposes an optional snackbar slot, or Profile retains a thin `Scaffold` purely for the
  snackbar while rendering `MusFitScreenHeader` as its content header).

Two rules baked in: **header action icons are neutral** (`onSurfaceVariant`), not accent-tinted (Training's
history icon goes neutral) — the accent is spent on the summary card instead; and the **shared scaffold/header
is the single new abstraction** — everything else reuses existing patterns.

## Section 3 — The summary card

`MusFitSummaryCard` in `com.musfit.ui.components` — the reusable "headline moment" that replaces Food's gradient
hero and gives every tab the same anchor.

- **Contained, inset** card — `shapes.large` (28dp), inside the normal `spacing.lg` margins (not edge-to-edge).
- **Soft accent tint**: background = `TabAccent.container`, text = `TabAccent.onContainer`, ring/figure strokes
  = `TabAccent.color`. Restrained, not a saturated gradient.
- **A content slot** each tab fills.

| Tab | Summary card content | Notes |
|---|---|---|
| Today | daily rings (calories/steps/…) + macro bars | existing `DailyRingsCard` becomes an instance of `MusFitSummaryCard` (Coral) |
| Food | `‹ date ›` nav row + calorie ring + macro bars | this *is* the old hero, now a contained Emerald tinted card |
| Training | "this week": sessions, volume, 7-day mini-bar | **new lightweight card** (Indigo) surfacing data already in state; stats stop living only in the header subtitle |
| Profile | weight-vs-goal | Profile's existing top card gains the Teal tint and becomes the summary card; Identity/Measurements/Vitals stay neutral cards |

Confirmed intensity: **soft `TabAccent.container` tint**, not the bold `TabAccent.color`.

## Section 4 — Content rhythm (shared component language)

Five accent-aware reusable composables in `com.musfit.ui.components`, so tab bodies match, not just tops:

1. **`SectionHeader(title, trailingAction?)`** — `titleMedium` label + optional accent-colored text action
   ("See all", "Edit", "Add"). Every titled block on every tab uses it.
2. **Segmented control** — one styling with the accent as the active-segment tint (`container`/`onContainer`),
   surface + `onSurfaceVariant` inactive. Training adopts it; available to other tabs.
3. **Buttons — three tiers, accent-driven**: primary = accent-filled (`TabAccent.color`/`onColor`);
   secondary = accent-tonal (`container`/`onContainer`); text = accent. Generalizes the recent
   "unified emerald secondary-button" work in Food to per-tab accent.
4. **Chips** — selected = accent container + accent border; unselected = surface + outline.
5. **`EmptyState(icon, title, body, action?)`** — muted icon, one-line calm body in sentence case, optional
   accent-tonal button. Matches the neutral "not-yet" tone recently adopted.

## Section 5 — Per-tab application + phased rollout

Purely presentational: **no Room/DB changes, no migrations, no new dependencies, ViewModels untouched.** Shared
components are built as the first consuming tab needs them, proving the system on simpler tabs before Food.
Each phase is an independent branch/commit, verified before the next.

**Phase 1 — Foundation + Today** (lowest risk; Today is closest to target)
- Create the shared composables: `MusFitScreenScaffold`, `MusFitScreenHeader`, `MusFitSummaryCard`,
  `SectionHeader`, segmented control, button tiers, chip, `EmptyState`.
- Convert Today: header via the scaffold; `DailyRingsCard` → `MusFitSummaryCard` (Coral); apply the Section 1
  spacing/card rules.

**Phase 2 — Training**
- Adopt scaffold + header; standardize the segmented control; add the Indigo "this week" summary card; move
  buttons/empty states to the shared language.

**Phase 3 — Profile**
- Drop the `TopAppBar` for the shared header (keep the snackbar host); top weight card → `MusFitSummaryCard`
  (Teal); remaining cards to the neutral card rule; standardize buttons/empty states.

**Phase 4 — Food** (biggest; done last on a proven system)
- Replace the edge-to-edge gradient hero with `MusFitSummaryCard` (Emerald) holding the `‹ date ›` nav +
  calorie ring + macro bars; overflow menu moves to the header action; standardize section headers, chips,
  buttons, and empty states throughout. Existing `FoodViewModelTest` / repository tests stay green (behavior
  unchanged).

Rollout order: **Today → Training → Profile → Food** (risk ascending; Food last so the shared components are
battle-tested before the ~2,000-line screen).

## Section 6 — Testing & verification

Presentational work, so the safety net is regression, not new tests:

- **Existing unit tests stay green.** ViewModels/repositories are untouched, so `FoodViewModelTest`,
  `LocalFoodRepositoryTest`, and the rest are the guard that behavior is preserved. Full suite runs each phase.
- **Behavior-surface checks where controls move.** Food's date nav (hero → summary card) and Profile's actions
  (TopAppBar → shared header) relocate interactive controls; confirm the same ViewModel callbacks stay wired.
  Add a test only if a behavior seam actually changes (mostly it won't — same lambdas, new location).
- **Per-phase full verification**: `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon
  --console=plain` must pass before a phase is done.
- **Manual visual check on device**, light and dark: build, install, screenshot each converted tab, confirm the
  header/summary-card/spacing match the spec and dark-mode parity holds. Icon `contentDescription`s preserved
  throughout.

## Risks / constraints

- **Food is the largest, riskiest surface** (~2,000-line `FoodScreen.kt`); done last, on proven components,
  behavior-preserving. Follow the existing `FoodAddMode` / `FoodSheetMode` conventions when touching it.
- **Dark mode**: both `lightMusFitColors` and `darkMusFitColors` (and light/dark `TabAccent`) must stay correct
  — verify every phase in both modes.
- **Snackbar on Profile** must survive the `TopAppBar` removal.
- **OneDrive/Gradle** flakiness on `app/build` is environmental; recover per `AGENTS.md` and rerun.

## Summary

A shared **scaffold + header**, a reusable **summary card**, and a common **component language** (section
headers, segmented controls, buttons, chips, empty states) — all driven by the existing **tokens** and **per-tab
accents** — rolled out **Today → Training → Profile → Food**, presentationally and behavior-preserving.
