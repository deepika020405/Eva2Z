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
import eva2z_connect_multiplatform.composeapp.generated.resources.Res
import eva2z_connect_multiplatform.composeapp.generated.resources.app_logo // apna drawable
import org.jetbrains.compose.resources.painterResource

// ------------ DATA MODEL ------------
@kotlinx.serialization.Serializable
data class ShopProduct(
    val id: String,
    val title: String,
    val subtitle: String,
    val priceText: String,
    val rating: Double,
    val ratingCount: Int,
    val isFavorite: Boolean = false,
    val imageUrl: String = ""   // ADD THIS
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
            ){

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(products) { product ->
                        ProductCard(
                            product = product,
                            onToggleFavorite = { onToggleFavorite(product) },
                            onPlaceOrder = { onPlaceOrder(product) }
                        )
                    }
                }

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
        Box(
            modifier = Modifier
                .height(140.dp)
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0xFFAAFFB9),
                            Color(0xFF59E37C)
                        )
                    )
                )
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
        colors = CardDefaults.cardColors(containerColor = Color.White)
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
//                Text(
//                    product.subtitle,
//                    fontSize = 12.sp,
//                    color = Color.Gray
//                )

                Spacer(Modifier.height(4.dp))

                Text(
                    product.priceText,
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
            Button(
                onClick = {
                    println("🟢 Button clicked for: ${product.title}")
                    onPlaceOrder()
                },
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34A853))
            ) {
                Text("View Details", fontSize = 12.sp)
            }
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




//@Composable
//fun ShopScreenHost(viewModel: ShopViewModel) {
//
//    val state by viewModel.state.collectAsState()
//
//    LaunchedEffect(Unit) { viewModel.loadData() }
//
////    LaunchedEffect(state.cart) {
////        viewModel.refreshCart()
////    }
//
//    if (state.loading) {
//        Box(
//            modifier = Modifier
//                .fillMaxSize(),
//            contentAlignment = Alignment.Center
//        ) {
//            CircularProgressIndicator()
//        }
//        return
//    }
//
//    ShopScreen(
//        products = state.products.map { dto ->
//            ShopProduct(
//                id = dto.id.toString(),
//                title = dto.name,
//                subtitle = dto.device_type ?: "",
//                priceText = "Rs. ${dto.sale_price}",
//                rating = 4.8,
//                ratingCount = 50,
//                isFavorite = viewModel.isFavorite(dto.id),
//                imageUrl = dto.image ?: ""
//            )
//        },
//        cartCount = state.cartCount,      // <-- use real count
//        onToggleFavorite = { p ->
//            viewModel.toggleWishlist(p.id.toInt())
//        },
//        onCartClick = {
//            // navigate to cart screen
//        },
//        onPlaceOrder = { ProductDetailsScreen(product = products) }
//    )
//}



@Composable
fun ShopScreenHost(viewModel: ShopViewModel) {
    val state by viewModel.state.collectAsState()
    val navigator = LocalNavigator.current

    // ✅ Debug: Check if navigator is null
    LaunchedEffect(Unit) {
        println("🔍 Navigator is: ${if (navigator == null) "NULL ❌" else "Available ✅"}")
        viewModel.loadData()
    }

    if (state.loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    ShopScreen(
        products = state.products.map { dto ->
            ShopProduct(
                id = dto.id.toString(),
                title = dto.name,
                subtitle = dto.device_type ?: "",
                priceText = "Rs. ${dto.sale_price}",
                rating = 4.8,
                ratingCount = 50,
                isFavorite = viewModel.isFavorite(dto.id),
                imageUrl = dto.image ?: ""
            )
        },
        cartCount = state.cartCount,
        onToggleFavorite = { p ->
            viewModel.toggleWishlist(p.id.toInt())
        },
        onCartClick = {
            println("🛒 Cart clicked")
        },
        onPlaceOrder = { product ->
            println("🔘 View Details clicked for: ${product.title}")
            println("🧭 Navigator: ${navigator != null}")

            // ✅ Navigation
            navigator?.push(ProductDetailsScreen(product = product, viewModel = viewModel))
        }
    )
}