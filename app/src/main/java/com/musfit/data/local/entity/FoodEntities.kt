package com.musfit.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "foods")
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
