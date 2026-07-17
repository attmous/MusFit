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
class RemainingOwnershipMigration38To39Test {
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
    fun migration38To39_assignsRemainingUserRowsToLocalDefaultAndAddsOwnerIndexes() {
        createDatabaseFromExportedSchema(38) { database ->
            database.execSQL("INSERT INTO body_metrics VALUES ('metric', 'weight', 80.5, 'kg', 1000, 'manual', NULL)")
            database.execSQL("INSERT INTO daily_health_summaries VALUES (20000, 8000, 300, 2200, 5000, 450, 40, 1, 80.5, 15, 58, 60, 2000)")
            database.execSQL("INSERT INTO health_connect_sync_state VALUES ('health_connect', 1, 'read', 1000, 1500, NULL, 'com.example.steps')")
            database.execSQL(
                """
                INSERT INTO coach_messages VALUES (
                    7, 20000, 'protein_gap', 'Nutrition', 'Protein', 'Eat protein',
                    'open_food', NULL, 1000, 0, 0, 'rules'
                )
                """.trimIndent(),
            )
            database.execSQL("INSERT INTO dashboard_pins VALUES ('steps', 0)")
        }

        val room = Room.databaseBuilder(context, MusFitDatabase::class.java, DATABASE_NAME)
            .addMigrations(DatabaseModule.MIGRATION_38_39, DatabaseModule.MIGRATION_39_40, DatabaseModule.MIGRATION_40_41, DatabaseModule.MIGRATION_41_42)
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
            assertEquals(7, intValue(database, "SELECT id FROM coach_messages WHERE accountId = 'local-default'"))
            assertEquals("com.example.steps", stringValue(database, "SELECT preferredStepsPackage FROM health_connect_sync_state WHERE accountId = 'local-default'"))
            assertEquals(0, intValue(database, "SELECT COUNT(*) FROM pragma_foreign_key_check"))
            assertTrue(
                queryPlan(
                    database,
                    "SELECT * FROM body_metrics WHERE accountId = 'local-default' AND type = 'weight' AND measuredAtEpochMillis >= 0 ORDER BY measuredAtEpochMillis DESC",
                ).contains("index_body_metrics_accountId_type_measuredAtEpochMillis", ignoreCase = true),
            )
            assertTrue(
                queryPlan(
                    database,
                    "SELECT * FROM dashboard_pins WHERE accountId = 'local-default' ORDER BY position",
                ).contains("index_dashboard_pins_accountId_position", ignoreCase = true),
            )
        }
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

    private fun stringValue(database: SQLiteDatabase, sql: String): String = database.rawQuery(sql, null).use { cursor ->
        cursor.moveToFirst()
        cursor.getString(0)
    }

    private fun queryPlan(database: SQLiteDatabase, sql: String): String = database.rawQuery("EXPLAIN QUERY PLAN $sql", null).use { cursor ->
        buildList {
            while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("detail")))
        }.joinToString("\n")
    }

    private fun resolveSchemaFile(version: Int): File {
        val relative = "schemas/com.musfit.data.local.MusFitDatabase/$version.json"
        return listOf(File(relative), File("app/$relative"), File("../app/$relative"), File("../../app/$relative")).firstOrNull(File::exists)
            ?: error("Could not find exported Room schema $version")
    }

    private companion object {
        const val DATABASE_NAME = "remaining-own-38-39"
        val OWNED_TABLES = listOf(
            "body_metrics",
            "daily_health_summaries",
            "health_connect_sync_state",
            "coach_messages",
            "dashboard_pins",
        )
    }
}
