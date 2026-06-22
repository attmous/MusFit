package com.musfit.domain.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachEngineTest {
    private fun input(
        timeOfDay: TimeOfDay = TimeOfDay.Morning,
        firstName: String? = "Mo",
        caloriesKcal: Double = 0.0,
        calorieGoalKcal: Double = 0.0,
        proteinGrams: Double = 0.0,
        proteinGoalGrams: Double = 0.0,
        daysSinceLastWorkout: Int? = null,
        nextRoutineName: String? = null,
        nextRoutineId: String? = null,
        weightDeltaKg: Double? = null,
        targetWeightKg: Double? = null,
        stepsToday: Long = 0L,
        stepGoal: Long = 0L,
    ) = CoachInput(
        timeOfDay, firstName, caloriesKcal, calorieGoalKcal, proteinGrams, proteinGoalGrams,
        daysSinceLastWorkout, nextRoutineName, nextRoutineId, weightDeltaKg, targetWeightKg, stepsToday, stepGoal,
    )

    @Test
    fun proteinGapProducesNutritionCue() {
        val briefing = CoachEngine.briefing(input(proteinGrams = 40.0, proteinGoalGrams = 100.0, daysSinceLastWorkout = 0))
        val cue = briefing.cues.first()
        assertEquals(CoachCategory.NutritionPacing, cue.category)
        assertEquals(CoachAction.OpenFood, cue.action)
        assertTrue(cue.text.contains("60 g protein"))
    }

    @Test
    fun daysSinceWorkoutProducesTrainingNudgeWithRoutineAction() {
        val briefing = CoachEngine.briefing(input(daysSinceLastWorkout = 3, nextRoutineName = "Leg day", nextRoutineId = "r1"))
        val cue = briefing.cues.first()
        assertEquals(CoachCategory.TrainingNudge, cue.category)
        assertEquals(CoachAction.StartRoutine("r1"), cue.action)
        assertTrue(cue.text.contains("3 days"))
    }

    @Test
    fun flatWeightProducesTrendCue() {
        val briefing = CoachEngine.briefing(input(weightDeltaKg = 0.0, targetWeightKg = 78.0, daysSinceLastWorkout = 0))
        assertTrue(briefing.cues.any { it.category == CoachCategory.Trend && it.text.contains("flat") })
    }

    @Test
    fun emptyInputProducesWelcomeCueAndGreeting() {
        val briefing = CoachEngine.briefing(input())
        assertEquals(1, briefing.cues.size)
        assertEquals("welcome", briefing.cues.first().id)
        assertEquals("Good morning, Mo.", briefing.greeting)
    }

    @Test
    fun higherPriorityCueLeads() {
        val briefing = CoachEngine.briefing(
            input(proteinGrams = 40.0, proteinGoalGrams = 100.0, daysSinceLastWorkout = 3, nextRoutineName = "Push", nextRoutineId = "r2"),
        )
        assertEquals(CoachCategory.TrainingNudge, briefing.cues.first().category)
    }
}
