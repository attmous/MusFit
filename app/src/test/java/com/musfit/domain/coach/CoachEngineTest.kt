package com.musfit.domain.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachEngineTest {
    private fun messageInput(
        timeOfDay: TimeOfDay = TimeOfDay.Afternoon,
        caloriesKcal: Double = 1200.0,
        calorieGoalKcal: Double = 2000.0,
        proteinGrams: Double = 80.0,
        proteinGoalGrams: Double = 150.0,
        daysSinceLastWorkout: Int? = 1,
        nextRoutineName: String? = "Legs Day",
        nextRoutineId: String? = "r1",
        weightDeltaKg: Double? = null,
        targetWeightKg: Double? = null,
        stepsToday: Long = 4000L,
        stepGoal: Long = 10_000L,
        carbsGrams: Double = 130.0,
        carbsGoalGrams: Double = 200.0,
        fatGrams: Double = 40.0,
        fatGoalGrams: Double = 60.0,
        waterMl: Double = 1500.0,
        waterGoalMl: Double = 2000.0,
        loggingStreakDays: Int = 0,
        hasLoggedToday: Boolean = true,
    ) = CoachInput(
        timeOfDay = timeOfDay,
        firstName = null,
        caloriesKcal = caloriesKcal,
        calorieGoalKcal = calorieGoalKcal,
        proteinGrams = proteinGrams,
        proteinGoalGrams = proteinGoalGrams,
        daysSinceLastWorkout = daysSinceLastWorkout,
        nextRoutineName = nextRoutineName,
        nextRoutineId = nextRoutineId,
        weightDeltaKg = weightDeltaKg,
        targetWeightKg = targetWeightKg,
        stepsToday = stepsToday,
        stepGoal = stepGoal,
        carbsGrams = carbsGrams,
        carbsGoalGrams = carbsGoalGrams,
        fatGrams = fatGrams,
        fatGoalGrams = fatGoalGrams,
        waterMl = waterMl,
        waterGoalMl = waterGoalMl,
        loggingStreakDays = loggingStreakDays,
        hasLoggedToday = hasLoggedToday,
    )

    @Test
    fun messages_planOnlyInTheMorning() {
        val morning = CoachEngine.messages(messageInput(timeOfDay = TimeOfDay.Morning))
        val evening = CoachEngine.messages(messageInput(timeOfDay = TimeOfDay.Evening))
        assertTrue(morning.any { it.ruleKey == "plan_morning" && it.category == CoachMessageCategory.Plan })
        assertTrue(evening.none { it.ruleKey == "plan_morning" })
    }

    @Test
    fun messages_recapOnlyInEveningAndOnlyWhenSomethingWasLogged() {
        val evening = CoachEngine.messages(messageInput(timeOfDay = TimeOfDay.Evening))
        val quietEvening = CoachEngine.messages(
            messageInput(timeOfDay = TimeOfDay.Evening, hasLoggedToday = false),
        )
        val morning = CoachEngine.messages(messageInput(timeOfDay = TimeOfDay.Morning))
        assertTrue(evening.any { it.ruleKey == "recap_evening" && it.category == CoachMessageCategory.Recap })
        assertTrue(quietEvening.none { it.ruleKey == "recap_evening" })
        assertTrue(morning.none { it.ruleKey == "recap_evening" })
    }

    @Test
    fun messages_proteinGapHasTitleBodyAndFoodAction() {
        val messages = CoachEngine.messages(messageInput(proteinGrams = 80.0, proteinGoalGrams = 150.0))
        val gap = messages.first { it.ruleKey == "protein_gap" }
        assertEquals(CoachMessageCategory.Nutrition, gap.category)
        assertTrue(gap.title.contains("70"))
        assertEquals(CoachAction.OpenFood, gap.action)
    }

    @Test
    fun messages_waterRuleFiresAfternoonWhenUnderHalfOfGoal() {
        val behind = CoachEngine.messages(messageInput(waterMl = 400.0, waterGoalMl = 2000.0))
        val fine = CoachEngine.messages(messageInput(waterMl = 1500.0, waterGoalMl = 2000.0))
        val morning = CoachEngine.messages(
            messageInput(timeOfDay = TimeOfDay.Morning, waterMl = 0.0, waterGoalMl = 2000.0),
        )
        assertTrue(behind.any { it.ruleKey == "water_low" })
        assertTrue(fine.none { it.ruleKey == "water_low" })
        assertTrue(morning.none { it.ruleKey == "water_low" })
    }

    @Test
    fun messages_trainingRecencyStartsRoutineWhenOverdue() {
        val messages = CoachEngine.messages(messageInput(daysSinceLastWorkout = 4))
        val nudge = messages.first { it.ruleKey == "train_recency" }
        assertEquals(CoachMessageCategory.Training, nudge.category)
        assertEquals(CoachAction.StartRoutine("r1"), nudge.action)
    }

    @Test
    fun messages_streakAchievementFromThreeDays() {
        val streak = CoachEngine.messages(messageInput(loggingStreakDays = 5))
        val noStreak = CoachEngine.messages(messageInput(loggingStreakDays = 2))
        assertTrue(streak.any { it.ruleKey == "streak" && it.category == CoachMessageCategory.Achievement })
        assertTrue(noStreak.none { it.ruleKey == "streak" })
    }

    @Test
    fun messages_caloriePacingAndOverUseDistinctKeys() {
        val pacing = CoachEngine.messages(messageInput(caloriesKcal = 1200.0, calorieGoalKcal = 2000.0))
        val over = CoachEngine.messages(messageInput(caloriesKcal = 2200.0, calorieGoalKcal = 2000.0))
        assertTrue(pacing.any { it.ruleKey == "calorie_pacing" && it.title.contains("800") })
        assertTrue(pacing.none { it.ruleKey == "calorie_over" })
        assertTrue(over.any { it.ruleKey == "calorie_over" && it.title.contains("200") })
    }

    @Test
    fun messages_weightTrendCoversDownFlatAndUp() {
        val down = CoachEngine.messages(messageInput(weightDeltaKg = -0.4))
        val flat = CoachEngine.messages(messageInput(weightDeltaKg = 0.0, targetWeightKg = 75.0))
        val up = CoachEngine.messages(messageInput(weightDeltaKg = 0.6))
        assertTrue(down.any { it.ruleKey == "weight_trend" && it.title.contains("down") })
        assertTrue(flat.any { it.ruleKey == "weight_trend" && it.title.contains("steady") })
        assertTrue(up.any { it.ruleKey == "weight_trend" && it.title.contains("up") })
    }

    @Test
    fun messages_stepsProgressOnlyWhenShortOfGoal() {
        val short = CoachEngine.messages(messageInput(stepsToday = 4000L, stepGoal = 10_000L))
        val done = CoachEngine.messages(messageInput(stepsToday = 11_000L, stepGoal = 10_000L))
        assertTrue(short.any { it.ruleKey == "steps_progress" && it.title.contains("6000") })
        assertTrue(done.none { it.ruleKey == "steps_progress" })
    }

    @Test
    fun messages_trainStartWhenNoHistory() {
        val messages = CoachEngine.messages(messageInput(daysSinceLastWorkout = null))
        val start = messages.first { it.ruleKey == "train_start" }
        assertEquals(CoachAction.StartRoutine("r1"), start.action)
    }

    @Test
    fun messages_ruleKeysAreStableAcrossInvocations() {
        val input = messageInput(timeOfDay = TimeOfDay.Evening, weightDeltaKg = -0.4)
        assertEquals(
            CoachEngine.messages(input).map { it.ruleKey },
            CoachEngine.messages(input).map { it.ruleKey },
        )
    }

    @Test
    fun messages_recapOmitsGoalWhenUnset() {
        val messages = CoachEngine.messages(
            messageInput(timeOfDay = TimeOfDay.Evening, hasLoggedToday = true, calorieGoalKcal = 0.0),
        )
        val recap = messages.first { it.ruleKey == "recap_evening" }
        assertTrue(recap.body.contains("kcal"))
        assertTrue(!recap.body.contains("of 0"))
    }

    @Test
    fun messages_proteinGapBoundaryAtTenGrams() {
        val atBoundary = CoachEngine.messages(messageInput(proteinGrams = 140.0, proteinGoalGrams = 150.0))
        val underBoundary = CoachEngine.messages(messageInput(proteinGrams = 140.5, proteinGoalGrams = 150.0))
        assertTrue(atBoundary.any { it.ruleKey == "protein_gap" })
        assertTrue(underBoundary.none { it.ruleKey == "protein_gap" })
    }

    @Test
    fun messages_calorieDeadZoneEmitsNothing() {
        val messages = CoachEngine.messages(messageInput(caloriesKcal = 1900.0, calorieGoalKcal = 2000.0))
        assertTrue(messages.none { it.ruleKey == "calorie_pacing" })
        assertTrue(messages.none { it.ruleKey == "calorie_over" })
    }

    @Test
    fun messages_waterExactlyHalfIsSilent() {
        val messages = CoachEngine.messages(messageInput(waterMl = 1000.0, waterGoalMl = 2000.0))
        assertTrue(messages.none { it.ruleKey == "water_low" })
    }

    @Test
    fun messages_trainingFallsBackToOpenTrainingWithoutRoutineId() {
        val messages = CoachEngine.messages(messageInput(daysSinceLastWorkout = 4, nextRoutineId = null))
        val nudge = messages.first { it.ruleKey == "train_recency" }
        assertEquals(CoachAction.OpenTraining, nudge.action)
    }

    @Test
    fun messages_planWithoutGoalsUsesFallbackBody() {
        val messages = CoachEngine.messages(
            messageInput(timeOfDay = TimeOfDay.Morning, calorieGoalKcal = 0.0),
        )
        val plan = messages.first { it.ruleKey == "plan_morning" }
        assertTrue(plan.body.contains("Log your first meal"))
    }
}
