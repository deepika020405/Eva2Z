package com.developer.ev2z.ui.screens.DriverScore

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.developer.ev2z.navigation.PlatformBackHandler

// ─────────────────────────────────────────────────────────────────────────────
//  Entry — routes to Single / Multi
//  PlatformBackHandler here handles the root dashboard back → HomeNavGraph
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DriverScoreScreen(
    uiState: DriverScoreUiState,
    onSelectVehicle: (Int) -> Unit,
    onNavigateToDetail: (VehicleScore) -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToCompare: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ✅ Root-level back — exits Driver Score flow back to HomeNavGraph
    PlatformBackHandler(enabled = true) { onBack() }

    when (uiState) {
        is DriverScoreUiState.Loading ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Eva2zColors.Green, strokeWidth = 2.dp)
            }
        is DriverScoreUiState.SingleVehicle ->
            SingleVehicleScoreScreen(
                vehicle = uiState.vehicle,
                onNavigateToDetail = { onNavigateToDetail(uiState.vehicle) },
                onNavigateToLeaderboard = onNavigateToLeaderboard,
                onBack = onBack,
                modifier = modifier
            )
        is DriverScoreUiState.MultiVehicle ->
            MultiVehicleScoreScreen(
                state = uiState,
                onSelectVehicle = onSelectVehicle,
                onNavigateToDetail = onNavigateToDetail,
                onNavigateToCompare = onNavigateToCompare,
                onBack = onBack,
                modifier = modifier
            )
        is DriverScoreUiState.Error ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(uiState.message, color = Eva2zColors.Error, fontSize = 14.sp)
            }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Single Vehicle Dashboard
//  NOTE: PlatformBackHandler is already handled by parent DriverScoreScreen
//        No need to add it again here — would double-trigger
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleVehicleScoreScreen(
    vehicle: VehicleScore,
    onNavigateToDetail: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Driver Score",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Eva2zColors.TextPrimary
                        )
                        Text(
                            vehicle.period,
                            fontSize = 11.sp,
                            color = Eva2zColors.TextMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Eva2zColors.Green)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToLeaderboard) {
                        Icon(Icons.Default.EmojiEvents, null, tint = Eva2zColors.TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Eva2zColors.Surface)
            )
        },
        containerColor = Eva2zColors.Background
    ) { padding ->
        LazyColumn(
            modifier = modifier.padding(padding),
            contentPadding = PaddingValues(
                horizontal = Eva2zSpacing.lg,
                vertical   = Eva2zSpacing.md
            ),
            verticalArrangement = Arrangement.spacedBy(Eva2zSpacing.sm)
        ) {
            item { ScoreHeroCard(vehicle = vehicle) }
            item { SectionTitle("Key Metrics") }
            item {
                val metrics = buildMetrics(vehicle)
                Column(verticalArrangement = Arrangement.spacedBy(Eva2zSpacing.sm)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Eva2zSpacing.sm)) {
                        metrics.take(2).forEach { m ->
                            MetricCard(
                                emoji = m.emoji, label = m.label, value = m.value,
                                statusText = m.statusText, statusColor = m.statusColor,
                                bgColor = Eva2zColors.Surface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Eva2zSpacing.sm)) {
                        metrics.drop(2).forEach { m ->
                            MetricCard(
                                emoji = m.emoji, label = m.label, value = m.value,
                                statusText = m.statusText, statusColor = m.statusColor,
                                bgColor = Eva2zColors.Surface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            item { SectionTitle("Recent Trips") }
            items(vehicle.recentTrips) { TripItem(trip = it) }
            item { Spacer(Modifier.height(Eva2zSpacing.sm)) }
            item {
                Button(
                    onClick = onNavigateToDetail,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Eva2zRadius.lg),
                    colors = ButtonDefaults.buttonColors(containerColor = Eva2zColors.Green),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text(
                        "View Full Score Breakdown",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Score Detail Screen
//  ✅ PlatformBackHandler → goes back to Dashboard route
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreDetailScreen(
    vehicle: VehicleScore,
    allVehicles: List<VehicleScore> = emptyList(),
    selectedVehicleIndex: Int = 0,
    onSelectVehicle: ((Int) -> Unit)? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ✅ Back gesture / button → goes back to Dashboard
    PlatformBackHandler(enabled = true) { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Score Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Eva2zColors.TextPrimary
                    )
                },
                navigationIcon = {
                    // ✅ Arrow button also calls same onBack — consistent
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Eva2zColors.Green)
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Share, null, tint = Eva2zColors.TextMuted)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Eva2zColors.Surface)
            )
        },
        containerColor = Eva2zColors.Background
    ) { padding ->
        LazyColumn(
            modifier = modifier.padding(padding),
            contentPadding = PaddingValues(bottom = Eva2zSpacing.xxxl)
        ) {

            // Vehicle tabs
            if (allVehicles.size > 1 && onSelectVehicle != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Eva2zColors.Surface)
                            .horizontalScroll(rememberScrollState())
                            .padding(
                                horizontal = Eva2zSpacing.lg,
                                vertical   = Eva2zSpacing.md
                            ),
                        horizontalArrangement = Arrangement.spacedBy(Eva2zSpacing.sm)
                    ) {
                        allVehicles.forEachIndexed { idx, v ->
                            VehicleTabPill(
                                vehicle    = v,
                                isSelected = idx == selectedVehicleIndex,
                                onClick    = { onSelectVehicle(idx) }
                            )
                        }
                    }
                    Divider(color = Eva2zColors.Border, thickness = 1.dp)
                }
            }

            // Driver profile row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Eva2zSpacing.lg, vertical = Eva2zSpacing.md)
                        .clip(RoundedCornerShape(Eva2zRadius.xl))
                        .background(Eva2zColors.Surface)
                        .border(1.dp, Eva2zColors.Border, RoundedCornerShape(Eva2zRadius.xl))
                        .padding(Eva2zSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Eva2zColors.Green),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = vehicle.driverName.firstOrNull()?.toString()
                                ?: vehicle.vehicleEmoji,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(Eva2zSpacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = vehicle.driverName.ifEmpty { vehicle.vehicleName },
                            color = Eva2zColors.TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${vehicle.vehicleEmoji}  ${vehicle.vehiclePlate}",
                            color = Eva2zColors.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    if (vehicle.isTopDriver) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(Eva2zRadius.sm))
                                .background(Eva2zColors.GreenLight)
                                .border(1.dp, Eva2zColors.GreenMid, RoundedCornerShape(Eva2zRadius.sm))
                                .padding(horizontal = Eva2zSpacing.sm, vertical = 4.dp)
                        ) {
                            Text(
                                "TOP\nDRIVER",
                                color = Eva2zColors.GreenDark,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Score summary card
            item {
                Box(
                    modifier = Modifier
                        .padding(horizontal = Eva2zSpacing.lg)
                        .clip(RoundedCornerShape(Eva2zRadius.xl))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Eva2zColors.GreenGradientStart,
                                    Eva2zColors.GreenGradientEnd
                                )
                            )
                        )
                        .padding(Eva2zSpacing.xl)
                ) {
                    Column {
                        Text(
                            "Overall Score",
                            color = Color.White.copy(alpha = 0.70f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AnimatedScoreNumber(vehicle.overallScore)
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    vehicle.grade.label.uppercase(),
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Top 20% of drivers",
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(Eva2zSpacing.md))
                        Divider(color = Color.White.copy(alpha = 0.20f))
                        Spacer(Modifier.height(Eva2zSpacing.md))
                        StatsSummaryRow(
                            totalTrips      = vehicle.totalTrips,
                            totalKm         = vehicle.totalKm,
                            safeRatePercent = vehicle.safeRatePercent
                        )
                    }
                }
            }

            // Score breakdown
            item {
                Box(modifier = Modifier.padding(horizontal = Eva2zSpacing.lg)) {
                    SectionTitle("Score Breakdown")
                }
            }
            items(vehicle.parameters) { param ->
                Box(modifier = Modifier.padding(horizontal = Eva2zSpacing.lg)) {
                    ScoreBreakdownBar(param = param)
                }
                Spacer(Modifier.height(Eva2zSpacing.sm))
            }

            // Weekly trend
            item {
                Box(modifier = Modifier.padding(horizontal = Eva2zSpacing.lg)) {
                    SectionTitle("Weekly Trend")
                }
            }
            item {
                Box(modifier = Modifier.padding(horizontal = Eva2zSpacing.lg)) {
                    WeeklyTrendChart(days = vehicle.weeklyTrend)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Multi-Vehicle Overview
//  NOTE: PlatformBackHandler is already handled by parent DriverScoreScreen
//        No need to add it again here — would double-trigger
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiVehicleScoreScreen(
    state: DriverScoreUiState.MultiVehicle,
    onSelectVehicle: (Int) -> Unit,
    onNavigateToDetail: (VehicleScore) -> Unit,
    onNavigateToCompare: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Driver Score",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Eva2zColors.TextPrimary
                        )
                        Text(
                            state.vehicles.first().period,
                            fontSize = 11.sp,
                            color = Eva2zColors.TextMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Eva2zColors.Green)
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.NotificationsNone, null, tint = Eva2zColors.TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Eva2zColors.Surface)
            )
        },
        containerColor = Eva2zColors.Background
    ) { padding ->
        LazyColumn(
            modifier = modifier.padding(padding),
            contentPadding = PaddingValues(
                horizontal = Eva2zSpacing.lg,
                vertical   = Eva2zSpacing.md
            ),
            verticalArrangement = Arrangement.spacedBy(Eva2zSpacing.sm)
        ) {

            // Fleet summary card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Eva2zRadius.xl))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Eva2zColors.GreenGradientStart,
                                    Eva2zColors.GreenGradientEnd
                                )
                            )
                        )
                        .padding(Eva2zSpacing.xl)
                ) {
                    Column {
                        Text(
                            state.vehicles.first().period,
                            color = Color.White.copy(alpha = 0.70f),
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            state.vehicles.first().driverName.ifEmpty { "Your Fleet" },
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(Eva2zSpacing.lg))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf(
                                "${state.averageScore}"      to "Avg Score",
                                "${state.vehicles.size}"     to "Vehicles",
                                "${state.totalTrips}"        to "Trips",
                                "${state.overallSafeRate}%"  to "Safe Rate"
                            ).forEachIndexed { i, (value, label) ->
                                if (i > 0) {
                                    Box(
                                        Modifier
                                            .width(1.dp)
                                            .height(32.dp)
                                            .background(Color.White.copy(alpha = 0.25f))
                                            .align(Alignment.CenterVertically)
                                    )
                                }
                                Column(
                                    Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        value,
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        label,
                                        color = Color.White.copy(alpha = 0.65f),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                SectionTitle(
                    "My Vehicles",
                    subtitle = "${state.vehicles.size} registered"
                )
            }

            itemsIndexed(state.vehicles) { _, vehicle ->
                VehicleScoreCard(
                    vehicle = vehicle,
                    onViewDetails = { onNavigateToDetail(vehicle) }
                )
            }

            // Compare CTA
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Eva2zRadius.lg))
                        .background(Eva2zColors.Surface)
                        .border(1.dp, Eva2zColors.Border, RoundedCornerShape(Eva2zRadius.lg))
                        .clickable(onClick = onNavigateToCompare)
                        .padding(Eva2zSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚖️", fontSize = 20.sp)
                    Spacer(Modifier.width(Eva2zSpacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Compare All Vehicles",
                            color = Eva2zColors.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Side-by-side score breakdown",
                            color = Eva2zColors.TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        null,
                        tint = Eva2zColors.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Vehicle Score Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VehicleScoreCard(
    vehicle: VehicleScore,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradeColor = Eva2zColors.gradeColor(vehicle.grade)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Eva2zRadius.xl))
            .background(Eva2zColors.Surface)
            .border(1.dp, Eva2zColors.Border, RoundedCornerShape(Eva2zRadius.xl))
            .padding(Eva2zSpacing.lg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(Eva2zRadius.md))
                    .background(Eva2zColors.GreenLight),
                contentAlignment = Alignment.Center
            ) {
                Text(vehicle.vehicleEmoji, fontSize = 22.sp)
            }
            Spacer(Modifier.width(Eva2zSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    vehicle.vehicleName,
                    color = Eva2zColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    vehicle.vehiclePlate,
                    color = Eva2zColors.TextMuted,
                    fontSize = 11.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${vehicle.overallScore}",
                    color = gradeColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Eva2zColors.gradeColorLight(vehicle.grade))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        vehicle.grade.label.uppercase(),
                        color = gradeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(Eva2zSpacing.md))

        // Score bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(Eva2zRadius.pill))
                .background(Eva2zColors.Border)
        ) {
            val f by animateFloatAsState(
                vehicle.overallScore / 100f, tween(900), label = "bar"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(Eva2zRadius.pill))
                    .background(gradeColor)
            )
        }

        Spacer(Modifier.height(Eva2zSpacing.md))

        // Mini metrics
        Row(horizontalArrangement = Arrangement.spacedBy(Eva2zSpacing.sm)) {
            data class Mini(val icon: String, val v: String, val l: String, val c: Color)
            listOf(
                Mini("🛑",
                    "${vehicle.parameters.find { it.id == "braking" }?.eventCount ?: 0}",
                    "Braking",
                    if ((vehicle.parameters.find { it.id == "braking" }?.score ?: 100) >= 75)
                        Eva2zColors.Success else Eva2zColors.Error),
                Mini("⚡",
                    if ((vehicle.parameters.find { it.id == "speed" }?.score ?: 100) >= 90) "0" else "!",
                    "Speed",
                    if ((vehicle.parameters.find { it.id == "speed" }?.score ?: 100) >= 90)
                        Eva2zColors.Success else Eva2zColors.Warning),
                Mini("🔄",
                    "${vehicle.parameters.find { it.id == "cornering" }?.eventCount ?: 0}",
                    "Turns",
                    if ((vehicle.parameters.find { it.id == "cornering" }?.score ?: 100) >= 75)
                        Eva2zColors.Success else Eva2zColors.Warning),
                Mini("🛣️",
                    "${vehicle.totalKm / vehicle.totalTrips.coerceAtLeast(1)}km",
                    "Avg Trip",
                    Eva2zColors.TextSecondary)
            ).forEach { m ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(Eva2zRadius.sm))
                        .background(Eva2zColors.Background)
                        .border(1.dp, Eva2zColors.Border, RoundedCornerShape(Eva2zRadius.sm))
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(m.icon, fontSize = 13.sp)
                    Text(m.v, color = m.c, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(m.l, color = Eva2zColors.TextMuted, fontSize = 9.sp)
                }
            }
        }

        Spacer(Modifier.height(Eva2zSpacing.sm))
        Divider(color = Eva2zColors.Border)
        Spacer(Modifier.height(Eva2zSpacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Last trip: ${vehicle.lastTripTime}",
                color = Eva2zColors.TextMuted,
                fontSize = 11.sp
            )
            TextButton(onClick = onViewDetails, contentPadding = PaddingValues(0.dp)) {
                Text(
                    "View Details",
                    color = Eva2zColors.Green,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    Icons.Default.ChevronRight,
                    null,
                    tint = Eva2zColors.Green,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Helpers
// ─────────────────────────────────────────────────────────────────────────────

private data class MetricData(
    val emoji: String, val label: String, val value: String,
    val statusText: String, val statusColor: Color, val bgColor: Color
)

private fun buildMetrics(vehicle: VehicleScore): List<MetricData> = listOf(
    vehicle.parameters.find { it.id == "braking" }?.let { p ->
        MetricData("🛑", "Harsh Braking", "${p.eventCount}",
            if (p.score >= 75) "No events" else "${p.eventCount} events",
            if (p.score >= 75) Eva2zColors.Success else Eva2zColors.Error,
            Eva2zColors.Surface)
    } ?: MetricData("🛑", "Harsh Braking", "0", "No events",
        Eva2zColors.Success, Eva2zColors.Surface),

    vehicle.parameters.find { it.id == "speed" }?.let { p ->
        MetricData("⚡", "Overspeeding",
            if (p.score >= 90) "0" else "${100 - p.score}",
            if (p.score >= 90) "No violations" else "${100 - p.score} events",
            if (p.score >= 90) Eva2zColors.Success else Eva2zColors.Warning,
            Eva2zColors.Surface)
    } ?: MetricData("⚡", "Overspeeding", "0", "No violations",
        Eva2zColors.Success, Eva2zColors.Surface),

    vehicle.parameters.find { it.id == "cornering" }?.let { p ->
        MetricData("🔄", "Sharp Turns", "${p.eventCount}",
            if (p.score >= 75) "Smooth" else "${p.eventCount} events",
            if (p.score >= 75) Eva2zColors.Success else Eva2zColors.Warning,
            Eva2zColors.Surface)
    } ?: MetricData("🔄", "Sharp Turns", "0", "Smooth",
        Eva2zColors.Success, Eva2zColors.Surface),

    vehicle.parameters.find { it.id == "fuel" }?.let { p ->
        MetricData("🕐", "Idle Time", "${p.eventCount}m", "Acceptable",
            Eva2zColors.TextSecondary, Eva2zColors.Surface)
    } ?: MetricData("🕐", "Idle Time", "12m", "Acceptable",
        Eva2zColors.TextSecondary, Eva2zColors.Surface)
)