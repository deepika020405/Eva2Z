package com.developer.ev2z.util

import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun VideoPlayerView(url: String, modifier: Modifier) {
    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                setVideoURI(android.net.Uri.parse(url))
                val mc = MediaController(ctx)
                mc.setAnchorView(this)
                setMediaController(mc)
                requestFocus()
                start()
            }
        },
        modifier = modifier
    )
}