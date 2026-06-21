package com.musfit.ui.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musfit.data.remote.food.FoodProductProvider
import com.musfit.data.remote.food.ProductLookupResult
import com.musfit.data.remote.food.ProductSearchResult
import com.musfit.data.repository.DiaryEntryUpdateInput
import com.musfit.data.repository.FoodDiary
import com.musfit.data.repository.FoodGoal
import com.musfit.data.repository.FoodGoalMode
import com.musfit.data.repository.FoodLogInput
import com.musfit.data.repository.FoodRepository
import com.musfit.data.repository.FoodServingInput
import com.musfit.data.repository.MealTemplate
import com.musfit.data.repository.NutritionDetails
import com.musfit.data.repository.QuickCalorieLogInput
import com.musfit.data.repository.Recipe
import com.musfit.data.repository.RecipeIngredientInput
import com.musfit.data.repository.RecipeUpsertInput
import com.musfit.data.repository.SavedFoodItem
import com.musfit.data.repository.SavedFoodLogInput
import com.musfit.data.repository.SavedFoodUpsertInput
import com.musfit.domain.model.FoodNutrition
import com.musfit.domain.model.NutritionTotals
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val fatGrams: Double = 0.0,
    val fiberGrams: Double = 0.0,
    val sugarGrams: Double = 0.0,
    val saturatedFatGrams: Double = 0.0,
    val sodiumMilligrams: Double = 0.0,
    val entries: List<FoodMealEntryUiState>,
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
    val category: String?,
    val imageUrl: String?,
)

data class MealTemplateUiState(
    val id: String,
    val name: String,
    val mealType: String,
    val itemSummary: String,
)

data class RecipeUiState(
    val id: String,
    val name: String,
    val category: String?,
    val servingName: String,
    val servingGrams: Double,
    val caloriesPerServingKcal: Double,
    val proteinPerServingGrams: Double,
    val itemSummary: String,
    val ingredients: List<RecipeIngredientDraftUiState> = emptyList(),
)

data class RecipeIngredientDraftUiState(
    val foodId: String,
    val foodName: String,
    val quantityGrams: Double,
)

data class DeletedDiaryEntrySnapshot(
    val foodId: String,
    val mealType: String,
    val quantityGrams: Double,
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
    val goalMode: FoodGoalMode = FoodGoalMode.Maintain,
    val includeTrainingCalories: Boolean = false,
    val eatenCaloriesKcal: Double = 0.0,
    val remainingCaloriesKcal: Double = CALORIE_GOAL_KCAL,
    val macroProgress: List<FoodMacroProgressUiState> = emptyMacroProgress(),
    val mealSections: List<FoodMealSectionUiState> = emptyMealSections(),
    val selectedMealDetailId: String? = null,
    val mealDetailSortMode: MealDetailSortMode = MealDetailSortMode.Logged,
    val savedFoods: List<SavedFoodUiState> = emptyList(),
    val duplicateFoodGroups: List<FoodDuplicateGroupUiState> = emptyList(),
    val mealTemplates: List<MealTemplateUiState> = emptyList(),
    val recipes: List<RecipeUiState> = emptyList(),
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
    val keepAddingFoods: Boolean = false,
    val foodDatabaseQuery: String = "",
    val editingDiaryEntryId: String? = null,
    val editingDiaryEntryName: String = "",
    val editingDiaryEntryMealType: String = "breakfast",
    val editingDiaryEntryQuantityGrams: String = "",
    val editingDiaryEntryServingChoices: List<FoodAmountServingChoiceUiState> = emptyList(),
    val editingDiaryEntryCaloriesKcal: Double = 0.0,
    val editingDiaryEntryOriginalQuantityGrams: Double = 0.0,
    val editingDiaryEntryOriginalProteinGrams: Double = 0.0,
    val editingDiaryEntryOriginalCarbsGrams: Double = 0.0,
    val editingDiaryEntryOriginalFatGrams: Double = 0.0,
    val editingDiaryEntryPreviewCaloriesKcal: Double = 0.0,
    val editingDiaryEntryPreviewProteinGrams: Double = 0.0,
    val editingDiaryEntryPreviewCarbsGrams: Double = 0.0,
    val editingDiaryEntryPreviewFatGrams: Double = 0.0,
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
    val savedFoodServingName: String = "",
    val savedFoodBarcode: String = "",
    val savedFoodCategory: String = "",
    val savedFoodIsFavorite: Boolean = false,
    val editingTemplateId: String? = null,
    val templateNameInput: String = "",
    val templateMealTypeInput: String = "breakfast",
    val goalCaloriesKcalInput: String = CALORIE_GOAL_KCAL.formatInputNumber(),
    val goalProteinGramsInput: String = PROTEIN_GOAL_GRAMS.formatInputNumber(),
    val goalCarbsGramsInput: String = CARBS_GOAL_GRAMS.formatInputNumber(),
    val goalFatGramsInput: String = FAT_GOAL_GRAMS.formatInputNumber(),
    val goalFiberGramsInput: String = FIBER_GOAL_GRAMS.formatInputNumber(),
    val goalSugarGramsInput: String = SUGAR_GOAL_GRAMS.formatInputNumber(),
    val goalSaturatedFatGramsInput: String = SATURATED_FAT_GOAL_GRAMS.formatInputNumber(),
    val goalSodiumMgInput: String = SODIUM_GOAL_MILLIGRAMS.formatInputNumber(),
    val goalModeInput: FoodGoalMode = FoodGoalMode.Maintain,
    val goalIncludeTrainingInput: Boolean = false,
    val editingRecipeId: String? = null,
    val recipeName: String = "",
    val recipeCategory: String = "",
    val recipeServingName: String = "Serving",
    val recipeServingGrams: String = "100",
    val recipeIngredientFoodId: String = "",
    val recipeIngredientQuantityGrams: String = "100",
    val recipeIngredients: List<RecipeIngredientDraftUiState> = emptyList(),
    val recipeServingsToLog: String = "1",
    val lastDeletedDiaryEntry: DeletedDiaryEntrySnapshot? = null,
)

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

    init {
        viewModelScope.launch {
            selectedDateFlow.flatMapLatest { date ->
                repository.observeFoodDiary(date)
            }.collect { diary ->
                mutableState.update { currentState -> currentState.withDiary(diary) }
            }
        }
        viewModelScope.launch {
            repository.observeSavedFoods().collect { savedFoods ->
                mutableState.update { currentState ->
                    val foodStates = savedFoods.map { it.toUiState() }
                    currentState.copy(
                        savedFoods = foodStates,
                        duplicateFoodGroups = foodStates.toDuplicateFoodGroups(),
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.observeFoodGoal().collect { goal ->
                mutableState.update { currentState -> currentState.withFoodGoal(goal) }
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

    fun openAddFood(mealType: String) {
        val normalizedMealType = mealType.normalizedMealType()
        mutableState.update {
            it.copy(
                isAddPanelVisible = true,
                sheetMode = FoodSheetMode.AddFood,
                mealType = normalizedMealType,
                selectedMealTitle = normalizedMealType.mealTitle(),
                addMode = FoodAddMode.Saved,
                message = null,
            )
        }
    }

    fun closeAddFood() {
        mutableState.update {
            it.copy(
                isAddPanelVisible = false,
                sheetMode = null,
                message = null,
                editingDiaryEntryId = null,
                editingSavedFoodId = null,
                editingTemplateId = null,
                editingRecipeId = null,
                selectedSavedFoodDetail = null,
            )
        }
    }

    fun selectAddMode(mode: FoodAddMode) {
        mutableState.update { it.copy(addMode = mode, message = null) }
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
                editingDiaryEntryId = null,
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
                    recipeIngredients = recipe.ingredients,
                    message = null,
                )
            }
        }
    }

    fun onFoodDatabaseQueryChanged(value: String) {
        mutableState.update { it.copy(foodDatabaseQuery = value, message = null) }
    }

    fun searchOnlineFoods() {
        val query = state.value.foodDatabaseQuery.trim()
        if (query.length < 2) {
            mutableState.update { it.copy(message = "Enter at least 2 characters", onlineFoodResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(isSearchingFoods = true, message = null) }
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
        if (!markSaving()) {
            return
        }
        viewModelScope.launch {
            try {
                repository.renameMealTemplate(
                    templateId = templateId,
                    name = currentState.templateNameInput,
                    mealType = currentState.templateMealTypeInput,
                )
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        editingTemplateId = null,
                        templateNameInput = "",
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
                editingDiaryEntryId = entry.id,
                editingDiaryEntryName = entry.name,
                editingDiaryEntryMealType = section.id,
                editingDiaryEntryQuantityGrams = entry.quantityGrams.formatInputNumber(),
                editingDiaryEntryServingChoices = servingChoices,
                editingDiaryEntryCaloriesKcal = entry.caloriesKcal,
                editingDiaryEntryOriginalQuantityGrams = entry.quantityGrams,
                editingDiaryEntryOriginalProteinGrams = entry.proteinGrams,
                editingDiaryEntryOriginalCarbsGrams = entry.carbsGrams,
                editingDiaryEntryOriginalFatGrams = entry.fatGrams,
                editingDiaryEntryPreviewCaloriesKcal = entry.caloriesKcal,
                editingDiaryEntryPreviewProteinGrams = entry.proteinGrams,
                editingDiaryEntryPreviewCarbsGrams = entry.carbsGrams,
                editingDiaryEntryPreviewFatGrams = entry.fatGrams,
            )
        }
    }

    fun onDiaryEntryMealChanged(value: String) {
        mutableState.update {
            it.copy(
                editingDiaryEntryMealType = value.normalizedMealType(),
                message = null,
            )
        }
    }

    fun onDiaryEntryQuantityChanged(value: String) {
        mutableState.update {
            it.copy(
                editingDiaryEntryQuantityGrams = value.sanitizeDecimalInput(),
                message = null,
            ).withDiaryEntryPreview()
        }
    }

    fun onDiaryEntryServingChoiceSelected(choiceId: String) {
        mutableState.update { currentState ->
            val choice = currentState.editingDiaryEntryServingChoices.firstOrNull { choice -> choice.id == choiceId }
            if (choice == null) {
                currentState.copy(message = "Serving not found")
            } else {
                currentState.copy(
                    editingDiaryEntryQuantityGrams = choice.grams.formatInputNumber(),
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
        val mealItemId = currentState.editingDiaryEntryId
        if (mealItemId == null) {
            mutableState.update { it.copy(message = "Choose a diary item") }
            return
        }
        val quantityGrams = currentState.editingDiaryEntryQuantityGrams.parsePositiveNumberOrNull()
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
                        mealType = currentState.editingDiaryEntryMealType,
                        quantityGrams = quantityGrams,
                        date = currentState.selectedDate,
                    ),
                )
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        isAddPanelVisible = false,
                        sheetMode = null,
                        editingDiaryEntryId = null,
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
        val mealItemId = currentState.editingDiaryEntryId
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
                        editingDiaryEntryId = null,
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
        val mealItemId = currentState.editingDiaryEntryId
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
                        editingDiaryEntryId = null,
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
                selectedMealTitle = value.normalizedMealType().mealTitle(),
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
                repository.logSavedFood(
                    SavedFoodLogInput(
                        foodId = foodId,
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
        mutableState.update { it.copy(goalCaloriesKcalInput = value.sanitizeDecimalInput(), message = null) }
    }

    fun onGoalProteinChanged(value: String) {
        mutableState.update { it.copy(goalProteinGramsInput = value.sanitizeDecimalInput(), message = null) }
    }

    fun onGoalCarbsChanged(value: String) {
        mutableState.update { it.copy(goalCarbsGramsInput = value.sanitizeDecimalInput(), message = null) }
    }

    fun onGoalFatChanged(value: String) {
        mutableState.update { it.copy(goalFatGramsInput = value.sanitizeDecimalInput(), message = null) }
    }

    fun onGoalFiberChanged(value: String) {
        mutableState.update { it.copy(goalFiberGramsInput = value.sanitizeDecimalInput(), message = null) }
    }

    fun onGoalSugarChanged(value: String) {
        mutableState.update { it.copy(goalSugarGramsInput = value.sanitizeDecimalInput(), message = null) }
    }

    fun onGoalSaturatedFatChanged(value: String) {
        mutableState.update { it.copy(goalSaturatedFatGramsInput = value.sanitizeDecimalInput(), message = null) }
    }

    fun onGoalSodiumChanged(value: String) {
        mutableState.update { it.copy(goalSodiumMgInput = value.sanitizeDecimalInput(), message = null) }
    }

    fun onGoalModeChanged(value: FoodGoalMode) {
        mutableState.update { it.copy(goalModeInput = value, message = null) }
    }

    fun onGoalIncludeTrainingChanged(value: Boolean) {
        mutableState.update { it.copy(goalIncludeTrainingInput = value, message = null) }
    }

    fun saveFoodGoal() {
        val currentState = state.value
        val goal =
            FoodGoal(
                dailyCaloriesKcal = currentState.goalCaloriesKcalInput.parsePositiveNumberOrNull() ?: run {
                    mutableState.update { it.copy(message = "Enter a valid calorie goal") }
                    return
                },
                proteinGrams = currentState.goalProteinGramsInput.parseNonNegativeNumberOrZero() ?: return invalidGoal(),
                carbsGrams = currentState.goalCarbsGramsInput.parseNonNegativeNumberOrZero() ?: return invalidGoal(),
                fatGrams = currentState.goalFatGramsInput.parseNonNegativeNumberOrZero() ?: return invalidGoal(),
                fiberGrams = currentState.goalFiberGramsInput.parseNonNegativeNumberOrZero() ?: return invalidGoal(),
                sugarGrams = currentState.goalSugarGramsInput.parseNonNegativeNumberOrZero() ?: return invalidGoal(),
                saturatedFatGrams = currentState.goalSaturatedFatGramsInput.parseNonNegativeNumberOrZero() ?: return invalidGoal(),
                sodiumMilligrams = currentState.goalSodiumMgInput.parseNonNegativeNumberOrZero() ?: return invalidGoal(),
                mode = currentState.goalModeInput,
                includeTrainingCalories = currentState.goalIncludeTrainingInput,
            )
        if (!markSaving()) {
            return
        }
        viewModelScope.launch {
            try {
                repository.updateFoodGoal(goal)
                mutableState.update {
                    it.withFoodGoal(goal).copy(
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
        mutableState.update { it.copy(recipeServingGrams = value.sanitizeDecimalInput(), message = null) }
    }

    fun onRecipeIngredientFoodChanged(value: String) {
        mutableState.update { it.copy(recipeIngredientFoodId = value, message = null) }
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
        mutableState.update {
            it.copy(
                recipeIngredients = it.recipeIngredients + RecipeIngredientDraftUiState(food.id, food.name, quantity),
                recipeIngredientFoodId = "",
                recipeIngredientQuantityGrams = "100",
                message = null,
            )
        }
    }

    fun saveRecipe() {
        val currentState = state.value
        val servingGrams = currentState.recipeServingGrams.parsePositiveNumberOrNull()
        if (currentState.recipeName.isBlank() || servingGrams == null || currentState.recipeIngredients.isEmpty()) {
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
                        ingredients = currentState.recipeIngredients.map { ingredient ->
                            RecipeIngredientInput(
                                foodId = ingredient.foodId,
                                quantityGrams = ingredient.quantityGrams,
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

        return NutritionDetails(
            fiberGrams = fiber,
            sugarGrams = sugar,
            saturatedFatGrams = saturatedFat,
            sodiumMilligrams = sodium,
        )
    }

    private fun FoodUiState.withAmountNutritionPreview(): FoodUiState {
        val quantity = quantityGrams.parsePositiveNumberOrNull()
        val nutrition = toEditedNutritionOrNull()
        val preview = if (quantity != null && nutrition != null) {
            val scale = quantity / 100.0
            FoodAmountNutritionPreviewUiState(
                quantityGrams = quantity,
                caloriesKcal = nutrition.caloriesKcal * scale,
                proteinGrams = nutrition.proteinGrams * scale,
                carbsGrams = nutrition.carbsGrams * scale,
                fatGrams = nutrition.fatGrams * scale,
            )
        } else {
            null
        }

        return copy(amountNutritionPreview = preview)
    }

    private fun FoodUiState.withDiaryEntryPreview(): FoodUiState {
        val quantity = editingDiaryEntryQuantityGrams.parsePositiveNumberOrNull()
        val originalQuantity = editingDiaryEntryOriginalQuantityGrams
        if (quantity == null || originalQuantity <= 0.0) {
            return copy(
                editingDiaryEntryPreviewCaloriesKcal = 0.0,
                editingDiaryEntryPreviewProteinGrams = 0.0,
                editingDiaryEntryPreviewCarbsGrams = 0.0,
                editingDiaryEntryPreviewFatGrams = 0.0,
            )
        }

        val scale = quantity / originalQuantity
        return copy(
            editingDiaryEntryPreviewCaloriesKcal = editingDiaryEntryCaloriesKcal * scale,
            editingDiaryEntryPreviewProteinGrams = editingDiaryEntryOriginalProteinGrams * scale,
            editingDiaryEntryPreviewCarbsGrams = editingDiaryEntryOriginalCarbsGrams * scale,
            editingDiaryEntryPreviewFatGrams = editingDiaryEntryOriginalFatGrams * scale,
        )
    }

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
        )
}

private data class MealDefinition(
    val id: String,
    val title: String,
    val recommendation: String,
    val calorieTargetKcal: Double,
)

private val mealDefinitions =
    listOf(
        MealDefinition("breakfast", "Breakfast", "Recommended 417 - 625 kcal", 625.0),
        MealDefinition("lunch", "Lunch", "Recommended 625 - 833 kcal", 833.0),
        MealDefinition("dinner", "Dinner", "Recommended 625 - 833 kcal", 833.0),
        MealDefinition("snacks", "Snacks", "Recommended 104 - 208 kcal", 208.0),
    )

private const val CALORIE_GOAL_KCAL = 2083.0
private const val CARBS_GOAL_GRAMS = 260.0
private const val PROTEIN_GOAL_GRAMS = 104.0
private const val FAT_GOAL_GRAMS = 69.0
private const val FIBER_GOAL_GRAMS = 30.0
private const val SUGAR_GOAL_GRAMS = 50.0
private const val SATURATED_FAT_GOAL_GRAMS = 20.0
private const val SODIUM_GOAL_MILLIGRAMS = 2300.0

private fun FoodUiState.withDiary(diary: FoodDiary): FoodUiState =
    copy(
        eatenCaloriesKcal = diary.totals.caloriesKcal,
        remainingCaloriesKcal = calorieGoalKcal - diary.totals.caloriesKcal,
        macroProgress = diary.totals.toMacroProgress(
            carbsGoalGrams = carbsGoalGrams,
            proteinGoalGrams = proteinGoalGrams,
            fatGoalGrams = fatGoalGrams,
        ),
        mealSections = diary.toMealSections(),
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
        remainingCaloriesKcal = goal.dailyCaloriesKcal - eatenCaloriesKcal,
        macroProgress = NutritionTotals(eatenCaloriesKcal, proteinGoalGrams, carbsGoalGrams, fatGoalGrams).copy(
            proteinGrams = macroProgress.firstOrNull { it.label == "Protein" }?.currentGrams ?: 0.0,
            carbsGrams = macroProgress.firstOrNull { it.label == "Carbs" }?.currentGrams ?: 0.0,
            fatGrams = macroProgress.firstOrNull { it.label == "Fat" }?.currentGrams ?: 0.0,
        ).toMacroProgress(
            carbsGoalGrams = goal.carbsGrams,
            proteinGoalGrams = goal.proteinGrams,
            fatGoalGrams = goal.fatGrams,
        ),
        goalCaloriesKcalInput = goal.dailyCaloriesKcal.formatInputNumber(),
        goalProteinGramsInput = goal.proteinGrams.formatInputNumber(),
        goalCarbsGramsInput = goal.carbsGrams.formatInputNumber(),
        goalFatGramsInput = goal.fatGrams.formatInputNumber(),
        goalFiberGramsInput = goal.fiberGrams.formatInputNumber(),
        goalSugarGramsInput = goal.sugarGrams.formatInputNumber(),
        goalSaturatedFatGramsInput = goal.saturatedFatGrams.formatInputNumber(),
        goalSodiumMgInput = goal.sodiumMilligrams.formatInputNumber(),
        goalModeInput = goal.mode,
        goalIncludeTrainingInput = goal.includeTrainingCalories,
    )

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

private fun FoodDiary.toMealSections(): List<FoodMealSectionUiState> {
    val mealsByType = meals.associateBy { it.type.normalizedMealType() }
    return mealDefinitions.map { definition ->
        val meal = mealsByType[definition.id]
        val totals = meal?.totals
        FoodMealSectionUiState(
            id = definition.id,
            title = definition.title,
            recommendation = definition.recommendation,
            caloriesKcal = totals?.caloriesKcal ?: 0.0,
            calorieTargetKcal = definition.calorieTargetKcal,
            calorieProgress = (totals?.caloriesKcal ?: 0.0).fractionOf(definition.calorieTargetKcal),
            proteinGrams = totals?.proteinGrams ?: 0.0,
            carbsGrams = totals?.carbsGrams ?: 0.0,
            fatGrams = totals?.fatGrams ?: 0.0,
            fiberGrams = meal?.detailTotals?.fiberGrams ?: 0.0,
            sugarGrams = meal?.detailTotals?.sugarGrams ?: 0.0,
            saturatedFatGrams = meal?.detailTotals?.saturatedFatGrams ?: 0.0,
            sodiumMilligrams = meal?.detailTotals?.sodiumMilligrams ?: 0.0,
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

private fun SavedFoodItem.sourceLabel(): String =
    when {
        category.equals("Nutrition label", ignoreCase = true) -> "Label"
        !barcode.isNullOrBlank() -> "Scanned"
        else -> "Manual"
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

private fun MealTemplate.toUiState(): MealTemplateUiState =
    MealTemplateUiState(
        id = id,
        name = name,
        mealType = mealType,
        itemSummary = items.joinToString { item -> "${item.foodName} ${item.quantityGrams.formatInputNumber()}g" },
    )

private fun Recipe.toUiState(): RecipeUiState =
    RecipeUiState(
        id = id,
        name = name,
        category = category,
        servingName = servingName,
        servingGrams = servingGrams,
        caloriesPerServingKcal = nutritionPerServing.caloriesKcal,
        proteinPerServingGrams = nutritionPerServing.proteinGrams,
        itemSummary = ingredients.joinToString { ingredient -> ingredient.foodName },
        ingredients = ingredients.map { ingredient ->
            RecipeIngredientDraftUiState(
                foodId = ingredient.foodId,
                foodName = ingredient.foodName,
                quantityGrams = ingredient.quantityGrams,
            )
        },
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
        category = category,
        imageUrl = imageUrl,
    )

private fun OnlineFoodResultUiState.toSavedFoodUpsertInput(): SavedFoodUpsertInput =
    SavedFoodUpsertInput(
        foodId = null,
        name = name,
        brand = brand,
        defaultServingGrams = servingQuantityGrams ?: 100.0,
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
): List<FoodMacroProgressUiState> =
    listOf(
        FoodMacroProgressUiState("Carbs", carbsGrams, carbsGoalGrams),
        FoodMacroProgressUiState("Protein", proteinGrams, proteinGoalGrams),
        FoodMacroProgressUiState("Fat", fatGrams, fatGoalGrams),
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

private fun emptyMacroProgress(): List<FoodMacroProgressUiState> =
    NutritionTotals(0.0, 0.0, 0.0, 0.0).toMacroProgress()

private fun String.normalizedMealType(): String {
    val normalized = trim().lowercase()
    return when (normalized) {
        "snack" -> "snacks"
        else -> normalized.ifBlank { "breakfast" }
    }
}

private fun String.mealTitle(): String =
    mealDefinitions.firstOrNull { it.id == normalizedMealType() }?.title
        ?: trim().replaceFirstChar { char -> char.uppercase() }.ifBlank { "Meal" }

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
