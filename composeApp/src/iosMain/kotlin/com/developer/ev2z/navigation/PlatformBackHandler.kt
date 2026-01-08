package com.developer.ev2z.navigation

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit
) {
    // ❌ iOS has no hardware back button
    // Navigation handled by UIKit / SwiftUI
    // Intentionally NO-OP
}

//actual fun exitApp() {
//    // ❌ Apple does NOT allow apps to close themselves
//    // This must be a NO-OP to avoid App Store rejection
//}

// iosMain
actual fun exitApp(activity: Any?) {
    // do nothing
}