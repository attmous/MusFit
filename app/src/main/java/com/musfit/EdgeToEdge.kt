package com.musfit

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge

/** Applies the one app-wide edge-to-edge window policy before Compose content. */
internal fun ComponentActivity.configureMusFitEdgeToEdge() {
    enableEdgeToEdge()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = false
    }
}
