package com.developer.ev2z.util

// 🔥 CMP friendly
expect fun compressImageBytes(
    imageBytes: ByteArray,
    maxSizeKB: Int = 500
): ByteArray