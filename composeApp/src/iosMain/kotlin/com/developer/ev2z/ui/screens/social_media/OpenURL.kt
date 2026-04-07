package com.developer.ev2z.ui.screens.social_media

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(nsUrl, emptyMap<Any?, Any>()) { _ -> }
}