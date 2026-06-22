package com.musfit.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musfit.data.local.entity.BarcodeProductEntity
import com.musfit.data.local.entity.FoodEntity
import com.musfit.data.local.entity.FoodGoalEntity
import com.musfit.data.local.entity.FoodHealthConnectSyncEntity
import com.musfit.data.local.entity.FoodServingEntity
import com.musfit.data.local.entity.MealEntity
import com.musfit.data.local.entity.MealDefinitionEntity
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
            "INNER JOIN meal_items ON meal_items.foodId = foods.id " +
            "INNER JOIN meals ON meals.id = meal_items.mealId " +
            "GROUP BY foods.id " +
            "ORDER BY MAX(meals.createdAtEpochMillis) DESC " +
            "LIMIT :limit",
    )
    fun observeRecentFoods(limit: Int): Flow<List<FoodEntity>>

    @Query(
        "SELECT foods.* FROM foods " +
            "INNER JOIN meal_items ON meal_items.foodId = foods.id " +
            "INNER JOIN meals ON meals.id = meal_items.mealId " +
            "WHERE meals.dateEpochDay = :dateEpochDay AND meals.type = :mealType " +
            "GROUP BY foods.id " +
            "ORDER BY MAX(meals.createdAtEpochMillis) DESC",
    )
    fun observeSameAsYesterday(dateEpochDay: Long, mealType: String): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods ORDER BY name")
    fun observeFoods(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM food_servings WHERE foodId = :foodId ORDER BY label")
    fun observeServings(foodId: String): Flow<List<FoodServingEntity>>

    @Query("SELECT * FROM food_servings WHERE foodId = :foodId ORDER BY label")
    suspend fun getServings(foodId: String): List<FoodServingEntity>

    @Query("SELECT * FROM foods WHERE id = :foodId LIMIT 1")
    suspend fun getFood(foodId: String): FoodEntity?

    @Query("SELECT * FROM foods WHERE barcode = :barcode LIMIT 1")
    suspend fun getFoodByBarcode(barcode: String): FoodEntity?

    @Query(
        "SELECT * FROM foods " +
            "WHERE lower(name) = lower(:name) " +
            "AND ((brand IS NULL AND :brand IS NULL) OR lower(brand) = lower(:brand)) " +
            "LIMIT 1",
    )
    suspend fun getFoodByNameAndBrand(name: String, brand: String?): FoodEntity?

    @Query("SELECT * FROM meals WHERE id = :mealId LIMIT 1")
    suspend fun getMeal(mealId: String): MealEntity?

    @Query("SELECT * FROM meal_items WHERE id = :mealItemId LIMIT 1")
    suspend fun getMealItem(mealItemId: String): MealItemEntity?

    @Query("SELECT COUNT(*) FROM meal_items WHERE foodId = :foodId")
    suspend fun countMealItemsForFood(foodId: String): Int

    @Query("SELECT * FROM meals WHERE dateEpochDay = :dateEpochDay ORDER BY createdAtEpochMillis")
    fun observeMealsForDate(dateEpochDay: Long): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE dateEpochDay = :dateEpochDay AND type = :mealType ORDER BY createdAtEpochMillis")
    suspend fun getMealsForDateAndType(dateEpochDay: Long, mealType: String): List<MealEntity>

    @Query("SELECT * FROM meal_definitions ORDER BY sortOrder, name")
    fun observeMealDefinitions(): Flow<List<MealDefinitionEntity>>

    @Query("SELECT * FROM meal_definitions WHERE id = :mealId LIMIT 1")
    suspend fun getMealDefinition(mealId: String): MealDefinitionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMealDefinition(definition: MealDefinitionEntity)

    @Query("SELECT * FROM meal_items WHERE mealId = :mealId")
    fun observeMealItems(mealId: String): Flow<List<MealItemEntity>>

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
            "INNER JOIN meals ON meals.id = meal_items.mealId " +
            "INNER JOIN foods ON foods.id = meal_items.foodId " +
            "WHERE meals.dateEpochDay = :dateEpochDay AND meal_items.status = 'logged'",
    )
    fun observeMealNutritionRowsForDate(dateEpochDay: Long): Flow<List<MealNutritionRow>>

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
            "INNER JOIN meals ON meals.id = meal_items.mealId " +
            "INNER JOIN foods ON foods.id = meal_items.foodId " +
            "WHERE meals.dateEpochDay = :dateEpochDay " +
            "ORDER BY meals.createdAtEpochMillis, meal_items.id",
    )
    fun observeFoodDiaryEntryRowsForDate(dateEpochDay: Long): Flow<List<FoodDiaryEntryRow>>

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
            "INNER JOIN meals ON meals.id = meal_items.mealId " +
            "INNER JOIN foods ON foods.id = meal_items.foodId " +
            "WHERE meals.dateEpochDay BETWEEN :startEpochDay AND :endEpochDay " +
            "ORDER BY meals.dateEpochDay, meals.createdAtEpochMillis, meal_items.id",
    )
    fun observeFoodDiaryEntryRowsForDateRange(startEpochDay: Long, endEpochDay: Long): Flow<List<FoodDiaryEntryRow>>

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
            "INNER JOIN meals ON meals.id = meal_items.mealId " +
            "INNER JOIN foods ON foods.id = meal_items.foodId " +
            "WHERE meals.dateEpochDay BETWEEN :startEpochDay AND :endEpochDay " +
            "ORDER BY meals.dateEpochDay, meals.createdAtEpochMillis, meal_items.id",
    )
    suspend fun getFoodDiaryEntryRowsForDateRange(startEpochDay: Long, endEpochDay: Long): List<FoodDiaryEntryRow>

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
            "INNER JOIN meals ON meals.id = meal_items.mealId " +
            "INNER JOIN foods ON foods.id = meal_items.foodId " +
            "WHERE meals.dateEpochDay = :dateEpochDay " +
            "ORDER BY meals.createdAtEpochMillis, meal_items.id",
    )
    suspend fun getFoodDiaryEntryRowsForDate(dateEpochDay: Long): List<FoodDiaryEntryRow>

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
            "INNER JOIN meals ON meals.id = meal_items.mealId " +
            "INNER JOIN foods ON foods.id = meal_items.foodId " +
            "WHERE meals.dateEpochDay = :dateEpochDay AND meals.type = :mealType " +
            "ORDER BY meals.createdAtEpochMillis, meal_items.id",
    )
    suspend fun getFoodDiaryEntryRowsForDateAndMeal(dateEpochDay: Long, mealType: String): List<FoodDiaryEntryRow>

    @Query("SELECT * FROM barcode_products WHERE barcode = :barcode LIMIT 1")
    suspend fun getBarcodeProduct(barcode: String): BarcodeProductEntity?

    @Query("SELECT * FROM barcode_products ORDER BY fetchedAtEpochMillis DESC")
    fun observeBarcodeProducts(): Flow<List<BarcodeProductEntity>>

    @Query("SELECT * FROM barcode_products WHERE linkedFoodId = :foodId ORDER BY fetchedAtEpochMillis DESC")
    fun observeBarcodeProducts(foodId: String): Flow<List<BarcodeProductEntity>>

    @Query("SELECT * FROM food_goals WHERE id = :id LIMIT 1")
    fun observeFoodGoal(id: String): Flow<FoodGoalEntity?>

    @Query("SELECT * FROM food_goals WHERE id = :id LIMIT 1")
    suspend fun getFoodGoal(id: String): FoodGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFoodGoal(goal: FoodGoalEntity)

    @Query("SELECT * FROM food_health_connect_sync WHERE `key` = :key LIMIT 1")
    fun observeFoodHealthConnectSyncState(key: String): Flow<FoodHealthConnectSyncEntity?>

    @Query("SELECT * FROM food_health_connect_sync WHERE `key` = :key LIMIT 1")
    suspend fun getFoodHealthConnectSyncState(key: String): FoodHealthConnectSyncEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFoodHealthConnectSyncState(state: FoodHealthConnectSyncEntity)

    @Query("SELECT COALESCE(SUM(amountMilliliters), 0.0) FROM water_entries WHERE dateEpochDay = :dateEpochDay")
    fun observeWaterTotalForDate(dateEpochDay: Long): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterEntry(entry: WaterEntryEntity)

    @Query("SELECT * FROM quick_calorie_presets ORDER BY isFavorite DESC, updatedAtEpochMillis DESC, name")
    fun observeQuickCaloriePresets(): Flow<List<QuickCaloriePresetEntity>>

    @Query("SELECT * FROM quick_calorie_presets WHERE id = :presetId LIMIT 1")
    suspend fun getQuickCaloriePreset(presetId: String): QuickCaloriePresetEntity?

    @Query(
        "SELECT * FROM shopping_list_items " +
            "ORDER BY isChecked ASC, category COLLATE NOCASE, sortOrder ASC, name COLLATE NOCASE",
    )
    fun observeShoppingListItems(): Flow<List<ShoppingListItemEntity>>

    @Query(
        "SELECT * FROM shopping_list_items " +
            "ORDER BY isChecked ASC, category COLLATE NOCASE, sortOrder ASC, name COLLATE NOCASE",
    )
    suspend fun getShoppingListItems(): List<ShoppingListItemEntity>

    @Query("SELECT * FROM shopping_list_items WHERE isManual = 0")
    suspend fun getGeneratedShoppingListItems(): List<ShoppingListItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShoppingListItem(item: ShoppingListItemEntity)

    @Query("UPDATE shopping_list_items SET isChecked = :isChecked, updatedAtEpochMillis = :updatedAtEpochMillis WHERE id = :itemId")
    suspend fun updateShoppingListItemChecked(itemId: String, isChecked: Boolean, updatedAtEpochMillis: Long): Int

    @Query("DELETE FROM shopping_list_items WHERE id = :itemId")
    suspend fun deleteShoppingListItemById(itemId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuickCaloriePreset(preset: QuickCaloriePresetEntity)

    @Query(
        "UPDATE quick_calorie_presets " +
            "SET isFavorite = :isFavorite, updatedAtEpochMillis = :updatedAtEpochMillis " +
            "WHERE id = :presetId",
    )
    suspend fun updateQuickCaloriePresetFavorite(
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
            "INNER JOIN meal_templates ON meal_templates.id = meal_template_items.templateId " +
            "INNER JOIN foods ON foods.id = meal_template_items.foodId " +
            "ORDER BY meal_templates.createdAtEpochMillis DESC, meal_template_items.sortOrder",
    )
    fun observeMealTemplateRows(): Flow<List<MealTemplateItemRow>>

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
            "INNER JOIN meal_templates ON meal_templates.id = meal_template_items.templateId " +
            "INNER JOIN foods ON foods.id = meal_template_items.foodId " +
            "WHERE meal_templates.id = :templateId " +
            "ORDER BY meal_template_items.sortOrder",
    )
    suspend fun getMealTemplateRows(templateId: String): List<MealTemplateItemRow>

    @Query("SELECT * FROM meal_templates WHERE id = :templateId LIMIT 1")
    suspend fun getMealTemplate(templateId: String): MealTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMealTemplate(template: MealTemplateEntity)

    @Query(
        "UPDATE meal_templates " +
            "SET name = :name, mealType = :mealType, updatedAtEpochMillis = :updatedAtEpochMillis " +
            "WHERE id = :templateId",
    )
    suspend fun updateMealTemplateMetadata(
        templateId: String,
        name: String,
        mealType: String,
        updatedAtEpochMillis: Long,
    ): Int

    @Query(
        "UPDATE meal_templates " +
            "SET isFavorite = :isFavorite, updatedAtEpochMillis = :updatedAtEpochMillis " +
            "WHERE id = :templateId",
    )
    suspend fun updateMealTemplateFavorite(
        templateId: String,
        isFavorite: Boolean,
        updatedAtEpochMillis: Long,
    ): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMealTemplateItem(item: MealTemplateItemEntity)

    @Query("DELETE FROM meal_template_items WHERE templateId = :templateId")
    suspend fun deleteMealTemplateItems(templateId: String)

    @Query("DELETE FROM meal_templates WHERE id = :templateId")
    suspend fun deleteMealTemplateById(templateId: String): Int

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
            "INNER JOIN recipes ON recipes.id = recipe_ingredients.recipeId " +
            "INNER JOIN foods ON foods.id = recipe_ingredients.foodId " +
            "ORDER BY recipes.createdAtEpochMillis DESC, recipe_ingredients.sortOrder",
    )
    fun observeRecipeRows(): Flow<List<RecipeIngredientRow>>

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
            "INNER JOIN recipes ON recipes.id = recipe_ingredients.recipeId " +
            "INNER JOIN foods ON foods.id = recipe_ingredients.foodId " +
            "WHERE recipes.id = :recipeId " +
            "ORDER BY recipe_ingredients.sortOrder",
    )
    suspend fun getRecipeRows(recipeId: String): List<RecipeIngredientRow>

    @Query("SELECT * FROM recipes WHERE id = :recipeId LIMIT 1")
    suspend fun getRecipe(recipeId: String): RecipeEntity?

    @Query("SELECT * FROM recipes WHERE lower(name) = lower(:name) ORDER BY updatedAtEpochMillis DESC LIMIT 1")
    suspend fun getRecipeByName(name: String): RecipeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecipe(recipe: RecipeEntity)

    @Query(
        "UPDATE recipes " +
            "SET isFavorite = :isFavorite, updatedAtEpochMillis = :updatedAtEpochMillis " +
            "WHERE id = :recipeId",
    )
    suspend fun updateRecipeFavorite(
        recipeId: String,
        isFavorite: Boolean,
        updatedAtEpochMillis: Long,
    ): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecipeIngredient(ingredient: RecipeIngredientEntity)

    @Query("DELETE FROM recipe_ingredients WHERE recipeId = :recipeId")
    suspend fun deleteRecipeIngredients(recipeId: String)

    @Query("DELETE FROM recipes WHERE id = :recipeId")
    suspend fun deleteRecipeById(recipeId: String): Int

    @Query("DELETE FROM food_servings WHERE foodId = :foodId")
    suspend fun deleteServingsForFood(foodId: String)

    @Delete
    suspend fun deleteFood(food: FoodEntity)

    @Query("UPDATE meal_items SET foodId = :primaryFoodId WHERE foodId IN (:duplicateFoodIds)")
    suspend fun reassignMealItemsToFood(primaryFoodId: String, duplicateFoodIds: List<String>)

    @Query("UPDATE meal_template_items SET foodId = :primaryFoodId WHERE foodId IN (:duplicateFoodIds)")
    suspend fun reassignMealTemplateItemsToFood(primaryFoodId: String, duplicateFoodIds: List<String>)

    @Query("UPDATE recipe_ingredients SET foodId = :primaryFoodId WHERE foodId IN (:duplicateFoodIds)")
    suspend fun reassignRecipeIngredientsToFood(primaryFoodId: String, duplicateFoodIds: List<String>)

    @Query("UPDATE barcode_products SET linkedFoodId = :primaryFoodId WHERE linkedFoodId IN (:duplicateFoodIds)")
    suspend fun reassignBarcodeProductsToFood(primaryFoodId: String, duplicateFoodIds: List<String>)

    @Query("DELETE FROM meal_items WHERE id = :mealItemId")
    suspend fun deleteMealItemById(mealItemId: String): Int

    @Query("UPDATE meal_items SET status = :status WHERE id = :mealItemId")
    suspend fun updateMealItemStatus(mealItemId: String, status: String): Int

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
