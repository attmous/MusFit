package com.musfit.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.musfit.core.di.DatabaseModule
import com.musfit.data.local.entity.HealthConnectExportRecordEntity
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class HealthConnectExportIdentityMigration39To40Test {
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
    fun migration39To40_addsAccountOwnedExportIdentityLedger() {
        createDatabaseFromExportedSchema(39)
        val room = Room.databaseBuilder(context, MusFitDatabase::class.java, DATABASE_NAME)
            .addMigrations(DatabaseModule.MIGRATION_39_40, DatabaseModule.MIGRATION_40_41, DatabaseModule.MIGRATION_41_42)
            .allowMainThreadQueries()
            .build()
        room.openHelper.writableDatabase
        val record = HealthConnectExportRecordEntity(
            accountId = "local-default",
            recordType = "workout",
            localEntityId = "session-1",
            clientRecordId = "client-1",
            clientRecordVersion = 1,
            payloadFingerprint = "fingerprint-1",
            providerRecordId = "provider-1",
            exportedAtEpochMillis = 1_000L,
        )

        kotlinx.coroutines.runBlocking {
            room.healthDao().upsertHealthConnectExportRecord(record)
            assertEquals(
                record,
                room.healthDao().getHealthConnectExportRecord("local-default", "workout", "session-1"),
            )
            room.openHelper.writableDatabase.execSQL("DELETE FROM accounts WHERE id = 'local-default'")
            assertNull(room.healthDao().getHealthConnectExportRecord("local-default", "workout", "session-1"))
        }
        room.close()
    }

    private fun createDatabaseFromExportedSchema(version: Int) {
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
            database.execSQL(
                "INSERT INTO accounts VALUES ('local-default', 'You', NULL, NULL, 'local', NULL, 0, 0)",
            )
            database.version = version
        }
    }

    private fun resolveSchemaFile(version: Int): File {
        val relative = "schemas/com.musfit.data.local.MusFitDatabase/$version.json"
        return listOf(File(relative), File("app/$relative"), File("../app/$relative")).firstOrNull(File::exists)
            ?: error("Could not find exported Room schema $version")
    }

    private companion object {
        const val DATABASE_NAME = "health-export-identity-39-40"
    }
}
