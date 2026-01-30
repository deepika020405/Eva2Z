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