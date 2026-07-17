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

@Serializable
data object BarcodeScannerNavKey : AppNavKey

@Serializable
data object NutritionLabelScannerNavKey : AppNavKey

sealed interface AppNavigationAction {
    data class SelectTopLevel(val destination: AppDestination) : AppNavigationAction

    data object OpenProfileSettings : AppNavigationAction

    data object OpenTrainingProgress : AppNavigationAction

    data object OpenNutritionTrends : AppNavigationAction

    data object OpenBarcodeScanner : AppNavigationAction

    data object OpenNutritionLabelScanner : AppNavigationAction

    data object OpenCoach : AppNavigationAction
}

sealed interface AppNavigationResult {
    data class BarcodeDetected(val barcode: String) : AppNavigationResult

    data class NutritionLabelCaptured(val text: String) : AppNavigationResult
}

internal class AppNavigator(
    private val backStack: MutableList<NavKey>,
    private val onResult: (AppNavigationResult) -> Unit = {},
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
            AppNavigationAction.OpenBarcodeScanner -> backStack += BarcodeScannerNavKey
            AppNavigationAction.OpenNutritionLabelScanner -> backStack += NutritionLabelScannerNavKey
            AppNavigationAction.OpenCoach -> onOpenCoach()
        }
    }

    fun complete(result: AppNavigationResult) {
        onResult(result)
        goBack()
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
    FoodNavKey, BarcodeScannerNavKey, NutritionLabelScannerNavKey -> AppDestination.Food
    TrainingNavKey -> AppDestination.Training
    ProfileNavKey, ProfileSettingsNavKey, TrainingProgressNavKey, NutritionTrendsNavKey -> AppDestination.Profile
    TodayNavKey, null -> AppDestination.Today
}
