package com.musfit.data.repository

import androidx.room.withTransaction
import com.musfit.data.local.MusFitDatabase
import com.musfit.data.local.dao.FoodDao
import com.musfit.data.local.entity.BarcodeProductEntity
import com.musfit.data.local.entity.FoodEntity
import com.musfit.data.local.entity.FoodServingEntity
import com.musfit.data.remote.food.ProductDataQuality
import com.musfit.data.remote.food.ProductLookupResult
import com.musfit.domain.model.FoodNutrition
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

interface FoodRepository {
    suspend fun saveConfirmedProduct(
        result: ProductLookupResult.Found,
        editedName: String,
        editedBrand: String?,
        editedNutrition: FoodNutrition,
    ): String
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
            val existingBarcodeProduct = foodDao.getBarcodeProduct(result.barcode)
            val existingFood = existingBarcodeProduct?.linkedFoodId?.let { linkedFoodId ->
                foodDao.getFood(linkedFoodId)
            }
            val now = System.currentTimeMillis()
            val servingGrams = result.servingQuantityGrams ?: 100.0
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

            foodId
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
    }
}
