package com.developer.ev2z

import androidx.compose.runtime.Composable

@Composable
expect fun BackPressHandler(onBack: () -> Unit)