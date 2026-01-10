package com.developer.ev2z.util

// 🔥 Common interface
expect fun fixAndCompressImage(
    imageBytes: ByteArray,
    maxSizeKB: Int = 500
): ByteArray


expect fun compressStoryImage(
    imageBytes: ByteArray,
    maxSizeKB: Int = 500
): ByteArray