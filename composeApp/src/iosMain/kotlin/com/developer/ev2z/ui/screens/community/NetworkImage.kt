// NetworkImage.ios.kt
package com.developer.ev2z.ui.screens.community

import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.jetbrains.skia.Image

@Composable
actual fun NetworkImage(
    url: String,
    modifier: Modifier
) {
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(url) {
        try {
            val client = HttpClient()
            val response: HttpResponse = client.get(url)
            val bytes: ByteArray = response.readBytes()

            bitmap = Image.makeFromEncoded(bytes).toComposeImageBitmap()
        } catch (e: Exception) {
            println("❌ Image load failed: ${e.message}")
        }
    }

    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = null,
            modifier = modifier
        )
    }
}