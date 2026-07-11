package com.musfit.data.transfer

import android.database.sqlite.SQLiteDatabase
import com.musfit.data.local.MUSFIT_DATABASE_VERSION
import com.musfit.data.local.MusFitDatabase
import java.io.File

data class DatabaseSnapshotReport(
    val schemaVersion: Int,
    val sha256: String,
    val tableRowCounts: Map<String, Long>,
) {
    val totalRows: Long = tableRowCounts.values.sum()
}

/** Consistent Room snapshot creation and validation for data transfer only. */
object MusFitDatabaseSnapshot {
    fun create(database: MusFitDatabase, destination: File): DatabaseSnapshotReport {
        destination.parentFile?.mkdirs()
        check(!destination.exists() || destination.delete()) { "Could not replace stale database snapshot." }
        val sqlite = database.openHelper.writableDatabase
        sqlite.query("PRAGMA wal_checkpoint(FULL)").use { cursor ->
            check(cursor.moveToFirst()) { "Could not checkpoint the MusFit database." }
        }
        val escapedPath = destination.absolutePath.replace("'", "''")
        sqlite.execSQL("VACUUM INTO '$escapedPath'")
        sanitizeCredentialIndicators(destination)
        return inspect(destination)
    }

    fun inspect(databaseFile: File): DatabaseSnapshotReport {
        require(databaseFile.isFile) { "MusFit database snapshot is missing." }
        require(databaseFile.length() in 1..DataTransferArchiveCodec.MAXIMUM_DATABASE_BYTES.toLong()) {
            "MusFit database snapshot exceeds the transfer size limit."
        }
        val database = SQLiteDatabase.openDatabase(
            databaseFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY,
        )
        return try {
            checkIntegrity(database)
            val version = database.version
            require(version in 1..MUSFIT_DATABASE_VERSION) {
                "Backup schema $version is newer than this MusFit build ($MUSFIT_DATABASE_VERSION)."
            }
            DatabaseSnapshotReport(
                schemaVersion = version,
                sha256 = DataTransferArchiveCodec.sha256(databaseFile.readBytes()),
                tableRowCounts = readUserTableCounts(database),
            )
        } finally {
            database.close()
        }
    }

    fun requireMatches(databaseFile: File, manifest: DataTransferManifest): DatabaseSnapshotReport {
        val actual = inspect(databaseFile)
        require(actual.schemaVersion == manifest.databaseSchemaVersion) { "Backup schema metadata does not match its database." }
        require(actual.sha256 == manifest.databaseSha256) { "Backup database checksum does not match." }
        require(actual.tableRowCounts == manifest.tableRowCounts) { "Backup table counts do not match." }
        return actual
    }

    fun sanitizeCredentialIndicators(databaseFile: File) {
        val database = SQLiteDatabase.openDatabase(
            databaseFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE,
        )
        try {
            if (database.hasTable("ai_coach_settings")) {
                database.execSQL("UPDATE ai_coach_settings SET apiKeyStored = 0 WHERE apiKeyStored != 0")
            }
            database.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { cursor ->
                cursor.moveToFirst()
            }
        } finally {
            database.close()
        }
    }

    private fun checkIntegrity(database: SQLiteDatabase) {
        database.rawQuery("PRAGMA integrity_check", null).use { cursor ->
            require(cursor.moveToFirst() && cursor.getString(0) == "ok") { "Backup database failed integrity_check." }
        }
    }

    private fun readUserTableCounts(database: SQLiteDatabase): Map<String, Long> {
        val tables = mutableListOf<String>()
        database.rawQuery(
            """
            SELECT name FROM sqlite_master
            WHERE type = 'table'
              AND name NOT LIKE 'sqlite_%'
              AND name NOT IN ('android_metadata', 'room_master_table')
            ORDER BY name
            """.trimIndent(),
            null,
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val table = cursor.getString(0)
                require(SAFE_TABLE_NAME.matches(table)) { "Unsafe table name in MusFit database." }
                tables += table
            }
        }
        return tables.associateWith { table ->
            database.rawQuery("SELECT COUNT(*) FROM \"$table\"", null).use { cursor ->
                check(cursor.moveToFirst())
                cursor.getLong(0)
            }
        }
    }

    private fun SQLiteDatabase.hasTable(name: String): Boolean =
        rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ? LIMIT 1",
            arrayOf(name),
        ).use { it.moveToFirst() }

    private val SAFE_TABLE_NAME = Regex("[A-Za-z_][A-Za-z0-9_]*")
}
