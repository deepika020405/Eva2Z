@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.developer.ev2z.util

import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIPopoverPresentationController
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController

actual fun shareText(text: String, subject: String) {
    val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: run {
        println("❌ Share failed: no root view controller")
        return
    }

    // Presenting on a controller that already has something on top of it is a no-op,
    // so walk to whatever is actually on screen.
    var top: UIViewController = root
    while (true) {
        top = top.presentedViewController ?: break
    }

    try {
        val activityController = UIActivityViewController(
            activityItems = listOf(text),
            applicationActivities = null
        )

        // On iPad the sheet is a popover and UIKit throws unless it has an anchor.
        // Anchoring to the centre of the presenting view gives a sane placement.
        activityController.popoverPresentationController?.let { popover ->
            popover.sourceView = top.view
            val bounds = top.view.bounds
            val centerX = bounds.useContents { size.width / 2.0 }
            val centerY = bounds.useContents { size.height / 2.0 }
            popover.sourceRect = CGRectMake(centerX, centerY, 0.0, 0.0)
        }

        top.presentViewController(activityController, animated = true, completion = null)
    } catch (e: Exception) {
        println("❌ Share failed: ${e.message}")
    }
}