//package com.developer.ev2z.ui.screens.tagHome
//
//import androidx.compose.foundation.BorderStroke
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyRow
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.remember
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//
//// ─── Colors ───────────────────────────────────────────────────────────────────
//private val NavyDark      = Color(0xFF0B1E3D)
//private val NavyMid       = Color(0xFF163561)
//private val GreenPrimary  = Color(0xFF00C853)
//private val GreenDark     = Color(0xFF00A844)
//private val GreenSoft     = Color(0xFF4ADE80)
//private val PageBg        = Color(0xFFF5F6FA)
//private val CardBg        = Color.White
//private val TextPrimary   = Color(0xFF111827)
//private val TextSecondary = Color(0xFF9CA3AF)
//private val TextMuted     = Color(0xFFC4C9D4)
//private val AmberColor    = Color(0xFFF59E0B)
//private val AmberLight    = Color(0xFFFDE68A)
//private val BorderLight   = Color(0xFFE5E7EB)
//
//// ================= ENTRY ROUTE =================
//
//@Composable
//fun TagHomeRoute(
//    userName: String? = null,
//    onGetGps: () -> Unit = {},
//    onViewVault: () -> Unit = {},
//    onDocTapped: (String) -> Unit = {},
//    onOpenCommunity: () -> Unit = {}
//) {
//    val viewModel = remember { TagHomeViewModel() }
//    if (userName != null) {
//        remember(userName) { viewModel.loadInitialData(userName); userName }
//    }
//    val state by viewModel.uiState.collectAsState()
//
//    TagHomeScreen(
//        state = state,
//        onGetGps = { viewModel.onGetGpsClicked(); onGetGps() },
//        onViewVault = { viewModel.onViewVaultClicked(); onViewVault() },
//        onDocTapped = { id -> viewModel.onDocumentTapped(id); onDocTapped(id) },
//        onOpenCommunity = onOpenCommunity
//    )
//}
//
//// ================= MAIN SCREEN =================
//
//@Composable
//fun TagHomeScreen(
//    state: TagHomeUiState,
//    onGetGps: () -> Unit,
//    onViewVault: () -> Unit,
//    onDocTapped: (String) -> Unit,
//    onOpenCommunity: () -> Unit
//) {
//    Scaffold(containerColor = PageBg) { padding ->
//        if (state.isLoading) {
//            Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(padding),
//                contentAlignment = Alignment.Center
//            ) { CircularProgressIndicator(color = GreenPrimary) }
//            return@Scaffold
//        }
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(padding)
//                .verticalScroll(rememberScrollState())
//        ) {
//            state.vehicle?.let {
//                TagHero(state.userName, it, state.latestScanAlert)
//            }
//            Spacer(Modifier.height(12.dp))
//            state.safetyScore?.let { TagSafetyScoreCard(it) }
//            Spacer(Modifier.height(12.dp))
//            if (state.documents.isNotEmpty()) {
//                TagDocVault(state.documents, onViewVault, onDocTapped)
//                Spacer(Modifier.height(12.dp))
//            }
//            if (state.features.isNotEmpty()) {
//                TagFeatures(state.features)
//                Spacer(Modifier.height(12.dp))
//            }
//            TagGpsUpsell(onGetGps)
//            Spacer(Modifier.height(12.dp))
//            if (state.communityAlerts.isNotEmpty()) {
//                TagCommunity(state.communityAlerts, onOpenCommunity)
//            }
//            Spacer(Modifier.height(16.dp))
//        }
//    }
//}
//
//// ─── Hero ─────────────────────────────────────────────────────────────────────
//@Composable
//private fun TagHero(userName: String, vehicle: TagVehicleInfo, scanAlert: TagScanAlert?) {
//    Box(
//        modifier = Modifier
//            .fillMaxWidth()
//            .background(Brush.linearGradient(listOf(NavyDark, NavyMid, NavyDark)))
//            .padding(horizontal = 18.dp, vertical = 20.dp)
//    ) {
//        Column {
//            Row(
//                Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.Top
//            ) {
//                Column {
//                    Text("Welcome back", color = Color.White.copy(0.38f), fontSize = 11.sp)
//                    Text(
//                        userName,
//                        color = Color.White,
//                        fontSize = 20.sp,
//                        fontWeight = FontWeight.ExtraBold,
//                        letterSpacing = (-0.3).sp
//                    )
//                }
//                Box(
//                    modifier = Modifier
//                        .size(36.dp)
//                        .background(Color.White.copy(0.07f), RoundedCornerShape(12.dp))
//                        .border(0.5.dp, Color.White.copy(0.1f), RoundedCornerShape(12.dp)),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Text("\uD83D\uDD14", fontSize = 14.sp)
//                    Box(
//                        modifier = Modifier
//                            .align(Alignment.TopEnd)
//                            .offset(x = (-6).dp, y = 6.dp)
//                            .size(7.dp)
//                            .background(GreenPrimary, CircleShape)
//                            .border(1.5.dp, NavyDark, CircleShape)
//                    )
//                }
//            }
//
//            Spacer(Modifier.height(14.dp))
//
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .background(Color.White.copy(0.06f), RoundedCornerShape(16.dp))
//                    .border(0.5.dp, Color.White.copy(0.1f), RoundedCornerShape(16.dp))
//                    .padding(11.dp),
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.spacedBy(10.dp)
//            ) {
//                Box(
//                    modifier = Modifier
//                        .size(38.dp)
//                        .background(Color.White, RoundedCornerShape(10.dp)),
//                    contentAlignment = Alignment.Center
//                ) { Text("\u2B1B", fontSize = 20.sp) }
//
//                Column(Modifier.weight(1f)) {
//                    Text(
//                        vehicle.name,
//                        color = Color.White,
//                        fontSize = 13.sp,
//                        fontWeight = FontWeight.SemiBold
//                    )
//                    Text(
//                        vehicle.plate,
//                        color = Color.White.copy(0.38f),
//                        fontSize = 10.sp
//                    )
//                }
//                if (vehicle.isLive) {
//                    Box(
//                        modifier = Modifier
//                            .background(GreenPrimary.copy(0.15f), RoundedCornerShape(20.dp))
//                            .border(0.5.dp, GreenPrimary.copy(0.3f), RoundedCornerShape(20.dp))
//                            .padding(horizontal = 10.dp, vertical = 4.dp)
//                    ) {
//                        Text(
//                            "● LIVE",
//                            color = GreenSoft,
//                            fontSize = 9.sp,
//                            fontWeight = FontWeight.Bold
//                        )
//                    }
//                }
//            }
//
//            scanAlert?.let {
//                Spacer(Modifier.height(10.dp))
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .background(Color(0xFFFDE68A).copy(0.08f), RoundedCornerShape(12.dp))
//                        .border(0.5.dp, Color(0xFFFDE68A).copy(0.2f), RoundedCornerShape(12.dp))
//                        .padding(12.dp, 9.dp),
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.spacedBy(9.dp)
//                ) {
//                    Box(
//                        Modifier
//                            .size(7.dp)
//                            .background(Color(0xFFFCD34D), CircleShape)
//                    )
//                    Column {
//                        Text(
//                            "QR Scanned " + it.timeAgo,
//                            color = Color(0xFFFEF9C3),
//                            fontSize = 11.sp,
//                            fontWeight = FontWeight.SemiBold
//                        )
//                        Text(
//                            it.location,
//                            color = Color(0xFFFDE68A),
//                            fontSize = 10.sp
//                        )
//                    }
//                }
//            }
//        }
//    }
//}
//
//// ─── Safety Score ─────────────────────────────────────────────────────────────
//@Composable
//private fun TagSafetyScoreCard(score: TagSafetyScore) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 14.dp),
//        shape = RoundedCornerShape(18.dp),
//        colors = CardDefaults.cardColors(containerColor = CardBg),
//        elevation = CardDefaults.cardElevation(0.dp),
//        border = BorderStroke(0.5.dp, BorderLight)
//    ) {
//        Column(Modifier.padding(14.dp)) {
//            Row(
//                Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Column {
//                    Text(
//                        "Vehicle Safety Score",
//                        fontSize = 12.sp,
//                        fontWeight = FontWeight.Bold,
//                        color = TextPrimary
//                    )
//                    Text(
//                        "Complete profile to improve",
//                        fontSize = 10.sp,
//                        color = TextSecondary
//                    )
//                }
//                Row(verticalAlignment = Alignment.Bottom) {
//                    Text(
//                        "${score.score}",
//                        fontSize = 28.sp,
//                        fontWeight = FontWeight.ExtraBold,
//                        color = AmberColor,
//                        lineHeight = 28.sp
//                    )
//                    Text(" /${score.maxScore}", fontSize = 11.sp, color = TextMuted)
//                }
//            }
//            Spacer(Modifier.height(10.dp))
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(5.dp)
//                    .clip(RoundedCornerShape(6.dp))
//                    .background(Color(0xFFF3F4F6))
//            ) {
//                Box(
//                    modifier = Modifier
//                        .fillMaxWidth(score.percentage.coerceIn(0f, 1f))
//                        .fillMaxHeight()
//                        .background(Brush.horizontalGradient(listOf(AmberColor, AmberLight)))
//                )
//            }
//            Spacer(Modifier.height(7.dp))
//            Text(
//                "\uD83D\uDCA1 ${score.hint}",
//                fontSize = 10.sp,
//                color = AmberColor,
//                lineHeight = 14.sp
//            )
//        }
//    }
//}
//
//// ─── Doc Vault ────────────────────────────────────────────────────────────────
//@Composable
//private fun TagDocVault(
//    docs: List<TagDocItem>,
//    onViewAll: () -> Unit,
//    onDocTapped: (String) -> Unit
//) {
//    Column(Modifier.padding(horizontal = 14.dp)) {
//        Row(
//            Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Text(
//                "\uD83D\uDCC1 Document Vault",
//                fontSize = 13.sp,
//                fontWeight = FontWeight.Bold,
//                color = TextPrimary
//            )
//            Text(
//                "View all →",
//                fontSize = 10.sp,
//                color = GreenDark,
//                fontWeight = FontWeight.SemiBold,
//                modifier = Modifier.clickable { onViewAll() }
//            )
//        }
//        Spacer(Modifier.height(9.dp))
//        docs.chunked(2).forEach { rowDocs ->
//            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
//                rowDocs.forEach { doc ->
//                    TagDocCard(doc, { onDocTapped(doc.id) }, Modifier.weight(1f))
//                }
//                if (rowDocs.size == 1) Spacer(Modifier.weight(1f))
//            }
//            Spacer(Modifier.height(7.dp))
//        }
//    }
//}
//
//@Composable
//private fun TagDocCard(doc: TagDocItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
//    val (badgeBg, badgeText, badgeLabel) = when (doc.status) {
//        TagDocStatus.VALID    -> Triple(Color(0xFFDCFCE7), Color(0xFF16A34A), "VALID")
//        TagDocStatus.EXPIRED  -> Triple(Color(0xFFFEE2E2), Color(0xFFDC2626), "EXPIRED")
//        TagDocStatus.EXPIRING -> Triple(Color(0xFFFEF9C3), Color(0xFFCA8A04), "EXPIRING")
//        TagDocStatus.MISSING  -> Triple(Color(0xFFF3F4F6), TextSecondary,     "ADD")
//    }
//    Card(
//        modifier = modifier.clickable { onClick() },
//        shape = RoundedCornerShape(14.dp),
//        colors = CardDefaults.cardColors(containerColor = CardBg),
//        elevation = CardDefaults.cardElevation(0.dp),
//        border = BorderStroke(0.5.dp, BorderLight)
//    ) {
//        Column(Modifier.padding(11.dp)) {
//            Row(
//                Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                Text(doc.emoji, fontSize = 20.sp)
//                Box(
//                    modifier = Modifier
//                        .background(badgeBg, RoundedCornerShape(20.dp))
//                        .padding(horizontal = 7.dp, vertical = 2.dp)
//                ) {
//                    Text(
//                        badgeLabel,
//                        fontSize = 8.sp,
//                        fontWeight = FontWeight.Bold,
//                        color = badgeText
//                    )
//                }
//            }
//            Spacer(Modifier.height(7.dp))
//            Text(
//                doc.name,
//                fontSize = 11.sp,
//                fontWeight = FontWeight.SemiBold,
//                color = TextPrimary
//            )
//            Text(
//                doc.subtitle,
//                fontSize = 9.sp,
//                color = TextSecondary,
//                modifier = Modifier.padding(top = 2.dp)
//            )
//        }
//    }
//}
//
//// ─── Features ─────────────────────────────────────────────────────────────────
//@Composable
//private fun TagFeatures(features: List<TagFeatureItem>) {
//    Column(Modifier.padding(horizontal = 14.dp)) {
//        Row(
//            Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Text(
//                "\u26A1 Vehicle Features",
//                fontSize = 13.sp,
//                fontWeight = FontWeight.Bold,
//                color = TextPrimary
//            )
//            Box(
//                modifier = Modifier
//                    .background(Color(0xFFFEE2E2), RoundedCornerShape(20.dp))
//                    .padding(horizontal = 9.dp, vertical = 3.dp)
//            ) {
//                Text(
//                    "GPS REQUIRED",
//                    fontSize = 8.sp,
//                    fontWeight = FontWeight.Bold,
//                    color = Color(0xFFB91C1C)
//                )
//            }
//        }
//        Spacer(Modifier.height(10.dp))
//        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//            items(features) { feat -> TagFeatureCard(feat) }
//        }
//    }
//}
//
//@Composable
//private fun TagFeatureCard(feat: TagFeatureItem) {
//    val glowColor = Color(feat.colorHex)
//    Box(
//        modifier = Modifier
//            .width(130.dp)
//            .clip(RoundedCornerShape(18.dp))
//            .background(NavyDark, RoundedCornerShape(18.dp))
//            .border(0.5.dp, Color.White.copy(0.07f), RoundedCornerShape(18.dp))
//            .padding(13.dp, 14.dp)
//    ) {
//        Box(
//            modifier = Modifier
//                .size(100.dp)
//                .align(Alignment.TopEnd)
//                .offset(x = 30.dp, y = (-30).dp)
//                .background(
//                    Brush.radialGradient(listOf(glowColor.copy(0.25f), Color.Transparent)),
//                    CircleShape
//                )
//        )
//        Column {
//            Box(
//                modifier = Modifier
//                    .size(38.dp)
//                    .background(glowColor.copy(0.15f), RoundedCornerShape(12.dp)),
//                contentAlignment = Alignment.Center
//            ) { Text(feat.emoji, fontSize = 18.sp) }
//            Spacer(Modifier.height(10.dp))
//            Text(
//                feat.name,
//                color = Color.White,
//                fontSize = 12.sp,
//                fontWeight = FontWeight.Bold,
//                lineHeight = 16.sp
//            )
//            Text(
//                feat.description,
//                color = Color.White.copy(0.35f),
//                fontSize = 9.sp,
//                lineHeight = 13.sp,
//                modifier = Modifier.padding(top = 3.dp),
//                maxLines = 3,
//                overflow = TextOverflow.Clip
//            )
//            Spacer(Modifier.height(10.dp))
//            if (feat.isLocked) {
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.spacedBy(4.dp)
//                ) {
//                    Box(
//                        modifier = Modifier
//                            .size(16.dp)
//                            .background(Color.White.copy(0.07f), RoundedCornerShape(5.dp)),
//                        contentAlignment = Alignment.Center
//                    ) { Text("\uD83D\uDD12", fontSize = 9.sp) }
//                    Text("Locked", color = Color.White.copy(0.3f), fontSize = 9.sp)
//                }
//            }
//        }
//    }
//}
//
//// ─── GPS Upsell ───────────────────────────────────────────────────────────────
//@Composable
//private fun TagGpsUpsell(onGetGps: () -> Unit) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 14.dp)
//            .background(
//                Brush.linearGradient(listOf(NavyDark, NavyMid)),
//                RoundedCornerShape(18.dp)
//            )
//            .border(0.5.dp, GreenPrimary.copy(0.2f), RoundedCornerShape(18.dp))
//            .padding(15.dp),
//        horizontalArrangement = Arrangement.spacedBy(12.dp),
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        Column(Modifier.weight(1f)) {
//            Text(
//                "Unlock GPS Protection",
//                color = Color.White,
//                fontSize = 14.sp,
//                fontWeight = FontWeight.ExtraBold,
//                lineHeight = 18.sp
//            )
//            Text(
//                "Your Tag identifies your vehicle.\nGPS tracks, protects & recovers it.",
//                color = Color.White.copy(0.38f),
//                fontSize = 10.sp,
//                lineHeight = 15.sp,
//                modifier = Modifier.padding(top = 4.dp)
//            )
//            Row(
//                modifier = Modifier.padding(top = 8.dp),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Text("Starting at ", color = Color.White.copy(0.3f), fontSize = 10.sp)
//                Text(
//                    "₹1,499",
//                    color = GreenSoft,
//                    fontSize = 15.sp,
//                    fontWeight = FontWeight.Bold
//                )
//            }
//        }
//        Box(
//            modifier = Modifier
//                .clip(RoundedCornerShape(14.dp))
//                .background(GreenPrimary, RoundedCornerShape(14.dp))
//                .clickable { onGetGps() }
//                .padding(horizontal = 14.dp, vertical = 10.dp),
//            contentAlignment = Alignment.Center
//        ) {
//            Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                Text("Get GPS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
//                Text("Device →", color = Color.White.copy(0.8f), fontSize = 8.sp)
//            }
//        }
//    }
//}
//
//// ─── Community ────────────────────────────────────────────────────────────────
//@Composable
//private fun TagCommunity(alerts: List<TagCommunityAlert>, onOpenCommunity: () -> Unit) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 14.dp)
//            .clickable { onOpenCommunity() },
//        shape = RoundedCornerShape(18.dp),
//        colors = CardDefaults.cardColors(containerColor = CardBg),
//        elevation = CardDefaults.cardElevation(0.dp),
//        border = BorderStroke(0.5.dp, BorderLight)
//    ) {
//        Column(Modifier.padding(12.dp)) {
//            Row(
//                Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Text(
//                    "\uD83D\uDEA8 Community Alerts",
//                    fontSize = 12.sp,
//                    fontWeight = FontWeight.Bold,
//                    color = TextPrimary
//                )
//                Box(
//                    modifier = Modifier
//                        .background(Color(0xFFEFF6FF), RoundedCornerShape(20.dp))
//                        .padding(horizontal = 8.dp, vertical = 2.dp)
//                ) {
//                    Text(
//                        "NEARBY",
//                        fontSize = 8.sp,
//                        fontWeight = FontWeight.Bold,
//                        color = Color(0xFF3B82F6)
//                    )
//                }
//            }
//            Spacer(Modifier.height(9.dp))
//            alerts.forEachIndexed { i, alert ->
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(vertical = 7.dp),
//                    horizontalArrangement = Arrangement.spacedBy(8.dp),
//                    verticalAlignment = Alignment.Top
//                ) {
//                    Box(
//                        modifier = Modifier
//                            .size(28.dp)
//                            .background(Color(alert.avatarColor), CircleShape),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        Text(
//                            alert.initial,
//                            fontSize = 11.sp,
//                            fontWeight = FontWeight.Bold,
//                            color = Color(alert.avatarTextColor)
//                        )
//                    }
//                    Text(
//                        alert.message,
//                        fontSize = 10.sp,
//                        color = Color(0xFF374151),
//                        lineHeight = 15.sp,
//                        modifier = Modifier.weight(1f)
//                    )
//                    Text(alert.timeAgo, fontSize = 9.sp, color = TextMuted)
//                }
//                if (i < alerts.lastIndex)
//                    HorizontalDivider(color = Color(0xFFF9FAFB), thickness = 0.5.dp)
//            }
//        }
//    }
//}