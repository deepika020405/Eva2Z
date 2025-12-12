package com.developer.ev2z.repository

import com.developer.ev2z.network.ApiService
import com.developer.ev2z.response.BillingDto
import com.developer.ev2z.response.CartResponse
import com.developer.ev2z.response.ProductDto
import com.developer.ev2z.response.PromoResponse
import com.developer.ev2z.response.WishlistProductWrapper
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
}