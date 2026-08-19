package com.developer.ev2z.ui.screens.GeoFencing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.developer.ev2z.data.local.model.GeoFencePolygon
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.response.Device
import com.developer.ev2z.response.GeoFenceDto
import com.developer.ev2z.util.formatToIST
import com.developer.ev2z.util.getAddressFromLatLng
import com.developer.ev2z.theme.AppPageBg

// ── Theme tokens — light green + white ────────────────────────────
private object G {
    val Primary      = Color(0xFF6A9F50)
    val PrimaryLight = Color(0xFF85B070)
    val PrimaryBg    = Color(0xFFF0F7EC)
    val PrimaryBorder= Color(0xFFD4E4CD)
    val Surface      = Color(0xFFFFFFFF)
    val Background   = AppPageBg
    val TextPrimary  = Color(0xFF1C2418)
    val TextSecondary= Color(0xFF5D6858)
    val TextHint     = Color(0xFFA0A99B)
    val Border       = Color(0xFFECF0E8)
    val Red          = Color(0xFFE5565B)
    val RedBg        = Color(0xFFFEF2F2)
}

// ═══════════════════════════════════════════════════════════════════
//  GEOFENCE LIST SCREEN
// ═══════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeoFencingScreen(
    onAddGeoFence: () -> Unit,
    onBackPressed: () -> Unit,
    onEditGeoFence: (GeoFenceDto) -> Unit
) {
    val viewModel: GeoFenceViewModel = rememberGeoFenceViewModel()
    val state by viewModel.uiState.collectAsState()

    PlatformBackHandler(enabled = true) { onBackPressed() }

    LaunchedEffect(Unit) { viewModel.loadGeoFencesSafely() }

    Scaffold(
        containerColor = G.Background,
        topBar = {
            Surface(color = G.Surface, shadowElevation = 1.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = G.TextPrimary)
                    }
                    Text(
                        "GeoFence",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                            color = G.TextPrimary
                        )
                    )
                    TextButton(onClick = onAddGeoFence) {
                        Icon(
                            Icons.Default.Add, null,
                            tint = G.Primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Add",
                            color = G.Primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (state) {
                GeoFenceUiState.Loading ->
                    CircularProgressIndicator(
                        color = G.Primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.align(Alignment.Center)
                    )

                GeoFenceUiState.NoDevice ->
                    NoDeviceView(onAddDevice = {})

                GeoFenceUiState.Empty ->
                    EmptyGeoFenceView(onAddGeoFence)

                is GeoFenceUiState.Success ->
                    GeoFenceList(
                        list = (state as GeoFenceUiState.Success).list,
                        onDelete = { viewModel.deleteGeoFence(it) },
                        onEdit = { onEditGeoFence(it) }
                    )

                is GeoFenceUiState.Error ->
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.ErrorOutline, null,
                            tint = G.TextHint, modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            (state as GeoFenceUiState.Error).message,
                            color = G.TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = { viewModel.loadGeoFencesSafely() }) {
                            Text("Retry", color = G.Primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
            }
        }
    }
}

// ── Empty States ──────────────────────────────────────────────────

@Composable
fun NoDeviceView(onAddDevice: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(G.PrimaryBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.SettingsInputAntenna, null,
                tint = G.Primary, modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "No Devices Found",
            fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = G.TextPrimary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Please add a device to create GeoFence",
            fontSize = 14.sp, color = G.TextSecondary, textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EmptyGeoFenceView(onAddGeoFence: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(G.PrimaryBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.LocationOn, null,
                tint = G.Primary, modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "No GeoFences Yet",
            fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = G.TextPrimary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Create your first GeoFence to get alerts\nwhen your vehicle enters or exits an area.",
            fontSize = 14.sp, color = G.TextSecondary,
            textAlign = TextAlign.Center, lineHeight = 20.sp
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onAddGeoFence,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = G.Primary),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Add First GeoFence", fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── GeoFence List ─────────────────────────────────────────────────

@Composable
fun GeoFenceList(
    list: List<GeoFenceDto>,
    onDelete: (GeoFenceDto) -> Unit,
    onEdit: (GeoFenceDto) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(list) { item ->
            GeoFenceCard(item, onDelete, onEdit)
        }
    }
}

// ── GeoFence Card ─────────────────────────────────────────────────

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
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = G.Surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, G.Border, RoundedCornerShape(16.dp))
    ) {
        Column(Modifier.padding(16.dp)) {
            // Title + Delete
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(G.PrimaryBg, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.LocationOn, null,
                        tint = G.Primary, modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.name,
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        color = G.TextPrimary, maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Device: ${item.device_id}",
                        fontSize = 11.sp, color = G.TextHint
                    )
                }
                // Delete icon
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(G.RedBg, RoundedCornerShape(8.dp))
                        .clickable { showDeleteDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Delete, "Delete",
                        tint = G.Red, modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Radius + timestamps row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Radius chip
                Surface(
                    color = G.PrimaryBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Radius: ${radiusKm.toInt()} km",
                        fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        color = G.Primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "Created: $createdAt",
                    fontSize = 10.sp, color = G.TextHint
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = G.Border, thickness = 0.5.dp)
            Spacer(Modifier.height(8.dp))

            // Edit button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { onEdit(item) },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Outlined.Edit, null,
                        tint = G.Primary, modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Edit",
                        color = G.Primary, fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // Delete Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape = RoundedCornerShape(20.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(G.RedBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Delete, null,
                        tint = G.Red, modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    "Delete GeoFence",
                    fontWeight = FontWeight.SemiBold, color = G.TextPrimary,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete \"${item.name}\"?",
                    color = G.TextSecondary, fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { showDeleteDialog = false; onDelete(item) },
                    colors = ButtonDefaults.buttonColors(containerColor = G.Red),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Delete", color = Color.White, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    shape = RoundedCornerShape(10.dp),
                    border = ButtonDefaults.outlinedButtonBorder
                ) { Text("Cancel", color = G.TextSecondary) }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
//  ADD / EDIT GEOFENCE SCREEN
// ═══════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditGeoFenceScreen(
    editingGeoFence: GeoFenceDto?,
    onBack: () -> Unit
) {
    val viewModel: GeoFenceViewModel = rememberGeoFenceViewModel()
    val deviceState by viewModel.deviceState.collectAsState()
    val polygon by viewModel.polygon.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    var geoFenceName by remember { mutableStateOf(editingGeoFence?.name ?: "") }
    var radiusKm by remember {
        mutableStateOf(
            if (editingGeoFence != null)
                (editingGeoFence.radius.toFloat() / 1000f).toInt().toString()
            else ""
        )
    }
    var radius by remember { mutableStateOf(editingGeoFence?.radius?.toString() ?: "") }
    var selectedDevice by remember { mutableStateOf<Device?>(null) }
    var selectedLocation by remember { mutableStateOf<LocationResult?>(null) }
    var showDeviceSheet by remember { mutableStateOf(false) }
    var showLocationPicker by remember { mutableStateOf(false) }

    val isFormValid = remember(geoFenceName, radiusKm, selectedDevice, selectedLocation) {
        geoFenceName.isNotBlank() && radiusKm.isNotBlank() &&
                selectedLocation != null && selectedDevice != null
    }

    PlatformBackHandler(enabled = true) { onBack() }

    // Prefill for edit
    LaunchedEffect(editingGeoFence, deviceState) {
        editingGeoFence?.let { geo ->
            val lat = geo.latitude.toDouble()
            val lng = geo.longitude.toDouble()
            val address = getAddressFromLatLng(lat, lng)
            selectedLocation = LocationResult(name = address, lat = lat, lng = lng)
            viewModel.setEditLocation(lat = geo.latitude, lng = geo.longitude)
            val device = viewModel.getDeviceById(geo.device_id)
            selectedDevice = device ?: Device(
                id = 0, device_id = geo.device_id,
                device_name = geo.device_id, status = "1",
                engine_status = "0", created_at = ""
            )
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) focusManager.clearFocus()
    }

    Scaffold(
        containerColor = G.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(color = G.Surface, shadowElevation = 1.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = G.TextPrimary)
                    }
                    Text(
                        if (editingGeoFence == null) "Add GeoFence" else "Edit GeoFence",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp, color = G.TextPrimary
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // Dismiss keyboard on tap
            Box(
                modifier = Modifier.matchParentSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { focusManager.clearFocus() }
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Map preview
                item {
                    GeoFenceMapPreview(polygon)
                }

                // GeoFence name
                item {
                    Text(
                        "GeoFence Name",
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = G.TextPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = geoFenceName,
                        onValueChange = { geoFenceName = it },
                        placeholder = { Text("Enter GeoFence name", color = G.TextHint) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = G.Primary,
                            unfocusedBorderColor = G.Border,
                            cursorColor = G.Primary
                        ),
                        singleLine = true
                    )
                }

                // Device selector
                item {
                    Text(
                        "Device",
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = G.TextPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = selectedDevice?.device_name ?: "",
                        enabled = false,
                        onValueChange = {},
                        placeholder = { Text("Select Device", color = G.TextHint) },
                        trailingIcon = {
                            if (editingGeoFence == null) {
                                TextButton(onClick = {
                                    showDeviceSheet = true
                                    viewModel.loadDevices()
                                }) {
                                    Text("Select", color = G.Primary, fontWeight = FontWeight.SemiBold)
                                }
                            } else {
                                Surface(
                                    color = G.PrimaryBg,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "Locked",
                                        color = G.TextHint, fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledBorderColor = G.Border,
                            disabledTextColor = G.TextPrimary
                        )
                    )
                }

                // Location selector
                item {
                    Text(
                        "Location",
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = G.TextPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = selectedLocation?.name ?: "",
                        enabled = false,
                        onValueChange = {},
                        placeholder = { Text("Select Location", color = G.TextHint) },
                        trailingIcon = {
                            TextButton(onClick = { showLocationPicker = true }) {
                                Text("Select", color = G.Primary, fontWeight = FontWeight.SemiBold)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledBorderColor = G.Border,
                            disabledTextColor = G.TextPrimary
                        )
                    )
                }

                // Radius
                item {
                    Text(
                        "Radius (KM)",
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = G.TextPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = radiusKm,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() }) radiusKm = input
                        },
                        placeholder = { Text("e.g. 5", color = G.TextHint) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = G.Primary,
                            unfocusedBorderColor = G.Border,
                            cursorColor = G.Primary
                        ),
                        singleLine = true
                    )
                }

                // Save / Update button
                item {
                    val isLoading = saveState is SaveGeoFenceState.Loading
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = {
                            if (isFormValid) {
                                focusManager.clearFocus()
                                val lat = selectedLocation!!.lat.toString()
                                val lng = selectedLocation!!.lng.toString()
                                if (editingGeoFence == null) {
                                    viewModel.saveGeoFence(
                                        name = geoFenceName, device = selectedDevice!!,
                                        radiusMeters = radiusKm.toInt(), lat = lat, lng = lng
                                    )
                                } else {
                                    viewModel.updateGeoFence(
                                        geoFence = editingGeoFence, name = geoFenceName,
                                        radius = radiusKm.toInt(), lat = lat, lng = lng
                                    )
                                }
                            }
                        },
                        enabled = isFormValid && !isLoading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = G.Primary),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White, strokeWidth = 2.dp,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Text(
                                if (editingGeoFence == null) "Save GeoFence" else "Update GeoFence",
                                color = Color.White, fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Device picker
    if (editingGeoFence == null && showDeviceSheet) {
        DevicePickerBottomSheet(
            deviceState = deviceState,
            selectedDeviceId = selectedDevice?.device_id,
            onSelect = { selectedDevice = it; showDeviceSheet = false },
            onDismiss = { showDeviceSheet = false }
        )
    }

    // Location picker
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

    // Close on success
    LaunchedEffect(saveState) {
        if (saveState is SaveGeoFenceState.Success) {
            viewModel.resetSaveState()
            onBack()
        }
    }
}

// ── Map Preview ───────────────────────────────────────────────────

@Composable
fun GeoFenceMapPreview(polygon: GeoFencePolygon?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(0.5.dp, G.Border, RoundedCornerShape(16.dp))
    ) {
        GeoFenceMap(polygon)
    }
}

// ── Device Picker ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicePickerBottomSheet(
    deviceState: DeviceUiState,
    selectedDeviceId: String?,
    onSelect: (Device) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = G.Surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Text(
            "Select Device",
            fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
            color = G.TextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )
        HorizontalDivider(color = G.Border, thickness = 0.5.dp)

        when (deviceState) {
            DeviceUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = G.Primary, strokeWidth = 2.dp)
                }
            }

            is DeviceUiState.Success -> {
                LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(deviceState.list) { device ->
                        val isSelected = device.device_id == selectedDeviceId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (isSelected) 0.4f else 1f)
                                .clickable(enabled = !isSelected) { onSelect(device) }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(G.PrimaryBg, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Router, null,
                                    tint = G.Primary, modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    device.device_name,
                                    fontWeight = FontWeight.Medium, color = G.TextPrimary
                                )
                                Text(
                                    "ID: ${device.device_id}",
                                    fontSize = 12.sp, color = G.TextHint
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Default.CheckCircle, null,
                                    tint = G.Primary, modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            is DeviceUiState.Error -> {
                Text(
                    deviceState.msg, color = G.Red,
                    modifier = Modifier.padding(20.dp)
                )
            }

            else -> {}
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ── Location Picker ───────────────────────────────────────────────

@Composable
fun LocationPickerDialog(
    onDismiss: () -> Unit,
    onLocationSelected: (LocationResult) -> Unit
) {
    val viewModel: GeoFenceViewModel = rememberGeoFenceViewModel()
    val results by viewModel.results.collectAsState()
    val isSearching by viewModel._isSearching.collectAsState()
    var query by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = G.Surface),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Select Location",
                        fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                        color = G.TextPrimary, modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(G.PrimaryBg, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Close, "Close",
                            modifier = Modifier.size(16.dp), tint = G.TextSecondary
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Search field
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        if (it.length >= 3) viewModel.searchLocation(it)
                    },
                    placeholder = { Text("Search area, city...", color = G.TextHint) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, null, tint = G.TextHint)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text, imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = G.Primary,
                        unfocusedBorderColor = G.Border,
                        cursorColor = G.Primary
                    ),
                    singleLine = true
                )

                Spacer(Modifier.height(8.dp))

                if (isSearching) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = G.Primary,
                        trackColor = G.PrimaryBg
                    )
                }

                Spacer(Modifier.height(8.dp))

                if (!isSearching && results.isEmpty() && query.length >= 3) {
                    Text(
                        "No results found",
                        color = G.TextHint, fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Results
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.LocationOn, null,
            tint = G.Primary, modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            location.name,
            fontWeight = FontWeight.Medium, maxLines = 2,
            color = G.TextPrimary, fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
    }
    HorizontalDivider(color = G.Border, thickness = 0.5.dp)
}