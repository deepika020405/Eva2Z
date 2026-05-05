package com.developer.ev2z.util

import kotlinx.cinterop.*
import platform.CoreGraphics.CGFloat
import platform.Foundation.*
import platform.UIKit.*
import platform.posix.memcpy

// ================= PROFILE IMAGE =================
actual fun fixAndCompressImage(
    imageBytes: ByteArray,
    maxSizeKB: Int
): ByteArray = autoreleasepool {

    val image = imageBytes.toUIImage() ?: return@autoreleasepool imageBytes

    var compression: CGFloat = 0.8
    var outputData: NSData?

    do {
        outputData = UIImageJPEGRepresentation(image, compression)
        compression -= 0.05
    } while (
        outputData != null &&
        (outputData?.length?.toInt() ?: 0) / 1024 > maxSizeKB &&
        compression > 0.4
    )

    outputData?.toByteArraySafe() ?: imageBytes
}

// ================= STORY IMAGE (NO ROTATION) =================
actual fun compressStoryImage(
    imageBytes: ByteArray,
    maxSizeKB: Int
): ByteArray = autoreleasepool {

    val image = imageBytes.toUIImage() ?: return@autoreleasepool imageBytes

    var compression: CGFloat = 0.85
    var outputData: NSData?

    do {
        outputData = UIImageJPEGRepresentation(image, compression)
        compression -= 0.05
    } while (
        outputData != null &&
        (outputData?.length?.toInt() ?: 0) / 1024 > maxSizeKB &&
        compression > 0.45
    )

    outputData?.toByteArraySafe() ?: imageBytes
}

@OptIn(ExperimentalForeignApi::class)
actual fun saveQrImageToGallery(
    bytes: ByteArray,
    fileName: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) = autoreleasepool {
    val image = bytes.toUIImage()
    if (image == null) { onError("Failed to decode image"); return@autoreleasepool }
    // UIImageWriteToSavedPhotosAlbum requests NSPhotoLibraryAddUsageDescription permission on first use
    UIImageWriteToSavedPhotosAlbum(image, null, null, null)
    onSuccess()
}

@OptIn(ExperimentalForeignApi::class)
actual fun shareQrImage(bytes: ByteArray, fileName: String) = autoreleasepool {
    val image = bytes.toUIImage() ?: return@autoreleasepool
    val activityVC = UIActivityViewController(
        activityItems = listOf(image),
        applicationActivities = null
    )
    val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController
    rootVC?.presentViewController(activityVC, animated = true, completion = null)
}

// ================= SAFE HELPERS =================
@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toUIImage(): UIImage? =
    usePinned { pinned ->
        val data = NSData.create(
            bytes = pinned.addressOf(0),
            length = size.toULong()
        )
        UIImage(data = data)
    }

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArraySafe(): ByteArray {
    val array = ByteArray(length.toInt())
    memcpy(array.refTo(0), bytes, length)
    return array
}