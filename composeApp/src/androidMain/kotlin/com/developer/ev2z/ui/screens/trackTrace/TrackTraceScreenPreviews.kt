package com.developer.ev2z.ui.screens.trackTrace

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

// ─────────────────────────────────────────────────────────────────────────────
//  @Preview entry points for TrackTraceScreen.
//
//  Open this file in Android Studio and use the split / design view. The sample
//  data and the stand-in map live in commonMain (TrackTracePreviewScreen.kt);
//  these functions only choose which state to render, at which size.
//
//  Nothing here ships: debugImplementation(compose.uiTooling) is what renders it,
//  and R8 drops @Preview functions from release builds.
// ─────────────────────────────────────────────────────────────────────────────

@Preview(
    name = "Track & Trace · live fix, 2 devices",
    showBackground = true,
    widthDp = 360,
    heightDp = 760
)
@Composable
private fun TrackTraceLivePreview() {
    TrackTracePreviewScreen(TrackTracePreviewState.LIVE_MULTI_DEVICE)
}

@Preview(
    name = "Track & Trace · idle, single device",
    showBackground = true,
    widthDp = 360,
    heightDp = 760
)
@Composable
private fun TrackTraceIdlePreview() {
    TrackTracePreviewScreen(TrackTracePreviewState.IDLE_SINGLE_DEVICE)
}

@Preview(
    name = "Track & Trace · no usable fix",
    showBackground = true,
    widthDp = 360,
    heightDp = 760
)
@Composable
private fun TrackTraceNoFixPreview() {
    TrackTracePreviewScreen(TrackTracePreviewState.NO_FIX)
}

@Preview(
    name = "Track & Trace · small phone",
    showBackground = true,
    widthDp = 320,
    heightDp = 640
)
@Composable
private fun TrackTraceSmallPhonePreview() {
    TrackTracePreviewScreen(TrackTracePreviewState.LIVE_MULTI_DEVICE)
}

@Preview(
    name = "Device card · long name, offline",
    showBackground = true,
    widthDp = 360
)
@Composable
private fun DeviceDropdownCardPreview() {
    TrackTraceDeviceCardPreview()
}

@Preview(
    name = "Info chip · each status",
    showBackground = true,
    widthDp = 360
)
@Composable
private fun InfoChipStatesPreview() {
    TrackTraceInfoChipPreview()
}