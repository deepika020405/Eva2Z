package com.developer.ev2z.util.crypto


import kotlinx.cinterop.*
import platform.CoreCrypto.CCCrypt
import platform.CoreCrypto.kCCAlgorithmAES
import platform.CoreCrypto.kCCDecrypt
import platform.CoreCrypto.kCCOptionPKCS7Padding
import platform.CoreCrypto.kCCSuccess
import platform.Foundation.*
import platform.Security.*
import platform.posix.size_tVar

@OptIn(ExperimentalForeignApi::class)
fun decryptAES(
    encrypted: NSData,
    key: NSData,
    iv: NSData
): NSData? {

    val outputLength = encrypted.length + 16u
    val output = NSMutableData.dataWithLength(outputLength) as? NSMutableData
        ?: return null
    memScoped {
        val moved = alloc<size_tVar>()

        val status = CCCrypt(
            kCCDecrypt,
            kCCAlgorithmAES,
            kCCOptionPKCS7Padding,
            key.bytes,
            key.length,
            iv.bytes,
            encrypted.bytes,
            encrypted.length,
            output.mutableBytes,
            outputLength,
            moved.ptr
        )

        if (status != kCCSuccess) return null

        output.setLength(moved.value) // ✅ FIX
    }

    return output
}