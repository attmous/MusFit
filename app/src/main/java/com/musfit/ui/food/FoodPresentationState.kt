@file:Suppress("TooManyFunctions")

package com.musfit.ui.food

import androidx.compose.runtime.Immutable
import com.musfit.data.repository.FoodDiary
import com.musfit.data.repository.FoodDiaryEntryStatus
import com.musfit.data.repository.FoodDiaryMeal
import com.musfit.data.repository.FoodWaterSummary
import com.musfit.data.repository.NutritionDetails
import com.musfit.domain.model.NutritionTotals
import java.time.LocalDate

/**
 * Presentation state consumed by the Food diary route.
 *
 * Keeping this projection separate from [FoodUiState] prevents editor, search,
 * recipe, and database updates from invalidating the diary surface.
 */
@Immutable
data class FoodDiaryUiState(
    val selectedDate: LocalDate,
    val isSaving: Boolean,
    val message: String?,
    val canUndoDelete: Boolean,
    val calorieGoalKcal: Double,
    val eatenCaloriesKcal: Double,
    val effectiveCalorieBudgetKcal: Double,
    val remainingCaloriesKcal: Double,
    val macroProgress: List<FoodMacroProgressUiState>,
    val advancedNutritionProgress: List<FoodNutrientProgressUiState>,
    val micronutrients: List<FoodMicronutrientUiState>,
    val dailyInsights: List<FoodInsightUiState>,
    val dayRating: FoodRatingUiState,
    val habitTrackers: List<FoodHabitTrackerUiState>,
    val mealSections: List<FoodMealSectionUiState>,
    val waterConsumedMilliliters: Double,
    val waterGoalMilliliters: Double,
)

/** State needed by the water and Health Connect tracker panels only. */
@Immutable
data class FoodTrackerUiState(
    val isSaving: Boolean,
    val waterConsumedMilliliters: Double,
    val waterGoalMilliliters: Double,
    val waterProgress: Double,
    val waterCustomAmountInput: String,
    val waterGoalInput: String,
    val foodHealthConnectSyncEnabled: Boolean,
    val foodHealthConnectCanRequestPermissions: Boolean,
    val foodHealthConnectCanSync: Boolean,
    val foodHealthConnectRequestablePermissions: Set<String>,
    val foodHealthConnectPermissionSummary: String,
    val foodHealthConnectLastFailureMessage: String?,
)

/** Minimal route-coordinator state collected while the diary is visible. */
@Immutable
data class FoodRouteUiState(
    val isAddPanelVisible: Boolean,
    val sheetMode: FoodSheetMode?,
    val selectedMealDetailId: String?,
) {
    val hasActiveSurface: Boolean
        get() = isAddPanelVisible || selectedMealDetailId != null
}

internal object FoodPresentationReducers {
    fun diary(state: FoodUiState): FoodDiaryUiState = FoodDiaryUiState(
        selectedDate = state.selectedDate,
        isSaving = state.isSaving,
        message = state.message,
        canUndoDelete = state.lastDeletedDiaryEntry != null,
        calorieGoalKcal = state.calorieGoalKcal,
        eatenCaloriesKcal = state.eatenCaloriesKcal,
        effectiveCalorieBudgetKcal = state.effectiveCalorieBudgetKcal,
        remainingCaloriesKcal = state.remainingCaloriesKcal,
        macroProgress = state.macroProgress,
        advancedNutritionProgress = state.advancedNutritionProgress,
        micronutrients = state.micronutrients,
        dailyInsights = state.dailyInsights,
        dayRating = state.dayRating,
        habitTrackers = state.habitTrackers,
        mealSections = state.mealSections,
        waterConsumedMilliliters = state.waterConsumedMilliliters,
        waterGoalMilliliters = state.waterGoalMilliliters,
    )

    fun trackers(state: FoodUiState): FoodTrackerUiState = FoodTrackerUiState(
        isSaving = state.isSaving,
        waterConsumedMilliliters = state.waterConsumedMilliliters,
        waterGoalMilliliters = state.waterGoalMilliliters,
        waterProgress = state.waterProgress,
        waterCustomAmountInput = state.waterCustomAmountInput,
        waterGoalInput = state.waterGoalInput,
        foodHealthConnectSyncEnabled = state.foodHealthConnectSyncEnabled,
        foodHealthConnectCanRequestPermissions = state.foodHealthConnectCanRequestPermissions,
        foodHealthConnectCanSync = state.foodHealthConnectCanSync,
        foodHealthConnectRequestablePermissions = state.foodHealthConnectRequestablePermissions,
        foodHealthConnectPermissionSummary = state.foodHealthConnectPermissionSummary,
        foodHealthConnectLastFailureMessage = state.foodHealthConnectLastFailureMessage,
    )

    fun route(state: FoodUiState): FoodRouteUiState = FoodRouteUiState(
        isAddPanelVisible = state.isAddPanelVisible,
        sheetMode = state.sheetMode,
        selectedMealDetailId = state.selectedMealDetailId,
    )

    @Suppress("LongMethod")
    fun favoriteAddItems(state: FoodUiState): List<FavoriteAddItemUiState> = buildList {
        state.savedFoods.filter { it.isFavorite }.forEach { food ->
            add(
                FavoriteAddItemUiState(
                    id = food.id,
                    type = FavoriteAddItemType.Food,
                    title = food.name,
                    subtitle = listOfNotNull(
                        "Food",
                        food.brand,
                        "${food.caloriesPerServingKcal.formatInputNumber()} kcal",
                    ).joinToString(" - "),
                ),
            )
        }
        state.mealTemplates.filter { it.isFavorite }.forEach { template ->
            add(
                FavoriteAddItemUiState(
                    id = template.id,
                    type = FavoriteAddItemType.MealTemplate,
                    title = template.name,
                    subtitle = listOf(
                        "Meal",
                        template.mealType,
                        template.itemSummary.ifBlank { "Saved template" },
                    ).joinToString(" - "),
                ),
            )
        }
        state.recipes.filter { it.isFavorite }.forEach { recipe ->
            add(
                FavoriteAddItemUiState(
                    id = recipe.id,
                    type = FavoriteAddItemType.Recipe,
                    title = recipe.name,
                    subtitle = listOf(
                        "Recipe",
                        "${recipe.caloriesPerServingKcal.formatInputNumber()} kcal",
                        "${recipe.proteinPerServingGrams.formatInputNumber()} g protein",
                    ).joinToString(" - "),
                ),
            )
        }
        state.quickCaloriePresets.filter { it.isFavorite }.forEach { preset ->
            add(
                FavoriteAddItemUiState(
                    id = preset.id,
                    type = FavoriteAddItemType.QuickLog,
                    title = preset.name,
                    subtitle = listOf(
                        "Quick log",
                        "${preset.caloriesKcal.formatInputNumber()} kcal",
                        "P ${preset.proteinGrams.formatInputNumber()}",
                        "C ${preset.carbsGrams.formatInputNumber()}",
                        "F ${preset.fatGrams.formatInputNumber()}",
                    ).joinToString(" - "),
                ),
            )
        }
    }
}

internal fun FoodUiState.withDiary(diary: FoodDiary): FoodUiState = copy(
    eatenCaloriesKcal = diary.totals.caloriesKcal,
    macroProgress = diary.totals.toMacroProgress(
        carbsGoalGrams = carbsGoalGrams,
        proteinGoalGrams = proteinGoalGrams,
        fatGoalGrams = fatGoalGrams,
        fiberGrams = diary.detailTotals.fiberGrams,
        useNetCarbs = useNetCarbs,
    ),
    advancedNutritionProgress = diary.detailTotals.toAdvancedNutritionProgress(
        fiberGoalGrams = fiberGoalGrams,
        sugarGoalGrams = sugarGoalGrams,
        saturatedFatGoalGrams = saturatedFatGoalGrams,
        sodiumGoalMilligrams = sodiumGoalMilligrams,
    ),
    micronutrients = diary.detailTotals.toMicronutrients(),
    dailyInsights = buildDailyInsights(diary),
    dayRating = buildDayRating(diary),
    habitTrackers = buildHabitTrackers(
        diary = diary,
        waterConsumedMilliliters = waterConsumedMilliliters,
        waterGoalMilliliters = waterGoalMilliliters,
    ),
    isFoodDiaryEmpty = diary.isEmptyLoggedDiary(),
    emptyDiaryActions = emptyList(),
    mealSections = diary.toMealSections(
        mealDefinitions = mealDefinitions,
        useNetCarbs = useNetCarbs,
        carbsGoalGrams = carbsGoalGrams,
        proteinGoalGrams = proteinGoalGrams,
        fatGoalGrams = fatGoalGrams,
        fiberGoalGrams = fiberGoalGrams,
        sugarGoalGrams = sugarGoalGrams,
        saturatedFatGoalGrams = saturatedFatGoalGrams,
        sodiumGoalMilligrams = sodiumGoalMilligrams,
    ),
)

internal fun FoodUiState.withWaterSummary(summary: FoodWaterSummary, diary: FoodDiary): FoodUiState = copy(
    waterConsumedMilliliters = summary.consumedMilliliters,
    waterGoalMilliliters = summary.goalMilliliters,
    waterProgress = summary.consumedMilliliters.fractionOf(summary.goalMilliliters),
    waterGoalInput = summary.goalMilliliters.formatInputNumber(),
    habitTrackers = buildHabitTrackers(
        diary = diary,
        waterConsumedMilliliters = summary.consumedMilliliters,
        waterGoalMilliliters = summary.goalMilliliters,
    ),
)

@Suppress("LongMethod")
internal fun FoodUiState.buildDailyInsights(diary: FoodDiary): List<FoodInsightUiState> {
    if (diary.totals.caloriesKcal <= 0.0) {
        return listOf(
            FoodInsightUiState(
                title = "Start with a meal",
                body = "Log a meal, favorite, or quick calories to see today clearly.",
                tone = FoodInsightTone.Neutral,
            ),
        )
    }

    val insights = mutableListOf<FoodInsightUiState>()
    if (diary.detailTotals.sodiumMilligrams > sodiumGoalMilligrams) {
        insights += FoodInsightUiState(
            title = "Sodium is high",
            body = "You are over ${sodiumGoalMilligrams.formatInputNumber()} mg. Choose lower-sodium foods next.",
            tone = FoodInsightTone.Warning,
        )
    }
    if (diary.totals.proteinGrams < proteinGoalGrams * 0.5) {
        val remainingProtein = (proteinGoalGrams - diary.totals.proteinGrams).coerceAtLeast(0.0)
        insights += FoodInsightUiState(
            title = "Protein is low",
            body = "Add about ${remainingProtein.coerceAtMost(35.0).formatInputNumber()} g protein to move toward goal.",
            tone = FoodInsightTone.Warning,
        )
    }
    if (diary.detailTotals.fiberGrams < fiberGoalGrams * 0.5) {
        val remainingFiber = (fiberGoalGrams - diary.detailTotals.fiberGrams).coerceAtLeast(0.0)
        insights += FoodInsightUiState(
            title = "Fiber is below target",
            body = "Add ${remainingFiber.coerceAtMost(10.0).formatInputNumber()} g fiber with fruit, oats, beans, or veg.",
            tone = FoodInsightTone.Warning,
        )
    }

    val balancedMeal = diary.meals.firstOrNull { meal -> meal.isBalancedLoggedMeal() }
    if (balancedMeal != null) {
        insights += FoodInsightUiState(
            title = "${balancedMeal.type.mealTitle()} was balanced",
            body = "Good protein and fiber for this meal.",
            tone = FoodInsightTone.Positive,
        )
    } else if (diary.isBalancedDay(this)) {
        insights += FoodInsightUiState(
            title = "Balanced day",
            body = "Calories, protein, fiber, and sodium are aligned with your goals.",
            tone = FoodInsightTone.Positive,
        )
    }

    val proteinRemaining = (proteinGoalGrams - diary.totals.proteinGrams).coerceAtLeast(0.0)
    val fiberRemaining = (fiberGoalGrams - diary.detailTotals.fiberGrams).coerceAtLeast(0.0)
    val caloriesRemaining = (calorieGoalKcal - diary.totals.caloriesKcal).coerceAtLeast(0.0)
    when {
        proteinRemaining >= 25.0 -> insights += FoodInsightUiState(
            title = "Add protein next",
            body = "A lean protein serving would close the biggest gap.",
            tone = FoodInsightTone.Neutral,
        )

        fiberRemaining >= 8.0 -> insights += FoodInsightUiState(
            title = "Add fiber next",
            body = "A high-fiber side would improve today quickly.",
            tone = FoodInsightTone.Neutral,
        )

        caloriesRemaining >= 300.0 -> insights += FoodInsightUiState(
            title = "Add a balanced meal",
            body = "Use protein plus carbs or veg to finish the day cleanly.",
            tone = FoodInsightTone.Neutral,
        )
    }

    return insights.distinctBy { insight -> insight.title }.ifEmpty {
        listOf(
            FoodInsightUiState(
                title = "Food is on track",
                body = "Today is balanced against your current goals.",
                tone = FoodInsightTone.Positive,
            ),
        )
    }.take(3)
}

internal fun FoodUiState.buildDayRating(diary: FoodDiary): FoodRatingUiState {
    if (diary.totals.caloriesKcal <= 0.0) return emptyFoodRating()

    var score = 100
    val reasons = mutableListOf<String>()
    val suggestions = mutableListOf<String>()
    if (diary.detailTotals.sodiumMilligrams > sodiumGoalMilligrams) {
        score -= 30
        reasons += "High sodium is pulling today down."
        suggestions += "Choose lower-sodium foods for the next meal."
    }
    if (diary.totals.proteinGrams < proteinGoalGrams * 0.6) {
        score -= 30
        reasons += "Protein is well below target."
        suggestions += "Add a protein-forward food next."
    } else if (diary.totals.proteinGrams < proteinGoalGrams * 0.9) {
        score -= 15
        reasons += "Protein is a little short."
        suggestions += "Add a modest protein serving."
    }
    if (diary.detailTotals.fiberGrams < fiberGoalGrams * 0.5) {
        score -= 15
        reasons += "Fiber is low for the day."
        suggestions += "Add fruit, oats, beans, or vegetables."
    }
    if (diary.totals.caloriesKcal > calorieGoalKcal * 1.1) {
        score -= 15
        reasons += "Calories are above goal."
        suggestions += "Keep the next choice lighter."
    } else if (diary.totals.caloriesKcal < calorieGoalKcal * 0.5) {
        score -= 10
        reasons += "Calories are still low."
        suggestions += "Add a balanced meal."
    }

    return FoodRatingUiState(
        label = score.toFoodRatingLabel(),
        reason = reasons.firstOrNull() ?: "Calories, protein, fiber, and sodium are aligned.",
        suggestion = suggestions.firstOrNull() ?: "Keep the same pattern for the next meal.",
        tone = score.toFoodRatingTone(),
        score = score.coerceIn(0, 100),
        factors = buildDayRatingFactors(diary),
    )
}

private fun FoodDiaryMeal.isBalancedLoggedMeal(): Boolean = entries.any { entry -> entry.status == FoodDiaryEntryStatus.Logged } &&
    totals.caloriesKcal in 250.0..800.0 &&
    totals.proteinGrams >= 20.0 &&
    detailTotals.fiberGrams >= 5.0

private fun FoodDiary.isBalancedDay(state: FoodUiState): Boolean = totals.caloriesKcal >= state.calorieGoalKcal * 0.85 &&
    totals.caloriesKcal <= state.calorieGoalKcal * 1.05 &&
    totals.proteinGrams >= state.proteinGoalGrams * 0.9 &&
    detailTotals.fiberGrams >= state.fiberGoalGrams * 0.8 &&
    detailTotals.sodiumMilligrams <= state.sodiumGoalMilligrams

internal fun NutritionTotals.toMacroProgress(
    carbsGoalGrams: Double = CARBS_GOAL_GRAMS,
    proteinGoalGrams: Double = PROTEIN_GOAL_GRAMS,
    fatGoalGrams: Double = FAT_GOAL_GRAMS,
    fiberGrams: Double = 0.0,
    useNetCarbs: Boolean = false,
): List<FoodMacroProgressUiState> = listOf(
    FoodMacroProgressUiState(
        label = if (useNetCarbs) "Net carbs" else "Carbs",
        currentGrams = if (useNetCarbs) (carbsGrams - fiberGrams).coerceAtLeast(0.0) else carbsGrams,
        goalGrams = carbsGoalGrams,
    ),
    FoodMacroProgressUiState("Protein", proteinGrams, proteinGoalGrams),
    FoodMacroProgressUiState("Fat", fatGrams, fatGoalGrams),
)

internal fun NutritionDetails.toAdvancedNutritionProgress(
    fiberGoalGrams: Double = FIBER_GOAL_GRAMS,
    sugarGoalGrams: Double = SUGAR_GOAL_GRAMS,
    saturatedFatGoalGrams: Double = SATURATED_FAT_GOAL_GRAMS,
    sodiumGoalMilligrams: Double = SODIUM_GOAL_MILLIGRAMS,
): List<FoodNutrientProgressUiState> = listOf(
    FoodNutrientProgressUiState("Fiber", fiberGrams, fiberGoalGrams, "g", false),
    FoodNutrientProgressUiState("Sugar", sugarGrams, sugarGoalGrams, "g", true),
    FoodNutrientProgressUiState("Sat fat", saturatedFatGrams, saturatedFatGoalGrams, "g", true),
    FoodNutrientProgressUiState("Sodium", sodiumMilligrams, sodiumGoalMilligrams, "mg", true),
)

internal fun NutritionDetails.toMicronutrients(): List<FoodMicronutrientUiState> = listOf(
    FoodMicronutrientUiState("Sodium", sodiumMilligrams, "mg"),
    FoodMicronutrientUiState("Potassium", potassiumMilligrams, "mg"),
    FoodMicronutrientUiState("Calcium", calciumMilligrams, "mg"),
    FoodMicronutrientUiState("Iron", ironMilligrams, "mg"),
    FoodMicronutrientUiState("Vitamin D", vitaminDMicrograms, "mcg"),
    FoodMicronutrientUiState("Vitamin C", vitaminCMilligrams, "mg"),
    FoodMicronutrientUiState("Magnesium", magnesiumMilligrams, "mg"),
)

internal fun List<SavedFoodUiState>.filterForDatabaseQuery(query: String): List<SavedFoodUiState> {
    val normalizedQuery = query.trim().lowercase()
    if (normalizedQuery.isBlank()) return this
    return filter { food ->
        listOf(food.name, food.brand.orEmpty(), food.barcode.orEmpty(), food.category.orEmpty())
            .any { value -> value.lowercase().contains(normalizedQuery) }
    }
}

internal fun String.sanitizeDecimalInput(): String {
    val trimmed = trim()
    val builder = StringBuilder(trimmed.length)
    var dotSeen = false
    for (char in trimmed) {
        when {
            char.isDigit() -> builder.append(char)

            char == '.' && !dotSeen -> {
                builder.append(char)
                dotSeen = true
            }
        }
    }
    return builder.toString()
}
