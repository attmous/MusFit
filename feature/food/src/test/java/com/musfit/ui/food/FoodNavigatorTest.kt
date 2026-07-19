package com.musfit.ui.food

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FoodNavigatorTest {
    @Test
    fun typedRoutes_preserveIdsAndPopOneAtATime() {
        val stack = mutableListOf<NavKey>(FoodDiaryNavKey)
        val navigator = FoodNavigator(stack)

        navigator.open(FoodAddNavKey("lunch"))
        navigator.open(FoodSavedFoodEditorNavKey("food-7"))

        assertEquals(FoodSavedFoodEditorNavKey("food-7"), navigator.currentKey)
        assertTrue(navigator.back())
        assertEquals(FoodAddNavKey("lunch"), navigator.currentKey)
        assertTrue(navigator.back())
        assertFalse(navigator.back())
    }

    @Test
    fun adaptiveDetails_replaceCurrentSelectionAndBackReturnsToList() {
        val stack = mutableListOf<NavKey>(FoodDiaryNavKey, FoodDatabaseNavKey)
        val navigator = FoodNavigator(stack)

        navigator.open(FoodDetailNavKey("food-1"))
        navigator.open(FoodDetailNavKey("food-2"))

        assertEquals(listOf(FoodDiaryNavKey, FoodDatabaseNavKey, FoodDetailNavKey("food-2")), stack)
        assertTrue(navigator.back())
        assertEquals(FoodDatabaseNavKey, navigator.currentKey)
    }

    @Test
    fun scannerResult_isConsumedOnceByMatchingProducer() {
        val stack = mutableListOf<NavKey>(FoodDiaryNavKey, FoodAddNavKey("breakfast"), FoodBarcodeScannerNavKey)
        val results = mutableListOf<FoodNavigationResult>()
        val navigator = FoodNavigator(stack, results::add)

        assertTrue(navigator.complete(FoodNavigationResult.BarcodeDetected("900000000001")))
        assertEquals(listOf(FoodNavigationResult.BarcodeDetected("900000000001")), results)
        assertEquals(FoodAddNavKey("breakfast"), navigator.currentKey)
        assertFalse(navigator.complete(FoodNavigationResult.BarcodeDetected("900000000001")))
        assertEquals(1, results.size)
    }

    @Test
    fun typedStack_roundTripsThroughProcessSavedState() {
        val stack: List<FoodNavKey> = listOf(
            FoodDiaryNavKey,
            FoodAddNavKey("dinner"),
            FoodDetailNavKey("food-42"),
            FoodSavedFoodEditorNavKey("food-42"),
            FoodNutritionLabelScannerNavKey,
        )

        val restored = decodeFromSavedState<List<FoodNavKey>>(encodeToSavedState(stack))

        assertEquals(stack, restored)
    }

    @Test
    fun scannerRoutes_hideRootNavigationChrome() {
        assertFalse(FoodBarcodeScannerNavKey.showsRootNavigationChrome())
        assertFalse(FoodNutritionLabelScannerNavKey.showsRootNavigationChrome())
        assertTrue(FoodDiaryNavKey.showsRootNavigationChrome())
        assertTrue(FoodAddNavKey("dinner").showsRootNavigationChrome())
    }

    @Test
    fun unsavedRecipeEditor_requiresDiscardConfirmationBeforeBack() {
        val cleanState = FoodUiState(recipeEditor = RecipeEditorState())
        val dirtyState = FoodUiState(recipeEditor = RecipeEditorState(name = "Draft dinner"))

        assertFalse(FoodRecipeEditorNavKey().shouldConfirmRecipeDiscard(cleanState))
        assertTrue(FoodRecipeEditorNavKey().shouldConfirmRecipeDiscard(dirtyState))
        assertFalse(FoodRecipeBrowserNavKey.shouldConfirmRecipeDiscard(dirtyState))
    }

    @Test
    fun closedEditorContent_noLongerMatchesItsTypedDestination() {
        val key = FoodSavedFoodEditorNavKey("food-42")
        val activeRoute = FoodPresentationReducers.route(
            FoodUiState(
                isAddPanelVisible = true,
                sheetMode = FoodSheetMode.SavedFoodEditor,
                savedFoodEditor = SavedFoodEditorState(id = "food-42"),
            ),
        )

        assertTrue(activeRoute.matchesFoodNavKey(key))
        assertFalse(FoodPresentationReducers.route(FoodUiState()).matchesFoodNavKey(key))
    }
}
