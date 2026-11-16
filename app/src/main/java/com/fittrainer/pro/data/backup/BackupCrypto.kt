package com.fittrainer.pro.data.backup

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupCrypto {
    const val MAX_BYTES = 20 * 1024 * 1024
    private const val MAGIC = "FITP1"
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val GCM_TAG_BITS = 128

    fun isEncrypted(bytes: ByteArray): Boolean =
        bytes.size > MAGIC.length && bytes.copyOfRange(0, MAGIC.length).toString(Charsets.US_ASCII) == MAGIC

    fun encrypt(plain: ByteArray, password: CharArray): ByteArray {
        require(password.isNotEmpty()) { "Informe uma senha para cifrar o backup" }
        val salt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LEN).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(password, salt), GCMParameterSpec(GCM_TAG_BITS, iv))
        val encrypted = cipher.doFinal(plain)
        return MAGIC.toByteArray(Charsets.US_ASCII) + salt + iv + encrypted
    }

    fun decrypt(blob: ByteArray, password: CharArray): ByteArray {
        require(isEncrypted(blob)) { "Arquivo não está cifrado" }
        require(password.isNotEmpty()) { "Informe a senha do backup" }
        val header = MAGIC.length
        require(blob.size > header + SALT_LEN + IV_LEN) { "Backup cifrado inválido" }
        val salt = blob.copyOfRange(header, header + SALT_LEN)
        val iv = blob.copyOfRange(header + SALT_LEN, header + SALT_LEN + IV_LEN)
        val payload = blob.copyOfRange(header + SALT_LEN + IV_LEN, blob.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(password, salt), GCMParameterSpec(GCM_TAG_BITS, iv))
        return runCatching { cipher.doFinal(payload) }.getOrElse {
            error("Senha incorreta ou arquivo corrompido")
        }
    }

    private fun key(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val encoded = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(encoded, "AES")
    }
}
