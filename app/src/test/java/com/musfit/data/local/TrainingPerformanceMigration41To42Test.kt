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
class TrainingPerformanceMigration41To42Test {
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
    fun migration41To42_batchesTrainingHistoryAndUsesMeasuredIndexes() {
        createDatabaseFromExportedSchema(41, MIGRATION_DATABASE_NAME, ::seedLargeWorkout)
        val baselineStorage: Long
        val legacyP90: Long
        openReadOnly(MIGRATION_DATABASE_NAME).use { database ->
            baselineStorage = storageBytes(database)
            legacyP90 = legacyHistoryP90Nanos(database)
        }

        val room = Room.databaseBuilder(context, MusFitDatabase::class.java, MIGRATION_DATABASE_NAME)
            .addMigrations(DatabaseModule.MIGRATION_41_42)
            .addCallback(DatabaseModule.TRAINING_PERFORMANCE_INDEX_CALLBACK)
            .build()
        room.openHelper.writableDatabase
        room.close()

        openReadOnly(MIGRATION_DATABASE_NAME).use { database ->
            val optimizedP90 = batchedHistoryP90Nanos(database)
            val optimizedStorage = storageBytes(database)
            val historyPlan = queryPlan(database, BATCHED_HISTORY_SQL)
            val latestPlan = queryPlan(database, LATEST_COMPLETED_SQL)
            val exercisePlan = queryPlan(
                database,
                "SELECT * FROM exercises WHERE accountId = 'local-default' AND isCustom = 1 " +
                    "ORDER BY name COLLATE NOCASE, id",
            )

            assertTrue(historyPlan, historyPlan.contains(HISTORY_INDEX, ignoreCase = true))
            assertFalse(historyPlan, historyPlan.contains("USE TEMP B-TREE", ignoreCase = true))
            assertFalse(latestPlan, latestPlan.contains("CORRELATED", ignoreCase = true))
            assertFalse(latestPlan, latestPlan.contains("USE TEMP B-TREE", ignoreCase = true))
            assertTrue(exercisePlan, exercisePlan.contains(EXERCISE_INDEX, ignoreCase = true))
            assertTrue("Expected $optimizedP90 ns < $legacyP90 ns", optimizedP90 < legacyP90)
            assertTrue(
                "Index storage grew from $baselineStorage to $optimizedStorage bytes",
                optimizedStorage <= baselineStorage + MAX_STORAGE_GROWTH_BYTES,
            )
            val setIndexes = queryStrings(database, "SELECT name FROM pragma_index_list('workout_sets')")
            assertTrue(setIndexes.toString(), HISTORY_INDEX in setIndexes)
            assertFalse(setIndexes.toString(), "index_workout_sets_accountId_exerciseId" in setIndexes)
            assertTrue(queryStrings(database, "SELECT name FROM pragma_index_list('exercises')").toString().contains(EXERCISE_INDEX))
            println(
                "W3-DATA-02 20-exercise history P90: $legacyP90 ns -> $optimizedP90 ns; " +
                    "storage: $baselineStorage -> $optimizedStorage bytes",
            )
        }
    }

    @Test
    fun productionCallback_addsTrainingSearchIndexToFreshDatabases() {
        val room = Room.databaseBuilder(context, MusFitDatabase::class.java, FRESH_DATABASE_NAME)
            .addCallback(DatabaseModule.TRAINING_PERFORMANCE_INDEX_CALLBACK)
            .build()
        room.openHelper.writableDatabase
        room.close()
        openReadOnly(FRESH_DATABASE_NAME).use { database ->
            assertTrue(queryStrings(database, "SELECT name FROM pragma_index_list('exercises')").toString().contains(EXERCISE_INDEX))
        }
    }

    private fun seedLargeWorkout(database: SQLiteDatabase) {
        database.execSQL("INSERT INTO accounts(id, displayName, authProvider, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('local-default', 'Local', 'local', 1, 1)")
        database.beginTransaction()
        try {
            repeat(20) { exercise ->
                val exerciseId = "exercise-$exercise"
                database.execSQL("INSERT INTO exercises(id, accountId, name, category, equipment, targetMuscles, isCustom, primaryMuscles, secondaryMuscles) VALUES ('$exerciseId', 'local-default', 'Exercise $exercise', 'strength', 'barbell', 'full body', 1, 'full body', '')")
            }
            repeat(25) { session ->
                val sessionId = "history-$session"
                database.execSQL("INSERT INTO workout_sessions(accountId, id, routineId, title, status, startedAtEpochMillis, endedAtEpochMillis, notes) VALUES ('local-default', '$sessionId', NULL, 'History', 'completed', ${session + 1L}, ${session + 2L}, NULL)")
                repeat(20) { exercise ->
                    database.execSQL("INSERT INTO workout_sets(accountId, id, sessionId, exerciseId, sortOrder, setType, reps, weightKg, completed) VALUES ('local-default', 'set-$session-$exercise', '$sessionId', 'exercise-$exercise', $exercise, 'working', 5, 100.0, 1)")
                }
            }
            database.execSQL("INSERT INTO workout_sessions(accountId, id, routineId, title, status, startedAtEpochMillis, endedAtEpochMillis, notes) VALUES ('local-default', 'active', NULL, 'Active', 'active', 1000, NULL, NULL)")
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }

    private fun legacyHistoryP90Nanos(database: SQLiteDatabase): Long = p90Nanos {
        repeat(20) { exercise ->
            queryStrings(database, LEGACY_LATEST_SQL.format(exercise))
            queryStrings(database, LEGACY_SETS_SQL.format(exercise))
            queryStrings(database, LEGACY_SETS_SQL.format(exercise))
        }
    }

    private fun batchedHistoryP90Nanos(database: SQLiteDatabase): Long = p90Nanos { queryStrings(database, BATCHED_HISTORY_SQL) }

    private fun p90Nanos(action: () -> Unit): Long {
        repeat(10) { action() }
        return List(101) { measureNanoTime(action) }.sorted()[90]
    }

    private fun storageBytes(database: SQLiteDatabase): Long = queryStrings(database, "PRAGMA page_count").single().toLong() * queryStrings(database, "PRAGMA page_size").single().toLong()

    private fun openReadOnly(name: String): SQLiteDatabase = SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READONLY)

    private fun queryPlan(database: SQLiteDatabase, sql: String): String = queryStrings(database, "EXPLAIN QUERY PLAN $sql").joinToString("\n")

    private fun queryStrings(database: SQLiteDatabase, sql: String): List<String> = database.rawQuery(sql, null).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(cursor.columnCount - 1)) } }

    private fun createDatabaseFromExportedSchema(version: Int, name: String, seed: (SQLiteDatabase) -> Unit) {
        val schema = JSONObject(resolveSchemaFile(version).readText()).getJSONObject("database")
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { database ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                database.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indexes = entity.optJSONArray("indices") ?: continue
                for (position in 0 until indexes.length()) database.execSQL(indexes.getJSONObject(position).getString("createSql").replace("\${TABLE_NAME}", table))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (index in 0 until setup.length()) database.execSQL(setup.getString(index))
            seed(database)
            database.version = version
        }
    }

    private fun resolveSchemaFile(version: Int): File {
        val relative = "schemas/com.musfit.data.local.MusFitDatabase/$version.json"
        return listOf(File(relative), File("app/$relative"), File("../app/$relative")).firstOrNull(File::exists) ?: error("Could not find exported Room schema $version")
    }

    private companion object {
        const val MIGRATION_DATABASE_NAME = "training-performance-41-42"
        const val FRESH_DATABASE_NAME = "training-performance-fresh"
        const val HISTORY_INDEX = "index_workout_sets_accountId_exerciseId_completed_sessionId_sortOrder"
        const val EXERCISE_INDEX = "index_exercises_accountId_isCustom_name_id_nocase"
        const val MAX_STORAGE_GROWTH_BYTES = 128L * 1024L
        const val LEGACY_LATEST_SQL = "SELECT workout_sets.id FROM workout_sets INNER JOIN workout_sessions ON workout_sessions.accountId = workout_sets.accountId AND workout_sessions.id = workout_sets.sessionId WHERE workout_sets.accountId = 'local-default' AND workout_sets.exerciseId = 'exercise-%d' AND workout_sets.completed = 1 AND workout_sets.reps IS NOT NULL AND workout_sets.weightKg IS NOT NULL AND workout_sessions.status = 'completed' AND workout_sessions.startedAtEpochMillis < 1000 ORDER BY workout_sessions.startedAtEpochMillis DESC, workout_sets.sortOrder DESC LIMIT 1"
        const val LEGACY_SETS_SQL = "SELECT workout_sets.id FROM workout_sets INNER JOIN workout_sessions ON workout_sessions.accountId = workout_sets.accountId AND workout_sessions.id = workout_sets.sessionId WHERE workout_sets.accountId = 'local-default' AND workout_sets.exerciseId = 'exercise-%d' AND workout_sets.completed = 1 AND workout_sets.reps IS NOT NULL AND workout_sets.weightKg IS NOT NULL AND workout_sessions.status = 'completed' AND workout_sessions.startedAtEpochMillis < 1000"
        const val BATCHED_HISTORY_SQL = "SELECT workout_sets.sessionId, workout_sets.exerciseId, workout_sets.sortOrder, workout_sets.reps, workout_sets.weightKg, workout_sessions.startedAtEpochMillis FROM workout_sets INNER JOIN workout_sessions ON workout_sessions.accountId = workout_sets.accountId AND workout_sessions.id = workout_sets.sessionId WHERE workout_sets.accountId = 'local-default' AND workout_sets.exerciseId IN ('exercise-0','exercise-1','exercise-2','exercise-3','exercise-4','exercise-5','exercise-6','exercise-7','exercise-8','exercise-9','exercise-10','exercise-11','exercise-12','exercise-13','exercise-14','exercise-15','exercise-16','exercise-17','exercise-18','exercise-19') AND workout_sets.completed = 1 AND workout_sets.reps IS NOT NULL AND workout_sets.weightKg IS NOT NULL AND workout_sessions.status = 'completed' AND workout_sessions.startedAtEpochMillis < 1000"
        const val LATEST_COMPLETED_SQL = "SELECT workout_sessions.* FROM workout_sessions INDEXED BY index_workout_sessions_accountId_status_startedAtEpochMillis INNER JOIN workout_sets ON workout_sets.accountId = workout_sessions.accountId AND workout_sets.sessionId = workout_sessions.id AND workout_sets.completed = 1 WHERE workout_sessions.accountId = 'local-default' AND workout_sessions.status = 'completed' ORDER BY workout_sessions.startedAtEpochMillis DESC LIMIT 1"
    }
}
