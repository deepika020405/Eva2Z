package com.developer.ev2z.util

expect object Base64Util {
    fun decode(input: ByteArray): ByteArray?
}