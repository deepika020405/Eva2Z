package com.developer.ev2z.ui.screens.market_screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import coil3.compose.AsyncImage
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import cafe.adriel.voyager.navigator.LocalNavigator
import com.developer.ev2z.util.Analytics

class ProductDetailsScreen(
    val product: ShopProduct,
    val onToggleFavorite: (ShopProduct) -> Unit
) : Screen {

    @Composable
    override fun Content() {
        val viewModel = ShopViewModelProvider.viewModel
        ProductDetailsContent(
            product = product,
            viewModel = viewModel,
            onToggleFavorite = {
                onToggleFavorite(product)
            }
        )
    }

}

@Composable
fun ProductDetailsContent(
    product: ShopProduct,
    viewModel: ShopViewModel,
    onToggleFavorite: () -> Unit,
) {
    val navigator = LocalNavigator.current

    var selectedType by remember { mutableStateOf<String?>(null) }
    var withRecharge by remember { mutableStateOf(false) }

    val finalPrice = product.price + if (withRecharge) product.rechargePrice else 0.0
    val fakeOldPrice = (finalPrice * 1.5).toInt() // Example old price = +50%
    val discountPercent = (((fakeOldPrice - finalPrice) / fakeOldPrice) * 100).toInt()
    var showDeviceTypePrompt by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Analytics.logEvent(
            name = "screen_view",
            params = mapOf(
                "screen_name" to "product_details",
                "screen_class" to "ProductDetailsScreen",
                "item_id" to product.id,
                "item_name" to product.title,
                "item_category" to "iot_devices"
            )
        )
    }

    Box(Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 90.dp) // space for Add to Cart button
        ) {

            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navigator?.pop() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Product Details", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }

            // Product image
            AsyncImage(
                model = product.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Column(Modifier.padding(16.dp)) {

                // Title
                Text(product.title, fontSize = 22.sp, fontWeight = FontWeight.Bold)

                Spacer(Modifier.height(8.dp))

                // Price Row (Fake discount)
                Row(verticalAlignment = Alignment.CenterVertically) {

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1B9F4A))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Rs. ${finalPrice.toInt()}", color = Color.White, fontSize = 16.sp)
                    }

                    Spacer(Modifier.width(10.dp))

                    Text(
                        "Rs. $fakeOldPrice",
                        fontSize = 14.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Medium,
                        textDecoration = TextDecoration.LineThrough
                    )

                    Spacer(Modifier.width(10.dp))

                    Text(
                        "${discountPercent}% OFF",
                        color = Color(0xFF1B9F4A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Spacer(Modifier.width(10.dp))
                    Column(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .width(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (product.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (product.isFavorite) Color(0xFFFF5252) else Color(0xFFCCCCCC),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { onToggleFavorite() }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Quick Info (7 Days Replacement + 24/7 Support)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoBox(
                        title = "7-Days",
                        subtitle = "Replacement",
                        modifier = Modifier.weight(1f)
                    )
                    InfoBox(
                        title = "24/7",
                        subtitle = "Customer Support",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Recharge option
                if (product.rechargePrice > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = withRecharge,
                            onCheckedChange = { withRecharge = it
                                Analytics.logEvent(
                                    name = "select_item",
                                    params = mapOf(
                                        "item_id" to product.id,
                                        "item_name" to product.title,
                                        "selection_type" to "recharge",
                                        "selection_value" to if (it) "with_recharge" else "without_recharge"
                                    )
                                )
                            }
                        )
                        Text("With Recharge (+₹${product.rechargePrice.toInt()})")
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Device type
                if (product.deviceType != "none") {
                    Text("Select Device Type", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))

                    Row {
                        FilterChip(
                            selected = selectedType == "android",
                            onClick = {
                                selectedType = "android"

                                Analytics.logEvent(
                                    name = "select_item",
                                    params = mapOf(
                                        "item_id" to product.id,
                                        "item_name" to product.title,
                                        "selection_type" to "device_type",
                                        "selection_value" to "android"
                                    )
                                )
                            },
                            label = { Text("Android") }
                        )
                        Spacer(Modifier.width(12.dp))
                        FilterChip(
                            selected = selectedType == "ios",
                            onClick = {
                                selectedType = "ios"

                                Analytics.logEvent(
                                    name = "select_item",
                                    params = mapOf(
                                        "item_id" to product.id,
                                        "item_name" to product.title,
                                        "selection_type" to "device_type",
                                        "selection_value" to "ios"
                                    )
                                )
                            },
                            label = { Text("iOS") }
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Description
                Text("Description", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(product.description, fontSize = 14.sp, color = Color.DarkGray)

                Spacer(Modifier.height(16.dp))

                // Features
                Text("Features", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))

                product.features.forEach {
                    Text("• $it", fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                }
            }
        }

        // BOTTOM FIXED BUTTON (Now Visible Always)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Button(
//                enabled = product.deviceType == "none" || selectedType != null,
                enabled = true,
                onClick = {

                    // 🔴 Device type required but not selected
                    if (product.deviceType != "none" && selectedType == null) {
                        showDeviceTypePrompt = true
                        return@Button
                    }

                    val planType =
                        if (product.id == 10 || product.id == 11) {
                            if (withRecharge) "with_recharge" else "without_recharge"
                        } else null

                    Analytics.logEvent(
                        name = "add_to_cart",
                        params = mapOf(
                            "currency" to "INR",
                            "value" to finalPrice,
                            "item_id" to product.id,
                            "item_name" to product.title,
                            "item_category" to "iot_devices",
                            "item_variant" to (selectedType ?: "default"),
                            "price" to finalPrice,
                            "quantity" to 1,
                            "with_recharge" to withRecharge,
                            "recharge_price" to product.rechargePrice,
                            "device_type" to (selectedType ?: ""),
                            "promo_code" to product.promoCode
                        )
                    )

                    viewModel.addToCart(
                        add = "add",
                        productId = product.id,
                        quantity = 1,
                        plan_type = planType ?: "",
                        device_type = selectedType ?: ""
                    )

                    navigator?.pop()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B9F4A)),
                shape = RoundedCornerShape(26.dp)
            ) {
                Text("Add to Cart", fontSize = 16.sp)
            }
        }

        LaunchedEffect(Unit) {
            Analytics.logEvent(
                name = "view_item",
                params = mapOf(
                    "currency" to "INR",
                    "value" to product.price,
                    "item_id" to product.id,
                    "item_name" to product.title,
                    "item_category" to "iot_devices",
                    "item_variant" to product.deviceType
                )
            )
        }
    }
    if (showDeviceTypePrompt) {
        AlertDialog(
            onDismissRequest = { showDeviceTypePrompt = false },
            confirmButton = {
                TextButton(onClick = { showDeviceTypePrompt = false }) {
                    Text("OK")
                }
            },
            title = {
                Text("Device Type Required")
            },
            text = {
                Text("Please select device type (Android or iOS) to continue.")
            }
        )
    }
}


@Composable
fun InfoBox(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFEFF7F0))
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = Color(0xFF1B9F4A), fontWeight = FontWeight.Bold)
            Text(subtitle, fontSize = 12.sp, color = Color.Black)
        }
    }
}