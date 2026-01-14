package com.developer.ev2z.util.crypto

expect object AesDecryptor {

    fun decrypt(
        encrypted: ByteArray,
        secretKey: ByteArray,
        iv: ByteArray
    ): ByteArray
}