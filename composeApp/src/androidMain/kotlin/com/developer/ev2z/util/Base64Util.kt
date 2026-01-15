package com.developer.ev2z.util

actual object Base64Util {
    actual fun decode(input: ByteArray): ByteArray? =
        try {
            android.util.Base64.decode(input, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
}