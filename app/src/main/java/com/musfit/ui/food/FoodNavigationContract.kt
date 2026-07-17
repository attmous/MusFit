package com.musfit.ui.food

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface FoodNavKey : NavKey

@Serializable
data object FoodDiaryNavKey : FoodNavKey

@Serializable
data class FoodMealDetailNavKey(val mealType: String) : FoodNavKey

@Serializable
data class FoodAddNavKey(val mealType: String) : FoodNavKey

@Serializable
data object FoodDatabaseNavKey : FoodNavKey

@Serializable
data class FoodDetailNavKey(val foodId: String) : FoodNavKey

@Serializable
data class FoodDiaryEntryEditorNavKey(val entryId: String) : FoodNavKey

@Serializable
data class FoodSavedFoodEditorNavKey(val foodId: String? = null) : FoodNavKey

@Serializable
data object FoodNutritionLabelReviewNavKey : FoodNavKey

@Serializable
data object FoodBarcodeComparisonNavKey : FoodNavKey

@Serializable
data object FoodFastingTimerNavKey : FoodNavKey

@Serializable
data object FoodGoalEditorNavKey : FoodNavKey

@Serializable
data object FoodRecipeBrowserNavKey : FoodNavKey

@Serializable
data class FoodRecipeEditorNavKey(val recipeId: String? = null) : FoodNavKey

@Serializable
data object FoodMealTemplatesNavKey : FoodNavKey

@Serializable
data class FoodMealTemplateEditorNavKey(val templateId: String) : FoodNavKey

@Serializable
data object FoodMealSettingsNavKey : FoodNavKey

@Serializable
data class FoodMealDefinitionEditorNavKey(val mealId: String) : FoodNavKey

@Serializable
data object FoodShoppingListNavKey : FoodNavKey

@Serializable
data object FoodWaterNavKey : FoodNavKey

@Serializable
data object FoodHealthConnectNavKey : FoodNavKey

@Serializable
data object FoodBarcodeScannerNavKey : FoodNavKey

@Serializable
data object FoodNutritionLabelScannerNavKey : FoodNavKey

sealed interface FoodNavigationResult {
    data class BarcodeDetected(val barcode: String) : FoodNavigationResult

    data class NutritionLabelCaptured(val text: String) : FoodNavigationResult
}

internal class FoodNavigator(
    private val backStack: MutableList<NavKey>,
    private val onResult: (FoodNavigationResult) -> Unit = {},
) {
    val currentKey: FoodNavKey
        get() = backStack.lastOrNull() as? FoodNavKey ?: FoodDiaryNavKey

    fun open(key: FoodNavKey) {
        if (backStack.lastOrNull() != key) backStack += key
    }

    fun replace(key: FoodNavKey) {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
        if (backStack.lastOrNull() != key) backStack += key
    }

    fun back(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }

    fun complete(result: FoodNavigationResult): Boolean {
        val producer = backStack.lastOrNull()
        val expectedProducer = when (result) {
            is FoodNavigationResult.BarcodeDetected -> FoodBarcodeScannerNavKey
            is FoodNavigationResult.NutritionLabelCaptured -> FoodNutritionLabelScannerNavKey
        }
        if (producer != expectedProducer) return false
        onResult(result)
        back()
        return true
    }
}
