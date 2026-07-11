package com.musfit.ui.permissions

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.musfit.data.repository.AiCoachProviderKind

const val LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"

fun requiresLocalNetworkPermission(providerKind: AiCoachProviderKind): Boolean =
    Build.VERSION.SDK_INT >= 37 && providerKind == AiCoachProviderKind.LocalAgent

fun hasLocalNetworkPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < 37 ||
        ContextCompat.checkSelfPermission(context, LOCAL_NETWORK_PERMISSION) == PackageManager.PERMISSION_GRANTED
