@file:OptIn(ExperimentalMaterial3Api::class)

package com.musfit.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musfit.data.repository.AiCoachProviderKind
import com.musfit.data.repository.LocalAgentKind
import com.musfit.ui.components.InnerScreenHeader
import com.musfit.ui.components.PillButton
import com.musfit.ui.components.SheetDragHandle
import com.musfit.ui.components.groupedShape
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent

/**
 * The Turn 11 AI coach settings page (11c): connection hero with the coral
 * composite mark and a status dot, grouped value rows for the endpoint
 * details, and the on-device key note. The editor opens as a sheet.
 */
@Composable
internal fun AiCoachSettingsPage(
    state: ProfileSettingsUiState,
    accent: TabAccent,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onClearApiKey: () -> Unit,
    onTestConnection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val coach = state.aiCoach
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MusFitTheme.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        InnerScreenHeader(
            title = "AI coach",
            subtitle = "The coach runs on your own model connection",
            onBack = onBack,
        )

        AiCoachConnectionHero(
            state = state,
            accent = accent,
            onTestConnection = onTestConnection,
        )

        val message = state.aiCoachMessage
        if (message != null) {
            Text(
                message,
                style = MusFitTheme.typography.bodySmall,
                color = MusFitTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        GroupLabel(
            text = "Connection",
            actionLabel = "Edit",
            actionColor = accent.color,
            onAction = onEdit,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            CompactValueRow(title = "Provider", value = coach.providerLabel, shape = groupedShape(0, 4))
            CompactValueRow(title = "Base URL", value = coach.endpointLabel, shape = groupedShape(1, 4))
            CompactValueRow(title = "Model", value = coach.modelLabel, shape = groupedShape(2, 4))
            ApiKeyRow(
                hasApiKey = coach.hasApiKey,
                accent = accent,
                shape = groupedShape(3, 4),
                onClear = onClearApiKey,
            )
        }

        Text(
            "Your key stays on this device.",
            style = MusFitTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = MusFitTheme.colors.onSurfaceFaint,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

/** Tonal hero: white mark circle · name + status dot line · filled Test pill. */
@Composable
private fun AiCoachConnectionHero(
    state: ProfileSettingsUiState,
    accent: TabAccent,
    onTestConnection: () -> Unit,
) {
    val coach = state.aiCoach
    val disabled = coach.providerKind == AiCoachProviderKind.Disabled
    Surface(color = accent.container, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(color = MusFitTheme.colors.surface, shape = CircleShape, modifier = Modifier.size(52.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    CoachChatMark(knockoutColor = MusFitTheme.colors.surface, size = 26.dp)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    if (disabled) "Not set up" else coach.providerLabel,
                    style = MusFitTheme.typography.titleLarge.copy(fontSize = 19.sp, letterSpacing = (-0.3).sp),
                    color = accent.onContainer,
                )
                AiCoachStatusLine(state = state, accent = accent)
            }
            HeroActionPill(
                text = if (state.isAiCoachTesting) "Testing" else "Test",
                accent = accent,
                onClick = onTestConnection,
                enabled = !disabled && !state.isAiCoachTesting,
            )
        }
    }
}

/** "● Connected" — 7dp status dot + 12.5 label driven by the test lifecycle. */
@Composable
private fun AiCoachStatusLine(state: ProfileSettingsUiState, accent: TabAccent) {
    val disabled = state.aiCoach.providerKind == AiCoachProviderKind.Disabled
    val (dotColor, label) = when {
        disabled -> MusFitTheme.colors.onSurfaceFaint to "Off"
        state.aiCoachTestState == AiCoachTestState.Testing -> accent.onContainerVariant to "Testing…"
        state.aiCoachTestState == AiCoachTestState.Success -> accent.color to "Connected"
        state.aiCoachTestState == AiCoachTestState.Failure ->
            MusFitTheme.colors.onDestructiveContainer to "Not reachable"
        else -> accent.onContainerVariant to "Not tested yet"
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(7.dp).background(dotColor, CircleShape))
        Text(
            label,
            style = MusFitTheme.typography.bodySmall,
            color = accent.onContainerVariant,
        )
    }
}

@Composable
private fun ApiKeyRow(
    hasApiKey: Boolean,
    accent: TabAccent,
    shape: RoundedCornerShape,
    onClear: () -> Unit,
) {
    Surface(color = MusFitTheme.colors.surface, shape = shape, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    "API key",
                    style = MusFitTheme.typography.titleSmall.copy(fontSize = 14.5.sp),
                    color = MusFitTheme.colors.onSurface,
                )
                Text(
                    // The key is stored encrypted, so only its presence can be shown.
                    if (hasApiKey) "•••• •••• saved" else "No key",
                    style = MusFitTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }
            if (hasApiKey) {
                Text(
                    "Clear",
                    style = MusFitTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.W800),
                    color = accent.color,
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .clickable(onClickLabel = "Clear API key", onClick = onClear)
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
        }
    }
}

/**
 * The coach connection editor as a Turn 11 sheet — the same fields the old
 * `AiCoachSettingsDialog` carried: provider kind, base URL, local-agent kind,
 * model, and the write-only API key.
 */
@Composable
fun AiCoachEditorSheet(
    provider: AiCoachProviderKind,
    baseUrl: String,
    modelName: String,
    localAgentKind: LocalAgentKind,
    apiKey: String,
    hasSavedApiKey: Boolean,
    error: String?,
    accent: TabAccent,
    onProviderChange: (AiCoachProviderKind) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onModelNameChange: (String) -> Unit,
    onLocalAgentKindChange: (LocalAgentKind) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MusFitTheme.colors.background,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)) { SheetDragHandle() }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Column {
                Text(
                    "AI coach setup",
                    style = MusFitTheme.typography.headlineMedium.copy(fontSize = 22.sp, lineHeight = 25.sp),
                )
                Text(
                    "Bring your own agent or API-compatible endpoint",
                    style = MusFitTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            ConnectedSegmentRow(
                options = AiCoachProviderKind.entries,
                selected = provider,
                label = { it.shortLabel() },
                accent = accent,
                onSelect = onProviderChange,
            )

            if (provider != AiCoachProviderKind.Disabled) {
                EditorTextTile(
                    label = "Base URL",
                    value = baseUrl,
                    onValueChange = onBaseUrlChange,
                    placeholder = "http://10.0.2.2:8080/v1/",
                    keyboardType = KeyboardType.Uri,
                )
            }
            if (provider == AiCoachProviderKind.LocalAgent) {
                ConnectedSegmentRow(
                    options = LocalAgentKind.entries,
                    selected = localAgentKind,
                    label = { it.shortLabel() },
                    accent = accent,
                    onSelect = onLocalAgentKindChange,
                )
            }
            if (provider != AiCoachProviderKind.Disabled) {
                EditorTextTile(
                    label = if (provider == AiCoachProviderKind.OpenAiCompatible) {
                        "Model"
                    } else {
                        "Model or agent id (optional)"
                    },
                    value = modelName,
                    onValueChange = onModelNameChange,
                )
                EditorTextTile(
                    label = apiKeyFieldLabel(provider, localAgentKind),
                    value = apiKey,
                    onValueChange = onApiKeyChange,
                    masked = true,
                    supporting = apiKeySupportingText(
                        provider = provider,
                        localAgentKind = localAgentKind,
                        hasSavedApiKey = hasSavedApiKey,
                    ),
                )
            }
            if (error != null) {
                Text(
                    error,
                    style = MusFitTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onDestructiveContainer,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            Text(
                aiCoachSecurityNote(provider, localAgentKind),
                style = MusFitTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MusFitTheme.colors.onSurfaceFaint,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            PillButton(
                text = "Save",
                onClick = onSave,
                containerColor = accent.color,
                contentColor = accent.onColor,
                height = 50.dp,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
            Text(
                "Cancel",
                style = MusFitTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.W800),
                color = MusFitTheme.colors.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(99.dp))
                    .clickable(onClickLabel = "Cancel", onClick = onDismiss)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

/** White field tile with optional masking, placeholder, and supporting text. */
@Composable
private fun EditorTextTile(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String? = null,
    supporting: String? = null,
    masked: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(
            color = MusFitTheme.colors.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            ) {
                Text(
                    text = label,
                    style = MusFitTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.W600,
                        letterSpacing = 0.sp,
                    ),
                    color = MusFitTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                )
                Box {
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            placeholder,
                            style = MusFitTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.W500),
                            color = MusFitTheme.colors.onSurfaceFaint,
                            maxLines = 1,
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        textStyle = MusFitTheme.typography.titleSmall.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.W700,
                            color = MusFitTheme.colors.onSurface,
                        ),
                        cursorBrush = SolidColor(MusFitTheme.colors.brand),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (masked) KeyboardType.Password else keyboardType,
                        ),
                        visualTransformation = if (masked) {
                            PasswordVisualTransformation()
                        } else {
                            androidx.compose.ui.text.input.VisualTransformation.None
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        if (supporting != null) {
            Text(
                supporting,
                style = MusFitTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = MusFitTheme.colors.onSurfaceFaint,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

private fun AiCoachProviderKind.shortLabel(): String = when (this) {
    AiCoachProviderKind.Disabled -> "Off"
    AiCoachProviderKind.OpenAiCompatible -> "API"
    AiCoachProviderKind.LocalAgent -> "Agent"
}

private fun LocalAgentKind.shortLabel(): String = when (this) {
    LocalAgentKind.OpenClaw -> "OpenClaw"
    LocalAgentKind.HermesAgent -> "Hermes"
    LocalAgentKind.Custom -> "Custom"
}

private fun apiKeyFieldLabel(provider: AiCoachProviderKind, localAgentKind: LocalAgentKind): String =
    if (provider == AiCoachProviderKind.LocalAgent && localAgentKind == LocalAgentKind.HermesAgent) {
        "API_SERVER_KEY"
    } else {
        "API key (optional)"
    }

private fun apiKeySupportingText(
    provider: AiCoachProviderKind,
    localAgentKind: LocalAgentKind,
    hasSavedApiKey: Boolean,
): String {
    val isHermes = provider == AiCoachProviderKind.LocalAgent && localAgentKind == LocalAgentKind.HermesAgent
    return when {
        isHermes && hasSavedApiKey -> "Leave blank to keep the saved API_SERVER_KEY."
        isHermes -> "Paste API_SERVER_KEY from ~/.hermes/.env. Stored encrypted on this device."
        hasSavedApiKey -> "Leave blank to keep the saved key."
        else -> "Stored encrypted on this device."
    }
}

private fun aiCoachSecurityNote(provider: AiCoachProviderKind, localAgentKind: LocalAgentKind): String =
    if (provider == AiCoachProviderKind.LocalAgent && localAgentKind == LocalAgentKind.HermesAgent) {
        "MusFit keeps this local. Hermes requires the API_SERVER_KEY bearer token from ~/.hermes/.env."
    } else {
        "MusFit keeps this local. Local agents can run without a key; hosted endpoints usually need one."
    }
