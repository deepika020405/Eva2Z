package com.developer.ev2z.navigation

// commonMain

import androidx.compose.runtime.Composable

@Composable
expect fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit
)

expect fun exitApp()