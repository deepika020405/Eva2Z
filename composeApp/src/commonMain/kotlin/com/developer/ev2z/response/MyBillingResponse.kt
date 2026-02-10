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

    val billing: BillingUserDto,

    @SerialName("order_total")
    val orderTotal: Int,

    val items: List<BillingItemDto>
)

@Serializable
data class BillingUserDto(
    val name: String,
    val phone: String,
    val email: String,
    val city: String,
    val state: String,
    val pincode: String
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
    val planType: String,

    @SerialName("recharge_amount")
    val rechargeAmount: String
)