# Profile Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish the Profile dashboard — draw the weight sparkline + week delta and the measurement up/down deltas the v1 UI stubbed, and add a recent-entries bottom sheet to edit/delete a mis-logged weight or measurement.

**Architecture:** Extend the existing `ui/profile` screen, `ProfileViewModel`, `ProfileRepository`, and `BodyMetricsCalculator`. Weight and measurements stay rows in the existing `body_metrics` table, so there is **no schema change** (DB stays version 18). Edit/delete operate by the existing `body_metrics` row `id`, which is threaded onto the `WeightEntry`/`BodyMeasurement` domain types.

**Tech Stack:** Android Kotlin, Jetpack Compose Material 3, Hilt, Room, Kotlin Flow/coroutines, JUnit, Robolectric, kotlinx-coroutines-test.

## Global Constraints

- Android-only, local-first, metric-only. No schema/migration change; the database stays at version 18.
- No new Gradle dependencies (sparkline is Compose `Canvas`).
- Before Gradle/adb on Windows, run `. .\.superpowers\sdd\android-env.ps1` first (in a worktree, source the main repo's copy: `. 'C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1'`, in the same command as Gradle).
- Full verification: `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain`.

## File Structure

- Modify `app/src/main/java/com/musfit/data/local/dao/HealthDao.kt` — add `deleteBodyMetric` + `updateBodyMetricValue`.
- Modify `app/src/main/java/com/musfit/data/repository/ProfileRepository.kt` — add `id` to `WeightEntry`/`BodyMeasurement`; thread it through mappings; add `deleteEntry`/`updateEntryValue`.
- Modify `app/src/main/java/com/musfit/domain/profile/BodyMetricsCalculator.kt` — add pure `changeOverWindow`.
- Modify `app/src/main/java/com/musfit/ui/profile/ProfileViewModel.kt` — add `weeklyWeightDeltaKg`, `weightEntries`, `measurementEntries` to state; `editEntry`/`deleteEntry` actions.
- Modify `app/src/main/java/com/musfit/ui/profile/ProfileScreen.kt` — sparkline, week delta, measurement deltas, sheet wiring.
- Modify `app/src/main/java/com/musfit/ui/profile/ProfileEditContent.kt` — add `EntriesSheet` (+ `EntrySheetItem`, date formatter), reusing the file's existing `NumberField`/`toPositiveDoubleOrNull`.
- Test: `app/src/test/java/com/musfit/data/repository/LocalProfileRepositoryTest.kt`, `app/src/test/java/com/musfit/domain/profile/BodyMetricsCalculatorTest.kt`, `app/src/test/java/com/musfit/ui/profile/ProfileViewModelTest.kt`.

---

### Task 1: Data layer — entry id, delete, and update-value

**Files:**
- Modify: `app/src/main/java/com/musfit/data/local/dao/HealthDao.kt`
- Modify: `app/src/main/java/com/musfit/data/repository/ProfileRepository.kt`
- Test: `app/src/test/java/com/musfit/data/repository/LocalProfileRepositoryTest.kt`

- [ ] **Step 1: Write failing repository tests**

Add to `LocalProfileRepositoryTest.kt` (after the existing `logMeasurement_storesUnderTypeWithUnit` test):

```kotlin
@Test
fun observeWeightSeries_exposesRowId() = runTest {
    repository.logWeight(85.0)
    val entry = repository.observeWeightSeries(0L).first().first()
    assertTrue(entry.id.isNotBlank())
}

@Test
fun updateEntryValue_changesOnlyThatRow() = runTest {
    repository.logWeight(85.0)
    repository.logWeight(84.0)
    val entries = repository.observeWeightSeries(0L).first()
    val target = entries.first { it.weightKg == 85.0 }

    repository.updateEntryValue(target.id, 86.5)

    val updated = repository.observeWeightSeries(0L).first()
    assertEquals(86.5, updated.first { it.id == target.id }.weightKg, 0.001)
    assertEquals(84.0, updated.first { it.weightKg != 86.5 }.weightKg, 0.001)
}

@Test
fun deleteEntry_removesRow() = runTest {
    repository.logMeasurement("waist", 84.0, "cm")
    val entry = repository.observeRecentMeasurements(0L).first()["waist"]!!.first()

    repository.deleteEntry(entry.id)

    assertTrue(repository.observeRecentMeasurements(0L).first()["waist"]!!.isEmpty())
}

@Test
fun updateEntryValue_rejectsNonPositive() = runTest {
    repository.logWeight(85.0)
    val id = repository.observeWeightSeries(0L).first().first().id
    var threw = false
    try {
        repository.updateEntryValue(id, 0.0)
    } catch (e: IllegalArgumentException) {
        threw = true
    }
    assertTrue(threw)
}
```

Add the import `import org.junit.Assert.assertTrue` to the test file (the others — `assertEquals`, `assertNotNull`, `assertNull`, `first`, `runTest` — are already imported).

- [ ] **Step 2: Run them to verify they fail**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.musfit.data.repository.LocalProfileRepositoryTest" --no-daemon --console=plain`
Expected: FAIL — unresolved `entry.id` / `repository.updateEntryValue` / `repository.deleteEntry`.

- [ ] **Step 3: Add DAO delete/update queries**

In `HealthDao.kt`, add inside the `@Dao interface HealthDao { ... }` body (e.g. after `upsertBodyMetric`):

```kotlin
@Query("DELETE FROM body_metrics WHERE id = :id")
suspend fun deleteBodyMetric(id: String)

@Query("UPDATE body_metrics SET value = :value WHERE id = :id")
suspend fun updateBodyMetricValue(id: String, value: Double)
```

- [ ] **Step 4: Thread `id` through the domain types and add repository methods**

In `ProfileRepository.kt`:

1. Add `id` as the first field of both data classes:

```kotlin
data class WeightEntry(val id: String, val measuredAtEpochMillis: Long, val weightKg: Double, val source: String)

data class BodyMeasurement(val id: String, val type: String, val value: Double, val unit: String, val measuredAtEpochMillis: Long)
```

2. Update the three mappings to pass `it.id`:

```kotlin
override fun observeLatestWeight(): Flow<WeightEntry?> =
    healthDao.observeBodyMetrics(WEIGHT_METRIC_TYPE, 0L).map { rows ->
        rows.firstOrNull()?.let { WeightEntry(it.id, it.measuredAtEpochMillis, it.value, it.source) }
    }

override fun observeWeightSeries(sinceEpochMillis: Long): Flow<List<WeightEntry>> =
    healthDao.observeBodyMetrics(WEIGHT_METRIC_TYPE, sinceEpochMillis).map { rows ->
        rows.map { WeightEntry(it.id, it.measuredAtEpochMillis, it.value, it.source) }
    }
```

and in `observeRecentMeasurements`, change the row mapping to:

```kotlin
type to rows.map { BodyMeasurement(it.id, it.type, it.value, it.unit, it.measuredAtEpochMillis) }
```

3. Add to the `ProfileRepository` interface (after `logMeasurement`):

```kotlin
suspend fun deleteEntry(id: String)
suspend fun updateEntryValue(id: String, value: Double)
```

4. Add to `LocalProfileRepository` (after `logMeasurement`'s implementation):

```kotlin
override suspend fun deleteEntry(id: String) {
    healthDao.deleteBodyMetric(id)
}

override suspend fun updateEntryValue(id: String, value: Double) {
    require(value.isFinite() && value > 0.0) { "Value must be positive" }
    healthDao.updateBodyMetricValue(id, value)
}
```

- [ ] **Step 5: Run the repository tests to verify they pass**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.musfit.data.repository.LocalProfileRepositoryTest" --no-daemon --console=plain`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: add body-metric entry id, delete, and update-value to ProfileRepository"
```

---

### Task 2: Domain — week-over-window change

**Files:**
- Modify: `app/src/main/java/com/musfit/domain/profile/BodyMetricsCalculator.kt`
- Test: `app/src/test/java/com/musfit/domain/profile/BodyMetricsCalculatorTest.kt`

- [ ] **Step 1: Write failing tests**

Add to `BodyMetricsCalculatorTest.kt`:

```kotlin
@Test
fun changeOverWindow_usesBaselineAtOrBeforeCutoff() {
    val day = 86_400_000L
    val points = listOf(0L to 86.0, 7L * day to 85.0, 14L * day to 84.0)
    val delta = BodyMetricsCalculator.changeOverWindow(points, windowMillis = 7L * day, nowMillis = 14L * day)
    assertEquals(-1.0, delta!!, 0.001)
}

@Test
fun changeOverWindow_fallsBackToEarliestWhenAllWithinWindow() {
    val day = 86_400_000L
    val points = listOf(10L * day to 85.0, 14L * day to 84.0)
    val delta = BodyMetricsCalculator.changeOverWindow(points, windowMillis = 7L * day, nowMillis = 14L * day)
    assertEquals(-1.0, delta!!, 0.001)
}

@Test
fun changeOverWindow_nullForFewerThanTwoPoints() {
    assertNull(BodyMetricsCalculator.changeOverWindow(listOf(5L to 80.0), windowMillis = 10L, nowMillis = 20L))
    assertNull(BodyMetricsCalculator.changeOverWindow(emptyList(), windowMillis = 10L, nowMillis = 20L))
}
```

(`assertEquals`, `assertNull` are already imported in this test file.)

- [ ] **Step 2: Run to verify they fail**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.musfit.domain.profile.BodyMetricsCalculatorTest" --no-daemon --console=plain`
Expected: FAIL — unresolved `changeOverWindow`.

- [ ] **Step 3: Implement `changeOverWindow`**

Add to `BodyMetricsCalculator`:

```kotlin
fun changeOverWindow(points: List<Pair<Long, Double>>, windowMillis: Long, nowMillis: Long): Double? {
    if (points.size < 2) return null
    val sorted = points.sortedBy { it.first }
    val latest = sorted.last()
    val cutoff = nowMillis - windowMillis
    val baseline = sorted.lastOrNull { it.first <= cutoff } ?: sorted.first()
    if (baseline.first == latest.first) return null
    return latest.second - baseline.second
}
```

- [ ] **Step 4: Run to verify they pass, then commit**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.musfit.domain.profile.BodyMetricsCalculatorTest" --no-daemon --console=plain`
Expected: PASS.

```bash
git add -A
git commit -m "feat: add BodyMetricsCalculator.changeOverWindow for weekly delta"
```

---

### Task 3: ViewModel — recent entries, weekly delta, edit/delete

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/profile/ProfileViewModel.kt`
- Test: `app/src/test/java/com/musfit/ui/profile/ProfileViewModelTest.kt`

- [ ] **Step 1: Write failing ViewModel tests**

In `ProfileViewModelTest.kt`:

1. Update the two `WeightEntry(1_000L, 80.0, "manual")` constructions (in `completeProfile_exposesTargetsAndBmi` and `applyTargetsToFood_writesGoalPreservingOtherFields`) to include an id: `WeightEntry("w1", 1_000L, 80.0, "manual")`.

2. In the private `FakeProfileRepository`, add capture fields and the two new overrides:

```kotlin
var updatedId: String? = null
var updatedValue: Double? = null
var deletedId: String? = null
override suspend fun deleteEntry(id: String) { deletedId = id }
override suspend fun updateEntryValue(id: String, value: Double) { updatedId = id; updatedValue = value }
```

3. Add tests:

```kotlin
@Test
fun editEntry_callsRepositoryWithIdAndValue() = runTest {
    val repo = FakeProfileRepository()
    val viewModel = ProfileViewModel(repo, FakeHealthRepo(), FakeFoodGoalRepo())
    dispatcher.scheduler.advanceUntilIdle()

    viewModel.editEntry("abc", 81.3)
    dispatcher.scheduler.advanceUntilIdle()

    assertEquals("abc", repo.updatedId)
    assertEquals(81.3, repo.updatedValue!!, 0.001)
}

@Test
fun deleteEntry_callsRepositoryWithId() = runTest {
    val repo = FakeProfileRepository()
    val viewModel = ProfileViewModel(repo, FakeHealthRepo(), FakeFoodGoalRepo())
    dispatcher.scheduler.advanceUntilIdle()

    viewModel.deleteEntry("xyz")
    dispatcher.scheduler.advanceUntilIdle()

    assertEquals("xyz", repo.deletedId)
}

@Test
fun state_exposesWeightEntriesForSheet() = runTest {
    val repo = FakeProfileRepository(latestWeight = WeightEntry("w9", 1_000L, 84.0, "manual"))
    val viewModel = ProfileViewModel(repo, FakeHealthRepo(), FakeFoodGoalRepo())
    dispatcher.scheduler.advanceUntilIdle()

    assertEquals("w9", viewModel.state.value.weightEntries.first().id)
}
```

- [ ] **Step 2: Run to verify they fail**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.profile.ProfileViewModelTest" --no-daemon --console=plain`
Expected: FAIL — unresolved `editEntry`/`deleteEntry`/`weightEntries`.

- [ ] **Step 3: Extend `ProfileUiState` and the ViewModel**

In `ProfileViewModel.kt`:

1. Add `import com.musfit.data.repository.WeightEntry` to the imports.

2. Add three fields to `ProfileUiState` (after `measurements`):

```kotlin
val weightEntries: List<WeightEntry> = emptyList(),
val measurementEntries: Map<String, List<BodyMeasurement>> = emptyMap(),
val weeklyWeightDeltaKg: Double? = null,
```

3. In the `dataState` builder, add a weekly-delta computation just before `ProfileUiState(`:

```kotlin
val weeklyDelta = BodyMetricsCalculator.changeOverWindow(
    points = weightSeries.map { it.measuredAtEpochMillis to it.weightKg },
    windowMillis = 7L * 86_400_000L,
    nowMillis = System.currentTimeMillis(),
)
```

and add these to the `ProfileUiState(...)` constructor call (after `measurements = ...`):

```kotlin
weightEntries = weightSeries,
measurementEntries = measurements,
weeklyWeightDeltaKg = weeklyDelta,
```

4. Add the two actions (after `logMeasurement`):

```kotlin
fun editEntry(id: String, value: Double) {
    viewModelScope.launch {
        runCatching { profileRepository.updateEntryValue(id, value) }
            .onFailure { messageFlow.value = it.message ?: "Could not update entry." }
    }
}

fun deleteEntry(id: String) {
    viewModelScope.launch {
        runCatching { profileRepository.deleteEntry(id) }
            .onFailure { messageFlow.value = it.message ?: "Could not delete entry." }
    }
}
```

- [ ] **Step 4: Run the ViewModel tests to verify they pass, then commit**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.profile.ProfileViewModelTest" --no-daemon --console=plain`
Expected: PASS.

```bash
git add -A
git commit -m "feat: expose recent entries, weekly delta, and edit/delete in ProfileViewModel"
```

---

### Task 4: UI — sparkline, deltas, and the recent-entries sheet

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/profile/ProfileEditContent.kt`
- Modify: `app/src/main/java/com/musfit/ui/profile/ProfileScreen.kt`

No automated UI tests; verify on device.

- [ ] **Step 1: Add the entries sheet to `ProfileEditContent.kt`**

Add these imports to `ProfileEditContent.kt` (these are not already present in that file; `Column`, `Row`, `fillMaxWidth`, `dp`, `Text`, `TextButton`, `AlertDialog`, `remember`/`mutableStateOf`/`getValue`/`setValue`, `Modifier` already are — do not re-add them):

```kotlin
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.ui.Alignment
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
```

Add at the bottom of the file:

```kotlin
data class EntrySheetItem(val id: String, val dateLabel: String, val value: Double, val unit: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntriesSheet(
    title: String,
    items: List<EntrySheetItem>,
    onDismiss: () -> Unit,
    onEdit: (id: String, value: Double) -> Unit,
    onDelete: (id: String) -> Unit,
) {
    var editing by remember { mutableStateOf<EntrySheetItem?>(null) }
    var deleting by remember { mutableStateOf<EntrySheetItem?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (items.isEmpty()) {
                Text(
                    "No entries yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
            items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${item.value.format1()} ${item.unit}", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            item.dateLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { editing = item }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { deleting = item }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }

    editing?.let { item ->
        var text by remember(item.id) { mutableStateOf(item.value.format1()) }
        val parsed = text.toPositiveDoubleOrNull()
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Edit value") },
            text = { NumberField(value = text, onValueChange = { text = it }, label = "Value (${item.unit})") },
            confirmButton = {
                TextButton(enabled = parsed != null, onClick = {
                    parsed?.let { onEdit(item.id, it) }
                    editing = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
        )
    }

    deleting?.let { item ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete entry?") },
            text = { Text("${item.value.format1()} ${item.unit} · ${item.dateLabel}") },
            confirmButton = {
                TextButton(onClick = { onDelete(item.id); deleting = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

internal fun formatEntryDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        .format(DateTimeFormatter.ofPattern("d MMM yyyy"))
```

- [ ] **Step 2: Add the sparkline + delta + sheet wiring to `ProfileScreen.kt`**

Add imports (none of these are already in `ProfileScreen.kt`; `clickable`, `Alignment`, `clip`, `dp`, `FontWeight` already are):

```kotlin
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.width
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.abs
```

In `ProfileScreen`, add two sheet-state variables next to the existing `showLogWeight` etc.:

```kotlin
var showWeightSheet by remember { mutableStateOf(false) }
var measurementSheetType by remember { mutableStateOf<String?>(null) }
```

Change the `WeightCard` and `MeasurementsCard` calls to pass the open-sheet callbacks:

```kotlin
WeightCard(state = state, onLog = { showLogWeight = true }, onOpenEntries = { showWeightSheet = true })
MeasurementsCard(state = state, onLog = { showLogMeasurement = true }, onOpenType = { measurementSheetType = it })
```

After the three existing `if (showXxx) { ... Dialog }` blocks, add the two sheets:

```kotlin
if (showWeightSheet) {
    EntriesSheet(
        title = "Weight history",
        items = state.weightEntries.map { EntrySheetItem(it.id, formatEntryDate(it.measuredAtEpochMillis), it.weightKg, "kg") },
        onDismiss = { showWeightSheet = false },
        onEdit = viewModel::editEntry,
        onDelete = viewModel::deleteEntry,
    )
}
measurementSheetType?.let { type ->
    val rows = state.measurementEntries[type].orEmpty()
    EntriesSheet(
        title = "${MEASUREMENT_SHEET_LABELS[type] ?: type} history",
        items = rows.map { EntrySheetItem(it.id, formatEntryDate(it.measuredAtEpochMillis), it.value, it.unit) },
        onDismiss = { measurementSheetType = null },
        onEdit = viewModel::editEntry,
        onDelete = viewModel::deleteEntry,
    )
}
```

Add a label map near the top-level helpers of the file:

```kotlin
private val MEASUREMENT_SHEET_LABELS = mapOf(
    "waist" to "Waist", "chest" to "Chest", "arms" to "Arms",
    "thighs" to "Thighs", "hips" to "Hips", "body_fat" to "Body fat",
)
```

Replace `WeightCard` with the sparkline + delta version:

```kotlin
@Composable
private fun WeightCard(state: ProfileUiState, onLog: () -> Unit, onOpenEntries: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenEntries)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Weight", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onLog) { Text("Log") }
            }
            if (state.latestWeightKg != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${state.latestWeightKg.format1()} kg", style = MaterialTheme.typography.titleLarge)
                        state.weeklyWeightDeltaKg?.let { delta ->
                            val arrow = if (delta < 0) "▼" else if (delta > 0) "▲" else "•"
                            Text(
                                "$arrow ${abs(delta).format1()} kg this week",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (state.weightTrend.size >= 2) {
                        Sparkline(values = state.weightTrend, modifier = Modifier.width(120.dp).height(36.dp))
                    }
                }
            } else {
                Text(
                    "No weight logged yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Sparkline(values: List<Double>, modifier: Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val minV = values.min()
        val maxV = values.max()
        val range = (maxV - minV).takeIf { it > 0.0 } ?: 1.0
        val stepX = size.width / (values.size - 1)
        val offsets = values.mapIndexed { i, v ->
            Offset(stepX * i, (size.height - ((v - minV) / range * size.height)).toFloat())
        }
        val path = Path().apply {
            moveTo(offsets.first().x, offsets.first().y)
            offsets.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(path, color = lineColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
```

Replace `MeasurementsCard` to show deltas and open the per-type sheet:

```kotlin
@Composable
private fun MeasurementsCard(state: ProfileUiState, onLog: () -> Unit, onOpenType: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Measurements", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onLog) { Text("Log") }
            }
            state.measurements.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { measurement ->
                        MeasurementCell(
                            row = measurement,
                            onClick = { onOpenType(measurement.type) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MeasurementCell(row: MeasurementRow, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(row.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                row.value?.let { "${it.format1()} ${row.unit}" } ?: "—",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
            row.deltaFromPrevious?.let { d ->
                if (d != 0.0) {
                    Text(
                        "${if (d < 0) "▼" else "▲"}${abs(d).format1()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
```

(Leave the existing `MiniMetric` in place — it is still used by `IdentityCard` and `VitalsCard`.)

- [ ] **Step 3: Build and verify on device**

Run: `.\gradlew.bat assembleDebug --no-daemon --console=plain`
Then install and launch: `adb install -r app\build\outputs\apk\debug\app-debug.apk` and `adb shell am start -n com.musfit/.MainActivity`.
On the Profile tab, confirm: after logging two+ weights, the Weight card shows a sparkline and a "▼/▲ … kg this week" line; each measurement cell shows its delta; tapping the Weight card or a measurement cell opens a sheet listing entries; edit changes the value and delete removes it, with the dashboard updating live.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: add weight sparkline, deltas, and recent-entries edit/delete sheet"
```

---

### Task 5: Full verification

- [ ] **Step 1: Run full verification**

Run: `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL; all unit tests pass; lint clean.

- [ ] **Step 2: Commit any verification fixes**

```bash
git add -A
git commit -m "chore: verify Profile polish"
```

---

## Acceptance Criteria

- The Weight card shows a Compose-drawn sparkline of recent weights and a "▼/▲ … kg this week" delta; tapping it opens a sheet of recent weights.
- Each measurement cell shows its ▲/▼ delta vs the previous entry; tapping a cell opens that type's entries sheet.
- A listed entry can be edited (value only) or deleted; the dashboard (latest value, sparkline, deltas, BMI, goal progress) updates immediately.
- No schema/migration change; database remains version 18.
- `testDebugUnitTest`, `lintDebug`, and `assembleDebug` pass.

## Non-Goals

- Full weight/measurement history screen or charting library; editing an entry's date; bulk operations.
- Units (imperial), theme/dark-mode, backup/export; Health Connect writeback or extra vitals.
