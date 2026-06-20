package com.musfit.ui.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musfit.data.remote.food.FoodProductProvider
import com.musfit.data.remote.food.ProductLookupResult
import com.musfit.data.repository.DiaryEntryUpdateInput
import com.musfit.data.repository.FoodDiary
import com.musfit.data.repository.FoodLogInput
import com.musfit.data.repository.FoodRepository
import com.musfit.data.repository.QuickCalorieLogInput
import com.musfit.data.repository.SavedFoodItem
import com.musfit.data.repository.SavedFoodLogInput
import com.musfit.data.repository.SavedFoodUpsertInput
import com.musfit.domain.model.FoodNutrition
import com.musfit.domain.model.NutritionTotals
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    DiaryEntryEditor,
    SavedFoodEditor,
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
)

data class FoodMealSectionUiState(
    val id: String,
    val title: String,
    val recommendation: String,
    val caloriesKcal: Double,
    val proteinGrams: Double = 0.0,
    val carbsGrams: Double = 0.0,
    val fatGrams: Double = 0.0,
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
)

data class FoodUiState(
    val barcode: String = "",
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
    val lookupResult: ProductLookupResult.Found? = null,
    val calorieGoalKcal: Double = CALORIE_GOAL_KCAL,
    val eatenCaloriesKcal: Double = 0.0,
    val remainingCaloriesKcal: Double = CALORIE_GOAL_KCAL,
    val macroProgress: List<FoodMacroProgressUiState> = emptyMacroProgress(),
    val mealSections: List<FoodMealSectionUiState> = emptyMealSections(),
    val selectedMealDetailId: String? = null,
    val savedFoods: List<SavedFoodUiState> = emptyList(),
    val isAddPanelVisible: Boolean = false,
    val sheetMode: FoodSheetMode? = null,
    val selectedMealTitle: String = "Breakfast",
    val addMode: FoodAddMode = FoodAddMode.Saved,
    val savedFoodQuantityGrams: String = "100",
    val quickCaloriesKcal: String = "",
    val quickProteinGrams: String = "",
    val quickCarbsGrams: String = "",
    val quickFatGrams: String = "",
    val foodDatabaseQuery: String = "",
    val editingDiaryEntryId: String? = null,
    val editingDiaryEntryName: String = "",
    val editingDiaryEntryMealType: String = "breakfast",
    val editingDiaryEntryQuantityGrams: String = "",
    val editingDiaryEntryCaloriesKcal: Double = 0.0,
    val editingSavedFoodId: String? = null,
    val savedFoodName: String = "",
    val savedFoodBrand: String = "",
    val savedFoodServingGrams: String = "100",
    val savedFoodCaloriesPer100g: String = "",
    val savedFoodProteinPer100g: String = "",
    val savedFoodCarbsPer100g: String = "",
    val savedFoodFatPer100g: String = "",
)

@HiltViewModel
class FoodViewModel @Inject constructor(
    private val provider: FoodProductProvider,
    private val repository: FoodRepository,
) : ViewModel() {
    private val selectedDate = LocalDate.now()
    private val mutableState = MutableStateFlow(FoodUiState())
    val state: StateFlow<FoodUiState> = mutableState.asStateFlow()
    private var lookupJob: Job? = null

    init {
        viewModelScope.launch {
            repository.observeFoodDiary(selectedDate).collect { diary ->
                mutableState.update { currentState -> currentState.withDiary(diary) }
            }
        }
        viewModelScope.launch {
            repository.observeSavedFoods().collect { savedFoods ->
                mutableState.update { currentState ->
                    currentState.copy(savedFoods = savedFoods.map { it.toUiState() })
                }
            }
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
            )
        }
    }

    fun selectAddMode(mode: FoodAddMode) {
        mutableState.update { it.copy(addMode = mode, message = null) }
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
            )
        }
    }

    fun onFoodDatabaseQueryChanged(value: String) {
        mutableState.update { it.copy(foodDatabaseQuery = value, message = null) }
    }

    fun openDiaryEntryEditor(entryId: String) {
        val sectionAndEntry = state.value.mealSections.firstNotNullOfOrNull { section ->
            section.entries.firstOrNull { entry -> entry.id == entryId }?.let { entry -> section to entry }
        }
        if (sectionAndEntry == null) {
            mutableState.update { it.copy(message = "Diary item not found") }
            return
        }

        val (section, entry) = sectionAndEntry
        mutableState.update {
            it.copy(
                isAddPanelVisible = true,
                sheetMode = FoodSheetMode.DiaryEntryEditor,
                message = null,
                editingDiaryEntryId = entry.id,
                editingDiaryEntryName = entry.name,
                editingDiaryEntryMealType = section.id,
                editingDiaryEntryQuantityGrams = entry.quantityGrams.formatInputNumber(),
                editingDiaryEntryCaloriesKcal = entry.caloriesKcal,
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
        mutableState.update { it.copy(editingDiaryEntryQuantityGrams = value.sanitizeDecimalInput(), message = null) }
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
                        date = selectedDate,
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
        if (servingGrams == null || nutrition == null) {
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
        mutableState.update { it.copy(caloriesPer100g = value, message = null) }
    }

    fun onProteinChanged(value: String) {
        mutableState.update { it.copy(proteinPer100g = value, message = null) }
    }

    fun onCarbsChanged(value: String) {
        mutableState.update { it.copy(carbsPer100g = value, message = null) }
    }

    fun onFatChanged(value: String) {
        mutableState.update { it.copy(fatPer100g = value, message = null) }
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
        mutableState.update { it.copy(quantityGrams = value.sanitizeDecimalInput(), message = null) }
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
                                lookupResult = result,
                                message = null,
                            )
                        }
                    }

                is ProductLookupResult.NotFound ->
                    mutableState.update { currentState ->
                        if (currentState.barcode != barcode) {
                            currentState.copy(isLoading = false)
                        } else {
                            currentState.clearedEditableFields().copy(
                                isLoading = false,
                                message = "Product not found",
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
                        servingGrams = currentState.lookupResult?.servingQuantityGrams ?: quantityGrams,
                        mealType = currentState.mealType,
                        quantityGrams = quantityGrams,
                        date = selectedDate,
                    ),
                )
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        isAddPanelVisible = false,
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
                        date = selectedDate,
                    ),
                )
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        isAddPanelVisible = false,
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
                        date = selectedDate,
                    ),
                )
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        isAddPanelVisible = false,
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

    private fun FoodUiState.clearedEditableFields(): FoodUiState =
        copy(
            productName = "",
            brand = "",
            caloriesPer100g = "",
            proteinPer100g = "",
            carbsPer100g = "",
            fatPer100g = "",
            lookupResult = null,
        )
}

private data class MealDefinition(
    val id: String,
    val title: String,
    val recommendation: String,
)

private val mealDefinitions =
    listOf(
        MealDefinition("breakfast", "Breakfast", "Recommended 417 - 625 kcal"),
        MealDefinition("lunch", "Lunch", "Recommended 625 - 833 kcal"),
        MealDefinition("dinner", "Dinner", "Recommended 625 - 833 kcal"),
        MealDefinition("snacks", "Snacks", "Recommended 104 - 208 kcal"),
    )

private const val CALORIE_GOAL_KCAL = 2083.0
private const val CARBS_GOAL_GRAMS = 260.0
private const val PROTEIN_GOAL_GRAMS = 104.0
private const val FAT_GOAL_GRAMS = 69.0

private fun FoodUiState.withDiary(diary: FoodDiary): FoodUiState =
    copy(
        eatenCaloriesKcal = diary.totals.caloriesKcal,
        remainingCaloriesKcal = calorieGoalKcal - diary.totals.caloriesKcal,
        macroProgress = diary.totals.toMacroProgress(),
        mealSections = diary.toMealSections(),
    )

private fun FoodDiary.toMealSections(): List<FoodMealSectionUiState> {
    val mealsByType = meals.associateBy { it.type.normalizedMealType() }
    return mealDefinitions.map { definition ->
        val meal = mealsByType[definition.id]
        FoodMealSectionUiState(
            id = definition.id,
            title = definition.title,
            recommendation = definition.recommendation,
            caloriesKcal = meal?.totals?.caloriesKcal ?: 0.0,
            proteinGrams = meal?.totals?.proteinGrams ?: 0.0,
            carbsGrams = meal?.totals?.carbsGrams ?: 0.0,
            fatGrams = meal?.totals?.fatGrams ?: 0.0,
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
                )
            },
        )
    }
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
    )
}

private fun NutritionTotals.toMacroProgress(): List<FoodMacroProgressUiState> =
    listOf(
        FoodMacroProgressUiState("Carbs", carbsGrams, CARBS_GOAL_GRAMS),
        FoodMacroProgressUiState("Protein", proteinGrams, PROTEIN_GOAL_GRAMS),
        FoodMacroProgressUiState("Fat", fatGrams, FAT_GOAL_GRAMS),
    )

private fun emptyMealSections(): List<FoodMealSectionUiState> =
    mealDefinitions.map { definition ->
        FoodMealSectionUiState(
            id = definition.id,
            title = definition.title,
            recommendation = definition.recommendation,
            caloriesKcal = 0.0,
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
