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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.musfit.data.repository.DEFAULT_USER_PROFILE
import com.musfit.ui.AppDestination
import com.musfit.ui.components.MusFitScreenHeader
import com.musfit.ui.components.MusFitSummaryCard
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
                title = "Profile",
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
            )
            if (state.isHealthConnectNudgeVisible) {
                HealthConnectNudge(onOpen = onSettingsClick)
            }
            WeightCard(state = state, accent = accent, onLog = { showLogWeight = true }, onOpenEntries = { showWeightSheet = true })
            SectionHeader(
                title = "Measurements",
                trailingActionLabel = "+ Log",
                trailingActionColor = accent.color,
                onTrailingAction = { showLogMeasurement = true },
            )
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
    val shape = MaterialTheme.shapes.extraLarge
    Card(
        modifier = Modifier
            .fillMaxWidth()
            // Clip before clickable so the ripple stays inside the rounded shape.
            .clip(shape)
            .clickable(onClickLabel = "Open Health Connect settings", onClick = onOpen),
        shape = shape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Connect Health Connect to mirror steps and heart rate",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text("Set up", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun PlanCardRow(card: PlanCard, onClick: () -> Unit) {
    val shape = MaterialTheme.shapes.extraLarge
    Card(
        modifier = Modifier
            .fillMaxWidth()
            // Clip before clickable so the ripple stays inside the rounded shape.
            .clip(shape)
            .clickable(
                onClickLabel = if (card.id == "diet") "Manage diet in Food" else "Manage program in Training",
                onClick = onClick,
            ),
        shape = shape,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(card.title, style = MaterialTheme.typography.titleMedium)
            Text(card.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GoalCard(state: ProfileUiState, onApply: () -> Unit, onComplete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

@Composable
private fun WeightCard(state: ProfileUiState, accent: TabAccent, onLog: () -> Unit, onOpenEntries: () -> Unit) {
    val hero = state.hero
    MusFitSummaryCard(accent = accent, onClick = onOpenEntries, onClickLabel = "Open weight history") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Weight", style = MaterialTheme.typography.titleMedium, color = accent.onContainer, modifier = Modifier.weight(1f))
                hero.goalWeightKg?.let { goal ->
                    val percent = hero.goalProgressFraction?.let { " · ${(it * 100).roundToInt()}%" } ?: ""
                    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), shape = MaterialTheme.shapes.small) {
                        Text(
                            "goal ${goal.format1()} kg$percent",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = accent.onContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
            if (hero.hasAnyEntry) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // hasAnyEntry ⇔ latestWeightKg != null by construction (both from the same series).
                    Text("${hero.latestWeightKg!!.format1()} kg", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = accent.onContainer)
                    Text(
                        hero.deltaKg?.let { d -> "${if (d < 0) "−" else "+"}${abs(d).format1()} kg · 7d" } ?: "latest",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent.onContainer,
                    )
                }
                hero.bmi?.let { Text("BMI ${it.format1()}", style = MaterialTheme.typography.labelSmall, color = accent.onContainer) }
                when {
                    hero.chartSeries.size >= 2 ->
                        TrendLineChart(values = hero.chartSeries, accent = accent.color, modifier = Modifier.fillMaxWidth().height(72.dp))
                    hero.chartSeries.isEmpty() -> // entries exist (outer branch) but none in the window
                        Text("No entries in the last 30 days.", style = MaterialTheme.typography.bodySmall, color = accent.onContainer)
                    else -> // exactly one point in the window — a chart or "no entries" text would both mislead
                        Text("Log again to see a trend.", style = MaterialTheme.typography.bodySmall, color = accent.onContainer)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("30 days", style = MaterialTheme.typography.labelSmall, color = accent.onContainer, modifier = Modifier.weight(1f))
                    // Accent-tonal per spec: content colored with the accent ink on the tinted card.
                    TextButton(onClick = onLog) { Text("+ Log weight", color = accent.onContainer, fontWeight = FontWeight.SemiBold) }
                }
            } else {
                Text("No weight logged yet.", style = MaterialTheme.typography.bodyMedium, color = accent.onContainer)
                TextButton(onClick = onLog) { Text("+ Log weight", color = accent.onContainer, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun MeasurementsGrid(state: ProfileUiState, accent: TabAccent, onOpenType: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.tiles.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { tile ->
                        MeasurementCell(
                            tile = tile,
                            accent = accent,
                            onClick = { onOpenType(tile.type) },
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
private fun MeasurementCell(tile: MeasurementTile, accent: TabAccent, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                onClickLabel = if (tile.entryCount == 0) "Log ${tile.label}" else "Open ${tile.label} history",
                onClick = onClick,
            )
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(tile.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (tile.entryCount == 0) {
            Text("— · Tap to log", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${tile.value!!.format1()} ${tile.unit}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                val delta = tile.deltaFromPrevious
                when {
                    delta == null -> Text("—", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) // spec: 1-entry state shows value + "—" delta
                    delta != 0.0 -> Text(
                        "${if (delta < 0) "▼" else "▲"}${abs(delta).format1()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics {
                            contentDescription = "${if (delta < 0) "down" else "up"} ${abs(delta).format1()}"
                        },
                    )
                }
            }
            if (tile.sparkline.size >= 2) {
                TrendLineChart(
                    values = tile.sparkline,
                    accent = accent.color,
                    modifier = Modifier.fillMaxWidth().height(24.dp).padding(top = 4.dp),
                )
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
