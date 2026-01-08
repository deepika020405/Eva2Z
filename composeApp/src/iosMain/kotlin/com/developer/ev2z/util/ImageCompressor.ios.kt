package com.developer.ev2z.util

import kotlinx.cinterop.*
import platform.CoreGraphics.CGFloat
import platform.Foundation.*
import platform.UIKit.*
import platform.posix.memcpy

actual fun compressImageBytes(
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

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData =
    NSData.create(bytes = this.refTo(0) as COpaquePointer?, length = size.toULong())

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val array = ByteArray(length.toInt())
    memcpy(array.refTo(0), bytes, length)
    return array
}