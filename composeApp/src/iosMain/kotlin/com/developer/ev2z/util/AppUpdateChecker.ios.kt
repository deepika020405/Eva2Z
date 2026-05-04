package com.developer.ev2z.util

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import platform.Foundation.NSBundle

actual suspend fun isUpdateAvailable(): Boolean {
    val currentVersion = NSBundle.mainBundle.infoDictionary
        ?.get("CFBundleShortVersionString") as? String ?: return false

    return try {
        val client = HttpClient(Darwin)
        val response = client.get(
            "https://itunes.apple.com/lookup?bundleId=com.agastya.ev2z.connect"
        )
        val body = response.bodyAsText()
        client.close()

        val json = Json.Default.parseToJsonElement(body)
        val storeVersion = json.jsonObject["results"]
            ?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("version")
            ?.jsonPrimitive?.content

        storeVersion?.let { compareVersions(it, currentVersion) > 0 } ?: false
    } catch (_: Exception) {
        false
    }
}