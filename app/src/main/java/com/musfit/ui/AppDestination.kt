package com.musfit.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Today
import androidx.compose.ui.graphics.vector.ImageVector
import com.musfit.R

enum class AppDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    Today(route = "today", labelRes = R.string.app_destination_today, icon = Icons.Outlined.Today, selectedIcon = Icons.Filled.Today),
    Food(route = "food", labelRes = R.string.app_destination_food, icon = Icons.Outlined.Restaurant, selectedIcon = Icons.Filled.Restaurant),
    Training(
        route = "training",
        labelRes = R.string.app_destination_training,
        icon = Icons.Outlined.FitnessCenter,
        selectedIcon = Icons.Filled.FitnessCenter,
    ),
    Profile(route = "profile", labelRes = R.string.app_destination_profile, icon = Icons.Outlined.Person, selectedIcon = Icons.Filled.Person),
}
