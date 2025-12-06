package com.developer.ev2z

import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable

actual @Composable
fun getPlatformContext(): Any? = LocalContext.current