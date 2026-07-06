package com.musfit.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.musfit.data.local.MusFitDatabase
import com.musfit.data.local.entity.DailyHealthSummaryEntity
import com.musfit.data.local.entity.FoodEntity
import com.musfit.data.local.entity.MealEntity
import com.musfit.data.local.entity.MealItemEntity
import com.musfit.data.remote.food.ProductDataQuality
import com.musfit.data.remote.food.ProductLookupResult
import com.musfit.domain.health.HealthConnectAvailability
import com.musfit.domain.health.HealthConnectStatus
import com.musfit.domain.model.FoodNutrition
import com.musfit.integrations.healthconnect.HealthConnectFoodExportPayload
import com.musfit.integrations.healthconnect.HealthConnectFoodExportResult
import com.musfit.integrations.healthconnect.HealthConnectGateway
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

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
    fun observeBurnedCalories_returnsActiveCaloriesFromDailyHealthSummary() = runTest {
        val date = LocalDate.of(2026, 7, 2)
        database.healthDao().upsertDailySummary(dailyHealthSummary(date, activeCaloriesKcal = 312.0))

        assertEquals(312.0, repository.observeBurnedCalories(date).first(), 0.0)
    }

    @Test
    fun observeBurnedCalories_returnsZeroWhenNoSummaryOrActiveCalories() = runTest {
        val date = LocalDate.of(2026, 7, 2)

        // No summary imported for the date yet.
        assertEquals(0.0, repository.observeBurnedCalories(date).first(), 0.0)

        // Summary present but Health Connect never reported active calories.
        database.healthDao().upsertDailySummary(dailyHealthSummary(date, activeCaloriesKcal = null))
        assertEquals(0.0, repository.observeBurnedCalories(date).first(), 0.0)
    }

    private fun dailyHealthSummary(date: LocalDate, activeCaloriesKcal: Double?) =
        DailyHealthSummaryEntity(
            dateEpochDay = date.toEpochDay(),
            steps = null,
            activeCaloriesKcal = activeCaloriesKcal,
            totalCaloriesKcal = null,
            distanceMeters = null,
            sleepMinutes = null,
            exerciseMinutes = null,
            exerciseSessionCount = null,
            latestWeightKg = null,
            latestBodyFatPercent = null,
            restingHeartRateBpm = null,
            updatedAtEpochMillis = 0L,
        )

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

    @Test
    fun logFood_persistsManualFoodMealItemAndDailyTotals() = runTest {
        val date = LocalDate.of(2026, 6, 20)

        repository.logFood(
            FoodLogInput(
                lookupResult = null,
                barcode = null,
                name = "Oats",
                brand = "Pantry",
                nutritionPer100g = nutrition(calories = 380.0, protein = 13.0, carbs = 67.0, fat = 7.0),
                servingGrams = 100.0,
                mealType = "breakfast",
                quantityGrams = 50.0,
                date = date,
            ),
        )

        val meals = database.foodDao().observeMealsForDate(date.toEpochDay()).first()
        val mealItems = database.foodDao().observeMealItems(meals.single().id).first()
        val foods = database.foodDao().observeFoods().first()
        val totals = repository.observeDailyNutrition(date).first()

        assertEquals("breakfast", meals.single().type)
        assertEquals("Oats", foods.single().name)
        assertEquals("Pantry", foods.single().brand)
        assertEquals(50.0, mealItems.single().quantityGrams, 0.01)
        assertEquals(190.0, totals.caloriesKcal, 0.01)
        assertEquals(6.5, totals.proteinGrams, 0.01)
        assertEquals(33.5, totals.carbsGrams, 0.01)
        assertEquals(3.5, totals.fatGrams, 0.01)
    }

    @Test
    fun logFood_persistsConfirmedBarcodeFoodMealItemAndDailyTotals() = runTest {
        val date = LocalDate.of(2026, 6, 20)
        val result = foundProduct()

        repository.logFood(
            FoodLogInput(
                lookupResult = result,
                barcode = result.barcode,
                name = "Edited Yogurt",
                brand = null,
                nutritionPer100g = nutrition(calories = 61.0, protein = 10.5, carbs = 4.0, fat = 0.5),
                servingGrams = 170.0,
                mealType = "snack",
                quantityGrams = 170.0,
                date = date,
            ),
        )

        val meals = database.foodDao().observeMealsForDate(date.toEpochDay()).first()
        val mealItems = database.foodDao().observeMealItems(meals.single().id).first()
        val barcodeProduct = database.foodDao().getBarcodeProduct(result.barcode)
        val totals = repository.observeDailyNutrition(date).first()

        assertEquals("snack", meals.single().type)
        assertEquals(barcodeProduct?.linkedFoodId, mealItems.single().foodId)
        assertEquals(170.0, mealItems.single().quantityGrams, 0.01)
        assertEquals(103.7, totals.caloriesKcal, 0.01)
        assertEquals(17.85, totals.proteinGrams, 0.01)
    }

    @Test
    fun observeSavedFoods_returnsReusableFoodDatabaseItems() = runTest {
        val date = LocalDate.of(2026, 6, 20)
        repository.logFood(
            FoodLogInput(
                lookupResult = null,
                barcode = null,
                name = "Oats",
                brand = "Pantry",
                nutritionPer100g = nutrition(calories = 380.0, protein = 13.0, carbs = 67.0, fat = 7.0),
                servingGrams = 100.0,
                mealType = "breakfast",
                quantityGrams = 50.0,
                date = date,
            ),
        )

        val savedFoods = repository.observeSavedFoods().first()

        assertEquals(1, savedFoods.size)
        assertEquals("Oats", savedFoods.single().name)
        assertEquals("Pantry", savedFoods.single().brand)
        assertEquals(100.0, savedFoods.single().defaultServingGrams, 0.01)
        assertEquals(380.0, savedFoods.single().nutritionPer100g.caloriesKcal, 0.01)
    }

    @Test
    fun observeFoodDiary_groupsLoggedFoodsByMealAndCalculatesSectionTotals() = runTest {
        val date = LocalDate.of(2026, 6, 20)
        repository.logFood(
            FoodLogInput(
                lookupResult = null,
                barcode = null,
                name = "Oats",
                brand = null,
                nutritionPer100g = nutrition(calories = 380.0, protein = 13.0, carbs = 67.0, fat = 7.0),
                servingGrams = 100.0,
                mealType = "breakfast",
                quantityGrams = 50.0,
                date = date,
            ),
        )
        repository.logFood(
            FoodLogInput(
                lookupResult = null,
                barcode = null,
                name = "Rice bowl",
                brand = null,
                nutritionPer100g = nutrition(calories = 180.0, protein = 6.0, carbs = 32.0, fat = 4.0),
                servingGrams = 100.0,
                mealType = "lunch",
                quantityGrams = 200.0,
                date = date,
            ),
        )

        val diary = repository.observeFoodDiary(date).first()

        assertEquals(550.0, diary.totals.caloriesKcal, 0.01)
        assertEquals(18.5, diary.totals.proteinGrams, 0.01)
        assertEquals(listOf("breakfast", "lunch"), diary.meals.map { it.type })
        assertEquals(190.0, diary.meals.first { it.type == "breakfast" }.totals.caloriesKcal, 0.01)
        assertEquals("Oats", diary.meals.first { it.type == "breakfast" }.entries.single().name)
        assertEquals(360.0, diary.meals.first { it.type == "lunch" }.totals.caloriesKcal, 0.01)
        assertEquals(200.0, diary.meals.first { it.type == "lunch" }.entries.single().quantityGrams, 0.01)
    }

    @Test
    fun observeFoodDiary_ordersMealsDeterministicallyWhenMealsTieOnCreatedAt() = runTest {
        val date = LocalDate.of(2026, 6, 20)
        val dao = database.foodDao()
        dao.upsertFood(
            FoodEntity(
                id = "food-1",
                name = "Test food",
                brand = null,
                defaultServingGrams = 100.0,
                caloriesPer100g = 100.0,
                proteinPer100g = 5.0,
                carbsPer100g = 10.0,
                fatPer100g = 2.0,
                createdAtEpochMillis = 1_000L,
                updatedAtEpochMillis = 1_000L,
            ),
        )
        // Breakfast and lunch are created in the same millisecond, so they tie on
        // createdAtEpochMillis. The lunch item id sorts before the breakfast item id, so an
        // ordering that only tie-breaks on meal_items.id surfaces lunch first. Meal order must
        // still be deterministic regardless of item ids.
        dao.upsertMeal(
            MealEntity("meal-breakfast", date.toEpochDay(), "breakfast", null, 1_000L, 1_000L),
        )
        dao.upsertMeal(
            MealEntity("meal-lunch", date.toEpochDay(), "lunch", null, 1_000L, 1_000L),
        )
        dao.upsertMealItem(
            MealItemEntity(id = "item-2-breakfast", mealId = "meal-breakfast", foodId = "food-1", quantityGrams = 100.0),
        )
        dao.upsertMealItem(
            MealItemEntity(id = "item-1-lunch", mealId = "meal-lunch", foodId = "food-1", quantityGrams = 100.0),
        )

        val diary = repository.observeFoodDiary(date).first()

        assertEquals(listOf("breakfast", "lunch"), diary.meals.map { it.type })
    }

    @Test
    fun logSavedFood_logsExistingFoodWithoutCreatingDuplicateSavedFood() = runTest {
        val date = LocalDate.of(2026, 6, 20)
        repository.logFood(
            FoodLogInput(
                lookupResult = null,
                barcode = null,
                name = "Greek yogurt",
                brand = "Kitchen",
                nutritionPer100g = nutrition(calories = 60.0, protein = 10.0, carbs = 4.0, fat = 1.0),
                servingGrams = 150.0,
                mealType = "breakfast",
                quantityGrams = 150.0,
                date = date,
            ),
        )
        val savedFood = repository.observeSavedFoods().first().single()

        repository.logSavedFood(
            SavedFoodLogInput(
                foodId = savedFood.id,
                mealType = "snack",
                quantityGrams = 75.0,
                date = date,
            ),
        )

        val savedFoods = repository.observeSavedFoods().first()
        val diary = repository.observeFoodDiary(date).first()
        val snack = diary.meals.first { it.type == "snack" }

        assertEquals(1, savedFoods.size)
        assertEquals(savedFood.id, snack.entries.single().foodId)
        assertEquals(45.0, snack.totals.caloriesKcal, 0.01)
    }

    @Test
    fun quickLog_persistsCaloriesWithoutFullFoodDetails() = runTest {
        val date = LocalDate.of(2026, 6, 20)

        repository.quickLog(
            QuickCalorieLogInput(
                mealType = "dinner",
                caloriesKcal = 450.0,
                proteinGrams = 25.0,
                carbsGrams = 40.0,
                fatGrams = 15.0,
                date = date,
            ),
        )

        val diary = repository.observeFoodDiary(date).first()
        val savedFoods = repository.observeSavedFoods().first()
        val dinner = diary.meals.single()

        assertEquals("dinner", dinner.type)
        assertEquals("Quick calories", dinner.entries.single().name)
        assertEquals(450.0, dinner.totals.caloriesKcal, 0.01)
        assertEquals(25.0, diary.totals.proteinGrams, 0.01)
        assertTrue(savedFoods.isEmpty())
    }

    @Test
    fun saveFavoriteQuickLogAndLogPreset_persistsFavoriteStateAndLogsCalories() = runTest {
        val date = LocalDate.of(2026, 6, 20)
        val presetId =
            repository.saveFavoriteQuickLog(
                QuickCaloriePresetInput(
                    name = "Protein snack",
                    caloriesKcal = 320.0,
                    proteinGrams = 25.0,
                    carbsGrams = 18.0,
                    fatGrams = 9.0,
                ),
            )

        val preset = repository.observeQuickCaloriePresets().first().single()

        assertEquals(presetId, preset.id)
        assertEquals("Protein snack", preset.name)
        assertTrue(preset.isFavorite)

        repository.logFavoriteQuickLog(presetId, "snacks", date)
        repository.toggleFavoriteQuickLog(presetId, false)

        val diary = repository.observeFoodDiary(date).first()
        val unfavorited = repository.observeQuickCaloriePresets().first().single()

        assertEquals("snacks", diary.meals.single().type)
        assertEquals(320.0, diary.totals.caloriesKcal, 0.01)
        assertEquals(25.0, diary.totals.proteinGrams, 0.01)
        assertFalse(unfavorited.isFavorite)
    }

    @Test
    fun upsertCustomMealDefinition_persistsMealNameTimeAndSortOrder() = runTest {
        val mealId =
            repository.upsertCustomMealDefinition(
                FoodMealDefinitionInput(
                    mealId = null,
                    name = "Pre-workout",
                    timeMinutes = 16 * 60 + 30,
                    sortOrder = 1,
                ),
            )

        val meal = repository.observeCustomMealDefinitions().first().single()

        assertEquals(mealId, meal.id)
        assertEquals("Pre-workout", meal.name)
        assertEquals(16 * 60 + 30, meal.timeMinutes)
        assertEquals(1, meal.sortOrder)
    }

    @Test
    fun upsertCustomMealDefinition_preservesExplicitMealIdForRenamedDefaultMeal() = runTest {
        val mealId =
            repository.upsertCustomMealDefinition(
                FoodMealDefinitionInput(
                    mealId = "breakfast",
                    name = "Morning",
                    timeMinutes = 8 * 60,
                    sortOrder = 12,
                ),
            )

        val meal = repository.observeCustomMealDefinitions().first().single()

        assertEquals("breakfast", mealId)
        assertEquals("breakfast", meal.id)
        assertEquals("Morning", meal.name)
        assertEquals(8 * 60, meal.timeMinutes)
        assertEquals(12, meal.sortOrder)
    }

    @Test
    fun upsertCustomMealDefinition_persistsHiddenFlag() = runTest {
        val mealId =
            repository.upsertCustomMealDefinition(
                FoodMealDefinitionInput(
                    mealId = null,
                    name = "Late snack",
                    timeMinutes = 22 * 60,
                    sortOrder = 40,
                    isHidden = true,
                ),
            )

        val meal = repository.observeCustomMealDefinitions().first().single()

        assertEquals(mealId, meal.id)
        assertTrue(meal.isHidden)
    }

    @Test
    fun upsertCustomMealDefinition_hidingDefaultMealMaterializesHiddenRow() = runTest {
        repository.upsertCustomMealDefinition(
            FoodMealDefinitionInput(
                mealId = "breakfast",
                name = "Breakfast",
                timeMinutes = null,
                sortOrder = 0,
                isHidden = true,
            ),
        )

        val meal = repository.observeCustomMealDefinitions().first().single()

        assertEquals("breakfast", meal.id)
        assertEquals("Breakfast", meal.name)
        assertTrue(meal.isHidden)
    }

    @Test
    fun updateDiaryEntry_changesQuantityAndMealWithoutDuplicatingSavedFood() = runTest {
        val date = LocalDate.of(2026, 6, 20)
        val mealItemId =
            repository.logFood(
                FoodLogInput(
                    lookupResult = null,
                    barcode = null,
                    name = "Oats",
                    brand = "Pantry",
                    nutritionPer100g = nutrition(calories = 380.0, protein = 13.0, carbs = 67.0, fat = 7.0),
                    servingGrams = 100.0,
                    mealType = "breakfast",
                    quantityGrams = 50.0,
                    date = date,
                ),
            )

        repository.updateDiaryEntry(
            DiaryEntryUpdateInput(
                mealItemId = mealItemId,
                mealType = "lunch",
                quantityGrams = 100.0,
                date = date,
            ),
        )

        val diary = repository.observeFoodDiary(date).first()
        val savedFoods = repository.observeSavedFoods().first()

        assertEquals(1, savedFoods.size)
        assertEquals(listOf("lunch"), diary.meals.map { it.type })
        assertEquals(380.0, diary.totals.caloriesKcal, 0.01)
        assertEquals(13.0, diary.totals.proteinGrams, 0.01)
        assertEquals(100.0, diary.meals.single().entries.single().quantityGrams, 0.01)
    }

    @Test
    fun deleteDiaryEntry_removesDailyTotalsAndKeepsSavedFoodReusable() = runTest {
        val date = LocalDate.of(2026, 6, 20)
        val mealItemId =
            repository.logFood(
                FoodLogInput(
                    lookupResult = null,
                    barcode = null,
                    name = "Rice bowl",
                    brand = null,
                    nutritionPer100g = nutrition(calories = 180.0, protein = 6.0, carbs = 32.0, fat = 4.0),
                    servingGrams = 100.0,
                    mealType = "dinner",
                    quantityGrams = 200.0,
                    date = date,
                ),
            )

        repository.deleteDiaryEntry(mealItemId)

        val diary = repository.observeFoodDiary(date).first()
        val savedFoods = repository.observeSavedFoods().first()

        assertEquals(0.0, diary.totals.caloriesKcal, 0.01)
        assertTrue(diary.meals.isEmpty())
        assertEquals(1, savedFoods.size)
        assertEquals("Rice bowl", savedFoods.single().name)
    }

    @Test
    fun saveConfirmedProduct_persistsImageUrl_andExposesItOnSavedFoods() = runTest {
        val result = foundProduct().copy(imageUrl = "https://images.test/egg.jpg")

        repository.saveConfirmedProduct(
            result = result,
            editedName = "Egg",
            editedBrand = "",
            editedNutrition = nutrition(calories = 155.0, protein = 13.0, carbs = 1.0, fat = 11.0),
        )

        val savedEntity = database.foodDao().observeFoods().first().single()
        assertEquals("https://images.test/egg.jpg", savedEntity.imageUrl)
        val savedFood = repository.observeSavedFoods().first().single()
        assertEquals("https://images.test/egg.jpg", savedFood.imageUrl)
    }

    @Test
    fun recentFoods_returnsDistinctFoodsMostRecentFirst() = runTest {
        val dao = database.foodDao()
        dao.upsertFood(foodEntity(id = "f-egg", name = "Egg"))
        dao.upsertFood(foodEntity(id = "f-ice", name = "Ice cream"))
        dao.upsertMeal(MealEntity(id = "m1", dateEpochDay = 20260, type = "breakfast", notes = null, createdAtEpochMillis = 100, updatedAtEpochMillis = 100))
        dao.upsertMealItem(MealItemEntity(id = "mi1", mealId = "m1", foodId = "f-egg", quantityGrams = 100.0))
        dao.upsertMeal(MealEntity(id = "m2", dateEpochDay = 20260, type = "lunch", notes = null, createdAtEpochMillis = 200, updatedAtEpochMillis = 200))
        dao.upsertMealItem(MealItemEntity(id = "mi2", mealId = "m2", foodId = "f-ice", quantityGrams = 100.0))

        val recents = repository.observeRecentFoods(limit = 10).first()
        assertEquals(listOf("Ice cream", "Egg"), recents.map { it.name })
    }

    @Test
    fun sameAsYesterday_returnsFoodsLoggedForThatMealYesterday() = runTest {
        val dao = database.foodDao()
        val yesterday = LocalDate.of(2026, 6, 21)
        val today = LocalDate.of(2026, 6, 22)
        dao.upsertFood(foodEntity(id = "f-oats", name = "Oats"))
        dao.upsertFood(foodEntity(id = "f-steak", name = "Steak"))
        dao.upsertMeal(MealEntity(id = "mb", dateEpochDay = yesterday.toEpochDay(), type = "breakfast", notes = null, createdAtEpochMillis = 100, updatedAtEpochMillis = 100))
        dao.upsertMealItem(MealItemEntity(id = "mib", mealId = "mb", foodId = "f-oats", quantityGrams = 100.0))
        dao.upsertMeal(MealEntity(id = "md", dateEpochDay = yesterday.toEpochDay(), type = "dinner", notes = null, createdAtEpochMillis = 110, updatedAtEpochMillis = 110))
        dao.upsertMealItem(MealItemEntity(id = "mid", mealId = "md", foodId = "f-steak", quantityGrams = 100.0))

        val same = repository.observeSameAsYesterday(mealType = "breakfast", date = today).first()
        assertEquals(listOf("Oats"), same.map { it.name })
    }

    private fun foodEntity(id: String, name: String) = FoodEntity(
        id = id, name = name, brand = null, defaultServingGrams = 100.0,
        caloriesPer100g = 100.0, proteinPer100g = 1.0, carbsPer100g = 1.0, fatPer100g = 1.0,
        createdAtEpochMillis = 1, updatedAtEpochMillis = 1,
    )

    @Test
    fun upsertSavedFood_createsAndUpdatesReusableDatabaseFood() = runTest {
        val foodId =
            repository.upsertSavedFood(
                SavedFoodUpsertInput(
                    foodId = null,
                    name = "Chicken breast",
                    brand = "Kitchen",
                    defaultServingGrams = 150.0,
                    nutritionPer100g = nutrition(calories = 165.0, protein = 31.0, carbs = 0.0, fat = 3.6),
                ),
            )

        repository.upsertSavedFood(
            SavedFoodUpsertInput(
                foodId = foodId,
                name = "Chicken breast cooked",
                brand = "",
                defaultServingGrams = 120.0,
                nutritionPer100g = nutrition(calories = 170.0, protein = 32.0, carbs = 0.0, fat = 4.0),
            ),
        )

        val savedFood = repository.observeSavedFoods().first().single()
        val servings = database.foodDao().getServings(foodId)

        assertEquals(foodId, savedFood.id)
        assertEquals("Chicken breast cooked", savedFood.name)
        assertNull(savedFood.brand)
        assertEquals(120.0, savedFood.defaultServingGrams, 0.01)
        assertEquals(170.0, savedFood.nutritionPer100g.caloriesKcal, 0.01)
        assertEquals(32.0, savedFood.nutritionPer100g.proteinGrams, 0.01)
        assertEquals(1, servings.size)
        assertEquals("120 g", servings.single().label)
    }

    @Test
    fun deleteSavedFood_removesUnusedFoodFromDatabase() = runTest {
        val foodId =
            repository.upsertSavedFood(
                SavedFoodUpsertInput(
                    foodId = null,
                    name = "Banana",
                    brand = null,
                    defaultServingGrams = 118.0,
                    nutritionPer100g = nutrition(calories = 89.0, protein = 1.1, carbs = 23.0, fat = 0.3),
                ),
            )

        repository.deleteSavedFood(foodId)

        assertTrue(repository.observeSavedFoods().first().isEmpty())
        assertNull(database.foodDao().getFood(foodId))
        assertTrue(database.foodDao().getServings(foodId).isEmpty())
    }

    @Test
    fun deleteSavedFood_blocksFoodThatIsUsedByDiaryEntries() = runTest {
        val date = LocalDate.of(2026, 6, 20)
        repository.logFood(
            FoodLogInput(
                lookupResult = null,
                barcode = null,
                name = "Greek yogurt",
                brand = "Kitchen",
                nutritionPer100g = nutrition(calories = 60.0, protein = 10.0, carbs = 4.0, fat = 1.0),
                servingGrams = 150.0,
                mealType = "breakfast",
                quantityGrams = 150.0,
                date = date,
            ),
        )
        val foodId = repository.observeSavedFoods().first().single().id

        val error = runCatching { repository.deleteSavedFood(foodId) }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals("Food is used in diary entries", error?.message)
        assertEquals(1, repository.observeSavedFoods().first().size)
    }

    @Test
    fun mergeDuplicateFoods_reassignsReferencesAndDeletesDuplicates() = runTest {
        val date = LocalDate.of(2026, 6, 20)
        val primaryFoodId =
            repository.upsertSavedFood(
                SavedFoodUpsertInput(
                    foodId = "food-primary",
                    name = "Oats",
                    brand = "Pantry",
                    defaultServingGrams = 40.0,
                    nutritionPer100g = nutrition(calories = 389.0, protein = 17.0, carbs = 66.0, fat = 7.0),
                ),
            )
        val duplicateFoodId =
            repository.upsertSavedFood(
                SavedFoodUpsertInput(
                    foodId = "food-duplicate",
                    name = "oats",
                    brand = "pantry",
                    defaultServingGrams = 40.0,
                    nutritionPer100g = nutrition(calories = 389.0, protein = 17.0, carbs = 66.0, fat = 7.0),
                ),
            )
        val mealItemId = repository.logSavedFood(SavedFoodLogInput(duplicateFoodId, "breakfast", 40.0, date))
        val templateId = repository.saveMealAsTemplate(date, "breakfast", "Duplicate breakfast")
        val recipeId =
            repository.upsertRecipe(
                RecipeUpsertInput(
                    recipeId = null,
                    name = "Overnight oats",
                    category = "Breakfast",
                    servingName = "Jar",
                    servingGrams = 250.0,
                    ingredients = listOf(RecipeIngredientInput(duplicateFoodId, 40.0)),
                ),
            )

        repository.mergeDuplicateFoods(primaryFoodId, listOf(duplicateFoodId))

        val savedFoods = repository.observeSavedFoods().first()
        val diary = repository.observeFoodDiary(date).first()
        val template = repository.observeMealTemplates().first().single { it.id == templateId }
        val recipe = repository.observeRecipes().first().single { it.id == recipeId }

        assertEquals(listOf(primaryFoodId), savedFoods.map { it.id })
        assertNull(database.foodDao().getFood(duplicateFoodId))
        assertEquals(primaryFoodId, database.foodDao().getMealItem(mealItemId)?.foodId)
        assertEquals(primaryFoodId, diary.meals.single().entries.single().foodId)
        assertEquals(primaryFoodId, template.items.single().foodId)
        assertEquals(primaryFoodId, recipe.ingredients.single().foodId)
    }

    @Test
    fun upsertSavedFood_storesAdvancedFoodDetailsServingsAndPreventsDuplicates() = runTest {
        val input =
            SavedFoodUpsertInput(
                foodId = null,
                name = "Greek yogurt",
                brand = "Kitchen",
                defaultServingGrams = 170.0,
                nutritionPer100g = nutrition(calories = 61.0, protein = 10.0, carbs = 4.0, fat = 1.0),
                nutritionDetailsPer100g = NutritionDetails(
                    fiberGrams = 0.2,
                    sugarGrams = 3.6,
                    saturatedFatGrams = 0.6,
                    sodiumMilligrams = 36.0,
                    potassiumMilligrams = 141.0,
                    calciumMilligrams = 110.0,
                    ironMilligrams = 0.1,
                    vitaminDMicrograms = 0.2,
                    vitaminCMilligrams = 1.0,
                    magnesiumMilligrams = 11.0,
                ),
                servingName = "Cup",
                barcode = "400000000001",
                category = "Dairy",
                isFavorite = true,
                servings = listOf(
                    FoodServingInput(label = "Cup", grams = 170.0),
                    FoodServingInput(label = "Small bowl", grams = 125.0),
                ),
            )

        val foodId = repository.upsertSavedFood(input)
        val duplicateFoodId = repository.upsertSavedFood(input.copy(foodId = null))

        val food = repository.getFoodDetail(foodId)!!

        assertEquals(foodId, duplicateFoodId)
        assertEquals("Greek yogurt", food.name)
        assertEquals("Kitchen", food.brand)
        assertEquals("Dairy", food.category)
        assertEquals("400000000001", food.barcode)
        assertEquals("Cup", food.servingName)
        assertTrue(food.isFavorite)
        assertEquals(0.2, food.nutritionDetailsPer100g.fiberGrams, 0.01)
        assertEquals(3.6, food.nutritionDetailsPer100g.sugarGrams, 0.01)
        assertEquals(0.6, food.nutritionDetailsPer100g.saturatedFatGrams, 0.01)
        assertEquals(36.0, food.nutritionDetailsPer100g.sodiumMilligrams, 0.01)
        assertEquals(141.0, food.nutritionDetailsPer100g.potassiumMilligrams, 0.01)
        assertEquals(110.0, food.nutritionDetailsPer100g.calciumMilligrams, 0.01)
        assertEquals(0.1, food.nutritionDetailsPer100g.ironMilligrams, 0.01)
        assertEquals(0.2, food.nutritionDetailsPer100g.vitaminDMicrograms, 0.01)
        assertEquals(1.0, food.nutritionDetailsPer100g.vitaminCMilligrams, 0.01)
        assertEquals(11.0, food.nutritionDetailsPer100g.magnesiumMilligrams, 0.01)
        assertEquals(listOf("Cup", "Small bowl"), food.servings.map { it.label })
        assertEquals(125.0, food.servings.single { it.label == "Small bowl" }.grams, 0.01)

        val date = LocalDate.of(2026, 6, 21)
        repository.logSavedFood(SavedFoodLogInput(foodId, "breakfast", 85.0, date))

        val diary = repository.observeFoodDiary(date).first()

        assertEquals(119.85, diary.detailTotals.potassiumMilligrams, 0.01)
        assertEquals(93.5, diary.detailTotals.calciumMilligrams, 0.01)
        assertEquals(0.085, diary.detailTotals.ironMilligrams, 0.01)
        assertEquals(0.17, diary.detailTotals.vitaminDMicrograms, 0.01)
        assertEquals(0.85, diary.detailTotals.vitaminCMilligrams, 0.01)
        assertEquals(9.35, diary.detailTotals.magnesiumMilligrams, 0.01)
    }

    @Test
    fun foodGoal_roundTripsUserTargetsAndMode() = runTest {
        repository.updateFoodGoal(
            FoodGoal(
                dailyCaloriesKcal = 2400.0,
                proteinGrams = 180.0,
                carbsGrams = 250.0,
                fatGrams = 80.0,
                fiberGrams = 35.0,
                sugarGrams = 60.0,
                saturatedFatGrams = 22.0,
                sodiumMilligrams = 2300.0,
                mode = FoodGoalMode.MediterraneanStyle,
                includeTrainingCalories = true,
                useNetCarbs = true,
            ),
        )

        val goal = repository.observeFoodGoal().first()

        assertEquals(2400.0, goal.dailyCaloriesKcal, 0.01)
        assertEquals(180.0, goal.proteinGrams, 0.01)
        assertEquals(35.0, goal.fiberGrams, 0.01)
        assertEquals(FoodGoalMode.MediterraneanStyle, goal.mode)
        assertTrue(goal.includeTrainingCalories)
        assertTrue(goal.useNetCarbs)
    }

    @Test
    fun saveMealAsTemplateAndLogTemplate_reusesMealItemsOnAnotherDay() = runTest {
        val sourceDate = LocalDate.of(2026, 6, 20)
        val targetDate = sourceDate.plusDays(1)
        val oatsId =
            repository.upsertSavedFood(
                SavedFoodUpsertInput(
                    foodId = null,
                    name = "Oats",
                    brand = null,
                    defaultServingGrams = 50.0,
                    nutritionPer100g = nutrition(calories = 380.0, protein = 13.0, carbs = 67.0, fat = 7.0),
                ),
            )
        val yogurtId =
            repository.upsertSavedFood(
                SavedFoodUpsertInput(
                    foodId = null,
                    name = "Yogurt",
                    brand = null,
                    defaultServingGrams = 170.0,
                    nutritionPer100g = nutrition(calories = 61.0, protein = 10.0, carbs = 4.0, fat = 1.0),
                ),
            )
        repository.logSavedFood(SavedFoodLogInput(oatsId, "breakfast", 50.0, sourceDate))
        repository.logSavedFood(SavedFoodLogInput(yogurtId, "breakfast", 170.0, sourceDate))

        val templateId = repository.saveMealAsTemplate(sourceDate, "breakfast", "Usual breakfast")
        repository.logMealTemplate(templateId, "lunch", targetDate)

        val template = repository.observeMealTemplates().first().single()
        val targetDiary = repository.observeFoodDiary(targetDate).first()

        assertEquals("Usual breakfast", template.name)
        assertEquals("breakfast", template.mealType)
        assertEquals(listOf("Oats", "Yogurt"), template.items.map { it.foodName })
        assertEquals(listOf("lunch"), targetDiary.meals.map { it.type })
        assertEquals(293.7, targetDiary.totals.caloriesKcal, 0.01)
        assertEquals(listOf("Oats", "Yogurt"), targetDiary.meals.single().entries.map { it.name })
    }

    @Test
    fun toggleFavoriteMealTemplate_persistsFavoriteState() = runTest {
        val date = LocalDate.of(2026, 6, 20)
        val foodId =
            repository.upsertSavedFood(
                SavedFoodUpsertInput(
                    foodId = null,
                    name = "Eggs",
                    brand = null,
                    defaultServingGrams = 100.0,
                    nutritionPer100g = nutrition(calories = 143.0, protein = 12.6, carbs = 0.7, fat = 9.5),
                ),
            )
        repository.logSavedFood(SavedFoodLogInput(foodId, "breakfast", 100.0, date))
        val templateId = repository.saveMealAsTemplate(date, "breakfast", "Protein breakfast")

        repository.toggleFavoriteMealTemplate(templateId, true)
        val favorited = repository.observeMealTemplates().first().single()

        assertTrue(favorited.isFavorite)

        repository.toggleFavoriteMealTemplate(templateId, false)
        val unfavorited = repository.observeMealTemplates().first().single()

        assertFalse(unfavorited.isFavorite)
    }

    @Test
    fun copyMeal_copiesMealItemsBetweenDates() = runTest {
        val sourceDate = LocalDate.of(2026, 6, 20)
        val targetDate = sourceDate.plusDays(1)
        val foodId =
            repository.upsertSavedFood(
                SavedFoodUpsertInput(
                    foodId = null,
                    name = "Rice",
                    brand = null,
                    defaultServingGrams = 100.0,
                    nutritionPer100g = nutrition(calories = 180.0, protein = 6.0, carbs = 32.0, fat = 4.0),
                ),
            )
        repository.logSavedFood(SavedFoodLogInput(foodId, "dinner", 200.0, sourceDate))

        repository.copyMeal(sourceDate, targetDate, "dinner")

        val targetDiary = repository.observeFoodDiary(targetDate).first()

        assertEquals(360.0, targetDiary.totals.caloriesKcal, 0.01)
        assertEquals("Rice", targetDiary.meals.single().entries.single().name)
        assertEquals(200.0, targetDiary.meals.single().entries.single().quantityGrams, 0.01)
    }

    @Test
    fun planSavedFood_keepsLoggedTotalsSeparateAndCanBecomeLogged() = runTest {
        val targetDate = LocalDate.of(2026, 6, 22)
        val foodId =
            repository.upsertSavedFood(
                SavedFoodUpsertInput(
                    foodId = null,
                    name = "Rice",
                    brand = null,
                    defaultServingGrams = 100.0,
                    nutritionPer100g = nutrition(calories = 180.0, protein = 6.0, carbs = 32.0, fat = 4.0),
                ),
            )

        val plannedEntryId = repository.planSavedFood(SavedFoodLogInput(foodId, "dinner", 200.0, targetDate))
        val plannedDiary = repository.observeFoodDiary(targetDate).first()

        assertEquals(0.0, plannedDiary.totals.caloriesKcal, 0.01)
        assertEquals(360.0, plannedDiary.plannedTotals.caloriesKcal, 0.01)
        assertEquals(FoodDiaryEntryStatus.Planned, plannedDiary.meals.single().entries.single().status)

        repository.markDiaryEntryLogged(plannedEntryId)
        val loggedDiary = repository.observeFoodDiary(targetDate).first()

        assertEquals(360.0, loggedDiary.totals.caloriesKcal, 0.01)
        assertEquals(0.0, loggedDiary.plannedTotals.caloriesKcal, 0.01)
        assertEquals(FoodDiaryEntryStatus.Logged, loggedDiary.meals.single().entries.single().status)
    }

    @Test
    fun copyDay_canCopyAllMealsAsPlannedEntries() = runTest {
        val sourceDate = LocalDate.of(2026, 6, 20)
        val targetDate = sourceDate.plusDays(2)
        val oatsId =
            repository.upsertSavedFood(
                SavedFoodUpsertInput(
                    foodId = null,
                    name = "Oats",
                    brand = null,
                    defaultServingGrams = 40.0,
                    nutritionPer100g = nutrition(calories = 389.0, protein = 16.9, carbs = 66.3, fat = 6.9),
                ),
            )
        val chickenId =
            repository.upsertSavedFood(
                SavedFoodUpsertInput(
                    foodId = null,
                    name = "Chicken",
                    brand = null,
                    defaultServingGrams = 150.0,
                    nutritionPer100g = nutrition(calories = 165.0, protein = 31.0, carbs = 0.0, fat = 3.6),
                ),
            )
        repository.logSavedFood(SavedFoodLogInput(oatsId, "breakfast", 40.0, sourceDate))
        repository.logSavedFood(SavedFoodLogInput(chickenId, "lunch", 150.0, sourceDate))

        repository.copyDay(sourceDate, targetDate, FoodDiaryEntryStatus.Planned)

        val targetDiary = repository.observeFoodDiary(targetDate).first()

        assertEquals(0.0, targetDiary.totals.caloriesKcal, 0.01)
        assertEquals(403.1, targetDiary.plannedTotals.caloriesKcal, 0.01)
        assertEquals(listOf("breakfast", "lunch"), targetDiary.meals.map { it.type }.sorted())
        assertEquals(
            listOf(FoodDiaryEntryStatus.Planned, FoodDiaryEntryStatus.Planned),
            targetDiary.meals.flatMap { it.entries }.map { it.status },
        )
    }

    @Test
    fun generateShoppingList_groupsPlannedFoodsAndPreservesCheckedState() = runTest {
        val startDate = LocalDate.of(2026, 6, 22)
        val endDate = startDate.plusDays(2)
        val riceId =
            repository.upsertSavedFood(
                SavedFoodUpsertInput(
                    foodId = null,
                    name = "Rice",
                    brand = null,
                    defaultServingGrams = 100.0,
                    nutritionPer100g = nutrition(calories = 180.0, protein = 6.0, carbs = 32.0, fat = 4.0),
                    category = "Grains",
                ),
            )
        repository.planSavedFood(SavedFoodLogInput(riceId, "dinner", 200.0, startDate))

        val generated = repository.generateShoppingList(startDate, endDate)
        val rice = generated.single { it.category == "Grains" }.items.single()

        assertEquals("Rice", rice.name)
        assertEquals("Grains", rice.category)
        assertEquals(200.0, rice.quantityGrams, 0.01)
        assertFalse(rice.isChecked)
        assertFalse(rice.isManual)

        repository.toggleShoppingListItem(rice.id, true)
        repository.generateShoppingList(startDate, endDate)
        val regenerated = repository.observeShoppingList().first().single { it.category == "Grains" }.items.single()

        assertTrue(regenerated.isChecked)
        assertEquals(200.0, regenerated.quantityGrams, 0.01)
    }

    @Test
    fun generateShoppingList_expandsPlannedRecipeIngredientsAndKeepsManualItems() = runTest {
        val sourceDate = LocalDate.of(2026, 6, 20)
        val planDate = sourceDate.plusDays(1)
        val chickenId =
            repository.upsertSavedFood(
                SavedFoodUpsertInput(
                    foodId = null,
                    name = "Chicken",
                    brand = null,
                    defaultServingGrams = 150.0,
                    nutritionPer100g = nutrition(calories = 165.0, protein = 31.0, carbs = 0.0, fat = 3.6),
                    category = "Protein",
                ),
            )
        val riceId =
            repository.upsertSavedFood(
                SavedFoodUpsertInput(
                    foodId = null,
                    name = "Rice",
                    brand = null,
                    defaultServingGrams = 100.0,
                    nutritionPer100g = nutrition(calories = 180.0, protein = 6.0, carbs = 32.0, fat = 4.0),
                    category = "Grains",
                ),
            )
        val recipeId =
            repository.upsertRecipe(
                RecipeUpsertInput(
                    recipeId = null,
                    name = "Chicken bowl",
                    category = "Dinner",
                    servingName = "Bowl",
                    servingGrams = 300.0,
                    servings = 2.0,
                    cookedYieldGrams = 600.0,
                    ingredients = listOf(
                        RecipeIngredientInput(chickenId, 300.0),
                        RecipeIngredientInput(riceId, 200.0),
                    ),
                ),
            )
        repository.logRecipe(recipeId, "dinner", servings = 1.0, date = sourceDate)
        repository.copyDay(sourceDate, planDate, FoodDiaryEntryStatus.Planned)
        repository.addManualShoppingListItem(
            ManualShoppingListItemInput(
                name = "Sparkling water",
                category = "Drinks",
                quantityGrams = 1000.0,
            ),
        )

        val generated = repository.generateShoppingList(planDate, planDate)

        val protein = generated.single { it.category == "Protein" }.items.single()
        val grains = generated.single { it.category == "Grains" }.items.single()
        val drinks = generated.single { it.category == "Drinks" }.items.single()
        assertEquals("Chicken", protein.name)
        assertEquals(150.0, protein.quantityGrams, 0.01)
        assertEquals("Rice", grains.name)
        assertEquals(100.0, grains.quantityGrams, 0.01)
        assertEquals("Sparkling water", drinks.name)
        assertTrue(drinks.isManual)
    }

    @Test
    fun waterTracking_persistsDailyTotalAndGoal() = runTest {
        val date = LocalDate.of(2026, 6, 22)
        val otherDate = date.plusDays(1)

        repository.logWater(WaterLogInput(date = date, amountMilliliters = 250.0))
        repository.logWater(WaterLogInput(date = date, amountMilliliters = 500.0))
        repository.logWater(WaterLogInput(date = otherDate, amountMilliliters = 1000.0))
        repository.updateWaterGoal(2400.0)

        val summary = repository.observeWaterSummary(date).first()
        val otherSummary = repository.observeWaterSummary(otherDate).first()

        assertEquals(date, summary.date)
        assertEquals(750.0, summary.consumedMilliliters, 0.01)
        assertEquals(2400.0, summary.goalMilliliters, 0.01)
        assertEquals(1000.0, otherSummary.consumedMilliliters, 0.01)
        assertEquals(2400.0, otherSummary.goalMilliliters, 0.01)
    }

    @Test
    fun weeklyFoodSummary_combinesLoggedNutritionAndWaterForSevenDays() = runTest {
        val startDate = LocalDate.of(2026, 6, 22)
        repository.updateFoodGoal(
            FoodGoal(
                dailyCaloriesKcal = 2000.0,
                proteinGrams = 120.0,
                carbsGrams = 220.0,
                fatGrams = 70.0,
                fiberGrams = 30.0,
                sugarGrams = 50.0,
                saturatedFatGrams = 20.0,
                sodiumMilligrams = 2300.0,
                mode = FoodGoalMode.Balanced,
                includeTrainingCalories = false,
                waterGoalMilliliters = 2000.0,
            ),
        )
        repository.logFood(
            FoodLogInput(
                lookupResult = null,
                barcode = null,
                name = "Balanced bowl",
                brand = null,
                nutritionPer100g = nutrition(calories = 2000.0, protein = 120.0, carbs = 220.0, fat = 70.0),
                nutritionDetailsPer100g = NutritionDetails(fiberGrams = 30.0, sodiumMilligrams = 1800.0),
                servingGrams = 100.0,
                mealType = "lunch",
                quantityGrams = 100.0,
                date = startDate,
            ),
        )
        repository.logFood(
            FoodLogInput(
                lookupResult = null,
                barcode = null,
                name = "Light snack",
                brand = null,
                nutritionPer100g = nutrition(calories = 900.0, protein = 30.0, carbs = 120.0, fat = 25.0),
                nutritionDetailsPer100g = NutritionDetails(fiberGrams = 5.0, sodiumMilligrams = 600.0),
                servingGrams = 100.0,
                mealType = "snacks",
                quantityGrams = 100.0,
                date = startDate.plusDays(1),
            ),
        )
        repository.logWater(WaterLogInput(startDate, 2000.0))
        repository.logWater(WaterLogInput(startDate.plusDays(1), 500.0))

        val summary = repository.observeWeeklyFoodSummary(startDate).first()

        assertEquals(startDate, summary.startDate)
        assertEquals(7, summary.days.size)
        assertEquals(2000.0, summary.goal.dailyCaloriesKcal, 0.01)
        assertEquals(2000.0, summary.days[0].diary.totals.caloriesKcal, 0.01)
        assertEquals(30.0, summary.days[0].diary.detailTotals.fiberGrams, 0.01)
        assertEquals(2000.0, summary.days[0].water.consumedMilliliters, 0.01)
        assertEquals(900.0, summary.days[1].diary.totals.caloriesKcal, 0.01)
        assertEquals(500.0, summary.days[1].water.consumedMilliliters, 0.01)
        assertTrue(
            summary.days.drop(2).all { day ->
                day.diary.meals.flatMap { meal -> meal.entries }.none { entry -> entry.status == FoodDiaryEntryStatus.Logged } &&
                    day.water.consumedMilliliters == 0.0
            },
        )
    }

    @Test
    fun foodProgressSummary_combinesLoggedNutritionAndWaterForTwentyEightDays() = runTest {
        val startDate = LocalDate.of(2026, 6, 1)
        repository.updateFoodGoal(
            FoodGoal(
                dailyCaloriesKcal = 2000.0,
                proteinGrams = 120.0,
                carbsGrams = 220.0,
                fatGrams = 70.0,
                fiberGrams = 30.0,
                sugarGrams = 50.0,
                saturatedFatGrams = 20.0,
                sodiumMilligrams = 2300.0,
                mode = FoodGoalMode.Balanced,
                includeTrainingCalories = false,
                waterGoalMilliliters = 2000.0,
            ),
        )
        repository.logFood(
            FoodLogInput(
                lookupResult = null,
                barcode = null,
                name = "Week one bowl",
                brand = null,
                nutritionPer100g = nutrition(calories = 1900.0, protein = 110.0, carbs = 210.0, fat = 60.0),
                nutritionDetailsPer100g = NutritionDetails(fiberGrams = 28.0, sodiumMilligrams = 1900.0),
                servingGrams = 100.0,
                mealType = "lunch",
                quantityGrams = 100.0,
                date = startDate,
            ),
        )
        repository.logFood(
            FoodLogInput(
                lookupResult = null,
                barcode = null,
                name = "Month end bowl",
                brand = null,
                nutritionPer100g = nutrition(calories = 2100.0, protein = 130.0, carbs = 230.0, fat = 75.0),
                nutritionDetailsPer100g = NutritionDetails(fiberGrams = 35.0, sodiumMilligrams = 2200.0),
                servingGrams = 100.0,
                mealType = "dinner",
                quantityGrams = 100.0,
                date = startDate.plusDays(27),
            ),
        )
        repository.logWater(WaterLogInput(startDate, 2000.0))
        repository.logWater(WaterLogInput(startDate.plusDays(27), 1600.0))

        val summary = repository.observeFoodProgressSummary(startDate, dayCount = 28).first()

        assertEquals(startDate, summary.startDate)
        assertEquals(28, summary.days.size)
        assertEquals(2000.0, summary.goal.dailyCaloriesKcal, 0.01)
        assertEquals(1900.0, summary.days.first().diary.totals.caloriesKcal, 0.01)
        assertEquals(2000.0, summary.days.first().water.consumedMilliliters, 0.01)
        assertEquals(2100.0, summary.days.last().diary.totals.caloriesKcal, 0.01)
        assertEquals(1600.0, summary.days.last().water.consumedMilliliters, 0.01)
        assertTrue(summary.days.drop(1).dropLast(1).all { it.diary.totals.caloriesKcal == 0.0 })
    }

    @Test
    fun upsertRecipeAndLogRecipePortion_calculatesRecipeNutrition() = runTest {
        val date = LocalDate.of(2026, 6, 20)
        val chickenId =
            repository.upsertSavedFood(
                SavedFoodUpsertInput(
                    foodId = null,
                    name = "Chicken",
                    brand = null,
                    defaultServingGrams = 150.0,
                    nutritionPer100g = nutrition(calories = 165.0, protein = 31.0, carbs = 0.0, fat = 3.6),
                ),
            )
        val riceId =
            repository.upsertSavedFood(
                SavedFoodUpsertInput(
                    foodId = null,
                    name = "Rice",
                    brand = null,
                    defaultServingGrams = 100.0,
                    nutritionPer100g = nutrition(calories = 180.0, protein = 6.0, carbs = 32.0, fat = 4.0),
                ),
            )

        val recipeId =
            repository.upsertRecipe(
                RecipeUpsertInput(
                    recipeId = null,
                    name = "Chicken rice bowl",
                    category = "Dinner",
                    servingName = "Bowl",
                    servingGrams = 350.0,
                    ingredients = listOf(
                        RecipeIngredientInput(chickenId, quantityGrams = 150.0),
                        RecipeIngredientInput(riceId, quantityGrams = 200.0),
                    ),
                ),
            )
        repository.logRecipe(recipeId, mealType = "dinner", servings = 0.5, date = date)

        val recipe = repository.observeRecipes().first().single()
        val dinner = repository.observeFoodDiary(date).first().meals.single()

        assertEquals("Chicken rice bowl", recipe.name)
        assertEquals(2, recipe.ingredients.size)
        assertEquals(303.75, dinner.totals.caloriesKcal, 0.01)
        assertEquals(29.25, dinner.totals.proteinGrams, 0.01)
        assertEquals("Chicken rice bowl", dinner.entries.single().name)
        assertEquals(175.0, dinner.entries.single().quantityGrams, 0.01)
    }

    @Test
    fun planRecipePortion_persistsPlannedRecipeNutritionWithoutLoggedTotals() = runTest {
        val date = LocalDate.of(2026, 6, 22)
        val chickenId =
            repository.upsertSavedFood(
                SavedFoodUpsertInput(
                    foodId = null,
                    name = "Chicken",
                    brand = null,
                    defaultServingGrams = 150.0,
                    nutritionPer100g = nutrition(calories = 165.0, protein = 31.0, carbs = 0.0, fat = 3.6),
                ),
            )
        val riceId =
            repository.upsertSavedFood(
                SavedFoodUpsertInput(
                    foodId = null,
                    name = "Rice",
                    brand = null,
                    defaultServingGrams = 100.0,
                    nutritionPer100g = nutrition(calories = 180.0, protein = 6.0, carbs = 32.0, fat = 4.0),
                ),
            )
        val recipeId =
            repository.upsertRecipe(
                RecipeUpsertInput(
                    recipeId = null,
                    name = "Chicken rice bowl",
                    category = "Dinner",
                    servingName = "Bowl",
                    servingGrams = 350.0,
                    ingredients = listOf(
                        RecipeIngredientInput(chickenId, quantityGrams = 150.0),
                        RecipeIngredientInput(riceId, quantityGrams = 200.0),
                    ),
                ),
            )

        repository.planRecipe(recipeId, mealType = "dinner", servings = 0.5, date = date)

        val diary = repository.observeFoodDiary(date).first()
        val dinner = diary.meals.single()

        assertEquals(0.0, diary.totals.caloriesKcal, 0.01)
        assertEquals(303.75, diary.plannedTotals.caloriesKcal, 0.01)
        assertEquals(29.25, diary.plannedTotals.proteinGrams, 0.01)
        assertEquals(FoodDiaryEntryStatus.Planned, dinner.entries.single().status)
        assertEquals("Chicken rice bowl", dinner.entries.single().name)
        assertEquals(175.0, dinner.entries.single().quantityGrams, 0.01)
    }

    @Test
    fun upsertRecipeWithCookedYieldAndServingUnits_calculatesPerServingNutritionAndLogsFraction() = runTest {
        val date = LocalDate.of(2026, 6, 21)
        val chickenId =
            repository.upsertSavedFood(
                SavedFoodUpsertInput(
                    foodId = null,
                    name = "Chicken",
                    brand = null,
                    defaultServingGrams = 150.0,
                    nutritionPer100g = nutrition(calories = 165.0, protein = 31.0, carbs = 0.0, fat = 3.6),
                    servingName = "Serving",
                    servings = listOf(FoodServingInput(label = "Serving", grams = 150.0)),
                ),
            )
        val riceId =
            repository.upsertSavedFood(
                SavedFoodUpsertInput(
                    foodId = null,
                    name = "Rice",
                    brand = null,
                    defaultServingGrams = 100.0,
                    nutritionPer100g = nutrition(calories = 180.0, protein = 6.0, carbs = 32.0, fat = 4.0),
                    servingName = "Cup",
                    servings = listOf(FoodServingInput(label = "Cup", grams = 100.0)),
                ),
            )

        val recipeId =
            repository.upsertRecipe(
                RecipeUpsertInput(
                    recipeId = null,
                    name = "Meal prep bowl",
                    category = "Dinner",
                    servingName = "Bowl",
                    servingGrams = 180.0,
                    servings = 4.0,
                    cookedYieldGrams = 720.0,
                    ingredients = listOf(
                        RecipeIngredientInput(
                            foodId = chickenId,
                            quantityGrams = 300.0,
                            unitLabel = "Serving",
                            unitGrams = 150.0,
                            unitQuantity = 2.0,
                        ),
                        RecipeIngredientInput(
                            foodId = riceId,
                            quantityGrams = 200.0,
                            unitLabel = "Cup",
                            unitGrams = 100.0,
                            unitQuantity = 2.0,
                        ),
                    ),
                ),
            )
        repository.logRecipe(recipeId, mealType = "dinner", servings = 0.5, date = date)

        val recipe = repository.observeRecipes().first().single()
        val dinner = repository.observeFoodDiary(date).first().meals.single()

        assertEquals(4.0, recipe.servings, 0.01)
        assertEquals(720.0, recipe.cookedYieldGrams, 0.01)
        assertEquals(180.0, recipe.servingGrams, 0.01)
        assertEquals(213.75, recipe.nutritionPerServing.caloriesKcal, 0.01)
        assertEquals(26.25, recipe.nutritionPerServing.proteinGrams, 0.01)
        assertEquals("Serving", recipe.ingredients.first { it.foodId == chickenId }.unitLabel)
        assertEquals(150.0, recipe.ingredients.first { it.foodId == chickenId }.unitGrams, 0.01)
        assertEquals(2.0, recipe.ingredients.first { it.foodId == chickenId }.unitQuantity, 0.01)
        assertEquals("Cup", recipe.ingredients.first { it.foodId == riceId }.unitLabel)
        assertEquals(100.0, recipe.ingredients.first { it.foodId == riceId }.unitGrams, 0.01)
        assertEquals(2.0, recipe.ingredients.first { it.foodId == riceId }.unitQuantity, 0.01)
        assertEquals(106.875, dinner.totals.caloriesKcal, 0.01)
        assertEquals(13.125, dinner.totals.proteinGrams, 0.01)
        assertEquals(90.0, dinner.entries.single().quantityGrams, 0.01)
    }

    @Test
    fun toggleFavoriteRecipe_persistsFavoriteState() = runTest {
        val chickenId =
            repository.upsertSavedFood(
                SavedFoodUpsertInput(
                    foodId = null,
                    name = "Chicken",
                    brand = null,
                    defaultServingGrams = 150.0,
                    nutritionPer100g = nutrition(calories = 165.0, protein = 31.0, carbs = 0.0, fat = 3.6),
                ),
            )
        val recipeId =
            repository.upsertRecipe(
                RecipeUpsertInput(
                    recipeId = null,
                    name = "Chicken bowl",
                    category = "Dinner",
                    servingName = "Bowl",
                    servingGrams = 350.0,
                    ingredients = listOf(RecipeIngredientInput(chickenId, quantityGrams = 150.0)),
                ),
            )

        repository.toggleFavoriteRecipe(recipeId, true)
        val favorited = repository.observeRecipes().first().single()

        assertTrue(favorited.isFavorite)

        repository.toggleFavoriteRecipe(recipeId, false)
        val unfavorited = repository.observeRecipes().first().single()

        assertFalse(unfavorited.isFavorite)
    }

    @Test
    fun duplicateRecipe_copiesIngredientsYieldServingsAndFavoriteState() = runTest {
        val chickenId =
            repository.upsertSavedFood(
                SavedFoodUpsertInput(
                    foodId = null,
                    name = "Chicken",
                    brand = null,
                    defaultServingGrams = 150.0,
                    nutritionPer100g = nutrition(calories = 165.0, protein = 31.0, carbs = 0.0, fat = 3.6),
                ),
            )
        val recipeId =
            repository.upsertRecipe(
                RecipeUpsertInput(
                    recipeId = null,
                    name = "Chicken tray bake",
                    category = "Dinner",
                    servingName = "Plate",
                    servingGrams = 200.0,
                    servings = 3.0,
                    cookedYieldGrams = 600.0,
                    ingredients = listOf(
                        RecipeIngredientInput(
                            foodId = chickenId,
                            quantityGrams = 300.0,
                            unitLabel = "Serving",
                            unitGrams = 150.0,
                            unitQuantity = 2.0,
                        ),
                    ),
                ),
            )
        repository.toggleFavoriteRecipe(recipeId, true)

        val duplicateId = repository.duplicateRecipe(recipeId, "Chicken tray bake copy")
        val recipes = repository.observeRecipes().first()
        val duplicate = recipes.single { it.id == duplicateId }

        assertTrue(duplicate.id != recipeId)
        assertEquals("Chicken tray bake copy", duplicate.name)
        assertEquals("Dinner", duplicate.category)
        assertEquals("Plate", duplicate.servingName)
        assertEquals(200.0, duplicate.servingGrams, 0.01)
        assertEquals(3.0, duplicate.servings, 0.01)
        assertEquals(600.0, duplicate.cookedYieldGrams, 0.01)
        assertTrue(duplicate.isFavorite)
        assertEquals(chickenId, duplicate.ingredients.single().foodId)
        assertEquals(300.0, duplicate.ingredients.single().quantityGrams, 0.01)
        assertEquals("Serving", duplicate.ingredients.single().unitLabel)
        assertEquals(150.0, duplicate.ingredients.single().unitGrams, 0.01)
        assertEquals(2.0, duplicate.ingredients.single().unitQuantity, 0.01)
    }

    @Test
    fun confirmedProductPersistsAdvancedNutritionCategoryAndSearchServing() = runTest {
        val result =
            foundProduct(
                barcode = "5000108236832",
                name = "Oats so simple",
                brand = "Quaker",
                servingQuantityGrams = null,
                rawJson = """{"code":"5000108236832"}""",
            ).copy(
                nutritionDetailsPer100g = NutritionDetails(
                    fiberGrams = 7.2,
                    sugarGrams = 19.0,
                    saturatedFatGrams = 1.2,
                    sodiumMilligrams = 20.0,
                ),
                category = "Breakfast cereals",
                imageUrl = "https://images.openfoodfacts.org/oats.jpg",
            )

        val foodId =
            repository.saveConfirmedProduct(
                result = result,
                editedName = result.name,
                editedBrand = result.brand,
                editedNutrition = result.nutritionPer100g,
            )

        val savedFood = repository.getFoodDetail(foodId)!!

        assertEquals("Breakfast cereals", savedFood.category)
        assertEquals("5000108236832", savedFood.barcode)
        assertEquals(7.2, savedFood.nutritionDetailsPer100g.fiberGrams, 0.01)
        assertEquals(19.0, savedFood.nutritionDetailsPer100g.sugarGrams, 0.01)
        assertEquals(1.2, savedFood.nutritionDetailsPer100g.saturatedFatGrams, 0.01)
        assertEquals(20.0, savedFood.nutritionDetailsPer100g.sodiumMilligrams, 0.01)
        assertEquals(100.0, savedFood.defaultServingGrams, 0.01)
        assertEquals(listOf("100 g"), savedFood.servings.map { it.label })
    }

    @Test
    fun templateManagement_renamesDuplicatesAndDeletesTemplates() = runTest {
        val sourceDate = LocalDate.of(2026, 6, 20)
        val foodId =
            repository.upsertSavedFood(
                SavedFoodUpsertInput(
                    foodId = null,
                    name = "Oats",
                    brand = null,
                    defaultServingGrams = 40.0,
                    nutritionPer100g = nutrition(calories = 389.0, protein = 16.9, carbs = 66.3, fat = 6.9),
                ),
            )
        repository.logSavedFood(SavedFoodLogInput(foodId, "breakfast", 40.0, sourceDate))
        val templateId = repository.saveMealAsTemplate(sourceDate, "breakfast", "Old breakfast")

        repository.renameMealTemplate(templateId, "Workout breakfast", "snacks")
        val duplicateId = repository.duplicateMealTemplate(templateId, "Workout breakfast copy")
        repository.deleteMealTemplate(templateId)

        val templates = repository.observeMealTemplates().first()

        assertEquals(listOf(duplicateId), templates.map { it.id })
        assertEquals("Workout breakfast copy", templates.single().name)
        assertEquals("snacks", templates.single().mealType)
        assertEquals("Oats", templates.single().items.single().foodName)
        assertEquals(40.0, templates.single().items.single().quantityGrams, 0.01)
    }

    @Test
    fun updateMealTemplate_replacesItemsAndLogUsesEditedRows() = runTest {
        val sourceDate = LocalDate.of(2026, 6, 20)
        val targetDate = sourceDate.plusDays(1)
        val oatsId =
            repository.upsertSavedFood(
                SavedFoodUpsertInput(
                    foodId = null,
                    name = "Oats",
                    brand = null,
                    defaultServingGrams = 50.0,
                    nutritionPer100g = nutrition(calories = 380.0, protein = 13.0, carbs = 67.0, fat = 7.0),
                ),
            )
        val yogurtId =
            repository.upsertSavedFood(
                SavedFoodUpsertInput(
                    foodId = null,
                    name = "Yogurt",
                    brand = null,
                    defaultServingGrams = 170.0,
                    nutritionPer100g = nutrition(calories = 61.0, protein = 10.0, carbs = 4.0, fat = 1.0),
                ),
            )
        val eggsId =
            repository.upsertSavedFood(
                SavedFoodUpsertInput(
                    foodId = null,
                    name = "Eggs",
                    brand = null,
                    defaultServingGrams = 100.0,
                    nutritionPer100g = nutrition(calories = 143.0, protein = 12.6, carbs = 0.7, fat = 9.5),
                ),
            )
        repository.logSavedFood(SavedFoodLogInput(oatsId, "breakfast", 50.0, sourceDate))
        repository.logSavedFood(SavedFoodLogInput(yogurtId, "breakfast", 170.0, sourceDate))
        val templateId = repository.saveMealAsTemplate(sourceDate, "breakfast", "Old breakfast")

        repository.updateMealTemplate(
            MealTemplateUpdateInput(
                templateId = templateId,
                name = "Training breakfast",
                mealType = "snacks",
                items = listOf(
                    MealTemplateItemInput(foodId = yogurtId, quantityGrams = 100.0),
                    MealTemplateItemInput(foodId = eggsId, quantityGrams = 200.0),
                ),
            ),
        )
        repository.logMealTemplate(templateId, mealType = "dinner", date = targetDate)

        val template = repository.observeMealTemplates().first().single()
        val dinner = repository.observeFoodDiary(targetDate).first().meals.single()

        assertEquals("Training breakfast", template.name)
        assertEquals("snacks", template.mealType)
        assertEquals(listOf("Yogurt", "Eggs"), template.items.map { it.foodName })
        assertEquals(listOf(100.0, 200.0), template.items.map { it.quantityGrams })
        assertEquals("dinner", dinner.type)
        assertEquals(listOf("Yogurt", "Eggs"), dinner.entries.map { it.name })
        assertEquals(listOf(100.0, 200.0), dinner.entries.map { it.quantityGrams })
    }

    @Test
    fun recipeManagement_updatesDeletesAndCopiesDiaryEntries() = runTest {
        val date = LocalDate.of(2026, 6, 20)
        val chickenId =
            repository.upsertSavedFood(
                SavedFoodUpsertInput(
                    foodId = null,
                    name = "Chicken",
                    brand = null,
                    defaultServingGrams = 150.0,
                    nutritionPer100g = nutrition(calories = 165.0, protein = 31.0, carbs = 0.0, fat = 3.6),
                ),
            )
        val recipeId =
            repository.upsertRecipe(
                RecipeUpsertInput(
                    recipeId = null,
                    name = "Chicken bowl",
                    category = "Dinner",
                    servingName = "Bowl",
                    servingGrams = 300.0,
                    ingredients = listOf(RecipeIngredientInput(chickenId, 150.0)),
                ),
            )

        repository.upsertRecipe(
            RecipeUpsertInput(
                recipeId = recipeId,
                name = "Chicken power bowl",
                category = "Lunch",
                servingName = "Plate",
                servingGrams = 320.0,
                ingredients = listOf(RecipeIngredientInput(chickenId, 200.0)),
            ),
        )
        val mealItemId = repository.logSavedFood(SavedFoodLogInput(chickenId, "lunch", 150.0, date))
        repository.copyDiaryEntry(mealItemId, "dinner", date.plusDays(1))
        repository.deleteRecipe(recipeId)

        val targetDiary = repository.observeFoodDiary(date.plusDays(1)).first()

        assertTrue(repository.observeRecipes().first().isEmpty())
        assertEquals("dinner", targetDiary.meals.single().type)
        assertEquals("Chicken", targetDiary.meals.single().entries.single().name)
        assertEquals(150.0, targetDiary.meals.single().entries.single().quantityGrams, 0.01)
    }

    @Test
    fun foodHealthConnectSync_persistsToggleAndExportsLoggedMealsAndWater() = runTest {
        val gateway = FakeHealthConnectGateway()
        repository =
            LocalFoodRepository(
                database = database,
                foodDao = database.foodDao(),
                healthConnectGateway = gateway,
            )
        val date = LocalDate.of(2026, 6, 20)
        val foodId =
            repository.upsertSavedFood(
                SavedFoodUpsertInput(
                    foodId = null,
                    name = "Oats",
                    brand = null,
                    defaultServingGrams = 50.0,
                    nutritionPer100g = nutrition(calories = 380.0, protein = 13.0, carbs = 67.0, fat = 7.0),
                    nutritionDetailsPer100g = NutritionDetails(
                        fiberGrams = 10.0,
                        sugarGrams = 1.0,
                        sodiumMilligrams = 6.0,
                    ),
                ),
            )
        repository.logSavedFood(SavedFoodLogInput(foodId, "breakfast", 50.0, date))
        repository.logWater(WaterLogInput(date = date, amountMilliliters = 500.0))

        repository.setFoodHealthConnectSyncEnabled(true)
        val result = repository.syncFoodToHealthConnect(date)
        val state = repository.observeFoodHealthConnectSyncState().first()

        assertEquals(FoodHealthConnectSyncResult(nutritionRecordCount = 1, hydrationRecordCount = 1), result)
        assertEquals(true, state.isEnabled)
        assertEquals(HealthConnectAvailability.Available, state.availability)
        assertEquals(null, state.lastFailureMessage)
        assertNotNull(state.lastSyncAtEpochMillis)
        assertEquals(date, gateway.lastPayload?.date)
        val meal = requireNotNull(gateway.lastPayload).meals.single()
        assertEquals("breakfast", meal.mealType)
        assertEquals(190.0, meal.caloriesKcal, 0.01)
        assertEquals(6.5, meal.proteinGrams, 0.01)
        assertEquals(5.0, meal.fiberGrams, 0.01)
        assertEquals(500.0, gateway.lastPayload?.hydrationMilliliters ?: 0.0, 0.01)
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
        nutritionDetailsPer100g = NutritionDetails(),
        category = null,
        imageUrl = null,
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

    private class FakeHealthConnectGateway : HealthConnectGateway {
        private val foodPermissions = setOf("write-nutrition", "write-hydration")
        var lastPayload: HealthConnectFoodExportPayload? = null

        override suspend fun status(): HealthConnectStatus =
            HealthConnectStatus(
                availability = HealthConnectAvailability.Available,
                grantedPermissions = foodPermissions,
            )

        override suspend fun requestablePermissions(): Set<String> = emptySet()

        override suspend fun foodRequestablePermissions(): Set<String> = foodPermissions

        override suspend fun readDailySummary(date: LocalDate) =
            com.musfit.domain.health.ImportedDailyHealthSummary()

        override suspend fun exportWorkout(
            session: com.musfit.data.local.entity.WorkoutSessionEntity,
            sets: List<com.musfit.data.local.entity.WorkoutSetEntity>,
        ): String? = null

        override suspend fun exportFood(payload: HealthConnectFoodExportPayload): HealthConnectFoodExportResult {
            lastPayload = payload
            return HealthConnectFoodExportResult(
                nutritionRecordCount = payload.meals.size,
                hydrationRecordCount = if (payload.hydrationMilliliters > 0.0) 1 else 0,
            )
        }
    }
}
