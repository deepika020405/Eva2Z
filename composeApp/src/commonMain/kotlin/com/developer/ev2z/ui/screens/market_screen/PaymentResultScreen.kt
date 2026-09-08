// commonMain/ui/screens/market_screen/PaymentResultScreen.kt
package com.developer.ev2z.ui.screens.market_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.util.Analytics
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.developer.ev2z.data.local.StoredOrderAddress
import com.developer.ev2z.data.local.StoredOrderItem
import com.developer.ev2z.repository.formatOrderDate
import com.developer.ev2z.theme.AppPageBg

/** Express shipping window quoted on the confirmation screen. */
private const val EXPECTED_DELIVERY_MS = 5L * 24 * 60 * 60 * 1000

class PaymentResultScreen(
    private val isSuccess: Boolean,
    private val transactionId: String? = null,
    private val amount: String? = null,
    private val reason: String? = null
) : Screen {

    @Composable
    override fun Content() {
        PaymentResultContent(
            isSuccess = isSuccess,
            transactionId = transactionId,
            amount = amount,
            reason = reason
        )
    }
}

@Composable
fun PaymentResultContent(
    isSuccess: Boolean,
    transactionId: String?,
    amount: String?,
    reason: String?
) {
    val navigator = LocalNavigator.currentOrThrow
    val viewModel = ShopViewModelProvider.viewModel
    val state by viewModel.state.collectAsState()
    // The cart is emptied the moment payment succeeds, so the ordered items come from the
    // order the ViewModel built at payment time — that is what actually holds them.
    val placedOrder by viewModel.lastPlacedOrder.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(isSuccess, placedOrder) {
        if (isSuccess) {
            Analytics.logEvent(
                name = "purchase",
                params = mapOf(
                    "transaction_id" to (transactionId ?: ""),
                    "currency" to "INR",
                    "value" to (amount?.toDoubleOrNull()
                        ?: state.finalAmount?.toDoubleOrNull()
                        ?: state.totalPrice?.toDoubleOrNull()
                        ?: 0.0),
                    "items" to (placedOrder?.items.orEmpty()).mapIndexed { index, item ->
                        mapOf(
                            "item_id" to item.productId.toString(),
                            "item_name" to item.name,
                            "item_category" to "iot_devices",
                            "price" to (item.price.toDoubleOrNull() ?: 0.0),
                            "quantity" to item.quantity,
                            "index" to index
                        )
                    }
                )
            )
        }
    }

    if (isSuccess) {
        // ✅ SUCCESS: Show Order Confirmed Design
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Success Illustration
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F5E9)),
                contentAlignment = Alignment.Center
            ) {
                Text("🛒", fontSize = 80.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title
            Text(
                text = "Order Confirmed",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontSize = 28.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Text(
                text = placedOrder?.let {
                    "Order #${it.orderId} has been placed successfully.\nYou can track it any time from My Orders."
                } ?: "Your order has been placed successfully.\nThank you for shopping with us!",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Order Details Card
            OrderDetailsCard(
                items = placedOrder?.items.orEmpty(),
                totalAmount = placedOrder?.totalAmount
                    ?: amount ?: state.finalAmount ?: state.totalPrice ?: "0",
                orderDate = formatOrderDate(placedOrder?.placedAt ?: 0L),
                expectedDelivery = placedOrder?.placedAt
                    ?.let { formatOrderDate(it + EXPECTED_DELIVERY_MS) },
                transactionId = transactionId,
                shippingAddress = placedOrder?.address
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Track Order — opens the order in My Orders
            Button(
                onClick = {
                    viewModel.resetPaymentState()
                    // replace, not push — back should land on the shop, not on this screen again
                    navigator.replace(OrderHistoryScreen(highlightOrderId = placedOrder?.orderId))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6A9F50)
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(
                    Icons.Outlined.ReceiptLong,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Track Order",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Back to Home Button
            OutlinedButton(
                onClick = {
                    viewModel.resetPaymentState()
                    navigator.popUntilRoot()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF6A9F50)
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(
                    Icons.Default.Home,
                    contentDescription = "Home",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Back to Home",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    } else {
        // ❌ FAILURE: Show Error Design

        LaunchedEffect(Unit) {
            Analytics.logEvent(
                name = "payment_failed",
                params = mapOf(
                    "transaction_id" to (transactionId ?: ""),
                    "reason" to (reason ?: "unknown"),
                    "currency" to "INR",
                    "value" to (amount?.toDoubleOrNull()
                        ?: state.finalAmount?.toDoubleOrNull()
                        ?: state.totalPrice?.toDoubleOrNull()
                        ?: 0.0),
                    "items" to state.cart.mapIndexed { index, item ->
                        mapOf(
                            "item_id" to (item.product?.id?.toString() ?: ""),
                            "item_name" to (item.product?.name ?: ""),
                            "item_category" to "iot_devices",
                            "price" to (item.product?.sale_price?.toDoubleOrNull() ?: 0.0),
                            "quantity" to item.quantity,
                            "index" to index
                        )
                    }
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFF5F5)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Failed Icon
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                color = Color(0xFFE5565B),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✕",
                            fontSize = 60.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // Title
                    Text(
                        text = "Payment Failed",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE5565B)
                    )

                    Spacer(Modifier.height(12.dp))

                    // Message
//                    Text(
//                        text = reason ?: "Something went wrong. Please try again.",
//                        fontSize = 14.sp,
//                        color = Color.Gray,
//                        textAlign = TextAlign.Center
//                    )
//
//                    Spacer(Modifier.height(24.dp))

                    // Failed Details Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(Color(0xFFFFEBEE))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = reason ?: "Payment cancelled or failed",
                                color = Color(0xFFE5565B),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Action Buttons
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = {
                                println("🔄 Retry Payment clicked")
                                viewModel.resetPaymentState()
                                navigator.pop()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(Color(0xFFE5565B))
                        ) {
                            Text("Retry Payment", fontSize = 16.sp)
                        }

                        Spacer(Modifier.height(12.dp))

                        TextButton(
                            onClick = {
                                viewModel.resetPaymentState()
                                navigator.popUntilRoot()
                            }
                        ) {
                            Text("Back to Shop", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderDetailsCard(
    items: List<StoredOrderItem>,
    totalAmount: String,
    orderDate: String,
    expectedDelivery: String?,
    transactionId: String?,
    shippingAddress: StoredOrderAddress?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppPageBg
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Section Title
            Text(
                text = "Order Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Order Items
            items.forEach { item ->
                OrderItemRow(item = item)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Dashed Divider
            DashedDivider()

            Spacer(modifier = Modifier.height(16.dp))

            // Order Information
            OrderInfoRow(
                label = "Total Amount",
                value = "₹${totalAmount.toDoubleOrNull()?.toInt() ?: totalAmount}",
                highlight = true
            )
            Spacer(modifier = Modifier.height(12.dp))

            OrderInfoRow(label = "Delivery Method", value = "Express Shipping")
            Spacer(modifier = Modifier.height(12.dp))

            OrderInfoRow(label = "Order Date", value = orderDate)

            if (expectedDelivery != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OrderInfoRow(label = "Expected Delivery", value = "by $expectedDelivery")
            }

            if (transactionId != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OrderInfoRow(label = "Transaction ID", value = transactionId)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OrderInfoRow(
                label = "Shipping Address",
                value = shippingAddress?.let {
                    "${it.address}, ${it.city}, ${it.state} - ${it.pincode}"
                } ?: "Not provided"
            )
        }
    }
}

@Composable
private fun OrderItemRow(item: StoredOrderItem) {
    val salePrice = item.price.toDoubleOrNull() ?: 0.0
    val fakeOldPrice = (salePrice * 1.5).toInt()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Product Image
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.name,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
            )

            // Product Name & Quantity
            Column {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Qty: ${item.quantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }

        // Price Section
        Column(
            horizontalAlignment = Alignment.End
        ) {
            // Discounted Price
            Text(
                text = "₹${salePrice.toInt()}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier
                    .background(Color(0xFF6A9F50), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )

            // Original Price
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "₹$fakeOldPrice",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                fontSize = 14.sp,
                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
            )
        }
    }
}

@Composable
private fun OrderInfoRow(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            color = Color.Black,
            fontSize = if (highlight) 18.sp else 14.sp,
            modifier = Modifier.weight(0.6f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun DashedDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(20) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(if (it % 2 == 0) Color.LightGray else Color.Transparent)
            )
        }
    }
}


