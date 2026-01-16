package com.developer.ev2z.ui.screens.market_screen


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import coil3.compose.AsyncImage
import com.developer.ev2z.ui.components.FloatingContactButton
import com.developer.ev2z.util.Analytics
import eva2z_connect_multiplatform.composeapp.generated.resources.Res
import eva2z_connect_multiplatform.composeapp.generated.resources.app_logo // apna drawable
import eva2z_connect_multiplatform.composeapp.generated.resources.banner_img_1
import eva2z_connect_multiplatform.composeapp.generated.resources.banner_img_2
import eva2z_connect_multiplatform.composeapp.generated.resources.shop_banner
import org.jetbrains.compose.resources.painterResource

// ------------ DATA MODEL ------------
@kotlinx.serialization.Serializable
data class ShopProduct(
    val id: Int,
    val title: String,
    val subtitle: String,        // device_type
    val price: Double,           // sale_price
    val rechargePrice: Double,   // recharge_price
    val promoAmount: Double,     // promo_amt
    val promoCode: String,       // promo_code
    val imageUrl: String,
    val description: String,
    val features: List<String>,  // features1–features7 merged
    val deviceType: String,      // "none" / "available"
    val rating: Double = 4.8,
    val ratingCount: Int = 50,
    val isFavorite: Boolean = false
)

// ------------ ROOT SCREEN ------------

@Composable
fun ShopScreen(
    products: List<ShopProduct>,
    cartCount: Int,
    onBackClick: () -> Unit = {},
    onToggleFavorite: (ShopProduct) -> Unit = {},
    onPlaceOrder: (ShopProduct) -> Unit = {},
    onCartClick: () -> Unit = {}
) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF6F6F6))
        ) {
            ShopTopBar()
            Spacer(Modifier.height(12.dp))
            ShopBanner()
            Spacer(Modifier.height(16.dp))

            Text(
                text = "Location Tracker Device",
                modifier = Modifier.padding(horizontal = 16.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(
                        bottom = 100.dp   // 👈 enough space for cart bar + comfort scroll
                    )
                ) {
                    items(products) { product ->
                        ProductCard(
                            product = product,
                            onToggleFavorite = { onToggleFavorite(product) },
                            onPlaceOrder = { onPlaceOrder(product) }
                        )
                    }
                }

                FloatingContactButton(
                    phoneNumber = "919755273822",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 20.dp)
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)   // ALWAYS stay at bottom
                ) {
                    CartBottomBar(
                        count = cartCount,
                        onClick = onCartClick
                    )
                }
            }
        }

}

// ------------ TOP BAR ------------

@Composable
private fun ShopTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App icon circle
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFF2ECC71)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "E",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        Spacer(Modifier.width(8.dp))

        Column {
            Text(
                "EVA2Z Connect IoT Devices",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("★", fontSize = 14.sp, color = Color(0xFFFFC107))
                Spacer(Modifier.width(4.dp))
                Text("4.8", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

// ------------ BANNER ------------

//@Composable
//private fun ShopBanner() {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 16.dp),
//        shape = RoundedCornerShape(18.dp),
//        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
//        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
//    ) {
//        Box(
//            modifier = Modifier
//                .height(140.dp)
//                .fillMaxWidth()
//                .background(
//                    brush = Brush.linearGradient(
//                        listOf(
//                            Color(0xFFAAFFB9),
//                            Color(0xFF59E37C)
//                        )
//                    )
//                )
//        )
//    }
//}

@Composable
private fun ShopBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Image(
            painter = painterResource(Res.drawable.banner_img_2),
            contentDescription = "Shop Banner",
            modifier = Modifier
                .height(160.dp)
                .fillMaxWidth(),
            contentScale = ContentScale.Crop   // 👈 image properly fill ho jayegi
        )
    }
}

// ------------ PRODUCT CARD ------------

@Composable
private fun ProductCard(
    product: ShopProduct,
    onToggleFavorite: () -> Unit,
    onPlaceOrder: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        onClick = {onPlaceOrder()}
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Favourite icon column
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

            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.title,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(50)),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(Res.drawable.app_logo),
                error = painterResource(Res.drawable.app_logo)
            )
            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    product.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))

                Text(
                    product.price.toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("★★★★★", fontSize = 10.sp, color = Color(0xFFFFC107))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "(${product.ratingCount})",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
//            Button(
//                onClick = {
//                    println("🟢 Button clicked for: ${product.title}")
//                    onPlaceOrder()
//                },
//                shape = RoundedCornerShape(30.dp),
//                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34A853))
//            ) {
//                Text("View Details", fontSize = 12.sp)
//            }
        }
    }
}

// ------------ BOTTOM CART BAR ------------
@Composable
private fun CartBottomBar(
    count: Int,
    onClick: () -> Unit
) {


        Box(
            modifier = Modifier
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(30.dp),
                modifier = Modifier
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34A853))
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text("Go To Cart", fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        count.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF34A853)
                    )
                }
            }
        }
    }


@Composable
fun ShopScreenHost() {
    val viewModel = ShopViewModelProvider.viewModel   // ✅ SINGLE SOURCE
    val state by viewModel.state.collectAsState()
    val navigator = LocalNavigator.current

    // ✅ Debug: Check if navigator is null
//    LaunchedEffect(Unit) {
//        println("🔍 Navigator is: ${if (navigator == null) "NULL ❌" else "Available ✅"}")
//        viewModel.loadData()
//    }
    LaunchedEffect(Unit) {
        Analytics.logEvent(
            name = "screen_view",
            params = mapOf(
                "screen_name" to "shop_screen",
                "screen_class" to "ShopScreen",
                "item_category" to "iot_devices",
                "source" to "home_or_menu" // agar pata ho
            )
        )

        viewModel.loadData()
    }
    if (state.loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LaunchedEffect(state.products) {
        if (state.products.isNotEmpty()) {
            Analytics.logEvent(
                name = "view_item_list",
                params = mapOf(
                    "item_list_id" to "shop_device_list",
                    "item_list_name" to "Location Tracker Device",
                    "currency" to "INR",
                    "items" to state.products.mapIndexed { index, p ->
                        mapOf(
                            "item_id" to p.id.toString(),
                            "item_name" to (p.name ?: ""),
                            "item_brand" to "EVA2Z",
                            "item_category" to "iot_devices",
                            "item_variant" to (p.device_type ?: ""),
                            "price" to (p.sale_price?.toDoubleOrNull() ?: 0.0),
                            "index" to index
                        ).toString()
                    }
                )
            )
        }
    }
    ShopScreen(
        products = state.products.map { dto ->
            ShopProduct(
                id = dto.id,
                title = dto.name ?: "",
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
                rating = 4.8,
                ratingCount = 50,
                isFavorite = viewModel.isFavorite(dto.id)
            )
        },
        cartCount = state.cartCount,
        onToggleFavorite = { p ->
            viewModel.toggleWishlist(p.id.toInt())

            Analytics.logEvent(
                name = "add_to_wishlist",
                params = mapOf(
                    "currency" to "INR",
                    "items" to listOf(
                        mapOf(
                            "item_id" to p.id.toString(),
                            "item_name" to p.title,
                            "item_brand" to "EVA2Z",
                            "item_category" to "iot_devices",
                            "item_variant" to p.subtitle,
                            "price" to p.price
                        )
                    )
                )
            )
        },
        onCartClick = {
            println("🛒 Cart clicked")
            navigator?.push(CartScreen())
        },
        onPlaceOrder = { product ->
            println("🔘 View Details clicked for: ${product.title}")
            println("🧭 Navigator: ${navigator != null}")

            // ✅ Navigation
            navigator?.push(
                ProductDetailsScreen(
                    product = product,
                    onToggleFavorite = { p ->
                        viewModel.toggleWishlist(p.id)

                        Analytics.logEvent(
                            name = "add_to_wishlist",
                            params = mapOf(
                                "currency" to "INR",
                                "items" to listOf(
                                    mapOf(
                                        "item_id" to p.id.toString(),
                                        "item_name" to p.title,
                                        "item_brand" to "EVA2Z",
                                        "item_category" to "iot_devices",
                                        "item_variant" to p.subtitle,
                                        "price" to p.price
                                    )
                                )
                            )
                        )
                    }
                )
            )
        }
    )
}