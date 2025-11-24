package com.developer.ev2z.network

import io.ktor.client.*

expect fun createHttpClient(): HttpClient

object KtorClient {
    const val BASE_URL = "https://gps.eva2z.in/api/"
    val client: HttpClient by lazy { createHttpClient() }
}