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
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.developer.ev2z.data.local.model.UpiApp
import com.developer.ev2z.phonepe.PaymentState
import com.developer.ev2z.phonepe.openPhonePeSdk
import com.developer.ev2z.upi.UPI_APPS
import com.developer.ev2z.upi.launchUpiPayment
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
        viewModel.loadCart()
        viewModel.loadBillingAddresses()
    }

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // ⭐ Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)     // VERY IMPORTANT!
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            CartHeader(state.cartCount)
            Spacer(Modifier.height(12.dp))

            state.cart.forEach { item ->
                CartItemCard(item, viewModel)
                Spacer(Modifier.height(12.dp))
            }

//            state.cart.forEach { item ->
//
//                SwipeToRemove(
//                    onRemove = {
//                        viewModel.removeFromCart(item)
//                    }
//                ) {
//                    CartItemCard(item, viewModel)
//                }
//
//                Spacer(Modifier.height(12.dp))
//            }

            AddressSection(
                selectedAddress = state.selectedAddress,
                addresses = state.addresses,
                onSelect = { viewModel.selectAddress(it) },
                onAddNew = {
                    navigator.push(AddNewAddressScreen())
                },
                viewModel = viewModel
            )

            CouponsSection(viewModel)
            PaymentSummarySection(viewModel)
        }

        // ⭐ Fixed Bottom Bar always visible
        CheckoutBottomBar(viewModel)
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
fun CartItemCard(item: CartItemDto,viewModel: ShopViewModel) {

    val salePrice = item.product?.sale_price?.toDoubleOrNull() ?: 0.0

    // ⭐ Fake old price — same logic as ProductDetails
    val fakeOldPrice = (salePrice * 1.5).toInt()

    // ⭐ Discount %
    val discountPercent = (((fakeOldPrice - salePrice) / fakeOldPrice) * 100).toInt()

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

                Text(displayText , fontWeight = FontWeight.Bold)
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

                // ⭐⭐⭐ SHOW RECHARGE PRICE IF WITH_RECHARGE
                if (item.plan_type == "with_recharge") {

                    val recharge = item.recharge_amount ?: "0"

                    Spacer(Modifier.height(6.dp))

                    Box(
                        Modifier
                            .background(Color(0xFF005BBB), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Recharge Included: ₹$recharge",
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

                // 🗑 DELETE ICON
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935))
                        .clickable {
                            viewModel.removeFromCart(item)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("🗑", color = Color.White, fontSize = 14.sp)
                }

                Spacer(Modifier.height(8.dp))

                // QUANTITY
                QuantityController(item, viewModel)
            }

//            QuantityController(item, viewModel )
        }
    }
}

@Composable
fun QuantityController(item: CartItemDto,viewModel: ShopViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = { viewModel.updateQuantity(
                productId = item.product_id,
                newQty = item.quantity + 1,
                plan_type = item.plan_type,
                device_type = item.device_type
            ) },
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(28.dp),
            shape = CircleShape
        ) {
            Text("+")
        }

        Text(item.quantity.toString(), fontWeight = FontWeight.Bold, fontSize = 14.sp)

        Button(
            onClick = { if (item.quantity > 1) {
                viewModel.updateQuantity(
                    productId = item.product_id,
                    newQty = item.quantity - 1,
                    plan_type = item.plan_type,
                    device_type = item.device_type
                )
            } },
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(28.dp),
            shape = CircleShape
        ) {
            Text("-")
        }
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
    }
}


@Composable
fun AddAddressUI(
    onSave: (
        String, String, String, String, String, String, String
    ) -> Unit,
    onCancel: () -> Unit
) {
    var fname by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var pincode by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {

        Text("Add New Address", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        CustomInput("Full Name", fname) { fname = it }
        CustomInput("Phone Number", phone) { phone = it }
        CustomInput("Email Address", email) { email = it }
        CustomInput("House / Street", address) { address = it }
        CustomInput("City", city) { city = it }
        CustomInput("State", state) { state = it }
        CustomInput("Pincode", pincode) { pincode = it }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                onSave(fname, phone, email, address, city, state, pincode)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(Color(0xFF34A853))
        ) {
            Text("Save Address", color = Color.White)
        }

        // Replace the UPI bottom sheet button with:

//        Button(
//            onClick = {
//                // ✅ Navigate to payment method selector
//                navigator.push(PaymentScreen())
//            },
//            shape = RoundedCornerShape(30.dp)
//        ) {
//            Text("Proceed to Payment")
//        }

        Spacer(Modifier.height(10.dp))

        TextButton(onClick = onCancel) {
            Text("Cancel")
        }
    }
}

@Composable
fun CustomInput(label: String, value: String, onChange: (String) -> Unit) {
    Column {
        Text(label, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))

        TextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun PaymentSummarySection(viewModel: ShopViewModel) {
    val state by viewModel.state.collectAsState()

    Column(Modifier.padding(16.dp)) {

        Text("Payment Summary", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        // ORIGINAL CART TOTAL (FROM SERVER BEFORE PROMO)
        SummaryRow("Cart Total", "₹${state.originalAmount ?: state.totalPrice ?: "0"}")

        // ONLY IF PROMO APPLIED
        if (state.promoApplied) {

            SummaryRow(
                "Promo Discount",
                "-₹${state.discountApplied ?: 0.0}"
            )
        }

        SummaryRow("Shipping", "Free")

        Divider(Modifier.padding(vertical = 12.dp))

        // FINAL AMOUNT (AFTER PROMO)
        SummaryRow(
            "Total Payable",
            "₹${state.finalAmount ?: state.totalPrice ?: "0"}",
            isBold = true
        )
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

//@Composable
//fun CheckoutBottomBar(viewModel: ShopViewModel) {
//    val state by viewModel.state.collectAsState()
//    val paymentState by viewModel.paymentState.collectAsState()
//    val context = getPlatformContext()!! // returns Activity on Android
//
//
//    var showUpiSheet by remember { mutableStateOf(false) } // 👈 OUTSIDE
//    val navigator = LocalNavigator.currentOrThrow  // ✅ ADD THIS
//
//    Box(
//        Modifier
//            .fillMaxWidth()
//            .padding(16.dp)
//    ) {
//        Row(
//            Modifier
//                .fillMaxWidth()
//                .clip(RoundedCornerShape(30.dp))
//                .background(Color.White)
//                .padding(16.dp),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Column {
//                Text("Total Amount", color = Color.Gray)
//                Text(
//                    "₹${state.finalAmount ?: state.totalPrice ?: "0"}",
//                    fontSize = 20.sp,
//                    fontWeight = FontWeight.Bold
//                )
//            }
//
//            if (paymentState is PaymentState.Processing) {
//                CircularProgressIndicator(
//                    modifier = Modifier.size(36.dp),
//                    strokeWidth = 3.dp
//                )
//            } else {
//                // ✅ CORRECT: Navigate to PaymentScreen
//                Button(
//                    onClick = {
//                        navigator.push(PaymentScreen())
//                    },
//                    shape = RoundedCornerShape(30.dp)
//                ) {
//                    Text("Checkout")
//                }
//            }
//        }
//    }
//
//
////    if (showUpiSheet) {
////        UpiBottomSheet(
////            amount = state.finalAmount ?: state.totalPrice ?: "0",
////            onAppClick = { app ->
////
////                // ✅ Close sheet immediately
////                showUpiSheet = false
////
////                viewModel.createOrder { txnId ->
////
////
////                    launchUpiPayment(
////                        activity = context,
////                        app = app,
////                        amount = state.finalAmount ?: state.totalPrice ?: "0",
////                        txnId = txnId
////                    ) {
////                        viewModel.markPaymentPending(txnId)
////                    }
////
////                }
////            },
////            onDismiss = { showUpiSheet = false }
////        )
////    }
//
//}


@Composable
fun CheckoutBottomBar(viewModel: ShopViewModel) {
    val state by viewModel.state.collectAsState()
    val paymentState by viewModel.paymentState.collectAsState()
    val navigator = LocalNavigator.currentOrThrow

    // 👇 Android-only, SAFE
//    val context = remember {
//        runCatching { getPlatformContext() }.getOrNull()
//    }

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

//@Composable
//fun CheckoutBottomBar(viewModel: ShopViewModel) {
//
//    val state by viewModel.state.collectAsState()
//    val paymentState by viewModel.paymentState.collectAsState()
//
//    var showUpiSheet by remember { mutableStateOf(false) } // 👈 OUTSIDE
//
//    Box(
//        Modifier
//            .fillMaxWidth()
//            .padding(16.dp)
//    ) {
//        Row(
//            Modifier
//                .fillMaxWidth()
//                .clip(RoundedCornerShape(30.dp))
//                .background(Color.White)
//                .padding(16.dp),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//
//            Column {
//                Text("Total Amount", color = Color.Gray)
//                Text(
//                    "₹${state.finalAmount ?: state.totalPrice ?: "0"}",
//                    fontSize = 20.sp,
//                    fontWeight = FontWeight.Bold
//                )
//            }
//
//            if (paymentState is PaymentState.Processing) {
//                CircularProgressIndicator(
//                    modifier = Modifier.size(36.dp),
//                    strokeWidth = 3.dp
//                )
//            } else {
//                Button(
//                    onClick = { showUpiSheet = true },
//                    shape = RoundedCornerShape(30.dp)
//                ) {
//                    Text("Checkout")
//                }
//            }
//        }
//    }
//
//    if (showUpiSheet) {
//        UpiBottomSheet(
//            amount = state.finalAmount ?: state.totalPrice ?: "0",
//            onAppClick = { app ->
//
//                // ✅ Close sheet immediately
//                showUpiSheet = false
//
//                viewModel.createOrder { txnId ->
//
//                    launchUpiPayment(
//                        app = app,
//                        amount = state.finalAmount ?: state.totalPrice ?: "0",
//                        txnId = txnId
//                    ) { opened ->
//
//                        if (opened) {
//                            // ✅ ONLY mark pending
//                            viewModel.markPaymentPending(txnId)
//                        } else {
//                            // ❌ UPI app not found / intent failed
//                            viewModel.paymentFailed("No UPI app found")
//                        }
//                    }
//                }
//            },
//            onDismiss = { showUpiSheet = false }
//        )
//    }
//}



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
@Composable
fun UpiBottomSheet(
    amount: String,
    onAppClick: (UpiApp) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {

                Text(
                    "Pay via UPI",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(Modifier.height(12.dp))

                UPI_APPS.forEach { app ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onAppClick(app) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(app.name, fontSize = 16.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    "UPI ID: 9990758825@ptyes",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }
    }
}