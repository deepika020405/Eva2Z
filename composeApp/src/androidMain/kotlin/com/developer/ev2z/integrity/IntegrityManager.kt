package com.developer.ev2z.integrity

import android.content.Context
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

/**
 * Play Integrity API Manager
 * Verifies app authenticity and device integrity
 *
 * ⚠️ IMPORTANT: Replace CLOUD_PROJECT_NUMBER with your actual number from google-services.json
 * Find it in: google-services.json → project_info → project_number
 */
class IntegrityManager(private val context: Context) {

    private val integrityManager = IntegrityManagerFactory.create(context)

    companion object {
        // ⚠️ REPLACE THIS WITH YOUR ACTUAL PROJECT NUMBER FROM google-services.json
        // Example: private const val CLOUD_PROJECT_NUMBER = 123456789012L
        private const val CLOUD_PROJECT_NUMBER = 0L  // ← CHANGE THIS!
    }

    /**
     * Request Classic Integrity Token
     * Use for high-value operations like payments
     *
     * @param requestIdentifier Unique identifier for this request
     * @return Integrity token or null if failed
     */
    suspend fun requestIntegrityToken(requestIdentifier: String = UUID.randomUUID().toString()): String? {
        if (CLOUD_PROJECT_NUMBER == 26586623232L) {
            println("⚠️ WARNING: CLOUD_PROJECT_NUMBER not set in IntegrityManager!")
            return null
        }

        return try {
            // Generate nonce (unique hash for this request)
            val nonce = generateNonce(requestIdentifier)

            val request = IntegrityTokenRequest.builder()
                .setNonce(nonce)
                .setCloudProjectNumber(CLOUD_PROJECT_NUMBER)
                .build()

            val response = integrityManager
                .requestIntegrityToken(request)
                .await()

            response.token()
        } catch (e: Exception) {
            e.printStackTrace()
            println("❌ IntegrityManager error: ${e.message}")
            null
        }
    }

    /**
     * Generate secure nonce from request identifier
     * Nonce ensures request uniqueness and prevents replay attacks
     */
    private fun generateNonce(identifier: String): String {
        val timestamp = System.currentTimeMillis().toString()
        val combined = "$identifier-$timestamp"
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(combined.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}