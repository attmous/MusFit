package com.musfit.data.transfer

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.musfit.data.local.MUSFIT_DATABASE_VERSION
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MusFitDatabaseSnapshotTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val files = mutableListOf<File>()

    @After
    fun tearDown() {
        files.forEach { it.delete() }
        context.getDatabasePath("snapshot-test.db").delete()
        context.getDatabasePath("musfit.db").delete()
        File(context.filesDir, "data-transfer").deleteRecursively()
        context.getSharedPreferences("data_transfer_receipt", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun inspectCountsOnlyUserTablesAndSanitizesCredentialIndicator() {
        val databaseFile = createDatabase("snapshot-test.db", foodRows = 2, apiKeyStored = true)

        MusFitDatabaseSnapshot.sanitizeCredentialIndicators(databaseFile)
        val report = MusFitDatabaseSnapshot.inspect(databaseFile)

        assertEquals(MUSFIT_DATABASE_VERSION, report.schemaVersion)
        assertEquals(mapOf("ai_coach_settings" to 1L, "foods" to 2L), report.tableRowCounts)
        SQLiteDatabase.openDatabase(databaseFile.path, null, SQLiteDatabase.OPEN_READONLY).use { database ->
            database.rawQuery("SELECT apiKeyStored FROM ai_coach_settings", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun pendingImportReplacesDatabaseAndRecordsVerifiedReceipt() {
        val imported = createDatabase("snapshot-test.db", foodRows = 3, apiKeyStored = true)
        MusFitDatabaseSnapshot.sanitizeCredentialIndicators(imported)
        val report = MusFitDatabaseSnapshot.inspect(imported)
        val bytes = imported.readBytes()
        val manifest = DataTransferManifest(
            createdAtEpochMillis = 1_752_000_000_000,
            sourceVersionCode = 512,
            databaseSchemaVersion = report.schemaVersion,
            databaseSha256 = report.sha256,
            tableRowCounts = report.tableRowCounts,
        )
        createDatabase("musfit.db", foodRows = 1, apiKeyStored = false)

        PendingDataImport.stage(context, bytes, manifest, archiveBytes = 4_096)
        val receipt = PendingDataImport.applyIfPending(context)

        assertEquals(4L, receipt?.totalRows)
        assertEquals(report.sha256, receipt?.databaseSha256)
        assertEquals(report.tableRowCounts, MusFitDatabaseSnapshot.inspect(context.getDatabasePath("musfit.db")).tableRowCounts)
        assertEquals(receipt, PendingDataImport.lastReceipt(context))
        assertFalse(File(context.filesDir, "data-transfer/pending-musfit.db").exists())
    }

    @Test
    fun mismatchedManifestIsRejectedBeforeExistingDatabaseChanges() {
        val imported = createDatabase("snapshot-test.db", foodRows = 2, apiKeyStored = false)
        val report = MusFitDatabaseSnapshot.inspect(imported)
        val manifest = DataTransferManifest(
            createdAtEpochMillis = 1,
            sourceVersionCode = 1,
            databaseSchemaVersion = report.schemaVersion,
            databaseSha256 = report.sha256,
            tableRowCounts = report.tableRowCounts + ("foods" to 99L),
        )

        assertThrows(IllegalArgumentException::class.java) {
            PendingDataImport.stage(context, imported.readBytes(), manifest, archiveBytes = 100)
        }
    }

    private fun createDatabase(name: String, foodRows: Int, apiKeyStored: Boolean): File {
        val file = context.getDatabasePath(name).also(files::add)
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { database ->
            database.version = MUSFIT_DATABASE_VERSION
            database.execSQL("CREATE TABLE foods (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL)")
            repeat(foodRows) { database.execSQL("INSERT INTO foods VALUES (?, ?)", arrayOf("food-$it", "Food $it")) }
            database.execSQL("CREATE TABLE ai_coach_settings (accountId TEXT PRIMARY KEY NOT NULL, apiKeyStored INTEGER NOT NULL)")
            database.execSQL("INSERT INTO ai_coach_settings VALUES ('local-default', ?)", arrayOf(if (apiKeyStored) 1 else 0))
        }
        return file
    }
}
