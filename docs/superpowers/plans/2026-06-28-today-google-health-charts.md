# Today Google-Health Chart Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restyle the Today dashboard's data visualisations to feel like Google Health's charts — soft rounded weekly bars with a highlighted day and value bubble, a smooth area-filled weight trend, and rounded-cap rings — via a reusable Compose `Canvas` chart kit.

**Architecture:** Surface the per-day series `WeeklyGoalsCalculator.compute()` already receives (5 defaulted fields on `WeeklyGoals`, plus a one-line widening of the weight-fetch window). Add a pure `BarChartScaler` and a stateless chart kit under `ui/components/charts/` (`MetricRing`, `WeekBarChart`, `TrendLineChart`, `ChartDefaults`); the trend line reuses the existing `domain/training/TrendChartScaler` by import only. `TodayViewModel` maps the series to a `WeeklyChartsUiState` via a pure `buildWeeklyCharts()`; `TodayScreen` swaps in the kit and the new cards, retiring the 2×2 "This week" grid.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), hand-rolled `Canvas` charts (no new dependency), JUnit + `StandardTestDispatcher`. No Room change, no migration.

**Spec:** [`docs/superpowers/specs/2026-06-28-today-google-health-charts-design.md`](../specs/2026-06-28-today-google-health-charts-design.md)

---

## Process constraints (read first)

- **Work in an isolated git worktree created from committed `master`, located OUTSIDE OneDrive.** The use-git-worktrees skill handles this. The main working tree may hold another session's uncommitted Training WIP — do not touch it.
- **Do not edit any file under `ui/training/**` or `domain/training/**`.** `TrendChartScaler` is reused by import only.
- **Source the toolchain once per shell** before any Gradle command:
  ```powershell
  . .\.superpowers\sdd\android-env.ps1
  ```
- **OneDrive/Gradle recovery** if a build fails with `AccessDenied` / `Cannot snapshot` / `not a regular file`:
  ```powershell
  .\gradlew.bat --stop
  Start-Sleep -Seconds 3
  Remove-Item -LiteralPath (Resolve-Path 'app\build').Path -Recurse -Force
  ```
  then rerun. This is environmental, not a code defect.
- Each task ends with a commit. Colour rule: the new charts use the Today accent (Coral) via `tabAccentFor(AppDestination.Today)`; the rings keep their per-metric colours (Calories = Emerald/brand, Protein = teal, Steps = blue) and change shape only.

## File structure

| File | Responsibility |
|------|----------------|
| `domain/today/WeeklyGoalsCalculator.kt` (modify) | Surface per-day calorie/step series + weight points through `WeeklyGoals` |
| `domain/charts/BarChartScaler.kt` (create) | Pure bar normalisation (fractions vs max+headroom, target line) |
| `ui/components/charts/ChartDefaults.kt` (create) | Shared chart visual tokens |
| `ui/components/charts/MetricRing.kt` (create) | Rounded-cap progress ring with a centre content slot |
| `ui/components/charts/WeekBarChart.kt` (create) | Weekly rounded bars, selection highlight, value bubble, target line |
| `ui/components/charts/TrendLineChart.kt` (create) | Smoothed area-filled trend line (reuses `TrendChartScaler`) |
| `ui/today/TodayViewModel.kt` (modify) | `WeeklyChartsUiState`/`DayBar`, pure `buildWeeklyCharts()`, selection state, widen weight window |
| `ui/today/TodayScreen.kt` (modify) | Swap in `MetricRing`; new `WeeklyCaloriesCard`/`WeightTrendCard`/`WeekStatsRow`; retire 2×2 grid + Weight glimpse |
| `test/.../domain/today/WeeklyGoalsCalculatorTest.kt` (modify) | Series pass-through test |
| `test/.../domain/charts/BarChartScalerTest.kt` (create) | Bar normalisation tests |
| `test/.../ui/today/BuildWeeklyChartsTest.kt` (create) | Pure mapping tests |
| `test/.../ui/today/TodayViewModelTest.kt` (modify) | `weeklyCharts` wiring + selection tests |

---

### Task 1: Surface per-day series through `WeeklyGoals`

**Files:**
- Modify: `app/src/main/java/com/musfit/domain/today/WeeklyGoalsCalculator.kt`
- Test: `app/src/test/java/com/musfit/domain/today/WeeklyGoalsCalculatorTest.kt`

- [ ] **Step 1: Write the failing test** — append this method inside `WeeklyGoalsCalculatorTest` (before the final `}`):

```kotlin
    @Test
    fun surfacesPerDaySeriesForCharts() {
        val result = WeeklyGoalsCalculator.compute(
            weekStartMillis = weekStart,
            sessionStartMillis = emptyList(),
            sessionTarget = 4,
            loggedCaloriesPerDay = listOf(2000.0, 1900.0, null, 2100.0, 2050.0, 1800.0, 2200.0),
            calorieGoalKcal = 2000.0,
            stepsPerDay = listOf(12_000L, 8_000L, 10_000L, 9_000L, 11_000L, 7_000L, 10_500L),
            stepGoal = 10_000L,
            weights = listOf(weekStart - 2 * day to 81.0, weekStart + day to 80.0),
            targetWeightKg = 78.0,
        )

        assertEquals(listOf(2000.0, 1900.0, null, 2100.0, 2050.0, 1800.0, 2200.0), result.caloriesPerDay)
        assertEquals(2000.0, result.calorieGoalKcal, 0.001)
        assertEquals(listOf(12_000L, 8_000L, 10_000L, 9_000L, 11_000L, 7_000L, 10_500L), result.stepsPerDay)
        assertEquals(10_000L, result.stepGoal)
        assertEquals(
            listOf(WeightPoint(weekStart - 2 * day, 81.0), WeightPoint(weekStart + day, 80.0)),
            result.weightPoints,
        )
    }
```

- [ ] **Step 2: Run the test to verify it fails**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.musfit.domain.today.WeeklyGoalsCalculatorTest" --no-daemon --console=plain
```
Expected: FAIL — `WeightPoint` unresolved and `caloriesPerDay`/`calorieGoalKcal`/`stepsPerDay`/`stepGoal`/`weightPoints` are not members of `WeeklyGoals`.

- [ ] **Step 3: Add `WeightPoint` + the five fields.** In `WeeklyGoalsCalculator.kt`, replace the `WeeklyGoals` data class (lines 4-13) with:

```kotlin
/** Pure weekly-goal rollup for the Today home. All inputs are primitives so the domain stays Android-free. */
data class WeeklyGoals(
    val sessionsDone: Int,
    val sessionTarget: Int,
    val calorieOnTargetDays: Int,
    val trackedDays: Int,
    val stepGoalDays: Int,
    val weightAvgKg: Double?,
    val weightDeltaKg: Double?,
    val targetWeightKg: Double?,
    // Per-day series for the Today charts. Defaults keep existing callsites/tests compiling unchanged.
    val caloriesPerDay: List<Double?> = emptyList(), // 7 entries Mon..Sun, null = untracked
    val calorieGoalKcal: Double = 0.0,               // for the bar target line
    val stepsPerDay: List<Long> = emptyList(),       // 7 entries Mon..Sun
    val stepGoal: Long = 0L,
    val weightPoints: List<WeightPoint> = emptyList(), // chronological, for the trend line
)

/** A single weight measurement, surfaced for the trend chart. */
data class WeightPoint(val epochMillis: Long, val valueKg: Double)
```

- [ ] **Step 4: Populate the new fields in `compute()`.** Replace the `return WeeklyGoals(...)` block (lines 49-58) with:

```kotlin
        return WeeklyGoals(
            sessionsDone = sessionsDone,
            sessionTarget = sessionTarget,
            calorieOnTargetDays = calorieOnTargetDays,
            trackedDays = TRACKED_DAYS,
            stepGoalDays = stepGoalDays,
            weightAvgKg = thisWeekAvg,
            weightDeltaKg = weightDelta,
            targetWeightKg = targetWeightKg.takeIf { it > 0.0 },
            caloriesPerDay = loggedCaloriesPerDay,
            calorieGoalKcal = calorieGoalKcal,
            stepsPerDay = stepsPerDay,
            stepGoal = stepGoal,
            weightPoints = weights.map { WeightPoint(it.first, it.second) },
        )
```

- [ ] **Step 5: Run the test to verify it passes**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.musfit.domain.today.WeeklyGoalsCalculatorTest" --no-daemon --console=plain
```
Expected: PASS (all three methods, including `surfacesPerDaySeriesForCharts`).

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/musfit/domain/today/WeeklyGoalsCalculator.kt app/src/test/java/com/musfit/domain/today/WeeklyGoalsCalculatorTest.kt
git commit -m "feat(today): surface per-day series through WeeklyGoals for charts"
```

---

### Task 2: `BarChartScaler` (pure bar normalisation)

**Files:**
- Create: `app/src/main/java/com/musfit/domain/charts/BarChartScaler.kt`
- Test: `app/src/test/java/com/musfit/domain/charts/BarChartScalerTest.kt`

- [ ] **Step 1: Write the failing test** — create `app/src/test/java/com/musfit/domain/charts/BarChartScalerTest.kt`:

```kotlin
package com.musfit.domain.charts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BarChartScalerTest {
    @Test
    fun normalisesValuesAgainstMaxWithoutHeadroom() {
        val g = BarChartScaler.compute(values = listOf(1000.0, 2000.0), target = null, headroom = 1.0)
        assertEquals(0.5, g.bars[0]!!, 0.001)
        assertEquals(1.0, g.bars[1]!!, 0.001)
        assertEquals(2000.0, g.maxValue, 0.001)
    }

    @Test
    fun headroomKeepsTallestBarBelowTheTop() {
        val g = BarChartScaler.compute(values = listOf(2000.0), target = null, headroom = 1.15)
        assertTrue(g.bars[0]!! < 1.0)
        assertEquals(2300.0, g.maxValue, 0.001)
    }

    @Test
    fun nullValuesPassThroughAsUntracked() {
        val g = BarChartScaler.compute(values = listOf(1000.0, null, 2000.0), target = null, headroom = 1.0)
        assertNull(g.bars[1])
        assertEquals(0.5, g.bars[0]!!, 0.001)
    }

    @Test
    fun targetCountsTowardsMaxAndGetsItsOwnFraction() {
        val g = BarChartScaler.compute(values = listOf(1000.0), target = 2000.0, headroom = 1.0)
        assertEquals(0.5, g.bars[0]!!, 0.001)
        assertEquals(1.0, g.targetFraction!!, 0.001)
    }

    @Test
    fun allNullSeriesProducesNullsAndZeroMax() {
        val g = BarChartScaler.compute(values = listOf(null, null), target = null)
        assertEquals(listOf<Double?>(null, null), g.bars)
        assertNull(g.targetFraction)
        assertEquals(0.0, g.maxValue, 0.001)
    }

    @Test
    fun allZeroSeriesProducesZeroFractionsNotNaN() {
        val g = BarChartScaler.compute(values = listOf(0.0, 0.0), target = null)
        assertTrue(g.bars.all { it == 0.0 })
        assertEquals(0.0, g.maxValue, 0.001)
    }

    @Test
    fun emptySeriesIsHandled() {
        val g = BarChartScaler.compute(values = emptyList(), target = null)
        assertTrue(g.bars.isEmpty())
        assertNull(g.targetFraction)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.musfit.domain.charts.BarChartScalerTest" --no-daemon --console=plain
```
Expected: FAIL — `BarChartScaler` / `BarChartGeometry` unresolved.

- [ ] **Step 3: Implement** — create `app/src/main/java/com/musfit/domain/charts/BarChartScaler.kt`:

```kotlin
package com.musfit.domain.charts

/** Normalised geometry for a small bar chart. Pure — no Android/Compose types. */
data class BarChartGeometry(
    val bars: List<Double?>,     // fraction in [0,1] per bar; null passes through (untracked)
    val targetFraction: Double?, // fraction in [0,1] for the target line, or null
    val maxValue: Double,        // the scaled max (with headroom) used for normalisation
)

/**
 * Maps bar values (nullable = untracked) and an optional target to fractions in [0,1] against a
 * shared max with headroom, so the tallest bar and the target line never touch the top edge.
 * Handles empty, all-null, and all-zero series without dividing by zero.
 */
object BarChartScaler {
    fun compute(values: List<Double?>, target: Double?, headroom: Double = 1.15): BarChartGeometry {
        val dataMax = values.filterNotNull().maxOrNull() ?: 0.0
        val rawMax = maxOf(dataMax, target ?: 0.0)
        if (rawMax <= 0.0) {
            return BarChartGeometry(
                bars = values.map { if (it == null) null else 0.0 },
                targetFraction = null,
                maxValue = 0.0,
            )
        }
        val maxValue = rawMax * headroom
        val bars = values.map { v -> v?.let { (it / maxValue).coerceIn(0.0, 1.0) } }
        val targetFraction = target?.takeIf { it > 0.0 }?.let { (it / maxValue).coerceIn(0.0, 1.0) }
        return BarChartGeometry(bars = bars, targetFraction = targetFraction, maxValue = maxValue)
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.musfit.domain.charts.BarChartScalerTest" --no-daemon --console=plain
```
Expected: PASS (7 methods).

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/musfit/domain/charts/BarChartScaler.kt app/src/test/java/com/musfit/domain/charts/BarChartScalerTest.kt
git commit -m "feat(charts): add pure BarChartScaler"
```

---

### Task 3: `ChartDefaults` + `MetricRing` (kit foundations)

**Files:**
- Create: `app/src/main/java/com/musfit/ui/components/charts/ChartDefaults.kt`
- Create: `app/src/main/java/com/musfit/ui/components/charts/MetricRing.kt`

No unit test (Canvas composables — verified by compile + on-device screenshots in Task 8).

- [ ] **Step 1: Create `ChartDefaults.kt`:**

```kotlin
package com.musfit.ui.components.charts

import androidx.compose.ui.unit.dp

/** Shared visual tokens for the hand-rolled chart kit — single source for the Google-Health look. */
object ChartDefaults {
    val ringStroke = 8.dp
    val barWidth = 22.dp
    val barCorner = 6.dp
    val barContextAlpha = 0.24f
    val areaAlpha = 0.10f
    val lineStroke = 2.5.dp
    val dotRadius = 5.dp
    val targetDash = floatArrayOf(3f, 6f)
}
```

- [ ] **Step 2: Create `MetricRing.kt`:**

```kotlin
package com.musfit.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.musfit.ui.theme.MusFitTheme

/**
 * A single progress ring: a soft full-circle track plus a rounded-cap progress arc from the top.
 * Restyle of the Today GoalRing with a content slot so callers place their own centre label.
 */
@Composable
fun MetricRing(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    diameter: Dp = 72.dp,
    strokeWidth: Dp = ChartDefaults.ringStroke,
    trackColor: Color = MusFitTheme.colors.track,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = progress.coerceIn(0f, 1f) * 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        content()
    }
}
```

- [ ] **Step 3: Verify it compiles**

```powershell
.\gradlew.bat assembleDebug --no-daemon --console=plain
```
Expected: `BUILD SUCCESSFUL`. (The composables are unused so far — that is fine; public functions are not flagged.)

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/java/com/musfit/ui/components/charts/ChartDefaults.kt app/src/main/java/com/musfit/ui/components/charts/MetricRing.kt
git commit -m "feat(charts): add ChartDefaults tokens and MetricRing"
```

---

### Task 4: `WeekBarChart`

**Files:**
- Create: `app/src/main/java/com/musfit/ui/components/charts/WeekBarChart.kt`

No unit test (the math is covered by `BarChartScalerTest`; drawing is verified in Task 8).

- [ ] **Step 1: Create `WeekBarChart.kt`:**

```kotlin
package com.musfit.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.musfit.domain.charts.BarChartScaler
import com.musfit.ui.theme.MusFitTheme
import kotlin.math.roundToInt

/** One bar: a day label and an optional value (null = untracked). */
data class BarDatum(val value: Double?, val label: String)

/**
 * A weekly bar chart in the Google-Health style: rounded-top bars, the selected bar solid in the
 * accent colour with the others faded, an optional dashed target line, a value bubble over the
 * selected bar, and day labels. Tapping a bar invokes [onBarSelected]. Stateless — the caller owns
 * the selection.
 */
@Composable
fun WeekBarChart(
    bars: List<BarDatum>,
    accent: Color,
    onAccent: Color,
    modifier: Modifier = Modifier,
    target: Double? = null,
    selectedIndex: Int? = null,
    valueFormatter: (Double) -> String = { it.roundToInt().toString() },
    onBarSelected: (Int) -> Unit = {},
) {
    val contextColor = accent.copy(alpha = ChartDefaults.barContextAlpha)
    val targetColor = MusFitTheme.colors.onSurfaceVariant.copy(alpha = 0.5f)
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MusFitTheme.colors.onSurfaceVariant)
    val selectedLabelStyle = MaterialTheme.typography.labelSmall.copy(color = accent, fontWeight = FontWeight.Bold)
    val bubbleStyle = MaterialTheme.typography.labelMedium.copy(color = onAccent, fontWeight = FontWeight.Bold)

    val geometry = remember(bars, target) { BarChartScaler.compute(bars.map { it.value }, target) }

    Canvas(
        modifier = modifier.pointerInput(bars) {
            detectTapGestures { offset ->
                if (bars.isEmpty()) return@detectTapGestures
                val slot = size.width / bars.size
                onBarSelected((offset.x / slot).toInt().coerceIn(0, bars.size - 1))
            }
        },
    ) {
        if (bars.isEmpty()) return@Canvas
        val labelGap = 18.dp.toPx()
        val bubbleSpace = 26.dp.toPx()
        val baseline = size.height - labelGap
        val plotH = (baseline - bubbleSpace).coerceAtLeast(0f)
        val slot = size.width / bars.size
        val barW = ChartDefaults.barWidth.toPx().coerceAtMost(slot * 0.6f)

        // Target line (under the bars).
        geometry.targetFraction?.let { tf ->
            val y = baseline - plotH * tf.toFloat()
            drawLine(
                color = targetColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(ChartDefaults.targetDash),
            )
        }

        // Bars + day labels.
        bars.forEachIndexed { index, datum ->
            val fraction = geometry.bars.getOrNull(index)
            val cx = slot * index + slot / 2f
            val left = cx - barW / 2f
            val right = cx + barW / 2f
            val selected = index == selectedIndex
            if (fraction == null) {
                drawLine(
                    color = contextColor,
                    start = Offset(left, baseline),
                    end = Offset(right, baseline),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round,
                )
            } else {
                val top = baseline - plotH * fraction.toFloat()
                val r = ChartDefaults.barCorner.toPx().coerceAtMost(barW / 2f).coerceAtMost(baseline - top).coerceAtLeast(0f)
                val barPath = Path().apply {
                    moveTo(left, baseline)
                    lineTo(left, top + r)
                    quadraticBezierTo(left, top, left + r, top)
                    lineTo(right - r, top)
                    quadraticBezierTo(right, top, right, top + r)
                    lineTo(right, baseline)
                    close()
                }
                drawPath(barPath, color = if (selected) accent else contextColor)
            }
            val measured = textMeasurer.measure(datum.label, if (selected) selectedLabelStyle else labelStyle)
            drawText(
                measured,
                topLeft = Offset(cx - measured.size.width / 2f, baseline + (labelGap - measured.size.height) / 2f),
            )
        }

        // Value bubble over the selected, tracked bar.
        val sel = selectedIndex ?: return@Canvas
        val selFraction = geometry.bars.getOrNull(sel) ?: return@Canvas
        val selValue = bars[sel].value ?: return@Canvas
        val cx = slot * sel + slot / 2f
        val top = baseline - plotH * selFraction.toFloat()
        val bubble = textMeasurer.measure(valueFormatter(selValue), bubbleStyle)
        val padX = 8.dp.toPx()
        val padY = 4.dp.toPx()
        val bw = bubble.size.width + padX * 2
        val bh = bubble.size.height + padY * 2
        val bx = (cx - bw / 2f).coerceIn(0f, (size.width - bw).coerceAtLeast(0f))
        val by = (top - bh - 6.dp.toPx()).coerceAtLeast(0f)
        drawRoundRect(
            color = accent,
            topLeft = Offset(bx, by),
            size = Size(bw, bh),
            cornerRadius = CornerRadius(bh / 2f, bh / 2f),
        )
        drawText(bubble, topLeft = Offset(bx + padX, by + padY))
    }
}
```

> Note: if your Compose version flags `quadraticBezierTo` as deprecated, replace both calls with `quadraticTo(...)` (identical arguments).

- [ ] **Step 2: Verify it compiles**

```powershell
.\gradlew.bat assembleDebug --no-daemon --console=plain
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```powershell
git add app/src/main/java/com/musfit/ui/components/charts/WeekBarChart.kt
git commit -m "feat(charts): add WeekBarChart with selection and value bubble"
```

---

### Task 5: `TrendLineChart`

**Files:**
- Create: `app/src/main/java/com/musfit/ui/components/charts/TrendLineChart.kt`

Reuses `com.musfit.domain.training.TrendChartScaler` by import (no Training edit). No unit test.

- [ ] **Step 1: Create `TrendLineChart.kt`:**

```kotlin
package com.musfit.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.musfit.domain.training.ChartPoint
import com.musfit.domain.training.TrendChartScaler
import com.musfit.ui.theme.MusFitTheme

/**
 * A smooth area-filled trend line in the Google-Health style: a Catmull-Rom-smoothed curve, a soft
 * area fill to the baseline, and an end-dot ringed in the surface colour. Reuses TrendChartScaler
 * for value→pixel geometry. Degrades to a single dot / nothing for 1 / 0 points.
 */
@Composable
fun TrendLineChart(
    values: List<Double>,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val surface = MusFitTheme.colors.surface
    val areaColor = accent.copy(alpha = ChartDefaults.areaAlpha)
    val pad = 8f

    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas
        val geometry = TrendChartScaler.computeChartGeometry(values, size.width, size.height, pad, pad, pad, pad)
        val points = geometry.points
        val baseline = size.height - pad

        if (points.size == 1) {
            drawCircle(color = accent, radius = ChartDefaults.dotRadius.toPx(), center = Offset(points[0].x, points[0].y))
            return@Canvas
        }

        val linePath = smoothPath(points)
        val areaPath = Path().apply {
            addPath(linePath)
            lineTo(points.last().x, baseline)
            lineTo(points.first().x, baseline)
            close()
        }
        drawPath(areaPath, areaColor)
        drawPath(
            linePath,
            color = accent,
            style = Stroke(width = ChartDefaults.lineStroke.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        val last = points.last()
        drawCircle(color = surface, radius = ChartDefaults.dotRadius.toPx() + 2f, center = Offset(last.x, last.y))
        drawCircle(color = accent, radius = ChartDefaults.dotRadius.toPx(), center = Offset(last.x, last.y))
    }
}

/** A Catmull-Rom spline through [points] converted to cubic Béziers, for a smooth line. */
private fun smoothPath(points: List<ChartPoint>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points.first().x, points.first().y)
    for (i in 0 until points.size - 1) {
        val p0 = points[if (i == 0) i else i - 1]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = points[if (i + 2 < points.size) i + 2 else i + 1]
        val c1x = p1.x + (p2.x - p0.x) / 6f
        val c1y = p1.y + (p2.y - p0.y) / 6f
        val c2x = p2.x - (p3.x - p1.x) / 6f
        val c2y = p2.y - (p3.y - p1.y) / 6f
        path.cubicTo(c1x, c1y, c2x, c2y, p2.x, p2.y)
    }
    return path
}
```

- [ ] **Step 2: Verify it compiles**

```powershell
.\gradlew.bat assembleDebug --no-daemon --console=plain
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```powershell
git add app/src/main/java/com/musfit/ui/components/charts/TrendLineChart.kt
git commit -m "feat(charts): add smoothed TrendLineChart"
```

---

### Task 6: ViewModel mapping + selection state

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/today/TodayViewModel.kt`
- Test: `app/src/test/java/com/musfit/ui/today/BuildWeeklyChartsTest.kt` (create)
- Test: `app/src/test/java/com/musfit/ui/today/TodayViewModelTest.kt` (modify)

- [ ] **Step 1: Write the failing pure-mapping test** — create `app/src/test/java/com/musfit/ui/today/BuildWeeklyChartsTest.kt`:

```kotlin
package com.musfit.ui.today

import com.musfit.domain.today.WeeklyGoals
import com.musfit.domain.today.WeightPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BuildWeeklyChartsTest {
    private fun weekly(
        caloriesPerDay: List<Double?> = List(7) { 2000.0 },
        calorieGoalKcal: Double = 2000.0,
        weightPoints: List<WeightPoint> = emptyList(),
    ) = WeeklyGoals(
        sessionsDone = 3,
        sessionTarget = 5,
        calorieOnTargetDays = 4,
        trackedDays = 7,
        stepGoalDays = 2,
        weightAvgKg = 80.5,
        weightDeltaKg = -0.6,
        targetWeightKg = 78.0,
        caloriesPerDay = caloriesPerDay,
        calorieGoalKcal = calorieGoalKcal,
        stepsPerDay = List(7) { 9000L },
        stepGoal = 10000L,
        weightPoints = weightPoints,
    )

    @Test
    fun mapsSevenLabelledBarsInMondayFirstOrder() {
        val state = buildWeeklyCharts(weekly(), todayIndex = 2)
        assertEquals(7, state.calorieBars.size)
        assertEquals(listOf("M", "T", "W", "T", "F", "S", "S"), state.calorieBars.map { it.label })
        assertEquals(2000.0, state.calorieBars[2].calories!!, 0.001)
    }

    @Test
    fun defaultsSelectionToTodayWhenTracked() {
        assertEquals(4, buildWeeklyCharts(weekly(), todayIndex = 4).defaultSelectedIndex)
    }

    @Test
    fun defaultsSelectionToLastTrackedDayWhenTodayUntracked() {
        val cals = listOf(2000.0, 2100.0, 1900.0, null, null, null, null)
        assertEquals(2, buildWeeklyCharts(weekly(caloriesPerDay = cals), todayIndex = 5).defaultSelectedIndex)
    }

    @Test
    fun defaultSelectionIsNullWhenNothingTracked() {
        assertNull(buildWeeklyCharts(weekly(caloriesPerDay = List(7) { null }), todayIndex = 1).defaultSelectedIndex)
    }

    @Test
    fun surfacesTargetTrendAndStats() {
        val points = listOf(WeightPoint(1L, 81.0), WeightPoint(2L, 80.2))
        val state = buildWeeklyCharts(weekly(weightPoints = points), todayIndex = 0)
        assertEquals(2000.0, state.calorieTarget!!, 0.001)
        assertEquals(listOf(81.0, 80.2), state.weightTrend)
        assertEquals(80.2, state.latestWeightKg!!, 0.001)
        assertEquals(-0.6, state.weightDeltaKg!!, 0.001)
        assertEquals(4, state.onTargetDays)
        assertEquals(3, state.sessionsDone)
        assertEquals(5, state.sessionTarget)
        assertEquals(2, state.stepGoalDays)
    }

    @Test
    fun noGoalYieldsNullTarget() {
        assertNull(buildWeeklyCharts(weekly(calorieGoalKcal = 0.0), todayIndex = 0).calorieTarget)
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.today.BuildWeeklyChartsTest" --no-daemon --console=plain
```
Expected: FAIL — `buildWeeklyCharts` / `WeeklyChartsUiState` / `DayBar` unresolved.

- [ ] **Step 3: Add the UI-state types + pure mapping.** In `TodayViewModel.kt`, add these data classes immediately after `MacroBreakdownUiState` (after line 52):

```kotlin
data class DayBar(val label: String, val calories: Double?)

data class WeeklyChartsUiState(
    val calorieBars: List<DayBar>,
    val calorieTarget: Double?,
    val onTargetDays: Int,
    val trackedDays: Int,
    val defaultSelectedIndex: Int?,
    val weightTrend: List<Double>,
    val weightDeltaKg: Double?,
    val latestWeightKg: Double?,
    val sessionsDone: Int,
    val sessionTarget: Int,
    val stepGoalDays: Int,
)
```

- [ ] **Step 4: Extend `TodayUiState`.** In the `TodayUiState` data class (lines 60-72), add two fields after `weekly` (after line 66):

```kotlin
    val weeklyCharts: WeeklyChartsUiState? = null,
    val selectedCalorieDayIndex: Int? = null,
```

- [ ] **Step 5: Add the pure mapping + day labels.** Add at file scope near the other private helpers (e.g. just above `private fun buildDaily(` at line 302):

```kotlin
internal val DAY_LABELS = listOf("M", "T", "W", "T", "F", "S", "S")

/** Pure mapping from the weekly rollup to chart UI state. [todayIndex] is Monday=0..Sunday=6. */
internal fun buildWeeklyCharts(weekly: WeeklyGoals, todayIndex: Int): WeeklyChartsUiState {
    val bars = DAY_LABELS.mapIndexed { i, label -> DayBar(label = label, calories = weekly.caloriesPerDay.getOrNull(i)) }
    val defaultSelected = when {
        bars.getOrNull(todayIndex)?.calories != null -> todayIndex
        else -> bars.indexOfLast { it.calories != null }.takeIf { it >= 0 }
    }
    return WeeklyChartsUiState(
        calorieBars = bars,
        calorieTarget = weekly.calorieGoalKcal.takeIf { it > 0.0 },
        onTargetDays = weekly.calorieOnTargetDays,
        trackedDays = weekly.trackedDays,
        defaultSelectedIndex = defaultSelected,
        weightTrend = weekly.weightPoints.map { it.valueKg },
        weightDeltaKg = weekly.weightDeltaKg,
        latestWeightKg = weekly.weightPoints.lastOrNull()?.valueKg ?: weekly.weightAvgKg,
        sessionsDone = weekly.sessionsDone,
        sessionTarget = weekly.sessionTarget,
        stepGoalDays = weekly.stepGoalDays,
    )
}
```

- [ ] **Step 6: Run the pure-mapping test to verify it passes**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.today.BuildWeeklyChartsTest" --no-daemon --console=plain
```
Expected: PASS (6 methods).

- [ ] **Step 7: Write the failing ViewModel wiring test** — append to `TodayViewModelTest` (before the first `private class` declaration, i.e. after line 119):

```kotlin
    @Test
    fun state_buildsWeeklyChartsWithSevenBarsAndTracksSelection() = runTest {
        val date = LocalDate.now()
        val viewModel = TodayViewModel(
            foodRepository = FakeFoodRepository(),
            trainingRepository = FakeTrainingRepository(),
            healthRepository = FakeHealthRepository(date),
            goalsRepository = FakeGoalsRepository(),
            dateProvider = { date },
        )
        dispatcher.scheduler.advanceUntilIdle()

        val charts = viewModel.state.value.weeklyCharts
        assertEquals(7, charts!!.calorieBars.size)
        assertEquals(listOf("M", "T", "W", "T", "F", "S", "S"), charts.calorieBars.map { it.label })

        assertEquals(null, viewModel.state.value.selectedCalorieDayIndex)
        viewModel.onCalorieDaySelected(3)
        assertEquals(3, viewModel.state.value.selectedCalorieDayIndex)
    }
```

- [ ] **Step 8: Run it to verify it fails**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.today.TodayViewModelTest" --no-daemon --console=plain
```
Expected: FAIL — `weeklyCharts` is always null (not yet wired) and `onCalorieDaySelected` is unresolved.

- [ ] **Step 9: Wire the mapping + selection into the ViewModel.** Three edits in `TodayViewModel.kt`:

(a) Widen the weight window — in `observeWeekly()`, change line 139 from:
```kotlin
        val weightFromMillis = (weekStart.toEpochDay() - 7L) * DAY_MILLIS
```
to:
```kotlin
        val weightFromMillis = (weekStart.toEpochDay() - 30L) * DAY_MILLIS
```

(b) Populate `weeklyCharts` — in `observeWeekly()`, replace the `.collect { weekly -> ... }` block (lines 165-167):
```kotlin
            }.collect { weekly ->
                mutableState.update { it.copy(weekly = weekly) }
            }
```
with:
```kotlin
            }.collect { weekly ->
                val todayIndex = date.dayOfWeek.value - 1
                mutableState.update { it.copy(weekly = weekly, weeklyCharts = buildWeeklyCharts(weekly, todayIndex)) }
            }
```

(c) Add the selection setter — add this method to the `TodayViewModel` class, just after `closeGoalsEditor()` (after line 261):
```kotlin
    fun onCalorieDaySelected(index: Int) {
        mutableState.update { it.copy(selectedCalorieDayIndex = index) }
    }
```

- [ ] **Step 10: Run both Today tests to verify they pass**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.today.*" --no-daemon --console=plain
```
Expected: PASS (`BuildWeeklyChartsTest` + `TodayViewModelTest`, including the new method).

- [ ] **Step 11: Commit**

```powershell
git add app/src/main/java/com/musfit/ui/today/TodayViewModel.kt app/src/test/java/com/musfit/ui/today/BuildWeeklyChartsTest.kt app/src/test/java/com/musfit/ui/today/TodayViewModelTest.kt
git commit -m "feat(today): map weekly series to WeeklyChartsUiState with selection"
```

---

### Task 7: Wire the kit into `TodayScreen`

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/today/TodayScreen.kt`

No unit test (UI). Verified by compile here + on-device screenshots in Task 8.

- [ ] **Step 1: Fix imports.** In `TodayScreen.kt`, **remove** these imports (all become unused after this task):
```
androidx.compose.foundation.Canvas
androidx.compose.foundation.layout.Box
androidx.compose.foundation.layout.size
androidx.compose.material.icons.outlined.MonitorWeight
androidx.compose.ui.geometry.Offset
androidx.compose.ui.geometry.Size
androidx.compose.ui.graphics.StrokeCap
androidx.compose.ui.graphics.drawscope.Stroke
com.musfit.domain.today.WeeklyGoals
```
and **add** these imports:
```kotlin
import com.musfit.ui.AppDestination
import com.musfit.ui.components.charts.BarDatum
import com.musfit.ui.components.charts.MetricRing
import com.musfit.ui.components.charts.TrendLineChart
import com.musfit.ui.components.charts.WeekBarChart
import com.musfit.ui.theme.TabAccent
import com.musfit.ui.theme.tabAccentFor
```

- [ ] **Step 2: Restyle the rings.** In `DailyRingsCard`, replace the `GoalRing(...)` call (line 171):
```kotlin
                        GoalRing(progress = ring.progress, color = ringColor(ring.kind), centerLabel = ring.centerLabel)
```
with:
```kotlin
                        MetricRing(progress = ring.progress, color = ringColor(ring.kind)) {
                            Text(
                                text = ring.centerLabel,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MusFitTheme.colors.onSurface,
                            )
                        }
```

- [ ] **Step 3: Delete the now-unused `GoalRing` composable** (lines 193-231 — the entire `@Composable private fun GoalRing(...) { ... }`).

- [ ] **Step 4: Replace the screen body from the rings card down to the weekly section.** Replace this block (lines 114-136):

```kotlin
        if (state.rings.isNotEmpty()) {
            DailyRingsCard(rings = state.rings, macros = state.macros, onClick = onOpenFood)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlimpseTile(
                icon = Icons.Outlined.FitnessCenter,
                value = state.training.title,
                label = state.training.subtitle,
                onClick = onOpenTraining,
            )
            GlimpseTile(
                icon = Icons.Outlined.MonitorWeight,
                value = state.weightKg?.let { "${it.formatMetric()} kg" } ?: "—",
                label = "Body weight",
                onClick = onOpenHealth,
            )
        }

        state.weekly?.let { WeeklyGoalsCard(it) }
```

with:

```kotlin
        val accent = tabAccentFor(AppDestination.Today)

        if (state.rings.isNotEmpty()) {
            DailyRingsCard(rings = state.rings, macros = state.macros, onClick = onOpenFood)
        }

        state.weeklyCharts?.let { charts ->
            WeeklyCaloriesCard(
                charts = charts,
                accent = accent,
                selectedIndex = state.selectedCalorieDayIndex ?: charts.defaultSelectedIndex,
                onDaySelected = viewModel::onCalorieDaySelected,
            )
            WeightTrendCard(charts = charts, accent = accent)
            WeekStatsRow(charts = charts)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlimpseTile(
                icon = Icons.Outlined.FitnessCenter,
                value = state.training.title,
                label = state.training.subtitle,
                onClick = onOpenTraining,
            )
        }
```

> `onOpenHealth` is still used by `CoachBriefingCard` (the `CoachAction.OpenHealth` branch), so the parameter stays. `state.weightKg` is no longer read here — that is intentional (latest weight now lives in `WeightTrendCard`).

- [ ] **Step 5: Replace `WeeklyGoalsCard` with the three new cards.** Delete the entire `WeeklyGoalsCard` composable (lines 289-324) and insert these three composables in its place:

```kotlin
@Composable
private fun WeeklyCaloriesCard(
    charts: WeeklyChartsUiState,
    accent: TabAccent,
    selectedIndex: Int?,
    onDaySelected: (Int) -> Unit,
) {
    Surface(color = MusFitTheme.colors.surface, shape = MusFitTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "Calories",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MusFitTheme.colors.onSurface,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "this week",
                        style = MaterialTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                }
                Surface(color = accent.container, shape = MusFitTheme.shapes.small) {
                    Text(
                        text = "${charts.onTargetDays}/${charts.trackedDays} on target",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = accent.onContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            WeekBarChart(
                bars = charts.calorieBars.map { BarDatum(value = it.calories, label = it.label) },
                accent = accent.color,
                onAccent = accent.onColor,
                target = charts.calorieTarget,
                selectedIndex = selectedIndex,
                onBarSelected = onDaySelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
            )
        }
    }
}

@Composable
private fun WeightTrendCard(charts: WeeklyChartsUiState, accent: TabAccent) {
    Surface(color = MusFitTheme.colors.surface, shape = MusFitTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(
                        text = "Weight · 30 days",
                        style = MaterialTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                    Text(
                        text = charts.latestWeightKg?.let { "${it.formatMetric()} kg" } ?: "—",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MusFitTheme.colors.onSurface,
                    )
                }
                charts.weightDeltaKg?.let { d ->
                    val arrow = if (d < -0.05) "↓" else if (d > 0.05) "↑" else "→"
                    Surface(color = MusFitTheme.colors.positiveContainer, shape = MusFitTheme.shapes.small) {
                        Text(
                            text = "$arrow ${abs(d).formatMetric()} kg",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MusFitTheme.colors.positive,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            if (charts.weightTrend.isEmpty()) {
                Text(
                    text = "Log your weight to see the trend.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            } else {
                TrendLineChart(
                    values = charts.weightTrend,
                    accent = accent.color,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp),
                )
            }
        }
    }
}

@Composable
private fun WeekStatsRow(charts: WeeklyChartsUiState) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        WeeklyMiniTracker(Modifier.weight(1f), "Sessions", "${charts.sessionsDone} / ${charts.sessionTarget}")
        WeeklyMiniTracker(Modifier.weight(1f), "Step-goal days", "${charts.stepGoalDays} / ${charts.trackedDays}")
    }
}
```

> `WeeklyMiniTracker` (lines 326-348), `MacroBar`, `GlimpseTile`, `ringColor`, `RingKind.label`, and `formatMetric` are all retained and reused. `positive` resolves to `MusFitTheme.colors.positive`.

- [ ] **Step 6: Verify it compiles**

```powershell
.\gradlew.bat assembleDebug --no-daemon --console=plain
```
Expected: `BUILD SUCCESSFUL`. If the compiler reports an unused-import or unresolved-reference error, fix per the message (e.g. an import you removed is still referenced, or vice-versa).

- [ ] **Step 7: Commit**

```powershell
git add app/src/main/java/com/musfit/ui/today/TodayScreen.kt
git commit -m "feat(today): lead the dashboard with Google-Health-style charts"
```

---

### Task 8: Full verification + on-device screenshot

**Files:** none (verification only).

- [ ] **Step 1: Run the full gate**

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain
```
Expected: `BUILD SUCCESSFUL`; all unit tests pass; lint clean. (If you hit the OneDrive `AccessDenied`/`Cannot snapshot` flake, run the recovery block from "Process constraints" and rerun — it is environmental.)

- [ ] **Step 2: Install and launch on the connected device**

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell monkey -p com.musfit -c android.intent.category.LAUNCHER 1
```

- [ ] **Step 3: Capture the Today screen and eyeball it**

```powershell
adb exec-out screencap -p > "$env:TEMP\today-charts.png"
```
Open the PNG and confirm: rounded-cap rings; the weekly calorie bar card shows 7 rounded bars with one solid (today/last-tracked) and a value bubble, a dashed target line, and the "N/7 on target" pill; the weight trend card shows a smooth area line with an end-dot and a delta pill (or the empty-state text if no weight data); and the Sessions / Step-goal-days stats row. Tap a bar and confirm the highlight + bubble move.

> Use `adb exec-out screencap -p > file` (not `adb shell screencap` piped through PowerShell `>`, which corrupts PNGs). Tap coordinates use the device's 1344×2992 px space.

- [ ] **Step 4: No code changes expected.** If the screenshot reveals a layout defect, fix it, rerun Step 1, and commit with a `fix(today):` message. Otherwise this task adds no commit.

---

## Notes for the executor

- The chart kit is built reusable but only **adopted by Today** here. Food's `CalorieRing` and Training's `ExerciseTrendChart` keep their own implementations this slice (a deliberate follow-up).
- Steps per day are surfaced on `WeeklyGoals`/`WeeklyChartsUiState` but not yet drawn — that's intentional headroom for a future steps view; do not add a steps chart now (YAGNI).
- If `lintDebug` flags any leftover unused import in `TodayScreen.kt`, remove it — the Task 7 import list targets the known set, but the compiler/lint is the source of truth.
