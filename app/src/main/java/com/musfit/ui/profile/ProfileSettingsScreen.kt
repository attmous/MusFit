package com.musfit.ui.profile

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.musfit.BuildConfig
import com.musfit.domain.health.StepSource
import com.musfit.ui.AppDestination
import com.musfit.ui.components.SectionHeader
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import com.musfit.ui.theme.tabAccentFor
import kotlinx.coroutines.launch

@Composable
fun ProfileSettingsScreen(
    onBack: () -> Unit,
    viewModel: ProfileSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val accent = tabAccentFor(AppDestination.Profile)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showProfileDialog by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { viewModel.refreshStatus() }
    val signInActions = providerSignInActions(
        googleConfigured = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank(),
        githubConfigured = state.isGitHubSignInConfigured,
        githubBusy = state.githubSignInInProgress,
    )

    LaunchedEffect(Unit) {
        viewModel.refreshStatus()
        viewModel.syncRecentHealthData()
    }

    Scaffold(
        containerColor = MusFitTheme.colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MusFitTheme.colors.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsHeader(onBack = onBack)

            SectionHeader(title = "Account")
            AccountSection(account = state.account, onEdit = viewModel::openAccountEditor)
            ProviderSignInActions(
                actions = signInActions,
                accent = accent,
                onGoogleSignIn = {
                    coroutineScope.launch {
                        runCatching {
                            signInWithGoogle(
                                context = context,
                                googleWebClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
                            )
                        }.onSuccess(viewModel::signInWithProvider)
                            .onFailure { viewModel.reportExternalSignInFailure("Google", it) }
                    }
                },
                onGitHubSignIn = viewModel::signInWithGitHub,
            )
            ProfileDetailsCard(accent = accent, onOpen = { showProfileDialog = true })

            SectionHeader(title = "AI coach")
            AiCoachSettingsSection(
                state = state.aiCoach,
                isTesting = state.isAiCoachTesting,
                onEdit = viewModel::openAiCoachEditor,
                onClearApiKey = viewModel::clearAiCoachApiKey,
                onTestConnection = viewModel::testAiCoachConnection,
            )

            SectionHeader(title = "Health Connect")
            HealthConnectSettingsCard(
                state = state,
                accent = accent,
                onRequestPermissions = {
                    if (state.canRequestPermissions) permissionLauncher.launch(state.requestablePermissions)
                },
                onRefresh = viewModel::refreshStatus,
                onSync = viewModel::syncRecentHealthData,
                onExport = viewModel::exportLatestWorkout,
                onOpenStepSourcePicker = viewModel::openStepSourcePicker,
                onSelectStepSource = viewModel::selectStepSource,
                onDismissStepSourcePicker = viewModel::dismissStepSourcePicker,
            )

            SectionHeader(title = "Preferences")
            SettingsCard {
                SettingsToggleRow(
                    title = "Add burned calories to budget",
                    detail = "Adds Health Connect burned calories to your daily allowance, so kcal left = goal " +
                        "− eaten + burned. It currently counts total burned calories (not just active), so " +
                        "turn it off if the numbers look too generous.",
                    checked = state.includeBurnedCalories,
                    onCheckedChange = viewModel::setIncludeBurnedCalories,
                )
            }
            SettingsInfoCard {
                SettingsValueRow(title = "Units", value = "Metric", detail = "Food, body weight, and measurements use kg and cm.")
                SettingsValueRow(title = "Theme", value = "System", detail = "Follows Android light and dark mode.")
                SettingsValueRow(title = "Data", value = "Local first", detail = "MusFit stores app data on this device.")
            }

            SectionHeader(title = "About")
            SettingsInfoCard {
                SettingsValueRow(title = "App", value = "MusFit", detail = "Fitness, nutrition, and Health Connect tracking.")
            }
        }
    }

    if (showProfileDialog) {
        ProfileEditDialog(
            initial = state.profile,
            initialWeightKg = state.latestWeightKg,
            onDismiss = { showProfileDialog = false },
            onSave = { profile, weightKg ->
                viewModel.saveProfile(profile)
                weightKg?.let(viewModel::logWeight)
                showProfileDialog = false
            },
        )
    }
    if (state.accountEditorOpen) {
        AccountEditDialog(
            name = state.accountNameInput,
            email = state.accountEmailInput,
            error = state.accountErrorMessage,
            onNameChange = viewModel::onAccountNameChanged,
            onEmailChange = viewModel::onAccountEmailChanged,
            onDismiss = viewModel::closeAccountEditor,
            onSave = viewModel::saveAccount,
        )
    }
    if (state.aiCoachEditorOpen) {
        AiCoachSettingsDialog(
            provider = state.aiCoachProviderInput,
            baseUrl = state.aiCoachBaseUrlInput,
            modelName = state.aiCoachModelNameInput,
            localAgentKind = state.aiCoachLocalAgentInput,
            apiKey = state.aiCoachApiKeyInput,
            hasSavedApiKey = state.aiCoach.hasApiKey,
            error = state.aiCoachErrorMessage,
            onProviderChange = viewModel::onAiCoachProviderChanged,
            onBaseUrlChange = viewModel::onAiCoachBaseUrlChanged,
            onModelNameChange = viewModel::onAiCoachModelNameChanged,
            onLocalAgentKindChange = viewModel::onAiCoachLocalAgentKindChanged,
            onApiKeyChange = viewModel::onAiCoachApiKeyChanged,
            onDismiss = viewModel::closeAiCoachEditor,
            onSave = viewModel::saveAiCoachSettings,
        )
    }
    val githubDeviceCode = state.githubDeviceCode
    if (githubDeviceCode != null && state.githubSignInInProgress) {
        GitHubDeviceCodeDialog(
            userCode = githubDeviceCode.userCode,
            verificationUri = githubDeviceCode.verificationUri,
            onOpen = {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(githubDeviceCode.verificationUri)),
                    )
                }.onFailure { viewModel.reportExternalSignInFailure("GitHub", it) }
            },
            onDismiss = viewModel::dismissGitHubDeviceCode,
        )
    }
}

@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Settings",
                style = MusFitTheme.typography.headlineMedium,
                color = MusFitTheme.colors.onSurface,
            )
            Text(
                "Profile, identity, coach, and device sync.",
                style = MusFitTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProviderSignInActions(
    actions: ProviderSignInActionsUiState,
    accent: TabAccent,
    onGoogleSignIn: () -> Unit,
    onGitHubSignIn: () -> Unit,
) {
    SettingsCard {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("External identity", style = MusFitTheme.typography.titleMedium)
            Text(
                "Optional account linking. It does not enable cloud sync.",
                style = MusFitTheme.typography.bodySmall,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
        }
        ProviderActionRow(
            action = actions.google,
            icon = Icons.Outlined.Link,
            accent = accent,
            onClick = onGoogleSignIn,
        )
        HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
        ProviderActionRow(
            action = actions.github,
            icon = Icons.Outlined.Code,
            accent = accent,
            onClick = onGitHubSignIn,
        )
    }
}

@Composable
private fun ProviderActionRow(
    action: ProviderSignInActionUiState,
    icon: ImageVector,
    accent: TabAccent,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconWell(
                icon = icon,
                tint = if (action.enabled) accent.color else MusFitTheme.colors.onSurfaceVariant,
                container = if (action.enabled) accent.container else MusFitTheme.colors.surfaceVariant,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(action.providerLabel, style = MusFitTheme.typography.titleSmall)
                    StatusPill(
                        label = action.statusLabel,
                        container = statusContainer(action.statusLabel, accent),
                        contentColor = statusContentColor(action.statusLabel, accent),
                    )
                }
                Text(
                    action.supportingText,
                    style = MusFitTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }
        }
        OutlinedButton(
            onClick = onClick,
            enabled = action.enabled,
            modifier = Modifier
                .align(Alignment.End)
                .heightIn(min = 44.dp)
                .widthIn(min = 180.dp),
            shape = MusFitTheme.shapes.medium,
        ) {
            Text(action.buttonLabel)
        }
    }
}

@Composable
private fun ProfileDetailsCard(accent: TabAccent, onOpen: () -> Unit) {
    SettingsCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconWell(icon = Icons.Outlined.Person, tint = accent.color, container = accent.container)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Profile details", style = MusFitTheme.typography.titleMedium)
                Text(
                    "Age, height, goal, pace, and latest body weight.",
                    style = MusFitTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }
            TextButton(onClick = onOpen, shape = MusFitTheme.shapes.small) {
                Text("Edit", color = accent.color, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun HealthConnectSettingsCard(
    state: ProfileSettingsUiState,
    accent: TabAccent,
    onRequestPermissions: () -> Unit,
    onRefresh: () -> Unit,
    onSync: () -> Unit,
    onExport: () -> Unit,
    onOpenStepSourcePicker: () -> Unit,
    onSelectStepSource: (String?) -> Unit,
    onDismissStepSourcePicker: () -> Unit,
) {
    SettingsCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconWell(icon = Icons.Outlined.FavoriteBorder, tint = accent.color, container = accent.container)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Health Connect sync", style = MusFitTheme.typography.titleMedium)
                Text(
                    permissionSummary(state),
                    style = MusFitTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }
            StatusPill(
                label = healthAvailabilityPillLabel(state.availabilityLabel),
                container = if (state.availabilityLabel == "Available") accent.container else MusFitTheme.colors.surfaceVariant,
                contentColor = if (state.availabilityLabel == "Available") accent.onContainer else MusFitTheme.colors.onSurfaceVariant,
            )
        }
        Text(
            state.message,
            style = MusFitTheme.typography.bodySmall,
            color = MusFitTheme.colors.onSurfaceVariant,
        )
        Button(
            onClick = onRequestPermissions,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 46.dp),
            enabled = state.canRequestPermissions,
            shape = MusFitTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = accent.onColor),
        ) {
            Text(healthConnectPrimaryActionLabel(state))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onRefresh,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp),
                shape = MusFitTheme.shapes.medium,
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Refresh", modifier = Modifier.padding(start = 6.dp))
            }
            OutlinedButton(
                onClick = onSync,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp),
                enabled = !state.isHealthConnectSyncing,
                shape = MusFitTheme.shapes.medium,
            ) {
                Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    if (state.isHealthConnectSyncing) "Syncing" else "Sync",
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
        OutlinedButton(
            onClick = onExport,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp),
            shape = MusFitTheme.shapes.medium,
        ) {
            Text("Export latest workout")
        }
        HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Steps source", style = MusFitTheme.typography.titleSmall)
                Text(
                    state.stepSourceLabel,
                    style = MusFitTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
                Text(
                    "Pick one app to match its step count, or keep the Health Connect combined total.",
                    style = MusFitTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }
            OutlinedButton(
                onClick = onOpenStepSourcePicker,
                enabled = state.availabilityLabel == "Available",
                shape = MusFitTheme.shapes.medium,
                modifier = Modifier.heightIn(min = 44.dp),
            ) {
                Text("Change")
            }
        }
    }

    if (state.stepSourcePickerOpen) {
        StepSourcePickerDialog(
            sources = state.stepSources,
            selectedPackage = state.preferredStepsPackage,
            onSelect = onSelectStepSource,
            onDismiss = onDismissStepSourcePicker,
        )
    }
}

@Composable
private fun StepSourcePickerDialog(
    sources: List<StepSource>,
    selectedPackage: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = { Text("Steps source") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "MusFit reads Health Connect's combined step total, which can read higher than a " +
                        "single app because it merges every source. Pick one source to mirror it exactly.",
                    style = MusFitTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
                StepSourceOption(
                    label = "All sources (unified)",
                    detail = "Health Connect combined total",
                    selected = selectedPackage == null,
                    onClick = { onSelect(null) },
                )
                sources.forEach { source ->
                    StepSourceOption(
                        label = source.label,
                        detail = "${source.steps} steps today",
                        selected = selectedPackage == source.packageName,
                        onClick = { onSelect(source.packageName) },
                    )
                }
                if (sources.isEmpty()) {
                    Text(
                        "No step sources found for today yet. Sync or walk a little, then reopen.",
                        style = MusFitTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                }
            }
        },
    )
}

@Composable
private fun StepSourceOption(
    label: String,
    detail: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MusFitTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(label, style = MusFitTheme.typography.bodyMedium)
            Text(
                detail,
                style = MusFitTheme.typography.bodySmall,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsInfoCard(content: @Composable ColumnScope.() -> Unit) {
    SettingsCard(content = content)
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Flat group on the pure surface — no border, no elevation, no card chrome;
    // whitespace and section headers do the separating.
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MusFitTheme.shapes.extraLarge,
        color = MusFitTheme.colors.surface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsValueRow(title: String, value: String, detail: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconWell(
            icon = Icons.Outlined.Info,
            tint = MusFitTheme.colors.onSurfaceVariant,
            container = MusFitTheme.colors.surfaceVariant,
            size = 34,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = MusFitTheme.typography.titleSmall)
                Text(
                    value,
                    style = MusFitTheme.typography.labelLarge,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )
            }
            Text(detail, style = MusFitTheme.typography.bodySmall, color = MusFitTheme.colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    detail: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconWell(
            icon = Icons.Outlined.FavoriteBorder,
            tint = MusFitTheme.colors.onSurfaceVariant,
            container = MusFitTheme.colors.surfaceVariant,
            size = 34,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MusFitTheme.typography.titleSmall)
            Text(detail, style = MusFitTheme.typography.bodySmall, color = MusFitTheme.colors.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun IconWell(
    icon: ImageVector,
    tint: Color,
    container: Color,
    size: Int = 38,
) {
    Surface(color = container, shape = MusFitTheme.shapes.small) {
        Box(modifier = Modifier.size(size.dp), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size((size / 2).dp))
        }
    }
}

@Composable
private fun StatusPill(label: String, container: Color, contentColor: Color) {
    Surface(color = container, shape = MusFitTheme.shapes.small) {
        Text(
            label,
            style = MusFitTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun statusContainer(statusLabel: String, accent: TabAccent): Color =
    when (statusLabel) {
        "Ready" -> accent.container
        "In progress" -> accent.container
        else -> MusFitTheme.colors.surfaceVariant
    }

@Composable
private fun statusContentColor(statusLabel: String, accent: TabAccent): Color =
    when (statusLabel) {
        "Ready" -> accent.onContainer
        "In progress" -> accent.onContainer
        else -> MusFitTheme.colors.onSurfaceVariant
    }

private fun permissionSummary(state: ProfileSettingsUiState): String =
    if (state.requestablePermissionCount == 0) {
        "Refresh to check available health permissions."
    } else {
        "${state.grantedPermissionCount}/${state.requestablePermissionCount} permissions granted"
    }

private fun healthConnectPrimaryActionLabel(state: ProfileSettingsUiState): String =
    when {
        state.canRequestPermissions && state.grantedPermissionCount > 0 -> "Review permissions"
        state.canRequestPermissions -> "Enable Health Connect"
        state.availabilityLabel == "Available" &&
            state.requestablePermissionCount > 0 &&
            state.grantedPermissionCount >= state.requestablePermissionCount -> "Permissions granted"
        state.availabilityLabel == "Unknown" -> "Refresh status first"
        else -> "Health Connect unavailable"
    }

private fun healthAvailabilityPillLabel(availabilityLabel: String): String =
    when (availabilityLabel) {
        "Install or update required" -> "Needs app"
        else -> availabilityLabel
    }

@Composable
private fun GitHubDeviceCodeDialog(
    userCode: String,
    verificationUri: String,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("GitHub sign-in") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter this code at GitHub:")
                Text(userCode, style = MusFitTheme.typography.headlineSmall)
                Text(verificationUri, style = MusFitTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onOpen) { Text("Open GitHub") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
