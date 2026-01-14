package com.developer.ev2z.network

import com.developer.ev2z.data.local.model.DeviceResponse
import com.developer.ev2z.data.local.remote.LiveLocationResponse
import com.developer.ev2z.request.CartAddRequest
import com.developer.ev2z.request.CommentRequest
import com.developer.ev2z.request.CreateOrderRequest
import com.developer.ev2z.request.DeviceAnalyticsRequest
import com.developer.ev2z.response.*
import com.developer.ev2z.util.AppTokenProvider
import io.ktor.client.call.*
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ApiService {

    private val client = KtorClient.client
    private val baseUrl = KtorClient.BASE_URL

    // ===============================================
    // AUTHENTICATION APIs
    // ===============================================

    // ---------------- LOGIN - FIXED ----------------
    suspend fun login(phone: String, password: String): JsonObject {
        println("🌐 API: Sending login request for phone=$phone")

        return try {
            val response = client.submitForm(
                url = "${baseUrl}login",
                formParameters = Parameters.build {
                    append("phone", phone)
                    append("password", password)
                }
            ) {
                header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }

            println("✔️ API Login status: ${response.status}")
            response.body()

        } catch (e: ResponseException) {
            val status = e.response.status
            val text = try {
                e.response.bodyAsText()
            } catch (_: Exception) {
                """{"message":"Network error"}"""
            }

            println("❌ API Login Error: HTTP $status")
            println("   response body: $text")

            // ✅ Parse and return error JSON (don't throw here)
            val json = Json { ignoreUnknownKeys = true }
            val errorObj = json.parseToJsonElement(text).jsonObject

            // ✅ Return error JSON as-is
            return errorObj

        } catch (e: Exception) {
            println("❌ API Login Error: ${e.message}")
            throw e
        }
    }

    // ---------------- REGISTER USER ----------------
    suspend fun registerUser(
        name: String,
        phone: String,
        email: String
    ): JsonObject {
        println("🌐 API: Registering user (sending OTP) phone=$phone")

        return try {
            val response = client.submitForm(
                url = "${baseUrl}register",
                formParameters = Parameters.build {
                    append("name", name)
                    append("phone", phone)
                    append("email", email)
                }
            ) {
                header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }

            println("✔️ Register User Status: ${response.status}")
            response.body()

        } catch (e: ResponseException) {
            val status = e.response.status
            val text = try {
                e.response.bodyAsText()
            } catch (_: Exception) {
                """{"message":"Network error"}"""
            }

            println("❌ Register User Error: HTTP $status")
            println("   response body: $text")

            // ✅ Parse and return error JSON (don't throw here)
            val json = Json { ignoreUnknownKeys = true }
            val errorObj = json.parseToJsonElement(text).jsonObject

            // ✅ Return error JSON as-is
            return errorObj

        } catch (e: Exception) {
            println("❌ Register User Error: ${e.message}")
            throw e
        }
    }

    // ---------------- VERIFY OTP (after signup) ----------------
    suspend fun verifyOtp(phone: String, otp: String): JsonObject {
        println("🌐 API: Verifying OTP for phone=$phone")

        return try {
            val response = client.post("${baseUrl}verify-otp") {
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "phone" to phone,
                    "otp" to otp
                ))
            }

            println("✔️ Verify OTP Status: ${response.status}")
            response.body()

        } catch (e: ResponseException) {
            val status = e.response.status
            val text = try { e.response.bodyAsText() } catch (_: Exception) { "unable-to-read-body" }
            println("❌ Verify OTP Error: HTTP $status")
            println("   response body: $text")
            throw e
        } catch (e: Exception) {
            println("❌ Verify OTP Error: ${e.message}")
            throw e
        }
    }

    // ---------------- RESEND OTP (after signup) ----------------
    suspend fun resendOtp(phone: String): JsonObject {
        println("🌐 API: Resending OTP for phone=$phone")

        return try {
            val response = client.post("${baseUrl}resend-otp") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("phone" to phone))
            }

            println("✔️ Resend OTP Status: ${response.status}")
            response.body()

        } catch (e: ResponseException) {
            val status = e.response.status
            val text = try { e.response.bodyAsText() } catch (_: Exception) { "unable-to-read-body" }
            println("❌ Resend OTP Error: HTTP $status")
            println("   response body: $text")
            throw e
        } catch (e: Exception) {
            println("❌ Resend OTP Error: ${e.message}")
            throw e
        }
    }

    // ---------------- SAVE PASSWORD (for registration after OTP) ----------------
    suspend fun savePassword(
        phone: String,
        password: String
    ): JsonObject {
        println("🌐 API: Saving password for phone=$phone")

        return try {
            val response = client.post("${baseUrl}save-password") {
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "phone" to phone,
                    "password" to password
                ))
            }

            println("✔️ Save Password Status: ${response.status}")
            response.body()

        } catch (e: ResponseException) {
            val status = e.response.status
            val text = try { e.response.bodyAsText() } catch (_: Exception) { "unable-to-read-body" }
            println("❌ Save Password Error: HTTP $status")
            println("   response body: $text")
            throw e
        } catch (e: Exception) {
            println("❌ Save Password Error: ${e.message}")
            throw e
        }
    }

    // ---------------- FORGOT PASSWORD (send OTP) ----------------
    suspend fun forgotPassword(phone: String): JsonObject {
        println("🌐 API: Forgot password request for phone=$phone")

        return try {
            val response = client.submitForm(
                url = "${baseUrl}forgot-password",
                formParameters = Parameters.build {
                    append("phone", phone)
                }
            ) {
                header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }

            println("✔️ Forgot Password Status: ${response.status}")
            response.body()

        } catch (e: ResponseException) {
            val status = e.response.status
            val text = try {
                e.response.bodyAsText()
            } catch (_: Exception) {
                """{"message":"Network error"}"""
            }

            println("❌ Forgot Password Error: HTTP $status")
            println("   response body: $text")

            // ✅ Parse and return error JSON (don't throw here)
            val json = Json { ignoreUnknownKeys = true }
            val errorObj = json.parseToJsonElement(text).jsonObject

            // ✅ Return error JSON as-is
            return errorObj

        } catch (e: Exception) {
            println("❌ Forgot Password Error: ${e.message}")
            throw e
        }
    }

    // ---------------- VERIFY FORGOT PASSWORD OTP ----------------
    suspend fun verifyForgotOtp(phone: String, otp: String): JsonObject {
        println("🌐 API: Verifying forgot password OTP for phone=$phone")

        return try {
            val response = client.post("${baseUrl}forgot-password-verify-otp") {
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "phone" to phone,
                    "otp" to otp
                ))
            }

            println("✔️ Verify Forgot OTP Status: ${response.status}")
            response.body()

        } catch (e: ResponseException) {
            val status = e.response.status
            val text = try { e.response.bodyAsText() } catch (_: Exception) { "unable-to-read-body" }
            println("❌ Verify Forgot OTP Error: HTTP $status")
            println("   response body: $text")
            throw e
        } catch (e: Exception) {
            println("❌ Verify Forgot OTP Error: ${e.message}")
            throw e
        }
    }

    // ---------------- RESEND FORGOT PASSWORD OTP ----------------
    suspend fun resendForgotOtp(phone: String): JsonObject {
        println("🌐 API: Resending forgot password OTP for phone=$phone")

        return try {
            val response = client.post("${baseUrl}resend-otp") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("phone" to phone))
            }

            println("✔️ Resend Forgot OTP Status: ${response.status}")
            response.body()

        } catch (e: ResponseException) {
            val status = e.response.status
            val text = try { e.response.bodyAsText() } catch (_: Exception) { "unable-to-read-body" }
            println("❌ Resend Forgot OTP Error: HTTP $status")
            println("   response body: $text")
            throw e
        } catch (e: Exception) {
            println("❌ Resend Forgot OTP Error: ${e.message}")
            throw e
        }
    }

    // ---------------- RESET PASSWORD ----------------
    suspend fun resetPassword(
        phone: String,
        password: String,
        confirmPassword: String
    ): JsonObject {
        println("🌐 API: Resetting password for phone=$phone")

        return try {
            val response = client.post("${baseUrl}reset-password") {
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "phone" to phone,
                    "password" to password,
                    "confirm_password" to confirmPassword
                ))
            }

            println("✔️ Reset Password Status: ${response.status}")
            response.body()

        } catch (e: ResponseException) {
            val status = e.response.status
            val text = try { e.response.bodyAsText() } catch (_: Exception) { "unable-to-read-body" }
            println("❌ Reset Password Error: HTTP $status")
            println("   response body: $text")
            throw e
        } catch (e: Exception) {
            println("❌ Reset Password Error: ${e.message}")
            throw e
        }
    }

    // ===============================================
    // DEVICE APIs (existing code)
    // ===============================================

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

    // ---------------- CHANGE PASSWORD ----------------
    suspend fun changePassword(
        token: String,
        currentPassword: String,
        newPassword: String,
        confirmNewPassword: String
    ): JsonObject {
        println("🌐 API: Change Password Request")

        return try {
            val response = client.submitForm(
                url = "${baseUrl}change-password",
                formParameters = Parameters.build {
                    append("current_password", currentPassword)
                    append("new_password", newPassword)
                    append("confirm_new_password", confirmNewPassword)
                }
            ) {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }

            println("✔️ Change Password Status: ${response.status}")
            val responseBody: JsonObject = response.body()

            println("📝 Response: $responseBody")
            responseBody

        } catch (e: ResponseException) {
            val status = e.response.status
            val text = try {
                e.response.bodyAsText()
            } catch (_: Exception) {
                """{"error":"Network error"}"""
            }

            println("❌ Change Password Error: HTTP $status")
            println("   response body: $text")

            // ✅ Parse and return error JSON (don't throw here)
            val json = Json { ignoreUnknownKeys = true }
            val errorObj = json.parseToJsonElement(text).jsonObject

            return errorObj

        } catch (e: Exception) {
            println("❌ Change Password Error: ${e.message}")
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

    // ---------------- GEO FENCING ----------------


    // ---------------- HISTORY PLAYBACK ----------------
    suspend fun getRouteHistory(
        token: String,
        startDate: String,
        endDate: String,
        terminalId: String
    ): RouteHistoryResponse {
        println("🌐 API: Fetching route history")

        return try {
            val response = client.post("${baseUrl}route-history") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "start_date" to startDate,
                    "end_date" to endDate,
                    "terminal_id" to terminalId
                ))
            }

            println("✔️ Route History Status: ${response.status}")
            response.body()

        } catch (e: Exception) {
            println("❌ Route History Error: ${e.message}")
            throw e
        }
    }

    // ---------------- DELETE ACCOUNT ----------------
    suspend fun deleteAccount(
        token: String,
        body: JsonObject
    ): JsonObject {
        println("🌐 API: Delete Account Request")

        return try {
            val response = client.delete("${baseUrl}user/delete") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(body)
            }

            println("✔️ Delete Account Status: ${response.status}")
            val responseBody: JsonObject = response.body()

            println("📝 Response: $responseBody")
            responseBody

        } catch (e: ResponseException) {
            val status = e.response.status
            val text = try {
                e.response.bodyAsText()
            } catch (_: Exception) {
                """{"success":false,"message":"Network error"}"""
            }

            println("❌ Delete Account Error: HTTP $status")
            println("   response body: $text")

            // ✅ Parse and return error JSON
            val json = Json { ignoreUnknownKeys = true }
            val errorObj = json.parseToJsonElement(text).jsonObject

            return errorObj

        } catch (e: Exception) {
            println("❌ Delete Account Error: ${e.message}")
            throw e
        }
    }


    // ---------------- REFER & EARN ----------------
    suspend fun getReferralCode(token: String): ReferralCodeResponse {
        println("🌐 API: Fetching referral code")

        return try {
            val response = client.get("${baseUrl}invite-friends") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            println("✔️ Referral Code Status: ${response.status}")
            response.body()

        } catch (e: ResponseException) {
            val status = e.response.status
            val text = try {
                e.response.bodyAsText()
            } catch (_: Exception) {
                """{"success":false,"message":"Network error"}"""
            }

            println("❌ Referral Code Error: HTTP $status")
            println("   response body: $text")

            val json = Json { ignoreUnknownKeys = true }
            json.decodeFromString<ReferralCodeResponse>(text)

        } catch (e: Exception) {
            println("❌ Referral Code Error: ${e.message}")
            ReferralCodeResponse(
                success = false,
                message = e.message ?: "Unknown error"
            )
        }
    }

    // ---------------- DEVICE STATUS ----------------
    suspend fun getDeviceStatus(
        token: String,
        deviceId: String
    ): JsonObject {
        println("🌐 API: Fetching device status for: $deviceId")

        return try {
            val response = client.get("${baseUrl}device-status/$deviceId") {
                header(HttpHeaders.Authorization, token)
            }

            println("✔️ Device Status: ${response.status}")
            response.body()

        } catch (e: Exception) {
            println("❌ Device Status Error: ${e.message}")
            throw e
        }
    }


    suspend fun getDevicesStatus(token: String, deviceId: String): DeviceStatusResponse {
        val response = client.get("${baseUrl}device/status-details/$deviceId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        // Always parse body as text first
        val text = response.bodyAsText()
        println("DEBUG: Response body: $text")

        // Now deserialize manually
        return kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true  // ✅ ignore extra fields
        }.decodeFromString(DeviceStatusResponse.serializer(), text)
    }


    // ===============================================
    // SHOP / PRODUCT APIs (existing code)
    // ===============================================

    // ---------------- GET PRODUCTS ----------------
    suspend fun getProducts(token: String): List<ProductDto> {
        println("🌐 API: getProducts()")
        return try {
            val res: List<ProductDto> = client.get("${baseUrl}products") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()
            println("✔️ getProducts: received ${res.size} items")
            res
        } catch (e: ResponseException) {
            val status = e.response.status
            val text = try { e.response.bodyAsText() } catch (_: Exception) { "unable-to-read-body" }
            println("❌ getProducts - HTTP error: $status")
            println("   response body: $text")
            emptyList()
        } catch (e: Exception) {
            println("❌ getProducts - error: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    // ---------------- GET WISHLIST ----------------
    suspend fun getWishlist(token: String): WishlistResponse {
        println("🌐 API: getWishlist()")
        return try {
            val res: WishlistResponse = client.get("${baseUrl}wishlist") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()
            println("✔️ getWishlist: success=${res.success} items=${res.wishlist.size}")
            res
        } catch (e: ResponseException) {
            val status = e.response.status
            val text = try { e.response.bodyAsText() } catch (_: Exception) { "unable-to-read-body" }
            println("❌ getWishlist - HTTP error: $status")
            println("   response body: $text")
            WishlistResponse(success = false, wishlist = emptyList())
        } catch (e: Exception) {
            println("❌ getWishlist - error: ${e.message}")
            e.printStackTrace()
            WishlistResponse(success = false, wishlist = emptyList())
        }
    }

    // ---------------- ADD WISHLIST ----------------
    suspend fun addToWishlist(token: String, productId: Int): WishlistActionResponse {
        println("🌐 API: addToWishlist(productId=$productId)")
        return try {
            val res: WishlistActionResponse = client.post("${baseUrl}wishlist/add") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(mapOf("product_id" to productId))
            }.body()
            println("✔️ addToWishlist: success=${res.success} message='${res.message}'")
            res
        } catch (e: ResponseException) {
            val status = e.response.status
            val text = try { e.response.bodyAsText() } catch (_: Exception) { "unable-to-read-body" }
            println("❌ addToWishlist - HTTP error: $status")
            println("   response body: $text")
            WishlistActionResponse(success = false, message = "HTTP ${status.value}")
        } catch (e: Exception) {
            println("❌ addToWishlist - error: ${e.message}")
            e.printStackTrace()
            WishlistActionResponse(success = false, message = e.message ?: "Unknown error")
        }
    }

    // ---------------- REMOVE WISHLIST ----------------
    suspend fun removeFromWishlist(token: String, productId: Int): WishlistActionResponse {
        println("🌐 API: removeFromWishlist(productId=$productId)")
        return try {
            val res: WishlistActionResponse = client.delete("${baseUrl}wishlist/remove/$productId") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()
            println("✔️ removeFromWishlist: success=${res.success} message='${res.message}'")
            res
        } catch (e: ResponseException) {
            val status = e.response.status
            val text = try { e.response.bodyAsText() } catch (_: Exception) { "unable-to-read-body" }
            println("❌ removeFromWishlist - HTTP error: $status")
            println("   response body: $text")
            WishlistActionResponse(success = false, message = "HTTP ${status.value}")
        } catch (e: Exception) {
            println("❌ removeFromWishlist - error: ${e.message}")
            e.printStackTrace()
            WishlistActionResponse(success = false, message = e.message ?: "Unknown error")
        }
    }

    // ---------------- GET CART ----------------
    suspend fun getCart(token: String): CartResponse {
        println("🌐 API: getCart()")
        return try {
            val resp: CartResponse = client.get("${baseUrl}cart") {
                header(io.ktor.http.HttpHeaders.Authorization, "Bearer $token")
            }.body()
            println("✔️ getCart: items=${resp.cart.size} totalPrice=${resp.totalPrice}")
            resp
        } catch (e: Exception) {
            println("❌ getCart error: ${e.message}")
            e.printStackTrace()
            CartResponse(success = false, cart = emptyList(), totalPrice = "0.00")
        }
    }

    // ---------------- ADD TO CART ----------------
    suspend fun addToCart(
        token: String,
        add: String,
        productId: Int,
        quantity: Int = 1,
        plan_type: String,
        deviceType: String
    ): CartActionResponse {

        val url = "${baseUrl}cart"

        val bodyToSend = CartAddRequest(
            action = add,
            product_id = productId,
            quantity = quantity,
            plan_type = plan_type,
            device_type = deviceType
        )

        println("🌐 POST URL = $url")
        println("📦 REQUEST BODY = ${kotlinx.serialization.json.Json.encodeToString(CartAddRequest.serializer(), bodyToSend)}")

        val response = client.post(url) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(bodyToSend)
        }

        val raw = response.bodyAsText()
        println("📝 RAW RESPONSE = $raw")
        println("📡 RESPONSE STATUS = ${response.status}")

        return try {
            response.body<CartActionResponse>()
        } catch (e: Exception) {
            println("❌ Parsing Error: ${e.message}")
            CartActionResponse(
                success = false,
                action = null,
                message = raw
            )
        }
    }

    // ---------------- UPDATE CART QUANTITY ----------------
    suspend fun updateCartQuantity(
        token: String,
        productId: Int,
        quantity: Int,
        planType: String?,
        deviceType: String?
    ): CartActionResponse {

        val body = CartAddRequest(
            action = "update_qty",
            product_id = productId,
            quantity = quantity,
            plan_type = planType,
            device_type = deviceType
        )

        return client.post("${baseUrl}cart") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()
    }

    // ---------------- GET GPS BILLING ----------------
    suspend fun getGpsBilling(token: String): BillingListResponse {
        val url = "${baseUrl}get-gps-billing"

        return try {
            val response = client.get(url) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            val raw = response.bodyAsText()
            println("🌐 GET $url")
            println("📡 Status: ${response.status}")
            println("📝 Raw Response: $raw")

            response.body()

        } catch (e: ResponseException) {
            val raw = try { e.response.bodyAsText() } catch (_: Exception) { "No Body" }

            println("❌ API HTTP ERROR: ${e.response.status}")
            println("📝 Error Body: $raw")

            BillingListResponse(status = "error", billings = emptyList())
        } catch (e: Exception) {
            println("❌ API ERROR: ${e.message}")
            BillingListResponse(status = "error", billings = emptyList())
        }
    }

    // ---------------- SAVE GPS BILLING ----------------
    suspend fun saveGpsBilling(
        token: String,
        body: JsonObject
    ): JsonObject {

        val url = "${baseUrl}gps-billing"

        return try {
            val response = client.post(url) {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(body)
            }

            val raw = response.bodyAsText()
            println("📤 SAVE BILLING RAW = $raw")

            response.body()

        } catch (e: Exception) {
            println("❌ SaveBilling Error: ${e.message}")
            JsonObject(mapOf("status" to Json.encodeToJsonElement("error")))
        }
    }

    // ---------------- APPLY PROMO ----------------
    suspend fun applyPromo(
        token: String,
        amount: String,
        code: String
    ): PromoResponse {

        val upper = code.trim().uppercase()

        val applyPromoValue = if (upper.startsWith("WELCOME")) upper else ""
        val referralValue   = if (upper.startsWith("REFER")) upper else ""

        val url = "${baseUrl}phonepe/apply-promo"

        val body = mapOf(
            "amount" to amount,
            "referral_code" to referralValue,
            "apply_promo" to applyPromoValue
        )

        println("📤 APPLY PROMO FINAL BODY = $body")

        return try {
            val response = client.post(url) {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(body)
            }

            val raw = response.bodyAsText()
            println("📥 RAW APPLY PROMO RESPONSE = $raw")

            response.body()

        } catch (e: Exception) {
            println("❌ applyPromo ERROR = ${e.message}")
            throw e
        }
    }

    // ---------------- CREATE PHONEPE ORDER ----------------
    suspend fun createPhonePeOrder(token: String, req: CreateOrderRequest): JsonObject {
        val url = "${baseUrl}phonepe/create-orders"

        val response = client.post(url) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(req)
        }

        return response.body()
    }


    // ApiService.kt

    suspend fun removeGpsBilling(
        token: String,
        billingId: Int
    ): JsonObject {

        val url = "${baseUrl}remove-gps-billing"

        return client.delete(url) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(
                mapOf("billing_id" to billingId.toString())
            )
        }.body()
    }


    // ---------------- COMMUNITY POSTS ----------------
    suspend fun getCommunityPosts(): CommunityResponse {
        val token = AppTokenProvider.getToken()

        println("🌐 API: Fetching community posts")

        return try {
            val response = client.get("${baseUrl}posts") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            println("✔️ Posts Status: ${response.status}")
            response.body()

        } catch (e: Exception) {
            println("❌ Community Posts Error: ${e.message}")
            throw e
        }
    }


    suspend fun createCommunityPost(
        title: String,
        body: String,
        images: List<ByteArray>
    ): JsonObject {

        val token = AppTokenProvider.getToken()

        return client.submitFormWithBinaryData(
            url = "${baseUrl}posts",
            formData = formData {
                append("title", title)
                append("body", body)

                images.forEachIndexed { index, bytes ->
                    append(
                        key = "media[]",
                        value = bytes,
                        headers = Headers.build {
                            append(HttpHeaders.ContentType, "image/jpeg")
                            append(HttpHeaders.ContentDisposition, "filename=image_$index.jpg")
                        }
                    )
                }
            }
        ) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body()
    }


    // ---------------- POST LIKE ----------------
    suspend fun togglePostLike(postId: Int): Boolean {

        val token = AppTokenProvider.getToken()
        val url = "${baseUrl}posts/$postId/like"

        println("🌐 API: Toggle like for postId=$postId")

        return try {
            val response = client.post(url) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            val raw = response.bodyAsText()
            println("📥 LIKE RAW RESPONSE = $raw")

            // response: { "liked": true/false }
            val json = Json.parseToJsonElement(raw).jsonObject
            json["liked"]?.jsonPrimitive?.boolean ?: false

        } catch (e: Exception) {
            println("❌ Like API Error: ${e.message}")
            throw e
        }
    }

    // GET COMMENTS
    suspend fun getPostComments(postId: Int): CommentResponse {
        val token = AppTokenProvider.getToken()

        return client.get("${baseUrl}posts/$postId/comments") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body()
    }

    // POST COMMENT / REPLY
    suspend fun postComment(
        postId: Int,
        comment: String,
        parentId: Int? = null
    ): JsonObject {
        val token = AppTokenProvider.getToken()

        return client.post("${baseUrl}posts/$postId/comments") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(
                CommentRequest(
                    comment = comment,
                    parent_id = parentId
                )
            )
        }.body()
    }

    // LIKE COMMENT
    suspend fun likeComment(commentId: Int): JsonObject {
        val token = AppTokenProvider.getToken()

        return client.post("${baseUrl}comments/$commentId/like") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body()
    }


    suspend fun getGeoFences(): GeoFenceResponse {
        val token = AppTokenProvider.getToken()

        return client.get("${baseUrl}get-geofences") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
        }.body()
    }


    // ---------------- SAVE GEOFENCE ----------------
    suspend fun saveGeoFence(
        token: String,
        deviceId: String,
        name: String,
        latitude: Double,
        longitude: Double,
        radius: Int
    ): JsonObject {

        val url = "${baseUrl}geofences"

        println("🌐 API: Saving GeoFence")

        println("🧪 SAVE GEOFENCE PAYLOAD")
        println("➡️ device_id=$deviceId")
        println("➡️ name=$name")
        println("➡️ lat=$latitude")
        println("➡️ lng=$longitude")
        println("➡️ radius=$radius")
        println("➡️ token=$token")

        return client.submitForm(
            url = url,
            formParameters = Parameters.build {
                append("device_id", deviceId)
                append("name", name)
                append("latitude", latitude.toString())
                append("longitude", longitude.toString())
                append("radius", radius.toString())
            }
        ) {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
        }.body()



    }



    suspend fun deleteGeoFence(
        token: String,
        geoFenceId: Int,
        deviceId: String,
        name: String,
        latitude: String,
        longitude: String,
        radius: String
    ): JsonObject {

        return client.delete("${baseUrl}geofences/$geoFenceId") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())

            setBody(
                FormDataContent(
                    Parameters.build {
                        append("device_id", deviceId)
                        append("name", name)
                        append("latitude", latitude.toString())
                        append("longitude", longitude.toString())
                        append("radius", radius.toString())
                    }
                )
            )
        }.body()
    }


    // ApiService.kt

    suspend fun updateGeoFence(
        geoFenceId: Int,
        token: String,
        deviceId: String,
        name: String,
        latitude: String,
        longitude: String,
        radius: Int
    ): JsonObject {

        return client.submitForm(
            url = "${baseUrl}geofences-update/$geoFenceId",
            formParameters = Parameters.build {
                append("device_id", deviceId)
                append("name", name)
                append("latitude", latitude.toString())
                append("longitude", longitude.toString())
                append("radius", radius.toString())
            }
        ) {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
        }.body()
    }


    // ---------------- STORIES ----------------
    suspend fun getStories(): StoryResponse {
        val token = AppTokenProvider.getToken()

        println("🌐 API: Fetching stories")

        return client.get("${baseUrl}stories") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body()
    }


    // ---------------- ADD STORY ----------------
    suspend fun addStory(
        body: String,
        media: List<ByteArray>
    ): JsonObject {

        val token = AppTokenProvider.getToken()
        val url = "${baseUrl}stories"

        println("🌐 API: Add Story")

        return client.submitFormWithBinaryData(
            url = url,
            formData = formData {

                // ✅ text body
                append("body", body)

                // ✅ images (media[])
                media.forEachIndexed { index, bytes ->
                    append(
                        key = "media[]",
                        value = bytes,
                        headers = Headers.build {
                            append(HttpHeaders.ContentType, "image/png")
                            append(
                                HttpHeaders.ContentDisposition,
                                "filename=story_$index.png"
                            )
                        }
                    )
                }
            }
        ) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body()
    }


    // ---------------- STORY VIEW ----------------
    suspend fun viewStory(storyId: Int): JsonObject {
        val token = AppTokenProvider.getToken()

        return client.post("${baseUrl}stories/$storyId/view") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body()
    }

    // ---------------- STORY VIEWERS ----------------
    suspend fun getStoryViewers(storyId: Int): StoryViewersResponse {
        val token = AppTokenProvider.getToken()

        return client.get("${baseUrl}stories/$storyId/viewers") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body()
    }



    suspend fun registerFcmToken(
        authHeader: String,
        fcmToken: String
    ) {
        val response = client.post("${baseUrl}fcm/register") {
            headers {
                append("Authorization", authHeader)
                append("Content-Type", "application/json")
            }
            setBody(
                mapOf(
                    "fcm_token" to fcmToken
                )
            )
        }

        println("📡 FCM API STATUS: ${response.status}")
        println("📡 FCM API BODY: ${response.bodyAsText()}")
    }


}

