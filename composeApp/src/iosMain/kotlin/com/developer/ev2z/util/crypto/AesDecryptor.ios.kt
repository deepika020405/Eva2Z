package com.developer.ev2z.util.crypto

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.Security.*

actual object AesDecryptor {

    @OptIn(ExperimentalForeignApi::class)
    actual fun decrypt(
        encrypted: ByteArray,
        secretKey: ByteArray,
        iv: ByteArray
    ): ByteArray {

        val encryptedData = encrypted.toNSData()
        val keyData = secretKey.toNSData()
        val ivData = iv.toNSData()

        val decryptedData = decryptAES(
            encryptedData,
            keyData,
            ivData
        ) ?: error("AES decryption failed")

        return decryptedData.toByteArray()
    }
}