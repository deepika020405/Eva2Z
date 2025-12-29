package com.developer.ev2z.ui.screens.ReferAndEarn

import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

actual fun shareViaWhatsApp(context: Any?, message: String) {
    try {
        // Encode message for URL
        val encodedMessage = message.addingPercentEncodingWithAllowedCharacters(
            NSCharacterSet.URLQueryAllowedCharacterSet
        ) ?: message

        // Try to open WhatsApp app
        val whatsappURL = "whatsapp://send?text=$encodedMessage"
        val url = NSURL(string = whatsappURL)

        if (UIApplication.sharedApplication.canOpenURL(url)) {
            UIApplication.sharedApplication.openURL(url)
            println("✅ Opening WhatsApp")
        } else {
            // WhatsApp not installed, fallback to generic share
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

        // Get the root view controller
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController

        rootViewController?.presentViewController(
            activityViewController,
            animated = true,
            completion = null
        )

        println("✅ Opening iOS share sheet")
    } catch (e: Exception) {
        println("❌ Share failed: ${e.message}")
    }
}