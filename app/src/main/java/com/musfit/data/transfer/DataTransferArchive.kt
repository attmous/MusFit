package com.musfit.data.transfer

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class DataTransferManifest(
    val createdAtEpochMillis: Long,
    val sourceVersionCode: Int,
    val databaseSchemaVersion: Int,
    val databaseSha256: String,
    val tableRowCounts: Map<String, Long>,
) {
    val totalRows: Long = tableRowCounts.values.sum()
}

data class DecodedDataTransferArchive(
    val manifest: DataTransferManifest,
    val databaseBytes: ByteArray,
)

class DataTransferArchiveException(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)

/**
 * Versioned, passphrase-protected MusFit database archive.
 *
 * The outer header is authenticated as AES-GCM AAD. The encrypted payload
 * contains both the manifest and database bytes so neither counts nor the
 * checksum can be changed independently. Passphrases are never persisted.
 */
object DataTransferArchiveCodec {
    const val MINIMUM_PASSPHRASE_LENGTH = 12
    const val MAXIMUM_DATABASE_BYTES = 256 * 1024 * 1024
    const val KDF_ITERATIONS = 310_000

    private const val FORMAT_VERSION = 1
    private const val KEY_BITS = 256
    private const val GCM_TAG_BITS = 128
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val MAX_TABLES = 512
    private val archiveMagic = "MUSFITX1".toByteArray(Charsets.US_ASCII)
    private val payloadMagic = "MUSFITDB1".toByteArray(Charsets.US_ASCII)

    fun encode(
        databaseBytes: ByteArray,
        manifest: DataTransferManifest,
        passphrase: CharArray,
        secureRandom: SecureRandom = SecureRandom(),
    ): ByteArray {
        requireValidPassphrase(passphrase)
        require(databaseBytes.size <= MAXIMUM_DATABASE_BYTES) { "MusFit database exceeds the 256 MiB transfer limit." }
        require(manifest.databaseSha256 == sha256(databaseBytes)) { "Database checksum does not match the manifest." }
        require(manifest.tableRowCounts.size <= MAX_TABLES) { "Too many database tables in transfer manifest." }

        val salt = ByteArray(SALT_BYTES).also(secureRandom::nextBytes)
        val iv = ByteArray(IV_BYTES).also(secureRandom::nextBytes)
        val header = headerBytes(salt, iv)
        val payload = payloadBytes(databaseBytes, manifest)
        val key = deriveKey(passphrase, salt)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.updateAAD(header)
            val ciphertext = cipher.doFinal(payload)
            ByteArrayOutputStream(header.size + ciphertext.size + Long.SIZE_BYTES).use { bytes ->
                DataOutputStream(bytes).use { output ->
                    output.write(header)
                    output.writeLong(ciphertext.size.toLong())
                    output.write(ciphertext)
                }
                bytes.toByteArray()
            }
        } finally {
            payload.fill(0)
            key.encoded?.fill(0)
        }
    }

    fun decode(archiveBytes: ByteArray, passphrase: CharArray): DecodedDataTransferArchive {
        requireValidPassphrase(passphrase)
        try {
            DataInputStream(ByteArrayInputStream(archiveBytes)).use { input ->
                val magic = input.readExactBytes(archiveMagic.size)
                if (!magic.contentEquals(archiveMagic)) throw invalidArchive()
                val formatVersion = input.readInt()
                if (formatVersion != FORMAT_VERSION) {
                    throw DataTransferArchiveException("Unsupported MusFit backup format version $formatVersion.")
                }
                val iterations = input.readInt()
                if (iterations != KDF_ITERATIONS) throw invalidArchive()
                val salt = input.readSizedBytes(expectedSize = SALT_BYTES)
                val iv = input.readSizedBytes(expectedSize = IV_BYTES)
                val header = headerBytes(salt, iv)
                val ciphertextLength = input.readLong()
                if (ciphertextLength <= GCM_TAG_BITS / 8 || ciphertextLength > MAXIMUM_DATABASE_BYTES.toLong() + 1_048_576L) {
                    throw invalidArchive()
                }
                val ciphertext = input.readExactBytes(ciphertextLength.toInt())
                if (input.read() != -1) throw invalidArchive()

                val key = deriveKey(passphrase, salt)
                val payload = try {
                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
                    cipher.updateAAD(header)
                    cipher.doFinal(ciphertext)
                } catch (error: AEADBadTagException) {
                    throw DataTransferArchiveException("Backup could not be authenticated. Check the passphrase and file.", error)
                } finally {
                    key.encoded?.fill(0)
                }
                return try {
                    decodePayload(payload)
                } finally {
                    payload.fill(0)
                }
            }
        } catch (error: DataTransferArchiveException) {
            throw error
        } catch (error: GeneralSecurityException) {
            throw DataTransferArchiveException("Backup could not be decrypted.", error)
        } catch (error: RuntimeException) {
            throw invalidArchive(error)
        }
    }

    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun payloadBytes(databaseBytes: ByteArray, manifest: DataTransferManifest): ByteArray =
        ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { output ->
                output.write(payloadMagic)
                output.writeLong(manifest.createdAtEpochMillis)
                output.writeInt(manifest.sourceVersionCode)
                output.writeInt(manifest.databaseSchemaVersion)
                output.writeUTF(manifest.databaseSha256)
                val sortedCounts = manifest.tableRowCounts.toSortedMap()
                output.writeInt(sortedCounts.size)
                sortedCounts.forEach { (table, count) ->
                    require(TABLE_NAME.matches(table)) { "Unsafe table name in transfer manifest." }
                    require(count >= 0) { "Negative row count in transfer manifest." }
                    output.writeUTF(table)
                    output.writeLong(count)
                }
                output.writeInt(databaseBytes.size)
                output.write(databaseBytes)
            }
            bytes.toByteArray()
        }

    private fun decodePayload(payload: ByteArray): DecodedDataTransferArchive {
        DataInputStream(ByteArrayInputStream(payload)).use { input ->
            if (!input.readExactBytes(payloadMagic.size).contentEquals(payloadMagic)) throw invalidArchive()
            val createdAt = input.readLong()
            val sourceVersionCode = input.readInt()
            val schemaVersion = input.readInt()
            val expectedSha256 = input.readUTF()
            if (!SHA_256.matches(expectedSha256)) throw invalidArchive()
            val tableCount = input.readInt()
            if (tableCount !in 0..MAX_TABLES) throw invalidArchive()
            val counts = buildMap {
                repeat(tableCount) {
                    val table = input.readUTF()
                    val count = input.readLong()
                    if (!TABLE_NAME.matches(table) || count < 0 || put(table, count) != null) throw invalidArchive()
                }
            }
            val databaseLength = input.readInt()
            if (databaseLength !in 1..MAXIMUM_DATABASE_BYTES) throw invalidArchive()
            val databaseBytes = input.readExactBytes(databaseLength)
            if (input.read() != -1) throw invalidArchive()
            if (sha256(databaseBytes) != expectedSha256) throw invalidArchive()
            return DecodedDataTransferArchive(
                manifest = DataTransferManifest(
                    createdAtEpochMillis = createdAt,
                    sourceVersionCode = sourceVersionCode,
                    databaseSchemaVersion = schemaVersion,
                    databaseSha256 = expectedSha256,
                    tableRowCounts = counts,
                ),
                databaseBytes = databaseBytes,
            )
        }
    }

    private fun headerBytes(salt: ByteArray, iv: ByteArray): ByteArray =
        ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { output ->
                output.write(archiveMagic)
                output.writeInt(FORMAT_VERSION)
                output.writeInt(KDF_ITERATIONS)
                output.writeInt(salt.size)
                output.write(salt)
                output.writeInt(iv.size)
                output.write(iv)
            }
            bytes.toByteArray()
        }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, KDF_ITERATIONS, KEY_BITS)
        return try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            SecretKeySpec(bytes, "AES").also { bytes.fill(0) }
        } finally {
            spec.clearPassword()
        }
    }

    private fun requireValidPassphrase(passphrase: CharArray) {
        require(passphrase.size >= MINIMUM_PASSPHRASE_LENGTH) {
            "Passphrase must be at least $MINIMUM_PASSPHRASE_LENGTH characters."
        }
    }

    private fun DataInputStream.readSizedBytes(expectedSize: Int): ByteArray {
        if (readInt() != expectedSize) throw invalidArchive()
        return readExactBytes(expectedSize)
    }

    private fun DataInputStream.readExactBytes(size: Int): ByteArray =
        ByteArray(size).also { bytes ->
            try {
                readFully(bytes)
            } catch (error: java.io.EOFException) {
                throw invalidArchive(error)
            }
        }

    private fun invalidArchive(cause: Throwable? = null) =
        DataTransferArchiveException("Invalid or damaged MusFit backup.", cause)

    private val TABLE_NAME = Regex("[A-Za-z_][A-Za-z0-9_]*")
    private val SHA_256 = Regex("[0-9a-f]{64}")
}
