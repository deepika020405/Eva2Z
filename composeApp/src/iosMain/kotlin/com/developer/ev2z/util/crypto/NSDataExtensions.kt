package com.developer.ev2z.util.crypto

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create

@OptIn(ExperimentalForeignApi::class)
fun ByteArray.toNSData(): NSData =
    usePinned {
        NSData.create(
            bytes = it.addressOf(0),
            length = size.toULong()
        )
    }
