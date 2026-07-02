package com.musfit.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.musfit.domain.coach.CoachAction
import com.musfit.ui.AppDestination
import com.musfit.ui.components.EmptyState
import com.musfit.ui.components.MusFitScreenScaffold
import com.musfit.ui.components.SectionHeader
import com.musfit.ui.components.charts.BarDatum
import com.musfit.ui.components.charts.TrendLineChart
import com.musfit.ui.components.charts.WeekBarChart
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import com.musfit.ui.theme.tabAccentFor
import java.util.Locale
import kotlin.math.abs

/** Scroll clearance under the chat FAB: FAB 52 + lg padding 16 + 8 slack. */
private val ChatFabClearance = 76.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: TodayViewModel = hiltViewModel(),
    onOpenFood: () -> Unit = {},
    onOpenTraining: () -> Unit = {},
    onOpenHealth: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onScreenResumed()
                Lifecycle.Event.ON_PAUSE -> viewModel.onScreenPaused()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val navigateTo: (AppDestination) -> Unit = { destination ->
        when (destination) {
            AppDestination.Food -> onOpenFood()
            AppDestination.Training -> onOpenTraining()
            AppDestination.Profile -> onOpenHealth()
            else -> Unit
        }
    }
    val onCoachAction: (CoachAction) -> Unit = { action -> navigateTo(coachActionDestination(action)) }

    Box(modifier = Modifier.fillMaxSize()) {
        MusFitScreenScaffold(
            title = "Today",
            subtitle = state.dateLabel,
            actions = {
                IconButton(onClick = viewModel::openDashboardEditor) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit dashboard", tint = MusFitTheme.colors.onSurfaceVariant)
                }
            },
        ) {
            MetricCarouselCard(
                carousel = state.carousel,
                onMetricClick = { metric -> navigateTo(metricDestination(metric)) },
            )

            SectionHeader(title = "Coach")
            if (state.feed.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    title = "Let's get started",
                    body = "Log your first meal and I'll take it from there.",
                    accent = tabAccentFor(AppDestination.Today),
                    actionLabel = "Log a meal",
                    onAction = onOpenFood,
                )
            } else {
                CoachFeed(groups = state.feed, onAction = onCoachAction, onDismiss = viewModel::dismissMessage)
            }
            Spacer(Modifier.height(ChatFabClearance))
        }

        ChatPreviewFab(
            onClick = viewModel::openChatPreview,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(MusFitTheme.spacing.lg),
        )
    }

    if (state.isChatPreviewVisible) {
        ChatPreviewSheet(onDismiss = viewModel::closeChatPreview)
    }

    if (state.isDashboardEditorVisible) {
        DashboardEditSheet(
            state = state,
            onTogglePin = viewModel::togglePin,
            onMovePin = viewModel::movePin,
            onStepGoalChanged = viewModel::onStepGoalInputChanged,
            onSessionTargetChanged = viewModel::onSessionTargetInputChanged,
            onTargetWeightChanged = viewModel::onTargetWeightInputChanged,
            onSave = viewModel::saveDashboard,
            onDismiss = viewModel::closeDashboardEditor,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RowScope.GlimpseTile(
    icon: ImageVector,
    value: String,
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = MusFitTheme.colors.surface,
        shape = MusFitTheme.shapes.medium,
        modifier = Modifier.weight(1f),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = MusFitTheme.colors.brand)
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MusFitTheme.colors.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeeklyCaloriesCard(
    charts: WeeklyChartsUiState,
    accent: TabAccent,
    selectedIndex: Int?,
    onDaySelected: (Int) -> Unit,
) {
    Surface(color = MusFitTheme.colors.surface, shape = MusFitTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
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
    Surface(color = MusFitTheme.colors.surface, shape = MusFitTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
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
                        style = MaterialTheme.typography.headlineMedium,
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

@Composable
private fun WeeklyMiniTracker(modifier: Modifier, label: String, value: String) {
    Column(
        modifier = modifier
            .clip(MusFitTheme.shapes.medium)
            .background(MusFitTheme.colors.surfaceVariant)
            .padding(12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MusFitTheme.colors.onSurfaceVariant,
            maxLines = 1,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MusFitTheme.colors.onSurface,
        )
    }
}

private fun Double.formatMetric(): String =
    if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", this)
    }
