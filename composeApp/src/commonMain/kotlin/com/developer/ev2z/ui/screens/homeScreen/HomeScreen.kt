package com.developer.ev2z.ui.screens.homeScreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import eva2z_connect_multiplatform.composeapp.generated.resources.Res
import eva2z_connect_multiplatform.composeapp.generated.resources.homebanner
import eva2z_connect_multiplatform.composeapp.generated.resources.scooter

// ================= COLORS =================

private val BrandGreen = Color(0xFF34A853)
private val BackgroundColor = Color(0xFFF6F6F6)
private val CardColor = Color.White
private val TextPrimary = Color(0xFF1E1E1E)
private val TextSecondary = Color(0xFF777777)

// ================= DATA MODEL =================

data class QuickActionItem(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

// ================= MAIN SCREEN =================

@Composable
fun HomeScreen(
    userName: String,
    onOpenLiveLocation: () -> Unit,
    onManageDevice: () -> Unit,
    onGpsData: () -> Unit,
    onGeoFence: () -> Unit,
    onHistory: () -> Unit,
    onOpenDocuments: () -> Unit,        // ✅ ADD THIS
    onOpenDeviceReport: () -> Unit ,
    onRefer: () -> Unit,
    onCommunity: () -> Unit
) {
    Scaffold(containerColor = BackgroundColor) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            ModernTopHeader(userName)

            TrackingPreviewCard(onOpenLiveLocation)

            HomeQuickActionsSection(
                onMyDevices = onOpenLiveLocation,
                onManageDevice = onManageDevice,
                onGpsData = onGpsData,
                onGeoFence = onGeoFence,
                onHistory = onHistory,
                onRefer = onRefer,
                onOpenDocuments = onOpenDocuments,
                onOpenDeviceReport = onOpenDeviceReport,
                onCommunity = onCommunity
            )

            ModernFeaturesSection()

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ================= HEADER =================

@Composable
fun ModernTopHeader(userName: String) {

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor)
    ) {

        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(
                        BrandGreen.copy(alpha = 0.12f),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.scooter),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {

                Text(
                    text = "Welcome Back",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = userName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = BrandGreen
            )
        }
    }
}

// ================= TRACKING PREVIEW =================

@Composable
fun TrackingPreviewCard(onClick: () -> Unit) {

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(190.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        onClick = onClick
    ) {
        Image(
            painter = painterResource(Res.drawable.homebanner),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

// ================= QUICK ACTIONS =================

@Composable
fun HomeQuickActionsSection(
    onMyDevices: () -> Unit,
    onManageDevice: () -> Unit,
    onGpsData: () -> Unit,
    onGeoFence: () -> Unit,
    onHistory: () -> Unit,
    onRefer: () -> Unit,
    onOpenDocuments: () -> Unit,      // ✅ ADD
    onOpenDeviceReport: () -> Unit,   // ✅ ADD
    onCommunity: () -> Unit
) {

    val features = listOf(
        QuickActionItem("Live Location", Icons.Default.LocationOn, onMyDevices),
        QuickActionItem("Create Geofencing", Icons.Default.Security, onGeoFence),
        QuickActionItem("History Playback", Icons.Default.History, onHistory),
        QuickActionItem("Engine On/Off", Icons.Default.PowerSettingsNew, onManageDevice),
//        QuickActionItem("Anti-theft", Icons.Default.Security, onManageDevice),
        QuickActionItem("Documents Vaults", Icons.Default.Folder, onOpenDocuments),   // ✅ FIXED
        QuickActionItem("Daily Report", Icons.Default.Assessment, onOpenDeviceReport), // ✅ FIXED
        QuickActionItem("Background Verification", Icons.Default.Badge, onRefer),
        QuickActionItem("Criminal Verification", Icons.Default.VerifiedUser, onRefer)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {

        Column(modifier = Modifier.padding(20.dp)) {

            Text(
                text = "Quick Actions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(Modifier.height(20.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                userScrollEnabled = false,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.height(340.dp) // perfect height for 3 rows
            ) {
                items(features) { item ->
                    ModernFeatureItem(
                        title = item.title,
                        icon = item.icon,
                        onClick = item.onClick
                    )
                }
            }
        }
    }
}

// ================= FEATURE ITEM =================

@Composable
fun ModernFeatureItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
        modifier = Modifier.aspectRatio(1f)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly // 👈 important
        ) {

            Box(
                modifier = Modifier
                    .size(50.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BrandGreen,
                    modifier = Modifier.size(26.dp)
                )
            }

            Text(
                text = title,
                fontSize = 11.sp, // 👈 slightly smaller
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = TextPrimary,
                maxLines = 2,
                lineHeight = 14.sp // 👈 better wrapping
            )
        }
    }
}
// ================= UPCOMING FEATURES =================

@Composable
fun ModernFeaturesSection() {

    val features = listOf(
        "Driver Score" to Icons.Default.EmojiEvents,
        "Service Market" to Icons.Default.Build,
        "Repair Market" to Icons.Default.DirectionsCar,
        "Charging" to Icons.Default.EvStation,
        "Parking" to Icons.Default.LocalParking,
        "SOS" to Icons.Default.Warning
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {

        Column(modifier = Modifier.padding(20.dp)) {

            Text(
                text = "Upcoming Features",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(20.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                userScrollEnabled = false,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.height(220.dp)
            ) {
                items(features) { (title, icon) ->
                    ModernFeatureItem(
                        title = title,
                        icon = icon,
                        onClick = {}
                    )
                }
            }
        }
    }
}