package com.developer.ev2z.ui.screens.market_screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import coil3.compose.AsyncImage
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
            Text(product.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(product.priceText, fontSize = 18.sp, color = Color(0xFF1B9F4A))
            Text("★★★★★ (${product.ratingCount})", color = Color(0xFFFFC107))
            Spacer(Modifier.height(10.dp))

            Text("Description", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(product.subtitle, fontSize = 14.sp, color = Color.DarkGray)
        }
    }

    // BOTTOM: Add to Cart button
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = {
                viewModel.addToCart(product.id.toInt())
                navigator?.pop() // Go back after adding OR navigate to cart
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B9F4A)),
            shape = RoundedCornerShape(26.dp)
        ) {
            Text("Add to Cart", fontSize = 16.sp)
        }
    }
}