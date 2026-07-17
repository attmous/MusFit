package com.musfit.ui

import androidx.compose.runtime.Composable
import com.musfit.ui.permissions.LOCAL_NETWORK_PERMISSION
import com.musfit.ui.permissions.LOCAL_NETWORK_PERMISSION_DENIED_MESSAGE
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
            permissionDeniedMessage = LOCAL_NETWORK_PERMISSION_DENIED_MESSAGE,
            requiresPermission = ::requiresLocalNetworkPermission,
            hasPermission = ::hasLocalNetworkPermission,
        ),
    )
}
