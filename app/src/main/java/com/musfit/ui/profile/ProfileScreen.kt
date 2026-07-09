package com.musfit.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.musfit.data.repository.DEFAULT_USER_PROFILE
import com.musfit.ui.AppDestination
import com.musfit.ui.components.MusFitScreenHeader
import com.musfit.ui.components.SectionHeader
import com.musfit.ui.components.charts.TrendLineChart
import com.musfit.ui.theme.TabAccent
import com.musfit.ui.theme.tabAccentFor
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ProfileScreen(
    onSettingsClick: () -> Unit,
    onOpenFood: () -> Unit = {},
    onOpenTraining: () -> Unit = {},
    onOpenTrainingProgress: () -> Unit = {},
    onOpenNutritionTrends: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val accent = tabAccentFor(AppDestination.Profile)
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Re-check the Health Connect nudge whenever the screen resumes: permissions are
    // granted outside the app, so init-only state would never hide it.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onScreenResumed()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showEditor by remember { mutableStateOf(false) }
    var showLogWeight by remember { mutableStateOf(false) }
    var showLogMeasurement by remember { mutableStateOf(false) }
    var logMeasurementInitialType by remember { mutableStateOf<String?>(null) }
    var showWeightSheet by remember { mutableStateOf(false) }
    var measurementSheetType by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.message) {
        val message = state.message
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // The app-level Scaffold already applies the system-bar insets; this inner
        // Scaffold (kept only for the snackbar) must not re-add them, or the top inset
        // doubles into a gap above the header.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MusFitScreenHeader(
                title = "Body",
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
            )
            if (state.isHealthConnectNudgeVisible) {
                HealthConnectNudge(onOpen = onSettingsClick)
            }
            WeightCard(state = state, accent = accent, onOpenEntries = { showWeightSheet = true })
            ProfileQuickActions(
                accent = accent,
                onLogWeight = { showLogWeight = true },
                onLogMeasurement = { showLogMeasurement = true },
            )
            SectionHeader(title = "Measurements")
            MeasurementsGrid(
                state = state,
                accent = accent,
                onOpenType = { type ->
                    state.tiles.firstOrNull { it.type == type }?.let { tile ->
                        if (tile.entryCount == 0) logMeasurementInitialType = type else measurementSheetType = type
                    }
                },
            )
            SectionHeader(
                title = "Goal",
                trailingActionLabel = "Edit",
                trailingActionColor = accent.color,
                onTrailingAction = { showEditor = true },
            )
            GoalCard(state = state, onApply = viewModel::applyTargetsToFood, onComplete = { showEditor = true })
            SectionHeader(title = "Plans")
            state.planCards.forEach { card ->
                PlanCardRow(card = card, onClick = { if (card.id == "diet") onOpenFood() else onOpenTraining() })
            }
            SectionHeader(title = "Progress & trends")
            ProfileNavRow(
                title = "Training progress",
                subtitle = "PRs, trends, and volume analytics per exercise.",
                onClick = onOpenTrainingProgress,
            )
            ProfileNavRow(
                title = "Nutrition trends",
                subtitle = "Weekly MusFit score and 7/28-day progress.",
                onClick = onOpenNutritionTrends,
            )
        }
    }

    if (showEditor) {
        ProfileEditDialog(
            initial = state.profile ?: DEFAULT_USER_PROFILE,
            initialWeightKg = state.hero.latestWeightKg,
            onDismiss = { showEditor = false },
            onSave = { profile, weightKg ->
                viewModel.saveProfile(profile)
                if (weightKg != null) viewModel.logWeight(weightKg)
                showEditor = false
            },
        )
    }
    if (showLogWeight) {
        LogWeightDialog(
            prefillKg = state.hero.latestWeightKg,
            onDismiss = { showLogWeight = false },
            onConfirm = { weightKg ->
                viewModel.logWeight(weightKg)
                showLogWeight = false
            },
        )
    }
    if (showLogMeasurement) {
        LogMeasurementDialog(
            onDismiss = { showLogMeasurement = false },
            onConfirm = { type, value, unit ->
                viewModel.logMeasurement(type, value, unit)
                showLogMeasurement = false
            },
        )
    }
    logMeasurementInitialType?.let { initialType ->
        LogMeasurementDialog(
            initialType = initialType,
            onDismiss = { logMeasurementInitialType = null },
            onConfirm = { type, value, unit ->
                viewModel.logMeasurement(type, value, unit)
                logMeasurementInitialType = null
            },
        )
    }
    if (showWeightSheet) {
        EntriesSheet(
            title = "Weight history",
            items = state.weightEntries.map {
                EntrySheetItem(it.id, formatEntryDate(it.measuredAtEpochMillis), it.weightKg, "kg")
            },
            onDismiss = { showWeightSheet = false },
            onEdit = viewModel::editEntry,
            onDelete = viewModel::deleteEntry,
            // Full all-time series — the sheet's chart is deliberately unwindowed.
            chartSeries = remember(state.weightEntries) { state.weightEntries.map { it.weightKg }.asReversed() },
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
            chartSeries = remember(rows) { rows.map { it.value }.asReversed() },
        )
    }
}

@Composable
private fun HealthConnectNudge(onOpen: () -> Unit) {
    // Quiet neutral strip — no card chrome.
    val shape = MaterialTheme.shapes.large
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            // Clip before clickable so the ripple stays inside the rounded shape.
            .clip(shape)
            .clickable(onClickLabel = "Open Health Connect settings", onClick = onOpen),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Connect Health Connect to mirror steps, sleep, workouts, weight, and heart rate",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text("Set up", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** A hairline list row: title 15/500, meta 13 secondary, no card chrome. */
@Composable
private fun ProfileNavRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = title, onClick = onClick)
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun PlanCardRow(card: PlanCard, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClickLabel = if (card.id == "diet") "Manage diet in Food" else "Manage program in Training",
                    onClick = onClick,
                )
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(card.title, style = MaterialTheme.typography.titleSmall)
            Text(card.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun GoalCard(state: ProfileUiState, onApply: () -> Unit, onComplete: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val profile = state.profile
            if (profile != null) {
                val goalText = buildString {
                    append(profile.goalType.label())
                    if (profile.goalType != com.musfit.domain.profile.GoalType.Maintain) {
                        append(" · ${profile.goalPaceKgPerWeek.format1()} kg/wk")
                    }
                }
                Text(goalText, style = MaterialTheme.typography.bodyMedium)
                val currentWeight = state.hero.latestWeightKg
                if (currentWeight != null && profile.goalWeightKg != null) {
                    Text(
                        "${currentWeight.format1()} kg → ${profile.goalWeightKg.format1()} kg",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val progress = state.hero.goalProgressFraction
                if (progress != null) {
                    ProgressBar(fraction = progress.toFloat())
                }
            }
            val targets = state.recommendedTargets
            if (targets != null) {
                Text(
                    "${targets.caloriesKcal.format1()} kcal · recommended",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    "P ${targets.proteinGrams.format1()} g · C ${targets.carbsGrams.format1()} g · F ${targets.fatGrams.format1()} g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onApply, modifier = Modifier.fillMaxWidth()) {
                    Text("Apply to Food goals")
                }
            } else {
                Text(
                    "Complete your profile to see recommended calories and macros.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onComplete, modifier = Modifier.fillMaxWidth()) {
                    Text("Complete your profile")
                }
            }
        }
    }
}

/**
 * The naked weight hero (mock 3d): `80.9 kg` at 44/300 with a smaller inline
 * unit, a tonal 7-day delta chip, a quiet goal caption, then a bare 90dp
 * sparkline over a hairline baseline. Tapping it opens the weight history.
 */
@Composable
private fun WeightCard(state: ProfileUiState, accent: TabAccent, onOpenEntries: () -> Unit) {
    val hero = state.hero
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClickLabel = "Open weight history") { onOpenEntries() },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (hero.hasAnyEntry) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    // hasAnyEntry ⇔ latestWeightKg != null by construction (both from the same series).
                    Text(
                        hero.latestWeightKg!!.format1(),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    Text(
                        "kg",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                    )
                }
                hero.deltaKg?.let { d ->
                    Surface(color = accent.container, shape = RoundedCornerShape(999.dp)) {
                        Text(
                            "${if (d < 0) "−" else "+"}${abs(d).format1()} · 7d",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = accent.onContainer,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            val caption = buildString {
                hero.goalWeightKg?.let { goal ->
                    append("Goal ${goal.format1()} kg")
                    hero.goalProgressFraction?.let { append(" · ${(it * 100).roundToInt()}% there") }
                }
                hero.bmi?.let {
                    if (isNotEmpty()) append(" · ")
                    append("BMI ${it.format1()}")
                }
            }
            if (caption.isNotEmpty()) {
                Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            when {
                hero.chartSeries.size >= 2 ->
                    TrendLineChart(
                        values = hero.chartSeries,
                        accent = accent.color,
                        showBaseline = true,
                        modifier = Modifier.fillMaxWidth().height(90.dp).padding(top = 6.dp),
                    )
                hero.chartSeries.isEmpty() -> // entries exist (outer branch) but none in the window
                    Text("No entries in the last 30 days.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> // exactly one point in the window — a chart or "no entries" text would both mislead
                    Text("Log again to see a trend.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (hero.chartSeries.size >= 2) {
                Text(
                    "30 days",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        } else {
            Text("No weight logged yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Quick actions row (mock 3d): a tonal accent "Log weight" chip + a neutral "Measure" chip. */
@Composable
private fun ProfileQuickActions(
    accent: TabAccent,
    onLogWeight: () -> Unit,
    onLogMeasurement: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ProfileQuickChip(
            label = "+ Log weight",
            container = accent.container,
            content = accent.onContainer,
            onClick = onLogWeight,
        )
        ProfileQuickChip(
            label = "Measure",
            container = MaterialTheme.colorScheme.surfaceVariant,
            content = MaterialTheme.colorScheme.onSurface,
            onClick = onLogMeasurement,
        )
    }
}

@Composable
private fun ProfileQuickChip(
    label: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Surface(
        color = container,
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = content,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
        )
    }
}

/** Measurements as a hairline list (mock 3d): name / small sparkline / value right-aligned. */
@Composable
private fun MeasurementsGrid(state: ProfileUiState, accent: TabAccent, onOpenType: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        state.tiles.forEach { tile ->
            MeasurementRow(tile = tile, accent = accent, onClick = { onOpenType(tile.type) })
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun MeasurementRow(tile: MeasurementTile, accent: TabAccent, onClick: () -> Unit) {
    val empty = tile.entryCount == 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = if (empty) "Log ${tile.label}" else "Open ${tile.label} history",
                onClick = onClick,
            )
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            tile.label,
            style = MaterialTheme.typography.titleSmall,
            color = if (empty) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (empty) {
            Text(
                "Tap to log",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        } else {
            if (tile.sparkline.size >= 2) {
                TrendLineChart(
                    values = tile.sparkline,
                    accent = accent.color,
                    modifier = Modifier.width(60.dp).height(16.dp),
                )
            }
            val delta = tile.deltaFromPrevious
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "${tile.value!!.format1()} ${tile.unit}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                if (delta != null && delta != 0.0) {
                    Text(
                        "${if (delta < 0) "▼" else "▲"}${abs(delta).format1()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics {
                            contentDescription = "${if (delta < 0) "down" else "up"} ${abs(delta).format1()}"
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressBar(fraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

private val MEASUREMENT_SHEET_LABELS = mapOf(
    "waist" to "Waist", "chest" to "Chest", "arms" to "Arms",
    "thighs" to "Thighs", "hips" to "Hips", "body_fat" to "Body fat",
)

internal fun Double.format1(): String =
    if (this % 1.0 == 0.0) toInt().toString() else String.format(Locale.US, "%.1f", this)

internal fun com.musfit.domain.profile.ActivityLevel.label(): String = when (this) {
    com.musfit.domain.profile.ActivityLevel.Sedentary -> "Sedentary"
    com.musfit.domain.profile.ActivityLevel.Light -> "Light"
    com.musfit.domain.profile.ActivityLevel.Moderate -> "Moderate"
    com.musfit.domain.profile.ActivityLevel.Active -> "Active"
    com.musfit.domain.profile.ActivityLevel.VeryActive -> "Very active"
}

internal fun com.musfit.domain.profile.GoalType.label(): String = when (this) {
    com.musfit.domain.profile.GoalType.Lose -> "Lose"
    com.musfit.domain.profile.GoalType.Maintain -> "Maintain"
    com.musfit.domain.profile.GoalType.Gain -> "Gain"
}

internal fun com.musfit.domain.profile.Sex.label(): String = when (this) {
    com.musfit.domain.profile.Sex.Male -> "Male"
    com.musfit.domain.profile.Sex.Female -> "Female"
}
