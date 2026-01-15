package com.developer.ev2z.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PlatformDocumentViewer(
    bytes: ByteArray,
    modifier: Modifier = Modifier
)

expect fun platformDownloadFile(
    context: Any?,
    bytes: ByteArray,
    fileName: String
)

