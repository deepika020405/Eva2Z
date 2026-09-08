package com.developer.ev2z.repository

import com.developer.ev2z.data.local.OrderHistoryStore
import com.developer.ev2z.data.local.StoredOrder
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.response.BillingOrderDto
import com.developer.ev2z.response.MyBillingResponse
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/** Stages an order moves through. Ordinal order is the timeline order. */
enum class OrderStatus(val label: String) {
    PLACED("Order Placed"),
    CONFIRMED("Payment Confirmed"),
    PROCESSING("Processing"),
    SHIPPED("Shipped"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled");

    companion object {
        fun from(raw: String?): OrderStatus = when (raw?.trim()?.lowercase()) {
            "placed", "pending", "created"        -> PLACED
            "confirmed", "paid", "success"        -> CONFIRMED
            "processing", "packed", "preparing"   -> PROCESSING
            "shipped", "dispatched", "in_transit",
            "out_for_delivery"                    -> SHIPPED
            "delivered", "completed"              -> DELIVERED
            "cancelled", "canceled", "refunded"   -> CANCELLED
            else                                  -> CONFIRMED
        }
    }
}

data class OrderItemUi(
    val productId: Int,
    val name: String,
    val imageUrl: String,
    val quantity: Int,
    val price: String,
    val planType: String?
)

data class OrderAddressUi(
    val name: String,
    val phone: String,
    val address: String,
    val city: String,
    val state: String,
    val pincode: String
) {
    val oneLine: String
        get() = listOf(address, city, state).filter { it.isNotBlank() }
            .joinToString(", ") + if (pincode.isNotBlank()) " - $pincode" else ""
}

data class OrderUi(
    val orderId: String,
    val transactionId: String?,
    /** Epoch millis; 0 when the source only gave a date string. */
    val placedAt: Long,
    val dateLabel: String,
    val expectedDelivery: String?,
    val totalAmount: String,
    val status: OrderStatus,
    val items: List<OrderItemUi>,
    val address: OrderAddressUi?,
    /** True when this order is only known locally (server has not returned it yet). */
    val isLocalOnly: Boolean
)

/**
 * One read of the user's order history.
 *
 * [totalOrders] / [grandTotal] come straight from `my-billing`; they cover server orders
 * only, so the summary stays consistent with what the backend has invoiced.
 */
data class OrderHistoryResult(
    val orders: List<OrderUi> = emptyList(),
    val totalOrders: Int = 0,
    val grandTotal: Int = 0,
    /** False when `my-billing` could not be read — the list is then local-only. */
    val serverReachable: Boolean = true
)

class OrderRepository(
    private val api: ApiService,
    private val store: OrderHistoryStore
) {

    /** product id → image url, filled from the shop catalogue; `my-billing` sends no images. */
    private var productImages: Map<Int, String> = emptyMap()

    /**
     * Server orders (authoritative) merged with orders this app placed that `my-billing`
     * has not returned yet.
     *
     * A local order is matched to its server twin on the PhonePe ids first, then on a
     * same-day + same-total + same-products fallback. Matched local copies hand over their
     * extras (item images, full street address, exact timestamp) and are then deleted —
     * from that point the order is served by the server alone.
     */
    suspend fun getOrders(token: String): OrderHistoryResult {
        val billing = fetchBilling(token) ?: return OrderHistoryResult(
            orders = getLocalOrders(),
            serverReachable = false
        )

        val stored = store.getOrders()
        val claimed = mutableSetOf<String>()   // local order ids the server now owns

        val serverOrders = billing.billings.map { dto ->
            val local = stored.firstOrNull { it.orderId !in claimed && it.matches(dto) }
            if (local != null) claimed += local.orderId
            dto.toUi(local)
        }

        store.removeOrders(claimed)

        val pending = stored.filterNot { it.orderId in claimed }.map { it.toUi() }

        return OrderHistoryResult(
            orders = (serverOrders + pending).sortedByDescending { it.placedAt },
            totalOrders = billing.totalOrders,
            grandTotal = billing.grandTotal,
            serverReachable = true
        )
    }

    fun getLocalOrders(): List<OrderUi> = store.getOrders().map { it.toUi() }

    /** Null means the call failed; an empty billing list means "no orders yet". */
    private suspend fun fetchBilling(token: String): MyBillingResponse? {
        if (token.isBlank()) return null
        return try {
            val response = api.getMyBilling(token)
            if (!response.status && response.billings.isEmpty()) {
                println("⚠️ my-billing returned status=false")
                return null
            }
            if (response.billings.isNotEmpty()) loadProductImages(token)
            response
        } catch (e: Exception) {
            println("❌ getOrders (server) failed: ${e.message}")
            null
        }
    }

    private suspend fun loadProductImages(token: String) {
        if (productImages.isNotEmpty()) return
        productImages = runCatching {
            api.getProducts(token)
                .mapNotNull { p -> p.image?.takeIf { it.isNotBlank() }?.let { p.id to it } }
                .toMap()
        }.getOrElse {
            println("❌ order history: product images unavailable — ${it.message}")
            emptyMap()
        }
    }

    /** Server order + the local copy of it (when this device placed it). */
    private fun BillingOrderDto.toUi(local: StoredOrder?): OrderUi {
        val placedAt = local?.placedAt?.takeIf { it > 0 } ?: parseServerDateMillis(orderDate)
        val status = OrderStatus.from(orderStatus)

        return OrderUi(
            orderId = orderId.toString(),
            transactionId = transactionId.takeIf { it.isNotBlank() } ?: local?.transactionId,
            placedAt = placedAt,
            dateLabel = if (placedAt > 0) formatOrderDate(placedAt) else orderDate.take(10),
            expectedDelivery = if (
                placedAt > 0 && status != OrderStatus.DELIVERED && status != OrderStatus.CANCELLED
            ) formatOrderDate(placedAt + DELIVERY_WINDOW_MS) else null,
            totalAmount = orderTotal.toString(),
            status = status,
            items = items.map { item ->
                OrderItemUi(
                    productId = item.productId,
                    name = item.productName.trim(),
                    imageUrl = productImages[item.productId]
                        ?: local?.items?.firstOrNull { it.productId == item.productId }?.imageUrl
                        ?: "",
                    quantity = item.quantity,
                    price = item.price,
                    planType = item.planType?.takeIf { p -> p.isNotBlank() }
                )
            },
            // The server omits the street line, so keep the local address when it matches.
            address = local?.address?.let {
                OrderAddressUi(it.name, it.phone, it.address, it.city, it.state, it.pincode)
            } ?: billing?.let {
                OrderAddressUi(
                    name = it.name.orEmpty(),
                    phone = it.phone.orEmpty(),
                    address = "",
                    city = it.city.orEmpty(),
                    state = it.state.orEmpty(),
                    pincode = it.pincode.orEmpty()
                )
            },
            isLocalOnly = false
        )
    }
}

/**
 * Is this local record the same order the server just returned?
 *
 * PhonePe ids match first. The fallback exists because the server may record its own
 * transaction reference: an order placed the same day, for the same total, with the same
 * products is the one we wrote locally.
 */
private fun StoredOrder.matches(dto: BillingOrderDto): Boolean {
    val serverTxn = dto.transactionId.trim()
    if (serverTxn.isNotEmpty()) {
        if (serverTxn.equals(transactionId?.trim(), ignoreCase = true)) return true
        if (serverTxn.equals(merchantOrderId?.trim(), ignoreCase = true)) return true
    }
    if (orderId == dto.orderId.toString()) return true

    val sameTotal = totalAmount.toDoubleOrNull()?.toInt() == dto.orderTotal
    val sameDay = placedAt > 0 &&
            formatOrderDate(placedAt) == formatOrderDate(parseServerDateMillis(dto.orderDate))
    val sameProducts = items.map { it.productId }.sorted() == dto.items.map { it.productId }.sorted()

    return sameTotal && sameDay && sameProducts && items.isNotEmpty()
}

private fun StoredOrder.toUi(): OrderUi = OrderUi(
    orderId = orderId,
    transactionId = transactionId,
    placedAt = placedAt,
    dateLabel = formatOrderDate(placedAt),
    expectedDelivery = if (placedAt > 0) formatOrderDate(placedAt + DELIVERY_WINDOW_MS) else null,
    totalAmount = totalAmount,
    status = OrderStatus.from(status),
    items = items.map {
        OrderItemUi(
            productId = it.productId,
            name = it.name,
            imageUrl = it.imageUrl,
            quantity = it.quantity,
            price = it.price,
            planType = it.planType?.takeIf { p -> p.isNotBlank() }
        )
    },
    address = address?.let {
        OrderAddressUi(
            name = it.name,
            phone = it.phone,
            address = it.address,
            city = it.city,
            state = it.state,
            pincode = it.pincode
        )
    },
    isLocalOnly = true
)

private const val DELIVERY_WINDOW_MS = 5L * 24 * 60 * 60 * 1000  // 5 days

private val MONTHS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

@OptIn(kotlin.time.ExperimentalTime::class)
fun formatOrderDate(epochMillis: Long): String {
    if (epochMillis <= 0) return "—"
    val date = Instant.fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault()).date
    return "${date.dayOfMonth} ${MONTHS[date.monthNumber - 1]} ${date.year}"
}

/**
 * Server sends `YYYY-MM-DD HH:mm:ss` (or the ISO form); the time is kept when present so
 * two orders placed on the same day still sort correctly. Unparseable dates sort last.
 */
@OptIn(kotlin.time.ExperimentalTime::class)
private fun parseServerDateMillis(raw: String): Long {
    val trimmed = raw.trim()
    if (trimmed.length > 10) {
        val isoish = trimmed.replace(' ', 'T').take(19)
        runCatching {
            return LocalDateTime.parse(isoish)
                .toInstant(TimeZone.currentSystemDefault())
                .toEpochMilliseconds()
        }
    }
    return runCatching {
        LocalDate.parse(trimmed.take(10))
            .atStartOfDayIn(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()
    }.getOrDefault(0L)
}