package com.developer.ev2z.ui.screens.community

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import coil3.compose.AsyncImage

@Composable
actual fun NetworkImage(
    url: String,
    modifier: Modifier
) {
    AsyncImage(
        model = url,
        contentDescription = null,
        modifier = modifier
    )
}