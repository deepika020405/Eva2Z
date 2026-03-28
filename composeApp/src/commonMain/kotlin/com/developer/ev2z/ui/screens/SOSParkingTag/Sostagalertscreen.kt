package com.developer.ev2z.ui.screens.SOSParkingTag


import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.navigation.PlatformBackHandler

// ─────────────────────────────────────────────────────────────────
// Design Tokens — Orange theme for SOS Tag (separate from BrandGreen)
// ─────────────────────────────────────────────────────────────────

internal val SosOrange       = Color(0xFFE8471A)
internal val SosOrangeLight  = Color(0xFFFFF1ED)
internal val SosOrangeMid    = Color(0xFFFFD5C8)
internal val BackgroundColor = Color(0xFFF6F6F6)
internal val CardColor       = Color.White
internal val TextPrimary     = Color(0xFF1A1A1A)
internal val TextSecondary   = Color(0xFF6B7280)
internal val TextTertiary    = Color(0xFF9CA3AF)
internal val BorderColor     = Color(0xFFEAECF0)
internal val ErrorRed        = Color(0xFFDC2626)
internal val SuccessGreen    = Color(0xFF16A34A)
internal val SuccessLight    = Color(0xFFDCFCE7)

// ─────────────────────────────────────────────────────────────────
// SosTagAlertScreen — Screen 1: Alert Detail + Respond Now
// ─────────────────────────────────────────────────────────────────

@Composable
fun SosTagAlertScreen(
    alertId: Int,
    viewModel: SosTagViewModel,
    onRespondNow: () -> Unit,
    onIgnore: () -> Unit,
    onBackPressed: () -> Unit
) {

    PlatformBackHandler(enabled = true) { onBackPressed() }

    val alertState    by viewModel.alertDetailState.collectAsState()
    val alertListState by viewModel.alertListState.collectAsState()  // ✅ ADD

    LaunchedEffect(alertId) {
        if (alertId > 0) {
            viewModel.loadAlertDetail(alertId)
        } else {
            viewModel.loadAlertList()  // ✅ manual open
        }
    }

    // ✅ Manual open mein alertListState se latest alert lo
    val effectiveState = if (alertId > 0) {
        alertState
    } else {
        when (val s = alertListState) {
            is SosAlertListUiState.Loading -> SosAlertUiState.Loading
            is SosAlertListUiState.Error   -> SosAlertUiState.Error(s.message)
            is SosAlertListUiState.Success -> {
                val latest = s.alerts.firstOrNull()
                if (latest != null) SosAlertUiState.Success(latest)
                else SosAlertUiState.Error("No alerts found")
            }
            else -> SosAlertUiState.Idle
        }
    }

    Scaffold(
        containerColor = BackgroundColor,
        topBar = { SosAlertTopBar(onBackPressed = onBackPressed) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = effectiveState) {
                is SosAlertUiState.Loading -> {
                    CircularProgressIndicator(
                        color    = SosOrange,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is SosAlertUiState.Error -> {
                    SosErrorState(
                        message = state.message,
                        onRetry = {
                            if (alertId > 0) viewModel.loadAlertDetail(alertId)
                            else viewModel.loadAlertList()
                        },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is SosAlertUiState.Success -> {
                    SosAlertContent(
                        alert        = state.alert,
                        onRespondNow = onRespondNow,
                        onIgnore     = onIgnore
                    )
                }

                // ✅ YAHAN ADD KARO
                is SosAlertUiState.Idle -> {
                    NoAlertState(modifier = Modifier.align(Alignment.Center))
                }

                else -> Unit
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Top Bar
// ─────────────────────────────────────────────────────────────────

@Composable
private fun SosAlertTopBar(onBackPressed: () -> Unit) {
    Surface(color = CardColor, shadowElevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick  = onBackPressed,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BackgroundColor)
            ) {
                Icon(
                    Icons.Default.ArrowBack, null,
                    tint     = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // EVA2Z SOS Tag title with brand icon
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier              = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SosOrangeLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Nfc, null,
                        tint     = SosOrange,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    "Eva2z SOS Tag",
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Main Content
// ─────────────────────────────────────────────────────────────────

@Composable
private fun SosAlertContent(
    alert: SosAlert,
    onRespondNow: () -> Unit,
    onIgnore: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // ── Critical Notification Banner ──────────────────────
        CriticalNotificationBanner()

        // ── Registered Vehicle Card ───────────────────────────
        RegisteredVehicleCard(alert = alert)

        // ── Reason for Alert ──────────────────────────────────
        ReasonForAlertCard(alert = alert)

        // ── Reporter's Message ────────────────────────────────
        ReporterMessageCard(alert = alert)

        Spacer(Modifier.height(8.dp))

        // ── Respond Now Button ────────────────────────────────
        Button(
            onClick  = onRespondNow,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape    = RoundedCornerShape(50),
            colors   = ButtonDefaults.buttonColors(containerColor = SosOrange),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(
                Icons.Default.Reply, null,
                modifier = Modifier.size(20.dp),
                tint     = Color.White
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Respond Now",
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
        }

        // ── Ignore Alert ──────────────────────────────────────
        TextButton(
            onClick  = onIgnore,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Ignore Alert",
                fontSize = 14.sp,
                color    = TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ─────────────────────────────────────────────────────────────────
// Critical Notification Banner
// ─────────────────────────────────────────────────────────────────

@Composable
private fun CriticalNotificationBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SosOrangeLight)
            .border(1.dp, SosOrangeMid, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment     = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.Warning, null,
            tint     = SosOrange,
            modifier = Modifier.size(22.dp)
        )
        Column {
            Text(
                "Critical Notification",
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                color      = SosOrange
            )
            Text(
                "Someone reported an issue with your vehicle.",
                fontSize   = 13.sp,
                color      = Color(0xFF7C2D12),
                lineHeight = 18.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Registered Vehicle Card
// ─────────────────────────────────────────────────────────────────

@Composable
private fun RegisteredVehicleCard(alert: SosAlert) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        colors    = CardDefaults.cardColors(containerColor = CardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "REPORTER DETAILS",
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.SemiBold,
                    color         = SosOrange,
                    letterSpacing = 0.8.sp
                )
                Text(
                    alert.visitor_name ?: "Unknown",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = TextPrimary
                )
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Outlined.Phone, null,
                        tint     = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        alert.visitor_phone ?: "–",
                        fontSize = 13.sp,
                        color    = TextSecondary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(width = 70.dp, height = 70.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SosOrangeLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person, null,
                    tint     = SosOrange,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
private fun ReasonForAlertCard(alert: SosAlert) {
    // ✅ reason field se directly map karo
    val alertType = AlertType.from(alert.reason)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("REASON FOR ALERT")

        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            colors    = CardDefaults.cardColors(containerColor = CardColor),
            border    = BorderStroke(1.5.dp, SosOrange)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SosOrangeLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(alertType.emoji, fontSize = 18.sp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        // ✅ actual reason dikhao
                        alert.reason ?: alertType.displayLabel,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary
                    )
                    Text(
                        "Reported by ${alert.visitor_name ?: "someone"}",
                        fontSize = 12.sp,
                        color    = TextSecondary
                    )
                }

                Icon(
                    Icons.Default.CheckCircle, null,
                    tint     = SosOrange,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun ReporterMessageCard(alert: SosAlert) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            SectionLabel("REPORTER'S MESSAGE")
            // ✅ created_at dikhao
            alert.created_at?.let {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule, null,
                        tint     = TextTertiary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        it.take(10),  // date only
                        fontSize = 11.sp,
                        color    = TextTertiary
                    )
                }
            }
        }

        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            colors    = CardDefaults.cardColors(containerColor = CardColor)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Default.FormatQuote, null,
                    tint     = SosOrange,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    // ✅ reason as message
                    alert.reason ?: "No message provided.",
                    fontSize   = 14.sp,
                    color      = TextPrimary,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Section Label helper
// ─────────────────────────────────────────────────────────────────

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text,
        fontSize      = 11.sp,
        fontWeight    = FontWeight.SemiBold,
        color         = TextSecondary,
        letterSpacing = 0.6.sp
    )
}

// ─────────────────────────────────────────────────────────────────
// Error State
// ─────────────────────────────────────────────────────────────────

@Composable
private fun SosErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier            = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.ErrorOutline, null,
            tint     = ErrorRed,
            modifier = Modifier.size(48.dp)
        )
        Text(message, fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center)
        OutlinedButton(onClick = onRetry) {
            Text("Retry", color = SosOrange)
        }
    }
}


@Composable
private fun NoAlertState(modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Icon ──
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(SosOrangeLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.NotificationsNone,
                contentDescription = null,
                tint     = SosOrange,
                modifier = Modifier.size(40.dp)
            )
        }

        // ── Title ──
        Text(
            "No Active Alerts",
            fontSize   = 18.sp,
            fontWeight = FontWeight.Bold,
            color      = TextPrimary,
            textAlign  = TextAlign.Center
        )

        // ── Subtitle ──
        Text(
            "You'll be notified here when someone\nscans your EVA2Z SOS Tag.",
            fontSize   = 13.sp,
            color      = TextSecondary,
            lineHeight = 20.sp,
            textAlign  = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        // ── Info card ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SosOrangeLight)
                .border(1.dp, SosOrangeMid, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalAlignment     = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint     = SosOrange,
                modifier = Modifier.size(18.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "How it works",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color      = SosOrange
                )
                Text(
                    "When someone scans your vehicle's QR tag and reports an issue, you'll receive a notification and can respond directly from here.",
                    fontSize   = 12.sp,
                    color      = Color(0xFF7C2D12),
                    lineHeight = 18.sp
                )
            }
        }
    }
}