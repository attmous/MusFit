package com.musfit.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.musfit.domain.today.MetricValue
import com.musfit.domain.today.TodayMetric
import com.musfit.ui.AppDestination
import com.musfit.ui.components.ExpressiveBadge
import com.musfit.ui.components.ExpressiveBadgeShape
import com.musfit.ui.components.WavyProgressBar
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import com.musfit.ui.theme.tabAccentFor

/**
 * Today's hero: a swipeable pager of configurable metrics inside the coral
 * M3 Expressive tonal container — emphasized display numerals, a sunny badge,
 * a live wavy day-progress wave, and pill stat chips.
 */
@Composable
fun MetricCarouselCard(
    carousel: CarouselUiState,
    onMetricClick: (TodayMetric) -> Unit,
) {
    if (carousel.pages.isEmpty()) return
    val accent = tabAccentFor(AppDestination.Today)
    val pagerState = rememberPagerState(pageCount = { carousel.pages.size })

    Surface(
        color = accent.container,
        shape = MusFitTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            HorizontalPager(state = pagerState) { pageIndex ->
                val page = carousel.pages[pageIndex]
                if (page.hero != null) {
                    HeroPage(page = page, accent = accent, onMetricClick = onMetricClick)
                } else {
                    ChipGridPage(chips = page.chips, accent = accent, onMetricClick = onMetricClick)
                }
            }
            if (carousel.pages.size > 1) {
                Spacer(Modifier.height(MusFitTheme.spacing.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    repeat(carousel.pages.size) { index ->
                        val selected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(width = if (selected) 16.dp else 6.dp, height = 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) accent.color else accent.onContainer.copy(alpha = 0.2f),
                                ),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun HeroPage(
    page: CarouselPageUiState,
    accent: TabAccent,
    onMetricClick: (TodayMetric) -> Unit,
) {
    val hero = page.hero ?: return
    val heroFigure = when (val value = hero.value) {
        is MetricValue.WithGoal -> value.figure
        is MetricValue.Plain -> value.figure
        is MetricValue.NoData -> "—"
    }
    val heroCaption = when (val value = hero.value) {
        is MetricValue.WithGoal -> value.caption
        is MetricValue.Plain -> value.caption
        is MetricValue.NoData -> value.caption
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            onClick = { onMetricClick(hero.metric) },
            color = Color.Transparent,
            contentColor = accent.onContainer,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "${hero.label}: $heroFigure $heroCaption" },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = heroFigure,
                        style = MusFitTheme.typography.displayLarge,
                        color = accent.onContainer,
                        maxLines = 1,
                    )
                    Text(
                        text = heroCaption,
                        style = MusFitTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = accent.onContainerVariant,
                    )
                }
                ExpressiveBadge(
                    icon = metricIcon(hero.metric),
                    shape = ExpressiveBadgeShape.Sunny,
                    containerColor = accent.badge,
                    contentColor = accent.onContainerVariant,
                    size = 84.dp,
                    iconSize = 34.dp,
                )
            }
        }
        (hero.value as? MetricValue.WithGoal)?.let { value ->
            Spacer(Modifier.height(14.dp))
            WavyProgressBar(
                progress = value.progress,
                color = accent.color,
                trackColor = accent.track,
                live = true,
            )
        }
        if (page.chips.isNotEmpty()) {
            Spacer(Modifier.height(MusFitTheme.spacing.lg))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MusFitTheme.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(MusFitTheme.spacing.sm),
            ) {
                page.chips.forEach { chip ->
                    MetricChip(chip = chip, accent = accent, onClick = { onMetricClick(chip.metric) })
                }
            }
        }
    }
}

@Composable
private fun ChipGridPage(
    chips: List<MetricCardUiState>,
    accent: TabAccent,
    onMetricClick: (TodayMetric) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MusFitTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(MusFitTheme.spacing.sm),
    ) {
        chips.forEach { chip ->
            MetricChip(chip = chip, accent = accent, onClick = { onMetricClick(chip.metric) })
        }
    }
}

/** A light pill stat chip inside the hero: emphasized figure, quiet caption. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetricChip(chip: MetricCardUiState, accent: TabAccent, onClick: () -> Unit) {
    val figure = when (val value = chip.value) {
        is MetricValue.WithGoal -> value.figure
        is MetricValue.Plain -> value.figure
        is MetricValue.NoData -> "—"
    }
    val caption = when (val value = chip.value) {
        is MetricValue.WithGoal -> value.caption
        is MetricValue.Plain -> value.caption
        is MetricValue.NoData -> value.caption
    }
    Surface(
        onClick = onClick,
        color = accent.chip,
        shape = CircleShape,
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) { append(figure) }
                append(" ${chip.label.lowercase()} · $caption")
            },
            style = MusFitTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = accent.onContainerVariant,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
        )
    }
}

/** Emphasis glyph for a metric's hero badge. */
internal fun metricIcon(metric: TodayMetric): ImageVector = when (metric) {
    TodayMetric.Calories -> Icons.Filled.LocalFireDepartment
    TodayMetric.Protein -> Icons.Filled.Egg
    TodayMetric.Carbs -> Icons.Filled.BakeryDining
    TodayMetric.Fat -> Icons.Filled.Restaurant
    TodayMetric.Water -> Icons.Filled.WaterDrop
    TodayMetric.Steps -> Icons.Filled.DirectionsWalk
    TodayMetric.Weight -> Icons.Filled.MonitorWeight
    TodayMetric.BodyFat -> Icons.Filled.Percent
    TodayMetric.Sessions -> Icons.Filled.FitnessCenter
    TodayMetric.Sleep -> Icons.Filled.Bedtime
    TodayMetric.Exercise -> Icons.Filled.DirectionsRun
    TodayMetric.ActiveCalories -> Icons.Filled.Whatshot
    TodayMetric.RestingHeartRate -> Icons.Filled.MonitorHeart
    TodayMetric.CalorieBalance -> Icons.Filled.Balance
    TodayMetric.LoggingStreak -> Icons.Filled.EmojiEvents
}

/** Spec §1 metric-pool deep-link table: each metric's home tab. */
internal fun metricDestination(metric: TodayMetric): AppDestination = when (metric) {
    TodayMetric.Calories, TodayMetric.Protein, TodayMetric.Carbs, TodayMetric.Fat,
    TodayMetric.Water, TodayMetric.CalorieBalance, TodayMetric.LoggingStreak,
    -> AppDestination.Food
    TodayMetric.Sessions, TodayMetric.Exercise -> AppDestination.Training
    TodayMetric.Steps, TodayMetric.Weight, TodayMetric.BodyFat,
    TodayMetric.Sleep, TodayMetric.ActiveCalories, TodayMetric.RestingHeartRate,
    -> AppDestination.Profile
}

/** Edit sheet: pin/order metrics + the Goals section (step/session). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardEditSheet(
    state: TodayUiState,
    onTogglePin: (TodayMetric) -> Unit,
    onMovePin: (TodayMetric, Boolean) -> Unit,
    onStepGoalChanged: (String) -> Unit,
    onSessionTargetChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val accent = tabAccentFor(AppDestination.Today)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MusFitTheme.colors.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(MusFitTheme.spacing.sm),
        ) {
            Text(
                text = "Dashboard",
                style = MusFitTheme.typography.titleLarge,
                color = MusFitTheme.colors.onSurface,
            )
            Text(
                text = "Pin the metrics you're focused on. The first one is your hero.",
                style = MusFitTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurfaceVariant,
            )

            // Pinned first (in order, with reorder controls), then the rest of the pool.
            val pinned = state.editPins
            pinned.forEachIndexed { index, metric ->
                // The last remaining pin cannot be unpinned — surface the rule as disabled.
                val toggleEnabled = pinned.size > 1
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = true,
                            enabled = toggleEnabled,
                            role = Role.Checkbox,
                            onValueChange = { onTogglePin(metric) },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = true, onCheckedChange = null, enabled = toggleEnabled)
                    Text(
                        text = metric.label,
                        style = MusFitTheme.typography.bodyMedium,
                        color = MusFitTheme.colors.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    val arrowColors = IconButtonDefaults.iconButtonColors(contentColor = MusFitTheme.colors.onSurfaceVariant)
                    IconButton(onClick = { onMovePin(metric, true) }, enabled = index > 0, colors = arrowColors) {
                        Icon(Icons.Outlined.ArrowUpward, contentDescription = "Move ${metric.label} up")
                    }
                    IconButton(onClick = { onMovePin(metric, false) }, enabled = index < pinned.lastIndex, colors = arrowColors) {
                        Icon(Icons.Outlined.ArrowDownward, contentDescription = "Move ${metric.label} down")
                    }
                }
            }
            TodayMetric.entries.filter { it !in pinned }.forEach { metric ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = false,
                            role = Role.Checkbox,
                            onValueChange = { onTogglePin(metric) },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = false, onCheckedChange = null)
                    Text(
                        text = metric.label,
                        style = MusFitTheme.typography.bodyMedium,
                        color = MusFitTheme.colors.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(MusFitTheme.spacing.sm))
            Text(
                text = "Goals",
                style = MusFitTheme.typography.titleMedium,
                color = MusFitTheme.colors.onSurface,
            )
            OutlinedTextField(
                value = state.stepGoalInput,
                onValueChange = onStepGoalChanged,
                label = { Text("Daily step goal") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.sessionTargetInput,
                onValueChange = onSessionTargetChanged,
                label = { Text("Workouts per week") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent.color,
                    contentColor = accent.onColor,
                ),
            ) {
                Text("Save dashboard")
            }
        }
    }
}
