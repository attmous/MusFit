package com.musfit.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.musfit.data.local.MusFitDatabase
import com.musfit.data.local.entity.MealEntity
import com.musfit.data.local.entity.MealItemEntity
import com.musfit.data.remote.food.ProductDataQuality
import com.musfit.data.remote.food.ProductLookupResult
import com.musfit.domain.model.FoodNutrition
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalFoodRepositoryTest {
    private lateinit var database: MusFitDatabase
    private lateinit var repository: LocalFoodRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room.inMemoryDatabaseBuilder(context, MusFitDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository = LocalFoodRepository(database = database, foodDao = database.foodDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun saveConfirmedProduct_persistsFoodServingAndBarcodeLink() = runTest {
        val result = foundProduct()
        val editedNutrition = nutrition(calories = 61.0, protein = 10.5, carbs = 4.0, fat = 0.5)

        val foodId =
            repository.saveConfirmedProduct(
                result = result,
                editedName = "Strained Greek Yogurt",
                editedBrand = "",
                editedNutrition = editedNutrition,
            )

        val savedFood = database.foodDao().observeFoods().first().single()
        val barcodeProduct = database.foodDao().getBarcodeProduct(result.barcode)
        val servings = database.foodDao().getServings(foodId)

        assertEquals(foodId, barcodeProduct?.linkedFoodId)
        assertEquals("Strained Greek Yogurt", savedFood.name)
        assertNull(savedFood.brand)
        assertEquals(61.0, savedFood.caloriesPer100g, 0.01)
        assertEquals(10.5, savedFood.proteinPer100g, 0.01)
        assertEquals(4.0, savedFood.carbsPer100g, 0.01)
        assertEquals(0.5, savedFood.fatPer100g, 0.01)
        assertEquals(170.0, savedFood.defaultServingGrams, 0.01)
        assertEquals("Example Dairy", barcodeProduct?.providerBrand)
        assertEquals("open_food_facts", barcodeProduct?.provider)
        assertEquals("complete", barcodeProduct?.quality)
        assertEquals(1, servings.size)
        assertEquals("170 g", servings.single().label)
        assertEquals(170.0, servings.single().grams, 0.01)
        assertNotNull(savedFood)
    }

    @Test
    fun saveConfirmedProduct_reusesExistingFoodWhenBarcodeDataIsIdentical() = runTest {
        val initialResult = foundProduct(rawJson = """{"code":"1234567890123","unknown":"first"}""")
        val identicalResult = initialResult.copy(rawJson = """{"code":"1234567890123","unknown":"second"}""")
        val editedNutrition = nutrition(calories = 61.0, protein = 10.5, carbs = 4.0, fat = 0.5)

        val firstFoodId =
            repository.saveConfirmedProduct(
                result = initialResult,
                editedName = "Strained Greek Yogurt",
                editedBrand = "",
                editedNutrition = editedNutrition,
            )

        val secondFoodId =
            repository.saveConfirmedProduct(
                result = identicalResult,
                editedName = "Strained Greek Yogurt",
                editedBrand = "",
                editedNutrition = editedNutrition,
            )

        val foods = database.foodDao().observeFoods().first()
        val barcodeProduct = database.foodDao().getBarcodeProduct(identicalResult.barcode)
        val servings = database.foodDao().getServings(firstFoodId)

        assertEquals(firstFoodId, secondFoodId)
        assertEquals(1, foods.size)
        assertEquals(firstFoodId, barcodeProduct?.linkedFoodId)
        assertEquals(1, servings.size)
        assertEquals("170 g", servings.single().label)
        assertEquals(170.0, servings.single().grams, 0.01)
        assertEquals(170.0, foods.single().defaultServingGrams, 0.01)
        assertNull(foods.single().brand)
        assertEquals(61.0, foods.single().caloriesPer100g, 0.01)
    }

    @Test
    fun saveConfirmedProduct_versionsLinkedFoodWhenBarcodeDataChanges() = runTest {
        val initialResult = foundProduct(rawJson = """{"code":"1234567890123","unknown":"first"}""")
        val changedResult =
            initialResult.copy(
                name = "Greek Yogurt Plain",
                servingQuantityGrams = 200.0,
                rawJson = """{"code":"1234567890123","unknown":"second"}""",
            )
        val originalNutrition = nutrition(calories = 61.0, protein = 10.5, carbs = 4.0, fat = 0.5)
        val updatedNutrition = nutrition(calories = 65.0, protein = 11.0, carbs = 4.5, fat = 0.6)

        val originalFoodId =
            repository.saveConfirmedProduct(
                result = initialResult,
                editedName = "Strained Greek Yogurt",
                editedBrand = "",
                editedNutrition = originalNutrition,
            )

        val meal =
            MealEntity(
                id = "meal-1",
                dateEpochDay = 20_000L,
                type = "breakfast",
                notes = null,
                createdAtEpochMillis = 1_000L,
                updatedAtEpochMillis = 1_000L,
            )
        val mealItem =
            MealItemEntity(
                id = "meal-item-1",
                mealId = meal.id,
                foodId = originalFoodId,
                quantityGrams = 170.0,
            )
        database.foodDao().upsertMeal(meal)
        database.foodDao().upsertMealItem(mealItem)

        val newFoodId =
            repository.saveConfirmedProduct(
                result = changedResult,
                editedName = "Strained Greek Yogurt Plus",
                editedBrand = "Example Dairy",
                editedNutrition = updatedNutrition,
            )

        val foodsById = database.foodDao().observeFoods().first().associateBy { it.id }
        val barcodeProduct = database.foodDao().getBarcodeProduct(initialResult.barcode)
        val originalFood = foodsById.getValue(originalFoodId)
        val newFood = foodsById.getValue(newFoodId)
        val originalServings = database.foodDao().getServings(originalFoodId)
        val newServings = database.foodDao().getServings(newFoodId)
        val savedMealItem = database.foodDao().observeMealItems(meal.id).first().single()

        assertEquals(2, foodsById.size)
        org.junit.Assert.assertNotEquals(originalFoodId, newFoodId)
        assertEquals(newFoodId, barcodeProduct?.linkedFoodId)
        assertEquals(originalFoodId, savedMealItem.foodId)

        assertEquals("Strained Greek Yogurt", originalFood.name)
        assertNull(originalFood.brand)
        assertEquals(170.0, originalFood.defaultServingGrams, 0.01)
        assertEquals(61.0, originalFood.caloriesPer100g, 0.01)
        assertEquals(1, originalServings.size)
        assertEquals("170 g", originalServings.single().label)
        assertEquals(170.0, originalServings.single().grams, 0.01)

        assertEquals("Strained Greek Yogurt Plus", newFood.name)
        assertEquals("Example Dairy", newFood.brand)
        assertEquals(200.0, newFood.defaultServingGrams, 0.01)
        assertEquals(65.0, newFood.caloriesPer100g, 0.01)
        assertEquals(1, newServings.size)
        assertEquals("200 g", newServings.single().label)
        assertEquals(200.0, newServings.single().grams, 0.01)
    }

    private fun foundProduct(
        barcode: String = "1234567890123",
        name: String = "Greek Yogurt",
        brand: String? = "Example Dairy",
        servingQuantityGrams: Double? = 170.0,
        rawJson: String = """{"code":"1234567890123"}""",
    ) = ProductLookupResult.Found(
        barcode = barcode,
        name = name,
        brand = brand,
        servingQuantityGrams = servingQuantityGrams,
        nutritionPer100g = nutrition(calories = 59.0, protein = 10.0, carbs = 3.6, fat = 0.4),
        quality = ProductDataQuality.Complete,
        rawJson = rawJson,
    )

    private fun nutrition(
        calories: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
    ) = FoodNutrition(
        caloriesKcal = calories,
        proteinGrams = protein,
        carbsGrams = carbs,
        fatGrams = fat,
    )
}
