package com.developer.ev2z.driverscore

import androidx.compose.runtime.Composable
import com.developer.ev2z.ui.screens.DriverScore.DriverScoreFlowScreen

/**
 * iOS entry point for Driver Score feature.
 * Matches Android implementation.
 */
@Composable
fun DriverScoreIosScreen(
    onBack: () -> Unit = {}   // optional for iOS
) {
    DriverScoreFlowScreen(onBack = onBack)
}