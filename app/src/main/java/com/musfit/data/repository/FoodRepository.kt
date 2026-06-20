package com.musfit.data.repository

import androidx.room.withTransaction
import com.musfit.data.local.MusFitDatabase
import com.musfit.data.local.dao.FoodDao
import com.musfit.data.local.entity.BarcodeProductEntity
import com.musfit.data.local.entity.FoodEntity
import com.musfit.data.local.entity.FoodServingEntity
import com.musfit.data.local.entity.MealEntity
import com.musfit.data.local.entity.MealItemEntity
import com.musfit.data.remote.food.ProductDataQuality
import com.musfit.data.remote.food.ProductLookupResult
import com.musfit.domain.model.FoodNutrition
import com.musfit.domain.model.MealItemInput
import com.musfit.domain.model.NutritionTotals
import com.musfit.domain.nutrition.NutritionCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class FoodLogInput(
    val lookupResult: ProductLookupResult.Found?,
    val barcode: String?,
    val name: String,
    val brand: String?,
    val nutritionPer100g: FoodNutrition,
    val servingGrams: Double?,
    val mealType: String,
    val quantityGrams: Double,
    val date: LocalDate,
)

interface FoodRepository {
    suspend fun saveConfirmedProduct(
        result: ProductLookupResult.Found,
        editedName: String,
        editedBrand: String?,
        editedNutrition: FoodNutrition,
    ): String

    suspend fun logFood(input: FoodLogInput): String

    fun observeDailyNutrition(date: LocalDate): Flow<NutritionTotals>
}

class LocalFoodRepository @Inject constructor(
    private val database: MusFitDatabase,
    private val foodDao: FoodDao,
) : FoodRepository {
    override suspend fun saveConfirmedProduct(
        result: ProductLookupResult.Found,
        editedName: String,
        editedBrand: String?,
        editedNutrition: FoodNutrition,
    ): String =
        database.withTransaction {
            upsertConfirmedFood(
                result = result,
                editedName = editedName,
                editedBrand = editedBrand,
                editedNutrition = editedNutrition,
                servingGrams = result.servingQuantityGrams ?: 100.0,
                now = System.currentTimeMillis(),
            )
        }

    override suspend fun logFood(input: FoodLogInput): String {
        input.requireValid()
        return database.withTransaction {
            val now = System.currentTimeMillis()
            val foodId = input.lookupResult?.let { result ->
                upsertConfirmedFood(
                    result = result,
                    editedName = input.name,
                    editedBrand = input.brand,
                    editedNutrition = input.nutritionPer100g,
                    servingGrams = input.servingGrams ?: result.servingQuantityGrams ?: 100.0,
                    now = now,
                )
            } ?: upsertManualFood(input, now)
            val mealId = UUID.randomUUID().toString()
            val mealItemId = UUID.randomUUID().toString()

            foodDao.upsertMeal(
                MealEntity(
                    id = mealId,
                    dateEpochDay = input.date.toEpochDay(),
                    type = input.mealType.trim().ifBlank { DEFAULT_MEAL_TYPE },
                    notes = null,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                ),
            )
            foodDao.upsertMealItem(
                MealItemEntity(
                    id = mealItemId,
                    mealId = mealId,
                    foodId = foodId,
                    quantityGrams = input.quantityGrams,
                ),
            )

            mealItemId
        }
    }

    override fun observeDailyNutrition(date: LocalDate): Flow<NutritionTotals> =
        foodDao.observeMealNutritionRowsForDate(date.toEpochDay()).map { rows ->
            NutritionCalculator.calculateMealTotals(
                rows.map { row ->
                    MealItemInput(
                        foodId = "",
                        quantityGrams = row.quantityGrams,
                        nutritionPer100g = FoodNutrition(
                            caloriesKcal = row.caloriesPer100g,
                            proteinGrams = row.proteinPer100g,
                            carbsGrams = row.carbsPer100g,
                            fatGrams = row.fatPer100g,
                        ),
                    )
                },
            )
        }

    private suspend fun upsertManualFood(input: FoodLogInput, now: Long): String {
        val foodId = UUID.randomUUID().toString()
        val servingGrams = input.servingGrams ?: input.quantityGrams
        val resolvedBrand = input.brand?.trim()?.takeIf { it.isNotEmpty() }

        foodDao.upsertFood(
            FoodEntity(
                id = foodId,
                name = input.name.trim(),
                brand = resolvedBrand,
                defaultServingGrams = servingGrams,
                caloriesPer100g = input.nutritionPer100g.caloriesKcal,
                proteinPer100g = input.nutritionPer100g.proteinGrams,
                carbsPer100g = input.nutritionPer100g.carbsGrams,
                fatPer100g = input.nutritionPer100g.fatGrams,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
            ),
        )
        foodDao.upsertServing(
            FoodServingEntity(
                id = defaultServingId(foodId),
                foodId = foodId,
                label = servingLabel(servingGrams),
                grams = servingGrams,
            ),
        )
        return foodId
    }

    private suspend fun upsertConfirmedFood(
        result: ProductLookupResult.Found,
        editedName: String,
        editedBrand: String?,
        editedNutrition: FoodNutrition,
        servingGrams: Double,
        now: Long,
    ): String {
        val existingBarcodeProduct = foodDao.getBarcodeProduct(result.barcode)
        val existingFood = existingBarcodeProduct?.linkedFoodId?.let { linkedFoodId ->
            foodDao.getFood(linkedFoodId)
        }
        val resolvedName = editedName.ifBlank { result.name }
        val resolvedBrand = editedBrand?.trim()?.takeIf { it.isNotEmpty() }
        val shouldReuseExistingFood =
            existingFood?.matchesLocalSnapshot(
                name = resolvedName,
                brand = resolvedBrand,
                servingGrams = servingGrams,
                nutrition = editedNutrition,
            ) == true
        val foodId = if (shouldReuseExistingFood) existingFood.id else UUID.randomUUID().toString()

        foodDao.upsertFood(
            FoodEntity(
                id = foodId,
                name = resolvedName,
                brand = resolvedBrand,
                defaultServingGrams = servingGrams,
                caloriesPer100g = editedNutrition.caloriesKcal,
                proteinPer100g = editedNutrition.proteinGrams,
                carbsPer100g = editedNutrition.carbsGrams,
                fatPer100g = editedNutrition.fatGrams,
                createdAtEpochMillis = existingFood?.takeIf { shouldReuseExistingFood }?.createdAtEpochMillis ?: now,
                updatedAtEpochMillis = now,
            ),
        )
        foodDao.upsertServing(
            FoodServingEntity(
                id = defaultServingId(foodId),
                foodId = foodId,
                label = servingLabel(servingGrams),
                grams = servingGrams,
            ),
        )
        foodDao.upsertBarcodeProduct(
            BarcodeProductEntity(
                id = existingBarcodeProduct?.id ?: UUID.randomUUID().toString(),
                barcode = result.barcode,
                provider = OPEN_FOOD_FACTS_PROVIDER,
                providerProductName = result.name,
                providerBrand = result.brand,
                rawJson = result.rawJson,
                quality = result.quality.asStorageValue(),
                linkedFoodId = foodId,
                fetchedAtEpochMillis = now,
            ),
        )

        return foodId
    }

    private fun servingLabel(servingGrams: Double): String {
        val rounded = servingGrams.toLong()
        return if (servingGrams == rounded.toDouble()) {
            "$rounded g"
        } else {
            "${String.format(Locale.US, "%.1f", servingGrams)} g"
        }
    }

    private fun ProductDataQuality.asStorageValue(): String =
        when (this) {
            ProductDataQuality.Complete -> "complete"
            ProductDataQuality.Incomplete -> "incomplete"
        }

    private fun defaultServingId(foodId: String): String = "$foodId:default-serving"

    private fun FoodEntity.matchesLocalSnapshot(
        name: String,
        brand: String?,
        servingGrams: Double,
        nutrition: FoodNutrition,
    ): Boolean =
        this.name == name &&
            this.brand == brand &&
            defaultServingGrams == servingGrams &&
            caloriesPer100g == nutrition.caloriesKcal &&
            proteinPer100g == nutrition.proteinGrams &&
            carbsPer100g == nutrition.carbsGrams &&
            fatPer100g == nutrition.fatGrams

    private companion object {
        const val OPEN_FOOD_FACTS_PROVIDER = "open_food_facts"
        const val DEFAULT_MEAL_TYPE = "meal"
    }
}

private fun FoodLogInput.requireValid() {
    require(name.isNotBlank()) { "Food name is required" }
    require(mealType.isNotBlank()) { "Meal type is required" }
    require(quantityGrams.isFinite() && quantityGrams > 0.0) { "Quantity must be positive" }
    servingGrams?.let {
        require(it.isFinite() && it > 0.0) { "Serving size must be positive" }
    }
    require(nutritionPer100g.caloriesKcal.isNonNegativeFinite())
    require(nutritionPer100g.proteinGrams.isNonNegativeFinite())
    require(nutritionPer100g.carbsGrams.isNonNegativeFinite())
    require(nutritionPer100g.fatGrams.isNonNegativeFinite())
}

private fun Double.isNonNegativeFinite(): Boolean = isFinite() && this >= 0.0
