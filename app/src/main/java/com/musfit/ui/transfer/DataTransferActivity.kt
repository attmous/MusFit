package com.musfit.ui.transfer

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.musfit.BuildConfig
import com.musfit.configureMusFitEdgeToEdge
import com.musfit.data.transfer.DataTransferArchiveCodec
import com.musfit.ui.components.InnerScreenHeader
import com.musfit.ui.theme.MusFitTheme
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate

@AndroidEntryPoint
class DataTransferActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureMusFitEdgeToEdge()
        setContent {
            MusFitTheme {
                DataTransferRoute(onBack = ::finish)
            }
        }
    }
}

@Composable
private fun DataTransferRoute(
    onBack: () -> Unit,
    viewModel: DataTransferViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val bridgeMode = BuildConfig.DATA_TRANSFER_MODE == "legacy-export"
    var exportDialog by remember { mutableStateOf(false) }
    var exportPassphrase by remember { mutableStateOf<String?>(null) }
    var importUri by remember { mutableStateOf<Uri?>(null) }
    var importDialog by remember { mutableStateOf(false) }

    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.musfit.backup"),
    ) { uri ->
        val passphrase = exportPassphrase
        exportPassphrase = null
        if (uri != null && passphrase != null) viewModel.export(uri, passphrase)
    }
    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            importUri = uri
            importDialog = true
        }
    }

    DataTransferScreen(
        state = state,
        bridgeMode = bridgeMode,
        onBack = onBack,
        onExport = { exportDialog = true },
        onImport = { openDocument.launch(arrayOf("application/vnd.musfit.backup", "application/octet-stream")) },
    )

    if (exportDialog) {
        PassphraseDialog(
            title = "Protect this backup",
            confirmPassphrase = true,
            onDismiss = { exportDialog = false },
            onConfirm = { passphrase ->
                exportDialog = false
                exportPassphrase = passphrase
                createDocument.launch("musfit-backup-${LocalDate.now()}.musfitbackup")
            },
        )
    }
    if (importDialog) {
        PassphraseDialog(
            title = "Unlock MusFit backup",
            confirmPassphrase = false,
            onDismiss = {
                importDialog = false
                importUri = null
            },
            onConfirm = { passphrase ->
                importDialog = false
                importUri?.let { viewModel.stageImport(it, passphrase) }
                importUri = null
            },
        )
    }
}

@Composable
private fun DataTransferScreen(
    state: DataTransferUiState,
    bridgeMode: Boolean,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MusFitTheme.colors.background)
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        InnerScreenHeader(
            title = if (bridgeMode) "Move MusFit data" else "Data transfer",
            subtitle = if (bridgeMode) "Step 1 of 3 · export before reinstalling" else "Encrypted, local, and user-controlled",
            onBack = onBack,
        )
        Surface(
            color = MusFitTheme.colors.surfaceVariant,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(30.dp))
                Text(
                    if (bridgeMode) "Preserve your existing data" else "Your backup stays yours",
                    style = MusFitTheme.typography.headlineSmall,
                    fontWeight = FontWeight.W800,
                )
                Text(
                    if (bridgeMode) {
                        "Create an encrypted backup, uninstall this legacy build, install the production app, then import the file."
                    } else {
                        "MusFit encrypts the database with your passphrase. API keys and Android permissions are never included."
                    },
                    style = MusFitTheme.typography.bodyMedium,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }
        }

        TransferAction(
            title = "Export encrypted backup",
            subtitle = "Includes Food, Training, profile, goals, and local history",
            icon = Icons.Outlined.Upload,
            enabled = !state.busy,
            onClick = onExport,
        )
        if (!bridgeMode) {
            TransferAction(
                title = "Import encrypted backup",
                subtitle = "Verifies checksum and row counts before staging restore",
                icon = Icons.Outlined.Download,
                enabled = !state.busy,
                onClick = onImport,
            )
        }

        if (state.busy) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                Text("Working locally…", style = MusFitTheme.typography.bodyMedium)
            }
        }
        state.message?.let {
            Text(it, style = MusFitTheme.typography.bodyMedium, color = MusFitTheme.colors.onSurfaceVariant)
        }
        state.lastReceipt?.let { receipt ->
            Text(
                "Last restore: ${receipt.totalRows} rows · ${receipt.databaseSha256.take(12)}",
                style = MusFitTheme.typography.bodySmall,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
        }
        state.lastRestoreFailure?.let {
            Text(
                "The pending restore was not applied; your previous data was retained.",
                style = MusFitTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Text(
            "Keep the passphrase separately. MusFit cannot recover it. The encrypted file contains personal health and fitness data.",
            style = MusFitTheme.typography.bodySmall,
            color = MusFitTheme.colors.onSurfaceFaint,
        )
    }
}

@Composable
private fun TransferAction(
    title: String,
    subtitle: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = MusFitTheme.colors.surface,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MusFitTheme.typography.titleMedium, fontWeight = FontWeight.W800)
                Text(subtitle, style = MusFitTheme.typography.bodySmall, color = MusFitTheme.colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PassphraseDialog(
    title: String,
    confirmPassphrase: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var passphrase by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val valid = passphrase.length >= DataTransferArchiveCodec.MINIMUM_PASSPHRASE_LENGTH &&
        (!confirmPassphrase || passphrase == confirmation)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Use at least 12 characters. It is never stored by MusFit.")
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Passphrase") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                )
                if (confirmPassphrase) {
                    OutlinedTextField(
                        value = confirmation,
                        onValueChange = { confirmation = it },
                        label = { Text("Confirm passphrase") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(passphrase) }, enabled = valid) { Text("Continue") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
