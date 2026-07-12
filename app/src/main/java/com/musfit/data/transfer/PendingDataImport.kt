package com.musfit.data.transfer

import android.content.Context
import com.musfit.data.local.MUSFIT_DATABASE_NAME
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

data class DataTransferReceipt(
    val restoredAtEpochMillis: Long,
    val databaseSha256: String,
    val totalRows: Long,
    val archiveBytes: Long,
)

object PendingDataImport {
    fun stage(
        context: Context,
        databaseBytes: ByteArray,
        manifest: DataTransferManifest,
        archiveBytes: Long,
    ): DatabaseSnapshotReport {
        val directory = transferDirectory(context).apply { mkdirs() }
        val temporary = File(directory, "pending-musfit.db.tmp")
        val staged = File(directory, "pending-musfit.db")
        temporary.writeSynced(databaseBytes)
        val report = MusFitDatabaseSnapshot.requireMatches(temporary, manifest)
        check(temporary.renameReplacing(staged)) { "Could not stage the verified MusFit database." }
        writeMarker(File(directory, "pending-import.bin"), manifest, archiveBytes)
        return report
    }

    /** Called before any activity injects a Room-backed repository. */
    fun applyIfPending(context: Context): DataTransferReceipt? {
        val directory = transferDirectory(context)
        val staged = File(directory, "pending-musfit.db")
        val marker = File(directory, "pending-import.bin")
        if (!staged.isFile || !marker.isFile) return null

        val pending = readMarker(marker)
        MusFitDatabaseSnapshot.requireMatches(staged, pending.manifest)
        val database = context.getDatabasePath(MUSFIT_DATABASE_NAME)
        database.parentFile?.mkdirs()
        val backup = File(directory, "pre-import-musfit.db")
        if (database.isFile) database.copySyncedTo(backup) else backup.delete()

        val wal = File(database.absolutePath + "-wal")
        val shm = File(database.absolutePath + "-shm")
        try {
            wal.delete()
            shm.delete()
            staged.copySyncedTo(database)
            MusFitDatabaseSnapshot.requireMatches(database, pending.manifest)
        } catch (error: Throwable) {
            if (backup.isFile) backup.copySyncedTo(database) else database.delete()
            throw IllegalStateException("MusFit data restore failed; the previous database was retained.", error)
        }

        val receipt = DataTransferReceipt(
            restoredAtEpochMillis = System.currentTimeMillis(),
            databaseSha256 = pending.manifest.databaseSha256,
            totalRows = pending.manifest.totalRows,
            archiveBytes = pending.archiveBytes,
        )
        saveReceipt(context, receipt)
        staged.delete()
        marker.delete()
        backup.delete()
        return receipt
    }

    fun lastReceipt(context: Context): DataTransferReceipt? {
        val preferences = context.getSharedPreferences(RECEIPT_PREFERENCES, Context.MODE_PRIVATE)
        val sha = preferences.getString("sha256", null) ?: return null
        return DataTransferReceipt(
            restoredAtEpochMillis = preferences.getLong("restoredAt", 0),
            databaseSha256 = sha,
            totalRows = preferences.getLong("totalRows", 0),
            archiveBytes = preferences.getLong("archiveBytes", 0),
        )
    }

    fun recordFailure(context: Context, error: Throwable) {
        context.getSharedPreferences(RECEIPT_PREFERENCES, Context.MODE_PRIVATE).edit()
            .putString("failure", error.message ?: "MusFit restore could not be completed.")
            .commit()
    }

    fun lastFailure(context: Context): String? =
        context.getSharedPreferences(RECEIPT_PREFERENCES, Context.MODE_PRIVATE).getString("failure", null)

    private fun saveReceipt(context: Context, receipt: DataTransferReceipt) {
        context.getSharedPreferences(RECEIPT_PREFERENCES, Context.MODE_PRIVATE).edit()
            .putLong("restoredAt", receipt.restoredAtEpochMillis)
            .putString("sha256", receipt.databaseSha256)
            .putLong("totalRows", receipt.totalRows)
            .putLong("archiveBytes", receipt.archiveBytes)
            .remove("failure")
            .commit()
    }

    private fun writeMarker(file: File, manifest: DataTransferManifest, archiveBytes: Long) {
        val temporary = File(file.parentFile, file.name + ".tmp")
        FileOutputStream(temporary).use { stream ->
            DataOutputStream(stream).use { output ->
                output.writeInt(MARKER_VERSION)
                output.writeLong(manifest.createdAtEpochMillis)
                output.writeInt(manifest.sourceVersionCode)
                output.writeInt(manifest.databaseSchemaVersion)
                output.writeUTF(manifest.databaseSha256)
                output.writeInt(manifest.tableRowCounts.size)
                manifest.tableRowCounts.toSortedMap().forEach { (table, count) ->
                    output.writeUTF(table)
                    output.writeLong(count)
                }
                output.writeLong(archiveBytes)
                output.flush()
                stream.fd.sync()
            }
        }
        check(temporary.renameReplacing(file)) { "Could not stage MusFit restore metadata." }
    }

    private fun readMarker(file: File): PendingMarker =
        DataInputStream(FileInputStream(file)).use { input ->
            require(input.readInt() == MARKER_VERSION) { "Unsupported pending MusFit restore metadata." }
            val createdAt = input.readLong()
            val sourceVersion = input.readInt()
            val schema = input.readInt()
            val sha = input.readUTF()
            val tableCount = input.readInt()
            require(tableCount in 0..512)
            val counts = buildMap {
                repeat(tableCount) { put(input.readUTF(), input.readLong()) }
            }
            val archiveBytes = input.readLong()
            require(input.read() == -1)
            PendingMarker(
                manifest = DataTransferManifest(createdAt, sourceVersion, schema, sha, counts),
                archiveBytes = archiveBytes,
            )
        }

    private fun transferDirectory(context: Context): File = File(context.filesDir, "data-transfer")

    private fun File.writeSynced(bytes: ByteArray) {
        parentFile?.mkdirs()
        FileOutputStream(this).use { stream ->
            stream.write(bytes)
            stream.flush()
            stream.fd.sync()
        }
    }

    private fun File.copySyncedTo(destination: File) {
        destination.parentFile?.mkdirs()
        inputStream().use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
                output.flush()
                output.fd.sync()
            }
        }
    }

    private fun File.renameReplacing(destination: File): Boolean {
        if (destination.exists() && !destination.delete()) return false
        return renameTo(destination)
    }

    private data class PendingMarker(val manifest: DataTransferManifest, val archiveBytes: Long)

    private const val MARKER_VERSION = 1
    private const val RECEIPT_PREFERENCES = "data_transfer_receipt"
}
