package com.developer.ev2z.ui.screens.deviceStatus

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.response.MyDevicesResponse
import com.developer.ev2z.response.StatusSession
import eva2z_connect_multiplatform.composeapp.generated.resources.Res
import eva2z_connect_multiplatform.composeapp.generated.resources.app_logo
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.painterResource


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceStatusScreen(onBack: () -> Unit) {

    val viewModel = DeviceStatusViewModelProvider.viewModel

    val devices by viewModel.devices.collectAsState()
    val statusResponse by viewModel.deviceStatus.collectAsState()
    val sessions by viewModel.filteredSessions.collectAsState(emptyList())
    val filter by viewModel.statusFilter.collectAsState()
    val addressMap by viewModel.addressMap.collectAsState()

    PlatformBackHandler(true) { onBack() }

    LaunchedEffect(Unit) {
        viewModel.loadDevices()
    }

    Scaffold(
        containerColor = Color(0xFFF5F5F5), // 👈 screen background
        topBar = {
            TopAppBar(
                title = { Text("Device Status") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
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
                    userName = devices!!.username!!,
                    devices = it,
                    selectedDeviceId = viewModel.selectedDeviceId.collectAsState().value,
                    onDeviceSelect = viewModel::selectDevice,
                    onLocationClick = { /* optional */ }
                )

            }

            Spacer(Modifier.height(12.dp))

            StatusFilters(
                selected = filter,
                onSelect = viewModel::setFilter
            )

//            Spacer(Modifier.height(12.dp))
//
//            LazyColumn {
//                items(sessions) { session ->
//
//                    val lat = session.latitude.toDouble()
//                    val lng = session.longitude.toDouble()
//                    val key = "$lat,$lng"
//
//                    // 🔁 address fetch trigger
//                    LaunchedEffect(key) {
//                        viewModel.resolveAddress(lat, lng)
//                    }
//
//                    StatusSessionCard(
//                        session = session,
//                        address = addressMap[key]
//                    )
//                }
//            }

            Spacer(Modifier.height(12.dp))

            when {
                viewModel.selectedDeviceId.collectAsState().value == null -> {
                    EmptyState(
                        title = "Select a device",
                        subtitle = "Please select a device to view status history"
                    )
                }

                statusResponse?.success == false -> {
                    EmptyState(
                        title = "No data found",
                        subtitle = statusResponse?.message ?: "No status available for today"
                    )
                }

                sessions.isEmpty() -> {
                    EmptyState(
                        title = "No activity",
                        subtitle = "This device has no status records today"
                    )
                }

                else -> {
                    LazyColumn {
                        items(sessions) { session ->

                            val lat = session.latitude.toDouble()
                            val lng = session.longitude.toDouble()
                            val key = "$lat,$lng"

                            LaunchedEffect(key) {
                                viewModel.resolveAddress(lat, lng)
                            }

                            StatusSessionCard(
                                session = session,
                                address = addressMap[key]
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
    onDeviceSelect: (String) -> Unit,
    onLocationClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Top row: Greeting + Location + Device count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hello, $userName",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
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
                        text = "${devices.devices.size} Devices Listed",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Device row (clickable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                    .clickable { expanded = true }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(Res.drawable.app_logo),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = devices.devices.find { it.device_id == selectedDeviceId }?.device_name
                            .takeIf { !it.isNullOrEmpty() }
                            ?: selectedDeviceId
                            ?: "Select Device",
                        fontSize = 14.sp
                    )
                }

                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }

            // Dropdown menu using Material3 DropdownMenu
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                devices.devices.forEach { device ->
                    DropdownMenuItem(
                        text = { Text(device.device_name.ifEmpty { device.device_id }) },
                        onClick = {
                            expanded = false
                            onDeviceSelect(device.device_id)
                        }
                    )
                }
            }
        }
    }
}




@Composable
fun StatusFilters(
    selected: String,
    onSelect: (String) -> Unit
) {
    val filters = listOf("all", "moving", "idle", "parked", "offline")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        filters.forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                label = { Text(filter.uppercase()) }
            )
        }
    }
}



@Composable
fun StatusSessionCard(
    session: StatusSession,
    address: String? = null
) {
    val (statusText, statusColor) = statusUi(session.status)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F2F2)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // ---------- STATUS HEADER ----------
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(statusColor, CircleShape)
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = statusText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor
                )

                Spacer(Modifier.weight(1f))

                Text(
                    text = "${session.duration_minutes} min",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }

            Spacer(Modifier.height(10.dp))

            // ---------- START / END ----------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TimeChip(
                    label = "Start",
                    value = session.start_time,
                    color = Color(0xFF4CAF50)
                )

                TimeChip(
                    label = "End",
                    value = session.end_time,
                    color = Color(0xFFF44336)
                )
            }

            Spacer(Modifier.height(10.dp))

            // ---------- ADDRESS ----------
            Text(
                text = address ?: "Fetching location…",
                fontSize = 13.sp,
                color = Color.DarkGray
            )
        }
    }
}


@Composable
fun TimeChip(
    label: String,
    value: String,
    color: Color
) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.Gray
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = color.copy(alpha = 0.15f)
        ) {
            Text(
                text = value,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                fontSize = 12.sp,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

fun statusUi(status: String): Pair<String, Color> {
    return when (status.lowercase()) {
        "moving", "running" -> "Running" to Color(0xFF86B071)
        "idle" -> "Idle" to Color(0xFFFCD158)
        "parked" -> "Parked" to Color(0xFFFA7575)
        "offline" -> "Offline" to Color(0xFF000000)
        else -> status to Color.Gray
    }
}


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
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.DarkGray
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}
