# MusFit — Today Tab Rethink: Coach Feed + Configurable Metric Carousel

**Date:** 2026-07-01
**Status:** Approved design, pending implementation plan
**Area:** `com.musfit.ui.today` + `domain/coach` + one Room migration; shared components reused from `com.musfit.ui.components`

> Part 1 of a two-part rethink (Today first, Profile second — Profile gets its own
> spec). Explicitly requested Today work, so the usual "keep changes scoped to
> Food" guardrail does not apply.

## Background

Today is currently a metrics dashboard with a small coach on top: a
`CoachBriefingCard` (deterministic cues with a pager), a `DailyRingsCard`
(calories/protein/steps rings + macro bar), a `WeeklyCaloriesCard` (bar chart),
a `WeightTrendCard` (30-day line), a `WeekStatsRow`, and a training glimpse
tile. The user wants the emphasis inverted, referencing Google's Health app
home: **the AI coach's messages become the star of Today**, and the metrics
compress into a **user-configurable dashboard in the header region** that
highlights what the user is currently focused on.

The real conversational AI does not exist yet (MusFit is local-first, no cloud
AI). This design builds the *product shape* now — a persistent coach feed and
its storage — filled by an upgraded deterministic rules engine
([`CoachEngine`](../../../app/src/main/java/com/musfit/domain/coach/CoachEngine.kt)).
A future AI coach writes into the same store behind the same interface.

## Approved product choices

| Decision | Choice |
|---|---|
| Sequencing | Today first; Profile is a separate follow-up spec |
| Coach shape | **Running feed** — a timeline of discrete coach messages through the day, newest on top (not a single daily digest, not digest+chips) |
| Header dashboard | **Google-style metric carousel** — a swipeable pager of metric cards (hero ring + stat chips) with page dots |
| Detail depth | **Lean & deep-link** — Today keeps *no* detailed chart cards; tapping any carousel metric jumps to its home tab |
| Feed memory | **Persistent timeline** — messages are stored in Room as they first appear; scrollable history across days, read/dismiss state |
| Chat button | **Preview FAB** — visible chat button opening a "Coach chat is coming" placeholder (no fake conversation) |
| Carousel configuration | **Pick & order, app auto-arranges** — user selects which metrics matter and their order; the app lays them into carousel pages; hero slot = first pinned metric (default: calories) |
| Goals editor | Folded into the dashboard edit sheet (the standalone `TodayGoalsEditorSheet` bottom sheet is removed) |

## Screen structure (top → bottom)

1. **Shared header** (`MusFitScreenHeader`, Coral accent): "Today" title +
   date subtitle, one neutral trailing action — **✎ edit dashboard** (replaces
   the current `Tune` goals icon).
2. **Metric carousel** — Today's contained Coral summary card
   (`MusFitSummaryCard` treatment), now a horizontal pager.
3. **Coach feed** — persistent message stream, newest first, grouped by day.
4. **Preview chat FAB** — Coral, anchored bottom-right with `spacing.lg`
   clearance above the `FloatingPillNav`; persistent (does not hide on
   scroll); the feed list gets enough bottom content padding to scroll clear
   of it. The "soon" cue is a small text badge on the FAB.
   *Superseded 2026-07-02: the chat button moved into the `FloatingPillNav`
   bar slot (all tabs, replacing the old "+"), and the floating copy left
   Today — see
   [`2026-07-02-fab-consolidation-design.md`](2026-07-02-fab-consolidation-design.md).
   The "Soon" text badge was also removed; the coming-soon cue lives in the
   button's `contentDescription` and the `ChatPreviewSheet` content, which
   are unchanged.*

Removed from Today entirely: `WeeklyCaloriesCard`, `WeightTrendCard`,
`WeekStatsRow`, the training glimpse tile, the macro bar, and the old
`CoachBriefingCard` + `DailyRingsCard` (replaced by feed + carousel).

**Interim reality of "lean & deep-link" (accepted):** in v1 the deep-link
destinations may show *current values only* — Food has no weekly-calories
chart and Profile's vitals card has no history yet. This spec does not add
them; the Profile follow-up spec is the planned consumer of the chart kit
(`WeekBarChart`, `TrendLineChart`), which **stays in
`ui/components/charts/`** even while temporarily unreferenced (the carousel
still uses `MetricRing`). Nobody should "restore" the old Today charts to fix
this — the depth lands in the destination tabs.

## Section 1 — Metric carousel

**Visual.** One contained, inset Coral card (`shapes.large`, `TabAccent.container`
tint, accent-ink `TabAccent.onContainer` figures, tint-safe neutral ring
tracks — per the design-language rules). Inside it a `HorizontalPager`:

- **Page 1 (hero page):** a large `MetricRing` for `pins[0]` (the hero) plus
  compact stat chips for `pins[1]` and `pins[2]`. With fewer pins, chip slots
  collapse (the hero grows into the space).
- **Pages 2+:** `pins[3...]` in chunks of four, as a 2×2 chip grid.
- Page indicator dots at the card's bottom (active dot elongated, Coral);
  **dots are hidden when there is only one page**.
- Hero rendering: if the hero metric has a goal, ring shows progress with the
  headline figure centered (e.g. "1,450 kcal left"); if it has no goal
  (e.g. weight), it renders as a large figure + delta instead of a ring.

**Metric resolver states.** Every metric resolves to one of three states,
computed by a pure domain mapper (`MetricResolver`-style, unit-tested):

1. **value + goal** → figure with progress (ring-capable),
2. **value without goal** → figure (+ delta where meaningful),
3. **no data** → an em-dash "—" with a one-word caption ("No data" /
   "Not connected" for Health Connect metrics).

Pinned-but-empty metrics keep their slot (page layout stays stable), and
their tap still deep-links — the destination tab is the fix-it path.

**Metric pool** (v1):

| Metric | Definition / formula | Source | Deep-link |
|---|---|---|---|
| Calories | remaining = goal − eaten (eaten shown when no goal) | FoodRepository daily nutrition + goal | Food |
| Protein / Carbs / Fat | grams vs goal grams | FoodRepository | Food |
| Water | intake vs water goal | FoodRepository | Food |
| Steps | today vs `user_goals` step goal | HealthRepository daily summary | Profile |
| Weight | latest entry + 7-day delta; goal = `user_goals.targetWeightKg` | HealthRepository weight series | Profile |
| Body fat % | latest measurement + delta | ProfileRepository measurements | Profile |
| Sessions this week | completed vs `user_goals` weekly target; week boundary = the app's existing week-start convention (as used by the food weekly plan) | TrainingRepository history | Training |
| Active calories | today's Health Connect active energy | HealthRepository | Profile |
| Resting HR | today's Health Connect value | HealthRepository | Profile |
| Calorie balance | eaten − Health Connect active calories; **no-data when HC absent** (Training tracks no energy data) | Food + Health | Food |
| Logging streak | consecutive days with ≥1 diary entry, counting back from today (from yesterday if today is still empty) | FoodRepository diary | Food |

**Interaction.** Tapping any metric navigates to its home tab (plain
bottom-nav navigation via the existing `AppNavGraph` destinations; no
sub-route anchors in v1).

**Configuration (✎ edit sheet).** A bottom sheet listing the metric pool:
check to pin, reorder (drag or up/down controls); `pins[0]` is the hero. The
sheet **enforces a minimum of one pin** (the last pinned metric cannot be
unchecked). Below the pins, the sheet **always shows a "Goals" section** with
the numeric fields that previously lived in `TodayGoalsEditorSheet` — daily
step goal, weekly session target, target weight — which continue to write to
the existing `user_goals` store. Target-weight prefill: from
`user_goals.targetWeightKg`, falling back to `user_profile.goalWeightKg` when
unset; the sheet writes to `user_goals` only. (Full reconciliation of the two
weight-goal stores is deferred to the Profile follow-up spec; interim
precedence for the carousel and coach is `user_goals`.) The old standalone
goals sheet is removed.

**Defaults & upgrade path.** Default pins: **Calories (hero), Steps,
Protein**. The 25→26 migration **seeds these three rows** so existing users
see the default carousel on first launch after update (asserted in the
migration test). A defensive read of an empty table also falls back to the
defaults.

## Section 2 — Coach feed

**Message anatomy.** Category icon + label (small caps), timestamp
(`firstSeenAt`, current time zone), bold one-line title, 1–2 line body,
optional single deep-link action button (accent-tonal), unread dot. Messages
are grouped under day headers ("Today", "Yesterday", then dates) — **grouping
is by `dayEpochDay`; ordering within a group is `firstSeenAt` descending**.
Calm, dense cards on the cream background — neutral surfaces, not
accent-tinted (the accent is spent on the carousel). The feed's section
header is a plain `SectionHeader("Coach")` — no "Mark all read" action (see
read model below).

**Categories** (superset of today's cue kinds):

- **Plan** — morning kickoff, day framing ("Front-load protein today…")
- **Nutrition** — pacing, macro gaps, water ("Protein's 28 g behind…")
- **Training** — recency nudges, routine suggestions, PR callouts
- **Trend** — weight direction, adherence streaks over the week
- **Achievement** — goals hit, streaks extended
- **Recap** — evening close-out of the day

**Read model (single source of truth: the `isRead` column).** A message is
unread while `isRead = false`; the dot is a soft "new since last visit"
signal, not an inbox. All non-dismissed messages are marked read **when the
user leaves the Today tab** (navigation away / screen pause), via one
`markAllRead()` call — so new dots stay visible for the whole visit and are
gone on the next one. There is no manual mark-read control.

**Dismiss.** Swipe or long-press dismisses **that day's instance of the
rule**: `isDismissed = true` — a **soft delete; the row is never removed**,
because it is the tombstone the upsert respects (a hard delete would let the
engine re-insert the same message the same day). The same rule may naturally
produce a new message on a later day. (Permanent per-rule muting is a
non-goal.)

**Actions.** Tap action → deep-link (sealed `CoachAction` mapped to
navigation: open tab / start routine / open food add flow). Degradation
rules: an action whose target can no longer be resolved (e.g. a stored
routine id whose routine was deleted) **falls back to tab-level navigation**;
an unrecognized `actionType` (future writers) renders the message **without a
button**.

**Empty / first-run state.** Purely presentational: when `observeFeed` emits
zero non-dismissed messages (fresh install), the feed area renders the shared
`EmptyState` composable ("Let's get started — log your first meal and I'll
take it from there") plus the default carousel. It is never persisted and has
no `ruleKey`. Established users on a quiet day simply see their history — no
filler is injected.

**Chat FAB.** Tapping opens a small sheet: "Coach chat is coming — your coach
will answer questions right here." No input field pretending to work.

## Section 3 — Storage & generation

**New Room table `coach_messages`** (migration **25 → 26** — the database is
currently at version 25; CLAUDE.md's "version 21" is stale):

| Column | Type | Notes |
|---|---|---|
| `id` | PK autogen | |
| `dayEpochDay` | Int | the engine snapshot's day (see generation) |
| `ruleKey` | String | stable rule identity, e.g. `protein_gap`, `legs_recency` |
| `category` | String | Plan / Nutrition / Training / Trend / Achievement / Recap |
| `title` | String | |
| `body` | String | |
| `actionType` | String? | sealed-action discriminator |
| `actionData` | String? | e.g. routine id |
| `firstSeenAtEpochMillis` | Long | stamped on first insert, never updated |
| `isRead` | Boolean | |
| `isDismissed` | Boolean | soft-delete tombstone |
| `source` | String | `rules` now; `ai` later |

Unique index on **`(dayEpochDay, ruleKey, source)`** — the dedupe key.
(Including `source` now means a future AI writer can never silently collide
with a rules message; choosing this at creation costs nothing, fixing it
later costs a migration.)

**Dashboard pins** persist in the same migration: a `dashboard_pins` table —
`metricId` (String PK), `position` (Int); hero = position 0. Seeded with the
three defaults (see Section 1). **Both tables are owned by the new `CoachDao`
and exposed through `CoachRepository`** (they ship in the same migration; no
separate module).

**Generation model — no background jobs.** Generation runs when the Today
screen resumes (`ON_RESUME` via `repeatOnLifecycle` — no new
process-lifecycle dependency) and on relevant data-change emissions while it
is visible. Each pass:

- resolves **`LocalDate.now()` at invocation time** (not ViewModel init — the
  current `TodayViewModel` resolves its date once at init; this changes),
- feeds a snapshot to `CoachEngine`, which returns today's candidates with
  stable `ruleKey`s,
- **upserts by `(dayEpochDay, ruleKey, source)`**:
  - new key today → insert, stamp `firstSeenAt` = now → appears at feed top;
  - existing key → refresh `title`/`body`/`actionType`/`actionData` **only if
    changed** (no-op syncs skip the write so Room invalidation doesn't churn
    the UI); `firstSeenAt`, `isRead`, `isDismissed` are never touched by sync.
    A message's content is deliberately *live* while its rule keeps firing —
    it freezes only when the rule stops refreshing it;
  - dismissed → never re-inserted or un-dismissed that day;
- **never back-fills prior days**: a time-gated message missed before
  midnight (e.g. an evening Recap the user never opened the app for) is
  simply not generated — holes in history are accepted, consistent with the
  no-background-work stance,
- prunes rows with `dayEpochDay < today − 90`.

**`CoachEngine` upgrade.** Keeps its pure, synchronous, **clock-free**
(time-of-day arrives as snapshot data; the clock lambda stays injected in the
ViewModel), unit-testable shape. Changes: each rule gains a stable `ruleKey`
and category; copy gets richer (title + body instead of one cue line); new
rule families for Plan (morning-gated), Recap (evening-gated), and
Achievement. **`CoachInput` must be extended** — today it carries only
calories/protein vs goals, days-since-workout, next routine, a pre-computed
weight delta, steps, and time of day. The new rules need: full macro totals
(carbs/fat), a water summary, windowed weight deltas (or the series), and
diary-adherence/streak data. Extending the snapshot and its ViewModel
assembly is explicitly part of slice 1b.

**AI-readiness.** The feed reads from `coach_messages` regardless of
`source`. A future AI coach is a second writer into the same table
(`source = 'ai'`, its own keys) — zero UI or schema change needed.

## Section 4 — Design-language reconciliation

This design extends the approved cross-tab language (Direction C) rather than
fighting it:

- The **metric carousel is Today's summary card** — contained, inset,
  `TabAccent.container` Coral tint, accent-ink figures, tint-safe ring
  tracks. This supersedes the consistency spec's note that Today's summary
  card is the static `DailyRingsCard` (noted there).
- The shared `MusFitScreenHeader` stays exactly as Phase 1 built it; only the
  trailing action changes meaning (goals `Tune` → dashboard `✎`, still
  neutral-tinted).
- **`SectionHeader` and `EmptyState` do not exist yet** — the design-language
  plan deferred them to "the phase whose tab first needs them." Today is that
  tab: slice 2 creates both in `com.musfit.ui.components`, per the
  definitions in the consistency spec (Section 4 there), and the feed uses
  them.

## Section 5 — Architecture

Existing layering preserved: Compose → `TodayViewModel` → repositories → DAO.

- **`CoachRepository`** (interface + `LocalCoachRepository`, Hilt `@Binds` in
  `RepositoryModule`): `observeFeed(): Flow<List<CoachMessage>>`,
  `syncToday(day, candidates)` (transactional upsert/dedupe/prune),
  `dismiss(id)`, `markAllRead()`, `observeDashboardPins()`,
  `saveDashboardPins(ordered)`. Backed by the new `CoachDao`.
- **`TodayViewModel`** slims down: carousel state (pinned metrics resolved
  against live data flows via `combine`), feed state, edit-sheet state. The
  weekly-chart flows and their `buildWeeklyCharts` mapping leave Today.
- **Domain:** upgraded `CoachEngine` + extended `CoachInput` (pure Kotlin, no
  Android deps), plus a pure metric-resolver mapper that turns raw repository
  values + goals into the three display states of Section 1 (testable without
  Android).

## Section 6 — Testing (TDD per repo norms)

- **Domain (pure JUnit):** each new/upgraded coach rule — expected
  ruleKey/category/copy for known snapshots; time-of-day gating (Plan only
  mornings, Recap only evenings); metric resolver: all three states per
  metric, formulas above (calorie balance nulls without HC, streak
  counts-back rule, week boundary), progress math.
- **Repository (Robolectric, in-memory Room):** migration 25→26 round-trip
  **including the three seeded default pins** (schema `26.json` committed);
  upsert-dedupe by `(day, ruleKey, source)`; `firstSeenAt` immutability on
  refresh; **a no-op re-sync does not alter any column**; **a dismissed row
  survives a subsequent `syncToday`** and is never resurrected; action fields
  refresh with content; 90-day prune predicate on `dayEpochDay`; pin-ordering
  round-trip; empty-pins defensive default.
- **ViewModel (JUnit + fakes, `StandardTestDispatcher`):** feed ordering
  (newest-first within day groups, groups by `dayEpochDay`); leave-tab
  triggers `markAllRead()`; dismiss flow; carousel derives pages from pins +
  live data (1-pin, 3-pin, 8-pin cases; dots hidden at one page); edit sheet
  saves pins and goal fields (min-one-pin enforced; target-weight prefill
  fallback); deep-link callbacks fire the right navigation events; **action
  degradation: tapping a message whose routine was deleted falls back to
  opening Training**; **generation across midnight: a mutable dateProvider
  crossing midnight writes under the new day**; empty/first-run state.
- **Full verification per phase:**
  `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain`
  plus install + visual check on the seeded emulator, light and dark.

## Implementation slices

1. **(a) Feed storage foundation** — migration 25→26 (`coach_messages`,
   `dashboard_pins` + seeded defaults, schema JSON), `CoachDao`,
   `CoachRepository` with the existing rules minimally adapted to
   ruleKey/category; round-trip + upsert/dedupe/tombstone/prune tests.
   **(b) Engine upgrade** — extended `CoachInput` + ViewModel snapshot
   assembly, richer copy, Plan/Recap/Achievement families, gating tests.
2. **Coach feed UI** — create `SectionHeader` + `EmptyState` shared
   composables; replace `CoachBriefingCard` with the feed (day groups,
   unread/leave-to-read, dismiss, actions + degradation), generation on
   resume, empty state, chat-preview FAB.
3. **Metric carousel** — replace `DailyRingsCard` with the pager summary
   card, metric resolver, deep-link taps, edit sheet (pins + Goals section),
   remove `TodayGoalsEditorSheet`.
4. **Slim-down & polish** — remove `WeeklyCaloriesCard`, `WeightTrendCard`,
   `WeekStatsRow`, training glimpse from Today; locale fix for day labels
   (drop the US-locale formatter); final spacing/dark-mode pass; emulator
   verification of the whole tab.

Each slice is independently verified (tests + build) before the next.

## Risks / constraints

- **Migration discipline:** v25→26 must ship with `26.json`, seeded-pin
  assertions, and round-trip tests; no destructive fallback exists.
- **No background work:** generation is foreground-driven by design; missed
  time-gated messages leave accepted holes in history (see Section 3).
- **Chart-card removal is a real feature change** (unlike the presentational
  design-language phases) — the weekly charts intentionally leave Today, and
  v1 deep-link destinations may show current values only (interim reality,
  Section "Screen structure"). The chart kit stays for the Profile follow-up.
- **Dual weight-goal stores** (`user_goals.targetWeightKg` vs
  `user_profile.goalWeightKg`) are bridged, not merged, in this spec —
  reconciliation belongs to the Profile follow-up.
- **OneDrive/Gradle** flakiness on `app/build` is environmental; recover per
  `AGENTS.md`.

## Non-goals

- Real conversational AI, cloud AI, accounts, sync, analytics (local-first).
- Message-level reactions (👍/👎) and permanent per-rule muting — cheap to
  add later.
- Back-filling missed time-gated messages for prior days.
- Sub-route deep-link anchors (e.g. scroll-to-weight-card) — v1 navigates to
  the tab.
- New charts/history views in Food or Profile (Profile follow-up spec).
- Profile tab changes — separate follow-up spec.
