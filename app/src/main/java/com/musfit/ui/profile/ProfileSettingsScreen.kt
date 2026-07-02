package com.musfit.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    onBack: () -> Unit,
    viewModel: ProfileSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showProfileDialog by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { viewModel.refreshStatus() }

    LaunchedEffect(Unit) {
        viewModel.refreshStatus()
        viewModel.syncRecentHealthData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Account", style = MaterialTheme.typography.titleMedium)
            AccountSection(account = state.account, onEdit = viewModel::openAccountEditor)
            TextButton(onClick = { showProfileDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Profile details")
            }

            Text("Health Connect & sync", style = MaterialTheme.typography.titleMedium)
            Text("Health Connect: ${state.availabilityLabel}", style = MaterialTheme.typography.bodyMedium)
            Text(state.message, style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = {
                    if (state.canRequestPermissions) permissionLauncher.launch(state.requestablePermissions)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canRequestPermissions,
            ) { Text("Enable Health Connect sync") }
            Button(onClick = viewModel::refreshStatus, modifier = Modifier.fillMaxWidth()) { Text("Refresh status") }
            Button(
                onClick = viewModel::syncRecentHealthData,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isHealthConnectSyncing,
            ) {
                Icon(Icons.Outlined.Sync, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (state.isHealthConnectSyncing) "Syncing Health Connect..." else "Sync Health Connect data")
            }
            Button(onClick = viewModel::exportLatestWorkout, modifier = Modifier.fillMaxWidth()) {
                Text("Export latest workout")
            }

            Text("Preferences", style = MaterialTheme.typography.titleMedium)
            Text("Units: Metric (kg, cm) · Later", style = MaterialTheme.typography.bodyMedium)
            Text("Theme: System · Later", style = MaterialTheme.typography.bodyMedium)

            Text("About", style = MaterialTheme.typography.titleMedium)
            Text("MusFit", style = MaterialTheme.typography.bodyMedium)
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
}
