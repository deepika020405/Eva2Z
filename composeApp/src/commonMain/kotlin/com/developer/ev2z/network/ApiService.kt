package com.developer.ev2z.network

import com.developer.ev2z.data.local.model.DeviceResponse
import com.developer.ev2z.request.LoginRequest
import com.developer.ev2z.response.LoginResponse
import com.developer.ev2z.response.MyDevicesResponse
import io.ktor.client.call.*
import io.ktor.client.request.*
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


    suspend fun getDevicesLatestLocations(token: String): DeviceResponse {
        println("🌐 API: Fetching latest device locations")
        println("🌐 API: Token: $token")

        try {
            val response = client.get("${baseUrl}user/devices/latest-locations") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
            }

            println("🌐 API: Response status: ${response.status}")
            val result: DeviceResponse = response.body()
            println("🌐 API: Devices fetched: ${result.totalDevices}")
            return result

        } catch (e: Exception) {
            println("❌ API Error: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }


    suspend fun addDevice(token: String, body: JsonObject): JsonObject {
        println("🌐 API: Adding device")

        try {
            val response = client.post("${baseUrl}add-device") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
                contentType(ContentType.Application.Json)
                setBody(body)
            }

            println("🌐 API: Response status: ${response.status}")
            val result: JsonObject = response.body()
            return result

        } catch (e: Exception) {
            println("❌ API Error: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    suspend fun updateDevice(token: String, body: JsonObject): JsonObject {
        println("🌐 API: Updating device")

        try {
            val response = client.post("${baseUrl}update-device") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
                contentType(ContentType.Application.Json)
                setBody(body)
            }

            println("🌐 API: Response status: ${response.status}")
            val result: JsonObject = response.body()
            return result

        } catch (e: Exception) {
            println("❌ API Error: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    suspend fun getMyDevices(token: String): MyDevicesResponse {
        println("🌐 API: Fetching my devices")

        try {
            val response = client.get("${baseUrl}devices/my-devices") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
            }

            println("🌐 API: Response status: ${response.status}")
            val result: MyDevicesResponse = response.body()
            println("🌐 API: Total devices: ${result.total_devices}")
            return result

        } catch (e: Exception) {
            println("❌ API Error: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}