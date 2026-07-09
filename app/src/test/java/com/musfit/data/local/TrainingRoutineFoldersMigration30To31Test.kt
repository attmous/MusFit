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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TrainingRoutineFoldersMigration30To31Test {
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
    fun migration30To31_addsRoutineFoldersRestAndSetPlans() {
        createDatabaseFromExportedSchema(version = 30)
        seedVersion30TrainingRows()

        val roomDatabase =
            Room.databaseBuilder(context, MusFitDatabase::class.java, TEST_DATABASE_NAME)
                .addMigrations(DatabaseModule.MIGRATION_30_31, DatabaseModule.MIGRATION_31_32, DatabaseModule.MIGRATION_32_33, DatabaseModule.MIGRATION_33_34, DatabaseModule.MIGRATION_34_35)
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
            assertTrue(tableExists(migrated, "routine_folders"))
            assertTrue(tableExists(migrated, "routine_exercise_sets"))
            assertTrue(tableHasColumn(migrated, "routines", "folderId"))
            assertTrue(tableHasColumn(migrated, "routine_exercises", "restSeconds"))
            assertTrue(tableHasColumn(migrated, "workout_sets", "restSeconds"))

            assertEquals("Push Pull Legs", stringValue(migrated, "SELECT name FROM routine_folders LIMIT 1"))
            assertEquals(
                "folder-push-pull-legs",
                stringValue(migrated, "SELECT folderId FROM routines WHERE id = 'routine-push'"),
            )
            assertEquals(
                listOf("working", "working", "working"),
                stringValues(
                    migrated,
                    "SELECT setType FROM routine_exercise_sets WHERE routineExerciseId = 'routine-push-bench' ORDER BY sortOrder",
                ),
            )
            assertEquals(
                listOf("8", "8", "8"),
                stringValues(
                    migrated,
                    "SELECT targetReps FROM routine_exercise_sets WHERE routineExerciseId = 'routine-push-bench' ORDER BY sortOrder",
                ),
            )
        } finally {
            migrated.close()
        }
    }

    private fun seedVersion30TrainingRows() {
        val databaseFile = context.getDatabasePath(TEST_DATABASE_NAME)
        val database = SQLiteDatabase.openDatabase(databaseFile.path, null, SQLiteDatabase.OPEN_READWRITE)
        try {
            database.execSQL(
                """
                INSERT INTO exercises (
                    id, name, category, equipment, targetMuscles, isCustom, primaryMuscles,
                    secondaryMuscles, instructions, localNotes, imageUrl, gifUrl
                ) VALUES (
                    'exercise-bench', 'Bench Press', 'strength', 'barbell', 'chest', 0, 'chest',
                    '', NULL, NULL, NULL, NULL
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO routines (
                    id, name, notes, createdAtEpochMillis, updatedAtEpochMillis, isStarter,
                    programName, tags
                ) VALUES (
                    'routine-push', 'Push #1', NULL, 1000, 2000, 0, 'Push Pull Legs', 'push'
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO routine_exercises (
                    id, routineId, exerciseId, sortOrder, targetSets, targetReps
                ) VALUES (
                    'routine-push-bench', 'routine-push', 'exercise-bench', 0, 3, '8'
                )
                """.trimIndent(),
            )
        } finally {
            database.close()
        }
    }

    private fun tableExists(database: SQLiteDatabase, tableName: String): Boolean =
        database.rawQuery(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf(tableName),
        ).use { cursor -> cursor.moveToFirst() }

    private fun tableHasColumn(database: SQLiteDatabase, tableName: String, columnName: String): Boolean =
        database.rawQuery("PRAGMA table_info($tableName)", null).use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            generateSequence { if (cursor.moveToNext()) cursor.getString(nameIndex) else null }
                .any { it == columnName }
        }

    private fun stringValue(database: SQLiteDatabase, query: String): String? =
        database.rawQuery(query, null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }

    private fun stringValues(database: SQLiteDatabase, query: String): List<String> =
        database.rawQuery(query, null).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getString(0))
                }
            }
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
        const val TEST_DATABASE_NAME = "training-routine-folders-30-31"
    }
}
