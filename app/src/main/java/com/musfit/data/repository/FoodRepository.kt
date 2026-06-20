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
import java.time.LocalDate
import java.util.Locale
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

data class SavedFoodLogInput(
    val foodId: String,
    val mealType: String,
    val quantityGrams: Double,
    val date: LocalDate,
)

data class QuickCalorieLogInput(
    val mealType: String,
    val caloriesKcal: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val date: LocalDate,
)

data class SavedFoodItem(
    val id: String,
    val name: String,
    val brand: String?,
    val defaultServingGrams: Double,
    val nutritionPer100g: FoodNutrition,
)

data class FoodDiaryEntry(
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

data class FoodDiaryMeal(
    val type: String,
    val entries: List<FoodDiaryEntry>,
    val totals: NutritionTotals,
)

data class FoodDiary(
    val totals: NutritionTotals,
    val meals: List<FoodDiaryMeal>,
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

    fun observeFoodDiary(date: LocalDate): Flow<FoodDiary>

    fun observeSavedFoods(): Flow<List<SavedFoodItem>>

    suspend fun logSavedFood(input: SavedFoodLogInput): String

    suspend fun quickLog(input: QuickCalorieLogInput): String
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

    override fun observeFoodDiary(date: LocalDate): Flow<FoodDiary> =
        foodDao.observeFoodDiaryEntryRowsForDate(date.toEpochDay()).map { rows ->
            val entriesByMeal = rows.groupBy { it.mealType }
            val meals = entriesByMeal.map { (mealType, mealRows) ->
                val entries = mealRows.map { row -> row.toDiaryEntry() }
                FoodDiaryMeal(
                    type = mealType,
                    entries = entries,
                    totals = entries.calculateTotals(),
                )
            }

            FoodDiary(
                totals = meals.flatMap { it.entries }.calculateTotals(),
                meals = meals,
            )
        }

    override fun observeSavedFoods(): Flow<List<SavedFoodItem>> =
        foodDao.observeFoods().map { foods ->
            foods.filterNot { food -> food.name == QUICK_CALORIES_NAME && food.brand == null }.map { food ->
                SavedFoodItem(
                    id = food.id,
                    name = food.name,
                    brand = food.brand,
                    defaultServingGrams = food.defaultServingGrams,
                    nutritionPer100g = FoodNutrition(
                        caloriesKcal = food.caloriesPer100g,
                        proteinGrams = food.proteinPer100g,
                        carbsGrams = food.carbsPer100g,
                        fatGrams = food.fatPer100g,
                    ),
                )
            }
        }

    override suspend fun logSavedFood(input: SavedFoodLogInput): String {
        input.requireValid()
        return database.withTransaction {
            foodDao.getFood(input.foodId) ?: error("Saved food not found")
            insertMealItem(
                foodId = input.foodId,
                mealType = input.mealType,
                quantityGrams = input.quantityGrams,
                date = input.date,
                now = System.currentTimeMillis(),
            )
        }
    }

    override suspend fun quickLog(input: QuickCalorieLogInput): String {
        input.requireValid()
        return logFood(
            FoodLogInput(
                lookupResult = null,
                barcode = null,
                name = QUICK_CALORIES_NAME,
                brand = null,
                nutritionPer100g = FoodNutrition(
                    caloriesKcal = input.caloriesKcal,
                    proteinGrams = input.proteinGrams,
                    carbsGrams = input.carbsGrams,
                    fatGrams = input.fatGrams,
                ),
                servingGrams = 100.0,
                mealType = input.mealType,
                quantityGrams = 100.0,
                date = input.date,
            ),
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

    private suspend fun insertMealItem(
        foodId: String,
        mealType: String,
        quantityGrams: Double,
        date: LocalDate,
        now: Long,
    ): String {
        val mealId = UUID.randomUUID().toString()
        val mealItemId = UUID.randomUUID().toString()

        foodDao.upsertMeal(
            MealEntity(
                id = mealId,
                dateEpochDay = date.toEpochDay(),
                type = mealType.trim().ifBlank { DEFAULT_MEAL_TYPE },
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
                quantityGrams = quantityGrams,
            ),
        )

        return mealItemId
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
        const val QUICK_CALORIES_NAME = "Quick calories"
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

private fun SavedFoodLogInput.requireValid() {
    require(foodId.isNotBlank()) { "Food id is required" }
    require(mealType.isNotBlank()) { "Meal type is required" }
    require(quantityGrams.isFinite() && quantityGrams > 0.0) { "Quantity must be positive" }
}

private fun QuickCalorieLogInput.requireValid() {
    require(mealType.isNotBlank()) { "Meal type is required" }
    require(caloriesKcal.isNonNegativeFinite())
    require(proteinGrams.isNonNegativeFinite())
    require(carbsGrams.isNonNegativeFinite())
    require(fatGrams.isNonNegativeFinite())
}

private fun Double.isNonNegativeFinite(): Boolean = isFinite() && this >= 0.0

private fun com.musfit.data.local.dao.FoodDiaryEntryRow.toDiaryEntry(): FoodDiaryEntry {
    val multiplier = quantityGrams / 100.0
    return FoodDiaryEntry(
        id = mealItemId,
        foodId = foodId,
        name = foodName,
        brand = brand,
        quantityGrams = quantityGrams,
        caloriesKcal = caloriesPer100g * multiplier,
        proteinGrams = proteinPer100g * multiplier,
        carbsGrams = carbsPer100g * multiplier,
        fatGrams = fatPer100g * multiplier,
    )
}

private fun List<FoodDiaryEntry>.calculateTotals(): NutritionTotals =
    NutritionTotals(
        caloriesKcal = sumOf { it.caloriesKcal },
        proteinGrams = sumOf { it.proteinGrams },
        carbsGrams = sumOf { it.carbsGrams },
        fatGrams = sumOf { it.fatGrams },
    )
