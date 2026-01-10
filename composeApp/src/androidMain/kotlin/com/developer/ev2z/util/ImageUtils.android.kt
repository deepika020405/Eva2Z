package com.developer.ev2z.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream

actual fun fixAndCompressImage(imageBytes: ByteArray, maxSizeKB: Int): ByteArray {
    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return imageBytes

    // ✅ Read EXIF orientation
    val exif = ExifInterface(imageBytes.inputStream())
    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        else -> { }
    }

    val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

    // ✅ Compress
    var quality = 80
    var output: ByteArray
    do {
        val stream = ByteArrayOutputStream()
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        output = stream.toByteArray()
        quality -= 5
    } while (output.size / 1024 > maxSizeKB && quality > 40)

    return output
}

actual fun compressStoryImage(
    imageBytes: ByteArray, maxSizeKB: Int
): ByteArray {

    // 1️⃣ Decode once
    val bitmap = BitmapFactory.decodeByteArray(
        imageBytes, 0, imageBytes.size
    ) ?: return imageBytes

    var quality = 85
    var output: ByteArray

    do {
        val stream = ByteArrayOutputStream()
        bitmap.compress(
            Bitmap.CompressFormat.JPEG,
            quality,
            stream
        )
        output = stream.toByteArray()
        quality -= 5
    } while (
        output.size / 1024 > maxSizeKB &&
        quality > 45
    )

    return output
}