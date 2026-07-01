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

/** Feed-message categories (supersede CoachCategory for the persistent feed). */
enum class CoachMessageCategory { Plan, Nutrition, Training, Trend, Achievement, Recap }

/** One candidate feed message produced by the rules engine for a given day. */
data class CoachMessageCandidate(
    val ruleKey: String,
    val category: CoachMessageCategory,
    val title: String,
    val body: String,
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
    val carbsGrams: Double = 0.0,
    val carbsGoalGrams: Double = 0.0,
    val fatGrams: Double = 0.0,
    val fatGoalGrams: Double = 0.0,
    val waterMl: Double = 0.0,
    val waterGoalMl: Double = 0.0,
    val loggingStreakDays: Int = 0,
    val hasLoggedToday: Boolean = false,
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

    /**
     * The persistent-feed candidates for the current day. Rule keys are stable —
     * they are the dedupe identity in `coach_messages` — and unique within one
     * invocation. Time-gated rules (Plan, Recap, water) are best-effort: a day the
     * user never opens the app during the gate simply has no such message
     * (accepted; no back-fill).
     */
    fun messages(input: CoachInput): List<CoachMessageCandidate> = buildList {
        planMessage(input)?.let(::add)
        proteinGapMessage(input)?.let(::add)
        caloriePacingMessage(input)?.let(::add)
        waterMessage(input)?.let(::add)
        trainingMessage(input)?.let(::add)
        weightTrendMessage(input)?.let(::add)
        stepsMessage(input)?.let(::add)
        streakMessage(input)?.let(::add)
        recapMessage(input)?.let(::add)
    }

    private fun planMessage(input: CoachInput): CoachMessageCandidate? {
        if (input.timeOfDay != TimeOfDay.Morning) return null
        val name = input.firstName?.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""
        val body = if (input.calorieGoalKcal > 0.0 && input.proteinGoalGrams > 0.0) {
            "${input.calorieGoalKcal.roundToInt()} kcal and ${input.proteinGoalGrams.roundToInt()} g protein on the plan — front-load the protein early."
        } else {
            "Log your first meal and I'll pace the day from there."
        }
        return CoachMessageCandidate(
            ruleKey = "plan_morning",
            category = CoachMessageCategory.Plan,
            title = "Good morning$name",
            body = body,
            action = CoachAction.OpenFood,
        )
    }

    private fun proteinGapMessage(input: CoachInput): CoachMessageCandidate? {
        if (input.proteinGoalGrams <= 0.0) return null
        val gap = input.proteinGoalGrams - input.proteinGrams
        if (gap < 10.0) return null
        return CoachMessageCandidate(
            ruleKey = "protein_gap",
            category = CoachMessageCategory.Nutrition,
            title = "Protein's ${gap.roundToInt()} g behind",
            body = "A high-protein snack or shake closes the gap.",
            action = CoachAction.OpenFood,
        )
    }

    private fun caloriePacingMessage(input: CoachInput): CoachMessageCandidate? {
        if (input.calorieGoalKcal <= 0.0) return null
        val remaining = input.calorieGoalKcal - input.caloriesKcal
        return when {
            remaining > 150.0 -> CoachMessageCandidate(
                ruleKey = "calorie_pacing",
                category = CoachMessageCategory.Nutrition,
                title = "${remaining.roundToInt()} kcal left today",
                body = "About ${(remaining / 3.0).roundToInt()} kcal for your next meal keeps you on pace.",
                action = CoachAction.OpenFood,
            )
            remaining < -100.0 -> CoachMessageCandidate(
                ruleKey = "calorie_over",
                category = CoachMessageCategory.Nutrition,
                title = "${(-remaining).roundToInt()} kcal over goal",
                body = "Ease up at the next meal — tomorrow resets the count.",
                action = CoachAction.OpenFood,
            )
            else -> null
        }
    }

    private fun waterMessage(input: CoachInput): CoachMessageCandidate? {
        if (input.timeOfDay == TimeOfDay.Morning) return null
        if (input.waterGoalMl <= 0.0 || input.waterMl >= input.waterGoalMl * 0.5) return null
        val remaining = (input.waterGoalMl - input.waterMl).roundToInt()
        return CoachMessageCandidate(
            ruleKey = "water_low",
            category = CoachMessageCategory.Nutrition,
            title = "Water's running behind",
            body = "$remaining ml to go — a glass now keeps you on track.",
            action = CoachAction.OpenFood,
        )
    }

    private fun trainingMessage(input: CoachInput): CoachMessageCandidate? {
        val days = input.daysSinceLastWorkout
            ?: return input.nextRoutineName?.let { name ->
                CoachMessageCandidate(
                    ruleKey = "train_start",
                    category = CoachMessageCategory.Training,
                    title = "Ready for your first session?",
                    body = "Start $name — it's set up and waiting.",
                    action = input.nextRoutineId?.let { CoachAction.StartRoutine(it) } ?: CoachAction.OpenTraining,
                )
            }
        if (days < 3) return null
        val suffix = input.nextRoutineName?.let { " $it is up next." } ?: ""
        return CoachMessageCandidate(
            ruleKey = "train_recency",
            category = CoachMessageCategory.Training,
            title = "$days days since your last workout",
            body = "You're recovered — today's a good day to train.$suffix",
            action = input.nextRoutineId?.let { CoachAction.StartRoutine(it) } ?: CoachAction.OpenTraining,
        )
    }

    private fun weightTrendMessage(input: CoachInput): CoachMessageCandidate? {
        val delta = input.weightDeltaKg ?: return null
        val (title, body) = when {
            abs(delta) < 0.05 -> "Weight is holding steady" to
                (input.targetWeightKg?.let { "Flat this week — keep nudging toward ${it.formatMetric()} kg." }
                    ?: "Flat this week.")
            delta < 0.0 -> "Weight down ${abs(delta).format1()} kg this week" to "Nice trend — the routine is working."
            else -> "Weight up ${delta.format1()} kg this week" to "One week isn't a trend — watch the next few days."
        }
        return CoachMessageCandidate(
            ruleKey = "weight_trend",
            category = CoachMessageCategory.Trend,
            title = title,
            body = body,
            action = CoachAction.OpenHealth,
        )
    }

    private fun stepsMessage(input: CoachInput): CoachMessageCandidate? {
        if (input.stepGoal <= 0L || input.stepsToday <= 0L) return null
        val remaining = input.stepGoal - input.stepsToday
        if (remaining <= 0L) return null
        return CoachMessageCandidate(
            ruleKey = "steps_progress",
            category = CoachMessageCategory.Trend,
            title = "$remaining steps to your goal",
            body = "A short walk closes it out.",
            action = CoachAction.OpenHealth,
        )
    }

    private fun streakMessage(input: CoachInput): CoachMessageCandidate? {
        if (input.loggingStreakDays < 3) return null
        return CoachMessageCandidate(
            ruleKey = "streak",
            category = CoachMessageCategory.Achievement,
            title = "${input.loggingStreakDays}-day logging streak",
            body = "Consistency is what moves the numbers — keep it rolling.",
            action = CoachAction.OpenFood,
        )
    }

    private fun recapMessage(input: CoachInput): CoachMessageCandidate? {
        if (input.timeOfDay != TimeOfDay.Evening || !input.hasLoggedToday) return null
        val calories = if (input.calorieGoalKcal > 0.0) {
            "${input.caloriesKcal.roundToInt()} of ${input.calorieGoalKcal.roundToInt()} kcal"
        } else {
            "${input.caloriesKcal.roundToInt()} kcal"
        }
        val macros = "${input.proteinGrams.roundToInt()} g protein · " +
            "${input.carbsGrams.roundToInt()} g carbs · ${input.fatGrams.roundToInt()} g fat"
        return CoachMessageCandidate(
            ruleKey = "recap_evening",
            category = CoachMessageCategory.Recap,
            title = "Closing out the day",
            body = "$calories · $macros. See you tomorrow morning.",
            action = null,
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
