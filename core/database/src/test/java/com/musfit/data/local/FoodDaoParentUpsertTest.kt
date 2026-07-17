package com.musfit.data.local

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.musfit.data.local.dao.FoodDao
import com.musfit.data.local.entity.AccountEntity
import com.musfit.data.local.entity.BarcodeProductEntity
import com.musfit.data.local.entity.FoodEntity
import com.musfit.data.local.entity.FoodServingEntity
import com.musfit.data.local.entity.MealEntity
import com.musfit.data.local.entity.MealItemEntity
import com.musfit.data.local.entity.MealTemplateEntity
import com.musfit.data.local.entity.MealTemplateItemEntity
import com.musfit.data.local.entity.RecipeEntity
import com.musfit.data.local.entity.RecipeIngredientEntity
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FoodDaoParentUpsertTest {
    private lateinit var database: MusFitDatabase
    private lateinit var dao: FoodDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room.inMemoryDatabaseBuilder(context, MusFitDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        dao = database.foodDao()
        runBlocking {
            database.accountDao().upsertAccount(AccountEntity(ACCOUNT_ID, "Test", null, null, "local", null, 1, 1))
            database.accountDao().upsertAccount(AccountEntity(SECOND_ACCOUNT_ID, "Other", null, null, "local", null, 1, 1))
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertFood_withoutRestrictingChildren_preservesServingAndBarcodeLink() = runTest {
        val food = food(id = "food-parent", name = "Oats")
        val serving = FoodServingEntity(ACCOUNT_ID, "serving-parent", food.id, "Bowl", 80.0)
        val barcode = barcodeProduct(id = "barcode-parent", barcode = "400000000001", foodId = food.id)
        dao.upsertFood(food)
        dao.upsertServing(serving)
        dao.upsertBarcodeProduct(barcode)

        dao.upsertFood(food.copy(name = "Rolled oats", updatedAtEpochMillis = 2L))

        assertEquals("Rolled oats", dao.getFood(ACCOUNT_ID, food.id)?.name)
        assertEquals(listOf(serving), dao.getServings(ACCOUNT_ID, food.id))
        assertEquals(food.id, dao.getBarcodeProduct(ACCOUNT_ID, barcode.barcode)?.linkedFoodId)
    }

    @Test
    fun upsertFood_withRestrictingChildren_updatesParentAndPreservesEntireGraph() = runTest {
        val food = food(id = "food-graph", name = "Yogurt")
        val meal = meal(id = "meal-graph")
        val mealItem = MealItemEntity(ACCOUNT_ID, "meal-item-graph", meal.id, food.id, 170.0)
        val template = template(id = "template-graph")
        val templateItem = MealTemplateItemEntity(ACCOUNT_ID, "template-item-graph", template.id, food.id, 170.0, 0)
        val recipe = recipe(id = "recipe-graph")
        val ingredient = RecipeIngredientEntity(ACCOUNT_ID, "ingredient-graph", recipe.id, food.id, 170.0, sortOrder = 0)
        val serving = FoodServingEntity(ACCOUNT_ID, "serving-graph", food.id, "Cup", 170.0)
        val barcode = barcodeProduct(id = "barcode-graph", barcode = "400000000002", foodId = food.id)
        dao.upsertFood(food)
        dao.upsertServing(serving)
        dao.upsertMeal(meal)
        dao.upsertMealItem(mealItem)
        dao.upsertMealTemplate(template)
        dao.upsertMealTemplateItem(templateItem)
        dao.upsertRecipe(recipe)
        dao.upsertRecipeIngredient(ingredient)
        dao.upsertBarcodeProduct(barcode)

        dao.upsertFood(food.copy(name = "Greek yogurt", isFavorite = true, updatedAtEpochMillis = 2L))

        assertEquals("Greek yogurt", dao.getFood(ACCOUNT_ID, food.id)?.name)
        assertEquals(true, dao.getFood(ACCOUNT_ID, food.id)?.isFavorite)
        assertEquals(listOf(serving), dao.getServings(ACCOUNT_ID, food.id))
        assertEquals(mealItem, dao.getMealItem(ACCOUNT_ID, mealItem.id))
        assertEquals(templateItem.id, dao.getMealTemplateRows(ACCOUNT_ID, template.id).single().itemId)
        assertEquals(ingredient.id, dao.getRecipeRows(ACCOUNT_ID, recipe.id).single().ingredientId)
        assertEquals(food.id, dao.getBarcodeProduct(ACCOUNT_ID, barcode.barcode)?.linkedFoodId)
    }

    @Test
    fun upsertMeal_preservesExistingMealItems() = runTest {
        val food = food(id = "food-meal", name = "Banana")
        val meal = meal(id = "meal-parent")
        val item = MealItemEntity(ACCOUNT_ID, "meal-item", meal.id, food.id, 118.0)
        dao.upsertFood(food)
        dao.upsertMeal(meal)
        dao.upsertMealItem(item)

        dao.upsertMeal(meal.copy(notes = "Pre-workout", updatedAtEpochMillis = 2L))

        assertEquals("Pre-workout", dao.getMeal(ACCOUNT_ID, meal.id)?.notes)
        assertEquals(item, dao.getMealItem(ACCOUNT_ID, item.id))
    }

    @Test
    fun upsertMealTemplate_preservesExistingTemplateItems() = runTest {
        val food = food(id = "food-template", name = "Eggs")
        val template = template(id = "template-parent")
        val item = MealTemplateItemEntity(ACCOUNT_ID, "template-item", template.id, food.id, 100.0, 0)
        dao.upsertFood(food)
        dao.upsertMealTemplate(template)
        dao.upsertMealTemplateItem(item)

        dao.upsertMealTemplate(template.copy(name = "Protein breakfast", updatedAtEpochMillis = 2L))

        assertEquals("Protein breakfast", dao.getMealTemplate(ACCOUNT_ID, template.id)?.name)
        assertEquals(item.id, dao.getMealTemplateRows(ACCOUNT_ID, template.id).single().itemId)
    }

    @Test
    fun upsertRecipe_preservesExistingIngredients() = runTest {
        val food = food(id = "food-recipe", name = "Chicken")
        val recipe = recipe(id = "recipe-parent")
        val ingredient = RecipeIngredientEntity(ACCOUNT_ID, "recipe-ingredient", recipe.id, food.id, 150.0, sortOrder = 0)
        dao.upsertFood(food)
        dao.upsertRecipe(recipe)
        dao.upsertRecipeIngredient(ingredient)

        dao.upsertRecipe(recipe.copy(name = "Chicken power bowl", updatedAtEpochMillis = 2L))

        assertEquals("Chicken power bowl", dao.getRecipe(ACCOUNT_ID, recipe.id)?.name)
        val row = dao.getRecipeRows(ACCOUNT_ID, recipe.id).single()
        assertEquals(ingredient.id, row.ingredientId)
        assertEquals(ingredient.quantityGrams, row.quantityGrams, 0.0)
        assertNotNull(dao.getFood(ACCOUNT_ID, row.foodId))
    }

    @Test
    fun upsertMealItem_rejectsFoodOwnedByAnotherAccount() = runTest {
        val food = food(id = "cross-account-food", name = "Private food")
        val otherMeal = meal(id = "other-meal").copy(accountId = SECOND_ACCOUNT_ID)
        dao.upsertFood(food)
        dao.upsertMeal(otherMeal)

        try {
            dao.upsertMealItem(
                MealItemEntity(
                    accountId = SECOND_ACCOUNT_ID,
                    id = "invalid-item",
                    mealId = otherMeal.id,
                    foodId = food.id,
                    quantityGrams = 100.0,
                ),
            )
            fail("Cross-account relation should violate the composite food foreign key")
        } catch (_: SQLiteConstraintException) {
            // Expected: accountId is part of both parent foreign keys.
        }
    }

    private fun food(id: String, name: String) = FoodEntity(
        accountId = ACCOUNT_ID,
        id = id,
        name = name,
        brand = null,
        defaultServingGrams = 100.0,
        caloriesPer100g = 100.0,
        proteinPer100g = 10.0,
        carbsPer100g = 10.0,
        fatPer100g = 5.0,
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L,
    )

    private fun meal(id: String) = MealEntity(
        accountId = ACCOUNT_ID,
        id = id,
        dateEpochDay = 20_000L,
        type = "breakfast",
        notes = null,
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L,
    )

    private fun template(id: String) = MealTemplateEntity(
        accountId = ACCOUNT_ID,
        id = id,
        name = "Breakfast",
        mealType = "breakfast",
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L,
    )

    private fun recipe(id: String) = RecipeEntity(
        accountId = ACCOUNT_ID,
        id = id,
        name = "Chicken bowl",
        category = "Dinner",
        servingName = "Bowl",
        servingGrams = 300.0,
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L,
    )

    private fun barcodeProduct(id: String, barcode: String, foodId: String) = BarcodeProductEntity(
        accountId = ACCOUNT_ID,
        id = id,
        barcode = barcode,
        provider = "test",
        providerProductName = "Test product",
        providerBrand = null,
        rawJson = "{}",
        quality = "verified",
        linkedFoodId = foodId,
        fetchedAtEpochMillis = 1L,
    )

    private companion object {
        const val ACCOUNT_ID = "food-parent-account"
        const val SECOND_ACCOUNT_ID = "food-parent-account-2"
    }
}
