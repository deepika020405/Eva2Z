package com.developer.ev2z.ui.screens.GeoFencing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.developer.ev2z.data.local.model.GeoFencePolygon
import com.developer.ev2z.response.Device
import com.developer.ev2z.response.GeoFenceDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeoFencingScreen(
    onAddGeoFence: () -> Unit,
    onBackPressed: () -> Unit
) {
    val viewModel: GeoFenceViewModel = rememberGeoFenceViewModel()
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GeoFence") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onAddGeoFence) {
                        Text("+ Add More GeoFence")
                    }
                }
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (state) {
                GeoFenceUiState.Loading ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                GeoFenceUiState.Empty ->
                    EmptyGeoFenceView(onAddGeoFence)

                is GeoFenceUiState.Success ->
                    GeoFenceList((state as GeoFenceUiState.Success).list)

                is GeoFenceUiState.Error ->
                    Text(
                        text = (state as GeoFenceUiState.Error).message,
                        modifier = Modifier.align(Alignment.Center)
                    )
            }
        }
    }
}


@Composable
fun EmptyGeoFenceView(
    onAddGeoFence: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .size(180.dp)
                .background(Color.LightGray, RoundedCornerShape(12.dp))
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor.",
            textAlign = TextAlign.Center,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onAddGeoFence,
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("Add First GeoFence")
        }
    }
}


@Composable
fun GeoFenceList(list: List<GeoFenceDto>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(list) { item ->
            GeoFenceCard(item)
        }
    }
}


@Composable
fun GeoFenceCard(item: GeoFenceDto) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.Red
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Device ID: ${item.device_id}",
                color = Color.Gray,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Radius: ${item.radius} m",
                fontSize = 12.sp
            )

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { /* Disable */ }) {
                    Text("Disable", color = Color.Red)
                }
                TextButton(onClick = { /* Edit */ }) {
                    Text("Edit")
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGeoFenceScreen(
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    val viewModel: GeoFenceViewModel = rememberGeoFenceViewModel()

    val deviceState by viewModel.deviceState.collectAsState()

    var geoFenceName by remember { mutableStateOf("") }
    var selectedDevice by remember { mutableStateOf<Device?>(null) }
    var showDeviceSheet by remember { mutableStateOf(false) }
    var radius by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf<LocationResult?>(null) }
    var showLocationPicker by remember { mutableStateOf(false) }
    val polygon by viewModel.polygon.collectAsState()


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add GeoFence") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {


            item { GeoFenceMapPreview(polygon) }

            item {
                OutlinedTextField(
                    value = geoFenceName,
                    onValueChange = { geoFenceName = it },
                    placeholder = { Text("Add Geofence Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 🔽 DEVICE FIELD
            item {
                OutlinedTextField(
                    value = selectedDevice?.device_name ?: "",
                    onValueChange = {},
                    enabled = false,
                    placeholder = { Text("Select Device") },
                    trailingIcon = {
                        TextButton(
                            onClick = {
                                showDeviceSheet = true
                                viewModel.loadDevices() // 🔥 API CALL
                            }
                        ) {
                            Text("+ Add Device", color = Color(0xFF4CAF50))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = selectedLocation?.name ?: "",
                    onValueChange = {},
                    enabled = false,
                    placeholder = { Text("Select Location") },
                    trailingIcon = {
                        TextButton(onClick = { showLocationPicker = true }) {
                            Text("+ Add Location", color = Color(0xFF4CAF50))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = radius,
                    onValueChange = { radius = it },
                    placeholder = { Text("Enter Radius") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text(
                    text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            item {
                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text("+ Save", color = Color.White)
                }
            }
        }
    }

    // 🔽 DEVICE LIST BOTTOM SHEET
    if (showDeviceSheet) {
        DevicePickerBottomSheet(
            deviceState = deviceState,
            selectedDeviceId = selectedDevice?.device_id,
            onSelect = {
                selectedDevice = it
                showDeviceSheet = false
            },
            onDismiss = { showDeviceSheet = false }
        )
    }

    if (showLocationPicker) {
        LocationPickerDialog(
            onDismiss = { showLocationPicker = false },
            onLocationSelected = {
                selectedLocation = it
                showLocationPicker = false
            }
        )
    }

}


@Composable
fun GeoFenceMapPreview(
    polygon: GeoFencePolygon?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        GeoFenceMap(polygon)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicePickerBottomSheet(
    deviceState: DeviceUiState,
    selectedDeviceId: String?,
    onSelect: (Device) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {

        Text(
            "Select Device",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        when (deviceState) {

            DeviceUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is DeviceUiState.Success -> {
                LazyColumn {
                    items(deviceState.list) { device ->

                        val isSelected =
                            device.device_id == selectedDeviceId

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .alpha(if (isSelected) 0.4f else 1f)
                                .clickable(enabled = !isSelected) {
                                    onSelect(device)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = device.device_name,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "ID: ${device.device_id}",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }

                            if (isSelected) {
                                Text(
                                    "Selected",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            is DeviceUiState.Error -> {
                Text(
                    text = deviceState.msg,
                    color = Color.Red,
                    modifier = Modifier.padding(16.dp)
                )
            }

            else -> {}
        }
    }
}



@Composable
fun LocationPickerDialog(
    onDismiss: () -> Unit,
    onLocationSelected: (LocationResult) -> Unit
) {
    val viewModel: GeoFenceViewModel = rememberGeoFenceViewModel()

    val results by viewModel.results.collectAsState()
    val isSearching by viewModel._isSearching.collectAsState()

    var query by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                // 🔝 Header
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Location",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 🔍 Search
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        if (it.length >= 3) {
                            viewModel.searchLocation(it)
                        }
                    },
                    placeholder = { Text("Search area, city (e.g. Noida)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                // 🔄 Loader
                if (isSearching) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(8.dp))

                // ❌ Empty state
                if (!isSearching && results.isEmpty() && query.length >= 3) {
                    Text(
                        "No results found",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }

                // 📍 Results list
                LazyColumn(
                    modifier = Modifier
                        .heightIn(max = 300.dp)
                ) {
                    items(results) { location ->
                        LocationRow(
                            location = location,
                            onClick = {
                                viewModel.selectLocation(location)
                                onLocationSelected(location)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LocationRow(
    location: LocationResult,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = location.name,
            fontWeight = FontWeight.Medium,
            maxLines = 2
        )
        Spacer(Modifier.height(2.dp))
        Divider()
    }
}


