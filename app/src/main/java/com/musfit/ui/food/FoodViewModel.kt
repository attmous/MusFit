package com.musfit.ui.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musfit.data.remote.food.FoodProductProvider
import com.musfit.data.remote.food.ProductLookupResult
import com.musfit.data.repository.FoodLogInput
import com.musfit.data.repository.FoodRepository
import com.musfit.domain.model.FoodNutrition
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
)

@HiltViewModel
class FoodViewModel @Inject constructor(
    private val provider: FoodProductProvider,
    private val repository: FoodRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(FoodUiState())
    val state: StateFlow<FoodUiState> = mutableState.asStateFlow()
    private var lookupJob: Job? = null

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
        mutableState.update { it.copy(mealType = value, message = null) }
    }

    fun onQuantityChanged(value: String) {
        mutableState.update { it.copy(quantityGrams = value.sanitizeDecimalInput(), message = null) }
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

        var saveStarted = false
        mutableState.update {
            if (it.isSaving) {
                it
            } else {
                saveStarted = true
                it.copy(isSaving = true, message = null)
            }
        }
        if (!saveStarted) {
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
                        date = LocalDate.now(),
                    ),
                )
                mutableState.update {
                    it.copy(
                        isSaving = false,
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
