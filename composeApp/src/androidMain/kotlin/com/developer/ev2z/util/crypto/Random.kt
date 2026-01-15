package com.developer.ev2z.util.crypto

import java.security.MessageDigest
import kotlin.random.Random

actual fun sha256(bytes: ByteArray): String {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)

    val hexChars = "0123456789abcdef"
    val result = StringBuilder(digest.size * 2)

    for (b in digest) {
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
            append(chars[Random.nextInt(chars.length)])
        }
    }
}