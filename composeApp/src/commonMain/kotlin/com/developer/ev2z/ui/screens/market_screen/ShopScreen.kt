package com.developer.ev2z.ui.screens.market_screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import coil3.compose.AsyncImage
import com.developer.ev2z.ui.components.FloatingContactButton
import com.developer.ev2z.util.Analytics
import com.developer.ev2z.util.DeepLinkBus
import eva2z_connect_multiplatform.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource

// ── Design Tokens — light green + white theme ─────────────────────
internal val P_Green        = Color(0xFF6A9F50)   // light green primary
internal val P_GreenLight   = Color(0xFF85B070)   // lighter green
internal val P_GreenSurface = Color(0xFFF0F7EC)   // very light green tint
internal val P_GreenBorder  = Color(0xFFD4E4CD)   // soft green border
internal val P_Bg           = Color(0xFFF9FBF7)   // near-white with green hint
internal val P_Card         = Color(0xFFFFFFFF)    // pure white
internal val P_Dark         = Color(0xFF1C2418)    // dark text
internal val P_Mid          = Color(0xFF5D6858)    // secondary text
internal val P_Light        = Color(0xFFA0A99B)    // hint text
internal val P_Border       = Color(0xFFECF0E8)    // light border
internal val P_Red          = Color(0xFFE5565B)    // app Red
internal val P_Gold         = Color(0xFFF4C44E)    // app Yellow


private fun getRatingCount(productId: Int): Int = when (productId) {
    1 -> 128
    2 -> 94
    3 -> 213
    4 -> 67
    5 -> 156
    6 -> 89
    7 -> 174
    8 -> 102
    9 -> 231
    10 -> 78
    11 -> 143
    else -> 50
}

private fun getRating(productId: Int): Double = when (productId) {
    1 -> 4.7
    2 -> 4.5
    3 -> 4.9
    4 -> 4.3
    5 -> 4.8
    6 -> 4.4
    7 -> 4.6
    8 -> 4.2
    9 -> 4.9
    10 -> 4.5
    11 -> 4.7
    else -> 4.8
}

// ── Shop Screen ───────────────────────────────────────────────────
@Composable
fun ShopScreen(
    products: List<ShopProduct>,
    cartCount: Int,
    onBackClick: () -> Unit = {},
    onToggleFavorite: (ShopProduct) -> Unit = {},
    onPlaceOrder: (ShopProduct) -> Unit = {},
    onCartClick: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize().background(P_Bg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            item { PremiumTopBar(cartCount, onCartClick) }
            item { PremiumBanner() }
            item { PremiumSectionHeader(products.size) }
            items(products) { product ->
                PremiumProductCard(
                    product = product,
                    onToggleFavorite = { onToggleFavorite(product) },
                    onPlaceOrder = { onPlaceOrder(product) }
                )
                Spacer(Modifier.height(16.dp))
            }
        }

        FloatingContactButton(
            phoneNumber = "919755273822",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 80.dp)
        )

        PremiumCartBar(count = cartCount, onClick = onCartClick,
            modifier = Modifier.align(Alignment.BottomCenter))
    }
}

// ── Top Bar ────────────────────────────────────────────────────────
@Composable
private fun PremiumTopBar(cartCount: Int, onCartClick: () -> Unit) {
    Surface(color = P_Card, shadowElevation = 0.dp) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            Brush.linearGradient(listOf(P_Green, P_GreenLight))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("E", color = Color.White,
                        fontWeight = FontWeight.Black, fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("EVA2Z Store",
                        fontSize = 17.sp, fontWeight = FontWeight.Bold, color = P_Dark)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) { Text("★", fontSize = 10.sp, color = P_Gold) }
                        Spacer(Modifier.width(4.dp))
                        Text("4.8  ·  Trusted by 10K+",
                            fontSize = 11.sp, color = P_Light)
                    }
                }
                // Cart
                Box {
                    IconButton(
                        onClick = onCartClick,
                        modifier = Modifier.size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(P_Bg)
                    ) {
                        Icon(Icons.Outlined.ShoppingBag, null,
                            tint = P_Dark, modifier = Modifier.size(22.dp))
                    }
                    if (cartCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(17.dp).clip(CircleShape)
                                .background(P_Red)
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-2).dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(cartCount.toString(),
                                fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }
            }
            HorizontalDivider(color = P_Border, thickness = 0.5.dp)
        }
    }
}

// ── Banner ─────────────────────────────────────────────────────────
@Composable
private fun PremiumBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .height(160.dp),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF5A8F42), P_Green, P_GreenLight),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(800f, 400f)
                    )
                )
        ) {
            // Decorative rings
            Box(
                Modifier.size(100.dp).offset(x = 250.dp, y = (-15).dp)
                    .border(2.dp, Color.White.copy(alpha = 0.08f), CircleShape)
            )
            Box(
                Modifier.size(60.dp).offset(x = 270.dp, y = 90.dp)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
            )

            Row(
                modifier = Modifier.fillMaxSize()
                    .padding(horizontal = 22.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "INDIA'S #1 GPS",
                            fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold,
                            color = Color.White, letterSpacing = 1.2.sp
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Track Smarter.\nDrive Safer.",
                        fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                        color = Color.White, lineHeight = 26.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Trusted by 10,000+ users",
                        fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f)
                    )
                }
                Box(
                    modifier = Modifier.size(56.dp)
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.GpsFixed, null,
                        tint = Color.White, modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

// ── Section Header ─────────────────────────────────────────────────
@Composable
private fun PremiumSectionHeader(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("All Products",
                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = P_Dark)
            Text("$count items available",
                fontSize = 13.sp, color = P_Light)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, P_Border, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.FilterList, null,
                    tint = P_Mid, modifier = Modifier.size(14.dp))
                Text("Filter", fontSize = 12.sp,
                    fontWeight = FontWeight.Medium, color = P_Mid)
            }
        }
    }
}

// ── Premium Product Card ───────────────────────────────────────────
@Composable
private fun PremiumProductCard(
    product: ShopProduct,
    onToggleFavorite: () -> Unit,
    onPlaceOrder: () -> Unit
) {
    val fakeOld = (product.price * 1.5).toInt()
    val disc = (((fakeOld - product.price) / fakeOld) * 100).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(6.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x14000000))
            .clickable { onPlaceOrder() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = P_Card),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column {
            // Image area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color(0xFFF9FAFB))
            ) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    contentScale = ContentScale.Fit,
                    placeholder = painterResource(Res.drawable.app_logo),
                    error = painterResource(Res.drawable.app_logo)
                )
                // Discount tag
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(P_Red)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("$disc% OFF", fontSize = 10.sp,
                        fontWeight = FontWeight.Bold, color = Color.White)
                }
                // Fav button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .shadow(2.dp, CircleShape)
                        .clickable { onToggleFavorite() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (product.isFavorite) Icons.Default.Favorite
                        else Icons.Default.FavoriteBorder,
                        null,
                        tint = if (product.isFavorite) P_Red else P_Light,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Info area
            Column(modifier = Modifier.padding(16.dp)) {
                // Subtitle chip
                if (product.subtitle.isNotBlank()) {
                    Text(
                        product.subtitle.uppercase(),
                        fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                        color = P_Green, letterSpacing = 0.8.sp
                    )
                    Spacer(Modifier.height(4.dp))
                }

                Text(
                    product.title,
                    fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = P_Dark, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    lineHeight = 22.sp
                )

                Spacer(Modifier.height(10.dp))

                // Rating
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) {
                        Icon(Icons.Default.Star, null,
                            tint = P_Gold, modifier = Modifier.size(13.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("${product.rating}",
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = P_Mid)
                    Text("  (${product.ratingCount})",
                        fontSize = 12.sp, color = P_Light)
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = P_Border, thickness = 0.5.dp)
                Spacer(Modifier.height(12.dp))

                // Price + CTA row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("₹${product.price.toInt()}",
                            fontSize = 22.sp, fontWeight = FontWeight.Black, color = P_Dark)
                        Text("₹$fakeOld",
                            fontSize = 13.sp, color = P_Light,
                            textDecoration = TextDecoration.LineThrough)
                    }
                    Button(
                        onClick = onPlaceOrder,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = P_Green),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Text("View Details",
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ── Cart Bar ───────────────────────────────────────────────────────
@Composable
private fun PremiumCartBar(count: Int, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = P_Card,
        shadowElevation = 8.dp,
        tonalElevation = 0.dp
    ) {
        // ✅ navigationBarsPadding() hatao — bottom nav khud handle karta hai
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .height(46.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = P_Green),
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ShoppingCart, null,
                        modifier = Modifier.size(17.dp))
                    Text("View Cart", fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("$count items", fontSize = 11.sp,
                        fontWeight = FontWeight.Medium, color = Color.White)
                }
            }
        }
    }
}

// ── ShopScreenHost — Logic unchanged ──────────────────────────────
@Composable
fun ShopScreenHost(category: String? = null) {
    val viewModel = ShopViewModelProvider.viewModel
    val state by viewModel.state.collectAsState()
    val navigator = LocalNavigator.current
    val deepLinkDestination by DeepLinkBus.navigateTo.collectAsState(initial = "") // ✅ ADD

    val filteredProducts = state.products.filter { dto ->
        when (category) {
            "wired" -> dto.device_type?.lowercase() == "wired"
            "wireless" -> dto.device_type?.lowercase() == "wireless"
            else -> true
        }
    }

    LaunchedEffect(Unit) {
        Analytics.logEvent("screen_view", mapOf(
            "screen_name" to "shop_screen", "screen_class" to "ShopScreen",
            "item_category" to "iot_devices", "source" to "home_or_menu"))
        viewModel.loadData()
    }

    // ✅ Product deeplink handle
    LaunchedEffect(deepLinkDestination, state.products) {
        val dest = deepLinkDestination ?: return@LaunchedEffect

        // ✅ Dono prefix handle karo
        if (!dest.startsWith("open_product_") && !dest.startsWith("product_detail_")) return@LaunchedEffect

        // ✅ Dono se pid nikalo
        val pid = when {
            dest.startsWith("open_product_") -> dest.removePrefix("open_product_")
                .substringBefore("_").toIntOrNull()
            dest.startsWith("product_detail_") -> dest.removePrefix("product_detail_")
                .substringBefore("_").toIntOrNull()
            else -> null
        } ?: return@LaunchedEffect

        if (state.products.isEmpty()) {
            viewModel.loadData()
            return@LaunchedEffect
        }

        val dto = state.products.firstOrNull { it.id == pid } ?: return@LaunchedEffect

        val product = ShopProduct(
            id = dto.id, title = dto.name ?: "",
            subtitle = dto.device_type ?: "",
            price = dto.sale_price?.toDoubleOrNull() ?: 0.0,
            rechargePrice = dto.recharge_price?.toDoubleOrNull() ?: 0.0,
            promoAmount = dto.promo_amt?.toDoubleOrNull() ?: 0.0,
            promoCode = dto.promo_code ?: "",
            imageUrl = dto.image ?: "",
            description = dto.description ?: "",
            features = listOfNotNull(
                dto.features1, dto.features2, dto.features3,
                dto.features4, dto.features5, dto.features6, dto.features7
            ),
            deviceType = dto.device_type ?: "",
            rating = getRating(dto.id), ratingCount = getRatingCount(dto.id),
            isFavorite = viewModel.isFavorite(dto.id)
        )

        DeepLinkBus.clear()

        navigator?.push(
            ProductDetailsScreen(
                product = product,
                onToggleFavorite = { p -> viewModel.toggleWishlist(p.id) }
            )
        )
    }

    if (state.loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = P_Green, strokeWidth = 2.dp)
        }
        return
    }

    LaunchedEffect(state.products) {
        if (state.products.isNotEmpty()) {
            Analytics.logEvent("view_item_list", mapOf(
                "item_list_id" to "shop_device_list",
                "item_list_name" to "Location Tracker Device",
                "currency" to "INR",
                "items" to state.products.mapIndexed { index, p ->
                    mapOf("item_id" to p.id.toString(), "item_name" to (p.name ?: ""),
                        "item_brand" to "EVA2Z", "item_category" to "iot_devices",
                        "item_variant" to (p.device_type ?: ""),
                        "price" to (p.sale_price?.toDoubleOrNull() ?: 0.0),
                        "index" to index).toString()
                }))
        }
    }

    ShopScreen(
        products = filteredProducts.map { dto ->
            ShopProduct(
                id = dto.id, title = dto.name ?: "", subtitle = dto.device_type ?: "",
                price = dto.sale_price?.toDoubleOrNull() ?: 0.0,
                rechargePrice = dto.recharge_price?.toDoubleOrNull() ?: 0.0,
                promoAmount = dto.promo_amt?.toDoubleOrNull() ?: 0.0,
                promoCode = dto.promo_code ?: "", imageUrl = dto.image ?: "",
                description = dto.description ?: "",
                features = listOfNotNull(dto.features1, dto.features2, dto.features3,
                    dto.features4, dto.features5, dto.features6, dto.features7),
                deviceType = dto.device_type ?: "", rating = getRating(dto.id), ratingCount = getRatingCount(dto.id),
                isFavorite = viewModel.isFavorite(dto.id))
        },
        cartCount = state.cartCount,
        onToggleFavorite = { p ->
            viewModel.toggleWishlist(p.id.toInt())
            Analytics.logEvent("add_to_wishlist", mapOf("currency" to "INR",
                "items" to listOf(mapOf("item_id" to p.id.toString(),
                    "item_name" to p.title, "item_brand" to "EVA2Z",
                    "item_category" to "iot_devices", "item_variant" to p.subtitle,
                    "price" to p.price))))
        },
        onCartClick = { navigator?.push(CartScreen()) },
        onPlaceOrder = { product ->
            navigator?.push(ProductDetailsScreen(product = product,
                onToggleFavorite = { p ->
                    viewModel.toggleWishlist(p.id)
                    Analytics.logEvent("add_to_wishlist", mapOf("currency" to "INR",
                        "items" to listOf(mapOf("item_id" to p.id.toString(),
                            "item_name" to p.title, "item_brand" to "EVA2Z",
                            "item_category" to "iot_devices",
                            "item_variant" to p.subtitle, "price" to p.price))))
                }))
        }
    )
}

// Aliases for other screens in market_screen folder
internal val ShopGreen = P_Green
internal val ShopGreenDark = P_Green
internal val ShopGreenSoft = P_GreenSurface
internal val ShopGreenMid = P_GreenBorder
internal val ShopBg = P_Bg
internal val ShopCard = P_Card
internal val ShopTextPrimary = P_Dark
internal val ShopTextSec = P_Mid
internal val ShopTextTert = P_Light
internal val ShopError = P_Red

// ══════════════════════════════════════════════════════════════════
// COMMENTED OUT: NEW WIRED/WIRELESS UI CODE (for reference)
// ══════════════════════════════════════════════════════════════════

// 
// import androidx.compose.foundation.Image
// import androidx.compose.foundation.background
// import androidx.compose.foundation.border
// import androidx.compose.foundation.clickable
// import androidx.compose.foundation.layout.*
// import androidx.compose.foundation.lazy.LazyColumn
// import androidx.compose.foundation.lazy.items
// import androidx.compose.foundation.shape.CircleShape
// import androidx.compose.foundation.shape.RoundedCornerShape
// import androidx.compose.material.icons.Icons
// import androidx.compose.material.icons.filled.*
// import androidx.compose.material.icons.outlined.*
// import androidx.compose.material3.*
// import androidx.compose.runtime.*
// import androidx.compose.ui.Alignment
// import androidx.compose.ui.Modifier
// import androidx.compose.ui.draw.clip
// import androidx.compose.ui.draw.shadow
// import androidx.compose.ui.graphics.Brush
// import androidx.compose.ui.graphics.Color
// import androidx.compose.ui.layout.ContentScale
// import androidx.compose.ui.text.font.FontWeight
// import androidx.compose.ui.text.style.TextDecoration
// import androidx.compose.ui.text.style.TextOverflow
// import androidx.compose.ui.unit.dp
// import androidx.compose.ui.unit.sp
// import cafe.adriel.voyager.navigator.LocalNavigator
// import coil3.compose.AsyncImage
// import com.developer.ev2z.ui.components.FloatingContactButton
// import com.developer.ev2z.util.Analytics
// import com.developer.ev2z.response.ProductDto
// import com.developer.ev2z.util.DeepLinkBus
// import eva2z_connect_multiplatform.composeapp.generated.resources.*
// import org.jetbrains.compose.resources.painterResource
// 
// // ── Premium Design Tokens ──────────────────────────────────────────
// internal val P_Green        = Color(0xFF00875A)
// internal val P_GreenLight   = Color(0xFF00A86B)
// internal val P_GreenSurface = Color(0xFFF0FAF5)
// internal val P_GreenBorder  = Color(0xFFB7E4CC)
// internal val P_Bg           = Color(0xFFF7F8FA)
// internal val P_Card         = Color(0xFFFFFFFF)
// internal val P_Dark         = Color(0xFF0A0F0D)
// internal val P_Mid          = Color(0xFF4A5568)
// internal val P_Light        = Color(0xFF9AA5B1)
// internal val P_Border       = Color(0xFFE8ECF0)
// internal val P_Red          = Color(0xFFD93025)
// internal val P_Gold         = Color(0xFFF59E0B)
// 
// private fun getRatingCount(productId: Int): Int = when (productId) {
//     1 -> 128; 2 -> 94; 3 -> 213; 4 -> 67; 5 -> 156
//     6 -> 89; 7 -> 174; 8 -> 102; 9 -> 231; 10 -> 78; 11 -> 143; else -> 50
// }
// private fun getRating(productId: Int): Double = when (productId) {
//     1 -> 4.7; 2 -> 4.5; 3 -> 4.9; 4 -> 4.3; 5 -> 4.8
//     6 -> 4.4; 7 -> 4.6; 8 -> 4.2; 9 -> 4.9; 10 -> 4.5; 11 -> 4.7; else -> 4.8
// }
// 
// // ── Shop Screen ───────────────────────────────────────────────────
// @Composable
// fun ShopScreen(
//     products: List<ShopProduct>,
//     cartCount: Int,
//     onBackClick: () -> Unit = {},
//     onToggleFavorite: (ShopProduct) -> Unit = {},
//     onPlaceOrder: (ShopProduct) -> Unit = {},
//     onGpsTrackerClick: () -> Unit = {},   // ✅ NEW — merged GPS card tap
//     onCartClick: () -> Unit = {}
// ) {
//     // Robust GPS detect — works regardless of exact API value
//     fun isGps(p: ShopProduct): Boolean {
//         val s = p.subtitle.lowercase().trim()
//         val d = p.deviceType.lowercase().trim()
//         return s.contains("wired") || s.contains("wireless") ||
//                 d.contains("wired") || d.contains("wireless")
//     }
//     fun isWired(p: ShopProduct): Boolean {
//         val s = p.subtitle.lowercase().trim()
//         val d = p.deviceType.lowercase().trim()
//         return s.contains("wired") || d.contains("wired")
//     }
//     fun isWireless(p: ShopProduct): Boolean {
//         val s = p.subtitle.lowercase().trim()
//         val d = p.deviceType.lowercase().trim()
//         return (s.contains("wireless") || d.contains("wireless")) && !isWired(p)
//     }
//     // Fallback: agar koi match nahi toh id 10,11 ko GPS consider karo
//     val gpsProducts = products.filter { isGps(it) }.let { list ->
//         if (list.isEmpty()) products.filter { it.id == 10 || it.id == 11 }
//         else list
//     }
//     val otherProducts = products.filter { p -> gpsProducts.none { it.id == p.id } }
// 
//     Box(modifier = Modifier.fillMaxSize().background(P_Bg)) {
//         LazyColumn(
//             modifier = Modifier.fillMaxSize(),
//             contentPadding = PaddingValues(bottom = 90.dp)
//         ) {
//             item { PremiumTopBar(cartCount, onCartClick) }
//             item { PremiumBanner() }
//             item { PremiumSectionHeader(otherProducts.size + if (gpsProducts.isNotEmpty()) 1 else 0) }
// 
//             // ✅ Merged GPS Tracker card (replaces wired + wireless separate cards)
//             if (gpsProducts.isNotEmpty()) {
//                 item {
//                     MergedGpsTrackerCard(
//                         wiredProduct = gpsProducts.firstOrNull { isWired(it) },
//                         wirelessProduct = gpsProducts.firstOrNull { isWireless(it) },
//                         onClick = onGpsTrackerClick
//                     )
//                     Spacer(Modifier.height(16.dp))
//                 }
//             }
// 
//             // Other products (non-GPS)
//             items(otherProducts) { product ->
//                 PremiumProductCard(
//                     product = product,
//                     onToggleFavorite = { onToggleFavorite(product) },
//                     onPlaceOrder = { onPlaceOrder(product) }
//                 )
//                 Spacer(Modifier.height(16.dp))
//             }
//         }
// 
//         FloatingContactButton(
//             phoneNumber = "919755273822",
//             modifier = Modifier
//                 .align(Alignment.BottomEnd)
//                 .padding(end = 16.dp, bottom = 80.dp)
//         )
// 
//         PremiumCartBar(
//             count = cartCount,
//             onClick = onCartClick,
//             modifier = Modifier.align(Alignment.BottomCenter)
//         )
//     }
// }
// 
// @Composable
// private fun MergedGpsTrackerCard(
//     wiredProduct: ShopProduct?,
//     wirelessProduct: ShopProduct?,
//     onClick: () -> Unit
// ) {
//     val wiredPrice    = wiredProduct?.price?.toInt() ?: 3200
//     val wirelessPrice = wirelessProduct?.price?.toInt() ?: 4200
//     val startingPrice = minOf(wiredPrice, wirelessPrice)
//     val fakeOld       = (startingPrice * 1.4).toInt()
//     val savings       = fakeOld - startingPrice
// 
//     Card(
//         modifier = Modifier
//             .fillMaxWidth()
//             .padding(horizontal = 20.dp)
//             .shadow(6.dp, RoundedCornerShape(20.dp),
//                 ambientColor = P_Green.copy(alpha = 0.12f))
//             .clickable { onClick() },
//         shape     = RoundedCornerShape(20.dp),
//         colors    = CardDefaults.cardColors(containerColor = Color.White),
//         elevation = CardDefaults.cardElevation(0.dp)
//     ) {
//         Column {
// 
//             // ── Image Area — wired + wireless side by side ────
//             Box(
//                 modifier = Modifier
//                     .fillMaxWidth()
//                     .height(200.dp)
//                     .background(
//                         Brush.verticalGradient(
//                             listOf(Color(0xFFF0FAF5), Color(0xFFE3F5EC))
//                         )
//                     )
//             ) {
//                 // Decorative bg circles
//                 Box(
//                     modifier = Modifier
//                         .size(140.dp).clip(CircleShape)
//                         .background(P_Green.copy(alpha = 0.04f))
//                         .align(Alignment.Center)
//                 )
//                 Box(
//                     modifier = Modifier
//                         .size(90.dp).clip(CircleShape)
//                         .background(P_Green.copy(alpha = 0.06f))
//                         .align(Alignment.Center)
//                 )
// 
//                 // ── Dono images side by side ──────────────────
//                 Row(
//                     modifier = Modifier
//                         .fillMaxSize()
//                         .padding(horizontal = 14.dp, vertical = 10.dp),
//                     horizontalArrangement = Arrangement.spacedBy(10.dp),
//                     verticalAlignment     = Alignment.CenterVertically
//                 ) {
//                     // Wired image card
//                     Box(
//                         modifier = Modifier
//                             .weight(1f)
//                             .fillMaxHeight()
//                             .clip(RoundedCornerShape(14.dp))
//                             .background(Color.White.copy(alpha = 0.7f))
//                             .border(1.dp, P_GreenBorder, RoundedCornerShape(14.dp)),
//                         contentAlignment = Alignment.Center
//                     ) {
//                         // ✅ wiredProduct ki imageUrl use kar raha hai
//                         if (!wiredProduct?.imageUrl.isNullOrBlank()) {
//                             AsyncImage(
//                                 model              = wiredProduct!!.imageUrl,
//                                 contentDescription = "Wired GPS Tracker",
//                                 modifier           = Modifier
//                                     .fillMaxSize()
//                                     .padding(10.dp),
//                                 contentScale       = ContentScale.Fit
//                             )
//                         } else {
//                             Icon(
//                                 Icons.Default.GpsFixed, null,
//                                 tint     = P_GreenBorder,
//                                 modifier = Modifier.size(44.dp)
//                             )
//                         }
//                         // Wired label
//                         Box(
//                             modifier = Modifier
//                                 .align(Alignment.BottomCenter)
//                                 .padding(bottom = 8.dp)
//                                 .clip(RoundedCornerShape(6.dp))
//                                 .background(P_Green)
//                                 .padding(horizontal = 8.dp, vertical = 3.dp)
//                         ) {
//                             Text("Wired · ₹$wiredPrice",
//                                 fontSize   = 9.sp,
//                                 fontWeight = FontWeight.Bold,
//                                 color      = Color.White)
//                         }
//                     }
// 
//                     // Wireless image card
//                     Box(
//                         modifier = Modifier
//                             .weight(1f)
//                             .fillMaxHeight()
//                             .clip(RoundedCornerShape(14.dp))
//                             .background(Color.White.copy(alpha = 0.7f))
//                             .border(1.dp, P_GreenBorder, RoundedCornerShape(14.dp)),
//                         contentAlignment = Alignment.Center
//                     ) {
//                         // ✅ wirelessProduct ki imageUrl use kar raha hai
//                         if (!wirelessProduct?.imageUrl.isNullOrBlank()) {
//                             AsyncImage(
//                                 model              = wirelessProduct!!.imageUrl,
//                                 contentDescription = "Wireless GPS Tracker",
//                                 modifier           = Modifier
//                                     .fillMaxSize()
//                                     .padding(10.dp),
//                                 contentScale       = ContentScale.Fit
//                             )
//                         } else {
//                             Icon(
//                                 Icons.Default.GpsFixed, null,
//                                 tint     = P_GreenBorder,
//                                 modifier = Modifier.size(44.dp)
//                             )
//                         }
//                         // Wireless label
//                         Box(
//                             modifier = Modifier
//                                 .align(Alignment.BottomCenter)
//                                 .padding(bottom = 8.dp)
//                                 .clip(RoundedCornerShape(6.dp))
//                                 .background(Color(0xFF7C3AED))
//                                 .padding(horizontal = 8.dp, vertical = 3.dp)
//                         ) {
//                             Text("Wireless · ₹$wirelessPrice",
//                                 fontSize   = 9.sp,
//                                 fontWeight = FontWeight.Bold,
//                                 color      = Color.White)
//                         }
//                     }
//                 }
// 
//                 // Top-left badge
//                 Box(
//                     modifier = Modifier
//                         .align(Alignment.TopStart)
//                         .padding(12.dp)
//                         .clip(RoundedCornerShape(8.dp))
//                         .background(P_GreenSurface)
//                         .border(1.dp, P_GreenBorder, RoundedCornerShape(8.dp))
//                         .padding(horizontal = 9.dp, vertical = 4.dp)
//                 ) {
//                     Row(
//                         verticalAlignment     = Alignment.CenterVertically,
//                         horizontalArrangement = Arrangement.spacedBy(4.dp)
//                     ) {
//                         Box(
//                             modifier = Modifier
//                                 .size(5.dp).clip(CircleShape)
//                                 .background(P_Green)
//                         )
//                         Text("INDIA'S #1 GPS",
//                             fontSize      = 9.sp,
//                             fontWeight    = FontWeight.Bold,
//                             color         = P_Green,
//                             letterSpacing = 0.5.sp)
//                     }
//                 }
// 
//                 // Top-right discount
//                 Box(
//                     modifier = Modifier
//                         .align(Alignment.TopEnd)
//                         .padding(12.dp)
//                         .clip(RoundedCornerShape(8.dp))
//                         .background(Color(0xFFFFF3EE))
//                         .border(1.dp, Color(0xFFFFCDB8), RoundedCornerShape(8.dp))
//                         .padding(horizontal = 8.dp, vertical = 4.dp)
//                 ) {
//                     Text("33% OFF",
//                         fontSize   = 10.sp,
//                         fontWeight = FontWeight.Bold,
//                         color      = P_Red)
//                 }
//             }
// 
//             // ── Info Section ──────────────────────────────────
//             Column(modifier = Modifier.padding(16.dp)) {
// 
//                 Text("GPS TRACKER",
//                     fontSize      = 9.sp,
//                     fontWeight    = FontWeight.Bold,
//                     color         = P_Green,
//                     letterSpacing = 1.2.sp)
// 
//                 Spacer(Modifier.height(5.dp))
// 
//                 Text("EVA2Z Smart GPS Tracker",
//                     fontSize   = 18.sp,
//                     fontWeight = FontWeight.Bold,
//                     color      = P_Dark,
//                     lineHeight = 24.sp)
// 
//                 Spacer(Modifier.height(5.dp))
// 
//                 Text("Real-time tracking · Anti-theft · Engine cut-off · Pan India",
//                     fontSize  = 12.sp,
//                     color     = P_Light,
//                     lineHeight = 17.sp)
// 
//                 Spacer(Modifier.height(10.dp))
// 
//                 Row(verticalAlignment = Alignment.CenterVertically) {
//                     repeat(5) {
//                         Icon(Icons.Default.Star, null,
//                             tint = P_Gold, modifier = Modifier.size(13.dp))
//                     }
//                     Spacer(Modifier.width(6.dp))
//                     Text("4.9",
//                         fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = P_Dark)
//                     Text("  ·  231 reviews", fontSize = 12.sp, color = P_Light)
//                 }
// 
//                 Spacer(Modifier.height(10.dp))
// 
//                 Box(
//                     modifier = Modifier
//                         .clip(RoundedCornerShape(6.dp))
//                         .background(P_GreenSurface)
//                         .border(1.dp, P_GreenBorder, RoundedCornerShape(6.dp))
//                         .padding(horizontal = 10.dp, vertical = 5.dp)
//                 ) {
//                     Row(
//                         verticalAlignment     = Alignment.CenterVertically,
//                         horizontalArrangement = Arrangement.spacedBy(5.dp)
//                     ) {
//                         Icon(Icons.Default.LocalOffer, null,
//                             tint = P_Green, modifier = Modifier.size(12.dp))
//                         Text("Save ₹$savings on this device",
//                             fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = P_Green)
//                     }
//                 }
// 
//                 Spacer(Modifier.height(14.dp))
//                 HorizontalDivider(color = P_Border, thickness = 0.5.dp)
//                 Spacer(Modifier.height(14.dp))
// 
//                 Row(
//                     modifier              = Modifier.fillMaxWidth(),
//                     horizontalArrangement = Arrangement.SpaceBetween,
//                     verticalAlignment     = Alignment.CenterVertically
//                 ) {
//                     Column {
//                         Row(verticalAlignment = Alignment.CenterVertically) {
//                             Text("₹$startingPrice",
//                                 fontSize   = 22.sp,
//                                 fontWeight = FontWeight.Black,
//                                 color      = P_Dark)
//                             Spacer(Modifier.width(6.dp))
//                             Text("₹$fakeOld",
//                                 fontSize       = 13.sp,
//                                 color          = P_Light,
//                                 textDecoration = TextDecoration.LineThrough)
//                         }
//                         Text("Starting price · 2 variants",
//                             fontSize = 11.sp, color = P_Light)
//                     }
// 
//                     Button(
//                         onClick        = onClick,
//                         shape          = RoundedCornerShape(12.dp),
//                         colors         = ButtonDefaults.buttonColors(containerColor = P_Green),
//                         contentPadding = PaddingValues(horizontal = 20.dp, vertical = 13.dp),
//                         elevation      = ButtonDefaults.buttonElevation(0.dp)
//                     ) {
//                         Text("Explore",
//                             fontSize = 13.sp, fontWeight = FontWeight.Bold)
//                         Spacer(Modifier.width(4.dp))
//                         Icon(Icons.Default.ArrowForward, null,
//                             modifier = Modifier.size(14.dp))
//                     }
//                 }
//             }
//         }
//     }
// }
// 
// // ── Light Variant Chip — replaces dark one ─────────────────────
// @Composable
// private fun LightVariantChip(label: String, price: String) {
//     Box(
//         modifier = Modifier
//             .clip(RoundedCornerShape(8.dp))
//             .background(Color.White.copy(alpha = 0.85f))
//             .border(1.dp, P_GreenBorder, RoundedCornerShape(8.dp))
//             .padding(horizontal = 10.dp, vertical = 6.dp)
//     ) {
//         Column(horizontalAlignment = Alignment.CenterHorizontally) {
//             Text(label,
//                 fontSize   = 10.sp,
//                 fontWeight = FontWeight.Bold,
//                 color      = P_Dark)
//             Text(price,
//                 fontSize   = 11.sp,
//                 fontWeight = FontWeight.SemiBold,
//                 color      = P_Green)
//         }
//     }
// }
// 
// @Composable
// private fun VariantChip(label: String, price: String) {
//     Box(
//         modifier = Modifier
//             .clip(RoundedCornerShape(8.dp))
//             .background(Color.White.copy(alpha = 0.15f))
//             .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
//             .padding(horizontal = 10.dp, vertical = 6.dp)
//     ) {
//         Column(horizontalAlignment = Alignment.CenterHorizontally) {
//             Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
//             Text(price, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = P_GreenLight)
//         }
//     }
// }
// 
// // ── Top Bar ────────────────────────────────────────────────────────
// @Composable
// private fun PremiumTopBar(cartCount: Int, onCartClick: () -> Unit) {
//     Surface(color = P_Card, shadowElevation = 0.dp) {
//         Column {
//             Row(
//                 modifier = Modifier
//                     .fillMaxWidth()
//                     .statusBarsPadding()
//                     .padding(horizontal = 20.dp, vertical = 16.dp),
//                 verticalAlignment = Alignment.CenterVertically
//             ) {
//                 Box(
//                     modifier = Modifier
//                         .size(42.dp)
//                         .clip(RoundedCornerShape(13.dp))
//                         .background(Brush.linearGradient(listOf(P_GreenLight, P_Green))),
//                     contentAlignment = Alignment.Center
//                 ) {
//                     Text("E", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
//                 }
//                 Spacer(Modifier.width(12.dp))
//                 Column(modifier = Modifier.weight(1f)) {
//                     Text("EVA2Z Store", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = P_Dark)
//                     Row(verticalAlignment = Alignment.CenterVertically) {
//                         repeat(5) { Text("★", fontSize = 10.sp, color = P_Gold) }
//                         Spacer(Modifier.width(4.dp))
//                         Text("4.8  ·  Trusted by 10K+", fontSize = 11.sp, color = P_Light)
//                     }
//                 }
//                 Box {
//                     IconButton(
//                         onClick = onCartClick,
//                         modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(P_Bg)
//                     ) {
//                         Icon(Icons.Outlined.ShoppingBag, null, tint = P_Dark, modifier = Modifier.size(22.dp))
//                     }
//                     if (cartCount > 0) {
//                         Box(
//                             modifier = Modifier
//                                 .size(17.dp).clip(CircleShape).background(P_Red)
//                                 .align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp),
//                             contentAlignment = Alignment.Center
//                         ) {
//                             Text(cartCount.toString(), fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
//                         }
//                     }
//                 }
//             }
//             HorizontalDivider(color = P_Border, thickness = 0.5.dp)
//         }
//     }
// }
// 
// // ── Banner ─────────────────────────────────────────────────────────
// @Composable
// private fun PremiumBanner() {
//     Box(
//         modifier = Modifier
//             .fillMaxWidth()
//             .padding(horizontal = 20.dp, vertical = 20.dp)
//             .height(180.dp)
//             .clip(RoundedCornerShape(20.dp))
//     ) {
//         Image(
//             painter = painterResource(Res.drawable.banner_img_2),
//             contentDescription = null,
//             modifier = Modifier.fillMaxSize(),
//             contentScale = ContentScale.Crop
//         )
//         Box(
//             modifier = Modifier.fillMaxSize()
//                 .background(Brush.horizontalGradient(
//                     listOf(Color(0xF0061C0E), Color(0x3000875A), Color.Transparent)
//                 ))
//         )
//         Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
//             Text("India's #1 GPS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
//                 color = P_GreenLight, letterSpacing = 1.5.sp)
//             Spacer(Modifier.height(4.dp))
//             Text("Track Smarter.\nDrive Safer.", fontSize = 22.sp,
//                 fontWeight = FontWeight.Black, color = Color.White, lineHeight = 28.sp)
//             Spacer(Modifier.height(14.dp))
//             Box(
//                 modifier = Modifier.clip(RoundedCornerShape(8.dp))
//                     .background(P_GreenLight).padding(horizontal = 16.dp, vertical = 9.dp)
//             ) {
//                 Text("Explore Now", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
//             }
//         }
//     }
// }
// 
// // ── Section Header ─────────────────────────────────────────────────
// @Composable
// private fun PremiumSectionHeader(count: Int) {
//     Row(
//         modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 12.dp),
//         horizontalArrangement = Arrangement.SpaceBetween,
//         verticalAlignment = Alignment.CenterVertically
//     ) {
//         Column {
//             Text("All Products", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = P_Dark)
//             Text("$count items available", fontSize = 13.sp, color = P_Light)
//         }
//         Box(
//             modifier = Modifier.clip(RoundedCornerShape(8.dp))
//                 .border(1.dp, P_Border, RoundedCornerShape(8.dp))
//                 .padding(horizontal = 12.dp, vertical = 7.dp)
//         ) {
//             Row(verticalAlignment = Alignment.CenterVertically,
//                 horizontalArrangement = Arrangement.spacedBy(4.dp)) {
//                 Icon(Icons.Default.FilterList, null, tint = P_Mid, modifier = Modifier.size(14.dp))
//                 Text("Filter", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = P_Mid)
//             }
//         }
//     }
// }
// 
// // ── Premium Product Card (non-GPS) ─────────────────────────────────
// @Composable
// private fun PremiumProductCard(
//     product: ShopProduct,
//     onToggleFavorite: () -> Unit,
//     onPlaceOrder: () -> Unit
// ) {
//     val fakeOld = (product.price * 1.5).toInt()
//     val disc = (((fakeOld - product.price) / fakeOld) * 100).toInt()
// 
//     Card(
//         modifier = Modifier
//             .fillMaxWidth()
//             .padding(horizontal = 20.dp)
//             .shadow(6.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x14000000))
//             .clickable { onPlaceOrder() },
//         shape = RoundedCornerShape(16.dp),
//         colors = CardDefaults.cardColors(containerColor = P_Card),
//         elevation = CardDefaults.cardElevation(0.dp)
//     ) {
//         Column {
//             Box(
//                 modifier = Modifier.fillMaxWidth().height(180.dp).background(Color(0xFFF9FAFB))
//             ) {
//                 AsyncImage(
//                     model = product.imageUrl, contentDescription = null,
//                     modifier = Modifier.fillMaxSize().padding(20.dp),
//                     contentScale = ContentScale.Fit,
//                     placeholder = painterResource(Res.drawable.app_logo),
//                     error = painterResource(Res.drawable.app_logo)
//                 )
//                 Box(
//                     modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
//                         .clip(RoundedCornerShape(6.dp)).background(P_Red)
//                         .padding(horizontal = 8.dp, vertical = 4.dp)
//                 ) {
//                     Text("$disc% OFF", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
//                 }
//                 Box(
//                     modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
//                         .size(34.dp).clip(CircleShape).background(Color.White)
//                         .shadow(2.dp, CircleShape).clickable { onToggleFavorite() },
//                     contentAlignment = Alignment.Center
//                 ) {
//                     Icon(
//                         if (product.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
//                         null,
//                         tint = if (product.isFavorite) P_Red else P_Light,
//                         modifier = Modifier.size(16.dp)
//                     )
//                 }
//             }
//             Column(modifier = Modifier.padding(16.dp)) {
//                 if (product.subtitle.isNotBlank()) {
//                     Text(product.subtitle.uppercase(), fontSize = 10.sp,
//                         fontWeight = FontWeight.SemiBold, color = P_Green, letterSpacing = 0.8.sp)
//                     Spacer(Modifier.height(4.dp))
//                 }
//                 Text(product.title, fontSize = 16.sp, fontWeight = FontWeight.Bold,
//                     color = P_Dark, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 22.sp)
//                 Spacer(Modifier.height(10.dp))
//                 Row(verticalAlignment = Alignment.CenterVertically) {
//                     repeat(5) { Icon(Icons.Default.Star, null, tint = P_Gold, modifier = Modifier.size(13.dp)) }
//                     Spacer(Modifier.width(6.dp))
//                     Text("${product.rating}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = P_Mid)
//                     Text("  (${product.ratingCount})", fontSize = 12.sp, color = P_Light)
//                 }
//                 Spacer(Modifier.height(12.dp))
//                 HorizontalDivider(color = P_Border, thickness = 0.5.dp)
//                 Spacer(Modifier.height(12.dp))
//                 Row(
//                     modifier = Modifier.fillMaxWidth(),
//                     horizontalArrangement = Arrangement.SpaceBetween,
//                     verticalAlignment = Alignment.CenterVertically
//                 ) {
//                     Column {
//                         Text("₹${product.price.toInt()}", fontSize = 22.sp,
//                             fontWeight = FontWeight.Black, color = P_Dark)
//                         Text("₹$fakeOld", fontSize = 13.sp, color = P_Light,
//                             textDecoration = TextDecoration.LineThrough)
//                     }
//                     Button(
//                         onClick = onPlaceOrder,
//                         shape = RoundedCornerShape(10.dp),
//                         colors = ButtonDefaults.buttonColors(containerColor = P_Green),
//                         contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
//                         elevation = ButtonDefaults.buttonElevation(0.dp)
//                     ) {
//                         Text("View Details", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
//                     }
//                 }
//             }
//         }
//     }
// }
// 
// // ── Cart Bar ───────────────────────────────────────────────────────
// @Composable
// private fun PremiumCartBar(count: Int, onClick: () -> Unit, modifier: Modifier) {
//     Surface(modifier = modifier.fillMaxWidth(), color = P_Card,
//         shadowElevation = 8.dp, tonalElevation = 0.dp) {
//         Button(
//             onClick = onClick,
//             modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp).height(46.dp),
//             shape = RoundedCornerShape(14.dp),
//             colors = ButtonDefaults.buttonColors(containerColor = P_Green),
//             elevation = ButtonDefaults.buttonElevation(0.dp)
//         ) {
//             Row(
//                 modifier = Modifier.fillMaxWidth(),
//                 verticalAlignment = Alignment.CenterVertically,
//                 horizontalArrangement = Arrangement.SpaceBetween
//             ) {
//                 Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
//                     verticalAlignment = Alignment.CenterVertically) {
//                     Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(17.dp))
//                     Text("View Cart", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
//                 }
//                 Box(
//                     modifier = Modifier.clip(RoundedCornerShape(6.dp))
//                         .background(Color.White.copy(alpha = 0.2f))
//                         .padding(horizontal = 8.dp, vertical = 3.dp)
//                 ) {
//                     Text("$count items", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.White)
//                 }
//             }
//         }
//     }
// }
// 
// // ── ShopScreenHost ────────────────────────────────────────────────
// @Composable
// fun ShopScreenHost(category: String? = null) {
//     val viewModel = ShopViewModelProvider.viewModel
//     val state by viewModel.state.collectAsState()
//     val navigator = LocalNavigator.current
//     val filteredProducts = state.products.filter { dto ->
//         when (category) {
//             "wired"    -> dto.device_type?.lowercase() == "wired"
//             "wireless" -> dto.device_type?.lowercase() == "wireless"
//             else       -> true
//         }
//     }
// 
//     LaunchedEffect(Unit) {
//         Analytics.logEvent("screen_view", mapOf(
//             "screen_name" to "shop_screen", "screen_class" to "ShopScreen",
//             "item_category" to "iot_devices", "source" to "home_or_menu"))
//         viewModel.loadData()
//     }
// 
//     // Deep link handling via SharedFlow collection
//     LaunchedEffect(Unit) {
//         DeepLinkBus.navigateTo.collect { dest ->
//             if (!dest.startsWith("open_product_") && !dest.startsWith("product_detail_")) return@collect
//             val pid = when {
//                 dest.startsWith("open_product_")   -> dest.removePrefix("open_product_").substringBefore("_").toIntOrNull()
//                 dest.startsWith("product_detail_") -> dest.removePrefix("product_detail_").substringBefore("_").toIntOrNull()
//                 else -> null
//             } ?: return@collect
//             val currentProducts = viewModel.state.value.products
//             if (currentProducts.isEmpty()) { viewModel.loadData(); return@collect }
//             val dto = currentProducts.firstOrNull { it.id == pid } ?: return@collect
//             val product = ShopProduct(
//                 id = dto.id, title = dto.name ?: "", subtitle = dto.device_type ?: "",
//                 price = dto.sale_price?.toDoubleOrNull() ?: 0.0,
//                 rechargePrice = dto.recharge_price?.toDoubleOrNull() ?: 0.0,
//                 promoAmount = dto.promo_amt?.toDoubleOrNull() ?: 0.0,
//                 promoCode = dto.promo_code ?: "", imageUrl = dto.image ?: "",
//                 description = dto.description ?: "",
//                 features = listOfNotNull(dto.features1, dto.features2, dto.features3,
//                     dto.features4, dto.features5, dto.features6, dto.features7),
//                 deviceType = dto.device_type ?: "", rating = getRating(dto.id),
//                 ratingCount = getRatingCount(dto.id), isFavorite = viewModel.isFavorite(dto.id)
//             )
//             DeepLinkBus.clear()
//             navigator?.push(ProductDetailsScreen(product = product,
//                 onToggleFavorite = { p -> viewModel.toggleWishlist(p.id) }))
//         }
//     }
// 
//     if (state.loading) {
//         Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//             CircularProgressIndicator(color = P_Green, strokeWidth = 2.dp)
//         }
//         return
//     }
// 
//     LaunchedEffect(state.products) {
//         if (state.products.isNotEmpty()) {
//             Analytics.logEvent("view_item_list", mapOf(
//                 "item_list_id" to "shop_device_list",
//                 "item_list_name" to "Location Tracker Device",
//                 "currency" to "INR",
//                 "items" to state.products.mapIndexed { index, p ->
//                     mapOf("item_id" to p.id.toString(), "item_name" to (p.name ?: ""),
//                         "item_brand" to "EVA2Z", "item_category" to "iot_devices",
//                         "item_variant" to (p.device_type ?: ""),
//                         "price" to (p.sale_price?.toDoubleOrNull() ?: 0.0),
//                         "index" to index).toString()
//                 }))
//         }
//     }
// 
//     val mappedProducts = filteredProducts.map { dto ->
//         ShopProduct(
//             id = dto.id, title = dto.name ?: "", subtitle = dto.device_type ?: "",
//             price = dto.sale_price?.toDoubleOrNull() ?: 0.0,
//             rechargePrice = dto.recharge_price?.toDoubleOrNull() ?: 0.0,
//             promoAmount = dto.promo_amt?.toDoubleOrNull() ?: 0.0,
//             promoCode = dto.promo_code ?: "", imageUrl = dto.image ?: "",
//             description = dto.description ?: "",
//             features = listOfNotNull(dto.features1, dto.features2, dto.features3,
//                 dto.features4, dto.features5, dto.features6, dto.features7),
//             deviceType = dto.device_type ?: "", rating = getRating(dto.id),
//             ratingCount = getRatingCount(dto.id), isFavorite = viewModel.isFavorite(dto.id)
//         )
//     }
// 
//     ShopScreen(
//         products = mappedProducts,
//         cartCount = state.cartCount,
//         // ✅ Merged GPS card tap → GpsTrackerFlowScreen
//         onGpsTrackerClick = {
//             Analytics.logEvent("select_item", mapOf(
//                 "item_list_id" to "shop_device_list",
//                 "item_name" to "GPS Tracker", "item_brand" to "EVA2Z"))
// 
//             // Debug: print all product subtitles/deviceTypes to find exact API values
//             mappedProducts.forEach { p ->
//                 println("🔍 Product id=${p.id} title=${p.title} subtitle='${p.subtitle}' deviceType='${p.deviceType}'")
//             }
// 
//             // Smart lookup: contains check + ID fallback
//             val allDtos = state.products
//             val wiredDto = allDtos.firstOrNull {
//                 (it.device_type?.lowercase()?.contains("wired") == true) ||
//                         it.id == 10
//             }
//             val wirelessDto = allDtos.firstOrNull {
//                 val dt = it.device_type?.lowercase() ?: ""
//                 (dt.contains("wireless")) ||
//                         it.id == 11
//             }
// 
//             fun dtoToProduct(dto: com.developer.ev2z.response.ProductDto) = ShopProduct(
//                 id = dto.id, title = dto.name ?: "",
//                 subtitle = dto.device_type ?: "",
//                 price = dto.sale_price?.toDoubleOrNull() ?: 0.0,
//                 rechargePrice = dto.recharge_price?.toDoubleOrNull() ?: 0.0,
//                 promoAmount = dto.promo_amt?.toDoubleOrNull() ?: 0.0,
//                 promoCode = dto.promo_code ?: "",
//                 imageUrl = dto.image ?: "",
//                 description = dto.description ?: "",
//                 features = listOfNotNull(dto.features1, dto.features2, dto.features3,
//                     dto.features4, dto.features5, dto.features6, dto.features7),
//                 deviceType = dto.device_type ?: "",
//                 rating = getRating(dto.id),
//                 ratingCount = getRatingCount(dto.id),
//                 isFavorite = viewModel.isFavorite(dto.id)
//             )
// 
//             println("✅ Wired: ${wiredDto?.device_type} id=${wiredDto?.id}")
//             println("✅ Wireless: ${wirelessDto?.device_type} id=${wirelessDto?.id}")
// 
//             navigator?.push(GpsHeroScreen(
//                 wiredProduct = wiredDto?.let { dtoToProduct(it) },
//                 wirelessProduct = wirelessDto?.let { dtoToProduct(it) },
//                 // AFTER — GPS products (id 10, 11) ke liye sahi plan_type
//                 onAddToCart = { product, planType, deviceType ->
//                     val finalPlan = if (product.id == 10 || product.id == 11) {
//                         "without_recharge"  // default — recharge sheet nahi dikhaya
//                     } else planType
// 
//                     val finalDevice = deviceType.ifBlank { "" }
// 
//                     println("🛒 addToCart → id=${product.id} plan=$finalPlan device=$finalDevice")
//                     viewModel.addToCart("add", product.id, 1, finalPlan, finalDevice)
//                 }
//             ))
//         },
//         onToggleFavorite = { p ->
//             viewModel.toggleWishlist(p.id.toInt())
//             Analytics.logEvent("add_to_wishlist", mapOf("currency" to "INR",
//                 "items" to listOf(mapOf("item_id" to p.id.toString(),
//                     "item_name" to p.title, "item_brand" to "EVA2Z",
//                     "item_category" to "iot_devices", "item_variant" to p.subtitle,
//                     "price" to p.price))))
//         },
//         onCartClick = { navigator?.push(CartScreen()) },
//         onPlaceOrder = { product ->
//             navigator?.push(ProductDetailsScreen(product = product,
//                 onToggleFavorite = { p ->
//                     viewModel.toggleWishlist(p.id)
//                     Analytics.logEvent("add_to_wishlist", mapOf("currency" to "INR",
//                         "items" to listOf(mapOf("item_id" to p.id.toString(),
//                             "item_name" to p.title, "item_brand" to "EVA2Z",
//                             "item_category" to "iot_devices",
//                             "item_variant" to p.subtitle, "price" to p.price))))
//                 }))
//         }
//     )
// }
// 
// // Compatibility tokens
// internal val ShopGreen = P_Green
// internal val ShopGreenDark = P_Green
// internal val ShopGreenSoft = P_GreenSurface
// internal val ShopGreenMid = P_GreenBorder
// internal val ShopBg = P_Bg
// internal val ShopCard = P_Card
// internal val ShopTextPrimary = P_Dark
// internal val ShopTextSec = P_Mid
// internal val ShopTextTert = P_Light
// internal val ShopError = P_Red