package com.developer.ev2z.network

import com.developer.ev2z.data.local.model.DeviceResponse
import com.developer.ev2z.data.local.remote.LiveLocationResponse
import com.developer.ev2z.request.DeviceAnalyticsRequest
import com.developer.ev2z.request.LoginRequest
import com.developer.ev2z.response.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.JsonObject

class ApiService {

    private val client = KtorClient.client
    private val baseUrl = KtorClient.BASE_URL

    // ---------------- LOGIN ----------------

    suspend fun login(request: LoginRequest): LoginResponse {
        println("🌐 API: Sending login request")

        return try {
            val response = client.post("${baseUrl}login") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            println("✔️ API Login status: ${response.status}")
            response.body()

        } catch (e: Exception) {
            println("❌ API Login Error: ${e.message}")
            throw e
        }
    }

    // ---------------- LATEST LOCATIONS ----------------

    suspend fun getDevicesLatestLocations(token: String): DeviceResponse {
        println("🌐 API: Fetching latest locations")

        return try {
            val response = client.get("${baseUrl}user/devices/latest-locations") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            println("✔️ Latest Locations Status: ${response.status}")
            response.body()

        } catch (e: Exception) {
            println("❌ Latest Location Error: ${e.message}")
            throw e
        }
    }

    // ---------------- ADD DEVICE ----------------

    suspend fun addDevice(token: String, body: JsonObject): JsonObject {
        println("🌐 API: Adding device")

        return try {
            val response = client.post("${baseUrl}add-device") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(body)
            }

            println("✔️ Add Device Status: ${response.status}")
            response.body()

        } catch (e: Exception) {
            println("❌ Add Device Error: ${e.message}")
            throw e
        }
    }

    // ---------------- UPDATE DEVICE ----------------

    suspend fun updateDevice(token: String, body: JsonObject): JsonObject {
        println("🌐 API: Updating device")

        return try {
            val response = client.post("${baseUrl}update-device") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(body)
            }

            println("✔️ Update Device Status: ${response.status}")
            response.body()

        } catch (e: Exception) {
            println("❌ Update Device Error: ${e.message}")
            throw e
        }
    }

    // ---------------- MY DEVICES ----------------

    suspend fun getMyDevices(token: String): MyDevicesResponse {
        println("🌐 API: Fetching my devices")

        return try {
            val response = client.get("${baseUrl}devices/my-devices") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            println("✔️ My Devices Status: ${response.status}")
            response.body()

        } catch (e: Exception) {
            println("❌ My Devices Error: ${e.message}")
            throw e
        }
    }

    // ---------------- LIVE LOCATION ----------------

    suspend fun getDeviceLiveLocation(
        token: String,
        deviceId: String
    ): LiveLocationResponse {

        return try {
            val response = client.get("${baseUrl}live-location/$deviceId") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            println("✔️ Live Location Status: ${response.status}")
            response.body()

        } catch (e: Exception) {
            println("❌ Live Location Error: ${e.message}")
            throw e
        }
    }

    // ---------------- TOTAL MILAGES ----------------

    suspend fun getTotalMilages(token: String, terminalId: String): TotalMilagesResponse {
        println("🌐 API: Fetching total milages")

        return try {
            val response = client.submitForm(
                url = "${baseUrl}total-milages",
                formParameters = Parameters.build {
                    append("terminal_id", terminalId)
                }
            ) {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }

            println("✔️ Total Milages Status: ${response.status}")
            response.body()

        } catch (e: Exception) {
            println("❌ Total Milages Error: ${e.message}")
            throw e
        }
    }

    // ---------------- MOBILIZE (CUT / RESTORE) ----------------

    suspend fun sendMobilizeCommand(
        token: String,
        terminalId: String,
        command: String
    ): MobilizeResponse {

        println("🌐 API: Mobilize command: $command")

        return try {
            val response = client.submitForm(
                url = "${baseUrl}mobilize",
                formParameters = Parameters.build {
                    append("terminal_id", terminalId)
                    append("command", command)
                }
            ) {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }

            println("✔️ Mobilize Status: ${response.status}")
            response.body()

        } catch (e: Exception) {
            println("❌ Mobilize Error: ${e.message}")
            throw e
        }
    }

    // ---------------- ANALYTICS ----------------

    suspend fun getDeviceAnalytics(
        token: String,
        deviceId: String,
        range: String
    ): DeviceAnalyticsResponse {

        println("🌐 API: Device Analytics")

        return try {
            val response = client.post("${baseUrl}device/analytics") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(DeviceAnalyticsRequest(device_id = deviceId, range = range))
            }

            println("✔️ Analytics Status: ${response.status}")
            response.body()

        } catch (e: Exception) {
            println("❌ Analytics Error: ${e.message}")
            throw e
        }
    }

    // ---------------- PROFILE PICTURE ----------------

    suspend fun getProfilePicture(token: String): JsonObject {
        return client.get("${baseUrl}profile-picture") {
            header(HttpHeaders.Authorization, token)
        }.body()
    }

    // ---------------- UPDATE PROFILE (MULTIPART) ----------------

    suspend fun updateProfile(
        token: String,
        name: String,
        email: String,
        photo: ByteArray? = null
    ): JsonObject {

        println("🌐 API: Updating profile")

        return try {
            val response = client.submitFormWithBinaryData(
                url = "${baseUrl}update-profile",
                formData = formData {
                    append("name", name)

                    if (email.isNotEmpty()) {
                        append("email", email)
                    }

                    photo?.let {
                        append("photo", it, Headers.build {
                            append(HttpHeaders.ContentType, "image/jpeg")
                            append(HttpHeaders.ContentDisposition, "filename=profile.jpg")
                        })
                    }
                }
            ) {
                header(HttpHeaders.Authorization, token)
            }

            println("✔️ Profile Update Status: ${response.status}")
            response.body()

        } catch (e: Exception) {
            println("❌ Profile Update Error: ${e.message}")
            throw e
        }
    }

    // ---------------- DELETE DEVICE ----------------

    suspend fun deleteDevice(deviceId: String, token: String): JsonObject {

        println("🌐 API: Deleting device: $deviceId")

        return try {
            val response = client.delete("${baseUrl}delete-device/$deviceId") {
                header(HttpHeaders.Authorization, token)
            }

            println("✔️ Delete Status: ${response.status}")
            response.body()

        } catch (e: Exception) {
            println("❌ Delete Error: ${e.message}")
            throw e
        }
    }

    // ---------------- MOBILIZE (2nd API) ----------------

    suspend fun mobilizeDevice(
        token: String,
        terminalId: String,
        command: String
    ): JsonObject {

        println("🌐 API: Mobilizing second method")

        return try {
            val response = client.submitForm(
                url = "${baseUrl}mobilize",
                formParameters = Parameters.build {
                    append("terminal_id", terminalId)
                    append("command", command)
                }
            ) {
                header(HttpHeaders.Authorization, token)
                header(HttpHeaders.Accept, "application/json")
            }

            println("✔️ Mobilize Device Status: ${response.status}")
            response.body()

        } catch (e: Exception) {
            println("❌ Mobilize Device Error: ${e.message}")
            throw e
        }
    }


}
