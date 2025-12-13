package com.developer.ev2z

import androidx.compose.runtime.Composable

@Composable
expect fun getPlatformContext(): Any?
expect fun getAppContext(): Any?