package com.developer.ev2z.util

import kotlinx.cinterop.*
import platform.CoreGraphics.CGFloat
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.*
import platform.Photos.*
import platform.UIKit.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
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

// ================= DOCUMENT SCAN (orientation baked in, resolution capped) =================
@OptIn(ExperimentalForeignApi::class)
actual fun prepareDocumentImage(
    imageBytes: ByteArray,
    maxSizeKB: Int,
    maxDimension: Int
): ByteArray = autoreleasepool {

    val image = imageBytes.toUIImage() ?: return@autoreleasepool imageBytes

    val width  = image.size.useContents { width }
    val height = image.size.useContents { height }
    if (width <= 0.0 || height <= 0.0) return@autoreleasepool imageBytes

    val longEdge = maxOf(width, height)
    val scale    = if (longEdge > maxDimension.toDouble()) maxDimension.toDouble() / longEdge else 1.0
    val targetW  = width * scale
    val targetH  = height * scale

    // Redrawing bakes the EXIF orientation into the pixels. UIImage keeps it as an
    // imageOrientation flag, so a straight JPEG re-encode can hand the server a
    // sideways document — which its OCR rejects.
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(targetW, targetH), false, 1.0)
    image.drawInRect(CGRectMake(0.0, 0.0, targetW, targetH))
    val upright = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()

    val source = upright ?: image

    var compression: CGFloat = 0.9
    var outputData: NSData? = UIImageJPEGRepresentation(source, compression)

    while (
        outputData != null &&
        (outputData?.length?.toInt() ?: 0) / 1024 > maxSizeKB &&
        compression > 0.6
    ) {
        compression -= 0.05
        outputData = UIImageJPEGRepresentation(source, compression)
    }

    outputData?.toByteArraySafe() ?: imageBytes
}

// ================= QR — DOWNLOAD (save directly to Photos gallery) =================
@OptIn(ExperimentalForeignApi::class)
actual fun saveQrImageToGallery(
    bytes: ByteArray,
    fileName: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val image = bytes.toUIImage()
    if (image == null) { onError("Failed to decode image"); return }

    val capturedSuccess = onSuccess
    val capturedError   = onError

    dispatch_async(dispatch_get_main_queue()) {
        val status = PHPhotoLibrary.authorizationStatusForAccessLevel(PHAccessLevelAddOnly)
        when (status) {
            PHAuthorizationStatusAuthorized, PHAuthorizationStatusLimited -> {
                doSaveToPhotos(image, capturedSuccess, capturedError)
            }
            PHAuthorizationStatusNotDetermined -> {
                PHPhotoLibrary.requestAuthorizationForAccessLevel(PHAccessLevelAddOnly) { granted ->
                    dispatch_async(dispatch_get_main_queue()) {
                        if (granted == PHAuthorizationStatusAuthorized || granted == PHAuthorizationStatusLimited) {
                            doSaveToPhotos(image, capturedSuccess, capturedError)
                        } else {
                            capturedError("Photo library access denied. Please allow in Settings.")
                        }
                    }
                }
            }
            else -> capturedError("Photo library access denied. Please allow in Settings.")
        }
    }
}

private fun doSaveToPhotos(image: UIImage, onSuccess: () -> Unit, onError: (String) -> Unit) {
    PHPhotoLibrary.sharedPhotoLibrary().performChanges({
        PHAssetChangeRequest.creationRequestForAssetFromImage(image)
    }, completionHandler = { success: Boolean, error: NSError? ->
        dispatch_async(dispatch_get_main_queue()) {
            if (success) onSuccess()
            else onError(error?.localizedDescription ?: "Failed to save to Photos")
        }
    })
}

// ================= QR — SHARE =================
// Traverse the VC stack to find the topmost presented VC so UIActivityViewController
// can be presented even when a Compose Dialog is already open on screen.
@OptIn(ExperimentalForeignApi::class)
actual fun shareQrImage(bytes: ByteArray, fileName: String) {
    val image = bytes.toUIImage() ?: return
    val capturedImage = image

    dispatch_async(dispatch_get_main_queue()) {
        val topVC = topViewController() ?: return@dispatch_async
        val activityVC = UIActivityViewController(
            activityItems        = listOf(capturedImage),
            applicationActivities = null
        )
        activityVC.popoverPresentationController?.sourceView = topVC.view
        activityVC.popoverPresentationController?.sourceRect = CGRectMake(
            topVC.view.bounds.useContents { origin.x + size.width / 2 },
            topVC.view.bounds.useContents { origin.y + size.height / 2 },
            0.0, 0.0
        )
        topVC.presentViewController(activityVC, animated = true, completion = null)
    }
}

// ================= HELPERS =================

// Walk the presentedViewController chain to reach the topmost VC.
// This ensures UIActivityViewController can always be presented, even when
// a Compose Dialog is already showing as a presented modal.
@Suppress("DEPRECATION")
private fun topViewController(
    base: UIViewController? = UIApplication.sharedApplication.keyWindow?.rootViewController
): UIViewController? {
    val presented = base?.presentedViewController
    return if (presented != null) topViewController(presented) else base
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toUIImage(): UIImage? =
    usePinned { pinned ->
        val data = NSData.create(
            bytes  = pinned.addressOf(0),
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