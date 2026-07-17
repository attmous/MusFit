package com.musfit.ui

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

enum class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    Today(route = "today", label = "Today", icon = Icons.Outlined.Today, selectedIcon = Icons.Filled.Today),
    Food(route = "food", label = "Food", icon = Icons.Outlined.Restaurant, selectedIcon = Icons.Filled.Restaurant),
    Training(
        route = "training",
        label = "Training",
        icon = Icons.Outlined.FitnessCenter,
        selectedIcon = Icons.Filled.FitnessCenter,
    ),
    Profile(route = "profile", label = "Profile", icon = Icons.Outlined.Person, selectedIcon = Icons.Filled.Person),
}
