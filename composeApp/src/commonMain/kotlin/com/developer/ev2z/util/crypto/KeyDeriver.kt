package com.developer.ev2z.util.crypto

expect object KeyDeriver {
    fun deriveAesKeyFromVaultToken(vaultToken: String): ByteArray
}