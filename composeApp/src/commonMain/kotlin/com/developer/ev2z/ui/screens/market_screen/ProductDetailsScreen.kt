package com.developer.ev2z.ui.screens.market_screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.graphics.vector.ImageVector
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

class ProductDetailsScreen(
    val product: ShopProduct,
    val onToggleFavorite: (ShopProduct) -> Unit
) : Screen {
    @Composable
    override fun Content() {
        val viewModel = ShopViewModelProvider.viewModel
        ProductDetailsContent(
            product          = product,
            viewModel        = viewModel,
            onToggleFavorite = { onToggleFavorite(product) }
        )
    }
}

@Composable
fun ProductDetailsContent(
    product: ShopProduct,
    viewModel: ShopViewModel,
    onToggleFavorite: () -> Unit,
) {
    val navigator            = LocalNavigator.current
    var selectedType         by remember { mutableStateOf<String?>(null) }
    var withRecharge         by remember { mutableStateOf(false) }
    var showDeviceTypePrompt by remember { mutableStateOf(false) }
    var cartAdded            by remember { mutableStateOf(false) }
    var cartAddFailed        by remember { mutableStateOf(false) }
    var viewCartTapped       by remember { mutableStateOf(false) }

    LaunchedEffect(cartAdded) {
        if (cartAdded) {
            delay(2500)
            if (!viewCartTapped) navigator?.pop()
        }
    }

    LaunchedEffect(cartAddFailed) {
        if (cartAddFailed) {
            delay(2500)
            cartAddFailed = false
        }
    }

    val finalPrice = product.price + if (withRecharge) product.rechargePrice else 0.0
    val fakeOld    = (finalPrice * 1.5).toInt()
    val disc       = (((fakeOld - finalPrice) / fakeOld) * 100).toInt()
    val savings    = fakeOld - finalPrice.toInt()

    LaunchedEffect(Unit) {
        Analytics.logEvent("screen_view", mapOf(
            "screen_name" to "product_details",
            "item_id"     to product.id,
            "item_name"   to product.title
        ))
        Analytics.logEvent("view_item", mapOf(
            "currency" to "INR",
            "value"    to product.price,
            "item_id"  to product.id
        ))
    }

    Box(modifier = Modifier.fillMaxSize().background(P_Bg)) {

        // ── Scrollable Content ────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 88.dp)
        ) {

            // ── Hero Image ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Color.White)
            ) {
                AsyncImage(
                    model              = product.imageUrl,
                    contentDescription = null,
                    modifier           = Modifier.fillMaxSize().padding(28.dp),
                    contentScale       = ContentScale.Fit
                )

                // Bottom fade
                Box(
                    modifier = Modifier
                        .fillMaxWidth().height(60.dp)
                        .align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, P_Bg)))
                )

                // Back button
                Box(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(start = 16.dp, top = 12.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .shadow(6.dp, CircleShape)
                        .background(Color.White)
                        .clickable { navigator?.pop() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ArrowBack, null,
                        tint     = P_Dark,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Discount badge
                if (disc > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, bottom = 18.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(P_Red)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            "$disc% OFF",
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White
                        )
                    }
                }
            }

            // ── Product Info ──────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp)
                    .padding(top = 14.dp, bottom = 18.dp)
            ) {
                // Category label
                if (product.subtitle.isNotBlank()) {
                    Text(
                        product.subtitle.uppercase(),
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.Bold,
                        color         = P_Green,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(Modifier.height(5.dp))
                }

                // Title
                Text(
                    product.title,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color      = P_Dark,
                    lineHeight = 27.sp
                )

                Spacer(Modifier.height(10.dp))

                // Rating + verified
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFFFFFBEB))
                            .border(1.dp, Color(0xFFFEF3C7), RoundedCornerShape(5.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            Icons.Default.Star, null,
                            tint     = P_Gold,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            "${product.rating}",
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color(0xFF92400E)
                        )
                    }
                    Text(
                        "(${product.ratingCount} reviews)",
                        fontSize = 11.sp,
                        color    = P_Mid
                    )
                    Spacer(Modifier.weight(1f))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(P_GreenSurface)
                            .border(1.dp, P_GreenBorder, RoundedCornerShape(5.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            Icons.Default.Verified, null,
                            tint     = P_Green,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            "Eva2z Certified",
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = P_GreenDark
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Divider(color = P_Border, thickness = 1.dp)
                Spacer(Modifier.height(16.dp))

                // ── Price ─────────────────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "₹${finalPrice.toInt()}",
                        fontSize   = 32.sp,
                        fontWeight = FontWeight.Black,
                        color      = P_Dark
                    )
                    Column(modifier = Modifier.padding(bottom = 4.dp)) {
                        Text(
                            "₹$fakeOld",
                            fontSize       = 14.sp,
                            color          = P_Light,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(P_GreenSurface)
                            .border(1.dp, P_GreenBorder, RoundedCornerShape(5.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "You save ₹$savings",
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = P_GreenDark
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFFFEF2F2))
                            .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(5.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "$disc% off",
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = P_Red
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "Inclusive of all taxes",
                    fontSize = 11.sp,
                    color    = P_Mid
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Trust Badges ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple(Icons.Outlined.Autorenew,    "7-Day\nReturn",   "Returns"),
                    Triple(Icons.Outlined.SupportAgent, "24/7\nSupport",   "Support"),
                    Triple(Icons.Outlined.LocalShipping,"Free\nShipping",  "Shipping"),
                ).forEach { (icon, label, _) ->
                    DetailTrustBadge(icon = icon, label = label, modifier = Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Recharge Option ───────────────────────────────────────────
            if (product.rechargePrice > 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        "Add-ons",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color      = P_Dark
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                1.5.dp,
                                if (withRecharge) P_Green else P_Border,
                                RoundedCornerShape(12.dp)
                            )
                            .background(if (withRecharge) P_GreenSurface else Color.White)
                            .clickable {
                                withRecharge = !withRecharge
                                Analytics.logEvent("select_item", mapOf(
                                    "item_id"         to product.id,
                                    "selection_type"  to "recharge",
                                    "selection_value" to if (withRecharge) "with_recharge" else "without_recharge"
                                ))
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(P_GreenSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Refresh, null,
                                tint     = P_Green,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Annual Recharge Plan",
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = P_Dark
                            )
                            Text(
                                "Keep live tracking active for 1 year",
                                fontSize = 11.sp,
                                color    = P_Mid
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "+₹${product.rechargePrice.toInt()}",
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color      = P_Green
                            )
                            Checkbox(
                                checked         = withRecharge,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(
                                    checkedColor   = P_Green,
                                    uncheckedColor = P_Border
                                ),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── Platform / Device Type ────────────────────────────────────
            if (product.deviceType != "none") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        "Select Platform",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color      = P_Dark
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("android" to "🤖  Android", "ios" to "🍎  iOS").forEach { (key, label) ->
                            val sel = selectedType == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        1.5.dp,
                                        if (sel) P_Green else P_Border,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .background(if (sel) P_GreenSurface else Color.White)
                                    .clickable {
                                        selectedType = key
                                        Analytics.logEvent("select_item", mapOf(
                                            "item_id"         to product.id,
                                            "selection_type"  to "device_type",
                                            "selection_value" to key
                                        ))
                                    }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    fontSize   = 14.sp,
                                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                                    color      = if (sel) P_GreenDark else P_Mid
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── About ─────────────────────────────────────────────────────
            if (product.description.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    SectionHeader(icon = Icons.Outlined.Info, title = "About this product")
                    Spacer(Modifier.height(10.dp))
                    Text(
                        product.description,
                        fontSize   = 13.sp,
                        color      = P_Mid,
                        lineHeight = 21.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── Key Features ──────────────────────────────────────────────
            if (product.features.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    SectionHeader(icon = Icons.Default.CheckCircle, title = "Key Features")
                    Spacer(Modifier.height(12.dp))
                    product.features.forEach { feat ->
                        Row(
                            modifier          = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Check, null,
                                tint     = P_Green,
                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                feat,
                                fontSize   = 13.sp,
                                color      = P_Dark,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── Guarantee / Quality Section ───────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(P_GreenSurface)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(P_GreenBorder),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Shield, null,
                        tint     = P_GreenDark,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Eva2z Quality Guarantee",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color      = P_GreenDark
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Certified hardware · 1-year warranty · dedicated support",
                        fontSize   = 11.sp,
                        color      = P_GreenDark.copy(alpha = 0.7f),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // ── Sticky Bottom Bar ─────────────────────────────────────────────
        Surface(
            modifier        = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            color           = Color.White,
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.navigationBarsPadding()) {
                AnimatedContent(
                    targetState   = when {
                        cartAdded    -> "added"
                        cartAddFailed -> "failed"
                        else          -> "button"
                    },
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label          = "cart_bar"
                ) { state ->
                    when (state) {

                        // ── Success confirmation ──────────────────────────
                        "added" -> Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(P_GreenSurface)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp).clip(CircleShape).background(P_Green),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, null, tint = Color.White,
                                    modifier = Modifier.size(22.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Added to cart!", fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold, color = P_GreenDark)
                                Text(product.title, fontSize = 11.sp,
                                    color = P_Mid, maxLines = 1)
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(P_Green)
                                    .clickable {
                                        viewCartTapped = true
                                        // Pop this screen, then push cart — stack: Shop → Cart
                                        navigator?.pop()
                                        navigator?.push(CartScreen())
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("View Cart", fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold, color = Color.White)
                                Icon(Icons.Default.ArrowForward, null,
                                    tint = Color.White, modifier = Modifier.size(13.dp))
                            }
                        }

                        // ── Error state ───────────────────────────────────
                        "failed" -> Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFEF2F2))
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Outlined.ErrorOutline, null,
                                tint = P_Red, modifier = Modifier.size(24.dp))
                            Text("Failed to add. Please try again.",
                                fontSize = 13.sp, color = P_Red,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f))
                        }

                        // ── Default: price + button ───────────────────────
                        else -> Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column {
                                Text("₹${finalPrice.toInt()}", fontSize = 22.sp,
                                    fontWeight = FontWeight.Black, color = P_Dark)
                                Text("₹$fakeOld", fontSize = 12.sp,
                                    color = P_Light,
                                    textDecoration = TextDecoration.LineThrough)
                            }
                            Button(
                                onClick = {
                                    if (product.deviceType != "none" && selectedType == null) {
                                        showDeviceTypePrompt = true; return@Button
                                    }
                                    val planType = if (product.id == 10 || product.id == 11) {
                                        if (withRecharge) "with_recharge" else "without_recharge"
                                    } else null
                                    Analytics.logEvent("add_to_cart", mapOf(
                                        "currency"    to "INR",
                                        "value"       to finalPrice,
                                        "item_id"     to product.id,
                                        "item_name"   to product.title,
                                        "price"       to finalPrice,
                                        "quantity"    to 1,
                                        "device_type" to (selectedType ?: ""),
                                        "promo_code"  to product.promoCode
                                    ))
                                    viewModel.addToCart(
                                        add         = "add",
                                        productId   = product.id,
                                        quantity    = 1,
                                        plan_type   = planType ?: "",
                                        device_type = selectedType ?: "",
                                        onSuccess   = { cartAdded = true },
                                        onError     = { cartAddFailed = true }
                                    )
                                },
                                modifier  = Modifier.weight(1f).height(50.dp),
                                shape     = RoundedCornerShape(12.dp),
                                colors    = ButtonDefaults.buttonColors(containerColor = P_Green),
                                elevation = ButtonDefaults.buttonElevation(0.dp)
                            ) {
                                Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Add to Cart", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Device type prompt dialog
        if (showDeviceTypePrompt) {
            AlertDialog(
                onDismissRequest = { showDeviceTypePrompt = false },
                confirmButton = {
                    TextButton(onClick = { showDeviceTypePrompt = false }) {
                        Text("Got it", color = P_Green, fontWeight = FontWeight.SemiBold)
                    }
                },
                title = { Text("Select Platform", fontWeight = FontWeight.Bold) },
                text  = { Text("Please choose Android or iOS before adding to cart.") },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = P_Green, modifier = Modifier.size(18.dp))
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = P_Dark)
    }
}

@Composable
private fun DetailTrustBadge(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(P_Bg)
            .border(1.dp, P_Border, RoundedCornerShape(10.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(P_GreenSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = P_Green, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            fontSize   = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color      = P_Mid,
            textAlign  = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}