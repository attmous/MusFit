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
class TrainingOwnershipMigration37To38Test {
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
    fun migration37To38_assignsTrainingRowsToLocalDefaultAndRetainsSharedCatalog() {
        createDatabaseFromExportedSchema(37) { database -> seedCompleteTrainingGraph(database) }

        val room = Room.databaseBuilder(context, MusFitDatabase::class.java, DATABASE_NAME)
            .addMigrations(DatabaseModule.MIGRATION_37_38, DatabaseModule.MIGRATION_38_39, DatabaseModule.MIGRATION_39_40, DatabaseModule.MIGRATION_40_41, DatabaseModule.MIGRATION_41_42)
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
            assertEquals(1, intValue(database, "SELECT COUNT(*) FROM exercises WHERE id = 'shared-exercise' AND accountId IS NULL"))
            assertEquals(1, intValue(database, "SELECT COUNT(*) FROM exercises WHERE id = 'custom-exercise' AND accountId = 'local-default'"))
            assertEquals(
                1,
                intValue(
                    database,
                    """
                    SELECT COUNT(*) FROM workout_sets sets
                    JOIN workout_sessions sessions
                        ON sessions.accountId = sets.accountId AND sessions.id = sets.sessionId
                    JOIN routines routines
                        ON routines.accountId = sessions.accountId AND routines.id = sessions.routineId
                    JOIN routine_exercises routine_exercises
                        ON routine_exercises.accountId = routines.accountId AND routine_exercises.routineId = routines.id
                    JOIN exercise_notes notes
                        ON notes.accountId = sets.accountId AND notes.exerciseId = sets.exerciseId
                    WHERE sets.id = 'workout-set' AND notes.notes = 'Legacy setup note'
                    """.trimIndent(),
                ),
            )
            assertEquals(0, intValue(database, "SELECT COUNT(*) FROM pragma_foreign_key_check"))
            assertTrue(
                queryPlan(
                    database,
                    "SELECT * FROM workout_sessions WHERE accountId = 'local-default' AND status = 'active' ORDER BY startedAtEpochMillis DESC LIMIT 1",
                ).contains("index_workout_sessions_accountId_status_startedAtEpochMillis", ignoreCase = true),
            )
            assertTrue(
                queryPlan(
                    database,
                    "SELECT * FROM routine_exercises WHERE accountId = 'local-default' AND routineId = 'routine' ORDER BY sortOrder",
                ).contains("index_routine_exercises_accountId_routineId_sortOrder", ignoreCase = true),
            )
        }
    }

    private fun seedCompleteTrainingGraph(database: SQLiteDatabase) {
        database.execSQL(
            """
            INSERT INTO exercises VALUES (
                'shared-exercise', 'Bench Press', 'strength', 'barbell', 'chest', 0,
                'chest', 'triceps', 'Press safely', 'Legacy setup note', NULL, NULL
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            INSERT INTO exercises VALUES (
                'custom-exercise', 'Custom Row', 'strength', 'cable', 'back', 1,
                'back', 'biceps', NULL, NULL, NULL, NULL
            )
            """.trimIndent(),
        )
        database.execSQL("INSERT INTO routine_folders VALUES ('folder', 'Strength', 0, 1, 1)")
        database.execSQL("INSERT INTO routines VALUES ('routine', 'Push', NULL, 1, 1, 0, NULL, '', 'folder')")
        database.execSQL("INSERT INTO routine_exercises VALUES ('routine-exercise', 'routine', 'shared-exercise', 0, 1, '5', 120)")
        database.execSQL("INSERT INTO routine_exercise_sets VALUES ('routine-set', 'routine-exercise', 0, 'working', '5', 100)")
        database.execSQL("INSERT INTO workout_sessions VALUES ('session', 'routine', 'Push', 'active', 1000, NULL, NULL, NULL, NULL)")
        database.execSQL("INSERT INTO workout_sets VALUES ('workout-set', 'session', 'shared-exercise', 0, 'working', 5, 100, NULL, NULL, 8, NULL, 1, NULL, 120)")
        database.execSQL("INSERT INTO training_settings VALUES ('default', 120, 20, '20,10,5')")
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
        return listOf(File(relative), File("app/$relative"), File("../app/$relative"), File("../../app/$relative")).firstOrNull(File::exists)
            ?: error("Could not find exported Room schema $version")
    }

    private companion object {
        const val DATABASE_NAME = "training-own-37-38"
        val OWNED_TABLES = listOf(
            "exercise_notes",
            "routine_folders",
            "routines",
            "routine_exercises",
            "routine_exercise_sets",
            "workout_sessions",
            "workout_sets",
            "training_settings",
        )
    }
}
