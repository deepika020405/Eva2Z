package com.developer.ev2z.ui.screens.social_media

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return

    val application = UIApplication.sharedApplication
    if (application.canOpenURL(nsUrl)) {
        application.openURL(nsUrl)
    }
}