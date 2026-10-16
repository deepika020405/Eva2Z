package com.developer.ev2z.ui.screens.trackTrace

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.network.LIVE_LOCATION_POLL_INTERVAL_MS
import com.developer.ev2z.response.Device
import com.developer.ev2z.response.markerVehicleSlug
import com.developer.ev2z.data.local.model.statusDisplayOf
import com.developer.ev2z.data.local.model.isMotionKind
import com.developer.ev2z.data.local.model.statusKindOf
import com.developer.ev2z.ui.components.FloatingContactButton
import com.developer.ev2z.ui.components.glideDurationMs
import com.developer.ev2z.ui.components.isFarJump
import com.developer.ev2z.ui.screens.device_Report.PlatformMapView
import com.developer.ev2z.ui.screens.homeScreen.rememberAddressAt
import com.developer.ev2z.util.rememberRoadWalk
import io.ktor.util.date.getTimeMillis
import eva2z_connect_multiplatform.composeapp.generated.resources.Res
import eva2z_connect_multiplatform.composeapp.generated.resources.app_logo
import org.jetbrains.compose.resources.painterResource
import kotlin.math.abs
import kotlin.text.ifEmpty
import com.developer.ev2z.theme.AppPageBg

// ── Theme Colors (Light Green — App Theme) ───────────────────────
internal val TT_Primary = Color(0xFF6A9F50)
private val TT_PrimaryDark = Color(0xFF4A7D39)
private val TT_PrimaryBg = Color(0xFFF0F7EC)
private val TT_Background = AppPageBg
private val TT_Surface = Color(0xFFFFFFFF)
private val TT_SectionBg = Color(0xFFF3F6F0)
private val TT_Border = Color(0xFFE2E8DD)
private val TT_TextPrimary = Color(0xFF1C2418)
private val TT_TextSec = Color(0xFF5D6858)
private val TT_TextHint = Color(0xFFA0A99B)
private val TT_Running = Color(0xFF85B070)
private val TT_Idle = Color(0xFFF4C44E)
private val TT_Stopped = Color(0xFFE5565B)

/**
 * The screen's own softer palette, but keyed off the shared vocabulary rather than off
 * raw strings. Matching literally missed "running" — which is exactly what
 * `live-location` sends for a moving vehicle — so the dot fell through to grey while
 * the label beside it read "Running".
 */
private fun ttStatusColor(status: String) = when (statusKindOf(status)) {
    "running" -> TT_Running
    "idle" -> TT_Idle
    "parked", "stopped" -> TT_Stopped
    // Offline is a tracker that stopped reporting, not a vehicle that stopped. Red
    // here would have claimed the vehicle is parked when the truth is the app has no
    // idea where it is — and the arrow on the map beside this badge draws it grey.
    else -> Color.Gray
}

private fun ttStatusLabel(status: String) = statusDisplayOf(status)

// ─────────────────────────────────────────────────────────────────────────────
//  TrackTraceScreen
//  liveLocation + trailPoints collected ONLY inside StableTrackMapSection
//  so location polling never recomposes the top-level screen.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun TrackTraceScreen(
    onBack: () -> Unit
) {
    val viewModel = TrackTraceViewModelProvider.viewModel

    PlatformBackHandler(true) { onBack() }

    DisposableEffect(Unit) {
        viewModel.start()
        onDispose { viewModel.stop() }
    }

    TrackTraceContent(
        onBack = onBack,
        map = { StableTrackMapSection(viewModel = viewModel) },
        deviceCard = { LiveDeviceCard(viewModel = viewModel) },
        bottomChip = { modifier -> BottomInfoChip(viewModel = viewModel, modifier = modifier) }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  TrackTraceContent
//  The layout alone — insets, overlay order, contact FAB — with every data read
//  pushed out into a slot. TrackTraceScreen fills the slots from the ViewModel;
//  TrackTracePreviewScreen fills them with static sample data so the screen can
//  be rendered at design time. Reading state inside the slots is also what keeps
//  location polling from recomposing this Box.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun TrackTraceContent(
    onBack: () -> Unit,
    map: @Composable () -> Unit,
    deviceCard: @Composable () -> Unit,
    bottomChip: @Composable (Modifier) -> Unit,
    contactButton: @Composable (Modifier) -> Unit = { modifier ->
        FloatingContactButton(phoneNumber = "919755273822", modifier = modifier)
    }
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TT_Background)
    ) {
        // ── Map (isolated recomposition scope) ──────────────────────
        map()

        // ── Top overlay: Back + Title + Device card ─────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = WindowInsets.statusBars
                        .asPaddingValues()
                        .calculateTopPadding() + 8.dp,
                    start = 14.dp,
                    end = 14.dp
                )
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                Surface(
                    shape = CircleShape,
                    color = TT_Surface,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onBack() }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(18.dp),
                            tint = TT_TextPrimary
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Title chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TT_Surface,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Timeline,
                            contentDescription = null,
                            tint = TT_Primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Track & Trace",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TT_TextPrimary
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Device selector card — reads live-location status inside its own
            // slot, so that poll never recomposes the screen around it.
            deviceCard()
        }

        // ── Bottom info chip (speed + status) ───────────────────────
        bottomChip(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = 14.dp,
                    end = 14.dp,
                    bottom = WindowInsets.navigationBars
                        .asPaddingValues()
                        .calculateBottomPadding() + 16.dp
                )
        )

        // ── Floating contact ────────────────────────────────────────
        contactButton(
            Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = 14.dp,
                    bottom = WindowInsets.navigationBars
                        .asPaddingValues()
                        .calculateBottomPadding() + 72.dp
                )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  LiveDeviceCard — the device selector bound to the ViewModel
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LiveDeviceCard(viewModel: TrackTraceViewModel) {
    val devices by viewModel.devices.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val username by viewModel.username.collectAsState()
    val liveLocation by viewModel.liveLocation.collectAsState()

    DeviceDropdownCardSimple(
        userName = username.ifEmpty { "User" },
        devices = devices,
        selectedDevice = selectedDevice,
        // Real-time source of truth for the LIVE/OFFLINE badge.
        selectedLiveStatus = liveLocation?.data?.latestLocation?.status,
        onDeviceSelect = viewModel::selectDevice
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  StableTrackMapSection
//  liveLocation & trailPoints collected HERE only — never in the parent scope
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StableTrackMapSection(viewModel: TrackTraceViewModel) {
    val liveLocation by viewModel.liveLocation.collectAsState()
    val trailPoints by viewModel.trailPoints.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()

    val latest = liveLocation?.data?.latestLocation

    // validPosition() rather than toDoubleOrNull(): a tracker with no satellite fix
    // reports "0"/"0", and feeding that through teleports the marker to the Gulf of
    // Guinea and back on the next good fix.
    val position = latest?.validPosition()

    // The live fix decides the colour, with the device list's cached state as the
    // fallback.
    //
    // motionKind(), not statusKind(): this endpoint reports a tracker that has not
    // phoned home recently as `"status":"offline"` while `"db_status"` still says
    // "parked", and statusKind() prefers `status` — so a parked vehicle came out grey
    // instead of red, and a running one would too the moment its tracker fell behind.
    //
    // The device row's own statusKind() behind it rather than engine_status, which is
    // ignition and comes back "unknown" on most devices — another route to grey.
    val statusKind = latest?.motionKind()?.takeIf { isMotionKind(it) }
        ?: selectedDevice?.statusKind()?.takeIf { isMotionKind(it) }
        ?: latest?.motionKind()
        ?: statusKindOf(selectedDevice?.engine_status)

    // Resolved from the real fix, not from the interpolated frame below: the lookup is
    // keyed on position, and handing it 60 positions a second would geocode the gaps
    // between the fixes instead of the fix. Called outside the `if` so the composable
    // keeps its slot when a vehicle drops to no-fix and comes back.
    val address = rememberAddressAt(
        latitude = position?.first,
        longitude = position?.second,
        ownerKey = selectedDevice?.device_id?.takeIf { it.isNotEmpty() } ?: "track-trace"
    )

    // The road between the last two fixes, walked rather than cut across.
    //
    // rememberAnimatedLatLng — which this replaces — slid the marker down the straight
    // line between two fixes. A tracker reports every few tens of seconds, so that
    // line is hundreds of metres of chord: the vehicle drove through the buildings
    // inside every bend and straight over the island of every roundabout, and its
    // heading was the chord's, so it came out of the turn facing the wrong way. The
    // walk follows the same snapped road geometry the history map uses and takes its
    // heading from the road ahead.
    val roadLeg by viewModel.roadLeg.collectAsState()
    val walked by rememberRoadWalk(roadLeg)

    if (position != null) {
        val (lat, lng) = position

        PlatformMapView(
            modifier = Modifier.fillMaxSize(),
            // The walk while there is one; the raw fix until the first leg exists, so
            // the map still opens on the vehicle rather than on nothing.
            latitude = walked?.latitude ?: lat,
            longitude = walked?.longitude ?: lng,
            zoom = 15.0,
            trailPoints = trailPoints,
            statusKind = statusKind,
            // The vehicle the server says this tracker is on; null until one is
            // assigned, and the marker falls back to the heading arrow.
            //
            // Both descriptions of this tracker, merged field by field rather than one
            // preferred whole — `live-location` sends no `vehicle_id` and the dropdown's
            // device (from `latest-locations`) is what carries it. See markerVehicleSlug.
            vehicleSlug = markerVehicleSlug(latest, selectedDevice),
            // Null until the first geocode lands, which draws no chip rather than an
            // empty box over the vehicle.
            addressLabel = address,
            // From the road ahead, not from where the coordinates have been. NaN until
            // the vehicle has travelled far enough to have a direction, which the map
            // then falls back to measuring itself.
            headingDegrees = walked?.bearingDegrees ?: Double.NaN
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  BottomInfoChip — shows speed + status, only recomposes on location changes
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BottomInfoChip(
    viewModel: TrackTraceViewModel,
    modifier: Modifier = Modifier
) {
    val liveLocation by viewModel.liveLocation.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val trailPoints by viewModel.trailPoints.collectAsState()

    val latest = liveLocation?.data?.latestLocation

    TrackTraceInfoChip(
        deviceName = selectedDevice?.device_name ?: "—",
        status = latest?.status ?: "offline",
        speed = latest?.speed?.toString() ?: "0",
        trailPointCount = trailPoints.size,
        modifier = modifier
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  TrackTraceInfoChip — the chip itself, with no ViewModel attached
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun TrackTraceInfoChip(
    deviceName: String,
    status: String,
    speed: String,
    trailPointCount: Int,
    modifier: Modifier = Modifier
) {
    val statusClr = ttStatusColor(status)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = TT_Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Device name + status
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deviceName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TT_TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Status badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusClr.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 8.dp,
                                vertical = 3.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .background(statusClr, CircleShape)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                ttStatusLabel(status),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = statusClr
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // Trail points count
                    Text(
                        "$trailPointCount points",
                        fontSize = 11.sp,
                        color = TT_TextHint
                    )
                }
            }

            // Speed display
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = TT_SectionBg
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        speed,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TT_TextPrimary
                    )
                    Text(
                        "km/h",
                        fontSize = 10.sp,
                        color = TT_TextHint
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  DeviceDropdownCardSimple — responsive device selector with app theme
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DeviceDropdownCardSimple(
    modifier: Modifier = Modifier,
    userName: String,
    devices: List<Device>,
    selectedDevice: Device?,
    selectedLiveStatus: String?,
    onDeviceSelect: (Device) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isSingleDevice = devices.size <= 1
    // Prefer real-time live-location status for the selected device;
    // fall back to the cached engine_status from the device list.
    val effectiveStatus = (selectedLiveStatus ?: selectedDevice?.engine_status ?: "offline")
        .lowercase()
    val isLive = effectiveStatus in listOf("on", "moving", "idle")

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = TT_Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(14.dp)) {

            // Header row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = TT_TextPrimary
                    )
                    Text(
                        text = "${devices.size} ${if (devices.size == 1) "Device" else "Devices"}",
                        fontSize = 12.sp,
                        color = TT_TextHint
                    )
                }

                // LIVE / OFFLINE indicator badge — driven by selected device status
                val badgeColor = if (isLive) TT_Primary else TT_Stopped
                val badgeBg = if (isLive) TT_PrimaryBg else TT_Stopped.copy(alpha = 0.12f)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(7.dp)
                                .background(badgeColor, CircleShape)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            if (isLive) "LIVE" else "OFFLINE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Device selector row
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isSingleDevice) {
                        if (!isSingleDevice) expanded = true
                    },
                shape = RoundedCornerShape(14.dp),
                color = TT_SectionBg
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(Res.drawable.app_logo),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(Modifier.width(10.dp))

                    Text(
                        text = selectedDevice?.device_name ?: "Select Device",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = TT_TextPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    if (!isSingleDevice) {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Select Device",
                            tint = TT_TextHint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Dropdown menu (multi-device only)
            if (!isSingleDevice) {
                MaterialTheme(
                    colorScheme = MaterialTheme.colorScheme.copy(
                        surface = TT_Surface,
                        surfaceContainer = TT_Surface
                    )
                ) {
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        devices.forEach { device ->
                            val statusClr = ttStatusColor(
                                device.engine_status ?: "offline"
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            Modifier
                                                .size(8.dp)
                                                .background(statusClr, CircleShape)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                device.device_name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = TT_TextPrimary
                                            )
                                            Text(
                                                "ID: ${device.device_id}",
                                                fontSize = 11.sp,
                                                color = TT_TextHint
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = statusClr.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                ttStatusLabel(
                                                    device.engine_status ?: "offline"
                                                ),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = statusClr,
                                                modifier = Modifier.padding(
                                                    horizontal = 6.dp,
                                                    vertical = 2.dp
                                                )
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    expanded = false
                                    onDeviceSelect(device)
                                },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  rememberAnimatedLatLng — smooth lat/lng animation helper
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Interpolates between two live fixes so the marker moves continuously.
 *
 * The glide is paced by the gap *actually observed* between the last two fixes rather
 * than by the nominal poll interval. A flat duration only matches reality when the
 * network does, and it used to be 1000 ms against fixes arriving every 10 s: the marker
 * covered a whole interval of travel in one second and then stood still for nine — a
 * lurch-and-freeze that read as the marker crawling far slower than the speed on the
 * chip. [durationMillis] now only seeds the very first glide, before there are two
 * fixes to measure a gap from.
 *
 * A progress fraction is animated rather than the coordinates themselves: a Float
 * longitude quantises to roughly a metre, which stair-steps at high zoom. A fix that
 * lands mid-glide resumes from the current interpolated point, so the marker never
 * snaps backwards; only an implausibly large jump skips straight to the new position.
 */
@Composable
fun rememberAnimatedLatLng(
    latitude: Double,
    longitude: Double,
    durationMillis: Int = LIVE_LOCATION_POLL_INTERVAL_MS.toInt()
): Pair<Double, Double> {

    val progress = remember { Animatable(1f) }
    var from by remember { mutableStateOf(latitude to longitude) }
    var to by remember { mutableStateOf(latitude to longitude) }
    // Arrival time of the fix currently being glided towards, so the next glide can be
    // paced by a real measurement. 0 until the first fix lands.
    var lastFixAtMs by remember { mutableStateOf(0L) }
    var glideMillis by remember { mutableStateOf(durationMillis) }

    LaunchedEffect(latitude, longitude) {
        if (latitude == to.first && longitude == to.second) return@LaunchedEffect

        val farJump = isFarJump(to.first, to.second, latitude, longitude)

        val now = getTimeMillis()
        if (lastFixAtMs != 0L) {
            glideMillis = glideDurationMs(now - lastFixAtMs)
        }
        lastFixAtMs = now

        // Resume from wherever the previous glide had reached.
        val reached = progress.value.toDouble()
        from = (from.first + (to.first - from.first) * reached) to
                (from.second + (to.second - from.second) * reached)
        to = latitude to longitude

        if (farJump) {
            from = to
            progress.snapTo(1f)
        } else {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(glideMillis, easing = LinearEasing))
        }
    }

    val fraction = progress.value.toDouble()
    return (from.first + (to.first - from.first) * fraction) to
            (from.second + (to.second - from.second) * fraction)
}