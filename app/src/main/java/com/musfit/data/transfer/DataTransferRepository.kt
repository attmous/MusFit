package com.musfit.data.transfer

import android.content.Context
import android.net.Uri
import com.musfit.BuildConfig
import com.musfit.data.local.MusFitDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DataTransferReport(
    val databaseSha256: String,
    val tableRowCounts: Map<String, Long>,
    val databaseBytes: Long,
    val archiveBytes: Long,
) {
    val totalRows: Long = tableRowCounts.values.sum()
    val checksumLabel: String = databaseSha256.take(12)
}

interface DataTransferRepository {
    suspend fun exportTo(uri: Uri, passphrase: CharArray): DataTransferReport
    suspend fun stageImportFrom(uri: Uri, passphrase: CharArray): DataTransferReport
    fun lastRestoreReceipt(): DataTransferReceipt?
    fun lastRestoreFailure(): String?
}

@Singleton
class AndroidDataTransferRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: MusFitDatabase,
) : DataTransferRepository {
    override suspend fun exportTo(uri: Uri, passphrase: CharArray): DataTransferReport = withContext(Dispatchers.IO) {
        val snapshot = File(context.cacheDir, "data-transfer/export-${System.nanoTime()}.db")
        try {
            val snapshotReport = MusFitDatabaseSnapshot.create(database, snapshot)
            val databaseBytes = snapshot.readBytes()
            val manifest = DataTransferManifest(
                createdAtEpochMillis = System.currentTimeMillis(),
                sourceVersionCode = BuildConfig.VERSION_CODE,
                databaseSchemaVersion = snapshotReport.schemaVersion,
                databaseSha256 = snapshotReport.sha256,
                tableRowCounts = snapshotReport.tableRowCounts,
            )
            val archive = DataTransferArchiveCodec.encode(databaseBytes, manifest, passphrase)
            try {
                context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                    output.write(archive)
                    output.flush()
                } ?: throw IllegalStateException("Could not open the selected backup file.")
                DataTransferReport(
                    databaseSha256 = manifest.databaseSha256,
                    tableRowCounts = manifest.tableRowCounts,
                    databaseBytes = databaseBytes.size.toLong(),
                    archiveBytes = archive.size.toLong(),
                )
            } finally {
                archive.fill(0)
                databaseBytes.fill(0)
            }
        } finally {
            snapshot.delete()
        }
    }

    override suspend fun stageImportFrom(uri: Uri, passphrase: CharArray): DataTransferReport = withContext(Dispatchers.IO) {
        val archive = context.contentResolver.openInputStream(uri)?.use(::readBoundedArchive)
            ?: throw IllegalStateException("Could not open the selected backup file.")
        try {
            val decoded = DataTransferArchiveCodec.decode(archive, passphrase)
            try {
                val report = PendingDataImport.stage(
                    context = context,
                    databaseBytes = decoded.databaseBytes,
                    manifest = decoded.manifest,
                    archiveBytes = archive.size.toLong(),
                )
                DataTransferReport(
                    databaseSha256 = report.sha256,
                    tableRowCounts = report.tableRowCounts,
                    databaseBytes = decoded.databaseBytes.size.toLong(),
                    archiveBytes = archive.size.toLong(),
                )
            } finally {
                decoded.databaseBytes.fill(0)
            }
        } finally {
            archive.fill(0)
        }
    }

    override fun lastRestoreReceipt(): DataTransferReceipt? = PendingDataImport.lastReceipt(context)
    override fun lastRestoreFailure(): String? = PendingDataImport.lastFailure(context)

    private fun readBoundedArchive(input: java.io.InputStream): ByteArray {
        val maximum = DataTransferArchiveCodec.MAXIMUM_DATABASE_BYTES + 1_048_576
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            require(total <= maximum) { "MusFit backup exceeds the 257 MiB archive limit." }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }
}
