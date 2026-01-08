package com.developer.ev2z.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

actual fun compressImageBytes(
    imageBytes: ByteArray,
    maxSizeKB: Int
): ByteArray {

    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        ?: return imageBytes

    var quality = 80
    var output: ByteArray

    do {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        output = stream.toByteArray()
        quality -= 5
    } while (output.size / 1024 > maxSizeKB && quality > 40)

    return output
}