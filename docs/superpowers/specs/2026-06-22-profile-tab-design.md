# Profile Tab Design

Date: 2026-06-22

## Objective

Replace the placeholder Health tab with a Profile miniapp: the "me" surface of MusFit where the user manages their personal profile, sets a body goal, tracks weight and body measurements, sees read-only Health Connect vitals, and reaches app/sync settings. Profile owns the user's body inputs and goal intent and computes recommended nutrition targets that Food consumes.

Profile stays Android-only, local-first, and integrated with the existing MusFit bottom navigation, Room database, Hilt ViewModels, Kotlin Flow, and the Health Connect boundary. This design intentionally does not add accounts, cloud sync, analytics, subscriptions, social features, or cloud AI.

## Approved Product Choices

- Tab identity: profile + goals + body (the "me" tab), renamed from Health to Profile.
- Breadth: full tab — profile, body metrics, goals, and app settings all live here.
- Goals model: Profile holds the profile inputs plus goal intent and computes recommended calorie/macro targets; Food consumes them as overridable defaults. No duplicate goal editor.
- Body depth for v1: weight (log, trend, goal progress) and body measurements, plus a read-only Health Connect vitals strip.
- Layout: a body/goal dashboard with a gear icon opening a separate Settings sub-screen.
- Settings for v1: Health Connect sync (rehomed) plus About. Imperial units, theme/dark mode, and backup/export are deferred.

## Naming And Navigation Change

- `AppDestination.Health` becomes `Profile` (route `profile`, label `Profile`, icon `Icons.Outlined.Person`).
- New UI package `ui/profile`; the existing `ui/health` screen and ViewModel are replaced.
- Settings is a sub-route reached from the gear icon (for example `PROFILE_SETTINGS_ROUTE`), matching the existing scanner-route pattern already used in `AppNavGraph.kt`.
- The Health Connect boundary stays under `integrations/healthconnect`; `HealthRepository` remains the Health Connect data source for vitals and the settings sync controls.

## Current Code Context

- `ui/health/HealthScreen.kt` and `HealthViewModel.kt` are currently a Health Connect status/permission screen: availability label, granted-permission count, and four buttons (Enable sync, Refresh status, Import today, Export latest workout). This behavior is preserved but relocated into the Profile Settings sub-screen.
- `HealthRepository` exposes `status()`, `requestablePermissions()`, `observeDailySummary(date)`, `importDailySummary(date)`, and `exportLatestWorkout()`. These are reused as-is for the vitals strip and settings.
- `ImportedDailyHealthSummary` carries `steps`, `activeCaloriesKcal`, `latestWeightKg`, and `restingHeartRateBpm`; `DailyHealthSummaryEntity` persists them per day.
- Food owns nutrition goals through `FoodRepository.observeFoodGoal(): Flow<FoodGoal>` and `updateFoodGoal(goal)`, backed by `FoodGoalEntity` (table `food_goals`, with daily calories, protein/carbs/fat grams, mode, training-calorie inclusion, net-carbs, and water goal). The "Apply to Food goals" action calls `updateFoodGoal`; Food source is not modified.
- Today reads the Health Connect daily summary (steps, active calories, body weight). Today is unchanged in v1.
- `MusFitDatabase` is at version 16 with `exportSchema = true` and no destructive-migration fallback. (The version 14 noted in CLAUDE.md is stale.)
- A generic `body_metrics` table already exists (`BodyMetricEntity`: `id`, `type`, `value`, `unit`, `measuredAtEpochMillis`, `source`, `externalId`) with `HealthDao` methods `upsertBodyMetric`, `observeBodyMetrics(type, fromEpochMillis)`, and `getBodyMetrics(type, fromEpochMillis)`. It is currently unused by any repository and is the natural store for weight and measurements.

The design extends these existing layers rather than replacing the data or Health Connect plumbing.

## User Experience

### Profile dashboard (the tab)

A scrollable stack of cards under a top app bar titled "Profile" with a gear icon that opens Settings. The dashboard reads as a practical "me" surface, not a marketing page.

- Identity card: avatar initial, age (derived from birthdate), height, and activity level, with mini-metrics for weight, BMI, and body fat. Tapping opens the profile editor (sex, birthdate, height, activity level).
- Goal and targets card: goal intent (lose, maintain, or gain) with pace, goal weight with a progress bar, and the computed recommended daily calories plus macros. An "Apply to Food goals" action writes the recommendation into Food.
- Weight card: latest weight, week-over-week delta, a compact trend visual, and a "Log weight" action prefilled from the latest Health Connect weight when available, plus progress toward goal weight.
- Measurements card: waist, chest, arms, thighs, hips, and body fat percent, each showing the latest value and delta, with a "Log measurement" action.
- From Health Connect strip: read-only resting heart rate, steps, and active calories from the daily summary, with a "Manage" link into Settings. When Health Connect is unavailable or unconnected, a connect prompt appears instead.

First-run and empty states: an incomplete profile shows a "Complete your profile" prompt and hides the recommendation until sex, birthdate, height, and a current weight exist; no weight history shows a "Log your first weight" prompt; no measurements shows a similar prompt.

### Profile settings (gear sub-screen)

- Health Connect and sync: availability and permission status, manage permissions (launches the Health Connect permission flow), Import today's data, and Export latest workout — the existing `HealthViewModel` behavior, relocated here.
- Preferences: Units (Metric; imperial later), Theme (System; later), and Backup and export (later), shown as disabled rows tagged "Later" to match the approved mockup.
- About: app name and version.

## Architecture

The existing direction of dependency is preserved:

Compose screen -> `ProfileViewModel` -> `ProfileRepository` / `HealthRepository` / `FoodRepository` -> DAO / Health Connect gateway -> Room / Health Connect.

- `ProfileViewModel` exposes a single immutable `StateFlow<ProfileUiState>` built from a private `MutableStateFlow(...).asStateFlow()`. It observes profile, weight series and latest weight, measurements, settings, and the Health Connect daily summary via flows, and runs mutations in `viewModelScope`.
- `ProfileRepository` (interface plus `LocalProfileRepository`, bound via Hilt `@Binds` in `RepositoryModule`) owns profile and settings through a new `ProfileDao`, and weight and measurements through the existing `body_metrics` table and `HealthDao`.
- `HealthRepository` is reused unchanged for Health Connect status, import, export, and the vitals strip.
- `FoodRepository` is injected only for the single "Apply to Food goals" write.
- Calculations live in pure domain code with no Android or Room dependencies.

If a screen file grows large, split composables by surface for readability (for example `ProfileScreen.kt` for the dashboard and cards, `ProfileSettingsScreen.kt` for settings, `ProfileEditContent.kt` for the profile editor) rather than by speculative architecture.

## Domain Calculations

`EnergyCalculator` (pure):

- BMR via Mifflin–St Jeor from sex, weight (kg), height (cm), and age.
- TDEE = BMR multiplied by an activity factor (sedentary 1.2, light 1.375, moderate 1.55, active 1.725, very active 1.9).
- Target calories from goal intent and pace: maintain equals TDEE; lose or gain shifts TDEE by `paceKgPerWeek * 7700 / 7`, floored at a safe minimum.
- Macro split (deterministic balanced default): protein about 1.8 g per kg bodyweight, fat about 25 percent of calories, carbs the remainder; returns recommended protein, carbs, and fat grams.

`BodyMetricsCalculator` (pure): BMI from weight and height; goal-weight progress fraction from start, current, and goal weight; weight-trend delta over a window.

The recommendation requires sex, birthdate, height, and a current weight (the latest `body_metrics` row of type `weight`); otherwise the targets card shows a complete-profile prompt instead of numbers.

## Apply To Food Goals

The action reads the current `FoodGoal` (first value of `observeFoodGoal()`), copies it with the recommended calories and protein/carbs/fat, preserves the mode, training-calorie inclusion, net-carbs setting, water goal, and any other fields, then calls `updateFoodGoal`. Food's own editor continues to allow overriding the value afterward. No Food source changes are required.

## Data Model

Weight and measurements reuse the existing `body_metrics` table (`BodyMetricEntity`: `id`, `type`, `value`, `unit`, `measuredAtEpochMillis`, `source`, `externalId`) and its existing `HealthDao` methods (`upsertBodyMetric`, `observeBodyMetrics(type, fromEpochMillis)`, `getBodyMetrics(type, fromEpochMillis)`). Weight is `type = "weight"` (unit `kg`); measurements are a `type` of `waist`, `chest`, `arms`, `thighs`, `hips`, or `body_fat` (unit `cm` or `%`); `source` is `manual` or `health_connect`. No new time-series tables are introduced.

Two new singleton entities are required:

- `UserProfileEntity` (singleton row, fixed id): `sex`, `birthDateEpochDay`, `heightCm`, `activityLevel`, `goalType`, `goalPaceKgPerWeek`, `goalWeightKg`, `updatedAtEpochMillis`.
- `AppSettingsEntity` (singleton row, fixed id): `unitSystem` (metric default), `themeMode` (system default), `updatedAtEpochMillis`. The unit and theme values are stored but not yet applied app-wide in v1.

Weights are stored in kilograms and lengths in centimeters regardless of any future unit preference.

The schema change is mandatory and follows the repository rule: add `MIGRATION_16_17` creating the `user_profile` and `app_settings` tables in `DatabaseModule.kt`, bump `MusFitDatabase` version from 16 to 17, register the migration in `addMigrations(...)`, let the build regenerate the schema JSON and commit `app/schemas/.../17.json`, and add DAO round-trip tests for the new tables. There is no destructive fallback.

A new `ProfileDao` exposes Flows for the two singletons (observe and upsert profile; observe and upsert settings). `ProfileRepository` injects both `ProfileDao` and the existing `HealthDao`, reading and writing weight and measurements through `body_metrics`.

## State And Navigation

`ProfileUiState` holds: the profile (or an incomplete flag), recommended targets (or an unavailable flag), goal and progress, latest weight with trend and delta, measurement latest values and deltas, Health Connect vitals (steps, active calories, resting heart rate) with availability, the settings snapshot, and loading/error state.

Profile editing and the settings screen use a sub-route from the Profile destination. The global bottom-nav set stays at the four tabs; no new top-level destinations are introduced.

## Error Handling And Empty States

- Health Connect unavailable or not connected: the vitals strip and settings show a graceful status (reusing existing messaging) and never crash.
- Incomplete profile: hide the recommendation and prompt the user to complete it.
- No weight or measurement history: show concise prompts to log the first entry.
- Input validation: reject non-positive weight, height, or measurement values and implausible ages, keeping edit state intact on failure.
- Apply to Food goals: surface a success or failure message; do not write a partial goal.

## Today Compatibility

Today continues to read the Health Connect daily summary and is unchanged in v1. A later slice could surface goal progress or the latest manual weight in Today, but that is out of scope here.

## Testing Strategy

TDD is expected for behavior changes.

- Domain (pure JUnit): `EnergyCalculator` BMR, TDEE, target calories, and macro split for known inputs across each goal intent and activity level; `BodyMetricsCalculator` BMI, progress, and trend.
- Repository (`RobolectricTestRunner` with in-memory Room): profile upsert and read; weight add, list, and latest; measurement add, list, and latest-by-type; settings upsert and read; and DAO round-trip for the new `user_profile` and `app_settings` tables (migration 16 to 17).
- ViewModel (JUnit with hand-written fakes including a `FakeProfileRepository`, reusing the existing Food and Health fakes; `StandardTestDispatcher` with `Dispatchers.setMain`/`resetMain`): empty and incomplete state; editing the profile recomputes the recommendation; Apply writes a computed `FoodGoal` that preserves the other goal fields; logging weight updates trend and progress; vitals mapping; and the relocated Health Connect status, import, and export behavior.

Full verification before claiming completion or pushing:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain
```

## Implementation Slices

### Slice 1: Rename and relocate

- Rename the destination, package, label, and icon from Health to Profile.
- Build the Profile dashboard shell with empty states.
- Move Health Connect status, import, and export into `ProfileSettingsScreen` behind the gear route, keeping current Health Connect behavior working.
- No database change in this slice. Add tests for the navigation and state shell.

### Slice 2: Profile and targets

- Add `UserProfileEntity` and `AppSettingsEntity` via migration 16 to 17 (a single schema bump); weight and measurements reuse the existing `body_metrics` table, so no new time-series tables are created.
- Add `ProfileDao`, `ProfileRepository` (injecting the new `ProfileDao` and the existing `HealthDao`), and `EnergyCalculator`.
- Add the profile editor (which captures the current weight as an initial weight entry so the recommendation has a value), the recommended-targets card, and the Apply to Food goals action.
- Add domain, repository, and ViewModel tests.

### Slice 3: Weight tracking

- Add weight logging to `body_metrics` (type `weight`, prefilled from the latest Health Connect weight), trend, and goal progress, backed by `BodyMetricsCalculator`.
- Add tests for weight logging, trend, and progress.

### Slice 4: Measurements

- Add measurement logging to `body_metrics` (types `waist`, `chest`, `arms`, `thighs`, `hips`, `body_fat`) and latest-value-plus-delta display.
- Add tests for measurement persistence and display.

### Slice 5: Vitals and polish

- Wire the read-only Health Connect vitals strip from the daily summary.
- Add About, refine empty and error states, and add accessibility labels.

## Acceptance Criteria

Profile v1 is complete when:

- The fourth tab reads "Profile" with a person icon and opens the body/goal dashboard.
- A user can complete their profile (sex, birthdate, height, activity level) and goal (intent, pace, goal weight) and see recommended calories and macros.
- "Apply to Food goals" updates Food's goal with the recommendation while preserving Food's other goal fields, and Food can still override it afterward.
- A user can log weight (prefilled from the latest Health Connect weight when available) and see trend and goal progress.
- A user can log body measurements and see the latest value and delta.
- The Health Connect vitals strip shows resting heart rate, steps, and active calories when granted, and degrades gracefully otherwise.
- Health Connect status, import, and export work from Settings exactly as before.
- The 16 to 17 migration ships with its schema JSON (`17.json`) and DAO round-trip tests, and existing installs upgrade without data loss.
- `testDebugUnitTest`, `lintDebug`, and `assembleDebug` pass.

## Non-Goals For V1

- Imperial units and app-wide unit conversion.
- Theme or dark-mode toggle.
- Data backup, export, and import.
- Writing weight back to Health Connect.
- Progress photos.
- Blood pressure, detailed sleep, or additional vitals.
- Editing nutrition goals inside Profile; Food remains the goal editor.
- Accounts, cloud sync, analytics, subscriptions, social features, and cloud AI.
