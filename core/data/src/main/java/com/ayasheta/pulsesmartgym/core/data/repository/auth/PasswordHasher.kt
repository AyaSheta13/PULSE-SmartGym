package com.ayasheta.pulsesmartgym.core.data.repository.auth

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Salted slow password hasher using PBKDF2-HMAC-SHA256.
 * Never stores or logs plaintext passwords.
 */
object PasswordHasher {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 100_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    data class HashedPassword(
        val saltBase64: String,
        val hashBase64: String,
    )

    fun hash(password: String): HashedPassword {
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)
        val derived = deriveKey(password, salt)
        return HashedPassword(
            saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP),
            hashBase64 = Base64.encodeToString(derived, Base64.NO_WRAP),
        )
    }

    fun verify(password: String, saltBase64: String, expectedHashBase64: String): Boolean {
        val salt = runCatching { Base64.decode(saltBase64, Base64.NO_WRAP) }.getOrNull() ?: return false
        val expectedHash = runCatching { Base64.decode(expectedHashBase64, Base64.NO_WRAP) }.getOrNull() ?: return false
        val actualHash = deriveKey(password, salt)
        return MessageDigest.isEqual(actualHash, expectedHash)
    }

    private fun deriveKey(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
