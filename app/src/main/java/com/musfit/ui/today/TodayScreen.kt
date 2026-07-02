package com.musfit.ui.today

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.tabAccentFor

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
