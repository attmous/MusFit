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

    @Test
    fun addDatabaseProjectionIgnoresEditorAndPlanningChanges() {
        val baseline = FoodUiState(
            foodDatabaseQuery = "oats",
            productName = "Rolled oats",
        )
        val unrelated = baseline.copy(
            recipeBrowserMealType = "dinner",
            shoppingStartDateInput = "2026-07-20",
            customMealNameInput = "Late snack",
        )

        assertEquals(
            FoodPresentationReducers.addDatabase(baseline),
            FoodPresentationReducers.addDatabase(unrelated),
        )
        assertNotEquals(
            FoodPresentationReducers.addDatabase(baseline),
            FoodPresentationReducers.addDatabase(baseline.copy(foodDatabaseQuery = "banana")),
        )
    }

    @Test
    fun editorPlanningProjectionIgnoresAddAndDatabaseChanges() {
        val baseline = FoodUiState(
            recipeBrowserMealType = "lunch",
            shoppingStartDateInput = "2026-07-16",
        )
        val unrelated = baseline.copy(
            foodDatabaseQuery = "banana",
            barcode = "123456789",
            aiLoggingText = "one banana",
        )

        assertEquals(
            FoodPresentationReducers.editorPlanning(baseline),
            FoodPresentationReducers.editorPlanning(unrelated),
        )
        assertNotEquals(
            FoodPresentationReducers.editorPlanning(baseline),
            FoodPresentationReducers.editorPlanning(baseline.copy(recipeBrowserMealType = "dinner")),
        )
    }

    @Test
    fun routeProjectionClassifiesDestinationLifetime() {
        assertEquals(
            FoodSurfaceGroup.AddDatabase,
            FoodPresentationReducers.route(
                FoodUiState(isAddPanelVisible = true, sheetMode = FoodSheetMode.FoodDatabase),
            ).surfaceGroup,
        )
        assertEquals(
            FoodSurfaceGroup.EditorPlanning,
            FoodPresentationReducers.route(
                FoodUiState(isAddPanelVisible = true, sheetMode = FoodSheetMode.RecipeEditor),
            ).surfaceGroup,
        )
        assertEquals(
            FoodSurfaceGroup.Tracker,
            FoodPresentationReducers.route(
                FoodUiState(isAddPanelVisible = true, sheetMode = FoodSheetMode.Water),
            ).surfaceGroup,
        )
    }
}
