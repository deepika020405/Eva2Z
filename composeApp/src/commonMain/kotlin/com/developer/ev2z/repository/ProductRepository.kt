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

    suspend fun addToCart(token: String, productId: Int, quantity: Int = 1): Boolean {
        return api.addToCart(token, productId, quantity, "android").success
    }
}