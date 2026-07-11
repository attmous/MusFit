@file:Suppress("UNUSED_PARAMETER")

package com.musfit.ui.permissions

import android.content.Context
import com.musfit.data.repository.AiCoachProviderKind

const val LOCAL_NETWORK_PERMISSION = ""

fun requiresLocalNetworkPermission(providerKind: AiCoachProviderKind): Boolean = false

fun hasLocalNetworkPermission(context: Context): Boolean = true
