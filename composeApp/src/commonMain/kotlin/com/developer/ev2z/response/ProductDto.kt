package com.developer.ev2z.response


import kotlinx.serialization.Serializable

@Serializable
data class ProductDto(
    val id: Int,
    val name: String,
    val sale_price: String,
    val promo_code: String? = null,
    val promo_amt: String? = null,
    val features1: String? = null,
    val features2: String? = null,
    val features3: String? = null,
    val features4: String? = null,
    val features5: String? = null,
    val features6: String? = null,
    val features7: String? = null,
    val device_type: String? = null,
    val image: String? = null,
    val description: String? = null,
    val recharge_price: String? = null
)

@Serializable
data class WishlistProductWrapper(
    val id: Int,
    val user_id: Int,
    val product_id: Int,
    val created_at: String,
    val product: ProductDto
)

@Serializable
data class WishlistResponse(
    val success: Boolean,
    val wishlist: List<WishlistProductWrapper>
)

@Serializable
data class WishlistActionResponse(
    val success: Boolean,
    val message: String
)
