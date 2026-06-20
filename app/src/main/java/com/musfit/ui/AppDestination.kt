package com.musfit.ui

enum class AppDestination(val route: String, val label: String) {
    Today(route = "today", label = "Today"),
    Food(route = "food", label = "Food"),
    Training(route = "training", label = "Training"),
    Health(route = "health", label = "Health"),
}
