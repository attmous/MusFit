package com.musfit.ui.food

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musfit.data.repository.DiaryEntryUpdateInput
import com.musfit.data.repository.FoodDiary
import com.musfit.data.repository.FoodDiaryEntry
import com.musfit.data.repository.FoodDiaryEntryStatus
import com.musfit.data.repository.FoodDiaryMeal
import com.musfit.data.repository.FoodGoal
import com.musfit.data.repository.FoodGoalMode
import com.musfit.data.repository.FoodHealthConnectSyncResult
import com.musfit.data.repository.FoodHealthConnectSyncState
import com.musfit.data.repository.FoodLogInput
import com.musfit.data.repository.FoodMealDefinition
import com.musfit.data.repository.FoodMealDefinitionInput
import com.musfit.data.repository.FoodPlanDay
import com.musfit.data.repository.FoodProductProvider
import com.musfit.data.repository.FoodRepository
import com.musfit.data.repository.FoodServingInput
import com.musfit.data.repository.FoodWaterSummary
import com.musfit.data.repository.ManualShoppingListItemInput
import com.musfit.data.repository.MealTemplate
import com.musfit.data.repository.MealTemplateItemInput
import com.musfit.data.repository.MealTemplateUpdateInput
import com.musfit.data.repository.NutritionDetails
import com.musfit.data.repository.ProductLookupResult
import com.musfit.data.repository.ProductSearchResult
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.Serializable
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlin.math.roundToInt

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

enum class RecipeDiscoveryFilter {
    All,
    HighProtein,
    LowCarb,
    Vegetarian,
    Quick,
    Favorites,
    Program,
}

enum class BarcodeComparisonSide {
    Left,
    Right,
}

enum class FoodSheetMode {
    AddFood,
    FoodDatabase,
    FoodDetail,
    DiaryEntryEditor,
    SavedFoodEditor,
    NutritionLabelScan,
    BarcodeComparison,
    FastingTimer,
    GoalEditor,
    RecipeBrowser,
    RecipeEditor,
    MealTemplates,
    MealSettings,
    ShoppingList,
    Water,
    HealthConnect,
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
    val score: Int? = null,
    val factors: List<FoodRatingFactorUiState> = emptyList(),
)

data class FoodRatingFactorUiState(
    val label: String,
    val valueLabel: String,
    val explanation: String,
    val tone: FoodInsightTone,
)

data class NutritionLabelScanReviewUiState(
    val confidenceLabel: String,
    val parsedFieldCount: Int,
)

data class FoodProgramUiState(
    val id: String,
    val mode: FoodGoalMode,
    val title: String,
    val subtitle: String,
    val description: String,
    val macroTargetsLabel: String,
    val suggestedHabits: List<String>,
    val mealPlanningTip: String,
    val isSelected: Boolean,
)

data class RecipeDiscoveryUiState(
    val filter: RecipeDiscoveryFilter = RecipeDiscoveryFilter.All,
    val query: String = "",
    val items: List<RecipeDiscoveryItemUiState> = emptyRecipeDiscoveryItems(),
    val visibleItems: List<RecipeDiscoveryItemUiState> = emptyRecipeDiscoveryItems(),
)

data class RecipeDiscoveryItemUiState(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val servingName: String,
    val servingGrams: Double,
    val caloriesKcal: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val tagLabels: List<String>,
    val isFavorite: Boolean,
    val isSavedRecipe: Boolean,
    val programRelevant: Boolean,
    val sourceRecipeId: String? = null,
    val mealTypeIds: List<String> = emptyList(),
    val thumbnailKey: String = "bowl",
)

enum class FoodHabitStatus {
    Complete,
    InProgress,
    Missing,
}

data class FoodHabitTrackerUiState(
    val id: String,
    val label: String,
    val valueLabel: String,
    val progress: Double,
    val status: FoodHabitStatus,
    val tone: FoodInsightTone,
    val suggestion: String,
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
    val rating: FoodRatingUiState? = null,
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
    val isHidden: Boolean = false,
)

enum class FoodTrustLevel {
    Imported,
    Manual,
    NeedsReview,
}

data class FoodTrustUiState(
    val level: FoodTrustLevel,
    val label: String,
    val explanation: String,
    val actionLabel: String,
    val isReported: Boolean = false,
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
    val trust: FoodTrustUiState = emptyFoodTrust(),
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

data class BarcodeComparisonItemUiState(
    val barcode: String,
    val name: String,
    val brand: String?,
    val sourceLabel: String,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val sugarPer100g: Double,
    val sodiumMgPer100g: Double,
    val imageUrl: String? = null,
)

data class BarcodeComparisonHighlightUiState(
    val label: String,
    val leftValue: String,
    val rightValue: String,
    val winnerSide: BarcodeComparisonSide?,
)

data class BarcodeComparisonUiState(
    val leftBarcodeInput: String = "",
    val rightBarcodeInput: String = "",
    val leftItem: BarcodeComparisonItemUiState? = null,
    val rightItem: BarcodeComparisonItemUiState? = null,
    val highlights: List<BarcodeComparisonHighlightUiState> = emptyList(),
    val isLoading: Boolean = false,
) {
    val items: List<BarcodeComparisonItemUiState>
        get() = listOfNotNull(leftItem, rightItem)
}

data class FastingProgramUiState(
    val id: String,
    val title: String,
    val fastingHours: Double,
    val eatingHours: Double,
    val description: String,
    val isSelected: Boolean,
)

data class FastingTimerUiState(
    val selectedProgramId: String = "16-8",
    val programs: List<FastingProgramUiState> = emptyFastingPrograms("16-8", 16.0, 8.0),
    val fastingStartInput: String = "20:00",
    val fastingWindowLabel: String = "20:00 - 12:00",
    val eatingWindowLabel: String = "12:00 - 20:00",
    val statusLabel: String = "16:8 fasting plan active",
    val progress: Double = 16.0 / 24.0,
    val customFastingHoursInput: String = "16",
    val customEatingHoursInput: String = "8",
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
) : Serializable {
    private companion object {
        const val serialVersionUID = 1L
    }
}

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
) : Serializable {
    private companion object {
        const val serialVersionUID = 1L
    }
}

data class RecipeIngredientServingChoiceUiState(
    val id: String,
    val label: String,
    val grams: Double,
) : Serializable {
    private companion object {
        const val serialVersionUID = 1L
    }
}

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

data class MealTemplateEditorState(
    val id: String,
    val name: String = "",
    val mealType: String = "breakfast",
    val items: List<MealTemplateItemDraftUiState> = emptyList(),
    val newItemFoodId: String = "",
    val newItemQuantityGrams: String = "100",
) : Serializable {
    private companion object {
        const val serialVersionUID = 1L
    }
}

data class RecipeEditorState(
    val editingRecipeId: String? = null,
    val name: String = "",
    val category: String = "",
    val servingName: String = "Serving",
    val servingGrams: String = "100",
    val servingsCount: String = "1",
    val cookedYieldGrams: String = "100",
    val ingredientFoodId: String = "",
    val ingredientServingChoiceId: String = "g",
    val ingredientServingChoices: List<RecipeIngredientServingChoiceUiState> = emptyList(),
    val ingredientQuantityGrams: String = "100",
    val ingredients: List<RecipeIngredientDraftUiState> = emptyList(),
) : Serializable {
    private companion object {
        const val serialVersionUID = 1L
    }
}

data class SavedFoodEditorState(
    val id: String? = null,
    val name: String = "",
    val brand: String = "",
    val servingGrams: String = "100",
    val caloriesPer100g: String = "",
    val proteinPer100g: String = "",
    val carbsPer100g: String = "",
    val fatPer100g: String = "",
    val fiberPer100g: String = "",
    val sugarPer100g: String = "",
    val saturatedFatPer100g: String = "",
    val sodiumMgPer100g: String = "",
    val potassiumMgPer100g: String = "",
    val calciumMgPer100g: String = "",
    val ironMgPer100g: String = "",
    val vitaminDMcgPer100g: String = "",
    val vitaminCMgPer100g: String = "",
    val magnesiumMgPer100g: String = "",
    val servingName: String = "",
    val barcode: String = "",
    val category: String = "",
    val isFavorite: Boolean = false,
) : Serializable {
    private companion object {
        const val serialVersionUID = 1L
    }
}

private data class FoodRestorationState(
    val selectedDate: String,
    val isAddPanelVisible: Boolean,
    val sheetMode: String?,
    val addMode: String,
    val addTab: String,
    val mealType: String,
    val selectedMealTitle: String,
    val foodDatabaseQuery: String,
    val barcode: String,
    val productName: String,
    val brand: String,
    val caloriesPer100g: String,
    val proteinPer100g: String,
    val carbsPer100g: String,
    val fatPer100g: String,
    val fiberPer100g: String,
    val sugarPer100g: String,
    val saturatedFatPer100g: String,
    val sodiumMgPer100g: String,
    val quantityGrams: String,
    val savedFoodEditor: SavedFoodEditorState?,
    val recipeEditor: RecipeEditorState?,
    val mealTemplateEditor: MealTemplateEditorState?,
) : Serializable {
    private companion object {
        const val serialVersionUID = 1L
    }
}

private const val FOOD_RESTORATION_STATE_KEY = "food.restoration.state"

private fun FoodUiState.toFoodRestorationState(): FoodRestorationState = FoodRestorationState(
    selectedDate = selectedDate.toString(),
    isAddPanelVisible = isAddPanelVisible,
    sheetMode = sheetMode?.name,
    addMode = addMode.name,
    addTab = addTab.name,
    mealType = mealType,
    selectedMealTitle = selectedMealTitle,
    foodDatabaseQuery = foodDatabaseQuery.take(80),
    barcode = barcode.take(80),
    productName = productName.take(160),
    brand = brand.take(160),
    caloriesPer100g = caloriesPer100g.take(24),
    proteinPer100g = proteinPer100g.take(24),
    carbsPer100g = carbsPer100g.take(24),
    fatPer100g = fatPer100g.take(24),
    fiberPer100g = fiberPer100g.take(24),
    sugarPer100g = sugarPer100g.take(24),
    saturatedFatPer100g = saturatedFatPer100g.take(24),
    sodiumMgPer100g = sodiumMgPer100g.take(24),
    quantityGrams = quantityGrams.take(24),
    savedFoodEditor = savedFoodEditor,
    recipeEditor = recipeEditor,
    mealTemplateEditor = mealTemplateEditor,
)

private fun FoodRestorationState.toFoodUiStateOrNull(): FoodUiState? {
    val restoredSheet = sheetMode?.let { savedSheet ->
        FoodSheetMode.entries.firstOrNull { it.name == savedSheet }
    }
    return FoodUiState(
        selectedDate = runCatching { LocalDate.parse(selectedDate) }.getOrElse { LocalDate.now() },
        isAddPanelVisible = isAddPanelVisible && restoredSheet != null,
        sheetMode = restoredSheet,
        addMode = FoodAddMode.entries.firstOrNull { it.name == addMode } ?: FoodAddMode.Saved,
        addTab = AddTab.entries.firstOrNull { it.name == addTab } ?: AddTab.Recents,
        mealType = mealType,
        selectedMealTitle = selectedMealTitle,
        foodDatabaseQuery = foodDatabaseQuery,
        barcode = barcode,
        productName = productName,
        brand = brand,
        caloriesPer100g = caloriesPer100g,
        proteinPer100g = proteinPer100g,
        carbsPer100g = carbsPer100g,
        fatPer100g = fatPer100g,
        fiberPer100g = fiberPer100g,
        sugarPer100g = sugarPer100g,
        saturatedFatPer100g = saturatedFatPer100g,
        sodiumMgPer100g = sodiumMgPer100g,
        quantityGrams = quantityGrams,
        savedFoodEditor = savedFoodEditor,
        recipeEditor = recipeEditor,
        mealTemplateEditor = mealTemplateEditor,
    )
}

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
    val fiberPer100g: String = "",
    val sugarPer100g: String = "",
    val saturatedFatPer100g: String = "",
    val sodiumMgPer100g: String = "",
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
    val burnedCaloriesKcal: Double = 0.0,
    val macroProgress: List<FoodMacroProgressUiState> = emptyMacroProgress(),
    val advancedNutritionProgress: List<FoodNutrientProgressUiState> = emptyAdvancedNutritionProgress(),
    val micronutrients: List<FoodMicronutrientUiState> = emptyMicronutrients(),
    val dailyInsights: List<FoodInsightUiState> = emptyDailyInsights(),
    val dayRating: FoodRatingUiState = emptyFoodRating(),
    val foodPrograms: List<FoodProgramUiState> = emptyFoodPrograms(),
    val habitTrackers: List<FoodHabitTrackerUiState> = emptyHabitTrackers(),
    val isFoodDiaryEmpty: Boolean = true,
    val emptyDiaryActions: List<EmptyDiaryActionUiState> = emptyList(),
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
    val reportedSavedFoodIds: Set<String> = emptySet(),
    val duplicateFoodGroups: List<FoodDuplicateGroupUiState> = emptyList(),
    val mealTemplates: List<MealTemplateUiState> = emptyList(),
    val recipes: List<RecipeUiState> = emptyList(),
    val recipeDiscovery: RecipeDiscoveryUiState = RecipeDiscoveryUiState(),
    val recipeBrowserDate: LocalDate = LocalDate.now(),
    val recipeBrowserMealType: String = "breakfast",
    val quickCaloriePresets: List<QuickCaloriePresetUiState> = emptyList(),
    val onlineFoodResults: List<OnlineFoodResultUiState> = emptyList(),
    val isSearchingFoods: Boolean = false,
    val barcodeComparison: BarcodeComparisonUiState = BarcodeComparisonUiState(),
    val fastingTimer: FastingTimerUiState = FastingTimerUiState(),
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
    val aiLoggingDraftReview: String? = null,
    val nutritionLabelScanReview: NutritionLabelScanReviewUiState? = null,
    val keepAddingFoods: Boolean = false,
    val foodDatabaseQuery: String = "",
    val recentFoods: List<SavedFoodUiState> = emptyList(),
    val sameAsYesterday: List<SavedFoodUiState> = emptyList(),
    val addTab: AddTab = AddTab.Recents,
    val diaryEntryEditor: DiaryEntryEditorState? = null,
    val savedFoodEditor: SavedFoodEditorState? = null,
    val mealTemplateEditor: MealTemplateEditorState? = null,
    val goalEditor: GoalEditorState = GoalEditorState(),
    val recipeEditor: RecipeEditorState? = null,
    val recipeServingsToLog: String = "1",
    val editingMealDefinitionId: String? = null,
    val customMealNameInput: String = "",
    val customMealTimeInput: String = "",
    val customMealSortOrderInput: String = "",
    val lastDeletedDiaryEntry: DeletedDiaryEntrySnapshot? = null,
) {
    /** Meal definitions offered as add/copy targets — hidden meals are excluded. */
    val visibleMealDefinitions: List<FoodMealDefinitionUiState>
        get() = mealDefinitions.filter { !it.isHidden }

    // When "Include training calories" is on, burned calories are added to the
    // daily allowance so "kcal left" reflects goal - eaten + burned. Kept as a
    // derived value so it stays correct no matter which input (goal, eaten,
    // burned, or the toggle) updates last.
    val trainingCalorieAllowanceKcal: Double
        get() = if (includeTrainingCalories) burnedCaloriesKcal else 0.0

    val effectiveCalorieBudgetKcal: Double
        get() = calorieGoalKcal + trainingCalorieAllowanceKcal

    val remainingCaloriesKcal: Double
        get() = effectiveCalorieBudgetKcal - eatenCaloriesKcal

    val favoriteAddItems: List<FavoriteAddItemUiState>
        get() = FoodPresentationReducers.favoriteAddItems(this)
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FoodViewModel @Inject constructor(
    private val provider: FoodProductProvider,
    private val repository: FoodRepository,
    private val savedStateHandle: SavedStateHandle? = null,
) : ViewModel() {
    private val restoredState = savedStateHandle
        ?.get<FoodRestorationState>(FOOD_RESTORATION_STATE_KEY)
        ?.toFoodUiStateOrNull()
    private val selectedDateFlow = MutableStateFlow(restoredState?.selectedDate ?: LocalDate.now())
    private val mutableState = MutableStateFlow(restoredState ?: FoodUiState())
    val state: StateFlow<FoodUiState> = mutableState.asStateFlow()
    val diaryState: StateFlow<FoodDiaryUiState> = mutableState
        .map(FoodPresentationReducers::diary)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = FoodPresentationReducers.diary(mutableState.value),
        )
    val routeState: StateFlow<FoodRouteUiState> = mutableState
        .map(FoodPresentationReducers::route)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = FoodPresentationReducers.route(mutableState.value),
        )
    val trackerState: StateFlow<FoodTrackerUiState> = mutableState
        .map(FoodPresentationReducers::trackers)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = FoodPresentationReducers.trackers(mutableState.value),
        )
    private var lookupJob: Job? = null
    private var transientMessageJob: Job? = null
    private var currentDiary: FoodDiary = emptyFoodDiary()

    init {
        viewModelScope.launch {
            mutableState.collect { currentState ->
                savedStateHandle?.set(FOOD_RESTORATION_STATE_KEY, currentState.toFoodRestorationState())
            }
        }
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
                mutableState.update { currentState -> currentState.withWaterSummary(summary, currentDiary) }
            }
        }
        viewModelScope.launch {
            selectedDateFlow.flatMapLatest { date ->
                repository.observeBurnedCalories(date)
            }.collect { burnedCalories ->
                mutableState.update { currentState -> currentState.copy(burnedCaloriesKcal = burnedCalories) }
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
                    val foodStates = savedFoods.map { food ->
                        food.toUiState(isReported = food.id in currentState.reportedSavedFoodIds)
                    }
                    currentState.copy(
                        savedFoods = foodStates,
                        visibleSavedFoods = foodStates.filterForDatabaseQuery(currentState.foodDatabaseQuery),
                        duplicateFoodGroups = foodStates.toDuplicateFoodGroups(),
                        selectedSavedFoodDetail = currentState.selectedSavedFoodDetail?.let { selectedFood ->
                            foodStates.firstOrNull { it.id == selectedFood.id } ?: selectedFood.withTrust(
                                isReported = selectedFood.id in currentState.reportedSavedFoodIds,
                            )
                        },
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
                    currentState.withRecipes(recipes.map { it.toUiState() })
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
        val targetDate = currentState.selectedDate.plusDays(1)
        if (!targetDate.isWithinFoodPlanningHorizon()) {
            mutableState.update { it.copy(message = FOOD_PLANNING_LIMIT_MESSAGE) }
            return
        }
        if (!markSaving()) {
            return
        }
        viewModelScope.launch {
            try {
                repository.copyDay(
                    fromDate = currentState.selectedDate,
                    toDate = targetDate,
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

    fun removeQuickWater(amountMilliliters: Double) {
        removeWaterAmount(amountMilliliters, clearCustomAmount = false)
    }

    fun removeCustomWater() {
        val amount = state.value.waterCustomAmountInput.parsePositiveNumberOrNull()
        if (amount == null) {
            mutableState.update { it.copy(message = "Enter a valid water amount") }
            return
        }
        removeWaterAmount(amount, clearCustomAmount = true)
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
                    )
                }
                showTransientMessage("Added ${amountMilliliters.formatInputNumber()} ml water")
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(isSaving = false, message = error.message ?: "Failed to log water") }
            }
        }
    }

    private fun removeWaterAmount(amountMilliliters: Double, clearCustomAmount: Boolean) {
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
                val removed = repository.removeWater(WaterLogInput(date = date, amountMilliliters = amountMilliliters))
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        waterCustomAmountInput = if (clearCustomAmount) "" else it.waterCustomAmountInput,
                        message = if (removed > 0.0) {
                            "Removed ${removed.formatInputNumber()} ml water"
                        } else {
                            "No water to remove"
                        },
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(isSaving = false, message = error.message ?: "Failed to remove water") }
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

    private fun showTransientMessage(message: String) {
        transientMessageJob?.cancel()
        mutableState.update { it.copy(message = message) }
        transientMessageJob = viewModelScope.launch {
            delay(TRANSIENT_MESSAGE_DISMISS_MILLIS)
            mutableState.update { currentState ->
                if (currentState.message == message) {
                    currentState.copy(message = null)
                } else {
                    currentState
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
                savedFoodEditor = null,
                mealTemplateEditor = null,
                recipeEditor = null,
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
        val draft = text.toLocalAiNutritionDraft()
        mutableState.update {
            it.withAiLoggingDraft(
                name = text.take(80),
                sourceLabel = "Text",
                caloriesPer100g = draft.caloriesKcal.formatInputNumber(),
                proteinPer100g = draft.proteinGrams.formatInputNumber(),
                carbsPer100g = draft.carbsGrams.formatInputNumber(),
                fatPer100g = draft.fatGrams.formatInputNumber(),
                review = draft.review,
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
                savedFoodEditor = null,
                selectedSavedFoodDetail = null,
            )
        }
    }

    fun openWaterSheet() {
        mutableState.update {
            it.copy(isAddPanelVisible = true, sheetMode = FoodSheetMode.Water, message = null)
        }
    }

    fun openHealthConnectSheet() {
        mutableState.update {
            it.copy(isAddPanelVisible = true, sheetMode = FoodSheetMode.HealthConnect, message = null)
        }
    }

    fun openBarcodeComparison() {
        mutableState.update {
            it.copy(
                isAddPanelVisible = true,
                sheetMode = FoodSheetMode.BarcodeComparison,
                message = null,
            )
        }
    }

    fun openFastingTimer() {
        mutableState.update {
            it.copy(
                isAddPanelVisible = true,
                sheetMode = FoodSheetMode.FastingTimer,
                message = null,
            )
        }
    }

    fun selectFastingProgram(programId: String) {
        if (fastingProgramDefinitions.none { it.id == programId }) {
            mutableState.update { it.copy(message = "Fasting program unavailable") }
            return
        }
        mutableState.update {
            it.copy(
                fastingTimer = buildFastingTimerUiState(
                    selectedProgramId = programId,
                    fastingStartInput = it.fastingTimer.fastingStartInput,
                    customFastingHoursInput = it.fastingTimer.customFastingHoursInput,
                    customEatingHoursInput = it.fastingTimer.customEatingHoursInput,
                ),
                message = null,
            )
        }
    }

    fun onFastingStartTimeChanged(value: String) {
        val sanitized = value.sanitizeFastingTimeInput()
        mutableState.update {
            it.copy(
                fastingTimer = buildFastingTimerUiState(
                    selectedProgramId = it.fastingTimer.selectedProgramId,
                    fastingStartInput = sanitized,
                    customFastingHoursInput = it.fastingTimer.customFastingHoursInput,
                    customEatingHoursInput = it.fastingTimer.customEatingHoursInput,
                ),
                message = null,
            )
        }
    }

    fun onCustomFastingHoursChanged(value: String) {
        mutableState.update {
            it.copy(
                fastingTimer = it.fastingTimer.copy(customFastingHoursInput = value.sanitizeDecimalInput()),
                message = null,
            )
        }
    }

    fun onCustomEatingHoursChanged(value: String) {
        mutableState.update {
            it.copy(
                fastingTimer = it.fastingTimer.copy(customEatingHoursInput = value.sanitizeDecimalInput()),
                message = null,
            )
        }
    }

    fun applyCustomFastingProgram() {
        val currentTimer = state.value.fastingTimer
        val fastingHours = currentTimer.customFastingHoursInput.parsePositiveNumberOrNull()
        val eatingHours = currentTimer.customEatingHoursInput.parsePositiveNumberOrNull()
        if (fastingHours == null || eatingHours == null || kotlin.math.abs((fastingHours + eatingHours) - 24.0) > 0.01) {
            mutableState.update { it.copy(message = "Enter fasting and eating hours that total 24") }
            return
        }
        mutableState.update {
            it.copy(
                fastingTimer = buildFastingTimerUiState(
                    selectedProgramId = "custom",
                    fastingStartInput = currentTimer.fastingStartInput,
                    customFastingHoursInput = fastingHours.formatInputNumber(),
                    customEatingHoursInput = eatingHours.formatInputNumber(),
                ),
                message = "Custom fasting plan active",
            )
        }
    }

    fun onBarcodeComparisonBarcodeChanged(side: BarcodeComparisonSide, value: String) {
        val sanitized = value.filter(Char::isDigit)
        mutableState.update {
            val comparison =
                when (side) {
                    BarcodeComparisonSide.Left ->
                        it.barcodeComparison.copy(leftBarcodeInput = sanitized, leftItem = null, highlights = emptyList())

                    BarcodeComparisonSide.Right ->
                        it.barcodeComparison.copy(rightBarcodeInput = sanitized, rightItem = null, highlights = emptyList())
                }
            it.copy(barcodeComparison = comparison, message = null)
        }
    }

    fun compareBarcodeProducts() {
        val currentState = state.value
        val leftBarcode = currentState.barcodeComparison.leftBarcodeInput
        val rightBarcode = currentState.barcodeComparison.rightBarcodeInput
        if (leftBarcode.isBlank() || rightBarcode.isBlank()) {
            mutableState.update { it.copy(message = "Enter two barcodes to compare") }
            return
        }
        if (currentState.barcodeComparison.isLoading) {
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(barcodeComparison = it.barcodeComparison.copy(isLoading = true), message = null) }
            try {
                val savedFoods = state.value.savedFoods
                val leftItem = lookupBarcodeComparisonItem(leftBarcode, savedFoods)
                val rightItem = lookupBarcodeComparisonItem(rightBarcode, savedFoods)
                mutableState.update { latestState ->
                    latestState.copy(
                        barcodeComparison = latestState.barcodeComparison.copy(
                            isLoading = false,
                            leftItem = leftItem,
                            rightItem = rightItem,
                            highlights = buildBarcodeComparisonHighlights(leftItem, rightItem),
                        ),
                        message = when {
                            leftItem != null && rightItem != null -> "Compared barcode products"
                            leftItem == null && rightItem == null -> "Products not found"
                            leftItem == null -> "Left product not found"
                            else -> "Right product not found"
                        },
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(barcodeComparison = it.barcodeComparison.copy(isLoading = false)) }
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        barcodeComparison = it.barcodeComparison.copy(isLoading = false),
                        message = error.message ?: "Failed to compare barcodes",
                    )
                }
            }
        }
    }

    private suspend fun lookupBarcodeComparisonItem(
        barcode: String,
        savedFoods: List<SavedFoodUiState>,
    ): BarcodeComparisonItemUiState? {
        val savedFood = savedFoods.firstOrNull { it.barcode == barcode }
        if (savedFood != null) {
            return savedFood.toBarcodeComparisonItem()
        }
        return when (val result = provider.lookupBarcode(barcode)) {
            is ProductLookupResult.Found -> result.toBarcodeComparisonItem()

            is ProductLookupResult.NotFound,
            is ProductLookupResult.Failed,
            -> null
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

    fun openRecipeBrowser() {
        mutableState.update {
            val keepCurrentTarget =
                it.sheetMode == FoodSheetMode.RecipeBrowser || it.sheetMode == FoodSheetMode.RecipeEditor
            val targetDate = if (keepCurrentTarget) it.recipeBrowserDate else it.selectedDate
            it.copy(
                isAddPanelVisible = true,
                sheetMode = FoodSheetMode.RecipeBrowser,
                recipeBrowserDate = targetDate.coerceInFoodPlanningHorizon(),
                recipeBrowserMealType = if (keepCurrentTarget) it.recipeBrowserMealType else it.mealType.normalizedMealType(),
                recipeEditor = null,
                message = null,
            )
        }
    }

    fun onRecipeBrowserMealChanged(value: String) {
        mutableState.update { it.copy(recipeBrowserMealType = value.normalizedMealType(), message = null) }
    }

    fun goToPreviousRecipeBrowserDay() {
        mutableState.update { it.copy(recipeBrowserDate = it.recipeBrowserDate.minusDays(1), message = null) }
    }

    fun goToNextRecipeBrowserDay() {
        mutableState.update {
            val nextDate = it.recipeBrowserDate.plusDays(1)
            if (nextDate.isWithinFoodPlanningHorizon()) {
                it.copy(recipeBrowserDate = nextDate, message = null)
            } else {
                it.copy(recipeBrowserDate = it.recipeBrowserDate.coerceInFoodPlanningHorizon(), message = FOOD_PLANNING_LIMIT_MESSAGE)
            }
        }
    }

    fun goToTodayRecipeBrowserDay() {
        mutableState.update { it.copy(recipeBrowserDate = LocalDate.now(), message = null) }
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

        // Preserve the meal's current visibility so editing name/time/order does not un-hide it.
        val preservedHidden =
            currentState.editingMealDefinitionId
                ?.let { id -> currentState.mealDefinitions.firstOrNull { it.id == id }?.isHidden }
                ?: false

        viewModelScope.launch {
            try {
                repository.upsertCustomMealDefinition(
                    FoodMealDefinitionInput(
                        mealId = currentState.editingMealDefinitionId,
                        name = mealName,
                        timeMinutes = timeMinutes,
                        sortOrder = sortOrder,
                        isHidden = preservedHidden,
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

    fun toggleMealHidden(mealId: String) {
        val currentState = state.value
        val normalizedId = mealId.normalizedMealType()
        val meal = currentState.mealDefinitions.firstOrNull { it.id == normalizedId } ?: return
        val willHide = !meal.isHidden
        if (willHide && currentState.mealDefinitions.count { !it.isHidden } <= 1) {
            mutableState.update { it.copy(message = "Keep at least one meal visible") }
            return
        }
        viewModelScope.launch {
            try {
                repository.upsertCustomMealDefinition(
                    FoodMealDefinitionInput(
                        mealId = meal.id,
                        name = meal.title,
                        timeMinutes = meal.timeMinutes,
                        sortOrder = meal.sortOrder,
                        isHidden = willHide,
                    ),
                )
                mutableState.update {
                    it.copy(message = if (willHide) "Hid ${meal.title}" else "Showing ${meal.title}")
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(message = error.message ?: "Failed to update meal") }
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
                    recipeEditor = RecipeEditorState(),
                    message = null,
                )
            } else {
                it.copy(
                    isAddPanelVisible = true,
                    sheetMode = FoodSheetMode.RecipeEditor,
                    recipeEditor = RecipeEditorState(
                        editingRecipeId = recipe.id,
                        name = recipe.name,
                        category = recipe.category.orEmpty(),
                        servingName = recipe.servingName,
                        servingGrams = recipe.servingGrams.formatInputNumber(),
                        servingsCount = recipe.servings.formatInputNumber(),
                        cookedYieldGrams = recipe.cookedYieldGrams.formatInputNumber(),
                        ingredients = recipe.ingredients,
                    ),
                    message = null,
                )
            }
        }
    }

    fun selectRecipeDiscoveryFilter(filter: RecipeDiscoveryFilter) {
        mutableState.update {
            it.copy(
                recipeDiscovery = it.recipeDiscovery.copy(
                    filter = filter,
                    visibleItems = it.recipeDiscovery.items.filterForRecipeDiscovery(filter, it.recipeDiscovery.query),
                ),
                message = null,
            )
        }
    }

    fun onRecipeDiscoveryQueryChanged(value: String) {
        val query = value.take(80)
        mutableState.update {
            it.copy(
                recipeDiscovery = it.recipeDiscovery.copy(
                    query = query,
                    visibleItems = it.recipeDiscovery.items.filterForRecipeDiscovery(it.recipeDiscovery.filter, query),
                ),
                message = null,
            )
        }
    }

    fun useRecipeDiscoveryItem(itemId: String) {
        val item = state.value.recipeDiscovery.items.firstOrNull { it.id == itemId } ?: run {
            mutableState.update { it.copy(message = "Recipe idea unavailable") }
            return
        }
        val savedRecipeId = item.sourceRecipeId
        if (savedRecipeId != null) {
            logRecipe(savedRecipeId)
            return
        }
        mutableState.update {
            it.copy(
                isAddPanelVisible = true,
                sheetMode = FoodSheetMode.RecipeEditor,
                recipeEditor = RecipeEditorState(
                    name = item.title,
                    category = item.category,
                    servingName = item.servingName,
                    servingGrams = item.servingGrams.formatInputNumber(),
                    servingsCount = "1",
                    cookedYieldGrams = item.servingGrams.formatInputNumber(),
                ),
                message = "Review and save ${item.title}",
            )
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
                mealTemplateEditor = MealTemplateEditorState(
                    id = template.id,
                    name = template.name,
                    mealType = template.mealType,
                    items = template.items,
                ),
                message = null,
            )
        }
    }

    fun onTemplateNameChanged(value: String) {
        mutableState.update { it.copy(mealTemplateEditor = it.mealTemplateEditor?.copy(name = value), message = null) }
    }

    fun onTemplateMealTypeChanged(value: String) {
        mutableState.update { it.copy(mealTemplateEditor = it.mealTemplateEditor?.copy(mealType = value.normalizedMealType()), message = null) }
    }

    fun onTemplateDraftItemQuantityChanged(index: Int, value: String) {
        mutableState.update { currentState ->
            val editor = currentState.mealTemplateEditor
            if (editor == null || index !in editor.items.indices) {
                currentState
            } else {
                currentState.copy(
                    mealTemplateEditor = editor.copy(
                        items = editor.items.mapIndexed { itemIndex, item ->
                            if (itemIndex == index) {
                                item.copy(quantityGrams = value.sanitizeDecimalInput())
                            } else {
                                item
                            }
                        },
                    ),
                    message = null,
                )
            }
        }
    }

    fun removeTemplateDraftItem(index: Int) {
        mutableState.update { currentState ->
            val editor = currentState.mealTemplateEditor
            if (editor == null || index !in editor.items.indices) {
                currentState
            } else {
                currentState.copy(
                    mealTemplateEditor = editor.copy(
                        items = editor.items.filterIndexed { itemIndex, _ -> itemIndex != index },
                    ),
                    message = null,
                )
            }
        }
    }

    fun onTemplateItemFoodChanged(value: String) {
        val food = state.value.savedFoods.firstOrNull { it.id == value }
        mutableState.update {
            val editor = it.mealTemplateEditor ?: return@update it
            it.copy(
                mealTemplateEditor = editor.copy(
                    newItemFoodId = value,
                    newItemQuantityGrams = food?.defaultServingGrams?.formatInputNumber() ?: editor.newItemQuantityGrams,
                ),
                message = null,
            )
        }
    }

    fun onTemplateNewItemQuantityChanged(value: String) {
        mutableState.update { it.copy(mealTemplateEditor = it.mealTemplateEditor?.copy(newItemQuantityGrams = value.sanitizeDecimalInput()), message = null) }
    }

    fun addTemplateItem() {
        val currentState = state.value
        val editor = currentState.mealTemplateEditor ?: return
        val food = currentState.savedFoods.firstOrNull { it.id == editor.newItemFoodId }
        if (food == null) {
            mutableState.update { it.copy(message = "Choose a food") }
            return
        }
        val quantity = editor.newItemQuantityGrams.parsePositiveNumberOrNull()
        if (quantity == null) {
            mutableState.update { it.copy(message = "Enter item amount") }
            return
        }
        mutableState.update {
            val current = it.mealTemplateEditor ?: return@update it
            it.copy(
                mealTemplateEditor = current.copy(
                    items = current.items + MealTemplateItemDraftUiState(
                        foodId = food.id,
                        foodName = food.name,
                        quantityGrams = quantity.formatInputNumber(),
                    ),
                    newItemFoodId = "",
                    newItemQuantityGrams = "100",
                ),
                message = null,
            )
        }
    }

    fun saveMealTemplateEdits() {
        val currentState = state.value
        val editor = currentState.mealTemplateEditor
        if (editor == null) {
            mutableState.update { it.copy(message = "Choose a template") }
            return
        }
        val templateId = editor.id
        if (editor.name.isBlank()) {
            mutableState.update { it.copy(message = "Enter a template name") }
            return
        }
        val items =
            editor.items.mapNotNull { item ->
                item.quantityGrams.parsePositiveNumberOrNull()?.let { quantity ->
                    MealTemplateItemInput(
                        foodId = item.foodId,
                        quantityGrams = quantity,
                    )
                }
            }
        if (items.size != editor.items.size || items.isEmpty()) {
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
                        name = editor.name,
                        mealType = editor.mealType,
                        items = items,
                    ),
                )
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        mealTemplateEditor = null,
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
                        mealTemplateEditor = null,
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
                savedFoodEditor = SavedFoodEditorState(),
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
                savedFoodEditor = SavedFoodEditorState(
                    name = "Scanned label",
                    caloriesPer100g = "250",
                    proteinPer100g = "12",
                    carbsPer100g = "30",
                    fatPer100g = "8",
                    servingName = "Label serving",
                    category = "Nutrition label",
                ),
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
                savedFoodEditor = SavedFoodEditorState(
                    id = savedFood.id,
                    name = savedFood.name,
                    brand = savedFood.brand.orEmpty(),
                    servingGrams = savedFood.defaultServingGrams.formatInputNumber(),
                    caloriesPer100g = savedFood.caloriesPer100g.formatInputNumber(),
                    proteinPer100g = savedFood.proteinPer100g.formatInputNumber(),
                    carbsPer100g = savedFood.carbsPer100g.formatInputNumber(),
                    fatPer100g = savedFood.fatPer100g.formatInputNumber(),
                    fiberPer100g = savedFood.fiberPer100g.formatInputNumber(),
                    sugarPer100g = savedFood.sugarPer100g.formatInputNumber(),
                    saturatedFatPer100g = savedFood.saturatedFatPer100g.formatInputNumber(),
                    sodiumMgPer100g = savedFood.sodiumMgPer100g.formatInputNumber(),
                    potassiumMgPer100g = savedFood.potassiumMgPer100g.formatInputNumber(),
                    calciumMgPer100g = savedFood.calciumMgPer100g.formatInputNumber(),
                    ironMgPer100g = savedFood.ironMgPer100g.formatInputNumber(),
                    vitaminDMcgPer100g = savedFood.vitaminDMcgPer100g.formatInputNumber(),
                    vitaminCMgPer100g = savedFood.vitaminCMgPer100g.formatInputNumber(),
                    magnesiumMgPer100g = savedFood.magnesiumMgPer100g.formatInputNumber(),
                    servingName = savedFood.servingName.orEmpty(),
                    barcode = savedFood.barcode.orEmpty(),
                    category = savedFood.category.orEmpty(),
                    isFavorite = savedFood.isFavorite,
                ),
                selectedSavedFoodDetail = null,
            )
        }
    }

    fun reportSavedFoodForReview(foodId: String) {
        val currentState = state.value
        if (currentState.savedFoods.none { it.id == foodId }) {
            mutableState.update { it.copy(message = "Saved food not found") }
            return
        }
        mutableState.update {
            it.withReportedSavedFoodIds(it.reportedSavedFoodIds + foodId)
                .copy(message = "Marked food for local review")
        }
    }

    fun startSavedFoodCorrection(foodId: String) {
        openSavedFoodEditor(foodId)
        mutableState.update { it.copy(message = "Review and correct nutrition before saving.") }
    }

    fun onSavedFoodNameChanged(value: String) {
        mutableState.update { it.copy(savedFoodEditor = it.savedFoodEditor?.copy(name = value), message = null) }
    }

    fun onSavedFoodBrandChanged(value: String) {
        mutableState.update { it.copy(savedFoodEditor = it.savedFoodEditor?.copy(brand = value), message = null) }
    }

    fun onSavedFoodServingChanged(value: String) {
        mutableState.update { it.copy(savedFoodEditor = it.savedFoodEditor?.copy(servingGrams = value.sanitizeDecimalInput()), message = null) }
    }

    fun onSavedFoodCaloriesChanged(value: String) {
        mutableState.update { it.copy(savedFoodEditor = it.savedFoodEditor?.copy(caloriesPer100g = value.sanitizeDecimalInput()), message = null) }
    }

    fun onSavedFoodProteinChanged(value: String) {
        mutableState.update { it.copy(savedFoodEditor = it.savedFoodEditor?.copy(proteinPer100g = value.sanitizeDecimalInput()), message = null) }
    }

    fun onSavedFoodCarbsChanged(value: String) {
        mutableState.update { it.copy(savedFoodEditor = it.savedFoodEditor?.copy(carbsPer100g = value.sanitizeDecimalInput()), message = null) }
    }

    fun onSavedFoodFatChanged(value: String) {
        mutableState.update { it.copy(savedFoodEditor = it.savedFoodEditor?.copy(fatPer100g = value.sanitizeDecimalInput()), message = null) }
    }

    fun onSavedFoodFiberChanged(value: String) {
        mutableState.update { it.copy(savedFoodEditor = it.savedFoodEditor?.copy(fiberPer100g = value.sanitizeDecimalInput()), message = null) }
    }

    fun onSavedFoodSugarChanged(value: String) {
        mutableState.update { it.copy(savedFoodEditor = it.savedFoodEditor?.copy(sugarPer100g = value.sanitizeDecimalInput()), message = null) }
    }

    fun onSavedFoodSaturatedFatChanged(value: String) {
        mutableState.update { it.copy(savedFoodEditor = it.savedFoodEditor?.copy(saturatedFatPer100g = value.sanitizeDecimalInput()), message = null) }
    }

    fun onSavedFoodSodiumChanged(value: String) {
        mutableState.update { it.copy(savedFoodEditor = it.savedFoodEditor?.copy(sodiumMgPer100g = value.sanitizeDecimalInput()), message = null) }
    }

    fun onSavedFoodPotassiumChanged(value: String) {
        mutableState.update { it.copy(savedFoodEditor = it.savedFoodEditor?.copy(potassiumMgPer100g = value.sanitizeDecimalInput()), message = null) }
    }

    fun onSavedFoodCalciumChanged(value: String) {
        mutableState.update { it.copy(savedFoodEditor = it.savedFoodEditor?.copy(calciumMgPer100g = value.sanitizeDecimalInput()), message = null) }
    }

    fun onSavedFoodIronChanged(value: String) {
        mutableState.update { it.copy(savedFoodEditor = it.savedFoodEditor?.copy(ironMgPer100g = value.sanitizeDecimalInput()), message = null) }
    }

    fun onSavedFoodVitaminDChanged(value: String) {
        mutableState.update { it.copy(savedFoodEditor = it.savedFoodEditor?.copy(vitaminDMcgPer100g = value.sanitizeDecimalInput()), message = null) }
    }

    fun onSavedFoodVitaminCChanged(value: String) {
        mutableState.update { it.copy(savedFoodEditor = it.savedFoodEditor?.copy(vitaminCMgPer100g = value.sanitizeDecimalInput()), message = null) }
    }

    fun onSavedFoodMagnesiumChanged(value: String) {
        mutableState.update { it.copy(savedFoodEditor = it.savedFoodEditor?.copy(magnesiumMgPer100g = value.sanitizeDecimalInput()), message = null) }
    }

    fun onSavedFoodServingNameChanged(value: String) {
        mutableState.update { it.copy(savedFoodEditor = it.savedFoodEditor?.copy(servingName = value), message = null) }
    }

    fun onSavedFoodBarcodeChanged(value: String) {
        mutableState.update { it.copy(savedFoodEditor = it.savedFoodEditor?.copy(barcode = value.filter { char -> char.isDigit() }), message = null) }
    }

    fun onSavedFoodCategoryChanged(value: String) {
        mutableState.update { it.copy(savedFoodEditor = it.savedFoodEditor?.copy(category = value), message = null) }
    }

    fun onSavedFoodFavoriteChanged(value: Boolean) {
        mutableState.update { it.copy(savedFoodEditor = it.savedFoodEditor?.copy(isFavorite = value), message = null) }
    }

    fun saveSavedFood() {
        val currentState = state.value
        if (currentState.isSaving) {
            return
        }
        val editor = currentState.savedFoodEditor ?: return
        val foodName = editor.name.trim()
        if (foodName.isBlank()) {
            mutableState.update { it.copy(message = "Enter a food name") }
            return
        }
        val servingGrams = editor.servingGrams.parsePositiveNumberOrNull()
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
                        foodId = editor.id,
                        name = foodName,
                        brand = editor.brand.ifBlank { null },
                        defaultServingGrams = servingGrams,
                        nutritionPer100g = nutrition,
                        nutritionDetailsPer100g = nutritionDetails,
                        servingName = editor.servingName.ifBlank { null },
                        barcode = editor.barcode.ifBlank { null },
                        category = editor.category.ifBlank { null },
                        isFavorite = editor.isFavorite,
                        servings = currentState.savedFoodServingInputsForUpsert(servingGrams),
                    ),
                )
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        isAddPanelVisible = true,
                        sheetMode = FoodSheetMode.FoodDatabase,
                        savedFoodEditor = null,
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
        val foodId = currentState.savedFoodEditor?.id
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
                        savedFoodEditor = null,
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
        val foodId = currentState.savedFoodEditor?.id
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
                        savedFoodEditor = null,
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
                fiberPer100g = parsed.fiberGrams?.formatInputNumber() ?: it.fiberPer100g,
                sugarPer100g = parsed.sugarGrams?.formatInputNumber() ?: it.sugarPer100g,
                saturatedFatPer100g = parsed.saturatedFatGrams?.formatInputNumber() ?: it.saturatedFatPer100g,
                sodiumMgPer100g = parsed.sodiumMilligrams?.formatInputNumber() ?: it.sodiumMgPer100g,
                nutritionLabelScanReview = NutritionLabelScanReviewUiState(
                    confidenceLabel = parsed.confidenceLabel,
                    parsedFieldCount = parsed.parsedFieldCount,
                ),
                message = if (parsed.hasAnyValue) {
                    "${parsed.confidenceLabel}. Review the scanned values below."
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
        // Keep the same-as-yesterday suggestions in sync with the new target
        // meal; the snapshot from openAddFood belongs to the previous meal.
        viewModelScope.launch {
            val items = repository.observeSameAsYesterday(value, state.value.selectedDate).first()
            mutableState.update { it.copy(sameAsYesterday = items.map { food -> food.toUiState() }) }
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
                                fiberPer100g = result.nutritionDetailsPer100g.fiberGrams.formatInputNumber(),
                                sugarPer100g = result.nutritionDetailsPer100g.sugarGrams.formatInputNumber(),
                                saturatedFatPer100g = result.nutritionDetailsPer100g.saturatedFatGrams.formatInputNumber(),
                                sodiumMgPer100g = result.nutritionDetailsPer100g.sodiumMilligrams.formatInputNumber(),
                                quantityGrams = (
                                    result.servingQuantityGrams
                                        ?.takeIf { it.isFinite() && it > 0.0 }
                                        ?: 100.0
                                    ).formatInputNumber(),
                                amountServingChoices = result.toAmountServingChoices(),
                                lookupResult = result,
                                nutritionLabelScanReview = null,
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
        val editedNutritionDetails = currentState.toEditedNutritionDetailsOrNull()
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
                            nutritionDetailsPer100g = editedNutritionDetails,
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
        val editedNutritionDetails = currentState.toEditedNutritionDetailsOrNull()
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
                        nutritionDetailsPer100g = currentState.lookupResult?.nutritionDetailsPer100g ?: editedNutritionDetails,
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
        if (currentState.isPlanningMode && !currentState.selectedDate.isWithinFoodPlanningHorizon()) {
            mutableState.update { it.copy(message = FOOD_PLANNING_LIMIT_MESSAGE) }
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

    fun logSameAsYesterday() {
        val currentState = state.value
        if (currentState.isSaving) {
            return
        }
        if (currentState.isPlanningMode && !currentState.selectedDate.isWithinFoodPlanningHorizon()) {
            mutableState.update { it.copy(message = FOOD_PLANNING_LIMIT_MESSAGE) }
            return
        }
        val items = currentState.sameAsYesterday
        if (items.isEmpty()) {
            return
        }
        if (!markSaving()) {
            return
        }

        viewModelScope.launch {
            try {
                items.forEach { food ->
                    val input = SavedFoodLogInput(
                        foodId = food.id,
                        mealType = currentState.mealType,
                        quantityGrams = food.defaultServingGrams,
                        date = currentState.selectedDate,
                    )
                    if (currentState.isPlanningMode) {
                        repository.planSavedFood(input)
                    } else {
                        repository.logSavedFood(input)
                    }
                }
                val noun = if (items.size == 1) "food" else "${items.size} foods"
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        isAddPanelVisible = currentState.keepAddingFoods,
                        message = if (currentState.isPlanningMode) "Planned $noun" else "Logged $noun",
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        message = error.message ?: "Failed to log foods",
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
                        useNetCarbsInput = preset.useNetCarbs,
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

    fun applyFoodProgram(programId: String) {
        val program = foodProgramDefinitions.firstOrNull { it.id == programId }
        val preset = program?.mode?.goalPreset()
        if (program == null || preset == null) {
            mutableState.update { it.copy(message = "Food program unavailable") }
            return
        }
        val currentState = state.value
        val includeTrainingCalories =
            if (currentState.sheetMode == FoodSheetMode.GoalEditor) {
                currentState.goalEditor.includeTrainingInput
            } else {
                currentState.includeTrainingCalories
            }
        val goal =
            FoodGoal(
                dailyCaloriesKcal = preset.dailyCaloriesKcal,
                proteinGrams = preset.proteinGrams,
                carbsGrams = preset.carbsGrams,
                fatGrams = preset.fatGrams,
                fiberGrams = preset.fiberGrams,
                sugarGrams = preset.sugarGrams,
                saturatedFatGrams = preset.saturatedFatGrams,
                sodiumMilligrams = preset.sodiumMilligrams,
                mode = program.mode,
                includeTrainingCalories = includeTrainingCalories,
                useNetCarbs = preset.useNetCarbs,
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
                            message = "Applied ${program.title} program",
                        )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        message = error.message ?: "Failed to apply food program",
                    )
                }
            }
        }
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
        mutableState.update { it.copy(recipeEditor = it.recipeEditor?.copy(name = value), message = null) }
    }

    fun onRecipeCategoryChanged(value: String) {
        mutableState.update { it.copy(recipeEditor = it.recipeEditor?.copy(category = value), message = null) }
    }

    fun onRecipeServingNameChanged(value: String) {
        mutableState.update { it.copy(recipeEditor = it.recipeEditor?.copy(servingName = value), message = null) }
    }

    fun onRecipeServingGramsChanged(value: String) {
        val sanitized = value.sanitizeDecimalInput()
        mutableState.update {
            val editor = it.recipeEditor ?: return@update it
            it.copy(
                recipeEditor = editor.copy(
                    servingGrams = sanitized,
                    cookedYieldGrams = if (editor.servingsCount.parsePositiveNumberOrNull() == 1.0) {
                        sanitized
                    } else {
                        editor.cookedYieldGrams
                    },
                ),
                message = null,
            )
        }
    }

    fun onRecipeServingsCountChanged(value: String) {
        val sanitized = value.sanitizeDecimalInput()
        mutableState.update {
            val editor = it.recipeEditor ?: return@update it
            val cookedYield = editor.cookedYieldGrams.parsePositiveNumberOrNull()
            val servings = sanitized.parsePositiveNumberOrNull()
            it.copy(
                recipeEditor = editor.copy(
                    servingsCount = sanitized,
                    servingGrams = if (cookedYield != null && servings != null) {
                        (cookedYield / servings).formatInputNumber()
                    } else {
                        editor.servingGrams
                    },
                ),
                message = null,
            )
        }
    }

    fun onRecipeCookedYieldGramsChanged(value: String) {
        val sanitized = value.sanitizeDecimalInput()
        mutableState.update {
            val editor = it.recipeEditor ?: return@update it
            val cookedYield = sanitized.parsePositiveNumberOrNull()
            val servings = editor.servingsCount.parsePositiveNumberOrNull()
            it.copy(
                recipeEditor = editor.copy(
                    cookedYieldGrams = sanitized,
                    servingGrams = if (cookedYield != null && servings != null) {
                        (cookedYield / servings).formatInputNumber()
                    } else {
                        editor.servingGrams
                    },
                ),
                message = null,
            )
        }
    }

    fun onRecipeIngredientFoodChanged(value: String) {
        val food = state.value.savedFoods.firstOrNull { it.id == value }
        val choices = food?.toRecipeIngredientServingChoices().orEmpty()
        mutableState.update {
            it.copy(
                recipeEditor = it.recipeEditor?.copy(
                    ingredientFoodId = value,
                    ingredientServingChoiceId = choices.firstOrNull()?.id ?: "g",
                    ingredientServingChoices = choices,
                ),
                message = null,
            )
        }
    }

    fun onRecipeIngredientServingChoiceSelected(choiceId: String) {
        val choice = state.value.recipeEditor?.ingredientServingChoices?.firstOrNull { it.id == choiceId } ?: return
        mutableState.update {
            val editor = it.recipeEditor ?: return@update it
            it.copy(
                recipeEditor = editor.copy(
                    ingredientServingChoiceId = choice.id,
                    ingredientQuantityGrams = if (choice.id == "g") editor.ingredientQuantityGrams else "1",
                ),
                message = null,
            )
        }
    }

    fun onRecipeIngredientQuantityChanged(value: String) {
        mutableState.update { it.copy(recipeEditor = it.recipeEditor?.copy(ingredientQuantityGrams = value.sanitizeDecimalInput()), message = null) }
    }

    fun onRecipeServingsToLogChanged(value: String) {
        mutableState.update { it.copy(recipeServingsToLog = value.sanitizeDecimalInput(), message = null) }
    }

    fun addRecipeIngredient() {
        val currentState = state.value
        val editor = currentState.recipeEditor ?: return
        val food = currentState.savedFoods.firstOrNull { it.id == editor.ingredientFoodId }
        if (food == null) {
            mutableState.update { it.copy(message = "Choose an ingredient") }
            return
        }
        val quantity = editor.ingredientQuantityGrams.parsePositiveNumberOrNull()
        if (quantity == null) {
            mutableState.update { it.copy(message = "Enter ingredient amount") }
            return
        }
        val servingChoice =
            editor.ingredientServingChoices.firstOrNull { it.id == editor.ingredientServingChoiceId }
                ?: RecipeIngredientServingChoiceUiState(id = "g", label = "g", grams = 1.0)
        val quantityGrams = quantity * servingChoice.grams
        mutableState.update {
            val current = it.recipeEditor ?: return@update it
            it.copy(
                recipeEditor = current.copy(
                    ingredients = current.ingredients + RecipeIngredientDraftUiState(
                        foodId = food.id,
                        foodName = food.name,
                        quantityGrams = quantityGrams,
                        unitLabel = servingChoice.label,
                        unitGrams = servingChoice.grams,
                        unitQuantity = quantity,
                    ),
                    ingredientFoodId = "",
                    ingredientServingChoiceId = "g",
                    ingredientServingChoices = emptyList(),
                    ingredientQuantityGrams = "100",
                ),
                message = null,
            )
        }
    }

    fun saveRecipe() {
        val currentState = state.value
        val editor = currentState.recipeEditor ?: return
        val servings = editor.servingsCount.parsePositiveNumberOrNull()
        val cookedYieldGrams = editor.cookedYieldGrams.parsePositiveNumberOrNull()
        val servingGrams = if (servings != null && cookedYieldGrams != null) {
            cookedYieldGrams / servings
        } else {
            editor.servingGrams.parsePositiveNumberOrNull()
        }
        if (
            editor.name.isBlank() ||
            servingGrams == null ||
            servings == null ||
            cookedYieldGrams == null ||
            editor.ingredients.isEmpty()
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
                        recipeId = editor.editingRecipeId,
                        name = editor.name,
                        category = editor.category.ifBlank { null },
                        servingName = editor.servingName.ifBlank { "Serving" },
                        servingGrams = servingGrams,
                        servings = servings,
                        cookedYieldGrams = cookedYieldGrams,
                        ingredients = editor.ingredients.map { ingredient ->
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
                        recipeEditor = null,
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
                        recipeEditor = null,
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

    fun logRecipeFromBrowser(recipeId: String) {
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
                    mealType = currentState.recipeBrowserMealType,
                    servings = servings,
                    date = currentState.recipeBrowserDate,
                )
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        isAddPanelVisible = true,
                        sheetMode = FoodSheetMode.RecipeBrowser,
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

    fun planRecipe(recipeId: String) {
        val currentState = state.value
        val servings = currentState.recipeServingsToLog.parsePositiveNumberOrNull()
        if (servings == null) {
            mutableState.update { it.copy(message = "Enter recipe servings") }
            return
        }
        if (!currentState.recipeBrowserDate.isWithinFoodPlanningHorizon()) {
            mutableState.update { it.copy(message = FOOD_PLANNING_LIMIT_MESSAGE) }
            return
        }
        if (!markSaving()) {
            return
        }
        viewModelScope.launch {
            try {
                repository.planRecipe(
                    recipeId = recipeId,
                    mealType = currentState.recipeBrowserMealType,
                    servings = servings,
                    date = currentState.recipeBrowserDate,
                )
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        isAddPanelVisible = true,
                        sheetMode = FoodSheetMode.RecipeBrowser,
                        message = "Planned recipe",
                    )
                }
            } catch (error: CancellationException) {
                mutableState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        message = error.message ?: "Failed to plan recipe",
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

    private fun FoodUiState.toEditedNutritionDetailsOrNull(): NutritionDetails = NutritionDetails(
        fiberGrams = fiberPer100g.parseNonNegativeNumberOrZero() ?: 0.0,
        sugarGrams = sugarPer100g.parseNonNegativeNumberOrZero() ?: 0.0,
        saturatedFatGrams = saturatedFatPer100g.parseNonNegativeNumberOrZero() ?: 0.0,
        sodiumMilligrams = sodiumMgPer100g.parseNonNegativeNumberOrZero() ?: 0.0,
    )

    private fun FoodUiState.toSavedFoodNutritionOrNull(): FoodNutrition? {
        val editor = savedFoodEditor ?: return null
        val calories = editor.caloriesPer100g.parseNutritionValue() ?: return null
        val protein = editor.proteinPer100g.parseNutritionValue() ?: return null
        val carbs = editor.carbsPer100g.parseNutritionValue() ?: return null
        val fat = editor.fatPer100g.parseNutritionValue() ?: return null

        return FoodNutrition(
            caloriesKcal = calories,
            proteinGrams = protein,
            carbsGrams = carbs,
            fatGrams = fat,
        )
    }

    private fun FoodUiState.toSavedFoodNutritionDetailsOrNull(): NutritionDetails? {
        val editor = savedFoodEditor ?: return null
        val fiber = editor.fiberPer100g.parseNonNegativeNumberOrZero() ?: return null
        val sugar = editor.sugarPer100g.parseNonNegativeNumberOrZero() ?: return null
        val saturatedFat = editor.saturatedFatPer100g.parseNonNegativeNumberOrZero() ?: return null
        val sodium = editor.sodiumMgPer100g.parseNonNegativeNumberOrZero() ?: return null
        val potassium = editor.potassiumMgPer100g.parseNonNegativeNumberOrZero() ?: return null
        val calcium = editor.calciumMgPer100g.parseNonNegativeNumberOrZero() ?: return null
        val iron = editor.ironMgPer100g.parseNonNegativeNumberOrZero() ?: return null
        val vitaminD = editor.vitaminDMcgPer100g.parseNonNegativeNumberOrZero() ?: return null
        val vitaminC = editor.vitaminCMgPer100g.parseNonNegativeNumberOrZero() ?: return null
        val magnesium = editor.magnesiumMgPer100g.parseNonNegativeNumberOrZero() ?: return null

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
        val editor = savedFoodEditor ?: return emptyList()
        val existingFood =
            editor.id?.let { foodId -> savedFoods.firstOrNull { savedFood -> savedFood.id == foodId } }
                ?: return emptyList()
        if (existingFood.servings.isEmpty()) {
            return emptyList()
        }

        val currentServingLabel =
            editor.servingName.ifBlank {
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
        caloriesPer100g: String = "250",
        proteinPer100g: String = "12",
        carbsPer100g: String = "30",
        fatPer100g: String = "8",
        review: String? = "Local estimate: generic meal",
        message: String,
    ): FoodUiState = copy(
        isAddPanelVisible = true,
        sheetMode = FoodSheetMode.AddFood,
        addMode = FoodAddMode.Ai,
        productName = name,
        brand = "",
        quantityGrams = "100",
        caloriesPer100g = caloriesPer100g,
        proteinPer100g = proteinPer100g,
        carbsPer100g = carbsPer100g,
        fatPer100g = fatPer100g,
        fiberPer100g = "",
        sugarPer100g = "",
        saturatedFatPer100g = "",
        sodiumMgPer100g = "",
        amountServingChoices = defaultAmountServingChoices(),
        lookupResult = null,
        aiLoggingHasDraft = true,
        aiLoggingDraftSourceLabel = sourceLabel,
        aiLoggingDraftReview = review,
        nutritionLabelScanReview = null,
        message = message,
    ).withAmountNutritionPreview()

    private fun FoodUiState.clearedEditableFields(): FoodUiState = copy(
        productName = "",
        brand = "",
        caloriesPer100g = "",
        proteinPer100g = "",
        carbsPer100g = "",
        fatPer100g = "",
        fiberPer100g = "",
        sugarPer100g = "",
        saturatedFatPer100g = "",
        sodiumMgPer100g = "",
        amountNutritionPreview = null,
        amountServingChoices = emptyList(),
        lookupResult = null,
        aiLoggingHasDraft = false,
        aiLoggingDraftSourceLabel = null,
        aiLoggingDraftReview = null,
        nutritionLabelScanReview = null,
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
    val useNetCarbs: Boolean = false,
)

private data class FoodProgramDefinition(
    val id: String,
    val mode: FoodGoalMode,
    val title: String,
    val subtitle: String,
    val description: String,
    val suggestedHabits: List<String>,
    val mealPlanningTip: String,
)

private data class RecipeCatalogItem(
    val id: String,
    val title: String,
    val category: String,
    val mealTypeIds: List<String>,
    val thumbnailKey: String,
    val servingName: String,
    val servingGrams: Double,
    val caloriesKcal: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val tags: List<String>,
    val relevantModes: Set<FoodGoalMode>,
)

private data class FastingProgramDefinition(
    val id: String,
    val title: String,
    val fastingHours: Double,
    val eatingHours: Double,
    val description: String,
)

private data class LocalAiNutritionDraft(
    val caloriesKcal: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val review: String,
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
internal const val CARBS_GOAL_GRAMS = 260.0
internal const val PROTEIN_GOAL_GRAMS = 104.0
internal const val FAT_GOAL_GRAMS = 69.0
internal const val FIBER_GOAL_GRAMS = 30.0
internal const val SUGAR_GOAL_GRAMS = 50.0
internal const val SATURATED_FAT_GOAL_GRAMS = 20.0
internal const val SODIUM_GOAL_MILLIGRAMS = 2300.0
private const val WATER_GOAL_MILLILITERS = 2000.0
private const val TRANSIENT_MESSAGE_DISMISS_MILLIS = 3_000L
private const val FOOD_PLANNING_LIMIT_DAYS = 7L
private const val FOOD_PLANNING_LIMIT_MESSAGE = "You can plan up to 1 week ahead."

private fun foodPlanningMaxDate(): LocalDate = LocalDate.now().plusDays(FOOD_PLANNING_LIMIT_DAYS)

private fun LocalDate.isWithinFoodPlanningHorizon(): Boolean = !isAfter(foodPlanningMaxDate())

private fun LocalDate.coerceInFoodPlanningHorizon(): LocalDate = if (isWithinFoodPlanningHorizon()) this else foodPlanningMaxDate()

private val foodProgramDefinitions =
    listOf(
        FoodProgramDefinition(
            id = "balanced",
            mode = FoodGoalMode.Balanced,
            title = "Balanced",
            subtitle = "Steady everyday nutrition",
            description = "A practical calorie and macro split for general tracking.",
            suggestedHabits = listOf("Water daily", "Fruit or vegetables at two meals", "Protein with each main meal"),
            mealPlanningTip = "Plan simple repeatable meals, then adjust portions by hunger and progress.",
        ),
        FoodProgramDefinition(
            id = "high-protein",
            mode = FoodGoalMode.HighProtein,
            title = "High protein",
            subtitle = "Protein-forward meals",
            description = "Raises protein while keeping carbs and fats moderate.",
            suggestedHabits = listOf("Lean protein each meal", "High-protein snack", "Fiber side with protein meals"),
            mealPlanningTip = "Build templates around eggs, yogurt, fish, meat, tofu, or legumes.",
        ),
        FoodProgramDefinition(
            id = "muscle-gain",
            mode = FoodGoalMode.MuscleGain,
            title = "Muscle gain",
            subtitle = "More energy with protein",
            description = "Adds calories and carbs while keeping protein high.",
            suggestedHabits = listOf("Protein every 3-5 hours", "Carbs around training", "Consistent water"),
            mealPlanningTip = "Plan larger lunches and dinners so calories do not pile up late.",
        ),
        FoodProgramDefinition(
            id = "weight-loss",
            mode = FoodGoalMode.WeightLoss,
            title = "Weight loss",
            subtitle = "Controlled energy, high satiety",
            description = "Moderates calories while protecting protein and fiber.",
            suggestedHabits = listOf("Protein first", "Vegetables daily", "Lower-sugar snacks"),
            mealPlanningTip = "Pre-plan snacks and dinner portions before the day gets busy.",
        ),
        FoodProgramDefinition(
            id = "keto-low-carb",
            mode = FoodGoalMode.KetoLowCarb,
            title = "Keto low carb",
            subtitle = "Lower carbs with net-carb tracking",
            description = "Keeps carbs low and uses fats for more energy.",
            suggestedHabits = listOf("Track net carbs", "Electrolyte-aware hydration", "Non-starchy vegetables"),
            mealPlanningTip = "Plan protein plus low-carb vegetables before adding fats.",
        ),
        FoodProgramDefinition(
            id = "mediterranean-style",
            mode = FoodGoalMode.MediterraneanStyle,
            title = "Mediterranean-style",
            subtitle = "Fiber, fish, olive oil, legumes",
            description = "Emphasizes higher fiber carbs, unsaturated fats, and fish or legumes.",
            suggestedHabits = listOf("Olive oil or nuts", "Fish weekly", "Beans, grains, fruit, and vegetables"),
            mealPlanningTip = "Plan bowls, salads, fish dinners, and legume-based lunches.",
        ),
        FoodProgramDefinition(
            id = "clean-eating",
            mode = FoodGoalMode.CleanEating,
            title = "Clean eating",
            subtitle = "Simple whole-food structure",
            description = "Focuses on protein, fiber, lower sugar, and less sodium.",
            suggestedHabits = listOf("Whole-food protein", "Vegetables twice daily", "Limit sugary packaged snacks"),
            mealPlanningTip = "Prep a protein, a grain or potato, and chopped vegetables for fast meals.",
        ),
    )

private val recipeCatalogItems =
    listOf(
        RecipeCatalogItem(
            id = "catalog-high-protein-chicken-bowl",
            title = "High-protein chicken bowl",
            category = "Lunch",
            mealTypeIds = listOf("lunch", "dinner"),
            thumbnailKey = "chicken-bowl",
            servingName = "Bowl",
            servingGrams = 420.0,
            caloriesKcal = 520.0,
            proteinGrams = 48.0,
            carbsGrams = 48.0,
            fatGrams = 14.0,
            tags = listOf("High protein", "Quick"),
            relevantModes = setOf(FoodGoalMode.Balanced, FoodGoalMode.HighProtein, FoodGoalMode.MuscleGain),
        ),
        RecipeCatalogItem(
            id = "catalog-keto-salmon-plate",
            title = "Low-carb salmon plate",
            category = "Dinner",
            mealTypeIds = listOf("dinner"),
            thumbnailKey = "salmon-plate",
            servingName = "Plate",
            servingGrams = 360.0,
            caloriesKcal = 610.0,
            proteinGrams = 42.0,
            carbsGrams = 16.0,
            fatGrams = 42.0,
            tags = listOf("High protein", "Low carb"),
            relevantModes = setOf(FoodGoalMode.KetoLowCarb, FoodGoalMode.HighProtein, FoodGoalMode.MediterraneanStyle),
        ),
        RecipeCatalogItem(
            id = "catalog-mediterranean-chickpea-bowl",
            title = "Mediterranean chickpea bowl",
            category = "Vegetarian",
            mealTypeIds = listOf("lunch", "dinner"),
            thumbnailKey = "chickpea-bowl",
            servingName = "Bowl",
            servingGrams = 380.0,
            caloriesKcal = 470.0,
            proteinGrams = 24.0,
            carbsGrams = 62.0,
            fatGrams = 15.0,
            tags = listOf("Vegetarian", "Quick"),
            relevantModes = setOf(FoodGoalMode.Balanced, FoodGoalMode.MediterraneanStyle, FoodGoalMode.CleanEating, FoodGoalMode.WeightLoss),
        ),
        RecipeCatalogItem(
            id = "catalog-clean-breakfast-bowl",
            title = "Clean breakfast bowl",
            category = "Breakfast",
            mealTypeIds = listOf("breakfast"),
            thumbnailKey = "breakfast-bowl",
            servingName = "Bowl",
            servingGrams = 330.0,
            caloriesKcal = 430.0,
            proteinGrams = 32.0,
            carbsGrams = 46.0,
            fatGrams = 12.0,
            tags = listOf("High protein", "Vegetarian", "Quick"),
            relevantModes = setOf(FoodGoalMode.Balanced, FoodGoalMode.HighProtein, FoodGoalMode.CleanEating, FoodGoalMode.WeightLoss),
        ),
        RecipeCatalogItem(
            id = "catalog-overnight-oats",
            title = "Silken tofu overnight oats",
            category = "Breakfast",
            mealTypeIds = listOf("breakfast"),
            thumbnailKey = "overnight-oats",
            servingName = "Jar",
            servingGrams = 340.0,
            caloriesKcal = 495.0,
            proteinGrams = 31.0,
            carbsGrams = 58.0,
            fatGrams = 14.0,
            tags = listOf("High protein", "Vegetarian", "Quick"),
            relevantModes = setOf(FoodGoalMode.Balanced, FoodGoalMode.HighProtein, FoodGoalMode.CleanEating),
        ),
        RecipeCatalogItem(
            id = "catalog-sweet-potato-muffins",
            title = "Sweet potato protein muffins",
            category = "Breakfast",
            mealTypeIds = listOf("breakfast", "snacks"),
            thumbnailKey = "muffins",
            servingName = "Plate",
            servingGrams = 180.0,
            caloriesKcal = 280.0,
            proteinGrams = 18.0,
            carbsGrams = 38.0,
            fatGrams = 7.0,
            tags = listOf("Vegetarian", "Quick"),
            relevantModes = setOf(FoodGoalMode.Balanced, FoodGoalMode.WeightLoss, FoodGoalMode.CleanEating),
        ),
        RecipeCatalogItem(
            id = "catalog-apple-kale-salad",
            title = "Apple, walnut, and kale salad",
            category = "Lunch",
            mealTypeIds = listOf("lunch"),
            thumbnailKey = "kale-salad",
            servingName = "Bowl",
            servingGrams = 320.0,
            caloriesKcal = 343.0,
            proteinGrams = 14.0,
            carbsGrams = 34.0,
            fatGrams = 19.0,
            tags = listOf("Vegetarian", "Quick"),
            relevantModes = setOf(FoodGoalMode.Balanced, FoodGoalMode.MediterraneanStyle, FoodGoalMode.CleanEating, FoodGoalMode.WeightLoss),
        ),
        RecipeCatalogItem(
            id = "catalog-avocado-bean-dip",
            title = "Avocado and bean dip",
            category = "Lunch",
            mealTypeIds = listOf("lunch", "snacks"),
            thumbnailKey = "bean-dip",
            servingName = "Bowl",
            servingGrams = 250.0,
            caloriesKcal = 225.0,
            proteinGrams = 11.0,
            carbsGrams = 27.0,
            fatGrams = 9.0,
            tags = listOf("Vegetarian", "Quick"),
            relevantModes = setOf(FoodGoalMode.Balanced, FoodGoalMode.MediterraneanStyle, FoodGoalMode.CleanEating, FoodGoalMode.WeightLoss),
        ),
        RecipeCatalogItem(
            id = "catalog-tomato-lentil-soup",
            title = "Tomato lentil soup",
            category = "Dinner",
            mealTypeIds = listOf("dinner", "lunch"),
            thumbnailKey = "soup",
            servingName = "Bowl",
            servingGrams = 420.0,
            caloriesKcal = 390.0,
            proteinGrams = 24.0,
            carbsGrams = 52.0,
            fatGrams = 10.0,
            tags = listOf("Vegetarian"),
            relevantModes = setOf(FoodGoalMode.Balanced, FoodGoalMode.MediterraneanStyle, FoodGoalMode.CleanEating, FoodGoalMode.WeightLoss),
        ),
        RecipeCatalogItem(
            id = "catalog-protein-snack-box",
            title = "Protein snack box",
            category = "Snacks",
            mealTypeIds = listOf("snacks"),
            thumbnailKey = "snack-box",
            servingName = "Box",
            servingGrams = 220.0,
            caloriesKcal = 310.0,
            proteinGrams = 30.0,
            carbsGrams = 22.0,
            fatGrams = 12.0,
            tags = listOf("High protein", "Quick"),
            relevantModes = setOf(FoodGoalMode.Balanced, FoodGoalMode.HighProtein, FoodGoalMode.MuscleGain),
        ),
    )

private val fastingProgramDefinitions =
    listOf(
        FastingProgramDefinition(
            id = "12-12",
            title = "12:12",
            fastingHours = 12.0,
            eatingHours = 12.0,
            description = "A gentle overnight fast with an even eating window.",
        ),
        FastingProgramDefinition(
            id = "14-10",
            title = "14:10",
            fastingHours = 14.0,
            eatingHours = 10.0,
            description = "A moderate fast that still leaves room for three meals.",
        ),
        FastingProgramDefinition(
            id = "16-8",
            title = "16:8",
            fastingHours = 16.0,
            eatingHours = 8.0,
            description = "A focused daily fasting window with a compact eating window.",
        ),
        FastingProgramDefinition(
            id = "custom",
            title = "Custom 16:8",
            fastingHours = 16.0,
            eatingHours = 8.0,
            description = "Set a custom fasting and eating split that totals 24 hours.",
        ),
    )

private fun FoodGoalMode.goalPreset(): GoalPreset? = when (this) {
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
            useNetCarbs = true,
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

    FoodGoalMode.MediterraneanStyle ->
        GoalPreset(
            dailyCaloriesKcal = CALORIE_GOAL_KCAL,
            proteinGrams = 120.0,
            carbsGrams = 240.0,
            fatGrams = 77.0,
            fiberGrams = 38.0,
            sugarGrams = 45.0,
            saturatedFatGrams = 18.0,
            sodiumMilligrams = 2100.0,
        )

    FoodGoalMode.CleanEating ->
        GoalPreset(
            dailyCaloriesKcal = CALORIE_GOAL_KCAL,
            proteinGrams = 130.0,
            carbsGrams = 230.0,
            fatGrams = 70.0,
            fiberGrams = 40.0,
            sugarGrams = 35.0,
            saturatedFatGrams = 18.0,
            sodiumMilligrams = 2000.0,
        )

    FoodGoalMode.Custom -> null
}

private fun buildFoodProgramUiStates(selectedMode: FoodGoalMode): List<FoodProgramUiState> = foodProgramDefinitions.map { program ->
    val preset = requireNotNull(program.mode.goalPreset()) { "Food programs need goal presets" }
    FoodProgramUiState(
        id = program.id,
        mode = program.mode,
        title = program.title,
        subtitle = program.subtitle,
        description = program.description,
        macroTargetsLabel = listOf(
            "${preset.dailyCaloriesKcal.formatInputNumber()} kcal",
            "${preset.proteinGrams.formatInputNumber()} g protein",
            "${preset.carbsGrams.formatInputNumber()} g carbs",
            "${preset.fatGrams.formatInputNumber()} g fat",
        ).joinToString(" - "),
        suggestedHabits = program.suggestedHabits,
        mealPlanningTip = program.mealPlanningTip,
        isSelected = program.mode == selectedMode,
    )
}

private fun buildRecipeDiscoveryItems(
    recipes: List<RecipeUiState>,
    goalMode: FoodGoalMode,
): List<RecipeDiscoveryItemUiState> = recipes.map { recipe -> recipe.toDiscoveryItem(goalMode) } +
    recipeCatalogItems.map { item -> item.toDiscoveryItem(goalMode) }

private fun RecipeUiState.toDiscoveryItem(goalMode: FoodGoalMode): RecipeDiscoveryItemUiState {
    val searchableText = listOf(name, category.orEmpty(), itemSummary).joinToString(" ").lowercase()
    val tagLabels = buildRecipeTags(
        proteinGrams = proteinPerServingGrams,
        carbsGrams = carbsPerServingGrams,
        category = category.orEmpty(),
        searchableText = searchableText,
    )
    return RecipeDiscoveryItemUiState(
        id = "saved-$id",
        title = name,
        subtitle = listOfNotNull(
            category?.takeIf { it.isNotBlank() },
            itemSummary.takeIf { it.isNotBlank() },
        ).joinToString(" - ").ifBlank { "Saved recipe" },
        category = category.orEmpty(),
        servingName = servingName,
        servingGrams = servingGrams,
        caloriesKcal = caloriesPerServingKcal,
        proteinGrams = proteinPerServingGrams,
        carbsGrams = carbsPerServingGrams,
        fatGrams = fatPerServingGrams,
        tagLabels = tagLabels,
        isFavorite = isFavorite,
        isSavedRecipe = true,
        programRelevant = isRecipeRelevantForProgram(
            goalMode = goalMode,
            tagLabels = tagLabels,
            caloriesKcal = caloriesPerServingKcal,
            searchableText = searchableText,
        ),
        sourceRecipeId = id,
        mealTypeIds = recipeMealTypeIds(category.orEmpty(), searchableText),
        thumbnailKey = recipeThumbnailKey(searchableText),
    )
}

private fun RecipeCatalogItem.toDiscoveryItem(goalMode: FoodGoalMode): RecipeDiscoveryItemUiState {
    val tagLabels = tags.distinct()
    return RecipeDiscoveryItemUiState(
        id = id,
        title = title,
        subtitle = "Recipe idea - ${caloriesKcal.formatInputNumber()} kcal",
        category = category,
        servingName = servingName,
        servingGrams = servingGrams,
        caloriesKcal = caloriesKcal,
        proteinGrams = proteinGrams,
        carbsGrams = carbsGrams,
        fatGrams = fatGrams,
        tagLabels = tagLabels,
        isFavorite = false,
        isSavedRecipe = false,
        programRelevant = goalMode in relevantModes,
        mealTypeIds = mealTypeIds,
        thumbnailKey = thumbnailKey,
    )
}

private fun buildRecipeTags(
    proteinGrams: Double,
    carbsGrams: Double,
    category: String,
    searchableText: String,
): List<String> = buildList {
    if (proteinGrams >= 30.0) {
        add("High protein")
    }
    if (carbsGrams <= 25.0) {
        add("Low carb")
    }
    if (
        category.contains("vegetarian", ignoreCase = true) ||
        listOf("tofu", "tempeh", "chickpea", "lentil", "bean", "vegetarian").any { keyword -> keyword in searchableText }
    ) {
        add("Vegetarian")
    }
    if (listOf("quick", "smoothie", "overnight", "salad", "bowl").any { keyword -> keyword in searchableText }) {
        add("Quick")
    }
}

private fun isRecipeRelevantForProgram(
    goalMode: FoodGoalMode,
    tagLabels: List<String>,
    caloriesKcal: Double,
    searchableText: String,
): Boolean = when (goalMode) {
    FoodGoalMode.Balanced -> true

    FoodGoalMode.HighProtein,
    FoodGoalMode.MuscleGain,
    -> "High protein" in tagLabels

    FoodGoalMode.KetoLowCarb -> "Low carb" in tagLabels

    FoodGoalMode.WeightLoss -> caloriesKcal <= 500.0 && ("High protein" in tagLabels || "Vegetarian" in tagLabels)

    FoodGoalMode.MediterraneanStyle ->
        listOf("fish", "salmon", "chickpea", "bean", "lentil", "olive", "whole grain").any { keyword -> keyword in searchableText }

    FoodGoalMode.CleanEating -> caloriesKcal <= 550.0 && ("High protein" in tagLabels || "Vegetarian" in tagLabels)

    FoodGoalMode.Custom -> false
}

private fun recipeMealTypeIds(category: String, searchableText: String): List<String> {
    val normalizedCategory = category.lowercase()
    val matches = buildList {
        if ("breakfast" in normalizedCategory || listOf("breakfast", "oat", "muffin", "yogurt").any { it in searchableText }) {
            add("breakfast")
        }
        if ("lunch" in normalizedCategory || listOf("salad", "dip", "wrap", "lunch").any { it in searchableText }) {
            add("lunch")
        }
        if ("dinner" in normalizedCategory || listOf("dinner", "salmon", "chicken", "soup", "curry").any { it in searchableText }) {
            add("dinner")
        }
        if ("snack" in normalizedCategory || listOf("snack", "muffin", "box", "dip").any { it in searchableText }) {
            add("snacks")
        }
    }.distinct()
    return matches.ifEmpty { listOf("dinner") }
}

private fun recipeThumbnailKey(searchableText: String): String = when {
    listOf("salmon", "fish").any { it in searchableText } -> "salmon-plate"
    listOf("chicken", "turkey").any { it in searchableText } -> "chicken-bowl"
    listOf("oat", "yogurt", "breakfast").any { it in searchableText } -> "breakfast-bowl"
    listOf("muffin", "bites").any { it in searchableText } -> "muffins"
    listOf("salad", "kale").any { it in searchableText } -> "kale-salad"
    listOf("bean", "dip").any { it in searchableText } -> "bean-dip"
    listOf("soup", "lentil").any { it in searchableText } -> "soup"
    else -> "bowl"
}

private fun List<RecipeDiscoveryItemUiState>.filterForRecipeDiscovery(
    filter: RecipeDiscoveryFilter,
    query: String,
): List<RecipeDiscoveryItemUiState> {
    val filteredByChip =
        when (filter) {
            RecipeDiscoveryFilter.All -> this
            RecipeDiscoveryFilter.HighProtein -> filter { "High protein" in it.tagLabels }
            RecipeDiscoveryFilter.LowCarb -> filter { "Low carb" in it.tagLabels }
            RecipeDiscoveryFilter.Vegetarian -> filter { "Vegetarian" in it.tagLabels }
            RecipeDiscoveryFilter.Quick -> filter { "Quick" in it.tagLabels }
            RecipeDiscoveryFilter.Favorites -> filter { it.isFavorite }
            RecipeDiscoveryFilter.Program -> filter { it.programRelevant }
        }
    val terms = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (terms.isEmpty()) {
        return filteredByChip
    }
    return filteredByChip.filter { item ->
        val searchableText =
            listOf(
                item.title,
                item.subtitle,
                item.category,
                item.servingName,
                item.tagLabels.joinToString(" "),
                item.mealTypeIds.joinToString(" "),
            ).joinToString(" ").lowercase()
        terms.all { term -> term in searchableText }
    }
}

private fun buildFastingTimerUiState(
    selectedProgramId: String,
    fastingStartInput: String,
    customFastingHoursInput: String,
    customEatingHoursInput: String,
): FastingTimerUiState {
    val customFastingHours = customFastingHoursInput.parseNonNegativeNumberOrZero() ?: 16.0
    val customEatingHours = customEatingHoursInput.parseNonNegativeNumberOrZero() ?: 8.0
    val programs = emptyFastingPrograms(selectedProgramId, customFastingHours, customEatingHours)
    val selectedProgram = programs.firstOrNull { it.id == selectedProgramId } ?: programs.first { it.id == "16-8" }
    val startTime = fastingStartInput.toFastingStartTimeOrDefault()
    val fastingEndTime = startTime.plusMinutes((selectedProgram.fastingHours * 60.0).roundToInt().toLong())
    val startLabel = startTime.toHourMinuteLabel()
    val fastingEndLabel = fastingEndTime.toHourMinuteLabel()
    return FastingTimerUiState(
        selectedProgramId = selectedProgram.id,
        programs = programs,
        fastingStartInput = startLabel,
        fastingWindowLabel = "$startLabel - $fastingEndLabel",
        eatingWindowLabel = "$fastingEndLabel - $startLabel",
        statusLabel = "${selectedProgram.title} fasting plan active",
        progress = selectedProgram.fastingHours.fractionOf(24.0),
        customFastingHoursInput = customFastingHoursInput,
        customEatingHoursInput = customEatingHoursInput,
    )
}

private fun emptyFastingPrograms(
    selectedProgramId: String,
    customFastingHours: Double,
    customEatingHours: Double,
): List<FastingProgramUiState> = fastingProgramDefinitions.map { program ->
    val fastingHours = if (program.id == "custom") customFastingHours else program.fastingHours
    val eatingHours = if (program.id == "custom") customEatingHours else program.eatingHours
    FastingProgramUiState(
        id = program.id,
        title = if (program.id == "custom") {
            "Custom ${fastingHours.formatInputNumber()}:${eatingHours.formatInputNumber()}"
        } else {
            program.title
        },
        fastingHours = fastingHours,
        eatingHours = eatingHours,
        description = program.description,
        isSelected = program.id == selectedProgramId,
    )
}

private fun String.sanitizeFastingTimeInput(): String {
    val digits = filter(Char::isDigit).take(4)
    return when {
        digits.length <= 2 -> digits
        else -> "${digits.take(2)}:${digits.drop(2)}"
    }
}

private fun String.toFastingStartTimeOrDefault(): LocalTime {
    val parts = split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull()
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return if (hour != null && hour in 0..23 && minute in 0..59) {
        LocalTime.of(hour, minute)
    } else {
        LocalTime.of(20, 0)
    }
}

private fun LocalTime.toHourMinuteLabel(): String = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

private fun String.toLocalAiNutritionDraft(): LocalAiNutritionDraft {
    val lower = lowercase()
    var calories = 0.0
    var protein = 0.0
    var carbs = 0.0
    var fat = 0.0
    val matched = mutableListOf<String>()

    val eggCount =
        Regex("""(\d+)\s*eggs?""").find(lower)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
            ?: if ("egg" in lower) 1.0 else 0.0
    if (eggCount > 0.0) {
        calories += 70.0 * eggCount
        protein += 6.0 * eggCount
        carbs += 0.5 * eggCount
        fat += 5.0 * eggCount
        matched += "eggs"
    }
    if ("toast" in lower) {
        calories += 100.0
        protein += 4.0
        carbs += 18.0
        fat += 1.0
        matched += "toast"
    }
    if ("yogurt" in lower || "yoghurt" in lower) {
        calories += 120.0
        protein += 18.0
        carbs += 8.0
        fat += 2.0
        matched += "yogurt"
    }
    if ("banana" in lower) {
        calories += 105.0
        protein += 1.0
        carbs += 27.0
        fat += 0.0
        matched += "banana"
    }

    if (matched.isEmpty()) {
        return LocalAiNutritionDraft(250.0, 12.0, 30.0, 8.0, "Local estimate: generic meal")
    }
    return LocalAiNutritionDraft(
        caloriesKcal = calories,
        proteinGrams = protein,
        carbsGrams = carbs,
        fatGrams = fat,
        review = "Local estimate: ${matched.joinToString(", ")}",
    )
}

private fun FoodUiState.withFoodGoal(goal: FoodGoal): FoodUiState {
    val updatedState = copy(
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
        foodPrograms = buildFoodProgramUiStates(goal.mode),
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
    return updatedState.withRecipes(updatedState.recipes)
}

private fun FoodUiState.withRecipes(recipes: List<RecipeUiState>): FoodUiState {
    val discoveryItems = buildRecipeDiscoveryItems(recipes, goalMode)
    return copy(
        recipes = recipes,
        recipeDiscovery = recipeDiscovery.copy(
            items = discoveryItems,
            visibleItems = discoveryItems.filterForRecipeDiscovery(recipeDiscovery.filter, recipeDiscovery.query),
        ),
    )
}

private fun FoodUiState.withReportedSavedFoodIds(reportedIds: Set<String>): FoodUiState {
    val updatedFoods = savedFoods.map { food -> food.withTrust(isReported = food.id in reportedIds) }
    return copy(
        reportedSavedFoodIds = reportedIds,
        savedFoods = updatedFoods,
        visibleSavedFoods = updatedFoods.filterForDatabaseQuery(foodDatabaseQuery),
        selectedSavedFoodDetail = selectedSavedFoodDetail?.let { selectedFood ->
            selectedFood.withTrust(isReported = selectedFood.id in reportedIds)
        },
    )
}

private fun FoodUiState.withFoodHealthConnectSyncState(syncState: FoodHealthConnectSyncState): FoodUiState = copy(
    foodHealthConnectSyncEnabled = syncState.isEnabled,
    foodHealthConnectCanRequestPermissions = syncState.canRequestPermissions,
    foodHealthConnectCanSync = syncState.canSync,
    foodHealthConnectRequestablePermissions = syncState.requestablePermissions,
    foodHealthConnectPermissionSummary = syncState.permissionSummary(),
    foodHealthConnectLastSyncAtEpochMillis = syncState.lastSyncAtEpochMillis,
    foodHealthConnectLastFailureMessage = syncState.lastFailureMessage,
)

private fun FoodHealthConnectSyncState.permissionSummary(): String = when (availability) {
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

internal fun FoodDiary.isEmptyLoggedDiary(): Boolean = meals
    .flatMap { meal -> meal.entries }
    .none { entry -> entry.status == FoodDiaryEntryStatus.Logged }

@Suppress("LongMethod", "CyclomaticComplexMethod")
internal fun FoodUiState.buildDayRatingFactors(diary: FoodDiary): List<FoodRatingFactorUiState> {
    val calories = diary.totals.caloriesKcal
    val protein = diary.totals.proteinGrams
    val fiber = diary.detailTotals.fiberGrams
    val sodium = diary.detailTotals.sodiumMilligrams
    return listOf(
        FoodRatingFactorUiState(
            label = "Calories",
            valueLabel = "${calories.roundToInt()} / ${calorieGoalKcal.roundToInt()} kcal",
            explanation = when {
                calories > calorieGoalKcal * 1.1 -> "Above today's calorie target."
                calories < calorieGoalKcal * 0.5 -> "Still low for today."
                else -> "Aligned with today's calorie target."
            },
            tone = when {
                calories > calorieGoalKcal * 1.1 -> FoodInsightTone.Warning
                calories < calorieGoalKcal * 0.5 -> FoodInsightTone.Neutral
                else -> FoodInsightTone.Positive
            },
        ),
        FoodRatingFactorUiState(
            label = "Protein",
            valueLabel = "${protein.formatNutritionDisplay()} / ${proteinGoalGrams.formatNutritionDisplay()} g",
            explanation = when {
                protein < proteinGoalGrams * 0.6 -> "Well below your protein target."
                protein < proteinGoalGrams * 0.9 -> "A little short of your protein target."
                else -> "On pace for your protein target."
            },
            tone = when {
                protein < proteinGoalGrams * 0.6 -> FoodInsightTone.Warning
                protein < proteinGoalGrams * 0.9 -> FoodInsightTone.Neutral
                else -> FoodInsightTone.Positive
            },
        ),
        FoodRatingFactorUiState(
            label = "Fiber",
            valueLabel = "${fiber.formatNutritionDisplay()} / ${fiberGoalGrams.formatNutritionDisplay()} g",
            explanation = when {
                fiber < fiberGoalGrams * 0.5 -> "Low fiber for the day."
                fiber < fiberGoalGrams * 0.8 -> "Some more fiber would help."
                else -> "Fiber is in a strong range."
            },
            tone = when {
                fiber < fiberGoalGrams * 0.5 -> FoodInsightTone.Warning
                fiber < fiberGoalGrams * 0.8 -> FoodInsightTone.Neutral
                else -> FoodInsightTone.Positive
            },
        ),
        FoodRatingFactorUiState(
            label = "Sodium",
            valueLabel = "${sodium.roundToInt()} / ${sodiumGoalMilligrams.roundToInt()} mg",
            explanation = if (sodium > sodiumGoalMilligrams) {
                "Above today's sodium target."
            } else {
                "Within today's sodium target."
            },
            tone = if (sodium > sodiumGoalMilligrams) FoodInsightTone.Warning else FoodInsightTone.Positive,
        ),
        buildDietModeRatingFactor(diary),
    )
}

private fun FoodUiState.buildDietModeRatingFactor(diary: FoodDiary): FoodRatingFactorUiState = when (goalMode) {
    FoodGoalMode.HighProtein -> {
        val protein = diary.totals.proteinGrams
        FoodRatingFactorUiState(
            label = "High protein focus",
            valueLabel = "${protein.formatNutritionDisplay()} / ${proteinGoalGrams.formatNutritionDisplay()} g",
            explanation = if (protein >= proteinGoalGrams * 0.9) {
                "High Protein mode is on track."
            } else {
                "High Protein mode needs more lean protein today."
            },
            tone = if (protein >= proteinGoalGrams * 0.9) FoodInsightTone.Positive else FoodInsightTone.Warning,
        )
    }

    FoodGoalMode.KetoLowCarb -> {
        val carbs = if (useNetCarbs) {
            (diary.totals.carbsGrams - diary.detailTotals.fiberGrams).coerceAtLeast(0.0)
        } else {
            diary.totals.carbsGrams
        }
        FoodRatingFactorUiState(
            label = "Low-carb focus",
            valueLabel = "${carbs.formatNutritionDisplay()} / ${carbsGoalGrams.formatNutritionDisplay()} g",
            explanation = if (carbs <= carbsGoalGrams) {
                "Low-carb mode is within today's carb target."
            } else {
                "Low-carb mode is over today's carb target."
            },
            tone = if (carbs <= carbsGoalGrams) FoodInsightTone.Positive else FoodInsightTone.Warning,
        )
    }

    FoodGoalMode.MuscleGain -> {
        val calories = diary.totals.caloriesKcal
        FoodRatingFactorUiState(
            label = "Muscle gain focus",
            valueLabel = "${calories.roundToInt()} / ${calorieGoalKcal.roundToInt()} kcal",
            explanation = if (calories >= calorieGoalKcal * 0.85 && diary.totals.proteinGrams >= proteinGoalGrams * 0.9) {
                "Muscle Gain mode has enough energy and protein."
            } else {
                "Muscle Gain mode needs more calories or protein."
            },
            tone = if (calories >= calorieGoalKcal * 0.85 && diary.totals.proteinGrams >= proteinGoalGrams * 0.9) {
                FoodInsightTone.Positive
            } else {
                FoodInsightTone.Warning
            },
        )
    }

    FoodGoalMode.WeightLoss -> {
        val calories = diary.totals.caloriesKcal
        FoodRatingFactorUiState(
            label = "Weight loss focus",
            valueLabel = "${calories.roundToInt()} / ${calorieGoalKcal.roundToInt()} kcal",
            explanation = if (calories <= calorieGoalKcal && diary.totals.proteinGrams >= proteinGoalGrams * 0.8) {
                "Weight Loss mode is controlled without losing protein."
            } else {
                "Weight Loss mode needs tighter calories or more protein."
            },
            tone = if (calories <= calorieGoalKcal && diary.totals.proteinGrams >= proteinGoalGrams * 0.8) {
                FoodInsightTone.Positive
            } else {
                FoodInsightTone.Warning
            },
        )
    }

    FoodGoalMode.MediterraneanStyle ->
        FoodRatingFactorUiState(
            label = "Mediterranean focus",
            valueLabel = goalMode.label,
            explanation = "Mediterranean-style mode emphasizes fiber, fish or legumes, and unsaturated fats.",
            tone = FoodInsightTone.Positive,
        )

    FoodGoalMode.CleanEating ->
        FoodRatingFactorUiState(
            label = "Clean eating focus",
            valueLabel = goalMode.label,
            explanation = "Clean Eating mode emphasizes protein, fiber, lower sugar, and calmer sodium.",
            tone = FoodInsightTone.Positive,
        )

    FoodGoalMode.Custom ->
        FoodRatingFactorUiState(
            label = "Custom focus",
            valueLabel = goalMode.label,
            explanation = "Rated against your custom calorie and nutrient targets.",
            tone = FoodInsightTone.Neutral,
        )

    FoodGoalMode.Balanced ->
        FoodRatingFactorUiState(
            label = "Balanced focus",
            valueLabel = goalMode.label,
            explanation = "Balanced mode checks calories, protein, fiber, and sodium together.",
            tone = FoodInsightTone.Positive,
        )
}

/**
 * Matches habit keywords against whole word tokens of the food name/brand, not raw substrings, so
 * "Pepperoni pizza" no longer counts as a vegetable ("pepper") and "Blueberry muffin" no longer
 * counts as fruit ("berry"). Keyword sets carry both singular and plural forms.
 */
internal fun FoodDiaryEntry.matchesHabitKeyword(keywords: Set<String>): Boolean {
    val tokens = "$name ${brand.orEmpty()}".lowercase().split(Regex("[^a-z]+"))
    return tokens.any { it.isNotEmpty() && it in keywords }
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
    val factors = listOf(
        FoodRatingFactorUiState(
            label = "Calories",
            valueLabel = "${meal.totals.caloriesKcal.roundToInt()} / ${calorieTargetKcal.roundToInt()} kcal",
            explanation = if (meal.totals.caloriesKcal > calorieTargetKcal * 1.15) {
                "Above this meal's target."
            } else {
                "Reasonable for this meal."
            },
            tone = if (meal.totals.caloriesKcal > calorieTargetKcal * 1.15) FoodInsightTone.Warning else FoodInsightTone.Positive,
        ),
        FoodRatingFactorUiState(
            label = "Protein",
            valueLabel = "${meal.totals.proteinGrams.formatNutritionDisplay()} g",
            explanation = if (meal.totals.proteinGrams >= 20.0) "Protein anchor is covered." else "Add a protein anchor.",
            tone = if (meal.totals.proteinGrams >= 20.0) FoodInsightTone.Positive else FoodInsightTone.Warning,
        ),
        FoodRatingFactorUiState(
            label = "Fiber",
            valueLabel = "${meal.detailTotals.fiberGrams.formatNutritionDisplay()} g",
            explanation = if (meal.detailTotals.fiberGrams >= 5.0) "Fiber is helpful here." else "Fiber is light.",
            tone = if (meal.detailTotals.fiberGrams >= 5.0) FoodInsightTone.Positive else FoodInsightTone.Neutral,
        ),
        FoodRatingFactorUiState(
            label = "Sodium",
            valueLabel = "${meal.detailTotals.sodiumMilligrams.roundToInt()} mg",
            explanation = if (meal.detailTotals.sodiumMilligrams > 900.0) "High for one meal." else "Not high for one meal.",
            tone = if (meal.detailTotals.sodiumMilligrams > 900.0) FoodInsightTone.Warning else FoodInsightTone.Positive,
        ),
    )

    return FoodRatingUiState(
        label = score.toFoodRatingLabel(),
        reason = reasons.firstOrNull() ?: "Protein, fiber, sodium, and calories look balanced.",
        suggestion = suggestions.firstOrNull() ?: "Repeat this structure when it fits your day.",
        tone = score.toFoodRatingTone(),
        score = score.coerceIn(0, 100),
        factors = factors,
    )
}

private fun FoodDiaryEntry.toFoodEntryRating(): FoodRatingUiState? {
    if (status != FoodDiaryEntryStatus.Logged || caloriesKcal <= 0.0) {
        return null
    }

    var score = 100
    val reasons = mutableListOf<String>()
    val suggestions = mutableListOf<String>()
    if (nutritionDetails.sugarGrams > 20.0) {
        score -= 35
        reasons += "Sugar is high for one food."
        suggestions += "Pair it with a lower-sugar choice next."
    }
    if (nutritionDetails.sodiumMilligrams > 700.0) {
        score -= 25
        reasons += "Sodium is high for one food."
        suggestions += "Balance it with lower-sodium foods."
    }
    if (nutritionDetails.saturatedFatGrams > 7.0) {
        score -= 20
        reasons += "Saturated fat is high."
        suggestions += "Keep the next fat source lighter."
    }
    if (caloriesKcal >= 300.0 && proteinGrams < 10.0) {
        score -= 20
        reasons += "Calories are not backed by much protein."
        suggestions += "Add a protein-forward food nearby."
    }
    if (caloriesKcal >= 200.0 && nutritionDetails.fiberGrams < 2.0 && carbsGrams > 25.0) {
        score -= 10
        reasons += "Fiber is light for the carbs."
        suggestions += "Add fruit, veg, beans, or whole grains."
    }

    val factors = listOf(
        FoodRatingFactorUiState(
            label = "Protein",
            valueLabel = "${proteinGrams.formatNutritionDisplay()} g",
            explanation = if (proteinGrams >= 10.0) "Useful protein contribution." else "Low protein contribution.",
            tone = if (proteinGrams >= 10.0) FoodInsightTone.Positive else FoodInsightTone.Neutral,
        ),
        FoodRatingFactorUiState(
            label = "Fiber",
            valueLabel = "${nutritionDetails.fiberGrams.formatNutritionDisplay()} g",
            explanation = if (nutritionDetails.fiberGrams >= 2.0) "Adds fiber." else "Little fiber.",
            tone = if (nutritionDetails.fiberGrams >= 2.0) FoodInsightTone.Positive else FoodInsightTone.Neutral,
        ),
        FoodRatingFactorUiState(
            label = "Sugar",
            valueLabel = "${nutritionDetails.sugarGrams.formatNutritionDisplay()} g",
            explanation = if (nutritionDetails.sugarGrams > 20.0) "High sugar for one food." else "Sugar is not high.",
            tone = if (nutritionDetails.sugarGrams > 20.0) FoodInsightTone.Warning else FoodInsightTone.Positive,
        ),
        FoodRatingFactorUiState(
            label = "Sodium",
            valueLabel = "${nutritionDetails.sodiumMilligrams.roundToInt()} mg",
            explanation = if (nutritionDetails.sodiumMilligrams > 700.0) "High sodium for one food." else "Sodium is not high.",
            tone = if (nutritionDetails.sodiumMilligrams > 700.0) FoodInsightTone.Warning else FoodInsightTone.Positive,
        ),
    )

    return FoodRatingUiState(
        label = score.coerceIn(0, 100).toFoodRatingLabel(),
        reason = reasons.firstOrNull() ?: "This food fits today's pattern well.",
        suggestion = suggestions.firstOrNull() ?: "Keep it when it supports your meal.",
        tone = score.coerceIn(0, 100).toFoodRatingTone(),
        score = score.coerceIn(0, 100),
        factors = factors,
    )
}

internal fun buildHabitTrackers(
    diary: FoodDiary,
    waterConsumedMilliliters: Double,
    waterGoalMilliliters: Double,
): List<FoodHabitTrackerUiState> {
    val loggedEntries =
        diary.meals
            .flatMap { meal -> meal.entries }
            .filter { entry -> entry.status == FoodDiaryEntryStatus.Logged }
    val waterProgress = waterConsumedMilliliters.fractionOf(waterGoalMilliliters)

    return listOf(
        habitFromEntries(
            id = "fruit",
            label = "Fruit",
            entries = loggedEntries,
            keywords = fruitHabitKeywords,
            suggestion = "Add fruit as a snack or side.",
        ),
        habitFromEntries(
            id = "vegetables",
            label = "Vegetables",
            entries = loggedEntries,
            keywords = vegetableHabitKeywords,
            suggestion = "Add vegetables to a meal.",
        ),
        habitFromEntries(
            id = "fish",
            label = "Fish",
            entries = loggedEntries,
            keywords = fishHabitKeywords,
            suggestion = "Plan fish or seafood this week.",
        ),
        FoodHabitTrackerUiState(
            id = "water",
            label = "Water",
            valueLabel = "${waterConsumedMilliliters.roundToInt()} / ${waterGoalMilliliters.roundToInt()} ml",
            progress = waterProgress,
            status = when {
                waterProgress >= 1.0 -> FoodHabitStatus.Complete
                waterConsumedMilliliters > 0.0 -> FoodHabitStatus.InProgress
                else -> FoodHabitStatus.Missing
            },
            tone = when {
                waterProgress >= 1.0 -> FoodInsightTone.Positive
                else -> FoodInsightTone.Neutral
            },
            suggestion = "Keep sipping through the day.",
        ),
    )
}

private fun habitFromEntries(
    id: String,
    label: String,
    entries: List<FoodDiaryEntry>,
    keywords: Set<String>,
    suggestion: String,
): FoodHabitTrackerUiState {
    val matched = entries.any { entry -> entry.matchesHabitKeyword(keywords) }
    return FoodHabitTrackerUiState(
        id = id,
        label = label,
        valueLabel = if (matched) "Logged" else "Not yet",
        progress = if (matched) 1.0 else 0.0,
        status = if (matched) FoodHabitStatus.Complete else FoodHabitStatus.Missing,
        // "Not yet" is incomplete, not a problem — keep it neutral so coral stays
        // reserved for real warnings (over-limit sodium, etc.).
        tone = if (matched) FoodInsightTone.Positive else FoodInsightTone.Neutral,
        suggestion = if (matched) "Covered today." else suggestion,
    )
}

internal val fruitHabitKeywords =
    setOf(
        "apple",
        "banana",
        "berry",
        "berries",
        "orange",
        "fruit",
        "mango",
        "grape",
        "pear",
        "peach",
        "pineapple",
        "melon",
        "kiwi",
    )

internal val vegetableHabitKeywords =
    setOf(
        "vegetable",
        "vegetables",
        "salad",
        "spinach",
        "broccoli",
        "carrot",
        "tomato",
        "cucumber",
        "pepper",
        "kale",
        "lettuce",
        "onion",
        "zucchini",
    )

internal val fishHabitKeywords =
    setOf(
        "fish",
        "salmon",
        "tuna",
        "cod",
        "sardine",
        "trout",
        "mackerel",
        "seafood",
        "shrimp",
        "prawn",
    )

internal fun Int.toFoodRatingLabel(): String = when {
    this >= 85 -> "Great"
    this >= 70 -> "Good"
    this >= 50 -> "Watch"
    else -> "Needs work"
}

internal fun Int.toFoodRatingTone(): FoodInsightTone = when {
    this >= 85 -> FoodInsightTone.Positive
    this >= 70 -> FoodInsightTone.Positive
    this >= 50 -> FoodInsightTone.Warning
    else -> FoodInsightTone.Warning
}

fun FoodUiState.selectedMealDetailForDisplay(): FoodMealSectionUiState? = selectedMealDetailId
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

@Suppress("LongMethod", "CyclomaticComplexMethod")
internal fun FoodDiary.toMealSections(
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

    // Hidden meals never render as diary sections (their entries still count toward the day
    // totals, which are aggregated separately). Unknown-meal synthesis above intentionally
    // checks the full definition list so a hidden-but-non-empty meal is not resurrected here.
    return (mealDefinitions.filterNot { it.isHidden } + unknownMealDefinitions).map { definition ->
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
                    rating = entry.toFoodEntryRating(),
                    isPlanned = entry.status == FoodDiaryEntryStatus.Planned,
                    imageUrl = entry.imageUrl,
                )
            },
        )
    }
}

internal fun Double.fractionOf(total: Double): Double = if (isFinite() && total.isFinite() && total > 0.0) {
    (this / total).coerceIn(0.0, 1.0)
} else {
    0.0
}

private fun SavedFoodItem.toUiState(isReported: Boolean = false): SavedFoodUiState {
    val servingMultiplier = defaultServingGrams / 100.0
    val sourceLabel = sourceLabel()
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
        sourceLabel = sourceLabel,
        trust = buildFoodTrust(
            sourceLabel = sourceLabel,
            barcode = barcode,
            category = category,
            caloriesPer100g = nutritionPer100g.caloriesKcal,
            proteinPer100g = nutritionPer100g.proteinGrams,
            carbsPer100g = nutritionPer100g.carbsGrams,
            fatPer100g = nutritionPer100g.fatGrams,
            isReported = isReported,
        ),
        servings = servings.map { serving ->
            SavedFoodServingUiState(
                id = serving.id,
                label = serving.label,
                grams = serving.grams,
            )
        },
    )
}

private fun SavedFoodUiState.withTrust(isReported: Boolean): SavedFoodUiState = copy(
    trust = buildFoodTrust(
        sourceLabel = sourceLabel,
        barcode = barcode,
        category = category,
        caloriesPer100g = caloriesPer100g,
        proteinPer100g = proteinPer100g,
        carbsPer100g = carbsPer100g,
        fatPer100g = fatPer100g,
        isReported = isReported,
    ),
)

private fun buildFoodTrust(
    sourceLabel: String,
    barcode: String?,
    category: String?,
    caloriesPer100g: Double,
    proteinPer100g: Double,
    carbsPer100g: Double,
    fatPer100g: Double,
    isReported: Boolean,
): FoodTrustUiState {
    val hasNutrition = listOf(caloriesPer100g, proteinPer100g, carbsPer100g, fatPer100g).any { it > 0.0 }
    return when {
        isReported ->
            FoodTrustUiState(
                level = FoodTrustLevel.NeedsReview,
                label = "Needs review",
                explanation = "Marked locally for correction. Review serving size and nutrition before relying on it.",
                actionLabel = "Correct",
                isReported = true,
            )

        !hasNutrition ->
            FoodTrustUiState(
                level = FoodTrustLevel.NeedsReview,
                label = "Missing nutrition",
                explanation = "This saved food has little nutrition data. Correct it before frequent logging.",
                actionLabel = "Correct",
            )

        category.equals("Nutrition label", ignoreCase = true) ->
            FoodTrustUiState(
                level = FoodTrustLevel.NeedsReview,
                label = "Review label scan",
                explanation = "Label scans are best-effort. Check extracted values before saving or repeated logging.",
                actionLabel = "Review",
            )

        sourceLabel == "Scanned" || !barcode.isNullOrBlank() ->
            FoodTrustUiState(
                level = FoodTrustLevel.Imported,
                label = "Barcode import",
                explanation = "Imported from barcode data. Serving size and brand should still be checked.",
                actionLabel = "Check",
            )

        else ->
            FoodTrustUiState(
                level = FoodTrustLevel.Manual,
                label = "Manual entry",
                explanation = "Created locally. Accuracy depends on the values entered in MusFit.",
                actionLabel = "Edit",
            )
    }
}

private fun FoodPlanDay.toUiState(): FoodPlanDayUiState = FoodPlanDayUiState(
    date = date,
    dayLabel = date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.titlecase() },
    loggedCaloriesKcal = loggedTotals.caloriesKcal,
    plannedCaloriesKcal = plannedTotals.caloriesKcal,
    loggedEntryCount = loggedEntryCount,
    plannedEntryCount = plannedEntryCount,
)

private fun ShoppingListGroup.toUiState(): ShoppingListGroupUiState = ShoppingListGroupUiState(
    category = category,
    items = items.map { it.toUiState() },
)

private fun ShoppingListItem.toUiState(): ShoppingListItemUiState = ShoppingListItemUiState(
    id = id,
    name = name,
    category = category,
    quantityGrams = quantityGrams,
    quantityLabel = "${quantityGrams.formatInputNumber()} g",
    isChecked = isChecked,
    isManual = isManual,
)

private fun SavedFoodItem.sourceLabel(): String = when {
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

private fun SavedFoodUiState.toBarcodeComparisonItem(): BarcodeComparisonItemUiState = BarcodeComparisonItemUiState(
    barcode = barcode.orEmpty(),
    name = name,
    brand = brand,
    sourceLabel = "Saved food",
    caloriesPer100g = caloriesPer100g,
    proteinPer100g = proteinPer100g,
    carbsPer100g = carbsPer100g,
    fatPer100g = fatPer100g,
    sugarPer100g = sugarPer100g,
    sodiumMgPer100g = sodiumMgPer100g,
    imageUrl = imageUrl,
)

private fun ProductLookupResult.Found.toBarcodeComparisonItem(): BarcodeComparisonItemUiState = BarcodeComparisonItemUiState(
    barcode = barcode,
    name = name,
    brand = brand,
    sourceLabel = "Open Food Facts",
    caloriesPer100g = nutritionPer100g.caloriesKcal,
    proteinPer100g = nutritionPer100g.proteinGrams,
    carbsPer100g = nutritionPer100g.carbsGrams,
    fatPer100g = nutritionPer100g.fatGrams,
    sugarPer100g = nutritionDetailsPer100g.sugarGrams,
    sodiumMgPer100g = nutritionDetailsPer100g.sodiumMilligrams,
    imageUrl = imageUrl,
)

private fun buildBarcodeComparisonHighlights(
    leftItem: BarcodeComparisonItemUiState?,
    rightItem: BarcodeComparisonItemUiState?,
): List<BarcodeComparisonHighlightUiState> {
    if (leftItem == null || rightItem == null) {
        return emptyList()
    }
    return listOf(
        barcodeComparisonHighlight("Calories", leftItem.caloriesPer100g, rightItem.caloriesPer100g, lowerIsBetter = true, unit = "kcal"),
        barcodeComparisonHighlight("Protein", leftItem.proteinPer100g, rightItem.proteinPer100g, lowerIsBetter = false, unit = "g"),
        barcodeComparisonHighlight("Carbs", leftItem.carbsPer100g, rightItem.carbsPer100g, lowerIsBetter = true, unit = "g"),
        barcodeComparisonHighlight("Fat", leftItem.fatPer100g, rightItem.fatPer100g, lowerIsBetter = true, unit = "g"),
        barcodeComparisonHighlight("Sugar", leftItem.sugarPer100g, rightItem.sugarPer100g, lowerIsBetter = true, unit = "g"),
        barcodeComparisonHighlight("Sodium", leftItem.sodiumMgPer100g, rightItem.sodiumMgPer100g, lowerIsBetter = true, unit = "mg"),
    )
}

private fun barcodeComparisonHighlight(
    label: String,
    leftValue: Double,
    rightValue: Double,
    lowerIsBetter: Boolean,
    unit: String,
): BarcodeComparisonHighlightUiState = BarcodeComparisonHighlightUiState(
    label = label,
    leftValue = leftValue.toComparisonValueLabel(unit),
    rightValue = rightValue.toComparisonValueLabel(unit),
    winnerSide = when {
        leftValue == rightValue -> null

        // Open Food Facts maps absent nutrients to 0.0, so a 0.0 on a lower-is-better nutrient
        // is ambiguous (likely "unknown"): don't crown the data-missing product the healthier one.
        lowerIsBetter && minOf(leftValue, rightValue) <= 0.0 -> null

        lowerIsBetter && leftValue < rightValue -> BarcodeComparisonSide.Left

        lowerIsBetter -> BarcodeComparisonSide.Right

        leftValue > rightValue -> BarcodeComparisonSide.Left

        else -> BarcodeComparisonSide.Right
    },
)

private fun Double.toComparisonValueLabel(unit: String): String = "${formatInputNumber()} $unit"

private fun MealTemplate.toUiState(): MealTemplateUiState = MealTemplateUiState(
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

private fun Recipe.toUiState(): RecipeUiState = RecipeUiState(
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

private fun QuickCaloriePreset.toUiState(): QuickCaloriePresetUiState = QuickCaloriePresetUiState(
    id = id,
    name = name,
    caloriesKcal = caloriesKcal,
    proteinGrams = proteinGrams,
    carbsGrams = carbsGrams,
    fatGrams = fatGrams,
    isFavorite = isFavorite,
)

private fun defaultAmountServingChoices(): List<FoodAmountServingChoiceUiState> = listOf(FoodAmountServingChoiceUiState("per-100g", "100 g", 100.0))

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

private fun ProductLookupResult.Found.toOnlineUiState(): OnlineFoodResultUiState = OnlineFoodResultUiState(
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

private fun OnlineFoodResultUiState.toSavedFoodUpsertInput(): SavedFoodUpsertInput = SavedFoodUpsertInput(
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

private fun emptyMealSections(): List<FoodMealSectionUiState> = mealDefinitions.map { definition ->
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

private fun emptyFoodDiary(): FoodDiary = FoodDiary(
    totals = NutritionTotals(0.0, 0.0, 0.0, 0.0),
    meals = emptyList(),
)

private fun defaultMealDefinitionUiStates(): List<FoodMealDefinitionUiState> = mealDefinitions.map { definition -> definition.toUiState() }

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

private fun MealDefinition.toUiState(): FoodMealDefinitionUiState = FoodMealDefinitionUiState(
    id = id,
    title = title,
    timeMinutes = timeMinutes,
    timeLabel = timeMinutes?.toMealTimeLabel() ?: "No time",
    sortOrder = sortOrder,
    isDefault = true,
    isHidden = false,
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
        isHidden = isHidden,
    )
}

private fun FoodMealDefinitionUiState.recommendation(): String = timeMinutes?.toMealTimeLabel()
    ?: mealDefinitions.firstOrNull { it.id == id }?.recommendation
    ?: "Custom meal"

private fun FoodMealDefinitionUiState.calorieTargetKcal(): Double = mealDefinitions.firstOrNull { it.id == id }?.calorieTargetKcal
    ?: (CALORIE_GOAL_KCAL / 4.0)

private fun FoodUiState.mealTitleFor(mealType: String): String = mealDefinitions.firstOrNull { it.id == mealType.normalizedMealType() }?.title
    ?: mealType.mealTitle()

private fun FoodUiState.nextMealSortOrder(): Int = (mealDefinitions.maxOfOrNull { it.sortOrder } ?: 30) + 10

private fun emptyMacroProgress(): List<FoodMacroProgressUiState> = NutritionTotals(0.0, 0.0, 0.0, 0.0).toMacroProgress()

private fun emptyAdvancedNutritionProgress(): List<FoodNutrientProgressUiState> = NutritionDetails().toAdvancedNutritionProgress()

private fun emptyMicronutrients(): List<FoodMicronutrientUiState> = NutritionDetails().toMicronutrients()

private fun emptyDailyInsights(): List<FoodInsightUiState> = listOf(
    FoodInsightUiState(
        title = "Start with a meal",
        body = "Log a meal, favorite, or quick calories to see today clearly.",
        tone = FoodInsightTone.Neutral,
    ),
)

private fun emptyFoodPrograms(): List<FoodProgramUiState> = buildFoodProgramUiStates(FoodGoalMode.Balanced)

private fun emptyRecipeDiscoveryItems(): List<RecipeDiscoveryItemUiState> = buildRecipeDiscoveryItems(emptyList(), FoodGoalMode.Balanced)

private fun emptyFoodTrust(): FoodTrustUiState = FoodTrustUiState(
    level = FoodTrustLevel.Manual,
    label = "Manual entry",
    explanation = "Created locally. Accuracy depends on the values entered in MusFit.",
    actionLabel = "Edit",
)

private fun emptyHabitTrackers(): List<FoodHabitTrackerUiState> = buildHabitTrackers(
    diary = emptyFoodDiary(),
    waterConsumedMilliliters = 0.0,
    waterGoalMilliliters = WATER_GOAL_MILLILITERS,
)

internal fun emptyFoodRating(): FoodRatingUiState = FoodRatingUiState(
    label = "No rating",
    reason = "Log food to rate today.",
    suggestion = "Start with a meal or favorite.",
    tone = FoodInsightTone.Neutral,
)

private fun String.normalizedMealType(): String {
    val normalized = trim().lowercase()
    return when (normalized) {
        "snack" -> "snacks"
        else -> normalized.ifBlank { "breakfast" }
    }
}

internal fun String.mealTitle(): String = mealDefinitions.firstOrNull { it.id == normalizedMealType() }?.title
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

private fun Int.toMealTimeLabel(): String = "${(this / 60).toString().padStart(2, '0')}:${(this % 60).toString().padStart(2, '0')}"

private fun String.parseNutritionValue(): Double? = trim()
    .takeIf { it.isNotEmpty() }
    ?.toDoubleOrNull()
    ?.takeIf { it.isFinite() && it >= 0.0 }

private fun String.parsePositiveNumberOrNull(): Double? = trim()
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

private fun String.parseDateOrNull(): LocalDate? = runCatching { LocalDate.parse(trim()) }.getOrNull()

internal fun Double.formatInputNumber(): String {
    val longValue = toLong()
    return if (this == longValue.toDouble()) {
        longValue.toString()
    } else {
        toString()
    }
}
