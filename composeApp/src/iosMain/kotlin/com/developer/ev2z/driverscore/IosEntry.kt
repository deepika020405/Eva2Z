package com.developer.ev2z.driverscore

// ─────────────────────────────────────────────────────────────────────────────
//  iosMain integration
//  File: src/iosMain/kotlin/com/eva2z/connect/driverScore/IosEntry.kt
// ─────────────────────────────────────────────────────────────────────────────


import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.developer.ev2z.ui.screens.DriverScore.DriverScoreViewModel


/**
 * iOS entry point — no Android ViewModel needed.
 * The ViewModel is retained in composition via `remember`.
 *
 * Usage in your iOS SwiftUI host:
 *
 *   struct DriverScoreView: View {
 *       var body: some View {
 *           ComposeView { _ in
 *               DriverScoreIosScreen()
 *           }
 *       }
 *   }
 *
 * Or via ComposeUIViewController:
 *
 *   fun DriverScoreViewController() = ComposeUIViewController {
 *       DriverScoreIosScreen()
 *   }
 */
@Composable
fun DriverScoreIosScreen() {
    val viewModel = remember { DriverScoreViewModel() }
}