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
import com.developer.ev2z.ui.screens.serviceWarranty.*
import com.developer.ev2z.util.AppTokenProvider
import kotlinx.coroutines.delay
import io.ktor.client.call.*
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
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
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class UnauthorizedException : Exception("Session expired. Please login again.")

/**
 * The server answered 404/405/501 — the route is not deployed yet.
 *
 * Thrown only by calls whose endpoint is still being built, so a caller can fall
 * back to a live one instead of dead-ending the user. See [ApiService.createServiceOrder].
 */
class ServiceEndpointMissingException(val path: String) :
    Exception("Endpoint $path is not available on the server yet.")

/** 404/405/501 all mean "this route does not exist here", not "your request was bad". */
private val MISSING_ENDPOINT_CODES = setOf(404, 405, 501)

/**
 * Type + message + cause, for logging a failure that has to be told apart from
 * the others without a debugger.
 *
 * `e.message` alone is not enough on iOS: a Darwin/NSURLSession failure often
 * carries a null message, so the only thing that reaches the console is
 * "failed (null)" — which is why a transport error and an expired session read
 * identically there.
 */
internal fun Throwable.describe(): String = buildString {
    append(this@describe::class.simpleName ?: "Exception")
    message?.let { append(": ").append(it) }
    cause?.let { append(" ← ").append(it::class.simpleName).append(": ").append(it.message) }
}

/**
 * Enough of a bearer token to tell three cases apart in a log without printing the
 * secret: never sent at all, sent but belonging to a different session, or sent and
 * simply rejected.
 *
 * The part before "|" is the Sanctum *personal access token* id, not the user id
 * (see AppUserProvider) — but it is what identifies which session was used, so a
 * 401 can be matched against the session the other platform is signed in with.
 */
private fun String.tokenFingerprint(): String =
    if (isBlank()) "MISSING (empty token)" else "id=${substringBefore("|")} len=$length"

/**
 * What `POST service/complaints` answered, status included.
 *
 * The status has to travel with the body: a 422 validation body carries neither
 * `success` nor `payment_required`, and reading it as a plain payload made the app
 * announce a ticket for a complaint the server had refused (seen 2026-09-26, a
 * `vehicle_type` 422 shown to the user as "Complaint submitted").
 */
data class ServiceComplaintPost(val statusCode: Int, val body: JsonObject) {
    val isSuccess: Boolean get() = statusCode in 200..299

    /**
     * The validation complaint, as one line: Laravel's `errors` map first (it names
     * the field that was refused), then the top-level `message`.
     */
    val validationMessage: String?
        get() {
            val fromErrors = body["errors"]
                ?.let { runCatching { it.jsonObject }.getOrNull() }
                ?.values
                ?.firstNotNullOfOrNull { entry ->
                    runCatching { entry.jsonArray.firstOrNull()?.jsonPrimitive?.content }.getOrNull()
                        ?: runCatching { entry.jsonPrimitive.content }.getOrNull()
                }
            return fromErrors ?: body["message"]?.let {
                runCatching { it.jsonPrimitive.content }.getOrNull()
            }
        }

    /** True when the server refused the `vehicle_type` value specifically. */
    val vehicleTypeRefused: Boolean
        get() = body["errors"]
            ?.let { runCatching { it.jsonObject }.getOrNull() }
            ?.containsKey("vehicle_type") == true
}

class ApiService {

    private val client = KtorClient.client
    private val baseUrl = KtorClient.BASE_URL
    private val gpsBaseUrl = "${KtorClient.SOCKET_SERVER_URL}/api/"

    companion object {
        /** Open-Meteo forecast endpoint — open source, keyless, no bearer token. */
        const val WEATHER_BASE_URL = "https://api.open-meteo.com/v1/forecast"

        /**
         * Resolves a possibly-relative image URL (e.g. "/storage/vault/x.jpeg")
         * returned by the API into an absolute URL against the app host.
         */
        fun absoluteImageUrl(imageUrl: String?): String? {
            if (imageUrl.isNullOrBlank()) return null
            if (imageUrl.startsWith("http")) return imageUrl
            val host = KtorClient.BASE_URL.removeSuffix("api/").removeSuffix("/")
            return host + if (imageUrl.startsWith("/")) imageUrl else "/$imageUrl"
        }
    }

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
    /**
     * Posted to the **GPS server**, not the app host.
     *
     * The two hosts run different versions of this route: app.eva2z.in validates
     * `sim_number` as `max:20` and so rejects every 20-digit SIM once the label's
     * "U" suffix is appended (21 chars, 422), while gps.eva2z.in accepts it —
     * verified 2026-09-19 with `89788978901234567698U`. Same bearer token works
     * on both, as `submit-bgv` already relies on.
     */
    suspend fun addDevice(token: String, body: JsonObject): JsonObject {
        println("🌐 API: Adding device → ${gpsBaseUrl}add-device")

        return try {
            val response = client.post("${gpsBaseUrl}add-device") {
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

    // ---------------- FUEL PRICES ----------------
    /** Lenient so a rate sent as a number instead of "117.51" still decodes. */
    private val fuelJson = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Public endpoint — no bearer token. Every state / union territory. */
    suspend fun getFuelPrices(): FuelPricesResponse {
        println("🌐 API: Fetching fuel prices (all states)")

        return try {
            val response = client.get("${baseUrl}fuel-prices") {
                header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }

            val raw = response.bodyAsText()
            println("✔️ Fuel Prices Status: ${response.status}")

            fuelJson.decodeFromString(FuelPricesResponse.serializer(), raw)

        } catch (e: Exception) {
            println("❌ Fuel Prices Error: ${e.message}")
            throw e
        }
    }

    /**
     * Public endpoint — refreshes a single state. The name is a path segment
     * (`fuel-prices/Andhra Pradesh`), so it is appended through the URL builder to
     * get encoded properly.
     */
    suspend fun getFuelPriceForState(state: String): FuelPriceStateResponse {
        println("🌐 API: Fetching fuel price for state=$state")

        return try {
            val response = client.get("${baseUrl}fuel-prices") {
                url { appendPathSegments(state) }
                header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }

            val raw = response.bodyAsText()
            println("✔️ Fuel Price ($state) Status: ${response.status}")

            fuelJson.decodeFromString(FuelPriceStateResponse.serializer(), raw)

        } catch (e: Exception) {
            println("❌ Fuel Price ($state) Error: ${e.message}")
            throw e
        }
    }

    // ---------------- WEATHER ----------------
    private val weatherJson = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Current conditions plus an hourly and 7-day forecast for a coordinate, from
     * Open-Meteo — an open-source, keyless service, so this call bypasses [baseUrl]
     * and carries no bearer token.
     *
     * `timezone=auto` makes every timestamp local to the coordinate, which is what the
     * screen renders directly; without it the hour labels would be UTC.
     */
    suspend fun getWeatherForecast(lat: Double, lng: Double): WeatherForecastResponse {
        println("🌐 API: Fetching weather for lat=$lat lng=$lng")

        return try {
            val response = client.get(WEATHER_BASE_URL) {
                url {
                    parameters.append("latitude", lat.toString())
                    parameters.append("longitude", lng.toString())
                    parameters.append(
                        "current",
                        "temperature_2m,relative_humidity_2m,apparent_temperature,is_day," +
                            "precipitation,weather_code,wind_speed_10m,pressure_msl"
                    )
                    parameters.append(
                        "hourly",
                        "temperature_2m,weather_code,precipitation_probability"
                    )
                    parameters.append(
                        "daily",
                        "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset," +
                            "precipitation_probability_max,wind_speed_10m_max,uv_index_max"
                    )
                    parameters.append("timezone", "auto")
                    parameters.append("forecast_days", "7")
                }
                header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }

            val raw = response.bodyAsText()
            println("✔️ Weather Status: ${response.status}")

            weatherJson.decodeFromString(WeatherForecastResponse.serializer(), raw)

        } catch (e: Exception) {
            println("❌ Weather Error: ${e.message}")
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

            // The gateway answers 200 even when it is reporting a failure — the real
            // status lives in the body: {"status":"error","error":{"status":409,…}}.
            // Without this the duplicate below (a ClientRequestException) never fires.
            val errorObj = response["error"] as? JsonObject
            if (errorObj != null ||
                (response["status"] as? JsonPrimitive)?.contentOrNull == "error"
            ) {
                val code    = (errorObj?.get("status") as? JsonPrimitive)?.intOrNull
                val message = (errorObj?.get("message") as? JsonPrimitive)?.contentOrNull
                    ?: (response["message"] as? JsonPrimitive)?.contentOrNull

                return if (code == 409) {
                    println("⚠️ Duplicate Candidate (409 in body)")
                    CandidateResult(isDuplicate = true, error = message)
                } else {
                    println("❌ API Error ($code): $message")
                    CandidateResult(error = message ?: "Something went wrong")
                }
            }

            val candidateId =
                response["data"]?.jsonObject
                    ?.get("data")?.jsonObject
                    ?.get("candidate_id")?.jsonPrimitive?.contentOrNull

            println("✅ Parsed candidateId = $candidateId")

            if (candidateId == null)
                CandidateResult(error = "Server did not return a candidate id")
            else
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

        val response = client.post("${baseUrl}crminal-create-order") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(mapOf("amount" to amount))
        }

        val raw = response.bodyAsText()
        println("💳 CRIMINAL CREATE-ORDER (${response.status}) → $raw")

        return kotlinx.serialization.json.Json
            .parseToJsonElement(raw)
            .jsonObject
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
        countryName: String,
        countryCode: String,
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
            country_name = countryName.trim(),
            country_code = countryCode.trim(),
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
        alternatePhone2: String?,
        countryName: String,
        countryCode: String
    ): ParkingResponse {
        val token = AppTokenProvider.getToken()
        val url   = "${baseUrl}vehicle-parking/update/$vehicleId"

        val requestBody = ParkingRequest(
            vehicle_type      = vehicleType.trim(),
            vehicle_number    = vehicleNumber.trim(),
            phone             = phone.trim(),
            alternate_phone_1 = alternatePhone1?.trim() ?: "",
            alternate_phone_2 = alternatePhone2?.trim()?.ifEmpty { null },
            country_name      = countryName.trim(),
            country_code      = countryCode.trim()
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
    // POST call/accept  { "room_id": "..." }
    suspend fun acceptCall(roomId: String): SosReplyResponse {
        val token = AppTokenProvider.getToken()
        val url   = "${baseUrl}call/accept"
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
    // POST call/decline  { "room_id": "..." }
    suspend fun declineCall(roomId: String): SosReplyResponse {
        val token = AppTokenProvider.getToken()
        val url   = "${baseUrl}call/decline"
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
        val url   = "${baseUrl}owner/call/initiate"
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

    /**
     * POST phonepe/webhook — the route PhonePe's own servers call, and the one
     * that turns a paid SERVICE-… order into a ticket.
     *
     * It is not the same thing as `phonepe/callback`: that one is authenticated
     * and settles the order (it answers "OK" for a service order and files
     * nothing), while this one is public — no bearer token — and is where the
     * complaint actually gets created. Confirmed by the backend team on
     * 2026-09-22 after they replayed this body by hand and the ticket appeared.
     *
     * Calling it from the app stands in for a webhook that is not reaching the
     * server on its own. Harmless to send twice: it is keyed by the order id, so
     * PhonePe's real delivery and this one converge on the same result.
     */
    suspend fun sendPhonePeWebhook(
        merchantOrderId: String,
        phonepeOrderId: String?,
        status: String,
    ): Boolean {
        println("📡 Sending PhonePe webhook → $merchantOrderId / $status")

        val response = client.post("${baseUrl}phonepe/webhook") {
            contentType(ContentType.Application.Json)
            setBody(
                buildMap {
                    put("merchantOrderId", merchantOrderId)
                    put("merchant_order_id", merchantOrderId)
                    phonepeOrderId?.takeIf { it.isNotBlank() }?.let {
                        put("phonepeOrderId", it)
                        put("phonepe_order_id", it)
                    }
                    put("status", status)
                    put("state", status)
                }
            )
        }

        val raw = runCatching { response.bodyAsText() }.getOrDefault("")
        println("📡 Webhook status = ${response.status} → ${raw.take(300)}")

        return response.status.isSuccess()
    }



    // ---------------- COMMUNITY POSTS ----------------
    suspend fun getCommunityPosts(): List<Post> {
        val token = AppTokenProvider.getToken()

        println("🌐 API: Fetching community posts")

        return try {
            val response = client.get("${baseUrl}posts") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            println("✔️ Posts Status: ${response.status}")

            val raw = response.bodyAsText()
            val json = Json { ignoreUnknownKeys = true }
            val element = json.parseToJsonElement(raw)

            // API may return either a bare array [ {...} ] or a wrapper { "data": [ {...} ] }
            val arrayElement = when {
                element is kotlinx.serialization.json.JsonArray -> element
                element is JsonObject && element["data"] is kotlinx.serialization.json.JsonArray ->
                    element["data"]!!.jsonArray
                else -> kotlinx.serialization.json.JsonArray(emptyList())
            }

            json.decodeFromJsonElement(ListSerializer(Post.serializer()), arrayElement)

        } catch (e: Exception) {
            println("❌ Community Posts Error: ${e.message}")
            throw e
        }
    }


    suspend fun createCommunityPost(
        title: String,
        body: String,
        images: List<ByteArray>,
        lat: Double?,
        long: Double?,
        city: String?,
        category: String?
    ): JsonObject {

        val token = AppTokenProvider.getToken()

        return client.submitFormWithBinaryData(
            url = "${baseUrl}posts",
            formData = formData {
                append("title", title)
                append("body", body)
                lat?.let { append("lat", it.toString()) }
                long?.let { append("long", it.toString()) }
                city?.takeIf { it.isNotBlank() }?.let { append("city", it) }
                category?.takeIf { it.isNotBlank() }?.let { append("category", it) }

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


    // ---------------- EDIT / DELETE POST ----------------
    // 🔧 ENDPOINTS: these follow the Laravel REST siblings of `POST posts`.
    // When the backend confirms the real routes, change only these two strings.
    private fun updatePostPath(postId: Int) = "posts/$postId"
    private fun deletePostPath(postId: Int) = "posts/$postId"

    // 🔧 FIELD NAMES for the media part of an edit. Rename here if the backend
    // expects something else (e.g. "images[]" / "keep_media[]" / "delete_media[]").
    private val newMediaField     = "media[]"
    private val keptMediaField    = "existing_media[]"
    private val removedMediaField = "removed_media[]"

    /**
     * Edit an existing community post — text, category and images.
     *
     * Sent as multipart (same shape as the create call) because new photos travel as
     * files. Laravel can't read multipart from a real PUT, so it goes out as POST with
     * `_method=PUT` (method spoofing); if the route turns out to be a plain POST and
     * answers 404/405, it is retried once without `_method`.
     *
     * @param newImages       freshly picked photos to upload
     * @param keptMediaUrls   existing media_urls the user kept — pass null to leave
     *                        the post's existing media untouched
     * @param removedMediaUrls existing media_urls the user deleted
     */
    suspend fun updateCommunityPost(
        postId: Int,
        title: String?,
        body: String,
        category: String?,
        newImages: List<ByteArray> = emptyList(),
        keptMediaUrls: List<String>? = null,
        removedMediaUrls: List<String> = emptyList(),
    ): ApiActionResult {
        val token = AppTokenProvider.getToken()
        val url = "${baseUrl}${updatePostPath(postId)}"

        println("🌐 API: Update post $postId → $url")
        println("   ➡️ new images=${newImages.size}, kept=${keptMediaUrls?.size ?: "untouched"}, removed=${removedMediaUrls.size}")

        // Built twice (with and without method spoofing), so it lives in a lambda.
        fun parts(spoofPut: Boolean) = formData {
            if (spoofPut) append("_method", "PUT")

            title?.takeIf { it.isNotBlank() }?.let { append("title", it) }
            append("body", body)
            category?.takeIf { it.isNotBlank() }?.let { append("category", it) }

            keptMediaUrls?.forEach { append(keptMediaField, it) }
            removedMediaUrls.forEach { append(removedMediaField, it) }

            newImages.forEachIndexed { index, bytes ->
                append(
                    key = newMediaField,
                    value = bytes,
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=image_$index.jpg")
                    }
                )
            }
        }

        return try {
            var response = client.submitFormWithBinaryData(url, parts(spoofPut = true)) {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }

            if (response.status == HttpStatusCode.NotFound ||
                response.status == HttpStatusCode.MethodNotAllowed
            ) {
                println("↩️ _method=PUT not routed (${response.status}) — retrying as plain POST")
                response = client.submitFormWithBinaryData(url, parts(spoofPut = false)) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    header(HttpHeaders.Accept, ContentType.Application.Json.toString())
                }
            }

            val raw = runCatching { response.bodyAsText() }.getOrDefault("")
            println("📥 UPDATE POST ${response.status} → ${raw.take(300)}")


            ApiActionResult(
                success = response.status.isSuccess(),
                statusCode = response.status.value,
                message = ApiActionResult.reasonFrom(raw),
            )

        } catch (e: Exception) {
            println("❌ Update Post Error: ${e.message}")
            ApiActionResult(success = false, message = e.message)
        }
    }

    /** Delete a community post the signed-in user owns. */
    suspend fun deleteCommunityPost(postId: Int): ApiActionResult {
        val token = AppTokenProvider.getToken()
        val url = "${baseUrl}${deletePostPath(postId)}"

        println("🌐 API: Delete post $postId → $url")

        return try {
            val response = client.delete(url) {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }

            val raw = runCatching { response.bodyAsText() }.getOrDefault("")
            println("📥 DELETE POST ${response.status} → ${raw.take(300)}")

            ApiActionResult(
                success = response.status.isSuccess(),
                statusCode = response.status.value,
                message = ApiActionResult.reasonFrom(raw),
            )

        } catch (e: Exception) {
            println("❌ Delete Post Error: ${e.message}")
            ApiActionResult(success = false, message = e.message)
        }
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

    // ===============================================
    // SERVICE & WARRANTY APIs
    // ===============================================

    /**
     * POST service/complaints — files a service complaint.
     *
     * The server, not the app, decides whether money is owed: it answers either
     * `complaint_submitted: true` with the ticket, or `payment_required: true` with
     * the ₹ amount for a customer_type of "new". After that payment clears, the same
     * complaint is posted again with [merchantOrderId] filled in and the server
     * completes it.
     *
     * Multipart, because the invoice and problem photos go up with it.
     */
    suspend fun createServiceComplaint(
        customerType: String,
        /** Null when the account has no registered device; the server allows it. */
        deviceId: String?,
        componentId: String,
        /** `vehicle_type` — "EV Vehicle (2 wheeler)" or "Petrol (2 wheeler)". */
        vehicleType: String?,
        /** `problem` — the one-line fault the user typed on the form. */
        problem: String?,
        issue: String,
        errorCode: String?,
        invoice: ByteArray?,
        invoiceName: String?,
        photos: List<Pair<String, ByteArray>>,
        merchantOrderId: String? = null,
        phonepeOrderId: String? = null,
    ): ServiceComplaintPost {
        val token = AppTokenProvider.getToken()

        println(
            "🛠️ POST service/complaints → type=$customerType device=$deviceId " +
                "component=$componentId vehicle=$vehicleType problem=$problem payment=$merchantOrderId " +
                "auth=${token.tokenFingerprint()}"
        )

        val response = client.submitFormWithBinaryData(
            url = "${baseUrl}service/complaints",
            formData = formData {
                append("customer_type", customerType)
                deviceId?.takeIf { it.isNotBlank() }?.let { append("device_id", it) }
                append("component_id", componentId)
                // Fields of their own since the form stopped asking which component
                // is affected — the server keeps and echoes both.
                vehicleType?.takeIf { it.isNotBlank() }?.let { append("vehicle_type", it) }
                problem?.takeIf { it.isNotBlank() }?.let { append("problem", it) }
                append("issue", issue)
                errorCode?.takeIf { it.isNotBlank() }?.let { append("error_code", it) }

                // Present only on the second post, after the ₹299 has been paid.
                // Both spellings go up: every other PhonePe route in this app
                // (phonepe/callback, crminal-callback) takes camelCase, so a
                // snake_case-only body would be silently ignored if this endpoint
                // follows the same convention as its siblings.
                merchantOrderId?.let {
                    append("merchant_order_id", it)
                    append("merchantOrderId", it)
                    append("payment_reference", it)
                    append("payment_status", "SUCCESS")
                    append("status", "SUCCESS")
                }
                phonepeOrderId?.let {
                    append("phonepe_order_id", it)
                    append("phonepeOrderId", it)
                }

                if (invoice != null) {
                    append(
                        key = "invoice",
                        value = invoice,
                        headers = Headers.build {
                            append(HttpHeaders.ContentType, "image/jpeg")
                            append(
                                HttpHeaders.ContentDisposition,
                                "filename=${invoiceName ?: "invoice.jpg"}"
                            )
                        }
                    )
                }

                photos.forEachIndexed { index, (name, bytes) ->
                    append(
                        key = "photos[]",
                        value = bytes,
                        headers = Headers.build {
                            append(HttpHeaders.ContentType, "image/jpeg")
                            append(
                                HttpHeaders.ContentDisposition,
                                "filename=${name.ifBlank { "photo_$index.jpg" }}"
                            )
                        }
                    )
                }
            }
        ) {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
        }

        // Read before the 401 bails out: the body is the only thing that separates
        // "no token reached the server" from "this token is not accepted", and
        // throwing first left the console with nothing to tell them apart.
        val raw = response.bodyAsText()
        println("🛠️ SERVICE COMPLAINT (${response.status}) → $raw")

        if (response.status == HttpStatusCode.Unauthorized) throw UnauthorizedException()

        if (response.status.value in MISSING_ENDPOINT_CODES) {
            throw ServiceEndpointMissingException("service/complaints")
        }
        // 422 carries a JSON validation body worth surfacing, so only bail out on
        // statuses that arrive as an HTML error page.
        if (!response.status.isSuccess() && response.status.value >= 500) {
            throw IllegalStateException("Complaint submission failed: HTTP ${response.status.value}")
        }

        return ServiceComplaintPost(
            statusCode = response.status.value,
            body = Json { ignoreUnknownKeys = true }.parseToJsonElement(raw).jsonObject,
        )
    }

    /**
     * GET service/my-complaints — every complaint the signed-in user has raised.
     *
     * Each entry carries its component, invoice, photos[] and status_history, so
     * the list and the dashboard counters are built entirely from this response.
     */
    suspend fun getMyComplaints(): JsonObject {
        val token = AppTokenProvider.getToken()

        val response = client.get("${baseUrl}service/my-complaints") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
        }

        val raw = response.bodyAsText()
        println("🛠️ MY-COMPLAINTS (${response.status}) → ${raw.take(400)}")

        if (response.status == HttpStatusCode.Unauthorized) throw UnauthorizedException()
        if (response.status.value in MISSING_ENDPOINT_CODES) {
            throw ServiceEndpointMissingException("service/my-complaints")
        }
        if (!response.status.isSuccess()) {
            throw IllegalStateException("Complaint list failed: HTTP ${response.status.value}")
        }

        return Json { ignoreUnknownKeys = true }.parseToJsonElement(raw).jsonObject
    }

    /**
     * GET service/open-complaints — the complaints the server currently counts as
     * open. This is the authority on that: the rows carry a `status` of their own
     * ("pending"), but which bucket they belong to is what the endpoint itself
     * says, so the app reads membership rather than re-deriving it.
     *
     * Laravel pagination, so the rows are at `data.data[]` and [page] walks them.
     * The shape is lighter than my-complaints — a `component_id` but no component
     * object, and no invoice, photos or status_history — which is why this is used
     * to update statuses and never to build a ticket from scratch.
     */
    suspend fun getOpenComplaints(page: Int = 1): JsonObject {
        val token = AppTokenProvider.getToken()

        val response = client.get("${baseUrl}service/open-complaints") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            parameter("page", page)
        }

        val raw = response.bodyAsText()
        println("🛠️ OPEN-COMPLAINTS p$page (${response.status}) → ${raw.take(400)}")

        if (response.status == HttpStatusCode.Unauthorized) throw UnauthorizedException()
        if (response.status.value in MISSING_ENDPOINT_CODES) {
            throw ServiceEndpointMissingException("service/open-complaints")
        }
        if (!response.status.isSuccess()) {
            throw IllegalStateException("Open complaints failed: HTTP ${response.status.value}")
        }

        return Json { ignoreUnknownKeys = true }.parseToJsonElement(raw).jsonObject
    }

    /**
     * GET service/my-complaints/{id} — one complaint in full.
     *
     * [id] is the numeric row id, not the SR-… ticket number. Beyond the list
     * payload this adds `device`, `remarks[]` and `documents[]`, and names the
     * photo array `media` rather than `photos`.
     */
    suspend fun getComplaintDetail(id: String): JsonObject {
        val token = AppTokenProvider.getToken()

        val response = client.get("${baseUrl}service/my-complaints/$id") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
        }

        val raw = response.bodyAsText()
        println("🛠️ COMPLAINT $id (${response.status}) → ${raw.take(400)}")

        if (response.status == HttpStatusCode.Unauthorized) throw UnauthorizedException()
        if (response.status.value in MISSING_ENDPOINT_CODES) {
            throw ServiceEndpointMissingException("service/my-complaints/$id")
        }
        if (!response.status.isSuccess()) {
            throw IllegalStateException("Complaint detail failed: HTTP ${response.status.value}")
        }

        return Json { ignoreUnknownKeys = true }.parseToJsonElement(raw).jsonObject
    }

    /**
     * POST service/create-order — the one-time service charge a customer who is
     * not on EVA2Z Connect pays before a complaint can be filed.
     *
     * The route is not deployed yet, so this throws [ServiceEndpointMissingException]
     * on a 404 and [buildServicePaymentOrder] retries against the criminal-verification
     * order endpoint, which is live. Once the real route ships the fallback stops
     * being reached on its own — no app change needed.
     */
    suspend fun createServiceOrder(amount: Int): JsonObject {
        val token = AppTokenProvider.getToken()

        val response = client.post("${baseUrl}service/create-order") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(mapOf("amount" to amount))
        }

        val raw = response.bodyAsText()
        println("💳 SERVICE CREATE-ORDER (${response.status}) → $raw")

        if (response.status == HttpStatusCode.Unauthorized) throw UnauthorizedException()
        if (response.status.value in MISSING_ENDPOINT_CODES) {
            throw ServiceEndpointMissingException("service/create-order")
        }
        // A 5xx arrives as an HTML error page, so check before handing it to the parser.
        if (!response.status.isSuccess()) {
            throw IllegalStateException("Service order failed: HTTP ${response.status.value}")
        }

        return Json { ignoreUnknownKeys = true }.parseToJsonElement(raw).jsonObject
    }

    /**
     * POST service/payment-callback — tells the server the service charge was paid.
     *
     * Like every other PhonePe flow in the app this is what actually moves the
     * order out of PENDING; the SDK's own success result proves nothing.
     *
     * Both spellings of each id go up. The route answered 500 — not the 422 a
     * missing field would produce — to a camelCase-only body on 2026-09-22, which
     * is what a handler reading `merchant_order_id`, finding null and then
     * dereferencing the order it failed to look up would do. Its sibling
     * `phonepe/callback` takes camelCase, so neither convention can be assumed.
     */
    suspend fun sendServicePaymentCallback(
        merchantOrderId: String,
        phonepeOrderId: String,
        status: String
    ): Boolean {
        val token = AppTokenProvider.getToken()

        println("📡 Sending service payment callback → $merchantOrderId / $status")

        val response = client.post("${baseUrl}service/payment-callback") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(
                mapOf(
                    "merchantOrderId" to merchantOrderId,
                    "merchant_order_id" to merchantOrderId,
                    "phonepeOrderId" to phonepeOrderId,
                    "phonepe_order_id" to phonepeOrderId,
                    "status" to status,
                    "payment_status" to status,
                )
            )
        }

        val raw = runCatching { response.bodyAsText() }.getOrDefault("")
        println("📡 Service callback status = ${response.status} → ${raw.take(300)}")

        if (response.status.value in MISSING_ENDPOINT_CODES) {
            throw ServiceEndpointMissingException("service/payment-callback")
        }

        return response.status.isSuccess()
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

        val body = response.bodyAsText()
        println("📡 FCM API STATUS: ${response.status}")
        println("📡 FCM API BODY: $body")

        // The client is not configured with expectSuccess, so a rejected registration used to
        // fall through and be logged as a success by the caller. Fail loudly instead: a
        // non-2xx here means this device is not on the push list.
        if (!response.status.isSuccess()) {
            throw Exception("fcm/register rejected (${response.status}): ${body.take(200)}")
        }
    }

    // Removes this device from the push list.
    //
    // Deliberately usable without a live session: the session-expiry logout path has nothing
    // but an already-rejected token, so a deregistration that needs valid auth can never run
    // on the exact path where the device most needs removing. device_uuid + fcm_token identify
    // the row on their own — the server must accept this pair the same way fcm/register does.
    suspend fun deregisterFcmToken(
        fcmToken:   String,
        deviceUuid: String,
        platform:   String = "android",
        authHeader: String? = null
    ) {
        val response = client.post("${baseUrl}fcm/deregister") {
            headers {
                authHeader?.let { append("Authorization", it) }
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

        println("📡 FCM DEREGISTER STATUS: ${response.status}")
        println("📡 FCM DEREGISTER BODY: ${response.bodyAsText()}")
    }

    // ---------------- LOGOUT ----------------
    // Mirrors registerFcmToken()'s payload — the server needs device_uuid to delete the
    // exact row it created at register time. Sending fcm_token alone left stale rows behind.
    suspend fun logout(
        authHeader: String,
        fcmToken:   String,
        deviceUuid: String = "",
        platform:   String = ""
    ) {
        println("🚪 API: Calling logout... fcmToken=${fcmToken.take(20)} deviceUuid=$deviceUuid")
        val body = buildMap {
            put("fcm_token", fcmToken)
            if (deviceUuid.isNotBlank()) put("device_uuid", deviceUuid)
            if (platform.isNotBlank()) put("platform", platform)
        }
        val response = client.post("${baseUrl}logout") {
            header(HttpHeaders.Authorization, authHeader)
            contentType(ContentType.Application.Json)
            setBody(body)
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

            // The server returns either a bare array (older builds) or a
            // wrapper `{"success":true,"data":[...]}`. Decoding the wrapper as
            // a list throws, and the catch below turns that into an empty
            // list — i.e. a vault full of documents shows up as empty.
            val json = Json { ignoreUnknownKeys = true }
            val element = json.parseToJsonElement(raw)
            val array = when (element) {
                is JsonArray  -> element
                is JsonObject -> element["data"] as? JsonArray
                    ?: throw Exception("Vault list has no 'data' array: $raw")
                else          -> throw Exception("Unexpected vault list payload: $raw")
            }

            json.decodeFromJsonElement(
                ListSerializer(VaultDocumentDto.serializer()),
                array
            )

        } catch (e: Exception) {
            println("❌ Vault Documents Error: ${e.message}")
            emptyList()
        }
    }


    suspend fun getVaultDocumentDetail(
        documentId: String,
        vaultToken: String,
        authToken: String
    ): VaultDocumentDetail? {

        val url = "${baseUrl}document/$documentId"

        println("🌐 API: Fetching document detail $documentId")

        return try {
            val response = client.get(url) {
                header("X-Vault-Token", vaultToken)
                header(HttpHeaders.Authorization, "Bearer $authToken")
                header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }

            val raw = response.bodyAsText()
            println("📥 document/$documentId ${response.status}")

            if (!response.status.isSuccess()) return null

            kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                .decodeFromString(VaultDocumentDetailResponse.serializer(), raw)
                .data

        } catch (e: Exception) {
            println("❌ document detail error: ${e.message}")
            null
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
            // Laravel answers this route with a full stack trace; log only the message so a
            // routine 404 (the server no longer stores the encrypted blob — callers fall back
            // to `image_url`) doesn't dump hundreds of lines into the console.
            val errorBody = try { response.bodyAsText() } catch (e: Exception) { "<no body>" }
            val summary = errorBody.take(200)
            println("❌ vault/$documentId ${response.status} → $summary")
            throw Exception("Failed to fetch document (${response.status}): $summary")
        }

        return response.body()
    }

    suspend fun deleteVaultDocument(
        documentId: String,
        vaultToken: String,
        authToken: String
    ) {
        // The server exposes two delete routes. vault/{id} is the sibling of the
        // working file fetch and resolves ids the same way; vault/document/{id}
        // uses route-model binding that 404s on these ids. Prefer the first and
        // keep the legacy path as a fallback.
        suspend fun attempt(url: String): HttpResponse =
            client.delete(url) {
                header("X-Vault-Token", vaultToken)
                header(HttpHeaders.Authorization, "Bearer $authToken")
            }

        var response = attempt("${baseUrl}vault/$documentId")
        println("🗑️ vault delete $documentId → ${response.status}")

        if (response.status == HttpStatusCode.NotFound ||
            response.status == HttpStatusCode.MethodNotAllowed
        ) {
            response = attempt("${baseUrl}vault/document/$documentId")
            println("🗑️ vault/document delete $documentId → ${response.status}")
        }

        if (!response.status.isSuccess()) {
            val errorBody = try { response.bodyAsText() } catch (e: Exception) { "<no body>" }
            println("❌ vault delete failed → $errorBody")
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
    ): VaultUploadResponse {
        logVaultPayload("upload", encryptedFile, fileName, fileType)
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

        return parseVaultUploadResponse(response)
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
    ): VaultUploadResponse {
        logVaultPayload("update/$documentId", encryptedFile, fileName, fileType)
        val response = client.submitFormWithBinaryData(
            url = "${baseUrl}vault/update/$documentId",
            formData = formData {

                encryptedFile?.let {
                    append("encrypted_file", it, Headers.build {
                        append(HttpHeaders.ContentType, fileType)
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName.jpg\"")
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

        return parseVaultUploadResponse(response)
    }


    /**
     * Logs what is actually being sent to the vault so a server-side
     * `document_rejected` can be told apart from the app shipping a broken or
     * mislabelled file (e.g. HEIC/PNG bytes declared as JPEG).
     */
    private fun logVaultPayload(
        op: String,
        bytes: ByteArray?,
        fileName: String,
        fileType: String
    ) {
        if (bytes == null) {
            println("📎 vault $op payload: <no file> name='$fileName' type=$fileType")
            return
        }
        val magic = bytes.take(12)
            .joinToString(" ") { it.toInt().and(0xFF).toString(16).padStart(2, '0') }
        val format = when {
            bytes.size < 4                                      -> "EMPTY/TRUNCATED"
            bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> "JPEG"
            bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() -> "PNG"
            bytes[0] == 0x25.toByte() && bytes[1] == 0x50.toByte() -> "PDF"
            bytes.size > 11 && bytes.copyOfRange(4, 12)
                .decodeToString().startsWith("ftyp")            -> "HEIC/MP4-container"
            else                                                -> "UNKNOWN"
        }
        println(
            "📎 vault $op payload: ${bytes.size} bytes · detected=$format · " +
                "declared=$fileType · name='$fileName' · magic=$magic"
        )
    }

    private suspend fun parseVaultUploadResponse(response: HttpResponse): VaultUploadResponse {
        val raw = response.bodyAsText()
        println("📤 vault upload/update ${response.status}: $raw")

        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        return try {
            json.decodeFromString(VaultUploadResponse.serializer(), raw)
        } catch (e: Exception) {
            println("❌ Could not parse vault upload response: ${e.message}")
            if (response.status.isSuccess()) {
                VaultUploadResponse(success = true)
            } else {
                VaultUploadResponse(
                    success = false,
                    message = "Upload failed (${response.status.value}). Please try again."
                )
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

    // ===============================================
    // SERVICE & WARRANTY — responses turned into models
    //
    // The raw calls are above; these wrap them so the screens get domain objects
    // instead of JsonObjects. Everything the flow needs lives here — there is no
    // repository layer, and no URL outside [KtorClient.BASE_URL].
    // ===============================================

    /**
     * The devices the complaint can be raised against. An empty list is normal —
     * the complaint still files, just without a `device_id`.
     */
    suspend fun fetchServiceVehicles(): List<ServiceVehicle> {
        val vehicles = getUserDevices().data.map { device ->
            val name = device.device_name?.takeIf { it.isNotBlank() }
            ServiceVehicle(
                vehicleId = device.device_id.toString(),
                registrationNumber = name ?: device.device_number,
                // The device API carries no vehicle model; the IMEI-bearing tracker
                // is what is actually under warranty, so label it as such.
                model = "EVA2Z GPS Tracker",
                vin = device.device_number,
                purchaseDate = device.warranty_start,
                warrantyExpiry = device.warranty_expiry,
                warrantyActive = device.warranty_status == "active",
            )
        }
        println("🛵 get-user-devices → ${vehicles.size} device(s)")
        return vehicles
    }

    /**
     * The signed-in user's complaints, newest first.
     *
     * `service/open-complaints` used to be read here to move tickets into the Open
     * bucket; it was dropped on the backend team's instruction (2026-09-22), so the
     * status `my-complaints` gives is now the only one. [fetchOpenComplaintKeys]
     * still exists for whoever wants that bucket back, but nothing calls it.
     *
     * [vehicles] only names the device a ticket was raised against; pass an empty
     * list when the caller does not have them and the ticket still reads fine.
     */
    suspend fun fetchServiceComplaints(
        vehicles: List<ServiceVehicle> = emptyList(),
    ): List<ServiceTicket> {
        val payload = getMyComplaints()
        val rows = payload["data"]?.let { runCatching { it.jsonArray }.getOrNull() }
            ?: run {
                println("⚠️ my-complaints returned no data array")
                return emptyList()
            }

        val tickets = rows.mapNotNull { row ->
            runCatching { row.jsonObject.toServiceTicket(vehicles) }
                .onFailure { println("⚠️ Skipped an unreadable complaint row: ${it.message}") }
                .getOrNull()
        }
        println("🛠️ my-complaints → ${tickets.size} ticket(s)")

        return tickets.sortedByDescending { it.lastUpdatedMillis }
    }

    /**
     * Every ticket number and row id `service/open-complaints` lists, following the
     * Laravel paginator. Capped at [MAX_OPEN_PAGES] so a paginator that never
     * reports a last page cannot spin; what was dropped is logged rather than
     * passed off as the whole set.
     */
    suspend fun fetchOpenComplaintKeys(): Set<String> {
        val keys = mutableSetOf<String>()
        var page = 1

        while (page <= MAX_OPEN_PAGES) {
            val payload = getOpenComplaints(page)
            val body = payload["data"]?.let { runCatching { it.jsonObject }.getOrNull() }

            // Paginated ({data: {data: [...]}}) is the shape today; a bare array is
            // accepted too so an unpaginated build of the route still works.
            val rows = body?.get("data")?.let { runCatching { it.jsonArray }.getOrNull() }
                ?: payload["data"]?.let { runCatching { it.jsonArray }.getOrNull() }
                ?: break

            rows.forEach { element ->
                val row = runCatching { element.jsonObject }.getOrNull() ?: return@forEach
                row.text("ticket_number", "ticket_no")?.let { keys += it }
                row.text("id")?.let { keys += it }
            }

            val lastPage = body?.number("last_page") ?: page
            if (page >= lastPage) {
                println("🛠️ open-complaints → ${keys.size} key(s) across $page page(s)")
                return keys
            }
            page++
        }

        println("⚠️ open-complaints stopped at the $MAX_OPEN_PAGES-page cap — later pages were not read.")
        return keys
    }

    /**
     * One complaint in full. `service/my-complaints/{id}` is keyed by the numeric
     * row id, which only the list carries, so the list is read first unless the
     * caller already knows the row ([known]).
     */
    suspend fun fetchServiceComplaint(
        ticketId: String,
        vehicles: List<ServiceVehicle> = emptyList(),
        known: ServiceTicket? = null,
    ): ServiceTicket? {
        val listed = known
            ?: runCatching { fetchServiceComplaints(vehicles) }
                .getOrElse { e ->
                    println("⚠️ Couldn't read the list for $ticketId: ${e.message}")
                    emptyList()
                }
                .firstOrNull { it.ticketId == ticketId }

        val rowId = listed?.serverId ?: ticketId.takeIf { id -> id.all { it.isDigit() } }
        if (rowId == null) {
            println("⚠️ No numeric id for $ticketId — showing the listed copy.")
            return listed
        }

        val fetched = runCatching {
            getComplaintDetail(rowId)["data"]
                ?.let { it.jsonObject }
                ?.toServiceTicket(vehicles)
        }.getOrElse { e ->
            println("❌ Complaint $rowId detail failed: ${e.message}")
            null
        } ?: return listed

        // The detail response has no idea about the open bucket, so the status the
        // list already resolved wins — the two screens must not disagree.
        val merged = listed?.let { fetched.enrichedWith(it).copy(status = it.status) } ?: fetched
        return merged
    }

    /** The serviceable components. There is no components endpoint; this is local. */
    fun serviceComponents(): List<ServiceComponentItem> = ServiceComponentCatalog

    /**
     * Files the complaint. [payment] is non-null only on the second attempt, after
     * the service charge the server asked for has been paid and confirmed.
     */
    @OptIn(ExperimentalTime::class)
    suspend fun submitServiceComplaint(
        draft: ComplaintDraft,
        payment: ServicePaymentReceipt? = null,
    ): ComplaintSubmission {
        // No device on the account is not a blocker — the server files the complaint
        // with a null device_id — so a stand-in stands in for display only.
        val vehicle = draft.vehicle ?: UnlinkedVehicle
        val component = draft.component ?: ServiceProblemComponent
        val customerType = draft.customerType ?: ServiceCustomerType.EVA2Z_CUSTOMER

        suspend fun postAs(vehicleType: String) = createServiceComplaint(
            customerType = customerType.apiValue,
            // The server keys complaints by the tracker's IMEI, which is what
            // get-user-devices calls device_number and this app calls the VIN.
            deviceId = vehicle.vin.ifBlank { vehicle.vehicleId }.takeIf { it.isNotBlank() },
            componentId = component.apiId,
            vehicleType = vehicleType,
            problem = draft.problem.trim(),
            // The description on its own — the problem and the vehicle type have
            // their own fields now. See ComplaintDraft.issueText.
            issue = draft.issueText,
            errorCode = draft.errorCode.trim().takeIf { it.isNotBlank() },
            invoice = draft.invoice?.bytes,
            invoiceName = draft.invoice?.name,
            photos = draft.media.mapNotNull { media -> media.bytes?.let { media.name to it } },
            merchantOrderId = payment?.merchantOrderId,
            // Both references go up: the server has been seen to ignore a lone
            // merchant_order_id and answer with a fresh charge instead.
            phonepeOrderId = payment?.phonepeOrderId,
        )

        // The server's `vehicle_type` vocabulary is not settled (see
        // ServiceVehicleType.apiValues): the spelling it accepted at 12:53 on
        // 2026-09-26 was refused by an `in:` rule at 13:09. A refusal of that field
        // alone costs nothing — the complaint is not created and no money is involved
        // — so the remaining spellings are tried before giving up on the user.
        suspend fun post(): ServiceComplaintPost {
            // The form cannot be submitted without a vehicle type — ComplaintDraft
            // asks for it first — so this stands in only for the type system.
            val vehicleType = draft.vehicleType ?: ServiceVehicleType.EV_TWO_WHEELER
            var refused: ServiceComplaintPost? = null
            for (value in vehicleType.apiValues) {
                val attempt = postAs(value)
                if (!attempt.vehicleTypeRefused) {
                    if (refused != null) {
                        println("✅ service/complaints accepted vehicle_type=\"$value\" — pin this one.")
                    }
                    return attempt
                }
                println("⚠️ service/complaints refused vehicle_type=\"$value\": ${attempt.validationMessage}")
                refused = attempt
            }
            println(
                "❌ Every vehicle_type spelling this app knows was refused " +
                    "(${vehicleType.apiValues.joinToString()}). The backend's `in:` " +
                    "list is something else — ask for it."
            )
            return refused ?: postAs(vehicleType.apiValue)
        }

        // A dropped upload ("unexpected end of stream" — the server closing a
        // pooled connection mid-multipart) produces no response at all, so whether
        // the complaint was filed is genuinely unknown. Blindly re-posting risks a
        // duplicate ticket, so the list endpoint is asked first and the retry only
        // happens once nothing matching is found there.
        val response = try {
            post()
        } catch (first: Exception) {
            println("⚠️ service/complaints failed (${first.describe()}) — checking whether it landed.")
            alreadyFiled(draft, component)?.let { return ComplaintSubmission.Created(it) }

            try {
                post()
            } catch (second: Exception) {
                println("⚠️ Retry also failed (${second.describe()}) — checking once more.")
                alreadyFiled(draft, component)?.let { return ComplaintSubmission.Created(it) }
                throw second
            }
        }

        val payload = response.body

        // A refused request is a refusal, whatever the body looks like. Checked before
        // anything is read out of it: a 422 carries no `success` and no
        // `payment_required`, so reading it as a payload announced a ticket for a
        // complaint that was never filed.
        if (!response.isSuccess) {
            val complaint = response.validationMessage
            println("❌ service/complaints refused the complaint (HTTP ${response.statusCode}): $complaint")
            return ComplaintSubmission.Rejected(
                complaint ?: "Your complaint could not be submitted. Please try again."
            )
        }

        val data = payload["data"]?.jsonObject
        val succeeded = payload["success"]?.jsonPrimitive?.booleanOrNull
            ?: payload["status"]?.jsonPrimitive?.booleanOrNull
            ?: true

        // The server owns this decision. Only `complaint_submitted: true` means a
        // ticket exists — a `success: true` alongside `payment_required` does not.
        val paymentRequired = data?.get("payment_required")?.jsonPrimitive?.booleanOrNull == true
        val complaintSubmitted = data?.get("complaint_submitted")?.jsonPrimitive?.booleanOrNull == true

        if (!succeeded && !paymentRequired) {
            return ComplaintSubmission.Rejected(
                payload.text("message") ?: "Your complaint could not be submitted."
            )
        }

        if (paymentRequired && !complaintSubmitted) {
            // Already paid, and the server is still answering payment_required. The
            // ticket may exist anyway — `phonepe/webhook` is what files it, and it
            // is called before this re-post. Ask the list before posting again:
            // every re-post mints a fresh PENDING order on some builds of this
            // route, so a blind retry leaves a trail of orphan charges behind.
            if (payment != null) {
                alreadyFiled(draft, component)?.let {
                    println("✅ ${it.ticketId} exists — the webhook filed it; not re-posting.")
                    return ComplaintSubmission.Created(it)
                }

                // Said plainly because it is the one fact that settles whose bug this
                // is: a different order id back means the server did not look at the
                // reference it was sent, and minted another PENDING order instead.
                val echoed = data?.text("merchant_order_id", "merchantOrderId")
                if (echoed != null && echoed != payment.merchantOrderId) {
                    println(
                        "❌ Sent merchant_order_id=${payment.merchantOrderId} " +
                            "(paid, settled via phonepe/webhook) but service/complaints " +
                            "answered with a NEW order $echoed — the payment reference was ignored."
                    )
                }
            }

            // What the server quoted, and what is actually charged. They differ on
            // the live route, which still answers `service_charge: 1` — a test
            // figure, not a price. Opening PhonePe for it takes ₹1 off the user and
            // leaves the real fee uncollected, so the published ₹299 is the floor:
            // the server can quote more and that stands, it just cannot quote less.
            val quoted = data?.number("service_charge", "amount")
            val charge = quoted?.coerceAtLeast(SERVICE_FEE_RUPEES) ?: SERVICE_FEE_RUPEES
            if (quoted != null && quoted != charge) {
                println("⚠️ service/complaints quoted ₹$quoted — charging the published ₹$charge instead.")
            }

            // The complaint itself, when this answer carries one. Since 2026-09-26 the
            // first post files the ticket and asks for the charge in the same breath
            // ("Complaint created. Please complete ₹299 payment." with
            // ticket_number SR-2026-000042), so posting again would file a duplicate.
            val filedTicket = data
                ?.takeIf { it.text("ticket_number", "ticket_no") != null }
                ?.toSubmittedTicket(
                    vehicle = vehicle,
                    component = component,
                    draft = draft,
                    customerType = customerType,
                    payment = payment,
                )
            if (filedTicket != null) {
                println(
                    "🧾 ${filedTicket.ticketId} was filed with the charge — " +
                        "₹$charge outstanding, do not re-post this complaint."
                )
            }

            return ComplaintSubmission.PaymentRequired(
                amountRupees = charge,
                buttonText = data?.text("button_text"),
                ticket = filedTicket,
                // "payment_required with payment_status SUCCESS" is the server saying
                // it has the money but has not filed the ticket yet — not a new charge.
                settled = data?.text("payment_status")?.serviceSlug() == "success",
                // The checkout may ride along with the amount; when it doesn't,
                // the caller falls back to createServicePaymentOrder().
                //
                // It is only usable when it was minted for the figure being charged:
                // the token resolves to the server's own order, so a ₹1 order opens
                // PhonePe at ₹1 no matter what this response is labelled. When the
                // quote was overridden above, the order is dropped and the caller
                // creates a ₹$charge one.
                order = payload.readPaymentOrder(charge)?.takeIf { quoted == charge },
            )
        }

        return ComplaintSubmission.Created(
            (data ?: payload).toSubmittedTicket(
                vehicle = vehicle,
                component = component,
                draft = draft,
                customerType = customerType,
                payment = payment,
            )
        )
    }

    /**
     * Whether the complaint being submitted is already on the server — asked after
     * a POST died without a response.
     *
     * Matched on problem + issue text within [DUPLICATE_WINDOW_MS]; the window is
     * what keeps a genuine repeat complaint about the same fault from being
     * mistaken for the one that just failed.
     *
     * The component is no longer part of the match: every complaint is filed against
     * the server's "Other", so it cannot tell two tickets apart. The typed problem —
     * `problem` on the row, [ServiceTicket.problemLabel] here — is what can.
     */
    @OptIn(ExperimentalTime::class)
    private suspend fun alreadyFiled(
        draft: ComplaintDraft,
        component: ServiceComponentItem,
    ): ServiceTicket? {
        val issue = draft.issueText
        if (issue.isEmpty()) return null
        val problem = draft.problem.trim().ifEmpty { component.label }

        val now = Clock.System.now().toEpochMilliseconds()
        val match = runCatching {
            fetchServiceComplaints().firstOrNull { ticket ->
                ticket.issueDescription.trim().equals(issue, ignoreCase = true) &&
                    now - ticket.createdAtMillis <= DUPLICATE_WINDOW_MS &&
                    // The problem only narrows the match when the row actually carries
                    // one: rows the server files for itself come back with
                    // `problem: null` (seen on SR-2026-000030), and insisting on it
                    // there would miss the very ticket being looked for and tell a
                    // paid user nothing was filed.
                    (ticket.problem.isNullOrBlank() ||
                        ticket.problemLabel.equals(problem, ignoreCase = true))
            }
        }.getOrElse { e ->
            println("⚠️ Couldn't re-read my-complaints after the failure: ${e.message}")
            null
        }

        if (match != null) println("✅ ${match.ticketId} was filed after all — not re-posting.")
        return match
    }

    /** Opens a PhonePe checkout for the service charge the server asked for. */
    suspend fun buildServicePaymentOrder(amountRupees: Int): ServicePaymentOrder {
        val (payload, viaLegacy) = try {
            createServiceOrder(amountRupees) to false
        } catch (e: ServiceEndpointMissingException) {
            // service/create-order is not deployed yet. crminal-create-order takes the
            // same {amount} body and returns the same PhonePe order, so the ₹299
            // checkout still completes — it is just booked against the wrong ledger
            // until the real route lands.
            println("⚠️ ${e.message} Falling back to criminal-create-order.")
            createCriminalOrder(amountRupees) to true
        }

        val merchantOrderId = payload.findStringDeep("merchantOrderId", "merchant_order_id")
            ?: throw IllegalStateException(
                "Merchant order id missing in order response (keys: ${payload.keys.joinToString()})"
            )
        val token = payload.findStringDeep("token", "orderToken", "order_token")
            ?: throw IllegalStateException(
                "Payment token missing in order response (keys: ${payload.keys.joinToString()})"
            )
        // PhonePe resolves the token against its own OMO… id. A bare `order_id` is
        // deliberately not searched — that is our numeric DB row.
        val phonepeOrderId = payload.findStringDeep("phonepeOrderId", "phonepe_order_id")
            ?: payload.findStringDeep("orderId")
            ?: merchantOrderId

        println("💳 Service fee order → merchant=$merchantOrderId, phonepe=$phonepeOrderId, legacy=$viaLegacy")

        return ServicePaymentOrder(
            merchantOrderId = merchantOrderId,
            phonepeOrderId = phonepeOrderId,
            token = token,
            amountRupees = amountRupees,
            viaLegacyEndpoint = viaLegacy,
        )
    }

    /**
     * Confirms a finished checkout. Returns false when the server would not confirm
     * it — the payment must then be treated as unconfirmed, never as paid.
     */
    suspend fun confirmServicePayment(
        order: ServicePaymentOrder,
        phonepeOrderId: String?,
    ): Boolean {
        val txnId = phonepeOrderId?.takeIf { it.isNotBlank() } ?: order.phonepeOrderId

        // The callback is confirmed against whichever endpoint created the order —
        // the criminal ledger cannot look up an order it never issued. Retried
        // because losing this call strands money the user has already spent.
        repeat(3) { attempt ->
            val confirmed = runCatching {
                if (order.viaLegacyEndpoint) {
                    sendCriminalPaymentCallback(order.merchantOrderId, txnId, "SUCCESS")
                } else {
                    sendServicePaymentCallback(order.merchantOrderId, txnId, "SUCCESS")
                }
            }.getOrElse { e ->
                println("❌ Service payment callback attempt ${attempt + 1} failed: ${e.message}")
                false
            }

            if (confirmed) {
                println("✅ Service charge confirmed with server (${order.merchantOrderId})")
                return true
            }
            delay(700L * (attempt + 1))
        }
        return false
    }

    /**
     * Last-resort settle attempt down the app's shared `phonepe/callback` route —
     * the one the Shop checkout uses and which is known to work there. Returns
     * false when the route will not take a SERVICE-… order, which is not an error.
     */
    suspend fun confirmServicePaymentViaSharedCallback(receipt: ServicePaymentReceipt): Boolean =
        runCatching {
            sendPhonePeCallback(
                PhonePeCallbackRequest(
                    merchantOrderId = receipt.merchantOrderId,
                    phonepeOrderId = receipt.phonepeOrderId ?: receipt.merchantOrderId,
                    status = "SUCCESS",
                )
            )
        }.getOrElse { e ->
            println("⚠️ Shared phonepe/callback wouldn't take ${receipt.merchantOrderId}: ${e.message}")
            false
        }

    /**
     * Tells the server the service charge was paid, down every route that will
     * take a SERVICE-… order.
     *
     * `phonepe/webhook` is the one that matters — it is what creates the ticket,
     * and PhonePe's own delivery of it is evidently not arriving. The other two
     * only settle the order: `service/payment-callback` (500s as of 2026-09-22)
     * and the shared `phonepe/callback` the Shop checkout uses. All three are
     * tried because a route refusing this order is not an error, and the webhook
     * is sent last so the order is already settled when it lands.
     */
    suspend fun settleServicePayment(receipt: ServicePaymentReceipt): Boolean {
        val txnId = receipt.phonepeOrderId?.takeIf { it.isNotBlank() } ?: receipt.merchantOrderId

        val viaService = runCatching {
            sendServicePaymentCallback(receipt.merchantOrderId, txnId, "SUCCESS")
        }.getOrElse { e ->
            println("⚠️ service/payment-callback wouldn't take ${receipt.merchantOrderId}: ${e.message}")
            false
        }

        val viaShared = confirmServicePaymentViaSharedCallback(receipt)

        val viaWebhook = runCatching {
            sendPhonePeWebhook(receipt.merchantOrderId, receipt.phonepeOrderId, "SUCCESS")
        }.getOrElse { e ->
            println("⚠️ phonepe/webhook wouldn't take ${receipt.merchantOrderId}: ${e.message}")
            false
        }

        println(
            "💳 ${receipt.merchantOrderId} settle → service=$viaService " +
                "shared=$viaShared webhook=$viaWebhook"
        )
        return viaService || viaShared || viaWebhook
    }
}

/* ════════════════ SERVICE & WARRANTY — JSON → domain ══════════════════════ *
 *
 * Parsing for the service endpoints, kept beside the calls that produce it so
 * the whole flow reads top to bottom in this one file.
 */

/** How recent a server-side ticket has to be to count as the one whose upload died. */
private const val DUPLICATE_WINDOW_MS = 5 * 60 * 1000L

/** How many pages of `service/open-complaints` are read before giving up. */
private const val MAX_OPEN_PAGES = 5

/**
 * Attachment paths are host-relative, so they are resolved against the API host —
 * [KtorClient.BASE_URL] minus its "/api/" suffix.
 *
 * Two shapes arrive: `file_url` already carries the "/storage" prefix Laravel
 * serves the disk under, while the `file_path` on the complaint endpoints is the
 * bare disk path ("service/invoices/x.pdf") and has to be given one.
 */
private val SERVICE_FILE_HOST = KtorClient.BASE_URL.removeSuffix("/").removeSuffix("/api")

private fun String.asFileUrl(): String {
    if (startsWith("http://") || startsWith("https://")) return this
    val path = trimStart('/')
    return if (path.startsWith("storage/")) "$SERVICE_FILE_HOST/$path"
    else "$SERVICE_FILE_HOST/storage/$path"
}

/**
 * Fills the gaps the server cannot fill from the copy the caller already holds:
 * the attachment bytes the user picked and the PhonePe receipt, which appears
 * nowhere in the complaint payload. Everything else stays as the server sent it.
 */
private fun ServiceTicket.enrichedWith(local: ServiceTicket): ServiceTicket {
    val withBytes = attachments.map { remote ->
        if (remote.bytes != null) return@map remote
        val match = local.attachments.firstOrNull { it.kind == remote.kind && it.name == remote.name }
            ?: local.attachments.filter { it.kind == remote.kind }
                .getOrNull(attachments.filter { it.kind == remote.kind }.indexOf(remote))
        match?.bytes?.let { remote.copy(bytes = it) } ?: remote
    }

    return copy(
        attachments = withBytes.ifEmpty { local.attachments },
        payment = payment ?: local.payment,
        paymentPaidAtMillis = paymentPaidAtMillis ?: local.paymentPaidAtMillis,
        paymentReference = paymentReference ?: local.paymentReference,
        customerType = local.customerType,
        allocation = allocation ?: local.allocation,
    )
}

/**
 * Moves a ticket to [TicketStatus.OPEN] when `service/open-complaints` named it.
 *
 * A ticket that has already reached In progress or Completed is left alone: the
 * open list lags behind those, and demoting a finished complaint back to Open
 * would read as the work being undone.
 */
private fun ServiceTicket.withOpenStatusApplied(openKeys: Set<String>): ServiceTicket {
    if (openKeys.isEmpty() || status == TicketStatus.OPEN) return this
    if (status == TicketStatus.IN_PROGRESS || status == TicketStatus.COMPLETED) return this

    val named = ticketId in openKeys || serverId?.let { it in openKeys } == true
    return if (named) copy(status = TicketStatus.OPEN) else this
}

/**
 * One `invoice` / `photos[]` entry. [local] is the copy the user picked, matched
 * positionally, and is only used for its bytes — every other field comes from the
 * server, which is why a PDF invoice keeps its real name and mime type instead of
 * the "invoice.jpg" we uploaded it as.
 */
private fun JsonObject.toStoredAttachment(
    kind: AttachmentKind,
    local: ServiceAttachment?,
): ServiceAttachment? {
    val url = text("file_url", "url", "file_path")?.asFileUrl() ?: return null

    return ServiceAttachment(
        id = text("id")?.let { "${kind.name.lowercase()}-$it" } ?: local?.id ?: url,
        // The complaint endpoints send no filename, so the last path segment is
        // the only honest label available — "invoice.pdf" beats "Attachment".
        name = text("original_name", "name", "file_name")
            ?: local?.name
            ?: url.substringAfterLast('/').takeIf { it.isNotBlank() }
            ?: "Attachment",
        sizeBytes = number("size", "file_size")?.toLong() ?: local?.sizeBytes ?: 0L,
        kind = kind,
        bytes = local?.bytes,
        url = url,
        mimeType = text("mime_type", "mime") ?: text("file_type"),
    )
}

/**
 * One row of `GET service/my-complaints`, or the `data` object of
 * `GET service/my-complaints/{id}`.
 *
 * The two shapes are the same bar three things, all handled here: the detail
 * names its photo array `media` where the list calls it `photos`, the detail
 * nests the full `device` the list reduces to a `device_id`, and the detail adds
 * `remarks` / `documents`.
 */
@OptIn(ExperimentalTime::class)
private fun JsonObject.toServiceTicket(vehicles: List<ServiceVehicle>): ServiceTicket {
    val rowId = text("id")
    val ticketId = text("ticket_number", "ticket_no")
        ?: rowId?.let { "SR-$it" }
        ?: error("complaint row carries neither ticket_number nor id")

    val component = readComponent()
    val created = timestamp("created_at", "raised_at") ?: 0L
    val issue = text("issue", "issue_description").orEmpty()

    // `problem` and `vehicle_type` are the form's own two fields; a row filed
    // before they existed carries neither, and problemLabel falls back to the
    // component for those.
    val problem = text("problem")

    return ServiceTicket(
        ticketId = ticketId,
        serverId = rowId,
        vehicle = readVehicle(vehicles),
        component = component,
        problem = problem,
        vehicleType = ServiceVehicleType.fromApi(text("vehicle_type")),
        // The problem is the ticket's headline now, so the title is the opening of
        // the description — the detail header shows one above the other.
        issueTitle = issue.take(60),
        issueDescription = issue,
        errorCode = text("error_code"),
        status = resolveServiceStatus(text("status", "complaint_status")),
        attachments = readRemoteAttachments(),
        // No technician fields exist on the payload yet; the detail screen already
        // hides the allocation card when this is null.
        allocation = null,
        customerType = ServiceCustomerType.fromApi(text("customer_type")),
        // The row's own account of the charge, and the only one there is now that the
        // device-local payment record is gone. The receipt is usually null — rows come
        // back with a null `merchant_order_id` even when paid — so the status and the
        // amount are read alongside it: they are what the detail screen can still say.
        payment = readServerPayment(created),
        paymentStatus = text("payment_status"),
        paymentAmountRupees = number("payment_amount", "service_charge", "amount"),
        // Read straight off the row as well, because readServerPayment gives up when
        // `merchant_order_id` is missing — which is exactly the common paid row. The
        // detail screen's "Paid on" and "Payment ref" come from these.
        paymentPaidAtMillis = readChargePaidAt(),
        paymentReference = readChargeReference(),
        timeline = readTimeline(ticketId, component, created),
        createdAtMillis = created,
        lastUpdatedMillis = timestamp("updated_at") ?: created,
    )
}

/**
 * Builds the ticket from the submission response, keeping the attachment bytes
 * the user just picked — `service/my-complaints` echoes paths, never bytes, so
 * this is the only copy that can preview without a download.
 */
@OptIn(ExperimentalTime::class)
private fun JsonObject.toSubmittedTicket(
    vehicle: ServiceVehicle,
    component: ServiceComponentItem,
    draft: ComplaintDraft,
    customerType: ServiceCustomerType,
    payment: ServicePaymentReceipt?,
): ServiceTicket {
    val now = Clock.System.now().toEpochMilliseconds()
    val ticketId = text("ticket_number", "ticket_no", "ticket_id", "id") ?: "EVSR-$now"
    val created = timestamp("created_at", "raised_at") ?: now

    return ServiceTicket(
        ticketId = ticketId,
        // Kept so the detail screen can read the ticket back straight away,
        // before the list endpoint has been re-polled.
        serverId = text("id"),
        vehicle = vehicle,
        component = component,
        problem = text("problem")?.takeIf { it.isNotBlank() } ?: draft.problem.trim(),
        vehicleType = ServiceVehicleType.fromApi(text("vehicle_type")) ?: draft.vehicleType,
        issueTitle = draft.issueDescription.trim().take(60),
        issueDescription = text("issue", "issue_description") ?: draft.issueText,
        errorCode = text("error_code") ?: draft.errorCode.trim().takeIf { it.isNotBlank() },
        status = resolveServiceStatus(text("status", "complaint_status")),
        attachments = readStoredAttachments(draft),
        allocation = null,
        customerType = text("customer_type")?.let { ServiceCustomerType.fromApi(it) } ?: customerType,
        payment = payment ?: readServerPayment(created),
        // A receipt in hand means paid, whatever the response still says — it is
        // written the moment PhonePe settles, and this body was composed before that.
        paymentStatus = if (payment != null) "SUCCESS" else text("payment_status"),
        paymentAmountRupees = payment?.amountRupees
            ?: number("service_charge", "payment_amount", "amount"),
        paymentPaidAtMillis = payment?.paidAtMillis ?: readChargePaidAt(),
        paymentReference = payment?.merchantOrderId ?: readChargeReference(),
        timeline = buildList {
            payment?.let {
                add(
                    TimelineEntry(
                        title = "Service charge paid",
                        note = "₹${it.amountRupees} received · ${it.merchantOrderId}",
                        timestampMillis = it.paidAtMillis,
                    )
                )
            }
            add(
                TimelineEntry(
                    title = "Complaint submitted",
                    note = "Ticket $ticketId created for ${component.label}.",
                    timestampMillis = created,
                )
            )
        }.reversed(),
        createdAtMillis = created,
        lastUpdatedMillis = created,
    )
}

/**
 * The server's `component` object is authoritative for the id and the name. The
 * local catalogue is consulted only for "does this need an invoice", matched by
 * name — its own numeric ids were guessed before this endpoint existed and do
 * not line up with the server's.
 */
private fun JsonObject.readComponent(): ServiceComponentItem {
    val node = this["component"]?.let { runCatching { it.jsonObject }.getOrNull() }
    val name = node?.text("name")
    val id = node?.text("id") ?: text("component_id")

    val catalogue = ServiceComponentCatalog.firstOrNull { it.label.equals(name, ignoreCase = true) }

    return ServiceComponentItem(
        id = catalogue?.id ?: name?.serviceSlug() ?: id ?: "other",
        label = name ?: catalogue?.label ?: "Other",
        requiresInvoice = catalogue?.requiresInvoice ?: true,
        serverId = id ?: catalogue?.serverId,
    )
}

/**
 * `device_id` is null on most complaints — the server files them without one —
 * and is matched against the device list when present. [UnlinkedVehicle] is the
 * display stand-in, exactly as it is at submission time.
 */
private fun JsonObject.readVehicle(vehicles: List<ServiceVehicle>): ServiceVehicle {
    val nested = this["device"]?.let { runCatching { it.jsonObject }.getOrNull() }
    val id = nested?.text("device_id", "id") ?: text("device_id") ?: return UnlinkedVehicle

    vehicles.firstOrNull { it.vehicleId == id || it.vin == id }?.let { return it }

    val number = nested?.text("device_number", "device_name") ?: return UnlinkedVehicle
    return ServiceVehicle(
        vehicleId = id,
        registrationNumber = number,
        model = "EVA2Z GPS Tracker",
        vin = number,
    )
}

/** Invoice, problem photos and — on the detail response — any extra documents. */
private fun JsonObject.readRemoteAttachments(): List<ServiceAttachment> {
    fun array(key: String) = this[key]?.let { runCatching { it.jsonArray }.getOrNull() }

    val invoice = this["invoice"]
        ?.let { runCatching { it.jsonObject }.getOrNull() }
        ?.toStoredAttachment(AttachmentKind.INVOICE, local = null)

    val photos = (array("media") ?: array("photos"))
        ?.mapNotNull { element ->
            runCatching {
                element.jsonObject.toStoredAttachment(AttachmentKind.PHOTO, local = null)
            }.getOrNull()
        }
        .orEmpty()

    val documents = array("documents")
        ?.mapNotNull { element ->
            runCatching {
                element.jsonObject.toStoredAttachment(AttachmentKind.INVOICE, local = null)
            }.getOrNull()
        }
        .orEmpty()

    return listOfNotNull(invoice) + photos + documents
}

/**
 * `status_history` is the timeline, newest first, with any free-text `remarks`
 * folded in. Falls back to a single "submitted" entry so the detail screen is
 * never blank for a ticket the server sent no history for.
 */
@OptIn(ExperimentalTime::class)
private fun JsonObject.readTimeline(
    ticketId: String,
    component: ServiceComponentItem,
    createdAtMillis: Long,
): List<TimelineEntry> {
    fun array(key: String) = this[key]?.let { runCatching { it.jsonArray }.getOrNull() }

    val history = array("status_history")
        ?.mapNotNull { element ->
            val entry = runCatching { element.jsonObject }.getOrNull() ?: return@mapNotNull null
            TimelineEntry(
                title = resolveServiceStatus(entry.text("new_status")).label,
                note = entry.text("remark", "note"),
                timestampMillis = entry.timestamp("created_at") ?: createdAtMillis,
            )
        }
        .orEmpty()

    val remarks = array("remarks")
        ?.mapNotNull { element ->
            val entry = runCatching { element.jsonObject }.getOrNull() ?: return@mapNotNull null
            val note = entry.text("remark", "message", "note", "body") ?: return@mapNotNull null
            TimelineEntry(
                title = entry.text("added_by_role", "role")?.let { "Update from $it" } ?: "Update",
                note = note,
                timestampMillis = entry.timestamp("created_at") ?: createdAtMillis,
            )
        }
        .orEmpty()

    return (history + remarks)
        .sortedByDescending { it.timestampMillis }
        .ifEmpty {
            listOf(
                TimelineEntry(
                    title = "Complaint submitted",
                    note = "Ticket $ticketId created for ${component.label}.",
                    timestampMillis = createdAtMillis,
                )
            )
        }
}

/**
 * The stored invoice and photos, falling back to what the user picked when the
 * server echoes nothing back — the detail screen should never look empty just
 * because the response shape changed.
 */
private fun JsonObject.readStoredAttachments(draft: ComplaintDraft): List<ServiceAttachment> {
    val invoice = this["invoice"]
        ?.let { runCatching { it.jsonObject }.getOrNull() }
        ?.toStoredAttachment(AttachmentKind.INVOICE, draft.invoice)
        ?: draft.invoice

    val photos = this["photos"]
        ?.let { runCatching { it.jsonArray }.getOrNull() }
        ?.mapIndexedNotNull { index, element ->
            runCatching {
                element.jsonObject.toStoredAttachment(
                    kind = AttachmentKind.PHOTO,
                    local = draft.media.getOrNull(index),
                )
            }.getOrNull()
        }
        ?.takeIf { it.isNotEmpty() }
        ?: draft.media

    return listOfNotNull(invoice) + photos
}

/**
 * A receipt reconstructed from the response, for a complaint whose charge the
 * server already considers settled but that this session did not pay for itself.
 * `payment_status: "not_required"` and a null order id both mean there is none.
 */
private fun JsonObject.readServerPayment(createdAtMillis: Long): ServicePaymentReceipt? {
    val merchantOrderId = text("merchant_order_id", "merchantOrderId") ?: return null
    // "payment_amount" is what a complaint row calls it; "service_charge" is the
    // submission response's name for the same figure.
    val charge = number("service_charge", "payment_amount", "amount") ?: return null
    if (charge <= 0) return null

    val settled = text("payment_status")?.serviceSlug()
    if (settled == "not_required" || settled == "pending" || settled == "failed") return null

    return ServicePaymentReceipt(
        merchantOrderId = merchantOrderId,
        phonepeOrderId = text("phonepe_order_id", "phonepeOrderId"),
        amountRupees = charge,
        paidAtMillis = timestamp("paid_at", "payment_completed_at") ?: createdAtMillis,
    )
}

/**
 * When the charge was paid, read off the complaint row on its own terms.
 *
 * [readServerPayment] returns null the moment `merchant_order_id` is absent, and
 * rows come back that way even when paid — so the timestamp has to be read
 * separately or the detail screen has nothing to put against "Paid on".
 */
@OptIn(ExperimentalTime::class)
private fun JsonObject.readChargePaidAt(): Long? = timestamp(
    "paid_at",
    "payment_completed_at",
    "payment_date",
    "paid_on",
    "transaction_date",
)

/**
 * The reference for the charge, in the order support can act on: our own
 * `merchant_order_id` first, then PhonePe's order id, then whatever transaction
 * id the row carries.
 */
private fun JsonObject.readChargeReference(): String? = text(
    "merchant_order_id",
    "merchantOrderId",
    "phonepe_order_id",
    "phonepeOrderId",
    "transaction_id",
    "payment_id",
    "order_id",
)

/**
 * Pulls a PhonePe checkout out of a `payment_required` response.
 *
 * `merchant_order_id` and `phonepe_order_id` are always present in the body but are
 * JSON null on a free complaint, which [findStringDeep] already discards. The token
 * is the part that decides: without it the SDK has nothing to resolve the order
 * against, so this returns null and the caller creates the order separately.
 */
private fun JsonObject.readPaymentOrder(amountRupees: Int): ServicePaymentOrder? {
    val merchantOrderId = findStringDeep("merchantOrderId", "merchant_order_id") ?: return null
    val token = findStringDeep("order_token", "token", "orderToken")
    if (token == null) {
        println("⚠️ service/complaints returned $merchantOrderId with no payment token — creating the order separately.")
        return null
    }
    val phonepeOrderId = findStringDeep("phonepe_order_id", "phonepeOrderId")
        ?: findStringDeep("orderId")
        ?: merchantOrderId

    return ServicePaymentOrder(
        merchantOrderId = merchantOrderId,
        phonepeOrderId = phonepeOrderId,
        token = token,
        amountRupees = amountRupees,
        // This order belongs to the complaint, so re-posting the complaint is how
        // it gets confirmed — there is no callback endpoint to reach for.
        confirmedByResubmit = true,
    )
}

/**
 * The backend reports four statuses — submitted, open, in progress, completed.
 * The extra spellings are older wordings and synonyms still present on historic
 * rows; each folds onto the nearest of the four rather than inventing a stage.
 */
private fun resolveServiceStatus(raw: String?): TicketStatus = when (raw?.serviceSlug()) {
    "submitted", "new", "raised", "complaint_raised", "pending" -> TicketStatus.SUBMITTED
    "open", "reopened", "assigned", "technician_assigned", "allocated", "approved", "verified" ->
        TicketStatus.OPEN
    "in_progress", "servicing", "work_in_progress", "processing" -> TicketStatus.IN_PROGRESS
    "completed", "resolved", "done", "closed" -> TicketStatus.COMPLETED
    else -> TicketStatus.SUBMITTED
}

/** "In Progress" / "in-progress" / "IN PROGRESS" all collapse to "in_progress". */
private fun String.serviceSlug(): String = trim().lowercase()
    .map { if (it.isLetterOrDigit()) it else '_' }
    .joinToString("")
    .trim('_')

private fun JsonObject.text(vararg keys: String): String? {
    for (key in keys) {
        val primitive = this[key] as? JsonPrimitive ?: continue
        val value = primitive.contentOrNull?.trim() ?: continue
        if (value.isNotEmpty() && !value.equals("null", ignoreCase = true)) return value
    }
    return null
}

/**
 * `service_charge` arrives as 1, "1" and "1.00" depending on the route, so a
 * decimal string is read too — without it a ₹1 charge fell through to the app's
 * own ₹299 expectation and the button quoted the wrong figure.
 */
private fun JsonObject.number(vararg keys: String): Int? {
    for (key in keys) {
        val primitive = this[key] as? JsonPrimitive ?: continue
        primitive.intOrNull?.let { return it }
        val raw = primitive.contentOrNull?.trim() ?: continue
        raw.toIntOrNull()?.let { return it }
        raw.toDoubleOrNull()?.let { return it.toInt() }
    }
    return null
}

/**
 * Laravel hands back either "2026-09-16T10:12:33.000000Z" or "2026-09-16 10:12:33";
 * the second form has no offset, so it is read as UTC — the same assumption the
 * rest of the app makes for server timestamps.
 */
@OptIn(ExperimentalTime::class)
private fun JsonObject.timestamp(vararg keys: String): Long? {
    val raw = text(*keys) ?: return null
    val candidates = listOf(raw, raw.replace(' ', 'T'), raw.replace(' ', 'T') + "Z")
    for (candidate in candidates) {
        runCatching { Instant.parse(candidate).toEpochMilliseconds() }.getOrNull()?.let { return it }
    }
    return null
}





