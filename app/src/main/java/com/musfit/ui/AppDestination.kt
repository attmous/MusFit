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
const val PROFILE_TRAINING_PROGRESS_ROUTE = "profile-training-progress"
const val PROFILE_NUTRITION_TRENDS_ROUTE = "profile-nutrition-trends"

enum class AppDestination(val route: String, val label: String, val icon: ImageVector) {
    Today(route = "today", label = "Today", icon = Icons.Outlined.Today),
    Food(route = "food", label = "Food", icon = Icons.Outlined.Restaurant),
    Training(route = "training", label = "Training", icon = Icons.Outlined.FitnessCenter),
    // Route stays "profile" (nav state, deep links); the user-facing name is "Body".
    Profile(route = "profile", label = "Body", icon = Icons.Outlined.Person),
}

internal fun bottomDestinationForRoute(route: String?): AppDestination =
    when (route) {
        PROFILE_SETTINGS_ROUTE,
        PROFILE_TRAINING_PROGRESS_ROUTE,
        PROFILE_NUTRITION_TRENDS_ROUTE,
        -> AppDestination.Profile
        BARCODE_SCANNER_ROUTE, NUTRITION_LABEL_SCANNER_ROUTE -> AppDestination.Food
        else -> AppDestination.entries.firstOrNull { it.route == route } ?: AppDestination.Today
    }
