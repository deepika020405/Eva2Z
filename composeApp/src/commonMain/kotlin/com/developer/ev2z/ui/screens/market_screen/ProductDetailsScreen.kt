package com.developer.ev2z.ui.screens.market_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import coil3.compose.AsyncImage
import com.developer.ev2z.util.Analytics

class ProductDetailsScreen(
    val product: ShopProduct,
    val onToggleFavorite: (ShopProduct) -> Unit
) : Screen {
    @Composable
    override fun Content() {
        val viewModel = ShopViewModelProvider.viewModel
        ProductDetailsContent(product = product, viewModel = viewModel,
            onToggleFavorite = { onToggleFavorite(product) })
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
    var showDeviceTypePrompt by remember { mutableStateOf(false) }

    val finalPrice = product.price + if (withRecharge) product.rechargePrice else 0.0
    val fakeOld = (finalPrice * 1.5).toInt()
    val disc = (((fakeOld - finalPrice) / fakeOld) * 100).toInt()
    val savings = fakeOld - finalPrice.toInt()

    LaunchedEffect(Unit) {
        Analytics.logEvent(
            "screen_view", mapOf(
                "screen_name" to "product_details", "item_id" to product.id,
                "item_name" to product.title
            )
        )
        Analytics.logEvent(
            "view_item", mapOf(
                "currency" to "INR",
                "value" to product.price, "item_id" to product.id
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(P_Bg)) {
        Column(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {

            // ── Hero ──────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth().height(320.dp)
                    .background(Color.White)
            ) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(28.dp),
                    contentScale = ContentScale.Fit
                )
                // Soft bottom fade
                Box(
                    modifier = Modifier.fillMaxWidth().height(60.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, P_Bg)
                            )
                        )
                )
                // Nav buttons
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(Color.White)
                            .shadow(4.dp, CircleShape)
                            .clickable { navigator?.pop() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ArrowBack, null,
                            tint = P_Dark, modifier = Modifier.size(18.dp)
                        )
                    }
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(Color.White)
                            .shadow(4.dp, CircleShape)
                            .clickable { onToggleFavorite() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (product.isFavorite) Icons.Default.Favorite
                            else Icons.Default.FavoriteBorder,
                            null,
                            tint = if (product.isFavorite) P_Red else P_Light,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                // Discount pill
                Box(
                    modifier = Modifier.align(Alignment.BottomStart)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(P_Red)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "$disc% OFF", fontSize = 12.sp,
                        fontWeight = FontWeight.Bold, color = Color.White
                    )
                }
            }

            // ── Content ───────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(8.dp))

                if (product.subtitle.isNotBlank()) {
                    Text(
                        product.subtitle.uppercase(), fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold, color = P_Green,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(Modifier.height(4.dp))
                }

                Text(
                    product.title, fontSize = 22.sp,
                    fontWeight = FontWeight.Bold, color = P_Dark,
                    lineHeight = 28.sp
                )

                Spacer(Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row {
                        repeat(5) {
                            Icon(
                                Icons.Default.Star, null,
                                tint = P_Gold, modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${product.rating}", fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, color = P_Mid
                    )
                    Text(
                        "  ·  ${product.ratingCount} reviews",
                        fontSize = 13.sp, color = P_Light
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ── Price Block ───────────────────────────────────
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = P_Card),
                    elevation = CardDefaults.cardElevation(0.dp),
                    modifier = Modifier.fillMaxWidth()
                        .border(1.dp, P_Border, RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "₹${finalPrice.toInt()}",
                                fontSize = 32.sp, fontWeight = FontWeight.Black,
                                color = P_Dark
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "₹$fakeOld", fontSize = 15.sp,
                                    color = P_Light,
                                    textDecoration = TextDecoration.LineThrough
                                )
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(P_GreenSurface)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "Save ₹$savings",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = P_Green
                                    )
                                }
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(P_GreenSurface)
                                    .border(1.dp, P_GreenBorder, RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    "Best\nPrice", fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = P_Green, textAlign = TextAlign.Center,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Trust Badges ──────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        Pair("↩", "7-Day Return"),
                        Pair("◎", "24/7 Support"),
                        Pair("⬆", "Free Shipping")
                    ).forEach { (icon, label) ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = P_Card),
                            elevation = CardDefaults.cardElevation(0.dp),
                            modifier = Modifier.weight(1f)
                                .border(1.dp, P_Border, RoundedCornerShape(12.dp))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(icon, fontSize = 18.sp, color = P_Green)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    label, fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = P_Mid, textAlign = TextAlign.Center,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Recharge Option ───────────────────────────────
                if (product.rechargePrice > 0) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = P_Card),
                        elevation = CardDefaults.cardElevation(0.dp),
                        modifier = Modifier.fillMaxWidth()
                            .border(1.dp, P_Border, RoundedCornerShape(14.dp))
                            .clickable { withRecharge = !withRecharge }) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = withRecharge,
                                onCheckedChange = {
                                    withRecharge = it
                                    Analytics.logEvent(
                                        "select_item", mapOf(
                                            "item_id" to product.id, "selection_type" to "recharge",
                                            "selection_value" to if (it) "with_recharge" else "without_recharge"
                                        )
                                    )
                                },
                                colors = CheckboxDefaults.colors(checkedColor = P_Green)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Annual Recharge Plan",
                                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                    color = P_Dark
                                )
                                Text(
                                    "Keep tracking active for 1 year",
                                    fontSize = 12.sp, color = P_Light
                                )
                            }
                            Text(
                                "+₹${product.rechargePrice.toInt()}",
                                fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                color = P_Green
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // ── Device Type ───────────────────────────────────
                if (product.deviceType != "none") {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = P_Card),
                        elevation = CardDefaults.cardElevation(0.dp),
                        modifier = Modifier.fillMaxWidth()
                            .border(1.dp, P_Border, RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Platform", fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = P_Light, letterSpacing = 0.8.sp
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf(
                                    "android" to "Android",
                                    "ios" to "iOS"
                                ).forEach { (key, label) ->
                                    val sel = selectedType == key
                                    Box(
                                        modifier = Modifier.weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(
                                                1.5.dp,
                                                if (sel) P_Green else P_Border,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .background(if (sel) P_GreenSurface else Color.Transparent)
                                            .clickable {
                                                selectedType = key
                                                Analytics.logEvent(
                                                    "select_item", mapOf(
                                                        "item_id" to product.id,
                                                        "selection_type" to "device_type",
                                                        "selection_value" to key
                                                    )
                                                )
                                            }
                                            .padding(vertical = 14.dp),
                                        contentAlignment = Alignment.Center) {
                                        Text(
                                            label, fontSize = 14.sp,
                                            fontWeight = if (sel) FontWeight.SemiBold
                                            else FontWeight.Normal,
                                            color = if (sel) P_Green else P_Mid
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // ── Description ───────────────────────────────────
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = P_Card),
                    elevation = CardDefaults.cardElevation(0.dp),
                    modifier = Modifier.fillMaxWidth()
                        .border(1.dp, P_Border, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            "About this product", fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold, color = P_Dark
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            product.description, fontSize = 14.sp,
                            color = P_Mid, lineHeight = 23.sp
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Features ──────────────────────────────────────
                if (product.features.isNotEmpty()) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = P_Card),
                        elevation = CardDefaults.cardElevation(0.dp),
                        modifier = Modifier.fillMaxWidth()
                            .border(1.dp, P_Border, RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                "Key Features", fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold, color = P_Dark
                            )
                            Spacer(Modifier.height(12.dp))
                            product.features.forEach { feat ->
                                Row(
                                    modifier = Modifier.padding(vertical = 5.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier.size(20.dp)
                                            .clip(CircleShape)
                                            .background(P_GreenSurface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Check, null,
                                            tint = P_Green,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        feat, fontSize = 14.sp,
                                        color = P_Mid, lineHeight = 21.sp,
                                        modifier = Modifier.padding(top = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Add to Cart ───────────────────────────────────────────
        // ── Add to Cart ───────────────────────────────────────────────────
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            color = P_Card,
            shadowElevation = 20.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                // ✅ navigationBarsPadding() hatao
            ) {
                Button(
                    onClick = {
                        if (product.deviceType != "none" && selectedType == null) {
                            showDeviceTypePrompt = true; return@Button
                        }
                        val planType = if (product.id == 10 || product.id == 11) {
                            if (withRecharge) "with_recharge" else "without_recharge"
                        } else null
                        Analytics.logEvent(
                            "add_to_cart", mapOf(
                                "currency" to "INR", "value" to finalPrice,
                                "item_id" to product.id, "item_name" to product.title,
                                "price" to finalPrice, "quantity" to 1,
                                "device_type" to (selectedType ?: ""),
                                "promo_code" to product.promoCode
                            )
                        )
                        viewModel.addToCart(
                            "add", product.id, 1,
                            planType ?: "", selectedType ?: ""
                        )
                        navigator?.pop()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = P_Green),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.ShoppingCart, // ✅ ShoppingBag replace
                            null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Add to Cart  ·  ₹${finalPrice.toInt()}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }


            if (showDeviceTypePrompt) {
                AlertDialog(
                    onDismissRequest = { showDeviceTypePrompt = false },
                    confirmButton = {
                        TextButton(onClick = { showDeviceTypePrompt = false }) {
                            Text("Got it", color = P_Green, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    title = { Text("Select Platform", fontWeight = FontWeight.Bold) },
                    text = { Text("Please choose Android or iOS to continue.") },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        @Composable
        fun InfoBox(title: String, subtitle: String, modifier: Modifier = Modifier) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = P_Card),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = modifier.border(1.dp, P_Border, RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = P_Green)
                    Text(subtitle, fontSize = 11.sp, color = P_Mid, textAlign = TextAlign.Center)
                }
            }
        }
    }
}