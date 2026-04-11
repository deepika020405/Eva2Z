package com.developer.ev2z.ui.screens.Profile

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun platformOpenUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url)
    if (nsUrl != null) {
        UIApplication.sharedApplication.openURL(nsUrl, emptyMap<Any?, Any>(), null)
    }
}

actual fun getPlatformStoreUrl(): String {
    // Opens directly in App Store app on the Eva2z Connect page
    return "itms-apps://itunes.apple.com/app/id6760705704"
}