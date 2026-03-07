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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.developer.ev2z.navigation.PlatformBackHandler

// ─────────────────────────────────────────────────────────────────────────────
//  Helper
// ─────────────────────────────────────────────────────────────────────────────

private fun parseHexColor(hex: String): Color {
    return try {
        val s = hex.removePrefix("#").removePrefix("0x").removePrefix("0X")
        val l = s.toLong(16)
        Color(if (s.length == 6) (0xFF000000L or l) else l)
    } catch (e: Exception) {
        Eva2zColors.Green
    }
}

// Podium rank colors — brand neutrals only, no rainbow
private val PodiumColors = listOf(
    Eva2zColors.Green,        // 1st — brand green
    Color(0xFF71717A),        // 2nd — zinc grey
    Color(0xFFA16207)         // 3rd — warm amber-brown
)
private val PodiumBg = listOf(
    Color(0xFFF0FAF5),        // 1st bg — pale mint
    Color(0xFFF4F4F5),        // 2nd bg — pale grey
    Color(0xFFFEFCE8)         // 3rd bg — pale amber
)

// Compare screen vehicle card gradients
private val CompareCardGradients = listOf(
    listOf(Color(0xFF22C57A), Color(0xFF1BAF6B)),   // green
    listOf(Color(0xFFD97706), Color(0xFFB45309)),   // amber
    listOf(Color(0xFF818CF8), Color(0xFF6366F1))    // soft indigo
)

// ─────────────────────────────────────────────────────────────────────────────
//  Screen 5: Leaderboard
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    state: LeaderboardUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ✅ PlatformBackHandler — intercepts Android back gesture / button
    PlatformBackHandler(enabled = true) { onBack() }

    Scaffold(containerColor = Eva2zColors.Background) { padding ->
        when (state) {

            is LeaderboardUiState.Loading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Eva2zColors.Green, strokeWidth = 2.dp)
                }

            is LeaderboardUiState.Success -> {
                val entries = state.entries
                val top3    = entries.take(3)

                LazyColumn(
                    modifier = modifier.padding(padding),
                    contentPadding = PaddingValues(bottom = 48.dp)
                ) {

                    // ── Green header ──────────────────────────────────────────
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Eva2zColors.GreenGradientStart,
                                            Eva2zColors.GreenGradientEnd
                                        )
                                    )
                                )
                                .padding(
                                    start   = Eva2zSpacing.lg,
                                    end     = Eva2zSpacing.lg,
                                    top     = Eva2zSpacing.xl,
                                    bottom  = Eva2zSpacing.xxl
                                )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "EVA2Z CONNECT",
                                        color      = Color.White.copy(alpha = 0.75f),
                                        fontSize   = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        letterSpacing = 1.5.sp
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Driver Leaderboard",
                                        color      = Color.White,
                                        fontSize   = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Top performers this month",
                                        color    = Color.White.copy(alpha = 0.70f),
                                        fontSize = 12.sp
                                    )
                                }
                                // Close button — also calls onBack, same as back gesture
                                IconButton(onClick = onBack) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }

                    // ── Podium ────────────────────────────────────────────────
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Eva2zColors.Surface)
                                .padding(Eva2zSpacing.lg)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                if (top3.size >= 2) PodiumItem(top3[1], 44.dp)
                                if (top3.isNotEmpty()) PodiumItem(top3[0], 56.dp, isFirst = true)
                                if (top3.size >= 3) PodiumItem(top3[2], 38.dp)
                            }
                        }
                        Divider(color = Eva2zColors.Border)
                    }

                    // ── All Drivers label ─────────────────────────────────────
                    item {
                        Text(
                            "All Drivers",
                            modifier = Modifier.padding(
                                horizontal = Eva2zSpacing.lg,
                                vertical   = Eva2zSpacing.md
                            ),
                            color      = Eva2zColors.TextPrimary,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(entries) { entry ->
                        LeaderboardRow(entry = entry)
                        Spacer(Modifier.height(Eva2zSpacing.sm))
                    }

                    // ── Needs attention alert ─────────────────────────────────
                    val poor = entries.filter { it.needsAttention }
                    if (poor.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = Eva2zSpacing.lg)
                                    .clip(RoundedCornerShape(Eva2zRadius.md))
                                    .background(Eva2zColors.ErrorLight)
                                    .border(
                                        1.dp,
                                        Color(0xFFFFDADA),
                                        RoundedCornerShape(Eva2zRadius.md)
                                    )
                                    .padding(Eva2zSpacing.md),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⚠️", fontSize = 16.sp)
                                Spacer(Modifier.width(Eva2zSpacing.sm))
                                Column {
                                    Text(
                                        "${poor.first().driverName} needs coaching",
                                        color      = Eva2zColors.Error,
                                        fontSize   = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "Score below 50 — action required",
                                        color    = Eva2zColors.TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            is LeaderboardUiState.Error ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = Eva2zColors.Error)
                }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Podium Item
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PodiumItem(
    entry: LeaderboardEntry,
    podiumHeight: Dp,
    isFirst: Boolean = false
) {
    val rankColor   = PodiumColors.getOrElse(entry.rank - 1) { Eva2zColors.TextSecondary }
    val rankBg      = PodiumBg.getOrElse(entry.rank - 1) { Eva2zColors.Background }
    val medalEmoji  = when (entry.rank) { 1 -> "🥇"; 2 -> "🥈"; else -> "🥉" }
    val avatarColor = parseHexColor(entry.avatarColorHex)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        if (isFirst) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Eva2zRadius.pill))
                    .background(Eva2zColors.GreenLight)
                    .padding(horizontal = Eva2zSpacing.md, vertical = 2.dp)
            ) {
                Text(
                    "BEST",
                    color      = Eva2zColors.GreenDark,
                    fontSize   = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(Modifier.height(4.dp))
        }

        Box(
            modifier = Modifier
                .size(if (isFirst) 60.dp else if (entry.rank == 2) 50.dp else 44.dp)
                .clip(CircleShape)
                .background(avatarColor.copy(alpha = 0.75f))
                .border(
                    width = if (isFirst) 2.dp else 1.5.dp,
                    color = if (isFirst) Eva2zColors.Green else Eva2zColors.Border,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                entry.avatarInitial,
                color      = Color.White,
                fontSize   = if (isFirst) 22.sp else 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(4.dp))

        val name = entry.driverName.split(" ").let {
            it.first() + " " + (it.getOrNull(1)?.firstOrNull() ?: "") + "."
        }
        Text(
            name,
            color      = Eva2zColors.TextPrimary,
            fontSize   = if (isFirst) 12.sp else 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign  = TextAlign.Center
        )
        Text(
            "${entry.score}",
            color      = rankColor,
            fontSize   = if (isFirst) 15.sp else 12.sp,
            fontWeight = FontWeight.Bold
        )

        Box(
            modifier = Modifier
                .width(if (isFirst) 76.dp else if (entry.rank == 2) 66.dp else 60.dp)
                .height(podiumHeight)
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .background(rankBg)
                .border(
                    1.dp,
                    rankColor.copy(alpha = 0.20f),
                    RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(medalEmoji, fontSize = if (isFirst) 20.sp else 16.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Leaderboard Row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LeaderboardRow(entry: LeaderboardEntry) {
    val avatarColor = parseHexColor(entry.avatarColorHex)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Eva2zSpacing.lg)
            .clip(RoundedCornerShape(Eva2zRadius.md))
            .background(Eva2zColors.Surface)
            .border(
                1.dp,
                if (entry.needsAttention) Color(0xFFFFDADA) else Eva2zColors.Border,
                RoundedCornerShape(Eva2zRadius.md)
            )
            .padding(Eva2zSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${entry.rank}",
            color      = if (entry.rank == 1) Eva2zColors.Green else Eva2zColors.TextMuted,
            fontSize   = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.width(22.dp),
            textAlign  = TextAlign.Center
        )
        Spacer(Modifier.width(Eva2zSpacing.sm))

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(avatarColor.copy(alpha = 0.70f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                entry.avatarInitial,
                color      = Color.White,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(Eva2zSpacing.sm))

        Column(Modifier.weight(1f)) {
            Text(
                entry.driverName,
                color      = Eva2zColors.TextPrimary,
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "${entry.totalTrips} trips  ·  ${entry.totalKm} km",
                color    = Eva2zColors.TextMuted,
                fontSize = 11.sp
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${entry.score}",
                color      = Eva2zColors.gradeColor(entry.grade),
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text("pts", color = Eva2zColors.TextMuted, fontSize = 9.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Screen 6: Compare Vehicles
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleCompareScreen(
    vehicles: List<VehicleScore>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ✅ PlatformBackHandler — same pattern as LeaderboardScreen
    PlatformBackHandler(enabled = true) { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Compare Vehicles",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp,
                        color      = Eva2zColors.TextPrimary
                    )
                },
                navigationIcon = {
                    // Back arrow also calls onBack — consistent with PlatformBackHandler
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Eva2zColors.Green
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Eva2zColors.Surface
                )
            )
        },
        containerColor = Eva2zColors.Background
    ) { padding ->
        LazyColumn(
            modifier = modifier.padding(padding),
            contentPadding = PaddingValues(
                start  = Eva2zSpacing.lg,
                end    = Eva2zSpacing.lg,
                top    = Eva2zSpacing.md,
                bottom = 48.dp
            ),
            verticalArrangement = Arrangement.spacedBy(Eva2zSpacing.md)
        ) {

            // ── Vehicle summary cards ─────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Eva2zSpacing.sm)
                ) {
                    vehicles.take(3).forEachIndexed { idx, v ->
                        val grad = CompareCardGradients.getOrElse(idx) {
                            listOf(Eva2zColors.Green, Eva2zColors.GreenDark)
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(Eva2zRadius.lg))
                                .background(Brush.linearGradient(grad))
                                .padding(
                                    vertical   = Eva2zSpacing.md,
                                    horizontal = Eva2zSpacing.sm
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(v.vehicleEmoji, fontSize = 20.sp)
                            Text(
                                v.vehicleName.split(" ").last(),
                                color      = Color.White,
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign  = TextAlign.Center
                            )
                            Text(
                                "${v.overallScore}",
                                color      = Color.White,
                                fontSize   = 26.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                v.grade.label.uppercase(),
                                color      = Color.White.copy(alpha = 0.80f),
                                fontSize   = 9.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    "Parameter Comparison",
                    color      = Eva2zColors.TextPrimary,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // ── Comparison table ──────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Eva2zRadius.lg))
                        .border(1.dp, Eva2zColors.Border, RoundedCornerShape(Eva2zRadius.lg))
                        .background(Eva2zColors.Surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Eva2zColors.Green,
                                shape = RoundedCornerShape(
                                    topStart = Eva2zRadius.lg,
                                    topEnd   = Eva2zRadius.lg
                                )
                            )
                            .padding(
                                horizontal = Eva2zSpacing.md,
                                vertical   = Eva2zSpacing.sm
                            )
                    ) {
                        Text(
                            "Parameter",
                            color      = Color.White.copy(alpha = 0.85f),
                            modifier   = Modifier.weight(1.4f),
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        vehicles.take(3).forEach { v ->
                            Text(
                                v.vehicleEmoji,
                                modifier  = Modifier.weight(1f),
                                fontSize  = 15.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    val ids    = listOf("acceleration", "braking", "speed", "cornering", "fuel")
                    val labels = listOf("🚀 Accel", "🛑 Braking", "📍 Speed", "🔄 Cornering", "⛽ Efficiency")

                    ids.forEachIndexed { i, id ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (i % 2 == 0) Eva2zColors.Surface else Eva2zColors.SurfaceAlt
                                )
                                .padding(
                                    horizontal = Eva2zSpacing.md,
                                    vertical   = Eva2zSpacing.sm
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                labels[i],
                                color      = Eva2zColors.TextPrimary,
                                modifier   = Modifier.weight(1.4f),
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            vehicles.take(3).forEach { v ->
                                val score = v.parameters.find { it.id == id }?.score ?: 0
                                Text(
                                    "$score",
                                    color      = Eva2zColors.gradeColor(ScoreGrade.fromScore(score)),
                                    modifier   = Modifier.weight(1f),
                                    fontSize   = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign  = TextAlign.Center
                                )
                            }
                        }
                        if (i < ids.size - 1) {
                            Divider(color = Eva2zColors.Border, thickness = 0.5.dp)
                        }
                    }
                }
            }

            // ── Insight card ──────────────────────────────────────────────────
            item {
                val best  = vehicles.maxByOrNull { it.overallScore }
                val worst = vehicles.minByOrNull { it.overallScore }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Eva2zRadius.lg))
                        .background(Eva2zColors.GreenLight)
                        .border(1.dp, Eva2zColors.GreenMid, RoundedCornerShape(Eva2zRadius.lg))
                        .padding(Eva2zSpacing.md)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text("💡", fontSize = 16.sp)
                        Spacer(Modifier.width(Eva2zSpacing.sm))
                        Column {
                            Text(
                                "Insight",
                                color      = Eva2zColors.GreenDark,
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "${best?.vehicleName} leads with a score of ${best?.overallScore}. " +
                                        "${worst?.vehicleName} needs improvement in braking and cornering.",
                                color    = Eva2zColors.TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // ── Best / Worst chips ────────────────────────────────────────────
            item {
                val best  = vehicles.maxByOrNull { it.overallScore }
                val worst = vehicles.minByOrNull { it.overallScore }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Eva2zSpacing.sm)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(Eva2zRadius.md))
                            .background(Eva2zColors.Surface)
                            .border(1.dp, Eva2zColors.GreenMid, RoundedCornerShape(Eva2zRadius.md))
                            .padding(Eva2zSpacing.md),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🏆", fontSize = 22.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Best",
                            color      = Eva2zColors.Green,
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${best?.vehicleEmoji} ${best?.vehicleName}",
                            color      = Eva2zColors.TextPrimary,
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign  = TextAlign.Center
                        )
                        Text(
                            "Score: ${best?.overallScore}",
                            color    = Eva2zColors.TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(Eva2zRadius.md))
                            .background(Eva2zColors.Surface)
                            .border(1.dp, Color(0xFFFFDADA), RoundedCornerShape(Eva2zRadius.md))
                            .padding(Eva2zSpacing.md),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("⚠️", fontSize = 22.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Needs Work",
                            color      = Eva2zColors.Error,
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${worst?.vehicleEmoji} ${worst?.vehicleName}",
                            color      = Eva2zColors.TextPrimary,
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign  = TextAlign.Center
                        )
                        Text(
                            "Score: ${worst?.overallScore}",
                            color    = Eva2zColors.TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}