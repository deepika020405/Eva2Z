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

enum class WebViewType {
    PRIVACY_POLICY,
    TERMS_CONDITIONS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicy_TandCScreen(
    type: WebViewType,
    onBackPressed: () -> Unit
) {
    val url = when (type) {
        WebViewType.PRIVACY_POLICY -> "https://www.eva2z.com/privacy-policy"
        WebViewType.TERMS_CONDITIONS -> "https://www.eva2z.com/about"
    }

    val title = when (type) {
        WebViewType.PRIVACY_POLICY -> "Privacy Policy"
        WebViewType.TERMS_CONDITIONS -> "Terms & Conditions"
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