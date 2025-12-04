// composeApp/src/androidMain/kotlin/com/developer/ev2z/navigation/SetupStatusBar.android.kt
package com.developer.ev2z.navigation

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
actual fun SetupStatusBar() {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.WHITE
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = true
        }
    }
}