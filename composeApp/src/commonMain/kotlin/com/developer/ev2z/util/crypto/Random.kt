// CryptoUtils.kt (commonMain)
package com.developer.ev2z.util.crypto

expect fun sha256(bytes: ByteArray): String

expect fun randomHex(length: Int): String