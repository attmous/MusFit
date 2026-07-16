package com.musfit.ui.food

import com.musfit.data.repository.FoodDiary
import com.musfit.data.repository.NutritionDetails
import com.musfit.domain.model.NutritionTotals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.LocalDate

class FoodPresentationReducersTest {
    @Test
    fun formReductionKeepsOneDecimalSeparatorAndDigits() {
        assertEquals("12.34", " 1a2..3g4 ".sanitizeDecimalInput())
    }

    @Test
    fun summaryCalculatorsPreserveInsightsAndNetCarbProgress() {
        val state = FoodUiState(
            proteinGoalGrams = 100.0,
            fiberGoalGrams = 30.0,
        )
        val diary = FoodDiary(
            totals = NutritionTotals(600.0, 20.0, 80.0, 15.0),
            detailTotals = NutritionDetails(fiberGrams = 4.0),
            meals = emptyList(),
        )

        assertEquals(
            listOf("Protein is low", "Fiber is below target", "Add protein next"),
            state.buildDailyInsights(diary).map { it.title },
        )
        assertEquals(
            76.0,
            diary.totals.toMacroProgress(
                carbsGoalGrams = 200.0,
                fiberGrams = diary.detailTotals.fiberGrams,
                useNetCarbs = true,
            ).first().currentGrams,
            0.0,
        )
    }

    @Test
    fun diaryProjectionIgnoresDatabaseAndEditorChanges() {
        val baseline = FoodUiState(
            selectedDate = LocalDate.of(2026, 7, 16),
            eatenCaloriesKcal = 850.0,
            calorieGoalKcal = 2_100.0,
        )

        val unrelated = baseline.copy(
            foodDatabaseQuery = "oats",
            productName = "Rolled oats",
            barcode = "123456",
        )

        assertEquals(
            FoodPresentationReducers.diary(baseline),
            FoodPresentationReducers.diary(unrelated),
        )
    }

    @Test
    fun trackerProjectionIgnoresDiaryAndSearchChanges() {
        val baseline = FoodUiState(
            waterConsumedMilliliters = 1_250.0,
            waterGoalMilliliters = 2_000.0,
            waterProgress = 0.625,
        )

        val unrelated = baseline.copy(
            eatenCaloriesKcal = 1_500.0,
            foodDatabaseQuery = "banana",
        )

        assertEquals(
            FoodPresentationReducers.trackers(baseline),
            FoodPresentationReducers.trackers(unrelated),
        )
    }

    @Test
    fun projectionsChangeOnlyWhenTheirInputsChange() {
        val baseline = FoodUiState()
        val diaryChange = baseline.copy(eatenCaloriesKcal = 420.0)
        val trackerChange = baseline.copy(waterCustomAmountInput = "330")

        assertNotEquals(
            FoodPresentationReducers.diary(baseline),
            FoodPresentationReducers.diary(diaryChange),
        )
        assertEquals(
            FoodPresentationReducers.trackers(baseline),
            FoodPresentationReducers.trackers(diaryChange),
        )
        assertNotEquals(
            FoodPresentationReducers.trackers(baseline),
            FoodPresentationReducers.trackers(trackerChange),
        )
        assertEquals(
            FoodPresentationReducers.diary(baseline),
            FoodPresentationReducers.diary(trackerChange),
        )
    }
}
