package com.musfit.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Upsert
import com.musfit.data.local.entity.BarcodeProductEntity
import com.musfit.data.local.entity.FoodEntity
import com.musfit.data.local.entity.FoodGoalEntity
import com.musfit.data.local.entity.FoodHealthConnectSyncEntity
import com.musfit.data.local.entity.FoodServingEntity
import com.musfit.data.local.entity.MealDefinitionEntity
import com.musfit.data.local.entity.MealEntity
import com.musfit.data.local.entity.MealItemEntity
import com.musfit.data.local.entity.MealTemplateEntity
import com.musfit.data.local.entity.MealTemplateItemEntity
import com.musfit.data.local.entity.QuickCaloriePresetEntity
import com.musfit.data.local.entity.RecipeEntity
import com.musfit.data.local.entity.RecipeIngredientEntity
import com.musfit.data.local.entity.ShoppingListItemEntity
import com.musfit.data.local.entity.WaterEntryEntity
import kotlinx.coroutines.flow.Flow

data class MealNutritionRow(
    val quantityGrams: Double,
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
)

data class FoodDiaryEntryRow(
    val mealId: String,
    val dateEpochDay: Long,
    val mealType: String,
    val mealItemId: String,
    val foodId: String,
    val foodName: String,
    val brand: String?,
    val foodCategory: String?,
    val imageUrl: String?,
    val quantityGrams: Double,
    val status: String,
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
    val createdAtEpochMillis: Long,
)

data class WaterTotalRow(
    val dateEpochDay: Long,
    val consumedMilliliters: Double,
)

data class FoodWithServingRow(
    @Embedded val food: FoodEntity,
    val servingId: String?,
    val servingLabel: String?,
    val servingGrams: Double?,
)

data class MealTemplateItemRow(
    val templateId: String,
    val templateName: String,
    val templateMealType: String,
    val templateCreatedAtEpochMillis: Long,
    val templateIsFavorite: Boolean,
    val itemId: String,
    val foodId: String,
    val foodName: String,
    val brand: String?,
    val quantityGrams: Double,
    val sortOrder: Int,
)

data class RecipeIngredientRow(
    val recipeId: String,
    val recipeName: String,
    val recipeCategory: String?,
    val recipeServingName: String,
    val recipeServingGrams: Double,
    val recipeServings: Double,
    val recipeCookedYieldGrams: Double,
    val recipeCreatedAtEpochMillis: Long,
    val recipeIsFavorite: Boolean,
    val ingredientId: String,
    val foodId: String,
    val foodName: String,
    val brand: String?,
    val foodCategory: String?,
    val quantityGrams: Double,
    val unitLabel: String,
    val unitGrams: Double,
    val unitQuantity: Double,
    val sortOrder: Int,
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
)

@Dao
interface FoodDao {
    @Query(
        "SELECT foods.* FROM foods " +
            "INNER JOIN meal_items ON meal_items.accountId = foods.accountId AND meal_items.foodId = foods.id " +
            "INNER JOIN meals ON meals.accountId = meal_items.accountId AND meals.id = meal_items.mealId " +
            "WHERE foods.accountId = :accountId " +
            "GROUP BY foods.accountId, foods.id " +
            "ORDER BY MAX(meals.createdAtEpochMillis) DESC " +
            "LIMIT :limit",
    )
    fun observeRecentFoods(accountId: String, limit: Int): Flow<List<FoodEntity>>

    @Query(
        "SELECT foods.* FROM foods " +
            "INNER JOIN meal_items ON meal_items.accountId = foods.accountId AND meal_items.foodId = foods.id " +
            "INNER JOIN meals ON meals.accountId = meal_items.accountId AND meals.id = meal_items.mealId " +
            "WHERE foods.accountId = :accountId AND meals.dateEpochDay = :dateEpochDay AND meals.type = :mealType " +
            "GROUP BY foods.accountId, foods.id " +
            "ORDER BY MAX(meals.createdAtEpochMillis) DESC",
    )
    fun observeSameAsYesterday(accountId: String, dateEpochDay: Long, mealType: String): Flow<List<FoodEntity>>

    @Query(
        "SELECT * FROM foods WHERE accountId = :accountId " +
            "ORDER BY name COLLATE NOCASE, brand COLLATE NOCASE, id",
    )
    fun observeFoods(accountId: String): Flow<List<FoodEntity>>

    @Query(
        "SELECT foods.*, food_servings.id AS servingId, food_servings.label AS servingLabel, " +
            "food_servings.grams AS servingGrams FROM foods " +
            "LEFT JOIN food_servings ON food_servings.accountId = foods.accountId AND food_servings.foodId = foods.id " +
            "WHERE foods.accountId = :accountId " +
            "ORDER BY foods.name COLLATE NOCASE, foods.brand COLLATE NOCASE, foods.id, " +
            "food_servings.label, food_servings.id",
    )
    fun observeFoodsWithServings(accountId: String): Flow<List<FoodWithServingRow>>

    @Query("SELECT * FROM food_servings WHERE accountId = :accountId AND foodId = :foodId ORDER BY label")
    fun observeServings(accountId: String, foodId: String): Flow<List<FoodServingEntity>>

    @Query("SELECT * FROM food_servings WHERE accountId = :accountId AND foodId = :foodId ORDER BY label")
    suspend fun getServings(accountId: String, foodId: String): List<FoodServingEntity>

    @Query("SELECT * FROM foods WHERE accountId = :accountId AND id = :foodId LIMIT 1")
    suspend fun getFood(accountId: String, foodId: String): FoodEntity?

    @Query(
        "SELECT foods.*, food_servings.id AS servingId, food_servings.label AS servingLabel, " +
            "food_servings.grams AS servingGrams FROM foods " +
            "LEFT JOIN food_servings ON food_servings.accountId = foods.accountId AND food_servings.foodId = foods.id " +
            "WHERE foods.accountId = :accountId AND foods.id = :foodId " +
            "ORDER BY food_servings.label, food_servings.id",
    )
    suspend fun getFoodWithServings(accountId: String, foodId: String): List<FoodWithServingRow>

    @Query("SELECT * FROM foods WHERE accountId = :accountId AND barcode = :barcode LIMIT 1")
    suspend fun getFoodByBarcode(accountId: String, barcode: String): FoodEntity?

    @Query(
        "SELECT * FROM foods WHERE accountId = :accountId AND name = :name COLLATE NOCASE " +
            "AND brand IS NULL LIMIT 1",
    )
    suspend fun getFoodByNameAndNullBrand(accountId: String, name: String): FoodEntity?

    @Query(
        "SELECT * FROM foods WHERE accountId = :accountId AND name = :name COLLATE NOCASE " +
            "AND brand = :brand COLLATE NOCASE LIMIT 1",
    )
    suspend fun getFoodByNameAndBrand(accountId: String, name: String, brand: String): FoodEntity?

    @Query("SELECT * FROM meals WHERE accountId = :accountId AND id = :mealId LIMIT 1")
    suspend fun getMeal(accountId: String, mealId: String): MealEntity?

    @Query("SELECT * FROM meal_items WHERE accountId = :accountId AND id = :mealItemId LIMIT 1")
    suspend fun getMealItem(accountId: String, mealItemId: String): MealItemEntity?

    @Query("SELECT COUNT(*) FROM meal_items WHERE accountId = :accountId AND foodId = :foodId")
    suspend fun countMealItemsForFood(accountId: String, foodId: String): Int

    @Query("SELECT * FROM meals WHERE accountId = :accountId AND dateEpochDay = :dateEpochDay ORDER BY createdAtEpochMillis")
    fun observeMealsForDate(accountId: String, dateEpochDay: Long): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE accountId = :accountId AND dateEpochDay = :dateEpochDay AND type = :mealType ORDER BY createdAtEpochMillis")
    suspend fun getMealsForDateAndType(accountId: String, dateEpochDay: Long, mealType: String): List<MealEntity>

    @Query(
        "SELECT DISTINCT meals.dateEpochDay FROM meals " +
            "INNER JOIN meal_items ON meal_items.accountId = meals.accountId AND meal_items.mealId = meals.id " +
            "WHERE meals.accountId = :accountId AND meal_items.status = 'logged' AND meals.dateEpochDay >= :fromEpochDay " +
            "ORDER BY meals.dateEpochDay DESC",
    )
    fun observeLoggedDayEpochDays(accountId: String, fromEpochDay: Long): Flow<List<Long>>

    @Query("SELECT * FROM meal_definitions WHERE accountId = :accountId ORDER BY sortOrder, name")
    fun observeMealDefinitions(accountId: String): Flow<List<MealDefinitionEntity>>

    @Query("SELECT * FROM meal_definitions WHERE accountId = :accountId AND id = :mealId LIMIT 1")
    suspend fun getMealDefinition(accountId: String, mealId: String): MealDefinitionEntity?

    @Upsert
    suspend fun upsertMealDefinition(definition: MealDefinitionEntity)

    @Query("SELECT * FROM meal_items WHERE accountId = :accountId AND mealId = :mealId")
    fun observeMealItems(accountId: String, mealId: String): Flow<List<MealItemEntity>>

    @Query(
        "SELECT meal_items.quantityGrams AS quantityGrams, " +
            "foods.caloriesPer100g AS caloriesPer100g, " +
            "foods.proteinPer100g AS proteinPer100g, " +
            "foods.carbsPer100g AS carbsPer100g, " +
            "foods.fatPer100g AS fatPer100g, " +
            "foods.fiberPer100g AS fiberPer100g, " +
            "foods.sugarPer100g AS sugarPer100g, " +
            "foods.saturatedFatPer100g AS saturatedFatPer100g, " +
            "foods.sodiumMgPer100g AS sodiumMgPer100g, " +
            "foods.potassiumMgPer100g AS potassiumMgPer100g, " +
            "foods.calciumMgPer100g AS calciumMgPer100g, " +
            "foods.ironMgPer100g AS ironMgPer100g, " +
            "foods.vitaminDMcgPer100g AS vitaminDMcgPer100g, " +
            "foods.vitaminCMgPer100g AS vitaminCMgPer100g, " +
            "foods.magnesiumMgPer100g AS magnesiumMgPer100g " +
            "FROM meal_items " +
            "INNER JOIN meals ON meals.accountId = meal_items.accountId AND meals.id = meal_items.mealId " +
            "INNER JOIN foods ON foods.accountId = meal_items.accountId AND foods.id = meal_items.foodId " +
            "WHERE meal_items.accountId = :accountId AND meals.dateEpochDay = :dateEpochDay AND meal_items.status = 'logged'",
    )
    fun observeMealNutritionRowsForDate(accountId: String, dateEpochDay: Long): Flow<List<MealNutritionRow>>

    @Query(
        "SELECT meals.id AS mealId, " +
            "meals.dateEpochDay AS dateEpochDay, " +
            "meals.type AS mealType, " +
            "meal_items.id AS mealItemId, " +
            "foods.id AS foodId, " +
            "foods.name AS foodName, " +
            "foods.brand AS brand, " +
            "foods.category AS foodCategory, " +
            "foods.imageUrl AS imageUrl, " +
            "meal_items.quantityGrams AS quantityGrams, " +
            "meal_items.status AS status, " +
            "foods.caloriesPer100g AS caloriesPer100g, " +
            "foods.proteinPer100g AS proteinPer100g, " +
            "foods.carbsPer100g AS carbsPer100g, " +
            "foods.fatPer100g AS fatPer100g, " +
            "foods.fiberPer100g AS fiberPer100g, " +
            "foods.sugarPer100g AS sugarPer100g, " +
            "foods.saturatedFatPer100g AS saturatedFatPer100g, " +
            "foods.sodiumMgPer100g AS sodiumMgPer100g, " +
            "foods.potassiumMgPer100g AS potassiumMgPer100g, " +
            "foods.calciumMgPer100g AS calciumMgPer100g, " +
            "foods.ironMgPer100g AS ironMgPer100g, " +
            "foods.vitaminDMcgPer100g AS vitaminDMcgPer100g, " +
            "foods.vitaminCMgPer100g AS vitaminCMgPer100g, " +
            "foods.magnesiumMgPer100g AS magnesiumMgPer100g, " +
            "meals.createdAtEpochMillis AS createdAtEpochMillis " +
            "FROM meal_items " +
            "INNER JOIN meals ON meals.accountId = meal_items.accountId AND meals.id = meal_items.mealId " +
            "INNER JOIN foods ON foods.accountId = meal_items.accountId AND foods.id = meal_items.foodId " +
            "WHERE meal_items.accountId = :accountId AND meals.dateEpochDay = :dateEpochDay " +
            "ORDER BY meals.createdAtEpochMillis, meals.type, meal_items.id",
    )
    fun observeFoodDiaryEntryRowsForDate(accountId: String, dateEpochDay: Long): Flow<List<FoodDiaryEntryRow>>

    @Query(
        "SELECT meals.id AS mealId, " +
            "meals.dateEpochDay AS dateEpochDay, " +
            "meals.type AS mealType, " +
            "meal_items.id AS mealItemId, " +
            "foods.id AS foodId, " +
            "foods.name AS foodName, " +
            "foods.brand AS brand, " +
            "foods.category AS foodCategory, " +
            "foods.imageUrl AS imageUrl, " +
            "meal_items.quantityGrams AS quantityGrams, " +
            "meal_items.status AS status, " +
            "foods.caloriesPer100g AS caloriesPer100g, " +
            "foods.proteinPer100g AS proteinPer100g, " +
            "foods.carbsPer100g AS carbsPer100g, " +
            "foods.fatPer100g AS fatPer100g, " +
            "foods.fiberPer100g AS fiberPer100g, " +
            "foods.sugarPer100g AS sugarPer100g, " +
            "foods.saturatedFatPer100g AS saturatedFatPer100g, " +
            "foods.sodiumMgPer100g AS sodiumMgPer100g, " +
            "foods.potassiumMgPer100g AS potassiumMgPer100g, " +
            "foods.calciumMgPer100g AS calciumMgPer100g, " +
            "foods.ironMgPer100g AS ironMgPer100g, " +
            "foods.vitaminDMcgPer100g AS vitaminDMcgPer100g, " +
            "foods.vitaminCMgPer100g AS vitaminCMgPer100g, " +
            "foods.magnesiumMgPer100g AS magnesiumMgPer100g, " +
            "meals.createdAtEpochMillis AS createdAtEpochMillis " +
            "FROM meal_items " +
            "INNER JOIN meals ON meals.accountId = meal_items.accountId AND meals.id = meal_items.mealId " +
            "INNER JOIN foods ON foods.accountId = meal_items.accountId AND foods.id = meal_items.foodId " +
            "WHERE meal_items.accountId = :accountId AND meals.dateEpochDay BETWEEN :startEpochDay AND :endEpochDay " +
            "ORDER BY meals.dateEpochDay, meals.createdAtEpochMillis, meals.type, meal_items.id",
    )
    fun observeFoodDiaryEntryRowsForDateRange(accountId: String, startEpochDay: Long, endEpochDay: Long): Flow<List<FoodDiaryEntryRow>>

    @Query(
        "SELECT meals.id AS mealId, " +
            "meals.dateEpochDay AS dateEpochDay, " +
            "meals.type AS mealType, " +
            "meal_items.id AS mealItemId, " +
            "foods.id AS foodId, " +
            "foods.name AS foodName, " +
            "foods.brand AS brand, " +
            "foods.category AS foodCategory, " +
            "foods.imageUrl AS imageUrl, " +
            "meal_items.quantityGrams AS quantityGrams, " +
            "meal_items.status AS status, " +
            "foods.caloriesPer100g AS caloriesPer100g, " +
            "foods.proteinPer100g AS proteinPer100g, " +
            "foods.carbsPer100g AS carbsPer100g, " +
            "foods.fatPer100g AS fatPer100g, " +
            "foods.fiberPer100g AS fiberPer100g, " +
            "foods.sugarPer100g AS sugarPer100g, " +
            "foods.saturatedFatPer100g AS saturatedFatPer100g, " +
            "foods.sodiumMgPer100g AS sodiumMgPer100g, " +
            "foods.potassiumMgPer100g AS potassiumMgPer100g, " +
            "foods.calciumMgPer100g AS calciumMgPer100g, " +
            "foods.ironMgPer100g AS ironMgPer100g, " +
            "foods.vitaminDMcgPer100g AS vitaminDMcgPer100g, " +
            "foods.vitaminCMgPer100g AS vitaminCMgPer100g, " +
            "foods.magnesiumMgPer100g AS magnesiumMgPer100g, " +
            "meals.createdAtEpochMillis AS createdAtEpochMillis " +
            "FROM meal_items " +
            "INNER JOIN meals ON meals.accountId = meal_items.accountId AND meals.id = meal_items.mealId " +
            "INNER JOIN foods ON foods.accountId = meal_items.accountId AND foods.id = meal_items.foodId " +
            "WHERE meal_items.accountId = :accountId AND meals.dateEpochDay BETWEEN :startEpochDay AND :endEpochDay " +
            "ORDER BY meals.dateEpochDay, meals.createdAtEpochMillis, meals.type, meal_items.id",
    )
    suspend fun getFoodDiaryEntryRowsForDateRange(accountId: String, startEpochDay: Long, endEpochDay: Long): List<FoodDiaryEntryRow>

    @Query(
        "SELECT meals.id AS mealId, " +
            "meals.dateEpochDay AS dateEpochDay, " +
            "meals.type AS mealType, " +
            "meal_items.id AS mealItemId, " +
            "foods.id AS foodId, " +
            "foods.name AS foodName, " +
            "foods.brand AS brand, " +
            "foods.category AS foodCategory, " +
            "foods.imageUrl AS imageUrl, " +
            "meal_items.quantityGrams AS quantityGrams, " +
            "meal_items.status AS status, " +
            "foods.caloriesPer100g AS caloriesPer100g, " +
            "foods.proteinPer100g AS proteinPer100g, " +
            "foods.carbsPer100g AS carbsPer100g, " +
            "foods.fatPer100g AS fatPer100g, " +
            "foods.fiberPer100g AS fiberPer100g, " +
            "foods.sugarPer100g AS sugarPer100g, " +
            "foods.saturatedFatPer100g AS saturatedFatPer100g, " +
            "foods.sodiumMgPer100g AS sodiumMgPer100g, " +
            "foods.potassiumMgPer100g AS potassiumMgPer100g, " +
            "foods.calciumMgPer100g AS calciumMgPer100g, " +
            "foods.ironMgPer100g AS ironMgPer100g, " +
            "foods.vitaminDMcgPer100g AS vitaminDMcgPer100g, " +
            "foods.vitaminCMgPer100g AS vitaminCMgPer100g, " +
            "foods.magnesiumMgPer100g AS magnesiumMgPer100g, " +
            "meals.createdAtEpochMillis AS createdAtEpochMillis " +
            "FROM meal_items " +
            "INNER JOIN meals ON meals.accountId = meal_items.accountId AND meals.id = meal_items.mealId " +
            "INNER JOIN foods ON foods.accountId = meal_items.accountId AND foods.id = meal_items.foodId " +
            "WHERE meal_items.accountId = :accountId AND meals.dateEpochDay = :dateEpochDay " +
            "ORDER BY meals.createdAtEpochMillis, meals.type, meal_items.id",
    )
    suspend fun getFoodDiaryEntryRowsForDate(accountId: String, dateEpochDay: Long): List<FoodDiaryEntryRow>

    @Query(
        "SELECT meals.id AS mealId, " +
            "meals.dateEpochDay AS dateEpochDay, " +
            "meals.type AS mealType, " +
            "meal_items.id AS mealItemId, " +
            "foods.id AS foodId, " +
            "foods.name AS foodName, " +
            "foods.brand AS brand, " +
            "foods.category AS foodCategory, " +
            "foods.imageUrl AS imageUrl, " +
            "meal_items.quantityGrams AS quantityGrams, " +
            "meal_items.status AS status, " +
            "foods.caloriesPer100g AS caloriesPer100g, " +
            "foods.proteinPer100g AS proteinPer100g, " +
            "foods.carbsPer100g AS carbsPer100g, " +
            "foods.fatPer100g AS fatPer100g, " +
            "foods.fiberPer100g AS fiberPer100g, " +
            "foods.sugarPer100g AS sugarPer100g, " +
            "foods.saturatedFatPer100g AS saturatedFatPer100g, " +
            "foods.sodiumMgPer100g AS sodiumMgPer100g, " +
            "foods.potassiumMgPer100g AS potassiumMgPer100g, " +
            "foods.calciumMgPer100g AS calciumMgPer100g, " +
            "foods.ironMgPer100g AS ironMgPer100g, " +
            "foods.vitaminDMcgPer100g AS vitaminDMcgPer100g, " +
            "foods.vitaminCMgPer100g AS vitaminCMgPer100g, " +
            "foods.magnesiumMgPer100g AS magnesiumMgPer100g, " +
            "meals.createdAtEpochMillis AS createdAtEpochMillis " +
            "FROM meal_items " +
            "INNER JOIN meals ON meals.accountId = meal_items.accountId AND meals.id = meal_items.mealId " +
            "INNER JOIN foods ON foods.accountId = meal_items.accountId AND foods.id = meal_items.foodId " +
            "WHERE meal_items.accountId = :accountId AND meals.dateEpochDay = :dateEpochDay AND meals.type = :mealType " +
            "ORDER BY meals.createdAtEpochMillis, meal_items.id",
    )
    suspend fun getFoodDiaryEntryRowsForDateAndMeal(accountId: String, dateEpochDay: Long, mealType: String): List<FoodDiaryEntryRow>

    @Query("SELECT * FROM barcode_products WHERE accountId = :accountId AND barcode = :barcode LIMIT 1")
    suspend fun getBarcodeProduct(accountId: String, barcode: String): BarcodeProductEntity?

    @Query("SELECT * FROM barcode_products WHERE accountId = :accountId ORDER BY fetchedAtEpochMillis DESC")
    fun observeBarcodeProducts(accountId: String): Flow<List<BarcodeProductEntity>>

    @Query("SELECT * FROM barcode_products WHERE accountId = :accountId AND linkedFoodId = :foodId ORDER BY fetchedAtEpochMillis DESC")
    fun observeBarcodeProducts(accountId: String, foodId: String): Flow<List<BarcodeProductEntity>>

    @Query("SELECT * FROM food_goals WHERE accountId = :accountId AND id = :id LIMIT 1")
    fun observeFoodGoal(accountId: String, id: String): Flow<FoodGoalEntity?>

    @Query("SELECT * FROM food_goals WHERE accountId = :accountId AND id = :id LIMIT 1")
    suspend fun getFoodGoal(accountId: String, id: String): FoodGoalEntity?

    @Upsert
    suspend fun upsertFoodGoal(goal: FoodGoalEntity)

    @Query("SELECT * FROM food_health_connect_sync WHERE accountId = :accountId AND `key` = :key LIMIT 1")
    fun observeFoodHealthConnectSyncState(accountId: String, key: String): Flow<FoodHealthConnectSyncEntity?>

    @Query("SELECT * FROM food_health_connect_sync WHERE accountId = :accountId AND `key` = :key LIMIT 1")
    suspend fun getFoodHealthConnectSyncState(accountId: String, key: String): FoodHealthConnectSyncEntity?

    @Upsert
    suspend fun upsertFoodHealthConnectSyncState(state: FoodHealthConnectSyncEntity)

    @Query("SELECT COALESCE(SUM(amountMilliliters), 0.0) FROM water_entries WHERE accountId = :accountId AND dateEpochDay = :dateEpochDay")
    fun observeWaterTotalForDate(accountId: String, dateEpochDay: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amountMilliliters), 0.0) FROM water_entries WHERE accountId = :accountId AND dateEpochDay = :dateEpochDay")
    suspend fun getWaterTotalForDate(accountId: String, dateEpochDay: Long): Double

    @Query(
        "SELECT dateEpochDay, SUM(amountMilliliters) AS consumedMilliliters " +
            "FROM water_entries " +
            "WHERE accountId = :accountId AND dateEpochDay BETWEEN :startEpochDay AND :endEpochDay " +
            "GROUP BY dateEpochDay",
    )
    fun observeWaterTotalsForDateRange(accountId: String, startEpochDay: Long, endEpochDay: Long): Flow<List<WaterTotalRow>>

    @Upsert
    suspend fun insertWaterEntry(entry: WaterEntryEntity)

    @Query("SELECT * FROM quick_calorie_presets WHERE accountId = :accountId ORDER BY isFavorite DESC, updatedAtEpochMillis DESC, name")
    fun observeQuickCaloriePresets(accountId: String): Flow<List<QuickCaloriePresetEntity>>

    @Query("SELECT * FROM quick_calorie_presets WHERE accountId = :accountId AND id = :presetId LIMIT 1")
    suspend fun getQuickCaloriePreset(accountId: String, presetId: String): QuickCaloriePresetEntity?

    @Query(
        "SELECT * FROM shopping_list_items WHERE accountId = :accountId " +
            "ORDER BY isChecked ASC, category COLLATE NOCASE, sortOrder ASC, name COLLATE NOCASE",
    )
    fun observeShoppingListItems(accountId: String): Flow<List<ShoppingListItemEntity>>

    @Query(
        "SELECT * FROM shopping_list_items WHERE accountId = :accountId " +
            "ORDER BY isChecked ASC, category COLLATE NOCASE, sortOrder ASC, name COLLATE NOCASE",
    )
    suspend fun getShoppingListItems(accountId: String): List<ShoppingListItemEntity>

    @Query("SELECT * FROM shopping_list_items WHERE accountId = :accountId AND isManual = 0")
    suspend fun getGeneratedShoppingListItems(accountId: String): List<ShoppingListItemEntity>

    @Upsert
    suspend fun upsertShoppingListItem(item: ShoppingListItemEntity)

    @Query("UPDATE shopping_list_items SET isChecked = :isChecked, updatedAtEpochMillis = :updatedAtEpochMillis WHERE accountId = :accountId AND id = :itemId")
    suspend fun updateShoppingListItemChecked(accountId: String, itemId: String, isChecked: Boolean, updatedAtEpochMillis: Long): Int

    @Query("DELETE FROM shopping_list_items WHERE accountId = :accountId AND id = :itemId")
    suspend fun deleteShoppingListItemById(accountId: String, itemId: String): Int

    @Upsert
    suspend fun upsertQuickCaloriePreset(preset: QuickCaloriePresetEntity)

    @Query(
        "UPDATE quick_calorie_presets " +
            "SET isFavorite = :isFavorite, updatedAtEpochMillis = :updatedAtEpochMillis " +
            "WHERE accountId = :accountId AND id = :presetId",
    )
    suspend fun updateQuickCaloriePresetFavorite(
        accountId: String,
        presetId: String,
        isFavorite: Boolean,
        updatedAtEpochMillis: Long,
    ): Int

    @Query(
        "SELECT meal_templates.id AS templateId, " +
            "meal_templates.name AS templateName, " +
            "meal_templates.mealType AS templateMealType, " +
            "meal_templates.createdAtEpochMillis AS templateCreatedAtEpochMillis, " +
            "meal_templates.isFavorite AS templateIsFavorite, " +
            "meal_template_items.id AS itemId, " +
            "foods.id AS foodId, " +
            "foods.name AS foodName, " +
            "foods.brand AS brand, " +
            "meal_template_items.quantityGrams AS quantityGrams, " +
            "meal_template_items.sortOrder AS sortOrder " +
            "FROM meal_template_items " +
            "INNER JOIN meal_templates ON meal_templates.accountId = meal_template_items.accountId AND meal_templates.id = meal_template_items.templateId " +
            "INNER JOIN foods ON foods.accountId = meal_template_items.accountId AND foods.id = meal_template_items.foodId " +
            "WHERE meal_template_items.accountId = :accountId " +
            "ORDER BY meal_templates.createdAtEpochMillis DESC, meal_template_items.sortOrder",
    )
    fun observeMealTemplateRows(accountId: String): Flow<List<MealTemplateItemRow>>

    @Query(
        "SELECT meal_templates.id AS templateId, " +
            "meal_templates.name AS templateName, " +
            "meal_templates.mealType AS templateMealType, " +
            "meal_templates.createdAtEpochMillis AS templateCreatedAtEpochMillis, " +
            "meal_templates.isFavorite AS templateIsFavorite, " +
            "meal_template_items.id AS itemId, " +
            "foods.id AS foodId, " +
            "foods.name AS foodName, " +
            "foods.brand AS brand, " +
            "meal_template_items.quantityGrams AS quantityGrams, " +
            "meal_template_items.sortOrder AS sortOrder " +
            "FROM meal_template_items " +
            "INNER JOIN meal_templates ON meal_templates.accountId = meal_template_items.accountId AND meal_templates.id = meal_template_items.templateId " +
            "INNER JOIN foods ON foods.accountId = meal_template_items.accountId AND foods.id = meal_template_items.foodId " +
            "WHERE meal_template_items.accountId = :accountId AND meal_templates.id = :templateId " +
            "ORDER BY meal_template_items.sortOrder",
    )
    suspend fun getMealTemplateRows(accountId: String, templateId: String): List<MealTemplateItemRow>

    @Query("SELECT * FROM meal_templates WHERE accountId = :accountId AND id = :templateId LIMIT 1")
    suspend fun getMealTemplate(accountId: String, templateId: String): MealTemplateEntity?

    @Upsert
    suspend fun upsertMealTemplate(template: MealTemplateEntity)

    @Query(
        "UPDATE meal_templates " +
            "SET name = :name, mealType = :mealType, updatedAtEpochMillis = :updatedAtEpochMillis " +
            "WHERE accountId = :accountId AND id = :templateId",
    )
    suspend fun updateMealTemplateMetadata(
        accountId: String,
        templateId: String,
        name: String,
        mealType: String,
        updatedAtEpochMillis: Long,
    ): Int

    @Query(
        "UPDATE meal_templates " +
            "SET isFavorite = :isFavorite, updatedAtEpochMillis = :updatedAtEpochMillis " +
            "WHERE accountId = :accountId AND id = :templateId",
    )
    suspend fun updateMealTemplateFavorite(
        accountId: String,
        templateId: String,
        isFavorite: Boolean,
        updatedAtEpochMillis: Long,
    ): Int

    @Upsert
    suspend fun upsertMealTemplateItem(item: MealTemplateItemEntity)

    @Query("DELETE FROM meal_template_items WHERE accountId = :accountId AND templateId = :templateId")
    suspend fun deleteMealTemplateItems(accountId: String, templateId: String)

    @Query("DELETE FROM meal_templates WHERE accountId = :accountId AND id = :templateId")
    suspend fun deleteMealTemplateById(accountId: String, templateId: String): Int

    @Query(
        "SELECT recipes.id AS recipeId, " +
            "recipes.name AS recipeName, " +
            "recipes.category AS recipeCategory, " +
            "recipes.servingName AS recipeServingName, " +
            "recipes.servingGrams AS recipeServingGrams, " +
            "recipes.servings AS recipeServings, " +
            "recipes.cookedYieldGrams AS recipeCookedYieldGrams, " +
            "recipes.createdAtEpochMillis AS recipeCreatedAtEpochMillis, " +
            "recipes.isFavorite AS recipeIsFavorite, " +
            "recipe_ingredients.id AS ingredientId, " +
            "foods.id AS foodId, " +
            "foods.name AS foodName, " +
            "foods.brand AS brand, " +
            "foods.category AS foodCategory, " +
            "recipe_ingredients.quantityGrams AS quantityGrams, " +
            "recipe_ingredients.unitLabel AS unitLabel, " +
            "recipe_ingredients.unitGrams AS unitGrams, " +
            "recipe_ingredients.unitQuantity AS unitQuantity, " +
            "recipe_ingredients.sortOrder AS sortOrder, " +
            "foods.caloriesPer100g AS caloriesPer100g, " +
            "foods.proteinPer100g AS proteinPer100g, " +
            "foods.carbsPer100g AS carbsPer100g, " +
            "foods.fatPer100g AS fatPer100g, " +
            "foods.fiberPer100g AS fiberPer100g, " +
            "foods.sugarPer100g AS sugarPer100g, " +
            "foods.saturatedFatPer100g AS saturatedFatPer100g, " +
            "foods.sodiumMgPer100g AS sodiumMgPer100g, " +
            "foods.potassiumMgPer100g AS potassiumMgPer100g, " +
            "foods.calciumMgPer100g AS calciumMgPer100g, " +
            "foods.ironMgPer100g AS ironMgPer100g, " +
            "foods.vitaminDMcgPer100g AS vitaminDMcgPer100g, " +
            "foods.vitaminCMgPer100g AS vitaminCMgPer100g, " +
            "foods.magnesiumMgPer100g AS magnesiumMgPer100g " +
            "FROM recipe_ingredients " +
            "INNER JOIN recipes ON recipes.accountId = recipe_ingredients.accountId AND recipes.id = recipe_ingredients.recipeId " +
            "INNER JOIN foods ON foods.accountId = recipe_ingredients.accountId AND foods.id = recipe_ingredients.foodId " +
            "WHERE recipe_ingredients.accountId = :accountId " +
            "ORDER BY recipes.createdAtEpochMillis DESC, recipe_ingredients.sortOrder",
    )
    fun observeRecipeRows(accountId: String): Flow<List<RecipeIngredientRow>>

    @Query(
        "SELECT recipes.id AS recipeId, " +
            "recipes.name AS recipeName, " +
            "recipes.category AS recipeCategory, " +
            "recipes.servingName AS recipeServingName, " +
            "recipes.servingGrams AS recipeServingGrams, " +
            "recipes.servings AS recipeServings, " +
            "recipes.cookedYieldGrams AS recipeCookedYieldGrams, " +
            "recipes.createdAtEpochMillis AS recipeCreatedAtEpochMillis, " +
            "recipes.isFavorite AS recipeIsFavorite, " +
            "recipe_ingredients.id AS ingredientId, " +
            "foods.id AS foodId, " +
            "foods.name AS foodName, " +
            "foods.brand AS brand, " +
            "foods.category AS foodCategory, " +
            "recipe_ingredients.quantityGrams AS quantityGrams, " +
            "recipe_ingredients.unitLabel AS unitLabel, " +
            "recipe_ingredients.unitGrams AS unitGrams, " +
            "recipe_ingredients.unitQuantity AS unitQuantity, " +
            "recipe_ingredients.sortOrder AS sortOrder, " +
            "foods.caloriesPer100g AS caloriesPer100g, " +
            "foods.proteinPer100g AS proteinPer100g, " +
            "foods.carbsPer100g AS carbsPer100g, " +
            "foods.fatPer100g AS fatPer100g, " +
            "foods.fiberPer100g AS fiberPer100g, " +
            "foods.sugarPer100g AS sugarPer100g, " +
            "foods.saturatedFatPer100g AS saturatedFatPer100g, " +
            "foods.sodiumMgPer100g AS sodiumMgPer100g, " +
            "foods.potassiumMgPer100g AS potassiumMgPer100g, " +
            "foods.calciumMgPer100g AS calciumMgPer100g, " +
            "foods.ironMgPer100g AS ironMgPer100g, " +
            "foods.vitaminDMcgPer100g AS vitaminDMcgPer100g, " +
            "foods.vitaminCMgPer100g AS vitaminCMgPer100g, " +
            "foods.magnesiumMgPer100g AS magnesiumMgPer100g " +
            "FROM recipe_ingredients " +
            "INNER JOIN recipes ON recipes.accountId = recipe_ingredients.accountId AND recipes.id = recipe_ingredients.recipeId " +
            "INNER JOIN foods ON foods.accountId = recipe_ingredients.accountId AND foods.id = recipe_ingredients.foodId " +
            "WHERE recipe_ingredients.accountId = :accountId AND recipes.id = :recipeId " +
            "ORDER BY recipe_ingredients.sortOrder",
    )
    suspend fun getRecipeRows(accountId: String, recipeId: String): List<RecipeIngredientRow>

    @Query("SELECT * FROM recipes WHERE accountId = :accountId AND id = :recipeId LIMIT 1")
    suspend fun getRecipe(accountId: String, recipeId: String): RecipeEntity?

    @Query("SELECT * FROM recipes WHERE accountId = :accountId AND lower(name) = lower(:name) ORDER BY updatedAtEpochMillis DESC LIMIT 1")
    suspend fun getRecipeByName(accountId: String, name: String): RecipeEntity?

    @Upsert
    suspend fun upsertRecipe(recipe: RecipeEntity)

    @Query(
        "UPDATE recipes " +
            "SET isFavorite = :isFavorite, updatedAtEpochMillis = :updatedAtEpochMillis " +
            "WHERE accountId = :accountId AND id = :recipeId",
    )
    suspend fun updateRecipeFavorite(
        accountId: String,
        recipeId: String,
        isFavorite: Boolean,
        updatedAtEpochMillis: Long,
    ): Int

    @Upsert
    suspend fun upsertRecipeIngredient(ingredient: RecipeIngredientEntity)

    @Query("DELETE FROM recipe_ingredients WHERE accountId = :accountId AND recipeId = :recipeId")
    suspend fun deleteRecipeIngredients(accountId: String, recipeId: String)

    @Query("DELETE FROM recipes WHERE accountId = :accountId AND id = :recipeId")
    suspend fun deleteRecipeById(accountId: String, recipeId: String): Int

    @Query("DELETE FROM food_servings WHERE accountId = :accountId AND foodId = :foodId")
    suspend fun deleteServingsForFood(accountId: String, foodId: String)

    @Delete
    suspend fun deleteFood(food: FoodEntity)

    @Query("UPDATE meal_items SET foodId = :primaryFoodId WHERE accountId = :accountId AND foodId IN (:duplicateFoodIds)")
    suspend fun reassignMealItemsToFood(accountId: String, primaryFoodId: String, duplicateFoodIds: List<String>)

    @Query("UPDATE meal_template_items SET foodId = :primaryFoodId WHERE accountId = :accountId AND foodId IN (:duplicateFoodIds)")
    suspend fun reassignMealTemplateItemsToFood(accountId: String, primaryFoodId: String, duplicateFoodIds: List<String>)

    @Query("UPDATE recipe_ingredients SET foodId = :primaryFoodId WHERE accountId = :accountId AND foodId IN (:duplicateFoodIds)")
    suspend fun reassignRecipeIngredientsToFood(accountId: String, primaryFoodId: String, duplicateFoodIds: List<String>)

    @Query("UPDATE barcode_products SET linkedFoodId = :primaryFoodId WHERE accountId = :accountId AND linkedFoodId IN (:duplicateFoodIds)")
    suspend fun reassignBarcodeProductsToFood(accountId: String, primaryFoodId: String, duplicateFoodIds: List<String>)

    @Query("DELETE FROM meal_items WHERE accountId = :accountId AND id = :mealItemId")
    suspend fun deleteMealItemById(accountId: String, mealItemId: String): Int

    @Query("UPDATE meal_items SET status = :status WHERE accountId = :accountId AND id = :mealItemId")
    suspend fun updateMealItemStatus(accountId: String, mealItemId: String, status: String): Int

    @Upsert
    suspend fun upsertFood(food: FoodEntity)

    @Query(
        "UPDATE foods SET isFavorite = :isFavorite, updatedAtEpochMillis = :updatedAtEpochMillis " +
            "WHERE accountId = :accountId AND id = :foodId",
    )
    suspend fun updateFoodFavorite(
        accountId: String,
        foodId: String,
        isFavorite: Boolean,
        updatedAtEpochMillis: Long,
    ): Int

    @Upsert
    suspend fun upsertServing(serving: FoodServingEntity)

    @Upsert
    suspend fun upsertMeal(meal: MealEntity)

    @Upsert
    suspend fun upsertMealItem(item: MealItemEntity)

    @Upsert
    suspend fun upsertBarcodeProduct(product: BarcodeProductEntity)
}
