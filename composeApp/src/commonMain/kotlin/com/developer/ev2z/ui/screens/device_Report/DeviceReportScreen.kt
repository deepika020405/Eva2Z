package com.developer.ev2z.ui.screens.device_Report

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eva2z_connect_multiplatform.composeapp.generated.resources.Res
import eva2z_connect_multiplatform.composeapp.generated.resources.app_logo
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.painterResource

@Composable
fun DeviceReportScreen() {

    var isExpanded by remember { mutableStateOf(false) }

    // Fake devices for dropdown – later you can replace with API data
    val devices = listOf("Name 1", "Name 2", "Name 3", "Truck 4")
    var selectedDevice by remember { mutableStateOf(devices[1]) }
    var isDropdownOpen by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEFF3EF))
    ) {

        // -------------------- MAP AREA --------------------
        MapArea()

        // Fake marker card on map
        MapMarkerChip()

        // -------------------- TOP GREETING + DROPDOWN BOX --------------------
        TopDeviceBox(
            userName = "Rishab",
            deviceCount = devices.size,
            selectedDevice = selectedDevice,
            onDropdownClick = {
                isDropdownOpen = true
            },
            onAddClick = {
                // TODO: Open Add Device Screen
            }
        )

        // -------------------- EXPANDABLE SHEET --------------------
        DeviceReportBottomSheet(
            isExpanded = isExpanded,
            onExpandToggle = { isExpanded = !isExpanded }
        )
    }
}


// ---------------------------------------------------------------------------
// MAP LAYER
// ---------------------------------------------------------------------------
@Composable
fun MapArea() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEFF3EF))
    )
}

// Small marker-like chip (Name + speed) on map
@Composable
fun BoxScope.MapMarkerChip() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        tonalElevation = 2.dp,
        modifier = Modifier
            .align(Alignment.Center)
            .padding(bottom = 100.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(Color(0xFF3EBB3E), RoundedCornerShape(9.dp))
            )
            Spacer(Modifier.width(8.dp))
            Text("Name 1", fontSize = 13.sp)
            Spacer(Modifier.width(6.dp))
            Text("50 km/h", fontSize = 13.sp, color = Color.Gray)
        }
    }
}


// ---------------------------------------------------------------------------
// TOP BOX : HELLO + DROPDOWN
// ---------------------------------------------------------------------------
@OptIn(InternalResourceApi::class)
@Composable
fun TopDeviceBox(
    userName: String,
    deviceCount: Int,
    selectedDevice: String?,
    onDropdownClick: () -> Unit,
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {

        Column(modifier = Modifier.padding(16.dp)) {

            // --------------------------------------------------
            // TOP ROW : Hello + device count
            // --------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hello, $userName",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "$deviceCount Devices Listed",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --------------------------------------------------
            // DROPDOWN ROW
            // --------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .clickable { onDropdownClick() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                // LEFT SIDE : Logo + title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(Res.drawable.app_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = selectedDevice ?: "Select Device",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // RIGHT SIDE : Dropdown + Add icons
                Row(verticalAlignment = Alignment.CenterVertically) {

                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { onAddClick() }
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// BOTTOM SHEET
// ---------------------------------------------------------------------------
@Composable
fun BoxScope.DeviceReportBottomSheet(
    isExpanded: Boolean,
    onExpandToggle: () -> Unit
) {
    val sheetHeight = if (isExpanded) 0.90f else 0.40f // Expand → 90%, minimize → 40%

    Surface(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .fillMaxWidth()
            .fillMaxHeight(sheetHeight)
            .animateContentSize(),
        color = Color.White,
        tonalElevation = 6.dp
    ) {

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            // -----------------------------------------
            // TOP HEADER
            // -----------------------------------------
            ReportHeader()

            // -----------------------------------------
            // DEVICE INFO
            // -----------------------------------------
            DeviceBasicInfo()

            Spacer(Modifier.height(8.dp))

            Divider(Modifier.padding(horizontal = 18.dp))

            // -----------------------------------------
            // DEVICE ANALYSIS ROW (View / Hide)
            // -----------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandToggle() }
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Device Analysis",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (isExpanded) "Hide" else "View",
                    fontSize = 14.sp,
                    color = Color(0xFF3EBB3E)
                )
                Spacer(Modifier.width(2.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown
                    else Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = Color(0xFF3EBB3E)
                )
            }

            // -----------------------------------------
            // FULL ANALYTICS ONLY IN EXPANDED STATE
            // -----------------------------------------
            if (isExpanded) {
                DeviceAnalysisContent()
            }
        }
    }
}


// ---------------------------------------------------------------------------
// HEADER SECTION (Name + Speed)
// ---------------------------------------------------------------------------
@Composable
fun ReportHeader() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("•  Name 2", fontSize = 20.sp, color = Color.Black)
        Spacer(Modifier.weight(1f))
        Text("65 km/h", fontSize = 18.sp, color = Color.Black)
    }
}


// ---------------------------------------------------------------------------
// ADDRESS + Buttons
// ---------------------------------------------------------------------------
@Composable
fun DeviceBasicInfo() {

    Text(
        "Live Location\nB-120, Saket, D-Mart Complex,\nPress Enclave Road, Delhi 110017",
        fontSize = 14.sp,
        lineHeight = 18.sp,
        modifier = Modifier.padding(horizontal = 18.dp)
    )

    Spacer(Modifier.height(10.dp))

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OutlinedButton(
            onClick = {},
            enabled = false,
            modifier = Modifier.weight(1f)
        ) {
            Text("Turn On")
        }

        Button(
            onClick = {},
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B))
        ) {
            Text("Turn Off")
        }
    }

    Spacer(Modifier.height(12.dp))

    Text(
        "Active from: Yesterday, 5:43pm",
        modifier = Modifier.padding(horizontal = 18.dp),
        color = Color.Gray,
        fontSize = 13.sp
    )
}


// ---------------------------------------------------------------------------
// FULL DEVICE ANALYSIS
// ---------------------------------------------------------------------------
@Composable
fun DeviceAnalysisContent() {

    Spacer(Modifier.height(8.dp))

    // Filter chips
    FilterChipsBar()

    Spacer(Modifier.height(16.dp))

    TotalDistanceCard()

    Spacer(Modifier.height(16.dp))

    HeadingGraphCard()

    Spacer(Modifier.height(40.dp))
}


// ---------------------------------------------------------------------------
// FILTER CHIPS
// ---------------------------------------------------------------------------
@Composable
fun FilterChipsBar() {
    Row(
        Modifier
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Chip("Today")
        Chip("Week")
        Chip("Month")
    }
}

@Composable
fun Chip(text: String) {
    Surface(
        color = Color(0xFFDBF2D9),
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontSize = 13.sp
        )
    }
}


// ---------------------------------------------------------------------------
// TOTAL DISTANCE CARD
// ---------------------------------------------------------------------------
@Composable
fun TotalDistanceCard() {
    Surface(
        Modifier
            .padding(horizontal = 18.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF2F4F2)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("Total Distance", fontSize = 16.sp, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            Text("70 km", fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}


// ---------------------------------------------------------------------------
// HEADING GRAPH SECTION
// ---------------------------------------------------------------------------
@Composable
fun HeadingGraphCard() {
    Surface(
        Modifier
            .padding(horizontal = 18.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF2F4F2)
    ) {
        Column(Modifier.padding(18.dp)) {

            Text("Heading", fontSize = 18.sp)

            Spacer(Modifier.height(12.dp))

            Box(
                Modifier
                    .size(160.dp)
                    .align(Alignment.CenterHorizontally)
                    .background(Color.LightGray, RoundedCornerShape(100.dp))
            )

            Spacer(Modifier.height(12.dp))

            Text("Running - 5 hr (50%)")
            Text("Stopped - 3 hr (30%)")
            Text("Parked - 2 hr (20%)")
        }
    }
}