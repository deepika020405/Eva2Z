package com.developer.ev2z.ui.screens.GeoFencing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.developer.ev2z.data.local.model.GeoFencePolygon
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.response.Device
import com.developer.ev2z.response.GeoFenceDto
import com.developer.ev2z.util.formatToIST
import com.developer.ev2z.util.getAddressFromLatLng

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeoFencingScreen(
    onAddGeoFence: () -> Unit,
    onBackPressed: () -> Unit,
    onEditGeoFence: (GeoFenceDto) -> Unit
) {
    val viewModel: GeoFenceViewModel = rememberGeoFenceViewModel()
    val state by viewModel.uiState.collectAsState()

    PlatformBackHandler(enabled = true) {
        onBackPressed()
    }

    LaunchedEffect(Unit) {
        viewModel.loadGeoFencesSafely()
    }

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
//            when (state) {
//                GeoFenceUiState.Loading ->
//                    CircularProgressIndicator(Modifier.align(Alignment.Center))
//
//                GeoFenceUiState.Empty ->
//                    EmptyGeoFenceView(onAddGeoFence)
//
//                is GeoFenceUiState.Success -> {
//                    GeoFenceList(
//                        list = (state as GeoFenceUiState.Success).list,
//                        onDelete = { viewModel.deleteGeoFence(it) },
//                        onEdit = { onEditGeoFence(it) } // ✅
//                    )
//                }
//
//                is GeoFenceUiState.Error ->
//                    Text(
//                        text = (state as GeoFenceUiState.Error).message,
//                        modifier = Modifier.align(Alignment.Center)
//                    )
//            }
            when (state) {

                GeoFenceUiState.Loading ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                GeoFenceUiState.NoDevice ->
                    NoDeviceView(
                        onAddDevice = {
                            // 👉 yahan navigate to Add Device screen
                        }
                    )

                GeoFenceUiState.Empty ->
                    EmptyGeoFenceView(onAddGeoFence)

                is GeoFenceUiState.Success -> {
                    GeoFenceList(
                        list = (state as GeoFenceUiState.Success).list,
                        onDelete = { viewModel.deleteGeoFence(it) },
                        onEdit = { onEditGeoFence(it) }
                    )
                }

                is GeoFenceUiState.Error ->
                    Text(
//                        text = (state as GeoFenceUiState.Error).message,
                        text = "Error: No Device Id Found",
                        modifier = Modifier.align(Alignment.Center)
                    )
            }
        }
    }
}

@Composable
fun NoDeviceView(
    onAddDevice: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("📡", fontSize = 64.sp)
        Text(
            text = "No Devices Found",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        Text(
            text = "Please add a device to create GeoFence",
            fontSize = 14.sp,
            color = Color.Gray
        )
//        Spacer(Modifier.height(8.dp))
//
//        Text(
//            text = "Please add a device to create GeoFence",
//            fontSize = 13.sp,
//            color = Color.Gray
//        )
//
//        Spacer(Modifier.height(20.dp))

//        Button(
//            onClick = onAddDevice,
//            shape = RoundedCornerShape(20.dp)
//        ) {
//            Text("Add Device")
//        }
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

//        Box(
//            modifier = Modifier
//                .size(180.dp)
//                .background(Color.LightGray, RoundedCornerShape(12.dp))
//        )
//
//        Spacer(Modifier.height(16.dp))

        Text(
            text = "Please Select GeoFence",
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
fun GeoFenceList(list: List<GeoFenceDto>, onDelete: (GeoFenceDto) -> Unit, onEdit: (GeoFenceDto) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(list) { item ->
            GeoFenceCard(item,onDelete,onEdit)
        }
    }
}





@Composable
fun GeoFenceCard(
    item: GeoFenceDto,
    onDelete: (GeoFenceDto) -> Unit,
    onEdit: (GeoFenceDto) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    val radiusKm = (item.radius.toFloatOrNull() ?: 0f) / 1000f
    val createdAt = formatToIST(item.created_at)
    val updatedAt = formatToIST(item.updated_at)

    Card(
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {

            // 🔝 Title + Delete
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Device ID: ${item.device_id}",
                        fontSize = 12.sp,
                        color = Color.Black
                    )
                }

                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.Red,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { showDeleteDialog = true }
                )
            }

            Spacer(Modifier.height(12.dp))

            // 📍 Radius Chip
            Surface(
                color = Color(0xFFE8F5E9),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Radius: ${radiusKm.toInt()} km",
                    fontSize = 12.sp,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            // ⏰ Time Info
            Column {
                Text(
                    text = "Created: $createdAt",
                    fontSize = 11.sp,
                    color = Color.Black
                )
                Text(
                    text = "Updated: $updatedAt",
                    fontSize = 11.sp,
                    color = Color.Black
                )
            }

            Spacer(Modifier.height(10.dp))
            Divider()
            Spacer(Modifier.height(6.dp))

            // ✏️ Edit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { onEdit(item) }) {
                    Text("Edit")
                }
            }
        }
    }

    // 🗑 Delete Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete GeoFence") },
            text = { Text("Are you sure you want to delete \"${item.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(item)
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditGeoFenceScreen(
    editingGeoFence: GeoFenceDto?,   // 👈 NULL = Add, NOT NULL = Edit
    onBack: () -> Unit
) {
    val viewModel: GeoFenceViewModel = rememberGeoFenceViewModel()

    val deviceState by viewModel.deviceState.collectAsState()
    val polygon by viewModel.polygon.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var geoFenceName by remember {
        mutableStateOf(editingGeoFence?.name ?: "")
    }

    var radiusKm by remember {
        mutableStateOf(
            if (editingGeoFence != null)
                (editingGeoFence.radius.toFloat() / 1000f).toInt().toString()
            else ""
        )
    }

    var radius by remember {
        mutableStateOf(editingGeoFence?.radius?.toString() ?: "")
    }

    var selectedDevice by remember {
        mutableStateOf<Device?>(null)
    }

    var selectedLocation by remember {
        mutableStateOf<LocationResult?>(null)
    }

    var showDeviceSheet by remember { mutableStateOf(false) }
    var showLocationPicker by remember { mutableStateOf(false) }
    val isFormValid = remember(
        geoFenceName,
        radiusKm,
        selectedDevice,
        selectedLocation
    ) {
        geoFenceName.isNotBlank() &&
                radiusKm.isNotBlank() &&
                selectedLocation != null &&
                selectedDevice != null
    }


    PlatformBackHandler(enabled = true) {
        onBack()
    }

    // 🔁 PREFILL FOR EDIT
    LaunchedEffect(editingGeoFence, deviceState) {
        editingGeoFence?.let { geo ->

            // 📍 Location
            val lat = geo.latitude.toDouble()
            val lng = geo.longitude.toDouble()

            // 📍 Convert lat/lng → address
            val address = getAddressFromLatLng(lat, lng)

            selectedLocation = LocationResult(
                name = address,     // 🔥 ADDRESS AB DIKHEGA
                lat = lat,
                lng = lng
            )

            viewModel.setEditLocation(
                lat = geo.latitude,
                lng = geo.longitude
            )

            // 📟 Device (ID → NAME)
            val device = viewModel.getDeviceById(geo.device_id)
            selectedDevice = device ?: Device(
                id = 0,
                device_id = geo.device_id,
                device_name = geo.device_id, // fallback
                status = "1",
                engine_status = "0",
                created_at = ""
            )
        }
    }



    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(if (editingGeoFence == null) "Add GeoFence" else "Edit GeoFence")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
//        var radiusKm by remember {
//            mutableStateOf(
//                if (editingGeoFence != null)
//                    (editingGeoFence.radius.toFloat() / 1000f).toInt().toString()
//                else ""
//            )
//        }
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
                    placeholder = { Text("Enter your GeoFence Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // DEVICE
            item {
                OutlinedTextField(
                    value = selectedDevice?.device_name ?: "",
                    enabled = false,
                    onValueChange = {},
                    placeholder = { Text("Select Device") },
                    trailingIcon = {

                        if (editingGeoFence == null) {
                            // ➕ ADD MODE → allow select
                            TextButton(onClick = {
                                showDeviceSheet = true
                                viewModel.loadDevices()
                            }) {
                                Text("Select", color = Color(0xFF4CAF50))
                            }
                        } else {
                            // ✏️ EDIT MODE → locked
                            Text(
                                "Locked",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

            }

            // LOCATION
            item {
                OutlinedTextField(
                    value = selectedLocation?.name ?: "",
                    enabled = false,
                    onValueChange = {},
                    placeholder = { Text("Select Location") },
                    trailingIcon = {
                        TextButton(onClick = { showLocationPicker = true }) {
                            Text("Select", color = Color(0xFF4CAF50))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // RADIUS
            item {
                OutlinedTextField(
                    value = radiusKm,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            radiusKm = input
                            // ✅ pass as-is, backend will multiply by 1000
                        }
                    },
                    placeholder = { Text("Radius (KM)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }



            item {
                val isLoading = saveState is SaveGeoFenceState.Loading
                Button(
                    onClick = {
                        if (
                            geoFenceName.isNotBlank() &&
                            radiusKm.isNotBlank() &&
                            selectedLocation != null &&
                            selectedDevice != null
                        ) {

                            val lat = selectedLocation!!.lat.toString()
                            val lng = selectedLocation!!.lng.toString()

                            if (editingGeoFence == null) {
                                // ➕ ADD
                                viewModel.saveGeoFence(
                                    name = geoFenceName,
                                    device = selectedDevice!!,
                                    radiusMeters = radiusKm.toInt(),
                                    lat = lat,
                                    lng = lng
                                )
                            } else {
                                // ✏️ UPDATE (🔥 FIX)
                                viewModel.updateGeoFence(
                                    geoFence = editingGeoFence,
                                    name = geoFenceName,
                                    radius = radiusKm.toInt(),
                                    lat = lat,
                                    lng = lng
                                )
                            }
                        }
                    },
                    enabled = isFormValid && !isLoading,   // 🔥 KEY LINE
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp)
                ) {

                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Text(
                            if (editingGeoFence == null) "Save" else "Update",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }


    if (editingGeoFence == null && showDeviceSheet) {
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
//    if (editingGeoFence != null) {
//        Text(
//            "Device cannot be changed after GeoFence creation",
//            fontSize = 11.sp,
//            color = Color.Gray
//        )
//        )
//    }



    // LOCATION DIALOG
    if (showLocationPicker) {
        LocationPickerDialog(
            onDismiss = { showLocationPicker = false },
            onLocationSelected = {
                selectedLocation = it
                viewModel.selectLocation(it)
                showLocationPicker = false
            }
        )
    }

    // CLOSE ON SUCCESS
    LaunchedEffect(saveState) {
        if (saveState is SaveGeoFenceState.Success) {
            viewModel.resetSaveState()   // ✅ VERY IMPORTANT
            onBack()
        }
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
                    placeholder = { Text("Search area, city") },
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




