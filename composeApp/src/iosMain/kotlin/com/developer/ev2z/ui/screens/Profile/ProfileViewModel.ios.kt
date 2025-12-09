package com.developer.ev2z.ui.screens.Profile

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun platformOpenUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url)
    if (nsUrl != null && UIApplication.sharedApplication.canOpenURL(nsUrl)) {
        UIApplication.sharedApplication.openURL(nsUrl)
    }
}