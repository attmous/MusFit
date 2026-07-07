package com.musfit.ui.food

import com.musfit.data.repository.FoodDiary
import com.musfit.data.repository.FoodDiaryEntry
import com.musfit.data.repository.FoodDiaryEntryStatus
import com.musfit.data.repository.FoodGoal
import com.musfit.data.repository.FoodProgressSummary
import com.musfit.data.repository.FoodWeeklyDaySummary
import com.musfit.data.repository.FoodWeeklySummary
import kotlin.math.roundToInt

// Nutrition trends: the weekly MusFit score and the 7/28-day progress stats. Moved out of
// FoodViewModel/FoodScreen when Trends graduated from a Food diary tab to a Profile sub-screen.
// The shared habit-keyword sets and `matchesHabitKeyword` stay in FoodViewModel (Summary uses them
// too); this file references them as same-package `internal` members.

data class FoodWeeklyScoreUiState(
    val title: String,
    val score: Int,
    val summary: String,
    val suggestion: String,
    val tone: FoodInsightTone,
    val factors: List<FoodRatingFactorUiState>,
)

data class FoodProgressStatsUiState(
    val weekly: FoodProgressPeriodUiState,
    val monthly: FoodProgressPeriodUiState,
)

data class FoodProgressMetricUiState(
    val caption: String,
    val value: String,
)

data class FoodProgressPeriodUiState(
    val title: String,
    val trackedDaysLabel: String,
    val metrics: List<FoodProgressMetricUiState>,
    val trendLabel: String,
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
            label = "Training signal",
            valueLabel = "Not available",
            explanation = "Food does not have a weekly training signal connected yet.",
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
        title = "Weekly MusFit score",
        score = score,
        summary = "${foodTrackedDays.size} tracked days in this 7-day window.",
        suggestion = weeklyScoreSuggestion(nutritionFactor.score, hydrationFactor.score, habitFactor.score),
        tone = score.toWeeklyScoreTone(),
        factors = factors.map { factor -> factor.uiState },
    )
}

internal fun FoodProgressSummary.toProgressStatsUiState(): FoodProgressStatsUiState {
    val rangeDays = days.take(dayCount)
    return FoodProgressStatsUiState(
        weekly = rangeDays.takeLast(7).toProgressPeriodUiState(title = "Last 7 days", goal = goal),
        monthly = rangeDays.takeLast(28).toProgressPeriodUiState(title = "Last 28 days", goal = goal),
    )
}

private fun List<FoodWeeklyDaySummary>.toProgressPeriodUiState(
    title: String,
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
        trackedDaysLabel = "$trackedCount tracked days",
        metrics = listOf(
            FoodProgressMetricUiState("Avg calories", "${averageCalories.roundToInt()} kcal"),
            FoodProgressMetricUiState("Avg protein", "${averageProtein.roundToInt()} g"),
            FoodProgressMetricUiState("Calorie target", "$calorieTargetDays/$trackedCount days"),
            FoodProgressMetricUiState("Hydration", "$hydrationDays/$trackedCount days"),
            FoodProgressMetricUiState("Habit days", "$habitDays/$trackedCount"),
        ),
        trendLabel = trendLabelForCalories(),
    )
}

private fun List<FoodWeeklyDaySummary>.trendLabelForCalories(): String {
    // Split the tracked days (not the calendar window, which includes untracked days) in half so each
    // half holds a comparable number of real data points regardless of where logging happened to fall.
    val trackedCalories = filter { it.diary.hasTrackedNutrition() }.map { it.diary.totals.caloriesKcal }
    val midpoint = trackedCalories.size / 2
    val firstAverage = trackedCalories.take(midpoint).averageOrNull()
    val secondAverage = trackedCalories.drop(midpoint).averageOrNull()
    if (firstAverage == null || secondAverage == null) {
        return "Trend needs more tracked days"
    }
    val difference = secondAverage - firstAverage
    return when {
        kotlin.math.abs(difference) < 100.0 -> "Calories stable"
        difference > 0.0 -> "Calories trending up"
        else -> "Calories trending down"
    }
}

private fun List<Double>.averageOrZero(): Double =
    averageOrNull() ?: 0.0

private fun List<Double>.averageOrNull(): Double? =
    if (isEmpty()) null else average()

private fun FoodDiary.hasTrackedNutrition(): Boolean =
    totals.caloriesKcal > 0.0 ||
        meals
            .flatMap { meal -> meal.entries }
            .any { entry -> entry.status == FoodDiaryEntryStatus.Logged }

private fun buildWeeklyNutritionFactor(days: List<FoodWeeklyDaySummary>, goal: FoodGoal): WeeklyScoreFactor {
    if (days.isEmpty()) {
        return WeeklyScoreFactor(
            score = 0,
            uiState = FoodRatingFactorUiState(
                label = "Nutrition consistency",
                valueLabel = "0%",
                explanation = "No logged food days in this window.",
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
            label = "Nutrition consistency",
            valueLabel = "$score%",
            explanation = when {
                score >= 80 -> "Most logged days align with calorie, protein, fiber, and sodium targets."
                score >= 60 -> "Several logged days are close, with a few nutrition gaps."
                else -> "Logged days often miss protein, fiber, sodium, or calorie targets."
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
            label = "Hydration",
            valueLabel = "$score%",
            explanation = when {
                score >= 90 -> "Water is consistently close to goal on tracked days."
                score >= 60 -> "Water is partly covered, but there is room to tighten consistency."
                else -> "Water is a clear weekly opportunity."
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
            label = "Habits",
            valueLabel = "$matchedCount / 3",
            explanation = when (matchedCount) {
                3 -> "Fruit, vegetables, and fish all appeared this week."
                2 -> "Two weekly food habits appeared."
                1 -> "One weekly food habit appeared."
                else -> "Fruit, vegetables, and fish were not detected in logged food names."
            },
            tone = score.toWeeklyScoreTone(),
        ),
    )
}

private fun List<FoodDiaryEntry>.anyHabitKeyword(keywords: Set<String>): Boolean =
    any { entry -> entry.matchesHabitKeyword(keywords) }

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

private fun weeklyScoreSuggestion(nutritionScore: Int, hydrationScore: Int, habitScore: Int): String =
    when {
        hydrationScore < 80 -> "Raise water consistency on tracked days first."
        nutritionScore < 80 -> "Anchor the week with protein, fiber, and calmer sodium."
        habitScore < 67 -> "Plan fruit, vegetables, and one fish meal into the week."
        else -> "Keep this pattern and repeat the strongest logged days."
    }

private fun Int.toWeeklyScoreTone(): FoodInsightTone =
    when {
        this >= 80 -> FoodInsightTone.Positive
        this >= 50 -> FoodInsightTone.Neutral
        else -> FoodInsightTone.Warning
    }

internal fun emptyWeeklyScore(): FoodWeeklyScoreUiState =
    FoodWeeklyScoreUiState(
        title = "Weekly MusFit score",
        score = 0,
        summary = "Log food to build a weekly score.",
        suggestion = "Track a few meals and water to see weekly patterns.",
        tone = FoodInsightTone.Neutral,
        factors = emptyList(),
    )

internal fun emptyProgressStats(): FoodProgressStatsUiState =
    FoodProgressStatsUiState(
        weekly = emptyProgressPeriod("Last 7 days"),
        monthly = emptyProgressPeriod("Last 28 days"),
    )

private fun emptyProgressPeriod(title: String): FoodProgressPeriodUiState =
    FoodProgressPeriodUiState(
        title = title,
        trackedDaysLabel = "0 tracked days",
        metrics = listOf(
            FoodProgressMetricUiState("Avg calories", "0 kcal"),
            FoodProgressMetricUiState("Avg protein", "0 g"),
            FoodProgressMetricUiState("Calorie target", "0/0 days"),
            FoodProgressMetricUiState("Hydration", "0/0 days"),
            FoodProgressMetricUiState("Habit days", "0/0"),
        ),
        trendLabel = "Trend needs more tracked days",
    )
