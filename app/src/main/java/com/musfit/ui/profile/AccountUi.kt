package com.musfit.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.musfit.data.repository.Account
import com.musfit.ui.AppDestination
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.tabAccentFor

data class AccountUiState(
    val displayName: String = "You",
    val email: String? = null,
    val providerLabel: String = "Local account",
    val avatarUrl: String? = null,
)

internal fun Account.toUiState() =
    AccountUiState(
        displayName = displayName,
        email = email,
        providerLabel = authProvider.displayName,
        avatarUrl = avatarUrl,
    )

private fun String.accountInitial(): String =
    trim().firstOrNull()?.uppercaseChar()?.toString() ?: "Y"

@Composable
fun AccountSection(account: AccountUiState, onEdit: () -> Unit) {
    val accent = tabAccentFor(AppDestination.Profile)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MusFitTheme.shapes.extraLarge,
        color = MusFitTheme.colors.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accent.container)
                    // Decorative initial: without this TalkBack reads a bare letter before the name.
                    .clearAndSetSemantics {},
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    account.displayName.accountInitial(),
                    style = MusFitTheme.typography.titleMedium,
                    color = accent.onContainer,
                    fontWeight = FontWeight.Medium,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(account.displayName, style = MusFitTheme.typography.titleMedium)
                Text(
                    account.email ?: account.providerLabel,
                    style = MusFitTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
                if (account.email != null) {
                    Text(
                        account.providerLabel,
                        style = MusFitTheme.typography.labelSmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = "Edit account")
            }
        }
    }
}

@Composable
fun AccountEditDialog(
    name: String,
    email: String,
    error: String?,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Local account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    isError = error != null,
                    supportingText = { if (error != null) Text(error) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Email (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Stored on this device. Sync and sign-in are not enabled.",
                    style = MusFitTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
