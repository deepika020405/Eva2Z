package com.developer.ev2z.util

import io.ktor.client.request.invoke
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import platform.UIKit.*
import platform.CoreGraphics.CGFloat
import platform.Foundation.NSData
import platform.Foundation.create   // 👈 THIS IS REQUIRED
import platform.posix.memcpy

// ================= PROFILE IMAGE =================
actual fun fixAndCompressImage(
    imageBytes: ByteArray,
    maxSizeKB: Int
): ByteArray {

    val data = imageBytes.toNSData()
    val image = UIImage(data = data) ?: return imageBytes

    var compression: CGFloat = 0.8
    var outputData: NSData?

    do {
        outputData = UIImageJPEGRepresentation(image, compression)
        compression -= 0.05
    } while (
        outputData != null &&
        (outputData.length.toInt() / 1024) > maxSizeKB &&
        compression > 0.4
    )

    return outputData?.toByteArray() ?: imageBytes
}

// ================= STORY IMAGE (NO ROTATION) =================
actual fun compressStoryImage(
    imageBytes: ByteArray,
    maxSizeKB: Int
): ByteArray {

    val data = imageBytes.toNSData()
    val image = UIImage(data = data) ?: return imageBytes

    var compression: CGFloat = 0.85
    var outputData: NSData?

    do {
        outputData = UIImageJPEGRepresentation(image, compression)
        compression -= 0.05
    } while (
        outputData != null &&
        (outputData.length.toInt() / 1024) > maxSizeKB &&
        compression > 0.45
    )

    return outputData?.toByteArray() ?: imageBytes
}

// ================= HELPERS =================
@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData =
    NSData.create(
        bytes = this.refTo(0) as COpaquePointer?,
        length = this.size.toULong()
    )

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val array = ByteArray(this.length.toInt())
    memcpy(array.refTo(0), this.bytes, this.length)
    return array
}

