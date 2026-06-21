package com.musfit.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

@Entity(tableName = "foods", indices = [Index("barcode")])
data class FoodEntity(
    @PrimaryKey val id: String,
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
)

@Entity(
    tableName = "food_servings",
    foreignKeys = [
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("foodId")],
)
data class FoodServingEntity(
    @PrimaryKey val id: String,
    val foodId: String,
    val label: String,
    val grams: Double,
)

@Entity(tableName = "meals", indices = [Index("dateEpochDay")])
data class MealEntity(
    @PrimaryKey val id: String,
    val dateEpochDay: Long,
    val type: String,
    val notes: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(tableName = "meal_definitions", indices = [Index("sortOrder")])
data class MealDefinitionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val timeMinutes: Int?,
    val sortOrder: Int,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "meal_items",
    foreignKeys = [
        ForeignKey(
            entity = MealEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("mealId"), Index("foodId")],
)
data class MealItemEntity(
    @PrimaryKey val id: String,
    val mealId: String,
    val foodId: String,
    val quantityGrams: Double,
)

@Entity(
    tableName = "barcode_products",
    foreignKeys = [
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedFoodId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index(value = ["barcode"], unique = true), Index("linkedFoodId")],
)
data class BarcodeProductEntity(
    @PrimaryKey val id: String,
    val barcode: String,
    val provider: String,
    val providerProductName: String?,
    val providerBrand: String?,
    val rawJson: String,
    val quality: String,
    val linkedFoodId: String?,
    val fetchedAtEpochMillis: Long,
)

@Entity(tableName = "food_goals")
data class FoodGoalEntity(
    @PrimaryKey val id: String,
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
    val updatedAtEpochMillis: Long,
)

@Entity(tableName = "quick_calorie_presets")
data class QuickCaloriePresetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val caloriesKcal: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    @ColumnInfo(defaultValue = "1") val isFavorite: Boolean = true,
)

@Entity(tableName = "meal_templates")
data class MealTemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val mealType: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    @ColumnInfo(defaultValue = "0") val isFavorite: Boolean = false,
)

@Entity(
    tableName = "meal_template_items",
    foreignKeys = [
        ForeignKey(
            entity = MealTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("templateId"), Index("foodId")],
)
data class MealTemplateItemEntity(
    @PrimaryKey val id: String,
    val templateId: String,
    val foodId: String,
    val quantityGrams: Double,
    val sortOrder: Int,
)

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String?,
    val servingName: String,
    val servingGrams: Double,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    @ColumnInfo(defaultValue = "0") val isFavorite: Boolean = false,
)

@Entity(
    tableName = "recipe_ingredients",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("recipeId"), Index("foodId")],
)
data class RecipeIngredientEntity(
    @PrimaryKey val id: String,
    val recipeId: String,
    val foodId: String,
    val quantityGrams: Double,
    val sortOrder: Int,
)
