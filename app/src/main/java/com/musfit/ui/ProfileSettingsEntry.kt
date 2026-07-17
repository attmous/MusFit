package com.musfit.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.musfit.BuildConfig
import com.musfit.ui.permissions.LOCAL_NETWORK_PERMISSION
import com.musfit.ui.permissions.LOCAL_NETWORK_PERMISSION_DENIED_MESSAGE
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
            localNetworkPermissionDeniedMessage = LOCAL_NETWORK_PERMISSION_DENIED_MESSAGE,
            requiresLocalNetworkPermission = ::requiresLocalNetworkPermission,
            hasLocalNetworkPermission = ::hasLocalNetworkPermission,
        ),
    )
}
