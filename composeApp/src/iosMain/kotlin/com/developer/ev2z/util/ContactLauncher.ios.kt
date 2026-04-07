//package com.developer.ev2z.util
//
//import platform.Foundation.NSURL
//import platform.UIKit.UIApplication
//import platform.Foundation.NSCharacterSet
//import platform.Foundation.URLQueryAllowedCharacterSet
//import platform.Foundation.stringByAddingPercentEncodingWithAllowedCharacters
//
//actual fun openCall(phoneNumber: String) {
//    val url = NSURL(string = "tel:$phoneNumber")
//
//    val app = UIApplication.sharedApplication
//    if (app.canOpenURL(url)) {
//        app.openURL(url)
//    }
//}
//
//actual fun openWhatsApp(phoneNumber: String, message: String) {
//
//    val encodedMessage = message
//        .stringByAddingPercentEncodingWithAllowedCharacters(
//            NSCharacterSet.URLQueryAllowedCharacterSet()
//        ) ?: return
//
//    // Prefer WhatsApp app directly
//    val appUrl = NSURL(string = "whatsapp://send?phone=$phoneNumber&text=$encodedMessage")
//
//    val application = UIApplication.sharedApplication
//
//    if (appUrl != null && application.canOpenURL(appUrl)) {
//        application.openURL(appUrl)
//    } else {
//        // fallback → WhatsApp web
//        val webUrl = NSURL(string = "https://wa.me/$phoneNumber?text=$encodedMessage")
//        if (webUrl != null) {
//            application.openURL(webUrl)
//        }
//    }
//}


package com.developer.ev2z.util

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openCall(phoneNumber: String) {
    val url = NSURL(string = "tel:$phoneNumber") ?: return
    UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any>(), null)
}

actual fun openWhatsApp(phoneNumber: String, message: String) {
    val encodedMessage =
        message.replace(" ", "%20").replace("\n", "%0A")

    // Try WhatsApp app first
    val appUrl = NSURL(string = "whatsapp://send?phone=$phoneNumber&text=$encodedMessage")
    val app = UIApplication.sharedApplication

    if (appUrl != null && app.canOpenURL(appUrl)) {
        app.openURL(appUrl, emptyMap<Any?, Any>(), null)
    } else {
        // Fallback to WhatsApp web
        val webUrl = NSURL(string = "https://wa.me/$phoneNumber?text=$encodedMessage") ?: return
        app.openURL(webUrl, emptyMap<Any?, Any>(), null)
    }
}