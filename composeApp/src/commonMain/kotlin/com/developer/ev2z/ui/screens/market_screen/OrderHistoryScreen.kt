// commonMain/ui/screens/market_screen/OrderHistoryScreen.kt
package com.developer.ev2z.ui.screens.market_screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.developer.ev2z.repository.OrderItemUi
import com.developer.ev2z.repository.OrderStatus
import com.developer.ev2z.repository.OrderUi

/**
 * Stages always shown on the tracking timeline. It stops at payment confirmation because
 * `my-billing` carries no dispatch/delivery status for most orders — later stages would
 * only ever render as permanently pending. [stagesFor] extends the timeline for an order
 * whose status the server does report.
 */
private val TRACKING_STAGES = listOf(
    OrderStatus.PLACED,
    OrderStatus.CONFIRMED
)

private val FULFILMENT_STAGES = listOf(
    OrderStatus.PROCESSING,
    OrderStatus.SHIPPED,
    OrderStatus.DELIVERED
)

private fun stagesFor(status: OrderStatus): List<OrderStatus> =
    TRACKING_STAGES + FULFILMENT_STAGES.filter { it.ordinal <= status.ordinal }

/**
 * Shop → My Orders.
 *
 * @param highlightOrderId order to open expanded (used right after a payment succeeds).
 */
data class OrderHistoryScreen(
    private val highlightOrderId: String? = null
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        OrderHistoryContent(
            highlightOrderId = highlightOrderId,
            onBack = { navigator.pop() },
            onShopNow = { navigator.popUntilRoot() }
        )
    }
}

@Composable
fun OrderHistoryContent(
    highlightOrderId: String? = null,
    onBack: () -> Unit,
    onShopNow: () -> Unit
) {
    val viewModel = OrderHistoryProvider.viewModel
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    // A just-placed order syncs to the server under its own id, so when the highlighted
    // local id is gone, open the newest order instead — that is the one the user paid for.
    val expandedId = remember(highlightOrderId, state.orders) {
        when {
            highlightOrderId == null -> null
            state.orders.any { it.orderId == highlightOrderId } -> highlightOrderId
            else -> state.orders.firstOrNull()?.orderId
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(P_Bg)
    ) {
        OrderHistoryTopBar(
            count = state.orders.size,
            refreshing = state.refreshing,
            onRefresh = { viewModel.refresh() },
            onBack = onBack
        )

        when {
            state.loading && state.orders.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = P_Green, strokeWidth = 2.dp)
            }

            state.orders.isEmpty() -> EmptyOrders(
                serverUnreachable = state.serverUnreachable,
                onRetry = { viewModel.refresh() },
                onShopNow = onShopNow
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (state.serverUnreachable) {
                    item(key = "offline") { OfflineNotice(onRetry = { viewModel.refresh() }) }
                } else if (state.totalOrders > 0) {
                    item(key = "summary") {
                        SpendSummary(totalOrders = state.totalOrders, grandTotal = state.grandTotal)
                    }
                }

                items(state.orders, key = { it.orderId }) { order ->
                    OrderCard(
                        order = order,
                        initiallyExpanded = order.orderId == expandedId
                    )
                }
            }
        }
    }
}

// ── Top bar ──────────────────────────────────────────────────────
@Composable
private fun OrderHistoryTopBar(
    count: Int,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    Surface(color = P_Bg, shadowElevation = 0.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp).clip(CircleShape).background(P_ChipBg)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Back",
                    tint = P_Dark, modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text("My Orders", fontSize = 22.sp, fontWeight = FontWeight.Black, color = P_Dark)
                Text(
                    when {
                        refreshing -> "Updating…"
                        count == 0 -> "Track your purchases"
                        else -> "$count ${if (count == 1) "order" else "orders"} placed"
                    },
                    fontSize = 12.sp, color = P_Mid
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp).clip(CircleShape).background(P_ChipBg)
                    .clickable(enabled = !refreshing) { onRefresh() },
                contentAlignment = Alignment.Center
            ) {
                if (refreshing) {
                    CircularProgressIndicator(
                        color = P_GreenDark, strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.Refresh, "Refresh orders",
                        tint = P_GreenDark, modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ── Server summary / connectivity ────────────────────────────────
@Composable
private fun SpendSummary(totalOrders: Int, grandTotal: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(P_InfoBoxBg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Total orders", fontSize = 11.sp, color = P_Mid)
            Text(
                totalOrders.toString(),
                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = P_Dark
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("Total spent", fontSize = 11.sp, color = P_Mid)
            Text(
                "₹$grandTotal",
                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = P_GreenDark
            )
        }
    }
}

@Composable
private fun OfflineNotice(onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(P_Gold.copy(alpha = 0.14f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Couldn't reach the server — showing orders saved on this device.",
            fontSize = 12.sp, color = P_Dark, lineHeight = 17.sp,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "Retry",
            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = P_GreenDark,
            modifier = Modifier.clickable { onRetry() }
        )
    }
}

// ── Empty state ──────────────────────────────────────────────────
@Composable
private fun EmptyOrders(
    serverUnreachable: Boolean,
    onRetry: () -> Unit,
    onShopNow: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp).clip(RoundedCornerShape(22.dp)).background(P_ChipBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.ReceiptLong, null,
                    tint = P_Green, modifier = Modifier.size(36.dp)
                )
            }

            Spacer(Modifier.height(18.dp))
            Text(
                if (serverUnreachable) "Couldn't load your orders" else "No orders yet",
                fontSize = 17.sp, fontWeight = FontWeight.Bold, color = P_Dark
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (serverUnreachable)
                    "Check your connection and try again."
                else
                    "Orders you place in the shop will show up here with their tracking status.",
                fontSize = 13.sp, color = P_Mid, textAlign = TextAlign.Center, lineHeight = 19.sp
            )

            Spacer(Modifier.height(22.dp))
            Button(
                onClick = if (serverUnreachable) onRetry else onShopNow,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = P_Green),
                modifier = Modifier.height(46.dp)
            ) {
                Text(
                    if (serverUnreachable) "Try Again" else "Start Shopping",
                    fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                )
            }
        }
    }
}

// ── Order card ───────────────────────────────────────────────────
@Composable
private fun OrderCard(order: OrderUi, initiallyExpanded: Boolean) {
    var expanded by remember(order.orderId) { mutableStateOf(initiallyExpanded) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = P_Card),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, P_Border, RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Order #${order.orderId}",
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, color = P_Dark,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (order.isLocalOnly) "Placed on ${order.dateLabel} • Syncing"
                        else "Placed on ${order.dateLabel}",
                        fontSize = 11.sp, color = P_Light
                    )
                }
                StatusChip(order.status)
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = P_Border, thickness = 0.8.dp)
            Spacer(Modifier.height(12.dp))

            order.items.forEach { item ->
                OrderItemRow(item)
                Spacer(Modifier.height(10.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total paid", fontSize = 12.sp, color = P_Mid)
                Text(
                    "₹${order.totalAmount.toDoubleOrNull()?.toInt() ?: order.totalAmount}",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = P_GreenDark
                )
            }

            Spacer(Modifier.height(12.dp))

            // Expand / collapse tracking
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(P_ChipBg)
                    .clickable { expanded = !expanded }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (expanded) "Hide tracking" else "Track order",
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = P_GreenDark
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null, tint = P_GreenDark, modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(16.dp))
                    TrackingTimeline(order)

                    order.address?.let { addr ->
                        Spacer(Modifier.height(16.dp))
                        DeliveryAddressBlock(
                            name = addr.name,
                            phone = addr.phone,
                            line = addr.oneLine
                        )
                    }

                    if (order.transactionId != null) {
                        Spacer(Modifier.height(12.dp))
                        DetailRow("Transaction ID", order.transactionId)
                    }
                    if (order.expectedDelivery != null && order.status != OrderStatus.DELIVERED &&
                        order.status != OrderStatus.CANCELLED
                    ) {
                        Spacer(Modifier.height(6.dp))
                        DetailRow("Expected delivery", "by ${order.expectedDelivery}")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: OrderStatus) {
    val color = when (status) {
        OrderStatus.CANCELLED -> P_Red
        OrderStatus.DELIVERED -> P_Green
        OrderStatus.SHIPPED   -> Color(0xFF4A90D9)
        else                  -> P_Gold
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            status.label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (status == OrderStatus.DELIVERED) P_GreenDark else color
        )
    }
}

@Composable
private fun OrderItemRow(item: OrderItemUi) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(46.dp).clip(RoundedCornerShape(10.dp)).background(P_ImageBg),
            contentAlignment = Alignment.Center
        ) {
            if (item.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("📦", fontSize = 20.sp)
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.name.ifBlank { "Item" },
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = P_Dark,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            val meta = listOfNotNull(
                "Qty ${item.quantity}",
                item.planType?.replace("_", " ")?.uppercase()
            ).joinToString(" • ")
            Text(meta, fontSize = 11.sp, color = P_Light)
        }

        Text(
            "₹${item.price.toDoubleOrNull()?.toInt() ?: item.price}",
            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = P_Dark
        )
    }
}

// ── Tracking timeline ────────────────────────────────────────────
@Composable
private fun TrackingTimeline(order: OrderUi) {
    if (order.status == OrderStatus.CANCELLED) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(P_Red.copy(alpha = 0.08f))
                .padding(14.dp)
        ) {
            Text(
                "This order was cancelled. Any amount paid is refunded to the original payment method.",
                fontSize = 12.sp, color = P_Red, lineHeight = 17.sp
            )
        }
        return
    }

    val stages = stagesFor(order.status)

    // A status past the last shown stage still marks every shown stage complete.
    val currentIndex = stages
        .indexOfLast { it.ordinal <= order.status.ordinal }
        .coerceAtLeast(0)

    Column {
        Text("Order Tracking", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = P_Dark)
        Spacer(Modifier.height(12.dp))

        stages.forEachIndexed { index, stage ->
            TimelineStep(
                label = stage.label,
                subtitle = when {
                    index == 0 -> order.dateLabel
                    index == currentIndex -> "Current status"
                    index > currentIndex -> "Pending"
                    else -> null
                },
                done = index <= currentIndex,
                isCurrent = index == currentIndex,
                isLast = index == stages.lastIndex
            )
        }

        if (currentIndex < stages.lastIndex) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Payment confirmation will appear here shortly.",
                fontSize = 11.sp, color = P_Light, lineHeight = 16.sp
            )
        } else if (order.isLocalOnly) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Waiting for the order to appear on our servers — it will update on its own.",
                fontSize = 11.sp, color = P_Light, lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun TimelineStep(
    label: String,
    subtitle: String?,
    done: Boolean,
    isCurrent: Boolean,
    isLast: Boolean
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (done) P_Green else P_Border),
                contentAlignment = Alignment.Center
            ) {
                if (done) {
                    Icon(
                        Icons.Default.Check, null,
                        tint = Color.White, modifier = Modifier.size(13.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(7.dp).clip(CircleShape).background(Color.White)
                    )
                }
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(26.dp)
                        .background(if (done) P_Green.copy(alpha = 0.45f) else P_Border)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.padding(bottom = if (isLast) 0.dp else 8.dp)) {
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                color = if (done) P_Dark else P_Light
            )
            if (subtitle != null) {
                Text(subtitle, fontSize = 11.sp, color = if (isCurrent) P_GreenDark else P_Light)
            }
        }
    }
}

// ── Address + detail rows ────────────────────────────────────────
@Composable
private fun DeliveryAddressBlock(name: String, phone: String, line: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(P_InfoBoxBg)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.LocationOn, null,
                tint = P_GreenDark, modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text("Delivery Address", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = P_GreenDark)
        }
        Spacer(Modifier.height(6.dp))
        if (name.isNotBlank()) {
            Text(name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = P_Dark)
        }
        if (line.isNotBlank()) {
            Text(line, fontSize = 12.sp, color = P_Mid, lineHeight = 17.sp)
        }
        if (phone.isNotBlank()) {
            Text("Phone: $phone", fontSize = 11.sp, color = P_Mid)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(label, fontSize = 11.sp, color = P_Light, modifier = Modifier.weight(0.45f))
        Text(
            value,
            fontSize = 11.sp, fontWeight = FontWeight.Medium, color = P_Mid,
            textAlign = TextAlign.End, modifier = Modifier.weight(0.55f)
        )
    }
}