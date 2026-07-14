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
class TrainingRoutineProgramMigration22To23Test {
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
    fun migration22To23_addsRoutineProgramColumnsAndPreservesExistingRows() {
        createDatabaseFromExportedSchema(version = 22)
        seedLegacyRoutine()

        val roomDatabase =
            Room.databaseBuilder(context, MusFitDatabase::class.java, TEST_DATABASE_NAME)
                .addMigrations(
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
                    DatabaseModule.MIGRATION_36_37, DatabaseModule.MIGRATION_37_38, DatabaseModule.MIGRATION_38_39, DatabaseModule.MIGRATION_39_40, DatabaseModule.MIGRATION_40_41,
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
            assertTrue(tableHasColumn(migratedDatabase, "routines", "programName"))
            assertTrue(tableHasColumn(migratedDatabase, "routines", "tags"))
            migratedDatabase.rawQuery(
                """
                SELECT name, programName, tags
                FROM routines
                WHERE id = 'legacy-routine'
                """.trimIndent(),
                null,
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Legacy Routine", cursor.getString(0))
                assertTrue(cursor.isNull(1))
                assertEquals("", cursor.getString(2))
            }
        } finally {
            migratedDatabase.close()
        }
    }

    private fun seedLegacyRoutine() {
        val database =
            SQLiteDatabase.openDatabase(
                context.getDatabasePath(TEST_DATABASE_NAME).path,
                null,
                SQLiteDatabase.OPEN_READWRITE,
            )
        try {
            database.execSQL(
                """
                INSERT INTO routines (
                    id, name, notes, createdAtEpochMillis, updatedAtEpochMillis, isStarter
                ) VALUES (
                    'legacy-routine', 'Legacy Routine', NULL, 1000, 1000, 0
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
        const val TEST_DATABASE_NAME = "routine-mig-22-23"
    }
}
