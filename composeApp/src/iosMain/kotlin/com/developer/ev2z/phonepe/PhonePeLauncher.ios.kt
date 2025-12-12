package com.developer.ev2z.phonepe

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openPhonePeCheckout(redirectUrl: String) {
    val url = NSURL(string = redirectUrl)
    url?.let { UIApplication.sharedApplication.openURL(it) }
}