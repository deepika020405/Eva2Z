package com.developer.ev2z.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
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
            val image = UIImage(data = bytes.toNSData())
            UIImageView(image).apply {
                contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
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
    val viewController = context as UIViewController

    val path = NSTemporaryDirectory() + fileName
    val url = NSURL.fileURLWithPath(path)

    bytes.toNSData().writeToURL(url, atomically = true)

    val controller = UIDocumentInteractionController()
    controller.URL = url

    controller.delegate =
        object : NSObject(), UIDocumentInteractionControllerDelegateProtocol {
            override fun documentInteractionControllerViewControllerForPreview(
                controller: UIDocumentInteractionController
            ): UIViewController = viewController
        }

    documentController = controller   // 🔥 CRITICAL
    controller.presentPreviewAnimated(true)
}



