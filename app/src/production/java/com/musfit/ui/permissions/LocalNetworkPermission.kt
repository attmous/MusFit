@file:Suppress("UNUSED_PARAMETER")

package com.musfit.ui.permissions

import android.content.Context

const val LOCAL_NETWORK_PERMISSION = ""

fun requiresLocalNetworkPermission(baseUrl: String): Boolean = false

fun hasLocalNetworkPermission(context: Context): Boolean = true
