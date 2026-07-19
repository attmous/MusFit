package com.musfit.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.musfit.data.repository.AccountErasureScope
import com.musfit.ui.components.ExpressiveBadge
import com.musfit.ui.components.ExpressiveBadgeShape
import com.musfit.ui.components.InnerScreenHeader
import com.musfit.ui.components.groupedShape
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent

@Composable
internal fun DataPrivacySettingsPage(
    accent: TabAccent,
    onBack: () -> Unit,
    onOpenDataTransfer: () -> Unit,
    onDeleteAccount: () -> Unit,
    onDeleteAllData: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MusFitTheme.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        InnerScreenHeader(
            title = "Data & privacy",
            onBack = onBack,
            subtitle = "Local storage, retention and erasure",
        )
        Text("MusFit stores account and fitness data locally on this device. Encrypted export files saved outside the app remain under your control.")
        ProfileHubRow(
            title = "Encrypted backups",
            subtitle = "Export or restore a MusFit backup file",
            shape = RoundedCornerShape(24.dp),
            onClick = onOpenDataTransfer,
            leading = null,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ProfileHubRow(title = "Delete this account", subtitle = "Erase this identity and only its local MusFit data", shape = groupedShape(0, 2), onClick = onDeleteAccount, leading = { DestructiveDataBadge() })
            ProfileHubRow(title = "Delete all MusFit data", subtitle = "Erase every local account, then create a fresh local account", shape = groupedShape(1, 2), onClick = onDeleteAllData, leading = { DestructiveDataBadge() })
        }
        Text("Health Connect deletion is optional and targets only records authored by MusFit.", color = accent.onContainer)
    }
}

@Composable
private fun DestructiveDataBadge() {
    ExpressiveBadge(
        icon = Icons.Outlined.DeleteForever,
        shape = ExpressiveBadgeShape.Squircle,
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        size = 40.dp,
        iconSize = 19.dp,
    )
}

@Composable
internal fun AccountErasureDialog(
    state: ProfileSettingsUiState,
    onDeleteAuthoredHealthRecordsChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val scope = requireNotNull(state.accountErasureScope)
    val inProgress = state.isErasingAccountData
    val deletingAll = scope == AccountErasureScope.AllAccounts
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (deletingAll) "Delete all MusFit data?" else "Delete this account?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (deletingAll) {
                        "This permanently removes every local account and all owned Food, Training, profile, AI coach, and Health cache data. A blank local account will be created."
                    } else {
                        "This permanently removes the current account and all data it owns. Other local accounts are kept and one becomes active."
                    },
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .toggleable(
                            value = state.deleteAuthoredHealthRecords,
                            enabled = !inProgress,
                            role = Role.Checkbox,
                            onValueChange = onDeleteAuthoredHealthRecordsChange,
                        ),
                ) {
                    Checkbox(
                        checked = state.deleteAuthoredHealthRecords,
                        onCheckedChange = null,
                        enabled = !inProgress,
                    )
                    Text("Also delete records MusFit authored in Health Connect")
                }
                Text("Encrypted export files already saved outside MusFit are not deleted.")
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !inProgress) {
                Text(if (inProgress) "Deleting..." else "Delete permanently")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !inProgress) { Text("Cancel") } },
    )
}
