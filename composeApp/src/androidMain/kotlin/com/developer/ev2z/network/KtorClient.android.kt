package com.developer.ev2z.network

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import okhttp3.ConnectionPool
import java.util.concurrent.TimeUnit

actual fun createHttpClient(): HttpClient {
    return HttpClient(OkHttp) {
        engine {
            config {
                // "unexpected end of stream" is OkHttp reusing a keep-alive
                // connection the server had already closed. OkHttp retries that by
                // itself for ordinary requests, but not for a multipart upload —
                // its body is one-shot and cannot be replayed — so the file upload
                // is the one call that surfaces it.
                //
                // app.eva2z.in was observed dropping a connection idle for only ~5s
                // (2026-09-19), so idle connections are evicted well inside that: a
                // fresh TLS handshake costs far less than a failed upload.
                connectionPool(ConnectionPool(5, 2, TimeUnit.SECONDS))
                retryOnConnectionFailure(true)
            }
        }

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = true
                encodeDefaults = true   // ⭐ THIS IS THE FIX
            })
        }

        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.NONE
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 60000
            connectTimeoutMillis = 60000
            socketTimeoutMillis = 60000
        }
    }
}