// composeApp/src/commonMain/kotlin/com/developer/ev2z/util/ImagePicker.kt
package com.developer.ev2z.util

import androidx.compose.runtime.Composable

/**
 * Cross-platform Image Picker
 * Use rememberImagePicker() to get an instance
 */
@Composable
expect fun rememberImagePicker(
    onImageSelected: (ByteArray?) -> Unit
): ImagePickerLauncher

/**
 * Interface for launching image picker
 */
interface ImagePickerLauncher {
    fun launch()
}