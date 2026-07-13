package com.musfit.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.musfit.core.di.DatabaseModule
import com.musfit.data.local.entity.ACTIVE_ACCOUNT_SESSION_KEY
import com.musfit.data.local.entity.LOCAL_DEFAULT_ACCOUNT_ID
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
class AccountMigrationTest {
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
    fun migration19ToLatest_createsAccountTablesAndSeedsLocalDefaultSession() {
        createDatabaseFromExportedSchema(version = 19)

        val roomDatabase =
            Room.databaseBuilder(context, MusFitDatabase::class.java, TEST_DATABASE_NAME)
                .addMigrations(
                    DatabaseModule.MIGRATION_19_20,
                    DatabaseModule.MIGRATION_20_21,
                    DatabaseModule.MIGRATION_21_22,
                    DatabaseModule.MIGRATION_22_23,
                    DatabaseModule.MIGRATION_23_24,
                    DatabaseModule.MIGRATION_24_25,
                    DatabaseModule.MIGRATION_25_26,
                    DatabaseModule.MIGRATION_26_27,
                    DatabaseModule.MIGRATION_27_28,
                    DatabaseModule.MIGRATION_28_29,
                    DatabaseModule.MIGRATION_29_30,
                    DatabaseModule.MIGRATION_30_31,
                    DatabaseModule.MIGRATION_31_32,
                    DatabaseModule.MIGRATION_32_33,
                    DatabaseModule.MIGRATION_33_34,
                    DatabaseModule.MIGRATION_34_35,
                    DatabaseModule.MIGRATION_35_36,
                    DatabaseModule.MIGRATION_36_37, DatabaseModule.MIGRATION_37_38,
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
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name IN ('accounts', 'account_session')",
                null,
            ).use { cursor ->
                assertEquals(2, cursor.count)
            }
            migratedDatabase.rawQuery(
                """
                SELECT id, displayName
                FROM accounts
                WHERE id = '$LOCAL_DEFAULT_ACCOUNT_ID'
                """.trimIndent(),
                null,
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(LOCAL_DEFAULT_ACCOUNT_ID, cursor.getString(0))
                assertEquals("You", cursor.getString(1))
            }
            migratedDatabase.rawQuery(
                """
                SELECT `key`, activeAccountId
                FROM account_session
                WHERE `key` = '$ACTIVE_ACCOUNT_SESSION_KEY'
                """.trimIndent(),
                null,
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(ACTIVE_ACCOUNT_SESSION_KEY, cursor.getString(0))
                assertEquals(LOCAL_DEFAULT_ACCOUNT_ID, cursor.getString(1))
            }
        } finally {
            migratedDatabase.close()
        }
    }

    @Test
    fun migration20To21_remapsLegacySingletonRowsToLocalDefaultAccount() {
        createDatabaseFromExportedSchema(version = 20)
        insertLegacySingletonRows(version = 20)

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
                    DatabaseModule.MIGRATION_27_28,
                    DatabaseModule.MIGRATION_28_29,
                    DatabaseModule.MIGRATION_29_30,
                    DatabaseModule.MIGRATION_30_31,
                    DatabaseModule.MIGRATION_31_32,
                    DatabaseModule.MIGRATION_32_33,
                    DatabaseModule.MIGRATION_33_34,
                    DatabaseModule.MIGRATION_34_35,
                    DatabaseModule.MIGRATION_35_36,
                    DatabaseModule.MIGRATION_36_37, DatabaseModule.MIGRATION_37_38,
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
            migratedDatabase.rawQuery("SELECT id, sex FROM user_profile", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(LOCAL_DEFAULT_ACCOUNT_ID, cursor.getString(0))
                assertEquals("Male", cursor.getString(1))
                assertEquals(1, cursor.count)
            }
            migratedDatabase.rawQuery("SELECT id, themeMode FROM app_settings", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(LOCAL_DEFAULT_ACCOUNT_ID, cursor.getString(0))
                assertEquals("dark", cursor.getString(1))
                assertEquals(1, cursor.count)
            }
            migratedDatabase.rawQuery("SELECT id, stepGoal FROM user_goals", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(LOCAL_DEFAULT_ACCOUNT_ID, cursor.getString(0))
                assertEquals(12_000L, cursor.getLong(1))
                assertEquals(1, cursor.count)
            }
        } finally {
            migratedDatabase.close()
        }
    }

    private companion object {
        const val TEST_DATABASE_NAME = "account-migration-test"
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

    private fun insertLegacySingletonRows(version: Int) {
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
                    'user', 'Male', 9000, 182.0, 'Active', 'Lose',
                    0.5, 78.0, 1000
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO app_settings (
                    id, unitSystem, themeMode, updatedAtEpochMillis
                ) VALUES (
                    'app', 'metric', 'dark', 1000
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO user_goals (
                    id, stepGoal, weeklySessionTarget, targetWeightKg, updatedAtEpochMillis
                ) VALUES (
                    'default', 12000, 5, 78.0, 1000
                )
                """.trimIndent(),
            )
            database.version = version
        } finally {
            database.close()
        }
    }

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
}
