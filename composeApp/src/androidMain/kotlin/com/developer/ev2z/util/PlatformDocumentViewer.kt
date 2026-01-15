package com.developer.ev2z.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import java.io.File

@Composable
actual fun PlatformDocumentViewer(
    bytes: ByteArray,
    modifier: Modifier
) {
    val bitmap = remember(bytes) {
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        modifier = modifier
    )
}

actual fun platformDownloadFile(
    context: Any?,
    bytes: ByteArray,
    fileName: String
) {
    val ctx = context as Context

    val (extension, mimeType) = detectExtension(bytes)

    val finalName =
        if (fileName.lowercase().endsWith(extension))
            fileName
        else
            fileName.substringBeforeLast(".") + extension

    val resolver = ctx.contentResolver

    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, finalName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS
            )
        }
    }

    val uri = resolver.insert(
        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
        values
    ) ?: return

    resolver.openOutputStream(uri)?.use {
        it.write(bytes)
    }

    showDownloadNotification(ctx, finalName)
}


private fun showDownloadNotification(
    context: Context,
    fileName: String
) {
    val manager =
        context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

    val channelId = "downloads"

    if (Build.VERSION.SDK_INT >= 26) {
        manager.createNotificationChannel(
            NotificationChannel(
                channelId,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setContentTitle(fileName)
        .setContentText("Download completed")
        .build()

    manager.notify(System.currentTimeMillis().toInt(), notification)
}

private fun detectExtension(bytes: ByteArray): Pair<String, String> {
    return when {
        // JPEG
        bytes.size >= 2 &&
                bytes[0] == 0xFF.toByte() &&
                bytes[1] == 0xD8.toByte() ->
            ".jpg" to "image/jpeg"

        // PNG
        bytes.size >= 4 &&
                bytes[0] == 0x89.toByte() &&
                bytes[1] == 0x50.toByte() ->
            ".png" to "image/png"

        // PDF
        bytes.size >= 4 &&
                bytes[0] == 0x25.toByte() &&
                bytes[1] == 0x50.toByte() &&
                bytes[2] == 0x44.toByte() &&
                bytes[3] == 0x46.toByte() ->
            ".pdf" to "application/pdf"

        else ->
            ".bin" to "application/octet-stream"
    }
}