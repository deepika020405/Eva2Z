package com.developer.ev2z.ui.screens.homeScreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import org.jetbrains.compose.resources.painterResource
import eva2z_connect_multiplatform.composeapp.generated.resources.Res
import eva2z_connect_multiplatform.composeapp.generated.resources.homebanner
import eva2z_connect_multiplatform.composeapp.generated.resources.homebanner1
import eva2z_connect_multiplatform.composeapp.generated.resources.homebanner2
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
    onCriminalVerification: () -> Unit,   // ✅ ADD THIS
    onDriverScore: () -> Unit ,            // ✅ NEW — Driver Score navigation
    onParking: () -> Unit,           // ✅ CHANGED — new param
    onSosTag: () -> Unit,              // ✅ NEW — SOS Tag
    onOpenCommunity: () -> Unit,
    onInstallationSupport: () -> Unit = {},
    onOpenAllFeatures: () -> Unit = {}
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
                    onOpenAddDevice = onOpenAddDevice,
                    onOpenCommunity = onOpenCommunity,
                    onParking     = onParking,         // ✅ CHANGED — pass through
                    onSosTag      = onSosTag,    // ✅ NEW — pass karo
                    onInstallationSupport = onInstallationSupport
                )
            }

            item { ModernFeaturesSection(
                onDriverScore = onDriverScore,  // ✅ NEW — pass karo
//                onParking     = onParking,         // ✅ CHANGED — pass through
//                onSosTag      = onSosTag    // ✅ NEW — pass karo


            ) }

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
        Res.drawable.homebanner1,
        Res.drawable.homebanner2
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
    onOpenAddDevice:  () -> Unit,
    onOpenCommunity: () -> Unit,
    onParking: () -> Unit,            // ✅ CHANGED — new param
    onSosTag: () -> Unit,              // ✅ NEW — SOS Tag param
    onInstallationSupport: () -> Unit = {}
) {

    val features = listOf(
        QuickActionItem("Live Location", Icons.Default.LocationOn, onMyDevices),
        QuickActionItem("Create Geofencing", Icons.Default.Security, onGeoFence),
        QuickActionItem("History Playback", Icons.Default.History, onHistory),
        QuickActionItem("Engine On/Off", Icons.Default.PowerSettingsNew, onManageDevice),
        QuickActionItem("Document Vault", Icons.Default.Folder, onOpenDocuments),
        QuickActionItem("Device Report", Icons.Default.Assessment, onOpenDeviceReport),
        QuickActionItem("Add Device", Icons.Default.Add, onOpenAddDevice) ,
        QuickActionItem("Vechicle TAG Registration",Icons.Default.LocalParking,   onParking),  // ✅ CHANGED
        QuickActionItem(" Vechicle TAG Alert",Icons.Default.Warning,onSosTag),  // ✅ WIRED
        QuickActionItem(
            "Criminal Verification",
            Icons.Default.VerifiedUser,
            onCriminalVerification   // ✅ connect
        ),

        QuickActionItem(
            "Community",
            Icons.Default.Groups,
            onOpenCommunity   // ✅ connect
        ),
        QuickActionItem(
            "Installation Support",
            Icons.Default.Build,
            onInstallationSupport
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
fun ModernFeaturesSection(
    onDriverScore: () -> Unit,
//    onParking: () -> Unit,            // ✅ CHANGED — new param
//    onSosTag: () -> Unit              // ✅ NEW — SOS Tag param


) {

    val features = listOf(
        QuickActionItem("Driver Score", Icons.Default.EmojiEvents){},
        QuickActionItem("Service Market", Icons.Default.Build) {},
        QuickActionItem("Repair Market", Icons.Default.DirectionsCar) {},
        QuickActionItem("Charging", Icons.Default.EvStation) {},
//        QuickActionItem("Parking",Icons.Default.LocalParking,   onParking),  // ✅ CHANGED
//        QuickActionItem("SOS Parking Tag",Icons.Default.Warning,onSosTag),  // ✅ WIRED
        QuickActionItem("Background Verification", Icons.Default.Badge) {}
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
                items(features) { item ->
                    ModernFeatureItem(item.title, item.icon, item.onClick)
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


// ================= SHARED MODELS (used by AllFeaturesScreen & HomeNavGraph) =================

data class QuickAction(
    val title: String,
    val desc: String,
    val icon: ImageVector,
    val tint: Color,
    val onClick: () -> Unit
)

// Single source of truth for the vehicle-feature list — used by AllFeaturesScreen (full grid).
fun buildVehicleFeatures(
    onOpenLiveLocation: () -> Unit,
    onGeoFence: () -> Unit,
    onManageDevice: () -> Unit,
    onGpsData: () -> Unit,
    onHistory: () -> Unit,
    onOpenDocuments: () -> Unit,
    onOpenDeviceReport: () -> Unit,
    onOpenAddDevice: () -> Unit,
    onParking: () -> Unit,
    onSosTag: () -> Unit,
    onCriminalVerification: () -> Unit,
    onOpenCommunity: () -> Unit,
    onInstallationSupport: () -> Unit,
): List<QuickAction> {
    val GreenPrimary = Color(0xFF00C853)
    val GreenDark    = Color(0xFF00A844)
    val RedColor     = Color(0xFFDC2626)
    val BlueColor    = Color(0xFF3B82F6)
    val AmberColor   = Color(0xFFF59E0B)
    return listOf(
        QuickAction("Live\nLocation",    "Track your vehicle in real-time on map",        Icons.Default.LocationOn,       GreenPrimary, onOpenLiveLocation),
        QuickAction("Geo\nFencing",      "Set safe zones, get breach alerts instantly",   Icons.Default.Security,         BlueColor,    onGeoFence),
        QuickAction("Engine\nOn/Off",    "Remotely cut or start your engine",             Icons.Default.PowerSettingsNew, RedColor,     onManageDevice),
        QuickAction("Speed\nAlert",      "Alert when vehicle crosses speed limit",        Icons.Default.Warning,          AmberColor,   onGpsData),
        QuickAction("History\nPlayback", "Replay past trips on the map",                  Icons.Default.History,          AmberColor,   onHistory),
        QuickAction("Document\nVault",   "Securely store RC, Insurance, PUC & DL",        Icons.Default.Folder,           GreenDark,    onOpenDocuments),
        QuickAction("Device\nReport",    "Daily summary of trips, mileage & alerts",      Icons.Default.Assessment,       BlueColor,    onOpenDeviceReport),
        QuickAction("Add\nDevice",       "Pair a new GPS tracker to your account",        Icons.Default.Add,              GreenPrimary, onOpenAddDevice),
        QuickAction("TAG\nRegistration", "Register your vehicle TAG for verification",    Icons.Default.LocalParking,     AmberColor,   onParking),
        QuickAction("TAG\nAlert",        "Get SOS alerts when someone scans your TAG",    Icons.Default.Warning,          RedColor,     onSosTag),
        QuickAction("Criminal\nVerify",  "Run a background check on a driver",            Icons.Default.VerifiedUser,     BlueColor,    onCriminalVerification),
        QuickAction("Community",         "Stay updated with nearby vehicle alerts",       Icons.Default.Groups,           GreenPrimary, onOpenCommunity),
        QuickAction("Install\nSupport",  "Book an installation expert at your doorstep",  Icons.Default.Build,            AmberColor,   onInstallationSupport),
    )
}

// FeatureCard used by AllFeaturesScreen
@Composable
fun FeatureCard(feat: QuickAction, modifier: Modifier = Modifier.width(148.dp)) {
    val palette = LocalHomeTheme.current.palette
    val CardBg = palette.cardBg
    val TextPrimary = palette.textPrimary
    val TextSecondary = palette.textSecondary
    val SoftShadow = palette.softShadow
    val BorderSoft = palette.borderSoft
    Box(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = SoftShadow,
                spotColor = SoftShadow
            )
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .border(0.5.dp, BorderSoft, RoundedCornerShape(20.dp))
            .clickable { feat.onClick() }
            .padding(14.dp)
    ) {
        // Soft tinted glow blob
        Box(
            modifier = Modifier
                .size(110.dp)
                .align(Alignment.TopEnd)
                .offset(x = 35.dp, y = (-35).dp)
                .background(
                    Brush.radialGradient(listOf(feat.tint.copy(0.18f), Color.Transparent)),
                    CircleShape
                )
        )
        Column {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(feat.tint.copy(0.18f), feat.tint.copy(0.08f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = feat.icon,
                    contentDescription = feat.title,
                    tint = feat.tint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                feat.title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 17.sp
            )
            Spacer(Modifier.height(3.dp))
            Text(
                feat.desc,
                color = TextSecondary,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


// ===============================================================================
// ========================= NEW UI CODE (COMMENTED OUT) =========================
// ===============================================================================
// To restore the new UI, uncomment everything below and comment out the old UI above.

/*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow

// ─── Static accent colors (theme-independent) ────────────────────────────────
private val GreenPrimary  = Color(0xFF00C853)
private val GreenDark     = Color(0xFF00A844)
private val AmberColor    = Color(0xFFF59E0B)
private val AmberLight    = Color(0xFFFDE68A)
private val RedColor      = Color(0xFFDC2626)
private val BlueColor     = Color(0xFF3B82F6)

// ─── Data Models ──────────────────────────────────────────────────────────────
data class DocItem(
    val emoji: String,
    val name: String,
    val sub: String,
    val status: DocStatus
)
enum class DocStatus { VALID, EXPIRED, EXPIRING, MISSING }

data class CommunityAlert(
    val initial: String,
    val avatarBg: Color,
    val avatarText: Color,
    val message: String,
    val time: String
)

data class QuickAction(
    val title: String,
    val desc: String,
    val icon: ImageVector,
    val tint: Color,
    val onClick: () -> Unit
)

fun buildVehicleFeatures(
    onOpenLiveLocation: () -> Unit,
    onGeoFence: () -> Unit,
    onManageDevice: () -> Unit,
    onGpsData: () -> Unit,
    onHistory: () -> Unit,
    onOpenDocuments: () -> Unit,
    onOpenDeviceReport: () -> Unit,
    onOpenAddDevice: () -> Unit,
    onParking: () -> Unit,
    onSosTag: () -> Unit,
    onCriminalVerification: () -> Unit,
    onOpenCommunity: () -> Unit,
    onInstallationSupport: () -> Unit,
): List<QuickAction> {
    val GreenPrimary = Color(0xFF00C853)
    val GreenDark    = Color(0xFF00A844)
    val RedColor     = Color(0xFFDC2626)
    val BlueColor    = Color(0xFF3B82F6)
    val AmberColor   = Color(0xFFF59E0B)
    return listOf(
        QuickAction("Live\nLocation",    "Track your vehicle in real-time on map",        Icons.Default.LocationOn,       GreenPrimary, onOpenLiveLocation),
        QuickAction("Geo\nFencing",      "Set safe zones, get breach alerts instantly",   Icons.Default.Security,         BlueColor,    onGeoFence),
        QuickAction("Engine\nOn/Off",    "Remotely cut or start your engine",             Icons.Default.PowerSettingsNew, RedColor,     onManageDevice),
        QuickAction("Speed\nAlert",      "Alert when vehicle crosses speed limit",        Icons.Default.Warning,          AmberColor,   onGpsData),
        QuickAction("History\nPlayback", "Replay past trips on the map",                  Icons.Default.History,          AmberColor,   onHistory),
        QuickAction("Document\nVault",   "Securely store RC, Insurance, PUC & DL",        Icons.Default.Folder,           GreenDark,    onOpenDocuments),
        QuickAction("Device\nReport",    "Daily summary of trips, mileage & alerts",      Icons.Default.Assessment,       BlueColor,    onOpenDeviceReport),
        QuickAction("Add\nDevice",       "Pair a new GPS tracker to your account",        Icons.Default.Add,              GreenPrimary, onOpenAddDevice),
        QuickAction("TAG\nRegistration", "Register your vehicle TAG for verification",    Icons.Default.LocalParking,     AmberColor,   onParking),
        QuickAction("TAG\nAlert",        "Get SOS alerts when someone scans your TAG",    Icons.Default.Warning,          RedColor,     onSosTag),
        QuickAction("Criminal\nVerify",  "Run a background check on a driver",            Icons.Default.VerifiedUser,     BlueColor,    onCriminalVerification),
        QuickAction("Community",         "Stay updated with nearby vehicle alerts",       Icons.Default.Groups,           GreenPrimary, onOpenCommunity),
        QuickAction("Install\nSupport",  "Book an installation expert at your doorstep",  Icons.Default.Build,            AmberColor,   onInstallationSupport),
    )
}

// ================= NEW MAIN SCREEN =================

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
    onCriminalVerification: () -> Unit,
    onDriverScore: () -> Unit,
    onParking: () -> Unit,
    onSosTag: () -> Unit,
    onOpenCommunity: () -> Unit,
    onInstallationSupport: () -> Unit = {},
    onOpenAllFeatures: () -> Unit = {}
) {
    val docs = listOf(
        DocItem("\uD83D\uDCC4", "RC Book",         "Expires 2031",   DocStatus.VALID),
        DocItem("\uD83D\uDEE1\uFE0F", "Insurance", "Expired 12 Jan", DocStatus.EXPIRED),
        DocItem("\uD83D\uDCA8", "PUC",             "33 days left",   DocStatus.EXPIRING),
        DocItem("\uD83E\uDEAA", "Driving Licence", "Tap to upload",  DocStatus.MISSING),
    )
    val alerts = listOf(
        CommunityAlert("R", Color(0xFFFEE2E2), RedColor,  "Bike stolen near Powai Lake — silver Activa, DL plate", "1h ago"),
        CommunityAlert("A", Color(0xFFDCFCE7), GreenDark, "Found keys near D-Mart Versova — contact if yours",     "3h ago"),
    )
    val vehicleFeatures = buildVehicleFeatures(
        onOpenLiveLocation     = onOpenLiveLocation,
        onGeoFence             = onGeoFence,
        onManageDevice         = onManageDevice,
        onGpsData              = onGpsData,
        onHistory              = onHistory,
        onOpenDocuments        = onOpenDocuments,
        onOpenDeviceReport     = onOpenDeviceReport,
        onOpenAddDevice        = onOpenAddDevice,
        onParking              = onParking,
        onSosTag               = onSosTag,
        onCriminalVerification = onCriminalVerification,
        onOpenCommunity        = onOpenCommunity,
        onInstallationSupport  = onInstallationSupport,
    )

    val palette = LocalHomeTheme.current.palette
    Scaffold(containerColor = palette.pageBg) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isTablet = maxWidth >= 600.dp
            val hPad: Dp = if (isTablet) 24.dp else 16.dp
            val featureCardWidth: Dp = if (isTablet) 168.dp else 148.dp
            val sectionGap: Dp = if (isTablet) 20.dp else 16.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                HeroSection(userName, hPad)
                Spacer(Modifier.height(sectionGap))
                SafetyScoreCard(hPad)
                Spacer(Modifier.height(sectionGap))
                DocumentVaultSection(docs, onOpenDocuments, hPad)
                Spacer(Modifier.height(sectionGap))
                VehicleFeaturesSection(
                    actions = vehicleFeatures,
                    onViewAll = onOpenAllFeatures,
                    horizontalPadding = hPad,
                    cardWidth = featureCardWidth
                )
                Spacer(Modifier.height(sectionGap))
                GpsUpsellBanner(onGetGps = onOpenAddDevice, horizontalPadding = hPad)
                Spacer(Modifier.height(sectionGap))
                CommunityAlertsSection(alerts, onOpenCommunity, hPad)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ─── Hero (theme-aware premium) ───────────────────────────────────────────────
@Composable
fun HeroSection(userName: String, horizontalPadding: Dp = 16.dp) {
    val palette = LocalHomeTheme.current.palette
    val HeroGradStart = palette.heroGradStart
    val HeroGradEnd   = palette.heroGradEnd
    val TextPrimary   = palette.textPrimary
    val TextSecondary = palette.textSecondary
    val SoftShadow    = palette.softShadow
    val BorderSoft    = palette.borderSoft
    val SurfaceColor  = if (palette.isDark) palette.cardBg else Color.White
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(HeroGradStart, HeroGradEnd))
            )
            .padding(horizontal = horizontalPadding, vertical = 22.dp)
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-60).dp)
                .background(
                    Brush.radialGradient(
                        listOf(GreenPrimary.copy(0.10f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        "Welcome back",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        userName,
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.4).sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .shadow(
                            elevation = 3.dp,
                            shape = RoundedCornerShape(14.dp),
                            ambientColor = SoftShadow,
                            spotColor = SoftShadow
                        )
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceColor)
                        .border(0.5.dp, BorderSoft, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsNone,
                        contentDescription = "Notifications",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-6).dp, y = 6.dp)
                            .size(8.dp)
                            .background(GreenPrimary, CircleShape)
                            .border(1.5.dp, SurfaceColor, CircleShape)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = SoftShadow,
                        spotColor = SoftShadow
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceColor)
                    .border(0.5.dp, BorderSoft, RoundedCornerShape(20.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(listOf(GreenPrimary, GreenDark))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = "QR",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        "Honda City \u2014 TAG Active",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "MH 02 BK 4421 \u00B7 3 scans this month",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(GreenPrimary.copy(0.12f))
                        .border(0.5.dp, GreenPrimary.copy(0.3f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        "\u25CF LIVE",
                        color = GreenDark,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFFFBEB))
                    .border(0.5.dp, Color(0xFFFDE68A), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(Color(0xFFF59E0B), CircleShape)
                )
                Column {
                    Text(
                        "QR Scanned 2h ago",
                        color = Color(0xFF92400E),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "near Andheri Station, Mumbai",
                        color = Color(0xFFB45309),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

// ─── Safety Score ─────────────────────────────────────────────────────────────
@Composable
fun SafetyScoreCard(horizontalPadding: Dp = 16.dp) {
    val palette = LocalHomeTheme.current.palette
    val CardBg = palette.cardBg
    val TextPrimary = palette.textPrimary
    val TextSecondary = palette.textSecondary
    val TextMuted = palette.textMuted
    val SoftShadow = palette.softShadow
    val BorderSoft = palette.borderSoft
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = SoftShadow,
                spotColor = SoftShadow
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(0.5.dp, BorderSoft)
    ) {
        Column(Modifier.padding(14.dp, 14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Vehicle Safety Score",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        "Complete profile to improve",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "45",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AmberColor,
                        lineHeight = 28.sp
                    )
                    Text(" /100", fontSize = 11.sp, color = TextMuted)
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFF3F4F6))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(listOf(AmberColor, AmberLight))
                        )
                )
            }
            Spacer(Modifier.height(7.dp))
            Text(
                "\uD83D\uDCA1 Add your Insurance doc to reach 65 and unlock more features",
                fontSize = 10.sp,
                color = AmberColor,
                lineHeight = 14.sp
            )
        }
    }
}

// ─── Document Vault ───────────────────────────────────────────────────────────
@Composable
fun DocumentVaultSection(
    docs: List<DocItem>,
    onViewAll: () -> Unit,
    horizontalPadding: Dp = 16.dp
) {
    val palette = LocalHomeTheme.current.palette
    val TextPrimary = palette.textPrimary
    Column(Modifier.padding(horizontal = horizontalPadding)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "\uD83D\uDCC1 Document Vault",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                "View all →",
                fontSize = 10.sp,
                color = GreenDark,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onViewAll() }
            )
        }
        Spacer(Modifier.height(9.dp))
        docs.chunked(2).forEach { rowDocs ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                rowDocs.forEach { doc ->
                    DocCard(doc, onViewAll, Modifier.weight(1f))
                }
                if (rowDocs.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(7.dp))
        }
    }
}

@Composable
fun DocCard(doc: DocItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalHomeTheme.current.palette
    val CardBg = palette.cardBg
    val TextPrimary = palette.textPrimary
    val TextSecondary = palette.textSecondary
    val SoftShadow = palette.softShadow
    val BorderSoft = palette.borderSoft
    val (badgeBg, badgeText, badgeLabel) = when (doc.status) {
        DocStatus.VALID    -> Triple(Color(0xFFDCFCE7), Color(0xFF16A34A), "VALID")
        DocStatus.EXPIRED  -> Triple(Color(0xFFFEE2E2), RedColor,          "EXPIRED")
        DocStatus.EXPIRING -> Triple(Color(0xFFFEF9C3), Color(0xFFCA8A04), "EXPIRING")
        DocStatus.MISSING  -> Triple(Color(0xFFF3F4F6), TextSecondary,     "ADD")
    }
    Card(
        modifier = modifier
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = SoftShadow,
                spotColor = SoftShadow
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(0.5.dp, BorderSoft)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(doc.emoji, fontSize = 20.sp)
                Box(
                    modifier = Modifier
                        .background(badgeBg, RoundedCornerShape(20.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        badgeLabel,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeText
                    )
                }
            }
            Spacer(Modifier.height(7.dp))
            Text(
                doc.name,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                doc.sub,
                fontSize = 9.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

// ─── Vehicle Features (carousel — shows first 4, theme-aware) ────────────────
@Composable
fun VehicleFeaturesSection(
    actions: List<QuickAction>,
    onViewAll: () -> Unit,
    horizontalPadding: Dp = 16.dp,
    cardWidth: Dp = 148.dp
) {
    val palette = LocalHomeTheme.current.palette
    val TextPrimary = palette.textPrimary
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "\u26A1 Vehicle Features",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                "View all \u2192",
                fontSize = 11.sp,
                color = GreenDark,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onViewAll() }
            )
        }
        Spacer(Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(actions.take(4)) { action ->
                FeatureCard(action, modifier = Modifier.width(cardWidth))
            }
        }
    }
}

@Composable
fun FeatureCard(feat: QuickAction, modifier: Modifier = Modifier.width(148.dp)) {
    val palette = LocalHomeTheme.current.palette
    val CardBg = palette.cardBg
    val TextPrimary = palette.textPrimary
    val TextSecondary = palette.textSecondary
    val SoftShadow = palette.softShadow
    val BorderSoft = palette.borderSoft
    Box(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = SoftShadow,
                spotColor = SoftShadow
            )
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .border(0.5.dp, BorderSoft, RoundedCornerShape(20.dp))
            .clickable { feat.onClick() }
            .padding(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .align(Alignment.TopEnd)
                .offset(x = 35.dp, y = (-35).dp)
                .background(
                    Brush.radialGradient(listOf(feat.tint.copy(0.18f), Color.Transparent)),
                    CircleShape
                )
        )
        Column {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(feat.tint.copy(0.18f), feat.tint.copy(0.08f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = feat.icon,
                    contentDescription = feat.title,
                    tint = feat.tint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                feat.title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 17.sp
            )
            Spacer(Modifier.height(3.dp))
            Text(
                feat.desc,
                color = TextSecondary,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─── GPS Upsell (theme-aware gradient) ────────────────────────────────────────
@Composable
fun GpsUpsellBanner(onGetGps: () -> Unit, horizontalPadding: Dp = 16.dp) {
    val palette = LocalHomeTheme.current.palette
    val TextPrimary = palette.textPrimary
    val TextSecondary = palette.textSecondary
    val gradStart = if (palette.isDark) Color(0xFF0F2950) else Color(0xFFECFDF5)
    val gradEnd   = if (palette.isDark) Color(0xFF132B52) else Color(0xFFFFFFFF)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = GreenPrimary.copy(0.18f),
                spotColor = GreenPrimary.copy(0.18f)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(gradStart, gradEnd)))
            .border(0.5.dp, GreenPrimary.copy(0.25f), RoundedCornerShape(22.dp))
            .padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Unlock GPS Protection",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 20.sp
            )
            Text(
                "Your Tag identifies your vehicle.\nGPS tracks, protects & recovers it.",
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
            Row(
                modifier = Modifier.padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Starting at ", color = TextSecondary, fontSize = 11.sp)
                Text(
                    "\u20B91,499",
                    color = GreenDark,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
        Box(
            modifier = Modifier
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(15.dp),
                    ambientColor = GreenPrimary.copy(0.4f),
                    spotColor = GreenPrimary.copy(0.4f)
                )
                .clip(RoundedCornerShape(15.dp))
                .background(
                    Brush.linearGradient(listOf(GreenPrimary, GreenDark))
                )
                .clickable { onGetGps() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Get GPS",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Device \u2192",
                    color = Color.White.copy(0.85f),
                    fontSize = 9.sp
                )
            }
        }
    }
}

// ─── Community Alerts ─────────────────────────────────────────────────────────
@Composable
fun CommunityAlertsSection(
    alerts: List<CommunityAlert>,
    onOpenCommunity: () -> Unit,
    horizontalPadding: Dp = 16.dp
) {
    val palette = LocalHomeTheme.current.palette
    val CardBg = palette.cardBg
    val TextPrimary = palette.textPrimary
    val TextMuted = palette.textMuted
    val SoftShadow = palette.softShadow
    val BorderSoft = palette.borderSoft
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = SoftShadow,
                spotColor = SoftShadow
            )
            .clickable { onOpenCommunity() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(0.5.dp, BorderSoft)
    ) {
        Column(Modifier.padding(12.dp, 12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "\uD83D\uDEA8 Community Alerts",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Box(
                    modifier = Modifier
                        .background(Color(0xFFEFF6FF), RoundedCornerShape(20.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        "NEARBY",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlueColor
                    )
                }
            }
            Spacer(Modifier.height(9.dp))
            alerts.forEachIndexed { i, alert ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(alert.avatarBg, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            alert.initial,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = alert.avatarText
                        )
                    }
                    Text(
                        alert.message,
                        fontSize = 10.sp,
                        color = Color(0xFF374151),
                        lineHeight = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(alert.time, fontSize = 9.sp, color = TextMuted)
                }
                if (i < alerts.lastIndex)
                    HorizontalDivider(color = Color(0xFFF9FAFB), thickness = 0.5.dp)
            }
        }
    }
}

*/