@file:Suppress(
    "MISSING_DEPENDENCY_SUPERCLASS_WARNING",
    "MISSING_DEPENDENCY_SUPERCLASS_IN_TYPE_ARGUMENT"
)

package com.developer.ev2z.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import com.developer.ev2z.util.crypto.toNSData
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject

@Composable
actual fun PlatformDocumentViewer(
    bytes: ByteArray,
    modifier: Modifier
) {
    UIKitView(
        modifier = modifier,
        factory = {
            // ✅ STEP 1: Clean corrupted header (IMPORTANT FIX)
            val cleanBytes = if (bytes.isNotEmpty() && bytes[0] == 0x20.toByte()) {
                bytes.copyOfRange(1, bytes.size)
            } else {
                bytes
            }

            // ✅ STEP 2: Empty check
            if (cleanBytes.isEmpty()) {
                return@UIKitView UILabel().apply {
                    text = "Empty file"
                    textAlignment = NSTextAlignmentCenter
                }
            }

            val data = cleanBytes.toNSData()
            val image = UIImage(data = data)

            // ✅ STEP 3: Safe handling (NO CRASH)
            if (image != null) {
                UIImageView(image).apply {
                    contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
                    clipsToBounds = true
                }
            } else {
                // 👉 NOT IMAGE → fallback UI
                UILabel().apply {
                    text = "Unsupported or corrupted file"
                    textAlignment = NSTextAlignmentCenter
                }
            }
        }
    )
}

private var documentController: UIDocumentInteractionController? = null

actual fun platformDownloadFile(
    context: Any?,
    bytes: ByteArray,
    fileName: String
) {
    val vc = context as? UIViewController ?: return

    // ✅ Safety: empty file check
    if (bytes.isEmpty()) {
        println("❌ File bytes empty")
        return
    }

    val url = NSURL.fileURLWithPath(
        NSTemporaryDirectory() + fileName
    )

    bytes.toNSData().writeToURL(url, atomically = true)

    val controller = UIDocumentInteractionController()
    controller.URL = url

    // ✅ IMPORTANT FIX: delegate added back
    controller.delegate = object : NSObject(), UIDocumentInteractionControllerDelegateProtocol {
        override fun documentInteractionControllerViewControllerForPreview(
            controller: UIDocumentInteractionController
        ): UIViewController {
            return vc
        }
    }

    documentController = controller

    // ✅ Safe preview call
    val success = controller.presentPreviewAnimated(true)
    if (!success) {
        println("❌ Preview failed to open")
    }
}