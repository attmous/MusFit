package com.musfit.ui.health

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun HealthScreen(viewModel: HealthViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) {
        viewModel.refreshStatus()
    }

    LaunchedEffect(Unit) {
        viewModel.refreshStatus()
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Health",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "Health Connect: ${state.availabilityLabel}",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = buildPermissionText(
                grantedPermissionCount = state.grantedPermissionCount,
                requestablePermissionCount = state.requestablePermissionCount,
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Reads: steps, body weight, active calories, and resting heart rate when granted.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Writes: workouts logged in MusFit.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = state.message,
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = {
                if (state.canRequestPermissions) {
                    permissionLauncher.launch(state.requestablePermissions)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.canRequestPermissions,
        ) {
            Text(text = "Enable Health Connect sync")
        }
        Button(
            onClick = viewModel::refreshStatus,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "Refresh status")
        }
    }
}

private fun buildPermissionText(
    grantedPermissionCount: Int,
    requestablePermissionCount: Int,
): String = if (requestablePermissionCount > 0) {
    "Granted permissions: $grantedPermissionCount of $requestablePermissionCount"
} else {
    "Granted permissions: $grantedPermissionCount"
}
