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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.musfit.data.repository.AccountErasureScope
import com.musfit.feature.profile.R
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
            title = stringResource(R.string.profile_data_privacy),
            onBack = onBack,
            subtitle = stringResource(R.string.profile_data_privacy_summary),
        )
        Text(stringResource(R.string.profile_data_privacy_local_storage))
        ProfileHubRow(
            title = stringResource(R.string.profile_encrypted_backups),
            subtitle = stringResource(R.string.profile_encrypted_backups_summary),
            shape = RoundedCornerShape(24.dp),
            onClick = onOpenDataTransfer,
            leading = null,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ProfileHubRow(title = stringResource(R.string.profile_delete_this_account), subtitle = stringResource(R.string.profile_delete_this_account_summary), shape = groupedShape(0, 2), onClick = onDeleteAccount, leading = { DestructiveDataBadge() })
            ProfileHubRow(title = stringResource(R.string.profile_delete_all_data), subtitle = stringResource(R.string.profile_delete_all_data_summary), shape = groupedShape(1, 2), onClick = onDeleteAllData, leading = { DestructiveDataBadge() })
        }
        Text(stringResource(R.string.profile_health_deletion_optional), color = accent.onContainer)
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
        title = {
            Text(stringResource(if (deletingAll) R.string.profile_delete_all_data_question else R.string.profile_delete_this_account_question))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(
                        if (deletingAll) R.string.profile_delete_all_data_warning else R.string.profile_delete_this_account_warning,
                    ),
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
                    Text(stringResource(R.string.profile_also_delete_health_records))
                }
                Text(stringResource(R.string.profile_saved_exports_not_deleted))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !inProgress) {
                Text(stringResource(if (inProgress) R.string.profile_deleting else R.string.profile_delete_permanently))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !inProgress) { Text(stringResource(R.string.profile_cancel)) } },
    )
}
