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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.musfit.ui.theme.tabAccentFor

@Composable
fun AiCoachSettingsSection(
    state: AiCoachSettingsUiState,
    onEdit: () -> Unit,
    onClearApiKey: () -> Unit,
) {
    val accent = tabAccentFor(AppDestination.Profile)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Coach connection", style = MaterialTheme.typography.titleMedium)
                    Text(
                        state.aiCoachSummary(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AiCoachStatusPill(
                    label = state.providerLabel,
                    container = if (state.providerKind == AiCoachProviderKind.Disabled) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        accent.container
                    },
                    contentColor = if (state.providerKind == AiCoachProviderKind.Disabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        accent.onContainer
                    },
                )
            }
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SettingsRow(label = "Endpoint", value = state.endpointLabel)
                    SettingsRow(label = "Model", value = state.modelLabel)
                    SettingsRow(label = "API key", value = state.apiKeyLabel)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = accent.onColor),
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = null)
                    Text("Configure", modifier = Modifier.padding(start = 8.dp))
                }
                if (state.hasApiKey) {
                    OutlinedButton(onClick = onClearApiKey, modifier = Modifier.weight(1f)) {
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
                        placeholder = { Text("http://10.0.2.2:11434") },
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
                        label = { Text("API key (optional)") },
                        supportingText = {
                            Text(
                                if (hasSavedApiKey) {
                                    "Leave blank to keep the saved key."
                                } else {
                                    "Stored encrypted on this device."
                                },
                            )
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "MusFit keeps this local. Local agents can run without a key; hosted endpoints usually need one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
        Text(value, modifier = Modifier.weight(0.65f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AiCoachStatusPill(label: String, container: Color, contentColor: Color) {
    Surface(color = container, shape = MaterialTheme.shapes.small) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun AiCoachSettingsUiState.aiCoachSummary(): String = when (providerKind) {
    AiCoachProviderKind.Disabled -> "Coach features stay off until you choose a local agent or API endpoint."
    AiCoachProviderKind.OpenAiCompatible -> "Uses your configured API-compatible endpoint. The key stays on this device."
    AiCoachProviderKind.LocalAgent -> "Connects to a local agent running on this device or your network."
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
