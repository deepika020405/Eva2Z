package com.developer.ev2z.util.crypto

actual object KeyDeriver {

    actual fun deriveAesKeyFromVaultToken(vaultToken: String): ByteArray {
        // ❗ Backend confirm ke baad proper SHA256 iOS impl lagayenge
        return vaultToken.encodeToByteArray().copyOf(16)
    }
}