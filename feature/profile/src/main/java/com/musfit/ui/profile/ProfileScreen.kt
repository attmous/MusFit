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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musfit.feature.profile.R
import com.musfit.ui.components.ExpressiveBadge
import com.musfit.ui.components.ExpressiveBadgeShape
import com.musfit.ui.components.MusFitScreenHeader
import com.musfit.ui.components.SectionHeader
import com.musfit.ui.components.TonalHeaderIconButton
import com.musfit.ui.components.gridGroupShape
import com.musfit.ui.components.groupedShape
import com.musfit.ui.text.LocalizedFormatter
import com.musfit.ui.text.UiText
import com.musfit.ui.text.asString
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

    val resolvedMessage = state.message?.asString()
    LaunchedEffect(state.message, resolvedMessage) {
        val message = resolvedMessage
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
            val measurementLabel = MEASUREMENT_LABEL_RESOURCES[openHistoryKey]
                ?.let { stringResource(it) }
                ?: openHistoryKey
            MeasurementHistoryScreen(
                title = if (isWeight) {
                    stringResource(R.string.profile_weight_history)
                } else {
                    stringResource(R.string.profile_measurement_history, measurementLabel)
                },
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
                    title = stringResource(R.string.profile_title),
                    actions = {
                        TonalHeaderIconButton(
                            icon = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.profile_settings),
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
                    title = stringResource(R.string.profile_measurements),
                    trailingActionLabel = stringResource(R.string.profile_log_action),
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
                SectionHeader(title = stringResource(R.string.profile_plans_and_progress))
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
            .clickable(
                onClickLabel = stringResource(R.string.profile_open_health_connect_settings),
                role = Role.Button,
                onClick = onOpen,
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.profile_health_connect_nudge),
                style = MusFitTheme.typography.bodySmall,
                color = body,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.profile_set_up),
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
            .clickable(
                onClickLabel = stringResource(R.string.profile_open_weight_history),
                role = Role.Button,
                onClick = onOpenEntries,
            ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (hero.hasAnyEntry) {
                WeightHeroSummary(state = state, accent = accent)
                when {
                    hero.chartSeries.size >= 2 ->
                        ProfileTrendChart(
                            values = hero.chartSeries,
                            color = accent.color,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                        )

                    hero.chartSeries.isEmpty() -> // entries exist (outer branch) but none in the window
                        Text(
                            stringResource(R.string.profile_no_entries_30_days),
                            style = MusFitTheme.typography.bodySmall,
                            color = accent.onContainerVariant,
                        )

                    else -> // exactly one point in the window — a chart or "no entries" text would both mislead
                        Text(
                            stringResource(R.string.profile_log_again_for_trend),
                            style = MusFitTheme.typography.bodySmall,
                            color = accent.onContainerVariant,
                        )
                }
            } else {
                Text(
                    stringResource(R.string.profile_no_weight_logged),
                    style = MusFitTheme.typography.bodyMedium,
                    color = accent.onContainerVariant,
                )
            }
            WeightHeroFooter(hero = hero, accent = accent, onLogWeight = onLogWeight)
        }
    }
}

@Composable
internal fun WeightHeroCardPreview(
    state: ProfileUiState,
    accent: TabAccent,
    onOpenEntries: () -> Unit,
    onLogWeight: () -> Unit,
) = WeightHeroCard(state, accent, onOpenEntries, onLogWeight)

@Composable
private fun WeightHeroFooter(
    hero: WeightHeroState,
    accent: TabAccent,
    onLogWeight: () -> Unit,
) {
    val progressText = if (hero.hasAnyEntry) {
        hero.goalProgressFraction?.let {
            stringResource(
                R.string.profile_30_days_to_goal,
                LocalizedFormatter.integer((it * 100).roundToInt().toLong(), locale = LocalConfiguration.current.locales[0]),
            )
        } ?: stringResource(R.string.profile_30_days)
    } else {
        ""
    }
    val action: @Composable () -> Unit = {
        HeroActionPill(
            text = stringResource(R.string.profile_log_weight),
            icon = Icons.Outlined.Add,
            accent = accent,
            onClick = onLogWeight,
        )
    }
    if (LocalDensity.current.fontScale >= 1.3f) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(progressText, style = MusFitTheme.typography.bodySmall, color = accent.onContainerVariant)
            action()
        }
    } else {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                progressText,
                style = MusFitTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = accent.onContainerVariant,
                modifier = Modifier.weight(1f),
            )
            action()
        }
    }
}

@Composable
private fun WeightHeroSummary(state: ProfileUiState, accent: TabAccent) {
    val hero = state.hero
    val goalChip: @Composable () -> Unit = {
        if (hero.goalWeightKg != null) {
            HeroChip(text = goalChipText(state), accent = accent)
        }
    }
    if (LocalDensity.current.fontScale >= 1.3f) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WeightHeroValue(hero = hero, accent = accent)
            goalChip()
        }
    } else {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            WeightHeroValue(hero = hero, accent = accent, modifier = Modifier.weight(1f))
            goalChip()
        }
    }
}

@Composable
private fun WeightHeroValue(
    hero: WeightHeroState,
    accent: TabAccent,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            // hasAnyEntry ⇔ latestWeightKg != null by construction (both from the same series).
            Text(
                hero.latestWeightKg!!.format1(LocalConfiguration.current.locales[0]),
                style = MusFitTheme.typography.displayLarge.copy(fontSize = 52.sp, lineHeight = 52.sp),
                color = accent.onContainer,
                maxLines = 1,
            )
            Text(
                stringResource(R.string.profile_unit_kg),
                style = MusFitTheme.typography.titleLarge.copy(fontSize = 19.sp),
                fontWeight = FontWeight.Medium,
                color = accent.onContainer,
                maxLines = 1,
                modifier = Modifier.padding(start = 6.dp, bottom = 6.dp),
            )
        }
        WeightMetaLine(hero = hero, accent = accent)
    }
}

/** "+0.5 kg · 7 days · BMI 25" — the delta emphasized, the rest quiet. */
@Composable
private fun WeightMetaLine(hero: WeightHeroState, accent: TabAccent) {
    val delta = hero.deltaKg
    val locale = LocalConfiguration.current.locales[0]
    val deltaText = delta?.let {
        stringResource(
            R.string.profile_weight_delta_7_days,
            stringResource(if (it < 0) R.string.profile_sign_minus else R.string.profile_sign_plus),
            abs(it).format1(locale),
        )
    }
    val bmiText = hero.bmi?.let {
        stringResource(R.string.profile_bmi, it.format1(locale))
    }
    val meta = when {
        deltaText != null && bmiText != null -> stringResource(R.string.profile_join_middle_dot, deltaText, bmiText)
        deltaText != null -> deltaText
        else -> bmiText.orEmpty()
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
@Composable
private fun goalChipText(state: ProfileUiState): String {
    val goalWeight = state.hero.goalWeightKg ?: return ""
    val profile = state.profile
    val locale = LocalConfiguration.current.locales[0]
    val paceText = profile?.let {
        when (it.goalType) {
            com.musfit.domain.profile.GoalType.Maintain -> stringResource(R.string.profile_pace_maintain)

            com.musfit.domain.profile.GoalType.Lose -> stringResource(
                R.string.profile_pace_lose,
                it.goalPaceKgPerWeek.format1(locale),
            )

            com.musfit.domain.profile.GoalType.Gain -> stringResource(
                R.string.profile_pace_gain,
                it.goalPaceKgPerWeek.format1(locale),
            )
        }
    }
    val goal = goalWeight.format1(locale)
    return paceText?.let { stringResource(R.string.profile_goal_weight_with_pace, goal, it) }
        ?: stringResource(R.string.profile_goal_weight, goal)
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
    val label = tile.label.asString()
    Surface(
        color = MusFitTheme.colors.surface,
        shape = shape,
        modifier = modifier
            .clip(shape)
            .clickable(
                onClickLabel = stringResource(
                    if (empty) R.string.profile_log_measurement_accessibility else R.string.profile_open_measurement_history,
                    label,
                ),
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                label,
                style = MusFitTheme.typography.labelMedium.copy(fontSize = 11.sp),
                fontWeight = FontWeight.SemiBold,
                color = MusFitTheme.colors.onSurfaceVariant,
                maxLines = 1,
            )
            if (empty) {
                Text(
                    stringResource(R.string.profile_tap_to_log),
                    style = MusFitTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                    fontWeight = FontWeight.Medium,
                    color = MusFitTheme.colors.onSurfaceFaint,
                    maxLines = 1,
                )
            } else {
                MeasurementValue(tile.value!!, tile.unit)
                MeasurementTrendRow(delta = tile.delta30d, accent = accent)
            }
        }
    }
}

@Composable
private fun MeasurementValue(value: Double, unit: String) {
    Row {
        Text(
            value.format1(LocalConfiguration.current.locales[0]),
            style = MusFitTheme.typography.titleLarge.copy(fontSize = 18.sp, letterSpacing = (-0.4).sp),
            fontWeight = FontWeight.ExtraBold,
            color = MusFitTheme.colors.onSurface,
            maxLines = 1,
            modifier = Modifier.alignByBaseline(),
        )
        Text(
            unit,
            style = MusFitTheme.typography.bodySmall.copy(fontSize = 11.sp),
            fontWeight = FontWeight.Medium,
            color = MusFitTheme.colors.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.alignByBaseline().padding(start = 2.dp),
        )
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
            text = if (flat) {
                stringResource(R.string.profile_flat)
            } else {
                stringResource(if (delta < 0) R.string.profile_sign_minus else R.string.profile_sign_plus) +
                    abs(delta).format1(LocalConfiguration.current.locales[0])
            },
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
    plansSummary: UiText?,
    onOpenFood: () -> Unit,
    onOpenTrainingProgress: () -> Unit,
    onOpenNutritionTrends: () -> Unit,
) {
    val profileAccent = tabAccentFor(TabAccentRole.Profile)
    val trainingAccent = tabAccentFor(TabAccentRole.Training)
    val foodAccent = tabAccentFor(TabAccentRole.Food)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ProfileHubRow(
            title = stringResource(R.string.profile_goals_and_programs),
            subtitle = plansSummary?.asString() ?: stringResource(R.string.profile_goals_and_programs_empty),
            shape = groupedShape(0, 3),
            onClick = onOpenFood,
            onClickLabel = stringResource(R.string.profile_open_goals_food),
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
            title = stringResource(R.string.profile_training_progress),
            subtitle = stringResource(R.string.profile_training_progress_summary),
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
            title = stringResource(R.string.profile_nutrition_trends),
            subtitle = stringResource(R.string.profile_nutrition_trends_summary),
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

internal fun Double.format1(locale: Locale = Locale.getDefault()): String = LocalizedFormatter.number(this, maximumFractionDigits = 1, locale = locale)

internal fun com.musfit.domain.profile.ActivityLevel.labelResource(): Int = when (this) {
    com.musfit.domain.profile.ActivityLevel.Sedentary -> R.string.profile_activity_sedentary
    com.musfit.domain.profile.ActivityLevel.Light -> R.string.profile_activity_light
    com.musfit.domain.profile.ActivityLevel.Moderate -> R.string.profile_activity_moderate
    com.musfit.domain.profile.ActivityLevel.Active -> R.string.profile_activity_active
    com.musfit.domain.profile.ActivityLevel.VeryActive -> R.string.profile_activity_very_active
}

internal fun com.musfit.domain.profile.GoalType.labelResource(): Int = when (this) {
    com.musfit.domain.profile.GoalType.Lose -> R.string.profile_goal_lose
    com.musfit.domain.profile.GoalType.Maintain -> R.string.profile_goal_maintain
    com.musfit.domain.profile.GoalType.Gain -> R.string.profile_goal_gain
}

internal fun com.musfit.domain.profile.Sex.labelResource(): Int = when (this) {
    com.musfit.domain.profile.Sex.Male -> R.string.profile_sex_male
    com.musfit.domain.profile.Sex.Female -> R.string.profile_sex_female
}
