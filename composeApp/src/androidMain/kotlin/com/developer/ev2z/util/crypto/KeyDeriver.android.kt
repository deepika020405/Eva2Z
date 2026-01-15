package com.developer.ev2z.util.crypto

import java.security.MessageDigest

actual object KeyDeriver {

    actual fun deriveAesKeyFromVaultToken(vaultToken: String): ByteArray {
        return MessageDigest
            .getInstance("SHA-256")
            .digest(vaultToken.toByteArray())
            .copyOf(16) // AES-128
    }
}