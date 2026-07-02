package com.musfit.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.musfit.domain.today.MetricValue
import com.musfit.domain.today.TodayMetric
import com.musfit.ui.AppDestination
import com.musfit.ui.components.MusFitSummaryCard
import com.musfit.ui.components.charts.MetricRing
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import com.musfit.ui.theme.tabAccentFor

/** Today's summary card: a swipeable pager of configurable metrics (Coral). */
@Composable
fun MetricCarouselCard(
    carousel: CarouselUiState,
    onMetricClick: (TodayMetric) -> Unit,
) {
    if (carousel.pages.isEmpty()) return
    val accent = tabAccentFor(AppDestination.Today)
    val pagerState = rememberPagerState(pageCount = { carousel.pages.size })

    MusFitSummaryCard(accent = accent) {
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
                            .background(if (selected) accent.color else accent.onContainer.copy(alpha = 0.25f)),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeroPage(
    page: CarouselPageUiState,
    accent: TabAccent,
    onMetricClick: (TodayMetric) -> Unit,
) {
    val hero = page.hero ?: return
    // Spec: with fewer pins the hero grows into the free space.
    val heroDiameter = if (page.chips.isEmpty()) 128.dp else 96.dp
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MusFitTheme.spacing.lg),
    ) {
        Surface(
            onClick = { onMetricClick(hero.metric) },
            color = Color.Transparent,
        ) {
            when (val value = hero.value) {
                is MetricValue.WithGoal ->
                    MetricRing(
                        progress = value.progress,
                        color = accent.color,
                        diameter = heroDiameter,
                        trackColor = MusFitTheme.colors.onSurface.copy(alpha = 0.12f),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = value.figure,
                                style = MusFitTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = accent.onContainer,
                            )
                            Text(
                                text = value.caption,
                                style = MusFitTheme.typography.labelSmall,
                                color = accent.onContainer,
                            )
                        }
                    }
                is MetricValue.Plain ->
                    HeroFigure(figure = value.figure, caption = value.caption, accent = accent)
                is MetricValue.NoData ->
                    HeroFigure(figure = "—", caption = value.caption, accent = accent)
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MusFitTheme.spacing.sm),
        ) {
            page.chips.forEach { chip ->
                MetricChip(chip = chip, accent = accent, onClick = { onMetricClick(chip.metric) })
            }
        }
    }
}

@Composable
private fun HeroFigure(figure: String, caption: String, accent: TabAccent) {
    Column {
        Text(
            text = figure,
            style = MusFitTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = accent.onContainer,
        )
        Text(text = caption, style = MusFitTheme.typography.labelSmall, color = accent.onContainer)
    }
}

@Composable
private fun ChipGridPage(
    chips: List<MetricCardUiState>,
    accent: TabAccent,
    onMetricClick: (TodayMetric) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MusFitTheme.spacing.sm)) {
        chips.chunked(2).forEach { rowChips ->
            Row(horizontalArrangement = Arrangement.spacedBy(MusFitTheme.spacing.sm)) {
                rowChips.forEach { chip ->
                    Box(modifier = Modifier.weight(1f)) {
                        MetricChip(chip = chip, accent = accent, onClick = { onMetricClick(chip.metric) })
                    }
                }
                if (rowChips.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetricChip(chip: MetricCardUiState, accent: TabAccent, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MusFitTheme.colors.surface.copy(alpha = 0.55f),
        shape = MusFitTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
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
            Text(
                text = figure,
                style = MusFitTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accent.onContainer,
            )
            Text(
                text = "${chip.label} · $caption",
                style = MusFitTheme.typography.labelSmall,
                color = accent.onContainer.copy(alpha = 0.8f),
                maxLines = 1,
            )
        }
    }
}

/** Spec §1 metric-pool deep-link table: each metric's home tab. */
internal fun metricDestination(metric: TodayMetric): AppDestination = when (metric) {
    TodayMetric.Calories, TodayMetric.Protein, TodayMetric.Carbs, TodayMetric.Fat,
    TodayMetric.Water, TodayMetric.CalorieBalance, TodayMetric.LoggingStreak,
    -> AppDestination.Food
    TodayMetric.Sessions -> AppDestination.Training
    TodayMetric.Steps, TodayMetric.Weight, TodayMetric.BodyFat,
    TodayMetric.ActiveCalories, TodayMetric.RestingHeartRate,
    -> AppDestination.Profile
}

/** Edit sheet: pin/order metrics + the Goals section (step/session/target-weight). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardEditSheet(
    state: TodayUiState,
    onTogglePin: (TodayMetric) -> Unit,
    onMovePin: (TodayMetric, Boolean) -> Unit,
    onStepGoalChanged: (String) -> Unit,
    onSessionTargetChanged: (String) -> Unit,
    onTargetWeightChanged: (String) -> Unit,
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
                fontWeight = FontWeight.Bold,
                color = MusFitTheme.colors.onSurface,
            )
            Text(
                text = "Pin the metrics you're focused on. The first one is your hero.",
                style = MusFitTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurfaceVariant,
            )

            // Pinned first (in order, with reorder controls), then the rest of the pool.
            val pinned = state.editPins
            val unpinned = TodayMetric.entries.filter { it !in pinned }
            (pinned + unpinned).forEach { metric ->
                val isPinned = metric in pinned
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = isPinned, onCheckedChange = { onTogglePin(metric) })
                    Text(
                        text = metric.label,
                        style = MusFitTheme.typography.bodyMedium,
                        color = MusFitTheme.colors.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (isPinned) {
                        IconButton(onClick = { onMovePin(metric, true) }) {
                            Icon(Icons.Outlined.ArrowUpward, contentDescription = "Move ${metric.label} up", tint = MusFitTheme.colors.onSurfaceVariant)
                        }
                        IconButton(onClick = { onMovePin(metric, false) }) {
                            Icon(Icons.Outlined.ArrowDownward, contentDescription = "Move ${metric.label} down", tint = MusFitTheme.colors.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(Modifier.height(MusFitTheme.spacing.sm))
            Text(
                text = "Goals",
                style = MusFitTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
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
            OutlinedTextField(
                value = state.targetWeightInput,
                onValueChange = onTargetWeightChanged,
                label = { Text("Target weight (kg)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
