package com.developer.ev2z.util

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openCall(phoneNumber: String) {
    val url = NSURL(string = "tel://$phoneNumber")
    UIApplication.sharedApplication.openURL(url!!)
}

actual fun openWhatsApp(phoneNumber: String, message: String) {
    val encodedMessage =
        message.replace(" ", "%20").replace("\n", "%0A")

    val url = NSURL(
        string = "https://wa.me/$phoneNumber?text=$encodedMessage"
    )
    UIApplication.sharedApplication.openURL(url!!)
}