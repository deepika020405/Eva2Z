package com.developer.ev2z.response

// package com.developer.ev2z.data.remote.billing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MyBillingResponse(
    val status: Boolean,
    @SerialName("total_orders")
    val totalOrders: Int,
    @SerialName("grand_total")
    val grandTotal: Int,
    val billings: List<BillingOrderDto>
)

@Serializable
data class BillingOrderDto(
    @SerialName("order_id")
    val orderId: Int,

    @SerialName("transaction_id")
    val transactionId: String,

    @SerialName("order_date")
    val orderDate: String,

    val billing: BillingUserDto? = null,   // ✅ nullable

    @SerialName("order_total")
    val orderTotal: Int,

    val items: List<BillingItemDto> = emptyList()  // ✅ safe default
)

@Serializable
data class BillingUserDto(
    @SerialName("billing_id")
    val billingId: Int? = null,            // ✅ API me hai

    val name: String? = null,              // ✅ nullable
    val phone: String? = null,
    val email: String? = null,
    val city: String? = null,
    val state: String? = null,
    val pincode: String? = null
)

@Serializable
data class BillingItemDto(
    @SerialName("product_id")
    val productId: Int,

    @SerialName("product_name")
    val productName: String,

    val quantity: Int,

    val price: String,

    @SerialName("plan_type")
    val planType: String? = null,          // safe

    @SerialName("recharge_amount")
    val rechargeAmount: String? = null
)