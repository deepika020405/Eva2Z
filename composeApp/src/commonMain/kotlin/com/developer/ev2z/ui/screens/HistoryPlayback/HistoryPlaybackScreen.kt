// composeApp/src/commonMain/kotlin/com/developer/ev2z/ui/screens/HistoryPlayback/HistoryPlaybackScreen.kt
package com.developer.ev2z.ui.screens.HistoryPlayback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.BackPressHandler
import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.response.Device
import com.developer.ev2z.response.RoutePoint
import kotlinx.datetime.*

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
    val showStartDatePicker by viewModel.showStartDatePicker.collectAsState()
    val showEndDatePicker by viewModel.showEndDatePicker.collectAsState()
    val showDeviceDropdown by viewModel.showDeviceDropdown.collectAsState()
    val showNoDataDialog by viewModel.showNoDataDialog.collectAsState()

    BackPressHandler { onBackPressed() }

    LaunchedEffect(Unit) {
        viewModel.loadDevices()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "History Playback",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (routePoints.isNotEmpty()) {
                HistoryMapView(
                    routePoints = routePoints,
                    currentIndex = currentIndex,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Select device and date range",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                        Text(
                            "to view history playback",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Device Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                            .clickable { viewModel.toggleDeviceDropdown() }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                selectedDevice?.device_name ?: "Select Device",
                                fontSize = 14.sp
                            )
                        }
                        Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray)
                    }

                    Spacer(Modifier.height(12.dp))

                    // ✅ START DATE
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                            .clickable { viewModel.toggleStartDatePicker() }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.DateRange,
                                null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Start Date", fontSize = 11.sp, color = Color.Gray)
                                Text(startDate, fontSize = 14.sp)
                            }
                        }
                        Icon(Icons.Default.Edit, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }

                    Spacer(Modifier.height(12.dp))

                    // ✅ END DATE
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                            .clickable { viewModel.toggleEndDatePicker() }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.DateRange,
                                null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("End Date", fontSize = 11.sp, color = Color.Gray)
                                Text(endDate, fontSize = 14.sp)
                            }
                        }
                        Icon(Icons.Default.Edit, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }

                    if (selectedDevice != null) {
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.fetchHistory() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            enabled = !viewModel.isLoading.collectAsState().value
                        ) {
                            if (viewModel.isLoading.collectAsState().value) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text("Fetch History")
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = routePoints.isNotEmpty(),
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem("Distance", "${totalDistance.toInt()} km")
                            StatItem("Avg Speed", "${averageSpeed.toInt()} km/h")
                            StatItem("Points", "${routePoints.size}")
                        }

                        Spacer(Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.restartPlayback() },
                                enabled = currentIndex > 0
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    "Restart",
                                    tint = if (currentIndex > 0) Color(0xFF4CAF50) else Color.Gray
                                )
                            }

                            Spacer(Modifier.width(16.dp))

                            FloatingActionButton(
                                onClick = { viewModel.togglePlayback() },
                                containerColor = Color(0xFF4CAF50)
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    if (isPlaying) "Pause" else "Play",
                                    tint = Color.White
                                )
                            }

                            Spacer(Modifier.width(16.dp))

                            IconButton(
                                onClick = { viewModel.stopPlayback() },
                                enabled = currentIndex > 0
                            ) {
                                Icon(
                                    Icons.Default.Stop,
                                    "Stop",
                                    tint = if (currentIndex > 0) Color(0xFFF44336) else Color.Gray
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        if (routePoints.isNotEmpty()) {
                            Slider(
                                value = currentIndex.toFloat(),
                                onValueChange = { viewModel.seekTo(it.toInt()) },
                                valueRange = 0f..(routePoints.size - 1).toFloat(),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Point ${currentIndex + 1}",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    "Total ${routePoints.size}",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
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

        // ✅ NO DATA DIALOG
        if (showNoDataDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissNoDataDialog() },
                icon = {
                    Icon(Icons.Default.Info, null, tint = Color(0xFF4CAF50))
                },
                title = {
                    Text("No History Found")
                },
                text = {
                    Text("No route data available for the selected date range. Please try different dates.")
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissNoDataDialog() }) {
                        Text("OK")
                    }
                }
            )
        }

        if (showDeviceDropdown) {
            DeviceDropdownDialog(
                devices = devices,
                onDeviceSelected = { viewModel.selectDevice(it) },
                onDismiss = { viewModel.toggleDeviceDropdown() }
            )
        }

        if (showStartDatePicker) {
            DateRangePickerDialog(
                title = "Select Start Date",
                selectedDate = startDate,
                minDate = viewModel.minSelectableDate,
                maxDate = viewModel.maxSelectableDate,
                onDateSelected = { viewModel.selectStartDate(it) },
                onDismiss = { viewModel.toggleStartDatePicker() }
            )
        }

        if (showEndDatePicker) {
            DateRangePickerDialog(
                title = "Select End Date",
                selectedDate = endDate,
                minDate = viewModel.minSelectableDate,
                maxDate = viewModel.maxSelectableDate,
                onDateSelected = { viewModel.selectEndDate(it) },
                onDismiss = { viewModel.toggleEndDatePicker() }
            )
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

@Composable
fun DeviceDropdownDialog(
    devices: List<Device>,
    onDeviceSelected: (Device) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Device") },
        text = {
            if (devices.isEmpty()) {
                Text("No devices found", color = Color.Gray)
            } else {
                LazyColumn {
                    items(devices) { device ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDeviceSelected(device) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
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
                            Text(device.device_name, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
    val datePickerState = rememberDatePickerState(
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
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
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