package com.developer.ev2z.util

import com.developer.ev2z.util.crypto.toByteArray
import platform.Foundation.*

actual object Base64Util {
    actual fun decode(input: ByteArray): ByteArray? {
        val str = input.decodeToString()
        val data = NSData.create(base64EncodedString = str, options = 0u)
        return data?.toByteArray()
    }
}