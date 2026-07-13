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
class MealDefinitionVisibilityMigration31To32Test {
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
    fun migration31To32_addsIsHiddenColumnDefaultingToVisibleAndPreservesRows() {
        createDatabaseFromExportedSchema(version = 31)
        seedVersion31MealDefinition()

        val roomDatabase =
            Room.databaseBuilder(context, MusFitDatabase::class.java, TEST_DATABASE_NAME)
                .addMigrations(DatabaseModule.MIGRATION_31_32, DatabaseModule.MIGRATION_32_33, DatabaseModule.MIGRATION_33_34, DatabaseModule.MIGRATION_34_35, DatabaseModule.MIGRATION_35_36, DatabaseModule.MIGRATION_36_37)
                .build()
        try {
            roomDatabase.openHelper.writableDatabase.close()
        } finally {
            roomDatabase.close()
        }

        val migrated =
            SQLiteDatabase.openDatabase(
                context.getDatabasePath(TEST_DATABASE_NAME).path,
                null,
                SQLiteDatabase.OPEN_READONLY,
            )
        try {
            assertTrue(tableHasColumn(migrated, "meal_definitions", "isHidden"))
            // Existing definitions are preserved and default to visible (isHidden = 0).
            assertEquals("Pre-workout", stringValue(migrated, "SELECT name FROM meal_definitions WHERE id = 'pre-workout'"))
            assertEquals("0", stringValue(migrated, "SELECT isHidden FROM meal_definitions WHERE id = 'pre-workout'"))
        } finally {
            migrated.close()
        }
    }

    private fun seedVersion31MealDefinition() {
        val databaseFile = context.getDatabasePath(TEST_DATABASE_NAME)
        val database = SQLiteDatabase.openDatabase(databaseFile.path, null, SQLiteDatabase.OPEN_READWRITE)
        try {
            database.execSQL(
                """
                INSERT INTO meal_definitions (
                    id, name, timeMinutes, sortOrder, createdAtEpochMillis, updatedAtEpochMillis
                ) VALUES (
                    'pre-workout', 'Pre-workout', 990, 5, 1000, 2000
                )
                """.trimIndent(),
            )
        } finally {
            database.close()
        }
    }

    private fun tableHasColumn(database: SQLiteDatabase, tableName: String, columnName: String): Boolean = database.rawQuery("PRAGMA table_info($tableName)", null).use { cursor ->
        val nameIndex = cursor.getColumnIndex("name")
        generateSequence { if (cursor.moveToNext()) cursor.getString(nameIndex) else null }
            .any { it == columnName }
    }

    private fun stringValue(database: SQLiteDatabase, query: String): String? = database.rawQuery(query, null).use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
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
                        resolveSchemaSql(entityIndex.getString("createSql"), entity.getString("tableName")),
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
        val candidates = listOf(File(relativePath), File("app/$relativePath"), File("../app/$relativePath"))
        return candidates.firstOrNull(File::exists)
            ?: throw IllegalStateException("Could not find exported Room schema for version $version.")
    }

    private companion object {
        const val TEST_DATABASE_NAME = "mealdef-31-32"
    }
}
