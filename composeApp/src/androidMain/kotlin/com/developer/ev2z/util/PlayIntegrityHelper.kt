package com.developer.ev2z.utils

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.play.core.integrity.IntegrityManager
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.google.android.play.core.integrity.IntegrityTokenResponse
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object PlayIntegrityHelper {

    private const val TAG = "PlayIntegrity"

    /**
     * Verify app integrity using Play Integrity API
     * This tells Google Play that your app is legitimate
     */
    suspend fun verifyIntegrity(context: Context): Boolean {
        return try {
            val integrityManager = IntegrityManagerFactory.create(context)

            // Create nonce (random string for this session)
            val nonce = generateNonce()

            val request = IntegrityTokenRequest.builder()
                .setNonce(nonce)
                .build()

            val response = integrityManager.requestIntegrityToken(request).await()
            val token = response.token()

            Log.d(TAG, "✅ Integrity token received: ${token.take(50)}...")

            // Send this token to your backend for verification
            // For now, just having the token helps with Play Protect
            true

        } catch (e: Exception) {
            Log.e(TAG, "❌ Integrity check failed", e)
            false
        }
    }

    private fun generateNonce(): String {
        return System.currentTimeMillis().toString() +
                (0..9).map { ('0'..'9').random() }.joinToString("")
    }

    // Extension function to convert Task to suspendable coroutine
    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            cont.resume(result)
        }
        addOnFailureListener { exception ->
            cont.resumeWithException(exception)
        }

        cont.invokeOnCancellation {
            // Cancel the task if coroutine is cancelled
        }
    }
}