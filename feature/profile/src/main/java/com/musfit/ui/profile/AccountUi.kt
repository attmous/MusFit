@file:OptIn(ExperimentalMaterial3Api::class)

package com.musfit.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musfit.data.repository.Account
import com.musfit.data.repository.AccountAuthProvider
import com.musfit.feature.profile.R
import com.musfit.ui.components.PillButton
import com.musfit.ui.components.SheetDragHandle
import com.musfit.ui.text.UiText
import com.musfit.ui.text.asString
import com.musfit.ui.text.uiText
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent

data class AccountUiState(
    val displayName: String = "",
    val email: String? = null,
    val provider: AccountAuthProvider = AccountAuthProvider.Local,
    val providerLabel: UiText = uiText(R.string.profile_local_account_title),
    val avatarUrl: String? = null,
)

internal fun Account.toUiState() = AccountUiState(
    displayName = displayName,
    email = email,
    provider = authProvider,
    providerLabel = authProvider.profileLabel(),
    avatarUrl = avatarUrl,
)

private fun AccountAuthProvider.profileLabel(): UiText = when (this) {
    AccountAuthProvider.Local -> uiText(R.string.profile_local_account_title)
    AccountAuthProvider.Google -> uiText(R.string.profile_google)
    AccountAuthProvider.GitHub -> uiText(R.string.profile_github)
}

/** Initials monogram: first letters of the first two words ("Max Berger" → "MB"). */
internal fun String.accountInitials(): String = trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    .take(2)
    .mapNotNull { word -> word.firstOrNull(Char::isLetterOrDigit)?.uppercaseChar() }
    .joinToString("")
    .ifEmpty { "Y" }

/**
 * The 11b local-account hero: tonal container with a white initials circle,
 * the account name, the local-first assurance line, and a white "Edit" pill.
 */
@Composable
internal fun LocalAccountHero(account: AccountUiState, accent: TabAccent, onEdit: () -> Unit) {
    val displayName = account.displayName.ifBlank { stringResource(R.string.profile_you) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = accent.container,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MusFitTheme.colors.surface)
                    // Decorative initials: without this TalkBack reads bare letters before the name.
                    .clearAndSetSemantics {},
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    displayName.accountInitials(),
                    style = MusFitTheme.typography.titleLarge.copy(fontSize = 17.sp),
                    color = accent.onContainer,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    displayName,
                    style = MusFitTheme.typography.titleLarge.copy(fontSize = 17.sp, letterSpacing = (-0.2).sp),
                    color = accent.onContainer,
                )
                Text(
                    stringResource(R.string.profile_account_device_summary, account.providerLabel.asString()),
                    style = MusFitTheme.typography.bodySmall,
                    color = accent.onContainerVariant,
                )
            }
            HeroChip(text = stringResource(R.string.profile_edit), accent = accent, onClick = onEdit)
        }
    }
}

/**
 * The local-account editor as a Turn 11 sheet: name + optional email in white
 * field tiles, the local-first note, and a filled Save pill.
 */
@Composable
fun AccountEditSheet(
    name: String,
    email: String,
    error: UiText?,
    accent: TabAccent,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MusFitTheme.colors.background,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)) { SheetDragHandle() }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Column {
                Text(
                    stringResource(R.string.profile_local_account_title),
                    style = MusFitTheme.typography.headlineMedium.copy(fontSize = 22.sp, lineHeight = 25.sp),
                )
                Text(
                    stringResource(R.string.profile_account_sheet_subtitle),
                    style = MusFitTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            AccountTextTile(label = stringResource(R.string.profile_name), value = name, onValueChange = onNameChange)
            AccountTextTile(
                label = stringResource(R.string.profile_email_optional),
                value = email,
                onValueChange = onEmailChange,
                keyboardType = KeyboardType.Email,
            )
            if (error != null) {
                Text(
                    error.asString(),
                    style = MusFitTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onDestructiveContainer,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            PillButton(
                text = stringResource(R.string.profile_save),
                onClick = onSave,
                containerColor = accent.color,
                contentColor = accent.onColor,
                height = 50.dp,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
            ProfileCancelAction(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

/** Free-text white field tile (name/email) in the 11e tile grammar. */
@Composable
private fun AccountTextTile(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Surface(color = MusFitTheme.colors.surface, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
        ) {
            Text(
                text = label,
                style = MusFitTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.W600,
                    letterSpacing = 0.sp,
                ),
                color = MusFitTheme.colors.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.clearAndSetSemantics { },
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MusFitTheme.typography.titleSmall.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W700,
                    color = MusFitTheme.colors.onSurface,
                ),
                cursorBrush = SolidColor(MusFitTheme.colors.brand),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = label },
            )
        }
    }
}
