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

    @Test
    fun recipeBrowserMealLanesGroupCatalogItemsByMealType() {
        val breakfastIdea =
            recipeDiscoveryItem(
                id = "catalog-breakfast",
                title = "Breakfast oats",
                isSavedRecipe = false,
                sourceRecipeId = null,
                mealTypeIds = listOf("breakfast"),
            )
        val lunchAndDinnerRecipe =
            recipeDiscoveryItem(
                id = "saved-bowl",
                title = "Chicken bowl",
                isSavedRecipe = true,
                sourceRecipeId = "recipe-1",
                mealTypeIds = listOf("lunch", "dinner"),
            )

        val lanes = recipeBrowserMealLanes(
            items = listOf(breakfastIdea, lunchAndDinnerRecipe),
            mealDefinitions = listOf(
                FoodMealDefinitionUiState("breakfast", "Breakfast", null, "", 0, true),
                FoodMealDefinitionUiState("lunch", "Lunch", null, "", 10, true),
                FoodMealDefinitionUiState("dinner", "Dinner", null, "", 20, true),
            ),
        )

        assertEquals(listOf("Breakfast", "Lunch", "Dinner"), lanes.map { it.title })
        assertEquals(listOf("Breakfast oats"), lanes[0].items.map { it.title })
        assertEquals(listOf("Chicken bowl"), lanes[1].items.map { it.title })
        assertEquals(listOf("Chicken bowl"), lanes[2].items.map { it.title })
    }

    private fun recipeDiscoveryItem(
        id: String,
        title: String,
        isSavedRecipe: Boolean,
        sourceRecipeId: String?,
        mealTypeIds: List<String> = listOf("dinner"),
    ): RecipeDiscoveryItemUiState = RecipeDiscoveryItemUiState(
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
        mealTypeIds = mealTypeIds,
        thumbnailKey = "bowl",
    )
}
