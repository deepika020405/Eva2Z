@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.developer.ev2z.util.crypto

import kotlinx.cinterop.*
import platform.CoreCrypto.*

actual fun sha256(bytes: ByteArray): String {

    // Output buffer (UByteArray)
    val hash = UByteArray(CC_SHA256_DIGEST_LENGTH)

    // Pin input + output safely
    bytes.usePinned { input ->
        hash.usePinned { output ->
            CC_SHA256(
                input.addressOf(0),          // const void*
                bytes.size.toUInt(),         // CC_LONG (UInt)
                output.addressOf(0)          // unsigned char*
            )
        }
    }

    // Hex encode (KMP safe)
    val hexChars = "0123456789abcdef"
    val result = StringBuilder(hash.size * 2)

    for (b in hash) {
        val i = b.toInt()
        result.append(hexChars[(i shr 4) and 0x0f])
        result.append(hexChars[i and 0x0f])
    }

    return result.toString()
}

actual fun randomHex(length: Int): String {
    val chars = "0123456789abcdef"
    return buildString {
        repeat(length) {
            append(chars.random())
        }
    }
}