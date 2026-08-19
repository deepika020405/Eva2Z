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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.developer.ev2z.ui.screens.weather.CurrentWeather
import com.developer.ev2z.ui.screens.weather.WeatherKind
import com.developer.ev2z.ui.screens.weather.formatTemperature
import kotlin.math.roundToInt
import com.developer.ev2z.theme.AppPageBg

// ─────────────────────────── COLORS ────────────────────────────────

private val BrandGreen      = Color(0xFF00A859)
private val BrandGreenDark  = Color(0xFF007A40)
private val BrandGreenLight = Color(0xFFE6F7EF)
private val BackgroundColor = AppPageBg
private val CardWhite       = Color.White
private val TextPrimary     = Color(0xFF111827)
private val TextSecondary   = Color(0xFF6B7280)
private val TextHint        = Color(0xFFADB5BD)
private val EngineRed       = Color(0xFFE53935)
private val EngineRedLight  = Color(0xFFFDECEC)
private val BellBg          = Color(0xFFEEEEEE)
private val ShadowColor     = Color(0x14000000)

// Locked-section palette (grey)
private val LockedIcon      = Color(0xFF9CA3AF)
private val LockedIconBg    = Color(0xFFE9ECEF)
private val LockedText      = Color(0xFF9CA3AF)
private val LockedCardBg    = Color(0xFFF3F4F6)

// ─────────────────── DATA MODEL (compatibility) ─────────────────────

/**
 * Which home section is locked based on the plan the user picked during onboarding:
 *  - PARKING → user chose GPS, so the QR Parking Tag section is locked
 *  - GPS     → user chose the QR tag, so the GPS Tracking section is locked
 *  - NONE    → nothing locked
 */
enum class HomeSectionLock { NONE, PARKING, GPS }

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
    unreadNotificationCount: Int = 0,
    parkingLocked: Boolean = false,
    gpsLocked: Boolean = false,
    onNavigateToShop: () -> Unit = {},
    onFuelPrice: () -> Unit = {},
    // Fuel rates for wherever the user currently is — null until a state is resolved.
    fuelLocationLabel: String = "your area",
    fuelPetrolPrice: Double? = null,
    fuelDieselPrice: Double? = null,
    onWeather: () -> Unit = {},
    // Today's conditions for the button above the fuel card. Null until a fix lands.
    weatherNow: CurrentWeather? = null
) {
    // Which locked section the user tapped — drives the popup content. null = no popup.
    var lockedDialogFor by remember { mutableStateOf<HomeSectionLock?>(null) }

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
                    Column(modifier = Modifier.padding(horizontal = hPad)) {
                        // Sits above the fuel card, pushed to the right edge.
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            WeatherHomeButton(now = weatherNow, onClick = onWeather)
                        }
                        Spacer(Modifier.height(8.dp))
                        FuelPriceHomeWidget(
                            locationLabel = fuelLocationLabel,
                            petrolPrice   = fuelPetrolPrice,
                            dieselPrice   = fuelDieselPrice,
                            onClick       = onFuelPrice
                        )
                    }
                }
                item {
                    ParkingTagSection(
                        onVehicleTagRegistration = onParking,
                        onVisitorChats           = onSosTag,
                        onManageTags             = onParking,
                        hPad                     = hPad,
                        locked                   = parkingLocked,
                        onLocked                 = { lockedDialogFor = HomeSectionLock.PARKING }
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
                        hPad           = hPad,
                        locked         = gpsLocked,
                        onLocked       = { lockedDialogFor = HomeSectionLock.GPS }
                    )
                }
                item {
                    GeneralSection(
                        onDocumentVault        = onOpenDocuments,
                        onCommunity            = onOpenCommunity,
                        onCriminalVerification = onCriminalVerification,
                        hPad                   = hPad
                    )
                }
            }
        }

        lockedDialogFor?.let { section ->
            LockedFeatureDialog(
                lockedSection = section,
                onDismiss = { lockedDialogFor = null },
                onBuy = {
                    lockedDialogFor = null
                    onNavigateToShop()
                }
            )
        }
    }
}

// ─────────────────────── WEATHER BUTTON ────────────────────────────

/**
 * Compact "today's weather" pill above the fuel price card. Shows the live temperature
 * and condition once a fix lands, and a plain "Weather" prompt until then. Tapping it
 * invokes [onClick] to open the full weather screen.
 */
@Composable
fun WeatherHomeButton(
    now: CurrentWeather?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val kind = now?.condition?.kind ?: WeatherKind.UNKNOWN
    val isDay = now?.isDay ?: true
    val accent = weatherAccentColor(kind, isDay)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(accent.copy(alpha = 0.10f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector        = weatherHomeIcon(kind, isDay),
            contentDescription = null,
            tint               = accent,
            modifier           = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (now != null) {
                "${formatTemperature(now.temperature)} · ${now.condition.label}"
            } else {
                "Today's weather"
            },
            fontSize   = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color      = TextPrimary,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(2.dp))
        Icon(
            imageVector        = Icons.Default.ChevronRight,
            contentDescription = "Open weather updates",
            tint               = accent,
            modifier           = Modifier.size(15.dp)
        )
    }
}

private fun weatherHomeIcon(kind: WeatherKind, isDay: Boolean): ImageVector = when (kind) {
    WeatherKind.CLEAR         -> if (isDay) Icons.Default.WbSunny else Icons.Default.DarkMode
    WeatherKind.PARTLY_CLOUDY -> if (isDay) Icons.Default.WbCloudy else Icons.Default.NightsStay
    WeatherKind.CLOUDY        -> Icons.Default.Cloud
    WeatherKind.FOG           -> Icons.Default.BlurOn
    WeatherKind.DRIZZLE       -> Icons.Default.Grain
    WeatherKind.RAIN          -> Icons.Default.Umbrella
    WeatherKind.SNOW          -> Icons.Default.AcUnit
    WeatherKind.THUNDERSTORM  -> Icons.Default.Bolt
    WeatherKind.UNKNOWN       -> Icons.Default.WbSunny
}

private fun weatherAccentColor(kind: WeatherKind, isDay: Boolean): Color = when {
    !isDay -> Color(0xFF3F51B5)
    kind == WeatherKind.CLEAR -> Color(0xFFF5A623)
    kind == WeatherKind.PARTLY_CLOUDY -> Color(0xFF4A90D9)
    kind == WeatherKind.CLOUDY || kind == WeatherKind.FOG -> Color(0xFF78909C)
    kind == WeatherKind.DRIZZLE || kind == WeatherKind.RAIN -> Color(0xFF2F80ED)
    kind == WeatherKind.SNOW -> Color(0xFF4FC3F7)
    kind == WeatherKind.THUNDERSTORM -> Color(0xFF6C63A6)
    else -> BrandGreen
}

// ─────────────────────── FUEL PRICE WIDGET ─────────────────────────

/**
 * Slim widget that sits right below the "Welcome back" header and above the
 * QR Parking Tag section. Tapping it invokes [onClick] (e.g. open a fuel
 * price screen). Adapted to the HomeScreen palette and KMP-safe formatting.
 */
@Composable
fun FuelPriceHomeWidget(
    locationLabel: String,
    petrolPrice: Double?,
    dieselPrice: Double?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick   = onClick,
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border    = BorderStroke(1.5.dp, BrandGreen)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(BrandGreenLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.LocalGasStation,
                        contentDescription = null,
                        tint               = BrandGreen,
                        modifier           = Modifier.size(18.dp)
                    )
                }
                Column(modifier = Modifier.padding(start = 10.dp)) {
                    Text(
                        text     = "Fuel price today · $locationLabel",
                        fontSize = 11.sp,
                        color    = TextSecondary
                    )
                    Text(
                        text = if (petrolPrice != null && dieselPrice != null) {
                            "Petrol ₹${formatPrice(petrolPrice)}  ·  Diesel ₹${formatPrice(dieselPrice)}"
                        } else {
                            "Tap to see today's rates"
                        },
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color      = TextPrimary
                    )
                }
            }
            Icon(
                imageVector        = Icons.Default.ChevronRight,
                contentDescription = "View fuel price details",
                tint               = BrandGreen,
                modifier           = Modifier.size(18.dp)
            )
        }
    }
}

/** KMP-safe 2-decimal formatter (String.format is JVM-only, unavailable in commonMain). */
private fun formatPrice(value: Double): String {
    val cents  = (value * 100).roundToInt()
    val rupees = cents / 100
    val paise  = cents % 100
    return "$rupees.${paise.toString().padStart(2, '0')}"
}

// ─────────────────────── LOCKED FEATURE DIALOG ─────────────────────

@Composable
private fun LockedFeatureDialog(
    lockedSection: HomeSectionLock,
    onDismiss: () -> Unit,
    onBuy: () -> Unit
) {
    val featureName = when (lockedSection) {
        HomeSectionLock.GPS     -> "GPS Tracking"
        HomeSectionLock.PARKING -> "QR Parking Tag"
        else                    -> "This feature"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(BrandGreenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(26.dp))
            }
        },
        title = {
            Text("$featureName is locked", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        },
        text = {
            Text(
                "You need to buy this to unlock $featureName. Tap Buy to explore it in the shop.",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onBuy,
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Buy", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary, fontWeight = FontWeight.SemiBold)
            }
        },
        containerColor = CardWhite
    )
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
    hPad: Dp,
    locked: Boolean = false,
    onLocked: () -> Unit = {}
) {
    Column(modifier = Modifier.padding(horizontal = hPad)) {
        // Section header
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "QR Parking Tag",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (locked) LockedText else TextPrimary
                )
                if (locked) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.Lock, contentDescription = "Locked", tint = LockedText, modifier = Modifier.size(16.dp))
                }
            }
            Surface(
                onClick   = if (locked) onLocked else onManageTags,
                shape     = RoundedCornerShape(20.dp),
                color     = if (locked) LockedIconBg else BrandGreenLight,
                modifier  = Modifier
            ) {
                Text(
                    "Manage Tags",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (locked) LockedText else BrandGreen,
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
                onClick  = if (locked) onLocked else onVehicleTagRegistration,
                locked   = locked,
                modifier = Modifier.weight(1f)
            )
            ParkingTagCard(
                title    = "QR Scan\nMessages",
                icon     = Icons.Default.ChatBubbleOutline,
                onClick  = if (locked) onLocked else onVisitorChats,
                locked   = locked,
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
    modifier: Modifier = Modifier,
    locked: Boolean = false
) {
    Card(
        onClick   = onClick,
        modifier  = modifier.height(150.dp),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = if (locked) LockedCardBg else CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border    = if (locked) BorderStroke(1.dp, Color.White) else null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Subtle green tint blob top-right (hidden when locked)
            if (!locked) {
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
            }
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
                            if (locked) Brush.linearGradient(listOf(LockedIconBg, LockedIconBg))
                            else Brush.linearGradient(
                                listOf(BrandGreen.copy(0.15f), BrandGreen.copy(0.07f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = if (locked) LockedIcon else BrandGreen, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    title,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = if (locked) LockedText else TextPrimary,
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
    hPad: Dp,
    locked: Boolean = false,
    onLocked: () -> Unit = {}
) {
    // Add Device is intentionally NOT in this grid — it's a separate, always-unlocked button below.
    val items = listOf(
        GpsItem("Live\nLocation",       Icons.Default.LocationOn,       BrandGreen, onLiveLocation),
        GpsItem("History\nPlayback",    Icons.Default.History,          BrandGreen, onHistory),
        GpsItem("Engine\nOn/Off",       Icons.Default.PowerSettingsNew, EngineRed,  onEngineToggle),
        GpsItem("Geofencing",           Icons.Default.Security,         BrandGreen, onGeofencing),
        GpsItem("Device\nReport",       Icons.Default.Assessment,       BrandGreen, onDeviceReport),
        GpsItem("Installation\nSupport", Icons.Default.Build,           BrandGreen, onInstallation),
    )

    Column(modifier = Modifier.padding(horizontal = hPad)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "GPS Tracking",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (locked) LockedText else TextPrimary
            )
            if (locked) {
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Default.Lock, contentDescription = "Locked", tint = LockedText, modifier = Modifier.size(16.dp))
            }
        }

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
                        GpsBoxItem(
                            item     = item,
                            locked   = locked,
                            onLocked = onLocked,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Fill remaining empty slots to keep alignment
                    repeat(3 - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Add Device — standalone, responsive, and always unlocked (never gated by `locked`)
        AddDeviceButton(onClick = onAddDevice)
    }
}

@Composable
private fun AddDeviceButton(onClick: () -> Unit) {
    // Full-width so it scales with any screen size; always active regardless of section lock.
    Button(
        onClick         = onClick,
        modifier        = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp),
        shape           = RoundedCornerShape(18.dp),
        colors          = ButtonDefaults.buttonColors(
            containerColor = BrandGreenLight,
            contentColor   = BrandGreen
        ),
        elevation       = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Icon(
            Icons.Default.AddCircleOutline,
            contentDescription = null,
            tint = BrandGreen,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "Add Device",
            fontSize   = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = BrandGreen
        )
    }
}

@Composable
private fun GpsBoxItem(
    item: GpsItem,
    modifier: Modifier = Modifier,
    locked: Boolean = false,
    onLocked: () -> Unit = {}
) {
    val bgTint = when {
        locked                 -> LockedIconBg
        item.tint == EngineRed -> EngineRedLight
        else                   -> BrandGreenLight
    }
    Card(
        onClick   = if (locked) onLocked else item.onClick,
        modifier  = modifier.height(108.dp),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = if (locked) LockedCardBg else CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border    = if (locked) BorderStroke(1.dp, Color.White) else null
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
                Icon(item.icon, contentDescription = null, tint = if (locked) LockedIcon else item.tint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                item.title,
                fontSize   = 10.sp,
                fontWeight = FontWeight.Bold,
                color      = if (locked) LockedText else TextPrimary,
                textAlign  = TextAlign.Center,
                lineHeight = 13.sp
            )
        }
    }
}

@Composable
private fun InstallationSupportCard(onClick: () -> Unit, locked: Boolean = false) {
    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = if (locked) LockedCardBg else CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border    = if (locked) BorderStroke(1.dp, Color.White) else null
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
                    .background(if (locked) LockedIconBg else BrandGreenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Build, null, tint = if (locked) LockedIcon else BrandGreen, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Text("Installation Support", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = if (locked) LockedText else TextPrimary, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = TextHint, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun CriminalVerificationCard(onClick: () -> Unit) {
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
                Icon(Icons.Default.VerifiedUser, null, tint = BrandGreen, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Text("Criminal Verification", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = TextHint, modifier = Modifier.size(22.dp))
        }
    }
}

// ─────────────────────────── GENERAL SECTION ───────────────────────

private data class GeneralItemData(val title: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
private fun GeneralSection(
    onDocumentVault: () -> Unit,
    onCommunity: () -> Unit,
    onCriminalVerification: () -> Unit,
    hPad: Dp
) {
    val gridItems = listOf(
        GeneralItemData("Document\nVault", Icons.Default.FolderOpen, onDocumentVault),
        GeneralItemData("Community",       Icons.Default.Groups,      onCommunity),
    )

    Column(modifier = Modifier.padding(horizontal = hPad)) {
        Text("General", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)

        Spacer(Modifier.height(16.dp))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            gridItems.forEach { item ->
                GeneralItem(title = item.title, icon = item.icon, onClick = item.onClick, modifier = Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(12.dp))

        CriminalVerificationCard(onClick = onCriminalVerification)
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