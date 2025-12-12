package com.developer.ev2z.network

import com.developer.ev2z.data.local.model.DeviceResponse
import com.developer.ev2z.data.local.remote.LiveLocationResponse
import com.developer.ev2z.request.CartAddRequest
import com.developer.ev2z.request.DeviceAnalyticsRequest
import com.developer.ev2z.request.LoginRequest
import com.developer.ev2z.response.*
import io.ktor.client.call.*
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement

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


    // imports you may need

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
            // Ktor HTTP error with response available
            val status = e.response.status
            val text = try { e.response.bodyAsText() } catch (_: Exception) { "unable-to-read-body" }
            println("❌ getProducts - HTTP error: $status")
            println("   response body: $text")
            emptyList()
        } catch (e: Exception) {
            // network / serialization / other errors
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
            // Return a safe fallback
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
            // safe fallback
            CartResponse(success = false, cart = emptyList(), totalPrice = "0.00")
        }
    }


    suspend fun addToCart(
        token: String,
        add: String,
        productId: Int,
        quantity: Int = 1,
        plan_type: String,
        deviceType: String
    ): CartActionResponse {

        val url = "${baseUrl}cart"

        // ---- PRINT REQUEST BEFORE SENDING ----
        val bodyToSend = CartAddRequest(
            action = add,
            product_id = productId,
            quantity = quantity,
            plan_type = plan_type,
            device_type = deviceType
        )

        println("🌐 POST URL = $url")
        println("📦 REQUEST BODY = ${kotlinx.serialization.json.Json.encodeToString(CartAddRequest.serializer(), bodyToSend)}")

        // ---- DO REQUEST ----
        val response = client.post(url) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)

            setBody(bodyToSend)
        }

        // ---- PRINT RAW RESPONSE ----
        val raw = response.bodyAsText()
        println("📝 RAW RESPONSE = $raw")
        println("📡 RESPONSE STATUS = ${response.status}")

        // ---- TRY PARSE ----
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
}
