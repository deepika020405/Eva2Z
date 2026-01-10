package com.developer.ev2z.ui.screens.trackTrace

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.response.Device
import com.developer.ev2z.ui.components.FloatingContactButton
import com.developer.ev2z.ui.screens.device_Report.PlatformMapView
import eva2z_connect_multiplatform.composeapp.generated.resources.Res
import eva2z_connect_multiplatform.composeapp.generated.resources.app_logo
import org.jetbrains.compose.resources.painterResource
import kotlin.text.ifEmpty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackTraceScreen(
    onBack: () -> Unit
) {
    val viewModel = TrackTraceViewModelProvider.viewModel

    val devices by viewModel.devices.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val liveLocation by viewModel.liveLocation.collectAsState()
    val username by viewModel.username.collectAsState()


    PlatformBackHandler(true) { onBack() }

    LaunchedEffect(Unit) {
        viewModel.loadDevices()
    }

    val lat = liveLocation?.data?.latestLocation?.latitude?.toDoubleOrNull()
    val lng = liveLocation?.data?.latestLocation?.longitude?.toDoubleOrNull()

    Scaffold(containerColor = Color(0xFFFFFFFF),
        topBar = {
            TopAppBar(
                title = { Text("Track & Trace") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
    ) { padding ->

        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            // 🗺️ LIVE MAP
            if (lat != null && lng != null) {

                val animatedLat by animateFloatAsState(
                    targetValue = lat.toFloat(),
                    animationSpec = tween(
                        durationMillis = 1000,
                        easing = LinearEasing
                    ),
                    label = "latAnimation"
                )

                val animatedLng by animateFloatAsState(
                    targetValue = lng.toFloat(),
                    animationSpec = tween(
                        durationMillis = 1000,
                        easing = LinearEasing
                    ),
                    label = "lngAnimation"
                )

                PlatformMapView(
                    modifier = Modifier.fillMaxSize(),
                    latitude = animatedLat.toDouble(),
                    longitude = animatedLng.toDouble(),
                    zoom = 15.0
                )
            }


            // 🔽 DEVICE SELECTOR (TOP)
            DeviceDropdownCardSimple(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                userName = username.ifEmpty { "User" },
                devices = devices,
                selectedDevice = selectedDevice,
                onDeviceSelect = viewModel::selectDevice
            )
            FloatingContactButton(
                phoneNumber = "919755273822",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 20.dp)
            )
        }
    }
}



@Composable
fun DeviceDropdownCardSimple(
    modifier: Modifier = Modifier,
    userName: String,
    devices: List<Device>,
    selectedDevice: Device?,
    onDeviceSelect: (Device) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(Modifier.padding(16.dp)) {

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Hello, $userName", fontWeight = FontWeight.SemiBold)

                Text(
                    "${devices.size} Devices",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF4F4F4), RoundedCornerShape(12.dp))
                    .clickable { expanded = true }
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
                    Spacer(Modifier.width(8.dp))
                    Text(
                        selectedDevice?.device_name ?: "Select Device",
                        fontSize = 14.sp
                    )
                }

                Icon(Icons.Default.ArrowDropDown, null)
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                devices.forEach { device ->
                    DropdownMenuItem(
                        text = { Text(device.device_name) },
                        onClick = {
                            expanded = false
                            onDeviceSelect(device)
                        }
                    )
                }
            }
        }
    }
}


@Composable
fun rememberAnimatedLatLng(
    latitude: Double,
    longitude: Double
): Pair<Double, Double> {

    val animatedLat by animateFloatAsState(
        targetValue = latitude.toFloat(),
        animationSpec = tween(
            durationMillis = 1000,
            easing = LinearEasing
        )
    )

    val animatedLng by animateFloatAsState(
        targetValue = longitude.toFloat(),
        animationSpec = tween(
            durationMillis = 1000,
            easing = LinearEasing
        )
    )

    return animatedLat.toDouble() to animatedLng.toDouble()
}
