package com.developer.ev2z.ui.screens.market_screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import cafe.adriel.voyager.navigator.LocalNavigator

class ProductDetailsScreen(
    private val product: ShopProduct,
    private val viewModel: ShopViewModel
) : Screen {

    @Composable
    override fun Content() {
        ProductDetailsContent(product, viewModel)
    }
}

@Composable
fun ProductDetailsContent(
    product: ShopProduct,
    viewModel: ShopViewModel
) {
    val navigator = LocalNavigator.current

    var selectedType by remember { mutableStateOf<String?>(null) }
    var withRecharge by remember { mutableStateOf(false) }

    val finalPrice = product.price + if (withRecharge) product.rechargePrice else 0.0
    val fakeOldPrice = (finalPrice * 1.5).toInt() // Example old price = +50%
    val discountPercent = (((fakeOldPrice - finalPrice) / fakeOldPrice) * 100).toInt()

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
                            onCheckedChange = { withRecharge = it }
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
                            onClick = { selectedType = "android" },
                            label = { Text("Android") }
                        )
                        Spacer(Modifier.width(12.dp))
                        FilterChip(
                            selected = selectedType == "ios",
                            onClick = { selectedType = "ios" },
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
                enabled = product.deviceType == "none" || selectedType != null,
                onClick = {
                    val planType = if (withRecharge) "with_recharge" else "without_recharge"

                    viewModel.addToCart(
                        productId = product.id,
                        quantity = 1,
                        plan_type = planType,
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