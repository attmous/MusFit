package com.musfit.ui.today

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.musfit.data.repository.AiCoachChatMessage
import com.musfit.data.repository.AiCoachChatMessageStatus
import com.musfit.data.repository.AiCoachChatRole
import com.musfit.data.repository.CoachMessage
import com.musfit.domain.coach.CoachAction
import com.musfit.domain.coach.CoachMessageCategory
import com.musfit.ui.AppDestination
import com.musfit.ui.components.groupedShape
import com.musfit.ui.permissions.LOCAL_NETWORK_PERMISSION
import com.musfit.ui.permissions.hasLocalNetworkPermission
import com.musfit.ui.theme.BrandCoral
import com.musfit.ui.theme.MusFitTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private fun currentTimeFormatter(): DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault())

/**
 * The feed body: day-grouped coach messages, newest first, each group rendered
 * as an M3E grouped list — white cards, 4dp gaps, 24dp outer / 8dp inner corners.
 */
@Composable
fun CoachFeed(
    groups: List<CoachFeedDayGroup>,
    onAction: (CoachAction) -> Unit,
    onDismiss: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MusFitTheme.spacing.xs)) {
        groups.forEach { group ->
            Text(
                text = group.label.uppercase(),
                style = MusFitTheme.typography.labelSmall,
                color = MusFitTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(top = MusFitTheme.spacing.sm, bottom = MusFitTheme.spacing.xs),
            )
            group.messages.forEachIndexed { index, message ->
                key(message.id) {
                    DismissableMessageCard(
                        message = message,
                        shape = groupedShape(index, group.messages.size),
                        onAction = onAction,
                        onDismiss = onDismiss,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DismissableMessageCard(
    message: CoachMessage,
    shape: RoundedCornerShape,
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
            shape = shape,
            onAction = onAction,
            onLongPress = { onDismiss(message.id) }, // spec: swipe OR long-press dismisses
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun CoachMessageCard(
    message: CoachMessage,
    shape: RoundedCornerShape,
    onAction: (CoachAction) -> Unit,
    onLongPress: () -> Unit,
) {
    // White cards on the cream ground; the coral accent carries the overline,
    // the unread dot, and the tonal action pill.
    Surface(
        color = MusFitTheme.colors.surface,
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .semantics { if (!message.isRead) stateDescription = "Unread" }
            .combinedClickable(
                onClick = { message.action?.let(onAction) },
                onClickLabel = message.action?.let(::coachActionLabel),
                onLongClick = onLongPress,
                onLongClickLabel = "Dismiss message",
            ),
    ) {
        Column(modifier = Modifier.padding(MusFitTheme.spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    message.category.icon(),
                    contentDescription = null, // decorative: the visible category label follows
                    tint = MusFitTheme.colors.accent,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = message.category.displayLabel().uppercase(),
                    style = MusFitTheme.typography.labelSmall,
                    color = MusFitTheme.colors.accent,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = currentTimeFormatter().format(
                        Instant.ofEpochMilli(message.firstSeenAtEpochMillis).atZone(ZoneId.systemDefault()),
                    ),
                    style = MusFitTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MusFitTheme.colors.onSurfaceFaint,
                )
                if (!message.isRead) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(MusFitTheme.colors.accent),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = message.title,
                style = MusFitTheme.typography.titleLarge,
                color = MusFitTheme.colors.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = message.body,
                style = MusFitTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
            message.action?.let { action ->
                Spacer(Modifier.height(MusFitTheme.spacing.md))
                Surface(
                    onClick = { onAction(action) },
                    color = MusFitTheme.colors.accentContainer,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text(
                        text = coachActionLabel(action),
                        style = MusFitTheme.typography.labelMedium,
                        color = MusFitTheme.colors.onAccentContainer,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
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

/**
 * The coach button: a compact 58dp rounded-square FAB (Material 3 Expressive),
 * docked inline at the right of the floating nav bar. Brand coral is the one
 * global accent — the coach never recolors per tab. Opens the coach chat sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPreviewFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(22.dp)
    Surface(
        onClick = onClick,
        color = BrandCoral,
        contentColor = Color.White,
        shape = shape,
        modifier = modifier
            .size(58.dp)
            .shadow(
                elevation = 10.dp,
                shape = shape,
                spotColor = BrandCoral,
                ambientColor = BrandCoral,
            ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Turn 8 composite mark: a filled chat bubble with an auto_awesome
            // sparkle knocked out in the FAB's own fill near the bubble top.
            Box(modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Filled.ChatBubble,
                    contentDescription = "Ask coach",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = BrandCoral,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 6.dp)
                        .size(13.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPreviewSheet(
    onDismiss: () -> Unit,
    onConfigure: () -> Unit,
    viewModel: CoachChatViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var pendingLocalNetworkAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val localNetworkPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val action = pendingLocalNetworkAction
        pendingLocalNetworkAction = null
        if (granted && action != null) {
            action()
        } else if (!granted) {
            viewModel.reportLocalNetworkPermissionDenied()
        }
    }
    fun runWithLocalNetworkPermission(action: () -> Unit) {
        if (state.requiresLocalNetworkPermission && !hasLocalNetworkPermission(context)) {
            pendingLocalNetworkAction = action
            localNetworkPermissionLauncher.launch(LOCAL_NETWORK_PERMISSION)
        } else {
            action()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MusFitTheme.colors.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(MusFitTheme.spacing.md),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(color = MusFitTheme.colors.accent.copy(alpha = 0.12f), shape = CircleShape) {
                    Icon(
                        Icons.Outlined.Forum,
                        contentDescription = null,
                        tint = MusFitTheme.colors.accent,
                        modifier = Modifier.padding(8.dp).size(22.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ask coach",
                        style = MusFitTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MusFitTheme.colors.onSurface,
                    )
                    Text(
                        text = state.providerLabel,
                        style = MusFitTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                }
                IconButton(onClick = viewModel::clearChat, enabled = state.messages.isNotEmpty()) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Clear coach chat")
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close coach chat")
                }
            }

            if (!state.isConfigured) {
                CoachChatEmptyState(
                    title = "Coach connection is off",
                    body = "Choose Hermes or another endpoint in Profile settings.",
                    actionLabel = "Configure",
                    onAction = {
                        onDismiss()
                        onConfigure()
                    },
                )
            } else {
                CoachChatMessages(
                    messages = state.messages,
                    isSending = state.isSending,
                    modifier = Modifier.heightIn(min = 220.dp, max = 420.dp),
                )
                state.errorMessage?.let { message ->
                    Surface(color = MusFitTheme.colors.surfaceVariant, shape = MusFitTheme.shapes.small) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = message,
                                style = MusFitTheme.typography.bodySmall,
                                color = MusFitTheme.colors.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(
                                onClick = { runWithLocalNetworkPermission(viewModel::testConnection) },
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.input,
                        onValueChange = viewModel::onInputChanged,
                        placeholder = { Text("Ask about today") },
                        singleLine = false,
                        minLines = 1,
                        maxLines = 4,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { runWithLocalNetworkPermission(viewModel::send) },
                        enabled = state.input.isNotBlank() && !state.isSending,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (state.input.isNotBlank() && !state.isSending) {
                                MusFitTheme.colors.accent
                            } else {
                                MusFitTheme.colors.surfaceVariant
                            },
                        ),
                    ) {
                        if (state.isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = MusFitTheme.colors.onSurfaceVariant,
                            )
                        } else {
                            Icon(
                                Icons.AutoMirrored.Outlined.Send,
                                contentDescription = "Send coach message",
                                tint = if (state.input.isNotBlank()) MusFitTheme.colors.onAccent else MusFitTheme.colors.onSurfaceVariant,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoachChatMessages(
    messages: List<AiCoachChatMessage>,
    isSending: Boolean,
    modifier: Modifier = Modifier,
) {
    if (messages.isEmpty()) {
        CoachChatEmptyState(
            title = "Coach is ready",
            body = "Ask about today's food, hydration, training, or recovery.",
            actionLabel = null,
            onAction = {},
            modifier = modifier,
        )
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(messages, key = { it.id }) { message ->
            CoachChatBubble(message)
        }
        if (isSending && messages.none { it.status == AiCoachChatMessageStatus.Sending }) {
            item {
                Surface(color = MusFitTheme.colors.surfaceVariant, shape = MusFitTheme.shapes.medium) {
                    Text(
                        text = "Thinking...",
                        style = MusFitTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CoachChatBubble(message: AiCoachChatMessage) {
    val isUser = message.role == AiCoachChatRole.User
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = if (isUser) MusFitTheme.colors.accent else MusFitTheme.colors.surfaceVariant,
            contentColor = if (isUser) MusFitTheme.colors.onAccent else MusFitTheme.colors.onSurface,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp,
            ),
            modifier = Modifier.fillMaxWidth(0.86f),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = when {
                        message.status == AiCoachChatMessageStatus.Sending -> "Thinking..."
                        else -> message.content
                    },
                    style = MusFitTheme.typography.bodyMedium,
                )
                if (message.status == AiCoachChatMessageStatus.Failed && message.errorMessage != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = message.errorMessage,
                        style = MusFitTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CoachChatEmptyState(
    title: String,
    body: String,
    actionLabel: String?,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Outlined.ChatBubbleOutline,
            contentDescription = null,
            tint = MusFitTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(30.dp),
        )
        Text(title, style = MusFitTheme.typography.titleSmall, color = MusFitTheme.colors.onSurface)
        Text(
            text = body,
            style = MusFitTheme.typography.bodyMedium,
            color = MusFitTheme.colors.onSurfaceVariant,
        )
        if (actionLabel != null) {
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}
