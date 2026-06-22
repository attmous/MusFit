package com.musfit.ui.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musfit.data.remote.food.FoodProductProvider
import com.musfit.data.remote.food.ProductLookupResult
import com.musfit.data.remote.food.ProductSearchResult
import com.musfit.data.repository.DiaryEntryUpdateInput
import com.musfit.data.repository.FoodDiary
import com.musfit.data.repository.FoodDiaryMeal
import com.musfit.data.repository.FoodDiaryEntryStatus
import com.musfit.data.repository.FoodGoal
import com.musfit.data.repository.FoodGoalMode
import com.musfit.data.repository.FoodHealthConnectSyncResult
import com.musfit.data.repository.FoodHealthConnectSyncState
import com.musfit.data.repository.FoodLogInput
import com.musfit.data.repository.FoodMealDefinition
import com.musfit.data.repository.FoodMealDefinitionInput
import com.musfit.data.repository.FoodPlanDay
import com.musfit.data.repository.FoodRepository
import com.musfit.data.repository.FoodServingInput
import com.musfit.data.repository.FoodWaterSummary
import com.musfit.data.repository.MealTemplate
import com.musfit.data.repository.MealTemplateItemInput
import com.musfit.data.repository.MealTemplateUpdateInput
import com.musfit.data.repository.ManualShoppingListItemInput
import com.musfit.data.repository.NutritionDetails
import com.musfit.data.repository.QuickCalorieLogInput
import com.musfit.data.repository.QuickCaloriePreset
import com.musfit.data.repository.QuickCaloriePresetInput
import com.musfit.data.repository.Recipe
import com.musfit.data.repository.RecipeIngredientInput
import com.musfit.data.repository.RecipeUpsertInput
import com.musfit.data.repository.SavedFoodItem
import com.musfit.data.repository.SavedFoodLogInput
import com.musfit.data.repository.SavedFoodUpsertInput
import com.musfit.data.repository.ShoppingListGroup
import com.musfit.data.repository.ShoppingListItem
import com.musfit.data.repository.WaterLogInput
import com.musfit.domain.food.NutritionLabelParser
import com.musfit.domain.health.HealthConnectAvailability
import com.musfit.domain.model.FoodNutrition
import com.musfit.domain.model.NutritionTotals
import com.musfit.domain.nutrition.NutritionCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class FoodAddMode {
    Saved,
    Manual,
    Barcode,
    Quick,
    Ai,
}

enum class AddTab {
    Recents,
    Favorites,
    Create,
}

enum class FoodSheetMode {
    AddFood,
    FoodDatabase,
    FoodDetail,
    DiaryEntryEditor,
    SavedFoodEditor,
    NutritionLabelScan,
    GoalEditor,
    RecipeEditor,
    MealTemplates,
    MealSettings,
    ShoppingList,
}

enum class MealDetailSortMode {
    Logged,
    Calories,
    Protein,
    Name,
}

data class FoodMacroProgressUiState(
    val label: String,
    val currentGrams: Double,
    val goalGrams: Double,
)

data class FoodNutrientProgressUiState(
    val label: String,
    val currentValue: Double,
    val goalValue: Double,
    val unit: String,
    val isLimit: Boolean,
)

data class FoodMicronutrientUiState(
    val label: String,
    val value: Double,
    val unit: String,
)

enum class FoodInsightTone {
    Positive,
    Warning,
    Neutral,
}

data class FoodInsightUiState(
    val title: String,
    val body: String,
    val tone: FoodInsightTone,
)

data class FoodRatingUiState(
    val label: String,
    val reason: String,
    val suggestion: String,
    val tone: FoodInsightTone,
)

enum class EmptyDiaryActionType {
    Breakfast,
    Barcode,
    Ai,
}

data class EmptyDiaryActionUiState(
    val type: EmptyDiaryActionType,
    val label: String,
    val accessibilityLabel: String,
)

data class FoodMealEntryUiState(
    val id: String,
    val foodId: String,
    val name: String,
    val brand: String?,
    val quantityGrams: Double,
    val caloriesKcal: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val fiberGrams: Double = 0.0,
    val sugarGrams: Double = 0.0,
    val saturatedFatGrams: Double = 0.0,
    val sodiumMilligrams: Double = 0.0,
    val calorieContribution: Double = 0.0,
    val proteinContribution: Double = 0.0,
    val carbsContribution: Double = 0.0,
    val fatContribution: Double = 0.0,
    val isPlanned: Boolean = false,
    val imageUrl: String? = null,
)

data class FoodMealSectionUiState(
    val id: String,
    val title: String,
    val recommendation: String,
    val caloriesKcal: Double,
    val calorieTargetKcal: Double,
    val calorieProgress: Double,
    val proteinGrams: Double = 0.0,
    val carbsGrams: Double = 0.0,
    val carbsLabel: String = "Carbs",
    val effectiveCarbsGrams: Double = carbsGrams,
    val fatGrams: Double = 0.0,
    val proteinGoalGrams: Double = PROTEIN_GOAL_GRAMS,
    val carbsGoalGrams: Double = CARBS_GOAL_GRAMS,
    val fatGoalGrams: Double = FAT_GOAL_GRAMS,
    val fiberGrams: Double = 0.0,
    val sugarGrams: Double = 0.0,
    val saturatedFatGrams: Double = 0.0,
    val sodiumMilligrams: Double = 0.0,
    val plannedCaloriesKcal: Double = 0.0,
    val plannedProteinGrams: Double = 0.0,
    val plannedCarbsGrams: Double = 0.0,
    val plannedFatGrams: Double = 0.0,
    val advancedNutritionProgress: List<FoodNutrientProgressUiState> = emptyList(),
    val micronutrients: List<FoodMicronutrientUiState> = emptyList(),
    val rating: FoodRatingUiState? = null,
    val entries: List<FoodMealEntryUiState>,
)

data class FoodPlanDayUiState(
    val date: LocalDate,
    val dayLabel: String,
    val loggedCaloriesKcal: Double,
    val plannedCaloriesKcal: Double,
    val loggedEntryCount: Int,
    val plannedEntryCount: Int,
)

data class ShoppingListItemUiState(
    val id: String,
    val name: String,
    val category: String,
    val quantityGrams: Double,
    val quantityLabel: String,
    val isChecked: Boolean,
    val isManual: Boolean,
)

data class ShoppingListGroupUiState(
    val category: String,
    val items: List<ShoppingListItemUiState>,
)

data class FoodMealDefinitionUiState(
    val id: String,
    val title: String,
    val timeMinutes: Int?,
    val timeLabel: String,
    val sortOrder: Int,
    val isDefault: Boolean,
)

data class SavedFoodUiState(
    val id: String,
    val name: String,
    val brand: String?,
    val defaultServingGrams: Double,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val caloriesPerServingKcal: Double,
    val proteinPerServingGrams: Double,
    val carbsPerServingGrams: Double,
    val fatPerServingGrams: Double,
    val fiberPer100g: Double = 0.0,
    val sugarPer100g: Double = 0.0,
    val saturatedFatPer100g: Double = 0.0,
    val sodiumMgPer100g: Double = 0.0,
    val potassiumMgPer100g: Double = 0.0,
    val calciumMgPer100g: Double = 0.0,
    val ironMgPer100g: Double = 0.0,
    val imageUrl: String? = null,
    val vitaminDMcgPer100g: Double = 0.0,
    val vitaminCMgPer100g: Double = 0.0,
    val magnesiumMgPer100g: Double = 0.0,
    val servingName: String? = null,
    val barcode: String? = null,
    val category: String? = null,
    val isFavorite: Boolean = false,
    val sourceLabel: String = "Manual",
    val servings: List<SavedFoodServingUiState> = emptyList(),
)

data class SavedFoodServingUiState(
    val id: String,
    val label: String,
    val grams: Double,
)

data class FoodDuplicateGroupUiState(
    val primaryFoodId: String,
    val duplicateFoodIds: List<String>,
    val title: String,
    val reason: String,
)

data class OnlineFoodResultUiState(
    val barcode: String,
    val name: String,
    val brand: String?,
    val servingQuantityGrams: Double?,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val fiberPer100g: Double,
    val sugarPer100g: Double,
    val saturatedFatPer100g: Double,
    val sodiumMgPer100g: Double,
    val potassiumMgPer100g: Double,
    val calciumMgPer100g: Double,
    val ironMgPer100g: Double,
    val vitaminDMcgPer100g: Double,
    val vitaminCMgPer100g: Double,
    val magnesiumMgPer100g: Double,
    val category: String?,
    val imageUrl: String?,
)

data class MealTemplateUiState(
    val id: String,
    val name: String,
    val mealType: String,
    val isFavorite: Boolean = false,
    val itemSummary: String,
    val items: List<MealTemplateItemDraftUiState> = emptyList(),
)

data class MealTemplateItemDraftUiState(
    val foodId: String,
    val foodName: String,
    val quantityGrams: String,
)

data class RecipeUiState(
    val id: String,
    val name: String,
    val category: String?,
    val servingName: String,
    val servingGrams: Double,
    val servings: Double,
    val cookedYieldGrams: Double,
    val isFavorite: Boolean = false,
    val caloriesPerServingKcal: Double,
    val proteinPerServingGrams: Double,
    val carbsPerServingGrams: Double,
    val fatPerServingGrams: Double,
    val itemSummary: String,
    val ingredients: List<RecipeIngredientDraftUiState> = emptyList(),
)

data class RecipeIngredientDraftUiState(
    val foodId: String,
    val foodName: String,
    val quantityGrams: Double,
    val unitLabel: String = "g",
    val unitGrams: Double = 1.0,
    val unitQuantity: Double = quantityGrams,
)

data class RecipeIngredientServingChoiceUiState(
    val id: String,
    val label: String,
    val grams: Double,
)

data class QuickCaloriePresetUiState(
    val id: String,
    val name: String,
    val caloriesKcal: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val isFavorite: Boolean,
)

enum class FavoriteAddItemType {
    Food,
    MealTemplate,
    Recipe,
    QuickLog,
}

data class FavoriteAddItemUiState(
    val id: String,
    val type: FavoriteAddItemType,
    val title: String,
    val subtitle: String,
)

data class DeletedDiaryEntrySnapshot(
    val foodId: String,
    val mealType: String,
    val quantityGrams: Double,
)

data class DiaryEntryEditorState(
    val id: String,
    val name: String,
    val mealType: String,
    val quantityGrams: String,
    val servingChoices: List<FoodAmountServingChoiceUiState> = emptyList(),
    val caloriesKcal: Double = 0.0,
    val originalQuantityGrams: Double = 0.0,
    val originalProteinGrams: Double = 0.0,
    val originalCarbsGrams: Double = 0.0,
    val originalFatGrams: Double = 0.0,
    val previewCaloriesKcal: Double = 0.0,
    val previewProteinGrams: Double = 0.0,
    val previewCarbsGrams: Double = 0.0,
    val previewFatGrams: Double = 0.0,
    val isPlanned: Boolean = false,
)

data class GoalEditorState(
    val caloriesKcalInput: String = CALORIE_GOAL_KCAL.formatInputNumber(),
    val proteinGramsInput: String = PROTEIN_GOAL_GRAMS.formatInputNumber(),
    val carbsGramsInput: String = CARBS_GOAL_GRAMS.formatInputNumber(),
    val fatGramsInput: String = FAT_GOAL_GRAMS.formatInputNumber(),
    val fiberGramsInput: String = FIBER_GOAL_GRAMS.formatInputNumber(),
    val sugarGramsInput: String = SUGAR_GOAL_GRAMS.formatInputNumber(),
    val saturatedFatGramsInput: String = SATURATED_FAT_GOAL_GRAMS.formatInputNumber(),
    val sodiumMgInput: String = SODIUM_GOAL_MILLIGRAMS.formatInputNumber(),
    val modeInput: FoodGoalMode = FoodGoalMode.Balanced,
    val includeTrainingInput: Boolean = false,
    val useNetCarbsInput: Boolean = false,
)

data class FoodAmountNutritionPreviewUiState(
    val quantityGrams: Double,
    val caloriesKcal: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
)

data class FoodAmountServingChoiceUiState(
    val id: String,
    val label: String,
    val grams: Double,
)

data class FoodUiState(
    val barcode: String = "",
    val selectedDate: LocalDate = LocalDate.now(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null,
    val productName: String = "",
    val brand: String = "",
    val caloriesPer100g: String = "",
    val proteinPer100g: String = "",
    val carbsPer100g: String = "",
    val fatPer100g: String = "",
    val mealType: String = "breakfast",
    val quantityGrams: String = "100",
    val amountNutritionPreview: FoodAmountNutritionPreviewUiState? = null,
    val amountServingChoices: List<FoodAmountServingChoiceUiState> = emptyList(),
    val lookupResult: ProductLookupResult.Found? = null,
    val calorieGoalKcal: Double = CALORIE_GOAL_KCAL,
    val proteinGoalGrams: Double = PROTEIN_GOAL_GRAMS,
    val carbsGoalGrams: Double = CARBS_GOAL_GRAMS,
    val fatGoalGrams: Double = FAT_GOAL_GRAMS,
    val fiberGoalGrams: Double = FIBER_GOAL_GRAMS,
    val sugarGoalGrams: Double = SUGAR_GOAL_GRAMS,
    val saturatedFatGoalGrams: Double = SATURATED_FAT_GOAL_GRAMS,
    val sodiumGoalMilligrams: Double = SODIUM_GOAL_MILLIGRAMS,
    val goalMode: FoodGoalMode = FoodGoalMode.Balanced,
    val includeTrainingCalories: Boolean = false,
    val useNetCarbs: Boolean = false,
    val eatenCaloriesKcal: Double = 0.0,
    val remainingCaloriesKcal: Double = CALORIE_GOAL_KCAL,
    val macroProgress: List<FoodMacroProgressUiState> = emptyMacroProgress(),
    val advancedNutritionProgress: List<FoodNutrientProgressUiState> = emptyAdvancedNutritionProgress(),
    val micronutrients: List<FoodMicronutrientUiState> = emptyMicronutrients(),
    val dailyInsights: List<FoodInsightUiState> = emptyDailyInsights(),
    val dayRating: FoodRatingUiState = emptyFoodRating(),
    val isFoodDiaryEmpty: Boolean = true,
    val emptyDiaryActions: List<EmptyDiaryActionUiState> = defaultEmptyDiaryActions(),
    val mealSections: List<FoodMealSectionUiState> = emptyMealSections(),
    val weeklyPlan: List<FoodPlanDayUiState> = emptyList(),
    val isPlanningMode: Boolean = false,
    val waterConsumedMilliliters: Double = 0.0,
    val waterGoalMilliliters: Double = WATER_GOAL_MILLILITERS,
    val waterProgress: Double = 0.0,
    val waterCustomAmountInput: String = "",
    val waterGoalInput: String = WATER_GOAL_MILLILITERS.formatInputNumber(),
    val foodHealthConnectSyncEnabled: Boolean = false,
    val foodHealthConnectCanRequestPermissions: Boolean = false,
    val foodHealthConnectCanSync: Boolean = false,
    val foodHealthConnectRequestablePermissions: Set<String> = emptySet(),
    val foodHealthConnectPermissionSummary: String = "Health Connect unavailable",
    val foodHealthConnectLastSyncAtEpochMillis: Long? = null,
    val foodHealthConnectLastFailureMessage: String? = null,
    val shoppingListGroups: List<ShoppingListGroupUiState> = emptyList(),
    val shoppingStartDateInput: String = LocalDate.now().toString(),
    val shoppingEndDateInput: String = LocalDate.now().plusDays(6).toString(),
    val manualShoppingNameInput: String = "",
    val manualShoppingCategoryInput: String = "",
    val manualShoppingQuantityInput: String = "1",
    val mealDefinitions: List<FoodMealDefinitionUiState> = defaultMealDefinitionUiStates(),
    val selectedMealDetailId: String? = null,
    val mealDetailSortMode: MealDetailSortMode = MealDetailSortMode.Logged,
    val savedFoods: List<SavedFoodUiState> = emptyList(),
    val visibleSavedFoods: List<SavedFoodUiState> = emptyList(),
    val duplicateFoodGroups: List<FoodDuplicateGroupUiState> = emptyList(),
    val mealTemplates: List<MealTemplateUiState> = emptyList(),
    val recipes: List<RecipeUiState> = emptyList(),
    val quickCaloriePresets: List<QuickCaloriePresetUiState> = emptyList(),
    val onlineFoodResults: List<OnlineFoodResultUiState> = emptyList(),
    val isSearchingFoods: Boolean = false,
    val isAddPanelVisible: Boolean = false,
    val sheetMode: FoodSheetMode? = null,
    val selectedMealTitle: String = "Breakfast",
    val addMode: FoodAddMode = FoodAddMode.Saved,
    val savedFoodQuantityGrams: String = "100",
    val selectedSavedFoodDetail: SavedFoodUiState? = null,
    val selectedSavedFoodServingGramsByFoodId: Map<String, Double> = emptyMap(),
    val quickCaloriesKcal: String = "",
    val quickProteinGrams: String = "",
    val quickCarbsGrams: String = "",
    val quickFatGrams: String = "",
    val aiLoggingText: String = "",
    val aiLoggingHasDraft: Boolean = false,
    val aiLoggingDraftSourceLabel: String? = null,
    val keepAddingFoods: Boolean = false,
    val foodDatabaseQuery: String = "",
    val recentFoods: List<SavedFoodUiState> = emptyList(),
    val sameAsYesterday: List<SavedFoodUiState> = emptyList(),
    val addTab: AddTab = AddTab.Recents,
    val diaryEntryEditor: DiaryEntryEditorState? = null,
    val editingSavedFoodId: String? = null,
    val savedFoodName: String = "",
    val savedFoodBrand: String = "",
    val savedFoodServingGrams: String = "100",
    val savedFoodCaloriesPer100g: String = "",
    val savedFoodProteinPer100g: String = "",
    val savedFoodCarbsPer100g: String = "",
    val savedFoodFatPer100g: String = "",
    val savedFoodFiberPer100g: String = "",
    val savedFoodSugarPer100g: String = "",
    val savedFoodSaturatedFatPer100g: String = "",
    val savedFoodSodiumMgPer100g: String = "",
    val savedFoodPotassiumMgPer100g: String = "",
    val savedFoodCalciumMgPer100g: String = "",
    val savedFoodIronMgPer100g: String = "",
    val savedFoodVitaminDMcgPer100g: String = "",
    val savedFoodVitaminCMgPer100g: String = "",
    val savedFoodMagnesiumMgPer100g: String = "",
    val savedFoodServingName: String = "",
    val savedFoodBarcode: String = "",
    val savedFoodCategory: String = "",
    val savedFoodIsFavorite: Boolean = false,
    val editingTemplateId: String? = null,
    val templateNameInput: String = "",
    val templateMealTypeInput: String = "breakfast",
    val templateItemsInput: List<MealTemplateItemDraftUiState> = emptyList(),
    val templateItemFoodId: String = "",
    val templateItemQuantityGrams: String = "100",
    val goalEditor: GoalEditorState = GoalEditorState(),
    val editingRecipeId: String? = null,
    val recipeName: String = "",
    val recipeCategory: String = "",
    val recipeServingName: String = "Serving",
    val recipeServingGrams: String = "100",
    val recipeServingsCount: String = "1",
    val recipeCookedYieldGrams: String = "100",
    val recipeIngredientFoodId: String = "",
    val recipeIngredientServingChoiceId: String = "g",
    val recipeIngredientServingChoices: List<RecipeIngredientServingChoiceUiState> = emptyList(),
    val recipeIngredientQuantityGrams: String = "100",
    val recipeIngredients: List<RecipeIngredientDraftUiState> = emptyList(),
    val recipeServingsToLog: String = "1",
    val editingMealDefinitionId: String? = null,
    val customMealNameInput: String = "",
    val customMealTimeInput: String = "",
    val customMealSortOrderInput: String = "",
    val lastDeletedDiaryEntry: DeletedDiaryEntrySnapshot? = null,
) {
    val favoriteAddItems: List<FavoriteAddItemUiState>
        get() =
            buildList {
                savedFoods
                    .filter { it.isFavorite }
                    .forEach { food ->
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
                mealTemplates
                    .filter { it.isFavorite }
                    .forEach { template ->
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
                recipes
                    .filter { it.isFavorite }
                    .forEach { recipe ->
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
                quickCaloriePresets
                    .filter { it.isFavorite }
                    .forEach { preset ->
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FoodViewModel @Inject constructor(
    private val provider: FoodProductProvider,
    private val repository: FoodRepository,
) : ViewModel() {
    private val selectedDateFlow = MutableStateFlow(LocalDate.now())
    private val mutableState = MutableStateFlow(FoodUiState())
    val state: StateFlow<FoodUiState> = mutableState.asStateFlow()
    private var lookupJob: Job? = null
    private var currentDiary: FoodDiary = emptyFoodDiary()

    init {
        viewModelScope.launch {
            selectedDateFlow.flatMapLatest { date ->
                repository.observeFoodDiary(date)
            }.collect { diary ->
                currentDiary = diary
                mutableState.update { currentState -> currentState.withDiary(diary) }
            }
        }
        viewModelScope.launch {
            selectedDateFlow.flatMapLatest { date ->
                repository.observeFoodPlan(date)
            }.collect { planDays ->
                mutableState.update { currentState ->
                    currentState.copy(weeklyPlan = planDays.map { it.toUiState() })
                }
            }
        }
        viewModelScope.launch {
            selectedDateFlow.flatMapLatest { date ->
                repository.observeWaterSummary(date)
            }.collect { summary ->
                mutableState.update { currentState -> currentState.withWaterSummary(summary) }
            }
        }
        viewModelScope.launch {
            repository.observeFoodHealthConnectSyncState().collect { syncState ->
                mutableState.update { currentState -> currentState.withFoodHealthConnectSyncState(syncState) }
            }
        }
        viewModelScope.launch {
            repository.observeRecentFoods().collect { recents ->
                mutableState.update { it.copy(recentFoods = recents.map { food -> food.toUiState() }) }
            }
        }
        viewModelScope.launch {
            repository.observeSavedFoods().collect { savedFoods ->
                mutableState.update { currentState ->
                    val foodStates = savedFoods.map { it.toUiState() }
                    currentState.copy(
                        savedFoods = foodStates,
                        visibleSavedFoods = foodStates.filterForDatabaseQuery(currentState.foodDatabaseQuery),
                        duplicateFoodGroups = foodStates.toDuplicateFoodGroups(),
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.observeFoodGoal().collect { goal ->
                mutableState.update { currentState -> currentState.withFoodGoal(goal).withDiary(currentDiary) }
            }
        }
        viewModelScope.launch {
            repository.observeMealTemplates().collect { templates ->
                mutableState.update { currentState ->
                    currentState.copy(mealTemplates = templates.map { it.toUiState() })
                }
            }
        }
        viewModelScope.launch {
            repository.observeRecipes().collect { recipes ->
                mutableState.update { currentState ->
                    currentState.copy(recipes = recipes.map { it.toUiState() })
                }
            }
        }
        viewModelScope.launch {
            repository.observeQuickCaloriePresets().collect { presets ->
                mutableState.update { currentState ->
                    currentState.copy(quickCaloriePresets = presets.map { it.toUiState() })
                }
            }
        }
        viewModelScope.launch {
            repository.observeShoppingList().collect { groups ->
                mutableState.update { currentState ->
                    currentState.copy(shoppingListGroups = groups.map { it.toUiState() })
                }
            }
        }
        viewModelScope.launch {
            repository.observeCustomMealDefinitions().collect { definitions ->
                mutableState.update { currentState ->
                    val mealDefinitions = definitions.toMealDefinitionUiStates()
                    val updatedState = currentState.copy(mealDefinitions = mealDefinitions)
                    updatedState
                        .copy(selectedMealTitle = updatedState.mealTitleFor(updatedState.mealType))
                        .withDiary(currentDiary)
                }
            }
        }
        refreshFoodHealthConnectSync()
    }

    fun goToPreviousDay() {
        setSelectedDate(state.value.selectedDate.minusDays(1))
    }

    fun goToNextDay() {
        setSelectedDate(state.value.selectedDate.plusDays(1))
    }

    fun goToToday() {
        setSelectedDate(LocalDate.now())
    }

    private fun setSelectedDate(date: LocalDate) {
        selectedDateFlow.value = date
        mutableState.update {
            it.copy(
                selectedDate = date,
                selectedMealDetailId = null,
                message = null,
            )
        }
    }

    fun togglePlanningMode() {
        mutableState.update { currentState ->
            currentState.copy(
                isPlanningMode = !currentState.isPlanningMode,
                message = null,
            )
        }
    }

    fun copySelectedDayToTomorrow() {
        val currentState = state.value
        if (!markSaving()) {
            return
        }
        viewModelScope.launch {
            try {
                repository.copyDay(
                    fromDate = currentState.selectedDate,
                    toDate = currentState.selectedDate.plusDays(1),
                    status = FoodDiaryEntryStatus.Planned,
                )
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        message = "Planned tomorrow from this day",
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        message = error.message ?: "Failed to copy day",
                    )
                }
            }
        }
    }

    fun logQuickWater(amountMilliliters: Double) {
        logWaterAmount(amountMilliliters, clearCustomAmount = false)
    }

    fun onWaterCustomAmountChanged(value: String) {
        mutableState.update { it.copy(waterCustomAmountInput = value.sanitizeDecimalInput(), message = null) }
    }

    fun logCustomWater() {
        val amount = state.value.waterCustomAmountInput.parsePositiveNumberOrNull()
        if (amount == null) {
            mutableState.update { it.copy(message = "Enter a valid water amount") }
            return
        }
        logWaterAmount(amount, clearCustomAmount = true)
    }

    fun onWaterGoalChanged(value: String) {
        mutableState.update { it.copy(waterGoalInput = value.sanitizeDecimalInput(), message = null) }
    }

    fun saveWaterGoal() {
        val goalMilliliters = state.value.waterGoalInput.parsePositiveNumberOrNull()
        if (goalMilliliters == null) {
            mutableState.update { it.copy(message = "Enter a valid water goal") }
            return
        }
        if (!markSaving()) {
            return
        }

        viewModelScope.launch {
            try {
                repository.updateWaterGoal(goalMilliliters)
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        waterGoalMilliliters = goalMilliliters,
                        waterProgress = it.waterConsumedMilliliters.fractionOf(goalMilliliters),
                        waterGoalInput = goalMilliliters.formatInputNumber(),
                        message = "Updated water goal",
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(isSaving = false, message = error.message ?: "Failed to update water goal") }
            }
        }
    }

    private fun logWaterAmount(amountMilliliters: Double, clearCustomAmount: Boolean) {
        if (!amountMilliliters.isFinite() || amountMilliliters <= 0.0) {
            mutableState.update { it.copy(message = "Enter a valid water amount") }
            return
        }
        val date = state.value.selectedDate
        if (!markSaving()) {
            return
        }

        viewModelScope.launch {
            try {
                repository.logWater(WaterLogInput(date = date, amountMilliliters = amountMilliliters))
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        waterCustomAmountInput = if (clearCustomAmount) "" else it.waterCustomAmountInput,
                        message = "Added ${amountMilliliters.formatInputNumber()} ml water",
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(isSaving = false, message = error.message ?: "Failed to log water") }
            }
        }
    }

    fun refreshFoodHealthConnectSync() {
        viewModelScope.launch {
            try {
                val syncState = repository.refreshFoodHealthConnectSyncState()
                mutableState.update { currentState ->
                    currentState.withFoodHealthConnectSyncState(syncState)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(message = error.message ?: "Failed to refresh Health Connect")
                }
            }
        }
    }

    fun onFoodHealthConnectSyncEnabledChanged(isEnabled: Boolean) {
        mutableState.update {
            it.copy(
                foodHealthConnectSyncEnabled = isEnabled,
                message = null,
            )
        }
        viewModelScope.launch {
            try {
                repository.setFoodHealthConnectSyncEnabled(isEnabled)
                val syncState = repository.refreshFoodHealthConnectSyncState()
                mutableState.update { currentState ->
                    currentState
                        .withFoodHealthConnectSyncState(syncState)
                        .copy(
                            message = if (isEnabled) {
                                "Food Health Connect sync enabled"
                            } else {
                                "Food Health Connect sync disabled"
                            },
                        )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(message = error.message ?: "Failed to update Health Connect sync")
                }
            }
        }
    }

    fun syncFoodToHealthConnect() {
        val date = state.value.selectedDate
        if (!markSaving()) {
            return
        }
        viewModelScope.launch {
            try {
                val result = repository.syncFoodToHealthConnect(date)
                val syncState = repository.refreshFoodHealthConnectSyncState()
                mutableState.update { currentState ->
                    currentState
                        .withFoodHealthConnectSyncState(syncState)
                        .copy(
                            isSaving = false,
                            message = result.toFoodHealthConnectMessage(),
                        )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        message = error.message ?: "Failed to sync Food to Health Connect",
                    )
                }
            }
        }
    }

    fun openAddFood(mealType: String) {
        val normalizedMealType = mealType.normalizedMealType()
        mutableState.update { currentState ->
            currentState.copy(
                isAddPanelVisible = true,
                sheetMode = FoodSheetMode.AddFood,
                mealType = normalizedMealType,
                selectedMealTitle = currentState.mealTitleFor(normalizedMealType),
                addMode = FoodAddMode.Saved,
                addTab = AddTab.Recents,
                message = null,
            )
        }
        viewModelScope.launch {
            val items = repository.observeSameAsYesterday(normalizedMealType, state.value.selectedDate).first()
            mutableState.update { it.copy(sameAsYesterday = items.map { food -> food.toUiState() }) }
        }
    }

    fun selectAddTab(tab: AddTab) {
        mutableState.update { it.copy(addTab = tab) }
    }

    fun closeAddFood() {
        mutableState.update {
            it.copy(
                isAddPanelVisible = false,
                sheetMode = null,
                message = null,
                diaryEntryEditor = null,
                editingSavedFoodId = null,
                editingTemplateId = null,
                editingRecipeId = null,
                editingMealDefinitionId = null,
                selectedSavedFoodDetail = null,
            )
        }
    }

    fun selectAddMode(mode: FoodAddMode) {
        mutableState.update { it.copy(addMode = mode, message = null) }
    }

    fun onAiLoggingTextChanged(value: String) {
        mutableState.update {
            it.copy(
                aiLoggingText = value,
                message = null,
            )
        }
    }

    fun generateAiTextFoodDraft() {
        val text = state.value.aiLoggingText.trim()
        if (text.isBlank()) {
            mutableState.update { it.copy(message = "Describe what you ate") }
            return
        }
        mutableState.update {
            it.withAiLoggingDraft(
                name = text.take(80),
                sourceLabel = "Text",
                message = "Review AI suggestion before logging.",
            )
        }
    }

    fun startAiVoiceLoggingPlaceholder() {
        mutableState.update {
            it.withAiLoggingDraft(
                name = "Voice draft",
                sourceLabel = "Voice",
                message = "Voice placeholder ready. Review before logging.",
            )
        }
    }

    fun startAiPhotoLoggingPlaceholder() {
        mutableState.update {
            it.withAiLoggingDraft(
                name = "Photo draft",
                sourceLabel = "Photo",
                message = "Photo placeholder ready. Review before logging.",
            )
        }
    }

    fun onKeepAddingFoodsChanged(value: Boolean) {
        mutableState.update { it.copy(keepAddingFoods = value, message = null) }
    }

    fun openMealDetail(mealType: String) {
        mutableState.update {
            it.copy(
                selectedMealDetailId = mealType.normalizedMealType(),
                message = null,
            )
        }
    }

    fun closeMealDetail() {
        mutableState.update {
            it.copy(
                selectedMealDetailId = null,
                message = null,
            )
        }
    }

    fun onMealDetailSortChanged(sortMode: MealDetailSortMode) {
        mutableState.update {
            it.copy(
                mealDetailSortMode = sortMode,
                message = null,
            )
        }
    }

    fun openAddFoodFromMealDetail() {
        val selectedMealType = state.value.selectedMealDetailId ?: return
        openAddFood(selectedMealType)
    }

    fun openFoodDatabase() {
        mutableState.update {
            it.copy(
                isAddPanelVisible = true,
                sheetMode = FoodSheetMode.FoodDatabase,
                message = null,
                diaryEntryEditor = null,
                editingSavedFoodId = null,
                selectedSavedFoodDetail = null,
            )
        }
    }

    fun openGoalEditor() {
        mutableState.update { it.copy(isAddPanelVisible = true, sheetMode = FoodSheetMode.GoalEditor, message = null) }
    }

    fun openMealTemplates() {
        mutableState.update { it.copy(isAddPanelVisible = true, sheetMode = FoodSheetMode.MealTemplates, message = null) }
    }

    fun openMealSettings() {
        mutableState.update {
            it.copy(
                isAddPanelVisible = true,
                sheetMode = FoodSheetMode.MealSettings,
                editingMealDefinitionId = null,
                customMealNameInput = "",
                customMealTimeInput = "",
                customMealSortOrderInput = it.nextMealSortOrder().toString(),
                message = null,
            )
        }
    }

    fun openShoppingList() {
        mutableState.update {
            it.copy(
                isAddPanelVisible = true,
                sheetMode = FoodSheetMode.ShoppingList,
                message = null,
            )
        }
    }

    fun onShoppingStartDateChanged(value: String) {
        mutableState.update { it.copy(shoppingStartDateInput = value.trim(), message = null) }
    }

    fun onShoppingEndDateChanged(value: String) {
        mutableState.update { it.copy(shoppingEndDateInput = value.trim(), message = null) }
    }

    fun onManualShoppingNameChanged(value: String) {
        mutableState.update { it.copy(manualShoppingNameInput = value, message = null) }
    }

    fun onManualShoppingCategoryChanged(value: String) {
        mutableState.update { it.copy(manualShoppingCategoryInput = value, message = null) }
    }

    fun onManualShoppingQuantityChanged(value: String) {
        mutableState.update { it.copy(manualShoppingQuantityInput = value.sanitizeDecimalInput(), message = null) }
    }

    fun generateShoppingList() {
        val currentState = state.value
        val startDate = currentState.shoppingStartDateInput.parseDateOrNull()
        val endDate = currentState.shoppingEndDateInput.parseDateOrNull()
        if (startDate == null || endDate == null) {
            mutableState.update { it.copy(message = "Enter dates as yyyy-MM-dd") }
            return
        }
        if (endDate.isBefore(startDate)) {
            mutableState.update { it.copy(message = "End date must be after start date") }
            return
        }
        if (!markSaving()) {
            return
        }

        viewModelScope.launch {
            try {
                val groups = repository.generateShoppingList(startDate, endDate)
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        shoppingListGroups = groups.map { group -> group.toUiState() },
                        message = "Shopping list generated",
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(isSaving = false, message = error.message ?: "Failed to generate shopping list")
                }
            }
        }
    }

    fun addManualShoppingListItem() {
        val currentState = state.value
        val name = currentState.manualShoppingNameInput.trim()
        val quantity = currentState.manualShoppingQuantityInput.parsePositiveNumberOrNull()
        if (name.isBlank()) {
            mutableState.update { it.copy(message = "Enter an item name") }
            return
        }
        if (quantity == null) {
            mutableState.update { it.copy(message = "Enter a valid quantity") }
            return
        }
        if (!markSaving()) {
            return
        }

        viewModelScope.launch {
            try {
                repository.addManualShoppingListItem(
                    ManualShoppingListItemInput(
                        name = name,
                        category = currentState.manualShoppingCategoryInput.trim(),
                        quantityGrams = quantity,
                    ),
                )
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        manualShoppingNameInput = "",
                        manualShoppingCategoryInput = "",
                        manualShoppingQuantityInput = "1",
                        message = "Added shopping item",
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(isSaving = false, message = error.message ?: "Failed to add shopping item")
                }
            }
        }
    }

    fun toggleShoppingListItem(itemId: String, isChecked: Boolean) {
        viewModelScope.launch {
            try {
                repository.toggleShoppingListItem(itemId, isChecked)
                mutableState.update { it.copy(message = "Updated shopping item") }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(message = error.message ?: "Failed to update shopping item") }
            }
        }
    }

    fun openMealDefinitionEditor(mealId: String) {
        val meal = state.value.mealDefinitions.firstOrNull { it.id == mealId.normalizedMealType() }
        if (meal == null) {
            mutableState.update { it.copy(message = "Meal not found") }
            return
        }
        mutableState.update {
            it.copy(
                isAddPanelVisible = true,
                sheetMode = FoodSheetMode.MealSettings,
                editingMealDefinitionId = meal.id,
                customMealNameInput = meal.title,
                customMealTimeInput = meal.timeMinutes?.toMealTimeLabel().orEmpty(),
                customMealSortOrderInput = meal.sortOrder.toString(),
                message = null,
            )
        }
    }

    fun onCustomMealNameChanged(value: String) {
        mutableState.update { it.copy(customMealNameInput = value, message = null) }
    }

    fun onCustomMealTimeChanged(value: String) {
        mutableState.update { it.copy(customMealTimeInput = value.take(5), message = null) }
    }

    fun onCustomMealSortOrderChanged(value: String) {
        mutableState.update { it.copy(customMealSortOrderInput = value.filter(Char::isDigit), message = null) }
    }

    fun saveCustomMealDefinition() {
        val currentState = state.value
        val mealName = currentState.customMealNameInput.trim()
        if (mealName.isBlank()) {
            mutableState.update { it.copy(message = "Enter a meal name") }
            return
        }
        val timeMinutes = currentState.customMealTimeInput.parseMealTimeMinutesOrNull()
        if (currentState.customMealTimeInput.isNotBlank() && timeMinutes == null) {
            mutableState.update { it.copy(message = "Enter meal time as HH:mm") }
            return
        }
        val sortOrder = currentState.customMealSortOrderInput.toIntOrNull()
        if (sortOrder == null) {
            mutableState.update { it.copy(message = "Enter a meal order") }
            return
        }
        if (!markSaving()) {
            return
        }

        viewModelScope.launch {
            try {
                repository.upsertCustomMealDefinition(
                    FoodMealDefinitionInput(
                        mealId = currentState.editingMealDefinitionId,
                        name = mealName,
                        timeMinutes = timeMinutes,
                        sortOrder = sortOrder,
                    ),
                )
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        editingMealDefinitionId = null,
                        customMealNameInput = "",
                        customMealTimeInput = "",
                        customMealSortOrderInput = it.nextMealSortOrder().toString(),
                        message = if (currentState.editingMealDefinitionId == null) {
                            "Saved custom meal"
                        } else {
                            "Saved meal"
                        },
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        message = error.message ?: "Failed to save meal",
                    )
                }
            }
        }
    }

    fun openRecipeEditor(recipeId: String? = null) {
        val recipe = recipeId?.let { id -> state.value.recipes.firstOrNull { it.id == id } }
        mutableState.update {
            if (recipe == null) {
                it.copy(
                    isAddPanelVisible = true,
                    sheetMode = FoodSheetMode.RecipeEditor,
                    editingRecipeId = null,
                    recipeName = "",
                    recipeCategory = "",
                    recipeServingName = "Serving",
                    recipeServingGrams = "100",
                    recipeServingsCount = "1",
                    recipeCookedYieldGrams = "100",
                    recipeIngredientFoodId = "",
                    recipeIngredientServingChoiceId = "g",
                    recipeIngredientServingChoices = emptyList(),
                    recipeIngredientQuantityGrams = "100",
                    recipeIngredients = emptyList(),
                    message = null,
                )
            } else {
                it.copy(
                    isAddPanelVisible = true,
                    sheetMode = FoodSheetMode.RecipeEditor,
                    editingRecipeId = recipe.id,
                    recipeName = recipe.name,
                    recipeCategory = recipe.category.orEmpty(),
                    recipeServingName = recipe.servingName,
                    recipeServingGrams = recipe.servingGrams.formatInputNumber(),
                    recipeServingsCount = recipe.servings.formatInputNumber(),
                    recipeCookedYieldGrams = recipe.cookedYieldGrams.formatInputNumber(),
                    recipeIngredientFoodId = "",
                    recipeIngredientServingChoiceId = "g",
                    recipeIngredientServingChoices = emptyList(),
                    recipeIngredientQuantityGrams = "100",
                    recipeIngredients = recipe.ingredients,
                    message = null,
                )
            }
        }
    }

    fun onFoodDatabaseQueryChanged(value: String) {
        mutableState.update {
            it.copy(
                foodDatabaseQuery = value,
                visibleSavedFoods = it.savedFoods.filterForDatabaseQuery(value),
                message = null,
            )
        }
    }

    fun searchOnlineFoods() {
        val query = state.value.foodDatabaseQuery.trim()
        if (query.length < 2) {
            mutableState.update { it.copy(message = "Enter at least 2 characters", onlineFoodResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(isSearchingFoods = true, message = null) }
            try {
                when (val result = provider.searchProducts(query, pageSize = 12)) {
                    is ProductSearchResult.Success -> {
                        mutableState.update { currentState ->
                            if (currentState.foodDatabaseQuery.trim() != query) {
                                currentState.copy(isSearchingFoods = false)
                            } else {
                                currentState.copy(
                                    isSearchingFoods = false,
                                    onlineFoodResults = result.products.map { it.toOnlineUiState() },
                                    message = if (result.products.isEmpty()) "No online foods found" else null,
                                )
                            }
                        }
                    }
                    is ProductSearchResult.Failed -> {
                        mutableState.update {
                            it.copy(
                                isSearchingFoods = false,
                                onlineFoodResults = emptyList(),
                                message = result.message,
                            )
                        }
                    }
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSearchingFoods = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        isSearchingFoods = false,
                        onlineFoodResults = emptyList(),
                        message = "Online food search failed",
                    )
                }
            }
        }
    }

    fun saveOnlineFoodResult(barcode: String) {
        val currentState = state.value
        val result = currentState.onlineFoodResults.firstOrNull { it.barcode == barcode }
        if (result == null) {
            mutableState.update { it.copy(message = "Online food not found") }
            return
        }
        if (!markSaving()) {
            return
        }

        viewModelScope.launch {
            try {
                repository.upsertSavedFood(result.toSavedFoodUpsertInput())
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        sheetMode = FoodSheetMode.FoodDatabase,
                        message = "Saved online food",
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        message = error.message ?: "Failed to save online food",
                    )
                }
            }
        }
    }

    fun openSavedFoodDetail(foodId: String) {
        val savedFood = state.value.savedFoods.firstOrNull { it.id == foodId }
        if (savedFood == null) {
            mutableState.update { it.copy(message = "Saved food not found") }
            return
        }

        mutableState.update {
            it.copy(
                isAddPanelVisible = true,
                sheetMode = FoodSheetMode.FoodDetail,
                selectedSavedFoodDetail = savedFood,
                message = null,
            )
        }
    }

    fun onSavedFoodServingSelected(foodId: String, grams: Double) {
        if (!grams.isFinite() || grams <= 0.0) {
            mutableState.update { it.copy(message = "Enter a valid amount") }
            return
        }
        mutableState.update {
            it.copy(
                selectedSavedFoodServingGramsByFoodId = it.selectedSavedFoodServingGramsByFoodId + (foodId to grams),
                savedFoodQuantityGrams = grams.formatInputNumber(),
                message = null,
            )
        }
    }

    fun openMealTemplateEditor(templateId: String) {
        val template = state.value.mealTemplates.firstOrNull { it.id == templateId }
        if (template == null) {
            mutableState.update { it.copy(message = "Template not found") }
            return
        }
        mutableState.update {
            it.copy(
                isAddPanelVisible = true,
                sheetMode = FoodSheetMode.MealTemplates,
                editingTemplateId = template.id,
                templateNameInput = template.name,
                templateMealTypeInput = template.mealType,
                templateItemsInput = template.items,
                templateItemFoodId = "",
                templateItemQuantityGrams = "100",
                message = null,
            )
        }
    }

    fun onTemplateNameChanged(value: String) {
        mutableState.update { it.copy(templateNameInput = value, message = null) }
    }

    fun onTemplateMealTypeChanged(value: String) {
        mutableState.update { it.copy(templateMealTypeInput = value.normalizedMealType(), message = null) }
    }

    fun onTemplateDraftItemQuantityChanged(index: Int, value: String) {
        mutableState.update { currentState ->
            if (index !in currentState.templateItemsInput.indices) {
                currentState
            } else {
                currentState.copy(
                    templateItemsInput = currentState.templateItemsInput.mapIndexed { itemIndex, item ->
                        if (itemIndex == index) {
                            item.copy(quantityGrams = value.sanitizeDecimalInput())
                        } else {
                            item
                        }
                    },
                    message = null,
                )
            }
        }
    }

    fun removeTemplateDraftItem(index: Int) {
        mutableState.update { currentState ->
            if (index !in currentState.templateItemsInput.indices) {
                currentState
            } else {
                currentState.copy(
                    templateItemsInput = currentState.templateItemsInput.filterIndexed { itemIndex, _ -> itemIndex != index },
                    message = null,
                )
            }
        }
    }

    fun onTemplateItemFoodChanged(value: String) {
        val food = state.value.savedFoods.firstOrNull { it.id == value }
        mutableState.update {
            it.copy(
                templateItemFoodId = value,
                templateItemQuantityGrams = food?.defaultServingGrams?.formatInputNumber() ?: it.templateItemQuantityGrams,
                message = null,
            )
        }
    }

    fun onTemplateNewItemQuantityChanged(value: String) {
        mutableState.update { it.copy(templateItemQuantityGrams = value.sanitizeDecimalInput(), message = null) }
    }

    fun addTemplateItem() {
        val currentState = state.value
        val food = currentState.savedFoods.firstOrNull { it.id == currentState.templateItemFoodId }
        if (food == null) {
            mutableState.update { it.copy(message = "Choose a food") }
            return
        }
        val quantity = currentState.templateItemQuantityGrams.parsePositiveNumberOrNull()
        if (quantity == null) {
            mutableState.update { it.copy(message = "Enter item amount") }
            return
        }
        mutableState.update {
            it.copy(
                templateItemsInput = it.templateItemsInput + MealTemplateItemDraftUiState(
                    foodId = food.id,
                    foodName = food.name,
                    quantityGrams = quantity.formatInputNumber(),
                ),
                templateItemFoodId = "",
                templateItemQuantityGrams = "100",
                message = null,
            )
        }
    }

    fun saveMealTemplateEdits() {
        val currentState = state.value
        val templateId = currentState.editingTemplateId
        if (templateId == null) {
            mutableState.update { it.copy(message = "Choose a template") }
            return
        }
        if (currentState.templateNameInput.isBlank()) {
            mutableState.update { it.copy(message = "Enter a template name") }
            return
        }
        val items =
            currentState.templateItemsInput.mapNotNull { item ->
                item.quantityGrams.parsePositiveNumberOrNull()?.let { quantity ->
                    MealTemplateItemInput(
                        foodId = item.foodId,
                        quantityGrams = quantity,
                    )
                }
            }
        if (items.size != currentState.templateItemsInput.size || items.isEmpty()) {
            mutableState.update { it.copy(message = "Add at least one valid template item") }
            return
        }
        if (!markSaving()) {
            return
        }
        viewModelScope.launch {
            try {
                repository.updateMealTemplate(
                    MealTemplateUpdateInput(
                        templateId = templateId,
                        name = currentState.templateNameInput,
                        mealType = currentState.templateMealTypeInput,
                        items = items,
                    ),
                )
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        editingTemplateId = null,
                        templateNameInput = "",
                        templateItemsInput = emptyList(),
                        templateItemFoodId = "",
                        templateItemQuantityGrams = "100",
                        message = "Updated meal template",
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        message = error.message ?: "Failed to update template",
                    )
                }
            }
        }
    }

    fun duplicateMealTemplate(templateId: String) {
        val template = state.value.mealTemplates.firstOrNull { it.id == templateId }
        if (template == null) {
            mutableState.update { it.copy(message = "Template not found") }
            return
        }
        if (!markSaving()) {
            return
        }
        viewModelScope.launch {
            try {
                repository.duplicateMealTemplate(templateId, "${template.name} copy")
                mutableState.update { it.copy(isSaving = false, message = "Duplicated meal template") }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(isSaving = false, message = error.message ?: "Failed to duplicate template") }
            }
        }
    }

    fun deleteMealTemplate(templateId: String) {
        if (!markSaving()) {
            return
        }
        viewModelScope.launch {
            try {
                repository.deleteMealTemplate(templateId)
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        editingTemplateId = null,
                        templateNameInput = "",
                        templateItemsInput = emptyList(),
                        templateItemFoodId = "",
                        templateItemQuantityGrams = "100",
                        message = "Deleted meal template",
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(isSaving = false, message = error.message ?: "Failed to delete template") }
            }
        }
    }

    fun toggleFavoriteMealTemplate(templateId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            try {
                repository.toggleFavoriteMealTemplate(templateId, isFavorite)
                mutableState.update {
                    it.copy(
                        message = if (isFavorite) {
                            "Template added to favorites"
                        } else {
                            "Template removed from favorites"
                        },
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(message = error.message ?: "Failed to update template favorite") }
            }
        }
    }

    fun openDiaryEntryEditor(entryId: String) {
        val currentState = state.value
        val sectionAndEntry = currentState.mealSections.firstNotNullOfOrNull { section ->
            section.entries.firstOrNull { entry -> entry.id == entryId }?.let { entry -> section to entry }
        }
        if (sectionAndEntry == null) {
            mutableState.update { it.copy(message = "Diary item not found") }
            return
        }

        val (section, entry) = sectionAndEntry
        val servingChoices =
            currentState.savedFoods
                .firstOrNull { savedFood -> savedFood.id == entry.foodId }
                ?.toDiaryEntryServingChoices()
                ?: defaultAmountServingChoices()
        mutableState.update {
            it.copy(
                isAddPanelVisible = true,
                sheetMode = FoodSheetMode.DiaryEntryEditor,
                message = null,
                diaryEntryEditor = DiaryEntryEditorState(
                    id = entry.id,
                    name = entry.name,
                    mealType = section.id,
                    quantityGrams = entry.quantityGrams.formatInputNumber(),
                    servingChoices = servingChoices,
                    caloriesKcal = entry.caloriesKcal,
                    originalQuantityGrams = entry.quantityGrams,
                    originalProteinGrams = entry.proteinGrams,
                    originalCarbsGrams = entry.carbsGrams,
                    originalFatGrams = entry.fatGrams,
                    previewCaloriesKcal = entry.caloriesKcal,
                    previewProteinGrams = entry.proteinGrams,
                    previewCarbsGrams = entry.carbsGrams,
                    previewFatGrams = entry.fatGrams,
                    isPlanned = entry.isPlanned,
                ),
            )
        }
    }

    fun onDiaryEntryMealChanged(value: String) {
        mutableState.update {
            it.copy(
                diaryEntryEditor = it.diaryEntryEditor?.copy(mealType = value.normalizedMealType()),
                message = null,
            )
        }
    }

    fun onDiaryEntryQuantityChanged(value: String) {
        mutableState.update {
            it.copy(
                diaryEntryEditor = it.diaryEntryEditor?.copy(quantityGrams = value.sanitizeDecimalInput()),
                message = null,
            ).withDiaryEntryPreview()
        }
    }

    fun onDiaryEntryServingChoiceSelected(choiceId: String) {
        mutableState.update { currentState ->
            val choice = currentState.diaryEntryEditor?.servingChoices?.firstOrNull { choice -> choice.id == choiceId }
            if (choice == null) {
                currentState.copy(message = "Serving not found")
            } else {
                currentState.copy(
                    diaryEntryEditor = currentState.diaryEntryEditor?.copy(quantityGrams = choice.grams.formatInputNumber()),
                    message = null,
                ).withDiaryEntryPreview()
            }
        }
    }

    fun saveDiaryEntry() {
        val currentState = state.value
        if (currentState.isSaving) {
            return
        }
        val editor = currentState.diaryEntryEditor
        if (editor == null) {
            mutableState.update { it.copy(message = "Choose a diary item") }
            return
        }
        val mealItemId = editor.id
        val quantityGrams = editor.quantityGrams.parsePositiveNumberOrNull()
        if (quantityGrams == null) {
            mutableState.update { it.copy(message = "Enter a valid amount") }
            return
        }
        if (!markSaving()) {
            return
        }

        viewModelScope.launch {
            try {
                repository.updateDiaryEntry(
                    DiaryEntryUpdateInput(
                        mealItemId = mealItemId,
                        mealType = editor.mealType,
                        quantityGrams = quantityGrams,
                        date = currentState.selectedDate,
                    ),
                )
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        isAddPanelVisible = false,
                        sheetMode = null,
                        diaryEntryEditor = null,
                        message = "Updated diary item",
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        message = error.message ?: "Failed to update diary item",
                    )
                }
            }
        }
    }

    fun deleteDiaryEntry() {
        val currentState = state.value
        if (currentState.isSaving) {
            return
        }
        val mealItemId = currentState.diaryEntryEditor?.id
        if (mealItemId == null) {
            mutableState.update { it.copy(message = "Choose a diary item") }
            return
        }
        val deletedSnapshot = currentState.mealSections.firstNotNullOfOrNull { section ->
            section.entries.firstOrNull { entry -> entry.id == mealItemId }?.let { entry ->
                DeletedDiaryEntrySnapshot(
                    foodId = entry.foodId,
                    mealType = section.id,
                    quantityGrams = entry.quantityGrams,
                )
            }
        }
        if (!markSaving()) {
            return
        }

        viewModelScope.launch {
            try {
                repository.deleteDiaryEntry(mealItemId)
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        isAddPanelVisible = false,
                        sheetMode = null,
                        diaryEntryEditor = null,
                        lastDeletedDiaryEntry = deletedSnapshot,
                        message = "Deleted diary item",
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        message = error.message ?: "Failed to delete diary item",
                    )
                }
            }
        }
    }

    fun undoDeleteDiaryEntry() {
        val currentState = state.value
        val snapshot = currentState.lastDeletedDiaryEntry
        if (snapshot == null) {
            mutableState.update { it.copy(message = "Nothing to restore") }
            return
        }
        if (!markSaving()) {
            return
        }

        viewModelScope.launch {
            try {
                repository.logSavedFood(
                    SavedFoodLogInput(
                        foodId = snapshot.foodId,
                        mealType = snapshot.mealType,
                        quantityGrams = snapshot.quantityGrams,
                        date = currentState.selectedDate,
                    ),
                )
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        lastDeletedDiaryEntry = null,
                        message = "Restored diary item",
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        message = error.message ?: "Failed to restore diary item",
                    )
                }
            }
        }
    }

    fun copyDiaryEntryTo(mealType: String, date: LocalDate) {
        val currentState = state.value
        val mealItemId = currentState.diaryEntryEditor?.id
        if (mealItemId == null) {
            mutableState.update { it.copy(message = "Choose a diary item") }
            return
        }
        if (!markSaving()) {
            return
        }

        viewModelScope.launch {
            try {
                repository.copyDiaryEntry(
                    mealItemId = mealItemId,
                    mealType = mealType.normalizedMealType(),
                    date = date,
                )
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        isAddPanelVisible = false,
                        sheetMode = null,
                        diaryEntryEditor = null,
                        message = "Copied diary item",
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        message = error.message ?: "Failed to copy diary item",
                    )
                }
            }
        }
    }

    fun markDiaryEntryLogged() {
        val currentState = state.value
        val editor = currentState.diaryEntryEditor
        if (editor == null) {
            mutableState.update { it.copy(message = "Choose a diary item") }
            return
        }
        val mealItemId = editor.id
        if (!editor.isPlanned) {
            mutableState.update { it.copy(message = "Diary item is already logged") }
            return
        }
        if (!markSaving()) {
            return
        }

        viewModelScope.launch {
            try {
                repository.markDiaryEntryLogged(mealItemId)
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        isAddPanelVisible = false,
                        sheetMode = null,
                        diaryEntryEditor = null,
                        message = "Logged planned food",
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        message = error.message ?: "Failed to log planned food",
                    )
                }
            }
        }
    }

    fun openNewSavedFoodEditor() {
        mutableState.update {
            it.copy(
                isAddPanelVisible = true,
                sheetMode = FoodSheetMode.SavedFoodEditor,
                message = null,
                editingSavedFoodId = null,
                savedFoodName = "",
                savedFoodBrand = "",
                savedFoodServingGrams = "100",
                savedFoodCaloriesPer100g = "",
                savedFoodProteinPer100g = "",
                savedFoodCarbsPer100g = "",
                savedFoodFatPer100g = "",
                savedFoodFiberPer100g = "",
                savedFoodSugarPer100g = "",
                savedFoodSaturatedFatPer100g = "",
                savedFoodSodiumMgPer100g = "",
                savedFoodPotassiumMgPer100g = "",
                savedFoodCalciumMgPer100g = "",
                savedFoodIronMgPer100g = "",
                savedFoodVitaminDMcgPer100g = "",
                savedFoodVitaminCMgPer100g = "",
                savedFoodMagnesiumMgPer100g = "",
                savedFoodServingName = "",
                savedFoodBarcode = "",
                savedFoodCategory = "",
                savedFoodIsFavorite = false,
                selectedSavedFoodDetail = null,
            )
        }
    }

    fun openNutritionLabelScan() {
        mutableState.update {
            it.copy(
                isAddPanelVisible = true,
                sheetMode = FoodSheetMode.NutritionLabelScan,
                message = "Review extracted nutrition before saving.",
                editingSavedFoodId = null,
                savedFoodName = "Scanned label",
                savedFoodBrand = "",
                savedFoodServingGrams = "100",
                savedFoodCaloriesPer100g = "250",
                savedFoodProteinPer100g = "12",
                savedFoodCarbsPer100g = "30",
                savedFoodFatPer100g = "8",
                savedFoodFiberPer100g = "",
                savedFoodSugarPer100g = "",
                savedFoodSaturatedFatPer100g = "",
                savedFoodSodiumMgPer100g = "",
                savedFoodPotassiumMgPer100g = "",
                savedFoodCalciumMgPer100g = "",
                savedFoodIronMgPer100g = "",
                savedFoodVitaminDMcgPer100g = "",
                savedFoodVitaminCMgPer100g = "",
                savedFoodMagnesiumMgPer100g = "",
                savedFoodServingName = "Label serving",
                savedFoodBarcode = "",
                savedFoodCategory = "Nutrition label",
                savedFoodIsFavorite = false,
                selectedSavedFoodDetail = null,
            )
        }
    }

    fun openSavedFoodEditor(foodId: String) {
        val savedFood = state.value.savedFoods.firstOrNull { it.id == foodId }
        if (savedFood == null) {
            mutableState.update { it.copy(message = "Saved food not found") }
            return
        }

        mutableState.update {
            it.copy(
                isAddPanelVisible = true,
                sheetMode = FoodSheetMode.SavedFoodEditor,
                message = null,
                editingSavedFoodId = savedFood.id,
                savedFoodName = savedFood.name,
                savedFoodBrand = savedFood.brand.orEmpty(),
                savedFoodServingGrams = savedFood.defaultServingGrams.formatInputNumber(),
                savedFoodCaloriesPer100g = savedFood.caloriesPer100g.formatInputNumber(),
                savedFoodProteinPer100g = savedFood.proteinPer100g.formatInputNumber(),
                savedFoodCarbsPer100g = savedFood.carbsPer100g.formatInputNumber(),
                savedFoodFatPer100g = savedFood.fatPer100g.formatInputNumber(),
                savedFoodFiberPer100g = savedFood.fiberPer100g.formatInputNumber(),
                savedFoodSugarPer100g = savedFood.sugarPer100g.formatInputNumber(),
                savedFoodSaturatedFatPer100g = savedFood.saturatedFatPer100g.formatInputNumber(),
                savedFoodSodiumMgPer100g = savedFood.sodiumMgPer100g.formatInputNumber(),
                savedFoodPotassiumMgPer100g = savedFood.potassiumMgPer100g.formatInputNumber(),
                savedFoodCalciumMgPer100g = savedFood.calciumMgPer100g.formatInputNumber(),
                savedFoodIronMgPer100g = savedFood.ironMgPer100g.formatInputNumber(),
                savedFoodVitaminDMcgPer100g = savedFood.vitaminDMcgPer100g.formatInputNumber(),
                savedFoodVitaminCMgPer100g = savedFood.vitaminCMgPer100g.formatInputNumber(),
                savedFoodMagnesiumMgPer100g = savedFood.magnesiumMgPer100g.formatInputNumber(),
                savedFoodServingName = savedFood.servingName.orEmpty(),
                savedFoodBarcode = savedFood.barcode.orEmpty(),
                savedFoodCategory = savedFood.category.orEmpty(),
                savedFoodIsFavorite = savedFood.isFavorite,
                selectedSavedFoodDetail = null,
            )
        }
    }

    fun onSavedFoodNameChanged(value: String) {
        mutableState.update { it.copy(savedFoodName = value, message = null) }
    }

    fun onSavedFoodBrandChanged(value: String) {
        mutableState.update { it.copy(savedFoodBrand = value, message = null) }
    }

    fun onSavedFoodServingChanged(value: String) {
        mutableState.update { it.copy(savedFoodServingGrams = value.sanitizeDecimalInput(), message = null) }
    }

    fun onSavedFoodCaloriesChanged(value: String) {
        mutableState.update { it.copy(savedFoodCaloriesPer100g = value.sanitizeDecimalInput(), message = null) }
    }

    fun onSavedFoodProteinChanged(value: String) {
        mutableState.update { it.copy(savedFoodProteinPer100g = value.sanitizeDecimalInput(), message = null) }
    }

    fun onSavedFoodCarbsChanged(value: String) {
        mutableState.update { it.copy(savedFoodCarbsPer100g = value.sanitizeDecimalInput(), message = null) }
    }

    fun onSavedFoodFatChanged(value: String) {
        mutableState.update { it.copy(savedFoodFatPer100g = value.sanitizeDecimalInput(), message = null) }
    }

    fun onSavedFoodFiberChanged(value: String) {
        mutableState.update { it.copy(savedFoodFiberPer100g = value.sanitizeDecimalInput(), message = null) }
    }

    fun onSavedFoodSugarChanged(value: String) {
        mutableState.update { it.copy(savedFoodSugarPer100g = value.sanitizeDecimalInput(), message = null) }
    }

    fun onSavedFoodSaturatedFatChanged(value: String) {
        mutableState.update { it.copy(savedFoodSaturatedFatPer100g = value.sanitizeDecimalInput(), message = null) }
    }

    fun onSavedFoodSodiumChanged(value: String) {
        mutableState.update { it.copy(savedFoodSodiumMgPer100g = value.sanitizeDecimalInput(), message = null) }
    }

    fun onSavedFoodPotassiumChanged(value: String) {
        mutableState.update { it.copy(savedFoodPotassiumMgPer100g = value.sanitizeDecimalInput(), message = null) }
    }

    fun onSavedFoodCalciumChanged(value: String) {
        mutableState.update { it.copy(savedFoodCalciumMgPer100g = value.sanitizeDecimalInput(), message = null) }
    }

    fun onSavedFoodIronChanged(value: String) {
        mutableState.update { it.copy(savedFoodIronMgPer100g = value.sanitizeDecimalInput(), message = null) }
    }

    fun onSavedFoodVitaminDChanged(value: String) {
        mutableState.update { it.copy(savedFoodVitaminDMcgPer100g = value.sanitizeDecimalInput(), message = null) }
    }

    fun onSavedFoodVitaminCChanged(value: String) {
        mutableState.update { it.copy(savedFoodVitaminCMgPer100g = value.sanitizeDecimalInput(), message = null) }
    }

    fun onSavedFoodMagnesiumChanged(value: String) {
        mutableState.update { it.copy(savedFoodMagnesiumMgPer100g = value.sanitizeDecimalInput(), message = null) }
    }

    fun onSavedFoodServingNameChanged(value: String) {
        mutableState.update { it.copy(savedFoodServingName = value, message = null) }
    }

    fun onSavedFoodBarcodeChanged(value: String) {
        mutableState.update { it.copy(savedFoodBarcode = value.filter { char -> char.isDigit() }, message = null) }
    }

    fun onSavedFoodCategoryChanged(value: String) {
        mutableState.update { it.copy(savedFoodCategory = value, message = null) }
    }

    fun onSavedFoodFavoriteChanged(value: Boolean) {
        mutableState.update { it.copy(savedFoodIsFavorite = value, message = null) }
    }

    fun saveSavedFood() {
        val currentState = state.value
        if (currentState.isSaving) {
            return
        }
        val foodName = currentState.savedFoodName.trim()
        if (foodName.isBlank()) {
            mutableState.update { it.copy(message = "Enter a food name") }
            return
        }
        val servingGrams = currentState.savedFoodServingGrams.parsePositiveNumberOrNull()
        val nutrition = currentState.toSavedFoodNutritionOrNull()
        val nutritionDetails = currentState.toSavedFoodNutritionDetailsOrNull()
        if (servingGrams == null || nutrition == null || nutritionDetails == null) {
            mutableState.update { it.copy(message = "Enter valid nutrition values") }
            return
        }
        if (!markSaving()) {
            return
        }

        viewModelScope.launch {
            try {
                repository.upsertSavedFood(
                    SavedFoodUpsertInput(
                        foodId = currentState.editingSavedFoodId,
                        name = foodName,
                        brand = currentState.savedFoodBrand.ifBlank { null },
                        defaultServingGrams = servingGrams,
                        nutritionPer100g = nutrition,
                        nutritionDetailsPer100g = nutritionDetails,
                        servingName = currentState.savedFoodServingName.ifBlank { null },
                        barcode = currentState.savedFoodBarcode.ifBlank { null },
                        category = currentState.savedFoodCategory.ifBlank { null },
                        isFavorite = currentState.savedFoodIsFavorite,
                        servings = currentState.savedFoodServingInputsForUpsert(servingGrams),
                    ),
                )
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        isAddPanelVisible = true,
                        sheetMode = FoodSheetMode.FoodDatabase,
                        editingSavedFoodId = null,
                        message = "Saved food",
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        message = error.message ?: "Failed to save food",
                    )
                }
            }
        }
    }

    fun deleteSavedFood() {
        val currentState = state.value
        if (currentState.isSaving) {
            return
        }
        val foodId = currentState.editingSavedFoodId
        if (foodId == null) {
            mutableState.update { it.copy(message = "Choose a saved food") }
            return
        }
        if (!markSaving()) {
            return
        }

        viewModelScope.launch {
            try {
                repository.deleteSavedFood(foodId)
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        isAddPanelVisible = true,
                        sheetMode = FoodSheetMode.FoodDatabase,
                        editingSavedFoodId = null,
                        message = "Deleted food",
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        message = error.message ?: "Failed to delete food",
                    )
                }
            }
        }
    }

    fun duplicateSavedFood() {
        val currentState = state.value
        if (currentState.isSaving) {
            return
        }
        val foodId = currentState.editingSavedFoodId
        if (foodId == null) {
            mutableState.update { it.copy(message = "Choose a saved food") }
            return
        }
        val savedFood = currentState.savedFoods.firstOrNull { it.id == foodId }
        if (savedFood == null) {
            mutableState.update { it.copy(message = "Saved food not found") }
            return
        }
        if (!markSaving()) {
            return
        }

        viewModelScope.launch {
            try {
                repository.upsertSavedFood(
                    SavedFoodUpsertInput(
                        foodId = null,
                        name = "${savedFood.name} copy",
                        brand = savedFood.brand,
                        defaultServingGrams = savedFood.defaultServingGrams,
                        nutritionPer100g = FoodNutrition(
                            caloriesKcal = savedFood.caloriesPer100g,
                            proteinGrams = savedFood.proteinPer100g,
                            carbsGrams = savedFood.carbsPer100g,
                            fatGrams = savedFood.fatPer100g,
                        ),
                        nutritionDetailsPer100g = NutritionDetails(
                            fiberGrams = savedFood.fiberPer100g,
                            sugarGrams = savedFood.sugarPer100g,
                            saturatedFatGrams = savedFood.saturatedFatPer100g,
                            sodiumMilligrams = savedFood.sodiumMgPer100g,
                            potassiumMilligrams = savedFood.potassiumMgPer100g,
                            calciumMilligrams = savedFood.calciumMgPer100g,
                            ironMilligrams = savedFood.ironMgPer100g,
                            vitaminDMicrograms = savedFood.vitaminDMcgPer100g,
                            vitaminCMilligrams = savedFood.vitaminCMgPer100g,
                            magnesiumMilligrams = savedFood.magnesiumMgPer100g,
                        ),
                        servingName = savedFood.servingName,
                        barcode = null,
                        category = savedFood.category,
                        isFavorite = savedFood.isFavorite,
                        servings = savedFood.servings.map { serving ->
                            FoodServingInput(serving.label, serving.grams)
                        },
                    ),
                )
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        isAddPanelVisible = true,
                        sheetMode = FoodSheetMode.FoodDatabase,
                        editingSavedFoodId = null,
                        message = "Duplicated food",
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        message = error.message ?: "Failed to duplicate food",
                    )
                }
            }
        }
    }

    fun mergeDuplicateFoods(primaryFoodId: String, duplicateFoodIds: List<String>) {
        if (state.value.isSaving) {
            return
        }
        if (primaryFoodId.isBlank() || duplicateFoodIds.isEmpty()) {
            mutableState.update { it.copy(message = "Choose duplicate foods to merge") }
            return
        }
        if (!markSaving()) {
            return
        }

        viewModelScope.launch {
            try {
                repository.mergeDuplicateFoods(primaryFoodId, duplicateFoodIds)
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        message = "Merged duplicate foods",
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        message = error.message ?: "Failed to merge foods",
                    )
                }
            }
        }
    }

    fun toggleFavoriteFood(foodId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            try {
                repository.toggleFavoriteFood(foodId, isFavorite)
                mutableState.update { it.copy(message = if (isFavorite) "Added to favorites" else "Removed from favorites") }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(message = error.message ?: "Failed to update favorite") }
            }
        }
    }

    fun seedStarterFoods() {
        if (!markSaving()) {
            return
        }
        viewModelScope.launch {
            try {
                repository.seedStarterFoods()
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        message = "Imported starter foods",
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        message = error.message ?: "Failed to import foods",
                    )
                }
            }
        }
    }

    fun onBarcodeChanged(value: String) {
        val sanitized = value.filter(Char::isDigit)
        val barcodeChanged = sanitized != state.value.barcode
        if (barcodeChanged) {
            lookupJob?.cancel()
        }
        mutableState.update {
            if (sanitized == it.barcode) {
                it.copy(message = null)
            } else {
                it.clearedEditableFields().copy(
                    barcode = sanitized,
                    isLoading = false,
                    message = null,
                )
            }
        }
    }

    /** Entry point when the barcode scanner returns: show the Create tab and auto-look-up the product. */
    fun onScannedBarcode(barcode: String) {
        onBarcodeChanged(barcode)
        mutableState.update {
            it.copy(
                isAddPanelVisible = true,
                sheetMode = FoodSheetMode.AddFood,
                addMode = FoodAddMode.Saved,
                addTab = AddTab.Create,
                message = null,
            )
        }
        lookupBarcode()
    }

    /** Entry point when the label scanner returns recognized text: parse it and autofill the Create form. */
    fun onScannedLabel(rawText: String) {
        val parsed = NutritionLabelParser.parse(rawText)
        mutableState.update {
            it.copy(
                isAddPanelVisible = true,
                sheetMode = FoodSheetMode.AddFood,
                addMode = FoodAddMode.Saved,
                addTab = AddTab.Create,
                caloriesPer100g = parsed.caloriesKcal?.formatInputNumber() ?: it.caloriesPer100g,
                proteinPer100g = parsed.proteinGrams?.formatInputNumber() ?: it.proteinPer100g,
                carbsPer100g = parsed.carbsGrams?.formatInputNumber() ?: it.carbsPer100g,
                fatPer100g = parsed.fatGrams?.formatInputNumber() ?: it.fatPer100g,
                message = if (parsed.hasAnyValue) {
                    "Review the scanned values below."
                } else {
                    "Couldn't read the label — enter the values manually."
                },
            ).withAmountNutritionPreview()
        }
    }

    fun onProductNameChanged(value: String) {
        mutableState.update { it.copy(productName = value, message = null) }
    }

    fun onBrandChanged(value: String) {
        mutableState.update { it.copy(brand = value, message = null) }
    }

    fun onCaloriesChanged(value: String) {
        mutableState.update {
            it.copy(caloriesPer100g = value, message = null).withAmountNutritionPreview()
        }
    }

    fun onProteinChanged(value: String) {
        mutableState.update {
            it.copy(proteinPer100g = value, message = null).withAmountNutritionPreview()
        }
    }

    fun onCarbsChanged(value: String) {
        mutableState.update {
            it.copy(carbsPer100g = value, message = null).withAmountNutritionPreview()
        }
    }

    fun onFatChanged(value: String) {
        mutableState.update {
            it.copy(fatPer100g = value, message = null).withAmountNutritionPreview()
        }
    }

    fun onMealTypeChanged(value: String) {
        mutableState.update {
            it.copy(
                mealType = value,
                selectedMealTitle = it.mealTitleFor(value),
                message = null,
            )
        }
    }

    fun onQuantityChanged(value: String) {
        mutableState.update {
            it.copy(quantityGrams = value.sanitizeDecimalInput(), message = null).withAmountNutritionPreview()
        }
    }

    fun onAmountServingChoiceSelected(choiceId: String) {
        mutableState.update { currentState ->
            val choice = currentState.amountServingChoices.firstOrNull { it.id == choiceId }
            if (choice == null) {
                currentState.copy(message = "Serving choice not found")
            } else {
                currentState.copy(
                    quantityGrams = choice.grams.formatInputNumber(),
                    message = null,
                ).withAmountNutritionPreview()
            }
        }
    }

    fun onSavedFoodQuantityChanged(value: String) {
        mutableState.update { it.copy(savedFoodQuantityGrams = value.sanitizeDecimalInput(), message = null) }
    }

    fun onQuickCaloriesChanged(value: String) {
        mutableState.update { it.copy(quickCaloriesKcal = value.sanitizeDecimalInput(), message = null) }
    }

    fun onQuickProteinChanged(value: String) {
        mutableState.update { it.copy(quickProteinGrams = value.sanitizeDecimalInput(), message = null) }
    }

    fun onQuickCarbsChanged(value: String) {
        mutableState.update { it.copy(quickCarbsGrams = value.sanitizeDecimalInput(), message = null) }
    }

    fun onQuickFatChanged(value: String) {
        mutableState.update { it.copy(quickFatGrams = value.sanitizeDecimalInput(), message = null) }
    }

    fun lookupBarcode() {
        val barcode = state.value.barcode
        if (barcode.isBlank()) {
            mutableState.update { it.copy(message = "Enter a barcode") }
            return
        }

        lookupJob?.cancel()
        lookupJob = viewModelScope.launch {
            mutableState.update {
                it.copy(
                    isLoading = true,
                    message = null,
                    lookupResult = null,
                )
            }

            when (val result = provider.lookupBarcode(barcode)) {
                is ProductLookupResult.Found ->
                    mutableState.update { currentState ->
                        if (currentState.barcode != barcode) {
                            currentState.copy(isLoading = false)
                        } else {
                            currentState.copy(
                                isLoading = false,
                                productName = result.name,
                                brand = result.brand.orEmpty(),
                                caloriesPer100g = result.nutritionPer100g.caloriesKcal.toString(),
                                proteinPer100g = result.nutritionPer100g.proteinGrams.toString(),
                                carbsPer100g = result.nutritionPer100g.carbsGrams.toString(),
                                fatPer100g = result.nutritionPer100g.fatGrams.toString(),
                                quantityGrams = (
                                    result.servingQuantityGrams
                                        ?.takeIf { it.isFinite() && it > 0.0 }
                                        ?: 100.0
                                    ).formatInputNumber(),
                                amountServingChoices = result.toAmountServingChoices(),
                                lookupResult = result,
                                message = null,
                            ).withAmountNutritionPreview()
                        }
                    }

                is ProductLookupResult.NotFound ->
                    mutableState.update { currentState ->
                        if (currentState.barcode != barcode) {
                            currentState.copy(isLoading = false)
                        } else {
                            currentState.clearedEditableFields().copy(
                                isLoading = false,
                                barcode = barcode,
                                productName = "Barcode $barcode",
                                quantityGrams = "100",
                                amountServingChoices = defaultAmountServingChoices(),
                                message = "Product not found. Add details to create it.",
                            )
                        }
                    }

                is ProductLookupResult.Failed ->
                    mutableState.update { currentState ->
                        if (currentState.barcode != barcode) {
                            currentState.copy(isLoading = false)
                        } else {
                            currentState.clearedEditableFields().copy(
                                isLoading = false,
                                message = result.message,
                            )
                        }
                    }
            }
        }
    }

    fun saveProduct() = logFood()

    fun saveScannedProductToDatabase() {
        val currentState = state.value
        val productName = currentState.productName.trim()
        if (productName.isBlank()) {
            mutableState.update { it.copy(message = "Enter a food name") }
            return
        }
        val editedNutrition = currentState.toEditedNutritionOrNull()
        if (editedNutrition == null) {
            mutableState.update { it.copy(message = "Enter valid nutrition values") }
            return
        }
        val result = currentState.lookupResult
        val customBarcode = currentState.barcode.takeIf { it.isNotBlank() }
        val customServingGrams = currentState.quantityGrams.parsePositiveNumberOrNull()
        if (result == null && customBarcode == null) {
            mutableState.update { it.copy(message = "Enter a barcode") }
            return
        }
        if (result == null && customServingGrams == null) {
            mutableState.update { it.copy(message = "Enter a valid amount") }
            return
        }
        if (!markSaving()) {
            return
        }

        viewModelScope.launch {
            try {
                if (result != null) {
                    repository.saveConfirmedProduct(
                        result = result,
                        editedName = productName,
                        editedBrand = currentState.brand.ifBlank { null },
                        editedNutrition = editedNutrition,
                    )
                } else {
                    val servingGrams = checkNotNull(customServingGrams)
                    repository.upsertSavedFood(
                        SavedFoodUpsertInput(
                            foodId = null,
                            name = productName,
                            brand = currentState.brand.ifBlank { null },
                            defaultServingGrams = servingGrams,
                            nutritionPer100g = editedNutrition,
                            nutritionDetailsPer100g = NutritionDetails(),
                            servingName = "${servingGrams.formatInputNumber()} g",
                            barcode = customBarcode,
                        ),
                    )
                }
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        message = "Saved product to database",
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        message = error.message ?: "Failed to save product",
                    )
                }
            }
        }
    }

    fun logFood() {
        val currentState = state.value
        if (currentState.isSaving) {
            return
        }
        val productName = currentState.productName.trim()
        if (productName.isBlank()) {
            mutableState.update { it.copy(message = "Enter a food name") }
            return
        }
        val editedNutrition = currentState.toEditedNutritionOrNull()
        if (editedNutrition == null) {
            mutableState.update { it.copy(message = "Enter valid nutrition values") }
            return
        }
        val quantityGrams = currentState.quantityGrams.parsePositiveNumberOrNull()
        if (quantityGrams == null) {
            mutableState.update { it.copy(message = "Enter a valid amount") }
            return
        }
        if (!markSaving()) {
            return
        }

        viewModelScope.launch {
            try {
                repository.logFood(
                    FoodLogInput(
                        lookupResult = currentState.lookupResult,
                        barcode = currentState.barcode.takeIf { it.isNotBlank() },
                        name = productName,
                        brand = currentState.brand.ifBlank { null },
                        nutritionPer100g = editedNutrition,
                        nutritionDetailsPer100g = currentState.lookupResult?.nutritionDetailsPer100g ?: NutritionDetails(),
                        servingGrams = currentState.lookupResult?.servingQuantityGrams ?: quantityGrams,
                        mealType = currentState.mealType,
                        quantityGrams = quantityGrams,
                        date = currentState.selectedDate,
                    ),
                )
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        isAddPanelVisible = currentState.keepAddingFoods,
                        message = "Logged food",
                        lookupResult = null,
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        message = error.message ?: "Failed to save food",
                    )
                }
            }
        }
    }

    fun logSavedFood(foodId: String) {
        val currentState = state.value
        if (currentState.isSaving) {
            return
        }
        val quantityGrams = currentState.savedFoodQuantityGrams.parsePositiveNumberOrNull()
        if (quantityGrams == null) {
            mutableState.update { it.copy(message = "Enter a valid amount") }
            return
        }
        if (!markSaving()) {
            return
        }

        viewModelScope.launch {
            try {
                val input = SavedFoodLogInput(
                    foodId = foodId,
                    mealType = currentState.mealType,
                    quantityGrams = quantityGrams,
                    date = currentState.selectedDate,
                )
                if (currentState.isPlanningMode) {
                    repository.planSavedFood(input)
                } else {
                    repository.logSavedFood(input)
                }
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        isAddPanelVisible = currentState.keepAddingFoods,
                        message = if (currentState.isPlanningMode) "Planned food" else "Logged food",
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        message = error.message ?: "Failed to log food",
                    )
                }
            }
        }
    }

    fun quickLog() {
        val currentState = state.value
        if (currentState.isSaving) {
            return
        }
        val caloriesKcal = currentState.quickCaloriesKcal.parsePositiveNumberOrNull()
        val proteinGrams = currentState.quickProteinGrams.parseNonNegativeNumberOrZero()
        val carbsGrams = currentState.quickCarbsGrams.parseNonNegativeNumberOrZero()
        val fatGrams = currentState.quickFatGrams.parseNonNegativeNumberOrZero()
        if (caloriesKcal == null || proteinGrams == null || carbsGrams == null || fatGrams == null) {
            mutableState.update { it.copy(message = "Enter valid quick calories") }
            return
        }
        if (!markSaving()) {
            return
        }

        viewModelScope.launch {
            try {
                repository.quickLog(
                    QuickCalorieLogInput(
                        mealType = currentState.mealType,
                        caloriesKcal = caloriesKcal,
                        proteinGrams = proteinGrams,
                        carbsGrams = carbsGrams,
                        fatGrams = fatGrams,
                        date = currentState.selectedDate,
                    ),
                )
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        isAddPanelVisible = currentState.keepAddingFoods,
                        message = "Logged quick calories",
                        quickCaloriesKcal = "",
                        quickProteinGrams = "",
                        quickCarbsGrams = "",
                        quickFatGrams = "",
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        message = error.message ?: "Failed to log calories",
                    )
                }
            }
        }
    }

    fun saveFavoriteQuickLog() {
        val currentState = state.value
        if (currentState.isSaving) {
            return
        }
        val caloriesKcal = currentState.quickCaloriesKcal.parsePositiveNumberOrNull()
        val proteinGrams = currentState.quickProteinGrams.parseNonNegativeNumberOrZero()
        val carbsGrams = currentState.quickCarbsGrams.parseNonNegativeNumberOrZero()
        val fatGrams = currentState.quickFatGrams.parseNonNegativeNumberOrZero()
        if (caloriesKcal == null || proteinGrams == null || carbsGrams == null || fatGrams == null) {
            mutableState.update { it.copy(message = "Enter valid quick calories") }
            return
        }
        if (!markSaving()) {
            return
        }

        viewModelScope.launch {
            try {
                repository.saveFavoriteQuickLog(
                    QuickCaloriePresetInput(
                        name = "${caloriesKcal.formatInputNumber()} kcal quick log",
                        caloriesKcal = caloriesKcal,
                        proteinGrams = proteinGrams,
                        carbsGrams = carbsGrams,
                        fatGrams = fatGrams,
                        isFavorite = true,
                    ),
                )
                mutableState.update { it.copy(isSaving = false, message = "Saved quick log favorite") }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(isSaving = false, message = error.message ?: "Failed to save quick log") }
            }
        }
    }

    fun logFavoriteQuickLog(presetId: String) {
        val currentState = state.value
        if (!markSaving()) {
            return
        }
        viewModelScope.launch {
            try {
                repository.logFavoriteQuickLog(
                    presetId = presetId,
                    mealType = currentState.mealType,
                    date = currentState.selectedDate,
                )
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        isAddPanelVisible = currentState.keepAddingFoods,
                        message = "Logged favorite quick log",
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(isSaving = false, message = error.message ?: "Failed to log quick favorite") }
            }
        }
    }

    fun toggleFavoriteQuickLog(presetId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            try {
                repository.toggleFavoriteQuickLog(presetId, isFavorite)
                mutableState.update {
                    it.copy(
                        message = if (isFavorite) {
                            "Quick log added to favorites"
                        } else {
                            "Quick log removed from favorites"
                        },
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(message = error.message ?: "Failed to update quick favorite") }
            }
        }
    }

    fun copySelectedMealFromYesterday() {
        val currentState = state.value
        val mealType = currentState.selectedMealDetailId ?: currentState.mealType
        if (!markSaving()) {
            return
        }
        viewModelScope.launch {
            try {
                repository.copyMeal(
                    fromDate = currentState.selectedDate.minusDays(1),
                    toDate = currentState.selectedDate,
                    mealType = mealType,
                )
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        message = "Copied meal from yesterday",
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        message = error.message ?: "Failed to copy meal",
                    )
                }
            }
        }
    }

    fun saveSelectedMealAsTemplate(name: String) {
        val currentState = state.value
        val mealType = currentState.selectedMealDetailId ?: currentState.mealType
        if (name.isBlank()) {
            mutableState.update { it.copy(message = "Enter a template name") }
            return
        }
        if (!markSaving()) {
            return
        }
        viewModelScope.launch {
            try {
                repository.saveMealAsTemplate(
                    date = currentState.selectedDate,
                    mealType = mealType,
                    name = name,
                )
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        message = "Saved meal template",
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        message = error.message ?: "Failed to save template",
                    )
                }
            }
        }
    }

    fun logMealTemplate(templateId: String) {
        val currentState = state.value
        if (!markSaving()) {
            return
        }
        viewModelScope.launch {
            try {
                repository.logMealTemplate(
                    templateId = templateId,
                    mealType = currentState.mealType,
                    date = currentState.selectedDate,
                )
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        isAddPanelVisible = currentState.keepAddingFoods,
                        message = "Logged meal template",
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        message = error.message ?: "Failed to log template",
                    )
                }
            }
        }
    }

    fun onGoalCaloriesChanged(value: String) {
        mutableState.update { it.copy(goalEditor = it.goalEditor.copy(caloriesKcalInput = value.sanitizeDecimalInput(), modeInput = FoodGoalMode.Custom), message = null) }
    }

    fun onGoalProteinChanged(value: String) {
        mutableState.update { it.copy(goalEditor = it.goalEditor.copy(proteinGramsInput = value.sanitizeDecimalInput(), modeInput = FoodGoalMode.Custom), message = null) }
    }

    fun onGoalCarbsChanged(value: String) {
        mutableState.update { it.copy(goalEditor = it.goalEditor.copy(carbsGramsInput = value.sanitizeDecimalInput(), modeInput = FoodGoalMode.Custom), message = null) }
    }

    fun onGoalFatChanged(value: String) {
        mutableState.update { it.copy(goalEditor = it.goalEditor.copy(fatGramsInput = value.sanitizeDecimalInput(), modeInput = FoodGoalMode.Custom), message = null) }
    }

    fun onGoalFiberChanged(value: String) {
        mutableState.update { it.copy(goalEditor = it.goalEditor.copy(fiberGramsInput = value.sanitizeDecimalInput(), modeInput = FoodGoalMode.Custom), message = null) }
    }

    fun onGoalSugarChanged(value: String) {
        mutableState.update { it.copy(goalEditor = it.goalEditor.copy(sugarGramsInput = value.sanitizeDecimalInput(), modeInput = FoodGoalMode.Custom), message = null) }
    }

    fun onGoalSaturatedFatChanged(value: String) {
        mutableState.update { it.copy(goalEditor = it.goalEditor.copy(saturatedFatGramsInput = value.sanitizeDecimalInput(), modeInput = FoodGoalMode.Custom), message = null) }
    }

    fun onGoalSodiumChanged(value: String) {
        mutableState.update { it.copy(goalEditor = it.goalEditor.copy(sodiumMgInput = value.sanitizeDecimalInput(), modeInput = FoodGoalMode.Custom), message = null) }
    }

    fun onGoalModeChanged(value: FoodGoalMode) {
        mutableState.update { currentState ->
            val preset = value.goalPreset()
            if (preset == null) {
                currentState.copy(goalEditor = currentState.goalEditor.copy(modeInput = value), message = null)
            } else {
                currentState.copy(
                    goalEditor = currentState.goalEditor.copy(
                        modeInput = value,
                        caloriesKcalInput = preset.dailyCaloriesKcal.formatInputNumber(),
                        proteinGramsInput = preset.proteinGrams.formatInputNumber(),
                        carbsGramsInput = preset.carbsGrams.formatInputNumber(),
                        fatGramsInput = preset.fatGrams.formatInputNumber(),
                        fiberGramsInput = preset.fiberGrams.formatInputNumber(),
                        sugarGramsInput = preset.sugarGrams.formatInputNumber(),
                        saturatedFatGramsInput = preset.saturatedFatGrams.formatInputNumber(),
                        sodiumMgInput = preset.sodiumMilligrams.formatInputNumber(),
                    ),
                    message = null,
                )
            }
        }
    }

    fun onGoalIncludeTrainingChanged(value: Boolean) {
        mutableState.update { it.copy(goalEditor = it.goalEditor.copy(includeTrainingInput = value), message = null) }
    }

    fun onGoalUseNetCarbsChanged(value: Boolean) {
        mutableState.update { it.copy(goalEditor = it.goalEditor.copy(useNetCarbsInput = value), message = null) }
    }

    fun saveFoodGoal() {
        val currentState = state.value
        val editor = currentState.goalEditor
        val goal =
            FoodGoal(
                dailyCaloriesKcal = editor.caloriesKcalInput.parsePositiveNumberOrNull() ?: run {
                    mutableState.update { it.copy(message = "Enter a valid calorie goal") }
                    return
                },
                proteinGrams = editor.proteinGramsInput.parseNonNegativeNumberOrZero() ?: return invalidGoal(),
                carbsGrams = editor.carbsGramsInput.parseNonNegativeNumberOrZero() ?: return invalidGoal(),
                fatGrams = editor.fatGramsInput.parseNonNegativeNumberOrZero() ?: return invalidGoal(),
                fiberGrams = editor.fiberGramsInput.parseNonNegativeNumberOrZero() ?: return invalidGoal(),
                sugarGrams = editor.sugarGramsInput.parseNonNegativeNumberOrZero() ?: return invalidGoal(),
                saturatedFatGrams = editor.saturatedFatGramsInput.parseNonNegativeNumberOrZero() ?: return invalidGoal(),
                sodiumMilligrams = editor.sodiumMgInput.parseNonNegativeNumberOrZero() ?: return invalidGoal(),
                mode = editor.modeInput,
                includeTrainingCalories = editor.includeTrainingInput,
                useNetCarbs = editor.useNetCarbsInput,
                waterGoalMilliliters = currentState.waterGoalMilliliters,
            )
        if (!markSaving()) {
            return
        }
        viewModelScope.launch {
            try {
                repository.updateFoodGoal(goal)
                mutableState.update {
                    it.withFoodGoal(goal)
                        .withDiary(currentDiary)
                        .copy(
                            isSaving = false,
                            isAddPanelVisible = false,
                            sheetMode = null,
                            message = "Updated nutrition goals",
                        )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        message = error.message ?: "Failed to save goals",
                    )
                }
            }
        }
    }

    private fun invalidGoal() {
        mutableState.update { it.copy(message = "Enter valid goal values") }
    }

    fun onRecipeNameChanged(value: String) {
        mutableState.update { it.copy(recipeName = value, message = null) }
    }

    fun onRecipeCategoryChanged(value: String) {
        mutableState.update { it.copy(recipeCategory = value, message = null) }
    }

    fun onRecipeServingNameChanged(value: String) {
        mutableState.update { it.copy(recipeServingName = value, message = null) }
    }

    fun onRecipeServingGramsChanged(value: String) {
        val sanitized = value.sanitizeDecimalInput()
        mutableState.update {
            it.copy(
                recipeServingGrams = sanitized,
                recipeCookedYieldGrams = if (it.recipeServingsCount.parsePositiveNumberOrNull() == 1.0) {
                    sanitized
                } else {
                    it.recipeCookedYieldGrams
                },
                message = null,
            )
        }
    }

    fun onRecipeServingsCountChanged(value: String) {
        val sanitized = value.sanitizeDecimalInput()
        mutableState.update {
            val cookedYield = it.recipeCookedYieldGrams.parsePositiveNumberOrNull()
            val servings = sanitized.parsePositiveNumberOrNull()
            it.copy(
                recipeServingsCount = sanitized,
                recipeServingGrams = if (cookedYield != null && servings != null) {
                    (cookedYield / servings).formatInputNumber()
                } else {
                    it.recipeServingGrams
                },
                message = null,
            )
        }
    }

    fun onRecipeCookedYieldGramsChanged(value: String) {
        val sanitized = value.sanitizeDecimalInput()
        mutableState.update {
            val cookedYield = sanitized.parsePositiveNumberOrNull()
            val servings = it.recipeServingsCount.parsePositiveNumberOrNull()
            it.copy(
                recipeCookedYieldGrams = sanitized,
                recipeServingGrams = if (cookedYield != null && servings != null) {
                    (cookedYield / servings).formatInputNumber()
                } else {
                    it.recipeServingGrams
                },
                message = null,
            )
        }
    }

    fun onRecipeIngredientFoodChanged(value: String) {
        val food = state.value.savedFoods.firstOrNull { it.id == value }
        val choices = food?.toRecipeIngredientServingChoices().orEmpty()
        mutableState.update {
            it.copy(
                recipeIngredientFoodId = value,
                recipeIngredientServingChoiceId = choices.firstOrNull()?.id ?: "g",
                recipeIngredientServingChoices = choices,
                message = null,
            )
        }
    }

    fun onRecipeIngredientServingChoiceSelected(choiceId: String) {
        val choice = state.value.recipeIngredientServingChoices.firstOrNull { it.id == choiceId } ?: return
        mutableState.update {
            it.copy(
                recipeIngredientServingChoiceId = choice.id,
                recipeIngredientQuantityGrams = if (choice.id == "g") it.recipeIngredientQuantityGrams else "1",
                message = null,
            )
        }
    }

    fun onRecipeIngredientQuantityChanged(value: String) {
        mutableState.update { it.copy(recipeIngredientQuantityGrams = value.sanitizeDecimalInput(), message = null) }
    }

    fun onRecipeServingsToLogChanged(value: String) {
        mutableState.update { it.copy(recipeServingsToLog = value.sanitizeDecimalInput(), message = null) }
    }

    fun addRecipeIngredient() {
        val currentState = state.value
        val food = currentState.savedFoods.firstOrNull { it.id == currentState.recipeIngredientFoodId }
        if (food == null) {
            mutableState.update { it.copy(message = "Choose an ingredient") }
            return
        }
        val quantity = currentState.recipeIngredientQuantityGrams.parsePositiveNumberOrNull()
        if (quantity == null) {
            mutableState.update { it.copy(message = "Enter ingredient amount") }
            return
        }
        val servingChoice =
            currentState.recipeIngredientServingChoices.firstOrNull { it.id == currentState.recipeIngredientServingChoiceId }
                ?: RecipeIngredientServingChoiceUiState(id = "g", label = "g", grams = 1.0)
        val quantityGrams = quantity * servingChoice.grams
        mutableState.update {
            it.copy(
                recipeIngredients = it.recipeIngredients + RecipeIngredientDraftUiState(
                    foodId = food.id,
                    foodName = food.name,
                    quantityGrams = quantityGrams,
                    unitLabel = servingChoice.label,
                    unitGrams = servingChoice.grams,
                    unitQuantity = quantity,
                ),
                recipeIngredientFoodId = "",
                recipeIngredientServingChoiceId = "g",
                recipeIngredientServingChoices = emptyList(),
                recipeIngredientQuantityGrams = "100",
                message = null,
            )
        }
    }

    fun saveRecipe() {
        val currentState = state.value
        val servings = currentState.recipeServingsCount.parsePositiveNumberOrNull()
        val cookedYieldGrams = currentState.recipeCookedYieldGrams.parsePositiveNumberOrNull()
        val servingGrams = if (servings != null && cookedYieldGrams != null) {
            cookedYieldGrams / servings
        } else {
            currentState.recipeServingGrams.parsePositiveNumberOrNull()
        }
        if (
            currentState.recipeName.isBlank() ||
            servingGrams == null ||
            servings == null ||
            cookedYieldGrams == null ||
            currentState.recipeIngredients.isEmpty()
        ) {
            mutableState.update { it.copy(message = "Complete the recipe") }
            return
        }
        if (!markSaving()) {
            return
        }
        viewModelScope.launch {
            try {
                repository.upsertRecipe(
                    RecipeUpsertInput(
                        recipeId = currentState.editingRecipeId,
                        name = currentState.recipeName,
                        category = currentState.recipeCategory.ifBlank { null },
                        servingName = currentState.recipeServingName.ifBlank { "Serving" },
                        servingGrams = servingGrams,
                        servings = servings,
                        cookedYieldGrams = cookedYieldGrams,
                        ingredients = currentState.recipeIngredients.map { ingredient ->
                            RecipeIngredientInput(
                                foodId = ingredient.foodId,
                                quantityGrams = ingredient.quantityGrams,
                                unitLabel = ingredient.unitLabel,
                                unitGrams = ingredient.unitGrams,
                                unitQuantity = ingredient.unitQuantity,
                            )
                        },
                    ),
                )
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        isAddPanelVisible = false,
                        sheetMode = null,
                        editingRecipeId = null,
                        recipeName = "",
                        recipeCategory = "",
                        recipeServingName = "Serving",
                        recipeServingGrams = "100",
                        recipeServingsCount = "1",
                        recipeCookedYieldGrams = "100",
                        recipeIngredientFoodId = "",
                        recipeIngredientServingChoiceId = "g",
                        recipeIngredientServingChoices = emptyList(),
                        recipeIngredientQuantityGrams = "100",
                        recipeIngredients = emptyList(),
                        message = "Saved recipe",
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        message = error.message ?: "Failed to save recipe",
                    )
                }
            }
        }
    }

    fun deleteRecipe(recipeId: String) {
        if (!markSaving()) {
            return
        }
        viewModelScope.launch {
            try {
                repository.deleteRecipe(recipeId)
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        isAddPanelVisible = false,
                        sheetMode = null,
                        editingRecipeId = null,
                        message = "Deleted recipe",
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(isSaving = false, message = error.message ?: "Failed to delete recipe") }
            }
        }
    }

    fun toggleFavoriteRecipe(recipeId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            try {
                repository.toggleFavoriteRecipe(recipeId, isFavorite)
                mutableState.update {
                    it.copy(
                        message = if (isFavorite) {
                            "Recipe added to favorites"
                        } else {
                            "Recipe removed from favorites"
                        },
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(message = error.message ?: "Failed to update recipe favorite") }
            }
        }
    }

    fun duplicateRecipe(recipeId: String) {
        val recipe = state.value.recipes.firstOrNull { it.id == recipeId }
        val copyName = "${recipe?.name ?: "Recipe"} copy"
        if (!markSaving()) {
            return
        }
        viewModelScope.launch {
            try {
                repository.duplicateRecipe(recipeId, copyName)
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        message = "Duplicated recipe",
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(isSaving = false, message = error.message ?: "Failed to duplicate recipe") }
            }
        }
    }

    fun logRecipe(recipeId: String) {
        val currentState = state.value
        val servings = currentState.recipeServingsToLog.parsePositiveNumberOrNull()
        if (servings == null) {
            mutableState.update { it.copy(message = "Enter recipe servings") }
            return
        }
        if (!markSaving()) {
            return
        }
        viewModelScope.launch {
            try {
                repository.logRecipe(
                    recipeId = recipeId,
                    mealType = currentState.mealType,
                    servings = servings,
                    date = currentState.selectedDate,
                )
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        isAddPanelVisible = currentState.keepAddingFoods,
                        message = "Logged recipe",
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        message = error.message ?: "Failed to log recipe",
                    )
                }
            }
        }
    }

    private fun markSaving(): Boolean {
        var saveStarted = false
        mutableState.update {
            if (it.isSaving) {
                it
            } else {
                saveStarted = true
                it.copy(isSaving = true, message = null)
            }
        }
        return saveStarted
    }

    private fun FoodUiState.toEditedNutritionOrNull(): FoodNutrition? {
        val calories = caloriesPer100g.parseNutritionValue() ?: return null
        val protein = proteinPer100g.parseNutritionValue() ?: return null
        val carbs = carbsPer100g.parseNutritionValue() ?: return null
        val fat = fatPer100g.parseNutritionValue() ?: return null

        return FoodNutrition(
            caloriesKcal = calories,
            proteinGrams = protein,
            carbsGrams = carbs,
            fatGrams = fat,
        )
    }

    private fun FoodUiState.toSavedFoodNutritionOrNull(): FoodNutrition? {
        val calories = savedFoodCaloriesPer100g.parseNutritionValue() ?: return null
        val protein = savedFoodProteinPer100g.parseNutritionValue() ?: return null
        val carbs = savedFoodCarbsPer100g.parseNutritionValue() ?: return null
        val fat = savedFoodFatPer100g.parseNutritionValue() ?: return null

        return FoodNutrition(
            caloriesKcal = calories,
            proteinGrams = protein,
            carbsGrams = carbs,
            fatGrams = fat,
        )
    }

    private fun FoodUiState.toSavedFoodNutritionDetailsOrNull(): NutritionDetails? {
        val fiber = savedFoodFiberPer100g.parseNonNegativeNumberOrZero() ?: return null
        val sugar = savedFoodSugarPer100g.parseNonNegativeNumberOrZero() ?: return null
        val saturatedFat = savedFoodSaturatedFatPer100g.parseNonNegativeNumberOrZero() ?: return null
        val sodium = savedFoodSodiumMgPer100g.parseNonNegativeNumberOrZero() ?: return null
        val potassium = savedFoodPotassiumMgPer100g.parseNonNegativeNumberOrZero() ?: return null
        val calcium = savedFoodCalciumMgPer100g.parseNonNegativeNumberOrZero() ?: return null
        val iron = savedFoodIronMgPer100g.parseNonNegativeNumberOrZero() ?: return null
        val vitaminD = savedFoodVitaminDMcgPer100g.parseNonNegativeNumberOrZero() ?: return null
        val vitaminC = savedFoodVitaminCMgPer100g.parseNonNegativeNumberOrZero() ?: return null
        val magnesium = savedFoodMagnesiumMgPer100g.parseNonNegativeNumberOrZero() ?: return null

        return NutritionDetails(
            fiberGrams = fiber,
            sugarGrams = sugar,
            saturatedFatGrams = saturatedFat,
            sodiumMilligrams = sodium,
            potassiumMilligrams = potassium,
            calciumMilligrams = calcium,
            ironMilligrams = iron,
            vitaminDMicrograms = vitaminD,
            vitaminCMilligrams = vitaminC,
            magnesiumMilligrams = magnesium,
        )
    }

    private fun FoodUiState.savedFoodServingInputsForUpsert(servingGrams: Double): List<FoodServingInput> {
        val existingFood =
            editingSavedFoodId?.let { foodId -> savedFoods.firstOrNull { savedFood -> savedFood.id == foodId } }
                ?: return emptyList()
        if (existingFood.servings.isEmpty()) {
            return emptyList()
        }

        val currentServingLabel =
            savedFoodServingName.ifBlank {
                existingFood.servingName ?: "${servingGrams.formatInputNumber()} g"
            }
        var updatedDefaultServing = false
        val servingInputs = existingFood.servings.map { serving ->
            val isExistingDefaultServing =
                serving.grams == existingFood.defaultServingGrams ||
                    existingFood.servingName?.let { serving.label == it } == true

            if (isExistingDefaultServing && !updatedDefaultServing) {
                updatedDefaultServing = true
                FoodServingInput(currentServingLabel, servingGrams)
            } else {
                FoodServingInput(serving.label, serving.grams)
            }
        }

        return if (updatedDefaultServing) {
            servingInputs
        } else {
            listOf(FoodServingInput(currentServingLabel, servingGrams)) + servingInputs
        }
    }

    private fun FoodUiState.withAmountNutritionPreview(): FoodUiState {
        val quantity = quantityGrams.parsePositiveNumberOrNull()
        val nutrition = toEditedNutritionOrNull()
        val preview = if (quantity != null && nutrition != null) {
            val scaled = NutritionCalculator.nutritionForAmount(nutrition, quantity)
            FoodAmountNutritionPreviewUiState(
                quantityGrams = quantity,
                caloriesKcal = scaled.caloriesKcal,
                proteinGrams = scaled.proteinGrams,
                carbsGrams = scaled.carbsGrams,
                fatGrams = scaled.fatGrams,
            )
        } else {
            null
        }

        return copy(amountNutritionPreview = preview)
    }

    private fun FoodUiState.withDiaryEntryPreview(): FoodUiState {
        val editor = diaryEntryEditor ?: return this
        val quantity = editor.quantityGrams.parsePositiveNumberOrNull()
        val originalQuantity = editor.originalQuantityGrams
        if (quantity == null || originalQuantity <= 0.0) {
            return copy(
                diaryEntryEditor = editor.copy(
                    previewCaloriesKcal = 0.0,
                    previewProteinGrams = 0.0,
                    previewCarbsGrams = 0.0,
                    previewFatGrams = 0.0,
                ),
            )
        }

        val scale = quantity / originalQuantity
        return copy(
            diaryEntryEditor = editor.copy(
                previewCaloriesKcal = editor.caloriesKcal * scale,
                previewProteinGrams = editor.originalProteinGrams * scale,
                previewCarbsGrams = editor.originalCarbsGrams * scale,
                previewFatGrams = editor.originalFatGrams * scale,
            ),
        )
    }

    private fun FoodUiState.withAiLoggingDraft(
        name: String,
        sourceLabel: String,
        message: String,
    ): FoodUiState =
        copy(
            isAddPanelVisible = true,
            sheetMode = FoodSheetMode.AddFood,
            addMode = FoodAddMode.Ai,
            productName = name,
            brand = "",
            quantityGrams = "100",
            caloriesPer100g = "250",
            proteinPer100g = "12",
            carbsPer100g = "30",
            fatPer100g = "8",
            amountServingChoices = defaultAmountServingChoices(),
            lookupResult = null,
            aiLoggingHasDraft = true,
            aiLoggingDraftSourceLabel = sourceLabel,
            message = message,
        ).withAmountNutritionPreview()

    private fun FoodUiState.clearedEditableFields(): FoodUiState =
        copy(
            productName = "",
            brand = "",
            caloriesPer100g = "",
            proteinPer100g = "",
            carbsPer100g = "",
            fatPer100g = "",
            amountNutritionPreview = null,
            amountServingChoices = emptyList(),
            lookupResult = null,
            aiLoggingHasDraft = false,
            aiLoggingDraftSourceLabel = null,
        )
}

private data class MealDefinition(
    val id: String,
    val title: String,
    val recommendation: String,
    val calorieTargetKcal: Double,
    val sortOrder: Int,
    val timeMinutes: Int? = null,
)

private data class GoalPreset(
    val dailyCaloriesKcal: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val fiberGrams: Double = FIBER_GOAL_GRAMS,
    val sugarGrams: Double = SUGAR_GOAL_GRAMS,
    val saturatedFatGrams: Double = SATURATED_FAT_GOAL_GRAMS,
    val sodiumMilligrams: Double = SODIUM_GOAL_MILLIGRAMS,
)

private val mealDefinitions =
    listOf(
        MealDefinition("breakfast", "Breakfast", "Recommended 417 - 625 kcal", 625.0, sortOrder = 0),
        MealDefinition("lunch", "Lunch", "Recommended 625 - 833 kcal", 833.0, sortOrder = 10),
        MealDefinition("dinner", "Dinner", "Recommended 625 - 833 kcal", 833.0, sortOrder = 20),
        MealDefinition("snacks", "Snacks", "Recommended 104 - 208 kcal", 208.0, sortOrder = 30),
    )

private val defaultMealDefinitionIds = mealDefinitions.map { it.id }.toSet()

private const val CALORIE_GOAL_KCAL = 2083.0
private const val CARBS_GOAL_GRAMS = 260.0
private const val PROTEIN_GOAL_GRAMS = 104.0
private const val FAT_GOAL_GRAMS = 69.0
private const val FIBER_GOAL_GRAMS = 30.0
private const val SUGAR_GOAL_GRAMS = 50.0
private const val SATURATED_FAT_GOAL_GRAMS = 20.0
private const val SODIUM_GOAL_MILLIGRAMS = 2300.0
private const val WATER_GOAL_MILLILITERS = 2000.0

private fun FoodGoalMode.goalPreset(): GoalPreset? =
    when (this) {
        FoodGoalMode.Balanced ->
            GoalPreset(
                dailyCaloriesKcal = CALORIE_GOAL_KCAL,
                proteinGrams = PROTEIN_GOAL_GRAMS,
                carbsGrams = CARBS_GOAL_GRAMS,
                fatGrams = FAT_GOAL_GRAMS,
            )

        FoodGoalMode.HighProtein ->
            GoalPreset(
                dailyCaloriesKcal = CALORIE_GOAL_KCAL,
                proteinGrams = 156.0,
                carbsGrams = 208.0,
                fatGrams = 69.0,
            )

        FoodGoalMode.KetoLowCarb ->
            GoalPreset(
                dailyCaloriesKcal = CALORIE_GOAL_KCAL,
                proteinGrams = 130.0,
                carbsGrams = 52.0,
                fatGrams = 150.0,
                fiberGrams = 25.0,
                sugarGrams = 25.0,
            )

        FoodGoalMode.MuscleGain ->
            GoalPreset(
                dailyCaloriesKcal = 2400.0,
                proteinGrams = 150.0,
                carbsGrams = 300.0,
                fatGrams = 67.0,
                fiberGrams = 35.0,
                sugarGrams = 60.0,
            )

        FoodGoalMode.WeightLoss ->
            GoalPreset(
                dailyCaloriesKcal = 1800.0,
                proteinGrams = 135.0,
                carbsGrams = 180.0,
                fatGrams = 60.0,
                fiberGrams = 35.0,
                sugarGrams = 40.0,
            )

        FoodGoalMode.Custom -> null
    }

private fun FoodUiState.withDiary(diary: FoodDiary): FoodUiState =
    copy(
        eatenCaloriesKcal = diary.totals.caloriesKcal,
        remainingCaloriesKcal = calorieGoalKcal - diary.totals.caloriesKcal,
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
        isFoodDiaryEmpty = diary.isEmptyLoggedDiary(),
        emptyDiaryActions = if (diary.isEmptyLoggedDiary()) defaultEmptyDiaryActions() else emptyList(),
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

private fun FoodUiState.withFoodGoal(goal: FoodGoal): FoodUiState =
    copy(
        calorieGoalKcal = goal.dailyCaloriesKcal,
        proteinGoalGrams = goal.proteinGrams,
        carbsGoalGrams = goal.carbsGrams,
        fatGoalGrams = goal.fatGrams,
        fiberGoalGrams = goal.fiberGrams,
        sugarGoalGrams = goal.sugarGrams,
        saturatedFatGoalGrams = goal.saturatedFatGrams,
        sodiumGoalMilligrams = goal.sodiumMilligrams,
        goalMode = goal.mode,
        includeTrainingCalories = goal.includeTrainingCalories,
        useNetCarbs = goal.useNetCarbs,
        remainingCaloriesKcal = goal.dailyCaloriesKcal - eatenCaloriesKcal,
        goalEditor = GoalEditorState(
            caloriesKcalInput = goal.dailyCaloriesKcal.formatInputNumber(),
            proteinGramsInput = goal.proteinGrams.formatInputNumber(),
            carbsGramsInput = goal.carbsGrams.formatInputNumber(),
            fatGramsInput = goal.fatGrams.formatInputNumber(),
            fiberGramsInput = goal.fiberGrams.formatInputNumber(),
            sugarGramsInput = goal.sugarGrams.formatInputNumber(),
            saturatedFatGramsInput = goal.saturatedFatGrams.formatInputNumber(),
            sodiumMgInput = goal.sodiumMilligrams.formatInputNumber(),
            modeInput = goal.mode,
            includeTrainingInput = goal.includeTrainingCalories,
            useNetCarbsInput = goal.useNetCarbs,
        ),
        waterGoalMilliliters = goal.waterGoalMilliliters,
        waterProgress = waterConsumedMilliliters.fractionOf(goal.waterGoalMilliliters),
        waterGoalInput = goal.waterGoalMilliliters.formatInputNumber(),
    )

private fun FoodUiState.withWaterSummary(summary: FoodWaterSummary): FoodUiState =
    copy(
        waterConsumedMilliliters = summary.consumedMilliliters,
        waterGoalMilliliters = summary.goalMilliliters,
        waterProgress = summary.consumedMilliliters.fractionOf(summary.goalMilliliters),
        waterGoalInput = summary.goalMilliliters.formatInputNumber(),
    )

private fun FoodUiState.withFoodHealthConnectSyncState(syncState: FoodHealthConnectSyncState): FoodUiState =
    copy(
        foodHealthConnectSyncEnabled = syncState.isEnabled,
        foodHealthConnectCanRequestPermissions = syncState.canRequestPermissions,
        foodHealthConnectCanSync = syncState.canSync,
        foodHealthConnectRequestablePermissions = syncState.requestablePermissions,
        foodHealthConnectPermissionSummary = syncState.permissionSummary(),
        foodHealthConnectLastSyncAtEpochMillis = syncState.lastSyncAtEpochMillis,
        foodHealthConnectLastFailureMessage = syncState.lastFailureMessage,
    )

private fun FoodHealthConnectSyncState.permissionSummary(): String =
    when (availability) {
        HealthConnectAvailability.Available ->
            if (requestablePermissionCount == 0) {
                "No Food permissions requested"
            } else {
                "$grantedPermissionCount / $requestablePermissionCount Food permissions granted"
            }

        HealthConnectAvailability.NotInstalled -> "Health Connect needs install or update"
        HealthConnectAvailability.NotSupported -> "Health Connect unavailable"
    }

private fun FoodHealthConnectSyncResult.toFoodHealthConnectMessage(): String {
    val mealText = when (nutritionRecordCount) {
        0 -> "no meals"
        1 -> "1 meal"
        else -> "$nutritionRecordCount meals"
    }
    val waterText = if (hydrationRecordCount > 0) " and water" else ""
    return "Synced $mealText$waterText to Health Connect"
}

private fun FoodUiState.buildDailyInsights(diary: FoodDiary): List<FoodInsightUiState> {
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
        proteinRemaining >= 25.0 ->
            insights += FoodInsightUiState(
                title = "Add protein next",
                body = "A lean protein serving would close the biggest gap.",
                tone = FoodInsightTone.Neutral,
            )

        fiberRemaining >= 8.0 ->
            insights += FoodInsightUiState(
                title = "Add fiber next",
                body = "A high-fiber side would improve today quickly.",
                tone = FoodInsightTone.Neutral,
            )

        caloriesRemaining >= 300.0 ->
            insights += FoodInsightUiState(
                title = "Add a balanced meal",
                body = "Use protein plus carbs or veg to finish the day cleanly.",
                tone = FoodInsightTone.Neutral,
            )
    }

    return insights
        .distinctBy { insight -> insight.title }
        .ifEmpty {
            listOf(
                FoodInsightUiState(
                    title = "Food is on track",
                    body = "Today is balanced against your current goals.",
                    tone = FoodInsightTone.Positive,
                ),
            )
        }
        .take(3)
}

private fun FoodDiaryMeal.isBalancedLoggedMeal(): Boolean =
    entries.any { entry -> entry.status == FoodDiaryEntryStatus.Logged } &&
        totals.caloriesKcal in 250.0..800.0 &&
        totals.proteinGrams >= 20.0 &&
        detailTotals.fiberGrams >= 5.0

private fun FoodDiary.isBalancedDay(state: FoodUiState): Boolean =
    totals.caloriesKcal >= state.calorieGoalKcal * 0.85 &&
        totals.caloriesKcal <= state.calorieGoalKcal * 1.05 &&
        totals.proteinGrams >= state.proteinGoalGrams * 0.9 &&
        detailTotals.fiberGrams >= state.fiberGoalGrams * 0.8 &&
        detailTotals.sodiumMilligrams <= state.sodiumGoalMilligrams

private fun FoodDiary.isEmptyLoggedDiary(): Boolean =
    meals
        .flatMap { meal -> meal.entries }
        .none { entry -> entry.status == FoodDiaryEntryStatus.Logged }

private fun FoodUiState.buildDayRating(diary: FoodDiary): FoodRatingUiState {
    if (diary.totals.caloriesKcal <= 0.0) {
        return emptyFoodRating()
    }

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
    )
}

private fun FoodDiaryMeal?.toFoodMealRating(calorieTargetKcal: Double): FoodRatingUiState? {
    val meal = this ?: return null
    if (meal.entries.none { entry -> entry.status == FoodDiaryEntryStatus.Logged }) {
        return null
    }

    var score = 100
    val reasons = mutableListOf<String>()
    val suggestions = mutableListOf<String>()
    if (meal.totals.proteinGrams < 20.0) {
        score -= 35
        reasons += "Protein is low for this meal."
        suggestions += "Add eggs, yogurt, fish, meat, tofu, or legumes."
    }
    if (meal.detailTotals.fiberGrams < 5.0) {
        score -= 15
        reasons += "Fiber is light."
        suggestions += "Add fruit, whole grains, or vegetables."
    }
    if (meal.detailTotals.sodiumMilligrams > 900.0) {
        score -= 25
        reasons += "Sodium is high for one meal."
        suggestions += "Balance it with lower-sodium choices later."
    }
    if (meal.totals.caloriesKcal > calorieTargetKcal * 1.15) {
        score -= 15
        reasons += "Calories are above the meal target."
        suggestions += "Keep the next meal lighter."
    } else if (meal.totals.caloriesKcal < calorieTargetKcal * 0.35) {
        score -= 10
        reasons += "This meal is very light."
        suggestions += "Add protein or fiber if you are still hungry."
    }

    return FoodRatingUiState(
        label = score.toFoodRatingLabel(),
        reason = reasons.firstOrNull() ?: "Protein, fiber, sodium, and calories look balanced.",
        suggestion = suggestions.firstOrNull() ?: "Repeat this structure when it fits your day.",
        tone = score.toFoodRatingTone(),
    )
}

private fun Int.toFoodRatingLabel(): String =
    when {
        this >= 85 -> "Great"
        this >= 70 -> "Good"
        this >= 50 -> "Watch"
        else -> "Needs work"
    }

private fun Int.toFoodRatingTone(): FoodInsightTone =
    when {
        this >= 85 -> FoodInsightTone.Positive
        this >= 70 -> FoodInsightTone.Positive
        this >= 50 -> FoodInsightTone.Warning
        else -> FoodInsightTone.Warning
    }

fun FoodUiState.selectedMealDetailForDisplay(): FoodMealSectionUiState? =
    selectedMealDetailId
        ?.let { selectedId -> mealSections.firstOrNull { meal -> meal.id == selectedId } }
        ?.sortedForDetail(mealDetailSortMode)

private fun FoodMealSectionUiState.sortedForDetail(sortMode: MealDetailSortMode): FoodMealSectionUiState {
    val sortedEntries =
        when (sortMode) {
            MealDetailSortMode.Logged -> entries
            MealDetailSortMode.Calories -> entries.sortedByDescending { entry -> entry.caloriesKcal }
            MealDetailSortMode.Protein -> entries.sortedByDescending { entry -> entry.proteinGrams }
            MealDetailSortMode.Name -> entries.sortedBy { entry -> entry.name.lowercase() }
        }
    return copy(entries = sortedEntries)
}

private fun FoodDiary.toMealSections(
    mealDefinitions: List<FoodMealDefinitionUiState>,
    useNetCarbs: Boolean,
    carbsGoalGrams: Double,
    proteinGoalGrams: Double,
    fatGoalGrams: Double,
    fiberGoalGrams: Double,
    sugarGoalGrams: Double,
    saturatedFatGoalGrams: Double,
    sodiumGoalMilligrams: Double,
): List<FoodMealSectionUiState> {
    val mealsByType = meals.associateBy { it.type.normalizedMealType() }
    val unknownMealDefinitions =
        mealsByType.keys
            .filterNot { mealType -> mealDefinitions.any { it.id == mealType } }
            .mapIndexed { index, mealType ->
                FoodMealDefinitionUiState(
                    id = mealType,
                    title = mealType.mealTitle(),
                    timeMinutes = null,
                    timeLabel = "No time",
                    sortOrder = 100 + index,
                    isDefault = false,
                )
            }

    return (mealDefinitions + unknownMealDefinitions).map { definition ->
        val meal = mealsByType[definition.id]
        val totals = meal?.totals
        val detailTotals = meal?.detailTotals ?: NutritionDetails()
        val calorieTargetKcal = definition.calorieTargetKcal()
        val effectiveCarbsGrams =
            if (useNetCarbs) {
                ((totals?.carbsGrams ?: 0.0) - detailTotals.fiberGrams).coerceAtLeast(0.0)
            } else {
                totals?.carbsGrams ?: 0.0
            }
        val defaultDefinition = mealDefinitions.firstOrNull { it.id == definition.id }
        FoodMealSectionUiState(
            id = definition.id,
            title = definition.title,
            recommendation = definition.timeMinutes?.toMealTimeLabel()
                ?: defaultDefinition?.recommendation()
                ?: "Custom meal",
            caloriesKcal = totals?.caloriesKcal ?: 0.0,
            calorieTargetKcal = calorieTargetKcal,
            calorieProgress = (totals?.caloriesKcal ?: 0.0).fractionOf(calorieTargetKcal),
            proteinGrams = totals?.proteinGrams ?: 0.0,
            carbsGrams = totals?.carbsGrams ?: 0.0,
            carbsLabel = if (useNetCarbs) "Net carbs" else "Carbs",
            effectiveCarbsGrams = effectiveCarbsGrams,
            fatGrams = totals?.fatGrams ?: 0.0,
            proteinGoalGrams = proteinGoalGrams,
            carbsGoalGrams = carbsGoalGrams,
            fatGoalGrams = fatGoalGrams,
            fiberGrams = detailTotals.fiberGrams,
            sugarGrams = detailTotals.sugarGrams,
            saturatedFatGrams = detailTotals.saturatedFatGrams,
            sodiumMilligrams = detailTotals.sodiumMilligrams,
            plannedCaloriesKcal = meal?.plannedTotals?.caloriesKcal ?: 0.0,
            plannedProteinGrams = meal?.plannedTotals?.proteinGrams ?: 0.0,
            plannedCarbsGrams = meal?.plannedTotals?.carbsGrams ?: 0.0,
            plannedFatGrams = meal?.plannedTotals?.fatGrams ?: 0.0,
            advancedNutritionProgress = detailTotals.toAdvancedNutritionProgress(
                fiberGoalGrams = fiberGoalGrams,
                sugarGoalGrams = sugarGoalGrams,
                saturatedFatGoalGrams = saturatedFatGoalGrams,
                sodiumGoalMilligrams = sodiumGoalMilligrams,
            ),
            micronutrients = detailTotals.toMicronutrients(),
            rating = meal.toFoodMealRating(calorieTargetKcal),
            entries = meal?.entries.orEmpty().map { entry ->
                FoodMealEntryUiState(
                    id = entry.id,
                    foodId = entry.foodId,
                    name = entry.name,
                    brand = entry.brand,
                    quantityGrams = entry.quantityGrams,
                    caloriesKcal = entry.caloriesKcal,
                    proteinGrams = entry.proteinGrams,
                    carbsGrams = entry.carbsGrams,
                    fatGrams = entry.fatGrams,
                    fiberGrams = entry.nutritionDetails.fiberGrams,
                    sugarGrams = entry.nutritionDetails.sugarGrams,
                    saturatedFatGrams = entry.nutritionDetails.saturatedFatGrams,
                    sodiumMilligrams = entry.nutritionDetails.sodiumMilligrams,
                    calorieContribution = entry.caloriesKcal.fractionOf(totals?.caloriesKcal ?: 0.0),
                    proteinContribution = entry.proteinGrams.fractionOf(totals?.proteinGrams ?: 0.0),
                    carbsContribution = entry.carbsGrams.fractionOf(totals?.carbsGrams ?: 0.0),
                    fatContribution = entry.fatGrams.fractionOf(totals?.fatGrams ?: 0.0),
                    isPlanned = entry.status == FoodDiaryEntryStatus.Planned,
                    imageUrl = entry.imageUrl,
                )
            },
        )
    }
}

private fun Double.fractionOf(total: Double): Double =
    if (isFinite() && total.isFinite() && total > 0.0) {
        (this / total).coerceIn(0.0, 1.0)
    } else {
        0.0
    }

private fun SavedFoodItem.toUiState(): SavedFoodUiState {
    val servingMultiplier = defaultServingGrams / 100.0
    return SavedFoodUiState(
        id = id,
        imageUrl = imageUrl,
        name = name,
        brand = brand,
        defaultServingGrams = defaultServingGrams,
        caloriesPer100g = nutritionPer100g.caloriesKcal,
        proteinPer100g = nutritionPer100g.proteinGrams,
        carbsPer100g = nutritionPer100g.carbsGrams,
        fatPer100g = nutritionPer100g.fatGrams,
        caloriesPerServingKcal = nutritionPer100g.caloriesKcal * servingMultiplier,
        proteinPerServingGrams = nutritionPer100g.proteinGrams * servingMultiplier,
        carbsPerServingGrams = nutritionPer100g.carbsGrams * servingMultiplier,
        fatPerServingGrams = nutritionPer100g.fatGrams * servingMultiplier,
        fiberPer100g = nutritionDetailsPer100g.fiberGrams,
        sugarPer100g = nutritionDetailsPer100g.sugarGrams,
        saturatedFatPer100g = nutritionDetailsPer100g.saturatedFatGrams,
        sodiumMgPer100g = nutritionDetailsPer100g.sodiumMilligrams,
        potassiumMgPer100g = nutritionDetailsPer100g.potassiumMilligrams,
        calciumMgPer100g = nutritionDetailsPer100g.calciumMilligrams,
        ironMgPer100g = nutritionDetailsPer100g.ironMilligrams,
        vitaminDMcgPer100g = nutritionDetailsPer100g.vitaminDMicrograms,
        vitaminCMgPer100g = nutritionDetailsPer100g.vitaminCMilligrams,
        magnesiumMgPer100g = nutritionDetailsPer100g.magnesiumMilligrams,
        servingName = servingName,
        barcode = barcode,
        category = category,
        isFavorite = isFavorite,
        sourceLabel = sourceLabel(),
        servings = servings.map { serving ->
            SavedFoodServingUiState(
                id = serving.id,
                label = serving.label,
                grams = serving.grams,
            )
        },
    )
}

private fun FoodPlanDay.toUiState(): FoodPlanDayUiState =
    FoodPlanDayUiState(
        date = date,
        dayLabel = date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.titlecase() },
        loggedCaloriesKcal = loggedTotals.caloriesKcal,
        plannedCaloriesKcal = plannedTotals.caloriesKcal,
        loggedEntryCount = loggedEntryCount,
        plannedEntryCount = plannedEntryCount,
    )

private fun ShoppingListGroup.toUiState(): ShoppingListGroupUiState =
    ShoppingListGroupUiState(
        category = category,
        items = items.map { it.toUiState() },
    )

private fun ShoppingListItem.toUiState(): ShoppingListItemUiState =
    ShoppingListItemUiState(
        id = id,
        name = name,
        category = category,
        quantityGrams = quantityGrams,
        quantityLabel = "${quantityGrams.formatInputNumber()} g",
        isChecked = isChecked,
        isManual = isManual,
    )

private fun SavedFoodItem.sourceLabel(): String =
    when {
        category.equals("Nutrition label", ignoreCase = true) -> "Label"
        !barcode.isNullOrBlank() -> "Scanned"
        else -> "Manual"
    }

private fun SavedFoodUiState.toRecipeIngredientServingChoices(): List<RecipeIngredientServingChoiceUiState> {
    val choices = mutableListOf(
        RecipeIngredientServingChoiceUiState(id = "g", label = "g", grams = 1.0),
    )
    defaultServingGrams.takeIf { it.isFinite() && it > 0.0 }?.let { grams ->
        choices += RecipeIngredientServingChoiceUiState(
            id = "default-serving",
            label = servingName?.takeIf { it.isNotBlank() } ?: "${grams.formatInputNumber()} g",
            grams = grams,
        )
    }
    servings
        .filter { serving -> serving.grams.isFinite() && serving.grams > 0.0 && serving.label.isNotBlank() }
        .forEach { serving ->
            choices += RecipeIngredientServingChoiceUiState(
                id = serving.id,
                label = serving.label,
                grams = serving.grams,
            )
        }
    return choices.distinctBy { choice -> choice.id }
}

private fun List<SavedFoodUiState>.filterForDatabaseQuery(query: String): List<SavedFoodUiState> {
    val normalizedQuery = query.trim().lowercase()
    if (normalizedQuery.isBlank()) {
        return this
    }
    return filter { food -> food.matchesDatabaseQuery(normalizedQuery) }
}

private fun SavedFoodUiState.matchesDatabaseQuery(query: String): Boolean =
    listOf(name, brand.orEmpty(), barcode.orEmpty(), category.orEmpty())
        .any { value -> value.lowercase().contains(query) }

private fun List<SavedFoodUiState>.toDuplicateFoodGroups(): List<FoodDuplicateGroupUiState> {
    val groups = mutableListOf<FoodDuplicateGroupUiState>()
    val groupedFoodIds = mutableSetOf<String>()

    filter { !it.barcode.isNullOrBlank() }
        .groupBy { it.barcode.orEmpty().trim().lowercase() }
        .values
        .filter { it.size > 1 }
        .forEach { duplicateFoods ->
            val sortedFoods = duplicateFoods.sortedBy { it.name.lowercase() }
            val primaryFood = sortedFoods.first()
            val duplicateIds = sortedFoods.drop(1).map { it.id }
            groups += FoodDuplicateGroupUiState(
                primaryFoodId = primaryFood.id,
                duplicateFoodIds = duplicateIds,
                title = primaryFood.name,
                reason = "Barcode ${primaryFood.barcode}",
            )
            groupedFoodIds += sortedFoods.map { it.id }
        }

    filter { it.id !in groupedFoodIds }
        .groupBy { "${it.name.trim().lowercase()}|${it.brand.orEmpty().trim().lowercase()}" }
        .values
        .filter { it.size > 1 }
        .forEach { duplicateFoods ->
            val sortedFoods = duplicateFoods.sortedWith(compareBy<SavedFoodUiState> { it.name.lowercase() }.thenBy { it.id })
            val primaryFood = sortedFoods.first()
            groups += FoodDuplicateGroupUiState(
                primaryFoodId = primaryFood.id,
                duplicateFoodIds = sortedFoods.drop(1).map { it.id },
                title = primaryFood.name,
                reason = "Name and brand",
            )
        }

    return groups
}

private fun MealTemplate.toUiState(): MealTemplateUiState =
    MealTemplateUiState(
        id = id,
        name = name,
        mealType = mealType,
        isFavorite = isFavorite,
        itemSummary = items.joinToString { item -> "${item.foodName} ${item.quantityGrams.formatInputNumber()}g" },
        items = items.map { item ->
            MealTemplateItemDraftUiState(
                foodId = item.foodId,
                foodName = item.foodName,
                quantityGrams = item.quantityGrams.formatInputNumber(),
            )
        },
    )

private fun Recipe.toUiState(): RecipeUiState =
    RecipeUiState(
        id = id,
        name = name,
        category = category,
        servingName = servingName,
        servingGrams = servingGrams,
        servings = servings,
        cookedYieldGrams = cookedYieldGrams,
        isFavorite = isFavorite,
        caloriesPerServingKcal = nutritionPerServing.caloriesKcal,
        proteinPerServingGrams = nutritionPerServing.proteinGrams,
        carbsPerServingGrams = nutritionPerServing.carbsGrams,
        fatPerServingGrams = nutritionPerServing.fatGrams,
        itemSummary = ingredients.joinToString { ingredient ->
            "${ingredient.foodName} ${ingredient.unitQuantity.formatInputNumber()} ${ingredient.unitLabel}"
        },
        ingredients = ingredients.map { ingredient ->
            RecipeIngredientDraftUiState(
                foodId = ingredient.foodId,
                foodName = ingredient.foodName,
                quantityGrams = ingredient.quantityGrams,
                unitLabel = ingredient.unitLabel,
                unitGrams = ingredient.unitGrams,
                unitQuantity = ingredient.unitQuantity,
            )
        },
    )

private fun QuickCaloriePreset.toUiState(): QuickCaloriePresetUiState =
    QuickCaloriePresetUiState(
        id = id,
        name = name,
        caloriesKcal = caloriesKcal,
        proteinGrams = proteinGrams,
        carbsGrams = carbsGrams,
        fatGrams = fatGrams,
        isFavorite = isFavorite,
    )

private fun defaultAmountServingChoices(): List<FoodAmountServingChoiceUiState> =
    listOf(FoodAmountServingChoiceUiState("per-100g", "100 g", 100.0))

private fun ProductLookupResult.Found.toAmountServingChoices(): List<FoodAmountServingChoiceUiState> {
    val choices = mutableListOf<FoodAmountServingChoiceUiState>()
    choices += defaultAmountServingChoices()
    val servingGrams = servingQuantityGrams?.takeIf { it.isFinite() && it > 0.0 }
    if (servingGrams != null && servingGrams != 100.0) {
        choices += FoodAmountServingChoiceUiState(
            id = "serving",
            label = "Serving ${servingGrams.formatInputNumber()} g",
            grams = servingGrams,
        )
    }
    return choices
}

private fun SavedFoodUiState.toDiaryEntryServingChoices(): List<FoodAmountServingChoiceUiState> {
    val choices = mutableListOf<FoodAmountServingChoiceUiState>()
    choices += defaultAmountServingChoices()

    val defaultGrams = defaultServingGrams.takeIf { it.isFinite() && it > 0.0 }
    if (defaultGrams != null && defaultGrams != 100.0) {
        choices += FoodAmountServingChoiceUiState(
            id = "default-serving",
            label = servingName?.takeIf { it.isNotBlank() } ?: "${defaultGrams.formatInputNumber()} g",
            grams = defaultGrams,
        )
    }

    servings
        .filter { serving -> serving.grams.isFinite() && serving.grams > 0.0 && serving.label.isNotBlank() }
        .forEach { serving ->
            choices += FoodAmountServingChoiceUiState(
                id = serving.id,
                label = serving.label,
                grams = serving.grams,
            )
        }

    return choices.distinctBy { choice -> "${choice.label.trim().lowercase()}|${choice.grams}" }
}

private fun ProductLookupResult.Found.toOnlineUiState(): OnlineFoodResultUiState =
    OnlineFoodResultUiState(
        barcode = barcode,
        name = name,
        brand = brand,
        servingQuantityGrams = servingQuantityGrams,
        caloriesPer100g = nutritionPer100g.caloriesKcal,
        proteinPer100g = nutritionPer100g.proteinGrams,
        carbsPer100g = nutritionPer100g.carbsGrams,
        fatPer100g = nutritionPer100g.fatGrams,
        fiberPer100g = nutritionDetailsPer100g.fiberGrams,
        sugarPer100g = nutritionDetailsPer100g.sugarGrams,
        saturatedFatPer100g = nutritionDetailsPer100g.saturatedFatGrams,
        sodiumMgPer100g = nutritionDetailsPer100g.sodiumMilligrams,
        potassiumMgPer100g = nutritionDetailsPer100g.potassiumMilligrams,
        calciumMgPer100g = nutritionDetailsPer100g.calciumMilligrams,
        ironMgPer100g = nutritionDetailsPer100g.ironMilligrams,
        vitaminDMcgPer100g = nutritionDetailsPer100g.vitaminDMicrograms,
        vitaminCMgPer100g = nutritionDetailsPer100g.vitaminCMilligrams,
        magnesiumMgPer100g = nutritionDetailsPer100g.magnesiumMilligrams,
        category = category,
        imageUrl = imageUrl,
    )

private fun OnlineFoodResultUiState.toSavedFoodUpsertInput(): SavedFoodUpsertInput =
    SavedFoodUpsertInput(
        foodId = null,
        name = name,
        brand = brand,
        defaultServingGrams = servingQuantityGrams ?: 100.0,
        imageUrl = imageUrl,
        nutritionPer100g = FoodNutrition(
            caloriesKcal = caloriesPer100g,
            proteinGrams = proteinPer100g,
            carbsGrams = carbsPer100g,
            fatGrams = fatPer100g,
        ),
        nutritionDetailsPer100g = NutritionDetails(
            fiberGrams = fiberPer100g,
            sugarGrams = sugarPer100g,
            saturatedFatGrams = saturatedFatPer100g,
            sodiumMilligrams = sodiumMgPer100g,
            potassiumMilligrams = potassiumMgPer100g,
            calciumMilligrams = calciumMgPer100g,
            ironMilligrams = ironMgPer100g,
            vitaminDMicrograms = vitaminDMcgPer100g,
            vitaminCMilligrams = vitaminCMgPer100g,
            magnesiumMilligrams = magnesiumMgPer100g,
        ),
        servingName = servingQuantityGrams?.formatInputNumber()?.let { "$it g" },
        barcode = barcode,
        category = category,
        servings = listOfNotNull(
            FoodServingInput("100 g", 100.0),
            servingQuantityGrams?.takeIf { it > 0.0 && it != 100.0 }?.let { grams ->
                FoodServingInput("${grams.formatInputNumber()} g", grams)
            },
        ),
    )

private fun NutritionTotals.toMacroProgress(
    carbsGoalGrams: Double = CARBS_GOAL_GRAMS,
    proteinGoalGrams: Double = PROTEIN_GOAL_GRAMS,
    fatGoalGrams: Double = FAT_GOAL_GRAMS,
    fiberGrams: Double = 0.0,
    useNetCarbs: Boolean = false,
): List<FoodMacroProgressUiState> =
    listOf(
        FoodMacroProgressUiState(
            label = if (useNetCarbs) "Net carbs" else "Carbs",
            currentGrams = if (useNetCarbs) (carbsGrams - fiberGrams).coerceAtLeast(0.0) else carbsGrams,
            goalGrams = carbsGoalGrams,
        ),
        FoodMacroProgressUiState("Protein", proteinGrams, proteinGoalGrams),
        FoodMacroProgressUiState("Fat", fatGrams, fatGoalGrams),
    )

private fun NutritionDetails.toAdvancedNutritionProgress(
    fiberGoalGrams: Double = FIBER_GOAL_GRAMS,
    sugarGoalGrams: Double = SUGAR_GOAL_GRAMS,
    saturatedFatGoalGrams: Double = SATURATED_FAT_GOAL_GRAMS,
    sodiumGoalMilligrams: Double = SODIUM_GOAL_MILLIGRAMS,
): List<FoodNutrientProgressUiState> =
    listOf(
        FoodNutrientProgressUiState(
            label = "Fiber",
            currentValue = fiberGrams,
            goalValue = fiberGoalGrams,
            unit = "g",
            isLimit = false,
        ),
        FoodNutrientProgressUiState(
            label = "Sugar",
            currentValue = sugarGrams,
            goalValue = sugarGoalGrams,
            unit = "g",
            isLimit = true,
        ),
        FoodNutrientProgressUiState(
            label = "Sat fat",
            currentValue = saturatedFatGrams,
            goalValue = saturatedFatGoalGrams,
            unit = "g",
            isLimit = true,
        ),
        FoodNutrientProgressUiState(
            label = "Sodium",
            currentValue = sodiumMilligrams,
            goalValue = sodiumGoalMilligrams,
            unit = "mg",
            isLimit = true,
        ),
    )

private fun NutritionDetails.toMicronutrients(): List<FoodMicronutrientUiState> =
    listOf(
        FoodMicronutrientUiState("Sodium", sodiumMilligrams, "mg"),
        FoodMicronutrientUiState("Potassium", potassiumMilligrams, "mg"),
        FoodMicronutrientUiState("Calcium", calciumMilligrams, "mg"),
        FoodMicronutrientUiState("Iron", ironMilligrams, "mg"),
        FoodMicronutrientUiState("Vitamin D", vitaminDMicrograms, "mcg"),
        FoodMicronutrientUiState("Vitamin C", vitaminCMilligrams, "mg"),
        FoodMicronutrientUiState("Magnesium", magnesiumMilligrams, "mg"),
    )

private fun emptyMealSections(): List<FoodMealSectionUiState> =
    mealDefinitions.map { definition ->
        FoodMealSectionUiState(
            id = definition.id,
            title = definition.title,
            recommendation = definition.recommendation,
            caloriesKcal = 0.0,
            calorieTargetKcal = definition.calorieTargetKcal,
            calorieProgress = 0.0,
            proteinGrams = 0.0,
            carbsGrams = 0.0,
            fatGrams = 0.0,
            entries = emptyList(),
        )
    }

private fun emptyFoodDiary(): FoodDiary =
    FoodDiary(
        totals = NutritionTotals(0.0, 0.0, 0.0, 0.0),
        meals = emptyList(),
    )

private fun defaultMealDefinitionUiStates(): List<FoodMealDefinitionUiState> =
    mealDefinitions.map { definition -> definition.toUiState() }

private fun List<FoodMealDefinition>.toMealDefinitionUiStates(): List<FoodMealDefinitionUiState> {
    val overridesById = associate { definition ->
        definition.id.normalizedMealType() to definition.toUiState()
    }
    val defaultDefinitions =
        mealDefinitions.map { defaultDefinition ->
            overridesById[defaultDefinition.id]
                ?.copy(isDefault = true)
                ?: defaultDefinition.toUiState()
        }
    val customDefinitions =
        overridesById.values.filterNot { definition -> definition.id in defaultMealDefinitionIds }

    return (defaultDefinitions + customDefinitions)
        .distinctBy { definition -> definition.id }
        .sortedWith(compareBy<FoodMealDefinitionUiState> { it.sortOrder }.thenBy { it.title.lowercase() })
}

private fun MealDefinition.toUiState(): FoodMealDefinitionUiState =
    FoodMealDefinitionUiState(
        id = id,
        title = title,
        timeMinutes = timeMinutes,
        timeLabel = timeMinutes?.toMealTimeLabel() ?: "No time",
        sortOrder = sortOrder,
        isDefault = true,
    )

private fun FoodMealDefinition.toUiState(): FoodMealDefinitionUiState {
    val mealId = id.normalizedMealType()
    return FoodMealDefinitionUiState(
        id = mealId,
        title = name,
        timeMinutes = timeMinutes,
        timeLabel = timeMinutes?.toMealTimeLabel() ?: "No time",
        sortOrder = sortOrder,
        isDefault = mealId in defaultMealDefinitionIds,
    )
}

private fun FoodMealDefinitionUiState.recommendation(): String =
    timeMinutes?.toMealTimeLabel()
        ?: mealDefinitions.firstOrNull { it.id == id }?.recommendation
        ?: "Custom meal"

private fun FoodMealDefinitionUiState.calorieTargetKcal(): Double =
    mealDefinitions.firstOrNull { it.id == id }?.calorieTargetKcal
        ?: (CALORIE_GOAL_KCAL / 4.0)

private fun FoodUiState.mealTitleFor(mealType: String): String =
    mealDefinitions.firstOrNull { it.id == mealType.normalizedMealType() }?.title
        ?: mealType.mealTitle()

private fun FoodUiState.nextMealSortOrder(): Int =
    (mealDefinitions.maxOfOrNull { it.sortOrder } ?: 30) + 10

private fun emptyMacroProgress(): List<FoodMacroProgressUiState> =
    NutritionTotals(0.0, 0.0, 0.0, 0.0).toMacroProgress()

private fun emptyAdvancedNutritionProgress(): List<FoodNutrientProgressUiState> =
    NutritionDetails().toAdvancedNutritionProgress()

private fun emptyMicronutrients(): List<FoodMicronutrientUiState> =
    NutritionDetails().toMicronutrients()

private fun emptyDailyInsights(): List<FoodInsightUiState> =
    listOf(
        FoodInsightUiState(
            title = "Start with a meal",
            body = "Log a meal, favorite, or quick calories to see today clearly.",
            tone = FoodInsightTone.Neutral,
        ),
    )

private fun emptyFoodRating(): FoodRatingUiState =
    FoodRatingUiState(
        label = "No rating",
        reason = "Log food to rate today.",
        suggestion = "Start with a meal or favorite.",
        tone = FoodInsightTone.Neutral,
    )

private fun defaultEmptyDiaryActions(): List<EmptyDiaryActionUiState> =
    listOf(
        EmptyDiaryActionUiState(
            type = EmptyDiaryActionType.Breakfast,
            label = "Add breakfast",
            accessibilityLabel = "Add breakfast to food diary",
        ),
        EmptyDiaryActionUiState(
            type = EmptyDiaryActionType.Barcode,
            label = "Scan barcode",
            accessibilityLabel = "Scan barcode to add food",
        ),
    )

private fun String.normalizedMealType(): String {
    val normalized = trim().lowercase()
    return when (normalized) {
        "snack" -> "snacks"
        else -> normalized.ifBlank { "breakfast" }
    }
}

private fun String.mealTitle(): String =
    mealDefinitions.firstOrNull { it.id == normalizedMealType() }?.title
        ?: trim()
            .replace('_', ' ')
            .replace('-', ' ')
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { char -> char.uppercase() }
            }
            .ifBlank { "Meal" }

private fun String.parseMealTimeMinutesOrNull(): Int? {
    val parts = trim().split(":")
    if (parts.size != 2) {
        return null
    }
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) {
        return null
    }
    return hour * 60 + minute
}

private fun Int.toMealTimeLabel(): String =
    "${(this / 60).toString().padStart(2, '0')}:${(this % 60).toString().padStart(2, '0')}"

private fun String.parseNutritionValue(): Double? =
    trim()
        .takeIf { it.isNotEmpty() }
        ?.toDoubleOrNull()
        ?.takeIf { it.isFinite() && it >= 0.0 }

private fun String.parsePositiveNumberOrNull(): Double? =
    trim()
        .takeIf { it.isNotEmpty() }
        ?.toDoubleOrNull()
        ?.takeIf { it.isFinite() && it > 0.0 }

private fun String.parseNonNegativeNumberOrZero(): Double? {
    val trimmed = trim()
    if (trimmed.isEmpty()) {
        return 0.0
    }
    return trimmed.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }
}

private fun String.parseDateOrNull(): LocalDate? =
    runCatching { LocalDate.parse(trim()) }.getOrNull()

private fun String.sanitizeDecimalInput(): String {
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

private fun Double.formatInputNumber(): String {
    val longValue = toLong()
    return if (this == longValue.toDouble()) {
        longValue.toString()
    } else {
        toString()
    }
}
