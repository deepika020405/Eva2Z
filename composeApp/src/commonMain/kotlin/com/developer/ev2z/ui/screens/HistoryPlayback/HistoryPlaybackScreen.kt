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
import com.developer.ev2z.response.Device
import com.developer.ev2z.response.RoutePoint
import eva2z_connect_multiplatform.composeapp.generated.resources.Res
import eva2z_connect_multiplatform.composeapp.generated.resources.app_logo
import kotlinx.datetime.*
import org.jetbrains.compose.resources.painterResource
import kotlin.time.ExperimentalTime

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
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentIndex by viewModel.currentPlaybackIndex.collectAsState()
    val totalDistance by viewModel.totalDistance.collectAsState()
    val averageSpeed by viewModel.averageSpeed.collectAsState()
    val showDeviceDropdown by viewModel.showDeviceDropdown.collectAsState()
    val showNoDataDialog by viewModel.showNoDataDialog.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }

    PlatformBackHandler(enabled = true) {
        onBackPressed()
    }

    LaunchedEffect(Unit) {
        viewModel.loadDevices()
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

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF9F9F9))
        ) {
            if (routePoints.isNotEmpty()) {
                HistoryMapView(
                    routePoints = routePoints,
                    currentIndex = currentIndex,
                    modifier = Modifier.fillMaxSize()
                )
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
            onBackClick = onBackPressed,
            onDropdownClick = { viewModel.toggleDeviceDropdown() }
        )

        HistoryPlaybackBottomSheet(
            hasData = routePoints.isNotEmpty(),
            selectedDevice = selectedDevice,
            startDate = startDate,
            endDate = endDate,
            totalDistance = totalDistance,
            averageSpeed = averageSpeed,
            isPlaying = isPlaying,
            onPreviousDate = { viewModel.navigateToPreviousDate() },
            onNextDate = { viewModel.navigateToNextDate() },
            onDateClick = { showDatePicker = true },
            onPlayClick = { viewModel.togglePlayback() }
        )

        // ✅ DROPDOWN (HOME SCREEN STYLE - FIXED)
        if (showDeviceDropdown) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x55000000))
                    .zIndex(999f)
                    .clickable { viewModel.toggleDeviceDropdown() },
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
                                    .clickable { viewModel.toggleDeviceDropdown() }
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
                                    val statusColor = when (device.engine_status.lowercase()) {
                                        "moving", "running" -> Color(0xFF86b071)  // Green
                                        "idle", "ideal" -> Color(0xFFfcd158)      // Yellow
                                        "parked" -> Color(0xFFfa7575)             // Red
                                        "offline" -> Color(0xFFe8e8e8)            // Gray
                                        else -> Color(0xFFe8e8e8)                 // Default Gray
                                    }

                                    // ✅ FIXED: Display text mapping
                                    val statusText = when (device.engine_status.lowercase()) {
                                        "moving" -> "Running"
                                        "running" -> "Running"
                                        "idle" -> "Idle"
                                        "ideal" -> "Idle"
                                        "parked" -> "Parked"
                                        "offline" -> "Offline"
                                        else -> device.engine_status.replaceFirstChar {
                                            if (it.isLowerCase()) it.titlecase() else it.toString()
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.selectDevice(device)
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
            AlertDialog(
                onDismissRequest = { viewModel.dismissNoDataDialog() },
                icon = { Icon(Icons.Default.Info, null, tint = Color(0xFF7CB342)) },
                title = { Text("No History Found") },
                text = { Text("No route data available for the selected date range. Please try a different date range.") },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissNoDataDialog() }) { Text("OK") }
                }
            )
        }

        if (uiState is HistoryPlaybackUiState.Loading) {
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
fun BoxScope.TopDeviceBoxWithData(
    deviceCount: Int,
    selectedDevice: String?,
    onBackClick: () -> Unit,
    onDropdownClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 35.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
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
                    Text(selectedDevice ?: "Select Device", fontSize = 15.sp)
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
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
            .padding(12.dp),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = Color.White,
        tonalElevation = 6.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 16.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFD0D0D0))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onPreviousDate,
                    modifier = Modifier.size(40.dp),
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
                        fontSize = 18.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                } else {
                    TextButton(onClick = onDateClick) {
                        Text(
                            text = formatDateRange(startDate, endDate),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black,
                            fontSize = 18.sp
                        )
                        Icon(Icons.Default.ArrowDropDown, "selector")
                    }
                }

                IconButton(
                    onClick = onNextDate,
                    modifier = Modifier.size(40.dp),
                    enabled = hasData && selectedDevice != null
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Next",
                        tint = if (hasData && selectedDevice != null) Color.Black else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .background(Color(0xFFF3F6F1), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Distance", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (hasData) "${totalDistance.toInt()}km" else "0km",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .background(Color(0xFFF3F6F1), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Average Speed", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (hasData) "${averageSpeed.toInt()} km" else "0 km",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }

                Button(
                    onClick = onPlayClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasData) Color(0xFF7CB342) else Color.Gray
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp),
                    enabled = hasData
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        var statePlay = if (!isPlaying) "Play History" else "Pause "
                        Text(statePlay, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("1X", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
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
    val dateRangePickerState = rememberDateRangePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val instant = Instant.fromEpochMilliseconds(utcTimeMillis)
                val date = instant.toLocalDateTime(TimeZone.UTC).date
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
                        val start = Instant.fromEpochMilliseconds(startMillis)
                            .toLocalDateTime(TimeZone.UTC).date
                        val end = Instant.fromEpochMilliseconds(endMillis)
                            .toLocalDateTime(TimeZone.UTC).date

                        onDatesSelected(
                            minOf(start, end).toString(),
                            maxOf(start, end).toString()
                        )
                        onDismiss()
                    }
                    startMillis != null -> {
                        val date = Instant.fromEpochMilliseconds(startMillis)
                            .toLocalDateTime(TimeZone.UTC).date
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

@Composable
expect fun HistoryMapView(
    routePoints: List<RoutePoint>,
    currentIndex: Int,
    modifier: Modifier
)