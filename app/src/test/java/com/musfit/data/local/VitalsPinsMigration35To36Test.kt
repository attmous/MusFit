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
class VitalsPinsMigration35To36Test {
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
    fun migration35To36_appendsWaterToTheUntouchedDefaultPins() {
        createDatabaseFromExportedSchema(version = 35) { database ->
            database.execSQL(
                "INSERT INTO dashboard_pins (metricId, position) " +
                    "VALUES ('calories', 0), ('steps', 1), ('protein', 2)",
            )
        }

        runMigration()

        readPins().let { pins ->
            assertEquals(
                listOf("calories" to 0, "steps" to 1, "protein" to 2, "water" to 3),
                pins,
            )
        }
    }

    @Test
    fun migration35To36_leavesCustomizedPinsAlone() {
        createDatabaseFromExportedSchema(version = 35) { database ->
            // The user reordered and trimmed the set — never touch it.
            database.execSQL(
                "INSERT INTO dashboard_pins (metricId, position) VALUES ('weight', 0), ('calories', 1)",
            )
        }

        runMigration()

        assertEquals(listOf("weight" to 0, "calories" to 1), readPins())
    }

    private fun runMigration() {
        val roomDatabase =
            Room.databaseBuilder(context, MusFitDatabase::class.java, TEST_DATABASE_NAME)
                .addMigrations(DatabaseModule.MIGRATION_35_36, DatabaseModule.MIGRATION_36_37, DatabaseModule.MIGRATION_37_38, DatabaseModule.MIGRATION_38_39)
                .build()
        try {
            roomDatabase.openHelper.writableDatabase.close()
        } finally {
            roomDatabase.close()
        }
    }

    private fun readPins(): List<Pair<String, Int>> {
        val migrated =
            SQLiteDatabase.openDatabase(
                context.getDatabasePath(TEST_DATABASE_NAME).path,
                null,
                SQLiteDatabase.OPEN_READONLY,
            )
        return migrated.use { database ->
            database.rawQuery(
                "SELECT metricId, position FROM dashboard_pins ORDER BY position",
                null,
            ).use { cursor ->
                generateSequence { if (cursor.moveToNext()) cursor.getString(0) to cursor.getInt(1) else null }
                    .toList()
            }
        }
    }

    private fun createDatabaseFromExportedSchema(version: Int, seed: (SQLiteDatabase) -> Unit) {
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
                        resolveSchemaSql(entityIndex.getString("createSql"), entity.getString("tableName")),
                    )
                }
            }
            val setupQueries = databaseJson.getJSONArray("setupQueries")
            for (index in 0 until setupQueries.length()) {
                database.execSQL(setupQueries.getString(index))
            }
            seed(database)
            database.version = version
        } finally {
            database.close()
        }
    }

    private fun resolveSchemaSql(sql: String, tableName: String): String = sql.replace("\${TABLE_NAME}", tableName)

    private fun resolveSchemaFile(version: Int): File {
        val relativePath = "schemas/com.musfit.data.local.MusFitDatabase/$version.json"
        val candidates = listOf(File(relativePath), File("app/$relativePath"), File("../app/$relativePath"))
        return candidates.firstOrNull(File::exists)
            ?: throw IllegalStateException("Could not find exported Room schema for version $version.")
    }

    private companion object {
        // Deliberately short: long Room DB names overflow Windows MAX_PATH at WAL setup.
        const val TEST_DATABASE_NAME = "pins-35-36"
    }
}
