package com.developer.ev2z.ui.screens.deviceStatus

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.data.local.model.statusColorOf
import com.developer.ev2z.data.local.model.statusDisplayOf
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.response.MyDevicesResponse
import com.developer.ev2z.response.StatusCount
import com.developer.ev2z.response.StatusDurations
import com.developer.ev2z.response.LegStart
import com.developer.ev2z.response.SessionLeg
import com.developer.ev2z.response.StatusSession
import com.developer.ev2z.response.durationLabel
import com.developer.ev2z.response.endClock
import com.developer.ev2z.response.formatDurationSeconds
import com.developer.ev2z.response.of
import com.developer.ev2z.response.sessionCount
import com.developer.ev2z.response.spansDays
import com.developer.ev2z.response.startClock
import com.developer.ev2z.response.trackedSeconds
import eva2z_connect_multiplatform.composeapp.generated.resources.Res
import eva2z_connect_multiplatform.composeapp.generated.resources.app_logo
import org.jetbrains.compose.resources.painterResource
import com.developer.ev2z.theme.AppPageBg
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceStatusScreen(onBack: () -> Unit) {

    val viewModel = DeviceStatusViewModelProvider.viewModel

    val devices by viewModel.devices.collectAsState()
    val statusResponse by viewModel.deviceStatus.collectAsState()
    val sessions by viewModel.filteredSessions.collectAsState(emptyList())
    val filter by viewModel.statusFilter.collectAsState()
    val addressMap by viewModel.addressMap.collectAsState()
    val selectedDeviceId by viewModel.selectedDeviceId.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()



    PlatformBackHandler(true) { onBack() }

    LaunchedEffect(Unit) {
        viewModel.loadDevices()
    }

    // One device is no choice at all, so it is made for the user — but only the device.
    // The day is still theirs to pick, which is what actually fires the request.
    LaunchedEffect(devices) {
        val deviceList = devices?.devices ?: emptyList()

        if (deviceList.size == 1) {
            viewModel.selectDevice(deviceList.first().device_id)
        }
    }

    Scaffold(
        containerColor = Color(0xFFF5F5F5), // 👈 screen background
        topBar = {
            TopAppBar(
                title = { Text("Status Report") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppPageBg
                )
            )

        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {

            devices?.let {
                DeviceDropdownCardSimple(
                    userName = devices?.username.orEmpty(),
                    devices = it,
                    selectedDeviceId = selectedDeviceId,
                    selectedDate = selectedDate,
                    onDeviceSelect = viewModel::selectDevice,
                    onDateSelect = viewModel::selectDate,
                    onLocationClick = { /* optional */ }
                )

            }

            val statusData = statusResponse?.data

            if (statusData != null) {
                Spacer(Modifier.height(12.dp))

                // The tiles are the filter: a count the user has just read is the thing
                // they reach for to see those sessions, and a separate chip row below it
                // only repeated the same five words without the numbers.
                StatusSummaryCard(
                    counts = statusData.status_count,
                    durations = statusData.status_duration,
                    totalSessions = statusData.sessionCount,
                    timezone = statusData.timezone,
                    // The window the report actually covers, first fix to last — read
                    // off the unfiltered day, so it does not shrink when a filter is on.
                    dayStart = statusData.sessions.firstOrNull()?.startClock(),
                    dayEnd = statusData.sessions.lastOrNull()?.endClock(),
                    selected = filter,
                    onSelect = viewModel::setFilter
                )
            }

            Spacer(Modifier.height(12.dp))

            when {
                // The two choices, in the order the screen asks for them: a device
                // first, then the day — nothing is fetched until both are in hand.
                selectedDeviceId == null -> {
                    EmptyState(
                        title = "Select a device",
                        subtitle = "Please select a device to view status history"
                    )
                }

                selectedDate == null -> {
                    EmptyState(
                        title = "Select a date",
                        subtitle = "Pick a date to view this device's status report"
                    )
                }

                loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF00A844))
                    }
                }

                error != null -> {
                    EmptyState(
                        title = "Could not load report",
                        subtitle = error ?: "Please try again"
                    )
                }

                statusResponse?.success == false -> {
                    EmptyState(
                        title = "No data found",
                        subtitle = statusResponse?.message
                            ?: "No status available for ${displayDate(selectedDate)}"
                    )
                }

                sessions.isEmpty() -> {
                    EmptyState(
                        title = if (filter == "all") "No activity" else "No $filter sessions",
                        subtitle = if (filter == "all")
                            "This device has no status records for ${displayDate(selectedDate)}"
                        else
                            "Nothing recorded with this status on this date"
                    )
                }

                else -> {
                    // Newest first: the day is read from what the vehicle is doing now
                    // backwards, and the server sends it in the order it happened.
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(sessions.asReversed()) { leg ->
                            // Only the rows on screen ask for an address, which is what
                            // keeps a day of sessions from firing twenty geocodes at
                            // once; resolveAddress dedupes the repeats, and a journey's
                            // start is the previous session's end, already resolved.
                            LaunchedEffect(leg.start, leg.end) {
                                leg.start?.let { viewModel.resolveAddress(it.first, it.second) }
                                leg.end?.let { viewModel.resolveAddress(it.first, it.second) }
                            }

                            StatusSessionCard(
                                leg = leg,
                                startAddress = leg.start?.let { addressMap[addressKey(it)] },
                                endAddress = leg.end?.let { addressMap[addressKey(it)] }
                            )
                        }
                    }
                }
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDropdownCardSimple(
    userName: String,
    devices: MyDevicesResponse,
    selectedDeviceId: String?,
    selectedDate: String?,
    onDeviceSelect: (String) -> Unit,
    onDateSelect: (String) -> Unit,
    onLocationClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    // Picking a device is the first half of the question; the picker for the second half
    // opens on its own so the user is not left wondering what else is wanted.
    var showDatePicker by remember { mutableStateOf(false) }

    val deviceCount = devices.devices.size
    val isSingleDevice = deviceCount == 1

    // 🔥 device name resolve — only for a device actually selected. Falling back to the
    // first device's name made an unselected dropdown look like a made choice, and the
    // user then waited for a report nothing had asked for.
    val selectedDeviceName = devices.devices
        .find { it.device_id == selectedDeviceId }
        ?.let { it.device_name.ifEmpty { it.device_id } }
        ?: "Select Device"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // 🔹 TOP ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$userName",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onLocationClick() }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$deviceCount Devices Listed",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 🔹 DEVICE ROW
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(41.dp)
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                    .clickable(enabled = !isSingleDevice) { expanded = true }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        text = selectedDeviceName,
                        fontSize = 15.sp,
                        maxLines = 1,
                        color = if (selectedDeviceId != null) Color(0xFF1B1B1B) else Color.Gray
                    )
                }

                // 🔥 Hide dropdown icon if single device
                if (!isSingleDevice) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }

            // 🔥 Dropdown only if multiple devices
            if (!isSingleDevice) {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    devices.devices.forEach { device ->
                        DropdownMenuItem(
                            text = {
                                Text(device.device_name.ifEmpty { device.device_id })
                            },
                            onClick = {
                                expanded = false
                                onDeviceSelect(device.device_id)
                                // Straight into step two — unless a day is already
                                // chosen, in which case the report is on its way.
                                if (selectedDate == null) showDatePicker = true
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(5.dp))

            // The date row only means anything once there is a device to report on.
            DateSelectorRow(
                selectedDate = selectedDate,
                enabled = selectedDeviceId != null,
                showDialog = showDatePicker,
                onShowDialogChange = { showDatePicker = it },
                onDateSelect = onDateSelect
            )
        }
    }
}

/**
 * The day picker: a row showing the chosen date, and the dialog behind it.
 *
 * Disabled until a device is selected — the report is for one device on one day, and a
 * date picked against no device has nothing to fetch. The dialog's open state lives in
 * the caller so selecting a device can open it directly.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun DateSelectorRow(
    selectedDate: String?,
    enabled: Boolean,
    showDialog: Boolean,
    onShowDialogChange: (Boolean) -> Unit,
    onDateSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(41.dp)
            .background(
                Color(0xFFF5F5F5),
                RoundedCornerShape(8.dp)
            )
            .clickable(enabled = enabled) { onShowDialogChange(true) }
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = when {
                selectedDate != null -> displayDate(selectedDate)
                enabled -> "Select Date"
                else -> "Select a device first"
            },
            fontSize = 14.sp,
            color = if (selectedDate != null) Color(0xFF1B1B1B) else Color.Gray
        )

        Icon(
            imageVector = Icons.Default.DateRange,
            contentDescription = "Select date",
            tint = if (enabled) Color(0xFF2E7D32) else Color.Gray
        )
    }

    if (showDialog) {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate?.let { isoToUtcMillis(it) },
            // There is no report for a day that has not happened.
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    Instant.fromEpochMilliseconds(utcTimeMillis)
                        .toLocalDateTime(TimeZone.UTC)
                        .date <= today
            }
        )

        DatePickerDialog(
            onDismissRequest = { onShowDialogChange(false) },
            confirmButton = {
                TextButton(
                    onClick = {
                        // The picker's millis are UTC midnight of the chosen day, so UTC
                        // is what reads the day back out — the device's own timezone
                        // would shift it by a day west of Greenwich.
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.fromEpochMilliseconds(millis)
                                .toLocalDateTime(TimeZone.UTC)
                                .date
                            onDateSelect(date.toString())
                        }
                        onShowDialogChange(false)
                    },
                    enabled = datePickerState.selectedDateMillis != null
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { onShowDialogChange(false) }) {
                    Text("Cancel")
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                DatePicker(
                    state = datePickerState,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** UTC midnight of an ISO `yyyy-MM-dd` day — what [DatePicker] counts its days in. */
@OptIn(ExperimentalTime::class)
private fun isoToUtcMillis(iso: String): Long? = try {
    LocalDate.parse(iso).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
} catch (e: Exception) {
    null
}

private val MONTHS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

/** `2026-10-19` → `19 Oct 2026`, with the ISO string kept for the API. */
fun displayDate(iso: String?): String {
    if (iso == null) return "Select Date"
    return try {
        val date = LocalDate.parse(iso)
        "${date.day} ${MONTHS[date.month.ordinal]} ${date.year}"
    } catch (e: Exception) {
        iso
    }
}



/** The map key [DeviceStatusViewModel.resolveAddress] files a resolved address under. */
private fun addressKey(point: Pair<Double, Double>): String = "${point.first},${point.second}"

/**
 * The day in four numbers and four totals, each of them the filter for its own
 * sessions.
 *
 * A tile reads as a count and taps as a filter — tapping the selected one clears it,
 * so "all" needs no chip of its own. The zero tiles stay: that a vehicle idled not
 * once today is as much a fact as the four times it moved, and a row that changes
 * width as statuses appear is harder to scan than one that does not.
 *
 * The time under each count is the thing the session list cannot show: four moving
 * sessions come to 6h 49m, and adding seven rows up by eye is work the summary should
 * already have done.
 */
@Composable
fun StatusSummaryCard(
    counts: StatusCount,
    durations: StatusDurations?,
    totalSessions: Int,
    timezone: String?,
    dayStart: String?,
    dayEnd: String?,
    selected: String,
    onSelect: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            /*Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's activity",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1B1B1B)
                )
                Text(
                    text = if (totalSessions == 1) "1 session" else "$totalSessions sessions",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }*/

//            val tracked = durations?.trackedSeconds ?: 0
//
//            if (durations != null && tracked > 0) {
//                Spacer(Modifier.height(12.dp))
//                StatusShareBar(durations, tracked)
//                Spacer(Modifier.height(6.dp))
//                Text(
//                    // "Tracked", not "today": the report covers what the tracker
//                    // reported, and the hours it was off the air belong to no bucket at
//                    // all — calling 6h 49m a whole day would be a lie by rounding. The
//                    // window beside it is what those hours were spread across, which is
//                    // also the only place the day's own clock times appear.
//                    text = buildString {
//                        append(formatDurationSeconds(tracked))
//                        append(" tracked")
//                        if (dayStart != null && dayEnd != null) {
//                            append("  ·  ")
//                            append(dayStart)
//                            append(" – ")
//                            append(dayEnd)
//                        }
//                    },
//                    fontSize = 13.sp,
//                    color = Color(0xFF555555)
//                )
//            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("moving", "idle", "parked", "offline").forEach { status ->
                    val (label, color) = statusUi(status)
                    val seconds = durations?.of(status)?.total_seconds ?: 0
                    StatusCountTile(
                        label = label,
                        count = counts.of(status),
                        duration = if (seconds > 0) formatDurationSeconds(seconds) else null,
                        color = color,
                        isSelected = selected == status,
                        onClick = { onSelect(if (selected == status) "all" else status) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (selected != "all" || timezone != null) {
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timezone?.let { "Times in $it" }.orEmpty(),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    if (selected != "all") {
                        Text(
                            text = "Show all",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.clickable { onSelect("all") }
                        )
                    }
                }
            }
        }
    }
}

/**
 * The day's four statuses as one bar, each segment its share of [tracked].
 *
 * A slice under half a percent is left out rather than widened into a visible sliver:
 * one second of parking inside seven hours of driving is not a part of this day the eye
 * should be able to see, and padding it up would claim the vehicle rested when it did
 * not. The counts below are where those sessions are accounted for.
 */
@Composable
private fun StatusShareBar(durations: StatusDurations, tracked: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFEDEDED))
    ) {
        listOf("moving", "idle", "parked", "offline").forEach { status ->
            val share = durations.of(status).total_seconds.toFloat() / tracked
            if (share >= 0.005f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(share)
                        .background(statusUi(status).second)
                )
            }
        }
    }
}

@Composable
private fun StatusCountTile(
    label: String,
    count: Int,
    duration: String?,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // A status with nothing in it is dimmed rather than coloured in: four full-strength
    // tiles where only two have sessions behind them reads as four kinds of activity.
    val hasSessions = count > 0
    val tint = if (hasSessions) color else Color(0xFFBDBDBD)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) tint.copy(alpha = 0.18f) else Color(0xFFF5F5F5)
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) tint else Color(0xFFE6E6E6),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(enabled = hasSessions || isSelected, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$count",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = tint
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            maxLines = 1,
            style = TextStyle(
                fontWeight = FontWeight.SemiBold
            ),
            color = if (hasSessions) Color(0xFF555555) else Color.Gray
        )
        // How long the day spent in this status, under the number of times it did.
        // Tinted like the count rather than grey: the two belong together — four
        // journeys, six and three quarter hours — and a grey line under a coloured
        // number reads as a caption for the tile instead of half of its content.
        //
        // An em dash rather than nothing when there is none: a tile that keeps its
        // height whether or not the status happened leaves all four on one baseline.
        Spacer(Modifier.height(3.dp))
        Text(
            text = duration ?: "—",
            fontSize = 12.sp,
            maxLines = 1,
            fontWeight = if (duration != null) FontWeight.SemiBold else FontWeight.Normal,
            color = if (duration != null) tint else Color(0xFFCCCCCC)
        )
    }
}




/**
 * One session as a two-stop timeline: when and where it started, when and where it
 * ended.
 *
 * Both ends are always shown, whatever the status — a parked session's two rows carry
 * the same place on purpose, which is the fact that the vehicle did not move. Times
 * come from the payload as sent: the response declares its own timezone and the stamps
 * are already in it.
 */
@Composable
fun StatusSessionCard(
    leg: SessionLeg,
    startAddress: String? = null,
    endAddress: String? = null
) {
    val session = leg.session
    val (statusText, statusColor) = statusUi(session.status)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // ---------- STATUS HEADER ----------
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(statusColor, CircleShape)
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = statusText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor
                )

                // The server inferred this one from a hole in the data rather than
                // reading it off the tracker — twelve seconds of lost signal, not a
                // stop. Said out loud so the row is not read as a journey.
                if (session.offline_inferred) {
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFEFEFEF)
                    ) {
                        Text(
                            text = "Signal gap",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = session.durationLabel(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ---------- START → END ----------
            // One stop per end, each with its own clock and its own address. A single
            // row showing only where the session finished left "where did this drive
            // begin?" to be answered by scrolling to the card below it.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF7F7F7), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Column {
                    SessionEndRow(
                        label = "Start",
                        clock = session.startClock(),
                        address = startAddress,
                        point = leg.start,
                        // The day's first journey has no session in front of it to
                        // inherit a start from — yesterday's report holds that one. Said
                        // plainly rather than repeating the end address as if the drive
                        // had begun where it finished.
                        missingNote = if (leg.startSource == LegStart.UNKNOWN)
                            "Start location not reported" else null,
                        dotColor = statusColor,
                        hasConnector = true
                    )

                    SessionEndRow(
                        label = "End",
                        clock = session.endClock(),
                        address = endAddress,
                        point = leg.end,
                        missingNote = null,
                        dotColor = statusColor,
                        hasConnector = false
                    )
                }
            }

            // A session that ran past midnight — the overnight one every report opens
            // with — would otherwise claim to have ended before it began.
            if (session.spansDays()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Ended the next day",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // Both ends in one place: the vehicle was recorded here and did not leave.
            // Said once, rather than leaving the reader to compare two address lines
            // word by word — and only when both ends are actually known, so a journey
            // with an unreported start is never labelled as having gone nowhere.
            if (leg.stayedPut) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "No movement between these times",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

/**
 * One end of a session: its clock time over its address, with a dot in the margin and
 * a line down to the next stop.
 *
 * The coordinate is shown while the geocoder is still working and kept if it fails:
 * "Fetching location…" that never resolves tells the user nothing, and a tracker's own
 * numbers are at least the truth. An end with no fix at all says so.
 */
@Composable
private fun SessionEndRow(
    label: String,
    clock: String,
    address: String?,
    point: Pair<Double, Double>?,
    missingNote: String?,
    dotColor: Color,
    hasConnector: Boolean
) {
    Row(modifier = Modifier.fillMaxWidth()) {

        // The dot column, and the thread between the two stops.
        Column(
            modifier = Modifier.width(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(5.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(dotColor, CircleShape)
            )
            if (hasConnector) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(Color(0xFFD8D8D8))
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.width(34.dp)
                )
                Text(
                    text = clock,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1B1B1B)
                )
            }

            Spacer(Modifier.height(2.dp))

            Text(
                text = address
                    ?: point?.let { "${format5(it.first)}, ${format5(it.second)}" }
                    ?: missingNote
                    ?: "No location recorded",
                fontSize = 14.sp,
                color = if (address != null) Color(0xFF444444) else Color.Gray,
                maxLines = 2,
                modifier = Modifier.padding(start = 34.dp, bottom = if (hasConnector) 8.dp else 0.dp)
            )
        }
    }
}

/** Five decimals — about a metre, and the precision the tracker actually sends. */
private fun format5(value: Double): String {
    val rounded = (value * 100_000).roundToInt() / 100_000.0
    return rounded.toString()
}

/**
 * The label and the colour this screen shows a status in: Running green, Idle yellow,
 * Parked red, Offline grey.
 *
 * Both come from the shared vocabulary ([statusDisplayOf], [statusColorOf]) rather than
 * from a `when` of their own, for two reasons.
 *
 * The colours have to be the app's one set. Offline was being drawn **black** here and
 * grey everywhere else, and Running was a muted olive against the live map's green — the
 * same vehicle in the same state looked like two different things on two screens.
 *
 * And the match has to go through `statusKindOf`. This screen's payload spells these
 * states several ways — "ideal" for idle, "park", "on"/"off", "stop" — none of which the
 * literal `when` here recognised; they fell through to its `else` and came out grey, next
 * to a label that still read plausibly, so a wrong status did not look broken.
 */
fun statusUi(status: String): Pair<String, Color> =
    statusDisplayOf(status) to Color(statusColorOf(status))


@Composable
fun EmptyState(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.DarkGray
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = subtitle,
            fontSize = 15.sp,
            color = Color.Gray
        )
    }
}
