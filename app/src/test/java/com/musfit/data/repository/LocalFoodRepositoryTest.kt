package com.musfit.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.musfit.data.local.MusFitDatabase
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
        repository = LocalFoodRepository(database.foodDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun saveConfirmedProduct_persistsFoodServingAndBarcodeLink() = runTest {
        val result =
            ProductLookupResult.Found(
                barcode = "1234567890123",
                name = "Greek Yogurt",
                brand = "Example Dairy",
                servingQuantityGrams = 170.0,
                nutritionPer100g =
                    FoodNutrition(
                        caloriesKcal = 59.0,
                        proteinGrams = 10.0,
                        carbsGrams = 3.6,
                        fatGrams = 0.4,
                    ),
                quality = ProductDataQuality.Complete,
                rawJson = """{"code":"1234567890123"}""",
            )
        val editedNutrition =
            FoodNutrition(
                caloriesKcal = 61.0,
                proteinGrams = 10.5,
                carbsGrams = 4.0,
                fatGrams = 0.5,
            )

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
    fun saveConfirmedProduct_reusesLinkedFoodForExistingBarcode() = runTest {
        val initialResult =
            ProductLookupResult.Found(
                barcode = "1234567890123",
                name = "Greek Yogurt",
                brand = "Example Dairy",
                servingQuantityGrams = 170.0,
                nutritionPer100g =
                    FoodNutrition(
                        caloriesKcal = 59.0,
                        proteinGrams = 10.0,
                        carbsGrams = 3.6,
                        fatGrams = 0.4,
                    ),
                quality = ProductDataQuality.Complete,
                rawJson = """{"code":"1234567890123","unknown":"first"}""",
            )
        val updatedResult =
            initialResult.copy(
                name = "Greek Yogurt Plain",
                servingQuantityGrams = 200.0,
                rawJson = """{"code":"1234567890123","unknown":"second"}""",
            )

        val firstFoodId =
            repository.saveConfirmedProduct(
                result = initialResult,
                editedName = "Strained Greek Yogurt",
                editedBrand = "",
                editedNutrition =
                    FoodNutrition(
                        caloriesKcal = 61.0,
                        proteinGrams = 10.5,
                        carbsGrams = 4.0,
                        fatGrams = 0.5,
                    ),
            )

        val secondFoodId =
            repository.saveConfirmedProduct(
                result = updatedResult,
                editedName = "Strained Greek Yogurt",
                editedBrand = "Example Dairy",
                editedNutrition =
                    FoodNutrition(
                        caloriesKcal = 65.0,
                        proteinGrams = 11.0,
                        carbsGrams = 4.5,
                        fatGrams = 0.6,
                    ),
            )

        val foods = database.foodDao().observeFoods().first()
        val barcodeProduct = database.foodDao().getBarcodeProduct(initialResult.barcode)
        val servings = database.foodDao().getServings(firstFoodId)

        assertEquals(firstFoodId, secondFoodId)
        assertEquals(1, foods.size)
        assertEquals(firstFoodId, barcodeProduct?.linkedFoodId)
        assertEquals("""{"code":"1234567890123","unknown":"second"}""", barcodeProduct?.rawJson)
        assertEquals(1, servings.size)
        assertEquals("200 g", servings.single().label)
        assertEquals(200.0, servings.single().grams, 0.01)
        assertEquals(200.0, foods.single().defaultServingGrams, 0.01)
        assertEquals("Example Dairy", foods.single().brand)
        assertEquals(65.0, foods.single().caloriesPer100g, 0.01)
    }
}
