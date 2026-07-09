package com.musfit.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import com.musfit.ui.theme.tabAccentFor

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
    val todayAccent = tabAccentFor(AppDestination.Today)
    val healthAccent = tabAccentFor(AppDestination.Profile)
    val pullRefreshState = rememberPullToRefreshState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(
                isRefreshing = state.isRefreshing,
                state = pullRefreshState,
                onRefresh = viewModel::refreshTodayData,
            ),
    ) {
        MusFitScreenScaffold(
            title = todayGreeting(java.time.LocalTime.now().hour),
            actions = {
                state.readiness?.let { readiness ->
                    ReadinessHeaderChip(
                        readiness = readiness,
                        onClick = onOpenHealth,
                        accent = healthAccent,
                    )
                }
                IconButton(onClick = viewModel::openDashboardEditor) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Edit dashboard",
                        tint = MusFitTheme.colors.onSurfaceVariant,
                    )
                }
            },
        ) {
            MetricCarouselCard(
                carousel = state.carousel,
                onMetricClick = { metric -> navigateTo(metricDestination(metric)) },
            )

            CoachSectionHeader(hasUnread = state.feed.any { group -> group.messages.any { !it.isRead } })
            if (state.feed.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    title = "Let's get started",
                    body = "Log your first meal and I'll take it from there.",
                    accent = todayAccent,
                    actionLabel = "Log a meal",
                    onAction = onOpenFood,
                )
            } else {
                CoachFeed(groups = state.feed, onAction = onCoachAction, onDismiss = viewModel::dismissMessage)
            }
        }

        if (state.isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = todayAccent.color,
                trackColor = Color.Transparent,
            )
        } else {
            LinearProgressIndicator(
                progress = {
                    todayRefreshIndicatorUiState(
                        isRefreshing = false,
                        pullDistanceFraction = pullRefreshState.distanceFraction,
                    ).progress ?: 0f
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = todayAccent.color,
                trackColor = Color.Transparent,
            )
        }
    }

    if (state.isDashboardEditorVisible) {
        DashboardEditSheet(
            state = state,
            onTogglePin = viewModel::togglePin,
            onMovePin = viewModel::movePin,
            onStepGoalChanged = viewModel::onStepGoalInputChanged,
            onSessionTargetChanged = viewModel::onSessionTargetInputChanged,
            onSave = viewModel::saveDashboard,
            onDismiss = viewModel::closeDashboardEditor,
        )
    }
}

/** Time-of-day greeting title — "Good morning", not a shouted tab label. */
internal fun todayGreeting(hour: Int): String = when (hour) {
    in 4..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    else -> "Good evening"
}

/** "Coach" section header with the 7dp coral unread dot from the mock. */
@Composable
private fun CoachSectionHeader(hasUnread: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Coach",
            style = MusFitTheme.typography.titleMedium,
            color = MusFitTheme.colors.onSurface,
        )
        if (hasUnread) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(MusFitTheme.colors.accent),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadinessHeaderChip(
    readiness: TodayReadinessUiState,
    onClick: () -> Unit,
    accent: TabAccent,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = accent.container,
        modifier = Modifier.semantics {
            contentDescription = "Readiness estimate ${readiness.score}, ${readiness.levelLabel}"
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.MonitorHeart,
                contentDescription = null,
                tint = accent.onContainer,
            )
            Text(
                text = readiness.label,
                style = MusFitTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = accent.onContainer,
            )
        }
    }
}

internal data class TodayRefreshIndicatorUiState(
    val isVisible: Boolean,
    val progress: Float?,
)

internal fun todayRefreshIndicatorUiState(
    isRefreshing: Boolean,
    pullDistanceFraction: Float,
): TodayRefreshIndicatorUiState =
    when {
        isRefreshing -> TodayRefreshIndicatorUiState(isVisible = true, progress = null)
        pullDistanceFraction > 0f -> TodayRefreshIndicatorUiState(
            isVisible = true,
            progress = pullDistanceFraction.coerceIn(0f, 1f),
        )
        else -> TodayRefreshIndicatorUiState(isVisible = false, progress = null)
    }
