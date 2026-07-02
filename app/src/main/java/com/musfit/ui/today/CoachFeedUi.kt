package com.musfit.ui.today

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.musfit.data.repository.CoachMessage
import com.musfit.domain.coach.CoachAction
import com.musfit.domain.coach.CoachMessageCategory
import com.musfit.ui.AppDestination
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.tabAccentFor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private val TIME_FORMATTER = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault())

/** The feed body: day-grouped coach messages, newest first. */
@Composable
fun CoachFeed(
    groups: List<CoachFeedDayGroup>,
    onAction: (CoachAction) -> Unit,
    onDismiss: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MusFitTheme.spacing.sm)) {
        groups.forEach { group ->
            Text(
                text = group.label.uppercase(),
                style = MusFitTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MusFitTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(top = MusFitTheme.spacing.sm),
            )
            group.messages.forEach { message ->
                key(message.id) {
                    DismissableMessageCard(message = message, onAction = onAction, onDismiss = onDismiss)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DismissableMessageCard(
    message: CoachMessage,
    onAction: (CoachAction) -> Unit,
    onDismiss: (Long) -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {},
        onDismiss = { onDismiss(message.id) },
    ) {
        CoachMessageCard(
            message = message,
            onAction = onAction,
            onLongPress = { onDismiss(message.id) }, // spec: swipe OR long-press dismisses
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun CoachMessageCard(
    message: CoachMessage,
    onAction: (CoachAction) -> Unit,
    onLongPress: () -> Unit,
) {
    val accent = tabAccentFor(AppDestination.Today)
    Surface(
        color = MusFitTheme.colors.surface,
        shape = MusFitTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MusFitTheme.shapes.medium)
            .semantics { if (!message.isRead) stateDescription = "Unread" }
            .combinedClickable(
                onClick = { message.action?.let(onAction) },
                onClickLabel = message.action?.let(::coachActionLabel),
                onLongClick = onLongPress,
                onLongClickLabel = "Dismiss message",
            ),
    ) {
        Column(modifier = Modifier.padding(MusFitTheme.spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    message.category.icon(),
                    contentDescription = null, // decorative: the visible category label follows
                    tint = accent.color,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = message.category.displayLabel().uppercase(),
                    style = MusFitTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = accent.color,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = TIME_FORMATTER.format(
                        Instant.ofEpochMilli(message.firstSeenAtEpochMillis).atZone(ZoneId.systemDefault()),
                    ),
                    style = MusFitTheme.typography.labelSmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
                if (!message.isRead) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(accent.color),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = message.title,
                style = MusFitTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MusFitTheme.colors.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = message.body,
                style = MusFitTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
            message.action?.let { action ->
                Spacer(Modifier.height(MusFitTheme.spacing.sm))
                Surface(
                    onClick = { onAction(action) },
                    color = accent.container,
                    shape = MusFitTheme.shapes.small,
                ) {
                    Text(
                        text = coachActionLabel(action),
                        style = MusFitTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = accent.onContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

internal fun coachActionLabel(action: CoachAction): String = when (action) {
    CoachAction.OpenFood -> "Open Food"
    CoachAction.OpenTraining -> "Open Training"
    CoachAction.OpenHealth -> "Open Profile"
    is CoachAction.StartRoutine -> "Start workout"
}

/**
 * v1 navigation table for coach actions: every action lands on its home tab.
 * StartRoutine intentionally maps to Training for ALL routines (deleted or live) —
 * the spec's "no sub-route anchors" non-goal; see the plan's recorded deviations.
 */
internal fun coachActionDestination(action: CoachAction): AppDestination = when (action) {
    CoachAction.OpenFood -> AppDestination.Food
    CoachAction.OpenTraining -> AppDestination.Training
    CoachAction.OpenHealth -> AppDestination.Profile
    is CoachAction.StartRoutine -> AppDestination.Training
}

private fun CoachMessageCategory.displayLabel(): String = when (this) {
    CoachMessageCategory.Plan -> "Plan"
    CoachMessageCategory.Nutrition -> "Nutrition"
    CoachMessageCategory.Training -> "Training"
    CoachMessageCategory.Trend -> "Trend"
    CoachMessageCategory.Achievement -> "Achievement"
    CoachMessageCategory.Recap -> "Recap"
}

private fun CoachMessageCategory.icon(): ImageVector = when (this) {
    CoachMessageCategory.Plan -> Icons.Outlined.WbSunny
    CoachMessageCategory.Nutrition -> Icons.Outlined.Restaurant
    CoachMessageCategory.Training -> Icons.Outlined.FitnessCenter
    CoachMessageCategory.Trend -> Icons.AutoMirrored.Outlined.TrendingUp
    CoachMessageCategory.Achievement -> Icons.Outlined.EmojiEvents
    CoachMessageCategory.Recap -> Icons.Outlined.NightsStay
}

/** Coach chat button in the bottom bar — opens the "coming soon" preview sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPreviewFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = tabAccentFor(AppDestination.Today)
    Surface(
        onClick = onClick,
        color = accent.color,
        shape = MusFitTheme.shapes.medium,
        shadowElevation = 4.dp,
        modifier = modifier,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Outlined.ChatBubbleOutline,
                contentDescription = "Coach chat (coming soon)",
                tint = accent.onColor,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPreviewSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MusFitTheme.colors.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                tint = MusFitTheme.colors.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.height(MusFitTheme.spacing.md))
            Text(
                text = "Coach chat is coming",
                style = MusFitTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MusFitTheme.colors.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Your coach will answer questions right here.",
                style = MusFitTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
        }
    }
}
