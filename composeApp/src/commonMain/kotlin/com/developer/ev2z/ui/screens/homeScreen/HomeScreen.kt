package com.developer.ev2z.ui.screens.homeScreen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import org.jetbrains.compose.resources.painterResource
import eva2z_connect_multiplatform.composeapp.generated.resources.Res
import eva2z_connect_multiplatform.composeapp.generated.resources.homebanner
import eva2z_connect_multiplatform.composeapp.generated.resources.homebanner1
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

// ================= GRID HEIGHT CALCULATOR =================

fun calculateGridHeight(
    itemCount: Int,
    columns: Int,
    itemHeight: Dp,
    verticalSpacing: Dp
): Dp {
    val rows = (itemCount + columns - 1) / columns
    return (rows * itemHeight) + ((rows - 1) * verticalSpacing)
}

// ================= MAIN SCREEN =================

@Composable
fun HomeScreen(
    userName: String,
    onOpenLiveLocation: () -> Unit,
    onManageDevice: () -> Unit,
    onGpsData: () -> Unit,
    onGeoFence: () -> Unit,
    onHistory: () -> Unit,
    onOpenDocuments: () -> Unit,
    onOpenDeviceReport: () -> Unit,
    onRefer: () -> Unit,
    onOpenAddDevice: () -> Unit,
    onCriminalVerification: () -> Unit   // ✅ ADD THIS

) {
    Scaffold(containerColor = BackgroundColor) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            item { ModernTopHeader(userName) }

            item { TrackingPreviewCard() }

            item {
                HomeQuickActionsSection(
                    onMyDevices = onOpenLiveLocation,
                    onManageDevice = onManageDevice,
                    onGpsData = onGpsData,
                    onGeoFence = onGeoFence,
                    onHistory = onHistory,
                    onRefer = onRefer,
                    onOpenDocuments = onOpenDocuments,
                    onOpenDeviceReport = onOpenDeviceReport,
                    onCriminalVerification = onCriminalVerification,   // ✅ ADD
                    onOpenAddDevice = onOpenAddDevice
                )
            }

            item { ModernFeaturesSection() }

            item { Spacer(modifier = Modifier.height(20.dp)) }
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

                Text("Welcome Back", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = userName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            Icon(Icons.Default.Notifications, null, tint = BrandGreen)
        }
    }
}

// ================= TRACKING CARD =================

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackingPreviewCard() {

    val images = listOf(
        Res.drawable.homebanner,
        Res.drawable.homebanner1
    )

    val pagerState = rememberPagerState(pageCount = { images.size })

    // 🔥 AUTO SCROLL EFFECT
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(5000) // ⏱ 3 sec delay
            val nextPage = (pagerState.currentPage + 1) % images.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Column {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(190.dp)
        ) { page ->

            Card(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                Image(
                    painter = painterResource(images[page]),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 🔘 Indicator Dots
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(images.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(if (isSelected) 10.dp else 6.dp)
                        .background(
                            if (isSelected) BrandGreen else Color.LightGray,
                            shape = RoundedCornerShape(50)
                        )
                )
            }
        }
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
    onOpenDocuments: () -> Unit,
    onOpenDeviceReport: () -> Unit,
    onCriminalVerification: () -> Unit,   // ✅ ADD
    onOpenAddDevice:  () -> Unit
) {

    val features = listOf(
        QuickActionItem("Live Location", Icons.Default.LocationOn, onMyDevices),
        QuickActionItem("Create Geofencing", Icons.Default.Security, onGeoFence),
        QuickActionItem("History Playback", Icons.Default.History, onHistory),
        QuickActionItem("Engine On/Off", Icons.Default.PowerSettingsNew, onManageDevice),
        QuickActionItem("Document Vault", Icons.Default.Folder, onOpenDocuments),
        QuickActionItem("Device Report", Icons.Default.Assessment, onOpenDeviceReport),
        QuickActionItem("Add Device", Icons.Default.Add, onOpenAddDevice) ,
        QuickActionItem(
            "Criminal Verification",
            Icons.Default.VerifiedUser,
            onCriminalVerification   // ✅ connect
        )
       )

    val columns = 3
    val itemHeight = 110.dp
    val spacing = 16.dp
    val gridHeight = calculateGridHeight(features.size, columns, itemHeight, spacing)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {

        Column(modifier = Modifier.padding(20.dp)) {

            Text("Quick Actions", fontSize = 18.sp, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(20.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                userScrollEnabled = false,
                verticalArrangement = Arrangement.spacedBy(spacing),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gridHeight)
            ) {
                items(features) { item ->
                    ModernFeatureItem(item.title, item.icon, item.onClick)
                }
            }
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
        "SOS" to Icons.Default.Warning,
        "Background Verfication" to Icons.Default.Badge,
//        "Criminal Verification" to Icons.Default.VerifiedUser   // ✅ added


    )

    val columns = 3
    val itemHeight = 110.dp
    val spacing = 16.dp
    val gridHeight = calculateGridHeight(features.size, columns, itemHeight, spacing)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {

        Column(modifier = Modifier.padding(20.dp)) {

            Text("Upcoming Features", fontSize = 18.sp, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(20.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                userScrollEnabled = false,
                verticalArrangement = Arrangement.spacedBy(spacing),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gridHeight)
            ) {
                items(features) { (title, icon) ->
                    ModernFeatureItem(title, icon) {}
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
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Icon(icon, null, tint = BrandGreen, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(8.dp))

            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}