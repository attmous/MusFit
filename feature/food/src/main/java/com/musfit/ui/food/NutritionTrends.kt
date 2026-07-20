package com.musfit.ui.food

import com.musfit.data.repository.FoodDiary
import com.musfit.data.repository.FoodDiaryEntry
import com.musfit.data.repository.FoodDiaryEntryStatus
import com.musfit.data.repository.FoodGoal
import com.musfit.data.repository.FoodProgressSummary
import com.musfit.data.repository.FoodWeeklyDaySummary
import com.musfit.data.repository.FoodWeeklySummary
import com.musfit.feature.food.R
import com.musfit.ui.text.UiText
import com.musfit.ui.text.pluralUiText
import com.musfit.ui.text.uiText
import kotlin.math.roundToInt

// Nutrition trends: the weekly MusFit score and the 7/28-day progress stats. Moved out of
// FoodViewModel/FoodScreen when Trends graduated from a Food diary tab to a Profile sub-screen.
// The shared habit-keyword sets and `matchesHabitKeyword` stay in FoodViewModel (Summary uses them
// too); this file references them as same-package `internal` members.

data class FoodWeeklyScoreUiState(
    val title: UiText,
    val score: Int,
    val summary: UiText,
    val suggestion: UiText,
    val tone: FoodInsightTone,
    val factors: List<FoodRatingFactorUiState>,
)

data class FoodProgressStatsUiState(
    val weekly: FoodProgressPeriodUiState,
    val monthly: FoodProgressPeriodUiState,
)

data class FoodProgressMetricUiState(
    val caption: UiText,
    val value: UiText,
)

data class FoodProgressPeriodUiState(
    val title: UiText,
    val trackedDaysLabel: UiText,
    val metrics: List<FoodProgressMetricUiState>,
    val trendLabel: UiText,
)

private data class WeeklyScoreFactor(
    val score: Int,
    val uiState: FoodRatingFactorUiState,
)

internal fun FoodWeeklySummary.toWeeklyScoreUiState(): FoodWeeklyScoreUiState {
    val foodTrackedDays = days.filter { day -> day.diary.hasTrackedNutrition() }
    val hydrationDays = foodTrackedDays.ifEmpty {
        days.filter { day -> day.water.consumedMilliliters > 0.0 }
    }
    if (foodTrackedDays.isEmpty() && hydrationDays.isEmpty()) {
        return emptyWeeklyScore()
    }

    val nutritionFactor = buildWeeklyNutritionFactor(foodTrackedDays, goal)
    val hydrationFactor = buildWeeklyHydrationFactor(hydrationDays)
    val habitFactor = buildWeeklyHabitFactor(days)
    val trainingFactor = WeeklyScoreFactor(
        score = 50,
        uiState = FoodRatingFactorUiState(
            label = uiText(R.string.food_training_signal),
            valueLabel = uiText(R.string.food_not_available),
            explanation = uiText(R.string.food_training_signal_unavailable),
            tone = FoodInsightTone.Neutral,
        ),
    )
    val factors = listOf(nutritionFactor, hydrationFactor, habitFactor, trainingFactor)
    val score =
        (
            nutritionFactor.score * 0.45 +
                hydrationFactor.score * 0.30 +
                habitFactor.score * 0.15 +
                trainingFactor.score * 0.10
            ).roundToInt()
            .coerceIn(0, 100)

    return FoodWeeklyScoreUiState(
        title = uiText(R.string.food_weekly_musfit_score),
        score = score,
        summary = pluralUiText(
            R.plurals.food_tracked_days_window,
            foodTrackedDays.size,
            UiText.Argument.Integer(foodTrackedDays.size),
        ),
        suggestion = weeklyScoreSuggestion(nutritionFactor.score, hydrationFactor.score, habitFactor.score),
        tone = score.toWeeklyScoreTone(),
        factors = factors.map { factor -> factor.uiState },
    )
}

internal fun FoodProgressSummary.toProgressStatsUiState(): FoodProgressStatsUiState {
    val rangeDays = days.take(dayCount)
    return FoodProgressStatsUiState(
        weekly = rangeDays.takeLast(7).toProgressPeriodUiState(
            title = uiText(R.string.food_last_seven_days),
            goal = goal,
        ),
        monthly = rangeDays.takeLast(28).toProgressPeriodUiState(
            title = uiText(R.string.food_last_twenty_eight_days),
            goal = goal,
        ),
    )
}

private fun List<FoodWeeklyDaySummary>.toProgressPeriodUiState(
    title: UiText,
    goal: FoodGoal,
): FoodProgressPeriodUiState {
    val trackedDays = filter { day -> day.diary.hasTrackedNutrition() }
    val trackedCount = trackedDays.size
    val averageCalories = trackedDays.map { it.diary.totals.caloriesKcal }.averageOrZero()
    val averageProtein = trackedDays.map { it.diary.totals.proteinGrams }.averageOrZero()
    val calorieTargetDays =
        trackedDays.count { day ->
            day.diary.totals.caloriesKcal >= goal.dailyCaloriesKcal * 0.9 &&
                day.diary.totals.caloriesKcal <= goal.dailyCaloriesKcal * 1.1
        }
    val hydrationDays =
        trackedDays.count { day ->
            day.water.goalMilliliters > 0.0 &&
                day.water.consumedMilliliters >= day.water.goalMilliliters
        }
    val habitDays =
        trackedDays.count { day ->
            val entries = day.diary.meals.flatMap { meal -> meal.entries }
            entries.anyHabitKeyword(fruitHabitKeywords) ||
                entries.anyHabitKeyword(vegetableHabitKeywords) ||
                entries.anyHabitKeyword(fishHabitKeywords)
        }
    return FoodProgressPeriodUiState(
        title = title,
        trackedDaysLabel = pluralUiText(
            R.plurals.food_tracked_days,
            trackedCount,
            UiText.Argument.Integer(trackedCount),
        ),
        metrics = buildProgressMetrics(
            ProgressMetricInputs(
                averageCalories = averageCalories,
                averageProtein = averageProtein,
                calorieTargetDays = calorieTargetDays,
                hydrationDays = hydrationDays,
                habitDays = habitDays,
                trackedCount = trackedCount,
            ),
        ),
        trendLabel = trendLabelForCalories(),
    )
}

private data class ProgressMetricInputs(
    val averageCalories: Double,
    val averageProtein: Double,
    val calorieTargetDays: Int,
    val hydrationDays: Int,
    val habitDays: Int,
    val trackedCount: Int,
)

private fun buildProgressMetrics(inputs: ProgressMetricInputs): List<FoodProgressMetricUiState> = listOf(
    FoodProgressMetricUiState(
        uiText(R.string.food_average_calories),
        uiText(R.string.food_integer_kcal, UiText.Argument.Integer(inputs.averageCalories.roundToInt())),
    ),
    FoodProgressMetricUiState(
        uiText(R.string.food_average_protein),
        uiText(R.string.food_integer_grams, UiText.Argument.Integer(inputs.averageProtein.roundToInt())),
    ),
    FoodProgressMetricUiState(
        uiText(R.string.food_calorie_target),
        pluralUiText(
            R.plurals.food_days_ratio,
            inputs.trackedCount,
            UiText.Argument.Integer(inputs.calorieTargetDays),
            UiText.Argument.Integer(inputs.trackedCount),
        ),
    ),
    FoodProgressMetricUiState(
        uiText(R.string.food_hydration),
        pluralUiText(
            R.plurals.food_days_ratio,
            inputs.trackedCount,
            UiText.Argument.Integer(inputs.hydrationDays),
            UiText.Argument.Integer(inputs.trackedCount),
        ),
    ),
    FoodProgressMetricUiState(
        uiText(R.string.food_habit_days),
        uiText(
            R.string.food_integer_ratio,
            UiText.Argument.Integer(inputs.habitDays),
            UiText.Argument.Integer(inputs.trackedCount),
        ),
    ),
)

private fun List<FoodWeeklyDaySummary>.trendLabelForCalories(): UiText {
    // Split the tracked days (not the calendar window, which includes untracked days) in half so each
    // half holds a comparable number of real data points regardless of where logging happened to fall.
    val trackedCalories = filter { it.diary.hasTrackedNutrition() }.map { it.diary.totals.caloriesKcal }
    val midpoint = trackedCalories.size / 2
    val firstAverage = trackedCalories.take(midpoint).averageOrNull()
    val secondAverage = trackedCalories.drop(midpoint).averageOrNull()
    if (firstAverage == null || secondAverage == null) {
        return uiText(R.string.food_trend_needs_more_days)
    }
    val difference = secondAverage - firstAverage
    return when {
        kotlin.math.abs(difference) < 100.0 -> uiText(R.string.food_calories_stable)
        difference > 0.0 -> uiText(R.string.food_calories_trending_up)
        else -> uiText(R.string.food_calories_trending_down)
    }
}

private fun List<Double>.averageOrZero(): Double = averageOrNull() ?: 0.0

private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()

private fun FoodDiary.hasTrackedNutrition(): Boolean = totals.caloriesKcal > 0.0 ||
    meals
        .flatMap { meal -> meal.entries }
        .any { entry -> entry.status == FoodDiaryEntryStatus.Logged }

private fun buildWeeklyNutritionFactor(days: List<FoodWeeklyDaySummary>, goal: FoodGoal): WeeklyScoreFactor {
    if (days.isEmpty()) {
        return WeeklyScoreFactor(
            score = 0,
            uiState = FoodRatingFactorUiState(
                label = uiText(R.string.food_nutrition_consistency),
                valueLabel = uiText(R.string.food_percentage, UiText.Argument.Integer(0)),
                explanation = uiText(R.string.food_no_logged_days_window),
                tone = FoodInsightTone.Warning,
            ),
        )
    }

    val dayScores =
        days.map { day ->
            listOf(
                day.diary.totals.caloriesKcal.weeklyRangeScore(goal.dailyCaloriesKcal, low = 0.8, high = 1.1),
                day.diary.totals.proteinGrams.weeklyMinimumScore(goal.proteinGrams, soft = 0.7, strong = 0.9),
                day.diary.detailTotals.fiberGrams.weeklyMinimumScore(goal.fiberGrams, soft = 0.5, strong = 0.8),
                day.diary.detailTotals.sodiumMilligrams.weeklyLimitScore(goal.sodiumMilligrams, soft = 1.0, high = 1.25),
            ).average()
        }
    val score = dayScores.average().roundToInt().coerceIn(0, 100)
    return WeeklyScoreFactor(
        score = score,
        uiState = FoodRatingFactorUiState(
            label = uiText(R.string.food_nutrition_consistency),
            valueLabel = uiText(R.string.food_percentage, UiText.Argument.Integer(score)),
            explanation = when {
                score >= 80 -> uiText(R.string.food_nutrition_consistency_strong)
                score >= 60 -> uiText(R.string.food_nutrition_consistency_mixed)
                else -> uiText(R.string.food_nutrition_consistency_low)
            },
            tone = score.toWeeklyScoreTone(),
        ),
    )
}

private fun buildWeeklyHydrationFactor(days: List<FoodWeeklyDaySummary>): WeeklyScoreFactor {
    val goal = days.sumOf { day -> day.water.goalMilliliters.coerceAtLeast(0.0) }
    val consumed = days.sumOf { day -> day.water.consumedMilliliters.coerceAtLeast(0.0) }
    val score = if (goal > 0.0) {
        ((consumed / goal).coerceIn(0.0, 1.0) * 100.0).roundToInt()
    } else {
        0
    }
    return WeeklyScoreFactor(
        score = score,
        uiState = FoodRatingFactorUiState(
            label = uiText(R.string.food_hydration),
            valueLabel = uiText(R.string.food_percentage, UiText.Argument.Integer(score)),
            explanation = when {
                score >= 90 -> uiText(R.string.food_hydration_consistency_strong)
                score >= 60 -> uiText(R.string.food_hydration_consistency_mixed)
                else -> uiText(R.string.food_hydration_consistency_low)
            },
            tone = score.toWeeklyScoreTone(),
        ),
    )
}

private fun buildWeeklyHabitFactor(days: List<FoodWeeklyDaySummary>): WeeklyScoreFactor {
    val entries =
        days
            .flatMap { day -> day.diary.meals }
            .flatMap { meal -> meal.entries }
            .filter { entry -> entry.status == FoodDiaryEntryStatus.Logged }
    val matchedCount =
        listOf(
            entries.anyHabitKeyword(fruitHabitKeywords),
            entries.anyHabitKeyword(vegetableHabitKeywords),
            entries.anyHabitKeyword(fishHabitKeywords),
        ).count { matched -> matched }
    val score = (matchedCount / 3.0 * 100.0).roundToInt()
    return WeeklyScoreFactor(
        score = score,
        uiState = FoodRatingFactorUiState(
            label = uiText(R.string.food_habits),
            valueLabel = uiText(
                R.string.food_integer_ratio,
                UiText.Argument.Integer(matchedCount),
                UiText.Argument.Integer(3),
            ),
            explanation = when (matchedCount) {
                3 -> uiText(R.string.food_weekly_habits_all)
                2 -> uiText(R.string.food_weekly_habits_two)
                1 -> uiText(R.string.food_weekly_habits_one)
                else -> uiText(R.string.food_weekly_habits_none)
            },
            tone = score.toWeeklyScoreTone(),
        ),
    )
}

private fun List<FoodDiaryEntry>.anyHabitKeyword(keywords: Set<String>): Boolean = any { entry -> entry.matchesHabitKeyword(keywords) }

private fun Double.weeklyRangeScore(goal: Double, low: Double, high: Double): Double {
    if (!isFinite() || !goal.isFinite() || goal <= 0.0) {
        return 0.0
    }
    val ratio = this / goal
    return when {
        ratio in low..high -> 100.0
        ratio >= low * 0.75 && ratio <= high * 1.2 -> 70.0
        else -> 35.0
    }
}

private fun Double.weeklyMinimumScore(goal: Double, soft: Double, strong: Double): Double {
    if (!isFinite() || !goal.isFinite() || goal <= 0.0) {
        return 0.0
    }
    val ratio = this / goal
    return when {
        ratio >= strong -> 100.0
        ratio >= soft -> 70.0
        else -> (ratio / soft * 50.0).coerceIn(0.0, 50.0)
    }
}

private fun Double.weeklyLimitScore(goal: Double, soft: Double, high: Double): Double {
    if (!isFinite() || !goal.isFinite() || goal <= 0.0) {
        return 0.0
    }
    val ratio = this / goal
    return when {
        ratio <= soft -> 100.0
        ratio <= high -> 70.0
        else -> 35.0
    }
}

private fun weeklyScoreSuggestion(nutritionScore: Int, hydrationScore: Int, habitScore: Int): UiText = when {
    hydrationScore < 80 -> uiText(R.string.food_suggestion_raise_water)
    nutritionScore < 80 -> uiText(R.string.food_suggestion_anchor_nutrition)
    habitScore < 67 -> uiText(R.string.food_suggestion_plan_habits)
    else -> uiText(R.string.food_suggestion_repeat_strong_days)
}

private fun Int.toWeeklyScoreTone(): FoodInsightTone = when {
    this >= 80 -> FoodInsightTone.Positive
    this >= 50 -> FoodInsightTone.Neutral
    else -> FoodInsightTone.Warning
}

internal fun emptyWeeklyScore(): FoodWeeklyScoreUiState = FoodWeeklyScoreUiState(
    title = uiText(R.string.food_weekly_musfit_score),
    score = 0,
    summary = uiText(R.string.food_log_to_build_weekly_score),
    suggestion = uiText(R.string.food_track_meals_for_patterns),
    tone = FoodInsightTone.Neutral,
    factors = emptyList(),
)

internal fun emptyProgressStats(): FoodProgressStatsUiState = FoodProgressStatsUiState(
    weekly = emptyProgressPeriod(uiText(R.string.food_last_seven_days)),
    monthly = emptyProgressPeriod(uiText(R.string.food_last_twenty_eight_days)),
)

private fun emptyProgressPeriod(title: UiText): FoodProgressPeriodUiState = FoodProgressPeriodUiState(
    title = title,
    trackedDaysLabel = pluralUiText(
        R.plurals.food_tracked_days,
        0,
        UiText.Argument.Integer(0),
    ),
    metrics = listOf(
        FoodProgressMetricUiState(
            uiText(R.string.food_average_calories),
            uiText(R.string.food_integer_kcal, UiText.Argument.Integer(0)),
        ),
        FoodProgressMetricUiState(
            uiText(R.string.food_average_protein),
            uiText(R.string.food_integer_grams, UiText.Argument.Integer(0)),
        ),
        FoodProgressMetricUiState(
            uiText(R.string.food_calorie_target),
            pluralUiText(
                R.plurals.food_days_ratio,
                0,
                UiText.Argument.Integer(0),
                UiText.Argument.Integer(0),
            ),
        ),
        FoodProgressMetricUiState(
            uiText(R.string.food_hydration),
            pluralUiText(
                R.plurals.food_days_ratio,
                0,
                UiText.Argument.Integer(0),
                UiText.Argument.Integer(0),
            ),
        ),
        FoodProgressMetricUiState(
            uiText(R.string.food_habit_days),
            uiText(
                R.string.food_integer_ratio,
                UiText.Argument.Integer(0),
                UiText.Argument.Integer(0),
            ),
        ),
    ),
    trendLabel = uiText(R.string.food_trend_needs_more_days),
)
