package com.musfit.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppNavKey : NavKey

sealed interface AppTopLevelKey : AppNavKey

@Serializable
data object TodayNavKey : AppTopLevelKey

@Serializable
data object FoodNavKey : AppTopLevelKey

@Serializable
data object TrainingNavKey : AppTopLevelKey

@Serializable
data object ProfileNavKey : AppTopLevelKey

@Serializable
data object ProfileSettingsNavKey : AppNavKey

@Serializable
data object TrainingProgressNavKey : AppNavKey

@Serializable
data object NutritionTrendsNavKey : AppNavKey

sealed interface AppNavigationAction {
    data class SelectTopLevel(val destination: AppDestination) : AppNavigationAction

    data object OpenProfileSettings : AppNavigationAction

    data object OpenTrainingProgress : AppNavigationAction

    data object OpenNutritionTrends : AppNavigationAction

    data object OpenCoach : AppNavigationAction
}

internal class AppNavigator(
    private val backStack: MutableList<NavKey>,
    private val onOpenCoach: () -> Unit = {},
) {
    val currentDestination: AppDestination
        get() = bottomDestinationForKey(backStack.lastOrNull() as? AppNavKey)

    fun navigate(action: AppNavigationAction) {
        when (action) {
            is AppNavigationAction.SelectTopLevel -> selectTopLevel(action.destination)
            AppNavigationAction.OpenProfileSettings -> backStack += ProfileSettingsNavKey
            AppNavigationAction.OpenTrainingProgress -> backStack += TrainingProgressNavKey
            AppNavigationAction.OpenNutritionTrends -> backStack += NutritionTrendsNavKey
            AppNavigationAction.OpenCoach -> onOpenCoach()
        }
    }

    fun goBack(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }

    private fun selectTopLevel(destination: AppDestination) {
        while (backStack.lastOrNull() !is AppTopLevelKey) {
            backStack.removeAt(backStack.lastIndex)
        }
        val target = destination.navKey
        if (backStack.lastOrNull() == target) return

        // Keep one retained entry per top-level destination. Re-selecting a previously visited
        // destination moves it to the end of the visit-order stack instead of creating a second
        // ViewModel/collector owner for the same screen.
        backStack.remove(target)
        backStack += target
    }
}

internal val AppDestination.navKey: AppTopLevelKey
    get() = when (this) {
        AppDestination.Today -> TodayNavKey
        AppDestination.Food -> FoodNavKey
        AppDestination.Training -> TrainingNavKey
        AppDestination.Profile -> ProfileNavKey
    }

internal fun bottomDestinationForKey(key: AppNavKey?): AppDestination = when (key) {
    FoodNavKey -> AppDestination.Food
    TrainingNavKey -> AppDestination.Training
    ProfileNavKey, ProfileSettingsNavKey, TrainingProgressNavKey, NutritionTrendsNavKey -> AppDestination.Profile
    TodayNavKey, null -> AppDestination.Today
}
