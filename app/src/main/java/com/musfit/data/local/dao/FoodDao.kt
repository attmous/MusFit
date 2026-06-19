package com.musfit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musfit.data.local.entity.BarcodeProductEntity
import com.musfit.data.local.entity.FoodEntity
import com.musfit.data.local.entity.FoodServingEntity
import com.musfit.data.local.entity.MealEntity
import com.musfit.data.local.entity.MealItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM foods ORDER BY name")
    fun observeFoods(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM meals WHERE dateEpochDay = :dateEpochDay ORDER BY createdAtEpochMillis")
    fun observeMealsForDate(dateEpochDay: Long): Flow<List<MealEntity>>

    @Query("SELECT * FROM meal_items WHERE mealId = :mealId")
    fun observeMealItems(mealId: String): Flow<List<MealItemEntity>>

    @Query("SELECT * FROM barcode_products WHERE barcode = :barcode LIMIT 1")
    suspend fun getBarcodeProduct(barcode: String): BarcodeProductEntity?

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
