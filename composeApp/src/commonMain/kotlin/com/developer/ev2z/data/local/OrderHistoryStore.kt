package com.developer.ev2z.data.local

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Local record of orders placed in this app.
 *
 * `my-billing` is the source of truth for order history, but a freshly paid order does not
 * appear there instantly (the server records it when the payment callback settles). Every
 * successful shop payment is written here so "My Orders" and the success screen have the
 * real order to show right away; [com.developer.ev2z.repository.OrderRepository] merges
 * these with the server list and drops each local copy once the server returns that order.
 */
@Serializable
data class StoredOrderItem(
    val productId: Int = 0,
    val name: String = "",
    val imageUrl: String = "",
    val quantity: Int = 1,
    val price: String = "0",
    val planType: String? = null
)

@Serializable
data class StoredOrderAddress(
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = ""
)

@Serializable
data class StoredOrder(
    val orderId: String,
    val transactionId: String? = null,
    /** PhonePe merchant order id returned by `phonepe/create-orders` — the strongest key
     *  for recognising this order once `my-billing` starts returning it. */
    val merchantOrderId: String? = null,
    /** Epoch millis when the payment succeeded. */
    val placedAt: Long = 0L,
    val totalAmount: String = "0",
    val items: List<StoredOrderItem> = emptyList(),
    val address: StoredOrderAddress? = null,
    /** Matches OrderStatus.name; the server drives status once the order syncs. */
    val status: String = "CONFIRMED"
)

class OrderHistoryStore(private val storage: StorageManager) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    fun getOrders(): List<StoredOrder> {
        val raw = storage.getString(KEY_ORDERS, "")
        if (raw.isBlank()) return emptyList()
        return runCatching {
            json.decodeFromString<List<StoredOrder>>(raw)
        }.getOrElse {
            println("❌ OrderHistoryStore decode failed: ${it.message}")
            emptyList()
        }.sortedByDescending { it.placedAt }
    }

    fun saveOrder(order: StoredOrder) {
        // Re-entering the success screen must not duplicate the same payment.
        val existing = getOrders().filterNot { it.orderId == order.orderId }
        val updated = (listOf(order) + existing).take(MAX_ORDERS)
        storage.saveString(KEY_ORDERS, json.encodeToString(updated))
        println("📦 Order saved locally: ${order.orderId} (${updated.size} in history)")
    }

    /** Drops local copies of orders the server has taken over. */
    fun removeOrders(orderIds: Set<String>) {
        if (orderIds.isEmpty()) return
        val remaining = getOrders().filterNot { it.orderId in orderIds }
        storage.saveString(KEY_ORDERS, json.encodeToString(remaining))
        println("🔄 ${orderIds.size} local order(s) now served by the server; ${remaining.size} left local")
    }

    fun clear() = storage.remove(KEY_ORDERS)

    private companion object {
        const val KEY_ORDERS = "shop_order_history"
        const val MAX_ORDERS = 50
    }
}