// composeApp/src/commonMain/kotlin/com/developer/ev2z/ui/screens/HistoryPlayback/HistoryPlaybackScreen.kt
package com.developer.ev2z.ui.screens.HistoryPlayback

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.developer.ev2z.BackPressHandler
import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.network.SnappedRoute
import com.developer.ev2z.repository.HistoryPlaybackRepository
import com.developer.ev2z.response.Device
import com.developer.ev2z.response.RoutePoint
import eva2z_connect_multiplatform.composeapp.generated.resources.Res
import eva2z_connect_multiplatform.composeapp.generated.resources.app_logo
import kotlinx.datetime.*
import org.jetbrains.compose.resources.painterResource
import kotlin.time.ExperimentalTime
import com.developer.ev2z.theme.AppPageBg
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class, kotlin.time.ExperimentalTime::class)
@Composable
fun HistoryPlaybackScreen(
    apiService: ApiService,
    storageManager: StorageManager,
    onBackPressed: () -> Unit = {}
) {
    val viewModel = remember {
        HistoryPlaybackViewModel(apiService, storageManager)
    }

    val uiState by viewModel.uiState.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val startDate by viewModel.startDate.collectAsState()
    val endDate by viewModel.endDate.collectAsState()
    val routePoints by viewModel.routePoints.collectAsState()
    val snappedRoute by viewModel.snappedRoute.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentIndex by viewModel.currentPlaybackIndex.collectAsState()
    val totalDistance by viewModel.totalDistance.collectAsState()
    val averageSpeed by viewModel.averageSpeed.collectAsState()
    val showDeviceDropdown by viewModel.showDeviceDropdown.collectAsState()
    val showNoDataDialog by viewModel.showNoDataDialog.collectAsState()
    val currentSpeed by viewModel.currentSpeed.collectAsState()
    val vehicleSlug by viewModel.vehicleSlug.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }

    PlatformBackHandler(enabled = true) {
        onBackPressed()
    }

    LaunchedEffect(Unit) {
        viewModel.loadDevices()
    }

    LaunchedEffect(devices) {
        if (devices.isNotEmpty() && selectedDevice == null) {
            viewModel.selectDevice(devices.first())
        }
    }

    LaunchedEffect(selectedDevice) {
        if (selectedDevice != null) {
            viewModel.fetchHistory()
        }
    }

    LaunchedEffect(startDate, endDate) {
        if (selectedDevice != null && startDate.isNotEmpty() && endDate.isNotEmpty()) {
            viewModel.fetchHistory()
        }
    }

    HistoryPlaybackContent(
        devices = devices,
        selectedDevice = selectedDevice,
        routePoints = routePoints,
        snappedRoute = snappedRoute,
        currentIndex = currentIndex,
        startDate = startDate,
        endDate = endDate,
        totalDistance = totalDistance,
        averageSpeed = averageSpeed,
        currentSpeed = currentSpeed,
        isPlaying = isPlaying,
        isLoading = uiState is HistoryPlaybackUiState.Loading,
        showDeviceDropdown = showDeviceDropdown,
        onBackClick = onBackPressed,
        onDropdownClick = {
            if (devices.size > 1) {
                viewModel.toggleDeviceDropdown()
            }
        },
        onDismissDropdown = { viewModel.toggleDeviceDropdown() },
        onDeviceSelected = { viewModel.selectDevice(it) },
        onPreviousDate = { viewModel.navigateToPreviousDate() },
        onNextDate = { viewModel.navigateToNextDate() },
        onDateClick = { showDatePicker = true },
        onPlayClick = { viewModel.togglePlayback() },
        vehicleSlug = vehicleSlug
    )

    if (showDatePicker) {
        DateRangePickerDialog(
            startDate = startDate,
            endDate = endDate,
            minDate = viewModel.minSelectableDate,
            maxDate = viewModel.maxSelectableDate,
            onDatesSelected = { start, end ->
                viewModel.selectDateRange(start, end)
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showNoDataDialog) {
        NoHistoryDialog(onDismiss = { viewModel.dismissNoDataDialog() })
    }
}

/**
 * Stateless UI for the playback screen — every value comes in as a parameter so the
 * layout can be rendered by `@Preview` without an ApiService, StorageManager or ViewModel.
 *
 * [mapContent] is a slot because [HistoryMapView] is an `expect` function with no
 * commonMain implementation; previews pass a placeholder instead.
 */
@Composable
fun HistoryPlaybackContent(
    devices: List<Device>,
    selectedDevice: Device?,
    routePoints: List<RoutePoint>,
    snappedRoute: SnappedRoute = SnappedRoute.EMPTY,
    currentIndex: Int,
    startDate: String,
    endDate: String,
    totalDistance: Double,
    averageSpeed: Double,
    currentSpeed: Int,
    isPlaying: Boolean,
    isLoading: Boolean,
    showDeviceDropdown: Boolean,
    onBackClick: () -> Unit,
    onDropdownClick: () -> Unit,
    onDismissDropdown: () -> Unit,
    onDeviceSelected: (Device) -> Unit,
    onPreviousDate: () -> Unit,
    onNextDate: () -> Unit,
    onDateClick: () -> Unit,
    onPlayClick: () -> Unit,
    // Defaulted so previews and tests can build this screen without one; the live
    // screen passes the slug the history response reported.
    vehicleSlug: String? = null,
    mapContent: @Composable (Modifier) -> Unit = { mapModifier ->
        HistoryMapView(
            routePoints = routePoints,
            snappedRoute = snappedRoute,
            currentIndex = currentIndex,
            modifier = mapModifier,
            vehicleSlug = vehicleSlug
        )
    }
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppPageBg)
        ) {
            if (routePoints.isNotEmpty()) {
                mapContent(Modifier.fillMaxSize())
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.LocationOn,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Select device to view playback", color = Color.Gray, fontSize = 16.sp)
                    }
                }
            }
        }

        TopDeviceBoxWithData(
            deviceCount = devices.size,
            selectedDevice = selectedDevice?.device_name,
            onBackClick = onBackClick,
            onDropdownClick = onDropdownClick
        )

        HistoryPlaybackBottomSheet(
            hasData = routePoints.isNotEmpty(),
            selectedDevice = selectedDevice,
            startDate = startDate,
            endDate = endDate,
            totalDistance = totalDistance,
            averageSpeed = averageSpeed,
            currentSpeed = currentSpeed,
            isPlaying = isPlaying,
            onPreviousDate = onPreviousDate,
            onNextDate = onNextDate,
            onDateClick = onDateClick,
            onPlayClick = onPlayClick
        )

        // ✅ DROPDOWN (HOME SCREEN STYLE - FIXED)
        if (showDeviceDropdown && devices.size > 1) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x55000000))
                    .zIndex(999f)
                    .clickable { onDismissDropdown() },
                contentAlignment = Alignment.TopCenter
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(top = 140.dp)
                        .heightIn(max = 500.dp)
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Select device", fontSize = 14.sp, color = Color.Gray)
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { onDismissDropdown() }
                            )
                        }

                        HorizontalDivider()

                        if (devices.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No devices found", color = Color.Gray)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                items(devices) { device ->
                                    // ✅ FIXED: Using engine_status correctly with proper color mapping
                                    val displayStatus = (device.status ?: device.engine_status).lowercase()
                                    val statusColor = when (displayStatus) {
                                        "moving", "running" -> Color(0xFF86b071)  // Green
                                        "idle", "ideal"     -> Color(0xFFfcd158)  // Yellow
                                        "parked"            -> Color(0xFFfa7575)  // Red
                                        else                -> Color(0xFFe8e8e8)  // Gray (offline/unknown/no_data)
                                    }
                                    val statusText = when (displayStatus) {
                                        "moving", "running" -> "Running"
                                        "idle", "ideal"     -> "Idle"
                                        "parked"            -> "Parked"
                                        "offline"           -> "Offline"
                                        "no_data"           -> "No Data"
                                        else                -> "Offline"
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onDeviceSelected(device)
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // ✅ STATUS DOT
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(statusColor, CircleShape)
                                        )

                                        Spacer(Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                device.device_name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                "Device ID: ${device.device_id}",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }

                                        // ✅ STATUS TEXT
                                        Text(
                                            text = statusText,
                                            fontSize = 12.sp,
                                            color = statusColor,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

@Composable
fun NoHistoryDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Info, null, tint = Color(0xFF7CB342)) },
        title = { Text("No History Found") },
        text = { Text("No route data available for the selected date range. Please try a different date range.") },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}

@Composable
fun BoxScope.TopDeviceBoxWithData(
    deviceCount: Int,
    selectedDevice: String?,
    onBackClick: () -> Unit,
    onDropdownClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
            .align(Alignment.TopCenter),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.clickable { onBackClick() }
                )
                Spacer(Modifier.width(10.dp))
                Text("Playback History", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                if (deviceCount > 0) {
                    Text("$deviceCount Devices", fontSize = 13.sp, color = Color.Gray)
                }
            }

            Spacer(Modifier.height(14.dp))

            if (deviceCount > 1) {

                // 🔽 Multiple devices → clickable dropdown
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                        .clickable { onDropdownClick() }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(Res.drawable.app_logo),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = selectedDevice ?: "Select Device",
                            fontSize = 15.sp,
                            maxLines = 1

                        )
                    }

                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }

            } else if (deviceCount == 1) {

                // 🔹 Single device → no dropdown, no click
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(Res.drawable.app_logo),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(selectedDevice ?: "", fontSize = 15.sp)
                }

            } else {

                // 🔹 No device case
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("No Device Found", fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun BoxScope.HistoryPlaybackBottomSheet(
    hasData: Boolean,
    selectedDevice: Device?,
    startDate: String,
    endDate: String,
    totalDistance: Double,
    averageSpeed: Double,
    currentSpeed: Int = 0,
    isPlaying: Boolean,
    onPreviousDate: () -> Unit,
    onNextDate: () -> Unit,
    onDateClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = Color.White,
        tonalElevation = 6.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFD0D0D0))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onPreviousDate,
                    modifier = Modifier.size(36.dp),
                    enabled = hasData && selectedDevice != null
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Previous",
                        tint = if (hasData && selectedDevice != null) Color.Black else Color.Gray
                    )
                }

                if (selectedDevice == null) {
                    Text(
                        text = "Please Select Device",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        maxLines = 1
                    )
                } else {
                    TextButton(
                        onClick = onDateClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = formatDateRange(startDate, endDate),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black,
                            fontSize = 15.sp,
                            maxLines = 1
                        )
                        Icon(Icons.Default.ArrowDropDown, "selector")
                    }
                }

                IconButton(
                    onClick = onNextDate,
                    modifier = Modifier.size(36.dp),
                    enabled = hasData && selectedDevice != null
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Next",
                        tint = if (hasData && selectedDevice != null) Color.Black else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stats row — 3 stat boxes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatBox(
                    label = "Distance",
                    value = if (hasData) "${totalDistance.toInt()} km" else "0 km",
                    bgColor = Color(0xFFF3F6F1),
                    modifier = Modifier.weight(1f)
                )

                StatBox(
                    label = "Avg Speed",
                    value = if (hasData) "${averageSpeed.toInt()} km/h" else "0 km/h",
                    bgColor = Color(0xFFF3F6F1),
                    modifier = Modifier.weight(1f)
                )

                StatBox(
                    label = "Speed",
                    value = if (isPlaying) "$currentSpeed km/h" else "0 km/h",
                    bgColor = if (isPlaying && currentSpeed > 0) Color(0xFFFFF3E0) else Color(0xFFF3F6F1),
                    valueColor = if (isPlaying && currentSpeed > 0) Color(0xFFE65100) else Color.Black,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Play button — full width
            Button(
                onClick = onPlayClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasData) Color(0xFF7CB342) else Color.Gray
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .height(48.dp),
                enabled = hasData
            ) {
                val statePlay = if (!isPlaying) "Play History" else "Pause"
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(statePlay, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(6.dp))
                Text("1X", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    bgColor: Color,
    valueColor: Color = Color.Black,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .background(bgColor, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 10.sp, color = Color.Gray, maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1
            )
        }
    }
}

fun formatDateRange(startDate: String, endDate: String): String {
    return try {
        val start = LocalDate.parse(startDate)
        val end = LocalDate.parse(endDate)

        if (startDate == endDate) {
            val dayOfWeek = when (start.dayOfWeek) {
                DayOfWeek.MONDAY -> "Mon"
                DayOfWeek.TUESDAY -> "Tue"
                DayOfWeek.WEDNESDAY -> "Wed"
                DayOfWeek.THURSDAY -> "Thu"
                DayOfWeek.FRIDAY -> "Fri"
                DayOfWeek.SATURDAY -> "Sat"
                DayOfWeek.SUNDAY -> "Sun"
                else -> ""
            }
            val monthName = when (start.month) {
                Month.JANUARY -> "Jan"; Month.FEBRUARY -> "Feb"; Month.MARCH -> "Mar"
                Month.APRIL -> "Apr"; Month.MAY -> "May"; Month.JUNE -> "Jun"
                Month.JULY -> "Jul"; Month.AUGUST -> "Aug"; Month.SEPTEMBER -> "Sep"
                Month.OCTOBER -> "Oct"; Month.NOVEMBER -> "Nov"; Month.DECEMBER -> "Dec"
                else -> ""
            }
            "$dayOfWeek, ${start.dayOfMonth} $monthName ${start.year}"
        } else {
            val startMonth = when (start.month) {
                Month.JANUARY -> "Jan"; Month.FEBRUARY -> "Feb"; Month.MARCH -> "Mar"
                Month.APRIL -> "Apr"; Month.MAY -> "May"; Month.JUNE -> "Jun"
                Month.JULY -> "Jul"; Month.AUGUST -> "Aug"; Month.SEPTEMBER -> "Sep"
                Month.OCTOBER -> "Oct"; Month.NOVEMBER -> "Nov"; Month.DECEMBER -> "Dec"
                else -> ""
            }
            val endMonth = when (end.month) {
                Month.JANUARY -> "Jan"; Month.FEBRUARY -> "Feb"; Month.MARCH -> "Mar"
                Month.APRIL -> "Apr"; Month.MAY -> "May"; Month.JUNE -> "Jun"
                Month.JULY -> "Jul"; Month.AUGUST -> "Aug"; Month.SEPTEMBER -> "Sep"
                Month.OCTOBER -> "Oct"; Month.NOVEMBER -> "Nov"; Month.DECEMBER -> "Dec"
                else -> ""
            }
            "${start.dayOfMonth} $startMonth - ${end.dayOfMonth} $endMonth ${end.year}"
        }
    } catch (e: Exception) {
        "$startDate - $endDate"
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun DateRangePickerDialog(
    startDate: String,
    endDate: String,
    minDate: LocalDate,
    maxDate: LocalDate,
    onDatesSelected: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
//    val dateRangePickerState = rememberDateRangePickerState(
//        selectableDates = object : SelectableDates {
//            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
//                val instant = Instant.fromEpochMilliseconds(utcTimeMillis)
////                val date = instant.toLocalDateTime(TimeZone.UTC).date
//                val date = instant
//                    .toLocalDateTime(TimeZone.currentSystemDefault())
//                    .date
//
//                return date in minDate..maxDate
//            }
//        }
//    )

    val dateRangePickerState = rememberDateRangePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = Instant
                    .fromEpochMilliseconds(utcTimeMillis)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date

                return date in minDate..maxDate
            }
        }
    )


    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val startMillis = dateRangePickerState.selectedStartDateMillis
                val endMillis = dateRangePickerState.selectedEndDateMillis

                when {
                    startMillis != null && endMillis != null -> {
//                        val start = Instant.fromEpochMilliseconds(startMillis)
//                            .toLocalDateTime(TimeZone.UTC).date
                        val start = Instant.fromEpochMilliseconds(startMillis)
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .date

                        val end = Instant.fromEpochMilliseconds(endMillis)
                            .toLocalDateTime(TimeZone.currentSystemDefault()).date

                        onDatesSelected(
                            minOf(start, end).toString(),
                            maxOf(start, end).toString()
                        )
                        onDismiss()
                    }
                    startMillis != null -> {
                        val date = Instant.fromEpochMilliseconds(startMillis)
                            .toLocalDateTime(TimeZone.currentSystemDefault()).date
                        onDatesSelected(date.toString(), date.toString())
                        onDismiss()
                    }
                }
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        colors = DatePickerDefaults.colors(
            selectedDayContainerColor = Color(0xFF7CB342),
            selectedDayContentColor = Color.White,
            todayContentColor = Color(0xFF7CB342),
            todayDateBorderColor = Color(0xFF7CB342),
            dayInSelectionRangeContainerColor = Color(0xFFD9ECCF),
            dayInSelectionRangeContentColor = Color.Black
        )
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            title = {
                Text(
                    text = "Select date",
                    modifier = Modifier.padding(16.dp)
                )
            },
            headline = {
                val start = dateRangePickerState.selectedStartDateMillis
                val end = dateRangePickerState.selectedEndDateMillis

                val text = when {
                    start != null && end != null -> {
                        val startDate = Instant.fromEpochMilliseconds(start)
                            .toLocalDateTime(TimeZone.UTC).date
                        val endDate = Instant.fromEpochMilliseconds(end)
                            .toLocalDateTime(TimeZone.UTC).date
                        formatShortDate(startDate) + " - " + formatShortDate(endDate)
                    }
                    start != null -> {
                        val date = Instant.fromEpochMilliseconds(start)
                            .toLocalDateTime(TimeZone.UTC).date
                        formatShortDate(date)
                    }
                    else -> ""
                }

                if (text.isNotEmpty()) {
                    Text(
                        text = text,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.headlineLarge
                    )
                }
            }
        )
    }
}

fun formatShortDate(date: LocalDate): String {
    val dayOfWeek = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "Mon"
        DayOfWeek.TUESDAY -> "Tue"
        DayOfWeek.WEDNESDAY -> "Wed"
        DayOfWeek.THURSDAY -> "Thu"
        DayOfWeek.FRIDAY -> "Fri"
        DayOfWeek.SATURDAY -> "Sat"
        DayOfWeek.SUNDAY -> "Sun"
        else -> ""
    }
    val monthName = when (date.month) {
        Month.JANUARY -> "Jan"; Month.FEBRUARY -> "Feb"; Month.MARCH -> "Mar"
        Month.APRIL -> "Apr"; Month.MAY -> "May"; Month.JUNE -> "Jun"
        Month.JULY -> "Jul"; Month.AUGUST -> "Aug"; Month.SEPTEMBER -> "Sep"
        Month.OCTOBER -> "Oct"; Month.NOVEMBER -> "Nov"; Month.DECEMBER -> "Dec"
        else -> ""
    }
    return "$dayOfWeek, $monthName ${date.dayOfMonth}"
}

/**
 * [snappedRoute] is the route laid onto the road network — what the map draws, so the
 * line follows the streets instead of cutting straight between fixes. It arrives a
 * moment after [routePoints] and is empty when snapping is unavailable; in both cases
 * the map falls back to drawing [routePoints] directly.
 */
@Composable
expect fun HistoryMapView(
    routePoints: List<RoutePoint>,
    snappedRoute: SnappedRoute,
    currentIndex: Int,
    modifier: Modifier,
    /**
     * The vehicle the terminal was fitted to, as the slug out of the history
     * response's `vehicle_icon_url` — see `RouteHistoryResponse.vehicleIconSlug`.
     * The marker draws that vehicle in the colour of the fix being played back.
     *
     * Null — a device the server names no vehicle for — falls back to the heading
     * arrow, the same as the live map.
     */
    vehicleSlug: String? = null
)


// =========================================================
// PREVIEWS
//
// These render HistoryPlaybackContent (the stateless UI) with mock data —
// no ApiService / StorageManager / ViewModel involved. The map is a
// placeholder because HistoryMapView is an `expect` function with no
// commonMain implementation.
// =========================================================

private val previewDevices = listOf(
    Device(
        id = 1,
        device_id = "864636051234567",
        device_name = "Tata Ace — MH12 AB 1234",
        engine_status = "running",
        status = "moving"
    ),
    Device(
        id = 2,
        device_id = "864636051234568",
        device_name = "Bolero Pickup — MH14 CD 5678",
        engine_status = "idle",
        status = "idle"
    ),
    Device(
        id = 3,
        device_id = "864636051234569",
        device_name = "Scooter — MH12 XY 9012",
        engine_status = "parked",
        status = "parked"
    ),
    Device(
        id = 4,
        device_id = "864636051234570",
        device_name = "Truck — MH09 ZZ 3456",
        engine_status = "offline",
        status = null
    )
)

private val previewRoutePoints = List(40) { i ->
    RoutePoint(
        latitude = (18.5204 + i * 0.001).toString(),
        longitude = (73.8567 + i * 0.001).toString(),
        speed = (20 + i % 25).toString(),
        createdAt = "2026-10-03 09:${(10 + i % 45).toString().padStart(2, '0')}:00",
        movement = "moving"
    )
}

@Composable
private fun PreviewMapPlaceholder(modifier: Modifier) {
    Box(
        modifier = modifier.background(Color(0xFFE8EDE4)),
        contentAlignment = Alignment.Center
    ) {
        Text("Map preview", color = Color.Gray, fontSize = 13.sp)
    }
}

/** Route loaded, playback paused — the common "happy path" state. */
@Preview(name = "1 · With route data", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun HistoryPlaybackContentPreview() {
    HistoryPlaybackContent(
        devices = previewDevices,
        selectedDevice = previewDevices.first(),
        routePoints = previewRoutePoints,
        currentIndex = 12,
        startDate = "2026-10-03",
        endDate = "2026-10-03",
        totalDistance = 42.0,
        averageSpeed = 36.0,
        currentSpeed = 0,
        isPlaying = false,
        isLoading = false,
        showDeviceDropdown = false,
        onBackClick = {},
        onDropdownClick = {},
        onDismissDropdown = {},
        onDeviceSelected = {},
        onPreviousDate = {},
        onNextDate = {},
        onDateClick = {},
        onPlayClick = {},
        mapContent = { PreviewMapPlaceholder(it) }
    )
}

/** Playing — Speed box turns orange and the button flips to "Pause". */
@Preview(name = "2 · Playing", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun HistoryPlaybackContentPlayingPreview() {
    HistoryPlaybackContent(
        devices = previewDevices,
        selectedDevice = previewDevices.first(),
        routePoints = previewRoutePoints,
        currentIndex = 18,
        startDate = "2026-09-28",
        endDate = "2026-10-03",
        totalDistance = 128.0,
        averageSpeed = 41.0,
        currentSpeed = 54,
        isPlaying = true,
        isLoading = false,
        showDeviceDropdown = false,
        onBackClick = {},
        onDropdownClick = {},
        onDismissDropdown = {},
        onDeviceSelected = {},
        onPreviousDate = {},
        onNextDate = {},
        onDateClick = {},
        onPlayClick = {},
        mapContent = { PreviewMapPlaceholder(it) }
    )
}

/** No device selected yet — disabled controls and the empty-map hint. */
@Preview(name = "3 · Empty state", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun HistoryPlaybackContentEmptyPreview() {
    HistoryPlaybackContent(
        devices = emptyList(),
        selectedDevice = null,
        routePoints = emptyList(),
        currentIndex = 0,
        startDate = "2026-10-03",
        endDate = "2026-10-03",
        totalDistance = 0.0,
        averageSpeed = 0.0,
        currentSpeed = 0,
        isPlaying = false,
        isLoading = false,
        showDeviceDropdown = false,
        onBackClick = {},
        onDropdownClick = {},
        onDismissDropdown = {},
        onDeviceSelected = {},
        onPreviousDate = {},
        onNextDate = {},
        onDateClick = {},
        onPlayClick = {},
        mapContent = { PreviewMapPlaceholder(it) }
    )
}

/** Device picker open — shows all four status colours. */
@Preview(name = "4 · Device dropdown", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun HistoryPlaybackContentDropdownPreview() {
    HistoryPlaybackContent(
        devices = previewDevices,
        selectedDevice = previewDevices.first(),
        routePoints = previewRoutePoints,
        currentIndex = 0,
        startDate = "2026-10-03",
        endDate = "2026-10-03",
        totalDistance = 42.0,
        averageSpeed = 36.0,
        currentSpeed = 0,
        isPlaying = false,
        isLoading = false,
        showDeviceDropdown = true,
        onBackClick = {},
        onDropdownClick = {},
        onDismissDropdown = {},
        onDeviceSelected = {},
        onPreviousDate = {},
        onNextDate = {},
        onDateClick = {},
        onPlayClick = {},
        mapContent = { PreviewMapPlaceholder(it) }
    )
}

/** Loading overlay on top of the screen. */
@Preview(name = "5 · Loading", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun HistoryPlaybackContentLoadingPreview() {
    HistoryPlaybackContent(
        devices = previewDevices,
        selectedDevice = previewDevices.first(),
        routePoints = emptyList(),
        currentIndex = 0,
        startDate = "2026-10-03",
        endDate = "2026-10-03",
        totalDistance = 0.0,
        averageSpeed = 0.0,
        currentSpeed = 0,
        isPlaying = false,
        isLoading = true,
        showDeviceDropdown = false,
        onBackClick = {},
        onDropdownClick = {},
        onDismissDropdown = {},
        onDeviceSelected = {},
        onPreviousDate = {},
        onNextDate = {},
        onDateClick = {},
        onPlayClick = {},
        mapContent = { PreviewMapPlaceholder(it) }
    )
}

/** The "No History Found" dialog on its own. */
@Preview(name = "6 · No history dialog", widthDp = 360, heightDp = 400, showBackground = true)
@Composable
private fun NoHistoryDialogPreview() {
    NoHistoryDialog(onDismiss = {})
}