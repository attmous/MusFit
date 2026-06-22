# Training Slice 3 Plan — Progress chart + routine-editor polish

> **For agentic workers:** Use superpowers:executing-plans. Steps use checkbox (`- [ ]`). TDD: write the failing test, run red, implement, run green. Source `.\.superpowers\sdd\android-env.ps1` before any gradle. Pure logic first, Compose visuals last (screenshot-verified, per repo convention).

**Goal:** Give the Training **Progress** tab a real Compose-`Canvas` line chart with a 3-way metric toggle + tap-to-inspect, and **polish the routine editor** (Indigo sweep, reorder affordances, inline target validation).

**Approved decisions:** 3-way toggle **Volume / Est-1RM / Heaviest** (default **Est-1RM**); **tap-to-inspect** (no drag scrub); **both** chart + routine polish in this slice; Delete stays error-red; compact "1.2k" volume axis.

**Architecture:** No Room/schema change. The chart is a hand-rolled `Canvas` (no chart lib); all geometry is a pure, unit-tested `TrendChartScaler`. The only domain edit is adding a computed `heaviestWeightKg` to `TrainingTrendPoint`. Routine polish is UI-only; new pure validators live beside the existing tested helpers (which stay byte-for-byte unchanged).

---

## Part A — Progress chart

### Task A1: `heaviestWeightKg` on the trend (domain, TDD)
**Files:** `domain/model/TrainingModels.kt`, `domain/training/WorkoutCalculator.kt`, test `domain/training/WorkoutCalculatorTest.kt`.
- [ ] **A1 (red):** In `WorkoutCalculatorTest`, assert each `trend` point's `heaviestWeightKg == max single-set weightKg for that date` (mirror the existing per-day volume / 1RM assertions). Run red.
- [ ] **A2 (green):** Add `val heaviestWeightKg: Double` to `TrainingTrendPoint`; in `WorkoutCalculator.exerciseProgress`, populate it per day as `daySets.maxOf { it.weightKg }`. Run green. (Trend is computed — no entity/migration.)

### Task A2: pure chart geometry (`TrendChartScaler`, TDD)
**Files:** new `domain/training/TrendChartScaler.kt`, test `domain/training/TrendChartScalerTest.kt`.
- [ ] **A3 (red):** Tests: two-point x-mapping (`x[0]==padL`, `x[last]==width-padR`); y-inversion (higher value → smaller y; max→padT, min→padT+plotH); single point centered (`x==width/2`, finite y, no NaN); flat series (min==max) → all y at vertical center, no divide-by-zero; empty → empty points; `niceTicks` ascending + spanning + degenerate; `nearestIndex` left→0 / right→lastIndex / midway deterministic / clamps. Run red.
- [ ] **A4 (green):** Implement (no Android imports):
  ```kotlin
  data class ChartPoint(val x: Float, val y: Float)
  data class ChartGeometry(val points: List<ChartPoint>, val minValue: Double, val maxValue: Double, val yTicks: List<Double>)
  fun computeChartGeometry(values: List<Double>, widthPx: Float, heightPx: Float, paddingLeftPx: Float, paddingTopPx: Float, paddingBottomPx: Float, tickCount: Int = 3): ChartGeometry
  fun nearestIndex(touchX: Float, pointXs: List<Float>): Int
  // private niceTicks(min, max, count)
  ```
  X(i)=padL + i*(plotW/max(1,n-1)); single→width/2. Y(v)=padT + plotH*(1-(v-min)/(max-min)); max==min→padT+plotH/2. Run green.

### Task A3: metric mapping (TDD)
**Files:** new `ui/training/ExerciseTrendChart.kt`, test `ui/training/ExerciseTrendChartLogicTest.kt` (or colocate in TrendChartScalerTest).
- [ ] **A5 (red):** `valueFor` mapping: `Volume→volumeKg`, `EstOneRepMax→bestEstimatedOneRepMaxKg`, `Heaviest→heaviestWeightKg`. Run red.
- [ ] **A6 (green):** `enum class TrendMetric { Volume, EstOneRepMax, Heaviest }` + `internal fun TrainingTrendPoint.valueFor(metric): Double`. Run green.

### Task A4: chart Compose (screenshot-verified)
**Files:** `ui/training/ExerciseTrendChart.kt`.
- [ ] **A7:** `TrendMetricToggle(selected, accent, onSelect)` — FilterChip row using `FilterChipDefaults.filterChipColors(selectedContainerColor = accent.container, selectedLabelColor = accent.onContainer)`. `ExerciseTrendChart(trend, metric, accent, modifier)` — `Canvas` (gridlines, flat area fill alpha 0.10, straight-segment line 2.5dp round cap in `accent.color`, data dots, last-point callout pill) + an overlay `Box(Canvas + Text)` for y-tick (compact "1.2k" for Volume, else `formatKg()`) and first/last date labels (`DateTimeFormatter "d MMM"`), + `pointerInput { detectTapGestures }` → `nearestIndex` → `selectedIndex` (vertical guide + tooltip). Empty/single/flat wired from geometry.

### Task A5: wire into Progress tab
**Files:** `ui/training/TrainingProgressContent.kt`.
- [ ] **A8:** Replace the text-trend `forEach` (lines ~67-73) with: `if (progress.trend.isEmpty()) Text("No history yet", …) else { var metric by rememberSaveable { mutableStateOf(TrendMetric.EstOneRepMax) }; TrendMetricToggle(metric, accent) { metric = it }; ExerciseTrendChart(progress.trend, metric, accent, Modifier.fillMaxWidth().height(168.dp)) }`. No public signature change (call site in `TrainingScreen.kt` untouched).

## Part B — Routine-editor polish

### Task B1: pure validators (TDD)
**Files:** `ui/training/TrainingRoutineContent.kt`, test `ui/training/TrainingHomeContentTest.kt`.
- [ ] **B1 (red):** `validateTargetSets`: `"3"/"1"/"20"→Valid`; `"0"/"21"/""/"abc"/"-1"→Invalid`. Run red.
- [ ] **B2 (green):** `sealed interface TargetFieldResult { object Valid; data class Invalid(val message) }` + `internal fun validateTargetSets(raw): TargetFieldResult` (trimmed digits, 1..20 else `"Enter 1-20 sets"`). Run green.
- [ ] **B3 (red):** `validateTargetReps`: `""/"8"/"8-12"/"8 - 12"→Valid`; `"12-8"/"0"/"101"/"8-"/"x"→Invalid`. Run red.
- [ ] **B4 (green):** `internal fun validateTargetReps(raw)` (blank ok; int 1..100; or `lo-hi` with lo≤hi, both 1..100; trim-tolerant; else `"Use a number or range, e.g. 8 or 8-12"`). Run green.
- [ ] **B5 (red):** `routineEditorCanSave`: blank name→false; valid name + empty exercises→false; valid name + a row with sets `"0"`→false; valid name + all-valid→true. Run red.
- [ ] **B6 (green):** `internal fun routineEditorCanSave(name, exercises): Boolean`. Run green. Confirm `routineCardActions_*` / `routineExercisePickerSuggestions_*` stay green (helpers untouched).

### Task B2: editor polish Compose (screenshot-verified)
**Files:** `ui/training/TrainingRoutineContent.kt`, `ui/training/TrainingScreen.kt`.
- [ ] **B7:** Thread `accent: TabAccent` into `TrainingRoutineEditor` → `RoutineExercisePicker(+ selectedCount)` → `RoutineEditorExerciseCard(+ position, + setsError, + repsError)`. Update `TrainingScreen.kt` editor call to pass `accent = accent` (already in scope).
- [ ] **B8:** Indigo sweep — Save/Cancel/Duplicate/Edit + picker "Add exercise"/suggestions → `accent`; **Delete → `MaterialTheme.colorScheme.error`**; `OutlinedTextField` focus colors → `accent`. Reorder: leading 3.dp Indigo bar (`drawBehind`), numbered index chip (`accent.container`/`onContainer`), move-arrow enabled/disabled tints. Wire `isError` + `supportingText` from `setsError`/`repsError`; `Save enabled = routineEditorCanSave(...)`. UX bundle: keep picker open on non-blank-query add, "(N added)" header, empty-routine hint.

## Task C: verify + device + commit
- [ ] `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain` → green. (OneDrive AccessDenied → `--stop`, delete `app/build`, rerun.)
- [ ] On the dev phone: Progress → pick an exercise → line chart renders; toggle Volume/Est-1RM/Heaviest; tap a session → tooltip. Routine editor → Indigo buttons + index chips + leading bar; type a bad set/rep → error helper text + Save disabled.
- [ ] Commit (`feat(training): progress trend chart + routine-editor polish`).

## Notes
- No DB/migration, no ViewModel/repository changes — chart metric/scrub + validation display are local UI state.
- `heaviestWeightKg` is the only domain edit (computed trend field; fully unit-tested).
