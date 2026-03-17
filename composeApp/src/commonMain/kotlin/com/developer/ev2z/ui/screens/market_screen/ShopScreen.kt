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

// ── Premium Design Tokens ──────────────────────────────────────────
internal val P_Green        = Color(0xFF00875A)   // deep emerald
internal val P_GreenLight   = Color(0xFF00A86B)
internal val P_GreenSurface = Color(0xFFF0FAF5)
internal val P_GreenBorder  = Color(0xFFB7E4CC)
internal val P_Bg           = Color(0xFFF7F8FA)
internal val P_Card         = Color(0xFFFFFFFF)
internal val P_Dark         = Color(0xFF0A0F0D)
internal val P_Mid          = Color(0xFF4A5568)
internal val P_Light        = Color(0xFF9AA5B1)
internal val P_Border       = Color(0xFFE8ECF0)
internal val P_Red          = Color(0xFFD93025)
internal val P_Gold         = Color(0xFFF59E0B)


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
                        .background(Brush.linearGradient(
                            listOf(P_GreenLight, P_Green)
                        )),
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(20.dp))
    ) {
        Image(
            painter = painterResource(Res.drawable.banner_img_2),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Dark overlay
        Box(
            modifier = Modifier.fillMaxSize()
                .background(Brush.horizontalGradient(
                    listOf(Color(0xF0061C0E), Color(0x3000875A), Color.Transparent)
                ))
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            Text("India's #1 GPS", fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = P_GreenLight, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(4.dp))
            Text("Track Smarter.\nDrive Safer.",
                fontSize = 22.sp, fontWeight = FontWeight.Black,
                color = Color.White, lineHeight = 28.sp)
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(P_GreenLight)
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text("Explore Now", fontSize = 13.sp,
                    fontWeight = FontWeight.Bold, color = Color.White)
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
    val deepLinkDestination by DeepLinkBus.navigateTo.collectAsState() // ✅ ADD

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

// Keep old tokens for compatibility
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