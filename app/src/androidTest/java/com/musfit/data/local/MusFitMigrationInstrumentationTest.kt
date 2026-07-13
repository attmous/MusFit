package com.musfit.data.local

import android.database.sqlite.SQLiteConstraintException
import android.os.Build
import android.os.SystemClock
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.musfit.core.di.DatabaseModule
import com.musfit.data.local.entity.AccountEntity
import com.musfit.data.local.entity.FoodEntity
import com.musfit.data.local.entity.FoodServingEntity
import com.musfit.data.local.entity.LOCAL_DEFAULT_ACCOUNT_ID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

private const val LARGE_DATASET_ROWS = 2_500
private const val LARGE_MIGRATION_BUDGET_MILLIS = 30_000L

@RunWith(Parameterized::class)
class MusFitMigrationInstrumentationTest(
    private val originVersion: Int,
) {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            MusFitDatabase::class.java,
        )

    @Test
    fun everyRetainedSchemaMigratesToLatestWithRelationsIntact() {
        val databaseName = "migration-origin-$originVersion"
        val sentinel = MigrationSentinel.forOrigin(originVersion)
        helper.createDatabase(databaseName, originVersion).apply {
            insertSentinelGraph(sentinel)
            close()
        }

        helper.runMigrationsAndValidate(
            databaseName,
            MUSFIT_DATABASE_VERSION,
            true,
            *DatabaseModule.ALL_MIGRATIONS,
        ).use { database ->
            assertEquals(MUSFIT_DATABASE_VERSION.toLong(), database.longValue("PRAGMA user_version"))
            assertEquals(
                sentinel.foodId,
                database.stringValue("SELECT id FROM foods WHERE id = ?", sentinel.foodId),
            )
            assertEquals(
                sentinel.servingId,
                database.stringValue("SELECT id FROM food_servings WHERE id = ?", sentinel.servingId),
            )
            assertEquals(
                sentinel.mealId,
                database.stringValue("SELECT id FROM meals WHERE id = ?", sentinel.mealId),
            )
            assertEquals(
                sentinel.mealItemId,
                database.stringValue("SELECT id FROM meal_items WHERE id = ?", sentinel.mealItemId),
            )
            assertEquals(
                1L,
                database.longValue(
                    """
                    SELECT COUNT(*)
                    FROM meal_items
                    JOIN meals ON meals.id = meal_items.mealId
                    JOIN foods ON foods.id = meal_items.foodId
                    JOIN food_servings ON food_servings.foodId = foods.id
                    WHERE meal_items.id = ?
                    """.trimIndent(),
                    sentinel.mealItemId,
                ),
            )
            database.assertForeignKeysValid()
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "schema {0} -> latest")
        fun retainedOrigins(): List<Int> = (1 until MUSFIT_DATABASE_VERSION).toList()
    }
}

@RunWith(AndroidJUnit4::class)
class MusFitRecentMigrationInstrumentationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            MusFitDatabase::class.java,
        )

    @Test
    fun fiveMostRecentAdjacentMigrationsMatchTheirExportedSchemas() {
        val firstRecentOrigin = (MUSFIT_DATABASE_VERSION - 5).coerceAtLeast(1)
        for (origin in firstRecentOrigin until MUSFIT_DATABASE_VERSION) {
            val databaseName = "adjacent-$origin-${origin + 1}"
            val sentinel = MigrationSentinel.forOrigin(origin)
            helper.createDatabase(databaseName, origin).apply {
                insertSentinelGraph(sentinel)
                close()
            }

            val migration =
                DatabaseModule.ALL_MIGRATIONS.single {
                    it.startVersion == origin && it.endVersion == origin + 1
                }
            helper.runMigrationsAndValidate(databaseName, origin + 1, true, migration).use { database ->
                assertEquals(
                    sentinel.mealItemId,
                    database.stringValue("SELECT id FROM meal_items WHERE id = ?", sentinel.mealItemId),
                )
                database.assertForeignKeysValid()
            }
        }
    }

    @Test
    fun schemaValidatorRejectsDeliberatelyBrokenFixture() {
        // Start two versions behind the schema head so the no-op skips the
        // structural Food ownership migration and Room's exported-schema
        // validator must reject the deliberately broken fixture.
        val origin = MUSFIT_DATABASE_VERSION - 2
        val databaseName = "deliberately-broken-$origin"
        helper.createDatabase(databaseName, origin).close()
        val brokenMigration =
            object : androidx.room.migration.Migration(origin, MUSFIT_DATABASE_VERSION) {
                override fun migrate(db: SupportSQLiteDatabase) = Unit
            }

        val failure =
            assertThrows(IllegalStateException::class.java) {
                helper.runMigrationsAndValidate(
                    databaseName,
                    MUSFIT_DATABASE_VERSION,
                    true,
                    brokenMigration,
                ).close()
            }
        assertTrue(failure.message.orEmpty().contains("Migration", ignoreCase = true))
    }
}

@RunWith(AndroidJUnit4::class)
class MusFitLargeMigrationInstrumentationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            MusFitDatabase::class.java,
        )

    @Test
    fun representativeLargeVersionOneDatabaseMigratesWithinBudget() {
        val databaseName = "large-version-one"
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        helper.createDatabase(databaseName, 1).apply {
            beginTransaction()
            try {
                repeat(LARGE_DATASET_ROWS) { index ->
                    insertSentinelGraph(MigrationSentinel.forOrigin(index + 1))
                }
                setTransactionSuccessful()
            } finally {
                endTransaction()
                close()
            }
        }
        val databaseFile = targetContext.getDatabasePath(databaseName)
        val beforeBytes = databaseFile.length()

        val startedAt = SystemClock.elapsedRealtime()
        helper.runMigrationsAndValidate(
            databaseName,
            MUSFIT_DATABASE_VERSION,
            true,
            *DatabaseModule.ALL_MIGRATIONS,
        ).use { database ->
            val elapsedMillis = SystemClock.elapsedRealtime() - startedAt
            assertEquals(LARGE_DATASET_ROWS.toLong(), database.longValue("SELECT COUNT(*) FROM foods"))
            assertEquals(LARGE_DATASET_ROWS.toLong(), database.longValue("SELECT COUNT(*) FROM food_servings"))
            assertEquals(LARGE_DATASET_ROWS.toLong(), database.longValue("SELECT COUNT(*) FROM meals"))
            assertEquals(LARGE_DATASET_ROWS.toLong(), database.longValue("SELECT COUNT(*) FROM meal_items"))
            database.assertForeignKeysValid()

            val report =
                """{"apiLevel":${Build.VERSION.SDK_INT},"originVersion":1,"targetVersion":$MUSFIT_DATABASE_VERSION,"rowsPerTable":$LARGE_DATASET_ROWS,"totalRows":${LARGE_DATASET_ROWS * 4},"beforeBytes":$beforeBytes,"afterBytes":${databaseFile.length()},"elapsedMillis":$elapsedMillis,"budgetMillis":$LARGE_MIGRATION_BUDGET_MILLIS}"""
            File(targetContext.filesDir, "w2-test-01-migration-api-${Build.VERSION.SDK_INT}.json").writeText(report)
            println("W2_TEST_01_MIGRATION_METRIC $report")
            assertTrue(
                "Migration took $elapsedMillis ms; budget is $LARGE_MIGRATION_BUDGET_MILLIS ms",
                elapsedMillis <= LARGE_MIGRATION_BUDGET_MILLIS,
            )
        }
    }
}

@RunWith(AndroidJUnit4::class)
class MusFitFrameworkDaoInstrumentationTest {
    private lateinit var database: MusFitDatabase

    @After
    fun closeDatabase() {
        if (::database.isInitialized) database.close()
    }

    @Test
    fun frameworkSQLiteEnforcesForeignKeysAndRollsBackMultiDaoTransaction() = runBlocking {
        database =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                MusFitDatabase::class.java,
            ).allowMainThreadQueries().build()
        val foodDao = database.foodDao()
        val food = sentinelFood("transaction-food")
        val serving = FoodServingEntity(LOCAL_DEFAULT_ACCOUNT_ID, "transaction-serving", food.id, "portion", 42.0)
        database.accountDao().upsertAccount(
            AccountEntity(
                id = LOCAL_DEFAULT_ACCOUNT_ID,
                displayName = "Framework SQLite owner",
                email = null,
                remoteUserId = null,
                authProvider = "local",
                avatarUrl = null,
                createdAtEpochMillis = 1_000L,
                updatedAtEpochMillis = 1_000L,
            ),
        )

        assertSuspendThrows(SQLiteConstraintException::class.java) {
            foodDao.upsertServing(serving.copy(foodId = "missing-parent"))
        }

        assertSuspendThrows(RollbackMarker::class.java) {
            database.withTransaction {
                foodDao.upsertFood(food)
                foodDao.upsertServing(serving)
                throw RollbackMarker()
            }
        }
        assertNull(foodDao.getFood(LOCAL_DEFAULT_ACCOUNT_ID, food.id))
        assertTrue(foodDao.getServings(LOCAL_DEFAULT_ACCOUNT_ID, food.id).isEmpty())

        database.withTransaction {
            foodDao.upsertFood(food)
            foodDao.upsertServing(serving)
        }
        assertNotNull(foodDao.getFood(LOCAL_DEFAULT_ACCOUNT_ID, food.id))
        assertEquals(listOf(serving), foodDao.getServings(LOCAL_DEFAULT_ACCOUNT_ID, food.id))
        database.openHelper.writableDatabase.assertForeignKeysValid()
    }

    private class RollbackMarker : RuntimeException()
}

private data class MigrationSentinel(
    val foodId: String,
    val servingId: String,
    val mealId: String,
    val mealItemId: String,
) {
    companion object {
        fun forOrigin(origin: Int): MigrationSentinel = MigrationSentinel(
            foodId = "sentinel-food-$origin",
            servingId = "sentinel-serving-$origin",
            mealId = "sentinel-meal-$origin",
            mealItemId = "sentinel-item-$origin",
        )
    }
}

private fun SupportSQLiteDatabase.insertSentinelGraph(sentinel: MigrationSentinel) {
    execSQL(
        """
        INSERT INTO foods (
            id, name, brand, defaultServingGrams, caloriesPer100g, proteinPer100g,
            carbsPer100g, fatPer100g, createdAtEpochMillis, updatedAtEpochMillis
        ) VALUES (?, ?, NULL, 100.0, 250.0, 12.0, 30.0, 8.0, 1000, 2000)
        """.trimIndent(),
        arrayOf<Any>(sentinel.foodId, "Sentinel ${sentinel.foodId}"),
    )
    execSQL(
        "INSERT INTO food_servings (id, foodId, label, grams) VALUES (?, ?, 'portion', 42.0)",
        arrayOf<Any>(sentinel.servingId, sentinel.foodId),
    )
    execSQL(
        """
        INSERT INTO meals (
            id, dateEpochDay, type, notes, createdAtEpochMillis, updatedAtEpochMillis
        ) VALUES (?, 20000, 'breakfast', 'sentinel', 1000, 2000)
        """.trimIndent(),
        arrayOf<Any>(sentinel.mealId),
    )
    execSQL(
        "INSERT INTO meal_items (id, mealId, foodId, quantityGrams) VALUES (?, ?, ?, 84.0)",
        arrayOf<Any>(sentinel.mealItemId, sentinel.mealId, sentinel.foodId),
    )
}

private fun SupportSQLiteDatabase.longValue(
    sql: String,
    vararg args: Any,
): Long = query(SimpleSQLiteQuery(sql, args)).use { cursor ->
    assertTrue("Expected one row for: $sql", cursor.moveToFirst())
    cursor.getLong(0)
}

private fun SupportSQLiteDatabase.stringValue(
    sql: String,
    vararg args: Any,
): String = query(SimpleSQLiteQuery(sql, args)).use { cursor ->
    assertTrue("Expected one row for: $sql", cursor.moveToFirst())
    cursor.getString(0)
}

private fun SupportSQLiteDatabase.assertForeignKeysValid() {
    query("PRAGMA foreign_key_check").use { cursor ->
        val hasViolation = cursor.moveToFirst()
        val detail =
            if (hasViolation) {
                "${cursor.getString(0)} row ${cursor.getLong(1)} references ${cursor.getString(2)}"
            } else {
                "none"
            }
        assertFalse(
            "Foreign-key violation: $detail",
            hasViolation,
        )
    }
}

private fun sentinelFood(id: String): FoodEntity = FoodEntity(
    accountId = LOCAL_DEFAULT_ACCOUNT_ID,
    id = id,
    name = "Framework SQLite sentinel",
    brand = null,
    defaultServingGrams = 100.0,
    caloriesPer100g = 250.0,
    proteinPer100g = 12.0,
    carbsPer100g = 30.0,
    fatPer100g = 8.0,
    createdAtEpochMillis = 1_000L,
    updatedAtEpochMillis = 2_000L,
)

private suspend fun <T : Throwable> assertSuspendThrows(
    expected: Class<T>,
    block: suspend () -> Unit,
): T = try {
    block()
    throw AssertionError("Expected ${expected.name} to be thrown")
} catch (error: Throwable) {
    if (!expected.isInstance(error)) throw error
    expected.cast(error)
}
