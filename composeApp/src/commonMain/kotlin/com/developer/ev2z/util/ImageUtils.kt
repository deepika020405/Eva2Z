package com.developer.ev2z.util

// 🔥 Common interface
expect fun fixAndCompressImage(
    imageBytes: ByteArray,
    maxSizeKB: Int = 500
): ByteArray


expect fun compressStoryImage(
    imageBytes: ByteArray,
    maxSizeKB: Int = 500
): ByteArray

/**
 * Prepares a photographed document for the vault upload.
 *
 * Unlike [compressStoryImage] this bakes the EXIF orientation into the pixels
 * (camera shots are landscape pixels + an orientation tag, and re-encoding drops
 * the tag — the server's OCR then sees a sideways document and rejects it with
 * `document_rejected / could_not_validate_try_again`), and it downscales the long
 * edge before the quality loop so small print stays legible at the target size
 * instead of being flattened by a low JPEG quality.
 */
expect fun prepareDocumentImage(
    imageBytes: ByteArray,
    maxSizeKB: Int = 800,
    maxDimension: Int = 2000
): ByteArray

expect fun saveQrImageToGallery(
    bytes: ByteArray,
    fileName: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
)

expect fun shareQrImage(bytes: ByteArray, fileName: String)