package com.musfit.ui.profile

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.musfit.BuildConfig
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    onBack: () -> Unit,
    viewModel: ProfileSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showProfileDialog by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { viewModel.refreshStatus() }

    LaunchedEffect(Unit) { viewModel.refreshStatus() }

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
            ProviderSignInActions(
                googleConfigured = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank(),
                githubConfigured = state.isGitHubSignInConfigured,
                githubBusy = state.githubSignInInProgress,
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
            Button(onClick = viewModel::importToday, modifier = Modifier.fillMaxWidth()) { Text("Import today") }
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
private fun ProviderSignInActions(
    googleConfigured: Boolean,
    githubConfigured: Boolean,
    githubBusy: Boolean,
    onGoogleSignIn: () -> Unit,
    onGitHubSignIn: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onGoogleSignIn,
            modifier = Modifier.fillMaxWidth(),
            enabled = googleConfigured && !githubBusy,
        ) {
            Icon(Icons.Outlined.Link, contentDescription = null)
            Text("Continue with Google", modifier = Modifier.padding(start = 8.dp))
        }
        OutlinedButton(
            onClick = onGitHubSignIn,
            modifier = Modifier.fillMaxWidth(),
            enabled = githubConfigured && !githubBusy,
        ) {
            Icon(Icons.Outlined.Code, contentDescription = null)
            Text(
                if (githubBusy) "Waiting for GitHub" else "Continue with GitHub",
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        if (!googleConfigured || !githubConfigured) {
            Text(
                "Provider sign-in needs OAuth client IDs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
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
                Text(userCode, style = MaterialTheme.typography.headlineSmall)
                Text(verificationUri, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onOpen) { Text("Open GitHub") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
