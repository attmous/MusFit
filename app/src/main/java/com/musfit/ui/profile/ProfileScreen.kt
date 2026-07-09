package com.musfit.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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
import com.musfit.ui.components.TonalHeaderIconButton
import com.musfit.ui.components.WavyProgressBar
import com.musfit.ui.components.charts.TrendLineChart
import com.musfit.ui.components.gridGroupShape
import com.musfit.ui.components.groupedShape
import com.musfit.ui.theme.LavenderBody
import com.musfit.ui.theme.LavenderBodyDark
import com.musfit.ui.theme.LavenderContainer
import com.musfit.ui.theme.LavenderContainerDark
import com.musfit.ui.theme.LavenderInk
import com.musfit.ui.theme.LavenderInkDark
import com.musfit.ui.theme.MusFitTheme
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
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MusFitScreenHeader(
                title = "Profile",
                actions = {
                    TonalHeaderIconButton(
                        icon = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        onClick = onSettingsClick,
                    )
                },
            )
            if (state.isHealthConnectNudgeVisible) {
                HealthConnectNudge(onOpen = onSettingsClick)
            }
            WeightCard(
                state = state,
                accent = accent,
                onOpenEntries = { showWeightSheet = true },
                onLogWeight = { showLogWeight = true },
            )
            SectionHeader(
                title = "Measurements",
                trailingActionLabel = "+ Log",
                trailingActionColor = accent.color,
                onTrailingAction = { showLogMeasurement = true },
            )
            MeasurementsGrid(
                state = state,
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
            GoalCard(state = state, accent = accent, onApply = viewModel::applyTargetsToFood, onComplete = { showEditor = true })
            SectionHeader(title = "Plans")
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                state.planCards.forEachIndexed { index, card ->
                    PlanCardRow(
                        card = card,
                        shape = groupedShape(index, state.planCards.size),
                        onClick = { if (card.id == "diet") onOpenFood() else onOpenTraining() },
                    )
                }
            }
            SectionHeader(title = "Progress & trends")
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ProfileNavRow(
                    title = "Training progress",
                    subtitle = "PRs, trends, and volume analytics per exercise.",
                    shape = groupedShape(0, 2),
                    onClick = onOpenTrainingProgress,
                )
                ProfileNavRow(
                    title = "Nutrition trends",
                    subtitle = "Weekly MusFit score and 7/28-day progress.",
                    shape = groupedShape(1, 2),
                    onClick = onOpenNutritionTrends,
                )
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

/** The mock-6d Health Connect banner: a fully rounded lavender pill. */
@Composable
private fun HealthConnectNudge(onOpen: () -> Unit) {
    val dark = isSystemInDarkTheme()
    val container = if (dark) LavenderContainerDark else LavenderContainer
    val body = if (dark) LavenderBodyDark else LavenderBody
    val action = if (dark) LavenderInkDark else LavenderInk
    val shape = RoundedCornerShape(999.dp)
    Surface(
        color = container,
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            // Clip before clickable so the ripple stays inside the rounded shape.
            .clip(shape)
            .clickable(onClickLabel = "Open Health Connect settings", onClick = onOpen),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Connect Health Connect to mirror steps and heart rate",
                style = MusFitTheme.typography.bodySmall,
                color = body,
                modifier = Modifier.weight(1f),
            )
            Text(
                "Set up",
                style = MusFitTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = action,
            )
        }
    }
}

/** A grouped white list row: emphasized title over one quiet meta line. */
@Composable
private fun ProfileNavRow(title: String, subtitle: String, shape: RoundedCornerShape, onClick: () -> Unit) {
    Surface(
        color = MusFitTheme.colors.surface,
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClickLabel = title, onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(title, style = MusFitTheme.typography.titleSmall)
            Text(subtitle, style = MusFitTheme.typography.bodySmall, color = MusFitTheme.colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun PlanCardRow(card: PlanCard, shape: RoundedCornerShape, onClick: () -> Unit) {
    Surface(
        color = MusFitTheme.colors.surface,
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(
                onClickLabel = if (card.id == "diet") "Manage diet in Food" else "Manage program in Training",
                onClick = onClick,
            ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(card.title, style = MusFitTheme.typography.titleSmall)
            Text(card.subtitle, style = MusFitTheme.typography.bodySmall, color = MusFitTheme.colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun GoalCard(state: ProfileUiState, accent: TabAccent, onApply: () -> Unit, onComplete: () -> Unit) {
    Surface(
        color = MusFitTheme.colors.surface,
        shape = MusFitTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val profile = state.profile
            if (profile != null) {
                val goalText = buildString {
                    append(profile.goalType.label())
                    if (profile.goalType != com.musfit.domain.profile.GoalType.Maintain) {
                        append(" · ${profile.goalPaceKgPerWeek.format1()} kg/wk")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        goalText,
                        style = MusFitTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    val currentWeight = state.hero.latestWeightKg
                    if (currentWeight != null && profile.goalWeightKg != null) {
                        Text(
                            "${currentWeight.format1()} → ${profile.goalWeightKg.format1()} kg",
                            style = MusFitTheme.typography.bodySmall,
                            color = MusFitTheme.colors.onSurfaceVariant,
                        )
                    }
                }
                val progress = state.hero.goalProgressFraction
                if (progress != null) {
                    WavyProgressBar(
                        progress = progress.toFloat().coerceIn(0f, 1f),
                        color = accent.color,
                        trackColor = accent.track,
                    )
                }
            }
            val targets = state.recommendedTargets
            if (targets != null) {
                Text(
                    "${targets.caloriesKcal.format1()} kcal · recommended",
                    style = MusFitTheme.typography.titleLarge,
                )
                Text(
                    "P ${targets.proteinGrams.format1()} g · C ${targets.carbsGrams.format1()} g · F ${targets.fatGrams.format1()} g",
                    style = MusFitTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
                Button(
                    onClick = onApply,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = accent.onColor),
                ) {
                    Text("Apply to Food goals")
                }
            } else {
                Text(
                    "Complete your profile to see recommended calories and macros.",
                    style = MusFitTheme.typography.bodyMedium,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
                Button(
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = accent.onColor),
                ) {
                    Text("Complete your profile")
                }
            }
        }
    }
}

/**
 * The mock-6d weight hero: a teal tonal container with the display weight and
 * inline unit, an emphasized delta/BMI line, a white goal chip top-right, the
 * 30-day sparkline, and a filled "+ Log weight" button. Tapping the container
 * opens the weight history.
 */
@Composable
private fun WeightCard(
    state: ProfileUiState,
    accent: TabAccent,
    onOpenEntries: () -> Unit,
    onLogWeight: () -> Unit,
) {
    val hero = state.hero
    val shape = MusFitTheme.shapes.extraLarge
    Surface(
        color = accent.container,
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClickLabel = "Open weight history") { onOpenEntries() },
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (hero.hasAnyEntry) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.weight(1f)) {
                        // hasAnyEntry ⇔ latestWeightKg != null by construction (both from the same series).
                        Text(
                            hero.latestWeightKg!!.format1(),
                            style = MusFitTheme.typography.displayLarge.copy(fontSize = 52.sp),
                            color = accent.onContainer,
                            maxLines = 1,
                        )
                        Text(
                            "kg",
                            style = MusFitTheme.typography.titleLarge.copy(fontSize = 19.sp),
                            fontWeight = FontWeight.Medium,
                            color = accent.onContainerVariant,
                            maxLines = 1,
                            modifier = Modifier.padding(start = 5.dp, bottom = 7.dp),
                        )
                    }
                    if (hero.goalWeightKg != null) {
                        Surface(color = MusFitTheme.colors.surface, shape = RoundedCornerShape(999.dp)) {
                            Text(
                                text = buildString {
                                    append("goal ${hero.goalWeightKg.format1()} kg")
                                    hero.goalProgressFraction?.let { append(" · ${(it * 100).roundToInt()}%") }
                                },
                                style = MusFitTheme.typography.labelMedium.copy(fontSize = 12.sp),
                                fontWeight = FontWeight.ExtraBold,
                                color = accent.onContainer,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
                val delta = hero.deltaKg
                val sub = buildAnnotatedString {
                    if (delta != null) {
                        withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = accent.onContainer)) {
                            append("${if (delta < 0) "−" else "+"}${abs(delta).format1()} kg")
                        }
                        append(" · 7 days")
                    }
                    hero.bmi?.let {
                        if (length > 0) append(" · ")
                        append("BMI ${it.format1()}")
                    }
                }
                if (sub.isNotEmpty()) {
                    Text(sub, style = MusFitTheme.typography.bodySmall, color = accent.onContainerVariant)
                }
                when {
                    hero.chartSeries.size >= 2 ->
                        TrendLineChart(
                            values = hero.chartSeries,
                            accent = accent.color,
                            modifier = Modifier.fillMaxWidth().height(64.dp).padding(top = 4.dp),
                        )
                    hero.chartSeries.isEmpty() -> // entries exist (outer branch) but none in the window
                        Text("No entries in the last 30 days.", style = MusFitTheme.typography.bodySmall, color = accent.onContainerVariant)
                    else -> // exactly one point in the window — a chart or "no entries" text would both mislead
                        Text("Log again to see a trend.", style = MusFitTheme.typography.bodySmall, color = accent.onContainerVariant)
                }
            } else {
                Text("No weight logged yet.", style = MusFitTheme.typography.bodyMedium, color = accent.onContainerVariant)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (hero.hasAnyEntry) "30 days" else "",
                    style = MusFitTheme.typography.bodySmall,
                    color = accent.onContainerVariant,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    onClick = onLogWeight,
                    color = accent.color,
                    contentColor = accent.onColor,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text(
                        "+ Log weight",
                        style = MusFitTheme.typography.labelMedium.copy(fontSize = 13.sp),
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

/**
 * Measurements as the mock-6d two-column grid: white cells with grouped corners
 * (24dp on the grid's outside, 8dp inside), quiet label over an emphasized
 * value — or a faint "Tap to log" placeholder.
 */
@Composable
private fun MeasurementsGrid(state: ProfileUiState, onOpenType: (String) -> Unit) {
    val columns = 2
    val rows = state.tiles.chunked(columns)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEachIndexed { rowIndex, rowTiles ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                rowTiles.forEachIndexed { columnIndex, tile ->
                    MeasurementCell(
                        tile = tile,
                        shape = gridGroupShape(rowIndex, rows.size, columnIndex, columns),
                        onClick = { onOpenType(tile.type) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowTiles.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MeasurementCell(
    tile: MeasurementTile,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val empty = tile.entryCount == 0
    Surface(
        color = MusFitTheme.colors.surface,
        shape = shape,
        modifier = modifier
            .clip(shape)
            .clickable(
                onClickLabel = if (empty) "Log ${tile.label}" else "Open ${tile.label} history",
                onClick = onClick,
            ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                tile.label,
                style = MusFitTheme.typography.labelMedium.copy(fontSize = 11.5.sp),
                fontWeight = FontWeight.SemiBold,
                color = MusFitTheme.colors.onSurfaceVariant,
                maxLines = 1,
            )
            if (empty) {
                Text(
                    "Tap to log",
                    style = MusFitTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                    fontWeight = FontWeight.Medium,
                    color = MusFitTheme.colors.onSurfaceFaint,
                    maxLines = 1,
                )
            } else {
                Text(
                    "${tile.value!!.format1()} ${tile.unit}",
                    style = MusFitTheme.typography.titleLarge.copy(fontSize = 18.sp),
                    color = MusFitTheme.colors.onSurface,
                    maxLines = 1,
                )
            }
        }
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
