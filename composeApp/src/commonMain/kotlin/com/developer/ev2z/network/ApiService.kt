package com.developer.ev2z.network

import com.developer.ev2z.data.local.model.DeviceResponse
import com.developer.ev2z.data.local.remote.LiveLocationResponse
import com.developer.ev2z.request.LoginRequest
import com.developer.ev2z.response.LoginResponse
import com.developer.ev2z.response.MyDevicesResponse
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.JsonObject
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.Parameters
import com.developer.ev2z.response.TotalMilagesResponse
import com.developer.ev2z.response.MobilizeResponse


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


    suspend fun getDeviceLiveLocation(
        token: String,
        deviceId: String
    ): LiveLocationResponse {
        println("🌐 API: Fetching live location for deviceId=$deviceId")

        try {
            val response = client.get("${baseUrl}live-location/$deviceId") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
            }

            println("🌐 API: Live location status: ${response.status}")
            val result: LiveLocationResponse = response.body()
            return result

        } catch (e: Exception) {
            println("❌ Live Location API Error: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }


    // ------------------ TOTAL MILAGES ------------------

    suspend fun getTotalMilages(
        token: String,
        terminalId: String
    ): TotalMilagesResponse {
        println("🌐 API: Fetching total milages for terminal_id=$terminalId")

        try {
            val response = client.submitForm(
                url = "${baseUrl}total-milages",
                formParameters = Parameters.build {
                    append("terminal_id", terminalId)
                }
            ) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                    append(HttpHeaders.Accept, ContentType.Application.Json.toString())
                }
            }

            println("🌐 API: Total milages status: ${response.status}")
            val result: TotalMilagesResponse = response.body()
            println("🌐 API: Total distance: ${result.data?.total_distance_km}, Today: ${result.data?.today_distance_km}")
            return result

        } catch (e: Exception) {
            println("❌ Total Milages API Error: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    // ------------------ MOBILIZE (CUT / RESTORE) ------------------

    suspend fun sendMobilizeCommand(
        token: String,
        terminalId: String,
        command: String   // "cut" or "restore"
    ): MobilizeResponse {

        println("🌐 API: Sending mobilize command=$command for terminal_id=$terminalId")

        try {
            val response = client.submitForm(
                url = "${baseUrl}mobilize",
                formParameters = Parameters.build {
                    append("terminal_id", terminalId)
                    append("command", command)
                }
            ) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                    append(HttpHeaders.Accept, ContentType.Application.Json.toString())
                }
            }

            println("🌐 API: Mobilize status: ${response.status}")

            val result: MobilizeResponse = response.body()

            println("🌐 API: Mobilize message: ${result.message}")

            return result

        } catch (e: Exception) {
            println("❌ Mobilize API Error: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }


}