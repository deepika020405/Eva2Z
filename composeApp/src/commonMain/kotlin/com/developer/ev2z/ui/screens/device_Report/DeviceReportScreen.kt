package com.developer.ev2z.ui.screens.device_Report

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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

@Composable
fun DeviceReportScreen() {

    var isExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {

        // MAP AREA (Placeholder)
        MapArea()

        // Bottom Expandable Card
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            DeviceReportCard(
                isExpanded = isExpanded,
                onExpandToggle = { isExpanded = !isExpanded }
            )
        }
    }
}



@Composable
fun MapArea() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEFF3EF))
    )
}


@Composable
fun DeviceReportCard(
    isExpanded: Boolean,
    onExpandToggle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {

            // --------------------
            // Header Section
            // --------------------
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Name 2", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("65 km/h", fontSize = 18.sp)
            }

            // --------------------
            // Address + Turn On/Off Row
            // --------------------
            Text(
                "Live Location\nB-120, Saket, D-Mart Complex, Press Enclave Road,\nDelhi, New Delhi, 110017",
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Row(
                Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(onClick = {}, enabled = false) { Text("Turn On") }
                Button(onClick = {}) { Text("Turn Off") }
            }

            // --------------------
            // Minimized → Expand Button
            // --------------------
            TextButton(
                onClick = onExpandToggle,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(if (isExpanded) "Hide" else "View")
            }

            // --------------------
            // EXPANDED SECTION (Device Analysis)
            // --------------------
            if (isExpanded) {
                DeviceAnalysisContent()
            }
        }
    }
}



@Composable
fun DeviceAnalysisContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 300.dp, max = 600.dp)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        Text("Device Analysis", fontSize = 22.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(12.dp))

        FilterBar()

        Spacer(Modifier.height(16.dp))

        TotalDistanceCard()

        Spacer(Modifier.height(16.dp))

        HeadingGraphCard()
    }
}


@Composable
fun FilterBar() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip("Today")
        FilterChip("Week")
        FilterChip("Month")
    }
}

@Composable
fun FilterChip(label: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color(0xFFDBF2D9)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}


@Composable
fun TotalDistanceCard() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF2F4F2),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Total Distance", fontSize = 18.sp)
            Text("70 km", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
    }
}



@Composable
fun HeadingGraphCard() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF2F4F2),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {

            Text("Heading", fontSize = 18.sp, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(10.dp))

            Box(
                Modifier
                    .size(150.dp)
                    .align(Alignment.CenterHorizontally)
                    .background(Color.LightGray, shape = CircleShape)
            )

            Spacer(Modifier.height(10.dp))

            Text("Running - 5 hr (50%)")
            Text("Stopped - 3 hr (30%)")
            Text("Parked - 2 hr (20%)")
        }
    }
}




