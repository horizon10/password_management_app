package com.example.kotlinpassapp

import android.util.Log

object EncryptionUtil {
    private const val TAG = "EncryptionUtil"

    // WARNING: This is still not secure for production!
    // In a real app, consider using Android Keystore or a secure key storage solution
    private const val secretKey = "KG7oB4n0u63nPH1h" // 16 characters key for AES-128
    private const val IV_SEPARATOR = "::"

    /**
     * Encrypt a string using AES with a random IV
     */
    fun encrypt(input: String): String {
        return try {
            // Use Android's crypto libraries
            val cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS7Padding")
            val keySpec = javax.crypto.spec.SecretKeySpec(secretKey.toByteArray(), "AES")

            // Generate a random IV for each encryption
            val ivBytes = ByteArray(16)
            java.security.SecureRandom().nextBytes(ivBytes)
            val ivSpec = javax.crypto.spec.IvParameterSpec(ivBytes)

            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            val encryptedBytes = cipher.doFinal(input.toByteArray(Charsets.UTF_8))

            // Store IV with encrypted data
            val ivBase64 = android.util.Base64.encodeToString(ivBytes, android.util.Base64.DEFAULT)
            val encryptedBase64 = android.util.Base64.encodeToString(encryptedBytes, android.util.Base64.DEFAULT)

            // Format: base64(IV)::base64(encryptedData)
            "$ivBase64$IV_SEPARATOR$encryptedBase64"
        } catch (e: Exception) {
            Log.e(TAG, "Encryption error: ${e.message}")
            e.printStackTrace()
            ""
        }
    }

    /**
     * Decrypt a string using AES with the stored IV
     */
    fun decrypt(encrypted: String): String {
        return try {
            // Split IV and encrypted data
            val parts = encrypted.split(IV_SEPARATOR)

            // Handle legacy data without IV
            if (parts.size == 1) {
                return decryptLegacy(encrypted)
            }

            val ivBase64 = parts[0]
            val encryptedBase64 = parts[1]

            val ivBytes = android.util.Base64.decode(ivBase64, android.util.Base64.DEFAULT)
            val encryptedBytes = android.util.Base64.decode(encryptedBase64, android.util.Base64.DEFAULT)

            // Use Android's crypto libraries
            val cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS7Padding")
            val keySpec = javax.crypto.spec.SecretKeySpec(secretKey.toByteArray(), "AES")
            val ivSpec = javax.crypto.spec.IvParameterSpec(ivBytes)

            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec, ivSpec)
            val decryptedBytes = cipher.doFinal(encryptedBytes)

            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption error: ${e.message}")
            e.printStackTrace()

            // Try legacy decryption as fallback
            decryptLegacy(encrypted)
        }
    }

    /**
     * Legacy decryption for backward compatibility
     */
    private fun decryptLegacy(encrypted: String): String {
        return try {
            val cipher = javax.crypto.Cipher.getInstance("AES")
            val keySpec = javax.crypto.spec.SecretKeySpec(secretKey.toByteArray(), "AES")

            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec)
            val decodedBytes = android.util.Base64.decode(encrypted, android.util.Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)

            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Legacy decryption error: ${e.message}")
            e.printStackTrace()
            ""
        }
    }
}