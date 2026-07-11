package com.musfit.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.musfit.data.repository.AiCoachProviderKind
import com.musfit.data.repository.LocalAgentKind
import com.musfit.ui.AppDestination
import com.musfit.ui.components.MusFitSegmented
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.tabAccentFor

@Composable
fun AiCoachSettingsSection(
    state: AiCoachSettingsUiState,
    isTesting: Boolean,
    message: String?,
    onEdit: () -> Unit,
    onClearApiKey: () -> Unit,
    onTestConnection: () -> Unit,
) {
    val accent = tabAccentFor(AppDestination.Profile)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MusFitTheme.shapes.extraLarge,
        color = MusFitTheme.colors.surface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Coach connection", style = MusFitTheme.typography.titleMedium)
                    Text(
                        state.aiCoachSummary(),
                        style = MusFitTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                }
                AiCoachStatusPill(
                    label = state.providerLabel,
                    container = if (state.providerKind == AiCoachProviderKind.Disabled) {
                        MusFitTheme.colors.surfaceVariant
                    } else {
                        accent.container
                    },
                    contentColor = if (state.providerKind == AiCoachProviderKind.Disabled) {
                        MusFitTheme.colors.onSurfaceVariant
                    } else {
                        accent.onContainer
                    },
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsRow(label = "Endpoint", value = state.endpointLabel)
                HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
                SettingsRow(label = "Model", value = state.modelLabel)
                HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
                SettingsRow(label = "API key", value = state.apiKeyLabel)
            }
            if (message != null) {
                Text(
                    text = message,
                    style = MusFitTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onEdit,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 46.dp),
                    shape = MusFitTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = accent.onColor),
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = null)
                    Text("Configure", modifier = Modifier.padding(start = 8.dp))
                }
                OutlinedButton(
                    onClick = onTestConnection,
                    enabled = state.providerKind != AiCoachProviderKind.Disabled && !isTesting,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(if (isTesting) "Testing" else "Test")
                }
                if (state.hasApiKey) {
                    OutlinedButton(
                        onClick = onClearApiKey,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp),
                        shape = MusFitTheme.shapes.medium,
                    ) {
                        Text("Clear key")
                    }
                }
            }
        }
    }
}

@Composable
fun AiCoachSettingsDialog(
    provider: AiCoachProviderKind,
    baseUrl: String,
    modelName: String,
    localAgentKind: LocalAgentKind,
    apiKey: String,
    hasSavedApiKey: Boolean,
    error: String?,
    onProviderChange: (AiCoachProviderKind) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onModelNameChange: (String) -> Unit,
    onLocalAgentKindChange: (LocalAgentKind) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val accent = tabAccentFor(AppDestination.Profile)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI coach setup") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MusFitSegmented(
                    options = AiCoachProviderKind.entries,
                    selected = provider,
                    accent = accent,
                    label = { it.shortLabel() },
                    onSelect = onProviderChange,
                )
                if (provider != AiCoachProviderKind.Disabled) {
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = onBaseUrlChange,
                        label = { Text("Base URL") },
                        placeholder = { Text(AI_COACH_BASE_URL_PLACEHOLDER) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (provider == AiCoachProviderKind.LocalAgent) {
                    MusFitSegmented(
                        options = LocalAgentKind.entries,
                        selected = localAgentKind,
                        accent = accent,
                        label = { it.shortLabel() },
                        onSelect = onLocalAgentKindChange,
                    )
                }
                if (provider != AiCoachProviderKind.Disabled) {
                    OutlinedTextField(
                        value = modelName,
                        onValueChange = onModelNameChange,
                        label = {
                            Text(
                                if (provider == AiCoachProviderKind.OpenAiCompatible) {
                                    "Model"
                                } else {
                                    "Model or agent id (optional)"
                                },
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = onApiKeyChange,
                        label = { Text(apiKeyFieldLabel(provider, localAgentKind)) },
                        supportingText = {
                            Text(
                                apiKeySupportingText(
                                    provider = provider,
                                    localAgentKind = localAgentKind,
                                    hasSavedApiKey = hasSavedApiKey,
                                ),
                            )
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MusFitTheme.typography.bodySmall)
                }
                Text(
                    aiCoachSecurityNote(provider, localAgentKind),
                    style = MusFitTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SettingsRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            modifier = Modifier.weight(0.35f),
            style = MusFitTheme.typography.bodySmall,
            color = MusFitTheme.colors.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
        Text(value, modifier = Modifier.weight(0.65f), style = MusFitTheme.typography.bodySmall)
    }
}

@Composable
private fun AiCoachStatusPill(label: String, container: Color, contentColor: Color) {
    Surface(color = container, shape = MusFitTheme.shapes.small) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MusFitTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun AiCoachSettingsUiState.aiCoachSummary(): String = when (providerKind) {
    AiCoachProviderKind.Disabled -> "Coach features stay off until you choose a local agent or API endpoint."
    AiCoachProviderKind.OpenAiCompatible -> "Uses your configured API-compatible endpoint. The key stays on this device."
    AiCoachProviderKind.LocalAgent -> AI_COACH_LOCAL_AGENT_SUMMARY
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
        "Hermes requires the API_SERVER_KEY bearer token from ~/.hermes/.env. $AI_COACH_ENDPOINT_POLICY_NOTE"
    } else {
        "Keys stay encrypted on this device. $AI_COACH_ENDPOINT_POLICY_NOTE"
    }
