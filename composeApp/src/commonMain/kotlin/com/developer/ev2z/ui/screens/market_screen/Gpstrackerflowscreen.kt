package com.developer.ev2z.ui.screens.market_screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import kotlinx.coroutines.delay

private val P_Orange    = Color(0xFFEA8C00)    // app-aligned orange
private val P_DarkGreen = Color(0xFF3A6330)    // medium-dark green for toast

private data class CRow(
    val criteria:  String,
    val eva2z:     String,
    val others:    String,
    val highlight: Boolean = true
)

private val compRows = listOf(
    CRow("Device + SIM",          "₹3,200",           "₹4,500+"),
    CRow("SIM Activation Charge", "No Charge",         "₹300–500"),
    CRow("Recharge Included",     "Yes",               "No"),
    CRow("App Fee Lifetime",      "Free",              "₹999/yr"),
    CRow("Live Tracking",         "✓",                "✓",  false),
    CRow("Fleet Management",      "Yes",               "No"),
    CRow("Accident & Overspeed",  "✓",                "✗"),
    CRow("Voice Monitoring",      "✓",                "✗"),
    CRow("Battery Backup",        "30 days / 6000mAh", "7 days"),
    CRow("Driving Score",         "Full Analytics",    "Basic"),
    CRow("Engine Cut-off",        "✓",                "✗"),
    CRow("Warranty",              "1yr + 1yr Extn.",  "6 months"),
    CRow("Removal Charge",        "₹150/year",         "₹500"),
)

private val fakeReviews = listOf(
    Triple("Rohit Sharma, Delhi",    "Ekdum badhiya product! Meri bike ka GPS bahut accurate hai. Peace of mind!", 5),
    Triple("Amit Varma, Mumbai",     "Fleet management feature ne mera kaam bahut aasan kar diya. Highly recommended!", 5),
    Triple("Priya Singh, Bangalore", "Anti-theft feature kamaal ka hai. Ek baar chori hone se bacha.", 4),
    Triple("Vikash Rathore, Jaipur", "Customer support bahut helpful hai. Professional team.", 5),
)

private val reviewColors = listOf(
    P_Green, Color(0xFF7C3AED), Color(0xFFD97706), Color(0xFFDC2626)
)

// ═══════════════════════════════════════════════════════════════
//  Screen Entry Point
// ═══════════════════════════════════════════════════════════════
class GpsTrackerFlowScreen(
    val wiredProduct:    ShopProduct?,
    val wirelessProduct: ShopProduct?,
    val onAddToCart:     (ShopProduct, String, String) -> Unit
) : Screen {
    @Composable
    override fun Content() {
        GpsTrackerFlowContent(
            wiredProduct    = wiredProduct,
            wirelessProduct = wirelessProduct,
            onAddToCart     = onAddToCart
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  Main Content
// ═══════════════════════════════════════════════════════════════
@Composable
fun GpsTrackerFlowContent(
    wiredProduct:    ShopProduct?,
    wirelessProduct: ShopProduct?,
    onAddToCart:     (ShopProduct, String, String) -> Unit
) {
    val navigator = LocalNavigator.current
    val viewModel = ShopViewModelProvider.viewModel
    val state by viewModel.state.collectAsState()

    var showDeviceSheet by remember { mutableStateOf(false) }
    var addedProduct by remember { mutableStateOf<ShopProduct?>(null) }
    var showAddedToast by remember { mutableStateOf(false) }

    LaunchedEffect(showAddedToast) {
        if (showAddedToast) {
            delay(2500)
            showAddedToast = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF7F8FA))) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = if (state.cartCount > 0) 82.dp else 24.dp)
        ) {
            Screen2TopBar(
                cartCount   = state.cartCount,
                onBackClick = { navigator?.pop() },
                onCartClick = { navigator?.push(CartScreen()) }
            )
            ComparisonSection(onBuyClick = { showDeviceSheet = true })
            Screen4Reviews()
            BookDemoInlineSection()
            Screen6FinalCta(onBuyClick = { showDeviceSheet = true })
        }

        // ── Floating Cart Bar ────────────────────────────────
        AnimatedVisibility(
            visible  = state.cartCount > 0,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter    = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit     = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            GpsCartBar(
                count   = state.cartCount,
                onClick = { navigator?.push(CartScreen()) }
            )
        }

        // ── Added to Cart Toast ──────────────────────────────
        AnimatedVisibility(
            visible  = showAddedToast,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (state.cartCount > 0) 90.dp else 20.dp)
                .padding(horizontal = 20.dp),
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit  = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
        ) {
            AddedToCartToast(product = addedProduct)
        }

        // ── Device Selector Sheet ────────────────────────────
        if (showDeviceSheet) {
            DeviceSelectorBottomSheet(
                wiredProduct    = wiredProduct,
                wirelessProduct = wirelessProduct,
                onDismiss       = { showDeviceSheet = false },
                onAddToCart     = { product ->
                    // ✅ FIXED: sab statements lambda ke ANDAR hain, comma nahi
                    showDeviceSheet = false
                    onAddToCart(product, "without_recharge", "")
                    addedProduct   = product
                    showAddedToast = true
                    Analytics.logEvent("add_to_cart", mapOf(
                        "item_id"   to product.id,
                        "item_name" to product.title,
                        "currency"  to "INR",
                        "value"     to product.price
                    ))
                }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Added to Cart Toast  — premium dark card
// ═══════════════════════════════════════════════════════════════
@Composable
private fun AddedToCartToast(product: ShopProduct?) {
    Card(
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = Color(0xFF0A2E1A)),
        elevation = CardDefaults.cardElevation(16.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp).clip(CircleShape)
                    .background(P_Green),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, null,
                    tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Added to Cart!", fontSize = 14.sp,
                    fontWeight = FontWeight.Bold, color = Color.White)
                Text(product?.title ?: "", fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f), maxLines = 1)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(P_Green.copy(alpha = 0.25f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("View Cart", fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold, color = P_GreenLight)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  GPS Cart Bar
// ═══════════════════════════════════════════════════════════════
@Composable
private fun GpsCartBar(count: Int, onClick: () -> Unit) {
    Surface(
        modifier        = Modifier.fillMaxWidth(),
        color           = Color.White,
        shadowElevation = 24.dp,
        tonalElevation  = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cart count pill
            Box(
                modifier = Modifier
                    .size(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(P_GreenSurface)
                    .border(1.dp, P_GreenBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ShoppingCart, null,
                        tint = P_Green, modifier = Modifier.size(16.dp))
                    Text(count.toString(), fontSize = 9.sp,
                        fontWeight = FontWeight.Black, color = P_Green)
                }
            }
            // Main button
            Button(
                onClick   = onClick,
                modifier  = Modifier.weight(1f).height(48.dp),
                shape     = RoundedCornerShape(13.dp),
                colors    = ButtonDefaults.buttonColors(containerColor = P_Green),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("View Cart", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("$count item${if (count != 1) "s" else ""}",
                            fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            color = Color.White)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  TopBar — premium with cart badge
// ═══════════════════════════════════════════════════════════════
@Composable
private fun Screen2TopBar(
    cartCount:   Int,
    onBackClick: () -> Unit,
    onCartClick: () -> Unit
) {
    Surface(
        color           = Color.White,
        shadowElevation = 0.dp,
        modifier        = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back
                Box(
                    modifier = Modifier
                        .size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF2F4F6))
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowBack, null,
                        tint = P_Dark, modifier = Modifier.size(18.dp))
                }

                // Title + subtitle
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Eva2z GPS Tracker",
                        fontSize = 15.sp, fontWeight = FontWeight.Bold, color = P_Dark)
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(5) {
                            Icon(Icons.Default.Star, null,
                                tint = P_Gold, modifier = Modifier.size(9.dp))
                        }
                        Text(" 4.9", fontSize = 10.sp,
                            color = P_Mid, fontWeight = FontWeight.Medium)
                    }
                }

                // Cart icon + badge
                Box(
                    modifier = Modifier
                        .size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (cartCount > 0) P_GreenSurface else Color(0xFFF2F4F6))
                        .border(
                            width = if (cartCount > 0) 1.dp else 0.dp,
                            color = if (cartCount > 0) P_GreenBorder else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onCartClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ShoppingBag, null,
                        tint = if (cartCount > 0) P_Green else P_Dark,
                        modifier = Modifier.size(19.dp))
                    if (cartCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(16.dp).clip(CircleShape)
                                .background(P_Red)
                                .align(Alignment.TopEnd)
                                .offset(x = 3.dp, y = (-3).dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(cartCount.toString(),
                                fontSize = 8.sp, fontWeight = FontWeight.Black,
                                color = Color.White)
                        }
                    }
                }
            }
            HorizontalDivider(color = P_Border, thickness = 0.5.dp)
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Comparison Section — premium redesign
// ═══════════════════════════════════════════════════════════════
@Composable
private fun ComparisonSection(onBuyClick: () -> Unit = {}) {
    Column(modifier = Modifier.fillMaxWidth()) {

        // Hero header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(
                    listOf(Color(0xFF061C0E), Color(0xFF0A2E1A), Color(0xFF0D3D22))))
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            // Decorative circles
            repeat(3) { i ->
                Box(
                    modifier = Modifier
                        .size((80 + i * 40).dp)
                        .offset(x = (200 - i * 30).dp, y = (-20 + i * 15).dp)
                        .clip(CircleShape)
                        .background(P_Green.copy(alpha = 0.07f))
                )
            }
            Column {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(P_Green.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("COMPARISON GUIDE", fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = P_GreenLight, letterSpacing = 1.2.sp)
                }
                Spacer(Modifier.height(10.dp))
                Text("Why Eva2z Beats\nEvery Other GPS",
                    fontSize = 22.sp, fontWeight = FontWeight.Black,
                    color = Color.White, lineHeight = 30.sp)
                Spacer(Modifier.height(6.dp))
                Text("Side-by-side comparison of price, features & value",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.55f), lineHeight = 18.sp)
            }
        }

        // Column headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF0F2F5))
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text("FEATURE", fontSize = 9.sp, fontWeight = FontWeight.Bold,
                color = P_Light, modifier = Modifier.weight(1.6f), letterSpacing = 1.sp)
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(P_Green)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("EVA2Z", fontSize = 10.sp,
                        fontWeight = FontWeight.Bold, color = Color.White,
                        textAlign = TextAlign.Center)
                }
            }
            Text("Others", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                color = P_Light, modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center)
        }

        // Table rows
        compRows.forEachIndexed { index, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (index % 2 == 0) Color.White else Color(0xFFFAFBFD))
                    .padding(horizontal = 20.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(row.criteria, fontSize = 12.sp, color = P_Mid,
                    modifier = Modifier.weight(1.6f), lineHeight = 17.sp)

                // Eva2z value
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (row.eva2z == "✓") {
                        Box(
                            modifier = Modifier
                                .size(22.dp).clip(CircleShape)
                                .background(P_GreenSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, null,
                                tint = P_Green, modifier = Modifier.size(13.dp))
                        }
                    } else {
                        Text(row.eva2z, fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (row.highlight) P_Green else P_Mid,
                            textAlign = TextAlign.Center)
                    }
                }

                // Others value
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (row.others == "✗") {
                        Box(
                            modifier = Modifier
                                .size(22.dp).clip(CircleShape)
                                .background(Color(0xFFFFF0F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, null,
                                tint = P_Red, modifier = Modifier.size(13.dp))
                        }
                    } else {
                        Text(row.others, fontSize = 11.sp,
                            color = if (row.others == "No") P_Red else P_Light,
                            textAlign = TextAlign.Center)
                    }
                }
            }
            if (index < compRows.lastIndex)
                HorizontalDivider(color = P_Border, thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 20.dp))
        }

        // CTA Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Stats row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(
                    "50K+" to "Happy Customers",
                    "4.9★" to "Avg Rating",
                    "1 Yr" to "Warranty"
                ).forEach { (val1, label) ->
                    Card(
                        modifier  = Modifier.weight(1f),
                        shape     = RoundedCornerShape(12.dp),
                        colors    = CardDefaults.cardColors(containerColor = P_GreenSurface),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(
                            modifier            = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(val1, fontSize = 16.sp,
                                fontWeight = FontWeight.Black, color = P_Green)
                            Text(label, fontSize = 9.sp, color = P_Mid,
                                textAlign = TextAlign.Center, lineHeight = 13.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick   = onBuyClick,
                modifier  = Modifier.fillMaxWidth().height(52.dp),
                shape     = RoundedCornerShape(13.dp),
                colors    = ButtonDefaults.buttonColors(containerColor = P_Green),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text("Buy Now", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Security, null,
                    tint = P_Green, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("100% Secure", fontSize = 11.sp, color = P_Mid)
                Text("  ·  ", fontSize = 11.sp, color = P_Border)
                Icon(Icons.Default.LocalShipping, null,
                    tint = P_Green, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("Free Delivery", fontSize = 11.sp, color = P_Mid)
                Text("  ·  ", fontSize = 11.sp, color = P_Border)
                Icon(Icons.Default.Replay, null,
                    tint = P_Green, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("Easy Returns", fontSize = 11.sp, color = P_Mid)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Device Card — premium redesign
// ═══════════════════════════════════════════════════════════════
@Composable
private fun DeviceCard(
    product:     ShopProduct,
    badge:       String,
    badgeColor:  Color = P_Green,
    onAddToCart: () -> Unit
) {
    val fakeOld = (product.price * 1.4).toInt()
    val savings = fakeOld - product.price.toInt()
    val discPct = ((savings.toFloat() / fakeOld) * 100).toInt()

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = badgeColor.copy(alpha = 0.15f)),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column {
            // Colored badge strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(
                        listOf(badgeColor, badgeColor.copy(alpha = 0.8f))))
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Verified, null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(13.dp))
                    Text(badge, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White, letterSpacing = 0.5.sp)
                }
            }

            Row(
                modifier          = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Product image
                Box(
                    modifier = Modifier
                        .size(100.dp).clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF8FAFB))
                        .border(1.dp, P_Border, RoundedCornerShape(14.dp))
                ) {
                    AsyncImage(
                        model              = product.imageUrl,
                        contentDescription = null,
                        modifier           = Modifier.fillMaxSize().padding(10.dp),
                        contentScale       = ContentScale.Fit
                    )
                    // Discount pill on image
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(P_Red)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text("$discPct% OFF", fontSize = 8.sp,
                            fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(product.title, fontSize = 14.sp,
                        fontWeight = FontWeight.Bold, color = P_Dark, lineHeight = 19.sp)
                    Spacer(Modifier.height(8.dp))
                    product.features.take(3).forEach { feat ->
                        Row(
                            modifier          = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(15.dp).clip(CircleShape)
                                    .background(P_GreenSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, null,
                                    tint = P_Green, modifier = Modifier.size(9.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(feat, fontSize = 11.sp, color = P_Mid, lineHeight = 15.sp)
                        }
                    }
                }
            }

            HorizontalDivider(color = P_Border, thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 16.dp))

            // Price + CTA
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("₹${product.price.toInt()}", fontSize = 24.sp,
                            fontWeight = FontWeight.Black, color = P_Dark)
                        Spacer(Modifier.width(8.dp))
                        Text("₹$fakeOld", fontSize = 13.sp, color = P_Light,
                            textDecoration = TextDecoration.LineThrough)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(P_GreenSurface)
                            .border(1.dp, P_GreenBorder, RoundedCornerShape(5.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text("You save ₹$savings", fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold, color = P_Green)
                    }
                }
                Button(
                    onClick        = onAddToCart,
                    shape          = RoundedCornerShape(12.dp),
                    colors         = ButtonDefaults.buttonColors(containerColor = P_Green),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 13.dp),
                    elevation      = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Icon(Icons.Default.ShoppingCart, null,
                        modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add to Cart", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Book Demo Inline Section
// ═══════════════════════════════════════════════════════════════
@Composable
private fun BookDemoInlineSection() {
    var name            by remember { mutableStateOf("") }
    var phone           by remember { mutableStateOf("") }
    var vehicleType     by remember { mutableStateOf("") }
    var vehicleExpanded by remember { mutableStateOf(false) }
    var submitted       by remember { mutableStateOf(false) }
    val isValid = name.isNotBlank() && phone.isNotBlank() && vehicleType.isNotBlank()

    Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
        // Dark header
        Box(
            modifier = Modifier
                .fillMaxWidth().height(190.dp)
                .background(Brush.verticalGradient(
                    listOf(Color(0xFF061C0E), Color(0xFF0A2E1A), Color(0xFF0D3D22))))
        ) {
            repeat(5) { i ->
                Box(
                    modifier = Modifier
                        .size((40 + i * 20).dp)
                        .offset(x = (i * 50 - 30).dp, y = (if (i % 2 == 0) 5 else 80).dp)
                        .clip(CircleShape)
                        .background(P_Green.copy(alpha = 0.1f))
                )
            }
            Column(
                modifier            = Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(P_Green.copy(alpha = 0.18f))
                        .border(1.dp, P_Green.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Text("LIVE DEMO AVAILABLE", fontSize = 9.sp,
                        fontWeight = FontWeight.Bold, color = P_GreenLight, letterSpacing = 1.2.sp)
                }
                Spacer(Modifier.height(12.dp))
                Text("See Eva2z in Action", fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White, textAlign = TextAlign.Center, lineHeight = 30.sp)
                Spacer(Modifier.height(6.dp))
                Text("Book a free live demo — we'll show you everything.",
                    fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center, lineHeight = 18.sp)
            }
        }

        if (submitted) {
            Column(
                modifier            = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape)
                        .background(P_GreenSurface)
                        .border(2.dp, P_GreenBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, null,
                        tint = P_Green, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("Demo Booked!", fontSize = 20.sp,
                    fontWeight = FontWeight.Bold, color = P_Dark)
                Spacer(Modifier.height(6.dp))
                Text("Our team will call you within 24 hours.",
                    fontSize = 13.sp, color = P_Mid, textAlign = TextAlign.Center)
            }
        } else {
            Column(modifier = Modifier.padding(20.dp)) {
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Full Name") },
                    placeholder   = { Text("John Doe") },
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = P_Green,
                        focusedLabelColor    = P_Green,
                        unfocusedBorderColor = P_Border),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Person, null,
                            tint = P_Light, modifier = Modifier.size(18.dp))
                    }
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value         = phone,
                    onValueChange = { phone = it },
                    label         = { Text("Phone Number") },
                    placeholder   = { Text("+91 98765 43210") },
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = P_Green,
                        focusedLabelColor    = P_Green,
                        unfocusedBorderColor = P_Border),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Phone, null,
                            tint = P_Light, modifier = Modifier.size(18.dp))
                    }
                )
                Spacer(Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value         = vehicleType,
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("Vehicle Type") },
                        placeholder   = { Text("Select vehicle type") },
                        trailingIcon  = {
                            Icon(
                                if (vehicleExpanded) Icons.Default.KeyboardArrowUp
                                else Icons.Default.KeyboardArrowDown,
                                null, tint = P_Mid,
                                modifier = Modifier.clickable { vehicleExpanded = !vehicleExpanded }
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.DirectionsCar, null,
                                tint = P_Light, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.fillMaxWidth()
                            .clickable { vehicleExpanded = !vehicleExpanded },
                        shape    = RoundedCornerShape(12.dp),
                        colors   = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = P_Green,
                            focusedLabelColor    = P_Green,
                            unfocusedBorderColor = P_Border)
                    )
                    DropdownMenu(
                        expanded         = vehicleExpanded,
                        onDismissRequest = { vehicleExpanded = false },
                        modifier         = Modifier.fillMaxWidth()
                    ) {
                        listOf("Car", "Bike", "Truck", "Bus", "Auto", "Other").forEach { type ->
                            DropdownMenuItem(
                                text    = { Text(type, fontSize = 14.sp, color = P_Dark) },
                                onClick = { vehicleType = type; vehicleExpanded = false }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick   = { if (isValid) submitted = true },
                    modifier  = Modifier.fillMaxWidth().height(52.dp),
                    shape     = RoundedCornerShape(13.dp),
                    colors    = ButtonDefaults.buttonColors(
                        containerColor         = P_Orange,
                        disabledContainerColor = P_Orange.copy(alpha = 0.4f)),
                    elevation = ButtonDefaults.buttonElevation(0.dp),
                    enabled   = isValid
                ) {
                    Icon(Icons.Default.PlayCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Book Free Demo", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, null,
                        tint = P_Green, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("100% Free · No commitment · Call within 24hrs",
                        fontSize = 11.sp, color = P_Mid)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Screen 4 — Reviews
// ═══════════════════════════════════════════════════════════════
@Composable
private fun Screen4Reviews() {
    Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Customer Reviews", fontSize = 18.sp,
                    fontWeight = FontWeight.Bold, color = P_Dark)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(P_GreenSurface)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text("Verified", fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold, color = P_Green)
                }
            }
            Spacer(Modifier.height(16.dp))

            // Rating summary card
            Card(
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFB)),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier  = Modifier.fillMaxWidth()
                    .border(1.dp, P_Border, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier          = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Big rating
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("4.9", fontSize = 48.sp,
                            fontWeight = FontWeight.Black, color = P_Dark)
                        Row {
                            repeat(5) {
                                Icon(Icons.Default.Star, null,
                                    tint = P_Gold, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text("6,988 reviews", fontSize = 11.sp, color = P_Light)
                    }

                    Spacer(Modifier.width(20.dp))

                    // Rating bars
                    Column(modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf(5 to 0.82f, 4 to 0.10f, 3 to 0.04f, 2 to 0.02f, 1 to 0.02f)
                            .forEach { (s, f) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("$s", fontSize = 11.sp, color = P_Mid,
                                        modifier = Modifier.width(12.dp))
                                    Icon(Icons.Default.Star, null,
                                        tint = P_Gold, modifier = Modifier.size(10.dp))
                                    Box(modifier = Modifier.weight(1f).height(5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(P_Border)) {
                                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(f)
                                            .background(
                                                if (s == 5) P_Green else P_GreenLight,
                                                RoundedCornerShape(3.dp)))
                                    }
                                    Text("${(f * 100).toInt()}%",
                                        fontSize = 10.sp, color = P_Light,
                                        modifier = Modifier.width(28.dp))
                                }
                            }
                    }
                }
            }

            // Filter chips
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Most Relevant", "Newest", "★ Highest").forEachIndexed { i, tab ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (i == 0) P_Green else Color(0xFFF0F2F5))
                            .border(
                                1.dp,
                                if (i == 0) P_Green else P_Border,
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(tab, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                            color = if (i == 0) Color.White else P_Mid)
                    }
                }
            }
        }

        HorizontalDivider(color = P_Border, thickness = 0.5.dp)

        // Review cards
        fakeReviews.forEachIndexed { index, (author, text, stars) ->
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(reviewColors[index % reviewColors.size]),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(author.first().toString(), fontSize = 16.sp,
                            fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(author, fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold, color = P_Dark)
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            repeat(stars) {
                                Icon(Icons.Default.Star, null,
                                    tint = P_Gold, modifier = Modifier.size(12.dp))
                            }
                            repeat(5 - stars) {
                                Icon(Icons.Default.Star, null,
                                    tint = P_Border, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                    // Verified badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(P_GreenSurface)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text("✓ Verified", fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold, color = P_Green)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(text, fontSize = 13.sp, color = P_Mid, lineHeight = 21.sp)
            }
            if (index < fakeReviews.lastIndex)
                HorizontalDivider(color = P_Border, thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 20.dp))
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Screen6FinalCta(onBuyClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {

        // Trust strip
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(P_GreenSurface)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.VerifiedUser, null,
                    tint = P_Green, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text("TRUSTED BY 5,000+ OWNERS", fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, color = P_Green, letterSpacing = 0.8.sp)
            }
        }

        // ── Main CTA ─────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 28.dp)
        ) {
            Row {
                Text("Protect Your ", fontSize = 28.sp,
                    fontWeight = FontWeight.Black, color = P_Dark, lineHeight = 36.sp)
            }
            Text("Vehicle Today", fontSize = 28.sp,
                fontWeight = FontWeight.Black, color = P_Green, lineHeight = 36.sp)

            Spacer(Modifier.height(10.dp))
            Text(
                "Join 5,000+ vehicle owners who trust Eva2z for real-time tracking, anti-theft alerts, and total peace of mind.",
                fontSize = 13.sp, color = P_Mid, lineHeight = 20.sp
            )
            Spacer(Modifier.height(20.dp))

            // Orange Buy button
            Button(
                onClick   = onBuyClick,
                modifier  = Modifier.fillMaxWidth().height(52.dp),
                shape     = RoundedCornerShape(12.dp),
                colors    = ButtonDefaults.buttonColors(containerColor = P_Orange),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Text("Buy Eva2z GPS", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(18.dp))
            }

            Spacer(Modifier.height(12.dp))

            // Book Demo — no click
            Box(
                modifier = Modifier
                    .fillMaxWidth().height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.5.dp, P_Border, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Book Demo", fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold, color = P_Dark)
            }

            Spacer(Modifier.height(20.dp))

            // Feature icons
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Icons.Default.LocationOn to "Real-time GPS",
                    Icons.Default.Security   to "Instant Alerts",
                    Icons.Default.Shield     to "Secure Data"
                ).forEach { (icon, label) ->
                    Column(modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                                .background(P_GreenSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, null, tint = P_Green, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(label, fontSize = 10.sp, color = P_Mid,
                            textAlign = TextAlign.Center, lineHeight = 14.sp,
                            fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // ── Map Section ───────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
        ) {
            // ✅ Real map image — OpenStreetMap static tile (India - Delhi NCR area)
            AsyncImage(
                model = "https://staticmap.openstreetmap.de/staticmap.php" +
                        "?center=28.6139,77.2090" +
                        "&zoom=13" +
                        "&size=600x400" +
                        "&maptype=mapnik",
                contentDescription = "Live Tracking Map",
                modifier           = Modifier.fillMaxSize(),
                contentScale       = ContentScale.Crop
            )

            // Slight overlay for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.08f))
            )

            // ── Location pin in center ─────────────────────
            Box(
                modifier         = Modifier.align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                // Outer pulse
                Box(
                    modifier = Modifier
                        .size(56.dp).clip(CircleShape)
                        .background(P_Green.copy(alpha = 0.2f))
                )
                // Middle ring
                Box(
                    modifier = Modifier
                        .size(36.dp).clip(CircleShape)
                        .background(P_Green.copy(alpha = 0.35f))
                )
                // Pin dot
                Box(
                    modifier = Modifier
                        .size(22.dp).clip(CircleShape)
                        .background(P_Green)
                        .border(2.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.LocationOn, null,
                        tint = Color.White, modifier = Modifier.size(13.dp))
                }
            }

            // ── Top label ─────────────────────────────────
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .border(1.dp, P_Border, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier.size(7.dp).clip(CircleShape)
                            .background(P_Green)
                    )
                    Text("Live Tracking", fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold, color = P_Dark)
                }
            }

            // ── "Parked • Secure" bottom badge ────────────
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp)
                    .shadow(8.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp).clip(CircleShape)
                            .background(P_GreenSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lock, null,
                            tint = P_Green, modifier = Modifier.size(16.dp))
                    }
                    Column {
                        Text("Parked • Secure", fontSize = 13.sp,
                            fontWeight = FontWeight.Bold, color = P_Dark)
                        Text("Last updated just now", fontSize = 10.sp, color = P_Light)
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(P_GreenSurface)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("● LIVE", fontSize = 9.sp,
                            fontWeight = FontWeight.Bold, color = P_Green)
                    }
                }
            }
        }
    }


        // Footer strip
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF040F07))
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            listOf(
                Icons.Default.Lock               to "Secure",
                Icons.Default.NotificationsActive to "Instant Alerts",
                Icons.Default.Verified           to "Certified"
            ).forEachIndexed { i, (icon, label) ->
                if (i > 0) Text("·", color = Color.White.copy(alpha = 0.2f), fontSize = 16.sp)
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(icon, null,
                        tint = P_GreenLight, modifier = Modifier.size(12.dp))
                    Text(label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                }
            }
        }
    }


// ═══════════════════════════════════════════════════════════════
//  Device Selector Bottom Sheet
// ═══════════════════════════════════════════════════════════════
@Composable
private fun DeviceSelectorBottomSheet(
    wiredProduct:    ShopProduct?,
    wirelessProduct: ShopProduct?,
    onDismiss:       () -> Unit,
    onAddToCart:     (ShopProduct) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 700.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color(0xFFF7F8FA))
                .clickable(onClick = {})
                .verticalScroll(rememberScrollState())
        ) {
            // Handle + header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .width(44.dp).height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(P_Border)
                            .align(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(14.dp))
                    Text("Choose Your GPS Tracker",
                        fontSize = 19.sp, fontWeight = FontWeight.Bold,
                        color = P_Dark, modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center)
                    Spacer(Modifier.height(5.dp))
                    Text("Both options include free SIM & lifetime app access",
                        fontSize = 12.sp, color = P_Light,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center, lineHeight = 17.sp)
                }
                // Close
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(34.dp).clip(CircleShape)
                        .background(Color(0xFFF0F2F5))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null,
                        tint = P_Mid, modifier = Modifier.size(17.dp))
                }
            }

            HorizontalDivider(color = P_Border, thickness = 0.5.dp)
            Spacer(Modifier.height(14.dp))

            wiredProduct?.let {
                DeviceCard(
                    product     = it,
                    badge       = "⚡  BEST FOR SECURITY",
                    badgeColor  = P_Green,
                    onAddToCart = { onAddToCart(it) }
                )
            }

            Spacer(Modifier.height(14.dp))

            wirelessProduct?.let {
                DeviceCard(
                    product     = it,
                    badge       = " WIRELESS FREEDOM",
                    badgeColor  = Color(0xFF7C3AED),
                    onAddToCart = { onAddToCart(it) }
                )
            }

            Spacer(Modifier.height(14.dp))

            // Trust bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                listOf(
                    Icons.Default.Security      to "Safe Checkout",
                    Icons.Default.LocalShipping to "Free Delivery",
                    Icons.Default.Replay        to "Easy Returns"
                ).forEachIndexed { i, (icon, label) ->
                    if (i > 0) Text("·", color = P_Border, fontSize = 18.sp)
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(icon, null, tint = P_Green, modifier = Modifier.size(13.dp))
                        Text(label, fontSize = 10.sp,
                            color = P_Mid, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}