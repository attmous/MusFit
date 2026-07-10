# Today dashboard — Google-Health chart redesign (design)

**Date:** 2026-06-28
**Status:** Approved design — pending spec review, then implementation plan
**Scope:** Today miniapp only. Builds a reusable chart kit but adopts it only on Today this slice.

## Goal

Restyle the Today dashboard so its data visualisations feel like the charts in Google's
new Health app — soft rounded bars with a highlighted day and a value bubble, a smooth
area-filled trend line, and cleaner rings — using the app's own theme palette (Today
accent = Coral). The "This week" 2×2 text grid is replaced by real charts. No data
semantics change; this is a presentation + light data-surfacing slice.

This is the first of an intended series: the chart kit is built **reusable** so Food
(`CalorieRing`) and Training (`ExerciseTrendChart`) can adopt it in later slices.

## Non-goals (YAGNI)

- No new chart library or other dependency — hand-rolled Compose `Canvas`, matching the
  existing `GoalRing` / `ExerciseTrendChart` / `CalorieRing` precedent.
- No Room schema change and **no migration** — the per-day series the charts need is
  already fetched; we only surface it.
- No refactor of the Training chart code. We **reuse** `TrendChartScaler` in place
  (import only) and do not edit any `ui/training/**` or `domain/training/**` file. (A
  future slice can converge the scalers into `domain/charts/` when Training adopts the
  kit.)
- No dark-theme work (the theme has a planned dark seam; out of scope here).
- Food/Training do not adopt the kit in this slice.

## Architecture

Three layers, respecting the app's one-directional dependency
(Compose → ViewModel → Repository → DAO). Only the first three are touched; the
repository/DAO layer is untouched.

```
domain/charts/   (new, pure)      ← BarChartScaler  (+ reuse domain/training/TrendChartScaler)
ui/components/charts/ (new)       ← MetricRing, WeekBarChart, TrendLineChart, ChartDefaults
ui/today/                         ← TodayViewModel maps series → WeeklyChartsUiState;
                                    TodayScreen swaps in the kit + new cards
domain/today/WeeklyGoalsCalculator← surface the already-computed per-day series
```

### Layer 1 — Domain

#### 1a. Surface the per-day series through `WeeklyGoals`

`WeeklyGoalsCalculator.compute(...)` already receives `loggedCaloriesPerDay`,
`stepsPerDay`, `weights`, `calorieGoalKcal`, and `stepGoal`, but discards the per-day
detail. Add fields to `WeeklyGoals` (all with defaults so the single non-test constructor
and the two test `compute(...)` callsites keep compiling unchanged):

```kotlin
data class WeeklyGoals(
    val sessionsDone: Int,
    val sessionTarget: Int,
    val calorieOnTargetDays: Int,
    val trackedDays: Int,
    val stepGoalDays: Int,
    val weightAvgKg: Double?,
    val weightDeltaKg: Double?,
    val targetWeightKg: Double?,
    // new — per-day series for the charts (defaults preserve existing callsites)
    val caloriesPerDay: List<Double?> = emptyList(),   // 7 entries Mon..Sun, null = untracked
    val calorieGoalKcal: Double = 0.0,                 // for the bar target line
    val stepsPerDay: List<Long> = emptyList(),         // 7 entries Mon..Sun (kept for a later steps view)
    val stepGoal: Long = 0L,
    val weightPoints: List<WeightPoint> = emptyList(), // for the trend line, chronological
)

data class WeightPoint(val epochMillis: Long, val valueKg: Double)
```

`compute(...)` populates them from data already in scope:
`caloriesPerDay = loggedCaloriesPerDay`, `calorieGoalKcal = calorieGoalKcal`,
`stepsPerDay = stepsPerDay`, `stepGoal = stepGoal`,
`weightPoints = weights.map { WeightPoint(it.first, it.second) }`.
`compute()`'s internal weekly aggregates are unchanged (they filter the series by window
themselves), so existing assertions still pass.

The only constructor callsite is `compute()` itself
([WeeklyGoalsCalculator.kt:49](app/src/main/java/com/musfit/domain/today/WeeklyGoalsCalculator.kt:49));
the two test callsites call `compute()` (not the constructor) and assert only existing
fields, so they are unaffected.

#### 1b. Widen the weight fetch window (ViewModel, one line)

`observeWeekly()` currently fetches weights from `weekStart - 7 days`
([TodayViewModel.kt:139](app/src/main/java/com/musfit/ui/today/TodayViewModel.kt:139)) —
enough for the prior-week delta but too short for a trend. Widen it to ~30 days before
`weekStart` (`weightFromMillis = (weekStart.toEpochDay() - 30L) * DAY_MILLIS`).
`observeWeightSeries(fromMillis)` already takes a from-bound, so this is a bound change
only — no new query, no DAO change. The prior-week window used by `weightTrend()` stays
inside the 30-day span, so the delta is unchanged.

#### 1c. `BarChartScaler` (new pure object, `domain/charts/BarChartScaler.kt`)

Pure, Android-free, modelled on `TrendChartScaler`'s edge-case discipline
(empty / all-null / flat). Normalises bar values + an optional target to fractions in
`[0,1]` against a max with headroom, so bars never touch the top and the target line sits
comfortably:

```kotlin
data class BarChartGeometry(
    val bars: List<Double?>,     // fraction in [0,1] per bar; null passes through (untracked)
    val targetFraction: Double?, // fraction in [0,1] for the target line, or null
    val maxValue: Double,        // the scaled max (with headroom) used for normalisation
)

object BarChartScaler {
    // headroom so the tallest bar / target sits below the top edge
    fun compute(values: List<Double?>, target: Double?, headroom: Double = 1.15): BarChartGeometry
}
```

Rules: `maxValue = max(max(non-null values), target ?: 0) * headroom`; if there is no
positive data, `maxValue = 0`, all fractions `0.0`/`null`, `targetFraction = null`. Each
non-null value → `value / maxValue` (clamped `[0,1]`); nulls pass through.

#### 1d. Reuse `TrendChartScaler` for the trend line

The weight trend line reuses `com.musfit.domain.training.TrendChartScaler.computeChartGeometry(...)`
unchanged (import only; **no edit to Training**). It already maps a `List<Double>` to
inverted pixel `ChartPoint`s with padding and handles empty/single/flat series.

### Layer 2 — Reusable chart kit (`ui/components/charts/`)

All hand-rolled `Canvas`, stateless, colours passed in (caller supplies the Today accent
via `tabAccentFor(AppDestination.Today)`), tokens from `MusFitTheme`. New package so it is
clearly shared, not Today-specific.

**`ChartDefaults`** — a small token object (single source for the look):

```kotlin
object ChartDefaults {
    val ringStroke = 8.dp
    val barWidth = 22.dp
    val barCorner = 6.dp
    val contextAlpha = 0.24f   // non-selected bars
    val areaAlpha = 0.10f      // trend area fill
    val lineStroke = 2.5.dp
    val dotRadius = 5.dp
    val targetDash = floatArrayOf(2f, 6f)
}
```

**`MetricRing`** — restyle of `GoalRing` (the daily rings adopt it; Food's `CalorieRing`
can later):

```kotlin
@Composable
fun MetricRing(
    progress: Float,                 // coerced to [0,1]
    color: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = MusFitTheme.colors.track,
    strokeWidth: Dp = ChartDefaults.ringStroke,
    content: @Composable () -> Unit, // centered label/value
)
```

Full-circle track arc + progress arc from `-90°`, `StrokeCap.Round` (soft, Google-ish),
centred `content`. Same math as `GoalRing` but rounded-cap + thicker stroke + a content
slot instead of a hardcoded label.

**`WeekBarChart`** — the centerpiece:

```kotlin
data class BarDatum(val value: Double?, val label: String)   // label = day initial; null value = untracked

@Composable
fun WeekBarChart(
    bars: List<BarDatum>,
    accent: Color,
    modifier: Modifier = Modifier,
    target: Double? = null,
    selectedIndex: Int? = null,
    valueFormatter: (Double) -> String = { it.roundToInt().toString() },
    onBarSelected: (Int) -> Unit = {},
)
```

Rounded-**top**, square-baseline bars (built with a `Path`: line up the left edge,
quadratic round at each top corner, down the right edge — not an all-corners `RoundRect`).
Selected bar = solid `accent`; others = `accent.copy(alpha = ChartDefaults.contextAlpha)`;
untracked (null) = a faint baseline tick or empty slot. Optional dashed target line via
`PathEffect.dashPathEffect(ChartDefaults.targetDash)`. A value **bubble**
(`drawRoundRect` + small pointer + `drawText`) sits above the selected bar showing
`valueFormatter(value)`. Day initials below in `onSurfaceVariant`, the selected initial in
`accent`. Tap selection via `Modifier.pointerInput { detectTapGestures }` mapping x → bar
index (state hoisted; caller owns `selectedIndex`). Geometry from `BarChartScaler`.

**`TrendLineChart`** — smooth area line:

```kotlin
@Composable
fun TrendLineChart(
    values: List<Double>,
    accent: Color,
    modifier: Modifier = Modifier,
)
```

Geometry via `TrendChartScaler.computeChartGeometry(...)`. Builds a **smoothed** path
(Catmull-Rom → cubic Bézier across the points, so it reads like Google's curves rather
than the straight segments of `ExerciseTrendChart`), an area fill to the baseline at
`ChartDefaults.areaAlpha`, a `ChartDefaults.lineStroke` round-cap/join stroke, and an
end-dot (`accent` fill + `surface`-colour ring) at the latest point. Degrades gracefully
for 0/1 points (nothing / a single dot).

### Layer 3 — Today UI

#### 3a. ViewModel mapping

In the `observeWeekly().collect` block, in addition to `weekly`, derive a
`WeeklyChartsUiState` and store it on `TodayUiState`:

```kotlin
data class DayBar(val label: String, val calories: Double?)   // 7, Mon..Sun

data class WeeklyChartsUiState(
    val calorieBars: List<DayBar>,
    val calorieTarget: Double?,        // null when no goal set
    val onTargetDays: Int,             // weekly.calorieOnTargetDays
    val trackedDays: Int,              // weekly.trackedDays (7)
    val defaultSelectedIndex: Int?,    // resolved default highlight (see below)
    val weightTrend: List<Double>,     // chronological kg values from weightPoints
    val weightDeltaKg: Double?,        // weekly.weightDeltaKg
    val latestWeightKg: Double?,       // most recent measurement: weightPoints.last ?: weightAvgKg
    val sessionsDone: Int,
    val sessionTarget: Int,
    val stepGoalDays: Int,
)
```

`TodayUiState` gains `weeklyCharts: WeeklyChartsUiState? = null` and
`selectedCalorieDayIndex: Int? = null` (the user's explicit tap override, null until
tapped). Day labels are the Mon..Sun initials (`"M","T","W","T","F","S","S"`).

**Selection resolution (reactive).** The mapping computes only
`defaultSelectedIndex` (today's weekday index if today is in the displayed week and
tracked, else the last tracked day, else null) — it never reads the user override, so it
stays a pure function of the data. The `WeeklyCaloriesCard` resolves the effective
highlight as `selectedCalorieDayIndex ?: weeklyCharts.defaultSelectedIndex`. The VM
function `onCalorieDaySelected(index)` sets `selectedCalorieDayIndex` directly on the
state, so a tap updates the highlight immediately (not only on the next weekly emission),
while a fresh data emission that leaves the override null still shows the default bubble.

The raw `weekly: WeeklyGoals?` field stays populated but is no longer read by the UI.

#### 3b. Screen layout (`TodayScreen`)

Final vertical order (Column, existing 16.dp rhythm):

1. Header — unchanged (`Today` + date + settings/Tune → `openGoalsEditor`).
2. `CoachBriefingCard` — unchanged.
3. **`DailyRingsCard`** — `GoalRing` → `MetricRing` (rounded cap, softer track); rings
   keep their existing per-metric colours; `MacroBar` retained unchanged.
4. **`WeeklyCaloriesCard`** (new) — header (`Calories · this week` + an
   `N/7 on target` pill) over `WeekBarChart` (calorie bars, target line, tap-to-select +
   bubble). Accent = Today/Coral.
5. **`WeightTrendCard`** (new) — header (`Weight · 30 days`, latest weight, a delta pill)
   over `TrendLineChart`. Accent = Today/Coral.
6. **`WeekStatsRow`** (new) — 2-up compact stat tiles (reuse the `WeeklyMiniTracker`
   look): `Sessions` (done/target) · `Step-goal days` (n/7).
7. **Training glimpse** — keep the existing Training `GlimpseTile` (sets / volume).

Retired: the "This week" 2×2 grid and the standalone **Weight** `GlimpseTile`. Nothing is
lost — *calories-on-target* → the pill on card 4, *weight* → card 5, *sessions* +
*step-goal-days* → card 6, *latest weight* → card 5 header.

### Colour rule (stated for review)

The new charts (calorie bars, weight trend) wear the **Today tab accent (Coral
`#FF7A66`, via `tabAccentFor(AppDestination.Today).color`)** — consistent with the app
convention that a screen's charts use its tab accent (Training's `ExerciseTrendChart`
uses Indigo). The three daily rings keep their existing per-metric colours
(`Calories = brand/Emerald`, `Protein = macroProtein/teal`, `Steps = water/blue`) and are
restyled in **shape only**. This deliberately leaves the calorie ring (Emerald, the
app-wide calorie/brand colour, matching Food's `CalorieRing`) a different colour from the
weekly calorie bars (Coral, the screen accent). If you'd prefer calories to read Coral
everywhere on Today, that's a one-line change to `ringColor()` — flagged as an open
decision below.

## Testing

TDD where the logic is pure; Canvas drawing is verified by build + on-device screenshots
(no screenshot-test infra in this repo).

- **`BarChartScalerTest`** (new, JUnit) — modelled on `TrendChartScalerTest`'s `geom()`
  helper style: normalisation against max+headroom, target fraction, all-null series,
  flat series, empty series, clamping.
- **`WeeklyGoalsCalculatorTest`** (extend) — a new method asserting the surfaced series:
  `caloriesPerDay`/`stepsPerDay`/`weightPoints` pass through with correct order and
  null handling. Existing two methods untouched (defaults keep them green). Uses the
  existing `weekStart = 1_700_000_000_000L`, `day = 86_400_000L` constants.
- **`TodayViewModelTest`** (extend) — assert `weeklyCharts` mapping: bar values + labels,
  `calorieTarget`, `onTargetDays`, default `selectedCalorieDayIndex`, `weightTrend`
  ordering, delta sign, and the surviving stats. Follows the existing
  `StandardTestDispatcher` + inline-fakes + `advanceUntilIdle()` pattern; the inline fakes
  (`FakeFoodRepository.observeFoodPlan`, `FakeHealthRepository.observeWeightSeries` /
  `observeDailySummaries`) gain known weekly emissions so the weekly flow produces
  deterministic chart state.
- Chart composables (`MetricRing`, `WeekBarChart`, `TrendLineChart`): no unit tests;
  verified by `assembleDebug` + on-device screenshots via the device-verification flow.

**Full gate:** `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain`.

## Build / process constraints

- **Isolated worktree.** All implementation + verification happens in a git worktree
  created from committed `master`. The
  main working tree currently holds **another session's uncommitted Training WIP that must
  not be touched, committed, or discarded.** Working from a clean worktree keeps this slice
  independent of that WIP.
- **Do not edit any `ui/training/**` or `domain/training/**` file** — `TrendChartScaler` is
  reused by import only. This also avoids colliding with the concurrent Training WIP.
- Source `. .\scripts\android\android-env.ps1` before Gradle.
- Deploy: push the verified branch to `origin/master` only when the user asks, advancing
  `master` without disturbing the main tree's WIP (as done previously).

## Files touched (preview for the plan)

| File | Change |
|------|--------|
| `domain/today/WeeklyGoalsCalculator.kt` | add 5 defaulted fields + `WeightPoint`; populate in `compute()` |
| `domain/charts/BarChartScaler.kt` | **new** — pure bar normaliser + `BarChartGeometry` |
| `ui/components/charts/ChartDefaults.kt` | **new** — token object |
| `ui/components/charts/MetricRing.kt` | **new** — restyled ring with content slot |
| `ui/components/charts/WeekBarChart.kt` | **new** — rounded bars + selection + bubble |
| `ui/components/charts/TrendLineChart.kt` | **new** — smoothed area line (reuses `TrendChartScaler`) |
| `ui/today/TodayViewModel.kt` | widen weight window; add `WeeklyChartsUiState`/`DayBar`, mapping, `selectedCalorieDayIndex`, `onCalorieDaySelected` |
| `ui/today/TodayScreen.kt` | `MetricRing` swap; new `WeeklyCaloriesCard`, `WeightTrendCard`, `WeekStatsRow`; retire 2×2 grid + Weight glimpse |
| `test/.../domain/charts/BarChartScalerTest.kt` | **new** |
| `test/.../domain/today/WeeklyGoalsCalculatorTest.kt` | add series pass-through test |
| `test/.../ui/today/TodayViewModelTest.kt` | add `weeklyCharts` mapping tests + fake weekly emissions |

## Open decisions (for spec review)

1. **Calorie colour cohesion** — keep the calorie ring Emerald (app-wide calorie/brand
   colour) while the calorie bars are Coral (screen accent), *or* recolour the calorie
   ring to Coral so calories read consistently on Today? Default in this design: keep
   Emerald ring + Coral bars.
2. **Bar metric** — the hero weekly bar chart shows **calories vs target**. `stepsPerDay`
   is surfaced too; a steps view is deferred (no toggle this slice) to keep the card
   focused. OK to defer?
3. **Trend window** — 30 days for the weight line. Acceptable, or prefer 14/8 weeks?
4. **Selection affordance** — bars are tappable to move the highlight/bubble; default
   selection is today (or last tracked day). Keep interactive, or static "today" only?
