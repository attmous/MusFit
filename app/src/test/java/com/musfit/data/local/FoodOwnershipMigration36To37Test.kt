package com.musfit.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.musfit.core.di.DatabaseModule
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class FoodOwnershipMigration36To37Test {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DATABASE_NAME)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun migration36To37_assignsEveryFoodRowToLocalDefaultAndPreservesRelations() {
        createDatabaseFromExportedSchema(36) { database -> seedCompleteFoodGraph(database) }

        val room = Room.databaseBuilder(context, MusFitDatabase::class.java, DATABASE_NAME)
            .addMigrations(DatabaseModule.MIGRATION_36_37, DatabaseModule.MIGRATION_37_38, DatabaseModule.MIGRATION_38_39, DatabaseModule.MIGRATION_39_40, DatabaseModule.MIGRATION_40_41, DatabaseModule.MIGRATION_41_42)
            .build()
        room.openHelper.writableDatabase
        room.close()

        SQLiteDatabase.openDatabase(
            context.getDatabasePath(DATABASE_NAME).path,
            null,
            SQLiteDatabase.OPEN_READONLY,
        ).use { database ->
            OWNED_TABLES.forEach { table ->
                assertEquals(table, 1, intValue(database, "SELECT COUNT(*) FROM `$table` WHERE accountId = 'local-default'"))
            }
            assertEquals(
                1,
                intValue(
                    database,
                    """
                    SELECT COUNT(*) FROM meal_items i
                    JOIN meals m ON m.accountId = i.accountId AND m.id = i.mealId
                    JOIN foods f ON f.accountId = i.accountId AND f.id = i.foodId
                    JOIN food_servings s ON s.accountId = f.accountId AND s.foodId = f.id
                    WHERE i.id = 'item-legacy'
                    """.trimIndent(),
                ),
            )
            assertEquals(0, intValue(database, "SELECT COUNT(*) FROM pragma_foreign_key_check"))
            assertTrue(
                queryPlan(database, "SELECT * FROM meals WHERE accountId = 'local-default' AND dateEpochDay = 20000")
                    .contains("index_meals_accountId_dateEpochDay", ignoreCase = true),
            )
            assertTrue(
                queryPlan(database, "SELECT * FROM water_entries WHERE accountId = 'local-default' AND dateEpochDay = 20000")
                    .contains("index_water_entries_accountId_dateEpochDay", ignoreCase = true),
            )
        }
    }

    private fun seedCompleteFoodGraph(database: SQLiteDatabase) {
        database.execSQL(
            """
            INSERT INTO foods (id, name, brand, defaultServingGrams, caloriesPer100g, proteinPer100g,
                carbsPer100g, fatPer100g, createdAtEpochMillis, updatedAtEpochMillis)
            VALUES ('food-legacy', 'Oats', NULL, 40, 389, 17, 66, 7, 1, 1)
            """.trimIndent(),
        )
        database.execSQL("INSERT INTO food_servings VALUES ('serving-legacy', 'food-legacy', 'Bowl', 40)")
        database.execSQL("INSERT INTO meals VALUES ('meal-legacy', 20000, 'breakfast', NULL, 1, 1)")
        database.execSQL("INSERT INTO meal_definitions VALUES ('breakfast', 'Breakfast', 480, 0, 1, 1, 0)")
        database.execSQL("INSERT INTO meal_items VALUES ('item-legacy', 'meal-legacy', 'food-legacy', 40, 'logged')")
        database.execSQL("INSERT INTO shopping_list_items VALUES ('shop-legacy', 'Oats', 'Grains', 40, 0, 0, 'food:food-legacy', 0, 1, 1)")
        database.execSQL("INSERT INTO water_entries VALUES ('water-legacy', 20000, 500, 1)")
        database.execSQL("INSERT INTO food_health_connect_sync VALUES ('food', 1, NULL, NULL, 1)")
        database.execSQL("INSERT INTO barcode_products VALUES ('barcode-legacy', '123', 'test', 'Oats', NULL, '{}', 'verified', 'food-legacy', 1)")
        database.execSQL("INSERT INTO food_goals VALUES ('default', 2000, 100, 200, 70, 30, 50, 20, 2300, 'Balanced', 0, 0, 2000, 1)")
        database.execSQL("INSERT INTO quick_calorie_presets VALUES ('quick-legacy', 'Shake', 200, 20, 10, 5, 1, 1, 1)")
        database.execSQL("INSERT INTO meal_templates VALUES ('template-legacy', 'Breakfast', 'breakfast', 1, 1, 1)")
        database.execSQL("INSERT INTO meal_template_items VALUES ('template-item-legacy', 'template-legacy', 'food-legacy', 40, 0)")
        database.execSQL("INSERT INTO recipes VALUES ('recipe-legacy', 'Oats', NULL, 'Bowl', 40, 1, 40, 1, 1, 1)")
        database.execSQL("INSERT INTO recipe_ingredients VALUES ('ingredient-legacy', 'recipe-legacy', 'food-legacy', 40, 'g', 1, 40, 0)")
    }

    private fun createDatabaseFromExportedSchema(version: Int, seed: (SQLiteDatabase) -> Unit) {
        val schemaJson = JSONObject(resolveSchemaFile(version).readText()).getJSONObject("database")
        val databaseFile = context.getDatabasePath(DATABASE_NAME)
        databaseFile.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(databaseFile, null).use { database ->
            val entities = schemaJson.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val tableName = entity.getString("tableName")
                database.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", tableName))
                val indices = entity.optJSONArray("indices") ?: continue
                for (position in 0 until indices.length()) {
                    database.execSQL(indices.getJSONObject(position).getString("createSql").replace("\${TABLE_NAME}", tableName))
                }
            }
            val setup = schemaJson.getJSONArray("setupQueries")
            for (index in 0 until setup.length()) database.execSQL(setup.getString(index))
            seed(database)
            database.version = version
        }
    }

    private fun intValue(database: SQLiteDatabase, sql: String): Int = database.rawQuery(sql, null).use { cursor ->
        cursor.moveToFirst()
        cursor.getInt(0)
    }

    private fun queryPlan(database: SQLiteDatabase, sql: String): String = database.rawQuery("EXPLAIN QUERY PLAN $sql", null).use { cursor ->
        buildList {
            while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("detail")))
        }.joinToString("\n")
    }

    private fun resolveSchemaFile(version: Int): File {
        val relative = "schemas/com.musfit.data.local.MusFitDatabase/$version.json"
        return listOf(File(relative), File("app/$relative"), File("../app/$relative")).firstOrNull(File::exists)
            ?: error("Could not find exported Room schema $version")
    }

    private companion object {
        const val DATABASE_NAME = "food-own-36-37"
        val OWNED_TABLES =
            listOf(
                "foods", "food_servings", "meals", "meal_definitions", "meal_items",
                "shopping_list_items", "water_entries", "food_health_connect_sync",
                "barcode_products", "food_goals", "quick_calorie_presets", "meal_templates",
                "meal_template_items", "recipes", "recipe_ingredients",
            )
    }
}
