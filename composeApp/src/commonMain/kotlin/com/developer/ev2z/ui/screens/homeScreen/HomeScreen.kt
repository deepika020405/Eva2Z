package com.developer.ev2z.ui.screens.homeScreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times

// ─────────────────────────── COLORS ────────────────────────────────

private val BrandGreen      = Color(0xFF00A859)
private val BrandGreenDark  = Color(0xFF007A40)
private val BrandGreenLight = Color(0xFFE6F7EF)
private val BackgroundColor = Color(0xFFF4F6F9)
private val CardWhite       = Color.White
private val TextPrimary     = Color(0xFF111827)
private val TextSecondary   = Color(0xFF6B7280)
private val TextHint        = Color(0xFFADB5BD)
private val EngineRed       = Color(0xFFE53935)
private val EngineRedLight  = Color(0xFFFDECEC)
private val BellBg          = Color(0xFFEEEEEE)
private val ShadowColor     = Color(0x14000000)

// ─────────────────── DATA MODEL (compatibility) ─────────────────────

data class QuickActionItem(val title: String, val icon: ImageVector, val onClick: () -> Unit)

fun calculateGridHeight(itemCount: Int, columns: Int, itemHeight: Dp, verticalSpacing: Dp): Dp {
    val rows = (itemCount + columns - 1) / columns
    return (rows * itemHeight) + ((rows - 1) * verticalSpacing)
}

// ─────────────────────────── MAIN SCREEN ───────────────────────────

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
    onOpenAllFeatures: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    unreadNotificationCount: Int = 0
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet  = maxWidth >= 600.dp
        val isCompact = maxWidth < 360.dp
        val hPad = when { isTablet -> 28.dp; isCompact -> 12.dp; else -> 16.dp }

        Scaffold(containerColor = BackgroundColor) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding()),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(bottom = 36.dp)
            ) {
                item { HomeHeader(userName, hPad, onOpenNotifications, unreadNotificationCount) }
                item {
                    ParkingTagSection(
                        onVehicleTagRegistration = onParking,
                        onVisitorChats           = onSosTag,
                        onManageTags             = onParking,
                        hPad                     = hPad
                    )
                }
                item {
                    GpsTrackingSection(
                        onLiveLocation = onOpenLiveLocation,
                        onHistory      = onHistory,
                        onEngineToggle = onManageDevice,
                        onGeofencing   = onGeoFence,
                        onDeviceReport = onOpenDeviceReport,
                        onAddDevice    = onOpenAddDevice,
                        onInstallation = onInstallationSupport,
                        hPad           = hPad
                    )
                }
                item {
                    GeneralSection(
                        onDocumentVault = onOpenDocuments,
                        onCommunity     = onOpenCommunity,
                        hPad            = hPad
                    )
                }
            }
        }
    }
}

// ─────────────────────────── HEADER ────────────────────────────────

@Composable
private fun HomeHeader(
    userName: String,
    hPad: Dp,
    onOpenNotifications: () -> Unit,
    unreadCount: Int
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = hPad, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Welcome Back,", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Normal)
            Spacer(Modifier.height(3.dp))
            Text(userName, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        }
        Box(modifier = Modifier.size(44.dp)) {
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(if (unreadCount > 0) Color(0xFFE8F5E9) else BellBg)
                    .clickable { onOpenNotifications() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector      = if (unreadCount > 0) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                    contentDescription = "Notifications",
                    tint             = if (unreadCount > 0) Color(0xFF00A859) else TextPrimary,
                    modifier         = Modifier.size(22.dp)
                )
            }
            // Unread badge — dot for 1, count for 2+
            if (unreadCount > 0) {
                if (unreadCount == 1) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                            .align(Alignment.TopEnd)
                    )
                } else {
                    Box(
                        modifier         = Modifier
                            .sizeIn(minWidth = 18.dp, minHeight = 18.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                            .align(Alignment.TopEnd)
                            .padding(horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = if (unreadCount > 9) "9+" else unreadCount.toString(),
                            fontSize   = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ───────────────────── QR PARKING TAG SECTION ──────────────────────

@Composable
private fun ParkingTagSection(
    onVehicleTagRegistration: () -> Unit,
    onVisitorChats: () -> Unit,
    onManageTags: () -> Unit,
    hPad: Dp
) {
    Column(modifier = Modifier.padding(horizontal = hPad)) {
        // Section header
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("QR Parking Tag", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Surface(
                onClick   = onManageTags,
                shape     = RoundedCornerShape(20.dp),
                color     = BrandGreenLight,
                modifier  = Modifier
            ) {
                Text(
                    "Manage Tags",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = BrandGreen,
                    modifier   = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ParkingTagCard(
                title    = "Vehicle TAG\nRegistration",
                icon     = Icons.Default.QrCode2,
                onClick  = onVehicleTagRegistration,
                modifier = Modifier.weight(1f)
            )
            ParkingTagCard(
                title    = "QR Scan\nMessages",
                icon     = Icons.Default.ChatBubbleOutline,
                onClick  = onVisitorChats,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ParkingTagCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick   = onClick,
        modifier  = modifier.height(150.dp),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Subtle green tint blob top-right
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 24.dp, y = (-24).dp)
                    .background(
                        Brush.radialGradient(listOf(BrandGreen.copy(0.10f), Color.Transparent)),
                        CircleShape
                    )
            )
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(BrandGreen.copy(0.15f), BrandGreen.copy(0.07f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    title,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = TextPrimary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// ─────────────────────── GPS TRACKING SECTION ──────────────────────

private data class GpsItem(
    val title: String,
    val icon: ImageVector,
    val tint: Color,
    val onClick: () -> Unit
)

@Composable
private fun GpsTrackingSection(
    onLiveLocation: () -> Unit,
    onHistory: () -> Unit,
    onEngineToggle: () -> Unit,
    onGeofencing: () -> Unit,
    onDeviceReport: () -> Unit,
    onAddDevice: () -> Unit,
    onInstallation: () -> Unit,
    hPad: Dp
) {
    val items = listOf(
        GpsItem("Live\nLocation",    Icons.Default.LocationOn,       BrandGreen, onLiveLocation),
        GpsItem("History\nPlayback", Icons.Default.History,          BrandGreen, onHistory),
        GpsItem("Engine\nOn/Off",    Icons.Default.PowerSettingsNew, EngineRed,  onEngineToggle),
        GpsItem("Geofencing",        Icons.Default.Security,         BrandGreen, onGeofencing),
        GpsItem("Device\nReport",    Icons.Default.Assessment,       BrandGreen, onDeviceReport),
        GpsItem("Add\nDevice",       Icons.Default.AddCircleOutline, BrandGreen, onAddDevice),
    )

    Column(modifier = Modifier.padding(horizontal = hPad)) {
        Text("GPS Tracking", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)

        Spacer(Modifier.height(16.dp))

        // 3-column grid with individual card boxes and gaps
        val rows = items.chunked(3)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEach { row ->
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { item ->
                        GpsBoxItem(item = item, modifier = Modifier.weight(1f))
                    }
                    // Fill remaining empty slots to keep alignment
                    repeat(3 - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Installation Support — premium wide card
        InstallationSupportCard(onClick = onInstallation)
    }
}

@Composable
private fun GpsBoxItem(item: GpsItem, modifier: Modifier = Modifier) {
    val bgTint = if (item.tint == EngineRed) EngineRedLight else BrandGreenLight
    Card(
        onClick   = item.onClick,
        modifier  = modifier.height(108.dp),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier         = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.icon, contentDescription = null, tint = item.tint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                item.title,
                fontSize   = 10.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary,
                textAlign  = TextAlign.Center,
                lineHeight = 13.sp
            )
        }
    }
}

@Composable
private fun InstallationSupportCard(onClick: () -> Unit) {
    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier         = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(BrandGreenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Build, null, tint = BrandGreen, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Text("Installation Support", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = TextHint, modifier = Modifier.size(22.dp))
        }
    }
}

// ─────────────────────────── GENERAL SECTION ───────────────────────

@Composable
private fun GeneralSection(
    onDocumentVault: () -> Unit,
    onCommunity: () -> Unit,
    hPad: Dp
) {
    Column(modifier = Modifier.padding(horizontal = hPad)) {
        Text("General", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)

        Spacer(Modifier.height(16.dp))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GeneralItem(title = "Document\nVault", icon = Icons.Default.FolderOpen, onClick = onDocumentVault, modifier = Modifier.weight(1f))
            GeneralItem(title = "Community",       icon = Icons.Default.Groups,     onClick = onCommunity,     modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun GeneralItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick   = onClick,
        modifier  = modifier.height(150.dp),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 24.dp, y = (-24).dp)
                    .background(
                        Brush.radialGradient(listOf(BrandGreen.copy(0.10f), Color.Transparent)),
                        CircleShape
                    )
            )
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(listOf(BrandGreen.copy(0.15f), BrandGreen.copy(0.07f)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary, lineHeight = 18.sp)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// SHARED MODELS — used by AllFeaturesScreen (do not modify)
// ══════════════════════════════════════════════════════════════════

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
        QuickAction("Live\nLocation",    "Track your vehicle in real-time on map",       Icons.Default.LocationOn,       GreenPrimary, onOpenLiveLocation),
        QuickAction("Geo\nFencing",      "Set safe zones, get breach alerts instantly",  Icons.Default.Security,         BlueColor,    onGeoFence),
        QuickAction("Engine\nOn/Off",    "Remotely cut or start your engine",            Icons.Default.PowerSettingsNew, RedColor,     onManageDevice),
        QuickAction("Speed\nAlert",      "Alert when vehicle crosses speed limit",       Icons.Default.Warning,          AmberColor,   onGpsData),
        QuickAction("History\nPlayback", "Replay past trips on the map",                 Icons.Default.History,          AmberColor,   onHistory),
        QuickAction("Document\nVault",   "Securely store RC, Insurance, PUC & DL",       Icons.Default.Folder,           GreenDark,    onOpenDocuments),
        QuickAction("Device\nReport",    "Daily summary of trips, mileage & alerts",     Icons.Default.Assessment,       BlueColor,    onOpenDeviceReport),
        QuickAction("Add\nDevice",       "Pair a new GPS tracker to your account",       Icons.Default.Add,              GreenPrimary, onOpenAddDevice),
        QuickAction("TAG\nRegistration", "Register your vehicle TAG for verification",   Icons.Default.LocalParking,     AmberColor,   onParking),
        QuickAction("TAG\nAlert",        "Get SOS alerts when someone scans your TAG",   Icons.Default.Warning,          RedColor,     onSosTag),
        QuickAction("Criminal\nVerify",  "Run a background check on a driver",           Icons.Default.VerifiedUser,     BlueColor,    onCriminalVerification),
        QuickAction("Community",         "Stay updated with nearby vehicle alerts",      Icons.Default.Groups,           GreenPrimary, onOpenCommunity),
        QuickAction("Install\nSupport",  "Book an installation expert at your doorstep", Icons.Default.Build,            AmberColor,   onInstallationSupport),
    )
}

@Composable
fun FeatureCard(feat: QuickAction, modifier: Modifier = Modifier.width(148.dp)) {
    val palette       = LocalHomeTheme.current.palette
    val CardBg        = palette.cardBg
    val TextPrimary   = palette.textPrimary
    val TextSecondary = palette.textSecondary
    val SoftShadow    = palette.softShadow
    val BorderSoft    = palette.borderSoft
    Box(
        modifier = modifier
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(20.dp), ambientColor = SoftShadow, spotColor = SoftShadow)
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
                .background(Brush.radialGradient(listOf(feat.tint.copy(0.18f), Color.Transparent)), CircleShape)
        )
        Column {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Brush.linearGradient(listOf(feat.tint.copy(0.18f), feat.tint.copy(0.08f)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(feat.icon, feat.title, tint = feat.tint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(feat.title, color = TextPrimary,   fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 17.sp)
            Spacer(Modifier.height(3.dp))
            Text(feat.desc,  color = TextSecondary, fontSize = 10.sp, lineHeight = 14.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}