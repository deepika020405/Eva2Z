package com.developer.ev2z.network

import io.ktor.client.*
import io.ktor.client.engine.darwin.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

actual fun createHttpClient(): HttpClient {
    return HttpClient(Darwin) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = true
                encodeDefaults = true   // ⭐ MUST ADD HERE ALSO
            })
        }

        install(Logging) {
            logger = Logger.DEFAULT
            // HEADERS, not NONE: a Darwin transport failure (NSURLSession refusing the
            // request, a dropped multipart body) leaves no response for the call site
            // to print, so without this the Xcode console shows only "failed (null)".
            // Bodies stay out — the Authorization header is enough of a leak already.
            level = LogLevel.HEADERS
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 60000
            connectTimeoutMillis = 60000
            socketTimeoutMillis = 60000
        }
    }
}