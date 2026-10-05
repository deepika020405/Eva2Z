package com.developer.ev2z.ui.screens.market_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.developer.ev2z.phonepe.PaymentState
import com.developer.ev2z.response.CartItemDto
import com.developer.ev2z.ui.components.FloatingContactButton
import com.developer.ev2z.util.Analytics
import kotlin.math.roundToInt

class CartScreen : Screen {
    @Composable
    override fun Content() { CartScreenContent() }
}

@Composable
fun CartScreenContent() {
    val viewModel = ShopViewModelProvider.viewModel
    val state by viewModel.state.collectAsState()
    val navigator = LocalNavigator.currentOrThrow

    LaunchedEffect(Unit) {
        Analytics.logEvent("screen_view", mapOf("screen_name" to "cart_screen"))
        viewModel.loadCart()
        viewModel.loadBillingAddresses()
    }

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = P_Green, strokeWidth = 2.dp)
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(P_Bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)) {
                PremiumCartHeader(state.cartCount)
                Spacer(Modifier.height(8.dp))

                if (state.cart.isEmpty()) {
                    PremiumEmptyCart()
                } else {
                    state.cart.forEach { item ->
                        PremiumCartItem(item, viewModel)
                        Spacer(Modifier.height(12.dp))
                    }
                    PremiumAddressSection(
                        selectedAddress = state.selectedAddress,
                        addresses = state.addresses,
                        onSelect = { viewModel.selectAddress(it) },
                        onAddNew = { navigator.push(AddNewAddressScreen()) },
                        viewModel = viewModel
                    )
                    if (state.cart.isNotEmpty()) {
                        PremiumCouponsSection(viewModel)
                        PremiumOrderSummary(viewModel)
                    }
                }
            }
            if (state.cart.isNotEmpty()) {
                PremiumCheckoutBar(viewModel)
            }
        }

        FloatingContactButton(
            phoneNumber = "919755273822",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 90.dp)
        )
    }
}

// ── Header ─────────────────────────────────────────────────────────
@Composable
fun CartHeader(count: Int) { PremiumCartHeader(count) }

@Composable
private fun PremiumCartHeader(count: Int) {
    val navigator = LocalNavigator.currentOrThrow
    Surface(color = P_Card, shadowElevation = 0.dp) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(38.dp).clip(CircleShape)
                    .background(P_Bg).clickable { navigator.pop() },
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ArrowBack, null,
                        tint = P_Dark, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("My Cart", fontSize = 18.sp,
                        fontWeight = FontWeight.Bold, color = P_Dark)
                    Text("$count item${if (count != 1) "s" else ""}",
                        fontSize = 12.sp, color = P_Light)
                }
            }
            HorizontalDivider(color = P_Border, thickness = 0.5.dp)
        }
    }
}

// ── Empty Cart ─────────────────────────────────────────────────────
@Composable
private fun PremiumEmptyCart() {
    val navigator = LocalNavigator.currentOrThrow
    Column(modifier = Modifier.fillMaxWidth().padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(80.dp).clip(CircleShape)
            .background(P_GreenSurface), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.ShoppingBag, null,
                tint = P_Green, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Cart is empty", fontSize = 18.sp,
            fontWeight = FontWeight.Bold, color = P_Dark)
        Spacer(Modifier.height(6.dp))
        Text("Browse our GPS trackers to get started",
            fontSize = 14.sp, color = P_Light)
        Spacer(Modifier.height(18.dp))
        OutlinedButton(
            onClick = { navigator.push(OrderHistoryScreen()) },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = P_Green),
            border = BorderStroke(1.dp, P_GreenBorder)
        ) {
            Icon(Icons.Outlined.ReceiptLong, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("View My Orders", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Cart Item ──────────────────────────────────────────────────────
@Composable
fun CartItemCard(item: CartItemDto, viewModel: ShopViewModel) {
    PremiumCartItem(item, viewModel)
}

@Composable
private fun PremiumCartItem(item: CartItemDto, viewModel: ShopViewModel) {
    val salePrice = item.product?.sale_price?.toDoubleOrNull() ?: 0.0
    val fakeOld = (salePrice * 1.5).toInt()
    val disc = (((fakeOld - salePrice) / fakeOld) * 100).toInt()
    var showRemove by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()
        .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = P_Card),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, P_Border)) {
        Column {
            Row(modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top) {
                // Image
                Box(modifier = Modifier.size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(P_Bg)) {
                    AsyncImage(model = item.product?.image.orEmpty(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        contentScale = ContentScale.Fit)
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.product?.name ?: "",
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        color = P_Dark, maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(5.dp))
                    // Tag
                    val tag = when {
                        item.plan_type == "with_recharge" -> "With Recharge"
                        item.plan_type == "without_recharge" -> "Without Recharge"
                        item.device_type?.lowercase() == "android" -> "Android"
                        item.device_type?.lowercase() == "ios" -> "iOS"
                        else -> null
                    }
                    tag?.let {
                        Box(modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(P_GreenSurface)
                            .padding(horizontal = 7.dp, vertical = 3.dp)) {
                            Text(it, fontSize = 10.sp,
                                fontWeight = FontWeight.Medium, color = P_Green)
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("₹${salePrice.toInt()}", fontSize = 18.sp,
                            fontWeight = FontWeight.Bold, color = P_Dark)
                        Spacer(Modifier.width(8.dp))
                        Text("₹$fakeOld", fontSize = 12.sp, color = P_Light,
                            textDecoration = TextDecoration.LineThrough)
                        Spacer(Modifier.width(6.dp))
                        Text("$disc% off", fontSize = 11.sp,
                            fontWeight = FontWeight.Medium, color = P_Green)
                    }
                }
                Spacer(Modifier.width(10.dp))
                PremiumQtyController(item, viewModel) { showRemove = true }
            }
            if (item.plan_type == "with_recharge") {
                Row(modifier = Modifier.fillMaxWidth()
                    .background(Color(0xFFF0F4FF))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Wifi, null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(14.dp))
                    Text("Recharge Plan: ₹${item.recharge_amount ?: "0"}",
                        fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        color = Color(0xFF3B82F6))
                }
            }
        }
    }

    if (showRemove) {
        AlertDialog(onDismissRequest = { showRemove = false },
            title = { Text("Remove item?", fontWeight = FontWeight.Bold) },
            text = { Text("This item will be removed from your cart.") },
            confirmButton = {
                TextButton(onClick = {
                    showRemove = false
                    Analytics.logEvent("remove_from_cart", mapOf(
                        "item_id" to item.product_id, "item_name" to item.product?.name))
                    viewModel.removeFromCart(item)
                }) { Text("Remove", color = P_Red, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showRemove = false }) {
                    Text("Cancel", color = P_Mid)
                }
            },
            shape = RoundedCornerShape(16.dp))
    }
}

// ── Qty Controller ─────────────────────────────────────────────────
@Composable
fun QuantityControllerWithRemove(item: CartItemDto, viewModel: ShopViewModel, onZeroReached: () -> Unit) {
    PremiumQtyController(item, viewModel, onZeroReached)
}

@Composable
private fun PremiumQtyController(
    item: CartItemDto, viewModel: ShopViewModel, onZeroReached: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp))
            .border(1.dp, P_GreenBorder, RoundedCornerShape(8.dp))
            .background(P_GreenSurface)
            .clickable {
                viewModel.updateQuantity(item.product_id, item.quantity + 1,
                    item.plan_type, item.device_type)
            }, contentAlignment = Alignment.Center) {
            Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = P_Green)
        }
        Text(item.quantity.toString(), fontSize = 15.sp,
            fontWeight = FontWeight.Bold, color = P_Dark)
        Box(modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp))
            .border(1.dp, if (item.quantity == 1) Color(0xFFFFCDD2) else P_Border,
                RoundedCornerShape(8.dp))
            .background(if (item.quantity == 1) Color(0xFFFFF0F0) else P_Bg)
            .clickable {
                if (item.quantity > 1) viewModel.updateQuantity(item.product_id,
                    item.quantity - 1, item.plan_type, item.device_type)
                else onZeroReached()
            }, contentAlignment = Alignment.Center) {
            Icon(
                if (item.quantity == 1) Icons.Default.DeleteOutline
                else Icons.Default.Remove,
                null,
                tint = if (item.quantity == 1) P_Red else P_Mid,
                modifier = Modifier.size(15.dp))
        }
    }
}

// ── Address Section ────────────────────────────────────────────────
@Composable
fun AddressSection(
    selectedAddress: BillingAddress?, addresses: List<BillingAddress>,
    onSelect: (BillingAddress) -> Unit, onAddNew: () -> Unit, viewModel: ShopViewModel
) { PremiumAddressSection(selectedAddress, addresses, onSelect, onAddNew, viewModel) }

@Composable
private fun PremiumAddressSection(
    selectedAddress: BillingAddress?, addresses: List<BillingAddress>,
    onSelect: (BillingAddress) -> Unit, onAddNew: () -> Unit, viewModel: ShopViewModel
) {
    var showDialog by remember { mutableStateOf(false) }
    val navigator = LocalNavigator.currentOrThrow

    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text("Delivery Address", fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold, color = P_Dark)
        Spacer(Modifier.height(10.dp))
        Card(shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = P_Card),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier.fillMaxWidth()
                .border(1.dp, P_Border, RoundedCornerShape(14.dp))
                .clickable { showDialog = true }) {
            Row(modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(38.dp).clip(CircleShape)
                    .background(P_GreenSurface), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.LocationOn, null,
                        tint = P_Green, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    if (selectedAddress == null) {
                        Text("Add delivery address", fontSize = 14.sp,
                            fontWeight = FontWeight.Medium, color = P_Mid)
                        Text("Tap to select or add",
                            fontSize = 12.sp, color = P_Light)
                    } else {
                        Text(selectedAddress.fullName, fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold, color = P_Dark)
                        Text("${selectedAddress.address}, ${selectedAddress.city}, ${selectedAddress.state}",
                            fontSize = 12.sp, color = P_Mid, maxLines = 2,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
                Icon(Icons.Default.ChevronRight, null,
                    tint = P_Light, modifier = Modifier.size(20.dp))
            }
        }
    }

    if (showDialog) {
        AddressDialog(
            list = addresses,
            onSelect = { onSelect(it); showDialog = false },
            // The pencil opens the address for editing — deleting is not what it means.
            onEdit = { showDialog = false; navigator.push(AddNewAddressScreen(it)) },
            onAddNew = { showDialog = false; navigator.push(AddNewAddressScreen()) },
            onDismiss = { showDialog = false })
    }
}

// ── Address Dialog ─────────────────────────────────────────────────
@Composable
fun AddressDialog(
    list: List<BillingAddress>, onSelect: (BillingAddress) -> Unit,
    onEdit: (BillingAddress) -> Unit, onAddNew: () -> Unit, onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = P_Card)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Delivery Address", fontWeight = FontWeight.Bold,
                    fontSize = 18.sp, color = P_Dark)
                Spacer(Modifier.height(16.dp))
                Column(modifier = Modifier.heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())) {
                    list.forEach { addr ->
                        Card(shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(P_Bg),
                            elevation = CardDefaults.cardElevation(0.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                                .border(1.dp, P_Border, RoundedCornerShape(12.dp))) {
                            Row(modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top) {
                                Column(modifier = Modifier.weight(1f)
                                    .clickable { onSelect(addr) }) {
                                    Text(addr.fullName, fontWeight = FontWeight.SemiBold,
                                        color = P_Dark)
                                    Spacer(Modifier.height(2.dp))
                                    Text(addr.phone, fontSize = 13.sp, color = P_Mid)
                                    Text("${addr.address}, ${addr.city}",
                                        fontSize = 12.sp, color = P_Light)
                                    Text("${addr.state} − ${addr.pincode}",
                                        fontSize = 12.sp, color = P_Light)
                                }
                                IconButton(onClick = { onEdit(addr) },
                                    modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.EditNote, "Edit address",
                                        tint = P_Green, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = onAddNew,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = P_Green),
                    elevation = ButtonDefaults.buttonElevation(0.dp)) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add New Address", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Coupons ────────────────────────────────────────────────────────
@Composable
fun CouponsSection(viewModel: ShopViewModel) { PremiumCouponsSection(viewModel) }

@Composable
private fun PremiumCouponsSection(viewModel: ShopViewModel) {
    val state by viewModel.state.collectAsState()
    var code by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text("Promo Code", fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold, color = P_Dark)
        Spacer(Modifier.height(10.dp))
        Card(shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = P_Card),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier.fillMaxWidth()
                .border(1.dp, P_Border, RoundedCornerShape(14.dp))) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it; viewModel.clearPromoError() },
                        placeholder = { Text("Enter promo code",
                            fontSize = 14.sp, color = P_Light) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = P_Green,
                            unfocusedBorderColor = P_Border))
                    Button(onClick = { viewModel.applyPromo(code) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = P_Green),
                        elevation = ButtonDefaults.buttonElevation(0.dp),
                        modifier = Modifier.height(56.dp)) {
                        Text("Apply", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
                state.promoError?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = P_Red, fontSize = 12.sp)
                }
                if (state.promoApplied) {
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(P_GreenSurface)
                        .border(1.dp, P_GreenBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CheckCircle, null,
                            tint = P_Green, modifier = Modifier.size(18.dp))
                        Column {
                            Text("${state.appliedPromoCode} applied",
                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                color = P_Green)
                            Text("You save ₹${state.discountApplied}",
                                fontSize = 12.sp, color = P_Mid)
                        }
                    }
                }
            }
        }
    }
}

// ── Order Summary ──────────────────────────────────────────────────
@Composable
fun PaymentSummarySection(viewModel: ShopViewModel) { PremiumOrderSummary(viewModel) }

@Composable
private fun PremiumOrderSummary(viewModel: ShopViewModel) {
    val state by viewModel.state.collectAsState()
    val hasRecharge = state.cart.any { it.plan_type == "with_recharge" }

    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text("Order Summary", fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold, color = P_Dark)
        Spacer(Modifier.height(10.dp))
        Card(shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = P_Card),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier.fillMaxWidth()
                .border(1.dp, P_Border, RoundedCornerShape(14.dp))) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (hasRecharge) {
                    state.cart.filter { it.plan_type == "with_recharge" }.forEach { item ->
                        SummaryRow("${item.product?.name} (Recharge)",
                            "₹${item.recharge_amount ?: "0"}")
                    }
                    state.cart.forEach { item ->
                        SummaryRow("${item.product?.name} (Base)",
                            "₹${item.product?.sale_price ?: 0}")
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp),
                        color = P_Border, thickness = 0.5.dp)
                    SummaryRow("Total",
                        "₹${state.finalAmount ?: state.totalPrice ?: "0"}", isBold = true)
                } else {
                    SummaryRow("Subtotal",
                        "₹${state.originalAmount ?: state.totalPrice ?: "0"}")
                    if (state.promoApplied) {
                        SummaryRow("Promo Discount",
                            "−₹${state.discountApplied ?: 0.0}", isGreen = true)
                    }
                    SummaryRow("Delivery", "FREE", isGreen = true)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp),
                        color = P_Border, thickness = 0.5.dp)
                    SummaryRow("Total Payable",
                        "₹${state.finalAmount ?: state.totalPrice ?: "0"}", isBold = true)
                }
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String, isBold: Boolean = false, isGreen: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp,
            fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isBold) P_Dark else P_Mid)
        Text(value, fontSize = 14.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = if (isGreen) P_Green else if (isBold) P_Dark else P_Mid)
    }
}

// ── Checkout Bar ───────────────────────────────────────────────────
@Composable
fun CheckoutBottomBar(viewModel: ShopViewModel) { PremiumCheckoutBar(viewModel) }

@Composable
private fun PremiumCheckoutBar(viewModel: ShopViewModel) {
    val state by viewModel.state.collectAsState()
    val paymentState by viewModel.paymentState.collectAsState()
    val navigator = LocalNavigator.currentOrThrow
    var showErr by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxWidth(), color = P_Card,
        shadowElevation = 24.dp, tonalElevation = 0.dp) {
        Column {
            HorizontalDivider(color = P_Border, thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                // ✅ .navigationBarsPadding() hatao
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ){
                Column {
                    Text("Total", fontSize = 12.sp, color = P_Light)
                    Text("₹${state.finalAmount ?: state.totalPrice ?: "0"}",
                        fontSize = 22.sp, fontWeight = FontWeight.Black, color = P_Dark)
                }
                if (paymentState is PaymentState.Processing) {
                    CircularProgressIndicator(color = P_Green,
                        modifier = Modifier.size(34.dp), strokeWidth = 2.dp)
                } else {
                    Button(onClick = {
                        if (state.selectedAddress == null) { showErr = true; return@Button }
                        Analytics.logEvent("begin_checkout", mapOf(
                            "currency" to "INR",
                            "value" to (state.finalAmount ?: state.totalPrice ?: 0),
                            "items_count" to state.cart.size))
                        navigator.push(PaymentScreen())
                    },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = P_Green),
                        elevation = ButtonDefaults.buttonElevation(0.dp),
                        modifier = Modifier.height(50.dp).widthIn(min = 150.dp)) {
                        Text("Proceed to Pay",
                            fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (showErr) {
        AlertDialog(onDismissRequest = { showErr = false },
            confirmButton = {
                TextButton(onClick = { showErr = false }) {
                    Text("OK", color = P_Green)
                }
            },
            title = { Text("Address Required", fontWeight = FontWeight.Bold) },
            text = { Text("Please add or select a delivery address.") },
            shape = RoundedCornerShape(16.dp))
    }
}

// ── Swipe to Remove ────────────────────────────────────────────────
@Composable
fun SwipeToRemove(onRemove: () -> Unit, content: @Composable () -> Unit) {
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffset by animateIntOffsetAsState(
        targetValue = IntOffset(offsetX.roundToInt(), 0))
    Box(modifier = Modifier.offset { animatedOffset }
        .pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    if (kotlin.math.abs(offsetX) > 250f) onRemove() else offsetX = 0f
                }) { _, drag -> offsetX += drag }
        }) { content() }
}