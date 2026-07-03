package com.musfit.ui.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FoodDiaryPresentationTest {
    @Test
    fun mealSectionSummaryUsesCompactDiaryCalories() {
        val loggedMeal = foodMealSection(caloriesKcal = 512.4)
        val plannedMeal = foodMealSection(plannedCaloriesKcal = 245.0)
        val mixedMeal = foodMealSection(caloriesKcal = 410.0, plannedCaloriesKcal = 180.0)

        assertEquals("512 kcal", loggedMeal.compactDiarySummaryLabel())
        assertEquals("245 kcal planned", plannedMeal.compactDiarySummaryLabel())
        assertEquals("410 kcal + 180 planned", mixedMeal.compactDiarySummaryLabel())
    }

    @Test
    fun mealEntriesUseSingleCommaSeparatedHomeLine() {
        val entries = listOf(
            foodMealEntry(name = "Blueberries", caloriesKcal = 46.0),
            foodMealEntry(name = "Rolled oats", caloriesKcal = 233.0),
            foodMealEntry(name = "Greek yogurt 2%", caloriesKcal = 146.0),
        )

        assertEquals(
            "Blueberries (46 kcal), Rolled oats (233 kcal), Greek yogurt 2% (146 kcal)",
            entries.compactDiaryEntriesLabel(),
        )
    }
}

private fun foodMealSection(
    caloriesKcal: Double = 0.0,
    plannedCaloriesKcal: Double = 0.0,
): FoodMealSectionUiState =
    FoodMealSectionUiState(
        id = "breakfast",
        title = "Breakfast",
        recommendation = "Recommended 417 - 625 kcal",
        caloriesKcal = caloriesKcal,
        calorieTargetKcal = 625.0,
        calorieProgress = 0.0,
        plannedCaloriesKcal = plannedCaloriesKcal,
        entries = emptyList(),
    )

private fun foodMealEntry(
    name: String,
    quantityGrams: Double = 100.0,
    caloriesKcal: Double,
    proteinGrams: Double = 0.0,
    carbsGrams: Double = 0.0,
    fatGrams: Double = 0.0,
): FoodMealEntryUiState =
    FoodMealEntryUiState(
        id = "entry-1",
        foodId = "food-1",
        name = name,
        brand = "Kitchen",
        quantityGrams = quantityGrams,
        caloriesKcal = caloriesKcal,
        proteinGrams = proteinGrams,
        carbsGrams = carbsGrams,
        fatGrams = fatGrams,
    )
