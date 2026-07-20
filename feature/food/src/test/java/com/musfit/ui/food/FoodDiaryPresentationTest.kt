package com.musfit.ui.food

import com.musfit.ui.text.UiText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FoodDiaryPresentationTest {
    @Test
    fun mealDiarySummary_countsLoggedItemsAndEmphasizesCalories() {
        val breakfast = foodMealSection(
            caloriesKcal = 545.0,
            entries = List(3) { foodMealEntry(name = "Item $it", caloriesKcal = 180.0) },
            rating = rating("Great"),
        )
        val lunch = foodMealSection(
            caloriesKcal = 620.0,
            entries = List(2) { foodMealEntry(name = "Item $it", caloriesKcal = 310.0) },
        )
        val single = foodMealSection(
            caloriesKcal = 278.0,
            entries = listOf(foodMealEntry(name = "Item", caloriesKcal = 278.0)),
        )

        assertEquals(
            MealDiarySummary(3, 545, MealDiaryQualifier.Rating, UiText.Verbatim("Great")),
            breakfast.mealDiarySummary(),
        )
        assertEquals(MealDiarySummary(2, 620, MealDiaryQualifier.None), lunch.mealDiarySummary())
        assertEquals(MealDiarySummary(1, 278, MealDiaryQualifier.None), single.mealDiarySummary())
    }

    @Test
    fun mealDiarySummary_saysSoFarWhilePlannedItemsRemain() {
        val dinner = foodMealSection(
            caloriesKcal = 278.0,
            plannedCaloriesKcal = 320.0,
            entries = listOf(
                foodMealEntry(name = "Logged", caloriesKcal = 278.0),
                foodMealEntry(name = "Planned", caloriesKcal = 320.0, isPlanned = true),
            ),
            rating = rating("Great"), // "so far" outranks the rating while items are pending
        )

        assertEquals(
            MealDiarySummary(1, 278, MealDiaryQualifier.SoFar, UiText.Verbatim("Great")),
            dinner.mealDiarySummary(),
        )
    }

    @Test
    fun mealDiarySummary_plannedOnlyAndEmptyStates() {
        val plannedMeal = foodMealSection(
            plannedCaloriesKcal = 245.0,
            entries = listOf(foodMealEntry(name = "Planned", caloriesKcal = 245.0, isPlanned = true)),
        )
        val emptyMeal = foodMealSection()

        assertEquals(MealDiarySummary(0, 245, MealDiaryQualifier.Planned), plannedMeal.mealDiarySummary())
        assertEquals(MealDiarySummary(0, null, MealDiaryQualifier.Empty), emptyMeal.mealDiarySummary())
    }
}

private fun foodMealSection(
    caloriesKcal: Double = 0.0,
    plannedCaloriesKcal: Double = 0.0,
    entries: List<FoodMealEntryUiState> = emptyList(),
    rating: FoodRatingUiState? = null,
): FoodMealSectionUiState = FoodMealSectionUiState(
    id = "breakfast",
    title = "Breakfast",
    recommendation = "Recommended 417 - 625 kcal",
    caloriesKcal = caloriesKcal,
    calorieTargetKcal = 625.0,
    calorieProgress = 0.0,
    plannedCaloriesKcal = plannedCaloriesKcal,
    rating = rating,
    entries = entries,
)

private fun rating(label: String): FoodRatingUiState = FoodRatingUiState(
    label = label,
    reason = "On plan",
    suggestion = "Keep going",
    tone = FoodInsightTone.Positive,
)

private fun foodMealEntry(
    name: String,
    quantityGrams: Double = 100.0,
    caloriesKcal: Double,
    proteinGrams: Double = 0.0,
    carbsGrams: Double = 0.0,
    fatGrams: Double = 0.0,
    isPlanned: Boolean = false,
): FoodMealEntryUiState = FoodMealEntryUiState(
    id = "entry-1",
    foodId = "food-1",
    name = name,
    brand = "Kitchen",
    quantityGrams = quantityGrams,
    caloriesKcal = caloriesKcal,
    proteinGrams = proteinGrams,
    carbsGrams = carbsGrams,
    fatGrams = fatGrams,
    isPlanned = isPlanned,
)
