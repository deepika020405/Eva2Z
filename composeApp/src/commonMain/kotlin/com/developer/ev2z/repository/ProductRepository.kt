package com.developer.ev2z.repository

import com.developer.ev2z.network.ApiService
import com.developer.ev2z.request.CreateOrderProduct
import com.developer.ev2z.request.CreateOrderRequest
import com.developer.ev2z.response.CartItemDto
import com.developer.ev2z.response.BillingDto
import com.developer.ev2z.response.CartResponse
import com.developer.ev2z.response.MyDevicesResponse
import com.developer.ev2z.response.ProductDto
import com.developer.ev2z.response.PromoResponse
import com.developer.ev2z.response.WishlistProductWrapper
import com.developer.ev2z.ui.screens.VehicleParkingTag.UserParkingListResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement

class ProductRepository(
    private val api: ApiService
) {

    suspend fun getProducts(token: String): List<ProductDto> {
        return api.getProducts(token)
    }

    suspend fun getWishlist(token: String): List<WishlistProductWrapper> {
        return api.getWishlist(token).wishlist
    }

    suspend fun addWishlist(token: String, productId: Int): Boolean {
        return api.addToWishlist(token, productId).success
    }

    suspend fun removeWishlist(token: String, productId: Int): Boolean {
        return api.removeFromWishlist(token, productId).success
    }

    suspend fun getCart(token: String): CartResponse {
        return api.getCart(token)
    }

    suspend fun addToCart(
        token: String,
        add: String,
        productId: Int,
        quantity: Int,
        plan_type: String,
        device_type: String
    ): Boolean {

        val resp = api.addToCart(token, add,productId, quantity, plan_type, device_type)

        return resp.success == true   // <-- null handled
    }


    suspend fun updateCartQuantity(
        token: String,
        productId: Int,
        quantity: Int,
        plan_type: String?,
        device_type: String?
    ): Boolean {
        return api.updateCartQuantity(
            token = token,
            productId = productId,
            quantity = quantity,
            planType = plan_type,
            deviceType = device_type
        ).success == true
    }


    suspend fun getBillingAddresses(token: String): List<BillingDto> {
        return try {
            val resp = api.getGpsBilling(token)
            println("✔ Billing API success, items: ${resp.billings.size}")
            resp.billings
        } catch (e: Exception) {
            println("❌ getBillingAddresses ERROR: ${e.message}")
            emptyList()
        }
    }


    suspend fun addBillingAddress(
        token: String,
        fname: String,
        phone: String,
        email: String,
        address: String,
        city: String,
        state: String,
        pincode: String
    ): Boolean {

        val json = JsonObject(
            mapOf(
                "fname" to Json.encodeToJsonElement(fname),
                "lname" to Json.encodeToJsonElement(""),
                "phone" to Json.encodeToJsonElement(phone),
                "email" to Json.encodeToJsonElement(email),
                "address" to Json.encodeToJsonElement(address),
                "city" to Json.encodeToJsonElement(city),
                "state" to Json.encodeToJsonElement(state),
                "pincode" to Json.encodeToJsonElement(pincode)
            )
        )

        return try {
            val resp = api.saveGpsBilling(token, json)
            resp["status"]?.toString()?.contains("success", true) == true
        } catch (e: Exception) {
            println("❌ AddBillingAddress Error: ${e.message}")
            false
        }
    }



    suspend fun applyPromo(
        token: String,
        amount: String,
        code: String
    ): PromoResponse {
        return api.applyPromo(token, amount, code.uppercase())
    }




    suspend fun createPhonePeOrder(
        token: String,
        amount: Double,
        billingId: Int,
        cart: List<CartItemDto>
    ): JsonObject {

        val body = CreateOrderRequest(
            amount = amount,
            final_amount = amount,
            billing_id = billingId,
            products = cart.map {
                CreateOrderProduct(
                    product_id = it.product_id,
                    quantity = it.quantity,
                    price = it.price,
                    plan_type = it.plan_type,
                    device_type = it.device_type
                )
            }
        )

        return api.createPhonePeOrder(token, body)
    }


    suspend fun getMyDevices(token: String): MyDevicesResponse {
        return try { api.getMyDevices(token) } catch (e: Exception) { MyDevicesResponse() }
    }

    suspend fun getUserParkingList(): UserParkingListResponse {
        return try { api.getUserParkingDetails() } catch (e: Exception) { UserParkingListResponse() }
    }

    // ProductRepository.kt

    suspend fun deleteBillingAddress(
        token: String,
        billingId: Int
    ): Boolean {
        return try {
            val resp = api.removeGpsBilling(token, billingId)
            resp["status"]?.toString()?.contains("success", true) == true
        } catch (e: Exception) {
            false
        }
    }
}