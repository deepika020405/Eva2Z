package com.developer.ev2z.util.crypto

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec

actual object AesDecryptor {

    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"

    actual fun decrypt(
        encrypted: ByteArray,
        secretKey: ByteArray,
        iv: ByteArray
    ): ByteArray {

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val keySpec = SecretKeySpec(secretKey, "AES")
        val ivSpec = IvParameterSpec(iv)

        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        return cipher.doFinal(encrypted)
    }
}