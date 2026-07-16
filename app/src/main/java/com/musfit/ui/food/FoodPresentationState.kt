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

/** Add-food, barcode, saved-food database, and saved-food editor state. */
@Immutable
data class FoodAddDatabaseUiState(
    val content: FoodUiState,
)

/** Diary editors, goals, recipes, meal setup, and planning state. */
@Immutable
data class FoodEditorPlanningUiState(
    val content: FoodUiState,
)

enum class FoodSurfaceGroup {
    AddDatabase,
    EditorPlanning,
    Tracker,
}

/** Minimal route-coordinator state collected while the diary is visible. */
@Immutable
data class FoodRouteUiState(
    val isAddPanelVisible: Boolean,
    val sheetMode: FoodSheetMode?,
    val selectedMealDetailId: String?,
) {
    val hasActiveSurface: Boolean
        get() = isAddPanelVisible || selectedMealDetailId != null

    val surfaceGroup: FoodSurfaceGroup?
        get() = when {
            selectedMealDetailId != null -> FoodSurfaceGroup.EditorPlanning
            !isAddPanelVisible -> null
            sheetMode == FoodSheetMode.Water || sheetMode == FoodSheetMode.HealthConnect -> FoodSurfaceGroup.Tracker
            sheetMode in ADD_DATABASE_SHEET_MODES -> FoodSurfaceGroup.AddDatabase
            else -> FoodSurfaceGroup.EditorPlanning
        }
}

private val ADD_DATABASE_SHEET_MODES = setOf(
    FoodSheetMode.AddFood,
    FoodSheetMode.FoodDatabase,
    FoodSheetMode.FoodDetail,
    FoodSheetMode.SavedFoodEditor,
    FoodSheetMode.NutritionLabelScan,
    FoodSheetMode.BarcodeComparison,
)

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
    fun addDatabase(state: FoodUiState): FoodAddDatabaseUiState = FoodAddDatabaseUiState(
        content = FoodUiState(
            barcode = state.barcode,
            selectedDate = state.selectedDate,
            isLoading = state.isLoading,
            isSaving = state.isSaving,
            message = state.message,
            productName = state.productName,
            brand = state.brand,
            caloriesPer100g = state.caloriesPer100g,
            proteinPer100g = state.proteinPer100g,
            carbsPer100g = state.carbsPer100g,
            fatPer100g = state.fatPer100g,
            fiberPer100g = state.fiberPer100g,
            sugarPer100g = state.sugarPer100g,
            saturatedFatPer100g = state.saturatedFatPer100g,
            sodiumMgPer100g = state.sodiumMgPer100g,
            mealType = state.mealType,
            quantityGrams = state.quantityGrams,
            amountNutritionPreview = state.amountNutritionPreview,
            amountServingChoices = state.amountServingChoices,
            lookupResult = state.lookupResult,
            calorieGoalKcal = state.calorieGoalKcal,
            eatenCaloriesKcal = state.eatenCaloriesKcal,
            isPlanningMode = state.isPlanningMode,
            mealDefinitions = state.mealDefinitions,
            savedFoods = state.savedFoods,
            visibleSavedFoods = state.visibleSavedFoods,
            reportedSavedFoodIds = state.reportedSavedFoodIds,
            duplicateFoodGroups = state.duplicateFoodGroups,
            mealTemplates = state.mealTemplates,
            recipes = state.recipes,
            quickCaloriePresets = state.quickCaloriePresets,
            onlineFoodResults = state.onlineFoodResults,
            isSearchingFoods = state.isSearchingFoods,
            barcodeComparison = state.barcodeComparison,
            isAddPanelVisible = state.isAddPanelVisible,
            sheetMode = state.sheetMode,
            selectedMealTitle = state.selectedMealTitle,
            addMode = state.addMode,
            savedFoodQuantityGrams = state.savedFoodQuantityGrams,
            selectedSavedFoodDetail = state.selectedSavedFoodDetail,
            selectedSavedFoodServingGramsByFoodId = state.selectedSavedFoodServingGramsByFoodId,
            quickCaloriesKcal = state.quickCaloriesKcal,
            quickProteinGrams = state.quickProteinGrams,
            quickCarbsGrams = state.quickCarbsGrams,
            quickFatGrams = state.quickFatGrams,
            aiLoggingText = state.aiLoggingText,
            aiLoggingHasDraft = state.aiLoggingHasDraft,
            aiLoggingDraftSourceLabel = state.aiLoggingDraftSourceLabel,
            aiLoggingDraftReview = state.aiLoggingDraftReview,
            nutritionLabelScanReview = state.nutritionLabelScanReview,
            keepAddingFoods = state.keepAddingFoods,
            foodDatabaseQuery = state.foodDatabaseQuery,
            recentFoods = state.recentFoods,
            sameAsYesterday = state.sameAsYesterday,
            addTab = state.addTab,
            savedFoodEditor = state.savedFoodEditor,
            recipeServingsToLog = state.recipeServingsToLog,
        ),
    )

    @Suppress("LongMethod")
    fun editorPlanning(state: FoodUiState): FoodEditorPlanningUiState = FoodEditorPlanningUiState(
        content = FoodUiState(
            selectedDate = state.selectedDate,
            isLoading = state.isLoading,
            isSaving = state.isSaving,
            message = state.message,
            mealType = state.mealType,
            calorieGoalKcal = state.calorieGoalKcal,
            proteinGoalGrams = state.proteinGoalGrams,
            carbsGoalGrams = state.carbsGoalGrams,
            fatGoalGrams = state.fatGoalGrams,
            fiberGoalGrams = state.fiberGoalGrams,
            sugarGoalGrams = state.sugarGoalGrams,
            saturatedFatGoalGrams = state.saturatedFatGoalGrams,
            sodiumGoalMilligrams = state.sodiumGoalMilligrams,
            goalMode = state.goalMode,
            includeTrainingCalories = state.includeTrainingCalories,
            useNetCarbs = state.useNetCarbs,
            eatenCaloriesKcal = state.eatenCaloriesKcal,
            burnedCaloriesKcal = state.burnedCaloriesKcal,
            foodPrograms = state.foodPrograms,
            mealSections = state.mealSections,
            weeklyPlan = state.weeklyPlan,
            isPlanningMode = state.isPlanningMode,
            shoppingListGroups = state.shoppingListGroups,
            shoppingStartDateInput = state.shoppingStartDateInput,
            shoppingEndDateInput = state.shoppingEndDateInput,
            manualShoppingNameInput = state.manualShoppingNameInput,
            manualShoppingCategoryInput = state.manualShoppingCategoryInput,
            manualShoppingQuantityInput = state.manualShoppingQuantityInput,
            mealDefinitions = state.mealDefinitions,
            selectedMealDetailId = state.selectedMealDetailId,
            mealDetailSortMode = state.mealDetailSortMode,
            savedFoods = state.savedFoods,
            mealTemplates = state.mealTemplates,
            recipes = state.recipes,
            recipeDiscovery = state.recipeDiscovery,
            recipeBrowserDate = state.recipeBrowserDate,
            recipeBrowserMealType = state.recipeBrowserMealType,
            fastingTimer = state.fastingTimer,
            isAddPanelVisible = state.isAddPanelVisible,
            sheetMode = state.sheetMode,
            selectedMealTitle = state.selectedMealTitle,
            recipeServingsToLog = state.recipeServingsToLog,
            diaryEntryEditor = state.diaryEntryEditor,
            mealTemplateEditor = state.mealTemplateEditor,
            goalEditor = state.goalEditor,
            recipeEditor = state.recipeEditor,
            editingMealDefinitionId = state.editingMealDefinitionId,
            customMealNameInput = state.customMealNameInput,
            customMealTimeInput = state.customMealTimeInput,
            customMealSortOrderInput = state.customMealSortOrderInput,
            lastDeletedDiaryEntry = state.lastDeletedDiaryEntry,
        ),
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
