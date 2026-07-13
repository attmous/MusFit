package com.musfit.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "foods",
    primaryKeys = ["accountId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["accountId", "barcode"]),
        Index(value = ["accountId", "name"]),
        Index(value = ["accountId", "brand"]),
        Index(value = ["accountId", "category"]),
        Index(value = ["accountId", "isFavorite"]),
    ],
)
data class FoodEntity(
    val accountId: String,
    val id: String,
    val name: String,
    val brand: String?,
    val defaultServingGrams: Double,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val servingName: String? = null,
    val barcode: String? = null,
    val category: String? = null,
    @ColumnInfo(defaultValue = "0") val isFavorite: Boolean = false,
    @ColumnInfo(defaultValue = "0") val fiberPer100g: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val sugarPer100g: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val saturatedFatPer100g: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val sodiumMgPer100g: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val potassiumMgPer100g: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val calciumMgPer100g: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val ironMgPer100g: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val vitaminDMcgPer100g: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val vitaminCMgPer100g: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val magnesiumMgPer100g: Double = 0.0,
    val imageUrl: String? = null,
)

@Entity(
    tableName = "food_servings",
    primaryKeys = ["accountId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["accountId", "id"],
            childColumns = ["accountId", "foodId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["accountId", "foodId", "label"])],
)
data class FoodServingEntity(
    val accountId: String,
    val id: String,
    val foodId: String,
    val label: String,
    val grams: Double,
)

@Entity(
    tableName = "meals",
    primaryKeys = ["accountId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["accountId", "dateEpochDay", "createdAtEpochMillis"]),
        Index(value = ["accountId", "dateEpochDay", "type", "createdAtEpochMillis"]),
    ],
)
data class MealEntity(
    val accountId: String,
    val id: String,
    val dateEpochDay: Long,
    val type: String,
    val notes: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "meal_definitions",
    primaryKeys = ["accountId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["accountId", "sortOrder", "name"])],
)
data class MealDefinitionEntity(
    val accountId: String,
    val id: String,
    val name: String,
    val timeMinutes: Int?,
    val sortOrder: Int,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    @ColumnInfo(defaultValue = "0") val isHidden: Boolean = false,
)

@Entity(
    tableName = "meal_items",
    primaryKeys = ["accountId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = MealEntity::class,
            parentColumns = ["accountId", "id"],
            childColumns = ["accountId", "mealId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["accountId", "id"],
            childColumns = ["accountId", "foodId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["accountId", "mealId", "status"]), Index(value = ["accountId", "foodId"])],
)
data class MealItemEntity(
    val accountId: String,
    val id: String,
    val mealId: String,
    val foodId: String,
    val quantityGrams: Double,
    @ColumnInfo(defaultValue = "'logged'") val status: String = "logged",
)

@Entity(
    tableName = "shopping_list_items",
    primaryKeys = ["accountId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["accountId", "category"]),
        Index(value = ["accountId", "sourceKey"], unique = true),
        Index(value = ["accountId", "isManual"]),
    ],
)
data class ShoppingListItemEntity(
    val accountId: String,
    val id: String,
    val name: String,
    val category: String,
    val quantityGrams: Double,
    val isChecked: Boolean,
    val isManual: Boolean,
    val sourceKey: String?,
    val sortOrder: Int,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "water_entries",
    primaryKeys = ["accountId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["accountId", "dateEpochDay"])],
)
data class WaterEntryEntity(
    val accountId: String,
    val id: String,
    val dateEpochDay: Long,
    val amountMilliliters: Double,
    val createdAtEpochMillis: Long,
)

@Entity(
    tableName = "food_health_connect_sync",
    primaryKeys = ["accountId", "key"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class FoodHealthConnectSyncEntity(
    val accountId: String,
    val key: String,
    val isEnabled: Boolean,
    val lastSyncAtEpochMillis: Long?,
    val lastFailureMessage: String?,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "barcode_products",
    primaryKeys = ["accountId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["accountId", "id"],
            childColumns = ["linkedFoodAccountId", "linkedFoodId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["accountId", "barcode"], unique = true),
        Index(value = ["accountId", "linkedFoodId"]),
        Index(value = ["linkedFoodAccountId", "linkedFoodId"]),
    ],
)
data class BarcodeProductEntity(
    val accountId: String,
    val id: String,
    val barcode: String,
    val provider: String,
    val providerProductName: String?,
    val providerBrand: String?,
    val rawJson: String,
    val quality: String,
    val linkedFoodAccountId: String? = accountId,
    val linkedFoodId: String?,
    val fetchedAtEpochMillis: Long,
)

@Entity(
    tableName = "food_goals",
    primaryKeys = ["accountId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class FoodGoalEntity(
    val accountId: String,
    val id: String,
    val dailyCaloriesKcal: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val fiberGrams: Double,
    val sugarGrams: Double,
    val saturatedFatGrams: Double,
    val sodiumMilligrams: Double,
    val mode: String,
    val includeTrainingCalories: Boolean,
    @ColumnInfo(defaultValue = "0") val useNetCarbs: Boolean = false,
    @ColumnInfo(defaultValue = "2000") val waterGoalMilliliters: Double = 2000.0,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "quick_calorie_presets",
    primaryKeys = ["accountId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["accountId", "isFavorite", "updatedAtEpochMillis", "name"])],
)
data class QuickCaloriePresetEntity(
    val accountId: String,
    val id: String,
    val name: String,
    val caloriesKcal: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    @ColumnInfo(defaultValue = "1") val isFavorite: Boolean = true,
)

@Entity(
    tableName = "meal_templates",
    primaryKeys = ["accountId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["accountId", "updatedAtEpochMillis"])],
)
data class MealTemplateEntity(
    val accountId: String,
    val id: String,
    val name: String,
    val mealType: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    @ColumnInfo(defaultValue = "0") val isFavorite: Boolean = false,
)

@Entity(
    tableName = "meal_template_items",
    primaryKeys = ["accountId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = MealTemplateEntity::class,
            parentColumns = ["accountId", "id"],
            childColumns = ["accountId", "templateId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["accountId", "id"],
            childColumns = ["accountId", "foodId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["accountId", "templateId", "sortOrder"]), Index(value = ["accountId", "foodId"])],
)
data class MealTemplateItemEntity(
    val accountId: String,
    val id: String,
    val templateId: String,
    val foodId: String,
    val quantityGrams: Double,
    val sortOrder: Int,
)

@Entity(
    tableName = "recipes",
    primaryKeys = ["accountId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["accountId", "updatedAtEpochMillis"]), Index(value = ["accountId", "name"])],
)
data class RecipeEntity(
    val accountId: String,
    val id: String,
    val name: String,
    val category: String?,
    val servingName: String,
    val servingGrams: Double,
    @ColumnInfo(defaultValue = "1") val servings: Double = 1.0,
    @ColumnInfo(defaultValue = "0") val cookedYieldGrams: Double = 0.0,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    @ColumnInfo(defaultValue = "0") val isFavorite: Boolean = false,
)

@Entity(
    tableName = "recipe_ingredients",
    primaryKeys = ["accountId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["accountId", "id"],
            childColumns = ["accountId", "recipeId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["accountId", "id"],
            childColumns = ["accountId", "foodId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["accountId", "recipeId", "sortOrder"]), Index(value = ["accountId", "foodId"])],
)
data class RecipeIngredientEntity(
    val accountId: String,
    val id: String,
    val recipeId: String,
    val foodId: String,
    val quantityGrams: Double,
    @ColumnInfo(defaultValue = "'g'") val unitLabel: String = "g",
    @ColumnInfo(defaultValue = "1") val unitGrams: Double = 1.0,
    @ColumnInfo(defaultValue = "0") val unitQuantity: Double = 0.0,
    val sortOrder: Int,
)
