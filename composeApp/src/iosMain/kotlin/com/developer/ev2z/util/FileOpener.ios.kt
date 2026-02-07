package com.developer.ev2z.util

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openPdfFile(filePath: String) {
    val url = NSURL.fileURLWithPath(filePath)
    UIApplication.sharedApplication.openURL(url)
}