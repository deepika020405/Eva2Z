package com.developer.ev2z.util

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.developer.ev2z.appCtx
import java.io.ByteArrayOutputStream
import java.io.File

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

actual fun prepareDocumentImage(
    imageBytes: ByteArray,
    maxSizeKB: Int,
    maxDimension: Int
): ByteArray {
    val decoded = decodeDownscaled(imageBytes, maxDimension) ?: return imageBytes
    val upright = applyExifOrientation(decoded, imageBytes)

    var quality = 90
    var output: ByteArray
    do {
        val stream = ByteArrayOutputStream()
        upright.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        output = stream.toByteArray()
        quality -= 5
    } while (output.size / 1024 > maxSizeKB && quality > 60)

    return output
}

/** Decodes the image with its long edge at or below [maxDimension]. */
private fun decodeDownscaled(imageBytes: ByteArray, maxDimension: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, bounds)

    val longEdge = maxOf(bounds.outWidth, bounds.outHeight)
    if (longEdge <= 0) return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

    // inSampleSize only halves, so it lands within 2x of the target — the exact
    // fit comes from the scale below, working on an already-small bitmap.
    val options = BitmapFactory.Options().apply {
        var sample = 1
        while (longEdge / (sample * 2) >= maxDimension) sample *= 2
        inSampleSize = sample
    }
    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options) ?: return null

    val decodedLongEdge = maxOf(bitmap.width, bitmap.height)
    if (decodedLongEdge <= maxDimension) return bitmap

    val scale = maxDimension.toFloat() / decodedLongEdge
    return Bitmap.createScaledBitmap(
        bitmap,
        (bitmap.width * scale).toInt().coerceAtLeast(1),
        (bitmap.height * scale).toInt().coerceAtLeast(1),
        true
    )
}

/** Rotates/flips [bitmap] so the EXIF orientation of [imageBytes] is baked into the pixels. */
private fun applyExifOrientation(bitmap: Bitmap, imageBytes: ByteArray): Bitmap {
    val orientation = try {
        ExifInterface(imageBytes.inputStream())
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } catch (_: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }

    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90     -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180    -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270    -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL   -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE     -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_TRANSVERSE    -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
        else -> return bitmap
    }

    return try {
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } catch (_: OutOfMemoryError) {
        bitmap
    }
}

private fun toJpegBytes(bytes: ByteArray, quality: Int = 95): ByteArray {
    return try {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return bytes
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        stream.toByteArray()
    } catch (_: Exception) {
        bytes
    }
}

actual fun saveQrImageToGallery(
    bytes: ByteArray,
    fileName: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        val context = appCtx
        val jpegBytes = toJpegBytes(bytes)
        val jpegName  = fileName.replaceAfterLast('.', "jpg").let {
            if (it == fileName) "$fileName.jpg" else it
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, jpegName)
                put(MediaStore.Downloads.MIME_TYPE, "image/jpeg")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/EVA2Z")
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { it.write(jpegBytes) }
                onSuccess()
            } else {
                onError("Failed to create file in Downloads")
            }
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "EVA2Z")
            dir.mkdirs()
            File(dir, jpegName).writeBytes(jpegBytes)
            onSuccess()
        }
    } catch (e: Exception) {
        onError(e.message ?: "Save failed")
    }
}

actual fun shareQrImage(bytes: ByteArray, fileName: String) {
    try {
        val context  = appCtx
        val jpegBytes = toJpegBytes(bytes)
        val jpegName  = fileName.replaceAfterLast('.', "jpg").let {
            if (it == fileName) "$fileName.jpg" else it
        }
        val file = File(context.cacheDir, jpegName)
        file.writeBytes(jpegBytes)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "EVA2Z Vehicle TAG QR Code")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share QR Code").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    } catch (e: Exception) {
        println("❌ shareQrImage: ${e.message}")
    }
}