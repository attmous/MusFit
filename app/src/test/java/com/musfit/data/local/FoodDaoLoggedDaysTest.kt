package com.musfit.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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

    private fun insertParentFood() {
        // meal_items has FOREIGN KEY(foodId) → foods(id) and Room enables PRAGMA foreign_keys —
        // a parent foods row must exist or every meal_items insert throws SQLiteConstraintException.
        database.openHelper.writableDatabase.execSQL(
            "INSERT INTO foods (id, name, brand, defaultServingGrams, caloriesPer100g, proteinPer100g, " +
                "carbsPer100g, fatPer100g, createdAtEpochMillis, updatedAtEpochMillis) " +
                "VALUES ('food-1', 'Test food', NULL, 100.0, 100.0, 10.0, 10.0, 5.0, 1, 1)",
        )
    }

    private fun insertMealWithItem(mealId: String, epochDay: Long, status: String) {
        val db = database.openHelper.writableDatabase
        db.execSQL(
            "INSERT INTO meals (id, dateEpochDay, type, notes, createdAtEpochMillis, updatedAtEpochMillis) " +
                "VALUES ('$mealId', $epochDay, 'breakfast', NULL, 1, 1)",
        )
        db.execSQL(
            "INSERT INTO meal_items (id, mealId, foodId, quantityGrams, status) " +
                "VALUES ('$mealId-item', '$mealId', 'food-1', 100.0, '$status')",
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

        val days = database.foodDao().observeLoggedDayEpochDays(fromEpochDay = 19_500L).first()

        assertEquals(listOf(20_001L, 20_000L), days)
    }
}
