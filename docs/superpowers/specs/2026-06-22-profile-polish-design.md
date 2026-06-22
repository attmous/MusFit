# Profile Polish Design

Date: 2026-06-22

## Objective

Finish the Profile dashboard that shipped in v1 (DB v18) by drawing the trend/delta visuals the v1 UI stubbed, and by letting the user fix a mis-logged weight or measurement. No new schema, no new dependencies, metric-only. Builds on the existing `ui/profile` screen, `ProfileViewModel`, `ProfileRepository`, and the reused `body_metrics` table.

This is incremental polish, not a new subsystem. It intentionally does not add a full history screen, a charting library, unit/theme/backup settings, or Health Connect writeback.

## Approved Product Choices

- Complete the dashboard visuals toward the approved v1 mockup: weight trend sparkline, measurement up/down deltas, consistent body-fat.
- Edit/delete uses a **recent-entries bottom sheet**: tapping the Weight card (or a measurement row) opens a sheet listing the metric's last several entries, each editable or deletable. Edit changes the value only; date is unchanged.
- No `body_metrics` schema change, so the database stays at version 18.

## Current Code Context

- `ui/profile/ProfileScreen.kt` renders the dashboard cards from `ProfileUiState`. The Weight card shows only the latest value; the Measurements card draws each `MeasurementRow` value but discards its already-computed `deltaFromPrevious`. There is no sparkline and no entry management.
- `ProfileViewModel` exposes `ProfileUiState` (latest weight, `bmi`, `weightTrend: List<Double>`, `measurements: List<MeasurementRow>`, `vitals`, etc.) and actions `saveProfile`, `logWeight`, `logMeasurement`, `applyTargetsToFood`, `dismissMessage`.
- `ProfileRepository` exposes `logWeight`, `observeLatestWeight`, `observeWeightSeries(since)`, `logMeasurement`, `observeRecentMeasurements(since)`, plus profile/settings. Weight and measurements are rows in `body_metrics`.
- `body_metrics` (`BodyMetricEntity`: `id`, `type`, `value`, `unit`, `measuredAtEpochMillis`, `source`, `externalId`) has a stable `id`. `HealthDao` has `upsertBodyMetric` and `observeBodyMetrics(type, fromEpochMillis)` but no delete/update.
- `WeightEntry(measuredAtEpochMillis, weightKg, source)` and `BodyMeasurement(type, value, unit, measuredAtEpochMillis)` currently omit the row `id`.
- `BodyMetricsCalculator` holds pure helpers (`bodyMassIndex`, `goalProgressFraction`).

## User Experience

### Weight card
- Shows the latest weight, a **week delta** ("▼ 0.4 kg this week" — latest minus the entry nearest seven days ago), colored by direction, and a compact **sparkline** of the last ~12 weight values (Compose `Canvas`, no axes).
- Tapping the card opens the **weight entries sheet**.

### Measurements card
- Each measurement row shows its value and unit plus the **▲/▼ delta from the previous entry** (already computed; just unrendered today). "—" when there is no value.
- Tapping a row opens the **entries sheet for that measurement type**.

### Recent-entries sheet (`ModalBottomSheet`)
- Lists the metric's last ~10 entries, newest first, each as date + value.
- Each entry has **edit** (a small numeric dialog seeded with the current value; saving updates the value, keeps the date) and **delete** (with a confirm). An empty metric shows a short "No entries yet" line.
- After edit/delete, the dashboard refreshes through the existing `StateFlow` (latest value, sparkline, deltas, BMI, goal progress all recompute).

### Empty/error states
- Unchanged from v1: incomplete profile hides the recommendation; no weight shows "No weight logged yet"; the vitals strip degrades gracefully.
- Invalid edit values (non-positive) are rejected, keeping the dialog open.

## Architecture And Data Model

No schema change; `body_metrics` is reused as-is and the database stays at version 18.

- `WeightEntry` and `BodyMeasurement` gain an `id: String` (the `body_metrics` row id), mapped through in `ProfileRepository`.
- `HealthDao` gains `deleteBodyMetric(id: String)` and `updateBodyMetricValue(id: String, value: Double)` (`@Query` UPDATE/DELETE by primary key).
- `ProfileRepository` gains `deleteEntry(id: String)` and `updateEntryValue(id: String, value: Double)` (one pair covers both weight and measurements since both are `body_metrics`). Validation rejects non-positive values.
- `BodyMetricsCalculator` gains a pure `changeOverWindow(points: List<Pair<Long, Double>>, windowMillis: Long, nowMillis: Long): Double?` (each pair is `epochMillis to value`) used for the weekly weight delta; the ViewModel maps weight entries to pairs, keeping the calculator free of data-layer types and testable.
- `ProfileViewModel`/`ProfileUiState` gain: `weeklyWeightDeltaKg: Double?`, recent weight entries (with id/date/value) for the sheet, and the per-type measurement series (with ids) for the sheet; plus actions `editEntry(id, value)` and `deleteEntry(id)`. The sparkline reuses the existing `weightTrend`.
- UI splits: keep dashboard cards in `ProfileScreen.kt`; add the sparkline and the entries sheet as small private composables (a new `ProfileEntriesSheet.kt` if `ProfileScreen.kt` grows unwieldy).

Direction of dependency is unchanged: Compose → `ProfileViewModel` → `ProfileRepository` → `HealthDao` → Room.

## Testing Strategy

TDD for behavior.

- Domain (pure JUnit): `BodyMetricsCalculator.changeOverWindow` for normal, single-entry, and out-of-window cases.
- Repository (`RobolectricTestRunner`, in-memory Room): `updateEntryValue` changes only the targeted row's value; `deleteEntry` removes it; `observeWeightSeries`/`observeRecentMeasurements` expose the row `id`.
- ViewModel (JUnit + fakes): `editEntry`/`deleteEntry` call the repository with the right id; state exposes recent entries and `weeklyWeightDeltaKg`.
- UI: no automated tests; verify on device (sparkline draws, deltas show, sheet edit/delete updates the dashboard).

Full verification before completion: `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain`.

## Implementation Slices

1. Data layer: add `id` to `WeightEntry`/`BodyMeasurement`; add `deleteBodyMetric`/`updateBodyMetricValue` to `HealthDao`; add `deleteEntry`/`updateEntryValue` to `ProfileRepository`. Repository tests.
2. Domain: add `changeOverWindow` to `BodyMetricsCalculator`. Pure tests.
3. ViewModel: add `weeklyWeightDeltaKg`, recent-entry exposure, and `editEntry`/`deleteEntry`. ViewModel tests.
4. UI: weight sparkline, week delta, measurement deltas, and the recent-entries `ModalBottomSheet` with edit/delete. Device verification.

## Acceptance Criteria

- The Weight card shows a sparkline and a colored week delta; tapping it opens a sheet of recent weights.
- Each measurement row shows its ▲/▼ delta; tapping a row opens a sheet of that type's recent entries.
- A user can edit or delete any listed entry; the dashboard (latest value, sparkline, deltas, BMI, goal progress) updates immediately.
- No schema/migration change; database remains version 18.
- `testDebugUnitTest`, `lintDebug`, and `assembleDebug` pass.

## Non-Goals

- Full weight/measurement history screen or charting library.
- Editing an entry's date; bulk operations.
- Units (imperial), theme/dark-mode, backup/export.
- Writing weight back to Health Connect; additional vitals.
