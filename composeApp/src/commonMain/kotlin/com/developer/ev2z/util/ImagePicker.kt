// composeApp/src/commonMain/kotlin/com/developer/ev2z/util/ImagePicker.kt
package com.developer.ev2z.util

import androidx.compose.runtime.Composable

enum class ImageSource {
    CAMERA, GALLERY
}
/**
 * Cross-platform Image Picker
 * Use rememberImagePicker() to get an instance
 */
@Composable
expect fun rememberImagePicker(
    onImageSelected: (ByteArray?) -> Unit
): ImagePickerLauncher

/**
 * NEW picker with direct source
 */

/**
 * Interface for launching image picker
 */
interface ImagePickerLauncher {
    fun launch()
}