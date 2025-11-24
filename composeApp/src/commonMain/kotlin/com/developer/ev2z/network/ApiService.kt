package com.developer.ev2z.network

import com.developer.ev2z.response.DeviceResponse
import com.developer.ev2z.response.MyDevicesResponse
import com.developer.ev2z.request.LoginRequest
import com.developer.ev2z.response.LoginResponse
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.submitForm
import io.ktor.http.*
import kotlinx.serialization.json.JsonObject

class ApiService {

    private val client = KtorClient.client
    private val baseUrl = KtorClient.BASE_URL

    suspend fun login(request: LoginRequest): LoginResponse {
        println("🌐 API: Sending login request to ${baseUrl}login")
        println("🌐 API: Request body: $request")

        try {
            val response = client.post("${baseUrl}login") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            println("🌐 API: Response status: ${response.status}")
            val result: LoginResponse = response.body()
            println("🌐 API: Response body: $result")
            return result

        } catch (e: Exception) {
            println("❌ API Error: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    // ... rest of your methods
}