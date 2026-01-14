package com.developer.ev2z.util.crypto

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
fun ByteArray.toNSData(): NSData =
    this.usePinned {
        NSData.create(
            bytes = it.addressOf(0),
            length = this.size.toULong()
        )
    }

@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    val byteArray = ByteArray(length)

    memScoped {
        memcpy(
            byteArray.refTo(0),
            this@toByteArray.bytes,
            this@toByteArray.length
        )
    }
    return byteArray
}