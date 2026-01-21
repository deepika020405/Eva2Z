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
                clipsToBounds = true
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

    val url = NSURL.fileURLWithPath(
        NSTemporaryDirectory() + fileName
    )

    bytes.toNSData().writeToURL(url, atomically = true)

    val controller = UIDocumentInteractionController()
    controller.URL = url

    // ❌ delegate REMOVED (important)
    documentController = controller
    controller.presentPreviewAnimated(true)
}