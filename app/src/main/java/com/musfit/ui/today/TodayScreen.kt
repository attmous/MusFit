package com.musfit.ui.today

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.musfit.domain.coach.CoachAction
import com.musfit.ui.AppDestination
import com.musfit.ui.components.EmptyState
import com.musfit.ui.components.MusFitScreenScaffold
import com.musfit.ui.components.SectionHeader
import com.musfit.ui.theme.MusFitTheme
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
    val pullRefreshState = rememberPullToRefreshState()
    val refreshIndicator = todayRefreshIndicatorUiState(
        isRefreshing = state.isRefreshing,
        pullDistanceFraction = pullRefreshState.distanceFraction,
    )

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = viewModel::refreshTodayData,
        modifier = Modifier.fillMaxSize(),
        state = pullRefreshState,
        indicator = {},
    ) {
        MusFitScreenScaffold(
            title = "Today",
            subtitle = state.dateLabel,
            actions = {
                IconButton(onClick = viewModel::openDashboardEditor) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit dashboard", tint = MusFitTheme.colors.onSurfaceVariant)
                }
            },
        ) {
            if (refreshIndicator.isVisible) {
                val progress = refreshIndicator.progress
                if (progress != null) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = todayAccent.color,
                        trackColor = todayAccent.container,
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = todayAccent.color,
                        trackColor = todayAccent.container,
                    )
                }
            }

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
                    accent = todayAccent,
                    actionLabel = "Log a meal",
                    onAction = onOpenFood,
                )
            } else {
                CoachFeed(groups = state.feed, onAction = onCoachAction, onDismiss = viewModel::dismissMessage)
            }
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
