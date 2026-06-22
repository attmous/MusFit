package com.musfit.domain.coach

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class CoachCategory { NutritionPacing, TrainingNudge, Trend }

sealed interface CoachAction {
    object OpenFood : CoachAction
    object OpenTraining : CoachAction
    object OpenHealth : CoachAction
    data class StartRoutine(val routineId: String) : CoachAction
}

data class CoachCue(
    val id: String,
    val category: CoachCategory,
    val priority: Int,
    val text: String,
    val action: CoachAction?,
)

enum class TimeOfDay { Morning, Afternoon, Evening }

data class CoachInput(
    val timeOfDay: TimeOfDay,
    val firstName: String?,
    val caloriesKcal: Double,
    val calorieGoalKcal: Double,
    val proteinGrams: Double,
    val proteinGoalGrams: Double,
    val daysSinceLastWorkout: Int?,
    val nextRoutineName: String?,
    val nextRoutineId: String?,
    val weightDeltaKg: Double?,
    val targetWeightKg: Double?,
    val stepsToday: Long,
    val stepGoal: Long,
)

data class CoachBriefing(
    val greeting: String,
    val cues: List<CoachCue>,
)

/** Deterministic, on-device coach. Builds a prioritized list of cues from the user's own data. */
object CoachEngine {
    fun briefing(input: CoachInput): CoachBriefing {
        val cues = buildList {
            proteinCue(input)?.let(::add)
            caloriesCue(input)?.let(::add)
            trainingCue(input)?.let(::add)
            weightCue(input)?.let(::add)
            stepsCue(input)?.let(::add)
        }.sortedByDescending { it.priority }

        return CoachBriefing(
            greeting = greeting(input),
            cues = cues.ifEmpty { listOf(welcomeCue()) },
        )
    }

    private fun greeting(input: CoachInput): String {
        val part = when (input.timeOfDay) {
            TimeOfDay.Morning -> "Good morning"
            TimeOfDay.Afternoon -> "Good afternoon"
            TimeOfDay.Evening -> "Good evening"
        }
        return if (input.firstName.isNullOrBlank()) "$part." else "$part, ${input.firstName}."
    }

    private fun proteinCue(input: CoachInput): CoachCue? {
        if (input.proteinGoalGrams <= 0.0) return null
        val gap = input.proteinGoalGrams - input.proteinGrams
        if (gap < 10.0) return null
        return CoachCue(
            id = "protein",
            category = CoachCategory.NutritionPacing,
            priority = 70,
            text = "${gap.roundToInt()} g protein to go — a high-protein snack covers it.",
            action = CoachAction.OpenFood,
        )
    }

    private fun caloriesCue(input: CoachInput): CoachCue? {
        if (input.calorieGoalKcal <= 0.0) return null
        val remaining = input.calorieGoalKcal - input.caloriesKcal
        return when {
            remaining > 150.0 -> CoachCue(
                id = "calories",
                category = CoachCategory.NutritionPacing,
                priority = 50,
                text = "${remaining.roundToInt()} kcal left — about ${(remaining / 3.0).roundToInt()} for your next meal.",
                action = CoachAction.OpenFood,
            )
            remaining < -100.0 -> CoachCue(
                id = "calories-over",
                category = CoachCategory.NutritionPacing,
                priority = 55,
                text = "${(-remaining).roundToInt()} kcal over goal — ease up at dinner.",
                action = CoachAction.OpenFood,
            )
            else -> null
        }
    }

    private fun trainingCue(input: CoachInput): CoachCue? {
        val days = input.daysSinceLastWorkout
            ?: return input.nextRoutineName?.let { name ->
                CoachCue(
                    id = "train-start",
                    category = CoachCategory.TrainingNudge,
                    priority = 60,
                    text = "Ready to train? Start $name.",
                    action = input.nextRoutineId?.let { CoachAction.StartRoutine(it) } ?: CoachAction.OpenTraining,
                )
            }
        if (days < 3) return null
        val suffix = input.nextRoutineName?.let { " Start $it?" } ?: ""
        return CoachCue(
            id = "train-overdue",
            category = CoachCategory.TrainingNudge,
            priority = 75,
            text = "$days days since your last workout.$suffix",
            action = input.nextRoutineId?.let { CoachAction.StartRoutine(it) } ?: CoachAction.OpenTraining,
        )
    }

    private fun weightCue(input: CoachInput): CoachCue? {
        val delta = input.weightDeltaKg ?: return null
        val target = input.targetWeightKg
        return when {
            abs(delta) < 0.05 -> CoachCue(
                id = "weight-flat",
                category = CoachCategory.Trend,
                priority = 40,
                text = "Weight is flat this week" + (target?.let { " — keep nudging toward ${it.formatMetric()} kg." } ?: "."),
                action = CoachAction.OpenHealth,
            )
            delta < 0.0 -> CoachCue(
                id = "weight-down",
                category = CoachCategory.Trend,
                priority = 45,
                text = "Weight down ${abs(delta).format1()} kg this week — nice trend.",
                action = CoachAction.OpenHealth,
            )
            else -> CoachCue(
                id = "weight-up",
                category = CoachCategory.Trend,
                priority = 42,
                text = "Weight up ${delta.format1()} kg this week.",
                action = CoachAction.OpenHealth,
            )
        }
    }

    private fun stepsCue(input: CoachInput): CoachCue? {
        if (input.stepGoal <= 0L || input.stepsToday <= 0L) return null
        val remaining = input.stepGoal - input.stepsToday
        if (remaining <= 0L) return null
        return CoachCue(
            id = "steps",
            category = CoachCategory.Trend,
            priority = 30,
            text = "$remaining steps to your goal.",
            action = CoachAction.OpenHealth,
        )
    }

    private fun welcomeCue(): CoachCue =
        CoachCue(
            id = "welcome",
            category = CoachCategory.NutritionPacing,
            priority = 0,
            text = "Log a meal or start a workout to get going.",
            action = CoachAction.OpenFood,
        )

    private fun Double.format1(): String = String.format(Locale.US, "%.1f", this)

    private fun Double.formatMetric(): String =
        if (this % 1.0 == 0.0) toInt().toString() else String.format(Locale.US, "%.1f", this)
}
