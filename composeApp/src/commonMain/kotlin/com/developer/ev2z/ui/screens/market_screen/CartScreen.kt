// This is CMP new code Android Ios Both
package com.developer.ev2z.ui.screens.market_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.developer.ev2z.getPlatformContext
import com.developer.ev2z.response.CartItemDto
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import com.developer.ev2z.data.local.model.UpiApp
import com.developer.ev2z.phonepe.PaymentState
import com.developer.ev2z.phonepe.openPhonePeSdk
import com.developer.ev2z.ui.components.FloatingContactButton
import com.developer.ev2z.upi.UPI_APPS
import com.developer.ev2z.upi.launchUpiPayment
import com.developer.ev2z.util.Analytics
import kotlin.math.roundToInt

class CartScreen : Screen {
    @Composable
    override fun Content() {
        CartScreenContent()
    }
}



@Composable
fun CartScreenContent() {
    val viewModel = ShopViewModelProvider.viewModel
    val state by viewModel.state.collectAsState()
    val navigator = LocalNavigator.currentOrThrow

    LaunchedEffect(Unit) {
        Analytics.logEvent(
            name = "screen_view",
            params = mapOf(
                "screen_name" to "cart_screen",
                "screen_class" to "CartScreen"
            )
        )
        viewModel.loadCart()
        viewModel.loadBillingAddresses()
    }

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LaunchedEffect(state.cart) {
        if (state.cart.isNotEmpty()) {
            Analytics.logEvent(
                name = "view_cart",
                params = mapOf(
                    "currency" to "INR",
                    "value" to (state.finalAmount ?: state.totalPrice ?: 0),
                    "items_count" to state.cart.size
                )
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ⭐ Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp)
            ) {
                CartHeader(state.cartCount)
                Spacer(Modifier.height(12.dp))

                state.cart.forEach { item ->
                    CartItemCard(item, viewModel)
                    Spacer(Modifier.height(12.dp))
                }

                AddressSection(
                    selectedAddress = state.selectedAddress,
                    addresses = state.addresses,
                    onSelect = { viewModel.selectAddress(it) },
                    onAddNew = { navigator.push(AddNewAddressScreen()) },
                    viewModel = viewModel
                )

                // 👇 Promo + Summary yahin condition ke saath aayega
                if (state.cart.isNotEmpty()) {
                    CouponsSection(viewModel)
                }

                PaymentSummarySection(viewModel)
            }
            if (state.cart.isNotEmpty()) {
                CheckoutBottomBar(viewModel)
            }

        }

        // ⭐ FLOATING CONTACT BUTTON (ALWAYS VISIBLE)
        FloatingContactButton(
            phoneNumber = "919755273822",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
        )
    }

}

@Composable
fun CartHeader(count: Int) {
    val navigator = LocalNavigator.currentOrThrow
    Row(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text("Cart ($count item)", fontSize = 20.sp, fontWeight = FontWeight.Bold)



        Text(
            "Continue Shopping ✕",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.clickable { navigator.pop() }
        )
    }
}




@Composable
fun CartItemCard(item: CartItemDto, viewModel: ShopViewModel) {
    val salePrice = item.product?.sale_price?.toDoubleOrNull() ?: 0.0
    val fakeOldPrice = (salePrice * 1.5).toInt()
    val discountPercent = (((fakeOldPrice - salePrice) / fakeOldPrice) * 100).toInt()

    var showRemoveDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFf2f5f0))
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.product?.image.orEmpty(),
                contentDescription = null,
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.product?.name ?: "", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))

                val planType = when (item.plan_type?.lowercase()) {
                    "with_recharge" -> "With Recharge"
                    "without_recharge" -> "Without Recharge"
                    else -> null
                }

                val deviceType = when (item.device_type?.lowercase()) {
                    "android" -> "Android"
                    "ios" -> "iOS"
                    else -> null
                }

                val displayText = planType ?: deviceType ?: ""
                Text(displayText, fontWeight = FontWeight.Bold)

                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .background(Color(0xFF1B9F4A), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Rs. $salePrice", color = Color.White)
                    }

                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Rs. $fakeOldPrice",
                        textDecoration = TextDecoration.LineThrough,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )

                    Spacer(Modifier.width(6.dp))
                    Text(
                        "$discountPercent% OFF",
                        color = Color(0xFF1B9F4A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                if (item.plan_type == "with_recharge") {
                    val recharge = item.recharge_amount ?: "0"
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .background(Color(0xFF005BBB), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "Recharge Included: ₹$recharge",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // DELETE ICON TRIGGERS CONFIRMATION DIALOG
//                Box(
//                    modifier = Modifier
//                        .size(28.dp)
//                        .clip(CircleShape)
//                        .background(Color(0xFFE53935))
//                        .clickable { showRemoveDialog = true },
//                    contentAlignment = Alignment.Center
//                ) {
//                    Text("🗑", color = Color.White, fontSize = 14.sp)
//                }

//                Spacer(Modifier.height(8.dp))

                QuantityControllerWithRemove(item, viewModel) {
                    // Callback when quantity becomes zero → show remove dialog
                    showRemoveDialog = true
                }
            }
        }
    }

    // ✅ Remove Confirmation Dialog
    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove Item?") },
            text = { Text("Are you sure you want to remove this item from your cart?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveDialog = false
                        // Trigger analytics on removal
                        Analytics.logEvent(
                            name = "remove_from_cart",
                            params = mapOf(
                                "item_id" to item.product_id,
                                "item_name" to item.product?.name,
                                "item_variant" to (item.device_type ?: ""),
                                "quantity" to item.quantity
                            )
                        )
                        viewModel.removeFromCart(item)
                    }
                ) { Text("Yes", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("No")
                }
            }
        )
    }
}



@Composable
fun QuantityControllerWithRemove(
    item: CartItemDto,
    viewModel: ShopViewModel,
    onZeroReached: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = {
                viewModel.updateQuantity(
                    productId = item.product_id,
                    newQty = item.quantity + 1,
                    plan_type = item.plan_type,
                    device_type = item.device_type
                )
            },
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(28.dp),
            shape = CircleShape
        ) { Text("+") }

        Text(item.quantity.toString(), fontWeight = FontWeight.Bold, fontSize = 14.sp)

        Button(
            onClick = {
                if (item.quantity > 1) {
                    viewModel.updateQuantity(
                        productId = item.product_id,
                        newQty = item.quantity - 1,
                        plan_type = item.plan_type,
                        device_type = item.device_type
                    )
                } else {
                    // If quantity is 1 → trigger remove confirmation
                    onZeroReached()
                }
            },
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(28.dp),
            shape = CircleShape
        ) { Text("-") }
    }
}



@Composable
fun AddressSection(
    selectedAddress: BillingAddress?,
    addresses: List<BillingAddress>,
    onSelect: (BillingAddress) -> Unit,
    onAddNew: () -> Unit,
    viewModel: ShopViewModel
) {
    var showDialog by remember { mutableStateOf(false) }
    val navigator = LocalNavigator.currentOrThrow
    Column(Modifier.padding(16.dp)) {

        Text(
            "Delivery Address",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        Spacer(Modifier.height(10.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(Color(0xFFEFF7F0)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDialog = true }
        ) {
            Column(Modifier.padding(16.dp)) {

                Text("📍 Selected Address", fontWeight = FontWeight.SemiBold)

                Spacer(Modifier.height(6.dp))

                if (selectedAddress == null) {
                    Text("No Billing Address Found")
                } else {
                    Text(
                        "${selectedAddress.fullName} | ${selectedAddress.phone}",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("${selectedAddress.address}, ${selectedAddress.city}")
                    Text("${selectedAddress.state} - ${selectedAddress.pincode}")
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    "Change / Select Address",
                    color = Color(0xFF1B9F4A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }

    if (showDialog) {
        AddressDialog(
            list = addresses,
            onSelect = {
                onSelect(it)
                showDialog = false
            },
            onDelete = {
                viewModel.deleteBillingAddress(it.id)
            },
            onAddNew = {
                showDialog = false
                navigator.push(AddNewAddressScreen())
            },
            onDismiss = { showDialog = false }
        )
    }
}


@Composable
fun AddressDialog(
    list: List<BillingAddress>,
    onSelect: (BillingAddress) -> Unit,
    onDelete: (BillingAddress) -> Unit,
    onAddNew: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {

        Card(
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {

                Text("Select Address", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .heightIn(max = 350.dp)
                        .verticalScroll(rememberScrollState())
                ) {

                    list.forEach { addr ->

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(Color(0xFFF4F7F4)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {

                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {

                                Column(
                                    Modifier
                                        .weight(1f)
                                        .clickable { onSelect(addr) }
                                ) {
                                    Text(addr.fullName, fontWeight = FontWeight.Bold)
                                    Text("📞 ${addr.phone}")
                                    Spacer(Modifier.height(4.dp))
                                    Text("${addr.address}, ${addr.city}")
                                    Text("${addr.state} - ${addr.pincode}")
                                }

                                IconButton(
                                    onClick = { onDelete(addr) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Address",
                                        tint = Color.Red
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onAddNew,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(Color(0xFF1B9F4A))
                ) {
                    Text("➕ Add New Address", color = Color.White)
                }
            }
        }
    }
}



@Composable
fun CouponsSection(viewModel: ShopViewModel) {
    val state by viewModel.state.collectAsState()

    var code by remember { mutableStateOf("") }

    Column(Modifier.padding(16.dp)) {

        Text("Apply Promo Code", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(12.dp))

        TextField(
            value = code,
            onValueChange = {
                code = it
                // Clear error while typing
                viewModel.clearPromoError()
            },
            label = { Text("Enter Promo Code") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        if (state.promoError != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = state.promoError!!,
                color = Color.Red,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(10.dp))

        Button(
            onClick = { viewModel.applyPromo(code) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(Color(0xFF1B9F4A))
        ) {
            Text("Apply Code", color = Color.White)
        }

        Spacer(Modifier.height(20.dp))

        if (state.promoApplied) {

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(Color(0xFFDFF5E1)),
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(Modifier.padding(16.dp)) {

                    Text(
                        "Promo Applied: ${state.appliedPromoCode}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1B9F4A)
                    )

                    Spacer(Modifier.height(10.dp))

                    Text("Price Breakup", fontWeight = FontWeight.SemiBold)

                    Spacer(Modifier.height(8.dp))

                    SummaryRow("Original Price", "₹${state.originalAmount}")
                    SummaryRow("Discount Applied", "-₹${state.discountApplied}")
                    Divider(Modifier.padding(vertical = 8.dp))
                    SummaryRow("Final Amount", "₹${state.finalAmount}", isBold = true)
                }
            }
        }
        LaunchedEffect(state.promoApplied) {
            if (state.promoApplied) {
                Analytics.logEvent(
                    name = "select_promotion",
                    params = mapOf(
                        "promotion_name" to state.appliedPromoCode,
                        "discount_value" to state.discountApplied,
                        "currency" to "INR"
                    )
                )
            }
        }
    }
}








@Composable
fun PaymentSummarySection(viewModel: ShopViewModel) {
    val state by viewModel.state.collectAsState()

    // Check if any item in cart is with_recharge
    val hasRecharge = state.cart.any { it.plan_type == "with_recharge" }

    Column(Modifier.padding(16.dp)) {

        Text("Payment Summary", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        if (hasRecharge) {
            // 1️⃣ Show Recharge Items
            state.cart.filter { it.plan_type == "with_recharge" }.forEach { item ->
                SummaryRow(
                    label = "${item.product?.name} \n(Recharge Included)",
                    value = "₹${item.recharge_amount ?: "0"}"
                )
            }

            Spacer(Modifier.height(8.dp))

            // 2️⃣ Show Product Base Prices
            state.cart.forEach { item ->
                val salePrice = item.product?.sale_price ?: 0
                SummaryRow(
                    label = "${item.product?.name} \nBase Price",
                    value = "₹$salePrice"
                )
            }

            Spacer(Modifier.height(8.dp))

            // 3️⃣ Cart Total
            SummaryRow("Cart Total", "₹${state.finalAmount ?: state.totalPrice ?: "0"}", isBold = true)

        } else {
            // Existing summary for normal products
            SummaryRow("Cart Total", "₹${state.originalAmount ?: state.totalPrice ?: "0"}")

            if (state.promoApplied) {
                SummaryRow(
                    "Promo Discount",
                    "-₹${state.discountApplied ?: 0.0}"
                )
            }

            SummaryRow("Shipping", "Free")
            Divider(Modifier.padding(vertical = 12.dp))

            SummaryRow(
                "Total Payable",
                "₹${state.finalAmount ?: state.totalPrice ?: "0"}",
                isBold = true
            )
        }
    }
}



@Composable
fun SummaryRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
    }
}




@Composable
fun CheckoutBottomBar(viewModel: ShopViewModel) {
    val state by viewModel.state.collectAsState()
    val paymentState by viewModel.paymentState.collectAsState()
    val navigator = LocalNavigator.currentOrThrow

    var showAddressError by remember { mutableStateOf(false) }

    // 🚨 ADDRESS NOT SELECTED POPUP
    if (showAddressError) {
        AlertDialog(
            onDismissRequest = { showAddressError = false },
            confirmButton = {
                TextButton(onClick = { showAddressError = false }) {
                    Text("OK")
                }
            },
            title = {
                Text("Address Required")
            },
            text = {
                Text("Please select or add a delivery address before checkout.")
            }
        )
    }

    Box(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(Color.White)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column {
                Text("Total Amount", color = Color.Gray)
                Text(
                    "₹${state.finalAmount ?: state.totalPrice ?: "0"}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (paymentState is PaymentState.Processing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp
                )
            } else {
                Button(
                    onClick = {

                        // 🔴 VALIDATION FIRST
                        if (state.selectedAddress == null) {
                            showAddressError = true
                            return@Button
                        }

                        // ✅ ADDRESS OK → CONTINUE
                        Analytics.logEvent(
                            name = "begin_checkout",
                            params = mapOf(
                                "currency" to "INR",
                                "value" to (state.finalAmount ?: state.totalPrice ?: 0),
                                "items_count" to state.cart.size
                            )
                        )

                        navigator.push(PaymentScreen())
                    },
                    shape = RoundedCornerShape(30.dp)
                ) {
                    Text("Checkout")
                }
            }
        }
    }
}

@Composable
fun SwipeToRemove(
    onRemove: () -> Unit,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }

    val animatedOffset by animateIntOffsetAsState(
        targetValue = IntOffset(offsetX.roundToInt(), 0)
    )

    Box(
        modifier = Modifier
            .offset { animatedOffset }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (kotlin.math.abs(offsetX) > 250f) {
                            onRemove()
                        } else {
                            offsetX = 0f
                        }
                    }
                ) { _, dragAmount ->
                    offsetX += dragAmount
                }
            }
    ) {
        content()
    }
}
