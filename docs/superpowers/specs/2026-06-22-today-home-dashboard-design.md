# MusFit Today — Home Dashboard with On-Device Coach

**Date:** 2026-06-22
**Status:** Approved design pending spec review, then plan
**Area:** Today miniapp (`com.musfit.ui.today`) + a new `domain/coach`, with read-only reuse of Food / Training / Health repositories.

## Background

Today is currently a bare text list (calories, macros, a one-line training summary, Health Connect steps/active-calories/weight) on plain Material — no goals, no targets, no design tokens. It should become the **home**: a glanceable dashboard that pulls Food and Training numbers together, tracks daily and weekly goals, and leads with an on-device "coach" that gives cues, hints, and a daily briefing.

Confirmed direction: **coach-led layout (A)**, **on-device rules engine** (no cloud, no LLM — keeps MusFit local-first), coach covers **nutrition pacing + training nudges + trends/progress**, and the weekly tracker shows **all four** of: sessions/week, calorie-goal adherence, body-weight trend, step-goal days.

## Goals

1. Replace the text list with a token-styled (`MusFitTheme` B1) dashboard home.
2. Lead with a **coach briefing** card: greeting + prioritized cues + quick actions.
3. Show **daily goals** as three rings — Calories, Protein, Steps — plus a macro bar.
4. Glimpse tiles into **Training today** and **Weight (7-day trend)**.
5. Track four **weekly goals**.
6. Deep-link rings/tiles/coach actions into the right tab.
7. Stay strictly local-first and offline.

## The screen (top → bottom)

1. **Header** — "Today" + full date; a goals/settings control (slider icon) opening the goals editor.
2. **Coach briefing** (emerald hero) — time-aware greeting ("Good morning, Moustafa"), the top 1–2 prioritized cues with small leading icons, action chips (e.g. "Start leg day", "Log a snack"), and a `n / total` pager to step through remaining cues.
3. **Daily rings** card — Calories (eaten/goal), Protein (g/goal), Steps (count/goal) as donut rings with centered values; a thin carbs/protein/fat macro bar below with gram labels.
4. **Glimpse tiles** row — Training today (sets · volume, or "No workout yet" → start) and Weight (latest kg + 7-day trend arrow).
5. **Weekly goals** card — 2×2 mini trackers: Sessions (3/4 + dots), Calories on target (5/7 + dots), Weight → target (latest + weekly delta), Step-goal days (4/7).

## The coach — a pure rules engine

New `domain/coach/` package, pure Kotlin, no Android/Room, fully unit-tested.

- **Input snapshot** (`CoachInput`): today's nutrition totals + goals (calories, protein, macros), water vs goal, training recency (days since each muscle group / last session), next planned routine, 7-day body-weight average + direction, today's steps vs goal, current time-of-day, and the weekly aggregates.
- **Output**: an ordered `List<CoachCue>` where `CoachCue = (id, category, priorityScore, text, action: CoachAction?)`. Categories: `NutritionPacing`, `TrainingNudge`, `Trend`. `CoachAction` is a small sealed type (e.g. `OpenFood`, `OpenTraining`, `StartRoutine(id)`, `LogQuickFood`) the UI maps to navigation.
- **Rules** (each a small, testable function; examples):
  - *Nutrition pacing* — protein gap with a concrete suggestion; calories remaining + rough "left for dinner"; over-sugar / low-fiber; water behind.
  - *Training nudges* — "N days since legs"; rest-day/recovery when volume high; surface the next planned routine; new estimated-1RM PR.
  - *Trend* — weight 7-day average + direction vs target; weekly volume change; calorie-goal adherence (k/7).
- **Prioritization** — each rule yields a score (urgency × relevance); the engine sorts and the briefing renders the greeting + top 1–2, pager for the rest. Time-of-day shapes the greeting and which cues lead (morning = plan the day; evening = close the rings).
- **Briefing line** — a one-sentence summary composed from the top cues.

The engine is deterministic and synchronous; the ViewModel supplies the clock and the snapshot.

## Data sources & new storage

**Reused (read-only):**
- `FoodRepository` — `observeDailyNutrition`, the food goal (calorie/macro/water targets), and a new weekly calorie-adherence query.
- `TrainingRepository` — today's `TrainingSummary`, sessions this week, last-trained-by-muscle / recency, next routine, best estimated 1RM.
- `HealthRepository` — `observeDailySummary` (steps, active calories, latest weight) + a 7-day weight series for the trend.

**New storage — `user_goals`** (single-row table; DB **migration v17**, schema `17.json`): `stepGoal`, `weeklySessionTarget`, `targetWeightKg` (the three settings that don't exist yet). Calorie/macro/water goals stay in `food_goals`. Exposed via a `GoalsRepository` (or an extension of an existing repo) with observe + update; edited from the header control via a small goals editor (reuse Food's goal-editor patterns).

**New repository queries (TDD):** sessions completed this week, calorie-goal-adherence days this week, step-goal days this week, 7-day weight average + delta. These live next to their domain (training/food/health repos) and return plain data classes.

## Architecture

`TodayScreen` (Compose) → `TodayViewModel` (`@HiltViewModel`, single `StateFlow<TodayUiState>`) → repositories + `CoachEngine`.

- `TodayUiState` expands to: daily rings (value/goal triples), macro breakdown, training-glimpse, weight-glimpse, the four weekly trackers, and `coachCues: List<CoachCueUiState>`.
- The ViewModel `combine`s the repository flows (now including goals + weekly aggregates), builds a `CoachInput`, runs `CoachEngine.cues(input)`, and maps to UI state. Clock injected (like `dateProvider`) for testability.
- Navigation: `TodayScreen` gains `onNavigate(AppDestination)` / action callbacks; `AppNavGraph` wires them to switch tabs (and, where possible, open a specific add/routine flow).
- Re-skin onto `MusFitTheme` tokens (colors, shapes, spacing, type), matching Food. New reusable composables: `CoachBriefingCard`, `DailyRingsCard` (+ a `GoalRing`), `GlimpseTile`, `WeeklyGoalsCard`.

## Reuse vs new work

- **Reused:** all three repositories' existing flows, the Food goal model + goal-editor UI patterns, `MusFitTheme` tokens, the existing nav scaffold.
- **New (data):** `user_goals` table + migration v17; weekly-aggregate queries; 7-day weight series.
- **New (feature):** the `CoachEngine` rules + cue model; the dashboard composables; the goals editor for the three new settings.

## Implementation slices

Sequenced, each independently shippable and verified:

1. **Slice 1 — Dashboard shell & re-skin.** Token re-skin; header; daily rings (Calories/Protein/Steps from existing nutrition + Food goals + a temporary fixed step goal); macro bar; glimpse tiles (Training today, Weight trend); deep-link navigation. No new storage yet.
2. **Slice 2 — Goals & weekly tracker.** `user_goals` table (migration v17) + `GoalsRepository` + goals editor (step goal, weekly-session target, target weight); weekly-aggregate queries (TDD); the Weekly goals card.
3. **Slice 3 — Coach.** `domain/coach` engine + rules (nutrition pacing, training nudges, trends) with unit tests; `CoachBriefingCard`; wire cues + actions into the ViewModel and navigation.

## Navigation map

| Element | Action |
|---|---|
| Calories / Protein ring, macro bar | Open Food |
| Steps ring, Weight tile | Open Health |
| Training tile | Open Training |
| Coach "Start leg day" | Open Training → the planned routine |
| Coach "Log a snack" | Open Food add (snacks) |
| Header slider icon | Open goals editor |

## Testing & verification

- **Coach** — pure JUnit over `CoachEngine`: given snapshots (protein gap, days-since-legs, weight flat/down, steps short, time-of-day), assert the expected cues, ordering, and actions; assert empty/fresh-install snapshot yields a sensible welcome cue.
- **ViewModel** — fakes for the three repositories + a fixed clock; assert rings, weekly trackers, and mapped cues.
- **Repository / DAO** — Robolectric in-memory Room for the weekly aggregates and `user_goals`, exercising migration v17.
- `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug` green per slice; on-device screenshots of each slice (and a clean install once v17 lands).

## Non-goals

- Cloud AI / LLM coaching, accounts, sync, analytics — out of scope (local-first).
- Reworking the Food, Training, or Health screens beyond adding navigation entry points and read-only queries.
- A scrollable multi-day Today; the home is **today only** (date shown, not navigable) for now.

## Risks

- **Coach feeling noisy or repetitive** — mitigate with strict prioritization, showing only the top 1–2 cues, and a pager; tune scores against real data on-device.
- **Migration v17 on the dev device** — same as v16: a clean install wipes local data on the test phone; real upgrade paths are unaffected.
- **Weekly aggregates correctness** — week boundary (Mon-start) and time zones; cover with DAO tests.

## Definition of done (design)

A coach-led Today home is specced: token re-skin, coach briefing (on-device rules over nutrition pacing / training nudges / trends), daily rings (Calories/Protein/Steps) + macro bar, Training & Weight glimpse tiles, a four-metric weekly tracker, the `user_goals` storage (migration v17) + editor, deep-link navigation, and a three-slice roadmap with a pure-unit-tested coach engine.
