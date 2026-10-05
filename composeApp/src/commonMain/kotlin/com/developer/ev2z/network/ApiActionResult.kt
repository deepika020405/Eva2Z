package com.developer.ev2z.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Outcome of a write call where the caller needs the server's reason, not just a
 * success flag — e.g. a community post edit rejected with 403 {"error":"Not authorized"}.
 */
data class ApiActionResult(
    val success: Boolean,
    val statusCode: Int = 0,
    val message: String? = null,
) {
    companion object {
        /** Pulls the human-readable reason out of Laravel's usual error shapes. */
        fun reasonFrom(rawBody: String): String? = try {
            val json = Json { ignoreUnknownKeys = true }
                .parseToJsonElement(rawBody) as? JsonObject
            listOf("error", "message")
                .firstNotNullOfOrNull { json?.get(it)?.jsonPrimitive?.content }
                ?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }
}