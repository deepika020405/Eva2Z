package com.developer.ev2z.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun VideoPlayerView(
    url: String,
    modifier: Modifier = Modifier
)