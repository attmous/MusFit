package com.musfit.ui.permissions

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.musfit.data.remote.coach.AiCoachEndpointPolicy

const val LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"
const val LOCAL_NETWORK_PERMISSION_DENIED_MESSAGE =
    "Allow Local Network access to reach the configured private AI coach endpoint."

fun requiresLocalNetworkPermission(baseUrl: String): Boolean =
    Build.VERSION.SDK_INT >= 37 && AiCoachEndpointPolicy.requiresPrivateLanRouting(baseUrl)

fun hasLocalNetworkPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < 37 ||
        ContextCompat.checkSelfPermission(context, LOCAL_NETWORK_PERMISSION) == PackageManager.PERMISSION_GRANTED
