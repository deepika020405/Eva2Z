package com.developer.ev2z.repository

import com.developer.ev2z.network.ApiService
import com.developer.ev2z.response.CartResponse
import com.developer.ev2z.response.ProductDto
import com.developer.ev2z.response.WishlistProductWrapper

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

}