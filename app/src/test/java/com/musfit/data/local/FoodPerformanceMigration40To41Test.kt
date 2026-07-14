package com.musfit.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.musfit.core.di.DatabaseModule
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlin.system.measureNanoTime

@RunWith(RobolectricTestRunner::class)
class FoodPerformanceMigration40To41Test {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(MIGRATION_DATABASE_NAME)
        context.deleteDatabase(FRESH_DATABASE_NAME)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(MIGRATION_DATABASE_NAME)
        context.deleteDatabase(FRESH_DATABASE_NAME)
    }

    @Test
    fun migration40To41_addsOnlyTheMeasuredFoodSearchIndexAndRetainsEffectivePlans() {
        createDatabaseFromExportedSchema(version = 40, databaseName = MIGRATION_DATABASE_NAME) { database ->
            seedOneThousandFoods(database)
        }
        val baselineStorageBytes: Long
        val baselineSearchP90Nanos: Long
        openReadOnly(MIGRATION_DATABASE_NAME).use { database ->
            baselineStorageBytes = storageBytes(database)
            baselineSearchP90Nanos = searchP90Nanos(database)
        }

        val room =
            Room.databaseBuilder(context, MusFitDatabase::class.java, MIGRATION_DATABASE_NAME)
                .addMigrations(DatabaseModule.MIGRATION_40_41)
                .addCallback(DatabaseModule.FOOD_PERFORMANCE_INDEX_CALLBACK)
                .build()
        room.openHelper.writableDatabase
        room.close()

        openReadOnly(MIGRATION_DATABASE_NAME).use { database ->
            val optimizedStorageBytes = storageBytes(database)
            val optimizedSearchP90Nanos = searchP90Nanos(database)
            val foodLookupPlan =
                queryPlan(
                    database,
                    "SELECT * FROM foods WHERE accountId = 'local-default' " +
                        "AND name = 'oats' COLLATE NOCASE AND brand = 'pantry' COLLATE NOCASE LIMIT 1",
                )
            assertTrue(foodLookupPlan, foodLookupPlan.contains(FOOD_SEARCH_INDEX, ignoreCase = true))

            val foodOrderPlan =
                queryPlan(
                    database,
                    "SELECT * FROM foods WHERE accountId = 'local-default' " +
                        "ORDER BY name COLLATE NOCASE, brand COLLATE NOCASE, id",
                )
            assertTrue(foodOrderPlan, foodOrderPlan.contains(FOOD_SEARCH_INDEX, ignoreCase = true))
            assertFalse(foodOrderPlan, foodOrderPlan.contains("USE TEMP B-TREE", ignoreCase = true))
            assertTrue(
                "Expected indexed P90 $optimizedSearchP90Nanos ns to improve baseline $baselineSearchP90Nanos ns",
                optimizedSearchP90Nanos < baselineSearchP90Nanos,
            )
            assertTrue(
                "Index storage grew from $baselineStorageBytes to $optimizedStorageBytes bytes",
                optimizedStorageBytes <= baselineStorageBytes + MAX_STORAGE_GROWTH_BYTES,
            )
            assertTrue(queryStrings(database, "SELECT COUNT(*) FROM foods").single() == "1000")

            val foodIndexes = queryStrings(database, "SELECT name FROM pragma_index_list('foods')")
            assertTrue(foodIndexes.toString(), FOOD_SEARCH_INDEX in foodIndexes)
            assertFalse(foodIndexes.toString(), "index_foods_accountId_name" in foodIndexes)
            assertFalse(foodIndexes.toString(), "index_foods_accountId_brand" in foodIndexes)
            println(
                "W3-DATA-01 1000-food search P90: $baselineSearchP90Nanos ns -> " +
                    "$optimizedSearchP90Nanos ns; storage: $baselineStorageBytes -> $optimizedStorageBytes bytes; " +
                    "secondary food indexes: 5 -> 4",
            )

            assertTrue(
                queryPlan(
                    database,
                    "SELECT * FROM meals WHERE accountId = 'local-default' AND dateEpochDay = 20000 " +
                        "AND type = 'breakfast' ORDER BY createdAtEpochMillis",
                ).contains("index_meals_accountId_dateEpochDay_type_createdAtEpochMillis", ignoreCase = true),
            )
            assertTrue(
                queryPlan(
                    database,
                    "SELECT * FROM body_metrics WHERE accountId = 'local-default' AND type = 'weight' " +
                        "AND measuredAtEpochMillis >= 0 ORDER BY measuredAtEpochMillis DESC",
                ).contains("index_body_metrics_accountId_type_measuredAtEpochMillis", ignoreCase = true),
            )
        }
    }

    @Test
    fun productionCallback_addsTheFoodSearchIndexToFreshDatabases() {
        val room =
            Room.databaseBuilder(context, MusFitDatabase::class.java, FRESH_DATABASE_NAME)
                .addCallback(DatabaseModule.FOOD_PERFORMANCE_INDEX_CALLBACK)
                .build()
        room.openHelper.writableDatabase
        room.close()

        openReadOnly(FRESH_DATABASE_NAME).use { database ->
            val indexNames = queryStrings(database, "SELECT name FROM pragma_index_list('foods')")
            assertTrue(indexNames.toString(), FOOD_SEARCH_INDEX in indexNames)
        }
    }

    private fun createDatabaseFromExportedSchema(
        version: Int,
        databaseName: String,
        seed: (SQLiteDatabase) -> Unit = {},
    ) {
        val schemaJson = JSONObject(resolveSchemaFile(version).readText()).getJSONObject("database")
        val databaseFile = context.getDatabasePath(databaseName)
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

    private fun seedOneThousandFoods(database: SQLiteDatabase) {
        database.execSQL(
            "INSERT INTO accounts(id, displayName, authProvider, createdAtEpochMillis, updatedAtEpochMillis) " +
                "VALUES ('local-default', 'Local', 'local', 1, 1)",
        )
        database.beginTransaction()
        try {
            val statement =
                database.compileStatement(
                    "INSERT INTO foods(accountId, id, name, brand, defaultServingGrams, caloriesPer100g, " +
                        "proteinPer100g, carbsPer100g, fatPer100g, createdAtEpochMillis, updatedAtEpochMillis) " +
                        "VALUES ('local-default', ?, ?, ?, 100, 100, 1, 1, 1, 1, 1)",
                )
            repeat(1_000) { index ->
                val suffix = index.toString().padStart(4, '0')
                statement.clearBindings()
                statement.bindString(1, "food-$suffix")
                statement.bindString(2, "Food $suffix")
                statement.bindString(3, "Brand ${index % 10}")
                statement.executeInsert()
            }
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }

    private fun searchP90Nanos(database: SQLiteDatabase): Long {
        val sql =
            "SELECT id FROM foods WHERE accountId = 'local-default' " +
                "AND name = 'food 0999' COLLATE NOCASE AND brand = 'brand 9' COLLATE NOCASE LIMIT 1"
        repeat(10) { queryStrings(database, sql) }
        val samples =
            List(101) {
                measureNanoTime {
                    check(queryStrings(database, sql).single() == "food-0999")
                }
            }.sorted()
        return samples[90]
    }

    private fun storageBytes(database: SQLiteDatabase): Long = queryStrings(database, "PRAGMA page_count").single().toLong() *
        queryStrings(database, "PRAGMA page_size").single().toLong()

    private fun openReadOnly(databaseName: String): SQLiteDatabase = SQLiteDatabase.openDatabase(context.getDatabasePath(databaseName).path, null, SQLiteDatabase.OPEN_READONLY)

    private fun queryPlan(database: SQLiteDatabase, sql: String): String = queryStrings(database, "EXPLAIN QUERY PLAN $sql").joinToString("\n")

    private fun queryStrings(database: SQLiteDatabase, sql: String): List<String> = database.rawQuery(sql, null).use { cursor ->
        buildList {
            while (cursor.moveToNext()) add(cursor.getString(cursor.columnCount - 1))
        }
    }

    private fun resolveSchemaFile(version: Int): File {
        val relative = "schemas/com.musfit.data.local.MusFitDatabase/$version.json"
        return listOf(File(relative), File("app/$relative"), File("../app/$relative")).firstOrNull(File::exists)
            ?: error("Could not find exported Room schema $version")
    }

    private companion object {
        const val MIGRATION_DATABASE_NAME = "food-performance-40-41"
        const val FRESH_DATABASE_NAME = "food-performance-fresh"
        const val FOOD_SEARCH_INDEX = "index_foods_accountId_name_brand_id_nocase"
        const val MAX_STORAGE_GROWTH_BYTES = 128L * 1024L
    }
}
