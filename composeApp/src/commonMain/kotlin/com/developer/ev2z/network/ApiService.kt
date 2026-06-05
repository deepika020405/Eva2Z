package com.developer.ev2z.network

import com.developer.ev2z.data.local.model.DeviceResponse
import com.developer.ev2z.data.local.remote.LiveLocationResponse
import com.developer.ev2z.request.CandidateDto
import com.developer.ev2z.request.CandidateRequest
import com.developer.ev2z.request.CartAddRequest
import com.developer.ev2z.request.CommentRequest
import com.developer.ev2z.request.CreateOrderRequest
import com.developer.ev2z.request.DeviceAnalyticsRequest
import com.developer.ev2z.request.PackageDto
import com.developer.ev2z.request.PhonePeCallbackRequest
import com.developer.ev2z.request.RenewPlanRequest
import com.developer.ev2z.request.SupportRequest
import com.developer.ev2z.response.SupportResponse
import com.developer.ev2z.response.*
import com.developer.ev2z.ui.screens.ChangePlan.UserDevicesResponse
import com.developer.ev2z.ui.screens.CriminalVerification.CandidateResult
import com.developer.ev2z.ui.screens.CriminalVerification.CriminalForm
import com.developer.ev2z.ui.screens.SOSParkingTag.ChatMessageDto
import com.developer.ev2z.ui.screens.SOSParkingTag.ChatMessagesResponse
import com.developer.ev2z.ui.screens.SOSParkingTag.InitiateCallResponse
import com.developer.ev2z.ui.screens.SOSParkingTag.MyChatsResponse
import com.developer.ev2z.ui.screens.SOSParkingTag.OwnerReplyRequest
import com.developer.ev2z.ui.screens.SOSParkingTag.SosAlertDetailResponse
import com.developer.ev2z.ui.screens.SOSParkingTag.SosAlertListResponse
import com.developer.ev2z.ui.screens.SOSParkingTag.SosReplyResponse
import com.developer.ev2z.ui.screens.VehicleParkingTag.ParkingData
import com.developer.ev2z.ui.screens.VehicleParkingTag.ParkingRequest
import com.developer.ev2z.ui.screens.VehicleParkingTag.ParkingResponse
import com.developer.ev2z.ui.screens.VehicleParkingTag.UserParkingListResponse
import com.developer.ev2z.util.AppTokenProvider
import io.ktor.client.call.*
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

class UnauthorizedException : Exception("Session expired. Please login again.")

class ApiService {

    private val client = KtorClient.client
    private val baseUrl = KtorClient.BASE_URL
    private val gpsBaseUrl = "${KtorClient.SOCKET_SERVER_URL}/api/"

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

    // ---------------- VERIFY PHONE OTP (after signup) ----------------
    suspend fun verifyOtp(phone: String, otp: String): JsonObject {
        println("🌐 API: Verifying Phone OTP for phone=$phone")

        return try {
            val response = client.post("${baseUrl}verify-otp") {
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "phone" to phone,
                    "otp" to otp
                ))
            }

            println("✔️ Verify Phone OTP Status: ${response.status}")
            response.body()

        } catch (e: ResponseException) {
            val status = e.response.status
            val text = try { e.response.bodyAsText() } catch (_: Exception) { "unable-to-read-body" }
            println("❌ Verify Phone OTP Error: HTTP $status — $text")
            throw e
        } catch (e: Exception) {
            println("❌ Verify Phone OTP Error: ${e.message}")
            throw e
        }
    }

    // ---------------- VERIFY EMAIL OTP (after signup) ----------------
    suspend fun verifyEmailOtp(phone: String, email: String, otp: String): JsonObject {
        println("🌐 API: Verifying Email OTP for phone=$phone email=$email otp=$otp")

        return try {
            val response = client.post("${baseUrl}verify-otp") {
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "phone" to phone,
                    "email_otp" to otp
                ))
            }

            val bodyText = response.bodyAsText()
            println("✔️ Verify Email OTP Status: ${response.status} — Body: $bodyText")
            Json.parseToJsonElement(bodyText).jsonObject

        } catch (e: ResponseException) {
            val status = e.response.status
            val text = try { e.response.bodyAsText() } catch (_: Exception) { "unable-to-read-body" }
            println("❌ Verify Email OTP Error: HTTP $status — $text")
            throw e
        } catch (e: Exception) {
            println("❌ Verify Email OTP Error: ${e.message}")
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

    // ---------------- CANCEL FORGOT PASSWORD SESSION ----------------
    suspend fun cancelForgotPassword(phone: String): JsonObject {
        println("🌐 API: Cancelling forgot password session for phone=$phone")
        return try {
            val response = client.submitForm(
                url = "${baseUrl}forgot-password-cancel",
                formParameters = Parameters.build { append("phone", phone) }
            ) {
                header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }
            println("✔️ Cancel Forgot Password Status: ${response.status}")
            response.body()
        } catch (e: Exception) {
            println("❌ Cancel Forgot Password Error: ${e.message}")
            buildJsonObject { put("error", e.message ?: "cancel failed") }
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

            if (response.status == HttpStatusCode.Unauthorized) {
                throw UnauthorizedException()
            }

            response.body()

        } catch (e: UnauthorizedException) {
            throw e
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

            val raw = response.bodyAsText()
            println("✔️ My Devices Status: ${response.status}")
            println("📝 MY DEVICES RAW = $raw")

            Json { ignoreUnknownKeys = true }
                .decodeFromString(MyDevicesResponse.serializer(), raw)

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

            val raw = response.bodyAsText()
            println("✔️ Live Location Status: ${response.status} deviceId=$deviceId")
            println("📝 LIVE LOCATION RAW = $raw")

            if (response.status == HttpStatusCode.Unauthorized) {
                throw UnauthorizedException()
            }

            Json { ignoreUnknownKeys = true }
                .decodeFromString(LiveLocationResponse.serializer(), raw)

        } catch (e: UnauthorizedException) {
            throw e
        } catch (e: Exception) {
            println("❌ Live Location Error: ${e.message}")
            throw e
        }
    }


    //create candidate id

    suspend fun createCandidateWithPackage(form: CriminalForm): CandidateResult {
        val token = AppTokenProvider.getToken()
        if (token.isBlank()) {
            return CandidateResult(error = "Session expired. Please login again.")
        }
        val url = "${baseUrl}candidate"

        // ✅ DTO instead of manual JSON
        val body = CandidateRequest(
            candidate = CandidateDto(
                name = form.name,
                email = form.email,
                phone = form.phone,
                employee_id = form.employeeId,
                invite = false,
                is_consent_undertaking_letter = form.consent,
                category_id = 200
            ),
            // ✅ Naya — CORRECT IDs
            // ✅ REPLACE WITH — yeh karo
            `package` = PackageDto(
                package_id = 7642,
                subtype_id = 8860
            )
        )

        println("📦 Request Body → $body")

        return try {
            val response: JsonObject = client.post(url) {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body()

            println("📥 RAW RESPONSE → $response")

            val candidateId =
                response["data"]?.jsonObject
                    ?.get("data")?.jsonObject
                    ?.get("candidate_id")?.jsonPrimitive?.contentOrNull

            println("✅ Parsed candidateId = $candidateId")

            CandidateResult(candidateId = candidateId)

        } catch (e: ClientRequestException) {
            if (e.response.status.value == 409) {
                println("⚠️ Duplicate Candidate (409)")
                CandidateResult(isDuplicate = true)
            } else {
                println("❌ API Error: ${e.message}")
                CandidateResult(error = "Something went wrong")
            }
        }
    }




    // ===============================================
// CRIMINAL VERIFICATION → SUBMIT BGV
// ===============================================
    suspend fun submitBgvVerification(
        candidateId: Int,
        form: CriminalForm
    ): JsonObject {

        val bgvUrl = "${gpsBaseUrl}submit-bgv"
        val appToken = AppTokenProvider.getToken()

        val body = buildJsonObject {
            put("integrationName", "SUBMIT_BGV")
            put("candidate_id", candidateId)

            putJsonObject("background_check_details") {
                putJsonObject("candidate_details") {
                    put("full_name", form.name)
                    put("email_address", form.email)
                    put("mobile_number", form.phone)
                    put("employee_id", form.employeeId)
                    put("uan_number", "")
                    put("uan_not_available", true)
                }

                put("address_details", buildJsonArray {
                    add(buildJsonObject {
                        put("house_no", form.houseNo)
                        put("colony", form.colony)
                        put("city", form.city)
                        put("state", form.state)
                        put("pincode", form.pincode)
                        put("country", "India")
                        put("staying_period", form.stayingPeriod)
                        put("is_current_permanent_same", true)
                        put("isProofDocOptional", true)
                        put("rented", false)
                        put("landmark", form.landmark)
                        put("doc_type", "others")
                    })
                })
            }
        }

        println("📦 BGV BODY → $body")

        val response: JsonObject = client.post(bgvUrl) {
            header(HttpHeaders.Authorization, "Bearer $appToken")  // ✅ FIXED
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

        // ✅ PRINT RESPONSE
        println("📥 BGV RESPONSE → $response")

        return response
    }

    //Candidate verfication callback
    suspend fun getCandidateStatus(candidateId: Int): JsonObject {
        val token = AppTokenProvider.getToken()
        val url = "${baseUrl}candidate/$candidateId"

        println("🌐 GET Candidate Status → $url")

        val response = client.get(url) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        val raw = response.bodyAsText()

        println("📡 Status Code = ${response.status}")
        println("📥 RAW RESPONSE = $raw")

        return kotlinx.serialization.json.Json
            .parseToJsonElement(raw)
            .jsonObject
    }


    //criminal payment create order
    suspend fun createCriminalOrder(amount: Int): JsonObject {
        val token = AppTokenProvider.getToken()

        return client.post("${baseUrl}crminal-create-order") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(mapOf("amount" to amount))
        }.body()
    }


//criminal payment callback
    suspend fun sendCriminalPaymentCallback(
        merchantOrderId: String,
        phonepeOrderId: String,
        status: String
    ): Boolean {

        val token = AppTokenProvider.getToken()

        val response = client.post("${baseUrl}crminal-callback") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(
                mapOf(
                    "merchantOrderId" to merchantOrderId,
                    "phonepeOrderId" to phonepeOrderId,
                    "status" to status
                )
            )
        }

        return response.status.isSuccess()
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
            println("📤 Route History Request: terminal_id=$terminalId, start=$startDate, end=$endDate")
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
            val raw = response.bodyAsText()
            println("📝 Route History RAW = $raw")
            Json { ignoreUnknownKeys = true }.decodeFromString(RouteHistoryResponse.serializer(), raw)

        } catch (e: Exception) {
            println("❌ Route History Error: ${e::class.simpleName} — ${e.message}")
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



    // ---------------- VEHICLE PARKING ----------------
    suspend fun parkVehicle(
        vehicleType: String,
        vehicleNumber: String,
        phone: String,
        alternatePhone1: String,
        alternatePhone2: String? = null,
        referralCode: String? = null
    ): ParkingResponse {

        val token = AppTokenProvider.getToken()
        val url   = "${baseUrl}vehicle-parking"

        val requestBody = ParkingRequest(
            vehicle_type = vehicleType.trim(),
            vehicle_number = vehicleNumber.trim(),
            phone = phone.trim(),
            alternate_phone_1 = alternatePhone1.trim(),
            alternate_phone_2 = alternatePhone2?.trim()?.ifEmpty { null },
            refral_code = referralCode?.trim()?.ifEmpty { null }
        )

        println("🌐 parkVehicle → POST $url")
        println("📤 parkVehicle body: ${Json.encodeToString(ParkingRequest.serializer(), requestBody)}")

        return try {
            val response = client.post(url) {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            val raw = response.bodyAsText()
            println("✔️ parkVehicle ${response.status}: $raw")

            val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

            when {
                response.status == HttpStatusCode.OK ||
                        response.status == HttpStatusCode.Created ->
                    json.decodeFromString(ParkingResponse.serializer(), raw)

                response.status == HttpStatusCode.UnprocessableEntity -> {
                    val body = runCatching {
                        json.decodeFromString(ParkingResponse.serializer(), raw)
                    }.getOrNull()
                    ParkingResponse(
                        status  = false,
                        message = body?.message ?: "Validation failed.",
                        errors  = body?.errors
                    )
                }

                response.status == HttpStatusCode.Unauthorized ->
                    ParkingResponse(status = false, message = "Session expired. Please login again.")

                else ->
                    ParkingResponse(status = false, message = "Error: ${response.status.value}")
            }

        } catch (e: Exception) {
            println("❌ parkVehicle: ${e.message}")
            ParkingResponse(status = false, message = "Network error: ${e.message ?: "Unknown"}")
        }
    }

    // ---------------- PARKING HISTORY ----------------
    suspend fun getParkingHistory(): List<ParkingData> {

        val token = AppTokenProvider.getToken()
        val url   = "${baseUrl}vehicle-parking"

        println("🌐 getParkingHistory → GET $url")

        return try {
            val response = client.get(url) {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
            }

            val raw = response.bodyAsText()
            println("📥 getParkingHistory: $raw")

            if (!response.status.isSuccess()) return emptyList()

            val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

            // Try list response: { "status": true, "data": [ {...}, {...} ] }
            runCatching {
                @kotlinx.serialization.Serializable
                data class HistoryList(
                    val status: Boolean          = false,
                    val data: List<ParkingData>? = null
                )
                json.decodeFromString(HistoryList.serializer(), raw).data ?: emptyList()
            }.getOrElse {
                // Fallback: single object response like POST
                runCatching {
                    listOfNotNull(json.decodeFromString(ParkingResponse.serializer(), raw).data)
                }.getOrElse { emptyList() }
            }

        } catch (e: Exception) {
            println("❌ getParkingHistory: ${e.message}")
            emptyList()
        }
    }


    // ---------------- GET USER PARKING DETAILS ----------------
    suspend fun getUserParkingDetails(): UserParkingListResponse {
        val token = AppTokenProvider.getToken()
        val url   = "${baseUrl}get-User-Details"

        println("🌐 getUserParkingDetails → GET $url")

        return try {
            val response = client.get(url) {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
            }

            val raw = response.bodyAsText()
            println("📥 getUserParkingDetails ${response.status}: $raw")

            Json {
                ignoreUnknownKeys = true
                isLenient         = true
                coerceInputValues = true
            }.decodeFromString(UserParkingListResponse.serializer(), raw)

        } catch (e: Exception) {
            println("❌ getUserParkingDetails: ${e.message}")
            UserParkingListResponse(status = false, message = e.message)
        }
    }


    // ---------------- DELETE VEHICLE ----------------
    suspend fun deleteVehicle(vehicleId: Int): JsonObject {
        val token = AppTokenProvider.getToken()
        val url   = "${baseUrl}delete-Vehicle/$vehicleId"

        println("🌐 deleteVehicle → DELETE $url")

        return try {
            val response = client.delete(url) {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
            }
            val raw = response.bodyAsText()
            println("📥 deleteVehicle ${response.status}: $raw")
            Json { ignoreUnknownKeys = true }.parseToJsonElement(raw).jsonObject
        } catch (e: Exception) {
            println("❌ deleteVehicle: ${e.message}")
            throw e
        }
    }


    // ---------------- UPDATE VEHICLE ----------------
    suspend fun updateVehicle(
        vehicleId: Int,
        vehicleType: String,
        vehicleNumber: String,
        phone: String,
        alternatePhone1: String?,
        alternatePhone2: String?
    ): ParkingResponse {
        val token = AppTokenProvider.getToken()
        val url   = "${baseUrl}vehicle-parking/update/$vehicleId"

        val requestBody = ParkingRequest(
            vehicle_type      = vehicleType.trim(),
            vehicle_number    = vehicleNumber.trim(),
            phone             = phone.trim(),
            alternate_phone_1 = alternatePhone1?.trim() ?: "",
            alternate_phone_2 = alternatePhone2?.trim()?.ifEmpty { null }
        )

        println("🌐 updateVehicle → POST $url")

        return try {
            val response = client.post(url) {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            val raw = response.bodyAsText()
            println("✔️ updateVehicle ${response.status}: $raw")
            val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
            when {
                response.status.isSuccess() ->
                    json.decodeFromString(ParkingResponse.serializer(), raw)
                response.status == HttpStatusCode.UnprocessableEntity -> {
                    val body = runCatching { json.decodeFromString(ParkingResponse.serializer(), raw) }.getOrNull()
                    ParkingResponse(status = false, message = body?.message ?: "Validation failed.", errors = body?.errors)
                }
                response.status == HttpStatusCode.Unauthorized ->
                    ParkingResponse(status = false, message = "Session expired. Please login again.")
                else ->
                    ParkingResponse(status = false, message = "Error: ${response.status.value}")
            }
        } catch (e: Exception) {
            println("❌ updateVehicle: ${e.message}")
            ParkingResponse(status = false, message = "Network error: ${e.message ?: "Unknown"}")
        }
    }


    // ─────────────────────────────────────────────────────────────────
// Add these 3 functions inside your existing ApiService class
// ─────────────────────────────────────────────────────────────────

    // ---------------- ALL CHATS WITH MESSAGES ----------------
    suspend fun getMyChatsWithMessages(): MyChatsResponse {
        val token = AppTokenProvider.getToken()
        val url   = "${baseUrl}my-chats-with-messages"
        println("🌐 getMyChatsWithMessages → GET $url")
        return try {
            val response = client.get(url) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            val raw = response.bodyAsText()
            println("📥 getMyChatsWithMessages ${response.status}: ${raw.take(300)}")
            if (!response.status.isSuccess()) {
                return MyChatsResponse(status = false)
            }
            Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
                .decodeFromString(MyChatsResponse.serializer(), raw)
        } catch (e: Exception) {
            println("❌ getMyChatsWithMessages: ${e.message}")
            MyChatsResponse(status = false)
        }
    }

    // ---------------- GET MESSAGES FOR ONE CHAT ----------------
    // New endpoint replaces /visitor-notifications which 404s now.
    suspend fun getChatMessages(chatId: Int): ChatMessagesResponse {

        val token = AppTokenProvider.getToken()
        val url   = "${baseUrl}chat-messages/$chatId"

        println("🌐 getChatMessages → GET $url")

        return try {
            val response = client.get(url) {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.Accept, "application/json")
                contentType(ContentType.Application.Json)
            }

            val raw = response.bodyAsText()
            println("📥 getChatMessages ${response.status}: $raw")

            if (!response.status.isSuccess()) {
                ChatMessagesResponse(status = false, message = "Error: ${response.status.value}")
            } else {
                val root = Json.parseToJsonElement(raw).jsonObject
                val status = root["status"]?.jsonPrimitive?.booleanOrNull ?: false
                val closed = root["closed"]?.jsonPrimitive?.booleanOrNull ?: false
                val message = root["message"]?.jsonPrimitive?.contentOrNull
                val dataArray = root["data"]?.jsonArray

                if (dataArray == null) {
                    ChatMessagesResponse(status = status, closed = closed, message = message)
                } else {
                    val messages = dataArray.mapNotNull { element ->
                        runCatching<ChatMessageDto> {
                            val obj = element.jsonObject
                            // id can be int (21) or string ("call_12") — extract numeric part
                            val id: Int? = when (val idEl = obj["id"]) {
                                is JsonPrimitive -> when {
                                    idEl.isString -> idEl.content.filter { it.isDigit() }.toIntOrNull()
                                    else          -> idEl.intOrNull
                                }
                                else -> null
                            }
                            ChatMessageDto(
                                id           = id,
                                message      = obj["message"]?.jsonPrimitive?.contentOrNull,
                                sender_id    = obj["sender_id"]?.jsonPrimitive?.intOrNull,
                                type         = obj["type"]?.jsonPrimitive?.contentOrNull,
                                time         = obj["time"]?.jsonPrimitive?.contentOrNull,
                                timestamp    = obj["timestamp"]?.jsonPrimitive?.contentOrNull,
                                is_delivered = obj["is_delivered"]?.jsonPrimitive?.intOrNull,
                                is_seen      = obj["is_seen"]?.jsonPrimitive?.intOrNull,
                                call_status  = obj["call_status"]?.jsonPrimitive?.contentOrNull,
                                room_id      = obj["room_id"]?.jsonPrimitive?.contentOrNull
                            )
                        }.getOrNull()
                    }
                    ChatMessagesResponse(status = status, closed = closed, message = message, data = messages)
                }
            }

        } catch (e: Exception) {
            println("❌ getChatMessages: ${e.message}")
            ChatMessagesResponse(status = false, message = "Network error: ${e.message ?: "Unknown"}")
        }
    }

    // ---------------- GET ALL SOS ALERTS (DEPRECATED — endpoint 404s) ----------------
    // Kept as a no-op so legacy callers compile; returns empty success.
    suspend fun getSosAlerts(): SosAlertListResponse {

        val token = AppTokenProvider.getToken()
        val url   = "${baseUrl}visitor-notifications"  // ⚠️ removed by backend; left for log only

        println("🌐 getSosAlerts → GET $url")

        return try {
            val response = client.get(url) {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
            }

            val raw = response.bodyAsText()
            println("📥 getSosAlerts ${response.status}: $raw")

            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            }

            if (response.status.isSuccess()) {
                json.decodeFromString(SosAlertListResponse.serializer(), raw)
            } else {
                SosAlertListResponse(
                    status = false,
                    message = "Error: ${response.status.value}"
                )
            }

        } catch (e: Exception) {
            println("❌ getSosAlerts: ${e.message}")
            SosAlertListResponse(
                status = false,
                message = "Network error: ${e.message ?: "Unknown"}"
            )
        }
    }

    // ---------------- GET SINGLE ALERT DETAIL ----------------
    suspend fun getSosAlertDetail(alertId: Int): SosAlertDetailResponse {

        val token = AppTokenProvider.getToken()

        // 🔥 FIXED ENDPOINT
        val url = "${baseUrl}visitor-notifications/$alertId"

        println("🌐 getSosAlertDetail → GET $url")

        return try {

            val response = client.get(url) {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
            }

            val raw = response.bodyAsText()

            println("📥 getSosAlertDetail ${response.status}: $raw")

            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            }

            when {

                response.status.isSuccess() -> {
                    json.decodeFromString(
                        SosAlertDetailResponse.serializer(),
                        raw
                    )
                }

                response.status == HttpStatusCode.NotFound -> {
                    SosAlertDetailResponse(
                        status = false,
                        message = "Alert not found."
                    )
                }

                response.status == HttpStatusCode.Unauthorized -> {
                    SosAlertDetailResponse(
                        status = false,
                        message = "Session expired. Please login again."
                    )
                }

                else -> {
                    SosAlertDetailResponse(
                        status = false,
                        message = "Error: ${response.status.value}"
                    )
                }
            }

        } catch (e: Exception) {

            println("❌ getSosAlertDetail: ${e.message}")

            SosAlertDetailResponse(
                status = false,
                message = "Network error: ${e.message ?: "Unknown"}"
            )
        }
    }
    // ---------------- REPLY TO SOS ALERT ----------------
    // ---------------- OWNER REPLY TO ALERT ----------------
    suspend fun sendOwnerReply(request: OwnerReplyRequest): SosReplyResponse {

        val token = AppTokenProvider.getToken()
        val url   = "${baseUrl}owner/send-message"

        // Build the body explicitly so we know exactly what hits the wire — avoids any doubt
        // about how a @Serializable data class is mapped to JSON.
        val bodyJson = buildJsonObject {
            put("chat_id", request.chat_id)
            put("message", request.message)
        }
        val bodyString = Json.encodeToString(JsonObject.serializer(), bodyJson)

        println("🌐 sendOwnerReply → POST $url")
        println("📦 sendOwnerReply body = $bodyString")
        println("🔑 sendOwnerReply token preview = ${token.take(20)}…")

        return try {
            val response = client.post(url) {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.Accept, "application/json")
                contentType(ContentType.Application.Json)
                setBody(bodyString)
            }

            val raw = response.bodyAsText()
            println("✔️ sendOwnerReply ${response.status}: $raw")

            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            }

            when {
                response.status == HttpStatusCode.OK ||
                        response.status == HttpStatusCode.Created ->
                    json.decodeFromString(SosReplyResponse.serializer(), raw)

                response.status == HttpStatusCode.Unauthorized ->
                    SosReplyResponse(status = false, message = "Session expired. Please login again.")

                else ->
                    SosReplyResponse(status = false, message = "Error: ${response.status.value}")
            }

        } catch (e: Exception) {
            println("❌ sendOwnerReply: ${e.message}")
            SosReplyResponse(status = false, message = "Network error: ${e.message ?: "Unknown"}")
        }
    }

    // ---------------- CLOSE CHAT (mark resolved) ----------------
    suspend fun closeChat(chatId: Int): SosReplyResponse {

        val token = AppTokenProvider.getToken()
        val url   = "${baseUrl}owner/chat/$chatId/close"

        println("🌐 closeChat → POST $url")

        return try {
            val response = client.post(url) {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.Accept, "application/json")
                contentType(ContentType.Application.Json)
            }

            val raw = response.bodyAsText()
            println("✔️ closeChat ${response.status}: $raw")

            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            }

            when {
                response.status == HttpStatusCode.OK ||
                        response.status == HttpStatusCode.Created ->
                    json.decodeFromString(SosReplyResponse.serializer(), raw)

                response.status == HttpStatusCode.Unauthorized ->
                    SosReplyResponse(status = false, message = "Session expired. Please login again.")

                else ->
                    SosReplyResponse(status = false, message = "Error: ${response.status.value}")
            }

        } catch (e: Exception) {
            println("❌ closeChat: ${e.message}")
            SosReplyResponse(status = false, message = "Network error: ${e.message ?: "Unknown"}")
        }
    }


    // ---------------- MARK MESSAGE DELIVERED ----------------
    // Called when a chat message arrives on device but user hasn't opened the chat yet.
    suspend fun markDelivered(chatId: Int) {
        val token = AppTokenProvider.getToken()
        val url   = "${baseUrl}chat/$chatId/delivered"
        try {
            val response = client.post(url) {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.Accept, "application/json")
            }
            val raw = response.bodyAsText()
            println("✔️ markDelivered #$chatId: ${response.status} | $raw")
        } catch (e: Exception) {
            println("❌ markDelivered #$chatId: ${e.message}")
        }
    }

    // ---------------- MARK CHAT SEEN ----------------
    // Called when the owner opens the chat screen and reads the messages.
    suspend fun markSeen(chatId: Int) {
        val token = AppTokenProvider.getToken()
        val url   = "${baseUrl}chat/$chatId/seen"
        println("🔵 markSeen → POST $url")
        try {
            val response = client.post(url) {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.Accept, "application/json")
            }
            val raw = response.bodyAsText()
            println("✔️ markSeen #$chatId: ${response.status} | response=$raw")
        } catch (e: Exception) {
            println("❌ markSeen #$chatId: ${e.message}")
        }
    }

    // ---------------- ACCEPT INCOMING CALL ----------------
    // POST gps/call/accept  { "room_id": "..." }
    suspend fun acceptCall(roomId: String): SosReplyResponse {
        val token = AppTokenProvider.getToken()
        val url   = "${gpsBaseUrl}call/accept"
        val body  = buildJsonObject { put("room_id", roomId) }
        println("🌐 acceptCall → POST $url  room_id=$roomId")
        return try {
            val response = client.post(url) {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.Accept, "application/json")
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(JsonObject.serializer(), body))
            }
            val raw = response.bodyAsText()
            println("✔️ acceptCall ${response.status}: $raw")
            val lenient = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
            when {
                response.status.value in 200..201 ->
                    lenient.decodeFromString(SosReplyResponse.serializer(), raw)
                response.status == HttpStatusCode.Unauthorized ->
                    SosReplyResponse(status = false, message = "Session expired.")
                else ->
                    SosReplyResponse(status = false, message = "Error: ${response.status.value}")
            }
        } catch (e: Exception) {
            println("❌ acceptCall: ${e.message}")
            SosReplyResponse(status = false, message = "Network error: ${e.message ?: "Unknown"}")
        }
    }

    // ---------------- DECLINE INCOMING CALL ----------------
    // POST gps/call/decline  { "room_id": "..." }
    suspend fun declineCall(roomId: String): SosReplyResponse {
        val token = AppTokenProvider.getToken()
        val url   = "${gpsBaseUrl}call/decline"
        val body  = buildJsonObject { put("room_id", roomId) }
        println("🌐 declineCall → POST $url  room_id=$roomId")
        return try {
            val response = client.post(url) {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.Accept, "application/json")
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(JsonObject.serializer(), body))
            }
            val raw = response.bodyAsText()
            println("✔️ declineCall ${response.status}: $raw")
            val lenient = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
            when {
                response.status.value in 200..201 ->
                    lenient.decodeFromString(SosReplyResponse.serializer(), raw)
                response.status == HttpStatusCode.Unauthorized ->
                    SosReplyResponse(status = false, message = "Session expired.")
                else ->
                    SosReplyResponse(status = false, message = "Error: ${response.status.value}")
            }
        } catch (e: Exception) {
            println("❌ declineCall: ${e.message}")
            SosReplyResponse(status = false, message = "Network error: ${e.message ?: "Unknown"}")
        }
    }

    // ---------------- INITIATE CALL (OWNER → VISITOR) ----------------
    // POST owner/call/initiate  { "chat_id": chatId }
    // Backend sends FCM to visitor and returns the room_id for WebRTC signaling.
    suspend fun initiateCall(chatId: Int): InitiateCallResponse {
        val token = AppTokenProvider.getToken()
        val url   = "${gpsBaseUrl}owner/call/initiate"
        val body  = buildJsonObject { put("chat_id", chatId) }
        println("🌐 initiateCall → POST $url  chat_id=$chatId")
        return try {
            val response = client.post(url) {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.Accept, "application/json")
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(JsonObject.serializer(), body))
            }
            val raw = response.bodyAsText()
            println("✔️ initiateCall ${response.status}: $raw")
            val lenient = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
            when {
                response.status.value in 200..201 ->
                    lenient.decodeFromString(InitiateCallResponse.serializer(), raw)
                response.status == HttpStatusCode.Unauthorized ->
                    InitiateCallResponse(success = false, message = "Session expired.")
                else ->
                    InitiateCallResponse(success = false, message = "Error: ${response.status.value}")
            }
        } catch (e: Exception) {
            println("❌ initiateCall: ${e.message}")
            InitiateCallResponse(success = false, message = "Network error: ${e.message ?: "Unknown"}")
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

        println("🌐 POST $url")
        println("📦 CREATE ORDER BODY = ${Json.encodeToString(CreateOrderRequest.serializer(), req)}")

        val response = client.post(url) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(req)
        }

        val raw = response.bodyAsText()
        println("📡 CREATE ORDER STATUS = ${response.status}")
        println("📝 CREATE ORDER RAW = $raw")

        return Json { ignoreUnknownKeys = true }.parseToJsonElement(raw).jsonObject
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


    suspend fun getMyBilling(token: String): MyBillingResponse {
        val url = "${baseUrl}my-billing"

        println("🌐 API: Fetching My Billing")

        return try {
            val response = client.get(url) {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }

            val raw = response.bodyAsText()
            println("🧾 MY BILLING RAW = $raw")

            Json {
                ignoreUnknownKeys = true
            }.decodeFromString(
                MyBillingResponse.serializer(),
                raw
            )

        } catch (e: Exception) {
            println("❌ My Billing Error: ${e.message}")
            MyBillingResponse(
                status = false,
                totalOrders = 0,
                grandTotal = 0,
                billings = emptyList()
            )
        }
    }



    // ---------------- DEVICE PLAN RENEW ----------------
    suspend fun renewDevicePlan(
        body: RenewPlanRequest
    ): RenewPlanResponse {

        val token = AppTokenProvider.getToken()

        val response = client.post("${baseUrl}device/renew") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(body)
        }

        val raw = response.bodyAsText()
        println("🧾 RENEW RAW RESPONSE = $raw")

        return Json {
            ignoreUnknownKeys = true
        }.decodeFromString(RenewPlanResponse.serializer(), raw)
    }


    suspend fun sendPhonePeCallback(
        request: PhonePeCallbackRequest
    ): Boolean {
        val token = AppTokenProvider.getToken()

        println("📡 Sending PhonePe Callback → $request")

        val response = client.post("${baseUrl}phonepe/callback") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(request)
        }

        println("📡 Callback status = ${response.status}")
        println("📡 Callback body = ${response.bodyAsText()}")

        return response.status.isSuccess()
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
        val url = "${baseUrl}get-geofences"

        println("🌐 POST $url")

        return try {
            val response = client.post(url) {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }

            val raw = response.bodyAsText()
            println("📡 GET GEOFENCES STATUS = ${response.status}")
            println("📝 GET GEOFENCES RAW = $raw")

            Json { ignoreUnknownKeys = true }
                .decodeFromString(GeoFenceResponse.serializer(), raw)

        } catch (e: Exception) {
            println("❌ getGeoFences ERROR = ${e.message}")
            GeoFenceResponse(success = false, message = e.message)
        }
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

        val response = client.submitForm(
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
        }

        val raw = response.bodyAsText()
        println("📡 SAVE GEOFENCE STATUS = ${response.status}")
        println("📝 SAVE GEOFENCE RAW = $raw")

        return Json { ignoreUnknownKeys = true }
            .parseToJsonElement(raw).jsonObject
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


     // Plans and Subcriptions
     suspend fun getUserDevices(): UserDevicesResponse {
         val token = AppTokenProvider.getToken()

         println("🌐 API: Fetching plans data")
         return client.get("${baseUrl}get-user-devices") {
             header(HttpHeaders.Authorization, "Bearer $token")
//             header(HttpHeaders.Accept, ContentType.Application.Json.toString())
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
        fcmToken:   String,
        deviceUuid: String,
        platform:   String = "android"
    ) {
        val response = client.post("${baseUrl}fcm/register") {
            headers {
                append("Authorization", authHeader)
                append("Content-Type", "application/json")
            }
            setBody(
                mapOf(
                    "fcm_token"   to fcmToken,
                    "device_uuid" to deviceUuid,
                    "platform"    to platform
                )
            )
        }

        println("📡 FCM API STATUS: ${response.status}")
        println("📡 FCM API BODY: ${response.bodyAsText()}")
    }

    // ---------------- LOGOUT ----------------
    suspend fun logout(authHeader: String, fcmToken: String) {
        println("🚪 API: Calling logout... fcmToken=${fcmToken.take(20)}")
        val response = client.post("${baseUrl}logout") {
            header(HttpHeaders.Authorization, authHeader)
            contentType(ContentType.Application.Json)
            setBody(mapOf("fcm_token" to fcmToken))
        }
        println("🚪 Logout status: ${response.status}")
        println("🚪 Logout body: ${response.bodyAsText()}")
    }

    // ---------------- VAULT UNLOCK ----------------
    suspend fun unlockVault(
        body: Map<String, String>
    ): VaultResponse {

        val token = AppTokenProvider.getToken() // 🔑 USER LOGIN TOKEN
        val url = "${baseUrl}vault/unlock"

        println("🌐 API: Unlocking Vault")

        val response = client.post(url) {
            header(HttpHeaders.Authorization, "Bearer $token") // ✅ FIX
            contentType(ContentType.Application.Json)
            setBody(body)
        }

        println("✔️ Vault Unlock Status: ${response.status}")

        // ❌ 401 / 403 handle separately
        if (response.status != HttpStatusCode.OK) {
            val errorBody = response.bodyAsText()
            println("❌ Vault Unlock Failed Body: $errorBody")
            throw Exception("Invalid password")
        }

        return response.body()
    }


    // ---------------- VAULT DOCUMENTS ----------------
    suspend fun getVaultDocuments(
        vaultToken: String,
        authToken: String
    ): List<VaultDocumentDto> {

        val url = "${baseUrl}vault/documents"

        println("🌐 API: Fetching Vault Documents")
        println("🔑 Vault Token: $vaultToken")

        return try {
            val response = client.get(url) {
                header("X-Vault-Token", vaultToken)
                header(HttpHeaders.Authorization, "Bearer $authToken")
                header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }

            val raw = response.bodyAsText()
            println("📥 Vault Documents RAW = $raw")
            println("📡 Status = ${response.status}")

            if (response.status != HttpStatusCode.OK) {
                throw Exception("Failed to fetch documents")
            }

            // ✅ Deserialize list
            kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
            }.decodeFromString(
                ListSerializer(VaultDocumentDto.serializer()),
                raw
            )

        } catch (e: Exception) {
            println("❌ Vault Documents Error: ${e.message}")
            emptyList()
        }
    }


    suspend fun getVaultDocumentFile(
        documentId: String,
        vaultToken: String,
        authToken: String
    ): ByteArray {

        val url = "${baseUrl}vault/$documentId"

        println("🌐 API: Fetching encrypted document")

        val response = client.get(url) {
            header("X-Vault-Token", vaultToken)
            header(HttpHeaders.Authorization, "Bearer $authToken")
        }

        if (response.status != HttpStatusCode.OK) {
            throw Exception("Failed to fetch document")
        }

        return response.body()
    }

    suspend fun deleteVaultDocument(
        documentId: String,
        vaultToken: String,
        authToken: String
    ) {
        val url = "${baseUrl}vault/$documentId"

        val response = client.delete(url) {
            header("X-Vault-Token", vaultToken)
            header(HttpHeaders.Authorization, "Bearer $authToken")
        }

        if (response.status != HttpStatusCode.OK) {
            throw Exception("Failed to delete document")
        }
    }


    suspend fun uploadVaultDocument(
        vaultToken: String,
        authToken: String,
        encryptedFile: ByteArray,
        fileName: String,
        fileType: String,
        salt: String,
        iv: String,
        authTag: String,
        checksum: String
    ) {
        val response = client.submitFormWithBinaryData(
            url = "${baseUrl}vault/upload",
            formData = formData {
                append("encrypted_file", encryptedFile, Headers.build {
                    append(HttpHeaders.ContentType, "image/jpeg")
                    append(
                        HttpHeaders.ContentDisposition,
                        "filename=\"$fileName.jpg\""
                    )
                })
                append("file_name", fileName)
                append("file_type", fileType)
                append("salt", salt)
                append("iv", iv)
                append("auth_tag", authTag)
                append("checksum", checksum)
            }
        ) {
            headers {
                append("X-Vault-Token", vaultToken)
                append("Authorization", "Bearer $authToken")
            }
        }

        val raw = response.bodyAsText()
        println("📤 uploadVaultDocument ${response.status}: $raw")

        if (!response.status.isSuccess()) {
            throw Exception("Upload failed (${response.status.value}): $raw")
        }
    }


    suspend fun updateVaultDocument(
        documentId: String,
        vaultToken: String,
        authToken: String,
        encryptedFile: ByteArray?, // 👈 optional
        fileName: String,
        fileType: String,
        salt: String,
        iv: String,
        authTag: String,
        checksum: String
    ) {
        client.submitFormWithBinaryData(
            url = "${baseUrl}vault/update/$documentId",
            formData = formData {

                encryptedFile?.let {
                    append("encrypted_file", it, Headers.build {
                        append(HttpHeaders.ContentType, "image/png")
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName.png\"")
                    })
                }

                append("file_name", fileName)
                append("file_type", fileType)
                append("salt", salt)
                append("iv", iv)
                append("auth_tag", authTag)
                append("checksum", checksum)
            }
        ) {
            headers {
                append("X-Vault-Token", vaultToken)
                append("Authorization", "Bearer $authToken")
            }
        }
    }

    // ---------------- SUBMIT SUPPORT REQUEST ----------------
    suspend fun submitSupportRequest(request: SupportRequest): SupportResponse {
        val token = AppTokenProvider.getToken()
        println("🌐 API: Submitting support request for ${request.name}")

        return try {
            val response = client.post("${baseUrl}support") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            val raw = response.bodyAsText()
            println("✔️ Support Request Status: ${response.status}")
            println("📝 Support Response: $raw")

            Json { ignoreUnknownKeys = true }.decodeFromString(SupportResponse.serializer(), raw)

        } catch (e: Exception) {
            println("❌ Support Request Error: ${e.message}")
            throw e
        }
    }
}





