package com.musfit.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Today
import androidx.compose.ui.graphics.vector.ImageVector

const val BARCODE_SCANNER_ROUTE = "barcode-scanner"

enum class AppDestination(val route: String, val label: String, val icon: ImageVector) {
    Today(route = "today", label = "Today", icon = Icons.Outlined.Today),
    Food(route = "food", label = "Food", icon = Icons.Outlined.Restaurant),
    Training(route = "training", label = "Training", icon = Icons.Outlined.FitnessCenter),
    Health(route = "health", label = "Health", icon = Icons.Outlined.MonitorHeart),
}
