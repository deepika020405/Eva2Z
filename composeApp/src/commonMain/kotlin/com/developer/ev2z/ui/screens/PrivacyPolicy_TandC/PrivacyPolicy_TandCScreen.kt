package com.developer.ev2z.ui.screens.PrivacyPolicy_TandC

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.navigation.PlatformBackHandler

enum class WebViewType {
    PRIVACY_POLICY,
    TERMS_CONDITIONS,
    REFUND_POLICY,
    SHIPPING_POLICY,
    FACEBOOK,
    INSTAGRAM,
    LINKEDIN,
    YOUTUBE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicy_TandCScreen(
    type: WebViewType,
    onBackPressed: () -> Unit
) {
    val url = when (type) {
        WebViewType.PRIVACY_POLICY -> "https://www.eva2z.com/privacy-policy"
        WebViewType.TERMS_CONDITIONS -> "https://eva2z.com/term-condition/"
        WebViewType.REFUND_POLICY -> "https://www.eva2z.com/refund-cancellation-policy"
        WebViewType.SHIPPING_POLICY -> "https://www.eva2z.com/shipping-installation-policy"

        WebViewType.FACEBOOK -> "https://www.facebook.com/eva2zconnect"
        WebViewType.INSTAGRAM -> "https://www.instagram.com/eva2zconnect"
        WebViewType.LINKEDIN -> "https://www.linkedin.com/company/eva2zconnect/"
        WebViewType.YOUTUBE -> "https://www.youtube.com/@eva2zconnect"
    }

    val title = when (type) {
        WebViewType.PRIVACY_POLICY -> "Privacy Policy"
        WebViewType.TERMS_CONDITIONS -> "Terms & Conditions"
        WebViewType.REFUND_POLICY -> "Refund & Cancellation Policy"
        WebViewType.SHIPPING_POLICY -> "Shipping & Installation Policy"

        WebViewType.FACEBOOK -> "Facebook"
        WebViewType.INSTAGRAM -> "Instagram"
        WebViewType.LINKEDIN -> "LinkedIn"
        WebViewType.YOUTUBE -> "YouTube"
    }

    PlatformBackHandler(enabled = true) {
        onBackPressed()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            WebViewComponent(url = url)
        }
    }
}

@Composable
expect fun WebViewComponent(url: String)