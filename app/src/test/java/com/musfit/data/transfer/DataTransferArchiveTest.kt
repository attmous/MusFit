package com.musfit.data.transfer

import java.security.SecureRandom
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DataTransferArchiveTest {
    private val database = "SQLite format 3\u0000seeded-musfit-data".toByteArray()
    private val manifest = DataTransferManifest(
        createdAtEpochMillis = 1_752_000_000_000,
        sourceVersionCode = 512,
        databaseSchemaVersion = 36,
        databaseSha256 = DataTransferArchiveCodec.sha256(database),
        tableRowCounts = mapOf("foods" to 14, "workout_sessions" to 3, "accounts" to 1),
    )

    @Test
    fun roundTripPreservesManifestCountsAndDatabase() {
        val passphrase = "correct horse battery staple".toCharArray()
        val encoded = DataTransferArchiveCodec.encode(database, manifest, passphrase, deterministicRandom())
        val decoded = DataTransferArchiveCodec.decode(encoded, passphrase)

        assertEquals(manifest, decoded.manifest)
        assertEquals(18, decoded.manifest.totalRows)
        assertArrayEquals(database, decoded.databaseBytes)
    }

    @Test
    fun randomSaltAndIvProduceDifferentArchives() {
        val passphrase = "correct horse battery staple".toCharArray()
        val first = DataTransferArchiveCodec.encode(database, manifest, passphrase)
        val second = DataTransferArchiveCodec.encode(database, manifest, passphrase)

        assertNotEquals(first.toList(), second.toList())
    }

    @Test
    fun wrongPassphraseAndTamperingHaveOneAuthenticationFailure() {
        val encoded = DataTransferArchiveCodec.encode(
            database,
            manifest,
            "correct horse battery staple".toCharArray(),
            deterministicRandom(),
        )

        val wrong = assertThrows(DataTransferArchiveException::class.java) {
            DataTransferArchiveCodec.decode(encoded, "incorrect horse battery staple".toCharArray())
        }
        assertTrue(wrong.message.orEmpty().contains("authenticated"))

        encoded[encoded.lastIndex] = (encoded.last().toInt() xor 1).toByte()
        val tampered = assertThrows(DataTransferArchiveException::class.java) {
            DataTransferArchiveCodec.decode(encoded, "correct horse battery staple".toCharArray())
        }
        assertEquals(wrong.message, tampered.message)
    }

    @Test
    fun rejectsShortPassphraseAndMismatchedChecksum() {
        assertThrows(IllegalArgumentException::class.java) {
            DataTransferArchiveCodec.encode(database, manifest, "too short".toCharArray())
        }
        assertThrows(IllegalArgumentException::class.java) {
            DataTransferArchiveCodec.encode(
                database,
                manifest.copy(databaseSha256 = "0".repeat(64)),
                "correct horse battery staple".toCharArray(),
            )
        }
    }

    private fun deterministicRandom(): SecureRandom =
        object : SecureRandom() {
            private var next = 1
            override fun nextBytes(bytes: ByteArray) {
                bytes.indices.forEach { bytes[it] = next++.toByte() }
            }
        }
}
