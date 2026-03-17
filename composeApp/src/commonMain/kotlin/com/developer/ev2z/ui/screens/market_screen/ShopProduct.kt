package com.developer.ev2z.ui.screens.market_screen

// ShopModels.kt — naya file banao

@kotlinx.serialization.Serializable
data class ShopProduct(
    val id: Int,
    val title: String,
    val subtitle: String,
    val price: Double,
    val rechargePrice: Double,
    val promoAmount: Double,
    val promoCode: String,
    val imageUrl: String,
    val description: String,
    val features: List<String>,
    val deviceType: String,
    val rating: Double = 4.8,
    val ratingCount: Int = 50,
    val isFavorite: Boolean = false
)