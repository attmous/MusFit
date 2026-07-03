package com.musfit.ui.food

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodPlanningModePresentationTest {
    @Test
    fun inactivePlanningModeKeepsHeaderActionShortAndHidesStatusWhenEmpty() {
        val presentation = FoodUiState(isPlanningMode = false).toPlanningModePresentation()

        assertEquals("Plan", presentation.buttonLabel)
        assertEquals("Start planning meals", presentation.buttonContentDescription)
        assertEquals("", presentation.statusActionLabel)
        assertFalse(presentation.showStatusCard)
    }

    @Test
    fun activePlanningModeKeepsPlannedWeekCardCompact() {
        val presentation =
            FoodUiState(
                isPlanningMode = true,
                weeklyPlan = listOf(
                    FoodPlanDayUiState(
                        date = LocalDate.of(2026, 7, 3),
                        dayLabel = "Fri",
                        loggedCaloriesKcal = 430.0,
                        plannedCaloriesKcal = 560.0,
                        loggedEntryCount = 2,
                        plannedEntryCount = 1,
                    ),
                    FoodPlanDayUiState(
                        date = LocalDate.of(2026, 7, 4),
                        dayLabel = "Sat",
                        loggedCaloriesKcal = 0.0,
                        plannedCaloriesKcal = 360.0,
                        loggedEntryCount = 0,
                        plannedEntryCount = 1,
                    ),
                ),
            ).toPlanningModePresentation()

        assertEquals("Planning", presentation.buttonLabel)
        assertEquals("Finish planning meals", presentation.buttonContentDescription)
        assertEquals("Planning mode", presentation.statusTitle)
        assertEquals("", presentation.statusDescription)
        assertEquals("Done", presentation.statusActionLabel)
        assertTrue(presentation.showStatusCard)
    }

    @Test
    fun inactivePlanningModeKeepsPlannedWeekCardCompact() {
        val presentation =
            FoodUiState(
                weeklyPlan = listOf(
                    FoodPlanDayUiState(
                        date = LocalDate.of(2026, 7, 3),
                        dayLabel = "Fri",
                        loggedCaloriesKcal = 430.0,
                        plannedCaloriesKcal = 560.0,
                        loggedEntryCount = 2,
                        plannedEntryCount = 1,
                    ),
                ),
            ).toPlanningModePresentation()

        assertEquals("Planned this week", presentation.statusTitle)
        assertEquals("", presentation.statusDescription)
        assertEquals("", presentation.statusActionLabel)
        assertTrue(presentation.showStatusCard)
    }

    @Test
    fun foodEntryActionLabelsFollowPlanningMode() {
        assertEquals("Log", FoodUiState().foodEntryActionVerb)
        assertEquals("Logging", FoodUiState(isSaving = true).foodEntryActionProgressLabel)
        assertEquals("Log food", FoodUiState().foodEntryActionLabel("food"))
        assertEquals("Save and log", FoodUiState().saveAndFoodEntryActionLabel)

        assertEquals("Plan", FoodUiState(isPlanningMode = true).foodEntryActionVerb)
        assertEquals("Planning", FoodUiState(isPlanningMode = true, isSaving = true).foodEntryActionProgressLabel)
        assertEquals("Plan food", FoodUiState(isPlanningMode = true).foodEntryActionLabel("food"))
        assertEquals("Save and plan", FoodUiState(isPlanningMode = true).saveAndFoodEntryActionLabel)
    }
}
