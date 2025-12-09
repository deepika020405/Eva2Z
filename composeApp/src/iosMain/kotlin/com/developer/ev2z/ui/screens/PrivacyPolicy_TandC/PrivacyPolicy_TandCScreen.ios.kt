package com.developer.ev2z.ui.screens.PrivacyPolicy_TandC

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKWebView
import platform.WebKit.WKNavigationDelegateProtocol
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun WebViewComponent(url: String) {
    var isLoading by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        UIKitView(
            factory = {
                WKWebView().apply {
                    val request = NSURLRequest(uRL = NSURL(string = url))
                    loadRequest(request)
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { webView ->
                // Update if needed
            }
        )

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = androidx.compose.ui.graphics.Color(0xFF4CAF50)
            )
        }

        // Set loading to false after a delay (simplified)
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000)
            isLoading = false
        }
    }
}