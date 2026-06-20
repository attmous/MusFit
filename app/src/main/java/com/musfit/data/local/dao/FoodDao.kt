package com.musfit.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musfit.data.local.entity.BarcodeProductEntity
import com.musfit.data.local.entity.FoodEntity
import com.musfit.data.local.entity.FoodServingEntity
import com.musfit.data.local.entity.MealEntity
import com.musfit.data.local.entity.MealItemEntity
import kotlinx.coroutines.flow.Flow

data class MealNutritionRow(
    val quantityGrams: Double,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
)

@Dao
interface FoodDao {
    @Query("SELECT * FROM foods ORDER BY name")
    fun observeFoods(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM food_servings WHERE foodId = :foodId ORDER BY label")
    fun observeServings(foodId: String): Flow<List<FoodServingEntity>>

    @Query("SELECT * FROM food_servings WHERE foodId = :foodId ORDER BY label")
    suspend fun getServings(foodId: String): List<FoodServingEntity>

    @Query("SELECT * FROM foods WHERE id = :foodId LIMIT 1")
    suspend fun getFood(foodId: String): FoodEntity?

    @Query("SELECT * FROM meals WHERE dateEpochDay = :dateEpochDay ORDER BY createdAtEpochMillis")
    fun observeMealsForDate(dateEpochDay: Long): Flow<List<MealEntity>>

    @Query("SELECT * FROM meal_items WHERE mealId = :mealId")
    fun observeMealItems(mealId: String): Flow<List<MealItemEntity>>

    @Query(
        "SELECT meal_items.quantityGrams AS quantityGrams, " +
            "foods.caloriesPer100g AS caloriesPer100g, " +
            "foods.proteinPer100g AS proteinPer100g, " +
            "foods.carbsPer100g AS carbsPer100g, " +
            "foods.fatPer100g AS fatPer100g " +
            "FROM meal_items " +
            "INNER JOIN meals ON meals.id = meal_items.mealId " +
            "INNER JOIN foods ON foods.id = meal_items.foodId " +
            "WHERE meals.dateEpochDay = :dateEpochDay",
    )
    fun observeMealNutritionRowsForDate(dateEpochDay: Long): Flow<List<MealNutritionRow>>

    @Query("SELECT * FROM barcode_products WHERE barcode = :barcode LIMIT 1")
    suspend fun getBarcodeProduct(barcode: String): BarcodeProductEntity?

    @Query("SELECT * FROM barcode_products ORDER BY fetchedAtEpochMillis DESC")
    fun observeBarcodeProducts(): Flow<List<BarcodeProductEntity>>

    @Query("SELECT * FROM barcode_products WHERE linkedFoodId = :foodId ORDER BY fetchedAtEpochMillis DESC")
    fun observeBarcodeProducts(foodId: String): Flow<List<BarcodeProductEntity>>

    @Delete
    suspend fun deleteFood(food: FoodEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFood(food: FoodEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertServing(serving: FoodServingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeal(meal: MealEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMealItem(item: MealItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBarcodeProduct(product: BarcodeProductEntity)
}
