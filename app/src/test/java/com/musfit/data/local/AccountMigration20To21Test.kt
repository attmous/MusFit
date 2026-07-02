package com.musfit.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.musfit.core.di.DatabaseModule
import java.io.File
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccountMigration20To21Test {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(TEST_DATABASE_NAME)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(TEST_DATABASE_NAME)
    }

    @Test
    fun migration20To21_remapsLegacySingletonRowsToLocalDefault() {
        createDatabaseFromExportedSchema(version = 20)
        seedLegacySingletonRows()

        val roomDatabase =
            Room.databaseBuilder(context, MusFitDatabase::class.java, TEST_DATABASE_NAME)
                .addMigrations(
                    DatabaseModule.MIGRATION_20_21,
                    DatabaseModule.MIGRATION_21_22,
                    DatabaseModule.MIGRATION_22_23,
                    DatabaseModule.MIGRATION_23_24,
                    DatabaseModule.MIGRATION_24_25,
                    DatabaseModule.MIGRATION_25_26,
                    DatabaseModule.MIGRATION_26_27,
                )
                .build()
        try {
            roomDatabase.openHelper.writableDatabase.close()
        } finally {
            roomDatabase.close()
        }

        val migratedDatabase =
            SQLiteDatabase.openDatabase(
                context.getDatabasePath(TEST_DATABASE_NAME).path,
                null,
                SQLiteDatabase.OPEN_READONLY,
            )
        try {
            migratedDatabase.rawQuery(
                """
                SELECT sex, birthDateEpochDay, heightCm, activityLevel, goalType, goalPaceKgPerWeek, goalWeightKg,
                    updatedAtEpochMillis
                FROM user_profile
                WHERE id = 'local-default'
                """.trimIndent(),
                null,
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Female", cursor.getString(0))
                assertEquals(8_765L, cursor.getLong(1))
                assertEquals(168.5, cursor.getDouble(2), 0.0)
                assertEquals("Light", cursor.getString(3))
                assertEquals("Gain", cursor.getString(4))
                assertEquals(0.25, cursor.getDouble(5), 0.0)
                // MIGRATION_26_27 carries the newer user_goals.targetWeightKg (71.4 @ 444555)
                // over the stale profile goal weight (64.2 @ 123456).
                assertEquals(71.4, cursor.getDouble(6), 0.0)
                assertEquals(123_456L, cursor.getLong(7))
            }
            migratedDatabase.rawQuery(
                """
                SELECT unitSystem, themeMode, updatedAtEpochMillis
                FROM app_settings
                WHERE id = 'local-default'
                """.trimIndent(),
                null,
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("imperial", cursor.getString(0))
                assertEquals("dark", cursor.getString(1))
                assertEquals(222_333L, cursor.getLong(2))
            }
            migratedDatabase.rawQuery(
                """
                SELECT stepGoal, weeklySessionTarget, targetWeightKg, updatedAtEpochMillis
                FROM user_goals
                WHERE id = 'local-default'
                """.trimIndent(),
                null,
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(9_500L, cursor.getLong(0))
                assertEquals(5, cursor.getInt(1))
                assertEquals(71.4, cursor.getDouble(2), 0.0)
                assertEquals(444_555L, cursor.getLong(3))
            }
            assertFalse(rowExists(migratedDatabase, "user_profile", "user"))
            assertFalse(rowExists(migratedDatabase, "app_settings", "app"))
            assertFalse(rowExists(migratedDatabase, "user_goals", "default"))
        } finally {
            migratedDatabase.close()
        }
    }

    private fun seedLegacySingletonRows() {
        val database =
            SQLiteDatabase.openDatabase(
                context.getDatabasePath(TEST_DATABASE_NAME).path,
                null,
                SQLiteDatabase.OPEN_READWRITE,
            )
        try {
            database.execSQL(
                """
                INSERT INTO user_profile (
                    id, sex, birthDateEpochDay, heightCm, activityLevel, goalType,
                    goalPaceKgPerWeek, goalWeightKg, updatedAtEpochMillis
                ) VALUES (
                    'user', 'Female', 8765, 168.5, 'Light', 'Gain', 0.25, 64.2, 123456
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO app_settings (
                    id, unitSystem, themeMode, updatedAtEpochMillis
                ) VALUES (
                    'app', 'imperial', 'dark', 222333
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO user_goals (
                    id, stepGoal, weeklySessionTarget, targetWeightKg, updatedAtEpochMillis
                ) VALUES (
                    'default', 9500, 5, 71.4, 444555
                )
                """.trimIndent(),
            )
        } finally {
            database.close()
        }
    }

    private fun rowExists(database: SQLiteDatabase, tableName: String, id: String): Boolean =
        database.rawQuery(
            "SELECT 1 FROM $tableName WHERE id = ?",
            arrayOf(id),
        ).use { cursor ->
            cursor.moveToFirst()
        }

    private fun createDatabaseFromExportedSchema(version: Int) {
        val schemaFile = resolveSchemaFile(version)
        val schemaJson = JSONObject(schemaFile.readText())
        val databaseJson = schemaJson.getJSONObject("database")
        val databaseFile = context.getDatabasePath(TEST_DATABASE_NAME)

        databaseFile.parentFile?.mkdirs()
        val database = SQLiteDatabase.openOrCreateDatabase(databaseFile, null)
        try {
            val entities = databaseJson.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                database.execSQL(resolveSchemaSql(entity.getString("createSql"), entity.getString("tableName")))
                val indices = entity.optJSONArray("indices") ?: continue
                for (indexPosition in 0 until indices.length()) {
                    val entityIndex = indices.getJSONObject(indexPosition)
                    database.execSQL(
                        resolveSchemaSql(
                            entityIndex.getString("createSql"),
                            entity.getString("tableName"),
                        ),
                    )
                }
            }
            val setupQueries = databaseJson.getJSONArray("setupQueries")
            for (index in 0 until setupQueries.length()) {
                database.execSQL(setupQueries.getString(index))
            }
            database.version = version
        } finally {
            database.close()
        }
    }

    private fun resolveSchemaSql(sql: String, tableName: String): String = sql.replace("\${TABLE_NAME}", tableName)

    private fun resolveSchemaFile(version: Int): File {
        val relativePath = "schemas/com.musfit.data.local.MusFitDatabase/$version.json"
        val candidates =
            listOf(
                File(relativePath),
                File("app/$relativePath"),
                File("../app/$relativePath"),
            )
        return candidates.firstOrNull(File::exists)
            ?: throw IllegalStateException(
                "Could not find exported Room schema for version $version. Checked: ${candidates.joinToString()}",
            )
    }

    private companion object {
        const val TEST_DATABASE_NAME = "account-migration-20-21-test"
    }
}
