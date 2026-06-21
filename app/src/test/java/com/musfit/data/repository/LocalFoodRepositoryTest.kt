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
        assertEquals(listOf("Cup", "Small bowl"), food.servings.map { it.label })
        assertEquals(125.0, food.servings.single { it.label == "Small bowl" }.grams, 0.01)
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
                mode = FoodGoalMode.HighProtein,
                includeTrainingCalories = true,
            ),
        )

        val goal = repository.observeFoodGoal().first()

        assertEquals(2400.0, goal.dailyCaloriesKcal, 0.01)
        assertEquals(180.0, goal.proteinGrams, 0.01)
        assertEquals(35.0, goal.fiberGrams, 0.01)
        assertEquals(FoodGoalMode.HighProtein, goal.mode)
        assertTrue(goal.includeTrainingCalories)
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
}
