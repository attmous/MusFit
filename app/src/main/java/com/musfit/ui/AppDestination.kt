package com.musfit.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Today
import androidx.compose.ui.graphics.vector.ImageVector

const val BARCODE_SCANNER_ROUTE = "barcode-scanner"
const val NUTRITION_LABEL_SCANNER_ROUTE = "nutrition-label-scanner"
const val PROFILE_SETTINGS_ROUTE = "profile-settings"

enum class AppDestination(val route: String, val label: String, val icon: ImageVector) {
    Today(route = "today", label = "Today", icon = Icons.Outlined.Today),
    Food(route = "food", label = "Food", icon = Icons.Outlined.Restaurant),
    Training(route = "training", label = "Training", icon = Icons.Outlined.FitnessCenter),
    Profile(route = "profile", label = "Profile", icon = Icons.Outlined.Person),
}

internal fun bottomDestinationForRoute(route: String?): AppDestination =
    when (route) {
        PROFILE_SETTINGS_ROUTE -> AppDestination.Profile
        BARCODE_SCANNER_ROUTE, NUTRITION_LABEL_SCANNER_ROUTE -> AppDestination.Food
        else -> AppDestination.entries.firstOrNull { it.route == route } ?: AppDestination.Today
    }
