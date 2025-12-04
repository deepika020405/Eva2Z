// composeApp/src/iosMain/kotlin/com/developer/ev2z/navigation/SetupStatusBar.ios.kt
package com.developer.ev2z.navigation

import androidx.compose.runtime.Composable
import platform.UIKit.UIApplication
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.setStatusBarStyle

@Composable
actual fun SetupStatusBar() {
    UIApplication.sharedApplication.setStatusBarStyle(
        UIStatusBarStyleDarkContent
    )
}