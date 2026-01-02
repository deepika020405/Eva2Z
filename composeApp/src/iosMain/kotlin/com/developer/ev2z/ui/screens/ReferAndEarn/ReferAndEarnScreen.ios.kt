package com.developer.ev2z.ui.screens.ReferAndEarn

import platform.Foundation.NSCharacterSet
import platform.Foundation.NSURL
import platform.Foundation.URLQueryAllowedCharacterSet
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.Foundation.NSString
import platform.Foundation.stringByAddingPercentEncodingWithAllowedCharacters
import platform.MessageUI.MFMessageComposeViewController
import platform.MessageUI.MFMessageComposeViewControllerDelegateProtocol
import platform.darwin.NSObject

//actual fun shareViaWhatsApp(context: Any?, message: String) {
//    try {
//        // Encode message for URL
//        val encodedMessage = message.addingPercentEncodingWithAllowedCharacters(
//            NSCharacterSet.URLQueryAllowedCharacterSet
//        ) ?: message
//
//        // Try to open WhatsApp app
//        val whatsappURL = "whatsapp://send?text=$encodedMessage"
//        val url = NSURL(string = whatsappURL)
//
//        if (UIApplication.sharedApplication.canOpenURL(url)) {
//            UIApplication.sharedApplication.openURL(url)
//            println("✅ Opening WhatsApp")
//        } else {
//            // WhatsApp not installed, fallback to generic share
//            shareViaOther(context, message)
//            println("⚠️ WhatsApp not installed, using generic share")
//        }
//    } catch (e: Exception) {
//        println("❌ WhatsApp share failed: ${e.message}")
//        shareViaOther(context, message)
//    }
//}
actual fun shareViaWhatsApp(context: Any?, message: String) {
    try {
        val encodedMessage = encodeUrl(message)

        val whatsappURL = "whatsapp://send?text=$encodedMessage"
        val url = NSURL(string = whatsappURL)

        if (UIApplication.sharedApplication.canOpenURL(url)) {
            UIApplication.sharedApplication.openURL(url)
            println("✅ Opening WhatsApp")
        } else {
            shareViaOther(context, message)
            println("⚠️ WhatsApp not installed, using generic share")
        }
    } catch (e: Exception) {
        println("❌ WhatsApp share failed: ${e.message}")
        shareViaOther(context, message)
    }
}

actual fun shareViaOther(context: Any?, message: String) {
    try {
        val activityViewController = UIActivityViewController(
            activityItems = listOf(message),
            applicationActivities = null
        )

        val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController

        rootVC?.presentViewController(
            activityViewController,
            animated = true,
            completion = null
        )

        println("✅ Opening iOS share sheet")
    } catch (e: Exception) {
        println("❌ Share failed: ${e.message}")
    }
}

actual fun openContactPicker(context: Any?, message: String) {
    try {
        val encodedMessage = encodeUrl(message)
        val smsURL = "sms:&body=$encodedMessage"
        val url = NSURL(string = smsURL)

        if (UIApplication.sharedApplication.canOpenURL(url)) {
            UIApplication.sharedApplication.openURL(url)
            println("✅ Opening Messages app")
        } else {
            shareViaOther(context, message)
            println("⚠️ Cannot open Messages, using generic share")
        }
    } catch (e: Exception) {
        println("❌ Contact picker failed: ${e.message}")
        shareViaOther(context, message)
    }
}

//actual fun shareViaOther(context: Any?, message: String) {
//    try {
//        val activityViewController = UIActivityViewController(
//            activityItems = listOf(message),
//            applicationActivities = null
//        )
//
//        // Get the root view controller
//        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
//
//        rootViewController?.presentViewController(
//            activityViewController,
//            animated = true,
//            completion = null
//        )
//
//        println("✅ Opening iOS share sheet")
//    } catch (e: Exception) {
//        println("❌ Share failed: ${e.message}")
//    }
//}
//
//// ✅ NEW: Contact Picker for iOS
//actual fun openContactPicker(context: Any?, message: String) {
//    try {
//        // For iOS, we'll use the Messages app with pre-filled text
//        // The user can then select contacts from within the Messages app
//
//        val smsURL = "sms:&body=${message.addingPercentEncodingWithAllowedCharacters(
//            NSCharacterSet.URLQueryAllowedCharacterSet
//        )}"
//
//        val url = NSURL(string = smsURL)
//
//        if (UIApplication.sharedApplication.canOpenURL(url)) {
//            UIApplication.sharedApplication.openURL(url)
//            println("✅ Opening Messages app")
//        } else {
//            // Fallback to generic share
//            shareViaOther(context, message)
//            println("⚠️ Cannot open Messages, using generic share")
//        }
//    } catch (e: Exception) {
//        println("❌ Contact picker failed: ${e.message}")
//        // Fallback to generic share
//        shareViaOther(context, message)
//    }
//}
private fun encodeUrl(value: String): String {
    return (value as NSString)
        .stringByAddingPercentEncodingWithAllowedCharacters(
            NSCharacterSet.URLQueryAllowedCharacterSet
        ) ?: value
}
