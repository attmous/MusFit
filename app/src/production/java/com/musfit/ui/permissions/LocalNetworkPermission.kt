@file:Suppress("UNUSED_PARAMETER")

package com.musfit.ui.permissions

import android.content.Context

const val LOCAL_NETWORK_PERMISSION = ""
const val LOCAL_NETWORK_PERMISSION_DENIED_MESSAGE = "Use an HTTPS AI coach endpoint."

fun requiresLocalNetworkPermission(baseUrl: String): Boolean = false

fun hasLocalNetworkPermission(context: Context): Boolean = true
