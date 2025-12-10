package com.developer.ev2z.ui.screens.market_screen

import androidx.compose.foundation.Image
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

    // Local screen states
    var selectedType by remember { mutableStateOf<String?>(null) }
    var withRecharge by remember { mutableStateOf(false) }

    val finalPrice = product.price + if (withRecharge) product.rechargePrice else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {

        // Top bar
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

        // Product Image
        AsyncImage(
            model = product.imageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp))
        )

        Spacer(Modifier.height(12.dp))

        // Product Details
        Column(modifier = Modifier.padding(16.dp)) {
            Text(product.title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                "₹${finalPrice.toInt()}",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1B9F4A)
            )

            Text("★★★★★ (${product.ratingCount})", color = Color(0xFFFFC107))
            Spacer(Modifier.height(16.dp))

            // Recharge Option
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

            // Device Type Selection (Android / iOS)
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
            Spacer(Modifier.height(8.dp))
            Text(
                product.description,
                fontSize = 14.sp,
                color = Color.DarkGray
            )

            Spacer(Modifier.height(16.dp))

            // Features Section
            Text("Features", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            product.features.forEach { feature ->
                Text("• $feature", fontSize = 14.sp, color = Color.Black)
                Spacer(Modifier.height(6.dp))
            }

            Spacer(Modifier.height(100.dp)) // Space for bottom button
        }
    }

    // BOTTOM: Add to Cart Button
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Button(
            enabled = product.deviceType == "none" || selectedType != null,
            onClick = {
//                viewModel.addToCart(
//                    productId = product.id,
//                    withRecharge = withRecharge,
//                    deviceType = selectedType
//                )
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