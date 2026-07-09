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
class LegacyVersion28RepairTest {
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
    fun repairLegacyHealthSyncVersion28Database_allowsCurrentRoomOpenWithoutDataWipe() {
        createLegacyHealthSyncVersion28Database()
        insertLegacyRows()

        DatabaseModule.repairLegacyVersion28Database(context, TEST_DATABASE_NAME)
        assertEquals(DatabaseModule.CURRENT_V28_IDENTITY_HASH, readRoomIdentityHash())

        val roomDatabase =
            Room.databaseBuilder(context, MusFitDatabase::class.java, TEST_DATABASE_NAME)
                .addMigrations(
                    DatabaseModule.MIGRATION_28_29,
                    DatabaseModule.MIGRATION_29_30,
                    DatabaseModule.MIGRATION_30_31,
                    DatabaseModule.MIGRATION_31_32,
                    DatabaseModule.MIGRATION_32_33,
                    DatabaseModule.MIGRATION_33_34,
                    DatabaseModule.MIGRATION_34_35,
                )
                .build()
        try {
            roomDatabase.openHelper.writableDatabase.close()
        } finally {
            roomDatabase.close()
        }

        val database =
            SQLiteDatabase.openDatabase(
                context.getDatabasePath(TEST_DATABASE_NAME).path,
                null,
                SQLiteDatabase.OPEN_READONLY,
            )
        try {
            assertEquals(CURRENT_DATABASE_IDENTITY_HASH, database.readRoomIdentityHash())
            assertTrue(tableColumns(database, "accounts").containsAll(listOf("authProvider", "avatarUrl")))
            val summaryColumns = tableColumns(database, "daily_health_summaries")
            assertTrue(summaryColumns.containsAll(CURRENT_DAILY_HEALTH_SUMMARY_COLUMNS))
            assertTrue(tableColumns(database, "ai_coach_settings").contains("accountId"))

            database.rawQuery(
                "SELECT displayName, authProvider, avatarUrl FROM accounts WHERE id = 'local-default'",
                null,
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("You", cursor.getString(0))
                assertEquals("local", cursor.getString(1))
                assertTrue(cursor.isNull(2))
            }
            database.rawQuery(
                """
                SELECT steps, activeCaloriesKcal, latestWeightKg, restingHeartRateBpm
                FROM daily_health_summaries
                WHERE dateEpochDay = 1
                """.trimIndent(),
                null,
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1234, cursor.getInt(0))
                assertEquals(250.0, cursor.getDouble(1), 0.001)
                assertEquals(82.5, cursor.getDouble(2), 0.001)
                assertEquals(58, cursor.getInt(3))
            }
            assertTrue(summaryColumns.contains("totalCaloriesKcal"))
        } finally {
            database.close()
        }
    }

    private fun createLegacyHealthSyncVersion28Database() {
        val schemaFile = resolveSchemaFile(version = 28)
        val schemaJson = JSONObject(schemaFile.readText())
        val databaseJson = schemaJson.getJSONObject("database")
        val databaseFile = context.getDatabasePath(TEST_DATABASE_NAME)

        databaseFile.parentFile?.mkdirs()
        val database = SQLiteDatabase.openOrCreateDatabase(databaseFile, null)
        try {
            val entities = databaseJson.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val tableName = entity.getString("tableName")
                when (tableName) {
                    "accounts" -> createLegacyAccountsTable(database)
                    "daily_health_summaries" -> createLegacyDailyHealthSummariesTable(database)
                    else -> {
                        database.execSQL(resolveSchemaSql(entity.getString("createSql"), tableName))
                        val indices = entity.optJSONArray("indices") ?: continue
                        for (indexPosition in 0 until indices.length()) {
                            val entityIndex = indices.getJSONObject(indexPosition)
                            database.execSQL(resolveSchemaSql(entityIndex.getString("createSql"), tableName))
                        }
                    }
                }
            }
            database.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
            database.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES(42, ?)",
                arrayOf<Any>(LEGACY_HEALTH_SYNC_V28_IDENTITY_HASH),
            )
            database.version = 28
        } finally {
            database.close()
        }
    }

    private fun createLegacyAccountsTable(database: SQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS accounts (
                id TEXT NOT NULL PRIMARY KEY,
                displayName TEXT NOT NULL,
                email TEXT,
                remoteUserId TEXT,
                createdAtEpochMillis INTEGER NOT NULL,
                updatedAtEpochMillis INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_email ON accounts(email)")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_accounts_remoteUserId ON accounts(remoteUserId)")
    }

    private fun createLegacyDailyHealthSummariesTable(database: SQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS daily_health_summaries (
                dateEpochDay INTEGER NOT NULL PRIMARY KEY,
                steps INTEGER,
                activeCaloriesKcal REAL,
                totalCaloriesKcal REAL,
                distanceMeters REAL,
                sleepMinutes INTEGER,
                exerciseMinutes INTEGER,
                exerciseSessionCount INTEGER,
                latestWeightKg REAL,
                latestBodyFatPercent REAL,
                restingHeartRateBpm INTEGER,
                updatedAtEpochMillis INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    private fun insertLegacyRows() {
        val database =
            SQLiteDatabase.openDatabase(
                context.getDatabasePath(TEST_DATABASE_NAME).path,
                null,
                SQLiteDatabase.OPEN_READWRITE,
            )
        try {
            database.execSQL(
                """
                INSERT INTO accounts (
                    id, displayName, email, remoteUserId, createdAtEpochMillis, updatedAtEpochMillis
                ) VALUES (
                    'local-default', 'You', NULL, NULL, 0, 0
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO daily_health_summaries (
                    dateEpochDay, steps, activeCaloriesKcal, totalCaloriesKcal, distanceMeters,
                    sleepMinutes, exerciseMinutes, exerciseSessionCount, latestWeightKg,
                    latestBodyFatPercent, restingHeartRateBpm, updatedAtEpochMillis
                ) VALUES (
                    1, 1234, 250.0, 2000.0, 1500.0, 480, 35, 1, 82.5, 18.0, 58, 99
                )
                """.trimIndent(),
            )
        } finally {
            database.close()
        }
    }

    private fun readRoomIdentityHash(): String? =
        SQLiteDatabase.openDatabase(
            context.getDatabasePath(TEST_DATABASE_NAME).path,
            null,
            SQLiteDatabase.OPEN_READONLY,
        ).use { database ->
            database.readRoomIdentityHash()
        }

    private fun SQLiteDatabase.readRoomIdentityHash(): String? =
        rawQuery("SELECT identity_hash FROM room_master_table WHERE id = 42 LIMIT 1", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }

    private fun tableColumns(database: SQLiteDatabase, tableName: String): List<String> =
        database.rawQuery("PRAGMA table_info(`$tableName`)", null).use { cursor ->
            buildList {
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) {
                    add(cursor.getString(nameIndex))
                }
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
        const val TEST_DATABASE_NAME = "legacy-v28-repair"
        const val LEGACY_HEALTH_SYNC_V28_IDENTITY_HASH = "71b5b71f394a9a0bedf45d1a67317f04"
        const val CURRENT_DATABASE_IDENTITY_HASH = "2a7735b2e2d090f9d9d380fa0cc83ca5"

        val CURRENT_DAILY_HEALTH_SUMMARY_COLUMNS = listOf(
            "dateEpochDay",
            "steps",
            "activeCaloriesKcal",
            "totalCaloriesKcal",
            "distanceMeters",
            "sleepMinutes",
            "exerciseMinutes",
            "exerciseSessionCount",
            "latestWeightKg",
            "latestBodyFatPercent",
            "restingHeartRateBpm",
            "hrvRmssdMillis",
            "updatedAtEpochMillis",
        )
    }
}
