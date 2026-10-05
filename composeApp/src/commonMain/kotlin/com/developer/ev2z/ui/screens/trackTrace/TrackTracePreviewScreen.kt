package com.developer.ev2z.ui.screens.trackTrace

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.developer.ev2z.response.Device
import com.developer.ev2z.theme.AppPageBg

// ─────────────────────────────────────────────────────────────────────────────
//  Design-time renderings of TrackTraceContent.
//
//  This file carries no real data and is never reached from the app: it exists so
//  the screen's layout can be rendered without a backend, a token or a Maps key.
//  The @Preview entry points live in androidMain/.../TrackTraceScreenPreviews.kt
//  (Android Studio's preview pane only renders Android-target previews); this part
//  stays in commonMain so iOS and desktop tooling can use the same sample data.
//
//  PlatformMapView needs the Google Maps SDK, which cannot inflate in a preview, so
//  TrackTracePreviewMap stands in for it — illustrative streets, not a real place.
// ─────────────────────────────────────────────────────────────────────────────

/** The three shapes the screen can actually take, keyed to what the ViewModel holds. */
enum class TrackTracePreviewState {
    /** A valid fix on a moving vehicle, two devices owned → selector is tappable. */
    LIVE_MULTI_DEVICE,

    /** A valid fix, engine idling, one device → selector has no dropdown affordance. */
    IDLE_SINGLE_DEVICE,

    /** No usable lat/lng: the map is never composed and AppPageBg shows through. */
    NO_FIX
}

private val previewScooter = Device(
    id = 1,
    device_id = "86429103775521",
    device_name = "EV2Z-4471 · Scooter",
    sim_no = "8991000912345678901U",
    device_type = "GPS Tracker",
    engine_status = "moving",
    created_at = "2026-02-14 09:12:00"
)

private val previewVan = Device(
    id = 2,
    device_id = "86429103779914",
    device_name = "EV2Z-8820 · Van",
    sim_no = "8991000912345678902U",
    device_type = "GPS Tracker",
    engine_status = "idle",
    created_at = "2026-03-02 16:40:00"
)

/**
 * [TrackTraceContent] with static data in every slot.
 *
 * @param state which of the screen's states to render.
 */
@Composable
fun TrackTracePreviewScreen(
    state: TrackTracePreviewState = TrackTracePreviewState.LIVE_MULTI_DEVICE
) {
    val devices = when (state) {
        TrackTracePreviewState.LIVE_MULTI_DEVICE -> listOf(previewScooter, previewVan)
        TrackTracePreviewState.IDLE_SINGLE_DEVICE -> listOf(previewVan.copy(engine_status = "idle"))
        TrackTracePreviewState.NO_FIX -> listOf(previewVan.copy(engine_status = "offline"))
    }
    val selected = devices.first()

    // What the live-location poll would be reporting in each state.
    val liveStatus = when (state) {
        TrackTracePreviewState.LIVE_MULTI_DEVICE -> "moving"
        TrackTracePreviewState.IDLE_SINGLE_DEVICE -> "idle"
        TrackTracePreviewState.NO_FIX -> "offline"
    }
    val speed = when (state) {
        TrackTracePreviewState.LIVE_MULTI_DEVICE -> "38"
        else -> "0"
    }
    val trailPoints = when (state) {
        TrackTracePreviewState.LIVE_MULTI_DEVICE -> 42
        TrackTracePreviewState.IDLE_SINGLE_DEVICE -> 17
        TrackTracePreviewState.NO_FIX -> 0
    }

    TrackTraceContent(
        onBack = {},
        map = {
            // NO_FIX draws nothing at all, exactly like StableTrackMapSection when
            // latitude/longitude fail to parse.
            if (state != TrackTracePreviewState.NO_FIX) {
                TrackTracePreviewMap(modifier = Modifier.fillMaxSize())
            }
        },
        deviceCard = {
            DeviceDropdownCardSimple(
                userName = "Rashmi Rani",
                devices = devices,
                selectedDevice = selected,
                selectedLiveStatus = liveStatus,
                onDeviceSelect = {}
            )
        },
        bottomChip = { modifier ->
            TrackTraceInfoChip(
                deviceName = selected.device_name,
                status = liveStatus,
                speed = speed,
                trailPointCount = trailPoints,
                modifier = modifier
            )
        }
    )
}

/**
 * The device selector on its own, with the awkward cases: a name long enough to
 * ellipsize, and a device whose cached `engine_status` disagrees with what the live
 * poll last said.
 */
@Composable
fun TrackTraceDeviceCardPreview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppPageBg)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        DeviceDropdownCardSimple(
            userName = "Rashmi Rani",
            devices = listOf(previewScooter, previewVan),
            selectedDevice = previewScooter.copy(
                device_name = "EV2Z-4471 · Delivery scooter, Indore north zone"
            ),
            selectedLiveStatus = "moving",
            onDeviceSelect = {}
        )

        // Live poll says nothing yet, so the badge falls back to engine_status.
        DeviceDropdownCardSimple(
            userName = "Rashmi Rani",
            devices = listOf(previewVan.copy(engine_status = "parked")),
            selectedDevice = previewVan.copy(engine_status = "parked"),
            selectedLiveStatus = null,
            onDeviceSelect = {}
        )
    }
}

/**
 * The info chip across the statuses the backend actually sends. "running" and
 * "ideal" are real values from the devices API that `ttStatusColor` does not match,
 * so they render grey with a raw capitalised label — visible here side by side.
 */
@Composable
fun TrackTraceInfoChipPreview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppPageBg)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TrackTraceInfoChip("EV2Z-4471 · Scooter", "moving", "38", 42)
        TrackTraceInfoChip("EV2Z-8820 · Van", "idle", "0", 17)
        TrackTraceInfoChip("EV2Z-8820 · Van", "parked", "0", 17)
        TrackTraceInfoChip("EV2Z-8820 · Van", "offline", "0", 0)
        TrackTraceInfoChip("EV2Z-1102 · Truck", "running", "54", 88)
        TrackTraceInfoChip("EV2Z-1102 · Truck", "ideal", "0", 88)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  TrackTracePreviewMap — a stand-in for PlatformMapView
//  Synthetic streets plus a trail, so the floating overlays can be judged against
//  the kind of contrast a real map tile gives them.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun TrackTracePreviewMap(modifier: Modifier = Modifier) {
    val land = Color(0xFFEAF1E6)
    val block = Color(0xFFE1EADC)
    val road = Color(0xFFFBFDFA)
    val park = Color(0xFFDCE9D4)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        fun x(f: Float) = w * f
        fun y(f: Float) = h * f

        drawRect(color = land)

        // City blocks
        listOf(
            Triple(Offset(x(-0.03f), y(0.08f)), x(0.42f), y(0.18f)),
            Triple(Offset(x(0.49f), y(0.05f)), x(0.36f), y(0.22f)),
            Triple(Offset(x(-0.03f), y(0.33f)), x(0.31f), y(0.21f)),
            Triple(Offset(x(0.39f), y(0.35f)), x(0.29f), y(0.16f)),
            Triple(Offset(x(0.79f), y(0.34f)), x(0.25f), y(0.19f)),
            Triple(Offset(x(0.06f), y(0.60f)), x(0.39f), y(0.17f)),
            Triple(Offset(x(0.56f), y(0.58f)), x(0.36f), y(0.21f)),
            Triple(Offset(x(0.11f), y(0.83f)), x(0.33f), y(0.17f)),
            Triple(Offset(x(0.58f), y(0.85f)), x(0.39f), y(0.15f))
        ).forEach { (topLeft, bw, bh) ->
            drawRect(
                color = block,
                topLeft = topLeft,
                size = Size(bw, bh)
            )
        }

        // Park
        drawOval(
            color = park,
            topLeft = Offset(x(0.67f), y(0.63f)),
            size = Size(x(0.32f), y(0.13f))
        )

        // Arterial roads
        listOf(0.30f, 0.56f, 0.80f).forEach { f ->
            drawLine(road, Offset(0f, y(f)), Offset(w, y(f)), strokeWidth = h * 0.022f)
        }
        listOf(0.43f, 0.92f).forEach { f ->
            drawLine(road, Offset(x(f), 0f), Offset(x(f), h), strokeWidth = w * 0.039f)
        }
        // Side streets
        drawLine(road, Offset(x(0.19f), 0f), Offset(x(0.19f), h), strokeWidth = w * 0.025f)
        listOf(0.42f, 0.92f).forEach { f ->
            drawLine(road, Offset(0f, y(f)), Offset(w, y(f)), strokeWidth = h * 0.010f)
        }

        // Travelled trail — the shape trailPoints would trace through those streets
        val trail = Path().apply {
            moveTo(x(0.19f), y(0.90f))
            lineTo(x(0.19f), y(0.80f))
            lineTo(x(0.43f), y(0.80f))
            lineTo(x(0.43f), y(0.56f))
            lineTo(x(0.70f), y(0.56f))
            lineTo(x(0.70f), y(0.42f))
            lineTo(x(0.92f), y(0.42f))
        }
        drawPath(
            path = trail,
            color = TT_Primary.copy(alpha = 0.18f),
            style = Stroke(
                width = w * 0.036f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        drawPath(
            path = trail,
            color = TT_Primary,
            style = Stroke(
                width = w * 0.014f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Trail start
        drawCircle(Color.White, radius = w * 0.016f, center = Offset(x(0.19f), y(0.90f)))
        drawCircle(
            TT_Primary,
            radius = w * 0.016f,
            center = Offset(x(0.19f), y(0.90f)),
            style = Stroke(width = w * 0.008f)
        )

        // Current position marker, with its accuracy halo
        val head = Offset(x(0.92f), y(0.42f))
        drawCircle(TT_Primary.copy(alpha = 0.14f), radius = w * 0.067f, center = head)
        drawCircle(Color.White, radius = w * 0.031f, center = head)
        drawCircle(TT_Primary, radius = w * 0.021f, center = head)
    }
}