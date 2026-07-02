# MusFit — Profile Tab Rethink: Body & Progress Hub

**Date:** 2026-07-02
**Status:** Approved design, pending implementation plan
**Area:** `com.musfit.ui.profile` + a goal-ownership change touching `ui/today` + `ui/AppNavGraph.kt` (plan-card navigation callbacks) + a data-only Room migration

> Part 2 of the two-part tab rethink (Today shipped 2026-07-02 as
> `docs/superpowers/specs/2026-07-01-today-coach-feed-design.md`). Explicitly
> requested Profile work, so the "keep changes scoped to Food" guardrail does
> not apply.

## Background

With Today now coach-led ("how am I doing right now") and Food/Training owning
the inputs, Profile earns its keep as the **output surface: is my body actually
changing, and toward what?** The user asked for a from-scratch re-derivation
rather than remodeling the old card inventory; the result cuts several
carried-over sections and keeps four that survive on merit.

## Approved product choices

| Decision | Choice |
|---|---|
| Tab identity | **Body & progress hub** — leads with body data over time; goals and plans below as context |
| Target-weight ownership | **Canonical in `user_profile.goalWeightKg`**; Today's dashboard sheet drops its target-weight field; `user_goals.targetWeightKg` retired from all read paths (column stays; one-time recency-aware carry-over) |
| Body layout | **Trend-hero + sparkline grid** — 30-day weight chart inside the Teal summary card; every measurement tile carries a mini-sparkline; tap → full history chart sheet |
| Plans | **Launcher cards, display-only** — diet mode / training program; tap → owning tab; no editors on Profile. (No fasting card — see Non-goals) |
| Identity strip | **Cut** — sex/birthdate/height/activity are setup inputs, edited via `ProfileEditDialog` (reached from the Goal section's "Edit" and a new Settings row) |
| Health-data (vitals) section | **Cut from the tab** — Health Connect stays manageable in Settings; a one-line connect nudge appears on the hub only while HC is disconnected |
| BMI | **Caption inside the weight hero** (computed; no tile, no logging) |
| Account card | Moves into the Profile **Settings** sub-screen |

## Windowing rule (applies to Sections 1–2)

**Latest values, deltas, goal progress, and tap-routing always derive from
all-time data** (the current `ProfileViewModel` already reads
`observeWeightSeries(0L)` / `observeRecentMeasurements(0L)`); **only the drawn
chart series are windowed** (hero 30 days, sparklines 90 days). A stale logger
therefore keeps their latest figure, delta, progress badge, and history-sheet
access; only the chart area degrades ("No entries in the last 30 days"
caption) — never the first-log prompt. This deliberately mirrors and
complements the Today plan's recorded deviation #7 (the carousel's windowed
NoData is acceptable *because* Profile is the fix-it path — so Profile itself
must not window its truth).

## Screen structure (top → bottom)

1. **Shared header** (`MusFitScreenHeader`, Teal accent): "Profile" + settings
   gear (unchanged).
2. **HC connect nudge** *(conditional)* — one-line surface row, shown only
   while Health Connect is disconnected/unavailable: "Connect Health Connect
   to mirror steps and heart rate" → opens Settings. Disappears once
   connected. (Replaces the discovery role of the removed Vitals card.)
3. **Weight hero** — the Teal `MusFitSummaryCard` (upgrade of the existing
   Phase-3 card).
4. **Measurements** — `SectionHeader("Measurements", "+ Log")` + sparkline
   tile grid.
5. **Goal** — `SectionHeader("Goal", "Edit")` + the goal & targets card.
6. **Plans** — `SectionHeader("Plans")` + launcher cards.

Removed from the tab: Account card (→ Settings), Identity card (its inputs
live in `ProfileEditDialog`; both entry points below are **new**), Vitals card
(cut; see Section 5 deviations).

## Section 1 — Weight hero (Teal summary card)

- **Headline row:** latest weight (all-time newest entry) + **7-day delta via
  `WeeklyGoalsCalculator.weightTrend`** — a deliberate semantics change from
  the current `BodyMetricsCalculator.changeOverWindow` so Profile and the
  Today carousel show the *same* number (both are avg-of-last-7d minus
  avg-of-prior-7d; Unicode minus formatting). When the delta is null (e.g. a
  single entry, or no prior-week data) the headline shows the weight alone
  with the caption "latest".
- **Goal badge** (`goal 75 kg · 62%`): reads `user_profile.goalWeightKg`;
  hidden when null or ≤ 0. Progress = `BodyMetricsCalculator
  .goalProgressFraction` with the **all-time first weight entry as baseline**
  (current behavior, restated so the 30-day chart window cannot leak into it);
  when progress is null (no entries, or start == goal) the badge shows
  "goal 75 kg" without a percent; at ≥ 100% it caps at "100%".
- **Chart:** 30-day `TrendLineChart` (Teal stroke) inside the card — the
  chart kit's first Profile consumer. Zero entries ever → the existing
  "No weight logged yet." text in place of the chart; entries exist but none
  in the window → "No entries in the last 30 days."
- **BMI caption:** small computed line (`BMI 25.2`,
  `BodyMetricsCalculator.bmi`) under the headline when height + any weight
  exist; nothing otherwise.
- **Actions:** "+ Log weight" accent-tonal button — the existing
  `LogWeightDialog`, **prefilled from the latest logged weight, as today**
  (note: there is no Health-Connect prefill today and none is added; users
  who track weight only via HC remain an accepted gap — see Non-goals).
  Tapping the card/chart opens the weight **history sheet** (Section 2's
  upgraded `EntriesSheet`, fed the full weight series — same chart-header
  treatment as measurements; "kept as-is" applies to its edit/delete
  behavior, not its appearance).

## Section 2 — Measurements (sparkline tiles)

One tile per existing type (waist, chest, arms, thighs, hips, body fat),
2-column grid. **Three tile states, keyed on all-time entry count:**

| Entries | Tile shows | Tap opens |
|---|---|---|
| 0 | muted "— · Tap to log" | `LogMeasurementDialog` **pre-selected to that type** (the dialog gains an optional `initialType` parameter — a small signature change) |
| 1 | value + "—" delta, no sparkline | history sheet |
| ≥ 2 | value + delta from previous + 90-day mini-sparkline (small `TrendLineChart`) | history sheet |

- **History sheet:** the shared `EntriesSheet` (`ProfileEditContent.kt`)
  gains an **optional chart-header slot** — full-size `TrendLineChart` above
  the existing entries list (log/edit/delete unchanged). The header obeys the
  same < 2-entries gating as tiles, so deleting down to 1/0 entries mid-sheet
  collapses the chart cleanly instead of leaving a lone dot. Both weight and
  measurement sheets get the header (one shared composable, one behavior).
- **"+ Log"** section action keeps the existing `LogMeasurementDialog` flow.
- **Data source:** the existing
  `ProfileRepository.observeRecentMeasurements(sinceEpochMillis)` already
  returns per-type dated series — **no new repository API**. The ViewModel
  keeps reading all-time (`0L`) for values/deltas/routing and windows the
  sparkline series in the mapper.
- No BMI tile (Section 1). No custom measurement types in v1
  (`body_metrics.type` already supports them later).

## Section 3 — Goal card + target-weight ownership change

The card keeps its existing mechanics: goal type (lose/maintain/gain) · pace ·
**target weight** · goal progress, plus recommended daily calories/macros
(`EnergyCalculator`) and the **"Apply to Food goals"** action (unchanged
semantics; proactively offering the update on pace/goal change is a future
idea, out of scope). The **`SectionHeader("Goal", "Edit")` trailing action
opens `ProfileEditDialog`** (one phrasing, used everywhere); the in-card
"Complete your profile" fallback button (shown while targets are
uncomputable) **stays**.

**Ownership change (the part that touches Today):**

- `user_profile.goalWeightKg` becomes the **single canonical target weight**.
- **Today's `DashboardEditSheet` drops its "Target weight (kg)" field**;
  `TodayViewModel.openDashboardEditor`/`saveDashboard` stop reading/writing it
  (steps + weekly sessions remain).
- The **coach** (`CoachInput.targetWeightKg`) switches from
  `userGoals.targetWeightKg` to the profile's goal weight (a profile flow
  already feeds the carousel combine; `coachInputFlow` gains the same source).
- `user_goals.targetWeightKg` is retired from every read path. The column
  stays (no destructive migration).
- **One-time carry-over, data-only `MIGRATION_26_27`.** Two facts force its
  shape: (1) a `user_profile` row exists **only** if the user ever saved the
  profile editor — a user who only set the target in Today has a `user_goals`
  row and *no* profile row; (2) at runtime the retired reads **preferred the
  `user_goals` value when > 0**, so that value is what the user was actually
  living with. Therefore, for each `user_goals` row with
  `targetWeightKg > 0` (matched on `id` — both tables are account-keyed):
  - **No matching `user_profile` row → INSERT one** carrying the target,
    with the in-memory-default values for the NOT-NULL columns (`sex` NULL,
    `birthDateEpochDay` NULL, `heightCm` NULL, `activityLevel` = the
    `DEFAULT_USER_PROFILE` value, `goalType` likewise, `goalPaceKgPerWeek`
    likewise, `updatedAtEpochMillis` = the goals row's) — copy the exact
    default literals from `ProfileRepository.DEFAULT_USER_PROFILE` at
    implementation time.
  - **Matching row exists → UPDATE `goalWeightKg`** when
    `goalWeightKg IS NULL OR goalWeightKg <= 0` **OR** the goals row's
    `updatedAtEpochMillis` is newer than the profile row's (recency-aware:
    a target corrected in Today's sheet after an old profile edit must not
    silently revert).
  - Schema unchanged; version bumps to 27, `27.json` committed per repo
    rules. Migration round-trip tests cover: copy-into-NULL, **insert-when-
    profile-row-missing**, no-overwrite when the profile value is newer,
    overwrite when the goals value is newer.

## Section 4 — Plans (launcher cards)

Display-only cards; Profile never edits a plan. `planCards` is a small
combine over **`FoodRepository` + `TrainingRepository` + `GoalsRepository`
flows — the latter two are newly injected into `ProfileViewModel`**. Tap
navigation requires **`ProfileScreen` gaining `onOpenFood`/`onOpenTraining`
callbacks wired through `AppNavGraph.kt`** (mirroring `TodayScreen`).

| Card | Content | Rules | Tap → |
|---|---|---|---|
| Diet | mode label + one key figure | Label map for all 8 `FoodGoalMode`s: Balanced → "Balanced diet", HighProtein → "High-protein diet", KetoLowCarb → "Keto / low-carb diet", MuscleGain → "Muscle-gain diet", WeightLoss → "Weight-loss diet", MediterraneanStyle → "Mediterranean diet", CleanEating → "Clean-eating diet", Custom → "Custom diet" (reuse Food's existing mode-label mapping if one exists — verify at plan time — so the two surfaces agree). Figure: protein target for HighProtein/MuscleGain, calorie target for all others incl. Custom. Always renders (a goal mode always exists). | Food |
| Training | program name if the first routine carries one, else the first routine's name ("next" = `observeRoutineSummaries().firstOrNull()`, the coach's existing convention); subtitle "n of m sessions this week" via `countSessionsInWeek` + `user_goals.weeklySessionTarget` — **when the target is ≤ 0 the subtitle drops the denominator** ("n sessions this week"), mirroring the carousel's Sessions rule. No routines → "No program yet — set one up in Training". | | Training |

## Section 5 — Settings & removals

- **Settings gains an Account section** (name/email + the derived initial
  avatar — no avatar picker), placed above the Health Connect block. This is
  a **move, not a drop-in**: `AccountEditDialog` (currently private in
  `ProfileScreen.kt`) is extracted, and its editor state/actions
  (`openAccountEditor`/`saveAccount`/input handlers) migrate from
  `ProfileViewModel` to `ProfileSettingsViewModel`, which gains
  `AccountRepository`; the corresponding `ProfileViewModelTest` account tests
  move to `ProfileSettingsViewModelTest`.
- **Settings gains a "Profile details" row** opening `ProfileEditDialog`
  (new entry point; the other is the Goal section's "Edit").
- **The Vitals card is removed.** Two consequences, recorded honestly:
  1. **Deep-link deviation (goes beyond the Today spec's stance):** Today's
     steps / active-kcal / resting-HR metrics still deep-link to Profile,
     which now has **no matching content** — not merely "current values
     only" as the Today spec's interim assumed. Accepted for v1 because the
     conditional HC connect nudge (screen structure #2) covers the
     disconnected case, HC management is one tap away in Settings, and a
     dedicated health-data view remains open as future work.
  2. The Vitals card was the tab's only HC **connect prompt** — replaced by
     the conditional nudge row (screen structure #2), which is part of this
     spec, not optional polish.
- `WeekBarChart` remains unconsumed by Profile (kept in the chart kit for a
  future Food weekly view); only `TrendLineChart` gains consumers here.

## Architecture

Existing layering preserved; no new repositories, no new repository APIs.

- **`ProfileViewModel`** reworked state: `heroState` (latest weight, 7-day
  delta via `weightTrend`, goal weight + progress, BMI, 30-day chart series),
  `measurementTiles` (per type: all-time latest/delta/count + 90-day sparkline
  series), `goalState` (existing fields), `planCards` (Section 4),
  `isHealthConnectNudgeVisible`, dialog/sheet state (existing, minus the
  account editor which moves out). New constructor deps: `TrainingRepository`,
  `GoalsRepository`. Account-editor state/actions move to
  `ProfileSettingsViewModel` (which gains `AccountRepository`).
- **`TodayViewModel`**: drop the target-weight field wiring; coach input's
  target weight sourced from the profile flow.
- **Domain:** reuse `BodyMetricsCalculator` (BMI, progress),
  `WeeklyGoalsCalculator.weightTrend` (hero delta), `TrendChartScaler`
  (charts), `countSessionsInWeek` (plans subtitle). New pure mappers only for
  tile/plan-card assembly. `LogMeasurementDialog` gains an optional
  `initialType` parameter; `EntriesSheet` gains an optional chart-header slot.
- **Migration:** `MIGRATION_26_27` (data-only, Section 3), registered per repo
  rules with `27.json` (and the mechanical chain-append in the existing
  migration-test suite, as every version bump requires).

## Testing (TDD per repo norms)

- **ViewModel (`ProfileViewModelTest`, fakes — gains fakes for Training +
  Goals):** hero mapping (weightTrend delta + null-delta "latest" state, goal
  badge hidden/percentless/capped states, BMI gating, both chart empty
  states); tile mapping (three states incl. never-logged preselect tap and
  the stale-logger keeping history routing); plan cards (all 8 diet labels +
  figure rule, training with/without routines, sessions n/m incl. target 0);
  HC nudge visibility; existing log/edit/delete behaviors stay green;
  account-editor tests move to `ProfileSettingsViewModelTest` (open / edit /
  validate / save).
- **`TodayViewModelTest`:** dashboard editor no longer exposes target weight
  (prefill/save tests updated); coach input carries the profile's goal weight
  (pin via the fake profile's `goalWeightKg`).
- **Repository/migration (Robolectric):** 26→27 round-trip — the four cases
  from Section 3 (copy-into-NULL, insert-when-missing, no-overwrite-when-
  profile-newer, overwrite-when-goals-newer); schema unchanged.
- **Full verification per slice** (`testDebugUnitTest lintDebug assembleDebug`)
  + final emulator light/dark pass with screenshots (hub, a tile-tap history
  sheet with chart, the upgraded Settings, the HC nudge in a disconnected
  state if reproducible).

## Implementation slices

1. **Goal ownership** — migration 26→27 + the four carry-over tests; Today's
   sheet drops the field; coach reads the profile goal weight. (Cross-tab
   slice first, smallest risk surface.)
2. **Body hub** — weight hero upgrade (chart + goal badge + BMI caption +
   delta change), measurement sparkline tiles, `EntriesSheet` chart header,
   `LogMeasurementDialog.initialType`.
3. **Goal & Plans sections + removals** — goal card refresh + Edit entry
   points, plan launcher cards + AppNavGraph callbacks, Account → Settings
   (state migration), Identity/Vitals cards removed, HC nudge row.
4. **Polish + verification** — spacing/dark-mode pass, full gate, emulator
   screenshots.

## Non-goals

- Progress photos; custom measurement types; unit preferences (imperial);
  editing diet/training plans from Profile; a vitals/health-data section;
  auto-applying recommended targets to Food; any new time-series tables or
  repository read APIs; accounts/cloud/AI (local-first).
- **Fasting plan card** — verified absent: fasting exists only as unpersisted
  `FoodViewModel` UI state with no repository observable; a card requires a
  persisted fasting store first (future Food work).
- **Health-Connect-sourced weight** in the hero/series — HC weight lands only
  in daily summaries today and is not imported into `body_metrics`; users who
  track weight exclusively via HC keep an empty hero (accepted gap; importing
  HC weights as `body_metrics` rows — the entity already has `source`/
  `externalId` for dedupe — is the designated future fix).
