// composeApp/src/commonMain/kotlin/com/developer/ev2z/ui/screens/HistoryPlayback/HistoryPlaybackScreen.kt
package com.developer.ev2z.ui.screens.HistoryPlayback

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.response.Device
import com.developer.ev2z.response.RoutePoint
import eva2z_connect_multiplatform.composeapp.generated.resources.Res
import eva2z_connect_multiplatform.composeapp.generated.resources.app_logo
import kotlinx.datetime.*
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
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

    BackPressHandler { onBackPressed() }

    LaunchedEffect(Unit) {
        viewModel.loadDevices()
    }

    // AUTO-FETCH when device selected
    LaunchedEffect(selectedDevice) {
        if (selectedDevice != null) {
            viewModel.fetchHistory()
        }
    }

    // ✅ FIX PROBLEM 3: Refetch when date changes
    LaunchedEffect(startDate, endDate) {
        if (selectedDevice != null && startDate.isNotEmpty() && endDate.isNotEmpty()) {
            viewModel.fetchHistory()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // MAP BACKGROUND
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

        // TOP BAR
        TopDeviceBoxWithData(
            deviceCount = devices.size,
            selectedDevice = selectedDevice?.device_name,
            onBackClick = onBackPressed,
            onDropdownClick = { viewModel.toggleDeviceDropdown() }
        )

        // BOTTOM SHEET
        HistoryPlaybackBottomSheet(
            hasData = routePoints.isNotEmpty(),
            selectedDevice = selectedDevice,
            startDate = startDate,
            endDate = endDate,
            totalDistance = totalDistance,
            averageSpeed = averageSpeed,
            isPlaying = isPlaying,
            onPreviousDate = {
                viewModel.navigateToPreviousDate()
            },
            onNextDate = {
                viewModel.navigateToNextDate()
            },
            onDateClick = { showDatePicker = true },
            onPlayClick = { viewModel.togglePlayback() }
        )

        // DEVICE DROPDOWN
        if (showDeviceDropdown) {
            DeviceSelectorPanel(
                devices = devices,
                onDeviceSelected = { device ->
                    viewModel.selectDevice(device)
                },
                onDismiss = { viewModel.toggleDeviceDropdown() }
            )
        }

        // DATE PICKER
        if (showDatePicker) {
            DateRangePickerDialog(
                title = "Select Date",
                selectedDate = startDate,
                minDate = viewModel.minSelectableDate,
                maxDate = viewModel.maxSelectableDate,
                onDateSelected = {
                    viewModel.selectStartDate(it)
                    viewModel.selectEndDate(it)
                },
                onDismiss = { showDatePicker = false }
            )
        }

        // NO DATA DIALOG
        if (showNoDataDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissNoDataDialog() },
                icon = { Icon(Icons.Default.Info, null, tint = Color(0xFF7CB342)) },
                title = { Text("No History Found") },
                text = { Text("No route data available for the selected date. Please try a different date.") },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissNoDataDialog() }) { Text("OK") }
                }
            )
        }

        // LOADING OVERLAY
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

                // ✅ FIX PROBLEM 1: Show "Please Select Device" - NOT CLICKABLE
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
                        Text("Play History", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
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

@Composable
fun DeviceSelectorPanel(
    devices: List<Device>,
    onDeviceSelected: (Device) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x55000000))
            .zIndex(999f)
            .clickable { onDismiss() },
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(top = 140.dp)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Select Device", fontSize = 16.sp, color = Color.Gray)
                Spacer(Modifier.height(16.dp))

                if (devices.isEmpty()) {
                    Text("No devices found", color = Color.Gray)
                } else {
                    devices.forEach { device ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // ✅ FIX PROBLEM 2: Call both device selection AND dismiss
                                    onDeviceSelected(device)
                                    onDismiss()
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .background(
                                        when (device.status) {
                                            "moving" -> Color(0xFF4CAF50)
                                            "idle" -> Color(0xFFFFC107)
                                            else -> Color(0xFFF44336)
                                        },
                                        CircleShape
                                    )
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(device.device_name, fontSize = 16.sp)
                                Text("ID: ${device.device_id}", fontSize = 12.sp, color = Color.Gray, maxLines = 1)
                            }
                            Text(
                                device.status ?: "Unknown",
                                color = when (device.status) {
                                    "moving" -> Color(0xFF4CAF50)
                                    "idle" -> Color(0xFFFFC107)
                                    else -> Color(0xFFF44336)
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
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
            Month.JANUARY -> "Jan"
            Month.FEBRUARY -> "Feb"
            Month.MARCH -> "Mar"
            Month.APRIL -> "Apr"
            Month.MAY -> "May"
            Month.JUNE -> "Jun"
            Month.JULY -> "Jul"
            Month.AUGUST -> "Aug"
            Month.SEPTEMBER -> "Sep"
            Month.OCTOBER -> "Oct"
            Month.NOVEMBER -> "Nov"
            Month.DECEMBER -> "Dec"
            else -> ""
        }
        "$dayOfWeek, ${start.dayOfMonth} $monthName ${start.year}"
    } catch (e: Exception) {
        startDate
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(
    title: String,
    selectedDate: String,
    minDate: LocalDate,
    maxDate: LocalDate,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // ✅ FIXED: Set initial selected date in calendar
    val initialSelectedDateMillis = try {
        val date = LocalDate.parse(selectedDate)
        date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
    } catch (e: Exception) {
        null
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialSelectedDateMillis,
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
                datePickerState.selectedDateMillis?.let { millis ->
                    val instant = Instant.fromEpochMilliseconds(millis)
                    val date = instant.toLocalDateTime(TimeZone.UTC).date
                    onDateSelected(date.toString())
                }
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        DatePicker(
            state = datePickerState,
            title = { Text(title, modifier = Modifier.padding(16.dp)) }
        )
    }
}

@Composable
expect fun HistoryMapView(
    routePoints: List<RoutePoint>,
    currentIndex: Int,
    modifier: Modifier
)