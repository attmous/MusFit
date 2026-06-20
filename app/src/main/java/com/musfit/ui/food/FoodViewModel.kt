package com.musfit.ui.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musfit.data.remote.food.FoodProductProvider
import com.musfit.data.remote.food.ProductLookupResult
import com.musfit.data.repository.FoodRepository
import com.musfit.domain.model.FoodNutrition
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FoodUiState(
    val barcode: String = "",
    val isLoading: Boolean = false,
    val message: String? = null,
    val productName: String = "",
    val brand: String = "",
    val caloriesPer100g: Double = 0.0,
    val proteinPer100g: Double = 0.0,
    val carbsPer100g: Double = 0.0,
    val fatPer100g: Double = 0.0,
    val lookupResult: ProductLookupResult.Found? = null,
)

@HiltViewModel
class FoodViewModel @Inject constructor(
    private val provider: FoodProductProvider,
    private val repository: FoodRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(FoodUiState())
    val state: StateFlow<FoodUiState> = mutableState.asStateFlow()

    fun onBarcodeChanged(value: String) {
        mutableState.update {
            it.copy(
                barcode = value.filter(Char::isDigit),
                message = null,
            )
        }
    }

    fun onProductNameChanged(value: String) {
        mutableState.update { it.copy(productName = value, message = null) }
    }

    fun onBrandChanged(value: String) {
        mutableState.update { it.copy(brand = value, message = null) }
    }

    fun onCaloriesChanged(value: String) {
        mutableState.update { it.copy(caloriesPer100g = value.toDoubleOrNull() ?: 0.0, message = null) }
    }

    fun onProteinChanged(value: String) {
        mutableState.update { it.copy(proteinPer100g = value.toDoubleOrNull() ?: 0.0, message = null) }
    }

    fun onCarbsChanged(value: String) {
        mutableState.update { it.copy(carbsPer100g = value.toDoubleOrNull() ?: 0.0, message = null) }
    }

    fun onFatChanged(value: String) {
        mutableState.update { it.copy(fatPer100g = value.toDoubleOrNull() ?: 0.0, message = null) }
    }

    fun lookupBarcode() {
        val barcode = state.value.barcode
        if (barcode.isBlank()) {
            mutableState.update { it.copy(message = "Enter a barcode") }
            return
        }

        viewModelScope.launch {
            mutableState.update {
                it.copy(
                    isLoading = true,
                    message = null,
                    lookupResult = null,
                )
            }

            when (val result = provider.lookupBarcode(barcode)) {
                is ProductLookupResult.Found ->
                    mutableState.update {
                        it.copy(
                            isLoading = false,
                            productName = result.name,
                            brand = result.brand.orEmpty(),
                            caloriesPer100g = result.nutritionPer100g.caloriesKcal,
                            proteinPer100g = result.nutritionPer100g.proteinGrams,
                            carbsPer100g = result.nutritionPer100g.carbsGrams,
                            fatPer100g = result.nutritionPer100g.fatGrams,
                            lookupResult = result,
                            message = null,
                        )
                    }

                is ProductLookupResult.NotFound ->
                    mutableState.update {
                        it.copy(
                            isLoading = false,
                            lookupResult = null,
                            message = "Product not found",
                        )
                    }

                is ProductLookupResult.Failed ->
                    mutableState.update {
                        it.copy(
                            isLoading = false,
                            lookupResult = null,
                            message = result.message,
                        )
                    }
            }
        }
    }

    fun saveProduct() {
        val currentState = state.value
        val result = currentState.lookupResult
        if (result == null) {
            mutableState.update { it.copy(message = "Look up a product before saving") }
            return
        }

        viewModelScope.launch {
            repository.saveConfirmedProduct(
                result = result,
                editedName = currentState.productName,
                editedBrand = currentState.brand.ifBlank { null },
                editedNutrition = currentState.toEditedNutrition(),
            )
            mutableState.update {
                it.copy(
                    message = "Saved food",
                    lookupResult = null,
                )
            }
        }
    }

    private fun FoodUiState.toEditedNutrition(): FoodNutrition =
        FoodNutrition(
            caloriesKcal = caloriesPer100g,
            proteinGrams = proteinPer100g,
            carbsGrams = carbsPer100g,
            fatGrams = fatPer100g,
        )
}
