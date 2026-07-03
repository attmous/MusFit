package com.musfit.ui.food

import org.junit.Assert.assertEquals
import org.junit.Test

class FoodRecipeBrowserUiTest {

    @Test
    fun recipeBrowserSectionsSeparatePlanReadySavedRecipesFromReviewOnlyIdeas() {
        val savedRecipe =
            recipeDiscoveryItem(
                id = "saved-recipe-1",
                title = "Saved salmon bowl",
                isSavedRecipe = true,
                sourceRecipeId = "recipe-1",
            )
        val recipeIdea =
            recipeDiscoveryItem(
                id = "catalog-chicken-bowl",
                title = "Chicken bowl idea",
                isSavedRecipe = false,
                sourceRecipeId = null,
            )
        val malformedSavedItem =
            recipeDiscoveryItem(
                id = "saved-missing-source",
                title = "Incomplete saved recipe",
                isSavedRecipe = true,
                sourceRecipeId = null,
            )

        val sections = sectionRecipeBrowserItems(listOf(savedRecipe, recipeIdea, malformedSavedItem))

        assertEquals(listOf(savedRecipe), sections.savedRecipes)
        assertEquals(listOf(recipeIdea, malformedSavedItem), sections.recipeIdeas)
    }

    private fun recipeDiscoveryItem(
        id: String,
        title: String,
        isSavedRecipe: Boolean,
        sourceRecipeId: String?,
    ): RecipeDiscoveryItemUiState =
        RecipeDiscoveryItemUiState(
            id = id,
            title = title,
            subtitle = "Test item",
            category = "Dinner",
            servingName = "Bowl",
            servingGrams = 350.0,
            caloriesKcal = 520.0,
            proteinGrams = 40.0,
            carbsGrams = 45.0,
            fatGrams = 16.0,
            tagLabels = listOf("High protein"),
            isFavorite = false,
            isSavedRecipe = isSavedRecipe,
            programRelevant = true,
            sourceRecipeId = sourceRecipeId,
        )
}
