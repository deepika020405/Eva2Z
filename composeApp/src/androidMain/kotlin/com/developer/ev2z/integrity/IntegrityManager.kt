package com.developer.ev2z.integrity

import android.content.Context
import android.util.Log
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

/**
 * Play Integrity API Manager
 * ✅ Safe version that works without Play Store
 */
class IntegrityManager(private val context: Context) {

    private val integrityManager by lazy {
        IntegrityManagerFactory.create(context)
    }

    companion object {
        // ⚠️ Replace with your project number from google-services.json
        // Find: "project_info" → "project_number"
        private const val CLOUD_PROJECT_NUMBER = 26586623232L

        // ✅ Enable/disable integrity checks
        private const val ENABLE_INTEGRITY = false  // Set false for testing
    }

    /**
     * Request integrity token
     * Returns null if disabled or fails (non-critical)
     */
    suspend fun requestIntegrityToken(
        requestIdentifier: String = UUID.randomUUID().toString()
    ): String? {
        // ✅ Skip if disabled (for testing/development)
        if (!ENABLE_INTEGRITY) {
            Log.w("IntegrityManager", "⚠️ Integrity checks disabled")
            return null
        }

        // ✅ Check if project number is set
        if (CLOUD_PROJECT_NUMBER == 0L) {
            Log.w("IntegrityManager", "⚠️ Project number not configured")
            return null
        }

        return try {
            val nonce = generateNonce(requestIdentifier)

            val request = IntegrityTokenRequest.builder()
                .setNonce(nonce)
                .setCloudProjectNumber(CLOUD_PROJECT_NUMBER)
                .build()

            val response = integrityManager
                .requestIntegrityToken(request)
                .await()

            Log.d("IntegrityManager", "✅ Token received")
            response.token()
        } catch (e: Exception) {
            // ✅ Don't crash - just log and return null
            Log.e("IntegrityManager", "❌ Token request failed: ${e.message}")
            null
        }
    }

    /**
     * Check if integrity is available and enabled
     */
    fun isIntegrityAvailable(): Boolean {
        return ENABLE_INTEGRITY && CLOUD_PROJECT_NUMBER != 0L
    }

    /**
     * Generate secure nonce
     */
    private fun generateNonce(identifier: String): String {
        val timestamp = System.currentTimeMillis().toString()
        val combined = "$identifier-$timestamp"
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(combined.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}

/**
 * ✅ USAGE:
 *
 * 1. For testing (no Play Store needed):
 *    Set ENABLE_INTEGRITY = false
 *
 * 2. For production:
 *    - Set ENABLE_INTEGRITY = true
 *    - Update CLOUD_PROJECT_NUMBER from google-services.json
 *
 * 3. In your code:
 *    val manager = IntegrityManager(context)
 *    if (manager.isIntegrityAvailable()) {
 *        val token = manager.requestIntegrityToken()
 *        // Send to backend
 *    } else {
 *        // Skip integrity check
 *    }
 */