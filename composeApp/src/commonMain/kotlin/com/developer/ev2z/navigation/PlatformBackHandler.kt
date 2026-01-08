package com.developer.ev2z.navigation

// commonMain

import androidx.compose.runtime.Composable

@Composable
expect fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit
)
// commonMain
expect fun exitApp(activity: Any?)