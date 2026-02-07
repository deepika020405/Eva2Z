package com.developer.ev2z.util

import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

//actual fun openPdfFile(
//    filePath: String
//) {
//    val context = OpenUrlHolder.context
//    val file = File(filePath)
//
//    val uri = FileProvider.getUriForFile(
//        context,
//        "${context.packageName}.fileprovider",
//        file
//    )
//
//    val intent = Intent(Intent.ACTION_VIEW).apply {
//        setDataAndType(uri, "application/pdf")
//        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//    }
//
//    context.startActivity(intent)
//}