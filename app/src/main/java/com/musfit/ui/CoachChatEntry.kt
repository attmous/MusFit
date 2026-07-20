package com.musfit.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.musfit.R
import com.musfit.ui.permissions.LOCAL_NETWORK_PERMISSION
import com.musfit.ui.permissions.hasLocalNetworkPermission
import com.musfit.ui.permissions.requiresLocalNetworkPermission
import com.musfit.ui.today.ChatPreviewSheet
import com.musfit.ui.today.TodayLocalNetworkConfig

@Composable
internal fun CoachChatEntry(
    onDismiss: () -> Unit,
    onConfigure: () -> Unit,
) {
    ChatPreviewSheet(
        onDismiss = onDismiss,
        onConfigure = onConfigure,
        localNetworkConfig = TodayLocalNetworkConfig(
            permission = LOCAL_NETWORK_PERMISSION,
            permissionDeniedMessage = stringResource(R.string.app_local_network_permission_denied),
            requiresPermission = ::requiresLocalNetworkPermission,
            hasPermission = ::hasLocalNetworkPermission,
        ),
    )
}
