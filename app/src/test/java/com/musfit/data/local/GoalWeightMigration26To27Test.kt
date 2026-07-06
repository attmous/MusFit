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
class GoalWeightMigration26To27Test {
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
    fun migration26To27_insertsProfileRowWhenMissing() {
        createDatabaseFromExportedSchema(version = 26)
        insertGoals("local-default", targetWeightKg = 75.0, updatedAt = 100L)

        runMigration()

        queryProfile("local-default") { goalWeight, activityLevel, goalType, pace ->
            assertEquals(75.0, goalWeight!!, 0.001)
            assertEquals("Moderate", activityLevel) // DEFAULT_USER_PROFILE values
            assertEquals("Maintain", goalType)
            assertEquals(0.5, pace, 0.001)
        }
    }

    @Test
    fun migration26To27_copiesIntoNullProfileGoal() {
        createDatabaseFromExportedSchema(version = 26)
        insertGoals("local-default", targetWeightKg = 75.0, updatedAt = 100L)
        insertProfile("local-default", goalWeightKg = null, updatedAt = 200L)

        runMigration()

        queryProfile("local-default") { goalWeight, _, _, _ -> assertEquals(75.0, goalWeight!!, 0.001) }
    }

    @Test
    fun migration26To27_doesNotOverwriteNewerProfileValue() {
        createDatabaseFromExportedSchema(version = 26)
        insertGoals("local-default", targetWeightKg = 70.0, updatedAt = 100L)
        insertProfile("local-default", goalWeightKg = 80.0, updatedAt = 200L) // profile is newer

        runMigration()

        queryProfile("local-default") { goalWeight, _, _, _ -> assertEquals(80.0, goalWeight!!, 0.001) }
    }

    @Test
    fun migration26To27_overwritesStaleProfileValueWithNewerGoals() {
        createDatabaseFromExportedSchema(version = 26)
        insertGoals("local-default", targetWeightKg = 70.0, updatedAt = 300L) // Today-set, newer
        insertProfile("local-default", goalWeightKg = 80.0, updatedAt = 200L)

        runMigration()

        queryProfile("local-default") { goalWeight, _, _, _ -> assertEquals(70.0, goalWeight!!, 0.001) }
    }

    @Test
    fun migration26To27_zeroSentinelProfileValueIsOverwrittenEvenWhenProfileNewer() {
        createDatabaseFromExportedSchema(version = 26)
        insertGoals("local-default", targetWeightKg = 75.0, updatedAt = 100L)
        insertProfile("local-default", goalWeightKg = 0.0, updatedAt = 200L) // 0 = unset sentinel

        runMigration()

        queryProfile("local-default") { goalWeight, _, _, _ -> assertEquals(75.0, goalWeight!!, 0.001) }
    }

    @Test
    fun migration26To27_zeroGoalsTargetDoesNotCreateProfileRow() {
        createDatabaseFromExportedSchema(version = 26)
        insertGoals("local-default", targetWeightKg = 0.0, updatedAt = 100L)

        runMigration()

        // The INSERT guards on g.targetWeightKg > 0: a 0 (unset sentinel) goals row
        // must not materialize a profile row at all.
        assertNoProfileRow("local-default")
    }

    @Test
    fun migration26To27_zeroGoalsTargetNeverOverwritesProfileEvenWhenNewer() {
        createDatabaseFromExportedSchema(version = 26)
        insertGoals("local-default", targetWeightKg = 0.0, updatedAt = 300L) // newer, but 0 = unset
        insertProfile("local-default", goalWeightKg = 80.0, updatedAt = 200L)

        runMigration()

        // The UPDATE guards on g.targetWeightKg > 0 before any recency comparison:
        // the 0 sentinel never wins, even with the newer timestamp.
        queryProfile("local-default") { goalWeight, _, _, _ -> assertEquals(80.0, goalWeight!!, 0.001) }
    }

    @Test
    fun migration26To27_timestampTieKeepsProfileValue() {
        createDatabaseFromExportedSchema(version = 26)
        insertGoals("local-default", targetWeightKg = 70.0, updatedAt = 200L)
        insertProfile("local-default", goalWeightKg = 80.0, updatedAt = 200L) // tie → no recency evidence

        runMigration()

        queryProfile("local-default") { goalWeight, _, _, _ -> assertEquals(80.0, goalWeight!!, 0.001) }
    }

    private fun runMigration() {
        val roomDatabase =
            Room.databaseBuilder(context, MusFitDatabase::class.java, TEST_DATABASE_NAME)
                .addMigrations(
                    DatabaseModule.MIGRATION_26_27,
                    DatabaseModule.MIGRATION_27_28,
                    DatabaseModule.MIGRATION_28_29,
                    DatabaseModule.MIGRATION_29_30,
                    DatabaseModule.MIGRATION_30_31,
                    DatabaseModule.MIGRATION_31_32,
                    DatabaseModule.MIGRATION_32_33,
                )
                .build()
        try {
            roomDatabase.openHelper.writableDatabase.close()
        } finally {
            roomDatabase.close()
        }
    }

    private fun insertGoals(id: String, targetWeightKg: Double, updatedAt: Long) {
        exec(
            "INSERT INTO user_goals (id, stepGoal, weeklySessionTarget, targetWeightKg, updatedAtEpochMillis) " +
                "VALUES ('$id', 10000, 4, $targetWeightKg, $updatedAt)",
        )
    }

    private fun insertProfile(id: String, goalWeightKg: Double?, updatedAt: Long) {
        exec(
            "INSERT INTO user_profile (id, sex, birthDateEpochDay, heightCm, activityLevel, goalType, " +
                "goalPaceKgPerWeek, goalWeightKg, updatedAtEpochMillis) " +
                "VALUES ('$id', NULL, NULL, NULL, 'Moderate', 'Maintain', 0.5, ${goalWeightKg ?: "NULL"}, $updatedAt)",
        )
    }

    private fun exec(sql: String) {
        val database =
            SQLiteDatabase.openDatabase(
                context.getDatabasePath(TEST_DATABASE_NAME).path,
                null,
                SQLiteDatabase.OPEN_READWRITE,
            )
        try {
            database.execSQL(sql)
        } finally {
            database.close()
        }
    }

    private fun assertNoProfileRow(id: String) {
        val database =
            SQLiteDatabase.openDatabase(
                context.getDatabasePath(TEST_DATABASE_NAME).path,
                null,
                SQLiteDatabase.OPEN_READONLY,
            )
        try {
            database.rawQuery(
                "SELECT COUNT(*) FROM user_profile WHERE id = ?",
                arrayOf(id),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
        } finally {
            database.close()
        }
    }

    private fun queryProfile(id: String, block: (goalWeight: Double?, activityLevel: String, goalType: String, pace: Double) -> Unit) {
        val database =
            SQLiteDatabase.openDatabase(
                context.getDatabasePath(TEST_DATABASE_NAME).path,
                null,
                SQLiteDatabase.OPEN_READONLY,
            )
        try {
            database.rawQuery(
                "SELECT goalWeightKg, activityLevel, goalType, goalPaceKgPerWeek FROM user_profile WHERE id = ?",
                arrayOf(id),
            ).use { cursor ->
                assertTrue("Expected a user_profile row for id=$id", cursor.moveToFirst())
                val goalWeight = if (cursor.isNull(0)) null else cursor.getDouble(0)
                block(goalWeight, cursor.getString(1), cursor.getString(2), cursor.getDouble(3))
            }
        } finally {
            database.close()
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
        const val TEST_DATABASE_NAME = "goal-weight-26-27"
    }
}
