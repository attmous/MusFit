package com.musfit.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musfit.ui.components.ExpressiveBadge
import com.musfit.ui.components.ExpressiveBadgeShape
import com.musfit.ui.components.MusFitScreenHeader
import com.musfit.ui.components.SectionHeader
import com.musfit.ui.components.TonalHeaderIconButton
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
import com.musfit.ui.theme.TabAccentRole
import com.musfit.ui.theme.tabAccentFor
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** History-surface key for the body-weight series (measurements use their type key). */
private const val WEIGHT_HISTORY_KEY = "weight"

@Composable
fun ProfileScreen(
    onSettingsClick: () -> Unit,
    onOpenFood: () -> Unit = {},
    onOpenTrainingProgress: () -> Unit = {},
    onOpenNutritionTrends: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val accent = tabAccentFor(TabAccentRole.Profile)
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

    var showLogWeight by remember { mutableStateOf(false) }
    var showLogMeasurement by remember { mutableStateOf(false) }
    var logMeasurementInitialType by remember { mutableStateOf<String?>(null) }
    // Turn 11: weight and measurement histories are full inner surfaces (11f),
    // hosted with the same takeover pattern Food/Training inner screens use.
    var historyKey by rememberSaveable { mutableStateOf<String?>(null) }

    BackHandler(enabled = historyKey != null) { historyKey = null }

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
        val openHistoryKey = historyKey
        if (openHistoryKey != null) {
            val isWeight = openHistoryKey == WEIGHT_HISTORY_KEY
            val entries = if (isWeight) {
                state.weightEntries.map { HistoryEntry(it.id, it.measuredAtEpochMillis, it.weightKg, "kg") }
            } else {
                state.measurementEntries[openHistoryKey].orEmpty().map {
                    HistoryEntry(it.id, it.measuredAtEpochMillis, it.value, it.unit)
                }
            }
            MeasurementHistoryScreen(
                title = if (isWeight) "Weight history" else "${MEASUREMENT_SHEET_LABELS[openHistoryKey] ?: openHistoryKey} history",
                entries = entries,
                accent = accent,
                onBack = { historyKey = null },
                onAdd = {
                    if (isWeight) showLogWeight = true else logMeasurementInitialType = openHistoryKey
                },
                onEdit = viewModel::editEntry,
                onDelete = viewModel::deleteEntry,
                goalValue = if (isWeight) state.hero.goalWeightKg else null,
                modifier = Modifier.padding(padding),
            )
        } else {
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
                WeightHeroCard(
                    state = state,
                    accent = accent,
                    onOpenEntries = { historyKey = WEIGHT_HISTORY_KEY },
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
                    accent = accent,
                    onOpenType = { type ->
                        state.tiles.firstOrNull { it.type == type }?.let { tile ->
                            if (tile.entryCount == 0) logMeasurementInitialType = type else historyKey = type
                        }
                    },
                )
                SectionHeader(title = "Plans & progress")
                PlansAndProgressList(
                    plansSummary = state.plansSummary,
                    onOpenFood = onOpenFood,
                    onOpenTrainingProgress = onOpenTrainingProgress,
                    onOpenNutritionTrends = onOpenNutritionTrends,
                )
            }
        }
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

/**
 * The Turn 11 weight hero: the one tonal container on the tab. Display weight
 * with inline unit, delta/BMI meta line, a white goal chip that now carries the
 * pace ("goal 82 kg · gain 0.3/wk"), the 30-day sparkline, and a footer with
 * "30 days · 50% to goal" plus the filled "＋ Log weight" pill. Tapping the
 * container opens the 11f weight history.
 */
@Composable
private fun WeightHeroCard(
    state: ProfileUiState,
    accent: TabAccent,
    onOpenEntries: () -> Unit,
    onLogWeight: () -> Unit,
) {
    val hero = state.hero
    val shape = RoundedCornerShape(28.dp)
    Surface(
        color = accent.container,
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClickLabel = "Open weight history") { onOpenEntries() },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (hero.hasAnyEntry) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            // hasAnyEntry ⇔ latestWeightKg != null by construction (both from the same series).
                            Text(
                                hero.latestWeightKg!!.format1(),
                                style = MusFitTheme.typography.displayLarge.copy(fontSize = 52.sp, lineHeight = 52.sp),
                                color = accent.onContainer,
                                maxLines = 1,
                            )
                            Text(
                                "kg",
                                style = MusFitTheme.typography.titleLarge.copy(fontSize = 19.sp),
                                fontWeight = FontWeight.Medium,
                                color = accent.onContainer,
                                maxLines = 1,
                                modifier = Modifier.padding(start = 6.dp, bottom = 6.dp),
                            )
                        }
                        WeightMetaLine(hero = hero, accent = accent)
                    }
                    if (hero.goalWeightKg != null) {
                        HeroChip(text = goalChipText(state), accent = accent)
                    }
                }
                when {
                    hero.chartSeries.size >= 2 ->
                        ProfileTrendChart(
                            values = hero.chartSeries,
                            color = accent.color,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                        )

                    hero.chartSeries.isEmpty() -> // entries exist (outer branch) but none in the window
                        Text(
                            "No entries in the last 30 days.",
                            style = MusFitTheme.typography.bodySmall,
                            color = accent.onContainerVariant,
                        )

                    else -> // exactly one point in the window — a chart or "no entries" text would both mislead
                        Text(
                            "Log again to see a trend.",
                            style = MusFitTheme.typography.bodySmall,
                            color = accent.onContainerVariant,
                        )
                }
            } else {
                Text("No weight logged yet.", style = MusFitTheme.typography.bodyMedium, color = accent.onContainerVariant)
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (hero.hasAnyEntry) {
                        buildAnnotatedString {
                            append("30 days")
                            hero.goalProgressFraction?.let {
                                append(" · ")
                                withStyle(SpanStyle(fontWeight = FontWeight.W800)) {
                                    append("${(it * 100).roundToInt()}%")
                                }
                                append(" to goal")
                            }
                        }
                    } else {
                        buildAnnotatedString {}
                    },
                    style = MusFitTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = accent.onContainerVariant,
                    modifier = Modifier.weight(1f),
                )
                HeroActionPill(
                    text = "Log weight",
                    icon = Icons.Outlined.Add,
                    accent = accent,
                    onClick = onLogWeight,
                )
            }
        }
    }
}

/** "+0.5 kg · 7 days · BMI 25" — the delta emphasized, the rest quiet. */
@Composable
private fun WeightMetaLine(hero: WeightHeroState, accent: TabAccent) {
    val delta = hero.deltaKg
    val meta = buildAnnotatedString {
        if (delta != null) {
            withStyle(SpanStyle(fontWeight = FontWeight.W800)) {
                append("${if (delta < 0) "−" else "+"}${abs(delta).format1()} kg")
            }
            append(" · 7 days")
        }
        hero.bmi?.let {
            if (length > 0) append(" · ")
            append("BMI ${it.format1()}")
        }
    }
    if (meta.isNotEmpty()) {
        Text(
            meta,
            style = MusFitTheme.typography.bodySmall,
            color = accent.onContainerVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** "goal 82 kg · gain 0.3/wk" — the goal chip carries the pace since Turn 11. */
private fun goalChipText(state: ProfileUiState): String {
    val goalWeight = state.hero.goalWeightKg ?: return ""
    val profile = state.profile
    val paceText = profile?.let {
        when (it.goalType) {
            com.musfit.domain.profile.GoalType.Maintain -> "maintain"
            com.musfit.domain.profile.GoalType.Lose -> "lose ${it.goalPaceKgPerWeek.format1()}/wk"
            com.musfit.domain.profile.GoalType.Gain -> "gain ${it.goalPaceKgPerWeek.format1()}/wk"
        }
    }
    return buildString {
        append("goal ${goalWeight.format1()} kg")
        paceText?.let { append(" · $it") }
    }
}

/**
 * Measurements as the Turn 11 (11a) three-column grid: white cells with grouped
 * corners (24dp on the grid's outside, 8dp inside), quiet label, emphasized
 * value with its unit at the baseline, and an icon+delta trend row — or a faint
 * "Tap to log" placeholder for genuinely empty cells.
 */
@Composable
private fun MeasurementsGrid(state: ProfileUiState, accent: TabAccent, onOpenType: (String) -> Unit) {
    val columns = 3
    val rows = state.tiles.chunked(columns)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEachIndexed { rowIndex, rowTiles ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                rowTiles.forEachIndexed { columnIndex, tile ->
                    MeasurementCell(
                        tile = tile,
                        accent = accent,
                        shape = gridGroupShape(rowIndex, rows.size, columnIndex, columns),
                        onClick = { onOpenType(tile.type) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(columns - rowTiles.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun MeasurementCell(
    tile: MeasurementTile,
    accent: TabAccent,
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
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                tile.label,
                style = MusFitTheme.typography.labelMedium.copy(fontSize = 11.sp),
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
                Row {
                    Text(
                        tile.value!!.format1(),
                        style = MusFitTheme.typography.titleLarge.copy(fontSize = 18.sp, letterSpacing = (-0.4).sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = MusFitTheme.colors.onSurface,
                        maxLines = 1,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Text(
                        tile.unit,
                        style = MusFitTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.Medium,
                        color = MusFitTheme.colors.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.alignByBaseline().padding(start = 2.dp),
                    )
                }
                MeasurementTrendRow(delta = tile.delta30d, accent = accent)
            }
        }
    }
}

/**
 * The 30-day trend row: direction icon + delta in the Profile teal for a
 * meaningful change, gray trending_flat with "flat" when the value is
 * effectively unchanged. Hidden until a 30-day baseline exists.
 */
@Composable
private fun MeasurementTrendRow(delta: Double?, accent: TabAccent) {
    if (delta == null) return
    // Below display resolution (one decimal) counts as flat.
    val flat = abs(delta) < 0.05
    val color = if (flat) MusFitTheme.colors.onSurfaceFaint else accent.color
    val icon = when {
        flat -> Icons.AutoMirrored.Outlined.TrendingFlat
        delta < 0 -> Icons.AutoMirrored.Outlined.TrendingDown
        else -> Icons.AutoMirrored.Outlined.TrendingUp
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        Text(
            text = if (flat) "flat" else "${if (delta < 0) "−" else "+"}${abs(delta).format1()}",
            style = MusFitTheme.typography.labelMedium.copy(fontSize = 11.sp),
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
        )
    }
}

/**
 * Turn 11 "Plans & progress": one grouped list of three launcher rows with
 * alternating expressive badges — goals & programs (Profile teal sunny),
 * training progress (Training indigo circle), nutrition trends (Food green
 * squircle). Cross-tab rows keep their own tab families.
 */
@Composable
private fun PlansAndProgressList(
    plansSummary: String,
    onOpenFood: () -> Unit,
    onOpenTrainingProgress: () -> Unit,
    onOpenNutritionTrends: () -> Unit,
) {
    val profileAccent = tabAccentFor(TabAccentRole.Profile)
    val trainingAccent = tabAccentFor(TabAccentRole.Training)
    val foodAccent = tabAccentFor(TabAccentRole.Food)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ProfileHubRow(
            title = "Goals & programs",
            subtitle = plansSummary.ifBlank { "Set your goal, diet, and program" },
            shape = groupedShape(0, 3),
            onClick = onOpenFood,
            onClickLabel = "Open goals and programs in Food",
            leading = {
                ExpressiveBadge(
                    icon = Icons.Outlined.Flag,
                    shape = ExpressiveBadgeShape.Sunny,
                    containerColor = profileAccent.container,
                    contentColor = profileAccent.onContainer,
                    size = 40.dp,
                    iconSize = 19.dp,
                )
            },
        )
        ProfileHubRow(
            title = "Training progress",
            subtitle = "PRs, trends and volume per exercise",
            shape = groupedShape(1, 3),
            onClick = onOpenTrainingProgress,
            leading = {
                ExpressiveBadge(
                    icon = Icons.AutoMirrored.Outlined.TrendingUp,
                    shape = ExpressiveBadgeShape.Circle,
                    containerColor = trainingAccent.container,
                    contentColor = trainingAccent.onContainer,
                    size = 40.dp,
                    iconSize = 19.dp,
                )
            },
        )
        ProfileHubRow(
            title = "Nutrition trends",
            subtitle = "Weekly score · 7 and 28-day progress",
            shape = groupedShape(2, 3),
            onClick = onOpenNutritionTrends,
            leading = {
                ExpressiveBadge(
                    icon = Icons.Outlined.QueryStats,
                    shape = ExpressiveBadgeShape.Squircle,
                    containerColor = foodAccent.container,
                    contentColor = foodAccent.onContainerVariant,
                    size = 40.dp,
                    iconSize = 19.dp,
                )
            },
        )
    }
}

internal val MEASUREMENT_SHEET_LABELS = mapOf(
    "waist" to "Waist",
    "chest" to "Chest",
    "arms" to "Arms",
    "thighs" to "Thighs",
    "hips" to "Hips",
    "body_fat" to "Body fat",
)

internal fun Double.format1(): String = if (this % 1.0 == 0.0) toInt().toString() else String.format(Locale.US, "%.1f", this)

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
