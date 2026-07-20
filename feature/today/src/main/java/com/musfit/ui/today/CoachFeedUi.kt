package com.musfit.ui.today

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musfit.data.repository.AiCoachChatMessage
import com.musfit.data.repository.AiCoachChatMessageStatus
import com.musfit.data.repository.AiCoachChatRole
import com.musfit.data.repository.CoachMessage
import com.musfit.domain.coach.CoachAction
import com.musfit.domain.coach.CoachMessageCategory
import com.musfit.feature.today.R
import com.musfit.ui.components.groupedShape
import com.musfit.ui.text.asString
import com.musfit.ui.theme.BrandCoral
import com.musfit.ui.theme.MusFitTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private fun currentTimeFormatter(locale: Locale): DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)

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
    val locale = LocalConfiguration.current.locales[0]
    Column(verticalArrangement = Arrangement.spacedBy(MusFitTheme.spacing.xs)) {
        groups.forEach { group ->
            Text(
                text = group.label.asString().uppercase(locale),
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

@Composable
private fun CoachMessageCard(
    message: CoachMessage,
    shape: RoundedCornerShape,
    onAction: (CoachAction) -> Unit,
    onLongPress: () -> Unit,
) {
    // White cards on the cream ground; the coral accent carries the overline,
    // the unread dot, and the tonal action pill.
    val unread = stringResource(R.string.today_unread)
    val dismissMessage = stringResource(R.string.today_dismiss_message)
    Surface(
        color = MusFitTheme.colors.surface,
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .pointerInput(onLongPress) {
                detectTapGestures(onLongPress = { onLongPress() })
            }
            .semantics(mergeDescendants = true) {
                if (!message.isRead) stateDescription = unread
                customActions = listOf(
                    CustomAccessibilityAction(label = dismissMessage) {
                        onLongPress()
                        true
                    },
                )
            },
    ) {
        Column(modifier = Modifier.padding(MusFitTheme.spacing.lg)) {
            CoachMessageHeader(message)
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
                CoachActionButton(action = action, onAction = onAction)
            }
        }
    }
}

@Composable
private fun CoachMessageHeader(message: CoachMessage) {
    val locale = LocalConfiguration.current.locales[0]
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            message.category.icon(),
            contentDescription = null, // decorative: the visible category label follows
            tint = MusFitTheme.colors.accent,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = message.category.displayLabel().uppercase(locale),
            style = MusFitTheme.typography.labelSmall,
            color = MusFitTheme.colors.accent,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = currentTimeFormatter(locale).format(
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
}

@Composable
private fun CoachActionButton(
    action: CoachAction,
    onAction: (CoachAction) -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .widthIn(min = 48.dp)
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(role = Role.Button) { onAction(action) },
    ) {
        Surface(
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

@Composable
internal fun coachActionLabel(action: CoachAction): String = when (action) {
    CoachAction.OpenFood -> stringResource(R.string.today_open_food)
    CoachAction.OpenTraining -> stringResource(R.string.today_open_training)
    CoachAction.OpenHealth -> stringResource(R.string.today_open_profile)
    is CoachAction.StartRoutine -> stringResource(R.string.today_start_workout)
}

/**
 * v1 navigation table for coach actions: every action lands on its home tab.
 * StartRoutine intentionally maps to Training for ALL routines (deleted or live) —
 * the spec's "no sub-route anchors" non-goal; see the plan's recorded deviations.
 */
internal fun coachActionDestination(action: CoachAction): TodayNavigationTarget = when (action) {
    CoachAction.OpenFood -> TodayNavigationTarget.Food
    CoachAction.OpenTraining -> TodayNavigationTarget.Training
    CoachAction.OpenHealth -> TodayNavigationTarget.Profile
    is CoachAction.StartRoutine -> TodayNavigationTarget.Training
}

@Composable
private fun CoachMessageCategory.displayLabel(): String = when (this) {
    CoachMessageCategory.Plan -> stringResource(R.string.today_category_plan)
    CoachMessageCategory.Nutrition -> stringResource(R.string.today_category_nutrition)
    CoachMessageCategory.Training -> stringResource(R.string.today_category_training)
    CoachMessageCategory.Trend -> stringResource(R.string.today_category_trend)
    CoachMessageCategory.Achievement -> stringResource(R.string.today_category_achievement)
    CoachMessageCategory.Recap -> stringResource(R.string.today_category_recap)
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
            .semantics { role = Role.Button }
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
                    contentDescription = stringResource(R.string.today_ask_coach),
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

data class TodayLocalNetworkConfig(
    val permission: String,
    val permissionDeniedMessage: String,
    val requiresPermission: (String) -> Boolean,
    val hasPermission: (Context) -> Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPreviewSheet(
    onDismiss: () -> Unit,
    onConfigure: () -> Unit,
    localNetworkConfig: TodayLocalNetworkConfig,
    viewModel: CoachChatViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
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
            viewModel.reportLocalNetworkPermissionDenied(localNetworkConfig.permissionDeniedMessage)
        }
    }
    fun runWithLocalNetworkPermission(action: () -> Unit) {
        if (localNetworkConfig.requiresPermission(state.baseUrl) && !localNetworkConfig.hasPermission(context)) {
            pendingLocalNetworkAction = action
            localNetworkPermissionLauncher.launch(localNetworkConfig.permission)
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
                        text = stringResource(R.string.today_ask_coach),
                        style = MusFitTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MusFitTheme.colors.onSurface,
                    )
                    Text(
                        text = state.providerLabel.asString(),
                        style = MusFitTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = viewModel::clearChat,
                    enabled = state.messages.isNotEmpty(),
                    modifier = Modifier.size(48.dp).semantics { role = Role.Button },
                ) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.today_clear_coach_chat))
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp).semantics { role = Role.Button },
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.today_close_coach_chat))
                }
            }

            if (!state.isConfigured) {
                CoachChatEmptyState(
                    title = stringResource(R.string.today_coach_connection_off),
                    body = stringResource(R.string.today_configure_coach_body),
                    actionLabel = stringResource(R.string.today_configure),
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
                                text = message.asString(),
                                style = MusFitTheme.typography.bodySmall,
                                color = MusFitTheme.colors.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(
                                onClick = { runWithLocalNetworkPermission(viewModel::testConnection) },
                            ) {
                                Text(stringResource(R.string.today_retry))
                            }
                        }
                    }
                }
                CoachChatComposer(
                    input = state.input,
                    isSending = state.isSending,
                    onInputChanged = viewModel::onInputChanged,
                    onSend = { runWithLocalNetworkPermission(viewModel::send) },
                )
            }
        }
    }
}

@Composable
internal fun CoachChatComposer(
    input: String,
    isSending: Boolean,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val coachMessageDescription = stringResource(R.string.today_coach_message)
    val sendDescription = stringResource(
        if (isSending) R.string.today_sending_coach_message else R.string.today_send_coach_message,
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChanged,
            placeholder = { Text(stringResource(R.string.today_ask_about_today)) },
            singleLine = false,
            minLines = 1,
            maxLines = 4,
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = coachMessageDescription },
        )
        val canSend = input.isNotBlank() && !isSending
        CoachSendButton(canSend, isSending, sendDescription, onSend)
    }
}

@Composable
private fun CoachSendButton(
    enabled: Boolean,
    isSending: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            },
    ) {
        Surface(
            color = if (enabled) MusFitTheme.colors.accent else MusFitTheme.colors.surfaceVariant,
            shape = CircleShape,
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                } else {
                    Icon(
                        Icons.AutoMirrored.Outlined.Send,
                        contentDescription = null,
                        tint = if (enabled) MusFitTheme.colors.onAccent else MusFitTheme.colors.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
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
            title = stringResource(R.string.today_coach_ready),
            body = stringResource(R.string.today_coach_ready_body),
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
                        text = stringResource(R.string.today_thinking),
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
                        message.status == AiCoachChatMessageStatus.Sending -> stringResource(R.string.today_thinking)
                        else -> message.content
                    },
                    style = MusFitTheme.typography.bodyMedium,
                )
                if (message.status == AiCoachChatMessageStatus.Failed) {
                    message.errorMessage?.let { errorMessage ->
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = errorMessage,
                            style = MusFitTheme.typography.bodySmall,
                            color = MusFitTheme.colors.onSurfaceVariant,
                        )
                    }
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
