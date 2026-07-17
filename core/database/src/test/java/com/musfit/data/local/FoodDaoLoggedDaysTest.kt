package com.musfit.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.musfit.data.local.entity.AccountEntity
import com.musfit.data.local.entity.FoodEntity
import com.musfit.data.local.entity.MealEntity
import com.musfit.data.local.entity.MealItemEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FoodDaoLoggedDaysTest {
    private lateinit var database: MusFitDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MusFitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun insertParentFood() {
        // meal_items has FOREIGN KEY(foodId) → foods(id) and Room enables PRAGMA foreign_keys —
        // a parent foods row must exist or every meal_items insert throws SQLiteConstraintException.
        database.accountDao().upsertAccount(
            AccountEntity(ACCOUNT_ID, "Test", null, null, "local", null, 1, 1),
        )
        database.foodDao().upsertFood(
            FoodEntity(
                accountId = ACCOUNT_ID,
                id = "food-1",
                name = "Test food",
                brand = null,
                defaultServingGrams = 100.0,
                caloriesPer100g = 100.0,
                proteinPer100g = 10.0,
                carbsPer100g = 10.0,
                fatPer100g = 5.0,
                createdAtEpochMillis = 1,
                updatedAtEpochMillis = 1,
            ),
        )
    }

    private suspend fun insertMealWithItem(mealId: String, epochDay: Long, status: String) {
        val dao = database.foodDao()
        dao.upsertMeal(
            MealEntity(
                accountId = ACCOUNT_ID,
                id = mealId,
                dateEpochDay = epochDay,
                type = "breakfast",
                notes = null,
                createdAtEpochMillis = 1,
                updatedAtEpochMillis = 1,
            ),
        )
        dao.upsertMealItem(
            MealItemEntity(
                accountId = ACCOUNT_ID,
                id = "$mealId-item",
                mealId = mealId,
                foodId = "food-1",
                quantityGrams = 100.0,
                status = status,
            ),
        )
    }

    @Test
    fun observeLoggedDayEpochDays_returnsDistinctLoggedDaysNewestFirst() = runTest {
        insertParentFood()
        insertMealWithItem("m1", 20_000L, "logged")
        insertMealWithItem("m2", 20_001L, "logged")
        insertMealWithItem("m2b", 20_001L, "logged") // same day twice → distinct
        insertMealWithItem("m3", 20_002L, "planned") // planned-only day excluded
        insertMealWithItem("m0", 19_000L, "logged") // before the window → excluded
        database.foodDao().upsertMealItem(
            // mixed day: planned item alongside m1's logged item → day still appears exactly once
            MealItemEntity(
                accountId = ACCOUNT_ID,
                id = "m1-planned-item",
                mealId = "m1",
                foodId = "food-1",
                quantityGrams = 50.0,
                status = "planned",
            ),
        )

        val days = database.foodDao().observeLoggedDayEpochDays(accountId = ACCOUNT_ID, fromEpochDay = 19_500L).first()

        assertEquals(listOf(20_001L, 20_000L), days)
    }

    private companion object {
        const val ACCOUNT_ID = "food-days-account"
    }
}
