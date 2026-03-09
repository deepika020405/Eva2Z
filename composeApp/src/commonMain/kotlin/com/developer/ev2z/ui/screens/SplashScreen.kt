package com.developer.ev2z.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.data.local.DeepLinkManager
import com.developer.ev2z.data.local.StorageKeys
import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.data.local.UserPrefs
import com.developer.ev2z.fetchReferrerIfNeeded
import eva2z_connect_multiplatform.composeapp.generated.resources.Res
import eva2z_connect_multiplatform.composeapp.generated.resources.app_logo
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SplashScreen(
    storage: StorageManager,
    userPrefs: UserPrefs,
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToShop: () -> Unit,
    onNavigateToProduct: (String) -> Unit,
) {
    LaunchedEffect(Unit) {
        delay(2000)

        // ✅ Main thread par callback ensure karo
        withContext(Dispatchers.Main) {
            fetchReferrerIfNeeded {
                val token = userPrefs.getToken()
                println("🔍 SplashScreen token: '$token'")

                val pendingLink = DeepLinkManager.consumePendingDeepLink()
                println("🔍 pendingLink: '$pendingLink'")

                when {
                    token.isEmpty() -> {
                        println("🔍 → Login")
                        onNavigateToLogin()
                    }
                    pendingLink?.startsWith("/shop/product/") == true -> {
                        val id = pendingLink.removePrefix("/shop/product/")
                        onNavigateToProduct(id)
                    }
                    pendingLink?.startsWith("/shop") == true -> onNavigateToShop()
                    else -> {
                        println("🔍 → Home")
                        onNavigateToHome()
                    }
                }
            }
        }
    }

    // ✅ Your existing UI — unchanged
    val infiniteTransition = rememberInfiniteTransition()

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFFFF)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(scale)
                    .rotate(rotation),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.app_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(140.dp)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "EVA2Z",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black.copy(alpha = alpha)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Connect",
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black.copy(alpha = alpha * 0.9f),
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(48.dp))
            CircularProgressIndicator(
                color = Color.Black,
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading...",
                fontSize = 14.sp,
                color = Color.Black.copy(alpha = 0.8f),
                letterSpacing = 1.sp
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {}
    }
}