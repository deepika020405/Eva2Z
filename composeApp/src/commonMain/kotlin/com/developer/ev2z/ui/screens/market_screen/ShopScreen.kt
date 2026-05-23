package com.developer.ev2z.ui.screens.market_screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import coil3.compose.AsyncImage
import com.developer.ev2z.ui.components.FloatingContactButton
import com.developer.ev2z.util.Analytics
import com.developer.ev2z.util.DeepLinkBus
import eva2z_connect_multiplatform.composeapp.generated.resources.*
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

// ── Design Tokens ────────────────────────────────────────────────
internal val P_Green        = Color(0xFF5BA03B)   // lighter vibrant green — was #4A7D39
internal val P_GreenDark    = Color(0xFF467D28)   // pressed / active states
internal val P_GreenLight   = Color(0xFF7FC25A)   // light accent
internal val P_GreenSurface = Color(0xFFF0F7EC)   // crisp light-green surface
internal val P_GreenBorder  = Color(0xFFC0D9B0)   // soft border
internal val P_Bg           = Color(0xFFFAFBF8)   // near-white, no yellow tint
internal val P_Card         = Color(0xFFFFFFFF)
internal val P_Dark         = Color(0xFF1C2D1A)   // text primary
internal val P_Mid          = Color(0xFF637263)   // text secondary
internal val P_Light        = Color(0xFFA5AFAA)   // text tertiary
internal val P_Border       = Color(0xFFDDE8D8)   // card borders
internal val P_Red          = Color(0xFFE5565B)   // brand Red
internal val P_Gold         = Color(0xFFF4C44E)   // brand Yellow
internal val P_BannerBg     = Color(0xFF1E2C1C)   // dark promo banner
internal val P_ChipBg       = Color(0xFFF0F7EC)
internal val P_ImageBg      = Color(0xFFF8FAF7)
internal val P_InfoBoxBg    = Color(0xFFEBF2E6)

private fun getRatingCount(productId: Int): Int = when (productId) {
    1 -> 128; 2 -> 94; 3 -> 213; 4 -> 67; 5 -> 156
    6 -> 89;  7 -> 174; 8 -> 102; 9 -> 231; 10 -> 78
    11 -> 143; else -> 50
}

private fun getRating(productId: Int): Double = when (productId) {
    1 -> 4.7; 2 -> 4.5; 3 -> 4.9; 4 -> 4.3; 5 -> 4.8
    6 -> 4.4; 7 -> 4.6; 8 -> 4.2; 9 -> 4.9; 10 -> 4.5
    11 -> 4.7; else -> 4.8
}

private fun getProductTagline(product: ShopProduct): String {
    if (product.description.isNotBlank()) return product.description
    val t = product.title.lowercase()
    return when {
        "wireless" in t || "wireless" in product.deviceType.lowercase() ->
            "Battery-powered GPS with live tracking"
        "wired" in t || "wired" in product.deviceType.lowercase() ->
            "Hardwired GPS for permanent installation"
        "roadside" in t -> "24x7 roadside assistance, Pan India"
        "qr" in t       -> "Smart QR tag for recovery & alerts"
        else            -> "Protect your vehicle, 24x7"
    }
}

private val FEATURED_BADGES = listOf("BESTSELLER", "TOP RATED", "POPULAR", "NEW", "HOT")

private enum class PromoProfile { NEW_USER, HAS_PARKING_TAG, HAS_GPS, HAS_BOTH }

// ── Shop Screen ──────────────────────────────────────────────────
@Composable
fun ShopScreen(
    products: List<ShopProduct>,
    cartCount: Int,
    totalPrice: String? = null,
    hasGpsDevice: Boolean = false,
    hasParkingTag: Boolean = false,
    onBackClick: () -> Unit = {},
    onPlaceOrder: (ShopProduct) -> Unit = {},
    onAddToCart: (ShopProduct, String) -> Unit = { _, _ -> },
    onCartClick: () -> Unit = {}
) {
    var selectedCategory  by remember { mutableStateOf("all") }
    var showPlanDetails   by remember { mutableStateOf(false) }
    var addedProduct      by remember { mutableStateOf<ShopProduct?>(null) }
    var addToCartKey      by remember { mutableStateOf(0) }
    var deviceTypeProduct by remember { mutableStateOf<ShopProduct?>(null) }

    LaunchedEffect(addToCartKey) {
        if (addedProduct != null) {
            delay(3000)
            addedProduct = null
        }
    }

    val filtered = remember(products, selectedCategory) {
        when (selectedCategory) {
            "gps" -> products.filter {
                val dt = it.deviceType.lowercase()
                dt == "wired" || dt == "wireless"
                    || dt.contains("gps")
                    || (it.title + " " + it.subtitle).lowercase().let { s ->
                        ("wired" in s || "wireless" in s) && "qr" !in s && "tag" !in s && "tracker" !in s
                    }
            }
            "trackers" -> products.filter {
                val dt = it.deviceType.lowercase()
                val s  = (it.title + " " + it.subtitle).lowercase()
                dt.contains("tracker") || (dt != "wired" && dt != "wireless" && "tracker" in s)
            }
            "qr" -> products.filter {
                val dt = it.deviceType.lowercase()
                // Strictly exclude GPS (wired/wireless) — only keep products whose
                // deviceType or title explicitly contains "qr" / "parking" / "tag"
                dt != "wired" && dt != "wireless"
                    && (dt.contains("qr") || dt.contains("parking") || dt.contains("tag")
                    || (it.title + " " + it.subtitle).lowercase().let { s -> "qr" in s || "parking tag" in s })
            }
            else -> products
        }
    }

    val featured = filtered.take(5)

    // Best GPS product (wired → wireless → "pro" → any)
    val proProduct = remember(products) {
        products.firstOrNull { it.deviceType.lowercase().contains("wireless") }
            ?: products.firstOrNull { it.title.lowercase().contains("pro") }
            ?: products.firstOrNull()
    }
    val proPrice = proProduct?.price?.toInt() ?: 4200

    // Explicit GPS product for HAS_PARKING_TAG upsell (wired or wireless GPS)
    val gpsProduct = remember(products) {
        products.firstOrNull { p ->
            val s = (p.title + " " + p.subtitle + " " + p.deviceType).lowercase()
            "wired" in s || "wireless" in s || "gps" in s
        } ?: proProduct
    }

    // QR / parking tag product for HAS_GPS cross-sell
    val qrProduct = remember(products) {
        products.firstOrNull { p ->
            val s = (p.title + " " + p.subtitle + " " + p.deviceType).lowercase()
            "qr" in s || "parking tag" in s || "tag" in s
        }
    }

    val promoProfile = remember(hasGpsDevice, hasParkingTag) {
        when {
            hasGpsDevice && hasParkingTag  -> PromoProfile.HAS_BOTH
            hasGpsDevice && !hasParkingTag -> PromoProfile.HAS_GPS
            !hasGpsDevice && hasParkingTag -> PromoProfile.HAS_PARKING_TAG
            else                           -> PromoProfile.NEW_USER
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(P_Bg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (cartCount > 0) 168.dp else 100.dp)
        ) {
            item { ShopTopBar(cartCount = cartCount, onCartClick = onCartClick) }

            item {
                Spacer(Modifier.height(4.dp))
                CategoryChips(selected = selectedCategory, onSelect = { selectedCategory = it })
                Spacer(Modifier.height(14.dp))
            }

//            item {
//                DarkPromoBanner(
//                    profile       = promoProfile,
//                    onPlanDetails = { showPlanDetails = true },
//                    onGetRecommended = {
//                        when (promoProfile) {
//                            PromoProfile.NEW_USER        -> proProduct?.let(onPlaceOrder)
//                            PromoProfile.HAS_PARKING_TAG -> gpsProduct?.let(onPlaceOrder)
//                            PromoProfile.HAS_GPS         -> (qrProduct ?: proProduct)?.let(onPlaceOrder)
//                            PromoProfile.HAS_BOTH        -> { /* already on shop — user browses naturally */ }
//                        }
//                    }
//                )
//                Spacer(Modifier.height(18.dp))
//            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("⭐", fontSize = 16.sp)
                    Text(
                        "Featured", fontSize = 17.sp,
                        fontWeight = FontWeight.Bold, color = P_Dark
                    )
                }
            }

            if (featured.isNotEmpty()) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(featured) { i, product ->
                            FeaturedCard(
                                product = product,
                                badge   = FEATURED_BADGES.getOrElse(i) { "FEATURED" },
                                onClick = { onPlaceOrder(product) },
                                onAdd   = {
                                    if (product.deviceType != "none") {
                                        deviceTypeProduct = product
                                    } else {
                                        onAddToCart(product, "")
                                        addedProduct = product
                                        addToCartKey++
                                    }
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                }
            }

            item {
                val sectionTitle = when (selectedCategory) {
                    "gps"      -> "GPS Devices"
                    "trackers" -> "Trackers"
                    "qr"       -> "QR Tags"
                    else       -> "All Products"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.Bottom
                ) {
                    Text(
                        sectionTitle, fontSize = 17.sp,
                        fontWeight = FontWeight.Bold, color = P_Dark
                    )
                    Text("${filtered.size} items", fontSize = 12.sp, color = P_Mid)
                }
            }

            items(filtered) { product ->
                ListProductCard(
                    product = product,
                    onClick = { onPlaceOrder(product) },
                    onAdd   = {
                        if (product.deviceType != "none") {
                            deviceTypeProduct = product
                        } else {
                            onAddToCart(product, "")
                            addedProduct = product
                            addToCartKey++
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))
            }
        }

        // ── Persistent cart bar ──────────────────────────────────
        AnimatedVisibility(
            visible  = cartCount > 0,
            enter    = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit     = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            PersistentCartBar(
                cartCount  = cartCount,
                totalPrice = totalPrice,
                onViewCart = onCartClick
            )
        }

        // ── Added-to-cart flash confirmation (floats above persistent bar) ─
        AnimatedVisibility(
            visible  = addedProduct != null,
            enter    = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit     = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp)
                .padding(bottom = if (cartCount > 0) 78.dp else 16.dp)
        ) {
            addedProduct?.let { p ->
                AddedToCartBar(
                    product    = p,
                    onViewCart = { addedProduct = null; onCartClick() },
                    onDismiss  = { addedProduct = null }
                )
            }
        }

        FloatingContactButton(
            phoneNumber = "919755273822",
            modifier    = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = if (cartCount > 0) 82.dp else 16.dp)
        )

        if (showPlanDetails) {
            PlanDetailsBottomSheet(
                price      = proPrice,
                onDismiss  = { showPlanDetails = false },
                onGetGpsPro = {
                    showPlanDetails = false
                    Analytics.logEvent("select_promotion", mapOf(
                        "promotion_id"   to "gps_pro_1yr_plan",
                        "promotion_name" to "GPS + 1 Year Plan Free"
                    ))
                    proProduct?.let(onPlaceOrder)
                }
            )
        }

        deviceTypeProduct?.let { product ->
            DeviceTypeSheet(
                product   = product,
                onSelect  = { selectedType ->
                    onAddToCart(product, selectedType)
                    addedProduct = product
                    addToCartKey++
                    deviceTypeProduct = null
                },
                onDismiss = { deviceTypeProduct = null }
            )
        }
    }
}

// ── Added to Cart Bar ────────────────────────────────────────────
@Composable
private fun AddedToCartBar(
    product: ShopProduct,
    onViewCart: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = P_GreenSurface),
        elevation = CardDefaults.cardElevation(6.dp),
        modifier  = Modifier
            .fillMaxWidth()
            .border(1.dp, P_GreenBorder, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(P_Green),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check, null,
                    tint     = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Added to cart!",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color      = P_GreenDark
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    product.title,
                    fontSize  = 11.sp,
                    color     = P_Mid,
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(P_Green)
                    .clickable { onViewCart() }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "View Cart",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                    Icon(
                        Icons.Default.ArrowForward, null,
                        tint     = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

// ── Persistent Cart Bar ──────────────────────────────────────────
@Composable
private fun PersistentCartBar(
    cartCount: Int,
    totalPrice: String?,
    onViewCart: () -> Unit
) {
    val formattedTotal = totalPrice
        ?.toDoubleOrNull()
        ?.let { "₹${it.toInt()}" }
        ?: totalPrice?.takeIf { it.isNotBlank() && it != "0" }?.let { "₹$it" }

    Surface(
        color           = P_Green,
        shadowElevation = 16.dp,
        modifier        = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.ShoppingCart, null,
                    tint     = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "$cartCount ${if (cartCount == 1) "item" else "items"} in cart",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
                if (formattedTotal != null) {
                    Text(
                        "Total: $formattedTotal",
                        fontSize = 11.sp,
                        color    = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .clickable { onViewCart() }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    "View Cart",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color      = P_Green
                )
                Icon(
                    Icons.Default.ArrowForward, null,
                    tint     = P_Green,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// ── Device Type Sheet ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceTypeSheet(
    product: ShopProduct,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf<String?>(null) }
    val sheetState   = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = P_Card,
        dragHandle       = null,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Select Platform",
                        fontSize   = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color      = P_Dark
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        product.title,
                        fontSize = 12.sp,
                        color    = P_Mid,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .size(30.dp).clip(CircleShape)
                        .border(1.dp, P_Border, CircleShape)
                        .background(P_Bg)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, tint = P_Mid, modifier = Modifier.size(15.dp))
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("android" to "🤖  Android", "ios" to "🍎  iOS").forEach { (key, label) ->
                    val sel = selectedType == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                1.5.dp,
                                if (sel) P_Green else P_Border,
                                RoundedCornerShape(12.dp)
                            )
                            .background(if (sel) P_GreenSurface else P_Bg)
                            .clickable { selectedType = key }
                            .padding(vertical = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontSize   = 15.sp,
                            fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                            color      = if (sel) P_GreenDark else P_Mid
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick   = { selectedType?.let { onSelect(it) } },
                enabled   = selectedType != null,
                modifier  = Modifier.fillMaxWidth().height(50.dp),
                shape     = RoundedCornerShape(12.dp),
                colors    = ButtonDefaults.buttonColors(containerColor = P_Green),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add to Cart", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick  = onDismiss,
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Text("Cancel", color = P_Mid, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}

// ── Top Bar ──────────────────────────────────────────────────────
@Composable
private fun ShopTopBar(cartCount: Int, onCartClick: () -> Unit) {
    Surface(color = P_Bg, shadowElevation = 0.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Shop", fontSize = 26.sp,
                    fontWeight = FontWeight.Black, color = P_Dark
                )
                Text(
                    "Protect every vehicle you own",
                    fontSize = 12.sp, color = P_Mid
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp).clip(CircleShape).background(P_ChipBg)
                    .clickable { /* TODO open search */ },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Search, null, tint = P_Dark, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Box {
                Box(
                    modifier = Modifier
                        .size(40.dp).clip(CircleShape).background(P_ChipBg)
                        .clickable { onCartClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.ShoppingCart, null,
                        tint = P_Dark, modifier = Modifier.size(20.dp)
                    )
                }
                if (cartCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(17.dp).clip(CircleShape).background(P_Red)
                            .border(1.5.dp, P_Bg, CircleShape)
                            .align(Alignment.TopEnd).offset(x = 3.dp, y = (-3).dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            cartCount.toString(), fontSize = 9.sp,
                            fontWeight = FontWeight.Black, color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ── Category Chips ───────────────────────────────────────────────
@Composable
private fun CategoryChips(selected: String, onSelect: (String) -> Unit) {
    data class Chip(val id: String, val label: String, val emoji: String)
    val chips = listOf(
        Chip("all",      "All",        "🛒"),
        Chip("gps",      "GPS Devices","📡"),
        Chip("trackers", "Trackers",   "🎯"),
        Chip("qr",       "QR Tags",    "🏷️")
    )
    LazyRow(
        contentPadding          = PaddingValues(horizontal = 20.dp),
        horizontalArrangement   = Arrangement.spacedBy(10.dp)
    ) {
        items(chips) { chip ->
            val isSel = chip.id == selected
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (isSel) P_Green else P_Card)
                    .border(1.dp, if (isSel) P_Green else P_Border, RoundedCornerShape(22.dp))
                    .clickable { onSelect(chip.id) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(chip.emoji, fontSize = 14.sp)
                Text(
                    chip.label, fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (isSel) Color.White else P_Dark
                )
            }
        }
    }
}

// ── Dark Promo Banner ────────────────────────────────────────────
//@Composable
//private fun DarkPromoBanner(
//    profile: PromoProfile,
//    onPlanDetails: () -> Unit,
//    onGetRecommended: () -> Unit
//) {
//    val icon = when (profile) {
//        PromoProfile.NEW_USER        -> Icons.Default.SettingsInputAntenna
//        PromoProfile.HAS_PARKING_TAG -> Icons.Default.LocationOn
//        PromoProfile.HAS_GPS         -> Icons.Default.LocalOffer
//        PromoProfile.HAS_BOTH        -> Icons.Default.CheckCircle
//    }
//    val title = when (profile) {
//        PromoProfile.NEW_USER        -> "GPS + 1 Year Plan Free"
//        PromoProfile.HAS_PARKING_TAG -> "Upgrade to GPS PRO"
//        PromoProfile.HAS_GPS         -> "Add a QR Parking Tag"
//        PromoProfile.HAS_BOTH        -> "You're Fully Protected!"
//    }
//    val subtitle = when (profile) {
//        PromoProfile.NEW_USER        -> "Save ₹2,000 on Eva2z GPS Pro this month"
//        PromoProfile.HAS_PARKING_TAG -> "You use parking tags. Now track your vehicle live, anywhere in the world."
//        PromoProfile.HAS_GPS         -> "GPS tracking active. Let visitors reach you when you block them."
//        PromoProfile.HAS_BOTH        -> "GPS tracking + QR parking tag active on your account."
//    }
//    val primaryLabel = when (profile) {
//        PromoProfile.NEW_USER        -> "Get GPS Pro"
//        PromoProfile.HAS_PARKING_TAG -> "Get GPS"
//        PromoProfile.HAS_GPS         -> "Get QR Tag"
//        PromoProfile.HAS_BOTH        -> "Browse All"
//    }
//    // Secondary "info" button only shown for profiles where plan details are relevant
//    val showPlanBtn = profile == PromoProfile.NEW_USER || profile == PromoProfile.HAS_PARKING_TAG
//    val planBtnLabel = "Plan details"
//
//    // Icon box tint: success green for HAS_BOTH, white otherwise
//    val iconBg   = if (profile == PromoProfile.HAS_BOTH) P_Green.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.09f)
//    val iconTint = if (profile == PromoProfile.HAS_BOTH) P_GreenLight else Color.White
//
//    Card(
//        modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
//        shape     = RoundedCornerShape(18.dp),
//        colors    = CardDefaults.cardColors(containerColor = P_BannerBg),
//        elevation = CardDefaults.cardElevation(0.dp)
//    ) {
//        Row(
//            modifier          = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 16.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Box(
//                modifier = Modifier
//                    .size(56.dp).clip(RoundedCornerShape(14.dp))
//                    .background(iconBg),
//                contentAlignment = Alignment.Center
//            ) {
//                Icon(icon, null, tint = iconTint, modifier = Modifier.size(28.dp))
//            }
//            Spacer(Modifier.width(14.dp))
//            Column(modifier = Modifier.weight(1f)) {
//                Text(
//                    title, fontSize = 15.sp,
//                    fontWeight = FontWeight.Bold, color = Color.White
//                )
//                Spacer(Modifier.height(3.dp))
//                Text(
//                    subtitle,
//                    fontSize = 11.sp, color = Color.White.copy(alpha = 0.65f),
//                    lineHeight = 15.sp
//                )
//                Spacer(Modifier.height(10.dp))
//                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//                    if (showPlanBtn) {
//                        Box(
//                            modifier = Modifier
//                                .clip(RoundedCornerShape(20.dp))
//                                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
//                                .clickable { onPlanDetails() }
//                                .padding(horizontal = 12.dp, vertical = 7.dp)
//                        ) {
//                            Text(
//                                planBtnLabel, fontSize = 11.sp,
//                                color = Color.White, fontWeight = FontWeight.SemiBold
//                            )
//                        }
//                    }
//                    Row(
//                        modifier = Modifier
//                            .clip(RoundedCornerShape(20.dp))
//                            .background(P_Green)
//                            .clickable { onGetRecommended() }
//                            .padding(horizontal = 12.dp, vertical = 7.dp),
//                        verticalAlignment     = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.spacedBy(4.dp)
//                    ) {
//                        Text(
//                            primaryLabel, fontSize = 11.sp,
//                            color = Color.White, fontWeight = FontWeight.SemiBold
//                        )
//                        Icon(
//                            Icons.Default.ArrowForward, null,
//                            tint = Color.White, modifier = Modifier.size(12.dp)
//                        )
//                    }
//                }
//            }
//        }
//    }
//}

// ── Plan Details Bottom Sheet ────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanDetailsBottomSheet(price: Int, onDismiss: () -> Unit, onGetGpsPro: () -> Unit) {
    val sheetState  = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = P_Bg,
        dragHandle       = null,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        "1 Year Plan — what's included",
                        fontSize = 17.sp, fontWeight = FontWeight.Bold,
                        color = P_Dark, lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Eva2z GPS Pro device + 12 months of premium tracking, geofencing & alerts. Worth ₹2,000, free this month.",
                        fontSize = 12.sp, color = P_Mid, lineHeight = 17.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(30.dp).clip(CircleShape)
                        .border(1.dp, P_Border, CircleShape)
                        .background(P_Card)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, tint = P_Mid, modifier = Modifier.size(15.dp))
                }
            }

            Spacer(Modifier.height(18.dp))
            FeatureRow(Icons.Default.LocationOn, "Live GPS tracking",
                "Real-time location refreshed every 10 seconds, with full trip history for 90 days.")
            Spacer(Modifier.height(10.dp))
            FeatureRow(Icons.Default.Shield, "Geofencing",
                "Draw safe zones on the map. Get instant alerts when your vehicle enters or exits.")
            Spacer(Modifier.height(10.dp))
            FeatureRow(Icons.Default.Notifications, "Smart alerts",
                "Tow detection, ignition on/off, harsh braking, overspeed, and SOS alerts.")
            Spacer(Modifier.height(10.dp))
            FeatureRow(Icons.Default.Build, "Engine immobilise",
                "Remotely cut fuel pump in case of theft, straight from the app.")

            Spacer(Modifier.height(16.dp))

            InfoBlock(title = "How geofencing works") {
                NumberedStep(num = 1, text = buildAnnotatedString {
                    append("Open ")
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = P_Dark)) {
                        append("Track → Geofences")
                    }
                    append(" and drop a pin.")
                })
                NumberedStep(num = 2, text = buildAnnotatedString {
                    append("Set a radius (50m – 5km) and choose entry/exit alerts.")
                })
                NumberedStep(num = 3, text = buildAnnotatedString {
                    append("Phone gets a push notification within ~10s of crossing the boundary.")
                })
            }

            Spacer(Modifier.height(10.dp))

            InfoBlock(title = "How live GPS works") {
                Text(
                    "The wired GPS Pro device installs under your dashboard and sends a location ping every 10 seconds while moving and every 5 minutes when parked.",
                    fontSize = 12.sp, color = P_Mid, lineHeight = 17.sp
                )
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick   = onGetGpsPro,
                modifier  = Modifier.fillMaxWidth().height(52.dp),
                shape     = RoundedCornerShape(14.dp),
                colors    = ButtonDefaults.buttonColors(containerColor = P_Green),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Get GPS Pro — ₹$price", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }


            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(46.dp)) {
                Text("Maybe later", color = P_Mid, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(P_Card).padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(P_GreenSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = P_Green, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = P_Dark)
            Spacer(Modifier.height(3.dp))
            Text(desc, fontSize = 12.sp, color = P_Mid, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun InfoBlock(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(P_InfoBoxBg).padding(14.dp)
    ) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = P_Dark)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun NumberedStep(num: Int, text: androidx.compose.ui.text.AnnotatedString) {
    Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
        Text("$num. ", fontSize = 12.sp, color = P_Mid, fontWeight = FontWeight.SemiBold, lineHeight = 17.sp)
        Text(text = text, fontSize = 12.sp, color = P_Mid, lineHeight = 17.sp)
    }
}

// ── Featured Card ────────────────────────────────────────────────
@Composable
private fun FeaturedCard(product: ShopProduct, badge: String, onClick: () -> Unit, onAdd: () -> Unit) {
    val fakeOld = (product.price * 1.5).toInt()

    Card(
        modifier = Modifier
            .width(178.dp)
            .clickable { onClick() }
            .shadow(3.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x10000000)),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = P_Card),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(128.dp).background(P_ImageBg)) {
                AsyncImage(
                    model              = product.imageUrl,
                    contentDescription = null,
                    modifier           = Modifier.fillMaxSize().padding(14.dp),
                    contentScale       = ContentScale.Fit,
                    placeholder        = painterResource(Res.drawable.app_logo),
                    error              = painterResource(Res.drawable.app_logo)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart).padding(10.dp)
                        .clip(RoundedCornerShape(4.dp)).background(P_Green)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        badge, fontSize = 8.sp, fontWeight = FontWeight.Black,
                        color = Color.White, letterSpacing = 0.6.sp
                    )
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    product.title, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold, color = P_Dark,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp, modifier = Modifier.height(34.dp)
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = P_Gold, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("${product.rating}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = P_Dark)
                    Text(" · ${product.ratingCount}", fontSize = 11.sp, color = P_Light)
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("₹${product.price.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.Black, color = P_Dark)
                        Text("₹$fakeOld", fontSize = 10.sp, color = P_Light, textDecoration = TextDecoration.LineThrough)
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp).clip(CircleShape).background(P_Green)
                            .clickable { onAdd() }
                            .shadow(2.dp, CircleShape, ambientColor = P_Green.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

// ── List Product Card ────────────────────────────────────────────
@Composable
private fun ListProductCard(product: ShopProduct, onClick: () -> Unit, onAdd: () -> Unit) {
    val fakeOld = (product.price * 1.5).toInt()
    val disc    = if (fakeOld > 0) (((fakeOld - product.price) / fakeOld) * 100).toInt() else 0

    Card(
        modifier = Modifier
            .fillMaxWidth().padding(horizontal = 20.dp)
            .shadow(3.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x10000000))
            .clickable { onClick() },
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = P_Card),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(88.dp).clip(RoundedCornerShape(12.dp)).background(P_ImageBg)
            ) {
                AsyncImage(
                    model              = product.imageUrl,
                    contentDescription = null,
                    modifier           = Modifier.fillMaxSize().padding(10.dp),
                    contentScale       = ContentScale.Fit,
                    placeholder        = painterResource(Res.drawable.app_logo),
                    error              = painterResource(Res.drawable.app_logo)
                )
                if (disc > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart).padding(5.dp)
                            .clip(RoundedCornerShape(5.dp)).background(P_Red)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text("$disc% OFF", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    product.title, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    color = P_Dark, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    getProductTagline(product), fontSize = 11.sp,
                    color = P_Mid, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = P_Gold, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("${product.rating}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = P_Dark)
                    Text("  (${product.ratingCount})", fontSize = 11.sp, color = P_Light)
                }
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("₹${product.price.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.Black, color = P_Dark)
                    Spacer(Modifier.width(6.dp))
                    Text("₹$fakeOld", fontSize = 11.sp, color = P_Light, textDecoration = TextDecoration.LineThrough)
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp)).background(P_Green)
                    .clickable { onAdd() }
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Text("Add", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ── ShopScreenHost ───────────────────────────────────────────────
@Composable
fun ShopScreenHost(category: String? = null) {
    val viewModel = ShopViewModelProvider.viewModel
    val state     by viewModel.state.collectAsState()
    val navigator = LocalNavigator.current
    val deepLinkDestination by DeepLinkBus.navigateTo.collectAsState(initial = "")

    val filteredProducts = state.products.filter { dto ->
        when (category) {
            "wired"    -> dto.device_type?.lowercase() == "wired"
            "wireless" -> dto.device_type?.lowercase() == "wireless"
            else       -> true
        }
    }

    LaunchedEffect(Unit) {
        Analytics.logEvent("screen_view", mapOf(
            "screen_name"    to "shop_screen",
            "screen_class"   to "ShopScreen",
            "item_category"  to "iot_devices",
            "source"         to "home_or_menu"
        ))
        viewModel.loadData()
    }

    LaunchedEffect(deepLinkDestination, state.products) {
        val dest = deepLinkDestination ?: return@LaunchedEffect

        if (!dest.startsWith("open_product_") && !dest.startsWith("product_detail_"))
            return@LaunchedEffect

        val pid = when {
            dest.startsWith("open_product_")   -> dest.removePrefix("open_product_")
                .substringBefore("_").toIntOrNull()
            dest.startsWith("product_detail_") -> dest.removePrefix("product_detail_")
                .substringBefore("_").toIntOrNull()
            else -> null
        } ?: return@LaunchedEffect

        if (state.products.isEmpty()) { viewModel.loadData(); return@LaunchedEffect }

        val dto = state.products.firstOrNull { it.id == pid } ?: return@LaunchedEffect
        val product = ShopProduct(
            id = dto.id, title = dto.name ?: "", subtitle = dto.device_type ?: "",
            price = dto.sale_price?.toDoubleOrNull() ?: 0.0,
            rechargePrice = dto.recharge_price?.toDoubleOrNull() ?: 0.0,
            promoAmount = dto.promo_amt?.toDoubleOrNull() ?: 0.0,
            promoCode = dto.promo_code ?: "", imageUrl = dto.image ?: "",
            description = dto.description ?: "",
            features = listOfNotNull(dto.features1, dto.features2, dto.features3,
                dto.features4, dto.features5, dto.features6, dto.features7),
            deviceType = dto.device_type ?: "",
            rating = getRating(dto.id), ratingCount = getRatingCount(dto.id),
            isFavorite = viewModel.isFavorite(dto.id)
        )
        DeepLinkBus.clear()
        navigator?.push(
            ProductDetailsScreen(product = product, onToggleFavorite = { p -> viewModel.toggleWishlist(p.id) })
        )
    }

    if (state.loading) {
        Box(modifier = Modifier.fillMaxSize().background(P_Bg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = P_Green, strokeWidth = 2.dp)
        }
        return
    }

    LaunchedEffect(state.products) {
        if (state.products.isNotEmpty()) {
            Analytics.logEvent("view_item_list", mapOf(
                "item_list_id"   to "shop_device_list",
                "item_list_name" to "Location Tracker Device",
                "currency"       to "INR",
                "items"          to state.products.mapIndexed { index, p ->
                    mapOf("item_id" to p.id.toString(), "item_name" to (p.name ?: ""),
                        "item_brand" to "EVA2Z", "item_category" to "iot_devices",
                        "item_variant" to (p.device_type ?: ""),
                        "price" to (p.sale_price?.toDoubleOrNull() ?: 0.0),
                        "index" to index).toString()
                }
            ))
        }
    }

    val mappedProducts = filteredProducts.map { dto ->
        ShopProduct(
            id = dto.id, title = dto.name ?: "", subtitle = dto.device_type ?: "",
            price = dto.sale_price?.toDoubleOrNull() ?: 0.0,
            rechargePrice = dto.recharge_price?.toDoubleOrNull() ?: 0.0,
            promoAmount = dto.promo_amt?.toDoubleOrNull() ?: 0.0,
            promoCode = dto.promo_code ?: "", imageUrl = dto.image ?: "",
            description = dto.description ?: "",
            features = listOfNotNull(dto.features1, dto.features2, dto.features3,
                dto.features4, dto.features5, dto.features6, dto.features7),
            deviceType = dto.device_type ?: "", rating = getRating(dto.id),
            ratingCount = getRatingCount(dto.id), isFavorite = viewModel.isFavorite(dto.id)
        )
    }

    ShopScreen(
        products      = mappedProducts,
        cartCount     = state.cartCount,
        totalPrice    = state.totalPrice,
        hasGpsDevice  = state.hasGpsDevice,
        hasParkingTag = state.hasParkingTag,
        onCartClick   = { navigator?.push(CartScreen()) },
        onAddToCart = { product, deviceType ->
            viewModel.addToCart(
                add         = "add",
                productId   = product.id,
                quantity    = 1,
                plan_type   = "",
                device_type = deviceType,
                onSuccess   = {
                    Analytics.logEvent("add_to_cart", mapOf(
                        "currency"    to "INR",
                        "value"       to product.price,
                        "item_id"     to product.id,
                        "item_name"   to product.title,
                        "device_type" to deviceType,
                        "source"      to "shop_listing"
                    ))
                }
            )
        },
        onPlaceOrder = { product ->
            navigator?.push(
                ProductDetailsScreen(
                    product          = product,
                    onToggleFavorite = { p ->
                        viewModel.toggleWishlist(p.id)
                        Analytics.logEvent("add_to_wishlist", mapOf(
                            "currency" to "INR",
                            "items"    to listOf(mapOf(
                                "item_id"       to p.id.toString(),
                                "item_name"     to p.title,
                                "item_brand"    to "EVA2Z",
                                "item_category" to "iot_devices",
                                "item_variant"  to p.subtitle,
                                "price"         to p.price
                            ))
                        ))
                    }
                )
            )
        }
    )
}

// ── Aliases used by other screens in this package ────────────────
internal val ShopGreen       = P_Green
internal val ShopGreenDark   = P_GreenDark
internal val ShopGreenSoft   = P_GreenSurface
internal val ShopGreenMid    = P_GreenBorder
internal val ShopBg          = P_Bg
internal val ShopCard        = P_Card
internal val ShopTextPrimary = P_Dark
internal val ShopTextSec     = P_Mid
internal val ShopTextTert    = P_Light
internal val ShopError       = P_Red