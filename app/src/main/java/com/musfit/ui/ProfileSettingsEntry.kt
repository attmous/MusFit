package com.musfit.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.musfit.BuildConfig
import com.musfit.R
import com.musfit.ui.permissions.LOCAL_NETWORK_PERMISSION
import com.musfit.ui.permissions.hasLocalNetworkPermission
import com.musfit.ui.permissions.requiresLocalNetworkPermission
import com.musfit.ui.profile.ProfileSettingsEntryConfig
import com.musfit.ui.profile.ProfileSettingsScreen
import com.musfit.ui.transfer.DataTransferActivity

@Composable
internal fun ProfileSettingsEntry(onBack: () -> Unit) {
    val context = LocalContext.current
    ProfileSettingsScreen(
        onBack = onBack,
        onOpenDataTransfer = {
            context.startActivity(Intent(context, DataTransferActivity::class.java))
        },
        entryConfig = ProfileSettingsEntryConfig(
            googleWebClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
            versionName = BuildConfig.VERSION_NAME,
            localNetworkPermission = LOCAL_NETWORK_PERMISSION,
            localNetworkPermissionDeniedMessage = stringResource(R.string.app_local_network_permission_denied),
            requiresLocalNetworkPermission = ::requiresLocalNetworkPermission,
            hasLocalNetworkPermission = ::hasLocalNetworkPermission,
        ),
    )
}
